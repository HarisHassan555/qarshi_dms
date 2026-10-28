import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProfileDrawerService {
  private readonly openSubject = new Subject<void>();
  readonly open$ = this.openSubject.asObservable();

  private readonly profilePhotoUpdatedSubject = new Subject<void>();
  /** Fires when the logged-in user changes their profile photo (header can refresh). */
  readonly profilePhotoUpdated$ = this.profilePhotoUpdatedSubject.asObservable();

  open(): void {
    this.openSubject.next();
  }

  notifyProfilePhotoUpdated(): void {
    this.profilePhotoUpdatedSubject.next();
  }
}
