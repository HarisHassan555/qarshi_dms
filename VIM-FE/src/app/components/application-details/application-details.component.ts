import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';
import { AbcComponent } from '../../pages/abc/abc.component';

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

  // PDF Generation
  isGeneratingPdf: boolean = false;
  pdfBlobUrl: string | null = null;

  // Approval Summary Modal
  showSummaryModal: boolean = false;

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
    private notificationService: NotificationService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit() {
    // Get application ID from route
    this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      this.applicationId = idParam ? parseInt(idParam, 10) : null;
      if (this.applicationId && !isNaN(this.applicationId)) {
        this.loadForms();
        this.loadApplicationDetails();
      } else {
        this.notificationService.showMessage('Invalid application ID', 'danger');
        this.router.navigate(['/applicationsview']);
      }
    });
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

              // Prepare budget approval specific data
              if (this.isBudgetApprovalForm()) {
                const content = this.applicationFormData.content || this.applicationFormData.editorContent || '';
                this.safeContent = this.sanitizer.bypassSecurityTrustHtml(content);
                this.preparedBy = this.applicationFormData.preparedBy;
                this.reviewers = this.applicationFormData.reviewers || [];
                this.recommenders = this.applicationFormData.recommenders || [];
                this.approver = this.applicationFormData.approver;
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

    const sortedPipelines = [...pipelines].sort((a: any, b: any) =>
      (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0)
    );
    console.log('Sorted pipelines:', sortedPipelines);
    return sortedPipelines;
  }

  goBack() {
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

  // Get approver name for a stage
  getStageApproverName(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.approverName || entry.approvedBy || entry.userName || '';
    }
    return '';
  }

  // Get approved via channel
  getStageApprovedVia(pipelineOrder: number, departmentId?: number): string {
    const entry = this.getStageHistoryEntry(pipelineOrder, departmentId);
    if (entry) {
      return entry.approvedVia || entry.channel || entry.via || 'System';
    }
    return '';
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

  isBudgetApprovalForm(): boolean {
    if (!this.applicationDetails) return false;
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.txtFormCode || '').toUpperCase();
    return name === 'BUDGET APPROVAL FORM' || name.includes('BUDGET APPROVAL') || code.startsWith('BDG');
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
    const role = user.cfgTblRole?.txtRoleName || 'Reviewer';
    return `${user.txtUserName}<br>(${role})`;
  }

  async generatePdf() {
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
      return;
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

      const link = document.createElement('a');
      link.href = pdfUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

    } catch (e) {
      console.error('Error generating PDF:', e);
      this.notificationService.showMessage('Error generating PDF', 'danger');
      this.isGeneratingPdf = false;
    }
  }
}
