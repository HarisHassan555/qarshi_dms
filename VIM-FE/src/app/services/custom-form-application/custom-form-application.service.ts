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
    const sanitized = this.stripTransientFields(payload);
    return this.http.post(urls.API_URL + 'updateApplication', sanitized);
  }

  updateApplicationPdf(applicationId: number, pdfBlob: Blob, filename?: string, refreshCapfSignatures?: boolean) {
    const formData = new FormData();
    formData.append('applicationId', String(applicationId));
    formData.append('pdf', pdfBlob, filename || 'application.pdf');
    if (refreshCapfSignatures) {
      formData.append('refreshCapfSignatures', 'true');
    }
    return this.http.post(urls.API_URL + 'updateApplicationPdf', formData);
  }

  deleteApplication(applicationId: number) {
    return this.http.post(urls.API_URL + 'deleteApplication?applicationId=' + applicationId, {});
  }

  getApplicationsByStatus(status: string) {
    return this.http.get(urls.API_URL + 'getApplicationsByStatus?status=' + status);
  }

  getApplicationsByStatusAndUserId(status: string, userId: number) {
    return this.http.get(urls.API_URL + `getApplicationsByStatusAndUserId?status=${status}&userId=${userId}`);
  }

  getApplicationsApprovedByUser(status: string, userId: number) {
    return this.http.get(urls.API_URL + `getApplicationsApprovedByUser?status=${status}&userId=${userId}`);
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

  approveApplication(applicationId: number, remarks?: string, approverUserId?: number) {
    const body: any = {
      applicationId: applicationId,
      remarks: remarks || ''
    };
    if (approverUserId != null) {
      body.approverUserId = approverUserId;
    }
    return this.http.post(urls.API_URL + 'approveApplication', body);
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

  sendBackToInitiator(applicationId: number, remarks: string) {
    return this.http.post(urls.API_URL + 'sendBackToInitiator', {
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

  assignPrCode(applicationId: number, prCode: string, userId?: number) {
    return this.http.post(urls.API_URL + 'assignPrCode', null, {
      params: {
        applicationId: applicationId as any,
        prCode: prCode,
        userId: userId ?? ''
      }
    });
  }

  private stripTransientFields(payload: any): any {
    if (!payload || typeof payload !== 'object') return payload;
    const sanitized = { ...payload };
    // Frontend-only flag; backend entity does not allow it.
    if ('isCapfForm' in sanitized) {
      delete (sanitized as any).isCapfForm;
    }
    return sanitized;
  }
}
