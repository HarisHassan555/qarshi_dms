import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface TokenRefreshResponse {
  token?: string;
  refreshToken?: string;
  user?: any;
  status?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  /**
   * Uses HttpBackend so auth calls do not go through AuthInterceptor
   * (avoids circular DI and refresh recursion).
   */
  private readonly http: HttpClient;

  constructor(httpBackend: HttpBackend) {
    this.http = new HttpClient(httpBackend);
  }

  signin(payload: any) {
    return this.http.post(urls.API_URL + urls.SIGNIN_URL, payload);
  }

  requestForgotPasswordOtp(email: string) {
    return this.http.post(urls.API_URL + urls.FORGOT_PASSWORD_REQUEST_OTP, { email });
  }

  verifyForgotPasswordOtp(email: string, otp: string) {
    return this.http.post(urls.API_URL + urls.FORGOT_PASSWORD_VERIFY_OTP, { email, otp });
  }

  resetForgotPassword(email: string, otp: string, newPassword: string) {
    return this.http.post(urls.API_URL + urls.FORGOT_PASSWORD_RESET, { email, otp, newPassword });
  }

  refreshAccessToken(refreshToken: string): Observable<TokenRefreshResponse> {
    return this.http
      .post<TokenRefreshResponse>(urls.API_URL + urls.REFRESH_URL, { refreshToken })
      .pipe(
        tap((response) => {
          const nextAccess = this.extractBearerToken(response?.token);
          if (nextAccess) {
            localStorage.setItem('token', nextAccess);
          }
          if (response?.refreshToken) {
            localStorage.setItem('refreshToken', String(response.refreshToken).trim());
          }
          if (response?.user) {
            localStorage.setItem('user', JSON.stringify(response.user));
          }
        })
      );
  }

  storeSessionTokens(accessToken: string, refreshToken?: string | null): void {
    if (accessToken) {
      localStorage.setItem('token', accessToken);
    }
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }
  }

  clearSession(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  }

  getRefreshToken(): string {
    return String(localStorage.getItem('refreshToken') || '').trim();
  }

  private extractBearerToken(value: string | undefined | null): string {
    const raw = String(value || '').trim();
    if (!raw) {
      return '';
    }
    if (raw.toLowerCase().startsWith('bearer ')) {
      return raw.substring(7).trim();
    }
    return raw;
  }
}
