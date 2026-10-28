import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface TemplateCodeConvention {
    pattern: string;
    prefix: string;
    serialLength: number;
}

export interface PdfSignatureMissingItem {
    userId: number;
    userName: string;
    signaturePath?: string;
}

export interface PdfSignatureCheckResult {
    allPresent: boolean;
    missing: PdfSignatureMissingItem[];
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
    isActive?: boolean;
    isDraft?: boolean;
    /** Template list: user's direct visibility */
    hasOwnAccess?: boolean;
    /** Template list: visible via active delegation */
    hasDelegatedAccess?: boolean;
    listingAccess?: 'OWN' | 'DELEGATION' | 'BOTH' | 'NONE';
    delegationOnBehalfOf?: Array<{ serOnBehalfUserId: number; onBehalfUserName: string }>;
}

export interface TemplateSubmitAccessContext {
    fillAccessMode?: 'self' | 'on_behalf';
    onBehalfUserId?: number | null;
}

export interface TemplateDefinitionSummaryResponse {
    items: SavedTemplateDefinition[];
    total: number;
    page: number;
    pageSize: number;
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

    saveTemplate(payload: any, existingId?: string, options?: { asDraft?: boolean }): Observable<SavedTemplateDefinition> {
        const asDraft = options?.asDraft === true;
        const convention = this.parseCodeConvention(payload.codeConvention || 'TPL-0000');
        const formId = this.normalizeBackendId(existingId || payload.id);
        const normalizedPayload = {
            ...payload,
            isDraft: asDraft,
            isActive: asDraft ? false : payload.isActive !== false
        };
        const backendPayload = this.buildBackendFormPayload(normalizedPayload, convention, formId, asDraft);
        const endpoint = backendPayload.serFormId ? 'updateCustomForm' : 'addNewCustomForm';

        return this.http.post<any>(urls.API_URL + endpoint, backendPayload).pipe(
            switchMap((response) => {
                const formId = Number(response?.formId || backendPayload.serFormId);
                if (!response || response.status !== 'Success' || !formId) {
                    throw new Error(response?.message || 'Template could not be saved');
                }
                return this.saveTemplateDefinition(normalizedPayload, convention, formId).pipe(
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

    getTemplateSummaries(
        page = 0,
        pageSize = 10,
        search = '',
        userId?: number | null,
        draftsOnly = false
    ): Observable<TemplateDefinitionSummaryResponse> {
        const resolvedUserId = userId ?? this.getCurrentUserId();
        const params = new URLSearchParams();
        params.set('page', String(Math.max(0, page)));
        params.set('pageSize', String(Math.max(1, pageSize)));
        if (resolvedUserId != null) {
            params.set('userId', String(resolvedUserId));
        }
        if (search.trim()) {
            params.set('search', search.trim());
        }
        if (draftsOnly) {
            params.set('draftsOnly', 'true');
        }

        return this.http.get<any>(urls.API_URL + 'getTemplateDefinitionSummaries?' + params.toString()).pipe(
            map((response) => ({
                items: (Array.isArray(response?.items) ? response.items : [])
                    .map((template: any) => this.mapTemplateSummaryToSavedTemplate(template))
                    .filter((template: SavedTemplateDefinition | null): template is SavedTemplateDefinition => !!template),
                total: Number(response?.total || 0),
                page: Number(response?.page || 0),
                pageSize: Number(response?.pageSize || pageSize || 10)
            }))
        );
    }

    getTemplate(
        id: string,
        userId?: number | null,
        options?: { includeBackground?: boolean }
    ): Observable<SavedTemplateDefinition | null> {
        const resolvedUserId = userId ?? this.getCurrentUserId();
        const params = new URLSearchParams();
        params.set('formId', id);
        if (resolvedUserId != null) {
            params.set('userId', String(resolvedUserId));
        }
        if (options?.includeBackground === false) {
            params.set('includeBackground', 'false');
        }
        return this.http.get<any>(urls.API_URL + 'getTemplateDefinitionByFormId?' + params.toString()).pipe(
            map((definition) => this.mapTemplateDefinitionToSavedTemplate(definition))
        );
    }

    getTemplateDefinitionBackground(formId: string | number, userId?: number | null): Observable<any> {
        const params = new URLSearchParams();
        params.set('formId', String(formId));
        const resolvedUserId = userId ?? this.getCurrentUserId();
        if (resolvedUserId != null) {
            params.set('userId', String(resolvedUserId));
        }
        return this.http.get<any>(urls.API_URL + 'getTemplateDefinitionBackground?' + params.toString());
    }

    duplicateTemplate(sourceTemplateId: string | number, newTemplateName: string): Observable<SavedTemplateDefinition> {
        const normalizedSourceId = String(sourceTemplateId || '').trim();
        const normalizedName = String(newTemplateName || '').trim();
        if (!normalizedSourceId) {
            throw new Error('Source form ID is required.');
        }
        if (!normalizedName) {
            throw new Error('New form name is required.');
        }

        return this.getTemplate(normalizedSourceId, this.getCurrentUserId()).pipe(
            switchMap((template) => {
                if (!template) {
                    throw new Error('Source form could not be found.');
                }
                const duplicatedPayload = this.buildDuplicatedTemplatePayload(template, normalizedName);
                return this.saveTemplate(duplicatedPayload);
            })
        );
    }

    updateTemplateVisibility(template: SavedTemplateDefinition, userIds: number[]): Observable<SavedTemplateDefinition> {
        if (!template?.payload) {
            return this.getTemplate(template.id, this.getCurrentUserId()).pipe(
                switchMap((fullTemplate) => {
                    if (!fullTemplate) {
                        throw new Error('Template could not be loaded');
                    }
                    return this.updateTemplateVisibility(fullTemplate, userIds);
                })
            );
        }
        const sanitizedUserIds = Array.from(new Set(
            (Array.isArray(userIds) ? userIds : [])
                .map((userId) => Number(userId))
                .filter((userId) => Number.isFinite(userId) && userId > 0)
        ));
        const nextPayload = this.applyTemplateMetadata(template?.payload || {}, {
            userIds: sanitizedUserIds,
            formId: template?.id,
            isActive: template?.isActive !== false
        });
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

    updateTemplateStatus(template: SavedTemplateDefinition, isActive: boolean): Observable<SavedTemplateDefinition> {
        if (!template?.payload) {
            return this.getTemplate(template.id, this.getCurrentUserId()).pipe(
                switchMap((fullTemplate) => {
                    if (!fullTemplate) {
                        throw new Error('Template could not be loaded');
                    }
                    return this.updateTemplateStatus(fullTemplate, isActive);
                })
            );
        }
        const nextPayload = this.applyTemplateMetadata(template?.payload || {}, {
            userIds: Array.isArray(template?.visibilityUserIds) ? template.visibilityUserIds : [],
            formId: template?.id,
            isActive,
            isDraft: isActive ? false : (template?.isDraft === true || template?.payload?.isDraft === true)
        });
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
                    throw new Error(response?.message || 'Template status could not be updated');
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

    getMyTemplateApplications(userId: number, page = 0, pageSize = 10, search = '', all = false): Observable<any> {
        const params = new URLSearchParams();
        params.set('userId', String(userId));
        params.set('all', String(!!all));
        params.set('page', String(Math.max(0, page)));
        params.set('pageSize', String(Math.max(1, pageSize)));
        if (search.trim()) {
            params.set('search', search.trim());
        }
        return this.http.get<any>(urls.API_URL + 'getMyTemplateApplications?' + params.toString());
    }

    getAuditAllTemplateApplications(
        userId: number,
        options: {
            page?: number;
            pageSize?: number;
            search?: string;
            departmentIds?: number[];
            serFormIds?: number[];
            statuses?: string[];
            submittedByUserIds?: number[];
            dateFrom?: string;
            dateTo?: string;
        } = {}
    ): Observable<any> {
        const params = new URLSearchParams();
        params.set('userId', String(userId));
        params.set('page', String(Math.max(0, options.page ?? 0)));
        params.set('pageSize', String(Math.max(1, options.pageSize ?? 10)));
        if (options.search?.trim()) {
            params.set('search', options.search.trim());
        }
        const departmentIds = (options.departmentIds || []).filter((id) => id > 0);
        if (departmentIds.length) {
            params.set('departmentIds', departmentIds.join(','));
        }
        const serFormIds = (options.serFormIds || []).filter((id) => id > 0);
        if (serFormIds.length) {
            params.set('serFormIds', serFormIds.join(','));
        }
        const statuses = (options.statuses || []).map((s) => s.trim()).filter(Boolean);
        if (statuses.length) {
            params.set('statuses', statuses.join(','));
        }
        const submittedByUserIds = (options.submittedByUserIds || []).filter((id) => id > 0);
        if (submittedByUserIds.length) {
            params.set('submittedByUserIds', submittedByUserIds.join(','));
        }
        if (options.dateFrom?.trim()) {
            params.set('dateFrom', options.dateFrom.trim());
        }
        if (options.dateTo?.trim()) {
            params.set('dateTo', options.dateTo.trim());
        }
        return this.http.get<any>(urls.API_URL + 'getAuditAllTemplateApplications?' + params.toString());
    }

    getAuditAllApplicationsFilterOptions(userId: number): Observable<any> {
        return this.http.get<any>(urls.API_URL + 'getAuditAllApplicationsFilterOptions?userId=' + userId);
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

    getCompletedTemplatePortalAttachments(page = 0, pageSize = 50, search = ''): Observable<any> {
        const params = new URLSearchParams({
            page: String(page),
            pageSize: String(pageSize)
        });
        if (search && search.trim()) {
            params.set('search', search.trim());
        }
        return this.http.get<any>(urls.API_URL + 'getCompletedTemplatePortalAttachments?' + params.toString());
    }

    searchVelocityReports(options: {
        page?: number;
        pageSize?: number;
        search?: string;
        sort?: string;
        semantic?: boolean;
        all?: boolean;
        statuses?: string;
        types?: string;
        departments?: string;
        uploadedBy?: string;
        dateFrom?: string;
        dateTo?: string;
        amountMin?: string;
        amountMax?: string;
    }): Observable<any> {
        const params = new URLSearchParams();
        params.set('page', String(Math.max(0, options.page ?? 0)));
        params.set('pageSize', String(Math.max(1, options.pageSize ?? 50)));
        params.set('all', String(options.all !== false));
        if (options.search?.trim()) {
            params.set('search', options.search.trim());
        }
        if (options.sort?.trim()) {
            params.set('sort', options.sort.trim());
        }
        if (options.statuses?.trim()) {
            params.set('statuses', options.statuses.trim());
        }
        if (options.types?.trim()) {
            params.set('types', options.types.trim());
        }
        if (options.departments?.trim()) {
            params.set('departments', options.departments.trim());
        }
        if (options.uploadedBy?.trim()) {
            params.set('uploadedBy', options.uploadedBy.trim());
        }
        if (options.dateFrom?.trim()) {
            params.set('dateFrom', options.dateFrom.trim());
        }
        if (options.dateTo?.trim()) {
            params.set('dateTo', options.dateTo.trim());
        }
        if (options.amountMin?.trim()) {
            params.set('amountMin', options.amountMin.trim());
        }
        if (options.amountMax?.trim()) {
            params.set('amountMax', options.amountMax.trim());
        }
        return this.http.get<any>(urls.API_URL + 'searchVelocityReports?' + params.toString());
    }

    getApplicationStorageInfo(): Observable<any> {
        return this.http.get<any>(urls.API_URL + 'getApplicationStorageInfo');
    }

    getCompletedTemplatePortalAttachmentContent(
        applicationId: number | string,
        fieldId: string,
        attachmentIndex: number
    ): Observable<any> {
        const params = new URLSearchParams({
            applicationId: String(applicationId),
            fieldId: String(fieldId || ''),
            attachmentIndex: String(attachmentIndex)
        });
        return this.http.get<any>(urls.API_URL + 'getCompletedTemplatePortalAttachmentContent?' + params.toString());
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

    saveTemplateDraft(
        template: SavedTemplateDefinition,
        values: Record<string, any>,
        userPipeline: any[] = [],
        options?: {
            applicationId?: number | string | null;
            formCode?: string | null;
            submitAccess?: TemplateSubmitAccessContext;
        }
    ): Observable<{ applicationId: number; formCode: string }> {
        const normalizedValues = this.normalizeApplicationValuesForStorage(values);
        const applicationData: Record<string, any> = {
            ...normalizedValues,
            templateValues: normalizedValues,
            footerFields: userPipeline,
            templatePayload: template.payload
        };
        const applicationId = Number(options?.applicationId);
        const payload: any = {
            serFormId: Number(template.id),
            txtFormCode: options?.formCode || null,
            txtApplicationData: '',
            serSubmittedBy: this.getCurrentUserId(),
            blIsActive: true,
            blIsDeleted: false,
            blnStatus: true
        };
        this.applySubmitAccessToApplication(payload, applicationData, options?.submitAccess);
        payload.txtApplicationData = JSON.stringify(applicationData);
        if (Number.isFinite(applicationId) && applicationId > 0) {
            payload.serApplicationId = applicationId;
        }

        return this.http.post<any>(urls.API_URL + 'saveTemplateApplicationDraft', payload).pipe(
            map((response) => {
                if (!response || response.status !== 'Success') {
                    throw new Error(response?.message || 'Draft could not be saved');
                }
                return {
                    applicationId: Number(response.applicationId),
                    formCode: String(response.formCode || options?.formCode || '')
                };
            })
        );
    }

    submitTemplate(
        template: SavedTemplateDefinition,
        values: Record<string, any>,
        userPipeline: any[] = [],
        codeOverride?: string,
        applicationId?: number | string | null,
        submitAccess?: TemplateSubmitAccessContext
    ): Observable<TemplateSubmission> {
        const normalizedValues = this.normalizeApplicationValuesForStorage(values);
        const applicationData: Record<string, any> = {
            ...normalizedValues,
            templateValues: normalizedValues,
            footerFields: userPipeline,
            templatePayload: template.payload
        };
        const numericApplicationId = Number(applicationId);
        const payload: any = {
            serFormId: Number(template.id),
            txtFormCode: codeOverride || null,
            txtApplicationData: '',
            txtStatus: 'PENDING',
            intCurrentApprovalLevel: 1,
            serSubmittedBy: this.getCurrentUserId(),
            blIsActive: true,
            blIsDeleted: false,
            blnStatus: true,
            deferEmail: true
        };
        this.applySubmitAccessToApplication(payload, applicationData, submitAccess);
        payload.txtApplicationData = JSON.stringify(applicationData);
        if (Number.isFinite(numericApplicationId) && numericApplicationId > 0) {
            payload.serApplicationId = numericApplicationId;
        }

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

    updateTemplateApplicationPdf(
        applicationId: string | number,
        pdfBlob: Blob,
        filename: string,
        queuePostApprovalEmails = false
    ) {
        const formData = new FormData();
        formData.append('applicationId', String(applicationId));
        formData.append('pdf', pdfBlob, filename || 'template-application.pdf');
        let url = urls.API_URL + 'updateApplicationPdf?suppressEditNotification=true';
        if (queuePostApprovalEmails) {
            url += '&queuePostApprovalEmails=true';
        }
        return this.http.post(url, formData);
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

    getApplication(applicationId: string | number, userId?: number | null): Observable<any> {
        let url = urls.API_URL + 'getTemplateApplicationById?applicationId=' + encodeURIComponent(String(applicationId));
        if (userId != null && Number(userId) > 0) {
            url += '&userId=' + encodeURIComponent(String(userId));
        }
        return this.http.get<any>(url);
    }

    getTemplateApplicationPdfBase64(applicationId: string | number): Observable<any> {
        return this.http.get<any>(
            urls.API_URL + 'getTemplateApplicationPdfBase64?applicationId=' + encodeURIComponent(String(applicationId))
        );
    }

    downloadTemplateApplicationPdf(applicationId: string | number): Observable<Blob> {
        return this.http.get(urls.API_URL + 'downloadTemplateApplicationPdf?applicationId=' + encodeURIComponent(String(applicationId)), {
            responseType: 'blob'
        });
    }

    checkApplicationPdfSignatures(applicationId: string | number): Observable<PdfSignatureCheckResult> {
        return this.http.get<PdfSignatureCheckResult>(
            urls.API_URL + 'checkApplicationPdfSignatures?applicationId=' + encodeURIComponent(String(applicationId))
        );
    }

    getTemplateApprovalReminderPreview(applicationId: string | number, userId: number): Observable<any> {
        const params = new URLSearchParams();
        params.set('applicationId', String(applicationId));
        params.set('userId', String(userId));
        return this.http.get<any>(urls.API_URL + 'getTemplateApprovalReminderPreview?' + params.toString());
    }

    sendTemplateApprovalManualReminder(applicationId: string | number, userId: number, subject: string, body: string): Observable<any> {
        return this.http.post<any>(urls.API_URL + 'sendTemplateApprovalManualReminder', {
            applicationId,
            userId,
            subject,
            body
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

    getTemplateApprovalLocationByIp(ip: string) {
        return this.http.get<any>(urls.API_URL + 'getTemplateApprovalLocationByIp', {
            params: { ip: ip || '' }
        });
    }

    approveTemplateApplication(
        applicationId: string | number,
        remarks: string,
        deferEmail = false,
        onBehalfOfUserId?: number | null
    ) {
        return this.http.post<any>(urls.API_URL + 'approveTemplateApplication', this.buildTemplateWorkflowActionBody(
            applicationId,
            remarks,
            { deferEmail },
            onBehalfOfUserId
        ));
    }

    rejectTemplateApplication(applicationId: string | number, remarks: string, onBehalfOfUserId?: number | null) {
        return this.http.post<any>(urls.API_URL + 'rejectTemplateApplication', this.buildTemplateWorkflowActionBody(
            applicationId,
            remarks,
            {},
            onBehalfOfUserId
        ));
    }

    sendBackTemplateApplication(applicationId: string | number, remarks: string, onBehalfOfUserId?: number | null) {
        return this.http.post<any>(urls.API_URL + 'sendBackTemplateApplication', this.buildTemplateWorkflowActionBody(
            applicationId,
            remarks,
            {},
            onBehalfOfUserId
        ));
    }

    sendBackTemplateApplicationToInitiator(applicationId: string | number, remarks: string, onBehalfOfUserId?: number | null) {
        return this.http.post<any>(urls.API_URL + 'sendBackTemplateApplicationToInitiator', this.buildTemplateWorkflowActionBody(
            applicationId,
            remarks,
            {},
            onBehalfOfUserId
        ));
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

    holdTemplateApplication(applicationId: string | number, remarks: string, onBehalfOfUserId?: number | null) {
        return this.http.post<any>(urls.API_URL + 'holdTemplateApplication', this.buildTemplateWorkflowActionBody(
            applicationId,
            remarks,
            {},
            onBehalfOfUserId
        ));
    }

    unholdTemplateApplication(applicationId: string | number, remarks: string = '', onBehalfOfUserId?: number | null) {
        return this.http.post<any>(urls.API_URL + 'unholdTemplateApplication', this.buildTemplateWorkflowActionBody(
            applicationId,
            remarks,
            {},
            onBehalfOfUserId
        ));
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
        const normalizedTemplatePayload = this.applyTemplateMetadata(templatePayload, {
            userIds: visibilityUserIds,
            formId,
            isActive: this.extractTemplateIsActive(payload),
            isDraft: payload?.isDraft === true
        });

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

    private buildBackendFormPayload(payload: any, convention: TemplateCodeConvention, existingId?: string, asDraft = false): any {
        const fields = Array.isArray(payload.fields) ? payload.fields : [];
        const publishActive = !asDraft && payload.isActive !== false;
        return {
            ...(existingId ? { serFormId: Number(existingId) } : {}),
            txtFormName: payload.name || 'Untitled Template',
            txtFormDescription: 'template-builder',
            txtConventionPrefix: convention.prefix,
            txtFormCode: convention.pattern,
            blIsActive: publishActive,
            blIsDeleted: false,
            blnStatus: publishActive,
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
        const isActive = this.extractTemplateIsActive(payload, definition);
        const isDraft = payload?.isDraft === true;
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
            isActive,
            isDraft,
            backendDefinition: definition
        };

        const hasOwnAccess = definition?.hasOwnAccess === true;
        const hasDelegatedAccess = definition?.hasDelegatedAccess === true;
        const listingAccess = definition?.listingAccess as SavedTemplateDefinition['listingAccess'];
        const delegationOnBehalfOf = Array.isArray(definition?.delegationOnBehalfOf)
            ? definition.delegationOnBehalfOf
            : [];

        return {
            id: String(definition.serFormId),
            name: payload.name || 'Untitled Template',
            codeConvention: convention,
            createdAt: definition.dteCreatedDate || new Date().toISOString(),
            updatedAt: definition.dteModifiedDate || definition.dteCreatedDate || new Date().toISOString(),
            payload,
            backendForm: definition,
            visibilityUserIds,
            isActive,
            isDraft,
            hasOwnAccess,
            hasDelegatedAccess,
            listingAccess,
            delegationOnBehalfOf,
        };
    }

    private mapTemplateSummaryToSavedTemplate(summary: any): SavedTemplateDefinition | null {
        if (!summary?.serFormId) {
            return null;
        }

        const convention = this.parseCodeConvention(summary?.txtCodeConvention || 'TPL-0000');
        const visibilityUserIds = this.normalizeVisibilityUserIds(
            summary?.visibilityUserIds ?? summary?.visibleUserIds ?? summary?.selectedUsers
        );
        const isActive = summary?.isActive === false ? false : this.extractTemplateIsActive(summary);
        const isDraft = summary?.isDraft === true;
        const hasOwnAccess = summary?.hasOwnAccess === true;
        const hasDelegatedAccess = summary?.hasDelegatedAccess === true;
        const listingAccess = summary?.listingAccess as SavedTemplateDefinition['listingAccess'];
        const delegationOnBehalfOf = Array.isArray(summary?.delegationOnBehalfOf)
            ? summary.delegationOnBehalfOf
            : [];

        return {
            id: String(summary.serFormId),
            name: summary?.txtTemplateName || 'Untitled Template',
            codeConvention: convention,
            createdAt: summary?.dteCreatedDate || new Date().toISOString(),
            updatedAt: summary?.dteModifiedDate || summary?.dteCreatedDate || new Date().toISOString(),
            payload: null,
            backendForm: summary,
            visibilityUserIds,
            isActive,
            isDraft,
            hasOwnAccess,
            hasDelegatedAccess,
            listingAccess,
            delegationOnBehalfOf,
        };
    }

    private buildDuplicatedTemplatePayload(template: SavedTemplateDefinition, newTemplateName: string): any {
        const payload = this.cloneTemplatePayload(template?.payload || {});
        delete payload.id;
        delete payload.createdAt;
        delete payload.updatedAt;

        return {
            ...payload,
            name: newTemplateName,
            isActive: template?.isActive !== false,
            isDraft: false,
            visibleUserIds: Array.isArray(template?.visibilityUserIds) ? [...template.visibilityUserIds] : [],
            visibilityUserIds: Array.isArray(template?.visibilityUserIds) ? [...template.visibilityUserIds] : []
        };
    }

    private cloneTemplatePayload<T>(value: T): T {
        try {
            return JSON.parse(JSON.stringify(value));
        } catch {
            return value;
        }
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

    private buildTemplateWorkflowActionBody(
        applicationId: string | number,
        remarks: string,
        extras: Record<string, unknown> = {},
        onBehalfOfUserId?: number | null
    ): Record<string, unknown> {
        const body: Record<string, unknown> = {
            applicationId: Number(applicationId),
            userId: this.getCurrentUserId(),
            remarks: remarks || '',
            ...extras
        };
        const onBehalfOf = Number(onBehalfOfUserId);
        if (Number.isFinite(onBehalfOf) && onBehalfOf > 0) {
            body['onBehalfOfUserId'] = onBehalfOf;
        }
        return body;
    }

    private applySubmitAccessToApplication(
        payload: { serSubmittedBy?: number | null },
        applicationData: Record<string, any>,
        submitAccess?: TemplateSubmitAccessContext
    ): void {
        const actorId = Number(this.getCurrentUserId());
        const onBehalfId = Number(submitAccess?.onBehalfUserId);
        const isOnBehalf = submitAccess?.fillAccessMode === 'on_behalf'
            && Number.isFinite(onBehalfId)
            && onBehalfId > 0;
        if (isOnBehalf) {
            applicationData['fillAccessMode'] = 'on_behalf';
            applicationData['onBehalfOfUserId'] = onBehalfId;
            applicationData['submittedByDelegateUserId'] = actorId;
            payload.serSubmittedBy = onBehalfId;
            return;
        }
        applicationData['fillAccessMode'] = 'self';
        delete applicationData['onBehalfOfUserId'];
        delete applicationData['submittedByDelegateUserId'];
        if (Number.isFinite(actorId) && actorId > 0) {
            payload.serSubmittedBy = actorId;
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

    private extractTemplateIsActive(payload: any, definition?: any): boolean {
        if (payload?.isActive === false || String(payload?.isActive).toLowerCase() === 'false') {
            return false;
        }
        if (payload?.blnStatus === false || String(payload?.blnStatus).toLowerCase() === 'false') {
            return false;
        }
        if (payload?.blIsActive === false || String(payload?.blIsActive).toLowerCase() === 'false') {
            return false;
        }
        if (definition?.getBlnStatus === false || definition?.blnStatus === false) {
            return false;
        }
        if (definition?.getBlIsActive === false || definition?.blIsActive === false) {
            return false;
        }
        return true;
    }

    private applyTemplateMetadata(payload: any, options: { userIds: number[]; formId?: string | number; isActive: boolean; isDraft?: boolean }): any {
        const { userIds, formId, isActive, isDraft } = options;
        return {
            ...payload,
            ...(formId != null ? { id: String(formId) } : {}),
            isActive,
            isDraft: isDraft === true,
            blIsActive: isActive,
            blnStatus: isActive,
            visibleUserIds: [...userIds],
            visibilityUserIds: [...userIds],
            selectedUsers: [...userIds]
        };
    }
}
