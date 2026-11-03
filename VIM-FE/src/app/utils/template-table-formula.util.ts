export type TableFormulaOperation = 'add' | 'subtract' | 'multiply' | 'divide' | 'validate';

export type TableFormulaValidationOperator = 'eq' | 'ne' | 'gt' | 'lt' | 'gte' | 'lte';

export interface TableFormulaCellRef {
    tableIndex: number;
    row: number;
    col: number;
}

export interface TableFormulaDefinition {
    version: 1;
    target: TableFormulaCellRef;
    operation: TableFormulaOperation;
    sources: TableFormulaCellRef[];
    /** Excel-style typed formula, e.g. "=(A2*B2)" */
    expression?: string;
    validation?: {
        operator: TableFormulaValidationOperator;
        value: number;
    };
}

export interface DocumentTableSummary {
    tableIndex: number;
    rowCount: number;
    columnCount: number;
    label: string;
}

export interface TableCellPreview {
    row: number;
    col: number;
    label: string;
    isFormula: boolean;
}

export const TEMPLATE_TABLE_FORMULA_ATTR = 'data-template-formula';

export interface SyncTableFormulaOptions {
    /** While the user edits a formula in the cell, do not replace that cell's content with the computed result. */
    editingTarget?: TableFormulaCellRef | null;
}

const FORMULA_CELL_CLASS = 'template-formula-cell';

export function formatTableCellRef(ref: TableFormulaCellRef | null | undefined): string {
    if (!ref) {
        return '—';
    }
    return `R${ref.row}C${ref.col}`;
}

export function isSameTableCellRef(left: TableFormulaCellRef, right: TableFormulaCellRef): boolean {
    return left.tableIndex === right.tableIndex && left.row === right.row && left.col === right.col;
}

export function getDomTableCellContext(editableRoot: HTMLElement | null | undefined): TableFormulaCellRef | null {
    if (!editableRoot) {
        return null;
    }
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) {
        return null;
    }
    let node: Node | null = selection.anchorNode;
    if (node?.nodeType === Node.TEXT_NODE) {
        node = node.parentElement;
    }
    const cell = (node as Element | null)?.closest?.('td, th');
    if (!cell || !editableRoot.contains(cell)) {
        return null;
    }
    const table = cell.closest('table');
    if (!table) {
        return null;
    }
    const tables = Array.from(editableRoot.querySelectorAll('table'));
    const tableIndex = tables.indexOf(table) + 1;
    if (tableIndex <= 0) {
        return null;
    }
    const rowElement = cell.parentElement;
    const rows = Array.from(table.querySelectorAll('tr'));
    const row = rows.indexOf(rowElement as HTMLTableRowElement) + 1;
    const cells = Array.from(rowElement?.querySelectorAll('th, td') || []);
    const col = cells.indexOf(cell as HTMLTableCellElement) + 1;
    if (row <= 0 || col <= 0) {
        return null;
    }
    return { tableIndex, row, col };
}

export function getTableCellMatrix(html: string, tableIndex: number): TableCellPreview[][] {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    const table = Array.from(host.querySelectorAll('table'))[tableIndex - 1];
    if (!table) {
        return [];
    }
    return Array.from(table.querySelectorAll('tr')).map((row, rowIndex) => (
        Array.from(row.querySelectorAll('th, td')).map((cell, colIndex) => {
            const element = cell as HTMLTableCellElement;
            const label = String(element.textContent || '')
                .replace(/\s+/g, ' ')
                .trim()
                .slice(0, 24);
            return {
                row: rowIndex + 1,
                col: colIndex + 1,
                label: label || 'Empty',
                isFormula: element.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR),
            };
        })
    ));
}

export function listDocumentTables(html: string): DocumentTableSummary[] {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    const tables = Array.from(host.querySelectorAll('table'));
    return tables.map((table, index) => {
        const rowCount = table.querySelectorAll('tr').length;
        let columnCount = 0;
        table.querySelectorAll('tr').forEach((row) => {
            columnCount = Math.max(columnCount, countRowColumns(row));
        });
        const caption = table.querySelector('caption')?.textContent?.trim();
        const firstCell = table.querySelector('th, td')?.textContent?.trim();
        const hint = caption || firstCell || `Table ${index + 1}`;
        return {
            tableIndex: index + 1,
            rowCount: Math.max(rowCount, 1),
            columnCount: Math.max(columnCount, 1),
            label: `Table ${index + 1} (${Math.max(rowCount, 1)}×${Math.max(columnCount, 1)}) — ${truncateLabel(hint)}`,
        };
    });
}

export function isExcelFormulaText(text: string | null | undefined): boolean {
    return /^\s*=/.test(String(text || ''));
}

export function isExcelFormulaComplete(text: string | null | undefined): boolean {
    const normalized = normalizeExcelFormulaText(String(text || '').replace(/\s+/g, ' ').trim());
    if (!isExcelFormulaText(normalized)) {
        return false;
    }
    if (!/[A-Za-z]{1,3}\d+|[A-Za-z][A-Za-z0-9]*/.test(normalized)) {
        return false;
    }
    const body = normalized.slice(1).trim();
    let depth = 0;
    for (const character of body) {
        if (character === '(') {
            depth += 1;
        }
        if (character === ')') {
            depth -= 1;
        }
        if (depth < 0) {
            return false;
        }
    }
    if (depth !== 0) {
        return false;
    }
    if (/[+\-*/]$/.test(body)) {
        return false;
    }
    let remainder = body
        .replace(/[A-Za-z]{1,3}\d+|[A-Za-z][A-Za-z0-9]*/g, '')
        .replace(/[+\-*/().]/g, '')
        .replace(/\d+(\.\d+)?/g, '')
        .replace(/\s/g, '');
    return remainder.length === 0;
}

export function readTableCellPlainText(cell: HTMLTableCellElement): string {
    return getTableCellPlainText(cell);
}

export function parseExcelFormulaToDefinition(
    text: string,
    target: TableFormulaCellRef
): TableFormulaDefinition | null {
    const expression = normalizeExcelFormulaText(String(text || '').replace(/\s+/g, ' ').trim());
    if (!isExcelFormulaText(expression)) {
        return null;
    }
    if (!/[A-Za-z]{1,3}\d+|[A-Za-z][A-Za-z0-9]*/.test(expression)) {
        return null;
    }
    return {
        version: 1,
        target,
        operation: inferOperationFromExcelExpression(expression),
        sources: [],
        expression,
    };
}

export function extractTableCellValuesFromHtml(html: string): Record<string, string> {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    const values: Record<string, string> = {};
    host.querySelectorAll('table').forEach((table) => {
        Array.from(table.querySelectorAll('tr')).forEach((row, rowIndex) => {
            Array.from(row.querySelectorAll('th, td')).forEach((cell, colIndex) => {
                const element = cell as HTMLTableCellElement;
                if (element.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR)) {
                    return;
                }
                const text = getTableCellPlainText(element);
                if (isExcelFormulaText(text)) {
                    return;
                }
                values[cellKeyFromIndexes(rowIndex, colIndex)] = text;
            });
        });
    });
    return values;
}

export function syncTableFormulasInHtml(
    html: string,
    tableValues?: Record<string, string> | null,
    options?: SyncTableFormulaOptions
): string {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    discoverTypedExcelFormulas(host);
    const mergedValues = {
        ...extractTableCellValuesFromHtml(host.innerHTML),
        ...normalizeTableValues(tableValues),
    };
    stripComputedValuesFromFormulaSources(mergedValues, host);
    const { values: computedValues } = evaluateTableFieldFormulas(host.innerHTML, mergedValues);

    host.querySelectorAll(`[${TEMPLATE_TABLE_FORMULA_ATTR}]`).forEach((node) => {
        const cell = node as HTMLTableCellElement;
        const table = cell.closest('table');
        if (!table) {
            return;
        }
        const raw = cell.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
        if (!raw) {
            return;
        }
        let formula: TableFormulaDefinition;
        try {
            formula = JSON.parse(raw) as TableFormulaDefinition;
        } catch {
            return;
        }
        const position = getCellPosition(table, cell);
        if (!position) {
            return;
        }
        const liveText = getTableCellPlainText(cell);
        if (isExcelFormulaText(liveText) && !isExcelFormulaComplete(liveText)) {
            return;
        }
        const cellRef: TableFormulaCellRef = {
            tableIndex: getTableIndexWithinRoot(table, host) || 1,
            row: position.rowIndex + 1,
            col: position.colIndex + 1,
        };
        if (isEditingFormulaTarget(options?.editingTarget, cellRef)) {
            cell.classList.add(FORMULA_CELL_CLASS);
            cell.setAttribute('title', buildFormulaDisplayLabel(formula));
            setTableCellDisplayText(cell, getFormulaEditText(formula));
            return;
        }
        const key = cellKeyFromIndexes(position.rowIndex, position.colIndex);
        const scopedKey = `t${cellRef.tableIndex}:${key}`;
        const displayValue = computedValues[scopedKey] ?? computedValues[key] ?? '';
        cell.classList.add(FORMULA_CELL_CLASS);
        cell.setAttribute('title', buildFormulaDisplayLabel(formula));
        setTableCellDisplayText(cell, displayValue);
    });

    return host.innerHTML;
}

/** Keeps CKEditor 5 table widgets intact (figure.table wrapper) after HTML transforms. */
export function ensureCkEditorTableFigures(html: string): string {
    if (!html || !/<table[\s>]/i.test(html)) {
        return html;
    }
    const host = document.createElement('div');
    host.innerHTML = html;
    let changed = false;

    host.querySelectorAll('table').forEach((table) => {
        const parent = table.parentElement;
        if (!(parent?.tagName === 'FIGURE' && parent.classList.contains('table'))) {
            const figure = document.createElement('figure');
            figure.className = 'table';
            table.parentNode?.insertBefore(figure, table);
            figure.appendChild(table);
            changed = true;
        }
    });

    host.querySelectorAll('figure.table').forEach((figure) => {
        if (!figure.querySelector('table')) {
            figure.remove();
            changed = true;
        }
    });

    return changed ? host.innerHTML : html;
}

export function countTablesInHtml(html: string): number {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    return host.querySelectorAll('table').length;
}

export function countTableRowsInHtml(html: string): number {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    return host.querySelectorAll('table tr').length;
}

/** Returns false when applying synced HTML would drop table structure (CKEditor "+" widget bug). */
export function isTableFormulaSyncHtmlSafe(rawHtml: string, syncedHtml: string): boolean {
    const rawTables = countTablesInHtml(rawHtml);
    const syncedTables = countTablesInHtml(syncedHtml);
    if (rawTables > 0 && syncedTables === 0) {
        return false;
    }
    if (syncedTables < rawTables) {
        return false;
    }
    const rawRows = countTableRowsInHtml(rawHtml);
    const syncedRows = countTableRowsInHtml(syncedHtml);
    if (rawRows > 0 && syncedRows < rawRows) {
        return false;
    }
    return true;
}

/** Moves A1-style references the way a spreadsheet does when a formula is pasted into another cell. */
export function shiftExcelFormulaReferences(expression: string, deltaRow: number, deltaCol: number): string {
    const rowShift = Math.round(Number(deltaRow) || 0);
    const columnShift = Math.round(Number(deltaCol) || 0);
    if (!rowShift && !columnShift) {
        return String(expression || '');
    }
    return String(expression || '').replace(
        /(\$?)([A-Za-z]{1,3})(\$?)(\d+)/g,
        (match, columnLock: string, letters: string, rowLock: string, rowDigits: string, offset: number, source: string) => {
            const previous = source.charAt(offset - 1);
            if (previous && /[A-Za-z]/.test(previous)) {
                return match;
            }
            let column = excelColumnLettersToIndex(letters);
            let row = Number(rowDigits);
            if (!Number.isFinite(column) || !Number.isFinite(row) || column < 1 || row < 1) {
                return match;
            }
            if (!columnLock) {
                column += columnShift;
            }
            if (!rowLock) {
                row += rowShift;
            }
            if (column < 1 || row < 1) {
                return '#REF!';
            }
            return `${columnLock}${formatExcelColumnLabel(column)}${rowLock}${row}`;
        }
    );
}

/** Shifts references at or after the inserted row or column, the way a spreadsheet does when a row or column is inserted. */
export function shiftExcelFormulaReferencesForInsertion(
    expression: string,
    axis: 'row' | 'column',
    insertAt: number
): string {
    const pivot = Math.round(Number(insertAt) || 0);
    if (pivot < 1) {
        return String(expression || '');
    }
    return String(expression || '').replace(
        /(\$?)([A-Za-z]{1,3})(\$?)(\d+)/g,
        (match, columnLock: string, letters: string, rowLock: string, rowDigits: string, offset: number, source: string) => {
            const previous = source.charAt(offset - 1);
            if (previous && /[A-Za-z]/.test(previous)) {
                return match;
            }
            let column = excelColumnLettersToIndex(letters);
            let row = Number(rowDigits);
            if (!Number.isFinite(column) || !Number.isFinite(row) || column < 1 || row < 1) {
                return match;
            }
            if (axis === 'column' && !columnLock && column >= pivot) {
                column += 1;
            }
            if (axis === 'row' && !rowLock && row >= pivot) {
                row += 1;
            }
            return `${columnLock}${formatExcelColumnLabel(column)}${rowLock}${row}`;
        }
    );
}

export function formatExcelColumnLabel(columnIndex: number): string {
    let letters = '';
    let index = Math.max(1, Math.round(columnIndex));
    while (index > 0) {
        const remainder = (index - 1) % 26;
        letters = String.fromCharCode(65 + remainder) + letters;
        index = Math.floor((index - 1) / 26);
    }
    return letters;
}

export function finalizeTableFormulaHtml(
    html: string,
    tableValues?: Record<string, string> | null,
    options?: SyncTableFormulaOptions
): string {
    return ensureCkEditorTableFigures(syncTableFormulasInHtml(html, tableValues, options));
}

export function applyTableFormulaToHtml(html: string, formula: TableFormulaDefinition): string {
    const host = document.createElement('div');
    host.innerHTML = String(html || '');
    const tables = Array.from(host.querySelectorAll('table'));
    const table = tables[formula.target.tableIndex - 1];
    if (!table) {
        return html;
    }
    const targetCell = getTableCellElement(table, formula.target.row, formula.target.col);
    if (!targetCell) {
        return html;
    }
    targetCell.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(formula));
    targetCell.classList.add(FORMULA_CELL_CLASS);
    return finalizeTableFormulaHtml(host.innerHTML);
}

export function buildFormulaDisplayLabel(formula: TableFormulaDefinition): string {
    if (formula.expression) {
        return formula.expression;
    }
    const opLabel = getOperationLabel(formula.operation);
    const target = `R${formula.target.row}C${formula.target.col}`;
    if (formula.operation === 'validate') {
        const rule = formula.validation
            ? `${getValidationOperatorLabel(formula.validation.operator)} ${formula.validation.value}`
            : 'rule';
        return `ƒ Validate ${target} (${rule})`;
    }
    const sourceCount = formula.sources.length;
    return `ƒ ${opLabel} → ${target}${sourceCount ? ` (${sourceCount} cell${sourceCount === 1 ? '' : 's'})` : ''}`;
}

export function evaluateTableFieldFormulas(
    tableHtml: string,
    tableValues: Record<string, string> | null | undefined
): { values: Record<string, string>; validationErrors: string[] } {
    const host = document.createElement('div');
    host.innerHTML = String(tableHtml || '');
    discoverTypedExcelFormulas(host);
    const discoveredHtml = host.innerHTML;
    const values = {
        ...extractTableCellValuesFromHtml(discoveredHtml),
        ...normalizeTableValues(tableValues),
    };
    stripComputedValuesFromFormulaSources(values, host);
    const validationErrors: string[] = [];
    const tables = Array.from(host.querySelectorAll('table'));

    host.querySelectorAll(`[${TEMPLATE_TABLE_FORMULA_ATTR}]`).forEach((node) => {
        const cell = node as HTMLTableCellElement;
        const table = cell.closest('table');
        if (!table) {
            return;
        }
        const tableIndex = tables.indexOf(table) + 1;
        const raw = cell.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
        if (!raw) {
            return;
        }
        let formula: TableFormulaDefinition;
        try {
            formula = JSON.parse(raw) as TableFormulaDefinition;
        } catch {
            return;
        }
        const position = getCellPosition(table, cell);
        if (!position) {
            return;
        }
        const key = cellKeyFromIndexes(position.rowIndex, position.colIndex);
        const htmlForRefs = host.innerHTML;
        const outcome = computeFormulaOutcome(formula, values, htmlForRefs, tableIndex);
        values[`t${tableIndex}:${key}`] = outcome.displayValue;
        if (outcome.validationMessage) {
            validationErrors.push(outcome.validationMessage);
        }
        values[key] = outcome.displayValue;
    });

    return { values, validationErrors };
}

export function validateTableFieldFormulasBeforeSubmit(
    fields: Array<{ id?: string; label?: string; type?: string; tableHtml?: string }>,
    valuesByFieldId: Record<string, any>
): string | null {
    for (const field of fields) {
        if (field.type !== 'table' || !field.tableHtml || !field.id) {
            continue;
        }
        const { validationErrors } = evaluateTableFieldFormulas(field.tableHtml, valuesByFieldId[field.id]);
        if (validationErrors.length > 0) {
            const label = field.label || 'Table';
            return `${label}: ${validationErrors[0]}`;
        }
    }
    return null;
}

export function isFormulaTableCell(element: Element | null | undefined): boolean {
    return !!element && element.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
}

export function getFormulaDefinitionFromCell(cell: HTMLTableCellElement): TableFormulaDefinition | null {
    const raw = cell.getAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw) as TableFormulaDefinition;
    } catch {
        return null;
    }
}

export function getFormulaEditText(formula: TableFormulaDefinition): string {
    if (formula.expression) {
        return formula.expression;
    }
    if (formula.sources.length === 0) {
        return buildFormulaDisplayLabel(formula);
    }
    const refs = formula.sources.map((source) => excelCellAddress(source.row, source.col));
    if (formula.operation === 'add') {
        return `=${refs.join('+')}`;
    }
    if (formula.operation === 'subtract' && refs.length >= 2) {
        return `=${refs[0]}-${refs.slice(1).join('-')}`;
    }
    if (formula.operation === 'multiply') {
        return `=(${refs.join('*')})`;
    }
    if (formula.operation === 'divide' && refs.length >= 2) {
        return `=${refs[0]}/${refs[1]}`;
    }
    return buildFormulaDisplayLabel(formula);
}

export function getTableFormulaCellRefFromElement(cell: HTMLTableCellElement, root?: ParentNode | null): TableFormulaCellRef | null {
    const table = cell.closest('table');
    if (!table) {
        return null;
    }
    const scope = root || cell.ownerDocument;
    const tables = Array.from(scope.querySelectorAll('table'));
    const tableIndex = tables.indexOf(table) + 1;
    if (tableIndex <= 0) {
        return null;
    }
    const position = getCellPosition(table, cell);
    if (!position) {
        return null;
    }
    return {
        tableIndex,
        row: position.rowIndex + 1,
        col: position.colIndex + 1,
    };
}

export function prepareFormulaCellForEditing(cell: HTMLTableCellElement): string | null {
    const formula = getFormulaDefinitionFromCell(cell);
    if (!formula) {
        return null;
    }
    const editText = getFormulaEditText(formula);
    cell.innerHTML = `<p>${escapeHtml(editText)}</p>`;
    return editText;
}

export function isEditingFormulaTarget(
    editingTarget: TableFormulaCellRef | null | undefined,
    cellRef: TableFormulaCellRef
): boolean {
    if (!editingTarget) {
        return false;
    }
    return isSameTableCellRef(editingTarget, cellRef);
}

function computeFormulaOutcome(
    formula: TableFormulaDefinition,
    values: Record<string, string>,
    html: string,
    defaultTableIndex: number
): { displayValue: string; validationMessage?: string } {
    if (formula.expression) {
        const formulaRow = Math.max(1, Number(formula.target?.row) || 1);
        const status = getExcelExpressionStatus(formula.expression, defaultTableIndex, values, html, new Set(), formulaRow);
        if (status === 'blank') {
            return { displayValue: '' };
        }
        const computed = evaluateExcelFormulaExpression(formula.expression, defaultTableIndex, values, html, new Set(), formulaRow);
        if (computed === null) {
            return { displayValue: '#ERR' };
        }
        return { displayValue: formatNumber(computed) };
    }

    const numbers = formula.sources
        .map((source) => readNumericCellValue(values, html, source, defaultTableIndex))
        .filter((value): value is number => value !== null);

    if (formula.operation === 'validate') {
        const computed = numbers.length > 0 ? aggregateNumbers('add', numbers) : null;
        const operator = formula.validation?.operator || 'eq';
        const expected = Number(formula.validation?.value);
        const passes = computed !== null
            && Number.isFinite(expected)
            && compareValues(computed, expected, operator);
        const targetLabel = `row ${formula.target.row}, column ${formula.target.col}`;
        if (!passes) {
            return {
                displayValue: 'Invalid',
                validationMessage: `Formula validation failed at ${targetLabel}.`,
            };
        }
        return { displayValue: 'Valid' };
    }

    if (numbers.length === 0) {
        return { displayValue: '' };
    }

    if (formula.operation === 'divide' && numbers.length < 2) {
        return { displayValue: '' };
    }

    const result = aggregateNumbers(formula.operation, numbers);
    if (result === null) {
        return { displayValue: '' };
    }

    return { displayValue: formatNumber(result) };
}

function aggregateNumbers(operation: TableFormulaOperation, numbers: number[]): number | null {
    if (numbers.length === 0) {
        return null;
    }
    if (operation === 'add') {
        return numbers.reduce((sum, value) => sum + value, 0);
    }
    if (operation === 'subtract') {
        return numbers.slice(1).reduce((acc, value) => acc - value, numbers[0]);
    }
    if (operation === 'multiply') {
        return numbers.reduce((product, value) => product * value, 1);
    }
    if (operation === 'divide') {
        const divisor = numbers[1];
        if (!divisor) {
            return null;
        }
        return numbers[0] / divisor;
    }
    return null;
}

function listFormulaTokens(expression: string): string[] {
    return normalizeExcelFormulaText(expression).match(/[A-Za-z]{1,3}\d+|[A-Za-z][A-Za-z0-9]*/g) || [];
}

function isCellAddressToken(token: string): boolean {
    return /^[A-Za-z]{1,3}\d+$/.test(String(token || '').trim());
}

/** A word such as "price" means the column whose header is that word, on the formula's own row. */
function resolveFormulaToken(
    token: string,
    tableIndex: number,
    values: Record<string, string>,
    html: string,
    stack: Set<string>,
    formulaRow: number
): { status: 'blank' | 'invalid' | 'ready'; value: number | null } {
    const normalized = String(token || '').trim();
    if (isCellAddressToken(normalized)) {
        return resolveReferencedNumber(normalized, tableIndex, values, html, stack);
    }
    if (!formulaRow || formulaRow < 1) {
        return { status: 'invalid', value: null };
    }
    const host = document.createElement('div');
    host.innerHTML = html;
    const table = host.querySelectorAll('table')[tableIndex - 1] as HTMLTableElement | undefined;
    if (!table) {
        return { status: 'invalid', value: null };
    }
    const column = findHeaderColumn(table, normalized, formulaRow);
    if (!column) {
        return { status: 'invalid', value: null };
    }
    return resolveReferencedNumber(excelCellAddress(formulaRow, column), tableIndex, values, html, stack);
}

function findHeaderColumn(table: HTMLTableElement, name: string, formulaRow: number): number {
    const wanted = name.trim().toLowerCase();
    if (!wanted) {
        return 0;
    }
    const rows = Array.from(table.querySelectorAll('tr'));
    for (let rowIndex = formulaRow - 2; rowIndex >= 0; rowIndex -= 1) {
        const cells = Array.from(rows[rowIndex]?.querySelectorAll('th, td') || []) as HTMLTableCellElement[];
        for (let columnIndex = 0; columnIndex < cells.length; columnIndex += 1) {
            const text = getTableCellPlainText(cells[columnIndex]).trim().toLowerCase();
            if (text && text === wanted) {
                return columnIndex + 1;
            }
        }
    }
    return 0;
}

function getExcelExpressionStatus(
    expression: string,
    tableIndex: number,
    values: Record<string, string>,
    html: string,
    stack: Set<string> = new Set(),
    formulaRow = 0
): 'blank' | 'invalid' | 'ready' {
    const tokens = listFormulaTokens(expression);
    if (tokens.length === 0) {
        return 'invalid';
    }
    let sawBlank = false;
    for (const token of tokens) {
        const resolved = resolveFormulaToken(token, tableIndex, values, html, stack, formulaRow);
        if (resolved.status === 'invalid') {
            return 'invalid';
        }
        if (resolved.status === 'blank') {
            sawBlank = true;
        }
    }
    return sawBlank ? 'blank' : 'ready';
}

function resolveReferencedNumber(
    token: string,
    tableIndex: number,
    values: Record<string, string>,
    html: string,
    stack: Set<string>
): { status: 'blank' | 'invalid' | 'ready'; value: number | null } {
    const match = /^([A-Za-z]+)(\d+)$/.exec(String(token || '').trim());
    if (!match) {
        return { status: 'invalid', value: null };
    }
    const col = excelColumnLettersToIndex(match[1]);
    const row = Number(match[2]);
    if (!Number.isFinite(row) || row <= 0 || col <= 0) {
        return { status: 'invalid', value: null };
    }
    const host = document.createElement('div');
    host.innerHTML = html;
    const tables = host.querySelectorAll('table');
    const table = tables[tableIndex - 1] as HTMLTableElement | undefined;
    if (!table) {
        return { status: 'invalid', value: null };
    }
    const cell = getTableCellElement(table, row, col);
    if (!cell) {
        return { status: 'invalid', value: null };
    }
    const position = getCellPosition(table, cell);
    if (!position) {
        return { status: 'invalid', value: null };
    }
    const identity = `${tableIndex}:${position.rowIndex}:${position.colIndex}`;
    const cacheKey = `__f:${identity}`;
    if (Object.prototype.hasOwnProperty.call(values, cacheKey)) {
        return cachedReferenceStatus(values[cacheKey]);
    }
    if (stack.has(identity)) {
        return { status: 'invalid', value: null };
    }

    const shown = getTableCellPlainText(cell);
    const formula = cell.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR) ? getFormulaDefinitionFromCell(cell) : null;
    const expression = formula?.expression && isExcelFormulaComplete(formula.expression)
        ? normalizeExcelFormulaText(formula.expression)
        : (isExcelFormulaComplete(shown) ? shown : '');
    if (expression) {
        stack.add(identity);
        const childRow = position.rowIndex + 1;
        const childTokens = listFormulaTokens(expression);
        let sawBlank = false;
        for (const child of childTokens) {
            const childResult = resolveFormulaToken(child, tableIndex, values, html, stack, childRow);
            if (childResult.status === 'invalid') {
                stack.delete(identity);
                values[cacheKey] = '#ERR';
                return { status: 'invalid', value: null };
            }
            if (childResult.status === 'blank') {
                sawBlank = true;
            }
        }
        if (sawBlank || childTokens.length === 0) {
            stack.delete(identity);
            values[cacheKey] = '';
            return { status: 'blank', value: null };
        }
        const computed = evaluateExcelFormulaExpression(expression, tableIndex, values, html, stack, position.rowIndex + 1);
        stack.delete(identity);
        if (computed === null) {
            values[cacheKey] = '#ERR';
            return { status: 'invalid', value: null };
        }
        values[cacheKey] = formatNumber(computed);
        return { status: 'ready', value: computed };
    }

    if (tables.length === 1) {
        const storedKey = cellKeyFromIndexes(position.rowIndex, position.colIndex);
        if (Object.prototype.hasOwnProperty.call(values, storedKey) && !isExcelFormulaText(values[storedKey])) {
            const stored = values[storedKey];
            if (!String(stored).trim()) {
                values[cacheKey] = '';
                return { status: 'blank', value: null };
            }
            const storedNumber = parseNumeric(stored);
            if (storedNumber === null) {
                values[cacheKey] = '#ERR';
                return { status: 'invalid', value: null };
            }
            values[cacheKey] = formatNumber(storedNumber);
            return { status: 'ready', value: storedNumber };
        }
    }

    if (!shown.trim()) {
        values[cacheKey] = '';
        return { status: 'blank', value: null };
    }
    const shownNumber = parseNumeric(shown);
    if (shownNumber === null) {
        values[cacheKey] = '#ERR';
        return { status: 'invalid', value: null };
    }
    values[cacheKey] = formatNumber(shownNumber);
    return { status: 'ready', value: shownNumber };
}

function cachedReferenceStatus(cached: string): { status: 'blank' | 'invalid' | 'ready'; value: number | null } {
    if (!String(cached).trim()) {
        return { status: 'blank', value: null };
    }
    if (cached === '#ERR') {
        return { status: 'invalid', value: null };
    }
    const parsed = parseNumeric(cached);
    return parsed === null
        ? { status: 'invalid', value: null }
        : { status: 'ready', value: parsed };
}

function readNumericCellValue(
    values: Record<string, string>,
    html: string,
    ref: TableFormulaCellRef,
    defaultTableIndex: number,
    stack: Set<string> = new Set()
): number | null {
    const tableIndex = ref.tableIndex || defaultTableIndex;
    const resolved = resolveReferencedNumber(excelCellAddress(ref.row, ref.col), tableIndex, values, html, stack);
    return resolved.status === 'ready' ? resolved.value : null;
}

function getTableCellElement(table: HTMLTableElement, row: number, col: number): HTMLTableCellElement | null {
    const targetRow = table.querySelectorAll('tr')[row - 1];
    if (!targetRow) {
        return null;
    }
    const cells = Array.from(targetRow.querySelectorAll('th, td'));
    return (cells[col - 1] as HTMLTableCellElement) || null;
}

function getCellPosition(table: HTMLTableElement, cell: HTMLTableCellElement): { rowIndex: number; colIndex: number } | null {
    const rows = Array.from(table.querySelectorAll('tr'));
    for (let rowIndex = 0; rowIndex < rows.length; rowIndex += 1) {
        const cells = Array.from(rows[rowIndex].querySelectorAll('th, td'));
        const colIndex = cells.indexOf(cell);
        if (colIndex >= 0) {
            return { rowIndex, colIndex };
        }
    }
    return null;
}

function cellKeyFromIndexes(rowIndex: number, colIndex: number): string {
    return `r${rowIndex}c${colIndex}`;
}

function normalizeTableValues(value: any): Record<string, string> {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        return {};
    }
    const normalized: Record<string, string> = {};
    Object.keys(value).forEach((key) => {
        normalized[key] = String(value[key] ?? '');
    });
    return normalized;
}

function parseNumeric(raw: string | undefined): number | null {
    const cleaned = String(raw ?? '').replace(/,/g, '').trim();
    if (!cleaned) {
        return null;
    }
    const parsed = Number(cleaned);
    return Number.isFinite(parsed) ? parsed : null;
}

function formatNumber(value: number): string {
    if (Number.isInteger(value)) {
        return String(value);
    }
    return String(Math.round(value * 1000) / 1000);
}

function compareValues(left: number, right: number, operator: TableFormulaValidationOperator): boolean {
    switch (operator) {
        case 'eq':
            return left === right;
        case 'ne':
            return left !== right;
        case 'gt':
            return left > right;
        case 'lt':
            return left < right;
        case 'gte':
            return left >= right;
        case 'lte':
            return left <= right;
        default:
            return false;
    }
}

function countRowColumns(row: Element): number {
    return Array.from(row.children).filter((cell) => ['TD', 'TH'].includes(cell.tagName)).length;
}

function truncateLabel(value: string): string {
    const trimmed = value.replace(/\s+/g, ' ').trim();
    return trimmed.length > 40 ? `${trimmed.slice(0, 37)}...` : trimmed;
}

function getOperationLabel(operation: TableFormulaOperation): string {
    switch (operation) {
        case 'add':
            return 'Sum';
        case 'subtract':
            return 'Subtract';
        case 'multiply':
            return 'Multiply';
        case 'divide':
            return 'Divide';
        case 'validate':
            return 'Validate';
        default:
            return operation;
    }
}

function getValidationOperatorLabel(operator: TableFormulaValidationOperator): string {
    switch (operator) {
        case 'eq':
            return '=';
        case 'ne':
            return '≠';
        case 'gt':
            return '>';
        case 'lt':
            return '<';
        case 'gte':
            return '≥';
        case 'lte':
            return '≤';
        default:
            return operator;
    }
}

function escapeHtml(value: string): string {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function getTableCellPlainText(cell: HTMLTableCellElement): string {
    const clone = cell.cloneNode(true) as HTMLTableCellElement;
    clone.querySelectorAll('.template-formula-label').forEach((node) => node.remove());
    const root = (clone.querySelector('p') || clone) as HTMLElement;
    let text = stripInvisibleFormulaChars(String(root.textContent || ''))
        .replace(/\s+/g, ' ')
        .trim();
    if (isExcelFormulaText(text)) {
        text = restoreFormulaTextFromCellHtml(root, text);
        text = normalizeExcelFormulaText(text);
    }
    return text;
}

function stripInvisibleFormulaChars(text: string): string {
    return String(text || '')
        .replace(/[\u200b\u200c\u200d\ufeff\u2060]/g, '')
        .replace(/\u00a0/g, ' ');
}

/** CKEditor auto-italic eats "*" in =A2*B2; also accept × and juxtaposed refs like =A2B2. */
function normalizeExcelFormulaText(text: string): string {
    const trimmed = stripInvisibleFormulaChars(String(text || '')).replace(/\s+/g, ' ').trim();
    if (!isExcelFormulaText(trimmed)) {
        return trimmed;
    }
    let body = stripInvisibleFormulaChars(trimmed.slice(1)).trim();
    body = body
        .replace(/\u00d7/g, '*')
        .replace(/\u00b7/g, '*')
        .replace(/\u00f7/g, '/')
        .replace(/\uff0b/g, '+')
        .replace(/[\u2212\u2013\u2014]/g, '-')
        .replace(/\s+/g, ' ');
    body = insertImplicitMultiplyBetweenCellRefs(body);
    return `=${body}`;
}

function insertImplicitMultiplyBetweenCellRefs(body: string): string {
    const withoutParens = body.replace(/[()]/g, '');
    if (/[+\-*/]/.test(withoutParens)) {
        return body;
    }
    return body.replace(/([A-Za-z]+\d+)(?=[A-Za-z]+\d+)/gi, '$1*');
}

function restoreFormulaTextFromCellHtml(root: HTMLElement, textContent: string): string {
    const html = root.innerHTML;
    if (!/<(?:em|i)\b/i.test(html)) {
        return textContent;
    }
    let restored = textContent;
    const wrappedRefs = html.matchAll(/<(?:em|i)[^>]*>([A-Za-z]+\d+)<\/(?:em|i)>/gi);
    for (const match of wrappedRefs) {
        const ref = match[1];
        const escaped = ref.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        restored = restored.replace(new RegExp(`([A-Za-z]+\\d+)${escaped}`, 'i'), `$1*${ref}`);
    }
    return restored;
}

function isComputedFormulaDisplayText(text: string): boolean {
    const normalized = String(text || '').trim();
    if (!normalized || normalized === '—' || normalized === '#ERR') {
        return true;
    }
    return /^-?\d+(\.\d+)?$/.test(normalized);
}

function excelCellAddress(row: number, col: number): string {
    let letters = '';
    let index = Math.max(1, Math.round(col));
    while (index > 0) {
        const remainder = (index - 1) % 26;
        letters = String.fromCharCode(65 + remainder) + letters;
        index = Math.floor((index - 1) / 26);
    }
    return `${letters}${Math.max(1, Math.round(row))}`;
}

function getTableIndexWithinRoot(table: HTMLTableElement, root: ParentNode): number {
    const tables = Array.from(root.querySelectorAll('table'));
    const index = tables.indexOf(table);
    return index >= 0 ? index + 1 : 1;
}

function stripComputedValuesFromFormulaSources(values: Record<string, string>, host: ParentNode): void {
    host.querySelectorAll(`[${TEMPLATE_TABLE_FORMULA_ATTR}]`).forEach((node) => {
        const cell = node as HTMLTableCellElement;
        const table = cell.closest('table');
        if (!table) {
            return;
        }
        const position = getCellPosition(table, cell);
        if (!position) {
            return;
        }
        const key = cellKeyFromIndexes(position.rowIndex, position.colIndex);
        delete values[key];
    });
}

function setTableCellDisplayText(cell: HTMLTableCellElement, text: string): void {
    const value = text || '\u00a0';
    const paragraphs = Array.from(cell.children).filter((node) => node.tagName === 'P');
    if (paragraphs.length === 1 && cell.children.length === 1) {
        paragraphs[0].textContent = value;
        return;
    }
    cell.textContent = value;
}

function discoverTypedExcelFormulas(host: HTMLElement): void {
    host.querySelectorAll('table').forEach((table, tableIndex) => {
        Array.from(table.querySelectorAll('tr')).forEach((row, rowIndex) => {
            Array.from(row.querySelectorAll('th, td')).forEach((cell, colIndex) => {
                const element = cell as HTMLTableCellElement;
                const text = getTableCellPlainText(element);
                if (element.hasAttribute(TEMPLATE_TABLE_FORMULA_ATTR)) {
                    if (isExcelFormulaText(text) && isExcelFormulaComplete(text)) {
                        const target = {
                            tableIndex: tableIndex + 1,
                            row: rowIndex + 1,
                            col: colIndex + 1,
                        };
                        const parsed = parseExcelFormulaToDefinition(text, target);
                        if (parsed) {
                            element.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(parsed));
                        }
                        return;
                    }
                    if (isExcelFormulaText(text) && !isExcelFormulaComplete(text)) {
                        return;
                    }
                    if (isComputedFormulaDisplayText(text)) {
                        return;
                    }
                    element.removeAttribute(TEMPLATE_TABLE_FORMULA_ATTR);
                    element.classList.remove(FORMULA_CELL_CLASS);
                    element.removeAttribute('title');
                    return;
                }
                if (!isExcelFormulaText(text) || !isExcelFormulaComplete(text)) {
                    return;
                }
                const target = {
                    tableIndex: tableIndex + 1,
                    row: rowIndex + 1,
                    col: colIndex + 1,
                };
                const parsed = parseExcelFormulaToDefinition(text, target);
                if (!parsed) {
                    return;
                }
                element.setAttribute(TEMPLATE_TABLE_FORMULA_ATTR, JSON.stringify(parsed));
                element.classList.add(FORMULA_CELL_CLASS);
            });
        });
    });
}

function inferOperationFromExcelExpression(expression: string): TableFormulaOperation {
    const body = normalizeExcelFormulaText(expression).replace(/^=/, '').replace(/[()\s]/g, '');
    if (body.includes('*')) {
        return 'multiply';
    }
    if (body.includes('/')) {
        return 'divide';
    }
    if (body.includes('-')) {
        return 'subtract';
    }
    return 'add';
}

function evaluateExcelFormulaExpression(
    expression: string,
    tableIndex: number,
    values: Record<string, string>,
    html: string,
    stack: Set<string> = new Set(),
    formulaRow = 0
): number | null {
    let body = normalizeExcelFormulaText(String(expression || '').trim());
    if (body.startsWith('=')) {
        body = body.slice(1).trim();
    }
    if (!body) {
        return null;
    }
    const replaced = body.replace(/[A-Za-z]{1,3}\d+|[A-Za-z][A-Za-z0-9]*/g, (token) => {
        const resolved = resolveFormulaToken(token, tableIndex, values, html, stack, formulaRow);
        if (resolved.status !== 'ready' || resolved.value === null) {
            return 'NaN';
        }
        return String(resolved.value);
    });
    if (/[A-Za-z]/.test(replaced.replace(/NaN/g, ''))) {
        return null;
    }
    if (!/^[\d+\-*/().\s]+$/.test(replaced.replace(/NaN/g, ''))) {
        return null;
    }
    try {
        const result = evaluateArithmeticExpression(replaced);
        return typeof result === 'number' && Number.isFinite(result) ? result : null;
    } catch {
        return null;
    }
}

function excelColumnLettersToIndex(letters: string): number {
    let index = 0;
    const normalized = String(letters || '').toUpperCase();
    for (let i = 0; i < normalized.length; i += 1) {
        const code = normalized.charCodeAt(i);
        if (code < 65 || code > 90) {
            return 0;
        }
        index = index * 26 + (code - 64);
    }
    return index;
}

function evaluateArithmeticExpression(expression: string): number {
    const tokens = tokenizeArithmeticExpression(expression);
    let position = 0;

    const parseExpression = (): number => {
        let value = parseTerm();
        while (position < tokens.length) {
            const operator = tokens[position];
            if (operator !== '+' && operator !== '-') {
                break;
            }
            position += 1;
            const rhs = parseTerm();
            value = operator === '+' ? value + rhs : value - rhs;
        }
        return value;
    };

    const parseTerm = (): number => {
        let value = parseFactor();
        while (position < tokens.length) {
            const operator = tokens[position];
            if (operator !== '*' && operator !== '/') {
                break;
            }
            position += 1;
            const rhs = parseFactor();
            if (operator === '/') {
                if (rhs === 0) {
                    throw new Error('divide by zero');
                }
                value /= rhs;
            } else {
                value *= rhs;
            }
        }
        return value;
    };

    const parseFactor = (): number => {
        const token = tokens[position];
        if (token === '(') {
            position += 1;
            const value = parseExpression();
            if (tokens[position] !== ')') {
                throw new Error('missing )');
            }
            position += 1;
            return value;
        }
        if (token === '-' || token === '+') {
            position += 1;
            const value = parseFactor();
            return token === '-' ? -value : value;
        }
        if (!token || !/^-?\d+(\.\d+)?$/.test(token)) {
            throw new Error('invalid token');
        }
        position += 1;
        return Number(token);
    };

    const result = parseExpression();
    if (position !== tokens.length) {
        throw new Error('unexpected token');
    }
    return result;
}

function tokenizeArithmeticExpression(expression: string): string[] {
    const tokens: string[] = [];
    const source = String(expression || '').replace(/\s+/g, '');
    let index = 0;
    while (index < source.length) {
        const char = source[index];
        if ('+-*/()'.includes(char)) {
            tokens.push(char);
            index += 1;
            continue;
        }
        if (/[\d.]/.test(char)) {
            let end = index + 1;
            while (end < source.length && /[\d.]/.test(source[end])) {
                end += 1;
            }
            tokens.push(source.slice(index, end));
            index = end;
            continue;
        }
        if (source.slice(index, index + 3) === 'NaN') {
            tokens.push('NaN');
            index += 3;
            continue;
        }
        throw new Error('invalid character');
    }
    return tokens.map((token) => (token === 'NaN' ? 'NaN' : token));
}
