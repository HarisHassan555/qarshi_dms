export type ThresholdOperator = 'equals' | 'not_equals' | 'greater_than' | 'less_than' | 'contains' | 'in_list';
export type ThresholdTargetType = 'department' | 'role' | 'individual';
export type ThresholdRoutingAction = 'override' | 'insert';
export type ThresholdInsertPosition = 'after' | 'before';

/** Runtime routing rule (no link to workflow pipeline library). */
export interface WorkflowPipelineThresholdRule {
    id: number;
    ruleName: string;
    serFormId?: number;
    fieldKey: string;
    fieldLabel: string;
    operator: ThresholdOperator;
    value: string;
    targetType: ThresholdTargetType;
    serTargetId?: number;
    targetLabel: string;
    /** Override: existing step to replace approver. Insert: anchor step (insert before/after). */
    routingAction?: ThresholdRoutingAction;
    insertPosition?: ThresholdInsertPosition;
    /** Step number in document workflow (1 = initiator). */
    approvalStepOrder: number;
    priority: number;
    active: boolean;
    notes?: string;
}

export const THRESHOLD_OPERATOR_LABELS: Record<ThresholdOperator, string> = {
    equals: 'Equals',
    not_equals: 'Not equals',
    greater_than: 'Greater than',
    less_than: 'Less than',
    contains: 'Contains',
    in_list: 'In list',
};

export const THRESHOLD_TARGET_TYPE_LABELS: Record<ThresholdTargetType, string> = {
    department: 'Department',
    role: 'Role',
    individual: 'Individual',
};

export const THRESHOLD_ROUTING_ACTION_LABELS: Record<ThresholdRoutingAction, string> = {
    override: 'Override approver at step',
    insert: 'Insert new approval step',
};

export const THRESHOLD_INSERT_POSITION_LABELS: Record<ThresholdInsertPosition, string> = {
    after: 'After',
    before: 'Before',
};

/** Form builder: `number`; template builder: `integer` / `number` / `decimal`. */
export function isThresholdNumericFieldType(fieldType: unknown): boolean {
    const normalized = String(fieldType || '')
        .trim()
        .toLowerCase()
        .replace(/\s+/g, '_');
    return normalized === 'number' || normalized === 'integer' || normalized === 'decimal';
}

export const THRESHOLD_DEMO_FORM_FIELDS: { key: string; label: string }[] = [
    { key: 'total_amount', label: 'Total Amount' },
    { key: 'expense_type', label: 'Expense Type' },
    { key: 'vendor_code', label: 'Vendor Code' },
    { key: 'requesting_department', label: 'Requesting Department' },
    { key: 'project_code', label: 'Project Code' },
];

export const THRESHOLD_APPROVAL_STEP_OPTIONS: { order: number; label: string }[] = [
    { order: 2, label: 'Approval step 2 (e.g. Department Head)' },
    { order: 3, label: 'Approval step 3' },
    { order: 4, label: 'Approval step 4' },
    { order: 5, label: 'Approval step 5' },
];

export const WORKFLOW_PIPELINE_THRESHOLD_DEMO_ROWS: WorkflowPipelineThresholdRule[] = [
    {
        id: 1,
        ruleName: 'High amount → Finance',
        fieldKey: 'total_amount',
        fieldLabel: 'Total Amount',
        operator: 'greater_than',
        value: '500000',
        targetType: 'department',
        targetLabel: 'Finance',
        routingAction: 'override',
        approvalStepOrder: 3,
        priority: 10,
        active: true,
        notes: 'On submit, if amount exceeds threshold, Finance approves at step 3.',
    },
    {
        id: 2,
        ruleName: 'CAPEX → CEO',
        fieldKey: 'expense_type',
        fieldLabel: 'Expense Type',
        operator: 'equals',
        value: 'CAPEX',
        targetType: 'role',
        targetLabel: 'CEO',
        routingAction: 'override',
        approvalStepOrder: 4,
        priority: 20,
        active: true,
    },
    {
        id: 3,
        ruleName: 'Vendor ABC → Procurement lead',
        fieldKey: 'vendor_code',
        fieldLabel: 'Vendor Code',
        operator: 'equals',
        value: 'V-ABC-001',
        targetType: 'individual',
        targetLabel: 'Mr. Ali Khan',
        routingAction: 'insert',
        insertPosition: 'after',
        approvalStepOrder: 2,
        priority: 15,
        active: true,
        notes: 'Extra procurement approval inserted after step 2 when vendor matches.',
    },
    {
        id: 4,
        ruleName: 'Department IT → Self HOD',
        fieldKey: 'requesting_department',
        fieldLabel: 'Requesting Department',
        operator: 'contains',
        value: 'Information Technology',
        targetType: 'department',
        targetLabel: 'Self HOD',
        routingAction: 'override',
        approvalStepOrder: 2,
        priority: 5,
        active: false,
        notes: 'Static preview — inactive rule example.',
    },
];
