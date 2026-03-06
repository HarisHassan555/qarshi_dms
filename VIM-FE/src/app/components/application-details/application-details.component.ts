import { Component, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
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
  departmentHeadMap: Map<number, number> = new Map();
  userNameMap: Map<number, string> = new Map();
  currentUser: any = null;

  // PDF Generation
  isGeneratingPdf: boolean = false;
  pdfBlobUrl: string | null = null;

  // Approval Summary Modal
  showSummaryModal: boolean = false;
  fromPendingApprovals: boolean = false;
  selectedApplicationForRemarks: any = null;
  remarksText: string = '';
  isApproving: boolean = false;
  isRejecting: boolean = false;
  isSendingBack: boolean = false;
  showFeasibilityModal: boolean = false;
  feasibilityPreviewUrl: any = null;
  private feasibilityObjectUrl: string | null = null;

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
  formHeading: string = 'Budget Approval Form';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private customFormApplicationService: CustomFormApplicationService,
    private customFormService: CustomFormService,
    private departmentService: DepartmentService,
    private userService: UserService,
    private notificationService: NotificationService,
    private sanitizer: DomSanitizer
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
              headMap.set(Number(id), Number(headId));
            }
          });
          this.departmentNameMap = map;
          this.departmentHeadMap = headMap;
          this.enrichPipelineWithDepartmentNames();
          this.applyDepartmentNamesToApprovalHistory();
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
          if (data.txtApplicationData) {
            try {
              this.applicationFormData = JSON.parse(data.txtApplicationData);
              const budgetApprovers = this.extractBudgetApprovers();
              this.preparedBy = budgetApprovers.preparedBy;
              this.reviewers = budgetApprovers.reviewers;
              this.recommenders = budgetApprovers.recommenders;
              this.approver = budgetApprovers.approver;

              // Prepare budget approval specific data
              if (this.isBudgetApprovalForm()) {
                const content =
                  this.applicationFormData?.content ||
                  this.applicationFormData?.editorContent ||
                  this.applicationFormData?.htmlContent ||
                  this.applicationFormData?.description ||
                  '';
                this.safeContent = this.sanitizer.bypassSecurityTrustHtml(content);
                this.formHeading = this.applicationFormData.heading || this.applicationDetails.cfgTblCustomForm?.txtFormName || 'Budget Approval Form';
              }
            } catch (e) {
              console.error('Error parsing application data:', e);
              this.applicationFormData = {};
            }
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

    return value;
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

    const budgetPipelines = this.getBudgetUserPipelines();
    if (!this.isCapfForm() && budgetPipelines.length > 0) {
      return budgetPipelines;
    }

    console.log('Sorted pipelines:', sortedPipelines);
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

  openSummaryModal() {
    this.showSummaryModal = true;
  }

  closeSummaryModal() {
    this.showSummaryModal = false;
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

    // Do not auto-generate/upload PDF on approve.

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

    // Budget Approval form is person-driven, not department-level.
    // Match history by assigned stage user first to avoid level-shift issues.
    if (!this.isCapfForm() && this.hasBudgetApproverSequence()) {
      const budgetEntry = this.getBudgetStageHistoryEntry(pipelineOrder);
      if (budgetEntry) return budgetEntry;
    }

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

  private getBudgetStageHistoryEntry(pipelineOrder: number): any {
    const assignedUser = this.getBudgetUserForStage(pipelineOrder);
    if (!assignedUser || !Array.isArray(this.approvalHistory) || this.approvalHistory.length === 0) {
      return null;
    }

    const assignedUserId = this.getUserId(assignedUser);
    const assignedUserName = this.resolveBudgetUserName(assignedUser).toLowerCase();

    const byActorId = (entry: any): boolean => {
      if (assignedUserId == null) return false;
      const actorId = entry?.approvedBy ?? entry?.approverUserId ?? entry?.userId ?? entry?.actorUserId;
      return actorId != null && Number(actorId) === Number(assignedUserId);
    };

    const byActorName = (entry: any): boolean => {
      if (!assignedUserName) return false;
      const actorName = String(
        entry?.approverName ??
        entry?.userName ??
        entry?.approvedByName ??
        entry?.actorName ??
        ''
      ).trim().toLowerCase();
      return !!actorName && actorName === assignedUserName;
    };

    const exactById = this.approvalHistory.find((e: any) => byActorId(e));
    if (exactById) return exactById;

    const exactByName = this.approvalHistory.find((e: any) => byActorName(e));
    if (exactByName) return exactByName;

    // Fallback by level when actor identity isn't available in history.
    return this.approvalHistory.find((e: any) => Number(e?.level) === Number(pipelineOrder)) || null;
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
    if (this.hasBudgetApproverSequence()) {
      const budgetUser = pipeline?.budgetUserName || this.getBudgetUserNameForStage(pipeline?.intApprovalOrder || (index + 1));
      if (budgetUser) return budgetUser;
    }
    if (!pipeline) return `Department ${index + 1}`;
    const directName =
      pipeline.hrTblDepartment?.txtDepartmentName ||
      pipeline.departmentName ||
      pipeline.txtDepartmentName;
    if (directName) return directName;

    const order = pipeline.intApprovalOrder || (index + 1);
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
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.approverName || entry.approvedBy || entry.userName || '';
    }
    if (this.hasBudgetApproverSequence()) {
      const budgetUser = this.getBudgetUserNameForStage(pipelineOrder);
      if (budgetUser) return budgetUser;
    }
    if (departmentId != null) {
      const headId = this.departmentHeadMap.get(Number(departmentId));
      if (headId != null) {
        const headName = this.userNameMap.get(Number(headId));
        if (headName) return headName;
      }
    }
    return '';
  }

  private getCapfUserNameForStage(order: number): string {
    return this.getBudgetUserNameForStage(order);
  }

  private resolveBudgetUserName(u: any): string {
    if (!u) return '';
    if (typeof u === 'string') return u.trim();
    if (typeof u === 'number') return this.userNameMap.get(Number(u)) || '';
    const nestedUser = u.user || u.value || u.selectedUser || null;
    if (nestedUser && nestedUser !== u) {
      const nestedName = this.resolveBudgetUserName(nestedUser);
      if (nestedName) return nestedName;
    }
    const direct =
      u.txtUserName ||
      u.userName ||
      u.username ||
      u.displayName ||
      u.fullName ||
      u.name ||
      u.label ||
      u.text;
    if (direct) return String(direct).trim();
    const id = u.serUserId || u.userId || u.id;
    if (id != null) return this.userNameMap.get(Number(id)) || '';
    return '';
  }

  private getBudgetUsersInOrder(): string[] {
    const rawUsers = this.getBudgetUserSequenceRaw();
    const ordered: string[] = [];
    const pushResolved = (u: any) => {
      const name = this.resolveBudgetUserName(u);
      if (name) ordered.push(name);
    };

    rawUsers.forEach((u: any) => pushResolved(u));

    return ordered;
  }

  private getBudgetUserSequenceRaw(): any[] {
    const footerFields = Array.isArray(this.applicationFormData?.footerFields)
      ? this.applicationFormData.footerFields
      : [];
    if (footerFields.length > 0) {
      const sequenceFromFooter: any[] = [];
      footerFields.forEach((f: any) => {
        const key = (f?.key || '').toString().toLowerCase();
        if (key === 'prepared_by') return;
        const users = Array.isArray(f?.users) ? f.users : [];
        users.forEach((u: any) => {
          if (u !== undefined && u !== null && u !== '') {
            sequenceFromFooter.push(u);
          }
        });
      });
      if (sequenceFromFooter.length > 0) {
        return sequenceFromFooter;
      }
    }

    const sequence: any[] = [];
    const pushUser = (u: any) => {
      if (u === undefined || u === null || u === '') return;
      sequence.push(u);
    };

    pushUser(this.preparedBy);
    const reviewers = Array.isArray(this.reviewers) ? this.reviewers : this.normalizeToArray(this.reviewers);
    reviewers.forEach((u: any) => pushUser(u));
    const recommenders = Array.isArray(this.recommenders) ? this.recommenders : this.normalizeToArray(this.recommenders);
    recommenders.forEach((u: any) => pushUser(u));
    pushUser(this.approver);

    return sequence;
  }

  private getBudgetUserForStage(order: number): any | null {
    if (!order) return null;
    const users = this.getBudgetUserSequenceRaw();
    const idx = Number(order) - 1;
    if (idx < 0 || idx >= users.length) return null;
    return users[idx];
  }

  private getBudgetUserNameForStage(order: number): string {
    if (!order) return '';
    const rawUsers = this.getBudgetUserSequenceRaw();
    const idx = Number(order) - 1;
    if (idx < 0 || idx >= rawUsers.length) return '';
    return this.resolveBudgetUserName(rawUsers[idx]) || `User ${order}`;
  }

  private getBudgetUserPipelines(): any[] {
    const rawUsers = this.getBudgetUserSequenceRaw();
    if (!rawUsers.length) return [];
    return rawUsers.map((user: any, index: number) => ({
      intApprovalOrder: index + 1,
      budgetUser: user,
      budgetUserName: this.resolveBudgetUserName(user) || `User ${index + 1}`
    }));
  }

  getApprovalFlowHintText(): string {
    if (this.hasBudgetApproverSequence()) {
      return 'Individuals with action details and time on arrows';
    }
    return 'Departments with action details and time on arrows';
  }

  private getValueByKeys(source: any, keys: string[]): any {
    if (!source) return null;
    for (const key of keys) {
      if (source[key] !== undefined && source[key] !== null) {
        return source[key];
      }
    }
    return null;
  }

  private normalizeToArray(value: any): any[] {
    if (Array.isArray(value)) return value.filter((v: any) => v != null);
    if (value === undefined || value === null || value === '') return [];
    return [value];
  }

  private extractBudgetApprovers(): { preparedBy: any; reviewers: any[]; recommenders: any[]; approver: any } {
    const data = this.applicationFormData || {};
    const footerFields = Array.isArray(data?.footerFields) ? data.footerFields : [];
    if (footerFields.length > 0) {
      const byKey = (key: string): any => footerFields.find((f: any) => (f?.key || '').toString().toLowerCase() === key);
      const preparedUsers = Array.isArray(byKey('prepared_by')?.users) ? byKey('prepared_by').users : [];
      const reviewedUsers = Array.isArray(byKey('reviewed_by')?.users) ? byKey('reviewed_by').users : [];
      const recommendedUsers = Array.isArray(byKey('recommended_by')?.users) ? byKey('recommended_by').users : [];
      const approvedUsers = Array.isArray(byKey('approved_by')?.users) ? byKey('approved_by').users : [];
      return {
        preparedBy: preparedUsers.length > 0 ? preparedUsers[0] : null,
        reviewers: reviewedUsers,
        recommenders: recommendedUsers,
        approver: approvedUsers.length > 0 ? approvedUsers[0] : null
      };
    }

    const nestedSources = [
      data,
      data.budgetApproval,
      data.budgetApprovalForm,
      data.formData,
      data.data
    ].filter(Boolean);

    const preparedByKeys = ['preparedBy', 'prepared_by', 'preparer', 'preparedby'];
    const reviewersKeys = ['reviewers', 'reviewer', 'reviewedBy', 'reviewed_by'];
    const recommendersKeys = ['recommenders', 'recommender', 'recommendedBy', 'recommended_by'];
    const approverKeys = ['approver', 'approvedBy', 'approved_by', 'finalApprover', 'final_approver'];

    let preparedBy: any = null;
    let reviewers: any[] = [];
    let recommenders: any[] = [];
    let approver: any = null;

    for (const src of nestedSources) {
      if (!preparedBy) preparedBy = this.getValueByKeys(src, preparedByKeys);
      if (reviewers.length === 0) reviewers = this.normalizeToArray(this.getValueByKeys(src, reviewersKeys));
      if (recommenders.length === 0) recommenders = this.normalizeToArray(this.getValueByKeys(src, recommendersKeys));
      if (!approver) approver = this.getValueByKeys(src, approverKeys);
    }

    return { preparedBy, reviewers, recommenders, approver };
  }

  private hasBudgetApproverSequence(): boolean {
    return this.getBudgetUserSequenceRaw().length > 0;
  }

  // Get approved via channel
  getStageApprovedVia(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.approvedVia || entry.channel || entry.via || 'System';
    }
    return '';
  }

  formatApprovedVia(via: string): string {
    if (!via) return '';
    const v = via.toString().trim().toUpperCase();
    if (v === 'EMAIL' || v === 'MAIL') return 'Email';
    if (v === 'SYSTEM' || v === 'TEMPLATE' || v === 'APP') return 'System';
    return via;
  }

  getStageApprovedIp(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (!entry) return '';
    const raw = (
      entry.approvedIp ||
      entry.approvedIpRaw ||
      entry.approverIp ||
      entry.ipAddress ||
      entry.ip ||
      ''
    ).toString().trim();
    return this.normalizeIpToIpv4Display(raw);
  }

  private normalizeIpToIpv4Display(ip: string): string {
    if (!ip) return '';
    let value = ip.split(',')[0]?.trim() || '';
    if (!value) return '';

    if (value === '::1' || value === '0:0:0:0:0:0:0:1') {
      return '127.0.0.1';
    }

    const mapped = value.match(/^::ffff:(\d{1,3}(?:\.\d{1,3}){3})$/i);
    if (mapped && mapped[1]) {
      return mapped[1];
    }

    if (/^\d{1,3}(?:\.\d{1,3}){3}$/.test(value)) {
      return value;
    }

    return value;
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

  getWorkflowSummaryHtml(pipelines: any[]): string {
    if (!pipelines || pipelines.length === 0) {
      return 'No approval pipeline configured for this form.';
    }

    const approved = this.countStages(pipelines, 'APPROVED');
    const rejected = this.countStages(pipelines, 'REJECTED');
    const current = this.countStages(pipelines, 'CURRENT');
    const pending = this.countStages(pipelines, 'PENDING');

    let maxDurationMs = -1;
    let maxDeptName = '';
    let maxDeptStatus = '';

    pipelines.forEach((p: any, i: number) => {
      const dur = this.getStageDurationMs(i, pipelines);
      if (dur !== null && dur > maxDurationMs) {
        maxDurationMs = dur;
        maxDeptName = this.getPipelineDepartmentName(p, i);
        maxDeptStatus = this.getStageStatus(p.intApprovalOrder || (i + 1), p.hrTblDepartment?.serDepartmentId);
      }
    });

    const slowestText = maxDurationMs > -1
      ? `<strong>Slowest:</strong> ${maxDeptName} • ${this.formatDuration(maxDurationMs)}`
      : `<strong>Slowest:</strong> n/a`;

    const currentDept = pipelines.find((p: any, i: number) =>
      this.getStageStatus(p.intApprovalOrder || (i + 1), p.hrTblDepartment?.serDepartmentId) === 'CURRENT'
    );
    const currentDeptName = currentDept ? this.getPipelineDepartmentName(currentDept, pipelines.indexOf(currentDept)) : '';
    const currentText = currentDeptName ? `<strong>Current:</strong> ${currentDeptName}` : `<strong>Current:</strong> n/a`;

    const stats = `<strong>Approved:</strong> ${approved} <strong>Rejected:</strong> ${rejected} <strong>Pending:</strong> ${pending}`;
    return `${currentText} — ${slowestText}. ${stats}`;
  }

  isBudgetApprovalForm(): boolean {
    if (!this.applicationDetails) {
      return this.hasBudgetApproverSequence();
    }
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.txtFormCode || '').toUpperCase();
    if (name === 'BUDGET APPROVAL FORM' || name.includes('BUDGET APPROVAL') || code.startsWith('BDG')) return true;
    return this.hasBudgetApproverSequence();
  }

  isCapfForm(): boolean {
    if (!this.applicationDetails) return false;
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.txtFormCode || '').toUpperCase();
    return name.includes('CAPITAL ASSETS PURCHASE') || name.includes('CAPF') || code.startsWith('CAPF');
  }

  formatUserForSignature(selectedUsers: any[], index: number): string {
    if (!selectedUsers || !selectedUsers[index]) return '';
    const user = selectedUsers[index];
    const role = this.getUserRoleName(user);
    const dept = this.getUserDepartmentName(user);
    const designation = this.getUserDesignation(user);
    const designationLine = designation ? `<br>${designation}` : '';
    const roleLine = role ? `<br>(${role})` : '';
    const deptLine = dept ? `<br>${dept}` : '';
    return `${user.txtUserName}${designationLine}${roleLine}${deptLine}`;
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

  private getUserDesignation(user: any): string {
    if (!user) return '';
    const directDesignation = user.txtDesignation || user.designation || '';
    if (directDesignation) return directDesignation;
    const userId = this.getUserId(user);
    if (!userId || !this.approvalHistory || this.approvalHistory.length === 0) return '';
    const entry = this.approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
    return entry?.txtDesignation || entry?.designation || '';
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
