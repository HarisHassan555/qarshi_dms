import {Component, OnInit, ViewChild} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, Validators} from '@angular/forms';
import {NotificationService} from 'src/app/NotificationService';
import {CustomerService} from 'src/app/services/customer/customer.service';
import {UserService} from "../../services/user/user.service";
import {MenuService} from "../../layout/menu-service/menu.service";
import {catchError, map, Observable, of} from "rxjs";
import {PermissionService} from "../../services/shared-data/permission-service";

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
    blIsEnabled: boolean;
    blIsview: boolean;
    blIsAdd: boolean;
    blIsDelete: boolean;
    blIsUpdate: boolean;
    blIsApprove: boolean;
    blIsAll: boolean;
    blIsNewCreate?: boolean;
    blIsNewView?: boolean;
    blIsNewUpdate?: boolean;
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

interface TransformedMenu {
    menuName: string;
    subMenus: {
        serSubMenuRoleId: any;
        subMenuId: any;
        subMenuName: string;
        canEnabled: boolean;
        canView: boolean;
        canAdd: boolean;
        canEdit: boolean;
        newCanCreate: boolean;
        newCanView: boolean;
        newCanUpdate: boolean;
    }[];
}

@Component({
    selector: 'app-permission',
    templateUrl: './permission.component.html',
    styleUrls: ['./permission.component.css']
})
export class PermissionComponent implements OnInit {
    @ViewChild('datatable') datatable: any;
    @ViewChild('modal') modal: any;
    search: string = '';
    form!: FormGroup;
    isSubmit = false;
    cities: any;
    countries: any;
    filteredCities: any;
    customers: any;
    blnIsFiler = false;
    blnStatus = false;
    users: any;
    filteredUsers: any;
    roles: any;
    menus: any;
    private userRole: any;
    private selectedRoleText: any;
    selectedRoleId: number | undefined;
    private selectedUserText: any;
    selectedUserId: number | undefined;
    private subMenuRoles: CfgTblSubMenuRole[] | undefined;
    transformedMenus: TransformedMenu[] = [];
    originalMenus: TransformedMenu[] = [];
    cols = [
        {field: 'serCustomerId', title: 'Sr. No'},
        {field: 'txtCustomerCode', title: 'Media House Code'},
        {field: 'txtCustomerName', title: 'Media House Name'},
        {field: 'txtCnicNo', title: 'CNIC'},
        {field: 'txtNtnNo', title: 'NTN'},
        {field: 'txtSapNo', title: 'SAP ID'},
        {field: 'txtSTR', title: 'STRN'},
        {field: 'txtEmailAddress', title: 'Email'},
        {field: 'cfgTblCountry.txtName', title: 'Country'},
        {field: 'cfgTblCity.txtCityName', title: 'City'},
        {field: 'blnIsFiler', title: 'Filer'},
        {field: 'txtBillingAddress', title: 'Billing Address'},
        {field: 'txtShippingAddress', title: 'Shipping Address'},
        {field: 'blnStatus', title: 'Status'},
        {field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center'},
    ];
    mainMenu: any[] | undefined;

    private frontendMenuStructure: { [key: string]: string[] } = {
        'Dashboard': [],
        'Master Data': ['Country', 'City', 'Media House', 'Product Category', 'Product', 'Tax Category', 'Department'],
        'VIM': ['Service Order View', 'Invoice', 'Invoice View', 'Marketing Approval', 'Procurement Approval', 'Tax Approval', 'Finance Approval', 'Audit Approval', 'Tax and Audit Logs', 'Scheduler', 'Vendor Invoice View'],
        'User Management': ['User', 'Password Policy', 'Change Password', 'Permission']
    };

    constructor(
        private fb: FormBuilder,
        private customerService: CustomerService,
        private notificationService: NotificationService,
        private userService: UserService,
        private menuService: MenuService,private permissionService: PermissionService
        /*
        private sidebarComponent : SidebarComponent*/
    ) {
    }

    ngOnInit(): void {
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
        this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {

            this.getRoles();
            this.getUsers();
        });

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
        this.filteredUsers = this.users.filter((user: any) => user.cfgTblRole?.serRoleId === this.selectedRoleId);
        this.selectedUserId = undefined;
        this.transformedMenus = [];
        this.originalMenus = [];
        this.search = '';
    }

    onUserChange(event: Event): void {
        this.selectedUserId = Number((event.target as HTMLSelectElement).value);
        this.selectedUserText = (event.target as HTMLSelectElement).options[(event.target as HTMLSelectElement).selectedIndex].text;
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
        this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
            // @ts-ignore
            this.permissionService.canAdd('Permission').subscribe(canAdd => {
                if (canAdd == true) {
                    /* this.notificationService.showMessage('You do not have permission to add new countries', 'danger');*/
                    /*this.isSubmit = false;
                    this.countryForm.reset();
                    this.blnStatus = false;
                    this.modal.open();*/
                   /* this.isSubmit = false;
                    this.form.reset();
                    this.blnStatus = false;
                    this.modal.open();*/
                    // @ts-ignore
                    this.loadSubMenuRoles(this.selectedRoleId, this.selectedUserId);
                    return;
                }else{
                    this.notificationService.showMessage('You do not have permission to add permission', 'danger');
                    return;
                }

            });
        });
       // this.loadSubMenuRoles(this.selectedRoleId, this.selectedUserId);
    }

    loadSubMenuRoles(roleId: number | undefined, userId: number): void {
        this.menuService.getAllSubMenuRoles(roleId, userId).subscribe({
            next: (data) => {
                this.menus = data;
                console.log(this.menus);
                if (this.menus.length > 0) {
                    // @ts-ignore
                    this.transformMenuData();
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

    updatePermission(subMenu: any, permissionType: string, checked: boolean): void {

        if (!this.permissionService.canUpdate('permission')) {
            this.notificationService.showMessage('You do not have permission to edit permission', 'danger');
            return;
        }

        subMenu[permissionType] = checked;
        console.log(`Updated ${permissionType} for ${subMenu.subMenuName} to ${checked}`);


        const originalMenu = this.originalMenus.find(menu =>
            menu.subMenus.some((sm: any) => sm.subMenuName === subMenu.subMenuName)
        );
        if (originalMenu) {
            const originalSubMenu = originalMenu.subMenus.find((sm: any) => sm.subMenuName === subMenu.subMenuName);
            if (originalSubMenu) {
                // @ts-ignore
                originalSubMenu[permissionType] = checked;
            }
        }

        console.log('Updated originalMenus:', JSON.parse(JSON.stringify(this.originalMenus)));
    }

    toggleAllPermissions(permission: string, value: boolean): void {
        this.transformedMenus.forEach(menu => {
            menu.subMenus.forEach(subMenu => {
                // @ts-ignore
                subMenu[permission] = value;
                // Update originalMenus
                const originalMenu = this.originalMenus.find(m => m.menuName === menu.menuName);
                if (originalMenu) {
                    const originalSubMenu = originalMenu.subMenus.find((sm: any) => sm.subMenuName === subMenu.subMenuName);
                    if (originalSubMenu) {
                        // @ts-ignore
                        originalSubMenu[permission] = value;
                    }
                }
            });
        });
        console.log(`Set all ${permission} to ${value}`);
        console.log('Updated originalMenus after toggle:', JSON.parse(JSON.stringify(this.originalMenus)));
    }

    savePermissions(): void {
        const menu = this.convertMenusToJson(this.originalMenus);
        console.log('Data to save:', menu);
        // @ts-ignore
        this.menuService.saveSubMenuRole(this.selectedUserId, this.selectedRoleId, menu).subscribe(
            response => {
                let data = typeof response === 'string' ? JSON.parse(response) : response;
                if (data && data.status === 'Success') {
                    console.log('Permissions saved successfully:', response);
                    this.notificationService.showMessage("Permissions saved successfully", 'success');
                   /* this.sidebarComponent.getMenus();*/
                } else {
                    this.notificationService.showMessage("Error saving permissions", 'danger');
                }
            },
            error => {
                console.error('Error saving permissions:', error);
                this.notificationService.showMessage("Error saving permissions", 'danger');
            }
        );
    }

    loadMenus(): void {
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
                    });
                    console.log("Total menus after filtering:", this.menus);
                    // @ts-ignore
                    this.transformMenuData();
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
        this.userRole = 'ROLE_' + this.selectedRoleText;
        if (!this.userRole) {
            return true;
        }
        const requiredRoles = roles.split(',').map(role => role.trim());
        return requiredRoles.some(role => role === this.userRole);
    }

    getAllMainMenu(id: number): Observable<string> {
        return this.menuService.getMenuBySubMenu(id).pipe(
            map((menus: any) => {
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

    convertMenusToJson(menus: TransformedMenu[]): any[] {
        return menus.flatMap(menu => {
            if (menu.subMenus && menu.subMenus.length > 0) {
                return menu.subMenus.map(subMenu => ({
                    serSubMenuRoleId: subMenu.serSubMenuRoleId ?? null,
                    blIsActive: true,
                    blnStatus: true,
                    dteCreatedDate: new Date().toISOString(),
                    dteModifiedDate: new Date().toISOString(),
                    serCreatedUser: this.selectedUserId ?? null,
                    serModifiedUser: this.selectedUserId ?? null,
                    cfgTblRole: {
                        serRoleId: this.selectedRoleId ?? null,
                        txtRoleName: this.selectedRoleText ?? "",
                    },
                    cfgTblSubMenu: {
                        serSubMenuId: subMenu.subMenuId ?? null,
                        txtSubMenuName: subMenu.subMenuName ?? "",
                        txtSubMenuUrl: "",
                    },
                    cfgTblUser: {
                        serUserId: this.selectedUserId ?? null,
                        username: this.selectedUserText ?? "",
                    },
                    blIsDeleted: false,
                    blIsEnabled: subMenu.canEnabled ?? false,
                    blIsview: subMenu.canView ?? false,
                    blIsAdd: subMenu.canAdd ?? false,
                    blIsDelete: false,
                    blIsUpdate: subMenu.canEdit ?? false,
                    blIsApprove: false,
                    blIsAll: false,
                    blIsNewCreate: subMenu.newCanCreate ?? false,
                    blIsNewView: subMenu.newCanView ?? false,
                    blIsNewUpdate: subMenu.newCanUpdate ?? false,
                }));
            }
            return [];
        });
    }

    transformMenuData(): void {
        const menuMap: { [key: string]: TransformedMenu } = {};
        Object.keys(this.frontendMenuStructure).forEach(menuName => {
            menuMap[menuName] = {
                menuName: menuName,
                subMenus: []
            };
        });

        this.menus.forEach((menu: {
            blIsEnabled: boolean;
            blIsview: boolean;
            blIsAdd: boolean;
            blIsUpdate: boolean;
            blIsNewCreate?: boolean;
            blIsNewView?: boolean;
            blIsNewUpdate?: boolean;
            serSubMenuRoleId: any;
            cfgTblSubMenu: {
                txtSubMenuName: string;
                cfgTblMenu: { txtMenuName: string };
                serSubMenuId: any;
                blIsview: boolean;
                blIsAdd: boolean;
                blIsUpdate: boolean;
            };
            subMenus: any[];
        }) => {
            const subMenuName = menu.cfgTblSubMenu?.txtSubMenuName ?? '';

            let parentMenuName: string | undefined;
            for (const menuName in this.frontendMenuStructure) {
                if (this.frontendMenuStructure[menuName].includes(subMenuName)) {
                    parentMenuName = menuName;
                    break;
                }
            }

            if (parentMenuName) {
                const subMenuData = {
                    serSubMenuRoleId: menu.serSubMenuRoleId,
                    subMenuId: menu.cfgTblSubMenu.serSubMenuId,
                    subMenuName: subMenuName,
                    canEnabled: menu.blIsEnabled ?? false,
                    canView: menu.blIsview ?? false,
                    canAdd: menu.blIsAdd ?? false,
                    canEdit: menu.blIsUpdate ?? false,
                    newCanCreate: menu.blIsNewCreate ?? false,
                    newCanView: menu.blIsNewView ?? false, // Changed default to false
                    newCanUpdate: menu.blIsNewUpdate ?? false,
                };
                menuMap[parentMenuName].subMenus.push(subMenuData);
            }
        });

        this.originalMenus = Object.values(menuMap).filter(menu =>
            menu.menuName === 'Dashboard' || menu.subMenus.length > 0
        );

        this.transformedMenus = JSON.parse(JSON.stringify(this.originalMenus));
        this.filterMenus();
        console.log("Final Menu Display", this.transformedMenus);
    }

    filterMenus(): void {
        if (!this.search.trim()) {
            this.transformedMenus = JSON.parse(JSON.stringify(this.originalMenus));
            return;
        }

        const searchLower = this.search.trim().toLowerCase();
        this.transformedMenus = this.originalMenus
            .map(menu => ({
                ...menu,
                subMenus: menu.subMenus.filter(subMenu =>
                    menu.menuName.toLowerCase().includes(searchLower) ||
                    subMenu.subMenuName.toLowerCase().includes(searchLower)
                )
            }))
            .filter(menu => menu.subMenus.length > 0 || menu.menuName.toLowerCase().includes(searchLower));
    }
}
