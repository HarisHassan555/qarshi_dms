import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PermissionService } from '../../services/shared-data/permission-service';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';

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

  constructor(
    private permissionService: PermissionService,
    private customFormApplicationService: CustomFormApplicationService,
    private customFormService: CustomFormService,
    private notificationService: NotificationService,
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
        console.log('User is not a department head or error loading pending approvals:', error);
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
      (app.txtStatus && app.txtStatus.toLowerCase().includes(searchLower))
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

  selectedApplicationForView: Application | null = null;
  applicationDetails: any = null;
  formFields: any[] = [];
  applicationFormData: any = {};

  viewApplication(application: Application) {
    this.selectedApplicationForView = application;
    this.applicationDetails = null;
    this.formFields = [];
    this.applicationFormData = {};
    
    // Fetch full application details
    if (application.serApplicationId) {
      this.customFormApplicationService.getApplicationById(application.serApplicationId).subscribe(
        (data: any) => {
          if (data) {
            this.applicationDetails = data;
            
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
              } catch (e) {
                console.error('Error parsing application data:', e);
                this.applicationFormData = {};
              }
            }
            
            this.viewModal.open();
          } else {
            this.notificationService.showMessage('Application not found', 'danger');
          }
        },
        (error) => {
          this.notificationService.showMessage('Error loading application details: ' + (error.error?.message || error.message), 'danger');
        }
      );
    }
  }

  getFieldName(label: string): string {
    // Convert label to a valid form control name (same logic as in application component)
    return label.toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  getFieldValue(field: any): any {
    // Get the field value from applicationFormData
    // Field names in the form data are based on the label converted to snake_case
    const fieldName = this.getFieldName(field.label);
    
    // Try multiple possible keys
    if (this.applicationFormData[fieldName] !== undefined) {
      return this.applicationFormData[fieldName];
    } else if (this.applicationFormData[field.label] !== undefined) {
      return this.applicationFormData[field.label];
    } else {
      // Try with field ID
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
      // Value should match one of the options
      return value;
    }
    
    return value;
  }

  canEditApplication(application: Application): boolean {
    // Only allow editing if status is PENDING or REJECTED
    // Don't allow editing if it's APPROVED or IN_PROGRESS (in approval pipeline)
    const status = application.txtStatus?.toUpperCase();
    return status === 'PENDING' || status === 'REJECTED';
  }

  editApplication(application: Application) {
    if (!this.canEditApplication(application)) {
      this.notificationService.showMessage('This application cannot be edited in its current status', 'warning');
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
              const validators = field.required ? [Validators.required] : [];
              
              if (field.type === 'email') {
                validators.push(Validators.email);
              }
              
              // Get existing value
              let existingValue = applicationData[fieldName] || applicationData[field.label] || null;
              
              // Set default value based on field type
              if (existingValue === null || existingValue === undefined) {
                existingValue = field.type === 'checkbox' ? false : '';
              }
              
              formControls[fieldName] = [existingValue, validators];
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
    const formData = this.editForm.value;
    
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
  @ViewChild('viewModal') viewModal: any;
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

  approveApplication() {
    if (!this.selectedApplicationForRemarks || !this.selectedApplicationForRemarks.serApplicationId) {
      this.notificationService.showMessage('Invalid application', 'danger');
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
}

