export type DocumentShareChannelId =
    | 'velocity'
    | 'whatsapp'
    | 'outlook'
    | 'teams'
    | 'email'
    | 'linkedin'
    | 'copy'
    | 'more';

export interface DocumentShareChannel {
    id: DocumentShareChannelId;
    label: string;
    /** Primary public CDN icon URL. */
    iconUrl: string;
    /** Used when primary URL fails to load (offline CDN / blocked host). */
    iconFallbackUrl?: string;
    accent?: string;
}

const SIMPLE_ICONS_VERSION = '11.14.0';

function iconifySimpleIcon(slug: string): string {
    return `https://api.iconify.design/simple-icons/${slug}.svg`;
}

function jsDelivrSimpleIcon(slug: string): string {
    return `https://cdn.jsdelivr.net/npm/simple-icons@${SIMPLE_ICONS_VERSION}/icons/${slug}.svg`;
}

function iconifyMdi(name: string, colorHex: string): string {
    return `https://api.iconify.design/mdi/${name}.svg?color=%23${colorHex.replace('#', '')}`;
}

/** Brand and action icons loaded from public CDNs at runtime. */
export const DOCUMENT_SHARE_CHANNELS: DocumentShareChannel[] = [
    {
        id: 'velocity',
        label: 'Velocity',
        iconUrl: iconifyMdi('chat-processing', 'e11d48'),
        accent: '#e11d48',
    },
    {
        id: 'whatsapp',
        label: 'WhatsApp',
        iconUrl: iconifySimpleIcon('whatsapp'),
        iconFallbackUrl: jsDelivrSimpleIcon('whatsapp'),
        accent: '#25D366',
    },
    {
        id: 'outlook',
        label: 'Outlook',
        iconUrl: iconifySimpleIcon('microsoftoutlook'),
        iconFallbackUrl: jsDelivrSimpleIcon('microsoftoutlook'),
        accent: '#0078D4',
    },
    {
        id: 'teams',
        label: 'Teams',
        iconUrl: iconifySimpleIcon('microsoftteams'),
        iconFallbackUrl: jsDelivrSimpleIcon('microsoftteams'),
        accent: '#6264A7',
    },
    {
        id: 'email',
        label: 'Email',
        iconUrl: iconifyMdi('email', '64748b'),
        accent: '#64748b',
    },
    {
        id: 'linkedin',
        label: 'LinkedIn',
        iconUrl: iconifySimpleIcon('linkedin'),
        iconFallbackUrl: jsDelivrSimpleIcon('linkedin'),
        accent: '#0A66C2',
    },
    {
        id: 'copy',
        label: 'Copy link',
        iconUrl: iconifyMdi('link-variant', '334155'),
        accent: '#334155',
    },
    {
        id: 'more',
        label: 'More',
        iconUrl: iconifyMdi('dots-horizontal', '475569'),
        accent: '#475569',
    },
];

export function buildDocumentShareText(shareUrl: string, documentTitle?: string | null): string {
    const title = (documentTitle || '').trim();
    if (title) {
        return `View "${title}" (view-only PDF):\n${shareUrl}`;
    }
    return `View this shared document (view-only PDF):\n${shareUrl}`;
}

export function buildDocumentShareSubject(documentTitle?: string | null): string {
    const title = (documentTitle || '').trim();
    return title ? `Shared document: ${title}` : 'Shared document link';
}

export function openShareChannel(
    channelId: DocumentShareChannelId,
    shareUrl: string,
    documentTitle?: string | null
): 'opened' | 'copied' | 'native' | 'velocity' | 'unsupported' {
    const text = buildDocumentShareText(shareUrl, documentTitle);
    const subject = buildDocumentShareSubject(documentTitle);
    const encodedUrl = encodeURIComponent(shareUrl);
    const encodedText = encodeURIComponent(text);
    const encodedSubject = encodeURIComponent(subject);

    switch (channelId) {
        case 'velocity':
            return 'velocity';
        case 'whatsapp':
            window.open(`https://wa.me/?text=${encodedText}`, '_blank', 'noopener,noreferrer');
            return 'opened';
        case 'outlook':
            window.open(
                `https://outlook.office.com/mail/deeplink/compose?subject=${encodedSubject}&body=${encodedText}`,
                '_blank',
                'noopener,noreferrer'
            );
            return 'opened';
        case 'teams':
            window.open(
                `https://teams.microsoft.com/share?href=${encodedUrl}&msgText=${encodedText}`,
                '_blank',
                'noopener,noreferrer'
            );
            return 'opened';
        case 'email':
            window.location.href = `mailto:?subject=${encodedSubject}&body=${encodedText}`;
            return 'opened';
        case 'linkedin':
            window.open(
                `https://www.linkedin.com/sharing/share-offsite/?url=${encodedUrl}`,
                '_blank',
                'noopener,noreferrer'
            );
            return 'opened';
        case 'copy':
            return 'copied';
        case 'more':
            if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
                void navigator.share({ title: subject, text, url: shareUrl });
                return 'native';
            }
            return 'unsupported';
        default:
            return 'unsupported';
    }
}

export function canUseNativeShare(): boolean {
    return typeof navigator !== 'undefined' && typeof navigator.share === 'function';
}
