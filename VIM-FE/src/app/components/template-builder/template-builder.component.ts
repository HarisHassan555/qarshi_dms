import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PDFDocument } from 'pdf-lib';
import { EditorConfig } from 'ckeditor5';
import { DepartmentService } from 'src/app/services/department/department.service';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';
import { DocumentHeaderBrandingService } from 'src/app/services/document-header-branding/document-header-branding.service';
import {
    WORD_EDITOR_CKEDITOR,
} from 'src/app/utils/word-editor-ckeditor.util';
import { TEMPLATE_BUILDER_WORD_EDITOR_CONFIG } from 'src/app/utils/template-builder-word-editor.config';
import {
    finalizeTableFormulaHtml,
    formatExcelColumnLabel,
    isTableFormulaSyncHtmlSafe,
    getFormulaDefinitionFromCell,
    getFormulaEditText,
    getTableFormulaCellRefFromElement,
    isExcelFormulaComplete,
    isExcelFormulaText,
    shiftExcelFormulaReferences,
    isSameTableCellRef,
    readTableCellPlainText,
    TableFormulaCellRef,
    TableFormulaDefinition,
    TEMPLATE_TABLE_FORMULA_ATTR,
} from 'src/app/utils/template-table-formula.util';
import {
    resolveDocumentHeaderAddress,
    resolveDocumentHeaderBrandTitle,
    resolveDocumentHeaderLogoPath,
} from 'src/app/utils/document-header.util';
import { buildIndividualFooterTableBands } from 'src/app/utils/individual-footer-pipeline.util';
import { getTemplateFieldNgStyle } from 'src/app/utils/word-editor-display.util';
import { WorkflowPipelineService } from 'src/app/services/workflow-pipeline/workflow-pipeline.service';
import { WorkflowPipelineListItem } from 'src/app/components/workflow-pipelines/workflow-pipelines.demo-data';

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
type PipelineStepType = 'initiator' | 'department' | 'individual' | 'role';
type PipelineApprovalMode = 'AND' | 'OR';
type PipelineDynamicTarget = 'initiator_hod' | 'initiator_manager' | 'initiator';
type PipelineFieldRight = 'fill' | 'edit' | 'hide';
const TEMPLATE_BUILDER_HIDE_CKEDITOR_BADGE_CLASS = 'template-builder-hide-ckeditor-badge';

interface TemplateFieldPlacement {
    x: number;
    y: number;
    width: number;
    height: number;
}

interface RadioOptionPlacement {
    id: string;
    label: string;
    page: number;
    placement: TemplateFieldPlacement;
}

interface TableFieldConfig {
    rows: number;
    columns: number;
    headers: string[];
}

interface TemplateFieldStyle {
    fontSize: number;
    bold: boolean;
    italic: boolean;
    underline: boolean;
    fontFamily: string;
    textAlign: 'left' | 'center' | 'right';
}

interface TemplateField {
    id: string;
    label: string;
    type: TemplateFieldType;
    required: boolean;
    placeholder: string;
    placement?: TemplateFieldPlacement;
    style: TemplateFieldStyle;
    page: number;
    pipelineStepId?: string;
    pipelineApproverIndex?: number;
    signatureTargetId?: string;
    options?: string[];
    optionPlacements?: RadioOptionPlacement[];
    tableConfig?: TableFieldConfig;
    tableHtml?: string;
}

interface TemplateBackground {
    name: string;
    type: 'image' | 'pdf';
    mimeType: string;
    dataUrl: string;
    pageCount?: number;
    renderedPages?: string[];
}

interface PipelineFieldPermission {
    fieldId: string;
    fieldLabel: string;
    fieldType: TemplateFieldType;
    right: PipelineFieldRight;
}

interface PipelineStep {
    id: string;
    name: string;
    type: PipelineStepType;
    approvalMode: PipelineApprovalMode;
    order?: number;
    dynamicTarget?: PipelineDynamicTarget;
    dynamicTargets?: PipelineDynamicTarget[];
    serDepartmentId?: number;
    hrTblDepartment?: any;
    serRoleId?: number;
    cfgTblRole?: any;
    users?: any[];
    userIds?: number[];
    fieldPermissions?: PipelineFieldPermission[];
    fieldPermissionsConfigured?: boolean;
}

@Component({
    selector: 'app-template-builder',
    templateUrl: './template-builder.component.html',
    styleUrls: ['./template-builder.component.css']
})
export class TemplateBuilderComponent implements OnInit, OnDestroy {
    @ViewChild('editorFrame') editorFrame?: ElementRef<HTMLElement>;
    @ViewChild('fieldLayer') fieldLayer?: ElementRef<HTMLElement>;

    readonly a4PortraitWidth = 794;
    readonly a4PortraitHeight = 1123;
    readonly pageGap = 16;
    readonly selfHodDepartmentValue = -1;
    readonly wordEditor = WORD_EDITOR_CKEDITOR;
    readonly wordEditorConfig = TEMPLATE_BUILDER_WORD_EDITOR_CONFIG;
    readonly tableFieldEditorConfig: EditorConfig = {
        ...TEMPLATE_BUILDER_WORD_EDITOR_CONFIG,
        toolbar: {
            items: [],
            shouldNotGroupWhenFull: true,
        },
    };
    private tableFormulaEditorCleanups: Array<() => void> = [];
    private tableFormulaSyncTimers = new WeakMap<any, ReturnType<typeof setTimeout>>();
    private tableFormulaSyncGuard = new WeakSet<any>();
    private tableFormulaEditTarget: TableFormulaCellRef | null = null;
    private copiedTableFormula: { expression: string; source: TableFormulaCellRef } | null = null;
    private tableFormulaSyncCommits = new WeakMap<any, (syncedHtml: string) => void>();
    private tableFormulaExpressions = new WeakMap<any, Map<string, string>>();
    private excelIndicatorTimers = new WeakMap<any, ReturnType<typeof setTimeout>>();
    private excelIndicatorCleanups: Array<() => void> = [];
    private dismissedExcelIndicators = new WeakSet<any>();
    private readonly pdfBackgroundRenderScale = 3;
    private readonly minFloatingFieldWidth = 1;
    private readonly minFloatingFieldHeight = 1;
    private readonly tableMinColumnWidth = 56;
    private readonly tableMinRowHeight = 36;
    private readonly tableVerticalChromeHeight = 36;
    private readonly initiatorPipelineStepId = 'initiator-step';
    private readonly initiatorPipelineUserId = '__initiator__';
    private readonly lineManagerPipelineUserId = '__initiator_manager__';
    readonly initiatorPipelineUserOption = {
        id: this.initiatorPipelineUserId,
        dynamicTarget: 'initiator' as PipelineDynamicTarget,
        txtUserName: 'Initiator'
    };
    readonly lineManagerPipelineUserOption = {
        id: this.lineManagerPipelineUserId,
        dynamicTarget: 'initiator_manager' as PipelineDynamicTarget,
        txtUserName: 'Line Manager'
    };

    pageOrientation: 'portrait' | 'landscape' = 'portrait';
    fieldLayerTop = 0;
    templateName = 'Untitled Template';
    codeConvention = 'TPL-0000';
    editingTemplateId = '';
    templateIsActive = true;
    templateIsDraft = false;
    isSavingTemplate = false;
    editorContent = '<h2 style="text-align:center">Template Title</h2><p>Select any text or table cell content, then assign it as a field from the side panel.</p><p>Example: Vendor Name, Amount, Delivery Date, Approval Notes.</p>';
    selectedFieldId = '';
    private routeTemplateId = '';
    newFieldLabel = 'New Field';
    newFieldType: TemplateFieldType = 'text';
    newFieldRequired = false;
    newFieldPage = 1;
    newRadioOptions: { id: string; label: string }[] = [];
    newTableRows = 3;
    newTableColumns = 3;
    newTableHeaders: string[] = ['Column 1', 'Column 2', 'Column 3'];
    newTableHtml = this.getDefaultTableHtml();
    newPipelineName = '';
    newPipelineType: PipelineStepType = 'department';
    newPipelineApprovalMode: PipelineApprovalMode = 'OR';
    selectedPipelineDepartmentId: number | null = null;
    selectedPipelineRoleId: number | null = null;
    selectedPipelineUsers: any[] = [];
    pipelineUserSearchTerm = '';
    showPipelineUserDropdown = false;
    selectedPipelineFieldRights: { [fieldId: string]: PipelineFieldRight } = {};
    editingPipelineStepId = '';
    editingPipelineFieldRights: { [fieldId: string]: PipelineFieldRight } = {};
    fields: TemplateField[] = [];
    pipelineSteps: PipelineStep[] = [];
    departments: any[] = [];
    allUsers: any[] = [];
    roles: any[] = [];
    backgroundFile: TemplateBackground | null = null;
    pageCount = 1;
    manualPageCount = 1;
    previewMode = false;
    showFieldModal = false;
    showPipelineModal = false;
    showPipelineRightsModal = false;
    showPipelineSourceModal = false;
    showPipelineLibraryModal = false;
    libraryPipelines: WorkflowPipelineListItem[] = [];
    libraryPipelineSearch = '';
    libraryPipelinesLoading = false;
    libraryPipelinesError = '';
    selectedLibraryPipelineId: number | null = null;
    linkedWorkflowPipelineId: number | null = null;
    fieldPropertiesCollapsed = false;
    private lastValidTableHtmlByFieldId: Record<string, string> = {};
    private lastValidNewTableHtml = this.newTableHtml;
    private revertingTableFieldIds = new Set<string>();
    private tableEditorsByFieldId: Record<string, any> = {};
    private newTableEditor: any = null;
    private revertingNewTableEditor = false;
    private tableResizeObserversByEditor = new WeakMap<object, ResizeObserver>();

    fieldTypes: { value: TemplateFieldType; label: string }[] = [
        { value: 'document_header', label: 'Document Header QI' },
        { value: 'document_header_qu', label: 'Document Header QU' },
        { value: 'document_header_qf', label: 'Document Header QF' },
        { value: 'document_header_qri', label: 'Document Header QRI' },
        { value: 'document_header_qb', label: 'Document Header QB' },
        { value: 'footer', label: 'Footer (Approval Pipeline)' },
        { value: 'individual_pipeline_footer', label: 'Individual Pipeline (Footer)' },
        { value: 'application_code', label: 'Application Code' },
        { value: 'dynamic_signature', label: 'Dynamic Signatures' },
        { value: 'dynamic_approver_name', label: 'Dynamic Approver Name' },
        { value: 'dynamic_approval_timestamp', label: 'Dynamic Approval Timestamp' },
        { value: 'dynamic_approver_department', label: 'Dynamic Approver Department' },
        { value: 'dynamic_approver_designation', label: 'Dynamic Approver Designation' },
        { value: 'dynamic_user_details', label: 'User Dynamic Details' },
        { value: 'text', label: 'Text' },
        { value: 'integer', label: 'Integer' },
        { value: 'decimal', label: 'Decimal' },
        { value: 'number', label: 'Number' },
        { value: 'date', label: 'Date' },
        { value: 'email', label: 'Email' },
        { value: 'textarea', label: 'Textarea' },
        { value: 'word_editor', label: 'Word Editor' },
        { value: 'attachment', label: 'Attachment' },
        { value: 'select', label: 'Select' },
        { value: 'checkbox', label: 'Checkbox' },
        { value: 'radio', label: 'Radio' },
        { value: 'table', label: 'Table' }
    ];
    pipelineTypes: { value: PipelineStepType; label: string }[] = [
        { value: 'department', label: 'Department' },
        { value: 'individual', label: 'Individual' },
        { value: 'role', label: 'Role' }
    ];
    pipelineFieldRights: { value: PipelineFieldRight; label: string }[] = [
        { value: 'fill', label: 'Fill' },
        { value: 'edit', label: 'Edit' },
        { value: 'hide', label: 'Hide' }
    ];
    fontFamilyOptions: { value: string; label: string }[] = [
        { value: 'Arial, sans-serif', label: 'Arial' },
        { value: 'Calibri, Arial, sans-serif', label: 'Calibri' },
        { value: 'Georgia, \"Times New Roman\", serif', label: 'Georgia' },
        { value: '\"Times New Roman\", Times, serif', label: 'Times New Roman' },
        { value: 'Verdana, Arial, sans-serif', label: 'Verdana' },
        { value: 'Tahoma, Geneva, sans-serif', label: 'Tahoma' },
        { value: '\"Courier New\", Courier, monospace', label: 'Courier New' }
    ];

    private editorInstance: any = null;
    private paginationFrame: number | null = null;
    private activePointerAction: {
        fieldId: string;
        radioOptionId?: string;
        mode: 'drag' | 'resize';
        startClientX: number;
        startClientY: number;
        startX: number;
        startY: number;
        startWidth: number;
        startHeight: number;
    } | null = null;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private departmentService: DepartmentService,
        private templateWorkflowService: TemplateWorkflowService,
        private userService: UserService,
        private notificationService: NotificationService,
        private documentHeaderBranding: DocumentHeaderBrandingService,
        private workflowPipelineService: WorkflowPipelineService,
    ) { }

    ngOnInit(): void {
        this.documentHeaderBranding.ensureLoaded();
        document.body.classList.add(TEMPLATE_BUILDER_HIDE_CKEDITOR_BADGE_CLASS);
        this.ensureInitiatorPipelineStep();
        this.loadDepartments();
        this.loadUsers();
        this.loadRoles();
        const templateId = this.route.snapshot.queryParamMap.get('id') || this.route.snapshot.paramMap.get('id') || '';
        this.routeTemplateId = templateId;
        if (templateId) {
            this.loadTemplateForEdit(templateId);
        }
        window.addEventListener('velocity-tour-prepare', this.onTourPrepare);
    }

    ngOnDestroy(): void {
        window.removeEventListener('velocity-tour-prepare', this.onTourPrepare);
        document.body.classList.remove(TEMPLATE_BUILDER_HIDE_CKEDITOR_BADGE_CLASS);
        this.excelIndicatorCleanups.forEach((cleanup) => cleanup());
        this.excelIndicatorCleanups = [];
        this.tableFormulaEditorCleanups.forEach((cleanup) => cleanup());
        this.tableFormulaEditorCleanups = [];
    }

    private onTourPrepare = (event: Event): void => {
        const action = (event as CustomEvent<{ action?: string }>).detail?.action;
        if (action === 'template-builder-close-modals') {
            this.closeFieldModal();
            this.closePipelineModal();
            this.closePipelineRightsModal();
        } else if (action === 'template-builder-open-field-modal' && !this.showFieldModal) {
            this.openFieldModal();
        }
    };

    get pageWidth(): number {
        return this.pageOrientation === 'portrait' ? this.a4PortraitWidth : this.a4PortraitHeight;
    }

    get pageHeight(): number {
        return this.pageOrientation === 'portrait' ? this.a4PortraitHeight : this.a4PortraitWidth;
    }

    get pageIndexes(): number[] {
        return Array.from({ length: this.pageCount }, (_, index) => index);
    }

    /** Document stage zoom (center canvas). */
    documentZoom = 1;
    readonly documentZoomMin = 0.5;
    readonly documentZoomMax = 2;
    readonly documentZoomStep = 0.1;

    get documentFrameHeight(): number {
        return this.pageCount * this.pageHeight + (this.pageCount - 1) * this.pageGap;
    }

    get zoomedDocumentWidth(): number {
        return Math.round(this.pageWidth * this.documentZoom);
    }

    get zoomedDocumentHeight(): number {
        return Math.round(this.documentFrameHeight * this.documentZoom);
    }

    get documentZoomPercentLabel(): string {
        return `${Math.round(this.documentZoom * 100)}%`;
    }

    get documentZoomTransform(): { [key: string]: string } {
        return {
            transform: `scale(${this.documentZoom})`,
            transformOrigin: 'top left',
            width: `${this.pageWidth}px`,
            minHeight: `${this.documentFrameHeight}px`,
        };
    }

    zoomInDocument(): void {
        this.setDocumentZoom(this.documentZoom + this.documentZoomStep);
    }

    zoomOutDocument(): void {
        this.setDocumentZoom(this.documentZoom - this.documentZoomStep);
    }

    resetDocumentZoom(): void {
        this.setDocumentZoom(1);
    }

    onDocumentStageWheel(event: WheelEvent): void {
        if (!event.ctrlKey && !event.metaKey) {
            return;
        }
        const target = event.target as HTMLElement | null;
        if (!target?.closest('.paper-wrap')) {
            return;
        }
        event.preventDefault();
        if (event.deltaY < 0) {
            this.zoomInDocument();
        } else if (event.deltaY > 0) {
            this.zoomOutDocument();
        }
    }

    private setDocumentZoom(value: number): void {
        const clamped = Math.min(this.documentZoomMax, Math.max(this.documentZoomMin, value));
        this.documentZoom = Math.round(clamped * 100) / 100;
        this.scheduleExcelTableIndicators(this.editorInstance);
        Object.values(this.tableEditorsByFieldId || {}).forEach((editor) => this.scheduleExcelTableIndicators(editor));
        if (this.newTableEditor) {
            this.scheduleExcelTableIndicators(this.newTableEditor);
        }
    }

    get selectedField(): TemplateField | undefined {
        return this.fields.find((field) => field.id === this.selectedFieldId);
    }

    get hidePayloadButton(): boolean {
        return this.routeTemplateId === '615';
    }

    get pipelineAssignableFields(): TemplateField[] {
        return this.fields.filter((field) => !this.isDocumentRegionFieldType(field.type));
    }

    get nonRadioFields(): TemplateField[] {
        return this.fields.filter((field) => field.type !== 'radio');
    }

    get editingPipelineStep(): PipelineStep | undefined {
        return this.pipelineSteps.find((step) => step.id === this.editingPipelineStepId);
    }

    get pipelineUserOptions(): any[] {
        const users = (this.allUsers || []).filter((user) => this.isSelectablePipelineEmployee(user));
        return [this.initiatorPipelineUserOption, this.lineManagerPipelineUserOption, ...users];
    }

    get filteredPipelineUserOptions(): any[] {
        const searchTerm = this.pipelineUserSearchTerm.trim().toLowerCase();
        if (!searchTerm) {
            return this.pipelineUserOptions;
        }
        return this.pipelineUserOptions.filter((user) => this.getUserName(user).toLowerCase().includes(searchTerm));
    }

    get dynamicSignatureTargetOptions(): { id: string; label: string; step: PipelineStep }[] {
        return this.pipelineSteps.map((step) => ({
            id: step.id,
            label: this.getDynamicSignatureTargetOptionLabel(step),
            step
        }));
    }

    setPageOrientation(orientation: 'portrait' | 'landscape'): void {
        if (this.pageOrientation === orientation) {
            return;
        }

        const oldWidth = this.pageWidth;
        const oldHeight = this.pageHeight;
        this.pageOrientation = orientation;
        const xScale = this.pageWidth / oldWidth;
        const yScale = this.pageHeight / oldHeight;

        this.fields.forEach((field) => {
            this.scalePlacementOwner(field, field.placement, xScale, yScale);
            (field.optionPlacements || []).forEach((option) => this.scalePlacementOwner(option, option.placement, xScale, yScale));
        });
    }

    onEditorReady(editor: any): void {
        this.editorInstance = editor;
        const initialHtml = String(this.editorContent || '');
        if (initialHtml && String(editor.getData?.() || '') !== initialHtml) {
            this.tableFormulaSyncGuard.add(editor);
            editor.setData(initialHtml);
            setTimeout(() => this.tableFormulaSyncGuard.delete(editor), 0);
        }
        this.attachTableFormulaLiveSync(editor, (syncedHtml) => {
            this.editorContent = syncedHtml;
            this.schedulePaginationUpdate();
        });
        this.attachExcelTableIndicators(editor);
        this.runInitialTableFormulaSync(editor, (syncedHtml) => {
            this.editorContent = syncedHtml;
        });
        this.protectTableInsert(editor);
        this.updateFieldLayerTop();
        this.schedulePaginationUpdate();
    }

    private attachTableFormulaLiveSync(editor: any, commit: (syncedHtml: string) => void): void {
        if (!editor?.model?.document?.on || editor.__templateBuilderFormulaSyncAttached) {
            return;
        }
        editor.__templateBuilderFormulaSyncAttached = true;
        this.tableFormulaSyncCommits.set(editor, commit);
        this.attachTableFormulaEditSupport(editor);
        editor.model.document.on('change:data', () => {
            this.scheduleExcelTableIndicators(editor);
        });
    }

    private disposeTableFormulaEditor(editor: any): void {
        if (!editor) {
            return;
        }
        this.tableFormulaSyncCommits.set(editor, () => {});
        delete editor.__templateBuilderFormulaSyncAttached;
        delete editor.__templateBuilderFormulaEditAttached;
        delete editor.__templateBuilderExcelIndicatorsAttached;
        delete editor.__templateBuilderInsertTableGuard;
    }

    private releaseTableFieldEditor(fieldId: string): void {
        if (!fieldId) {
            return;
        }
        const editor = this.tableEditorsByFieldId[fieldId];
        if (editor) {
            this.disposeTableFormulaEditor(editor);
            delete this.tableEditorsByFieldId[fieldId];
        }
        if (this.tableFormulaEditTarget) {
            this.tableFormulaEditTarget = null;
        }
    }

    private shouldDeferTableFormulaSync(editor: any): boolean {
        return this.editorHasIncompleteFormulaDraft(editor);
    }

    private editorHasIncompleteFormulaDraft(editor: any): boolean {
        const html = String(editor?.getData?.() || '');
        if (!/<table[\s>]/i.test(html)) {
            return false;
        }
        const host = document.createElement('div');
        host.innerHTML = html;
        const cells = host.querySelectorAll('td, th');
        for (let index = 0; index < cells.length; index += 1) {
            const cell = cells[index] as HTMLTableCellElement;
            const text = readTableCellPlainText(cell);
            if (isExcelFormulaText(text) && !isExcelFormulaComplete(text)) {
                return true;
            }
        }
        return false;
    }

    private moveSelectionOutsideTables(editor: any): void {
        const root = editor?.model?.document?.getRoot?.();
        if (!root || !editor?.model?.change) {
            return;
        }
        try {
            editor.model.change((writer: any) => {
                writer.setSelection(root, 'end');
            });
        } catch {
            // Selection can stay in the cell; setData is still skipped while typing.
        }
    }

    private readModelCellPlainText(cell: any): string {
        if (!cell?.getChildren) {
            return '';
        }
        const parts: string[] = [];
        const visit = (node: any) => {
            if (!node) {
                return;
            }
            if (typeof node.data === 'string') {
                parts.push(node.data);
                return;
            }
            if (typeof node.getChildren === 'function') {
                Array.from(node.getChildren()).forEach(visit);
            }
        };
        Array.from(cell.getChildren()).forEach(visit);
        return parts.join('').replace(/\u00a0/g, ' ').replace(/\s+/g, ' ').trim();
    }

    private getModelCellRefFromSelection(editor: any): TableFormulaCellRef | null {
        const position = editor?.model?.document?.selection?.getFirstPosition?.();
        let cell = position?.parent;
        while (cell && cell.name !== 'tableCell' && cell.name !== 'tableHeaderCell') {
            cell = cell.parent;
        }
        const row = cell?.parent;
        const table = row?.parent;
        if (!cell || !row || row.name !== 'tableRow' || !table || table.name !== 'table') {
            return null;
        }
        const root = editor.model.document.getRoot();
        let tableIndex = 0;
        let matched = false;
        const visit = (element: any): void => {
            if (!element?.getChildren || matched) {
                return;
            }
            for (const child of element.getChildren()) {
                if (matched) {
                    return;
                }
                if (child?.name === 'table') {
                    tableIndex += 1;
                    if (child === table) {
                        matched = true;
                        return;
                    }
                    continue;
                }
                visit(child);
            }
        };
        visit(root);
        if (!matched) {
            return null;
        }
        const rows = Array.from(row.parent.getChildren()).filter((item: any) => item?.name === 'tableRow');
        const cells = Array.from(row.getChildren()).filter((item: any) =>
            item?.name === 'tableCell' || item?.name === 'tableHeaderCell');
        const rowNumber = rows.indexOf(row) + 1;
        const colNumber = cells.indexOf(cell) + 1;
        if (rowNumber < 1 || colNumber < 1) {
            return null;
        }
        return { tableIndex, row: rowNumber, col: colNumber };
    }

    private isSelectionInsideTable(editor: any): boolean {
        const position = editor?.model?.document?.selection?.getFirstPosition?.();
        let parent = position?.parent;
        while (parent) {
            if (parent.name === 'table' || parent.name === 'tableCell' || parent.name === 'tableHeaderCell') {
                return true;
            }
            parent = parent.parent;
        }
        return false;
    }

    private findModelTableCell(editor: any, tableIndex: number, row: number, col: number): any | null {
        const root = editor?.model?.document?.getRoot?.();
        if (!root?.getChildren) {
            return null;
        }
        let seen = 0;
        const visit = (element: any): any | null => {
            for (const child of element.getChildren()) {
                if (child?.name === 'table') {
                    seen += 1;
                    if (seen === tableIndex) {
                        const rows = Array.from(child.getChildren()).filter((item: any) => item?.name === 'tableRow');
                        const rowNode = rows[row - 1] as any;
                        if (!rowNode?.getChildren) {
                            return null;
                        }
                        const cells = Array.from(rowNode.getChildren()).filter((item: any) =>
                            item?.name === 'tableCell' || item?.name === 'tableHeaderCell');
                        return cells[col - 1] || null;
                    }
                    continue;
                }
                if (typeof child?.getChildren === 'function') {
                    const found = visit(child);
                    if (found) {
                        return found;
                    }
                }
            }
            return null;
        };
        return visit(root);
    }

    private replaceEditorTableCellText(editor: any, cellRef: TableFormulaCellRef, text: string): void {
        const cell = this.findModelTableCell(editor, cellRef.tableIndex, cellRef.row, cellRef.col);
        if (!cell || !editor?.model?.change) {
            return;
        }
        this.tableFormulaSyncGuard.add(editor);
        try {
            editor.model.change((writer: any) => {
                const range = editor.model.createRangeIn(cell);
                writer.remove(range);
                const paragraph = writer.createElement('paragraph');
                writer.append(paragraph, cell);
                if (text) {
                    writer.insertText(text, paragraph);
                }
            });
        } catch (error) {
            console.warn('[template-builder] could not restore formula text in cell', error);
        }
        setTimeout(() => this.tableFormulaSyncGuard.delete(editor), 0);
    }

    private isEditingFormulaInTableCell(editor: any): boolean {
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        const active = editable?.ownerDocument?.activeElement as HTMLElement | null;
        if (!editable || !active || !editable.contains(active)) {
            return false;
        }
        const cell = active.closest?.('td, th') as HTMLTableCellElement | null;
        if (!cell || !editable.contains(cell)) {
            return false;
        }
        const text = readTableCellPlainText(cell);
        return isExcelFormulaText(text);
    }

    private runTableFormulaSync(editor: any, force = false): void {
        if (this.tableFormulaSyncGuard.has(editor)) {
            return;
        }
        if (!force && this.shouldDeferTableFormulaSync(editor)) {
            this.scheduleExcelTableIndicators(editor);
            return;
        }
        if (!force && this.isEditingFormulaInTableCell(editor)) {
            this.scheduleExcelTableIndicators(editor);
            return;
        }
        const commit = this.tableFormulaSyncCommits.get(editor);
        if (!commit) {
            return;
        }
        const rawHtml = String(editor.getData?.() || '');
        const syncedHtml = finalizeTableFormulaHtml(rawHtml, null, {
            editingTarget: this.tableFormulaEditTarget,
        });
        if (syncedHtml === rawHtml) {
            this.scheduleExcelTableIndicators(editor);
            return;
        }
        if (!isTableFormulaSyncHtmlSafe(rawHtml, syncedHtml)) {
            console.warn('[template-builder] Skipped formula sync to protect table structure.');
            this.scheduleExcelTableIndicators(editor);
            return;
        }
        if (this.isSelectionInsideTable(editor) && !force) {
            this.scheduleExcelTableIndicators(editor);
            return;
        }
        this.tableFormulaSyncGuard.add(editor);
        if (this.isSelectionInsideTable(editor)) {
            this.moveSelectionOutsideTables(editor);
        }
        editor.setData?.(syncedHtml);
        commit(syncedHtml);
        setTimeout(() => {
            this.tableFormulaSyncGuard.delete(editor);
            this.scheduleExcelTableIndicators(editor);
        }, 0);
    }

    private attachTableFormulaEditSupport(editor: any): void {
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        if (!editable || editor.__templateBuilderFormulaEditAttached) {
            return;
        }
        editor.__templateBuilderFormulaEditAttached = true;

        const onFocusIn = (event: FocusEvent) => {
            const target = event.target as HTMLElement | null;
            const cell = target?.closest?.('td, th') as HTMLTableCellElement | null;
            if (!cell || !editable.contains(cell)) {
                return;
            }
            const cellRef = getTableFormulaCellRefFromElement(cell, editable);
            if (!cellRef) {
                return;
            }
            const remembered = this.recallFormulaExpression(editor, cellRef);
            const formula = getFormulaDefinitionFromCell(cell);
            const editText = formula ? getFormulaEditText(formula) : remembered;
            if (!editText) {
                return;
            }
            this.tableFormulaEditTarget = cellRef;
            if (readTableCellPlainText(cell) !== editText) {
                this.replaceEditorTableCellText(editor, cellRef, editText);
            }
        };

        const onFocusOut = (event: FocusEvent) => {
            const target = event.target as HTMLElement | null;
            const cell = target?.closest?.('td, th') as HTMLTableCellElement | null;
            if (!cell || !editable.contains(cell)) {
                return;
            }
            window.setTimeout(() => {
                const active = editable.ownerDocument.activeElement as HTMLElement | null;
                const activeCell = active?.closest?.('td, th') as HTMLTableCellElement | null;
                if (activeCell && cell.contains(activeCell)) {
                    return;
                }
                if (activeCell && editable.contains(activeCell)) {
                    this.tableFormulaEditTarget = getTableFormulaCellRefFromElement(activeCell, editable);
                } else {
                    this.tableFormulaEditTarget = null;
                }
                if (this.shouldDeferTableFormulaSync(editor)) {
                    this.scheduleExcelTableIndicators(editor);
                    return;
                }
                this.renderFormulaResultsInModel(editor);
            }, 0);
        };

        const onCopyFormula = () => {
            const source = this.getModelCellRefFromSelection(editor);
            if (!source) {
                this.copiedTableFormula = null;
                return;
            }
            const expression = this.formulaExpressionForCell(editor, editable, source);
            this.copiedTableFormula = expression
                ? { expression, source }
                : null;
        };
        const onPasteFormula = (event: ClipboardEvent) => {
            const copied = this.copiedTableFormula;
            const destination = this.getModelCellRefFromSelection(editor);
            if (!copied || !destination || !isExcelFormulaText(copied.expression)) {
                return;
            }
            const plainText = String(event.clipboardData?.getData('text/plain') || '');
            const htmlText = String(event.clipboardData?.getData('text/html') || '');
            const htmlHost = document.createElement('div');
            htmlHost.innerHTML = htmlText;
            const pasted = (isExcelFormulaText(plainText) ? plainText : htmlHost.textContent || '')
                .replace(/\u00a0/g, ' ')
                .trim();
            if (!isExcelFormulaText(pasted)) {
                return;
            }
            if (this.normalizeCopiedFormula(pasted) !== this.normalizeCopiedFormula(copied.expression)) {
                return;
            }
            const deltaRow = destination.row - copied.source.row;
            const deltaCol = destination.col - copied.source.col;
            if (!deltaRow && !deltaCol) {
                return;
            }
            const shifted = shiftExcelFormulaReferences(copied.expression, deltaRow, deltaCol);
            event.preventDefault();
            event.stopPropagation();
            this.tableFormulaEditTarget = destination;
            this.rememberFormulaExpression(editor, destination, shifted);
            this.replaceEditorTableCellText(editor, destination, shifted);
        };
        editable.addEventListener('focusin', onFocusIn);
        editable.addEventListener('focusout', onFocusOut, true);
        editable.addEventListener('copy', onCopyFormula, true);
        editable.addEventListener('cut', onCopyFormula, true);
        editable.addEventListener('paste', onPasteFormula, true);
        const onSelectionChange = () => {
            if (this.tableFormulaSyncGuard.has(editor)) {
                return;
            }
            const nextRef = this.getModelCellRefFromSelection(editor);
            const previousRef = this.tableFormulaEditTarget;
            if (previousRef && nextRef && isSameTableCellRef(previousRef, nextRef)) {
                return;
            }
            if (!previousRef && !nextRef) {
                return;
            }
            this.tableFormulaEditTarget = nextRef;
            window.setTimeout(() => {
                if (this.tableFormulaSyncGuard.has(editor)) {
                    return;
                }
                if (!this.shouldDeferTableFormulaSync(editor)) {
                    this.renderFormulaResultsInModel(editor);
                }
                if (!nextRef) {
                    return;
                }
                const remembered = this.recallFormulaExpression(editor, nextRef);
                if (!remembered) {
                    return;
                }
                const cell = this.findModelTableCell(editor, nextRef.tableIndex, nextRef.row, nextRef.col);
                const liveText = this.readModelCellPlainText(cell);
                if (liveText === remembered) {
                    return;
                }
                this.tableFormulaEditTarget = nextRef;
                this.replaceEditorTableCellText(editor, nextRef, remembered);
            }, 40);
        };
        editor.model.document.selection.on('change:range', onSelectionChange);
        this.tableFormulaEditorCleanups.push(() => {
            editable.removeEventListener('focusin', onFocusIn);
            editable.removeEventListener('focusout', onFocusOut, true);
            editable.removeEventListener('copy', onCopyFormula, true);
            editable.removeEventListener('cut', onCopyFormula, true);
            editable.removeEventListener('paste', onPasteFormula, true);
            editor.model.document.selection.off('change:range', onSelectionChange);
        });
    }

    private formulaExpressionForCell(editor: any, editable: HTMLElement, cellRef: TableFormulaCellRef): string {
        const tables = Array.from(editable.querySelectorAll('table'));
        const table = tables[cellRef.tableIndex - 1];
        const row = table?.querySelectorAll('tr')[cellRef.row - 1];
        const cell = row?.querySelectorAll('th, td')[cellRef.col - 1] as HTMLTableCellElement | undefined;
        const liveText = cell ? readTableCellPlainText(cell) : '';
        if (isExcelFormulaText(liveText)) {
            return liveText.trim();
        }
        const formula = cell ? getFormulaDefinitionFromCell(cell) : null;
        const fromAttribute = formula ? getFormulaEditText(formula) : '';
        if (isExcelFormulaText(fromAttribute)) {
            return fromAttribute.trim();
        }
        const remembered = this.recallFormulaExpression(editor, cellRef);
        return isExcelFormulaText(remembered) ? remembered.trim() : '';
    }

    private normalizeCopiedFormula(value: string): string {
        return String(value || '').replace(/\u00a0/g, ' ').replace(/\s+/g, '').toUpperCase();
    }

    private runInitialTableFormulaSync(editor: any, commit: (syncedHtml: string) => void): void {
        if (!editor?.getData) {
            return;
        }
        const rawHtml = String(editor.getData() || '');
        const syncedHtml = finalizeTableFormulaHtml(rawHtml, null, {
            editingTarget: this.tableFormulaEditTarget,
        });
        if (syncedHtml === rawHtml) {
            return;
        }
        if (!isTableFormulaSyncHtmlSafe(rawHtml, syncedHtml)) {
            return;
        }
        this.tableFormulaSyncGuard.add(editor);
        editor.setData(syncedHtml);
        commit(syncedHtml);
        setTimeout(() => {
            this.tableFormulaSyncGuard.delete(editor);
            this.scheduleExcelTableIndicators(editor);
        }, 0);
    }

    private attachExcelTableIndicators(editor: any): void {
        if (!editor?.model?.document?.on || editor.__templateBuilderExcelIndicatorsAttached) {
            return;
        }
        editor.__templateBuilderExcelIndicatorsAttached = true;
        const schedule = () => this.scheduleExcelTableIndicators(editor);
        editor.model.document.on('change:data', schedule);
        editor.model.document.selection?.on?.('change:range', schedule);
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        const onScroll = () => schedule();
        editable?.addEventListener('scroll', onScroll, { passive: true });
        window.addEventListener('resize', onScroll, { passive: true });
        this.excelIndicatorCleanups.push(() => {
            editable?.removeEventListener('scroll', onScroll);
            window.removeEventListener('resize', onScroll);
        });
        schedule();
    }

    private scheduleExcelTableIndicators(editor: any): void {
        const pending = this.excelIndicatorTimers.get(editor);
        if (pending) {
            clearTimeout(pending);
        }
        this.excelIndicatorTimers.set(editor, setTimeout(() => this.refreshExcelTableIndicators(editor), 60));
    }

    private refreshExcelTableIndicators(editor: any): void {
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        const editorRoot = editable?.closest('.ck-editor') as HTMLElement | null;
        if (!editable || !editorRoot) {
            return;
        }

        editorRoot.querySelectorAll('.template-excel-indicator-layer').forEach((node) => node.remove());
        if (this.dismissedExcelIndicators.has(editor)) {
            return;
        }

        const tables = Array.from(editable.querySelectorAll('table')).filter((table) => !table.parentElement?.closest('table'));
        if (tables.length === 0) {
            return;
        }

        const main = editorRoot.querySelector('.ck-editor__main') as HTMLElement | null;
        const host = main || editorRoot;
        if (getComputedStyle(host).position === 'static') {
            host.style.position = 'relative';
        }
        host.style.overflow = 'visible';

        const layer = document.createElement('div');
        layer.className = 'template-excel-indicator-layer';
        const editableBox = this.boxWithin(editable, host);
        layer.style.left = `${editableBox.left}px`;
        layer.style.top = `${editableBox.top}px`;
        layer.style.width = `${Math.max(editable.offsetWidth, editable.scrollWidth)}px`;
        layer.style.height = `${Math.max(editable.offsetHeight, editable.scrollHeight)}px`;
        host.appendChild(layer);

        const rowGutter = 22;
        const colGutter = 16;
        let needsRetry = false;

        tables.forEach((tableNode) => {
            const table = tableNode as HTMLTableElement;
            const rows = Array.from(table.querySelectorAll(':scope > tbody > tr, :scope > thead > tr, :scope > tr'));
            const measuredRows = rows.length ? rows : Array.from(table.querySelectorAll('tr'));
            if (measuredRows.length === 0) {
                needsRetry = true;
                return;
            }

            const firstRowCells = Array.from(measuredRows[0].querySelectorAll(':scope > th, :scope > td')) as HTMLElement[];
            const headerCells = firstRowCells.length
                ? firstRowCells
                : Array.from(measuredRows[0].querySelectorAll('th, td')) as HTMLElement[];
            if (headerCells.length === 0) {
                needsRetry = true;
                return;
            }

            const anchorBox = this.boxWithin(headerCells[0], editable);
            if (anchorBox.width < 1 || anchorBox.height < 1) {
                needsRetry = true;
                return;
            }

            const corner = document.createElement('button');
            corner.type = 'button';
            corner.className = 'template-excel-corner';
            corner.title = 'Remove row and column labels';
            corner.setAttribute('aria-label', 'Remove row and column labels');
            corner.textContent = '×';
            corner.style.left = `${anchorBox.left - rowGutter}px`;
            corner.style.top = `${anchorBox.top - colGutter}px`;
            corner.style.width = `${rowGutter}px`;
            corner.style.height = `${colGutter}px`;
            corner.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                this.dismissedExcelIndicators.add(editor);
                layer.remove();
            });
            layer.appendChild(corner);

            headerCells.forEach((cell, columnIndex) => {
                const cellBox = this.boxWithin(cell, editable);
                const label = document.createElement('span');
                label.className = 'template-excel-col-label';
                label.style.left = `${cellBox.left}px`;
                label.style.top = `${cellBox.top - colGutter}px`;
                label.style.width = `${Math.max(cellBox.width, 1)}px`;
                label.style.height = `${colGutter}px`;
                label.textContent = formatExcelColumnLabel(columnIndex + 1);
                layer.appendChild(label);
            });

            measuredRows.forEach((row, rowIndex) => {
                const firstCell = row.querySelector(':scope > th, :scope > td, th, td') as HTMLElement | null;
                if (!firstCell) {
                    return;
                }
                const rowBox = this.boxWithin(firstCell, editable);
                if (rowBox.height < 1) {
                    needsRetry = true;
                    return;
                }
                const label = document.createElement('span');
                label.className = 'template-excel-row-label';
                label.style.left = `${rowBox.left - rowGutter}px`;
                label.style.top = `${rowBox.top}px`;
                label.style.width = `${rowGutter}px`;
                label.style.height = `${rowBox.height}px`;
                label.textContent = String(rowIndex + 1);
                layer.appendChild(label);
            });
        });

        if (needsRetry) {
            const attempts = Number(editor.__excelIndicatorRetries || 0);
            if (attempts < 8) {
                editor.__excelIndicatorRetries = attempts + 1;
                setTimeout(() => this.refreshExcelTableIndicators(editor), 120);
            }
        } else {
            editor.__excelIndicatorRetries = 0;
        }
    }

    private boxWithin(element: HTMLElement, ancestor: HTMLElement): { left: number; top: number; width: number; height: number } {
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

    private formulaMemoryFor(editor: any): Map<string, string> {
        let memory = this.tableFormulaExpressions.get(editor);
        if (!memory) {
            memory = new Map<string, string>();
            this.tableFormulaExpressions.set(editor, memory);
        }
        return memory;
    }

    private rememberFormulaExpression(editor: any, cellRef: TableFormulaCellRef, expression: string): void {
        if (!expression) {
            return;
        }
        this.formulaMemoryFor(editor).set(`${cellRef.tableIndex}:${cellRef.row}:${cellRef.col}`, expression);
    }

    private recallFormulaExpression(editor: any, cellRef: TableFormulaCellRef): string {
        return this.formulaMemoryFor(editor).get(`${cellRef.tableIndex}:${cellRef.row}:${cellRef.col}`) || '';
    }

    private renderFormulaResultsInModel(editor: any): void {
        if (!editor?.getData || !editor?.model?.change || this.tableFormulaSyncGuard.has(editor)) {
            return;
        }
        if (this.shouldDeferTableFormulaSync(editor)) {
            return;
        }
        const rawHtml = String(editor.getData() || '');
        if (!/<table[\s>]/i.test(rawHtml)) {
            return;
        }
        const syncedHtml = finalizeTableFormulaHtml(rawHtml, null, {
            editingTarget: this.tableFormulaEditTarget,
        });
        if (!isTableFormulaSyncHtmlSafe(rawHtml, syncedHtml)) {
            return;
        }
        const host = document.createElement('div');
        host.innerHTML = syncedHtml;
        const updates: Array<{ ref: TableFormulaCellRef; text: string; rawAttr: string }> = [];
        host.querySelectorAll('table').forEach((table, tableIndex) => {
            Array.from(table.querySelectorAll('tr')).forEach((row, rowIndex) => {
                Array.from(row.querySelectorAll('td, th')).forEach((cell, colIndex) => {
                    const element = cell as HTMLTableCellElement;
                    const rawAttr = element.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR) || '';
                    if (!rawAttr) {
                        return;
                    }
                    const ref: TableFormulaCellRef = {
                        tableIndex: tableIndex + 1,
                        row: rowIndex + 1,
                        col: colIndex + 1,
                    };
                    if (this.tableFormulaEditTarget && isSameTableCellRef(this.tableFormulaEditTarget, ref)) {
                        return;
                    }
                    let expression = '';
                    try {
                        expression = String((JSON.parse(rawAttr) as TableFormulaDefinition)?.expression || '');
                    } catch {
                        expression = '';
                    }
                    if (expression) {
                        this.rememberFormulaExpression(editor, ref, expression);
                    }
                    updates.push({
                        ref,
                        text: readTableCellPlainText(element),
                        rawAttr,
                    });
                });
            });
        });
        if (updates.length === 0) {
            return;
        }
        this.tableFormulaSyncGuard.add(editor);
        try {
            editor.model.change((writer: any) => {
                updates.forEach((update) => {
                    const cell = this.findModelTableCell(editor, update.ref.tableIndex, update.ref.row, update.ref.col);
                    if (!cell) {
                        return;
                    }
                    const range = editor.model.createRangeIn(cell);
                    writer.remove(range);
                    const paragraph = writer.createElement('paragraph');
                    writer.append(paragraph, cell);
                    if (update.text) {
                        writer.insertText(update.text, paragraph);
                    }
                    if (editor.model.schema.checkAttribute(cell, 'htmlTdAttributes')) {
                        writer.setAttribute('htmlTdAttributes', {
                            attributes: {
                                'data-template-formula': update.rawAttr,
                                title: update.text,
                            },
                            classes: ['template-formula-cell'],
                        }, cell);
                    }
                });
            });
        } catch (error) {
            console.warn('[template-builder] could not render table formula result', error);
        }
        setTimeout(() => {
            this.tableFormulaSyncGuard.delete(editor);
            this.scheduleExcelTableIndicators(editor);
        }, 0);
    }

    onEditorContentChanged(event?: { editor?: any }): void {
        const editor = event?.editor || this.editorInstance;
        if (editor && this.tableFormulaSyncGuard.has(editor)) {
            this.schedulePaginationUpdate();
            return;
        }
        const html = String(editor?.getData?.() || this.editorContent || '');
        const cleanedContent = this.removeRadioFieldMarkers(html);
        this.editorContent = cleanedContent;
        this.schedulePaginationUpdate();
    }

    private protectTableInsert(editor: any): void {
        const insertTable = editor?.commands?.get?.('insertTable');
        if (!insertTable?.on || editor.__templateBuilderInsertTableGuard) {
            return;
        }
        editor.__templateBuilderInsertTableGuard = true;
        insertTable.on('execute', () => {
            this.tableFormulaSyncGuard.add(editor);
            setTimeout(() => {
                this.tableFormulaSyncGuard.delete(editor);
                this.editorContent = String(editor.getData?.() || this.editorContent || '');
                this.scheduleExcelTableIndicators(editor);
                setTimeout(() => this.scheduleExcelTableIndicators(editor), 200);
            }, 300);
        });
    }

    assignField(): void {
        if (!this.editorInstance) {
            return;
        }
        if (this.isAttachmentFieldType(this.newFieldType)) {
            this.addInlineOnlyField();
            return;
        }

        const field = this.createField();
        if (field.type === 'radio') {
            this.addMissingRadioOptionBoxes(field);
            this.selectedFieldId = field.id;
            this.editorContent = this.removeRadioFieldMarkers(this.replaceFieldMarkers(field.id));
            this.editorInstance?.setData(this.editorContent);
            this.schedulePaginationUpdate();
            this.closeAndResetFieldModal();
            return;
        }

        const selectedText = this.getSelectedEditorText().trim();
        const markerText = selectedText || field.placeholder;
        const markerHtml = `<span class="template-field selected" data-field-id="${field.id}" data-field-type="${field.type}" data-required="${field.required}" title="${this.escapeHtml(field.label)} (${field.type})">${this.escapeHtml(markerText)}</span>`;
        const viewFragment = this.editorInstance.data.processor.toView(markerHtml);
        const modelFragment = this.editorInstance.data.toModel(viewFragment);

        this.editorInstance.model.change(() => {
            this.editorInstance.model.insertContent(modelFragment, this.editorInstance.model.document.selection);
        });

        this.selectedFieldId = field.id;
        this.editorContent = this.editorInstance.getData();
        this.schedulePaginationUpdate();
        this.closeAndResetFieldModal();
    }

    addFloatingField(): void {
        if (this.isAttachmentFieldType(this.newFieldType)) {
            this.addInlineOnlyField();
            return;
        }

        const field = this.createField();
        if (field.type === 'radio') {
            this.addMissingRadioOptionBoxes(field);
            this.selectedFieldId = field.id;
            this.closeAndResetFieldModal();
            return;
        }

        const index = this.fields.length - 1;
        const isHeader = this.isHeaderFieldType(field.type);
        const isFooter = this.isFooterFieldType(field.type);
        const pageTop = (field.page - 1) * (this.pageHeight + this.pageGap);
        field.placement = field.type === 'table' ? this.buildDefaultTablePlacement(field.page, index) : {
            x: isHeader || isFooter ? 0 : 32 + (index % 4) * 18,
            y: isHeader ? pageTop : (isFooter ? pageTop + this.pageHeight - 86 : pageTop + 120 + (index % 5) * 18),
            width: isHeader || isFooter ? this.pageWidth : 160,
            height: isHeader ? 116 : (isFooter ? 86 : 38)
        };
        this.selectedFieldId = field.id;
        this.closeAndResetFieldModal();
    }

    addPage(): void {
        this.manualPageCount += 1;
        this.pageCount = Math.max(this.pageCount, this.manualPageCount);
    }

    async onBackgroundFileSelected(event: Event): Promise<void> {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        if (!file) {
            return;
        }

        const isImage = file.type.startsWith('image/');
        const isPdf = file.type === 'application/pdf' || /\.pdf$/i.test(file.name);
        if (!isImage && !isPdf) {
            input.value = '';
            return;
        }

        const dataUrl = await this.readFileAsDataUrl(file);
        const renderedPages = isPdf ? await this.renderPdfPages(file) : undefined;
        const pdfPageCount = isPdf ? await this.getPdfPageCount(file) : 1;
        this.backgroundFile = {
            name: file.name,
            type: isPdf ? 'pdf' : 'image',
            mimeType: isPdf ? 'application/pdf' : file.type,
            dataUrl,
            pageCount: renderedPages?.length || pdfPageCount,
            renderedPages
        };

        if (isImage) {
            this.applyImagePageOrientation(dataUrl);
        } else {
            const count = renderedPages?.length || pdfPageCount;
            this.manualPageCount = Math.max(this.manualPageCount, count);
            this.pageCount = Math.max(this.pageCount, count);
        }

        if (this.editorContent === '<h2 style="text-align:center">Template Title</h2><p>Select any text or table cell content, then assign it as a field from the side panel.</p><p>Example: Vendor Name, Amount, Delivery Date, Approval Notes.</p>') {
            this.editorContent = '';
            this.editorInstance?.setData('');
        }
        input.value = '';
    }

    removeBackgroundFile(): void {
        this.backgroundFile = null;
    }

    openFieldModal(): void {
        this.ensureNewRadioOptions();
        this.showFieldModal = true;
    }

    closeFieldModal(): void {
        this.showFieldModal = false;
    }

    toggleFieldPropertiesCollapsed(): void {
        this.fieldPropertiesCollapsed = !this.fieldPropertiesCollapsed;
    }

    onNewFieldTypeChanged(): void {
        if (this.newFieldType === 'radio') {
            this.ensureNewRadioOptions();
        }
        if (this.newFieldType === 'table') {
            this.ensureNewTableHeaders();
            this.newTableHtml = this.normalizeTableHtml(this.newTableHtml);
        }
    }

    onNewTableSizeChanged(): void {
        this.newTableRows = this.normalizeTableSize(this.newTableRows, 1, 50);
        this.newTableColumns = this.normalizeTableSize(this.newTableColumns, 1, 20);
        this.ensureNewTableHeaders();
    }

    onNewTableHtmlChanged(nextHtml: string): void {
        const normalizedHtml = this.normalizeTableHtml(nextHtml);
        const placement = this.buildDefaultTablePlacement(this.newFieldPage, this.fields.length);
        if (this.doesTableHtmlFitPlacement(normalizedHtml, placement)) {
            this.newTableHtml = normalizedHtml;
            return;
        }

        this.newTableHtml = this.lastValidNewTableHtml || this.getDefaultTableHtml();
        console.warn('[template-builder] table size limit reached for new table field', {
            placement,
            attempted: this.getTableHtmlDimensions(normalizedHtml),
            allowed: this.getAllowedTableDimensions(placement)
        });
    }

    onNewTableEditorReady(editor: any): void {
        this.newTableEditor = editor;
        this.lastValidNewTableHtml = this.normalizeTableHtml(this.newTableHtml);
        this.attachTableFormulaLiveSync(editor, (syncedHtml) => {
            this.newTableHtml = syncedHtml;
            this.lastValidNewTableHtml = syncedHtml;
        });
        this.attachExcelTableIndicators(editor);
        this.runInitialTableFormulaSync(editor, (syncedHtml) => {
            this.newTableHtml = syncedHtml;
            this.lastValidNewTableHtml = syncedHtml;
        });
        this.hideTableEditorTopToolbar(editor);
        this.attachNewTableEditorSizeGuard(editor);
    }

    onNewTableEditorChanged(event: any): void {
        if (this.revertingNewTableEditor) {
            return;
        }
        const editor = event?.editor || this.newTableEditor;
        const nextHtml = this.normalizeTableHtml(editor?.getData ? editor.getData() : this.newTableHtml);
        const placement = this.buildDefaultTablePlacement(this.newFieldPage, this.fields.length);
        if (!this.shouldRejectNewTableChange(editor, nextHtml, placement)) {
            this.newTableHtml = nextHtml;
            this.lastValidNewTableHtml = nextHtml;
            return;
        }

        this.revertingNewTableEditor = true;
        this.revertTableEditor(editor, this.lastValidNewTableHtml || this.getDefaultTableHtml());
        this.newTableHtml = this.lastValidNewTableHtml || this.getDefaultTableHtml();
        setTimeout(() => {
            this.revertingNewTableEditor = false;
        });
        console.warn('[template-builder] table size limit reached for new table editor', {
            placement,
            attempted: this.getTableHtmlDimensions(nextHtml),
            allowed: this.getAllowedTableDimensions(placement)
        });
    }

    onFieldTableSizeChanged(field: TemplateField): void {
        if (field.type !== 'table') {
            return;
        }
        field.tableConfig = this.normalizeTableConfig(field.tableConfig);
    }

    onTableFieldHtmlChanged(field: TemplateField, nextHtml: string): void {
        if (field.type !== 'table') {
            field.tableHtml = nextHtml;
            return;
        }
        if (this.revertingTableFieldIds.has(field.id)) {
            return;
        }

        const normalizedHtml = this.normalizeTableHtml(nextHtml);
        if (!this.shouldRejectTableFieldChange(field, null, normalizedHtml)) {
            field.tableHtml = normalizedHtml;
            return;
        }

        const previousHtml = this.getRestorableTableHtmlForField(field);
        this.revertingTableFieldIds.add(field.id);
        field.tableHtml = previousHtml;
        setTimeout(() => {
            field.tableHtml = previousHtml;
            this.revertingTableFieldIds.delete(field.id);
        });
        console.warn('[template-builder] table size limit reached for field', {
            fieldId: field.id,
            fieldLabel: field.label,
            placement: field.placement,
            attempted: this.getTableHtmlDimensions(normalizedHtml),
            allowed: field.placement ? this.getAllowedTableDimensions(field.placement) : null
        });
    }

    onTableFieldEditorChanged(field: TemplateField, event: any): void {
        if (field.type !== 'table' || this.revertingTableFieldIds.has(field.id)) {
            return;
        }
        const editor = event?.editor || this.tableEditorsByFieldId[field.id];
        const nextHtml = this.normalizeTableHtml(editor?.getData ? editor.getData() : field.tableHtml);
        if (!this.shouldRejectTableFieldChange(field, editor, nextHtml)) {
            field.tableHtml = nextHtml;
            this.lastValidTableHtmlByFieldId[field.id] = nextHtml;
            return;
        }

        const previousHtml = this.getRestorableTableHtmlForField(field);
        this.revertingTableFieldIds.add(field.id);
        this.revertTableEditor(editor, previousHtml);
        field.tableHtml = previousHtml;
        setTimeout(() => {
            field.tableHtml = previousHtml;
            this.revertingTableFieldIds.delete(field.id);
        });
        console.warn('[template-builder] table editor change reverted because it exceeds field size', {
            fieldId: field.id,
            fieldLabel: field.label,
            placement: field.placement,
            attempted: this.getTableHtmlDimensions(nextHtml),
            allowed: field.placement ? this.getAllowedTableDimensions(field.placement) : null
        });
    }

    addNewRadioOption(): void {
        this.newRadioOptions.push({
            id: this.createId('radio_option_input'),
            label: `Option ${this.newRadioOptions.length + 1}`
        });
    }

    removeNewRadioOption(index: number): void {
        if (this.newRadioOptions.length <= 1) {
            this.newRadioOptions[0].label = 'Option 1';
            return;
        }
        this.newRadioOptions.splice(index, 1);
    }

    trackByRadioOptionId(_: number, option: { id: string }): string {
        return option.id;
    }

    onAddPipelineClicked(): void {
        this.showPipelineSourceModal = true;
    }

    closePipelineSourceModal(): void {
        this.showPipelineSourceModal = false;
    }

    choosePipelineManualStep(): void {
        this.closePipelineSourceModal();
        this.openPipelineModal();
    }

    openPipelineLibraryPicker(): void {
        this.closePipelineSourceModal();
        this.showPipelineLibraryModal = true;
        this.libraryPipelineSearch = '';
        this.selectedLibraryPipelineId = null;
        this.libraryPipelinesError = '';
        this.loadLibraryPipelines();
    }

    closePipelineLibraryModal(): void {
        this.showPipelineLibraryModal = false;
        this.selectedLibraryPipelineId = null;
    }

    loadLibraryPipelines(): void {
        this.libraryPipelinesLoading = true;
        this.libraryPipelinesError = '';
        this.workflowPipelineService.getAll().subscribe({
            next: (rows) => {
                this.libraryPipelines = (rows || []).filter((row) => row?.active !== false);
                this.libraryPipelinesLoading = false;
            },
            error: () => {
                this.libraryPipelines = [];
                this.libraryPipelinesLoading = false;
                this.libraryPipelinesError =
                    'Could not load workflow pipelines. Ensure you have access to Master Data → Workflow Pipelines.';
            },
        });
    }

    getFilteredLibraryPipelines(): WorkflowPipelineListItem[] {
        const term = this.libraryPipelineSearch.trim().toLowerCase();
        if (!term) {
            return this.libraryPipelines;
        }
        return this.libraryPipelines.filter((row) => {
            const name = String(row.name || '').toLowerCase();
            const code = String(row.code || '').toLowerCase();
            const desc = String(row.description || '').toLowerCase();
            return name.includes(term) || code.includes(term) || desc.includes(term);
        });
    }

    selectLibraryPipeline(row: WorkflowPipelineListItem): void {
        this.selectedLibraryPipelineId = row?.id ?? null;
    }

    applySelectedLibraryPipeline(): void {
        const id = this.selectedLibraryPipelineId;
        if (!id) {
            this.notificationService.showMessage('Select a pipeline from the library.', 'warning');
            return;
        }
        this.workflowPipelineService.getById(id).subscribe({
            next: (detail) => {
                const steps = Array.isArray(detail.pipeline) ? detail.pipeline : [];
                if (steps.length === 0) {
                    this.notificationService.showMessage('Selected pipeline has no steps.', 'warning');
                    return;
                }
                this.pipelineSteps = this.hydratePipelineStepsFromLibrary(steps as PipelineStep[]);
                this.linkedWorkflowPipelineId = id;
                this.ensureInitiatorPipelineStep();
                this.reorderPipelineSteps();
                this.closePipelineLibraryModal();
                this.notificationService.showMessage(`Loaded pipeline "${detail.name}".`, 'success');
            },
            error: () => {
                this.notificationService.showMessage('Failed to load pipeline details.', 'error');
            },
        });
    }

    private hydratePipelineStepsFromLibrary(steps: PipelineStep[]): PipelineStep[] {
        let cloned: PipelineStep[];
        try {
            cloned = JSON.parse(JSON.stringify(steps)) as PipelineStep[];
        } catch {
            return [];
        }
        return cloned.map((step) => {
            step = this.normalizeLineManagerPipelineStep(step);
            if (step.type === 'department' && step.serDepartmentId && !step.hrTblDepartment) {
                const dept = this.departments.find(
                    (d) => Number(d.serDepartmentId) === Number(step.serDepartmentId)
                );
                if (dept) {
                    step.hrTblDepartment = dept;
                }
            }
            if (step.type === 'role' && step.serRoleId && !step.cfgTblRole) {
                const role = this.roles.find((r) => Number(r.serRoleId) === Number(step.serRoleId));
                if (role) {
                    step.cfgTblRole = role;
                }
            }
            if (step.type === 'individual') {
                const ids = Array.isArray(step.userIds) ? step.userIds : [];
                const fromJson = Array.isArray(step.users) ? step.users : [];
                const users = fromJson
                    .map((user) => this.findUserById(user?.serUserId || user?.userId || user?.id) || user)
                    .filter((user) => !!user);
                if (users.length === 0 && ids.length > 0) {
                    step.users = ids.map((userId) => this.findUserById(userId)).filter((user) => !!user);
                } else {
                    step.users = users;
                }
            }
            return step;
        });
    }

    openPipelineModal(step?: PipelineStep): void {
        this.resetPipelineModalState();
        if (step) {
            this.editingPipelineStepId = step.id;
            this.newPipelineName = step.name || '';
            this.newPipelineApprovalMode = step.approvalMode || 'OR';
            this.selectedPipelineDepartmentId = null;
            this.selectedPipelineRoleId = null;
            this.selectedPipelineUsers = [];

            const pipelineStep = this.normalizeLineManagerPipelineStep(step);
            this.newPipelineType = pipelineStep.type;
            if (pipelineStep.type === 'department') {
                if (pipelineStep.dynamicTarget === 'initiator_hod') {
                    this.selectedPipelineDepartmentId = this.selfHodDepartmentValue;
                } else {
                    this.selectedPipelineDepartmentId = Number(
                        pipelineStep.serDepartmentId || pipelineStep.hrTblDepartment?.serDepartmentId || null
                    );
                }
            } else if (pipelineStep.type === 'role') {
                this.selectedPipelineRoleId = Number(pipelineStep.serRoleId || pipelineStep.cfgTblRole?.serRoleId || null);
            } else if (pipelineStep.type === 'individual') {
                const selectedUsers = (Array.isArray(pipelineStep.users) ? pipelineStep.users : [])
                    .map((user) => this.findUserById(user?.serUserId || user?.userId || user?.id) || user)
                    .filter((user) => !!user);
                if (pipelineStep.dynamicTarget === 'initiator_manager') {
                    selectedUsers.unshift(this.lineManagerPipelineUserOption);
                }
                if (Array.isArray(pipelineStep.dynamicTargets) && pipelineStep.dynamicTargets.includes('initiator')) {
                    selectedUsers.unshift(this.initiatorPipelineUserOption);
                }
                this.selectedPipelineUsers = selectedUsers;
            }

            this.selectedPipelineFieldRights = {};
            this.getEffectivePipelineFieldPermissions(step).forEach((permission) => {
                this.selectedPipelineFieldRights[permission.fieldId] = permission.right;
            });
        }
        this.showPipelineModal = true;
    }

    openPipelineStep(step: PipelineStep, event?: Event): void {
        event?.stopPropagation();
        if (step.type === 'initiator') {
            this.openPipelineRightsModal(step);
            return;
        }
        this.openPipelineModal(step);
    }

    closePipelineModal(): void {
        this.showPipelineModal = false;
        this.resetPipelineModalState();
    }

    openPipelineRightsModal(step: PipelineStep): void {
        this.editingPipelineStepId = step.id;
        this.editingPipelineFieldRights = {};
        this.getEffectivePipelineFieldPermissions(step).forEach((permission) => {
            this.editingPipelineFieldRights[permission.fieldId] = permission.right;
        });
        this.showPipelineRightsModal = true;
    }

    closePipelineRightsModal(): void {
        this.showPipelineRightsModal = false;
        this.editingPipelineStepId = '';
        this.editingPipelineFieldRights = {};
    }

    savePipelineStep(): void {
        const step = this.buildPipelineStep();
        if (!step) {
            return;
        }

        const existingIndex = this.pipelineSteps.findIndex((item) => item.id === this.editingPipelineStepId);
        if (existingIndex >= 0) {
            this.pipelineSteps[existingIndex] = {
                ...step,
                id: this.editingPipelineStepId,
                order: this.pipelineSteps[existingIndex].order
            };
        } else {
            this.pipelineSteps.push(step);
        }
        this.reorderPipelineSteps();
        this.closePipelineModal();
    }

    savePipelineRights(): void {
        const step = this.editingPipelineStep;
        if (!step) {
            this.closePipelineRightsModal();
            return;
        }

        step.fieldPermissions = this.buildFieldPermissionsFromRights(this.editingPipelineFieldRights);
        step.fieldPermissionsConfigured = true;
        this.closePipelineRightsModal();
    }

    movePipelineStep(index: number, direction: 'up' | 'down'): void {
        const targetIndex = direction === 'up' ? index - 1 : index + 1;
        if (
            targetIndex < 0 ||
            targetIndex >= this.pipelineSteps.length ||
            this.pipelineSteps[index]?.type === 'initiator' ||
            this.pipelineSteps[targetIndex]?.type === 'initiator'
        ) {
            return;
        }

        [this.pipelineSteps[index], this.pipelineSteps[targetIndex]] = [this.pipelineSteps[targetIndex], this.pipelineSteps[index]];
        this.reorderPipelineSteps();
    }

    removePipelineStep(index: number): void {
        if (this.pipelineSteps[index]?.type === 'initiator') {
            return;
        }
        this.pipelineSteps.splice(index, 1);
        this.reorderPipelineSteps();
    }

    onPipelineTypeChanged(): void {
        this.selectedPipelineDepartmentId = null;
        this.selectedPipelineRoleId = null;
        this.selectedPipelineUsers = [];
        this.showPipelineUserDropdown = false;
        this.newPipelineApprovalMode = 'OR';
    }

    shouldShowPipelineApprovalMode(): boolean {
        return this.newPipelineType !== 'individual' || this.selectedPipelineUsers.length > 1;
    }

    isPipelineFieldSelected(field: TemplateField): boolean {
        return !!this.selectedPipelineFieldRights[field.id];
    }

    onPipelineFieldSelectionChanged(field: TemplateField, checked: boolean): void {
        if (checked) {
            this.selectedPipelineFieldRights[field.id] = this.selectedPipelineFieldRights[field.id] || 'fill';
            return;
        }
        delete this.selectedPipelineFieldRights[field.id];
    }

    getPipelineFieldRight(field: TemplateField): PipelineFieldRight {
        return this.selectedPipelineFieldRights[field.id] || 'fill';
    }

    setPipelineFieldRight(field: TemplateField, right: PipelineFieldRight): void {
        this.selectedPipelineFieldRights[field.id] = right;
    }

    togglePipelineFieldSelection(field: TemplateField): void {
        this.onPipelineFieldSelectionChanged(field, !this.isPipelineFieldSelected(field));
    }

    isEditingPipelineFieldSelected(field: TemplateField): boolean {
        return !!this.editingPipelineFieldRights[field.id];
    }

    onEditingPipelineFieldSelectionChanged(field: TemplateField, checked: boolean): void {
        if (checked) {
            this.editingPipelineFieldRights[field.id] = this.editingPipelineFieldRights[field.id] || 'fill';
            return;
        }
        delete this.editingPipelineFieldRights[field.id];
    }

    getEditingPipelineFieldRight(field: TemplateField): PipelineFieldRight {
        return this.editingPipelineFieldRights[field.id] || 'fill';
    }

    setEditingPipelineFieldRight(field: TemplateField, right: PipelineFieldRight): void {
        this.editingPipelineFieldRights[field.id] = right;
    }

    toggleEditingPipelineFieldSelection(field: TemplateField): void {
        this.onEditingPipelineFieldSelectionChanged(field, !this.isEditingPipelineFieldSelected(field));
    }

    comparePipelineUsers(a: any, b: any): boolean {
        if (this.isInitiatorPipelineUser(a) || this.isInitiatorPipelineUser(b)) {
            return this.isInitiatorPipelineUser(a) && this.isInitiatorPipelineUser(b);
        }
        if (this.isLineManagerPipelineUser(a) || this.isLineManagerPipelineUser(b)) {
            return this.isLineManagerPipelineUser(a) && this.isLineManagerPipelineUser(b);
        }
        const aId = Number(a?.serUserId ?? a?.userId ?? a?.id);
        const bId = Number(b?.serUserId ?? b?.userId ?? b?.id);
        if (!Number.isFinite(aId) || !Number.isFinite(bId) || aId <= 0 || bId <= 0) {
            return false;
        }
        return aId === bId;
    }

    togglePipelineUserDropdown(event?: Event): void {
        event?.stopPropagation();
        this.showPipelineUserDropdown = !this.showPipelineUserDropdown;
        if (!this.showPipelineUserDropdown) {
            this.pipelineUserSearchTerm = '';
        }
    }

    closePipelineUserDropdown(event?: Event): void {
        event?.stopPropagation();
        this.showPipelineUserDropdown = false;
        this.pipelineUserSearchTerm = '';
    }

    isPipelineUserSelected(user: any): boolean {
        return this.selectedPipelineUsers.some((selectedUser) => this.comparePipelineUsers(selectedUser, user));
    }

    togglePipelineUserOption(user: any, event: Event): void {
        event.stopPropagation();
        this.onPipelineUserSelectionChanged(user, !this.isPipelineUserSelected(user));
    }

    trackPipelineUserOption = (index: number, user: any): string => {
        if (user?.dynamicTarget === 'initiator' || user?.id === this.initiatorPipelineUserId) {
            return this.initiatorPipelineUserId;
        }
        if (user?.dynamicTarget === 'initiator_manager' || user?.id === this.lineManagerPipelineUserId) {
            return this.lineManagerPipelineUserId;
        }
        const userId = Number(user?.serUserId ?? user?.userId ?? user?.id);
        return Number.isFinite(userId) && userId > 0 ? `user-${userId}` : `user-index-${index}`;
    };

    onPipelineUserSelectionChanged(user: any, checked: boolean): void {
        let normalizedUser = user;
        if (this.isInitiatorPipelineUser(user)) {
            normalizedUser = this.initiatorPipelineUserOption;
        } else if (this.isLineManagerPipelineUser(user)) {
            normalizedUser = this.lineManagerPipelineUserOption;
        }
        if (checked) {
            if (!this.isPipelineUserSelected(normalizedUser)) {
                this.selectedPipelineUsers = [...this.selectedPipelineUsers, normalizedUser];
            }
            return;
        }
        this.selectedPipelineUsers = this.selectedPipelineUsers.filter(
            (selectedUser) => !this.comparePipelineUsers(selectedUser, normalizedUser)
        );
    }

    private isSelectablePipelineEmployee(user: any): boolean {
        if (!user || this.isInitiatorPipelineUser(user) || this.isLineManagerPipelineUser(user)) {
            return false;
        }
        const userId = Number(user?.serUserId ?? user?.userId ?? user?.id);
        return Number.isFinite(userId) && userId > 0;
    }

    getSelectedPipelineUsersSummary(): string {
        const count = this.selectedPipelineUsers.length;
        if (count === 0) {
            return 'Select employees';
        }
        if (count === 1) {
            return this.getUserName(this.selectedPipelineUsers[0]);
        }
        return `${count} employees selected`;
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        const target = event.target as HTMLElement | null;
        if (!target?.closest('.pipeline-user-dropdown')) {
            this.showPipelineUserDropdown = false;
            this.pipelineUserSearchTerm = '';
        }
    }

    getPipelineFieldPermissionSummary(step: PipelineStep): string {
        if (step.type === 'initiator' && !step.fieldPermissionsConfigured) {
            const count = this.pipelineAssignableFields.length;
            return count > 0 ? `${count} fill` : 'Fills submitted form';
        }

        const permissions = this.getEffectivePipelineFieldPermissions(step);
        if (permissions.length === 0) {
            return 'No field rights assigned';
        }

        const counts = permissions.reduce((acc, permission) => {
            acc[permission.right] += 1;
            return acc;
        }, { fill: 0, edit: 0, hide: 0 });
        return [
            counts.fill ? `${counts.fill} fill` : '',
            counts.edit ? `${counts.edit} edit` : '',
            counts.hide ? `${counts.hide} hidden` : ''
        ].filter(Boolean).join(', ');
    }

    getPipelineTargetLabel(step: PipelineStep): string {
        if (step.type === 'initiator') {
            return 'Current user submitting the form';
        }
        if (step.dynamicTarget === 'initiator_hod') {
            return 'Self HOD - resolves from initiator department';
        }
        if (step.dynamicTarget === 'initiator_manager') {
            return 'Line Manager - resolves from initiator reporting manager';
        }
        if (step.dynamicTarget === 'initiator') {
            return 'Initiator - resolves to current form submitter';
        }
        if (step.type === 'department') {
            return this.getDepartmentName(step.hrTblDepartment) || step.name;
        }
        if (step.type === 'role') {
            return this.getRoleName(step.cfgTblRole) || step.name;
        }

        const users = Array.isArray(step.users) ? step.users : [];
        const labels = [
            ...(Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator') ? ['Initiator'] : []),
            ...users.map((user) => this.getUserName(user)).filter(Boolean)
        ];
        return labels.length > 0 ? labels.join(', ') : step.name;
    }

    getPipelineStepOptionLabel(step: PipelineStep): string {
        const order = step.order || this.pipelineSteps.indexOf(step) + 1;
        return `${order}. ${step.name || this.getPipelineTargetLabel(step)}`;
    }

    getDynamicSignatureTargetOptionLabel(step: PipelineStep): string {
        const stepLabel = this.getPipelineStepOptionLabel(step);
        const targetLabel = this.getPipelineTargetLabel(step);
        const normalizedStepLabel = stepLabel.replace(/^\d+\.\s*/, '').trim().toLowerCase();
        const normalizedTargetLabel = String(targetLabel || '').trim().toLowerCase();

        if (!normalizedTargetLabel || normalizedStepLabel === normalizedTargetLabel) {
            return stepLabel;
        }

        return `${stepLabel} - ${targetLabel}`;
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

    getDynamicSignatureTargetLabel(field: TemplateField): string {
        return this.dynamicSignatureTargetOptions.find((option) => option.id === this.getDynamicSignatureTargetId(field))?.label
            || 'Select signature target';
    }

    getDynamicSignatureTargetId(field: TemplateField): string | undefined {
        return field.signatureTargetId || field.pipelineStepId;
    }

    setDynamicSignatureTarget(field: TemplateField, targetId: string): void {
        field.signatureTargetId = targetId;
        field.pipelineStepId = targetId;
        field.pipelineApproverIndex = 1;
    }

    getDepartmentName(department: any): string {
        return department?.txtDepartmentName || department?.departmentName || department?.name || '';
    }

    getUserName(user: any): string {
        if (this.isInitiatorPipelineUser(user)) {
            return 'Initiator';
        }
        if (this.isLineManagerPipelineUser(user)) {
            return 'Line Manager';
        }
        const name = String(user?.txtUserName || user?.userName || user?.name || '').trim();
        if (name) {
            return name;
        }
        const email = String(user?.txtAddress || user?.email || '').trim();
        if (email) {
            return email;
        }
        const userId = Number(user?.serUserId ?? user?.userId ?? user?.id);
        if (Number.isFinite(userId) && userId > 0) {
            return `Employee #${userId}`;
        }
        return 'Unnamed employee';
    }

    isInitiatorPipelineUser(user: any): boolean {
        return user?.dynamicTarget === 'initiator' || user?.id === this.initiatorPipelineUserId;
    }

    isLineManagerPipelineUser(user: any): boolean {
        return user?.dynamicTarget === 'initiator_manager' || user?.id === this.lineManagerPipelineUserId;
    }

    getRoleName(role: any): string {
        return role?.txtRoleName || role?.roleName || role?.name || '';
    }

    getBuilderIndividualFooterSections(): Array<{ label: string; users: any[] }> {
        const steps = this.pipelineSteps
            .filter((step) => step && step.type !== 'initiator')
            .sort((left, right) => (Number(left?.order) || 0) - (Number(right?.order) || 0));
        if (steps.length === 0) {
            return [{ label: 'New Field', users: [this.getBuilderPlaceholderFooterUser()] }];
        }
        return steps.map((step) => ({
            label: step.name || this.getPipelineTargetLabel(step) || 'New Field',
            users: this.getBuilderIndividualFooterUsers(step)
        }));
    }

    getBuilderIndividualFooterColSpan(section: { users: any[] } | null | undefined): number {
        const users = Array.isArray(section?.users) ? (section?.users ?? []) : [];
        return Math.max(users.length, 1);
    }

    getBuilderIndividualFooterBands() {
        return buildIndividualFooterTableBands(
            this.getBuilderIndividualFooterSections(),
            (section) => this.getBuilderIndividualFooterSlots(section)
        );
    }

    getBuilderIndividualFooterSlots(section: { users: any[] } | null | undefined): any[] {
        const users = Array.isArray(section?.users) ? (section?.users ?? []) : [];
        return users.length > 0 ? users : [null];
    }

    getBuilderIndividualFooterUserLabel(user: any): string {
        return this.getBuilderIndividualFooterUserParts(user)
            .map((value) => this.escapeHtml(value))
            .join('<br>');
    }

    getBuilderIndividualFooterSignatureLabel(user: any): string {
        const name = String(user?.txtUserName || user?.userName || user?.name || 'User').trim();
        return this.escapeHtml(`${name} Signature`);
    }

    getBuilderIndividualFooterTimestampLabel(user: any): string {
        const name = String(user?.txtUserName || user?.userName || user?.name || 'User').trim();
        return this.escapeHtml(`${name} Timestamp`);
    }

    private getBuilderIndividualFooterUsers(step: PipelineStep): any[] {
        const configuredUsers = Array.isArray(step?.users) ? step.users.filter((user) => !!user) : [];
        const dynamicTargets = Array.isArray(step?.dynamicTargets) ? step.dynamicTargets : [];
        const users = [...configuredUsers];

        if (step?.dynamicTarget === 'initiator_hod') {
            users.push({ txtUserName: 'Initiator HOD' });
        } else if (step?.dynamicTarget === 'initiator_manager') {
            users.push({ txtUserName: 'Initiator Manager' });
        } else if (step?.dynamicTarget === 'initiator' || dynamicTargets.includes('initiator')) {
            users.push({ txtUserName: 'Initiator' });
        }

        if (users.length > 0) {
            return users;
        }

        if (step?.type === 'department') {
            return [{ txtUserName: this.getDepartmentName(step.hrTblDepartment) || step.name || 'Department' }];
        }
        if (step?.type === 'role') {
            return [{ txtUserName: this.getRoleName(step.cfgTblRole) || step.name || 'Role' }];
        }

        return [this.getBuilderPlaceholderFooterUser()];
    }

    private getBuilderIndividualFooterUserParts(user: any): string[] {
        const name = user?.txtUserName || user?.userName || user?.name || 'User Name';
        const designation = user?.txtDesignation || user?.designation || 'User Designation';
        const department = user?.txtDepartmentName || user?.departmentName || user?.hrTblDepartment?.txtDepartmentName || 'User Department';
        return [name, designation, department]
            .map((value) => String(value || '').trim())
            .filter((value) => value.length > 0);
    }

    private getBuilderPlaceholderFooterUser(): any {
        return {
            txtUserName: 'User Name',
            txtDesignation: 'User Designation',
            txtDepartmentName: 'User Department'
        };
    }

    selectField(fieldId: string): void {
        if (this.selectedFieldId && this.selectedFieldId !== fieldId) {
            this.releaseTableFieldEditor(this.selectedFieldId);
        }
        this.selectedFieldId = fieldId;
        this.editorContent = this.withSelectedField(fieldId);
        this.editorInstance?.setData(this.editorContent);
    }

    removeField(fieldId: string): void {
        this.releaseTableFieldEditor(fieldId);
        delete this.lastValidTableHtmlByFieldId[fieldId];
        this.fields = this.fields.filter((field) => field.id !== fieldId);
        this.editorContent = this.replaceFieldMarkers(fieldId);
        this.editorInstance?.setData(this.editorContent);
        if (this.selectedFieldId === fieldId) {
            this.selectedFieldId = '';
        }
    }

    moveField(index: number, direction: 'up' | 'down'): void {
        const targetIndex = direction === 'up' ? index - 1 : index + 1;
        if (index < 0 || index >= this.fields.length || targetIndex < 0 || targetIndex >= this.fields.length) {
            return;
        }

        const reordered = [...this.fields];
        const [movedField] = reordered.splice(index, 1);
        reordered.splice(targetIndex, 0, movedField);
        this.fields = reordered;
    }

    onSelectedFieldTypeChanged(field: TemplateField, nextType: TemplateFieldType): void {
        if (!field || field.type === nextType) {
            return;
        }

        const previousType = field.type;
        const previousLabel = field.label;
        const previousPlaceholder = field.placeholder;
        const previousPlacement = field.placement ? { ...field.placement } : undefined;
        const previousRadioPlacement = field.optionPlacements?.[0]
            ? {
                page: field.optionPlacements[0].page,
                placement: { ...field.optionPlacements[0].placement }
            }
            : undefined;
        const previousDefaultLabel = this.getDynamicApprovalDefaultLabel(previousType);
        const previousDefaultPlaceholder = this.getDynamicApprovalPlaceholder(previousType);

        field.type = nextType;

        if (nextType === 'radio') {
            field.options = this.getExistingOrDefaultRadioOptions(field.options);
            delete field.tableConfig;
            field.optionPlacements = [];
            const page = this.clamp(Math.round(Number(field.page) || 1), 1, this.pageCount);
            this.getRadioOptions(field).forEach((option, index) => {
                field.optionPlacements?.push({
                    id: this.createId('radio_option'),
                    label: option,
                    page,
                    placement: this.buildRadioOptionPlacement(
                        previousPlacement?.x,
                        previousPlacement?.y,
                        page,
                        index,
                        previousPlacement
                    )
                });
            });
            delete field.placement;
            field.style.fontSize = Math.max(field.style.fontSize || 14, 18);
            field.style.bold = true;
        } else {
            delete field.options;
            delete field.optionPlacements;
            if (nextType === 'table') {
                field.tableConfig = this.normalizeTableConfig(field.tableConfig);
                field.tableHtml = this.normalizeTableHtml(field.tableHtml);
            } else {
                delete field.tableConfig;
                delete field.tableHtml;
            }

            if (this.isAttachmentFieldType(nextType)) {
                delete field.placement;
            } else if (this.isDocumentRegionFieldType(nextType)) {
                field.placement = this.buildDocumentRegionPlacement(nextType, field.page);
            } else if (nextType === 'table') {
                field.placement = previousPlacement || this.buildDefaultTablePlacement(field.page, this.fields.indexOf(field));
            } else if (previousType === 'radio' && previousRadioPlacement) {
                field.page = previousRadioPlacement.page;
                field.placement = {
                    x: previousRadioPlacement.placement.x,
                    y: previousRadioPlacement.placement.y,
                    width: Math.max(160, previousRadioPlacement.placement.width * 4),
                    height: Math.max(38, previousRadioPlacement.placement.height)
                };
            } else if (previousPlacement && !this.isDocumentRegionFieldType(previousType)) {
                field.placement = previousPlacement;
            } else {
                field.placement = this.buildDefaultFloatingPlacement(field.page, this.fields.indexOf(field));
            }
        }

        if (this.isDynamicApprovalDataField(field)) {
            if (!previousLabel || previousLabel === previousDefaultLabel) {
                field.label = this.getDynamicApprovalDefaultLabel(nextType);
            }
            if (!previousPlaceholder || previousPlaceholder === previousDefaultPlaceholder) {
                field.placeholder = this.getDynamicApprovalPlaceholder(nextType);
            }
            field.signatureTargetId = field.signatureTargetId || this.dynamicSignatureTargetOptions[0]?.id || this.initiatorPipelineStepId;
            field.pipelineStepId = field.signatureTargetId;
            field.pipelineApproverIndex = 1;
        } else {
            delete field.signatureTargetId;
            delete field.pipelineStepId;
            delete field.pipelineApproverIndex;
            if (previousPlaceholder === previousDefaultPlaceholder || !previousPlaceholder) {
                field.placeholder = `{{${this.slugify(field.label || 'field')}}}`;
            }
        }

        if (!this.isDocumentRegionFieldType(nextType) && !this.isAttachmentFieldType(nextType) && !field.placement) {
            field.placement = this.buildDefaultFloatingPlacement(field.page, this.fields.indexOf(field));
        }

        this.syncFieldMarkers(field);
        this.selectedFieldId = field.id;
    }

    getRadioOptions(field: TemplateField): string[] {
        return (field.options || []).filter((option) => !!String(option || '').trim());
    }

    getRadioOptionPlacements(field: TemplateField): RadioOptionPlacement[] {
        return Array.isArray(field.optionPlacements) ? field.optionPlacements : [];
    }

    getTableHeaders(field: TemplateField): string[] {
        field.tableConfig = this.normalizeTableConfig(field.tableConfig);
        return field.tableConfig.headers;
    }

    getTablePreviewHtml(field: TemplateField): string {
        field.tableHtml = this.normalizeTableHtml(field.tableHtml);
        return field.tableHtml;
    }

    onInlineTableEditorReady(field: TemplateField, editor: any): void {
        if (field.id !== this.selectedFieldId) {
            return;
        }
        this.releaseTableFieldEditor(field.id);
        field.tableHtml = this.normalizeTableHtml(field.tableHtml);
        this.lastValidTableHtmlByFieldId[field.id] = field.tableHtml;
        this.tableEditorsByFieldId[field.id] = editor;
        this.attachTableFormulaLiveSync(editor, (syncedHtml) => {
            if (field.id !== this.selectedFieldId) {
                return;
            }
            field.tableHtml = syncedHtml;
            this.lastValidTableHtmlByFieldId[field.id] = syncedHtml;
        });
        this.attachExcelTableIndicators(editor);
        this.runInitialTableFormulaSync(editor, (syncedHtml) => {
            if (field.id !== this.selectedFieldId) {
                return;
            }
            field.tableHtml = syncedHtml;
            this.lastValidTableHtmlByFieldId[field.id] = syncedHtml;
        });
        this.attachTableFieldEditorSizeGuard(field, editor);
        this.hideTableEditorTopToolbar(editor);
    }

    addRadioOptionBox(field: TemplateField, optionLabel: string): void {
        if (field.type !== 'radio') {
            return;
        }

        field.optionPlacements = field.optionPlacements || [];
        const existing = field.optionPlacements.find((option) => option.label === optionLabel);
        if (existing) {
            this.selectedFieldId = field.id;
            return;
        }

        const page = this.clamp(Math.round(Number(field.page || this.newFieldPage) || 1), 1, this.pageCount);
        const pageTop = (page - 1) * (this.pageHeight + this.pageGap);
        const index = field.optionPlacements.length;
        field.optionPlacements.push({
            id: this.createId('radio_option'),
            label: optionLabel,
            page,
            placement: {
                x: 32 + (index % 3) * 28,
                y: pageTop + 120 + index * 34,
                width: 30,
                height: 30
            }
        });
        this.selectedFieldId = field.id;
    }

    removeRadioOptionBox(field: TemplateField, optionId: string): void {
        field.optionPlacements = (field.optionPlacements || []).filter((option) => option.id !== optionId);
    }

    startFieldDrag(event: MouseEvent, field: TemplateField): void {
        if (!field.placement || this.isDocumentRegionFieldType(field.type)) {
            return;
        }
        this.startPointerAction(event, field, field.placement, field.id);
    }

    startFieldResize(event: MouseEvent, field: TemplateField): void {
        if (!field.placement || this.isDocumentRegionFieldType(field.type)) {
            return;
        }
        this.startPointerAction(event, field, field.placement, field.id, undefined, 'resize');
    }

    startRadioOptionDrag(event: MouseEvent, field: TemplateField, option: RadioOptionPlacement): void {
        this.startPointerAction(event, field, option.placement, field.id, option.id);
    }

    startRadioOptionResize(event: MouseEvent, field: TemplateField, option: RadioOptionPlacement): void {
        this.startPointerAction(event, field, option.placement, field.id, option.id, 'resize');
    }

    @HostListener('document:mousemove', ['$event'])
    onDocumentMouseMove(event: MouseEvent): void {
        if (!this.activePointerAction) {
            return;
        }

        const target = this.getActivePlacementTarget();
        if (!target) {
            return;
        }

        const { placement } = target;
        const zoom = this.documentZoom || 1;
        const dx = (event.clientX - this.activePointerAction.startClientX) / zoom;
        const dy = (event.clientY - this.activePointerAction.startClientY) / zoom;
        if (this.activePointerAction.mode === 'drag') {
            placement.x = this.clamp(this.activePointerAction.startX + dx, 0, Math.max(0, this.pageWidth - placement.width));
            placement.y = this.clamp(this.activePointerAction.startY + dy, 0, Math.max(0, this.getDocumentHeight() - placement.height));
            target.pageOwner.page = this.getPageForY(placement.y + (placement.height / 2));
            return;
        }

        const pageTop = (target.pageOwner.page - 1) * (this.pageHeight + this.pageGap);
        placement.width = Math.min(this.pageWidth - placement.x, Math.max(this.minFloatingFieldWidth, this.activePointerAction.startWidth + dx));
        placement.height = Math.min(
            pageTop + this.pageHeight - placement.y,
            Math.max(this.minFloatingFieldHeight, this.activePointerAction.startHeight + dy)
        );
    }

    @HostListener('document:mouseup')
    onDocumentMouseUp(): void {
        const target = this.getActivePlacementTarget();
        if (target && !this.isDocumentRegionFieldType(target.field.type)) {
            target.pageOwner.page = this.getPageForY(target.placement.y + (target.placement.height / 2));
            const pageTop = (target.pageOwner.page - 1) * (this.pageHeight + this.pageGap);
            target.placement.y = this.clamp(target.placement.y, pageTop, pageTop + this.pageHeight - target.placement.height);
            this.newFieldPage = target.pageOwner.page;
        }
        this.activePointerAction = null;
    }

    moveFieldToPage(field: TemplateField, value: number | string): void {
        if (!field.placement) {
            return;
        }
        this.movePlacementToPage(field, field.placement, value);
    }

    moveRadioOptionToPage(option: RadioOptionPlacement, value: number | string): void {
        this.movePlacementToPage(option, option.placement, value);
    }

    getFieldStyle(field: TemplateField): { [key: string]: string | number } {
        return {
            ...getTemplateFieldNgStyle(field),
            'font-size.px': this.getFieldPreviewFontSize(field),
        };
    }

    private loadTemplateForEdit(templateId: string): void {
        this.templateWorkflowService.getTemplate(templateId).subscribe({
            next: (template) => {
                const payload = template?.payload || template;
                if (!payload) {
                    return;
                }
                this.editingTemplateId = String(template?.id || payload.id || templateId);
                this.templateName = payload.name || template?.name || 'Untitled Template';
                this.codeConvention = this.resolveCodeConvention(payload.codeConvention || template?.codeConvention);
                this.templateIsDraft = payload.isDraft === true || template?.isDraft === true;
                this.templateIsActive = !this.templateIsDraft
                    && payload.isActive !== false
                    && template?.isActive !== false;
                this.editorContent = this.removeRadioFieldMarkers(payload.html || '');
                this.backgroundFile = payload.background || null;
                this.fields = Array.isArray(payload.fields) ? payload.fields : [];
                this.fields.forEach((field) => {
                    if (field.type === 'table') {
                        field.tableConfig = this.normalizeTableConfig(field.tableConfig);
                        field.tableHtml = this.normalizeTableHtml(field.tableHtml);
                    }
                });
                this.pipelineSteps = (Array.isArray(payload.pipeline) ? payload.pipeline : []).map((step: PipelineStep) =>
                    this.normalizeLineManagerPipelineStep(step)
                );
                const linkedId = Number(payload.workflowPipelineId);
                this.linkedWorkflowPipelineId =
                    Number.isFinite(linkedId) && linkedId > 0 ? linkedId : null;

                const page = payload.page || {};
                this.pageOrientation = page.orientation || (Number(page.width) > Number(page.height) ? 'landscape' : 'portrait');
                const savedPageCount = Math.max(1, Math.round(Number(page.count || this.backgroundFile?.pageCount || 1)));
                this.manualPageCount = savedPageCount;
                this.pageCount = savedPageCount;

                this.ensureInitiatorPipelineStep();
                this.reorderPipelineSteps();
                this.selectedFieldId = '';
                this.previewMode = false;
                this.showFieldModal = false;
                this.showPipelineModal = false;
                this.showPipelineRightsModal = false;
                this.showPipelineSourceModal = false;
                this.showPipelineLibraryModal = false;
                this.linkedWorkflowPipelineId = null;
                this.editorInstance?.setData(this.editorContent);
                this.schedulePaginationUpdate();
            },
            error: (error) => console.error('Template edit load failed', error)
        });
    }

    getTemplatePayload(): string {
        return JSON.stringify({
            name: this.templateName,
            codeConvention: this.codeConvention,
            isActive: this.templateIsActive,
            html: this.editorContent || '',
            page: {
                width: this.pageWidth,
                height: this.pageHeight,
                gap: this.pageGap,
                count: this.pageCount,
                orientation: this.pageOrientation
            },
            background: this.backgroundFile,
            fields: this.fields,
            pipeline: this.getPipelineStepsForPayload().map((step, index) => ({
                ...step,
                order: index + 1
            })),
            ...(this.linkedWorkflowPipelineId
                ? { workflowPipelineId: this.linkedWorkflowPipelineId }
                : {})
        }, null, 2);
    }

    saveTemplateAsDraft(): void {
        if (this.isSavingTemplate) {
            return;
        }
        const payload = JSON.parse(this.getTemplatePayload());
        if (this.editingTemplateId) {
            payload.id = this.editingTemplateId;
        }
        this.isSavingTemplate = true;
        this.templateWorkflowService.saveTemplate(payload, this.editingTemplateId || undefined, { asDraft: true }).subscribe({
            next: (template) => {
                this.isSavingTemplate = false;
                this.editingTemplateId = String(template.id);
                this.templateIsDraft = true;
                this.templateIsActive = false;
                sessionStorage.setItem('templateBuilder:lastTemplate', JSON.stringify(template.payload));
                this.router.navigate(['/template-builder'], {
                    queryParams: { id: template.id },
                    replaceUrl: true
                });
                this.notificationService.showMessage('Template saved as draft. Publish with Save when ready.', 'success');
            },
            error: (error) => {
                this.isSavingTemplate = false;
                this.notificationService.showMessage(error?.message || 'Template draft could not be saved.', 'danger');
                console.error('Template draft save failed', error);
            }
        });
    }

    saveTemplate(): void {
        if (this.isSavingTemplate) {
            return;
        }
        const payload = JSON.parse(this.getTemplatePayload());
        if (this.editingTemplateId) {
            payload.id = this.editingTemplateId;
        }
        payload.isDraft = false;
        payload.isActive = true;
        this.isSavingTemplate = true;
        this.templateWorkflowService.saveTemplate(payload, this.editingTemplateId || undefined, { asDraft: false }).subscribe({
            next: (template) => {
                this.isSavingTemplate = false;
                this.templateIsDraft = false;
                this.templateIsActive = true;
                sessionStorage.setItem('templateBuilder:lastTemplate', JSON.stringify(template.payload));
                this.notificationService.showMessage('Template published successfully.', 'success');
                this.router.navigate(['/template-list']);
            },
            error: (error) => {
                this.isSavingTemplate = false;
                this.notificationService.showMessage(error?.message || 'Template could not be saved.', 'danger');
                console.error('Template save failed', error);
            }
        });
    }

    private resolveCodeConvention(convention: any): string {
        if (typeof convention === 'string') {
            return convention;
        }
        return convention?.pattern || 'TPL-0000';
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

    isWordEditorFieldType(type: TemplateFieldType): boolean {
        return type === 'word_editor';
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

    isAttachmentFieldType(type: TemplateFieldType): boolean {
        return type === 'attachment';
    }

    getFieldAddActionLabel(): string {
        if (this.isAttachmentFieldType(this.newFieldType)) {
            return 'Add Attachment Field';
        }
        if (this.newFieldType === 'table') {
            return 'Place Table Field';
        }
        return this.isDocumentRegionFieldType(this.newFieldType) ? 'Add Page Region' : 'Place Draggable Field';
    }

    private addInlineOnlyField(): void {
        this.createField();
        this.closeAndResetFieldModal();
    }

    private createField(): TemplateField {
        const defaultLabel = this.getFieldTypeLabel(this.newFieldType);
        const label = this.newFieldLabel.trim() && this.newFieldLabel.trim() !== 'New Field'
            ? this.newFieldLabel.trim()
            : defaultLabel;
        const field: TemplateField = {
            id: this.createId('field'),
            label,
            type: this.newFieldType,
            required: this.newFieldRequired,
            placeholder: `{{${this.slugify(label || 'field')}}}`,
            style: {
                fontSize: 14,
                bold: false,
                italic: false,
                underline: false,
                fontFamily: 'Arial, sans-serif',
                textAlign: 'center'
            },
            page: this.getClampedTargetPage()
        };

        if (field.type === 'radio') {
            field.options = this.getCleanNewRadioOptions();
            field.optionPlacements = [];
            field.style.fontSize = 18;
            field.style.bold = true;
        }

        if (field.type === 'table') {
            field.tableConfig = this.buildNewTableConfig();
            field.tableHtml = this.normalizeTableHtml(this.cloneTableFieldHtml(this.newTableHtml));
            this.lastValidTableHtmlByFieldId[field.id] = field.tableHtml;
            field.style.textAlign = 'left';
        }

        if (this.isDynamicApprovalDataField(field)) {
            field.label = label || this.getDynamicApprovalDefaultLabel(field.type);
            field.placeholder = this.getDynamicApprovalPlaceholder(field.type);
            field.signatureTargetId = this.dynamicSignatureTargetOptions[0]?.id || this.initiatorPipelineStepId;
            field.pipelineStepId = field.signatureTargetId;
            field.pipelineApproverIndex = 1;
        }

        this.fields.push(field);
        this.selectedFieldId = field.id;
        this.resetNewFieldForm();
        return field;
    }

    private addMissingRadioOptionBoxes(field: TemplateField): void {
        this.getRadioOptions(field).forEach((option) => this.addRadioOptionBox(field, option));
    }

    private getCleanNewRadioOptions(): string[] {
        this.ensureNewRadioOptions();
        const seen = new Set<string>();
        const cleaned = this.newRadioOptions
            .map((option) => String(option.label || '').trim())
            .filter((label) => {
                if (!label || seen.has(label.toLowerCase())) {
                    return false;
                }
                seen.add(label.toLowerCase());
                return true;
            });
        return cleaned.length > 0 ? cleaned : ['Option 1'];
    }

    private ensureNewRadioOptions(): void {
        if (this.newRadioOptions.length > 0) {
            return;
        }
        this.newRadioOptions = [
            { id: this.createId('radio_option_input'), label: 'Option 1' },
            { id: this.createId('radio_option_input'), label: 'Option 2' }
        ];
    }

    private ensureNewTableHeaders(): void {
        const columns = this.normalizeTableSize(this.newTableColumns, 1, 20);
        this.newTableColumns = columns;
        this.newTableRows = this.normalizeTableSize(this.newTableRows, 1, 50);
        this.newTableHeaders = Array.from({ length: columns }, (_, index) => {
            const existing = String(this.newTableHeaders[index] || '').trim();
            return existing || `Column ${index + 1}`;
        });
    }

    private buildNewTableConfig(): TableFieldConfig {
        this.ensureNewTableHeaders();
        return this.normalizeTableConfig({
            rows: this.newTableRows,
            columns: this.newTableColumns,
            headers: [...this.newTableHeaders]
        });
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

    private normalizeTableSize(value: any, min: number, max: number): number {
        const numeric = Math.round(Number(value));
        if (!Number.isFinite(numeric)) {
            return min;
        }
        return this.clamp(numeric, min, max);
    }

    private normalizeTableHtml(value: any): string {
        const html = String(value || '').trim();
        return /<table[\s>]/i.test(html) ? html : this.getDefaultTableHtml();
    }

    private doesTableHtmlFitField(field: TemplateField, html: string): boolean {
        if (!field.placement) {
            return true;
        }
        return this.doesTableHtmlFitPlacement(html, field.placement);
    }

    private doesTableHtmlFitPlacement(html: string, placement: TemplateFieldPlacement): boolean {
        const dimensions = this.getTableHtmlDimensions(html);
        const allowed = this.getAllowedTableDimensions(placement);
        return dimensions.rows <= allowed.rows && dimensions.columns <= allowed.columns;
    }

    private shouldRejectTableFieldChange(field: TemplateField, editor: any, nextHtml: string): boolean {
        if (!field.placement) {
            return false;
        }

        const nextDimensions = this.getTableHtmlDimensions(nextHtml);
        const allowedDimensions = this.getAllowedTableDimensions(field.placement);
        const previousDimensions = this.getTableHtmlDimensions(
            this.lastValidTableHtmlByFieldId[field.id] || this.normalizeTableHtml(field.tableHtml)
        );
        const growsStructure =
            nextDimensions.rows > previousDimensions.rows ||
            nextDimensions.columns > previousDimensions.columns;

        if (nextDimensions.rows > allowedDimensions.rows || nextDimensions.columns > allowedDimensions.columns) {
            return growsStructure;
        }

        if (!editor || this.doesRenderedTableFitField(field, editor)) {
            return false;
        }

        return growsStructure;
    }

    private shouldRejectNewTableChange(editor: any, nextHtml: string, placement: TemplateFieldPlacement): boolean {
        const nextDimensions = this.getTableHtmlDimensions(nextHtml);
        const allowedDimensions = this.getAllowedTableDimensions(placement);
        const previousDimensions = this.getTableHtmlDimensions(this.lastValidNewTableHtml || this.getDefaultTableHtml());
        const growsStructure =
            nextDimensions.rows > previousDimensions.rows ||
            nextDimensions.columns > previousDimensions.columns;

        if (nextDimensions.rows > allowedDimensions.rows || nextDimensions.columns > allowedDimensions.columns) {
            return growsStructure;
        }

        if (!editor || this.doesRenderedTableFitPlacement(editor, placement)) {
            return false;
        }

        return growsStructure;
    }

    private getAllowedTableDimensions(placement: TemplateFieldPlacement): { rows: number; columns: number } {
        const usableWidth = Math.max(0, Number(placement?.width || 0) - 12);
        const usableHeight = Math.max(0, Number(placement?.height || 0) - this.tableVerticalChromeHeight);
        return {
            rows: Math.max(1, Math.floor(usableHeight / this.tableMinRowHeight)),
            columns: Math.max(1, Math.floor(usableWidth / this.tableMinColumnWidth))
        };
    }

    private getRestorableTableHtmlForField(field: TemplateField): string {
        const previousHtml = this.lastValidTableHtmlByFieldId[field.id] || this.normalizeTableHtml(field.tableHtml);
        if (!field.placement || this.doesTableHtmlFitField(field, previousHtml)) {
            return previousHtml;
        }
        const trimmedHtml = this.trimTableHtmlToPlacement(previousHtml, field.placement);
        this.lastValidTableHtmlByFieldId[field.id] = trimmedHtml;
        return trimmedHtml;
    }

    private getRestorableNewTableHtml(placement: TemplateFieldPlacement): string {
        const previousHtml = this.lastValidNewTableHtml || this.getDefaultTableHtml();
        if (this.doesTableHtmlFitPlacement(previousHtml, placement)) {
            return previousHtml;
        }
        const trimmedHtml = this.trimTableHtmlToPlacement(previousHtml, placement);
        this.lastValidNewTableHtml = trimmedHtml;
        return trimmedHtml;
    }

    private trimTableHtmlToPlacement(html: string, placement: TemplateFieldPlacement): string {
        const allowed = this.getAllowedTableDimensions(placement);
        const host = document.createElement('div');
        host.innerHTML = this.normalizeTableHtml(html);
        const table = host.querySelector('table');
        if (!table) {
            return this.getDefaultTableHtml();
        }

        const rows = Array.from(table.querySelectorAll('tr'));
        rows.forEach((row, rowIndex) => {
            if (rowIndex >= allowed.rows) {
                row.remove();
                return;
            }
            let columnCount = 0;
            Array.from(row.children).forEach((cell) => {
                columnCount += Number((cell as HTMLTableCellElement).colSpan || 1) || 1;
                if (columnCount > allowed.columns) {
                    cell.remove();
                }
            });
        });

        return host.innerHTML || this.getDefaultTableHtml();
    }

    private getTableHtmlDimensions(html: string): { rows: number; columns: number } {
        const host = document.createElement('div');
        host.innerHTML = this.normalizeTableHtml(html);
        const table = host.querySelector('table');
        if (!table) {
            return { rows: 0, columns: 0 };
        }

        const rows = Array.from(table.querySelectorAll('tr'));
        const columns = rows.reduce((maxColumns, row) => {
            const rowColumns = Array.from(row.children).reduce((count, cell) => {
                const span = Number((cell as HTMLTableCellElement).colSpan || 1);
                return count + (Number.isFinite(span) && span > 0 ? span : 1);
            }, 0);
            return Math.max(maxColumns, rowColumns);
        }, 0);

        return { rows: rows.length, columns: Math.max(columns, 1) };
    }

    private attachTableFieldEditorSizeGuard(field: TemplateField, editor: any): void {
        if (!editor || editor.__templateBuilderTableSizeGuardAttached) {
            return;
        }
        editor.__templateBuilderTableSizeGuardAttached = true;
        editor.model?.document?.on?.('change:data', () => {
            this.scheduleTableSizeEnforcement(() => this.enforceRenderedTableFieldSize(field, editor));
        });
        this.observeEditorTableSize(editor, () => this.enforceRenderedTableFieldSize(field, editor));
        this.scheduleTableSizeEnforcement(() => this.enforceRenderedTableFieldSize(field, editor));
    }

    private attachNewTableEditorSizeGuard(editor: any): void {
        if (!editor || editor.__templateBuilderNewTableSizeGuardAttached) {
            return;
        }
        editor.__templateBuilderNewTableSizeGuardAttached = true;
        editor.model?.document?.on?.('change:data', () => {
            this.scheduleTableSizeEnforcement(() => this.enforceRenderedNewTableSize(editor));
        });
        this.observeEditorTableSize(editor, () => this.enforceRenderedNewTableSize(editor));
        this.scheduleTableSizeEnforcement(() => this.enforceRenderedNewTableSize(editor));
    }

    private enforceRenderedTableFieldSize(field: TemplateField, editor: any): void {
        if (field.type !== 'table' || this.revertingTableFieldIds.has(field.id)) {
            return;
        }
        const nextHtml = this.normalizeTableHtml(editor?.getData ? editor.getData() : field.tableHtml);
        if (!this.shouldRejectTableFieldChange(field, editor, nextHtml)) {
            field.tableHtml = nextHtml;
            this.lastValidTableHtmlByFieldId[field.id] = nextHtml;
            return;
        }

        const previousHtml = this.getRestorableTableHtmlForField(field);
        this.revertingTableFieldIds.add(field.id);
        field.tableHtml = previousHtml;
        this.revertTableEditor(editor, previousHtml);
        setTimeout(() => {
            field.tableHtml = previousHtml;
            this.revertingTableFieldIds.delete(field.id);
        });
        console.warn('[template-builder] rendered table change reverted because it exceeds field bounds', {
            fieldId: field.id,
            fieldLabel: field.label,
            placement: field.placement,
            rendered: this.getRenderedTableSize(editor),
            allowed: field.placement ? this.getAllowedRenderedTableSize(field.placement) : null
        });
    }

    private enforceRenderedNewTableSize(editor: any): void {
        if (this.revertingNewTableEditor) {
            return;
        }
        const placement = this.buildDefaultTablePlacement(this.newFieldPage, this.fields.length);
        const nextHtml = this.normalizeTableHtml(editor?.getData ? editor.getData() : this.newTableHtml);
        if (!this.shouldRejectNewTableChange(editor, nextHtml, placement)) {
            this.newTableHtml = nextHtml;
            this.lastValidNewTableHtml = nextHtml;
            return;
        }

        this.revertingNewTableEditor = true;
        this.newTableHtml = this.getRestorableNewTableHtml(placement);
        this.revertTableEditor(editor, this.newTableHtml);
        setTimeout(() => {
            this.revertingNewTableEditor = false;
        });
        console.warn('[template-builder] rendered new table change reverted because it exceeds field bounds', {
            placement,
            rendered: this.getRenderedTableSize(editor),
            allowed: this.getAllowedRenderedTableSize(placement)
        });
    }

    private doesRenderedTableFitField(field: TemplateField, editor: any): boolean {
        if (!field.placement) {
            return true;
        }
        return this.doesRenderedTableFitPlacement(editor, field.placement);
    }

    private doesRenderedTableFitPlacement(editor: any, placement: TemplateFieldPlacement): boolean {
        const editableOverflow = this.getInlineEditorOverflow(editor);
        if (editableOverflow && (editableOverflow.overflowsX || editableOverflow.overflowsY)) {
            return false;
        }
        const rendered = this.getRenderedTableSize(editor);
        if (!rendered) {
            return true;
        }
        const allowed = this.getAllowedRenderedTableSize(placement);
        return rendered.width <= allowed.width + 2 && rendered.height <= allowed.height + 2;
    }

    private getRenderedTableSize(editor: any): { width: number; height: number } | null {
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        const table = editable?.querySelector('figure.table table, table') as HTMLElement | null;
        if (!editable || !table) {
            return null;
        }
        const tableRect = table.getBoundingClientRect();
        return {
            width: Math.ceil(tableRect.width),
            height: Math.ceil(tableRect.height)
        };
    }

    private getInlineEditorOverflow(editor: any): { overflowsX: boolean; overflowsY: boolean; scrollWidth: number; clientWidth: number; scrollHeight: number; clientHeight: number } | null {
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        if (!editable || !editable.closest('.table-field-inline-editor')) {
            return null;
        }
        const scrollWidth = Math.ceil(editable.scrollWidth);
        const clientWidth = Math.ceil(editable.clientWidth);
        const scrollHeight = Math.ceil(editable.scrollHeight);
        const clientHeight = Math.ceil(editable.clientHeight);
        return {
            overflowsX: scrollWidth > clientWidth + 2,
            overflowsY: scrollHeight > clientHeight + 2,
            scrollWidth,
            clientWidth,
            scrollHeight,
            clientHeight
        };
    }

    private getAllowedRenderedTableSize(placement: TemplateFieldPlacement): { width: number; height: number } {
        return {
            width: Math.max(1, Number(placement?.width || 0) - 12),
            height: Math.max(1, Number(placement?.height || 0) - this.tableVerticalChromeHeight)
        };
    }

    private observeEditorTableSize(editor: any, callback: () => void): void {
        if (!editor || typeof ResizeObserver === 'undefined' || this.tableResizeObserversByEditor.has(editor)) {
            return;
        }
        const editable = editor?.ui?.view?.editable?.element as HTMLElement | undefined;
        const table = editable?.querySelector('figure.table table, table') as HTMLElement | null;
        if (!editable || !table) {
            return;
        }
        const observer = new ResizeObserver(() => this.scheduleTableSizeEnforcement(callback));
        observer.observe(table);
        this.tableResizeObserversByEditor.set(editor, observer);
    }

    private revertTableEditor(editor: any, html: string): void {
        if (!editor) {
            return;
        }
        try {
            if (editor.execute) {
                editor.execute('undo');
            }
        } catch {
            // Fall through to setData; some CKEditor operations cannot be undone cleanly.
        }
        try {
            if (editor.setData) {
                editor.setData(html);
            }
        } catch (error) {
            console.warn('[template-builder] failed to restore table editor data', error);
        }
    }

    private scheduleTableSizeEnforcement(callback: () => void): void {
        setTimeout(() => {
            if (typeof requestAnimationFrame === 'function') {
                requestAnimationFrame(callback);
            } else {
                callback();
            }
        }, 0);
    }

    private hideTableEditorTopToolbar(editor: any): void {
        const toolbar = editor?.ui?.view?.toolbar?.element as HTMLElement | undefined;
        const editorTop = toolbar?.closest('.ck-editor__top') as HTMLElement | null;
        if (editorTop) {
            editorTop.style.display = 'none';
        } else if (toolbar) {
            toolbar.style.display = 'none';
        }
    }

    private getDefaultTableHtml(): string {
        return '<figure class="table"><table><tbody><tr><td>Header 1</td><td>Header 2</td></tr><tr><td></td><td></td></tr></tbody></table></figure>';
    }

    private cloneTableFieldHtml(html: string): string {
        const host = document.createElement('div');
        host.innerHTML = String(html || '');
        return host.innerHTML;
    }

    private resetNewFieldForm(): void {
        this.newFieldLabel = 'New Field';
        this.newFieldRequired = false;
        this.newRadioOptions = [];
        this.newTableRows = 3;
        this.newTableColumns = 3;
        this.newTableHeaders = ['Column 1', 'Column 2', 'Column 3'];
        this.newTableHtml = this.getDefaultTableHtml();
        this.lastValidNewTableHtml = this.newTableHtml;
    }

    private getDynamicApprovalDefaultLabel(type: TemplateFieldType): string {
        if (type === 'dynamic_approver_name') {
            return 'Dynamic Approver Name';
        }
        if (type === 'dynamic_approval_timestamp') {
            return 'Dynamic Approval Timestamp';
        }
        if (type === 'dynamic_approver_department') {
            return 'Dynamic Approver Department';
        }
        if (type === 'dynamic_approver_designation') {
            return 'Dynamic Approver Designation';
        }
        if (type === 'dynamic_user_details') {
            return 'User Dynamic Details';
        }
        return 'Dynamic Signatures';
    }

    private getDynamicApprovalPlaceholder(type: TemplateFieldType): string {
        if (type === 'dynamic_approver_name') {
            return '{{dynamic_approver_name}}';
        }
        if (type === 'dynamic_approval_timestamp') {
            return '{{dynamic_approval_timestamp}}';
        }
        if (type === 'dynamic_approver_department') {
            return '{{dynamic_approver_department}}';
        }
        if (type === 'dynamic_approver_designation') {
            return '{{dynamic_approver_designation}}';
        }
        if (type === 'dynamic_user_details') {
            return '{{dynamic_user_details}}';
        }
        return '{{dynamic_signatures}}';
    }

    private getExistingOrDefaultRadioOptions(options?: string[]): string[] {
        const cleaned = (options || [])
            .map((option) => String(option || '').trim())
            .filter((option) => !!option);
        return cleaned.length > 0 ? cleaned : ['Option 1', 'Option 2'];
    }

    private buildDefaultFloatingPlacement(page: number, index: number): TemplateFieldPlacement {
        const normalizedPage = this.clamp(Math.round(Number(page) || 1), 1, this.pageCount);
        const pageTop = (normalizedPage - 1) * (this.pageHeight + this.pageGap);
        return {
            x: 32 + (Math.max(index, 0) % 4) * 18,
            y: pageTop + 120 + (Math.max(index, 0) % 5) * 18,
            width: 160,
            height: 38
        };
    }

    private buildDefaultTablePlacement(page: number, index: number): TemplateFieldPlacement {
        const base = this.buildDefaultFloatingPlacement(page, index);
        return {
            ...base,
            width: Math.min(this.pageWidth - base.x, 420),
            height: 160
        };
    }

    private buildDocumentRegionPlacement(type: TemplateFieldType, page: number): TemplateFieldPlacement {
        const normalizedPage = this.clamp(Math.round(Number(page) || 1), 1, this.pageCount);
        const pageTop = (normalizedPage - 1) * (this.pageHeight + this.pageGap);
        const isHeader = this.isHeaderFieldType(type);
        return {
            x: 0,
            y: isHeader ? pageTop : pageTop + this.pageHeight - 86,
            width: this.pageWidth,
            height: isHeader ? 116 : 86
        };
    }

    private buildRadioOptionPlacement(
        baseX: number | undefined,
        baseY: number | undefined,
        page: number,
        index: number,
        fallbackPlacement?: TemplateFieldPlacement
    ): TemplateFieldPlacement {
        const pageTop = (page - 1) * (this.pageHeight + this.pageGap);
        const offsetX = baseX ?? fallbackPlacement?.x ?? 32;
        const offsetY = baseY ?? fallbackPlacement?.y ?? pageTop + 120;
        return {
            x: offsetX + ((index % 3) * 36),
            y: offsetY + (Math.floor(index / 3) * 36),
            width: 30,
            height: 30
        };
    }

    private syncFieldMarkers(field: TemplateField): void {
        const parser = new DOMParser();
        const doc = parser.parseFromString(this.editorContent || '', 'text/html');
        const markers = Array.from(doc.querySelectorAll(`[data-field-id="${field.id}"]`));

        if (field.type === 'radio') {
            markers.forEach((node) => node.remove());
        } else {
            markers.forEach((node) => {
                node.setAttribute('data-field-type', field.type);
                node.setAttribute('data-required', String(field.required));
                node.setAttribute('title', `${field.label} (${field.type})`);
            });
        }

        this.editorContent = this.removeRadioFieldMarkers(doc.body.innerHTML);
        this.editorInstance?.setData(this.editorContent);
    }

    private closeAndResetFieldModal(): void {
        this.showFieldModal = false;
        this.resetNewFieldForm();
    }

    private startPointerAction(
        event: MouseEvent,
        field: TemplateField,
        placement: TemplateFieldPlacement,
        fieldId: string,
        radioOptionId?: string,
        mode: 'drag' | 'resize' = 'drag'
    ): void {
        event.preventDefault();
        event.stopPropagation();
        this.selectedFieldId = field.id;
        this.activePointerAction = {
            fieldId,
            radioOptionId,
            mode,
            startClientX: event.clientX,
            startClientY: event.clientY,
            startX: placement.x,
            startY: placement.y,
            startWidth: placement.width,
            startHeight: placement.height
        };
    }

    private getActivePlacementTarget(): { field: TemplateField; placement: TemplateFieldPlacement; pageOwner: { page: number } } | null {
        if (!this.activePointerAction) {
            return null;
        }

        const field = this.fields.find((item) => item.id === this.activePointerAction?.fieldId);
        if (!field) {
            return null;
        }

        if (this.activePointerAction.radioOptionId) {
            const option = (field.optionPlacements || []).find((item) => item.id === this.activePointerAction?.radioOptionId);
            return option ? { field, placement: option.placement, pageOwner: option } : null;
        }

        return field.placement ? { field, placement: field.placement, pageOwner: field } : null;
    }

    private movePlacementToPage(owner: { page: number }, placement: TemplateFieldPlacement, value: number | string): void {
        const currentPageTop = (owner.page - 1) * (this.pageHeight + this.pageGap);
        const pageOffsetY = placement.y - currentPageTop;
        const nextPage = this.clamp(Math.round(Number(value) || 1), 1, this.pageCount);
        const nextPageTop = (nextPage - 1) * (this.pageHeight + this.pageGap);
        owner.page = nextPage;
        placement.y = this.clamp(nextPageTop + pageOffsetY, nextPageTop, nextPageTop + this.pageHeight - placement.height);
    }

    private scalePlacementOwner(owner: { page: number }, placement: TemplateFieldPlacement | undefined, xScale: number, yScale: number): void {
        if (!placement) {
            return;
        }
        const oldPageTop = (owner.page - 1) * (this.pageOrientation === 'portrait' ? this.a4PortraitWidth + this.pageGap : this.a4PortraitHeight + this.pageGap);
        placement.x *= xScale;
        placement.y = oldPageTop + ((placement.y - oldPageTop) * yScale);
        placement.width *= xScale;
        placement.height *= yScale;
    }

    private buildPipelineStep(): PipelineStep | null {
        const id = this.createId('step');
        const name = this.newPipelineName.trim();
        const approvalMode = this.shouldShowPipelineApprovalMode() ? this.newPipelineApprovalMode : 'OR';
        const fieldPermissions = this.buildPipelineFieldPermissions();

        if (this.newPipelineType === 'department') {
            if (Number(this.selectedPipelineDepartmentId) === this.selfHodDepartmentValue) {
                return { id, name: name || 'Self HOD', type: 'department', approvalMode, dynamicTarget: 'initiator_hod', fieldPermissions, fieldPermissionsConfigured: true };
            }
            const department = this.departments.find((dept) => Number(dept.serDepartmentId) === Number(this.selectedPipelineDepartmentId));
            if (!department) {
                return null;
            }
            return {
                id,
                name: name || this.getDepartmentName(department),
                type: 'department',
                approvalMode,
                serDepartmentId: Number(department.serDepartmentId),
                hrTblDepartment: department,
                fieldPermissions,
                fieldPermissionsConfigured: true
            };
        }

        if (this.newPipelineType === 'role') {
            const role = this.roles.find((item) => Number(item.serRoleId) === Number(this.selectedPipelineRoleId));
            if (!role) {
                return null;
            }
            return {
                id,
                name: name || this.getRoleName(role),
                type: 'role',
                approvalMode,
                serRoleId: Number(role.serRoleId),
                cfgTblRole: role,
                fieldPermissions,
                fieldPermissionsConfigured: true
            };
        }

        const selectedUsers = Array.isArray(this.selectedPipelineUsers) ? this.selectedPipelineUsers : [];
        const users = selectedUsers.filter(
            (user) => !this.isInitiatorPipelineUser(user) && !this.isLineManagerPipelineUser(user)
        );
        const hasInitiator = selectedUsers.some((user) => this.isInitiatorPipelineUser(user));
        const hasLineManager = selectedUsers.some((user) => this.isLineManagerPipelineUser(user));
        if (users.length === 0 && !hasInitiator && !hasLineManager) {
            return null;
        }
        const defaultName = name ||
            (hasLineManager && !hasInitiator && users.length === 0
                ? 'Line Manager'
                : hasInitiator && !hasLineManager && users.length === 0
                    ? 'Initiator'
                    : users.map((user) => this.getUserName(user)).join(', '));
        const individualStep: PipelineStep = {
            id,
            name: defaultName,
            type: 'individual',
            approvalMode,
            users,
            userIds: users.map((user) => Number(user.serUserId || user.userId || user.id)).filter((value) => !!value),
            dynamicTargets: hasInitiator ? ['initiator'] : [],
            fieldPermissions,
            fieldPermissionsConfigured: true
        };
        if (hasLineManager) {
            individualStep.dynamicTarget = 'initiator_manager';
        }
        return individualStep;
    }

    private normalizeLineManagerPipelineStep(step: PipelineStep): PipelineStep {
        if (!step) {
            return step;
        }
        if (step.type === 'department' && step.dynamicTarget === 'initiator_manager') {
            const normalized: PipelineStep = {
                ...step,
                type: 'individual',
                users: Array.isArray(step.users) ? step.users : [],
                userIds: Array.isArray(step.userIds) ? step.userIds : []
            };
            delete (normalized as any).serDepartmentId;
            delete (normalized as any).hrTblDepartment;
            if (!normalized.name || normalized.name === 'Self Manager') {
                normalized.name = 'Line Manager';
            }
            return normalized;
        }
        return step;
    }

    private resetPipelineModalState(): void {
        this.editingPipelineStepId = '';
        this.newPipelineName = '';
        this.newPipelineType = 'department';
        this.newPipelineApprovalMode = 'OR';
        this.selectedPipelineDepartmentId = null;
        this.selectedPipelineRoleId = null;
        this.selectedPipelineUsers = [];
        this.pipelineUserSearchTerm = '';
        this.showPipelineUserDropdown = false;
        this.selectedPipelineFieldRights = {};
    }

    private findUserById(userId: any): any | null {
        const id = Number(userId);
        if (!Number.isFinite(id) || id <= 0) {
            return null;
        }
        return this.allUsers.find((user) => Number(user?.serUserId ?? user?.userId ?? user?.id) === id) || null;
    }

    private buildPipelineFieldPermissions(): PipelineFieldPermission[] {
        return this.buildFieldPermissionsFromRights(this.selectedPipelineFieldRights);
    }

    private buildFieldPermissionsFromRights(rights: { [fieldId: string]: PipelineFieldRight }): PipelineFieldPermission[] {
        return Object.keys(rights).map((fieldId) => {
            const field = this.fields.find((item) => item.id === fieldId);
            return {
                fieldId,
                fieldLabel: field?.label || fieldId,
                fieldType: field?.type || 'text',
                right: rights[fieldId]
            };
        });
    }

    private hasIndividualFooterPipeline(): boolean {
        return this.fields.some((field) => field.type === 'individual_pipeline_footer');
    }

    private getEffectivePipelineFieldPermissions(step: PipelineStep): PipelineFieldPermission[] {
        if (step.type === 'initiator' && !step.fieldPermissionsConfigured) {
            const defaultRight: PipelineFieldRight = this.hasIndividualFooterPipeline() ? 'edit' : 'fill';
            return this.pipelineAssignableFields.map((field) => ({
                fieldId: field.id,
                fieldLabel: field.label,
                fieldType: field.type,
                right: defaultRight
            }));
        }
        return Array.isArray(step.fieldPermissions) ? step.fieldPermissions : [];
    }

    private getPipelineStepsForPayload(): PipelineStep[] {
        this.ensureInitiatorPipelineStep();
        return this.pipelineSteps.map((step) => ({
            ...this.normalizeLineManagerPipelineStep(step),
            fieldPermissions: this.getEffectivePipelineFieldPermissions(step)
        }));
    }

    private ensureInitiatorPipelineStep(): void {
        if (this.pipelineSteps.some((step) => step.type === 'initiator')) {
            return;
        }
        this.pipelineSteps.unshift({
            id: this.initiatorPipelineStepId,
            name: 'Initiator',
            type: 'initiator',
            approvalMode: 'OR',
            dynamicTarget: 'initiator'
        });
        this.reorderPipelineSteps();
    }

    private reorderPipelineSteps(): void {
        this.pipelineSteps.forEach((step, index) => {
            step.order = index + 1;
        });
    }

    private loadDepartments(): void {
        this.departmentService.getAll().subscribe({
            next: (data: any) => {
                this.departments = Array.isArray(data)
                    ? data.filter((dept) => dept && dept.blIsDeleted !== true && dept.blnStatus !== false)
                    : [];
            },
            error: () => this.departments = []
        });
    }

    private loadUsers(): void {
        this.userService.getUsers().subscribe({
            next: (data: any) => this.allUsers = Array.isArray(data) ? data : [],
            error: () => this.allUsers = []
        });
    }

    private loadRoles(): void {
        this.userService.getRoles().subscribe({
            next: (data: any) => this.roles = Array.isArray(data) ? data : [],
            error: () => this.roles = []
        });
    }

    private getSelectedEditorText(): string {
        const selection = this.editorInstance?.model?.document?.selection;
        if (!selection) {
            return '';
        }
        const selected: string[] = [];
        for (const range of selection.getRanges()) {
            for (const item of range.getItems()) {
                if (item.is?.('$textProxy') || item.is?.('$text')) {
                    selected.push(item.data || '');
                }
            }
        }
        return selected.join('');
    }

    private createId(prefix: string): string {
        return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    }

    private slugify(value: string): string {
        return value.toLowerCase().trim().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || 'field';
    }

    private getFieldTypeLabel(type: TemplateFieldType): string {
        return this.fieldTypes.find((fieldType) => fieldType.value === type)?.label || 'New Field';
    }

    private getClampedTargetPage(): number {
        const page = this.clamp(Math.round(Number(this.newFieldPage) || 1), 1, this.pageCount);
        this.newFieldPage = page;
        return page;
    }

    private getFieldPreviewFontSize(field: TemplateField): number {
        const placement = field.placement || field.optionPlacements?.[0]?.placement;
        if (!placement) {
            return field.style.fontSize;
        }
        const text = field.label || field.placeholder || '';
        const availableWidth = Math.max(1, placement.width - 6);
        const availableHeight = Math.max(1, placement.height - 4);
        const widthLimited = Math.floor((availableWidth / Math.max(1, text.length)) * 1.8);
        return Math.max(4, Math.min(field.style.fontSize, availableHeight, widthLimited || field.style.fontSize));
    }

    private schedulePaginationUpdate(): void {
        if (this.paginationFrame !== null) {
            cancelAnimationFrame(this.paginationFrame);
        }
        this.paginationFrame = requestAnimationFrame(() => {
            this.paginationFrame = null;
            this.updateFieldLayerTop();
            const editable = this.editorInstance?.ui?.view?.editable?.element as HTMLElement | undefined;
            const contentHeight = editable?.scrollHeight || this.pageHeight;
            const contentPages = Math.max(1, Math.ceil(contentHeight / (this.pageHeight + this.pageGap)));
            this.pageCount = Math.max(this.manualPageCount, this.backgroundFile?.pageCount || 1, contentPages);
        });
    }

    private updateFieldLayerTop(): void {
        const frame = this.editorFrame?.nativeElement;
        const editable = this.editorInstance?.ui?.view?.editable?.element as HTMLElement | undefined;
        if (!frame || !editable) {
            this.fieldLayerTop = 0;
            return;
        }
        const frameRect = frame.getBoundingClientRect();
        const editableRect = editable.getBoundingClientRect();
        this.fieldLayerTop = Math.max(0, editableRect.top - frameRect.top);
    }

    private getDocumentHeight(): number {
        return this.pageCount * this.pageHeight + ((this.pageCount - 1) * this.pageGap);
    }

    private getPageForY(y: number): number {
        return this.clamp(Math.floor(y / (this.pageHeight + this.pageGap)) + 1, 1, this.pageCount);
    }

    private clamp(value: number, min: number, max: number): number {
        return Math.min(Math.max(value, min), max);
    }

    private withSelectedField(fieldId: string): string {
        const parser = new DOMParser();
        const doc = parser.parseFromString(this.editorContent || '', 'text/html');
        doc.querySelectorAll('.template-field').forEach((node) => node.classList.remove('selected'));
        doc.querySelectorAll(`[data-field-id="${fieldId}"]`).forEach((node) => node.classList.add('selected'));
        return doc.body.innerHTML;
    }

    private replaceFieldMarkers(fieldId: string): string {
        const parser = new DOMParser();
        const doc = parser.parseFromString(this.editorContent || '', 'text/html');
        doc.querySelectorAll(`[data-field-id="${fieldId}"]`).forEach((node) => {
            node.replaceWith(doc.createTextNode(node.textContent || ''));
        });
        return doc.body.innerHTML;
    }

    private removeRadioFieldMarkers(html: string): string {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html || '', 'text/html');
        const radioFieldIds = new Set(this.fields.filter((field) => field.type === 'radio').map((field) => field.id));
        doc.querySelectorAll('.template-field[data-field-id], .template-field[data-field-type="radio"]').forEach((node) => {
            const fieldId = node.getAttribute('data-field-id') || '';
            const fieldType = node.getAttribute('data-field-type') || '';
            if (fieldType === 'radio' || radioFieldIds.has(fieldId)) {
                node.remove();
            }
        });
        return doc.body.innerHTML;
    }

    private escapeHtml(value: string): string {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    private readFileAsDataUrl(file: File): Promise<string> {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result || ''));
            reader.onerror = () => reject(reader.error);
            reader.readAsDataURL(file);
        });
    }

    private async getPdfPageCount(file: File): Promise<number> {
        try {
            const bytes = await file.arrayBuffer();
            const pdf = await PDFDocument.load(bytes);
            return pdf.getPageCount();
        } catch {
            return 1;
        }
    }

    private async renderPdfPages(file: File): Promise<string[]> {
        try {
            const pdfjsLib: any = await import('pdfjs-dist/legacy/build/pdf');
            const worker: any = await import('pdfjs-dist/legacy/build/pdf.worker.entry');
            pdfjsLib.GlobalWorkerOptions.workerSrc = worker;
            const pdf = await pdfjsLib.getDocument({ data: await file.arrayBuffer() }).promise;
            const pages: string[] = [];
            for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
                const page = await pdf.getPage(pageNumber);
                const viewport = page.getViewport({ scale: this.pdfBackgroundRenderScale });
                const canvas = document.createElement('canvas');
                canvas.width = viewport.width;
                canvas.height = viewport.height;
                const context = canvas.getContext('2d');
                if (!context) {
                    continue;
                }
                await page.render({ canvasContext: context, viewport }).promise;
                pages.push(canvas.toDataURL('image/png'));
            }
            return pages;
        } catch (error) {
            console.error('PDF background render failed', error);
            return [];
        }
    }

    private applyImagePageOrientation(dataUrl: string): void {
        const image = new Image();
        image.onload = () => {
            const nextOrientation = image.naturalWidth > image.naturalHeight ? 'landscape' : 'portrait';
            this.setPageOrientation(nextOrientation);
        };
        image.src = dataUrl;
    }
}
