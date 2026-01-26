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

  // AI Suggestions
  suggestions: string[] = [];
  isLoadingSuggestions = false;
  lastAppliedField: string | null = null;
  private openRouterKey = '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';

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

  fullSuggestions: any[] = [];

  async suggestAllFields() {
    if (!this.selectedForm || !this.applicationForm) return;

    // Find the Item Name field
    const itemNameField = this.selectedForm.fields.find(f =>
      f.label.toLowerCase().includes('name of item') ||
      f.label.toLowerCase().includes('item name') ||
      f.label.toLowerCase().includes('item')
    );

    const fieldName = itemNameField ? this.getFieldName(itemNameField.label) : '';
    const itemName = fieldName ? this.applicationForm.get(fieldName)?.value : '';

    if (!itemName) {
      this.notificationService.showMessage('Please enter the Item Name first for AI to suggest details.', 'warning');
      return;
    }

    this.isLoadingSuggestions = true;
    this.fullSuggestions = [];

    const prompt = `
      You are an AI assistant that generates structured product suggestions.

      Item Name: "${itemName}"

      Generate EXACTLY 3 distinct variations.

      For EACH variation return an object with ONLY these keys:
      - "specification"
      - "purpose"
      - "price"
      - "imageUrl"

      Rules:

      1) specification  
      - Include realistic technical details  
      - Mention material, capacity, size, or key features where applicable  
      - Keep it concise (1–3 sentences)

      2) purpose  
      - Describe how the item is used and its main benefit  
      - Keep it concise (1–2 sentences)

      3) price  
      - Must be a realistic numeric value in USD  
      - Base it on common global market prices for this item category  
      - Do NOT include currency symbols  
      - Do NOT include text like "approx" or "about"  
      - Example: 1200, 349.99, 85

      4) imageUrl  
      - MUST use this exact format:
        https://source.unsplash.com/featured/?${encodeURIComponent(itemName)}
      - Do NOT invent photo IDs  
      - Do NOT change the domain  
      - Do NOT use placeholder images  

      Output Rules:
      - Return ONLY valid JSON  
      - Return ONLY a JSON array of 3 objects  
      - Do NOT add explanations  
      - Do NOT add markdown  
      - Do NOT add extra keys  
      - Do NOT wrap the JSON in quotes  

      Expected JSON format:

      [
        {
          "specification": "...",
          "purpose": "...",
          "price": 0,
          "imageUrl": "https://source.unsplash.com/featured/?..."
        },
        {
          "specification": "...",
          "purpose": "...",
          "price": 0,
          "imageUrl": "https://source.unsplash.com/featured/?..."
        },
        {
          "specification": "...",
          "purpose": "...",
          "price": 0,
          "imageUrl": "https://source.unsplash.com/featured/?..."
        }
      ]
    `;

    try {
      const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${this.openRouterKey}`,
          "Content-Type": "application/json",
          "HTTP-Referer": window.location.origin,
          "X-Title": "Qarshi DMS"
        },
        body: JSON.stringify({
          "model": "google/gemini-2.0-flash-exp:free",
          "messages": [
            {
              "role": "user",
              "content": prompt
            }
          ]
        })
      });

      if (!response.ok) {
        throw new Error(`API returned status ${response.status}`);
      }

      const result = await response.json();
      const content = result?.choices?.[0]?.message?.content;

      if (!content) throw new Error("Empty response from AI");

      // Extract JSON from response
      const start = content.indexOf('[');
      const end = content.lastIndexOf(']');
      if (start !== -1 && end !== -1) {
        const jsonStr = content.substring(start, end + 1);
        this.fullSuggestions = JSON.parse(jsonStr).map((item: any) => ({
          specification: item.specification || '',
          purpose: item.purpose || '',
          price: Number(item.price) || 0,
          imageUrl:
            item.imageUrl ||
            `https://source.unsplash.com/featured/?${encodeURIComponent(itemName)}`
        }));
        this.notificationService.showMessage('AI suggestions generated! Please select one.', 'success');
      } else {
        throw new Error("Invalid AI response format");
      }
    } catch (error) {
      console.error('AI Error:', error);
      this.notificationService.showMessage('Failed to get suggestions from AI', 'warning');
    } finally {
      this.isLoadingSuggestions = false;
    }
  }

  selectFullSuggestion(suggestion: any) {
    if (!this.selectedForm || !this.applicationForm) return;

    this.selectedForm.fields.forEach(field => {
      const label = field.label.toLowerCase();
      const targetFieldName = this.getFieldName(field.label);

      if (label.includes('specification')) {
        this.applicationForm.get(targetFieldName)?.patchValue(suggestion.specification);
      } else if (label.includes('utility') || label.includes('purpose')) {
        this.applicationForm.get(targetFieldName)?.patchValue(suggestion.purpose);
      } else if (label.includes('price')) {
        this.applicationForm.get(targetFieldName)?.patchValue(suggestion.price);
      }
    });

    this.fullSuggestions = [];
    this.notificationService.showMessage('Form populated with selected suggestion!', 'success');
  }

  async getAISuggestions(field: FormField) {
    // This is also updated to use AI for consistency if called
    const itemNameField = this.selectedForm?.fields.find(f => f.label.toLowerCase().includes('item') || f.label.toLowerCase().includes('product'));
    const itemName = itemNameField ? this.applicationForm.get(this.getFieldName(itemNameField.label))?.value : '';

    this.isLoadingSuggestions = true;
    this.suggestions = [];
    this.lastAppliedField = field.label;

    const prompt = `Based on the Item Name: "${itemName}", suggest 3 detailed variations for the field "${field.label}". 
                    Return ONLY a JSON array of 3 strings. No other text.`;

    try {
      const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${this.openRouterKey}`,
          "Content-Type": "application/json",
          "HTTP-Referer": window.location.origin,
          "X-Title": "Qarshi DMS"
        },
        body: JSON.stringify({
          "model": "meta-llama/llama-3.2-3b-instruct:free",
          "messages": [
            {
              "role": "user",
              "content": prompt
            }
          ]
        })
      });

      if (!response.ok) {
        throw new Error(`API returned status ${response.status}`);
      }

      const result = await response.json();
      const content = result?.choices?.[0]?.message?.content;

      if (!content) throw new Error("Empty response from AI");

      const start = content.indexOf('[');
      const end = content.lastIndexOf(']');
      if (start !== -1 && end !== -1) {
        const jsonStr = content.substring(start, end + 1);
        this.suggestions = JSON.parse(jsonStr);
      } else {
        throw new Error("Invalid AI response format");
      }
    } catch (error) {
      console.error('AI Error:', error);
      this.notificationService.showMessage('Failed to get suggestions from AI', 'warning');
    } finally {
      this.isLoadingSuggestions = false;
    }
  }

  selectSuggestion(suggestion: string, field: FormField) {
    const fieldName = this.getFieldName(field.label);
    this.applicationForm.get(fieldName)?.patchValue(suggestion);
    this.suggestions = [];
  }
}

