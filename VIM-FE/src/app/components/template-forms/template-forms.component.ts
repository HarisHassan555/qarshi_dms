import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { DepartmentService } from 'src/app/services/department/department.service';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';

@Component({
    selector: 'app-template-forms',
    templateUrl: './template-forms.component.html',
    styleUrls: ['./template-forms.component.css']
})
export class TemplateFormsComponent implements OnInit, OnDestroy {
    @ViewChild('visibilityModal') visibilityModal: any;
    @ViewChild('duplicateModal') duplicateModal: any;

    templates: SavedTemplateDefinition[] = [];
    loadError = '';
    search = '';
    isLoading = false;
    page = 0;
    pageSize = 10;
    totalTemplates = 0;
    allUsers: any[] = [];
    departments: any[] = [];
    selectedTemplateForVisibility: SavedTemplateDefinition | null = null;
    selectedVisibilityUserIds: number[] = [];
    selectedVisibilityDepartmentIds: number[] = [];
    savingVisibility = false;
    duplicatingTemplate = false;
    togglingTemplateStatusId = '';
    isAdmin = false;
    currentUserId: number | null = null;
    duplicateSourceTemplateId = '';
    duplicateTemplateName = '';
    private searchTimer: ReturnType<typeof setTimeout> | null = null;
    cols = [
        { field: 'name', title: 'Template Name' },
        { field: 'codeConvention', title: 'Code Convention' },
        { field: 'status', title: 'Status' },
        { field: 'visibility', title: 'Visibility' },
        { field: 'updatedAt', title: 'Updated' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' }
    ];

    constructor(
        private router: Router,
        private templateWorkflowService: TemplateWorkflowService,
        private departmentService: DepartmentService,
        private userService: UserService,
        private notificationService: NotificationService
    ) { }

    ngOnInit(): void {
        this.currentUserId = this.getCurrentUserId();
        this.isAdmin = this.isCurrentUserAdmin();
        if (this.isAdmin) {
            this.loadDepartments();
            this.loadUsers();
        }
        this.loadTemplates();
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
    }

    loadTemplates(): void {
        this.isLoading = true;
        this.templateWorkflowService.getTemplateSummaries(this.page, this.pageSize, this.search, this.currentUserId)
            .pipe(finalize(() => {
                this.isLoading = false;
            }))
            .subscribe({
            next: (response) => {
                this.templates = (response?.items || []).filter((template: any) => template && template.id);
                this.totalTemplates = Number(response?.total || 0);
                this.loadError = '';
            },
            error: () => {
                this.templates = [];
                this.totalTemplates = 0;
                this.loadError = 'Saved template list could not be loaded.';
            }
        });
    }

    onSearchChanged(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => {
            this.page = 0;
            this.loadTemplates();
        }, 300);
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.loadTemplates();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10;
        this.page = 0;
        this.loadTemplates();
    }

    loadUsers(): void {
        this.userService.getUsers().subscribe({
            next: (users: any) => {
                this.allUsers = Array.isArray(users) ? users : [];
            },
            error: () => {
                this.allUsers = [];
            }
        });
    }

    loadDepartments(): void {
        this.departmentService.getAll().subscribe({
            next: (departments: any) => {
                this.departments = Array.isArray(departments)
                    ? departments.filter((department: any) => department && department.blIsDeleted !== true && department.blnStatus !== false)
                    : [];
            },
            error: () => {
                this.departments = [];
            }
        });
    }

    fillTemplate(template: SavedTemplateDefinition): void {
        this.router.navigate(['/template-fill', template.id]);
    }

    addTemplate(): void {
        if (!this.isAdmin) {
            return;
        }
        this.router.navigate(['/template-builder']);
    }

    openDuplicateModal(): void {
        if (!this.isAdmin) {
            return;
        }
        this.duplicateSourceTemplateId = '';
        this.duplicateTemplateName = '';
        this.duplicateModal?.open();
    }

    closeDuplicateModal(): void {
        this.duplicateModal?.close();
        this.duplicateSourceTemplateId = '';
        this.duplicateTemplateName = '';
        this.duplicatingTemplate = false;
    }

    duplicateTemplate(): void {
        if (!this.isAdmin || this.duplicatingTemplate) {
            return;
        }

        const sourceTemplateId = String(this.duplicateSourceTemplateId || '').trim();
        const newTemplateName = String(this.duplicateTemplateName || '').trim();
        if (!sourceTemplateId) {
            this.notificationService.showMessage('Please enter the existing form ID to copy.', 'warning');
            return;
        }
        if (!newTemplateName) {
            this.notificationService.showMessage('Please enter a name for the new form.', 'warning');
            return;
        }

        this.duplicatingTemplate = true;
        this.templateWorkflowService.duplicateTemplate(sourceTemplateId, newTemplateName).subscribe({
            next: (createdTemplate) => {
                this.templates = [createdTemplate, ...this.templates.filter((template) => template.id !== createdTemplate.id)];
                this.notificationService.showMessage(
                    `Form duplicated successfully. New form ID: ${createdTemplate.id}.`,
                    'success'
                );
                this.closeDuplicateModal();
            },
            error: (error: any) => {
                this.duplicatingTemplate = false;
                this.notificationService.showMessage(
                    error?.message || error?.error?.message || 'Unable to duplicate form.',
                    'danger'
                );
            }
        });
    }

    editTemplate(template: SavedTemplateDefinition): void {
        if (!this.isAdmin) {
            return;
        }
        this.router.navigate(['/template-builder'], { queryParams: { id: template.id } });
    }

    toggleTemplateStatus(template: SavedTemplateDefinition): void {
        if (!this.isAdmin || !template?.id || this.togglingTemplateStatusId === template.id) {
            return;
        }

        const nextIsActive = template.isActive === false;
        const actionLabel = nextIsActive ? 'activate' : 'inactive';
        const confirmed = window.confirm(
            nextIsActive
                ? `Activate "${this.getTemplateName(template)}" so users can access it again?`
                : `Mark "${this.getTemplateName(template)}" as inactive? It will be hidden from users.`
        );
        if (!confirmed) {
            return;
        }

        this.togglingTemplateStatusId = template.id;
        this.templateWorkflowService.updateTemplateStatus(template, nextIsActive).subscribe({
            next: (updatedTemplate) => {
                this.templates = this.templates.map((item) =>
                    item.id === updatedTemplate.id ? updatedTemplate : item
                );
                this.notificationService.showMessage(
                    `Template ${actionLabel === 'activate' ? 'activated' : 'marked inactive'} successfully.`,
                    'success'
                );
                this.togglingTemplateStatusId = '';
            },
            error: (error: any) => {
                this.togglingTemplateStatusId = '';
                this.notificationService.showMessage(
                    error?.message || error?.error?.message || 'Unable to update template status.',
                    'danger'
                );
            }
        });
    }

    openVisibilityModal(template: SavedTemplateDefinition): void {
        if (!this.isAdmin) {
            return;
        }
        this.selectedTemplateForVisibility = template;
        this.selectedVisibilityUserIds = Array.isArray(template.visibilityUserIds) ? [...template.visibilityUserIds] : [];
        this.selectedVisibilityDepartmentIds = [];
        this.visibilityModal?.open();
    }

    closeVisibilityModal(): void {
        this.visibilityModal?.close();
        this.selectedTemplateForVisibility = null;
        this.selectedVisibilityUserIds = [];
        this.selectedVisibilityDepartmentIds = [];
        this.savingVisibility = false;
    }

    saveVisibility(): void {
        if (!this.selectedTemplateForVisibility || this.savingVisibility) {
            return;
        }
        this.savingVisibility = true;
        this.templateWorkflowService.updateTemplateVisibility(
            this.selectedTemplateForVisibility,
            this.selectedVisibilityUserIds
        ).subscribe({
            next: (updatedTemplate) => {
                this.templates = this.templates.map((template) =>
                    template.id === updatedTemplate.id ? updatedTemplate : template
                );
                this.notificationService.showMessage('Template visibility updated successfully.', 'success');
                this.closeVisibilityModal();
            },
            error: (error: any) => {
                this.savingVisibility = false;
                this.notificationService.showMessage(
                    error?.message || error?.error?.message || 'Unable to update template visibility.',
                    'danger'
                );
            }
        });
    }

    getTemplateName(template: SavedTemplateDefinition): string {
        return template?.name || template?.payload?.name || 'Untitled Template';
    }

    getCodeConvention(template: SavedTemplateDefinition): string {
        const convention: any = template?.codeConvention || template?.payload?.codeConvention;
        if (typeof convention === 'string') {
            return convention;
        }
        return convention?.pattern || template?.payload?.codeConvention || 'TPL-0000';
    }

    getVisibilitySummary(template: SavedTemplateDefinition): string {
        const userIds = Array.isArray(template?.visibilityUserIds) ? template.visibilityUserIds : [];
        if (userIds.length === 0) {
            return 'All Users';
        }
        return `${userIds.length} User${userIds.length === 1 ? '' : 's'}`;
    }

    getStatusSummary(template: SavedTemplateDefinition): string {
        return template?.isActive === false ? 'Inactive' : 'Active';
    }

    getDisplayedTemplates(): SavedTemplateDefinition[] {
        return this.templates || [];
    }

    get totalPages(): number {
        return Math.max(1, Math.ceil(this.totalTemplates / this.pageSize));
    }

    get pageStart(): number {
        return this.totalTemplates === 0 ? 0 : (this.page * this.pageSize) + 1;
    }

    get pageEnd(): number {
        return Math.min(this.totalTemplates, (this.page + 1) * this.pageSize);
    }

    get visibilityUserOptions(): Array<{ id: number; name: string; roleName: string }> {
        return this.allUsers
            .map((user) => ({
                id: this.getUserId(user),
                name: this.getUserDisplayName(user),
                roleName: this.getUserRoleName(user)
            }))
            .filter((user) => Number.isFinite(user.id) && user.id > 0);
    }

    onVisibilityDepartmentsChanged(): void {
        const departmentUserIds = this.selectedVisibilityDepartmentIds.flatMap((departmentId) =>
            this.getDepartmentUsers(departmentId).map((user: any) => this.getUserId(user))
        );
        const mergedUserIds = Array.from(new Set([
            ...this.selectedVisibilityUserIds,
            ...departmentUserIds
        ].filter((userId) => Number.isFinite(userId) && userId > 0)));
        this.selectedVisibilityUserIds = mergedUserIds;
    }

    removeVisibilityUser(userId: number): void {
        this.selectedVisibilityUserIds = this.selectedVisibilityUserIds.filter((id) => id !== userId);
    }

    getSelectedVisibilityUsers(): any[] {
        return this.selectedVisibilityUserIds
            .map((userId) => this.findUserById(userId))
            .filter((user) => !!user);
    }

    getDepartmentUsers(departmentId: number): any[] {
        return this.allUsers.filter((user) => this.getDepartmentIdForUser(user) === Number(departmentId));
    }

    getDepartmentName(department: any): string {
        return department?.txtDepartmentName || department?.departmentName || department?.name || 'Department';
    }

    getUserDisplayName(user: any): string {
        return user?.txtUserName || user?.userName || user?.name || 'User';
    }

    getUserRoleName(user: any): string {
        return user?.cfgTblRole?.txtRoleName || user?.txtrole || user?.roleName || '';
    }

    getSelectedDepartmentUserCount(departmentId: number): number {
        return this.getDepartmentUsers(departmentId)
            .filter((user) => this.selectedVisibilityUserIds.includes(this.getUserId(user)))
            .length;
    }

    getDepartmentNameById(departmentId: number): string {
        return this.getDepartmentName(
            this.departments.find((department) => Number(department?.serDepartmentId) === Number(departmentId))
        );
    }

    private getCurrentUserId(): number | null {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            const userId = Number(user?.serUserId || user?.userId || user?.id || 0);
            return Number.isFinite(userId) && userId > 0 ? userId : null;
        } catch {
            return null;
        }
    }

    private isCurrentUserAdmin(): boolean {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            const roleName = (
                user?.cfgTblRole?.txtRoleName ||
                user?.txtrole ||
                user?.roleName ||
                ''
            ).toString().trim().toUpperCase();
            return roleName === 'ADMIN'
                || roleName === 'ROLE_ADMIN'
                || roleName === 'SUPER ADMIN'
                || roleName === 'ROLE_SUPER ADMIN'
                || roleName.includes('ADMIN');
        } catch {
            return false;
        }
    }

    getUserId(user: any): number {
        return Number(user?.serUserId || user?.userId || user?.id || 0);
    }

    private findUserById(userId: number): any | null {
        const numericUserId = Number(userId);
        return this.allUsers.find((user) => this.getUserId(user) === numericUserId) || null;
    }

    private getDepartmentIdForUser(user: any): number | null {
        const directDepartmentId = Number(
            user?.hrTblDepartment?.serDepartmentId ||
            user?.serDepartmentId ||
            user?.departmentId ||
            0
        );
        if (Number.isFinite(directDepartmentId) && directDepartmentId > 0) {
            return directDepartmentId;
        }

        const departmentName = (
            user?.txtDepartmentName ||
            user?.departmentName ||
            user?.hrTblDepartment?.txtDepartmentName ||
            ''
        ).toString().trim().toLowerCase();
        if (!departmentName) {
            return null;
        }

        const matchedDepartment = this.departments.find((department) =>
            (department?.txtDepartmentName || department?.departmentName || department?.name || '')
                .toString()
                .trim()
                .toLowerCase() === departmentName
        );

        const matchedDepartmentId = Number(
            matchedDepartment?.serDepartmentId ||
            matchedDepartment?.departmentId ||
            0
        );
        return Number.isFinite(matchedDepartmentId) && matchedDepartmentId > 0 ? matchedDepartmentId : null;
    }
}
