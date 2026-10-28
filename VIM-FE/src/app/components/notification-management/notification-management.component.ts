import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import {
    NotificationRealtimeFrame,
    NotificationRealtimeService,
} from 'src/app/services/user-notification/notification-realtime.service';
import {
    UserNotificationDto,
    UserNotificationService,
} from 'src/app/services/user-notification/user-notification.service';
import { DocumentNotificationAlertService } from 'src/app/services/user-notification/document-notification-alert.service';

@Component({
    selector: 'app-notification-management',
    templateUrl: './notification-management.component.html',
    styleUrls: ['./notification-management.component.css'],
})
export class NotificationManagementComponent implements OnInit, OnDestroy {
    search = '';
    filterUnreadOnly = false;
    loading = false;
    rows: UserNotificationDto[] = [];
    displayRows: UserNotificationDto[] = [];
    documentNotificationSoundEnabled = true;

    cols = [
        { field: 'txtTitle', title: 'Title' },
        { field: 'txtEventType', title: 'Event' },
        { field: 'txtMessage', title: 'Message' },
        { field: 'blnRead', title: 'Read', width: '90px' },
        { field: 'dteCreatedDate', title: 'Created' },
        { field: 'actions', title: 'Actions', width: '120px', sort: false, headerClass: 'justify-center' },
    ];

    private realtimeSub?: Subscription;

    constructor(
        private userNotificationService: UserNotificationService,
        private notificationRealtime: NotificationRealtimeService,
        private notificationService: NotificationService,
        private router: Router,
        private documentNotificationAlert: DocumentNotificationAlertService
    ) {}

    ngOnInit(): void {
        this.documentNotificationSoundEnabled = this.documentNotificationAlert.isSoundEnabled();
        this.notificationRealtime.connect();
        this.realtimeSub = this.notificationRealtime.frame$.subscribe((frame) => this.onRealtimeFrame(frame));
        this.loadNotifications();
    }

    ngOnDestroy(): void {
        this.realtimeSub?.unsubscribe();
    }

    loadNotifications(): void {
        this.loading = true;
        this.userNotificationService
            .getAll({ unreadOnly: this.filterUnreadOnly, limit: 200 })
            .pipe(finalize(() => (this.loading = false)))
            .subscribe({
                next: (data) => {
                    this.rows = data || [];
                    this.displayRows = this.rows;
                },
                error: () => {
                    this.rows = [];
                    this.displayRows = [];
                    this.notificationService.showMessage('Could not load notifications.', 'danger');
                },
            });
    }

    applyFilters(): void {
        this.loadNotifications();
    }

    clearFilters(): void {
        this.search = '';
        this.filterUnreadOnly = false;
        this.loadNotifications();
    }

    markRead(row: UserNotificationDto): void {
        if (!row?.serUserNotificationId || row.blnRead) {
            return;
        }
        this.userNotificationService.markRead(row.serUserNotificationId).subscribe({
            next: () => this.loadNotifications(),
            error: () => this.notificationService.showMessage('Could not mark as read.', 'danger'),
        });
    }

    markAllRead(): void {
        this.userNotificationService.markAllRead().subscribe({
            next: (count) => {
                this.notificationRealtime.setUnreadTotal(count);
                this.loadNotifications();
            },
            error: () => this.notificationService.showMessage('Could not mark all as read.', 'danger'),
        });
    }

    deleteRow(row: UserNotificationDto): void {
        if (!row?.serUserNotificationId) {
            return;
        }
        this.userNotificationService.delete(row.serUserNotificationId).subscribe({
            next: () => {
                this.notificationService.showMessage('Notification removed.', 'success');
                this.loadNotifications();
                this.userNotificationService.getUnreadCount().subscribe((c) => this.notificationRealtime.setUnreadTotal(c));
            },
            error: () => this.notificationService.showMessage('Could not delete notification.', 'danger'),
        });
    }

    openDeepLink(row: UserNotificationDto): void {
        const link = (row.txtDeepLink || '').trim();
        if (!link) {
            return;
        }
        if (!row.blnRead && row.serUserNotificationId) {
            this.userNotificationService.markRead(row.serUserNotificationId).subscribe({
                next: () => this.navigateDeepLink(link),
            });
            return;
        }
        this.navigateDeepLink(link);
    }

    readLabel(row: UserNotificationDto): string {
        return row.blnRead ? 'Read' : 'Unread';
    }

    readBadgeClass(row: UserNotificationDto): string {
        return row.blnRead ? 'badge-outline-secondary' : 'badge-outline-primary';
    }

    setDocumentNotificationSound(enabled: boolean): void {
        this.documentNotificationSoundEnabled = enabled;
        this.documentNotificationAlert.setSoundEnabled(enabled);
        if (enabled) {
            this.documentNotificationAlert.playAlertTone();
        }
    }

    private navigateDeepLink(link: string): void {
        const path = link.replace(/^\/+/, '').replace(/^velocity\/?/i, '');
        this.router.navigateByUrl('/' + path);
    }

    private onRealtimeFrame(frame: NotificationRealtimeFrame): void {
        if (frame.type === 'NOTIFICATION' || frame.type === 'SYNC') {
            this.loadNotifications();
        }
    }
}
