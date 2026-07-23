import { Component, ElementRef, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { urls } from 'src/app/utils/urls';
import {
    WORD_EDITOR_CKEDITOR,
    WORD_EDITOR_CKEDITOR_CONFIG,
    normalizeWordEditorValueForCkeditor,
} from 'src/app/utils/word-editor-ckeditor.util';
import {
    resolveDocumentHeaderAddress,
    resolveDocumentHeaderBrandTitle,
    resolveDocumentHeaderLogoPath,
} from 'src/app/utils/document-header.util';

type TemplateFieldType =
    'document_header'
    | 'document_header_qu'
    | 'document_header_qf'
    | 'document_header_qri'
    | 'document_header_qb'
    | 'footer'
    | 'individual_pipeline_footer'
    | 'application_code'
    | 'pipeline_signature'
    | 'dynamic_signature'
    | 'text'
    | 'integer'
    | 'decimal'
    | 'number'
    | 'date'
    | 'email'
    | 'textarea'
    | 'word_editor'
    | 'attachment'
    | 'select'
    | 'checkbox'
    | 'radio'
    | 'table'
    | 'orientation';
type PipelineFieldRight = 'fill' | 'edit' | 'hide';

interface TemplateField {
    id: string;
    label: string;
    type: TemplateFieldType;
    required: boolean;
    placeholder: string;
    placement?: { x: number; y: number; width: number; height: number };
    style: {
        fontSize: number;
        bold: boolean;
        italic: boolean;
        underline: boolean;
        textAlign: 'left' | 'center' | 'right';
    };
    pipelineStepId?: string;
    pipelineApproverIndex?: number;
    signatureTargetId?: string;
    options?: string[];
    optionPlacements?: RadioOptionPlacement[];
}

interface RadioOptionPlacement {
    id: string;
    label: string;
    page: number;
    placement: { x: number; y: number; width: number; height: number };
}

interface IndividualPipelineFooterSection {
    key: string;
    label: string;
    order: number;
    users: any[];
}

interface PipelineFieldPermission {
    fieldId: string;
    fieldLabel?: string;
    fieldType?: TemplateFieldType;
    right: PipelineFieldRight;
}

interface PipelineStep {
    id: string;
    name: string;
    type: string;
    fieldPermissions?: PipelineFieldPermission[];
    fieldPermissionsConfigured?: boolean;
}

@Component({
    selector: 'app-template-fill',
    templateUrl: './template-fill.component.html',
    styleUrls: ['./template-fill.component.css']
})
export class TemplateFillComponent implements OnInit {
    template: any = null;
    savedTemplate: SavedTemplateDefinition | null = null;
    safeHtml: SafeHtml = '';
    values: { [fieldId: string]: any } = {};
    allUsers: any[] = [];
    userPipeline: IndividualPipelineFooterSection[] = [];
    submittedCode = '';
    generatedApplicationCode = '';
    isSendingTestEmail = false;
    testEmailMessage = '';
    readonly wordEditor = WORD_EDITOR_CKEDITOR;
    readonly wordEditorConfig = WORD_EDITOR_CKEDITOR_CONFIG;
    private initiatorSignatureApproved = false;
    private acceptedWordEditorValues: { [fieldId: string]: string } = {};
    private revertingWordEditorFields = new Set<string>();

    constructor(
        private elementRef: ElementRef<HTMLElement>,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        private notificationService: NotificationService,
        private templateWorkflowService: TemplateWorkflowService,
        private userService: UserService
    ) { }

    async ngOnInit(): Promise<void> {
        this.loadUsers();
        const templateId = this.route.snapshot.paramMap.get('id');
        try {
            if (templateId) {
                this.savedTemplate = await firstValueFrom(this.templateWorkflowService.getTemplate(templateId));
                this.template = this.savedTemplate?.payload || null;
            } else {
                const rawTemplate = sessionStorage.getItem('templateBuilder:lastTemplate');
                this.template = rawTemplate ? JSON.parse(rawTemplate) : null;
            }

            if (!this.template) {
                return;
            }

            this.generatedApplicationCode = this.savedTemplate
                ? await firstValueFrom(this.templateWorkflowService.peekNextTemplateCode(this.savedTemplate))
                : this.getSessionTemplatePreviewCode();
        } catch (error) {
            console.error('Template load failed', error);
            this.template = null;
            return;
        }

        (this.template.fields || []).forEach((field: TemplateField) => {
            this.values[field.id] = field.type === 'application_code'
                ? this.generatedApplicationCode
                : (field.type === 'checkbox' ? false : '');
            if (field.type === 'word_editor') {
                this.acceptedWordEditorValues[field.id] = '';
            }
        });
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.template.html || ''));
    }

    get pageIndexes(): number[] {
        const count = this.template?.page?.count || 1;
        return Array.from({ length: count }, (_, index) => index);
    }

    get fillableFields(): TemplateField[] {
        return (this.template?.fields || []).filter((field: TemplateField) =>
            !this.isDocumentRegionFieldType(field.type) && this.canCurrentStepFillField(field)
        );
    }

    isHeaderFieldType(type: TemplateFieldType): boolean {
        return [
            'document_header',
            'document_header_qu',
            'document_header_qf',
            'document_header_qri',
            'document_header_qb'
        ].includes(type);
    }

    isFooterFieldType(type: TemplateFieldType): boolean {
        return type === 'footer' || type === 'individual_pipeline_footer';
    }

    isDocumentRegionFieldType(type: TemplateFieldType): boolean {
        return this.isHeaderFieldType(type) || this.isFooterFieldType(type);
    }

    canCurrentStepFillField(field: TemplateField): boolean {
        if (field.type === 'application_code' || this.isDynamicSignatureField(field)) {
            return false;
        }

        const right = this.getCurrentStepFieldRight(field);
        return right === 'fill' || right === 'edit';
    }

    isFieldHiddenForCurrentStep(field: TemplateField): boolean {
        return this.getCurrentStepFieldRight(field) === 'hide';
    }

    getHeaderLogoPath(type: TemplateFieldType): string {
        return resolveDocumentHeaderLogoPath(type);
    }

    getHeaderBrandTitle(type: TemplateFieldType): string {
        return resolveDocumentHeaderBrandTitle(type);
    }

    getHeaderAddress(type: TemplateFieldType): string {
        return resolveDocumentHeaderAddress(type);
    }

    getInputStyle(field: TemplateField): { [key: string]: string | number } {
        return {
            'font-size.px': field.style?.fontSize || 14,
            'font-weight': field.style?.bold ? '700' : '400',
            'font-style': field.style?.italic ? 'italic' : 'normal',
            'text-decoration': field.style?.underline ? 'underline' : 'none',
            'text-align': field.style?.textAlign || 'left'
        };
    }

    getFieldValueText(field: TemplateField, optionLabel?: string): string {
        if (field.type === 'application_code') {
            return this.generatedApplicationCode;
        }

        if (this.isDynamicSignatureField(field)) {
            return this.getDynamicSignatureFallbackLabel(field);
        }

        if (field.type === 'checkbox') {
            return this.values[field.id] ? '✓' : '';
        }

        if (field.type === 'radio' && optionLabel !== undefined) {
            return this.normalizeRadioValue(this.values[field.id]) === this.normalizeRadioValue(optionLabel) ? '✓' : '';
        }

        return String(this.values[field.id] ?? '');
    }

    getRadioOptions(field: TemplateField): string[] {
        return (field.options || []).filter((option) => !!String(option || '').trim());
    }

    getRadioOptionPlacements(field: TemplateField): RadioOptionPlacement[] {
        return Array.isArray(field.optionPlacements) ? field.optionPlacements : [];
    }

    getFieldValueHtml(field: TemplateField): SafeHtml {
        return this.sanitizer.bypassSecurityTrustHtml(String(this.values[field.id] || ''));
    }

    getPanelInputType(field: TemplateField): string {
        return this.getHtmlInputType(field.type);
    }

    onPanelValueChanged(): void {
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.template.html || ''));
    }

    onWordEditorChanged(field: TemplateField, event: any): void {
        if (this.revertingWordEditorFields.has(field.id)) {
            const restoredValue = normalizeWordEditorValueForCkeditor(event?.editor?.getData?.() ?? this.values[field.id]);
            this.values[field.id] = restoredValue;
            this.acceptedWordEditorValues[field.id] = restoredValue;
            this.onPanelValueChanged();
            this.revertingWordEditorFields.delete(field.id);
            return;
        }

        const nextValue = normalizeWordEditorValueForCkeditor(this.values[field.id]);
        const previousValue = this.acceptedWordEditorValues[field.id] || '';
        this.values[field.id] = nextValue;
        this.onPanelValueChanged();

        if (!field.placement) {
            this.acceptedWordEditorValues[field.id] = nextValue;
            return;
        }

        setTimeout(() => {
            if (!this.doesWordEditorFieldOverflow(field.id)) {
                this.acceptedWordEditorValues[field.id] = nextValue;
                return;
            }

            this.revertingWordEditorFields.add(field.id);
            if (event?.editor?.execute) {
                event.editor.execute('undo');
            } else {
                this.values[field.id] = previousValue;
                this.onPanelValueChanged();
                this.revertingWordEditorFields.delete(field.id);
            }
        });
    }

    hasIndividualPipelineFooter(): boolean {
        return (this.template?.fields || []).some((field: TemplateField) => field.type === 'individual_pipeline_footer');
    }

    addUserPipelineSection(): void {
        this.userPipeline.push({
            key: `wf_${this.userPipeline.length + 1}_${Date.now()}`,
            label: 'New Field',
            order: this.userPipeline.length + 1,
            users: []
        });
    }

    removeUserPipelineSection(index: number): void {
        this.userPipeline.splice(index, 1);
        this.userPipeline = this.userPipeline.map((section, sectionIndex) => ({
            ...section,
            order: sectionIndex + 1,
            key: section.key || `wf_${sectionIndex + 1}`
        }));
    }

    markUserPipelineChanged(): void {
        this.userPipeline.forEach((section, index) => {
            section.order = index + 1;
            section.users = Array.isArray(section.users) ? section.users : [];
        });
    }

    getUserName(user: any): string {
        return user?.txtUserName || user?.userName || user?.name || user?.email || '';
    }

    getIndividualFooterColSpan(section: IndividualPipelineFooterSection): number {
        const users = Array.isArray(section?.users) ? section.users : [];
        return Math.max(users.length, 1);
    }

    getIndividualFooterSlots(section: IndividualPipelineFooterSection): any[] {
        const users = Array.isArray(section?.users) ? section.users : [];
        return users.length > 0 ? users : [null];
    }

    getIndividualFooterUserLabel(user: any): SafeHtml {
        const html = this.getIndividualFooterUserParts(user)
            .map((value) => this.escapeHtml(value))
            .join('<br>');
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }

    isDynamicSignatureField(field: TemplateField): boolean {
        return field.type === 'dynamic_signature' || field.type === 'pipeline_signature';
    }

    getDynamicSignatureSlots(field: TemplateField): any[] {
        const step = this.getDynamicSignatureStep(field);
        if (!step) {
            return [{ label: 'Dynamic Signatures' }];
        }
        if (step.type === 'initiator') {
            return this.getInitiatorSignatureSlots();
        }

        const approvedSlots = this.getApprovedSignatureSlots(step);
        if (approvedSlots.length > 0 && step.approvalMode !== 'AND') {
            return approvedSlots;
        }

        const configuredSlots = this.getConfiguredSignatureSlots(step);
        if (step.approvalMode === 'AND') {
            return configuredSlots.length > 0 ? this.mergeSignatureSlots(configuredSlots, approvedSlots) : approvedSlots;
        }

        return approvedSlots.length > 0
            ? approvedSlots
            : (configuredSlots.length > 0 ? configuredSlots : [{ label: step.name || 'Dynamic Signatures' }]);
    }

    getDynamicSignatureImageUrl(slot: any): string {
        if (!slot?.__signatureApproved) {
            return '';
        }

        const signaturePath = slot?.txtSignaturePath || slot?.signaturePath || slot?.signature || '';
        const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id);
        if (Number.isFinite(userId) && userId > 0) {
            return `${urls.API_URL}getSignature?userId=${userId}`;
        }

        if (typeof signaturePath === 'string' && (signaturePath.startsWith('data:') || /^https?:\/\//i.test(signaturePath))) {
            return signaturePath;
        }

        return '';
    }

    getDynamicSignatureLabel(slot: any): string {
        return this.getUserName(slot) || slot?.label || slot?.name || 'Approver';
    }

    getDynamicSignatureFallbackLabel(field: TemplateField): string {
        const step = this.getDynamicSignatureStep(field);
        return step?.name || 'Dynamic Signatures';
    }

    async submitTemplate(): Promise<void> {
        if (!this.savedTemplate) {
            return;
        }
        (this.template?.fields || []).forEach((field: TemplateField) => {
            if (field.type === 'word_editor' && field.placement) {
                this.values[field.id] = this.acceptedWordEditorValues[field.id] || '';
            }
        });
        const allowedValues = this.getAllowedSubmissionValues();

        try {
            const userPipeline = this.hasIndividualPipelineFooter() ? this.getNormalizedUserPipeline() : [];
            const submission = await firstValueFrom(this.templateWorkflowService.submitTemplate(
                this.savedTemplate,
                allowedValues,
                userPipeline,
                this.generatedApplicationCode
            ));
            this.submittedCode = submission.code || this.generatedApplicationCode;
            this.generatedApplicationCode = this.submittedCode;
            this.initiatorSignatureApproved = true;

            const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.submittedCode || submission.id}.pdf`;
            const pdfBlob = await this.renderTemplatePreviewPdfBlob();
            const pdfResponse: any = await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(
                submission.id,
                pdfBlob,
                filename
            ));
            if (!pdfResponse || pdfResponse.status !== 'Success') {
                throw new Error(pdfResponse?.message || 'PDF could not be attached to application');
            }

            const emailResponse: any = await firstValueFrom(this.templateWorkflowService.sendTemplateApplicationEmails(submission.id));
            if (!emailResponse || emailResponse.status !== 'Success') {
                throw new Error(emailResponse?.message || 'Approval email could not be sent');
            }
            this.notificationService.showMessage('Template application submitted successfully.', 'success');
        } catch (error: any) {
            this.notificationService.showMessage(error?.message || 'Template application could not be submitted.', 'danger');
        }
    }

    async sendTestEmailToAdmin(): Promise<void> {
        if (!this.template || this.isSendingTestEmail) {
            return;
        }

        const recipients = this.getAdminEmails();
        if (recipients.length === 0) {
            this.testEmailMessage = 'No admin user email found.';
            this.notificationService.showMessage(this.testEmailMessage, 'warning');
            return;
        }

            this.isSendingTestEmail = true;
        this.testEmailMessage = '';
        try {
            const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}.pdf`;
            const pdfBlob = await this.renderTemplatePreviewPdfBlob();
            const response: any = await firstValueFrom(this.templateWorkflowService.sendTemplateTestEmailPdf(
                recipients,
                `Template test email - ${this.template?.name || 'Template Form'}`,
                this.buildTemplateEmailBodyHtml(),
                pdfBlob,
                filename
            ));

            if (response?.status === 'Success') {
                this.testEmailMessage = `Test email sent to ${recipients.join(', ')}`;
                this.notificationService.showMessage(this.testEmailMessage, 'success');
            } else {
                this.testEmailMessage = response?.message || 'Test email could not be sent.';
                this.notificationService.showMessage(this.testEmailMessage, 'danger');
            }
        } catch (error: any) {
            this.testEmailMessage = error?.error?.message || error?.message || 'Test email could not be sent.';
            this.notificationService.showMessage(this.testEmailMessage, 'danger');
        } finally {
            this.isSendingTestEmail = false;
        }
    }

    private getAllowedSubmissionValues(): { [fieldId: string]: any } {
        return (this.template?.fields || []).reduce((acc: { [fieldId: string]: any }, field: TemplateField) => {
            if (field.type === 'application_code') {
                acc[field.id] = this.generatedApplicationCode;
                return acc;
            }

            if (this.canCurrentStepFillField(field)) {
                acc[field.id] = this.values[field.id];
            }
            return acc;
        }, {});
    }

    private getSessionTemplatePreviewCode(): string {
        const convention = this.templateWorkflowService.parseCodeConvention(this.template?.codeConvention || 'FORM-0000');
        const prefix = String(this.template?.codePrefix || convention.prefix);
        const serialLength = Number(this.template?.serialLength || convention.serialLength);
        return `${prefix}-${String(1).padStart(serialLength, '0')}`;
    }

    private getAdminEmails(): string[] {
        return (this.allUsers || [])
            .filter((user) => this.isAdminUser(user))
            .map((user) => String(user?.txtAddress || user?.email || '').trim())
            .filter((email, index, emails) => !!email && emails.indexOf(email) === index);
    }

    private isAdminUser(user: any): boolean {
        const role = (
            user?.cfgTblRole?.txtRoleName ||
            user?.cfgTblRole?.txtRoleCode ||
            user?.txtrole ||
            user?.roleName ||
            ''
        ).toString().trim().toUpperCase();
        return role === 'ADMIN' || role === 'ROLE_ADMIN' || role.includes('ADMIN');
    }

    private buildTemplateEmailBodyHtml(): string {
        return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <style>
    body { margin: 0; padding: 20px; background: #f8fafc; font-family: Arial, sans-serif; color: #111827; }
    .email-shell { max-width: 680px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; }
    h2 { font-size: 18px; margin: 0 0 8px; }
    p { color: #475569; font-size: 13px; line-height: 1.5; margin: 0; }
  </style>
</head>
<body>
  <div class="email-shell">
    <h2>${this.escapeHtml(this.template?.name || 'Template Form')}</h2>
    <p>Attached is the exact filled template form PDF generated from the template-fill preview.</p>
  </div>
</body>
</html>`;
    }

    private async renderTemplatePreviewPdfBlob(): Promise<Blob> {
        const sourceFrame = this.elementRef.nativeElement.querySelector('.fill-frame') as HTMLElement | null;
        if (!sourceFrame) {
            throw new Error('Template preview not found');
        }

        if ((document as any).fonts?.ready) {
            try {
                await (document as any).fonts.ready;
            } catch {
                // Best effort only.
            }
        }

        const [html2canvasModule, jsPDFModule] = await Promise.all([
            import('html2canvas'),
            import('jspdf')
        ]);
        const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
        const jsPDF = (jsPDFModule.default || jsPDFModule) as any;
        const pageWidth = Number(this.template?.page?.width || 794);
        const pageHeight = Number(this.template?.page?.height || 1123);
        const pageGap = Number(this.template?.page?.gap || 32);
        const pageCount = Number(this.template?.page?.count || 1);
        const orientation = pageWidth > pageHeight ? 'landscape' : 'portrait';
        const pdfWidthMm = orientation === 'landscape' ? 297 : 210;
        const pdfHeightMm = orientation === 'landscape' ? 210 : 297;
        const pdf = new jsPDF({
            orientation,
            unit: 'mm',
            format: 'a4',
            compress: true
        });

        const captureHost = document.createElement('div');
        captureHost.style.position = 'fixed';
        captureHost.style.left = '-100000px';
        captureHost.style.top = '0';
        captureHost.style.opacity = '0';
        captureHost.style.pointerEvents = 'none';
        captureHost.style.zIndex = '-1';
        document.body.appendChild(captureHost);

        try {
            for (let pageIndex = 0; pageIndex < pageCount; pageIndex += 1) {
                const pageViewport = document.createElement('div');
                pageViewport.style.background = '#ffffff';
                pageViewport.style.height = `${pageHeight}px`;
                pageViewport.style.overflow = 'hidden';
                pageViewport.style.position = 'relative';
                pageViewport.style.width = `${pageWidth}px`;

                const clone = sourceFrame.cloneNode(true) as HTMLElement;
                clone.querySelectorAll('.page-boundary span').forEach((node) => node.remove());
                clone.style.boxShadow = 'none';
                clone.style.left = '0';
                clone.style.margin = '0';
                clone.style.position = 'absolute';
                clone.style.top = `${-pageIndex * (pageHeight + pageGap)}px`;
                clone.style.transform = 'none';
                clone.style.width = `${pageWidth}px`;
                this.prepareTemplatePdfFieldText(clone);
                pageViewport.appendChild(clone);
                captureHost.appendChild(pageViewport);

                await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)));
                const canvas = await html2canvas(pageViewport, {
                    scale: 3,
                    useCORS: true,
                    logging: false,
                    backgroundColor: '#ffffff',
                    width: pageWidth,
                    height: pageHeight,
                    windowWidth: pageWidth,
                    windowHeight: pageHeight
                });

                if (pageIndex > 0) {
                    pdf.addPage('a4', orientation);
                }
                pdf.addImage(canvas.toDataURL('image/jpeg', 1), 'JPEG', 0, 0, pdfWidthMm, pdfHeightMm);
                captureHost.removeChild(pageViewport);
            }

            return pdf.output('blob');
        } finally {
            if (captureHost.parentNode) {
                captureHost.parentNode.removeChild(captureHost);
            }
        }
    }

    private sanitizeFilename(value: string): string {
        return String(value || 'template-form')
            .trim()
            .replace(/[^a-z0-9-_]+/gi, '-')
            .replace(/^-+|-+$/g, '')
            || 'template-form';
    }

    private prepareTemplatePdfFieldText(root: HTMLElement): void {
        root.querySelectorAll('.filled-field-value').forEach((node) => {
            const valueEl = node as HTMLElement;
            if (!valueEl || valueEl.classList.contains('word-editor-filled-value')) {
                return;
            }

            valueEl.style.setProperty('box-sizing', 'border-box', 'important');
            valueEl.style.setProperty('display', 'block', 'important');
            valueEl.style.setProperty('height', '100%', 'important');
            valueEl.style.setProperty('line-height', '1.05', 'important');
            valueEl.style.setProperty('overflow', 'hidden', 'important');
            valueEl.style.setProperty('padding', '0', 'important');
            valueEl.style.setProperty('transform', 'translateY(1px)', 'important');
            valueEl.style.setProperty('white-space', 'pre-wrap', 'important');
        });
    }

    private getNormalizedUserPipeline(): IndividualPipelineFooterSection[] {
        return this.userPipeline.map((section, index) => ({
            key: section.key || `wf_${index + 1}`,
            label: section.label || 'New Field',
            order: index + 1,
            users: Array.isArray(section.users) ? section.users.map((user) => this.normalizePipelineUser(user)) : []
        }));
    }

    private normalizePipelineUser(user: any): any {
        if (!user || typeof user !== 'object') {
            return user;
        }

        return {
            serUserId: user.serUserId ?? user.userId ?? user.id ?? null,
            txtUserName: user.txtUserName || user.userName || user.name || '',
            txtSignaturePath: user.txtSignaturePath || '',
            txtDepartmentName: user.txtDepartmentName || user.departmentName || user.hrTblDepartment?.txtDepartmentName || '',
            txtDesignation: user.txtDesignation || user.designation || '',
            cfgTblRole: user.cfgTblRole && typeof user.cfgTblRole === 'object'
                ? {
                    serRoleId: user.cfgTblRole.serRoleId ?? null,
                    txtRoleName: user.cfgTblRole.txtRoleName || ''
                }
                : null
        };
    }

    private getIndividualFooterUserParts(user: any): string[] {
        if (!user) {
            return [];
        }

        const name = user.txtUserName || user.userName || user.name || '';
        const designation = user.txtDesignation || user.designation || '';
        const department = user.txtDepartmentName || user.departmentName || user.hrTblDepartment?.txtDepartmentName || '';
        return [name, designation, department]
            .map((value: string) => String(value || '').trim())
            .filter((value: string) => value.length > 0);
    }

    private escapeHtml(value: any): string {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    private getFillHtml(html: string): string {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const fields = new Map<string, TemplateField>(
            (this.template.fields || []).map((field: TemplateField) => [field.id, field])
        );

        doc.querySelectorAll('.template-field[data-field-id]').forEach((node) => {
            const fieldId = node.getAttribute('data-field-id') || '';
            const field = fields.get(fieldId);
            if (!field || field.placement || this.isDocumentRegionFieldType(field.type)) {
                return;
            }

            if (this.isFieldHiddenForCurrentStep(field)) {
                node.remove();
                return;
            }

            node.replaceWith(this.createInlineValue(doc, field));
        });

        return doc.body.innerHTML;
    }

    private getCurrentStepFieldRight(field: TemplateField): PipelineFieldRight | 'view' {
        const step = this.getCurrentPipelineStep();
        if (!step) {
            return 'fill';
        }

        if (step.type === 'initiator' && !step.fieldPermissionsConfigured) {
            return 'fill';
        }

        const permission = (step.fieldPermissions || []).find((item) => item.fieldId === field.id);
        return permission?.right || 'view';
    }

    private getCurrentPipelineStep(): PipelineStep | null {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline as PipelineStep[] : [];
        return steps.find((step) => step?.type === 'initiator') || steps[0] || null;
    }

    private getInitiatorSignatureSlots(): any[] {
        if (!this.initiatorSignatureApproved) {
            return [{ label: 'Initiator' }];
        }
        const userId = this.getCurrentUserId();
        return userId ? [{
            serUserId: userId,
            txtUserName: 'Initiator',
            __signatureApproved: true
        }] : [{ label: 'Initiator' }];
    }

    private getCurrentUserId(): number | null {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            const userId = Number(user?.serUserId ?? user?.userId ?? user?.id);
            return Number.isFinite(userId) && userId > 0 ? userId : null;
        } catch {
            return null;
        }
    }

    private getDynamicSignatureStep(field: TemplateField): any | null {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline : [];
        const targetId = field.signatureTargetId || field.pipelineStepId;
        return steps.find((step: any) => step?.id === targetId) || null;
    }

    private getConfiguredSignatureSlots(step: any): any[] {
        const users = Array.isArray(step.users) ? step.users : [];
        if (users.length > 0) {
            return users;
        }

        if (step.dynamicTarget === 'initiator' || (Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator'))) {
            return [{ label: 'Initiator' }];
        }

        return [{ label: step.name || this.getPipelineTargetFallbackLabel(step) }];
    }

    private getApprovedSignatureSlots(step: any): any[] {
        const approvalSources = [
            { source: step.approvedUsers, trustedApproved: true },
            { source: step.approvers, trustedApproved: false },
            { source: step.approvals, trustedApproved: false },
            { source: step.approvalHistory, trustedApproved: false },
            { source: step.history, trustedApproved: false },
            { source: step.signatureUsers, trustedApproved: false },
            { source: step.signatures, trustedApproved: false }
        ];

        return approvalSources
            .filter((item) => Array.isArray(item.source))
            .flatMap((item) => item.source.map((entry: any) => ({
                ...this.normalizeSignatureEntry(entry),
                __signatureApproved: item.trustedApproved || this.signatureEntryIsApproved(entry)
            })))
            .filter((entry: any) => !!entry && entry.__signatureApproved)
            .filter((entry: any) => this.signatureEntryMatchesStep(entry, step))
            .filter((entry: any) => !!this.getDynamicSignatureImageUrl(entry) || !!this.getDynamicSignatureLabel(entry));
    }

    private mergeSignatureSlots(configuredSlots: any[], approvedSlots: any[]): any[] {
        if (approvedSlots.length === 0) {
            return configuredSlots;
        }

        const approvedById = new Map(
            approvedSlots
                .map((slot) => [Number(slot?.serUserId ?? slot?.userId ?? slot?.id), slot] as [number, any])
                .filter(([userId]) => Number.isFinite(userId) && userId > 0)
        );

        return configuredSlots.map((slot) => {
            const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.id);
            return approvedById.get(userId) || slot;
        });
    }

    private normalizeSignatureEntry(entry: any): any {
        if (!entry || typeof entry !== 'object') {
            return null;
        }

        const user = entry.user || entry.approver || entry.employee || {};
        return {
            ...user,
            ...entry,
            serUserId: entry.serUserId ?? entry.userId ?? entry.approvedBy ?? entry.approverId ?? user.serUserId ?? user.userId ?? user.id ?? null,
            txtUserName: entry.txtUserName || entry.userName || entry.approverName || user.txtUserName || user.userName || user.name || '',
            txtSignaturePath: entry.txtSignaturePath || entry.signaturePath || entry.signature || user.txtSignaturePath || user.signaturePath || '',
            action: entry.action || entry.status || '',
            __signatureApproved: false
        };
    }

    private signatureEntryIsApproved(entry: any): boolean {
        const action = String(entry?.action || '').toUpperCase();
        return action === 'APPROVED';
    }

    private signatureEntryMatchesStep(entry: any, step: any): boolean {
        const entryStepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim();
        const stepId = String(step?.id ?? '').trim();
        if (entryStepId && stepId) {
            return entryStepId === stepId;
        }

        const entryLevel = Number(entry?.intApprovalOrder ?? entry?.level);
        if (!Number.isFinite(entryLevel)) {
            return true;
        }
        const expectedLevel = this.getSignatureStepHistoryLevel(step);
        return Number.isFinite(expectedLevel) && [expectedLevel, expectedLevel - 1, expectedLevel - 2].includes(entryLevel);
    }

    private getSignatureStepHistoryLevel(step: any): number {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline : [];
        const stepIndex = steps.findIndex((item: any) => item?.id === step?.id);
        const hasInitiator = steps.some((item: any) => item?.type === 'initiator');
        const order = Number(step?.order);
        if (Number.isFinite(order) && order > 0) {
            return hasInitiator ? order + 1 : order;
        }
        return hasInitiator ? stepIndex + 1 : stepIndex + 2;
    }

    private getPipelineTargetFallbackLabel(step: any): string {
        if (step?.type === 'department') {
            return step?.hrTblDepartment?.txtDepartmentName || step?.name || 'Department';
        }

        if (step?.type === 'role') {
            return step?.cfgTblRole?.txtRoleName || step?.name || 'Role';
        }

        return step?.name || 'Approver';
    }

    private createInlineValue(doc: Document, field: TemplateField): HTMLElement {
        const span = doc.createElement('span');
        span.className = `inline-filled-value${field.type === 'textarea' ? ' inline-filled-textarea-value' : ''}${field.type === 'word_editor' ? ' inline-filled-word-editor-value' : ''}`;
        span.dataset['fieldId'] = field.id;
        if (field.type === 'word_editor') {
            span.innerHTML = String(this.values[field.id] || '');
        } else {
            span.textContent = this.getFieldValueText(field);
        }
        this.applyInlineValueAttributes(span, field);
        return span;
    }

    private applyInlineValueAttributes(control: HTMLElement, field: TemplateField): void {
        control.style.fontSize = `${field.style?.fontSize || 14}px`;
        control.style.fontWeight = field.style?.bold ? '700' : '400';
        control.style.fontStyle = field.style?.italic ? 'italic' : 'normal';
        control.style.textDecoration = field.style?.underline ? 'underline' : 'none';
        control.style.textAlign = field.style?.textAlign || 'left';
    }

    private getHtmlInputType(type: TemplateFieldType): string {
        if (type === 'integer' || type === 'decimal' || type === 'number') {
            return 'number';
        }

        if (type === 'email' || type === 'date') {
            return type;
        }

        return 'text';
    }

    private normalizeRadioValue(value: any): string {
        return String(value ?? '').trim().toLowerCase();
    }

    private loadUsers(): void {
        this.userService.getUsers().subscribe({
            next: (data: any) => {
                this.allUsers = Array.isArray(data) ? data : [];
            },
            error: () => {
                this.allUsers = [];
            }
        });
    }

    private doesWordEditorFieldOverflow(fieldId: string): boolean {
        const fieldElement = this.elementRef.nativeElement.querySelector(
            `.filled-field[data-field-id="${fieldId}"] .word-editor-filled-value`
        ) as HTMLElement | null;
        if (!fieldElement) {
            return false;
        }

        return fieldElement.scrollHeight > fieldElement.clientHeight + 1
            || fieldElement.scrollWidth > fieldElement.clientWidth + 1;
    }
}
