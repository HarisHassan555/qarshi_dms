import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
import {Observable, of} from "rxjs";
import { filter, map } from 'rxjs/operators';

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

  getCurrentUser() {
    return this.http.get(urls.API_URL + 'getCurrentUser');
  }

  profilePictureUrl(userId: number): string {
    return `${urls.API_URL}getUserProfilePicture?userId=${encodeURIComponent(String(userId))}`;
  }

  loadProfilePictureObjectUrl(userId: number): Observable<string> {
    return this.http.get(this.profilePictureUrl(userId), { responseType: 'blob' }).pipe(
      filter((blob) => blob != null && blob.size > 64),
      map((blob) => URL.createObjectURL(blob))
    );
  }

  getRoles() {
    return this.http.get(urls.API_URL + 'getActiveRole');
  }

  addRole(payload: any) {
    return this.http.post(urls.API_URL + 'addNewRole', payload, { responseType: 'text' });
  }

  updateRole(payload: any) {
    return this.http.post(urls.API_URL + 'updateRole', payload, { responseType: 'text' });
  }

  deleteRole(ids: (number | string)[]) {
    const csv = (ids || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0).join(',');
    return this.http.post(urls.API_URL + 'deleteRole', JSON.stringify(csv), { responseType: 'text' });
  }

  getPasswordPolicy() {
    return this.http.get(urls.API_URL + 'getActivePasswordPolicy');
  }

  suggestLoginName(employeeId: string, fullName: string) {
    const params = new URLSearchParams();
    if (employeeId) {
      params.set('employeeId', employeeId);
    }
    if (fullName) {
      params.set('fullName', fullName);
    }
    const q = params.toString();
    return this.http.get<{ loginName: string }>(
      urls.API_URL + 'suggestUserLoginName' + (q ? '?' + q : '')
    );
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

  changePasswordAdmin(payload: any) {
      return this.http.post(urls.API_URL + 'UpdatePasswordAdmin', payload,{ responseType: 'text' });
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
