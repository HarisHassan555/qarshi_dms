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
  showNewPass = false;
  showConfirmPass = false;

  constructor (
    private fb: FormBuilder,
    private userService: UserService,
    private sharedDataService: SharedDataService,
    private notificationService: NotificationService
  ) { }

    initializeForm(): void {
        const formControls: any = {
            userId: [''],
            newPass: ['', Validators.required],
            confirmPass: ['', Validators.required],
        };

        if (this.role !== 'ADMIN') {
            formControls.oldPass = ['', Validators.required];
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

      const userJson = localStorage.getItem('user');
      if (userJson) {
          this.user = JSON.parse(userJson);
          this.role = this.user?.cfgTblRole?.txtRoleName || '';
      }
      this.initializeForm();

      if (this.role !== 'ADMIN') {
          this.form.patchValue({
              userId: this.user?.serUserId,
          });
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
            if (this.role === 'ADMIN') {
                const randomPass = this.generateRandomPassword();
                this.form.patchValue({
                    newPass: randomPass,
                    confirmPass: randomPass
                });
            }
        } else {
            this.form.get('userId')?.setValue('');
            if (this.role === 'ADMIN') {
                this.form.patchValue({
                    newPass: '',
                    confirmPass: ''
                });
            }
        }
    }

    generateRandomPassword(length: number = 10): string {
        const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+";
        let retVal = "";
        for (let i = 0; i < length; ++i) {
            retVal += charset.charAt(Math.floor(Math.random() * charset.length));
        }
        return retVal;
    }

  getUser() {
    this.sharedDataService
      .getUser()
      .subscribe((data: any) => {
          this.user=data
        if (data && this.role !== 'ADMIN') {
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

    if (this.form.value.newPass !== this.form.value.confirmPass) {
          this.isUnmatched = true;
          return;
    }

    let payload = this.form.value;
    delete payload.confirmPass;
    delete payload.selectedUser;
    const request$ = this.role === 'ADMIN'
      ? this.userService.changePasswordAdmin(payload)
      : this.userService.changePassword(payload);

    request$.subscribe((data: any) => {
      if (data == "Success") {
        this.notificationService.showMessage('Record saved successfully','success');
        this.isSubmit = false;
        this.form.reset();
        this.isUnmatched = false;
      } else if (data == "CPNM") {
        this.notificationService.showMessage('Current Password does not match','danger');
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
