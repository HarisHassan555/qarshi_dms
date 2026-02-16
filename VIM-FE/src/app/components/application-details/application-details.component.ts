import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';

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

  isBudgetApprovalForm(): boolean {
    if (!this.applicationDetails) return false;
    const name = (this.applicationDetails.cfgTblCustomForm?.txtFormName || this.applicationDetails.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (this.applicationDetails.txtFormCode || '').toUpperCase();
    return name === 'BUDGET APPROVAL FORM' || name.includes('BUDGET APPROVAL') || code.startsWith('BDG');
  }

  formatUserForSignature(selectedUsers: any[], index: number): string {
    if (!selectedUsers || !selectedUsers[index]) return '';
    const user = selectedUsers[index];
    const role = user.cfgTblRole?.txtRoleName || 'Reviewer';
    return `${user.txtUserName}<br>(${role})`;
  }
}

