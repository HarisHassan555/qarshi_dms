import { REPORTS_SEMANTIC_MAP } from './reports-sample-corpus';

export interface ReportSearchDocument {
    id: string;
    serApplicationId?: number;
    filename: string;
    mime: string;
    type: string;
    department: string;
    uploadedBy: string;
    uploadedAt: string;
    amount: number;
    status: string;
    txtStatus?: string;
    text: string;
    hasPdf?: boolean;
    txtFormCode?: string;
}

export interface ReportSearchFacets {
    type: Set<string>;
    department: Set<string>;
    uploadedBy: Set<string>;
    status: Set<string>;
    dateFrom: string;
    dateTo: string;
    amountMin: string;
    amountMax: string;
}

export interface ParsedReportQuery {
    phrases: string[];
    keywords: string[];
    fields: Record<string, string>;
    amountMin: number | null;
    amountMax: number | null;
}

export interface ReportSearchMatchInfo {
    fields: string[];
    phrases: string[];
    keywords: string[];
}

export interface ReportSearchResult extends ReportSearchDocument {
    score: number;
    matches: ReportSearchMatchInfo;
}

export function buildApiSearchQuery(query: string, semantic: boolean): string {
    const trimmed = (query || '').trim();
    if (!trimmed || !semantic) {
        return trimmed;
    }
    const parsed = parseReportQuery(trimmed);
    const expanded = expandSemantic(parsed.keywords);
    const extras = expanded.filter((token) => !parsed.keywords.includes(token));
    if (!extras.length) {
        return trimmed;
    }
    return `${trimmed} ${extras.join(' ')}`.trim();
}

export function mapApiReportItem(item: any): ReportSearchResult {
    return {
        id: String(item?.id || item?.serApplicationId || ''),
        serApplicationId: Number(item?.serApplicationId || 0) || undefined,
        filename: String(item?.filename || item?.txtFormCode || 'Document'),
        mime: String(item?.mimeType || 'application/octet-stream'),
        type: String(item?.type || item?.templateName || 'Template'),
        department: String(item?.department || ''),
        uploadedBy: String(item?.uploadedBy || ''),
        uploadedAt: String(item?.uploadedAt || ''),
        amount: Number(item?.amount || 0),
        status: String(item?.status || item?.txtStatus || ''),
        txtStatus: String(item?.txtStatus || item?.status || ''),
        text: String(item?.text || ''),
        hasPdf: !!item?.hasPdf,
        txtFormCode: String(item?.txtFormCode || ''),
        score: 1,
        matches: { fields: [], phrases: [], keywords: [] },
    };
}

export function createEmptyFacets(): ReportSearchFacets {
    return {
        type: new Set(),
        department: new Set(),
        uploadedBy: new Set(),
        status: new Set(),
        dateFrom: '',
        dateTo: '',
        amountMin: '',
        amountMax: '',
    };
}

function tokenize(str: string): string[] {
    return (str || '')
        .toLowerCase()
        .replace(/[^\w\s'"]/g, ' ')
        .split(/\s+/)
        .filter((t) => t.length > 1);
}

function expandSemantic(tokens: string[]): string[] {
    const expanded = new Set(tokens);
    tokens.forEach((t) => {
        (REPORTS_SEMANTIC_MAP[t] || []).forEach((rel) => expanded.add(rel));
    });
    return [...expanded];
}

export function parseReportQuery(query: string): ParsedReportQuery {
    const phrases: string[] = [];
    const phraseRe = /"([^"]+)"/g;
    let match: RegExpExecArray | null;
    while ((match = phraseRe.exec(query))) {
        phrases.push(match[1].toLowerCase());
    }

    const remaining = query.replace(phraseRe, ' ').trim();
    const tokens = tokenize(remaining);
    const fields: Record<string, string> = {};
    const keywords: string[] = [];

    tokens.forEach((t) => {
        const colonIdx = t.indexOf(':');
        if (colonIdx > 0 && colonIdx < t.length - 1) {
            const field = t.slice(0, colonIdx);
            const val = t.slice(colonIdx + 1);
            if (['type', 'department', 'uploadedby', 'status'].includes(field)) {
                fields[field] = val;
                return;
            }
        }
        if (t.startsWith('-') && t.length > 1) {
            return;
        }
        keywords.push(t);
    });

    let amountMin: number | null = null;
    let amountMax: number | null = null;
    const amtMatch = query.match(/amount\s*([<>=]+)\s*(\d+)/i);
    if (amtMatch) {
        const op = amtMatch[1];
        const num = parseInt(amtMatch[2], 10);
        if (op.includes('>')) {
            amountMin = num;
        }
        if (op.includes('<')) {
            amountMax = num;
        }
    }

    return { phrases, keywords, fields, amountMin, amountMax };
}

function scoreDocument(
    doc: ReportSearchDocument,
    parsed: ParsedReportQuery,
    facets: ReportSearchFacets,
    semantic: boolean
): { score: number; matches: ReportSearchMatchInfo } {
    const docText = (
        doc.filename +
        ' ' +
        doc.text +
        ' ' +
        doc.type +
        ' ' +
        doc.department +
        ' ' +
        doc.uploadedBy
    ).toLowerCase();

    let score = 0;
    const matches: ReportSearchMatchInfo = { fields: [], phrases: [], keywords: [] };

    const mapField: Record<string, keyof ReportSearchDocument> = {
        type: 'type',
        department: 'department',
        uploadedby: 'uploadedBy',
        status: 'status',
    };

    Object.entries(parsed.fields).forEach(([field, val]) => {
        const key = mapField[field];
        const docVal = String(doc[key] || '').toLowerCase();
        if (docVal.includes(val.toLowerCase())) {
            score += 5;
            matches.fields.push(`${field}:${val}`);
        }
    });

    parsed.phrases.forEach((p) => {
        if (docText.includes(p)) {
            score += 10;
            matches.phrases.push(p);
        }
    });

    const searchTokens = semantic ? expandSemantic(parsed.keywords) : parsed.keywords;
    searchTokens.forEach((t) => {
        if (docText.includes(t)) {
            score += 2;
            if (!matches.keywords.includes(t)) {
                matches.keywords.push(t);
            }
        }
    });

    if (parsed.amountMin !== null && doc.amount < parsed.amountMin) {
        score = 0;
    }
    if (parsed.amountMax !== null && doc.amount > parsed.amountMax) {
        score = 0;
    }

    if (facets.type.size > 0 && !facets.type.has(doc.type)) {
        score = 0;
    }
    if (facets.department.size > 0 && !facets.department.has(doc.department)) {
        score = 0;
    }
    if (facets.uploadedBy.size > 0 && !facets.uploadedBy.has(doc.uploadedBy)) {
        score = 0;
    }
    if (facets.status.size > 0 && !facets.status.has(doc.status)) {
        score = 0;
    }
    if (facets.dateFrom && doc.uploadedAt < facets.dateFrom) {
        score = 0;
    }
    if (facets.dateTo && doc.uploadedAt > facets.dateTo) {
        score = 0;
    }
    if (facets.amountMin && doc.amount < parseFloat(facets.amountMin)) {
        score = 0;
    }
    if (facets.amountMax && doc.amount > parseFloat(facets.amountMax)) {
        score = 0;
    }

    return { score, matches };
}

export function runReportSearch(
    corpus: ReportSearchDocument[],
    query: string,
    facets: ReportSearchFacets,
    semantic: boolean,
    sort: string
): { results: ReportSearchResult[]; parsed: ParsedReportQuery; elapsedMs: number } {
    const started = performance.now();
    const parsed = parseReportQuery(query);
    const hasAnyQuery =
        query.trim().length > 0 || Object.keys(parsed.fields).length > 0;

    let results: ReportSearchResult[] = corpus.map((doc) => {
        const scored = scoreDocument(doc, parsed, facets, semantic);
        return { ...doc, score: scored.score, matches: scored.matches };
    });

    if (!hasAnyQuery) {
        results = results.map((r) => ({ ...r, score: 1 }));
    } else {
        results = results.filter((r) => r.score > 0);
    }

    if (sort === 'relevance') {
        results.sort((a, b) => b.score - a.score || a.filename.localeCompare(b.filename));
    } else if (sort === 'date-desc') {
        results.sort((a, b) => b.uploadedAt.localeCompare(a.uploadedAt));
    } else if (sort === 'date-asc') {
        results.sort((a, b) => a.uploadedAt.localeCompare(b.uploadedAt));
    } else if (sort === 'amount-desc') {
        results.sort((a, b) => b.amount - a.amount);
    } else if (sort === 'amount-asc') {
        results.sort((a, b) => a.amount - b.amount);
    }

    const elapsedMs = Math.max(1, performance.now() - started);
    return { results, parsed, elapsedMs };
}

export function facetCounts(
    source: ReportSearchResult[] | ReportSearchDocument[]
): Record<'type' | 'department' | 'uploadedBy' | 'status', Record<string, number>> {
    const counts = {
        type: {} as Record<string, number>,
        department: {} as Record<string, number>,
        uploadedBy: {} as Record<string, number>,
        status: {} as Record<string, number>,
    };
    source.forEach((doc) => {
        counts.type[doc.type] = (counts.type[doc.type] || 0) + 1;
        counts.department[doc.department] = (counts.department[doc.department] || 0) + 1;
        counts.uploadedBy[doc.uploadedBy] = (counts.uploadedBy[doc.uploadedBy] || 0) + 1;
        counts.status[doc.status] = (counts.status[doc.status] || 0) + 1;
    });
    return counts;
}

export function escapeHtml(s: string): string {
    return String(s ?? '').replace(/[&<>"']/g, (m) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m] || m)
    );
}

export function highlightHtml(
    raw: string,
    parsed: ParsedReportQuery,
    semantic: boolean
): string {
    let result = escapeHtml(raw);
    const tokens = semantic ? expandSemantic(parsed.keywords) : parsed.keywords;
    tokens.forEach((t) => {
        if (t.length < 2) {
            return;
        }
        const re = new RegExp(`\\b(${t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})\\b`, 'gi');
        result = result.replace(re, '<mark>$1</mark>');
    });
    parsed.phrases.forEach((p) => {
        const re = new RegExp(`(${p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
        result = result.replace(re, '<mark>$1</mark>');
    });
    return result;
}

export function truncateSnippet(
    text: string,
    parsed: ParsedReportQuery,
    semantic: boolean
): string {
    if (!text) {
        return '';
    }
    const tokens = semantic ? expandSemantic(parsed.keywords) : parsed.keywords;
    const lower = text.toLowerCase();
    let firstIdx = -1;
    tokens.forEach((t) => {
        const i = lower.indexOf(t);
        if (i !== -1 && (firstIdx === -1 || i < firstIdx)) {
            firstIdx = i;
        }
    });
    if (firstIdx === -1) {
        return text.slice(0, 220) + (text.length > 220 ? '…' : '');
    }
    const start = Math.max(0, firstIdx - 60);
    const end = Math.min(text.length, firstIdx + 180);
    return (start > 0 ? '…' : '') + text.slice(start, end) + (end < text.length ? '…' : '');
}
