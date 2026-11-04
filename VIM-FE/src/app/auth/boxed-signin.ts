import { animate, style, transition, trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppService } from 'src/app/service/app.service';
import { AuthService } from '../services/auth/auth.service';
import { UserService } from '../services/user/user.service';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import {NotificationService} from "../NotificationService";
import { DocumentHeaderBrandingService } from '../services/document-header-branding/document-header-branding.service';
import { ApplicationSettingService } from '../services/application-setting/application-setting.service';

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
export class BoxedSigninComponent implements OnInit, OnDestroy {
    store: any;
    showPassword: boolean = false;
    applicationName = 'Qarshi Workflow';
    applicationLogoUrl: SafeUrl | string = 'assets/images/qarshi-logo.png';
    private applicationLogoBlobUrl: string | null = null;

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
        public translate: TranslateService, public storeData: Store<any>, public router: Router, private appSetting: AppService,private notificationService: NotificationService,private route: ActivatedRoute, private documentHeaderBranding: DocumentHeaderBrandingService, private applicationSettingService: ApplicationSettingService, private sanitizer: DomSanitizer) {
        this.initStore();
        this.route.queryParams.subscribe(params => {
            if (params['error']) {
                // Show the error message as a notification
                this.notificationService.showMessage(params['error'], 'danger');
            }
        });
    }

    ngOnInit(): void {
        this.loadApplicationBrand();
    }

    ngOnDestroy(): void {
        if (this.applicationLogoBlobUrl) {
            URL.revokeObjectURL(this.applicationLogoBlobUrl);
        }
    }

    private loadApplicationBrand(): void {
        this.applicationSettingService.getPublicBranding().subscribe({
            next: (brand) => {
                const name = (brand?.applicationName || '').trim();
                this.applicationName = name || 'Qarshi Workflow';
                if (!brand?.hasLogo) {
                    this.applicationLogoUrl = 'assets/images/qarshi-logo.png';
                    return;
                }
                this.applicationSettingService.getPublicLogo().subscribe({
                    next: (blob) => {
                        if (!blob || blob.size === 0) {
                            return;
                        }
                        const typed = blob.type ? blob : new Blob([blob], { type: 'image/png' });
                        const objectUrl = URL.createObjectURL(typed);
                        if (this.applicationLogoBlobUrl) {
                            URL.revokeObjectURL(this.applicationLogoBlobUrl);
                        }
                        this.applicationLogoBlobUrl = objectUrl;
                        this.applicationLogoUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
                    },
                    error: () => {
                        this.applicationLogoUrl = 'assets/images/qarshi-logo.png';
                    },
                });
            },
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

    private resolveReturnUrl(): string | null {
        const raw = this.route.snapshot.queryParamMap.get('returnUrl');
        if (!raw) return null;
        const trimmed = raw.trim();
        if (!trimmed.startsWith('/') || trimmed.startsWith('//')) return null;
        return trimmed;
    }

    getUser() {
        // @ts-ignore
        this.userService.me().subscribe((data: CfgTblUser | null) => {
            if (data) {
                this.sharedDataService.saveUser(data);
                this.documentHeaderBranding.refresh();
                const returnUrl = this.resolveReturnUrl();
                this.router.navigateByUrl(returnUrl || '/Dashboard');
            } else {
                console.error('User data is null');
            }
        });
    }
}
