import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { resolveApplicationTitle } from 'src/app/utils/application-title.util';

interface AuditApplicationRow {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    dteCreatedDate?: string;
    templateName?: string;
    submittedByUserName?: string;
    submittedDepartmentName?: string;
    applicationTitle?: string;
}

interface FilterDepartment {
    serDepartmentId: number;
    txtDepartmentName: string;
}

interface FilterTemplate {
    serFormId: number;
    templateName: string;
}

interface FilterSubmitter {
    serUserId: number;
    txtUserName: string;
}

@Component({
    selector: 'app-all-applications',
    templateUrl: './all-applications.component.html',
    styleUrls: ['./all-applications.component.css'],
})
export class AllApplicationsComponent implements OnInit, OnDestroy {
    search = '';
    selectedDepartmentIds: number[] = [];
    selectedSerFormIds: number[] = [];
    selectedStatuses: string[] = [];
    selectedSubmittedByUserIds: number[] = [];
    dateFrom = '';
    dateTo = '';

    applications: AuditApplicationRow[] = [];
    departments: FilterDepartment[] = [];
    templates: FilterTemplate[] = [];
    statuses: string[] = [];
    submitters: FilterSubmitter[] = [];

    currentUser: any = null;
    isLoading = false;
    isFilterOptionsLoading = false;
    openingApplicationId: number | null = null;

    page = 0;
    pageSize = 10;
    totalApplications = 0;
    private searchTimer: ReturnType<typeof setTimeout> | null = null;

    cols = [
        { field: 'txtFormCode', title: 'Application Code' },
        { field: 'applicationTitle', title: 'Title' },
        { field: 'templateName', title: 'Form / Template' },
        { field: 'submittedByUserName', title: 'Submitted By' },
        { field: 'submittedDepartmentName', title: 'Department' },
        { field: 'txtStatus', title: 'Status' },
        { field: 'dteCreatedDate', title: 'Submitted Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService,
        private router: Router
    ) {}

    ngOnInit(): void {
        try {
            this.currentUser = JSON.parse(localStorage.getItem('user') || 'null');
        } catch {
            this.currentUser = null;
        }
        this.loadFilterOptions();
        this.loadApplications();
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
    }

    private getUserId(): number {
        return Number(
            this.currentUser?.serUserId ||
            this.currentUser?.userId ||
            this.currentUser?.id ||
            0
        );
    }

    loadFilterOptions(): void {
        const userId = this.getUserId();
        if (!userId) {
            return;
        }
        this.isFilterOptionsLoading = true;
        this.templateWorkflowService.getAuditAllApplicationsFilterOptions(userId)
            .pipe(finalize(() => (this.isFilterOptionsLoading = false)))
            .subscribe({
                next: (response: any) => {
                    this.departments = Array.isArray(response?.departments) ? response.departments : [];
                    this.templates = Array.isArray(response?.templates) ? response.templates : [];
                    this.statuses = Array.isArray(response?.statuses) ? response.statuses : [];
                    this.submitters = Array.isArray(response?.submitters) ? response.submitters : [];
                },
                error: () => {
                    this.departments = [];
                    this.templates = [];
                    this.statuses = [];
                    this.submitters = [];
                },
            });
    }

    loadApplications(): void {
        const userId = this.getUserId();
        if (!userId) {
            this.notificationService.showMessage('Current user could not be resolved.', 'danger');
            return;
        }

        this.isLoading = true;
        this.templateWorkflowService.getAuditAllTemplateApplications(userId, {
            page: this.page,
            pageSize: this.pageSize,
            search: this.search,
            departmentIds: this.selectedDepartmentIds,
            serFormIds: this.selectedSerFormIds,
            statuses: this.selectedStatuses,
            submittedByUserIds: this.selectedSubmittedByUserIds,
            dateFrom: this.dateFrom,
            dateTo: this.dateTo,
        }).pipe(finalize(() => (this.isLoading = false))).subscribe({
            next: (response: any) => {
                this.applications = (Array.isArray(response?.items) ? response.items : []).map((row: any) => ({
                    ...row,
                    applicationTitle: resolveApplicationTitle(row),
                }));
                this.totalApplications = Number(response?.total || 0);
            },
            error: (error) => {
                this.applications = [];
                this.totalApplications = 0;
                this.notificationService.showMessage(
                    'Could not load applications: ' + (error.error?.message || error.message),
                    'danger'
                );
            },
        });
    }

    onSearchChanged(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => {
            this.page = 0;
            this.loadApplications();
        }, 300);
    }

    onFiltersChanged(): void {
        this.page = 0;
        this.loadApplications();
    }

    onDateFilterChanged(): void {
        this.onFiltersChanged();
    }

    clearFilters(): void {
        this.search = '';
        this.selectedDepartmentIds = [];
        this.selectedSerFormIds = [];
        this.selectedStatuses = [];
        this.selectedSubmittedByUserIds = [];
        this.dateFrom = '';
        this.dateTo = '';
        this.page = 0;
        this.loadApplications();
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.loadApplications();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10;
        this.page = 0;
        this.loadApplications();
    }

    get totalPages(): number {
        return Math.max(1, Math.ceil(this.totalApplications / this.pageSize));
    }

    get pageStart(): number {
        return this.totalApplications === 0 ? 0 : this.page * this.pageSize + 1;
    }

    get pageEnd(): number {
        return Math.min(this.totalApplications, (this.page + 1) * this.pageSize);
    }

    isOpeningApplication(application: AuditApplicationRow): boolean {
        return !!application.serApplicationId && this.openingApplicationId === application.serApplicationId;
    }

    viewApplication(application: AuditApplicationRow): void {
        if (!application.serApplicationId) {
            this.notificationService.showMessage('Invalid application ID', 'danger');
            return;
        }
        this.openingApplicationId = application.serApplicationId;
        this.router.navigate(['/my-application', application.serApplicationId], {
            queryParams: { from: 'all-applications' },
        }).then((opened) => {
            if (!opened) {
                this.openingApplicationId = null;
            }
        }).catch(() => {
            this.openingApplicationId = null;
        });
    }

    getStatusBadgeClass(status?: string): string {
        const normalized = String(status || '').toUpperCase();
        if (normalized === 'APPROVED' || normalized === 'COMPLETED') {
            return 'badge-outline-success';
        }
        if (normalized === 'REJECTED') {
            return 'badge-outline-danger';
        }
        if (normalized === 'DRAFT') {
            return 'badge-outline-secondary';
        }
        if (normalized === 'HOLD' || normalized === 'ON_HOLD') {
            return 'badge-outline-warning';
        }
        return 'badge-outline-primary';
    }
}
