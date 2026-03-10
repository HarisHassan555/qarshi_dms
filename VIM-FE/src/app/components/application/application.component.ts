import { AfterViewChecked, Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, FormArray, Validators, FormControl, ValidationErrors } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { PermissionService } from '../../services/shared-data/permission-service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { NotificationService } from 'src/app/NotificationService';
import { ApplicationPdfService } from 'src/app/services/application-pdf/application-pdf.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BudgetApprovalComponent } from '../budget-approval/budget-approval.component';
import { UserService } from '../../services/user/user.service';
import { Store } from '@ngrx/store';
import * as QuillNamespace from 'quill';

const Quill: any = QuillNamespace;
const ExistingTableBlot = Quill.imports?.['formats/table-blot'];
if (!ExistingTableBlot) {
  const BlockEmbed = Quill.import('blots/block/embed');
  class TableBlot extends BlockEmbed {
    static create(value: string) {
      const node = super.create();
      node.innerHTML = value;
      node.setAttribute('contenteditable', 'false');
      return node;
    }
    static value(node: HTMLElement) {
      return node.innerHTML;
    }
  }
  TableBlot['blotName'] = 'table-blot';
  TableBlot['tagName'] = 'div';
  TableBlot['className'] = 'q-table-wrapper';
  Quill.register(TableBlot);
}
const QuillIcons = Quill.import('ui/icons');
QuillIcons['tableInsert'] = '<svg viewBox="0 0 18 18"><rect class="ql-stroke" height="12" width="12" x="3" y="3"></rect><line class="ql-stroke" x1="3" x2="15" y1="7" y2="7"></line><line class="ql-stroke" x1="3" x2="15" y1="11" y2="11"></line><line class="ql-stroke" x1="7" x2="7" y1="3" y2="15"></line><line class="ql-stroke" x1="11" x2="11" y1="3" y2="15"></line></svg>';
QuillIcons['mergeRight'] = '<svg viewBox="0 0 18 18"><rect class="ql-stroke" x="3" y="4" width="5" height="10"></rect><rect class="ql-stroke" x="10" y="4" width="5" height="10"></rect><line class="ql-stroke" x1="8.5" y1="9" x2="10" y2="9"></line><polyline class="ql-stroke" points="11,7 13,9 11,11"></polyline></svg>';
QuillIcons['mergeDown'] = '<svg viewBox="0 0 18 18"><rect class="ql-stroke" x="4" y="3" width="10" height="5"></rect><rect class="ql-stroke" x="4" y="10" width="10" height="5"></rect><line class="ql-stroke" x1="9" y1="8.5" x2="9" y2="10"></line><polyline class="ql-stroke" points="7,11 9,13 11,11"></polyline></svg>';

interface FormField {
  serFieldId?: number;
  label: string;
  type: string;
  required: boolean;
  placeholder?: string;
  intFieldOrder?: number;
  txtFieldOptions?: string;
}

interface IndividualPipelineFooterField {
  key: string;
  label: string;
  order: number;
  users: any[];
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
export class ApplicationComponent implements OnInit, AfterViewChecked, OnDestroy {
  @ViewChild(BudgetApprovalComponent) budgetApprovalCmp?: BudgetApprovalComponent;
  @ViewChild('previewCanvas') previewCanvas?: ElementRef<HTMLElement>;
  @ViewChild('previewScale') previewScale?: ElementRef<HTMLElement>;
  search = '';
  customForms: CustomForm[] = [];
  selectedForm: CustomForm | null = null;
  applicationForm!: FormGroup;
  generatedApplicationCode: string | null = null;
  showBudgetApproval: boolean = false;
  selectedFormId: string = '';
  editData: any = null;
  documentHeaderField: FormField | null = null;
  allUsers: any[] = [];
  store: any;
  attachmentFiles: Record<string, File> = {};
  attachmentPayloads: Record<string, { fileName: string; mimeType: string; dataUrl: string; base64: string }> = {};
  multiAttachmentFiles: Record<string, File[]> = {};
  multiAttachmentPayloads: Record<string, { fileName: string; mimeType: string; dataUrl: string; base64: string }[]> = {};
  previewDate = new Date().toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).replace(/ /g, '-');
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
  private previewFitPending = false;
  private previewFitFrame: number | null = null;
  private lastFocusedTableCellByEditor = new WeakMap<any, HTMLTableCellElement>();
  private async buildAttachmentPayload(file: File): Promise<{ fileName: string; mimeType: string; dataUrl: string; base64: string }> {
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

  constructor(
    private permissionService: PermissionService,
    private customFormService: CustomFormService,
    private customFormApplicationService: CustomFormApplicationService,
    private applicationPdfService: ApplicationPdfService,
    private fb: FormBuilder,
    private notificationService: NotificationService,
    private router: Router,
    private sanitizer: DomSanitizer,
    private userService: UserService,
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
    this.loadUsers();
    this.checkEditMode();
  }

  ngAfterViewChecked(): void {
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

  loadUsers() {
    this.userService.getUsers().subscribe(
      (data: any) => {
        if (data) {
          this.allUsers = data;
        }
      },
      (error) => {
        console.error('Error loading users:', error);
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
      this.documentHeaderField = null;
      this.ensureSidebarHidden(false);
      this.initializeForm();
    }
    this.requestPreviewFit();
  }

  resetBudgetForm() {
    this.selectedFormId = '';
    this.selectedForm = null;
    this.generatedApplicationCode = null;
    this.showBudgetApproval = false;
    this.documentHeaderField = null;
    this.ensureSidebarHidden(false);
    this.initializeForm();
    this.attachmentFiles = {};
    this.requestPreviewFit();
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

    const availableWidth = Math.max(canvasEl.clientWidth, 0);
    const naturalWidth = Math.max(previewContentEl.scrollWidth || previewContentEl.offsetWidth, 0);
    const naturalHeight = Math.max(previewContentEl.scrollHeight || previewContentEl.offsetHeight, 0);

    if (!availableWidth || !naturalWidth || !naturalHeight) {
      return;
    }

    const scale = availableWidth / naturalWidth;
    const safeScale = Number.isFinite(scale) ? Math.max(scale, 0.1) : 1;

    scaleHostEl.style.width = `${availableWidth}px`;
    scaleHostEl.style.height = `${naturalHeight * safeScale}px`;
    previewContentEl.style.transform = `scale(${safeScale})`;
  }

  private getPreviewContentElement(scaleHostEl: HTMLElement): HTMLElement | null {
    const selectors = ['.xyz-paper', '.abc-wrapper.embedded .page', '.abc-wrapper .page'];
    for (const selector of selectors) {
      const match = scaleHostEl.querySelector(selector);
      if (match instanceof HTMLElement) {
        return match;
      }
    }
    return null;
  }

  private ensureSidebarHidden(shouldHide: boolean) {
    const isHidden = !!this.store?.sidebar;
    const shouldToggle = (shouldHide && !isHidden) || (!shouldHide && isHidden);
    if (shouldToggle) {
      setTimeout(() => {
        this.storeData.dispatch({ type: 'toggleSidebar' });
      }, 0);
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
    this.documentHeaderField = null;

    form.fields.forEach((field: FormField) => {
      const fieldName = this.getFieldName(field.label);
      const validators: any[] = [];
      const normalizedFieldType = (field.type || '').toString().trim().toLowerCase();

      if (field.required && !this.isIndividualPipelineFooterType(field.type)) {
        if (this.isWordEditorType(field.type)) {
          validators.push(this.richTextRequiredValidator);
        } else {
          validators.push(Validators.required);
        }
      }

      // Add type-specific validators
      if (normalizedFieldType === 'email') {
        validators.push(Validators.email);
      }

      // Handle table fields
      if (normalizedFieldType === 'table') {
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
      } else if (normalizedFieldType === 'individual_pipeline_footer') {
        formControls[fieldName] = [this.getInitialIndividualFooterSections(field)];
      } else if (normalizedFieldType === 'multi_attachment') {
        formControls[fieldName] = [[], validators];
      } else {
        formControls[fieldName] = [normalizedFieldType === 'checkbox' ? false : '', validators];
      }

      if (normalizedFieldType === 'document_header' && !this.documentHeaderField) {
        this.documentHeaderField = field;
      }
    });

    this.applicationForm = this.fb.group(formControls);
  }

  isDocumentHeaderType(fieldType: string | undefined): boolean {
    return (fieldType || '').toString().trim().toLowerCase() === 'document_header';
  }

  getDocumentHeaderControl(): FormControl | null {
    if (!this.documentHeaderField || !this.applicationForm) {
      return null;
    }
    const controlName = this.getFieldName(this.documentHeaderField.label);
    const control = this.applicationForm.get(controlName);
    return control instanceof FormControl ? control : null;
  }

  onAttachmentChange(field: FormField, event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input?.files && input.files.length > 0 ? input.files[0] : null;
    const fieldName = this.getFieldName(field.label);
    if (file) {
      this.attachmentFiles[fieldName] = file;
      this.applicationForm.get(fieldName)?.setValue(file.name);
      this.buildAttachmentPayload(file).then((payload) => {
        this.attachmentPayloads[fieldName] = payload;
      }).catch(() => {
        // ignore read errors here; will be handled on submit
      });
    } else {
      delete this.attachmentFiles[fieldName];
      delete this.attachmentPayloads[fieldName];
      this.applicationForm.get(fieldName)?.setValue('');
    }
    this.applicationForm.get(fieldName)?.markAsTouched();
  }

  async onMultiAttachmentChange(field: FormField, event: Event) {
    const input = event.target as HTMLInputElement;
    const files = input?.files ? Array.from(input.files) : [];
    const fieldName = this.getFieldName(field.label);
    
    if (files.length > 0) {
      this.multiAttachmentFiles[fieldName] = files;
      
      const fileNames = files.map(f => f.name);
      this.applicationForm.get(fieldName)?.setValue(fileNames);
      
      try {
        const payloads = await Promise.all(files.map(f => this.buildAttachmentPayload(f)));
        this.multiAttachmentPayloads[fieldName] = payloads;
      } catch (err) {
        console.error('Error generating payloads', err);
      }
    } else {
      delete this.multiAttachmentFiles[fieldName];
      delete this.multiAttachmentPayloads[fieldName];
      this.applicationForm.get(fieldName)?.setValue([]);
    }
    this.applicationForm.get(fieldName)?.markAsTouched();
  }

  removeMultiAttachment(field: FormField, indexToRemove: number) {
    const fieldName = this.getFieldName(field.label);
    
    // Remove from the files array
    if (this.multiAttachmentFiles[fieldName] && this.multiAttachmentFiles[fieldName].length > indexToRemove) {
      this.multiAttachmentFiles[fieldName].splice(indexToRemove, 1);
      if (this.multiAttachmentFiles[fieldName].length === 0) {
        delete this.multiAttachmentFiles[fieldName];
      }
    }
    
    // Remove from the payloads array
    if (this.multiAttachmentPayloads[fieldName] && this.multiAttachmentPayloads[fieldName].length > indexToRemove) {
      this.multiAttachmentPayloads[fieldName].splice(indexToRemove, 1);
      if (this.multiAttachmentPayloads[fieldName].length === 0) {
        delete this.multiAttachmentPayloads[fieldName];
      }
    }
    
    // Update the form control value
    const currentValues = this.applicationForm.get(fieldName)?.value || [];
    if (Array.isArray(currentValues) && currentValues.length > indexToRemove) {
      currentValues.splice(indexToRemove, 1);
      this.applicationForm.get(fieldName)?.setValue([...currentValues]);
    }
    
    this.applicationForm.get(fieldName)?.markAsTouched();
  }

  getFieldName(label: string): string {
    // Convert label to a valid form control name
    return label.toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
  }

  isWordEditorType(fieldType: string | undefined): boolean {
    const normalizedType = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
    return normalizedType === 'word_editor' || normalizedType === 'wordeditor' || normalizedType === 'rich_text' || normalizedType === 'richtext';
  }

  onWordEditorCreated(fieldName: string, editor: any): void {
    editor?.root?.addEventListener('focusin', (event: FocusEvent) => {
      const target = event.target as HTMLElement | null;
      const cell = target?.closest('td') as HTMLTableCellElement | null;
      if (cell && editor?.root?.contains(cell)) {
        this.lastFocusedTableCellByEditor.set(editor, cell);
      }
    });

    const toolbarModule = editor?.getModule?.('toolbar');
    if (toolbarModule) {
      const toolbarEl = toolbarModule.container as HTMLElement | undefined;
      if (toolbarEl) {
        this.ensureToolbarActionButton(toolbarEl, 'tableInsert', () => this.promptAndInsertTable(editor));
        this.ensureToolbarActionButton(toolbarEl, 'mergeRight', () => this.mergeTableCellRight(editor));
        this.ensureToolbarActionButton(toolbarEl, 'mergeDown', () => this.mergeTableCellDown(editor));
      }
    }
  }

  private ensureToolbarActionButton(toolbarEl: HTMLElement, classSuffix: string, onClick: () => void): void {
    const selector = `.ql-${classSuffix}`;
    let btn = toolbarEl.querySelector(selector) as HTMLButtonElement | null;
    if (!btn) {
      const groups = toolbarEl.querySelectorAll('.ql-formats');
      const targetGroup = (groups[groups.length - 1] as HTMLElement) || toolbarEl;
      const createdBtn = document.createElement('button');
      createdBtn.type = 'button';
      createdBtn.className = `ql-${classSuffix}`;
      createdBtn.innerHTML = QuillIcons[classSuffix] || '';
      targetGroup.appendChild(createdBtn);
      btn = createdBtn;
    }
    const boundKey = `${classSuffix}Bound`;
    if (btn.dataset[boundKey] !== '1') {
      btn.dataset[boundKey] = '1';
      btn.addEventListener('click', (event) => {
        event.preventDefault();
        event.stopPropagation();
        onClick();
      });
    }
  }

  promptAndInsertTable(editor: any): void {
    const sizeInput = window.prompt('Enter table size as rows x columns (e.g., 3x4):', '2x2');
    if (!sizeInput) {
      return;
    }
    const match = sizeInput.trim().toLowerCase().match(/^(\d+)\s*[x,]\s*(\d+)$/);
    if (!match) {
      this.notificationService.showMessage('Invalid table size. Use format like 3x4.', 'warning');
      return;
    }
    const rows = Math.max(1, Math.min(20, Number(match[1])));
    const columns = Math.max(1, Math.min(10, Number(match[2])));

    let tableHtml = '<table style="width:100%; border-collapse:collapse; border:1px solid #000; margin:10px 0; table-layout:fixed;">';
    for (let r = 0; r < rows; r++) {
      tableHtml += '<tr>';
      for (let c = 0; c < columns; c++) {
        const defaultValue = r === 0 ? `Header ${c + 1}` : '';
        tableHtml += `<td style="border:1px solid #000; padding:1px; vertical-align:top;">
          <textarea
            rows="1"
            oninput="this.style.height='auto';this.style.height=this.scrollHeight+'px';this.textContent=this.value"
            style="width:100%; border:none; outline:none; background:transparent; font:inherit; padding:1px; line-height:1.2; resize:none; overflow:hidden; white-space:pre-wrap; word-break:break-word; box-sizing:border-box;">${defaultValue}</textarea>
        </td>`;
      }
      tableHtml += '</tr>';
    }
    tableHtml += '</table>';

    const range = editor.getSelection(true);
    const index = range ? range.index : editor.getLength();
    editor.insertEmbed(index, 'table-blot', tableHtml, 'user');
    editor.setSelection(index + 1, 0, 'api');
  }

  private getFocusedTableCell(editor: any): HTMLTableCellElement | null {
    const activeElement = document.activeElement as HTMLElement | null;
    const activeCell = activeElement?.closest('td') as HTMLTableCellElement | null;
    if (activeCell && editor?.root?.contains(activeCell)) {
      this.lastFocusedTableCellByEditor.set(editor, activeCell);
      return activeCell;
    }

    const rememberedCell = this.lastFocusedTableCellByEditor.get(editor) || null;
    if (rememberedCell && editor?.root?.contains(rememberedCell)) {
      return rememberedCell;
    }

    if (!activeElement || !activeCell || !editor?.root?.contains(activeCell)) {
      this.notificationService.showMessage('Place cursor inside a table cell first.', 'warning');
      return null;
    }
    return activeCell;
  }

  private normalizeInputCell(cell: HTMLTableCellElement, fallback: string): HTMLTextAreaElement {
    let input = cell.querySelector('textarea') as HTMLTextAreaElement | null;
    if (!input) {
      const legacyInput = cell.querySelector('input') as HTMLInputElement | null;
      const initialValue = legacyInput ? legacyInput.value : fallback;
      input = document.createElement('textarea');
      input.rows = 1;
      input.style.width = '100%';
      input.style.border = 'none';
      input.style.outline = 'none';
      input.style.background = 'transparent';
      input.style.font = 'inherit';
      input.style.padding = '1px';
      input.style.lineHeight = '1.2';
      input.style.resize = 'none';
      input.style.overflow = 'hidden';
      input.style.whiteSpace = 'pre-wrap';
      input.style.wordBreak = 'break-word';
      input.style.boxSizing = 'border-box';
      input.value = initialValue;
      input.textContent = initialValue;
      input.setAttribute('oninput', "this.style.height='auto';this.style.height=this.scrollHeight+'px';this.textContent=this.value");
      cell.innerHTML = '';
      cell.appendChild(input);
    }
    return input;
  }

  private mergeCellValues(primary: HTMLTextAreaElement, secondary: HTMLTextAreaElement | null): void {
    const primaryVal = (primary.value || '').trim();
    const secondaryVal = (secondary?.value || '').trim();
    if (!primaryVal && secondaryVal) {
      primary.value = secondaryVal;
      primary.textContent = secondaryVal;
      primary.style.height = 'auto';
      primary.style.height = `${primary.scrollHeight}px`;
    }
  }

  mergeTableCellRight(editor: any): void {
    const cell = this.getFocusedTableCell(editor);
    if (!cell) return;

    const nextCell = cell.nextElementSibling as HTMLTableCellElement | null;
    if (!nextCell) {
      this.notificationService.showMessage('No cell available on the right to merge.', 'warning');
      return;
    }

    const currentColspan = Number(cell.getAttribute('colspan') || '1');
    const nextColspan = Number(nextCell.getAttribute('colspan') || '1');
    cell.setAttribute('colspan', String(currentColspan + nextColspan));

    const primaryInput = this.normalizeInputCell(cell, '');
    const secondaryInput = (nextCell.querySelector('textarea') || nextCell.querySelector('input')) as HTMLTextAreaElement | null;
    this.mergeCellValues(primaryInput, secondaryInput);

    nextCell.remove();
    this.normalizeMergedTable(cell.closest('table'));
    primaryInput.focus();
  }

  private getColumnStartIndex(cell: HTMLTableCellElement): number {
    let index = 0;
    let pointer: Element | null = cell.parentElement?.firstElementChild || null;
    while (pointer && pointer !== cell) {
      if (pointer instanceof HTMLTableCellElement) {
        index += Number(pointer.getAttribute('colspan') || '1');
      }
      pointer = pointer.nextElementSibling;
    }
    return index;
  }

  private getCoveringCell(row: HTMLTableRowElement, columnStart: number): HTMLTableCellElement | null {
    let cursor = 0;
    for (const candidate of Array.from(row.cells)) {
      const span = Number(candidate.getAttribute('colspan') || '1');
      const start = cursor;
      const end = cursor + span - 1;
      if (columnStart >= start && columnStart <= end) {
        return candidate;
      }
      cursor += span;
    }
    return null;
  }

  mergeTableCellDown(editor: any): void {
    const cell = this.getFocusedTableCell(editor);
    if (!cell) return;

    const row = cell.parentElement as HTMLTableRowElement | null;
    const nextRow = row?.nextElementSibling as HTMLTableRowElement | null;
    if (!row || !nextRow) {
      this.notificationService.showMessage('No row available below to merge.', 'warning');
      return;
    }

    const colStart = this.getColumnStartIndex(cell);
    const belowCell = this.getCoveringCell(nextRow, colStart);
    if (!belowCell) {
      this.notificationService.showMessage('No matching cell below to merge.', 'warning');
      return;
    }

    const currentRowspan = Number(cell.getAttribute('rowspan') || '1');
    const belowRowspan = Number(belowCell.getAttribute('rowspan') || '1');
    cell.setAttribute('rowspan', String(currentRowspan + belowRowspan));

    const primaryInput = this.normalizeInputCell(cell, '');
    const secondaryInput = (belowCell.querySelector('textarea') || belowCell.querySelector('input')) as HTMLTextAreaElement | null;
    this.mergeCellValues(primaryInput, secondaryInput);

    belowCell.remove();
    this.normalizeMergedTable(cell.closest('table'));
    primaryInput.focus();
  }

  private normalizeMergedTable(table: HTMLTableElement | null): void {
    if (!table || !table.rows || table.rows.length === 0) {
      return;
    }

    const targetColumns = this.getRowVisualColspan(table.rows[0]);
    if (targetColumns <= 0) {
      return;
    }

    Array.from(table.rows).forEach((row: HTMLTableRowElement) => {
      let consumed = 0;
      const cells = Array.from(row.cells);
      cells.forEach((cell: HTMLTableCellElement) => {
        const rawSpan = Number(cell.getAttribute('colspan') || '1');
        const span = Number.isFinite(rawSpan) && rawSpan > 0 ? rawSpan : 1;

        if (consumed >= targetColumns) {
          cell.remove();
          return;
        }

        if (consumed + span > targetColumns) {
          const allowed = targetColumns - consumed;
          if (allowed <= 0) {
            cell.remove();
            return;
          }
          if (allowed === 1) {
            cell.removeAttribute('colspan');
          } else {
            cell.setAttribute('colspan', String(allowed));
          }
          consumed += allowed;
          return;
        }

        consumed += span;
      });
    });
  }

  private getRowVisualColspan(row: HTMLTableRowElement): number {
    return Array.from(row.cells).reduce((sum: number, cell: HTMLTableCellElement) => {
      const rawSpan = Number(cell.getAttribute('colspan') || '1');
      const span = Number.isFinite(rawSpan) && rawSpan > 0 ? rawSpan : 1;
      return sum + span;
    }, 0);
  }

  isAttachmentType(fieldType: string | undefined): boolean {
    const normalizedType = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
    return normalizedType === 'attachment' || normalizedType === 'file';
  }

  isTableType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'table';
  }

  isIndividualPipelineFooterType(fieldType: string | undefined): boolean {
    return (fieldType || '').toLowerCase().replace(/\s+/g, '_') === 'individual_pipeline_footer';
  }

  getGenericPreviewFields(): FormField[] {
    if (!this.selectedForm?.fields) {
      return [];
    }
    return this.selectedForm.fields.filter((field: FormField) =>
      !this.isDocumentHeaderType(field.type) &&
      field.type !== 'footer' &&
      !this.isIndividualPipelineFooterType(field.type)
    );
  }

  getPreviewDepartmentFooterField(): FormField | null {
    if (!this.selectedForm?.fields) {
      return null;
    }
    return this.selectedForm.fields.find((field: FormField) => field.type === 'footer') || null;
  }

  getPreviewIndividualFooterField(): FormField | null {
    if (!this.selectedForm?.fields) {
      return null;
    }
    return this.selectedForm.fields.find((field: FormField) => this.isIndividualPipelineFooterType(field.type)) || null;
  }

  getDocumentHeaderPreviewValue(): string {
    const control = this.getDocumentHeaderControl();
    const value = control?.value;
    if (value === null || value === undefined || value === '') {
      return this.selectedForm?.name || this.selectedForm?.txtFormName || 'Form Preview';
    }
    return String(value);
  }

  isFormNameLabel(label: string | undefined): boolean {
    const normalizedLabel = (label || '').trim().toLowerCase();
    const normalizedFormName = (this.selectedForm?.name || this.selectedForm?.txtFormName || '').trim().toLowerCase();
    return !!normalizedLabel && !!normalizedFormName && normalizedLabel === normalizedFormName;
  }

  getPreviewFieldValue(field: FormField): any {
    const key = this.getFieldName(field.label);
    return this.applicationForm?.get(key)?.value;
  }

  getPreviewFieldDisplayValue(field: FormField): string {
    const value = this.getPreviewFieldValue(field);
    if (value === null || value === undefined || value === '') {
      return '-';
    }

    if (field.type === 'checkbox') {
      return value ? 'Yes' : 'No';
    }

    if (field.type === 'date') {
      const date = new Date(value);
      if (!Number.isNaN(date.getTime())) {
        return date.toLocaleDateString();
      }
    }

    if (this.isAttachmentType(field.type) && typeof value === 'object') {
      return value.fileName || '-';
    }

    return String(value);
  }

  getPreviewWordEditorValue(field: FormField): SafeHtml {
    const value = this.getPreviewFieldValue(field);
    if (value === null || value === undefined || value === '') {
      return this.sanitizer.bypassSecurityTrustHtml('<p>-</p>');
    }
    return this.sanitizer.bypassSecurityTrustHtml(this.normalizeWordEditorHtmlForDisplay(String(value)));
  }

  private normalizeWordEditorHtmlForDisplay(html: string): string {
    if (!html) return '';
    const wrapper = document.createElement('div');
    wrapper.innerHTML = html;

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

  getPreviewTableValue(field: FormField): any[][] {
    const value = this.getPreviewFieldValue(field);
    if (Array.isArray(value)) {
      return value;
    }
    return [];
  }

  getIndividualPipelineFooterFields(field: FormField): IndividualPipelineFooterField[] {
    const fieldName = this.getFieldName(field.label);
    const controlValue = this.applicationForm?.get(fieldName)?.value;
    if (Array.isArray(controlValue)) {
      return controlValue as IndividualPipelineFooterField[];
    }
    return [];
  }

  addIndividualFooterSection(field: FormField): void {
    const fieldName = this.getFieldName(field.label);
    const current = this.getIndividualPipelineFooterFields(field);
    const next = [
      ...current,
      {
        key: `wf_${current.length + 1}`,
        label: 'New Field',
        order: current.length + 1,
        users: []
      }
    ];
    this.applicationForm.get(fieldName)?.setValue(next);
    this.applicationForm.get(fieldName)?.markAsDirty();
  }

  removeIndividualFooterSection(field: FormField, index: number): void {
    const fieldName = this.getFieldName(field.label);
    const current = this.getIndividualPipelineFooterFields(field);
    const next = current.filter((_: IndividualPipelineFooterField, i: number) => i !== index).map((section, i) => ({
      ...section,
      order: i + 1,
      key: section.key || `wf_${i + 1}`
    }));
    this.applicationForm.get(fieldName)?.setValue(next);
    this.applicationForm.get(fieldName)?.markAsDirty();
  }

  updateIndividualFooterSectionLabel(field: FormField, index: number, label: string): void {
    const fieldName = this.getFieldName(field.label);
    const current = this.getIndividualPipelineFooterFields(field);
    const next = current.map((section: IndividualPipelineFooterField, i: number) =>
      i === index
        ? {
            ...section,
            label: label ?? ''
          }
        : section
    );
    this.applicationForm.get(fieldName)?.setValue(next);
    this.applicationForm.get(fieldName)?.markAsDirty();
  }

  updateIndividualFooterSectionUsers(field: FormField, index: number, users: any[]): void {
    const fieldName = this.getFieldName(field.label);
    const current = this.getIndividualPipelineFooterFields(field);
    const next = current.map((section: IndividualPipelineFooterField, i: number) =>
      i === index
        ? {
            ...section,
            users: Array.isArray(users) ? users : []
          }
        : section
    );
    this.applicationForm.get(fieldName)?.setValue(next);
    this.applicationForm.get(fieldName)?.markAsDirty();
  }

  markIndividualFooterDirty(field: FormField): void {
    const fieldName = this.getFieldName(field.label);
    this.applicationForm.get(fieldName)?.markAsDirty();
  }

  syncIndividualFooterSections(field: FormField, sections: IndividualPipelineFooterField[]): void {
    const fieldName = this.getFieldName(field.label);
    const next = (sections || []).map((section, i) => ({
      key: section.key || `wf_${i + 1}`,
      label: section.label || 'New Field',
      order: i + 1,
      users: Array.isArray(section.users) ? section.users : []
    }));
    this.applicationForm.get(fieldName)?.setValue(next);
    this.applicationForm.get(fieldName)?.markAsDirty();
  }

  private getInitialIndividualFooterSections(field: FormField): IndividualPipelineFooterField[] {
    if (!field?.txtFieldOptions) {
      return [];
    }
    try {
      const parsed = JSON.parse(field.txtFieldOptions);
      const sections = Array.isArray(parsed?.sections) ? parsed.sections : [];
      return sections
        .map((section: any, index: number) => ({
          key: section?.key || `wf_${index + 1}`,
          label: section?.label || 'New Field',
          order: Number(section?.order) || (index + 1),
          users: Array.isArray(section?.users) ? section.users : []
        }))
        .sort((a: IndividualPipelineFooterField, b: IndividualPipelineFooterField) => a.order - b.order);
    } catch (e) {
      return [];
    }
  }

  private normalizePipelineUser(user: any): any {
    if (!user || typeof user !== 'object') return user;
    return {
      serUserId: user.serUserId ?? user.userId ?? user.id ?? null,
      txtUserName: user.txtUserName || user.userName || user.name || '',
      txtSignaturePath: user.txtSignaturePath || '',
      txtDepartmentName: user.txtDepartmentName || user.hrTblDepartment?.txtDepartmentName || '',
      txtDesignation: user.txtDesignation || '',
      cfgTblRole: user.cfgTblRole && typeof user.cfgTblRole === 'object'
        ? {
            serRoleId: user.cfgTblRole.serRoleId ?? null,
            txtRoleName: user.cfgTblRole.txtRoleName || ''
          }
        : null
    };
  }

  getIndividualFooterColSpan(section: IndividualPipelineFooterField): number {
    const users = Array.isArray(section?.users) ? section.users : [];
    return Math.max(users.length, 1);
  }

  getIndividualFooterSlots(section: IndividualPipelineFooterField): any[] {
    const users = Array.isArray(section?.users) ? section.users : [];
    return users.length > 0 ? users : [null];
  }

  getIndividualFooterUserLabel(user: any, section: IndividualPipelineFooterField): string {
    if (!user) return '';
    const name = user.txtUserName || user.userName || user.name || '';
    const role =
      user.cfgTblRole?.txtRoleName ||
      user.txtRoleName ||
      user.roleName ||
      '';
    const roleLine = role ? `\n(${role})` : '';
    return `${name}${roleLine}`;
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

      // Ensure attachments are captured as base64 payloads
      const attachmentFieldNames = new Set<string>();
      this.selectedForm.fields.forEach((field: FormField) => {
        const type = (field.type || '').toString().toLowerCase();
        if (type === 'attachment' || type === 'file') {
          attachmentFieldNames.add(this.getFieldName(field.label));
        }
      });
      for (const fieldName of Object.keys(this.attachmentFiles)) {
        if (!attachmentFieldNames.has(fieldName)) continue;
        if (!this.attachmentPayloads[fieldName]) {
          try {
            this.attachmentPayloads[fieldName] = await this.buildAttachmentPayload(this.attachmentFiles[fieldName]);
          } catch {}
        }
      }

      // Convert table FormArrays to regular arrays for JSON serialization
      this.selectedForm.fields.forEach((field: FormField) => {
        const type = (field.type || '').toString().toLowerCase();
        if (type === 'table') {
          const fieldName = this.getFieldName(field.label);
          const tableArray = this.getTableFormArray(fieldName);
          if (tableArray) {
            formData[fieldName] = tableArray.value;
          }
        }
        if (type === 'attachment' || type === 'file') {
          const fieldName = this.getFieldName(field.label);
          if (this.attachmentPayloads[fieldName]) {
            formData[fieldName] = this.attachmentPayloads[fieldName];
          }
        }
        if (type === 'multi_attachment') {
          const fieldName = this.getFieldName(field.label);
          if (this.multiAttachmentPayloads[fieldName]) {
            formData[fieldName] = this.multiAttachmentPayloads[fieldName];
          }
        }
        if (type === 'individual_pipeline_footer') {
          const fieldName = this.getFieldName(field.label);
          const footerFields = this.getIndividualPipelineFooterFields(field).map((section, idx) => ({
            key: section.key || `wf_${idx + 1}`,
            label: section.label || 'New Field',
            order: idx + 1,
            users: Array.isArray(section.users) ? section.users.map((u: any) => this.normalizePipelineUser(u)) : []
          }));
          formData[fieldName] = footerFields;
          formData.footerFields = footerFields;
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
