import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface OfficialDocumentArchiveItem {
    serOfficialDocumentId: number;
    txtTitle: string;
    txtSummary: string;
    txtLabel: string;
    txtOriginalFileName: string;
    txtMimeType?: string;
    intPageCount?: number;
    blAiAnalyzed?: boolean;
    dteCreatedDate?: string | number;
}

export interface OfficialDocumentListResponse {
    items: OfficialDocumentArchiveItem[];
    total: number;
    page: number;
    pageSize: number;
    labels: string[];
    geminiConfigured: boolean;
}

@Injectable({
    providedIn: 'root',
})
export class OfficialDocumentArchiveService {
    constructor(private http: HttpClient) {}

    list(q: string, label: string, page: number, pageSize: number): Observable<OfficialDocumentListResponse> {
        let params = new HttpParams()
            .set('page', String(page))
            .set('pageSize', String(pageSize));
        if (q?.trim()) {
            params = params.set('q', q.trim());
        }
        if (label?.trim()) {
            params = params.set('label', label.trim());
        }
        return this.http.get<OfficialDocumentListResponse>(urls.API_URL + 'listOfficialDocuments', { params });
    }

    upload(file: File, runAi = true): Observable<any> {
        const form = new FormData();
        form.append('file', file);
        form.append('runAi', runAi ? 'true' : 'false');
        return this.http.post(urls.API_URL + 'uploadOfficialDocument', form);
    }

    delete(id: number): Observable<any> {
        const params = new HttpParams().set('id', String(id));
        return this.http.post(urls.API_URL + 'deleteOfficialDocument', null, { params });
    }

    reanalyze(id: number): Observable<any> {
        const params = new HttpParams().set('id', String(id));
        return this.http.post(urls.API_URL + 'reanalyzeOfficialDocument', null, { params });
    }

    downloadUrl(id: number): string {
        return urls.API_URL + 'downloadOfficialDocument?id=' + encodeURIComponent(String(id));
    }
}
