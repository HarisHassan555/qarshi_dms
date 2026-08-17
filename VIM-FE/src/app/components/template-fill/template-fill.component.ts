import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
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
import { stripEditorTableChromeFromHtml } from 'src/app/utils/word-editor-table.util';
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
    | 'dynamic_approver_name'
    | 'dynamic_approval_timestamp'
    | 'dynamic_approver_department'
    | 'dynamic_approver_designation'
    | 'dynamic_user_details'
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
        fontFamily: string;
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

interface AttachmentPayload {
    fileName: string;
    mimeType: string;
    dataUrl: string;
    base64: string;
}

interface AttachmentListItem {
    fieldId: string;
    fieldLabel: string;
    attachmentIndex: number;
    attachment: AttachmentPayload;
}

interface PipelineStep {
    id: string;
    name: string;
    type: string;
    fieldPermissions?: PipelineFieldPermission[];
    fieldPermissionsConfigured?: boolean;
}

const TEMPLATE_FILL_HIDE_CKEDITOR_BADGE_CLASS = 'template-fill-hide-ckeditor-badge';

@Component({
    selector: 'app-template-fill',
    templateUrl: './template-fill.component.html',
    styleUrls: ['./template-fill.component.css']
})
export class TemplateFillComponent implements OnInit, OnDestroy {
    private static readonly MAX_ATTACHMENT_TOTAL_BYTES = 5 * 1024 * 1024;
    private static readonly ALLOWED_ATTACHMENT_MIME_TYPES = new Set([
        'application/pdf',
        'image/webp',
        'image/png',
        'image/jpeg'
    ]);
    private static readonly ALLOWED_ATTACHMENT_EXTENSIONS = new Set(['pdf', 'webp', 'png', 'jpeg', 'jpg']);
    private static readonly INDIVIDUAL_FOOTER_MIN_HEIGHT = 136;
    template: any = null;
    savedTemplate: SavedTemplateDefinition | null = null;
    editingApplication: any = null;
    safeHtml: SafeHtml = '';
    values: { [fieldId: string]: any } = {};
    persistedValues: { [fieldId: string]: any } = {};
    allUsers: any[] = [];
    userPipeline: IndividualPipelineFooterSection[] = [];
    submittedCode = '';
    generatedApplicationCode = '';
    isSendingTestEmail = false;
    testEmailMessage = '';
    isSubmitting = false;
    showAttachmentsModal = false;
    activeInlineWordEditorFieldId: string | null = null;
    readonly wordEditor = WORD_EDITOR_CKEDITOR;
    readonly wordEditorConfig = WORD_EDITOR_CKEDITOR_CONFIG;
    private initiatorSignatureApproved = false;
    private acceptedWordEditorValues: { [fieldId: string]: string } = {};
    private revertingWordEditorFields = new Set<string>();
    private inlineWordEditors = new Map<string, any>();
    private activeToolbarElement: HTMLElement | null = null;
    private activeToolbarOriginParent: HTMLElement | null = null;
    private activeToolbarOriginNextSibling: ChildNode | null = null;

    constructor(
        private elementRef: ElementRef<HTMLElement>,
        private route: ActivatedRoute,
        private router: Router,
        private sanitizer: DomSanitizer,
        private notificationService: NotificationService,
        private templateWorkflowService: TemplateWorkflowService,
        private userService: UserService
    ) { }

    async ngOnInit(): Promise<void> {
        document.body.classList.add(TEMPLATE_FILL_HIDE_CKEDITOR_BADGE_CLASS);
        this.loadUsers();
        const templateId = this.route.snapshot.paramMap.get('id');
        const applicationId = Number(this.route.snapshot.queryParamMap.get('applicationId'));
        try {
            if (Number.isFinite(applicationId) && applicationId > 0) {
                await this.loadEditableApplication(applicationId, templateId);
            } else if (templateId) {
                this.savedTemplate = await firstValueFrom(this.templateWorkflowService.getTemplate(templateId));
                this.template = this.savedTemplate?.payload || null;
            } else {
                const rawTemplate = sessionStorage.getItem('templateBuilder:lastTemplate');
                this.template = rawTemplate ? JSON.parse(rawTemplate) : null;
            }

            if (!this.template) {
                return;
            }

            if (!this.editingApplication) {
                this.generatedApplicationCode = this.savedTemplate
                    ? await firstValueFrom(this.templateWorkflowService.peekNextTemplateCode(this.savedTemplate))
                    : this.getSessionTemplatePreviewCode();
            }
        } catch (error) {
            console.error('Template load failed', error);
            this.template = null;
            return;
        }

        (this.template.fields || []).forEach((field: TemplateField) => {
            const existingValue = this.values[field.id];
            this.values[field.id] = existingValue !== undefined
                ? existingValue
                : (field.type === 'application_code'
                    ? this.generatedApplicationCode
                    : (field.type === 'checkbox' ? false : ''));
            if (this.isAttachmentField(field)) {
                this.values[field.id] = this.normalizeExistingAttachmentPayloads(this.values[field.id]);
            }
            this.persistedValues[field.id] = this.values[field.id];
            if (field.type === 'word_editor') {
                this.acceptedWordEditorValues[field.id] = String(this.values[field.id] || '');
            }
        });
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.template.html || ''));
    }

    get isEditingExistingApplication(): boolean {
        return !!this.editingApplication?.serApplicationId;
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

    get panelFields(): TemplateField[] {
        return this.fillableFields.filter((field: TemplateField) => !this.isInlineWordEditorField(field));
    }

    getActiveInlineWordEditorField(): TemplateField | null {
        const activeFieldId = this.activeInlineWordEditorFieldId;
        if (!activeFieldId) {
            return null;
        }
        return (this.template?.fields || []).find((field: TemplateField) => field.id === activeFieldId) || null;
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

    getRenderedFieldTop(field: TemplateField): number | null {
        const placement = field?.placement;
        if (!placement) {
            return null;
        }
        if (field.type !== 'individual_pipeline_footer') {
            return placement.y;
        }
        const height = Number(placement.height || 0);
        const renderedHeight = this.getRenderedFieldHeight(field) ?? height;
        const extraHeight = Math.max(0, renderedHeight - height);
        return Math.max(0, Number(placement.y || 0) - extraHeight);
    }

    getRenderedFieldHeight(field: TemplateField): number | null {
        const placement = field?.placement;
        if (!placement) {
            return null;
        }
        const height = Number(placement.height || 0);
        if (field.type !== 'individual_pipeline_footer') {
            return height;
        }
        return Math.max(height, TemplateFillComponent.INDIVIDUAL_FOOTER_MIN_HEIGHT);
    }

    isDocumentRegionFieldType(type: TemplateFieldType): boolean {
        return this.isHeaderFieldType(type) || this.isFooterFieldType(type);
    }

    private hasIndividualFooterPipeline(): boolean {
        return (this.template?.fields || []).some((field: TemplateField) => field.type === 'individual_pipeline_footer');
    }

    canCurrentStepFillField(field: TemplateField): boolean {
        if (field.type === 'application_code' || this.isDynamicApprovalDataField(field)) {
            return false;
        }

        const right = this.getCurrentStepFieldRight(field);
        if (right === 'edit') {
            return true;
        }
        if (right === 'fill') {
            return this.isFieldFillAvailable(field);
        }
        return false;
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
            'font-family': field.style?.fontFamily || 'Arial, sans-serif',
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

        if (field.type === 'attachment') {
            return this.getAttachmentDisplayText(field);
        }

        if (field.type === 'dynamic_approver_name'
            || field.type === 'dynamic_approval_timestamp'
            || field.type === 'dynamic_approver_department'
            || field.type === 'dynamic_approver_designation'
            || field.type === 'dynamic_user_details') {
            return this.getDynamicApprovalDisplayText(field);
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

    isInlineWordEditorField(field: TemplateField): boolean {
        return field.type === 'word_editor' && !!field.placement;
    }

    isInlineWordEditorActive(field: TemplateField): boolean {
        return this.activeInlineWordEditorFieldId === field.id;
    }

    activateInlineWordEditor(field: TemplateField): void {
        if (!this.isInlineWordEditorField(field)) {
            return;
        }
        this.activeInlineWordEditorFieldId = field.id;
        this.attachInlineWordEditorToolbar(field.id);
    }

    onInlineWordEditorReady(field: TemplateField, editor: any): void {
        if (!this.isInlineWordEditorField(field)) {
            return;
        }
        this.inlineWordEditors.set(field.id, editor);
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        if (editable) {
            editable.setAttribute('data-inline-word-editor-field-id', field.id);
        }
        if (this.activeInlineWordEditorFieldId === field.id) {
            this.attachInlineWordEditorToolbar(field.id);
        }
    }

    ngOnDestroy(): void {
        document.body.classList.remove(TEMPLATE_FILL_HIDE_CKEDITOR_BADGE_CLASS);
    }

    onInlineWordEditorFocus(field: TemplateField): void {
        this.activateInlineWordEditor(field);
    }

    isAttachmentField(field: TemplateField): boolean {
        return field.type === 'attachment';
    }

    getAttachmentPayloads(field: TemplateField): AttachmentPayload[] {
        const currentValue = this.values[field.id];
        return Array.isArray(currentValue) ? currentValue : [];
    }

    getAttachmentDisplayText(field: TemplateField): string {
        const attachments = this.getAttachmentPayloads(field);
        return attachments.length > 0 ? attachments.map((attachment) => attachment.fileName).join(', ') : '';
    }

    get hasAnyAttachments(): boolean {
        return this.getAllAttachmentItems().length > 0;
    }

    getAllAttachmentItems(): AttachmentListItem[] {
        return (this.template?.fields || []).reduce((items: AttachmentListItem[], field: TemplateField) => {
            if (!this.isAttachmentField(field)) {
                return items;
            }
            this.getAttachmentPayloads(field).forEach((attachment, attachmentIndex) => {
                items.push({
                    fieldId: field.id,
                    fieldLabel: field.label || 'Attachment',
                    attachmentIndex,
                    attachment
                });
            });
            return items;
        }, []);
    }

    async onAttachmentChange(field: TemplateField, event: Event): Promise<void> {
        const input = event.target as HTMLInputElement;
        const files = input?.files ? Array.from(input.files) : [];
        if (!files.length) {
            input.value = '';
            return;
        }

        const disallowed = files.filter((file) => !this.isAllowedAttachmentFile(file));
        if (disallowed.length > 0) {
            this.notificationService.showMessage('Only PDF, WEBP, PNG, and JPEG files are allowed for attachments.', 'danger');
            input.value = '';
            return;
        }

        const currentPayloads = this.getAttachmentPayloads(field);
        const totalBytes = this.getAttachmentBytes(currentPayloads)
            + files.reduce((sum, file) => sum + (Number(file.size) || 0), 0);
        if (totalBytes > TemplateFillComponent.MAX_ATTACHMENT_TOTAL_BYTES) {
            this.notificationService.showMessage(
                `Combined attachment size (${(totalBytes / (1024 * 1024)).toFixed(2)} MB) exceeds 5 MB.`,
                'danger'
            );
            input.value = '';
            return;
        }

        try {
            const payloads = await Promise.all(files.map((file) => this.buildAttachmentPayload(file)));
            this.setAttachmentPayloads(field, [...currentPayloads, ...payloads]);
            this.onPanelValueChanged();
        } catch (error) {
            console.error('Failed generating attachment payload', error);
            this.notificationService.showMessage('Failed to process selected files.', 'danger');
        } finally {
            input.value = '';
        }
    }

    removeAttachment(field: TemplateField, indexToRemove: number, event?: Event): void {
        event?.preventDefault();
        event?.stopPropagation();

        const attachments = this.getAttachmentPayloads(field);
        if (indexToRemove < 0 || indexToRemove >= attachments.length) {
            return;
        }
        this.setAttachmentPayloads(field, attachments.filter((_, index) => index !== indexToRemove));
        if (!this.hasAnyAttachments) {
            this.showAttachmentsModal = false;
        }
        this.onPanelValueChanged();
    }

    removeAttachmentByFieldId(fieldId: string, indexToRemove: number, event?: Event): void {
        event?.preventDefault();
        event?.stopPropagation();

        const field = (this.template?.fields || []).find((templateField: TemplateField) => templateField.id === fieldId);
        if (!field || !this.isAttachmentField(field)) {
            return;
        }
        this.removeAttachment(field, indexToRemove);
    }

    viewAttachment(attachment: AttachmentPayload): void {
        const base64 = String(attachment?.base64 || '').trim();
        const mimeType = String(attachment?.mimeType || 'application/octet-stream').trim();
        const objectUrl = base64 ? this.createAttachmentObjectUrl(base64, mimeType) : '';
        const dataUrl = String(attachment?.dataUrl || '').trim();
        const resolvedUrl = objectUrl || dataUrl;
        if (!resolvedUrl) {
            this.notificationService.showMessage('Attachment content is unavailable.', 'warning');
            return;
        }
        window.open(resolvedUrl, '_blank', 'noopener');
        if (objectUrl) {
            setTimeout(() => URL.revokeObjectURL(objectUrl), 60000);
        }
    }

    openAttachmentsModal(): void {
        if (!this.hasAnyAttachments) {
            return;
        }
        this.showAttachmentsModal = true;
    }

    closeAttachmentsModal(): void {
        this.showAttachmentsModal = false;
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

    @HostListener('document:mousedown', ['$event'])
    onDocumentMouseDown(event: MouseEvent): void {
        const target = event.target as HTMLElement | null;
        if (!target) {
            return;
        }
        if (
            target.closest('.inline-word-editor-shell')
            || target.closest('.inline-word-editor-toolbar-panel')
            || target.closest('.ck.ck-balloon-panel')
            || target.closest('.ck-body-wrapper')
        ) {
            return;
        }
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

    isDynamicApprovalDataField(field: TemplateField): boolean {
        return this.isDynamicSignatureField(field)
            || field.type === 'dynamic_approver_name'
            || field.type === 'dynamic_approval_timestamp'
            || field.type === 'dynamic_approver_department'
            || field.type === 'dynamic_approver_designation'
            || field.type === 'dynamic_user_details';
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
            if (approvedSlots.length > 0 && !this.signatureSlotsHaveConcreteUsers(configuredSlots)) {
                return approvedSlots;
            }
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

    getDynamicApprovalDisplayText(field: TemplateField): string {
        const values = this.getDynamicSignatureSlots(field)
            .map((slot) => this.getDynamicApprovalSlotValue(field, slot))
            .filter((value) => !!value);
        if (values.length > 0) {
            return values.join('\n');
        }
        return '';
    }

    async submitTemplate(): Promise<void> {
        if (!this.savedTemplate || this.isSubmitting) {
            return;
        }
        (this.template?.fields || []).forEach((field: TemplateField) => {
            if (field.type === 'word_editor' && field.placement) {
                this.values[field.id] = this.acceptedWordEditorValues[field.id] || '';
            }
        });

        const missingRequiredFields = this.getMissingRequiredFields();
        if (missingRequiredFields.length > 0) {
            const fieldSummary = missingRequiredFields.slice(0, 3).join(', ');
            const remainingCount = missingRequiredFields.length - 3;
            const suffix = remainingCount > 0 ? ` and ${remainingCount} more` : '';
            this.notificationService.showMessage(
                `Please complete all required fields before submitting. Missing: ${fieldSummary}${suffix}.`,
                'danger'
            );
            return;
        }

        this.isSubmitting = true;
        const allowedValues = this.getAllowedSubmissionValues();

        try {
            const userPipeline = this.hasIndividualPipelineFooter() ? this.getNormalizedUserPipeline() : [];
            if (this.isEditingExistingApplication) {
                await firstValueFrom(this.templateWorkflowService.updateTemplateApplication(
                    this.editingApplication,
                    allowedValues,
                    this.savedTemplate.payload,
                    userPipeline
                ));
                const resubmitResponse = await firstValueFrom(this.templateWorkflowService.resubmitTemplateApplicationFromInitiator(
                    this.editingApplication.serApplicationId,
                    'Resubmitted by initiator after revision',
                    this.getCurrentUserId()
                ));
                if (!resubmitResponse || resubmitResponse.status !== 'Success') {
                    throw new Error(resubmitResponse?.message || 'Application could not be resubmitted');
                }

                this.submittedCode = this.editingApplication.txtFormCode || this.generatedApplicationCode;
                this.generatedApplicationCode = this.submittedCode;
                this.initiatorSignatureApproved = true;

                const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.submittedCode || this.editingApplication.serApplicationId}.pdf`;
                const pdfBlob = await this.renderTemplatePreviewPdfBlob();
                const pdfResponse: any = await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(
                    this.editingApplication.serApplicationId,
                    pdfBlob,
                    filename
                ));
                if (!pdfResponse || pdfResponse.status !== 'Success') {
                    throw new Error(pdfResponse?.message || 'PDF could not be attached to application');
                }

                const emailResponse: any = await firstValueFrom(this.templateWorkflowService.sendTemplatePostApprovalEmails(
                    this.editingApplication.serApplicationId,
                    pdfBlob,
                    pdfBlob,
                    filename
                ));
                if (!emailResponse || emailResponse.status !== 'Success') {
                    throw new Error(emailResponse?.message || 'Approval email could not be sent');
                }

                this.notificationService.showMessage('Application updated and resubmitted successfully.', 'success');
                this.router.navigate(['/my-application', this.editingApplication.serApplicationId]);
                return;
            }

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

            const emailResponse: any = await firstValueFrom(this.templateWorkflowService.sendTemplatePostApprovalEmails(
                submission.id,
                pdfBlob,
                pdfBlob,
                filename
            ));
            if (!emailResponse || emailResponse.status !== 'Success') {
                throw new Error(emailResponse?.message || 'Approval email could not be sent');
            }
            this.notificationService.showMessage('Template application submitted successfully.', 'success');
            this.router.navigate(['/template-list']);
        } catch (error: any) {
            this.notificationService.showMessage(error?.message || 'Template application could not be submitted.', 'danger');
        } finally {
            this.isSubmitting = false;
        }
    }

    private async loadEditableApplication(applicationId: number, fallbackTemplateId: string | null): Promise<void> {
        const application = await firstValueFrom(this.templateWorkflowService.getApplication(applicationId));
        const currentUserId = this.getCurrentUserId();
        const submittedBy = Number(application?.serSubmittedBy || 0);
        const currentLevel = Number(application?.intCurrentApprovalLevel);
        const status = String(application?.txtStatus || '').toUpperCase();
        const canEdit = currentUserId != null
            && submittedBy === currentUserId
            && status === 'PENDING'
            && Number.isFinite(currentLevel)
            && currentLevel < 0;

        if (!canEdit) {
            throw new Error('This application is not available for initiator editing.');
        }

        const formId = String(application?.serFormId || fallbackTemplateId || '');
        this.savedTemplate = await firstValueFrom(this.templateWorkflowService.getTemplate(formId));
        this.template = this.savedTemplate?.payload || null;
        this.editingApplication = application;
        this.generatedApplicationCode = String(application?.txtFormCode || '');

        const applicationData = this.parseApplicationData(application?.txtApplicationData);
        const templateValues = applicationData?.templateValues || {};
        (this.template?.fields || []).forEach((field: TemplateField) => {
            const nextValue = templateValues[field.id] ?? applicationData[field.id];
            this.values[field.id] = nextValue !== undefined
                ? nextValue
                : (field.type === 'application_code' ? this.generatedApplicationCode : (field.type === 'checkbox' ? false : ''));
            if (this.isAttachmentField(field)) {
                this.values[field.id] = this.normalizeExistingAttachmentPayloads(this.values[field.id]);
            }
            this.persistedValues[field.id] = this.values[field.id];
        });
        const footerFields = Array.isArray(applicationData?.footerFields) ? applicationData.footerFields : [];
        this.userPipeline = footerFields.map((section: any, index: number) => ({
            key: section?.key || `wf_${index + 1}`,
            label: section?.label || 'New Field',
            order: Number(section?.order || index + 1),
            users: Array.isArray(section?.users) ? section.users : []
        }));
    }

    private parseApplicationData(raw: any): any {
        if (!raw) {
            return {};
        }
        if (typeof raw === 'object') {
            return raw;
        }
        try {
            return JSON.parse(String(raw));
        } catch {
            return {};
        }
    }

    private normalizeExistingAttachmentPayloads(value: any): AttachmentPayload[] {
        const list = Array.isArray(value) ? value : (value ? [value] : []);
        const normalized: AttachmentPayload[] = [];
        for (const item of list) {
            if (!item || typeof item !== 'object') {
                continue;
            }
            const fileName = String(item.fileName || item.name || '').trim();
            const mimeType = String(item.mimeType || item.type || '').trim() || 'application/octet-stream';
            const dataUrl = String(item.dataUrl || '').trim();
            const base64 = String(item.base64 || (dataUrl.includes(',') ? dataUrl.split(',', 2)[1] : '')).trim();
            if (!fileName) {
                continue;
            }
            normalized.push({ fileName, mimeType, dataUrl, base64 });
        }
        return normalized;
    }

    private setAttachmentPayloads(field: TemplateField, attachments: AttachmentPayload[]): void {
        this.values = {
            ...this.values,
            [field.id]: this.normalizeExistingAttachmentPayloads(attachments)
        };
    }

    private getAttachmentBytes(attachments: AttachmentPayload[]): number {
        return (attachments || []).reduce((sum, attachment) => {
            const base64 = String(attachment?.base64 || '').trim();
            return sum + (base64 ? Math.ceil((base64.length * 3) / 4) : 0);
        }, 0);
    }

    private isAllowedAttachmentFile(file: File): boolean {
        const mime = String(file?.type || '').trim().toLowerCase();
        if (mime && TemplateFillComponent.ALLOWED_ATTACHMENT_MIME_TYPES.has(mime)) {
            return true;
        }
        const name = String(file?.name || '').toLowerCase();
        const dotIndex = name.lastIndexOf('.');
        const ext = dotIndex >= 0 ? name.substring(dotIndex + 1) : '';
        return !!ext && TemplateFillComponent.ALLOWED_ATTACHMENT_EXTENSIONS.has(ext);
    }

    private async buildAttachmentPayload(file: File): Promise<AttachmentPayload> {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const dataUrl = String(reader.result || '');
                resolve({
                    fileName: file.name,
                    mimeType: file.type || 'application/octet-stream',
                    dataUrl: '',
                    base64: dataUrl.includes(',') ? dataUrl.split(',', 2)[1] : ''
                });
            };
            reader.onerror = () => reject(reader.error);
            reader.readAsDataURL(file);
        });
    }

    private createAttachmentObjectUrl(base64: string, mimeType: string): string {
        try {
            const binary = window.atob(base64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            return URL.createObjectURL(new Blob([bytes], { type: mimeType || 'application/octet-stream' }));
        } catch (error) {
            console.error('Failed to create attachment object URL', error);
            return '';
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

            const right = this.getCurrentStepFieldRight(field);
            if (right === 'edit' || (right === 'fill' && this.isFieldFillAvailable(field))) {
                acc[field.id] = this.values[field.id];
            }
            return acc;
        }, {});
    }

    private getMissingRequiredFields(): string[] {
        return (this.template?.fields || [])
            .filter((field: TemplateField) =>
                field.required
                && !this.isDocumentRegionFieldType(field.type)
                && !this.isDynamicApprovalDataField(field)
                && field.type !== 'application_code'
                && this.canCurrentStepFillField(field)
                && this.isFieldValueEmpty(field, this.values[field.id])
            )
            .map((field: TemplateField) => field.label || field.placeholder || 'Unnamed field');
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
                this.applyTemplateEmailFooterCaptureStyles(clone);
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
                pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 0, 0, pdfWidthMm, pdfHeightMm);
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
        root.querySelectorAll('.inline-word-editor-shell[data-field-id]').forEach((node) => {
            const shell = node as HTMLElement;
            const fieldId = String(shell.getAttribute('data-field-id') || '').trim();
            const field = (this.template?.fields || []).find((item: TemplateField) => item.id === fieldId);
            const staticValue = root.ownerDocument.createElement('div');
            staticValue.className = 'filled-field-value word-editor-filled-value pdf-word-editor-value';
            staticValue.innerHTML = normalizeWordEditorValueForCkeditor(this.values[fieldId] ?? '');
            staticValue.style.cssText = shell.style.cssText;
            if (field) {
                this.applyInlineValueAttributes(staticValue, field, field.type !== 'word_editor');
            }
            shell.replaceWith(staticValue);
        });

        root.querySelectorAll('.word-editor-filled-value').forEach((node) => {
            const valueEl = node as HTMLElement;
            valueEl.style.setProperty('box-sizing', 'border-box', 'important');
            valueEl.style.setProperty('display', 'block', 'important');
            valueEl.style.setProperty('width', '100%', 'important');
            valueEl.style.setProperty('height', '100%', 'important');
            valueEl.style.setProperty('max-width', '100%', 'important');
            valueEl.style.setProperty('overflow', 'hidden', 'important');
            valueEl.style.setProperty('padding', '0', 'important');
        });

        root.querySelectorAll('.word-editor-filled-value .word-editor-value, .word-editor-filled-value .word-editor-value .ql-editor').forEach((node) => {
            const hostEl = node as HTMLElement;
            hostEl.style.setProperty('display', 'block', 'important');
            hostEl.style.setProperty('width', '100%', 'important');
            hostEl.style.setProperty('max-width', '100%', 'important');
        });

        root.querySelectorAll('.word-editor-filled-value figure.table, .document-html .inline-filled-word-editor-value figure.table').forEach((node) => {
            const figureEl = node as HTMLElement;
            figureEl.style.setProperty('display', 'table', 'important');
            figureEl.style.setProperty('width', 'auto', 'important');
            figureEl.style.setProperty('max-width', '100%', 'important');
            figureEl.style.setProperty('margin-left', 'auto', 'important');
            figureEl.style.setProperty('margin-right', 'auto', 'important');
        });

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

        root.querySelectorAll('.word-editor-filled-value table, .document-html .inline-filled-word-editor-value table').forEach((node) => {
            const tableEl = node as HTMLElement;
            tableEl.style.setProperty('border-collapse', 'separate', 'important');
            tableEl.style.setProperty('border-spacing', '0', 'important');
            tableEl.style.setProperty('table-layout', 'auto', 'important');
            tableEl.style.setProperty('width', 'auto', 'important');
            tableEl.style.setProperty('max-width', '100%', 'important');
            tableEl.style.setProperty('margin-left', 'auto', 'important');
            tableEl.style.setProperty('margin-right', 'auto', 'important');
            tableEl.style.setProperty('border', 'none', 'important');
        });

        root.querySelectorAll('.word-editor-filled-value table, .document-html .inline-filled-word-editor-value table').forEach((node) => {
            const tableEl = node as HTMLTableElement;
            const rows = Array.from(tableEl.querySelectorAll('tr'));
            rows.forEach((rowNode, rowIndex) => {
                const cells = Array.from(rowNode.children).filter((child) => {
                    const tagName = child.tagName.toLowerCase();
                    return tagName === 'td' || tagName === 'th';
                }) as HTMLElement[];

                cells.forEach((cellEl, cellIndex) => {
                    cellEl.style.setProperty('border', 'none', 'important');
                    cellEl.style.setProperty('border-top', '1px solid #000000', 'important');
                    cellEl.style.setProperty('border-left', '1px solid #000000', 'important');
                    if (cellIndex === cells.length - 1) {
                        cellEl.style.setProperty('border-right', '1px solid #000000', 'important');
                    } else {
                        cellEl.style.removeProperty('border-right');
                    }
                    if (rowIndex === rows.length - 1) {
                        cellEl.style.setProperty('border-bottom', '1px solid #000000', 'important');
                    } else {
                        cellEl.style.removeProperty('border-bottom');
                    }
                    cellEl.style.setProperty('padding', '6px', 'important');
                    cellEl.style.setProperty('vertical-align', 'top', 'important');
                });
            });
        });
    }

    private applyTemplateEmailFooterCaptureStyles(root: HTMLElement): void {
        root.querySelectorAll('.individual-footer-preview .xyz-signatures .xyz-signatures-blank td > div').forEach((node) => {
            const wrapper = node as HTMLElement;
            wrapper.style.setProperty('transform', 'translateY(-2px)', 'important');
        });

        root.querySelectorAll('.individual-footer-preview .xyz-signatures .xyz-sig-time').forEach((node) => {
            const timeEl = node as HTMLElement;
            timeEl.style.setProperty('font-size', '8px', 'important');
            timeEl.style.setProperty('line-height', '1', 'important');
            timeEl.style.setProperty('margin-top', '0', 'important');
            timeEl.style.setProperty('transform', 'translateY(-2px)', 'important');
        });

        root.querySelectorAll('.individual-footer-preview .xyz-signatures tr:last-child td').forEach((node) => {
            const cell = node as HTMLElement;
            cell.style.setProperty('padding-top', '3px', 'important');
        });

        root.querySelectorAll('.individual-footer-preview .xyz-signatures tr:last-child td > span').forEach((node) => {
            const valueEl = node as HTMLElement;
            valueEl.style.setProperty('transform', 'translateY(-2px)', 'important');
            valueEl.style.setProperty('line-height', '1.2', 'important');
        });
    }

    private attachInlineWordEditorToolbar(fieldId: string): void {
        const toolbarHost = this.elementRef.nativeElement.querySelector('.inline-word-editor-toolbar-host') as HTMLElement | null;
        const editor = this.inlineWordEditors.get(fieldId);
        const toolbarElement = editor?.ui?.view?.toolbar?.element as HTMLElement | undefined;
        if (!toolbarHost || !toolbarElement) {
            return;
        }
        if (this.activeToolbarElement === toolbarElement && toolbarElement.parentElement === toolbarHost) {
            return;
        }

        this.restoreDetachedInlineWordEditorToolbar();

        this.activeToolbarElement = toolbarElement;
        this.activeToolbarOriginParent = toolbarElement.parentElement;
        this.activeToolbarOriginNextSibling = toolbarElement.nextSibling;
        toolbarHost.appendChild(toolbarElement);
    }

    private restoreDetachedInlineWordEditorToolbar(): void {
        if (!this.activeToolbarElement || !this.activeToolbarOriginParent) {
            return;
        }

        if (this.activeToolbarOriginNextSibling && this.activeToolbarOriginNextSibling.parentNode === this.activeToolbarOriginParent) {
            this.activeToolbarOriginParent.insertBefore(this.activeToolbarElement, this.activeToolbarOriginNextSibling);
        } else {
            this.activeToolbarOriginParent.appendChild(this.activeToolbarElement);
        }

        this.activeToolbarElement = null;
        this.activeToolbarOriginParent = null;
        this.activeToolbarOriginNextSibling = null;
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
            return this.hasIndividualFooterPipeline() ? 'edit' : 'fill';
        }

        const permission = (step.fieldPermissions || []).find((item) => item.fieldId === field.id);
        return permission?.right || 'view';
    }

    private isFieldValueEmpty(field: TemplateField, value: any): boolean {
        if (field.type === 'checkbox') {
            return value !== true;
        }
        if (field.type === 'attachment') {
            const attachments = Array.isArray(value) ? value : (value ? [value] : []);
            return attachments.length === 0;
        }
        if (Array.isArray(value)) {
            return value.length === 0;
        }
        if (value === null || value === undefined) {
            return true;
        }
        return String(value).trim() === '';
    }

    private isFieldFillAvailable(field: TemplateField): boolean {
        return this.isFieldValueEmpty(field, this.persistedValues[field.id]);
    }

    private getCurrentPipelineStep(): PipelineStep | null {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline as PipelineStep[] : [];
        return steps.find((step) => step?.type === 'initiator') || steps[0] || null;
    }

    private getInitiatorSignatureSlots(): any[] {
        const currentUser = this.getCurrentUserProfile();
        const initiatorName = this.getCurrentUserDisplayName();
        if (!this.initiatorSignatureApproved) {
            return [{
                label: initiatorName || 'Initiator',
                txtUserName: initiatorName || '',
                txtDepartmentName: this.resolveDepartmentName(currentUser),
                txtDesignation: this.resolveDesignation(currentUser)
            }];
        }
        const userId = this.getCurrentUserId();
        const submissionTimestamp = this.editingApplication?.dteCreatedDate || new Date().toISOString();
        return userId ? [{
            serUserId: userId,
            txtUserName: initiatorName || 'Initiator',
            txtDepartmentName: this.resolveDepartmentName(currentUser),
            txtDesignation: this.resolveDesignation(currentUser),
            approvedDate: submissionTimestamp,
            approvedAt: submissionTimestamp,
            dteCreatedDate: submissionTimestamp,
            __signatureApproved: true
        }] : [{
            label: initiatorName || 'Initiator',
            txtUserName: initiatorName || '',
            txtDepartmentName: this.resolveDepartmentName(currentUser),
            txtDesignation: this.resolveDesignation(currentUser)
        }];
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

    private getCurrentUserDisplayName(): string {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            return String(user?.txtUserName || user?.userName || user?.name || '').trim();
        } catch {
            return '';
        }
    }

    private getCurrentUserProfile(): any {
        try {
            return JSON.parse(localStorage.getItem('user') || 'null') || {};
        } catch {
            return {};
        }
    }

    private getDynamicSignatureStep(field: TemplateField): any | null {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline : [];
        const targetId = field.signatureTargetId || field.pipelineStepId;
        return steps.find((step: any) => step?.id === targetId) || null;
    }

    private getDynamicApprovalSlotValue(field: TemplateField, slot: any): string {
        if (!slot?.__signatureApproved) {
            return '';
        }
        if (field.type === 'dynamic_approver_name') {
            return this.getDynamicSignatureLabel(slot);
        }
        if (field.type === 'dynamic_approval_timestamp') {
            const value = this.getHistoryDate(slot);
            return value !== '--' ? value : '';
        }
        if (field.type === 'dynamic_approver_department') {
            return this.getDynamicSignatureDepartment(slot);
        }
        if (field.type === 'dynamic_approver_designation') {
            return this.getDynamicSignatureDesignation(slot);
        }
        if (field.type === 'dynamic_user_details') {
            const timestamp = this.getHistoryDate(slot);
            return [
                this.getDynamicSignatureLabel(slot),
                this.getDynamicSignatureDesignation(slot),
                this.getDynamicSignatureDepartment(slot),
                timestamp !== '--' ? timestamp : ''
            ]
                .map((value) => String(value || '').trim())
                .filter((value) => value.length > 0)
                .join('\n');
        }
        return '';
    }

    private getDynamicSignatureDepartment(slot: any): string {
        return this.resolveDepartmentName(slot);
    }

    private getDynamicSignatureDesignation(slot: any): string {
        return this.resolveDesignation(slot);
    }

    private resolveDepartmentName(value: any): string {
        return String(
            value?.txtDepartmentName
            || value?.departmentName
            || value?.userDepartmentName
            || value?.hrTblDepartment?.txtDepartmentName
            || value?.hrTblDepartment?.departmentName
            || ''
        ).trim();
    }

    private resolveDesignation(value: any): string {
        return String(
            value?.txtDesignation
            || value?.designation
            || value?.cfgTblRole?.txtRoleName
            || value?.roleName
            || ''
        ).trim();
    }

    private getConfiguredSignatureSlots(step: any): any[] {
        const users = this.getConfiguredStepUsers(step);
        if (users.length > 0) {
            return users;
        }

        if (step.dynamicTarget === 'initiator' || (Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator'))) {
            const initiatorName = this.getCurrentUserDisplayName();
            return [{ label: initiatorName || 'Initiator', txtUserName: initiatorName || '' }];
        }

        const resolvedName = this.getResolvedDynamicSlotLabel(step);
        return [{ label: resolvedName || step.name || this.getPipelineTargetFallbackLabel(step) }];
    }

    private getConfiguredStepUsers(step: any): any[] {
        const users = Array.isArray(step.users) ? step.users : [];
        if (users.length > 0) {
            return users;
        }

        const departmentHeadUsers = this.getDepartmentHeadUsers(step);
        if (departmentHeadUsers.length > 0) {
            return departmentHeadUsers;
        }

        const rawHeadIds = String(step?.hrTblDepartment?.serDepartmentHeadId || '').trim();
        if (rawHeadIds) {
            return [];
        }

        const departmentUsers = Array.isArray(step?.hrTblDepartment?.cfgTblUsers) ? step.hrTblDepartment.cfgTblUsers : [];
        if (departmentUsers.length > 0) {
            return departmentUsers;
        }

        return [];
    }

    private signatureSlotsHaveConcreteUsers(slots: any[]): boolean {
        return (slots || []).some((slot) => {
            const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id);
            return Number.isFinite(userId) && userId > 0;
        });
    }

    private getDepartmentHeadUsers(step: any): any[] {
        const department = step?.hrTblDepartment;
        const rawHeadIds = String(department?.serDepartmentHeadId || '').trim();
        const departmentUsers = Array.isArray(department?.cfgTblUsers) ? department.cfgTblUsers : [];
        if (!rawHeadIds || departmentUsers.length === 0) {
            return [];
        }

        const headIds = rawHeadIds
            .split(',')
            .map((value: string) => Number(String(value).trim()))
            .filter((value: number, index: number, array: number[]) => Number.isFinite(value) && value > 0 && array.indexOf(value) === index);
        if (headIds.length === 0) {
            return [];
        }

        const userById = new Map(
            departmentUsers
                .map((user: any) => [Number(user?.serUserId ?? user?.userId ?? user?.id), user] as [number, any])
                .filter(([userId]: [number, any]) => Number.isFinite(userId) && userId > 0)
        );

        return headIds
            .map((userId: number) => userById.get(userId))
            .filter((user: any) => !!user);
    }

    private getApprovedSignatureSlots(step: any): any[] {
        const approvalSources = [
            { source: step.approvedUsers, trustedApproved: true },
            { source: step.approvers, trustedApproved: false },
            { source: step.approvals, trustedApproved: false },
            { source: step.approvalHistory, trustedApproved: false },
            { source: step.history, trustedApproved: false },
            { source: step.signatureUsers, trustedApproved: false },
            { source: step.signatures, trustedApproved: false },
            { source: this.getWorkflowApprovalHistory(), trustedApproved: false }
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

    private getApplicationApprovalHistory(): any[] {
        const history = this.parseApplicationData(this.editingApplication?.txtApprovalHistory);
        return Array.isArray(history) ? history : [];
    }

    private getWorkflowApprovalHistory(): any[] {
        return this.getActiveApprovalHistory(this.getApplicationApprovalHistory());
    }

    private getActiveApprovalHistory(entries: any[]): any[] {
        const active: any[] = [];
        (entries || []).forEach((entry) => {
            const action = String(entry?.action || entry?.status || '').toUpperCase();
            if (action === 'SENT_BACK_TO_INITIATOR') {
                for (let index = active.length - 1; index >= 0; index--) {
                    if (!this.entryBelongsToSubmission(active[index])) {
                        active.splice(index, 1);
                    }
                }
                active.push(entry);
                return;
            }

            if (action === 'SENT_BACK') {
                const toLevel = Number(entry?.toLevel ?? entry?.targetLevel ?? entry?.returnLevel);
                const resetLevel = Number.isFinite(toLevel) && toLevel > 0 ? toLevel : 1;
                for (let index = active.length - 1; index >= 0; index--) {
                    if (this.entryBelongsToSubmission(active[index])) {
                        continue;
                    }
                    const existingLevel = Number(active[index]?.intApprovalOrder ?? active[index]?.level);
                    if (Number.isFinite(existingLevel) && existingLevel >= resetLevel) {
                        active.splice(index, 1);
                    }
                }
                active.push(entry);
                return;
            }

            if (action === 'RESUBMITTED_BY_INITIATOR') {
                for (let index = active.length - 1; index >= 0; index--) {
                    if (!this.entryBelongsToSubmission(active[index])) {
                        active.splice(index, 1);
                    }
                }
                active.push(entry);
                return;
            }

            active.push(entry);
        });
        return active;
    }

    private entryBelongsToSubmission(entry: any): boolean {
        const stepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim().toLowerCase();
        const role = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        const level = Number(entry?.intApprovalOrder ?? entry?.level);
        const action = String(entry?.action || entry?.status || '').toUpperCase();
        return stepId === 'initiator' || role === 'submission' || action === 'SUBMITTED' || level === 1 && role === 'initiator';
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
        const initiatorStep = steps.find((item: any) => item?.type === 'initiator');
        const initiatorOrder = Number(initiatorStep?.order);
        const hasExplicitInitiatorOrder = Number.isFinite(initiatorOrder) && initiatorOrder > 0;
        const hasInitiator = steps.some((item: any) => item?.type === 'initiator');
        const order = Number(step?.order);
        if (Number.isFinite(order) && order > 0) {
            return hasExplicitInitiatorOrder ? order : (hasInitiator ? order + 1 : order);
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

    private getResolvedDynamicSlotLabel(step: any): string {
        return String(
            step?.txtUserName
            || step?.userName
            || step?.approverName
            || step?.currentApproverName
            || step?.txtCurrentApproverName
            || step?.currentApproverUserName
            || step?.name
            || ''
        ).trim();
    }

    private getHistoryDate(history: any): string {
        const value = history?.approvedAt || history?.approvedDate || history?.date || history?.dteCreatedDate || history?.timestamp;
        return value ? new Date(value).toLocaleString() : '--';
    }

    private createInlineValue(doc: Document, field: TemplateField): HTMLElement {
        const tagName = field.type === 'word_editor' ? 'div' : 'span';
        const element = doc.createElement(tagName);
        element.className = `inline-filled-value${field.type === 'textarea' ? ' inline-filled-textarea-value' : ''}${field.type === 'word_editor' ? ' inline-filled-word-editor-value' : ''}`;
        element.dataset['fieldId'] = field.id;
        if (field.type === 'word_editor') {
            element.innerHTML = this.normalizeWordEditorHtmlForDisplay(String(this.values[field.id] || ''));
        } else {
            element.textContent = this.getFieldValueText(field);
        }
        this.applyInlineValueAttributes(element, field, field.type !== 'word_editor');
        return element;
    }

    private normalizeWordEditorHtmlForDisplay(html: string): string {
        if (!html) {
            return '';
        }

        const wrapper = document.createElement('div');
        wrapper.innerHTML = stripEditorTableChromeFromHtml(html);

        const qlEditor = document.createElement('div');
        qlEditor.className = 'ql-editor';
        qlEditor.innerHTML = wrapper.innerHTML;

        qlEditor.querySelectorAll('figure.table').forEach((figure) => {
            const el = figure as HTMLElement;
            el.style.maxWidth = '100%';
            el.style.width = el.style.width || 'auto';
        });

        qlEditor.querySelectorAll('table').forEach((tableNode) => {
            const table = tableNode as HTMLTableElement;
            table.style.borderCollapse = 'collapse';
            table.style.maxWidth = '100%';
            table.style.width = table.style.width || 'auto';
        });

        qlEditor.querySelectorAll('td, th').forEach((cellNode) => {
            const cell = cellNode as HTMLElement;
            cell.style.border = cell.style.border || '1px solid #000000';
            cell.style.padding = cell.style.padding || '6px';
            cell.style.verticalAlign = cell.style.verticalAlign || 'top';
        });

        return `<div class="word-editor-value">${qlEditor.outerHTML}</div>`;
    }

    private applyInlineValueAttributes(control: HTMLElement, field: TemplateField, applyTextAlign = true): void {
        control.style.fontSize = `${field.style?.fontSize || 14}px`;
        control.style.fontFamily = field.style?.fontFamily || 'Arial, sans-serif';
        control.style.fontWeight = field.style?.bold ? '700' : '400';
        control.style.fontStyle = field.style?.italic ? 'italic' : 'normal';
        control.style.textDecoration = field.style?.underline ? 'underline' : 'none';
        if (applyTextAlign) {
            control.style.textAlign = field.style?.textAlign || 'left';
        } else {
            control.style.removeProperty('text-align');
        }
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
            `.filled-field[data-field-id="${fieldId}"] .inline-word-editor-shell .ck-editor__editable_inline`
        ) as HTMLElement | null;
        if (!fieldElement) {
            return false;
        }

        return fieldElement.scrollHeight > fieldElement.clientHeight + 1
            || fieldElement.scrollWidth > fieldElement.clientWidth + 1;
    }
}
