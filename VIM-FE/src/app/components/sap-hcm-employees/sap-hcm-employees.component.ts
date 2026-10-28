import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { map, Observable, of } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { DepartmentService } from 'src/app/services/department/department.service';
import { OrgStructureService } from 'src/app/services/org-structure/org-structure.service';
import { UserService } from 'src/app/services/user/user.service';
import { SapHcmEmployeeRecord } from './sap-hcm-employees.demo-data';
import { SapHcmEmployeesService } from 'src/app/services/sap-hcm-employees/sap-hcm-employees.service';

@Component({
    selector: 'app-sap-hcm-employees',
    templateUrl: './sap-hcm-employees.component.html',
    styleUrls: ['./sap-hcm-employees.component.css'],
})
export class SapHcmEmployeesComponent implements OnInit {
    @ViewChild('createUserModal') createUserModal: any;

    loading = false;
    syncInProgress = false;
    loadError = '';
    syncMessage = '';

    search = '';
    filterCompany = '';
    filterDepartment = '';
    filterActive = '';

    allEmployees: SapHcmEmployeeRecord[] = [];
    displayRows: SapHcmEmployeeRecord[] = [];
    selectedEmployee: SapHcmEmployeeRecord | null = null;
    showDetailModal = false;

    createUserForm!: FormGroup;
    createUserSubmit = false;
    createUserSource: SapHcmEmployeeRecord | null = null;
    createUserMetaLoaded = false;
    createUserMetaLoading = false;
    users: any[] = [];
    roles: any[] = [];
    departments: any[] = [];
    organizations: any[] = [];
    subareas: any[] = [];
    divisions: any[] = [];
    filteredSubareas: any[] = [];
    filteredDivisions: any[] = [];
    filteredDepartments: any[] = [];
    customers: any[] = [];
    loginNameEditedManually = false;
    profilePicturePreview: string | null = null;
    profilePictureBase64: string | null = null;
    isRoleVendor = false;
    isRoleAdmin = false;

    companyOptions: string[] = [];
    departmentOptions: string[] = [];
    /** Raw blob URLs (for revokeObjectURL). */
    private photoBlobUrls: Record<string, string> = {};
    /** Trusted URLs for [src] (blob URLs must bypass Angular sanitizer). */
    private photoSafeUrls: Record<string, SafeUrl> = {};

    cols = [
        { field: 'employee', title: 'Employee ID' },
        { field: 'empName', title: 'Name' },
        { field: 'departmentName', title: 'Department' },
        { field: 'division', title: 'Division' },
        { field: 'designation', title: 'Designation' },
        { field: 'email', title: 'Email' },
        { field: 'companyName', title: 'Company' },
        { field: 'active', title: 'Status' },
        { field: 'reportToName', title: 'Reports To' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center', width: '200px' },
    ];

    constructor(
        private sapHcmEmployeesService: SapHcmEmployeesService,
        private cdr: ChangeDetectorRef,
        private sanitizer: DomSanitizer,
        private fb: FormBuilder,
        private userService: UserService,
        private departmentService: DepartmentService,
        private orgStructureService: OrgStructureService,
        private customerService: CustomerService,
        private notificationService: NotificationService
    ) {}

    ngOnInit(): void {
        this.createUserForm = this.fb.group({
            serUserId: [''],
            txtUserName: ['', Validators.required],
            txtLoginName: ['', Validators.required],
            txtAddress: ['', [Validators.required, Validators.email]],
            txtCnic: ['', [Validators.required], this.asyncCnicValidator()],
            cfgTblRole: this.fb.group({
                serRoleId: ['', Validators.required],
            }),
            txtContactNo: [''],
            serOrganizationId: ['', Validators.required],
            serSubareaId: ['', Validators.required],
            serDivisionId: ['', Validators.required],
            serDepartmentId: ['', Validators.required],
            txtDesignation: ['', Validators.required],
            cfgTblManager: this.fb.group({
                serUserId: [''],
            }),
            cfgTblCustomer: this.fb.group({
                serCustomerId: [''],
            }),
            numMaxAttachmentMb: [5],
            blnStatus: [true],
        });
        this.loadEmployees();
    }

    loadEmployees(): void {
        this.loading = true;
        this.loadError = '';
        this.sapHcmEmployeesService.getAll().subscribe({
            next: (rows) => {
                this.allEmployees = rows || [];
                this.companyOptions = this.uniqueSorted(this.allEmployees.map((row) => row.companyName));
                this.refreshDepartmentOptions();
                this.applyFilters();
                this.prefetchPhotos(this.allEmployees);
                this.loading = false;
            },
            error: () => {
                this.loadError = 'Could not load employees from server.';
                this.allEmployees = [];
                this.displayRows = [];
                this.loading = false;
            },
        });
    }

    syncFromSap(): void {
        this.syncInProgress = true;
        this.syncMessage = '';
        this.sapHcmEmployeesService.syncFromSap().subscribe({
            next: (result) => {
                this.syncInProgress = false;
                if (result?.status === 'Success') {
                    this.syncMessage = `SAP sync OK: ${result.totalItems} rows (${result.inserted} new, ${result.updated} updated, ${result.picturesSaved} photos).`;
                    this.loadEmployees();
                } else {
                    this.syncMessage = result?.message || 'SAP sync failed.';
                }
            },
            error: (err) => {
                this.syncInProgress = false;
                this.syncMessage = err?.error?.message || 'SAP sync failed.';
            },
        });
    }

    onXmlFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files && input.files.length ? input.files[0] : null;
        if (!file) {
            return;
        }
        this.syncInProgress = true;
        this.syncMessage = '';
        this.sapHcmEmployeesService.syncFromXml(file).subscribe({
            next: (result) => {
                this.syncInProgress = false;
                if (result?.status === 'Success') {
                    this.syncMessage = `Sync OK: ${result.totalItems} rows (${result.inserted} new, ${result.updated} updated, ${result.picturesSaved} photos).`;
                    this.loadEmployees();
                } else {
                    this.syncMessage = result?.message || 'Sync failed.';
                }
                input.value = '';
            },
            error: (err) => {
                this.syncInProgress = false;
                this.syncMessage = err?.error?.message || 'Sync failed.';
                input.value = '';
            },
        });
    }

    hasPhoto(row: SapHcmEmployeeRecord): boolean {
        return this.getPhotoUrl(row) != null;
    }

    getPhotoUrl(row: SapHcmEmployeeRecord): SafeUrl | null {
        if (!row?.employee) {
            return null;
        }
        if (row.photoDisplaySafeUrl) {
            return row.photoDisplaySafeUrl;
        }
        const safe = this.photoSafeUrls[row.employee];
        if (safe) {
            return safe;
        }
        if (!row.hasPicture && !row.pictureUrl) {
            return null;
        }
        return null;
    }

    private prefetchPhotos(rows: SapHcmEmployeeRecord[]): void {
        Object.values(this.photoBlobUrls).forEach((url) => URL.revokeObjectURL(url));
        this.photoBlobUrls = {};
        this.photoSafeUrls = {};
        rows.forEach((row) => {
            if (row.photoDisplayUrl) {
                URL.revokeObjectURL(row.photoDisplayUrl);
                row.photoDisplayUrl = undefined;
                row.photoDisplaySafeUrl = undefined;
            }
        });

        rows
            .filter((row) => row.employee && (row.hasPicture || row.pictureUrl))
            .forEach((row) => {
                this.sapHcmEmployeesService.loadPhotoObjectUrl(row.employee).subscribe({
                    next: (objectUrl) => {
                        row.photoDisplayUrl = objectUrl;
                        const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
                        row.photoDisplaySafeUrl = safeUrl;
                        this.photoBlobUrls[row.employee] = objectUrl;
                        this.photoSafeUrls[row.employee] = safeUrl;
                        this.cdr.markForCheck();
                    },
                    error: () => {
                        /* keep initials fallback */
                    },
                });
            });
    }

    onCompanyFilterChange(): void {
        this.filterDepartment = '';
        this.refreshDepartmentOptions();
        this.applyFilters();
    }

    private refreshDepartmentOptions(): void {
        const source = this.filterCompany
            ? this.allEmployees.filter((row) => row.companyName === this.filterCompany)
            : this.allEmployees;
        this.departmentOptions = this.uniqueSorted(source.map((row) => row.departmentName));
        if (this.filterDepartment && !this.departmentOptions.includes(this.filterDepartment)) {
            this.filterDepartment = '';
        }
    }

    applyFilters(): void {
        const searchText = this.search.trim().toLowerCase();
        this.displayRows = this.allEmployees.filter((row) => {
            if (this.filterCompany && row.companyName !== this.filterCompany) {
                return false;
            }
            if (this.filterDepartment && row.departmentName !== this.filterDepartment) {
                return false;
            }
            if (this.filterActive && row.active !== this.filterActive) {
                return false;
            }
            if (!searchText) {
                return true;
            }
            const haystack = [
                row.employee,
                row.empName,
                row.departmentName,
                row.division,
                row.designation,
                row.email,
                row.companyName,
                row.reportToName,
                row.reportTo,
            ].join(' ').toLowerCase();
            return haystack.includes(searchText);
        });
    }

    clearFilters(): void {
        this.search = '';
        this.filterCompany = '';
        this.filterDepartment = '';
        this.filterActive = '';
        this.refreshDepartmentOptions();
        this.applyFilters();
    }

    openDetails(row: SapHcmEmployeeRecord): void {
        this.selectedEmployee = row;
        this.showDetailModal = true;
    }

    closeDetails(): void {
        this.showDetailModal = false;
        this.selectedEmployee = null;
    }

    formatSapDate(value: string): string {
        const normalized = String(value || '').trim();
        if (!normalized || normalized === '0000-00-00') {
            return '—';
        }
        return normalized;
    }

    getInitials(name: string): string {
        const parts = String(name || '').trim().split(/\s+/).filter(Boolean);
        if (parts.length === 0) {
            return '?';
        }
        if (parts.length === 1) {
            return parts[0].substring(0, 2).toUpperCase();
        }
        return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }

    isActiveStatus(active: string): boolean {
        return String(active || '').trim().toLowerCase() === 'active';
    }

    openCreateUser(employee: SapHcmEmployeeRecord): void {
        this.createUserSubmit = false;
        this.createUserSource = employee;
        this.isRoleVendor = false;
        this.isRoleAdmin = false;

        const ensureOpen = () => {
            const email = String(employee.email || '').trim();
            const employeeId = String(employee.employee || '').trim();
            const orgCascade = this.resolveOrgCascadeFromSap(employee);
            this.loginNameEditedManually = false;
            this.clearProfilePicture();
            this.createUserForm.reset({
                serUserId: '',
                txtUserName: String(employee.empName || '').trim(),
                txtLoginName: '',
                txtAddress: email,
                txtCnic: employeeId,
                cfgTblRole: { serRoleId: '' },
                txtContactNo: '',
                serOrganizationId: orgCascade.organizationId || '',
                serSubareaId: orgCascade.subareaId || '',
                serDivisionId: orgCascade.divisionId || '',
                serDepartmentId: orgCascade.departmentId || '',
                txtDesignation: String(employee.designation || '').trim(),
                cfgTblManager: { serUserId: '' },
                cfgTblCustomer: { serCustomerId: '' },
                numMaxAttachmentMb: 5,
                blnStatus: this.isActiveStatus(employee.active),
            });
            this.refreshOrgFilters();
            this.prefillSuggestedLoginName(employeeId, String(employee.empName || '').trim());
            this.prefillSapPhotoAsProfile(employee);
            this.createUserModal?.open();
        };

        if (this.createUserMetaLoaded) {
            ensureOpen();
            return;
        }
        if (this.createUserMetaLoading) {
            return;
        }
        this.createUserMetaLoading = true;
        this.userService.getUsers().subscribe({
            next: (data) => {
                this.users = Array.isArray(data) ? data : [];
                this.userService.getRoles().subscribe({
                    next: (roles) => {
                        this.roles = Array.isArray(roles) ? roles : [];
                        this.departmentService.getAll().subscribe({
                            next: (depts: any) => {
                                this.departments = Array.isArray(depts) ? depts : [];
                                this.orgStructureService.getOrganizations().subscribe({
                                    next: (orgs) => {
                                        this.organizations = orgs || [];
                                        this.orgStructureService.getSubareas().subscribe({
                                            next: (subs) => {
                                                this.subareas = subs || [];
                                                this.orgStructureService.getDivisions().subscribe({
                                                    next: (divs) => {
                                                        this.divisions = divs || [];
                                                        this.customerService.getCustomers().subscribe({
                                                            next: (cust) => {
                                                                this.customers = Array.isArray(cust) ? cust : [];
                                                                this.createUserMetaLoaded = true;
                                                                this.createUserMetaLoading = false;
                                                                ensureOpen();
                                                            },
                                                            error: () => {
                                                                this.createUserMetaLoading = false;
                                                                this.notificationService.showMessage('Could not load vendor list', 'danger');
                                                            },
                                                        });
                                                    },
                                                    error: () => {
                                                        this.createUserMetaLoading = false;
                                                        this.notificationService.showMessage('Could not load divisions', 'danger');
                                                    },
                                                });
                                            },
                                            error: () => {
                                                this.createUserMetaLoading = false;
                                                this.notificationService.showMessage('Could not load sub areas', 'danger');
                                            },
                                        });
                                    },
                                    error: () => {
                                        this.createUserMetaLoading = false;
                                        this.notificationService.showMessage('Could not load organizations', 'danger');
                                    },
                                });
                            },
                            error: () => {
                                this.createUserMetaLoading = false;
                                this.notificationService.showMessage('Could not load departments', 'danger');
                            },
                        });
                    },
                    error: () => {
                        this.createUserMetaLoading = false;
                        this.notificationService.showMessage('Could not load roles', 'danger');
                    },
                });
            },
            error: () => {
                this.createUserMetaLoading = false;
                this.notificationService.showMessage('Could not load users', 'danger');
            },
        });
    }

    onOrganizationChange(): void {
        this.createUserForm.patchValue({ serSubareaId: '', serDivisionId: '', serDepartmentId: '' });
        this.refreshOrgFilters();
    }

    onSubareaChange(): void {
        this.createUserForm.patchValue({ serDivisionId: '', serDepartmentId: '' });
        this.refreshOrgFilters();
    }

    onDivisionChange(): void {
        this.createUserForm.patchValue({ serDepartmentId: '' });
        this.refreshOrgFilters();
    }

    refreshOrgFilters(): void {
        const orgId = Number(this.createUserForm?.get('serOrganizationId')?.value);
        const subareaId = Number(this.createUserForm?.get('serSubareaId')?.value);
        const divisionId = Number(this.createUserForm?.get('serDivisionId')?.value);

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
        const employeeId = String(this.createUserForm.get('txtCnic')?.value || '').trim();
        const fullName = String(this.createUserForm.get('txtUserName')?.value || '').trim();
        this.prefillSuggestedLoginName(employeeId, fullName);
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

    private prefillSuggestedLoginName(employeeId: string, fullName: string): void {
        if (!employeeId && !fullName) {
            return;
        }
        this.userService.suggestLoginName(employeeId, fullName).subscribe({
            next: (res) => {
                if (!this.loginNameEditedManually && res?.loginName) {
                    this.createUserForm.patchValue({ txtLoginName: res.loginName });
                }
            },
        });
    }

    private prefillSapPhotoAsProfile(employee: SapHcmEmployeeRecord): void {
        if (!employee?.employee || !employee.hasPicture) {
            return;
        }
        this.sapHcmEmployeesService.loadPhotoObjectUrl(employee.employee).subscribe({
            next: async (blobUrl) => {
                try {
                    const response = await fetch(blobUrl);
                    const blob = await response.blob();
                    const reader = new FileReader();
                    reader.onload = () => {
                        const result = String(reader.result || '');
                        if (!this.profilePictureBase64) {
                            this.profilePicturePreview = result;
                            this.profilePictureBase64 = result;
                        }
                    };
                    reader.readAsDataURL(blob);
                } catch {
                    // optional SAP photo prefill
                }
            },
        });
    }

    private resolveOrgCascadeFromSap(employee: SapHcmEmployeeRecord): {
        organizationId: number | null;
        subareaId: number | null;
        divisionId: number | null;
        departmentId: number | null;
    } {
        const orgName = String(employee.companyName || '').trim().toLowerCase();
        const subName = String(employee.subarea || '').trim().toLowerCase();
        const divName = String(employee.division || '').trim().toLowerCase();
        const deptName = String(employee.departmentName || '').trim().toLowerCase();

        const organization = (this.organizations || []).find(
            (o: any) => String(o.txtOrganizationName || '').trim().toLowerCase() === orgName
        );
        const organizationId = organization?.serOrganizationId ? Number(organization.serOrganizationId) : null;

        let subareaId: number | null = null;
        if (organizationId && subName) {
            const sub = (this.subareas || []).find(
                (s: any) =>
                    Number(s.serOrganizationId) === organizationId &&
                    String(s.txtSubareaName || '').trim().toLowerCase() === subName
            );
            subareaId = sub?.serSubareaId ? Number(sub.serSubareaId) : null;
        }

        let divisionId: number | null = null;
        if (subareaId && divName) {
            const div = (this.divisions || []).find(
                (d: any) =>
                    Number(d.serSubareaId) === subareaId &&
                    String(d.txtDivisionName || '').trim().toLowerCase() === divName
            );
            divisionId = div?.serDivisionId ? Number(div.serDivisionId) : null;
        }

        let departmentId: number | null = null;
        if (divisionId && deptName) {
            const dept = (this.departments || []).find(
                (d: any) =>
                    Number(d.serDivisionId) === divisionId &&
                    String(d.txtDepartmentName || '').trim().toLowerCase() === deptName
            );
            departmentId = dept?.serDepartmentId ? Number(dept.serDepartmentId) : null;
        }
        if (!departmentId) {
            departmentId = this.matchDepartmentIdByName(employee.departmentName);
        }

        return { organizationId, subareaId, divisionId, departmentId };
    }

    submitCreateUser(): void {
        this.createUserSubmit = true;
        if (this.createUserForm.invalid) {
            return;
        }
        const payload = this.buildCreateUserPayload();
        if (this.isLoginNameDuplicate(payload.txtLoginName)) {
            const loginNameControl = this.createUserForm.controls['txtLoginName'];
            loginNameControl.setErrors({ ...(loginNameControl.errors || {}), duplicate: true });
            this.notificationService.showMessage('Login name already exists. Please choose a different login name', 'danger');
            return;
        }
        delete payload.serUserId;
        if (this.isRoleVendor) {
            payload.cfgTblManager = null;
        } else {
            payload.cfgTblCustomer = null;
            if (this.isRoleAdmin) {
                payload.cfgTblManager = null;
            }
        }
        this.userService.save(payload).subscribe({
            next: (response: any) => {
                const status = this.resolveSaveStatus(response);
                if (status === 'Success') {
                    const queued = this.isCredentialEmailQueued(response);
                    const welcome = this.isWelcomeEmailResponse(response);
                    let message = 'User created successfully.';
                    if (queued && welcome) {
                        message += ' Welcome email with login details is being sent in the background.';
                    } else if (queued) {
                        message += ' Login credentials are being emailed in the background.';
                    }
                    this.notificationService.showMessage(message, 'success');
                    this.createUserSubmit = false;
                    this.clearProfilePicture();
                    this.createUserForm.reset();
                    this.createUserModal?.close();
                    this.userService.getUsers().subscribe((data) => {
                        this.users = Array.isArray(data) ? data : [];
                    });
                } else if (status === 'AX') {
                    this.notificationService.showMessage('User already exists with the same details', 'danger');
                } else {
                    this.notificationService.showMessage('Error occurred while saving', 'danger');
                }
            },
            error: () => {
                this.notificationService.showMessage('Error occurred while saving', 'danger');
            },
        });
    }

    onCreateUserRoleChange($event: Event): void {
        const select = $event.target as HTMLSelectElement;
        const role = select.options[select.selectedIndex]?.text || '';
        this.setCreateUserRoleValidations(role.toLowerCase());
    }

    get createUserCfgTblRole(): FormGroup {
        return this.createUserForm.get('cfgTblRole') as FormGroup;
    }

    get createUserCfgTblCustomer(): FormGroup {
        return this.createUserForm.get('cfgTblCustomer') as FormGroup;
    }

    formatReportTo(row: SapHcmEmployeeRecord): string {
        const name = String(row?.reportToName || '').trim();
        if (name) {
            return name;
        }
        const id = String(row?.reportTo || '').trim();
        if (!id || id === '00000000') {
            return '—';
        }
        return id;
    }

    private uniqueSorted(values: string[]): string[] {
        return Array.from(new Set(values.map((v) => String(v || '').trim()).filter(Boolean))).sort((a, b) =>
            a.localeCompare(b)
        );
    }

    private matchDepartmentIdByName(departmentName: string): number | null {
        const needle = String(departmentName || '').trim().toLowerCase();
        if (!needle) {
            return null;
        }
        const matched = (this.departments || []).find(
            (d: any) => String(d?.txtDepartmentName || '').trim().toLowerCase() === needle
        );
        const id = Number(matched?.serDepartmentId);
        return Number.isFinite(id) && id > 0 ? id : null;
    }

    private setCreateUserRoleValidations(role: string): void {
        if (role === 'vendor') {
            this.isRoleVendor = true;
            this.isRoleAdmin = false;
            this.createUserForm.get('cfgTblCustomer.serCustomerId')?.setValidators([Validators.required]);
        } else if (role === 'admin') {
            this.isRoleVendor = false;
            this.isRoleAdmin = true;
            this.createUserForm.get('cfgTblCustomer.serCustomerId')?.clearValidators();
        } else {
            this.isRoleVendor = false;
            this.isRoleAdmin = false;
            this.createUserForm.get('cfgTblCustomer.serCustomerId')?.clearValidators();
        }
        this.createUserForm.get('cfgTblCustomer.serCustomerId')?.updateValueAndValidity();
    }

    private buildCreateUserPayload(): any {
        const formValue = this.createUserForm.getRawValue();
        const departmentId = Number(formValue?.serDepartmentId);
        const orgId = Number(formValue?.serOrganizationId);
        const subareaId = Number(formValue?.serSubareaId);
        const divisionId = Number(formValue?.serDivisionId);
        const selectedDepartment = (this.departments || []).find(
            (d: any) => Number(d?.serDepartmentId) === departmentId
        );
        const selectedOrg = (this.organizations || []).find((o: any) => Number(o.serOrganizationId) === orgId);
        const selectedSub = (this.subareas || []).find((s: any) => Number(s.serSubareaId) === subareaId);
        const selectedDiv = (this.divisions || []).find((d: any) => Number(d.serDivisionId) === divisionId);

        const payload: any = {
            ...formValue,
            cfgTblRole: formValue?.cfgTblRole?.serRoleId
                ? { serRoleId: Number(formValue.cfgTblRole.serRoleId) }
                : null,
            cfgTblManager: formValue?.cfgTblManager?.serUserId
                ? { serUserId: Number(formValue.cfgTblManager.serUserId) }
                : null,
            cfgTblCustomer: formValue?.cfgTblCustomer?.serCustomerId
                ? { serCustomerId: Number(formValue.cfgTblCustomer.serCustomerId) }
                : null,
            hrTblDepartment: departmentId > 0 ? { serDepartmentId: departmentId } : null,
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
            blnStatus: !!formValue?.blnStatus,
        };
        delete payload.serDepartmentId;
        if (this.profilePictureBase64) {
            payload.profilePictureBase64 = this.profilePictureBase64;
        }
        payload.sendWelcomeEmail = true;
        payload.createdFromSapHcm = true;
        return payload;
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

    private isWelcomeEmailResponse(response: any): boolean {
        if (response == null) {
            return true;
        }
        if (typeof response === 'object') {
            return String(response?.welcomeEmail || 'true').toLowerCase() !== 'false';
        }
        if (typeof response === 'string') {
            try {
                const parsed = JSON.parse(response.trim());
                return String(parsed?.welcomeEmail || 'true').toLowerCase() !== 'false';
            } catch {
                return true;
            }
        }
        return true;
    }

    private resolveMaxAttachmentMb(value: any): number {
        const parsed = Number(value);
        if (!Number.isFinite(parsed) || parsed <= 0) {
            return 5;
        }
        return Math.min(Math.floor(parsed), 100);
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

    private normalizeUsername(value: string): string {
        return (value || '').trim().toLowerCase();
    }

    private isLoginNameDuplicate(loginName: string): boolean {
        if (!Array.isArray(this.users)) {
            return false;
        }
        const normalized = this.normalizeUsername(loginName);
        if (!normalized) {
            return false;
        }
        return this.users.some((user: any) => {
            const userName = this.normalizeUsername(user?.txtLoginName || user?.txtUserName || '');
            return userName === normalized;
        });
    }

    private asyncCnicValidator(): (control: AbstractControl) => Observable<ValidationErrors | null> {
        return (control) =>
            of(control.value).pipe(
                map((value: string) => {
                    const isValid = this.checkCnicUnique(value);
                    return isValid ? null : { uniqueCnic: true };
                })
            );
    }

    private checkCnicUnique(cnic: string): boolean {
        const needle = String(cnic || '').trim();
        if (!needle) {
            return true;
        }
        if (!Array.isArray(this.users)) {
            return true;
        }
        return !this.users.some((u: any) => String(u?.txtCnic || '').trim() === needle);
    }
}
