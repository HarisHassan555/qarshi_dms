import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { DocumentShareService } from 'src/app/services/document-share/document-share.service';
import { ChatService, ChatUser } from 'src/app/services/chat/chat.service';
import {
    DOCUMENT_SHARE_CHANNELS,
    DocumentShareChannel,
    buildDocumentShareText,
    canUseNativeShare,
    openShareChannel,
} from 'src/app/utils/document-share-outbound.util';

@Component({
    moduleId: module.id,
    selector: 'app-document-share-link-button',
    templateUrl: './document-share-link-button.component.html',
    styleUrls: ['./document-share-link-button.component.css'],
})
export class DocumentShareLinkButtonComponent {
    @Input() applicationId: number | null | undefined = 0;
    @Input() documentTitle: string | null | undefined = '';
    @Input() buttonClass = 'btn btn-outline-secondary';
    @Input() disabled = false;

    showModal = false;
    isWorking = false;
    shareUrl = '';
    expiresAt: string | null = null;
    expiresInDays = 30;

    showInAppPicker = false;
    inAppUsers: ChatUser[] = [];
    inAppUserSearch = '';
    loadingInAppUsers = false;
    sendingInAppUserId: number | null = null;
    /** Per-channel icon URL override after load error (fallback CDN). */
    private shareIconUrlOverrides: Partial<Record<DocumentShareChannel['id'], string>> = {};

    readonly shareChannels: DocumentShareChannel[] = DOCUMENT_SHARE_CHANNELS.filter(
        (c) => c.id !== 'more' || canUseNativeShare()
    );

    constructor(
        private documentShareService: DocumentShareService,
        private notificationService: NotificationService,
        private chatService: ChatService,
        private router: Router
    ) {}

    get canShare(): boolean {
        return Number(this.applicationId) > 0 && !this.disabled;
    }

    get filteredInAppUsers(): ChatUser[] {
        const q = this.inAppUserSearch.trim().toLowerCase();
        if (!q) {
            return this.inAppUsers;
        }
        return this.inAppUsers.filter(
            (u) =>
                u.displayName?.toLowerCase().includes(q) ||
                u.loginName?.toLowerCase().includes(q) ||
                u.organizationName?.toLowerCase().includes(q)
        );
    }

    openModal(): void {
        if (!this.canShare) {
            return;
        }
        this.showModal = true;
        this.shareUrl = '';
        this.expiresAt = null;
        this.shareIconUrlOverrides = {};
        this.showInAppPicker = false;
        this.inAppUserSearch = '';
        this.inAppUsers = [];
    }

    closeModal(): void {
        this.showModal = false;
        this.showInAppPicker = false;
    }

    async createLink(): Promise<void> {
        if (!this.canShare || this.isWorking) {
            return;
        }
        this.isWorking = true;
        try {
            const response = await firstValueFrom(
                this.documentShareService.createShareLink(Number(this.applicationId), this.expiresInDays)
            );
            if (response?.status !== 'Success' || !response?.token) {
                this.notificationService.showMessage(response?.message || 'Could not create share link.', 'error');
                return;
            }
            this.shareUrl = this.documentShareService.buildShareUrl(String(response.token));
            this.expiresAt = response.expiresAt || null;
            this.notificationService.showMessage('Link ready — choose how to share below.', 'success');
        } catch {
            this.notificationService.showMessage('Could not create share link.', 'error');
        } finally {
            this.isWorking = false;
        }
    }

    async ensureLink(): Promise<boolean> {
        if (this.shareUrl) {
            return true;
        }
        await this.createLink();
        return !!this.shareUrl;
    }

    async copyLink(): Promise<void> {
        if (!(await this.ensureLink())) {
            return;
        }
        try {
            await navigator.clipboard.writeText(this.shareUrl);
            this.notificationService.showMessage('Link copied to clipboard.', 'success');
        } catch {
            this.notificationService.showMessage('Copy failed. Select and copy the link manually.', 'warning');
        }
    }

    async onShareChannel(channel: DocumentShareChannel): Promise<void> {
        if (!(await this.ensureLink())) {
            return;
        }

        if (channel.id === 'copy') {
            await this.copyLink();
            return;
        }

        if (channel.id === 'velocity') {
            this.showInAppPicker = true;
            void this.loadInAppUsers();
            return;
        }

        const result = openShareChannel(channel.id, this.shareUrl, this.documentTitle);
        if (result === 'unsupported') {
            this.notificationService.showMessage('Sharing is not supported on this device. Copy the link instead.', 'warning');
            return;
        }
        if (result === 'opened' || result === 'native') {
            this.notificationService.showMessage(`Opened ${channel.label}.`, 'success');
        }
    }

    private async loadInAppUsers(): Promise<void> {
        if (this.loadingInAppUsers || this.inAppUsers.length > 0) {
            return;
        }
        this.loadingInAppUsers = true;
        try {
            this.inAppUsers = await firstValueFrom(this.chatService.getUsers());
        } catch {
            this.notificationService.showMessage('Could not load users for in-app sharing.', 'error');
        } finally {
            this.loadingInAppUsers = false;
        }
    }

    async sendInApp(user: ChatUser): Promise<void> {
        if (!user?.userId || this.sendingInAppUserId) {
            return;
        }
        if (!(await this.ensureLink())) {
            return;
        }
        const body = buildDocumentShareText(this.shareUrl, this.documentTitle);
        this.sendingInAppUserId = user.userId;
        try {
            const conv = await firstValueFrom(this.chatService.openDirect(user.userId));
            if (!conv?.conversationId) {
                this.notificationService.showMessage('Could not open chat.', 'error');
                return;
            }
            const sent = await firstValueFrom(this.chatService.sendMessage(conv.conversationId, body));
            if (sent?.status !== 'Success') {
                this.notificationService.showMessage('Could not send message.', 'error');
                return;
            }
            this.notificationService.showMessage(`Link sent to ${user.displayName} in Velocity.`, 'success');
            this.closeModal();
            void this.router.navigate(['chat'], { queryParams: { conversationId: conv.conversationId } });
        } catch {
            this.notificationService.showMessage('Could not send link in Velocity.', 'error');
        } finally {
            this.sendingInAppUserId = null;
        }
    }

    userInitial(name?: string): string {
        const n = (name || '?').trim();
        return n ? n.charAt(0).toUpperCase() : '?';
    }

    channelIconUrl(channel: DocumentShareChannel): string {
        return this.shareIconUrlOverrides[channel.id] || channel.iconUrl;
    }

    onShareChannelIconError(channel: DocumentShareChannel): void {
        const fallback = channel.iconFallbackUrl;
        if (!fallback || this.shareIconUrlOverrides[channel.id] === fallback) {
            return;
        }
        this.shareIconUrlOverrides[channel.id] = fallback;
    }
}
