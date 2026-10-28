export interface WorkflowPipelineListItem {
    id: number;
    name: string;
    code: string;
    description: string;
    pipelineType: string;
    stepCount: number;
    active: boolean;
    modifiedDate: string;
}

export const WORKFLOW_PIPELINE_DEMO_ROWS: WorkflowPipelineListItem[] = [
    {
        id: 1,
        name: 'Finance — 3 level approval',
        code: 'WF-FIN-01',
        description: 'Initiator → Department head → Finance manager',
        pipelineType: 'STANDARD',
        stepCount: 3,
        active: true,
        modifiedDate: '2026-03-10',
    },
    {
        id: 2,
        name: 'HR — standard routing',
        code: 'WF-HR-02',
        description: 'Initiator → HR → Final approver',
        pipelineType: 'STANDARD',
        stepCount: 2,
        active: true,
        modifiedDate: '2026-03-08',
    },
    {
        id: 3,
        name: 'Legacy CAPF style (draft)',
        code: '',
        description: 'Placeholder for migration from old forms',
        pipelineType: 'STANDARD',
        stepCount: 5,
        active: false,
        modifiedDate: '2026-02-20',
    },
];
