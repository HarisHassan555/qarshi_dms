import { Component, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { DepartmentService } from '../../services/department/department.service';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';
import { AbcComponent } from '../../pages/abc/abc.component';
import { urls } from 'src/app/utils/urls';
import { finalize, firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-application-details',
  templateUrl: './application-details.component.html',
  styleUrls: ['./application-details.component.css']
})
export class ApplicationDetailsComponent implements OnInit {
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

    // ONLY the Procurement Department Head can see this button
    const isProcHod = this.isProcurementHod();

    // Status check: only allow editing on PENDING applications
    const rawStatus = (this.applicationDetails?.txtStatus || '').toUpperCase();
    const isPending = rawStatus === 'PENDING' || rawStatus === '' || rawStatus === 'NEW';

    // Debug log — visible in browser console
    console.log('[canEditVendorDetails]', { isProcHod, rawStatus, isPending });

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
      const response: any = await this.http.post(`${urls.API_URL}updateApplication`, updatedApp).toPromise();
      if (response && response.status === 'Success') {
        this.notificationService.showMessage('Vendor details updated successfully', 'success');
        this.applicationDetails.txtApplicationData = updatedApp.txtApplicationData;
        this.applicationFormData = appData;
        this.isEditingVendor = false;
        if (this.vendorEditModal) {
          this.vendorEditModal.close();
        }

        // Refresh local data in ABC component if it exists
        if (this.isCapfForm()) {
          // Trigger any internal refresh needed
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
    private customFormService: CustomFormService,
    private departmentService: DepartmentService,
    private userService: UserService,
    private notificationService: NotificationService,
    private sanitizer: DomSanitizer,
    private http: HttpClient
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
    // Get application ID from route
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.applicationId = idParam ? parseInt(idParam, 10) : null;
      if (this.applicationId && !isNaN(this.applicationId)) {
        this.loadForms();
        this.loadDepartments();
        this.loadApplicationDetails();
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
    const users = Array.isArray(section?.users) ? section.users : [];
    return Math.max(users.length, 1);
  }

  getIndividualFooterSlots(section: any): any[] {
    const users = Array.isArray(section?.users) ? section.users : [];
    return users.length > 0 ? users : [null];
  }

  getIndividualFooterUserLabel(user: any, section: any): string {
    if (!user) return '';
    const name = user.txtUserName || user.userName || user.name || '';
    const role =
      user.cfgTblRole?.txtRoleName ||
      user.txtRoleName ||
      user.roleName ||
      '';
    const roleLine = role ? `<br>(${role})` : '';
    return `${name}${roleLine}`;
  }

  isDocumentHeaderType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'document_header';
  }

  isTableType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'table';
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

  getWordEditorValue(field: any): SafeHtml {
    const value = this.getFieldValue(field);
    if (value === null || value === undefined || value === '') {
      return this.sanitizer.bypassSecurityTrustHtml('<span>-</span>');
    }
    return this.sanitizer.bypassSecurityTrustHtml(this.normalizeWordEditorHtmlForDisplay(String(value)));
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
      el.style.height = '30px';
      el.style.minHeight = '30px';
      el.style.padding = '0 4px';
      el.style.lineHeight = '1';
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
        el.innerHTML = '&nbsp;';
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

  // Check if a department in the pipeline has been approved
  isDepartmentApproved(pipelineOrder: number): boolean {
    if (!this.applicationDetails) return false;
    const currentLevel = this.applicationDetails.intCurrentApprovalLevel || 0;
    return currentLevel >= pipelineOrder;
  }

  // Get remarks for a specific department from approval history
  getDepartmentRemarks(pipelineOrder: number, departmentId?: number): string {
    if (!this.isDepartmentApproved(pipelineOrder)) {
      return '';
    }

    // Find the approval history entry for this department/level
    if (this.approvalHistory && this.approvalHistory.length > 0) {
      // Try to find by both level and departmentId for accuracy
      let historyEntry = null;

      if (departmentId) {
        // First try exact match by both level and departmentId
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.level === pipelineOrder && entry.departmentId === departmentId
        );
      }

      // If not found, try by level only
      if (!historyEntry) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.level === pipelineOrder
        );
      }

      // If still not found and departmentId is provided, try by departmentId only
      if (!historyEntry && departmentId) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.departmentId === departmentId
        );
      }

      if (historyEntry && historyEntry.remarks) {
        return historyEntry.remarks;
      }
    }

    // Fallback: if this is the current level, show current remarks
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    if (pipelineOrder === currentLevel && this.applicationDetails?.txtRemarks) {
      return this.applicationDetails.txtRemarks;
    }

    return 'Approved';
  }

  // Get approval date for a department
  getDepartmentApprovalDate(pipelineOrder: number, departmentId?: number): string {
    if (!this.isDepartmentApproved(pipelineOrder)) {
      return '';
    }

    if (this.approvalHistory && this.approvalHistory.length > 0) {
      // Try to find by both level and departmentId for accuracy
      let historyEntry = null;

      if (departmentId) {
        // First try exact match by both level and departmentId
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.level === pipelineOrder && entry.departmentId === departmentId
        );
      }

      // If not found, try by level only
      if (!historyEntry) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.level === pipelineOrder
        );
      }

      // If still not found and departmentId is provided, try by departmentId only
      if (!historyEntry && departmentId) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.departmentId === departmentId
        );
      }

      if (historyEntry && historyEntry.approvedDate) {
        try {
          const date = new Date(historyEntry.approvedDate);
          return date.toLocaleString();
        } catch (e) {
          return historyEntry.approvedDate;
        }
      }
    }

    return '';
  }

  // Get signature for a department
  getDepartmentSignature(pipelineOrder: number, departmentId?: number): string {
    if (!this.isDepartmentApproved(pipelineOrder)) {
      return '';
    }

    if (this.approvalHistory && this.approvalHistory.length > 0) {
      let historyEntry = null;

      if (departmentId) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.level === pipelineOrder && entry.departmentId === departmentId
        );
      }

      if (!historyEntry) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.level === pipelineOrder
        );
      }

      if (!historyEntry && departmentId) {
        historyEntry = this.approvalHistory.find((entry: any) =>
          entry.departmentId === departmentId
        );
      }

      if (historyEntry && historyEntry.signature) {
        return historyEntry.signature;
      }
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

    if ((!pipelines || !Array.isArray(pipelines) || pipelines.length === 0) && form.txtApprovalPipeline) {
      try {
        pipelines = JSON.parse(form.txtApprovalPipeline);
        console.log('Parsed pipelines from JSON:', pipelines);
      } catch (e) {
        console.error('Error parsing approval pipeline JSON:', e);
        return [];
      }
    }

    if (!pipelines || !Array.isArray(pipelines) || pipelines.length === 0) {
      console.log('No pipelines found or empty array');
      return [];
    }

    const normalized = [...pipelines].filter((p: any) => {
      if (!p) return false;
      const deptId = p.hrTblDepartment?.serDepartmentId || p.serDepartmentId || p.departmentId;
      const deptName =
        p.hrTblDepartment?.txtDepartmentName ||
        p.departmentName ||
        p.txtDepartmentName;
      if (deptName && String(deptName).trim() !== '') return true;
      if (deptId == null) return false;
      // If departments list is loaded, only keep valid departments
      if (this.departmentNameMap && this.departmentNameMap.size > 0) {
        return this.departmentNameMap.has(Number(deptId));
      }
      // Otherwise keep and let enrichment resolve later
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

    if (!hasInitiatorStage) {
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
      if (!hasInitiatorHistory) {
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
    if (!this.fromPendingApprovals || !this.applicationDetails) return false;
    // Always show action buttons from Pending Approvals view
    return true;
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
      return ['APPROVED', 'REJECTED', 'SEND_BACK', 'SENT_BACK', 'SENTBACK'].includes(action) || !!e.approvedDate;
    });
  }

  private isCurrentLevelAlreadyHandled(): boolean {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return false;
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    if (!currentLevel) return false;
    return this.approvalHistory.some((e: any) => {
      if (Number(e.level) !== Number(currentLevel)) return false;
      const action = (e.action || e.status || '').toString().toUpperCase();
      return ['APPROVED', 'REJECTED', 'SEND_BACK', 'SENT_BACK', 'SENTBACK'].includes(action) || !!e.approvedDate;
    });
  }

  @ViewChild('approveModal') approveModal: any;
  @ViewChild('rejectModal') rejectModal: any;
  @ViewChild('sendBackModal') sendBackModal: any;
  @ViewChild('vendorEditModal') vendorEditModal: any;

  openApproveModal() {
    if (!this.applicationDetails?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
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
    this.selectedApplicationForRemarks = this.applicationDetails;
    this.remarksText = '';
    this.rejectModal.open();
  }

  openSendBackModal() {
    if (!this.applicationDetails?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }
    this.selectedApplicationForRemarks = this.applicationDetails;
    this.remarksText = '';
    this.sendBackModal.open();
  }

  async approveApplication() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }

    if (this.isApproving) return;
    this.isApproving = true;

    try {
      const pdfBlob = await this.generatePdf(false);
      if (pdfBlob && this.selectedApplicationForRemarks?.serApplicationId) {
        const filename = `application_${this.applicationDetails?.txtFormCode || this.selectedApplicationForRemarks.serApplicationId}.pdf`;
        const pdfResponse: any = await firstValueFrom(
          this.customFormApplicationService.updateApplicationPdf(
            this.selectedApplicationForRemarks.serApplicationId,
            pdfBlob,
            filename
          )
        );
        if (!pdfResponse || pdfResponse.status !== 'Success') {
          this.notificationService.showMessage(pdfResponse?.message || 'Failed to upload latest form snapshot', 'danger');
          this.isApproving = false;
          return;
        }
      }
    } catch (e) {
      this.notificationService.showMessage('Failed to prepare latest form snapshot', 'danger');
      this.isApproving = false;
      return;
    }

    this.customFormApplicationService.approveApplication(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
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
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to approve application', 'danger');
        }
      },
      (error) => {
        this.notificationService.showMessage('Error approving application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  rejectApplication() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
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
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to reject application', 'danger');
        }
      },
      (error) => {
        this.notificationService.showMessage('Error rejecting application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  sendBackApplication() {
    if (!this.selectedApplicationForRemarks?.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
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
          this.loadApplicationDetails();
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to send back application', 'danger');
        }
      },
      (error) => {
        this.notificationService.showMessage('Error sending back application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  // Get history entry for a given pipeline stage (by order/level)
  getStageHistoryEntry(pipelineOrder: number, departmentId?: number): any {
    if (!this.approvalHistory || this.approvalHistory.length === 0) return null;

    let entry = null;
    if (departmentId) {
      entry = this.approvalHistory.find((e: any) =>
        e.level === pipelineOrder && e.departmentId === departmentId
      );
    }
    if (!entry) {
      entry = this.approvalHistory.find((e: any) => e.level === pipelineOrder);
    }
    if (!entry && departmentId) {
      entry = this.approvalHistory.find((e: any) => e.departmentId === departmentId);
    }
    return entry || null;
  }

  // Get stage status: APPROVED, REJECTED, CURRENT (pending at this level), PENDING
  getStageStatus(pipelineOrder: number, departmentId?: number): string {
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel || 0;
    const overallStatus = (this.applicationDetails?.txtStatus || '').toUpperCase();
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);

    if (entry) {
      const action = (entry.action || entry.status || '').toUpperCase();
      if (action === 'REJECTED') return 'REJECTED';
      if (action === 'APPROVED' || pipelineOrder < currentLevel) return 'APPROVED';
    }
    if (pipelineOrder < currentLevel) return 'APPROVED';
    if (pipelineOrder === currentLevel) {
      return overallStatus === 'REJECTED' ? 'REJECTED' : 'CURRENT';
    }
    return 'PENDING';
  }

  getPipelineDepartmentName(pipeline: any, index: number): string {
    const order = pipeline?.intApprovalOrder || (index + 1);

    // For Budget Approval, show the individual's name as the box title
    if (this.isBudgetApprovalForm()) {
      const personName = this.getCapfUserNameForStage(order);
      if (personName) return personName;
    }

    // For other forms (including CAPF), show the Department Name as requested
    if (!pipeline) return `Department ${index + 1}`;
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
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.txtFormCode || '').toUpperCase();
    return name.includes('CAPITAL ASSETS PURCHASE') || name.includes('CAPF') || code.startsWith('CAPF');
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

  getFooterSlots(field: any): any[] {
    if (!field || !Array.isArray(field.users) || field.users.length === 0) return [null];
    return field.users;
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
    const entry = this.approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
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
    return user.serUserId || user.userId || user.id || null;
  }

  getUserSignatureUrl(user: any): string {
    const userId = this.getUserId(user);
    if (!userId) return '';
    if (!this.approvalHistory || this.approvalHistory.length === 0) return '';
    const entry = this.approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
    if (!entry || !entry.signaturePath) return '';
    return `${urls.API_URL}getSignature?userId=${userId}`;
  }

  isUserApproved(user: any): boolean {
    const userId = this.getUserId(user);
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return false;
    const entry = this.approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
    if (!entry) return false;
    if (!entry.signaturePath) return false;
    const action = (entry.action || entry.status || '').toString().toUpperCase();
    if (action === 'REJECTED') return false;
    if (action === 'APPROVED') return true;
    return !!entry.approvedDate;
  }

  getUserApprovalDate(user: any): string {
    const userId = this.getUserId(user);
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return '';
    const entry = this.approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
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

      const rect = captureTarget.getBoundingClientRect();
      const contentWidthPx = rect.width || captureTarget.scrollWidth;
      const contentHeightPx = rect.height || captureTarget.scrollHeight;
      const pxToMm = (px: number) => (px * 25.4) / 96;
      const contentWidthMm = pxToMm(contentWidthPx);
      const contentHeightMm = pxToMm(contentHeightPx);

      const canvas = await html2canvas(captureTarget, {
        scale: 3,
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff',
        width: captureTarget.scrollWidth,
        height: captureTarget.scrollHeight,
        windowWidth: captureTarget.scrollWidth,
        windowHeight: captureTarget.scrollHeight
      });

      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4',
        compress: true
      });
      const PDF_WIDTH = 210;
      const PDF_HEIGHT = 297;
      const marginX = 0;
      const marginY = 0;
      const availableWidth = PDF_WIDTH - marginX * 2;
      const availableHeight = PDF_HEIGHT - marginY * 2;
      const scaleByWidth = availableWidth / contentWidthMm;
      const scaleByHeight = availableHeight / contentHeightMm;
      const finalScale = contentHeightMm * scaleByWidth <= availableHeight ? scaleByWidth : scaleByHeight;
      const imgWidth = contentWidthMm * finalScale;
      const imgHeight = contentHeightMm * finalScale;
      const xOffset = (PDF_WIDTH - imgWidth) / 2;
      const yOffset = (PDF_HEIGHT - imgHeight) / 2;

      const imgData = canvas.toDataURL('image/jpeg', 0.98);
      pdf.addImage(imgData, 'JPEG', xOffset, yOffset, imgWidth, imgHeight);

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
}
