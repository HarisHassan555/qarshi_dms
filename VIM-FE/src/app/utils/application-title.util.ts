export function resolveApplicationTitle(application: any): string {
    const data = parseApplicationData(application?.txtApplicationData);
    const fieldMatch = findTitleField(application, data);
    const candidates = fieldMatch ? getFieldValueCandidates(fieldMatch, data) : getDirectTitleCandidates(data);

    for (const candidate of candidates) {
        const normalized = normalizeDisplayValue(candidate);
        if (normalized) {
            return normalized;
        }
    }

    return '';
}

function parseApplicationData(raw: any): any {
    if (!raw) {
        return {};
    }
    if (typeof raw === 'object') {
        return raw;
    }
    try {
        return JSON.parse(String(raw));
    } catch {
        return {};
    }
}

function findTitleField(application: any, data: any): any | null {
    const fields = [
        ...normalizeFields(application?.cfgTblCustomForm?.cfgTblCustomFormFields),
        ...normalizeFields(application?.template?.payload?.fields),
        ...normalizeFields(data?.templatePayload?.fields),
        ...normalizeFields(data?.fields)
    ];

    return fields.find((field) => normalizeLabel(getFieldLabel(field)) === 'title') || null;
}

function normalizeFields(fields: any): any[] {
    return Array.isArray(fields) ? fields.filter((field) => !!field) : [];
}

function getFieldLabel(field: any): string {
    return String(field?.label || field?.txtFieldLabel || field?.name || field?.key || '').trim();
}

function getFieldValueCandidates(field: any, data: any): any[] {
    const label = getFieldLabel(field);
    const id = String(field?.id || field?.fieldId || '').trim();
    const slug = slugify(label);
    const templateValues = data?.templateValues || {};

    return [
        id ? templateValues[id] : undefined,
        id ? data[id] : undefined,
        label ? data[label] : undefined,
        slug ? data[slug] : undefined,
        label ? templateValues[label] : undefined,
        slug ? templateValues[slug] : undefined
    ];
}

function getDirectTitleCandidates(data: any): any[] {
    return [
        data?.Title,
        data?.title,
        data?.templateValues?.Title,
        data?.templateValues?.title
    ];
}

function normalizeDisplayValue(value: any): string {
    if (value === null || value === undefined) {
        return '';
    }
    if (Array.isArray(value) || typeof value === 'object') {
        return '';
    }
    return String(value).trim();
}

function normalizeLabel(value: string): string {
    return value.trim().toLowerCase().replace(/\s+/g, ' ');
}

function slugify(value: string): string {
    return value.toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
}
