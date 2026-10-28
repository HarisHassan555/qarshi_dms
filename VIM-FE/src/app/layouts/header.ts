import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Router, NavigationEnd } from '@angular/router';
import { AppService } from '../service/app.service';
import { animate, style, transition, trigger } from '@angular/animations';
import { DomSanitizer, SafeHtml, SafeUrl } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import { ProfileDrawerService } from '../services/profile-drawer/profile-drawer.service';
import { ProductTourService } from '../product-tour/product-tour.service';
import { PageBreadcrumbItem, PageBreadcrumbService } from '../services/page-breadcrumb/page-breadcrumb.service';
import { ChatRealtimeService } from '../services/chat/chat-realtime.service';
import { ChatService } from '../services/chat/chat.service';
import { ChatNotificationService } from '../services/chat/chat-notification.service';
import { NotificationRealtimeService } from '../services/user-notification/notification-realtime.service';
import { UserNotificationService } from '../services/user-notification/user-notification.service';
import { DocumentNotificationAlertService } from '../services/user-notification/document-notification-alert.service';
import { OpenMeteoWeatherService } from '../services/weather/open-meteo-weather.service';
import { DocumentHeaderBrandingService } from '../services/document-header-branding/document-header-branding.service';
import { UserService } from '../services/user/user.service';
import { Subscription } from 'rxjs';
@Component({
    moduleId: module.id,
    selector: 'header',
    templateUrl: './header.html',
    styleUrls: ['./header.css'],
    animations: [
        trigger('toggleAnimation', [
            transition(':enter', [style({ opacity: 0, transform: 'scale(0.95)' }), animate('100ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))]),
            transition(':leave', [animate('75ms', style({ opacity: 0, transform: 'scale(0.95)' }))]),
        ]),
    ],
})
export class HeaderComponent implements OnInit, OnDestroy {
    store: any;
    search = false;
    breadcrumbs: PageBreadcrumbItem[] = [];
    clockHours = '00';
    clockMinutes = '00';
    clockSeconds = '00';
    clockAmPm = 'AM';
    clockDate = '';
    clockColonLit = true;
    weatherKind: 'sun' | 'moon' | 'cloudy' = 'sun';
    weatherLabel = '';
    weatherTemp = '';
    chatUnreadTotal = 0;
    documentNotificationUnreadTotal = 0;
    chatBrowserNotifyPermission: NotificationPermission | 'unsupported' = 'default';
    private clockTimer: number | null = null;
    private chatUnreadSub?: Subscription;
    private documentNotificationUnreadSub?: Subscription;
    private documentNotificationFrameSub?: Subscription;
    private profilePhotoUpdatedSub?: Subscription;
    private colonTimer: number | null = null;
    private weatherRefreshTimer: number | null = null;
    private static readonly WEATHER_REFRESH_MS = 15 * 60 * 1000;
    headerProfilePhotoSafeUrl: SafeUrl | null = null;
    private headerProfilePhotoBlobUrl: string | null = null;
    notifications = [
        {
            id: 1,
            profile: 'user-profile.jpeg',
            message: '<strong class="text-sm mr-1">John Doe</strong>invite you to <strong>Prototyping</strong>',
            time: '45 min ago',
        },
        {
            id: 2,
            profile: 'profile-34.jpeg',
            message: '<strong class="text-sm mr-1">Adam Nolan</strong>mentioned you to <strong>UX Basics</strong>',
            time: '9h Ago',
        },
        {
            id: 3,
            profile: 'profile-16.jpeg',
            message: '<strong class="text-sm mr-1">Anna Morgan</strong>Upload a file',
            time: '9h Ago',
        },
    ];
    messages = [
        {
            id: 1,
            image: this.sanitizer.bypassSecurityTrustHtml(
                `<span class="grid place-content-center w-9 h-9 rounded-full bg-success-light dark:bg-success text-success dark:text-success-light"><svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg></span>`
            ),
            title: 'Congratulations!',
            message: 'Your OS has been updated.',
            time: '1hr',
        },
        {
            id: 2,
            image: this.sanitizer.bypassSecurityTrustHtml(
                `<span class="grid place-content-center w-9 h-9 rounded-full bg-info-light dark:bg-info text-info dark:text-info-light"><svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg></span>`
            ),
            title: 'Did you know?',
            message: 'You can switch between artboards.',
            time: '2hr',
        },
        {
            id: 3,
            image: this.sanitizer.bypassSecurityTrustHtml(
                `<span class="grid place-content-center w-9 h-9 rounded-full bg-danger-light dark:bg-danger text-danger dark:text-danger-light"> <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg></span>`
            ),
            title: 'Something went wrong!',
            message: 'Send Reposrt',
            time: '2days',
        },
        {
            id: 4,
            image: this.sanitizer.bypassSecurityTrustHtml(
                `<span class="grid place-content-center w-9 h-9 rounded-full bg-warning-light dark:bg-warning text-warning dark:text-warning-light"><svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">    <circle cx="12" cy="12" r="10"></circle>    <line x1="12" y1="8" x2="12" y2="12"></line>    <line x1="12" y1="16" x2="12.01" y2="16"></line></svg></span>`
            ),
            title: 'Warning',
            message: 'Your password strength is low.',
            time: '5days',
        },
    ];
    user: any;

    constructor(
        public translate: TranslateService,
        public storeData: Store<any>,
        public router: Router,
        private appSetting: AppService,
        private sanitizer: DomSanitizer,
        private sharedDataService: SharedDataService,
        private profileDrawerService: ProfileDrawerService,
        private productTourService: ProductTourService,
        private pageBreadcrumbService: PageBreadcrumbService,
        private chatRealtime: ChatRealtimeService,
        private chatService: ChatService,
        private chatNotification: ChatNotificationService,
        private documentHeaderBranding: DocumentHeaderBrandingService,
        private userService: UserService,
        private notificationRealtime: NotificationRealtimeService,
        private userNotificationService: UserNotificationService,
        private documentNotificationAlert: DocumentNotificationAlertService,
        private openMeteoWeather: OpenMeteoWeatherService,
    ) {
        this.initStore();
    }
    async initStore() {
        this.storeData
            .select((d) => d.index)
            .subscribe((d) => {
                this.store = d;
            });
    }

    ngOnInit() {
        this.setActiveDropdown();
        this.pageBreadcrumbService.crumbs$.subscribe((crumbs) => {
            this.breadcrumbs = crumbs;
        });
        this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                this.setActiveDropdown();
            }
        });
        this.getUser();
        this.loadHeaderProfilePhoto();
        this.profilePhotoUpdatedSub = this.profileDrawerService.profilePhotoUpdated$.subscribe(() =>
            this.loadHeaderProfilePhoto()
        );
        this.startClock();
        this.startWeatherRefresh();
        this.initChatRealtime();
        this.initDocumentNotificationRealtime();
        this.documentHeaderBranding.ensureLoaded();
    }

    ngOnDestroy(): void {
        this.chatUnreadSub?.unsubscribe();
        this.documentNotificationUnreadSub?.unsubscribe();
        this.documentNotificationFrameSub?.unsubscribe();
        this.profilePhotoUpdatedSub?.unsubscribe();
        this.revokeHeaderProfilePhoto();
        if (this.clockTimer !== null) {
            window.clearInterval(this.clockTimer);
        }
        if (this.colonTimer !== null) {
            window.clearInterval(this.colonTimer);
        }
        if (this.weatherRefreshTimer !== null) {
            window.clearInterval(this.weatherRefreshTimer);
        }
    }

    private startClock(): void {
        this.tickClock();
        this.clockTimer = window.setInterval(() => this.tickClock(), 1000);
        this.colonTimer = window.setInterval(() => {
            this.clockColonLit = !this.clockColonLit;
        }, 500);
    }

    private tickClock(): void {
        const now = new Date();
        const hours24 = now.getHours();
        const hours12 = hours24 % 12 || 12;
        this.clockHours = hours12.toString().padStart(2, '0');
        this.clockMinutes = now.getMinutes().toString().padStart(2, '0');
        this.clockSeconds = now.getSeconds().toString().padStart(2, '0');
        this.clockAmPm = hours24 >= 12 ? 'PM' : 'AM';
        this.clockDate = now.toLocaleDateString(undefined, {
            weekday: 'short',
            day: 'numeric',
            month: 'short',
            year: 'numeric',
        });
    }

    private startWeatherRefresh(): void {
        this.refreshWeatherFromApi();
        this.weatherRefreshTimer = window.setInterval(
            () => this.refreshWeatherFromApi(),
            HeaderComponent.WEATHER_REFRESH_MS
        );
    }

    private refreshWeatherFromApi(): void {
        this.openMeteoWeather.getNavbarWeather().subscribe((snapshot) => {
            if (!snapshot) {
                this.applyWeatherFallback();
                return;
            }
            this.weatherKind = snapshot.kind;
            this.weatherLabel = snapshot.label;
            this.weatherTemp = snapshot.tempC;
        });
    }

    /** Used only when Open-Meteo is unreachable. */
    private applyWeatherFallback(): void {
        const hours24 = new Date().getHours();
        if (hours24 >= 20 || hours24 < 6) {
            this.weatherKind = 'moon';
            this.weatherLabel = 'Clear';
        } else if (hours24 < 11) {
            this.weatherKind = 'sun';
            this.weatherLabel = 'Sunny';
        } else {
            this.weatherKind = 'cloudy';
            this.weatherLabel = 'Partly cloudy';
        }
        if (!this.weatherTemp) {
            this.weatherTemp = '—';
        }
    }

    getUser() {
        this.sharedDataService.getUser().subscribe(data => {
            if (data) {
                this.user = data;
            }
        })
    }

    headerShowAvatarImage(): boolean {
        return this.headerProfilePhotoSafeUrl != null;
    }

    headerAvatarUrl(): SafeUrl | null {
        return this.headerProfilePhotoSafeUrl;
    }

    private loadHeaderProfilePhoto(): void {
        this.userService.getCurrentUser().subscribe({
            next: (data) => {
                if (data) {
                    this.user = this.user || data;
                    this.fetchHeaderProfilePhoto(data);
                }
            },
            error: () => {
                if (this.user) {
                    this.fetchHeaderProfilePhoto(this.user);
                }
            },
        });
    }

    private fetchHeaderProfilePhoto(profile: any): void {
        this.revokeHeaderProfilePhoto();
        const userId = Number(profile?.serUserId);
        const path = String(profile?.txtProfilePicturePath || '').trim();
        if (!userId || !path) {
            return;
        }
        this.userService.loadProfilePictureObjectUrl(userId).subscribe({
            next: (objectUrl) => {
                this.headerProfilePhotoBlobUrl = objectUrl;
                this.headerProfilePhotoSafeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
            },
            error: () => {
                this.headerProfilePhotoSafeUrl = null;
            },
        });
    }

    private revokeHeaderProfilePhoto(): void {
        if (this.headerProfilePhotoBlobUrl) {
            URL.revokeObjectURL(this.headerProfilePhotoBlobUrl);
            this.headerProfilePhotoBlobUrl = null;
        }
        this.headerProfilePhotoSafeUrl = null;
    }

    openProfileDrawer(): void {
        this.profileDrawerService.open();
    }

    openProductTour(): void {
        this.productTourService.openTourForCurrentPage();
    }

    openChat(): void {
        this.router.navigateByUrl('chat');
    }

    openDocumentNotifications(): void {
        this.router.navigateByUrl('notification-management');
    }

    async enableChatDesktopNotifications(): Promise<void> {
        await this.chatNotification.ensureBrowserPermission();
        this.chatBrowserNotifyPermission = this.chatNotification.getBrowserPermission();
    }

    private initDocumentNotificationRealtime(): void {
        if (!localStorage.getItem('token')) {
            return;
        }
        this.notificationRealtime.connect();
        this.documentNotificationUnreadSub = this.notificationRealtime.unreadCount$.subscribe((n) => {
            this.documentNotificationUnreadTotal = n;
        });
        this.userNotificationService.getUnreadCount().subscribe({
            next: (count) => this.notificationRealtime.setUnreadTotal(count),
        });
        this.documentNotificationFrameSub = this.notificationRealtime.frame$.subscribe((frame) => {
            if (frame.type !== 'NOTIFICATION' || frame.action !== 'CREATED' || !frame.notification) {
                return;
            }
            this.documentNotificationAlert.notifyIncoming(frame.notification);
        });
    }

    private initChatRealtime(): void {
        if (!localStorage.getItem('token')) {
            return;
        }
        this.chatRealtime.connect();
        this.chatBrowserNotifyPermission = this.chatNotification.getBrowserPermission();
        void this.chatNotification.ensureBrowserPermission().then(() => {
            this.chatBrowserNotifyPermission = this.chatNotification.getBrowserPermission();
        });
        this.chatUnreadSub = this.chatRealtime.totalUnread$.subscribe((n) => {
            this.chatUnreadTotal = n;
        });
        this.chatService.getConversations().subscribe({
            next: (conversations) => {
                const total = (conversations || []).reduce((sum, c) => sum + (c.unreadCount || 0), 0);
                this.chatRealtime.setUnreadTotal(total);
            },
        });
    }

    setActiveDropdown() {
        const selector = document.querySelector('ul.horizontal-menu a[routerLink="' + window.location.pathname + '"]');
        if (selector) {
            selector.classList.add('active');
            const all: any = document.querySelectorAll('ul.horizontal-menu .nav-link.active');
            for (let i = 0; i < all.length; i++) {
                all[0]?.classList.remove('active');
            }
            const ul: any = selector.closest('ul.sub-menu');
            if (ul) {
                let ele: any = ul.closest('li.menu').querySelectorAll('.nav-link');
                if (ele) {
                    ele = ele[0];
                    setTimeout(() => {
                        ele?.classList.add('active');
                    });
                }
            }
        }
    }

    removeNotification(value: number) {
        this.notifications = this.notifications.filter((d) => d.id !== value);
    }

    removeMessage(value: number) {
        this.messages = this.messages.filter((d) => d.id !== value);
    }

    changeLanguage(item: any) {
        this.translate.use(item.code);
        this.appSetting.toggleLanguage(item);
        if (this.store.locale?.toLowerCase() === 'ae') {
            this.storeData.dispatch({type: 'toggleRTL', payload: 'rtl'});
        } else {
            this.storeData.dispatch({type: 'toggleRTL', payload: 'ltr'});
        }
        window.location.reload();
    }

    doLogout() {
        this.router.navigateByUrl('auth/signin');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }
}
