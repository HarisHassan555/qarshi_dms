import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import {
  ChatConversation,
  ChatDeliveryStatus,
  ChatMessage,
  ChatService,
  ChatUser,
} from 'src/app/services/chat/chat.service';
import { ChatRealtimeService } from 'src/app/services/chat/chat-realtime.service';
import { ChatNotificationService } from 'src/app/services/chat/chat-notification.service';
import { UserService } from 'src/app/services/user/user.service';

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css'],
})
export class ChatComponent implements OnInit, OnDestroy {
  users: ChatUser[] = [];
  conversations: ChatConversation[] = [];
  messages: ChatMessage[] = [];
  selectedConversation: ChatConversation | null = null;
  selectedUserId: number | null = null;
  selectedPeer: ChatUser | null = null;
  selectedPeerName = '';
  selectedPeerOrgLine = '';
  draft = '';
  loadingUsers = false;
  loadingMessages = false;
  openingChat = false;
  sending = false;
  chatError = '';
  userSearch = '';
  conversationSearch = '';
  showNewChat = false;
  chatSoundEnabled = true;
  browserNotifyPermission: NotificationPermission | 'unsupported' = 'default';
  @ViewChild('messagesPane') messagesPane?: ElementRef<HTMLElement>;
  private messageSub?: Subscription;
  private readReceiptSub?: Subscription;
  private deliveryReceiptSub?: Subscription;
  private pendingClientMessageId = 0;
  private messagesLoadSeq = 0;
  /** userId → blob URL, or null if load failed / no photo */
  private profilePhotoUrls: Record<number, string | null> = {};
  private profilePhotoSafeUrls: Record<number, SafeUrl> = {};
  private profilePhotoRequested = new Set<number>();

  constructor(
    private chatService: ChatService,
    private chatRealtime: ChatRealtimeService,
    private chatNotification: ChatNotificationService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.chatSoundEnabled = this.chatNotification.isSoundEnabled();
    this.browserNotifyPermission = this.chatNotification.getBrowserPermission();
    this.chatRealtime.connect();
    this.loadUsers();
    this.loadConversations();
    this.route.queryParamMap.subscribe((params) => {
      const id = Number(params.get('conversationId'));
      if (id > 0) {
        this.openConversationById(id);
      }
    });
    this.messageSub = this.chatRealtime.message$.subscribe((msg) => {
      this.onIncomingMessage(msg);
      this.cdr.detectChanges();
      this.scrollMessagesToBottom();
    });
    this.readReceiptSub = this.chatRealtime.readReceipt$.subscribe((receipt) =>
      this.applyReadReceipt(receipt)
    );
    this.deliveryReceiptSub = this.chatRealtime.deliveryReceipt$.subscribe((receipt) =>
      this.applyDeliveryReceipt(receipt)
    );
  }

  ngOnDestroy(): void {
    this.messageSub?.unsubscribe();
    this.readReceiptSub?.unsubscribe();
    this.deliveryReceiptSub?.unsubscribe();
    this.chatRealtime.setActiveConversationId(null);
    Object.values(this.profilePhotoUrls).forEach((url) => {
      if (url) {
        URL.revokeObjectURL(url);
      }
    });
    this.profilePhotoSafeUrls = {};
  }

  get filteredConversations(): ChatConversation[] {
    const q = this.conversationSearch.trim().toLowerCase();
    if (!q) {
      return this.conversations;
    }
    return this.conversations.filter(
      (c) =>
        c.otherUserName?.toLowerCase().includes(q) ||
        c.lastMessageBody?.toLowerCase().includes(q)
    );
  }

  get filteredUsers(): ChatUser[] {
    const q = this.userSearch.trim().toLowerCase();
    if (!q) {
      return this.users;
    }
    return this.users.filter(
      (u) =>
        u.displayName?.toLowerCase().includes(q) ||
        u.loginName?.toLowerCase().includes(q)
    );
  }

  async enableBrowserNotifications(): Promise<void> {
    const granted = await this.chatNotification.ensureBrowserPermission();
    this.browserNotifyPermission = this.chatNotification.getBrowserPermission();
    if (!granted && this.browserNotifyPermission === 'denied') {
      this.chatError = 'Notifications are blocked in the browser. Allow them in site settings.';
    }
    this.cdr.detectChanges();
  }

  setChatSound(enabled: boolean): void {
    this.chatSoundEnabled = enabled;
    this.chatNotification.setSoundEnabled(enabled);
    if (enabled) {
      this.chatNotification.playRingtone();
    }
  }

  openNewChat(): void {
    this.userSearch = '';
    this.showNewChat = true;
    if (!this.users.length && !this.loadingUsers) {
      this.loadUsers();
    }
  }

  closeNewChat(): void {
    this.showNewChat = false;
    this.userSearch = '';
  }

  loadUsers(): void {
    this.loadingUsers = true;
    this.chatService.getUsers().subscribe({
      next: (data) => {
        this.users = (data || []).map((u) => this.normalizeChatUser(u));
        this.loadingUsers = false;
        this.prefetchUserPhotos(this.users);
        this.syncSelectedPeer();
      },
      error: () => {
        this.loadingUsers = false;
      },
    });
  }

  loadConversations(): void {
    this.chatService.getConversations().subscribe({
      next: (data) => {
        this.conversations = (data || []).map((c) => this.normalizeConversation(c));
        this.prefetchConversationPhotos(this.conversations);
        const total = this.conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
        this.chatRealtime.setUnreadTotal(total);
        const deepLinkId = Number(this.route.snapshot.queryParamMap.get('conversationId'));
        if (deepLinkId > 0) {
          this.openConversationById(deepLinkId);
        }
      },
    });
  }

  openConversationById(conversationId: number): void {
    const conv = this.conversations.find((c) => Number(c.conversationId) === conversationId);
    if (conv) {
      this.selectConversation(conv);
    }
  }

  startChatWithUser(user: ChatUser): void {
    this.openChatWithUserId(user.userId, user.displayName, user);
  }

  openChatWithUserId(userId: number, displayName?: string, user?: ChatUser): void {
    if (!userId) {
      return;
    }
    this.chatError = '';
    this.selectedUserId = userId;
    const existing = this.conversations.find((c) => c.otherUserId === userId);
    if (existing) {
      this.closeNewChat();
      if (!this.isActiveConversation(existing.conversationId)) {
        this.selectConversation(existing);
      }
      return;
    }
    this.openingChat = true;
    this.chatService.openDirect(userId).subscribe({
      next: (conv) => {
        this.openingChat = false;
        if (!conv) {
          this.chatError = 'Could not start chat. Please try again.';
          return;
        }
        const normalized = this.normalizeConversation(conv);
        if (!normalized.otherUserName && displayName) {
          normalized.otherUserName = displayName;
        }
        if (user?.hasProfilePicture && !normalized.otherUserHasProfilePicture) {
          normalized.otherUserHasProfilePicture = true;
        }
        this.upsertConversation(normalized);
        this.closeNewChat();
        this.selectConversation(normalized);
      },
      error: (err) => {
        this.openingChat = false;
        this.chatError =
          err?.error?.message ||
          'Could not open chat. Run create_chat_tables.sql on the database and restart the API.';
      },
    });
  }

  isUserActive(userId?: number): boolean {
    if (!userId) {
      return false;
    }
    return this.selectedUserId === userId || this.selectedConversation?.otherUserId === userId;
  }

  userInitial(name?: string): string {
    const n = (name || '?').trim();
    return n ? n.charAt(0).toUpperCase() : '?';
  }

  showAvatarImage(userId: number | null | undefined, hasProfilePicture?: boolean): boolean {
    const id = Number(userId);
    if (!id || !hasProfilePicture) {
      return false;
    }
    return !!this.profilePhotoSafeUrls[id];
  }

  getAvatarImageUrl(userId: number | null | undefined): SafeUrl | null {
    const id = Number(userId);
    if (!id) {
      return null;
    }
    return this.profilePhotoSafeUrls[id] || null;
  }

  ensureProfilePhoto(userId: number | null | undefined, hasProfilePicture?: boolean): void {
    const id = Number(userId);
    if (!id || !hasProfilePicture || this.profilePhotoRequested.has(id)) {
      return;
    }
    this.profilePhotoRequested.add(id);
    this.userService.loadProfilePictureObjectUrl(id).subscribe({
      next: (objectUrl) => {
        this.profilePhotoUrls[id] = objectUrl;
        this.profilePhotoSafeUrls[id] = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        this.cdr.detectChanges();
      },
      error: () => {
        this.profilePhotoUrls[id] = null;
        delete this.profilePhotoSafeUrls[id];
        this.cdr.detectChanges();
      },
      complete: () => {
        if (this.profilePhotoUrls[id] === undefined) {
          this.profilePhotoUrls[id] = null;
          this.cdr.detectChanges();
        }
      },
    });
  }

  private prefetchConversationPhotos(conversations: ChatConversation[]): void {
    (conversations || []).forEach((c) =>
      this.ensureProfilePhoto(c.otherUserId, c.otherUserHasProfilePicture)
    );
  }

  private prefetchUserPhotos(users: ChatUser[]): void {
    (users || []).forEach((u) => this.ensureProfilePhoto(u.userId, u.hasProfilePicture));
  }

  selectedPeerHasProfilePicture(): boolean {
    if (this.selectedPeer?.hasProfilePicture) {
      return true;
    }
    return !!this.selectedConversation?.otherUserHasProfilePicture;
  }

  selectConversation(conv: ChatConversation): void {
    const conversationId = Number(conv.conversationId);
    const sameChat = this.sameConversationId(this.selectedConversation?.conversationId, conversationId);
    this.selectedConversation = conv;
    this.selectedUserId = conv.otherUserId ?? null;
    this.syncSelectedPeer();
    this.ensureProfilePhoto(this.selectedUserId, this.selectedPeerHasProfilePicture());
    this.chatError = '';
    if (!sameChat) {
      this.messages = [];
    }
    this.chatRealtime.setActiveConversationId(conversationId);
    this.loadingMessages = true;
    const loadSeq = ++this.messagesLoadSeq;
    this.chatService.getMessages(conversationId).subscribe({
      next: (msgs) => {
        if (loadSeq !== this.messagesLoadSeq) {
          return;
        }
        if (!this.isActiveConversation(conversationId)) {
          this.loadingMessages = false;
          return;
        }
        const loaded = this.parseMessageList(msgs);
        const pending = this.messages.filter((m) => m.messageId < 0);
        this.setThreadMessages(this.mergeLoadedAndPending(loaded, pending));
        this.loadingMessages = false;
        const last = this.messages[this.messages.length - 1];
        if (last?.messageId && last.messageId > 0) {
          this.chatService.markRead(conversationId, last.messageId).subscribe(() => {
            conv.unreadCount = 0;
            this.syncUnreadBadge();
          });
        }
      },
      error: () => {
        if (loadSeq !== this.messagesLoadSeq) {
          return;
        }
        this.loadingMessages = false;
        this.chatError = 'Could not load messages.';
        this.cdr.detectChanges();
      },
    });
  }

  send(): void {
    const text = this.draft.trim();
    const conv = this.selectedConversation;
    if (!text || !conv || this.sending) {
      return;
    }
    this.draft = '';
    this.sending = true;
    this.chatError = '';

    const tempId = --this.pendingClientMessageId;
    const optimistic = this.buildOutgoingMessage(conv.conversationId, text, tempId);
    this.upsertMessageInThread(optimistic);
    this.updateConversationPreview(conv, text, optimistic.sentAt);

    this.chatService.sendMessage(conv.conversationId, text).subscribe({
      next: (res) => {
        this.sending = false;
        const saved = this.extractSavedMessage(res);
        if (saved) {
          this.upsertMessageInThread(saved, tempId);
          this.updateConversationPreview(conv, saved.body, saved.sentAt);
        } else {
          this.syncActiveThreadFromServer();
        }
      },
      error: () => {
        this.sending = false;
        this.removeMessage(tempId);
        this.chatError = 'Message failed to send.';
        this.draft = text;
        this.cdr.detectChanges();
      },
    });
  }

  onIncomingMessage(msg: ChatMessage): void {
    const normalized = this.normalizeMessage(msg);
    if (this.isMine(normalized)) {
      this.upsertMessageInThread(normalized);
      return;
    }
    const conv = this.conversations.find((c) =>
      this.sameConversationId(c.conversationId, normalized.conversationId)
    );
    if (conv) {
      conv.lastMessageBody = normalized.body;
      conv.lastMessageAt = normalized.sentAt;
    }
    if (this.isActiveConversation(normalized.conversationId)) {
      this.upsertMessageInThread(normalized);
      if (normalized.messageId > 0) {
        this.chatService.markRead(normalized.conversationId, normalized.messageId).subscribe();
      }
    } else if (conv) {
      conv.unreadCount = (conv.unreadCount || 0) + 1;
      this.syncUnreadBadge();
    }
  }

  private upsertMessageInThread(msg: ChatMessage, replaceTempId?: number): void {
    const normalized = this.parseMessageFromApi(msg) ?? this.normalizeMessage(msg);
    if (!normalized.body?.trim() || !this.isActiveConversation(normalized.conversationId)) {
      return;
    }
    if (this.isMine(normalized) && normalized.messageId > 0 && !normalized.deliveryStatus) {
      normalized.deliveryStatus = 'sent';
    }

    let list = [...this.messages];
    if (replaceTempId != null) {
      const tempIdx = list.findIndex((m) => m.messageId === replaceTempId);
      if (tempIdx >= 0) {
        list[tempIdx] = this.withMergedDeliveryStatus(list[tempIdx], normalized);
      } else {
        list = this.addOrReplaceById(list, normalized);
      }
    } else {
      list = this.addOrReplaceById(list, normalized);
    }
    this.setThreadMessages(list);
  }

  private addOrReplaceById(list: ChatMessage[], msg: ChatMessage): ChatMessage[] {
    if (msg.messageId > 0) {
      const existingIdx = list.findIndex((m) => m.messageId === msg.messageId);
      if (existingIdx >= 0) {
        const next = [...list];
        next[existingIdx] = this.withMergedDeliveryStatus(next[existingIdx], msg);
        return next;
      }
    }
    const pendingIdx = list.findIndex(
      (m) =>
        m.messageId < 0 &&
        m.body === msg.body &&
        this.sameConversationId(m.conversationId, msg.conversationId) &&
        Number(m.senderUserId) === Number(msg.senderUserId)
    );
    if (pendingIdx >= 0) {
      const next = [...list];
      next[pendingIdx] = msg;
      return next;
    }
    return [...list, msg];
  }

  private setThreadMessages(list: ChatMessage[]): void {
    this.messages = this.sortMessages(list);
    this.cdr.detectChanges();
    this.scrollMessagesToBottom();
  }

  private removeMessage(messageId: number): void {
    this.messages = this.messages.filter((m) => m.messageId !== messageId);
  }

  private syncActiveThreadFromServer(): void {
    const conv = this.selectedConversation;
    if (!conv) {
      return;
    }
    const conversationId = Number(conv.conversationId);
    this.chatService.getMessages(conversationId).subscribe({
      next: (msgs) => {
        if (!this.isActiveConversation(conversationId)) {
          return;
        }
        const loaded = this.parseMessageList(msgs);
        const pending = this.messages.filter((m) => m.messageId < 0);
        this.setThreadMessages(this.mergeLoadedAndPending(loaded, pending));
      },
    });
  }

  private buildOutgoingMessage(conversationId: number, body: string, tempId: number): ChatMessage {
    return {
      messageId: tempId,
      conversationId,
      senderUserId: this.currentUserId() ?? 0,
      body,
      sentAt: Date.now(),
      deliveryStatus: 'pending',
    };
  }

  applyReadReceipt(receipt: {
    conversationId: number;
    lastReadMessageId: number;
    readerUserId: number;
  }): void {
    if (!this.isActiveConversation(receipt.conversationId)) {
      return;
    }
    const myId = this.currentUserId();
    if (myId != null && Number(receipt.readerUserId) === myId) {
      return;
    }
    this.messages = this.messages.map((m) => {
      if (!this.isMine(m) || m.messageId <= 0 || m.messageId > receipt.lastReadMessageId) {
        return m;
      }
      return { ...m, deliveryStatus: this.mergeDeliveryStatus(m.deliveryStatus, 'read') };
    });
    this.cdr.detectChanges();
  }

  applyDeliveryReceipt(receipt: { conversationId: number; messageId: number }): void {
    if (!this.isActiveConversation(receipt.conversationId)) {
      return;
    }
    this.messages = this.messages.map((m) => {
      if (!this.isMine(m) || m.messageId !== receipt.messageId) {
        return m;
      }
      return { ...m, deliveryStatus: this.mergeDeliveryStatus(m.deliveryStatus, 'delivered') };
    });
    this.cdr.detectChanges();
  }

  private mergeDeliveryStatus(
    current?: ChatDeliveryStatus,
    incoming?: ChatDeliveryStatus
  ): ChatDeliveryStatus {
    const rank: Record<ChatDeliveryStatus, number> = {
      pending: 0,
      sent: 1,
      delivered: 2,
      read: 3,
    };
    const a = current && rank[current] != null ? current : 'sent';
    const b = incoming && rank[incoming] != null ? incoming : 'sent';
    return rank[a] >= rank[b] ? a : b;
  }

  messageDeliveryStatus(msg: ChatMessage): ChatDeliveryStatus {
    if (!this.isMine(msg)) {
      return 'sent';
    }
    if (msg.messageId < 0) {
      return 'pending';
    }
    const status = msg.deliveryStatus;
    if (status === 'read' || status === 'delivered' || status === 'sent') {
      return status;
    }
    return 'delivered';
  }

  showDoubleTick(msg: ChatMessage): boolean {
    const s = this.messageDeliveryStatus(msg);
    return s === 'delivered' || s === 'read';
  }

  ticksAreRead(msg: ChatMessage): boolean {
    return this.messageDeliveryStatus(msg) === 'read';
  }

  ticksAreDelivered(msg: ChatMessage): boolean {
    return this.messageDeliveryStatus(msg) === 'delivered';
  }

  ticksAreSent(msg: ChatMessage): boolean {
    return this.messageDeliveryStatus(msg) === 'sent';
  }

  tickStatusLabel(msg: ChatMessage): string {
    switch (this.messageDeliveryStatus(msg)) {
      case 'pending':
        return 'Sending…';
      case 'sent':
        return 'Sent';
      case 'delivered':
        return 'Delivered';
      case 'read':
        return 'Read';
      default:
        return '';
    }
  }

  private withMergedDeliveryStatus(existing: ChatMessage, incoming: ChatMessage): ChatMessage {
    if (!this.isMine(incoming)) {
      return incoming;
    }
    return {
      ...incoming,
      deliveryStatus: this.mergeDeliveryStatus(existing.deliveryStatus, incoming.deliveryStatus),
    };
  }

  private extractSavedMessage(res: unknown): ChatMessage | null {
    if (!res || typeof res !== 'object') {
      return null;
    }
    const body = res as Record<string, unknown>;
    if (body['message'] != null && typeof body['message'] === 'object') {
      return this.parseMessageFromApi(body['message']);
    }
    return this.parseMessageFromApi(res);
  }

  private parseMessageList(msgs: ChatMessage[] | null | undefined): ChatMessage[] {
    return (msgs || [])
      .map((m) => this.parseMessageFromApi(m))
      .filter((m): m is ChatMessage => m != null);
  }

  private parseMessageFromApi(raw: unknown): ChatMessage | null {
    if (!raw || typeof raw !== 'object') {
      return null;
    }
    const data = raw as Record<string, unknown>;
    const text = data['body'] ?? data['txtBody'];
    if (text == null || String(text).trim() === '') {
      return null;
    }
    const messageId = data['messageId'] ?? data['serMessageId'];
    const conversationId = data['conversationId'] ?? data['serConversationId'];
    const senderUserId = data['senderUserId'] ?? data['serSenderUserId'];
    let sentAt = data['sentAt'];
    if (sentAt == null && data['dteCreatedDate'] != null) {
      const created = data['dteCreatedDate'];
      sentAt = typeof created === 'number' ? created : Date.parse(String(created));
    }
    const parsedMessageId = messageId != null ? Number(messageId) : 0;
    const parsedConversationId = Number(conversationId);
    if (!Number.isFinite(parsedConversationId)) {
      return null;
    }
    const deliveryRaw = data['deliveryStatus'];
    let deliveryStatus: ChatDeliveryStatus | undefined;
    if (deliveryRaw === 'read' || deliveryRaw === 'delivered' || deliveryRaw === 'sent' || deliveryRaw === 'pending') {
      deliveryStatus = deliveryRaw;
    }
    return {
      messageId: Number.isFinite(parsedMessageId) ? parsedMessageId : 0,
      conversationId: parsedConversationId,
      senderUserId: Number(senderUserId ?? 0),
      senderName: (data['senderName'] ?? data['txtSenderName']) as string | undefined,
      body: String(text),
      sentAt:
        sentAt != null && Number.isFinite(Number(sentAt)) ? Number(sentAt) : Date.now(),
      deliveryStatus,
    };
  }

  private isActiveConversation(conversationId: number): boolean {
    return this.sameConversationId(this.selectedConversation?.conversationId, conversationId);
  }

  private sameConversationId(a?: number | null, b?: number | null): boolean {
    if (a == null || b == null) {
      return false;
    }
    return Number(a) === Number(b);
  }

  private mergeLoadedAndPending(loaded: ChatMessage[], pending: ChatMessage[]): ChatMessage[] {
    const stillPending = pending.filter(
      (p) =>
        !loaded.some(
          (s) =>
            s.body === p.body &&
            this.sameConversationId(s.conversationId, p.conversationId) &&
            Number(s.senderUserId) === Number(p.senderUserId)
        )
    );
    return this.sortMessages([...loaded, ...stillPending]);
  }

  private sortMessages(list: ChatMessage[]): ChatMessage[] {
    return [...list].sort((a, b) => {
      const ta = a.sentAt ?? 0;
      const tb = b.sentAt ?? 0;
      if (ta !== tb) {
        return ta - tb;
      }
      return a.messageId - b.messageId;
    });
  }

  private normalizeMessage(msg: ChatMessage): ChatMessage {
    const parsed = this.parseMessageFromApi(msg);
    if (parsed) {
      return parsed;
    }
    return {
      messageId: Number(msg.messageId) || 0,
      conversationId: Number(msg.conversationId),
      senderUserId: Number(msg.senderUserId ?? 0),
      senderName: msg.senderName,
      body: msg.body,
      sentAt: msg.sentAt != null ? Number(msg.sentAt) : Date.now(),
    };
  }

  private updateConversationPreview(conv: ChatConversation, body: string, sentAt?: number): void {
    conv.lastMessageBody = body;
    conv.lastMessageAt = sentAt ?? Date.now();
  }

  private normalizeChatUser(raw: ChatUser): ChatUser {
    const anyRaw = raw as ChatUser & { isHasProfilePicture?: boolean };
    return {
      ...raw,
      userId: Number(raw.userId),
      hasProfilePicture: !!(raw.hasProfilePicture || anyRaw.isHasProfilePicture),
    };
  }

  private normalizeConversation(raw: ChatConversation): ChatConversation {
    const anyRaw = raw as ChatConversation & { isOtherUserHasProfilePicture?: boolean };
    return {
      ...raw,
      conversationId: Number(raw.conversationId),
      otherUserId: Number(raw.otherUserId),
      otherUserHasProfilePicture: !!(
        raw.otherUserHasProfilePicture || anyRaw.isOtherUserHasProfilePicture
      ),
    };
  }

  private currentUserId(): number | null {
    try {
      const raw = localStorage.getItem('user');
      const user = raw ? JSON.parse(raw) : null;
      const id = user?.serUserId ?? user?.userId ?? user?.id;
      return id != null ? Number(id) : null;
    } catch {
      return null;
    }
  }

  private scrollMessagesToBottom(): void {
    setTimeout(() => {
      const el = this.messagesPane?.nativeElement;
      if (el) {
        el.scrollTop = el.scrollHeight;
      }
    }, 0);
  }

  private upsertConversation(conv: ChatConversation): void {
    const normalized = this.normalizeConversation(conv);
    const idx = this.conversations.findIndex((c) => c.conversationId === normalized.conversationId);
    if (idx >= 0) {
      this.conversations[idx] = normalized;
    } else {
      this.conversations = [normalized, ...this.conversations];
    }
    this.ensureProfilePhoto(normalized.otherUserId, normalized.otherUserHasProfilePicture);
  }

  private syncUnreadBadge(): void {
    const total = this.conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
    this.chatRealtime.setUnreadTotal(total);
  }

  isMine(msg: ChatMessage): boolean {
    const currentId = this.currentUserId();
    return currentId != null && currentId === Number(msg.senderUserId);
  }

  formatTime(ts?: number): string {
    if (!ts) {
      return '';
    }
    return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  trackMessage(_index: number, msg: ChatMessage): number {
    return msg.messageId;
  }

  private syncSelectedPeer(): void {
    const userId = this.selectedConversation?.otherUserId;
    this.selectedPeer = userId ? this.users.find((u) => u.userId === userId) || null : null;
    this.selectedPeerName =
      this.selectedPeer?.displayName || this.selectedConversation?.otherUserName || 'User';
    if (!this.selectedPeer) {
      this.selectedPeerOrgLine = '';
      return;
    }
    this.selectedPeerOrgLine = [
      this.selectedPeer.organizationName,
      this.selectedPeer.divisionName,
      this.selectedPeer.departmentName,
    ]
      .map((part) => (part || '').trim())
      .filter((part) => !!part)
      .join('  ·  ');
  }

}
