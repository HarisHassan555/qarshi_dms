export const Q_TABLE_SIZE_CONTROLS_GUARD = '__qTableSizeControlsGuard';
const Q_TABLE_COL_RESIZER_BOUND = '__qTableColResizerBound';
const MIN_COL_WIDTH_PERCENT = 6;

const TEXTAREA_ONINPUT =
  "this.style.height='auto';this.style.height=this.scrollHeight+'px';this.textContent=this.value";

function stripResizeChromeFromTable(table: HTMLTableElement): HTMLTableElement {
  const clone = table.cloneNode(true) as HTMLTableElement;
  clone.querySelectorAll('.q-table-col-resizer, .q-table-col-resize-layer').forEach((el) => el.remove());
  return clone;
}

export function getTableBlotInnerHtml(node: HTMLElement): string {
  const table = node.querySelector('table');
  if (table) {
    return stripResizeChromeFromTable(table).outerHTML;
  }
  const clone = node.cloneNode(true) as HTMLElement;
  clone.querySelectorAll('.q-table-controls, .q-table-layout, .q-table-main').forEach((el) => el.remove());
  clone.querySelectorAll('.q-table-col-resizer, .q-table-col-resize-layer').forEach((el) => el.remove());
  return clone.innerHTML;
}

/** Remove editor-only table chrome (layout wrappers, +/- controls) from HTML used in preview/PDF. */
export function stripEditorTableChromeFromHtml(html: string): string {
  if (!html || !/q-table-(wrapper|controls|layout|main)/i.test(html)) {
    return html;
  }
  const wrapper = document.createElement('div');
  wrapper.innerHTML = html;

  wrapper.querySelectorAll('.q-table-controls').forEach((el) => el.remove());
  wrapper.querySelectorAll('.q-table-col-resizer, .q-table-col-resize-layer').forEach((el) => el.remove());

  wrapper.querySelectorAll('.q-table-layout').forEach((layout) => {
    const table = layout.querySelector('table');
    if (table && layout.parentElement) {
      layout.parentElement.insertBefore(stripResizeChromeFromTable(table), layout);
      layout.remove();
    } else {
      layout.remove();
    }
  });

  wrapper.querySelectorAll('.q-table-main').forEach((main) => {
    const table = main.querySelector('table');
    if (table && main.parentElement) {
      main.parentElement.insertBefore(stripResizeChromeFromTable(table), main);
      main.remove();
    } else {
      main.remove();
    }
  });

  wrapper.querySelectorAll('.q-table-wrapper').forEach((wrap) => {
    const table = wrap.querySelector('table');
    if (table) {
      wrap.replaceWith(stripResizeChromeFromTable(table));
    } else {
      wrap.remove();
    }
  });

  return wrapper.innerHTML;
}

export function buildEditorTableCellHtml(
  cellTag: 'th' | 'td',
  defaultValue: string,
  options?: { colSpan?: number; rowSpan?: number }
): string {
  const isHeader = cellTag === 'th';
  const cellHeaderStyle = isHeader ? 'background-color:#f1f1f1;' : '';
  const taWeight = isHeader ? 'font-weight:700;' : '';
  const taAlign = isHeader ? 'text-align:center;' : 'text-align:left;';
  const safeValue = String(defaultValue || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
  const colSpan = Number(options?.colSpan || 1);
  const rowSpan = Number(options?.rowSpan || 1);
  const colSpanAttr = Number.isFinite(colSpan) && colSpan > 1 ? ` colspan="${Math.floor(colSpan)}"` : '';
  const rowSpanAttr = Number.isFinite(rowSpan) && rowSpan > 1 ? ` rowspan="${Math.floor(rowSpan)}"` : '';

  return `<${cellTag}${rowSpanAttr}${colSpanAttr} style="border:1px solid #000; padding:1px; vertical-align:top; ${cellHeaderStyle}${taAlign}">
          <textarea
            rows="1"
            oninput="${TEXTAREA_ONINPUT}"
            style="width:100%; border:none; outline:none; background:transparent; font:inherit; padding:1px; line-height:1.2; resize:none; overflow:hidden; white-space:pre-wrap; word-break:break-word; box-sizing:border-box;${taWeight}${taAlign}">${safeValue}</textarea>
        </${cellTag}>`;
}

function getRowVisualColspan(row: HTMLTableRowElement): number {
  return Array.from(row.cells).reduce((sum, cell) => {
    const rawSpan = Number(cell.getAttribute('colspan') || '1');
    const span = Number.isFinite(rawSpan) && rawSpan > 0 ? rawSpan : 1;
    return sum + span;
  }, 0);
}

function getVisualColumnIndex(row: HTMLTableRowElement, cell: HTMLTableCellElement): number {
  let index = 0;
  for (const candidate of Array.from(row.cells)) {
    if (candidate === cell) {
      return index;
    }
    const rawSpan = Number(candidate.getAttribute('colspan') || '1');
    index += Number.isFinite(rawSpan) && rawSpan > 0 ? rawSpan : 1;
  }
  return index;
}

function parseColWidthPercent(col: HTMLTableColElement, tableWidth: number): number {
  const width = (col.style.width || col.getAttribute('width') || '').trim();
  if (width.endsWith('%')) {
    const parsed = Number.parseFloat(width);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  if (width.endsWith('px')) {
    const parsed = Number.parseFloat(width);
    return tableWidth > 0 && Number.isFinite(parsed) ? (parsed / tableWidth) * 100 : 0;
  }
  const numeric = Number.parseFloat(width);
  if (Number.isFinite(numeric) && numeric > 0 && numeric <= 100) {
    return numeric;
  }
  return 0;
}

function ensureColGroup(table: HTMLTableElement): HTMLTableColElement[] {
  if (!table.rows.length) {
    return [];
  }
  const colCount = getRowVisualColspan(table.rows[0]);
  if (colCount <= 0) {
    return [];
  }

  let colgroup = table.querySelector('colgroup');
  if (!colgroup) {
    colgroup = document.createElement('colgroup');
    table.insertBefore(colgroup, table.firstChild);
  }

  while (colgroup.children.length < colCount) {
    colgroup.appendChild(document.createElement('col'));
  }
  while (colgroup.children.length > colCount) {
    colgroup.lastElementChild?.remove();
  }

  const cols = Array.from(colgroup.querySelectorAll('col'));
  const tableWidth = table.getBoundingClientRect().width || table.offsetWidth || 1;
  let assigned = 0;
  cols.forEach((col) => {
    assigned += parseColWidthPercent(col, tableWidth);
  });

  if (assigned <= 0) {
    const even = 100 / colCount;
    cols.forEach((col) => {
      col.style.width = `${even}%`;
    });
  } else if (Math.abs(assigned - 100) > 0.5) {
    const scale = 100 / assigned;
    cols.forEach((col) => {
      const current = parseColWidthPercent(col, tableWidth) || 100 / colCount;
      col.style.width = `${current * scale}%`;
    });
  }

  if (!table.style.tableLayout) {
    table.style.tableLayout = 'fixed';
  }
  if (!table.style.width) {
    table.style.width = '100%';
  }

  return cols;
}

function syncColGroupAfterStructureChange(table: HTMLTableElement): void {
  if (!table.rows.length) {
    return;
  }

  const colCount = getRowVisualColspan(table.rows[0]);
  if (colCount <= 0) {
    return;
  }

  let colgroup = table.querySelector('colgroup');
  if (!colgroup) {
    colgroup = document.createElement('colgroup');
    table.insertBefore(colgroup, table.firstChild);
  }

  const tableWidth = table.getBoundingClientRect().width || table.offsetWidth || 1;
  let cols = Array.from(colgroup.querySelectorAll('col'));
  const previousCount = cols.length;

  while (cols.length < colCount) {
    const col = document.createElement('col');
    colgroup.appendChild(col);
    cols.push(col);
  }
  while (cols.length > colCount) {
    cols[cols.length - 1].remove();
    cols.pop();
  }

  if (!cols.length) {
    return;
  }

  if (previousCount === 0 || cols.every((col) => parseColWidthPercent(col, tableWidth) <= 0)) {
    const evenWidth = 100 / colCount;
    cols.forEach((col) => {
      col.style.width = `${evenWidth}%`;
    });
  } else if (previousCount < colCount) {
    const oldCols = cols.slice(0, previousCount);
    const newCols = cols.slice(previousCount);
    const oldWidths = oldCols.map((col) => {
      const parsed = parseColWidthPercent(col, tableWidth);
      return parsed > 0 ? parsed : 100 / colCount;
    });
    const oldTotal = oldWidths.reduce((sum, width) => sum + width, 0) || 100;
    const addedCount = newCols.length;
    const targetNewTotal = Math.max(addedCount * MIN_COL_WIDTH_PERCENT, (100 / colCount) * addedCount);
    const availableForOld = Math.max(100 - targetNewTotal, oldCols.length * MIN_COL_WIDTH_PERCENT);
    const scale = availableForOld / oldTotal;

    oldCols.forEach((col, index) => {
      col.style.width = `${Math.max(MIN_COL_WIDTH_PERCENT, oldWidths[index] * scale)}%`;
    });
    const eachNew = targetNewTotal / addedCount;
    newCols.forEach((col) => {
      col.style.width = `${Math.max(MIN_COL_WIDTH_PERCENT, eachNew)}%`;
    });
    normalizeColGroupWidths(cols, tableWidth);
  } else if (previousCount > colCount) {
    normalizeColGroupWidths(cols, tableWidth);
  }

  if (!table.style.tableLayout) {
    table.style.tableLayout = 'fixed';
  }
  if (!table.style.width) {
    table.style.width = '100%';
  }
}

function normalizeColGroupWidths(cols: HTMLTableColElement[], tableWidth: number): void {
  const widths = cols.map((col) => {
    const parsed = parseColWidthPercent(col, tableWidth);
    return Math.max(MIN_COL_WIDTH_PERCENT, parsed > 0 ? parsed : 100 / cols.length);
  });
  const total = widths.reduce((sum, width) => sum + width, 0) || 100;
  cols.forEach((col, index) => {
    col.style.width = `${(widths[index] / total) * 100}%`;
  });
}

function startColumnResize(
  event: MouseEvent,
  table: HTMLTableElement,
  colIndex: number,
  onStructureChange?: () => void
): void {
  const cols = ensureColGroup(table);
  if (colIndex < 0 || colIndex >= cols.length - 1) {
    return;
  }

  const tableWidth = table.getBoundingClientRect().width || table.offsetWidth || 1;
  const startX = event.clientX;
  const startWidths = cols.map((col) => {
    const parsed = parseColWidthPercent(col, tableWidth);
    return parsed > 0 ? parsed : 100 / cols.length;
  });

  const leftCol = cols[colIndex];
  const rightCol = cols[colIndex + 1];
  const resizer = event.currentTarget as HTMLElement | null;

  const onMouseMove = (moveEvent: MouseEvent) => {
    const deltaPercent = ((moveEvent.clientX - startX) / tableWidth) * 100;
    let leftWidth = startWidths[colIndex] + deltaPercent;
    let rightWidth = startWidths[colIndex + 1] - deltaPercent;
    const pairTotal = startWidths[colIndex] + startWidths[colIndex + 1];

    if (leftWidth < MIN_COL_WIDTH_PERCENT) {
      leftWidth = MIN_COL_WIDTH_PERCENT;
      rightWidth = pairTotal - leftWidth;
    }
    if (rightWidth < MIN_COL_WIDTH_PERCENT) {
      rightWidth = MIN_COL_WIDTH_PERCENT;
      leftWidth = pairTotal - rightWidth;
    }

    leftCol.style.width = `${leftWidth}%`;
    rightCol.style.width = `${rightWidth}%`;
  };

  const onMouseUp = () => {
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
    resizer?.classList.remove('is-dragging');
    onStructureChange?.();
  };

  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  resizer?.classList.add('is-dragging');
  document.addEventListener('mousemove', onMouseMove);
  document.addEventListener('mouseup', onMouseUp);
}

function attachColumnResizeHandles(wrapper: HTMLElement, onStructureChange?: () => void): void {
  const table = wrapper.querySelector('table');
  if (!table?.rows?.length) {
    return;
  }

  ensureColGroup(table);
  const firstRow = table.rows[0];
  const colCount = getRowVisualColspan(firstRow);
  const activeVisualIndexes = new Set<number>();

  Array.from(firstRow.cells).forEach((cell) => {
    const colspan = Number(cell.getAttribute('colspan') || '1');
    if (colspan > 1) {
      return;
    }
    const visualIndex = getVisualColumnIndex(firstRow, cell);
    if (visualIndex >= colCount - 1) {
      return;
    }
    activeVisualIndexes.add(visualIndex);

    const cellEl = cell as HTMLElement;
    cellEl.style.position = 'relative';

    let resizer = cellEl.querySelector(':scope > .q-table-col-resizer') as HTMLElement | null;
    if (!resizer) {
      resizer = document.createElement('span');
      resizer.className = 'q-table-col-resizer';
      resizer.setAttribute('data-q-table-editor-only', 'true');
      resizer.title = 'Drag to resize column';
      resizer.setAttribute('aria-label', 'Drag to resize column');
      cellEl.appendChild(resizer);
    }

    if ((resizer as any)[Q_TABLE_COL_RESIZER_BOUND]) {
      return;
    }
    (resizer as any)[Q_TABLE_COL_RESIZER_BOUND] = true;

    resizer.addEventListener('mousedown', (e) => {
      e.preventDefault();
      e.stopPropagation();
      startColumnResize(e, table, visualIndex, onStructureChange);
    });
  });

  table.querySelectorAll('.q-table-col-resizer').forEach((node) => {
    const resizer = node as HTMLElement;
    const cell = resizer.parentElement as HTMLTableCellElement | null;
    if (!cell || cell.parentElement !== firstRow) {
      resizer.remove();
      return;
    }
    const visualIndex = getVisualColumnIndex(firstRow, cell);
    if (!activeVisualIndexes.has(visualIndex)) {
      resizer.remove();
    }
  });
}

function refreshColumnResizeHandles(wrapper: HTMLElement): void {
  const table = wrapper.querySelector('table');
  if (!table) {
    return;
  }
  table.querySelectorAll('.q-table-col-resizer').forEach((node) => {
    delete (node as any)[Q_TABLE_COL_RESIZER_BOUND];
    node.remove();
  });
}

function isHeaderRow(row: HTMLTableRowElement): boolean {
  return row.cells.length > 0 && Array.from(row.cells).every((cell) => cell.tagName === 'TH');
}

export function addTableRow(wrapper: HTMLElement): boolean {
  const table = wrapper.querySelector('table');
  if (!table?.rows?.length) {
    return false;
  }
  const colCount = getRowVisualColspan(table.rows[0]);
  if (colCount <= 0) {
    return false;
  }
  const tr = document.createElement('tr');
  for (let c = 0; c < colCount; c++) {
    tr.insertAdjacentHTML('beforeend', buildEditorTableCellHtml('td', ''));
  }
  table.appendChild(tr);
  syncColGroupAfterStructureChange(table);
  return true;
}

export function removeTableRow(wrapper: HTMLElement): boolean {
  const table = wrapper.querySelector('table');
  if (!table?.rows || table.rows.length <= 1) {
    return false;
  }
  table.deleteRow(table.rows.length - 1);
  syncColGroupAfterStructureChange(table);
  return true;
}

export function addTableColumn(wrapper: HTMLElement): boolean {
  const table = wrapper.querySelector('table');
  if (!table?.rows?.length) {
    return false;
  }
  const targetColCount = getRowVisualColspan(table.rows[0]) + 1;
  Array.from(table.rows).forEach((row) => {
    const headerRow = row === table.rows[0] && isHeaderRow(row);
    const nextColNumber = getRowVisualColspan(row) + 1;
    const defaultValue = headerRow ? `Header ${nextColNumber}` : '';
    row.insertAdjacentHTML('beforeend', buildEditorTableCellHtml(headerRow ? 'th' : 'td', defaultValue));
  });
  if (getRowVisualColspan(table.rows[0]) !== targetColCount) {
    return false;
  }
  syncColGroupAfterStructureChange(table);
  refreshColumnResizeHandles(wrapper);
  return true;
}

export function removeTableColumn(wrapper: HTMLElement): boolean {
  const table = wrapper.querySelector('table');
  if (!table?.rows?.length) {
    return false;
  }
  const colCount = getRowVisualColspan(table.rows[0]);
  if (colCount <= 1) {
    return false;
  }
  Array.from(table.rows).forEach((row) => {
    if (row.cells.length) {
      row.cells[row.cells.length - 1].remove();
    }
  });
  syncColGroupAfterStructureChange(table);
  refreshColumnResizeHandles(wrapper);
  return true;
}

function createSizeButton(action: string, label: string, type: 'plus' | 'minus'): HTMLButtonElement {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = `q-table-size-btn q-table-size-btn--${type}`;
  btn.dataset['action'] = action;
  btn.title = label;
  btn.setAttribute('aria-label', label);
  btn.setAttribute('data-q-table-editor-only', 'true');
  btn.innerHTML = type === 'plus'
    ? '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 3.25v9.5M3.25 8h9.5" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></svg>'
    : '<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M3.25 8h9.5" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></svg>';
  return btn;
}

function buildRowControlsElement(): HTMLElement {
  const controls = document.createElement('div');
  controls.className = 'q-table-controls q-table-row-controls';
  controls.setAttribute('contenteditable', 'false');
  controls.setAttribute('data-q-table-editor-only', 'true');
  controls.appendChild(createSizeButton('row-plus', 'Add row', 'plus'));
  controls.appendChild(createSizeButton('row-minus', 'Remove row', 'minus'));
  return controls;
}

function buildColControlsElement(): HTMLElement {
  const controls = document.createElement('div');
  controls.className = 'q-table-controls q-table-col-controls';
  controls.setAttribute('contenteditable', 'false');
  controls.setAttribute('data-q-table-editor-only', 'true');
  controls.appendChild(createSizeButton('col-plus', 'Add column', 'plus'));
  controls.appendChild(createSizeButton('col-minus', 'Remove column', 'minus'));
  return controls;
}

function ensureTableLayout(wrapper: HTMLElement): HTMLTableElement | null {
  const table = wrapper.querySelector('table');
  if (!table) {
    return null;
  }

  wrapper.querySelectorAll('.q-table-controls:not(.q-table-row-controls):not(.q-table-col-controls)').forEach((el) => el.remove());

  let layout = wrapper.querySelector('.q-table-layout') as HTMLElement | null;
  if (!layout) {
    layout = document.createElement('div');
    layout.className = 'q-table-layout';
    layout.setAttribute('contenteditable', 'false');

    const main = document.createElement('div');
    main.className = 'q-table-main';
    main.setAttribute('contenteditable', 'false');

    const tableParent = table.parentElement;
    if (tableParent) {
      tableParent.insertBefore(layout, table);
    } else {
      wrapper.appendChild(layout);
    }
    main.appendChild(table);
    layout.appendChild(main);
    layout.appendChild(buildColControlsElement());
    layout.appendChild(buildRowControlsElement());
    return table;
  }

  if (!layout.querySelector('.q-table-row-controls')) {
    layout.appendChild(buildRowControlsElement());
  }
  if (!layout.querySelector('.q-table-col-controls')) {
    layout.appendChild(buildColControlsElement());
  }
  if (!layout.querySelector('.q-table-main')) {
    const main = document.createElement('div');
    main.className = 'q-table-main';
    main.setAttribute('contenteditable', 'false');
    main.appendChild(table);
    layout.prepend(main);
  }

  return table;
}

function handleControlClick(
  wrapper: HTMLElement,
  event: Event,
  onStructureChange?: () => void
): void {
  event.preventDefault();
  event.stopPropagation();
  const target = event.target as HTMLElement | null;
  const btn = target?.closest('.q-table-size-btn') as HTMLButtonElement | null;
  if (!btn) {
    return;
  }
  const action = btn.dataset['action'] || '';
  let changed = false;
  switch (action) {
    case 'row-plus':
      changed = addTableRow(wrapper);
      break;
    case 'row-minus':
      changed = removeTableRow(wrapper);
      break;
    case 'col-plus':
      changed = addTableColumn(wrapper);
      break;
    case 'col-minus':
      changed = removeTableColumn(wrapper);
      break;
  }
  if (changed && onStructureChange) {
    onStructureChange();
  }
  if (changed) {
    refreshColumnResizeHandles(wrapper);
    attachColumnResizeHandles(wrapper, onStructureChange);
  }
}

export function attachTableSizeControls(
  wrapper: HTMLElement,
  onStructureChange?: () => void
): void {
  if (!ensureTableLayout(wrapper)) {
    return;
  }

  const hasEdgeControls =
    !!wrapper.querySelector('.q-table-row-controls') &&
    !!wrapper.querySelector('.q-table-col-controls');
  if ((wrapper as any)[Q_TABLE_SIZE_CONTROLS_GUARD] && hasEdgeControls) {
    attachColumnResizeHandles(wrapper, onStructureChange);
    return;
  }
  (wrapper as any)[Q_TABLE_SIZE_CONTROLS_GUARD] = true;

  const onClick = (e: Event) => handleControlClick(wrapper, e, onStructureChange);
  wrapper.querySelectorAll('.q-table-row-controls, .q-table-col-controls').forEach((controls) => {
    const el = controls as HTMLElement;
    el.addEventListener('mousedown', (e) => e.stopPropagation());
    el.addEventListener('click', onClick);
  });

  attachColumnResizeHandles(wrapper, onStructureChange);
}

export function attachTableSizeControlsForEditor(
  editor: any,
  onStructureChange?: () => void
): void {
  const root = editor?.root as HTMLElement | undefined;
  if (!root) {
    return;
  }
  root.querySelectorAll('.q-table-wrapper').forEach((wrap) => {
    attachTableSizeControls(wrap as HTMLElement, () => {
      if (editor?.emitter) {
        editor.emitter.emit('text-change', editor.getContents(), editor.getContents(), 'user');
      }
      if (onStructureChange) {
        onStructureChange();
      }
    });
  });
}
