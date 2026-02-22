import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpResponse
} from '@angular/common/http';
import { finalize, Observable, tap } from 'rxjs';
import { Store } from '@ngrx/store';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    public storeData: Store<any>
  ) {}


    intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        const isLoginUrl = request.url.includes("login");
        const isUserDetails = request.url.includes("getloginCustomer");
        const isSaleInvoiceEditUrl = request.url.includes("addNewSaleOrderNew");
        const isUploadDocument = request.url.includes("uploadCandidateDocument");
        const authToken = localStorage.getItem('token');
        const isuploadDocument = request.url.includes("uploadDocument");
        const isUpdateApplicationPdf = request.url.includes("updateApplicationPdf");
        const isEmailApproval = request.url.includes("approveApplicationFromEmail") || request.url.includes("rejectApplicationFromEmail");
        const isFormDataRequest = request.body instanceof FormData;
        let ok: string;


        if (isLoginUrl || isEmailApproval) {
            return next.handle(request);
        }

        this.storeData.dispatch({ type: 'toggleMainLoader', payload: true });

        if (!isUserDetails) {
            if (isSaleInvoiceEditUrl || isUploadDocument || isuploadDocument || isUpdateApplicationPdf || isFormDataRequest) {
                const authReq = request.clone({
                    setHeaders: {
                        Authorization: `Bearer ${authToken}`
                    }
                });
                return next.handle(authReq).pipe(
                    tap({
                        next: (event) => (ok = event instanceof HttpResponse ? 'succeeded' : ''),
                        error: (_error) => (ok = 'failed')
                      }),
                      finalize(() => {
                        this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
                      })
                );
            }

            const authReq = request.clone({
                setHeaders: {
                    Authorization: `Bearer ${authToken}`,
                    'Content-Type': 'application/json'
                }
            });

            return next.handle(authReq).pipe(
                tap({
                    next: (event) => (ok = event instanceof HttpResponse ? 'succeeded' : ''),
                    error: (_error) => (ok = 'failed')
                  }),
                  finalize(() => {
                    this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
                  })
            );
        }

        return next.handle(request).pipe(
            tap({
                next: (event) => (ok = event instanceof HttpResponse ? 'succeeded' : ''),
                error: (_error) => (ok = 'failed')
              }),
              finalize(() => {
                this.storeData.dispatch({ type: 'toggleMainLoader', payload: false });
              })
        );
    }

}
