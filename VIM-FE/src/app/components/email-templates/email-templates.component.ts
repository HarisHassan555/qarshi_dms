import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ModalComponent } from 'angular-custom-modal';
import { NotificationService } from 'src/app/NotificationService';
import {
  EmailLayout,
  EmailTemplate,
  EmailTemplateService,
} from 'src/app/services/email-template/email-template.service';

@Component({
  selector: 'app-email-templates',
  templateUrl: './email-templates.component.html',
  styleUrls: ['./email-templates.component.css'],
})
export class EmailTemplatesComponent implements OnInit, OnDestroy {
  @ViewChild('templateModal') templateModal!: ModalComponent;
  @ViewChild('layoutModal') layoutModal!: ModalComponent;
  @ViewChild('previewModal') previewModal!: ModalComponent;

  previewSubject = '';
  previewHtmlSafe: SafeHtml | null = null;

  activeTab: 'templates' | 'layouts' = 'templates';
  search = '';
  loading = false;
  saving = false;
  templateSubmit = false;
  layoutSubmit = false;

  templates: EmailTemplate[] = [];
  layouts: EmailLayout[] = [];

  templateForm!: FormGroup;
  layoutForm!: FormGroup;

  templateEditorToken = 0;
  layoutEditorToken = 0;

  templateCols = [
    { field: 'templateCode', title: 'Code', width: '160px' },
    { field: 'templateName', title: 'Name', width: '200px' },
    { field: 'category', title: 'Category', width: '120px' },
    { field: 'subject', title: 'Subject' },
    { field: 'layoutName', title: 'Layout', width: '140px' },
    { field: 'active', title: 'Active', width: '90px' },
    { field: 'actions', title: 'Actions', width: '200px', sort: false, headerClass: 'justify-center' },
  ];

  layoutCols = [
    { field: 'layoutCode', title: 'Code', width: '160px' },
    { field: 'layoutName', title: 'Name' },
    { field: 'active', title: 'Active', width: '90px' },
    { field: 'actions', title: 'Actions', width: '100px', sort: false, headerClass: 'justify-center' },
  ];

  readonly categoryOptions = ['USER', 'DELEGATION', 'WORKFLOW', 'SYSTEM'];

  constructor(
    private fb: FormBuilder,
    private emailTemplateService: EmailTemplateService,
    private notificationService: NotificationService,
    private sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.templateForm = this.fb.group({
      id: [null],
      templateCode: ['', [Validators.required, Validators.maxLength(64)]],
      templateName: ['', Validators.required],
      description: [''],
      subject: ['', Validators.required],
      bodyHtml: ['', Validators.required],
      layoutId: [null],
      placeholderHelp: [''],
      category: ['USER'],
      sortOrder: [100],
      active: [true],
    });

    this.layoutForm = this.fb.group({
      id: [null],
      layoutCode: ['', [Validators.required, Validators.maxLength(64)]],
      layoutName: ['', Validators.required],
      description: [''],
      htmlWrapper: ['', Validators.required],
      active: [true],
    });

    this.loadAll();
    window.addEventListener('velocity-tour-prepare', this.onTourPrepare);
  }

  ngOnDestroy(): void {
    window.removeEventListener('velocity-tour-prepare', this.onTourPrepare);
  }

  private onTourPrepare = (event: Event): void => {
    const action = (event as CustomEvent<{ action?: string }>).detail?.action;
    switch (action) {
      case 'email-templates-close-modals':
        this.closeTourModals();
        break;
      case 'email-templates-tab-templates':
        this.closeTourModals();
        this.setTab('templates');
        break;
      case 'email-templates-tab-layouts':
        this.closeTourModals();
        this.setTab('layouts');
        break;
      case 'email-templates-open-template-modal':
        this.setTab('templates');
        if (!this.isTourTargetVisible('email-tpl-template-modal')) {
          this.previewModal?.close();
          this.layoutModal?.close();
          this.openAddTemplate();
        }
        break;
      default:
        break;
    }
  };

  private closeTourModals(): void {
    this.templateModal?.close();
    this.layoutModal?.close();
    this.previewModal?.close();
  }

  private isTourTargetVisible(target: string): boolean {
    const el = document.querySelector(`[data-tour="${target}"]`) as HTMLElement | null;
    if (!el) {
      return false;
    }
    const rect = el.getBoundingClientRect();
    return rect.width > 8 && rect.height > 8 && rect.bottom > 0 && rect.top < window.innerHeight;
  }

  setTab(tab: 'templates' | 'layouts'): void {
    this.activeTab = tab;
  }

  loadAll(): void {
    this.loading = true;
    this.emailTemplateService.getLayouts().subscribe({
      next: (layouts) => {
        this.layouts = layouts || [];
        this.emailTemplateService.getTemplates().subscribe({
          next: (templates) => {
            this.templates = templates || [];
            this.loading = false;
          },
          error: () => {
            this.templates = [];
            this.loading = false;
            this.notificationService.showMessage('Could not load email templates.', 'danger');
          },
        });
      },
      error: () => {
        this.layouts = [];
        this.loading = false;
        this.notificationService.showMessage('Could not load email layouts.', 'danger');
      },
    });
  }

  openAddTemplate(): void {
    this.templateSubmit = false;
    this.templateForm.reset({
      id: null,
      templateCode: '',
      templateName: '',
      description: '',
      subject: '',
      bodyHtml: this.defaultTemplateBody(),
      layoutId: null,
      placeholderHelp: '{{displayName}}, {{loginName}}, {{loginUrl}}',
      category: 'USER',
      sortOrder: 100,
      active: true,
    });
    this.bumpTemplateEditor();
    this.templateModal.open();
  }

  openPreviewTemplate(row: EmailTemplate): void {
    const runPreview = (template: EmailTemplate) => {
      const rendered = this.buildTemplatePreview(template);
      this.previewSubject = rendered.subject;
      this.previewHtmlSafe = this.sanitizer.bypassSecurityTrustHtml(rendered.html);
      this.previewModal.open();
    };
    if (row.id) {
      this.emailTemplateService.getTemplate(row.id).subscribe({
        next: (full) => runPreview(full || row),
        error: () => runPreview(row),
      });
      return;
    }
    runPreview(row);
  }

  openEditTemplate(row: EmailTemplate): void {
    this.templateSubmit = false;
    this.templateForm.patchValue({
      id: row.id,
      templateCode: row.templateCode,
      templateName: row.templateName,
      description: row.description || '',
      subject: row.subject,
      bodyHtml: row.bodyHtml,
      layoutId: row.layoutId || null,
      placeholderHelp: row.placeholderHelp || '',
      category: row.category || 'USER',
      sortOrder: row.sortOrder ?? 100,
      active: row.active !== false,
    });
    this.bumpTemplateEditor();
    this.templateModal.open();
  }

  saveTemplate(): void {
    this.templateSubmit = true;
    if (this.templateForm.invalid) {
      return;
    }
    this.saving = true;
    const payload = this.templateForm.getRawValue() as EmailTemplate;
    this.emailTemplateService.saveTemplate(payload).subscribe({
      next: () => {
        this.saving = false;
        this.templateModal.close();
        this.notificationService.showMessage('Email template saved.', 'success');
        this.loadAll();
      },
      error: () => {
        this.saving = false;
        this.notificationService.showMessage('Could not save template. Check code is unique (e.g. USER_WELCOME).', 'danger');
      },
    });
  }

  deleteTemplate(row: EmailTemplate): void {
    if (!row.id || !confirm('Delete this email template?')) {
      return;
    }
    this.emailTemplateService.deleteTemplates([row.id]).subscribe({
      next: (res) => {
        if (String(res).includes('Success')) {
          this.notificationService.showMessage('Template deleted.', 'success');
          this.loadAll();
        } else {
          this.notificationService.showMessage('Could not delete template.', 'danger');
        }
      },
      error: () => this.notificationService.showMessage('Could not delete template.', 'danger'),
    });
  }

  openAddLayout(): void {
    this.layoutSubmit = false;
    this.layoutForm.reset({
      id: null,
      layoutCode: 'QARSHI_RED_WELCOME',
      layoutName: 'Qarshi Red – Welcome',
      description: '',
      htmlWrapper: this.defaultLayoutWrapper(),
      active: true,
    });
    this.bumpLayoutEditor();
    this.layoutModal.open();
  }

  openEditLayout(row: EmailLayout): void {
    this.layoutSubmit = false;
    this.layoutForm.patchValue({
      id: row.id,
      layoutCode: row.layoutCode,
      layoutName: row.layoutName,
      description: row.description || '',
      htmlWrapper: row.htmlWrapper,
      active: row.active !== false,
    });
    this.bumpLayoutEditor();
    this.layoutModal.open();
  }

  saveLayout(): void {
    this.layoutSubmit = true;
    if (this.layoutForm.invalid) {
      return;
    }
    const wrapper = this.layoutForm.get('htmlWrapper')?.value || '';
    if (!String(wrapper).includes('{{content}}')) {
      this.notificationService.showMessage('Layout HTML must include {{content}}.', 'danger');
      return;
    }
    this.saving = true;
    const payload = this.layoutForm.getRawValue() as EmailLayout;
    this.emailTemplateService.saveLayout(payload).subscribe({
      next: () => {
        this.saving = false;
        this.layoutModal.close();
        this.notificationService.showMessage('Email layout saved.', 'success');
        this.loadAll();
      },
      error: () => {
        this.saving = false;
        this.notificationService.showMessage('Could not save layout.', 'danger');
      },
    });
  }

  deleteLayout(row: EmailLayout): void {
    if (!row.id || !confirm('Delete this layout? Templates using it must be reassigned first.')) {
      return;
    }
    this.emailTemplateService.deleteLayouts([row.id]).subscribe({
      next: (res) => {
        if (String(res).includes('Success')) {
          this.notificationService.showMessage('Layout deleted.', 'success');
          this.loadAll();
        } else {
          this.notificationService.showMessage('Could not delete layout (may be in use).', 'danger');
        }
      },
      error: () => this.notificationService.showMessage('Could not delete layout.', 'danger'),
    });
  }

  layoutOptions(): { id: number; name: string }[] {
    return (this.layouts || [])
      .filter((l) => l.id && l.active !== false)
      .map((l) => ({ id: l.id as number, name: l.layoutName }));
  }

  private defaultLayoutWrapper(): string {
    return `<!DOCTYPE html>
<html lang="en">
<body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#f3f4f6;">
<tr><td align="center" style="padding:28px 16px;">
<table role="presentation" width="600" style="max-width:600px;background:#fff;border-radius:10px;overflow:hidden;border:1px solid #e5e7eb;">
<tr><td style="background:#c8102e;padding:32px 28px;text-align:center;">
<h1 style="margin:0;font-size:24px;color:#fff;">Qarshi DMS</h1>
</td></tr>
<tr><td style="padding:32px 28px;color:#1f2937;">{{content}}</td></tr>
<tr><td style="background:#fef2f2;padding:18px 28px;border-top:4px solid #c8102e;">
<p style="margin:0;font-size:12px;color:#991b1b;text-align:center;">Qarshi DMS – keep your password confidential.</p>
</td></tr>
</table>
</td></tr>
</table>
</body>
</html>`;
  }

  private buildTemplatePreview(template: EmailTemplate): { subject: string; html: string } {
    const samples = this.samplePlaceholderValues();
    const body = this.applyPlaceholders(template.bodyHtml || '', samples);
    const layout = this.resolveLayoutForTemplate(template);
    let html = body;

    if (layout?.htmlWrapper) {
      html = this.mergeLayoutAndBody(layout.htmlWrapper, body);
      html = this.applyPlaceholders(html, samples, ['content', 'body']);
    } else if (!/^\s*<!DOCTYPE/i.test(html) && !/^\s*<html/i.test(html)) {
      html = `<!DOCTYPE html><html><head><meta charset="utf-8"></head><body style="margin:0;padding:16px;font-family:Arial,sans-serif;">${body}</body></html>`;
    }

    const subject = this.applyPlaceholders(template.subject || '', samples);
    return { subject, html };
  }

  private resolveLayoutForTemplate(template: EmailTemplate): EmailLayout | undefined {
    const layoutId = Number(template.layoutId);
    if (Number.isFinite(layoutId) && layoutId > 0) {
      const byId = (this.layouts || []).find((l) => Number(l.id) === layoutId);
      if (byId) {
        return byId;
      }
    }
    const name = (template.layoutName || '').trim().toLowerCase();
    if (name) {
      return (this.layouts || []).find(
        (l) => (l.layoutName || '').trim().toLowerCase() === name,
      );
    }
    return undefined;
  }

  private mergeLayoutAndBody(wrapper: string, body: string): string {
    if (/\{\{\s*content\s*\}\}/i.test(wrapper)) {
      return wrapper.replace(/\{\{\s*content\s*\}\}/gi, body);
    }
    if (/\{\{\s*body\s*\}\}/i.test(wrapper)) {
      return wrapper.replace(/\{\{\s*body\s*\}\}/gi, body);
    }
    if (/\[body\]/i.test(wrapper)) {
      return wrapper.replace(/\[body\]/gi, body);
    }
    return `${wrapper}${body}`;
  }

  private samplePlaceholderValues(): Record<string, string> {
    const origin = typeof window !== 'undefined' ? window.location.origin : '';
    const loginUrl = `${origin}/velocity/auth/login`;
    return {
      displayName: 'Ali Khan',
      loginName: 'ali.khan',
      plainPassword: 'TempPass123!',
      loginUrl,
      userName: 'ali.khan',
      recipientEmail: 'ali.khan@example.com',
      applicationName: 'Qarshi DMS',
    };
  }

  private applyPlaceholders(text: string, values: Record<string, string>, skipKeys: string[] = []): string {
    const skip = new Set(skipKeys.map((k) => k.toLowerCase()));
    let result = text || '';
    for (const [key, value] of Object.entries(values)) {
      if (skip.has(key.toLowerCase())) {
        continue;
      }
      const pattern = new RegExp(`\\{\\{\\s*${this.escapeRegExp(key)}\\s*\\}\\}`, 'gi');
      result = result.replace(pattern, value);
    }
    return result;
  }

  private escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  private bumpTemplateEditor(): void {
    this.templateEditorToken += 1;
  }

  private bumpLayoutEditor(): void {
    this.layoutEditorToken += 1;
  }

  private defaultTemplateBody(): string {
    return `<h2 style="margin:0 0 16px;font-size:20px;color:#c8102e;">Welcome aboard</h2>
<p>Dear {{displayName}},</p>
<p>Your user account has been created.</p>
<table role="presentation" width="100%" style="border-collapse:collapse;margin:16px 0;">
<tr>
<td style="padding:10px 12px;background:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Login name</td>
<td style="padding:10px 12px;background:#f9fafb;">{{loginName}}</td>
</tr>
<tr>
<td style="padding:10px 12px;background:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Password</td>
<td style="padding:10px 12px;background:#f9fafb;">{{plainPassword}}</td>
</tr>
</table>
<p style="text-align:center;"><a href="{{loginUrl}}" style="display:inline-block;padding:12px 28px;background:#c8102e;color:#fff;text-decoration:none;font-weight:bold;border-radius:6px;">Sign in to DMS</a></p>`;
  }
}
