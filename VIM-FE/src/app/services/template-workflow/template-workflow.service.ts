import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface TemplateCodeConvention {
    pattern: string;
    prefix: string;
    serialLength: number;
}

export interface SavedTemplateDefinition {
    id: string;
    name: string;
    codeConvention: TemplateCodeConvention;
    createdAt: string;
    updatedAt: string;
    payload: any;
    backendForm?: any;
    visibilityUserIds?: number[];
}

export interface TemplateSubmission {
    id: string;
    templateId: string;
    templateName: string;
    code: string;
    values: Record<string, any>;
    pipeline: any[];
    userPipeline: any[];
    submittedAt: string;
    payload: any;
}

@Injectable({
    providedIn: 'root'
})
export class TemplateWorkflowService {
    constructor(private http: HttpClient) { }

    parseCodeConvention(pattern: string): TemplateCodeConvention {
        const value = (pattern || '').trim();
        const match = value.match(/^(.+)-([0]+)$/);
        if (!match) {
            return {
                pattern: value,
                prefix: value || 'FORM',
                serialLength: 4
            };
        }

        return {
            pattern: value,
            prefix: match[1],
            serialLength: match[2].length
        };
    }

    saveTemplate(payload: any, existingId?: string): Observable<SavedTemplateDefinition> {
        const convention = this.parseCodeConvention(payload.codeConvention || 'TPL-0000');
        const formId = this.normalizeBackendId(existingId || payload.id);
        const backendPayload = this.buildBackendFormPayload(payload, convention, formId);
        const endpoint = backendPayload.serFormId ? 'updateCustomForm' : 'addNewCustomForm';

        return this.http.post<any>(urls.API_URL + endpoint, backendPayload).pipe(
            switchMap((response) => {
                const formId = Number(response?.formId || backendPayload.serFormId);
                if (!response || response.status !== 'Success' || !formId) {
                    throw new Error(response?.message || 'Template could not be saved');
                }
                return this.saveTemplateDefinition(payload, convention, formId).pipe(
                    switchMap(() => this.getTemplate(String(formId)))
                );
            }),
            map((template) => {
                if (!template) {
                    throw new Error('Saved template could not be loaded');
                }
                return template;
            })
        );
    }

    getTemplates(userId?: number | null): Observable<SavedTemplateDefinition[]> {
        const resolvedUserId = userId ?? this.getCurrentUserId();
        const query = resolvedUserId != null ? ('?userId=' + encodeURIComponent(String(resolvedUserId))) : '';
        return this.http.get<any[]>(urls.API_URL + 'getAllTemplateDefinitions' + query).pipe(
            map((templates) => (Array.isArray(templates) ? templates : [])
                .map((template) => this.mapTemplateDefinitionToSavedTemplate(template))
                .filter((template): template is SavedTemplateDefinition => !!template))
        );
    }

    getTemplate(id: string, userId?: number | null): Observable<SavedTemplateDefinition | null> {
        const resolvedUserId = userId ?? this.getCurrentUserId();
        const query = resolvedUserId != null
            ? '&userId=' + encodeURIComponent(String(resolvedUserId))
            : '';
        return this.http.get<any>(urls.API_URL + 'getTemplateDefinitionByFormId?formId=' + encodeURIComponent(id) + query).pipe(
            map((definition) => this.mapTemplateDefinitionToSavedTemplate(definition))
        );
    }

    updateTemplateVisibility(template: SavedTemplateDefinition, userIds: number[]): Observable<SavedTemplateDefinition> {
        const sanitizedUserIds = Array.from(new Set(
            (Array.isArray(userIds) ? userIds : [])
                .map((userId) => Number(userId))
                .filter((userId) => Number.isFinite(userId) && userId > 0)
        ));
        const nextPayload = this.applyVisibilityToPayload(template?.payload || {}, sanitizedUserIds, template?.id);
        const definitionPayload = {
            serFormId: Number(template.id),
            txtTemplateName: template.name || nextPayload.name || 'Untitled Template',
            txtCodeConvention: template.codeConvention?.pattern || nextPayload.codeConvention || 'TPL-0000',
            txtTemplatePayload: JSON.stringify(nextPayload),
            serModifiedUser: this.getCurrentUserId()
        };

        return this.http.post<any>(urls.API_URL + 'saveTemplateDefinition', definitionPayload).pipe(
            switchMap((response) => {
                if (!response || response.status !== 'Success') {
                    throw new Error(response?.message || 'Template visibility could not be saved');
                }
                return this.getTemplate(template.id, this.getCurrentUserId());
            }),
            map((savedTemplate) => {
                if (!savedTemplate) {
                    throw new Error('Updated template could not be loaded');
                }
                return savedTemplate;
            })
        );
    }

    getMyTemplateApplications(userId: number, page = 0, pageSize = 10, search = ''): Observable<any> {
        const params = new URLSearchParams();
        params.set('userId', String(userId));
        params.set('page', String(Math.max(0, page)));
        params.set('pageSize', String(Math.max(1, pageSize)));
        if (search.trim()) {
            params.set('search', search.trim());
        }
        return this.http.get<any>(urls.API_URL + 'getMyTemplateApplications?' + params.toString());
    }

    getTemplatePendingApprovals(userId: number, all = false, page = 0, pageSize = 10, search = ''): Observable<any> {
        const params = new URLSearchParams();
        params.set('userId', String(userId || 0));
        params.set('all', String(!!all));
        params.set('page', String(Math.max(0, page)));
        params.set('pageSize', String(Math.max(1, pageSize)));
        if (search.trim()) {
            params.set('search', search.trim());
        }
        return this.http.get<any>(urls.API_URL + 'getTemplatePendingApprovals?' + params.toString());
    }

    getTemplateApprovedApplications(userId: number, page = 0, pageSize = 10, search = ''): Observable<any> {
        const params = new URLSearchParams();
        params.set('userId', String(userId || 0));
        params.set('page', String(Math.max(0, page)));
        params.set('pageSize', String(Math.max(1, pageSize)));
        if (search.trim()) {
            params.set('search', search.trim());
        }
        return this.http.get<any>(urls.API_URL + 'getTemplateApprovedApplications?' + params.toString());
    }

    peekNextTemplateCode(template: SavedTemplateDefinition): Observable<string> {
        return this.http.get<any>(urls.API_URL + 'getNextApplicationCode?formId=' + encodeURIComponent(template.id)).pipe(
            map((response) => String(response?.code || '').trim() || this.formatCode(template.codeConvention, 1))
        );
    }

    submitTemplate(
        template: SavedTemplateDefinition,
        values: Record<string, any>,
        userPipeline: any[] = [],
        codeOverride?: string
    ): Observable<TemplateSubmission> {
        const normalizedValues = this.normalizeApplicationValuesForStorage(values);
        const applicationData = {
            ...normalizedValues,
            templateValues: normalizedValues,
            footerFields: userPipeline,
            templatePayload: template.payload
        };
        const payload = {
            serFormId: Number(template.id),
            txtFormCode: codeOverride || null,
            txtApplicationData: JSON.stringify(applicationData),
            txtStatus: 'PENDING',
            intCurrentApprovalLevel: 1,
            serSubmittedBy: this.getCurrentUserId(),
            blIsActive: true,
            blIsDeleted: false,
            blnStatus: true,
            deferEmail: true
        };

        return this.http.post<any>(urls.API_URL + 'submitTemplateApplication', payload).pipe(
            map((response) => {
                if (!response || response.status !== 'Success') {
                    throw new Error(response?.message || 'Template application could not be submitted');
                }
                const code = response.formCode || codeOverride || '';
                return {
                    id: String(response.applicationId),
                    templateId: template.id,
                    templateName: template.name,
                    code,
                    values: normalizedValues,
                    pipeline: template.payload?.pipeline || [],
                    userPipeline,
                    submittedAt: new Date().toISOString(),
                    payload: template.payload
                };
            })
        );
    }

    updateTemplateApplicationPdf(applicationId: string | number, pdfBlob: Blob, filename: string) {
        const formData = new FormData();
        formData.append('applicationId', String(applicationId));
        formData.append('pdf', pdfBlob, filename || 'template-application.pdf');
        return this.http.post(urls.API_URL + 'updateApplicationPdf?suppressEditNotification=true', formData);
    }

    sendTemplateApplicationEmails(applicationId: string | number) {
        return this.http.post(urls.API_URL + 'sendSubmissionEmails?applicationId=' + encodeURIComponent(String(applicationId)), {});
    }

    sendTemplatePostApprovalEmails(
        applicationId: string | number,
        initiatorPdfBlob?: Blob,
        approverPdfBlob?: Blob,
        filename = 'template-application.pdf'
    ) {
        if (initiatorPdfBlob || approverPdfBlob) {
            const formData = new FormData();
            formData.append('applicationId', String(applicationId));
            if (initiatorPdfBlob) {
                formData.append('initiatorPdf', initiatorPdfBlob, filename);
            }
            if (approverPdfBlob) {
                formData.append('approverPdf', approverPdfBlob, filename);
            }
            return this.http.post<any>(urls.API_URL + 'sendTemplatePostApprovalEmailsWithPdfs', formData);
        }
        return this.http.post<any>(urls.API_URL + 'sendTemplatePostApprovalEmails?applicationId=' + encodeURIComponent(String(applicationId)), {});
    }

    getApplication(applicationId: string | number): Observable<any> {
        return this.http.get<any>(urls.API_URL + 'getTemplateApplicationById?applicationId=' + encodeURIComponent(String(applicationId)));
    }

    downloadTemplateApplicationPdf(applicationId: string | number): Observable<Blob> {
        return this.http.get(urls.API_URL + 'downloadTemplateApplicationPdf?applicationId=' + encodeURIComponent(String(applicationId)), {
            responseType: 'blob'
        });
    }

    updateTemplateApplication(application: any, values: Record<string, any>, templatePayload: any, userPipeline: any[] = []) {
        const existingData = this.parseApplicationData(application?.txtApplicationData);
        const normalizedValues = this.normalizeApplicationValuesForStorage(values);
        const nextData = {
            ...existingData,
            ...normalizedValues,
            templateValues: {
                ...(existingData.templateValues || {}),
                ...normalizedValues
            },
            footerFields: userPipeline.length > 0 ? userPipeline : (existingData.footerFields || []),
            templatePayload
        };

        const payload = {
            serApplicationId: application.serApplicationId,
            serFormId: application.serFormId,
            txtFormCode: application.txtFormCode,
            txtApplicationData: JSON.stringify(nextData),
            txtStatus: application.txtStatus,
            intCurrentApprovalLevel: application.intCurrentApprovalLevel,
            serSubmittedBy: application.serSubmittedBy,
            blIsActive: application.blIsActive ?? true,
            blIsDeleted: application.blIsDeleted ?? false,
            blnStatus: application.blnStatus ?? true,
            deferEmail: true
        };

        return this.http.post<any>(urls.API_URL + 'updateApplication', payload).pipe(
            map((response) => {
                if (!response || response.status !== 'Success') {
                    throw new Error(response?.message || 'Template application could not be updated');
                }
                return response;
            })
        );
    }

    approveTemplateApplication(applicationId: string | number, remarks: string, approverUserId?: number | null, deferEmail = false) {
        const body: any = {
            applicationId: Number(applicationId),
            remarks: remarks || '',
            deferEmail
        };
        if (approverUserId != null) {
            body.approverUserId = approverUserId;
            body.userId = approverUserId;
        } else {
            body.userId = this.getCurrentUserId();
        }
        return this.http.post<any>(urls.API_URL + 'approveTemplateApplication', body);
    }

    rejectTemplateApplication(applicationId: string | number, remarks: string) {
        return this.http.post<any>(urls.API_URL + 'rejectTemplateApplication', {
            applicationId: Number(applicationId),
            userId: this.getCurrentUserId(),
            remarks: remarks || ''
        });
    }

    sendBackTemplateApplication(applicationId: string | number, remarks: string) {
        return this.http.post<any>(urls.API_URL + 'sendBackTemplateApplication', {
            applicationId: Number(applicationId),
            remarks,
            userId: this.getCurrentUserId()
        });
    }

    sendBackTemplateApplicationToInitiator(applicationId: string | number, remarks: string) {
        return this.http.post<any>(urls.API_URL + 'sendBackTemplateApplicationToInitiator', {
            applicationId: Number(applicationId),
            remarks,
            userId: this.getCurrentUserId()
        });
    }

    resubmitTemplateApplicationFromInitiator(applicationId: string | number, remarks: string, userId?: number | null) {
        const body: any = {
            applicationId: Number(applicationId),
            remarks: remarks || ''
        };
        if (userId != null) {
            body.userId = userId;
        }
        return this.http.post<any>(urls.API_URL + 'resubmitTemplateApplicationFromInitiator', body);
    }

    requestTemplateOpinion(applicationId: string | number, opinionUserId: number, remarks: string) {
        return this.http.post<any>(urls.API_URL + 'requestTemplateApplicationOpinion', {
            applicationId: Number(applicationId),
            opinionUserId: Number(opinionUserId),
            userId: this.getCurrentUserId(),
            remarks: remarks || ''
        });
    }

    submitTemplateOpinion(applicationId: string | number, action: 'approve' | 'reject', remarks: string) {
        return this.http.post<any>(urls.API_URL + 'submitTemplateApplicationOpinion', {
            applicationId: Number(applicationId),
            action,
            userId: this.getCurrentUserId(),
            remarks: remarks || ''
        });
    }

    sendTemplateTestEmailPdf(recipients: string[], subject: string, bodyHtml: string, pdfBlob: Blob, filename: string) {
        const formData = new FormData();
        formData.append('recipients', JSON.stringify(recipients || []));
        formData.append('subject', subject || 'Template test email');
        formData.append('bodyHtml', bodyHtml || '');
        formData.append('pdf', pdfBlob, filename || 'template-form.pdf');
        return this.http.post(urls.API_URL + 'sendTemplateTestEmailPdf', formData);
    }

    private saveTemplateDefinition(payload: any, convention: TemplateCodeConvention, formId: number): Observable<any> {
        const templatePayload = {
            ...payload,
            id: String(formId),
            codeConvention: convention.pattern,
            codePrefix: convention.prefix,
            serialLength: convention.serialLength
        };
        const visibilityUserIds = this.normalizeVisibilityUserIds(payload.visibleUserIds ?? payload.visibilityUserIds);
        const normalizedTemplatePayload = this.applyVisibilityToPayload(templatePayload, visibilityUserIds, formId);

        const definitionPayload = {
            serFormId: formId,
            txtTemplateName: payload.name || 'Untitled Template',
            txtCodeConvention: convention.pattern,
            txtTemplatePayload: JSON.stringify(normalizedTemplatePayload),
            serModifiedUser: this.getCurrentUserId()
        };

        return this.http.post<any>(urls.API_URL + 'saveTemplateDefinition', definitionPayload).pipe(
            map((response) => {
                if (!response || response.status !== 'Success') {
                    throw new Error(response?.message || 'Template definition could not be saved');
                }
                return response;
            })
        );
    }

    private buildBackendFormPayload(payload: any, convention: TemplateCodeConvention, existingId?: string): any {
        const fields = Array.isArray(payload.fields) ? payload.fields : [];
        return {
            ...(existingId ? { serFormId: Number(existingId) } : {}),
            txtFormName: payload.name || 'Untitled Template',
            txtFormDescription: 'template-builder',
            txtConventionPrefix: convention.prefix,
            txtFormCode: convention.pattern,
            blIsActive: true,
            blIsDeleted: false,
            blnStatus: true,
            txtUserIds: null,
            cfgTblCustomFormFields: fields.map((field: any, index: number) => ({
                    txtFieldLabel: field.label,
                    txtFieldType: field.type,
                    txtPlaceholder: field.placeholder || '',
                    blIsRequired: !!field.required,
                    intFieldOrder: index,
                    blIsActive: true,
                    blIsDeleted: false,
                    txtFieldOptions: null
                })),
            cfgTblCustomFormApprovalPipelines: this.toDepartmentPipeline(payload.pipeline || []),
            txtApprovalPipeline: JSON.stringify(this.toBackendApprovalPipeline(payload.pipeline || []))
        };
    }

    private mapTemplateDefinitionToSavedTemplate(definition: any): SavedTemplateDefinition | null {
        if (!definition?.txtTemplatePayload || !definition?.serFormId) {
            return null;
        }

        let payload: any;
        try {
            payload = JSON.parse(definition.txtTemplatePayload);
        } catch {
            return null;
        }

        const visibilityUserIds = this.extractVisibilityUserIds(payload);
        const convention = this.parseCodeConvention(payload.codeConvention || definition.txtCodeConvention || 'TPL-0000');
        payload = {
            ...payload,
            id: String(definition.serFormId),
            name: payload.name || definition.txtTemplateName,
            codeConvention: convention.pattern,
            codePrefix: convention.prefix,
            serialLength: convention.serialLength,
            visibleUserIds: visibilityUserIds,
            visibilityUserIds,
            backendDefinition: definition
        };

        return {
            id: String(definition.serFormId),
            name: payload.name || 'Untitled Template',
            codeConvention: convention,
            createdAt: definition.dteCreatedDate || new Date().toISOString(),
            updatedAt: definition.dteModifiedDate || definition.dteCreatedDate || new Date().toISOString(),
            payload,
            backendForm: definition,
            visibilityUserIds
        };
    }

    private normalizeBackendId(value: any): string | undefined {
        if (value === null || value === undefined || value === '') {
            return undefined;
        }
        const numeric = Number(value);
        return Number.isFinite(numeric) && numeric > 0 ? String(numeric) : undefined;
    }

    private toDepartmentPipeline(pipeline: any[]): any[] {
        return this.toBackendApprovalPipeline(pipeline)
            .filter((step) => step.type === 'department' && step.serDepartmentId)
            .map((step) => ({
                type: 'department',
                serDepartmentId: step.serDepartmentId,
                intApprovalOrder: step.intApprovalOrder,
                hrTblDepartment: step.hrTblDepartment
            }));
    }

    private toBackendApprovalPipeline(pipeline: any[]): any[] {
        const steps = (Array.isArray(pipeline) ? pipeline : [])
            .filter((step) => step && step.type !== 'initiator');
        const result: any[] = [];

        steps.forEach((step) => {
            const base = {
                id: step.id,
                type: step.type,
                approvalMode: step.approvalMode || 'OR',
                fieldPermissions: step.fieldPermissions || [],
                fieldPermissionsConfigured: step.fieldPermissionsConfigured === true,
                intApprovalOrder: result.length + 1,
                stageName: step.name
            };

            if (step.type === 'department') {
                result.push({
                    ...base,
                    type: 'department',
                    serDepartmentId: step.dynamicTarget === 'initiator_hod' ? null : (step.serDepartmentId ?? step.hrTblDepartment?.serDepartmentId ?? null),
                    departmentId: step.dynamicTarget === 'initiator_hod' ? null : (step.serDepartmentId ?? step.hrTblDepartment?.serDepartmentId ?? null),
                    txtDepartmentName: step.dynamicTarget === 'initiator_hod'
                        ? 'User Dept HOD'
                        : (step.hrTblDepartment?.txtDepartmentName || step.name || null),
                    departmentName: step.dynamicTarget === 'initiator_hod'
                        ? 'User Dept HOD'
                        : (step.hrTblDepartment?.txtDepartmentName || step.name || null),
                    dynamicTarget: step.dynamicTarget,
                    hrTblDepartment: step.hrTblDepartment
                });
                return;
            }

            if (step.type === 'individual') {
                const users = Array.isArray(step.users) ? step.users : [];
                users.forEach((user: any) => {
                    const userId = user?.serUserId ?? user?.userId ?? user?.id;
                    if (!userId) {
                        return;
                    }
                    result.push({
                        ...base,
                        type: 'individual',
                        intApprovalOrder: result.length + 1,
                        serUserId: Number(userId),
                        userId: Number(userId),
                        txtUserName: user.txtUserName || user.userName || user.name || null,
                        hrTblUser: user
                    });
                });
                if (step.dynamicTarget === 'initiator' || (Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator'))) {
                    result.push({
                        ...base,
                        type: 'individual',
                        intApprovalOrder: result.length + 1,
                        dynamicTarget: 'initiator',
                        txtUserName: 'Initiator'
                    });
                }
                return;
            }

            if (step.type === 'role') {
                result.push({
                    ...base,
                    type: 'role',
                    serRoleId: step.serRoleId ?? step.cfgTblRole?.serRoleId ?? null,
                    roleName: step.cfgTblRole?.txtRoleName || step.name || null,
                    cfgTblRole: step.cfgTblRole
                });
            }
        });

        return result.map((step, index) => ({
            ...step,
            intApprovalOrder: index + 1
        }));
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

    private normalizeApplicationValuesForStorage(values: Record<string, any>): Record<string, any> {
        const normalized: Record<string, any> = {};
        Object.keys(values || {}).forEach((key) => {
            normalized[key] = this.normalizeValueForStorage(values[key]);
        });
        return normalized;
    }

    private normalizeValueForStorage(value: any): any {
        if (Array.isArray(value)) {
            return value.map((item) => this.normalizeValueForStorage(item));
        }
        if (!value || typeof value !== 'object') {
            return value;
        }
        if (this.looksLikeAttachmentPayload(value)) {
            return this.normalizeAttachmentPayloadForStorage(value);
        }
        const normalized: Record<string, any> = {};
        Object.keys(value).forEach((key) => {
            normalized[key] = this.normalizeValueForStorage(value[key]);
        });
        return normalized;
    }

    private looksLikeAttachmentPayload(value: any): boolean {
        return !!value && typeof value === 'object'
            && ('fileName' in value || 'name' in value || 'originalName' in value)
            && ('base64' in value || 'dataUrl' in value || 'mimeType' in value || 'type' in value);
    }

    private normalizeAttachmentPayloadForStorage(value: any): any {
        const fileName = String(value?.fileName || value?.name || value?.originalName || '').trim();
        const mimeType = String(value?.mimeType || value?.type || 'application/octet-stream').trim();
        const dataUrl = String(value?.dataUrl || '').trim();
        const base64 = String(value?.base64 || (dataUrl.includes(',') ? dataUrl.split(',', 2)[1] : '')).trim();
        return {
            fileName,
            mimeType,
            base64
        };
    }

    private formatCode(convention: TemplateCodeConvention, serial: number): string {
        return `${convention.prefix}-${String(serial).padStart(convention.serialLength, '0')}`;
    }

    private extractVisibilityUserIds(payload: any): number[] {
        return this.normalizeVisibilityUserIds(
            payload?.visibleUserIds ??
            payload?.visibilityUserIds ??
            payload?.selectedUsers
        );
    }

    private normalizeVisibilityUserIds(value: any): number[] {
        if (!Array.isArray(value)) {
            return [];
        }
        return Array.from(new Set(
            value
                .map((userId) => Number(userId))
                .filter((userId) => Number.isFinite(userId) && userId > 0)
        ));
    }

    private applyVisibilityToPayload(payload: any, userIds: number[], formId?: string | number): any {
        return {
            ...payload,
            ...(formId != null ? { id: String(formId) } : {}),
            visibleUserIds: [...userIds],
            visibilityUserIds: [...userIds],
            selectedUsers: [...userIds]
        };
    }
}
