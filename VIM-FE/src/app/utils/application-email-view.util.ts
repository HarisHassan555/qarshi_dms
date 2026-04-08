import { urls } from './urls';

/** Field list shape expected by {@link AbcComponent}. */
export function normalizeFormFields(cfgFields: any[] | null | undefined): any[] {
  if (!Array.isArray(cfgFields) || cfgFields.length === 0) return [];
  return cfgFields
    .map((field: any) => ({
      serFieldId: field.serFieldId,
      label: field.txtFieldLabel,
      type: field.txtFieldType,
      required: field.blIsRequired || false,
      placeholder: field.txtPlaceholder || '',
      intFieldOrder: field.intFieldOrder || 0,
      txtFieldOptions: field.txtFieldOptions,
    }))
    .sort((a: any, b: any) => (a.intFieldOrder || 0) - (b.intFieldOrder || 0));
}

export function parseApplicationFormData(txtApplicationData: string | null | undefined): Record<string, any> {
  const rawData = txtApplicationData || '{}';
  try {
    let parsed: any = JSON.parse(rawData);
    if (typeof parsed === 'string') {
      try {
        parsed = JSON.parse(parsed);
      } catch {
        parsed = { content: parsed };
      }
    }
    return parsed?.appData || parsed || {};
  } catch {
    return { content: rawData };
  }
}

export function parseApprovalHistory(txtApprovalHistory: string | null | undefined): any[] {
  if (!txtApprovalHistory) return [];
  try {
    const parsed = JSON.parse(txtApprovalHistory);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/**
 * Detect CAPF / post-CAPF assignment views (matches application-details.isCapfForm
 * without requiring the full forms catalog).
 */
export function isCapfLike(app: any, approvalHistory: any[]): boolean {
  if (!app) return false;
  const appCode = (app.txtFormCode || '').toUpperCase();
  if (appCode.includes('CAPF')) return true;
  if (app.isCapfForm === true) return true;
  const name = (app.cfgTblCustomForm?.txtFormName || app.formName || '')
    .replace(/\s+/g, ' ')
    .toUpperCase();
  const code = (app.cfgTblCustomForm?.txtFormCode || '').toUpperCase();
  if (
    name.includes('CAPITAL ASSETS PURCHASE') ||
    name.includes('CAPF') ||
    code.includes('CAPF')
  ) {
    return true;
  }
  const status = (app.txtStatus || '').toString().toUpperCase();
  if (status === 'CEO_PENDING' || status === 'ASSET_PENDING') return true;
  const currentLevel = app.intCurrentApprovalLevel;
  if (typeof currentLevel === 'number' && currentLevel < 0) return true;
  if (app.txtAssetCode || app.txtPrCode) return true;
  if (Array.isArray(approvalHistory) && approvalHistory.length > 0) {
    const hasCapfStyleEntry = approvalHistory.some((e: any) => {
      const action = (e?.action || e?.status || '').toString().toUpperCase();
      const role = (e?.role || e?.designation || e?.txtDesignation || e?.departmentName || '')
        .toString()
        .toUpperCase();
      return (
        action === 'PR_CODE_ASSIGNED' ||
        role.includes('CEO') ||
        role.includes('FINANCE')
      );
    });
    if (hasCapfStyleEntry) return true;
  }
  return false;
}

export function getApprovalLogRows(approvalHistory: any[] | null | undefined): any[] {
  if (!Array.isArray(approvalHistory) || approvalHistory.length === 0) return [];
  return approvalHistory;
}

export function getApprovalLogLevel(entry: any): string {
  const level = entry?.level ?? entry?.intApprovalOrder;
  return level != null && String(level).trim() !== '' ? String(level) : '--';
}

export function getApprovalLogApprover(entry: any, userNameMap: Map<number, string>): string {
  if (!entry) return '--';
  const direct = entry?.approverName || entry?.approvedByName || entry?.userName;
  if (direct && String(direct).trim() !== '') return String(direct);
  const id = entry?.approvedBy ?? entry?.approverUserId ?? entry?.userId;
  const n = id != null ? Number(id) : NaN;
  if (!isNaN(n)) {
    return (userNameMap.get(n) as string) || `User ${n}`;
  }
  return '--';
}

export function getApprovalLogRole(entry: any): string {
  const role = entry?.role || entry?.departmentName || entry?.txtDepartmentName;
  return role && String(role).trim() !== '' ? String(role) : '--';
}

export function getApprovalLogStatus(entry: any): string {
  const action = entry?.action || entry?.status;
  return action && String(action).trim() !== '' ? String(action) : '--';
}

export function getApprovalLogDate(entry: any): string {
  const date = entry?.approvedDate || entry?.sentBackDate;
  return date && String(date).trim() !== '' ? String(date) : '--';
}

export function getApprovalLogRemarks(entry: any): string {
  const r = entry?.remarks;
  return r != null && String(r).trim() !== '' ? String(r) : '--';
}

export function getApprovalLogSignatureUrl(entry: any): string {
  if (!entry || !entry?.signaturePath) return '';
  const id = entry?.approvedBy ?? entry?.approverUserId ?? entry?.userId;
  const n = id != null ? Number(id) : NaN;
  if (isNaN(n) || n <= 0) return '';
  return `${urls.API_URL}getSignature?userId=${n}`;
}
