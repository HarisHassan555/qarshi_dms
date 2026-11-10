import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, catchError, finalize, map, shareReplay, switchMap, throwError } from 'rxjs';
import { Store } from '@ngrx/store';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private refreshInFlight$: Observable<string> | null = null;

  constructor(
    public storeData: Store<any>,
    private authService: AuthService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const isLoginUrl = request.url.includes('api/auth/login') || request.url.includes('/login');
    const isRefreshUrl = request.url.includes('api/auth/refresh');
    const isUserDetails = request.url.includes('getloginCustomer');
    const isSaleInvoiceEditUrl = request.url.includes('addNewSaleOrderNew');
    const isUploadDocument = request.url.includes('uploadCandidateDocument');
    const authToken = localStorage.getItem('token');
    const isuploadDocument = request.url.includes('uploadDocument');
    const isUpdateApplicationPdf = request.url.includes('updateApplicationPdf');
    const isEmailApproval =
      request.url.includes('approveApplicationFromEmail') ||
      request.url.includes('rejectApplicationFromEmail');
    const isPublicDocumentShare = request.url.includes('public/documentShare');
    const isOpenMeteo = request.url.includes('api.open-meteo.com');
    const isFormDataRequest = request.body instanceof FormData;

    if (isLoginUrl || isRefreshUrl || isEmailApproval || isOpenMeteo || isPublicDocumentShare) {
      return next.handle(request);
    }

    this.storeData.dispatch({ type: 'toggleMainLoader', payload: true });

    if (!isUserDetails) {
      if (
        isSaleInvoiceEditUrl ||
        isUploadDocument ||
        isuploadDocument ||
        isUpdateApplicationPdf ||
        isFormDataRequest
      ) {
        const authReq = request.clone({
          setHeaders: {
            Authorization: `Bearer ${authToken}`
          }
        });
        return this.handleAuthorizedRequest(authReq, next, () => {
          this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
        });
      }

      const headers: Record<string, string> = {
        Authorization: `Bearer ${authToken}`
      };
      if (request.method !== 'GET' && request.method !== 'HEAD') {
        headers['Content-Type'] = 'application/json';
      }

      const authReq = request.clone({ setHeaders: headers });
      return this.handleAuthorizedRequest(authReq, next, () => {
        this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
      });
    }

    return next.handle(request).pipe(
      finalize(() => {
        this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
      })
    );
  }

  private handleAuthorizedRequest(
    request: HttpRequest<unknown>,
    next: HttpHandler,
    onFinalize: () => void
  ): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401 || request.url.includes('api/auth/refresh')) {
          return throwError(() => error);
        }
        return this.refreshAccessToken().pipe(
          switchMap((nextToken) => {
            const retryRequest = request.clone({
              setHeaders: {
                Authorization: `Bearer ${nextToken}`
              }
            });
            return next.handle(retryRequest);
          }),
          catchError((refreshError) => {
            this.authService.clearSession();
            void this.router.navigate(['/auth/signin']);
            return throwError(() => refreshError);
          })
        );
      }),
      finalize(onFinalize)
    );
  }

  private refreshAccessToken(): Observable<string> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const refreshToken = this.authService.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    this.refreshInFlight$ = this.authService.refreshAccessToken(refreshToken).pipe(
      map((response) => {
        const nextAccess = this.extractBearerToken(response?.token) || localStorage.getItem('token') || '';
        if (!nextAccess) {
          throw new Error(response?.message || 'Token refresh failed');
        }
        return nextAccess;
      }),
      finalize(() => {
        this.refreshInFlight$ = null;
      }),
      shareReplay(1)
    );

    return this.refreshInFlight$;
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
