import { AfterViewChecked, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { ApplicationPdfService } from '../../services/application-pdf/application-pdf.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { DepartmentService } from '../../services/department/department.service';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';
import { AbcComponent } from '../../pages/abc/abc.component';
import { urls } from 'src/app/utils/urls';
import { finalize, firstValueFrom, forkJoin } from 'rxjs';

/** Blocks for DOM height–based generic pagination (same approach as /application preview). */
interface DetailsGenericBlock {
  key: string;
  field: any;
  showLabel: boolean;
  /** Pre-split rich HTML for one measured block (optional). */
  wordEditorChunkHtml?: string;
}

@Component({
  selector: 'app-application-details',
  templateUrl: './application-details.component.html',
  styleUrls: ['./application-details.component.css']
})
export class ApplicationDetailsComponent implements OnInit, AfterViewChecked, OnDestroy {
  applicationId: number | null = null;
  applicationDetails: any = null;
  formFields: any[] = [];
  applicationFormData: any = {};
  forms: any[] = [];
  isLoading: boolean = true;
  approvalHistory: any[] = []; // Store approval history with remarks
  departmentNameMap: Map<number, string> = new Map();
  departmentHeadMap: Map<number, any> = new Map();
  userNameMap: Map<number, string> = new Map();
  currentUser: any = null;

  // PDF Generation
  isGeneratingPdf: boolean = false;
  pdfBlobUrl: string | null = null;


  fromPendingApprovals: boolean = false;
  selectedApplicationForRemarks: any = null;
  remarksText: string = '';
  isApproving: boolean = false;
  isRejecting: boolean = false;
  isSendingBack: boolean = false;
  isSendingBackToInitiator: boolean = false;
  showFeasibilityModal: boolean = false;
  feasibilityPreviewUrl: any = null;
  private feasibilityObjectUrl: string | null = null;

  showQuotationModal: boolean = false;
  quotationAttachments: any[] = [];
  selectedQuotation: any = null;
  currentQuotationPreviewUrl: any = null;
  private quotationObjectUrl: string | null = null;

  isEditingVendor: boolean = false;
  vendorEditForm: any = {
    vendorName: '',
    vendorAddress: '',
    approvedPrice: '',
    deliveryPeriod: '',
    termsConditions: ''
  };
  isSavingVendor: boolean = false;

  assetCodeInput: string = '';
  isSavingAssetCode: boolean = false;

  prCodeInput: string = '';
  isSavingPrCode: boolean = false;

  /** For generic form: body fields split into pages (each page ≈ A4). */
  genericPages: any[][] = [];
  /** When individual footer pipeline: pages from real DOM height measurement (matches /application). */
  genericMeasurePages: DetailsGenericBlock[][] = [];
  /** Blocks rendered in the hidden measure row (kept in sync in rebuild). */
  detailsGenericMeasureBlocks: DetailsGenericBlock[] = [];
  private genericMeasureLayoutSignature = '';
  private genericMeasureFrame: number | null = null;
  private genericMeasurePending = false;
  @ViewChild('genericMeasurePaper') genericMeasurePaper?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureHeader') genericMeasureHeader?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureContent') genericMeasureContent?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureFooter') genericMeasureFooter?: ElementRef<HTMLElement>;
  /** For budget approval form: HTML content split into pages. */
  budgetPages: SafeHtml[] = [];

  hasFeasibilityReport(): boolean {
    const report = this.applicationFormData?.feasibility_report_attached
      ?? this.applicationFormData?.feasibility_attached_report;
    if (report && this.isPreviewableAttachment(report)) return true;
    const fieldValue = this.getFeasibilityFieldValue();
    if (fieldValue && this.isPreviewableAttachment(fieldValue)) return true;
    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string' && raw.includes('feasibility_report_attached')) {
      return raw.includes('data:application') || raw.includes('base64,') || raw.includes('"feasibility_report_attached"');
    }
    if (typeof raw === 'string' && raw.includes('feasibility_attached_report')) {
      return raw.includes('data:application') || raw.includes('base64,') || raw.includes('"feasibility_attached_report"');
    }
    return false;
  }

  isFinance(): boolean {
    if (!this.currentUser) return false;
    const role = (this.currentUser?.cfgTblRole?.txtRoleName || this.currentUser?.txtrole || '').toUpperCase();
    return role.includes('FINANCE');
  }

  showAssetCodeForm(): boolean {
    return this.applicationDetails?.txtStatus === 'ASSET_PENDING' && this.isFinance();
  }

  showPrCodeForm(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;
    const status = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    const hasAsset = !!(this.applicationDetails?.txtAssetCode && String(this.applicationDetails.txtAssetCode).trim());
    const hasPr = !!(this.applicationDetails?.txtPrCode && String(this.applicationDetails.txtPrCode).trim());
    if (status !== 'APPROVED') return false;
    if (!hasAsset || hasPr) return false;
    // Business: PR code assignment is typically done by Procurement HOD.
    return this.isProcurementHod();
  }

  saveAssetCode() {
    if (!this.applicationId || !this.assetCodeInput.trim()) {
      this.notificationService.showMessage('Asset code is required', 'danger');
      return;
    }
    this.isSavingAssetCode = true;
    this.customFormApplicationService
      .assignAssetCode(this.applicationId, this.assetCodeInput.trim(), this.currentUser?.serUserId)
      .pipe(finalize(() => (this.isSavingAssetCode = false)))
      .subscribe(
        () => {
          this.notificationService.showMessage('Asset code saved; application approved', 'success');
          this.loadApplicationDetails();
        },
        () => {
          this.notificationService.showMessage('Failed to save asset code', 'danger');
        }
      );
  }

  savePrCode() {
    if (!this.applicationId || !this.prCodeInput.trim()) {
      this.notificationService.showMessage('PR code is required', 'danger');
      return;
    }
    this.isSavingPrCode = true;
    this.customFormApplicationService
      .assignPrCode(this.applicationId, this.prCodeInput.trim(), this.currentUser?.serUserId)
      .pipe(finalize(() => (this.isSavingPrCode = false)))
      .subscribe(
        () => {
          this.notificationService.showMessage('PR code saved successfully', 'success');
          this.loadApplicationDetails();
        },
        () => {
          this.notificationService.showMessage('Failed to save PR code', 'danger');
        }
      );
  }

  private getFeasibilityReportValue(): any {
    const direct = this.applicationFormData?.feasibility_report_attached
      ?? this.applicationFormData?.feasibility_attached_report;
    if (direct) {
      this.logFeasibilityValue('direct', direct);
      if (this.isPreviewableAttachment(direct)) return direct;
    }
    const fieldValue = this.getFeasibilityFieldValue();
    if (fieldValue) {
      this.logFeasibilityValue('fieldValue', fieldValue);
      if (this.isPreviewableAttachment(fieldValue)) return fieldValue;
    }
    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string') {
      try {
        const parsed = JSON.parse(raw);
        const candidate = parsed?.feasibility_report_attached ?? parsed?.feasibility_attached_report;
        if (candidate) {
          this.logFeasibilityValue('parsedCandidate', candidate);
          if (this.isPreviewableAttachment(candidate)) return candidate;
        }
        const found = this.findFeasibilityAttachmentInData(parsed);
        if (found) {
          this.logFeasibilityValue('foundInData', found);
          if (this.isPreviewableAttachment(found)) return found;
        }
      } catch {
        return null;
      }
    }
    return null;
  }

  private getFeasibilityFieldValue(): any {
    if (!this.formFields || this.formFields.length === 0) return null;
    const field = this.formFields.find((f: any) =>
      (f?.label || '').toString().toLowerCase().includes('feasibility')
    );
    if (!field) return null;
    return this.getFieldValue(field);
  }

  private findFeasibilityAttachmentInData(parsed: any): any {
    if (!parsed || typeof parsed !== 'object') return null;
    const keys = Object.keys(parsed);
    for (const key of keys) {
      if (!key) continue;
      const keyLower = key.toLowerCase();
      if (!keyLower.includes('feasibility')) continue;
      const val = parsed[key];
      if (val && typeof val === 'object') {
        if (val.dataUrl || val.base64 || val.data || val.content || val.fileBase64 || val.fileData) return val;
      }
      if (typeof val === 'string' && this.looksLikeBase64(val)) return val;
    }
    return null;
  }

  private isPreviewableAttachment(value: any): boolean {
    if (!value) return false;
    if (typeof value === 'object') {
      return !!(value.dataUrl || value.base64 || value.data || value.content || value.fileBase64 || value.fileData);
    }
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (!trimmed) return false;
      if (trimmed.startsWith('data:')) return true;
      if (this.looksLikeFileName(trimmed)) return false;
      return this.looksLikeBase64(trimmed);
    }
    return false;
  }

  private logFeasibilityValue(label: string, value: any) {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      console.log(`[feasibility:${label}] type=string len=${trimmed.length} prefix=${trimmed.slice(0, 30)}`);
    } else if (value && typeof value === 'object') {
      console.log(`[feasibility:${label}] type=object keys=${Object.keys(value).join(',')}`);
    } else {
      console.log(`[feasibility:${label}] type=${typeof value}`);
    }
  }

  private looksLikeBase64(value: string): boolean {
    if (!value) return false;
    const trimmed = value.trim();
    if (!trimmed) return false;
    if (trimmed.startsWith('data:')) return true;
    if (trimmed.length < 100) return false;
    if (/[^A-Za-z0-9+/=]/.test(trimmed)) return false;
    return true;
  }

  private looksLikeFileName(value: string): boolean {
    if (!value) return false;
    const trimmed = value.trim();
    if (trimmed.length > 200) return false;
    if (trimmed.startsWith('data:')) return false;
    return /\.[a-z0-9]{2,5}$/i.test(trimmed);
  }

  private inferMimeType(base64: string): string {
    if (!base64) return 'application/octet-stream';
    const trimmed = base64.trim();
    if (trimmed.startsWith('JVBER')) return 'application/pdf';
    if (trimmed.startsWith('/9j/')) return 'image/jpeg';
    if (trimmed.startsWith('iVBOR')) return 'image/png';
    return 'application/octet-stream';
  }

  getFeasibilityReportDataUrl(): string {
    const report = this.getFeasibilityReportValue();
    if (!report) return '';
    if (typeof report === 'object' && report.dataUrl) return String(report.dataUrl);
    if (typeof report === 'object') {
      const candidate = report.base64 || report.data || report.content || report.fileBase64 || report.fileData;
      if (typeof candidate === 'string') {
        const trimmed = candidate.trim();
        if (!trimmed) return '';
        if (trimmed.startsWith('data:')) return trimmed;
        const mime = report.mimeType || report.type || this.inferMimeType(trimmed);
        const dataUrl = `data:${mime};base64,${trimmed}`;
        console.log(`[feasibility:dataUrl] len=${dataUrl.length} prefix=${dataUrl.slice(0, 40)}`);
        return dataUrl;
      }
    }
    if (typeof report === 'string') {
      const trimmed = report.trim();
      if (!trimmed) return '';
      if (trimmed.startsWith('data:')) return trimmed;
      if (this.looksLikeFileName(trimmed) && !this.looksLikeBase64(trimmed)) {
        return '';
      }
      const mime = this.inferMimeType(trimmed);
      const dataUrl = `data:${mime};base64,${trimmed}`;
      console.log(`[feasibility:dataUrl] len=${dataUrl.length} prefix=${dataUrl.slice(0, 40)}`);
      return dataUrl;
    }
    return '';
  }

  openFeasibilityReport() {
    if (!this.isCapfForm() || !this.hasFeasibilityReport()) {
      this.notificationService.showMessage('Feasibility report not available', 'danger');
      return;
    }
    if (this.feasibilityObjectUrl) {
      URL.revokeObjectURL(this.feasibilityObjectUrl);
      this.feasibilityObjectUrl = null;
    }
    const dataUrl = this.getFeasibilityReportDataUrl();
    console.log('dataUrl', dataUrl);
    if (!dataUrl) {
      this.notificationService.showMessage('Feasibility report not available', 'danger');
      return;
    }
    // Convert base64 to Blob for better PDF rendering in iframe.
    if (dataUrl.startsWith('data:')) {
      const [meta, base64] = dataUrl.split(',', 2);
      const mime = meta?.match(/data:([^;]+);base64/)?.[1] || 'application/octet-stream';
      try {
        const byteString = atob(base64 || '');
        const bytes = new Uint8Array(byteString.length);
        for (let i = 0; i < byteString.length; i++) {
          bytes[i] = byteString.charCodeAt(i);
        }
        const blob = new Blob([bytes], { type: mime });
        this.feasibilityObjectUrl = URL.createObjectURL(blob);
        this.feasibilityPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.feasibilityObjectUrl);
      } catch {
        this.feasibilityPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(dataUrl);
      }
    } else {
      this.feasibilityPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(dataUrl);
    }
    this.showFeasibilityModal = true;
  }

  closeFeasibilityModal() {
    this.showFeasibilityModal = false;
    this.feasibilityPreviewUrl = null;
    if (this.feasibilityObjectUrl) {
      URL.revokeObjectURL(this.feasibilityObjectUrl);
      this.feasibilityObjectUrl = null;
    }
  }

  downloadFeasibilityReport() {
    const dataUrl = this.getFeasibilityReportDataUrl();
    if (!dataUrl) {
      this.notificationService.showMessage('Feasibility report data not found', 'danger');
      return;
    }

    // Try to get the original file name if it exists
    let fileName = 'feasibility_report';
    const report = this.getFeasibilityReportValue();
    if (report && typeof report === 'object' && (report.fileName || report.name)) {
      fileName = report.fileName || report.name;
    } else {
      // Determine optional file extension from dataUrl mime type
      const mime = dataUrl.match(/data:([^;]+);base64/)?.[1] || '';
      if (mime === 'application/pdf') fileName += '.pdf';
      else if (mime === 'image/jpeg') fileName += '.jpg';
      else if (mime === 'image/png') fileName += '.png';
    }

    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  isFeasibilityImage(): boolean {
    const url = this.getFeasibilityReportDataUrl();
    return url.startsWith('data:image/');
  }

  showFeasibilityReportButton(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;
    const report = this.getFeasibilityReportValue();
    return this.isPreviewableAttachment(report);
  }

  hasQuotationAttachments(): boolean {
    const attachments = this.getQuotationAttachmentsValue();
    return Array.isArray(attachments) && attachments.length > 0;
  }

  showQuotationAttachmentsButton(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;
    return this.hasQuotationAttachments();
  }

  private getQuotationAttachmentsValue(): any[] {
    const direct = this.applicationFormData?.quotation_attachments;
    if (Array.isArray(direct)) return direct;

    const fieldName = this.getFieldName('Quotation Attachments');
    const fromFieldName = this.applicationFormData[fieldName];
    if (Array.isArray(fromFieldName)) return fromFieldName;

    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string') {
      try {
        const parsed = JSON.parse(raw);
        const candidate = parsed?.quotation_attachments || parsed[fieldName];
        if (Array.isArray(candidate)) return candidate;
      } catch { }
    }
    return [];
  }

  /** All form attachment fields (attachment + multi_attachment) for view/download on application-details only */
  getFormAttachmentsForView(): { fieldLabel: string; files: any[] }[] {
    const out: { fieldLabel: string; files: any[] }[] = [];
    const fields = this.getGenericPreviewFields() || [];
    for (const field of fields) {
      if (!this.isAttachmentType(field.type) && !this.isMultiAttachmentType(field.type)) continue;
      const value = this.getFieldValue(field);
      const files: any[] = Array.isArray(value) ? value : (value != null && typeof value === 'object' ? [value] : []);
      const valid = files.filter((f: any) => f && (f.fileName || f.base64 || f.dataUrl || f.data || f.content));
      if (valid.length > 0) {
        out.push({ fieldLabel: field.label || 'Attachment', files: valid });
      }
    }
    return out;
  }

  hasAnyFormAttachments(): boolean {
    return this.getFormAttachmentsForView().some((g) => g.files.length > 0);
  }

  openFormAttachmentsModal() {
    const groups = this.getFormAttachmentsForView();
    const flat: any[] = [];
    for (const g of groups) {
      for (const file of g.files) {
        flat.push({ ...file, _fieldLabel: g.fieldLabel });
      }
    }
    if (flat.length === 0) {
      this.notificationService.showMessage('No attachments available', 'info');
      return;
    }
    this.quotationAttachments = flat;
    this.showQuotationModal = true;
    this.previewQuotation(flat[0]);
  }

  openQuotationAttachmentsModal() {
    this.quotationAttachments = this.getQuotationAttachmentsValue();
    if (this.quotationAttachments.length === 0) {
      this.notificationService.showMessage('No quotation attachments available', 'danger');
      return;
    }
    this.showQuotationModal = true;

    // Auto-select and preview the first attachment by default
    if (this.quotationAttachments.length > 0) {
      this.previewQuotation(this.quotationAttachments[0]);
    }
  }

  closeQuotationModal() {
    this.showQuotationModal = false;
    this.selectedQuotation = null;
    this.currentQuotationPreviewUrl = null;
    if (this.quotationObjectUrl) {
      URL.revokeObjectURL(this.quotationObjectUrl);
      this.quotationObjectUrl = null;
    }
  }

  previewQuotation(attachment: any) {
    this.selectedQuotation = attachment;
    if (this.quotationObjectUrl) {
      URL.revokeObjectURL(this.quotationObjectUrl);
      this.quotationObjectUrl = null;
    }

    let dataUrl = '';
    if (typeof attachment === 'object') {
      dataUrl = attachment.dataUrl || '';
      if (!dataUrl) {
        const content = attachment.base64 || attachment.data || attachment.content || attachment.fileBase64 || attachment.fileData;
        if (content) {
          const mime = attachment.mimeType || attachment.type || this.inferMimeType(content);
          dataUrl = `data:${mime};base64,${content}`;
        }
      }
    }

    if (!dataUrl) {
      this.notificationService.showMessage('Attachment data not found', 'danger');
      return;
    }

    if (dataUrl.startsWith('data:')) {
      const [meta, base64] = dataUrl.split(',', 2);
      const mime = meta?.match(/data:([^;]+);base64/)?.[1] || 'application/octet-stream';
      try {
        const byteString = atob(base64 || '');
        const bytes = new Uint8Array(byteString.length);
        for (let i = 0; i < byteString.length; i++) {
          bytes[i] = byteString.charCodeAt(i);
        }
        const blob = new Blob([bytes], { type: mime });
        this.quotationObjectUrl = URL.createObjectURL(blob);
        this.currentQuotationPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.quotationObjectUrl);
      } catch {
        this.currentQuotationPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(dataUrl);
      }
    } else {
      this.currentQuotationPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(dataUrl);
    }
  }

  downloadCurrentQuotation() {
    if (!this.selectedQuotation) return;

    let dataUrl = '';
    const att = this.selectedQuotation;
    if (typeof att === 'object') {
      dataUrl = att.dataUrl || '';
      if (!dataUrl) {
        const content = att.base64 || att.data || att.content || att.fileBase64 || att.fileData;
        if (content) {
          const mime = att.mimeType || att.type || this.inferMimeType(content);
          dataUrl = `data:${mime};base64,${content}`;
        }
      }
    }

    if (!dataUrl) {
      this.notificationService.showMessage('Download data not found', 'danger');
      return;
    }

    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = att.fileName || att.name || 'quotation_attachment';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  isQuotationImage(attachment: any): boolean {
    if (!attachment) return false;
    const content = attachment.dataUrl || attachment.base64 || attachment.data || attachment.content || attachment.fileBase64 || attachment.fileData || '';
    if (typeof content === 'string' && content.startsWith('data:image/')) return true;
    const mime = attachment.mimeType || attachment.type || '';
    return mime.startsWith('image/');
  }

  isAdmin(): boolean {
    if (!this.currentUser) return false;
    const role = (this.currentUser.cfgTblRole?.txtRoleName || this.currentUser.txtrole || '').trim().toUpperCase();
    return role === 'ADMIN' || role === 'SUPER ADMIN' || role === 'ROLE_ADMIN' || role === 'ROLE_SUPER ADMIN';
  }

  isProcurementUser(): boolean {
    if (!this.currentUser) return false;
    const deptName = this.currentUser.hrTblDepartment?.txtDepartmentName ||
      this.currentUser.departmentName ||
      this.currentUser.txtDepartmentName ||
      '';
    const deptCode = this.currentUser.hrTblDepartment?.txtDepartmentCode ||
      this.currentUser.departmentCode ||
      '';
    const roleName = this.currentUser.cfgTblRole?.txtRoleName ||
      this.currentUser.txtrole ||
      '';

    const name = deptName.trim().toUpperCase();
    const code = deptCode.trim().toUpperCase();
    const role = roleName.trim().toUpperCase();

    return name.includes('PROCUREMENT') || name === 'PRC' ||
      code === 'PRC' || code.includes('PROC') ||
      role.includes('PROCURE');
  }

  isProcurementHod(): boolean {
    if (!this.currentUser || !this.departmentHeadMap) return false;

    const userId = this.currentUser.serUserId || this.currentUser.userId || this.currentUser.id;
    if (!userId) return false;

    // NOTE: We cannot rely on currentUser.hrTblDepartment because it is not
    // stored in localStorage. Instead, we scan the departmentHeadMap (loaded
    // from the API) to find any Procurement department that lists this user as head.

    const PROCUREMENT_KEYWORDS = ['PROCUREMENT', 'PROCURE', 'PRC', 'PROC'];

    for (const [deptId, headIdsRaw] of this.departmentHeadMap.entries()) {
      const deptName = (this.departmentNameMap.get(deptId) || '').toUpperCase().trim();
      const isProc = PROCUREMENT_KEYWORDS.some(kw => deptName.includes(kw));

      if (isProc) {
        const headIds = String(headIdsRaw || '').split(',').map(h => h.trim()).filter(h => h !== '');
        if (headIds.includes(String(userId))) {
          console.log('[isProcurementHod] MATCH — user', userId, 'is HOD of', deptName, '(deptId:', deptId, ')');
          return true;
        }
      }
    }

    console.log('[isProcurementHod] No match. userId=' + userId + ', mapSize=' + this.departmentHeadMap.size);
    return false;
  }

  canEditVendorDetails(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;

    // The user MUST come from the Pending Approvals page to see this button
    // AND must be authorized (Procurement HOD or similar authorized role)
    if (!this.fromPendingApprovals) return false;

    const isProcHod = this.isProcurementHod();

    // Status check: only allow editing on PENDING or IN_PROGRESS applications
    const rawStatus = (this.applicationDetails?.txtStatus || '').toUpperCase();
    const isPending = rawStatus === 'PENDING' || rawStatus === '' || rawStatus === 'NEW' || rawStatus === 'IN_PROGRESS';

    // Debug log — visible in browser console
    console.log('[canEditVendorDetails]', { isProcHod, fromPending: this.fromPendingApprovals, rawStatus, isPending });

    return isProcHod && isPending;
  }

  startEditingVendor() {
    this.vendorEditForm = {
      vendorName: this.getFieldValueByLabel('NAME') || this.getFieldValueByLabel('Vendor Name'),
      vendorAddress: this.getFieldValueByLabel('ADDRESS') || this.getFieldValueByLabel('Address'),
      approvedPrice: this.getFieldValueByLabel('APPROVED PRICE') || this.getFieldValueByLabel('Approved price'),
      deliveryPeriod: this.getFieldValueByLabel('DELIVERY PERIOD & DATE') || this.getFieldValueByLabel('DELIVERY PERIOD'),
      termsConditions: this.getFieldValueByLabel('TERMS & CONDITIONS') || this.getFieldValueByLabel('Terms & Conditions')
    };
    this.isEditingVendor = true;
    if (this.vendorEditModal) {
      this.vendorEditModal.open();
    }
  }

  cancelEditingVendor() {
    this.isEditingVendor = false;
    if (this.vendorEditModal) {
      this.vendorEditModal.close();
    }
  }

  async saveVendorDetails() {
    if (!this.applicationDetails) return;
    this.isSavingVendor = true;
    try {
      // 1. Get existing data
      let appData: any = {};
      const raw = this.applicationDetails.txtApplicationData;
      if (typeof raw === 'string') {
        appData = JSON.parse(raw);
      } else if (typeof raw === 'object') {
        appData = raw;
      }

      // 2. Map labels to keys and update
      const mappings: any = {
        'vendor_name': this.vendorEditForm.vendorName,
        'vendor_address': this.vendorEditForm.vendorAddress,
        'approved_price': this.vendorEditForm.approvedPrice,
        'delivery_period': this.vendorEditForm.deliveryPeriod,
        'terms_conditions': this.vendorEditForm.termsConditions
      };

      // Also update based on Field Labels to be safe
      const fieldNameMap: any = {
        'NAME': 'vendorName',
        'Vendor Name': 'vendorName',
        'ADDRESS': 'vendorAddress',
        'Address': 'vendorAddress',
        'APPROVED PRICE': 'approvedPrice',
        'Approved price': 'approvedPrice',
        'DELIVERY PERIOD & DATE': 'deliveryPeriod',
        'DELIVERY PERIOD': 'deliveryPeriod',
        'TERMS & CONDITIONS': 'termsConditions',
        'Terms & Conditions': 'termsConditions'
      };

      for (const [label, formKey] of Object.entries(fieldNameMap)) {
        appData[label] = this.vendorEditForm[formKey as string];
        const derived = this.getFieldName(label);
        appData[derived] = this.vendorEditForm[formKey as string];
      }

      // Explicitly update fixed keys used by template
      for (const [key, value] of Object.entries(mappings)) {
        appData[key] = value;
      }

      // 3. Prepare application object for update
      const updatedApp = {
        ...this.applicationDetails,
        txtApplicationData: JSON.stringify(appData)
      };

      // 4. Call API
      const sanitizedApp: any = { ...updatedApp };
      if ('isCapfForm' in sanitizedApp) {
        delete sanitizedApp.isCapfForm;
      }
      const response: any = await this.http.post(`${urls.API_URL}updateApplication`, sanitizedApp).toPromise();
      if (response && response.status === 'Success') {
        this.notificationService.showMessage('Vendor details updated successfully', 'success');
        this.applicationDetails.txtApplicationData = updatedApp.txtApplicationData;
        this.applicationFormData = appData;
        this.isEditingVendor = false;
        if (this.vendorEditModal) {
          this.vendorEditModal.close();
        }

        if (this.isCapfForm() && this.applicationDetails?.serApplicationId) {
          void this.refreshCapfPdfSnapshotAfterVendorUpdate(this.applicationDetails.serApplicationId);
        }
      } else {
        this.notificationService.showMessage(response?.message || 'Failed to update vendor details', 'danger');
      }
    } catch (error) {
      console.error('Error saving vendor details:', error);
      this.notificationService.showMessage('An error occurred while saving', 'danger');
    } finally {
      this.isSavingVendor = false;
    }
  }

  /** Maps DB form fields to ApplicationPdfService shape (same as applications-view edit PDF refresh). */
  private mapCfgFormFieldsToPdfFields(form: any): any[] {
    if (!form?.cfgTblCustomFormFields) {
      return [];
    }
    return form.cfgTblCustomFormFields
      .map((field: any) => ({
        serFieldId: field.serFieldId,
        label: field.txtFieldLabel,
        type: field.txtFieldType,
        required: field.blIsRequired || false,
        placeholder: field.txtPlaceholder || '',
        intFieldOrder: field.intFieldOrder || 0,
        txtFieldOptions: field.txtFieldOptions
      }))
      .sort((a: any, b: any) => (a.intFieldOrder || 0) - (b.intFieldOrder || 0));
  }

  /**
   * Regenerates CAPF PDF from updated application JSON (e.g. procurement vendor edits), replaces stage-0
   * base snapshot, and re-applies department/CEO signatures on the server so emails/previews stay correct.
   */
  private async refreshCapfPdfSnapshotAfterVendorUpdate(applicationId: number): Promise<void> {
    if (!this.isCapfForm()) {
      return;
    }
    try {
      const application: any = await firstValueFrom(
        this.customFormApplicationService.getApplicationById(applicationId)
      );
      if (!application) {
        return;
      }
      const form = this.forms.find((f: any) => f.serFormId === application.serFormId);
      if (!form) {
        return;
      }
      const formFields = this.mapCfgFormFieldsToPdfFields(form);
      let appData: any = {};
      try {
        appData = application.txtApplicationData ? JSON.parse(application.txtApplicationData) : {};
      } catch {
        appData = {};
      }
      const formName = (form?.txtFormName || application?.cfgTblCustomForm?.txtFormName || '').trim();
      const formCode = (application?.txtFormCode || '').trim();
      const htmlContent = this.applicationPdfService.buildPdfHtmlForApplication(
        application,
        form,
        formFields,
        appData,
        { formName, txtFormCode: formCode, omitApprovalSignaturesInPdf: true }
      );
      if (!htmlContent) {
        return;
      }
      const filename = `application_${formCode || applicationId}.pdf`;
      const pdfBlob = await this.applicationPdfService.renderHtmlToPdfBlob(htmlContent, filename);
      const pdfResponse: any = await firstValueFrom(
        this.customFormApplicationService.updateApplicationPdf(applicationId, pdfBlob, filename, true)
      );
      if (!pdfResponse || pdfResponse.status !== 'Success') {
        this.notificationService.showMessage(
          pdfResponse?.message || 'Vendor details saved, but the PDF snapshot could not be refreshed.',
          'warning'
        );
      } else {
        this.loadApplicationDetails();
      }
    } catch (e) {
      console.warn('CAPF PDF refresh after vendor update failed', e);
      this.notificationService.showMessage(
        'Vendor details saved, but the PDF snapshot could not be refreshed.',
        'warning'
      );
    }
  }

  private getFieldValueByLabel(label: string): string {
    if (!this.applicationFormData) return '';
    const derivedKey = this.getFieldName(label);
    return this.applicationFormData[label] || this.applicationFormData[derivedKey] || '';
  }

  private openInNewTab(url: string): boolean {
    if (!url) return false;
    const a = document.createElement('a');
    a.href = url;
    a.target = '_blank';
    a.rel = 'noopener';
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    return true;
  }

  // Signature and Content properties for Budget Approval
  safeContent: SafeHtml = '';
  preparedBy: any = null;
  reviewers: any[] = [];
  recommenders: any[] = [];
  approver: any = null;
  footerFields: any[] = [];
  formHeading: string = 'Budget Approval Form';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private customFormApplicationService: CustomFormApplicationService,
    private applicationPdfService: ApplicationPdfService,
    private customFormService: CustomFormService,
    private departmentService: DepartmentService,
    private userService: UserService,
    private notificationService: NotificationService,
    private sanitizer: DomSanitizer,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      try {
        this.currentUser = JSON.parse(userJson);
      } catch (e) {
        console.error('Error parsing user data:', e);
      }
    }
    // Get application ID from route - load forms & departments first so CAPF detection has form data
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.applicationId = idParam ? parseInt(idParam, 10) : null;
      if (this.applicationId && !isNaN(this.applicationId)) {
        forkJoin({
          forms: this.customFormService.getAll(),
          departments: this.departmentService.getAll()
        }).subscribe(
          ({ forms, departments }) => {
            if (Array.isArray(forms)) this.forms = forms;
            if (Array.isArray(departments)) {
              const map = new Map<number, string>();
              const headMap = new Map<number, number>();
              departments.forEach((d: any) => {
                const id = d?.serDepartmentId;
                const name = d?.txtDepartmentName;
                if (id != null && name) map.set(Number(id), String(name));
                const headId = d?.serDepartmentHeadId;
                if (id != null && headId != null) headMap.set(Number(id), headId);
              });
              this.departmentNameMap = map;
              this.departmentHeadMap = headMap;
            }
            this.loadApplicationDetails();
          },
          () => {
            this.loadApplicationDetails();
          }
        );
      } else {
        this.notificationService.showMessage('Invalid application ID', 'danger');
        this.router.navigate(['/applicationsview']);
      }
    });
    this.route.queryParamMap.subscribe(params => {
      this.fromPendingApprovals = params.get('from') === 'pending';
    });
    this.loadUsers();
  }

  ngAfterViewChecked(): void {
    if (typeof window === 'undefined' || !this.shouldUseMeasuredGenericPagination()) {
      return;
    }
    if (this.genericMeasurePending) {
      return;
    }
    this.genericMeasurePending = true;
    this.genericMeasureFrame = window.requestAnimationFrame(() => {
      this.genericMeasurePending = false;
      this.genericMeasureFrame = null;
      this.rebuildGenericMeasurePagesIfNeeded();
    });
  }

  ngOnDestroy(): void {
    if (this.genericMeasureFrame !== null && typeof window !== 'undefined') {
      window.cancelAnimationFrame(this.genericMeasureFrame);
      this.genericMeasureFrame = null;
    }
  }

  loadForms() {
    this.customFormService.getAll().subscribe(
      (data: any) => {
        if (data) {
          this.forms = data;
        }
      },
      (error) => {
        console.error('Error loading forms:', error);
      }
    );
  }

  loadDepartments() {
    this.departmentService.getAll().subscribe(
      (data: any) => {
        if (Array.isArray(data)) {
          const map = new Map<number, string>();
          const headMap = new Map<number, number>();
          data.forEach((d: any) => {
            const id = d?.serDepartmentId;
            const name = d?.txtDepartmentName;
            if (id != null && name) {
              map.set(Number(id), String(name));
            }
            const headId = d?.serDepartmentHeadId;
            if (id != null && headId != null) {
              headMap.set(Number(id), headId);
            }
          });
          this.departmentNameMap = map;
          this.departmentHeadMap = headMap;
          this.enrichPipelineWithDepartmentNames();
          this.applyDepartmentNamesToApprovalHistory();

          // Debug: log HOD check result after departments load
          const userId = this.currentUser?.serUserId || this.currentUser?.userId;
          const procEntries = Array.from(headMap.entries()).filter(([deptId]) => {
            const name = (map.get(deptId) || '').toUpperCase();
            return ['PROCUREMENT', 'PROCURE', 'PRC', 'PROC'].some(kw => name.includes(kw));
          });
          console.log('[loadDepartments] userId=' + userId + ', procurementDepts:', procEntries, ', isProcurementHod:', this.isProcurementHod());
        }
      },
      (error) => {
        console.error('Error loading departments:', error);
      }
    );
  }

  loadUsers() {
    this.userService.getUsers().subscribe(
      (data: any) => {
        if (!Array.isArray(data)) return;
        const map = new Map<number, string>();
        data.forEach((u: any) => {
          const id = u?.serUserId ?? u?.userId ?? u?.id;
          const name = u?.txtUserName ?? u?.userName ?? u?.name;
          if (id != null && name) {
            map.set(Number(id), String(name));
          }
        });
        this.userNameMap = map;
      },
      (error) => {
        console.error('Error loading users:', error);
      }
    );
  }

  loadApplicationDetails() {
    if (!this.applicationId) return;

    this.isLoading = true;
    this.customFormApplicationService.getApplicationById(this.applicationId).subscribe(
      (data: any) => {
        if (data) {
          this.applicationDetails = data;
          this.genericMeasureLayoutSignature = '';
          this.genericMeasurePages = [];
          this.detailsGenericMeasureBlocks = [];

          // If cfgTblCustomForm is null (lazy-load issue or API omission), fetch and attach it so isCapfForm() works
          if (!data.cfgTblCustomForm && data.serFormId) {
            this.customFormService.getById(data.serFormId).subscribe(
              (formData: any) => {
                if (formData) {
                  this.applicationDetails = { ...this.applicationDetails, cfgTblCustomForm: formData };
                  this.buildGenericPages();
                  this.cdr.detectChanges();
                }
              }
            );
          }

          // Debug: Log pipeline data
          if (data.cfgTblCustomForm) {
            console.log('Form data:', data.cfgTblCustomForm);
            console.log('Approval Pipelines:', data.cfgTblCustomForm.approvalPipelines || data.cfgTblCustomForm.cfgTblCustomFormApprovalPipelines);
          }

          // Get form structure
          const form = this.forms.find(f => f.serFormId === data.serFormId);
          if (form && form.cfgTblCustomFormFields) {
            this.formFields = form.cfgTblCustomFormFields
              .map((field: any) => ({
                serFieldId: field.serFieldId,
                label: field.txtFieldLabel,
                type: field.txtFieldType,
                required: field.blIsRequired || false,
                placeholder: field.txtPlaceholder || '',
                intFieldOrder: field.intFieldOrder || 0,
                txtFieldOptions: field.txtFieldOptions
              }))
              .sort((a: any, b: any) => (a.intFieldOrder || 0) - (b.intFieldOrder || 0));
          } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblCustomFormFields) {
            this.formFields = data.cfgTblCustomForm.cfgTblCustomFormFields
              .map((field: any) => ({
                serFieldId: field.serFieldId,
                label: field.txtFieldLabel,
                type: field.txtFieldType,
                required: field.blIsRequired || false,
                placeholder: field.txtPlaceholder || '',
                intFieldOrder: field.intFieldOrder || 0,
                txtFieldOptions: field.txtFieldOptions
              }))
              .sort((a: any, b: any) => (a.intFieldOrder || 0) - (b.intFieldOrder || 0));
          }

          // Parse application data JSON
          let rawData = data.txtApplicationData || '{}';
          try {
            let parsed = JSON.parse(rawData);
            // Handle double-serialization
            if (typeof parsed === 'string') {
              try {
                parsed = JSON.parse(parsed);
              } catch (e) {
                parsed = { content: parsed };
              }
            }
            // Handle appData wrapper
            this.applicationFormData = parsed.appData || parsed || {};
          } catch (e) {
            console.error('Error parsing application data:', e);
            this.applicationFormData = { content: rawData };
          }

          // Prepare budget approval specific data
          // Ensure this runs even if txtApplicationData was empty
          if (this.isBudgetApprovalForm()) {
            const content = this.applicationFormData.word_editor ||
              this.applicationFormData.budget_approval_form ||
              this.applicationFormData.content ||
              this.applicationFormData.editorContent ||
              this.applicationFormData.richTextContent ||
              this.applicationFormData.txtApplicationData ||
              (typeof rawData === 'string' && !rawData.startsWith('{') ? rawData : '') || '';

            this.safeContent = this.sanitizer.bypassSecurityTrustHtml(content);
            this.buildBudgetPages(String(content || ''));
            this.preparedBy = this.applicationFormData.preparedBy || this.applicationDetails.cfgTblUser;
            this.reviewers = this.applicationFormData.reviewers || [];
            this.recommenders = this.applicationFormData.recommenders || [];
            this.approver = this.applicationFormData.approver;
            this.formHeading = this.applicationFormData.heading || this.applicationDetails.cfgTblCustomForm?.txtFormName || 'Budget Approval Form';
            this.footerFields = this.buildFooterFields(this.applicationFormData);
          }
          // Parse approval history JSON
          if (data.txtApprovalHistory) {
            try {
              this.approvalHistory = JSON.parse(data.txtApprovalHistory);
              console.log('Approval history loaded:', this.approvalHistory);
            } catch (e) {
              console.error('Error parsing approval history:', e);
              this.approvalHistory = [];
            }
          } else {
            this.approvalHistory = [];
          }

          this.enrichPipelineWithDepartmentNames();
          this.applyDepartmentNamesToApprovalHistory();
          this.buildGenericPages();

          // If formFields is empty (e.g. cfgTblCustomFormFields stripped by backend or forms not loaded yet),
          // fetch the form by ID to ensure CAPF and other form previews have the field structure
          if (this.formFields.length === 0 && data.serFormId) {
            this.customFormService.getById(data.serFormId).subscribe(
              (formData: any) => {
                if (formData && formData.cfgTblCustomFormFields && formData.cfgTblCustomFormFields.length > 0) {
                  this.formFields = formData.cfgTblCustomFormFields
                    .map((field: any) => ({
                      serFieldId: field.serFieldId,
                      label: field.txtFieldLabel,
                      type: field.txtFieldType,
                      required: field.blIsRequired || false,
                      placeholder: field.txtPlaceholder || '',
                      intFieldOrder: field.intFieldOrder || 0,
                      txtFieldOptions: field.txtFieldOptions
                    }))
                    .sort((a: any, b: any) => (a.intFieldOrder || 0) - (b.intFieldOrder || 0));
                  this.buildGenericPages();
                  this.cdr.detectChanges();
                }
              }
            );
          }

          this.isLoading = false;
        } else {
          this.notificationService.showMessage('Application not found', 'danger');
          this.router.navigate(['/applicationsview']);
        }
      },
      (error) => {
        this.isLoading = false;
        this.notificationService.showMessage('Error loading application details: ' + (error.error?.message || error.message), 'danger');
        this.router.navigate(['/applicationsview']);
      }
    );
  }

  private enrichPipelineWithDepartmentNames() {
    if (!this.applicationDetails || !this.applicationDetails.cfgTblCustomForm || this.departmentNameMap.size === 0) {
      return;
    }
    const form = this.applicationDetails.cfgTblCustomForm;

    const applyNameToPipeline = (pipeline: any) => {
      if (!pipeline) return;
      const deptId = pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId || pipeline.departmentId;
      if (!deptId) return;
      const deptName = this.departmentNameMap.get(Number(deptId));
      if (!deptName) return;
      if (pipeline.hrTblDepartment && !pipeline.hrTblDepartment.txtDepartmentName) {
        pipeline.hrTblDepartment.txtDepartmentName = deptName;
      }
      if (!pipeline.departmentName) pipeline.departmentName = deptName;
      if (!pipeline.txtDepartmentName) pipeline.txtDepartmentName = deptName;
    };

    if (Array.isArray(form.cfgTblCustomFormApprovalPipelines)) {
      form.cfgTblCustomFormApprovalPipelines.forEach(applyNameToPipeline);
    }
    if (Array.isArray(form.approvalPipelines)) {
      form.approvalPipelines.forEach(applyNameToPipeline);
    }

    if (form.txtApprovalPipeline) {
      try {
        const pipelines = JSON.parse(form.txtApprovalPipeline);
        if (Array.isArray(pipelines)) {
          pipelines.forEach(applyNameToPipeline);
          form.txtApprovalPipeline = JSON.stringify(pipelines);
        }
      } catch (e) {
        // ignore parse errors
      }
    }
  }

  private applyDepartmentNamesToApprovalHistory() {
    if (!this.applicationDetails || !this.approvalHistory || this.approvalHistory.length === 0) {
      return;
    }

    const map = new Map<number, string>();
    const mapByOrder = new Map<number, { deptId?: number; deptName?: string }>();
    const pipelines = this.getPipelineData();
    pipelines.forEach((pipeline: any) => {
      const deptId = pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId || pipeline.departmentId;
      const deptName =
        pipeline.hrTblDepartment?.txtDepartmentName ||
        pipeline.departmentName ||
        pipeline.txtDepartmentName ||
        (deptId ? this.departmentNameMap.get(Number(deptId)) : undefined);
      if (deptId != null && deptName) {
        map.set(Number(deptId), String(deptName));
      }
      const order = pipeline.intApprovalOrder || undefined;
      if (order != null) {
        mapByOrder.set(Number(order), {
          deptId: deptId != null ? Number(deptId) : undefined,
          deptName: deptName ? String(deptName) : undefined
        });
      }
    });

    let changed = false;
    this.approvalHistory = this.approvalHistory.map((entry: any) => {
      if (!entry) return entry;
      if (entry.departmentName && entry.departmentName.trim() !== '') return entry;

      const deptId = entry.departmentId || entry.serDepartmentId;
      const level = entry.level || entry.intApprovalOrder;
      let name = null as string | null;
      let resolvedDeptId: number | undefined = deptId != null ? Number(deptId) : undefined;

      if (resolvedDeptId != null) {
        name = map.get(resolvedDeptId) || this.departmentNameMap.get(resolvedDeptId) || null;
      }

      if (!name && level != null) {
        const byOrder = mapByOrder.get(Number(level));
        if (byOrder?.deptName) {
          name = byOrder.deptName;
        }
        if (resolvedDeptId == null && byOrder?.deptId != null) {
          resolvedDeptId = byOrder.deptId;
        }
      }

      if (!name) return entry;
      changed = true;
      return {
        ...entry,
        departmentId: resolvedDeptId ?? entry.departmentId,
        departmentName: name,
        role: entry.role || name
      };
    });

    if (changed) {
      try {
        this.applicationDetails.txtApprovalHistory = JSON.stringify(this.approvalHistory);
      } catch {
        // ignore serialize errors
      }
    }
  }

  getFieldName(label: string): string {
    return label.toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  getFieldValue(field: any): any {
    const fieldName = this.getFieldName(field.label);

    if (this.applicationFormData[fieldName] !== undefined) {
      return this.applicationFormData[fieldName];
    } else if (this.applicationFormData[field.label] !== undefined) {
      return this.applicationFormData[field.label];
    } else {
      const fieldId = `field_${field.serFieldId}`;
      if (this.applicationFormData[fieldId] !== undefined) {
        return this.applicationFormData[fieldId];
      }
    }

    return null;
  }

  getFieldOptions(field: any): string[] {
    if (!field.txtFieldOptions) {
      return [];
    }

    try {
      const options = JSON.parse(field.txtFieldOptions);
      if (Array.isArray(options)) {
        return options.filter((opt: any) => opt && opt.trim() !== '');
      }
    } catch (e) {
      if (typeof field.txtFieldOptions === 'string') {
        return field.txtFieldOptions
          .split(',')
          .map((opt: string) => opt.trim())
          .filter((opt: string) => opt !== '');
      }
    }

    return [];
  }

  formatFieldValue(field: any, value: any): string {
    if (value === null || value === undefined || value === '') {
      return '-';
    }

    if (field.type === 'checkbox') {
      return value ? 'Yes' : 'No';
    }

    if (field.type === 'date' && value) {
      try {
        const date = new Date(value);
        return date.toLocaleDateString();
      } catch (e) {
        return value;
      }
    }

    if (field.type === 'radio' || field.type === 'select') {
      return value;
    }

    const normalizedType = (field?.type || '').toString().toLowerCase().replace(/\s+/g, '_');
    if (normalizedType === 'attachment' || normalizedType === 'file') {
      if (typeof value === 'object') {
        return value.fileName || value.name || '-';
      }
      return String(value);
    }

    return value;
  }

  isWordEditorType(fieldType: string | undefined): boolean {
    const normalizedType = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
    return normalizedType === 'word_editor' || normalizedType === 'wordeditor' || normalizedType === 'rich_text' || normalizedType === 'richtext';
  }

  private isWordEditorChunkType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'word_editor_chunk';
  }

  isIndividualPipelineFooterType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'individual_pipeline_footer';
  }

  getIndividualPipelineFooterFields(): any[] {
    const footerFields =
      this.applicationFormData?.footerFields ??
      this.applicationFormData?.individual_pipeline_footer;
    if (!Array.isArray(footerFields)) return [];
    return [...footerFields].sort((a: any, b: any) => (Number(a?.order) || 0) - (Number(b?.order) || 0));
  }

  getIndividualFooterColSpan(section: any): number {
    const slots = this.getIndividualFooterSlots(section);
    return Math.max(slots.length === 1 && slots[0] === null ? 1 : slots.length, 1);
  }

  /**
   * Slots for signature row: one column per person configured on the section (deduped by user id).
   * When some approvers at this level have signed, merge history onto those users so names/signatures
   * stay accurate — but still show every selected co-approver until all have acted (partial approvals
   * used to drop pending people because only history rows were returned).
   */
  getIndividualFooterSlots(section: any): any[] {
    const sections = this.getIndividualPipelineFooterFields();
    const sectionIndex = sections.findIndex((s: any) => s === section);
    let level: number;
    if (Number(section?.order) > 0) {
      level = Number(section.order);
    } else if (sectionIndex >= 0) {
      level = this.isCapfForm() && sectionIndex === 0 ? 0 : sectionIndex + 1;
    } else {
      level = 1;
    }

    const submitterIdRaw =
      this.applicationDetails?.serSubmittedBy ??
      this.applicationDetails?.cfgTblUser?.serUserId ??
      null;
    const submitterIdNum =
      submitterIdRaw != null ? Number(submitterIdRaw) : NaN;

    const baseUsers = this.dedupedSectionUsers(section);
    const hasConfiguredUsers = baseUsers.some((u: any) => u != null);

    if (!this.approvalHistory || this.approvalHistory.length === 0) {
      return baseUsers;
    }

    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const matches = this.approvalHistory.filter((e: any) => {
      const action = (e.action || '').toString().toUpperCase();
      if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;
      const entryLevel = e.level ?? e.intApprovalOrder;
      if (entryLevel == null) return false;
      if (Number(entryLevel) !== level) return false;
      if (Number(entryLevel) > currentLevel) return false;
      return action === 'APPROVED' || !!(e.approvedDate);
    });

    if (matches.length === 0) return baseUsers;

    matches.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    const historyByUserId = new Map<number, any>();
    for (const e of matches) {
      const uid = e.approvedBy ?? e.userId;
      const n = uid != null ? Number(uid) : NaN;
      if (isNaN(n) || historyByUserId.has(n)) continue;
      historyByUserId.set(n, {
        serUserId: n,
        userId: n,
        id: n,
        approvedBy: n,
        txtUserName: e.approverName ?? e.userName,
        userName: e.approverName ?? e.userName,
        name: e.approverName ?? e.userName,
        departmentName: e.departmentName ?? e.userDepartmentName,
        txtDesignation: e.txtDesignation ?? e.designation,
        designation: e.txtDesignation ?? e.designation,
      });
    }

    if (!hasConfiguredUsers && historyByUserId.size > 0) {
      let vals = Array.from(historyByUserId.values());
      if (sectionIndex > 0 && !isNaN(submitterIdNum)) {
        vals = vals.filter((h: any) => {
          const id = h?.serUserId ?? h?.userId ?? h?.id ?? h?.approvedBy;
          return Number(id) !== submitterIdNum;
        });
      }
      return vals.length > 0 ? vals : baseUsers;
    }

    const merged: any[] = [];
    for (const u of baseUsers) {
      if (u == null) {
        merged.push(null);
        continue;
      }
      const uid = u.serUserId ?? u.userId ?? u.id ?? u.approvedBy;
      const n = uid != null ? Number(uid) : NaN;
      const hist = !isNaN(n) ? historyByUserId.get(n) : undefined;
      merged.push(hist ? { ...u, ...hist } : u);
    }

    // Do not append unmatched history-only users as extra columns.
    // UI must follow the configured footer pipeline exactly for general forms.
    // History still overlays configured users above via merge.

    return merged.length > 0 ? merged : baseUsers;
  }

  /** Dedupe section.users by userId (used when no approval history for this stage). */
  private dedupedSectionUsers(section: any): any[] {
    const users = Array.isArray(section?.users) ? section.users : [];
    if (users.length === 0) return [null];
    const seen = new Set<number>();
    const deduped: any[] = [];
    for (const u of users) {
      if (!u) continue;
      const uid = u.serUserId ?? u.userId ?? u.id ?? u.approvedBy;
      const n = uid != null ? Number(uid) : NaN;
      if (!isNaN(n) && seen.has(n)) continue;
      if (!isNaN(n)) seen.add(n);
      deduped.push(u);
    }
    return deduped.length > 0 ? deduped : [null];
  }

  getIndividualFooterUserLabel(user: any, section: any): string {
    if (!user) return '';
    const name = user.txtUserName || user.userName || user.name || '';
    const designation = user.txtDesignation || user.designation || '';
    const department = 
      user.hrTblDepartment?.txtDepartmentName ||
      user.txtDepartmentName ||
      user.departmentName ||
      '';
    const role =
      user.cfgTblRole?.txtRoleName ||
      user.txtRoleName ||
      user.roleName ||
      '';
    
    const designationLine = designation ? `<br>${designation}` : '';
    const departmentLine = department ? `<br>${department}` : '';
    const roleLine = role ? `<br>(${role})` : '';
    
    return `${name}${designationLine}${departmentLine}${roleLine}`;
  }

  isDocumentHeaderType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'document_header';
  }

  isTableType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'table';
  }

  isAttachmentType(fieldType: string | undefined): boolean {
    const t = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
    return t === 'attachment' || t === 'file';
  }

  isMultiAttachmentType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'multi_attachment';
  }

  /** Form body: exclude attachment fields so they only appear in the Attachments section for view/download */
  getBodyPreviewFields(): any[] {
    return (this.getGenericPreviewFields() || []).filter(
      (f: any) =>
        !this.isAttachmentType(f?.type) &&
        !this.isMultiAttachmentType(f?.type) &&
        // Footer fields are rendered once at the end (last page only)
        (String(f?.type || '').toLowerCase().replace(/\s+/g, '_') !== 'footer') &&
        !this.isIndividualPipelineFooterType(f?.type)
    );
  }

  /** Split body fields into pages for generic form (each page ~A4). Header on every page, footer only on last. */
  buildGenericPages(): void {
    const allFields = this.getBodyPreviewFields() || [];
    const pages: any[][] = [];
    const individualLayout = this.useIndividualFooterDocumentLayout();
    const hasFooterOnLastPage = individualLayout && this.hasIndividualPipelineFooter();
    // Approximate A4 by text length (not field count).
    // Keep this closer to `/application` preview chunking so details-page pagination
    // matches what users saw while creating/submitting the same general form.
    const MAX_CHARS_FIRST_PAGE = individualLayout ? 1500 : 2200;
    const MAX_CHARS_OTHER_PAGES = individualLayout ? 1700 : 2400;
    // Keep reserved room on the final page when signature footer is present.
    const MAX_CHARS_LAST_PAGE = hasFooterOnLastPage ? 1200 : MAX_CHARS_OTHER_PAGES;
    let current: any[] = [];
    let currentChars = 0;
    let pageIndex = 0;
    for (const rawField of allFields) {
      const expandedFields = this.expandWordEditorFieldIntoChunks(rawField);
      for (const field of expandedFields) {
        const queue: any[] = [field];
        while (queue.length > 0) {
          const part = queue.shift();
          if (!part) continue;
          const fieldText = this.getFieldTextLength(part);
          const pageLimit = pageIndex === 0 ? MAX_CHARS_FIRST_PAGE : MAX_CHARS_OTHER_PAGES;
          const remaining = pageLimit - currentChars;
          const isSplittable =
            this.isWordEditorType(part?.type) ||
            this.isWordEditorChunkType(part?.type) ||
            this.isHtmlPreviewField(part);

          // Prefer splitting rich text to fill remaining space (even mid-sentence),
          // instead of moving entire paragraph/chunk to next page.
          if (isSplittable && fieldText > remaining && remaining > 80) {
            const split = this.splitFieldByTextLength(part, remaining);
            if (split) {
              current.push(split.head);
              currentChars += this.getFieldTextLength(split.head);
              pages.push([...current]);
              current = [];
              currentChars = 0;
              pageIndex++;
              queue.unshift(split.tail);
              continue;
            }
          }

          // If this part still doesn't fit and page already has content, start next page.
          if (current.length > 0 && currentChars + fieldText > pageLimit) {
            pages.push([...current]);
            current = [];
            currentChars = 0;
            pageIndex++;
            queue.unshift(part);
            continue;
          }

          // If single rich chunk is larger than empty page, force-split by page limit.
          if (isSplittable && current.length === 0 && fieldText > pageLimit) {
            const split = this.splitFieldByTextLength(part, pageLimit);
            if (split) {
              current.push(split.head);
              currentChars += this.getFieldTextLength(split.head);
              pages.push([...current]);
              current = [];
              currentChars = 0;
              pageIndex++;
              queue.unshift(split.tail);
              continue;
            }
          }

          current.push(part);
          currentChars += fieldText;
        }
      }
    }
    if (current.length) pages.push([...current]);
    if (pages.length > 0 && hasFooterOnLastPage) {
      this.enforceGenericLastPageLimit(pages, MAX_CHARS_LAST_PAGE);
    }
    this.genericPages = pages.length > 0 ? pages : (allFields.length > 0 ? [[...allFields]] : [[]]);
    if (this.shouldUseMeasuredGenericPagination()) {
      this.detailsGenericMeasureBlocks = this.buildDetailsGenericBlocks();
    } else {
      this.detailsGenericMeasureBlocks = [];
    }
    this.cdr.markForCheck();
  }

  /** Same condition as /application paginated generic preview: individual pipeline footer on a general form. */
  shouldUseMeasuredGenericPagination(): boolean {
    return !this.isBudgetApprovalForm() && !this.isCapfForm() && this.hasIndividualPipelineFooter();
  }

  getGenericMeasurePagesForDisplay(): DetailsGenericBlock[][] {
    if (!this.shouldUseMeasuredGenericPagination()) {
      return [];
    }
    if (this.genericMeasurePages.length > 0) {
      return this.genericMeasurePages;
    }
    const blocks = this.detailsGenericMeasureBlocks.length
      ? this.detailsGenericMeasureBlocks
      : this.buildDetailsGenericBlocks();
    return blocks.length ? [blocks] : [[]];
  }

  getDetailsBlockWordHtml(block: DetailsGenericBlock): SafeHtml {
    if (block.wordEditorChunkHtml !== undefined) {
      return this.sanitizer.bypassSecurityTrustHtml(block.wordEditorChunkHtml);
    }
    return this.getWordEditorValue(block.field);
  }

  trackByDetailsBlockKey(_index: number, block: DetailsGenericBlock): string {
    return block.key;
  }

  private isDetailsFormNameLabel(label: string | undefined): boolean {
    const normalizedLabel = (label || '').trim().toLowerCase();
    const normalizedFormName = (
      this.applicationDetails?.cfgTblCustomForm?.txtFormName ||
      this.applicationDetails?.formName ||
      ''
    ).trim()
      .toLowerCase();
    return !!normalizedLabel && !!normalizedFormName && normalizedLabel === normalizedFormName;
  }

  private buildDetailsGenericBlocks(): DetailsGenericBlock[] {
    const fields = this.getBodyPreviewFields() || [];
    const blocks: DetailsGenericBlock[] = [];
    fields.forEach((field: any, index: number) => {
      if (this.isDocumentHeaderType(field?.type)) {
        return;
      }
      if (!this.isWordEditorType(field.type) && !this.isHtmlPreviewField(field)) {
        blocks.push({
          key: `f_${index}_${this.getFieldName(field.label)}`,
          field,
          showLabel: !this.isDetailsFormNameLabel(field.label)
        });
        return;
      }
      const rawHtml = this.getWordEditorHtml(field);
      const normalizedHtml =
        rawHtml === null || rawHtml === undefined || rawHtml === ''
          ? '<p>-</p>'
          : this.normalizeWordEditorHtmlForDisplay(String(rawHtml));
      const chunks = this.splitDetailsWordEditorHtmlIntoChunks(normalizedHtml);
      chunks.forEach((chunkHtml: string, chunkIndex: number) => {
        blocks.push({
          key: `f_${index}_${this.getFieldName(field.label)}_w_${chunkIndex}`,
          field,
          showLabel: chunkIndex === 0 && !this.isDetailsFormNameLabel(field.label),
          wordEditorChunkHtml: chunkHtml
        });
      });
    });
    return blocks;
  }

  private splitDetailsWordEditorHtmlIntoChunks(html: string, maxChunkChars: number = 2200): string[] {
    if (!html) {
      return ['<p>-</p>'];
    }
    const wrapper = document.createElement('div');
    wrapper.innerHTML = html;
    const nodes = Array.from(wrapper.childNodes).filter((node: ChildNode) => {
      if (node.nodeType === Node.TEXT_NODE) {
        return !!(node.textContent || '').trim();
      }
      return true;
    });
    if (nodes.length <= 1) {
      return [html];
    }
    const chunks: string[] = [];
    let current = '';
    nodes.forEach((node: ChildNode) => {
      const serialized =
        node.nodeType === Node.ELEMENT_NODE
          ? (node as HTMLElement).outerHTML
          : `<p>${this.escapeHtml(node.textContent || '')}</p>`;
      if (!current) {
        current = serialized;
        return;
      }
      if (current.length + serialized.length > maxChunkChars) {
        chunks.push(current);
        current = serialized;
      } else {
        current += serialized;
      }
    });
    if (current) {
      chunks.push(current);
    }
    return chunks.length > 0 ? chunks : [html];
  }

  private mmToPx(mm: number): number {
    return (mm * 96) / 25.4;
  }

  private buildDetailsMeasureSignature(blocks: DetailsGenericBlock[]): string {
    const fields = this.getBodyPreviewFields() || [];
    const fieldValues = fields.map((f: any) => {
      const key = this.getFieldName(f.label);
      const v = this.applicationFormData?.[key];
      if (v === null || v === undefined) {
        return `${key}:`;
      }
      if (typeof v === 'string') {
        return `${key}:${v.length}:${v}`;
      }
      if (typeof v === 'number' || typeof v === 'boolean') {
        return `${key}:${String(v)}`;
      }
      try {
        return `${key}:${JSON.stringify(v)}`;
      } catch {
        return `${key}:${String(v)}`;
      }
    });
    const footerSig = JSON.stringify(this.getIndividualPipelineFooterFields() || []);
    return [
      String(this.applicationDetails?.serFormId || ''),
      String(this.applicationId || ''),
      this.getGenericDocumentHeading(),
      fieldValues.join('|'),
      String(blocks.length),
      footerSig
    ].join('::');
  }

  private rebuildGenericMeasurePagesIfNeeded(): void {
    if (!this.shouldUseMeasuredGenericPagination()) {
      this.genericMeasureLayoutSignature = '';
      this.genericMeasurePages = [];
      return;
    }

    const blocks = this.buildDetailsGenericBlocks();
    this.detailsGenericMeasureBlocks = blocks;

    const paperEl = this.genericMeasurePaper?.nativeElement;
    const headerEl = this.genericMeasureHeader?.nativeElement;
    const contentEl = this.genericMeasureContent?.nativeElement;
    const footerEl = this.genericMeasureFooter?.nativeElement;
    const signature = this.buildDetailsMeasureSignature(blocks);

    if (
      signature === this.genericMeasureLayoutSignature &&
      this.genericMeasurePages.length > 0 &&
      paperEl &&
      headerEl &&
      contentEl &&
      footerEl
    ) {
      return;
    }

    if (!paperEl || !headerEl || !contentEl || !footerEl) {
      this.genericMeasurePages = [blocks];
      this.genericMeasureLayoutSignature = signature;
      this.cdr.markForCheck();
      return;
    }

    const styles = window.getComputedStyle(paperEl);
    const minHeightPx = parseFloat(styles.minHeight || '0') || this.mmToPx(297);
    const paddingTopPx = parseFloat(styles.paddingTop || '0') || 0;
    const paddingBottomPx = parseFloat(styles.paddingBottom || '0') || 0;
    const pageContentHeight = Math.max(minHeightPx - paddingTopPx - paddingBottomPx, 200);

    const headerHeight = Math.max(headerEl.getBoundingClientRect().height, 0);
    const footerHeight = Math.max(footerEl.getBoundingClientRect().height, 0);

    // Slight pessimism so a block is not placed on a page when its measured height is a few
    // subpixels short — that used to clip the last line on page 1 and show the same line again
    // at the top of page 2 (next chunk).
    const layoutFudgePx = 4;
    const firstPageContentHeight = Math.max(
      pageContentHeight - headerHeight - layoutFudgePx,
      pageContentHeight * 0.3
    );
    const middlePageContentHeight = Math.max(pageContentHeight - layoutFudgePx, 200);
    const lastPageContentHeight = Math.max(
      pageContentHeight - footerHeight - layoutFudgePx,
      pageContentHeight * 0.3
    );

    const measureBlocks = Array.from(contentEl.querySelectorAll('.xyz-measure-field-block')) as HTMLElement[];
    if (!measureBlocks.length || measureBlocks.length !== blocks.length) {
      this.genericMeasurePages = [blocks];
      this.genericMeasureLayoutSignature = signature;
      this.cdr.markForCheck();
      return;
    }

    const blockHeights = measureBlocks.map((blockEl: HTMLElement) => {
      const blockStyle = window.getComputedStyle(blockEl);
      const marginTop = parseFloat(blockStyle.marginTop || '0') || 0;
      const marginBottom = parseFloat(blockStyle.marginBottom || '0') || 0;
      const h = blockEl.getBoundingClientRect().height + marginTop + marginBottom;
      return Math.max(Math.ceil(h) + 1, 1);
    });

    const paged = this.chunkDetailsBlocksIntoPages(
      blocks,
      blockHeights,
      firstPageContentHeight,
      middlePageContentHeight,
      lastPageContentHeight
    );

    this.genericMeasurePages = paged;
    this.genericMeasureLayoutSignature = signature;
    this.cdr.markForCheck();
  }

  private chunkDetailsBlocksIntoPages(
    blocks: DetailsGenericBlock[],
    blockHeights: number[],
    firstPageCapacity: number,
    middlePageCapacity: number,
    lastPageCapacity: number
  ): DetailsGenericBlock[][] {
    if (!blocks.length || blocks.length !== blockHeights.length) {
      return [blocks];
    }

    const pages: number[][] = [[]];
    const pageHeights: number[] = [0];
    let currentPageIndex = 0;

    const getRegularPageCapacity = (pageIndex: number): number =>
      pageIndex === 0 ? firstPageCapacity : middlePageCapacity;

    blocks.forEach((_: DetailsGenericBlock, fieldIndex: number) => {
      const blockHeight = blockHeights[fieldIndex];
      const pageCapacity = getRegularPageCapacity(currentPageIndex);
      const nextHeight = pageHeights[currentPageIndex] + blockHeight;
      if (pages[currentPageIndex].length > 0 && nextHeight > pageCapacity) {
        pages.push([]);
        pageHeights.push(0);
        currentPageIndex += 1;
      }
      pages[currentPageIndex].push(fieldIndex);
      pageHeights[currentPageIndex] += blockHeight;
    });

    const footerReserve = Math.max(middlePageCapacity - lastPageCapacity, 0);
    const getLastPageAllowedHeight = (): number =>
      pages.length === 1
        ? Math.max(firstPageCapacity - footerReserve, firstPageCapacity * 0.25)
        : lastPageCapacity;

    let safetyCounter = 0;
    while (safetyCounter < blocks.length * 2) {
      const lastPageIndex = pages.length - 1;
      const allowedHeight = getLastPageAllowedHeight();
      if (pageHeights[lastPageIndex] <= allowedHeight) {
        break;
      }

      if (pages[lastPageIndex].length <= 1) {
        break;
      }

      const movedToNextPage: number[] = [];
      while (pageHeights[lastPageIndex] > allowedHeight && pages[lastPageIndex].length > 1) {
        const movedFieldIndex = pages[lastPageIndex].pop() as number;
        movedToNextPage.unshift(movedFieldIndex);
        pageHeights[lastPageIndex] -= blockHeights[movedFieldIndex];
      }

      const movedHeight = movedToNextPage.reduce((sum: number, idx: number) => sum + blockHeights[idx], 0);
      pages.push(movedToNextPage);
      pageHeights.push(movedHeight);
      safetyCounter += 1;
    }

    return pages.map((pageFieldIndices: number[]) => pageFieldIndices.map((idx: number) => blocks[idx]));
  }

  private splitFieldByTextLength(field: any, maxTextChars: number): { head: any; tail: any } | null {
    if (!field || maxTextChars <= 0) return null;
    const html = this.getWordEditorHtml(field);
    if (!html) return null;
    const split = this.splitHtmlAtTextLength(html, maxTextChars);
    if (!split) return null;

    const head = {
      ...field,
      type: 'word_editor_chunk',
      _chunkHtml: split.headHtml
    };
    const tail = {
      ...field,
      type: 'word_editor_chunk',
      _chunkHtml: split.tailHtml,
      _hideLabel: true
    };
    if (this.getFieldTextLength(head) === 0 || this.getFieldTextLength(tail) === 0) return null;
    return { head, tail };
  }

  private splitHtmlAtTextLength(html: string, maxTextChars: number): { headHtml: string; tailHtml: string } | null {
    const normalized = this.normalizeWordEditorHtmlForDisplay(String(html || ''));
    if (!normalized) return null;

    const wrapper = document.createElement('div');
    wrapper.innerHTML = normalized;

    const container =
      wrapper.children.length === 1 && wrapper.firstElementChild
        ? (wrapper.firstElementChild as HTMLElement)
        : wrapper;

    const styleNodes = Array.from(container.querySelectorAll('style'));
    const preservedStyleHtml = styleNodes.map((s) => s.outerHTML).join('');
    styleNodes.forEach((s) => s.remove());

    const textNodes = this.collectTextNodes(container);
    if (textNodes.length === 0) return null;
    const totalLen = textNodes.reduce((sum: number, n: Text) => sum + ((n.nodeValue || '').length), 0);
    if (maxTextChars <= 0 || maxTextChars >= totalLen) return null;

    const splitPos = this.locateTextPosition(textNodes, maxTextChars);
    if (!splitPos) return null;

    const headRange = document.createRange();
    headRange.setStart(container, 0);
    headRange.setEnd(splitPos.node, splitPos.offset);

    const tailRange = document.createRange();
    tailRange.setStart(splitPos.node, splitPos.offset);
    tailRange.setEnd(container, container.childNodes.length);

    const toHtml = (frag: DocumentFragment): string => {
      const div = document.createElement('div');
      div.appendChild(frag);
      return div.innerHTML;
    };

    const headInner = toHtml(headRange.cloneContents()).trim();
    const tailInner = toHtml(tailRange.cloneContents()).trim();
    if (!headInner || !tailInner) return null;

    const wrapperTag = container !== wrapper ? container.tagName.toLowerCase() : '';
    const wrapperAttr = container !== wrapper ? this.serializeElementAttributes(container) : '';
    const wrapOpen = wrapperTag ? `<${wrapperTag}${wrapperAttr}>` : '';
    const wrapClose = wrapperTag ? `</${wrapperTag}>` : '';

    return {
      headHtml: `${preservedStyleHtml}${wrapOpen}${headInner}${wrapClose}`,
      tailHtml: `${preservedStyleHtml}${wrapOpen}${tailInner}${wrapClose}`
    };
  }

  private stripHtmlToText(html: string): string {
    if (!html) return '';
    return html
      .replace(/<style[\s\S]*?<\/style>/gi, ' ')
      .replace(/<script[\s\S]*?<\/script>/gi, ' ')
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/p>/gi, '\n')
      .replace(/<[^>]+>/g, ' ')
      .replace(/&nbsp;/gi, ' ')
      .replace(/&amp;/gi, '&')
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>')
      .replace(/\s+/g, ' ')
      .trim();
  }

  private getFieldTextLength(field: any): number {
    if (!field) return 0;
    const labelLen = String(field.label || '').trim().length;
    try {
      if (this.isWordEditorType(field.type) || this.isWordEditorChunkType(field.type) || this.isHtmlPreviewField(field)) {
        const html = String(this.getWordEditorHtml(field) || '');
        const textLen = this.stripHtmlToText(html).length;
        const brCount = (html.match(/<br\s*\/?>/gi) || []).length;
        const pCount = (html.match(/<\/p>/gi) || []).length;
        const liCount = (html.match(/<li\b/gi) || []).length;
        const trCount = (html.match(/<tr\b/gi) || []).length;
        const imgCount = (html.match(/<img\b/gi) || []).length;
        // Heuristic weight for visual height so long, sparse HTML paginates properly.
        // Individual footer pipeline content often has many manual line breaks with
        // little plain text; give structural breaks stronger weight.
        const extra =
          brCount * 42 +
          pCount * 54 +
          liCount * 42 +
          trCount * 80 +
          imgCount * 180;
        return labelLen + textLen + extra;
      }
      if (this.isTableType(field.type)) {
        const rawValue = this.getFieldValue(field);
        if (Array.isArray(rawValue)) {
          const rowCount = rawValue.length;
          return labelLen + rowCount * 90;
        }
      }
      const rawValue = this.getFieldValue(field);
      const formatted = this.formatFieldValue(field, rawValue);
      return labelLen + String(formatted ?? '').length;
    } catch {
      const rawValue = this.getFieldValue(field);
      return labelLen + String(rawValue ?? '').length;
    }
  }

  private getWordEditorHtml(field: any): string {
    if (field && typeof field === 'object' && typeof field._chunkHtml === 'string') {
      return field._chunkHtml;
    }
    const value = this.getFieldValue(field);
    const decoded = this.decodeHtmlEntitiesIfNeeded(String(value ?? ''));
    return this.normalizeWordEditorHtmlForDisplay(decoded);
  }

  getWordEditorValue(field: any): SafeHtml {
    const html = this.getWordEditorHtml(field);
    if (!html) {
      return this.sanitizer.bypassSecurityTrustHtml('<span>-</span>');
    }
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private expandWordEditorFieldIntoChunks(field: any): any[] {
    if (!field || !(this.isWordEditorType(field.type) || this.isHtmlPreviewField(field))) return [field];
    const html = this.getWordEditorHtml(field);
    if (!html) return [field];

    const wrapper = document.createElement('div');
    wrapper.innerHTML = html;

    // Preserve top-level wrapper styles/classes if the editor content is wrapped
    // in a single container element.
    const container =
      wrapper.children.length === 1 && wrapper.firstElementChild
        ? (wrapper.firstElementChild as HTMLElement)
        : wrapper;

    const styleNodes = Array.from(container.querySelectorAll('style'));
    const preservedStyleHtml = styleNodes.map(s => s.outerHTML).join('');
    styleNodes.forEach(s => s.remove());

    const wrapperTag = container !== wrapper ? container.tagName.toLowerCase() : '';
    const wrapperAttr = container !== wrapper ? this.serializeElementAttributes(container) : '';
    const wrapOpen = wrapperTag ? `<${wrapperTag}${wrapperAttr}>` : '';
    const wrapClose = wrapperTag ? `</${wrapperTag}>` : '';

    const blocks = Array.from(container.childNodes).filter(n => {
      if (n.nodeType === Node.TEXT_NODE) return (n.textContent || '').trim().length > 0;
      if (n.nodeType !== Node.ELEMENT_NODE) return false;
      const tag = (n as Element).tagName.toLowerCase();
      return tag === 'p' || tag === 'div' || tag === 'table' || tag === 'ul' || tag === 'ol' || tag.startsWith('h');
    });

    const MAX_CHARS_PER_CHUNK = 4200;
    const chunks: string[] = [];
    let currentHtml = '';
    let currentChars = 0;

    for (const node of blocks) {
      const nodeChunks = this.splitNodeToChunkHtml(node, MAX_CHARS_PER_CHUNK);
      for (const nodeHtml of nodeChunks) {
        const nodeTextLen = this.stripHtmlToText(nodeHtml).length;
        if (currentChars > 0 && currentChars + nodeTextLen > MAX_CHARS_PER_CHUNK) {
          chunks.push(currentHtml);
          currentHtml = '';
          currentChars = 0;
        }
        currentHtml += nodeHtml;
        currentChars += nodeTextLen;
      }
    }

    if (currentChars > 0) chunks.push(currentHtml);
    if (chunks.length <= 1) return [field];

    return chunks.map((chunkHtml, idx) => ({
      ...field,
      type: 'word_editor_chunk',
      // Keep original wrapper styling + embedded styles on each chunk,
      // so formatting doesn't disappear after pagination.
      _chunkHtml: `${preservedStyleHtml}${wrapOpen}${chunkHtml}${wrapClose}`,
      _hideLabel: idx > 0
    }));
  }

  private serializeElementAttributes(el: HTMLElement): string {
    if (!el || !el.attributes) return '';
    const parts: string[] = [];
    for (let i = 0; i < el.attributes.length; i++) {
      const attr = el.attributes.item(i);
      if (!attr) continue;
      // Skip id to avoid duplicate ids across chunks/pages
      if (attr.name.toLowerCase() === 'id') continue;
      parts.push(` ${attr.name}="${this.escapeHtml(attr.value)}"`);
    }
    return parts.join('');
  }

  /** Split budget approval HTML into pages by approximate text length. */
  buildBudgetPages(contentHtml: string): void {
    const raw = String(contentHtml || '').trim();
    if (!raw) {
      this.budgetPages = [this.sanitizer.bypassSecurityTrustHtml('<i>(No content found in application data)</i>')];
      return;
    }

    // Budget forms are mostly rich HTML; preserve styling by chunking HTML blocks
    // (do not convert to plain text + <br>, which destroys formatting).
    const MAX_CHARS_PER_PAGE = 2400;
    const chunks = this.splitHtmlIntoChunksPreserveWrapper(raw, MAX_CHARS_PER_PAGE);
    const nonEmpty = chunks.map(c => c.trim()).filter(Boolean);
    this.budgetPages = (nonEmpty.length ? nonEmpty : [raw]).map(h => this.sanitizer.bypassSecurityTrustHtml(h));
  }

  private splitHtmlIntoChunksPreserveWrapper(html: string, maxChars: number): string[] {
    const normalized = this.normalizeWordEditorHtmlForDisplay(String(html || ''));
    const wrapper = document.createElement('div');
    wrapper.innerHTML = normalized;

    // If content is wrapped in a single container, preserve it (classes/styles)
    const container =
      wrapper.children.length === 1 && wrapper.firstElementChild
        ? (wrapper.firstElementChild as HTMLElement)
        : wrapper;

    const styleNodes = Array.from(container.querySelectorAll('style'));
    const preservedStyleHtml = styleNodes.map(s => s.outerHTML).join('');
    styleNodes.forEach(s => s.remove());

    const wrapperTag = container !== wrapper ? container.tagName.toLowerCase() : '';
    const wrapperAttr = container !== wrapper ? this.serializeElementAttributes(container) : '';
    const wrapOpen = wrapperTag ? `<${wrapperTag}${wrapperAttr}>` : '';
    const wrapClose = wrapperTag ? `</${wrapperTag}>` : '';

    const blocks = Array.from(container.childNodes).filter(n => {
      if (n.nodeType === Node.TEXT_NODE) return (n.textContent || '').trim().length > 0;
      if (n.nodeType !== Node.ELEMENT_NODE) return false;
      const tag = (n as Element).tagName.toLowerCase();
      return tag === 'p' || tag === 'div' || tag === 'table' || tag === 'ul' || tag === 'ol' || tag.startsWith('h');
    });

    // If we can't split meaningfully, return as-is (but keep styles)
    if (blocks.length <= 1) {
      return [`${preservedStyleHtml}${wrapOpen}${container.innerHTML}${wrapClose}`];
    }

    const chunks: string[] = [];
    let currentHtml = '';
    let currentChars = 0;

    for (const node of blocks) {
      const nodeHtml =
        node.nodeType === Node.TEXT_NODE
          ? `<p>${this.escapeHtml((node.textContent || '').trim())}</p>`
          : (node as Element).outerHTML;
      const nodeTextLen = this.stripHtmlToText(nodeHtml).length;

      if (currentChars > 0 && currentChars + nodeTextLen > maxChars) {
        chunks.push(`${preservedStyleHtml}${wrapOpen}${currentHtml}${wrapClose}`);
        currentHtml = '';
        currentChars = 0;
      }

      currentHtml += nodeHtml;
      currentChars += nodeTextLen;
    }

    if (currentChars > 0) {
      chunks.push(`${preservedStyleHtml}${wrapOpen}${currentHtml}${wrapClose}`);
    }

    return chunks;
  }

  private escapeHtml(value: string): string {
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private decodeHtmlEntitiesIfNeeded(value: string): string {
    const raw = String(value || '');
    if (!raw) return '';
    // Decode only when it clearly looks HTML-encoded to avoid altering normal text.
    if (!(raw.includes('&lt;') && raw.includes('&gt;'))) return raw;
    const textarea = document.createElement('textarea');
    textarea.innerHTML = raw;
    return textarea.value || raw;
  }

  isHtmlPreviewField(field: any): boolean {
    if (!field) return false;
    if (this.isWordEditorType(field?.type) || this.isWordEditorChunkType(field?.type)) return true;
    if (this.isTableType(field?.type)) return false;
    const v = this.getFieldValue(field);
    if (typeof v !== 'string') return false;
    const value = this.decodeHtmlEntitiesIfNeeded(v).trim();
    if (!value) return false;
    // Consider as rich HTML if at least one common block/inline tag is present.
    return /<(p|div|span|a|strong|em|ul|ol|li|h[1-6]|table|tr|td|th|br)\b[\s\S]*?>/i.test(value);
  }

  private splitNodeToChunkHtml(node: ChildNode, maxChars: number): string[] {
    const nodeHtml =
      node.nodeType === Node.TEXT_NODE
        ? `<p>${this.escapeHtml((node.textContent || '').trim())}</p>`
        : (node as Element).outerHTML;
    const nodeTextLen = this.stripHtmlToText(nodeHtml).length;
    if (nodeTextLen <= maxChars) return [nodeHtml];

    if (node.nodeType !== Node.ELEMENT_NODE) {
      return this.splitTextIntoParagraphChunks(this.stripHtmlToText(nodeHtml), maxChars)
        .map((piece: string) => `<p>${this.escapeHtml(piece)}</p>`);
    }

    const el = node as Element;
    const tag = el.tagName.toLowerCase();
    // Keep complex structural nodes unsplit to avoid malformed structure.
    if (tag === 'table' || tag === 'ul' || tag === 'ol') return [nodeHtml];

    return this.splitElementPreservingMarkup(el, maxChars);
  }

  private splitTextIntoParagraphChunks(text: string, maxChars: number): string[] {
    const normalized = (text || '').replace(/\r\n/g, '\n').trim();
    if (!normalized) return [];
    if (normalized.length <= maxChars) return [normalized];

    const words = normalized.split(/\s+/).filter(Boolean);
    const out: string[] = [];
    let current = '';
    for (const w of words) {
      const candidate = current ? `${current} ${w}` : w;
      if (current && candidate.length > maxChars) {
        out.push(current);
        current = w;
      } else {
        current = candidate;
      }
    }
    if (current) out.push(current);
    return out.length > 0 ? out : [normalized];
  }

  private splitElementPreservingMarkup(el: Element, maxChars: number): string[] {
    const totalTextLen = (el.textContent || '').length;
    if (totalTextLen <= maxChars) return [el.outerHTML];

    const textNodes = this.collectTextNodes(el);
    if (textNodes.length === 0) return [el.outerHTML];

    const chunks: string[] = [];
    let startIndex = 0;

    while (startIndex < totalTextLen) {
      const endIndex = Math.min(startIndex + maxChars, totalTextLen);
      const startPos = this.locateTextPosition(textNodes, startIndex);
      const endPos = this.locateTextPosition(textNodes, endIndex);
      if (!startPos || !endPos) break;

      const range = document.createRange();
      range.setStart(startPos.node, startPos.offset);
      range.setEnd(endPos.node, endPos.offset);

      const fragment = range.cloneContents();
      const wrapper = document.createElement(el.tagName.toLowerCase());
      for (let i = 0; i < el.attributes.length; i++) {
        const attr = el.attributes.item(i);
        if (attr) wrapper.setAttribute(attr.name, attr.value);
      }
      wrapper.appendChild(fragment);
      const html = wrapper.outerHTML;
      if (html && this.stripHtmlToText(html).trim().length > 0) {
        chunks.push(html);
      }

      startIndex = endIndex;
    }

    return chunks.length > 0 ? chunks : [el.outerHTML];
  }

  private collectTextNodes(root: Node): Text[] {
    const out: Text[] = [];
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let current = walker.nextNode();
    while (current) {
      const t = current as Text;
      if ((t.nodeValue || '').length > 0) out.push(t);
      current = walker.nextNode();
    }
    return out;
  }

  private locateTextPosition(nodes: Text[], absoluteIndex: number): { node: Text; offset: number } | null {
    if (!nodes || nodes.length === 0) return null;
    if (absoluteIndex <= 0) return { node: nodes[0], offset: 0 };

    let remaining = absoluteIndex;
    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i];
      const len = (node.nodeValue || '').length;
      if (remaining <= len) {
        return { node, offset: remaining };
      }
      remaining -= len;
    }
    const last = nodes[nodes.length - 1];
    return { node: last, offset: (last.nodeValue || '').length };
  }

  private enforceGenericLastPageLimit(pages: any[][], maxLastChars: number): void {
    if (!pages || pages.length === 0) return;
    const last = pages[pages.length - 1];
    if (!last || last.length === 0) return;
    const currentLastChars = last.reduce((sum: number, f: any) => sum + this.getFieldTextLength(f), 0);
    if (currentLastChars <= maxLastChars) return;

    const head: any[] = [];
    const tail: any[] = [];
    let tailChars = 0;
    for (let i = last.length - 1; i >= 0; i--) {
      const field = last[i];
      const len = this.getFieldTextLength(field);
      if (tail.length === 0 || tailChars + len <= maxLastChars) {
        tail.unshift(field);
        tailChars += len;
      } else {
        head.unshift(field);
      }
    }
    if (head.length > 0) {
      pages.pop();
      pages.push(head);
      pages.push(tail);
    }
  }

  useIndividualFooterDocumentLayout(): boolean {
    const hasHeaderField = (this.formFields || []).some((field: any) => this.isDocumentHeaderType(field?.type));
    const hasWordEditorField = this.getBodyPreviewFields().some((field: any) => this.isWordEditorType(field?.type));
    return this.hasIndividualPipelineFooter() && hasHeaderField && hasWordEditorField;
  }

  getGenericPreviewFields(): any[] {
    const fields = (this.formFields || []).filter((field: any) => !this.isDocumentHeaderType(field?.type));
    if (fields.length > 0) {
      return fields;
    }

    if (!this.applicationFormData || typeof this.applicationFormData !== 'object') {
      return [];
    }

    const excludedKeys = new Set([
      'preparedBy',
      'reviewers',
      'recommenders',
      'approver',
      'heading',
      'header',
      'content',
      'editorContent',
      'date',
      'footerFields',
      'footerfields'
    ]);

    return Object.keys(this.applicationFormData)
      .filter((key: string) => !excludedKeys.has(key))
      .map((key: string) => {
        const value = this.applicationFormData[key];
        const normalizedKey = this.getFieldName(key);
        let type = 'text';
        if (normalizedKey === 'individual_pipeline_footer') {
          type = 'individual_pipeline_footer';
        } else if (normalizedKey === 'footer') {
          type = 'footer';
        } else if (Array.isArray(value) && value.length > 0 && Array.isArray(value[0])) {
          type = 'table';
        } else if (typeof value === 'boolean') {
          type = 'checkbox';
        } else if (typeof value === 'string' && value.includes('<') && value.includes('>')) {
          type = 'word_editor';
        } else if (Array.isArray(value) && value.length > 0 && value.every((v: any) => typeof v === 'object' && v && (v.fileName || v.base64 || v.dataUrl))) {
          type = 'multi_attachment';
        } else if (typeof value === 'object' && value && (value.fileName || value.mimeType || value.dataUrl || value.base64)) {
          type = 'attachment';
        }

        const label = key
          .replace(/_/g, ' ')
          .replace(/\b\w/g, (m: string) => m.toUpperCase());

        return {
          serFieldId: undefined,
          label,
          type,
          required: false,
          placeholder: '',
          intFieldOrder: 0,
          txtFieldOptions: null
        };
      });
  }

  getGenericDocumentHeading(): string {
    const headerField = (this.formFields || []).find((field: any) => this.isDocumentHeaderType(field?.type));
    if (headerField) {
      const value = this.getFieldValue(headerField);
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        return String(value).trim();
      }
      return headerField.label || this.applicationDetails?.cfgTblCustomForm?.txtFormName || 'Application Form';
    }
    return this.applicationDetails?.cfgTblCustomForm?.txtFormName || this.applicationDetails?.formName || 'Application Form';
  }

  getHeaderFieldValue(): string {
    const headerField = (this.formFields || []).find((field: any) => this.isDocumentHeaderType(field?.type));
    if (headerField) {
      const value = this.getFieldValue(headerField);
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        return String(value).trim();
      }
    }
    // Fallback to form name if header field is not found or empty
    return this.applicationDetails?.cfgTblCustomForm?.txtFormName || 'Custom Form';
  }

  private normalizeWordEditorHtmlForDisplay(html: string): string {
    if (!html) return '';
    const wrapper = document.createElement('div');
    wrapper.innerHTML = html;

    wrapper.querySelectorAll('textarea').forEach((node: HTMLTextAreaElement) => {
      const replacement = document.createElement('div');
      const raw = node.value || node.textContent || '';
      const cleaned = raw.replace(/\r\n/g, '\n').trim();
      replacement.style.whiteSpace = 'normal';
      replacement.style.margin = '0';
      replacement.style.padding = '0';
      replacement.textContent = cleaned;
      node.replaceWith(replacement);
    });

    wrapper.querySelectorAll('td,th').forEach((cell: Element) => {
      const el = cell as HTMLElement;
      el.style.minHeight = '32px';
      el.style.padding = '6px 6px';
      el.style.lineHeight = '1.35';
      el.style.verticalAlign = 'middle';

      while (el.firstChild && el.firstChild.nodeType === Node.TEXT_NODE && !(el.firstChild.textContent || '').trim()) {
        el.removeChild(el.firstChild);
      }
      while (el.lastChild && el.lastChild.nodeType === Node.TEXT_NODE && !(el.lastChild.textContent || '').trim()) {
        el.removeChild(el.lastChild);
      }
      while (el.firstElementChild && el.firstElementChild.tagName === 'BR') {
        el.removeChild(el.firstElementChild);
      }
      while (el.lastElementChild && el.lastElementChild.tagName === 'BR') {
        el.removeChild(el.lastElementChild);
      }

      const plainText = (el.textContent || '').replace(/\u00a0/g, '').trim();
      const hasMedia = !!el.querySelector('img,svg,canvas');
      if (!plainText && !hasMedia && el.children.length === 0) {
        el.innerHTML = '<span style="display:block;min-height:1.35em;line-height:1.35;">&nbsp;</span>';
      }
    });

    wrapper.querySelectorAll('tr').forEach((row: Element) => {
      const rowEl = row as HTMLElement;
      rowEl.style.height = '30px';
      rowEl.style.minHeight = '30px';
    });

    return wrapper.innerHTML;
  }

  getTableConfig(field: any): { rows: number; columns: number; rowLabels: string[] } {
    if (field.txtFieldOptions) {
      try {
        const config = JSON.parse(field.txtFieldOptions);
        return {
          rows: config.rows || 2,
          columns: config.columns || 2,
          rowLabels: Array.isArray(config.rowLabels) ? config.rowLabels : []
        };
      } catch (e) {
        // If parsing fails, return defaults
      }
    }
    return { rows: 2, columns: 2, rowLabels: [] };
  }

  getTableData(field: any): any[][] {
    const fieldName = this.getFieldName(field.label);
    const value = this.getFieldValue(field);

    if (value && Array.isArray(value)) {
      return value;
    }

    // Return empty table if no data
    const config = this.getTableConfig(field);
    return Array.from({ length: config.rows }, () => Array(config.columns).fill(''));
  }

  getTableRowLabel(field: any, rowIndex: number): string {
    const config = this.getTableConfig(field);
    if (config.rowLabels && config.rowLabels[rowIndex]) {
      return config.rowLabels[rowIndex];
    }
    return `Row ${rowIndex + 1}`;
  }

  getTableColumns(field: any): number[] {
    const tableData = this.getTableData(field);
    if (tableData.length > 0 && Array.isArray(tableData[0])) {
      return Array.from({ length: tableData[0].length }, (_, i) => i);
    }
    const config = this.getTableConfig(field);
    return Array.from({ length: config.columns }, (_, i) => i);
  }

  getStatusBadgeClass(status: string | undefined): string {
    if (!status) return 'badge-outline-secondary';
    switch (status.toUpperCase()) {
      case 'PENDING':
        return 'badge-outline-warning';
      case 'APPROVED':
        return 'badge-outline-success';
      case 'REJECTED':
        return 'badge-outline-danger';
      case 'IN_PROGRESS':
        return 'badge-outline-info';
      default:
        return 'badge-outline-secondary';
    }
  }

  // Check if a department in the pipeline has been approved (and still valid after send-back).
  isDepartmentApproved(pipelineOrder: number): boolean {
    if (!this.applicationDetails) return false;
    // CAPF virtual initiator stage uses negative pipelineOrder (e.g. -1).
    // Never treat it as automatically approved; backend/signature must exist.
    if (pipelineOrder < 0) return false;
    if (this.isCapfForm()) {
      const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
      return pend != null && pipelineOrder < pend;
    }
    const currentLevel = this.applicationDetails.intCurrentApprovalLevel ?? 0;
    return currentLevel > pipelineOrder;
  }

  // When a department has multiple heads/HOD approvers, show them separately in the UI.
  // `departmentHeadMap` stores the departmentId -> headIds mapping (can be a comma-separated string).
  getDepartmentHeadIds(departmentId?: number): number[] {
    if (departmentId == null) return [];
    if (!this.departmentHeadMap || this.departmentHeadMap.size === 0) return [];

    const raw = this.departmentHeadMap.get(Number(departmentId));
    if (raw == null) return [];

    const parseOne = (v: any): number | null => {
      const n = typeof v === 'number' ? v : parseInt(String(v), 10);
      return !isNaN(n) && n > 0 ? n : null;
    };

    if (typeof raw === 'number') return [raw].filter(n => n > 0);

    if (Array.isArray(raw)) {
      return Array.from(new Set(raw.map(parseOne).filter((n): n is number => n != null)));
    }

    const asString = String(raw);
    if (!asString.trim()) return [];
    return Array.from(new Set(
      asString.split(',').map(s => parseOne(s.trim())).filter((n): n is number => n != null)
    ));
  }

  // For a given stage+department, return all distinct approvers/users we can find:
  // 1) from departmentHeadMap (if configured)
  // 2) from approvalHistory (so even non-HOD users within the same department show up)
  getDepartmentStageApproverIds(pipelineOrder: number, departmentId?: number): number[] {
    if (departmentId == null) return [];
    if (!this.applicationDetails) return [];

    const ids = new Set<number>();

    for (const id of this.getDepartmentHeadIds(departmentId)) {
      ids.add(Number(id));
    }

    const parseId = (v: any): number | null => {
      const n = typeof v === 'number' ? v : parseInt(String(v), 10);
      return !isNaN(n) && n > 0 ? n : null;
    };

    if (this.approvalHistory && this.approvalHistory.length > 0) {
      // CAPF level↔pipelineOrder mapping can drift by a couple steps (virtual initiator,
      // re-approval after send-back, etc.). Use a wider tolerance so we still list
      // all approvers that belong to "this stage".
      // Important: keep this tolerance tight to avoid pulling approvals from
      // other repeated steps (e.g. Finance approved at step 1 showing as approved at step 4).
      const allowedLevels = [
        pipelineOrder,
        pipelineOrder - 1, pipelineOrder + 1,
        pipelineOrder - 2, pipelineOrder + 2
      ];

      for (const e of this.approvalHistory) {
        if (!e) continue;
        const action = (e.action || e.status || '').toString().toUpperCase();
        if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') continue;

        const entryLevel = e.level ?? e.intApprovalOrder;
        if (entryLevel == null) continue;
        const lvl = Number(entryLevel);
        if (!allowedLevels.includes(lvl)) continue;

        const entryDeptId = e.departmentId;
        if (entryDeptId == null || Number(entryDeptId) !== Number(departmentId)) continue;

        const approver = e.approvedBy ?? e.userId ?? e.userApproverId;
        const approverId = parseId(approver);
        if (approverId != null) ids.add(approverId);
      }
    }

    return Array.from(ids).filter((n) => !isNaN(n) && n > 0);
  }

  /**
   * Minimum history level / intApprovalOrder not yet completed (aligns with backend CAPF PDF filtering).
   */
  private getCapfPendingExclusiveMinHistoryLevelFe(): number | null {
    if (!this.isCapfForm() || !this.applicationDetails) return null;
    const capfLevel = this.applicationDetails.intCurrentApprovalLevel;
    const pipelines = this.getPipelineData() || [];
    if (capfLevel == null || pipelines.length === 0) return null;
    const hasPrependedInitiator = pipelines[0]?.intApprovalOrder === -1;
    if (capfLevel === -1) return 0;
    if (capfLevel <= 0) {
      const firstReal =
        pipelines.find((p: any) => (p?.intApprovalOrder ?? 0) >= 0) ?? (hasPrependedInitiator ? pipelines[1] : pipelines[0]);
      return firstReal?.intApprovalOrder ?? 1;
    }
    const pipelineIndex = hasPrependedInitiator ? capfLevel : capfLevel - 1;
    if (pipelineIndex < 0 || pipelineIndex >= pipelines.length) return null;
    return pipelines[pipelineIndex]?.intApprovalOrder ?? capfLevel;
  }

  getDepartmentHeadStatus(pipelineOrder: number, departmentId: number | undefined, headId: number): string {
    if (!departmentId) return '';
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();

    const entry = this.getStageHeadHistoryEntry(pipelineOrder, departmentId, headId);
    const entryAction = (entry?.action || entry?.status || '').toString().toUpperCase();
    const hasApproved = entryAction === 'APPROVED';
    const hasRejected = entryAction === 'REJECTED';

    if (hasRejected) return 'REJECTED';

    if (this.isCapfForm() && pend != null && !isVirtualInitiatorStage) {
      if (pipelineOrder > pend) return 'PENDING';
      if (pipelineOrder < pend) {
        return hasApproved ? 'APPROVED' : 'PENDING';
      }
      if (hasApproved) {
        const coHeads = this.getDepartmentStageApproverIds(pipelineOrder, departmentId);
        if (coHeads.length > 1) return 'APPROVED';
        return 'CURRENT';
      }
      return 'CURRENT';
    }

    if (hasApproved) {
      // Virtual initiator stage: rely on history (it isn't a real pipeline step).
      if (isVirtualInitiatorStage) return 'APPROVED';
      // Multi-HOD: this card's head already signed while peers are still pending at the same stage.
      const coHeads = this.getDepartmentStageApproverIds(pipelineOrder, departmentId);
      if (coHeads.length > 1 && pipelineOrder === currentLevel) return 'APPROVED';
      // Real stages: only show approved when the workflow has advanced past it.
      if (pipelineOrder < currentLevel) return 'APPROVED';
      if (pipelineOrder === currentLevel) return 'CURRENT';
      return 'PENDING';
    }

    // No approved entry yet.
    if (!isVirtualInitiatorStage && pipelineOrder === currentLevel) return 'CURRENT';
    return 'PENDING';
  }

  getDepartmentApproverApprovalDate(pipelineOrder: number, departmentId: number | undefined, approverId: number): string {
    if (!departmentId) return '';
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
    if (this.isCapfForm() && pend != null && !isVirtualInitiatorStage && pipelineOrder > pend) return '';
    if (!this.isCapfForm() && !isVirtualInitiatorStage && pipelineOrder > currentLevel) return '';
    const entry = this.getStageHeadHistoryEntry(pipelineOrder, departmentId, approverId);
    if (!entry?.approvedDate) return '';
    try {
      const date = new Date(entry.approvedDate);
      return date.toLocaleString();
    } catch {
      return entry.approvedDate;
    }
  }

  getDepartmentApproverRemarks(pipelineOrder: number, departmentId: number | undefined, approverId: number): string {
    if (!departmentId) return '';
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
    if (this.isCapfForm() && pend != null && !isVirtualInitiatorStage && pipelineOrder > pend) return '';
    if (!this.isCapfForm() && !isVirtualInitiatorStage && pipelineOrder > currentLevel) return '';
    const entry = this.getStageHeadHistoryEntry(pipelineOrder, departmentId, approverId);
    return entry?.remarks ? String(entry.remarks) : '';
  }

  getUserNameById(userId?: number): string {
    if (!userId || !this.userNameMap) return '';
    return (this.userNameMap.get(Number(userId)) as string) || '';
  }

  getApprovalLogRows(): any[] {
    if (!Array.isArray(this.approvalHistory) || this.approvalHistory.length === 0) {
      return [];
    }
    return this.approvalHistory;
  }

  getApprovalLogLevel(entry: any): string {
    const level = entry?.level ?? entry?.intApprovalOrder;
    return level != null && String(level).trim() !== '' ? String(level) : '--';
  }

  getApprovalLogApprover(entry: any): string {
    if (!entry) return '--';
    const direct =
      entry?.approverName ||
      entry?.approvedByName ||
      entry?.userName;
    if (direct && String(direct).trim() !== '') return String(direct);

    const id = entry?.approvedBy ?? entry?.approverUserId ?? entry?.userId;
    const n = id != null ? Number(id) : NaN;
    if (!isNaN(n)) {
      return this.getUserNameById(n) || `User ${n}`;
    }
    return '--';
  }

  getApprovalLogRole(entry: any): string {
    const role = entry?.role || entry?.departmentName || entry?.txtDepartmentName;
    return role && String(role).trim() !== '' ? String(role) : '--';
  }

  getApprovalLogStatus(entry: any): string {
    const action = entry?.action || entry?.status;
    return action && String(action).trim() !== '' ? String(action) : '--';
  }

  getApprovalLogDate(entry: any): string {
    const date = entry?.approvedDate || entry?.sentBackDate;
    return date && String(date).trim() !== '' ? String(date) : '--';
  }

  getApprovalLogComments(entry: any): string {
    const comments = entry?.remarks ?? entry?.comment;
    return comments && String(comments).trim() !== '' ? String(comments) : '--';
  }

  getApprovalLogSignatureUrl(entry: any): string {
    if (!entry || !entry?.signaturePath) return '';
    const id = entry?.approvedBy ?? entry?.approverUserId ?? entry?.userId;
    const n = id != null ? Number(id) : NaN;
    if (isNaN(n) || n <= 0) return '';
    return `${urls.API_URL}getSignature?userId=${n}`;
  }

  // Latest approval entry for a specific head at a specific stage.
  private getStageHeadHistoryEntry(pipelineOrder: number, departmentId: number, headId: number): any {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return null;

    const candidates = this.approvalHistory.filter((e: any) => {
      const action = (e.action || '').toString().toUpperCase();
      if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;

      const entryLevel = e.level ?? e.intApprovalOrder;
      if (entryLevel == null) return false;
      const lvl = Number(entryLevel);
      // Keep tolerant matching tight to avoid pulling approvals from other repeated steps.
      const allowedLevels = [
        pipelineOrder,
        pipelineOrder - 1, pipelineOrder + 1,
        pipelineOrder - 2, pipelineOrder + 2
      ];
      if (!allowedLevels.includes(lvl)) {
        return false;
      }

      if (e.departmentId == null || Number(e.departmentId) !== Number(departmentId)) return false;

      const entryApprover = (e.approvedBy ?? e.userId ?? e.userApproverId);
      if (entryApprover == null) return false;
      if (Number(entryApprover) !== Number(headId)) return false;

      return action === 'APPROVED' || action === 'REJECTED' || action === (e.status || '').toString().toUpperCase();
    });

    if (candidates.length === 0) return null;
    candidates.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));

    if (this.isCapfForm()) {
      const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
      if (pend != null && pipelineOrder === pend) {
        const resetT = this.getCurrentRoundResetTime();
        if (resetT > 0) {
          const alive = candidates.filter((c) => this.getApprovalEntryTime(c) > resetT);
          if (alive.length === 0) return null;
          return alive[0];
        }
      }
    }
    return candidates[0];
  }

  // Get remarks for a specific department from approval history (uses latest approval entry)
  getDepartmentRemarks(pipelineOrder: number, departmentId?: number): string {
    if (!this.isDepartmentApproved(pipelineOrder)) {
      return '';
    }
    const historyEntry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (historyEntry && historyEntry.remarks) {
      return historyEntry.remarks;
    }
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    if (pipelineOrder === currentLevel && this.applicationDetails?.txtRemarks) {
      return this.applicationDetails.txtRemarks;
    }
    // Avoid misleading default text; if no remarks exist, show empty.
    return '';
  }

  // Get approval date for a department (uses latest approval entry)
  getDepartmentApprovalDate(pipelineOrder: number, departmentId?: number): string {
    if (!this.isDepartmentApproved(pipelineOrder)) {
      return '';
    }
    const historyEntry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (historyEntry && historyEntry.approvedDate) {
      try {
        const date = new Date(historyEntry.approvedDate);
        return date.toLocaleString();
      } catch (e) {
        return historyEntry.approvedDate;
      }
    }
    return '';
  }

  // Get signature for a department (uses latest approval entry)
  getDepartmentSignature(pipelineOrder: number, departmentId?: number): string {
    if (!this.isDepartmentApproved(pipelineOrder)) {
      return '';
    }
    const historyEntry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (historyEntry && historyEntry.signature) {
      return historyEntry.signature;
    }
    return '';
  }

  // Get pipeline data from application details
  getPipelineData(): any[] {
    if (!this.applicationDetails || !this.applicationDetails.cfgTblCustomForm) {
      console.log('No application details or form found');
      return [];
    }

    const form = this.applicationDetails.cfgTblCustomForm;
    console.log('Getting pipeline data from form:', form);

    let pipelines = form.approvalPipelines || form.cfgTblCustomFormApprovalPipelines;

    console.log('Found pipelines:', pipelines);

    if (form.txtApprovalPipeline && form.txtApprovalPipeline.trim()) {
      try {
        const parsed = JSON.parse(form.txtApprovalPipeline);
        if (Array.isArray(parsed) && parsed.length > 0) {
          pipelines = parsed;
          console.log('Parsed pipelines from txtApprovalPipeline (mixed dept+individual):', pipelines);
        }
      } catch (e) {
        console.error('Error parsing approval pipeline JSON:', e);
      }
    }
    if (!pipelines || !Array.isArray(pipelines) || pipelines.length === 0) {
      pipelines = form.approvalPipelines || form.cfgTblCustomFormApprovalPipelines;
    }

    if (!pipelines || !Array.isArray(pipelines) || pipelines.length === 0) {
      console.log('No pipelines found or empty array');
      return [];
    }

    const normalized = [...pipelines].filter((p: any) => {
      if (!p) return false;
      if (p.type === 'individual') {
        const uid = p.serUserId ?? p.userId ?? p.hrTblUser?.serUserId;
        return uid != null;
      }
      const deptId = p.hrTblDepartment?.serDepartmentId || p.serDepartmentId || p.departmentId;
      const deptName =
        p.hrTblDepartment?.txtDepartmentName ||
        p.departmentName ||
        p.txtDepartmentName;
      if (deptName && String(deptName).trim() !== '') return true;
      if (deptId == null) return false;
      if (this.departmentNameMap && this.departmentNameMap.size > 0) {
        return this.departmentNameMap.has(Number(deptId));
      }
      return true;
    });

    const sortedPipelines = normalized.sort((a: any, b: any) =>
      (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0)
    );
    console.log('Sorted pipelines:', sortedPipelines);

    // Prepend Initiator stage so it appears in the workflow
    const initiatorUserId =
      this.applicationDetails?.serSubmittedBy ||
      this.applicationDetails?.cfgTblUser?.serUserId ||
      this.applicationDetails?.serUserId ||
      null;
    const initiatorUserName =
      this.applicationDetails?.cfgTblUser?.txtUserName ||
      this.applicationDetails?.txtSubmittedBy ||
      this.applicationDetails?.submittedByName ||
      this.applicationDetails?.txtUserName ||
      'Initiator';

    let initiatorDeptId =
      this.applicationDetails?.hrTblDepartment?.serDepartmentId ||
      this.applicationDetails?.serDepartmentId ||
      this.applicationDetails?.cfgTblUser?.hrTblDepartment?.serDepartmentId ||
      this.applicationDetails?.cfgTblUser?.serDepartmentId ||
      null;

    let initiatorDeptName =
      this.applicationDetails?.hrTblDepartment?.txtDepartmentName ||
      this.applicationDetails?.departmentName ||
      this.applicationDetails?.cfgTblUser?.hrTblDepartment?.txtDepartmentName ||
      this.applicationDetails?.cfgTblUser?.txtDepartmentName ||
      null;

    // If we have a departmentId but no name, try the loaded department map
    if (!initiatorDeptName && initiatorDeptId && this.departmentNameMap?.has(Number(initiatorDeptId))) {
      initiatorDeptName = this.departmentNameMap.get(Number(initiatorDeptId)) as string;
    }

    // Fallback to approval history entry for level 0 (if exists)
    if (!initiatorDeptName && this.approvalHistory && this.approvalHistory.length > 0) {
      const first = this.approvalHistory.find((e: any) => Number(e.level) === 0);
      if (first?.departmentName) initiatorDeptName = first.departmentName;
      if (!initiatorDeptId && first?.departmentId) initiatorDeptId = first.departmentId;
    }

    // Final fallback: look up department by submitter's departmentId via map; if still none, keep generic.
    if (!initiatorDeptName && initiatorDeptId && this.departmentNameMap?.has(Number(initiatorDeptId))) {
      initiatorDeptName = this.departmentNameMap.get(Number(initiatorDeptId)) as string;
    }
    if (!initiatorDeptName) {
      initiatorDeptName = initiatorDeptId ? `Department ${initiatorDeptId}` : 'Department';
    }

    const hasInitiatorStage = sortedPipelines.some((p: any) => p.isInitiator === true);

    if (!hasInitiatorStage && this.isCapfForm()) {
      sortedPipelines.unshift({
        intApprovalOrder: -1,
        hrTblDepartment: initiatorDeptId
          ? { serDepartmentId: initiatorDeptId, txtDepartmentName: initiatorDeptName }
          : null,
        serDepartmentId: initiatorDeptId,
        departmentName: initiatorDeptName,
        txtDepartmentName: initiatorDeptName,
        isInitiator: true
      });

      // Also inject a history entry so "Approved By" shows the initiator and submission time
      const hasInitiatorHistory = this.approvalHistory?.some(
        (e: any) => Number(e.level) === 0
      );
      const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
      // Do NOT auto-mark initiator as approved unless backend has actually advanced the workflow.
      // Otherwise the first stage appears "APPROVED" even before initiator HOD approval.
      if (!hasInitiatorHistory && currentLevel > 0) {
        const entry: any = {
          level: 0,
          departmentId: initiatorDeptId,
          departmentName: initiatorDeptName,
          approverName: initiatorUserName,
          approvedBy: initiatorUserId,
          approvedDate: this.applicationDetails?.dteCreatedDate || this.applicationDetails?.createdAt || new Date().toISOString(),
          approvedVia: this.applicationDetails?.txtIpAddress ? 'IP:' + this.applicationDetails.txtIpAddress : 'SUBMISSION',
          approvedIp: this.applicationDetails?.txtIpAddress || '',
          status: 'APPROVED',
          action: 'APPROVED'
        };
        this.approvalHistory = [entry, ...(this.approvalHistory || [])];
      }
    }

    return sortedPipelines;
  }

  getPipelineDepartmentId(pipeline: any): number | undefined {
    const v =
      pipeline?.hrTblDepartment?.serDepartmentId ??
      pipeline?.serDepartmentId ??
      pipeline?.departmentId ??
      pipeline?.hrTblDepartment?.serDepartmentHeadId ??
      pipeline?.intDepartmentId;
    const n = typeof v === 'number' ? v : v != null ? parseInt(String(v), 10) : NaN;
    return !isNaN(n) && n > 0 ? n : undefined;
  }

  /** One entry per "card" in the workflow. Multi-approver stages (e.g. Technical Expert with Haris + user 1) get one card per approver. */
  getPipelineCardsForDisplay(): Array<{ pipeline: any; pipelineIndex: number; approverId: number | null; approverIndex: number; totalApproversInStage: number }> {
    const pipelines = this.getPipelineData() || [];
    const cards: Array<{ pipeline: any; pipelineIndex: number; approverId: number | null; approverIndex: number; totalApproversInStage: number }> = [];
    for (let i = 0; i < pipelines.length; i++) {
      const pipeline = pipelines[i];
      const order = pipeline?.intApprovalOrder ?? (i + 1);
      const deptId = this.getPipelineDepartmentId(pipeline);
      const approverIds = this.getDepartmentStageApproverIds(order, deptId ?? undefined);
      const total = approverIds.length || 1;
      if (approverIds.length <= 1) {
        cards.push({ pipeline, pipelineIndex: i, approverId: approverIds[0] ?? null, approverIndex: 0, totalApproversInStage: total });
      } else {
        approverIds.forEach((approverId, j) => {
          cards.push({ pipeline, pipelineIndex: i, approverId, approverIndex: j, totalApproversInStage: total });
        });
      }
    }

    // CAPF: append final "extra" stages after departmental pipeline.
    if (this.shouldShowCapfExtraStages()) {
      const baseIndex = pipelines.length;
      cards.push({ pipeline: { type: 'capf_ceo' }, pipelineIndex: baseIndex, approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      cards.push({ pipeline: { type: 'capf_asset_code' }, pipelineIndex: baseIndex + 1, approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      cards.push({ pipeline: { type: 'capf_pr_code' }, pipelineIndex: baseIndex + 2, approverId: null, approverIndex: 0, totalApproversInStage: 1 });
    }

    return cards;
  }

  getPipelineCardTitle(card: { pipeline: any; pipelineIndex: number; approverId: number | null; totalApproversInStage: number }): string {
    if (this.isCapfExtraPipeline(card.pipeline)) {
      return this.getCapfExtraStageTitle(card.pipeline?.type);
    }
    const baseName = this.getPipelineDepartmentName(card.pipeline, card.pipelineIndex);
    if (card.totalApproversInStage > 1 && card.approverId != null) {
      return baseName + ' - ' + (this.getUserNameById(card.approverId) || ('User ' + card.approverId));
    }
    return baseName;
  }

  getPipelineCardStatus(card: { pipeline: any; pipelineIndex: number; approverId: number | null }): string {
    if (this.isCapfExtraPipeline(card.pipeline)) {
      return this.getCapfExtraStageStatus(card.pipeline?.type);
    }
    const order = card.pipeline?.intApprovalOrder ?? (card.pipelineIndex + 1);
    const deptId = this.getPipelineDepartmentId(card.pipeline);
    if (card.approverId == null) {
      return this.getStageStatus(order, deptId ?? undefined);
    }
    return this.getDepartmentHeadStatus(order, deptId ?? undefined, card.approverId);
  }

  isCapfExtraPipeline(pipeline: any): boolean {
    const t = (pipeline?.type || '').toString().toLowerCase();
    return t === 'capf_ceo' || t === 'capf_asset_code' || t === 'capf_pr_code';
  }

  getCapfExtraStageTitle(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    if (t === 'capf_ceo') return 'CEO Approval';
    if (t === 'capf_asset_code') return 'Asset Code Assign';
    if (t === 'capf_pr_code') return 'PR';
    return 'CAPF';
  }

  private findApprovalHistoryEntry(predicate: (e: any) => boolean): any | null {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return null;
    const sorted = [...this.approvalHistory].sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    for (const e of sorted) {
      if (e && predicate(e)) return e;
    }
    return null;
  }

  getCapfExtraStageStatus(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    const status = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    const hasAsset = !!(this.applicationDetails?.txtAssetCode && String(this.applicationDetails.txtAssetCode).trim());
    const hasPr = !!(this.applicationDetails?.txtPrCode && String(this.applicationDetails.txtPrCode).trim());

    if (t === 'capf_ceo') {
      if (status === 'CEO_PENDING') return 'CURRENT';
      const ceoApproved = !!this.findApprovalHistoryEntry((e: any) => {
        const action = (e.action || e.status || '').toString().toUpperCase();
        const desig = (e.designation || e.txtDesignation || e.departmentName || '').toString().toUpperCase();
        return action === 'APPROVED' && desig.includes('CEO');
      });
      if (ceoApproved || status === 'ASSET_PENDING' || status === 'APPROVED') return 'APPROVED';
      return 'PENDING';
    }

    if (t === 'capf_asset_code') {
      if (status === 'ASSET_PENDING') return 'CURRENT';
      if (hasAsset && status === 'APPROVED') return 'APPROVED';
      // If CEO is not done yet, keep this pending.
      return 'PENDING';
    }

    if (t === 'capf_pr_code') {
      // PR is after asset code and final approval.
      if (hasPr) return 'APPROVED';
      if (hasAsset && status === 'APPROVED') return 'CURRENT';
      return 'PENDING';
    }

    return 'PENDING';
  }

  getCapfExtraStageApprovedBy(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    if (t === 'capf_ceo') {
      const e = this.findApprovalHistoryEntry((x: any) => {
        const action = (x.action || x.status || '').toString().toUpperCase();
        const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
        return action === 'APPROVED' && desig.includes('CEO');
      });
      return e?.approverName || e?.approvedByName || this.getUserNameById(e?.approvedBy) || '--';
    }
    if (t === 'capf_asset_code') {
      const e = this.findApprovalHistoryEntry((x: any) => {
        const action = (x.action || x.status || '').toString().toUpperCase();
        const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
        return action === 'APPROVED' && desig.includes('FINANCE');
      });
      return e?.approverName || this.getUserNameById(e?.approvedBy) || '--';
    }
    if (t === 'capf_pr_code') {
      const e = this.findApprovalHistoryEntry((x: any) => (x.action || x.status || '').toString().toUpperCase() === 'PR_CODE_ASSIGNED');
      return e?.approverName || this.getUserNameById(e?.approvedBy) || '--';
    }
    return '--';
  }

  getCapfExtraStageApprovedAt(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    const match = (x: any) => {
      if (t === 'capf_ceo') {
        const action = (x.action || x.status || '').toString().toUpperCase();
        const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
        return action === 'APPROVED' && desig.includes('CEO');
      }
      if (t === 'capf_asset_code') {
        const action = (x.action || x.status || '').toString().toUpperCase();
        const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
        return action === 'APPROVED' && desig.includes('FINANCE');
      }
      if (t === 'capf_pr_code') {
        return (x.action || x.status || '').toString().toUpperCase() === 'PR_CODE_ASSIGNED';
      }
      return false;
    };
    const e = this.findApprovalHistoryEntry(match);
    if (!e?.approvedDate) return '';
    try {
      return new Date(e.approvedDate).toLocaleString();
    } catch {
      return e.approvedDate;
    }
  }

  getCapfExtraStageRemarks(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    const e = this.findApprovalHistoryEntry((x: any) => {
      const action = (x.action || x.status || '').toString().toUpperCase();
      const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
      if (t === 'capf_ceo') return action === 'APPROVED' && desig.includes('CEO');
      if (t === 'capf_asset_code') return action === 'APPROVED' && desig.includes('FINANCE');
      if (t === 'capf_pr_code') return action === 'PR_CODE_ASSIGNED';
      return false;
    });
    return e?.remarks ? String(e.remarks) : '';
  }

  getCapfExtraStageApprovedVia(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    const e = this.findApprovalHistoryEntry((x: any) => {
      const action = (x.action || x.status || '').toString().toUpperCase();
      const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
      if (t === 'capf_ceo') return action === 'APPROVED' && desig.includes('CEO');
      if (t === 'capf_asset_code') return action === 'APPROVED' && desig.includes('FINANCE');
      if (t === 'capf_pr_code') return action === 'PR_CODE_ASSIGNED';
      return false;
    });
    return (e?.approvedIp ?? e?.approvedVia ?? '') || '';
  }

  getDepartmentApproverIp(pipelineOrder: number, departmentId: number | undefined, approverId: number): string {
    if (!departmentId) return '';
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
    if (this.isCapfForm() && pend != null && !isVirtualInitiatorStage && pipelineOrder > pend) return '';
    if (!this.isCapfForm() && !isVirtualInitiatorStage && pipelineOrder > currentLevel) return '';
    const entry = this.getStageHeadHistoryEntry(pipelineOrder, departmentId, approverId);
    return (entry?.approvedIp ?? entry?.approvedVia ?? '') || '';
  }

  getCardTimeTaken(cardIndex: number, cards: Array<{ pipeline: any; pipelineIndex: number; approverId: number | null }>): string {
    if (!cards || cardIndex <= 0 || cardIndex >= cards.length) return '';
    const prev = cards[cardIndex - 1];
    const curr = cards[cardIndex];
    const orderPrev = prev.pipeline?.intApprovalOrder ?? (prev.pipelineIndex + 1);
    const orderCurr = curr.pipeline?.intApprovalOrder ?? (curr.pipelineIndex + 1);
    const deptPrev = this.getPipelineDepartmentId(prev.pipeline);
    const deptCurr = this.getPipelineDepartmentId(curr.pipeline);
    if (deptPrev == null || deptCurr == null) return '';
    const datePrev = prev.approverId != null
      ? (this.getStageHeadHistoryEntry(orderPrev, deptPrev, prev.approverId)?.approvedDate)
      : (this.getStageHistoryEntry(orderPrev, deptPrev)?.approvedDate);
    const dateCurr = curr.approverId != null
      ? (this.getStageHeadHistoryEntry(orderCurr, deptCurr, curr.approverId)?.approvedDate)
      : (this.getStageHistoryEntry(orderCurr, deptCurr)?.approvedDate);
    if (!datePrev || !dateCurr) return '';
    try {
      const t1 = new Date(datePrev).getTime();
      const t2 = new Date(dateCurr).getTime();
      if (isNaN(t1) || isNaN(t2)) return '';
      const diffMs = t2 - t1;
      const mins = Math.floor(diffMs / 60000);
      const hours = Math.floor(mins / 60);
      const days = Math.floor(hours / 24);
      if (days > 0) return `${days}d ${hours % 24}h`;
      if (hours > 0) return `${hours}h ${mins % 60}m`;
      return `${mins}m`;
    } catch {
      return '';
    }
  }

  goBack() {
    if (this.fromPendingApprovals) {
      this.router.navigate(['/pending-approvals']);
      return;
    }
    this.router.navigate(['/applicationsview']);
  }

  // Count stages by status — used by modal quick-stats
  countStages(pipelines: any[], status: string): number {
    if (!pipelines) return 0;
    return pipelines.filter((p: any, i: number) =>
      this.getStageStatus(p.intApprovalOrder || (i + 1), p.hrTblDepartment?.serDepartmentId) === status
    ).length;
  }



  showApprovalActions(): boolean {
    // Always show action buttons when the application details are visible.
    // Backend will enforce whether the current user can actually perform the action.
    return !!this.applicationDetails;
  }

  /** Returns true only if the current user is the approver for the current level (prevents Level 2 acting on behalf of Level 3) */
  private isCurrentUserApproverForCurrentLevel(): boolean {
    const userId = this.getCurrentUserId();
    if (!userId) return false;
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    if (currentLevel < 0) return false;

    const fields = this.getIndividualPipelineFooterFields();
    if (fields && fields.length > 0) {
      const sequence: number[] = [];
      for (const section of fields) {
        const key = (section?.key || '').toString().toLowerCase();
        if (key === 'prepared_by') continue;
        const users = this.getIndividualFooterSlots(section);
        for (const u of users) {
          if (!u) continue;
          const uid = u.serUserId ?? u.userId ?? u.id;
          if (uid != null) sequence.push(Number(uid));
        }
      }
      if (currentLevel >= sequence.length) return false;
      return sequence[currentLevel] === Number(userId);
    }

    const pipelines = this.getPipelineData();
    if (pipelines && pipelines.length > 0) {
      // CAPF note:
      // `getPipelineData()` prepends an "Initiator" stage (intApprovalOrder = -1) for CAPF so indices shift by +1.
      // Therefore, when that initiator stage exists, CAPF currentLevel maps directly to pipeline index.
      // For safety (older data / other screens), fallback to the legacy mapping when initiator stage is not present.
      let pipelineIndex = currentLevel;
      if (this.isCapfForm()) {
        const hasPrependedInitiator = (pipelines[0]?.intApprovalOrder === -1);
        pipelineIndex = hasPrependedInitiator ? currentLevel : (currentLevel - 1);
      }
      if (pipelineIndex < 0 || pipelineIndex >= pipelines.length) return false;
      const pipeline = pipelines[pipelineIndex];
      if (pipeline?.type === 'individual') {
        const uid = pipeline.serUserId ?? pipeline.userId ?? pipeline.hrTblUser?.serUserId;
        return uid != null && Number(uid) === Number(userId);
      }
      const deptId = pipeline?.hrTblDepartment?.serDepartmentId ?? pipeline?.serDepartmentId ?? pipeline?.departmentId;
      if (deptId == null) return false;
      const order = pipeline?.intApprovalOrder ?? (pipelineIndex + 1);
      const headIds = this.getDepartmentStageApproverIds(order, Number(deptId));
      if (headIds.length > 0) {
        return headIds.some((h) => Number(h) === Number(userId));
      }
      const headId = this.departmentHeadMap.get(Number(deptId));
      return headId != null && Number(headId) === Number(userId);
    }
    return false;
  }

  /** Show "Send back to initiator" only when level >= 2 (same as in emails). */
  showSendBackToInitiatorButton(): boolean {
    if (!this.showApprovalActions() || !this.applicationDetails) return false;
    const level = this.applicationDetails.intCurrentApprovalLevel ?? 0;
    return level >= 2;
  }

  private getCurrentUserId(): number | null {
    return this.currentUser?.serUserId || this.currentUser?.userId || this.currentUser?.id || null;
  }

  private hasUserAlreadyActed(): boolean {
    const userId = this.getCurrentUserId();
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return false;
    return this.approvalHistory.some((e: any) => {
      const actorId = e.approvedBy || e.approverUserId || e.userId;
      if (Number(actorId) !== Number(userId)) return false;
      const action = (e.action || e.status || '').toString().toUpperCase();
      return ['APPROVED', 'REJECTED', 'SEND_BACK', 'SENT_BACK', 'SENTBACK', 'SENT_BACK_TO_INITIATOR'].includes(action) || !!e.approvedDate;
    });
  }

  /**
   * For individual pipeline footer / budget approval, backend stores history `level` as 1-based
   * (approver sequence index + 1) while `intCurrentApprovalLevel` is the 0-based index of the pending approver.
   * Department pipeline entries may use a different convention; keep comparing to the raw index there.
   */
  private getExpectedHistoryLevelForPendingStage(): number {
    const pendingIndex = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    if (this.hasIndividualPipelineFooter() || this.isBudgetApprovalForm()) {
      return pendingIndex + 1;
    }
    return pendingIndex;
  }

  /**
   * Department pipeline (e.g. CAPF): multiple HODs share intCurrentApprovalLevel until all have signed.
   * Guard logic must not treat a co-head's approval as "current user already finished".
   */
  private isDepartmentMultiHeadPendingStage(): boolean {
    if (this.hasIndividualPipelineFooter() || this.isBudgetApprovalForm()) return false;
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    if (currentLevel < 0) return false;
    const pipelines = this.getPipelineData() || [];
    if (pipelines.length === 0) return false;
    let pipelineIndex = currentLevel;
    if (this.isCapfForm()) {
      const hasPrependedInitiator = pipelines[0]?.intApprovalOrder === -1;
      pipelineIndex = hasPrependedInitiator ? currentLevel : currentLevel - 1;
    }
    if (pipelineIndex < 0 || pipelineIndex >= pipelines.length) return false;
    const pipeline = pipelines[pipelineIndex];
    if ((pipeline?.type || '').toString().toLowerCase() === 'individual') return false;
    const deptId = this.getPipelineDepartmentId(pipeline);
    if (deptId == null) return false;
    const order = pipeline?.intApprovalOrder ?? (pipelineIndex + 1);
    return this.getDepartmentStageApproverIds(order, deptId).length > 1;
  }

  private isCurrentLevelAlreadyHandled(): boolean {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return false;
    const useSeq = this.hasIndividualPipelineFooter() || this.isBudgetApprovalForm();
    const pendingIndex = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    if (!useSeq && !pendingIndex) return false;
    const expectedLevel = this.getExpectedHistoryLevelForPendingStage();
    const multiHeadDept = this.isDepartmentMultiHeadPendingStage();
    const currentUserId = this.getCurrentUserId();
    return this.approvalHistory.some((e: any) => {
      if (Number(e.level) !== Number(expectedLevel)) return false;
      const action = (e.action || e.status || '').toString().toUpperCase();
      // Send-back is not "stage finished": the same approver must act again after a lower level re-approves.
      if (action !== 'APPROVED' && action !== 'REJECTED') return false;
      if (multiHeadDept && currentUserId != null) {
        const actorId = e.approvedBy ?? e.approverUserId ?? e.userId;
        if (Number(actorId) !== Number(currentUserId)) return false;
      }
      return true;
    });
  }

  /** Reset time: when did a higher level last send the app down to us? (SENT_BACK/SENT_BACK_TO_INITIATOR from level > currentLevel) */
  private getCurrentRoundResetTime(): number {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return 0;
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const terminalActions = ['SENT_BACK', 'SENTBACK', 'SENT_BACK_TO_INITIATOR'];
    const sorted = [...this.approvalHistory].sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    for (const e of sorted) {
      const action = (e.action || e.status || '').toString().toUpperCase();
      const entryLevel = Number(e.level ?? e.intApprovalOrder ?? 0);
      if (terminalActions.includes(action) && entryLevel > currentLevel) {
        return this.getApprovalEntryTime(e);
      }
    }
    return 0;
  }

  /** True if the current level has already been acted upon in this round (after any send-back from higher level). */
  private isCurrentLevelHandledThisRound(): boolean {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return false;
    const useSeq = this.hasIndividualPipelineFooter() || this.isBudgetApprovalForm();
    const pendingIndex = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    if (!useSeq && !pendingIndex) return false;
    const expectedLevel = this.getExpectedHistoryLevelForPendingStage();
    const resetTime = this.getCurrentRoundResetTime();
    const multiHeadDept = this.isDepartmentMultiHeadPendingStage();
    const currentUserId = this.getCurrentUserId();
    return this.approvalHistory.some((e: any) => {
      if (Number(e.level) !== Number(expectedLevel)) return false;
      const action = (e.action || e.status || '').toString().toUpperCase();
      // Only approve/reject complete this stage. Old SENT_BACK at this level must not block the next cycle.
      if (action !== 'APPROVED' && action !== 'REJECTED') return false;
      if (this.getApprovalEntryTime(e) < resetTime) return false;
      if (multiHeadDept && currentUserId != null) {
        const actorId = e.approvedBy ?? e.approverUserId ?? e.userId;
        if (Number(actorId) !== Number(currentUserId)) return false;
      }
      return true;
    });
  }

  /** Returns true when the application has already been approved, rejected, sent back, or sent back to initiator. */
  private isApplicationAlreadyActedUpon(): boolean {
    if (!this.applicationDetails) return false;
    const status = (this.applicationDetails.txtStatus || '').toUpperCase();
    // Only block immediately after a send-back-to-initiator action while the
    // application is still in that terminal state. Once initiator resubmits and
    // status moves back to PENDING/IN_PROGRESS, approvers must be able to act again.
    if (status === 'SENT_BACK_TO_INITIATOR') return true;
    if (status === 'APPROVED' || status === 'REJECTED') return true;
    if (this.isCurrentLevelHandledThisRound()) return true;
    return false;
  }

  private readonly NOT_AUTHORIZED_MSG = 'You are not authorized to perform this action';

  @ViewChild('approveModal') approveModal: any;
  @ViewChild('rejectModal') rejectModal: any;
  @ViewChild('sendBackModal') sendBackModal: any;
  @ViewChild('sendBackToInitiatorModal') sendBackToInitiatorModal: any;
  @ViewChild('vendorEditModal') vendorEditModal: any;

  openApproveModal() {
    if (!this.applicationDetails?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      return;
    }
    this.selectedApplicationForRemarks = this.applicationDetails;
    this.remarksText = '';
    this.approveModal.open();
  }

  openRejectModal() {
    if (!this.applicationDetails?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      return;
    }
    this.selectedApplicationForRemarks = this.applicationDetails;
    this.remarksText = '';
    this.rejectModal.open();
  }

  openSendBackModal() {
    if (!this.applicationDetails?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      return;
    }
    this.selectedApplicationForRemarks = this.applicationDetails;
    this.remarksText = '';
    this.sendBackModal.open();
  }

  openSendBackToInitiatorModal() {
    if (!this.applicationDetails?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      return;
    }
    this.selectedApplicationForRemarks = this.applicationDetails;
    this.remarksText = '';
    this.sendBackToInitiatorModal.open();
  }

  async approveApplication() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      this.approveModal.close();
      return;
    }

    if (this.isApproving) return;

    if (!this.remarksText || this.remarksText.trim() === '') {
      this.notificationService.showMessage('Please enter comments or remarks before approving', 'danger');
      return;
    }

    this.isApproving = true;

    // Do not upload a new PDF before approving: the backend updates the stored PDF by appending
    // only the new signature. Uploading a frontend-generated PDF here would overwrite the PDF
    // and cause duplicate signatures when mixing email and portal approvals.

     this.customFormApplicationService.approveApplication(
       this.selectedApplicationForRemarks.serApplicationId,
       this.remarksText,
       this.getCurrentUserId() ?? undefined
     ).pipe(
      finalize(() => {
        this.isApproving = false;
      })
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application approved successfully', 'success');
          this.approveModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.applyOptimisticAction('APPROVED');
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to approve application', 'danger');
        }
      },
      (error) => {
        const msg = (error.error?.message || error.message || '').toString().toLowerCase();
        if (msg.includes('not authorized') || msg.includes('already approved') || msg.includes('already acted') || error.status === 403) {
          this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
        } else {
          this.notificationService.showMessage('Error approving application: ' + (error.error?.message || error.message), 'danger');
        }
      }
    );
  }

  rejectApplication() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      this.rejectModal.close();
      return;
    }

    if (!this.remarksText || this.remarksText.trim() === '') {
      this.notificationService.showMessage('Please provide a rejection reason', 'danger');
      return;
    }

    if (this.isRejecting) return;
    this.isRejecting = true;
    this.customFormApplicationService.rejectApplication(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
    ).pipe(
      finalize(() => {
        this.isRejecting = false;
      })
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application rejected successfully', 'success');
          this.rejectModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.applyOptimisticAction('REJECTED');
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to reject application', 'danger');
        }
      },
      (error) => {
        const msg = (error.error?.message || error.message || '').toString().toLowerCase();
        if (msg.includes('not authorized') || msg.includes('already rejected') || msg.includes('already acted') || error.status === 403) {
          this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
        } else {
          this.notificationService.showMessage('Error rejecting application: ' + (error.error?.message || error.message), 'danger');
        }
      }
    );
  }

  sendBackApplication() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      this.sendBackModal.close();
      return;
    }

    if (!this.remarksText || this.remarksText.trim() === '') {
      this.notificationService.showMessage('Please provide remarks for sending back the application', 'danger');
      return;
    }

    if (!this.selectedApplicationForRemarks.intCurrentApprovalLevel || this.selectedApplicationForRemarks.intCurrentApprovalLevel <= 0) {
      this.notificationService.showMessage('Cannot send back application: Already at initial level', 'danger');
      return;
    }

    if (this.isSendingBack) return;
    this.isSendingBack = true;
    this.customFormApplicationService.sendBackApplication(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
    ).pipe(
      finalize(() => {
        this.isSendingBack = false;
      })
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application sent back successfully', 'success');
          this.sendBackModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.applyOptimisticAction('SENT_BACK');
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to send back application', 'danger');
        }
      },
      (error) => {
        const msg = (error.error?.message || error.message || '').toString().toLowerCase();
        if (msg.includes('not authorized') || msg.includes('already acted') || error.status === 403) {
          this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
        } else {
          this.notificationService.showMessage('Error sending back application: ' + (error.error?.message || error.message), 'danger');
        }
      }
    );
  }

  sendBackToInitiator() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    if (this.isApplicationAlreadyActedUpon()) {
      this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
      this.sendBackToInitiatorModal.close();
      return;
    }

    if (!this.remarksText || this.remarksText.trim() === '') {
      this.notificationService.showMessage('Please provide remarks for sending back the application to initiator', 'danger');
      return;
    }

    if (this.isSendingBackToInitiator) return;
    this.isSendingBackToInitiator = true;
    this.customFormApplicationService.sendBackToInitiator(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
    ).pipe(
      finalize(() => {
        this.isSendingBackToInitiator = false;
      })
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application sent back to initiator successfully', 'success');
          this.sendBackToInitiatorModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.applyOptimisticAction('SENT_BACK_TO_INITIATOR');
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to send back application to initiator', 'danger');
        }
      },
      (error) => {
        const msg = (error.error?.message || error.message || '').toString().toLowerCase();
        if (msg.includes('not authorized') || msg.includes('already acted') || error.status === 403) {
          this.notificationService.showMessage(this.NOT_AUTHORIZED_MSG, 'danger');
        } else {
          this.notificationService.showMessage('Error sending back to initiator: ' + (error.error?.message || error.message), 'danger');
        }
      }
    );
  }

  /** Optimistically update local state immediately after a successful action so rapid re-clicks are blocked before loadApplicationDetails completes */
  private applyOptimisticAction(action: string): void {
    if (!this.applicationDetails) return;
    const now = new Date().toISOString();
    if (action === 'APPROVED' || action === 'REJECTED') {
      this.applicationDetails = { ...this.applicationDetails, txtStatus: action };
      return;
    }
    const userId = this.getCurrentUserId();
    const currentLevel = this.applicationDetails.intCurrentApprovalLevel ?? 0;
    const newEntry: any = {
      action,
      level: currentLevel,
      approvedDate: now,
      approvedBy: userId,
      approverName: this.currentUser?.txtUserName || this.currentUser?.userName || this.currentUser?.name || '',
    };
    this.approvalHistory = [...(this.approvalHistory || []), newEntry];
  }

  /** Get approvedDate as timestamp for sorting (earliest first) */
  private getApprovalEntryTime(e: any): number {
    const d = e?.approvedDate;
    if (!d) return 0;
    if (typeof d === 'number') return d;
    const t = new Date(d).getTime();
    return isNaN(t) ? 0 : t;
  }

  // Get history entry for a given pipeline stage (by order/level)
  // When there are multiple approvals at the same level (e.g. re-approval after send-back), return the latest one so we show the most recent signature and timestamp
  getStageHistoryEntry(pipelineOrder: number, departmentId?: number): any {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return null;

    const hasIndividualPipelineFooter = this.getIndividualPipelineFooterFields().length > 0;
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;

    const matches = (pred: (e: any) => boolean): any[] => {
      return this.approvalHistory!.filter((e: any) => {
        const action = (e.action || '').toString().toUpperCase();
        if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;
        if (hasIndividualPipelineFooter) {
          const entryLevel = e.level || e.intApprovalOrder;
          if (entryLevel != null && entryLevel > currentLevel) return false;
        }
        return pred(e);
      });
    };

    let candidates: any[] = [];
    if (departmentId) {
      candidates = matches((e) => e.level === pipelineOrder && e.departmentId === departmentId);
    }
    if (candidates.length === 0) {
      candidates = matches((e) => e.level === pipelineOrder);
    }
    if (candidates.length === 0 && departmentId && pipelineOrder < 0) {
      // Virtual CAPF initiator stage may be represented by a different "level" in stored history.
      // For real (non-negative) stages, avoid department-only fallback because it can mark
      // a later stage as approved using another stage's history.
      candidates = matches((e) => e.departmentId === departmentId);
    }
    if (candidates.length === 0) return null;
    // Sort by approvedDate descending and return the latest approval
    candidates.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));

    if (this.isCapfForm()) {
      const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
      if (pend != null && pipelineOrder === pend) {
        const resetT = this.getCurrentRoundResetTime();
        if (resetT > 0) {
          const alive = candidates.filter((c) => this.getApprovalEntryTime(c) > resetT);
          if (alive.length === 0) return null;
          return alive[0];
        }
      }
    }
    return candidates[0];
  }

  // Get stage status: APPROVED, REJECTED, CURRENT (pending at this level), PENDING
  getStageStatus(pipelineOrder: number, departmentId?: number): string {
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    const overallStatus = (this.applicationDetails?.txtStatus || '').toUpperCase();
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();

    if (this.isCapfForm() && pend != null && !isVirtualInitiatorStage) {
      if (pipelineOrder > pend) return 'PENDING';
      if (pipelineOrder < pend) {
        if (entry) {
          const action = (entry.action || entry.status || '').toUpperCase();
          if (action === 'REJECTED') return 'REJECTED';
          if (action === 'APPROVED') return 'APPROVED';
        }
        return 'PENDING';
      }
      if (entry) {
        const action = (entry.action || entry.status || '').toUpperCase();
        if (action === 'REJECTED') return 'REJECTED';
        if (action === 'APPROVED') return 'CURRENT';
      }
      return overallStatus === 'REJECTED' ? 'REJECTED' : 'CURRENT';
    }

    if (entry) {
      const action = (entry.action || entry.status || '').toUpperCase();
      if (action === 'REJECTED') return 'REJECTED';
      if (action === 'APPROVED') {
        // Virtual CAPF initiator stage uses negative pipelineOrder.
        // If backend history says it is approved, show it as approved.
        if (isVirtualInitiatorStage) return 'APPROVED';
        // A stage becomes fully approved only after workflow advances past it.
        // If the stage is the current active level, some approver might have signed
        // but the department is not complete yet.
        if (!isVirtualInitiatorStage && pipelineOrder < currentLevel) return 'APPROVED';
        if (!isVirtualInitiatorStage && pipelineOrder === currentLevel) return 'CURRENT';
        // For virtual initiator (negative order), fall through to pending/current logic
      }
      // If there is any other entry type, treat it as current/pending based on level.
    }
    if (!isVirtualInitiatorStage && pipelineOrder < currentLevel) return 'APPROVED';
    if (pipelineOrder === currentLevel) {
      return overallStatus === 'REJECTED' ? 'REJECTED' : 'CURRENT';
    }
    return 'PENDING';
  }

  getPipelineDepartmentName(pipeline: any, index: number): string {
    const order = pipeline?.intApprovalOrder || (index + 1);

    if (this.isBudgetApprovalForm()) {
      const personName = this.getCapfUserNameForStage(order);
      if (personName) return personName;
    }

    if (!pipeline) return `Stage ${index + 1}`;
    if (pipeline.type === 'individual') {
      const name = pipeline.hrTblUser?.txtUserName || pipeline.txtUserName || pipeline.userName;
      if (name) return name;
      const uid = pipeline.serUserId ?? pipeline.userId ?? pipeline.hrTblUser?.serUserId;
      if (uid && this.userNameMap.has(Number(uid))) return this.userNameMap.get(Number(uid)) as string;
      const entry = this.getStageHistoryEntry(order, undefined);
      if (entry?.departmentName) return entry.departmentName;
      return uid ? `User #${uid}` : `Individual ${index + 1}`;
    }

    const directName =
      pipeline.hrTblDepartment?.txtDepartmentName ||
      pipeline.departmentName ||
      pipeline.txtDepartmentName;
    if (directName) return directName;

    const deptId = pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId;
    if (deptId && this.departmentNameMap.has(Number(deptId))) {
      return this.departmentNameMap.get(Number(deptId)) as string;
    }
    const entry = this.getStageHistoryEntry(order, deptId);
    if (entry?.departmentName) return entry.departmentName;

    return `Department ${index + 1}`;
  }

  // Get approver name for a stage
  getStageApproverName(pipelineOrder: number, departmentId?: number): string {
    // 1. History always takes precedence (who actually approved it)
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.approverName || entry.approvedBy || entry.userName || '';
    }

    // 2. For Budget Approval, show the assigned individual's name
    if (this.isBudgetApprovalForm()) {
      const personName = this.getCapfUserNameForStage(pipelineOrder);
      if (personName) return personName;
    }

    // 3. Fallback to Department Head for other forms if pending
    if (departmentId != null) {
      const headId = this.departmentHeadMap.get(Number(departmentId));
      if (headId != null) {
        const headName = this.userNameMap.get(Number(headId));
        if (headName) return headName;
      }
    }
    return '';
  }

  // Get approver designation for a stage
  getStageApproverDesignation(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.txtDesignation || entry.designation || '';
    }
    return '';
  }

  // Get approver department for a stage
  getStageApproverDepartment(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.departmentName || entry.txtDepartmentName || '';
    }
    return '';
  }

  private getCapfUserNameForStage(order: number): string {
    if (!order || !this.applicationFormData) return '';
    const names: string[] = [];
    const preparedBy = this.applicationFormData?.preparedBy;
    if (preparedBy) {
      const name = preparedBy.txtUserName || preparedBy.userName || preparedBy.name;
      if (name) names.push(String(name));
    }
    const reviewers = Array.isArray(this.applicationFormData?.reviewers) ? this.applicationFormData.reviewers : [];
    reviewers.forEach((u: any) => {
      const name = u?.txtUserName || u?.userName || u?.name;
      if (name) names.push(String(name));
    });
    const recommenders = Array.isArray(this.applicationFormData?.recommenders) ? this.applicationFormData.recommenders : [];
    recommenders.forEach((u: any) => {
      const name = u?.txtUserName || u?.userName || u?.name;
      if (name) names.push(String(name));
    });
    const approver = this.applicationFormData?.approver;
    if (approver) {
      const name = approver.txtUserName || approver.userName || approver.name;
      if (name) names.push(String(name));
    }
    const idx = Number(order) - 1;
    return idx >= 0 && idx < names.length ? names[idx] : '';
  }

  // Get approved via channel
  getStageApprovedVia(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.approvedIp || entry.ipAddress || entry.ip || '--';
    }
    return '';
  }

  formatApprovedVia(via: string): string {
    if (!via) return '';
    return via;
  }

  getStageApprovedAt(pipelineOrder: number, departmentId?: number): string {
    return this.getDepartmentApprovalDate(pipelineOrder, departmentId) || '';
  }

  // Calculate time taken relative to previous stage or submission
  getStageTimeTaken(index: number, pipelines: any[]): string {
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    const pipeline = pipelines[index];
    const order = pipeline.intApprovalOrder || (index + 1);
    const entry = this.getStageHistoryEntry(order, pipeline.hrTblDepartment?.serDepartmentId);

    if (!entry?.approvedDate) return '';

    const currentDate = new Date(entry.approvedDate);
    let prevDate: Date;

    if (index === 0) {
      // Compare to submission date
      prevDate = new Date(this.applicationDetails?.dteCreatedDate || this.applicationDetails?.createdAt);
    } else {
      const prevPipeline = pipelines[index - 1];
      const prevOrder = prevPipeline.intApprovalOrder || index;
      const prevEntry = this.getStageHistoryEntry(prevOrder, prevPipeline.hrTblDepartment?.serDepartmentId);
      if (!prevEntry?.approvedDate) return '';
      prevDate = new Date(prevEntry.approvedDate);
    }

    if (isNaN(currentDate.getTime()) || isNaN(prevDate.getTime())) return '';

    const diffMs = currentDate.getTime() - prevDate.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 0) return `${diffDays}d ${diffHours % 24}h`;
    if (diffHours > 0) return `${diffHours}h ${diffMins % 60}m`;
    return `${diffMins}m`;
  }

  // Overall pipeline completion percentage
  getPipelineProgress(): number {
    const pipelines = this.getPipelineData();
    if (!pipelines || pipelines.length === 0) return 0;
    const approved = pipelines.filter((p: any, i: number) =>
      this.getStageStatus(p.intApprovalOrder || (i + 1), p.hrTblDepartment?.serDepartmentId) === 'APPROVED'
    ).length;
    return Math.round((approved / pipelines.length) * 100);
  }

  private getStageDurationMs(index: number, pipelines: any[]): number | null {
    const pipeline = pipelines[index];
    if (!pipeline) return null;
    const order = pipeline.intApprovalOrder || (index + 1);
    const entry = this.getStageHistoryEntry(order, pipeline.hrTblDepartment?.serDepartmentId);
    if (!entry?.approvedDate) return null;

    const endDate = new Date(entry.approvedDate);
    if (isNaN(endDate.getTime())) return null;

    let startDate: Date | null = null;
    if (index === 0) {
      const created = this.applicationDetails?.dteCreatedDate || this.applicationDetails?.createdAt;
      if (created) {
        startDate = new Date(created);
      }
    } else {
      const prevPipeline = pipelines[index - 1];
      const prevOrder = prevPipeline.intApprovalOrder || index;
      const prevEntry = this.getStageHistoryEntry(prevOrder, prevPipeline.hrTblDepartment?.serDepartmentId);
      if (prevEntry?.approvedDate) {
        startDate = new Date(prevEntry.approvedDate);
      }
    }

    if (!startDate || isNaN(startDate.getTime())) return null;
    return endDate.getTime() - startDate.getTime();
  }

  private formatDuration(ms: number): string {
    const mins = Math.floor(ms / 60000);
    const hours = Math.floor(mins / 60);
    const days = Math.floor(hours / 24);
    if (days > 0) return `${days}d ${hours % 24}h`;
    if (hours > 0) return `${hours}h ${mins % 60}m`;
    return `${mins}m`;
  }



  isBudgetApprovalForm(): boolean {
    if (!this.applicationDetails) return false;
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.txtFormCode || '').toUpperCase();
    return name === 'BUDGET APPROVAL FORM' || name.includes('BUDGET APPROVAL') || code.startsWith('BDG') || code.includes('BAF');
  }

  isCapfForm(): boolean {
    if (!this.applicationDetails) return false;
    // Primary: txtFormCode like "CAPF-0083" is the most reliable signal (shown in Application Information)
    const appCode = (this.applicationDetails.txtFormCode || '').toUpperCase();
    if (appCode.includes('CAPF')) return true;
    // Backend-provided flag
    if (this.applicationDetails.isCapfForm === true) return true;
    // Fallback: form name or form's code from cfgTblCustomForm
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.cfgTblCustomForm?.txtFormCode || '').toUpperCase();
    if (name.includes('CAPITAL ASSETS PURCHASE') || name.includes('CAPF') || code.includes('CAPF')) return true;
    const formId = this.applicationDetails.serFormId;
    if (formId && this.forms?.length) {
      const form = this.forms.find((f: any) => f.serFormId === formId);
      const formName = (form?.txtFormName || form?.cfgTblCustomForm?.txtFormName || '').replace(/\s+/g, ' ').toUpperCase();
      const formCode = (form?.txtFormCode || form?.cfgTblCustomForm?.txtFormCode || '').toUpperCase();
      if (formName.includes('CAPITAL ASSETS PURCHASE') || formName.includes('CAPF') || formCode.includes('CAPF')) return true;
    }

    // Backend CAPF lifecycle markers (useful when naming/code conventions differ)
    const status = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    if (status === 'CEO_PENDING' || status === 'ASSET_PENDING') return true;

    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel;
    if (typeof currentLevel === 'number' && currentLevel < 0) return true; // CAPF often starts at -1

    if (this.applicationDetails?.txtAssetCode || this.applicationDetails?.txtPrCode) return true;

    if (Array.isArray(this.approvalHistory) && this.approvalHistory.length > 0) {
      const hasCapfStyleEntry = this.approvalHistory.some((e: any) => {
        const action = (e?.action || e?.status || '').toString().toUpperCase();
        const role = (e?.role || e?.designation || e?.txtDesignation || e?.departmentName || '').toString().toUpperCase();
        return action === 'PR_CODE_ASSIGNED' || role.includes('CEO') || role.includes('FINANCE');
      });
      if (hasCapfStyleEntry) return true;
    }

    return false;
  }

  private isCurrentUserCapfInitialSigner(): boolean {
    const rawSigner = this.applicationFormData?.initial_signer;
    if (rawSigner == null) return false;

    const currentUserId = this.getCurrentUserId();
    const signerText = this.extractSignerName(rawSigner);
    if (!signerText) return false;

    // Support numeric payloads if ever sent as ID.
    const signerAsNumber = Number(signerText);
    if (!isNaN(signerAsNumber) && currentUserId != null) {
      return Number(currentUserId) === signerAsNumber;
    }

    // Mirror backend resolveUserIdByName behavior:
    // trim and drop " (Role)" suffix before exact case-insensitive user-name match.
    const cleanedSigner = signerText.split('(')[0].trim().toLowerCase();
    if (cleanedSigner && currentUserId != null && this.userNameMap && this.userNameMap.size > 0) {
      const matched = Array.from(this.userNameMap.entries()).find(([, name]) =>
        String(name || '').trim().toLowerCase() === cleanedSigner
      );
      if (matched) {
        return Number(currentUserId) === Number(matched[0]);
      }
    }

    const currentNames = [
      this.currentUser?.txtUserName,
      this.currentUser?.userName,
      this.currentUser?.name
    ]
      .filter((v: any) => v != null && String(v).trim() !== '')
      .map((v: any) => String(v).trim().toLowerCase());

    return currentNames.includes(signerText.toLowerCase());
  }

  private extractSignerName(rawSigner: any): string {
    if (rawSigner == null) return '';
    if (typeof rawSigner === 'string' || typeof rawSigner === 'number') {
      return String(rawSigner).trim();
    }
    if (typeof rawSigner === 'object') {
      const name = rawSigner?.txtUserName ?? rawSigner?.userName ?? rawSigner?.name;
      return name != null ? String(name).trim() : '';
    }
    return String(rawSigner).trim();
  }

  private isCurrentUserInitiatorDeptHead(): boolean {
    const userId = this.getCurrentUserId();
    if (!userId || !this.applicationDetails) return false;

    const submitterDeptId =
      this.applicationDetails?.hrTblDepartment?.serDepartmentId ??
      this.applicationDetails?.serDepartmentId ??
      this.applicationDetails?.cfgTblUser?.hrTblDepartment?.serDepartmentId ??
      this.applicationDetails?.cfgTblUser?.serDepartmentId;

    if (submitterDeptId == null) return false;
    const headId = this.departmentHeadMap.get(Number(submitterDeptId));
    return headId != null && Number(headId) === Number(userId);
  }

  /**
   * CAPF tile workflow should be visible when the form is CAPF OR
   * when backend state/history clearly indicates CAPF lifecycle stages.
   */
  shouldShowCapfExtraStages(): boolean {
    if (this.isCapfForm()) return true;

    const status = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    if (status === 'CEO_PENDING' || status === 'ASSET_PENDING') return true;

    const hasAsset = !!(this.applicationDetails?.txtAssetCode && String(this.applicationDetails.txtAssetCode).trim());
    const hasPr = !!(this.applicationDetails?.txtPrCode && String(this.applicationDetails.txtPrCode).trim());
    if (hasAsset || hasPr) return true;

    if (Array.isArray(this.approvalHistory) && this.approvalHistory.length > 0) {
      const hasPrAction = this.approvalHistory.some((e: any) =>
        (e?.action || e?.status || '').toString().toUpperCase() === 'PR_CODE_ASSIGNED'
      );
      if (hasPrAction) return true;
    }

    return false;
  }

  buildFooterFields(source: any): any[] {
    if (!source) return [];

    // Try multiple possible keys for dynamic footer fields
    const dynamicFooter = source.footerFields || source.individual_pipeline_footer || source.field_footer;

    if (Array.isArray(dynamicFooter) && dynamicFooter.length > 0) {
      return dynamicFooter.map((f: any) => ({
        label: f?.label || 'New Field',
        users: Array.isArray(f?.users) ? f.users : []
      }));
    }

    const fieldLabels = source.fieldLabels || {};

    const preparedUsers = Array.isArray(source.preparedByUsers)
      ? source.preparedByUsers
      : (source.preparedBy ? [source.preparedBy] : []);

    const reviewers = Array.isArray(source.reviewers) ? source.reviewers : [];
    const recommenders = Array.isArray(source.recommenders) ? source.recommenders : [];

    const approvers = Array.isArray(source.approvers)
      ? source.approvers
      : (source.approver ? [source.approver] : []);

    const defaults: any[] = [
      { label: fieldLabels.preparedBy || 'Prepared By', users: preparedUsers },
      { label: fieldLabels.reviewedBy || 'Reviewed By', users: reviewers },
      { label: fieldLabels.recommendedBy || 'Recommended By', users: recommenders },
      { label: fieldLabels.approvedBy || 'Approved By', users: approvers }
    ];

    const dynamic: any[] = Array.isArray(source.dynamicUserFields)
      ? source.dynamicUserFields.map((f: any) => ({
        label: f?.label || 'New Field',
        users: Array.isArray(f?.selectedUsers) ? f.selectedUsers : []
      }))
      : [];

    const combined = [...defaults, ...dynamic];

    // For Budget Approval, always show standard stages if we are in BA view,
    // otherwise filter out empty stages for generic forms.
    if (this.isBudgetApprovalForm()) {
      return combined;
    }

    return combined.filter(f => f.users && f.users.length > 0);
  }

  getFooterColSpan(field: any): number {
    if (!field || !Array.isArray(field.users)) return 1;
    return Math.max(field.users.length, 1);
  }

  /** One slot per user per field; deduplicate by userId so send-back + re-approval shows only the latest signature. */
  getFooterSlots(field: any): any[] {
    if (!field || !Array.isArray(field.users) || field.users.length === 0) return [null];
    const seen = new Set<number>();
    const deduped: any[] = [];
    for (const u of field.users) {
      if (!u) continue;
      const uid = u.serUserId ?? u.userId ?? u.id;
      const n = uid != null ? Number(uid) : NaN;
      if (!isNaN(n) && seen.has(n)) continue;
      if (!isNaN(n)) seen.add(n);
      deduped.push(u);
    }
    return deduped.length > 0 ? deduped : [null];
  }

  formatUserForSignature(selectedUsers: any[], index: number): string {
    if (!selectedUsers || !selectedUsers[index]) return '';
    const user = selectedUsers[index];
    const role = this.getUserRoleName(user);
    const dept = this.getUserDepartmentName(user);
    const roleLine = role ? `<br>(${role})` : '';
    const deptLine = dept ? `<br>${dept}` : '';
    return `${user.txtUserName}${roleLine}${deptLine}`;
  }

  private getUserDepartmentName(user: any): string {
    if (!user) return '';
    const directDept = user.hrTblDepartment?.txtDepartmentName || user.departmentName || user.txtDepartmentName || '';
    if (directDept) return directDept;
    const userId = this.getUserId(user);
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return '';
    const entry = this.getLatestApprovalEntryForUser(userId);
    return entry?.departmentName || '';
  }

  private getUserRoleName(user: any): string {
    if (!user) return '';
    return user.cfgTblRole?.txtRoleName || user.roleName || '';
  }

  getUserDepartmentDisplay(user: any): string {
    return this.getUserDepartmentName(user);
  }

  getUserRoleDisplay(user: any): string {
    return this.getUserRoleName(user);
  }

  getUserId(user: any): number | null {
    if (!user) return null;
    const v = user.serUserId ?? user.userId ?? user.id ?? user.approvedBy;
    if (v == null) return null;
    const n = Number(v);
    return isNaN(n) ? null : n;
  }

  /** Get the latest approval entry for a user/role so re-approvals show the most recent signature and timestamp */
  private getLatestApprovalEntryForUser(userId: number, role?: string): any {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return null;
    const hasIndividualPipelineFooter = this.getIndividualPipelineFooterFields().length > 0;
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    const candidates = this.approvalHistory.filter((e: any) => {
      const action = (e.action || '').toString().toUpperCase();
      if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;
      if (hasIndividualPipelineFooter) {
        const entryLevel = e.level || e.intApprovalOrder;
        if (entryLevel != null && entryLevel > currentLevel) return false;
      }
      const entryUserId = e.approvedBy || e.userId;
      if (entryUserId !== userId) return false;
      if (role) {
        const entryRole = (e.role || '').toString().trim();
        if (entryRole.toUpperCase() === 'PREPARED' && role.toUpperCase() !== 'PREPARED') return false;
        if (entryRole && role && entryRole.toUpperCase() !== role.toUpperCase()) return false;
      } else {
        const entryRole = (e.role || '').toString().trim().toUpperCase();
        if (entryRole === 'PREPARED') return false;
      }
      return true;
    });
    if (candidates.length === 0) return null;
    candidates.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    return candidates[0];
  }

  getUserSignatureUrl(user: any, role?: string): string {
    const userId = this.getUserId(user);
    if (!userId) return '';
    if (!this.approvalHistory || this.approvalHistory.length === 0) return '';
    const entry = this.getLatestApprovalEntryForUser(userId, role);
    if (!entry || !entry.signaturePath) return '';
    return `${urls.API_URL}getSignature?userId=${userId}`;
  }

  isUserApproved(user: any, role?: string): boolean {
    const userId = this.getUserId(user);
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return false;
    const entry = this.getLatestApprovalEntryForUser(userId, role);
    if (!entry) return false;
    if (!entry.signaturePath) return false;
    const action = (entry.action || entry.status || '').toString().toUpperCase();
    if (action === 'REJECTED') return false;
    if (action === 'APPROVED') return true;
    return !!entry.approvedDate;
  }

  getUserApprovalDate(user: any, role?: string): string {
    const userId = this.getUserId(user);
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return '';
    const entry = this.getLatestApprovalEntryForUser(userId, role);
    if (!entry || !entry.approvedDate) return '';
    try {
      const dt = new Date(entry.approvedDate);
      if (isNaN(dt.getTime())) return String(entry.approvedDate);
      return dt.toLocaleString();
    } catch (e) {
      return String(entry.approvedDate);
    }
  }

  async generatePdf(download: boolean = true): Promise<Blob | null> {
    this.isGeneratingPdf = true;
    this.pdfBlobUrl = null;

    // Determine which element to capture
    let elementId = '';
    if (this.isCapfForm()) {
      elementId = 'capf-pdf-content';
    } else if (this.isBudgetApprovalForm()) {
      elementId = 'budget-pdf-content';
    } else {
      elementId = 'generic-pdf-content';
    }

    const element = document.getElementById(elementId);
    if (!element) {
      console.error('Content element not found:', elementId);
      this.notificationService.showMessage('Content to generate PDF not found', 'danger');
      this.isGeneratingPdf = false;
      return null;
    }

    const filename = `${this.applicationDetails?.txtFormCode || 'application'}.pdf`;

    try {
      // Dynamically import html2canvas and jsPDF
      const [html2canvasModule, jsPDFModule] = await Promise.all([
        import('html2canvas'),
        import('jspdf')
      ]);

      const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
      const jsPDF = (jsPDFModule.default || jsPDFModule) as any;

      // ── Temporarily strip visual noise before screenshot ──────────────
      const isCapf = this.isCapfForm();
      // Collect all .page elements inside the target and remove their
      // min-height (which adds huge empty space) and border.
      const pageEls = Array.from(element.querySelectorAll('.page')) as HTMLElement[];
      const paperEls = Array.from(element.querySelectorAll('.xyz-paper')) as HTMLElement[];

      // Also strip the element itself if it has a border/min-height
      const savedElementStyles: { el: HTMLElement; minHeight: string; maxHeight: string; border: string; boxShadow: string; overflow: string }[] = [];
      if (!isCapf) {
        savedElementStyles.push(
          ...[...pageEls, ...paperEls, element].map(el => {
            const saved = {
              el,
              minHeight: el.style.minHeight,
              maxHeight: el.style.maxHeight,
              border: el.style.border,
              boxShadow: el.style.boxShadow,
              overflow: el.style.overflow
            };
            el.style.minHeight = 'auto';
            el.style.maxHeight = 'none';
            el.style.border = 'none';
            el.style.boxShadow = 'none';
            el.style.overflow = 'visible';
            return saved;
          })
        );
      }

      const captureTarget = (element.querySelector('.page') as HTMLElement) || element;
      const hadPdfCapture = element.classList.contains('pdf-capture');
      const hadPdfFix = element.classList.contains('pdf-fix');
      if (!hadPdfCapture && !isCapf) {
        element.classList.add('pdf-capture');
      }
      if (!hadPdfFix && isCapf) {
        element.classList.add('pdf-fix');
      }

      const emptyLineSnapshots: { el: HTMLElement; html: string }[] = [];
      const boxcheckSnapshots: { el: HTMLElement; transform: string }[] = [];
      const boxcheckSpanSnapshots: { el: HTMLElement; transform: string }[] = [];
      const sbSubSnapshots: { el: HTMLElement; textAlign: string; width: string; display: string; paddingRight: string; boxSizing: string; marginLeft: string }[] = [];
      if (isCapf) {
        element.querySelectorAll('.line, .date-line, .inline-line').forEach((el) => {
          const ht = el as HTMLElement;
          if ((ht.textContent || '').trim() === '') {
            emptyLineSnapshots.push({ el: ht, html: ht.innerHTML });
            ht.innerHTML = '<span class="pdf-empty">&nbsp;</span>';
          }
        });

        element.querySelectorAll('.boxcheck').forEach((el) => {
          const ht = el as HTMLElement;
          boxcheckSnapshots.push({ el: ht, transform: ht.style.transform });
          ht.style.transform = 'translateY(6px)';
        });
        element.querySelectorAll('.boxcheck > span').forEach((el) => {
          const ht = el as HTMLElement;
          boxcheckSpanSnapshots.push({ el: ht, transform: ht.style.transform });
          ht.style.transform = 'translateY(-6px)';
        });
      }

      // Small timeout so browser repaints before capture
      await new Promise(resolve => setTimeout(resolve, 100));

      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4',
        compress: true
      });
      const pagesForPdf = !isCapf
        ? (Array.from(element.querySelectorAll('.xyz-paper')) as HTMLElement[])
        : [];
      const targets = pagesForPdf.length > 0 ? pagesForPdf : [captureTarget];
      const PDF_WIDTH = 210;
      const PDF_HEIGHT = 297;
      const pxToMm = (px: number) => (px * 25.4) / 96;

      for (let i = 0; i < targets.length; i++) {
        const target = targets[i];
        const rect = target.getBoundingClientRect();
        const contentWidthPx = rect.width || target.scrollWidth;
        const contentHeightPx = rect.height || target.scrollHeight;
        const contentWidthMm = pxToMm(contentWidthPx);
        const contentHeightMm = pxToMm(contentHeightPx);
        const availableWidth = PDF_WIDTH;
        const availableHeight = PDF_HEIGHT;
        const scaleByWidth = availableWidth / contentWidthMm;
        const scaleByHeight = availableHeight / contentHeightMm;
        const finalScale = contentHeightMm * scaleByWidth <= availableHeight ? scaleByWidth : scaleByHeight;
        const imgWidth = contentWidthMm * finalScale;
        const imgHeight = contentHeightMm * finalScale;
        const xOffset = (PDF_WIDTH - imgWidth) / 2;
        const yOffset = (PDF_HEIGHT - imgHeight) / 2;

        const canvas = await html2canvas(target, {
          scale: 3,
          useCORS: true,
          logging: false,
          backgroundColor: '#ffffff',
          width: target.scrollWidth,
          height: target.scrollHeight,
          windowWidth: target.scrollWidth,
          windowHeight: target.scrollHeight
        });

        if (i > 0) {
          pdf.addPage('a4', 'portrait');
        }
        const imgData = canvas.toDataURL('image/jpeg', 0.98);
        pdf.addImage(imgData, 'JPEG', xOffset, yOffset, imgWidth, imgHeight);
      }

      // ── Restore styles after capture ────────────────────────────────────
      savedElementStyles.forEach(({ el, minHeight, maxHeight, border, boxShadow, overflow }) => {
        el.style.minHeight = minHeight;
        el.style.maxHeight = maxHeight;
        el.style.border = border;
        el.style.boxShadow = boxShadow;
        el.style.overflow = overflow;
      });
      if (!hadPdfCapture && !isCapf) {
        element.classList.remove('pdf-capture');
      }
      if (!hadPdfFix && isCapf) {
        element.classList.remove('pdf-fix');
      }
      emptyLineSnapshots.forEach(({ el, html }) => {
        el.innerHTML = html;
      });
      boxcheckSnapshots.forEach(({ el, transform }) => {
        el.style.transform = transform;
      });
      boxcheckSpanSnapshots.forEach(({ el, transform }) => {
        el.style.transform = transform;
      });
      sbSubSnapshots.forEach(({ el, textAlign, width, display, paddingRight, boxSizing, marginLeft }) => {
        el.style.textAlign = textAlign;
        el.style.width = width;
        el.style.display = display;
        el.style.paddingRight = paddingRight;
        el.style.boxSizing = boxSizing;
        el.style.marginLeft = marginLeft;
      });

      const pdfBlob = pdf.output('blob');
      const pdfUrl = URL.createObjectURL(pdfBlob);
      this.pdfBlobUrl = pdfUrl;
      this.isGeneratingPdf = false;

      if (download) {
        const link = document.createElement('a');
        link.href = pdfUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
      }

      return pdfBlob;

    } catch (e) {
      console.error('Error generating PDF:', e);
      this.notificationService.showMessage('Error generating PDF', 'danger');
      this.isGeneratingPdf = false;
      return null;
    }
  }

  hasIndividualPipelineFooter(): boolean {
    return this.getIndividualPipelineFooterFields().length > 0;
  }

  getDynamicApprovalWorkflow(): any[] {
    const fields = this.getIndividualPipelineFooterFields();
    const dynamicNodes: any[] = [];
    
    const initiatorUserId =
      this.applicationDetails?.serSubmittedBy ||
      this.applicationDetails?.cfgTblUser?.serUserId ||
      this.applicationDetails?.serUserId ||
      null;
    const initiatorUserName =
      this.applicationDetails?.cfgTblUser?.txtUserName ||
      this.applicationDetails?.txtSubmittedBy ||
      this.applicationDetails?.submittedByName ||
      this.applicationDetails?.txtUserName ||
      'Initiator';

    if (this.isCapfForm()) {
      dynamicNodes.push({
        isInitiator: true,
        title: 'Initiator',
        user: { serUserId: initiatorUserId, txtUserName: initiatorUserName, userName: initiatorUserName, name: initiatorUserName }
      });
    }
    
    for (const section of fields) {
      const users = this.getIndividualFooterSlots(section);
      for (const slotUser of users) {
        if (!slotUser) continue;
        dynamicNodes.push({
           isInitiator: false,
           title: section.label || 'Approval Stage',
           user: slotUser
        });
      }
    }
    
    return dynamicNodes;
  }

  getDynamicStageStatus(node: any, nodeIndex?: number): string {
    if (node.isInitiator) return 'APPROVED';
    const userId = this.getUserId(node.user);
    if (!userId || !this.approvalHistory) {
      return 'PENDING';
    }
    
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    
    // Determine the node's level in the workflow (0-indexed)
    // If nodeIndex is provided, use it; otherwise, find it from the workflow array
    let workflowLevel: number;
    if (nodeIndex !== undefined && nodeIndex !== null) {
      workflowLevel = nodeIndex;
    } else {
      const workflow = this.getDynamicApprovalWorkflow();
      workflowLevel = workflow.findIndex(n => 
        this.getUserId(n.user) === userId && n.title === node.title
      );
      if (workflowLevel === -1) {
        return 'PENDING';
      }
    }
    
    // If this node is at the current pending level, it should be PENDING
    // (even if there's an old approval entry, because send-back requires re-approval)
    if (workflowLevel === currentLevel) {
      return 'PENDING';
    }
    
    // If this node is after the current level, it's PENDING (future level)
    if (workflowLevel > currentLevel) {
      return 'PENDING';
    }
    
    // If this node is before the current level, check if it was approved
    // Use latest approval entry for this user at this level (so re-approval after send-back shows latest only)
    const candidates = (this.approvalHistory || []).filter((e: any) => {
      const action = (e.action || '').toString().toUpperCase();
      if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;
      const entryLevel = e.level || e.intApprovalOrder;
      if (entryLevel != null) {
        if (entryLevel !== (workflowLevel + 1)) return false;
        if (entryLevel > currentLevel) return false;
      }
      return e.approvedBy === userId || e.userId === userId;
    });
    candidates.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    const entry = candidates.length > 0 ? candidates[0] : null;
    
    // If no entry found for a level that's before current level, it's PENDING (shouldn't happen but safe)
    if (!entry) {
      return 'PENDING';
    }
    
    const action = (entry.action || entry.status || '').toString().toUpperCase();
    if (action === 'REJECTED') return 'REJECTED';
    if (action === 'APPROVED' || !!entry.approvedDate) return 'APPROVED';
    
    return 'PENDING';
  }

  getDynamicStageApproverName(node: any): string {
    if (node.isInitiator) return node.user.txtUserName || node.user.userName || node.user.name || 'Initiator';
    const userId = this.getUserId(node.user);
    if (!userId || !this.approvalHistory) return node.user.txtUserName || node.user.userName || node.user.name || '--';
    const entry = this.getLatestApprovalEntryForUser(userId);
    return entry?.approverName || node.user.txtUserName || node.user.userName || node.user.name || '--';
  }

  getDynamicStageApprovedAt(node: any): string {
    if (node.isInitiator) {
      const d = this.applicationDetails?.dteCreatedDate || this.applicationDetails?.createdAt;
      return d ? new Date(d).toLocaleString() : '--';
    }
    return this.getUserApprovalDate(node.user) || '--';
  }

  getDynamicStageRemarks(node: any): string {
    if (node.isInitiator) return '--';
    const userId = this.getUserId(node.user);
    if (!userId || !this.approvalHistory) return '--';
    const entry = this.getLatestApprovalEntryForUser(userId);
    return entry?.remarks || '--';
  }

  getDynamicStageApprovedVia(node: any): string {
    if (node.isInitiator) {
       return this.applicationDetails?.txtIpAddress ? 'IP:' + this.applicationDetails.txtIpAddress : 'SUBMISSION';
    }
    const userId = this.getUserId(node.user);
    if (!userId || !this.approvalHistory) return '--';
    const entry = this.getLatestApprovalEntryForUser(userId);
    // Prefer actual IP fields over approvedVia (medium like email/system) so "IP Address" label shows IP
    return entry?.approvedIp || entry?.ipAddress || entry?.ip || entry?.approvedVia || '--';
  }

  getDynamicStageTitle(node: any): string {
    if (node.isInitiator) return 'Initiator';
    return node.title || 'Approval Stage';
  }

  getDynamicStageTimeTaken(index: number, nodes: any[]): string {
    if (index === 0) return '--';
    const current = nodes[index];
    const previous = nodes[index - 1];
    
    const currentStr = this.getDynamicStageApprovedAt(current);
    const previousStr = this.getDynamicStageApprovedAt(previous);
    if (currentStr === '--' || previousStr === '--') return '--';
    
    const currDate = new Date(currentStr);
    const prevDate = new Date(previousStr);
    if (isNaN(currDate.getTime()) || isNaN(prevDate.getTime())) return '--';
    
    const diffMs = currDate.getTime() - prevDate.getTime();
    if (diffMs < 0) return '--';
    
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 60) return `${diffMins} min`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) {
      const remMins = diffMins % 60;
      return `${diffHours}h ${remMins}m`;
    }
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} days`;
  }

  getDynamicPipelineProgress(): number {
    const nodes = this.getDynamicApprovalWorkflow();
    if (nodes.length === 0) return 0;
    const approved = nodes.filter(n => this.getDynamicStageStatus(n) === 'APPROVED').length;
    return Math.round((approved / nodes.length) * 100);
  }
}
