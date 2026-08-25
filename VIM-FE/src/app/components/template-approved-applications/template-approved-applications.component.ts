import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';

interface TemplateApprovedApplication {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    dteCreatedDate?: string;
    myApprovalDate?: string;
    templateName?: string;
}

type ApprovedApplicationCsvColumnKey = 'txtFormCode' | 'templateName' | 'txtStatus' | 'myApprovalDate' | 'dteCreatedDate';
interface ApprovedApplicationCsvColumn { key: ApprovedApplicationCsvColumnKey; label: string; }

@Component({
    selector: 'app-template-approved-applications',
    templateUrl: './template-approved-applications.component.html',
    styleUrls: ['./template-approved-applications.component.css']
})
export class TemplateApprovedApplicationsComponent implements OnInit, OnDestroy {
    search = '';
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

    cols = [
        { field: 'txtFormCode', title: 'Application Code' },
        { field: 'templateName', title: 'Template' },
        { field: 'txtStatus', title: 'Current Status' },
        { field: 'myApprovalDate', title: 'Approved On' },
        { field: 'dteCreatedDate', title: 'Submitted Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];
    readonly csvColumns: ApprovedApplicationCsvColumn[] = [
        { key: 'txtFormCode', label: 'Application Code' },
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
        this.loadApprovedApplications();
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
    }

    loadApprovedApplications(): void {
        this.isLoading = true;

        this.templateWorkflowService.getTemplateApprovedApplications(
            this.currentUser?.serUserId || this.currentUser?.userId || this.currentUser?.id || 0,
            this.page,
            this.pageSize,
            this.search
        ).pipe(finalize(() => this.isLoading = false)).subscribe({
            next: (response: any) => {
                this.approvedApplications = (Array.isArray(response?.items) ? response.items : [])
                    .map((application: any) => this.toShallowApprovedApplication(application));
                this.totalApprovedApplications = Number(response?.total || 0);
            },
            error: (error) => {
                this.approvedApplications = [];
                this.totalApprovedApplications = 0;
                this.notificationService.showMessage(
                    'Error loading template approved applications: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
    }

    onSearchChanged(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => {
            this.page = 0;
            this.loadApprovedApplications();
        }, 300);
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.loadApprovedApplications();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10;
        this.page = 0;
        this.loadApprovedApplications();
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
        const userId = this.currentUser?.serUserId || this.currentUser?.userId || this.currentUser?.id || 0;
        const exportSize = Math.max(this.totalApprovedApplications || 0, this.pageSize, 1000);
        this.templateWorkflowService.getTemplateApprovedApplications(userId, 0, exportSize, this.search).subscribe({
            next: (response: any) => {
                const rows = (Array.isArray(response?.items) ? response.items : [])
                    .map((application: any) => this.toShallowApprovedApplication(application));
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
            },
            error: (error) => {
                this.notificationService.showMessage(
                    'Approved applications CSV could not be exported: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
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
            case 'PENDING':
                return 'badge-outline-warning';
            case 'APPROVED':
            case 'COMPLETED':
                return 'badge-outline-success';
            case 'REJECTED':
                return 'badge-outline-danger';
            case 'IN_PROGRESS':
            case 'OPINION_PENDING':
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

    private toShallowApprovedApplication(application: any): TemplateApprovedApplication {
        return {
            serApplicationId: application?.serApplicationId,
            serFormId: application?.serFormId,
            txtFormCode: application?.txtFormCode,
            txtStatus: application?.txtStatus,
            intCurrentApprovalLevel: application?.intCurrentApprovalLevel,
            dteCreatedDate: application?.dteCreatedDate,
            myApprovalDate: application?.myApprovalDate,
            templateName: application?.templateName || 'Template'
        };
    }

    private getCsvCellValue(application: TemplateApprovedApplication, key: ApprovedApplicationCsvColumnKey): string {
        switch (key) {
            case 'txtFormCode': return application.txtFormCode || '';
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
