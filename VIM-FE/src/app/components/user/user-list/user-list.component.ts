import { Component, OnInit, ViewChild } from '@angular/core';
import {AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators} from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { UserService } from 'src/app/services/user/user.service';
import {map, Observable, of} from "rxjs";
import {PermissionService} from "../../../services/shared-data/permission-service";

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
  roles: any;
  passwordPolicies: any;
  customers: any;
  isRoleVendor = false;
  isRoleAdmin = false;

  cols = [
    // { field: '', title: 'Sr. No' },
    { field: 'txtUserName', title: 'Username' },
    { field: 'txtCnic', title: 'CNIC' },
    { field: 'txtContactNo', title: 'Contact Number' },
    { field: 'cfgTblRole.txtRoleName', title: 'Role' },
   /* { field: 'cfgTblPasswordPolicy.txtCode', title: 'Password Policy' },*/
    { field: 'txtAddress', title: 'Email' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private customerService: CustomerService,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
      this.form = this.fb.group({
          serUserId: [''],
          txtUserName: ['', Validators.required],
          txtAddress: ['', [Validators.required, Validators.email]], // Correct email validation
          txtCnic: ['', [Validators.required, Validators.pattern(/^\d{5}-\d{7}-\d{1}$/)], this.asyncCnicValidator()],
          cfgTblRole: this.fb.group({
              serRoleId: ['', Validators.required]
          }),
          txtContactNo: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
          cfgTblManager: [''],
          cfgTblCustomer: this.fb.group({
              serCustomerId: ['']
          }),
          blnStatus: [true, Validators.requiredTrue],
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

    getRoles() {
        this.userService.getRoles()
            .subscribe(data => {
                if (data) {
                    this.roles = data;
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

  add() {

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
          this.permissionService.canAdd('user').subscribe(canAdd => {
              if (canAdd == true) {
                  /* this.notificationService.showMessage('You do not have permission to add new countries', 'danger');*/
                  /*this.isSubmit = false;
                  this.countryForm.reset();
                  this.blnStatus = false;
                  this.modal.open();*/
                  this.isSubmit = false;
                  this.form.reset();
                  this.blnStatus = false;
                  this.modal.open();
                  return;
              }else{
                  this.notificationService.showMessage('You do not have permission to add New user', 'danger');
                  return;
              }

          });
      });


  }

  edit(user: any) {
      if (!this.permissionService.canUpdate('user')) {
          this.notificationService.showMessage('You do not have permission to edit user', 'danger');
          return;
      }
    console.log(user);
    this.form.reset();
    this.modal.open();
    debugger;
    this.form.patchValue(user);
    this.blnStatus = user.blnStatus;
    if (user.cfgTblRole && user.cfgTblRole.txtRoleName) {
      this.setControlValidations(user.cfgTblRole.txtRoleName.toLowerCase());
    }
  }

  submit() {
    this.isSubmit = true;
      console.log(this.form.controls['txtCnic'].errors);
    if (this.form.invalid) return;
    let payload = this.form.value;

    if (payload.serUserId) {
     // payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serUserId;
    }

    if (this.isRoleVendor) {
      delete payload.cfgTblManager;
    } else {
      delete payload.cfgTblCustomer
      if (this.isRoleAdmin) {
        delete payload.cfgTblManager
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

  onChangeRole($event: any) {
    let role = $event.target.options[$event.target.options.selectedIndex].text;
    this.setControlValidations(role.toLowerCase());
  }

  setControlValidations(role: any) {
    if (role === 'vendor') {
      console.log(role);
      this.isRoleVendor = true;
      this.form.get('cfgTblManager.serUserId')?.clearValidators();
      this.form.get('cfgTblCustomer.serCustomerId')?.setValidators([Validators.required]);
    } else if (role === 'admin') {
      this.isRoleVendor = false;
      this.isRoleAdmin = true;
      this.form.get('cfgTblManager.serUserId')?.clearValidators();
      this.form.get('cfgTblCustomer.serCustomerId')?.clearValidators();
    } else {
      this.isRoleVendor = false;
      this.form.get('cfgTblManager.serUserId')?.setValidators([Validators.required]);
      this.form.get('cfgTblCustomer.serCustomerId')?.clearValidators();
    }
    this.form.get('cfgTblManager.serUserId')?.updateValueAndValidity();
    this.form.get('cfgTblCustomer.serCustomerId')?.updateValueAndValidity();
  }

  get cfgTblRole(){
    return this.form.get('cfgTblRole') as FormGroup;
  }

  get cfgTblPasswordPolicy(){
    return this.form.get('cfgTblPasswordPolicy') as FormGroup;
  }

  get cfgTblManager(){
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
}
