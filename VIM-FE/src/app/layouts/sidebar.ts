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
                }
        });
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

        const userJson = localStorage.getItem('user');
        let user: {
            cfgTblRole: number | undefined;
            serUserId: number;
        };

        if (userJson) {
            // @ts-ignore
            user = JSON.parse(userJson) as CfgTblUser;
        }

        // @ts-ignore
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
                                role.cfgTblSubMenu.serSubMenuId === sm.subMenuId && role.blIsEnabled === true
                            )
                        );
                        menu.subMenus.sort((a: any, b: any) => a.submenuOrder - b.submenuOrder);
                        menu.canViewSubmenu = menu.subMenus.some((sm: any) => this.canViewMenu(sm.roles));
                    } else {
                        menu.canViewSubmenu = false;
                    }
                });

                this.menus = allMenus;
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
        return Object.keys(subMenus).length ? true : false;
    }

    /*canViewMenu(roles: any) {
        const userRole = 'ROLE_ADMIN'
        const rolesArr = roles.split(',');
        return rolesArr.includes(userRole);
    }*/

    canViewMenu(roles: string): boolean {
        const userRole = 'ROLE_' + this.user.txtrole;
        if (!roles) {
            return true;
        }
        const requiredRoles = roles.split(',').map(role => role.trim());
        return requiredRoles.some(role => role === userRole);
    }

}
