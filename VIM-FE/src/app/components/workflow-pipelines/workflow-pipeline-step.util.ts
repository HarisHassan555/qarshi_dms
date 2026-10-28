export type WorkflowPipelineStepType = 'initiator' | 'department' | 'individual' | 'role';
export type WorkflowPipelineApprovalMode = 'AND' | 'OR';

export interface WorkflowPipelineStep {
    id: string;
    name: string;
    type: WorkflowPipelineStepType;
    approvalMode?: WorkflowPipelineApprovalMode;
    order?: number;
    dynamicTarget?: string;
    dynamicTargets?: string[];
    serDepartmentId?: number;
    hrTblDepartment?: Record<string, unknown>;
    serRoleId?: number;
    cfgTblRole?: Record<string, unknown>;
    users?: Record<string, unknown>[];
    userIds?: number[];
    fieldPermissions?: unknown[];
    fieldPermissionsConfigured?: boolean;
}

export const WORKFLOW_PIPELINE_STEP_TYPES: { value: Exclude<WorkflowPipelineStepType, 'initiator'>; label: string }[] = [
    { value: 'department', label: 'Department' },
    { value: 'individual', label: 'Individual' },
    { value: 'role', label: 'Role' },
];

export const SELF_HOD_DEPARTMENT_ID = -1;

export function createWorkflowStepId(): string {
    return `step-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function defaultInitiatorStep(): WorkflowPipelineStep {
    return {
        id: 'initiator',
        name: 'Initiator',
        type: 'initiator',
        approvalMode: 'OR',
        order: 1,
        dynamicTarget: 'initiator',
    };
}

export function ensureInitiatorStep(steps: WorkflowPipelineStep[]): WorkflowPipelineStep[] {
    const list = Array.isArray(steps) ? [...steps] : [];
    if (!list.some((s) => s?.type === 'initiator')) {
        list.unshift(defaultInitiatorStep());
    }
    return reorderWorkflowPipelineSteps(list);
}

export function reorderWorkflowPipelineSteps(steps: WorkflowPipelineStep[]): WorkflowPipelineStep[] {
    return steps.map((step, index) => ({ ...step, order: index + 1 }));
}

export function clonePipelineSteps(steps: WorkflowPipelineStep[] | undefined | null): WorkflowPipelineStep[] {
    if (!Array.isArray(steps)) {
        return ensureInitiatorStep([]);
    }
    try {
        return ensureInitiatorStep(JSON.parse(JSON.stringify(steps)) as WorkflowPipelineStep[]);
    } catch {
        return ensureInitiatorStep([]);
    }
}

export function departmentDisplayName(dept: Record<string, unknown> | null | undefined): string {
    if (!dept) {
        return '';
    }
    return String(dept['txtDepartmentName'] || dept['departmentName'] || dept['name'] || '').trim();
}

export function roleDisplayName(role: Record<string, unknown> | null | undefined): string {
    if (!role) {
        return '';
    }
    return String(role['txtRoleName'] || role['roleName'] || role['name'] || '').trim();
}

export function userDisplayName(user: Record<string, unknown> | null | undefined): string {
    if (!user) {
        return '';
    }
    const name = String(user['txtUserName'] || user['userName'] || user['name'] || '').trim();
    if (name) {
        return name;
    }
    const id = Number(user['serUserId'] ?? user['userId'] ?? user['id']);
    return Number.isFinite(id) && id > 0 ? `User #${id}` : 'User';
}

export function pipelineStepTargetLabel(step: WorkflowPipelineStep): string {
    if (!step) {
        return '';
    }
    if (step.type === 'initiator') {
        return 'Submitter';
    }
    if (step.dynamicTarget === 'initiator_hod') {
        return 'Self HOD';
    }
    if (step.type === 'department') {
        return departmentDisplayName(step.hrTblDepartment as Record<string, unknown>) || step.name;
    }
    if (step.type === 'role') {
        return roleDisplayName(step.cfgTblRole as Record<string, unknown>) || step.name;
    }
    const users = Array.isArray(step.users) ? step.users : [];
    const parts = users.map((u) => userDisplayName(u)).filter(Boolean);
    if (Array.isArray(step.dynamicTargets) && step.dynamicTargets.includes('initiator')) {
        parts.unshift('Initiator');
    }
    return parts.length ? parts.join(', ') : step.name;
}

export function pipelineStepRowLabel(step: WorkflowPipelineStep): string {
    const order = step.order ?? 0;
    const title = step.name || pipelineStepTargetLabel(step);
    const type = step.type === 'initiator' ? 'Initiator' : step.type;
    return `${order}. ${title} (${type})`;
}
