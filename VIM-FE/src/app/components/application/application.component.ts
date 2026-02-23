import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, FormControl } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { PermissionService } from '../../services/shared-data/permission-service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { NotificationService } from 'src/app/NotificationService';
import { ApplicationPdfService } from 'src/app/services/application-pdf/application-pdf.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BudgetApprovalComponent } from '../budget-approval/budget-approval.component';
import { Store } from '@ngrx/store';

interface FormField {
  serFieldId?: number;
  label: string;
  type: string;
  required: boolean;
  placeholder?: string;
  intFieldOrder?: number;
  txtFieldOptions?: string;
}

interface ApprovalPipeline {
  serApprovalPipelineId?: number;
  serDepartmentId: number;
  intApprovalOrder: number;
  hrTblDepartment?: any;
}

interface CustomForm {
  serFormId?: number;
  name: string;
  txtFormName?: string;
  txtFormCode?: string;
  fields: FormField[];
  cfgTblCustomFormFields?: any[];
  approvalPipelines?: ApprovalPipeline[];
  cfgTblCustomFormApprovalPipelines?: any[];
}

@Component({
  selector: 'app-application',
  templateUrl: './application.component.html',
  styleUrls: ['./application.component.css']
})
export class ApplicationComponent implements OnInit {
  @ViewChild(BudgetApprovalComponent) budgetApprovalCmp?: BudgetApprovalComponent;
  search = '';
  customForms: CustomForm[] = [];
  selectedForm: CustomForm | null = null;
  applicationForm!: FormGroup;
  generatedApplicationCode: string | null = null;
  showBudgetApproval: boolean = false;
  selectedFormId: string = '';
  editData: any = null;
  store: any;
  attachmentFiles: Record<string, File> = {};

  constructor(
    private permissionService: PermissionService,
    private customFormService: CustomFormService,
    private customFormApplicationService: CustomFormApplicationService,
    private applicationPdfService: ApplicationPdfService,
    private fb: FormBuilder,
    private notificationService: NotificationService,
    private router: Router,
    private sanitizer: DomSanitizer,
    public storeData: Store<any>
  ) { }

  ngOnInit() {
    const userJson = localStorage.getItem('user');
    let user: {
      cfgTblRole: number | undefined;
      serUserId: number;
    };

    this.storeData.select((d: any) => d.index).subscribe((d: any) => {
      this.store = d;
    });

    if (userJson) {
      // @ts-ignore
      user = JSON.parse(userJson) as CfgTblUser;
    }
    // @ts-ignore
    this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
      // Component initialized
    });

    this.loadForms();
    this.initializeForm();
    this.checkEditMode();
  }


  checkEditMode() {
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras?.state || history.state;

    if (state && state.editData) {
      console.log('Edit mode detected:', state.editData);
      this.editData = state.editData;
      this.selectedFormId = String(this.editData.serFormId);

      // We need to wait for forms to load to set showBudgetApproval correctly
      // But typically we can just check the name/id if we have it
      // Let's force it if it looks like a budget form
      const name = (this.editData.formName || '').toUpperCase();
      const code = (this.editData.txtFormCode || '').toUpperCase();
      if (name.includes('BUDGET APPROVAL') || code.startsWith('BDG')) {
        this.showBudgetApproval = true;
        this.generatedApplicationCode = this.editData.txtFormCode;
      }
    }
  }

  initializeForm() {
    this.applicationForm = this.fb.group({});
  }

  loadForms() {
    this.customFormService.getAll().subscribe(
      (data: any) => {
        if (data) {
          // Map backend entities to frontend interface
          this.customForms = data.map((form: any) => ({
            serFormId: form.serFormId,
            name: form.txtFormName,
            txtFormName: form.txtFormName,
            txtFormCode: form.txtFormCode,
            fields: (form.cfgTblCustomFormFields || []).map((field: any) => ({
              serFieldId: field.serFieldId,
              label: field.txtFieldLabel,
              type: (field.txtFieldType || '').toString().trim().toLowerCase(),
              required: field.blIsRequired || false,
              placeholder: field.txtPlaceholder || '',
              intFieldOrder: field.intFieldOrder || 0,
              txtFieldOptions: field.txtFieldOptions
            })).sort((a: FormField, b: FormField) => (a.intFieldOrder || 0) - (b.intFieldOrder || 0)),
            approvalPipelines: (form.cfgTblCustomFormApprovalPipelines || []).map((pipeline: any) => ({
              serApprovalPipelineId: pipeline.serApprovalPipelineId,
              serDepartmentId: pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId,
              intApprovalOrder: pipeline.intApprovalOrder || 0,
              hrTblDepartment: pipeline.hrTblDepartment
            })).sort((a: ApprovalPipeline, b: ApprovalPipeline) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0))
          }));
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading forms: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  onFormSelect(event: Event) {
    const formId = Number((event.target as HTMLSelectElement).value);
    this.selectedFormId = (event.target as HTMLSelectElement).value;
    if (formId) {
      this.selectedForm = this.customForms.find(f => f.serFormId === formId) || null;
      this.ensureSidebarHidden(true);
      if (this.selectedForm) {
        const formName = (this.selectedForm.name || '').trim().toLowerCase();
        if (formName === 'budget approval form') {
          this.showBudgetApproval = true;
          this.generateApplicationCode(formId);
        } else {
          this.showBudgetApproval = false;
          this.buildDynamicForm(this.selectedForm);
          // Generate next application code
          this.generateApplicationCode(formId);
        }
      }
    } else {
      this.selectedForm = null;
      this.generatedApplicationCode = null;
      this.showBudgetApproval = false;
      this.ensureSidebarHidden(false);
      this.initializeForm();
    }
  }

  resetBudgetForm() {
    this.selectedFormId = '';
    this.selectedForm = null;
    this.generatedApplicationCode = null;
    this.showBudgetApproval = false;
    this.ensureSidebarHidden(false);
    this.initializeForm();
    this.attachmentFiles = {};
  }

  private ensureSidebarHidden(shouldHide: boolean) {
    const isHidden = !!this.store?.sidebar;
    if (shouldHide && !isHidden) {
      this.storeData.dispatch({ type: 'toggleSidebar' });
    } else if (!shouldHide && isHidden) {
      this.storeData.dispatch({ type: 'toggleSidebar' });
    }
  }

  isCapfSelected(): boolean {
    const name = (this.selectedForm?.name || this.selectedForm?.txtFormName || '').toLowerCase();
    return name.includes('capf');
  }

  isBudgetSelected(): boolean {
    const name = (this.selectedForm?.name || this.selectedForm?.txtFormName || '').toLowerCase();
    return name.includes('budget approval');
  }

  getPreviewFormData(): any {
    const data: any = {};
    if (!this.selectedForm || !this.applicationForm) return data;
    const raw = this.applicationForm.getRawValue();
    this.selectedForm.fields.forEach((field: FormField) => {
      const key = this.getFieldName(field.label);
      const value = raw[key];
      if (value === undefined || value === null) return;
      data[field.label] = value;
      data[key] = value;
    });
    return data;
  }

  getPreviewApplication(): any {
    return {
      dteCreatedDate: new Date(),
      txtFormCode: this.generatedApplicationCode || '',
      cfgTblCustomForm: this.selectedForm ? { txtFormName: this.selectedForm.name || this.selectedForm.txtFormName } : null
    };
  }

  getBudgetPreviewContent(): SafeHtml {
    const html = this.budgetApprovalCmp?.editorContent || '';
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  getBudgetPreviewHeading(): string {
    return this.budgetApprovalCmp?.formHeading || 'Budget Approval Form';
  }

  getBudgetPreviewDate(): string {
    return this.budgetApprovalCmp?.currentDate || new Date().toLocaleDateString();
  }

  getBudgetPreparedBy(): any {
    return this.budgetApprovalCmp?.preparedBy || null;
  }

  getBudgetReviewers(): any[] {
    return this.budgetApprovalCmp?.selectedReviewers || [];
  }

  getBudgetRecommenders(): any[] {
    return this.budgetApprovalCmp?.selectedRecommenders || [];
  }

  getBudgetApprover(): any {
    return this.budgetApprovalCmp?.selectedApprover || null;
  }

  formatUserDisplay(user: any): string {
    if (!user) return '';
    const name = user.txtUserName || user.userName || '';
    const role = user.cfgTblRole?.txtRoleName || user.txtRoleName || user.roleName || '';
    return role ? `${name} (${role})` : name;
  }

  generateApplicationCode(formId: number) {
    this.customFormApplicationService.getNextApplicationCode(formId).subscribe(
      (response: any) => {
        if (response && response.status === 'Success' && response.code) {
          this.generatedApplicationCode = response.code;
        } else {
          this.generatedApplicationCode = null;
          this.notificationService.showMessage('Could not generate application code', 'warning');
        }
      },
      (error) => {
        this.generatedApplicationCode = null;
        this.notificationService.showMessage('Error generating application code: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  buildDynamicForm(form: CustomForm) {
    const formControls: any = {};

    form.fields.forEach((field: FormField) => {
      const fieldName = this.getFieldName(field.label);
      const validators: any[] = [];

      if (field.required) {
        validators.push(Validators.required);
      }

      // Add type-specific validators
      if (field.type === 'email') {
        validators.push(Validators.email);
      }

      // Handle table fields
      if (field.type === 'table') {
        const tableConfig = this.getTableConfig(field);
        const tableFormArray: FormArray = this.fb.array([]);

        // Create form controls for each cell in the table
        for (let row = 0; row < tableConfig.rows; row++) {
          const rowArray = this.fb.array([]);
          for (let col = 0; col < tableConfig.columns; col++) {
            rowArray.push(this.fb.control(''));
          }
          tableFormArray.push(rowArray);
        }

        formControls[fieldName] = tableFormArray;
      } else {
        formControls[fieldName] = [field.type === 'checkbox' ? false : '', validators];
      }
    });

    this.applicationForm = this.fb.group(formControls);
  }

  onAttachmentChange(field: FormField, event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input?.files && input.files.length > 0 ? input.files[0] : null;
    const fieldName = this.getFieldName(field.label);
    if (file) {
      this.attachmentFiles[fieldName] = file;
      this.applicationForm.get(fieldName)?.setValue(file.name);
    } else {
      delete this.attachmentFiles[fieldName];
      this.applicationForm.get(fieldName)?.setValue('');
    }
    this.applicationForm.get(fieldName)?.markAsTouched();
  }

  getFieldName(label: string): string {
    // Convert label to a valid form control name
    return label.toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  getFieldOptions(field: FormField): string[] {
    if (field.txtFieldOptions) {
      try {
        // Try parsing as JSON first
        const parsed = JSON.parse(field.txtFieldOptions);
        if (Array.isArray(parsed)) {
          return parsed;
        }
        // If it's a string, try splitting
        if (typeof parsed === 'string') {
          return parsed.split(',').map(opt => opt.trim()).filter(opt => opt.length > 0);
        }
      } catch (e) {
        // If not JSON, treat as comma-separated values
        if (typeof field.txtFieldOptions === 'string') {
          return field.txtFieldOptions.split(',').map(opt => opt.trim()).filter(opt => opt.length > 0);
        }
      }
    }
    // Return empty array if no options configured
    return [];
  }

  hasFieldOptions(field: FormField): boolean {
    return this.getFieldOptions(field).length > 0;
  }

  getTableConfig(field: FormField): { rows: number; columns: number; rowLabels: string[] } {
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
    return this.applicationForm.get(fieldName) as FormArray;
  }

  getTableRowFormArray(fieldName: string, rowIndex: number): FormArray {
    const tableArray = this.getTableFormArray(fieldName);
    return tableArray.at(rowIndex) as FormArray;
  }

  getTableCellControl(fieldName: string, rowIndex: number, colIndex: number): FormControl {
    return this.getTableRowFormArray(fieldName, rowIndex).at(colIndex) as FormControl;
  }

  getTableRows(field: FormField): number[] {
    const config = this.getTableConfig(field);
    return Array.from({ length: config.rows }, (_, i) => i);
  }

  getTableColumns(field: FormField): number[] {
    const config = this.getTableConfig(field);
    return Array.from({ length: config.columns }, (_, i) => i);
  }

  getTableRowLabel(field: FormField, rowIndex: number): string {
    const config = this.getTableConfig(field);
    if (config.rowLabels && config.rowLabels[rowIndex]) {
      return config.rowLabels[rowIndex];
    }
    return `Row ${rowIndex + 1}`;
  }

  async onSubmit() {
    if (this.applicationForm.valid && this.selectedForm) {
      const formData = { ...this.applicationForm.value };

      // Enforce feasibility attachment if the field exists
      const feasibilityField = this.selectedForm.fields.find(f =>
        (f.label || '').toLowerCase() === 'feasibility_attached_report' ||
        (f.label || '').toLowerCase() === 'feasibility report attached'
      );
      if (feasibilityField && (feasibilityField.type === 'attachment' || feasibilityField.type === 'file')) {
        const feasibilityFieldName = this.getFieldName(feasibilityField.label);
        if (!this.attachmentFiles[feasibilityFieldName]) {
          this.applicationForm.get(feasibilityFieldName)?.setErrors({ required: true });
          this.notificationService.showMessage('Please upload feasibility report attachment.', 'danger');
          return;
        }
      }

      // Convert table FormArrays to regular arrays for JSON serialization
      this.selectedForm.fields.forEach((field: FormField) => {
        if (field.type === 'table') {
          const fieldName = this.getFieldName(field.label);
          const tableArray = this.getTableFormArray(fieldName);
          if (tableArray) {
            formData[fieldName] = tableArray.value;
          }
        }
      });

      // Convert form data to JSON string
      const applicationDataJson = JSON.stringify(formData);

      // Get current user from localStorage
      const userJson = localStorage.getItem('user');
      let userId: number | null = null;
      if (userJson) {
        try {
          const user = JSON.parse(userJson);
          userId = user.serUserId || null;
        } catch (e) {
          console.error('Error parsing user data:', e);
        }
      }

      // Prepare payload for backend
      const payload: any = {
        serFormId: this.selectedForm.serFormId,
        txtFormCode: this.generatedApplicationCode || null,
        txtApplicationData: applicationDataJson,
        txtStatus: 'PENDING',
        intCurrentApprovalLevel: 0,
        serSubmittedBy: userId,
        blIsActive: true,
        blIsDeleted: false,
        blnStatus: true,
        deferEmail: true
      };

      try {
        const response: any = await firstValueFrom(this.customFormApplicationService.submitApplication(payload));
        if (response && response.status === 'Success') {
          const applicationId = Number(response.applicationId);
          if (!applicationId) {
            this.notificationService.showMessage('Application submitted but ID was not returned.', 'warning');
          } else {
            try {
              await this.uploadPdfAndSendEmails(applicationId, formData);
              this.notificationService.showMessage(response.message || 'Application submitted successfully!', 'success');
            } catch (emailError: any) {
              console.error('Failed to upload PDF or send emails:', emailError);
              this.notificationService.showMessage('Application submitted, but approval email could not be sent.', 'warning');
            }
          }

          // Reset form after successful submission
          this.applicationForm.reset();
          this.selectedForm = null;
          this.generatedApplicationCode = null;
          const selectElement = document.querySelector('select') as HTMLSelectElement;
          if (selectElement) {
            selectElement.value = '';
          }
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to submit application', 'danger');
        }
      } catch (error: any) {
        this.notificationService.showMessage('Error submitting application: ' + (error.error?.message || error.message), 'danger');
      }
    } else {
      this.notificationService.showMessage('Please fill all required fields', 'danger');
      // Mark all fields as touched to show validation errors
      Object.keys(this.applicationForm.controls).forEach(key => {
        this.applicationForm.get(key)?.markAsTouched();
      });
    }
  }

  private async uploadPdfAndSendEmails(applicationId: number, formData: any): Promise<void> {
    const applicationResponse: any = await firstValueFrom(
      this.customFormApplicationService.getApplicationById(applicationId)
    );
    const application = applicationResponse || {};

    const form = this.selectedForm;
    const formFields = form?.fields || [];
    const formName = (form?.txtFormName || form?.name || application?.cfgTblCustomForm?.txtFormName || '').trim();
    const formCode = (application?.txtFormCode || this.generatedApplicationCode || form?.txtFormCode || '').trim();

    const htmlContent = this.applicationPdfService.buildPdfHtmlForApplication(
      application,
      form,
      formFields,
      formData,
      { formName, txtFormCode: formCode }
    );
    if (!htmlContent) {
      throw new Error('PDF HTML generation failed');
    }

    const filename = `application_${formCode || applicationId}.pdf`;
    const pdfBlob = await this.applicationPdfService.renderHtmlToPdfBlob(htmlContent, filename);
    const pdfResponse: any = await firstValueFrom(
      this.customFormApplicationService.updateApplicationPdf(applicationId, pdfBlob, filename)
    );
    if (!pdfResponse || pdfResponse.status !== 'Success') {
      throw new Error(pdfResponse?.message || 'Failed to update application PDF');
    }

    const emailResponse: any = await firstValueFrom(
      this.customFormApplicationService.sendSubmissionEmails(applicationId)
    );
    if (!emailResponse || emailResponse.status !== 'Success') {
      throw new Error(emailResponse?.message || 'Failed to send submission emails');
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.applicationForm.get(fieldName);
    return !!(control && control.invalid && control.touched);
  }

  getFieldErrorMessage(fieldName: string): string {
    const control = this.applicationForm.get(fieldName);
    if (control?.errors) {
      if (control.errors['required']) {
        return 'This field is required';
      }
      if (control.errors['email']) {
        return 'Please enter a valid email address';
      }
    }
    return '';
  }
}
