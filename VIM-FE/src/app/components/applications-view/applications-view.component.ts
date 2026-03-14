import { Component, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, FormArray, Validators, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { PermissionService } from '../../services/shared-data/permission-service';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { ApplicationPdfService } from '../../services/application-pdf/application-pdf.service';
import { NotificationService } from 'src/app/NotificationService';
import { saveAs } from 'file-saver';
import { urls } from 'src/app/utils/urls';
import { firstValueFrom } from 'rxjs';
// @ts-ignore
import html2pdf from 'html2pdf.js';

interface Application {
  serApplicationId?: number;
  serFormId?: number;
  txtFormCode?: string;
  txtApplicationData?: string;
  txtStatus?: string;
  intCurrentApprovalLevel?: number;
  serSubmittedBy?: number;
  dteCreatedDate?: string;
  cfgTblCustomForm?: any;
  formName?: string; // Added for display purposes
  txtRemarks?: string; // Added for download functionality
}

@Component({
  selector: 'app-applications-view',
  templateUrl: './applications-view.component.html',
  styleUrls: ['./applications-view.component.css']
})
export class ApplicationsViewComponent implements OnInit {
  search = '';
  applications: Application[] = [];
  pendingApprovals: Application[] = [];
  forms: any[] = [];
  currentUser: any;
  isDepartmentHead: boolean = false;
  showPendingApprovals: boolean = false;
  selectedApplicationForRemarks: Application | null = null;
  remarksText: string = '';

  cols = [
    { field: 'txtFormCode', title: 'Application Code' },
    { field: 'formName', title: 'Form Name' },
    { field: 'txtStatus', title: 'Status' },
    { field: 'intCurrentApprovalLevel', title: 'Approval Level' },
    { field: 'dteCreatedDate', title: 'Submitted Date' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  pendingApprovalCols = [
    { field: 'txtFormCode', title: 'Application Code' },
    { field: 'formName', title: 'Form Name' },
    { field: 'intCurrentApprovalLevel', title: 'Approval Level' },
    { field: 'dteCreatedDate', title: 'Submitted Date' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  editForm!: FormGroup;
  selectedApplicationForEdit: Application | null = null;
  editFormFields: any[] = [];
  isSubmitting: boolean = false;
  isGeneratingPDF: boolean = false; // Flag to prevent multiple simultaneous PDF generations
  isPreparingApprovalPdf: boolean = false;
  wordEditorModules = {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{ 'header': 1 }, { 'header': 2 }],
      [{ 'list': 'ordered' }, { 'list': 'bullet' }],
      [{ 'script': 'sub' }, { 'script': 'super' }],
      [{ 'indent': '-1' }, { 'indent': '+1' }],
      [{ 'direction': 'rtl' }],
      [{ 'size': ['small', false, 'large', 'huge'] }],
      [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
      [{ 'color': [] }, { 'background': [] }],
      [{ 'font': [] }],
      [{ 'align': [] }],
      ['clean'],
    ],
  };

  constructor(
    private permissionService: PermissionService,
    private customFormApplicationService: CustomFormApplicationService,
    private customFormService: CustomFormService,
    private applicationPdfService: ApplicationPdfService,
    private notificationService: NotificationService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.editForm = this.fb.group({});
  }

  ngOnInit() {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      try {
        this.currentUser = JSON.parse(userJson);
      } catch (e) {
        console.error('Error parsing user data:', e);
        this.notificationService.showMessage('Error loading user data.', 'danger');
        return;
      }
    } else {
      this.notificationService.showMessage('User not logged in.', 'danger');
      return;
    }

    // @ts-ignore
    this.permissionService.loadPermissionRoles(this.currentUser.cfgTblRole?.serRoleId, this.currentUser.serUserId).subscribe(() => {
      // Load forms first as fallback
      this.loadForms();
      // Load applications for current user
      this.loadApplications(this.currentUser.serUserId);
      // Check if user is a department head and load pending approvals
      this.checkIfDepartmentHeadAndLoadPendingApprovals();
    });
  }

  checkIfDepartmentHeadAndLoadPendingApprovals() {
    // Check if user is a department head by calling the backend
    // For now, we'll try to load pending approvals - if user is not a head, it will return empty
    this.customFormApplicationService.getApplicationsPendingApproval(this.currentUser.serUserId).subscribe(
      (data: any) => {
        if (data && data.length > 0) {
          this.isDepartmentHead = true;
          this.pendingApprovals = data.map((app: any) => ({
            ...app,
            formName: this.getFormName(app)
          }));
        }
      },
      (error) => {
        // User is not a department head or error occurred
        this.isDepartmentHead = false;
      }
    );
  }

  loadForms() {
    this.customFormService.getAll().subscribe(
      (data: any) => {
        if (data) {
          this.forms = data;
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading forms: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  loadApplications(userId?: number) {
    // If userId is provided, fetch only that user's applications
    // Otherwise, get all applications (for admin users if needed)
    const request = userId
      ? this.customFormApplicationService.getApplicationsByUserId(userId)
      : this.customFormApplicationService.getAllApplications();

    request.subscribe(
      (data: any) => {
        if (data) {
          this.applications = data.map((app: any) => ({
            ...app,
            formName: this.getFormName(app)
          }));
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading applications: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  getFormName(app: any): string {
    // First try to get form name from the eagerly loaded relationship
    if (app.cfgTblCustomForm && app.cfgTblCustomForm.txtFormName) {
      return app.cfgTblCustomForm.txtFormName;
    }

    // Fallback: try to find in forms array (if forms are loaded)
    if (app.serFormId && this.forms && this.forms.length > 0) {
      const form = this.forms.find(f => f.serFormId === app.serFormId);
      if (form && form.txtFormName) {
        return form.txtFormName;
      }
    }

    // Last resort: return N/A or Unknown Form
    return app.serFormId ? 'Unknown Form' : 'N/A';
  }

  getDisplayedApplications(): Application[] {
    if (!this.search) {
      return this.applications;
    }
    const searchLower = this.search.toLowerCase();
    return this.applications.filter(app =>
      (app.txtFormCode && app.txtFormCode.toLowerCase().includes(searchLower)) ||
      (app.formName && app.formName.toLowerCase().includes(searchLower)) ||
      (app.txtStatus && app.txtStatus.toLowerCase().includes(searchLower)) ||
      (app.intCurrentApprovalLevel !== undefined && app.intCurrentApprovalLevel !== null &&
        String(app.intCurrentApprovalLevel).toLowerCase().includes(searchLower)) ||
      (app.dteCreatedDate && new Date(app.dteCreatedDate).toLocaleString().toLowerCase().includes(searchLower))
    );
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


  viewApplication(application: Application) {
    // Navigate to application details page instead of opening modal
    if (application.serApplicationId) {
      this.router.navigate(['/application-details', application.serApplicationId]);
    } else {
      this.notificationService.showMessage('Invalid application ID', 'danger');
    }
  }

  openAbcPage(application: Application) {
    if (!application.serApplicationId) {
      this.notificationService.showMessage('Invalid application ID', 'danger');
      return;
    }

    // Fetch full application details to get form data
    this.customFormApplicationService.getApplicationById(application.serApplicationId).subscribe(
      (data: any) => {
        if (!data) {
          this.notificationService.showMessage('Application not found', 'danger');
          return;
        }

        // Parse application form data
        let applicationFormData: any = {};
        if (data.txtApplicationData) {
          try {
            applicationFormData = JSON.parse(data.txtApplicationData);
          } catch (e) {
            console.error('Error parsing application data:', e);
          }
        }

        // Get form fields structure
        let formFields: any[] = [];
        const form = this.forms.find(f => f.serFormId === data.serFormId);
        if (form && form.cfgTblCustomFormFields) {
          formFields = form.cfgTblCustomFormFields
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
          formFields = data.cfgTblCustomForm.cfgTblCustomFormFields
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

        // Navigate to abc page with state containing form data
        this.router.navigate(['/abc'], {
          state: {
            formData: applicationFormData,
            formFields: formFields,
            application: data
          }
        });
      },
      (error) => {
        this.notificationService.showMessage('Error loading application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  getFieldName(label: string): string {
    // Convert label to a valid form control name (same logic as in application component)
    return label.toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  isWordEditorType(fieldType: string | undefined): boolean {
    const normalizedType = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
    return normalizedType === 'word_editor' || normalizedType === 'wordeditor' || normalizedType === 'rich_text' || normalizedType === 'richtext';
  }

  private richTextRequiredValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (value === null || value === undefined) {
      return { required: true };
    }
    const plainText = String(value)
      .replace(/<(.|\n)*?>/g, ' ')
      .replace(/&nbsp;/gi, ' ')
      .trim();
    return plainText.length > 0 ? null : { required: true };
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

  getTableFormArray(fieldName: string): FormArray {
    return this.editForm.get(fieldName) as FormArray;
  }

  getTableRowFormArray(fieldName: string, rowIndex: number): FormArray | null {
    const tableArray = this.getTableFormArray(fieldName);
    if (!tableArray || rowIndex >= tableArray.length) {
      return null;
    }
    return tableArray.at(rowIndex) as FormArray;
  }

  getTableCellControl(fieldName: string, rowIndex: number, colIndex: number): any {
    const rowArray = this.getTableRowFormArray(fieldName, rowIndex);
    if (!rowArray || colIndex >= rowArray.length) {
      return null;
    }
    return rowArray.at(colIndex);
  }

  getTableRows(field: any): number[] {
    const config = this.getTableConfig(field);
    return Array.from({ length: config.rows }, (_, i) => i);
  }

  getTableColumns(field: any): number[] {
    const config = this.getTableConfig(field);
    return Array.from({ length: config.columns }, (_, i) => i);
  }

  getTableRowLabel(field: any, rowIndex: number): string {
    const config = this.getTableConfig(field);
    if (config.rowLabels && config.rowLabels[rowIndex]) {
      return config.rowLabels[rowIndex];
    }
    return `Row ${rowIndex + 1}`;
  }

  getFieldOptions(field: any): string[] {
    if (!field.txtFieldOptions) {
      return [];
    }

    try {
      // Try parsing as JSON first
      const options = JSON.parse(field.txtFieldOptions);
      if (Array.isArray(options)) {
        return options.filter((opt: any) => opt && opt.trim() !== '');
      }
    } catch (e) {
      // If not JSON, try comma-separated
      if (typeof field.txtFieldOptions === 'string') {
        return field.txtFieldOptions
          .split(',')
          .map((opt: string) => opt.trim())
          .filter((opt: string) => opt !== '');
      }
    }

    return [];
  }

  canEditApplication(application: Application): boolean {
    // Only allow editing if status is PENDING or REJECTED
    // Don't allow editing if it's APPROVED or IN_PROGRESS (in approval pipeline)
    const status = application.txtStatus?.toUpperCase();
    return status === 'PENDING' || status === 'REJECTED';
  }

  isCapfForm(application: Application): boolean {
    // Check if form name is "CAPF Form" or form code starts with "CAPF"
    const formNameMatch = application.formName &&
      application.formName.trim().toUpperCase() === 'CAPF FORM';
    const formCodeMatch = application.txtFormCode &&
      application.txtFormCode.trim().toUpperCase().startsWith('CAPF');
    return !!(formNameMatch || formCodeMatch);
  }

  isBudgetApprovalForm(application: Application): boolean {
    const name = (application.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (application.txtFormCode || '').toUpperCase();
    return name === 'BUDGET APPROVAL FORM' || name.includes('BUDGET APPROVAL') || code.startsWith('BDG');
  }

  openBudgetApprovalPage(application: Application) {
    if (!application.serApplicationId) {
      this.notificationService.showMessage('Invalid application ID', 'danger');
      return;
    }

    this.customFormApplicationService.getApplicationById(application.serApplicationId).subscribe(
      (data: any) => {
        if (!data) {
          this.notificationService.showMessage('Application not found', 'danger');
          return;
        }

        let applicationFormData: any = {};
        if (data.txtApplicationData) {
          try {
            applicationFormData = JSON.parse(data.txtApplicationData);
          } catch (e) {
            console.error('Error parsing application data:', e);
          }
        }

        const dateText = data.dteCreatedDate ? new Date(data.dteCreatedDate).toLocaleDateString() : '';
        const budgetResult = this.buildBudgetApprovalContent(applicationFormData);

        this.router.navigate(['/xyz'], {
          state: {
            content: budgetResult.contentHtml,
            heading: budgetResult.heading || (data.cfgTblCustomForm?.txtFormName || application.formName || 'Budget Approval'),
            date: dateText,
            preparedBy: budgetResult.preparedBy,
            reviewers: budgetResult.reviewers,
            recommenders: budgetResult.recommenders,
            approver: budgetResult.approver
          }
        });
      },
      (error) => {
        this.notificationService.showMessage('Error loading application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  editApplication(application: Application) {
    if (!this.canEditApplication(application)) {
      this.notificationService.showMessage('This application cannot be edited in its current status', 'warning');
      return;
    }

    if (this.isBudgetApprovalForm(application)) {
      this.router.navigate(['/application'], { state: { editData: application } });
      return;
    }

    this.selectedApplicationForEdit = application;
    this.editFormFields = [];
    this.isSubmitting = false;

    // Fetch full application details
    if (application.serApplicationId) {
      this.customFormApplicationService.getApplicationById(application.serApplicationId).subscribe(
        (data: any) => {
          if (data) {
            // Get form structure
            const form = this.forms.find(f => f.serFormId === data.serFormId);
            if (form && form.cfgTblCustomFormFields) {
              this.editFormFields = form.cfgTblCustomFormFields
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
              this.editFormFields = data.cfgTblCustomForm.cfgTblCustomFormFields
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
            let applicationData: any = {};
            if (data.txtApplicationData) {
              try {
                applicationData = JSON.parse(data.txtApplicationData);
              } catch (e) {
                console.error('Error parsing application data:', e);
                applicationData = {};
              }
            }

            // Build form with existing values
            const formControls: any = {};
            this.editFormFields.forEach((field: any) => {
              const fieldName = this.getFieldName(field.label);
              const validators: any[] = [];

              if (field.required) {
                if (this.isWordEditorType(field.type)) {
                  validators.push(this.richTextRequiredValidator);
                } else {
                  validators.push(Validators.required);
                }
              }

              if (field.type === 'email') {
                validators.push(Validators.email);
              }

              // Handle table fields
              if (field.type === 'table') {
                const tableConfig = this.getTableConfig(field);
                const tableFormArray = this.fb.array<FormArray>([]);

                // Get existing table data
                let existingTableData = applicationData[fieldName] || applicationData[field.label] || null;

                // Create form controls for each cell in the table
                for (let row = 0; row < tableConfig.rows; row++) {
                  const rowArray = this.fb.array<any>([]);
                  for (let col = 0; col < tableConfig.columns; col++) {
                    const cellValue = (existingTableData && Array.isArray(existingTableData) && existingTableData[row] && Array.isArray(existingTableData[row]))
                      ? (existingTableData[row][col] || '')
                      : '';
                    rowArray.push(this.fb.control(cellValue));
                  }
                  tableFormArray.push(rowArray);
                }

                formControls[fieldName] = tableFormArray;
              } else {
                // Get existing value
                let existingValue = applicationData[fieldName] || applicationData[field.label] || null;

                // Set default value based on field type
                if (existingValue === null || existingValue === undefined) {
                  existingValue = field.type === 'checkbox' ? false : '';
                }

                formControls[fieldName] = [existingValue, validators];
              }
            });

            this.editForm = this.fb.group(formControls);
            this.editModal.open();
          } else {
            this.notificationService.showMessage('Application not found', 'danger');
          }
        },
        (error) => {
          this.notificationService.showMessage('Error loading application for editing: ' + (error.error?.message || error.message), 'danger');
        }
      );
    }
  }

  updateApplication() {
    if (this.editForm.invalid) {
      this.notificationService.showMessage('Please fill all required fields', 'danger');
      return;
    }

    if (!this.selectedApplicationForEdit || !this.selectedApplicationForEdit.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }

    this.isSubmitting = true;
    const formData = { ...this.editForm.value };

    // Convert table FormArrays to regular arrays for JSON serialization
    this.editFormFields.forEach((field: any) => {
      if (field.type === 'table') {
        const fieldName = this.getFieldName(field.label);
        const tableArray = this.editForm.get(fieldName) as FormArray;
        if (tableArray) {
          formData[fieldName] = tableArray.value;
        }
      }
    });

    // Convert form data to JSON string
    const applicationDataJson = JSON.stringify(formData);

    // Prepare payload for backend
    const payload: any = {
      serApplicationId: this.selectedApplicationForEdit.serApplicationId,
      serFormId: this.selectedApplicationForEdit.serFormId,
      txtFormCode: this.selectedApplicationForEdit.txtFormCode,
      txtApplicationData: applicationDataJson,
      txtStatus: this.selectedApplicationForEdit.txtStatus || 'PENDING',
      intCurrentApprovalLevel: this.selectedApplicationForEdit.intCurrentApprovalLevel || 0,
      serSubmittedBy: this.selectedApplicationForEdit.serSubmittedBy,
      blIsActive: true,
      blIsDeleted: false,
      blnStatus: true
    };

    // Submit to backend
    this.customFormApplicationService.updateApplication(payload).subscribe(
      (response: any) => {
        this.isSubmitting = false;
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application updated successfully', 'success');
          this.editModal.close();
          this.selectedApplicationForEdit = null;
          this.loadApplications(this.currentUser.serUserId);
          if (this.isDepartmentHead) {
            this.checkIfDepartmentHeadAndLoadPendingApprovals();
          }
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to update application', 'danger');
        }
      },
      (error) => {
        this.isSubmitting = false;
        this.notificationService.showMessage('Error updating application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.editForm.get(fieldName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  getFieldErrorMessage(fieldName: string): string {
    const control = this.editForm.get(fieldName);
    if (control && control.errors) {
      if (control.errors['required']) {
        return 'This field is required';
      }
      if (control.errors['email']) {
        return 'Please enter a valid email address';
      }
    }
    return '';
  }

  deleteApplication(application: Application) {
    if (confirm('Are you sure you want to delete this application? You will not be able to recover it!')) {
      const applicationId = application.serApplicationId;
      if (!applicationId) {
        this.notificationService.showMessage('Invalid application ID', 'danger');
        return;
      }

      this.customFormApplicationService.deleteApplication(applicationId).subscribe(
        (response: any) => {
          if (response && response.status === 'Success') {
            this.notificationService.showMessage(response.message || 'Application deleted successfully', 'success');
            this.loadApplications(this.currentUser.serUserId);
            if (this.isDepartmentHead) {
              this.checkIfDepartmentHeadAndLoadPendingApprovals();
            }
          } else {
            this.notificationService.showMessage(response?.message || 'Failed to delete application', 'danger');
          }
        },
        (error) => {
          this.notificationService.showMessage('Error deleting application: ' + (error.error?.message || error.message), 'danger');
        }
      );
    }
  }

  @ViewChild('approveModal') approveModal: any;
  @ViewChild('rejectModal') rejectModal: any;
  @ViewChild('sendBackModal') sendBackModal: any;
  @ViewChild('editModal') editModal: any;

  openApproveModal(application: Application) {
    this.selectedApplicationForRemarks = application;
    this.remarksText = '';
    this.approveModal.open();
  }

  openRejectModal(application: Application) {
    this.selectedApplicationForRemarks = application;
    this.remarksText = '';
    this.rejectModal.open();
  }

  openSendBackModal(application: Application) {
    this.selectedApplicationForRemarks = application;
    this.remarksText = '';
    this.sendBackModal.open();
  }

  async approveApplication() {
    if (!this.selectedApplicationForRemarks || !this.selectedApplicationForRemarks.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }

    if (this.isPreparingApprovalPdf) {
      this.notificationService.showMessage('Preparing PDF, please wait...', 'warning');
      return;
    }

    let uploadOk = false;
    this.isPreparingApprovalPdf = true;
    try {
      const pdfResult = await this.generateApprovalPdfBlob(this.selectedApplicationForRemarks);
      const uploadResponse: any = await firstValueFrom(
        this.customFormApplicationService.updateApplicationPdf(
          this.selectedApplicationForRemarks.serApplicationId,
          pdfResult.blob,
          pdfResult.filename
        )
      );
      if (uploadResponse && uploadResponse.status === 'Success') {
        uploadOk = true;
      } else {
        this.notificationService.showMessage(uploadResponse?.message || 'Failed to upload application PDF', 'danger');
      }
    } catch (e: any) {
      console.error('Error preparing approval PDF:', e);
      this.notificationService.showMessage('Error preparing approval PDF', 'danger');
    } finally {
      this.isPreparingApprovalPdf = false;
    }

    if (!uploadOk) {
      return;
    }

    this.customFormApplicationService.approveApplication(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application approved successfully', 'success');
          this.approveModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.loadApplications(this.currentUser.serUserId);
          this.checkIfDepartmentHeadAndLoadPendingApprovals();
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
    if (!this.selectedApplicationForRemarks || !this.selectedApplicationForRemarks.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }

    if (!this.remarksText || this.remarksText.trim() === '') {
      this.notificationService.showMessage('Please provide a rejection reason', 'danger');
      return;
    }

    this.customFormApplicationService.rejectApplication(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application rejected successfully', 'success');
          this.rejectModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.loadApplications(this.currentUser.serUserId);
          this.checkIfDepartmentHeadAndLoadPendingApprovals();
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
    if (!this.selectedApplicationForRemarks || !this.selectedApplicationForRemarks.serApplicationId) {
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

    this.customFormApplicationService.sendBackApplication(
      this.selectedApplicationForRemarks.serApplicationId,
      this.remarksText
    ).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Application sent back successfully', 'success');
          this.sendBackModal.close();
          this.selectedApplicationForRemarks = null;
          this.remarksText = '';
          this.loadApplications(this.currentUser.serUserId);
          if (this.isDepartmentHead) {
            this.checkIfDepartmentHeadAndLoadPendingApprovals();
          }
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to send back application', 'danger');
        }
      },
      (error) => {
        this.notificationService.showMessage('Error sending back application: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  togglePendingApprovalsView() {
    this.showPendingApprovals = !this.showPendingApprovals;
  }

  getDisplayedPendingApprovals(): Application[] {
    if (!this.search) {
      return this.pendingApprovals;
    }
    const searchLower = this.search.toLowerCase();
    return this.pendingApprovals.filter(app =>
      (app.txtFormCode && app.txtFormCode.toLowerCase().includes(searchLower)) ||
      (app.formName && app.formName.toLowerCase().includes(searchLower))
    );
  }

  downloadApplications() {
    const applicationsToExport = this.getDisplayedApplications();

    if (applicationsToExport.length === 0) {
      this.notificationService.showMessage('No applications to download', 'warning');
      return;
    }

    // Prepare CSV data
    const headers = ['Application Code', 'Form Name', 'Status', 'Approval Level', 'Submitted Date', 'Remarks'];
    const csvRows: string[] = [];

    // Add headers
    csvRows.push(headers.join(','));

    // Add data rows
    applicationsToExport.forEach(app => {
      const row = [
        this.escapeCsvValue(app.txtFormCode || ''),
        this.escapeCsvValue(app.formName || ''),
        this.escapeCsvValue(app.txtStatus || ''),
        (app.intCurrentApprovalLevel || 0).toString(),
        app.dteCreatedDate ? new Date(app.dteCreatedDate).toLocaleString() : '',
        this.escapeCsvValue(app.txtRemarks || '')
      ];
      csvRows.push(row.join(','));
    });

    // Create CSV content
    const csvContent = csvRows.join('\n');

    // Create blob and download
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const fileName = `applications_${new Date().toISOString().split('T')[0]}.csv`;
    saveAs(blob, fileName);

    this.notificationService.showMessage(`Downloaded ${applicationsToExport.length} application(s)`, 'success');
  }

  private escapeCsvValue(value: string): string {
    if (!value) return '';
    // If value contains comma, newline, or quote, wrap it in quotes and escape quotes
    if (value.includes(',') || value.includes('\n') || value.includes('"')) {
      return `"${value.replace(/"/g, '""')}"`;
    }
    return value;
  }

  downloadApplicationAsPDF(application: Application) {
    if (!application.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
      return;
    }

    const initialIsBudgetApproval = this.isBudgetApprovalForm(application);
    const forceBudgetApprovalByCode = (application.txtFormCode || '').toUpperCase().startsWith('BDG');

    // Prevent multiple simultaneous PDF generations
    if (this.isGeneratingPDF) {
      this.notificationService.showMessage('PDF generation in progress, please wait...', 'warning');
      return;
    }

    // Store the requested application ID and form code to ensure we're processing the correct one
    const requestedApplicationId = application.serApplicationId;
    const requestedFormCode = application.txtFormCode; // Store original form code for verification

    this.isGeneratingPDF = true;
    this.notificationService.showMessage('Generating PDF...', 'info');

    this.customFormApplicationService.getApplicationById(requestedApplicationId).subscribe(
      (data: any) => {
        // Verify we got the correct application data
        if (!data || data.serApplicationId !== requestedApplicationId) {
          this.isGeneratingPDF = false;
          this.notificationService.showMessage('Application data mismatch or not found', 'danger');
          return;
        }

        // Verify form code matches (additional safety check)
        if (requestedFormCode && data.txtFormCode && data.txtFormCode !== requestedFormCode) {
          this.isGeneratingPDF = false;
          this.notificationService.showMessage('Application form code mismatch', 'danger');
          return;
        }

        const form = this.forms.find(f => f.serFormId === data.serFormId);
        let formFields: any[] = [];

        if (form && form.cfgTblCustomFormFields) {
          formFields = form.cfgTblCustomFormFields
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
          formFields = data.cfgTblCustomForm.cfgTblCustomFormFields
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

        let applicationFormData: any = {};
        if (data.txtApplicationData) {
          try {
            applicationFormData = JSON.parse(data.txtApplicationData);
          } catch (e) {
            console.error('Error parsing application data:', e);
          }
        }

        // ALWAYS use ABC design for ALL PDF downloads - no exceptions
        // This ensures consistent ABC format for all application types
        // Check if this is a CAPF form
        let isCapf = false;
        if (data.txtFormCode && data.txtFormCode.trim().toUpperCase().startsWith('CAPF')) {
          isCapf = true;
        } else if (form && form.txtFormName && form.txtFormName.trim().toUpperCase() === 'CAPF FORM') {
          isCapf = true;
        } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.txtFormName && data.cfgTblCustomForm.txtFormName.trim().toUpperCase() === 'CAPF FORM') {
          isCapf = true;
        }

        let htmlContent = '';
        let handledBudgetApproval = false;
        const resolvedFormName = (form?.txtFormName || data.cfgTblCustomForm?.txtFormName || application.formName || '').trim();
        const resolvedFormCode = (data.txtFormCode || application.txtFormCode || '').trim();
        const normalizedFormName = resolvedFormName.replace(/\s+/g, ' ').toUpperCase();
        const normalizedFormCode = resolvedFormCode.toUpperCase();
        const isBudgetApproval =
          normalizedFormName === 'BUDGET APPROVAL FORM' ||
          normalizedFormName.includes('BUDGET APPROVAL') ||
          normalizedFormCode.startsWith('BDG') ||
          normalizedFormCode.includes('BDG-') ||
          normalizedFormCode.includes('BAF') ||
          initialIsBudgetApproval ||
          forceBudgetApprovalByCode;

        console.log('[download] formName=', resolvedFormName, 'formCode=', resolvedFormCode, 'isBudgetApproval=', isBudgetApproval, 'isCapf=', isCapf);

        let branch = 'generic';
        if (forceBudgetApprovalByCode) {
          handledBudgetApproval = true;
          htmlContent = this.generateBudgetApprovalPdfHtml(data, formFields, applicationFormData, resolvedFormName || 'Budget Approval');
          branch = 'budget';
        }

        if (!handledBudgetApproval && isBudgetApproval) {
          handledBudgetApproval = true;
          htmlContent = this.generateBudgetApprovalPdfHtml(data, formFields, applicationFormData, resolvedFormName || 'Budget Approval');
          branch = 'budget';
        } else if (!handledBudgetApproval && isCapf) {
          branch = 'capf';
          // Get approval pipelines for CAPF form
          let pipelines = [];
          if (form && form.cfgTblFormApprovalPipelines) {
            pipelines = form.cfgTblFormApprovalPipelines;
          } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblFormApprovalPipelines) {
            pipelines = data.cfgTblCustomForm.cfgTblFormApprovalPipelines;
          }

          // Sort pipelines
          if (pipelines) {
            pipelines.sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0));
          } else {
            pipelines = [];
          }

          // Use ABC design for CAPF forms
          htmlContent = this.generateCapfAbcHtml(data, formFields, applicationFormData, pipelines);

          // Verify HTML contains HTML wrapper
          if (!htmlContent || !htmlContent.includes('abc-wrapper')) {
            this.isGeneratingPDF = false;
            this.notificationService.showMessage('Error: ABC HTML generation failed', 'danger');
            return;
          }
        } else if (!handledBudgetApproval) {
          branch = 'generic';
          // Use Dynamic/Simple design for other forms
          let pipelines = [];
          if (form && form.cfgTblFormApprovalPipelines) {
            pipelines = form.cfgTblFormApprovalPipelines;
          } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblFormApprovalPipelines) {
            pipelines = data.cfgTblCustomForm.cfgTblFormApprovalPipelines;
          }

          // Sort pipelines
          if (pipelines) {
            pipelines.sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0));
          } else {
            pipelines = [];
          }

          htmlContent = this.generatePDFContent(data, resolvedFormName || 'Unknown Form', formFields, applicationFormData, pipelines);
        }

        console.log('[download] branch=', branch, 'htmlHasXyz=', htmlContent.includes('xyz-paper'), 'htmlHasAbc=', htmlContent.includes('abc-wrapper'));

        // Skip the old verification logic (commented out below or removed)
        /* Old verification logic was here */





        // Create an iframe to render the HTML properly
        // Remove any existing iframes first to prevent multiple PDFs
        const existingIframes = document.querySelectorAll('iframe[data-pdf-generator]');
        existingIframes.forEach((iframe: Element) => {
          if (iframe.parentNode) {
            iframe.parentNode.removeChild(iframe);
          }
        });

        const iframe = document.createElement('iframe');
        iframe.setAttribute('data-pdf-generator', 'true');
        iframe.style.position = 'absolute';
        iframe.style.left = '-9999px';
        iframe.style.top = '0';
        // Use wider iframe to match abc page width (820px + padding)
        iframe.style.width = '900px';
        iframe.style.height = '1200px';
        iframe.style.border = 'none';
        iframe.style.overflow = 'hidden';
        document.body.appendChild(iframe);

        // Write HTML content to iframe
        const iframeDoc = iframe.contentDocument || (iframe.contentWindow as any)?.document;
        if (!iframeDoc) {
          if (document.body.contains(iframe)) {
            document.body.removeChild(iframe);
          }
          this.isGeneratingPDF = false;
          this.notificationService.showMessage('Error: Could not access iframe document', 'danger');
          return;
        }

        iframeDoc.open();
        iframeDoc.write(htmlContent);
        iframeDoc.close();

        // Ensure styles are loaded by accessing computed styles
        if (iframeDoc.head) {
          const styleSheets = iframeDoc.styleSheets;
          // Force style computation
          if (iframeDoc.body) {
            const testElement = iframeDoc.createElement('div');
            testElement.style.display = 'none';
            iframeDoc.body.appendChild(testElement);
            const computedStyle = window.getComputedStyle(testElement);
            iframeDoc.body.removeChild(testElement);
          }
        }

        // Wait for iframe content to load and styles to be applied
        iframe.onload = () => {
          setTimeout(() => {
            // Use the wrapper div to ensure all styles are captured
            const element = (iframeDoc.querySelector('.abc-wrapper') || iframeDoc.querySelector('.xyz-paper') || iframeDoc.body) as HTMLElement;

            // Force a reflow to ensure styles are computed
            if (element) {
              element.offsetHeight; // Trigger reflow
            }

            const ts = new Date().toISOString().replace(/[:T]/g, '-').split('.')[0];
            const opt = {
              margin: [2, 5, 2, 5] as [number, number, number, number],
              filename: `application_${data.txtFormCode || data.serApplicationId}_${ts}.pdf`,
              image: { type: 'jpeg' as const, quality: 0.98 },
              html2canvas: {
                scale: 1.5,
                useCORS: true,
                logging: false,
                letterRendering: true,
                allowTaint: true,
                backgroundColor: '#ffffff',
                windowWidth: element.scrollWidth || 900,
                windowHeight: element.scrollHeight || 1200,
                width: element.scrollWidth || 900,
                height: element.scrollHeight || 1200,
                onclone: (clonedDoc: Document) => {
                  // Ensure styles are preserved in the cloned document
                  const clonedElement = (clonedDoc.querySelector('.abc-wrapper') || clonedDoc.querySelector('.xyz-paper') || clonedDoc.body) as HTMLElement;
                  if (clonedElement) {
                    // Force style computation in cloned document
                    clonedElement.offsetHeight;
                  }
                }
              },
              jsPDF: {
                unit: 'mm' as const,
                format: 'a4' as const,
                orientation: 'portrait' as const
              }
            };

            // Generate PDF and auto-download without dialog
            html2pdf().set(opt).from(element).outputPdf('blob').then((pdfBlob: Blob) => {
              // Create download link and trigger automatic download
              const url = URL.createObjectURL(pdfBlob);
              const link = document.createElement('a');
              link.href = url;
              link.download = `application_${data.txtFormCode || data.serApplicationId}_${ts}.pdf`;
              document.body.appendChild(link);
              link.click();
              document.body.removeChild(link);
              URL.revokeObjectURL(url);

              if (document.body.contains(iframe)) {
                document.body.removeChild(iframe);
              }
              this.isGeneratingPDF = false; // Reset flag
              this.notificationService.showMessage('PDF downloaded successfully', 'success');
            }).catch((error: any) => {
              if (document.body.contains(iframe)) {
                document.body.removeChild(iframe);
              }
              this.isGeneratingPDF = false; // Reset flag on error
              console.error('Error generating PDF:', error);
              this.notificationService.showMessage('Error generating PDF: ' + (error.message || 'Unknown error'), 'danger');
            });
          }, 1000); // Delay to ensure styles load
        };

        // Fallback if onload doesn't fire
        setTimeout(() => {
          if (iframe.contentDocument && iframe.contentDocument.body) {
            // Use the wrapper div to ensure all styles are captured
            const element = (iframe.contentDocument.querySelector('.abc-wrapper') || iframe.contentDocument.querySelector('.xyz-paper') || iframe.contentDocument.body) as HTMLElement;

            // Force a reflow to ensure styles are computed
            if (element) {
              element.offsetHeight; // Trigger reflow
            }

            const ts = new Date().toISOString().replace(/[:T]/g, '-').split('.')[0];
            const opt = {
              margin: [2, 5, 2, 5] as [number, number, number, number],
              filename: `application_${data.txtFormCode || data.serApplicationId}_${ts}.pdf`,
              image: { type: 'jpeg' as const, quality: 0.98 },
              html2canvas: {
                scale: 1.5,
                useCORS: true,
                logging: false,
                letterRendering: true,
                allowTaint: true,
                backgroundColor: '#ffffff',
                windowWidth: element.scrollWidth || 900,
                windowHeight: element.scrollHeight || 1200,
                width: element.scrollWidth || 900,
                height: element.scrollHeight || 1200,
                onclone: (clonedDoc: Document) => {
                  // Ensure styles are preserved in the cloned document
                  const clonedElement = (clonedDoc.querySelector('.abc-wrapper') || clonedDoc.querySelector('.xyz-paper') || clonedDoc.body) as HTMLElement;
                  if (clonedElement) {
                    // Force style computation in cloned document
                    clonedElement.offsetHeight;
                  }
                }
              },
              jsPDF: { unit: 'mm' as const, format: 'a4' as const, orientation: 'portrait' as const }
            };

            // Generate PDF and auto-download without dialog
            html2pdf().set(opt).from(element).outputPdf('blob').then((pdfBlob: Blob) => {
              // Create download link and trigger automatic download
              const url = URL.createObjectURL(pdfBlob);
              const link = document.createElement('a');
              link.href = url;
              link.download = `application_${data.txtFormCode || data.serApplicationId}_${ts}.pdf`;
              document.body.appendChild(link);
              link.click();
              document.body.removeChild(link);
              URL.revokeObjectURL(url);

              if (document.body.contains(iframe)) {
                document.body.removeChild(iframe);
              }
              this.isGeneratingPDF = false; // Reset flag
              this.notificationService.showMessage('PDF downloaded successfully', 'success');
            }).catch((error: any) => {
              if (document.body.contains(iframe)) {
                document.body.removeChild(iframe);
              }
              this.isGeneratingPDF = false; // Reset flag on error
              console.error('Error generating PDF:', error);
              this.notificationService.showMessage('Error generating PDF: ' + (error.message || 'Unknown error'), 'danger');
            });
          }
        }, 1500); // Longer delay to ensure styles are fully loaded
      },
      (error) => {
        this.isGeneratingPDF = false; // Reset flag on error
        this.notificationService.showMessage('Error loading application details: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  private async generateApprovalPdfBlob(application: Application): Promise<{ blob: Blob; filename: string }> {
    if (!application.serApplicationId) {
      throw new Error('Invalid application ID');
    }

    const requestedApplicationId = application.serApplicationId;
    const requestedFormCode = application.txtFormCode;

    const data: any = await firstValueFrom(this.customFormApplicationService.getApplicationById(requestedApplicationId));
    if (!data || data.serApplicationId !== requestedApplicationId) {
      throw new Error('Application data mismatch or not found');
    }
    if (requestedFormCode && data.txtFormCode && data.txtFormCode !== requestedFormCode) {
      throw new Error('Application form code mismatch');
    }

    const form = this.forms.find(f => f.serFormId === data.serFormId);
    let formFields: any[] = [];

    if (form && form.cfgTblCustomFormFields) {
      formFields = form.cfgTblCustomFormFields
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
      formFields = data.cfgTblCustomForm.cfgTblCustomFormFields
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

    let applicationFormData: any = {};
    if (data.txtApplicationData) {
      try {
        applicationFormData = JSON.parse(data.txtApplicationData);
      } catch (e) {
        console.error('Error parsing application data:', e);
      }
    }

    const resolvedFormName = (form?.txtFormName || data?.cfgTblCustomForm?.txtFormName || application.formName || '').trim();
    const resolvedFormCode = (data.txtFormCode || application.txtFormCode || '').trim();
    const htmlContent = this.applicationPdfService.buildPdfHtmlForApplication(
      data,
      form,
      formFields,
      applicationFormData,
      { formName: resolvedFormName, txtFormCode: resolvedFormCode }
    );
    if (!htmlContent) {
      throw new Error('PDF HTML generation failed');
    }

    const filename = `application_${data.txtFormCode || data.serApplicationId}.pdf`;
    const blob = await this.applicationPdfService.renderHtmlToPdfBlob(htmlContent, filename);
    return { blob, filename };
  }

  private buildPdfHtmlForApproval(
    data: any,
    form: any,
    formFields: any[],
    applicationFormData: any,
    application: Application
  ): string {
    let isCapf = false;
    if (data.txtFormCode && data.txtFormCode.trim().toUpperCase().startsWith('CAPF')) {
      isCapf = true;
    } else if (form && form.txtFormName && form.txtFormName.trim().toUpperCase() === 'CAPF FORM') {
      isCapf = true;
    } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.txtFormName && data.cfgTblCustomForm.txtFormName.trim().toUpperCase() === 'CAPF FORM') {
      isCapf = true;
    }

    let htmlContent = '';
    let handledBudgetApproval = false;
    const resolvedFormName = (form?.txtFormName || data.cfgTblCustomForm?.txtFormName || application.formName || '').trim();
    const resolvedFormCode = (data.txtFormCode || application.txtFormCode || '').trim();
    const normalizedFormName = resolvedFormName.replace(/\s+/g, ' ').toUpperCase();
    const normalizedFormCode = resolvedFormCode.toUpperCase();
    const initialIsBudgetApproval = this.isBudgetApprovalForm(application);
    const forceBudgetApprovalByCode = (application.txtFormCode || '').toUpperCase().startsWith('BDG');

    const isBudgetApproval =
      normalizedFormName === 'BUDGET APPROVAL FORM' ||
      normalizedFormName.includes('BUDGET APPROVAL') ||
      normalizedFormCode.startsWith('BDG') ||
      normalizedFormCode.includes('BDG-') ||
      normalizedFormCode.includes('BAF') ||
      initialIsBudgetApproval ||
      forceBudgetApprovalByCode;

    if (forceBudgetApprovalByCode) {
      handledBudgetApproval = true;
      htmlContent = this.generateBudgetApprovalPdfHtml(data, formFields, applicationFormData, resolvedFormName || 'Budget Approval');
    }

    if (!handledBudgetApproval && isBudgetApproval) {
      handledBudgetApproval = true;
      htmlContent = this.generateBudgetApprovalPdfHtml(data, formFields, applicationFormData, resolvedFormName || 'Budget Approval');
    } else if (!handledBudgetApproval && isCapf) {
      let pipelines: any[] = [];
      if (form && form.cfgTblFormApprovalPipelines) {
        pipelines = form.cfgTblFormApprovalPipelines;
      } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblFormApprovalPipelines) {
        pipelines = data.cfgTblCustomForm.cfgTblFormApprovalPipelines;
      }
      if (pipelines) {
        pipelines.sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0));
      } else {
        pipelines = [];
      }

      htmlContent = this.generateCapfAbcHtml(data, formFields, applicationFormData, pipelines);
      if (!htmlContent || !htmlContent.includes('abc-wrapper')) {
        throw new Error('ABC HTML generation failed');
      }
    } else if (!handledBudgetApproval) {
      let pipelines: any[] = [];
      if (form && form.cfgTblFormApprovalPipelines) {
        pipelines = form.cfgTblFormApprovalPipelines;
      } else if (data.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblFormApprovalPipelines) {
        pipelines = data.cfgTblCustomForm.cfgTblFormApprovalPipelines;
      }
      if (pipelines) {
        pipelines.sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0));
      } else {
        pipelines = [];
      }

      htmlContent = this.generatePDFContent(data, resolvedFormName || 'Unknown Form', formFields, applicationFormData, pipelines);
    }

    return htmlContent;
  }

  private renderHtmlToPdfBlob(htmlContent: string, filename: string): Promise<Blob> {
    return new Promise((resolve, reject) => {
      let done = false;

      const cleanup = (iframe: HTMLIFrameElement) => {
        if (document.body.contains(iframe)) {
          document.body.removeChild(iframe);
        }
      };

      const existingIframes = document.querySelectorAll('iframe[data-pdf-generator]');
      existingIframes.forEach((iframe: Element) => {
        if (iframe.parentNode) {
          iframe.parentNode.removeChild(iframe);
        }
      });

      const iframe = document.createElement('iframe');
      iframe.setAttribute('data-pdf-generator', 'true');
      iframe.style.position = 'absolute';
      iframe.style.left = '-9999px';
      iframe.style.top = '0';
      iframe.style.width = '900px';
      iframe.style.height = '1200px';
      iframe.style.border = 'none';
      iframe.style.overflow = 'hidden';
      document.body.appendChild(iframe);

      const iframeDoc = iframe.contentDocument || (iframe.contentWindow as any)?.document;
      if (!iframeDoc) {
        cleanup(iframe);
        reject(new Error('Could not access iframe document'));
        return;
      }

      iframeDoc.open();
      iframeDoc.write(htmlContent);
      iframeDoc.close();

      const generatePdf = () => {
        if (done) return;
        try {
          const element = (iframeDoc.querySelector('.abc-wrapper') || iframeDoc.querySelector('.xyz-paper') || iframeDoc.body) as HTMLElement;
          if (!element) {
            done = true;
            cleanup(iframe);
            reject(new Error('PDF content element not found'));
            return;
          }

          element.offsetHeight;
          const opt = {
            margin: [2, 5, 2, 5] as [number, number, number, number],
            filename,
            image: { type: 'jpeg' as const, quality: 0.98 },
            html2canvas: {
              scale: 1.5,
              useCORS: true,
              logging: false,
              letterRendering: true,
              allowTaint: true,
              backgroundColor: '#ffffff',
              windowWidth: element.scrollWidth || 900,
              windowHeight: element.scrollHeight || 1200,
              width: element.scrollWidth || 900,
              height: element.scrollHeight || 1200,
              onclone: (clonedDoc: Document) => {
                const clonedElement = (clonedDoc.querySelector('.abc-wrapper') || clonedDoc.querySelector('.xyz-paper') || clonedDoc.body) as HTMLElement;
                if (clonedElement) {
                  clonedElement.offsetHeight;
                }
              }
            },
            jsPDF: {
              unit: 'mm' as const,
              format: 'a4' as const,
              orientation: 'portrait' as const
            }
          };

          html2pdf().set(opt).from(element).outputPdf('blob').then((pdfBlob: Blob) => {
            if (done) return;
            done = true;
            cleanup(iframe);
            resolve(pdfBlob);
          }).catch((error: any) => {
            if (done) return;
            done = true;
            cleanup(iframe);
            reject(error);
          });
        } catch (error) {
          if (done) return;
          done = true;
          cleanup(iframe);
          reject(error);
        }
      };

      iframe.onload = () => {
        setTimeout(generatePdf, 1000);
      };

      setTimeout(() => {
        if (!done) {
          generatePdf();
        }
      }, 1500);
    });
  }

  private generatePDFContent(application: any, formName: string, formFields: any[], applicationFormData: any, pipelines: any[]): string {
    const formatDate = (dateStr: string | number) => {
      if (!dateStr) return '-';
      const date = new Date(dateStr);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    const formatFieldValue = (field: any, value: any): string => {
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
      return String(value);
    };

    const isWordEditorType = (fieldType: string | undefined): boolean => {
      const normalizedType = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
      return normalizedType === 'word_editor' || normalizedType === 'wordeditor' || normalizedType === 'rich_text' || normalizedType === 'richtext';
    };

    const getFieldValue = (field: any): any => {
      const fieldName = field.label.toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');

      if (applicationFormData[fieldName] !== undefined) {
        return applicationFormData[fieldName];
      } else if (applicationFormData[field.label] !== undefined) {
        return applicationFormData[field.label];
      } else {
        const fieldId = `field_${field.serFieldId}`;
        if (applicationFormData[fieldId] !== undefined) {
          return applicationFormData[fieldId];
        }
      }
      return null;
    };

    const isDepartmentApproved = (pipelineOrder: number): boolean => {
      const currentLevel = application.intCurrentApprovalLevel || 0;
      return currentLevel >= pipelineOrder;
    };

    const escapeHtml = (text: string): string => {
      if (!text) return '';
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    };

    const getFieldDisplayHtml = (field: any): string => {
      const value = formatFieldValue(field, getFieldValue(field));
      if (isWordEditorType(field.type) && value !== '-') {
        return `<div class="word-editor-value">${String(value)}</div>`;
      }
      return escapeHtml(String(value));
    };

    let html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      font-size: 11px;
      line-height: 1.6;
      color: #2c3e50;
      padding: 25px;
      background-color: #ffffff;
    }
    .header {
      text-align: center;
      margin-bottom: 30px;
      padding-bottom: 20px;
      border-bottom: 4px solid #3498db;
    }
    .header h1 {
      color: #2c3e50;
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 5px;
      border: none;
      padding: 0;
    }
    .header .subtitle {
      color: #7f8c8d;
      font-size: 12px;
      font-weight: normal;
    }
    .section {
      margin-bottom: 30px;
      page-break-inside: avoid;
    }
    .section-title {
      color: #34495e;
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 15px;
      padding-bottom: 8px;
      border-bottom: 2px solid #ecf0f1;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .info-grid {
      display: grid;
      grid-template-columns: 180px 1fr;
      gap: 12px 20px;
      margin-bottom: 15px;
    }
    .info-label {
      font-weight: 600;
      color: #555;
      font-size: 11px;
    }
    .info-value {
      color: #2c3e50;
      font-size: 11px;
      word-wrap: break-word;
    }
    .badge {
      display: inline-block;
      padding: 5px 12px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .badge-success {
      background-color: #27ae60;
      color: white;
    }
    .badge-warning {
      background-color: #f39c12;
      color: white;
    }
    .badge-danger {
      background-color: #e74c3c;
      color: white;
    }
    .badge-info {
      background-color: #3498db;
      color: white;
    }
    .badge-secondary {
      background-color: #95a5a6;
      color: white;
    }
    .data-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 10px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .data-table thead {
      background: linear-gradient(to bottom, #f8f9fa, #e9ecef);
    }
    .data-table th {
      padding: 12px 15px;
      text-align: left;
      font-weight: 600;
      font-size: 11px;
      color: #2c3e50;
      border-bottom: 2px solid #dee2e6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .data-table td {
      padding: 12px 15px;
      border-bottom: 1px solid #e9ecef;
      font-size: 11px;
      color: #495057;
    }
    .data-table tbody tr:hover {
      background-color: #f8f9fa;
    }
    .data-table tbody tr:last-child td {
      border-bottom: none;
    }
    .word-editor-value {
      font-size: 11px;
      line-height: 1.45;
    }
    .word-editor-value p {
      margin: 0 0 6px 0;
    }
    .word-editor-value ul,
    .word-editor-value ol {
      margin: 0 0 6px 18px;
      padding: 0;
    }
    .word-editor-value table {
      width: 100%;
      border-collapse: collapse;
      margin: 6px 0;
    }
    .word-editor-value td,
    .word-editor-value th {
      border: 1px solid #d9d9d9;
      padding: 4px 6px;
    }
    .pipeline-section {
      margin-top: 20px;
    }
    .pipeline-flow {
      display: flex;
      align-items: center;
      justify-content: flex-start;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 15px;
    }
    .pipeline-card {
      border: 2px solid #ddd;
      border-radius: 8px;
      padding: 15px;
      min-width: 160px;
      text-align: center;
      background-color: #ffffff;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .pipeline-card.approved {
      border-color: #27ae60;
      background: linear-gradient(to bottom, #d5f4e6, #ffffff);
    }
    .pipeline-card.pending {
      border-color: #f39c12;
      background: linear-gradient(to bottom, #fef5e7, #ffffff);
    }
    .pipeline-card.not-started {
      border-color: #bdc3c7;
      background: linear-gradient(to bottom, #ecf0f1, #ffffff);
    }
    .pipeline-card .dept-name {
      font-weight: 700;
      font-size: 12px;
      margin-bottom: 8px;
      color: #2c3e50;
    }
    .pipeline-card .level {
      font-size: 10px;
      color: #7f8c8d;
      margin-bottom: 8px;
    }
    .pipeline-card .status {
      font-size: 11px;
      font-weight: 600;
      margin-bottom: 10px;
      padding: 4px 8px;
      border-radius: 4px;
      display: inline-block;
    }
    .pipeline-card.approved .status {
      background-color: #27ae60;
      color: white;
    }
    .pipeline-card.pending .status {
      background-color: #f39c12;
      color: white;
    }
    .pipeline-card.not-started .status {
      background-color: #bdc3c7;
      color: #2c3e50;
    }
    .pipeline-card .remarks {
      font-size: 9px;
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid #ddd;
      text-align: left;
      color: #555;
      font-style: italic;
    }
    .arrow {
      font-size: 24px;
      color: #95a5a6;
      font-weight: bold;
    }
    .footer {
      margin-top: 40px;
      padding-top: 20px;
      border-top: 2px solid #ecf0f1;
      text-align: center;
      color: #95a5a6;
      font-size: 9px;
    }
    .remarks-box {
      background-color: #f8f9fa;
      border-left: 4px solid #3498db;
      padding: 12px 15px;
      margin-top: 10px;
      border-radius: 4px;
      font-size: 11px;
      color: #495057;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Application Details</h1>
    <div class="subtitle">Application Code: ${escapeHtml(application.txtFormCode || 'N/A')}</div>
  </div>

  <div class="section">
    <h2 class="section-title">Application Information</h2>
    <div class="info-grid">
      <div class="info-label">Application Code:</div>
      <div class="info-value"><strong>${escapeHtml(application.txtFormCode || '-')}</strong></div>
      
      <div class="info-label">Form Name:</div>
      <div class="info-value">${escapeHtml(formName)}</div>
      
      <div class="info-label">Status:</div>
      <div class="info-value">
        <span class="badge badge-${application.txtStatus === 'APPROVED' ? 'success' : application.txtStatus === 'REJECTED' ? 'danger' : application.txtStatus === 'PENDING' ? 'warning' : 'secondary'}">
          ${escapeHtml(application.txtStatus || 'N/A')}
        </span>
      </div>
      
      <div class="info-label">Approval Level:</div>
      <div class="info-value">
        <span class="badge badge-info">Level ${application.intCurrentApprovalLevel || 0}</span>
      </div>
      
      <div class="info-label">Submitted Date:</div>
      <div class="info-value">${formatDate(application.dteCreatedDate)}</div>
    </div>
    ${application.txtRemarks ? `
    <div class="remarks-box">
      <strong>Remarks:</strong> ${escapeHtml(application.txtRemarks)}
    </div>
    ` : ''}
  </div>

  ${formFields.length > 0 ? `
  <div class="section">
    <h2 class="section-title">Form Data</h2>
    <table class="data-table">
      <thead>
        <tr>
          <th style="width: 40%;">Field Name</th>
          <th style="width: 60%;">Value</th>
        </tr>
      </thead>
      <tbody>
        ${formFields.map(field => `
          <tr>
            <td><strong>${escapeHtml(field.label)}${field.required ? ' <span style="color: #e74c3c;">*</span>' : ''}</strong></td>
            <td>${getFieldDisplayHtml(field)}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>
  ` : ''}

  ${pipelines.length > 0 ? `
  <div class="section pipeline-section">
    <h2 class="section-title">Department Approval Pipeline</h2>
    <div class="pipeline-flow">
      ${pipelines.map((pipeline: any, index: number) => {
      const approved = isDepartmentApproved(pipeline.intApprovalOrder);
      const currentLevel = application.intCurrentApprovalLevel || 0;
      const isPending = !approved && currentLevel === (pipeline.intApprovalOrder - 1);
      const statusClass = approved ? 'approved' : isPending ? 'pending' : 'not-started';
      const statusText = approved ? '✓ Approved' : isPending ? '⏳ Pending' : '○ Not Started';
      const deptName = pipeline.hrTblDepartment?.txtDepartmentName || `Department ${pipeline.serDepartmentId || pipeline.intApprovalOrder}`;

      return `
          <div class="pipeline-card ${statusClass}">
            <div class="dept-name">${escapeHtml(deptName)}</div>
            <div class="level">Level ${pipeline.intApprovalOrder}</div>
            <div class="status">${statusText}</div>
            ${approved && application.txtRemarks && currentLevel === pipeline.intApprovalOrder ? `
              <div class="remarks">
                <strong>Remarks:</strong><br>
                "${escapeHtml(application.txtRemarks)}"
              </div>
            ` : ''}
          </div>
          ${index < pipelines.length - 1 ? '<span class="arrow">→</span>' : ''}
        `;
    }).join('')}
    </div>
  </div>
  ` : ''}

  <div class="footer">
    Generated on ${new Date().toLocaleString()} | Document ID: ${application.txtFormCode || application.serApplicationId || 'N/A'}
  </div>
</body>
</html>`;

    return html;
  }

  private generateBudgetApprovalPdfHtml(application: any, formFields: any[], applicationFormData: any, formName: string): string {
    const { heading, contentHtml, preparedBy, reviewers, recommenders, approver, footerFields } = this.buildBudgetApprovalContent(applicationFormData);
    const headingText = heading || formName || 'Budget Approval';
    const dateStr = application?.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleDateString() : new Date().toLocaleDateString();
    return this.generateBudgetApprovalXyzHtml(headingText, dateStr, contentHtml, application?.txtApprovalHistory, preparedBy, reviewers, recommenders, approver, footerFields || []);
  }

  private generateBudgetApprovalXyzHtml(
    headingText: string,
    dateStr: string,
    contentHtml: string,
    approvalHistoryJson?: string,
    preparedBy?: any,
    reviewers: any[] = [],
    recommenders: any[] = [],
    approver?: any,
    footerFields: any[] = []
  ): string {
    let approvalHistory: any[] = [];
    if (approvalHistoryJson) {
      try {
        approvalHistory = JSON.parse(approvalHistoryJson);
      } catch (e) {
        approvalHistory = [];
      }
    }

    const getUserId = (user: any): number | null => {
      if (!user) return null;
      return user.serUserId || user.userId || user.id || null;
    };

    const getUserSignatureUrl = (user: any): string => {
      const userId = getUserId(user);
      if (!userId) return '';
      const entry = approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
      if (!entry || !entry.signaturePath) return '';
      return `${urls.API_URL}getSignature?userId=${userId}`;
    };

    const isUserApproved = (user: any): boolean => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory || approvalHistory.length === 0) return false;
      const entry = approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
      if (!entry) return false;
      if (!entry.signaturePath) return false;
      const action = (entry.action || entry.status || '').toString().toUpperCase();
      if (action === 'REJECTED') return false;
      if (action === 'APPROVED') return true;
      return !!entry.approvedDate;
    };

    const getUserApprovalDate = (user: any): string => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory || approvalHistory.length === 0) return '';
      const entry = approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
      if (!entry || !entry.approvedDate) return '';
      try {
        const dt = new Date(entry.approvedDate);
        if (isNaN(dt.getTime())) return String(entry.approvedDate);
        return dt.toLocaleString();
      } catch (e) {
        return String(entry.approvedDate);
      }
    };

    const renderUserCell = (user: any): string => {
      if (!user) return '';
      const sigUrl = getUserSignatureUrl(user);
      const sigDate = getUserApprovalDate(user);
      const approved = isUserApproved(user);
      return `
        ${approved && sigUrl ? `<img class="xyz-sig-img" src="${sigUrl}" alt="Signature" crossorigin="anonymous" />` : ''}
        ${approved && sigDate ? `<div class="xyz-sig-time">${sigDate}</div>` : ''}
      `;
    };

    const renderUserNameCell = (user: any, fallbackName?: string, fallbackRole?: string): string => {
      if (!user && !fallbackName) return '';
      const name = user?.txtUserName || fallbackName || '';
      const role = user?.cfgTblRole?.txtRoleName || fallbackRole || '';
      return `<div>${name}${role ? `<br>(${role})` : ''}</div>`;
    };
    const renderUsersInline = (users: any[]): string => {
      if (!Array.isArray(users) || users.length === 0) return '--';
      return users
        .map((u: any) => {
          const name = u?.txtUserName || u?.userName || u?.name || '';
          const role = u?.cfgTblRole?.txtRoleName || u?.roleName || u?.designation || '';
          return role ? `${name} (${role})` : name;
        })
        .filter((v: string) => !!v)
        .join(', ');
    };
    const renderFooterSignatureCell = (users: any[]): string => {
      if (!Array.isArray(users) || users.length === 0) return '';
      const approvedUsers = users.filter((u: any) => isUserApproved(u));
      if (approvedUsers.length === 0) return '';
      return approvedUsers.slice(0, 2).map((u: any) => renderUserCell(u)).join('');
    };
    const hasDynamicFooter = Array.isArray(footerFields) && footerFields.length > 0;
    const css = `
    :root { --ink:#111827; --muted:#6b7280; --line:#c7cdd4; --accent:#0f766e; --soft:#eef4f3; }
    * { box-sizing: border-box; }
    body { margin: 0; padding: 0; background:#ffffff; color:var(--ink); }
    .xyz-page { background:#ffffff; padding: 0; display:block; }
    .xyz-paper { width: 210mm; min-height: 297mm; background:#ffffff; font-family: "Georgia", "Times New Roman", serif; font-size: 13.5px; line-height: 1.45; border: none; padding: 16mm 16mm 16mm 16mm; display:flex; flex-direction:column; }
    .xyz-date-row { display:flex; justify-content:flex-end; margin-bottom:6px; font-family: "Calibri", "Arial", sans-serif; color:var(--muted); }
    .xyz-date { font-size:12px; text-align:right; }
    .xyz-header { display:grid; grid-template-columns:70px 1fr 140px; align-items:center; column-gap:10px; margin-bottom:4px; }
    .xyz-logo { align-self:start; }
    .xyz-logo img { width:58px; height:auto; display:block; }
    .xyz-company { text-align:center; }
    .xyz-company-name { font-family: "Cambria", "Georgia", "Times New Roman", serif; font-size:26px; font-weight:700; letter-spacing:0.2px; }
    .xyz-company-address { font-family: "Calibri", "Arial", sans-serif; font-size:11.5px; color:var(--muted); margin-top:2px; }
    .xyz-meta { text-align:right; font-family: "Calibri", "Arial", sans-serif; font-size:11.5px; color:var(--muted); }
    .xyz-rule { height:1px; background:var(--line); margin:8px 0 12px 0; position:relative; }
    .xyz-rule::before, .xyz-rule::after { content:""; position:absolute; left:0; right:0; height:1px; background:var(--line); }
    .xyz-rule::before { top:-2px; }
    .xyz-rule::after { bottom:-2px; }
    .xyz-title { text-align:center; font-family: "Cambria", "Georgia", "Times New Roman", serif; font-weight:700; font-size:22px; letter-spacing:0.6px; text-transform:uppercase; margin:6px 0 14px 0; }
    .xyz-dynamic { margin-top:4px; }
    .xyz-section { margin-bottom:10px; }
    .xyz-section-title { font-family: "Georgia", "Times New Roman", serif; font-size:13.5px; font-weight:700; text-transform:uppercase; letter-spacing:0.4px; border-left:3px solid var(--accent); padding-left:8px; margin-bottom:4px; }
    .xyz-section-text { font-family: "Georgia", "Times New Roman", serif; font-size:13.5px; font-weight:400; text-align:justify; color:var(--ink); }
    .xyz-list { margin: 4px 0 0 18px; padding: 0; }
    .xyz-list li { margin-bottom: 6px; }
    .xyz-bold { font-weight:700; }
    .xyz-table { width:100%; border-collapse:collapse; margin:10px 0; font-size:13px; }
    .xyz-table th, .xyz-table td { border:1px solid var(--line); padding:6px 8px; }
    .xyz-table thead th { background:var(--soft); text-align:center; font-weight:700; }
    .xyz-table tbody td:first-child, .xyz-table tbody td:last-child { text-align:center; }
    .xyz-col-sr { width:8%; text-align:center; }
    .xyz-col-amount { width:18%; text-align:center; }
    .xyz-note { margin-top:6px; font-size:11.5px; color:var(--muted); }
    .xyz-signatures { width:100%; border-collapse:collapse; margin-top:14px; font-family: "Calibri", "Arial", sans-serif; font-size:12px; }
    .xyz-signatures th, .xyz-signatures td { border:1px solid var(--line); padding:6px 8px; vertical-align:top; text-align:left; }
    .xyz-signatures-blank td { height:62px; padding:6px 8px; background:#fff; }
    .xyz-signatures th { font-size:12.5px; font-weight:700; background:#e5e7eb; text-transform:uppercase; letter-spacing:0.3px; }
    .xyz-signatures th[colspan="2"] { text-align:center; }
    .xyz-sig-img { max-height: 30px; max-width: 100%; object-fit: contain; display:block; margin-bottom:4px; }
    .xyz-sig-time { font-size:10px; color:var(--muted); margin-bottom:4px; }
    .xyz-footer { margin-top:auto; }
    `;

    return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body>
  <div class="abc-wrapper">
    <style>${css}</style>
    <div class="xyz-page">
      <div class="xyz-paper">
      <div class="xyz-date-row"><div class="xyz-date">Date: ${dateStr}</div></div>
      <div class="xyz-header">
        <div class="xyz-logo"><img src="assets/images/qarshi-logo.png" alt="Qarshi" /></div>
        <div class="xyz-company">
          <div class="xyz-company-name">Qarshi Industries (Pvt) Ltd.</div>
          <div class="xyz-company-address">15-6, Jam-e-Shirin Boulevard, Gulberg-III, Lahore</div>
        </div>
        <div class="xyz-meta">Form: ${headingText}</div>
      </div>
      <div class="xyz-rule"></div>
      <div class="xyz-title">${headingText}</div>
      <div class="xyz-dynamic">${contentHtml}</div>

      <div class="xyz-footer">
      <table class="xyz-signatures">
        ${hasDynamicFooter ? `
        <tr class="xyz-signatures-blank">
          ${(footerFields || []).map((f: any) => `<td>${renderFooterSignatureCell(Array.isArray(f?.users) ? f.users : [])}</td>`).join('')}
        </tr>
        <tr>
          ${(footerFields || []).map((f: any) => `<th>${f?.label || 'New Field'}</th>`).join('')}
        </tr>
        <tr>
          ${(footerFields || []).map((f: any) => `<td>${renderUsersInline(Array.isArray(f?.users) ? f.users : [])}</td>`).join('')}
        </tr>` : `
        <tr class="xyz-signatures-blank">
          <td>${isUserApproved(preparedBy) ? renderUserCell(preparedBy) : ''}</td>
          <td>${isUserApproved(reviewers?.[0]) ? renderUserCell(reviewers?.[0]) : ''}</td>
          <td>${isUserApproved(reviewers?.[1]) ? renderUserCell(reviewers?.[1]) : ''}</td>
          <td>${isUserApproved(recommenders?.[0]) ? renderUserCell(recommenders?.[0]) : ''}</td>
          <td>${isUserApproved(approver) ? renderUserCell(approver) : ''}</td>
        </tr>
        <tr>
          <th>Prepared by:</th>
          <th colspan="2">Reviewed by:</th>
          <th>Recommended by:</th>
          <th>Approved by:</th>
        </tr>
        <tr>
          <td>${renderUserNameCell(preparedBy, preparedBy?.txtUserName, preparedBy?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(reviewers?.[0], reviewers?.[0]?.txtUserName, reviewers?.[0]?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(reviewers?.[1], reviewers?.[1]?.txtUserName, reviewers?.[1]?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(recommenders?.[0], recommenders?.[0]?.txtUserName, recommenders?.[0]?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(approver, approver?.txtUserName, approver?.cfgTblRole?.txtRoleName)}</td>
        </tr>`}
      </table>
      </div>
    </div>
  </div>
  </div>
</body>
</html>`;
  }

  private buildBudgetApprovalContent(applicationFormData: any): {
    heading: string;
    contentHtml: string;
    preparedBy?: any,
    reviewers?: any[],
    recommenders?: any[],
    approver?: any,
    footerFields?: any[]
  } {
    const escapeHtml = (text: string): string => {
      if (text === null || text === undefined) return '';
      const div = document.createElement('div');
      div.textContent = String(text);
      return div.innerHTML;
    };

    const sanitizeBudgetHtml = (html: string): string => {
      const wrapper = document.createElement('div');
      wrapper.innerHTML = html;

      const candidates = wrapper.querySelectorAll('button, input, a, [role="button"]');
      candidates.forEach(el => {
        const tag = el.tagName.toLowerCase();
        const text = (el.textContent || '').toLowerCase();
        const value = (el as HTMLInputElement).value ? (el as HTMLInputElement).value.toLowerCase() : '';
        const type = tag === 'input' ? ((el as HTMLInputElement).type || '').toLowerCase() : '';
        const hasResetText = text.includes('reset') || text.includes('close & reset');
        const hasResetValue = value.includes('reset') || value.includes('close & reset');
        const isResetType = type === 'reset';
        if (hasResetText || hasResetValue || isResetType) {
          el.remove();
        }
      });

      return wrapper.innerHTML;
    };

    const getValueByLabelContains = (needle: string): any => {
      if (!applicationFormData) return '';
      const key = Object.keys(applicationFormData).find(k => k.toLowerCase().includes(needle.toLowerCase()));
      return key ? applicationFormData[key] : '';
    };

    const getValueByAnyLabel = (needles: string[]): any => {
      if (!applicationFormData) return '';
      for (const n of needles) {
        const key = Object.keys(applicationFormData).find(k => k.toLowerCase().includes(n.toLowerCase()));
        if (key) return applicationFormData[key];
      }
      return '';
    };

    const headingRaw = getValueByAnyLabel(['heading', 'title', 'subject', 'request title', 'form title']);
    const heading = headingRaw ? String(headingRaw).trim() : '';
    const background = getValueByLabelContains('background');
    const proposal = getValueByLabelContains('proposal');
    const request = getValueByLabelContains('request');
    const finances = getValueByLabelContains('finance');
    const note = getValueByLabelContains('note');

    let contentHtml = '';

    // If rich HTML content exists, prefer it (still keep heading from fields)
    const rawHtml = applicationFormData?.content || applicationFormData?.editorContent || applicationFormData?.html;
    const result: any = {
      heading,
      contentHtml: '', // default
      preparedBy: applicationFormData?.preparedBy,
      reviewers: applicationFormData?.reviewers || [],
      recommenders: applicationFormData?.recommenders || [],
      approver: applicationFormData?.approver,
      footerFields: applicationFormData?.footerFields || []
    };

    if (rawHtml) {
      result.contentHtml = sanitizeBudgetHtml(String(rawHtml));
      return result;
    }

    if (background) {
      contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Background</div><div class="xyz-section-text">${escapeHtml(background)}</div></div>`;
    }

    if (proposal) {
      const proposalText = String(proposal);
      const items = proposalText
        .split(/\r?\n/)
        .map(i => i.replace(/\t+/g, ' ').trim())
        .filter(i => i.length > 0);
      const listItems = items.length > 1 ? items : proposalText.split(/(?=\d+\.)/).map(i => i.trim()).filter(Boolean);
      if (listItems.length > 1) {
        contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Proposal</div><ol class="xyz-list">${listItems.map(i => `<li>${escapeHtml(i.replace(/^\d+\.\s*/, '').trim())}</li>`).join('')}</ol></div>`;
      } else {
        contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Proposal</div><div class="xyz-section-text">${escapeHtml(proposalText)}</div></div>`;
      }
    }

    if (request) {
      contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Request</div><div class="xyz-section-text">${escapeHtml(request)}</div></div>`;
    }

    if (finances) {
      let rows: string[][] = [];
      let headerRow: string[] | null = null;
      if (Array.isArray(finances)) {
        if (finances.length && typeof finances[0] === 'object' && !Array.isArray(finances[0])) {
          // array of objects
          rows = finances.map((r: any) => [r.sr || r.Sr || r['Sr.'] || '', r.description || r.Description || '', r.amount || r.Amount || ''].map((c: any) => String(c)));
        } else {
          rows = finances.map((r: any) => Array.isArray(r) ? r.map((c: any) => String(c)) : [String(r)]);
        }
      } else if (typeof finances === 'object' && finances !== null) {
        // object with rows
        if (Array.isArray(finances.rows)) {
          rows = finances.rows.map((r: any) => Array.isArray(r) ? r.map((c: any) => String(c)) : [String(r)]);
        }
      } else if (typeof finances === 'string') {
        const parts = finances.split(',').map(p => p.trim()).filter(Boolean);
        if (parts.length >= 3 && parts[0].toLowerCase().includes('sr') && parts[1].toLowerCase().includes('description')) {
          parts.splice(0, 3);
        }
        for (let i = 0; i < parts.length; i += 3) {
          rows.push([parts[i] || '', parts[i + 1] || '', parts[i + 2] || '']);
        }
      }

      if (rows.length) {
        const first = rows[0] || [];
        const isHeader =
          first.length >= 3 &&
          first[0].toLowerCase().includes('sr') &&
          first[1].toLowerCase().includes('description') &&
          first[2].toLowerCase().includes('amount');
        if (isHeader) {
          headerRow = first;
          rows = rows.slice(1);
        }

        const headerHtml = headerRow
          ? `<tr><th class="xyz-col-sr">${escapeHtml(headerRow[0])}</th><th>${escapeHtml(headerRow[1])}</th><th class="xyz-col-amount">${escapeHtml(headerRow[2])}</th></tr>`
          : `<tr><th class="xyz-col-sr">Sr.</th><th>Description</th><th class="xyz-col-amount">Amount</th></tr>`;

        contentHtml += `<table class="xyz-table"><thead>${headerHtml}</thead><tbody>`;
        rows.forEach(r => {
          contentHtml += `<tr><td>${escapeHtml(r[0] || '')}</td><td>${escapeHtml(r[1] || '')}</td><td>${escapeHtml(r[2] || '')}</td></tr>`;
        });
        contentHtml += `</tbody></table>`;
      }
    }

    if (note) {
      contentHtml += `<div class="xyz-note"><span class="xyz-bold">Note:</span> ${escapeHtml(note)}</div>`;
    }

    return { heading, contentHtml };
  }

  private generateCapfAbcHtml(application: any, formFields: any[], applicationFormData: any, pipelines: any[] = []): string {
    // Helper function to get field value - completely self-contained, no dependency on abc component
    const getFieldValue = (fieldLabel: string): string => {
      // Helper: Slugify label to match backend keys
      const getSlug = (label: string): string => {
        return label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
      };

      if (!applicationFormData || Object.keys(applicationFormData).length === 0) {
        return '';
      }

      // 1. Concept Mapping (Template Label -> Concept)
      const templateKeyToConcept: { [key: string]: string } = {
        'DIVISION / DEPARTMENT': 'division',
        'CAPF #': 'capfNumber',
        'Date': 'date',
        'NAME OF ASSET / ITEM': 'assetName',
        'DETAIL SPECIFICATION': 'specification',
        'UTILITY & PURPOSE': 'utility',
        'FEASIBILITY REPORT ATTACHED': 'feasibilityReport',
        'IF NO THEN MENTION REASON': 'reason',
        'NAME': 'vendorName',
        'ADDRESS': 'vendorAddress',
        'APPROVED PRICE': 'approvedPrice',
        'DELIVERY PERIOD & DATE': 'deliveryPeriod',
        'TERMS & CONDITIONS': 'termsConditions',
        'Third Party assessment carried out': 'thirdPartyAssessment',
        'Third Party Assessment': 'thirdPartyAssessment',
        'Third Party assessment': 'thirdPartyAssessment'
      };

      // 2. Synonyms Mapping (Concept -> Potential Labels)
      // ORDER MATTERS: Put most specific/likely labels first to avoid collisions
      const fieldMappings: { [key: string]: string[] } = {
        'division': ['DIVISION / DEPARTMENT', 'Division', 'Department', 'division'],
        'capfNumber': ['CAPF #', 'CAPF', 'Capf Number', 'capf_number'],
        'date': ['Date', 'Submission Date', 'date'],
        'assetName': ['NAME OF ASSET / ITEM', 'Name of Asset', 'Asset Name', 'Item Name', 'asset_name'],
        'specification': ['DETAIL SPECIFICATION', 'DETAIL SPECIFICATION:', 'Detail Specification', 'Detail Specification:', 'Specification', 'specification', 'detail_specification', 'DETAIL_SPECIFICATION'],
        'utility': ['UTILITY & PURPOSE', 'Utility', 'Purpose', 'utility_purpose'],
        'feasibilityReport': ['FEASIBILITY REPORT ATTACHED', 'Feasibility Report', 'feasibility_report'],
        'reason': ['IF NO THEN MENTION REASON', 'Reason', 'If No Reason', 'reason'],
        'vendorName': ['Vendor Name', 'Vendor', 'Name of Vendor', 'NAME'], // 'NAME' moved to end to avoid collision with Asset Name 'Name'
        'vendorAddress': ['Vendor Address', 'Address', 'ADDRESS'],
        'approvedPrice': ['APPROVED PRICE', 'Approved Price', 'Price', 'Cost'],
        'deliveryPeriod': ['DELIVERY PERIOD & DATE', 'Delivery Period', 'Delivery Date'],
        'termsConditions': ['TERMS & CONDITIONS', 'Terms and Conditions', 'Terms & Conditions'],
        'thirdPartyAssessment': ['Third Party assessment carried out', 'Third Party Assessment', 'Third Party assessment', 'Third Party Assessment Carried Out', 'third_party_assessment', 'thirdPartyAssessment']
      };

      const lookupLabel = (lbl: string): string | null => {
        // Normalize label (remove colon, trim)
        const normalizedLbl = lbl.replace(/[:;]/g, '').trim();

        // Direct key check (with original label)
        if (applicationFormData[lbl] !== undefined && applicationFormData[lbl] !== null && applicationFormData[lbl] !== '') {
          return String(applicationFormData[lbl]);
        }

        // Direct key check (with normalized label)
        if (applicationFormData[normalizedLbl] !== undefined && applicationFormData[normalizedLbl] !== null && applicationFormData[normalizedLbl] !== '') {
          return String(applicationFormData[normalizedLbl]);
        }

        // Slug check (keys are usually slugs) - with original
        const slug = getSlug(lbl);
        if (applicationFormData[slug] !== undefined && applicationFormData[slug] !== null && applicationFormData[slug] !== '') {
          return String(applicationFormData[slug]);
        }

        // Slug check with normalized
        const normalizedSlug = getSlug(normalizedLbl);
        if (applicationFormData[normalizedSlug] !== undefined && applicationFormData[normalizedSlug] !== null && applicationFormData[normalizedSlug] !== '') {
          return String(applicationFormData[normalizedSlug]);
        }

        // Case-insensitive check (with original)
        const lowerLbl = lbl.toLowerCase().trim();
        for (const key in applicationFormData) {
          if (key.toLowerCase().trim() === lowerLbl && applicationFormData[key] !== null && applicationFormData[key] !== undefined && applicationFormData[key] !== '') {
            return String(applicationFormData[key]);
          }
        }

        // Case-insensitive check (with normalized - no colon)
        const lowerNormalized = normalizedLbl.toLowerCase().trim();
        for (const key in applicationFormData) {
          const keyNormalized = key.replace(/[:;]/g, '').toLowerCase().trim();
          if (keyNormalized === lowerNormalized && applicationFormData[key] !== null && applicationFormData[key] !== undefined && applicationFormData[key] !== '') {
            return String(applicationFormData[key]);
          }
        }

        // Form Fields lookup (match label exact, then checkout slug)
        if (formFields && formFields.length > 0) {
          // Try exact match first
          const field = formFields.find(f => {
            if (!f.label) return false;
            const fieldLabelNormalized = f.label.replace(/[:;]/g, '').toLowerCase().trim();
            return fieldLabelNormalized === lowerNormalized || f.label.toLowerCase().trim() === lowerLbl;
          });

          if (field) {
            const fieldSlug = getSlug(field.label);
            if (applicationFormData[fieldSlug] !== undefined && applicationFormData[fieldSlug] !== null && applicationFormData[fieldSlug] !== '') {
              return String(applicationFormData[fieldSlug]);
            }

            // Also try normalized slug
            const fieldLabelNormalized = field.label.replace(/[:;]/g, '').trim();
            const normalizedFieldSlug = getSlug(fieldLabelNormalized);
            if (applicationFormData[normalizedFieldSlug] !== undefined && applicationFormData[normalizedFieldSlug] !== null && applicationFormData[normalizedFieldSlug] !== '') {
              return String(applicationFormData[normalizedFieldSlug]);
            }
          }

          // Try partial match for "specification" related fields
          if (lowerNormalized.includes('specification') || lowerLbl.includes('specification')) {
            const specField = formFields.find(f => {
              if (!f.label) return false;
              const fieldLabelLower = f.label.toLowerCase();
              return fieldLabelLower.includes('specification') || fieldLabelLower.includes('detail');
            });
            if (specField) {
              const specFieldSlug = getSlug(specField.label);
              if (applicationFormData[specFieldSlug] !== undefined && applicationFormData[specFieldSlug] !== null && applicationFormData[specFieldSlug] !== '') {
                return String(applicationFormData[specFieldSlug]);
              }
            }
          }
        }
        return null;
      };

      const concept = templateKeyToConcept[fieldLabel];
      if (concept && fieldMappings[concept]) {
        for (const label of fieldMappings[concept]) {
          const val = lookupLabel(label);
          if (val !== null && val !== '') return val;
        }

        // Additional fallback: search through all form fields for concept-related fields
        if (formFields && formFields.length > 0) {
          const conceptKeywords: { [key: string]: string[] } = {
            'specification': ['specification', 'detail', 'spec'],
            'division': ['division', 'dept', 'department'],
            'assetName': ['asset', 'item', 'name'],
            'utility': ['utility', 'purpose'],
            'vendorName': ['vendor', 'name'],
            'vendorAddress': ['address', 'vendor'],
            'approvedPrice': ['price', 'approved', 'cost'],
            'deliveryPeriod': ['delivery', 'period', 'date'],
            'termsConditions': ['terms', 'conditions'],
            'thirdPartyAssessment': ['third', 'party', 'assessment', 'carried']
          };

          const keywords = conceptKeywords[concept];
          if (keywords) {
            for (const field of formFields) {
              if (!field.label) continue;
              const fieldLabelLower = field.label.toLowerCase();
              const matchesKeyword = keywords.some(kw => fieldLabelLower.includes(kw));

              if (matchesKeyword) {
                const fieldSlug = getSlug(field.label);
                if (applicationFormData[fieldSlug] !== undefined && applicationFormData[fieldSlug] !== null && applicationFormData[fieldSlug] !== '') {
                  return String(applicationFormData[fieldSlug]);
                }
              }
            }
          }
        }
      }

      // Fallback
      const val = lookupLabel(fieldLabel);
      return val !== null && val !== '' ? val : '';
    };

    const getCapfNumber = (): string => {
      return getFieldValue('CAPF #') || application.txtFormCode || '';
    };

    const getDate = (): string => {
      const dateValue = getFieldValue('Date') || application.dteCreatedDate || '';
      if (dateValue) {
        try {
          const date = new Date(dateValue);
          return date.toLocaleDateString();
        } catch (e) {
          return dateValue;
        }
      }
      return '';
    };

    const escapeHtml = (text: string): string => {
      if (!text) return '';
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    };

    const feasibilityValue = getFieldValue('FEASIBILITY REPORT ATTACHED');
    const feasibilityYes = feasibilityValue && (feasibilityValue.toLowerCase() === 'yes' || feasibilityValue.toLowerCase() === 'true');
    const feasibilityNo = feasibilityValue && (feasibilityValue.toLowerCase() === 'no' || feasibilityValue.toLowerCase() === 'false');

    const thirdPartyValue = getFieldValue('Third Party assessment carried out') || getFieldValue('Third Party Assessment') || getFieldValue('Third Party assessment');
    const thirdPartyYes = thirdPartyValue && (thirdPartyValue.toLowerCase() === 'yes' || thirdPartyValue.toLowerCase() === 'true');
    const thirdPartyNo = thirdPartyValue && (thirdPartyValue.toLowerCase() === 'no' || thirdPartyValue.toLowerCase() === 'false');
    const thirdPartyNA = thirdPartyValue && (thirdPartyValue.toLowerCase() === 'na' || thirdPartyValue.toLowerCase() === 'n/a' || thirdPartyValue.toLowerCase() === 'not applicable');

    // Parse approval history for signatures
    let approvalHistory: any[] = [];
    if (application?.txtApprovalHistory) {
      try {
        approvalHistory = JSON.parse(application.txtApprovalHistory);
        console.log('[CAPF FE][applications-view] parsed approval history', approvalHistory);
      } catch (e) {
        console.error('[CAPF FE][applications-view] failed to parse approval history', e);
        approvalHistory = [];
      }
    }
    console.log('[CAPF FE][applications-view] approvalHistoryCount=', approvalHistory.length, 'appId=', application?.serApplicationId);

    const getApprovalEntryForPipeline = (order: number, departmentId?: number, departmentName?: string): any | null => {
      if (!approvalHistory || approvalHistory.length === 0) return null;

      let entry = null;
      if (departmentId) {
        entry = approvalHistory.find((e: any) =>
          (e.level === order || e.intApprovalOrder === order) &&
          (Number(e.departmentId) === Number(departmentId) || Number(e.serDepartmentId) === Number(departmentId))
        );
      }
      if (!entry) {
        entry = approvalHistory.find((e: any) => e.level === order || e.intApprovalOrder === order);
      }
      if (!entry && departmentId) {
        entry = approvalHistory.find((e: any) =>
          Number(e.departmentId) === Number(departmentId) || Number(e.serDepartmentId) === Number(departmentId)
        );
      }
      if (!entry && departmentName) {
        const nameLower = departmentName.toLowerCase();
        entry = approvalHistory.find((e: any) =>
          (e.departmentName || '').toString().toLowerCase() === nameLower
        );
      }
      console.log('[CAPF FE][applications-view] getApprovalEntryForPipeline', {
        order,
        departmentId,
        departmentName,
        matched: !!entry,
        matchedLevel: entry?.level,
        matchedOrder: entry?.intApprovalOrder,
        matchedApprovedBy: entry?.approvedBy || entry?.approverUserId || entry?.userId,
        matchedSignaturePath: entry?.signaturePath || ''
      });
      return entry || null;
    };

    const isPipelineApproved = (order: number): boolean => {
      const currentLevel = application.intCurrentApprovalLevel || 0;
      const status = application.txtStatus?.toUpperCase() || '';
      if (status === 'APPROVED') return true;
      return currentLevel >= order;
    };

    const buildSignatureSlots = (): { nameText: string; designationText: string; departmentText: string; html: string; time: string }[] => {
      const staticLabels = [
        'User Deptt. (HoD)',
        'Technical Expert',
        'Procurement',
        'Finance',
        'Core Team HTR. / CCT HO'
      ];
      const getNameText = (entry: any): string => {
        return (
          entry?.approverName ||
          entry?.approvedByName ||
          entry?.userName ||
          (entry?.approvedBy && isNaN(Number(entry.approvedBy)) ? String(entry.approvedBy) : '') ||
          ''
        );
      };
      const getDesignationText = (entry: any): string => {
        return (
          entry?.txtDesignation ||
          entry?.designation ||
          entry?.approverDesignation ||
          entry?.role ||
          ''
        );
      };

      const sortedPipelines = Array.isArray(pipelines)
        ? [...pipelines].sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0))
        : [];

      if (sortedPipelines.length === 0) {
        const fallback = [
          { label: 'User Deptt. (HoD)', order: 1 },
          { label: 'Technical Expert', order: 2 },
          { label: 'Procurement', order: 3 },
          { label: 'Finance', order: 4 },
          { label: 'Core Team HTR. / CCT HO', order: 5 }
        ];
        return fallback.map((f) => {
          const entry = getApprovalEntryForPipeline(f.order);
          const nameText = getNameText(entry);
          const designationText = getDesignationText(entry);
          const departmentText = f.label;
          const userId = entry?.approvedBy || entry?.approverUserId || entry?.userId;
          const hasSignature = !!entry?.signaturePath;
          const signatureUrl = userId && hasSignature ? `${urls.API_URL}getSignature?userId=${userId}` : '';
          console.log('[CAPF FE][applications-view][slot-fallback]', {
            slot: f.order,
            label: f.label,
            approvedBy: userId,
            entrySignaturePath: entry?.signaturePath || '',
            hasSignature,
            signatureUrl
          });
          const html = signatureUrl
            ? `<img class="sig-img" src="${signatureUrl}" alt="Signature" crossorigin="anonymous" />`
            : (isPipelineApproved(f.order) ? '<span style="font-weight: bold; font-size: 11px;">Approved</span>' : '');
          const time = hasSignature && entry?.approvedDate ? (() => {
            try {
              const dt = new Date(entry.approvedDate);
              return isNaN(dt.getTime()) ? String(entry.approvedDate) : dt.toLocaleString();
            } catch {
              return String(entry.approvedDate);
            }
          })() : '';
          return { nameText, designationText, departmentText, html, time };
        });
      }

      return sortedPipelines.slice(0, staticLabels.length).map((pipeline: any, index: number) => {
        const order = pipeline.intApprovalOrder || (index + 1);
        const departmentId = pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId || pipeline.departmentId;
        const entry = getApprovalEntryForPipeline(order, departmentId);
        const nameText = getNameText(entry);
        const designationText = getDesignationText(entry);
        const departmentText = staticLabels[index] || `Department ${order}`;
        const userId = entry?.approvedBy || entry?.approverUserId || entry?.userId;
        const hasSignature = !!entry?.signaturePath;
        const signatureUrl = userId && hasSignature ? `${urls.API_URL}getSignature?userId=${userId}` : '';
        console.log('[CAPF FE][applications-view][slot-pipeline]', {
          slot: index + 1,
          order,
          departmentId,
          approvedBy: userId,
          entryLevel: entry?.level,
          entryOrder: entry?.intApprovalOrder,
          entrySignaturePath: entry?.signaturePath || '',
          hasSignature,
          signatureUrl
        });
        const html = signatureUrl
          ? `<img class="sig-img" src="${signatureUrl}" alt="Signature" crossorigin="anonymous" />`
          : (isPipelineApproved(order) ? '<span style="font-weight: bold; font-size: 11px;">Approved</span>' : '');
        const time = hasSignature && entry?.approvedDate ? (() => {
          try {
            const dt = new Date(entry.approvedDate);
            return isNaN(dt.getTime()) ? String(entry.approvedDate) : dt.toLocaleString();
          } catch {
            return String(entry.approvedDate);
          }
        })() : '';
        return { nameText, designationText, departmentText, html, time };
      });
    };

    const signatureSlots = buildSignatureSlots();

    // CSS styles for CAPF form PDF - exact copy of abc.component.css to ensure identical rendering
    const cssStyles = `
    :root {
      --ink: #111;
      --line: #222;
    }

    .abc-wrapper {
      box-sizing: border-box;
    }

    .abc-wrapper * {
      box-sizing: border-box;
    }

    .abc-wrapper {
      margin: 0;
      padding: 0;
      background: #fff;
      color: var(--ink);
      font-family: Arial, Helvetica, sans-serif;
      min-height: 100vh;
      width: 100%;
      position: absolute;
      top: 0;
      left: 0;
      z-index: 9999;
      /* Ensure it covers everything if needed */
    }

    /* PAGE */
    .page {
      width: 820px;
      margin: 0 auto;
      border: 2px solid var(--line);
      padding: 5px;
      box-sizing: border-box;
    }

    /* COMMON */
    .b {
      font-weight: 700;
    }

    .u {
      text-decoration: underline;
    }

    .small {
      font-size: 12px;
    }

    .xs {
      font-size: 11px;
    }

    .tight {
      line-height: 1.1;
    }

    .mt6 {
      margin-top: 6px;
    }

    .mt8 {
      margin-top: 8px;
    }

    .mt10 {
      margin-top: 10px;
    }

    .mt12 {
      margin-top: 12px;
    }

    .mt14 {
      margin-top: 14px;
    }

    .mb6 {
      margin-bottom: 6px;
    }

    .mb8 {
      margin-bottom: 8px;
    }

    .mb10 {
      margin-bottom: 10px;
    }

    .mb12 {
      margin-bottom: 12px;
    }

    /* TABLES */
    table {
      width: 100% !important;
      border-collapse: collapse;
      margin: 0;
      box-sizing: border-box;
    }

    .grid {
      width: 100%;
      table-layout: fixed;
      margin: 0;
      border-spacing: 0;
      box-sizing: border-box;
      display: table;
    }

    .grid td,
    .grid th {
      border: 1px solid var(--line);
      padding: 4px 6px;
      vertical-align: top;
      font-size: 12px;
      box-sizing: border-box;
    }

    .grid tr:first-child td {
      width: calc(100% / 3);
    }

    .grid th {
      font-weight: 700;
      text-align: left;
    }

    /* TOP BRAND */
    .brand-row {
      display: flex;
      gap: 10px;
      align-items: flex-start;
      margin-bottom: 2px;
    }

    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .logo img {
      max-width: 64px;
      height: auto;
    }

    .brand-title {
      font-weight: 700;
      font-size: 20px;
      letter-spacing: .2px;
    }

    /* MAIN TITLE BAR */
    .titlebar {
      border: 2px solid var(--line);
      text-align: center;
      font-weight: 700;
      font-weight: 700;
      padding: 4px 8px;
      margin-top: 4px;
      margin-bottom: 4px;
      letter-spacing: .5px;
    }

    /* SECTION BOX */
    .box {
      border: 2px solid var(--line);
      padding: 5px 5px 4px 5px;
      margin-top: 4px;
    }

    .box-title {
      text-align: center;
      font-weight: 700;
      margin: -2px 0 5px 0;
      letter-spacing: .2px;
    }

    /* FORM ROWS */
    .row {
      display: flex;
      gap: 12px;
      align-items: flex-end;
      margin-top: 4px;
      margin-bottom: 5px;
    }

    .field {
      display: flex;
      gap: 8px;
      align-items: flex-end;
      flex: 1;
      min-width: 0;
    }

    .label {
      font-size: 13px;
      font-weight: 700;
      white-space: nowrap;
    }

    .line {
      flex: 1;
      border-bottom: 1px solid var(--line);
      min-height: 22px;
      min-width: 40px;
      padding-bottom: 5px;
      margin-bottom: 5px;
      word-wrap: break-word;
      overflow-wrap: break-word;
      white-space: normal;
      line-height: 1.4;
    }

    .line.tall {
      min-height: 26px;
    }

    .line.xl {
      min-height: 32px;
    }

    /* CAPF + Date at right */
    .capf-right {
      display: flex;
      align-items: flex-end;
      gap: 12px;
      white-space: nowrap;
      margin-left: auto;
    }

    .capf-box {
      display: flex;
      align-items: flex-end;
      gap: 8px;
    }

    .capf-num {
      font-weight: 700;
      font-size: 20px;
      letter-spacing: 2px;
      border-bottom: 1px solid var(--line);
      padding: 0 6px 5px 6px;
      min-width: 86px;
      text-align: right;
      margin-bottom: 5px;
    }

    .date-line {
      width: 150px;
      border-bottom: 1px solid var(--line);
      min-height: 22px;
      padding-bottom: 5px;
      margin-bottom: 5px;
      word-wrap: break-word;
      overflow-wrap: break-word;
      white-space: normal;
      line-height: 1.4;
    }

    /* CHECKBOXES */
    .checks {
      display: flex;
      align-items: center;
      gap: 14px;
      margin: 8px 0;
      font-size: 13px;
      font-weight: 700;
    }

    .checks .label {
      flex-shrink: 0;
      white-space: nowrap;
    }

    .check-group {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 40px;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 700;
    }

    .boxcheck {
      width: 60px;
      height: 22px;
      border: 1px solid var(--line);
      border-radius: 4px;
      display: inline-block;
      position: relative;
      background: transparent;
    }
    
    .boxcheck.checked::after {
      content: '✓';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      font-size: 16px;
      font-weight: bold;
      color: #000;
    }

    /* MULTILINE NOTES */
    .note {
      margin-top: 10px;
      font-size: 12px;
    }

    .note ol {
      margin: 6px 0 0 18px;
      padding: 0;
    }

    .note li {
      margin: 2px 0;
    }

    /* SUBSECTION HEADING */
    .subhead {
      margin: 10px -10px 10px -10px;
      padding: 6px 10px;
      border-top: 2px solid var(--line);
      /* border-bottom removed */
      font-weight: 700;
      letter-spacing: .2px;
    }

    /* TERMS small parenthetical */
    .paren {
      font-size: 12px;
      font-weight: normal;
    }

    /* SIGNATURES */
    .sig-row {
      display: flex;
      gap: 18px;
      align-items: flex-end;
      margin-top: 30px;
      flex-wrap: nowrap;
    }

    .sig {
      flex: 1;
      min-width: 0;
    }

    .sig-line {
      border-bottom: 1px solid var(--line);
      height: 18px;
      margin-bottom: 4px;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .sig-img {
      max-height: 16px;
      max-width: 100%;
      object-fit: contain;
      display: block;
    }

    .sig-time {
      font-size: 10px;
      text-align: center;
      margin-bottom: 2px;
      line-height: 1.1;
      white-space: nowrap;
    }

    .sig-meta {
      font-size: 10px;
      text-align: center;
      margin-bottom: 2px;
      line-height: 1.1;
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
    }

    .sig-label {
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
      line-height: 1.2;
    }

    .approved {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      align-items: flex-end;
      margin-top: 8px;
    }

    .approved .who {
      font-weight: 700;
      white-space: nowrap;
    }

    .approved-sig {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .approved .appline {
      width: 150px;
      border-bottom: 1px solid var(--line);
      height: 14px;
      margin-bottom: 2px;
    }

    .approved-sig .who {
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      white-space: nowrap;
      width: 100%;
    }

    /* PART-2 + JOB COMPLETION */
    .part2-title {
      border-top: 2px solid var(--line);
      /* border-bottom removed */
      margin: 4px -5px 4px -5px;
      padding: 4px 10px;
      text-align: center;
      font-weight: 700;
      letter-spacing: .2px;
    }

    .two-col {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: 20px;
      margin-top: 5px;
    }

    .sign-block {
      width: 46%;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
    }

    .sign-block .sb-label {
      font-weight: 700;
      font-size: 13px;
    }

    .sign-block .sb-line {
      width: 100%;
      border-bottom: 1px solid var(--line);
      height: 14px;
    }

    .sign-block .sb-sub {
      font-size: 11px;
      font-weight: 700;
      margin-top: -2px;
      padding-left: 4px;
    }

    .job-title {
      text-align: center;
      font-weight: 700;
      margin: 5px -5px 5px -5px;
      padding-top: 4px;
      border-top: 2px solid var(--line);
      letter-spacing: .2px;
    }

    .job-text {
      font-size: 13px;
      font-weight: 700;
      margin-top: 4px;
      line-height: 1.35;
    }

    .inline-line {
      display: inline-block;
      border-bottom: 1px solid var(--line);
      min-height: 22px;
      vertical-align: baseline;
      width: 140px;
      margin: 0 6px 5px 6px;
      padding-bottom: 5px;
      word-wrap: break-word;
      overflow-wrap: break-word;
      white-space: normal;
      line-height: 1.4;
    }

    .inline-line.short {
      width: 110px;
    }

    .inline-line.long {
      width: 190px;
    }

    @media print {
      .abc-wrapper {
        padding: 0;
        position: static;
        background: #fff;
        width: 100%;
        margin: 0;
      }

      .page {
        border: 2px solid #000;
        width: 125%;
        max-width: 125%;
        margin: 0;
        page-break-after: avoid;
        transform: scale(0.75);
        transform-origin: top left;
      }

      body {
        margin: 0;
        padding: 0;
      }

      @page {
        size: A4;
        margin: 0.5cm;
      }
    }
    `;

    const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body style="margin:0; padding:0; background:#fff;">
  <div class="abc-wrapper">
    <style>${cssStyles}</style>
    <div class="page">
      <!-- Header -->
      <div class="brand-row">
        <div class="logo">
          <img src="assets/images/qarshi-logo.png" alt="" class="ml-[5px] w-16 flex-none" style="max-width: 64px;">
        </div>
        <div class="brand-title">Qarshi Industries (Pvt) Ltd.</div>
      </div>

      <table class="grid">
        <tr>
          <td><span class="b">Division:</span> ***</td>
          <td><span class="b">Department:</span> PRC</td>
          <td><span class="b">Section:</span> GEN</td>
        </tr>
        <tr>
          <td><span class="b">Document No.:</span> PRC-GEN-FM-03</td>
          <td><span class="b">Original Issue:</span> 01-06-2006</td>
          <td>
            <div style="display:flex; justify-content:space-between; gap:10px;">
              <span><span class="b">Rev.</span> # 05</span>
              <span><span class="b">Rev. Date:</span> 01-12-2015</span>
            </div>
          </td>
        </tr>
      </table>

      <div class="titlebar">CAPITAL ASSETS PURCHASE FORM</div>

      <!-- PART 1 -->
      <div class="box">
        <div class="box-title">PART-1 (TO BE FILLED BY <span class="u">CONCERNED</span> DEPARTMENT)</div>

        <div class="row">
          <div class="field">
            <div class="label">DIVISION / DEPARTMENT:</div>
            <div class="line">${escapeHtml(getFieldValue('DIVISION / DEPARTMENT'))}</div>
          </div>

          <div class="capf-right" style="flex-direction: column; align-items: flex-end;">
            <div class="capf-box" style="margin-bottom: 8px;">
              <div class="label">CAPF #</div>
              <div class="capf-num">${escapeHtml(getCapfNumber())}</div>
            </div>
            <div class="capf-box">
              <div class="label">Date:</div>
              <div class="date-line">${escapeHtml(getDate())}</div>
            </div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">NAME OF ASSET / ITEM:</div>
            <div class="line">${escapeHtml(getFieldValue('NAME OF ASSET / ITEM'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DETAIL SPECIFICATION:</div>
            <div class="line">${escapeHtml(getFieldValue('DETAIL SPECIFICATION'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">UTILITY &amp; PURPOSE:</div>
            <div class="line">${escapeHtml(getFieldValue('UTILITY & PURPOSE'))}</div>
          </div>
        </div>

        <div class="checks">
          <div class="label">FEASIBILITY REPORT ATTACHED:</div>
          <div class="check-group">
            <div class="check">Yes <span class="boxcheck ${feasibilityYes ? 'checked' : ''}"></span></div>
            <div class="check">No <span class="boxcheck ${feasibilityNo ? 'checked' : ''}"></span></div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">IF NO THEN MENTION REASON:</div>
            <div class="line xl">${escapeHtml(getFieldValue('IF NO THEN MENTION REASON'))}</div>
          </div>
        </div>


        <div class="note">
          <div class="b">NOTE:</div>
          <ol class="xs tight" type="i">
            <li>In case of technical item, verification of technical expert is mandatory on feasibility / proposal.</li>
            <li>In case of price more than 5 million, third party assessment is mandatory.</li>
            <li>Capital Asset Purchase Checklist (PRC-GEN-FM-29) must be completed along with Capital Asset Purchase Form (PRC-GEN-FM-03).</li>
          </ol>
        </div>

        <div class="subhead">PARTICULARS OF SELECTED VENDOR(S) (AS PER APPROVED QUOTATION)</div>

        <div class="row">
          <div class="field">
            <div class="label">NAME:</div>
            <div class="line">${escapeHtml(getFieldValue('NAME'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">ADDRESS:</div>
            <div class="line">${escapeHtml(getFieldValue('ADDRESS'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">APPROVED PRICE:</div>
            <div class="line">${escapeHtml(getFieldValue('APPROVED PRICE'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DELIVERY PERIOD &amp; DATE:</div>
            <div class="line">${escapeHtml(getFieldValue('DELIVERY PERIOD & DATE'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">TERMS &amp; CONDITIONS:</div>
            <div class="line">${escapeHtml(getFieldValue('TERMS & CONDITIONS'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label paren">(Payment, After Sale Service, Warranty etc.)</div>
            <div class="line xl">${escapeHtml(getFieldValue('TERMS & CONDITIONS'))}</div>
          </div>
        </div>


        <div class="checks">
          <div class="label">Third Party assessment carried out</div>
          <div class="check-group">
            <div class="check">Yes <span class="boxcheck ${thirdPartyYes ? 'checked' : ''}"></span></div>
            <div class="check">No <span class="boxcheck ${thirdPartyNo ? 'checked' : ''}"></span></div>
            <div class="check">NA <span class="boxcheck ${thirdPartyNA ? 'checked' : ''}"></span></div>
          </div>
        </div>

        <div class="sig-row">
          ${signatureSlots.map(slot => `
            <div class="sig">
              <div class="sig-line">${slot.html}</div>
              ${slot.html && slot.time ? '<div class="sig-time">' + escapeHtml(slot.time) + '</div>' : ''}
              ${slot.nameText ? '<div class="sig-meta">' + escapeHtml(slot.nameText) + '</div>' : ''}
              ${slot.designationText ? '<div class="sig-meta">' + escapeHtml(slot.designationText) + '</div>' : ''}
              <div class="sig-label">${escapeHtml(slot.departmentText)}</div>
            </div>
          `).join('')}
        </div>

        <div class="xs mt6"><span class="b">Note:</span> Designation must be mentioned against each signature.</div>

        <div class="approved">
          <div class="who b">Approved By:</div>
          <div class="approved-sig">
            <div class="appline"></div>
            <div class="who b">Chief Executive</div>
          </div>
        </div>

        <!-- PART 2 -->
        <div class="part2-title">PART-2 (TO BE FILLED BY PROCUREMENT DEPARTMENT)</div>

        <div class="row">
          <div class="field">
            <div class="label">P. O. No. WITH DATE:</div>
            <div class="line"></div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">PARTICULARS OF VENDOR(S):</div>
            <div class="line"></div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DELIVERY DATE:</div>
            <div class="line" style="max-width:220px;"></div>
          </div>
        </div>

        <div class="two-col">
          <div class="sign-block">
            <div class="sb-label">Checked By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
          <div class="sign-block" style="align-items:flex-end;">
            <div class="sb-label">Verified By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
        </div>

        <div class="job-title">(JOB COMPLETION CERTIFICATE)</div>

        <div class="job-text">
          THIS IS TO CERTIFY THAT JOB AGAINST CAPF #
          <span class="inline-line">${escapeHtml(getCapfNumber())}</span>
          DATED
          <span class="inline-line short">${escapeHtml(getDate())}</span>
          HAS BEEN COMPLETED.
        </div>

        <div class="job-text">
          GRN #:
          <span class="inline-line short"></span>
          DATED
          <span class="inline-line long"></span>
          (REPORT ATTACHED)
        </div>

        <div class="two-col">
          <div class="sign-block">
            <div class="sb-label">Checked By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
          <div class="sign-block" style="align-items:flex-end;">
            <div class="sb-label">Verified By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>`;

    return html;
  }

}
