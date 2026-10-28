import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { urls } from 'src/app/utils/urls';
import { UserNotificationDto } from './user-notification.service';

export interface NotificationRealtimeFrame {
    type: string;
    action?: string;
    notification?: UserNotificationDto;
    unreadCount?: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationRealtimeService {
    private socket: WebSocket | null = null;
    private reconnectTimer: number | null = null;
    private unreadTotal = 0;

    readonly frame$ = new Subject<NotificationRealtimeFrame>();
    readonly unreadCount$ = new Subject<number>();

    constructor(private ngZone: NgZone) {}

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
        const url = `${urls.WS_BASE_URL.replace(/\/$/, '')}/ws/notifications?token=${encodeURIComponent(token)}`;
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
            this.unreadCount$.next(0);
        }
    }

    setUnreadTotal(count: number): void {
        this.unreadTotal = Math.max(0, Number(count) || 0);
        this.unreadCount$.next(this.unreadTotal);
    }

    private handleFrame(raw: string): void {
        try {
            const frame = JSON.parse(raw) as NotificationRealtimeFrame;
            this.ngZone.run(() => {
                if (frame.unreadCount != null) {
                    this.setUnreadTotal(frame.unreadCount);
                }
                this.frame$.next(frame);
            });
        } catch {
            // ignore malformed frames
        }
    }

    private scheduleReconnect(): void {
        if (this.reconnectTimer !== null || !localStorage.getItem('token')) {
            return;
        }
        this.reconnectTimer = window.setTimeout(() => {
            this.reconnectTimer = null;
            this.connect();
        }, 5000);
    }
}
