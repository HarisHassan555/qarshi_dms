import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { PermissionService } from '../../services/shared-data/permission-service';
import { NotificationService } from 'src/app/NotificationService';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { DepartmentService } from '../../services/department/department.service';

interface FormField {
  id?: string;
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
  id?: string;
  serFormId?: number;
  name: string;
  txtFormName?: string;
  txtFormCode?: string;
  txtConventionPrefix?: string;
  fields: FormField[];
  cfgTblCustomFormFields?: any[];
  approvalPipelines?: ApprovalPipeline[];
  cfgTblCustomFormApprovalPipelines?: any[];
  createdAt?: Date;
  dteCreatedDate?: Date;
}

@Component({
  selector: 'app-form-builder',
  templateUrl: './form-builder.component.html',
  styleUrls: ['./form-builder.component.css']
})
export class FormBuilderComponent implements OnInit {
  @ViewChild('modal') modal: any;
  
  search = '';
  customForms: CustomForm[] = [];
  formBuilderForm!: FormGroup;
  isSubmit = false;
  editingFormId: number | null = null;
  fieldTypes = [
    {value : 'document_header', label: 'Document Header'},
    { value: 'text', label: 'Text' },
    { value: 'number', label: 'Number' },
    { value: 'email', label: 'Email' },
    { value: 'date', label: 'Date' },
    { value: 'textarea', label: 'Textarea' },
    {value: 'word_editor', label: 'Word Editor'},
    { value: 'attachment', label: 'Attachment' },
    { value: 'select', label: 'Select' },
    { value: 'checkbox', label: 'Checkbox' },
    { value: 'radio', label: 'Radio' },
    { value: 'table', label: 'Table' },
    { value: 'footer', label: 'Footer (Approval Pipeline)' }
  ];

  cols = [
    { field: 'name', title: 'Form Name' },
    { field: 'txtFormCode', title: 'Form Code' },
    { field: 'fieldCount', title: 'Fields' },
    { field: 'createdAt', title: 'Created Date' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  departments: any[] = [];
  approvalPipelines: ApprovalPipeline[] = [];
  showApprovalPipeline = false;

  constructor(
    private fb: FormBuilder,
    private permissionService: PermissionService,
    private notificationService: NotificationService,
    private customFormService: CustomFormService,
    private departmentService: DepartmentService
  ) { }

  ngOnInit() {
    this.initializeForm();
    this.loadForms();
    this.loadDepartments();
    
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
  }

  loadDepartments() {
    this.departmentService.getAll().subscribe(
      (data: any) => {
        if (data) {
          this.departments = data.filter((dept: any) => 
            dept.blIsDeleted === false && dept.blnStatus === true
          );
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading departments: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  addApprovalPipeline() {
    const selectedDepartmentId = (document.getElementById('departmentSelect') as HTMLSelectElement)?.value;
    if (!selectedDepartmentId || selectedDepartmentId === '0') {
      this.notificationService.showMessage('Please select a department', 'danger');
      return;
    }
    const departmentId = Number(selectedDepartmentId);

    if (this.approvalPipelines.some(p => p.serDepartmentId === departmentId)) {
      this.notificationService.showMessage('Department already added to the pipeline', 'warning');
      return;
    }

    const department = this.departments.find(d => d.serDepartmentId === departmentId);
    if (department) {
      this.approvalPipelines.push({
        serDepartmentId: department.serDepartmentId,
        intApprovalOrder: this.approvalPipelines.length + 1,
        hrTblDepartment: department
      });
      // Reset the select
      (document.getElementById('departmentSelect') as HTMLSelectElement).value = '0';
    }
  }

  removeApprovalPipeline(index: number) {
    this.approvalPipelines.splice(index, 1);
    this.approvalPipelines.forEach((p, i) => p.intApprovalOrder = i + 1); // Reorder
  }

  moveApprovalPipeline(index: number, direction: 'up' | 'down') {
    if (direction === 'up' && index > 0) {
      [this.approvalPipelines[index - 1], this.approvalPipelines[index]] = [this.approvalPipelines[index], this.approvalPipelines[index - 1]];
    } else if (direction === 'down' && index < this.approvalPipelines.length - 1) {
      [this.approvalPipelines[index + 1], this.approvalPipelines[index]] = [this.approvalPipelines[index], this.approvalPipelines[index + 1]];
    }
    this.approvalPipelines.forEach((p, i) => p.intApprovalOrder = i + 1); // Reorder
  }

  initializeForm() {
    this.formBuilderForm = this.fb.group({
      formName: ['', Validators.required],
      convention: [''], // Convention prefix (e.g., "CAPF", "PRC")
      fields: this.fb.array([])
    });
  }

  get fields(): FormArray {
    return this.formBuilderForm.get('fields') as FormArray;
  }

  addField() {
    const fieldForm = this.fb.group({
      label: ['', Validators.required],
      type: ['text', Validators.required],
      required: [false],
      placeholder: [''],
      options: [''], // For select/radio fields
      tableRows: [2], // For table fields
      tableColumns: [2], // For table fields
      tableRowLabels: [''] // For table fields - comma-separated row labels
    });
    this.fields.push(fieldForm);
  }

  removeField(index: number) {
    this.fields.removeAt(index);
  }

  onTypeChange(index: number) {
    const field = this.fields.at(index);
    const type = field.get('type')?.value;
    if (type === 'word_editor' || type === 'footer') {
      field.get('label')?.setValue(type === 'word_editor' ? 'Word Editor' : 'Form Footer');
      field.get('label')?.clearValidators();
      field.get('label')?.updateValueAndValidity();
    } else {
      field.get('label')?.setValidators([Validators.required]);
      field.get('label')?.updateValueAndValidity();
    }
  }

  add() {
    this.isSubmit = false;
    this.editingFormId = null;
    this.approvalPipelines = [];
    this.showApprovalPipeline = false;
    this.initializeForm();
    this.addField(); // Add one field by default
    this.modal.open();
  }

  submit() {
    this.isSubmit = true;
    if (this.formBuilderForm.invalid) {
      this.notificationService.showMessage('Please fill all required fields', 'danger');
      return;
    }

    if (this.fields.length === 0) {
      this.notificationService.showMessage('Please add at least one field to the form', 'danger');
      return;
    }

    const formData = this.formBuilderForm.value;
    
    // Extract convention prefix from input (e.g., "CAPF-0000" -> "CAPF")
    let conventionPrefix = '';
    if (formData.convention && formData.convention.trim()) {
      const conventionInput = formData.convention.trim().toUpperCase();
      // Check if input contains a dash (e.g., "CAPF-0000")
      if (conventionInput.includes('-')) {
        conventionPrefix = conventionInput.split('-')[0];
      } else {
        // Just the prefix (e.g., "CAPF")
        conventionPrefix = conventionInput;
      }
    }
    
    // Map to backend entity structure
    const payload: any = {
      txtFormName: formData.formName,
      txtFormDescription: '',
      txtConventionPrefix: conventionPrefix || null,
      blIsActive: true,
      blIsDeleted: false,
      blnStatus: true,
      cfgTblCustomFormFields: formData.fields.map((field: any, index: number) => {
        let fieldOptions = null;
        
        // Handle select/radio options
        if ((field.type === 'select' || field.type === 'radio') && field.options && field.options.trim()) {
          fieldOptions = field.options.trim();
        }
        // Handle table configuration
        else if (field.type === 'table') {
          const tableConfig = {
            rows: field.tableRows || 2,
            columns: field.tableColumns || 2,
            rowLabels: field.tableRowLabels ? field.tableRowLabels.split(',').map((label: string) => label.trim()).filter((label: string) => label.length > 0) : []
          };
          fieldOptions = JSON.stringify(tableConfig);
        }
        
        return {
          txtFieldLabel: field.label,
          txtFieldType: field.type,
          txtPlaceholder: field.placeholder || '',
          blIsRequired: field.required || false,
          intFieldOrder: index,
          blIsActive: true,
          blIsDeleted: false,
          txtFieldOptions: fieldOptions
        };
      }),
      cfgTblCustomFormApprovalPipelines: this.approvalPipelines.filter(p => p.serDepartmentId > 0).map((pipeline, index) => ({
        serDepartmentId: pipeline.serDepartmentId,
        intApprovalOrder: index + 1,
        hrTblDepartment: this.departments.find(d => d.serDepartmentId === pipeline.serDepartmentId)
      }))
    };

    // If editing, include the form ID
    if (this.editingFormId) {
      payload.serFormId = this.editingFormId;
    }

    this.customFormService.save(payload).subscribe(
      (response: any) => {
        if (response && response.status === 'Success') {
          this.notificationService.showMessage(response.message || 'Form saved successfully', 'success');
          this.modal.close();
          this.initializeForm();
          this.loadForms(); // Reload from backend
        } else {
          this.notificationService.showMessage(response?.message || 'Failed to save form', 'danger');
        }
      },
      (error) => {
        this.notificationService.showMessage('Error saving form: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  generateId(): string {
    return 'form_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
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
            txtConventionPrefix: form.txtConventionPrefix,
            fields: (form.cfgTblCustomFormFields || []).map((field: any) => ({
              serFieldId: field.serFieldId,
              label: field.txtFieldLabel,
              type: field.txtFieldType,
              required: field.blIsRequired || false,
              placeholder: field.txtPlaceholder || '',
              intFieldOrder: field.intFieldOrder || 0,
              txtFieldOptions: field.txtFieldOptions
            })),
            approvalPipelines: ((form.approvalPipelines || form.cfgTblCustomFormApprovalPipelines) || []).map((pipeline: any) => ({
              serApprovalPipelineId: pipeline.serApprovalPipelineId,
              serDepartmentId: pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId,
              intApprovalOrder: pipeline.intApprovalOrder || 0,
              hrTblDepartment: pipeline.hrTblDepartment
            })).sort((a: ApprovalPipeline, b: ApprovalPipeline) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0)),
            createdAt: form.dteCreatedDate ? new Date(form.dteCreatedDate) : new Date(),
            dteCreatedDate: form.dteCreatedDate
          }));
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading forms: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  deleteForm(form: CustomForm) {
    if (confirm('Are you sure you want to delete this form?')) {
      const formId = form.serFormId;
      if (!formId) {
        this.notificationService.showMessage('Invalid form ID', 'danger');
        return;
      }
      
      this.customFormService.delete(formId).subscribe(
        (response: any) => {
          if (response && response.status === 'Success') {
            this.notificationService.showMessage(response.message || 'Form deleted successfully', 'success');
            this.loadForms(); // Reload from backend
          } else {
            this.notificationService.showMessage(response?.message || 'Failed to delete form', 'danger');
          }
        },
        (error) => {
          this.notificationService.showMessage('Error deleting form: ' + (error.error?.message || error.message), 'danger');
        }
      );
    }
  }

  editForm(form: CustomForm) {
    this.isSubmit = false;
    this.editingFormId = form.serFormId || null;
    
    this.formBuilderForm.patchValue({
      formName: form.name || form.txtFormName,
      convention: form.txtConventionPrefix || ''
    });

    // Clear existing fields
    while (this.fields.length !== 0) {
      this.fields.removeAt(0);
    }

    // Add fields from the form
    form.fields.forEach(field => {
      let tableRows = 2;
      let tableColumns = 2;
      let tableRowLabels = '';
      let options = '';
      
      // Parse table configuration if it's a table field
      if (field.type === 'table' && field.txtFieldOptions) {
        try {
          const tableConfig = JSON.parse(field.txtFieldOptions);
          tableRows = tableConfig.rows || 2;
          tableColumns = tableConfig.columns || 2;
          tableRowLabels = Array.isArray(tableConfig.rowLabels) ? tableConfig.rowLabels.join(', ') : '';
        } catch (e) {
          // If parsing fails, use defaults
        }
      } else if (field.txtFieldOptions) {
        options = field.txtFieldOptions;
      }
      
      const fieldForm = this.fb.group({
        label: [field.label, (field.type === 'word_editor' || field.type === 'footer') ? [] : [Validators.required]],
        type: [field.type, Validators.required],
        required: [field.required || false],
        placeholder: [field.placeholder || ''],
        options: [options],
        tableRows: [tableRows],
        tableColumns: [tableColumns],
        tableRowLabels: [tableRowLabels]
      });
      this.fields.push(fieldForm);
    });

    // Load approval pipelines
    this.approvalPipelines = [];
    if (form.approvalPipelines && form.approvalPipelines.length > 0) {
      this.approvalPipelines = form.approvalPipelines.map(p => ({
        serApprovalPipelineId: p.serApprovalPipelineId,
        serDepartmentId: p.serDepartmentId,
        intApprovalOrder: p.intApprovalOrder,
        hrTblDepartment: p.hrTblDepartment || this.departments.find(d => d.serDepartmentId === p.serDepartmentId)
      }));
      // Enable the approval pipeline toggle if pipelines exist
      this.showApprovalPipeline = true;
    } else {
      // Disable if no pipelines
      this.showApprovalPipeline = false;
    }

    this.modal.open();
  }

  getFieldCount(form: CustomForm): number {
    return form.fields.length;
  }

  getDisplayedForms() {
    if (!this.search) {
      return this.customForms;
    }
    return this.customForms.filter(form => 
      form.name.toLowerCase().includes(this.search.toLowerCase())
    );
  }
}
