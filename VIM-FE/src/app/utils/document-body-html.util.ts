const DOCUMENT_TABLE_CELL_BORDER = '1px solid #334155';

function hasExplicitTableWidth(table: HTMLTableElement): boolean {
  const inline = (table.style.width || '').trim();
  if (inline && inline !== 'auto') {
    return true;
  }
  const attr = (table.getAttribute('width') || '').trim();
  return !!attr;
}

function normalizeDocumentTableElement(table: HTMLTableElement): void {
  table.style.borderCollapse = 'collapse';
  table.style.boxSizing = 'border-box';
  table.style.maxWidth = '100%';

  if (!hasExplicitTableWidth(table)) {
    table.style.width = '100%';
  } else {
    const inlineWidth = (table.style.width || '').trim();
    if (inlineWidth.endsWith('px')) {
      const px = Number.parseFloat(inlineWidth);
      if (Number.isFinite(px) && px > 0 && px <= 720) {
        table.style.width = '100%';
      }
    }
  }

  const hasColgroup = !!table.querySelector('colgroup col');
  table.style.tableLayout = hasColgroup ? 'fixed' : 'auto';

  table.querySelectorAll('td, th').forEach((cellNode) => {
    const cell = cellNode as HTMLElement;
    cell.style.setProperty('border', DOCUMENT_TABLE_CELL_BORDER, 'important');
    if (!cell.style.padding) {
      cell.style.padding = '8px';
    }
    if (!cell.style.verticalAlign) {
      cell.style.verticalAlign = 'top';
    }
    cell.style.removeProperty('max-width');
    cell.style.minWidth = '2em';
    cell.style.boxSizing = 'border-box';
    cell.style.overflowWrap = 'anywhere';
    cell.style.wordBreak = 'break-word';

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
    figure.style.display = 'block';
    figure.style.maxWidth = '100%';
    figure.style.boxSizing = 'border-box';
    if (!figure.style.marginTop && !figure.style.marginBottom) {
      figure.style.marginTop = '12px';
      figure.style.marginBottom = '12px';
    }

    const table = figure.querySelector('table');
    if (table) {
      normalizeDocumentTableElement(table);
    }
  });

  wrapper.querySelectorAll('table').forEach((tableNode) => {
    const table = tableNode as HTMLTableElement;
    if (table.closest('figure.table')) {
      return;
    }
    if (table.closest('.word-editor-value, .ql-editor, .inline-filled-word-editor-value')) {
      return;
    }
    normalizeDocumentTableElement(table);
  });

  return wrapper.innerHTML;
}
