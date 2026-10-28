import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { ChatMessage } from './chat.service';
import { NotificationService } from 'src/app/NotificationService';

const SOUND_ENABLED_KEY = 'chat_sound_enabled';
const RINGTONE_PATH = 'assets/sounds/chat-ringtone.mp3';

@Injectable({ providedIn: 'root' })
export class ChatNotificationService {
  private permissionRequested = false;
  private ringtoneAudio: HTMLAudioElement | null = null;

  constructor(
    private router: Router,
    private notificationService: NotificationService
  ) {}

  isSoundEnabled(): boolean {
    const raw = localStorage.getItem(SOUND_ENABLED_KEY);
    return raw !== 'false';
  }

  setSoundEnabled(enabled: boolean): void {
    localStorage.setItem(SOUND_ENABLED_KEY, enabled ? 'true' : 'false');
  }

  getBrowserPermission(): NotificationPermission | 'unsupported' {
    if (!('Notification' in window)) {
      return 'unsupported';
    }
    return Notification.permission;
  }

  async ensureBrowserPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      return false;
    }
    if (Notification.permission === 'granted') {
      return true;
    }
    if (Notification.permission === 'denied') {
      return false;
    }
    if (this.permissionRequested) {
      return false;
    }
    this.permissionRequested = true;
    const result = await Notification.requestPermission();
    return result === 'granted';
  }

  /**
   * WhatsApp Web–style alerts: OS notification when the tab is hidden or the window
   * is not focused; sound + in-app toast when you are actively using Velocity.
   * (Requires the app to stay open in a browser tab — same as WhatsApp Web.)
   */
  notifyIncomingMessage(msg: ChatMessage, options?: { inAppToast?: boolean }): void {
    const away = this.isUserAwayFromApp();
    if (away) {
      this.showBrowserNotification(msg);
      this.playRingtone();
      return;
    }
    this.playRingtone();
    if (options?.inAppToast !== false) {
      this.showInAppToast(msg);
    }
  }

  /** Tab in background, minimized browser, or another app/window focused. */
  isUserAwayFromApp(): boolean {
    if (typeof document === 'undefined') {
      return true;
    }
    return document.hidden || !document.hasFocus();
  }

  private showInAppToast(msg: ChatMessage): void {
    const sender = (msg.senderName || 'Someone').trim();
    const preview = (msg.body || '').trim();
    const text = preview.length > 120 ? `${preview.slice(0, 117)}…` : preview;
    this.notificationService.showMessage(`${sender}: ${text}`, 'success');
  }

  private showBrowserNotification(msg: ChatMessage): void {
    if (!('Notification' in window) || Notification.permission !== 'granted') {
      return;
    }
    const sender = (msg.senderName || 'New message').trim();
    const body = (msg.body || '').trim();
    const preview = body.length > 240 ? `${body.slice(0, 237)}…` : body;
    try {
      const notification = new Notification(sender, {
        body: preview || 'You have a new chat message',
        icon: 'assets/images/qarshi-logo.png',
        tag: `chat-${msg.conversationId}`,
        requireInteraction: false,
        silent: false,
      });
      notification.onclick = () => {
        window.focus();
        const convId = msg.conversationId;
        if (convId != null) {
          void this.router.navigate(['/chat'], { queryParams: { conversationId: convId } });
        } else {
          void this.router.navigateByUrl('/chat');
        }
        notification.close();
      };
    } catch {
      // notifications optional
    }
  }

  playRingtone(): void {
    if (!this.isSoundEnabled()) {
      return;
    }
    if (this.playCustomRingtone()) {
      return;
    }
    this.playBuiltInChime();
  }

  private playCustomRingtone(): boolean {
    try {
      if (!this.ringtoneAudio) {
        this.ringtoneAudio = new Audio(RINGTONE_PATH);
        this.ringtoneAudio.preload = 'auto';
      }
      const audio = this.ringtoneAudio;
      audio.currentTime = 0;
      const playPromise = audio.play();
      if (playPromise) {
        playPromise.catch(() => this.playBuiltInChime());
      }
      return true;
    } catch {
      return false;
    }
  }

  /** Short two-tone chime when no custom MP3 is present or it fails to load. */
  private playBuiltInChime(): void {
    try {
      const Ctx = window.AudioContext || (window as any).webkitAudioContext;
      if (!Ctx) {
        return;
      }
      const ctx = new Ctx();
      const now = ctx.currentTime;
      const tones = [
        { freq: 880, start: 0, duration: 0.12 },
        { freq: 1174, start: 0.14, duration: 0.16 },
      ];
      tones.forEach(({ freq, start, duration }) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.0001, now + start);
        gain.gain.exponentialRampToValueAtTime(0.12, now + start + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + start + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + start);
        osc.stop(now + start + duration + 0.02);
      });
      window.setTimeout(() => ctx.close(), 400);
    } catch {
      // audio optional
    }
  }
}
