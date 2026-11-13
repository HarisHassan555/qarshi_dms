import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { DepartmentService } from 'src/app/services/department/department.service';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';
import { resolveApplicationTitle } from 'src/app/utils/application-title.util';
import { urls } from 'src/app/utils/urls';

interface AllApplicationScopeRow {
    serPermissionId?: number;
    serDepartmentId?: number | null;
    serSubmitterUserId?: number | null;
    serApproverUserId?: number | null;
    serFormId?: number | null;
    serApplicationId?: number | null;
    blIsEnabled: boolean;
}

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

    /** Role-based data scope (all_application_page_permission); admin only. */
    scopePanelExpanded = false;
    scopeRoles: any[] = [];
    scopeSelectedRoleId: number | null = null;
    scopeViewerUserId: number | null = null;
    scopeRows: AllApplicationScopeRow[] = [];
    scopePermissionLoading = false;
    scopeSaving = false;
    scopeMasterDepartments: any[] = [];
    scopeMasterTemplates: Array<{ serFormId: number; templateName: string }> = [];
    scopeMasterUsers: any[] = [];
    scopeMasterDataLoaded = false;

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
        private router: Router,
        private userService: UserService,
        private departmentService: DepartmentService,
        private http: HttpClient
    ) {}

    ngOnInit(): void {
        try {
            this.currentUser = JSON.parse(localStorage.getItem('user') || 'null');
        } catch {
            this.currentUser = null;
        }
        if (this.isAdminUser()) {
            this.loadFilterOptions();
        }
        this.loadApplications();
        this.scopeSelectedRoleId = this.getRoleId() || null;
    }

    isAdminUser(): boolean {
        const roleName = (
            this.currentUser?.cfgTblRole?.txtRoleName ||
            this.currentUser?.txtrole ||
            this.currentUser?.roleName ||
            ''
        )
            .toString()
            .trim()
            .toUpperCase();
        return (
            roleName === 'ADMIN' ||
            roleName === 'ROLE_ADMIN' ||
            roleName === 'SUPER ADMIN' ||
            roleName === 'ROLE_SUPER ADMIN' ||
            roleName.includes('ADMIN')
        );
    }

    private getRoleId(): number {
        return Number(
            this.currentUser?.cfgTblRole?.serRoleId ||
            this.currentUser?.serRoleId ||
            0
        );
    }

    toggleScopeConfigurator(): void {
        this.scopePanelExpanded = !this.scopePanelExpanded;
        if (this.scopePanelExpanded) {
            this.ensureScopeMasterData();
            if (!this.isAdminUser()) {
                this.scopeSelectedRoleId = this.getRoleId() || null;
                this.scopeViewerUserId = null;
            } else if (!this.scopeSelectedRoleId) {
                this.scopeSelectedRoleId = this.getRoleId() || null;
            }
            if (this.scopeSelectedRoleId) {
                this.loadScopePermissions();
            }
        }
    }

    private ensureScopeMasterData(): void {
        if (this.scopeMasterDataLoaded) {
            return;
        }
        this.scopeMasterDataLoaded = true;
        this.userService.getRoles().subscribe({
            next: (roles: any) => {
                this.scopeRoles = Array.isArray(roles) ? roles : [];
            },
            error: () => {
                this.scopeRoles = [];
            },
        });
        this.departmentService.getAll().subscribe({
            next: (rows: any) => {
                this.scopeMasterDepartments = Array.isArray(rows) ? rows : [];
            },
            error: () => {
                this.scopeMasterDepartments = [];
            },
        });
        const userId = this.getUserId();
        const templateUrl =
            urls.API_URL + 'getAllTemplateDefinitions' + (userId ? `?userId=${encodeURIComponent(String(userId))}` : '');
        this.http.get<any[]>(templateUrl).subscribe({
            next: (definitions) => {
                this.scopeMasterTemplates = (Array.isArray(definitions) ? definitions : [])
                    .map((d) => ({
                        serFormId: Number(d?.serFormId || 0),
                        templateName: String(d?.txtTemplateName || d?.templateName || `Form #${d?.serFormId || ''}`).trim(),
                    }))
                    .filter((t) => t.serFormId > 0)
                    .sort((a, b) => a.templateName.localeCompare(b.templateName));
            },
            error: () => {
                this.scopeMasterTemplates = [];
            },
        });
        this.userService.getUsers().subscribe({
            next: (users: any) => {
                this.scopeMasterUsers = Array.isArray(users) ? users : [];
            },
            error: () => {
                this.scopeMasterUsers = [];
            },
        });
    }

    onScopeRoleChanged(): void {
        this.loadScopePermissions();
    }

    onScopeViewerUserChanged(): void {
        this.loadScopePermissions();
    }

    loadScopePermissions(): void {
        const roleId = Number(this.scopeSelectedRoleId);
        if (!roleId) {
            this.scopeRows = [];
            return;
        }
        this.scopePermissionLoading = true;
        this.templateWorkflowService
            .getAllApplicationPagePermissions(roleId, this.scopeViewerUserId || null)
            .pipe(finalize(() => (this.scopePermissionLoading = false)))
            .subscribe({
                next: (response: any) => {
                    const items = Array.isArray(response?.items) ? response.items : [];
                    this.scopeRows = items.map((row: any) => this.mapScopeRowFromApi(row));
                },
                error: (error) => {
                    this.scopeRows = [];
                    this.notificationService.showMessage(
                        error.error?.message || 'Could not load role data scope.',
                        'danger'
                    );
                },
            });
    }

    private mapScopeRowFromApi(row: any): AllApplicationScopeRow {
        return {
            serPermissionId: row?.serPermissionId ? Number(row.serPermissionId) : undefined,
            serDepartmentId: this.positiveOrNull(row?.serDepartmentId),
            serSubmitterUserId: this.positiveOrNull(row?.serSubmitterUserId),
            serApproverUserId: this.positiveOrNull(row?.serApproverUserId),
            serFormId: this.positiveOrNull(row?.serFormId),
            serApplicationId: this.positiveOrNull(row?.serApplicationId),
            blIsEnabled: row?.blIsEnabled !== false && row?.blnStatus !== false,
        };
    }

    private positiveOrNull(value: unknown): number | null {
        const n = Number(value);
        return Number.isFinite(n) && n > 0 ? n : null;
    }

    addScopeRow(): void {
        this.scopeRows = [
            ...this.scopeRows,
            {
                blIsEnabled: true,
                serDepartmentId: null,
                serSubmitterUserId: null,
                serApproverUserId: null,
                serFormId: null,
                serApplicationId: null,
            },
        ];
    }

    removeScopeRow(index: number): void {
        this.scopeRows = this.scopeRows.filter((_, i) => i !== index);
    }

    clearScopeRows(): void {
        this.scopeRows = [];
    }

    hasScopeRowFilters(row: AllApplicationScopeRow): boolean {
        return !!(
            row.serDepartmentId ||
            row.serSubmitterUserId ||
            row.serApproverUserId ||
            row.serFormId ||
            row.serApplicationId
        );
    }

    saveScopePermissions(): void {
        const roleId = Number(this.scopeSelectedRoleId);
        if (!roleId) {
            this.notificationService.showMessage('Select a role first.', 'warning');
            return;
        }
        const incomplete = this.scopeRows.filter((row) => row.blIsEnabled && !this.hasScopeRowFilters(row));
        if (incomplete.length > 0) {
            this.notificationService.showMessage(
                'Each enabled rule needs at least one filter (form, department, user, or application id). Leave “Any” or remove the row.',
                'warning'
            );
            return;
        }
        const items = this.scopeRows
            .map((row) => ({
                serPermissionId: row.serPermissionId,
                serUserId: this.scopeViewerUserId || null,
                serDepartmentId: row.serDepartmentId,
                serSubmitterUserId: row.serSubmitterUserId,
                serApproverUserId: row.serApproverUserId,
                serFormId: row.serFormId,
                serApplicationId: row.serApplicationId,
                blIsEnabled: row.blIsEnabled,
                blnStatus: row.blIsEnabled,
            }))
            .filter(
                (row) =>
                    !!row.serDepartmentId ||
                    !!row.serSubmitterUserId ||
                    !!row.serApproverUserId ||
                    !!row.serFormId ||
                    !!row.serApplicationId
            );

        this.scopeSaving = true;
        this.templateWorkflowService
            .saveAllApplicationPagePermissions({
                serRoleId: roleId,
                serUserId: this.scopeViewerUserId || null,
                replaceForRole: true,
                items,
            })
            .pipe(finalize(() => (this.scopeSaving = false)))
            .subscribe({
                next: () => {
                    this.notificationService.showMessage('Role data scope saved.', 'success');
                    this.loadScopePermissions();
                    this.loadFilterOptions();
                    this.loadApplications();
                },
                error: (error) => {
                    this.notificationService.showMessage(
                        error.error?.message || 'Could not save role data scope.',
                        'danger'
                    );
                },
            });
    }

    getScopeRoleLabel(role: any): string {
        return String(role?.txtRoleName || role?.roleName || role?.name || `Role #${role?.serRoleId || ''}`).trim();
    }

    getScopeUserLabel(user: any): string {
        return String(user?.txtUserName || user?.userName || user?.name || `User #${user?.serUserId || ''}`).trim();
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
