const DOCUMENT_TABLE_CELL_BORDER = '1px solid #334155';

function hasExplicitTableWidth(table: HTMLTableElement): boolean {
  const inline = (table.style.width || '').trim();
  if (inline && inline !== 'auto') {
    return true;
  }
  const attr = (table.getAttribute('width') || '').trim();
  return !!attr;
}

function isWordEditorTable(node: Element): boolean {
  return !!node.closest('.word-editor-value, .ql-editor, .inline-filled-word-editor-value, .word-editor-filled-value');
}

function normalizeDocumentTableElement(table: HTMLTableElement): void {
  table.style.borderCollapse = 'separate';
  table.style.borderSpacing = '0';
  table.style.boxSizing = 'border-box';
  table.style.maxWidth = '100%';
  table.style.marginLeft = 'auto';
  table.style.marginRight = 'auto';
  table.style.tableLayout = 'auto';

  if (!hasExplicitTableWidth(table)) {
    table.style.width = 'auto';
  }

  table.querySelectorAll('td, th').forEach((cellNode) => {
    const cell = cellNode as HTMLElement;
    if (!cell.style.border && !cell.style.borderTop && !cell.style.borderLeft) {
      cell.style.border = DOCUMENT_TABLE_CELL_BORDER;
    }
    if (!cell.style.padding) {
      cell.style.padding = '6px';
    }
    if (!cell.style.verticalAlign) {
      cell.style.verticalAlign = 'top';
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
    figure.style.display = 'table';
    figure.style.width = figure.style.width || 'auto';
    figure.style.maxWidth = '100%';
    figure.style.boxSizing = 'border-box';
    figure.style.marginLeft = 'auto';
    figure.style.marginRight = 'auto';
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

  return wrapper.innerHTML;
}
