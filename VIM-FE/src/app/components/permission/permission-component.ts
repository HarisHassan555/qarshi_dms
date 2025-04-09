import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CustomerService } from 'src/app/services/customer/customer.service';
import {UserService} from "../../services/user/user.service";
import {MenuService} from "../../layout/menu-service/menu.service";
import {Menu} from "@angular/cdk/menu";
import {catchError, map, Observable, of} from "rxjs";


export interface UserPermission {
    id?: number;
    user?: number;
    submenu: number;
    canView: boolean;
    canAdd: boolean;
    canEdit: boolean;
}


export interface RawMenuData {
    menuName: string;
    menuIcon: string;
    subMenuId: number;
    subMenuName: string;
    subMenuAction: string;
    roles: string;
    canView: boolean;
    canEdit: boolean;
    canAdd?: boolean;
    canDelete?: boolean;
}

interface CfgTblSubMenuRole {
    serSubMenuRoleId: number;
    blIsActive: boolean;
    blnStatus: boolean;
    dteCreatedDate: Date;
    dteModifiedDate: Date;
    serCreatedUser: number;
    serModifiedUser: number;
    cfgTblRole: any;
    cfgTblSubMenu: any;
    cfgTblUser: any;
    blIsDeleted: boolean;
    blIsEnabled:boolean;
    blIsview: boolean;
    blIsAdd: boolean;
    blIsDelete: boolean;
    blIsUpdate: boolean;
    blIsApprove: boolean;
    blIsAll: boolean;
}

interface MenuPayload {
    menuName: string;
    menuIcon: string;
    subMenus: SubMenuPayload[];
}

interface SubMenuPayload {
    canAdd: boolean;
    canDelete: boolean;
}

@Component({
    selector: 'app-permission',
    templateUrl: './permission.component.html',
    styleUrls: ['./permission.component.css']
})
export class PermissionComponent implements OnInit {
    @ViewChild('datatable') datatable: any;
    @ViewChild('modal') modal: any;
    search = '';
    form!: FormGroup;
    isSubmit = false;
    cities: any;
    countries: any;
    filteredCities: any;
    customers: any;
    blnIsFiler = false;
    blnStatus = false;
    users: any;
    filteredUsers:  any;
    roles:  any;
    menus:  any;
    private userRole: any;
    private selectedRoleText: any;
    selectedRoleId: number | undefined;
    private selectedUserText: any;
    selectedUserId: number | undefined;
    private subMenuRoles: CfgTblSubMenuRole[] | undefined;
    cols = [
        { field: 'serCustomerId', title: 'Sr. No' },
        { field: 'txtCustomerCode', title: 'Media House Code' },
        { field: 'txtCustomerName', title: 'Media House Name' },
        { field: 'txtCnicNo', title: 'CNIC' },
        { field: 'txtNtnNo', title: 'NTN' },
        { field: 'txtSapNo', title: 'SAP ID' },
        { field: 'txtSTR', title: 'STRN' },
        { field: 'txtEmailAddress', title: 'Email' },
        { field: 'cfgTblCountry.txtName', title: 'Country' },
        { field: 'cfgTblCity.txtCityName', title: 'City' },
        { field: 'blnIsFiler', title: 'Filer' },
        { field: 'txtBillingAddress', title: 'Billing Address' },
        { field: 'txtShippingAddress', title: 'Shipping Address' },
        { field: 'blnStatus', title: 'Status' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];
     mainMenu: any[] | undefined;


    constructor(
        private fb: FormBuilder,
        private customerService: CustomerService,
        private notificationService: NotificationService,
        private userService: UserService, private menuService : MenuService
    ) { }

    ngOnInit(): void {
        this.getRoles();
        this.getUsers();
    }

    getRoles() {
        this.userService.getRoles()
            .subscribe(data => {
                if (data) {
                    this.roles = data;
                }
            });
    }

    getUsers() {
        this.users = [];
        this.userService.getUsers()
            .subscribe(data => {
                this.users = data;
                this.users = this.users.map((user: { cfgTblRole: null; }) => {
                    if (user.cfgTblRole) {
                        const filteredRole = this.roles.find((role: { serRoleId: null; }) => role.serRoleId === user.cfgTblRole);
                        user.cfgTblRole = filteredRole || null;
                    } else {
                        user.cfgTblRole = null;
                    }

                    return user;
                });

                console.log(this.users);
            });
    }

    onRoleChange(event: Event): void {
        this.selectedRoleId = Number((event.target as HTMLSelectElement).value);
        this.selectedRoleText = (event.target as HTMLSelectElement).options[(event.target as HTMLSelectElement).selectedIndex].text;
        // @ts-ignore
        this.filteredUsers = this.users.filter((user: User) => user.cfgTblRole?.serRoleId === this.selectedRoleId);
        /*this.filteredUsers = this.users.filter((user: { cfgTblRole: { id: number; }; }) => user.cfgTblRole?.serRoleId === this.selectedRoleId );*/
    }

    onUserChange(event: Event): void {
        this.selectedUserId = Number((event.target as HTMLSelectElement).value);
        this.selectedUserText = (event.target as HTMLSelectElement).options[(event.target as HTMLSelectElement).selectedIndex].text;
        this.loadSubMenuRoles(this.selectedRoleId,this.selectedUserId);
    }


    loadSubMenuRoles(roleId: number | undefined, userId: number): void {
        this.menuService.getAllSubMenuRoles(roleId, userId).subscribe({
            next: (data) => {
                this.menus = data;

                console.log(this.menus);
                if (this.menus.length > 0) {

                    this.transformMenuData()
                    return;
                }

                this.loadMenus();
            },
            error: (error) => {
                console.error('Error fetching submenu roles:', error);
                this.loadMenus();
            }
        });
    }

    // @ts-ignore
    updatePermission(subMenu: SubMenu, permissionType: 'canView' | 'canAdd' | 'canEdit' | 'canEnabled', checked: boolean): void {

        if (permissionType === 'canView') {
            subMenu.canView = checked;
        } else if (permissionType === 'canAdd') {
            subMenu.canAdd = checked;
        } else if (permissionType === 'canEdit') {
            subMenu.canEdit = checked;
        }else if (permissionType === 'canEnabled') {
            subMenu.canEnabled = checked;
        }
    }

    savePermissions(): void {

        let menu;
        menu = this.convertMenusToJson(this.menus);
        debugger;
        // @ts-ignore
        this.menuService.saveSubMenuRole(this.selectedUserId,this.selectedRoleId,menu).subscribe(
            response => {
                let data = typeof response === 'string' ? JSON.parse(response) : response;
                if (data && data.status === 'Success') {

                    console.log('Permissions saved successfully:', response);
                    this.notificationService.showMessage("Permissions saved successfully",'success')

                } else  {
                    /*console.error('Error saving permissions:', error);*/
                    this.notificationService.showMessage("Error saving permissions",'danger')
                }
            }

        );
    }

    loadMenus(): void {
        // @ts-ignore
        this.menuService.getUserMenus().subscribe(
            (menus) => {
                console.log('Fetched menus:', menus);
                if (Array.isArray(menus)) {
                    this.menus = menus.map(menu => {
                        const authorizedSubMenus = menu.subMenus.filter((subMenu: { roles: string; }) => this.isAuthorized(subMenu.roles));

                        return {
                            ...menu,
                            subMenus: authorizedSubMenus
                        };
                    })/*.filter(menu => menu.subMenus.length > 0)*/;

                    console.log("Total menus after filtering:", this.menus);
                } else {
                    console.error('Expected an array but received:', menus);
                }
            },
            error => {
                console.error('Error fetching user menus:', error);
            }
        );

    }

    isAuthorized(roles: string): boolean {

        // @ts-ignore
        this.userService.me().subscribe((data: CfgTblUser | null) => {
            if (data) {
                this.userRole = data.cfgTblRole.txtRoleName; // "MARKETING"
                console.log("ROLE_"+this.selectedRoleText);

            } else {
                console.error('User data is null');
            }
        });
        // @ts-ignore
        this.userRole = 'ROLE_' + this.selectedRoleText;
        if (!this.userRole) {
            return true;
        }
        const requiredRoles = roles.split(',').map(role => role.trim());
        return requiredRoles.some(role => role === this.userRole);
    }



    getAllMainMenu(id: number): Observable<string> {
        // @ts-ignore
        return this.menuService.getMenuBySubMenu(id).pipe(
            map((menus: any) => {
                // Ensure that the response is an array
                if (Array.isArray(menus) && menus.length > 0) {
                    return menus[0].txtMenuName || '';
                }
                return '';
            }),
            catchError(error => {
                console.error('Error fetching menu name:', error);
                return of('');
            })
        );
    }


    convertMenusToJson(menus: any[]): {
        cfgTblUser: { serUserId: number | null; username: string };
        blIsAdd: boolean;
        blIsAll: boolean;
        blIsEnabled:boolean;
        blnStatus: boolean;
        dteCreatedDate: string;
        blIsActive: boolean;
        blIsview: boolean;
        dteModifiedDate: string;
        serSubMenuRoleId: number | null;
        blIsDeleted: boolean;
        cfgTblSubMenu: { txtSubMenuName: string; serSubMenuId: number | null; txtSubMenuUrl: string };
        cfgTblRole: { txtRoleName: string; serRoleId: number | null };
        serCreatedUser: number | null;
        blIsDelete: boolean;
        serModifiedUser: number | null;
        blIsUpdate: boolean;
        blIsApprove: boolean;
    }[] {

        return menus.flatMap(menu => {
            if (menu.subMenus && menu.subMenus.length > 0) {
                // @ts-ignore
                return menu.subMenus.map((subMenu: {
                    canEnabled:boolean;
                    canView: boolean;
                    canAdd: boolean;
                    canEdit: boolean; serSubMenuRoleId: any; blIsActive: any; blnStatus: any; dteCreatedDate: { toISOString: () => any; }; dteModifiedDate: { toISOString: () => any; }; serCreatedUser: any; serModifiedUser: any; subMenuId: any; subMenuName: any; subMenuAction: any; blIsDeleted: any; blIsview: any; blIsAdd: any; blIsDelete: any; blIsUpdate: any; blIsApprove: any; blIsAll: any;
                }) => ({
                    serSubMenuRoleId: subMenu.serSubMenuRoleId ?? null,
                    blIsActive: subMenu.blIsActive ?? false,
                    blnStatus: subMenu.blnStatus ?? false,
                    dteCreatedDate: subMenu.dteCreatedDate ? subMenu.dteCreatedDate.toISOString() : new Date().toISOString(),
                    dteModifiedDate: subMenu.dteModifiedDate ? subMenu.dteModifiedDate.toISOString() : new Date().toISOString(),
                    serCreatedUser: subMenu.serCreatedUser ?? null,
                    serModifiedUser: subMenu.serModifiedUser ?? null,
                    cfgTblRole: {
                        serRoleId: this.selectedRoleId ?? null,
                        txtRoleName: this.selectedRoleText ?? "",
                    },
                    cfgTblSubMenu: {
                        serSubMenuId: subMenu.subMenuId ?? null,
                        txtSubMenuName: subMenu.subMenuName ?? "",
                        txtSubMenuUrl: subMenu.subMenuAction ?? "",
                    },
                    cfgTblUser: {
                        serUserId: this.selectedUserId ?? null,
                        username: this.selectedUserText ?? "",
                    },
                    blIsDeleted: subMenu.blIsDeleted ?? false,
                    blIsEnabled: subMenu.canEnabled ?? false,
                    blIsview: subMenu.canView ?? false,
                    blIsAdd: subMenu.canAdd ?? false,
                    blIsDelete: subMenu.blIsDelete ?? false,
                    blIsUpdate: subMenu.canEdit ?? false,
                    blIsApprove: subMenu.blIsApprove ?? false,
                    blIsAll: subMenu.blIsAll ?? false,
                }));
            }
            return [];
        });
    }


    transformMenuData(): void {
        this.menus = this.menus.map((menu: {
            blIsEnabled:boolean;
            blIsview: boolean;
            blIsAdd: boolean;
            blIsUpdate: boolean;
            serSubMenuRoleId: any;
            cfgTblSubMenu: {
                txtSubMenuName: string;
                cfgTblMenu: any;
                serSubMenuId: any;
                blIsview: boolean;
                blIsAdd: boolean;
                blIsUpdate: boolean;
            }; subMenus: any[]; }) => {
            return {
                menuName: "VIM",
                subMenus: [{
                    serSubMenuRoleId:menu.serSubMenuRoleId,
                    subMenuId: menu.cfgTblSubMenu.serSubMenuId,
                    subMenuName: menu.cfgTblSubMenu.txtSubMenuName ?? '',
                    canEnabled: menu.blIsEnabled ?? false,
                    canView: menu.blIsview ?? false,
                    canAdd: menu.blIsAdd ?? false,
                    canEdit: menu.blIsUpdate ?? false,
                }]
            };
        });
        console.log("Final Menu Display", this.menus);
    }



}
