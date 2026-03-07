import { animate, style, transition, trigger } from '@angular/animations';
import {Component, NgZone, ViewChild} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { slideDownUp } from '../shared/animations';
import { MenuService } from '../layout/menu-service/menu.service';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import {catchError, Observable, of, switchMap, tap} from "rxjs";

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
        if (!user?.cfgTblRole?.serRoleId || !user?.serUserId) {
            console.warn('Sidebar menus not loaded: missing user role/userId');
            this.menus = [];
            this.isDashboardPresent = false;
            return;
        }

        this.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).pipe(
            switchMap(() => {
                return this.menuService.getUserMenus();
            })
        ).subscribe((response: any) => {
            if (response) {
                let allMenus = response;
                allMenus.forEach((menu: any) => {
                    if (this.isSubMenuExist(menu.subMenus)) {
                        menu.subMenus = menu.subMenus.filter((sm: any) =>
                            this.subMenuRoles.some((role: any) =>
                                Number(role?.cfgTblSubMenu?.serSubMenuId) === Number(sm?.subMenuId ?? sm?.serSubMenuId) &&
                                this.isSubMenuEnabled(role)
                            )
                        );
                        menu.subMenus.sort((a: any, b: any) => a.submenuOrder - b.submenuOrder);
                        menu.canViewSubmenu = menu.subMenus.some((sm: any) => this.canViewMenu(sm.roles));
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

    canViewMenu(roles: string): boolean {
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
        const requiredRoles = roles.split(',').map(role => role.trim());
        return requiredRoles.some(role => role === userRole);
    }

    private isSubMenuEnabled(role: any): boolean {
        if (!role) return false;
        const enabled = role?.blIsEnabled;
        if (enabled === true || enabled === 1 || enabled === 'true') return true;
        // Fallback for payloads that don't send blIsEnabled reliably.
        return role?.blnStatus === true || role?.blIsActive === true;
    }

    getDisplaySubMenuName(name: string): string {
        const label = (name || '').trim().toLowerCase();
        if (label === 'budget approval') {
            return 'Document Builder';
        }
        return name || '';
    }

}
