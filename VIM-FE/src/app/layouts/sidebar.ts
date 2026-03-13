import { animate, style, transition, trigger } from '@angular/animations';
import { Component, NgZone, ViewChild } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { slideDownUp } from '../shared/animations';
import { MenuService } from '../layout/menu-service/menu.service';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import { catchError, Observable, of, switchMap, tap } from "rxjs";

@Component({
    moduleId: module.id,
    selector: 'sidebar',
    templateUrl: './sidebar.html',
    animations: [slideDownUp],
})
export class SidebarComponent {
    active = false;
    store: any;
    activeDropdown: string[] = [];
    parentDropdown: string = '';
    menus: any;
    user: any;
    subMenuRoles: any[] = [];
    isDashboardPresent!: boolean;
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
        this.getMenus();
        const permission = this.subMenuRoles.find(
            role => role.cfgTblSubMenu?.txtSubMenuName === subMenuName
        );
        return permission?.blIsNewUpdate ?? false;
    }

    canView(subMenuName: string): boolean {
        this.getMenus();
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

        if (this.menus && this.menus.length) return;

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

        this.loadPermissionRoles(roleId, user.serUserId).pipe(
            switchMap(() => {
                return this.menuService.getUserMenus();
            })
        ).subscribe((response: any) => {
            if (response) {
                let allMenus = response;
                allMenus.forEach((menu: any) => {
                    if (this.isSubMenuExist(menu.subMenus)) {
                        menu.subMenus = menu.subMenus.filter((sm: any) => {
                            const isPermitted = this.subMenuRoles.some((role: any) =>
                                Number(role?.cfgTblSubMenu?.serSubMenuId) === Number(sm?.subMenuId ?? sm?.serSubMenuId) &&
                                this.isSubMenuEnabled(role)
                            );

                            // Force show 'Pending Approvals' for HODs and Procurement
                            if (this.isPendingApprovalsMenu(sm.subMenuName) && (this.isHOD() || this.isProcurementUser())) {
                                return true;
                            }

                            return isPermitted;
                        });
                        menu.subMenus.sort((a: any, b: any) => a.submenuOrder - b.submenuOrder);
                        menu.canViewSubmenu = menu.subMenus.some((sm: any) => this.canViewMenu(sm));
                    } else {
                        menu.canViewSubmenu = false;
                    }
                });

                this.menus = allMenus;
                debugger;
                this.isDashboardPresent = this.menus.some((menu: { subMenus: string | any[]; }) => menu.subMenus && menu.subMenus.length > 0);

                this.ngZone.run(() => {
                    if (!this.isDashboardPresent) {
                        console.log("Redirecting to signin...");
                        /* this.router.navigateByUrl('auth/signin');*/
                        this.router.navigateByUrl('auth/signin?error=You%20do%20not%20have%20access%20to%20the%20application.%20Please%20contact%20the%20administrator%20for%20further%20assistance.');
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

    private isSubMenuEnabled(role: any): boolean {
        if (!role) return false;
        const enabled = role?.blIsEnabled;
        if (enabled === true || enabled === 1 || enabled === 'true') return true;
        // Fallback for payloads that don't send blIsEnabled reliably.
        return role?.blnStatus === true || role?.blIsActive === true;
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
        return name || '';
    }

}
