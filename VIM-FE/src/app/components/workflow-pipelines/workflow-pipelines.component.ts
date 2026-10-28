import { Component, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CustomFormService } from 'src/app/services/custom-form/custom-form.service';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import { DepartmentService } from 'src/app/services/department/department.service';
import { UserService } from 'src/app/services/user/user.service';
import {
    WorkflowPipelineDetail,
    WorkflowPipelineSavePayload,
    WorkflowPipelineService,
} from 'src/app/services/workflow-pipeline/workflow-pipeline.service';
import {
    WorkflowThresholdRuleSavePayload,
    WorkflowThresholdRuleService,
} from 'src/app/services/workflow-threshold-rule/workflow-threshold-rule.service';
import { WorkflowPipelineListItem } from './workflow-pipelines.demo-data';
import {
    THRESHOLD_APPROVAL_STEP_OPTIONS,
    THRESHOLD_INSERT_POSITION_LABELS,
    THRESHOLD_OPERATOR_LABELS,
    THRESHOLD_ROUTING_ACTION_LABELS,
    THRESHOLD_TARGET_TYPE_LABELS,
    ThresholdInsertPosition,
    ThresholdOperator,
    ThresholdRoutingAction,
    ThresholdTargetType,
    WorkflowPipelineThresholdRule,
    isThresholdNumericFieldType,
} from './workflow-pipeline-thresholds.demo-data';
import {
    SELF_HOD_DEPARTMENT_ID,
    WORKFLOW_PIPELINE_STEP_TYPES,
    WorkflowPipelineApprovalMode,
    WorkflowPipelineStep,
    WorkflowPipelineStepType,
    clonePipelineSteps,
    createWorkflowStepId,
    defaultInitiatorStep,
    ensureInitiatorStep,
    pipelineStepRowLabel,
    pipelineStepTargetLabel,
    reorderWorkflowPipelineSteps,
    departmentDisplayName,
    roleDisplayName,
    userDisplayName,
} from './workflow-pipeline-step.util';
import {
    FormPipelinePreviewStep,
    approvalPipelinesAsRawSteps,
    buildFormPipelinePreviewFromBackendSteps,
    buildFormPipelinePreviewFromTemplatePipeline,
    isTemplateBuilderForm,
    parseJsonApprovalPipelineSteps,
} from './workflow-form-pipeline.util';

export type WorkflowPipelinesPageTab = 'pipelines' | 'thresholds';

export interface ThresholdFormListItem {
    serFormId: number;
    name: string;
    code?: string;
    title?: string;
}

export interface ThresholdFormFieldOption {
    key: string;
    label: string;
    fieldType?: string;
}

@Component({
    selector: 'app-workflow-pipelines',
    templateUrl: './workflow-pipelines.component.html',
    styleUrls: ['./workflow-pipelines.component.css'],
})
export class WorkflowPipelinesComponent implements OnInit, OnDestroy {
    @ViewChild('editorModal') editorModal: any;
    @ViewChild('thresholdRuleModal') thresholdRuleModal: any;

    pageTab: WorkflowPipelinesPageTab = 'pipelines';
    search = '';
    thresholdSearch = '';
    loading = false;
    loadError = '';
    rows: WorkflowPipelineListItem[] = [];
    thresholdRows: WorkflowPipelineThresholdRule[] = [];
    thresholdRulesLoading = false;
    thresholdRulesLoadError = '';
    thresholdRuleSaving = false;

    editorForm!: FormGroup;
    editorSubmit = false;
    editorSaving = false;
    editorLoading = false;
    editingId: number | null = null;
    pipelineSteps: WorkflowPipelineStep[] = [];

    departments: any[] = [];
    roles: any[] = [];
    allUsers: any[] = [];

    showStepPanel = false;
    editingStepId = '';
    stepName = '';
    stepType: Exclude<WorkflowPipelineStepType, 'initiator'> = 'department';
    stepApprovalMode: WorkflowPipelineApprovalMode = 'OR';
    stepDepartmentId: number | null = null;
    stepRoleId: number | null = null;
    stepSelectedUserIds: number[] = [];
    stepUserSearch = '';
    showStepUserDropdown = false;

    readonly pipelineStepTypes = WORKFLOW_PIPELINE_STEP_TYPES;
    readonly selfHodDepartmentId = SELF_HOD_DEPARTMENT_ID;
    readonly thresholdOperators = Object.entries(THRESHOLD_OPERATOR_LABELS).map(([value, label]) => ({
        value: value as ThresholdOperator,
        label,
    }));
    readonly thresholdTargetTypes = Object.entries(THRESHOLD_TARGET_TYPE_LABELS).map(([value, label]) => ({
        value: value as ThresholdTargetType,
        label,
    }));
    readonly thresholdRoutingActions = Object.entries(THRESHOLD_ROUTING_ACTION_LABELS).map(([value, label]) => ({
        value: value as ThresholdRoutingAction,
        label,
    }));
    readonly thresholdInsertPositions = Object.entries(THRESHOLD_INSERT_POSITION_LABELS).map(([value, label]) => ({
        value: value as ThresholdInsertPosition,
        label,
    }));
    readonly thresholdApprovalSteps = THRESHOLD_APPROVAL_STEP_OPTIONS;

    thresholdForms: ThresholdFormListItem[] = [];
    thresholdFormsLoading = false;
    thresholdFormFields: ThresholdFormFieldOption[] = [];
    thresholdFormFieldsLoading = false;
    thresholdFormPipelineSteps: FormPipelinePreviewStep[] = [];
    thresholdFormPipelineLoading = false;

    thresholdRuleForm!: FormGroup;
    thresholdRuleSubmit = false;
    editingThresholdId: number | null = null;
    thresholdPreviewSampleValue = '';

    thresholdCols = [
        { field: 'ruleName', title: 'Rule' },
        { field: 'condition', title: 'Condition', sort: false },
        { field: 'routing', title: 'Approver @ step', sort: false },
        { field: 'priority', title: 'Priority', width: '90px' },
        { field: 'active', title: 'Active', width: '90px' },
        { field: 'actions', title: 'Actions', width: '100px', sort: false, headerClass: 'justify-center' },
    ];

    cols = [
        { field: 'name', title: 'Name' },
        { field: 'code', title: 'Code', width: '120px' },
        { field: 'stepCount', title: 'Steps', width: '90px' },
        { field: 'pipelineType', title: 'Type', width: '120px' },
        { field: 'modifiedDate', title: 'Modified', width: '120px' },
        { field: 'active', title: 'Active', width: '90px' },
        { field: 'actions', title: 'Actions', width: '140px', sort: false, headerClass: 'justify-center' },
    ];

    constructor(
        private fb: FormBuilder,
        private notificationService: NotificationService,
        private workflowPipelineService: WorkflowPipelineService,
        private departmentService: DepartmentService,
        private userService: UserService,
        private customFormService: CustomFormService,
        private templateWorkflowService: TemplateWorkflowService,
        private workflowThresholdRuleService: WorkflowThresholdRuleService
    ) {}

    ngOnInit(): void {
        window.addEventListener('velocity-tour-prepare', this.onTourPrepare);
        this.editorForm = this.fb.group({
            name: ['', Validators.required],
            code: [''],
            description: [''],
            active: [true],
        });
        this.thresholdRuleForm = this.fb.group({
            ruleName: ['', Validators.required],
            serFormId: [null as number | null, Validators.required],
            fieldKey: ['', Validators.required],
            fieldLabel: ['', Validators.required],
            operator: ['greater_than', Validators.required],
            value: ['', Validators.required],
            targetType: ['department', Validators.required],
            targetId: [null as number | null, Validators.required],
            targetLabel: ['', Validators.required],
            routingAction: ['override' as ThresholdRoutingAction, Validators.required],
            insertPosition: ['after' as ThresholdInsertPosition],
            approvalStepOrder: [3, Validators.required],
            priority: [10, Validators.required],
            active: [true],
            notes: [''],
        });
        this.loadMasterData();
        this.loadRows();
    }

    ngOnDestroy(): void {
        window.removeEventListener('velocity-tour-prepare', this.onTourPrepare);
    }

    private onTourPrepare = (event: Event): void => {
        const action = (event as CustomEvent<{ action?: string }>).detail?.action;
        switch (action) {
            case 'wp-close-modals':
                this.closeEditor();
                this.closeThresholdRuleModal();
                break;
            case 'wp-tab-pipelines':
                this.setPageTab('pipelines');
                break;
            case 'wp-tab-thresholds':
                this.setPageTab('thresholds');
                break;
            default:
                break;
        }
    };

    loadMasterData(): void {
        this.departmentService.getAll().subscribe({
            next: (data) => {
                this.departments = Array.isArray(data)
                    ? data.filter((d) => d && d.blIsDeleted !== true && d.blnStatus !== false)
                    : [];
            },
            error: () => (this.departments = []),
        });
        this.userService.getRoles().subscribe({
            next: (data) => (this.roles = Array.isArray(data) ? data : []),
            error: () => (this.roles = []),
        });
        this.userService.getUsers().subscribe({
            next: (data) => (this.allUsers = Array.isArray(data) ? data : []),
            error: () => (this.allUsers = []),
        });
    }

    loadRows(): void {
        this.loading = true;
        this.loadError = '';
        this.workflowPipelineService.getAll().subscribe({
            next: (data) => {
                this.rows = (data || []).map((row) => ({ ...row }));
                this.loading = false;
            },
            error: () => {
                this.loading = false;
                this.loadError = 'Could not load workflow pipelines.';
                this.notificationService.showMessage(this.loadError, 'error');
            },
        });
    }

    onNewPipeline(): void {
        this.editingId = null;
        this.editorSubmit = false;
        this.editorLoading = true;
        this.workflowPipelineService.getNew().subscribe({
            next: (detail) => {
                this.openEditorWithDetail(detail);
                this.editorLoading = false;
            },
            error: () => {
                this.editorLoading = false;
                this.openEditorWithDetail({
                    id: 0,
                    name: '',
                    code: '',
                    description: '',
                    pipelineType: 'STANDARD',
                    stepCount: 0,
                    active: true,
                    modifiedDate: '',
                    pipeline: [defaultInitiatorStep()],
                });
            },
        });
    }

    onEdit(row: WorkflowPipelineListItem): void {
        if (!row?.id) {
            return;
        }
        this.editingId = row.id;
        this.editorSubmit = false;
        this.editorLoading = true;
        this.workflowPipelineService.getById(row.id).subscribe({
            next: (detail) => {
                this.openEditorWithDetail(detail);
                this.editorLoading = false;
            },
            error: () => {
                this.editorLoading = false;
                this.notificationService.showMessage('Could not load pipeline.', 'error');
            },
        });
    }

    private openEditorWithDetail(detail: WorkflowPipelineDetail): void {
        this.editorForm.reset({
            name: detail.name || '',
            code: detail.code || '',
            description: detail.description || '',
            active: detail.active !== false,
        });
        this.pipelineSteps = clonePipelineSteps(detail.pipeline);
        this.closeStepPanel();
        this.editorModal?.open();
    }

    closeEditor(): void {
        this.editorModal?.close();
        this.closeStepPanel();
    }

    saveEditor(): void {
        this.editorSubmit = true;
        if (this.editorForm.invalid) {
            return;
        }
        const steps = ensureInitiatorStep(this.pipelineSteps);
        if (steps.filter((s) => s.type !== 'initiator').length === 0) {
            this.notificationService.showMessage('Add at least one approval step after Initiator.', 'warning');
            return;
        }

        const raw = this.editorForm.value;
        const payload: WorkflowPipelineSavePayload = {
            name: String(raw.name || '').trim(),
            code: String(raw.code || '').trim() || null,
            description: String(raw.description || '').trim() || null,
            pipelineType: 'STANDARD',
            active: !!raw.active,
            pipeline: steps,
        };

        this.editorSaving = true;
        const request$ =
            this.editingId != null && this.editingId > 0
                ? this.workflowPipelineService.update({ ...payload, serWorkflowPipelineId: this.editingId })
                : this.workflowPipelineService.create(payload);

        request$.subscribe({
            next: () => {
                this.editorSaving = false;
                this.notificationService.showMessage(
                    this.editingId ? 'Pipeline updated.' : 'Pipeline created.',
                    'success'
                );
                this.closeEditor();
                this.loadRows();
            },
            error: (err) => {
                this.editorSaving = false;
                const msg =
                    err?.status === 403
                        ? 'You do not have permission to save pipelines.'
                        : 'Save failed. Check name, code, and steps.';
                this.notificationService.showMessage(msg, 'error');
            },
        });
    }

    onDuplicate(row: WorkflowPipelineListItem): void {
        if (!row?.id) {
            return;
        }
        this.workflowPipelineService.duplicate(row.id).subscribe({
            next: () => {
                this.notificationService.showMessage(`Duplicated "${row.name}".`, 'success');
                this.loadRows();
            },
            error: () => this.notificationService.showMessage('Duplicate failed.', 'error'),
        });
    }

    getStepRowLabel(step: WorkflowPipelineStep): string {
        return pipelineStepRowLabel(step);
    }

    getStepTargetLabel(step: WorkflowPipelineStep): string {
        return pipelineStepTargetLabel(step);
    }

    openAddStepPanel(): void {
        this.editingStepId = '';
        this.stepName = '';
        this.stepType = 'department';
        this.stepApprovalMode = 'OR';
        this.stepDepartmentId = null;
        this.stepRoleId = null;
        this.stepSelectedUserIds = [];
        this.stepUserSearch = '';
        this.showStepPanel = true;
    }

    openEditStepPanel(step: WorkflowPipelineStep): void {
        if (step.type === 'initiator') {
            this.notificationService.showMessage('Initiator step is fixed.', 'info');
            return;
        }
        this.editingStepId = step.id;
        this.stepName = step.name || '';
        this.stepType = step.type as Exclude<WorkflowPipelineStepType, 'initiator'>;
        this.stepApprovalMode = (step.approvalMode as WorkflowPipelineApprovalMode) || 'OR';
        if (step.dynamicTarget === 'initiator_hod') {
            this.stepDepartmentId = this.selfHodDepartmentId;
        } else {
            this.stepDepartmentId = step.serDepartmentId != null ? Number(step.serDepartmentId) : null;
        }
        this.stepRoleId = step.serRoleId != null ? Number(step.serRoleId) : null;
        const ids = Array.isArray(step.userIds) ? step.userIds : [];
        const fromUsers = (step.users || [])
            .map((u) => Number(u['serUserId'] ?? u['userId'] ?? u['id']))
            .filter((id) => Number.isFinite(id) && id > 0);
        this.stepSelectedUserIds = [...new Set([...ids, ...fromUsers])];
        this.showStepPanel = true;
    }

    closeStepPanel(): void {
        this.showStepPanel = false;
        this.showStepUserDropdown = false;
        this.editingStepId = '';
    }

    onStepTypeChanged(): void {
        this.stepDepartmentId = null;
        this.stepRoleId = null;
        this.stepSelectedUserIds = [];
        this.stepApprovalMode = 'OR';
    }

    saveStepPanel(): void {
        const built = this.buildStepFromPanel();
        if (!built) {
            this.notificationService.showMessage('Complete required fields for this step type.', 'warning');
            return;
        }
        if (this.editingStepId) {
            const idx = this.pipelineSteps.findIndex((s) => s.id === this.editingStepId);
            if (idx >= 0) {
                this.pipelineSteps[idx] = { ...built, id: this.editingStepId, order: this.pipelineSteps[idx].order };
            }
        } else {
            this.pipelineSteps.push(built);
        }
        this.pipelineSteps = reorderWorkflowPipelineSteps(this.pipelineSteps);
        this.closeStepPanel();
    }

    private buildStepFromPanel(): WorkflowPipelineStep | null {
        const id = this.editingStepId || createWorkflowStepId();
        const name = this.stepName.trim();
        const approvalMode = this.stepApprovalMode || 'OR';
        const emptyPermissions: unknown[] = [];

        if (this.stepType === 'department') {
            if (Number(this.stepDepartmentId) === this.selfHodDepartmentId) {
                return {
                    id,
                    name: name || 'Self HOD',
                    type: 'department',
                    approvalMode,
                    dynamicTarget: 'initiator_hod',
                    fieldPermissions: emptyPermissions,
                    fieldPermissionsConfigured: true,
                };
            }
            const dept = this.departments.find((d) => Number(d.serDepartmentId) === Number(this.stepDepartmentId));
            if (!dept) {
                return null;
            }
            return {
                id,
                name: name || departmentDisplayName(dept),
                type: 'department',
                approvalMode,
                serDepartmentId: Number(dept.serDepartmentId),
                hrTblDepartment: dept,
                fieldPermissions: emptyPermissions,
                fieldPermissionsConfigured: true,
            };
        }

        if (this.stepType === 'role') {
            const role = this.roles.find((r) => Number(r.serRoleId) === Number(this.stepRoleId));
            if (!role) {
                return null;
            }
            return {
                id,
                name: name || roleDisplayName(role),
                type: 'role',
                approvalMode,
                serRoleId: Number(role.serRoleId),
                cfgTblRole: role,
                fieldPermissions: emptyPermissions,
                fieldPermissionsConfigured: true,
            };
        }

        const users = this.allUsers.filter((u) => {
            const uid = Number(u?.serUserId ?? u?.userId ?? u?.id);
            return this.stepSelectedUserIds.includes(uid);
        });
        if (users.length === 0) {
            return null;
        }
        return {
            id,
            name: name || users.map((u) => userDisplayName(u)).join(', '),
            type: 'individual',
            approvalMode,
            users,
            userIds: users.map((u) => Number(u.serUserId ?? u.userId ?? u.id)).filter((v) => v > 0),
            fieldPermissions: emptyPermissions,
            fieldPermissionsConfigured: true,
        };
    }

    removeStep(index: number): void {
        if (this.pipelineSteps[index]?.type === 'initiator') {
            return;
        }
        this.pipelineSteps.splice(index, 1);
        this.pipelineSteps = reorderWorkflowPipelineSteps(this.pipelineSteps);
    }

    moveStep(index: number, direction: 'up' | 'down'): void {
        const target = direction === 'up' ? index - 1 : index + 1;
        if (
            target < 0 ||
            target >= this.pipelineSteps.length ||
            this.pipelineSteps[index]?.type === 'initiator' ||
            this.pipelineSteps[target]?.type === 'initiator'
        ) {
            return;
        }
        const steps = [...this.pipelineSteps];
        [steps[index], steps[target]] = [steps[target], steps[index]];
        this.pipelineSteps = reorderWorkflowPipelineSteps(steps);
    }

    filteredStepUsers(): any[] {
        const term = this.stepUserSearch.trim().toLowerCase();
        return (this.allUsers || []).filter((user) => {
            const id = Number(user?.serUserId ?? user?.userId ?? user?.id);
            if (!Number.isFinite(id) || id <= 0) {
                return false;
            }
            if (!term) {
                return true;
            }
            const label = userDisplayName(user).toLowerCase();
            const email = String(user?.txtAddress || user?.email || '').toLowerCase();
            return label.includes(term) || email.includes(term);
        });
    }

    isStepUserSelected(user: any): boolean {
        const id = Number(user?.serUserId ?? user?.userId ?? user?.id);
        return this.stepSelectedUserIds.includes(id);
    }

    toggleStepUser(user: any, event: Event): void {
        event.stopPropagation();
        const id = Number(user?.serUserId ?? user?.userId ?? user?.id);
        if (!Number.isFinite(id) || id <= 0) {
            return;
        }
        if (this.stepSelectedUserIds.includes(id)) {
            this.stepSelectedUserIds = this.stepSelectedUserIds.filter((x) => x !== id);
        } else {
            this.stepSelectedUserIds = [...this.stepSelectedUserIds, id];
        }
    }

    stepUsersSummary(): string {
        if (!this.stepSelectedUserIds.length) {
            return 'Select employees';
        }
        if (this.stepSelectedUserIds.length === 1) {
            const user = this.allUsers.find(
                (u) => Number(u.serUserId ?? u.userId ?? u.id) === this.stepSelectedUserIds[0]
            );
            return user ? userDisplayName(user) : '1 employee';
        }
        return `${this.stepSelectedUserIds.length} employees selected`;
    }

    showStepApprovalMode(): boolean {
        return this.stepType !== 'individual' || this.stepSelectedUserIds.length > 1;
    }

    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent): void {
        const target = event.target as HTMLElement | null;
        if (!target?.closest('.wp-step-user-dropdown')) {
            this.showStepUserDropdown = false;
        }
    }

    setPageTab(tab: WorkflowPipelinesPageTab): void {
        this.pageTab = tab;
        if (tab === 'thresholds') {
            if (!this.thresholdForms.length && !this.thresholdFormsLoading) {
                this.loadThresholdForms();
            }
            this.loadThresholdRules();
        }
    }

    loadThresholdRules(): void {
        this.thresholdRulesLoading = true;
        this.thresholdRulesLoadError = '';
        this.workflowThresholdRuleService.getAll().subscribe({
            next: (data) => {
                this.thresholdRulesLoading = false;
                this.thresholdRows = Array.isArray(data) ? data.map((r) => ({ ...r })) : [];
            },
            error: () => {
                this.thresholdRulesLoading = false;
                this.thresholdRulesLoadError = 'Could not load rules.';
                this.notificationService.showMessage(this.thresholdRulesLoadError, 'error');
            },
        });
    }

    loadThresholdForms(): void {
        this.thresholdFormsLoading = true;
        this.customFormService.getAll().subscribe({
            next: (data: any) => {
                this.thresholdFormsLoading = false;
                const list = Array.isArray(data) ? data : [];
                this.thresholdForms = list
                    .map((form: any) => ({
                        serFormId: Number(form.serFormId),
                        name: String(form.txtFormName || '').trim() || `Form #${form.serFormId}`,
                        code: form.txtFormCode ? String(form.txtFormCode).trim() : undefined,
                        title: form.txtFormDescription ? String(form.txtFormDescription).trim() : undefined,
                    }))
                    .filter((f) => f.serFormId > 0)
                    .sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
            },
            error: () => {
                this.thresholdFormsLoading = false;
                this.notificationService.showMessage('Could not load forms.', 'error');
            },
        });
    }

    thresholdFormDisplayLabel(form: ThresholdFormListItem): string {
        const parts = [form.name];
        if (form.title && form.title !== form.name) {
            parts.push(form.title);
        }
        if (form.code) {
            parts.push(`(${form.code})`);
        }
        return parts.join(' — ');
    }

    onThresholdFormChanged(): void {
        const formId = Number(this.thresholdRuleForm.get('serFormId')?.value);
        this.thresholdRuleForm.patchValue({ fieldKey: '', fieldLabel: '' });
        this.thresholdFormFields = [];
        this.thresholdFormPipelineSteps = [];
        if (!formId) {
            return;
        }
        this.loadThresholdFormFields(formId);
    }

    private loadThresholdFormFields(formId: number, preferredFieldKey?: string, preferredStepOrder?: number): void {
        this.thresholdFormFieldsLoading = true;
        this.customFormService.getById(formId).subscribe({
            next: (form: any) => {
                this.thresholdFormFieldsLoading = false;
                this.loadThresholdFormPipeline(formId, form, preferredStepOrder);
                const raw = (form?.cfgTblCustomFormFields || []) as any[];
                const filtered = raw
                    .filter((field) => field && field.blIsDeleted !== true && field.blIsActive !== false)
                    .filter((field) => isThresholdNumericFieldType(field.txtFieldType))
                    .sort((a, b) => (Number(a.intFieldOrder) || 0) - (Number(b.intFieldOrder) || 0));
                this.thresholdFormFields = filtered.map((field) => {
                    const label = String(field.txtFieldLabel || '').trim() || `Field #${field.serFieldId}`;
                    return {
                        key: this.thresholdFieldKeyFromLabel(label),
                        label,
                        fieldType: field.txtFieldType ? String(field.txtFieldType) : undefined,
                    };
                });
                const pick =
                    (preferredFieldKey && this.thresholdFormFields.find((f) => f.key === preferredFieldKey)) ||
                    this.thresholdFormFields[0];
                if (pick) {
                    this.thresholdRuleForm.patchValue({ fieldKey: pick.key, fieldLabel: pick.label });
                }
            },
            error: () => {
                this.thresholdFormFieldsLoading = false;
                this.thresholdFormPipelineLoading = false;
                this.notificationService.showMessage('Could not load form fields.', 'error');
            },
        });
    }

    private loadThresholdFormPipeline(formId: number, form: any, preferredStepOrder?: number): void {
        this.thresholdFormPipelineLoading = true;
        const apply = (steps: FormPipelinePreviewStep[]) => {
            this.thresholdFormPipelineLoading = false;
            this.thresholdFormPipelineSteps = steps;
            this.syncThresholdApprovalStepOrder(preferredStepOrder);
        };

        const finishFromBackend = () => {
            const jsonSteps = parseJsonApprovalPipelineSteps(form);
            const raw = jsonSteps.length ? jsonSteps : approvalPipelinesAsRawSteps(form);
            apply(buildFormPipelinePreviewFromBackendSteps(raw));
        };

        if (isTemplateBuilderForm(form)) {
            this.templateWorkflowService.getTemplate(String(formId)).subscribe({
                next: (template) => {
                    const pipeline = template?.payload?.pipeline;
                    if (Array.isArray(pipeline) && pipeline.length) {
                        apply(buildFormPipelinePreviewFromTemplatePipeline(pipeline));
                    } else {
                        finishFromBackend();
                    }
                },
                error: () => finishFromBackend(),
            });
            return;
        }

        finishFromBackend();
    }

    private syncThresholdApprovalStepOrder(preferredStepOrder?: number): void {
        const options = this.getThresholdApprovalStepOptions();
        if (!options.length) {
            return;
        }
        const current = Number(this.thresholdRuleForm.get('approvalStepOrder')?.value);
        const preferredValid = preferredStepOrder != null && options.some((o) => o.order === preferredStepOrder);
        const currentValid = options.some((o) => o.order === current);
        if (preferredValid) {
            this.thresholdRuleForm.patchValue({ approvalStepOrder: preferredStepOrder });
        } else if (!currentValid) {
            this.thresholdRuleForm.patchValue({ approvalStepOrder: options[0].order });
        }
    }

    isThresholdInsertRouting(): boolean {
        return this.thresholdRuleForm.get('routingAction')?.value === 'insert';
    }

    onThresholdRoutingActionChanged(): void {
        const action = this.thresholdRuleForm.get('routingAction')?.value as ThresholdRoutingAction;
        if (action === 'insert' && !this.thresholdRuleForm.get('insertPosition')?.value) {
            this.thresholdRuleForm.patchValue({ insertPosition: 'after' });
        }
        this.syncThresholdApprovalStepOrder();
    }

    getThresholdAnchorStepOptions(): { order: number; label: string }[] {
        const fromForm = this.thresholdFormPipelineSteps.map((s) => ({
            order: s.order,
            label: `${s.order}. ${s.name} — ${s.approverLabel}`,
        }));
        if (fromForm.length) {
            return fromForm;
        }
        return [
            { order: 1, label: '1. Initiator' },
            ...this.thresholdApprovalSteps.map((s) => ({ order: s.order, label: s.label })),
        ];
    }

    getThresholdApprovalStepOptions(): { order: number; label: string }[] {
        if (this.isThresholdInsertRouting()) {
            return this.getThresholdAnchorStepOptions();
        }
        const fromForm = this.thresholdFormPipelineSteps
            .filter((s) => s.order > 1)
            .map((s) => ({
                order: s.order,
                label: `${s.order}. ${s.name} — ${s.approverLabel}`,
            }));
        if (fromForm.length) {
            return fromForm;
        }
        return this.thresholdApprovalSteps;
    }

    thresholdRoutingActionLabel(action: string | undefined): string {
        return THRESHOLD_ROUTING_ACTION_LABELS[action as ThresholdRoutingAction] || action || 'Override';
    }

    getThresholdRoutingSummary(rule: WorkflowPipelineThresholdRule): string {
        const action = rule.routingAction || 'override';
        const stepLabel = this.getThresholdApprovalStepLabel(rule.approvalStepOrder);
        const who = `${this.thresholdTargetTypeLabel(rule.targetType)}: ${rule.targetLabel}`;
        if (action === 'insert') {
            const pos = rule.insertPosition === 'before' ? 'before' : 'after';
            return `Insert ${pos} ${stepLabel} → ${who}`;
        }
        return `Override at ${stepLabel} → ${who}`;
    }

    getThresholdInsertPreviewOrder(): number | null {
        if (!this.isThresholdInsertRouting()) {
            return null;
        }
        const anchor = Number(this.thresholdRuleForm.get('approvalStepOrder')?.value);
        if (!Number.isFinite(anchor) || anchor < 1) {
            return null;
        }
        const pos = this.thresholdRuleForm.get('insertPosition')?.value as ThresholdInsertPosition;
        return pos === 'before' ? anchor : anchor + 1;
    }

    shouldShowThresholdInsertPreviewBefore(step: FormPipelinePreviewStep): boolean {
        const insertAt = this.getThresholdInsertPreviewOrder();
        return insertAt != null && step.order === insertAt && this.thresholdRuleForm.get('insertPosition')?.value === 'before';
    }

    shouldShowThresholdInsertPreviewAfter(step: FormPipelinePreviewStep): boolean {
        const insertAt = this.getThresholdInsertPreviewOrder();
        const pos = this.thresholdRuleForm.get('insertPosition')?.value;
        return insertAt != null && pos === 'after' && step.order === Number(this.thresholdRuleForm.get('approvalStepOrder')?.value);
    }

    getThresholdInsertPreviewLabel(): string {
        const v = this.thresholdRuleForm.value;
        const type = this.thresholdTargetTypeLabel(v.targetType);
        const who = v.targetLabel ? `${type}: ${v.targetLabel}` : '…';
        return `+ New step — ${who}`;
    }

    isThresholdPreviewStepHighlighted(step: FormPipelinePreviewStep): boolean {
        if (this.isThresholdInsertRouting()) {
            return false;
        }
        return step.order === Number(this.thresholdRuleForm.get('approvalStepOrder')?.value);
    }

    thresholdPreviewStepApprover(step: FormPipelinePreviewStep): string {
        if (this.isThresholdInsertRouting()) {
            return step.approverLabel;
        }
        const selected = Number(this.thresholdRuleForm.get('approvalStepOrder')?.value);
        const v = this.thresholdRuleForm.value;
        if (step.order === selected && v.targetLabel) {
            const type = this.thresholdTargetTypeLabel(v.targetType);
            return `${type}: ${v.targetLabel} (rule override)`;
        }
        return step.approverLabel;
    }

    /** Same slug as application submit payload keys (`application.component.getFieldName`). */
    private thresholdFieldKeyFromLabel(label: string): string {
        return label
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, '_')
            .replace(/^_+|_+$/g, '');
    }

    thresholdOperatorLabel(operator: string): string {
        return THRESHOLD_OPERATOR_LABELS[operator as keyof typeof THRESHOLD_OPERATOR_LABELS] || operator;
    }

    thresholdTargetTypeLabel(type: string): string {
        return THRESHOLD_TARGET_TYPE_LABELS[type as keyof typeof THRESHOLD_TARGET_TYPE_LABELS] || type;
    }

    onThresholdFieldChanged(): void {
        const key = this.thresholdRuleForm.get('fieldKey')?.value;
        const field = this.thresholdFormFields.find((f) => f.key === key);
        if (field) {
            this.thresholdRuleForm.patchValue({ fieldLabel: field.label });
        }
    }

    onThresholdTargetTypeChanged(): void {
        this.thresholdRuleForm.patchValue({ targetId: null, targetLabel: '' });
    }

    onThresholdTargetChanged(): void {
        const type = this.thresholdRuleForm.get('targetType')?.value as ThresholdTargetType;
        const id = Number(this.thresholdRuleForm.get('targetId')?.value);
        if (!Number.isFinite(id) || id <= 0) {
            this.thresholdRuleForm.patchValue({ targetLabel: '' });
            return;
        }
        let label = '';
        if (type === 'department') {
            const dept = this.departments.find((d) => Number(d.serDepartmentId) === id);
            label = String(dept?.txtDepartmentName || '').trim();
        } else if (type === 'role') {
            const role = this.roles.find((r) => Number(r.serRoleId) === id);
            label = roleDisplayName(role);
        } else if (type === 'individual') {
            const user = this.allUsers.find((u) => Number(u.serUserId ?? u.userId ?? u.id) === id);
            label = userDisplayName(user);
        }
        this.thresholdRuleForm.patchValue({ targetLabel: label });
    }

    private resolveThresholdTargetSelection(
        targetType: ThresholdTargetType,
        targetLabel: string,
        serTargetId?: number
    ): { targetId: number | null; label: string } {
        if (serTargetId != null && serTargetId > 0) {
            return { targetId: serTargetId, label: String(targetLabel || '').trim() };
        }
        const normalized = String(targetLabel || '').trim().toLowerCase();
        if (!normalized) {
            return { targetId: null, label: '' };
        }
        if (targetType === 'department') {
            const dept = this.departments.find(
                (d) => String(d.txtDepartmentName || '').trim().toLowerCase() === normalized
            );
            if (dept) {
                return { targetId: Number(dept.serDepartmentId), label: String(dept.txtDepartmentName).trim() };
            }
        }
        if (targetType === 'role') {
            const role = this.roles.find((r) => roleDisplayName(r).toLowerCase() === normalized);
            if (role) {
                return { targetId: Number(role.serRoleId), label: roleDisplayName(role) };
            }
        }
        if (targetType === 'individual') {
            const user = this.allUsers.find((u) => userDisplayName(u).toLowerCase() === normalized);
            if (user) {
                const id = Number(user.serUserId ?? user.userId ?? user.id);
                return { targetId: id, label: userDisplayName(user) };
            }
        }
        return { targetId: null, label: String(targetLabel).trim() };
    }

    getThresholdApprovalStepLabel(order: number): string {
        const fromPipeline = this.thresholdFormPipelineSteps.find((s) => s.order === order);
        if (fromPipeline) {
            return `${fromPipeline.order}. ${fromPipeline.name}`;
        }
        const match = this.thresholdApprovalSteps.find((s) => s.order === order);
        return match?.label || `Approval step ${order}`;
    }

    getThresholdPreviewConditionText(): string {
        const v = this.thresholdRuleForm.value;
        const op = this.thresholdOperatorLabel(v.operator);
        return `IF "${v.fieldLabel || v.fieldKey}" ${op} "${v.value || '…'}"`;
    }

    getThresholdPreviewEffectText(): string {
        const v = this.thresholdRuleForm.value;
        const type = this.thresholdTargetTypeLabel(v.targetType);
        const who = `${type}: ${v.targetLabel || '…'}`;
        const stepLabel = this.getThresholdApprovalStepLabel(Number(v.approvalStepOrder));
        if (v.routingAction === 'insert') {
            const pos = v.insertPosition === 'before' ? 'before' : 'after';
            return `THEN insert new approval step ${pos} ${stepLabel} → ${who} (later steps shift down)`;
        }
        return `THEN override at ${stepLabel} → approver becomes ${who}`;
    }

    wouldThresholdPreviewMatch(): boolean | null {
        const sample = this.thresholdPreviewSampleValue.trim();
        const expected = String(this.thresholdRuleForm.get('value')?.value ?? '').trim();
        const operator = this.thresholdRuleForm.get('operator')?.value as ThresholdOperator;
        if (!sample || !expected) {
            return null;
        }
        if (operator === 'equals') {
            return sample.toLowerCase() === expected.toLowerCase();
        }
        if (operator === 'not_equals') {
            return sample.toLowerCase() !== expected.toLowerCase();
        }
        if (operator === 'contains') {
            return sample.toLowerCase().includes(expected.toLowerCase());
        }
        if (operator === 'greater_than' || operator === 'less_than') {
            const a = Number(sample.replace(/,/g, ''));
            const b = Number(expected.replace(/,/g, ''));
            if (!Number.isFinite(a) || !Number.isFinite(b)) {
                return null;
            }
            return operator === 'greater_than' ? a > b : a < b;
        }
        if (operator === 'in_list') {
            const parts = expected.split(',').map((p) => p.trim().toLowerCase()).filter(Boolean);
            return parts.includes(sample.toLowerCase());
        }
        return null;
    }

    onNewThresholdRule(): void {
        if (!this.thresholdForms.length && !this.thresholdFormsLoading) {
            this.loadThresholdForms();
        }
        this.editingThresholdId = null;
        this.thresholdRuleSubmit = false;
        this.thresholdPreviewSampleValue = '';
        this.thresholdFormFields = [];
        this.thresholdFormPipelineSteps = [];
        this.thresholdRuleForm.reset({
            ruleName: '',
            serFormId: null,
            fieldKey: '',
            fieldLabel: '',
            operator: 'greater_than',
            value: '',
            targetType: 'department',
            targetId: null,
            targetLabel: '',
            routingAction: 'override',
            insertPosition: 'after',
            approvalStepOrder: 3,
            priority: 10,
            active: true,
            notes: '',
        });
        this.thresholdRuleModal?.open();
    }

    onEditThresholdRule(row: WorkflowPipelineThresholdRule): void {
        if (!this.thresholdForms.length && !this.thresholdFormsLoading) {
            this.loadThresholdForms();
        }
        this.editingThresholdId = row.id;
        this.thresholdRuleSubmit = false;
        this.thresholdPreviewSampleValue = row.value;
        const serFormId = row.serFormId ?? null;
        this.thresholdFormFields = [];
        this.thresholdFormPipelineSteps = [];
        const resolved = this.resolveThresholdTargetSelection(row.targetType, row.targetLabel, row.serTargetId);
        this.thresholdRuleForm.reset({
            ruleName: row.ruleName,
            serFormId,
            fieldKey: row.fieldKey,
            fieldLabel: row.fieldLabel,
            operator: row.operator,
            value: row.value,
            targetType: row.targetType,
            targetId: resolved.targetId,
            targetLabel: resolved.label || row.targetLabel,
            routingAction: row.routingAction || 'override',
            insertPosition: row.insertPosition || 'after',
            approvalStepOrder: row.approvalStepOrder,
            priority: row.priority,
            active: row.active,
            notes: row.notes || '',
        });
        if (serFormId) {
            this.loadThresholdFormFields(serFormId, row.fieldKey, row.approvalStepOrder);
        }
        this.thresholdRuleModal?.open();
    }

    closeThresholdRuleModal(): void {
        this.thresholdRuleModal?.close();
    }

    saveThresholdRule(): void {
        this.thresholdRuleSubmit = true;
        if (this.thresholdRuleForm.invalid) {
            return;
        }
        this.onThresholdTargetChanged();
        const v = this.thresholdRuleForm.value;
        const serTargetId = Number(v.targetId);
        if (!Number.isFinite(serTargetId) || serTargetId <= 0) {
            this.notificationService.showMessage('Please select an approver.', 'error');
            return;
        }
        const payload: WorkflowThresholdRuleSavePayload = {
            ruleName: String(v.ruleName).trim(),
            serFormId: Number(v.serFormId),
            fieldKey: v.fieldKey,
            fieldLabel: v.fieldLabel,
            operator: v.operator,
            value: String(v.value).trim(),
            targetType: v.targetType,
            serTargetId,
            targetLabel: String(v.targetLabel).trim(),
            routingAction: (v.routingAction || 'override') as ThresholdRoutingAction,
            insertPosition:
                v.routingAction === 'insert'
                    ? ((v.insertPosition || 'after') as ThresholdInsertPosition)
                    : undefined,
            approvalStepOrder: Number(v.approvalStepOrder),
            priority: Number(v.priority),
            active: !!v.active,
            notes: v.notes ? String(v.notes).trim() : undefined,
        };
        this.thresholdRuleSaving = true;
        const request$ =
            this.editingThresholdId != null
                ? this.workflowThresholdRuleService.update({ ...payload, id: this.editingThresholdId! })
                : this.workflowThresholdRuleService.create(payload);
        request$.subscribe({
            next: (saved) => {
                this.thresholdRuleSaving = false;
                if (this.editingThresholdId != null) {
                    this.thresholdRows = this.thresholdRows.map((r) => (r.id === saved.id ? saved : r));
                    this.notificationService.showMessage('Rule updated.', 'success');
                } else {
                    this.thresholdRows = [saved, ...this.thresholdRows];
                    this.notificationService.showMessage('Rule created.', 'success');
                }
                this.closeThresholdRuleModal();
            },
            error: (err) => {
                this.thresholdRuleSaving = false;
                const msg = err?.error?.message || err?.message || 'Could not save rule.';
                this.notificationService.showMessage(msg, 'error');
            },
        });
    }
}
