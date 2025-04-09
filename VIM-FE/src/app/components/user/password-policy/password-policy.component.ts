import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { UserService } from 'src/app/services/user/user.service';

@Component({
  selector: 'app-password-policy',
  templateUrl: './password-policy.component.html',
  styleUrls: ['./password-policy.component.css']
})
export class PasswordPolicyComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  passwordPolicies: any;
  blIsNumberRequired = false;
  blIsLowerUpper = false;
  blIsSpecialChaReq = false;
  blnStatus = false;

  cols = [
    // { field: '', title: 'Sr. No' },
    { field: 'txtCode', title: 'Password Policy' },
    { field: 'numHistoryCount', title: 'History Count' },
    { field: 'numExpireDays', title: 'Expiry Days' },
    { field: 'numAttempt', title: 'Password Attempts' },
    { field: 'numPassLength', title: 'Password Length' },
    { field: 'blIsNumberRequired', title: 'Number Required' },
    { field: 'blIsSpecialChaReq', title: 'Special Character Required' },
    { field: 'blIsLowerUpper', title: 'Lower Upper Letter' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private notificationService: NotificationService
  ) { }

  ngOnInit() {
    this.form = this.fb.group({
      serPasswordPolicyId: [''],
      txtCode: ['', Validators.required],
      numHistoryCount: ['', Validators.required],
      numExpireDays: ['', Validators.required],
      numAttempt: ['', Validators.required],
      numPassLength: ['', Validators.required]
    });

    this.getPasswordPolicies();
  }

  getPasswordPolicies() {
    this.userService.getAllPasswordPolicy()
    .subscribe(data => {
      if (data) {
        this.passwordPolicies = data;
      }
    });
  }

  add() {
    this.isSubmit = false;
    this.form.reset();
    this.blIsNumberRequired = false;
    this.blIsLowerUpper = false;
    this.blIsSpecialChaReq = false;
    this.blnStatus = false;
    this.modal.open();
  }

  edit(user: any) {
    console.log(user);
    this.form.reset();
    this.modal.open();
    this.form.patchValue(user);
    this.blIsNumberRequired = user.blIsNumberRequired;
    this.blIsLowerUpper = user.blIsLowerUpper;
    this.blIsSpecialChaReq = user.blIsSpecialChaReq;
    this.blnStatus = user.blnStatus;
  }

  submit() {
    this.isSubmit = true;
    if (this.form.invalid) return;
    let payload = this.form.value;

    payload.blIsNumberRequired = this.blIsNumberRequired;
    payload.blIsLowerUpper = this.blIsLowerUpper;
    payload.blIsSpecialChaReq = this.blIsSpecialChaReq;

    if (payload.serPasswordPolicyId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serPasswordPolicyId;
    }

    this.userService
      .savePasswordPolicy(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Record saved successfully','success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getPasswordPolicies();
        } else {
          this.notificationService.showMessage('Error occured while saving','danger');
        }
      });
  }
}
