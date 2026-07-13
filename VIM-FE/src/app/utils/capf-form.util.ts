/**
 * Display value for the CAPF # field on the printed/electronic CAPF form.
 * The field label already includes "CAPF #", so strip a leading "CAPF-" / "CAPF " prefix.
 */
export function formatCapfFormNumberDisplay(value: string | null | undefined): string {
  const raw = (value ?? '').toString().trim();
  if (!raw) {
    return '';
  }
  return raw.replace(/^CAPF[-\s#]*/i, '');
}

/** Pipeline orders 1–4 map to departmental slots; order 5+ maps to the Chief Executive signature column. */
export const CAPF_CEO_SIGNATURE_PIPELINE_ORDER = 5;

export function getPipelineDepartmentName(pipeline: any): string {
  return (
    pipeline?.hrTblDepartment?.txtDepartmentName ||
    pipeline?.departmentName ||
    pipeline?.txtDepartmentName ||
    ''
  ).toString();
}

export function resolveCapfPipelineOrder(pipeline: any, sortedIndex: number): number {
  const order = Number(pipeline?.intApprovalOrder);
  return !isNaN(order) && order > 0 ? order : sortedIndex + 1;
}

export function isCapfPipelineCeoSignatureStage(pipeline: any, sortedIndex: number): boolean {
  return resolveCapfPipelineOrder(pipeline, sortedIndex) >= CAPF_CEO_SIGNATURE_PIPELINE_ORDER;
}

export function capfHasPipelineCeoSignatureSlot(pipelines: any[] | null | undefined): boolean {
  if (!Array.isArray(pipelines) || pipelines.length === 0) {
    return false;
  }
  const sorted = [...pipelines].sort(
    (a, b) => resolveCapfPipelineOrder(a, 0) - resolveCapfPipelineOrder(b, 0)
  );
  return sorted.some((pipeline, index) => isCapfPipelineCeoSignatureStage(pipeline, index));
}
