import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { urls } from 'src/app/utils/urls';

export interface UserNotificationDto {
    serUserNotificationId: number;
    serRecipientUserId?: number;
    serApplicationId?: number;
    serFormId?: number;
    txtEventType: string;
    txtTitle: string;
    txtMessage?: string;
    txtDeepLink?: string;
    txtPayload?: string;
    blnRead?: boolean;
    dteReadDate?: string;
    serActorUserId?: number;
    dteCreatedDate?: string;
}

export interface UserNotificationSaveRequest {
    serUserNotificationId?: number;
    serRecipientUserId?: number;
    serApplicationId?: number;
    serFormId?: number;
    txtEventType?: string;
    txtTitle?: string;
    txtMessage?: string;
    txtDeepLink?: string;
    txtPayload?: string;
    blnRead?: boolean;
    serActorUserId?: number;
}

@Injectable({ providedIn: 'root' })
export class UserNotificationService {
    constructor(private http: HttpClient) {}

    getAll(options?: { unreadOnly?: boolean; limit?: number }): Observable<UserNotificationDto[]> {
        let params = new HttpParams();
        if (options?.unreadOnly) {
            params = params.set('unreadOnly', 'true');
        }
        if (options?.limit) {
            params = params.set('limit', String(options.limit));
        }
        return this.http.get<UserNotificationDto[]>(urls.API_URL + 'getUserNotifications', { params });
    }

    getById(id: number): Observable<UserNotificationDto> {
        const params = new HttpParams().set('id', String(id));
        return this.http.get<UserNotificationDto>(urls.API_URL + 'getUserNotificationById', { params });
    }

    getUnreadCount(): Observable<number> {
        return this.http
            .get<{ unreadCount: number }>(urls.API_URL + 'getUnreadNotificationCount')
            .pipe(map((r) => Number(r?.unreadCount) || 0));
    }

    create(body: UserNotificationSaveRequest): Observable<UserNotificationDto> {
        return this.http
            .post<{ status: string; data: UserNotificationDto }>(urls.API_URL + 'createUserNotification', body)
            .pipe(map((r) => r.data));
    }

    update(body: UserNotificationSaveRequest): Observable<UserNotificationDto> {
        return this.http
            .post<{ status: string; data: UserNotificationDto }>(urls.API_URL + 'updateUserNotification', body)
            .pipe(map((r) => r.data));
    }

    delete(id: number): Observable<void> {
        const params = new HttpParams().set('id', String(id));
        return this.http.post<void>(urls.API_URL + 'deleteUserNotification', null, { params });
    }

    markRead(id: number): Observable<UserNotificationDto> {
        const params = new HttpParams().set('id', String(id));
        return this.http
            .post<{ status: string; data: UserNotificationDto }>(urls.API_URL + 'markNotificationRead', null, { params })
            .pipe(map((r) => r.data));
    }

    markAllRead(): Observable<number> {
        return this.http
            .post<{ status: string; unreadCount: number }>(urls.API_URL + 'markAllNotificationsRead', {})
            .pipe(map((r) => Number(r?.unreadCount) || 0));
    }
}
