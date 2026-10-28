import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { urls } from 'src/app/utils/urls';

@Injectable({
    providedIn: 'root',
})
export class DocumentShareService {
    constructor(private http: HttpClient) {}

    createShareLink(applicationId: number, expiresInDays = 30): Observable<any> {
        return this.http.post<any>(urls.API_URL + 'createDocumentShareLink', {
            applicationId,
            expiresInDays,
        });
    }

    getShareLinks(applicationId: number): Observable<any> {
        return this.http.get<any>(
            urls.API_URL + 'getDocumentShareLinks?applicationId=' + encodeURIComponent(String(applicationId))
        );
    }

    revokeShareLink(shareId: number): Observable<any> {
        return this.http.post<any>(urls.API_URL + 'revokeDocumentShareLink', { shareId });
    }

    getPublicShare(token: string): Observable<any> {
        return this.http.get<any>(
            urls.API_URL + 'public/documentShare?token=' + encodeURIComponent(token)
        );
    }

    getPublicSharePdf(token: string): Observable<any> {
        return this.http.get<any>(
            urls.API_URL + 'public/documentSharePdf?token=' + encodeURIComponent(token)
        );
    }

    buildShareUrl(token: string): string {
        const baseHref = document.querySelector('base')?.getAttribute('href') || '/';
        const normalizedBase = baseHref.endsWith('/') ? baseHref.slice(0, -1) : baseHref;
        const origin = window.location.origin;
        if (!normalizedBase || normalizedBase === '/') {
            return `${origin}/share/${token}`;
        }
        return `${origin}${normalizedBase}/share/${token}`;
    }
}
