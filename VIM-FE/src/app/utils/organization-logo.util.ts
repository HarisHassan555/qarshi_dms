import { urls } from 'src/app/utils/urls';

/** Resolve stored logo value to a URL the browser can load (Angular assets or API upload). */
export function organizationLogoDisplayUrl(
    logo: string | null | undefined,
    serOrganizationId?: number | null
): string {
    const raw = String(logo || '').trim();
    if (!raw) {
        return '';
    }
    if (/^https?:\/\//i.test(raw) || raw.startsWith('data:')) {
        return raw;
    }
    if (raw.startsWith('org-logos/') && serOrganizationId) {
        return `${urls.API_URL}getOrganizationLogo?serOrganizationId=${serOrganizationId}`;
    }
    const normalized = raw.replace(/^\/+/, '');
    if (normalized.startsWith('assets/')) {
        return normalized;
    }
    return `assets/${normalized}`;
}
