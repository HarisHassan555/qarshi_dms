import { HttpClient } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { DepartmentService } from 'src/app/services/department/department.service';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';
import { urls } from 'src/app/utils/urls';

export interface AllApplicationScopeRow {
    serPermissionId?: number;
    serRoleId: number;
    serDepartmentId?: number | null;
    serSubmitterUserId?: number | null;
    serApproverUserId?: number | null;
    serFormId?: number | null;
    serApplicationId?: number | null;
    blIsEnabled: boolean;
}

@Component({
    selector: 'app-role-application-management',
    templateUrl: './role-application-management.component.html',
    styleUrls: ['./role-application-management.component.css'],
})
export class RoleApplicationManagementComponent implements OnInit {
    @Input()
    set initialRoles(value: Array<{ serRoleId: number; txtRoleName: string }> | null | undefined) {
        if (!Array.isArray(value) || !value.length) {
            return;
        }
        this.scopeRoles = value
            .map((role) => ({
                serRoleId: Number(role?.serRoleId || 0),
                txtRoleName: String(role?.txtRoleName || '').trim(),
            }))
            .filter((role) => role.serRoleId > 0);
    }

    scopeRoles: any[] = [];
    searchText = '';
    scopeRows: AllApplicationScopeRow[] = [];
    scopePermissionLoading = false;
    scopeSaving = false;
    scopeMasterDepartments: any[] = [];
    scopeMasterTemplates: Array<{ serFormId: number; templateName: string }> = [];
    scopeMasterUsers: any[] = [];
    scopeMasterDataLoaded = false;
    scopeModalReferenceLoading = false;
    showRuleModal = false;
    ruleModalIndex: number | null = null;
    ruleDraft: AllApplicationScopeRow | null = null;

    readonly ruleModalAppendTo = '#role-app-rule-modal';

    private currentUser: any = null;

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService,
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
        if (!this.isAdminUser()) {
            return;
        }
        this.bootstrapListing();
    }

    private bootstrapListing(): void {
        const continueLoad = () => {
            this.ensureTableReferenceLabels();
            this.loadScopePermissions();
        };
        if (this.scopeRoles.length > 0) {
            continueLoad();
            return;
        }
        this.scopePermissionLoading = true;
        this.userService
            .getRoles()
            .pipe(catchError(() => of([])))
            .subscribe((roles: any) => {
                this.scopeRoles = Array.isArray(roles) ? roles : [];
                continueLoad();
            });
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

    private getUserId(): number {
        return Number(
            this.currentUser?.serUserId ||
            this.currentUser?.userId ||
            this.currentUser?.id ||
            0
        );
    }

    /** Called from parent header “Add” button. */
    addScopeRule(): void {
        this.openNewRuleModal();
    }

    openNewRuleModal(): void {
        this.ruleModalIndex = null;
        this.ruleDraft = this.createEmptyScopeRow();
        this.showRuleModal = true;
        this.ensureScopeModalReferenceData();
    }

    get filteredScopeRows(): AllApplicationScopeRow[] {
        const query = this.searchText.trim().toLowerCase();
        if (!query) {
            return this.scopeRows;
        }
        return this.scopeRows.filter((row) => {
            const haystack = [
                this.getRoleLabel(row.serRoleId),
                row.blIsEnabled ? 'active' : 'inactive',
                this.getTemplateLabel(row.serFormId),
                this.getDepartmentLabel(row.serDepartmentId),
                this.getUserLabel(row.serSubmitterUserId),
                this.getUserLabel(row.serApproverUserId),
                this.getApplicationIdLabel(row.serApplicationId),
            ]
                .join(' ')
                .toLowerCase();
            return haystack.includes(query);
        });
    }

    openEditRuleModal(row: AllApplicationScopeRow): void {
        const index = this.scopeRows.indexOf(row);
        if (index < 0) {
            return;
        }
        this.ruleModalIndex = index;
        this.ruleDraft = { ...row };
        this.showRuleModal = true;
        this.ensureScopeModalReferenceData();
    }

    closeRuleModal(): void {
        this.showRuleModal = false;
        this.ruleModalIndex = null;
        this.ruleDraft = null;
    }

    confirmRuleModal(): void {
        if (!this.ruleDraft) {
            return;
        }
        const roleId = Number(this.ruleDraft.serRoleId || 0);
        if (!roleId) {
            this.notificationService.showMessage('Select a role for this rule.', 'warning');
            return;
        }
        if (this.ruleDraft.blIsEnabled && !this.hasScopeRowFilters(this.ruleDraft)) {
            this.notificationService.showMessage(
                'Choose at least one filter (form, department, user, or application id).',
                'warning'
            );
            return;
        }
        const savedDraft = { ...this.ruleDraft, serRoleId: roleId };
        const isNew = this.ruleModalIndex === null;
        this.closeRuleModal();
        this.persistSingleRule(savedDraft, isNew ? 'Rule added.' : 'Rule updated.');
    }

    private createEmptyScopeRow(): AllApplicationScopeRow {
        return {
            serRoleId: this.getRoleId() || Number(this.scopeRoles[0]?.serRoleId || 0),
            blIsEnabled: true,
            serDepartmentId: null,
            serSubmitterUserId: null,
            serApproverUserId: null,
            serFormId: null,
            serApplicationId: null,
        };
    }

    getTemplateLabel(serFormId: number | null | undefined): string {
        if (!serFormId) {
            return 'Any form';
        }
        const match = this.scopeMasterTemplates.find((t) => t.serFormId === serFormId);
        return match?.templateName || `Form #${serFormId}`;
    }

    getDepartmentLabel(serDepartmentId: number | null | undefined): string {
        if (!serDepartmentId) {
            return 'Any department';
        }
        const match = this.scopeMasterDepartments.find((d) => Number(d?.serDepartmentId) === serDepartmentId);
        return match?.txtDepartmentName || `Dept #${serDepartmentId}`;
    }

    getUserLabel(serUserId: number | null | undefined): string {
        if (!serUserId) {
            return 'Any user';
        }
        const match = this.scopeMasterUsers.find((u) => Number(u?.serUserId) === serUserId);
        return match?.txtUserName || `User #${serUserId}`;
    }

    getApplicationIdLabel(serApplicationId: number | null | undefined): string {
        return serApplicationId ? String(serApplicationId) : '—';
    }

    getRoleLabel(serRoleId: number | null | undefined): string {
        const roleId = Number(serRoleId || 0);
        if (!roleId) {
            return '—';
        }
        const match = this.scopeRoles.find((r) => Number(r?.serRoleId) === roleId);
        return String(match?.txtRoleName || match?.roleName || `Role #${roleId}`).trim();
    }

    isRuleRowIncomplete(row: AllApplicationScopeRow): boolean {
        return row.blIsEnabled && !this.hasScopeRowFilters(row);
    }

    hasIncompleteRules(): boolean {
        return this.scopeRows.some((row) => this.isRuleRowIncomplete(row));
    }

    /** Departments, templates, and users — loaded only when opening the rule modal (avoids heavy getAllUser on tab open). */
    private ensureScopeModalReferenceData(): void {
        if (this.scopeMasterDataLoaded || this.scopeModalReferenceLoading) {
            return;
        }
        this.scopeModalReferenceLoading = true;
        const userId = this.getUserId();
        const templateUrl =
            urls.API_URL + 'getAllTemplateDefinitions' + (userId ? `?userId=${encodeURIComponent(String(userId))}` : '');

        forkJoin({
            departments: this.departmentService.getAll().pipe(catchError(() => of([]))),
            templates: this.http.get<any[]>(templateUrl).pipe(catchError(() => of([]))),
            users: this.userService.getUsers().pipe(catchError(() => of([]))),
        })
            .pipe(
                finalize(() => {
                    this.scopeModalReferenceLoading = false;
                    this.scopeMasterDataLoaded = true;
                })
            )
            .subscribe(({ departments, templates, users }) => {
                this.scopeMasterDepartments = Array.isArray(departments) ? departments : [];
                this.scopeMasterTemplates = (Array.isArray(templates) ? templates : [])
                    .map((d) => ({
                        serFormId: Number(d?.serFormId || 0),
                        templateName: String(d?.txtTemplateName || d?.templateName || `Form #${d?.serFormId || ''}`).trim(),
                    }))
                    .filter((t) => t.serFormId > 0)
                    .sort((a, b) => a.templateName.localeCompare(b.templateName));
                this.scopeMasterUsers = this.mapSlimUsers(Array.isArray(users) ? users : []);
                this.mergeUsersFromScopeRows();
            });
    }

    private mapSlimUsers(users: any[]): Array<{ serUserId: number; txtUserName: string }> {
        return (Array.isArray(users) ? users : [])
            .map((u) => ({
                serUserId: Number(u?.serUserId || 0),
                txtUserName: String(u?.txtUserName || u?.userName || '').trim(),
            }))
            .filter((u) => u.serUserId > 0 && u.txtUserName)
            .sort((a, b) => a.txtUserName.localeCompare(b.txtUserName));
    }

    /** After rules load, prefetch only departments + templates for table labels (no full user list). */
    private ensureTableReferenceLabels(): void {
        if (this.scopeMasterDepartments.length > 0 && this.scopeMasterTemplates.length > 0) {
            return;
        }
        const userId = this.getUserId();
        const templateUrl =
            urls.API_URL + 'getAllTemplateDefinitions' + (userId ? `?userId=${encodeURIComponent(String(userId))}` : '');
        forkJoin({
            departments: this.departmentService.getAll().pipe(catchError(() => of([]))),
            templates: this.http.get<any[]>(templateUrl).pipe(catchError(() => of([]))),
        }).subscribe(({ departments, templates }) => {
            if (!this.scopeMasterDepartments.length) {
                this.scopeMasterDepartments = Array.isArray(departments) ? departments : [];
            }
            if (!this.scopeMasterTemplates.length) {
                this.scopeMasterTemplates = (Array.isArray(templates) ? templates : [])
                    .map((d) => ({
                        serFormId: Number(d?.serFormId || 0),
                        templateName: String(d?.txtTemplateName || d?.templateName || `Form #${d?.serFormId || ''}`).trim(),
                    }))
                    .filter((t) => t.serFormId > 0)
                    .sort((a, b) => a.templateName.localeCompare(b.templateName));
            }
        });
    }

    private mergeUsersFromScopeRows(): void {
        const knownIds = new Set(this.scopeMasterUsers.map((u) => u.serUserId));
        for (const row of this.scopeRows) {
            for (const id of [row.serSubmitterUserId, row.serApproverUserId]) {
                const userId = Number(id || 0);
                if (userId > 0 && !knownIds.has(userId)) {
                    this.scopeMasterUsers.push({ serUserId: userId, txtUserName: `User #${userId}` });
                    knownIds.add(userId);
                }
            }
        }
    }

    loadScopePermissions(): void {
        this.scopePermissionLoading = true;
        this.templateWorkflowService
            .getAllApplicationPagePermissions(null, null)
            .pipe(finalize(() => (this.scopePermissionLoading = false)))
            .subscribe({
                next: (response: any) => {
                    const items = Array.isArray(response?.items) ? response.items : [];
                    this.scopeRows = items.map((row: any) => this.mapScopeRowFromApi(row));
                    this.ensureTableReferenceLabels();
                    if (this.scopeMasterDataLoaded) {
                        this.mergeUsersFromScopeRows();
                    }
                },
                error: (error) => {
                    this.scopeRows = [];
                    this.notificationService.showMessage(
                        error.error?.message || 'Could not load application scope rules.',
                        'danger'
                    );
                },
            });
    }

    private mapScopeRowFromApi(row: any): AllApplicationScopeRow {
        return {
            serPermissionId: row?.serPermissionId ? Number(row.serPermissionId) : undefined,
            serRoleId: Number(row?.serRoleId || 0),
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

    deleteScopeRule(row: AllApplicationScopeRow): void {
        if (!row || this.scopeSaving) {
            return;
        }
        const roleId = Number(row.serRoleId || 0);
        if (!roleId) {
            return;
        }
        const permissionId = Number(row.serPermissionId || 0);
        if (!permissionId) {
            this.notificationService.showMessage('This rule has no id yet. Refresh the list and try again.', 'warning');
            return;
        }
        const confirmed = window.confirm('Delete this rule? This cannot be undone.');
        if (!confirmed) {
            return;
        }
        this.scopeSaving = true;
        this.templateWorkflowService
            .deleteAllApplicationPagePermission(permissionId)
            .pipe(finalize(() => (this.scopeSaving = false)))
            .subscribe({
                next: (response: any) => {
                    const status = String(response?.status || 'Success').toLowerCase();
                    if (status && status !== 'success') {
                        this.notificationService.showMessage(response?.message || 'Could not delete this rule.', 'danger');
                        return;
                    }
                    this.scopeRows = this.scopeRows.filter((item) => Number(item.serPermissionId) !== permissionId);
                    this.notificationService.showMessage('Rule deleted.', 'success');
                },
                error: (error) => {
                    this.notificationService.showMessage(
                        error.error?.message || 'Could not delete this rule.',
                        'danger'
                    );
                },
            });
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

    /** Inserts or updates one rule. Does not rewrite the other rules for that role. */
    private persistSingleRule(row: AllApplicationScopeRow, successMessage = 'Rule saved.'): void {
        const roleId = Number(row.serRoleId || 0);
        if (!roleId) {
            this.notificationService.showMessage('Select a role first.', 'warning');
            return;
        }
        this.scopeSaving = true;
        this.templateWorkflowService
            .saveAllApplicationPagePermissions({
                serRoleId: roleId,
                serUserId: null,
                replaceForRole: false,
                items: [
                    {
                        serPermissionId: row.serPermissionId,
                        serUserId: null,
                        serDepartmentId: row.serDepartmentId,
                        serSubmitterUserId: row.serSubmitterUserId,
                        serApproverUserId: row.serApproverUserId,
                        serFormId: row.serFormId,
                        serApplicationId: row.serApplicationId,
                        blIsEnabled: row.blIsEnabled,
                        blnStatus: row.blIsEnabled,
                    },
                ],
            })
            .pipe(finalize(() => (this.scopeSaving = false)))
            .subscribe({
                next: () => {
                    if (successMessage) {
                        this.notificationService.showMessage(successMessage, 'success');
                    }
                    this.loadScopePermissions();
                },
                error: (error) => {
                    this.notificationService.showMessage(
                        error.error?.message || 'Could not save application rules.',
                        'danger'
                    );
                    this.loadScopePermissions();
                },
            });
    }
}