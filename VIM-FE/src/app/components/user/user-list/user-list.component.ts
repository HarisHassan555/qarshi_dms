import { Component, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { DepartmentService } from 'src/app/services/department/department.service';
import { UserService } from 'src/app/services/user/user.service';
import { map, Observable, of } from "rxjs";
import { PermissionService } from "../../../services/shared-data/permission-service";

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  blnStatus = false;

  users: any;
  filteredUsers: any[] = [];
  roles: any;
  passwordPolicies: any;
  customers: any;
  departments: any[] = [];
  isRoleVendor = false;
  isRoleAdmin = false;

  cols = [
    { field: 'txtUserName', title: 'Name' },
    { field: 'txtLoginName', title: 'Login Name', width: '180px' },
    { field: 'txtCnic', title: 'Employee ID', width: '160px' },
    { field: 'txtDepartmentName', title: 'Department', width: '180px' },
    { field: 'txtDesignation', title: 'Designation', width: '180px' },
    { field: 'txtContactNo', title: 'Contact Number', width: '160px' },
    { field: 'cfgTblRole.txtRoleName', title: 'Role', width: '120px' },
    { field: 'txtAddress', title: 'Email' },
    { field: 'blnStatus', title: 'Status', width: '100px' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center', width: '100px' },
  ];

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private customerService: CustomerService,
    private departmentService: DepartmentService,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
    this.form = this.fb.group({
      serUserId: [''],
      txtUserName: ['', Validators.required],
      txtLoginName: ['', Validators.required],
      txtAddress: ['', [Validators.required, Validators.email]], // Correct email validation
      // Keep txtCnic as payload key for backend compatibility, but use it as Employee ID in UI.
      txtCnic: ['', [Validators.required], this.asyncCnicValidator()],
      cfgTblRole: this.fb.group({
        serRoleId: ['', Validators.required]
      }),
      txtContactNo: [''],
      serDepartmentId: ['', Validators.required],
      txtDesignation: ['', Validators.required],
      cfgTblManager: this.fb.group({
        serUserId: ['']
      }),
      cfgTblCustomer: this.fb.group({
        serCustomerId: ['']
      }),
      blnStatus: [true],
    });
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
      this.getPasswordPolicies();
      this.getCustomers();
      this.getDepartments();
    });

  }

  getUsers() {
    this.users = [];
    this.userService.getUsers()
      .subscribe(data => {
        this.users = data;
        this.users = (this.users || []).map((user: any) => ({
          ...user,
          cfgTblRole: this.resolveRoleObject(user?.cfgTblRole)
        }));
        this.filteredUsers = this.getDisplayedUsers();

      });
  }

  getRoles() {
    this.userService.getRoles()
      .subscribe(data => {
        if (data) {
          this.roles = data;
          this.users = (this.users || []).map((user: any) => ({
            ...user,
            cfgTblRole: this.resolveRoleObject(user?.cfgTblRole)
          }));
          this.filteredUsers = this.getDisplayedUsers();
        }
      });
  }

  getPasswordPolicies() {
    this.userService.getPasswordPolicy()
      .subscribe(data => {
        if (data) {
          this.passwordPolicies = data;
        }
      });
  }

  getCustomers() {
    this.customerService.getCustomers()
      .subscribe(data => {
        if (data) {
          this.customers = data;
        }
      });
  }

  getDepartments() {
    this.departmentService.getAll()
      .subscribe((data: any) => {
        this.departments = Array.isArray(data) ? data : [];
      });
  }

  add() {
    this.isSubmit = false;
    this.isRoleVendor = false;
    this.isRoleAdmin = false;
    this.form.reset({
      serUserId: '',
      txtUserName: '',
      txtLoginName: '',
      txtAddress: '',
      txtCnic: '',
      cfgTblRole: {
        serRoleId: ''
      },
      txtContactNo: '',
      serDepartmentId: '',
      txtDesignation: '',
      cfgTblManager: {
        serUserId: ''
      },
      cfgTblCustomer: {
        serCustomerId: ''
      },
      blnStatus: true
    });
    this.blnStatus = true;
    this.modal.open();
  }

  edit(user: any) {
    const roleName = this.getRoleName(user);
    const departmentId = this.resolveDepartmentId(user);

    this.isSubmit = false;
    this.isRoleVendor = false;
    this.isRoleAdmin = false;
    this.form.reset({
      serUserId: user?.serUserId || '',
      txtUserName: user?.txtUserName || '',
      txtLoginName: user?.txtLoginName || user?.txtUserName || '',
      txtAddress: user?.txtAddress || '',
      txtCnic: user?.txtCnic || '',
      cfgTblRole: {
        serRoleId: user?.cfgTblRole?.serRoleId || user?.cfgTblRole || ''
      },
      txtContactNo: user?.txtContactNo || '',
      serDepartmentId: departmentId || '',
      txtDesignation: user?.txtDesignation || '',
      cfgTblManager: {
        serUserId: user?.cfgTblManager?.serUserId || ''
      },
      cfgTblCustomer: {
        serCustomerId: user?.cfgTblCustomer?.serCustomerId || ''
      },
      blnStatus: user?.blnStatus ?? true
    });
    this.modal.open();
    this.blnStatus = user?.blnStatus ?? true;
    if (roleName) {
      this.setControlValidations(roleName.toLowerCase());
    }
  }

  submit() {
    this.isSubmit = true;
    if (this.form.invalid) return;
    const payload = this.buildPayload();

    if (this.isLoginNameDuplicate(payload.txtLoginName, payload.serUserId)) {
      const loginNameControl = this.form.controls['txtLoginName'];
      loginNameControl.setErrors({ ...(loginNameControl.errors || {}), duplicate: true });
      this.notificationService.showMessage('Login name already exists. Please choose a different login name', 'danger');
      return;
    }

    if (payload.serUserId) {
      payload.blIsDeleted = false;
    } else {
      delete payload.serUserId;
    }

    if (this.isRoleVendor) {
      payload.cfgTblManager = null;
    } else {
      payload.cfgTblCustomer = null;
      if (this.isRoleAdmin) {
        payload.cfgTblManager = null;
      }
    }
    this.userService
      .save(payload).subscribe((response: any) => {
        let data = typeof response === 'string' ? JSON.parse(response) : response;
        if (data.status === 'Success') {
          this.notificationService.showMessage('Record saved successfully', 'success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getUsers();
        } else {
          this.notificationService.showMessage('Error occurred while saving', 'danger');
        }
      });
  }

  getDisplayedUsers(): any[] {
    if (!this.users || !Array.isArray(this.users)) return [];
    if (!this.search || !this.search.trim()) return this.users;
    const needle = this.search.toLowerCase().trim();
    return this.users.filter((u: any) => {
      const roleName = u?.cfgTblRole?.txtRoleName || '';
      const departmentName = u?.txtDepartmentName || u?.hrTblDepartment?.txtDepartmentName || '';
      const designation = u?.txtDesignation || '';
      const statusText = u?.blnStatus ? 'active' : 'inactive';
      const haystack = [
        u?.txtUserName,
        u?.txtLoginName,
        u?.txtCnic,
        departmentName,
        designation,
        u?.txtContactNo,
        u?.txtAddress,
        roleName,
        statusText
      ].map(v => (v ?? '').toString().toLowerCase());
      return haystack.some(v => v.includes(needle));
    });
  }

  onChangeRole($event: any) {
    let role = $event.target.options[$event.target.options.selectedIndex].text;
    this.setControlValidations(role.toLowerCase());
  }

  setControlValidations(role: any) {
    if (role === 'vendor') {
      this.isRoleVendor = true;
      this.isRoleAdmin = false;
      this.form.get('cfgTblCustomer.serCustomerId')?.setValidators([Validators.required]);
    } else if (role === 'admin') {
      this.isRoleVendor = false;
      this.isRoleAdmin = true;
      this.form.get('cfgTblCustomer.serCustomerId')?.clearValidators();
    } else {
      this.isRoleVendor = false;
      this.isRoleAdmin = false;
      this.form.get('cfgTblCustomer.serCustomerId')?.clearValidators();
    }
    this.form.get('cfgTblManager.serUserId')?.updateValueAndValidity();
    this.form.get('cfgTblCustomer.serCustomerId')?.updateValueAndValidity();
  }

  get cfgTblRole() {
    return this.form.get('cfgTblRole') as FormGroup;
  }

  get cfgTblPasswordPolicy() {
    return this.form.get('cfgTblPasswordPolicy') as FormGroup;
  }

  get cfgTblManager() {
    return this.form.get('cfgTblManager') as FormGroup;
  }

  get cfgTblCustomer() {
    return this.form.get('cfgTblCustomer') as FormGroup;
  }


  asyncCnicValidator(): (control: AbstractControl) => Observable<ValidationErrors | null> {
    return (control) => {
      return of(control.value).pipe(
        map((value: string) => {
          const isValid = this.checkCnic(value);
          return isValid ? null : { uniqueCnic: true };
        })
      );
    };
  }

  checkCnic(cnic: string): boolean {
    const existingCnics = ['12345-1234567-1', '67890-9876543-2'];
    return !existingCnics.includes(cnic);
  }

  private normalizeUsername(value: string): string {
    return (value || '').trim().toLowerCase();
  }

  private resolveRoleObject(roleValue: any): any {
    if (!roleValue) {
      return null;
    }
    if (typeof roleValue === 'object') {
      return roleValue;
    }
    if (!Array.isArray(this.roles)) {
      return roleValue;
    }
    return this.roles.find((role: any) => Number(role?.serRoleId) === Number(roleValue)) || null;
  }

  private getRoleName(user: any): string {
    const resolvedRole = this.resolveRoleObject(user?.cfgTblRole);
    return resolvedRole?.txtRoleName || '';
  }

  private resolveDepartmentId(user: any): number | null {
    const departmentId = Number(user?.hrTblDepartment?.serDepartmentId);
    if (Number.isFinite(departmentId) && departmentId > 0) {
      return departmentId;
    }

    const departmentName = (user?.txtDepartmentName || '').toString().trim().toLowerCase();
    if (!departmentName) {
      return null;
    }

    const matchedDepartment = (this.departments || []).find((department: any) =>
      (department?.txtDepartmentName || '').toString().trim().toLowerCase() === departmentName
    );

    return matchedDepartment?.serDepartmentId || null;
  }

  private buildPayload(): any {
    const formValue = this.form.getRawValue();
    const departmentId = Number(formValue?.serDepartmentId);
    const selectedDepartment = (this.departments || []).find((department: any) =>
      Number(department?.serDepartmentId) === departmentId
    );

    return {
      ...formValue,
      cfgTblRole: formValue?.cfgTblRole?.serRoleId ? {
        serRoleId: Number(formValue.cfgTblRole.serRoleId)
      } : null,
      cfgTblManager: formValue?.cfgTblManager?.serUserId ? {
        serUserId: Number(formValue.cfgTblManager.serUserId)
      } : null,
      cfgTblCustomer: formValue?.cfgTblCustomer?.serCustomerId ? {
        serCustomerId: Number(formValue.cfgTblCustomer.serCustomerId)
      } : null,
      hrTblDepartment: departmentId > 0 ? {
        serDepartmentId: departmentId
      } : null,
      txtUserName: (formValue?.txtUserName || '').toString().trim(),
      txtLoginName: (formValue?.txtLoginName || '').toString().trim(),
      txtDepartmentName: selectedDepartment?.txtDepartmentName || '',
      txtDesignation: (formValue?.txtDesignation || '').toString().trim(),
      blnStatus: !!formValue?.blnStatus
    };
  }

  private isLoginNameDuplicate(loginName: string, currentUserId?: number): boolean {
    if (!Array.isArray(this.users)) return false;
    const normalized = this.normalizeUsername(loginName);
    if (!normalized) return false;

    return this.users.some((user: any) => {
      const userName = this.normalizeUsername(user?.txtLoginName || user?.txtUserName || '');
      const userId = Number(user?.serUserId || 0);
      const editingUserId = Number(currentUserId || 0);
      return userName === normalized && userId !== editingUserId;
    });
  }
}
