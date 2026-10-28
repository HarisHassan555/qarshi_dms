import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { urls } from 'src/app/utils/urls';

export interface UserDelegationDto {
    serDelegationId: number;
    serDelegateUserId: number;
    delegateUserName?: string;
    delegateEmployeeId?: string;
    serOnBehalfUserId: number;
    onBehalfUserName?: string;
    onBehalfRole?: string;
    dteValidFrom: string;
    dteValidTo: string;
    txtScope: string;
    documentIds?: string[];
    scopedDocuments?: UserDelegatableDocumentDto[];
    documentsLabel?: string;
    txtStatus: string;
    txtNotes?: string;
    hasAttachment?: boolean;
    attachmentFileName?: string;
    createdByName?: string;
}

export interface UserDelegatableDocumentDto {
    serFormId: number;
    txtTemplateName?: string;
    txtCodeConvention?: string;
}

export interface UserDelegationSaveRequest {
    serDelegationId?: number;
    serDelegateUserId: number;
    serOnBehalfUserId: number;
    dteValidFrom: string;
    dteValidTo: string;
    documentIds: string[];
    txtNotes?: string;
    attachmentBase64?: string;
    attachmentFileName?: string;
    removeAttachment?: boolean;
    notifyBothParties?: boolean;
}

@Injectable({
    providedIn: 'root',
})
export class UserDelegationService {
    constructor(private http: HttpClient) {}

    getAll(filters?: { status?: string; delegateUserId?: number; onBehalfUserId?: number }): Observable<UserDelegationDto[]> {
        let params = new HttpParams();
        if (filters?.status) {
            params = params.set('status', filters.status);
        }
        if (filters?.delegateUserId) {
            params = params.set('delegateUserId', String(filters.delegateUserId));
        }
        if (filters?.onBehalfUserId) {
            params = params.set('onBehalfUserId', String(filters.onBehalfUserId));
        }
        return this.http.get<UserDelegationDto[]>(urls.API_URL + 'getAllUserDelegations', { params });
    }

    getById(id: number): Observable<UserDelegationDto> {
        const params = new HttpParams().set('id', String(id));
        return this.http.get<UserDelegationDto>(urls.API_URL + 'getUserDelegationById', { params });
    }

    getDelegatableDocuments(onBehalfUserId: number): Observable<UserDelegatableDocumentDto[]> {
        const params = new HttpParams().set('onBehalfUserId', String(onBehalfUserId));
        return this.http.get<UserDelegatableDocumentDto[]>(
            urls.API_URL + 'getDelegatableDocumentsForDelegation',
            { params }
        );
    }

    attachmentDownloadUrl(delegationId: number): string {
        return `${urls.API_URL}getUserDelegationAttachment?id=${encodeURIComponent(String(delegationId))}`;
    }

    downloadAttachment(delegationId: number, fileName?: string): Observable<Blob> {
        const params = new HttpParams().set('id', String(delegationId));
        return this.http.get(urls.API_URL + 'getUserDelegationAttachment', {
            params,
            responseType: 'blob',
        });
    }

    create(payload: UserDelegationSaveRequest): Observable<UserDelegationDto> {
        return this.http
            .post<{ status: string; data?: UserDelegationDto; message?: string }>(
                urls.API_URL + 'addNewUserDelegation',
                payload
            )
            .pipe(
                map((res) => {
                    if (res?.status !== 'Success' || !res.data) {
                        throw new Error(res?.message || 'Could not create delegation');
                    }
                    return res.data;
                })
            );
    }

    update(payload: UserDelegationSaveRequest): Observable<UserDelegationDto> {
        return this.http
            .post<{ status: string; data?: UserDelegationDto; message?: string }>(
                urls.API_URL + 'updateUserDelegation',
                payload
            )
            .pipe(
                map((res) => {
                    if (res?.status !== 'Success' || !res.data) {
                        throw new Error(res?.message || 'Could not update delegation');
                    }
                    return res.data;
                })
            );
    }

    revoke(serDelegationId: number): Observable<UserDelegationDto> {
        return this.http
            .post<{ status: string; data?: UserDelegationDto; message?: string }>(
                urls.API_URL + 'revokeUserDelegation',
                { serDelegationId }
            )
            .pipe(
                map((res) => {
                    if (res?.status !== 'Success' || !res.data) {
                        throw new Error(res?.message || 'Could not revoke delegation');
                    }
                    return res.data;
                })
            );
    }

    delete(serDelegationId: number): Observable<void> {
        return this.http
            .post<{ status: string; message?: string }>(urls.API_URL + 'deleteUserDelegation', { serDelegationId })
            .pipe(
                map((res) => {
                    if (res?.status !== 'Success') {
                        throw new Error(res?.message || 'Could not delete delegation');
                    }
                })
            );
    }
}
