import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PermissionService } from '../../services/shared-data/permission-service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { NotificationService } from 'src/app/NotificationService';

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
  search = '';
  customForms: CustomForm[] = [];
  selectedForm: CustomForm | null = null;
  applicationForm!: FormGroup;
  generatedApplicationCode: string | null = null;
  
  constructor(
    private permissionService: PermissionService,
    private customFormService: CustomFormService,
    private customFormApplicationService: CustomFormApplicationService,
    private fb: FormBuilder,
    private notificationService: NotificationService
  ) { }

  ngOnInit() {
    const userJson = localStorage.getItem('user');
    let user: {
      cfgTblRole: number | undefined;
      serUserId: number;
    };

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
              type: field.txtFieldType,
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
    if (formId) {
      this.selectedForm = this.customForms.find(f => f.serFormId === formId) || null;
      if (this.selectedForm) {
        this.buildDynamicForm(this.selectedForm);
        // Generate next application code
        this.generateApplicationCode(formId);
      }
    } else {
      this.selectedForm = null;
      this.generatedApplicationCode = null;
      this.initializeForm();
    }
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
      
      formControls[fieldName] = [field.type === 'checkbox' ? false : '', validators];
    });
    
    this.applicationForm = this.fb.group(formControls);
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

  onSubmit() {
    if (this.applicationForm.valid && this.selectedForm) {
      const formData = this.applicationForm.value;
      
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
        blnStatus: true
      };
      
      // Submit to backend
      this.customFormApplicationService.submitApplication(payload).subscribe(
        (response: any) => {
          if (response && response.status === 'Success') {
            this.notificationService.showMessage(response.message || 'Application submitted successfully!', 'success');
            // Reset form after successful submission
            this.applicationForm.reset();
            this.selectedForm = null;
            this.generatedApplicationCode = null;
            // Reset form selection dropdown
            const selectElement = document.querySelector('select') as HTMLSelectElement;
            if (selectElement) {
              selectElement.value = '';
            }
          } else {
            this.notificationService.showMessage(response?.message || 'Failed to submit application', 'danger');
          }
        },
        (error) => {
          this.notificationService.showMessage('Error submitting application: ' + (error.error?.message || error.message), 'danger');
        }
      );
    } else {
      this.notificationService.showMessage('Please fill all required fields', 'danger');
      // Mark all fields as touched to show validation errors
      Object.keys(this.applicationForm.controls).forEach(key => {
        this.applicationForm.get(key)?.markAsTouched();
      });
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

