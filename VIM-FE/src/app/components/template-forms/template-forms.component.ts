import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from 'src/app/NotificationService';
import { DepartmentService } from 'src/app/services/department/department.service';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';

@Component({
    selector: 'app-template-forms',
    templateUrl: './template-forms.component.html',
    styleUrls: ['./template-forms.component.css']
})
export class TemplateFormsComponent implements OnInit {
    @ViewChild('visibilityModal') visibilityModal: any;

    templates: SavedTemplateDefinition[] = [];
    loadError = '';
    search = '';
    allUsers: any[] = [];
    departments: any[] = [];
    selectedTemplateForVisibility: SavedTemplateDefinition | null = null;
    selectedVisibilityUserIds: number[] = [];
    selectedVisibilityDepartmentIds: number[] = [];
    selectedVisibilityUserToAdd: number | null = null;
    savingVisibility = false;
    isAdmin = false;
    currentUserId: number | null = null;
    cols = [
        { field: 'name', title: 'Template Name' },
        { field: 'codeConvention', title: 'Code Convention' },
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

    loadTemplates(): void {
        this.templateWorkflowService.getTemplates(this.currentUserId).subscribe({
            next: (templates) => {
                this.templates = (templates || []).filter((template: any) => template && template.id);
                this.loadError = '';
            },
            error: () => {
                this.templates = [];
                this.loadError = 'Saved template list could not be loaded.';
            }
        });
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

    editTemplate(template: SavedTemplateDefinition): void {
        if (!this.isAdmin) {
            return;
        }
        this.router.navigate(['/template-builder'], { queryParams: { id: template.id } });
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
        this.selectedVisibilityUserToAdd = null;
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

    getDisplayedTemplates(): SavedTemplateDefinition[] {
        const term = (this.search || '').toString().trim().toLowerCase();
        if (!term) {
            return this.templates;
        }
        return this.templates.filter((template) => {
            const haystack = [
                this.getTemplateName(template),
                this.getCodeConvention(template),
                this.getVisibilitySummary(template),
                template?.updatedAt
            ]
                .map((value) => String(value || '').toLowerCase())
                .join(' ');
            return haystack.includes(term);
        });
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

    addVisibilityUser(userId: number | null): void {
        const numericUserId = Number(userId || 0);
        if (!Number.isFinite(numericUserId) || numericUserId <= 0) {
            this.selectedVisibilityUserToAdd = null;
            return;
        }
        if (!this.selectedVisibilityUserIds.includes(numericUserId)) {
            this.selectedVisibilityUserIds = [...this.selectedVisibilityUserIds, numericUserId];
        }
        this.selectedVisibilityUserToAdd = null;
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
