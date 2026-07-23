import { Component, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { UserService } from 'src/app/services/user/user.service';
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
    'document_header' | 'document_header_qu' | 'document_header_qf' | 'document_header_qri' | 'document_header_qb'
    | 'footer' | 'individual_pipeline_footer' | 'application_code' | 'pipeline_signature' | 'dynamic_signature'
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
    fieldPermissions?: PipelineFieldPermission[];
    fieldPermissionsConfigured?: boolean;
}

@Component({
    selector: 'app-template-approval',
    templateUrl: './template-approval.component.html',
    styleUrls: ['./template-approval.component.css', '../template-fill/template-fill.component.css']
})
export class TemplateApprovalComponent implements OnInit, OnDestroy {
    template: any = null;
    savedTemplate: SavedTemplateDefinition | null = null;
    application: any = null;
    safeHtml: SafeHtml = '';
    values: { [fieldId: string]: any } = {};
    userPipeline: any[] = [];
    remarks = '';
    isLoading = true;
    isSaving = false;
    isActing = false;
    isRedirectingAfterAction = false;
    allUsers: any[] = [];
    selectedOpinionUserId: number | null = null;
    readonly wordEditor = WORD_EDITOR_CKEDITOR;
    readonly wordEditorConfig = WORD_EDITOR_CKEDITOR_CONFIG;
    private acceptedWordEditorValues: { [fieldId: string]: string } = {};
    private revertingWordEditorFields = new Set<string>();
    private previewStepOverride: PipelineStep | null | undefined = undefined;
    private inlineFieldIds = new Set<string>();
    private previewRebuildTimer: ReturnType<typeof setTimeout> | null = null;

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
        const applicationId = this.route.snapshot.paramMap.get('id');
        if (!applicationId) {
            this.isLoading = false;
            return;
        }

        try {
            this.application = await firstValueFrom(this.templateWorkflowService.getApplication(applicationId));
            if (!this.application?.serApplicationId || !this.application?.serFormId) {
                throw new Error('Template application not found');
            }
            this.savedTemplate = await firstValueFrom(this.templateWorkflowService.getTemplate(String(this.application.serFormId)));
            this.template = this.savedTemplate?.payload || null;
            if (!this.template) {
                throw new Error('Template definition not found');
            }
            this.refreshInlineFieldIds();
            this.loadApplicationValues();
            this.rebuildPreviewHtml();
            this.loadUsers();
        } catch (error: any) {
            this.notificationService.showMessage(error?.message || 'Template approval could not be loaded.', 'danger');
        } finally {
            this.isLoading = false;
        }
    }

    ngOnDestroy(): void {
        if (this.previewRebuildTimer) {
            clearTimeout(this.previewRebuildTimer);
            this.previewRebuildTimer = null;
        }
    }

    get activeStep(): PipelineStep | null {
        if (this.previewStepOverride !== undefined) {
            return this.previewStepOverride;
        }
        return this.getPipelineStepForApplication(this.application);
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
        const level = Number(application?.intCurrentApprovalLevel || 0);
        return steps[level] || steps[Math.max(level - 1, 0)] || steps[0];
    }

    get pageIndexes(): number[] {
        const count = this.template?.page?.count || 1;
        return Array.from({ length: count }, (_, index) => index);
    }

    get editableFields(): TemplateField[] {
        if (this.isOpinionPending()) {
            return [];
        }
        return (this.template?.fields || []).filter((field: TemplateField) =>
            !this.isDocumentRegionFieldType(field.type) && this.canCurrentStepEditField(field)
        );
    }

    trackByFieldId(_index: number, field: TemplateField): string {
        return field.id;
    }

    trackByRadioOption(_index: number, option: string): string {
        return option;
    }

    canCurrentStepEditField(field: TemplateField): boolean {
        if (this.isOpinionPending()) {
            return false;
        }
        if (field.type === 'application_code' || this.isDynamicSignatureField(field)) {
            return false;
        }
        const right = this.getCurrentStepFieldRight(field);
        return right === 'fill' || right === 'edit';
    }

    isFieldHiddenForCurrentStep(field: TemplateField): boolean {
        return this.getFieldRightForStep(field, this.activeStep) === 'hide';
    }

    getCurrentStepFieldRight(field: TemplateField): PipelineFieldRight | 'view' {
        return this.getFieldRightForStep(field, this.activeStep);
    }

    isInitiatorResubmissionStep(application: any = this.application): boolean {
        if (!application || this.isOpinionPending()) {
            return false;
        }
        const currentUserId = Number(this.getCurrentUserId() || 0);
        const submitterId = Number(application?.serSubmittedBy || 0);
        if (!Number.isFinite(currentUserId) || currentUserId <= 0 || currentUserId !== submitterId) {
            return false;
        }
        const status = String(application?.txtStatus || '').toUpperCase();
        if (status === 'REJECTED' || status === 'APPROVED' || status === 'COMPLETED') {
            return false;
        }
        const currentApproverId = Number(application?.serCurrentApprover || 0);
        if (currentApproverId > 0 && currentApproverId === submitterId) {
            return true;
        }
        return this.hasLatestSendBackToInitiatorAction(application);
    }

    getFieldRightForStep(field: TemplateField, step: PipelineStep | null): PipelineFieldRight | 'view' {
        if (!step) {
            return 'view';
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

    isDynamicSignatureField(field: TemplateField): boolean {
        return field.type === 'dynamic_signature' || field.type === 'pipeline_signature';
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
        return this.sanitizer.bypassSecurityTrustHtml(String(this.values[field.id] || ''));
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

    async approve(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please enter comments or remarks before approving')) {
            return;
        }
        await this.performAction('approve');
    }

    async reject(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide a rejection reason')) {
            return;
        }
        await this.performAction('reject');
    }

    async sendBack(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide remarks for sending back the application')) {
            return;
        }
        await this.performAction('sendBack');
    }

    async sendBackToInitiator(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
        if (!this.validateRemarks('Please provide remarks for sending back to initiator')) {
            return;
        }
        await this.performAction('sendBackToInitiator');
    }

    async resubmitToPipeline(remarksValue?: string): Promise<void> {
        this.syncRemarks(remarksValue);
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

            this.application = await firstValueFrom(this.templateWorkflowService.getApplication(this.application.serApplicationId));
            const approverStep = this.getPipelineStepForApplication(this.application);
            const initiatorStep = this.getInitiatorPipelineStep();
            const approverPdfBlob = await this.renderTemplatePreviewPdfBlobForStep(approverStep);
            const initiatorPdfBlob = this.stepsHaveSameVisibility(approverStep, initiatorStep)
                ? approverPdfBlob
                : await this.renderTemplatePreviewPdfBlobForStep(initiatorStep);
            const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.application?.txtFormCode || this.application?.serApplicationId}.pdf`;
            await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(this.application.serApplicationId, approverPdfBlob, filename));
            const emailResponse: any = await firstValueFrom(this.templateWorkflowService.sendTemplatePostApprovalEmails(
                this.application.serApplicationId,
                initiatorPdfBlob,
                approverPdfBlob,
                filename
            ));
            if (!emailResponse || emailResponse.status !== 'Success') {
                throw new Error(emailResponse?.message || 'Resubmission email could not be sent');
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
        if (!this.application?.serApplicationId || this.isActing) {
            return;
        }
        this.isActing = true;
        try {
            await this.persistCurrentTemplateState(false);
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
        const data = this.parseApplicationData(this.application?.txtApplicationData);
        const request = data?.templateOpinionRequest;
        return String(this.application?.txtStatus || '').toUpperCase() === 'OPINION_PENDING'
            && (request?.active === true || String(request?.active).toLowerCase() === 'true');
    }

    getOpinionRequesterName(): string {
        const data = this.parseApplicationData(this.application?.txtApplicationData);
        return data?.templateOpinionRequest?.requestedByName || 'Current approver';
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

    private async performAction(action: 'approve' | 'reject' | 'sendBack' | 'sendBackToInitiator'): Promise<void> {
        if (!this.application?.serApplicationId || this.isActing) {
            return;
        }
        this.isActing = true;
        const actingStep = this.activeStep;
        try {
            await this.persistCurrentTemplateState(false);

            let response: any;
            if (action === 'approve') {
                response = await firstValueFrom(this.templateWorkflowService.approveTemplateApplication(
                    this.application.serApplicationId,
                    this.remarks.trim(),
                    this.getCurrentUserId(),
                    true
                ));
            } else if (action === 'reject') {
                response = await firstValueFrom(this.templateWorkflowService.rejectTemplateApplication(this.application.serApplicationId, this.remarks.trim()));
            } else if (action === 'sendBack') {
                response = await firstValueFrom(this.templateWorkflowService.sendBackTemplateApplication(this.application.serApplicationId, this.remarks.trim()));
            } else {
                response = await firstValueFrom(this.templateWorkflowService.sendBackTemplateApplicationToInitiator(this.application.serApplicationId, this.remarks.trim()));
            }

            if (!response || response.status !== 'Success') {
                throw new Error(response?.message || 'Action could not be completed');
            }
            if (action === 'approve') {
                this.application = await firstValueFrom(this.templateWorkflowService.getApplication(this.application.serApplicationId));
                const approverStep = this.getPipelineStepForApplication(this.application);
                this.previewStepOverride = actingStep;
                this.rebuildPreviewHtml();
                const initiatorStep = this.getInitiatorPipelineStep();
                const approverPdfBlob = await this.renderTemplatePreviewPdfBlobForStep(approverStep);
                const initiatorPdfBlob = this.stepsHaveSameVisibility(approverStep, initiatorStep)
                    ? approverPdfBlob
                    : await this.renderTemplatePreviewPdfBlobForStep(initiatorStep);
                const filename = `${this.sanitizeFilename(this.template?.name || 'template-form')}_${this.application?.txtFormCode || this.application?.serApplicationId}.pdf`;
                await firstValueFrom(this.templateWorkflowService.updateTemplateApplicationPdf(this.application.serApplicationId, approverPdfBlob, filename));
                const emailResponse: any = await firstValueFrom(this.templateWorkflowService.sendTemplatePostApprovalEmails(
                    this.application.serApplicationId,
                    initiatorPdfBlob,
                    approverPdfBlob,
                    filename
                ));
                if (!emailResponse || emailResponse.status !== 'Success') {
                    throw new Error(emailResponse?.message || 'Approval email could not be sent');
                }
            }
            this.notificationService.showMessage(response.message || 'Action completed successfully.', 'success');
            await this.redirectToPendingApprovals();
        } catch (error: any) {
            this.notificationService.showMessage(error?.error?.message || error?.message || 'Action could not be completed.', 'danger');
        } finally {
            this.isActing = false;
        }
    }

    private async redirectToPendingApprovals(): Promise<void> {
        this.isRedirectingAfterAction = true;
        await this.router.navigateByUrl('/template-pending-approvals', { replaceUrl: true });
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
        this.application = await firstValueFrom(this.templateWorkflowService.getApplication(this.application.serApplicationId));
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
            if (this.canCurrentStepEditField(field)) {
                acc[field.id] = this.values[field.id];
            }
            return acc;
        }, {});
    }

    private loadApplicationValues(): void {
        const data = this.parseApplicationData(this.application?.txtApplicationData);
        const templateValues = data.templateValues || {};
        this.userPipeline = Array.isArray(data.footerFields) ? data.footerFields : [];
        (this.template?.fields || []).forEach((field: TemplateField) => {
            const value = templateValues[field.id] ?? data[field.id] ?? (field.type === 'application_code' ? this.application?.txtFormCode : (field.type === 'checkbox' ? false : ''));
            this.values[field.id] = value;
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
        return doc.body.innerHTML;
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
        span.style.fontSize = `${field.style?.fontSize || 14}px`;
        span.style.fontWeight = field.style?.bold ? '700' : '400';
        span.style.fontStyle = field.style?.italic ? 'italic' : 'normal';
        span.style.textDecoration = field.style?.underline ? 'underline' : 'none';
        span.style.textAlign = field.style?.textAlign || 'left';
        return span;
    }

    private getDynamicSignatureStep(field: TemplateField): PipelineStep | null {
        const targetId = field.signatureTargetId || field.pipelineStepId;
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline : [];
        return steps.find((step: PipelineStep) => step?.id === targetId) || null;
    }

    private getConfiguredSignatureSlots(step: PipelineStep): any[] {
        const users = Array.isArray(step.users) ? step.users : [];
        if (users.length > 0) {
            return users;
        }

        if ((step as any).dynamicTarget === 'initiator' || (Array.isArray((step as any).dynamicTargets) && (step as any).dynamicTargets.includes('initiator'))) {
            return [{ label: 'Initiator' }];
        }

        return [{ label: step.name || 'Approver' }];
    }

    private getInitiatorSignatureSlots(): any[] {
        const userId = Number(this.application?.serSubmittedBy ?? this.application?.submittedBy ?? this.application?.initiatorUserId);
        return Number.isFinite(userId) && userId > 0 ? [{
            serUserId: userId,
            txtUserName: 'Initiator',
            __signatureApproved: true
        }] : [{ label: 'Initiator' }];
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
            { source: this.getApplicationApprovalHistory(), trustedApproved: false }
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

        return configuredSlots.map((slot) => {
            const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id);
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
        return String(entry?.action || '').toUpperCase() === 'APPROVED';
    }

    private getApplicationApprovalHistory(): any[] {
        return this.parseApplicationData(this.application?.txtApprovalHistory);
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

        const configuredUsers = Array.isArray(step.users) ? step.users : [];
        const configuredUserIds = configuredUsers
            .map((user) => Number(user?.serUserId ?? user?.userId ?? user?.id))
            .filter((userId) => Number.isFinite(userId) && userId > 0);
        const entryUserId = Number(entry?.serUserId ?? entry?.userId ?? entry?.approvedBy ?? entry?.approverId ?? entry?.id);
        const entryLevel = Number(entry.intApprovalOrder ?? entry.level);
        if (configuredUserIds.length > 0) {
            const levelMatches = Number.isFinite(entryLevel) ? this.signatureEntryLevelMatchesStep(entryLevel, step) : true;
            return Number.isFinite(entryUserId) && configuredUserIds.includes(entryUserId) && levelMatches;
        }

        if (Number.isFinite(entryLevel)) {
            return this.signatureEntryLevelMatchesStep(entryLevel, step);
        }

        const role = String(entry.role || '').trim().toLowerCase();
        const stepName = String(step.name || '').trim().toLowerCase();
        return !!role && !!stepName && (role === stepName || role.includes(stepName) || stepName.includes(role));
    }

    private signatureEntryLevelMatchesStep(entryLevel: number, step: PipelineStep): boolean {
        const expectedLevel = this.getSignatureStepHistoryLevel(step);
        return Number.isFinite(expectedLevel) && [expectedLevel, expectedLevel - 1, expectedLevel - 2].includes(entryLevel);
    }

    private getSignatureStepHistoryLevel(step: PipelineStep): number {
        const steps = Array.isArray(this.template?.pipeline) ? this.template.pipeline as PipelineStep[] : [];
        const stepIndex = steps.findIndex((item) => item?.id === step.id);
        const hasInitiator = steps.some((item) => item?.type === 'initiator');
        const order = Number(step.order);
        if (Number.isFinite(order) && order > 0) {
            return hasInitiator ? order + 1 : order;
        }
        return hasInitiator ? stepIndex + 1 : stepIndex + 2;
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

    private async renderTemplatePreviewPdfBlobForStep(step: PipelineStep | null): Promise<Blob> {
        return this.renderTemplatePreviewPdfBlob(step);
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
            valueEl.style.setProperty('transform', 'translateY(1px)', 'important');
            valueEl.style.setProperty('white-space', 'pre-wrap', 'important');
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
