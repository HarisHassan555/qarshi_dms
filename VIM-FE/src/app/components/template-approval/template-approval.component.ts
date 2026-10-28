import { ChangeDetectorRef, Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import Swal from 'sweetalert2';
import { confirmTemplatePdfDownloadIfNeeded } from 'src/app/utils/template-pdf-download.util';
import {
    buildIndividualFooterTableBands,
    getIndividualFooterRenderedHeightPx,
    IndividualFooterTableBand
} from 'src/app/utils/individual-footer-pipeline.util';
import { NotificationService } from 'src/app/NotificationService';
import { ApplicationPdfService } from 'src/app/services/application-pdf/application-pdf.service';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';
import { urls } from 'src/app/utils/urls';
import {
    WORD_EDITOR_CKEDITOR,
    WORD_EDITOR_CKEDITOR_CONFIG,
    normalizeWordEditorValueForCkeditor,
} from 'src/app/utils/word-editor-ckeditor.util';
import { stripEditorTableChromeFromHtml } from 'src/app/utils/word-editor-table.util';
import { applyWordEditorAlignmentClasses, getTemplateFieldNgStyle } from 'src/app/utils/word-editor-display.util';
import { normalizeDocumentBodyHtmlForDisplay } from 'src/app/utils/document-body-html.util';
import {
    resolveDocumentHeaderAddress,
    resolveDocumentHeaderBrandTitle,
    resolveDocumentHeaderLogoPath,
} from 'src/app/utils/document-header.util';

type TemplateFieldType =
    'document_header' | 'document_header_qu' | 'document_header_qf' | 'document_header_qri' | 'document_header_qb'
    | 'footer' | 'individual_pipeline_footer' | 'application_code' | 'pipeline_signature' | 'dynamic_signature'
    | 'dynamic_approver_name' | 'dynamic_approval_timestamp'
    | 'dynamic_approver_department' | 'dynamic_approver_designation' | 'dynamic_user_details'
    | 'text' | 'integer' | 'decimal' | 'number' | 'date' | 'email' | 'textarea' | 'word_editor'
    | 'attachment' | 'select' | 'checkbox' | 'radio' | 'table' | 'orientation';
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
    approvalMode?: string;
    order?: number;
    users?: any[];
    userIds?: number[];
    fieldPermissions?: PipelineFieldPermission[];
    fieldPermissionsConfigured?: boolean;
}

interface ProgressTile {
    key: string;
    title: string;
    type: string;
    status: 'APPROVED' | 'PENDING' | 'REJECTED' | 'WAITING' | 'OPINION';
    approver: string;
    approvedAt: string;
    remarks: string;
    ipAddress: string;
    approvalLatitude?: number | null;
    approvalLongitude?: number | null;
    approvalLocationLabel?: string;
    rawDate?: Date | null;
}

interface ProgressApproverSlot {
    user?: any;
    history?: any;
    index: number;
}

interface ApprovalLogRow {
    level: string;
    approver: string;
    role: string;
    status: string;
    date: string;
    comments: string;
    signatureUrl: string;
}

type ApprovalLogCsvColumnKey = 'level' | 'approver' | 'role' | 'status' | 'date' | 'comments';

interface ApprovalLogCsvColumn {
    key: ApprovalLogCsvColumnKey;
    label: string;
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
    attachment: AttachmentPayload;
}

const TEMPLATE_APPROVAL_HIDE_CKEDITOR_BADGE_CLASS = 'template-fill-hide-ckeditor-badge';
const SIGNATURE_REQUIRED_MESSAGE = 'Before approving the document, kindly upload your signature.';

@Component({
    selector: 'app-template-approval',
    templateUrl: './template-approval.component.html',
    styleUrls: ['./template-approval.component.css', '../template-fill/template-fill.component.css']
})
export class TemplateApprovalComponent implements OnInit, OnDestroy {
    private static readonly INDIVIDUAL_FOOTER_MIN_HEIGHT = 136;
    private static readonly DEFAULT_MAX_ATTACHMENT_MB = 5;
    private static readonly ALLOWED_ATTACHMENT_MIME_TYPES = new Set([
        'application/pdf',
        'image/webp',
        'image/png',
        'image/jpeg'
    ]);
    private static readonly ALLOWED_ATTACHMENT_EXTENSIONS = new Set(['pdf', 'webp', 'png', 'jpeg', 'jpg']);
    template: any = null;
    savedTemplate: SavedTemplateDefinition | null = null;
    application: any = null;
    safeHtml: SafeHtml = '';
    values: { [fieldId: string]: any } = {};
    persistedValues: { [fieldId: string]: any } = {};
    userPipeline: any[] = [];
    remarks = '';
    isLoading = true;
    isSaving = false;
    isActing = false;
    isRedirectingAfterAction = false;
    isDownloadingWithLogsPdf = false;
    showApprovalLogsCsvModal = false;
    selectedApprovalLogCsvColumnKeys: ApprovalLogCsvColumnKey[] = [];
    availableApprovalLogCsvColumns: ApprovalLogCsvColumn[] = [];
    selectedApprovalLogCsvColumns: ApprovalLogCsvColumn[] = [];
    readonly approvalLogCsvColumns: ApprovalLogCsvColumn[] = [
        { key: 'level', label: 'Level' },
        { key: 'approver', label: 'Approver' },
        { key: 'role', label: 'Role' },
        { key: 'status', label: 'Status' },
        { key: 'date', label: 'Date' },
        { key: 'comments', label: 'Comments' }
    ];
    allUsers: any[] = [];
    selectedOpinionUserId: number | null = null;
    showAttachmentsModal = false;
    showPortalAttachmentModal = false;
    private portalAttachmentTargetField: TemplateField | null = null;
    private attachmentSourceByFieldId: { [fieldId: string]: 'computer' | 'portal' | '' } = {};
    showLocationModal = false;
    locationModalLoading = false;
    locationModalError = '';
    locationModalLabel = '';
    locationMapUrl: SafeResourceUrl | null = null;
    readonly wordEditor = WORD_EDITOR_CKEDITOR;
    readonly wordEditorConfig = WORD_EDITOR_CKEDITOR_CONFIG;
    activeInlineWordEditorFieldId: string | null = null;
    private acceptedWordEditorValues: { [fieldId: string]: string } = {};
    private revertingWordEditorFields = new Set<string>();
    private inlineWordEditors = new Map<string, any>();
    private activeToolbarElement: HTMLElement | null = null;
    private activeToolbarOriginParent: HTMLElement | null = null;
    private activeToolbarOriginNextSibling: ChildNode | null = null;
    private previewStepOverride: PipelineStep | null | undefined = undefined;
    private inlineFieldIds = new Set<string>();
    private previewRebuildTimer: ReturnType<typeof setTimeout> | null = null;
    private applicationDataCache: any = {};
    private approvalHistoryCache: any[] = [];
    private pipelineStepsCache: PipelineStep[] = [];
    currentStep: PipelineStep | null = null;
    editableFieldsCache: TemplateField[] = [];
    progressTilesCache: ProgressTile[] = [];
    approvalLogRowsCache: ApprovalLogRow[] = [];
    pipelineProgressPercent = 0;
    attachmentItemsCache: AttachmentListItem[] = [];
    hasAnyAttachmentsCache = false;
    hasAttachmentFieldsCache = false;
    readOnlyStepMessage = '';
    adminProxyNotice = '';
    canEditCurrentStep = false;
    canTakeWorkflowActions = false;
    opinionPending = false;
    opinionRequesterName = 'Current approver';
    opinionPendingWithName = 'Selected user';
    holdPending = false;
    holdReason = '';
    individualFooterSectionsCache: any[] = [];

    constructor(
        private elementRef: ElementRef<HTMLElement>,
        private route: ActivatedRoute,
        private router: Router,
        private sanitizer: DomSanitizer,
        private notificationService: NotificationService,
        private applicationPdfService: ApplicationPdfService,
        private templateWorkflowService: TemplateWorkflowService,
        private userService: UserService,
        private http: HttpClient,
        private cdr: ChangeDetectorRef
    ) { }

    async ngOnInit(): Promise<void> {
        document.body.classList.add(TEMPLATE_APPROVAL_HIDE_CKEDITOR_BADGE_CLASS);
        const applicationId = this.route.snapshot.paramMap.get('id');
        if (!applicationId) {
            this.isLoading = false;
            return;
        }

        try {
            this.application = await firstValueFrom(
                this.templateWorkflowService.getApplication(applicationId, this.getCurrentUserId())
            );
            if (!this.application?.serApplicationId || !this.application?.serFormId) {
                throw new Error('Template application not found');
            }
            this.savedTemplate = await firstValueFrom(this.templateWorkflowService.getTemplate(String(this.application.serFormId)));
            this.template = this.buildSelectedTemplate();
            if (!this.template) {
                throw new Error('Template definition not found');
            }
            this.refreshInlineFieldIds();
            this.loadApplicationValues();
            this.refreshDerivedState();
            this.rebuildPreviewHtml();
            this.loadUsers();
        } catch (error: any) {
            this.notificationService.showMessage(error?.message || 'Template approval could not be loaded.', 'danger');
        } finally {
            this.isLoading = false;
        }
    }

    ngOnDestroy(): void {
        document.body.classList.remove(TEMPLATE_APPROVAL_HIDE_CKEDITOR_BADGE_CLASS);
        this.restoreDetachedInlineWordEditorToolbar();
        if (this.previewRebuildTimer) {
            clearTimeout(this.previewRebuildTimer);
            this.previewRebuildTimer = null;
        }
    }

    get activeStep(): PipelineStep | null {
        return this.currentStep;
    }

    private getPipelineStepForApplication(application: any): PipelineStep | null {
        if (this.isInitiatorResubmissionStep(application)) {
            return this.getInitiatorPipelineStep();
        }
        const steps = (Array.isArray(this.template?.pipeline) ? this.template.pipeline : [])
            .filter((step: PipelineStep) => step && step.type !== 'initiator');
        if (steps.length === 0) {
            return null;
        }
        const level = Number(application?.intCurrentApprovalLevel || 2);
        const index = Math.max(0, level - 2);
        return steps[index] || steps[0];
    }

    get pageIndexes(): number[] {
        const count = this.template?.page?.count || 1;
        return Array.from({ length: count }, (_, index) => index);
    }

    get editableFields(): TemplateField[] {
        return this.editableFieldsCache;
    }

    get panelFields(): TemplateField[] {
        return this.editableFields.filter((field: TemplateField) => !this.isInlineWordEditorField(field));
    }

    getActiveInlineWordEditorField(): TemplateField | null {
        const activeFieldId = this.activeInlineWordEditorFieldId;
        if (!activeFieldId) {
            return null;
        }
        return (this.template?.fields || []).find((field: TemplateField) => field.id === activeFieldId) || null;
    }

    trackByFieldId(_index: number, field: TemplateField): string {
        return field.id;
    }

    trackByRadioOption(_index: number, option: string): string {
        return option;
    }

    trackByTile(_index: number, tile: ProgressTile): string {
        return tile.key;
    }

    get progressTiles(): ProgressTile[] {
        return this.progressTilesCache;
    }

    getApprovalLogRows(): ApprovalLogRow[] {
        return this.approvalLogRowsCache;
    }

    getArrowLabel(index: number): string {
        const previous = this.progressTiles[index - 1]?.rawDate;
        const current = this.progressTiles[index]?.rawDate;
        if (!previous || !current) {
            return '--';
        }
        const diffMs = current.getTime() - previous.getTime();
        if (!Number.isFinite(diffMs) || diffMs < 0) {
            return '--';
        }
        const minutes = Math.round(diffMs / 60000);
        if (minutes < 60) {
            return `${minutes}m`;
        }
        const hours = Math.round(minutes / 60);
        if (hours < 48) {
            return `${hours}h`;
        }
        return `${Math.round(hours / 24)}d`;
    }

    getPipelineProgress(): number {
        return this.pipelineProgressPercent;
    }

    getTileClass(tile: ProgressTile): string {
        return `is-${tile.status.toLowerCase()}`;
    }

    canCurrentStepEditField(field: TemplateField): boolean {
        if (this.isOnHold() || this.isOpinionPending()) {
            return false;
        }
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

    isInlineWordEditorField(field: TemplateField): boolean {
        return field.type === 'word_editor' && !!field.placement;
    }

    isInlineWordEditorActive(field: TemplateField): boolean {
        return this.activeInlineWordEditorFieldId === field.id;
    }

    activateInlineWordEditor(field: TemplateField): void {
        if (!this.isInlineWordEditorField(field) || !this.canCurrentStepEditField(field)) {
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

    onInlineWordEditorFocus(field: TemplateField): void {
        this.activateInlineWordEditor(field);
    }

    isFieldHiddenForCurrentStep(field: TemplateField): boolean {
        return this.getFieldRightForStep(field, this.activeStep) === 'hide';
    }

    getCurrentStepFieldRight(field: TemplateField): PipelineFieldRight | 'view' {
        return this.getFieldRightForStep(field, this.activeStep);
    }

    isInitiatorResubmissionStep(application: any = this.application): boolean {
        if (!application || this.isOpinionPending() || this.isOnHold(application)) {
            return false;
        }
        const currentUserId = Number(this.getCurrentUserId() || 0);
        const submitterId = Number(application?.serSubmittedBy || 0);
        if (!Number.isFinite(currentUserId) || currentUserId <= 0 || currentUserId !== submitterId) {
            return false;
        }
        const status = String(application?.txtStatus || '').toUpperCase();
        if (status === 'REJECTED' || status === 'APPROVED' || status === 'COMPLETED' || status === 'HOLD') {
            return false;
        }
        const currentLevel = Number(application?.intCurrentApprovalLevel || 0);
        if (Number.isFinite(currentLevel) && currentLevel > 1) {
            return false;
        }
        const currentApproverId = Number(application?.serCurrentApprover || 0);
        if (currentApproverId > 0 && currentApproverId === submitterId) {
            return true;
        }
        return this.hasLatestSendBackToInitiatorAction(application);
    }

    canCurrentUserEditCurrentStep(application: any = this.application): boolean {
        if (application === this.application) {
            return this.canEditCurrentStep;
        }
        if (!application) {
            return false;
        }
        if (this.isInitiatorResubmissionStep(application)) {
            return true;
        }
        if (this.isOpinionPending()) {
            return this.isCurrentOpinionUser(application);
        }
        return this.isCurrentPendingApprover(application);
    }

    canCurrentUserTakeWorkflowActions(application: any = this.application): boolean {
        return application === this.application ? this.canTakeWorkflowActions : this.canCurrentUserEditCurrentStep(application);
    }

    getReadOnlyStepMessage(application: any = this.application): string {
        if (application === this.application) {
            return this.readOnlyStepMessage;
        }
        if (!application) {
            return 'This application is available in read-only mode.';
        }
        if (this.isOpinionPending() && !this.isCurrentOpinionUser(application)) {
            return 'Opinion is currently assigned to another user. This application is read-only for you.';
        }
        if (this.isInitiatorResubmissionStep(application)) {
            return '';
        }
        if (!this.isCurrentPendingApprover(application)) {
            return 'This approval step is currently assigned to another user. This application is read-only for you.';
        }
        return '';
    }

    getFieldRightForStep(field: TemplateField, step: PipelineStep | null): PipelineFieldRight | 'view' {
        if (!step) {
            return 'view';
        }
        if (step.type === 'initiator' && !step.fieldPermissionsConfigured) {
            return this.hasIndividualFooterPipeline() ? 'edit' : 'fill';
        }
        const permission = (step.fieldPermissions || []).find((item) => item.fieldId === field.id);
        return permission?.right || 'view';
    }

    isHeaderFieldType(type: TemplateFieldType): boolean {
        return ['document_header', 'document_header_qu', 'document_header_qf', 'document_header_qri', 'document_header_qb'].includes(type);
    }

    isFooterFieldType(type: TemplateFieldType): boolean {
        return type === 'footer' || type === 'individual_pipeline_footer';
    }

    isDocumentRegionFieldType(type: TemplateFieldType): boolean {
        return this.isHeaderFieldType(type) || this.isFooterFieldType(type);
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
        const bandCount = this.getIndividualFooterBands().length;
        return getIndividualFooterRenderedHeightPx(
            height,
            bandCount,
            TemplateApprovalComponent.INDIVIDUAL_FOOTER_MIN_HEIGHT
        );
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
        return getTemplateFieldNgStyle(field);
    }

    getPanelInputType(field: TemplateField): string {
        if (field.type === 'integer' || field.type === 'decimal' || field.type === 'number') {
            return 'number';
        }
        if (field.type === 'email' || field.type === 'date') {
            return field.type;
        }
        return 'text';
    }

    getFieldValueText(field: TemplateField, optionLabel?: string): string {
        if (field.type === 'application_code') {
            return this.application?.txtFormCode || '';
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
        if (field.type === 'checkbox') {
            return this.values[field.id] ? '✓' : '';
        }
        if (field.type === 'radio' && optionLabel !== undefined) {
            return this.normalizeRadioValue(this.values[field.id]) === this.normalizeRadioValue(optionLabel) ? '✓' : '';
        }
        if (this.isDynamicSignatureField(field)) {
            return this.getDynamicSignatureFallbackLabel(field);
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
        return this.sanitizer.bypassSecurityTrustHtml(this.normalizeWordEditorHtmlForDisplay(String(this.values[field.id] || '')));
    }

    getIndividualFooterSections(): any[] {
        return this.individualFooterSectionsCache;
    }

    getIndividualFooterColSpan(section: any): number {
        const users = Array.isArray(section?.users) ? section.users : [];
        return Math.max(users.length, 1);
    }

    getIndividualFooterSlots(section: any): any[] {
        const users = Array.isArray(section?.users) ? section.users : [];
        return users.length > 0 ? users : [null];
    }

    getIndividualFooterBands(): IndividualFooterTableBand[] {
        return buildIndividualFooterTableBands(
            this.getIndividualFooterSections(),
            (section) => this.getIndividualFooterSlots(section)
        );
    }

    getIndividualFooterUserLabel(user: any): SafeHtml {
        const html = this.getIndividualFooterUserParts(user)
            .map((value) => this.escapeHtml(value))
            .join('<br>');
        return this.sanitizer.bypassSecurityTrustHtml(html);
    }

    getIndividualFooterSignatureUrl(user: any, section?: any, slotIndex = 0): string {
        const history = this.getIndividualFooterUserHistory(user, section, slotIndex);
        const userId = this.getUserId(user);
        if (!Number.isFinite(userId) || userId <= 0) {
            return '';
        }
        if (!history && !this.isIndividualFooterSlotPassed(section, slotIndex)) {
            return '';
        }
        return `${urls.API_URL}getSignature?userId=${userId}`;
    }

    isIndividualFooterUserApproved(user: any, section?: any, slotIndex = 0): boolean {
        const history = this.getIndividualFooterUserHistory(user, section, slotIndex);
        if (!history) {
            return this.isIndividualFooterSlotPassed(section, slotIndex);
        }
        const action = String(history?.action || history?.status || '').toUpperCase();
        if (action === 'REJECTED') {
            return false;
        }
        return action === 'APPROVED';
    }

    getIndividualFooterUserApprovalDate(user: any, section?: any, slotIndex = 0): string {
        const history = this.getIndividualFooterUserHistory(user, section, slotIndex);
        return history ? this.getHistoryDate(history) : '';
    }

    isAttachmentField(field: TemplateField): boolean {
        return field.type === 'attachment';
    }

    getAttachmentFields(): TemplateField[] {
        return (this.template?.fields || []).filter((field: TemplateField) => this.isAttachmentField(field));
    }

    get hasAnyAttachments(): boolean {
        return this.hasAnyAttachmentsCache;
    }

    get hasAttachmentFields(): boolean {
        return this.hasAttachmentFieldsCache;
    }

    getAllAttachmentItems(): AttachmentListItem[] {
        return this.attachmentItemsCache;
    }

    getAttachmentPayloads(field: TemplateField): AttachmentPayload[] {
        return this.normalizeExistingAttachmentPayloads(this.values[field.id]);
    }

    getAttachmentDisplayText(field: TemplateField): string {
        const attachments = this.getAttachmentPayloads(field);
        return attachments.length > 0 ? attachments.map((attachment) => attachment.fileName).join(', ') : '';
    }

    getAttachmentSource(fieldId: string): 'computer' | 'portal' | '' {
        return this.attachmentSourceByFieldId[fieldId] || '';
    }

    onAttachmentSourceChanged(field: TemplateField, source: 'computer' | 'portal' | ''): void {
        this.attachmentSourceByFieldId[field.id] = source || '';
        if (source === 'portal' && this.canCurrentStepEditField(field)) {
            this.openPortalAttachmentModal(field);
        }
    }

    openPortalAttachmentModal(field: TemplateField): void {
        if (!this.canCurrentStepEditField(field)) {
            return;
        }
        this.attachmentSourceByFieldId[field.id] = 'portal';
        this.portalAttachmentTargetField = field;
        this.showPortalAttachmentModal = true;
    }

    closePortalAttachmentModal(): void {
        this.showPortalAttachmentModal = false;
        this.portalAttachmentTargetField = null;
    }

    onPortalFilesSelected(payloads: AttachmentPayload[]): void {
        const field = this.portalAttachmentTargetField;
        if (!field || !payloads?.length) {
            return;
        }
        const currentPayloads = this.getAttachmentPayloads(field);
        const nextBytes = this.getAttachmentBytes(currentPayloads) + this.getAttachmentBytes(payloads);
        const maxBytes = this.getMaxAttachmentFieldBytes();
        if (nextBytes > maxBytes) {
            this.notificationService.showMessage(this.buildAttachmentLimitMessage(nextBytes, maxBytes), 'danger');
            return;
        }
        this.values = {
            ...this.values,
            [field.id]: [...currentPayloads, ...payloads]
        };
        this.patchApplicationDataValue(field.id, this.values[field.id]);
        this.refreshAttachmentDerivedState();
        this.onPanelValueChanged(field);
        this.notificationService.showMessage(
            payloads.length === 1
                ? `Added ${payloads[0].fileName} from portal.`
                : `Added ${payloads.length} files from portal.`,
            'success'
        );
        this.closePortalAttachmentModal();
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
        const fieldBytes = this.getAttachmentBytes(currentPayloads)
            + files.reduce((sum, file) => sum + (Number(file.size) || 0), 0);
        const maxBytes = this.getMaxAttachmentFieldBytes();
        if (fieldBytes > maxBytes) {
            this.notificationService.showMessage(this.buildAttachmentLimitMessage(fieldBytes, maxBytes), 'danger');
            input.value = '';
            return;
        }

        try {
            const payloads = await Promise.all(files.map((file) => this.buildAttachmentPayload(file)));
            this.values[field.id] = [...currentPayloads, ...payloads];
            this.patchApplicationDataValue(field.id, this.values[field.id]);
            this.refreshAttachmentDerivedState();
            this.onPanelValueChanged(field);
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
        const fieldId = field?.id || '';
        console.log('[template-approval] remove attachment event', {
            applicationId: this.application?.serApplicationId,
            fieldId,
            fieldLabel: field?.label,
            indexToRemove,
            eventType: event?.type || '',
            rawValue: fieldId ? this.values?.[fieldId] : undefined,
            normalizedCount: this.getAttachmentPayloads(field).length
        });
        const attachments = this.getAttachmentPayloads(field);
        if (indexToRemove < 0 || indexToRemove >= attachments.length) {
            console.log('[template-approval] remove attachment ignored: invalid index', {
                indexToRemove,
                attachmentCount: attachments.length
            });
            return;
        }
        const nextAttachments = attachments.filter((_, index) => index !== indexToRemove);
        this.values = {
            ...this.values,
            [fieldId]: nextAttachments
        };
        this.patchApplicationDataValue(fieldId, this.values[fieldId]);
        this.refreshAttachmentDerivedState();
        if (!this.hasAnyAttachments) {
            this.showAttachmentsModal = false;
        }
        this.onPanelValueChanged(field);
        this.cdr.detectChanges();
        console.log('[template-approval] remove attachment applied', {
            fieldId,
            beforeCount: attachments.length,
            afterCount: nextAttachments.length,
            remainingFiles: nextAttachments.map((attachment) => attachment.fileName)
        });
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

    onPanelValueChanged(field?: TemplateField): void {
        if (field?.placement) {
            return;
        }
        if (field && !this.inlineFieldIds.has(field.id)) {
            return;
        }
        this.schedulePreviewHtmlRebuild();
    }

    onWordEditorChanged(field: TemplateField, event: any): void {
        if (this.revertingWordEditorFields.has(field.id)) {
            const restoredValue = normalizeWordEditorValueForCkeditor(event?.editor?.getData?.() ?? this.values[field.id]);
            this.values[field.id] = restoredValue;
            this.acceptedWordEditorValues[field.id] = restoredValue;
            this.onPanelValueChanged(field);
            this.revertingWordEditorFields.delete(field.id);
            return;
        }

        const nextValue = normalizeWordEditorValueForCkeditor(this.values[field.id]);
        const previousValue = this.acceptedWordEditorValues[field.id] || '';
        this.values[field.id] = nextValue;
        this.onPanelValueChanged(field);

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
                this.onPanelValueChanged(field);
                this.revertingWordEditorFields.delete(field.id);
            }
        });
    }

    async saveChanges(): Promise<void> {
        if (!this.application || !this.template) {
            return;
        }
        if (!this.canEditCurrentStep) {
            this.notificationService.showMessage('This application is not available for editing.', 'danger');
            return;
        }
        this.isSaving = true;
        try {
            await this.persistCurrentTemplateState(false);
            this.notificationService.showMessage('Template application saved.', 'success');
        } catch (error: any) {
            this.notificationService.showMessage(error?.message || 'Template application could not be saved.', 'danger');
        } finally {
            this.isSaving = false;
        }
    }

    async downloadApplicationPdf(): Promise<void> {
        if (!this.application?.serApplicationId) {
            this.notificationService.showMessage('Invalid application ID', 'danger');
            return;
        }
        if (!(await confirmTemplatePdfDownloadIfNeeded(this.templateWorkflowService, this.application.serApplicationId))) {
            return;
        }
        this.templateWorkflowService.downloadTemplateApplicationPdf(this.application.serApplicationId).subscribe({
            next: (blob) => {
                const filename = `${this.application?.txtFormCode || 'template-application'}.pdf`;
                this.downloadBlob(blob, filename);
            },
            error: (error) => {
                this.notificationService.showMessage(
                    'Application PDF could not be downloaded: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
    }

    async approve(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please enter comments or remarks before approving')) {
            return;
        }
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!(await this.confirmWorkflowAction('approve'))) {
            return;
        }
        await this.performAction('approve');
    }

    async reject(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide a rejection reason')) {
            return;
        }
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!(await this.confirmWorkflowAction('reject'))) {
            return;
        }
        await this.performAction('reject');
    }

    async sendBack(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide remarks for sending back the application')) {
            return;
        }
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!(await this.confirmWorkflowAction('sendBack'))) {
            return;
        }
        await this.performAction('sendBack');
    }

    async sendBackToInitiator(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide remarks for sending back to initiator')) {
            return;
        }
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!(await this.confirmWorkflowAction('sendBackToInitiator'))) {
            return;
        }
        await this.performAction('sendBackToInitiator');
    }

    async resubmitToPipeline(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!this.application?.serApplicationId || this.isActing) {
            return;
        }
        this.isActing = true;
        try {
            await this.persistCurrentTemplateState(false);
            const response = await firstValueFrom(this.templateWorkflowService.resubmitTemplateApplicationFromInitiator(
                this.application.serApplicationId,
                this.remarks.trim(),
                this.getCurrentUserId()
            ));
            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Application could not be resubmitted');
            }

            this.application = await firstValueFrom(
                this.templateWorkflowService.getApplication(this.application.serApplicationId, this.getCurrentUserId())
            );
            const approverStep = this.getPipelineStepForApplication(this.application);
            const approverPdfBlob = await this.renderTemplatePreviewPdfBlobForStep(approverStep);
            const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.application?.txtFormCode || this.application?.serApplicationId}.pdf`;
            const pdfResponse: any = await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(
                this.application.serApplicationId,
                approverPdfBlob,
                filename,
                true
            ));
            if (!pdfResponse || pdfResponse.status !== 'Success') {
                throw new Error(pdfResponse?.message || 'Resubmission PDF could not be saved or emails could not be queued');
            }

            this.notificationService.showMessage(response.message || 'Application resubmitted successfully.', 'success');
            await this.redirectToPendingApprovals();
        } catch (error: any) {
            this.notificationService.showMessage(error?.error?.message || error?.message || 'Application could not be resubmitted.', 'danger');
        } finally {
            this.isActing = false;
        }
    }

    async requestOpinion(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.selectedOpinionUserId) {
            this.notificationService.showMessage('Please select a user for opinion', 'danger');
            return;
        }
        if (!this.validateRemarks('Please provide remarks for the opinion request')) {
            return;
        }
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!this.application?.serApplicationId || this.isActing) {
            return;
        }
        this.isActing = true;
        try {
            const response = await firstValueFrom(this.templateWorkflowService.requestTemplateOpinion(
                this.application.serApplicationId,
                Number(this.selectedOpinionUserId),
                this.remarks.trim()
            ));
            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Opinion request could not be sent');
            }
            this.notificationService.showMessage(response.message || 'Application sent for opinion.', 'success');
            await this.redirectToPendingApprovals();
        } catch (error: any) {
            this.notificationService.showMessage(error?.error?.message || error?.message || 'Opinion request could not be sent.', 'danger');
        } finally {
            this.isActing = false;
        }
    }

    async submitOpinion(action: 'approve' | 'reject', remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide remarks for your opinion')) {
            return;
        }
        if (!(await this.ensureActorSignatureUploaded())) {
            return;
        }
        if (!this.application?.serApplicationId || this.isActing) {
            return;
        }
        this.isActing = true;
        try {
            const response = await firstValueFrom(this.templateWorkflowService.submitTemplateOpinion(
                this.application.serApplicationId,
                action,
                this.remarks.trim()
            ));
            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Opinion could not be submitted');
            }
            this.notificationService.showMessage(response.message || 'Opinion submitted.', 'success');
            await this.redirectToPendingApprovals();
        } catch (error: any) {
            this.notificationService.showMessage(error?.error?.message || error?.message || 'Opinion could not be submitted.', 'danger');
        } finally {
            this.isActing = false;
        }
    }

    isOpinionPending(): boolean {
        return this.opinionPending;
    }

    isOnHold(application: any = this.application): boolean {
        if (application === this.application) {
            return this.holdPending;
        }
        return String(application?.txtStatus || '').toUpperCase() === 'HOLD';
    }

    getHoldReason(): string {
        return this.holdReason || 'No reason provided';
    }

    async holdApplication(remarksValue?: string): Promise<void> {
        if (!this.application?.serApplicationId || this.isActing || this.isOnHold()) {
            return;
        }
        const reason = String(remarksValue ?? this.remarks ?? '').trim();
        if (!reason) {
            this.notificationService.showMessage('Please enter a Hold reason in Remarks before putting this application on Hold.', 'danger');
            return;
        }
        if (!(await this.confirmWorkflowAction('hold'))) {
            return;
        }
        this.remarks = reason;
        this.isActing = true;
        try {
            const response: any = await firstValueFrom(
                this.templateWorkflowService.holdTemplateApplication(
                    this.application.serApplicationId,
                    reason,
                    this.getWorkflowOnBehalfOfUserId()
                )
            );
            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Could not put application on Hold');
            }
            this.notificationService.showMessage(response.message || 'Application put on Hold.', 'success');
            this.application = await firstValueFrom(
                this.templateWorkflowService.getApplication(this.application.serApplicationId, this.getCurrentUserId())
            );
            this.refreshDerivedState();
            this.rebuildPreviewHtml();
        } catch (error: any) {
            this.notificationService.showMessage(
                error?.error?.message || error?.message || 'Could not put application on Hold.',
                'danger'
            );
        } finally {
            this.isActing = false;
        }
    }

    async unholdApplication(remarksValue?: string): Promise<void> {
        if (!this.application?.serApplicationId || this.isActing || !this.isOnHold()) {
            return;
        }
        if (!(await this.confirmWorkflowAction('unhold'))) {
            return;
        }
        const note = String(remarksValue ?? this.remarks ?? '').trim();
        this.isActing = true;
        try {
            const response: any = await firstValueFrom(
                this.templateWorkflowService.unholdTemplateApplication(
                    this.application.serApplicationId,
                    note,
                    this.getWorkflowOnBehalfOfUserId()
                )
            );
            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Could not take application off Hold');
            }
            this.notificationService.showMessage(response.message || 'Application taken off Hold.', 'success');
            this.application = await firstValueFrom(
                this.templateWorkflowService.getApplication(this.application.serApplicationId, this.getCurrentUserId())
            );
            this.refreshDerivedState();
            this.rebuildPreviewHtml();
        } catch (error: any) {
            this.notificationService.showMessage(
                error?.error?.message || error?.message || 'Could not take application off Hold.',
                'danger'
            );
        } finally {
            this.isActing = false;
        }
    }

    getOpinionRequesterName(): string {
        return this.opinionRequesterName;
    }

    getOpinionPendingWithName(): string {
        return this.opinionPendingWithName;
    }

    getOpinionUsers(): any[] {
        const currentUserId = Number(this.getCurrentUserId() || 0);
        return (this.allUsers || []).filter((user) => {
            const userId = Number(user?.serUserId ?? user?.userId ?? user?.id);
            return Number.isFinite(userId) && userId > 0 && userId !== currentUserId;
        });
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
            : (configuredSlots.length > 0 ? configuredSlots : [{ label: step.name || 'Approver' }]);
    }

    getDynamicSignatureImageUrl(slot: any): string {
        if (!slot?.__signatureApproved) {
            return '';
        }

        const signaturePath = slot?.txtSignaturePath || slot?.signaturePath || slot?.signature || '';
        const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id);
        if (typeof signaturePath === 'string' && signaturePath.trim()) {
            const query = `signaturePath=${encodeURIComponent(signaturePath.trim())}`;
            if (Number.isFinite(userId) && userId > 0) {
                return `${urls.API_URL}getSignature?${query}&userId=${userId}`;
            }
            return `${urls.API_URL}getSignature?${query}`;
        }
        if (Number.isFinite(userId) && userId > 0) {
            return `${urls.API_URL}getSignature?userId=${userId}`;
        }
        if (typeof signaturePath === 'string' && (signaturePath.startsWith('data:') || /^https?:\/\//i.test(signaturePath))) {
            return signaturePath;
        }
        return '';
    }

    getDynamicSignatureLabel(slot: any): string {
        return slot?.txtUserName || slot?.userName || slot?.approverName || slot?.name || slot?.label || 'Approver';
    }

    getDynamicSignatureFallbackLabel(field: TemplateField): string {
        return this.getDynamicSignatureStep(field)?.name || 'Dynamic Signatures';
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
            || value?.submittedByDepartmentName
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

    private async ensureActorSignatureUploaded(): Promise<boolean> {
        const userId = this.getCurrentUserId();
        if (!userId) {
            this.notificationService.showMessage('Unable to verify your user session. Please sign in again.', 'danger');
            return false;
        }

        try {
            const response = await firstValueFrom(
                this.http.get<any>(`${urls.API_URL}getUserSignaturePath`, { params: { userId: String(userId) } })
            );
            if (response?.status === 'Success' && response?.hasSignature) {
                return true;
            }
        } catch {
            // Fall through to prompt; backend also enforces signature on submit.
        }

        const result = await Swal.fire({
            title: 'Signature required',
            html: `<p class="workflow-confirm-message">${SIGNATURE_REQUIRED_MESSAGE}</p>`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Upload signature',
            cancelButtonText: 'Close',
            reverseButtons: true,
            focusCancel: true,
            customClass: {
                popup: 'sweet-alerts workflow-confirm-swal',
                title: 'workflow-confirm-title',
                htmlContainer: 'workflow-confirm-body',
                actions: 'workflow-confirm-actions',
                confirmButton: 'workflow-confirm-primary',
                cancelButton: 'workflow-confirm-cancel',
            },
        });
        if (result.isConfirmed) {
            await this.router.navigate(['/signature']);
        }
        return false;
    }

    private async confirmWorkflowAction(
        action: 'approve' | 'reject' | 'sendBack' | 'sendBackToInitiator' | 'hold' | 'unhold'
    ): Promise<boolean> {
        const configs: Record<
            'approve' | 'reject' | 'sendBack' | 'sendBackToInitiator' | 'hold' | 'unhold',
            { title: string; text: string; confirmButtonText: string; confirmButtonColor?: string }
        > = {
            approve: {
                title: 'Approve application?',
                text: 'Move this application to the next step in the pipeline.',
                confirmButtonText: 'Approve',
            },
            reject: {
                title: 'Reject application?',
                text: 'This will reject the application. You cannot undo this action.',
                confirmButtonText: 'Reject',
                confirmButtonColor: '#e7515a',
            },
            sendBack: {
                title: 'Send back?',
                text: 'Return this application to the previous approver.',
                confirmButtonText: 'Send back',
            },
            sendBackToInitiator: {
                title: 'Send to initiator?',
                text: 'Return this application to the person who submitted it.',
                confirmButtonText: 'Send to initiator',
            },
            hold: {
                title: 'Put application on Hold?',
                text: 'Workflow actions will be paused. Hold time can be excluded from assessment scoring when a valid reason is recorded.',
                confirmButtonText: 'Put on Hold',
                confirmButtonColor: '#e2a03f',
            },
            unhold: {
                title: 'Take application off Hold?',
                text: 'The application will resume at the same approval step.',
                confirmButtonText: 'Unhold',
            },
        };
        const { title, text, confirmButtonText, confirmButtonColor } = configs[action];
        const result = await Swal.fire({
            title,
            html: `<p class="workflow-confirm-message">${text}</p>`,
            showCancelButton: true,
            confirmButtonText,
            cancelButtonText: 'Cancel',
            confirmButtonColor,
            reverseButtons: true,
            focusCancel: true,
            customClass: {
                popup: 'sweet-alerts workflow-confirm-swal',
                title: 'workflow-confirm-title',
                htmlContainer: 'workflow-confirm-body',
                actions: 'workflow-confirm-actions',
                confirmButton: 'workflow-confirm-primary',
                cancelButton: 'workflow-confirm-cancel',
            },
        });
        return result.isConfirmed;
    }

    private async performAction(action: 'approve' | 'reject' | 'sendBack' | 'sendBackToInitiator'): Promise<void> {
        if (!this.application?.serApplicationId || this.isActing) {
            return;
        }
        this.isActing = true;
        const actingStep = this.activeStep;
        const shouldSendBackToInitiator = action === 'sendBack'
            && !!actingStep
            && actingStep.type !== 'initiator'
            && this.isFirstApprovalStep(actingStep);
        try {
            await this.persistCurrentTemplateState(false);

            let response: any;
            const onBehalfOfUserId = this.getWorkflowOnBehalfOfUserId();
            if (action === 'approve') {
                response = await firstValueFrom(this.templateWorkflowService.approveTemplateApplication(
                    this.application.serApplicationId,
                    this.remarks.trim(),
                    true,
                    onBehalfOfUserId
                ));
            } else if (action === 'reject') {
                response = await firstValueFrom(this.templateWorkflowService.rejectTemplateApplication(
                    this.application.serApplicationId,
                    this.remarks.trim(),
                    onBehalfOfUserId
                ));
            } else if (action === 'sendBack' && !shouldSendBackToInitiator) {
                response = await firstValueFrom(this.templateWorkflowService.sendBackTemplateApplication(
                    this.application.serApplicationId,
                    this.remarks.trim(),
                    onBehalfOfUserId
                ));
            } else {
                response = await firstValueFrom(this.templateWorkflowService.sendBackTemplateApplicationToInitiator(
                    this.application.serApplicationId,
                    this.remarks.trim(),
                    onBehalfOfUserId
                ));
            }

            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Action could not be completed');
            }
            if (action === 'approve') {
                this.application = await firstValueFrom(
                    this.templateWorkflowService.getApplication(this.application.serApplicationId, this.getCurrentUserId())
                );
                const approverStep = this.getPipelineStepForApplication(this.application);
                this.previewStepOverride = actingStep;
                this.refreshDerivedState();
                this.rebuildPreviewHtml();
                const approverPdfBlob = await this.renderTemplatePreviewPdfBlobForStep(approverStep);
                const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.application?.txtFormCode || this.application?.serApplicationId}.pdf`;
                const pdfResponse: any = await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(
                    this.application.serApplicationId,
                    approverPdfBlob,
                    filename,
                    true
                ));
                if (!pdfResponse || pdfResponse.status !== 'Success') {
                    throw new Error(pdfResponse?.message || 'Approval PDF could not be saved or emails could not be queued');
                }
                this.refreshDerivedState();
            }
            this.notificationService.showMessage(response.message || 'Action completed successfully.', 'success');
            await this.redirectToPendingApprovals();
        } catch (error: any) {
            const message = error?.error?.message || error?.message || 'Action could not be completed.';
            this.notificationService.showMessage(
                message,
                this.isOrAlreadyApprovedMessage(message) ? 'warning' : 'danger'
            );
        } finally {
            this.isActing = false;
        }
    }

    private isOrAlreadyApprovedMessage(message: string | null | undefined): boolean {
        return (message || '').toLowerCase().includes('already approved by');
    }

    private async redirectToPendingApprovals(): Promise<void> {
        this.isRedirectingAfterAction = true;
        const navigated = await this.router.navigate(['/template-pending-approvals'], { replaceUrl: true });
        if (navigated) {
            return;
        }

        window.location.href = '/template-pending-approvals';
    }

    private async persistCurrentTemplateState(refreshPdf: boolean): Promise<void> {
        (this.template?.fields || []).forEach((field: TemplateField) => {
            if (field.type === 'word_editor' && field.placement) {
                this.values[field.id] = this.acceptedWordEditorValues[field.id] ?? this.values[field.id] ?? '';
            }
        });
        const allowedValues = this.getAllowedValuesForUpdate();
        await firstValueFrom(this.templateWorkflowService.updateTemplateApplication(
            this.application,
            allowedValues,
            this.template,
            this.userPipeline
        ));
        this.application = await firstValueFrom(
            this.templateWorkflowService.getApplication(this.application.serApplicationId, this.getCurrentUserId())
        );
        this.refreshDerivedState();
        if (refreshPdf) {
            const pdfBlob = await this.renderTemplatePreviewPdfBlob();
            const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.application?.txtFormCode || this.application?.serApplicationId}.pdf`;
            await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(this.application.serApplicationId, pdfBlob, filename));
        }
    }

    private getAllowedValuesForUpdate(): { [fieldId: string]: any } {
        return (this.template?.fields || []).reduce((acc: { [fieldId: string]: any }, field: TemplateField) => {
            if (field.type === 'application_code') {
                acc[field.id] = this.application?.txtFormCode || '';
                return acc;
            }
            const right = this.getCurrentStepFieldRight(field);
            if (right === 'edit' || (right === 'fill' && this.isFieldFillAvailable(field))) {
                acc[field.id] = this.values[field.id];
            }
            return acc;
        }, {});
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

    private loadApplicationValues(): void {
        const data = this.applicationDataCache && Object.keys(this.applicationDataCache).length > 0
            ? this.applicationDataCache
            : this.parseApplicationData(this.application?.txtApplicationData);
        const templateValues = data.templateValues || {};
        this.userPipeline = Array.isArray(data.footerFields) ? data.footerFields : [];
        (this.template?.fields || []).forEach((field: TemplateField) => {
            const value = templateValues[field.id] ?? data[field.id] ?? (field.type === 'application_code' ? this.application?.txtFormCode : (field.type === 'checkbox' ? false : ''));
            this.values[field.id] = value;
            this.persistedValues[field.id] = value;
            if (field.type === 'word_editor') {
                this.acceptedWordEditorValues[field.id] = String(value || '');
            }
        });
    }

    private refreshInlineFieldIds(): void {
        this.inlineFieldIds = new Set(
            (this.template?.fields || [])
                .filter((field: TemplateField) => !field.placement && !this.isDocumentRegionFieldType(field.type))
                .map((field: TemplateField) => field.id)
        );
    }

    private loadUsers(): void {
        this.userService.getUsers().subscribe({
            next: (users: any) => {
                this.allUsers = Array.isArray(users) ? users : [];
            },
            error: () => {
                this.allUsers = [];
            }
        });
    }

    private rebuildPreviewHtml(): void {
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.template?.html || ''));
    }

    private refreshDerivedState(): void {
        this.applicationDataCache = this.parseApplicationData(this.application?.txtApplicationData);
        this.opinionPending = this.computeIsOpinionPending();
        this.opinionRequesterName = this.applicationDataCache?.templateOpinionRequest?.requestedByName || 'Current approver';
        this.opinionPendingWithName = this.resolveOpinionPendingWithName();
        this.holdPending = this.computeIsOnHold();
        this.holdReason = this.resolveHoldReason();
        this.currentStep = this.previewStepOverride !== undefined
            ? this.previewStepOverride
            : this.getPipelineStepForApplication(this.application);
        this.pipelineStepsCache = this.buildPipelineStepsCache();
        this.canEditCurrentStep = this.computeCanCurrentUserEditCurrentStep(this.application);
        this.canTakeWorkflowActions = this.canEditCurrentStep || this.canCurrentUserUnhold();
        this.readOnlyStepMessage = this.computeReadOnlyStepMessage(this.application);
        this.adminProxyNotice = this.computeAdminProxyNotice(this.application);
        this.individualFooterSectionsCache = Array.isArray(this.userPipeline) ? this.userPipeline : [];
        this.editableFieldsCache = this.computeEditableFields();
        this.attachmentItemsCache = this.computeAttachmentItems();
        this.hasAttachmentFieldsCache = this.getAttachmentFields().length > 0;
        this.hasAnyAttachmentsCache = this.attachmentItemsCache.length > 0;
        this.approvalHistoryCache = this.buildApplicationApprovalHistoryCache();
        this.progressTilesCache = this.buildProgressTilesCache();
        this.pipelineProgressPercent = this.computePipelineProgress(this.progressTilesCache);
        this.approvalLogRowsCache = this.buildApprovalLogRowsCache();
    }

    private refreshAttachmentDerivedState(): void {
        this.attachmentItemsCache = this.computeAttachmentItems();
        this.hasAttachmentFieldsCache = this.getAttachmentFields().length > 0;
        this.hasAnyAttachmentsCache = this.attachmentItemsCache.length > 0;
    }

    private patchApplicationDataValue(fieldId: string, value: any): void {
        const nextData = {
            ...(this.applicationDataCache || this.parseApplicationData(this.application?.txtApplicationData)),
        };
        nextData[fieldId] = value;
        nextData.templateValues = {
            ...(nextData.templateValues || {}),
            [fieldId]: value
        };
        this.applicationDataCache = nextData;
        if (this.application) {
            this.application = {
                ...this.application,
                txtApplicationData: JSON.stringify(nextData)
            };
        }
    }

    private computeIsOpinionPending(): boolean {
        const request = this.applicationDataCache?.templateOpinionRequest;
        return String(this.application?.txtStatus || '').toUpperCase() === 'OPINION_PENDING'
            && (request?.active === true || String(request?.active).toLowerCase() === 'true');
    }

    private computeIsOnHold(): boolean {
        return String(this.application?.txtStatus || '').toUpperCase() === 'HOLD';
    }

    private resolveHoldReason(): string {
        const holdState = this.applicationDataCache?.templateHoldState;
        return String(holdState?.reason || this.application?.txtRemarks || '').trim();
    }

    private canCurrentUserUnhold(): boolean {
        if (!this.computeIsOnHold()) {
            return false;
        }
        const currentUserId = Number(this.getCurrentUserId() || 0);
        const heldBy = Number(this.applicationDataCache?.templateHoldState?.heldBy || 0);
        if (Number.isFinite(heldBy) && heldBy > 0 && heldBy === currentUserId) {
            return true;
        }
        if (this.isCurrentPendingApprover(this.application)) {
            return true;
        }
        return this.isCurrentUserAdmin() && this.resolveOnBehalfOfApproverId(this.application) != null;
    }

    private resolveOpinionPendingWithName(): string {
        const request = this.applicationDataCache?.templateOpinionRequest;
        const explicitName = String(
            request?.requestedFromName
            || request?.opinionUserName
            || request?.requestedUserName
            || ''
        ).trim();
        if (explicitName) {
            return explicitName;
        }

        const requestedFrom = Number(request?.requestedFrom || 0);
        if (Number.isFinite(requestedFrom) && requestedFrom > 0) {
            const matchedUser = this.allUsers.find((user) => this.getUserId(user) === requestedFrom);
            const matchedName = String(
                matchedUser?.txtUserName
                || matchedUser?.userName
                || matchedUser?.name
                || ''
            ).trim();
            if (matchedName) {
                return matchedName;
            }
        }

        return 'Selected user';
    }

    private computeCanCurrentUserEditCurrentStep(application: any = this.application): boolean {
        if (!application) {
            return false;
        }
        if (this.computeIsInitiatorResubmissionStep(application)) {
            return true;
        }
        if (this.opinionPending) {
            return this.isCurrentOpinionUser(application);
        }
        if (this.isCurrentPendingApprover(application)) {
            return true;
        }
        return this.canAdminProxyWorkflowActions(application);
    }

    private computeReadOnlyStepMessage(application: any = this.application): string {
        if (!application) {
            return 'This application is available in read-only mode.';
        }
        if (this.canAdminProxyWorkflowActions(application)) {
            return '';
        }
        if (this.computeIsOnHold() && this.canCurrentUserUnhold()) {
            return 'This application is on Hold. Take it off Hold to continue approval actions.';
        }
        if (this.computeIsOnHold()) {
            return 'This application is on Hold and is read-only for you.';
        }
        if (this.opinionPending && !this.isCurrentOpinionUser(application)) {
            return 'Opinion is currently assigned to another user. This application is read-only for you.';
        }
        if (this.computeIsInitiatorResubmissionStep(application)) {
            return '';
        }
        if (!this.isCurrentPendingApprover(application)) {
            const workflowNotice = String(application?.workflowUserNotice || '').trim();
            if (workflowNotice) {
                return workflowNotice;
            }
            return 'This approval step is currently assigned to another user. This application is read-only for you.';
        }
        return '';
    }

    private computeIsInitiatorResubmissionStep(application: any = this.application): boolean {
        if (!application || this.opinionPending || this.computeIsOnHold()) {
            return false;
        }
        const currentUserId = Number(this.getCurrentUserId() || 0);
        const submitterId = Number(application?.serSubmittedBy || 0);
        if (!Number.isFinite(currentUserId) || currentUserId <= 0 || currentUserId !== submitterId) {
            return false;
        }
        const status = String(application?.txtStatus || '').toUpperCase();
        if (status === 'REJECTED' || status === 'APPROVED' || status === 'COMPLETED' || status === 'HOLD') {
            return false;
        }
        const currentLevel = Number(application?.intCurrentApprovalLevel || 0);
        if (Number.isFinite(currentLevel) && currentLevel > 1) {
            return false;
        }
        const currentApproverId = Number(application?.serCurrentApprover || 0);
        if (currentApproverId > 0 && currentApproverId === submitterId) {
            return true;
        }
        return this.hasLatestSendBackToInitiatorAction(application);
    }

    private buildPipelineStepsCache(): PipelineStep[] {
        const pipeline = Array.isArray(this.template?.pipeline) ? this.template.pipeline : [];
        if (pipeline.some((step: PipelineStep) => step?.type === 'initiator')) {
            return pipeline;
        }
        return [{ id: 'initiator', name: 'Initiator', type: 'initiator' }, ...pipeline];
    }

    private computeEditableFields(): TemplateField[] {
        if (this.holdPending || this.opinionPending || !this.canEditCurrentStep) {
            return [];
        }
        return (this.template?.fields || []).filter((field: TemplateField) =>
            !this.isDocumentRegionFieldType(field.type) && this.canCurrentStepEditField(field)
        );
    }

    private computeAttachmentItems(): AttachmentListItem[] {
        return this.getAttachmentFields().reduce((items: AttachmentListItem[], field: TemplateField) => {
            this.getAttachmentPayloads(field).forEach((attachment) => {
                items.push({
                    fieldId: field.id,
                    fieldLabel: field.label || 'Attachment',
                    attachment
                });
            });
            return items;
        }, []);
    }

    private buildApplicationApprovalHistoryCache(): any[] {
        const entries: any[] = [];
        const prior = this.parseApplicationData(this.application?.txtPriorApprovals);
        if (Array.isArray(prior) && prior.length > 0) {
            entries.push(...prior);
        }
        const history = this.parseApplicationData(this.application?.txtApprovalHistory);
        if (Array.isArray(history) && history.length > 0) {
            entries.push(...history);
        }
        const nestedHistory = this.applicationDataCache?.approvalHistory
            || this.applicationDataCache?.priorApprovals
            || this.applicationDataCache?.templateApprovalHistory;
        if (Array.isArray(nestedHistory) && nestedHistory.length > 0) {
            entries.push(...nestedHistory);
        }

        const submissionEntry = this.getSyntheticSubmissionEntry(entries);
        if (submissionEntry) {
            entries.unshift(submissionEntry);
        }

        const seen = new Set<string>();
        return entries.filter((entry) => {
            const key = [
                entry?.action || entry?.status || '',
                entry?.stepId || entry?.pipelineStepId || entry?.signatureTargetId || '',
                entry?.intApprovalOrder ?? entry?.level ?? '',
                this.getUserId(entry) || '',
                entry?.approvedAt || entry?.approvedDate || entry?.date || entry?.dteCreatedDate || entry?.timestamp || '',
                entry?.remarks || entry?.comments || entry?.txtRemarks || ''
            ].join('|');
            if (seen.has(key)) {
                return false;
            }
            seen.add(key);
            return true;
        });
    }

    async downloadApplicationWithLogsPdf(): Promise<void> {
        if (!this.application?.serApplicationId || this.isDownloadingWithLogsPdf) {
            return;
        }

        this.isDownloadingWithLogsPdf = true;
        try {
            if (!(await confirmTemplatePdfDownloadIfNeeded(this.templateWorkflowService, this.application.serApplicationId))) {
                return;
            }
            const filename = `${this.application?.txtFormCode || 'template-application'}_with_logs.pdf`;
            const applicationPdfBlob = await firstValueFrom(
                this.templateWorkflowService.downloadTemplateApplicationPdf(this.application.serApplicationId)
            );
            const logsPdfHtml = this.buildApprovalLogsPdfHtml(this.approvalLogRowsCache);
            const logsPdfBlob = await this.applicationPdfService.renderHtmlToPdfBlob(logsPdfHtml, filename);
            const mergedPdfBlob = await this.applicationPdfService.mergePdfBlobs([applicationPdfBlob, logsPdfBlob]);
            this.downloadBlob(mergedPdfBlob, filename);
        } catch (error: any) {
            this.notificationService.showMessage(
                error?.message || 'Application and logs PDF could not be downloaded.',
                'danger'
            );
        } finally {
            this.isDownloadingWithLogsPdf = false;
        }
    }

    openApprovalLogsCsvModal(): void {
        if (this.approvalLogRowsCache.length === 0) {
            this.notificationService.showMessage('No approval logs available to export.', 'warning');
            return;
        }
        this.selectedApprovalLogCsvColumnKeys = [];
        this.availableApprovalLogCsvColumns = [...this.approvalLogCsvColumns];
        this.selectedApprovalLogCsvColumns = [];
        this.showApprovalLogsCsvModal = true;
    }

    closeApprovalLogsCsvModal(): void {
        this.showApprovalLogsCsvModal = false;
    }

    selectAllApprovalLogCsvColumns(): void {
        this.availableApprovalLogCsvColumns = [];
        this.selectedApprovalLogCsvColumnKeys = this.approvalLogCsvColumns.map((column) => column.key);
        this.selectedApprovalLogCsvColumns = [...this.approvalLogCsvColumns];
    }

    unselectAllApprovalLogCsvColumns(): void {
        this.availableApprovalLogCsvColumns = [...this.approvalLogCsvColumns];
        this.selectedApprovalLogCsvColumnKeys = [];
        this.selectedApprovalLogCsvColumns = [];
    }

    addApprovalLogCsvColumn(column: ApprovalLogCsvColumn): void {
        if (!this.selectedApprovalLogCsvColumnKeys.includes(column.key)) {
            this.selectedApprovalLogCsvColumnKeys = [...this.selectedApprovalLogCsvColumnKeys, column.key];
            this.availableApprovalLogCsvColumns = this.availableApprovalLogCsvColumns.filter((item) => item.key !== column.key);
            this.selectedApprovalLogCsvColumns = [...this.selectedApprovalLogCsvColumns, column];
        }
    }

    removeApprovalLogCsvColumn(column: ApprovalLogCsvColumn): void {
        this.selectedApprovalLogCsvColumnKeys = this.selectedApprovalLogCsvColumnKeys.filter((key) => key !== column.key);
        this.selectedApprovalLogCsvColumns = this.selectedApprovalLogCsvColumns.filter((item) => item.key !== column.key);
        this.availableApprovalLogCsvColumns = [...this.availableApprovalLogCsvColumns, column]
            .sort((left, right) => this.approvalLogCsvColumns.findIndex((item) => item.key === left.key) - this.approvalLogCsvColumns.findIndex((item) => item.key === right.key));
    }

    downloadApprovalLogsCsv(): void {
        if (!this.application?.serApplicationId) {
            return;
        }
        if (this.selectedApprovalLogCsvColumns.length === 0) {
            this.notificationService.showMessage('Select at least one log column before downloading CSV.', 'warning');
            return;
        }

        const selectedColumns = this.selectedApprovalLogCsvColumns;
        const csvRows = [
            selectedColumns.map((column) => this.escapeCsvValue(column.label)).join(','),
            ...this.approvalLogRowsCache.map((row) => selectedColumns
                .map((column) => this.escapeCsvValue(this.getApprovalLogCsvCellValue(row, column.key)))
                .join(','))
        ];

        this.downloadBlob(
            new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' }),
            `${this.application?.txtFormCode || 'template-application'}_logs.csv`
        );
        this.showApprovalLogsCsvModal = false;
    }

    private buildApprovalLogsPdfHtml(rows: ApprovalLogRow[]): string {
        const tableRows = rows.length > 0
            ? rows.map((row, index) => `
                <tr>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;text-align:center;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${this.escapeHtml(row.level)}</td>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;line-height:1.35;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${this.escapeHtml(row.approver)}</td>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;line-height:1.35;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${this.escapeHtml(row.role)}</td>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;line-height:1.35;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${this.escapeHtml(row.status)}</td>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;line-height:1.35;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${this.escapeHtml(row.date)}</td>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;line-height:1.45;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${this.escapeHtml(row.comments)}</td>
                    <td style="border:1px solid #d1d5db;border-bottom:1px solid #d1d5db;padding:8px 10px;font-size:12px;text-align:center;vertical-align:middle;background:${index % 2 === 0 ? '#ffffff' : '#f9fafb'};">${row.signatureUrl ? `<img src="${this.escapeHtml(row.signatureUrl)}" alt="Signature" style="display:block;margin:0 auto;max-width:90px;max-height:30px;object-fit:contain;" />` : '--'}</td>
                </tr>
            `).join('')
            : `
                <tr>
                    <td colspan="7" style="border:1px solid #d1d5db;padding:18px 12px;text-align:center;color:#6b7280;font-size:12px;">No approval logs available.</td>
                </tr>
            `;

        return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <style>
    html, body { min-height: 100%; width: 1120px; max-width: none; }
    * { box-sizing: border-box; }
    table { page-break-inside: auto; break-inside: auto; }
    tr { page-break-inside: avoid; break-inside: avoid; }
  </style>
</head>
<body style="width:1120px;max-width:none;margin:0;padding:18px 18px 48px;font-family:Arial,sans-serif;background:#ffffff;color:#111827;box-sizing:border-box;">
  <div style="width:100%;max-width:none;margin:0;padding-bottom:24px;background:#ffffff;box-sizing:border-box;">
    <div style="font-size:14px;font-weight:700;color:#111827;margin:0 0 8px;">Logs (${rows.length})</div>
    <table style="width:100%;margin:0 auto;border-collapse:collapse;table-layout:fixed;border:1px solid #d1d5db;">
      <thead>
        <tr>
          <th style="width:8%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Level</th>
          <th style="width:16%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Approver</th>
          <th style="width:14%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Role</th>
          <th style="width:12%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Status</th>
          <th style="width:18%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Date</th>
          <th style="width:22%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Comments</th>
          <th style="width:10%;background:#f3f4f6;color:#111827;padding:8px;border:1px solid #d1d5db;font-size:12px;text-align:left;">Signature</th>
        </tr>
      </thead>
      <tbody>${tableRows}</tbody>
    </table>
  </div>
</body>
</html>`;
    }

    private getApprovalLogCsvCellValue(row: ApprovalLogRow, key: ApprovalLogCsvColumnKey): string {
        switch (key) {
            case 'level':
                return row.level || '';
            case 'approver':
                return row.approver || '';
            case 'role':
                return row.role || '';
            case 'status':
                return row.status || '';
            case 'date':
                return row.date || '';
            case 'comments':
                return row.comments || '';
            default:
                return '';
        }
    }

    trackByApprovalLogCsvColumn(index: number, column: ApprovalLogCsvColumn): ApprovalLogCsvColumnKey {
        return column.key;
    }

    private escapeCsvValue(value: any): string {
        const normalized = String(value ?? '');
        return `"${normalized.replace(/"/g, '""')}"`;
    }

    private buildProgressTilesCache(): ProgressTile[] {
        if (!this.application || !this.template) {
            return [];
        }
        const steps = this.getPipelineSteps();
        const applicationStatus = String(this.application?.txtStatus || '').toUpperCase();
        const currentLevel = Number(this.application?.intCurrentApprovalLevel || 1);
        let stepLevel = 1;

        return steps.flatMap((step) => {
            if (step.type === 'initiator') {
                return [this.buildProgressTile(step, 1, applicationStatus, currentLevel, {
                    history: this.getInitiatorHistoryEntry(),
                    index: 0
                })];
            }
            stepLevel += 1;
            return this.getProgressApproverSlots(step, stepLevel)
                .map((slot) => this.buildProgressTile(step, stepLevel, applicationStatus, currentLevel, slot));
        });
    }

    private computePipelineProgress(tiles: ProgressTile[]): number {
        if (!tiles.length) {
            return 0;
        }
        const approved = tiles.filter((tile) => tile.status === 'APPROVED').length;
        return Math.round((approved / tiles.length) * 100);
    }

    private buildApprovalLogRowsCache(): ApprovalLogRow[] {
        return this.getApplicationApprovalHistory()
            .map((entry, index) => {
                const userId = Number(entry?.performedBy ?? entry?.serUserId ?? entry?.userId ?? entry?.approvedBy ?? entry?.approverId ?? entry?.id);
                const action = String(entry?.action || entry?.status || '--').toUpperCase();
                const step = this.getStepForHistoryEntry(entry);
                return {
                    level: String(this.getDisplayLevelForHistoryEntry(entry, index)),
                    approver: this.formatHistoryActorLabel(entry, userId),
                    role: action === 'SUBMITTED' ? 'Submission' : (step?.name || entry?.role || entry?.stageName || entry?.stepName || '--'),
                    status: action,
                    date: this.getHistoryDate(entry),
                    comments: entry?.remarks || entry?.comments || entry?.txtRemarks || '--',
                    signatureUrl: Number.isFinite(userId) && userId > 0 && action === 'APPROVED'
                        ? `${urls.API_URL}getSignature?userId=${userId}`
                        : ''
                };
            });
    }

    private schedulePreviewHtmlRebuild(): void {
        if (this.previewRebuildTimer) {
            clearTimeout(this.previewRebuildTimer);
        }
        this.previewRebuildTimer = setTimeout(() => {
            this.previewRebuildTimer = null;
            this.rebuildPreviewHtml();
        }, 80);
    }

    private syncRemarks(remarksValue?: string): void {
        if (remarksValue !== undefined) {
            this.remarks = remarksValue;
        }
    }

    private getFillHtml(html: string, step: PipelineStep | null = this.activeStep): string {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const fields = new Map<string, TemplateField>((this.template?.fields || []).map((field: TemplateField) => [field.id, field]));
        doc.querySelectorAll('.template-field[data-field-id]').forEach((node) => {
            const field = fields.get(node.getAttribute('data-field-id') || '');
            if (!field || field.placement || this.isDocumentRegionFieldType(field.type)) {
                return;
            }
            if (this.getFieldRightForStep(field, step) === 'hide') {
                node.remove();
                return;
            }
            node.replaceWith(this.createInlineValue(doc, field));
        });
        return normalizeDocumentBodyHtmlForDisplay(doc.body.innerHTML);
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
        element.style.fontSize = `${field.style?.fontSize || 14}px`;
        element.style.fontFamily = field.style?.fontFamily || 'Arial, sans-serif';
        element.style.fontWeight = field.style?.bold ? '700' : '400';
        element.style.fontStyle = field.style?.italic ? 'italic' : 'normal';
        element.style.textDecoration = field.style?.underline ? 'underline' : 'none';
        if (field.type !== 'word_editor') {
            element.style.textAlign = field.style?.textAlign || 'left';
        }
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

        applyWordEditorAlignmentClasses(qlEditor);

        return `<div class="word-editor-value">${qlEditor.outerHTML}</div>`;
    }

    private getDynamicSignatureStep(field: TemplateField): PipelineStep | null {
        const targetId = String(field.signatureTargetId || field.pipelineStepId || '').trim();
        if (!targetId) {
            return null;
        }
        return this.getPipelineSteps().find((step: PipelineStep) => this.isMatchingWorkflowStepId(step?.id, targetId)) || null;
    }

    private isMatchingWorkflowStepId(left: any, right: any): boolean {
        const leftId = String(left || '').trim().toLowerCase();
        const rightId = String(right || '').trim().toLowerCase();
        if (!leftId || !rightId) {
            return false;
        }
        if (leftId === rightId) {
            return true;
        }
        return this.isInitiatorStepId(leftId) && this.isInitiatorStepId(rightId);
    }

    private isInitiatorStepId(value: string): boolean {
        return value === 'initiator' || value === 'initiator-step';
    }

    private getConfiguredSignatureSlots(step: PipelineStep): any[] {
        const users = this.getConfiguredStepUsers(step);
        if (users.length > 0) {
            return users;
        }

        if ((step as any).dynamicTarget === 'initiator' || (Array.isArray((step as any).dynamicTargets) && (step as any).dynamicTargets.includes('initiator'))) {
            const initiatorName = String(this.application?.submittedByUserName || '').trim();
            return [{ label: initiatorName || 'Initiator', txtUserName: initiatorName || '' }];
        }

        const resolvedName = this.getResolvedDynamicSlotLabel(step);
        return [{ label: resolvedName || step.name || 'Approver' }];
    }

    private getInitiatorSignatureSlots(): any[] {
        const userId = Number(this.application?.serSubmittedBy ?? this.application?.submittedBy ?? this.application?.initiatorUserId);
        const initiatorName = String(this.application?.submittedByUserName || '').trim();
        const initiatorHistory = this.getInitiatorHistoryEntry();
        if (this.hasLatestSendBackToInitiatorAction(this.application)) {
            return [{
                label: initiatorName || 'Initiator',
                txtUserName: initiatorName || '',
                txtDepartmentName: this.resolveDepartmentName(initiatorHistory),
                txtDesignation: this.resolveDesignation(initiatorHistory)
            }];
        }
        return Number.isFinite(userId) && userId > 0 ? [{
            serUserId: userId,
            txtUserName: initiatorName || 'Initiator',
            txtDepartmentName: this.resolveDepartmentName(initiatorHistory),
            txtDesignation: this.resolveDesignation(initiatorHistory),
            approvedDate: this.application?.dteCreatedDate || null,
            approvedAt: this.application?.dteCreatedDate || null,
            dteCreatedDate: this.application?.dteCreatedDate || null,
            __signatureApproved: true
        }] : [{
            label: initiatorName || 'Initiator',
            txtUserName: initiatorName || '',
            txtDepartmentName: this.resolveDepartmentName(initiatorHistory),
            txtDesignation: this.resolveDesignation(initiatorHistory)
        }];
    }

    private getResolvedDynamicSlotLabel(step: PipelineStep): string {
        return String(
            (step as any)?.txtUserName
            || (step as any)?.userName
            || (step as any)?.approverName
            || (step as any)?.currentApproverName
            || (step as any)?.txtCurrentApproverName
            || (step as any)?.currentApproverUserName
            || step?.name
            || ''
        ).trim();
    }

    private getApprovedSignatureSlots(step: PipelineStep): any[] {
        const approvalSources = [
            { source: (step as any).approvedUsers, trustedApproved: true },
            { source: (step as any).approvers, trustedApproved: false },
            { source: (step as any).approvals, trustedApproved: false },
            { source: (step as any).approvalHistory, trustedApproved: false },
            { source: (step as any).history, trustedApproved: false },
            { source: (step as any).signatureUsers, trustedApproved: false },
            { source: (step as any).signatures, trustedApproved: false },
            { source: this.getWorkflowApprovalHistory(), trustedApproved: false }
        ];

        return approvalSources
            .filter((item) => Array.isArray(item.source))
            .flatMap((item) => item.source.map((entry: any) => ({
                ...this.normalizeSignatureEntry(entry),
                __signatureApproved: item.trustedApproved || this.signatureEntryIsApproved(entry)
            })))
            .filter((entry: any) => !!entry && entry.__signatureApproved)
            .filter((entry: any) => !!entry && this.signatureEntryMatchesStep(entry, step))
            .filter((entry: any) => !!this.getDynamicSignatureImageUrl(entry) || !!this.getDynamicSignatureLabel(entry));
    }

    private mergeSignatureSlots(configuredSlots: any[], approvedSlots: any[]): any[] {
        if (approvedSlots.length === 0) {
            return configuredSlots;
        }

        const approvedById = new Map(
            approvedSlots
                .map((slot) => [Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id), slot] as [number, any])
                .filter(([userId]) => Number.isFinite(userId) && userId > 0)
        );

        const usedApproved = new Set<any>();
        const mergedSlots = configuredSlots.map((slot) => {
            const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id);
            if (Number.isFinite(userId) && userId > 0) {
                const approvedSlot = approvedById.get(userId);
                if (approvedSlot) {
                    usedApproved.add(approvedSlot);
                    return approvedSlot;
                }
            }

            const configuredLabel = String(slot?.txtUserName || slot?.userName || slot?.approverName || slot?.name || slot?.label || '')
                .trim()
                .toLowerCase();
            if (configuredLabel) {
                const approvedSlot = approvedSlots.find((candidate) => {
                    if (usedApproved.has(candidate)) {
                        return false;
                    }
                    const approvedLabel = String(
                        candidate?.txtUserName || candidate?.userName || candidate?.approverName || candidate?.name
                        || candidate?.role || candidate?.departmentName || candidate?.txtDepartmentName || candidate?.label || ''
                    ).trim().toLowerCase();
                    return !!approvedLabel && (approvedLabel === configuredLabel || approvedLabel.includes(configuredLabel) || configuredLabel.includes(approvedLabel));
                });
                if (approvedSlot) {
                    usedApproved.add(approvedSlot);
                    return approvedSlot;
                }
            }

            return slot;
        });

        const unmatchedApproved = approvedSlots.filter((slot) => !usedApproved.has(slot));
        return unmatchedApproved.length > 0 ? [...mergedSlots, ...unmatchedApproved] : mergedSlots;
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
        return String(entry?.action || entry?.status || '').toUpperCase() === 'APPROVED';
    }

    private getApplicationApprovalHistory(): any[] {
        return this.approvalHistoryCache;
    }

    private hasLatestSendBackToInitiatorAction(application: any): boolean {
        const history = this.parseApplicationData(application?.txtApprovalHistory);
        if (!Array.isArray(history) || history.length === 0) {
            return false;
        }
        for (let index = history.length - 1; index >= 0; index--) {
            const action = String(history[index]?.action || '').toUpperCase();
            if (!action) {
                continue;
            }
            if (action === 'RESUBMITTED_BY_INITIATOR' || action === 'APPROVED' || action === 'REJECTED') {
                return false;
            }
            if (action === 'SENT_BACK_TO_INITIATOR') {
                return true;
            }
        }
        return false;
    }

    private signatureEntryMatchesStep(entry: any, step: PipelineStep): boolean {
        if (!entry) {
            return false;
        }

        const action = String(entry.action || '').toUpperCase();
        if (action && action !== 'APPROVED') {
            return false;
        }

        const entryStepId = String(entry.stepId ?? entry.pipelineStepId ?? entry.signatureTargetId ?? '').trim();
        const stepId = String(step.id ?? '').trim();
        if (entryStepId && stepId) {
            return entryStepId === stepId;
        }

        const configuredUserIds = this.getConfiguredStepUsers(step)
            .map((user) => Number(user?.serUserId ?? user?.userId ?? user?.id))
            .filter((userId) => Number.isFinite(userId) && userId > 0);
        const entryUserId = Number(entry?.serUserId ?? entry?.userId ?? entry?.approvedBy ?? entry?.approverId ?? entry?.id);
        const entryLevel = Number(entry.intApprovalOrder ?? entry.level);
        if (configuredUserIds.length > 0) {
            const levelMatches = Number.isFinite(entryLevel) ? this.signatureEntryLevelMatchesStep(entryLevel, step) : true;
            return Number.isFinite(entryUserId) && configuredUserIds.includes(entryUserId) && levelMatches;
        }

        const stepDepartmentIds = this.getStepDepartmentIds(step);
        const entryDepartmentIds = this.getEntryDepartmentIds(entry);
        if (stepDepartmentIds.length > 0 && entryDepartmentIds.length > 0) {
            const departmentMatches = entryDepartmentIds.some((id) => stepDepartmentIds.includes(id));
            if (departmentMatches) {
                return Number.isFinite(entryLevel) ? this.signatureEntryLevelMatchesStep(entryLevel, step) : true;
            }
        }

        const stepDepartmentNames = this.getStepDepartmentNames(step);
        const entryDepartmentNames = this.getEntryDepartmentNames(entry);
        if (stepDepartmentNames.length > 0 && entryDepartmentNames.length > 0) {
            const departmentNameMatches = entryDepartmentNames.some((name) =>
                stepDepartmentNames.some((stepName) => name === stepName || name.includes(stepName) || stepName.includes(name))
            );
            if (departmentNameMatches) {
                return Number.isFinite(entryLevel) ? this.signatureEntryLevelMatchesStep(entryLevel, step) : true;
            }
        }

        if (Number.isFinite(entryLevel)) {
            return this.signatureEntryLevelMatchesStep(entryLevel, step);
        }

        const role = String(entry.role || '').trim().toLowerCase();
        const stepName = String(step.name || '').trim().toLowerCase();
        return !!role && !!stepName && (role === stepName || role.includes(stepName) || stepName.includes(role));
    }

    private getPipelineSteps(): PipelineStep[] {
        return this.pipelineStepsCache;
    }

    private buildProgressTile(
        step: PipelineStep,
        stepLevel: number,
        applicationStatus: string,
        currentLevel: number,
        slot: ProgressApproverSlot
    ): ProgressTile {
        const isInitiator = step.type === 'initiator';
        const history = slot.history || null;
        const historyAction = String(history?.action || history?.status || '').toUpperCase();
        let tileStatus: ProgressTile['status'] = 'WAITING';
        const currentStepLevel = currentLevel > 0 ? currentLevel : 1;
        const isInitiatorReopened = this.hasLatestSendBackToInitiatorAction(this.application)
            || (applicationStatus === 'PENDING' && Number.isFinite(currentLevel) && currentLevel < 0);

        if (isInitiator) {
            tileStatus = isInitiatorReopened ? 'PENDING' : 'APPROVED';
        } else if (historyAction === 'REJECTED') {
            tileStatus = 'REJECTED';
        } else if (historyAction === 'APPROVED') {
            tileStatus = 'APPROVED';
        } else if (applicationStatus === 'APPROVED' || applicationStatus === 'COMPLETED') {
            tileStatus = 'APPROVED';
        } else if (applicationStatus === 'REJECTED' && stepLevel === currentStepLevel) {
            tileStatus = 'REJECTED';
        } else if (applicationStatus === 'OPINION_PENDING' && stepLevel === currentStepLevel) {
            tileStatus = 'OPINION';
        } else if (stepLevel === currentStepLevel) {
            tileStatus = 'PENDING';
        } else if (stepLevel < currentStepLevel && !this.stepRequiresIndividualTiles(step)) {
            tileStatus = 'APPROVED';
        }

        const userId = this.getUserId(slot.user || history);
        return {
            key: `${step.id || step.name || 'step'}-${stepLevel}-${Number.isFinite(userId) ? userId : slot.index}`,
            title: isInitiator ? 'Submission' : (step.name || `Step ${stepLevel}`),
            type: isInitiator ? 'Submission' : this.formatStepType(step),
            status: tileStatus,
            approver: this.getStepApproverName(step, history, slot.user),
            approvedAt: this.getHistoryDate(history),
            remarks: history?.remarks || history?.comments || history?.txtRemarks || '--',
            ipAddress: this.getHistoryIpAddress(history),
            approvalLatitude: this.getHistoryLatitude(history),
            approvalLongitude: this.getHistoryLongitude(history),
            approvalLocationLabel: this.getHistoryLocationLabel(history),
            rawDate: this.getHistoryDateObject(history)
        };
    }

    canShowViewLocationButton(tile: ProgressTile): boolean {
        if (!tile || tile.status !== 'APPROVED') {
            return false;
        }
        if (this.hasStoredApprovalCoordinates(tile)) {
            return true;
        }
        const ip = String(tile.ipAddress || '').trim();
        return ip.length > 0 && ip !== '--';
    }

    async openApprovalLocationModal(tile: ProgressTile): Promise<void> {
        this.showLocationModal = true;
        this.locationModalLoading = true;
        this.locationModalError = '';
        this.locationModalLabel = tile.approvalLocationLabel || '';
        this.locationMapUrl = null;

        try {
            const storedLatitude = Number(tile.approvalLatitude);
            const storedLongitude = Number(tile.approvalLongitude);
            if (this.isUsableMapCoordinate(storedLatitude, storedLongitude)) {
                this.applyLocationMap(storedLatitude, storedLongitude);
                return;
            }

            const ip = String(tile.ipAddress || '').trim();
            if (!ip || ip === '--' || this.isPrivateOrLocalIp(ip)) {
                this.locationModalError = 'Location is not available for local or private network addresses.';
                return;
            }

            const response = await firstValueFrom(this.templateWorkflowService.getTemplateApprovalLocationByIp(ip));
            if (!response || response.status !== 'Success') {
                this.locationModalError = response?.message || 'Could not resolve location for this IP address.';
                return;
            }
            const latitude = Number(response.latitude);
            const longitude = Number(response.longitude);
            if (!this.isUsableMapCoordinate(latitude, longitude)) {
                this.locationModalError = 'Location coordinates are not available for this approval.';
                return;
            }
            if (response.locationLabel) {
                this.locationModalLabel = String(response.locationLabel);
            }
            this.applyLocationMap(latitude, longitude);
        } catch (error: any) {
            this.locationModalError = error?.error?.message || error?.message || 'Failed to load approval location.';
        } finally {
            this.locationModalLoading = false;
            this.cdr.markForCheck();
        }
    }

    closeApprovalLocationModal(): void {
        this.showLocationModal = false;
        this.locationModalLoading = false;
        this.locationModalError = '';
        this.locationModalLabel = '';
        this.locationMapUrl = null;
    }

    private hasStoredApprovalCoordinates(tile: ProgressTile): boolean {
        return this.isUsableMapCoordinate(Number(tile.approvalLatitude), Number(tile.approvalLongitude));
    }

    private isUsableMapCoordinate(latitude: number, longitude: number): boolean {
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
            return false;
        }
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            return false;
        }
        return !(Math.abs(latitude) < 1 && Math.abs(longitude) < 1);
    }

    private applyLocationMap(latitude: number, longitude: number): void {
        if (!this.isUsableMapCoordinate(latitude, longitude)) {
            this.locationModalError = 'Location coordinates are not available for this approval.';
            return;
        }
        const url = this.buildOpenStreetMapEmbedUrl(latitude, longitude);
        this.locationMapUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }

    private buildOpenStreetMapEmbedUrl(latitude: number, longitude: number): string {
        const delta = 0.08;
        const minLon = longitude - delta;
        const minLat = latitude - delta;
        const maxLon = longitude + delta;
        const maxLat = latitude + delta;
        return `https://www.openstreetmap.org/export/embed.html?bbox=${minLon},${minLat},${maxLon},${maxLat}&layer=mapnik&marker=${latitude},${longitude}`;
    }

    private isPrivateOrLocalIp(ip: string): boolean {
        const normalized = String(ip || '').trim().toLowerCase();
        if (!normalized || normalized === '--') {
            return true;
        }
        if (normalized === 'localhost' || normalized === '::1' || normalized === '0:0:0:0:0:0:0:1') {
            return true;
        }
        if (normalized.startsWith('::ffff:127.') || normalized.startsWith('::ffff:10.')
            || normalized.startsWith('::ffff:192.168.')) {
            return true;
        }
        if (normalized.startsWith('127.') || normalized.startsWith('10.')
            || normalized.startsWith('192.168.')) {
            return true;
        }
        if (normalized.startsWith('172.')) {
            const second = Number(normalized.split('.')[1]);
            if (Number.isFinite(second) && second >= 16 && second <= 31) {
                return true;
            }
        }
        return false;
    }

    private getHistoryLatitude(history: any): number | null {
        const value = history?.approvalLatitude ?? history?.latitude ?? history?.lat;
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }

    private getHistoryLongitude(history: any): number | null {
        const value = history?.approvalLongitude ?? history?.longitude ?? history?.lon;
        const parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }

    private getHistoryLocationLabel(history: any): string {
        const label = history?.approvalLocationLabel ?? history?.locationLabel;
        return label != null && String(label).trim() ? String(label).trim() : '';
    }

    private getHistoryIpAddress(history: any): string {
        if (!history) {
            return '--';
        }
        const ip = history?.approvedIp ?? history?.ipAddress ?? history?.ip;
        if (ip != null && String(ip).trim()) {
            return String(ip).trim();
        }
        return '--';
    }

    private getProgressApproverSlots(step: PipelineStep, stepLevel: number): ProgressApproverSlot[] {
        const stepHistory = this.getApprovalEntriesForStep(step, stepLevel);
        const users = this.getConfiguredStepUsers(step);
        if (this.stepRequiresIndividualTiles(step) && users.length > 0) {
            const usedHistory = new Set<any>();
            const slots: ProgressApproverSlot[] = users.map((user, index) => {
                const userHistory = this.getLatestHistoryForUser(stepHistory, this.getUserId(user));
                if (userHistory) {
                    usedHistory.add(userHistory);
                }
                return { user, history: userHistory, index };
            });
            stepHistory
                .filter((entry) => !usedHistory.has(entry))
                .forEach((entry, index) => slots.push({ history: entry, index: users.length + index }));
            return slots;
        }

        if (stepHistory.length > 0) {
            return stepHistory.map((entry, index) => ({ history: entry, index }));
        }

        if (users.length === 1) {
            return [{ user: users[0], index: 0 }];
        }

        return [{ index: 0 }];
    }

    private stepRequiresIndividualTiles(step: PipelineStep): boolean {
        if (String(step.type || '').toLowerCase() === 'individual') {
            return true;
        }
        return String(step.approvalMode || '').toUpperCase() === 'AND';
    }

    private getLatestHistoryForUser(history: any[], userId: number): any {
        if (!Number.isFinite(userId) || userId <= 0) {
            return null;
        }
        return [...history]
            .reverse()
            .find((entry) => this.getUserId(entry) === userId) || null;
    }

    private getIndividualFooterUserHistory(user: any, section?: any, slotIndex = 0): any {
        const userId = this.getUserId(user);
        if (!Number.isFinite(userId) || userId <= 0) {
            return null;
        }
        const expectedLevel = this.getIndividualFooterExpectedLevel(section, slotIndex);
        if (expectedLevel !== null && Number.isFinite(expectedLevel) && expectedLevel > 0) {
            const approvedEntries = this.getWorkflowApprovalHistory().filter((entry) => {
                const action = String(entry?.action || entry?.status || '').toUpperCase();
                if (action !== 'APPROVED' || this.getUserId(entry) !== userId) {
                    return false;
                }
                const entryLevel = Number(entry?.intApprovalOrder ?? entry?.level);
                return Number.isFinite(entryLevel) && entryLevel === expectedLevel;
            });
            return this.getLatestHistoryForUser(approvedEntries, userId);
        }
        return this.getLatestHistoryForUser(
            this.getWorkflowApprovalHistory().filter((entry) => {
                const action = String(entry?.action || entry?.status || '').toUpperCase();
                return action === 'APPROVED';
            }),
            userId
        );
    }

    private getIndividualFooterExpectedLevel(section: any, slotIndex = 0): number | null {
        const sortedSections = [...this.individualFooterSectionsCache]
            .sort((left: any, right: any) => (Number(left?.order) || 0) - (Number(right?.order) || 0));
        const sectionPosition = sortedSections.findIndex((item: any) => item === section
            || (!!item?.key && !!section?.key && item.key === section.key));
        if (sectionPosition < 0) {
            return null;
        }
        const stepsBeforeSection = sortedSections
            .slice(0, sectionPosition)
            .reduce((count, item) => count + (Array.isArray(item?.users) ? item.users.length : 0), 0);
        return stepsBeforeSection + slotIndex + 2;
    }

    private isIndividualFooterSlotPassed(section: any, slotIndex = 0): boolean {
        const expectedLevel = this.getIndividualFooterExpectedLevel(section, slotIndex);
        if (expectedLevel === null || !Number.isFinite(expectedLevel) || expectedLevel <= 0) {
            return false;
        }
        const status = String(this.application?.txtStatus || '').toUpperCase();
        if (status === 'APPROVED' || status === 'COMPLETED') {
            return true;
        }
        const currentLevel = Number(this.application?.intCurrentApprovalLevel);
        return Number.isFinite(currentLevel) && currentLevel > expectedLevel;
    }

    private getApprovalEntriesForStep(step: PipelineStep, _stepLevel: number): any[] {
        return this.getWorkflowApprovalHistory().filter((entry) => this.historyEntryMatchesPipelineStep(entry, step));
    }

    private parseIndividualFooterPipelineStepId(stepId: string): { sectionKey: string; userId: number } | null {
        const trimmed = String(stepId || '').trim();
        if (!trimmed) {
            return null;
        }
        const match = trimmed.match(/^(.+)-(\d+)-(\d+)-(\d+)$/);
        if (!match) {
            return null;
        }
        const userId = Number(match[3]);
        if (!Number.isFinite(userId) || userId <= 0) {
            return null;
        }
        return { sectionKey: match[1], userId };
    }

    private historyEntryMatchesPipelineStep(entry: any, step: PipelineStep): boolean {
        const action = String(entry?.action || entry?.status || '').toUpperCase();
        if (!['APPROVED', 'REJECTED', 'SENT_BACK', 'SENT_BACK_TO_INITIATOR', 'RESUBMITTED_BY_INITIATOR'].includes(action)) {
            return false;
        }
        const stepId = String(step.id || '').trim();
        const entryStepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim();
        if (entryStepId && stepId && entryStepId === stepId) {
            return true;
        }
        const footerStep = this.parseIndividualFooterPipelineStepId(stepId);
        if (footerStep && entryStepId && entryStepId === footerStep.sectionKey) {
            return this.getUserId(entry) === footerStep.userId;
        }
        const rawLevel = this.getRawHistoryLevel(entry);
        if (rawLevel === 0) {
            return this.isFirstApprovalStep(step);
        }
        if (!footerStep) {
            const entryRole = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
            const stepName = String(step.name || '').trim().toLowerCase();
            if (entryRole && stepName && entryRole !== stepName) {
                return false;
            }
        }
        if (footerStep && this.getUserId(entry) === footerStep.userId) {
            return this.getLegacyDisplayLevelsForStep(step).includes(rawLevel);
        }
        if (footerStep) {
            return false;
        }
        return this.getLegacyDisplayLevelsForStep(step).includes(rawLevel);
    }

    private getInitiatorHistoryEntry(): any {
        if (this.hasLatestSendBackToInitiatorAction(this.application)) {
            return null;
        }
        return [...this.getWorkflowApprovalHistory()]
            .reverse()
            .find((entry) => {
                const action = String(entry?.action || entry?.status || '').toUpperCase();
                return ['SUBMITTED', 'APPROVED', 'RESUBMITTED_BY_INITIATOR'].includes(action) && this.entryBelongsToSubmission(entry);
            }) || null;
    }

    private signatureEntryLevelMatchesStep(entryLevel: number, step: PipelineStep): boolean {
        const expectedLevel = this.getSignatureStepHistoryLevel(step);
        return Number.isFinite(expectedLevel) && [expectedLevel, expectedLevel - 1, expectedLevel - 2].includes(entryLevel);
    }

    private getSignatureStepHistoryLevel(step: PipelineStep): number {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline as PipelineStep[] : [];
        const stepIndex = steps.findIndex((item) => item?.id === step.id);
        const initiatorStep = steps.find((item) => item?.type === 'initiator');
        const initiatorOrder = Number(initiatorStep?.order);
        const hasExplicitInitiatorOrder = Number.isFinite(initiatorOrder) && initiatorOrder > 0;
        const hasInitiator = steps.some((item) => item?.type === 'initiator');
        const order = Number(step.order);
        if (Number.isFinite(order) && order > 0) {
            return hasExplicitInitiatorOrder ? order : (hasInitiator ? order + 1 : order);
        }
        return hasInitiator ? stepIndex + 1 : stepIndex + 2;
    }

    private getConfiguredStepUsers(step: PipelineStep): any[] {
        if (Array.isArray(step.users) && step.users.length > 0) {
            return step.users;
        }

        if (Array.isArray(step.userIds) && step.userIds.length > 0) {
            return step.userIds
                .map((userId) => this.allUsers.find((user) => this.getUserId(user) === Number(userId)) || { serUserId: userId })
                .filter((user) => !!user);
        }

        const departmentHeadUsers = this.getDepartmentHeadUsers(step);
        if (departmentHeadUsers.length > 0) {
            return departmentHeadUsers;
        }

        const rawHeadIds = String((step as any)?.hrTblDepartment?.serDepartmentHeadId || '').trim();
        if (rawHeadIds) {
            return [];
        }

        const departmentUsers = (step as any)?.hrTblDepartment?.cfgTblUsers;
        if (Array.isArray(departmentUsers) && departmentUsers.length > 0) {
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

    private getDepartmentHeadUsers(step: PipelineStep): any[] {
        const department = (step as any)?.hrTblDepartment;
        const rawHeadIds = String(department?.serDepartmentHeadId || '').trim();
        const departmentUsers = Array.isArray(department?.cfgTblUsers) ? department.cfgTblUsers : [];
        if (!rawHeadIds || departmentUsers.length === 0) {
            return [];
        }

        const headIds = rawHeadIds
            .split(',')
            .map((value) => Number(String(value).trim()))
            .filter((value, index, array) => Number.isFinite(value) && value > 0 && array.indexOf(value) === index);
        if (headIds.length === 0) {
            return [];
        }

        const userById = new Map(
            departmentUsers
                .map((user: any) => [Number(user?.serUserId ?? user?.userId ?? user?.id), user] as [number, any])
                .filter(([userId]: [number, any]) => Number.isFinite(userId) && userId > 0)
        );

        return headIds
            .map((userId) => userById.get(userId))
            .filter((user) => !!user);
    }

    private getStepDepartmentIds(step: PipelineStep): number[] {
        const candidates = [
            (step as any)?.serDepartmentId,
            (step as any)?.departmentId,
            (step as any)?.hrTblDepartment?.serDepartmentId
        ];
        return candidates
            .map((value) => Number(value))
            .filter((value, index, array) => Number.isFinite(value) && value > 0 && array.indexOf(value) === index);
    }

    private getEntryDepartmentIds(entry: any): number[] {
        const candidates = [
            entry?.departmentId,
            entry?.serDepartmentId,
            entry?.submittedDepartmentId
        ];
        return candidates
            .map((value) => Number(value))
            .filter((value, index, array) => Number.isFinite(value) && value > 0 && array.indexOf(value) === index);
    }

    private getStepDepartmentNames(step: PipelineStep): string[] {
        const names = [
            String(step?.name || '').trim().toLowerCase(),
            String((step as any)?.departmentName || '').trim().toLowerCase(),
            String((step as any)?.txtDepartmentName || '').trim().toLowerCase(),
            String((step as any)?.hrTblDepartment?.txtDepartmentName || '').trim().toLowerCase()
        ].filter((value, index, array) => !!value && array.indexOf(value) === index);

        if ((step as any)?.dynamicTarget === 'initiator_hod') {
            names.push('user hod', 'user dept hod', 'user deptt. (hod)', 'user deptt. hod');
        }

        return names;
    }

    private getEntryDepartmentNames(entry: any): string[] {
        return [
            String(entry?.departmentName || '').trim().toLowerCase(),
            String(entry?.txtDepartmentName || '').trim().toLowerCase(),
            String(entry?.userDepartmentName || '').trim().toLowerCase(),
            String(entry?.role || '').trim().toLowerCase(),
            String(entry?.stageName || '').trim().toLowerCase(),
            String(entry?.stepName || '').trim().toLowerCase()
        ].filter((value, index, array) => !!value && array.indexOf(value) === index);
    }

    private validateRemarks(message: string): boolean {
        if (!this.remarks || !this.remarks.trim()) {
            this.notificationService.showMessage(message, 'danger');
            return false;
        }
        return true;
    }

    private getCurrentUserId(): number | null {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            return user?.serUserId || user?.userId || user?.id || null;
        } catch {
            return null;
        }
    }

    private isAdminUser(user: any): boolean {
        const role = (
            user?.cfgTblRole?.txtRoleName ||
            user?.cfgTblRole?.txtRoleCode ||
            user?.txtrole ||
            user?.roleName ||
            ''
        ).toString().trim().toUpperCase();
        return role === 'ADMIN'
            || role === 'ROLE_ADMIN'
            || role === 'SUPER ADMIN'
            || role === 'ROLE_SUPER ADMIN'
            || role.includes('ADMIN');
    }

    private isCurrentUserAdmin(): boolean {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            return this.isAdminUser(user);
        } catch {
            return false;
        }
    }

    private canAdminProxyWorkflowActions(application: any = this.application): boolean {
        if (!this.isCurrentUserAdmin() || !application) {
            return false;
        }
        if (this.computeIsInitiatorResubmissionStep(application)) {
            return false;
        }
        if (String(application?.txtStatus || '').toUpperCase() === 'OPINION_PENDING') {
            return false;
        }
        if (String(application?.txtStatus || '').toUpperCase() === 'HOLD') {
            return false;
        }
        const status = String(application?.txtStatus || '').toUpperCase();
        if (['REJECTED', 'APPROVED', 'COMPLETED'].includes(status)) {
            return false;
        }
        return this.resolveOnBehalfOfApproverId(application) != null;
    }

    private shouldSendOnBehalfOfUserId(application: any = this.application): boolean {
        if (!this.isCurrentUserAdmin() || !application) {
            return false;
        }
        if (this.isCurrentPendingApprover(application)) {
            return false;
        }
        if (this.computeIsInitiatorResubmissionStep(application)) {
            return false;
        }
        if (String(application?.txtStatus || '').toUpperCase() === 'OPINION_PENDING') {
            return false;
        }
        return this.resolveOnBehalfOfApproverId(application) != null;
    }

    private getWorkflowOnBehalfOfUserId(): number | null {
        return this.shouldSendOnBehalfOfUserId() ? this.resolveOnBehalfOfApproverId() : null;
    }

    private resolveOnBehalfOfApproverId(application: any = this.application): number | null {
        if (!application) {
            return null;
        }
        const currentApproverIds = Array.isArray(application?.currentApproverIds)
            ? application.currentApproverIds
                .map((value: any) => Number(value))
                .filter((value: number) => Number.isFinite(value) && value > 0)
            : [];
        const primaryApproverId = Number(application?.serCurrentApprover || 0);
        if (currentApproverIds.length > 0) {
            if (Number.isFinite(primaryApproverId) && primaryApproverId > 0 && currentApproverIds.includes(primaryApproverId)) {
                return primaryApproverId;
            }
            return currentApproverIds[0];
        }
        if (Number.isFinite(primaryApproverId) && primaryApproverId > 0) {
            return primaryApproverId;
        }
        const step = this.getPipelineStepForApplication(application);
        const configuredApproverIds = this.getConfiguredStepApproverIds(step);
        return configuredApproverIds.length > 0 ? configuredApproverIds[0] : null;
    }

    private getOnBehalfOfApproverDisplayName(application: any = this.application): string {
        const approverId = this.resolveOnBehalfOfApproverId(application);
        if (!Number.isFinite(approverId) || !approverId) {
            return '';
        }
        const matchedUser = (this.allUsers || []).find((user: any) => this.getUserId(user) === approverId);
        if (matchedUser) {
            return String(matchedUser.txtUserName || matchedUser.userName || matchedUser.name || '').trim();
        }
        const step = this.getPipelineStepForApplication(application);
        if (!step) {
            return `User ${approverId}`;
        }
        const configuredUser = this.getConfiguredStepUsers(step).find((user: any) => this.getUserId(user) === approverId);
        return String(configuredUser?.txtUserName || configuredUser?.userName || configuredUser?.name || `User ${approverId}`).trim();
    }

    private computeAdminProxyNotice(application: any = this.application): string {
        if (!this.shouldSendOnBehalfOfUserId(application)) {
            return '';
        }
        const approverName = this.getOnBehalfOfApproverDisplayName(application);
        if (!approverName) {
            return 'You are acting as administrator for the current approval step. Only fields allowed for this step are editable.';
        }
        return `You are acting as administrator on behalf of ${approverName}. Only fields allowed for this step are editable.`;
    }

    private formatHistoryActorLabel(entry: any, fallbackUserId: number): string {
        const performedByName = String(entry?.performedByName || '').trim();
        const onBehalfOfName = String(entry?.onBehalfOfName || entry?.approverName || entry?.txtUserName || entry?.userName || entry?.name || '').trim();
        if (performedByName && onBehalfOfName && performedByName.toLowerCase() !== onBehalfOfName.toLowerCase()) {
            return `${performedByName} (on behalf of ${onBehalfOfName})`;
        }
        if (onBehalfOfName) {
            return onBehalfOfName;
        }
        return Number.isFinite(fallbackUserId) && fallbackUserId > 0 ? `User ${fallbackUserId}` : '--';
    }

    private isCurrentPendingApprover(application: any = this.application): boolean {
        if (!application) {
            return false;
        }
        const currentUserId = Number(this.getCurrentUserId() || 0);
        const status = String(application?.txtStatus || '').toUpperCase();
        if (!Number.isFinite(currentUserId) || currentUserId <= 0) {
            return false;
        }
        if (['REJECTED', 'APPROVED', 'COMPLETED'].includes(status)) {
            return false;
        }

        const currentApproverIds = Array.isArray(application?.currentApproverIds)
            ? application.currentApproverIds
                .map((value: any) => Number(value))
                .filter((value: number) => Number.isFinite(value) && value > 0)
            : [];
        if (currentApproverIds.length > 0) {
            return currentApproverIds.includes(currentUserId);
        }

        const currentStep = this.getPipelineStepForApplication(application);
        if (currentStep && String(currentStep?.approvalMode || 'OR').toUpperCase() !== 'AND') {
            const configuredApproverIds = this.getConfiguredStepApproverIds(currentStep);
            if (configuredApproverIds.includes(currentUserId)) {
                return true;
            }
        }

        const currentApproverId = Number(application?.serCurrentApprover || 0);
        return Number.isFinite(currentApproverId) && currentApproverId > 0 && currentApproverId === currentUserId;
    }

    private getConfiguredStepApproverIds(step: PipelineStep | null): number[] {
        if (!step) {
            return [];
        }

        const approverIds = new Set<number>();
        this.getConfiguredStepUsers(step).forEach((user) => {
            const userId = this.getUserId(user);
            if (Number.isFinite(userId) && userId > 0) {
                approverIds.add(userId);
            }
        });

        (Array.isArray(step.userIds) ? step.userIds : []).forEach((userId) => {
            const numericUserId = Number(userId);
            if (Number.isFinite(numericUserId) && numericUserId > 0) {
                approverIds.add(numericUserId);
            }
        });

        if ((step as any)?.dynamicTarget === 'initiator' || (Array.isArray((step as any)?.dynamicTargets) && (step as any).dynamicTargets.includes('initiator'))) {
            const initiatorUserId = Number(this.application?.serSubmittedBy || 0);
            if (Number.isFinite(initiatorUserId) && initiatorUserId > 0) {
                approverIds.add(initiatorUserId);
            }
        }

        return Array.from(approverIds);
    }

    private isCurrentOpinionUser(application: any = this.application): boolean {
        if (!application || !this.isOpinionPending()) {
            return false;
        }
        const currentUserId = Number(this.getCurrentUserId() || 0);
        const data = this.parseApplicationData(application?.txtApplicationData);
        const request = data?.templateOpinionRequest;
        const requestedFrom = Number(request?.requestedFrom || 0);
        return Number.isFinite(currentUserId) && currentUserId > 0
            && Number.isFinite(requestedFrom) && requestedFrom > 0
            && currentUserId === requestedFrom;
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

    private getMaxAttachmentFieldMb(): number {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            const configured = Number(user?.numMaxAttachmentMb);
            if (Number.isFinite(configured) && configured > 0) {
                return Math.floor(configured);
            }
        } catch {
            // fall through to default
        }
        return TemplateApprovalComponent.DEFAULT_MAX_ATTACHMENT_MB;
    }

    private getMaxAttachmentFieldBytes(): number {
        return this.getMaxAttachmentFieldMb() * 1024 * 1024;
    }

    private buildAttachmentLimitMessage(actualBytes: number, maxBytes: number): string {
        const actualMb = (actualBytes / (1024 * 1024)).toFixed(2);
        const maxMb = Math.max(1, Math.round(maxBytes / (1024 * 1024)));
        return `These files are about ${actualMb} MB. Your account allows up to ${maxMb} MB per attachment field. Please remove some files or choose smaller ones.`;
    }

    private getAttachmentBytes(attachments: AttachmentPayload[]): number {
        return (attachments || []).reduce((sum, attachment) => {
            const base64 = String(attachment?.base64 || '').trim();
            return sum + (base64 ? Math.ceil((base64.length * 3) / 4) : 0);
        }, 0);
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

    private isAllowedAttachmentFile(file: File): boolean {
        const mime = String(file?.type || '').trim().toLowerCase();
        if (mime && TemplateApprovalComponent.ALLOWED_ATTACHMENT_MIME_TYPES.has(mime)) {
            return true;
        }
        const name = String(file?.name || '').toLowerCase();
        const dotIndex = name.lastIndexOf('.');
        const ext = dotIndex >= 0 ? name.substring(dotIndex + 1) : '';
        return !!ext && TemplateApprovalComponent.ALLOWED_ATTACHMENT_EXTENSIONS.has(ext);
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

    private downloadBlob(blob: Blob, filename: string): void {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(objectUrl);
    }

    private getApplicationTemplatePayload(): any | null {
        const data = this.parseApplicationData(this.application?.txtApplicationData);
        const payload = data?.templatePayload;
        return payload && typeof payload === 'object' ? payload : null;
    }

    private buildSelectedTemplate(): any | null {
        const liveTemplate = this.savedTemplate?.payload;
        const savedTemplate = this.getApplicationTemplatePayload();
        if (!liveTemplate && !savedTemplate) {
            return null;
        }
        if (!liveTemplate) {
            return savedTemplate;
        }
        if (!savedTemplate) {
            return liveTemplate;
        }
        return {
            ...liveTemplate,
            ...savedTemplate,
            fields: Array.isArray(savedTemplate.fields) && savedTemplate.fields.length > 0 ? savedTemplate.fields : liveTemplate.fields,
            pipeline: this.resolveWorkflowPipeline(savedTemplate, liveTemplate),
            page: savedTemplate.page || liveTemplate.page,
            html: typeof savedTemplate.html === 'string' && savedTemplate.html.length > 0 ? savedTemplate.html : liveTemplate.html
        };
    }

    private resolveWorkflowPipeline(savedTemplate: any, liveTemplate: any): PipelineStep[] {
        const savedPipeline = Array.isArray(savedTemplate?.pipeline) ? savedTemplate.pipeline : [];
        const livePipeline = Array.isArray(liveTemplate?.pipeline) ? liveTemplate.pipeline : [];
        const templatePipeline = savedPipeline.length > 0 ? savedPipeline : livePipeline;
        if (this.hasTemplateApprovalPipelineSteps(templatePipeline)) {
            return templatePipeline;
        }
        const footerPipeline = this.buildFooterWorkflowPipeline();
        if (footerPipeline.length > 0) {
            return footerPipeline;
        }
        return templatePipeline;
    }

    private hasTemplateApprovalPipelineSteps(pipeline: PipelineStep[]): boolean {
        if (!Array.isArray(pipeline) || pipeline.length === 0) {
            return false;
        }
        return pipeline.some((step) => step?.type && step.type !== 'initiator');
    }

    private buildFooterWorkflowPipeline(): PipelineStep[] {
        const data = this.parseApplicationData(this.application?.txtApplicationData);
        const footerFields = Array.isArray(data?.footerFields) ? data.footerFields : [];
        const templateFields = (this.savedTemplate?.payload?.fields || this.getApplicationTemplatePayload()?.fields || []) as TemplateField[];
        const hasIndividualFooter = templateFields.some((field: TemplateField) => field.type === 'individual_pipeline_footer');
        if (!hasIndividualFooter || footerFields.length === 0) {
            return [];
        }

        const attachmentPermissions = templateFields
            .filter((field: TemplateField) => this.isAttachmentField(field))
            .map((field: TemplateField) => ({
                fieldId: field.id,
                fieldLabel: field.label,
                fieldType: field.type,
                right: 'edit' as PipelineFieldRight
            }));

        const steps: PipelineStep[] = [{
            id: 'initiator',
            name: 'Initiator',
            type: 'initiator',
            approvalMode: 'OR',
            fieldPermissions: templateFields
                .filter((field: TemplateField) =>
                    !this.isDocumentRegionFieldType(field.type)
                    && field.type !== 'application_code'
                    && !this.isDynamicApprovalDataField(field)
                )
                .map((field: TemplateField) => ({
                    fieldId: field.id,
                    fieldLabel: field.label,
                    fieldType: field.type,
                    right: 'edit' as PipelineFieldRight
                })),
            fieldPermissionsConfigured: true
        }];

        [...footerFields]
            .sort((left: any, right: any) => (Number(left?.order) || 0) - (Number(right?.order) || 0))
            .forEach((section: any, sectionIndex: number) => {
                const users = Array.isArray(section?.users) ? section.users : [];
                users.forEach((user: any, userIndex: number) => {
                    const userId = Number(user?.serUserId ?? user?.userId ?? user?.id);
                    if (!Number.isFinite(userId) || userId <= 0) {
                        return;
                    }
                    steps.push({
                        id: `${section?.key || 'footer'}-${sectionIndex + 1}-${userId}-${userIndex + 1}`,
                        name: user?.txtUserName || user?.userName || user?.name || section?.label || `Approver ${steps.length}`,
                        type: 'individual',
                        approvalMode: 'OR',
                        order: steps.length + 1,
                        users: [user],
                        fieldPermissions: attachmentPermissions,
                        fieldPermissionsConfigured: true
                    });
                });
            });

        return steps;
    }

    private hasIndividualFooterPipeline(): boolean {
        return (this.template?.fields || []).some((field: TemplateField) => field.type === 'individual_pipeline_footer');
    }

    private entryBelongsToSubmission(entry: any): boolean {
        const stepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim().toLowerCase();
        const role = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        const level = Number(entry?.intApprovalOrder ?? entry?.level);
        const action = String(entry?.action || entry?.status || '').toUpperCase();
        return this.isInitiatorStepId(stepId) || role === 'submission' || action === 'SUBMITTED' || level === 1 && role === 'initiator';
    }

    private getSyntheticSubmissionEntry(existingEntries: any[] = []): any | null {
        if (!this.application?.dteCreatedDate && !this.application?.serSubmittedBy) {
            return null;
        }
        if (existingEntries.some((entry) => this.entryBelongsToSubmission(entry))) {
            return null;
        }
        return {
            action: 'SUBMITTED',
            status: 'SUBMITTED',
            level: 1,
            intApprovalOrder: 1,
            stepId: 'initiator',
            pipelineStepId: 'initiator',
            signatureTargetId: 'initiator',
            role: 'Submission',
            approverName: this.application?.submittedByUserName || 'Initiator',
            userName: this.application?.submittedByUserName || 'Initiator',
            userId: this.application?.serSubmittedBy,
            approvedBy: this.application?.serSubmittedBy,
            approvedDate: this.application?.dteCreatedDate,
            txtDepartmentName: this.resolveDepartmentName(this.parseApplicationData(this.application?.txtApplicationData)),
            txtDesignation: this.resolveDesignation(this.parseApplicationData(this.application?.txtApplicationData)),
            remarks: 'Submitted application'
        };
    }

    private getDisplayLevelForHistoryEntry(entry: any, index: number): number {
        if (this.entryBelongsToSubmission(entry)) {
            return 1;
        }
        const step = this.getStepForHistoryEntry(entry);
        if (step) {
            return this.getDisplayLevelForStep(step);
        }
        const rawLevel = this.getRawHistoryLevel(entry);
        return Number.isFinite(rawLevel) && rawLevel > 1 ? rawLevel : index + 1;
    }

    private getStepForHistoryEntry(entry: any): PipelineStep | null {
        const steps = this.getPipelineSteps().filter((step) => step.type !== 'initiator');
        const entryStepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim();
        if (entryStepId) {
            const byId = steps.find((step) => String(step.id || '').trim() === entryStepId);
            if (byId) {
                return byId;
            }
        }

        const rawLevel = this.getRawHistoryLevel(entry);
        if (rawLevel === 0) {
            return steps[0] || null;
        }

        const entryRole = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        if (entryRole) {
            const byName = steps.find((step) => String(step.name || '').trim().toLowerCase() === entryRole);
            if (byName) {
                return byName;
            }
        }

        const matchingByLevel = steps.find((step) => this.getLegacyDisplayLevelsForStep(step).includes(rawLevel));
        return matchingByLevel || null;
    }

    private getDisplayLevelForStep(step: PipelineStep): number {
        const steps = this.getPipelineSteps().filter((item) => item.type !== 'initiator');
        const fullPipeline = this.getPipelineSteps();
        const initiatorStep = fullPipeline.find((item) => item.type === 'initiator');
        const initiatorOrder = Number(initiatorStep?.order);
        const hasExplicitInitiatorOrder = Number.isFinite(initiatorOrder) && initiatorOrder > 0;
        const index = steps.findIndex((item) => item.id === step.id);
        const order = Number(step.order);
        if (Number.isFinite(order) && order > 0) {
            return hasExplicitInitiatorOrder ? order : order + 1;
        }
        return index >= 0 ? index + 2 : 2;
    }

    private getLegacyDisplayLevelsForStep(step: PipelineStep): number[] {
        const displayLevel = this.getDisplayLevelForStep(step);
        return Array.from(new Set([displayLevel - 2, displayLevel - 1, displayLevel, displayLevel + 1, displayLevel + 2]
            .filter((level) => level >= 0)));
    }

    private getRawHistoryLevel(entry: any): number {
        return Number(entry?.intApprovalOrder ?? entry?.level);
    }

    private isFirstApprovalStep(step: PipelineStep): boolean {
        const steps = this.getPipelineSteps().filter((item) => item.type !== 'initiator');
        return steps.length > 0 && steps[0]?.id === step.id;
    }

    private getUserId(value: any): number {
        return Number(value?.serUserId ?? value?.userId ?? value?.approvedBy ?? value?.approverId ?? value?.id);
    }

    private formatStepType(step: PipelineStep): string {
        if ((step as any).dynamicTarget === 'initiator_hod') {
            return 'Self HOD';
        }
        return String(step.type || 'Step').replace(/_/g, ' ');
    }

    private getStepApproverName(step: PipelineStep, history: any, configuredUser: any = null): string {
        if (step.type === 'initiator') {
            return this.application?.submittedByUserName || 'Initiator';
        }
        if (history) {
            const delegatedLabel = this.formatHistoryActorLabel(history, Number(history?.performedBy ?? history?.approvedBy ?? 0));
            if (delegatedLabel && delegatedLabel !== '--') {
                return delegatedLabel;
            }
        }
        if (history?.approverName || history?.txtUserName || history?.userName) {
            return history.approverName || history.txtUserName || history.userName;
        }
        if (configuredUser) {
            const userId = this.getUserId(configuredUser);
            return configuredUser.txtUserName || configuredUser.userName || configuredUser.name || (Number.isFinite(userId) ? `User ${userId}` : '--');
        }
        const users = this.getConfiguredStepUsers(step);
        if (users.length > 0) {
            return users.map((user) => user.txtUserName || user.userName || user.name || `User ${user.serUserId || user.userId || user.id}`).join(', ');
        }
        if ((step as any).dynamicTarget === 'initiator_hod') {
            return 'Current HOD';
        }
        return step.name || '--';
    }

    private getHistoryDate(history: any): string {
        const value = history?.approvedAt || history?.approvedDate || history?.date || history?.dteCreatedDate || history?.timestamp;
        return value ? new Date(value).toLocaleString() : '--';
    }

    private getHistoryDateObject(history: any): Date | null {
        const value = history?.approvedAt || history?.approvedDate || history?.date || history?.dteCreatedDate || history?.timestamp;
        if (!value) {
            return null;
        }
        const date = new Date(value);
        return Number.isFinite(date.getTime()) ? date : null;
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

    private doesWordEditorFieldOverflow(fieldId: string): boolean {
        const fieldElement = this.elementRef.nativeElement.querySelector(
            `.filled-field[data-field-id="${fieldId}"] .word-editor-filled-value`
        ) as HTMLElement | null;
        return !!fieldElement && (
            fieldElement.scrollHeight > fieldElement.clientHeight + 1 ||
            fieldElement.scrollWidth > fieldElement.clientWidth + 1
        );
    }

    private async renderTemplatePreviewPdfBlob(step: PipelineStep | null = this.activeStep): Promise<Blob> {
        const sourceFrame = this.elementRef.nativeElement.querySelector('.fill-frame') as HTMLElement | null;
        if (!sourceFrame) {
            throw new Error('Template preview not found');
        }
        if ((document as any).fonts?.ready) {
            try {
                await (document as any).fonts.ready;
            } catch { }
        }
        const [html2canvasModule, jsPDFModule] = await Promise.all([import('html2canvas'), import('jspdf')]);
        const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
        const jsPDF = (jsPDFModule.default || jsPDFModule) as any;
        const pageWidth = Number(this.template?.page?.width || 794);
        const pageHeight = Number(this.template?.page?.height || 1123);
        const pageGap = Number(this.template?.page?.gap || 32);
        const pageCount = Number(this.template?.page?.count || 1);
        const orientation = pageWidth > pageHeight ? 'landscape' : 'portrait';
        const pdfWidthMm = orientation === 'landscape' ? 297 : 210;
        const pdfHeightMm = orientation === 'landscape' ? 210 : 297;
        const pdf = new jsPDF({ orientation, unit: 'mm', format: 'a4', compress: true });
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
                this.applyStepVisibilityToPdfClone(clone, step);
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

    private async renderTemplatePreviewPdfBlobForStep(step: PipelineStep | null): Promise<Blob> {
        return this.renderTemplatePreviewPdfBlob(step);
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

        root.querySelectorAll('.individual-footer-preview .xyz-signatures tr.individual-footer-names-row td').forEach((node) => {
            const cell = node as HTMLElement;
            cell.style.setProperty('padding-top', '3px', 'important');
        });

        root.querySelectorAll('.individual-footer-preview .xyz-signatures tr.individual-footer-names-row td > span').forEach((node) => {
            const valueEl = node as HTMLElement;
            valueEl.style.setProperty('transform', 'translateY(-2px)', 'important');
            valueEl.style.setProperty('line-height', '1.2', 'important');
        });
    }

    private applyStepVisibilityToPdfClone(clone: HTMLElement, step: PipelineStep | null): void {
        const htmlHost = clone.querySelector('.document-html') as HTMLElement | null;
        if (htmlHost) {
            htmlHost.innerHTML = this.getFillHtml(this.template?.html || '', step);
        }

        const fields = new Map<string, TemplateField>((this.template?.fields || []).map((field: TemplateField) => [field.id, field]));
        clone.querySelectorAll<HTMLElement>('.filled-field[data-field-id]').forEach((node) => {
            const field = fields.get(node.getAttribute('data-field-id') || '');
            if (!field) {
                return;
            }
            const hidden = field.type === 'radio' || !field.placement || this.getFieldRightForStep(field, step) === 'hide';
            node.hidden = hidden;
            if (hidden) {
                node.setAttribute('hidden', '');
            } else {
                node.removeAttribute('hidden');
            }
        });

        clone.querySelectorAll<HTMLElement>('.radio-option-filled-field[data-field-id]').forEach((node) => {
            const field = fields.get(node.getAttribute('data-field-id') || '');
            const hidden = !field || field.type !== 'radio' || this.getFieldRightForStep(field, step) === 'hide';
            node.hidden = hidden;
            if (hidden) {
                node.setAttribute('hidden', '');
            } else {
                node.removeAttribute('hidden');
            }
        });
    }

    private prepareTemplatePdfFieldText(root: HTMLElement): void {
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
            if (valueEl.classList.contains('word-editor-filled-value')) {
                return;
            }
            valueEl.style.setProperty('box-sizing', 'border-box', 'important');
            valueEl.style.setProperty('display', 'block', 'important');
            valueEl.style.setProperty('height', '100%', 'important');
            valueEl.style.setProperty('line-height', '1.05', 'important');
            valueEl.style.setProperty('overflow', 'hidden', 'important');
            valueEl.style.setProperty('padding', '0', 'important');
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

    private sanitizeFilename(value: string): string {
        return String(value || 'template-form')
            .trim()
            .replace(/[^a-z0-9-_]+/gi, '-')
            .replace(/^-+|-+$/g, '')
            || 'template-form';
    }

    private getInitiatorPipelineStep(): PipelineStep | null {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline as PipelineStep[] : [];
        return steps.find((step) => step?.type === 'initiator') || null;
    }

    private stepsHaveSameVisibility(a: PipelineStep | null, b: PipelineStep | null): boolean {
        const aHidden = this.getHiddenFieldIdSet(a);
        const bHidden = this.getHiddenFieldIdSet(b);
        if (aHidden.size !== bHidden.size) {
            return false;
        }
        for (const fieldId of aHidden) {
            if (!bHidden.has(fieldId)) {
                return false;
            }
        }
        return true;
    }

    private getHiddenFieldIdSet(step: PipelineStep | null): Set<string> {
        if (!step) {
            return new Set<string>();
        }
        return new Set(
            (step.fieldPermissions || [])
                .filter((permission) => permission?.right === 'hide' && !!permission.fieldId)
                .map((permission) => permission.fieldId)
        );
    }

    private normalizeRadioValue(value: any): string {
        return String(value ?? '').trim().toLowerCase();
    }
}
