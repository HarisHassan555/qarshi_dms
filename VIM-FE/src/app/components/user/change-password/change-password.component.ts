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
  users : any;
  role: string = '';

  constructor (
    private fb: FormBuilder,
    private userService: UserService,
    private sharedDataService: SharedDataService,
    private notificationService: NotificationService
  ) { }

    initializeForm(): void {
        const formControls: any = {
            userId: [''],
            oldPass: ['', Validators.required],
        };

        if (this.role !== 'ADMIN') {
            formControls.newPass = ['', Validators.required];
            formControls.confirmPass = ['', Validators.required];
        }

        if (this.role === 'ADMIN') {
            formControls.selectedUser = ['', Validators.required];
        }

        this.form = this.fb.group(formControls);
    }

  ngOnInit(): void {
    /*this.form = this.fb.group({
      userId: [''],
      oldPass: ['', Validators.required],
      newPass: ['', Validators.required],
      confirmPass: ['', Validators.required],
      selectedUser :[''],
    });*/

      this.initializeForm();


      const userJson = localStorage.getItem('user');
      if (userJson) {
          this.user = JSON.parse(userJson);
          this.role = this.user?.cfgTblRole?.txtRoleName || '';
          debugger;

          if (this.role !== 'ADMIN') {
              this.form.patchValue({
                  userId: this.user?.serUserId,
              });
          }
      }

      if (this.role === 'ADMIN') {
          this.getUsers();
      }

      this.getUser();

  }


    updateUserId(): void {
        const selectedUser = this.form.get('selectedUser')?.value;
        if (selectedUser) {
            this.form.get('userId')?.setValue(selectedUser);
        } else {
            this.form.get('userId')?.setValue('');
        }
    }

  getUser() {
    this.sharedDataService
      .getUser()
      .subscribe((data: any) => {
          this.user=data
        if (data) {
          this.form.patchValue({
              userId: data.serUserId
          })
        }
      });
  }

    getUsers() {
        this.users = [];
        this.userService.getUsers()
            .subscribe(data => {
                this.users = data;
                console.log(this.users);
            });
    }



  save() {
    this.isSubmit = true;
    if (this.form.invalid) return;

    /*if (this.form.value.newPass !== this.form.value.confirmPass) {
      this.isUnmatched = true;
      return;
    }*/

    // Check password match for non-admin users
    if (this.role !== 'ADMIN' && this.form.value.newPass !== this.form.value.confirmPass) {
          this.isUnmatched = true;
          return;
    }

    let payload = this.form.value;
    delete payload.confirmPass;
    delete payload.selectedUser;
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


    /*resetPassword(): void {
        this.isSubmit = true;
        this.isUnmatched = false;

        if (this.form.valid) {
            const resetPass = this.form.get('resetPass')?.value;
            const confirmPass = this.form.get('confirmPass')?.value;
            const selectedUser = this.form.get('selectedUser')?.value;

            if (resetPass !== confirmPass) {
                this.isUnmatched = true;
                return;
            }

            console.log('Resetting password for user:', selectedUser);
            console.log('New password:', resetPass);

            // Reset form after successful submission
            this.form.reset();
            this.isSubmit = false;
        }
    }*/



}
