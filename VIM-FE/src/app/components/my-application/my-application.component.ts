import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { urls } from 'src/app/utils/urls';

type TemplateFieldType =
    'document_header' | 'document_header_qu' | 'document_header_qf' | 'document_header_qri' | 'document_header_qb'
    | 'footer' | 'individual_pipeline_footer' | 'application_code' | 'pipeline_signature' | 'dynamic_signature'
    | 'text' | 'integer' | 'decimal' | 'number' | 'date' | 'email' | 'textarea' | 'word_editor'
    | 'attachment' | 'select' | 'checkbox' | 'radio' | 'table' | 'orientation';

interface TemplateField {
    id: string;
    label: string;
    type: TemplateFieldType;
    required: boolean;
    placeholder: string;
    placement?: { x: number; y: number; width: number; height: number };
    style?: {
        fontSize: number;
        bold: boolean;
        italic: boolean;
        underline: boolean;
        textAlign: 'left' | 'center' | 'right';
    };
    pipelineStepId?: string;
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

interface PipelineStep {
    id: string;
    name: string;
    type: string;
    approvalMode?: string;
    order?: number;
    users?: any[];
    dynamicTarget?: string;
    dynamicTargets?: string[];
}

interface MyTemplateApplication {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    dteCreatedDate?: string;
    txtApplicationData?: string;
    txtApprovalHistory?: string;
    txtPriorApprovals?: string;
    serSubmittedBy?: number;
    submittedByUserName?: string;
    templateName?: string;
    template?: SavedTemplateDefinition;
}

interface ProgressTile {
    key: string;
    title: string;
    type: string;
    status: 'APPROVED' | 'PENDING' | 'REJECTED' | 'WAITING' | 'OPINION';
    approver: string;
    approvedAt: string;
    remarks: string;
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

@Component({
    selector: 'app-my-application',
    templateUrl: './my-application.component.html',
    styleUrls: ['./my-application.component.css', '../template-fill/template-fill.component.css']
})
export class MyApplicationComponent implements OnInit, OnDestroy {
    search = '';
    isLoading = false;
    applications: MyTemplateApplication[] = [];
    selectedApplication: MyTemplateApplication | null = null;
    selectedTemplate: any = null;
    safeHtml: SafeHtml = '';
    values: { [fieldId: string]: any } = {};
    page = 0;
    pageSize = 10;
    totalApplications = 0;
    private searchTimer: ReturnType<typeof setTimeout> | null = null;
    cols = [
        { field: 'txtFormCode', title: 'Application Code' },
        { field: 'templateName', title: 'Template' },
        { field: 'txtStatus', title: 'Status' },
        { field: 'intCurrentApprovalLevel', title: 'Approval Level' },
        { field: 'dteCreatedDate', title: 'Submitted Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private sanitizer: DomSanitizer,
        private notificationService: NotificationService,
        private route: ActivatedRoute,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.loadApplications();
        this.route.paramMap.subscribe((params) => {
            if (this.isLoading) {
                return;
            }
            const applicationId = Number(params.get('id'));
            if (!applicationId) {
                this.selectApplication(null);
                return;
            }
            this.loadApplicationDetails(applicationId);
        });
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
    }

    loadApplications(): void {
        const userId = this.getCurrentUserId();
        if (!userId) {
            this.notificationService.showMessage('Current user could not be resolved.', 'danger');
            return;
        }

        this.isLoading = true;
        this.templateWorkflowService.getMyTemplateApplications(userId, this.page, this.pageSize, this.search)
            .pipe(finalize(() => this.isLoading = false))
            .subscribe({
            next: (response: any) => {
                this.applications = (Array.isArray(response?.items) ? response.items : [])
                    .map((application: any) => this.toShallowApplication(application));
                this.totalApplications = Number(response?.total || 0);

                const routeApplicationId = Number(this.route.snapshot.paramMap.get('id'));
                if (routeApplicationId) {
                    this.loadApplicationDetails(routeApplicationId);
                } else {
                    this.selectApplication(null);
                }
            },
            error: (error) => {
                this.applications = [];
                this.totalApplications = 0;
                this.selectApplication(null);
                this.notificationService.showMessage(
                    'Error loading my applications: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
    }

    onSearchChanged(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => {
            this.page = 0;
            this.loadApplications();
        }, 300);
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.loadApplications();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10;
        this.page = 0;
        this.loadApplications();
    }

    get totalPages(): number {
        return Math.max(1, Math.ceil(this.totalApplications / this.pageSize));
    }

    get pageStart(): number {
        return this.totalApplications === 0 ? 0 : (this.page * this.pageSize) + 1;
    }

    get pageEnd(): number {
        return Math.min(this.totalApplications, (this.page + 1) * this.pageSize);
    }

    selectApplication(application: MyTemplateApplication | null): void {
        this.selectedApplication = application;
        this.selectedTemplate = application?.template?.payload || null;
        this.values = {};
        if (!application || !this.selectedTemplate) {
            this.safeHtml = '';
            return;
        }
        this.loadApplicationValues(application);
        this.safeHtml = this.sanitizer.bypassSecurityTrustHtml(this.getFillHtml(this.selectedTemplate.html || ''));
    }

    openApplication(application: MyTemplateApplication): void {
        if (!application?.serApplicationId) {
            this.notificationService.showMessage('Invalid application ID', 'danger');
            return;
        }
        this.router.navigate(['/my-application', application.serApplicationId]);
    }

    backToList(): void {
        this.router.navigate(['/my-application']);
        this.selectApplication(null);
    }

    private loadApplicationDetails(applicationId: number): void {
        this.templateWorkflowService.getApplication(applicationId).subscribe({
            next: (application) => {
                this.templateWorkflowService.getTemplate(String(application?.serFormId || '')).subscribe({
                    next: (template) => {
                        if (!template) {
                            this.notificationService.showMessage('Template definition could not be found for this application.', 'danger');
                            this.selectApplication(null);
                            return;
                        }
                        const listApplication = this.applications.find((item) => Number(item.serApplicationId) === Number(applicationId));
                        this.selectApplication({
                            ...(listApplication || {}),
                            ...application,
                            template,
                            templateName: template.name || listApplication?.templateName || 'Template'
                        });
                    },
                    error: (error) => {
                        this.notificationService.showMessage(
                            'Template definition could not be loaded: ' + (error.error?.message || error.message),
                            'danger'
                        );
                        this.selectApplication(null);
                    }
                });
            },
            error: (error) => {
                this.notificationService.showMessage(
                    'Application details could not be loaded: ' + (error.error?.message || error.message),
                    'danger'
                );
                this.selectApplication(null);
            }
        });
    }

    private toShallowApplication(application: any): MyTemplateApplication {
        return {
            serApplicationId: application?.serApplicationId,
            serFormId: application?.serFormId,
            txtFormCode: application?.txtFormCode,
            txtStatus: application?.txtStatus,
            intCurrentApprovalLevel: application?.intCurrentApprovalLevel,
            dteCreatedDate: application?.dteCreatedDate,
            serSubmittedBy: application?.serSubmittedBy,
            templateName: application?.templateName || application?.cfgTblCustomForm?.txtFormName || 'Template'
        };
    }

    getDisplayedApplications(): MyTemplateApplication[] {
        return this.applications;
    }

    get pageIndexes(): number[] {
        const count = this.selectedTemplate?.page?.count || 1;
        return Array.from({ length: count }, (_, index) => index);
    }

    get progressTiles(): ProgressTile[] {
        if (!this.selectedApplication || !this.selectedTemplate) {
            return [];
        }
        const steps = this.getPipelineSteps();
        const applicationStatus = String(this.selectedApplication.txtStatus || '').toUpperCase();
        const currentLevel = Number(this.selectedApplication.intCurrentApprovalLevel || 0);
        let stepLevel = 1;

        return steps.flatMap((step) => {
            const isInitiator = step.type === 'initiator';
            if (isInitiator) {
                return [this.buildProgressTile(step, 1, applicationStatus, currentLevel, { history: this.getInitiatorHistoryEntry(), index: 0 })];
            }
            stepLevel += 1;
            return this.getProgressApproverSlots(step, stepLevel)
                .map((slot) => this.buildProgressTile(step, stepLevel, applicationStatus, currentLevel, slot));
        });
    }

    getApprovalLogRows(): ApprovalLogRow[] {
        return this.getApplicationApprovalHistory()
            .map((entry, index) => {
                const userId = Number(entry?.serUserId ?? entry?.userId ?? entry?.approvedBy ?? entry?.approverId ?? entry?.id);
                const action = String(entry?.action || entry?.status || '--').toUpperCase();
                const step = this.getStepForHistoryEntry(entry);
                return {
                    level: String(this.getDisplayLevelForHistoryEntry(entry, index)),
                    approver: entry?.approverName || entry?.txtUserName || entry?.userName || entry?.name || (Number.isFinite(userId) ? `User ${userId}` : '--'),
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
        const tiles = this.progressTiles;
        if (tiles.length === 0) {
            return 0;
        }
        const approved = tiles.filter((tile) => tile.status === 'APPROVED').length;
        return Math.round((approved / tiles.length) * 100);
    }

    trackByApplicationId(_index: number, application: MyTemplateApplication): number | string {
        return application.serApplicationId || application.txtFormCode || _index;
    }

    trackByFieldId(_index: number, field: TemplateField): string {
        return field.id;
    }

    trackByTile(_index: number, tile: ProgressTile): string {
        return tile.key;
    }

    isSelected(application: MyTemplateApplication): boolean {
        return !!application.serApplicationId && application.serApplicationId === this.selectedApplication?.serApplicationId;
    }

    getStatusClass(status?: string): string {
        const value = String(status || '').toUpperCase();
        if (value === 'APPROVED') return 'status-approved';
        if (value === 'REJECTED') return 'status-rejected';
        if (value === 'OPINION_PENDING') return 'status-opinion';
        return 'status-pending';
    }

    getStatusBadgeClass(status: string | undefined): string {
        if (!status) return 'badge-outline-secondary';
        switch (status.toUpperCase()) {
            case 'PENDING':
                return 'badge-outline-warning';
            case 'APPROVED':
                return 'badge-outline-success';
            case 'REJECTED':
                return 'badge-outline-danger';
            case 'IN_PROGRESS':
            case 'OPINION_PENDING':
                return 'badge-outline-info';
            default:
                return 'badge-outline-secondary';
        }
    }

    getTileClass(tile: ProgressTile): string {
        return `is-${tile.status.toLowerCase()}`;
    }

    getApplicationDisplayLevel(application: MyTemplateApplication): number {
        const currentLevel = Number(application?.intCurrentApprovalLevel);
        return Number.isFinite(currentLevel) ? currentLevel + 2 : 1;
    }

    getRadioOptionPlacements(field: TemplateField): RadioOptionPlacement[] {
        return Array.isArray(field.optionPlacements) ? field.optionPlacements : [];
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
            return this.selectedApplication?.txtFormCode || '';
        }
        if (field.type === 'checkbox') {
            return this.values[field.id] ? '✓' : '';
        }
        if (field.type === 'radio' && optionLabel !== undefined) {
            return this.normalizeRadioValue(this.values[field.id]) === this.normalizeRadioValue(optionLabel) ? '✓' : '';
        }
        if (this.isDynamicSignatureField(field)) {
            return '';
        }
        return String(this.values[field.id] ?? '');
    }

    getFieldValueHtml(field: TemplateField): SafeHtml {
        return this.sanitizer.bypassSecurityTrustHtml(String(this.values[field.id] || ''));
    }

    isDocumentRegionFieldType(type: TemplateFieldType): boolean {
        return ['document_header', 'document_header_qu', 'document_header_qf', 'document_header_qri', 'document_header_qb', 'footer', 'individual_pipeline_footer'].includes(type);
    }

    isDynamicSignatureField(field: TemplateField): boolean {
        return field.type === 'dynamic_signature' || field.type === 'pipeline_signature';
    }

    getDynamicSignatureSlots(field: TemplateField): any[] {
        const step = this.getDynamicSignatureStep(field);
        if (!step) {
            return [];
        }
        if (step.type === 'initiator') {
            return this.getInitiatorSignatureSlots();
        }

        const approvedSlots = this.getApprovedSignatureSlots(step);
        if (approvedSlots.length > 0 && step.approvalMode !== 'AND') {
            return approvedSlots;
        }

        const configuredSlots = Array.isArray(step.users) ? step.users : [];
        if (step.approvalMode === 'AND') {
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

        return approvedSlots;
    }

    getDynamicSignatureImageUrl(slot: any): string {
        if (!slot?.__signatureApproved) {
            return '';
        }
        const userId = Number(slot?.serUserId ?? slot?.userId ?? slot?.approvedBy ?? slot?.approverId ?? slot?.id);
        if (Number.isFinite(userId) && userId > 0) {
            return `${urls.API_URL}getSignature?userId=${userId}`;
        }
        return '';
    }

    getDynamicSignatureLabel(slot: any): string {
        return slot?.txtUserName || slot?.userName || slot?.approverName || slot?.name || slot?.label || 'Approver';
    }

    private loadApplicationValues(application: MyTemplateApplication): void {
        const data = this.parseApplicationData(application.txtApplicationData);
        const templateValues = data.templateValues || {};
        (this.selectedTemplate?.fields || []).forEach((field: TemplateField) => {
            this.values[field.id] = templateValues[field.id] ?? data[field.id] ?? (field.type === 'application_code' ? application.txtFormCode : (field.type === 'checkbox' ? false : ''));
        });
    }

    private getFillHtml(html: string): string {
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const fields = new Map<string, TemplateField>((this.selectedTemplate?.fields || []).map((field: TemplateField) => [field.id, field]));
        doc.querySelectorAll('.template-field[data-field-id]').forEach((node) => {
            const field = fields.get(node.getAttribute('data-field-id') || '');
            if (!field || field.placement || this.isDocumentRegionFieldType(field.type)) {
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

    private getPipelineSteps(): PipelineStep[] {
        const pipeline = Array.isArray(this.selectedTemplate?.pipeline) ? this.selectedTemplate.pipeline : [];
        if (pipeline.some((step: PipelineStep) => step?.type === 'initiator')) {
            return pipeline;
        }
        return [{ id: 'initiator', name: 'Initiator', type: 'initiator' }, ...pipeline];
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
        const historyAction = String(history?.action || '').toUpperCase();
        let tileStatus: ProgressTile['status'] = 'WAITING';
        const currentStepLevel = currentLevel + 2;

        if (isInitiator) {
            tileStatus = 'APPROVED';
        } else if (historyAction === 'REJECTED') {
            tileStatus = 'REJECTED';
        } else if (historyAction === 'APPROVED') {
            tileStatus = 'APPROVED';
        } else if (applicationStatus === 'APPROVED') {
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
            rawDate: this.getHistoryDateObject(history)
        };
    }

    private getProgressApproverSlots(step: PipelineStep, stepLevel: number): ProgressApproverSlot[] {
        const stepHistory = this.getApprovalEntriesForStep(step, stepLevel);
        const users = Array.isArray(step.users) ? step.users : [];
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

    private getApprovalEntriesForStep(step: PipelineStep, stepLevel: number): any[] {
        const stepId = String(step.id || '').trim();
        return this.getApplicationApprovalHistory().filter((entry) => {
            const action = String(entry?.action || entry?.status || '').toUpperCase();
            if (!['APPROVED', 'REJECTED', 'SENT_BACK', 'SENT_BACK_TO_INITIATOR', 'RESUBMITTED_BY_INITIATOR'].includes(action)) {
                return false;
            }
            const entryStepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim();
            if (entryStepId && stepId) {
                return entryStepId === stepId;
            }
            const rawLevel = this.getRawHistoryLevel(entry);
            if (rawLevel === 0) {
                return this.isFirstApprovalStep(step);
            }
            const entryRole = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
            const stepName = String(step.name || '').trim().toLowerCase();
            if (entryRole && stepName && entryRole !== stepName) {
                return false;
            }
            return this.getLegacyDisplayLevelsForStep(step).includes(rawLevel);
        });
    }

    private getInitiatorHistoryEntry(): any {
        return [...this.getApplicationApprovalHistory()]
            .reverse()
            .find((entry) => {
                const action = String(entry?.action || entry?.status || '').toUpperCase();
                return ['SUBMITTED', 'APPROVED', 'RESUBMITTED_BY_INITIATOR'].includes(action) && this.entryBelongsToSubmission(entry);
            }) || null;
    }

    private getApplicationApprovalHistory(): any[] {
        const entries: any[] = [];
        const prior = this.parseApplicationData(this.selectedApplication?.txtPriorApprovals);
        if (Array.isArray(prior) && prior.length > 0) {
            entries.push(...prior);
        }
        const history = this.parseApplicationData(this.selectedApplication?.txtApprovalHistory);
        if (Array.isArray(history) && history.length > 0) {
            entries.push(...history);
        }
        const data = this.parseApplicationData(this.selectedApplication?.txtApplicationData);
        const nestedHistory = data?.approvalHistory || data?.priorApprovals || data?.templateApprovalHistory;
        if (Array.isArray(nestedHistory) && nestedHistory.length > 0) {
            entries.push(...nestedHistory);
        }

        const submissionEntry = this.getSyntheticSubmissionEntry();
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

    private getSyntheticSubmissionEntry(): any | null {
        if (!this.selectedApplication?.dteCreatedDate && !this.selectedApplication?.serSubmittedBy) {
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
            approverName: this.selectedApplication?.submittedByUserName || 'Initiator',
            userName: this.selectedApplication?.submittedByUserName || 'Initiator',
            userId: this.selectedApplication?.serSubmittedBy,
            approvedBy: this.selectedApplication?.serSubmittedBy,
            approvedDate: this.selectedApplication?.dteCreatedDate,
            remarks: 'Submitted application'
        };
    }

    private entryBelongsToSubmission(entry: any): boolean {
        const stepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim().toLowerCase();
        const role = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        const level = Number(entry?.intApprovalOrder ?? entry?.level);
        const action = String(entry?.action || entry?.status || '').toUpperCase();
        return stepId === 'initiator' || role === 'submission' || action === 'SUBMITTED' || level === 1 && role === 'initiator';
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

        const role = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        if (role) {
            const byRole = steps.find((step) => String(step.name || '').trim().toLowerCase() === role);
            if (byRole) {
                return byRole;
            }
        }

        return steps.find((step) => this.getLegacyDisplayLevelsForStep(step).includes(rawLevel)) || null;
    }

    private getDisplayLevelForStep(step: PipelineStep): number {
        const steps = this.getPipelineSteps().filter((item) => item.type !== 'initiator');
        const index = steps.findIndex((item) => item.id === step.id);
        const order = Number(step.order);
        if (Number.isFinite(order) && order > 0) {
            return order + 1;
        }
        return index >= 0 ? index + 2 : 2;
    }

    private getLegacyDisplayLevelsForStep(step: PipelineStep): number[] {
        const displayLevel = this.getDisplayLevelForStep(step);
        return Array.from(new Set([displayLevel, displayLevel - 1, displayLevel - 2].filter((level) => level >= 0)));
    }

    private getRawHistoryLevel(entry: any): number {
        return Number(entry?.intApprovalOrder ?? entry?.level);
    }

    private isFirstApprovalStep(step: PipelineStep): boolean {
        const steps = this.getPipelineSteps().filter((item) => item.type !== 'initiator');
        return steps.length > 0 && steps[0]?.id === step.id;
    }

    private getApprovedSignatureSlots(step: PipelineStep): any[] {
        return this.getApplicationApprovalHistory()
            .filter((entry) => String(entry?.action || '').toUpperCase() === 'APPROVED')
            .map((entry) => ({
                ...entry,
                serUserId: entry.serUserId ?? entry.userId ?? entry.approvedBy ?? entry.approverId ?? entry.id ?? null,
                txtUserName: entry.txtUserName || entry.userName || entry.approverName || entry.name || '',
                __signatureApproved: true
            }))
            .filter((entry) => this.signatureEntryMatchesStep(entry, step));
    }

    private getInitiatorSignatureSlots(): any[] {
        const userId = Number(this.selectedApplication?.serSubmittedBy);
        return Number.isFinite(userId) && userId > 0
            ? [{ serUserId: userId, txtUserName: 'Initiator', __signatureApproved: true }]
            : [];
    }

    private signatureEntryMatchesStep(entry: any, step: PipelineStep): boolean {
        const entryStepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim();
        const stepId = String(step?.id ?? '').trim();
        if (entryStepId && stepId) {
            return entryStepId === stepId;
        }
        const rawEntryLevel = this.getRawHistoryLevel(entry);
        if (rawEntryLevel === 0) {
            return this.isFirstApprovalStep(step);
        }
        const entryRole = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        const stepName = String(step.name || '').trim().toLowerCase();
        if (entryRole && stepName && entryRole !== stepName) {
            return false;
        }

        const configuredIds = (step.users || [])
            .map((user) => Number(user?.serUserId ?? user?.userId ?? user?.id))
            .filter((userId) => Number.isFinite(userId) && userId > 0);
        const entryUserId = Number(entry?.serUserId ?? entry?.userId ?? entry?.approvedBy ?? entry?.approverId ?? entry?.id);
        if (configuredIds.length > 0) {
            const levelMatches = Number.isFinite(rawEntryLevel) ? this.getLegacyDisplayLevelsForStep(step).includes(rawEntryLevel) : true;
            return Number.isFinite(entryUserId) && configuredIds.includes(entryUserId) && levelMatches;
        }

        return Number.isFinite(rawEntryLevel) && this.getLegacyDisplayLevelsForStep(step).includes(rawEntryLevel);
    }

    private getDynamicSignatureStep(field: TemplateField): PipelineStep | null {
        const targetId = field.signatureTargetId || field.pipelineStepId;
        return this.getPipelineSteps().find((step) => step?.id === targetId) || null;
    }

    private formatStepType(step: PipelineStep): string {
        if (step.dynamicTarget === 'initiator_hod') {
            return 'Self HOD';
        }
        return String(step.type || 'Step').replace(/_/g, ' ');
    }

    private getStepApproverName(step: PipelineStep, history: any, configuredUser: any = null): string {
        if (step.type === 'initiator') {
            return this.selectedApplication?.submittedByUserName || 'Initiator';
        }
        if (history?.approverName || history?.txtUserName || history?.userName) {
            return history.approverName || history.txtUserName || history.userName;
        }
        if (configuredUser) {
            const userId = this.getUserId(configuredUser);
            return configuredUser.txtUserName || configuredUser.userName || configuredUser.name || (Number.isFinite(userId) ? `User ${userId}` : '--');
        }
        const users = Array.isArray(step.users) ? step.users : [];
        if (users.length > 0) {
            return users.map((user) => user.txtUserName || user.userName || user.name || `User ${user.serUserId || user.userId || user.id}`).join(', ');
        }
        if (step.dynamicTarget === 'initiator_hod') {
            return 'Current HOD';
        }
        return step.name || '--';
    }

    private getUserId(value: any): number {
        return Number(value?.serUserId ?? value?.userId ?? value?.approvedBy ?? value?.approverId ?? value?.id);
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

    private normalizeRadioValue(value: any): string {
        return String(value ?? '').trim().toLowerCase();
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

    private getCurrentUserId(): number | null {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            return user?.serUserId || user?.userId || user?.id || null;
        } catch {
            return null;
        }
    }
}
