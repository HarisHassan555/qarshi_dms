import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { NotificationService } from 'src/app/NotificationService';
import {
    EmailDeliveryLogRow,
    EmailDeliveryLogService,
} from 'src/app/services/email-delivery-log/email-delivery-log.service';

@Component({
    selector: 'app-email-delivery-log',
    templateUrl: './email-delivery-log.component.html',
    styleUrls: ['./email-delivery-log.component.css'],
})
export class EmailDeliveryLogComponent implements OnInit {
    search = '';
    loading = false;
    rows: EmailDeliveryLogRow[] = [];
    displayRows: EmailDeliveryLogRow[] = [];
    total = 0;
    page = 0;
    pageSize = 50;

    filterStatus = '';
    filterTriggerType = '';
    filterRecipient = '';
    filterApplicationId: number | null = null;

    previewOpen = false;
    previewLoading = false;
    previewRow: EmailDeliveryLogRow | null = null;
    previewHtmlSafe: SafeHtml | null = null;

    cols = [
        { field: 'sentAt', title: 'Sent at', width: '170px' },
        { field: 'status', title: 'Status', width: '90px' },
        { field: 'triggerType', title: 'Trigger', width: '160px' },
        { field: 'recipientEmail', title: 'Recipient' },
        { field: 'subject', title: 'Subject' },
        { field: 'applicationId', title: 'App ID', width: '90px' },
        { field: 'actions', title: 'Actions', width: '100px', sort: false, headerClass: 'justify-center' },
    ];

    readonly statusOptions = ['', 'SENT', 'FAILED', 'SKIPPED'];

    constructor(
        private emailDeliveryLogService: EmailDeliveryLogService,
        private notificationService: NotificationService,
        private sanitizer: DomSanitizer
    ) {}

    ngOnInit(): void {
        this.loadRows();
    }

    loadRows(): void {
        this.loading = true;
        this.emailDeliveryLogService
            .search({
                status: this.filterStatus || undefined,
                triggerType: this.filterTriggerType || undefined,
                recipient: this.filterRecipient || undefined,
                applicationId: this.filterApplicationId,
                page: this.page,
                size: this.pageSize,
            })
            .subscribe({
                next: (response) => {
                    this.rows = response?.items || [];
                    this.displayRows = this.rows;
                    this.total = response?.total ?? this.rows.length;
                    this.loading = false;
                },
                error: () => {
                    this.rows = [];
                    this.displayRows = [];
                    this.loading = false;
                    this.notificationService.showMessage('Could not load email delivery log.', 'danger');
                },
            });
    }

    applyFilters(): void {
        this.page = 0;
        this.loadRows();
    }

    clearFilters(): void {
        this.filterStatus = '';
        this.filterTriggerType = '';
        this.filterRecipient = '';
        this.filterApplicationId = null;
        this.search = '';
        this.page = 0;
        this.loadRows();
    }

    statusBadgeClass(row: EmailDeliveryLogRow): string {
        const status = String(row?.status || '').toUpperCase();
        if (status === 'SENT') {
            return 'bg-success';
        }
        if (status === 'FAILED') {
            return 'bg-danger';
        }
        return 'bg-secondary';
    }

    openPreview(row: EmailDeliveryLogRow): void {
        if (!row?.id) {
            return;
        }
        this.previewOpen = true;
        this.previewLoading = true;
        this.previewRow = row;
        this.previewHtmlSafe = null;
        this.emailDeliveryLogService.getById(row.id).subscribe({
            next: (detail) => {
                this.previewRow = detail;
                const html = this.rewriteSignInLinks(detail?.bodyHtml || detail?.bodyText || '');
                this.previewHtmlSafe = this.sanitizer.bypassSecurityTrustHtml(
                    html || '<p><em>No body stored for this message.</em></p>'
                );
                this.previewLoading = false;
            },
            error: () => {
                this.previewLoading = false;
                this.notificationService.showMessage('Could not load email details.', 'danger');
            },
        });
    }

    closePreview(): void {
        this.previewOpen = false;
        this.previewRow = null;
        this.previewHtmlSafe = null;
    }

    /** Older messages stored a localhost sign-in link. Show the address of this server instead. */
    private rewriteSignInLinks(html: string): string {
        const signInUrl = this.currentSignInUrl();
        if (!html || !signInUrl) {
            return html;
        }
        return html.replace(
            /href\s*=\s*(["'])(https?:\/\/[^"']+)\1/gi,
            (full, quote: string, rawUrl: string) => {
                let parsed: URL;
                try {
                    parsed = new URL(rawUrl);
                } catch {
                    return full;
                }
                const path = parsed.pathname.replace(/\/+$/, '');
                if (!/\/auth\/(?:login|signin)$/i.test(path)) {
                    return full;
                }
                if (rawUrl.replace(/\/+$/, '') === signInUrl) {
                    return full;
                }
                return `href=${quote}${signInUrl}${quote}`;
            }
        );
    }

    private currentSignInUrl(): string {
        if (typeof window === 'undefined') {
            return '';
        }
        const baseHref = document.querySelector('base')?.getAttribute('href') || '/velocity/';
        let basePath = baseHref;
        if (/^https?:\/\//i.test(baseHref)) {
            try {
                basePath = new URL(baseHref).pathname;
            } catch {
                basePath = '/velocity/';
            }
        }
        const prefix = ('/' + basePath.replace(/^\/+|\/+$/g, '')).replace(/\/$/, '');
        const path = prefix === '/' ? '' : prefix;
        return `${window.location.origin}${path}/auth/signin`;
    }
}