import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface EmailDeliveryLogRow {
    id: number;
    triggerType: string;
    templateCode?: string;
    applicationId?: number;
    recipientEmail: string;
    subject?: string;
    bodyHtml?: string;
    bodyText?: string;
    status: string;
    errorMessage?: string;
    hasAttachments?: boolean;
    contextLabel?: string;
    sentAt?: string;
}

export interface EmailDeliveryLogSearchResponse {
    items: EmailDeliveryLogRow[];
    total: number;
    page: number;
    size: number;
}

@Injectable({
    providedIn: 'root',
})
export class EmailDeliveryLogService {
    constructor(private http: HttpClient) {}

    search(filters: {
        status?: string;
        triggerType?: string;
        recipient?: string;
        applicationId?: number | null;
        page?: number;
        size?: number;
    }): Observable<EmailDeliveryLogSearchResponse> {
        let params = new HttpParams();
        if (filters.status) {
            params = params.set('status', filters.status);
        }
        if (filters.triggerType) {
            params = params.set('triggerType', filters.triggerType);
        }
        if (filters.recipient) {
            params = params.set('recipient', filters.recipient);
        }
        if (filters.applicationId != null && filters.applicationId > 0) {
            params = params.set('applicationId', String(filters.applicationId));
        }
        params = params.set('page', String(filters.page ?? 0));
        params = params.set('size', String(filters.size ?? 50));
        return this.http.get<EmailDeliveryLogSearchResponse>(urls.API_URL + 'searchEmailDeliveryLogs', { params });
    }

    getById(id: number): Observable<EmailDeliveryLogRow> {
        const params = new HttpParams().set('id', String(id));
        return this.http.get<EmailDeliveryLogRow>(urls.API_URL + 'getEmailDeliveryLogById', { params });
    }
}