import {
    WorkflowPipelineStep,
    pipelineStepRowLabel,
    pipelineStepTargetLabel,
} from './workflow-pipeline-step.util';

export interface FormPipelinePreviewStep {
    order: number;
    name: string;
    approverLabel: string;
    stepType: string;
}

export function parseJsonApprovalPipelineSteps(form: Record<string, unknown> | null | undefined): any[] {
    const raw = form?.['txtApprovalPipeline'];
    if (typeof raw !== 'string' || !raw.trim()) {
        return [];
    }
    try {
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    } catch {
        return [];
    }
}

export function approvalPipelinesAsRawSteps(form: Record<string, unknown> | null | undefined): any[] {
    const list =
        (form?.['approvalPipelines'] as any[]) ||
        (form?.['cfgTblCustomFormApprovalPipelines'] as any[]) ||
        [];
    if (!Array.isArray(list)) {
        return [];
    }
    return list
        .map((p) => ({
            type: 'department',
            intApprovalOrder: p?.intApprovalOrder,
            serDepartmentId: p?.serDepartmentId ?? p?.hrTblDepartment?.serDepartmentId,
            txtDepartmentName: p?.hrTblDepartment?.txtDepartmentName,
            hrTblDepartment: p?.hrTblDepartment,
            stageName: p?.hrTblDepartment?.txtDepartmentName,
        }))
        .filter((p) => p.serDepartmentId != null || p.txtDepartmentName);
}

function backendStepName(step: any): string {
    return String(
        step?.stageName || step?.name || step?.txtDepartmentName || step?.departmentName || step?.roleName || 'Approval step'
    ).trim();
}

function backendStepApproverLabel(step: any): string {
    const type = String(step?.type || '').toLowerCase();
    if (step?.dynamicTarget === 'initiator_hod') {
        return 'Self HOD';
    }
    if (step?.dynamicTarget === 'initiator' || step?.txtUserName === 'Initiator') {
        return 'Initiator';
    }
    if (type === 'individual') {
        return String(step?.txtUserName || step?.hrTblUser?.txtUserName || step?.hrTblUser?.userName || 'Individual').trim();
    }
    if (type === 'role') {
        return String(step?.roleName || step?.cfgTblRole?.txtRoleName || step?.name || 'Role').trim();
    }
    return String(step?.txtDepartmentName || step?.departmentName || step?.hrTblDepartment?.txtDepartmentName || 'Department').trim();
}

/** Steps from `txtApprovalPipeline` JSON or legacy `approvalPipelines` (initiator prepended). */
export function buildFormPipelinePreviewFromBackendSteps(rawSteps: any[]): FormPipelinePreviewStep[] {
    const sorted = [...(rawSteps || [])].sort(
        (a, b) => (Number(a?.intApprovalOrder) || 0) - (Number(b?.intApprovalOrder) || 0)
    );
    const out: FormPipelinePreviewStep[] = [
        { order: 1, name: 'Initiator', approverLabel: 'Submitter', stepType: 'initiator' },
    ];
    sorted.forEach((step, index) => {
        out.push({
            order: index + 2,
            name: backendStepName(step),
            approverLabel: backendStepApproverLabel(step),
            stepType: String(step?.type || 'department'),
        });
    });
    return out;
}

/** Template builder `payload.pipeline` (may already include initiator). */
export function buildFormPipelinePreviewFromTemplatePipeline(pipeline: any[]): FormPipelinePreviewStep[] {
    const list = Array.isArray(pipeline) ? pipeline : [];
    if (!list.length) {
        return [];
    }
    return list
        .map((step, index) => {
            const asWorkflow = step as WorkflowPipelineStep;
            const order = Number(step?.order) || index + 1;
            const name =
                String(step?.name || '').trim() ||
                (asWorkflow.type ? pipelineStepRowLabel({ ...asWorkflow, order }).replace(/^\d+\.\s*/, '') : 'Step');
            const approverLabel =
                asWorkflow.type === 'initiator'
                    ? 'Submitter'
                    : pipelineStepTargetLabel(asWorkflow) || name;
            return {
                order,
                name: String(step?.name || name).trim() || `Step ${order}`,
                approverLabel,
                stepType: String(step?.type || 'department'),
            };
        })
        .sort((a, b) => a.order - b.order);
}

export function isTemplateBuilderForm(form: Record<string, unknown> | null | undefined): boolean {
    return String(form?.['txtFormDescription'] || '').trim() === 'template-builder';
}
