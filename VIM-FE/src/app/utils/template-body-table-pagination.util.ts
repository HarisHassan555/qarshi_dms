/** Live WYSIWYG page-break helpers for document-body (CKEditor) tables. */

export const TEMPLATE_PAGE_BREAK_ATTR = 'data-template-page-break';
export const TEMPLATE_TABLE_GROUP_ATTR = 'data-table-group-id';
export const TEMPLATE_TABLE_CONTINUATION_ATTR = 'data-table-continuation';
export const TEMPLATE_TABLE_HEADER_CLONE_ATTR = 'data-table-header-clone';

export interface BodyTablePaginationOptions {
    pageHeight: number;
    pageGap: number;
    /** Prefer repeating thead / first header row on continuation fragments. Default true. */
    repeatHeader?: boolean;
    /** Epsilon when comparing against page bottoms. */
    epsilonPx?: number;
    /**
     * Divide live getBoundingClientRect deltas by this scale (e.g. canvas zoom).
     * Default 1.
     */
    coordinateScale?: number;
}

const DEFAULT_EPSILON = 2;

function createGroupId(): string {
    return `tbl-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function isPageBreakElement(node: Element | null | undefined): boolean {
    return !!node && node.getAttribute?.(TEMPLATE_PAGE_BREAK_ATTR) === 'true';
}

function isContinuationFigure(figure: Element): boolean {
    return figure.getAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR) === 'true';
}

function getTableRoot(figureOrTable: Element): HTMLTableElement | null {
    if (figureOrTable.tagName.toLowerCase() === 'table') {
        return figureOrTable as HTMLTableElement;
    }
    return figureOrTable.querySelector('table');
}

function getDirectRows(table: HTMLTableElement): HTMLTableRowElement[] {
    const sections = [
        ...Array.from(table.tHead ? [table.tHead] : []),
        ...Array.from(table.tBodies || []),
        ...Array.from(table.tFoot ? [table.tFoot] : [])
    ];
    if (sections.length) {
        return sections.flatMap((section) => Array.from(section.rows));
    }
    return Array.from(table.rows);
}

function isHeaderRow(row: HTMLTableRowElement): boolean {
    if (row.closest('thead')) {
        return true;
    }
    if (row.getAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR) === 'true') {
        return true;
    }
    const cells = Array.from(row.cells);
    return cells.length > 0 && cells.every((cell) => cell.tagName.toLowerCase() === 'th');
}

function getHeaderRowCount(rows: HTMLTableRowElement[]): number {
    let count = 0;
    for (const row of rows) {
        if (!isHeaderRow(row)) {
            break;
        }
        count += 1;
    }
    return count;
}

function getRowSpanBlockEnd(rows: HTMLTableRowElement[], startIndex: number): number {
    let coverUntil = startIndex;
    for (let i = startIndex; i <= coverUntil && i < rows.length; i++) {
        Array.from(rows[i].cells).forEach((cell) => {
            const span = Math.max(1, Number(cell.rowSpan) || 1);
            coverUntil = Math.max(coverUntil, i + span - 1);
        });
    }
    return Math.min(rows.length - 1, coverUntil);
}

function pageTop(pageIndex: number, pageHeight: number, pageGap: number): number {
    return pageIndex * (pageHeight + pageGap);
}

function pageBottom(pageIndex: number, pageHeight: number, pageGap: number): number {
    return pageTop(pageIndex, pageHeight, pageGap) + pageHeight;
}

function pageIndexForY(y: number, pageHeight: number, pageGap: number): number {
    const stride = pageHeight + pageGap;
    if (stride <= 0) {
        return 0;
    }
    return Math.max(0, Math.floor(Math.max(0, y) / stride));
}

function buildPageBreakSpacer(heightPx: number): HTMLElement {
    const spacer = document.createElement('div');
    spacer.setAttribute(TEMPLATE_PAGE_BREAK_ATTR, 'true');
    spacer.setAttribute('contenteditable', 'false');
    spacer.className = 'template-page-break';
    spacer.setAttribute('aria-hidden', 'true');
    spacer.style.display = 'block';
    spacer.style.width = '100%';
    spacer.style.height = `${Math.max(1, Math.ceil(heightPx))}px`;
    spacer.style.margin = '0';
    spacer.style.padding = '0';
    spacer.style.border = '0';
    spacer.style.pointerEvents = 'none';
    spacer.innerHTML = '&nbsp;';
    return spacer;
}

function cloneRow(row: HTMLTableRowElement, asHeaderClone = false): HTMLTableRowElement {
    const clone = row.cloneNode(true) as HTMLTableRowElement;
    if (asHeaderClone) {
        clone.setAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR, 'true');
    } else {
        clone.removeAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR);
    }
    return clone;
}

function ensureFigureWrapper(tableLike: Element): HTMLElement {
    if (tableLike.tagName.toLowerCase() === 'figure') {
        return tableLike as HTMLElement;
    }
    const figure = document.createElement('figure');
    figure.className = 'table';
    const parent = tableLike.parentElement;
    if (parent) {
        parent.insertBefore(figure, tableLike);
        figure.appendChild(tableLike);
    } else {
        figure.appendChild(tableLike);
    }
    return figure;
}

function collectLogicalTableStarts(root: ParentNode): HTMLElement[] {
    const starts: HTMLElement[] = [];
    const seenGroups = new Set<string>();
    const rootEl = root as Element;
    const candidates = Array.from(
        rootEl.querySelectorAll?.('figure.table, table') || []
    ) as HTMLElement[];

    for (const node of candidates) {
        if (node.tagName.toLowerCase() === 'table' && node.closest('figure.table')) {
            continue;
        }
        if (node.closest('.floating-field, .table-field-preview, .table-field-editor-surface, .inline-table-shell, .template-word-editor-body, .document-body-word-editor, [data-body-word-editor], .ck-editor, .word-editor-value, .ql-editor')) {
            continue;
        }
        const groupId = node.getAttribute(TEMPLATE_TABLE_GROUP_ATTR) || '';
        if (groupId) {
            if (seenGroups.has(groupId) || isContinuationFigure(node)) {
                continue;
            }
            seenGroups.add(groupId);
            starts.push(node);
            continue;
        }
        if (isContinuationFigure(node)) {
            continue;
        }
        starts.push(node);
    }
    return starts;
}

/**
 * Merge continuation table fragments and remove page-break spacers so measurement
 * starts from a single logical document.
 */
export function collapseBodyTablePagination(html: string): string {
    if (!html || typeof document === 'undefined') {
        return html || '';
    }

    const host = document.createElement('div');
    host.innerHTML = html;

    host.querySelectorAll(`[${TEMPLATE_PAGE_BREAK_ATTR}="true"]`).forEach((node) => node.remove());

    const groups = new Map<string, HTMLElement[]>();
    host.querySelectorAll(`[${TEMPLATE_TABLE_GROUP_ATTR}]`).forEach((node) => {
        const id = node.getAttribute(TEMPLATE_TABLE_GROUP_ATTR) || '';
        if (!id) {
            return;
        }
        if (node.tagName.toLowerCase() === 'table' && node.closest(`[${TEMPLATE_TABLE_GROUP_ATTR}] figure, figure[${TEMPLATE_TABLE_GROUP_ATTR}]`)) {
            // Prefer figure-level grouping when both are marked.
        }
        const list = groups.get(id) || [];
        list.push(node as HTMLElement);
        groups.set(id, list);
    });

    groups.forEach((nodes) => {
        const figures = nodes.filter((n) => {
            const tag = n.tagName.toLowerCase();
            if (tag === 'figure') {
                return true;
            }
            return tag === 'table' && !n.closest('figure.table');
        });
        if (!figures.length) {
            return;
        }
        figures.sort((a, b) => {
            const pos = a.compareDocumentPosition(b);
            if (pos & Node.DOCUMENT_POSITION_FOLLOWING) {
                return -1;
            }
            if (pos & Node.DOCUMENT_POSITION_PRECEDING) {
                return 1;
            }
            return 0;
        });

        const primary = figures[0];
        const primaryTable = getTableRoot(primary);
        if (!primaryTable) {
            figures.slice(1).forEach((n) => n.remove());
            return;
        }

        for (let i = 1; i < figures.length; i++) {
            const fragment = figures[i];
            const table = getTableRoot(fragment);
            if (!table) {
                fragment.remove();
                continue;
            }
            getDirectRows(table).forEach((row) => {
                if (row.getAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR) === 'true') {
                    return;
                }
                const targetSection = primaryTable.tBodies[0] || primaryTable;
                targetSection.appendChild(row);
            });
            fragment.remove();
        }

        primary.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
        primary.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
        primaryTable.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
        primaryTable.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
        primary.querySelectorAll(`[${TEMPLATE_TABLE_HEADER_CLONE_ATTR}]`).forEach((row) => {
            row.removeAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR);
        });
    });

    host.querySelectorAll(`[${TEMPLATE_TABLE_CONTINUATION_ATTR}]`).forEach((node) => {
        node.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
    });
    host.querySelectorAll(`[${TEMPLATE_TABLE_GROUP_ATTR}]`).forEach((node) => {
        node.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
    });
    host.querySelectorAll(`[${TEMPLATE_TABLE_HEADER_CLONE_ATTR}]`).forEach((node) => {
        node.removeAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR);
    });

    return host.innerHTML;
}

function relativeTop(el: Element, root: HTMLElement, scale = 1): number {
    const rootRect = root.getBoundingClientRect();
    const rect = el.getBoundingClientRect();
    const safeScale = Math.max(0.01, Number(scale) || 1);
    return (rect.top - rootRect.top) / safeScale + (root.scrollTop || 0);
}

function relativeBottom(el: Element, root: HTMLElement, scale = 1): number {
    const rootRect = root.getBoundingClientRect();
    const rect = el.getBoundingClientRect();
    const safeScale = Math.max(0.01, Number(scale) || 1);
    return (rect.bottom - rootRect.top) / safeScale + (root.scrollTop || 0);
}

interface RowMeasure {
    row: HTMLTableRowElement;
    top: number;
    bottom: number;
    height: number;
}

function measureRows(table: HTMLTableElement, root: HTMLElement, scale = 1): RowMeasure[] {
    return getDirectRows(table).map((row) => {
        const top = relativeTop(row, root, scale);
        const bottom = relativeBottom(row, root, scale);
        return {
            row,
            top,
            bottom,
            height: Math.max(1, bottom - top)
        };
    });
}

/** Measure per-column widths so split fragments stay visually consistent. */
function measureColumnWidthsPx(table: HTMLTableElement, scale = 1): number[] {
    const safeScale = Math.max(0.01, Number(scale) || 1);
    const rows = Array.from(table.rows);
    if (!rows.length) {
        return [];
    }
    let columnCount = 0;
    Array.from(rows[0].cells).forEach((cell) => {
        columnCount += Math.max(1, Number(cell.colSpan) || 1);
    });
    if (columnCount <= 0) {
        return [];
    }
    const widths = Array.from({ length: columnCount }, () => 0);
    rows.forEach((row) => {
        let column = 0;
        Array.from(row.cells).forEach((cell) => {
            const span = Math.max(1, Number(cell.colSpan) || 1);
            const total = Math.max(span, cell.getBoundingClientRect().width / safeScale);
            const each = total / span;
            for (let i = 0; i < span && column < columnCount; i += 1, column += 1) {
                widths[column] = Math.max(widths[column], Math.round(each));
            }
        });
    });
    return widths.map((width) => Math.max(1, width));
}

function zipMaxWidths(current: number[], next: number[]): number[] {
    const length = Math.max(current.length, next.length);
    const widths = Array.from({ length }, (_, index) => 0);
    for (let i = 0; i < length; i += 1) {
        widths[i] = Math.max(1, Number(current[i]) || 0, Number(next[i]) || 0);
    }
    return widths;
}

function scaleWidthsToTotal(widths: number[], totalPx: number): number[] {
    if (!widths.length) {
        return widths;
    }
    const safeTotal = Math.max(widths.length, Math.round(totalPx));
    const sum = widths.reduce((acc, width) => acc + Math.max(1, width), 0) || widths.length;
    const scaled = widths.map((width) => Math.max(1, Math.round((Math.max(1, width) * safeTotal) / sum)));
    const scaledSum = scaled.reduce((acc, width) => acc + width, 0);
    if (scaledSum !== safeTotal && scaled.length) {
        scaled[scaled.length - 1] = Math.max(1, scaled[scaled.length - 1] + (safeTotal - scaledSum));
    }
    return scaled;
}

function resolveAvailableTableWidthPx(table: HTMLTableElement): number {
    const figure = table.closest('figure') as HTMLElement | null;
    if (figure && figure.clientWidth > 0) {
        return figure.clientWidth;
    }
    const host = table.parentElement as HTMLElement | null;
    if (host && host.clientWidth > 0) {
        const styles = getComputedStyle(host);
        const pad =
            (parseFloat(styles.paddingLeft) || 0) + (parseFloat(styles.paddingRight) || 0);
        return Math.max(1, host.clientWidth - pad);
    }
    const rectWidth = table.getBoundingClientRect().width;
    return rectWidth > 0 ? Math.round(rectWidth) : 0;
}

/**
 * Clear prior locks, let the table reflow at full width, then return shared
 * column widths scaled to the available page content width.
 */
function measureSharedColumnWidthsPx(table: HTMLTableElement, scale = 1): number[] {
    clearLockedColumnWidths(table);
    table.style.width = '100%';
    table.style.maxWidth = '100%';
    table.style.tableLayout = 'auto';
    void table.offsetHeight;
    const measured = measureColumnWidthsPx(table, scale);
    if (!measured.length) {
        return measured;
    }
    const available = Math.round(resolveAvailableTableWidthPx(table));
    if (available > 0) {
        return scaleWidthsToTotal(measured, available);
    }
    return measured;
}

/**
 * After splits (or content edits), force every fragment in a group to share the
 * widest measured column widths so page N and page N+1 stay aligned.
 */
export function syncBodyTableGroupColumnWidths(root: HTMLElement, scale = 1): void {
    if (!root || typeof document === 'undefined') {
        return;
    }
    const groups = new Map<string, HTMLTableElement[]>();
    root.querySelectorAll(`table[${TEMPLATE_TABLE_GROUP_ATTR}]`).forEach((node) => {
        const table = node as HTMLTableElement;
        if (table.closest('.inline-table-shell, .floating-field, .word-editor-value, .ql-editor, .template-word-editor-body, .document-body-word-editor, [data-body-word-editor], .ck-editor')) {
            return;
        }
        const groupId = table.getAttribute(TEMPLATE_TABLE_GROUP_ATTR) || '';
        if (!groupId) {
            return;
        }
        const list = groups.get(groupId) || [];
        list.push(table);
        groups.set(groupId, list);
    });

    groups.forEach((tables) => {
        tables.forEach((table) => {
            clearLockedColumnWidths(table);
            table.style.width = '100%';
            table.style.maxWidth = '100%';
            table.style.marginLeft = '0';
            table.style.marginRight = '0';
            table.style.tableLayout = 'auto';
        });
        void root.offsetHeight;

        let maxWidths: number[] = [];
        tables.forEach((table) => {
            maxWidths = zipMaxWidths(maxWidths, measureColumnWidthsPx(table, scale));
        });
        if (!maxWidths.length) {
            return;
        }
        const available = Math.round(resolveAvailableTableWidthPx(tables[0]));
        const locked = available > 0 ? scaleWidthsToTotal(maxWidths, available) : maxWidths;
        tables.forEach((table) => applyColumnWidths(table, locked));
    });
}

function stripCellWidthOverrides(table: HTMLTableElement): void {
    table.querySelectorAll('td, th').forEach((node) => {
        const cell = node as HTMLElement;
        if (cell.style.width) {
            cell.style.width = '';
        }
        if (cell.style.minWidth) {
            cell.style.minWidth = '';
        }
        if (cell.style.maxWidth) {
            cell.style.maxWidth = '';
        }
        cell.removeAttribute('width');
    });
}

function applyColumnWidths(table: HTMLTableElement, widths: number[]): void {
    if (!table || !widths.length) {
        return;
    }
    table.querySelectorAll('colgroup').forEach((node) => node.remove());
    const total = widths.reduce((sum, width) => sum + Math.max(1, width), 0) || widths.length;
    const colgroup = document.createElement('colgroup');
    widths.forEach((width) => {
        const col = document.createElement('col');
        // Percent locks keep the table full-bleed; absolute px totals shrink/center in PDF/email.
        col.style.width = `${(Math.max(1, width) / total) * 100}%`;
        colgroup.appendChild(col);
    });
    table.insertBefore(colgroup, table.firstChild);
    stripCellWidthOverrides(table);
    table.style.tableLayout = 'fixed';
    table.style.width = '100%';
    table.style.maxWidth = '100%';
    table.style.marginLeft = '0';
    table.style.marginRight = '0';
}

/** Drop previous split locks so content can reflow before the next measure. */
export function clearLockedColumnWidths(table: HTMLTableElement | null | undefined): void {
    if (!table) {
        return;
    }
    table.querySelectorAll('colgroup').forEach((node) => node.remove());
    stripCellWidthOverrides(table);
    if (table.style.tableLayout === 'fixed') {
        table.style.tableLayout = '';
    }
    if (table.style.width) {
        table.style.width = '';
    }
}

function buildSplitFigure(
    sourceFigure: HTMLElement,
    sourceTable: HTMLTableElement,
    keepRows: HTMLTableRowElement[],
    options: {
        groupId: string;
        continuation: boolean;
        headerRows: HTMLTableRowElement[];
        repeatHeader: boolean;
        sourceHasThead: boolean;
        columnWidths?: number[];
    }
): HTMLElement {
    const figure = sourceFigure.cloneNode(false) as HTMLElement;
    if (!figure.classList.contains('table')) {
        figure.classList.add('table');
    }
    figure.setAttribute(TEMPLATE_TABLE_GROUP_ATTR, options.groupId);
    if (options.continuation) {
        figure.setAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR, 'true');
    } else {
        figure.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
    }

    const table = sourceTable.cloneNode(false) as HTMLTableElement;
    Array.from(sourceTable.attributes).forEach((attr) => {
        table.setAttribute(attr.name, attr.value);
    });
    table.setAttribute(TEMPLATE_TABLE_GROUP_ATTR, options.groupId);
    if (options.continuation) {
        table.setAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR, 'true');
    } else {
        table.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
    }

    const headerSet = new Set(options.headerRows);
    const wantRepeatedHeader =
        options.continuation && options.repeatHeader && options.headerRows.length > 0;
    const wantNativeHeader =
        !options.continuation && options.sourceHasThead && options.headerRows.length > 0;

    if (wantRepeatedHeader || wantNativeHeader) {
        const thead = document.createElement('thead');
        options.headerRows.forEach((row) => {
            thead.appendChild(cloneRow(row, wantRepeatedHeader));
        });
        table.appendChild(thead);
    }

    const tbody = document.createElement('tbody');
    keepRows.forEach((row) => {
        if ((wantRepeatedHeader || wantNativeHeader) && headerSet.has(row)) {
            return;
        }
        tbody.appendChild(cloneRow(row, false));
    });
    table.appendChild(tbody);
    if (options.columnWidths?.length) {
        applyColumnWidths(table, options.columnWidths);
    }
    figure.appendChild(table);
    return figure;
}

/**
 * Pack rows into page fragments using measured tops/bottoms (first pass) and
 * packed heights for subsequent pages (after an implicit page break).
 *
 * A row/cell that would be sliced by the page edge is moved entirely to the
 * next page whenever it fits on a full page. Only rows taller than one page
 * are allowed to overflow.
 */
function computeFragments(
    measures: RowMeasure[],
    pageHeight: number,
    pageGap: number,
    epsilon: number,
    headerCount: number,
    repeatHeader: boolean
): Array<{ start: number; end: number }> {
    const rows = measures.map((m) => m.row);
    const fragments: Array<{ start: number; end: number }> = [];
    let index = 0;

    while (index < measures.length) {
        const isContinuation = fragments.length > 0;
        const headerExtra =
            isContinuation && repeatHeader
                ? measures.slice(0, headerCount).reduce((sum, m) => sum + m.height, 0)
                : 0;
        const pageCapacity = Math.max(1, pageHeight - headerExtra);

        let pageIdx = pageIndexForY(measures[index].top, pageHeight, pageGap);
        let limit = pageBottom(pageIdx, pageHeight, pageGap);

        // If this row starts in the gray gap, pack onto the next page.
        if (measures[index].top >= pageTop(pageIdx, pageHeight, pageGap) + pageHeight - epsilon) {
            pageIdx += 1;
            limit = pageBottom(pageIdx, pageHeight, pageGap);
        }

        let remaining = isContinuation
            ? pageCapacity
            : limit - measures[index].top;

        let end = index - 1;
        let cursor = index;
        let used = 0;
        /** When the first block must start on the next page, pack with synthetic Y. */
        let packFromNextPage = false;
        let packCursorY = 0;

        while (cursor < measures.length) {
            const blockEnd = getRowSpanBlockEnd(rows, cursor);
            const blockHeight = measures
                .slice(cursor, blockEnd + 1)
                .reduce((sum, m) => sum + m.height, 0);

            const overflowsCurrent = packFromNextPage
                ? used + blockHeight > pageCapacity + epsilon
                : isContinuation
                    ? used + blockHeight > remaining + epsilon
                    : measures[blockEnd].bottom > limit + epsilon
                        || used + blockHeight > remaining + epsilon;

            if (overflowsCurrent) {
                if (end >= index) {
                    // Already have rows on this page — move this whole block to the next page.
                    break;
                }

                // First block of this fragment would be cut on the current page.
                const atPageTop =
                    packFromNextPage
                    || measures[cursor].top
                        <= pageTop(pageIdx, pageHeight, pageGap) + Math.max(epsilon, 8);

                if (!atPageTop && !packFromNextPage) {
                    // Start mid-page and would be sliced — relocate to the next page top
                    // even when the row is taller than one page.
                    packFromNextPage = true;
                    packCursorY = pageTop(pageIdx + 1, pageHeight, pageGap);
                    remaining = pageCapacity;
                    used = 0;
                    continue;
                }

                if (blockHeight <= pageCapacity + epsilon) {
                    // Fits on a clean next page — never keep a sliced cell here.
                    if (!isContinuation && !packFromNextPage) {
                        packFromNextPage = true;
                        packCursorY = pageTop(pageIdx + 1, pageHeight, pageGap);
                        remaining = pageCapacity;
                        used = 0;
                        continue;
                    }
                    // Continuation with no room even for the first body block: end empty → force below.
                    break;
                }

                // Already at page top and taller than one page: unavoidable overflow.
                end = blockEnd;
                used += blockHeight;
                cursor = blockEnd + 1;
                break;
            }

            end = blockEnd;
            used += blockHeight;
            if (packFromNextPage) {
                packCursorY += blockHeight;
            }
            cursor = blockEnd + 1;
        }

        if (end < index) {
            const blockEnd = getRowSpanBlockEnd(rows, index);
            const blockHeight = measures
                .slice(index, blockEnd + 1)
                .reduce((sum, m) => sum + m.height, 0);
            // Only force-keep when the block cannot fit on any single page.
            if (blockHeight > pageCapacity + epsilon) {
                end = blockEnd;
            } else {
                // Place on this fragment; caller adds a leading spacer when needed so
                // the row starts on the next white page instead of being cut.
                end = blockEnd;
            }
        }

        fragments.push({ start: index, end });
        index = end + 1;
    }

    return fragments;
}

/**
 * Ensure no fragment keeps a row that is sliced by a page bottom. Such rows move
 * to the following fragment (and then to the next page via spacer), including
 * rows taller than one page that currently start mid-page.
 */
function splitFragmentsBeforeCutRows(
    measures: RowMeasure[],
    fragments: Array<{ start: number; end: number }>,
    pageHeight: number,
    pageGap: number,
    epsilon: number
): Array<{ start: number; end: number }> {
    if (!fragments.length) {
        return fragments;
    }
    const refined: Array<{ start: number; end: number }> = [];
    fragments.forEach((frag) => {
        let start = frag.start;
        for (let i = frag.start; i <= frag.end; i++) {
            const pageIdx = pageIndexForY(measures[i].top, pageHeight, pageGap);
            const limit = pageBottom(pageIdx, pageHeight, pageGap);
            const pageStart = pageTop(pageIdx, pageHeight, pageGap);
            const cut =
                measures[i].top < limit - epsilon
                && measures[i].bottom > limit + epsilon;
            // Mid-fragment cut → close previous rows, start a new fragment with this row.
            // Also split when a later row starts clearly below the page top but still
            // crosses the edge (do not leave a sliver above the gutter).
            if (cut && i > start) {
                refined.push({ start, end: i - 1 });
                start = i;
            } else if (
                cut
                && i === start
                && measures[i].top > pageStart + Math.max(epsilon, 8)
            ) {
                // First row of fragment starts mid-page and is cut — keep as its own
                // fragment so leading-spacer / next-page packing can relocate it.
                start = i;
            }
        }
        if (start <= frag.end) {
            refined.push({ start, end: frag.end });
        }
    });
    return refined.length ? refined : fragments;
}

/**
 * Hard cap: no fragment may keep more row-height than one white page (minus
 * repeated header). Catches tall cells that continuous Y-measure missed after
 * collapse, so they move to the next page instead of painting into the gutter.
 */
function splitFragmentsByPageCapacity(
    measures: RowMeasure[],
    fragments: Array<{ start: number; end: number }>,
    pageHeight: number,
    epsilon: number,
    headerCount: number,
    repeatHeader: boolean
): Array<{ start: number; end: number }> {
    if (!fragments.length) {
        return fragments;
    }
    const headerHeight =
        repeatHeader && headerCount > 0
            ? measures.slice(0, headerCount).reduce((sum, m) => sum + m.height, 0)
            : 0;
    const refined: Array<{ start: number; end: number }> = [];

    fragments.forEach((frag, fragIndex) => {
        const capacity = Math.max(
            1,
            pageHeight - (fragIndex > 0 ? headerHeight : 0)
        );
        let start = frag.start;
        let used = 0;
        for (let i = frag.start; i <= frag.end; i++) {
            const height = Math.max(1, measures[i].height);
            if (used > 0 && used + height > capacity + epsilon) {
                refined.push({ start, end: i - 1 });
                start = i;
                used = 0;
            }
            used += height;
            // Row taller than a full page: keep alone (unavoidable overflow).
            if (used > capacity + epsilon && i === start) {
                refined.push({ start, end: i });
                start = i + 1;
                used = 0;
            }
        }
        if (start <= frag.end) {
            refined.push({ start, end: frag.end });
        }
    });

    return refined.length ? refined : fragments;
}

function refineTableFragments(
    measures: RowMeasure[],
    fragments: Array<{ start: number; end: number }>,
    pageHeight: number,
    pageGap: number,
    epsilon: number,
    headerCount: number,
    repeatHeader: boolean
): Array<{ start: number; end: number }> {
    return splitFragmentsByPageCapacity(
        measures,
        splitFragmentsBeforeCutRows(
            measures,
            fragments,
            pageHeight,
            pageGap,
            epsilon
        ),
        pageHeight,
        epsilon,
        headerCount,
        repeatHeader
    );
}

function splitTableFigureInPlace(
    figure: HTMLElement,
    root: HTMLElement,
    options: BodyTablePaginationOptions
): boolean {
    const table = getTableRoot(figure);
    if (!table) {
        return false;
    }

    const pageHeight = Math.max(1, Number(options.pageHeight) || 1123);
    const pageGap = Math.max(0, Number(options.pageGap) || 0);
    const epsilon = options.epsilonPx ?? DEFAULT_EPSILON;
    const repeatHeader = options.repeatHeader !== false;
    const scale = options.coordinateScale ?? 1;

    const columnWidths = measureSharedColumnWidthsPx(table, scale);
    void table.offsetHeight;

    const measures = measureRows(table, root, scale);
    if (!measures.length) {
        return false;
    }

    const allRows = measures.map((m) => m.row);
    const headerCount = getHeaderRowCount(allRows);
    const headerRows = allRows.slice(0, headerCount);
    const sourceHasThead = !!table.tHead;

    const firstTop = measures[0].top;
    const lastBottom = measures[measures.length - 1].bottom;
    const startPage = pageIndexForY(firstTop, pageHeight, pageGap);
    const startLimit = pageBottom(startPage, pageHeight, pageGap);

    // Entire table fits on one white band.
    if (lastBottom <= startLimit + epsilon) {
        // Starts inside the gap stripe — push to next page with a spacer.
        if (firstTop >= pageTop(startPage, pageHeight, pageGap) + pageHeight - epsilon) {
            const nextTop = pageTop(startPage + 1, pageHeight, pageGap);
            const spacerHeight = nextTop - firstTop;
            if (spacerHeight > epsilon) {
                figure.parentElement?.insertBefore(buildPageBreakSpacer(spacerHeight), figure);
                return true;
            }
        }
        return false;
    }

    let fragments = refineTableFragments(
        measures,
        computeFragments(
            measures,
            pageHeight,
            pageGap,
            epsilon,
            headerCount,
            repeatHeader
        ),
        pageHeight,
        pageGap,
        epsilon,
        headerCount,
        repeatHeader
    );

    const firstFragRow = measures[fragments[0]?.start ?? 0];
    const firstFragRowLimit = pageBottom(
        pageIndexForY(firstFragRow?.top ?? firstTop, pageHeight, pageGap),
        pageHeight,
        pageGap
    );
    const firstFragRowPageStart = pageTop(
        pageIndexForY(firstFragRow?.top ?? firstTop, pageHeight, pageGap),
        pageHeight,
        pageGap
    );
    const firstFragRowCut = !!firstFragRow
        && firstFragRow.top < firstFragRowLimit - epsilon
        && firstFragRow.bottom > firstFragRowLimit + epsilon
        && firstFragRow.top > firstFragRowPageStart + Math.max(epsilon, 8);

    if (fragments.length <= 1) {
        // Still overflows one page visually — force at least a leading push if needed.
        if (firstTop + measures[0].height > startLimit + epsilon || firstFragRowCut) {
            const nextTop = pageTop(startPage + 1, pageHeight, pageGap);
            const spacerHeight = nextTop - firstTop;
            if (spacerHeight > epsilon && fragments.length === 1) {
                // Recompute after conceptual push: if multiple rows remain, split by packed height.
                const shiftedMeasures = measures.map((m, i) => ({
                    ...m,
                    top: nextTop + measures.slice(0, i).reduce((s, x) => s + x.height, 0),
                    bottom:
                        nextTop +
                        measures.slice(0, i + 1).reduce((s, x) => s + x.height, 0)
                }));
                const packed = refineTableFragments(
                    shiftedMeasures,
                    computeFragments(
                        shiftedMeasures,
                        pageHeight,
                        pageGap,
                        epsilon,
                        headerCount,
                        repeatHeader
                    ),
                    pageHeight,
                    pageGap,
                    epsilon,
                    headerCount,
                    repeatHeader
                );
                if (packed.length > 1) {
                    return applyFragments(
                        figure,
                        table,
                        measures,
                        packed,
                        {
                            pageHeight,
                            pageGap,
                            epsilon,
                            repeatHeader,
                            headerRows,
                            sourceHasThead,
                            leadingSpacerHeight: spacerHeight,
                            columnWidths
                        }
                    );
                }
                figure.parentElement?.insertBefore(buildPageBreakSpacer(spacerHeight), figure);
                return true;
            }
        }
        return false;
    }

    // Leading spacer when the first fragment cannot start at its current Y
    // without cutting a row across the page edge.
    let leadingSpacerHeight = 0;
    const firstFragEndBottom = measures[fragments[0].end].bottom;
    if (
        fragments[0].start === 0
        && (firstFragEndBottom > startLimit + epsilon || firstFragRowCut)
    ) {
        // First rows don't fit — move whole table start to next page.
        const nextTop = pageTop(startPage + 1, pageHeight, pageGap);
        leadingSpacerHeight = Math.max(0, nextTop - firstTop);
        const shiftedMeasures = measures.map((m, i) => ({
            ...m,
            top: nextTop + measures.slice(0, i).reduce((s, x) => s + x.height, 0),
            bottom:
                nextTop + measures.slice(0, i + 1).reduce((s, x) => s + x.height, 0)
        }));
        const shifted = refineTableFragments(
            shiftedMeasures,
            computeFragments(
                shiftedMeasures,
                pageHeight,
                pageGap,
                epsilon,
                headerCount,
                repeatHeader
            ),
            pageHeight,
            pageGap,
            epsilon,
            headerCount,
            repeatHeader
        );
        return applyFragments(figure, table, measures, shifted.length ? shifted : fragments, {
            pageHeight,
            pageGap,
            epsilon,
            repeatHeader,
            headerRows,
            sourceHasThead,
            leadingSpacerHeight,
            columnWidths
        });
    }

    return applyFragments(figure, table, measures, fragments, {
        pageHeight,
        pageGap,
        epsilon,
        repeatHeader,
        headerRows,
        sourceHasThead,
        leadingSpacerHeight,
        columnWidths
    });
}

function applyFragments(
    figure: HTMLElement,
    table: HTMLTableElement,
    measures: RowMeasure[],
    fragments: Array<{ start: number; end: number }>,
    ctx: {
        pageHeight: number;
        pageGap: number;
        epsilon: number;
        repeatHeader: boolean;
        headerRows: HTMLTableRowElement[];
        sourceHasThead: boolean;
        leadingSpacerHeight: number;
        columnWidths?: number[];
    }
): boolean {
    const parent = figure.parentElement;
    if (!parent || !fragments.length) {
        return false;
    }

    const groupId = createGroupId();
    const columnWidths = ctx.columnWidths?.length
        ? ctx.columnWidths
        : measureColumnWidthsPx(table);
    const nodesToInsert: Node[] = [];

    if (ctx.leadingSpacerHeight > ctx.epsilon) {
        nodesToInsert.push(buildPageBreakSpacer(ctx.leadingSpacerHeight));
    }

    // Place fragments with spacers derived from packed heights so later cuts stay
    // aligned to real page tops (collapsed Y drifts once earlier spacers are inserted).
    const headerHeight =
        ctx.repeatHeader && ctx.headerRows.length
            ? ctx.headerRows.reduce((sum, row) => {
                const match = measures.find((m) => m.row === row);
                return sum + (match ? match.height : 0);
            }, 0)
            : 0;
    let placedBottom =
        ctx.leadingSpacerHeight > ctx.epsilon
            ? measures[fragments[0].start].top + ctx.leadingSpacerHeight
            : measures[fragments[0].start].top;

    fragments.forEach((frag, fragIndex) => {
        const isContinuation = fragIndex > 0;
        let rows = measures.slice(frag.start, frag.end + 1).map((m) => m.row);
        if (isContinuation) {
            rows = rows.filter((row) => !ctx.headerRows.includes(row));
            if (!rows.length) {
                return;
            }
        }

        const bodyHeight = rows.reduce((sum, row) => {
            const match = measures.find((m) => m.row === row);
            return sum + (match ? match.height : 0);
        }, 0);
        const fragHeight = bodyHeight + (isContinuation ? headerHeight : 0);

        if (isContinuation) {
            const pageIdx = pageIndexForY(Math.max(0, placedBottom - ctx.epsilon), ctx.pageHeight, ctx.pageGap);
            const targetTop = pageTop(pageIdx + 1, ctx.pageHeight, ctx.pageGap);
            const spacerHeight = Math.max(ctx.pageGap, targetTop - placedBottom);
            nodesToInsert.push(buildPageBreakSpacer(spacerHeight));
            placedBottom = targetTop + fragHeight;
        } else {
            placedBottom = (ctx.leadingSpacerHeight > ctx.epsilon
                ? pageTop(
                    pageIndexForY(measures[frag.start].top, ctx.pageHeight, ctx.pageGap) + 1,
                    ctx.pageHeight,
                    ctx.pageGap
                )
                : measures[frag.start].top) + fragHeight;
        }

        nodesToInsert.push(
            buildSplitFigure(figure, table, rows, {
                groupId,
                continuation: isContinuation,
                headerRows: ctx.headerRows,
                repeatHeader: ctx.repeatHeader,
                sourceHasThead: ctx.sourceHasThead,
                columnWidths
            })
        );
    });

    if (!nodesToInsert.length) {
        return false;
    }

    const marker = document.createElement('span');
    parent.insertBefore(marker, figure);
    figure.remove();
    nodesToInsert.forEach((node) => parent.insertBefore(node, marker));
    marker.remove();
    return true;
}

/**
 * Collapse existing pagination markers in a live root (groups + spacers).
 */
export function collapseBodyTablesInRoot(root: HTMLElement): boolean {
    if (!root) {
        return false;
    }
    let changed = false;

    const groupIds = new Set<string>();
    root.querySelectorAll(`[${TEMPLATE_TABLE_GROUP_ATTR}]`).forEach((node) => {
        const id = node.getAttribute(TEMPLATE_TABLE_GROUP_ATTR);
        if (id) {
            groupIds.add(id);
        }
    });

    groupIds.forEach((groupId) => {
        const nodes = Array.from(
            root.querySelectorAll(`[${TEMPLATE_TABLE_GROUP_ATTR}="${groupId}"]`)
        ) as HTMLElement[];
        const figures = nodes.filter((n) => {
            const tag = n.tagName.toLowerCase();
            return tag === 'figure' || (tag === 'table' && !n.closest('figure.table'));
        });
        if (figures.length <= 1) {
            figures[0]?.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
            figures[0]?.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
            const t = figures[0] ? getTableRoot(figures[0]) : null;
            t?.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
            t?.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
            return;
        }
        figures.sort((a, b) =>
            a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1
        );
        const primary = ensureFigureWrapper(figures[0]);
        const primaryTable = getTableRoot(primary);
        if (!primaryTable) {
            return;
        }
        for (let i = 1; i < figures.length; i++) {
            const frag = figures[i];
            let sibling = primary.nextElementSibling;
            while (sibling && sibling !== frag) {
                const next = sibling.nextElementSibling;
                if (isPageBreakElement(sibling)) {
                    sibling.remove();
                    changed = true;
                }
                sibling = next;
            }
            const table = getTableRoot(frag);
            if (table) {
                getDirectRows(table).forEach((row) => {
                    if (row.getAttribute(TEMPLATE_TABLE_HEADER_CLONE_ATTR) === 'true') {
                        return;
                    }
                    const section = primaryTable.tBodies[0] || primaryTable;
                    section.appendChild(row);
                });
            }
            frag.remove();
            changed = true;
        }
        primary.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
        primary.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
        primaryTable.removeAttribute(TEMPLATE_TABLE_CONTINUATION_ATTR);
        primaryTable.removeAttribute(TEMPLATE_TABLE_GROUP_ATTR);
        clearLockedColumnWidths(primaryTable);
    });

    root.querySelectorAll(`[${TEMPLATE_PAGE_BREAK_ATTR}="true"]`).forEach((node) => {
        node.remove();
        changed = true;
    });

    root.querySelectorAll('figure.table > table, table').forEach((node) => {
        if ((node as HTMLElement).closest?.('.inline-table-shell, .floating-field, .template-word-editor-body, .document-body-word-editor, [data-body-word-editor], .ck-editor, .word-editor-value, .ql-editor')) {
            return;
        }
        clearLockedColumnWidths(node as HTMLTableElement);
    });

    return changed;
}

/**
 * Paginate tables inside a live rooted element (CKEditor editable or fill document root).
 * Mutates the DOM in place. Returns true if any change was made.
 */
export function paginateBodyTablesInRoot(
    root: HTMLElement,
    options: BodyTablePaginationOptions
): boolean {
    if (!root || typeof document === 'undefined') {
        return false;
    }

    let changed = collapseBodyTablesInRoot(root);
    void root.offsetHeight;

    const starts = collectLogicalTableStarts(root);
    starts.forEach((node) => {
        const figure = ensureFigureWrapper(node);
        if (splitTableFigureInPlace(figure, root, options)) {
            changed = true;
        }
    });

    // Re-measure across every fragment so a wide cell on page 2 grows that
    // column on page 1 as well (and the reverse).
    syncBodyTableGroupColumnWidths(root, options.coordinateScale ?? 1);

    return changed;
}

export interface BodyTableSplitPlan {
    tableIndex: number;
    headerCount: number;
    sourceHasThead: boolean;
    leadingSpacerHeight: number;
    fragments: Array<{ start: number; end: number }>;
    /** Measured bottoms of the last row in each fragment (live editable coords). */
    fragmentBottoms: number[];
    /** Shared column widths (px) applied to every fragment. */
    columnWidths?: number[];
}

/**
 * Build split plans from a live editor/root so cuts match what the user sees.
 */
export function planBodyTableSplitsFromRoot(
    root: HTMLElement,
    options: BodyTablePaginationOptions
): BodyTableSplitPlan[] {
    if (!root) {
        return [];
    }
    const pageHeight = Math.max(1, Number(options.pageHeight) || 1123);
    const pageGap = Math.max(0, Number(options.pageGap) || 0);
    const epsilon = options.epsilonPx ?? DEFAULT_EPSILON;
    const repeatHeader = options.repeatHeader !== false;
    const plans: BodyTableSplitPlan[] = [];

    const starts = collectLogicalTableStarts(root);
    starts.forEach((node, tableIndex) => {
        const figure = node.tagName.toLowerCase() === 'figure' ? node : (node.closest('figure.table') as HTMLElement) || node;
        const table = getTableRoot(figure);
        if (!table) {
            return;
        }
        const scale = options.coordinateScale ?? 1;
        const columnWidths = measureSharedColumnWidthsPx(table, scale);
        void table.offsetHeight;
        const measures = measureRows(table, root, scale);
        if (!measures.length) {
            return;
        }
        const allRows = measures.map((m) => m.row);
        const headerCount = getHeaderRowCount(allRows);
        const sourceHasThead = !!table.tHead;
        const firstTop = measures[0].top;
        const lastBottom = measures[measures.length - 1].bottom;
        const startPage = pageIndexForY(firstTop, pageHeight, pageGap);
        const startLimit = pageBottom(startPage, pageHeight, pageGap);

        if (lastBottom <= startLimit + epsilon) {
            if (firstTop >= pageTop(startPage, pageHeight, pageGap) + pageHeight - epsilon) {
                const nextTop = pageTop(startPage + 1, pageHeight, pageGap);
                const spacerHeight = nextTop - firstTop;
                if (spacerHeight > epsilon) {
                    plans.push({
                        tableIndex,
                        headerCount,
                        sourceHasThead,
                        leadingSpacerHeight: spacerHeight,
                        fragments: [{ start: 0, end: measures.length - 1 }],
                        fragmentBottoms: [lastBottom],
                        columnWidths
                    });
                }
            }
            return;
        }

        let leadingSpacerHeight = 0;
        let working = measures;
        const firstFragProbe = refineTableFragments(
            measures,
            computeFragments(
                measures,
                pageHeight,
                pageGap,
                epsilon,
                headerCount,
                repeatHeader
            ),
            pageHeight,
            pageGap,
            epsilon,
            headerCount,
            repeatHeader
        );
        const probeFirst = measures[firstFragProbe[0]?.start ?? 0];
        const probeLimit = pageBottom(
            pageIndexForY(probeFirst?.top ?? firstTop, pageHeight, pageGap),
            pageHeight,
            pageGap
        );
        const probePageStart = pageTop(
            pageIndexForY(probeFirst?.top ?? firstTop, pageHeight, pageGap),
            pageHeight,
            pageGap
        );
        const probeCut = !!probeFirst
            && probeFirst.top < probeLimit - epsilon
            && probeFirst.bottom > probeLimit + epsilon
            && probeFirst.top > probePageStart + Math.max(epsilon, 8);
        if (
            firstFragProbe.length
            && firstFragProbe[0].start === 0
            && (
                measures[firstFragProbe[0].end].bottom > startLimit + epsilon
                || firstTop + measures[0].height > startLimit + epsilon
                || probeCut
            )
        ) {
            const nextTop = pageTop(startPage + 1, pageHeight, pageGap);
            leadingSpacerHeight = Math.max(0, nextTop - firstTop);
            working = measures.map((m, i) => ({
                ...m,
                top: nextTop + measures.slice(0, i).reduce((s, x) => s + x.height, 0),
                bottom: nextTop + measures.slice(0, i + 1).reduce((s, x) => s + x.height, 0)
            }));
        }

        const fragments = refineTableFragments(
            working,
            computeFragments(
                working,
                pageHeight,
                pageGap,
                epsilon,
                headerCount,
                repeatHeader
            ),
            pageHeight,
            pageGap,
            epsilon,
            headerCount,
            repeatHeader
        );
        if (fragments.length <= 1 && leadingSpacerHeight <= epsilon) {
            return;
        }

        const bottomsSource = working;
        plans.push({
            tableIndex,
            headerCount,
            sourceHasThead,
            leadingSpacerHeight,
            fragments: fragments.length ? fragments : [{ start: 0, end: measures.length - 1 }],
            fragmentBottoms: (fragments.length ? fragments : [{ start: 0, end: measures.length - 1 }]).map(
                (frag) => bottomsSource[frag.end].bottom
            ),
            columnWidths
        });
    });

    return plans;
}

/**
 * Apply live-measured split plans onto collapsed HTML (no re-measure).
 */
export function applyBodyTableSplitPlansToHtml(
    html: string,
    plans: BodyTableSplitPlan[],
    options: BodyTablePaginationOptions
): string {
    if (!html || !plans.length || typeof document === 'undefined') {
        return html || '';
    }

    const pageHeight = Math.max(1, Number(options.pageHeight) || 1123);
    const pageGap = Math.max(0, Number(options.pageGap) || 0);
    const epsilon = options.epsilonPx ?? DEFAULT_EPSILON;
    const repeatHeader = options.repeatHeader !== false;

    const host = document.createElement('div');
    host.innerHTML = collapseBodyTablePagination(html);
    const starts = collectLogicalTableStarts(host);

    plans.forEach((plan) => {
        const node = starts[plan.tableIndex];
        if (!node) {
            return;
        }
        const figure = ensureFigureWrapper(node);
        const table = getTableRoot(figure);
        if (!table || !figure.parentElement) {
            return;
        }
        const rows = getDirectRows(table);
        if (!rows.length) {
            return;
        }
        const headerRows = rows.slice(0, plan.headerCount);
        const groupId = createGroupId();
        const nodesToInsert: Node[] = [];

        if (plan.leadingSpacerHeight > epsilon) {
            nodesToInsert.push(buildPageBreakSpacer(plan.leadingSpacerHeight));
        }

        let placedBottom = plan.fragmentBottoms[0] ?? 0;
        plan.fragments.forEach((frag, fragIndex) => {
            const isContinuation = fragIndex > 0;
            let keep = rows.slice(frag.start, frag.end + 1);
            if (isContinuation) {
                keep = keep.filter((row) => !headerRows.includes(row));
                if (!keep.length) {
                    return;
                }
                const targetPage = pageIndexForY(Math.max(0, placedBottom - epsilon), pageHeight, pageGap) + 1;
                const targetTop = pageTop(targetPage, pageHeight, pageGap);
                const spacerHeight = Math.max(pageGap, targetTop - placedBottom);
                nodesToInsert.push(buildPageBreakSpacer(spacerHeight));
                const plannedBottom = plan.fragmentBottoms[fragIndex];
                const plannedPrev = plan.fragmentBottoms[fragIndex - 1];
                const fragHeight = plannedBottom != null && plannedPrev != null
                    ? Math.max(0, plannedBottom - plannedPrev)
                    : 0;
                placedBottom = targetTop + fragHeight;
            }

            nodesToInsert.push(
                buildSplitFigure(figure, table, keep, {
                    groupId,
                    continuation: isContinuation,
                    headerRows,
                    repeatHeader,
                    sourceHasThead: plan.sourceHasThead,
                    columnWidths: plan.columnWidths
                })
            );
        });

        if (!nodesToInsert.length) {
            return;
        }
        const marker = document.createElement('span');
        figure.parentElement.insertBefore(marker, figure);
        figure.remove();
        nodesToInsert.forEach((n) => marker.parentElement?.insertBefore(n, marker));
        marker.remove();
    });

    return host.innerHTML;
}

/**
 * HTML in → collapsed → measured in an offscreen host mirroring width → paginated HTML out.
 * Prefer {@link paginateBodyTableHtmlFromLiveRoot} for WYSIWYG accuracy.
 */
export function paginateBodyTableHtml(
    html: string,
    options: BodyTablePaginationOptions & { contentWidthPx: number }
): string {
    if (!html || typeof document === 'undefined') {
        return html || '';
    }

    const collapsed = collapseBodyTablePagination(html);
    const host = document.createElement('div');
    // Keep `.document-html` so shared body CSS (paragraph margins, table rules) matches canvas.
    host.className = 'ck-content document-html template-body-table-measure-host';
    host.style.cssText = [
        'position:absolute',
        'left:-100000px',
        'top:0',
        `width:${Math.max(1, options.contentWidthPx)}px`,
        'box-sizing:border-box',
        'padding:22mm 18mm',
        'font-family:Georgia,"Times New Roman",serif',
        'font-size:14px',
        'line-height:1.55',
        'color:#111827',
        'background:#fff',
        'visibility:hidden',
        'pointer-events:none'
    ].join(';');
    host.innerHTML = collapsed;
    document.body.appendChild(host);
    try {
        paginateBodyTablesInRoot(host, options);
        return host.innerHTML;
    } finally {
        host.remove();
    }
}

/**
 * Measure splits from the live editable, then apply them to HTML for setData.
 */
export function paginateBodyTableHtmlFromLiveRoot(
    liveRoot: HTMLElement,
    html: string,
    options: BodyTablePaginationOptions
): string {
    const collapsed = collapseBodyTablePagination(html);
    const plans = planBodyTableSplitsFromRoot(liveRoot, options);
    if (!plans.length) {
        return collapsed;
    }
    return applyBodyTableSplitPlansToHtml(collapsed, plans, options);
}

/** True when HTML already contains pagination markers. */
export function hasBodyTablePaginationMarkers(html: string): boolean {
    return (
        !!html &&
        (html.includes(TEMPLATE_PAGE_BREAK_ATTR) ||
            html.includes(TEMPLATE_TABLE_CONTINUATION_ATTR) ||
            html.includes(TEMPLATE_TABLE_GROUP_ATTR))
    );
}
