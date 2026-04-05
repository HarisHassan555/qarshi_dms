import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SharedDataService {
  private user = new BehaviorSubject(null);

  /** Emits a void signal whenever role/permission data changes so subscribers can refresh. */
  private refreshMenu = new Subject<void>();

  constructor() { }

  getUser(): Observable<any> {
    return this.user.asObservable();
  }

  saveUser(user: any) {
    this.user.next(user);
  }

  /** Trigger a sidebar menu refresh (call after save / update / delete role). */
  triggerMenuRefresh(): void {
    this.refreshMenu.next();
  }

  /** Sidebar subscribes to this to know when to re-fetch menus. */
  getRefreshMenu(): Observable<void> {
    return this.refreshMenu.asObservable();
  }

}
