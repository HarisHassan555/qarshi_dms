import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class CustomFormApplicationService {

  constructor(
    private http: HttpClient
  ) { }

  submitApplication(payload: any) {
    return this.http.post(urls.API_URL + 'submitApplication', payload);
  }

  getAllApplications() {
    return this.http.get(urls.API_URL + 'getAllApplications');
  }

  getApplicationsByFormId(formId: number) {
    return this.http.get(urls.API_URL + 'getApplicationsByFormId?formId=' + formId);
  }

  getApplicationsByUserId(userId: number) {
    return this.http.get(urls.API_URL + 'getApplicationsByUserId?userId=' + userId);
  }

  getApplicationById(applicationId: number) {
    return this.http.get(urls.API_URL + 'getApplicationById?applicationId=' + applicationId);
  }

  updateApplication(payload: any) {
    return this.http.post(urls.API_URL + 'updateApplication', payload);
  }

  updateApplicationPdf(applicationId: number, pdfBlob: Blob, filename?: string) {
    const formData = new FormData();
    formData.append('applicationId', String(applicationId));
    formData.append('pdf', pdfBlob, filename || 'application.pdf');
    return this.http.post(urls.API_URL + 'updateApplicationPdf', formData);
  }

  deleteApplication(applicationId: number) {
    return this.http.post(urls.API_URL + 'deleteApplication?applicationId=' + applicationId, {});
  }

  getApplicationsByStatus(status: string) {
    return this.http.get(urls.API_URL + 'getApplicationsByStatus?status=' + status);
  }

  getNextApplicationCode(formId: number) {
    return this.http.get(urls.API_URL + 'getNextApplicationCode?formId=' + formId);
  }

  getApplicationsPendingApproval(departmentHeadUserId: number) {
    return this.http.get(urls.API_URL + 'getApplicationsPendingApproval?departmentHeadUserId=' + departmentHeadUserId);
  }

  getAllApplicationsPendingApproval() {
    return this.http.get(urls.API_URL + 'getAllApplicationsPendingApproval');
  }

  approveApplication(applicationId: number, remarks?: string) {
    return this.http.post(urls.API_URL + 'approveApplication', {
      applicationId: applicationId,
      remarks: remarks || ''
    });
  }

  rejectApplication(applicationId: number, remarks?: string) {
    return this.http.post(urls.API_URL + 'rejectApplication', {
      applicationId: applicationId,
      remarks: remarks || ''
    });
  }

  sendBackApplication(applicationId: number, remarks: string) {
    return this.http.post(urls.API_URL + 'sendBackApplication', {
      applicationId: applicationId,
      remarks: remarks
    });
  }

  sendSubmissionEmails(applicationId: number) {
    return this.http.post(urls.API_URL + 'sendSubmissionEmails?applicationId=' + applicationId, {});
  }

  assignAssetCode(applicationId: number, assetCode: string, userId?: number) {
    return this.http.post(urls.API_URL + 'assignAssetCode', null, {
      params: {
        applicationId: applicationId as any,
        assetCode: assetCode,
        userId: userId ?? ''
      }
    });
  }
}

