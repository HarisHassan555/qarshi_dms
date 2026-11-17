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
    shiftExcelFormulaReferencesForDeletion,
    shiftExcelFormulaReferencesForInsertion,
    TableFormulaDefinition,
    TEMPLATE_TABLE_FORMULA_ATTR,
} from 'src/app/utils/template-table-formula.util';

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

function getTableRenderLimits(_field: TableFieldLike): { rows: number; columns: number } {
    return { rows: TABLE_RENDER_HARD_MAX_ROWS, columns: TABLE_RENDER_HARD_MAX_COLUMNS };
}

/** Word-editor tables: treat cells as fill inputs (not static row-0 headers) so formulas and typing work. */
export function ensureWordEditorTableInputCells(tableHtml: string): string {
    const host = document.createElement('div');
    host.innerHTML = String(tableHtml || '');
    const table = host.querySelector('table');
    if (!table) {
        return String(tableHtml || '');
    }
    table.querySelectorAll('td, th').forEach((cell) => {
        if (isFormulaTableCell(cell)) {
            return;
        }
        cell.setAttribute('data-template-input-cell', 'true');
    });
    return host.innerHTML;
}

export function prepareTableFieldOnLoad(field: TableFieldLike, storedValue: any): Record<string, string> {
    field.tableHtml = normalizeTableHtmlForField(field, field.tableHtml);
    const merged = {
        ...extractTableCellValuesFromHtml(field.tableHtml || ''),
        ...normalizeTableCellValues(storedValue),
    };
    const evaluated = evaluateTableFieldFormulas(field.tableHtml || '', merged);
    field.tableHtml = finalizeTableFormulaHtml(field.tableHtml || '', evaluated.values);
    return evaluateTableFieldFormulas(field.tableHtml, evaluated.values).values;
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
        const authoredLabel = rowIndex === 0
            && !hasFormula
            && !cell.hasAttribute('data-template-input-cell')
            && hasMeaningfulTableCellContent(cell, html);
        return {
            key,
            tagName: cell.tagName.toLowerCase() === 'th' ? 'th' : 'td',
            html,
            editable: !hasFormula && !authoredLabel,
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

export type FillableTableStructureAction =
    | 'add-row-above'
    | 'add-row-below'
    | 'add-column-before'
    | 'add-column-after'
    | 'delete-row'
    | 'delete-column';

export function applyFillableTableStructureChange(
    field: TableFieldLike,
    cellValues: Record<string, string>,
    action: FillableTableStructureAction,
    index?: number
): { values: Record<string, string>; tableHtml: string } | null {
    const html = normalizeTableHtml(field.tableHtml);
    const host = document.createElement('div');
    host.innerHTML = html;
    const table = host.querySelector('table');
    if (!table) {
        return null;
    }
    const rows = tableRowElements(table);
    if (rows.length === 0) {
        return null;
    }
    const columnCount = rows.reduce((max, row) => Math.max(max, rowCells(row).length), 0);
    if (columnCount === 0) {
        return null;
    }
    if ((action === 'delete-row' && rows.length <= 1) || (action === 'delete-column' && columnCount <= 1)) {
        return null;
    }
    const addingRow = action === 'add-row-above' || action === 'add-row-below';
    const addingColumn = action === 'add-column-before' || action === 'add-column-after';
    if ((addingRow && rows.length >= TABLE_RENDER_HARD_MAX_ROWS)
        || (addingColumn && columnCount >= TABLE_RENDER_HARD_MAX_COLUMNS)) {
        return null;
    }

    const clickedRow = clampStructureIndex(index, rows.length);
    const clickedColumn = clampStructureIndex(index, columnCount);
    const insertRowAt = action === 'add-row-below' ? clickedRow + 1 : clickedRow;
    const insertColumnAt = action === 'add-column-after' ? clickedColumn + 1 : clickedColumn;

    if (addingRow) {
        shiftFormulaExpressions(host, 'row', insertRowAt + 1, 'insert');
        const row = document.createElement('tr');
        for (let column = 0; column < columnCount; column += 1) {
            row.appendChild(createEmptyInputCell());
        }
        const referenceRow = rows[insertRowAt];
        if (referenceRow?.parentElement) {
            referenceRow.parentElement.insertBefore(row, referenceRow);
        } else {
            (table.querySelector('tbody') || table).appendChild(row);
        }
        retargetFormulaCells(host);
    } else if (addingColumn) {
        shiftFormulaExpressions(host, 'column', insertColumnAt + 1, 'insert');
        const headerCells = rowCells(rows[0]);
        const headerIsLabel = headerCells.length > 0 && headerCells.every((cell) =>
            !cell.hasAttribute('data-template-input-cell') && !isFormulaTableCell(cell) && !!String(cell.textContent || '').trim()
        );
        rows.forEach((row, rowIndex) => {
            const cell = rowIndex === 0 && headerIsLabel
                ? createHeaderCell(`Header ${columnCount + 1}`)
                : createEmptyInputCell();
            const cells = rowCells(row);
            const reference = cells[insertColumnAt];
            if (reference) {
                row.insertBefore(cell, reference);
            } else {
                row.appendChild(cell);
            }
        });
        retargetFormulaCells(host);
    } else if (action === 'delete-row') {
        rows[clickedRow].remove();
        shiftFormulaExpressions(host, 'row', clickedRow + 1, 'delete');
        retargetFormulaCells(host);
    } else {
        rows.forEach((row) => {
            const cells = rowCells(row);
            if (cells[clickedColumn]) {
                cells[clickedColumn].remove();
            }
        });
        shiftFormulaExpressions(host, 'column', clickedColumn + 1, 'delete');
        retargetFormulaCells(host);
    }

    const structureIndex = addingRow ? insertRowAt : addingColumn ? insertColumnAt : action === 'delete-row' ? clickedRow : clickedColumn;
    const tableHtml = normalizeTableHtml(host.innerHTML);
    const remapped = remapCellValuesForStructure(normalizeTableCellValues(cellValues), action, structureIndex);
    field.tableHtml = finalizeTableFormulaHtml(tableHtml, remapped);
    const values = refreshTableFieldFormulaValues(field, remapped);
    return { values, tableHtml: field.tableHtml };
}

function createEmptyInputCell(): HTMLTableCellElement {
    const cell = document.createElement('td');
    cell.setAttribute('data-template-input-cell', 'true');
    cell.innerHTML = '&nbsp;';
    return cell;
}

function createHeaderCell(label: string): HTMLTableCellElement {
    const cell = document.createElement('td');
    cell.textContent = label;
    return cell;
}

function tableRowElements(table: HTMLTableElement): HTMLTableRowElement[] {
    return Array.from(table.querySelectorAll('tr'));
}

function rowCells(row: HTMLTableRowElement): HTMLTableCellElement[] {
    return Array.from(row.children).filter((cell) => cell.tagName === 'TD' || cell.tagName === 'TH') as HTMLTableCellElement[];
}

function clampStructureIndex(index: number | undefined, length: number): number {
    const requested = Number(index);
    if (!Number.isFinite(requested)) {
        return length - 1;
    }
    return Math.min(length - 1, Math.max(0, Math.round(requested)));
}

function shiftFormulaExpressions(
    host: HTMLElement,
    axis: 'row' | 'column',
    pivot: number,
    mode: 'insert' | 'delete'
): void {
    host.querySelectorAll(`[${TEMPLATE_TABLE_FORMULA_ATTR}]`).forEach((node) => {
        const cell = node as HTMLTableCellElement;
        const formula = getFormulaDefinitionFromCell(cell);
        if (!formula?.expression) {
            return;
        }
        formula.expression = mode === 'insert'
            ? shiftExcelFormulaReferencesForInsertion(formula.expression, axis, pivot)
            : shiftExcelFormulaReferencesForDeletion(formula.expression, axis, pivot);
        cell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(formula));
    });
}

function retargetFormulaCells(host: HTMLElement): void {
    host.querySelectorAll(`[${TEMPLATE_TABLE_FORMULA_ATTR}]`).forEach((node) => {
        const cell = node as HTMLTableCellElement;
        const formula = getFormulaDefinitionFromCell(cell);
        const table = cell.closest('table');
        const row = cell.parentElement as HTMLTableRowElement | null;
        if (!formula || !table || !row) {
            return;
        }
        const rowIndex = tableRowElements(table).indexOf(row);
        const columnIndex = rowCells(row).indexOf(cell);
        if (rowIndex < 0 || columnIndex < 0) {
            return;
        }
        formula.target = {
            ...formula.target,
            tableIndex: formula.target?.tableIndex || 1,
            row: rowIndex + 1,
            col: columnIndex + 1,
        };
        cell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(formula));
    });
}

function remapCellValuesForStructure(
    values: Record<string, string>,
    action: FillableTableStructureAction,
    index: number
): Record<string, string> {
    const insertingRow = action === 'add-row-above' || action === 'add-row-below';
    const insertingColumn = action === 'add-column-before' || action === 'add-column-after';
    return Object.keys(values).reduce((next: Record<string, string>, key) => {
        const match = /^r(\d+)c(\d+)$/.exec(key);
        if (!match) {
            return next;
        }
        let row = Number(match[1]);
        let column = Number(match[2]);
        if (insertingRow && row >= index) {
            row += 1;
        }
        if (insertingColumn && column >= index) {
            column += 1;
        }
        if (action === 'delete-row') {
            if (row === index) {
                return next;
            }
            if (row > index) {
                row -= 1;
            }
        }
        if (action === 'delete-column') {
            if (column === index) {
                return next;
            }
            if (column > index) {
                column -= 1;
            }
        }
        next[`r${row}c${column}`] = values[key];
        return next;
    }, {});
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
    if (!(isExcelFormulaText(nextValue) && isExcelFormulaComplete(nextValue))) {
        tableHtml = writePlainTableCellText(tableHtml, cellKey, nextValue);
    }
    field.tableHtml = tableHtml;
    const values = refreshTableFieldFormulaValues({ ...field, tableHtml }, tableValues);
    return { values, tableHtml };
}

function writePlainTableCellText(tableHtml: string, cellKey: string, value: string): string {
    const match = /^r(\d+)c(\d+)$/.exec(cellKey);
    if (!match || !tableHtml) {
        return tableHtml;
    }
    const host = document.createElement('div');
    host.innerHTML = tableHtml;
    const table = host.querySelector('table');
    const row = table?.querySelectorAll('tr')[Number(match[1])];
    const cell = row?.querySelectorAll('th, td')[Number(match[2])] as HTMLTableCellElement | undefined;
    if (!cell) {
        return tableHtml;
    }
    if (isFormulaTableCell(cell)) {
        cell.removeAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
        cell.classList.remove('template-formula-cell');
        cell.removeAttribute('title');
    }
    cell.setAttribute('data-template-input-cell', 'true');
    cell.textContent = value;
    return host.innerHTML;
}
