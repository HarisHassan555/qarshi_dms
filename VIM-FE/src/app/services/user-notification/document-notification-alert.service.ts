import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from 'src/app/NotificationService';
import { UserNotificationDto } from './user-notification.service';

const SOUND_ENABLED_KEY = 'document_notification_sound_enabled';
const RINGTONE_PATH = 'assets/sounds/document-notification-ringtone.mp3';

@Injectable({ providedIn: 'root' })
export class DocumentNotificationAlertService {
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

  /**
   * Document / workflow in-app notification (bell icon) — separate sound from chat messages.
   */
  notifyIncoming(notification: UserNotificationDto, options?: { inAppToast?: boolean }): void {
    const away = this.isUserAwayFromApp();
    if (away) {
      this.showBrowserNotification(notification);
    }
    this.playAlertTone();
    if (!away && options?.inAppToast !== false) {
      this.showInAppToast(notification);
    }
  }

  isUserAwayFromApp(): boolean {
    if (typeof document === 'undefined') {
      return true;
    }
    return document.hidden || !document.hasFocus();
  }

  playAlertTone(): void {
    if (!this.isSoundEnabled()) {
      return;
    }
    if (this.playCustomRingtone()) {
      return;
    }
    this.playBuiltInChime();
  }

  private showInAppToast(notification: UserNotificationDto): void {
    const title = (notification.txtTitle || 'Notification').trim();
    const body = (notification.txtMessage || '').trim();
    const text = body.length > 120 ? `${body.slice(0, 117)}…` : body;
    this.notificationService.showMessage(text ? `${title}: ${text}` : title, 'success');
  }

  private showBrowserNotification(notification: UserNotificationDto): void {
    if (!('Notification' in window) || Notification.permission !== 'granted') {
      return;
    }
    const title = (notification.txtTitle || 'Document notification').trim();
    const body = (notification.txtMessage || '').trim();
    const preview = body.length > 240 ? `${body.slice(0, 237)}…` : body;
    try {
      const n = new Notification(title, {
        body: preview || 'You have a new document notification',
        icon: 'assets/images/qarshi-logo.png',
        tag: `doc-notification-${notification.serUserNotificationId ?? 'new'}`,
        requireInteraction: false,
        silent: true,
      });
      n.onclick = () => {
        window.focus();
        const link = (notification.txtDeepLink || '').trim();
        if (link) {
          const path = link.replace(/^\/+/, '').replace(/^velocity\/?/i, '');
          void this.router.navigateByUrl('/' + path);
        } else {
          void this.router.navigateByUrl('/notification-management');
        }
        n.close();
      };
    } catch {
      // optional
    }
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

  /** Soft three-note alert (distinct from chat’s two-tone chime). */
  private playBuiltInChime(): void {
    try {
      const Ctx = window.AudioContext || (window as any).webkitAudioContext;
      if (!Ctx) {
        return;
      }
      const ctx = new Ctx();
      const now = ctx.currentTime;
      const tones = [
        { freq: 523.25, start: 0, duration: 0.1 },
        { freq: 659.25, start: 0.11, duration: 0.1 },
        { freq: 783.99, start: 0.22, duration: 0.14 },
      ];
      tones.forEach(({ freq, start, duration }) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.0001, now + start);
        gain.gain.exponentialRampToValueAtTime(0.1, now + start + 0.015);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + start + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now + start);
        osc.stop(now + start + duration + 0.02);
      });
      window.setTimeout(() => ctx.close(), 450);
    } catch {
      // audio optional
    }
  }
}
