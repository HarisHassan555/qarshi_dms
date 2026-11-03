import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';
import { UserDelegationDto, UserDelegationService } from 'src/app/services/user-delegation/user-delegation.service';
import {
    SavedTemplateDefinition,
    TemplateSubmitAccessContext,
    TemplateWorkflowService,
} from 'src/app/services/template-workflow/template-workflow.service';
import { urls } from 'src/app/utils/urls';
import {
    WORD_EDITOR_CKEDITOR,
    WORD_EDITOR_CKEDITOR_CONFIG,
    normalizeWordEditorValueForCkeditor,
} from 'src/app/utils/word-editor-ckeditor.util';
import { stripEditorTableChromeFromHtml } from 'src/app/utils/word-editor-table.util';
import {
    evaluateTableFieldFormulas,
    extractTableCellValuesFromHtml,
    finalizeTableFormulaHtml,
    syncTableFormulasInHtml,
    getFormulaDefinitionFromCell,
    getFormulaEditText,
    isExcelFormulaComplete,
    isExcelFormulaText,
    isFormulaTableCell,
    parseExcelFormulaToDefinition,
    formatExcelColumnLabel,
    shiftExcelFormulaReferences,
    shiftExcelFormulaReferencesForInsertion,
    TableFormulaDefinition,
    TEMPLATE_TABLE_FORMULA_ATTR,
    validateTableFieldFormulasBeforeSubmit,
} from 'src/app/utils/template-table-formula.util';
import { applyWordEditorAlignmentClasses, getTemplateFieldNgStyle } from 'src/app/utils/word-editor-display.util';
import { normalizeDocumentBodyHtmlForDisplay } from 'src/app/utils/document-body-html.util';
import {
    resolveDocumentHeaderAddress,
    resolveDocumentHeaderBrandTitle,
    resolveDocumentHeaderLogoPath,
} from 'src/app/utils/document-header.util';
import {
    buildIndividualFooterTableBands,
    getIndividualFooterRenderedHeightPx
} from 'src/app/utils/individual-footer-pipeline.util';

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
    tableConfig?: TableFieldConfig;
    tableHtml?: string;
}

interface TableFieldConfig {
    rows: number;
    columns: number;
    headers: string[];
}

interface TableRenderCell {
    key: string;
    tagName: 'td' | 'th';
    html: string;
    editable: boolean;
    formula: boolean;
    colspan: number;
    rowspan: number;
    className: string;
    style: string;
}

interface TableRenderRow {
    cells: TableRenderCell[];
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
    private static readonly DEFAULT_MAX_ATTACHMENT_MB = 5;
    private static readonly TABLE_MIN_COLUMN_WIDTH = 56;
    private static readonly TABLE_MIN_ROW_HEIGHT = 36;
    private static readonly TABLE_VERTICAL_CHROME_HEIGHT = 36;
    private static readonly TABLE_RENDER_HARD_MAX_ROWS = 80;
    private static readonly TABLE_RENDER_HARD_MAX_COLUMNS = 30;
    private static readonly ALLOWED_ATTACHMENT_MIME_TYPES = new Set([
        'application/pdf',
        'image/webp',
        'image/png',
        'image/jpeg'
    ]);
    private static readonly ALLOWED_ATTACHMENT_EXTENSIONS = new Set(['pdf', 'webp', 'png', 'jpeg', 'jpg']);
    private static readonly INDIVIDUAL_FOOTER_MIN_HEIGHT = 136;
    private individualFooterBandsCacheKey = '';
    private individualFooterBandsCache: ReturnType<typeof buildIndividualFooterTableBands> = [];
    private documentTableBindVersion = 0;
    template: any = null;
    @ViewChild('documentHtml') documentHtmlRef?: ElementRef<HTMLElement>;
    /** Opened from template builder Preview — show document only, no submit. */
    builderPreviewMode = false;
    savedTemplate: SavedTemplateDefinition | null = null;
    /** UI-only for now: which access path the user is filling under when both own and delegated apply. */
    fillAccessMode: 'self' | 'on_behalf' = 'self';
    selectedOnBehalfUserId: number | null = null;
    userHasActiveDelegations = false;
    private activeDelegationsForCurrentUser: UserDelegationDto[] = [];
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
    isPostSubmitProcessing = false;
    isSavingDraft = false;
    showAttachmentsModal = false;
    showPortalAttachmentModal = false;
    private portalAttachmentTargetField: TemplateField | null = null;
    private attachmentSourceByFieldId: { [fieldId: string]: 'computer' | 'portal' | '' } = {};
    activeInlineWordEditorFieldId: string | null = null;
    readonly wordEditor = WORD_EDITOR_CKEDITOR;
    readonly wordEditorConfig = WORD_EDITOR_CKEDITOR_CONFIG;
    private routeTemplateId = '';
    private tableRenderRowsCache = new Map<string, { html: string; rows: TableRenderRow[] }>();
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
        private userService: UserService,
        private userDelegationService: UserDelegationService
    ) { }

    async ngOnInit(): Promise<void> {
        document.body.classList.add(TEMPLATE_FILL_HIDE_CKEDITOR_BADGE_CLASS);
        this.builderPreviewMode = this.route.snapshot.queryParamMap.get('builderPreview') === '1';
        this.loadUsers();
        await this.loadDelegateAccessFlag();
        const templateId = this.route.snapshot.paramMap.get('id');
        this.routeTemplateId = templateId || '';
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

            this.syncDelegationFillAccessAfterTemplateLoad();

            if (!this.editingApplication) {
                this.generatedApplicationCode = await this.resolveInitialApplicationCode();
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
            if (field.type === 'table') {
                field.tableHtml = this.normalizeTableHtmlForField(field, field.tableHtml);
                this.values[field.id] = this.initializeTableFieldValues(field);
            }
            this.persistedValues[field.id] = this.values[field.id];
            if (field.type === 'word_editor') {
                this.acceptedWordEditorValues[field.id] = String(this.values[field.id] || '');
            }
        });
        if (this.builderPreviewMode) {
            this.applyBuilderPreviewSampleData();
        }
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.template.html || ''));
        this.queueDocumentTableBinding();
    }

    get isEditingExistingApplication(): boolean {
        return !!this.editingApplication?.serApplicationId;
    }

    get isDraftApplication(): boolean {
        return String(this.editingApplication?.txtStatus || '').toUpperCase() === 'DRAFT';
    }

    get isSentBackForEdit(): boolean {
        if (!this.editingApplication || this.isDraftApplication) {
            return false;
        }
        const status = String(this.editingApplication?.txtStatus || '').toUpperCase();
        const currentLevel = Number(this.editingApplication?.intCurrentApprovalLevel);
        return status === 'PENDING' && Number.isFinite(currentLevel) && currentLevel < 0;
    }

    private async resolveInitialApplicationCode(): Promise<string> {
        if (!this.savedTemplate) {
            return this.getSessionTemplatePreviewCode();
        }
        try {
            return await firstValueFrom(this.templateWorkflowService.peekNextTemplateCode(this.savedTemplate));
        } catch (error) {
            console.warn('Next application code could not be loaded; using local fallback.', error);
            return this.getSessionTemplatePreviewCode();
        }
    }

    get pageIndexes(): number[] {
        const count = this.template?.page?.count || 1;
        return Array.from({ length: count }, (_, index) => index);
    }

    /** Document preview zoom (fill-stage only). */
    previewZoom = 1;
    readonly previewZoomMin = 0.5;
    readonly previewZoomMax = 2;
    readonly previewZoomStep = 0.1;

    get templateFrameWidth(): number {
        return Number(this.template?.page?.width) || 794;
    }

    get templateFrameHeight(): number {
        const pageHeight = Number(this.template?.page?.height) || 1123;
        const pageCount = Number(this.template?.page?.count) || 1;
        const gap = Number(this.template?.page?.gap) || 32;
        return pageCount * pageHeight + (pageCount - 1) * gap;
    }

    get zoomedFrameWidth(): number {
        return Math.round(this.templateFrameWidth * this.previewZoom);
    }

    get zoomedFrameHeight(): number {
        return Math.round(this.templateFrameHeight * this.previewZoom);
    }

    get previewZoomPercentLabel(): string {
        return `${Math.round(this.previewZoom * 100)}%`;
    }

    get fillFrameZoomTransform(): { [key: string]: string } {
        return {
            transform: `scale(${this.previewZoom})`,
            transformOrigin: 'top left',
            width: `${this.templateFrameWidth}px`,
            minHeight: `${this.templateFrameHeight}px`,
        };
    }

    zoomInPreview(): void {
        this.setPreviewZoom(this.previewZoom + this.previewZoomStep);
    }

    zoomOutPreview(): void {
        this.setPreviewZoom(this.previewZoom - this.previewZoomStep);
    }

    resetPreviewZoom(): void {
        this.setPreviewZoom(1);
    }

    onFillStageWheel(event: WheelEvent): void {
        if (!event.ctrlKey && !event.metaKey) {
            return;
        }
        const target = event.target as HTMLElement | null;
        if (!target?.closest('.fill-stage')) {
            return;
        }
        event.preventDefault();
        if (event.deltaY < 0) {
            this.zoomInPreview();
        } else if (event.deltaY > 0) {
            this.zoomOutPreview();
        }
    }

    private setPreviewZoom(value: number): void {
        const clamped = Math.min(this.previewZoomMax, Math.max(this.previewZoomMin, value));
        this.previewZoom = Math.round(clamped * 100) / 100;
        this.scheduleFillExcelIndicators();
    }

    get fillableFields(): TemplateField[] {
        return (this.template?.fields || []).filter((field: TemplateField) =>
            !this.isDocumentRegionFieldType(field.type) && this.canCurrentStepFillField(field)
        );
    }

    get panelFields(): TemplateField[] {
        return this.fillableFields.filter((field: TemplateField) => !this.isInlineWordEditorField(field));
    }

    get hideTestEmailAdminButton(): boolean {
        return this.routeTemplateId === '615';
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
        const bandCount = this.getIndividualFooterBands().length;
        return getIndividualFooterRenderedHeightPx(
            height,
            bandCount,
            TemplateFillComponent.INDIVIDUAL_FOOTER_MIN_HEIGHT
        );
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
        return getTemplateFieldNgStyle(field);
    }

    getFieldValueText(field: TemplateField, optionLabel?: string): string {
        if (field.type === 'application_code') {
            return this.generatedApplicationCode;
        }

        if (field.type === 'attachment') {
            return this.getAttachmentDisplayText(field);
        }

        if (field.type === 'table') {
            return '';
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

    trackTableRow(index: number): number {
        return index;
    }

    trackTableCell(_index: number, cell: TableRenderCell): string {
        return cell.key;
    }

    beginTableCellEdit(field: TemplateField, cell: TableRenderCell, event?: FocusEvent): void {
        if (!cell.formula) {
            return;
        }
        this.removeDocumentTableMenu();
        this.beginTableFormulaEdit(field, cell.key);
        const input = event?.target as HTMLInputElement | null;
        const expression = this.getTableFormulaExpression(field, cell.key);
        if (!input) {
            return;
        }
        if (input.value !== expression) {
            input.value = expression;
        }
        input.setSelectionRange(0, expression.length);
        setTimeout(() => {
            if (document.activeElement === input && input.value === expression) {
                input.setSelectionRange(0, expression.length);
            }
        });
    }

    endTableCellEdit(field: TemplateField, cell: TableRenderCell): void {
        this.endTableFormulaEdit(field, cell.key);
    }

    onTableCellInput(field: TemplateField, cell: TableRenderCell, value: string): void {
        if (cell.formula || this.isTableFormulaEditing(field, cell.key)) {
            if (!this.isTableFormulaEditing(field, cell.key)) {
                this.beginTableFormulaEdit(field, cell.key);
            }
            this.setTableFormulaExpression(field, cell.key, value);
            return;
        }
        this.setTableCellValue(field, cell.key, value);
    }

    getTableRenderRows(field: TemplateField): TableRenderRow[] {
        const html = this.normalizeTableHtmlForField(field, field.tableHtml);
        const cached = this.tableRenderRowsCache.get(field.id);
        if (cached && cached.html === html) {
            return cached.rows;
        }

        const host = document.createElement('div');
        host.innerHTML = html;
        const table = host.querySelector('table');
        if (!table) {
            return [];
        }

        const rows = Array.from(table.querySelectorAll('tr'))
            .slice(0, TemplateFillComponent.TABLE_RENDER_HARD_MAX_ROWS)
            .map((row, rowIndex) => ({
            cells: Array.from(row.children)
                .filter((cell) => ['TD', 'TH'].includes(cell.tagName))
                .slice(0, TemplateFillComponent.TABLE_RENDER_HARD_MAX_COLUMNS)
                .map((cell, cellIndex) => this.buildTableRenderCell(cell as HTMLTableCellElement, rowIndex, cellIndex))
        }));
        this.tableRenderRowsCache.set(field.id, { html, rows });
        return rows;
    }

    getTableCellValue(field: TemplateField, cellKey: string): string {
        const tableValues = this.normalizeTableCellValues(this.values[field.id]);
        return tableValues[cellKey] || '';
    }

    tableFormulaInputValue(field: TemplateField, cellKey: string): string {
        if (this.isTableFormulaEditing(field, cellKey)) {
            return this.getTableFormulaExpression(field, cellKey);
        }
        return this.getTableCellValue(field, cellKey);
    }

    isTableFormulaEditing(field: TemplateField, cellKey: string): boolean {
        return this.editingTableFormulaKey === this.tableFormulaDraftKey(field, cellKey);
    }

    beginTableFormulaEdit(field: TemplateField, cellKey: string): void {
        this.editingTableFormulaKey = this.tableFormulaDraftKey(field, cellKey);
    }

    endTableFormulaEdit(field: TemplateField, cellKey: string): void {
        const draftKey = this.tableFormulaDraftKey(field, cellKey);
        if (this.editingTableFormulaKey !== draftKey) {
            return;
        }
        const draft = this.tableFormulaDrafts.get(draftKey);
        this.editingTableFormulaKey = null;
        if (draft !== undefined && isExcelFormulaComplete(draft)) {
            this.tableFormulaDrafts.delete(draftKey);
        }
    }

    onTableFormulaInput(field: TemplateField, cellKey: string, value: string): void {
        if (!this.isTableFormulaEditing(field, cellKey)) {
            return;
        }
        this.setTableFormulaExpression(field, cellKey, value);
    }

    getTableFormulaExpression(field: TemplateField, cellKey: string): string {
        const draft = this.tableFormulaDrafts.get(this.tableFormulaDraftKey(field, cellKey));
        if (draft !== undefined) {
            return draft;
        }
        const definition = this.getFormulaDefinitionForCellKey(field, cellKey);
        return definition ? getFormulaEditText(definition) : '';
    }

    setTableFormulaExpression(field: TemplateField, cellKey: string, expression: string): void {
        const trimmed = String(expression || '').trim();
        const draftKey = this.tableFormulaDraftKey(field, cellKey);
        if (!trimmed) {
            this.tableFormulaDrafts.delete(draftKey);
            this.clearFormulaAtCellKey(field, cellKey);
            this.refreshTableFieldFormulas(field);
            this.onPanelValueChanged();
            return;
        }
        if (!isExcelFormulaText(trimmed)) {
            this.tableFormulaDrafts.delete(draftKey);
            this.clearFormulaAtCellKey(field, cellKey);
            this.setTableCellValue(field, cellKey, trimmed);
            return;
        }
        if (!isExcelFormulaComplete(trimmed)) {
            this.tableFormulaDrafts.set(draftKey, trimmed);
            return;
        }
        this.tableFormulaDrafts.set(draftKey, trimmed);
        this.setTableCellValue(field, cellKey, trimmed);
    }

    setTableCellValue(field: TemplateField, cellKey: string, value: string): void {
        const nextValue = String(value ?? '');
        const tableValues = this.normalizeTableCellValues(this.values[field.id]);
        if (isExcelFormulaText(nextValue) && isExcelFormulaComplete(nextValue)) {
            const match = /^r(\d+)c(\d+)$/.exec(cellKey);
            if (match) {
                const target = {
                    tableIndex: 1,
                    row: Number(match[1]) + 1,
                    col: Number(match[2]) + 1,
                };
                const parsed = parseExcelFormulaToDefinition(nextValue, target);
                if (parsed) {
                    field.tableHtml = finalizeTableFormulaHtml(
                        this.applyFormulaDefinitionToTableHtml(field, parsed),
                        {
                            ...tableValues,
                            [cellKey]: nextValue,
                        }
                    );
                    this.tableRenderRowsCache.delete(field.id);
                }
            }
        }
        tableValues[cellKey] = nextValue;
        this.values[field.id] = tableValues;
        this.refreshTableFieldFormulas(field);
        this.onPanelValueChanged();
    }

    private applyFormulaDefinitionToTableHtml(field: TemplateField, formula: TableFormulaDefinition): string {
        const html = this.normalizeTableHtmlForField(field, field.tableHtml);
        const host = document.createElement('div');
        host.innerHTML = html;
        const table = host.querySelector('table');
        if (!table) {
            return html;
        }
        const row = table.querySelectorAll('tr')[formula.target.row - 1];
        const cell = row?.querySelectorAll('th, td')[formula.target.col - 1] as HTMLTableCellElement | undefined;
        if (!cell) {
            return html;
        }
        cell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(formula));
        cell.classList.add('template-formula-cell');
        return host.innerHTML;
    }

    tableFieldIndicatorsDismissed = false;
    private copiedTableFieldFormula: { expression: string; display: string; sourceRow: number; sourceCol: number } | null = null;
    private editingTableFormulaKey: string | null = null;
    private tableFormulaDrafts = new Map<string, string>();

    private tableFormulaDraftKey(field: TemplateField, cellKey: string): string {
        return `${field.id}:${cellKey}`;
    }

    dismissTableFieldIndicators(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
        this.tableFieldIndicatorsDismissed = true;
    }

    getTableIndicatorColumns(field: TemplateField): string[] {
        const count = this.getTableRenderRows(field).reduce((max, row) => Math.max(max, row.cells.length), 0);
        return Array.from({ length: count }, (_, index) => formatExcelColumnLabel(index + 1));
    }

    openTableFieldMenu(field: TemplateField, cell: TableRenderCell, event: MouseEvent): void {
        if (event.type === 'contextmenu') {
            event.preventDefault();
        }
        if (!this.canCurrentStepFillField(field)) {
            return;
        }
        const anchor = event.currentTarget as HTMLTableCellElement | null;
        const table = anchor?.closest('table') as HTMLTableElement | null;
        const position = this.parseTableCellKey(cell.key);
        if (!anchor || !table || !position) {
            return;
        }
        this.openDocumentTableMenu(table, event.clientX, event.clientY, position.row, position.col, anchor, (action) => {
            if (action === 'row-above' || action === 'row-below') {
                this.addTableFieldLine(field, 'row', position.row, action === 'row-above' ? 'above' : 'below');
            } else if (action === 'column-before' || action === 'column-after') {
                this.addTableFieldLine(field, 'column', position.col, action === 'column-before' ? 'before' : 'after');
            }
        });
    }

    rememberTableFieldFormula(field: TemplateField, cell: TableRenderCell, event: ClipboardEvent): void {
        const input = event.target as HTMLInputElement | null;
        const typed = String(input?.value || '').trim();
        const expression = isExcelFormulaText(typed) ? typed : this.getTableFormulaExpression(field, cell.key);
        const position = this.parseTableCellKey(cell.key);
        if (!expression || !isExcelFormulaText(expression) || !position) {
            this.copiedTableFieldFormula = null;
            return;
        }
        const display = this.getTableCellValue(field, cell.key);
        this.copiedTableFieldFormula = {
            expression,
            display,
            sourceRow: position.row,
            sourceCol: position.col
        };
        const selected = input ? input.value.slice(input.selectionStart || 0, input.selectionEnd || 0).trim() : '';
        const showingFormula = isExcelFormulaText(typed);
        if (!showingFormula || !selected || this.normalizeCopiedFormula(selected) === this.normalizeCopiedFormula(expression)) {
            event.preventDefault();
            event.clipboardData?.setData('text/plain', expression);
            if (event.type === 'cut') {
                this.setTableFormulaExpression(field, cell.key, '');
            }
        }
    }

    pasteTableFieldFormula(field: TemplateField, cell: TableRenderCell, event: ClipboardEvent): void {
        const copied = this.copiedTableFieldFormula;
        const position = this.parseTableCellKey(cell.key);
        const pasted = String(event.clipboardData?.getData('text/plain') || '').replace(/\u00a0/g, ' ').trim();
        if (!copied || !position || !this.isCopiedTableFormulaPaste(pasted, copied)) {
            return;
        }
        const deltaRow = position.row - copied.sourceRow;
        const deltaCol = position.col - copied.sourceCol;
        if (!deltaRow && !deltaCol) {
            return;
        }
        event.preventDefault();
        const shifted = shiftExcelFormulaReferences(copied.expression, deltaRow, deltaCol);
        const draftKey = this.tableFormulaDraftKey(field, cell.key);
        this.tableFormulaDrafts.delete(draftKey);
        if (this.editingTableFormulaKey === draftKey) {
            this.editingTableFormulaKey = null;
        }
        this.setTableCellValue(field, cell.key, shifted);
        const input = event.target as HTMLInputElement | null;
        input?.blur();
    }

    private isCopiedTableFormulaPaste(
        pasted: string,
        copied: { expression: string; display: string }
    ): boolean {
        const normalized = this.normalizeCopiedFormula(pasted);
        if (!normalized) {
            return false;
        }
        if (isExcelFormulaText(pasted)) {
            return normalized === this.normalizeCopiedFormula(copied.expression);
        }
        return !!copied.display && normalized === this.normalizeCopiedFormula(copied.display);
    }

    private parseTableCellKey(cellKey: string): { row: number; col: number } | null {
        const match = /^r(\d+)c(\d+)$/.exec(cellKey);
        if (!match) {
            return null;
        }
        return { row: Number(match[1]), col: Number(match[2]) };
    }

    private addTableFieldLine(
        field: TemplateField,
        axis: 'row' | 'column',
        index: number,
        place: 'above' | 'below' | 'before' | 'after'
    ): void {
        const host = document.createElement('div');
        host.innerHTML = this.normalizeTableHtmlForField(field, field.tableHtml);
        const table = host.querySelector('table');
        if (!table || table.rows.length === 0) {
            return;
        }
        const insertAt = axis === 'row'
            ? (place === 'above' ? index : index + 1)
            : (place === 'before' ? index : index + 1);
        if (axis === 'row' && table.rows.length >= TemplateFillComponent.TABLE_RENDER_HARD_MAX_ROWS) {
            return;
        }
        const columnCount = table.rows[0]?.cells.length || 0;
        if (axis === 'column' && (!columnCount || columnCount >= TemplateFillComponent.TABLE_RENDER_HARD_MAX_COLUMNS)) {
            return;
        }
        const inlineTable = this.findInlineTableFieldTable(field.id);
        if (inlineTable && this.wouldTableLeavePage(inlineTable, axis)) {
            this.notificationService.showMessage(
                axis === 'column'
                    ? (this.isLandscapePage()
                        ? 'This column would take the table outside the page, so it was not added.'
                        : 'This column would take the table outside the page. Set the page to Landscape in the Document Builder before adding more columns.')
                    : 'This row would take the table outside the page, so it was not added.',
                'warning'
            );
            return;
        }
        if (axis === 'row') {
            const sourceRow = table.rows[Math.min(index, table.rows.length - 1)];
            const row = document.createElement('tr');
            Array.from(sourceRow.cells).forEach((sourceCell) => {
                row.appendChild(this.createBlankTableFieldCell(sourceCell));
            });
            const parent = sourceRow.parentElement || table.tBodies[0] || table;
            parent.insertBefore(row, table.rows[insertAt] || null);
        } else {
            Array.from(table.rows).forEach((row) => {
                const sourceCell = row.cells[Math.min(index, row.cells.length - 1)];
                if (!sourceCell) {
                    return;
                }
                const cell = this.createBlankTableFieldCell(sourceCell);
                const reference = row.cells[insertAt];
                if (reference) {
                    row.insertBefore(cell, reference);
                } else {
                    row.appendChild(cell);
                }
            });
        }
        this.shiftTableFieldFormulas(table, axis, insertAt);
        field.tableHtml = host.innerHTML;
        this.shiftTableFieldValues(field, axis, insertAt);
        this.persistedValues[field.id] = this.shiftTableValueMap(
            this.normalizeTableCellValues(this.persistedValues[field.id]),
            axis,
            insertAt
        );
        if (field.placement) {
            field.placement = {
                ...field.placement,
                width: field.placement.width + (axis === 'column' ? TemplateFillComponent.TABLE_MIN_COLUMN_WIDTH : 0),
                height: field.placement.height + (axis === 'row' ? TemplateFillComponent.TABLE_MIN_ROW_HEIGHT : 0)
            };
        }
        if (field.tableConfig) {
            field.tableConfig = {
                ...field.tableConfig,
                rows: axis === 'row' ? field.tableConfig.rows + 1 : field.tableConfig.rows,
                columns: axis === 'column' ? field.tableConfig.columns + 1 : field.tableConfig.columns,
                headers: axis === 'column'
                    ? [...(field.tableConfig.headers || []), '']
                    : field.tableConfig.headers
            };
        }
        this.tableRenderRowsCache.delete(field.id);
        Array.from(this.tableFormulaDrafts.keys()).forEach((key) => {
            if (key.startsWith(`${field.id}:`)) {
                this.tableFormulaDrafts.delete(key);
            }
        });
        if (this.editingTableFormulaKey?.startsWith(`${field.id}:`)) {
            this.editingTableFormulaKey = null;
        }
        this.refreshTableFieldFormulas(field);
        this.onPanelValueChanged();
    }

    private createBlankTableFieldCell(source: HTMLTableCellElement): HTMLTableCellElement {
        const cell = document.createElement('td');
        const style = source.getAttribute('style');
        if (style) {
            cell.setAttribute('style', this.cleanTableCellStyle(style));
        }
        cell.innerHTML = '&nbsp;';
        return cell;
    }

    private shiftTableFieldFormulas(table: HTMLTableElement, axis: 'row' | 'column', insertAt: number): void {
        const pivot = insertAt + 1;
        table.querySelectorAll(`[${TEMPLATE_TABLE_FORMULA_ATTR}]`).forEach((node) => {
            const cell = node as HTMLTableCellElement;
            const formula = getFormulaDefinitionFromCell(cell);
            if (!formula) {
                return;
            }
            if (formula.expression) {
                formula.expression = shiftExcelFormulaReferencesForInsertion(formula.expression, axis, pivot);
            }
            if (axis === 'row' && formula.target.row >= pivot) {
                formula.target.row += 1;
            }
            if (axis === 'column' && formula.target.col >= pivot) {
                formula.target.col += 1;
            }
            formula.sources = (formula.sources || []).map((source) => ({
                ...source,
                row: axis === 'row' && source.row >= pivot ? source.row + 1 : source.row,
                col: axis === 'column' && source.col >= pivot ? source.col + 1 : source.col
            }));
            cell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(formula));
        });
    }

    private shiftTableFieldValues(field: TemplateField, axis: 'row' | 'column', insertAt: number): void {
        this.values[field.id] = this.shiftTableValueMap(
            this.normalizeTableCellValues(this.values[field.id]),
            axis,
            insertAt
        );
    }

    private shiftTableValueMap(
        current: { [cellKey: string]: string },
        axis: 'row' | 'column',
        insertAt: number
    ): { [cellKey: string]: string } {
        const next: { [cellKey: string]: string } = {};
        Object.keys(current).forEach((key) => {
            const position = this.parseTableCellKey(key);
            if (!position) {
                return;
            }
            const row = axis === 'row' && position.row >= insertAt ? position.row + 1 : position.row;
            const col = axis === 'column' && position.col >= insertAt ? position.col + 1 : position.col;
            next[`r${row}c${col}`] = current[key];
        });
        return next;
    }

    private findInlineTableFieldTable(fieldId: string): HTMLTableElement | null {
        return this.elementRef.nativeElement.querySelector(
            `.inline-table-shell[data-field-id="${fieldId}"] table`
        ) as HTMLTableElement | null;
    }

    private getFormulaDefinitionForCellKey(field: TemplateField, cellKey: string): TableFormulaDefinition | null {
        const match = /^r(\d+)c(\d+)$/.exec(cellKey);
        if (!match) {
            return null;
        }
        const html = this.normalizeTableHtmlForField(field, field.tableHtml);
        const host = document.createElement('div');
        host.innerHTML = html;
        const table = host.querySelector('table');
        if (!table) {
            return null;
        }
        const row = table.querySelectorAll('tr')[Number(match[1])];
        const cell = row?.querySelectorAll('th, td')[Number(match[2])] as HTMLTableCellElement | undefined;
        if (!cell) {
            return null;
        }
        return getFormulaDefinitionFromCell(cell);
    }

    private clearFormulaAtCellKey(field: TemplateField, cellKey: string): void {
        const match = /^r(\d+)c(\d+)$/.exec(cellKey);
        if (!match) {
            return;
        }
        const html = this.normalizeTableHtmlForField(field, field.tableHtml);
        const host = document.createElement('div');
        host.innerHTML = html;
        const table = host.querySelector('table');
        if (!table) {
            return;
        }
        const row = table.querySelectorAll('tr')[Number(match[1])];
        const cell = row?.querySelectorAll('th, td')[Number(match[2])] as HTMLTableCellElement | undefined;
        if (!cell) {
            return;
        }
        cell.removeAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
        cell.classList.remove('template-formula-cell');
        cell.removeAttribute('title');
        cell.innerHTML = '&nbsp;';
        field.tableHtml = host.innerHTML;
        this.tableRenderRowsCache.delete(field.id);
        const tableValues = this.normalizeTableCellValues(this.values[field.id]);
        const position = /^r(\d+)c(\d+)$/.exec(cellKey);
        const cacheSuffix = position ? `:${position[1]}:${position[2]}` : '';
        Object.keys(tableValues).forEach((key) => {
            if (
                key === cellKey
                || key.endsWith(`:${cellKey}`)
                || (cacheSuffix && key.startsWith('__f:') && key.endsWith(cacheSuffix))
            ) {
                delete tableValues[key];
            }
        });
        this.values[field.id] = tableValues;
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
        this.removeDocumentTableMenu();
        if (this.fillExcelIndicatorTimer) {
            clearTimeout(this.fillExcelIndicatorTimer);
        }
        this.elementRef.nativeElement.querySelectorAll('.fill-excel-indicator-layer').forEach((node: Element) => node.remove());
        document.body.classList.remove(TEMPLATE_FILL_HIDE_CKEDITOR_BADGE_CLASS);
    }

    @HostListener('window:resize')
    onWindowResizeForExcelIndicators(): void {
        this.scheduleFillExcelIndicators();
    }

    get showFillAccessChoice(): boolean {
        return this.hasDelegatedFillAccessForCurrentForm() && this.isBothFillAccess();
    }

    get showDelegatedOnlyFillSelector(): boolean {
        return !this.showFillAccessChoice
            && this.hasDelegatedFillAccessForCurrentForm()
            && !this.hasOwnFillAccess();
    }

    private async loadDelegateAccessFlag(): Promise<void> {
        const userId = this.getCurrentUserId();
        if (!userId) {
            this.userHasActiveDelegations = false;
            this.activeDelegationsForCurrentUser = [];
            return;
        }
        try {
            const rows = await firstValueFrom(
                this.userDelegationService.getAll({ delegateUserId: userId, status: 'ACTIVE' })
            );
            this.activeDelegationsForCurrentUser = (Array.isArray(rows) ? rows : [])
                .filter((row) => this.isDelegationEffective(row));
            this.userHasActiveDelegations = this.activeDelegationsForCurrentUser.length > 0;
        } catch {
            this.userHasActiveDelegations = false;
            this.activeDelegationsForCurrentUser = [];
        }
    }

    get fillForSelfChecked(): boolean {
        return this.fillAccessMode === 'self';
    }

    get fillOnBehalfChecked(): boolean {
        return this.fillAccessMode === 'on_behalf';
    }

    get showOnBehalfUserDropdown(): boolean {
        return this.fillOnBehalfChecked && this.getDelegationParties().length > 1;
    }

    get singleOnBehalfPartyLabel(): string {
        const parties = this.getDelegationParties();
        if (parties.length !== 1) {
            return '';
        }
        const party = parties[0];
        return party.onBehalfUserName || `User #${party.serOnBehalfUserId}`;
    }

    getDelegationParties(): Array<{ serOnBehalfUserId: number; onBehalfUserName: string }> {
        const formId = this.resolveCurrentFormId();
        const allowedOnBehalfIds = this.getEffectiveOnBehalfUserIdsForForm(formId);
        if (allowedOnBehalfIds.size === 0) {
            return [];
        }
        const parties: Array<{ serOnBehalfUserId: number; onBehalfUserName: string }> = [];
        const seen = new Set<number>();
        const templateParties = Array.isArray(this.savedTemplate?.delegationOnBehalfOf)
            ? this.savedTemplate!.delegationOnBehalfOf!
            : [];
        for (const party of templateParties) {
            const onBehalfId = Number(party?.serOnBehalfUserId);
            if (!Number.isFinite(onBehalfId) || onBehalfId <= 0 || !allowedOnBehalfIds.has(onBehalfId) || seen.has(onBehalfId)) {
                continue;
            }
            seen.add(onBehalfId);
            parties.push({
                serOnBehalfUserId: onBehalfId,
                onBehalfUserName: party.onBehalfUserName || `User #${onBehalfId}`,
            });
        }
        for (const delegation of this.activeDelegationsForCurrentUser) {
            const onBehalfId = Number(delegation?.serOnBehalfUserId);
            if (!Number.isFinite(onBehalfId) || onBehalfId <= 0 || !allowedOnBehalfIds.has(onBehalfId) || seen.has(onBehalfId)) {
                continue;
            }
            seen.add(onBehalfId);
            parties.push({
                serOnBehalfUserId: onBehalfId,
                onBehalfUserName: delegation.onBehalfUserName || `User #${onBehalfId}`,
            });
        }
        return parties;
    }

    hasDelegatedFillAccessForCurrentForm(): boolean {
        return this.hasDelegatedFillAccess() && this.getDelegationParties().length > 0;
    }

    private syncDelegationFillAccessAfterTemplateLoad(): void {
        if (!this.hasDelegatedFillAccessForCurrentForm()) {
            this.fillAccessMode = 'self';
            this.selectedOnBehalfUserId = null;
            return;
        }
        this.initFillAccessSelection();
        const parties = this.getDelegationParties();
        const selectedId = Number(this.selectedOnBehalfUserId);
        if (!parties.some((party) => party.serOnBehalfUserId === selectedId)) {
            const firstId = Number(parties[0]?.serOnBehalfUserId);
            this.selectedOnBehalfUserId = Number.isFinite(firstId) && firstId > 0 ? firstId : null;
        }
    }

    private resolveCurrentFormId(): number {
        const fromTemplate = Number(this.savedTemplate?.id || this.template?.id);
        if (Number.isFinite(fromTemplate) && fromTemplate > 0) {
            return fromTemplate;
        }
        const fromRoute = Number(this.routeTemplateId);
        return Number.isFinite(fromRoute) && fromRoute > 0 ? fromRoute : 0;
    }

    private getEffectiveOnBehalfUserIdsForForm(formId: number): Set<number> {
        const allowed = new Set<number>();
        if (!formId) {
            return allowed;
        }
        for (const delegation of this.activeDelegationsForCurrentUser) {
            if (!this.isDelegationEffective(delegation) || !this.delegationIncludesForm(delegation, formId)) {
                continue;
            }
            const onBehalfId = Number(delegation.serOnBehalfUserId);
            if (Number.isFinite(onBehalfId) && onBehalfId > 0) {
                allowed.add(onBehalfId);
            }
        }
        return allowed;
    }

    private isDelegationEffective(delegation: UserDelegationDto): boolean {
        const status = String(delegation?.txtStatus || '').trim().toUpperCase();
        if (status === 'REVOKED') {
            return false;
        }
        const today = this.toLocalDateOnly(new Date());
        const validFrom = this.parseDelegationDate(delegation?.dteValidFrom);
        const validTo = this.parseDelegationDate(delegation?.dteValidTo);
        if (!validFrom || !validTo) {
            return false;
        }
        return today >= validFrom && today <= validTo;
    }

    private delegationIncludesForm(delegation: UserDelegationDto, formId: number): boolean {
        const scopeIds = Array.isArray(delegation.documentIds) && delegation.documentIds.length > 0
            ? delegation.documentIds
            : String(delegation.txtScope || '').split(',').map((part) => part.trim()).filter(Boolean);
        return scopeIds.some((id) => Number(id) === formId);
    }

    private parseDelegationDate(value: string | null | undefined): Date | null {
        if (!value) {
            return null;
        }
        const parsed = new Date(`${String(value).trim()}T00:00:00`);
        return Number.isFinite(parsed.getTime()) ? this.toLocalDateOnly(parsed) : null;
    }

    private toLocalDateOnly(value: Date): Date {
        return new Date(value.getFullYear(), value.getMonth(), value.getDate());
    }

    hasOwnFillAccess(): boolean {
        if (this.savedTemplate?.hasOwnAccess === true) {
            return true;
        }
        const mode = (this.savedTemplate?.listingAccess || '').toUpperCase();
        if (mode === 'OWN' || mode === 'BOTH') {
            return true;
        }
        return !this.hasDelegatedFillAccess();
    }

    hasDelegatedFillAccess(): boolean {
        if (this.savedTemplate?.hasDelegatedAccess === true) {
            return true;
        }
        const mode = (this.savedTemplate?.listingAccess || '').toUpperCase();
        return mode === 'DELEGATION' || mode === 'BOTH';
    }

    isBothFillAccess(): boolean {
        return this.hasOwnFillAccess() && this.hasDelegatedFillAccess();
    }

    private initFillAccessSelection(): void {
        const parties = this.getDelegationParties();
        const firstId = Number(parties[0]?.serOnBehalfUserId);
        this.selectedOnBehalfUserId = Number.isFinite(firstId) && firstId > 0 ? firstId : null;
        if (!this.hasOwnFillAccess() && this.hasDelegatedFillAccessForCurrentForm()) {
            this.fillAccessMode = 'on_behalf';
        } else {
            this.fillAccessMode = 'self';
        }
    }

    isOnBehalfFillMode(): boolean {
        if (!this.hasDelegatedFillAccessForCurrentForm()) {
            return false;
        }
        if (this.fillAccessMode === 'on_behalf' && this.selectedOnBehalfUserId != null) {
            return true;
        }
        return !this.hasOwnFillAccess() && this.hasDelegatedFillAccessForCurrentForm();
    }

    getTemplateSubmitAccessContext(): TemplateSubmitAccessContext {
        if (this.isOnBehalfFillMode() && this.selectedOnBehalfUserId != null) {
            return {
                fillAccessMode: 'on_behalf',
                onBehalfUserId: this.selectedOnBehalfUserId,
            };
        }
        return { fillAccessMode: 'self', onBehalfUserId: null };
    }

    private getEffectiveInitiatorUserId(): number | null {
        if (this.isOnBehalfFillMode()) {
            const onBehalfId = Number(this.selectedOnBehalfUserId);
            return Number.isFinite(onBehalfId) && onBehalfId > 0 ? onBehalfId : null;
        }
        return this.getCurrentUserId();
    }

    private getEffectiveInitiatorProfile(): any {
        const userId = this.getEffectiveInitiatorUserId();
        if (!userId) {
            return this.getCurrentUserProfile();
        }
        const fromDirectory = this.allUsers.find((user: any) => {
            const id = Number(user?.serUserId ?? user?.userId ?? user?.id);
            return id === userId;
        });
        if (fromDirectory) {
            return fromDirectory;
        }
        const party = this.getDelegationParties().find((row) => row.serOnBehalfUserId === userId);
        return {
            serUserId: userId,
            txtUserName: party?.onBehalfUserName || `User #${userId}`,
        };
    }

    private getEffectiveInitiatorDisplayName(): string {
        const profile = this.getEffectiveInitiatorProfile();
        return String(profile?.txtUserName || profile?.userName || profile?.name || '').trim();
    }

    private validateSubmitAccessSelection(): string | null {
        if (!this.isOnBehalfFillMode()) {
            return null;
        }
        const onBehalfId = Number(this.selectedOnBehalfUserId);
        if (!Number.isFinite(onBehalfId) || onBehalfId <= 0) {
            return 'Please select the user you are submitting on behalf of.';
        }
        const allowed = this.getDelegationParties().some((party) => party.serOnBehalfUserId === onBehalfId);
        if (!allowed) {
            return 'You are not delegated to submit on behalf of the selected user for this form.';
        }
        return null;
    }

    onFillForSelfChanged(checked: boolean): void {
        if (checked) {
            this.fillAccessMode = 'self';
        }
    }

    onFillOnBehalfChanged(checked: boolean): void {
        if (!checked) {
            return;
        }
        this.fillAccessMode = 'on_behalf';
        if (this.selectedOnBehalfUserId == null) {
            const parties = this.getDelegationParties();
            const firstId = Number(parties[0]?.serOnBehalfUserId);
            this.selectedOnBehalfUserId = Number.isFinite(firstId) && firstId > 0 ? firstId : null;
        }
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

    getAttachmentSource(fieldId: string): 'computer' | 'portal' | '' {
        return this.attachmentSourceByFieldId[fieldId] || '';
    }

    onAttachmentSourceChanged(field: TemplateField, source: 'computer' | 'portal' | ''): void {
        this.attachmentSourceByFieldId[field.id] = source || '';
        if (source === 'portal' && this.canCurrentStepFillField(field)) {
            this.openPortalAttachmentModal(field);
        }
    }

    openPortalAttachmentModal(field: TemplateField): void {
        if (!this.canCurrentStepFillField(field)) {
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
        this.setAttachmentPayloads(field, [...currentPayloads, ...payloads]);
        this.onPanelValueChanged();
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
        this.persistDocumentTableEdits();
        (this.template?.fields || []).forEach((field: TemplateField) => {
            if (field.type === 'table') {
                this.refreshTableFieldFormulas(field);
            }
        });
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.template.html || ''));
        this.queueDocumentTableBinding();
    }

    private refreshTableFieldFormulas(field: TemplateField): void {
        if (field.type !== 'table' || !field.tableHtml) {
            return;
        }
        const { values } = evaluateTableFieldFormulas(field.tableHtml, this.values[field.id]);
        this.values[field.id] = values;
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
        this.individualFooterBandsCacheKey = '';
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

    getIndividualFooterBands() {
        const cacheKey = JSON.stringify(
            (this.userPipeline || []).map((section) => ({
                label: section?.label,
                users: (section?.users || []).map((user) => user?.serUserId ?? user?.userId ?? user?.id ?? null)
            }))
        );
        if (cacheKey !== this.individualFooterBandsCacheKey) {
            this.individualFooterBandsCacheKey = cacheKey;
            this.individualFooterBandsCache = buildIndividualFooterTableBands(
                this.userPipeline,
                (section) => this.getIndividualFooterSlots(section)
            );
        }
        return this.individualFooterBandsCache;
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
        if (this.builderPreviewMode) {
            const previewSample = this.getBuilderPreviewDynamicApprovalSample(field);
            if (previewSample) {
                return previewSample;
            }
        }
        const values = this.getDynamicSignatureSlots(field)
            .map((slot) => this.getDynamicApprovalSlotValue(field, slot))
            .filter((value) => !!value);
        if (values.length > 0) {
            return values.join('\n');
        }
        return '';
    }

    async saveAsDraft(): Promise<void> {
        if (!this.savedTemplate || this.isSavingDraft || this.isSubmitting) {
            return;
        }
        this.persistDocumentTableEdits();
        (this.template?.fields || []).forEach((field: TemplateField) => {
            if (field.type === 'word_editor' && field.placement) {
                this.values[field.id] = this.acceptedWordEditorValues[field.id] || '';
            }
        });

        const accessError = this.validateSubmitAccessSelection();
        if (accessError) {
            this.notificationService.showMessage(accessError, 'danger');
            return;
        }

        this.isSavingDraft = true;
        const allowedValues = this.getAllowedSubmissionValues();
        const userPipeline = this.hasIndividualPipelineFooter() ? this.getNormalizedUserPipeline() : [];
        const submitAccess = this.getTemplateSubmitAccessContext();

        try {
            const draft = await firstValueFrom(this.templateWorkflowService.saveTemplateDraft(
                this.savedTemplate,
                allowedValues,
                userPipeline,
                {
                    applicationId: this.editingApplication?.serApplicationId,
                    formCode: this.editingApplication?.txtFormCode || this.generatedApplicationCode,
                    submitAccess,
                }
            ));
            this.generatedApplicationCode = draft.formCode || this.generatedApplicationCode;
            this.submittedCode = this.generatedApplicationCode;
            this.editingApplication = {
                ...(this.editingApplication || {}),
                serApplicationId: draft.applicationId,
                serFormId: Number(this.savedTemplate.id),
                txtFormCode: this.generatedApplicationCode,
                txtStatus: 'DRAFT',
                serSubmittedBy: this.getEffectiveInitiatorUserId()
            };
            const templateRouteId = this.routeTemplateId || this.savedTemplate.id;
            await this.router.navigate(['/template-fill', templateRouteId], {
                queryParams: { applicationId: draft.applicationId },
                replaceUrl: true
            });
            this.notificationService.showMessage(
                `Draft saved with code ${this.generatedApplicationCode}.`,
                'success'
            );
        } catch (error: any) {
            const backendMessage = error?.error?.message || error?.message || 'Draft could not be saved.';
            this.notificationService.showMessage(backendMessage, 'danger');
        } finally {
            this.isSavingDraft = false;
        }
    }

    async submitTemplate(): Promise<void> {
        if (!this.savedTemplate || this.isSubmitting || this.isPostSubmitProcessing || this.isSavingDraft) {
            return;
        }
        this.persistDocumentTableEdits();
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

        const formulaError = validateTableFieldFormulasBeforeSubmit(this.template?.fields || [], this.values);
        if (formulaError) {
            this.notificationService.showMessage(formulaError, 'danger');
            return;
        }

        const accessError = this.validateSubmitAccessSelection();
        if (accessError) {
            this.notificationService.showMessage(accessError, 'danger');
            return;
        }

        this.isSubmitting = true;
        const allowedValues = this.getAllowedSubmissionValues();
        const submitAccess = this.getTemplateSubmitAccessContext();

        try {
            const userPipeline = this.hasIndividualPipelineFooter() ? this.getNormalizedUserPipeline() : [];
            if (this.isDraftApplication) {
                const submission = await firstValueFrom(this.templateWorkflowService.submitTemplate(
                    this.savedTemplate,
                    allowedValues,
                    userPipeline,
                    this.editingApplication?.txtFormCode || this.generatedApplicationCode,
                    this.editingApplication?.serApplicationId,
                    submitAccess
                ));
                this.submittedCode = submission.code || this.generatedApplicationCode;
                this.generatedApplicationCode = this.submittedCode;
                this.initiatorSignatureApproved = true;
                this.isSubmitting = false;
                this.isPostSubmitProcessing = true;

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

                const emailResponse: any = await firstValueFrom(
                    this.templateWorkflowService.sendTemplatePostApprovalEmails(submission.id)
                );
                if (!emailResponse || emailResponse.status !== 'Success') {
                    throw new Error(emailResponse?.message || 'Approval email could not be queued');
                }
                this.notificationService.showMessage('Template application submitted successfully.', 'success');
                this.router.navigate(['/my-application', submission.id]);
                return;
            }

            if (this.isSentBackForEdit) {
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
                this.isSubmitting = false;
                this.isPostSubmitProcessing = true;

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

                const emailResponse: any = await firstValueFrom(
                    this.templateWorkflowService.sendTemplatePostApprovalEmails(this.editingApplication.serApplicationId)
                );
                if (!emailResponse || emailResponse.status !== 'Success') {
                    throw new Error(emailResponse?.message || 'Approval email could not be queued');
                }

                this.notificationService.showMessage('Application updated and resubmitted successfully.', 'success');
                this.router.navigate(['/my-application', this.editingApplication.serApplicationId]);
                return;
            }

            const submission = await firstValueFrom(this.templateWorkflowService.submitTemplate(
                this.savedTemplate,
                allowedValues,
                userPipeline,
                this.generatedApplicationCode,
                null,
                submitAccess
            ));
            this.submittedCode = submission.code || this.generatedApplicationCode;
            this.generatedApplicationCode = this.submittedCode;
            this.initiatorSignatureApproved = true;
            this.isSubmitting = false;
            this.isPostSubmitProcessing = true;

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

            const emailResponse: any = await firstValueFrom(
                this.templateWorkflowService.sendTemplatePostApprovalEmails(submission.id)
            );
            if (!emailResponse || emailResponse.status !== 'Success') {
                throw new Error(emailResponse?.message || 'Approval email could not be queued');
            }
            this.notificationService.showMessage('Template application submitted successfully.', 'success');
            this.router.navigate(['/template-list']);
        } catch (error: any) {
            this.notificationService.showMessage(error?.message || 'Template application could not be submitted.', 'danger');
        } finally {
            this.isSubmitting = false;
            this.isPostSubmitProcessing = false;
        }
    }

    private async loadEditableApplication(applicationId: number, fallbackTemplateId: string | null): Promise<void> {
        const application = await firstValueFrom(this.templateWorkflowService.getApplication(applicationId));
        const currentUserId = this.getCurrentUserId();
        const submittedBy = Number(application?.serSubmittedBy || 0);
        const currentLevel = Number(application?.intCurrentApprovalLevel);
        const status = String(application?.txtStatus || '').toUpperCase();
        const isDraft = status === 'DRAFT';
        const isSentBack = status === 'PENDING' && Number.isFinite(currentLevel) && currentLevel < 0;
        const isOwner = currentUserId != null && submittedBy === currentUserId;
        const canEdit = currentUserId != null
            && (isDraft ? (isOwner || this.isCurrentUserAdmin()) : (isOwner && isSentBack));

        if (!canEdit) {
            throw new Error('This application is not available for editing.');
        }

        const formId = String(application?.serFormId || fallbackTemplateId || '');
        this.savedTemplate = await firstValueFrom(this.templateWorkflowService.getTemplate(formId));
        this.template = this.savedTemplate?.payload || null;
        this.editingApplication = application;
        const applicationDataForHtml = this.parseApplicationData(application?.txtApplicationData);
        const savedDocumentHtml = String(
            applicationDataForHtml?.documentHtml
            || applicationDataForHtml?.templatePayload?.html
            || ''
        ).trim();
        if (savedDocumentHtml && this.template) {
            this.template.html = savedDocumentHtml;
        }
        this.generatedApplicationCode = String(application?.txtFormCode || '');
        if (isDraft && this.generatedApplicationCode) {
            this.submittedCode = this.generatedApplicationCode;
        }

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
            if (field.type === 'table') {
                field.tableHtml = this.normalizeTableHtmlForField(field, field.tableHtml);
                this.values[field.id] = this.initializeTableFieldValues(field);
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
        return TemplateFillComponent.DEFAULT_MAX_ATTACHMENT_MB;
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
        const missingFields = (this.template?.fields || [])
            .filter((field: TemplateField) =>
                field.required
                && !this.isDocumentRegionFieldType(field.type)
                && !this.isDynamicApprovalDataField(field)
                && field.type !== 'application_code'
                && this.canCurrentStepFillField(field)
                && this.isFieldValueEmpty(field, this.values[field.id])
            )
            .map((field: TemplateField) => field.label || field.placeholder || 'Unnamed field');

        const requiredIndividualFooter = (this.template?.fields || []).find((field: TemplateField) =>
            field.type === 'individual_pipeline_footer' && field.required
        );
        if (requiredIndividualFooter && this.isRequiredIndividualFooterIncomplete()) {
            missingFields.push(requiredIndividualFooter.label || requiredIndividualFooter.placeholder || 'Individual Footer Sections');
        }

        return missingFields;
    }

    private isRequiredIndividualFooterIncomplete(): boolean {
        const normalizedPipeline = this.getNormalizedUserPipeline();
        if (normalizedPipeline.length === 0) {
            return true;
        }

        return normalizedPipeline.some((section) => {
            const label = String(section?.label || '').trim();
            const users = Array.isArray(section?.users) ? section.users : [];
            return !label || users.length === 0;
        });
    }

    private getSessionTemplatePreviewCode(): string {
        const convention = this.templateWorkflowService.parseCodeConvention(this.template?.codeConvention || 'FORM-0000');
        const prefix = String(this.template?.codePrefix || convention.prefix);
        const serialLength = Number(this.template?.serialLength || convention.serialLength);
        return `${prefix}-${String(1).padStart(serialLength, '0')}`;
    }

    private applyBuilderPreviewSampleData(): void {
        if (!this.builderPreviewMode || !this.template) {
            return;
        }
        this.seedBuilderPreviewUserPipeline();
        (this.template.fields || []).forEach((field: TemplateField) => {
            if (this.isDocumentRegionFieldType(field.type) || this.isDynamicApprovalDataField(field)) {
                return;
            }
            if (field.type === 'application_code') {
                this.values[field.id] = this.generatedApplicationCode;
                this.persistedValues[field.id] = this.values[field.id];
                return;
            }
            const sampleValue = this.buildBuilderPreviewSampleValue(field);
            if (sampleValue !== undefined) {
                this.values[field.id] = sampleValue;
                this.persistedValues[field.id] = sampleValue;
            }
            if (field.type === 'word_editor') {
                this.acceptedWordEditorValues[field.id] = String(this.values[field.id] || '');
            }
        });
        this.tableRenderRowsCache.clear();
        this.individualFooterBandsCacheKey = '';
        (this.template.fields || []).forEach((field: TemplateField) => {
            if (field.type === 'table') {
                this.refreshTableFieldFormulas(field);
            }
        });
    }

    private buildBuilderPreviewSampleValue(field: TemplateField): any {
        const label = String(field.label || field.id || 'Field').trim();
        switch (field.type) {
            case 'checkbox':
                return true;
            case 'radio': {
                const options = this.getRadioOptions(field);
                return options[0] || 'Option 1';
            }
            case 'select': {
                const options = (field.options || []).filter((option) => !!String(option || '').trim());
                return options[0] || 'Sample option';
            }
            case 'integer':
            case 'number':
                return '100';
            case 'decimal':
                return '99.50';
            case 'date':
                return new Date().toISOString().slice(0, 10);
            case 'email':
                return 'sample.user@example.com';
            case 'textarea':
                return `Sample text for "${label}".`;
            case 'word_editor':
                return `<p>Sample content for <strong>${this.escapeHtml(label)}</strong>.</p>`;
            case 'attachment':
                return [{ fileName: 'sample-document.pdf', mimeType: 'application/pdf', dataUrl: '', base64: '' }];
            case 'table':
                return this.buildBuilderPreviewTableValues(field);
            default:
                return `Sample ${label}`;
        }
    }

    private buildBuilderPreviewTableValues(field: TemplateField): Record<string, string> {
        const html = this.normalizeTableHtmlForField(field, field.tableHtml);
        const tableValues = {
            ...extractTableCellValuesFromHtml(html),
            ...this.normalizeTableCellValues(this.values[field.id]),
        };
        this.getTableRenderRows(field).forEach((row, rowIndex) => {
            row.cells.forEach((cell, cellIndex) => {
                if (!cell.editable || tableValues[cell.key]) {
                    return;
                }
                const fallback = (rowIndex + 1) * 10 + cellIndex;
                tableValues[cell.key] = String(fallback);
            });
        });
        const { values } = evaluateTableFieldFormulas(html, tableValues);
        return values;
    }

    private initializeTableFieldValues(field: TemplateField): Record<string, string> {
        const html = this.normalizeTableHtmlForField(field, field.tableHtml);
        const merged = {
            ...extractTableCellValuesFromHtml(html),
            ...this.normalizeTableCellValues(this.values[field.id]),
        };
        const { values } = evaluateTableFieldFormulas(html, merged);
        return values;
    }

    private getBuilderPreviewDynamicApprovalSample(field: TemplateField): string {
        const step = this.getDynamicSignatureStep(field);
        const stepLabel = String(step?.name || field.label || 'Approver').trim();
        const today = new Date().toLocaleDateString();
        switch (field.type) {
            case 'dynamic_approver_name':
                return `${stepLabel} (Sample)`;
            case 'dynamic_approval_timestamp':
                return today;
            case 'dynamic_approver_department':
                return 'Sample Department';
            case 'dynamic_approver_designation':
                return 'Sample Designation';
            case 'dynamic_user_details':
                return [
                    `${stepLabel} (Sample)`,
                    'Sample Designation',
                    'Sample Department',
                    today
                ].join('\n');
            default:
                return '';
        }
    }

    private seedBuilderPreviewUserPipeline(): void {
        if (!this.hasIndividualPipelineFooter()) {
            return;
        }
        const steps = (Array.isArray(this.template?.pipeline) ? this.template.pipeline : [])
            .filter((step: any) => step && step.type !== 'initiator')
            .sort((left: any, right: any) => (Number(left?.order) || 0) - (Number(right?.order) || 0));
        if (steps.length === 0) {
            this.userPipeline = [{
                key: 'preview_1',
                label: 'New Field',
                order: 1,
                users: [this.getBuilderPreviewPlaceholderFooterUser()]
            }];
            return;
        }
        this.userPipeline = steps.map((step: any, index: number) => ({
            key: `preview_${index + 1}`,
            label: step.name || 'New Field',
            order: index + 1,
            users: this.getBuilderPreviewFooterUsersForStep(step)
        }));
    }

    private getBuilderPreviewFooterUsersForStep(step: any): any[] {
        const configuredUsers = Array.isArray(step?.users) ? step.users.filter((user: any) => !!user) : [];
        const dynamicTargets = Array.isArray(step?.dynamicTargets) ? step.dynamicTargets : [];
        const users = [...configuredUsers];

        if (step?.dynamicTarget === 'initiator_hod') {
            users.push({ txtUserName: 'Initiator HOD', txtDesignation: 'Head of Department', txtDepartmentName: 'Sample Department' });
        } else if (step?.dynamicTarget === 'initiator_manager') {
            users.push({ txtUserName: 'Initiator Manager', txtDesignation: 'Manager', txtDepartmentName: 'Sample Department' });
        } else if (step?.dynamicTarget === 'initiator' || dynamicTargets.includes('initiator')) {
            users.push({ txtUserName: 'Initiator', txtDesignation: 'Sample Designation', txtDepartmentName: 'Sample Department' });
        }

        if (users.length > 0) {
            return users.map((user) => this.normalizePipelineUser(user));
        }

        if (step?.type === 'department') {
            const departmentName = String(
                step?.hrTblDepartment?.txtDepartmentName
                || step?.hrTblDepartment?.departmentName
                || step?.name
                || 'Department'
            ).trim();
            return [this.getBuilderPreviewPlaceholderFooterUser({ txtUserName: departmentName })];
        }
        if (step?.type === 'role') {
            const roleName = String(
                step?.cfgTblRole?.txtRoleName
                || step?.name
                || 'Role'
            ).trim();
            return [this.getBuilderPreviewPlaceholderFooterUser({ txtUserName: roleName })];
        }

        return [this.getBuilderPreviewPlaceholderFooterUser()];
    }

    private getBuilderPreviewPlaceholderFooterUser(overrides: Partial<any> = {}): any {
        return {
            txtUserName: 'User Name',
            txtDesignation: 'User Designation',
            txtDepartmentName: 'User Department',
            ...overrides
        };
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
        const previousZoom = this.previewZoom;
        this.previewZoom = 1;
        await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));

        const sourceFrame = this.elementRef.nativeElement.querySelector('.fill-frame') as HTMLElement | null;
        if (!sourceFrame) {
            this.previewZoom = previousZoom;
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
            this.previewZoom = previousZoom;
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
        root.querySelectorAll('[data-fill-table-tools]').forEach((node) => node.remove());
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

    private queueDocumentTableBinding(): void {
        const version = ++this.documentTableBindVersion;
        setTimeout(() => {
            if (version !== this.documentTableBindVersion) {
                return;
            }
            this.bindDocumentTables();
        });
    }

    private bindDocumentTables(): void {
        const root = this.documentHtmlRef?.nativeElement;
        if (!root) {
            return;
        }
        root.querySelectorAll('table').forEach((node) => {
            const table = node as HTMLTableElement;
            if (this.isDocumentTableLocked(table)) {
                return;
            }
            this.bindDocumentTableMenu(table);
            const rows = Array.from(table.querySelectorAll('tr'));
            const headerIsLabels = !!rows[0] && this.isLabelHeaderRow(rows[0]);
            rows.forEach((row, rowIndex) => {
                Array.from(row.querySelectorAll('td, th')).forEach((cellNode) => {
                    const cell = cellNode as HTMLTableCellElement;
                    if (isFormulaTableCell(cell)) {
                        cell.classList.add('document-formula-cell');
                        cell.classList.remove('document-fill-cell');
                        cell.contentEditable = 'true';
                        return;
                    }
                    if (headerIsLabels && rowIndex === 0) {
                        cell.contentEditable = 'false';
                        return;
                    }
                    cell.contentEditable = 'true';
                    cell.classList.add('document-fill-cell');
                });
            });
            if (table.dataset['fillBound'] === '1') {
                this.recalculateDocumentTable(table);
                return;
            }
            table.dataset['fillBound'] = '1';
            table.addEventListener('input', () => this.recalculateDocumentTable(table));
            this.bindDocumentTableFormulaPaste(table);
            this.recalculateDocumentTable(table);
        });
        this.scheduleFillExcelIndicators();
    }

    private isLabelHeaderRow(row: HTMLTableRowElement): boolean {
        const cells = Array.from(row.querySelectorAll('td, th')) as HTMLTableCellElement[];
        if (cells.length === 0) {
            return false;
        }
        return cells.every((cell) => {
            if (isFormulaTableCell(cell)) {
                return false;
            }
            const text = String(cell.textContent || '').replace(/\u00a0/g, ' ').trim();
            return !!text && text !== '#ERR' && Number.isNaN(Number(text.replace(/,/g, '')));
        });
    }

    private recalculateDocumentTable(table: HTMLTableElement): void {
        this.releaseEditedDocumentFormula(table);
        const host = document.createElement('div');
        host.appendChild(table.cloneNode(true));
        const syncedHost = document.createElement('div');
        syncedHost.innerHTML = syncTableFormulasInHtml(host.innerHTML);
        const syncedTable = syncedHost.querySelector('table');
        if (!syncedTable) {
            return;
        }
        const liveCells = Array.from(table.querySelectorAll('td, th')) as HTMLTableCellElement[];
        const syncedCells = Array.from(syncedTable.querySelectorAll('td, th')) as HTMLTableCellElement[];
        liveCells.forEach((liveCell, index) => {
            const syncedCell = syncedCells[index];
            if (!syncedCell) {
                return;
            }
            const formulaAttr = syncedCell.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
            if (!formulaAttr) {
                if (liveCell.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR)) {
                    this.clearDocumentFormulaCell(liveCell);
                }
                return;
            }
            liveCell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, formulaAttr);
            liveCell.classList.add('document-formula-cell');
            liveCell.classList.remove('document-fill-cell');
            liveCell.contentEditable = 'true';
            if (document.activeElement === liveCell) {
                return;
            }
            const nextText = String(syncedCell.textContent || '').replace(/\u00a0/g, ' ').trim();
            const current = String(liveCell.textContent || '').replace(/\u00a0/g, ' ').trim();
            if (current !== nextText) {
                liveCell.textContent = nextText;
            }
        });
        this.persistDocumentTableEdits();
        this.scheduleFillExcelIndicators();
    }

    private bindDocumentTableFormulaPaste(table: HTMLTableElement): void {
        if (table.dataset['formulaPasteBound'] === '1') {
            return;
        }
        table.dataset['formulaPasteBound'] = '1';
        table.addEventListener('focusin', (event) => {
            const cell = (event.target as HTMLElement | null)?.closest('td, th') as HTMLTableCellElement | null;
            if (!cell || !table.contains(cell)) {
                return;
            }
            const expression = this.documentFormulaExpression(cell);
            if (!expression) {
                return;
            }
            if (this.documentCellPlainText(cell) !== expression) {
                cell.textContent = expression;
            }
        });
        table.addEventListener('focusout', (event) => {
            const cell = (event.target as HTMLElement | null)?.closest('td, th') as HTMLTableCellElement | null;
            const next = (event as FocusEvent).relatedTarget as Node | null;
            if (!cell || !table.contains(cell) || (next && cell.contains(next))) {
                return;
            }
            window.setTimeout(() => this.recalculateDocumentTable(table), 0);
        });
        const remember = (event: ClipboardEvent) => this.rememberCopiedDocumentFormula(table, event);
        table.addEventListener('copy', remember, true);
        table.addEventListener('cut', remember, true);
        table.addEventListener('paste', (event) => this.pasteCopiedDocumentFormula(table, event), true);
    }

    private rememberCopiedDocumentFormula(table: HTMLTableElement, event: ClipboardEvent): void {
        const cell = this.documentTableCellFromSelection(table);
        if (!cell) {
            this.copiedDocumentFormula = null;
            return;
        }
        const expression = this.documentFormulaExpression(cell);
        const source = this.documentTableCellPosition(table, cell);
        if (!expression || !source) {
            this.copiedDocumentFormula = null;
            return;
        }
        this.copiedDocumentFormula = { expression, source };
        const selected = String(window.getSelection()?.toString() || '').replace(/\u00a0/g, ' ').trim();
        const showingFormula = isExcelFormulaText(this.documentCellPlainText(cell));
        if (!showingFormula || !selected || this.normalizeCopiedFormula(selected) === this.normalizeCopiedFormula(expression)) {
            event.preventDefault();
            event.clipboardData?.setData('text/plain', expression);
            if (event.type === 'cut') {
                cell.textContent = '';
                this.clearDocumentFormulaCell(cell);
                this.recalculateDocumentTable(table);
            }
        }
    }

    private pasteCopiedDocumentFormula(table: HTMLTableElement, event: ClipboardEvent): void {
        const copied = this.copiedDocumentFormula;
        const cell = this.documentTableCellFromSelection(table);
        if (!copied || !cell || !isExcelFormulaText(copied.expression)) {
            return;
        }
        const destination = this.documentTableCellPosition(table, cell);
        if (!destination) {
            return;
        }
        const plainText = String(event.clipboardData?.getData('text/plain') || '');
        const htmlHost = document.createElement('div');
        htmlHost.innerHTML = String(event.clipboardData?.getData('text/html') || '');
        const pasted = (isExcelFormulaText(plainText) ? plainText : htmlHost.textContent || '')
            .replace(/\u00a0/g, ' ')
            .trim();
        if (!isExcelFormulaText(pasted) || this.normalizeCopiedFormula(pasted) !== this.normalizeCopiedFormula(copied.expression)) {
            return;
        }
        const deltaRow = destination.row - copied.source.row;
        const deltaCol = destination.col - copied.source.col;
        if (!deltaRow && !deltaCol) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        cell.textContent = shiftExcelFormulaReferences(copied.expression, deltaRow, deltaCol);
        cell.contentEditable = 'true';
        this.recalculateDocumentTable(table);
    }

    private releaseEditedDocumentFormula(table: HTMLTableElement): void {
        const active = document.activeElement as HTMLElement | null;
        const cell = active?.closest?.('td, th') as HTMLTableCellElement | null;
        if (!cell || !table.contains(cell) || !cell.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR)) {
            return;
        }
        if (isExcelFormulaText(this.documentCellPlainText(cell))) {
            return;
        }
        this.clearDocumentFormulaCell(cell);
    }

    private clearDocumentFormulaCell(cell: HTMLTableCellElement): void {
        cell.removeAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
        cell.classList.remove('document-formula-cell', 'template-formula-cell');
        cell.removeAttribute('title');
        cell.classList.add('document-fill-cell');
        cell.contentEditable = 'true';
    }

    private documentFormulaExpression(cell: HTMLTableCellElement): string {
        const liveText = this.documentCellPlainText(cell);
        if (isExcelFormulaText(liveText)) {
            return liveText;
        }
        const formula = getFormulaDefinitionFromCell(cell);
        const stored = formula ? getFormulaEditText(formula).trim() : '';
        return isExcelFormulaText(stored) ? stored : '';
    }

    private documentCellPlainText(cell: HTMLTableCellElement): string {
        return String(cell.textContent || '').replace(/\u00a0/g, ' ').trim();
    }

    private documentTableCellFromSelection(table: HTMLTableElement): HTMLTableCellElement | null {
        const selection = window.getSelection();
        const anchor = selection?.anchorNode;
        const element = anchor?.nodeType === Node.TEXT_NODE ? anchor.parentElement : anchor as Element | null;
        const cell = element?.closest?.('td, th') as HTMLTableCellElement | null;
        if (!cell || !table.contains(cell)) {
            return null;
        }
        return cell;
    }

    private documentTableCellPosition(table: HTMLTableElement, cell: HTMLTableCellElement): { row: number; col: number } | null {
        const row = cell.parentElement as HTMLTableRowElement | null;
        if (!row) {
            return null;
        }
        const rowIndex = Array.from(table.rows).indexOf(row);
        const columnIndex = Array.from(row.cells).indexOf(cell);
        if (rowIndex < 0 || columnIndex < 0) {
            return null;
        }
        return { row: rowIndex + 1, col: columnIndex + 1 };
    }

    private normalizeCopiedFormula(value: string): string {
        return String(value || '').replace(/\u00a0/g, ' ').replace(/\s+/g, '').toUpperCase();
    }

    private scheduleFillExcelIndicators(): void {
        if (this.fillExcelIndicatorTimer) {
            clearTimeout(this.fillExcelIndicatorTimer);
        }
        this.fillExcelIndicatorTimer = setTimeout(() => this.refreshFillExcelIndicators(), 60);
    }

    private refreshFillExcelIndicators(attempt = 0): void {
        const frame = this.elementRef.nativeElement.querySelector('.fill-frame') as HTMLElement | null;
        const root = this.documentHtmlRef?.nativeElement;
        if (!frame || !root) {
            return;
        }
        frame.querySelectorAll('.fill-excel-indicator-layer').forEach((node) => node.remove());
        if (this.fillExcelIndicatorsDismissed) {
            return;
        }
        const tables = Array.from(root.querySelectorAll('table')).filter((table) => (
            !this.isDocumentTableLocked(table as HTMLTableElement)
        )) as HTMLTableElement[];
        if (tables.length === 0) {
            return;
        }

        const layer = document.createElement('div');
        layer.className = 'fill-excel-indicator-layer';
        layer.setAttribute('data-fill-table-tools', 'excel-indicators');
        layer.style.position = 'absolute';
        layer.style.left = '0';
        layer.style.top = '0';
        layer.style.width = '100%';
        layer.style.height = '100%';
        layer.style.overflow = 'visible';
        layer.style.pointerEvents = 'none';
        layer.style.zIndex = '6';
        frame.appendChild(layer);

        const rowGutter = 22;
        const columnGutter = 16;
        let needsRetry = false;
        tables.forEach((table) => {
            const rows = Array.from(table.rows);
            const headerCells = Array.from(rows[0]?.cells || []);
            if (rows.length === 0 || headerCells.length === 0) {
                needsRetry = true;
                return;
            }
            const anchor = this.boxWithinFill(headerCells[0], frame);
            if (anchor.width < 1 || anchor.height < 1) {
                needsRetry = true;
                return;
            }
            layer.appendChild(this.createFillExcelRemoveButton(
                anchor.left - rowGutter,
                anchor.top - columnGutter,
                rowGutter,
                columnGutter
            ));
            headerCells.forEach((cell, columnIndex) => {
                const box = this.boxWithinFill(cell, frame);
                layer.appendChild(this.createFillExcelLabel(
                    formatExcelColumnLabel(columnIndex + 1),
                    box.left,
                    box.top - columnGutter,
                    Math.max(box.width, 1),
                    columnGutter
                ));
            });
            rows.forEach((row, rowIndex) => {
                const firstCell = row.cells[0];
                if (!firstCell) {
                    return;
                }
                const box = this.boxWithinFill(firstCell, frame);
                if (box.height < 1) {
                    needsRetry = true;
                    return;
                }
                layer.appendChild(this.createFillExcelLabel(
                    String(rowIndex + 1),
                    box.left - rowGutter,
                    box.top,
                    rowGutter,
                    box.height
                ));
            });
        });

        if (needsRetry && attempt < 8) {
            setTimeout(() => this.refreshFillExcelIndicators(attempt + 1), 120);
        }
    }

    private createFillExcelLabel(text: string, left: number, top: number, width: number, height: number): HTMLElement {
        const label = document.createElement('span');
        label.className = 'fill-excel-label';
        label.textContent = text;
        label.style.position = 'absolute';
        label.style.left = `${left}px`;
        label.style.top = `${top}px`;
        label.style.width = `${width}px`;
        label.style.height = `${height}px`;
        label.style.boxSizing = 'border-box';
        label.style.display = 'flex';
        label.style.alignItems = 'center';
        label.style.justifyContent = 'center';
        label.style.background = '#f8fafc';
        label.style.border = '1px solid #e2e8f0';
        label.style.color = '#94a3b8';
        label.style.fontFamily = 'Arial, sans-serif';
        label.style.fontSize = '10px';
        label.style.fontWeight = '600';
        label.style.lineHeight = '1';
        label.style.pointerEvents = 'none';
        return label;
    }

    private createFillExcelRemoveButton(left: number, top: number, width: number, height: number): HTMLButtonElement {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'fill-excel-remove';
        button.title = 'Remove row and column labels';
        button.setAttribute('aria-label', 'Remove row and column labels');
        button.textContent = '×';
        button.style.position = 'absolute';
        button.style.left = `${left}px`;
        button.style.top = `${top}px`;
        button.style.width = `${width}px`;
        button.style.height = `${height}px`;
        button.style.boxSizing = 'border-box';
        button.style.display = 'flex';
        button.style.alignItems = 'center';
        button.style.justifyContent = 'center';
        button.style.margin = '0';
        button.style.padding = '0';
        button.style.background = '#f8fafc';
        button.style.border = '1px solid #e2e8f0';
        button.style.color = '#94a3b8';
        button.style.fontFamily = 'Arial, sans-serif';
        button.style.fontSize = '12px';
        button.style.fontWeight = '600';
        button.style.lineHeight = '1';
        button.style.cursor = 'pointer';
        button.style.pointerEvents = 'auto';
        button.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            this.fillExcelIndicatorsDismissed = true;
            this.elementRef.nativeElement.querySelectorAll('.fill-excel-indicator-layer').forEach((node: Element) => node.remove());
        });
        return button;
    }

    private boxWithinFill(element: HTMLElement, ancestor: HTMLElement): { left: number; top: number; width: number; height: number } {
        const ancestorRect = ancestor.getBoundingClientRect();
        const rect = element.getBoundingClientRect();
        const unscaledWidth = ancestor.offsetWidth || ancestorRect.width || 1;
        const unscaledHeight = ancestor.offsetHeight || ancestorRect.height || 1;
        const scaleX = ancestorRect.width / unscaledWidth || 1;
        const scaleY = ancestorRect.height / unscaledHeight || 1;
        return {
            left: (rect.left - ancestorRect.left) / scaleX,
            top: (rect.top - ancestorRect.top) / scaleY,
            width: rect.width / scaleX,
            height: rect.height / scaleY,
        };
    }

    private isDocumentTableLocked(table: HTMLTableElement): boolean {
        return !!table.closest(
            '.word-editor-value, .inline-filled-word-editor-value, .inline-word-editor-shell, .filled-field, .document-footer-preview, .individual-footer-preview'
        ) || !!table.parentElement?.closest('table');
    }

    private fillExcelIndicatorTimer: ReturnType<typeof setTimeout> | null = null;
    private fillExcelIndicatorsDismissed = false;
    private documentTableMenu: HTMLElement | null = null;
    private copiedDocumentFormula: { expression: string; source: { row: number; col: number } } | null = null;
    private readonly dismissDocumentTableMenu = (event?: Event) => {
        if (event && this.documentTableMenu?.contains(event.target as Node)) {
            return;
        }
        this.removeDocumentTableMenu();
    };
    private readonly dismissDocumentTableMenuOnEscape = (event: Event) => {
        if ((event as KeyboardEvent).key === 'Escape') {
            this.removeDocumentTableMenu();
        }
    };

    private bindDocumentTableMenu(table: HTMLTableElement): void {
        if (this.builderPreviewMode || table.dataset['fillMenuBound'] === '1') {
            return;
        }
        table.dataset['fillMenuBound'] = '1';
        table.title = 'Click a cell to add a row or column at that position';
        const openFromClick = (event: Event) => {
            const mouseEvent = event as MouseEvent;
            const cell = (mouseEvent.target as HTMLElement | null)?.closest('td, th') as HTMLTableCellElement | null;
            if (!cell || !table.contains(cell)) {
                return;
            }
            const row = cell.parentElement as HTMLTableRowElement | null;
            const rowIndex = row ? Array.from(table.rows).indexOf(row) : 0;
            const columnIndex = row ? Array.from(row.cells).indexOf(cell) : 0;
            this.openDocumentTableMenu(table, mouseEvent.clientX, mouseEvent.clientY, rowIndex, columnIndex, cell);
        };
        table.addEventListener('click', openFromClick);
        table.addEventListener('keydown', (event) => {
            const keyEvent = event as KeyboardEvent;
            if (keyEvent.key === 'Escape' || keyEvent.key === 'Shift' || keyEvent.key === 'Control' || keyEvent.key === 'Alt' || keyEvent.key === 'Meta') {
                return;
            }
            this.removeDocumentTableMenu();
        });
        table.addEventListener('beforeinput', () => this.removeDocumentTableMenu());
    }

    private openDocumentTableMenu(
        table: HTMLTableElement,
        x: number,
        y: number,
        rowIndex: number,
        columnIndex: number,
        cell: HTMLTableCellElement,
        onAction?: (action: string) => void
    ): void {
        this.removeDocumentTableMenu();
        table.querySelectorAll('.document-table-target-cell').forEach((node) => {
            node.classList.remove('document-table-target-cell');
        });
        cell.classList.add('document-table-target-cell');
        const menu = document.createElement('div');
        menu.className = 'fill-table-context-menu';
        menu.setAttribute('data-fill-table-tools', 'true');
        menu.innerHTML = [
            '<button type="button" data-table-action="row-above">Add row above</button>',
            '<button type="button" data-table-action="row-below">Add row below</button>',
            '<button type="button" data-table-action="column-before">Add column before</button>',
            '<button type="button" data-table-action="column-after">Add column after</button>'
        ].join('');
        Object.assign(menu.style, {
            position: 'fixed',
            zIndex: '20000',
            display: 'flex',
            flexDirection: 'column',
            minWidth: '168px',
            padding: '4px',
            background: '#ffffff',
            border: '1px solid #cbd5e1',
            borderRadius: '8px',
            boxShadow: '0 10px 24px rgba(15, 23, 42, 0.18)',
            fontFamily: 'Arial, sans-serif'
        });
        menu.querySelectorAll('button').forEach((button) => {
            Object.assign((button as HTMLButtonElement).style, {
                background: 'transparent',
                border: '0',
                borderRadius: '6px',
                color: '#172033',
                cursor: 'pointer',
                font: '600 13px Arial, sans-serif',
                padding: '8px 10px',
                textAlign: 'left'
            });
        });
        menu.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            const action = (event.target as HTMLElement | null)?.closest('button')?.getAttribute('data-table-action');
            this.removeDocumentTableMenu();
            if (!action) {
                return;
            }
            if (onAction) {
                onAction(action);
                return;
            }
            if (action === 'row-above' || action === 'row-below') {
                this.addDocumentTableRow(table, rowIndex, action === 'row-above' ? 'above' : 'below');
            } else if (action === 'column-before' || action === 'column-after') {
                this.addDocumentTableColumn(table, columnIndex, action === 'column-before' ? 'before' : 'after');
            }
        });
        document.body.appendChild(menu);
        const menuRect = menu.getBoundingClientRect();
        const cellRect = cell.getBoundingClientRect();
        const left = Math.max(8, Math.min(cellRect.left, window.innerWidth - menuRect.width - 8));
        const preferredTop = cellRect.bottom + 6;
        const top = preferredTop + menuRect.height > window.innerHeight - 8
            ? Math.max(8, cellRect.top - menuRect.height - 6)
            : preferredTop;
        menu.style.left = `${left}px`;
        menu.style.top = `${top}px`;
        this.documentTableMenu = menu;
        setTimeout(() => {
            document.addEventListener('mousedown', this.dismissDocumentTableMenu, true);
            document.addEventListener('keydown', this.dismissDocumentTableMenuOnEscape, true);
        });
    }

    removeDocumentTableMenu(): void {
        document.removeEventListener('mousedown', this.dismissDocumentTableMenu, true);
        document.removeEventListener('keydown', this.dismissDocumentTableMenuOnEscape, true);
        this.documentTableMenu?.remove();
        this.documentTableMenu = null;
        this.elementRef.nativeElement.querySelectorAll('.document-table-target-cell').forEach((node: Element) => {
            node.classList.remove('document-table-target-cell');
        });
    }

    private addDocumentTableRow(table: HTMLTableElement, rowIndex = table.rows.length - 1, place: 'above' | 'below' = 'below'): void {
        const sourceRow = table.rows[rowIndex] || table.rows[table.rows.length - 1];
        if (!sourceRow || table.rows.length >= TemplateFillComponent.TABLE_RENDER_HARD_MAX_ROWS) {
            return;
        }
        if (this.wouldTableLeavePage(table, 'row')) {
            this.notificationService.showMessage(
                'This row would take the table outside the page, so it was not added.',
                'warning'
            );
            return;
        }
        const row = document.createElement('tr');
        Array.from(sourceRow.cells).forEach((sourceCell) => {
            row.appendChild(this.createBlankDocumentTableCell(sourceCell));
        });
        const parent = sourceRow.parentElement || table.tBodies[0] || table;
        parent.insertBefore(row, place === 'above' ? sourceRow : sourceRow.nextSibling);
        this.replaceStoredDocumentTable(table);
        this.scheduleFillExcelIndicators();
    }

    private addDocumentTableColumn(
        table: HTMLTableElement,
        columnIndex = Math.max(0, (table.rows[0]?.cells.length || 1) - 1),
        place: 'before' | 'after' = 'after'
    ): void {
        const columnCount = table.rows[0]?.cells.length || 0;
        if (!columnCount || columnCount >= TemplateFillComponent.TABLE_RENDER_HARD_MAX_COLUMNS) {
            return;
        }
        if (this.wouldTableLeavePage(table, 'column')) {
            this.notificationService.showMessage(
                this.isLandscapePage()
                    ? 'This column would take the table outside the page, so it was not added.'
                    : 'This column would take the table outside the page. Set the page to Landscape in the Document Builder before adding more columns.',
                'warning'
            );
            return;
        }
        const insertAt = place === 'before' ? columnIndex : columnIndex + 1;
        Array.from(table.rows).forEach((row) => {
            const sourceCell = row.cells[Math.min(columnIndex, row.cells.length - 1)];
            if (!sourceCell) {
                return;
            }
            const cell = this.createBlankDocumentTableCell(sourceCell);
            const reference = row.cells[insertAt];
            if (reference) {
                row.insertBefore(cell, reference);
            } else {
                row.appendChild(cell);
            }
        });
        this.replaceStoredDocumentTable(table);
        this.scheduleFillExcelIndicators();
    }

    private isLandscapePage(): boolean {
        const page = this.template?.page || {};
        const width = Number(page.width) || 794;
        const height = Number(page.height) || 1123;
        return String(page.orientation || '').toLowerCase() === 'landscape' || width > height;
    }

    private wouldTableLeavePage(table: HTMLTableElement, change: 'row' | 'column'): boolean {
        const pageBox = this.getTablePageBox(table);
        if (!pageBox) {
            return false;
        }
        const tableBox = table.getBoundingClientRect();
        if (change === 'column') {
            const sample = table.rows[0]?.cells[table.rows[0].cells.length - 1];
            const extra = sample ? sample.getBoundingClientRect().width : 48;
            return tableBox.right + extra > pageBox.right + 1;
        }
        const sampleRow = table.rows[table.rows.length - 1];
        const extra = sampleRow ? sampleRow.getBoundingClientRect().height : 28;
        return tableBox.bottom + extra > pageBox.bottom + 1;
    }

    private getTablePageBox(table: HTMLTableElement): DOMRect | null {
        const frame = table.closest('.fill-frame');
        if (!frame) {
            return null;
        }
        const tableBox = table.getBoundingClientRect();
        const boundaries = Array.from(frame.querySelectorAll('.page-boundary')) as HTMLElement[];
        const current = boundaries.find((boundary) => {
            const box = boundary.getBoundingClientRect();
            return tableBox.top < box.bottom - 1 && tableBox.bottom > box.top + 1;
        });
        return (current || boundaries[0])?.getBoundingClientRect() || null;
    }

    private createBlankDocumentTableCell(source: HTMLTableCellElement): HTMLTableCellElement {
        const cell = document.createElement(source.tagName.toLowerCase() === 'th' ? 'th' : 'td');
        const style = source.getAttribute('style');
        if (style) {
            cell.setAttribute('style', style);
        }
        cell.innerHTML = '&nbsp;';
        cell.contentEditable = 'true';
        cell.classList.add('document-fill-cell');
        return cell;
    }

    private replaceStoredDocumentTable(liveTable: HTMLTableElement): void {
        const root = this.documentHtmlRef?.nativeElement;
        if (!root || !this.template?.html) {
            return;
        }
        const doc = new DOMParser().parseFromString(String(this.template.html), 'text/html');
        const storedTables = Array.from(doc.body.querySelectorAll('table'));
        const liveTables = Array.from(root.querySelectorAll('table'));
        const storedTable = storedTables[liveTables.indexOf(liveTable)];
        if (!storedTable) {
            return;
        }
        const cleanTable = liveTable.cloneNode(true) as HTMLTableElement;
        cleanTable.removeAttribute('data-fill-bound');
        cleanTable.querySelectorAll('td, th').forEach((node) => {
            const cell = node as HTMLTableCellElement;
            cell.removeAttribute('contenteditable');
            cell.classList.remove('document-fill-cell', 'document-formula-cell');
            if (!cell.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR)) {
                cell.classList.remove('template-formula-cell');
                cell.textContent = String(cell.textContent || '').replace(/\u00a0/g, ' ').trim();
            }
        });
        storedTable.replaceWith(cleanTable);
        this.template.html = finalizeTableFormulaHtml(doc.body.innerHTML);
    }

    private persistDocumentTableEdits(): void {
        const root = this.documentHtmlRef?.nativeElement;
        if (!root || !this.template?.html) {
            return;
        }
        const doc = new DOMParser().parseFromString(String(this.template.html), 'text/html');
        const storedTables = Array.from(doc.body.querySelectorAll('table'));
        const liveTables = Array.from(root.querySelectorAll('table'));
        if (storedTables.length === 0 || liveTables.length === 0) {
            return;
        }
        liveTables.forEach((liveTable, tableIndex) => {
            const storedTable = storedTables[tableIndex];
            if (!storedTable) {
                return;
            }
            const liveCells = Array.from(liveTable.querySelectorAll('td, th'));
            const storedCells = Array.from(storedTable.querySelectorAll('td, th'));
            if (liveCells.length !== storedCells.length) {
                return;
            }
            liveCells.forEach((liveCell, index) => {
                const storedCell = storedCells[index] as HTMLTableCellElement | undefined;
                if (!storedCell) {
                    return;
                }
                const liveElement = liveCell as HTMLTableCellElement;
                const formulaAttr = liveElement.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
                if (formulaAttr) {
                    storedCell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, formulaAttr);
                    storedCell.classList.add('template-formula-cell');
                } else {
                    storedCell.removeAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
                    storedCell.classList.remove('template-formula-cell');
                }
                const text = String(liveCell.textContent || '').replace(/\u00a0/g, ' ').trim();
                if (formulaAttr && isExcelFormulaText(text)) {
                    return;
                }
                storedCell.textContent = text;
            });
        });
        this.template.html = finalizeTableFormulaHtml(doc.body.innerHTML);
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

        return normalizeDocumentBodyHtmlForDisplay(finalizeTableFormulaHtml(doc.body.innerHTML));
    }

    private getCurrentStepFieldRight(field: TemplateField): PipelineFieldRight | 'view' {
        if (this.builderPreviewMode) {
            return this.hasIndividualFooterPipeline() ? 'edit' : 'fill';
        }
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
        if (field.type === 'table') {
            const tableValues = this.normalizeTableCellValues(value);
            return this.getTableRenderRows(field)
                .filter((row) => row.cells.some((cell) => cell.editable))
                .every((row) => row.cells
                    .filter((cell) => cell.editable)
                    .every((cell) => String(tableValues[cell.key] || '').trim() === ''));
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
        const initiatorProfile = this.getEffectiveInitiatorProfile();
        const initiatorName = this.getEffectiveInitiatorDisplayName();
        if (!this.initiatorSignatureApproved) {
            return [{
                label: initiatorName || 'Initiator',
                txtUserName: initiatorName || '',
                txtDepartmentName: this.resolveDepartmentName(initiatorProfile),
                txtDesignation: this.resolveDesignation(initiatorProfile)
            }];
        }
        const userId = this.getEffectiveInitiatorUserId();
        const submissionTimestamp = this.editingApplication?.dteCreatedDate || new Date().toISOString();
        return userId ? [{
            serUserId: userId,
            txtUserName: initiatorName || 'Initiator',
            txtDepartmentName: this.resolveDepartmentName(initiatorProfile),
            txtDesignation: this.resolveDesignation(initiatorProfile),
            approvedDate: submissionTimestamp,
            approvedAt: submissionTimestamp,
            dteCreatedDate: submissionTimestamp,
            __signatureApproved: true
        }] : [{
            label: initiatorName || 'Initiator',
            txtUserName: initiatorName || '',
            txtDepartmentName: this.resolveDepartmentName(initiatorProfile),
            txtDesignation: this.resolveDesignation(initiatorProfile)
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

    private isCurrentUserAdmin(): boolean {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            return this.isAdminUser(user);
        } catch {
            return false;
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
            const initiatorName = this.getEffectiveInitiatorDisplayName();
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

        applyWordEditorAlignmentClasses(qlEditor);

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

    private getTableColumnCount(field: TemplateField): number {
        return this.normalizeTableConfig(field.tableConfig).columns;
    }

    private buildTableRenderCell(cell: HTMLTableCellElement, rowIndex: number, cellIndex: number): TableRenderCell {
        const html = this.cleanTableCellHtml(cell.innerHTML);
        const key = `r${rowIndex}c${cellIndex}`;
        const hasFormula = isFormulaTableCell(cell);
        return {
            key,
            tagName: cell.tagName.toLowerCase() === 'th' ? 'th' : 'td',
            html,
            editable: hasFormula ? false : !this.hasMeaningfulTableCellContent(cell, html),
            formula: hasFormula,
            colspan: Math.max(1, Number(cell.getAttribute('colspan') || cell.colSpan || 1)),
            rowspan: Math.max(1, Number(cell.getAttribute('rowspan') || cell.rowSpan || 1)),
            className: this.cleanTableCellClass(cell.getAttribute('class') || ''),
            style: this.cleanTableCellStyle(cell.getAttribute('style') || '')
        };
    }

    private hasMeaningfulTableCellContent(cell: HTMLTableCellElement, cleanedHtml?: string): boolean {
        const text = String(cell.textContent || '').replace(/\u00a0/g, ' ').trim();
        if (text && text !== '#ERR' && text !== '—' && text !== '-') {
            return true;
        }
        const html = String(cleanedHtml ?? this.cleanTableCellHtml(cell.innerHTML))
            .replace(/&nbsp;/gi, '')
            .replace(/<br\s*\/?>/gi, '')
            .replace(/<\/?p[^>]*>/gi, '')
            .replace(/<[^>]+>/g, '')
            .trim();
        return !!html;
    }

    private cleanTableCellHtml(value: any): string {
        const host = document.createElement('div');
        host.innerHTML = String(value || '');
        host.querySelectorAll('[data-cke-filler], br[data-cke-filler]').forEach((node) => node.remove());
        host.querySelectorAll('.ck-table-bogus-paragraph').forEach((node) => {
            const element = node as HTMLElement;
            if (!String(element.textContent || '').replace(/\u00a0/g, ' ').trim()) {
                element.remove();
            }
        });
        return host.innerHTML.trim();
    }

    private cleanTableCellClass(value: string): string {
        return String(value || '')
            .split(/\s+/)
            .filter((className) => className && !className.startsWith('ck-'))
            .join(' ');
    }

    private cleanTableCellStyle(value: string): string {
        return String(value || '')
            .split(';')
            .map((rule) => rule.trim())
            .filter((rule) => {
                if (!rule) return false;
                const property = rule.split(':')[0]?.trim().toLowerCase();
                return !!property && !property.startsWith('--ck-');
            })
            .join('; ');
    }

    private normalizeTableHtml(value: any): string {
        const html = stripEditorTableChromeFromHtml(String(value || '').trim());
        if (/<table[\s>]/i.test(html)) {
            const host = document.createElement('div');
            host.innerHTML = html;
            host.querySelectorAll('[data-cke-filler], br[data-cke-filler]').forEach((node) => node.remove());
            host.querySelectorAll('.ck-widget__selection-handle, .ck-table-column-resizer, .ck-table-bogus-paragraph').forEach((node) => {
                if (!String(node.textContent || '').replace(/\u00a0/g, ' ').trim()) {
                    node.remove();
                }
            });
            return host.innerHTML;
        }
        return /<table[\s>]/i.test(html)
            ? html
            : '<figure class="table"><table><tbody><tr><td>Header 1</td><td>Header 2</td></tr><tr><td></td><td></td></tr></tbody></table></figure>';
    }

    private normalizeTableHtmlForField(field: TemplateField, value: any): string {
        const html = this.normalizeTableHtml(value);
        return this.trimTableHtmlForField(field, html);
    }

    private trimTableHtmlForField(field: TemplateField, html: string): string {
        const limits = this.getTableRenderLimits(field);
        const host = document.createElement('div');
        host.innerHTML = html;
        const table = host.querySelector('table');
        if (!table) {
            return html;
        }

        Array.from(table.querySelectorAll('tr')).forEach((row, rowIndex) => {
            if (rowIndex >= limits.rows) {
                row.remove();
                return;
            }
            let columnCount = 0;
            Array.from(row.children).forEach((cell) => {
                if (!['TD', 'TH'].includes(cell.tagName)) {
                    cell.remove();
                    return;
                }
                const colSpan = Math.max(1, Number((cell as HTMLTableCellElement).colSpan || cell.getAttribute('colspan') || 1));
                columnCount += Number.isFinite(colSpan) ? colSpan : 1;
                if (columnCount > limits.columns) {
                    cell.remove();
                }
            });
        });

        return host.innerHTML || html;
    }

    private getTableRenderLimits(field: TemplateField): { rows: number; columns: number } {
        const placement = field.placement;
        if (!placement) {
            return {
                rows: TemplateFillComponent.TABLE_RENDER_HARD_MAX_ROWS,
                columns: TemplateFillComponent.TABLE_RENDER_HARD_MAX_COLUMNS
            };
        }

        const usableWidth = Math.max(0, Number(placement.width || 0) - 12);
        const usableHeight = Math.max(0, Number(placement.height || 0) - TemplateFillComponent.TABLE_VERTICAL_CHROME_HEIGHT);
        return {
            rows: Math.min(
                TemplateFillComponent.TABLE_RENDER_HARD_MAX_ROWS,
                Math.max(1, Math.floor(usableHeight / TemplateFillComponent.TABLE_MIN_ROW_HEIGHT))
            ),
            columns: Math.min(
                TemplateFillComponent.TABLE_RENDER_HARD_MAX_COLUMNS,
                Math.max(1, Math.floor(usableWidth / TemplateFillComponent.TABLE_MIN_COLUMN_WIDTH))
            )
        };
    }

    private normalizeTableCellValues(value: any): { [cellKey: string]: string } {
        if (!value || Array.isArray(value) || typeof value !== 'object') {
            return {};
        }
        return Object.keys(value).reduce((acc: { [cellKey: string]: string }, key) => {
            acc[key] = String(value[key] ?? '');
            return acc;
        }, {});
    }

    private normalizeTableConfig(config?: Partial<TableFieldConfig>): TableFieldConfig {
        const columns = this.normalizeTableSize(config?.columns, 1, 20);
        const rows = this.normalizeTableSize(config?.rows, 1, 50);
        const headers = Array.from({ length: columns }, (_, index) => {
            const existing = String(config?.headers?.[index] || '').trim();
            return existing || `Column ${index + 1}`;
        });
        return { rows, columns, headers };
    }

    private normalizeTableValue(field: TemplateField, value: any): string[][] {
        const config = this.normalizeTableConfig(field.tableConfig);
        const sourceRows = Array.isArray(value) ? value : [];
        return Array.from({ length: Math.max(config.rows, sourceRows.length, 1) }, (_, rowIndex) => {
            const sourceRow = Array.isArray(sourceRows[rowIndex]) ? sourceRows[rowIndex] : [];
            return Array.from({ length: config.columns }, (_, columnIndex) => String(sourceRow[columnIndex] ?? ''));
        });
    }

    private isNormalizedTableValue(field: TemplateField, value: any): value is string[][] {
        const config = this.normalizeTableConfig(field.tableConfig);
        return Array.isArray(value)
            && value.length >= Math.max(config.rows, 1)
            && value.every((row) => Array.isArray(row) && row.length === config.columns);
    }

    private normalizeTableSize(value: any, min: number, max: number): number {
        const numeric = Math.round(Number(value));
        if (!Number.isFinite(numeric)) {
            return min;
        }
        return Math.min(Math.max(numeric, min), max);
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
