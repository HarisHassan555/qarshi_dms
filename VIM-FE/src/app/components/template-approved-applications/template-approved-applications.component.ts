import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { resolveApplicationTitle } from 'src/app/utils/application-title.util';

interface TemplateApprovedApplication {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    dteCreatedDate?: string;
    myApprovalDate?: string;
    templateName?: string;
    txtApplicationData?: string;
    applicationTitle?: string;
}

type ApprovedApplicationCsvColumnKey = 'txtFormCode' | 'applicationTitle' | 'templateName' | 'txtStatus' | 'myApprovalDate' | 'dteCreatedDate';
interface ApprovedApplicationCsvColumn { key: ApprovedApplicationCsvColumnKey; label: string; }

interface ApprovedApplicationMetric {
    label: string;
    value: number;
    tone: 'slate' | 'amber' | 'emerald' | 'rose' | 'sky';
    hint: string;
}

@Component({
    selector: 'app-template-approved-applications',
    templateUrl: './template-approved-applications.component.html',
    styleUrls: ['./template-approved-applications.component.css']
})
export class TemplateApprovedApplicationsComponent implements OnInit, OnDestroy {
    search = '';
    filterStartDate = '';
    filterEndDate = '';
    filterStatus = '';
    filterFormType = '';
    availableStatuses: string[] = [];
    availableFormTypes: string[] = [];
    applicationMetrics: ApprovedApplicationMetric[] = [];
    currentUser: any = null;
    isLoading = false;
    approvedApplications: TemplateApprovedApplication[] = [];
    showCsvModal = false;
    availableCsvColumns: ApprovedApplicationCsvColumn[] = [];
    selectedCsvColumns: ApprovedApplicationCsvColumn[] = [];
    page = 0;
    pageSize = 10;
    totalApprovedApplications = 0;
    private searchTimer: ReturnType<typeof setTimeout> | null = null;
    private applicationLoadId = 0;
    private allApprovedApplications: TemplateApprovedApplication[] = [];
    private filteredApprovedApplications: TemplateApprovedApplication[] = [];

    cols = [
        { field: 'txtFormCode', title: 'Application Code' },
        { field: 'applicationTitle', title: 'Title' },
        { field: 'templateName', title: 'Template' },
        { field: 'txtStatus', title: 'Current Status' },
        { field: 'myApprovalDate', title: 'Approved On' },
        { field: 'dteCreatedDate', title: 'Submitted Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];
    readonly csvColumns: ApprovedApplicationCsvColumn[] = [
        { key: 'txtFormCode', label: 'Application Code' },
        { key: 'applicationTitle', label: 'Title' },
        { key: 'templateName', label: 'Template' },
        { key: 'txtStatus', label: 'Current Status' },
        { key: 'myApprovalDate', label: 'Approved On' },
        { key: 'dteCreatedDate', label: 'Submitted Date' }
    ];

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService,
        private router: Router
    ) { }

    ngOnInit(): void {
        try {
            this.currentUser = JSON.parse(localStorage.getItem('user') || 'null');
        } catch {
            this.currentUser = null;
        }
        this.applicationMetrics = this.buildApplicationMetrics([]);
        this.loadApprovedApplications();
        window.addEventListener('velocity-tour-prepare', this.onTourPrepare);
    }

    ngOnDestroy(): void {
        window.removeEventListener('velocity-tour-prepare', this.onTourPrepare);
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
    }

    private onTourPrepare = (event: Event): void => {
        const action = (event as CustomEvent<{ action?: string }>).detail?.action;
        if (action === 'template-approved-close-csv') {
            this.closeCsvModal();
        } else if (action === 'template-approved-open-csv' && this.getDisplayedApprovedApplications().length > 0) {
            this.openCsvModal();
        }
    };

    loadApprovedApplications(): void {
        const userId = this.getCurrentUserId();
        const requestId = ++this.applicationLoadId;
        this.isLoading = true;
        void this.fetchAllApprovedApplications(userId)
            .then((items) => {
                if (requestId !== this.applicationLoadId) {
                    return;
                }
                this.allApprovedApplications = items.map((application: any) => this.toShallowApprovedApplication(application));
                this.availableStatuses = this.buildDistinctOptions(this.allApprovedApplications.map((item) => item.txtStatus || ''));
                this.availableFormTypes = this.buildDistinctOptions(this.allApprovedApplications.map((item) => item.templateName || ''));
                this.refreshApplicationView();
            })
            .catch((error) => {
                if (requestId !== this.applicationLoadId) {
                    return;
                }
                this.allApprovedApplications = [];
                this.filteredApprovedApplications = [];
                this.approvedApplications = [];
                this.totalApprovedApplications = 0;
                this.applicationMetrics = this.buildApplicationMetrics([]);
                this.notificationService.showMessage(
                    'Error loading template approved applications: ' + (error?.error?.message || error?.message || 'Request failed'),
                    'danger'
                );
            })
            .finally(() => {
                if (requestId === this.applicationLoadId) {
                    this.isLoading = false;
                }
            });
    }

    applyApplicationFilters(): void {
        this.page = 0;
        this.refreshApplicationView();
    }

    clearApplicationFilters(): void {
        this.search = '';
        this.filterStartDate = '';
        this.filterEndDate = '';
        this.filterStatus = '';
        this.filterFormType = '';
        this.page = 0;
        this.refreshApplicationView();
    }

    onSearchChanged(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => {
            this.page = 0;
            this.refreshApplicationView();
        }, 200);
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.refreshApplicationView();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10;
        this.page = 0;
        this.refreshApplicationView();
    }

    getMetricClasses(tone: ApprovedApplicationMetric['tone']): string {
        switch (tone) {
            case 'amber':
                return 'border-amber-200 bg-amber-50 text-amber-900';
            case 'emerald':
                return 'border-emerald-200 bg-emerald-50 text-emerald-900';
            case 'rose':
                return 'border-rose-200 bg-rose-50 text-rose-900';
            case 'sky':
                return 'border-sky-200 bg-sky-50 text-sky-900';
            default:
                return 'border-slate-200 bg-white text-slate-900';
        }
    }

    get hasAnyApprovedApplications(): boolean {
        return this.allApprovedApplications.length > 0;
    }

    openCsvModal(): void {
        if (this.getDisplayedApprovedApplications().length === 0) {
            this.notificationService.showMessage('No approved applications available to export.', 'warning');
            return;
        }
        this.availableCsvColumns = [...this.csvColumns];
        this.selectedCsvColumns = [];
        this.showCsvModal = true;
    }

    closeCsvModal(): void {
        this.showCsvModal = false;
    }

    selectAllCsvColumns(): void {
        this.availableCsvColumns = [];
        this.selectedCsvColumns = [...this.csvColumns];
    }

    unselectAllCsvColumns(): void {
        this.availableCsvColumns = [...this.csvColumns];
        this.selectedCsvColumns = [];
    }

    addCsvColumn(column: ApprovedApplicationCsvColumn): void {
        if (!this.selectedCsvColumns.some((item) => item.key === column.key)) {
            this.availableCsvColumns = this.availableCsvColumns.filter((item) => item.key !== column.key);
            this.selectedCsvColumns = [...this.selectedCsvColumns, column];
        }
    }

    removeCsvColumn(column: ApprovedApplicationCsvColumn): void {
        this.selectedCsvColumns = this.selectedCsvColumns.filter((item) => item.key !== column.key);
        const restoredColumns = [...this.availableCsvColumns, column];
        this.availableCsvColumns = this.csvColumns.filter((candidate) => restoredColumns.some((item) => item.key === candidate.key));
    }

    downloadCsv(): void {
        if (!this.selectedCsvColumns.length) {
            this.notificationService.showMessage('Select at least one column before downloading CSV.', 'warning');
            return;
        }
        const rows = this.filteredApprovedApplications;
        if (!rows.length) {
            this.notificationService.showMessage('No approved applications available to export.', 'warning');
            return;
        }
        const csvRows = [
            this.selectedCsvColumns.map((column) => this.escapeCsvValue(column.label)).join(','),
            ...rows.map((application: TemplateApprovedApplication) => this.selectedCsvColumns
                .map((column) => this.escapeCsvValue(this.getCsvCellValue(application, column.key)))
                .join(','))
        ];
        this.downloadBlob(
            new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' }),
            `template-approved-applications_${new Date().toISOString().split('T')[0]}.csv`
        );
        this.showCsvModal = false;
    }

    trackByCsvColumn(index: number, column: ApprovedApplicationCsvColumn): ApprovedApplicationCsvColumnKey {
        return column.key;
    }

    get totalPages(): number {
        return Math.max(1, Math.ceil(this.totalApprovedApplications / this.pageSize));
    }

    get pageStart(): number {
        return this.totalApprovedApplications === 0 ? 0 : (this.page * this.pageSize) + 1;
    }

    get pageEnd(): number {
        return Math.min(this.totalApprovedApplications, (this.page + 1) * this.pageSize);
    }

    getDisplayedApprovedApplications(): TemplateApprovedApplication[] {
        return this.approvedApplications || [];
    }

    getStatusBadgeClass(status: string | undefined): string {
        if (!status) return 'badge-outline-secondary';
        switch (status.toUpperCase()) {
            case 'DRAFT':
                return 'badge-outline-secondary';
            case 'PENDING':
            case 'HOLD':
                return 'badge-outline-warning';
            case 'APPROVED':
            case 'COMPLETED':
                return 'badge-outline-success';
            case 'REJECTED':
                return 'badge-outline-danger';
            case 'IN_PROGRESS':
            case 'OPINION_PENDING':
            case 'CEO_PENDING':
            case 'ASSET_PENDING':
            case 'PR_PENDING':
            case 'PO_PENDING':
            case 'PO_VENDOR_TE_PENDING':
                return 'badge-outline-info';
            default:
                return 'badge-outline-secondary';
        }
    }

    openApplication(application: TemplateApprovedApplication): void {
        if (!application.serApplicationId) {
            this.notificationService.showMessage('Invalid application ID', 'danger');
            return;
        }
        this.router.navigate(['/template-approval', application.serApplicationId]);
    }

    private getCurrentUserId(): number {
        return Number(this.currentUser?.serUserId || this.currentUser?.userId || this.currentUser?.id || 0);
    }

    private async fetchAllApprovedApplications(userId: number): Promise<any[]> {
        const pageSize = 100;
        const collected: any[] = [];
        let page = 0;
        let total = 0;
        do {
            const response = await firstValueFrom(
                this.templateWorkflowService.getTemplateApprovedApplications(userId, page, pageSize, '')
            );
            const items = Array.isArray(response?.items) ? response.items : [];
            total = Number(response?.total || 0);
            collected.push(...items);
            page += 1;
            if (items.length === 0) {
                break;
            }
        } while (collected.length < total);
        return collected;
    }

    private refreshApplicationView(): void {
        this.filteredApprovedApplications = this.getFilteredApplications();
        this.totalApprovedApplications = this.filteredApprovedApplications.length;
        const maxPage = Math.max(0, Math.ceil(this.totalApprovedApplications / this.pageSize) - 1);
        if (this.page > maxPage) {
            this.page = maxPage;
        }
        const start = this.page * this.pageSize;
        this.approvedApplications = this.filteredApprovedApplications.slice(start, start + this.pageSize);
        this.applicationMetrics = this.buildApplicationMetrics(this.filteredApprovedApplications);
    }

    private getFilteredApplications(): TemplateApprovedApplication[] {
        const query = this.search.trim().toLowerCase();
        return this.allApprovedApplications.filter((item) => {
            if (this.filterStatus && (item.txtStatus || '') !== this.filterStatus) {
                return false;
            }
            if (this.filterFormType && (item.templateName || '') !== this.filterFormType) {
                return false;
            }
            const createdAt = new Date(item.dteCreatedDate || '').getTime();
            if (this.filterStartDate) {
                const startMs = new Date(`${this.filterStartDate}T00:00:00`).getTime();
                if (Number.isFinite(startMs) && (!Number.isFinite(createdAt) || createdAt < startMs)) {
                    return false;
                }
            }
            if (this.filterEndDate) {
                const endMs = new Date(`${this.filterEndDate}T23:59:59.999`).getTime();
                if (Number.isFinite(endMs) && (!Number.isFinite(createdAt) || createdAt > endMs)) {
                    return false;
                }
            }
            if (!query) {
                return true;
            }
            const haystack = [
                item.txtFormCode,
                item.applicationTitle,
                item.templateName,
                item.txtStatus
            ].join(' ').toLowerCase();
            return haystack.includes(query);
        });
    }

    private buildApplicationMetrics(items: TemplateApprovedApplication[]): ApprovedApplicationMetric[] {
        const draft = items.filter((item) => this.isDraftStatus(item.txtStatus)).length;
        const pending = items.filter((item) => this.isOpenWorkflowStatus(item.txtStatus)).length;
        const completed = items.filter((item) => this.isCompletedStatus(item.txtStatus)).length;
        const rejected = items.filter((item) => this.isRejectedStatus(item.txtStatus)).length;
        return [
            {
                label: 'Total Applications',
                value: items.length - draft,
                tone: 'slate',
                hint: 'Submitted applications in the current filters'
            },
            {
                label: 'Draft Applications',
                value: draft,
                tone: 'sky',
                hint: 'Saved as draft and not yet submitted'
            },
            {
                label: 'Pending In Workflow',
                value: pending,
                tone: 'amber',
                hint: 'Applications still moving through approval'
            },
            {
                label: 'Completed',
                value: completed,
                tone: 'emerald',
                hint: 'Applications that reached final approval'
            },
            {
                label: 'Rejected',
                value: rejected,
                tone: 'rose',
                hint: 'Applications stopped with rejection'
            }
        ];
    }

    private isOpenWorkflowStatus(status: string | undefined): boolean {
        const normalizedStatus = String(status || '').trim().toUpperCase();
        return normalizedStatus === 'PENDING'
            || normalizedStatus === 'IN_PROGRESS'
            || normalizedStatus === 'OPINION_PENDING'
            || normalizedStatus === 'CEO_PENDING'
            || normalizedStatus === 'ASSET_PENDING'
            || normalizedStatus === 'PR_PENDING'
            || normalizedStatus === 'PO_PENDING'
            || normalizedStatus === 'PO_VENDOR_TE_PENDING';
    }

    private isCompletedStatus(status: string | undefined): boolean {
        const normalizedStatus = String(status || '').trim().toUpperCase();
        return normalizedStatus === 'APPROVED' || normalizedStatus === 'COMPLETED';
    }

    private isRejectedStatus(status: string | undefined): boolean {
        return String(status || '').trim().toUpperCase() === 'REJECTED';
    }

    private isDraftStatus(status: string | undefined): boolean {
        return String(status || '').trim().toUpperCase() === 'DRAFT';
    }

    private buildDistinctOptions(values: string[]): string[] {
        return Array.from(new Set(
            values.map((value) => String(value || '').trim()).filter((value) => !!value)
        )).sort((left, right) => left.localeCompare(right));
    }

    private toShallowApprovedApplication(application: any): TemplateApprovedApplication {
        return {
            serApplicationId: application?.serApplicationId,
            serFormId: application?.serFormId,
            txtFormCode: application?.txtFormCode,
            txtStatus: application?.txtStatus,
            intCurrentApprovalLevel: application?.intCurrentApprovalLevel,
            dteCreatedDate: application?.dteCreatedDate,
            myApprovalDate: application?.myApprovalDate,
            templateName: application?.templateName || 'Template',
            txtApplicationData: application?.txtApplicationData,
            applicationTitle: resolveApplicationTitle(application)
        };
    }

    private getCsvCellValue(application: TemplateApprovedApplication, key: ApprovedApplicationCsvColumnKey): string {
        switch (key) {
            case 'txtFormCode': return application.txtFormCode || '';
            case 'applicationTitle': return application.applicationTitle || '';
            case 'templateName': return application.templateName || '';
            case 'txtStatus': return application.txtStatus || '';
            case 'myApprovalDate': return application.myApprovalDate ? new Date(application.myApprovalDate).toLocaleString() : '';
            case 'dteCreatedDate': return application.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleString() : '';
            default: return '';
        }
    }

    private escapeCsvValue(value: any): string {
        const normalized = String(value ?? '');
        return `"${normalized.replace(/"/g, '""')}"`;
    }

    private downloadBlob(blob: Blob, filename: string): void {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(objectUrl);
    }
}
