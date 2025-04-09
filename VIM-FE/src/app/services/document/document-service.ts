import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {urls} from "../../utils/urls";

@Injectable({
    providedIn: 'root',
})
export class DocumentService {


    constructor(private http: HttpClient) {}

    removeDocument(documentId: string): Observable<string> {
        return this.http.post<string>(urls.API_URL + `removeCandidateDocument`, JSON.stringify(documentId));
    }

    downloadDocument(documentId: string): Observable<Blob> {
        return this.http.post(urls.API_URL + `downloadDocument`, documentId, {
            responseType: 'blob',
        });
    }

    uploadDocument(formData: FormData) {
        return this.http.post(urls.API_URL + `uploadCandidateDocument`, formData);
    }


}
