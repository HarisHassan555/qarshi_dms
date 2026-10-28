import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { DepartmentService } from 'src/app/services/department/department.service';
import { OrgStructureService } from 'src/app/services/org-structure/org-structure.service';
import { UserService } from 'src/app/services/user/user.service';
import { map, Observable, of } from "rxjs";
import { PermissionService } from "../../../services/shared-data/permission-service";

@Component({
  selector: 'app-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit, OnDestroy {
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
  organizations: any[] = [];
  subareas: any[] = [];
  divisions: any[] = [];
  filteredSubareas: any[] = [];
  filteredDivisions: any[] = [];
  filteredDepartments: any[] = [];
  loginNameEditedManually = false;
  profilePicturePreview: string | null = null;
  profilePictureBase64: string | null = null;
  private profilePhotoBlobUrls: Record<number, string> = {};
  private profilePhotoSafeUrls: Record<number, SafeUrl> = {};
  isRoleVendor = false;
  isRoleAdmin = false;

  cols = [
    { field: 'txtUserName', title: 'Name', width: '220px' },
    { field: 'txtLoginName', title: 'Login Name', width: '180px' },
    { field: 'txtCnic', title: 'Employee ID', width: '160px' },
    { field: 'txtOrganizationName', title: 'Organization', width: '160px' },
    { field: 'txtSubareaName', title: 'Sub Area', width: '140px' },
    { field: 'txtDivisionName', title: 'Division', width: '140px' },
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
    private orgStructureService: OrgStructureService,
    private notificationService: NotificationService,
    private permissionService: PermissionService,
    private sanitizer: DomSanitizer,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnDestroy(): void {
    Object.values(this.profilePhotoBlobUrls).forEach((url) => URL.revokeObjectURL(url));
    this.profilePhotoBlobUrls = {};
    this.profilePhotoSafeUrls = {};
  }

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
      serOrganizationId: ['', Validators.required],
      serSubareaId: ['', Validators.required],
      serDivisionId: ['', Validators.required],
      serDepartmentId: ['', Validators.required],
      txtDesignation: ['', Validators.required],
      cfgTblManager: this.fb.group({
        serUserId: ['']
      }),
      cfgTblCustomer: this.fb.group({
        serCustomerId: ['']
      }),
      numMaxAttachmentMb: [5],
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
      this.loadOrgStructure();
    });

  }

  loadOrgStructure(): void {
    this.orgStructureService.getOrganizations().subscribe((data) => {
      this.organizations = data || [];
    });
    this.orgStructureService.getSubareas().subscribe((data) => {
      this.subareas = data || [];
      this.refreshOrgFilters();
    });
    this.orgStructureService.getDivisions().subscribe((data) => {
      this.divisions = data || [];
      this.refreshOrgFilters();
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
        this.prefetchProfilePhotos(this.users);
        this.filteredUsers = this.getDisplayedUsers();

      });
  }

  getAvatarLetter(name: string): string {
    const trimmed = String(name || '').trim();
    if (!trimmed) {
      return '?';
    }
    return trimmed.charAt(0).toUpperCase();
  }

  hasProfilePhoto(user: any): boolean {
    return this.getProfilePhotoUrl(user) != null;
  }

  getProfilePhotoUrl(user: any): SafeUrl | null {
    const userId = Number(user?.serUserId);
    if (!userId) {
      return null;
    }
    if (user?.profilePhotoSafeUrl) {
      return user.profilePhotoSafeUrl;
    }
    const cached = this.profilePhotoSafeUrls[userId];
    return cached || null;
  }

  userHasProfilePicturePath(user: any): boolean {
    const path = String(user?.txtProfilePicturePath || '').trim();
    return path.length > 0;
  }

  private prefetchProfilePhotos(users: any[]): void {
    Object.values(this.profilePhotoBlobUrls).forEach((url) => URL.revokeObjectURL(url));
    this.profilePhotoBlobUrls = {};
    this.profilePhotoSafeUrls = {};
    (users || []).forEach((user) => {
      user.profilePhotoDisplayUrl = undefined;
      user.profilePhotoSafeUrl = undefined;
    });

    (users || [])
      .filter((user) => this.userHasProfilePicturePath(user) && Number(user?.serUserId) > 0)
      .forEach((user) => {
        const userId = Number(user.serUserId);
        this.userService.loadProfilePictureObjectUrl(userId).subscribe({
          next: (objectUrl) => {
            user.profilePhotoDisplayUrl = objectUrl;
            const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
            user.profilePhotoSafeUrl = safeUrl;
            this.profilePhotoBlobUrls[userId] = objectUrl;
            this.profilePhotoSafeUrls[userId] = safeUrl;
            this.cdr.markForCheck();
          },
          error: () => {
            /* default avatar in template */
          },
        });
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
        this.refreshOrgFilters();
      });
  }

  onOrganizationChange(): void {
    this.form.patchValue({ serSubareaId: '', serDivisionId: '', serDepartmentId: '' });
    this.refreshOrgFilters();
  }

  onSubareaChange(): void {
    this.form.patchValue({ serDivisionId: '', serDepartmentId: '' });
    this.refreshOrgFilters();
  }

  onDivisionChange(): void {
    this.form.patchValue({ serDepartmentId: '' });
    this.refreshOrgFilters();
  }

  refreshOrgFilters(): void {
    const orgId = Number(this.form?.get('serOrganizationId')?.value);
    const subareaId = Number(this.form?.get('serSubareaId')?.value);
    const divisionId = Number(this.form?.get('serDivisionId')?.value);

    this.filteredSubareas = (this.subareas || []).filter((s) =>
      !orgId || Number(s.serOrganizationId) === orgId
    );
    this.filteredDivisions = (this.divisions || []).filter((d) =>
      !subareaId || Number(d.serSubareaId) === subareaId
    );
    this.filteredDepartments = (this.departments || []).filter((d) =>
      !divisionId || Number(d.serDivisionId) === divisionId
    );
  }

  onEmployeeIdOrNameChange(): void {
    if (this.loginNameEditedManually) {
      return;
    }
    const employeeId = String(this.form.get('txtCnic')?.value || '').trim();
    const fullName = String(this.form.get('txtUserName')?.value || '').trim();
    if (!employeeId && !fullName) {
      return;
    }
    this.userService.suggestLoginName(employeeId, fullName).subscribe({
      next: (res) => {
        if (!this.loginNameEditedManually && res?.loginName) {
          this.form.patchValue({ txtLoginName: res.loginName });
        }
      },
    });
  }

  onLoginNameInput(): void {
    this.loginNameEditedManually = true;
  }

  onProfilePictureSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    if (!file) {
      this.profilePicturePreview = null;
      this.profilePictureBase64 = null;
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result || '');
      this.profilePicturePreview = result;
      this.profilePictureBase64 = result;
    };
    reader.readAsDataURL(file);
  }

  clearProfilePicture(): void {
    this.profilePicturePreview = null;
    this.profilePictureBase64 = null;
  }

  add() {
    this.isSubmit = false;
    this.isRoleVendor = false;
    this.isRoleAdmin = false;
    this.loginNameEditedManually = false;
    this.clearProfilePicture();
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
      serOrganizationId: '',
      serSubareaId: '',
      serDivisionId: '',
      serDepartmentId: '',
      txtDesignation: '',
      cfgTblManager: {
        serUserId: ''
      },
      cfgTblCustomer: {
        serCustomerId: ''
      },
      numMaxAttachmentMb: 5,
      blnStatus: true
    });
    this.blnStatus = true;
    this.refreshOrgFilters();
    this.modal.open();
  }

  edit(user: any) {
    const roleName = this.getRoleName(user);
    const departmentId = this.resolveDepartmentId(user);
    const orgCascade = this.resolveOrgCascadeIds(user, departmentId);

    this.isSubmit = false;
    this.loginNameEditedManually = true;
    this.clearProfilePicture();
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
      serOrganizationId: orgCascade.organizationId || user?.serOrganizationId || '',
      serSubareaId: orgCascade.subareaId || user?.serSubareaId || '',
      serDivisionId: orgCascade.divisionId || user?.serDivisionId || '',
      serDepartmentId: departmentId || '',
      txtDesignation: user?.txtDesignation || '',
      cfgTblManager: {
        serUserId: user?.cfgTblManager?.serUserId || ''
      },
      cfgTblCustomer: {
        serCustomerId: user?.cfgTblCustomer?.serCustomerId || ''
      },
      numMaxAttachmentMb: this.resolveMaxAttachmentMb(user?.numMaxAttachmentMb),
      blnStatus: user?.blnStatus ?? true
    });
    this.refreshOrgFilters();
    this.modal.open();
    this.blnStatus = user?.blnStatus ?? true;
    if (roleName) {
      this.setControlValidations(roleName.toLowerCase());
    }
  }

  private resolveOrgCascadeIds(user: any, departmentId: number | null): {
    organizationId: number | null;
    subareaId: number | null;
    divisionId: number | null;
  } {
    let divisionId = Number(user?.serDivisionId) || null;
    if (!divisionId && departmentId) {
      const dept = (this.departments || []).find((d: any) => Number(d.serDepartmentId) === departmentId);
      divisionId = dept?.serDivisionId ? Number(dept.serDivisionId) : null;
    }
    let subareaId = Number(user?.serSubareaId) || null;
    let organizationId = Number(user?.serOrganizationId) || null;
    if (divisionId) {
      const division = (this.divisions || []).find((d: any) => Number(d.serDivisionId) === divisionId);
      if (division) {
        subareaId = subareaId || Number(division.serSubareaId) || null;
        organizationId = organizationId || (division.organizationName
          ? this.findOrganizationIdByName(division.organizationName)
          : null);
      }
    }
    if (subareaId && !organizationId) {
      const sub = (this.subareas || []).find((s: any) => Number(s.serSubareaId) === subareaId);
      organizationId = sub?.serOrganizationId ? Number(sub.serOrganizationId) : null;
    }
    return { organizationId, subareaId, divisionId };
  }

  private findOrganizationIdByName(name: string): number | null {
    const needle = String(name || '').trim().toLowerCase();
    const org = (this.organizations || []).find(
      (o: any) => String(o.txtOrganizationName || '').trim().toLowerCase() === needle
    );
    return org?.serOrganizationId ? Number(org.serOrganizationId) : null;
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
        const status = this.resolveSaveStatus(response);
        if (status === 'Success') {
          const queued = this.isCredentialEmailQueued(response);
          const message = queued
            ? 'User saved. Login credentials are being emailed in the background.'
            : 'Record saved successfully';
          this.notificationService.showMessage(message, 'success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getUsers();
        } else if (status === 'AX') {
          this.notificationService.showMessage('User already exists with the same details', 'danger');
        } else {
          this.notificationService.showMessage('Error occurred while saving', 'danger');
        }
      });
  }

  private isCredentialEmailQueued(response: any): boolean {
    if (response == null) {
      return false;
    }
    if (typeof response === 'object') {
      return String(response?.credentialEmailQueued || '').toLowerCase() === 'true';
    }
    if (typeof response === 'string') {
      try {
        const parsed = JSON.parse(response.trim());
        return String(parsed?.credentialEmailQueued || '').toLowerCase() === 'true';
      } catch {
        return false;
      }
    }
    return false;
  }

  private resolveSaveStatus(response: any): string {
    if (response == null) {
      return 'Failure';
    }
    if (typeof response === 'string') {
      const trimmed = response.trim();
      if (trimmed === 'Success' || trimmed === 'Failure' || trimmed === 'AX') {
        return trimmed;
      }
      try {
        const parsed = JSON.parse(trimmed);
        return String(parsed?.status || trimmed);
      } catch {
        return trimmed;
      }
    }
    return String(response?.status || 'Failure');
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
        u?.txtOrganizationName,
        u?.txtSubareaName,
        u?.txtDivisionName,
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
    const orgId = Number(formValue?.serOrganizationId);
    const subareaId = Number(formValue?.serSubareaId);
    const divisionId = Number(formValue?.serDivisionId);
    const selectedOrg = (this.organizations || []).find((o: any) => Number(o.serOrganizationId) === orgId);
    const selectedSub = (this.subareas || []).find((s: any) => Number(s.serSubareaId) === subareaId);
    const selectedDiv = (this.divisions || []).find((d: any) => Number(d.serDivisionId) === divisionId);

    const payload: any = {
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
      serOrganizationId: orgId > 0 ? orgId : null,
      serSubareaId: subareaId > 0 ? subareaId : null,
      serDivisionId: divisionId > 0 ? divisionId : null,
      txtOrganizationName: selectedOrg?.txtOrganizationName || '',
      txtSubareaName: selectedSub?.txtSubareaName || '',
      txtDivisionName: selectedDiv?.txtDivisionName || '',
      txtDesignation: (formValue?.txtDesignation || '').toString().trim(),
      numMaxAttachmentMb: this.resolveMaxAttachmentMb(formValue?.numMaxAttachmentMb),
      blnStatus: !!formValue?.blnStatus
    };
    delete payload.serDepartmentId;
    if (this.profilePictureBase64) {
      payload.profilePictureBase64 = this.profilePictureBase64;
    }
    if (!formValue?.serUserId) {
      payload.sendWelcomeEmail = true;
    }
    return payload;
  }

  private resolveMaxAttachmentMb(value: any): number {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      return 5;
    }
    return Math.min(Math.floor(parsed), 100);
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
