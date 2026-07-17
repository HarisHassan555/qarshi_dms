/** Logo and header branding for CAPF form variants. */
export const DEFAULT_CAPF_LOGO = 'assets/images/qarshi-logo.png';
export const DEFAULT_CAPF_BRAND_TITLE = 'Qarshi Industries (Pvt) Ltd.';

/** Matches original qarshi-logo (158x79) rendered at max-width 64px. */
export const CAPF_LOGO_DISPLAY_HEIGHT_PX = 32;
export const CAPF_LOGO_COMPACT_HEIGHT_PX = 18;
export const CAPF_LOGO_CSS_CLASS = 'ml-[5px] flex-none';

export function normalizeCapfFormName(formName?: string | null): string {
  return (formName || '').trim().toUpperCase();
}

export function resolveCapfLogoPath(formName?: string | null): string {
  const normalized = normalizeCapfFormName(formName);
  if (normalized === 'CAPF QU') {
    return 'assets/images/QU_logo.png';
  }
  if (normalized === 'CAPF QF') {
    return 'assets/images/QF_logo.png';
  }
  if (normalized === 'CAPF QRI') {
    return 'assets/images/QRI Logo.jpg.jpeg';
  }
  return DEFAULT_CAPF_LOGO;
}

export function resolveCapfBrandTitle(formName?: string | null): string {
  const normalized = normalizeCapfFormName(formName);
  if (normalized === 'CAPF QU') {
    return 'Qarshi University (Pvt) Ltd.';
  }
  if (normalized === 'CAPF QF') {
    return 'Qarshi Foundation (Pvt) Ltd.';
  }
  if (normalized === 'CAPF QRI') {
    return 'Qarshi Research International (Pvt) Ltd.';
  }
  if (normalized === 'CAPF QB') {
    return 'Qarshi Brands (Pvt) Ltd.';
  }
  return DEFAULT_CAPF_BRAND_TITLE;
}

export function resolveCapfApprovedByTitle(formName?: string | null): string {
  return normalizeCapfFormName(formName) === 'CAPF QU'
    ? 'Vice Chancellor'
    : 'Chief Executive';
}

export function resolveCapfLogoCssClass(): string {
  return CAPF_LOGO_CSS_CLASS;
}

export function resolveCapfLogoDisplayHeightPx(): number {
  return CAPF_LOGO_DISPLAY_HEIGHT_PX;
}

export function resolveCapfFormNameFromSources(
  formName?: string | null,
  application?: { cfgTblCustomForm?: { txtFormName?: string }; formName?: string } | null
): string {
  return formName
    || application?.cfgTblCustomForm?.txtFormName
    || application?.formName
    || '';
}
