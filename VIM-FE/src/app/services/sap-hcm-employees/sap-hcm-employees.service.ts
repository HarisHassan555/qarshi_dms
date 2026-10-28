import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, filter } from 'rxjs/operators';
import { SapHcmEmployeeRecord } from 'src/app/components/sap-hcm-employees/sap-hcm-employees.demo-data';
import { urls } from 'src/app/utils/urls';

export interface SapHcmXmlSyncResult {
    status: string;
    message: string;
    totalItems: number;
    inserted: number;
    updated: number;
    picturesSaved: number;
    skipped: number;
}

@Injectable({
    providedIn: 'root',
})
export class SapHcmEmployeesService {
    constructor(private http: HttpClient) {}

    getAll(): Observable<SapHcmEmployeeRecord[]> {
        return this.http
            .get<SapHcmEmployeeRecord[]>(urls.API_URL + 'getAllSapHcmEmployees')
            .pipe(map((rows) => (rows || []).map((row) => this.normalizeEmployee(row))));
    }

    /** Ensures API rows match {@link SapHcmEmployeeRecord} (camelCase from {@code SapHcmEmployeeDto}). */
    private normalizeEmployee(raw: SapHcmEmployeeRecord | Record<string, unknown>): SapHcmEmployeeRecord {
        const r = (raw || {}) as Record<string, unknown>;
        return {
            employee: String(r['employee'] ?? r['EMPLOYEE'] ?? '').trim(),
            empName: String(r['empName'] ?? r['EMP_NAME'] ?? '').trim(),
            departmentName: String(r['departmentName'] ?? r['DEPARTMENT_NAME'] ?? '').trim(),
            division: String(r['division'] ?? r['DIVISION'] ?? '').trim(),
            designation: String(r['designation'] ?? r['DESIGNATION'] ?? '').trim(),
            email: String(r['email'] ?? r['EMAIL'] ?? '').trim(),
            payScale: String(r['payScale'] ?? r['PAY_SCALE'] ?? '').trim(),
            subarea: String(r['subarea'] ?? r['SUBAREA'] ?? '').trim(),
            companyName: String(r['companyName'] ?? r['COMPANY_NAME'] ?? '').trim(),
            hiringDate: String(r['hiringDate'] ?? r['HIRING_DATE'] ?? '').trim(),
            confirmationDate: String(r['confirmationDate'] ?? r['CONFIRMATION_DATE'] ?? '').trim(),
            retirementDate: String(r['retirementDate'] ?? r['RETIREMENT_DATE'] ?? '').trim(),
            separationDate: String(r['separationDate'] ?? r['SEPARATION_DATE'] ?? '').trim(),
            rehiringDate: String(r['rehiringDate'] ?? r['REHIRING_DATE'] ?? '').trim(),
            annualIncType: String(r['annualIncType'] ?? r['ANNUAL_INC_TYPE'] ?? '').trim(),
            active: String(r['active'] ?? r['ACTIVE'] ?? '').trim(),
            reportTo: String(r['reportTo'] ?? r['REPORT_TO'] ?? '').trim(),
            reportToName: String(r['reportToName'] ?? r['REPORT_TO_NAME'] ?? '').trim(),
            hasPicture: this.coerceHasPicture(r),
            pictureUrl: r['pictureUrl'] as string | undefined,
        };
    }

    private coerceHasPicture(r: Record<string, unknown>): boolean {
        const flag = r['hasPicture'];
        if (flag === true || flag === 1 || flag === '1' || flag === 'true') {
            return true;
        }
        const path = r['pictureUrl'];
        return typeof path === 'string' && path.trim().length > 0;
    }

    syncFromXml(file: File): Observable<SapHcmXmlSyncResult> {
        const formData = new FormData();
        formData.append('file', file, file.name);
        return this.http.post<SapHcmXmlSyncResult>(urls.API_URL + 'syncSapHcmEmployeesFromXml', formData);
    }

    syncFromSap(): Observable<SapHcmXmlSyncResult> {
        return this.http.post<SapHcmXmlSyncResult>(urls.API_URL + 'syncSapHcmEmployeesFromSap', {});
    }

    photoUrl(employeeId: string): string {
        const id = encodeURIComponent(employeeId || '');
        return `${urls.API_URL}getSapHcmEmployeePhoto?employeeId=${id}`;
    }

    loadPhotoObjectUrl(employeeId: string): Observable<string> {
        return this.http.get(this.photoUrl(employeeId), { responseType: 'blob' }).pipe(
            filter((blob) => blob != null && blob.size > 64),
            map((blob) => URL.createObjectURL(blob))
        );
    }
}
