import { animate, style, transition, trigger } from '@angular/animations';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppService } from 'src/app/service/app.service';
import { AuthService } from '../services/auth/auth.service';
import { UserService } from '../services/user/user.service';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import {NotificationService} from "../NotificationService";

@Component({
    moduleId: module.id,
    templateUrl: './boxed-signin.html',
    animations: [
        trigger('toggleAnimation', [
            transition(':enter', [style({ opacity: 0, transform: 'scale(0.95)' }), animate('100ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))]),
            transition(':leave', [animate('75ms', style({ opacity: 0, transform: 'scale(0.95)' }))]),
        ]),
    ],
})
export class BoxedSigninComponent {
    store: any;
    showPassword: boolean = false;

    public signinForm = this.formBuilder.group({
        username: ['', Validators.required],
        password: ['', Validators.required],
    });
    isSubmitted = false;
    isShowLoader = false;
    isSuccess = false;
    isError = false;

    constructor(
        private formBuilder: FormBuilder,
        private authService: AuthService,
        private userService: UserService,
        private sharedDataService: SharedDataService,
        public translate: TranslateService, public storeData: Store<any>, public router: Router, private appSetting: AppService,private notificationService: NotificationService,private route: ActivatedRoute) {
        this.initStore();
        this.route.queryParams.subscribe(params => {
            if (params['error']) {
                // Show the error message as a notification
                this.notificationService.showMessage(params['error'], 'danger');
            }
        });
    }
    async initStore() {
        this.storeData
            .select((d) => d.index)
            .subscribe((d) => {
                this.store = d;
            });
    }

    changeLanguage(item: any) {
        this.translate.use(item.code);
        this.appSetting.toggleLanguage(item);
        if (this.store.locale?.toLowerCase() === 'ae') {
            this.storeData.dispatch({ type: 'toggleRTL', payload: 'rtl' });
        } else {
            this.storeData.dispatch({ type: 'toggleRTL', payload: 'ltr' });
        }
        window.location.reload();
    }

    onSignin() {
        this.isSubmitted = true;
        if (this.signinForm.invalid) return;
        const payload = this.signinForm.value;
        this.isShowLoader = true;
        this.authService
            .signin(payload)
            .subscribe((data: any) => {
                if (data) {
                    this.isShowLoader = false;
                    this.isSuccess = true;
                    const token = data.token.split(' ')[1];
                    localStorage.setItem('token', token);
                    localStorage.setItem('user', JSON.stringify(data.user));
                    /*localStorage.setItem('token', data.split(' ')[1]);*/
                    this.getUser();
                }
            }, err => {
                this.isShowLoader = false;
                this.isError = true;
            });

    }

    togglePasswordVisibility() {
        this.showPassword = !this.showPassword;
    }

    getUser() {
        // @ts-ignore
        this.userService.me().subscribe((data: CfgTblUser | null) => {
            if (data) {
                this.sharedDataService.saveUser(data);
                this.router.navigateByUrl('/Dashboard');
            } else {
                console.error('User data is null');
            }
        });
    }
}
