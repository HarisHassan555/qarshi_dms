import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { SharedDataService } from 'src/app/services/shared-data/shared-data.service';
import { UserService } from 'src/app/services/user/user.service';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css']
})
export class ChangePasswordComponent implements OnInit {
  form!: FormGroup;
  isSubmit = false;
  user: any;
  isUnmatched = false;
  constructor (
    private fb: FormBuilder,
    private userService: UserService,
    private sharedDataService: SharedDataService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.form = this.fb.group({
      userId: [''],
      oldPass: ['', Validators.required],
      newPass: ['', Validators.required],
      confirmPass: ['', Validators.required]
    });

    this.getUser();
  }

  getUser() {
    this.sharedDataService
      .getUser()
      .subscribe((data: any) => {
        if (data) {
          this.form.patchValue({
              userId: data.serUserId
          })
        }
      });
  }

  save() {
    this.isSubmit = true;
    if (this.form.invalid) return;

    if (this.form.value.newPass !== this.form.value.confirmPass) {
      this.isUnmatched = true;
      return;
    }

    let payload = this.form.value;
    delete payload.confirmPass;

    this.userService
      .changePassword(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Record saved successfully','success');
          this.isSubmit = false;
          this.form.reset();
          this.isUnmatched = false;
        } else {
          this.notificationService.showMessage('Error occured while saving','danger');
        }
      });
  }

}
