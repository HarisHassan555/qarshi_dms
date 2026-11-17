export interface SavedDocumentTable {
    tableHtml: string;
    values: Record<string, string>;
}

export function isSavedDocumentTable(value: any): value is SavedDocumentTable {
    return !!value
        && typeof value === 'object'
        && !Array.isArray(value)
        && typeof value.tableHtml === 'string';
}

export function extractTopLevelTables(html: string): string[] {
    const doc = new DOMParser().parseFromString(String(html || ''), 'text/html');
    return topLevelTables(doc.body).map((table) => table.outerHTML);
}

export function replaceNthTable(html: string, index: number, tableHtml: string): string {
    const doc = new DOMParser().parseFromString(String(html || ''), 'text/html');
    const tables = topLevelTables(doc.body);
    const current = tables[index];
    const incoming = new DOMParser().parseFromString(String(tableHtml || ''), 'text/html').querySelector('table');
    if (!current || !incoming) {
        return String(html || '');
    }
    current.replaceWith(doc.importNode(incoming, true));
    return doc.body.innerHTML;
}

export function replaceDocumentTables(
    html: string,
    saved: Record<string, any> | null | undefined,
    editable: boolean
): { html: string; tables: Array<{ key: string; tableHtml: string }> } {
    const source = String(html || '');
    if (!/<table[\s>]/i.test(source)) {
        return { html: source, tables: [] };
    }
    const doc = new DOMParser().parseFromString(source, 'text/html');
    const store = saved || {};
    const extracted: Array<{ key: string; tableHtml: string }> = [];
    const documentTables = topLevelTables(doc.body).filter((table) => !table.closest('.inline-filled-value, .template-field'));
    documentTables.forEach((table, index) => {
        const key = `doc:${index}`;
        const savedTable = store[key];
        if (!editable) {
            replaceWithSavedTable(doc, table, savedTable, key, store);
            return;
        }
        extracted.push({ key, tableHtml: savedTableHtml(savedTable) || table.outerHTML });
        table.replaceWith(createTableHost(doc, key));
    });
    doc.querySelectorAll('.inline-filled-word-editor-value[data-field-id]').forEach((node) => {
        const host = node as HTMLElement;
        const fieldId = host.getAttribute('data-field-id') || '';
        if (!fieldId) {
            return;
        }
        topLevelTables(host).forEach((table, index) => {
            const key = `field:${fieldId}:${index}`;
            const savedTable = store[key];
            if (!editable) {
                replaceWithSavedTable(doc, table, savedTable, key, store);
                return;
            }
            extracted.push({ key, tableHtml: savedTableHtml(savedTable) || table.outerHTML });
            table.replaceWith(createTableHost(doc, key));
        });
    });
    return { html: doc.body.innerHTML, tables: extracted };
}

function topLevelTables(root: ParentNode): HTMLTableElement[] {
    return Array.from(root.querySelectorAll('table')).filter((table) => !table.parentElement?.closest('table'));
}

function savedTableHtml(value: any): string {
    return isSavedDocumentTable(value) ? value.tableHtml : '';
}

function createTableHost(doc: Document, key: string): HTMLDivElement {
    const host = doc.createElement('div');
    host.className = 'document-fillable-table-host';
    host.setAttribute('data-document-table-key', key);
    return host;
}

function replaceWithSavedTable(
    doc: Document,
    table: HTMLTableElement,
    savedTable: any,
    key: string,
    store: Record<string, any>
): void {
    const html = savedTableHtml(savedTable);
    if (html) {
        const incoming = new DOMParser().parseFromString(html, 'text/html').querySelector('table');
        if (incoming) {
            table.replaceWith(doc.importNode(incoming, true));
            return;
        }
    }
    applyLegacyCellText(table, key, store);
}

function applyLegacyCellText(table: HTMLTableElement, key: string, store: Record<string, any>): void {
    const match = /^(.*):(\d+)$/.exec(key);
    if (!match) {
        return;
    }
    const prefix = `${match[1]}:t${match[2]}`;
    Array.from(table.rows).forEach((row, rowIndex) => {
        Array.from(row.cells).forEach((cell, cellIndex) => {
            const cellKey = `${prefix}r${rowIndex}c${cellIndex}`;
            if (typeof store[cellKey] === 'string') {
                cell.textContent = store[cellKey];
            }
        });
    });
}
