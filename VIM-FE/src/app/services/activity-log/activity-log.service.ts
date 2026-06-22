import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

export interface ActivityLogFilters {
  actionType?: string;
  status?: string;
  userId?: number;
  entityId?: number;
  startDate?: string;
  endDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ActivityLogService {

  constructor(private http: HttpClient) { }

  getAll(filters?: ActivityLogFilters) {
    let params = new HttpParams();
    if (filters?.actionType) {
      params = params.set('actionType', filters.actionType);
    }
    if (filters?.status) {
      params = params.set('status', filters.status);
    }
    if (filters?.userId) {
      params = params.set('userId', filters.userId.toString());
    }
    if (filters?.entityId) {
      params = params.set('entityId', filters.entityId.toString());
    }
    if (filters?.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    if (filters?.endDate) {
      params = params.set('endDate', filters.endDate);
    }
    return this.http.get<any[]>(urls.API_URL + 'getAllActivityLogs', { params });
  }

  getById(id: number) {
    return this.http.get<any>(urls.API_URL + 'getActivityLogById', {
      params: { id: id.toString() }
    });
  }

  update(payload: any) {
    return this.http.post<any>(urls.API_URL + 'updateActivityLog', payload);
  }

  getTransaction(logId: number) {
    return this.http.get<any>(urls.API_URL + 'getActivityLogTransaction', {
      params: { id: logId.toString() }
    });
  }

  updateTransaction(payload: any) {
    return this.http.post<any>(urls.API_URL + 'updateActivityLogTransaction', payload);
  }
}
