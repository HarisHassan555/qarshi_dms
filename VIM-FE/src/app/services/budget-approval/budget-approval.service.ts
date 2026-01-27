import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
    providedIn: 'root'
})
export class BudgetApprovalService {

    constructor(private http: HttpClient) { }

    save(content: string) {
        return this.http.post(urls.API_URL + 'api/budget-approval/save', { content });
    }

    getLatest() {
        return this.http.get<any>(urls.API_URL + 'api/budget-approval/latest');
    }

    getAll() {
        return this.http.get<any[]>(urls.API_URL + 'api/budget-approval/all');
    }
}
