import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
import {Observable, of} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private http: HttpClient
  ) { }

  getUsers() {
    return this.http.get(urls.API_URL + 'getAllUser');
  }

  getRoles() {
    return this.http.get(urls.API_URL + 'getActiveRole');
  }

  getPasswordPolicy() {
    return this.http.get(urls.API_URL + 'getActivePasswordPolicy');
  }

  save(payload: any) {
    if (payload.serUserId) {
      return this.http.post(urls.API_URL + 'updateUser', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewUser', payload,{ responseType: 'text' });
    }
  }

  getAllPasswordPolicy() {
    return this.http.get(urls.API_URL + 'getAllPasswordPolicy');
  }

  savePasswordPolicy(payload: any) {
    if (payload.serPasswordPolicyId) {
      return this.http.post(urls.API_URL + 'updatePasswordPolicy', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewPasswordPolicy', payload,{ responseType: 'text' });
    }
  }

  changePassword(payload: any) {
      return this.http.post(urls.API_URL + 'UpdatePasswordReconfirm', payload,{ responseType: 'text' });
  }


    // @ts-ignore
    me(): Observable<CfgTblUser | null> {
        const userJson = localStorage.getItem('user');
        if (userJson) {
            // @ts-ignore
            const user = JSON.parse(userJson) as CfgTblUser;
            return of(user);

        } else {
            console.error('No user found in localStorage');
            return of(null);
        }
    }

}
