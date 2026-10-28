import { organizationLogoDisplayUrl } from 'src/app/utils/organization-logo.util';

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
export const QRI_DOCUMENT_HEADER_ADDRESS = '149, 1st Floor, Al Hassan Plaza, Near Jamia Ashrafia, Ferozpur Road, Lahore';

/**
 * Maps document header field type → hr_tbl_organization.txt_organization_code (SAP sync / master data).
 * Verified against velocity.hr_tbl_organization (SAP HCM upsert uses SapHcmOrgStructureSyncService.toCode).
 */
export const DOCUMENT_HEADER_FIELD_TYPE_TO_ORG_CODE: Record<string, string> = {
    document_header: 'QARSHI_INDUSTRIES_PVT_LTD',
    header: 'QARSHI_INDUSTRIES_PVT_LTD',
    document_header_qu: 'QARSHI_UNIVERSITY',
    document_header_qf: 'QARSHI_FOUNDATION_TRUST',
    document_header_qri: 'QARSHI_RESEARCH_INT_PVT',
    document_header_qb: 'QARSHI_BRANDS_SMC_PVT_LTD',
};

export interface DocumentHeaderBrandingSlice {
    logoPath: string;
    brandTitle: string;
    address: string;
    organizationId?: number;
}

let brandingCacheByFieldType: Record<string, DocumentHeaderBrandingSlice> = {};

export function applyDocumentHeaderBrandingCache(
    cache: Record<string, DocumentHeaderBrandingSlice>
): void {
    brandingCacheByFieldType = { ...cache };
}

export function clearDocumentHeaderBrandingCache(): void {
    brandingCacheByFieldType = {};
}

function getCachedBranding(fieldType?: string | null): DocumentHeaderBrandingSlice | null {
    const normalized = normalizeDocumentHeaderType(fieldType);
    const cached = brandingCacheByFieldType[normalized];
    if (!cached) {
        return null;
    }
    return cached;
}

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

function fallbackLogoPath(normalized: string): string {
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

function fallbackBrandTitle(normalized: string): string {
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

function fallbackAddress(normalized: string): string {
  if (normalized === 'document_header_qri') {
    return QRI_DOCUMENT_HEADER_ADDRESS;
  }
  return DOCUMENT_HEADER_ADDRESS;
}

export function resolveDocumentHeaderLogoPath(fieldType?: string | null): string {
  const normalized = normalizeDocumentHeaderType(fieldType);
  const cached = getCachedBranding(fieldType);
  if (cached?.logoPath) {
    return organizationLogoDisplayUrl(cached.logoPath, cached.organizationId);
  }
  return organizationLogoDisplayUrl(fallbackLogoPath(normalized));
}

export function resolveDocumentHeaderBrandTitle(fieldType?: string | null): string {
  const normalized = normalizeDocumentHeaderType(fieldType);
  const cached = getCachedBranding(fieldType);
  if (cached?.brandTitle) {
    return cached.brandTitle;
  }
  return fallbackBrandTitle(normalized);
}

export function resolveDocumentHeaderAddress(fieldType?: string | null): string {
  const normalized = normalizeDocumentHeaderType(fieldType);
  const cached = getCachedBranding(fieldType);
  if (cached?.address) {
    return cached.address;
  }
  return fallbackAddress(normalized);
}
