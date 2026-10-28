import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { urls } from 'src/app/utils/urls';
import { ChatMessage } from './chat.service';
import { ChatNotificationService } from './chat-notification.service';

@Injectable({ providedIn: 'root' })
export class ChatRealtimeService {
  constructor(
    private ngZone: NgZone,
    private chatNotification: ChatNotificationService
  ) {}
  private socket: WebSocket | null = null;
  private reconnectTimer: number | null = null;
  private activeConversationId: number | null = null;

  readonly message$ = new Subject<ChatMessage>();
  readonly readReceipt$ = new Subject<{
    conversationId: number;
    lastReadMessageId: number;
    readerUserId: number;
  }>();
  readonly deliveryReceipt$ = new Subject<{
    conversationId: number;
    messageId: number;
  }>();
  readonly totalUnread$ = new Subject<number>();

  private unreadTotal = 0;

  setActiveConversationId(id: number | null): void {
    this.activeConversationId = id;
  }

  getUnreadTotal(): number {
    return this.unreadTotal;
  }

  connect(): void {
    const token = localStorage.getItem('token');
    if (!token || this.socket?.readyState === WebSocket.OPEN) {
      return;
    }
    if (this.socket?.readyState === WebSocket.CONNECTING) {
      return;
    }
    this.disconnect(false);
    const url = this.buildWebSocketUrl(token);
    this.socket = new WebSocket(url);
    this.socket.onopen = () => {
      if (this.reconnectTimer !== null) {
        window.clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }
    };
    this.socket.onmessage = (event) => this.handleFrame(event.data);
    this.socket.onclose = () => this.scheduleReconnect();
    this.socket.onerror = () => this.scheduleReconnect();
  }

  disconnect(clearUnread = true): void {
    if (this.reconnectTimer !== null) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.close();
      this.socket = null;
    }
    if (clearUnread) {
      this.unreadTotal = 0;
      this.totalUnread$.next(0);
    }
  }

  send(conversationId: number, body: string): void {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      return;
    }
    this.socket.send(JSON.stringify({ type: 'SEND', conversationId, body }));
  }

  setUnreadTotal(total: number): void {
    this.unreadTotal = Math.max(0, total);
    this.totalUnread$.next(this.unreadTotal);
  }

  bumpUnread(): void {
    this.unreadTotal += 1;
    this.totalUnread$.next(this.unreadTotal);
  }

  private scheduleReconnect(): void {
    if (!localStorage.getItem('token')) {
      return;
    }
    if (this.reconnectTimer !== null) {
      return;
    }
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, 4000);
  }

  private handleFrame(raw: string): void {
    try {
      const frame = JSON.parse(raw) as { type?: string; payload?: unknown };
      if (frame.type === 'READ_RECEIPT' && frame.payload && typeof frame.payload === 'object') {
        const p = frame.payload as Record<string, unknown>;
        const conversationId = Number(p['conversationId']);
        const lastReadMessageId = Number(p['lastReadMessageId']);
        const readerUserId = Number(p['readerUserId']);
        if (conversationId > 0 && lastReadMessageId > 0) {
          this.ngZone.run(() =>
            this.readReceipt$.next({ conversationId, lastReadMessageId, readerUserId })
          );
        }
        return;
      }
      if (frame.type === 'DELIVERY_RECEIPT' && frame.payload && typeof frame.payload === 'object') {
        const p = frame.payload as Record<string, unknown>;
        const conversationId = Number(p['conversationId']);
        const messageId = Number(p['messageId']);
        if (conversationId > 0 && messageId > 0) {
          this.ngZone.run(() => this.deliveryReceipt$.next({ conversationId, messageId }));
        }
        return;
      }
      if (frame.type !== 'NEW_MESSAGE' || !frame.payload) {
        return;
      }
      const msg = this.normalizePayload(frame.payload as ChatMessage);
      this.ngZone.run(() => {
        const isOwn = this.isCurrentUser(msg.senderUserId);
        if (isOwn) {
          this.message$.next(msg);
          return;
        }
        this.message$.next(msg);
        const inThread =
          this.activeConversationId != null &&
          Number(msg.conversationId) === Number(this.activeConversationId);
        if (!inThread) {
          this.bumpUnread();
        }
        const shouldAlert = !inThread || document.hidden;
        if (shouldAlert) {
          this.chatNotification.notifyIncomingMessage(msg, {
            inAppToast: !document.hidden,
          });
        }
      });
    } catch {
      // ignore malformed frames
    }
  }

  private isCurrentUser(senderUserId: number): boolean {
    try {
      const raw = localStorage.getItem('user');
      if (!raw) {
        return false;
      }
      const user = JSON.parse(raw) as { serUserId?: number | string; userId?: number | string; id?: number | string };
      const currentId = user?.serUserId ?? user?.userId ?? user?.id;
      return Number(currentId) === Number(senderUserId);
    } catch {
      return false;
    }
  }

  private buildWebSocketUrl(token: string): string {
    const wsBase = urls.WS_BASE_URL.replace(/\/$/, '');
    return `${wsBase}/ws/chat?token=${encodeURIComponent(token)}`;
  }

  private normalizePayload(payload: ChatMessage): ChatMessage {
    const raw = payload as ChatMessage & { deliveryStatus?: string };
    return {
      ...payload,
      messageId: Number(payload.messageId),
      conversationId: Number(payload.conversationId),
      senderUserId: Number(payload.senderUserId),
      sentAt: payload.sentAt != null ? Number(payload.sentAt) : undefined,
      deliveryStatus: raw.deliveryStatus as ChatMessage['deliveryStatus'],
    };
  }

  playMessageTone(): void {
    this.chatNotification.playRingtone();
  }
}
