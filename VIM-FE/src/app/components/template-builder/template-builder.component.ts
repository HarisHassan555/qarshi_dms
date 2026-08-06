import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PDFDocument } from 'pdf-lib';
import { DepartmentService } from 'src/app/services/department/department.service';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';
import {
    WORD_EDITOR_CKEDITOR,
    WORD_EDITOR_CKEDITOR_CONFIG,
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
    | 'dynamic_approver_name'
    | 'dynamic_approval_timestamp'
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
type PipelineDynamicTarget = 'initiator_hod' | 'initiator';
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

interface TemplateFieldStyle {
    fontSize: number;
    bold: boolean;
    italic: boolean;
    underline: boolean;
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
    readonly wordEditorConfig = WORD_EDITOR_CKEDITOR_CONFIG;
    private readonly pdfBackgroundRenderScale = 3;
    private readonly minFloatingFieldWidth = 1;
    private readonly minFloatingFieldHeight = 1;
    private readonly initiatorPipelineStepId = 'initiator-step';
    private readonly initiatorPipelineUserId = '__initiator__';

    pageOrientation: 'portrait' | 'landscape' = 'portrait';
    fieldLayerTop = 0;
    templateName = 'Untitled Template';
    codeConvention = 'TPL-0000';
    editingTemplateId = '';
    editorContent = '<h2>Template Title</h2><p>Select any text or table cell content, then assign it as a field from the side panel.</p><p>Example: Vendor Name, Amount, Delivery Date, Approval Notes.</p>';
    selectedFieldId = '';
    newFieldLabel = 'New Field';
    newFieldType: TemplateFieldType = 'text';
    newFieldRequired = false;
    newFieldPage = 1;
    newRadioOptions: { id: string; label: string }[] = [];
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
        private userService: UserService
    ) { }

    ngOnInit(): void {
        document.body.classList.add(TEMPLATE_BUILDER_HIDE_CKEDITOR_BADGE_CLASS);
        this.ensureInitiatorPipelineStep();
        this.loadDepartments();
        this.loadUsers();
        this.loadRoles();
        const templateId = this.route.snapshot.queryParamMap.get('id') || this.route.snapshot.paramMap.get('id') || '';
        if (templateId) {
            this.loadTemplateForEdit(templateId);
        }
    }

    ngOnDestroy(): void {
        document.body.classList.remove(TEMPLATE_BUILDER_HIDE_CKEDITOR_BADGE_CLASS);
    }

    get pageWidth(): number {
        return this.pageOrientation === 'portrait' ? this.a4PortraitWidth : this.a4PortraitHeight;
    }

    get pageHeight(): number {
        return this.pageOrientation === 'portrait' ? this.a4PortraitHeight : this.a4PortraitWidth;
    }

    get pageIndexes(): number[] {
        return Array.from({ length: this.pageCount }, (_, index) => index);
    }

    get selectedField(): TemplateField | undefined {
        return this.fields.find((field) => field.id === this.selectedFieldId);
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

    get initiatorPipelineUserOption(): any {
        return {
            id: this.initiatorPipelineUserId,
            dynamicTarget: 'initiator',
            txtUserName: 'Initiator'
        };
    }

    get pipelineUserOptions(): any[] {
        return [this.initiatorPipelineUserOption, ...this.allUsers];
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
        this.updateFieldLayerTop();
        this.schedulePaginationUpdate();
    }

    onEditorContentChanged(): void {
        const cleanedContent = this.removeRadioFieldMarkers(this.editorContent || '');
        if (cleanedContent !== this.editorContent) {
            this.editorContent = cleanedContent;
            this.editorInstance?.setData(this.editorContent);
        }
        this.schedulePaginationUpdate();
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
        field.placement = {
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

        if (this.editorContent === '<h2>Template Title</h2><p>Select any text or table cell content, then assign it as a field from the side panel.</p><p>Example: Vendor Name, Amount, Delivery Date, Approval Notes.</p>') {
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

    onNewFieldTypeChanged(): void {
        if (this.newFieldType === 'radio') {
            this.ensureNewRadioOptions();
        }
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

    openPipelineModal(step?: PipelineStep): void {
        this.resetPipelineModalState();
        if (step) {
            this.editingPipelineStepId = step.id;
            this.newPipelineName = step.name || '';
            this.newPipelineType = step.type;
            this.newPipelineApprovalMode = step.approvalMode || 'OR';
            this.selectedPipelineDepartmentId = null;
            this.selectedPipelineRoleId = null;
            this.selectedPipelineUsers = [];

            if (step.type === 'department') {
                this.selectedPipelineDepartmentId = step.dynamicTarget === 'initiator_hod'
                    ? this.selfHodDepartmentValue
                    : Number(step.serDepartmentId || step.hrTblDepartment?.serDepartmentId || null);
            } else if (step.type === 'role') {
                this.selectedPipelineRoleId = Number(step.serRoleId || step.cfgTblRole?.serRoleId || null);
            } else if (step.type === 'individual') {
                const selectedUsers = (Array.isArray(step.users) ? step.users : [])
                    .map((user) => this.findUserById(user?.serUserId || user?.userId || user?.id) || user)
                    .filter((user) => !!user);
                if (Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator')) {
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
        return Number(a?.serUserId ?? a?.userId ?? a?.id) === Number(b?.serUserId ?? b?.userId ?? b?.id);
    }

    togglePipelineUserDropdown(event?: Event): void {
        event?.stopPropagation();
        this.showPipelineUserDropdown = !this.showPipelineUserDropdown;
        if (!this.showPipelineUserDropdown) {
            this.pipelineUserSearchTerm = '';
        }
    }

    isPipelineUserSelected(user: any): boolean {
        return this.selectedPipelineUsers.some((selectedUser) => this.comparePipelineUsers(selectedUser, user));
    }

    onPipelineUserSelectionChanged(user: any, checked: boolean): void {
        if (checked) {
            if (!this.isPipelineUserSelected(user)) {
                this.selectedPipelineUsers = [...this.selectedPipelineUsers, user];
            }
            return;
        }
        this.selectedPipelineUsers = this.selectedPipelineUsers.filter((selectedUser) => !this.comparePipelineUsers(selectedUser, user));
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
            || field.type === 'dynamic_approval_timestamp';
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
        return user?.txtUserName || user?.userName || user?.name || user?.email || '';
    }

    isInitiatorPipelineUser(user: any): boolean {
        return user?.dynamicTarget === 'initiator' || user?.id === this.initiatorPipelineUserId;
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
        this.selectedFieldId = fieldId;
        this.editorContent = this.withSelectedField(fieldId);
        this.editorInstance?.setData(this.editorContent);
    }

    removeField(fieldId: string): void {
        this.fields = this.fields.filter((field) => field.id !== fieldId);
        this.editorContent = this.replaceFieldMarkers(fieldId);
        this.editorInstance?.setData(this.editorContent);
        if (this.selectedFieldId === fieldId) {
            this.selectedFieldId = '';
        }
    }

    getRadioOptions(field: TemplateField): string[] {
        return (field.options || []).filter((option) => !!String(option || '').trim());
    }

    getRadioOptionPlacements(field: TemplateField): RadioOptionPlacement[] {
        return Array.isArray(field.optionPlacements) ? field.optionPlacements : [];
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
        const dx = event.clientX - this.activePointerAction.startClientX;
        const dy = event.clientY - this.activePointerAction.startClientY;
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
            'font-size.px': this.getFieldPreviewFontSize(field),
            'font-weight': field.style.bold ? '700' : '400',
            'font-style': field.style.italic ? 'italic' : 'normal',
            'text-decoration': field.style.underline ? 'underline' : 'none',
            'text-align': field.style.textAlign
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
                this.editorContent = this.removeRadioFieldMarkers(payload.html || '');
                this.backgroundFile = payload.background || null;
                this.fields = Array.isArray(payload.fields) ? payload.fields : [];
                this.pipelineSteps = Array.isArray(payload.pipeline) ? payload.pipeline : [];

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
            }))
        }, null, 2);
    }

    saveTemplate(): void {
        const payload = JSON.parse(this.getTemplatePayload());
        if (this.editingTemplateId) {
            payload.id = this.editingTemplateId;
        }
        this.templateWorkflowService.saveTemplate(payload, this.editingTemplateId || undefined).subscribe({
            next: (template) => {
                sessionStorage.setItem('templateBuilder:lastTemplate', JSON.stringify(template.payload));
                this.router.navigate(['/template-list']);
            },
            error: (error) => console.error('Template save failed', error)
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

    private resetNewFieldForm(): void {
        this.newFieldLabel = 'New Field';
        this.newFieldRequired = false;
        this.newRadioOptions = [];
    }

    private getDynamicApprovalDefaultLabel(type: TemplateFieldType): string {
        if (type === 'dynamic_approver_name') {
            return 'Dynamic Approver Name';
        }
        if (type === 'dynamic_approval_timestamp') {
            return 'Dynamic Approval Timestamp';
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
        return '{{dynamic_signatures}}';
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
        const users = selectedUsers.filter((user) => !this.isInitiatorPipelineUser(user));
        const hasInitiator = selectedUsers.some((user) => this.isInitiatorPipelineUser(user));
        if (users.length === 0 && !hasInitiator) {
            return null;
        }
        return {
            id,
            name: name || (hasInitiator && users.length === 0 ? 'Initiator' : users.map((user) => this.getUserName(user)).join(', ')),
            type: 'individual',
            approvalMode,
            users,
            userIds: users.map((user) => Number(user.serUserId || user.userId || user.id)).filter((value) => !!value),
            dynamicTargets: hasInitiator ? ['initiator'] : [],
            fieldPermissions,
            fieldPermissionsConfigured: true
        };
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

    private getEffectivePipelineFieldPermissions(step: PipelineStep): PipelineFieldPermission[] {
        if (step.type === 'initiator' && !step.fieldPermissionsConfigured) {
            return this.pipelineAssignableFields.map((field) => ({
                fieldId: field.id,
                fieldLabel: field.label,
                fieldType: field.type,
                right: 'fill'
            }));
        }
        return Array.isArray(step.fieldPermissions) ? step.fieldPermissions : [];
    }

    private getPipelineStepsForPayload(): PipelineStep[] {
        this.ensureInitiatorPipelineStep();
        return this.pipelineSteps.map((step) => ({
            ...step,
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
