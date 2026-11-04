import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface ApplicationSetting {
    serApplicationSettingId?: number;
    txtApplicationName?: string;
    txtAddress?: string;
    txtEmail?: string;
    txtLogoPath?: string;
    txtDashboardHeroImagePath?: string;
    txtSmtpHost?: string;
    txtSmtpPort?: number;
    txtSmtpUsername?: string;
    txtSmtpPassword?: string;
    blSmtpAuth?: boolean;
    blSmtpSslEnable?: boolean;
    blSmtpStarttlsEnable?: boolean;
    blIsActive?: boolean;
    blIsDemo?: boolean;
}

export interface PublicApplicationBranding {
    applicationName?: string;
    hasLogo?: boolean;
}

@Injectable({
    providedIn: 'root',
})
export class ApplicationSettingService {
    private readonly brandingChanged = new Subject<void>();
    readonly brandingChanged$ = this.brandingChanged.asObservable();

    constructor(private http: HttpClient) {}

    notifyBrandingChanged(): void {
        this.brandingChanged.next();
    }

    getPublicBranding(): Observable<PublicApplicationBranding> {
        return this.http.get<PublicApplicationBranding>(urls.API_URL + 'getPublicApplicationBranding');
    }

    getPublicLogo(): Observable<Blob> {
        return this.http.get(urls.API_URL + 'getPublicApplicationLogo', { responseType: 'blob' });
    }

    getCurrent(): Observable<ApplicationSetting | null> {
        return this.http.get<ApplicationSetting | null>(urls.API_URL + 'getApplicationSetting');
    }

    getAll(): Observable<ApplicationSetting[]> {
        return this.http.get<ApplicationSetting[]>(urls.API_URL + 'getAllApplicationSettings');
    }

    save(payload: FormData, updating: boolean): Observable<ApplicationSetting> {
        const action = updating ? 'updateApplicationSetting' : 'addApplicationSetting';
        return this.http.post<ApplicationSetting>(urls.API_URL + action, payload);
    }

    delete(id: number): Observable<{ status: string }> {
        return this.http.post<{ status: string }>(
            urls.API_URL + 'deleteApplicationSetting?serApplicationSettingId=' + id,
            {}
        );
    }

    getImage(id: number, kind: 'logo' | 'hero'): Observable<Blob> {
        return this.http.get(urls.API_URL + 'getApplicationSettingImage', {
            params: { serApplicationSettingId: String(id), kind },
            responseType: 'blob',
        });
    }
}
