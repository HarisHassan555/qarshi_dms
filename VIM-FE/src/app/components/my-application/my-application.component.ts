import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { urls } from 'src/app/utils/urls';
import { stripEditorTableChromeFromHtml } from 'src/app/utils/word-editor-table.util';
import {
    resolveDocumentHeaderAddress,
    resolveDocumentHeaderBrandTitle,
    resolveDocumentHeaderLogoPath,
} from 'src/app/utils/document-header.util';

type TemplateFieldType =
    'document_header' | 'document_header_qu' | 'document_header_qf' | 'document_header_qri' | 'document_header_qb'
    | 'footer' | 'individual_pipeline_footer' | 'application_code' | 'pipeline_signature' | 'dynamic_signature'
    | 'dynamic_approver_name' | 'dynamic_approval_timestamp'
    | 'dynamic_approver_department' | 'dynamic_approver_designation'
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
        fontFamily: string;
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
    fieldPermissions?: any[];
    fieldPermissionsConfigured?: boolean;
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

interface AttachmentPayload {
    fileName: string;
    mimeType: string;
    dataUrl: string;
    base64: string;
}

@Component({
    selector: 'app-my-application',
    templateUrl: './my-application.component.html',
    styleUrls: ['./my-application.component.css', '../template-fill/template-fill.component.css']
})
export class MyApplicationComponent implements OnInit, OnDestroy {
    private static readonly INDIVIDUAL_FOOTER_MIN_HEIGHT = 136;
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

    exportApplicationsCsv(): void {
        const userId = this.getCurrentUserId();
        if (!userId) {
            this.notificationService.showMessage('Current user could not be resolved.', 'danger');
            return;
        }
        const exportSize = Math.max(this.totalApplications || 0, this.pageSize, 1000);
        this.templateWorkflowService.getMyTemplateApplications(userId, 0, exportSize, this.search).subscribe({
            next: (response: any) => {
                const rows = (Array.isArray(response?.items) ? response.items : [])
                    .map((application: any) => this.toShallowApplication(application));
                if (!rows.length) {
                    this.notificationService.showMessage('No applications available to export.', 'warning');
                    return;
                }
                const headers = ['Application Code', 'Template', 'Status', 'Approval Level', 'Submitted Date'];
                const csvRows = [
                    headers.join(','),
                    ...rows.map((application: MyTemplateApplication) => ([
                        application.txtFormCode || '',
                        application.templateName || '',
                        application.txtStatus || '',
                        String(this.getApplicationDisplayLevel(application)),
                        application.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleString() : ''
                    ].map((value) => this.escapeCsvValue(value)).join(',')))
                ];
                this.downloadBlob(
                    new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' }),
                    `my-applications_${new Date().toISOString().split('T')[0]}.csv`
                );
            },
            error: (error) => {
                this.notificationService.showMessage(
                    'Applications CSV could not be exported: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
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
        this.selectedTemplate = this.buildSelectedTemplate(application);
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
        if (this.canEditApplication(application)) {
            this.router.navigate(['/template-fill', application.serFormId], {
                queryParams: { applicationId: application.serApplicationId }
            });
            return;
        }
        this.router.navigate(['/my-application', application.serApplicationId]);
    }

    editApplication(application: MyTemplateApplication): void {
        if (!this.canEditApplication(application)) {
            this.notificationService.showMessage('This application is not available for editing.', 'danger');
            return;
        }
        this.router.navigate(['/template-fill', application.serFormId], {
            queryParams: { applicationId: application.serApplicationId }
        });
    }

    backToList(): void {
        this.router.navigate(['/my-application']);
        this.selectApplication(null);
    }

    downloadApplicationPdf(application: MyTemplateApplication | null = this.selectedApplication): void {
        if (!application?.serApplicationId) {
            this.notificationService.showMessage('Invalid application ID', 'danger');
            return;
        }
        this.templateWorkflowService.downloadTemplateApplicationPdf(application.serApplicationId).subscribe({
            next: (blob) => {
                const filename = `${application.txtFormCode || 'template-application'}.pdf`;
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
        const currentLevel = Number(this.selectedApplication.intCurrentApprovalLevel || 1);
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

    isOpinionPendingApplication(): boolean {
        return String(this.selectedApplication?.txtStatus || '').toUpperCase() === 'OPINION_PENDING';
    }

    getOpinionRequesterName(): string {
        const request = this.getTemplateOpinionRequest();
        const explicitName = String(
            request?.requestedByName
            || request?.approverName
            || request?.requestedByUserName
            || ''
        ).trim();
        if (explicitName) {
            return explicitName;
        }

        const currentStep = this.getCurrentWorkflowStep();
        if (currentStep) {
            const stepApproverName = this.getStepApproverName(currentStep, null);
            if (stepApproverName && stepApproverName !== '--') {
                return stepApproverName;
            }
        }

        return 'Current approver';
    }

    getOpinionPendingWithName(): string {
        const request = this.getTemplateOpinionRequest();
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
            const currentStep = this.getCurrentWorkflowStep();
            const matchedUser = this.getConfiguredStepUsers(currentStep || {} as PipelineStep)
                .find((user) => this.getUserId(user) === requestedFrom);
            const matchedName = String(
                matchedUser?.txtUserName
                || matchedUser?.userName
                || matchedUser?.name
                || ''
            ).trim();
            if (matchedName) {
                return matchedName;
            }
            return `User ${requestedFrom}`;
        }

        const currentStep = this.getCurrentWorkflowStep();
        if (currentStep) {
            const configuredUsers = this.getConfiguredStepUsers(currentStep)
                .map((user) => String(user?.txtUserName || user?.userName || user?.name || '').trim())
                .filter((name, index, list) => !!name && list.indexOf(name) === index);
            if (configuredUsers.length > 0) {
                return configuredUsers.join(', ');
            }
        }

        return 'Selected user';
    }

    getTileClass(tile: ProgressTile): string {
        return `is-${tile.status.toLowerCase()}`;
    }

    getApplicationDisplayLevel(application: MyTemplateApplication): number {
        const currentLevel = Number(application?.intCurrentApprovalLevel);
        return Number.isFinite(currentLevel) && currentLevel > 0 ? currentLevel : 1;
    }

    canEditApplication(application: MyTemplateApplication | null): boolean {
        const currentUserId = this.getCurrentUserId();
        const submittedBy = Number(application?.serSubmittedBy || 0);
        const status = String(application?.txtStatus || '').toUpperCase();
        const currentLevel = Number(application?.intCurrentApprovalLevel);
        return currentUserId != null
            && submittedBy === currentUserId
            && status === 'PENDING'
            && Number.isFinite(currentLevel)
            && currentLevel < 0;
    }

    getRadioOptionPlacements(field: TemplateField): RadioOptionPlacement[] {
        return Array.isArray(field.optionPlacements) ? field.optionPlacements : [];
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
            return this.selectedApplication?.txtFormCode || '';
        }
        if (field.type === 'attachment') {
            return this.getAttachmentDisplayText(field);
        }
        if (field.type === 'dynamic_approver_name'
            || field.type === 'dynamic_approval_timestamp'
            || field.type === 'dynamic_approver_department'
            || field.type === 'dynamic_approver_designation') {
            return this.getDynamicApprovalDisplayText(field);
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
        return this.sanitizer.bypassSecurityTrustHtml(this.normalizeWordEditorHtmlForDisplay(String(this.values[field.id] || '')));
    }

    getIndividualFooterSections(): any[] {
        const data = this.parseApplicationData(this.selectedApplication?.txtApplicationData);
        return Array.isArray(data?.footerFields) ? data.footerFields : [];
    }

    getIndividualFooterColSpan(section: any): number {
        const users = Array.isArray(section?.users) ? section.users : [];
        return Math.max(users.length, 1);
    }

    getIndividualFooterSlots(section: any): any[] {
        const users = Array.isArray(section?.users) ? section.users : [];
        return users.length > 0 ? users : [null];
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
        return (this.selectedTemplate?.fields || []).filter((field: TemplateField) => this.isAttachmentField(field));
    }

    getAttachmentPayloads(field: TemplateField): AttachmentPayload[] {
        return this.normalizeExistingAttachmentPayloads(this.values[field.id]);
    }

    getAttachmentDisplayText(field: TemplateField): string {
        const attachments = this.getAttachmentPayloads(field);
        return attachments.length > 0 ? attachments.map((attachment) => attachment.fileName).join(', ') : '';
    }

    viewAttachment(attachment: AttachmentPayload): void {
        const dataUrl = String(attachment?.dataUrl || '').trim();
        const base64 = String(attachment?.base64 || '').trim();
        const mimeType = String(attachment?.mimeType || 'application/octet-stream').trim();
        const resolvedUrl = dataUrl || (base64 ? `data:${mimeType};base64,${base64}` : '');
        if (!resolvedUrl) {
            this.notificationService.showMessage('Attachment content is unavailable.', 'warning');
            return;
        }
        window.open(resolvedUrl, '_blank', 'noopener');
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
        return Math.max(height, MyApplicationComponent.INDIVIDUAL_FOOTER_MIN_HEIGHT);
    }

    isDynamicSignatureField(field: TemplateField): boolean {
        return field.type === 'dynamic_signature' || field.type === 'pipeline_signature';
    }

    isDynamicApprovalDataField(field: TemplateField): boolean {
        return this.isDynamicSignatureField(field)
            || field.type === 'dynamic_approver_name'
            || field.type === 'dynamic_approval_timestamp'
            || field.type === 'dynamic_approver_department'
            || field.type === 'dynamic_approver_designation';
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
        const configuredSlots = this.getConfiguredSignatureSlots(step);
        if (step.approvalMode === 'AND') {
            if (approvedSlots.length > 0 && !this.signatureSlotsHaveConcreteUsers(configuredSlots)) {
                return approvedSlots;
            }
            return this.mergeSignatureSlots(configuredSlots, approvedSlots);
        }

        return approvedSlots.length > 0 ? approvedSlots : configuredSlots;
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

        return '';
    }

    getDynamicSignatureLabel(slot: any): string {
        return slot?.txtUserName || slot?.userName || slot?.approverName || slot?.name || slot?.label || 'Approver';
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

        return `<div class="word-editor-value">${qlEditor.outerHTML}</div>`;
    }

    private getPipelineSteps(): PipelineStep[] {
        const pipeline = Array.isArray(this.selectedTemplate?.pipeline) ? this.selectedTemplate.pipeline : [];
        if (pipeline.some((step: PipelineStep) => step?.type === 'initiator')) {
            return pipeline;
        }
        return [{ id: 'initiator', name: 'Initiator', type: 'initiator' }, ...pipeline];
    }

    private getCurrentWorkflowStep(): PipelineStep | null {
        const currentLevel = Number(this.selectedApplication?.intCurrentApprovalLevel || 1);
        if (!Number.isFinite(currentLevel) || currentLevel <= 1) {
            return null;
        }
        return this.getPipelineSteps()
            .filter((step) => step.type !== 'initiator')
            .find((step) => this.getDisplayLevelForStep(step) === currentLevel) || null;
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
        const currentStepLevel = currentLevel > 0 ? currentLevel : 1;
        const isInitiatorReopened = this.hasLatestSendBackToInitiatorAction(this.selectedApplication)
            || (applicationStatus === 'PENDING' && Number.isFinite(currentLevel) && currentLevel < 0);

        if (isInitiator) {
            tileStatus = isInitiatorReopened ? 'PENDING' : 'APPROVED';
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
        const sortedSections = [...this.getIndividualFooterSections()]
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
        const status = String(this.selectedApplication?.txtStatus || '').toUpperCase();
        if (status === 'APPROVED' || status === 'COMPLETED') {
            return true;
        }
        const currentLevel = Number(this.selectedApplication?.intCurrentApprovalLevel);
        return Number.isFinite(currentLevel) && currentLevel > expectedLevel;
    }

    private getApprovalEntriesForStep(step: PipelineStep, stepLevel: number): any[] {
        const stepId = String(step.id || '').trim();
        return this.getWorkflowApprovalHistory().filter((entry) => {
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
        if (this.hasLatestSendBackToInitiatorAction(this.selectedApplication)) {
            return null;
        }
        return [...this.getWorkflowApprovalHistory()]
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

    private getSyntheticSubmissionEntry(existingEntries: any[] = []): any | null {
        if (!this.selectedApplication?.dteCreatedDate && !this.selectedApplication?.serSubmittedBy) {
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
            approverName: this.selectedApplication?.submittedByUserName || 'Initiator',
            userName: this.selectedApplication?.submittedByUserName || 'Initiator',
            userId: this.selectedApplication?.serSubmittedBy,
            approvedBy: this.selectedApplication?.serSubmittedBy,
            approvedDate: this.selectedApplication?.dteCreatedDate,
            txtDepartmentName: this.resolveDepartmentName(this.parseApplicationData(this.selectedApplication?.txtApplicationData)),
            txtDesignation: this.resolveDesignation(this.parseApplicationData(this.selectedApplication?.txtApplicationData)),
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
        return Array.from(new Set([displayLevel, displayLevel - 1, displayLevel - 2].filter((level) => level >= 0)));
    }

    private getRawHistoryLevel(entry: any): number {
        return Number(entry?.intApprovalOrder ?? entry?.level);
    }

    private isFirstApprovalStep(step: PipelineStep): boolean {
        const steps = this.getPipelineSteps().filter((item) => item.type !== 'initiator');
        return steps.length > 0 && steps[0]?.id === step.id;
    }

    private getConfiguredSignatureSlots(step: PipelineStep): any[] {
        const users = this.getConfiguredStepUsers(step);
        if (users.length > 0) {
            return users;
        }

        if (step.dynamicTarget === 'initiator' || (Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator'))) {
            const initiatorName = String(this.selectedApplication?.submittedByUserName || '').trim();
            return [{ label: initiatorName || 'Initiator', txtUserName: initiatorName || '' }];
        }

        const resolvedName = this.getResolvedDynamicSlotLabel(step);
        return [{ label: resolvedName || step.name || 'Approver' }];
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

    private getInitiatorSignatureSlots(): any[] {
        const userId = Number(this.selectedApplication?.serSubmittedBy);
        const initiatorName = String(this.selectedApplication?.submittedByUserName || '').trim();
        const initiatorHistory = this.getInitiatorHistoryEntry();
        if (this.hasLatestSendBackToInitiatorAction(this.selectedApplication)) {
            return [{
                label: initiatorName || 'Initiator',
                txtUserName: initiatorName || '',
                txtDepartmentName: this.resolveDepartmentName(initiatorHistory),
                txtDesignation: this.resolveDesignation(initiatorHistory)
            }];
        }
        return Number.isFinite(userId) && userId > 0
            ? [{
                serUserId: userId,
                txtUserName: initiatorName || 'Initiator',
                txtDepartmentName: this.resolveDepartmentName(initiatorHistory),
                txtDesignation: this.resolveDesignation(initiatorHistory),
                approvedDate: this.selectedApplication?.dteCreatedDate || null,
                approvedAt: this.selectedApplication?.dteCreatedDate || null,
                dteCreatedDate: this.selectedApplication?.dteCreatedDate || null,
                __signatureApproved: true
            }]
            : [{
                label: initiatorName || 'Initiator',
                txtUserName: initiatorName || '',
                txtDepartmentName: this.resolveDepartmentName(initiatorHistory),
                txtDesignation: this.resolveDesignation(initiatorHistory)
            }];
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

    private getResolvedDynamicSlotLabel(step: PipelineStep): string {
        const dynamicStep = step as any;
        return String(
            dynamicStep?.txtUserName
            || dynamicStep?.userName
            || dynamicStep?.approverName
            || dynamicStep?.currentApproverName
            || dynamicStep?.txtCurrentApproverName
            || dynamicStep?.currentApproverUserName
            || step?.name
            || ''
        ).trim();
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

    private signatureEntryMatchesStep(entry: any, step: PipelineStep): boolean {
        if (!entry) {
            return false;
        }

        const action = String(entry?.action || entry?.status || '').toUpperCase();
        if (action && action !== 'APPROVED') {
            return false;
        }

        const entryStepId = String(entry?.stepId ?? entry?.pipelineStepId ?? entry?.signatureTargetId ?? '').trim();
        const stepId = String(step?.id ?? '').trim();
        if (entryStepId && stepId) {
            return entryStepId === stepId;
        }

        const configuredUserIds = this.getConfiguredStepUsers(step)
            .map((user) => Number(user?.serUserId ?? user?.userId ?? user?.id))
            .filter((userId) => Number.isFinite(userId) && userId > 0);
        const entryUserId = Number(entry?.serUserId ?? entry?.userId ?? entry?.approvedBy ?? entry?.approverId ?? entry?.id);
        const entryLevel = Number(entry?.intApprovalOrder ?? entry?.level);
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

        const entryRole = String(entry?.role || entry?.stageName || entry?.stepName || '').trim().toLowerCase();
        const stepName = String(step.name || '').trim().toLowerCase();
        return !!entryRole && !!stepName && (entryRole === stepName || entryRole.includes(stepName) || stepName.includes(entryRole));
    }

    private signatureEntryLevelMatchesStep(entryLevel: number, step: PipelineStep): boolean {
        const expectedLevel = this.getSignatureStepHistoryLevel(step);
        return Number.isFinite(expectedLevel) && [expectedLevel, expectedLevel - 1, expectedLevel - 2].includes(entryLevel);
    }

    private getSignatureStepHistoryLevel(step: PipelineStep): number {
        const steps = Array.isArray(this.selectedTemplate?.pipeline) ? this.selectedTemplate.pipeline as PipelineStep[] : [];
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

        if (step.dynamicTarget === 'initiator_hod') {
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
        const users = this.getConfiguredStepUsers(step);
        if (users.length > 0) {
            return users.map((user) => user.txtUserName || user.userName || user.name || `User ${user.serUserId || user.userId || user.id}`).join(', ');
        }
        if (step.dynamicTarget === 'initiator_hod') {
            return 'Current HOD';
        }
        return step.name || '--';
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

    private getTemplateOpinionRequest(): any {
        const data = this.parseApplicationData(this.selectedApplication?.txtApplicationData);
        return data?.templateOpinionRequest || {};
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

    private getApplicationTemplatePayload(application: MyTemplateApplication | null): any | null {
        const data = this.parseApplicationData(application?.txtApplicationData);
        const payload = data?.templatePayload;
        return payload && typeof payload === 'object' ? payload : null;
    }

    private buildSelectedTemplate(application: MyTemplateApplication | null): any | null {
        const liveTemplate = application?.template?.payload;
        const savedTemplate = this.getApplicationTemplatePayload(application);
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
            pipeline: this.resolveWorkflowPipeline(application, savedTemplate, liveTemplate),
            page: savedTemplate.page || liveTemplate.page,
            html: typeof savedTemplate.html === 'string' && savedTemplate.html.length > 0 ? savedTemplate.html : liveTemplate.html
        };
    }

    private resolveWorkflowPipeline(application: MyTemplateApplication | null, savedTemplate: any, liveTemplate: any): PipelineStep[] {
        const footerPipeline = this.buildFooterWorkflowPipeline(application, savedTemplate, liveTemplate);
        if (footerPipeline.length > 0) {
            return footerPipeline;
        }
        if (Array.isArray(savedTemplate?.pipeline) && savedTemplate.pipeline.length > 0) {
            return savedTemplate.pipeline;
        }
        return Array.isArray(liveTemplate?.pipeline) ? liveTemplate.pipeline : [];
    }

    private buildFooterWorkflowPipeline(application: MyTemplateApplication | null, savedTemplate: any, liveTemplate: any): PipelineStep[] {
        const data = this.parseApplicationData(application?.txtApplicationData);
        const footerFields = Array.isArray(data?.footerFields) ? data.footerFields : [];
        const templateFields = (savedTemplate?.fields || liveTemplate?.fields || []) as TemplateField[];
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
                right: 'edit'
            }));

        const steps: PipelineStep[] = [{
            id: 'initiator',
            name: 'Initiator',
            type: 'initiator',
            approvalMode: 'OR',
            order: 1,
            users: []
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
                        fieldPermissions: attachmentPermissions as any,
                        fieldPermissionsConfigured: true
                    } as any);
                });
            });

        return steps;
    }

    private getCurrentUserId(): number | null {
        try {
            const user = JSON.parse(localStorage.getItem('user') || 'null');
            return user?.serUserId || user?.userId || user?.id || null;
        } catch {
            return null;
        }
    }

    private escapeCsvValue(value: any): string {
        const normalized = String(value ?? '');
        return `"${normalized.replace(/"/g, '""')}"`;
    }

    private downloadBlob(blob: Blob, filename: string): void {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(objectUrl);
    }
}
