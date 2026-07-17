import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
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
import {
  FormOrientation,
  formHasOrientationField,
  getA4PaperSizeMm,
  getPreviewPaperOrientationClass,
  getPreviewPaperStyle,
  isLandscapeFormOrientation,
  isOrientationFieldType,
  resolveFormOrientation,
} from 'src/app/utils/form-orientation.util';
import { finalize, firstValueFrom, forkJoin } from 'rxjs';
import { stripEditorTableChromeFromHtml } from 'src/app/utils/word-editor-table.util';
import { capfHasPipelineCeoSignatureSlot } from 'src/app/utils/capf-form.util';
import {
  isDocumentHeaderFieldType,
  resolveDocumentHeaderAddress,
  resolveDocumentHeaderBrandTitle,
  resolveDocumentHeaderLogoPath,
} from 'src/app/utils/document-header.util';
import {
  DOCUMENT_RENDER_A4_LONG_EDGE_MM,
  DOCUMENT_RENDER_A4_SHORT_EDGE_MM,
  DOCUMENT_RENDER_FOOTER_PAGE_FILL_SLACK_PX,
  DOCUMENT_RENDER_PAGE_FILL_LINE_SLACK_PX,
  DOCUMENT_RENDER_PAGE_FIT_SAFETY_PX,
  DOCUMENT_RENDER_TABLE_SPLIT_SAFETY_PX,
  getDocumentRenderPreviewMaxCharsPerLine,
} from 'src/app/utils/document-render.util';

export interface GenericPreviewBlock {
  field: any;
}

export interface GenericPageRenderSegment {
  type: 'word' | 'field';
  html?: SafeHtml;
  block?: GenericPreviewBlock;
}

@Component({
  selector: 'app-application-details',
  templateUrl: './application-details.component.html',
  styleUrls: ['./application-details.component.css', '../application/application.component.css']
})
export class ApplicationDetailsComponent implements OnInit, AfterViewInit, OnDestroy {
  private static readonly MAX_VENDOR_ATTACHMENT_TOTAL_BYTES = 5 * 1024 * 1024;
  private static readonly ALLOWED_VENDOR_ATTACHMENT_MIME_TYPES = new Set([
    'application/pdf',
    'image/webp',
    'image/png',
    'image/jpeg'
  ]);
  private static readonly ALLOWED_VENDOR_ATTACHMENT_EXTENSIONS = new Set(['pdf', 'webp', 'png', 'jpeg', 'jpg']);

  private genericPreviewLayoutSignature = '';
  private genericMeasureRoot: HTMLElement | null = null;
  private cachedMmToPx: number | null = null;
  private previewFitPending = false;
  private previewFitFrame: number | null = null;

  @ViewChild('previewCanvas') previewCanvas?: ElementRef<HTMLElement>;
  @ViewChild('previewScale') previewScale?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasurePaper') genericMeasurePaper?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureHeader') genericMeasureHeader?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureBody') genericMeasureBody?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureFooterSpacer') genericMeasureFooterSpacer?: ElementRef<HTMLElement>;
  @ViewChild('genericMeasureFooter') genericMeasureFooter?: ElementRef<HTMLElement>;
  applicationId: number | null = null;
  applicationDetails: any = null;
  formFields: any[] = [];
  applicationFormData: any = {};
  formOrientation: FormOrientation = 'portrait';
  forms: any[] = [];
  isLoading: boolean = true;
  approvalHistory: any[] = []; // Store approval history with remarks
  priorApprovalHistory: any[] = []; // Persisted historical entries (survive send-back resets)
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
    /** Free-text delivery period (e.g. "30 days"). */
    deliveryPeriod: '',
    /** yyyy-mm-dd for date picker; combined with deliveryPeriod on save for CAPF field. */
    deliveryDate: '',
    termsConditions: ''
  };
  isSavingVendor: boolean = false;
  vendorAttachmentPayloads: Record<string, { fileName: string; mimeType: string; dataUrl: string; base64: string }[]> = {};

  assetCodeInput: string = '';
  isSavingAssetCode: boolean = false;

  prCodeInput: string = '';
  isSavingPrCode: boolean = false;

  poCodeInput: string = '';
  isSavingPoCode: boolean = false;
  /** Set from API data in loadApplicationDetails — avoids template method timing issues. */
  showPoCodeUI = false;
  showAssetCodeUI = false;
  showPrCodeUI = false;

  /** For budget approval form: HTML content split into pages. */
  budgetPages: SafeHtml[] = [];
  /** General forms: field blocks split across A4 pages (no in-page scrollbar). */
  genericPages: GenericPreviewBlock[][] = [];
  /** Plain general forms: line-based pagination to mirror /application preview exactly. */
  genericPreviewLinePages: string[][] = [];
  private genericPreviewUsedLiveMeasure = false;

  hasFeasibilityReport(): boolean {
    return this.getFeasibilityAttachmentsList().length > 0;
  }

  private isFinanceRoleToken(role: string): boolean {
    if (!role) return false;
    return role === 'FINANCE' || role === 'FINANCE_HEAD' || role.includes('FINANCE');
  }

  isFinance(): boolean {
    return this.collectCurrentUserRoleTokens().some((role) => this.isFinanceRoleToken(role));
  }

  private isApplicationSubmitter(): boolean {
    const userId = this.getCurrentUserId();
    if (userId == null || !this.applicationDetails) return false;
    return Number(this.applicationDetails.serSubmittedBy) === Number(userId);
  }

  private hasAssignedAssetCode(): boolean {
    const code = (this.applicationDetails?.txtAssetCode || '').toString().trim();
    if (code) return true;
    if (Array.isArray(this.approvalHistory) && this.approvalHistory.length > 0) {
      return this.approvalHistory.some((e: any) => {
        const action = (e?.action || e?.status || '').toString().toUpperCase();
        return action === 'ASSET_CODE_ASSIGNED' || action === 'ASSET_CODE_CONFIRMED';
      });
    }
    return false;
  }

  private computeShowAssetCodeForm(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;
    if (!this.isFinance()) return false;
    const status = (this.applicationDetails.txtStatus || '').toString().trim().toUpperCase();
    const hasAsset = this.hasAssignedAssetCode();
    const allowed = ['ASSET_PENDING', 'PR_PENDING', 'PO_PENDING', 'APPROVED', 'PO_VENDOR_TE_PENDING', 'CEO_PENDING'];
    if (!allowed.includes(status)) return false;
    if (!hasAsset && status !== 'ASSET_PENDING') return false;
    return true;
  }

  private computeShowPrCodeForm(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;
    if (!this.isApplicationSubmitter()) return false;
    if (!this.hasAssignedAssetCode()) return false;
    const status = (this.applicationDetails.txtStatus || '').toString().trim().toUpperCase();
    const hasPr = !!(this.applicationDetails.txtPrCode && String(this.applicationDetails.txtPrCode).trim());
    const allowed = ['PR_PENDING', 'PO_PENDING', 'APPROVED', 'PO_VENDOR_TE_PENDING', 'CEO_PENDING'];
    if (!allowed.includes(status)) return false;
    if (!hasPr && status !== 'PR_PENDING') return false;
    return true;
  }

  showAssetCodeForm(): boolean {
    return this.showAssetCodeUI;
  }

  showPrCodeForm(): boolean {
    return this.showPrCodeUI;
  }

  /** Normalize role tokens so PO_Approver, PO Approver, and PO_APPROVER all match. */
  private normalizeRoleToken(value: unknown): string {
    return (value ?? '').toString().trim().toUpperCase().replace(/[\s-]+/g, '_');
  }

  private collectCurrentUserRoleTokens(): string[] {
    if (!this.currentUser) return [];
    const u = this.currentUser;
    const tokens = new Set<string>();
    const add = (value: unknown) => {
      const token = this.normalizeRoleToken(value);
      if (token) tokens.add(token);
    };

    if (u.cfgTblRole != null && typeof u.cfgTblRole === 'object') {
      add(u.cfgTblRole.txtRoleName);
      add(u.cfgTblRole.txtRoleCode);
    }

    add(u.txtrole);
    add(u.txtRole);
    add(u.roleName);
    add(u.txt_designation);
    add(u.txtDesignation);

    if (Array.isArray(u.cfgTblUserRoles)) {
      for (const userRole of u.cfgTblUserRoles) {
        add(userRole?.cfgTblRole?.txtRoleName);
        add(userRole?.cfgTblRole?.txtRoleCode);
      }
    }

    return Array.from(tokens);
  }

  private isPoApproverRoleToken(role: string): boolean {
    if (!role) return false;
    return role === 'PO_APPROVER' || (role.includes('PO') && role.includes('APPROVER'));
  }

  isPoApprover(): boolean {
    return this.collectCurrentUserRoleTokens().some((role) => this.isPoApproverRoleToken(role));
  }

  getApplicationPrCode(): string {
    const d = this.applicationDetails;
    if (!d) return '';
    const raw = d.txtPrCode ?? d.txt_pr_code ?? this.prCodeInput ?? '';
    return String(raw).trim();
  }

  private hasAssignedPrCode(): boolean {
    if (this.getApplicationPrCode()) return true;
    if (Array.isArray(this.approvalHistory) && this.approvalHistory.length > 0) {
      return this.approvalHistory.some((e: any) => {
        const action = (e?.action || e?.status || '').toString().toUpperCase();
        return action === 'PR_CODE_ASSIGNED' || action === 'PR_CODE_UPDATED';
      });
    }
    return false;
  }

  private isPoPendingStage(): boolean {
    return (this.applicationDetails?.txtStatus || '').toString().trim().toUpperCase() === 'PO_PENDING';
  }

  /** Original working rules from Pending Approvals → Application Details flow. */
  private computeShowPoCodeForm(): boolean {
    if (!this.applicationDetails) return false;
    if (!this.isCapfForm()) return false;
    if (!this.isPoApprover()) return false;
    if (!this.fromPendingApprovals) return false;
    if (this.isCapfPoEditReapprovalActive()) return false;
    if (this.isCapfPoVendorTeReapprovalActive()) return false;
    const status = (this.applicationDetails.txtStatus || '').toString().trim().toUpperCase();
    const hasPo = !!(this.applicationDetails.txtPoCode && String(this.applicationDetails.txtPoCode).trim());
    const allowed = ['PO_PENDING'];
    if (!allowed.includes(status)) return false;
    if (!this.hasAssignedPrCode()) return false;
    if (hasPo) return false;
    return true;
  }

  private syncFromPendingApprovals(): void {
    this.fromPendingApprovals = this.route.snapshot.queryParamMap.get('from') === 'pending';
  }

  private updateCapfCodeFormVisibility(): void {
    this.showAssetCodeUI = this.computeShowAssetCodeForm();
    this.showPrCodeUI = this.computeShowPrCodeForm();
    this.showPoCodeUI = this.computeShowPoCodeForm();
  }

  /** @deprecated use updateCapfCodeFormVisibility */
  private updatePoCodeVisibility(): void {
    this.updateCapfCodeFormVisibility();
  }

  showPoCodeForm(): boolean {
    return this.showPoCodeUI;
  }

  /** PO approver vendor edit triggered shortened re-approval (HOD → Finance → CEO). */
  private isCapfPoEditReapprovalActive(): boolean {
    if (!this.isCapfForm()) return false;
    const flag = this.applicationFormData?.capfPoEditReapproval;
    const active = flag === true || flag === 'true' || flag === 1 || flag === '1';
    if (!active) return false;
    const status = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    // Only in-flight re-approval (HOD → Finance → CEO). PR/PO_PENDING means re-approval finished or normal flow resumed.
    if (status === 'PR_PENDING' || status === 'PO_PENDING' || status === 'APPROVED') return false;
    return status === 'IN_PROGRESS' || status === 'CEO_PENDING' || status === 'ASSET_PENDING';
  }

  /** Post-PO vendor edit: independent Technical Expert review (outside normal pipeline). */
  private isCapfPoVendorTeReapprovalActive(): boolean {
    if (!this.isCapfForm()) return false;
    return (this.applicationDetails?.txtStatus || '').toString().toUpperCase() === 'PO_VENDOR_TE_PENDING';
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
        (res: any) => {
          if (res?.status === 'Failure') {
            this.notificationService.showMessage(
              String(res?.message || 'Failed to save asset code').replace(/^Failure:\s*/i, ''),
              'danger'
            );
            return;
          }
          this.notificationService.showMessage('Asset code saved successfully', 'success');
          this.loadApplicationDetails();
          this.updateCapfCodeFormVisibility();
        },
        (err) => {
          const msg = err?.error?.message || err?.error || err?.message || 'Failed to save asset code';
          this.notificationService.showMessage(String(msg).replace(/^Failure:\s*/i, ''), 'danger');
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
        (res: any) => {
          if (res?.status === 'Failure') {
            this.notificationService.showMessage(
              String(res?.message || 'Failed to save PR code').replace(/^Failure:\s*/i, ''),
              'danger'
            );
            return;
          }
          this.notificationService.showMessage('PR code saved successfully', 'success');
          this.loadApplicationDetails();
          this.updateCapfCodeFormVisibility();
        },
        (err) => {
          const msg = err?.error?.message || err?.error || err?.message || 'Failed to save PR code';
          this.notificationService.showMessage(String(msg).replace(/^Failure:\s*/i, ''), 'danger');
        }
      );
  }

  savePoCode() {
    if (!this.applicationId || !this.poCodeInput.trim()) {
      this.notificationService.showMessage('PO code is required', 'danger');
      return;
    }
    this.isSavingPoCode = true;
    this.customFormApplicationService
      .assignPoCode(this.applicationId, this.poCodeInput.trim(), this.currentUser?.serUserId)
      .pipe(finalize(() => (this.isSavingPoCode = false)))
      .subscribe(
        (res: any) => {
          if (res?.status === 'Failure') {
            this.notificationService.showMessage(
              String(res?.message || 'Failed to save PO code').replace(/^Failure:\s*/i, ''),
              'danger'
            );
            return;
          }
          this.notificationService.showMessage(
            'PO code saved; application completed. You can still edit vendor details from Approved Applications.',
            'success'
          );
          this.loadApplicationDetails();
          this.updatePoCodeVisibility();
        },
        (err) => {
          const msg = err?.error?.message || err?.error || err?.message || 'Failed to save PO code';
          this.notificationService.showMessage(String(msg).replace(/^Failure:\s*/i, ''), 'danger');
        }
      );
  }

  private getFeasibilityAttachmentsList(): any[] {
    const extractFromData = (data: any): any[] => {
      if (!data || typeof data !== 'object') return [];

      const keys: string[] = [
        'feasibility_report_attached',
        'feasibility_attached_report',
        'FEASIBILITY REPORT ATTACHED'
      ];
      const feasibilityField = (this.formFields || []).find((f: any) => {
        const label = (f?.label || '').toString().toLowerCase();
        return label.includes('feasibility') &&
          (this.isAttachmentType(f?.type) || this.isMultiAttachmentType(f?.type));
      });
      if (feasibilityField) {
        keys.push(this.getFieldName(feasibilityField.label));
        keys.push(feasibilityField.label);
        if (feasibilityField.serFieldId) {
          keys.push(`field_${feasibilityField.serFieldId}`);
        }
      }

      let best: any[] = [];
      for (const key of [...new Set(keys.filter(Boolean))]) {
        const normalized = this.normalizeAttachmentList(data[key]);
        if (normalized.length > best.length) {
          best = normalized;
        }
      }
      return best;
    };

    const fromForm = extractFromData(this.applicationFormData);
    if (fromForm.length) return fromForm;

    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string') {
      try {
        return extractFromData(JSON.parse(raw));
      } catch { }
    }
    return [];
  }

  private getFeasibilityReportValue(): any {
    const attachments = this.getFeasibilityAttachmentsList();
    if (attachments.length > 0) {
      const report = attachments[0];
      this.logFeasibilityValue('list', report);
      return report;
    }
    const fieldValue = this.getFeasibilityFieldValue();
    if (fieldValue) {
      this.logFeasibilityValue('fieldValue', fieldValue);
      if (this.isPreviewableAttachment(fieldValue)) return fieldValue;
      const normalized = this.normalizeAttachmentList(fieldValue);
      if (normalized.length > 0) return normalized[0];
    }
    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string') {
      try {
        const found = this.findFeasibilityAttachmentInData(JSON.parse(raw));
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

  private normalizeAttachmentList(value: any): any[] {
    if (!value) return [];
    if (Array.isArray(value)) return value.filter((item) => item != null && item !== '');
    if (typeof value === 'object') return [value];
    return [];
  }

  private getQuotationAttachmentsValue(): any[] {
    const extractFromData = (data: any): any[] => {
      if (!data || typeof data !== 'object') return [];

      const fromAlias = this.normalizeAttachmentList(data.quotation_attachments);
      if (fromAlias.length) return fromAlias;

      const fieldName = this.getFieldName('Quotation Attachments');
      const fromFieldName = this.normalizeAttachmentList(data[fieldName]);
      if (fromFieldName.length) return fromFieldName;

      const fromLabel = this.normalizeAttachmentList(data['Quotation Attachments']);
      if (fromLabel.length) return fromLabel;

      const quotationField = (this.formFields || []).find((f: any) => {
        const label = (f?.label || '').toString().toLowerCase();
        return label.includes('quotation') &&
          (this.isAttachmentType(f?.type) || this.isMultiAttachmentType(f?.type));
      });
      if (quotationField?.serFieldId) {
        const fromFieldId = this.normalizeAttachmentList(data[`field_${quotationField.serFieldId}`]);
        if (fromFieldId.length) return fromFieldId;
      }
      return [];
    };

    const fromForm = extractFromData(this.applicationFormData);
    if (fromForm.length) return fromForm;

    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string') {
      try {
        return extractFromData(JSON.parse(raw));
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

    const rawStatus = (this.applicationDetails?.txtStatus || '').toUpperCase();

    // PO stage: PO approver or anyone routed from Pending Approvals.
    if (rawStatus === 'PO_PENDING' && (this.isPoApprover() || this.fromPendingApprovals)) {
      return true;
    }

    // Post-PO: PO approver may edit after PO code is assigned.
    if (this.isPoApprover()) {
      const hasPo = !!(this.applicationDetails?.txtPoCode && String(this.applicationDetails.txtPoCode).trim());
      if (rawStatus === 'APPROVED' && hasPo) {
        return true;
      }
    }

    // Pre-PO procurement stage: department head of Procurement (role-agnostic).
    // Must not short-circuit above — users with PO_Approver + Procurement HOD need this path too.
    if (this.isCapfForm() && this.fromPendingApprovals && this.isProcurementHod()) {
      const isPending = rawStatus === 'PENDING' || rawStatus === '' || rawStatus === 'NEW' || rawStatus === 'IN_PROGRESS';
      if (isPending) {
        return true;
      }
    }

    return false;
  }

  /** Split stored "DELIVERY PERIOD & DATE" into text + ISO date when we previously saved as "text / yyyy-mm-dd". */
  private parseVendorDeliveryPeriodAndDate(raw: string): { text: string; date: string } {
    const s = (raw != null ? String(raw) : '').trim();
    if (!s) return { text: '', date: '' };
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return { text: '', date: s };
    const m = s.match(/^(.+?)\s*[/|]\s*(\d{4}-\d{2}-\d{2})$/);
    if (m) return { text: m[1].trim(), date: m[2] };
    return { text: s, date: '' };
  }

  private formatVendorDeliveryPeriodAndDate(text: string, date: string): string {
    const t = (text != null ? String(text) : '').trim();
    const d = (date != null ? String(date) : '').trim();
    if (t && d) return `${t} / ${d}`;
    return t || d || '';
  }

  getVendorEditAttachmentFields(): any[] {
    if (!this.isCapfForm()) return [];
    const fromForm = (this.formFields || []).filter(
      (f: any) => this.isAttachmentType(f?.type) || this.isMultiAttachmentType(f?.type)
    );
    if (fromForm.length > 0) {
      return fromForm;
    }
    return (this.getGenericPreviewFields() || []).filter(
      (f: any) => this.isAttachmentType(f?.type) || this.isMultiAttachmentType(f?.type)
    );
  }

  private getVendorAttachmentPayloadKey(field: any): string {
    const label = (field?.label || '').toString().trim();
    if (label) {
      return this.getFieldName(label);
    }
    return field?.serFieldId ? `field_${field.serFieldId}` : '';
  }

  private getVendorAttachmentStorageKeys(field: any): string[] {
    const keys: string[] = [];
    const label = (field?.label || '').toString().trim();
    if (label) {
      keys.push(this.getFieldName(label));
      keys.push(label);
    }
    if (field?.serFieldId) {
      keys.push(`field_${field.serFieldId}`);
    }
    const labelLower = label.toLowerCase();
    if (labelLower.includes('quotation')) {
      keys.push('quotation_attachments');
    }
    if (labelLower.includes('feasibility')) {
      keys.push('feasibility_report_attached', 'feasibility_attached_report');
    }
    return [...new Set(keys.filter(Boolean))];
  }

  private getVendorAttachmentPayloadsForField(field: any): { fileName: string; mimeType: string; dataUrl: string; base64: string }[] {
    const canonicalKey = this.getVendorAttachmentPayloadKey(field);
    if (canonicalKey && this.vendorAttachmentPayloads[canonicalKey]?.length) {
      return this.vendorAttachmentPayloads[canonicalKey];
    }
    for (const key of this.getVendorAttachmentStorageKeys(field)) {
      const payloads = this.vendorAttachmentPayloads[key];
      if (payloads?.length) {
        return payloads;
      }
    }
    return [];
  }

  getVendorAttachmentFileNames(field: any): string[] {
    return this.getVendorAttachmentPayloadsForField(field).map((p) => p.fileName);
  }

  private normalizeVendorAttachmentPayloads(value: any): { fileName: string; mimeType: string; dataUrl: string; base64: string }[] {
    const list = Array.isArray(value) ? value : (value ? [value] : []);
    const normalized: { fileName: string; mimeType: string; dataUrl: string; base64: string }[] = [];
    for (const item of list) {
      if (!item || typeof item !== 'object') continue;
      const complete = this.ensureVendorAttachmentPayloadComplete(item);
      if (!complete.fileName) continue;
      normalized.push(complete);
    }
    return normalized;
  }

  private ensureVendorAttachmentPayloadComplete(item: any): { fileName: string; mimeType: string; dataUrl: string; base64: string } {
    const fileName = String(item.fileName || item.name || '').trim();
    const mimeType = String(item.mimeType || item.type || '').trim() || 'application/octet-stream';
    let dataUrl = String(item.dataUrl || '').trim();
    let base64 = String(
      item.base64 || item.data || item.content || item.fileBase64 || item.fileData || ''
    ).trim();
    if (!base64 && dataUrl.includes(',')) {
      base64 = dataUrl.split(',', 2)[1] || '';
    }
    if (!dataUrl && base64) {
      dataUrl = `data:${mimeType};base64,${base64}`;
    }
    return { fileName, mimeType, dataUrl, base64 };
  }

  private resolveVendorAttachmentRawValue(field: any): any {
    let best: any = null;
    let bestCount = 0;
    for (const key of this.getVendorAttachmentStorageKeys(field)) {
      const fromForm = this.applicationFormData?.[key];
      const count = this.normalizeAttachmentList(fromForm).length;
      if (count > bestCount) {
        bestCount = count;
        best = fromForm;
      }
    }
    if (bestCount > 0) {
      return best;
    }
    const direct = this.getFieldValue(field);
    const directCount = this.normalizeAttachmentList(direct).length;
    if (directCount > 0) {
      return direct;
    }
    const labelLower = (field?.label || '').toString().toLowerCase();
    if (labelLower.includes('quotation')) {
      const quotations = this.getQuotationAttachmentsValue();
      if (quotations.length > 0) return quotations;
    }
    if (labelLower.includes('feasibility')) {
      const feasibility = this.getFeasibilityAttachmentsList();
      if (feasibility.length > 0) return feasibility;
    }
    return direct;
  }

  private parseApplicationDataForVendorSave(): Record<string, any> {
    let base: Record<string, any> = {};
    const raw = this.applicationDetails?.txtApplicationData;
    if (typeof raw === 'string' && raw.trim()) {
      try {
        let parsed: any = JSON.parse(raw);
        if (typeof parsed === 'string') {
          parsed = JSON.parse(parsed);
        }
        if (parsed?.appData && typeof parsed.appData === 'object') {
          parsed = parsed.appData;
        }
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
          base = { ...parsed };
        }
      } catch {
        base = {};
      }
    } else if (raw && typeof raw === 'object') {
      base = { ...(raw as Record<string, any>) };
    }
    if (this.applicationFormData && typeof this.applicationFormData === 'object') {
      const merged = { ...this.applicationFormData };
      for (const field of this.getVendorEditAttachmentFields()) {
        if (!this.getVendorAttachmentPayloadsForField(field).length) {
          continue;
        }
        for (const key of this.getVendorAttachmentStorageKeys(field)) {
          delete merged[key];
        }
      }
      base = { ...base, ...merged };
    }
    return base;
  }

  private estimateVendorAttachmentBytes(payloads: { base64?: string; dataUrl?: string; data?: string; content?: string }[]): number {
    return (payloads || []).reduce((sum, payload) => {
      const complete = this.ensureVendorAttachmentPayloadComplete(payload);
      const base64 = String(complete.base64 || '').trim();
      return sum + (base64 ? Math.ceil((base64.length * 3) / 4) : 0);
    }, 0);
  }

  private estimateAppDataAttachmentBytes(value: any, seen: WeakSet<object> = new WeakSet()): number {
    if (value == null) return 0;
    if (typeof value === 'object') {
      if (seen.has(value)) return 0;
      seen.add(value);
    }
    if (Array.isArray(value)) {
      return value.reduce((sum, item) => sum + this.estimateAppDataAttachmentBytes(item, seen), 0);
    }
    if (typeof value === 'object') {
      const asPayload = this.ensureVendorAttachmentPayloadComplete(value);
      if (asPayload.base64 || asPayload.dataUrl) {
        return this.estimateVendorAttachmentBytes([asPayload]);
      }
      return Object.values(value).reduce<number>(
        (sum, nested) => sum + this.estimateAppDataAttachmentBytes(nested, seen),
        0
      );
    }
    return 0;
  }

  private validateVendorAttachmentPayloadSizeBeforeSave(appData: Record<string, any>): string | null {
    const totalBytes = this.estimateAppDataAttachmentBytes(appData, new WeakSet());
    if (totalBytes > ApplicationDetailsComponent.MAX_VENDOR_ATTACHMENT_TOTAL_BYTES) {
      return `Combined attachment size (${(totalBytes / (1024 * 1024)).toFixed(2)} MB) exceeds 5 MB.`;
    }
    return null;
  }

  private isAllowedVendorAttachmentFile(file: File): boolean {
    const mime = String(file?.type || '').trim().toLowerCase();
    if (mime && ApplicationDetailsComponent.ALLOWED_VENDOR_ATTACHMENT_MIME_TYPES.has(mime)) {
      return true;
    }
    const name = String(file?.name || '').toLowerCase();
    const dotIndex = name.lastIndexOf('.');
    const ext = dotIndex >= 0 ? name.substring(dotIndex + 1) : '';
    return !!ext && ApplicationDetailsComponent.ALLOWED_VENDOR_ATTACHMENT_EXTENSIONS.has(ext);
  }

  private buildVendorAttachmentPayload(file: File): Promise<{ fileName: string; mimeType: string; dataUrl: string; base64: string }> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = String(reader.result || '');
        const base64 = dataUrl.includes(',') ? dataUrl.split(',', 2)[1] : '';
        resolve({
          fileName: file.name,
          mimeType: file.type || 'application/octet-stream',
          dataUrl,
          base64
        });
      };
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  private getVendorAttachmentBytesWithCandidate(field: any, candidateFiles: File[], includeCurrentField: boolean): number {
    const fieldKeys = new Set([
      this.getVendorAttachmentPayloadKey(field),
      ...this.getVendorAttachmentStorageKeys(field)
    ]);
    const otherBytes = Object.entries(this.vendorAttachmentPayloads).reduce((sum, [key, payloads]) => {
      if (!includeCurrentField && fieldKeys.has(key)) return sum;
      const fieldBytes = this.estimateVendorAttachmentBytes(payloads || []);
      return sum + fieldBytes;
    }, 0);
    const candidateBytes = (candidateFiles || []).reduce((sum, file) => sum + (Number(file?.size) || 0), 0);
    return otherBytes + candidateBytes;
  }

  private loadVendorAttachmentPayloadsFromFormData(): void {
    this.vendorAttachmentPayloads = {};
    for (const field of this.getVendorEditAttachmentFields()) {
      const canonicalKey = this.getVendorAttachmentPayloadKey(field);
      if (!canonicalKey) continue;
      const existing = this.resolveVendorAttachmentRawValue(field);
      const normalized = this.normalizeVendorAttachmentPayloads(existing);
      if (normalized.length) {
        this.vendorAttachmentPayloads[canonicalKey] = normalized.map((p) => this.ensureVendorAttachmentPayloadComplete(p));
      }
    }
  }

  async onVendorAttachmentChange(field: any, event: Event, replaceExisting: boolean = false): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = input?.files ? Array.from(input.files) : [];
    const fieldName = this.getVendorAttachmentPayloadKey(field);

    if (!files.length) {
      input.value = '';
      return;
    }

    const disallowed = files.filter((f) => !this.isAllowedVendorAttachmentFile(f));
    if (disallowed.length > 0) {
      this.notificationService.showMessage('Only PDF, WEBP, PNG, and JPEG files are allowed for attachments.', 'danger');
      input.value = '';
      return;
    }

    const bytesCandidate = this.getVendorAttachmentBytesWithCandidate(field, files, !replaceExisting);
    if (bytesCandidate > ApplicationDetailsComponent.MAX_VENDOR_ATTACHMENT_TOTAL_BYTES) {
      this.notificationService.showMessage(
        `Combined attachment size (${(bytesCandidate / (1024 * 1024)).toFixed(2)} MB) exceeds 5 MB.`,
        'danger'
      );
      input.value = '';
      return;
    }

    try {
      const newPayloads = await Promise.all(
        files.map((file) => this.buildVendorAttachmentPayload(file).then((p) => this.ensureVendorAttachmentPayloadComplete(p)))
      );
      const existingPayloads = replaceExisting ? [] : this.getVendorAttachmentPayloadsForField(field);
      this.vendorAttachmentPayloads[fieldName] = [...existingPayloads, ...newPayloads];
    } catch (err) {
      console.error('Failed generating vendor attachment payload', err);
      this.notificationService.showMessage('Failed to process selected files.', 'danger');
    } finally {
      input.value = '';
    }
  }

  removeVendorAttachment(field: any, indexToRemove: number): void {
    const fieldName = this.getVendorAttachmentPayloadKey(field);
    const existing = [...(this.getVendorAttachmentPayloadsForField(field) || [])];
    if (indexToRemove < 0 || indexToRemove >= existing.length) return;
    existing.splice(indexToRemove, 1);
    if (existing.length) {
      this.vendorAttachmentPayloads[fieldName] = existing;
    } else {
      delete this.vendorAttachmentPayloads[fieldName];
    }
  }

  private formatVendorAttachmentValueForSave(field: any, payloads: { fileName: string; mimeType: string; dataUrl: string; base64: string }[]): any {
    if (this.isMultiAttachmentType(field?.type) || payloads.length > 1) {
      return payloads;
    }
    return payloads[0] || null;
  }

  private applyVendorAttachmentAliases(appData: any, field: any, value: any): void {
    const label = (field?.label || '').toString();
    const fieldName = this.getFieldName(label);
    appData[fieldName] = value;
    appData[label] = value;
    if (field?.serFieldId) {
      appData[`field_${field.serFieldId}`] = value;
    }

    const labelLower = label.toLowerCase();
    if (labelLower.includes('quotation')) {
      const arr = Array.isArray(value) ? value : (value ? [value] : []);
      appData['quotation_attachments'] = arr;
    }
    if (labelLower.includes('feasibility')) {
      const arr = Array.isArray(value) ? value : (value ? [value] : []);
      const stored = (this.isMultiAttachmentType(field?.type) || arr.length > 1) ? arr : (arr[0] || null);
      appData['feasibility_report_attached'] = stored;
      appData['feasibility_attached_report'] = stored;
    }
  }

  private applyVendorAttachmentsToAppData(appData: Record<string, any>): void {
    for (const field of this.getVendorEditAttachmentFields()) {
      const payloads = this.getVendorAttachmentPayloadsForField(field).map((p) =>
        this.ensureVendorAttachmentPayloadComplete(p)
      );
      if (!payloads.length) {
        continue;
      }
      const value = this.formatVendorAttachmentValueForSave(field, payloads);
      this.applyVendorAttachmentAliases(appData, field, value);
    }
  }

  startEditingVendor() {
    const rawDelivery =
      this.getFieldValueByLabel('DELIVERY PERIOD & DATE') || this.getFieldValueByLabel('DELIVERY PERIOD') || '';
    const deliveryParts = this.parseVendorDeliveryPeriodAndDate(rawDelivery);
    this.vendorEditForm = {
      vendorName: this.getFieldValueByLabel('NAME') || this.getFieldValueByLabel('Vendor Name'),
      vendorAddress: this.getFieldValueByLabel('ADDRESS') || this.getFieldValueByLabel('Address'),
      approvedPrice: this.getFieldValueByLabel('APPROVED PRICE') || this.getFieldValueByLabel('Approved price'),
      deliveryPeriod: deliveryParts.text,
      deliveryDate: deliveryParts.date,
      termsConditions: this.getFieldValueByLabel('TERMS & CONDITIONS') || this.getFieldValueByLabel('Terms & Conditions')
    };
    this.loadVendorAttachmentPayloadsFromFormData();
    this.isEditingVendor = true;
    if (this.vendorEditModal) {
      this.vendorEditModal.open();
    }
  }

  cancelEditingVendor() {
    this.isEditingVendor = false;
    this.vendorAttachmentPayloads = {};
    if (this.vendorEditModal) {
      this.vendorEditModal.close();
    }
  }

  async saveVendorDetails() {
    if (!this.applicationDetails) return;
    this.isSavingVendor = true;
    try {
      const appData = this.parseApplicationDataForVendorSave();

      const deliveryCombined = this.formatVendorDeliveryPeriodAndDate(
        this.vendorEditForm.deliveryPeriod,
        this.vendorEditForm.deliveryDate
      );

      // 2. Map labels to keys and update
      const mappings: any = {
        'vendor_name': this.vendorEditForm.vendorName,
        'vendor_address': this.vendorEditForm.vendorAddress,
        'approved_price': this.vendorEditForm.approvedPrice,
        'delivery_period': deliveryCombined,
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
        const value =
          formKey === 'deliveryPeriod' ? deliveryCombined : this.vendorEditForm[formKey as string];
        appData[label] = value;
        const derived = this.getFieldName(label);
        appData[derived] = value;
      }

      // Explicitly update fixed keys used by template
      for (const [key, value] of Object.entries(mappings)) {
        appData[key] = value;
      }

      this.applyVendorAttachmentsToAppData(appData);

      const sizeError = this.validateVendorAttachmentPayloadSizeBeforeSave(appData);
      if (sizeError) {
        this.notificationService.showMessage(sizeError, 'danger');
        return;
      }

      const applicationDataJson = JSON.stringify(appData);
      const payload: any = {
        serApplicationId: this.applicationDetails.serApplicationId,
        serFormId: this.applicationDetails.serFormId,
        txtFormCode: this.applicationDetails.txtFormCode,
        txtApplicationData: applicationDataJson,
        txtStatus: this.applicationDetails.txtStatus,
        intCurrentApprovalLevel: this.applicationDetails.intCurrentApprovalLevel,
        serSubmittedBy: this.applicationDetails.serSubmittedBy,
        blIsActive: this.applicationDetails.blIsActive ?? true,
        blIsDeleted: this.applicationDetails.blIsDeleted ?? false,
        blnStatus: this.applicationDetails.blnStatus ?? true
      };

      const response: any = await firstValueFrom(this.customFormApplicationService.updateApplication(payload));
      if (response && response.status === 'Success') {
        const rawStatus = (this.applicationDetails?.txtStatus || '').toUpperCase();
        const hasPo = !!(this.applicationDetails?.txtPoCode && String(this.applicationDetails.txtPoCode).trim());
        const onPoStage = rawStatus === 'PO_PENDING' || (rawStatus === 'APPROVED' && hasPo);
        let restartMsg = 'Vendor details updated successfully';
        if (this.isPoApprover() && onPoStage) {
          restartMsg = rawStatus === 'APPROVED' && hasPo
            ? 'Vendor details updated. Sent to Technical Expert for approval.'
            : 'Vendor details updated. Application sent for re-approval (Initiator HOD → Finance → CEO).';
        }
        this.notificationService.showMessage(restartMsg, 'success');
        this.applicationDetails.txtApplicationData = applicationDataJson;
        this.applicationFormData = { ...appData };
        this.vendorAttachmentPayloads = {};
        this.isEditingVendor = false;
        if (this.vendorEditModal) {
          this.vendorEditModal.close();
        }

        if (this.isCapfForm() && this.applicationDetails?.serApplicationId) {
          void this.refreshCapfPdfSnapshotAfterVendorUpdate(this.applicationDetails.serApplicationId);
        }
        if (this.isPoApprover() && onPoStage) {
          if (rawStatus === 'APPROVED' && hasPo) {
            this.applicationDetails.txtStatus = 'PO_VENDOR_TE_PENDING';
            this.applicationFormData = { ...appData, capfPoVendorTeReapproval: true };
          } else {
            this.applicationDetails.txtStatus = 'IN_PROGRESS';
            this.applicationDetails.intCurrentApprovalLevel = 0;
            this.applicationFormData = { ...appData, capfPoEditReapproval: true };
          }
          this.router.navigate(['/pending-approvals']);
        }
      } else {
        this.notificationService.showMessage(response?.message || 'Failed to update vendor details', 'danger');
      }
    } catch (error) {
      console.error('Error saving vendor details:', error);
      const msg = (error as any)?.error?.message || (error as any)?.error || (error as any)?.message || 'An error occurred while saving';
      this.notificationService.showMessage(String(msg).replace(/^Failure:\s*/i, ''), 'danger');
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
    // Refresh role from server (localStorage can be stale after role changes)
    this.syncFromPendingApprovals();
    this.http.get(urls.API_URL + 'getCurrentUser').subscribe({
      next: (user: any) => {
        if (user) {
          this.currentUser = user;
          try {
            localStorage.setItem('user', JSON.stringify(user));
          } catch {
            /* ignore quota errors */
          }
          this.updateCapfCodeFormVisibility();
          this.cdr.detectChanges();
        }
      },
      error: () => { /* keep localStorage user */ }
    });
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
    this.route.queryParamMap.subscribe(() => {
      this.syncFromPendingApprovals();
      this.updateCapfCodeFormVisibility();
      this.cdr.detectChanges();
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

    this.syncFromPendingApprovals();
    this.isLoading = true;
    this.customFormApplicationService.getApplicationById(this.applicationId).subscribe(
      (data: any) => {
        if (data) {
          this.applicationDetails = data;

          // If cfgTblCustomForm is null (lazy-load issue or API omission), fetch and attach it so isCapfForm() works
          if (!data.cfgTblCustomForm && data.serFormId) {
            this.customFormService.getById(data.serFormId).subscribe(
              (formData: any) => {
                if (formData) {
                  this.applicationDetails = { ...this.applicationDetails, cfgTblCustomForm: formData };
                  this.updatePoCodeVisibility();
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
          this.syncFormOrientationFromApplication();

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
          if (data.txtPriorApprovals) {
            try {
              this.priorApprovalHistory = JSON.parse(data.txtPriorApprovals);
            } catch (e) {
              console.error('Error parsing prior approvals:', e);
              this.priorApprovalHistory = [];
            }
          } else {
            this.priorApprovalHistory = [];
          }

          this.enrichPipelineWithDepartmentNames();
          this.applyDepartmentNamesToApprovalHistory();

          this.assetCodeInput = (data.txtAssetCode || '').toString().trim();
          this.prCodeInput = (data.txtPrCode || '').toString().trim();
          this.poCodeInput = (data.txtPoCode || '').toString().trim();
          this.updatePoCodeVisibility();

          // If formFields is empty
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
                  this.syncFormOrientationFromApplication();
                  this.cdr.detectChanges();
                  requestAnimationFrame(() => {
                    requestAnimationFrame(() => this.rebuildGenericPreviewPages(true));
                  });
                }
              }
            );
          }

          this.isLoading = false;
          this.updatePoCodeVisibility();
          this.cdr.detectChanges();
          requestAnimationFrame(() => {
            requestAnimationFrame(() => this.rebuildGenericPreviewPages(true));
          });
        } else {
          this.showAssetCodeUI = false;
          this.showPrCodeUI = false;
          this.showPoCodeUI = false;
          this.notificationService.showMessage('Application not found', 'danger');
          this.router.navigate(['/applicationsview']);
        }
      },
      (error) => {
        this.isLoading = false;
        this.showAssetCodeUI = false;
        this.showPrCodeUI = false;
        this.showPoCodeUI = false;
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

  /** Slip footer columns for EXP-* / TAS-* (includes visual-only pipeline from form builder). */
  getSlipPipelineFooterFields(): any[] {
    const footerFields =
      this.applicationFormData?.footerFields ??
      this.applicationFormData?.individual_pipeline_footer ??
      ((this.isExpenseClaimForm() || this.isTemporaryAdvanceSlipForm())
        ? this.applicationFormData?.slipApprovalPipeline
        : undefined);
    if (!Array.isArray(footerFields)) return [];
    return [...footerFields].sort((a: any, b: any) => (Number(a?.order) || 0) - (Number(b?.order) || 0));
  }

  hasSlipPipelineFooter(): boolean {
    return this.getSlipPipelineFooterFields().length > 0;
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
    
    const designationLine = designation ? `<br>${designation}` : '';
    const departmentLine = department ? `<br>${department}` : '';
    
    return `${name}${designationLine}${departmentLine}`;
  }

  isDocumentHeaderType(fieldType: string | undefined): boolean {
    return isDocumentHeaderFieldType(fieldType);
  }

  getDocumentHeaderField(): any | null {
    return (this.formFields || []).find((field: any) => this.isDocumentHeaderType(field?.type)) || null;
  }

  hasDocumentHeaderField(): boolean {
    return !!this.getDocumentHeaderField();
  }

  getDocumentHeaderLogoPath(): string {
    return resolveDocumentHeaderLogoPath(this.getDocumentHeaderField()?.type);
  }

  getDocumentHeaderBrandTitle(): string {
    return resolveDocumentHeaderBrandTitle(this.getDocumentHeaderField()?.type);
  }

  getDocumentHeaderBrandAddress(): string {
    return resolveDocumentHeaderAddress(this.getDocumentHeaderField()?.type);
  }

  isOrientationType(fieldType: string | undefined): boolean {
    return isOrientationFieldType(fieldType);
  }

  hasOrientationField(): boolean {
    return formHasOrientationField(this.formFields);
  }

  isLandscapeOrientation(): boolean {
    return isLandscapeFormOrientation(this.formOrientation);
  }

  getPreviewPaperOrientationClass(): Record<string, boolean> {
    return getPreviewPaperOrientationClass(this.formOrientation);
  }

  getPreviewPaperStyle(): Record<string, string> {
    return getPreviewPaperStyle(this.formOrientation);
  }

  private requestPreviewFit(): void {
    if (typeof window === 'undefined' || this.previewFitPending || !this.previewCanvas || !this.previewScale) {
      return;
    }

    this.previewFitPending = true;
    this.previewFitFrame = window.requestAnimationFrame(() => {
      this.previewFitPending = false;
      this.previewFitFrame = null;
      this.fitPreviewToAvailableSpace();
    });
  }

  private fitPreviewToAvailableSpace(): void {
    this.rebuildGenericPreviewPages();

    const canvasEl = this.previewCanvas?.nativeElement;
    const scaleHostEl = this.previewScale?.nativeElement;
    if (!canvasEl || !scaleHostEl) {
      return;
    }

    const previewContentEl = this.getPreviewContentElement(scaleHostEl);
    if (!previewContentEl) {
      return;
    }

    previewContentEl.style.transform = 'none';
    previewContentEl.style.transformOrigin = 'top left';
    this.applyPreviewPaperDimensions(previewContentEl);

    const horizontalSafeInset = 12;
    const horizontalPadding = 12;
    const availableWidth = Math.max(canvasEl.clientWidth - horizontalSafeInset, 0);
    const renderableWidth = Math.max(availableWidth - horizontalPadding, 0);
    const paperSize = getA4PaperSizeMm(this.formOrientation);
    const naturalWidth = this.mmToPx(paperSize.widthMm);
    const minSheetHeight = this.mmToPx(paperSize.heightMm);
    // Match /application: the preview host should size to the full rendered
    // page stack for both portrait and landscape, otherwise extra landscape
    // pages are clipped while the gray canvas stays at one-sheet height.
    const naturalHeight = Math.max(this.getPreviewContentNaturalHeight(previewContentEl), minSheetHeight);

    if (!renderableWidth || !naturalWidth || !naturalHeight) {
      return;
    }

    const portraitWidthPx = this.mmToPx(DOCUMENT_RENDER_A4_SHORT_EDGE_MM);
    const scale = this.hasOrientationField()
      ? renderableWidth / portraitWidthPx
      : renderableWidth / naturalWidth;
    const safeScale = Number.isFinite(scale) ? Math.max(scale, 0.1) : 1;
    const scaledWidth = naturalWidth * safeScale;
    const scaledHeight = naturalHeight * safeScale;

    scaleHostEl.style.width = `${Math.ceil(scaledWidth + horizontalPadding)}px`;
    scaleHostEl.style.height = `${Math.ceil(scaledHeight)}px`;
    scaleHostEl.style.paddingLeft = '6px';
    scaleHostEl.style.paddingRight = '6px';
    scaleHostEl.style.boxSizing = 'border-box';
    scaleHostEl.style.marginLeft = 'auto';
    scaleHostEl.style.marginRight = 'auto';
    previewContentEl.style.transform = `scale(${safeScale})`;
  }

  private getPreviewContentElement(scaleHostEl: HTMLElement): HTMLElement | null {
    const pages = scaleHostEl.querySelector('.app-preview-pages');
    if (pages instanceof HTMLElement) {
      const papers = pages.querySelectorAll(':scope > .xyz-paper');
      if (papers.length === 1 && papers[0] instanceof HTMLElement) {
        return papers[0] as HTMLElement;
      }
      return pages;
    }
    const selectors = ['.xyz-paper', '.abc-wrapper.embedded .page', '.abc-wrapper .page'];
    for (const selector of selectors) {
      const match = scaleHostEl.querySelector(selector);
      if (match instanceof HTMLElement) {
        return match;
      }
    }
    return null;
  }

  private getLinePreviewPaperPaintExtentHeight(paper: HTMLElement): number {
    const rect = paper.getBoundingClientRect();
    let extentBottom = rect.bottom;
    const lastLine = paper.querySelector('.xyz-preview-line:last-of-type');
    if (lastLine instanceof HTMLElement) {
      extentBottom = Math.max(extentBottom, lastLine.getBoundingClientRect().bottom);
    }
    paper.querySelectorAll('.xyz-footer').forEach((node) => {
      if (node instanceof HTMLElement) {
        extentBottom = Math.max(extentBottom, node.getBoundingClientRect().bottom);
      }
    });
    return Math.ceil(extentBottom - rect.top);
  }

  private applyPreviewPaperDimensions(paper: HTMLElement): void {
    const { widthMm, heightMm } = getA4PaperSizeMm(this.formOrientation);
    const isPagesContainer = paper.classList.contains('app-preview-pages');
    paper.style.boxSizing = 'border-box';
    paper.style.width = `${widthMm}mm`;

    if (isPagesContainer) {
      paper.style.height = 'auto';
      paper.style.minHeight = '0';
      paper.style.maxHeight = 'none';
      paper.style.aspectRatio = '';
      paper.style.overflow = 'visible';
      return;
    }

    if (this.isLandscapeOrientation()) {
      paper.style.height = `${heightMm}mm`;
      paper.style.minHeight = `${heightMm}mm`;
      paper.style.maxHeight = `${heightMm}mm`;
      paper.style.aspectRatio = `${widthMm} / ${heightMm}`;
      paper.style.overflow = 'hidden';
    } else {
      paper.style.height = 'auto';
      paper.style.minHeight = `${heightMm}mm`;
      paper.style.maxHeight = 'none';
      paper.style.aspectRatio = '';
      paper.style.overflow = 'visible';
    }
  }

  private getPreviewContentNaturalHeight(el: HTMLElement): number {
    if (el.classList.contains('xyz-paper') && el.querySelector('.xyz-preview-line')) {
      const paintExtent = this.getLinePreviewPaperPaintExtentHeight(el);
      const base = Math.max(el.scrollHeight || 0, el.offsetHeight || 0, paintExtent);
      return Math.max(base, 1);
    }

    const base = Math.max(el.scrollHeight || 0, el.offsetHeight || 0);
    const children = el.children;
    if (!children.length) {
      return Math.max(base, 1);
    }

    let sumHeights = 0;
    for (let i = 0; i < children.length; i++) {
      const child = children[i] as HTMLElement;
      sumHeights += Math.max(child.offsetHeight, child.getBoundingClientRect().height);
    }
    if (children.length > 1) {
      const cs = window.getComputedStyle(el);
      const gapRaw = cs.rowGap || cs.columnGap || cs.gap || '0px';
      const gapPx = parseFloat(gapRaw) || 0;
      sumHeights += gapPx * (children.length - 1);
    }

    const last = children[children.length - 1] as HTMLElement;
    const stackedBottom = last.offsetTop + last.offsetHeight;
    return Math.max(base, sumHeights, stackedBottom, 1);
  }

  private syncFormOrientationFromApplication(): void {
    this.formOrientation = resolveFormOrientation(this.applicationFormData, this.formFields);
    this.rebuildGenericPreviewPages();
  }

  ngAfterViewInit(): void {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.rebuildGenericPreviewPages(true);
        this.requestPreviewFit();
      });
    });
  }

  ngAfterViewChecked(): void {
    if (!this.genericPreviewUsedLiveMeasure && this.canUseLiveGenericMeasure()) {
      this.rebuildGenericPreviewPages();
    }
    this.requestPreviewFit();
  }

  ngOnDestroy(): void {
    if (this.previewFitFrame !== null && typeof window !== 'undefined') {
      window.cancelAnimationFrame(this.previewFitFrame);
      this.previewFitFrame = null;
    }
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    this.requestPreviewFit();
  }

  getGenericPreviewPages(): string[][] {
    if (!this.useLineBasedGenericPagination()) {
      return [[]];
    }
    return this.genericPreviewLinePages.length ? this.genericPreviewLinePages : [[]];
  }

  private rebuildGenericPreviewPages(force = false): void {
    if (
      this.isBudgetApprovalForm() ||
      this.isCapfForm() ||
      this.isExpenseClaimForm() ||
      this.isTemporaryAdvanceSlipForm()
    ) {
      return;
    }
    const fields = this.getBodyPreviewFields();
    const signature = this.buildGenericPreviewLayoutSignature(fields);
    const hasLiveMeasure = this.canUseLiveGenericMeasure();

    if (this.useLineBasedGenericPagination()) {
      if (
        !force &&
        signature === this.genericPreviewLayoutSignature &&
        this.genericPreviewLinePages.length > 0 &&
        (!hasLiveMeasure || this.genericPreviewUsedLiveMeasure)
      ) {
        return;
      }

      if (!hasLiveMeasure && !force) {
        return;
      }

      this.buildGenericLinePages(fields);
      this.genericPreviewLayoutSignature = signature;
      this.genericPreviewUsedLiveMeasure = hasLiveMeasure;
      this.cdr.markForCheck();
      this.requestPreviewFit();
      return;
    }

    if (
      !force &&
      signature === this.genericPreviewLayoutSignature &&
      this.genericPages.length > 0 &&
      (!hasLiveMeasure || this.genericPreviewUsedLiveMeasure)
    ) {
      return;
    }
    if (!hasLiveMeasure && !force) {
      return;
    }
    this.buildGenericPages();
    this.genericPreviewLayoutSignature = signature;
    this.genericPreviewUsedLiveMeasure = hasLiveMeasure;
    this.cdr.markForCheck();
    this.requestPreviewFit();
  }

  private buildGenericPreviewLayoutSignature(fields: any[]): string {
    const fieldValues = fields.map((field: any) => {
      const key = String(field?.serFieldId || field?.label || '');
      const value = this.getFieldValue(field);
      if (value === null || value === undefined) {
        return `${key}:`;
      }
      if (typeof value === 'string') {
        return `${key}:${value.length}:${value}`;
      }
      if (typeof value === 'number' || typeof value === 'boolean') {
        return `${key}:${String(value)}`;
      }
      try {
        return `${key}:${JSON.stringify(value)}`;
      } catch {
        return `${key}:${String(value)}`;
      }
    });

    const footerSignature = JSON.stringify(this.getIndividualPipelineFooterFields() || []);
    return [
      this.formOrientation,
      this.getGenericDocumentHeading(),
      fieldValues.join('|'),
      footerSignature
    ].join('::');
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
        // Footer fields are rendered once at the end of the document
        (String(f?.type || '').toLowerCase().replace(/\s+/g, '_') !== 'footer') &&
        !this.isIndividualPipelineFooterType(f?.type)
    );
  }

  useLineBasedGenericPagination(): boolean {
    return false;
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

  private getWordEditorHtml(field: any): string {
    const raw =
      field && typeof field === 'object' && typeof field._chunkHtml === 'string'
        ? field._chunkHtml
        : this.decodeHtmlEntitiesIfNeeded(String(this.getFieldValue(field) ?? ''));
    return this.normalizeWordEditorHtmlForDisplay(raw);
  }

  getWordEditorValue(field: any): SafeHtml {
    const html = this.getWordEditorHtml(field);
    if (!html) {
      return this.sanitizer.bypassSecurityTrustHtml('<span>-</span>');
    }
    return this.sanitizer.bypassSecurityTrustHtml(html);
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

  /** Split general form body fields across A4 pages using in-component DOM measurement. */
  buildGenericPages(): void {
    if (typeof document === 'undefined') {
      this.genericPages = [[]];
      this.genericPreviewLinePages = [[]];
      return;
    }

    const fields = this.getBodyPreviewFields();
    if (this.useLineBasedGenericPagination()) {
      this.buildGenericLinePages(fields);
      return;
    }

    const landscape = this.isLandscapeOrientation();
    const hasFooter = this.hasIndividualPipelineFooter();
    const blocks = this.collectAtomicGenericBlocks(fields, landscape);

    this.genericPreviewLinePages = [];
    this.genericPages = this.canUseLiveGenericMeasure()
      ? this.paginateGenericBlocksSequentially(blocks, hasFooter)
      : this.packMeasuredBlocksIntoPages(blocks, landscape, hasFooter);
    if (!this.genericPages.length) {
      this.genericPages = [[]];
    }

    const packedCount = this.countGenericBlocks(this.genericPages);
    if (packedCount !== blocks.length && this.canUseLiveGenericMeasure()) {
      this.genericPages = this.paginateGenericBlocksSequentially(blocks, hasFooter);
    }
  }

  private buildGenericLinePages(fields: any[]): void {
    const lines = this.buildGenericPreviewFlatLines(fields);
    const hasFooter = this.hasIndividualPipelineFooter();
    const hasLiveMeasure = this.canUseLiveGenericMeasure();

    if (!hasLiveMeasure) {
      this.genericPreviewLinePages = [lines.length ? lines : ['-']];
      this.genericPreviewUsedLiveMeasure = false;
      this.genericPages = this.genericPreviewLinePages.map(() => []);
      return;
    }

    this.genericPreviewLinePages = this.paginateGenericPreviewLines(lines.length ? lines : ['-'], hasFooter);
    if (!this.genericPreviewLinePages.length) {
      this.genericPreviewLinePages = [['-']];
    }
    this.genericPreviewUsedLiveMeasure = true;
    this.genericPages = this.genericPreviewLinePages.map(() => []);
  }

  /** One block per word-editor fragment / table row-group so packing can place each on a page. */
  private collectAtomicGenericBlocks(fields: any[], landscape: boolean): GenericPreviewBlock[] {
    const blocks: GenericPreviewBlock[] = [];
    for (const field of fields) {
      blocks.push(...this.expandFieldToAtomicBlocks(field, landscape));
    }
    return blocks;
  }

  private expandFieldToAtomicBlocks(field: any, landscape: boolean): GenericPreviewBlock[] {
    if (this.isWordEditorType(field?.type) || this.isHtmlPreviewField(field)) {
      const value = this.getFieldValue(field);
      const decoded = this.decodeHtmlEntitiesIfNeeded(String(value ?? ''));
      const html = this.normalizeWordEditorHtmlForDisplay(decoded);
      if (!html.trim()) {
        return [];
      }
      return this.extractWordEditorHtmlFragments(html).map((fragment) => ({
        field: { ...field, _chunkHtml: fragment },
      }));
    }

    if (this.isTableType(field?.type)) {
      return this.splitTableIntoMeasuredBlocks(field, landscape);
    }

    const value = this.getFieldValue(field);
    if (value === null || value === undefined || value === '') {
      return [];
    }
    return [{ field }];
  }

  private canUseLiveGenericMeasure(): boolean {
    return !!(
      this.genericMeasurePaper?.nativeElement &&
      this.genericMeasureBody?.nativeElement
    );
  }

  private configureLiveMeasurePaper(pageIndex: number, showFooter: boolean): void {
    const paper = this.genericMeasurePaper?.nativeElement;
    const header = this.genericMeasureHeader?.nativeElement;
    const footerSpacer = this.genericMeasureFooterSpacer?.nativeElement;
    const footer = this.genericMeasureFooter?.nativeElement;

    if (paper) {
      const { widthMm, heightMm } = getA4PaperSizeMm(this.formOrientation);
      paper.style.setProperty('width', `${widthMm}mm`, 'important');
      paper.style.setProperty('min-width', `${widthMm}mm`, 'important');
      paper.style.setProperty('max-width', `${widthMm}mm`, 'important');
      paper.style.setProperty('height', `${heightMm}mm`, 'important');
      paper.style.setProperty('min-height', `${heightMm}mm`, 'important');
      paper.style.setProperty('max-height', `${heightMm}mm`, 'important');
      paper.style.setProperty('overflow', 'hidden', 'important');
      paper.classList.toggle('xyz-paper--continuation', pageIndex > 0);
      paper.classList.toggle('xyz-last-page', showFooter);
      paper.classList.toggle('xyz-paper--footer-pinned', showFooter);
    }

    if (header) {
      header.style.display = pageIndex === 0 ? '' : 'none';
    }
    if (footer) {
      footer.style.display = showFooter ? '' : 'none';
    }
    if (footerSpacer) {
      footerSpacer.style.display = showFooter ? '' : 'none';
    }

  }

  private hasTableLikePreviewBlock(blocks: GenericPreviewBlock[]): boolean {
    return (blocks || []).some((block) => {
      if (Array.isArray(block?.field?._tableRows)) {
        return true;
      }
      const chunkHtml = block?.field?._chunkHtml;
      return typeof chunkHtml === 'string' && this.isWordEditorTableFragment(chunkHtml);
    });
  }

  private getTableFitSafetyPx(blocks: GenericPreviewBlock[]): number {
    return this.hasTableLikePreviewBlock(blocks)
      ? DOCUMENT_RENDER_TABLE_SPLIT_SAFETY_PX
      : 0;
  }

  private getPageFitAllowancePx(
    clientHeight: number,
    showFooter: boolean
  ): number {
    const slack = showFooter
      ? DOCUMENT_RENDER_FOOTER_PAGE_FILL_SLACK_PX
      : DOCUMENT_RENDER_PAGE_FILL_LINE_SLACK_PX;
    return Math.max(0, clientHeight - DOCUMENT_RENDER_PAGE_FIT_SAFETY_PX + slack);
  }

  /** Measure laid-out content height (more reliable than scrollHeight for grid-clipped bodies). */
  private measureRenderedBodyContentPx(body: HTMLElement): number {
    if (!body.childElementCount) {
      return 0;
    }
    const linePageContainer = body.querySelector('.xyz-generic-content--line-pages') as HTMLElement | null;
    if (linePageContainer) {
      const bodyTop = body.getBoundingClientRect().top;
      const paddingBottom = parseFloat(window.getComputedStyle(body).paddingBottom) || 0;
      const lines = Array.from(linePageContainer.querySelectorAll('.xyz-preview-line')) as HTMLElement[];
      const lastVisibleLine = [...lines].reverse().find((line) => {
        const text = (line.textContent || '').replace(/\u00a0/g, '').trim();
        return text !== '' || line.getBoundingClientRect().height > 0;
      }) || lines[lines.length - 1];

      if (lastVisibleLine) {
        const lastLineRect = lastVisibleLine.getBoundingClientRect();
        return Math.ceil(Math.max(0, lastLineRect.bottom - bodyTop) + paddingBottom);
      }
    }
    const bodyTop = body.getBoundingClientRect().top;
    let bottom = bodyTop;
    for (const child of Array.from(body.children) as HTMLElement[]) {
      const rect = child.getBoundingClientRect();
      bottom = Math.max(bottom, rect.bottom);
      bottom = Math.max(bottom, rect.top + child.scrollHeight);
      for (const descendant of Array.from(child.querySelectorAll('*')) as HTMLElement[]) {
        const descendantRect = descendant.getBoundingClientRect();
        bottom = Math.max(bottom, descendantRect.bottom);
        bottom = Math.max(bottom, descendantRect.top + descendant.scrollHeight);
      }
    }
    const paddingBottom = parseFloat(window.getComputedStyle(body).paddingBottom) || 0;
    return Math.ceil(bottom - bodyTop + paddingBottom);
  }

  private renderBlocksIntoLiveMeasureBody(blocks: GenericPreviewBlock[]): void {
    const body = this.genericMeasureBody?.nativeElement;
    if (!body) {
      return;
    }
    body.innerHTML = this.renderPageBlocksMeasureHtml(blocks);
  }

  /** Match getPageRenderSegments: one ql-editor per consecutive word-editor run on the page. */
  private renderPageBlocksMeasureHtml(blocks: GenericPreviewBlock[]): string {
    const parts: string[] = [];
    let wordHtmlParts: string[] = [];

    const flushWord = () => {
      if (!wordHtmlParts.length) {
        return;
      }
      parts.push(
        `<div class="xyz-generic-field"><div class="xyz-generic-word xyz-word-preview"><div class="ql-editor">${wordHtmlParts.join('')}</div></div></div>`
      );
      wordHtmlParts = [];
    };

    for (const block of blocks) {
      if (this.isWordEditorType(block.field?.type) || this.isHtmlPreviewField(block.field)) {
        wordHtmlParts.push(this.getWordEditorHtml(block.field));
      } else {
        flushWord();
        parts.push(this.renderBlockMeasureHtml(block));
      }
    }
    flushWord();
    return parts.join('');
  }

  private forceLiveMeasureLayout(): void {
    const paper = this.genericMeasurePaper?.nativeElement;
    const body = this.genericMeasureBody?.nativeElement;
    if (paper) {
      void paper.offsetHeight;
    }
    if (body) {
      void body.offsetHeight;
    }
  }

  private getVisibleElementOuterHeightPx(element?: HTMLElement | null): number {
    if (!element) {
      return 0;
    }
    const style = window.getComputedStyle(element);
    if (style.display === 'none' || style.visibility === 'hidden') {
      return 0;
    }
    const rect = element.getBoundingClientRect();
    const marginTop = parseFloat(style.marginTop) || 0;
    const marginBottom = parseFloat(style.marginBottom) || 0;
    return rect.height + marginTop + marginBottom;
  }

  private getLiveBodyBudgetPx(pageIndex: number, showFooter: boolean): number {
    const paper = this.genericMeasurePaper?.nativeElement;
    const header = this.genericMeasureHeader?.nativeElement;
    const body = this.genericMeasureBody?.nativeElement;
    const footer = this.genericMeasureFooter?.nativeElement;
    if (!body) {
      const landscape = this.isLandscapeOrientation();
      return this.getPageContentBudgetPx(landscape, pageIndex === 0, showFooter);
    }
    this.configureLiveMeasurePaper(pageIndex, showFooter);
    body.innerHTML = '';
    this.forceLiveMeasureLayout();

    const liveBodyHeight = body.clientHeight || body.getBoundingClientRect().height || 0;
    if (liveBodyHeight > 0) {
      return Math.max(48, Math.floor(liveBodyHeight));
    }

    if (paper) {
      const paperHeight = paper.clientHeight || paper.getBoundingClientRect().height || 0;
      const paperStyle = window.getComputedStyle(paper);
      const paperVerticalPadding =
        (parseFloat(paperStyle.paddingTop) || 0) +
        (parseFloat(paperStyle.paddingBottom) || 0);
      const headerHeight = pageIndex === 0 ? this.getVisibleElementOuterHeightPx(header) : 0;
      const footerHeight = showFooter ? this.getVisibleElementOuterHeightPx(footer) : 0;
      const bodyStyle = window.getComputedStyle(body);
      const bodyMargins =
        (parseFloat(bodyStyle.marginTop) || 0) +
        (parseFloat(bodyStyle.marginBottom) || 0);
      const available = Math.floor(
        paperHeight - paperVerticalPadding - headerHeight - footerHeight - bodyMargins
      );
      if (available > 0) {
        return Math.max(48, available);
      }
    }

    return Math.max(48, body.clientHeight);
  }

  private blocksFitLiveMeasurePage(
    blocks: GenericPreviewBlock[],
    pageIndex: number,
    showFooter: boolean
  ): boolean {
    if (!this.canUseLiveGenericMeasure()) {
      const landscape = this.isLandscapeOrientation();
      return this.blocksFitMeasuredPage(blocks, landscape, pageIndex, showFooter);
    }

    const availableBodyBudget = this.getLiveBodyBudgetPx(pageIndex, showFooter);
    this.configureLiveMeasurePaper(pageIndex, showFooter);
    this.renderBlocksIntoLiveMeasureBody(blocks);
    this.forceLiveMeasureLayout();
    const body = this.genericMeasureBody!.nativeElement;

    if (!blocks.length) {
      const emptyBudget = this.getPageFitAllowancePx(availableBodyBudget, showFooter);
      return !showFooter || this.getLiveFooterHeightPx() <= emptyBudget + 2;
    }

    if (body.clientHeight <= 0) {
      const landscape = this.isLandscapeOrientation();
      const total = blocks.reduce((sum, block) => sum + this.measureBlockHeight(block, landscape), 0);
      return total <= this.getPageContentBudgetPx(landscape, pageIndex === 0, showFooter);
    }

    const allowance = this.getPageFitAllowancePx(availableBodyBudget, showFooter);
    const usedHeight = Math.max(body.scrollHeight, this.measureRenderedBodyContentPx(body));
    return usedHeight <= allowance;
  }

  private buildGenericPreviewFlatLines(fields: any[]): string[] {
    const lines: string[] = [];
    const maxChars = this.getPreviewMaxCharsPerLine();

    for (const field of fields) {
      if (this.isWordEditorType(field?.type)) {
        const raw = String(this.getFieldValue(field) ?? '');
        lines.push(...this.wordEditorHtmlToPlainLines(raw));
        lines.push('');
        continue;
      }

      if (this.isTableType(field?.type)) {
        const rows = this.getTableData(field);
        if (!rows.length) {
          lines.push('-');
        } else {
          rows.forEach((row: any[], rowIndex: number) => {
            const rowLabel = this.getTableRowLabel(field, rowIndex);
            const rowLine = `${rowLabel} | ${row.map((cell) => cell ?? '-').join(' | ')}`;
            lines.push(...this.wrapPlainLineToSegments(rowLine, maxChars));
          });
        }
        lines.push('');
        continue;
      }

      const display = this.formatFieldValue(field, this.getFieldValue(field));
      const parts = String(display).split(/\r?\n/);
      parts.forEach((part) => {
        const trimmed = part.trimEnd();
        if (trimmed === '') {
          lines.push('');
        } else {
          lines.push(...this.wrapPlainLineToSegments(trimmed, maxChars));
        }
      });
      lines.push('');
    }

    while (lines.length && lines[lines.length - 1] === '') {
      lines.pop();
    }
    return lines.length ? lines : ['-'];
  }

  private paginateGenericPreviewLines(lines: string[], hasFooter: boolean): string[][] {
    if (!lines.length) {
      return [['-']];
    }

    const pages: string[][] = [];
    let currentPage: string[] = [];

    const flush = () => {
      if (currentPage.length) {
        pages.push([...currentPage]);
        currentPage = [];
      }
    };

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      const trial = [...currentPage, line];
      const pageIndex = pages.length;
      const isLastLine = i === lines.length - 1;
      const reserveFooter = hasFooter && isLastLine;
      const fitsWithoutFooter = this.previewLinesFitPage(trial, pageIndex, false);
      const fitsWithFooter = !reserveFooter || this.previewLinesFitPage(trial, pageIndex, true);

      if (currentPage.length > 0 && (!fitsWithoutFooter || !fitsWithFooter)) {
        flush();
        currentPage = [line];
        continue;
      }

      currentPage = trial;
    }

    flush();
    if (!pages.length) {
      return [['-']];
    }
    return hasFooter ? this.trimPreviewLinePagesForFooter(pages) : pages;
  }

  private trimPreviewLinePagesForFooter(pages: string[][]): string[][] {
    const result = pages.map((page) => [...page]);
    let preserveTrailingEmpty = false;
    let guard = 0;

    while (guard++ < 300 && result.length) {
      const lastPageIndex = result.length - 1;
      const lastPage = result[lastPageIndex];
      if (this.previewLinesFitPage(lastPage, lastPageIndex, true)) {
        break;
      }
      if (lastPage.length <= 1) {
        if (!this.previewLinesFitPage(lastPage, lastPageIndex, false)) {
          break;
        }
        result.push([]);
        preserveTrailingEmpty = true;
        break;
      }
      const moved = lastPage.pop();
      if (moved === undefined) {
        break;
      }
      if (!result[lastPageIndex + 1]) {
        result.push([]);
      }
      result[lastPageIndex + 1].unshift(moved);
    }

    if (preserveTrailingEmpty && result.length > 0) {
      while (result.length > 1 && result[result.length - 2].length === 0) {
        result.splice(result.length - 2, 1);
      }
      return result;
    }
    return result.filter((page) => page.length > 0);
  }

  private previewLinesFitPage(lines: string[], pageIndex: number, showFooter: boolean): boolean {
    if (!this.canUseLiveGenericMeasure()) {
      return true;
    }

    const availableBodyBudget = this.getLiveBodyBudgetPx(pageIndex, showFooter);
    this.configureLiveMeasurePaper(pageIndex, showFooter);
    this.renderPreviewLinesIntoLiveMeasureBody(lines);
    this.forceLiveMeasureLayout();

    const body = this.genericMeasureBody?.nativeElement;
    if (!body) {
      return true;
    }

    const hasLinePageContainer = !!body.querySelector('.xyz-generic-content--line-pages');
    const usedHeight = hasLinePageContainer
      ? this.measureRenderedBodyContentPx(body)
      : Math.max(body.scrollHeight, this.measureRenderedBodyContentPx(body));
    const allowance = Math.max(0, availableBodyBudget - DOCUMENT_RENDER_PAGE_FIT_SAFETY_PX);
    return usedHeight <= allowance;
  }

  private renderPreviewLinesIntoLiveMeasureBody(lines: string[]): void {
    const body = this.genericMeasureBody?.nativeElement;
    if (!body) {
      return;
    }
    body.innerHTML = `
      <div class="xyz-generic-content--line-pages">
        ${lines.map((line) => `<div class="xyz-preview-line">${line ? this.escapeHtml(line) : '&nbsp;'}</div>`).join('')}
      </div>
    `;
  }

  /** Merge consecutive word-editor blocks on a page so tables/paragraphs render without gaps or clipping. */
  getPageRenderSegments(pageFields: GenericPreviewBlock[]): GenericPageRenderSegment[] {
    const segments: GenericPageRenderSegment[] = [];
    let wordHtmlParts: string[] = [];

    const flushWord = () => {
      if (!wordHtmlParts.length) {
        return;
      }
      segments.push({
        type: 'word',
        html: this.sanitizer.bypassSecurityTrustHtml(wordHtmlParts.join('')),
      });
      wordHtmlParts = [];
    };

    for (const block of pageFields || []) {
      if (this.isWordEditorType(block.field?.type) || this.isHtmlPreviewField(block.field)) {
        wordHtmlParts.push(this.getWordEditorHtml(block.field));
      } else {
        flushWord();
        segments.push({ type: 'field', block });
      }
    }
    flushWord();
    return segments;
  }

  isWordEditorRenderSegment(segment: GenericPageRenderSegment): boolean {
    return segment.type === 'word';
  }

  isFieldRenderSegment(segment: GenericPageRenderSegment): boolean {
    return segment.type === 'field' && !!segment.block;
  }

  /**
   * Pack each page with the maximum number of consecutive blocks that fit.
   * Only overflow goes to the next page — avoids leaving large gaps on non-last pages.
   */
  private packMaxFillGenericBlocks(
    blocks: GenericPreviewBlock[],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    if (!blocks.length) {
      return hasFooter ? [[]] : [[]];
    }

    const pages: GenericPreviewBlock[][] = [];
    let start = 0;

    while (start < blocks.length) {
      const pageIndex = pages.length;
      const remaining = blocks.length - start;
      let best = start;

      for (let count = 1; count <= remaining; count++) {
        const trial = blocks.slice(start, start + count);
        const isLastPage = start + count === blocks.length;
        const showFooter = hasFooter && isLastPage;

      if (!this.blocksFitLiveMeasurePage(trial, pageIndex, showFooter)) {
          const candidateIndex = start + count - 1;
          const existingBlocks = blocks.slice(start, candidateIndex);
          if (existingBlocks.length > 0) {
            const split = this.trySplitBlockIntoCurrentPage(
              blocks[candidateIndex],
              existingBlocks,
              pageIndex,
              showFooter
            );
            if (split && split.length > 1) {
              const firstPieceTrial = [...existingBlocks, split[0]];
              if (this.blocksFitLiveMeasurePage(firstPieceTrial, pageIndex, false)) {
                blocks.splice(candidateIndex, 1, ...split);
                best = candidateIndex + 1;
              }
            }
          }
          break;
        }
        best = start + count;
      }

      if (best === start) {
        let block = blocks[start];
        const showFooter = hasFooter && blocks.length === start + 1;
        const split = this.trySplitOverflowBlock(block, pageIndex, showFooter);
        if (split && split.length > 1) {
          blocks.splice(start, 1, ...split);
          continue;
        }
        best = start + 1;
      }

      pages.push(
        blocks.slice(start, best).map((block) => ({
          field: { ...block.field },
        }))
      );
      start = best;
    }

    if (!pages.length) {
      return [[]];
    }

    return this.postProcessPackedPages(pages, blocks.length, hasFooter);
  }

  /**
   * Match /application live pagination behavior exactly: place blocks in order
   * and flush to a new page as soon as the next block no longer fits.
   * This is more conservative than max-fill packing and avoids clipping content
   * into a single landscape page on application-details.
   */
  private paginateGenericBlocksSequentially(
    sourceBlocks: GenericPreviewBlock[],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    if (!sourceBlocks.length) {
      return hasFooter ? [[]] : [[]];
    }

    const blocks = sourceBlocks.map((block) => ({ field: { ...block.field } }));
    const pages: GenericPreviewBlock[][] = [];
    let currentPage: GenericPreviewBlock[] = [];
    let index = 0;
    let guard = 0;

    const flushPage = () => {
      pages.push(currentPage.map((block) => ({ field: { ...block.field } })));
      currentPage = [];
    };

    while (index < blocks.length && guard++ < 4000) {
      const pageIndex = pages.length;
      const block = blocks[index];
      const isLastBlock = index === blocks.length - 1;
      const showFooterIfPlacedHere = hasFooter && isLastBlock;

      if (!currentPage.length && !this.blocksFitLiveMeasurePage([block], pageIndex, showFooterIfPlacedHere)) {
        const split = this.trySplitOverflowBlock(block, pageIndex, showFooterIfPlacedHere);
        if (split && split.length > 1) {
          blocks.splice(index, 1, ...split);
          continue;
        }
      }

      const trial = [...currentPage, block];
      const fitsWithoutFooter = this.blocksFitLiveMeasurePage(trial, pageIndex, false);
      const fitsWithFooter =
        !showFooterIfPlacedHere || this.blocksFitLiveMeasurePage(trial, pageIndex, true);

      if (fitsWithoutFooter && fitsWithFooter) {
        currentPage = trial;
        index += 1;
        continue;
      }

      if (currentPage.length > 0) {
        const split = this.trySplitBlockIntoCurrentPage(block, currentPage, pageIndex, showFooterIfPlacedHere);
        if (split && split.length > 1) {
          blocks.splice(index, 1, ...split);
          continue;
        }
        flushPage();
        continue;
      }

      const split = this.trySplitOverflowBlock(block, pageIndex, showFooterIfPlacedHere);
      if (split && split.length > 1) {
        blocks.splice(index, 1, ...split);
        continue;
      }

      currentPage = [block];
      index += 1;
      flushPage();
    }

    if (currentPage.length) {
      flushPage();
    }

    if (!pages.length) {
      return hasFooter ? [[]] : [[]];
    }

    return hasFooter
      ? this.compactTrailingEmptyPagesPreserveFooterSlot(this.trimLastPageForFooter(pages), true)
      : this.compactTrailingEmptyPages(pages);
  }

  private getRemainingLiveMeasureAllowancePx(
    pageBlocks: GenericPreviewBlock[],
    pageIndex: number,
    showFooter: boolean
  ): number {
    const landscape = this.isLandscapeOrientation();
    if (!this.canUseLiveGenericMeasure()) {
      const budget = this.getPageContentBudgetPx(landscape, pageIndex === 0, showFooter);
      const used = pageBlocks.reduce((sum, block) => sum + this.measureBlockHeight(block, landscape), 0);
      return Math.max(0, budget - used);
    }

    const availableBodyBudget = this.getLiveBodyBudgetPx(pageIndex, showFooter);
    this.configureLiveMeasurePaper(pageIndex, showFooter);
    this.renderBlocksIntoLiveMeasureBody(pageBlocks);
    this.forceLiveMeasureLayout();
    const body = this.genericMeasureBody?.nativeElement;
    if (!body) {
      return 0;
    }
    const allowance = this.getPageFitAllowancePx(availableBodyBudget, showFooter);
    const usedHeight = Math.max(body.scrollHeight, this.measureRenderedBodyContentPx(body));
    return Math.max(0, allowance - usedHeight);
  }

  private trySplitBlockIntoCurrentPage(
    block: GenericPreviewBlock,
    currentPageBlocks: GenericPreviewBlock[],
    pageIndex: number,
    showFooter = false
  ): GenericPreviewBlock[] | null {
    const landscape = this.isLandscapeOrientation();

    if (Array.isArray(block?.field?._tableRows)) {
      const split = this.canUseLiveGenericMeasure()
        ? this.splitTableRowsBlockForLivePage(block.field, currentPageBlocks, pageIndex, showFooter)
        : this.splitTableRowsBlockForCurrentPage(
            block.field,
            landscape,
            this.getRemainingLiveMeasureAllowancePx(currentPageBlocks, pageIndex, showFooter)
          );
      return split.length > 1 ? split : null;
    }

    const chunkHtml = block?.field?._chunkHtml;
    if (typeof chunkHtml !== 'string') {
      return null;
    }

    if (this.isWordEditorTableFragment(chunkHtml)) {
      const split = this.canUseLiveGenericMeasure()
        ? this.splitHtmlTableFragmentForLivePage(
            block.field,
            chunkHtml,
            currentPageBlocks,
            pageIndex,
            showFooter
          )
        : this.splitHtmlTableFragmentForCurrentPage(
            block.field,
            chunkHtml,
            landscape,
            this.getRemainingLiveMeasureAllowancePx(currentPageBlocks, pageIndex, showFooter)
          );
      return split.length > 1 ? split : null;
    }

    const remainingBudget = this.getRemainingLiveMeasureAllowancePx(currentPageBlocks, pageIndex, showFooter);
    if (remainingBudget < 24) {
      return null;
    }

    const split = this.splitWordEditorIntoMeasuredBlocks(
      block.field,
      chunkHtml,
      landscape,
      showFooter,
      remainingBudget
    );
    return split.length > 1 ? split : null;
  }

  private splitTableRowsBlockForCurrentPage(
    field: any,
    landscape: boolean,
    budget: number
  ): GenericPreviewBlock[] {
    const rows = Array.isArray(field?._tableRows) ? field._tableRows : this.getTableData(field);
    if (!rows.length) {
      return [];
    }

    let keepCount = 0;
    for (let i = 0; i < rows.length; i++) {
      const trialRows = rows.slice(0, i + 1);
      const trialBlock: GenericPreviewBlock = { field: { ...field, _tableRows: trialRows } };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);
      if (trialHeight > budget) {
        break;
      }
      keepCount = i + 1;
    }

    if (keepCount <= 0 || keepCount >= rows.length) {
      return [{ field: { ...field, _tableRows: rows } }];
    }

    return [
      { field: { ...field, _tableRows: rows.slice(0, keepCount) } },
      { field: { ...field, _tableRows: rows.slice(keepCount) } },
    ];
  }

  private splitTableRowsBlockForLivePage(
    field: any,
    pageBlocks: GenericPreviewBlock[],
    pageIndex: number,
    showFooter: boolean
  ): GenericPreviewBlock[] {
    const rows = Array.isArray(field?._tableRows) ? field._tableRows : this.getTableData(field);
    if (rows.length <= 1) {
      return rows.length ? [{ field: { ...field, _tableRows: rows } }] : [];
    }

    let keepCount = 0;
    for (let i = 0; i < rows.length; i++) {
      const trialBlock: GenericPreviewBlock = { field: { ...field, _tableRows: rows.slice(0, i + 1) } };
      if (!this.blocksFitLiveMeasurePage([...pageBlocks, trialBlock], pageIndex, showFooter)) {
        break;
      }
      keepCount = i + 1;
    }

    if (keepCount <= 0 || keepCount >= rows.length) {
      return [{ field: { ...field, _tableRows: rows } }];
    }

    return [
      { field: { ...field, _tableRows: rows.slice(0, keepCount) } },
      { field: { ...field, _tableRows: rows.slice(keepCount) } },
    ];
  }

  private splitHtmlTableFragmentForCurrentPage(
    field: any,
    tableHtml: string,
    landscape: boolean,
    budget: number
  ): GenericPreviewBlock[] {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = tableHtml;
    const table = wrapper.querySelector('table');
    if (!table) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    const bodyRows = Array.from(table.querySelectorAll('tbody tr'));
    const rows = bodyRows.length ? bodyRows : Array.from(table.querySelectorAll('tr'));
    if (rows.length <= 1) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    const hasThead = !!table.querySelector('thead');
    let keepCount = 0;
    for (let i = 0; i < rows.length; i++) {
      const trialHtml = this.buildWordEditorTableFromRows(table, rows.slice(0, i + 1), hasThead);
      const trialBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: trialHtml } };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);
      if (trialHeight > budget) {
        break;
      }
      keepCount = i + 1;
    }

    if (keepCount <= 0 || keepCount >= rows.length) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    return [
      { field: { ...field, _chunkHtml: this.buildWordEditorTableFromRows(table, rows.slice(0, keepCount), hasThead) } },
      { field: { ...field, _chunkHtml: this.buildWordEditorTableFromRows(table, rows.slice(keepCount), hasThead) } },
    ];
  }

  private splitHtmlTableFragmentForLivePage(
    field: any,
    tableHtml: string,
    pageBlocks: GenericPreviewBlock[],
    pageIndex: number,
    showFooter: boolean
  ): GenericPreviewBlock[] {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = tableHtml;
    const table = wrapper.querySelector('table');
    if (!table) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    const bodyRows = Array.from(table.querySelectorAll('tbody tr'));
    const rows = bodyRows.length ? bodyRows : Array.from(table.querySelectorAll('tr'));
    if (rows.length <= 1) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    const hasThead = !!table.querySelector('thead');
    let keepCount = 0;
    for (let i = 0; i < rows.length; i++) {
      const trialHtml = this.buildWordEditorTableFromRows(table, rows.slice(0, i + 1), hasThead);
      const trialBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: trialHtml } };
      if (!this.blocksFitLiveMeasurePage([...pageBlocks, trialBlock], pageIndex, showFooter)) {
        break;
      }
      keepCount = i + 1;
    }

    if (keepCount <= 0 || keepCount >= rows.length) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    return [
      { field: { ...field, _chunkHtml: this.buildWordEditorTableFromRows(table, rows.slice(0, keepCount), hasThead) } },
      { field: { ...field, _chunkHtml: this.buildWordEditorTableFromRows(table, rows.slice(keepCount), hasThead) } },
    ];
  }

  private postProcessPackedPages(
    pages: GenericPreviewBlock[][],
    expectedBlockCount: number,
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    let result = pages.map((page) => page.map((block) => ({ field: { ...block.field } })));

    if (hasFooter) {
      const trimmed = this.trimLastPageForFooter(result);
      if (this.countGenericBlocks(trimmed) === expectedBlockCount) {
        result = trimmed;
      }
    }

    const densified = this.densifyGenericPages(result);
    if (this.countGenericBlocks(densified) === expectedBlockCount) {
      result = densified;
    }

    const filled = this.fillNonLastPagesGreedy(result);
    if (this.countGenericBlocks(filled) === expectedBlockCount) {
      result = filled;
    }

    const groupedLeadIn = this.keepLeadInTextWithFollowingTable(result, hasFooter);
    if (this.countGenericBlocks(groupedLeadIn) === expectedBlockCount) {
      result = groupedLeadIn;
    }

    if (this.countGenericBlocks(result) !== expectedBlockCount) {
      return pages.map((page) => page.map((block) => ({ field: { ...block.field } })));
    }

    return hasFooter
      ? this.compactTrailingEmptyPagesPreserveFooterSlot(result, true)
      : this.compactTrailingEmptyPages(result);
  }

  /**
   * If pagination leaves a tiny trailing last page that only exists because the
   * footer was reserved too conservatively, collapse that final page back into
   * the previous page when the previous page can hold all remaining content plus
   * the footer.
   */
  private mergeLastFooterPageBackIfItFits(
    pages: GenericPreviewBlock[][]
  ): GenericPreviewBlock[][] {
    if (pages.length < 2) {
      return pages.map((page) => [...page]);
    }

    const result = pages.map((page) => [...page]);
    const lastIdx = result.length - 1;
    const prevIdx = lastIdx - 1;
    const lastPage = result[lastIdx];
    const prevPage = result[prevIdx];

    // Table fragments are too sensitive to be re-packed across the footer merge.
    // Keeping the split stable avoids the distorted "pulled from previous page"
    // layout seen after adding a footer below pasted tables.
    if (this.hasTableLikePreviewBlock(prevPage) || this.hasTableLikePreviewBlock(lastPage)) {
      return result;
    }

    if (!lastPage.length) {
      return this.compactTrailingEmptyPagesPreserveFooterSlot(result, true);
    }

    const merged = [...prevPage, ...lastPage];
    const fitsMerged = this.canUseLiveGenericMeasure()
      ? this.blocksFitLiveMeasurePage(merged, prevIdx, true)
      : this.blocksFitMeasuredPage(merged, this.isLandscapeOrientation(), prevIdx, true);

    if (!fitsMerged) {
      return result;
    }

    result[prevIdx] = merged;
    result.splice(lastIdx, 1);
    return this.compactTrailingEmptyPagesPreserveFooterSlot(result, true);
  }

  private keepLeadInTextWithFollowingTable(
    pages: GenericPreviewBlock[][],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    if (pages.length < 2) {
      return pages.map((page) => [...page]);
    }

    const result = pages.map((page) => [...page]);
    const useLiveMeasure = this.canUseLiveGenericMeasure();
    const landscape = this.isLandscapeOrientation();

    for (let i = 0; i < result.length - 1; i++) {
      const currentPage = result[i];
      const nextPage = result[i + 1];
      if (currentPage.length <= 1 || !nextPage.length) {
        continue;
      }

      const leadingTableBlock = nextPage[0];
      if (!this.isWordEditorTableBlock(leadingTableBlock)) {
        continue;
      }

      const leadInStart = this.findTrailingWordEditorLeadInStart(currentPage, leadingTableBlock);
      if (leadInStart < 0) {
        continue;
      }

      const trailingTextBlocks = currentPage.slice(leadInStart);
      if (!trailingTextBlocks.length) {
        continue;
      }

      const nextPageIndex = i + 1;
      const showFooter = hasFooter && nextPageIndex === result.length - 1;
      const groupedNextPage = [...trailingTextBlocks, ...nextPage];
      const fitsGrouped = useLiveMeasure
        ? this.blocksFitLiveMeasurePage(groupedNextPage, nextPageIndex, showFooter)
        : this.blocksFitMeasuredPage(groupedNextPage, landscape, nextPageIndex, showFooter);

      if (!fitsGrouped) {
        continue;
      }

      currentPage.splice(leadInStart, trailingTextBlocks.length);
      nextPage.unshift(...trailingTextBlocks);
    }

    return hasFooter
      ? this.compactTrailingEmptyPagesPreserveFooterSlot(result, true)
      : this.compactTrailingEmptyPages(result);
  }

  private isWordEditorTextBlock(block: GenericPreviewBlock | null | undefined): boolean {
    const chunkHtml = block?.field?._chunkHtml;
    return typeof chunkHtml === 'string'
      && (this.isWordEditorType(block?.field?.type) || this.isHtmlPreviewField(block?.field))
      && !this.isWordEditorTableFragment(chunkHtml);
  }

  private isWordEditorTableBlock(block: GenericPreviewBlock | null | undefined): boolean {
    const chunkHtml = block?.field?._chunkHtml;
    return typeof chunkHtml === 'string'
      && (this.isWordEditorType(block?.field?.type) || this.isHtmlPreviewField(block?.field))
      && this.isWordEditorTableFragment(chunkHtml);
  }

  private areBlocksFromSameWordEditorField(
    first: GenericPreviewBlock | null | undefined,
    second: GenericPreviewBlock | null | undefined
  ): boolean {
    const firstField = first?.field;
    const secondField = second?.field;
    if (!firstField || !secondField) {
      return false;
    }

    if (firstField.serFieldId && secondField.serFieldId) {
      return firstField.serFieldId === secondField.serFieldId;
    }

    return this.getFieldName(String(firstField.label || '')) === this.getFieldName(String(secondField.label || ''))
      && String(firstField.type || '').toLowerCase() === String(secondField.type || '').toLowerCase();
  }

  private findTrailingWordEditorLeadInStart(
    pageBlocks: GenericPreviewBlock[],
    tableBlock: GenericPreviewBlock | null | undefined
  ): number {
    if (!pageBlocks.length || !tableBlock) {
      return -1;
    }

    let start = -1;
    for (let i = pageBlocks.length - 1; i >= 0; i--) {
      const candidate = pageBlocks[i];
      if (!this.isWordEditorTextBlock(candidate) || !this.areBlocksFromSameWordEditorField(candidate, tableBlock)) {
        break;
      }
      start = i;
    }

    return start;
  }

  /**
   * Pull as many blocks as possible from page i+1 onto page i before starting a new page.
   * Uses binary search so non-last pages are filled to the true live-measure limit.
   */
  private fillNonLastPagesGreedy(pages: GenericPreviewBlock[][]): GenericPreviewBlock[][] {
    if (!this.canUseLiveGenericMeasure()) {
      return pages.map((page) => [...page]);
    }

    const expected = this.countGenericBlocks(pages);
    let result = pages.map((page) => [...page]);
    let changed = true;
    let guard = 0;

    while (changed && guard++ < 40) {
      changed = false;

      for (let i = 0; i < result.length - 1; i++) {
        const nextPage = result[i + 1];
        if (!nextPage?.length) {
          continue;
        }

        let lo = 0;
        let hi = nextPage.length;
        while (lo < hi) {
          const mid = Math.ceil((lo + hi) / 2);
          const trial = [...result[i], ...nextPage.slice(0, mid)];
          if (this.blocksFitLiveMeasurePage(trial, i, false)) {
            lo = mid;
          } else {
            hi = mid - 1;
          }
        }

        if (lo > 0) {
          result[i] = [...result[i], ...nextPage.slice(0, lo)];
          result[i + 1] = nextPage.slice(lo);
          if (!result[i + 1].length) {
            result.splice(i + 1, 1);
          }
          changed = true;
        }
      }
    }

    result = this.compactTrailingEmptyPages(result);
    if (this.countGenericBlocks(result) !== expected) {
      return pages.map((page) => [...page]);
    }
    return result;
  }

  private getLiveFooterHeightPx(): number {
    const footer = this.genericMeasureFooter?.nativeElement;
    if (!footer) {
      return 0;
    }
    const prevDisplay = footer.style.display;
    footer.style.display = '';
    const height = footer.offsetHeight;
    footer.style.display = prevDisplay;
    return height;
  }

  private paginateGenericContentBlocks(
    blocks: GenericPreviewBlock[],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    if (!blocks.length) {
      return hasFooter ? [[]] : [[]];
    }

    const pages: GenericPreviewBlock[][] = [];
    let current: GenericPreviewBlock[] = [];

    const flush = () => {
      if (current.length) {
        pages.push(current);
        current = [];
      }
    };

    for (let i = 0; i < blocks.length; i++) {
      let block = blocks[i];
      const pageIndex = pages.length;
      const isLastBlock = i === blocks.length - 1;
      const reserveFooter = hasFooter && isLastBlock;

      if (!this.blocksFitLiveMeasurePage([block], pageIndex, false)) {
        const split = this.trySplitOverflowBlock(block, pageIndex, reserveFooter);
        if (split && split.length > 1) {
          blocks.splice(i, 1, ...split);
          block = blocks[i];
        }
      }

      const trial = [...current, block];

      if (current.length > 0) {
        const fitsWithoutFooter = this.blocksFitLiveMeasurePage(trial, pageIndex, false);
        const fitsWithFooter =
          !hasFooter || !isLastBlock || this.blocksFitLiveMeasurePage(trial, pageIndex, true);

        if (!fitsWithoutFooter || !fitsWithFooter) {
          flush();
          current = [block];
        } else {
          current = trial;
        }
      } else {
        current = [block];
      }
    }
    flush();

    if (!pages.length) {
      return [[]];
    }

    return hasFooter
      ? this.compactTrailingEmptyPagesPreserveFooterSlot(this.trimLastPageForFooter(pages), true)
      : this.compactTrailingEmptyPages(pages);
  }

  private countGenericBlocks(pages: GenericPreviewBlock[][]): number {
    return pages.reduce((sum, page) => sum + page.length, 0);
  }

  private compactTrailingEmptyPages(pages: GenericPreviewBlock[][]): GenericPreviewBlock[][] {
    const result = pages.map((page) => [...page]);
    while (result.length && !result[result.length - 1].length) {
      result.pop();
    }
    return result;
  }

  private compactTrailingEmptyPagesPreserveFooterSlot(
    pages: GenericPreviewBlock[][],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    const result = pages.map((page) => [...page]);
    while (result.length && !result[result.length - 1].length) {
      result.pop();
    }
    if (hasFooter && pages.length > 0 && pages[pages.length - 1].length === 0) {
      result.push([]);
    }
    return result;
  }

  /** Repack from a flat block list — guarantees every block appears on exactly one page. */
  private repackGenericBlocksFromFlat(
    blocks: GenericPreviewBlock[],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    if (!blocks.length) {
      return hasFooter ? [[]] : [[]];
    }

    const pages: GenericPreviewBlock[][] = [];
    let current: GenericPreviewBlock[] = [];

    const flush = () => {
      if (current.length) {
        pages.push([...current]);
        current = [];
      }
    };

    for (let i = 0; i < blocks.length; i++) {
      const block = blocks[i];
      const trial = [...current, block];
      const pageIndex = pages.length;
      const isLastBlock = i === blocks.length - 1;

      if (current.length > 0) {
        const fitsWithoutFooter = this.blocksFitLiveMeasurePage(trial, pageIndex, false);
        const fitsWithFooter =
          !hasFooter || !isLastBlock || this.blocksFitLiveMeasurePage(trial, pageIndex, true);

        if (!fitsWithoutFooter || !fitsWithFooter) {
          flush();
          current = [block];
        } else {
          current = trial;
        }
      } else {
        current = [block];
      }
    }
    flush();

    if (!pages.length) {
      return [[]];
    }

    let result = hasFooter ? this.trimLastPageForFooter(pages) : pages;
    result = this.densifyGenericPages(result);
    if (hasFooter) {
      result = this.trimLastPageForFooter(result);
    }

    if (this.countGenericBlocks(result) !== blocks.length) {
      return pages;
    }
    return hasFooter
      ? this.compactTrailingEmptyPagesPreserveFooterSlot(result, true)
      : this.compactTrailingEmptyPages(result);
  }

  /** Trim for footer, then pull blocks forward so pages fill before breaking early. */
  private finalizeGenericPages(
    pages: GenericPreviewBlock[][],
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    const flat = pages.flat();
    if (!flat.length) {
      return hasFooter ? [[]] : [[]];
    }

    if (!this.canUseLiveGenericMeasure()) {
      if (!hasFooter) {
        return pages.map((page) => [...page]);
      }
      return this.ensureFooterFitsOnLastPageMeasured(pages, this.isLandscapeOrientation());
    }

    const result = this.packMaxFillGenericBlocks(flat, hasFooter);
    return result.length ? result : [[]];
  }

  private densifyGenericPages(pages: GenericPreviewBlock[][]): GenericPreviewBlock[][] {
    if (!this.canUseLiveGenericMeasure()) {
      return pages.map((page) => [...page]);
    }

    const expected = this.countGenericBlocks(pages);
    let result = pages.map((page) => [...page]);
    let changed = true;
    let guard = 0;

    while (changed && guard++ < 120) {
      changed = false;

      for (let i = 0; i < result.length - 1; i++) {
        const nextPage = result[i + 1];
        if (!nextPage?.length) {
          continue;
        }

        let lo = 0;
        let hi = nextPage.length;
        while (lo < hi) {
          const mid = Math.ceil((lo + hi) / 2);
          const trial = [...result[i], ...nextPage.slice(0, mid)];
          if (this.blocksFitLiveMeasurePage(trial, i, false)) {
            lo = mid;
          } else {
            hi = mid - 1;
          }
        }

        if (lo > 0) {
          result[i] = [...result[i], ...nextPage.slice(0, lo)];
          result[i + 1] = nextPage.slice(lo);
          if (!result[i + 1].length) {
            result.splice(i + 1, 1);
          }
          changed = true;
          break;
        }

        const split = this.trySplitBlockIntoCurrentPage(nextPage[0], result[i], i, false);
        if (split && split.length > 1 && this.blocksFitLiveMeasurePage([...result[i], split[0]], i, false)) {
          result[i] = [...result[i], split[0]];
          result[i + 1] = [...split.slice(1), ...nextPage.slice(1)];
          changed = true;
          break;
        }
      }
    }

    if (this.countGenericBlocks(result) !== expected) {
      return pages.map((page) => [...page]);
    }
    return result;
  }

  /**
   * Footer shares the last page with content. Move overflow off the last page
   * until content + footer fit — never drop blocks.
   */
  private trimLastPageForFooter(pages: GenericPreviewBlock[][]): GenericPreviewBlock[][] {
    const expected = this.countGenericBlocks(pages);
    let result = pages.map((page) => [...page]);
    let guard = 0;

    while (guard++ < 120) {
      result = this.compactTrailingEmptyPagesPreserveFooterSlot(result, true);
      if (!result.length) {
        return [[]];
      }

      const lastIdx = result.length - 1;
      const lastPage = result[lastIdx];

      if (!lastPage.length) {
        result.pop();
        continue;
      }

      if (this.blocksFitLiveMeasurePage(lastPage, lastIdx, true)) {
        break;
      }

      if (lastPage.length === 1) {
        const split = this.trySplitOverflowBlock(lastPage[0], lastIdx, true);
        if (split && split.length > 1) {
          const repacked = this.packMaxFillGenericBlocks(split, true);
          result.pop();
          result.push(...repacked);
          if (this.countGenericBlocks(result) !== expected) {
            return pages.map((page) => [...page]);
          }
          continue;
        }
        if (this.blocksFitLiveMeasurePage(lastPage, lastIdx, false)) {
          result.push([]);
          break;
        }
        break;
      }

      const priorBlocks = lastPage.slice(0, -1);
      const trailingBlock = lastPage[lastPage.length - 1];
      const splitIntoFooterPage = this.trySplitBlockIntoCurrentPage(trailingBlock, priorBlocks, lastIdx, true);
      if (splitIntoFooterPage && splitIntoFooterPage.length > 1) {
        const trailingPages = this.packMaxFillGenericBlocks(splitIntoFooterPage.slice(1), true);
        result.splice(
          lastIdx,
          1,
          [...priorBlocks, splitIntoFooterPage[0]],
          ...trailingPages
        );
        if (this.countGenericBlocks(result) !== expected) {
          return pages.map((page) => [...page]);
        }
        continue;
      }

      const moved = lastPage[lastPage.length - 1];
      result[lastIdx] = lastPage.slice(0, -1);
      result.splice(lastIdx + 1, 0, [moved]);
    }

    result = this.compactTrailingEmptyPagesPreserveFooterSlot(result, true);
    if (this.countGenericBlocks(result) !== expected) {
      return pages.map((page) => [...page]);
    }
    const mergedResult = this.mergeLastFooterPageBackIfItFits(result);
    if (this.countGenericBlocks(mergedResult) !== expected) {
      return pages.map((page) => [...page]);
    }
    return mergedResult.length ? mergedResult : [[]];
  }

  private trySplitOverflowBlock(
    block: GenericPreviewBlock,
    pageIndex: number,
    reserveFooter: boolean
  ): GenericPreviewBlock[] | null {
    if (Array.isArray(block?.field?._tableRows)) {
      const split = this.canUseLiveGenericMeasure()
        ? this.splitTableRowsBlockForLivePage(block.field, [], pageIndex, false)
        : this.splitTableRowsBlockByBudget(
            block.field,
            this.isLandscapeOrientation(),
            this.getPageContentBudgetPx(this.isLandscapeOrientation(), pageIndex === 0, false)
          );
      return split.length > 1 ? split : null;
    }

    const chunkHtml = block?.field?._chunkHtml;
    if (typeof chunkHtml !== 'string') {
      return null;
    }
    const landscape = this.isLandscapeOrientation();

    if (this.isWordEditorTableFragment(chunkHtml)) {
      const tableSplit = this.canUseLiveGenericMeasure()
        ? this.splitHtmlTableFragmentForLivePage(block.field, chunkHtml, [], pageIndex, false)
        : this.splitHtmlTableByMeasuredRows(
            block.field,
            chunkHtml,
            landscape,
            this.getPageContentBudgetPx(landscape, pageIndex === 0, false)
          );
      return tableSplit.length > 1 ? tableSplit : null;
    }

    const split = this.splitWordEditorIntoMeasuredBlocks(
      block.field,
      chunkHtml,
      landscape,
      false
    );
    return split.length > 1 ? split : null;
  }

  private mmToPx(mm: number): number {
    if (this.cachedMmToPx === null) {
      const probe = document.createElement('div');
      probe.style.width = '1mm';
      probe.style.position = 'absolute';
      probe.style.visibility = 'hidden';
      document.body.appendChild(probe);
      this.cachedMmToPx = probe.offsetWidth || 3.7795275591;
      document.body.removeChild(probe);
    }
    return this.cachedMmToPx * mm;
  }

  private ensureMeasureRoot(landscape: boolean): HTMLElement {
    if (!this.genericMeasureRoot) {
      const root = document.createElement('div');
      root.className = 'app-details-measure';
      root.setAttribute('aria-hidden', 'true');
      Object.assign(root.style, {
        position: 'fixed',
        left: '-10000px',
        top: '0',
        visibility: 'hidden',
        pointerEvents: 'none',
        zIndex: '-1',
        boxSizing: 'border-box',
      });
      document.body.appendChild(root);
      this.genericMeasureRoot = root;
    }
    const widthMm = landscape
      ? DOCUMENT_RENDER_A4_LONG_EDGE_MM
      : DOCUMENT_RENDER_A4_SHORT_EDGE_MM;
    this.genericMeasureRoot.style.width = `${widthMm}mm`;
    this.genericMeasureRoot.style.padding = landscape ? '10mm 10mm 10mm 12mm' : '18mm 16mm 16mm 16mm';
    this.genericMeasureRoot.innerHTML = '';
    return this.genericMeasureRoot;
  }

  private getPageContentBudgetPx(landscape: boolean, isFirstPage: boolean, reserveFooter: boolean): number {
    const liveBody = this.genericMeasureBody?.nativeElement;
    if (this.canUseLiveGenericMeasure() && liveBody) {
      this.configureLiveMeasurePaper(isFirstPage ? 0 : 1, reserveFooter);
      liveBody.innerHTML = '';
      this.forceLiveMeasureLayout();
      return Math.max(48, liveBody.clientHeight);
    }

    const paperHeightMm = landscape
      ? DOCUMENT_RENDER_A4_SHORT_EDGE_MM
      : DOCUMENT_RENDER_A4_LONG_EDGE_MM;
    const paperPx = this.mmToPx(paperHeightMm);
    const verticalPaddingPx = this.mmToPx(landscape ? 20 : 34);
    const headerPx = isFirstPage ? this.mmToPx(landscape ? 54 : 58) : 0;
    const footerPx = reserveFooter ? this.mmToPx(landscape ? 54 : 50) : 0;
    return Math.max(48, paperPx - verticalPaddingPx - headerPx - footerPx);
  }

  private measureBlockHeight(block: GenericPreviewBlock, landscape: boolean): number {
    const liveBody = this.genericMeasureBody?.nativeElement;
    if (this.canUseLiveGenericMeasure() && liveBody) {
      this.configureLiveMeasurePaper(0, false);
      this.renderBlocksIntoLiveMeasureBody([block]);
      this.forceLiveMeasureLayout();
      return Math.max(liveBody.scrollHeight, this.measureRenderedBodyContentPx(liveBody), 1);
    }

    const root = this.ensureMeasureRoot(landscape);
    const slot = document.createElement('div');
    slot.className = 'xyz-generic-measured-body';
    slot.style.width = '100%';
    slot.style.fontFamily = '"Times New Roman", Times, serif';
    slot.style.fontSize = '13.5px';
    slot.style.lineHeight = '1.35';
    slot.innerHTML = this.renderBlockMeasureHtml(block);
    root.appendChild(slot);
    const height = Math.ceil(slot.getBoundingClientRect().height);
    root.removeChild(slot);
    return Math.max(height, 1);
  }

  private renderBlockMeasureHtml(block: GenericPreviewBlock): string {
    const field = block.field;
    if (typeof field?._chunkHtml === 'string') {
      return `<div class="xyz-generic-field"><div class="xyz-generic-word xyz-word-preview"><div class="ql-editor">${field._chunkHtml}</div></div></div>`;
    }
    if (Array.isArray(field?._tableRows)) {
      const rows = field._tableRows
        .map(
          (row: any[], rowIndex: number) =>
            `<tr><td class="xyz-row-label">${this.escapeHtml(this.getTableRowLabel(field, rowIndex))}</td>${row
              .map((cell) => `<td>${this.escapeHtml(String(cell ?? '-'))}</td>`)
              .join('')}</tr>`
        )
        .join('');
      return `<div class="xyz-generic-field"><div class="overflow-x-auto"><table class="xyz-generic-table"><tbody>${rows}</tbody></table></div></div>`;
    }
    const display = this.formatFieldValue(field, this.getFieldValue(field));
    return `<div class="xyz-generic-field"><div class="xyz-generic-value">${this.escapeHtml(String(display ?? ''))}</div></div>`;
  }

  private expandFieldToMeasuredBlocks(field: any, landscape: boolean): GenericPreviewBlock[] {
    if (this.isWordEditorType(field?.type) || this.isHtmlPreviewField(field)) {
      const value = this.getFieldValue(field);
      const decoded = this.decodeHtmlEntitiesIfNeeded(String(value ?? ''));
      const html = this.normalizeWordEditorHtmlForDisplay(decoded);
      if (!html.trim()) {
        return [];
      }
      return this.splitWordEditorIntoMeasuredBlocks(field, html, landscape);
    }

    if (this.isTableType(field?.type)) {
      return this.splitTableIntoMeasuredBlocks(field, landscape);
    }

    const value = this.getFieldValue(field);
    if (value === null || value === undefined || value === '') {
      return [];
    }
    return [{ field }];
  }

  private splitWordEditorIntoMeasuredBlocks(
    field: any,
    html: string,
    landscape: boolean,
    reserveFooter = false,
    budgetOverridePx?: number
  ): GenericPreviewBlock[] {
    const fragments = this.extractWordEditorHtmlFragments(html);
    const blocks: GenericPreviewBlock[] = [];
    let batch: string[] = [];

    const flush = () => {
      if (!batch.length) {
        return;
      }
      blocks.push({ field: { ...field, _chunkHtml: batch.join('') } });
      batch = [];
    };

    const chunkBudget = () =>
      budgetOverridePx ?? this.getPageContentBudgetPx(landscape, blocks.length === 0 && batch.length === 0, reserveFooter);

    for (const fragment of fragments) {
      const singleBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: fragment } };
      const singleHeight = this.measureBlockHeight(singleBlock, landscape);
      const budget = chunkBudget();

      if (singleHeight > budget) {
        flush();
        if (this.isWordEditorTableFragment(fragment)) {
          blocks.push(...this.splitHtmlTableByMeasuredRows(field, fragment, landscape, budget));
        } else {
          blocks.push(...this.splitPlainTextWordEditorFragment(field, fragment, landscape, budget));
        }
        continue;
      }

      const trial = [...batch, fragment];
      const trialBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: trial.join('') } };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);

      if (batch.length > 0 && trialHeight > budget) {
        if (this.isWordEditorTableFragment(fragment)) {
          const existingHtml = batch.join('');
          const existingBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: existingHtml } };
          const existingHeight = this.measureBlockHeight(existingBlock, landscape);
          const remainingBudget = Math.max(0, budget - existingHeight);
          const split = this.splitHtmlTableByMeasuredRows(field, fragment, landscape, remainingBudget);

          if (split.length > 1 && typeof split[0]?.field?._chunkHtml === 'string') {
            const firstPieceHtml = String(split[0].field._chunkHtml || '');
            const combinedHtml = `${existingHtml}${firstPieceHtml}`;
            const combinedBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: combinedHtml } };
            const combinedHeight = this.measureBlockHeight(combinedBlock, landscape);

            if (combinedHeight <= budget) {
              blocks.push({ field: { ...field, _chunkHtml: combinedHtml } });
              batch = [];
              blocks.push(...split.slice(1));
              continue;
            }

            flush();
            blocks.push(...split);
            continue;
          }
        }

        flush();
        batch = [fragment];
      } else {
        batch = trial;
      }
    }

    flush();
    return blocks.length ? blocks : [{ field: { ...field, _chunkHtml: html } }];
  }

  private isWordEditorTableFragment(fragment: string): boolean {
    const trimmed = String(fragment || '').trim();
    return /^\s*<table\b/i.test(trimmed) || /<table\b/i.test(trimmed);
  }

  /** Preserve Quill/HTML structure (tables, lists, paragraphs) as pagination fragments. */
  private extractWordEditorHtmlFragments(html: string): string[] {
    const normalized = this.normalizeWordEditorHtmlForDisplay(String(html || ''));
    const wrapper = document.createElement('div');
    wrapper.innerHTML = normalized;

    const container =
      wrapper.children.length === 1 && wrapper.firstElementChild
        ? (wrapper.firstElementChild as HTMLElement)
        : wrapper;

    const fragments: string[] = [];
    const pushFragment = (piece: string) => {
      const trimmed = String(piece || '').trim();
      if (trimmed) {
        fragments.push(trimmed);
      }
    };

    const blockTags = new Set(['p', 'div', 'table', 'ul', 'ol', 'blockquote', 'pre', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6']);
    const visitNode = (node: ChildNode) => {
      if (node.nodeType === Node.TEXT_NODE) {
        const text = (node.textContent || '').trim();
        if (text) {
          pushFragment(`<p>${this.escapeHtml(text)}</p>`);
        }
        return;
      }
      if (node.nodeType !== Node.ELEMENT_NODE) {
        return;
      }

      const el = node as HTMLElement;
      const tag = el.tagName.toLowerCase();

      if (tag === 'table') {
        pushFragment(el.outerHTML);
        return;
      }

      if (tag === 'p' && /<br\s*\/?>/i.test(el.innerHTML)) {
        el.innerHTML
          .split(/<br\s*\/?>/gi)
          .map((part) => part.trim())
          .filter(Boolean)
          .forEach((part) => pushFragment(`<p>${part}</p>`));
        return;
      }

      if (tag === 'br') {
        return;
      }

      if (tag === 'div') {
        const childElements = Array.from(el.children) as HTMLElement[];
        const hasStructuredChildren = childElements.some((child) => {
          const childTag = child.tagName.toLowerCase();
          return childTag === 'table' || blockTags.has(childTag) || childTag.startsWith('h');
        });
        const hasMultipleNodes = el.childNodes.length > 1;
        const hasInlineMarkup = childElements.some((child) => {
          const childTag = child.tagName.toLowerCase();
          return childTag === 'span' || childTag === 'strong' || childTag === 'em' || childTag === 'b' || childTag === 'i' || childTag === 'u';
        });
        if (hasStructuredChildren || hasMultipleNodes || hasInlineMarkup || /<br\s*\/?>/i.test(el.innerHTML)) {
          Array.from(el.childNodes).forEach((child) => visitNode(child));
          return;
        }
        const divText = (el.textContent || '').trim();
        if (divText) {
          pushFragment(`<p>${this.escapeHtml(divText)}</p>`);
          return;
        }
      }

      if (blockTags.has(tag) || tag.startsWith('h')) {
        pushFragment(el.outerHTML);
        return;
      }

      pushFragment(el.outerHTML);
    };

    for (const node of Array.from(container.childNodes)) {
      visitNode(node);
    }

    if (!fragments.length && container.innerHTML.trim()) {
      pushFragment(container.innerHTML);
    }

    return fragments;
  }

  private buildWordEditorTableFromRows(
    sourceTable: HTMLTableElement,
    rows: Element[],
    includeHeader: boolean
  ): string {
    const table = sourceTable.cloneNode(false) as HTMLTableElement;
    Array.from(sourceTable.children).forEach((child) => {
      const tag = child.tagName.toLowerCase();
      if (tag === 'colgroup') {
        table.appendChild(child.cloneNode(true));
      }
    });
    if (includeHeader) {
      const thead = sourceTable.querySelector('thead');
      if (thead) {
        table.appendChild(thead.cloneNode(true));
      }
    }
    const tbody = document.createElement('tbody');
    rows.forEach((row) => tbody.appendChild(row.cloneNode(true)));
    table.appendChild(tbody);
    return table.outerHTML;
  }

  private splitHtmlTableByMeasuredRows(
    field: any,
    tableHtml: string,
    landscape: boolean,
    budget: number
  ): GenericPreviewBlock[] {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = tableHtml;
    const table = wrapper.querySelector('table');
    if (!table) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    const bodyRows = Array.from(table.querySelectorAll('tbody tr'));
    const rows = bodyRows.length ? bodyRows : Array.from(table.querySelectorAll('tr'));
    if (rows.length <= 1) {
      return [{ field: { ...field, _chunkHtml: tableHtml } }];
    }

    const hasThead = !!table.querySelector('thead');
    const blocks: GenericPreviewBlock[] = [];
    let batch: Element[] = [];
    let includeHeader = hasThead;

    const flushRows = () => {
      if (!batch.length) {
        return;
      }
      blocks.push({
        field: {
          ...field,
          _chunkHtml: this.buildWordEditorTableFromRows(table, batch, includeHeader),
        },
      });
      includeHeader = false;
      batch = [];
    };

    for (const row of rows) {
      const trial = [...batch, row];
      const trialHtml = this.buildWordEditorTableFromRows(table, trial, includeHeader);
      const trialBlock: GenericPreviewBlock = { field: { ...field, _chunkHtml: trialHtml } };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);

      if (batch.length > 0 && trialHeight > budget) {
        flushRows();
        batch = [row];
        continue;
      }
      batch.push(row);
    }

    flushRows();
    return blocks.length ? blocks : [{ field: { ...field, _chunkHtml: tableHtml } }];
  }

  private splitPlainTextWordEditorFragment(
    field: any,
    fragment: string,
    landscape: boolean,
    budget: number
  ): GenericPreviewBlock[] {
    const text = this.stripHtmlToText(fragment);
    if (!text) {
      return [{ field: { ...field, _chunkHtml: fragment } }];
    }

    const lines = this.wrapPlainLineToSegments(text, this.getPreviewMaxCharsPerLine());
    const blocks: GenericPreviewBlock[] = [];
    let batch: string[] = [];

    const flush = () => {
      if (!batch.length) {
        return;
      }
      blocks.push({
        field: { ...field, _chunkHtml: this.linesToWordEditorHtml(batch) },
      });
      batch = [];
    };

    for (const line of lines) {
      const trial = [...batch, line];
      const trialBlock: GenericPreviewBlock = {
        field: { ...field, _chunkHtml: this.linesToWordEditorHtml(trial) },
      };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);
      if (batch.length > 0 && trialHeight > budget) {
        flush();
      }
      batch.push(line);
    }

    flush();
    return blocks.length ? blocks : [{ field: { ...field, _chunkHtml: fragment } }];
  }

  private splitTableIntoMeasuredBlocks(field: any, landscape: boolean): GenericPreviewBlock[] {
    const rows = this.getTableData(field);
    if (!rows.length) {
      return [];
    }

    const blocks: GenericPreviewBlock[] = [];
    let batch: any[][] = [];

    const flush = () => {
      if (!batch.length) {
        return;
      }
      blocks.push({ field: { ...field, _tableRows: [...batch] } });
      batch = [];
    };

    for (const row of rows) {
      const trial = [...batch, row];
      const trialBlock: GenericPreviewBlock = { field: { ...field, _tableRows: trial } };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);
      const budget = this.getPageContentBudgetPx(landscape, blocks.length === 0 && batch.length === 0, false);
      if (batch.length > 0 && trialHeight > budget) {
        flush();
        batch = [row];
      } else {
        batch = trial;
      }
    }

    flush();
    return blocks;
  }

  private splitTableRowsBlockByBudget(field: any, landscape: boolean, budget: number): GenericPreviewBlock[] {
    const rows = Array.isArray(field?._tableRows) ? field._tableRows : this.getTableData(field);
    if (!rows.length) {
      return [];
    }

    const blocks: GenericPreviewBlock[] = [];
    let batch: any[][] = [];

    const flush = () => {
      if (!batch.length) {
        return;
      }
      blocks.push({ field: { ...field, _tableRows: [...batch] } });
      batch = [];
    };

    for (const row of rows) {
      const trial = [...batch, row];
      const trialBlock: GenericPreviewBlock = { field: { ...field, _tableRows: trial } };
      const trialHeight = this.measureBlockHeight(trialBlock, landscape);
      if (batch.length > 0 && trialHeight > budget) {
        flush();
      }
      batch.push(row);
    }

    flush();
    return blocks.length ? blocks : [{ field: { ...field, _tableRows: rows } }];
  }
  private packMeasuredBlocksIntoPages(
    blocks: GenericPreviewBlock[],
    landscape: boolean,
    hasFooter: boolean
  ): GenericPreviewBlock[][] {
    if (!blocks.length) {
      return hasFooter ? this.ensureFooterFitsOnLastPageMeasured([[]], landscape) : [[]];
    }

    const pages: GenericPreviewBlock[][] = [];
    let current: GenericPreviewBlock[] = [];
    let currentHeight = 0;

    const flush = () => {
      if (current.length) {
        pages.push(current);
        current = [];
        currentHeight = 0;
      }
    };

    for (let i = 0; i < blocks.length; i++) {
      const block = blocks[i];
      const blockHeight = this.measureBlockHeight(block, landscape);
      const isFirstPage = pages.length === 0 && current.length === 0;
      const isFinalPage = i === blocks.length - 1;
      const reserveFooter = hasFooter && isFinalPage;
      let budget = this.getPageContentBudgetPx(landscape, isFirstPage, reserveFooter);

      if (blockHeight > budget) {
        flush();
        pages.push([block]);
        continue;
      }

      if (current.length > 0 && currentHeight + blockHeight > budget) {
        flush();
        budget = this.getPageContentBudgetPx(
          landscape,
          pages.length === 0 && current.length === 0,
          hasFooter && i === blocks.length - 1
        );
      }

      current.push(block);
      currentHeight += blockHeight;
    }

    flush();

    if (!pages.length) {
      return hasFooter ? this.ensureFooterFitsOnLastPageMeasured([[]], landscape) : [[]];
    }

    return hasFooter
      ? this.ensureFooterFitsOnLastPageMeasured(pages, landscape)
      : pages;
  }

  private blocksFitMeasuredPage(
    blocks: GenericPreviewBlock[],
    landscape: boolean,
    pageIndex: number,
    reserveFooter: boolean
  ): boolean {
    if (!blocks.length) {
      if (!reserveFooter) {
        return true;
      }
      const budget = this.getPageContentBudgetPx(landscape, pageIndex === 0, true);
      return budget > 0;
    }
    const totalHeight = blocks.reduce((sum, block) => sum + this.measureBlockHeight(block, landscape), 0);
    const budget =
      this.getPageContentBudgetPx(landscape, pageIndex === 0, reserveFooter) -
      DOCUMENT_RENDER_PAGE_FIT_SAFETY_PX;
    return totalHeight <= budget;
  }

  private ensureFooterFitsOnLastPageMeasured(
    pages: GenericPreviewBlock[][],
    landscape: boolean
  ): GenericPreviewBlock[][] {
    let result = pages.map((page) => [...page]);
    if (!result.length) {
      result.push([]);
    }

    const expected = this.countGenericBlocks(result);
    let guard = 0;
    while (guard++ < 120) {
      result = this.compactTrailingEmptyPagesPreserveFooterSlot(result, true);
      if (!result.length) {
        break;
      }

      const lastIdx = result.length - 1;
      const lastPage = result[lastIdx];

      if (!lastPage.length) {
        result.pop();
        continue;
      }

      if (this.blocksFitMeasuredPage(lastPage, landscape, lastIdx, true)) {
        break;
      }

      if (lastPage.length === 1) {
        const split = this.trySplitOverflowBlock(lastPage[0], lastIdx, true);
        if (split && split.length > 1) {
          const repacked = this.packMeasuredBlocksIntoPages(split, landscape, true);
          result.pop();
          result.push(...repacked);
          if (this.countGenericBlocks(result) !== expected) {
            return pages.map((page) => [...page]);
          }
          continue;
        }
        if (this.blocksFitMeasuredPage(lastPage, landscape, lastIdx, false)) {
          result.push([]);
          break;
        }
        break;
      }

      const priorBlocks = lastPage.slice(0, -1);
      const trailingBlock = lastPage[lastPage.length - 1];
      const splitIntoFooterPage = this.trySplitBlockIntoCurrentPage(trailingBlock, priorBlocks, lastIdx, true);
      if (splitIntoFooterPage && splitIntoFooterPage.length > 1) {
        const trailingPages = this.packMeasuredBlocksIntoPages(splitIntoFooterPage.slice(1), landscape, true);
        result.splice(
          lastIdx,
          1,
          [...priorBlocks, splitIntoFooterPage[0]],
          ...trailingPages
        );
        if (this.countGenericBlocks(result) !== expected) {
          return pages.map((page) => [...page]);
        }
        continue;
      }

      const moved = lastPage[lastPage.length - 1];
      result[lastIdx] = lastPage.slice(0, -1);
      result.splice(lastIdx + 1, 0, [moved]);
    }

    if (this.countGenericBlocks(result) !== expected) {
      return pages.map((page) => [...page]);
    }
    const mergedResult = this.mergeLastFooterPageBackIfItFits(result);
    return this.countGenericBlocks(mergedResult) === expected ? mergedResult : pages.map((page) => [...page]);
  }

  getTableRowsForBlock(field: any): any[][] {
    if (field && Array.isArray(field._tableRows)) {
      return field._tableRows;
    }
    return this.getTableData(field);
  }

  private getPreviewMaxCharsPerLine(): number {
    return getDocumentRenderPreviewMaxCharsPerLine(this.isLandscapeOrientation());
  }

  private wordEditorHtmlToPlainLines(html: string): string[] {
    if (!html || !String(html).trim()) {
      return ['-'];
    }
    const normalized = this.normalizeWordEditorHtmlForDisplay(String(html));
    const withBreaks = normalized
      .replace(/<style[\s\S]*?<\/style>/gi, '')
      .replace(/<script[\s\S]*?<\/script>/gi, '')
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/p>/gi, '\n')
      .replace(/<\/div>/gi, '\n')
      .replace(/<\/li>/gi, '\n')
      .replace(/<\/tr>/gi, '\n')
      .replace(/<\/h[1-6]>/gi, '\n');

    const tmp = document.createElement('div');
    tmp.innerHTML = withBreaks.replace(/<[^>]+>/g, ' ');
    const plain = (tmp.textContent || tmp.innerText || '')
      .replace(/\u00a0/g, ' ')
      .replace(/\r/g, '');

    const rawLines = plain.split('\n').map((line) => line.replace(/\s+$/g, ''));
    const out: string[] = [];
    const maxChars = this.getPreviewMaxCharsPerLine();

    for (const raw of rawLines) {
      if (raw === '') {
        out.push('');
        continue;
      }
      out.push(...this.wrapPlainLineToSegments(raw, maxChars));
    }

    return out.length ? out : ['-'];
  }

  private wrapPlainLineToSegments(text: string, maxChars: number): string[] {
    const trimmed = text.trim();
    if (!trimmed) {
      return [];
    }
    if (trimmed.length <= maxChars) {
      return [trimmed];
    }

    const segments: string[] = [];
    let remaining = trimmed;
    while (remaining.length > 0) {
      if (remaining.length <= maxChars) {
        segments.push(remaining);
        break;
      }
      let slice = remaining.slice(0, maxChars);
      const lastSpace = slice.lastIndexOf(' ');
      if (lastSpace > maxChars / 3) {
        slice = remaining.slice(0, lastSpace);
        remaining = remaining.slice(lastSpace).trimStart();
      } else {
        slice = remaining.slice(0, maxChars);
        remaining = remaining.slice(maxChars);
      }
      segments.push(slice);
    }
    return segments;
  }

  private linesToWordEditorHtml(lines: string[]): string {
    return lines
      .map((line) => (line ? `<p>${this.escapeHtml(line)}</p>` : '<p><br></p>'))
      .join('');
  }

  useGenericFooterPinnedLayout(): boolean {
    return this.hasIndividualPipelineFooter();
  }

  private normalizeWordEditorChunkSizes(chunks: string[], maxChars: number): string[] {
    const result: string[] = [];
    for (const chunk of chunks) {
      const trimmed = String(chunk || '').trim();
      if (!trimmed) {
        continue;
      }
      if (this.stripHtmlToText(trimmed).length <= maxChars) {
        result.push(trimmed);
      } else {
        result.push(...this.splitHtmlByCharacterBudget(trimmed, maxChars));
      }
    }
    return result;
  }

  private splitHtmlByCharacterBudget(html: string, maxChars: number): string[] {
    const text = this.stripHtmlToText(html);
    if (!text) {
      return [];
    }
    if (text.length <= maxChars) {
      return [html.trim() || `<p>${this.escapeHtml(text)}</p>`];
    }

    const chunks: string[] = [];
    const paragraphs = text.split(/\n+/);
    let current = '';
    let currentLen = 0;

    const flush = () => {
      if (!current) {
        return;
      }
      chunks.push(`<p>${this.escapeHtml(current)}</p>`);
      current = '';
      currentLen = 0;
    };

    for (const para of paragraphs) {
      const trimmed = para.trim();
      if (!trimmed) {
        continue;
      }
      if (trimmed.length > maxChars) {
        flush();
        for (let i = 0; i < trimmed.length; i += maxChars) {
          chunks.push(`<p>${this.escapeHtml(trimmed.slice(i, i + maxChars))}</p>`);
        }
        continue;
      }
      if (currentLen > 0 && currentLen + trimmed.length + 1 > maxChars) {
        flush();
      }
      current = current ? `${current}\n${trimmed}` : trimmed;
      currentLen = current.length;
    }

    flush();
    return chunks.length ? chunks : [`<p>${this.escapeHtml(text.slice(0, maxChars))}</p>`];
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

    let blocks = Array.from(container.childNodes).filter((n) => {
      if (n.nodeType === Node.TEXT_NODE) {
        return (n.textContent || '').trim().length > 0;
      }
      if (n.nodeType !== Node.ELEMENT_NODE) {
        return false;
      }
      const tag = (n as Element).tagName.toLowerCase();
      return tag === 'p' || tag === 'div' || tag === 'table' || tag === 'ul' || tag === 'ol' || tag.startsWith('h');
    });

    if (blocks.length === 1 && blocks[0].nodeType === Node.ELEMENT_NODE) {
      const onlyEl = blocks[0] as HTMLElement;
      const tag = onlyEl.tagName.toLowerCase();
      if (tag === 'p' && /<br\s*\/?>/i.test(onlyEl.innerHTML)) {
        blocks = onlyEl.innerHTML
          .split(/<br\s*\/?>/gi)
          .map((part) => part.trim())
          .filter(Boolean)
          .map((part) => {
            const p = document.createElement('p');
            p.innerHTML = part;
            return p;
          });
      }
    }

    const fullHtml = `${preservedStyleHtml}${wrapOpen}${container.innerHTML}${wrapClose}`;
    const fullTextLen = this.stripHtmlToText(fullHtml).length;

    if (blocks.length <= 1) {
      if (fullTextLen <= maxChars) {
        return [fullHtml];
      }
      return this.normalizeWordEditorChunkSizes([fullHtml], maxChars);
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

      if (nodeTextLen > maxChars) {
        if (currentChars > 0) {
          chunks.push(`${preservedStyleHtml}${wrapOpen}${currentHtml}${wrapClose}`);
          currentHtml = '';
          currentChars = 0;
        }
        chunks.push(
          ...this.normalizeWordEditorChunkSizes([`${preservedStyleHtml}${wrapOpen}${nodeHtml}${wrapClose}`], maxChars)
        );
        continue;
      }

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

  useIndividualFooterDocumentLayout(): boolean {
    const hasHeaderField = (this.formFields || []).some((field: any) => this.isDocumentHeaderType(field?.type));
    const hasWordEditorField = this.getBodyPreviewFields().some((field: any) => this.isWordEditorType(field?.type));
    return this.hasIndividualPipelineFooter() && hasHeaderField && hasWordEditorField;
  }

  getGenericPreviewFields(): any[] {
    const fields = (this.formFields || []).filter(
      (field: any) => !this.isDocumentHeaderType(field?.type) && !this.isOrientationType(field?.type)
    );
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
      'footerfields',
      '_formOrientation',
      'formOrientation'
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
    wrapper.innerHTML = stripEditorTableChromeFromHtml(html);

    wrapper.querySelectorAll('figure.table').forEach((figure: Element) => {
      const el = figure as HTMLElement;
      el.style.width = '100%';
      el.style.maxWidth = '100%';
      el.style.margin = '6px 0';
      el.style.boxSizing = 'border-box';
    });

    wrapper.querySelectorAll('table').forEach((tableNode: Element) => {
      const table = tableNode as HTMLTableElement;
      table.style.width = '100%';
      table.style.maxWidth = '100%';
      table.style.tableLayout = 'fixed';
      table.style.borderCollapse = 'collapse';
      table.style.boxSizing = 'border-box';
    });

    // Replace editor textareas with static content so table cells don't keep textarea heights.
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
      el.style.overflowWrap = 'anywhere';
      el.style.wordBreak = 'break-word';

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
      rowEl.style.minHeight = '32px';
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
    const value = this.getFieldValue(field);
    return this.coerceTableData(value);
  }

  private coerceTableData(value: any): any[][] {
    const normalizeRows = (candidate: any): any[][] => {
      if (!Array.isArray(candidate)) {
        return [];
      }
      return candidate
        .filter((row: any) => Array.isArray(row))
        .map((row: any[]) => row.map((cell: any) => cell ?? ''));
    };

    const directRows = normalizeRows(value);
    if (directRows.length > 0) {
      return directRows;
    }

    if (typeof value !== 'string') {
      return [];
    }

    const raw = value.trim();
    if (!raw) {
      return [];
    }

    try {
      let parsed: any = JSON.parse(raw);
      if (typeof parsed === 'string') {
        parsed = JSON.parse(parsed);
      }
      return normalizeRows(parsed);
    } catch {
      return [];
    }
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

  /**
   * History levels allowed when attributing an approval row to a pipeline card.
   * CAPF uses exact `intApprovalOrder` in history — wide tolerance pulled the same user’s
   * earlier stage (e.g. initiator / HoD) into another department card when they are also that dept’s head.
   */
  private getAllowedHistoryLevelsForStage(pipelineOrder: number): number[] {
    if (this.isCapfForm() && pipelineOrder >= 0) {
      return [pipelineOrder];
    }
    return [
      pipelineOrder,
      pipelineOrder - 1, pipelineOrder + 1,
      pipelineOrder - 2, pipelineOrder + 2
    ];
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
      const allowedLevels = this.getAllowedHistoryLevelsForStage(pipelineOrder);

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
    const overallStatus = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();

    // When application is sent back to initiator, all actual approval stages must reset visually.
    if (overallStatus === 'SENT_BACK_TO_INITIATOR' && !isVirtualInitiatorStage) {
      return 'PENDING';
    }

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
    if (this.isSentBackToInitiatorState()) return '';
    if (this.getDepartmentHeadStatus(pipelineOrder, departmentId, approverId) !== 'APPROVED') return '';
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
    if (this.isSentBackToInitiatorState()) return '';
    if (this.getDepartmentHeadStatus(pipelineOrder, departmentId, approverId) !== 'APPROVED') return '';
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
    const current = Array.isArray(this.approvalHistory) ? this.approvalHistory : [];
    const prior = Array.isArray(this.priorApprovalHistory) ? this.priorApprovalHistory : [];
    const merged = [...prior, ...current];
    if (merged.length === 0) {
      return [];
    }
    const seen = new Set<string>();
    const rows: any[] = [];
    for (const entry of merged) {
      if (!entry || typeof entry !== 'object') continue;
      const key = [
        entry?.action || entry?.status || '',
        entry?.approvedBy ?? entry?.approverUserId ?? entry?.userId ?? '',
        entry?.approvedDate || entry?.sentBackDate || '',
        entry?.level ?? entry?.intApprovalOrder ?? '',
        entry?.remarks ?? entry?.comment ?? ''
      ].join('|');
      if (seen.has(key)) continue;
      seen.add(key);
      rows.push(entry);
    }
    rows.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    return rows;
  }

  getApprovalLogLevel(entry: any): string {
    const level = entry?.level ?? entry?.intApprovalOrder;
    if (level == null || String(level).trim() === '') return '--';
    const numericLevel = Number(level);
    if (!Number.isNaN(numericLevel) && numericLevel === -99) return 'CEO';
    const action = (entry?.action || entry?.status || '').toString().toUpperCase();
    if (!Number.isNaN(numericLevel) && numericLevel === 999 && action === 'ASSET_CODE_ASSIGNED') {
      return 'Asset Code';
    }
    return String(level);
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
    const action = (entry?.action || entry?.status || '').toString().trim();
    if (!action) return '--';
    const normalized = action.toUpperCase();
    if (normalized === 'ASSET_CODE_ASSIGNED') return 'Asset Code Assigned';
    if (normalized === 'PR_CODE_ASSIGNED') return 'PR Code Assigned';
    if (normalized === 'PO_CODE_ASSIGNED') return 'PO Code Assigned';
    if (normalized === 'PO_EDIT_REAPPROVAL') return 'PO Edit - Re-approval Required';
    if (normalized === 'PO_VENDOR_EDIT_TE') return 'PO Vendor Edit - Technical Expert Review';
    if (normalized === 'PO_VENDOR_TE_APPROVED') return 'PO Vendor Edit - Technical Expert Approved';
    if (normalized === 'PO_VENDOR_TE_REJECTED') return 'PO Vendor Edit - Technical Expert Rejected';
    if (normalized === 'ASSET_CODE_CONFIRMED') return 'Asset Code Confirmed';
    if (normalized === 'SENT_BACK_TO_INITIATOR') return 'Sent Back To Initiator';
    if (normalized === 'SENT_BACK') return 'Sent Back';
    return action;
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

    const allowedLevels = this.getAllowedHistoryLevelsForStage(pipelineOrder);

    const candidates = this.approvalHistory.filter((e: any) => {
      const action = (e.action || '').toString().toUpperCase();
      if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;

      const entryLevel = e.level ?? e.intApprovalOrder;
      if (entryLevel == null) return false;
      const lvl = Number(entryLevel);
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
      const resetToInitiatorT = this.getSendBackToInitiatorResetTime();
      if (resetToInitiatorT > 0) {
        const afterReset = candidates.filter((c) => this.getApprovalEntryTime(c) > resetToInitiatorT);
        if (afterReset.length === 0) return null;
        return afterReset[0];
      }
    }

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
    if (this.isSentBackToInitiatorState()) return '';
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
    if (this.isSentBackToInitiatorState()) return '';
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

    return this.applyCapfEffectivePipelineRouting(sortedPipelines);
  }

  /**
   * CAPF Project mode: replace Technical Expert pipeline stage with the selected substitute
   * department for workflow display only (form-builder pipeline JSON stays unchanged).
   */
  private applyCapfEffectivePipelineRouting(pipelines: any[]): any[] {
    if (!pipelines?.length || !this.isCapfForm() || !this.isCapfTechnicalExpertProjectMode()) {
      return pipelines;
    }

    const substituteDeptId = this.getCapfSubstituteDepartmentId();
    if (substituteDeptId == null) {
      return pipelines;
    }

    const substituteDeptName =
      this.departmentNameMap.get(Number(substituteDeptId)) ||
      `Department ${substituteDeptId}`;

    return pipelines.map((pipeline) => {
      if (!pipeline || pipeline.isInitiator || this.isCapfExtraPipeline(pipeline)) {
        return pipeline;
      }
      if (!this.isTechnicalExpertPipelineStage(pipeline)) {
        return pipeline;
      }

      return {
        ...pipeline,
        serDepartmentId: substituteDeptId,
        departmentId: substituteDeptId,
        departmentName: substituteDeptName,
        txtDepartmentName: substituteDeptName,
        hrTblDepartment: {
          ...(pipeline.hrTblDepartment || {}),
          serDepartmentId: substituteDeptId,
          departmentId: substituteDeptId,
          departmentName: substituteDeptName,
          txtDepartmentName: substituteDeptName
        }
      };
    });
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
      if (!this.capfUsesPipelineCeoApproval()) {
        cards.push({ pipeline: { type: 'capf_ceo' }, pipelineIndex: baseIndex, approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      }
      cards.push({ pipeline: { type: 'capf_asset_code' }, pipelineIndex: baseIndex + (this.capfUsesPipelineCeoApproval() ? 0 : 1), approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      cards.push({ pipeline: { type: 'capf_pr_code' }, pipelineIndex: baseIndex + (this.capfUsesPipelineCeoApproval() ? 1 : 2), approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      cards.push({ pipeline: { type: 'capf_po_code' }, pipelineIndex: baseIndex + (this.capfUsesPipelineCeoApproval() ? 2 : 3), approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      if (this.shouldShowCapfPoVendorTeStage()) {
        cards.push({ pipeline: { type: 'capf_po_vendor_te' }, pipelineIndex: baseIndex + (this.capfUsesPipelineCeoApproval() ? 3 : 4), approverId: null, approverIndex: 0, totalApproversInStage: 1 });
      }
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

  capfUsesPipelineCeoApproval(): boolean {
    if (!this.isCapfForm()) {
      return false;
    }
    return capfHasPipelineCeoSignatureSlot(this.getPipelineData());
  }

  isCapfExtraPipeline(pipeline: any): boolean {
    const t = (pipeline?.type || '').toString().toLowerCase();
    return t === 'capf_ceo' || t === 'capf_asset_code' || t === 'capf_pr_code' || t === 'capf_po_code'
      || t === 'capf_po_vendor_te';
  }

  private shouldShowCapfPoVendorTeStage(): boolean {
    const status = (this.applicationDetails?.txtStatus || '').toString().toUpperCase();
    if (status === 'PO_VENDOR_TE_PENDING') return true;
    return !!this.findApprovalHistoryEntry((e: any) =>
      (e?.action || e?.status || '').toString().toUpperCase() === 'PO_VENDOR_TE_APPROVED'
      || (e?.action || e?.status || '').toString().toUpperCase() === 'PO_VENDOR_TE_REJECTED'
    );
  }

  getCapfExtraStageTitle(type: string | undefined): string {
    const t = (type || '').toString().toLowerCase();
    if (t === 'capf_ceo') return 'CEO Approval';
    if (t === 'capf_asset_code') return 'Asset Code Assign';
    if (t === 'capf_pr_code') return 'PR';
    if (t === 'capf_po_code') return 'PO';
    if (t === 'capf_po_vendor_te') return 'PO Vendor Review (Technical Expert)';
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
    const hasPo = !!(this.applicationDetails?.txtPoCode && String(this.applicationDetails.txtPoCode).trim());

    if (this.isCapfPoEditReapprovalActive()) {
      if (t === 'capf_ceo') {
        if (status === 'CEO_PENDING') return 'CURRENT';
        return 'PENDING';
      }
      if (t === 'capf_asset_code') {
        if (status === 'ASSET_PENDING') return 'CURRENT';
        if (status === 'CEO_PENDING') return 'APPROVED';
        return 'PENDING';
      }
      if (t === 'capf_pr_code') {
        return hasPr ? 'APPROVED' : 'PENDING';
      }
      if (t === 'capf_po_code') {
        if (hasPo) return 'APPROVED';
        if (hasPr && status === 'PO_PENDING') return 'CURRENT';
        return 'PENDING';
      }
      return 'PENDING';
    }

    if (t === 'capf_ceo') {
      if (status === 'CEO_PENDING') return 'CURRENT';
      const ceoApproved = !!this.findApprovalHistoryEntry((e: any) => {
        const action = (e.action || e.status || '').toString().toUpperCase();
        const desig = (e.designation || e.txtDesignation || e.departmentName || '').toString().toUpperCase();
        return action === 'APPROVED' && desig.includes('CEO');
      });
      if (ceoApproved || status === 'ASSET_PENDING' || status === 'PR_PENDING' || status === 'PO_PENDING' || status === 'APPROVED') return 'APPROVED';
      return 'PENDING';
    }

    if (t === 'capf_asset_code') {
      if (status === 'ASSET_PENDING') return 'CURRENT';
      if (hasAsset && (status === 'PR_PENDING' || status === 'PO_PENDING' || status === 'APPROVED' || hasPr)) return 'APPROVED';
      // If CEO is not done yet, keep this pending.
      return 'PENDING';
    }

    if (t === 'capf_pr_code') {
      if (hasPr) return 'APPROVED';
      if (hasAsset && status === 'PR_PENDING') return 'CURRENT';
      return 'PENDING';
    }

    if (t === 'capf_po_code') {
      if (hasPo) return 'APPROVED';
      if (hasPr && status === 'PO_PENDING') return 'CURRENT';
      return 'PENDING';
    }

    if (t === 'capf_po_vendor_te') {
      if (status === 'PO_VENDOR_TE_PENDING') return 'CURRENT';
      const teDecision = this.findApprovalHistoryEntry((e: any) => {
        const action = (e.action || e.status || '').toString().toUpperCase();
        return action === 'PO_VENDOR_TE_APPROVED' || action === 'PO_VENDOR_TE_REJECTED';
      });
      if (teDecision) {
        const action = (teDecision.action || teDecision.status || '').toString().toUpperCase();
        return action === 'PO_VENDOR_TE_REJECTED' ? 'REJECTED' : 'APPROVED';
      }
      return 'PENDING';
    }

    return 'PENDING';
  }

  getCapfExtraStageApprovedBy(type: string | undefined): string {
    if (this.getCapfExtraStageStatus(type) !== 'APPROVED') return '';
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
        return action === 'ASSET_CODE_ASSIGNED' || (action === 'APPROVED' && desig.includes('FINANCE'));
      });
      return e?.approverName || this.getUserNameById(e?.approvedBy) || '--';
    }
    if (t === 'capf_pr_code') {
      const e = this.findApprovalHistoryEntry((x: any) => (x.action || x.status || '').toString().toUpperCase() === 'PR_CODE_ASSIGNED');
      return e?.approverName || this.getUserNameById(e?.approvedBy) || '--';
    }
    if (t === 'capf_po_code') {
      const e = this.findApprovalHistoryEntry((x: any) => (x.action || x.status || '').toString().toUpperCase() === 'PO_CODE_ASSIGNED');
      return e?.approverName || this.getUserNameById(e?.approvedBy) || '--';
    }
    if (t === 'capf_po_vendor_te') {
      const e = this.findApprovalHistoryEntry((x: any) =>
        (x.action || x.status || '').toString().toUpperCase() === 'PO_VENDOR_TE_APPROVED'
      );
      return e?.approverName || this.getUserNameById(e?.approvedBy) || '--';
    }
    return '--';
  }

  getCapfExtraStageApprovedAt(type: string | undefined): string {
    if (this.getCapfExtraStageStatus(type) !== 'APPROVED') return '';
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
        return action === 'ASSET_CODE_ASSIGNED' || (action === 'APPROVED' && desig.includes('FINANCE'));
      }
      if (t === 'capf_pr_code') {
        return (x.action || x.status || '').toString().toUpperCase() === 'PR_CODE_ASSIGNED';
      }
      if (t === 'capf_po_code') {
        return (x.action || x.status || '').toString().toUpperCase() === 'PO_CODE_ASSIGNED';
      }
      if (t === 'capf_po_vendor_te') {
        return (x.action || x.status || '').toString().toUpperCase() === 'PO_VENDOR_TE_APPROVED';
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
    if (this.getCapfExtraStageStatus(type) !== 'APPROVED') return '';
    const t = (type || '').toString().toLowerCase();
    const e = this.findApprovalHistoryEntry((x: any) => {
      const action = (x.action || x.status || '').toString().toUpperCase();
      const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
      if (t === 'capf_ceo') return action === 'APPROVED' && desig.includes('CEO');
      if (t === 'capf_asset_code') return action === 'ASSET_CODE_ASSIGNED' || (action === 'APPROVED' && desig.includes('FINANCE'));
      if (t === 'capf_pr_code') return action === 'PR_CODE_ASSIGNED';
      if (t === 'capf_po_code') return action === 'PO_CODE_ASSIGNED';
      return false;
    });
    return e?.remarks ? String(e.remarks) : '';
  }

  getCapfExtraStageApprovedVia(type: string | undefined): string {
    if (this.getCapfExtraStageStatus(type) !== 'APPROVED') return '';
    const t = (type || '').toString().toLowerCase();
    const e = this.findApprovalHistoryEntry((x: any) => {
      const action = (x.action || x.status || '').toString().toUpperCase();
      const desig = (x.designation || x.txtDesignation || x.departmentName || '').toString().toUpperCase();
      if (t === 'capf_ceo') return action === 'APPROVED' && desig.includes('CEO');
      if (t === 'capf_asset_code') return action === 'ASSET_CODE_ASSIGNED' || (action === 'APPROVED' && desig.includes('FINANCE'));
      if (t === 'capf_pr_code') return action === 'PR_CODE_ASSIGNED';
      if (t === 'capf_po_code') return action === 'PO_CODE_ASSIGNED';
      return false;
    });
    return (e?.approvedIp ?? e?.approvedVia ?? '') || '';
  }

  getDepartmentApproverIp(pipelineOrder: number, departmentId: number | undefined, approverId: number): string {
    if (!departmentId) return '';
    if (this.isSentBackToInitiatorState()) return '';
    if (this.getDepartmentHeadStatus(pipelineOrder, departmentId, approverId) !== 'APPROVED') return '';
    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel ?? 0;
    const isVirtualInitiatorStage = pipelineOrder < 0;
    const pend = this.getCapfPendingExclusiveMinHistoryLevelFe();
    if (this.isCapfForm() && pend != null && !isVirtualInitiatorStage && pipelineOrder > pend) return '';
    if (!this.isCapfForm() && !isVirtualInitiatorStage && pipelineOrder > currentLevel) return '';
    const entry = this.getStageHeadHistoryEntry(pipelineOrder, departmentId, approverId);
    return (entry?.approvedIp ?? entry?.approvedVia ?? '') || '';
  }

  getCardTimeTaken(cardIndex: number, cards: Array<{ pipeline: any; pipelineIndex: number; approverId: number | null }>): string {
    if (this.isSentBackToInitiatorState()) return '';
    if (!cards || cardIndex <= 0 || cardIndex >= cards.length) return '';

    const curr = cards[cardIndex];
    const prev = cards[cardIndex - 1];
    const currOrder = this.getCardPipelineOrder(curr);
    const prevOrder = this.getCardPipelineOrder(prev);

    // Multiple HOD cards at the same stage are parallel; no duration between them.
    if (currOrder === prevOrder) return '';

    let prevEndMs = 0;
    for (let i = 0; i < cardIndex; i++) {
      if (this.getCardPipelineOrder(cards[i]) !== prevOrder) continue;
      const ts = this.getCardApprovalTimestamp(cards[i]);
      if (ts != null) prevEndMs = Math.max(prevEndMs, ts);
    }

    // First card of a new stage: measure from previous stage completion to this card's approval.
    const currStartMs = this.getCardApprovalTimestamp(curr);
    if (!prevEndMs || !currStartMs || currStartMs < prevEndMs) return '';

    return this.formatDuration(currStartMs - prevEndMs);
  }

  private getCardPipelineOrder(card: { pipeline: any; pipelineIndex: number }): number {
    return card.pipeline?.intApprovalOrder ?? (card.pipelineIndex + 1);
  }

  private getCardApprovalTimestamp(card: { pipeline: any; pipelineIndex: number; approverId: number | null }): number | null {
    if (this.isCapfExtraPipeline(card.pipeline)) {
      const at = this.getCapfExtraStageApprovedAt(card.pipeline?.type);
      if (!at) return null;
      const ts = new Date(at).getTime();
      return !isNaN(ts) && ts > 0 ? ts : null;
    }

    const order = this.getCardPipelineOrder(card);
    const deptId = this.getPipelineDepartmentId(card.pipeline);

    if (card.approverId != null && deptId != null) {
      const headEntry = this.getStageHeadHistoryEntry(order, deptId, card.approverId);
      if (headEntry?.approvedDate) {
        const ts = this.getApprovalEntryTime(headEntry);
        return ts > 0 ? ts : null;
      }
    }

    if (deptId != null) {
      const stageEntry = this.getStageHistoryEntry(order, deptId);
      if (stageEntry?.approvedDate) {
        const ts = this.getApprovalEntryTime(stageEntry);
        return ts > 0 ? ts : null;
      }
    }

    // CAPF Project routing may store a different departmentId in history than the displayed substitute dept.
    if (this.isCapfForm()) {
      const byLevel = this.getStageHistoryEntry(order, undefined);
      if (byLevel?.approvedDate) {
        const ts = this.getApprovalEntryTime(byLevel);
        if (ts > 0) return ts;
      }

      if (card.approverId != null) {
        const byLevelUser = this.findCapfHistoryEntryByLevelAndApprover(order, card.approverId);
        if (byLevelUser?.approvedDate) {
          const ts = this.getApprovalEntryTime(byLevelUser);
          if (ts > 0) return ts;
        }
      }
    }

    if (order < 0) {
      const created = this.applicationDetails?.dteCreatedDate || this.applicationDetails?.createdAt;
      if (created) {
        const ts = new Date(created).getTime();
        if (!isNaN(ts) && ts > 0) return ts;
      }
    }

    return null;
  }

  private findCapfHistoryEntryByLevelAndApprover(pipelineOrder: number, approverId: number): any | null {
    if (!this.approvalHistory?.length) return null;
    const allowedLevels = this.getAllowedHistoryLevelsForStage(pipelineOrder);
    const candidates = this.approvalHistory.filter((e: any) => {
      if (!e) return false;
      const action = (e.action || e.status || '').toString().toUpperCase();
      if (action === 'SENT_BACK' || action === 'SENT_BACK_TO_INITIATOR') return false;
      if (action !== 'APPROVED' && action !== 'REJECTED') return false;
      const entryLevel = e.level ?? e.intApprovalOrder;
      if (entryLevel == null || !allowedLevels.includes(Number(entryLevel))) return false;
      const entryApprover = e.approvedBy ?? e.userId ?? e.userApproverId;
      return entryApprover != null && Number(entryApprover) === Number(approverId);
    });
    if (!candidates.length) return null;
    candidates.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    return candidates[0];
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

  private isCapfTechnicalExpertProjectMode(): boolean {
    const mode = this.readCapfFieldValueByKeywords(['technical', 'expect'])
      || this.readCapfFieldValueByKeywords(['technical', 'expert']);
    return mode.toLowerCase() === 'project';
  }

  private getCapfSubstituteDepartmentId(): number | null {
    const virtualSubstitute = this.applicationFormData?.['project_substitute_department'];
    const raw = (virtualSubstitute != null && String(virtualSubstitute).trim() !== '')
      ? String(virtualSubstitute).trim()
      : this.readCapfFieldValueByKeywords(['substitut'])
      || this.readCapfFieldValueByKeywords(['substitute', 'department'])
      || this.readCapfFieldValueByKeywords(['project', 'department']);
    if (!raw) return null;
    const asNumber = Number(raw);
    if (Number.isFinite(asNumber) && asNumber > 0) {
      return asNumber;
    }
    const target = this.normalizeLookupText(raw);
    for (const [deptId, deptName] of this.departmentNameMap.entries()) {
      if (this.normalizeLookupText(deptName) === target) {
        return Number(deptId);
      }
    }
    return null;
  }

  private isTechnicalExpertPipelineStage(pipeline: any): boolean {
    if (!pipeline) return false;
    const deptId = pipeline?.hrTblDepartment?.serDepartmentId ?? pipeline?.serDepartmentId ?? pipeline?.departmentId;
    const deptName = pipeline?.hrTblDepartment?.txtDepartmentName
      || pipeline?.departmentName
      || pipeline?.txtDepartmentName
      || (deptId != null ? this.departmentNameMap.get(Number(deptId)) : '');
    const normalized = this.normalizeLookupText(String(deptName || ''));
    return normalized.includes('technical') && normalized.includes('expert');
  }

  private readCapfFieldValueByKeywords(keywords: string[]): string {
    if (!this.applicationDetails || !Array.isArray(this.formFields) || !keywords?.length) return '';
    const lowerKeywords = keywords.map((k) => this.normalizeLookupText(k));
    for (const field of this.formFields) {
      const label = String(field?.label || '');
      const normalizedLabel = this.normalizeLookupText(label);
      const matches = lowerKeywords.every((k) => normalizedLabel.includes(k));
      if (!matches) continue;
      const value = this.getFieldValue(field);
      if (value == null) continue;
      const text = String(value).trim();
      if (text) return text;
    }
    const data = this.applicationFormData || {};
    for (const [key, value] of Object.entries(data)) {
      const normalizedKey = this.normalizeLookupText(key);
      const matches = lowerKeywords.every((k) => normalizedKey.includes(k));
      if (!matches || value == null) continue;
      const text = String(value).trim();
      if (text) return text;
    }
    return '';
  }

  private normalizeLookupText(value: string): string {
    return String(value || '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, ' ')
      .trim();
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

  /** Full reset marker: latest explicit send-back-to-initiator action. */
  private getSendBackToInitiatorResetTime(): number {
    const combined = [
      ...(Array.isArray(this.approvalHistory) ? this.approvalHistory : []),
      ...(Array.isArray(this.priorApprovalHistory) ? this.priorApprovalHistory : [])
    ];
    if (combined.length === 0) return 0;
    const sorted = [...combined].sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));
    for (const e of sorted) {
      const action = (e.action || e.status || '').toString().toUpperCase();
      if (action === 'SENT_BACK_TO_INITIATOR') {
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

  private isSentBackToInitiatorState(): boolean {
    return (this.applicationDetails?.txtStatus || '').toString().toUpperCase() === 'SENT_BACK_TO_INITIATOR';
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
    const d =
      e?.approvedDate ??
      e?.sentBackDate ??
      e?.actionDate ??
      e?.createdAt ??
      e?.updatedAt;
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

    const levelEq = (e: any, order: number): boolean => {
      const raw = e.level ?? e.intApprovalOrder;
      if (raw == null) return false;
      return Number(raw) === Number(order);
    };
    const deptEq = (e: any, dept?: number): boolean => {
      if (dept == null) return true;
      if (e.departmentId == null) return false;
      return Number(e.departmentId) === Number(dept);
    };

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
      candidates = matches((e) => levelEq(e, pipelineOrder) && deptEq(e, departmentId));
    }
    // Never fall back to "level only" when a department is known: same approver can act at multiple
    // stages (initiator + another HoD) and would otherwise show the wrong row on later cards.
    if (candidates.length === 0 && !departmentId) {
      candidates = matches((e) => levelEq(e, pipelineOrder));
    }
    if (candidates.length === 0 && departmentId && pipelineOrder < 0) {
      // Virtual CAPF initiator stage may be represented by a different "level" in stored history.
      // For real (non-negative) stages, avoid department-only fallback because it can mark
      // a later stage as approved using another stage's history.
      candidates = matches((e) => deptEq(e, departmentId));
    }
    if (candidates.length === 0) return null;
    // Sort by approvedDate descending and return the latest approval
    candidates.sort((a, b) => this.getApprovalEntryTime(b) - this.getApprovalEntryTime(a));

    if (this.isCapfForm() && pipelineOrder >= 0) {
      const resetToInitiatorT = this.getSendBackToInitiatorResetTime();
      if (resetToInitiatorT > 0) {
        const afterReset = candidates.filter((c) => this.getApprovalEntryTime(c) > resetToInitiatorT);
        if (afterReset.length === 0) return null;
        return afterReset[0];
      }
    }

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

    // Keep all approval cards reset after "send back to initiator" until re-submission starts a new cycle.
    if (overallStatus === 'SENT_BACK_TO_INITIATOR' && !isVirtualInitiatorStage) {
      return 'PENDING';
    }

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
    if (this.isSentBackToInitiatorState()) return '';
    // 1. History only when this stage is fully approved (avoid same user’s earlier step bleeding in)
    if (this.isDepartmentApproved(pipelineOrder)) {
      const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
      if (entry) {
        return entry.approverName || entry.approvedByName || entry.userName || '';
      }
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
    if (this.isSentBackToInitiatorState()) return '';
    if (!this.isDepartmentApproved(pipelineOrder)) return '';
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
    const approvedPipelineStages = pipelines.filter((p: any, i: number) =>
      this.getStageStatus(p.intApprovalOrder || (i + 1), p.hrTblDepartment?.serDepartmentId) === 'APPROVED'
    ).length;
    let totalStages = pipelines.length;
    let approvedStages = approvedPipelineStages;
    if (this.shouldShowCapfExtraStages()) {
      const capfExtraTypes = ['capf_ceo', 'capf_asset_code', 'capf_pr_code', 'capf_po_code'];
      totalStages += capfExtraTypes.length;
      approvedStages += capfExtraTypes.filter((type) => this.getCapfExtraStageStatus(type) === 'APPROVED').length;
    }
    return totalStages > 0 ? Math.round((approvedStages / totalStages) * 100) : 0;
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
    if (status === 'CEO_PENDING' || status === 'ASSET_PENDING' || status === 'PR_PENDING' || status === 'PO_PENDING' || status === 'PO_VENDOR_TE_PENDING') return true;

    const currentLevel = this.applicationDetails?.intCurrentApprovalLevel;
    if (typeof currentLevel === 'number' && currentLevel < 0) return true; // CAPF often starts at -1

    if (this.applicationDetails?.txtAssetCode || this.applicationDetails?.txtPrCode || this.applicationDetails?.txtPoCode) return true;

    if (Array.isArray(this.approvalHistory) && this.approvalHistory.length > 0) {
      const hasCapfStyleEntry = this.approvalHistory.some((e: any) => {
        const action = (e?.action || e?.status || '').toString().toUpperCase();
        const role = (e?.role || e?.designation || e?.txtDesignation || e?.departmentName || '').toString().toUpperCase();
        return action === 'PR_CODE_ASSIGNED' || action === 'PO_CODE_ASSIGNED' || role.includes('CEO') || role.includes('FINANCE');
      });
      if (hasCapfStyleEntry) return true;
    }

    return false;
  }

  /** Saved expense claims (EXP-*): same slip preview as create-flow live preview. */
  isExpenseClaimForm(): boolean {
    if (!this.applicationDetails) return false;
    const appCode = (this.applicationDetails.txtFormCode || '').trim().toUpperCase();
    if (appCode.startsWith('EXP-')) return true;
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '')
      .replace(/\s+/g, ' ')
      .toLowerCase();
    if (name.includes('expense claim')) return true;
    const cfgCode = (this.applicationDetails.cfgTblCustomForm?.txtFormCode || '').trim().toUpperCase();
    if (cfgCode.startsWith('EXP-')) return true;
    const formId = this.applicationDetails.serFormId;
    if (formId && this.forms?.length) {
      const form = this.forms.find((f: any) => f.serFormId === formId);
      const fc = (form?.txtFormCode || form?.cfgTblCustomForm?.txtFormCode || '').trim().toUpperCase();
      const fn = (form?.txtFormName || form?.cfgTblCustomForm?.txtFormName || '').toLowerCase();
      if (fc.startsWith('EXP-') || fn.includes('expense claim')) return true;
    }
    return false;
  }

  getExpenseClaimLinesForPreview(): Array<{ description?: string; deptName?: string; sign?: string; amount?: string }> {
    const raw = this.applicationFormData?.expenseClaimLines;
    if (!Array.isArray(raw)) return [];
    return raw.map((row: any) => ({
      description: row?.description != null ? String(row.description) : '',
      deptName: row?.deptName != null ? String(row.deptName) : row?.deptt_name != null ? String(row.deptt_name) : '',
      sign: row?.sign != null ? String(row.sign) : '',
      amount: row?.amount != null ? String(row.amount) : ''
    }));
  }

  trackByExpenseClaimDetailIndex(index: number): number {
    return index;
  }

  getExpenseClaimSlipFieldDisplay(value: string | undefined | null): string {
    const v = (value ?? '').trim();
    return v.length > 0 ? v : '\u00a0';
  }

  getExpenseClaimHeaderPreviewValue(...needles: string[]): string {
    return this.getExpenseClaimSlipFieldDisplay(this.getExpenseClaimHeaderRawValue(...needles));
  }

  private getExpenseClaimHeaderRawValue(...needles: string[]): string {
    if (!this.formFields?.length || !needles.length) return '';
    const loweredNeedles = needles.map((n) => n.toLowerCase());
    for (const field of this.formFields) {
      const label = (field.label || '').toLowerCase();
      if (!loweredNeedles.some((needle) => label.includes(needle))) continue;
      const value = this.getFieldValue(field);
      if (value !== undefined && value !== null && String(value).trim().length > 0) {
        return String(value);
      }
    }
    return '';
  }

  getExpenseClaimPreviewCell(value: string | undefined | null): string {
    const v = (value ?? '').trim();
    return v.length > 0 ? v : '\u00a0';
  }

  getExpenseClaimAmountTotal(): string {
    const lines = this.applicationFormData?.expenseClaimLines;
    if (!Array.isArray(lines)) return '';
    let sum = 0;
    for (const row of lines) {
      const raw = String(row?.amount ?? '').trim().replace(/,/g, '');
      const n = parseFloat(raw);
      if (!Number.isNaN(n)) sum += n;
    }
    if (sum === 0 && !lines.some((r: any) => String(r?.amount ?? '').trim() !== '')) {
      return '';
    }
    return sum.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  }

  getExpenseClaimTotalCellDisplay(): string {
    const t = this.getExpenseClaimAmountTotal();
    return t && t.trim().length > 0 ? t : '\u00a0';
  }

  isTemporaryAdvanceSlipForm(): boolean {
    if (!this.applicationDetails) return false;
    const appCode = (this.applicationDetails.txtFormCode || '').trim().toUpperCase();
    if (appCode.startsWith('TAS-')) return true;
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '')
      .replace(/\s+/g, ' ')
      .toLowerCase();
    if (name.includes('temporary advance')) return true;
    const cfgCode = (this.applicationDetails.cfgTblCustomForm?.txtFormCode || '').trim().toUpperCase();
    if (cfgCode.startsWith('TAS-')) return true;
    const formId = this.applicationDetails.serFormId;
    if (formId && this.forms?.length) {
      const form = this.forms.find((f: any) => f.serFormId === formId);
      const fc = (form?.txtFormCode || form?.cfgTblCustomForm?.txtFormCode || '').trim().toUpperCase();
      const fn = (form?.txtFormName || form?.cfgTblCustomForm?.txtFormName || '').toLowerCase();
      if (fc.startsWith('TAS-') || fn.includes('temporary advance')) return true;
    }
    return false;
  }

  getTemporaryAdvanceSlipSlipFieldDisplay(value: string | undefined | null): string {
    const v = (value ?? '').trim();
    return v.length > 0 ? v : '\u00a0';
  }

  getTemporaryAdvanceSlipPreviewValue(...needles: string[]): string {
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(this.getTemporaryAdvanceSlipHeaderRawValue(...needles));
  }

  getTemporaryAdvanceSlipPurposeDisplay(): string {
    const raw = this.getTemporaryAdvanceSlipHeaderRawValue('for the purpose', 'purpose of', 'purpose');
    return raw.trim().length > 0 ? raw : '\u00a0';
  }

  private getTemporaryAdvanceSlipHeaderRawValue(...needles: string[]): string {
    if (!this.formFields?.length || !needles.length) return '';
    const loweredNeedles = needles.map((n) => n.toLowerCase());
    for (const field of this.formFields) {
      const label = (field.label || '').toLowerCase();
      if (!loweredNeedles.some((needle) => label.includes(needle))) continue;
      const value = this.getFieldValue(field);
      if (value !== undefined && value !== null && String(value).trim().length > 0) {
        return String(value);
      }
    }
    return '';
  }

  getTemporaryAdvanceSlipMetaDivision(): string {
    const v = this.getTemporaryAdvanceSlipHeaderRawValue('division').trim();
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(v || 'Finance');
  }

  getTemporaryAdvanceSlipMetaDepartment(): string {
    const v = this.getTemporaryAdvanceSlipHeaderRawValue('department', 'dept').trim();
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(v || 'Book Keeping');
  }

  getTemporaryAdvanceSlipMetaSection(): string {
    const v = this.getTemporaryAdvanceSlipHeaderRawValue('section').trim();
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(v || '***');
  }

  getTemporaryAdvanceSlipMetaDocumentNo(): string {
    const v = this.getTemporaryAdvanceSlipHeaderRawValue('document no', 'fin-bkp', 'form number').trim();
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(v || 'FIN-BKP-FM-06');
  }

  getTemporaryAdvanceSlipMetaOriginalIssue(): string {
    const v = this.getTemporaryAdvanceSlipHeaderRawValue('original issue').trim();
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(v || '01-06-2006');
  }

  getTemporaryAdvanceSlipMetaRev(): string {
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(
      this.getTemporaryAdvanceSlipHeaderRawValue('rev #', 'rev.', 'revision')
    );
  }

  getTemporaryAdvanceSlipMetaRevDate(): string {
    return this.getTemporaryAdvanceSlipSlipFieldDisplay(
      this.getTemporaryAdvanceSlipHeaderRawValue('rev. date', 'revision date', 'rev date')
    );
  }

  getTemporaryAdvanceSlipDateLine(): string {
    const v = this.getTemporaryAdvanceSlipHeaderRawValue('slip date', 'dated', 'form date', 'advance date').trim();
    if (v) {
      return this.getTemporaryAdvanceSlipSlipFieldDisplay(v);
    }
    const d = this.applicationDetails?.dteCreatedDate;
    if (d) {
      try {
        const formatted = new Date(d).toLocaleDateString('en-GB', {
          day: '2-digit',
          month: 'short',
          year: 'numeric'
        });
        return this.getTemporaryAdvanceSlipSlipFieldDisplay(formatted.replace(/ /g, '-'));
      } catch {
        return '\u00a0';
      }
    }
    return '\u00a0';
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
    if (status === 'CEO_PENDING' || status === 'ASSET_PENDING' || status === 'PO_PENDING' || status === 'PO_VENDOR_TE_PENDING') return true;

    const hasAsset = !!(this.applicationDetails?.txtAssetCode && String(this.applicationDetails.txtAssetCode).trim());
    const hasPr = !!(this.applicationDetails?.txtPrCode && String(this.applicationDetails.txtPrCode).trim());
    const hasPo = !!(this.applicationDetails?.txtPoCode && String(this.applicationDetails.txtPoCode).trim());
    if (hasAsset || hasPr || hasPo) return true;

    if (Array.isArray(this.approvalHistory) && this.approvalHistory.length > 0) {
      const hasPrAction = this.approvalHistory.some((e: any) =>
        (e?.action || e?.status || '').toString().toUpperCase() === 'PR_CODE_ASSIGNED'
      );
      const hasPoAction = this.approvalHistory.some((e: any) =>
        (e?.action || e?.status || '').toString().toUpperCase() === 'PO_CODE_ASSIGNED'
      );
      if (hasPrAction || hasPoAction) return true;
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

    let element: HTMLElement | null = null;
    let elementId = '';
    if (this.isCapfForm()) {
      elementId = 'capf-pdf-content';
      element = document.getElementById(elementId);
    } else if (this.isBudgetApprovalForm()) {
      elementId = 'budget-pdf-content';
      element = document.getElementById(elementId);
    } else {
      element =
        (this.previewScale?.nativeElement.querySelector('.app-preview-pages') as HTMLElement | null) ||
        this.previewScale?.nativeElement ||
        document.getElementById('generic-pdf-content');
      elementId = element?.id || 'application-details-preview';
    }

    if (!element) {
      console.error('Content element not found:', elementId);
      this.notificationService.showMessage('Content to generate PDF not found', 'danger');
      this.isGeneratingPdf = false;
      return null;
    }

    const filename = `${this.applicationDetails?.txtFormCode || 'application'}.pdf`;

    const finishPdfGeneration = (pdfBlob: Blob): Blob => {
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
    };

    try {
      // Dynamically import html2canvas and jsPDF
      const [html2canvasModule, jsPDFModule] = await Promise.all([
        import('html2canvas'),
        import('jspdf')
      ]);

      const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
      const jsPDF = (jsPDFModule.default || jsPDFModule) as any;
      const isCapf = this.isCapfForm();

      // Match /application exactly: capture the live preview DOM first for all non-CAPF forms.
      if (!isCapf) {
        this.rebuildGenericPreviewPages(true);
        this.requestPreviewFit();
        await new Promise((resolve) => setTimeout(resolve, 80));

        const previewScaleEl = this.previewScale?.nativeElement || null;
        const previewCaptureTarget =
          (previewScaleEl?.querySelector('.tas-slip-preview-root') as HTMLElement | null) ||
          (previewScaleEl?.querySelector('.expense-claim-preview-root') as HTMLElement | null) ||
          previewScaleEl ||
          element;

        if (previewCaptureTarget?.classList.contains('tas-slip-preview-root')) {
          return finishPdfGeneration(
            await this.applicationPdfService.renderXyzHostElementToPdf(previewCaptureTarget)
          );
        }

        if (previewCaptureTarget?.classList.contains('expense-claim-preview-root')) {
          return finishPdfGeneration(
            await this.applicationPdfService.renderXyzHostElementToPdf(previewCaptureTarget)
          );
        }

        if (previewCaptureTarget) {
          return finishPdfGeneration(
            await this.applicationPdfService.renderExactPreviewToPdfBlob(previewCaptureTarget)
          );
        }
      }

      // Capture from an offscreen clone so on-screen preview never changes.
      const captureHost = document.createElement('div');
      captureHost.style.position = 'fixed';
      captureHost.style.left = '-100000px';
      captureHost.style.top = '0';
      captureHost.style.opacity = '0';
      captureHost.style.pointerEvents = 'none';
      captureHost.style.zIndex = '-1';
      const captureRoot = element.cloneNode(true) as HTMLElement;
      captureHost.appendChild(captureRoot);
      document.body.appendChild(captureHost);

      // ── Temporarily strip visual noise before screenshot ──────────────
      // Collect all .page elements inside the target and remove their
      // min-height (which adds huge empty space) and border.
      const pageEls = Array.from(captureRoot.querySelectorAll('.page')) as HTMLElement[];
      const paperEls = Array.from(captureRoot.querySelectorAll('.xyz-paper')) as HTMLElement[];

      // Also strip the element itself if it has a border/min-height
      const savedElementStyles: { el: HTMLElement; minHeight: string; maxHeight: string; border: string; boxShadow: string; overflow: string }[] = [];
      if (!isCapf) {
        savedElementStyles.push(
          ...[...pageEls, ...paperEls, captureRoot].map(el => {
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

      const captureTarget = (captureRoot.querySelector('.page') as HTMLElement) || captureRoot;
      const hadPdfCapture = captureRoot.classList.contains('pdf-capture');
      const hadPdfFix = captureRoot.classList.contains('pdf-fix');
      if (!hadPdfCapture && !isCapf) {
        captureRoot.classList.add('pdf-capture');
      }
      if (!hadPdfFix && isCapf) {
        captureRoot.classList.add('pdf-fix');
      }

      const emptyLineSnapshots: { el: HTMLElement; html: string }[] = [];
      const boxcheckSnapshots: { el: HTMLElement; transform: string }[] = [];
      const boxcheckSpanSnapshots: { el: HTMLElement; transform: string }[] = [];
      const sbSubSnapshots: { el: HTMLElement; textAlign: string; width: string; display: string; paddingRight: string; boxSizing: string; marginLeft: string }[] = [];
      if (isCapf) {
        captureRoot.querySelectorAll('.line, .date-line, .inline-line').forEach((el) => {
          const ht = el as HTMLElement;
          if ((ht.textContent || '').trim() === '') {
            emptyLineSnapshots.push({ el: ht, html: ht.innerHTML });
            ht.innerHTML = '<span class="pdf-empty">&nbsp;</span>';
          }
        });

        captureRoot.querySelectorAll('.boxcheck').forEach((el) => {
          const ht = el as HTMLElement;
          boxcheckSnapshots.push({ el: ht, transform: ht.style.transform });
          ht.style.transform = 'translateY(6px)';
        });
        captureRoot.querySelectorAll('.boxcheck > span').forEach((el) => {
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
        ? (Array.from(captureRoot.querySelectorAll('.xyz-paper-page')) as HTMLElement[])
            .filter((paper: HTMLElement) => {
              const style = window.getComputedStyle(paper);
              if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
                return false;
              }
              const rect = paper.getBoundingClientRect();
              return rect.width > 0 && rect.height > 0;
            })
        : [];
      const targets = pagesForPdf.length > 0 ? pagesForPdf : [captureTarget];
      const PDF_WIDTH = 210;
      const PDF_HEIGHT = 297;
      let firstPdfPage = true;

      for (let i = 0; i < targets.length; i++) {
        const target = targets[i];
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

        // Chrome-like page sequence: slice captured canvas into true A4-height snapshots.
        const pageSliceHeightPx = Math.max(Math.round((canvas.width * PDF_HEIGHT) / PDF_WIDTH), 1);
        const totalSlices = Math.max(Math.ceil(canvas.height / pageSliceHeightPx), 1);

        for (let sliceIndex = 0; sliceIndex < totalSlices; sliceIndex++) {
          const srcY = sliceIndex * pageSliceHeightPx;
          const remainingHeight = canvas.height - srcY;
          const sliceHeightPx = Math.max(Math.min(pageSliceHeightPx, remainingHeight), 1);
          // Ignore tiny tail slices that render as visually blank extra pages.
          if (sliceHeightPx < 24) {
            continue;
          }

          const pageCanvas = document.createElement('canvas');
          pageCanvas.width = canvas.width;
          pageCanvas.height = sliceHeightPx;
          const pageCtx = pageCanvas.getContext('2d');
          if (!pageCtx) {
            throw new Error('Failed to create page canvas context');
          }
          pageCtx.fillStyle = '#ffffff';
          pageCtx.fillRect(0, 0, pageCanvas.width, pageCanvas.height);
          pageCtx.drawImage(canvas, 0, srcY, canvas.width, sliceHeightPx, 0, 0, canvas.width, sliceHeightPx);

          const renderedHeightMm = (sliceHeightPx * PDF_WIDTH) / canvas.width;
          const imgData = pageCanvas.toDataURL('image/jpeg', 0.98);

          if (!firstPdfPage) {
            pdf.addPage('a4', 'portrait');
          }
          pdf.addImage(imgData, 'JPEG', 0, 0, PDF_WIDTH, renderedHeightMm);
          firstPdfPage = false;
        }
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
        captureRoot.classList.remove('pdf-capture');
      }
      if (!hadPdfFix && isCapf) {
        captureRoot.classList.remove('pdf-fix');
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
      if (captureHost.parentNode) {
        captureHost.parentNode.removeChild(captureHost);
      }

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
