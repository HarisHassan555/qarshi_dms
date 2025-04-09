// dashboard.service.ts
import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
// @ts-ignore
import { DashboardResponse } from './models/dashboard-response.model';
import {urls} from "../../utils/urls";

@Injectable({
    providedIn: 'root',
})
export class DashboardService {

    constructor(private http: HttpClient) {}

    getDashboardData(startDateFrom: string, endDateTo: string) {
        const startDate = new Date(startDateFrom);
        const endDate = new Date(endDateTo);

        const params = new HttpParams()
            .set('startDate', startDate.toISOString())
            .set('endDate', endDate.toISOString());
        return this.http.get(urls.API_URL + 'admin-dashboard',{ params });
    }

}
