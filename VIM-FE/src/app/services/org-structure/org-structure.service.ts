import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
    providedIn: 'root',
})
export class OrgStructureService {
    constructor(private http: HttpClient) {}

    getOrganizations() {
        return this.http.get<any[]>(urls.API_URL + 'getAllOrganizations');
    }

    getSubareas() {
        return this.http.get<any[]>(urls.API_URL + 'getAllSubareas');
    }

    getDivisions() {
        return this.http.get<any[]>(urls.API_URL + 'getAllDivisions');
    }

    updateOrganizationBranding(payload: {
        serOrganizationId: number;
        txtOrganizationName?: string | null;
        txtAddress?: string | null;
        logo?: string | null;
        logoBase64?: string | null;
    }) {
        return this.http.post<any>(urls.API_URL + 'updateOrganizationBranding', payload);
    }
}
