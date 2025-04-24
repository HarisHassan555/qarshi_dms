import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SharedDataService {
  private user = new BehaviorSubject(null);

  constructor() { }

  getUser(): Observable<any> {
    return this.user.asObservable();
  }

  saveUser(user: any) {
    this.user.next(user);
  }

}
