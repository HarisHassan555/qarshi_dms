import { animate, style, transition, trigger } from '@angular/animations';
import { Component, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { slideDownUp } from '../shared/animations';
import { MenuService } from '../layout/menu-service/menu.service';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import { catchError, Observable, of, Subject, switchMap, takeUntil, tap } from 'rxjs';

@Component({
    moduleId: module.id,
    selector: 'sidebar',
    templateUrl: './sidebar.html',
    animations: [slideDownUp],
})
export class SidebarComponent implements OnInit, OnDestroy {
    active = false;
    store: any;
    activeDropdown: string[] = [];
    parentDropdown: string = '';
    menus: any;
    user: any;
    subMenuRoles: any[] = [];
    isDashboardPresent!: boolean;

    /** Used to unsubscribe all long-lived subscriptions on destroy. */
    private destroy$ = new Subject<void>();
    constructor(
        private menuService: MenuService,
        public translate: TranslateService,
        public storeData: Store<any>,
        public router: Router,
        private sharedDataService: SharedDataService,
        private ngZone: NgZone
    ) {
        this.initStore();
    }
    async initStore() {
        this.storeData
            .select((d) => d.index)
            .subscribe((d) => {
                this.store = d;
            });
    }

    ngOnInit() {
        this.getUser();
        this.setActiveDropdown();

        // Re-fetch menus whenever a role/permission change is broadcast from RolesManagementComponent.
        this.sharedDataService.getRefreshMenu()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                console.log('Sidebar: refreshMenu signal received — reloading menus.');
                this.getMenus();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next();
        this.destroy$.complete();
    }

    getUser() {
        this.sharedDataService.getUser()
            .subscribe(data => {
                if (data) {
                    console.log('User', data);
                    this.user = data;
                    this.getMenus();
                } else {
                    this.user = this.getUserFromLocalStorage();
                    this.getMenus();
                }
            });
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

    loadPermissionRoles(roleId: number | undefined, userId: number): Observable<any> {
        return this.menuService.getAllSubMenuRoles(roleId, userId).pipe(
            tap((data) => {
                this.subMenuRoles = data;
                console.log("Loaded subMenuRoles:", this.subMenuRoles);
            }),
            catchError((error: any) => {
                console.error('Error fetching submenu roles:', error);
                return of([]);
            })
        );
    }


    canAdd(subMenuName: string): boolean {

        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewCreate ?? false;
    }

    canUpdate(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewUpdate ?? false;
    }

    canView(subMenuName: string): boolean {
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewView ?? false;
    }

    setActiveDropdown() {
        const selector = document.querySelector('.sidebar ul a[routerLink="' + window.location.pathname + '"]');
        if (selector) {
            selector.classList.add('active');
            const ul: any = selector.closest('ul.sub-menu');
            if (ul) {
                let ele: any = ul.closest('li.menu').querySelectorAll('.nav-link') || [];
                if (ele.length) {
                    ele = ele[0];
                    setTimeout(() => {
                        ele.click();
                    });
                }
            }
        }
    }

    toggleMobileMenu() {
        if (window.innerWidth < 1024) {
            this.storeData.dispatch({ type: 'toggleSidebar' });
        }
    }

    toggleAccordion(name: string, parent?: string) {
        if (this.activeDropdown.includes(name)) {
            this.activeDropdown = this.activeDropdown.filter((d) => d !== name);
        } else {
            this.activeDropdown.push(name);
        }
    }

    getMenus() {
        // Always refresh menu state from API to avoid stale in-memory sidebar cache.
        this.menus = [];

        const user = this.getUserFromLocalStorage();
        if (!this.user && user) {
            this.user = user;
        }
        const rawRoleId = user?.cfgTblRole?.serRoleId ?? user?.cfgTblRole;
        if (!rawRoleId || !user?.serUserId) {
            console.warn('Sidebar menus not loaded: missing user role/userId');
            this.menus = [];
            this.isDashboardPresent = false;
            return;
        }

        const roleId = this.getRoleIdFromUser(user);
        if (!roleId) {
            console.warn('Sidebar menus not loaded: missing roleId after fallback resolution', user?.cfgTblRole);
            this.menus = [];
            this.isDashboardPresent = false;
            return;
        }

        // Use userId=0 so the sidebar loads only ROLE-LEVEL permission rows
        // (cfgTblUser IS NULL in DB), exactly matching what Role Management
        // displays. Passing the real userId would also pull old user-specific
        // override rows that no longer reflect the current role configuration.
        this.loadPermissionRoles(roleId, 0).pipe(
            switchMap(() => {
                // Use getAllMenusRaw() so the sidebar sees the complete menu tree —
                // the same source used by Role Management — and filters by subMenuRoles.
                return this.menuService.getAllMenusRaw();
            })
        ).subscribe((response: any) => {
            if (response) {
                let allMenus: any[] = Array.isArray(response) ? response : [];

                // Normalise each menu from the raw API shape (cfgTblSubMenus → subMenus, serSubMenuId → subMenuId)
                allMenus = allMenus
                    .filter((menu: any) => {
                        const isExplicitlyDeleted = menu?.blIsDeleted === true || menu?.blIsDeleted === 1;
                        // Treat null/undefined status flags as 'active' — only block when
                        // BOTH flags are explicitly false (never set can mean newly seeded rows).
                        const isExplicitlyInactive = menu?.blIsActive === false && menu?.blnStatus === false;
                        return !isExplicitlyDeleted && !isExplicitlyInactive;
                    })
                    .map((menu: any) => {
                        // Support both raw entity (cfgTblSubMenus / serSubMenuId) and
                        // projected DTO (subMenus / subMenuId) property names.
                        const rawSubs: any[] = menu?.cfgTblSubMenus || menu?.subMenus || [];
                        const normSubs: any[] = rawSubs
                            .filter((sm: any) => {
                                const isExplicitlyDeleted = sm?.blIsDeleted === true || sm?.blIsDeleted === 1;
                                // Same permissive rule: null status = treat as active.
                                const isExplicitlyInactive = sm?.blIsActive === false && sm?.blnStatus === false;
                                return !isExplicitlyDeleted && !isExplicitlyInactive;
                            })
                            .map((sm: any) => ({
                                ...sm,
                                // Ensure both id aliases are always present so downstream code
                                // can use either without extra guards.
                                subMenuId:    Number(sm?.serSubMenuId   || sm?.subMenuId   || 0),
                                serSubMenuId: Number(sm?.serSubMenuId   || sm?.subMenuId   || 0),
                                subMenuName:  sm?.txtSubMenuName || sm?.subMenuName || '',
                                subMenuAction: sm?.txtSubMenuUrl || sm?.subMenuAction || '',
                                submenuOrder:  Number(sm?.intSubMenuOrder || sm?.submenuOrder || 0),
                                roles: sm?.roles || ''
                            }))
                            .filter((sm: any) => sm.subMenuId > 0)
                            .sort((a: any, b: any) => a.submenuOrder - b.submenuOrder);

                        return {
                            ...menu,
                            menuId:   Number(menu?.serMenuId || menu?.menuId || 0),
                            menuName: menu?.txtMenuName || menu?.menuName || '',
                            subMenus: normSubs
                        };
                    })
                    .filter((menu: any) => menu.menuId > 0 && menu.menuName);

                allMenus.forEach((menu: any) => {
                    if (this.isSubMenuExist(menu.subMenus)) {
                        menu.subMenus = menu.subMenus.filter((sm: any) => {
                            // Match against subMenuRoles using both id aliases for robustness.
                            const smId = Number(sm?.serSubMenuId || sm?.subMenuId || 0);
                            const matchedPermissionRows = this.subMenuRoles.filter((role: any) =>
                                Number(role?.cfgTblSubMenu?.serSubMenuId || role?.cfgTblSubMenu?.subMenuId || 0) === smId
                            );
                            const hasPermissionRows    = matchedPermissionRows.length > 0;
                            const hasEnabledPermission = matchedPermissionRows.some((role: any) =>
                                this.isSubMenuEnabled(role)
                            );

                            // Special case: always show Pending Approvals for HODs / Procurement.
                            if (this.isPendingApprovalsMenu(sm.subMenuName) && (this.isHOD() || this.isProcurementUser())) {
                                return true;
                            }

                            // getAllMenusRaw() returns raw entities — sm.roles is NEVER populated,
                            // so canViewMenu() would always return true as a fallback, showing
                            // every active tab to every user.
                            // Rule: a submenu is only visible when the DB carries an ACTIVE,
                            // non-deleted permission row for this role. No row → no access.
                            return hasPermissionRows && hasEnabledPermission;
                        });

                        // Ensure admin always sees Role Management under User Management.
                        if (this.isAdminUser() && (menu.menuName || '').trim().toLowerCase() === 'user management') {
                            const hasRoleManagement = menu.subMenus.some((sm: any) => {
                                const name   = (sm?.subMenuName   || sm?.txtSubMenuName || '').toString().trim().toLowerCase();
                                const action = (sm?.subMenuAction || sm?.txtSubMenuUrl  || '').toString().trim().toLowerCase();
                                return name === 'role management' || action === 'roles' || action === '/roles';
                            });

                            if (!hasRoleManagement) {
                                menu.subMenus.push({
                                    subMenuId: -9999,
                                    serSubMenuId: -9999,
                                    subMenuName: 'Role Management',
                                    subMenuAction: 'roles',
                                    roles: 'ROLE_ADMIN',
                                    submenuOrder: 9999
                                });
                            }
                        }

                        menu.subMenus.sort((a: any, b: any) => (a?.submenuOrder ?? 0) - (b?.submenuOrder ?? 0));
                        menu.canViewSubmenu = menu.subMenus.length > 0;
                    } else {
                        menu.canViewSubmenu = false;
                    }
                });

                this.menus = allMenus;
                this.isDashboardPresent = this.menus.some(
                    (menu: any) => menu.subMenus && menu.subMenus.length > 0
                );

                this.ngZone.run(() => {
                    if (!this.isDashboardPresent) {
                        console.log('Sidebar: no accessible menus — redirecting to signin.');
                        this.router.navigateByUrl(
                            'auth/signin?error=You%20do%20not%20have%20access%20to%20the%20application.%20Please%20contact%20the%20administrator%20for%20further%20assistance.'
                        );
                        localStorage.removeItem('token');
                        localStorage.removeItem('user');
                    }
                });
            }
        });
    }





    /*getMenus() {
      if (this.menus && this.menus.length) return;
        const userJson = localStorage.getItem('user');
        // @ts-ignore
        let user: {
            cfgTblRole: number | undefined;
            serUserId: number; };
        if (userJson) {
            // @ts-ignore
            user = JSON.parse(userJson) as CfgTblUser;
        }
     // @ts-ignore
     this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);
      this.menuService
        .getUserMenus()
        .subscribe((response: any) => {
          if (response) {
              let allMenus = response;
              allMenus.forEach((menu: any) => {

                  if (this.isSubMenuExist(menu.subMenus)) {

                      debugger;
                      menu.subMenus = menu.subMenus.filter((sm: any) =>
                          this.subMenuRoles.some((role: any) =>
                              role.cfgTblSubMenu.serSubMenuId === sm.serSubMenuId
                          )
                      );


                       debugger;
                       menu.subMenus.sort((a: any,b: any) => a.submenuOrder - b.submenuOrder); // asc
                       menu.canViewSubmenu = menu.subMenus.some((sm: any) => this.canViewMenu(sm.roles));
                  } else {
                      menu.canViewSubmenu = false;
                  }
              });
            this.menus = allMenus;
          }
        });
    }*/

    isSubMenuExist(subMenus: any) {
        return !!subMenus && Object.keys(subMenus).length > 0;
    }

    /*canViewMenu(roles: any) {
        const userRole = 'ROLE_ADMIN'
        const rolesArr = roles.split(',');
        return rolesArr.includes(userRole);
    }*/

    canViewMenu(sm: any): boolean {
        // Force show 'Pending Approvals' for HODs and Procurement
        if (this.isPendingApprovalsMenu(sm.subMenuName) && (this.isHOD() || this.isProcurementUser())) {
            return true;
        }

        const roles = sm.roles;
        const roleName =
            this.user?.txtrole ||
            this.user?.cfgTblRole?.txtRoleName ||
            this.user?.cfgTblRole?.txtrole ||
            '';
        const userRole = roleName ? ('ROLE_' + String(roleName).trim().toUpperCase()) : '';
        if (!roles) {
            return true;
        }
        if (!userRole) {
            return true;
        }
        const requiredRoles = roles.split(',').map((role: string) => role.trim());
        return requiredRoles.some((role: string) => role === userRole);
    }

    isHOD(): boolean {
        if (!this.user) return false;

        // Check if user is the head of their department
        if (this.user.hrTblDepartment && this.user.hrTblDepartment.serDepartmentHeadId) {
            const headIds = String(this.user.hrTblDepartment.serDepartmentHeadId)
                .split(',')
                .map(id => id.trim());

            if (headIds.includes(String(this.user.serUserId))) {
                return true;
            }
        }

        // Fallback or secondary check (some users might have 'HOD' in their designation or role)
        const role = (this.user?.cfgTblRole?.txtRoleName || this.user?.txtrole || '').toUpperCase();
        const designation = (this.user?.txtDesignation || '').toUpperCase();

        return role.includes('HOD') || designation.includes('HOD') || role.includes('HEAD');
    }

    isProcurementUser(): boolean {
        if (!this.user) return false;

        const deptName = this.user.hrTblDepartment?.txtDepartmentName ||
            this.user.departmentName ||
            this.user.txtDepartmentName || '';

        const deptCode = this.user.hrTblDepartment?.txtDepartmentCode ||
            this.user.departmentCode || '';

        const roleName = this.user.cfgTblRole?.txtRoleName ||
            this.user.txtrole || '';

        const name = deptName.trim().toUpperCase();
        const code = deptCode.trim().toUpperCase();
        const role = roleName.trim().toUpperCase();

        return name.includes('PROCUREMENT') || name === 'PRC' ||
            code === 'PRC' || code.includes('PROC') ||
            role.includes('PROCURE');
    }

    isPendingApprovalsMenu(name: string): boolean {
        const label = (name || '').trim().toLowerCase();
        return label === 'pending approvals' || label === 'pending-approvals';
    }

    private isAdminUser(): boolean {
        const roleName = (
            this.user?.cfgTblRole?.txtRoleName ||
            this.user?.txtrole ||
            this.user?.roleName ||
            ''
        ).toString().trim().toUpperCase();
        return roleName === 'ADMIN' || roleName === 'ROLE_ADMIN' || roleName === 'SUPER ADMIN' || roleName === 'ROLE_SUPER ADMIN';
    }

    private isSubMenuEnabled(role: any): boolean {
        if (!role) return false;
        const toBool = (v: any) => v === true || v === 1 || v === '1' || v === 'true';

        // The permission row must not be soft-deleted.
        if (toBool(role?.blIsDeleted)) return false;

        // At least one positive flag on the permission ROW must be set.
        // We intentionally do NOT check the nested cfgTblSubMenu entity's blnStatus /
        // blIsActive here — those flags may be null in the JSON response when the
        // sub-entity was DB-seeded without explicit values.  Submenu liveness is
        // already guaranteed by the normalisation filter at the top of getMenus().
        return toBool(role?.blIsEnabled) || toBool(role?.blnStatus) || toBool(role?.blIsActive);
    }

    /**
     * Extract a numeric role id from user object, tolerating payloads where cfgTblRole
     * is either an object ({ serRoleId }) or just a numeric id.
     */
    private getRoleIdFromUser(user: any): number | null {
        if (!user) return null;
        const candidate =
            user?.cfgTblRole?.serRoleId ??
            user?.cfgTblRole ??
            user?.cfgTblRoleId ??
            user?.roleId;
        const num = Number(candidate);
        if (Number.isFinite(num) && num > 0) {
            return num;
        }

        // Fallback: derive from role name when payload only sends txtrole/txtRoleName
        const roleName = (user?.cfgTblRole?.txtRoleName || user?.txtrole || '').toString().toUpperCase().trim();
        if (!roleName) return null;
        const roleMap: { [key: string]: number } = {
            'ADMIN': 1,
            'VENDOR': 2,
            'MARKETING': 3,
            'PROCURE': 4,
            'PROCUREMENT': 4,
            'FINANCE': 5,
            'AUDIT': 6,
            'CEO': 7
        };
        return roleMap[roleName] || null;
    }

    getDisplaySubMenuName(name: string): string {
        const label = (name || '').trim().toLowerCase();
        if (label === 'budget approval') {
            return 'Document Builder';
        }
        if (label === 'template builder') {
            return 'Digital Document Builder';
        }
        if (label === 'template list' || label === 'template forms') {
            return 'Digital Document List';
        }
        if (label === 'template pending approvals') {
            return 'Digital Pending Approvals';
        }
        if (label === 'my applications') {
            return 'My Digital Applications';
        }
        if (label === 'department application' || label === 'department applications') {
            return 'My Department Digital Application';
        }
        return name || '';
    }

}
