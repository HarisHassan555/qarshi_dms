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
            'Form Builder',
            'Budget Approval'
        ],
        'User Management': ['User', 'Password Policy', 'Change Password', 'Permission']
    };

    constructor(private menuService: MenuService) {}


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

        if (!user || !user.cfgTblRole) {
            return of(false);
        }

        // @ts-ignore
        return this.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).pipe(
            map(roles => {
                // Validate and update subMenuRoles
                console.log('Roles received:', roles);
                this.subMenuRoles = Array.isArray(roles) ? roles : [];
                const permission = this.subMenuRoles.find(
                    role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
                );
                console.log('Permission for', subMenuName, ':', permission);
                return permission?.blIsNewCreate ?? false;
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
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewUpdate ?? false;
    }

    /**
     * Check if the user can view items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canView(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewView ?? false;
    }

    /**
     * Check if the user can delete items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canDelete(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsDelete ?? false;
    }

    /**
     * Check if the user can approve items for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canApprove(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsApprove ?? false;
    }

    /**
     * Check if the user can create new items (new schema) for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canNewCreate(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewCreate ?? false;
    }

    /**
     * Check if the user can update items (new schema) for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canNewUpdate(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewUpdate ?? false;
    }

    /**
     * Check if the user can view items (new schema) for a specific submenu
     * @param subMenuName The name of the submenu
     * @returns boolean
     */
    canNewView(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewView ?? false;
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
