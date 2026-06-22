import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, switchMap } from 'rxjs/operators';
import { NotificationService } from 'src/app/NotificationService';
import { MenuService } from 'src/app/layout/menu-service/menu.service';
import { UserService } from 'src/app/services/user/user.service';
import { SharedDataService } from 'src/app/services/shared-data/shared-data.service';

interface SubMenuNode {
    subMenuId: number;
    subMenuName: string;
    menuId: number;
    selected: boolean;
}

interface MenuNode {
    menuId: number;
    menuName: string;
    selected: boolean;
    subMenus: SubMenuNode[];
}

interface RoleViewModel {
    serRoleId: number;
    txtRoleName: string;
    txtRoleCode?: string;
    blnStatus: boolean;
    userCount: number;
    permissionsCount: number;
    permissionTiles: string[];
}

@Component({
    selector: 'app-roles-management',
    templateUrl: './roles-management.component.html',
    styleUrls: ['./roles-management.component.css']
})
export class RolesManagementComponent implements OnInit {
    roles: RoleViewModel[] = [];
    filteredRoles: RoleViewModel[] = [];
    menus: MenuNode[] = [];
    roleForm!: FormGroup;
    loading = false;
    saving = false;
    searchText = '';
    showModal = false;
    showEditModal = false;
    editForm!: FormGroup;
    editingRole: RoleViewModel | null = null;

    private currentUser: any;

    constructor(
        private fb: FormBuilder,
        private userService: UserService,
        private menuService: MenuService,
        private notificationService: NotificationService,
        private sharedDataService: SharedDataService
    ) { }

    ngOnInit(): void {
        this.currentUser = this.getUserFromLocalStorage();
        this.roleForm = this.fb.group({
            roleName: ['', [Validators.required, Validators.maxLength(100)]]
        });
        this.editForm = this.fb.group({
            serRoleId: [null, [Validators.required]],
            txtRoleName: ['', [Validators.required, Validators.maxLength(100)]],
            txtRoleCode: ['', [Validators.required, Validators.maxLength(100)]],
            blnStatus: [true]
        });
        this.loadData();
    }

    loadData(): void {
        this.loading = true;
        forkJoin({
            roles: this.userService.getRoles().pipe(catchError(() => of([]))),
            users: this.userService.getUsers().pipe(catchError(() => of([]))),
            menuTree: this.menuService.getAllMenusRaw().pipe(catchError(() => of([])))
        })
            .pipe(finalize(() => (this.loading = false)))
            .subscribe(({ roles, users, menuTree }: any) => {
                this.initializeMenuTree(menuTree || []);
                this.mergeActivityLogsSubMenu(menuTree || []);
                this.roles = (roles || []).map((role: any) => {
                    const roleId = Number(role?.serRoleId);
                    const roleUsers = (users || []).filter((u: any) => Number(u?.cfgTblRole) === roleId || Number(u?.cfgTblRole?.serRoleId) === roleId);
                    return {
                        serRoleId: roleId,
                        txtRoleName: role?.txtRoleName || '',
                        txtRoleCode: role?.txtRoleCode || '',
                        blnStatus: role?.blnStatus === true,
                        userCount: roleUsers.length,
                        permissionsCount: 0,
                        permissionTiles: []
                    } as RoleViewModel;
                });

                this.loadPermissionSummaryPerRole();
            });
    }

    onSearch(term: string): void {
        this.searchText = (term || '').toLowerCase();
        this.applyFilter();
    }

    openAddModal(): void {
        this.roleForm.reset({ roleName: '' });
        this.resetMenuSelections();
        this.showModal = true;
    }

    closeAddModal(): void {
        this.showModal = false;
    }

    openEditModal(role: RoleViewModel): void {
        this.editingRole = role;
        this.editForm.reset({
            serRoleId: role.serRoleId,
            txtRoleName: role.txtRoleName || '',
            txtRoleCode: role.txtRoleCode || (role.txtRoleName || '').toUpperCase().replace(/\s+/g, '_'),
            blnStatus: role.blnStatus === true
        });

        // Load existing permissions for this role
        this.resetMenuSelections();
        this.menuService.getAllSubMenuRoles(role.serRoleId, 0).subscribe({
            next: (perms: any[]) => {
                const subMenuIds = (perms || [])
                    .filter((p) => {
                        const isDeleted = p?.blIsDeleted === true || p?.blIsDeleted === 1;
                        const isEnabled = p?.blIsEnabled === true || p?.blIsEnabled === 1;
                        const isActive = p?.blIsActive === true || p?.blIsActive === 1;
                        const isStatus = p?.blnStatus === true || p?.blnStatus === 1;
                        return !isDeleted && (isEnabled || isActive || isStatus);
                    })
                    .map((p) => Number(p?.cfgTblSubMenu?.serSubMenuId || 0));

                this.menus.forEach((menu) => {
                    menu.subMenus.forEach((sm) => {
                        if (subMenuIds.includes(sm.subMenuId)) {
                            sm.selected = true;
                        }
                    });
                    // A menu is 'selected' if all its submenus are selected
                    menu.selected = menu.subMenus.length > 0 && menu.subMenus.every((sm) => sm.selected);
                });
            },
            error: () => {
                this.notificationService.showMessage('Unable to load role permissions.', 'warning');
            }
        });

        this.showEditModal = true;
    }

    closeEditModal(): void {
        this.showEditModal = false;
        this.editingRole = null;
        this.resetMenuSelections();
    }

    toggleMenu(menu: MenuNode): void {
        menu.subMenus.forEach((sm) => (sm.selected = menu.selected));
    }

    toggleSubMenu(menu: MenuNode): void {
        menu.selected = menu.subMenus.every((sm) => sm.selected);
    }

    saveRole(): void {
        if (this.roleForm.invalid) {
            this.roleForm.markAllAsTouched();
            return;
        }

        const roleName = (this.roleForm.value.roleName || '').trim();
        if (!roleName) {
            return;
        }

        const selectedSubMenus = this.getSelectedSubMenus();
        if (!selectedSubMenus.length) {
            this.notificationService.showMessage('Please select at least one submenu permission.', 'danger');
            return;
        }

        const userId = Number(this.currentUser?.serUserId || 0);
        if (!userId) {
            this.notificationService.showMessage('Unable to identify current user.', 'danger');
            return;
        }

        this.saving = true;
        const selectedSubMenuIds = selectedSubMenus
            .map((subMenu) => Number(subMenu?.subMenuId || 0))
            .filter((id) => id > 0);
        const rolePayload = {
            txtRoleName: roleName,
            txtRoleCode: roleName.toUpperCase().replace(/\s+/g, '_'),
            blIsActive: true,
            blnStatus: true,
            blIsDeleted: false,
            serCreatedUser: userId,
            // Include selected submenu ids in addNewRole payload in a backend-safe nested shape.
            cfgTblSubMenuRoles: selectedSubMenuIds.map((serSubMenuId) => ({
                serCreatedUser: userId,
                cfgTblSubMenu: { serSubMenuId }
            }))
        };

        this.userService
            .addRole(rolePayload)
            .pipe(
                switchMap((result: any) => {
                    const status = (result || '').toString().trim().toUpperCase();
                    if (status === 'EXIST') {
                        throw new Error('ROLE_EXISTS');
                    }
                    if (status !== 'SUCCESS') {
                        throw new Error('ROLE_CREATE_FAILED');
                    }
                    return this.userService.getRoles();
                }),
                switchMap((roles: any) => {
                    const createdRole = (roles || [])
                        .filter((r: any) => (r?.txtRoleName || '').trim().toLowerCase() === roleName.toLowerCase())
                        .sort((a: any, b: any) => Number(b?.serRoleId || 0) - Number(a?.serRoleId || 0))[0];

                    const roleId = Number(createdRole?.serRoleId || 0);
                    if (!roleId) {
                        return of({ status: 'role-created-no-permissions' });
                    }

                    const listPayload = this.constructPermissionPayload(roleId, userId, selectedSubMenus);

                    // Use role-level assignment (userId=0 interpreted server-side as NULL user).
                    return this.menuService.saveSubMenuRole('0', String(roleId), listPayload);
                }),
                finalize(() => (this.saving = false))
            )
            .subscribe({
                next: (permissionSaveResult: any) => {
                    const parsedStatus =
                        typeof permissionSaveResult === 'string'
                            ? permissionSaveResult.trim()
                            : (permissionSaveResult?.status || permissionSaveResult?.message || '').toString().trim();

                    let isSuccess = parsedStatus.toUpperCase() === 'SUCCESS';
                    if (!isSuccess && typeof permissionSaveResult === 'string' && permissionSaveResult.trim().startsWith('{')) {
                        try {
                            const obj = JSON.parse(permissionSaveResult);
                            isSuccess = (obj?.status || '').toString().trim().toUpperCase() === 'SUCCESS';
                        } catch {
                            isSuccess = false;
                        }
                    }
                    if (!isSuccess) {
                        this.notificationService.showMessage('Role created, but permissions failed to save.', 'danger');
                        this.loadData();
                        return;
                    }
                    this.notificationService.showMessage('Role created successfully.', 'success');
                    this.sharedDataService.triggerMenuRefresh();
                    this.closeAddModal();
                    this.loadData();
                },
                error: (error: any) => {
                    if (error?.message === 'ROLE_EXISTS') {
                        this.notificationService.showMessage('Role already exists.', 'danger');
                        return;
                    }
                    this.notificationService.showMessage('Unable to create role.', 'danger');
                }
            });
    }

    updateRole(): void {
        if (this.editForm.invalid || !this.editingRole) {
            this.editForm.markAllAsTouched();
            return;
        }

        const selectedSubMenus = this.getSelectedSubMenus();
        if (!selectedSubMenus.length) {
            this.notificationService.showMessage('Please select at least one permission.', 'danger');
            return;
        }

        const formValue = this.editForm.value;
        const roleId = Number(formValue.serRoleId);
        const userId = Number(this.currentUser?.serUserId || 0) || 1;
        const payload = {
            serRoleId: roleId,
            txtRoleName: (formValue.txtRoleName || '').toString().trim(),
            txtRoleCode: (formValue.txtRoleCode || '').toString().trim(),
            blnStatus: formValue.blnStatus === true,
            blIsActive: true,
            blIsDeleted: false,
            serModifiedUser: userId
        };

        this.saving = true;
        this.userService
            .updateRole(payload)
            .pipe(
                switchMap((res: any) => {
                    const status = this.parseStatus(res);
                    if (status === 'EXIST') {
                        throw new Error('ROLE_EXISTS');
                    }
                    if (status !== 'SUCCESS') {
                        throw new Error('ROLE_UPDATE_FAILED');
                    }

                    // For existing role, we must handle permissions:
                    // 1. Fetch current permissions to see what needs deletion
                    // 2. Delete those not in selected list
                    // 3. Save/Update selected ones
                    return this.menuService.getAllSubMenuRoles(roleId, 0);
                }),
                switchMap((existingPerms: any[]) => {
                    const selectedSubMenuIds = selectedSubMenus.map((sm) => sm.subMenuId);
                    const idsToRemove = (existingPerms || [])
                        .filter((p) => p?.cfgTblSubMenu && !selectedSubMenuIds.includes(Number(p.cfgTblSubMenu.serSubMenuId)))
                        .map((p) => Number(p.serSubMenuRoleId))
                        .filter((id) => id > 0);

                    const listPayload = this.constructPermissionPayload(roleId, userId, selectedSubMenus);

                    if (idsToRemove.length > 0) {
                        return this.menuService.deleteSubMenuRole(idsToRemove).pipe(
                            switchMap(() => this.menuService.saveSubMenuRole('0', String(roleId), listPayload)),
                            catchError(() => this.menuService.saveSubMenuRole('0', String(roleId), listPayload)) // Try saving even if delete fails
                        );
                    } else {
                        return this.menuService.saveSubMenuRole('0', String(roleId), listPayload);
                    }
                }),
                finalize(() => (this.saving = false))
            )
            .subscribe({
                next: (permissionSaveResult: any) => {
                    const status = this.parseStatus(permissionSaveResult);
                    if (status !== 'SUCCESS') {
                        this.notificationService.showMessage('Role updated, but permissions failed to sync.', 'warning');
                        this.loadData();
                        return;
                    }
                    this.notificationService.showMessage('Role and permissions updated successfully.', 'success');
                    this.sharedDataService.triggerMenuRefresh();
                    this.closeEditModal();
                    this.loadData();
                },
                error: (error: any) => {
                    if (error?.message === 'ROLE_EXISTS') {
                        this.notificationService.showMessage('Role name already exists.', 'danger');
                    } else {
                        this.notificationService.showMessage('Unable to update role or permissions.', 'danger');
                    }
                }
            });
    }

    private constructPermissionPayload(roleId: number, userId: number, selectedSubMenus: SubMenuNode[]): any[] {
        return selectedSubMenus.map((subMenu) => ({
            serSubMenuRoleId: null,
            blIsActive: true,
            blnStatus: true,
            blIsDeleted: false,
            blIsEnabled: true,
            blIsview: true,
            blIsAdd: true,
            blIsDelete: false,
            blIsUpdate: true,
            blIsApprove: false,
            blIsAll: false,
            blIsNewCreate: true,
            blIsNewView: true,
            blIsNewUpdate: true,
            serCreatedUser: userId,
            cfgTblRole: { serRoleId: roleId },
            cfgTblSubMenu: {
                serSubMenuId: subMenu.subMenuId,
                cfgTblMenu: { serMenuId: subMenu.menuId }
            },
            cfgTblUser: null
        }));
    }

    deleteRole(role: RoleViewModel): void {
        if (!role?.serRoleId) return;

        const roleName = (role.txtRoleName || '').toString().trim().toUpperCase();
        if (role.serRoleId === 1 || roleName === 'ADMIN' || roleName === 'ROLE_ADMIN' || roleName.includes('ADMIN')) {
            this.notificationService.showMessage('Admin role cannot be deleted.', 'danger');
            return;
        }
        if (Number(role.userCount || 0) > 0) {
            this.notificationService.showMessage('Role has assigned users. Reassign users before delete.', 'danger');
            return;
        }

        const confirmed = window.confirm(`Delete role "${role.txtRoleName}"?`);
        if (!confirmed) return;

        this.saving = true;
        this.userService.deleteRole([role.serRoleId])
            .pipe(finalize(() => (this.saving = false)))
            .subscribe({
                next: (res: any) => {
                    const status = this.parseStatus(res);
                    if (status === 'SUCCESS') {
                        this.notificationService.showMessage('Role deleted successfully.', 'success');
                        this.sharedDataService.triggerMenuRefresh();
                        this.loadData();
                        return;
                    }
                    this.notificationService.showMessage('Unable to delete role.', 'danger');
                },
                error: () => {
                    this.notificationService.showMessage('Unable to delete role.', 'danger');
                }
            });
    }

    getStatusBadgeClass(role: RoleViewModel): string {
        return role.blnStatus ? 'badge bg-success' : 'badge bg-secondary';
    }

    private applyFilter(): void {
        if (!this.searchText) {
            this.filteredRoles = [...this.roles];
            return;
        }

        this.filteredRoles = this.roles.filter((role) => {
            const haystack = `${role.txtRoleName} ${role.txtRoleCode || ''} ${role.blnStatus ? 'active' : 'inactive'}`.toLowerCase();
            return haystack.includes(this.searchText);
        });
    }

    private loadPermissionSummaryPerRole(): void {
        if (!this.roles.length) {
            this.applyFilter();
            return;
        }

        // Use userId=0 intentionally to force role-level permission rows (ser_user_id IS NULL fallback)
        // and avoid current user's overrides reducing displayed role permissions.
        const roleLevelUserId = 0;
        const requests = this.roles.map((role) =>
            this.menuService.getAllSubMenuRoles(role.serRoleId, roleLevelUserId).pipe(catchError(() => of([])))
        );

        forkJoin(requests).subscribe((allRolePermissions: any[]) => {
            this.roles = this.roles.map((role, index) => {
                const rows = allRolePermissions[index] || [];
                const enabledRows = rows.filter((r: any) => {
                    const isDeleted = r?.blIsDeleted === true || r?.blIsDeleted === 1;
                    const isEnabled = r?.blIsEnabled === true || r?.blIsEnabled === 1;
                    const isActive = r?.blIsActive === true || r?.blIsActive === 1;
                    const isStatus = r?.blnStatus === true || r?.blnStatus === 1;
                    return !isDeleted && (isEnabled || isActive || isStatus);
                });
                const names: string[] = enabledRows
                    .map((r: any) => r?.cfgTblSubMenu?.txtSubMenuName)
                    .filter((name: any): name is string => typeof name === 'string' && name.trim().length > 0);
                const uniqueNames: string[] = Array.from(new Set<string>(names));

                // Frontend fallback so admin always sees Role Management permission tile
                // even when submenu permission rows are not fully seeded in DB.
                const roleName = (role?.txtRoleName || '').toString().trim().toUpperCase();
                const isAdminRole =
                    roleName === 'ADMIN' ||
                    roleName === 'ROLE_ADMIN' ||
                    roleName === 'SUPER ADMIN' ||
                    roleName === 'ROLE_SUPER ADMIN' ||
                    roleName.includes('ADMIN');
                if (isAdminRole && !uniqueNames.some((p) => p.toLowerCase() === 'role management')) {
                    uniqueNames.push('Role Management');
                }
                return {
                    ...role,
                    permissionsCount: uniqueNames.length,
                    permissionTiles: uniqueNames
                };
            });

            this.applyFilter();
        });
    }

    private initializeMenuTree(apiMenus: any[]): void {
        this.menus = (apiMenus || [])
            .filter((menu: any) => {
                const isDeleted = menu?.blIsDeleted === true || menu?.blIsDeleted === 1;
                const isActive = menu?.blIsActive === true || menu?.blIsActive === 1;
                const isStatus = menu?.blnStatus === true || menu?.blnStatus === 1;
                return !isDeleted && (isActive || isStatus);
            })
            .map((menu: any) => ({
                menuId: Number(menu?.serMenuId || menu?.menuId || 0),
                menuName: menu?.txtMenuName || menu?.menuName || '',
                selected: false,
                subMenus: (menu?.cfgTblSubMenus || menu?.subMenus || [])
                    .filter((sm: any) => {
                        const isDeleted = sm?.blIsDeleted === true || sm?.blIsDeleted === 1;
                        const isActive = sm?.blIsActive === true || sm?.blIsActive === 1;
                        const isStatus = sm?.blnStatus === true || sm?.blnStatus === 1;
                        return !isDeleted && (isActive || isStatus);
                    })
                    .sort((a: any, b: any) => Number(a?.intSubMenuOrder || a?.submenuOrder || 0) - Number(b?.intSubMenuOrder || b?.submenuOrder || 0))
                    .map((sm: any) => ({
                        subMenuId: Number(sm?.serSubMenuId || sm?.subMenuId || 0),
                        subMenuName: sm?.txtSubMenuName || sm?.subMenuName || '',
                        menuId: Number(menu?.serMenuId || menu?.menuId || 0),
                        selected: false
                    }))
                    .filter((sm: SubMenuNode) => sm.subMenuId > 0)
            }))
            .filter((menu: MenuNode) => !!menu.menuName && menu.menuId > 0 && menu.subMenus.length > 0);
    }

    private mergeActivityLogsSubMenu(apiMenus: any[]): void {
        const userMgmt = this.menus.find(
            (menu) => (menu.menuName || '').trim().toLowerCase() === 'user management'
        );
        if (!userMgmt) {
            return;
        }

        const alreadyListed = userMgmt.subMenus.some(
            (sm) => (sm.subMenuName || '').trim().toLowerCase() === 'activity logs'
        );
        if (alreadyListed) {
            return;
        }

        let activityLogsRaw: any = null;
        for (const menu of apiMenus || []) {
            const subMenus = menu?.cfgTblSubMenus || menu?.subMenus || [];
            for (const sm of subMenus) {
                const name = (sm?.txtSubMenuName || sm?.subMenuName || '').toString().trim().toLowerCase();
                const url = (sm?.txtSubMenuUrl || sm?.subMenuAction || '')
                    .toString()
                    .trim()
                    .replace(/^\/+/, '')
                    .toLowerCase();
                if (name === 'activity logs' || url === 'activitylogs') {
                    activityLogsRaw = sm;
                    break;
                }
            }
            if (activityLogsRaw) {
                break;
            }
        }

        const subMenuId = Number(activityLogsRaw?.serSubMenuId || activityLogsRaw?.subMenuId || 0);
        if (subMenuId <= 0) {
            return;
        }

        userMgmt.subMenus.push({
            subMenuId,
            subMenuName: activityLogsRaw?.txtSubMenuName || activityLogsRaw?.subMenuName || 'Activity Logs',
            menuId: userMgmt.menuId,
            selected: false
        });
        userMgmt.subMenus.sort((a, b) => a.subMenuName.localeCompare(b.subMenuName));
    }

    private resetMenuSelections(): void {
        this.menus.forEach((menu) => {
            menu.selected = false;
            menu.subMenus.forEach((sm) => (sm.selected = false));
        });
    }

    private getSelectedSubMenus(): SubMenuNode[] {
        return this.menus.flatMap((menu) => menu.subMenus.filter((sm) => sm.selected));
    }

    private getUserFromLocalStorage(): any {
        const userJson = localStorage.getItem('user');
        if (!userJson) return null;
        try {
            return JSON.parse(userJson);
        } catch {
            return null;
        }
    }

    private parseStatus(res: any): string {
        if (typeof res === 'string') {
            const raw = res.trim();
            if (raw.startsWith('{')) {
                try {
                    const obj = JSON.parse(raw);
                    return (obj?.status || obj?.message || '').toString().trim().toUpperCase();
                } catch {
                    return raw.toUpperCase();
                }
            }
            return raw.toUpperCase();
        }
        return (res?.status || res?.message || '').toString().trim().toUpperCase();
    }
}
