import { stripEditorTableChromeFromHtml } from 'src/app/utils/word-editor-table.util';
import {
    evaluateTableFieldFormulas,
    extractTableCellValuesFromHtml,
    finalizeTableFormulaHtml,
    formatExcelColumnLabel,
    getFormulaDefinitionFromCell,
    isExcelFormulaComplete,
    isExcelFormulaText,
    isFormulaTableCell,
    parseExcelFormulaToDefinition,
    TableFormulaDefinition,
    TEMPLATE_TABLE_FORMULA_ATTR,
} from 'src/app/utils/template-table-formula.util';

const TABLE_MIN_COLUMN_WIDTH = 56;
const TABLE_MIN_ROW_HEIGHT = 36;
const TABLE_VERTICAL_CHROME_HEIGHT = 36;
const TABLE_RENDER_HARD_MAX_ROWS = 80;
const TABLE_RENDER_HARD_MAX_COLUMNS = 30;

export interface TableFieldConfig {
    rows: number;
    columns: number;
    headers: string[];
}

export interface TableFieldLike {
    id: string;
    type?: string;
    tableHtml?: string;
    tableConfig?: TableFieldConfig;
    placement?: { x: number; y: number; width: number; height: number };
}

export interface TableRenderCell {
    key: string;
    tagName: 'td' | 'th';
    html: string;
    editable: boolean;
    formula: boolean;
    colspan: number;
    rowspan: number;
    className: string;
    style: string;
}

export interface TableRenderRow {
    cells: TableRenderCell[];
}

export function normalizeTableCellValues(value: any): Record<string, string> {
    if (!value || Array.isArray(value) || typeof value !== 'object') {
        return {};
    }
    return Object.keys(value).reduce((acc: Record<string, string>, key) => {
        acc[key] = String(value[key] ?? '');
        return acc;
    }, {});
}

export function normalizeTableHtml(value: any): string {
    const html = stripEditorTableChromeFromHtml(String(value || '').trim());
    if (/<table[\s>]/i.test(html)) {
        const host = document.createElement('div');
        host.innerHTML = html;
        host.querySelectorAll('[data-cke-filler], br[data-cke-filler]').forEach((node) => node.remove());
        host.querySelectorAll('.ck-widget__selection-handle, .ck-table-column-resizer, .ck-table-bogus-paragraph').forEach((node) => {
            if (!String(node.textContent || '').replace(/\u00a0/g, ' ').trim()) {
                node.remove();
            }
        });
        return host.innerHTML;
    }
    return /<table[\s>]/i.test(html)
        ? html
        : '<figure class="table"><table><tbody><tr><td>Header 1</td><td>Header 2</td></tr><tr><td></td><td></td></tr></tbody></table></figure>';
}

export function normalizeTableHtmlForField(field: TableFieldLike, value: any): string {
    const html = normalizeTableHtml(value ?? field.tableHtml);
    return trimTableHtmlForField(field, html);
}

function trimTableHtmlForField(field: TableFieldLike, html: string): string {
    const limits = getTableRenderLimits(field);
    const host = document.createElement('div');
    host.innerHTML = html;
    const table = host.querySelector('table');
    if (!table) {
        return html;
    }
    Array.from(table.querySelectorAll('tr')).forEach((row, rowIndex) => {
        if (rowIndex >= limits.rows) {
            row.remove();
            return;
        }
        let columnCount = 0;
        Array.from(row.children).forEach((cell) => {
            if (!['TD', 'TH'].includes(cell.tagName)) {
                cell.remove();
                return;
            }
            const colSpan = Math.max(1, Number((cell as HTMLTableCellElement).colSpan || cell.getAttribute('colspan') || 1));
            columnCount += Number.isFinite(colSpan) ? colSpan : 1;
            if (columnCount > limits.columns) {
                cell.remove();
            }
        });
    });
    return host.innerHTML || html;
}

function getTableRenderLimits(field: TableFieldLike): { rows: number; columns: number } {
    const placement = field.placement;
    if (!placement) {
        return { rows: TABLE_RENDER_HARD_MAX_ROWS, columns: TABLE_RENDER_HARD_MAX_COLUMNS };
    }
    const usableWidth = Math.max(0, Number(placement.width || 0) - 12);
    const usableHeight = Math.max(0, Number(placement.height || 0) - TABLE_VERTICAL_CHROME_HEIGHT);
    return {
        rows: Math.min(TABLE_RENDER_HARD_MAX_ROWS, Math.max(1, Math.floor(usableHeight / TABLE_MIN_ROW_HEIGHT))),
        columns: Math.min(TABLE_RENDER_HARD_MAX_COLUMNS, Math.max(1, Math.floor(usableWidth / TABLE_MIN_COLUMN_WIDTH))),
    };
}

export function prepareTableFieldOnLoad(field: TableFieldLike, storedValue: any): Record<string, string> {
    field.tableHtml = normalizeTableHtmlForField(field, field.tableHtml);
    const merged = {
        ...extractTableCellValuesFromHtml(field.tableHtml || ''),
        ...normalizeTableCellValues(storedValue),
    };
    return evaluateTableFieldFormulas(field.tableHtml || '', merged).values;
}

export function refreshTableFieldFormulaValues(field: TableFieldLike, cellValues: Record<string, string>): Record<string, string> {
    if (!field.tableHtml) {
        return cellValues;
    }
    return evaluateTableFieldFormulas(field.tableHtml, cellValues).values;
}

export function isTableFieldValueEmpty(field: TableFieldLike, value: any): boolean {
    const tableValues = normalizeTableCellValues(value);
    const rows = buildTableRenderRows(field, field.tableHtml || '');
    const editableRows = rows.filter((row) => row.cells.some((cell) => cell.editable));
    if (editableRows.length === 0) {
        return Object.values(tableValues).every((item) => !String(item || '').trim());
    }
    return editableRows.every((row) =>
        row.cells
            .filter((cell) => cell.editable)
            .every((cell) => !String(tableValues[cell.key] || '').trim())
    );
}

export function buildTableRenderRows(field: TableFieldLike, html: string): TableRenderRow[] {
    const normalized = normalizeTableHtmlForField(field, html);
    const host = document.createElement('div');
    host.innerHTML = normalized;
    const table = host.querySelector('table');
    if (!table) {
        return [];
    }
    return Array.from(table.querySelectorAll('tr'))
        .slice(0, TABLE_RENDER_HARD_MAX_ROWS)
        .map((row, rowIndex) => ({
            cells: Array.from(row.children)
                .filter((cell) => ['TD', 'TH'].includes(cell.tagName))
                .slice(0, TABLE_RENDER_HARD_MAX_COLUMNS)
                .map((cell, cellIndex) => buildTableRenderCell(cell as HTMLTableCellElement, rowIndex, cellIndex)),
        }));
}

export function getTableIndicatorColumnCount(field: TableFieldLike): string[] {
    const count = buildTableRenderRows(field, field.tableHtml || '').reduce((max, row) => Math.max(max, row.cells.length), 0);
    return Array.from({ length: count }, (_, index) => formatExcelColumnLabel(index + 1));
}

function buildTableRenderCell(cell: HTMLTableCellElement, rowIndex: number, cellIndex: number): TableRenderCell {
    const html = cleanTableCellHtml(cell.innerHTML);
    const key = `r${rowIndex}c${cellIndex}`;
    const hasFormula = isFormulaTableCell(cell);
    return {
        key,
        tagName: cell.tagName.toLowerCase() === 'th' ? 'th' : 'td',
        html,
        editable: hasFormula ? false : !hasMeaningfulTableCellContent(cell, html),
        formula: hasFormula,
        colspan: Math.max(1, Number(cell.getAttribute('colspan') || cell.colSpan || 1)),
        rowspan: Math.max(1, Number(cell.getAttribute('rowspan') || cell.rowSpan || 1)),
        className: cleanTableCellClass(cell.getAttribute('class') || ''),
        style: cleanTableCellStyle(cell.getAttribute('style') || ''),
    };
}

function hasMeaningfulTableCellContent(cell: HTMLTableCellElement, cleanedHtml?: string): boolean {
    const text = String(cell.textContent || '').replace(/\u00a0/g, ' ').trim();
    if (text && text !== '#ERR' && text !== '—' && text !== '-') {
        return true;
    }
    const html = String(cleanedHtml ?? cleanTableCellHtml(cell.innerHTML))
        .replace(/&nbsp;/gi, '')
        .replace(/<br\s*\/?>/gi, '')
        .replace(/<\/?p[^>]*>/gi, '')
        .replace(/<[^>]+>/g, '')
        .trim();
    return !!html;
}

function cleanTableCellHtml(value: any): string {
    const host = document.createElement('div');
    host.innerHTML = String(value || '');
    host.querySelectorAll('[data-cke-filler], br[data-cke-filler]').forEach((node) => node.remove());
    host.querySelectorAll('.ck-table-bogus-paragraph').forEach((node) => {
        const element = node as HTMLElement;
        if (!String(element.textContent || '').replace(/\u00a0/g, ' ').trim()) {
            element.remove();
        }
    });
    return host.innerHTML.trim();
}

function cleanTableCellClass(value: string): string {
    return String(value || '')
        .split(/\s+/)
        .filter((className) => className && !className.startsWith('ck-'))
        .join(' ');
}

function cleanTableCellStyle(value: string): string {
    return String(value || '')
        .split(';')
        .map((rule) => rule.trim())
        .filter((rule) => {
            if (!rule) {
                return false;
            }
            const property = rule.split(':')[0]?.trim().toLowerCase();
            return !!property && !property.startsWith('--ck-');
        })
        .join('; ');
}

export function applyFormulaDefinitionToTableHtml(field: TableFieldLike, formula: TableFormulaDefinition): string {
    const html = normalizeTableHtmlForField(field, field.tableHtml);
    const host = document.createElement('div');
    host.innerHTML = html;
    const table = host.querySelector('table');
    if (!table) {
        return html;
    }
    const row = table.querySelectorAll('tr')[formula.target.row - 1];
    const cell = row?.querySelectorAll('th, td')[formula.target.col - 1] as HTMLTableCellElement | undefined;
    if (!cell) {
        return html;
    }
    cell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(formula));
    cell.classList.add('template-formula-cell');
    return host.innerHTML;
}

export function clearFormulaAtCellKey(field: TableFieldLike, cellKey: string): string {
    const match = /^r(\d+)c(\d+)$/.exec(cellKey);
    if (!match) {
        return field.tableHtml || '';
    }
    const html = normalizeTableHtmlForField(field, field.tableHtml);
    const host = document.createElement('div');
    host.innerHTML = html;
    const table = host.querySelector('table');
    if (!table) {
        return html;
    }
    const row = table.querySelectorAll('tr')[Number(match[1])];
    const cell = row?.querySelectorAll('th, td')[Number(match[2])] as HTMLTableCellElement | undefined;
    if (!cell) {
        return html;
    }
    cell.removeAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
    cell.classList.remove('template-formula-cell');
    cell.removeAttribute('title');
    cell.innerHTML = '&nbsp;';
    return host.innerHTML;
}

export function getFormulaDefinitionForCellKey(field: TableFieldLike, cellKey: string): TableFormulaDefinition | null {
    const match = /^r(\d+)c(\d+)$/.exec(cellKey);
    if (!match) {
        return null;
    }
    const html = normalizeTableHtmlForField(field, field.tableHtml);
    const host = document.createElement('div');
    host.innerHTML = html;
    const table = host.querySelector('table');
    if (!table) {
        return null;
    }
    const row = table.querySelectorAll('tr')[Number(match[1])];
    const cell = row?.querySelectorAll('th, td')[Number(match[2])] as HTMLTableCellElement | undefined;
    if (!cell) {
        return null;
    }
    return getFormulaDefinitionFromCell(cell);
}

export function applyTableCellValueChange(
    field: TableFieldLike,
    cellValues: Record<string, string>,
    cellKey: string,
    value: string
): { values: Record<string, string>; tableHtml: string } {
    const nextValue = String(value ?? '');
    let tableHtml = field.tableHtml || '';
    const tableValues = { ...normalizeTableCellValues(cellValues) };

    if (isExcelFormulaText(nextValue) && isExcelFormulaComplete(nextValue)) {
        const match = /^r(\d+)c(\d+)$/.exec(cellKey);
        if (match) {
            const target = {
                tableIndex: 1,
                row: Number(match[1]) + 1,
                col: Number(match[2]) + 1,
            };
            const parsed = parseExcelFormulaToDefinition(nextValue, target);
            if (parsed) {
                tableHtml = finalizeTableFormulaHtml(
                    applyFormulaDefinitionToTableHtml({ ...field, tableHtml }, parsed),
                    { ...tableValues, [cellKey]: nextValue }
                );
            }
        }
    }
    tableValues[cellKey] = nextValue;
    field.tableHtml = tableHtml;
    const values = refreshTableFieldFormulaValues({ ...field, tableHtml }, tableValues);
    return { values, tableHtml };
}
