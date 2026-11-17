import { HttpClient } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { DepartmentService } from 'src/app/services/department/department.service';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';
import { urls } from 'src/app/utils/urls';

export interface AllApplicationScopeRule {
    serRoleId: number;
    serPermissionIds: number[];
    serFormIds: number[];
    serDepartmentIds: number[];
    serSubmitterUserIds: number[];
    serApproverUserIds: number[];
    serApplicationIds: number[];
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
    scopeRules: AllApplicationScopeRule[] = [];
    scopePermissionLoading = false;
    scopeSaving = false;
    scopeMasterDepartments: any[] = [];
    scopeMasterTemplates: Array<{ serFormId: number; templateName: string }> = [];
    scopeMasterUsers: any[] = [];
    scopeMasterDataLoaded = false;
    scopeModalReferenceLoading = false;
    showRuleModal = false;
    ruleModalIndex: number | null = null;
    ruleDraft: AllApplicationScopeRule | null = null;

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
        if (!this.availableRoles.length) {
            this.notificationService.showMessage('Every role already has a rule. Edit that rule to change its values.', 'warning');
            return;
        }
        this.ruleModalIndex = null;
        this.ruleDraft = this.createEmptyScopeRule();
        this.showRuleModal = true;
        this.ensureScopeModalReferenceData();
    }

    get availableRoles(): any[] {
        const used = new Set(this.scopeRules.map((rule) => Number(rule.serRoleId)));
        return this.scopeRoles.filter((role) => !used.has(Number(role?.serRoleId || 0)));
    }

    get filteredScopeRules(): AllApplicationScopeRule[] {
        const query = this.searchText.trim().toLowerCase();
        if (!query) {
            return this.scopeRules;
        }
        return this.scopeRules.filter((rule) => {
            const haystack = [
                this.getRoleLabel(rule.serRoleId),
                rule.blIsEnabled ? 'active' : 'inactive',
                this.joinLabels(rule.serFormIds, (id) => this.getTemplateLabel(id), 'Any form'),
                this.joinLabels(rule.serDepartmentIds, (id) => this.getDepartmentLabel(id), 'Any department'),
                this.joinLabels(rule.serSubmitterUserIds, (id) => this.getUserLabel(id), 'Any user'),
                this.joinLabels(rule.serApproverUserIds, (id) => this.getUserLabel(id), 'Any user'),
                this.joinLabels(rule.serApplicationIds, (id) => String(id), '—'),
            ]
                .join(' ')
                .toLowerCase();
            return haystack.includes(query);
        });
    }

    openEditRuleModal(rule: AllApplicationScopeRule): void {
        const index = this.scopeRules.indexOf(rule);
        if (index < 0) {
            return;
        }
        this.ruleModalIndex = index;
        this.ruleDraft = {
            ...rule,
            serPermissionIds: [...rule.serPermissionIds],
            serFormIds: [...rule.serFormIds],
            serDepartmentIds: [...rule.serDepartmentIds],
            serSubmitterUserIds: [...rule.serSubmitterUserIds],
            serApproverUserIds: [...rule.serApproverUserIds],
            serApplicationIds: [...rule.serApplicationIds],
        };
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
        if (this.ruleModalIndex === null && this.scopeRules.some((rule) => Number(rule.serRoleId) === roleId)) {
            this.notificationService.showMessage('This role already has a rule. Edit that rule instead.', 'warning');
            return;
        }
        if (this.ruleDraft.blIsEnabled && !this.hasScopeFilters(this.ruleDraft)) {
            this.notificationService.showMessage(
                'Choose at least one filter (form, department, user, or application id).',
                'warning'
            );
            return;
        }
        const savedDraft = this.cloneRule({ ...this.ruleDraft, serRoleId: roleId });
        const isNew = this.ruleModalIndex === null;
        this.closeRuleModal();
        this.persistRoleRule(savedDraft, isNew ? 'Rule added.' : 'Rule updated.');
    }

    addApplicationIdTag = (term: string): number | null => {
        const id = Number(String(term || '').trim());
        if (!Number.isInteger(id) || id <= 0) {
            return null;
        }
        if (this.ruleDraft?.serApplicationIds.includes(id)) {
            return null;
        }
        return id;
    };

    private createEmptyScopeRule(): AllApplicationScopeRule {
        const preferred = Number(this.getRoleId() || 0);
        const availableIds = this.availableRoles.map((role) => Number(role?.serRoleId || 0));
        const serRoleId = availableIds.includes(preferred) ? preferred : Number(availableIds[0] || 0);
        return {
            serRoleId,
            serPermissionIds: [],
            serFormIds: [],
            serDepartmentIds: [],
            serSubmitterUserIds: [],
            serApproverUserIds: [],
            serApplicationIds: [],
            blIsEnabled: true,
        };
    }

    private cloneRule(rule: AllApplicationScopeRule): AllApplicationScopeRule {
        return {
            serRoleId: Number(rule.serRoleId || 0),
            serPermissionIds: this.uniquePositiveIds(rule.serPermissionIds),
            serFormIds: this.uniquePositiveIds(rule.serFormIds),
            serDepartmentIds: this.uniquePositiveIds(rule.serDepartmentIds),
            serSubmitterUserIds: this.uniquePositiveIds(rule.serSubmitterUserIds),
            serApproverUserIds: this.uniquePositiveIds(rule.serApproverUserIds),
            serApplicationIds: this.uniquePositiveIds(rule.serApplicationIds),
            blIsEnabled: rule.blIsEnabled !== false,
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

    joinLabels(ids: number[] | null | undefined, labelFor: (id: number) => string, emptyLabel: string): string {
        const values = this.uniquePositiveIds(ids).map((id) => labelFor(id));
        return values.length ? values.join(', ') : emptyLabel;
    }

    formatForms(rule: AllApplicationScopeRule): string {
        return this.joinLabels(rule.serFormIds, (id) => this.getTemplateLabel(id), 'Any form');
    }

    formatDepartments(rule: AllApplicationScopeRule): string {
        return this.joinLabels(rule.serDepartmentIds, (id) => this.getDepartmentLabel(id), 'Any department');
    }

    formatSubmitters(rule: AllApplicationScopeRule): string {
        return this.joinLabels(rule.serSubmitterUserIds, (id) => this.getUserLabel(id), 'Any user');
    }

    formatApprovers(rule: AllApplicationScopeRule): string {
        return this.joinLabels(rule.serApproverUserIds, (id) => this.getUserLabel(id), 'Any user');
    }

    formatApplicationIds(rule: AllApplicationScopeRule): string {
        return this.joinLabels(rule.serApplicationIds, (id) => String(id), '—');
    }

    getRoleLabel(serRoleId: number | null | undefined): string {
        const roleId = Number(serRoleId || 0);
        if (!roleId) {
            return '—';
        }
        const match = this.scopeRoles.find((r) => Number(r?.serRoleId) === roleId);
        return String(match?.txtRoleName || match?.roleName || `Role #${roleId}`).trim();
    }

    isRuleRowIncomplete(rule: AllApplicationScopeRule): boolean {
        return rule.blIsEnabled && !this.hasScopeFilters(rule);
    }

    hasIncompleteRules(): boolean {
        return this.scopeRules.some((rule) => this.isRuleRowIncomplete(rule));
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
        for (const rule of this.scopeRules) {
            for (const id of [...rule.serSubmitterUserIds, ...rule.serApproverUserIds]) {
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
                    this.scopeRules = this.groupScopeRules(items.map((row: any) => this.mapScopeRowFromApi(row)));
                    this.ensureTableReferenceLabels();
                    if (this.scopeMasterDataLoaded) {
                        this.mergeUsersFromScopeRows();
                    }
                },
                error: (error) => {
                    this.scopeRules = [];
                    this.notificationService.showMessage(
                        error.error?.message || 'Could not load application scope rules.',
                        'danger'
                    );
                },
            });
    }

    private mapScopeRowFromApi(row: any): {
        serPermissionId?: number;
        serRoleId: number;
        serDepartmentId: number | null;
        serSubmitterUserId: number | null;
        serApproverUserId: number | null;
        serFormId: number | null;
        serApplicationId: number | null;
        blIsEnabled: boolean;
    } {
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

    private groupScopeRules(rows: ReturnType<RoleApplicationManagementComponent['mapScopeRowFromApi']>[]): AllApplicationScopeRule[] {
        const byRole = new Map<number, AllApplicationScopeRule>();
        for (const row of rows) {
            const roleId = Number(row.serRoleId || 0);
            if (!roleId) {
                continue;
            }
            let rule = byRole.get(roleId);
            if (!rule) {
                rule = {
                    serRoleId: roleId,
                    serPermissionIds: [],
                    serFormIds: [],
                    serDepartmentIds: [],
                    serSubmitterUserIds: [],
                    serApproverUserIds: [],
                    serApplicationIds: [],
                    blIsEnabled: false,
                };
                byRole.set(roleId, rule);
            }
            if (row.serPermissionId) {
                rule.serPermissionIds.push(row.serPermissionId);
            }
            this.pushId(rule.serFormIds, row.serFormId);
            this.pushId(rule.serDepartmentIds, row.serDepartmentId);
            this.pushId(rule.serSubmitterUserIds, row.serSubmitterUserId);
            this.pushId(rule.serApproverUserIds, row.serApproverUserId);
            this.pushId(rule.serApplicationIds, row.serApplicationId);
            if (row.blIsEnabled) {
                rule.blIsEnabled = true;
            }
        }
        return Array.from(byRole.values());
    }

    private pushId(target: number[], value: number | null): void {
        const id = Number(value || 0);
        if (id > 0 && !target.includes(id)) {
            target.push(id);
        }
    }

    private uniquePositiveIds(values: number[] | null | undefined): number[] {
        const ids: number[] = [];
        for (const value of values || []) {
            this.pushId(ids, Number(value));
        }
        return ids;
    }

    private positiveOrNull(value: unknown): number | null {
        const n = Number(value);
        return Number.isFinite(n) && n > 0 ? n : null;
    }

    deleteScopeRule(rule: AllApplicationScopeRule): void {
        if (!rule || this.scopeSaving) {
            return;
        }
        const roleId = Number(rule.serRoleId || 0);
        if (!roleId) {
            return;
        }
        const confirmed = window.confirm(`Delete the rule for ${this.getRoleLabel(roleId)}? This cannot be undone.`);
        if (!confirmed) {
            return;
        }
        this.persistRoleRule(
            {
                serRoleId: roleId,
                serPermissionIds: [],
                serFormIds: [],
                serDepartmentIds: [],
                serSubmitterUserIds: [],
                serApproverUserIds: [],
                serApplicationIds: [],
                blIsEnabled: false,
            },
            'Rule deleted.',
            true
        );
    }

    hasScopeFilters(rule: AllApplicationScopeRule): boolean {
        return !!(
            rule.serFormIds.length ||
            rule.serDepartmentIds.length ||
            rule.serSubmitterUserIds.length ||
            rule.serApproverUserIds.length ||
            rule.serApplicationIds.length
        );
    }

    /** Replaces every stored value for this role with the current multi-selects. */
    private persistRoleRule(rule: AllApplicationScopeRule, successMessage = 'Rule saved.', clearing = false): void {
        const roleId = Number(rule.serRoleId || 0);
        if (!roleId) {
            this.notificationService.showMessage('Select a role first.', 'warning');
            return;
        }
        const enabled = rule.blIsEnabled !== false;
        const base = {
            serUserId: null,
            blIsEnabled: enabled,
            blnStatus: enabled,
        };
        const items = clearing
            ? []
            : [
                ...rule.serFormIds.map((serFormId) => ({ ...base, serFormId })),
                ...rule.serDepartmentIds.map((serDepartmentId) => ({ ...base, serDepartmentId })),
                ...rule.serSubmitterUserIds.map((serSubmitterUserId) => ({ ...base, serSubmitterUserId })),
                ...rule.serApproverUserIds.map((serApproverUserId) => ({ ...base, serApproverUserId })),
                ...rule.serApplicationIds.map((serApplicationId) => ({ ...base, serApplicationId })),
            ];
        this.scopeSaving = true;
        this.templateWorkflowService
            .saveAllApplicationPagePermissions({
                serRoleId: roleId,
                serUserId: null,
                replaceForRole: true,
                items,
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