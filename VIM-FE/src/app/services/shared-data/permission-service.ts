import { Injectable } from '@angular/core';
import { Observable, of, BehaviorSubject } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { MenuService } from 'src/app/layout/menu-service/menu.service';


interface SubMenuPermission {
    blIsActive: boolean;
    blIsAdd: boolean;
    blIsAll: boolean;
    blIsApprove: boolean;
    blIsDelete: boolean;
    blIsDeleted: boolean;
    blIsEnabled: boolean;
    blIsNewCreate: boolean | null;
    blIsNewUpdate: boolean | null;
    blIsNewView: boolean | null;
    blIsUpdate: boolean;
    blIsview: boolean;
    blnStatus: boolean;
    cfgTblSubMenu: {
        serSubMenuId: number;
        txtSubMenuName: string;
        // Other fields as needed
    };
    cfgTblUser: {
        serUserId: number;
    };
    dteCreatedDate: number;
    dteModifiedDate: number;
    serCreatedUser: number;
    serModifiedUser: number | null;
    serSubMenuRoleId: number;
}

interface CfgTblUser {
    cfgTblRole: number | undefined;
    serUserId: number;
}

@Injectable({
    providedIn: 'root'
})
export class PermissionService {

    private subMenuRoles: SubMenuPermission[] = [];

    private permissionsLoadedSubject = new BehaviorSubject<boolean>(false);
    permissionsLoaded$ = this.permissionsLoadedSubject.asObservable();

    private moduleSubMenuMap: { [key: string]: string[] } = {
        'Dashboard': [],
        'Master Data': ['Department', 'Signature'],
        'Velocity': [
            'Application',
            'Applications View',
            'Pending Approvals',
            'Form Builder',
            'Budget Approval'
        ],
        'User Management': ['User', 'Password Policy', 'Change Password', 'Permission', 'Role Management', 'Activity Logs']
    };

    constructor(private menuService: MenuService) {}

    private normalizeSubMenuName(name: string): string {
        return (name || '').trim().toLowerCase();
    }

    private getRoleId(user: any): number | undefined {
        if (!user) {
            return undefined;
        }
        const rawRole = user.cfgTblRole;
        if (rawRole && typeof rawRole === 'object') {
            const nested = Number(rawRole.serRoleId);
            return Number.isFinite(nested) ? nested : undefined;
        }
        const direct = Number(rawRole);
        return Number.isFinite(direct) ? direct : undefined;
    }

    private isAdminUser(user: any): boolean {
        const roleName = (user?.cfgTblRole?.txtRoleName || user?.txtrole || '').toString().trim().toLowerCase();
        return roleName.includes('admin');
    }

    private getMatchingPermissions(subMenuName: string): SubMenuPermission[] {
        const normalized = this.normalizeSubMenuName(subMenuName);
        return (this.subMenuRoles || []).filter(role =>
            this.normalizeSubMenuName(role?.cfgTblSubMenu?.txtSubMenuName || '') === normalized
        );
    }

    private resolvePermissionFlag(
        subMenuName: string,
        newFlag: keyof SubMenuPermission,
        legacyFlag?: keyof SubMenuPermission
    ): boolean {
        const matching = this.getMatchingPermissions(subMenuName);
        if (!matching.length) {
            return false;
        }

        return matching.some(permission => {
            const newValue = permission[newFlag];
            const legacyValue = legacyFlag ? permission[legacyFlag] : false;
            return newValue === true || legacyValue === true;
        });
    }


    getUserFromLocalStorage(): CfgTblUser | null {
        const userJson = localStorage.getItem('user');

        if (userJson) {
            try {
                // Parse and assert type
                return JSON.parse(userJson) as CfgTblUser;
            } catch (error) {
                console.error('Error parsing user JSON from localStorage:', error);
                return null;
            }
        }

        return null;
    }


    /**
     * Load permissions for a given role and user
     * @param roleId The role ID (optional)
     * @param userId The user ID
     * @returns Observable of submenu permissions
     */
    loadPermissionRoles(roleId: number | undefined, userId: number): Observable<SubMenuPermission[]> {
        // Skip if already loaded
        /*if (this.permissionsLoadedSubject.value) {
            return of(this.subMenuRoles);
        }*/

        return this.menuService.getAllSubMenuRoles(roleId, userId).pipe(
            tap((data: SubMenuPermission[]) => {
                // @ts-ignore
              //  this.subMenuRoles = [];
                this.subMenuRoles = data;
                this.permissionsLoadedSubject.next(true);
                console.log('Loaded subMenuRoles:', this.subMenuRoles);
            }),
            catchError((error: any) => {
                console.error('Error fetching submenu roles:', error);
                this.subMenuRoles = [];
                this.permissionsLoadedSubject.next(true);
                return of([]);
            })
        );
    }

    // @ts-ignore
    // @ts-ignore
    /**
     * Check if the user can add items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canAdd(subMenuName: string): Observable<boolean> {
        const user = this.getUserFromLocalStorage();

        if (!user) {
            return of(false);
        }

        if (this.isAdminUser(user)) {
            return of(true);
        }

        const roleId = this.getRoleId(user);
        if (!roleId) {
            return of(false);
        }

        return this.loadPermissionRoles(roleId, user.serUserId).pipe(
            map(roles => {
                // Validate and update subMenuRoles
                console.log('Roles received:', roles);
                this.subMenuRoles = Array.isArray(roles) ? roles : [];
                const canCreate = this.resolvePermissionFlag(subMenuName, 'blIsNewCreate', 'blIsAdd');
                console.log('Permission for', subMenuName, ':', canCreate);
                return canCreate;
            }),
            catchError(error => {
                console.error('Error loading permission roles:', error);
                this.subMenuRoles = []; // Clear subMenuRoles on error
                return of(false);
            })
        );
    }
    /*canAdd(subMenuName: string): boolean {

        debugger;
        const user = this.getUserFromLocalStorage();

        if (!user || !user.cfgTblRole) {
            // @ts-ignore
            return of(false);
        }
        // @ts-ignore
        return this.loadPermissionRoles(user.cfgTblRole, user.serUserId).pipe(
            map(() => {
                const permission = this.subMenuRoles.find(
                    role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
                );
                debugger;
                return permission?.blIsNewCreate ?? false;
            }),
            catchError(error => {
                console.error('Error loading permission roles:', error);
                return of(false);
            })
        );
    }*/

    /**
     * Check if the user can update items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canUpdate(subMenuName: string): boolean {
        const user = this.getUserFromLocalStorage();
        if (this.isAdminUser(user)) {
            return true;
        }
        return this.resolvePermissionFlag(subMenuName, 'blIsNewUpdate', 'blIsUpdate');
    }

    /**
     * Async update permission check that refreshes permissions before evaluating.
     * Useful for components that should not rely on previously cached permission state.
     */
    canUpdateAsync(subMenuName: string): Observable<boolean> {
        const user = this.getUserFromLocalStorage();

        if (!user) {
            return of(false);
        }

        if (this.isAdminUser(user)) {
            return of(true);
        }

        const roleId = this.getRoleId(user);
        if (!roleId) {
            return of(false);
        }

        return this.loadPermissionRoles(roleId, user.serUserId).pipe(
            map(roles => {
                this.subMenuRoles = Array.isArray(roles) ? roles : [];
                return this.resolvePermissionFlag(subMenuName, 'blIsNewUpdate', 'blIsUpdate');
            }),
            catchError(error => {
                console.error('Error loading permission roles for update check:', error);
                return of(false);
            })
        );
    }

    /**
     * Check if the user can view items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canView(subMenuName: string): boolean {
        return this.resolvePermissionFlag(subMenuName, 'blIsNewView', 'blIsview');
    }

    /**
     * Check enabled submenu permission row exists and view access is granted.
     */
    canAccessSubMenu(subMenuName: string): boolean {
        if (!this.isSubMenuPermissionEnabled(subMenuName)) {
            return false;
        }
        return this.canView(subMenuName);
    }

    /**
     * Check enabled submenu permission row exists and update access is granted.
     */
    canEditSubMenu(subMenuName: string): boolean {
        if (!this.isSubMenuPermissionEnabled(subMenuName)) {
            return false;
        }
        return this.canUpdate(subMenuName) || this.canNewUpdate(subMenuName);
    }

    private isSubMenuPermissionEnabled(subMenuName: string): boolean {
        const matching = this.getMatchingPermissions(subMenuName);
        return matching.some((permission) => {
            const isDeleted = permission?.blIsDeleted === true;
            const isEnabled = permission?.blIsEnabled === true;
            const isActive = permission?.blIsActive === true;
            const isStatus = permission?.blnStatus === true;
            return !isDeleted && isEnabled && (isActive || isStatus);
        });
    }

    /**
     * Check if the user can delete items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canDelete(subMenuName: string): boolean {
        return this.resolvePermissionFlag(subMenuName, 'blIsDelete');
    }

    /**
     * Check if the user can approve items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canApprove(subMenuName: string): boolean {
        return this.resolvePermissionFlag(subMenuName, 'blIsApprove');
    }

    /**
     * Check if the user can create new items (new schema) for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canNewCreate(subMenuName: string): boolean {
        return this.resolvePermissionFlag(subMenuName, 'blIsNewCreate', 'blIsAdd');
    }

    /**
     * Check if the user can update items (new schema) for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canNewUpdate(subMenuName: string): boolean {
        return this.resolvePermissionFlag(subMenuName, 'blIsNewUpdate', 'blIsUpdate');
    }

    /**
     * Check if the user can view items (new schema) for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canNewView(subMenuName: string): boolean {
        return this.resolvePermissionFlag(subMenuName, 'blIsNewView', 'blIsview');
    }

    /**
     * Check if the user can add items for any submenu under a module
     * @param module The module name
     * @returns boolean
     */
    canAddForModule(module: string): boolean {
        const subMenus = this.moduleSubMenuMap[module] || [];
        return subMenus.some(subMenu => this.canAdd(subMenu));
    }

    /**
     * Check if the user can update items for any submenu under a module
     * @param module The module name
     * @returns boolean
     */
    canUpdateForModule(module: string): boolean {
        const subMenus = this.moduleSubMenuMap[module] || [];
        return subMenus.some(subMenu => this.canUpdate(subMenu));
    }

    /**
     * Check if the user can view items for any submenu under a module
     * @param module The module name
     * @returns boolean
     */
    canViewForModule(module: string): boolean {
        const subMenus = this.moduleSubMenuMap[module] || [];
        return subMenus.some(subMenu => this.canView(subMenu));
    }

    /**
     * Check if the user can delete items for any submenu under a module
     * @param module The module name
     * @returns boolean
     */
    canDeleteForModule(module: string): boolean {
        const subMenus = this.moduleSubMenuMap[module] || [];
        return subMenus.some(subMenu => this.canDelete(subMenu));
    }

    /**
     * Clear all permissions (e.g., on logout)
     */
    clearPermissions(): void {
        this.subMenuRoles = [];
        this.permissionsLoadedSubject.next(false);
    }

    /**
     * Check if permissions have been loaded
     * @returns boolean
     */
    arePermissionsLoaded(): boolean {
        return this.permissionsLoadedSubject.value;
    }






}
