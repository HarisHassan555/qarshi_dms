export const DEFAULT_DOCUMENT_HEADER_LOGO = 'assets/images/qarshi-logo.png';
export const DEFAULT_DOCUMENT_HEADER_BRAND_TITLE = 'Qarshi Industries (Pvt) Ltd.';
export const QU_DOCUMENT_HEADER_LOGO = 'assets/images/QU_logo.png';
export const QU_DOCUMENT_HEADER_BRAND_TITLE = 'Qarshi University';
export const QF_DOCUMENT_HEADER_LOGO = 'assets/images/QF_logo.png';
export const QF_DOCUMENT_HEADER_BRAND_TITLE = 'Qarshi Foundation';
export const QRI_DOCUMENT_HEADER_LOGO = 'assets/images/QRI Logo.jpg.jpeg';
export const QRI_DOCUMENT_HEADER_BRAND_TITLE = 'Qarshi Research International';
export const QB_DOCUMENT_HEADER_BRAND_TITLE = 'Qarshi Brands';
export const DOCUMENT_HEADER_ADDRESS = '15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore';

export function normalizeDocumentHeaderType(fieldType?: string | null): string {
  return String(fieldType || '').trim().toLowerCase().replace(/\s+/g, '_');
}

export function isDocumentHeaderFieldType(fieldType?: string | null): boolean {
  const normalized = normalizeDocumentHeaderType(fieldType);
  return normalized === 'document_header'
    || normalized === 'document_header_qu'
    || normalized === 'document_header_qf'
    || normalized === 'document_header_qri'
    || normalized === 'document_header_qb'
    || normalized === 'header';
}

export function resolveDocumentHeaderLogoPath(fieldType?: string | null): string {
  const normalized = normalizeDocumentHeaderType(fieldType);
  if (normalized === 'document_header_qu') {
    return QU_DOCUMENT_HEADER_LOGO;
  }
  if (normalized === 'document_header_qf') {
    return QF_DOCUMENT_HEADER_LOGO;
  }
  if (normalized === 'document_header_qri') {
    return QRI_DOCUMENT_HEADER_LOGO;
  }
  return DEFAULT_DOCUMENT_HEADER_LOGO;
}

export function resolveDocumentHeaderBrandTitle(fieldType?: string | null): string {
  const normalized = normalizeDocumentHeaderType(fieldType);
  if (normalized === 'document_header_qu') {
    return QU_DOCUMENT_HEADER_BRAND_TITLE;
  }
  if (normalized === 'document_header_qf') {
    return QF_DOCUMENT_HEADER_BRAND_TITLE;
  }
  if (normalized === 'document_header_qri') {
    return QRI_DOCUMENT_HEADER_BRAND_TITLE;
  }
  if (normalized === 'document_header_qb') {
    return QB_DOCUMENT_HEADER_BRAND_TITLE;
  }
  return DEFAULT_DOCUMENT_HEADER_BRAND_TITLE;
}
