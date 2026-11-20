import {
  TEMPLATE_TABLE_GROUP_ATTR,
} from 'src/app/utils/template-body-table-pagination.util';

const DOCUMENT_TABLE_CELL_BORDER = '1px solid #334155';

function isWordEditorTable(node: Element): boolean {
  return !!node.closest('.word-editor-value, .ql-editor, .inline-filled-word-editor-value, .word-editor-filled-value');
}

function readColWidths(table: HTMLTableElement): number[] {
  const cols = Array.from(table.querySelectorAll('colgroup col'));
  if (!cols.length) {
    return [];
  }
  return cols.map((col) => {
    const el = col as HTMLElement;
    const styleWidth = parseFloat(el.style.width || '');
    if (Number.isFinite(styleWidth) && styleWidth > 0) {
      return styleWidth;
    }
    const attrWidth = parseFloat(el.getAttribute('width') || '');
    return Number.isFinite(attrWidth) && attrWidth > 0 ? attrWidth : 0;
  });
}

function zipMaxWidths(current: number[], next: number[]): number[] {
  const length = Math.max(current.length, next.length);
  const widths = Array.from({ length }, () => 0);
  for (let i = 0; i < length; i += 1) {
    widths[i] = Math.max(1, Number(current[i]) || 0, Number(next[i]) || 0);
  }
  return widths;
}

function applyPercentColumnWidths(table: HTMLTableElement, widths: number[]): void {
  if (!table || !widths.length) {
    return;
  }
  const sum = widths.reduce((acc, width) => acc + Math.max(1, width), 0) || widths.length;
  table.querySelectorAll('colgroup').forEach((node) => node.remove());
  const colgroup = document.createElement('colgroup');
  widths.forEach((width) => {
    const col = document.createElement('col');
    const pct = (Math.max(1, width) / sum) * 100;
    col.style.width = `${pct}%`;
    colgroup.appendChild(col);
  });
  table.insertBefore(colgroup, table.firstChild);
  table.style.tableLayout = 'fixed';
  table.style.width = '100%';
  table.style.maxWidth = '100%';
}

/** Keep page-split fragments on the same column grid. */
function syncGroupedTableColumnWidths(wrapper: HTMLElement): void {
  const groups = new Map<string, HTMLTableElement[]>();
  wrapper.querySelectorAll(`table[${TEMPLATE_TABLE_GROUP_ATTR}]`).forEach((node) => {
    const table = node as HTMLTableElement;
    if (isWordEditorTable(table)) {
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
    if (tables.length < 2) {
      return;
    }
    let maxWidths: number[] = [];
    tables.forEach((table) => {
      const widths = readColWidths(table);
      if (widths.length) {
        maxWidths = zipMaxWidths(maxWidths, widths);
        return;
      }
      const firstRow = table.rows[0];
      if (!firstRow) {
        return;
      }
      let columnCount = 0;
      Array.from(firstRow.cells).forEach((cell) => {
        columnCount += Math.max(1, Number(cell.colSpan) || 1);
      });
      if (columnCount > 0) {
        maxWidths = zipMaxWidths(maxWidths, Array.from({ length: columnCount }, () => 1));
      }
    });
    if (!maxWidths.length) {
      return;
    }
    tables.forEach((table) => applyPercentColumnWidths(table, maxWidths));
  });
}

function normalizeDocumentTableElement(table: HTMLTableElement): void {
  const grouped = !!table.getAttribute(TEMPLATE_TABLE_GROUP_ATTR);
  const existingCols = readColWidths(table);

  // Match document-body-content.css so application / fill / PDF stay aligned.
  table.style.borderCollapse = 'collapse';
  table.style.borderSpacing = '0';
  table.style.boxSizing = 'border-box';
  table.style.maxWidth = '100%';
  table.style.marginLeft = '0';
  table.style.marginRight = '0';

  if (grouped && existingCols.length) {
    // Keep shared locks (as %) so split fragments stay aligned.
    applyPercentColumnWidths(table, existingCols);
  } else if (!grouped) {
    table.querySelectorAll('colgroup').forEach((node) => node.remove());
    table.querySelectorAll('td, th').forEach((cellNode) => {
      const cell = cellNode as HTMLElement;
      cell.style.removeProperty('width');
      cell.style.removeProperty('min-width');
      cell.style.removeProperty('max-width');
      cell.removeAttribute('width');
    });
    table.removeAttribute('width');
    table.style.tableLayout = 'auto';
    table.style.width = '100%';
  } else {
    table.style.tableLayout = 'fixed';
    table.style.width = '100%';
  }

  table.querySelectorAll('td, th').forEach((cellNode) => {
    const cell = cellNode as HTMLElement;
    const isHeader = cell.tagName === 'TH';
    if (!cell.style.border && !cell.style.borderTop && !cell.style.borderLeft) {
      cell.style.border = DOCUMENT_TABLE_CELL_BORDER;
    }
    if (!cell.style.padding) {
      cell.style.padding = '8px';
    }
    if (!cell.style.verticalAlign) {
      cell.style.verticalAlign = 'top';
    }
    if (isHeader) {
      if (!cell.style.background && !cell.style.backgroundColor) {
        cell.style.background = '#e2e8f0';
      }
      if (!cell.style.fontWeight) {
        cell.style.fontWeight = '700';
      }
    }
    cell.style.boxSizing = 'border-box';
    cell.style.removeProperty('overflow-wrap');
    cell.style.removeProperty('word-break');

    const hasContent = !!(cell.textContent || '').replace(/\u00a0/g, ' ').trim()
      || cell.querySelector('img,table,ul,ol,p,span.template-field');
    if (!hasContent && !cell.innerHTML.trim()) {
      cell.innerHTML = '&nbsp;';
    }
  });
}

/** Normalize CKEditor document body HTML so tables match builder structure in fill/preview/PDF. */
export function normalizeDocumentBodyHtmlForDisplay(html: string): string {
  if (!html) {
    return '';
  }
  if (typeof document === 'undefined') {
    return html;
  }

  const wrapper = document.createElement('div');
  wrapper.innerHTML = html;

  wrapper.querySelectorAll('figure.table').forEach((figureNode) => {
    const figure = figureNode as HTMLElement;
    if (isWordEditorTable(figure)) {
      return;
    }
    figure.style.display = 'block';
    figure.style.width = '100%';
    figure.style.maxWidth = '100%';
    figure.style.boxSizing = 'border-box';
    figure.style.marginLeft = '0';
    figure.style.marginRight = '0';
    if (!figure.style.marginTop && !figure.style.marginBottom) {
      figure.style.marginTop = '0.9em';
      figure.style.marginBottom = '0.9em';
    }

    const table = figure.querySelector('table');
    if (table) {
      normalizeDocumentTableElement(table);
    }
  });

  wrapper.querySelectorAll('table').forEach((tableNode) => {
    const table = tableNode as HTMLTableElement;
    if (table.closest('figure.table') || isWordEditorTable(table)) {
      return;
    }
    normalizeDocumentTableElement(table);
  });

  syncGroupedTableColumnWidths(wrapper);

  return wrapper.innerHTML;
}
