import { TEMPLATE_TABLE_GROUP_ATTR } from 'src/app/utils/template-body-table-pagination.util';

function isWordEditorTable(node: Element): boolean {
  return !!node.closest(
    '.word-editor-value, .ql-editor, .inline-filled-word-editor-value, .word-editor-filled-value'
  );
}

/** Strip fill-only chrome so PDF/email capture matches the read-only application view. */
export function stripDocumentFillChromeForPdf(root: ParentNode): void {
  root.querySelectorAll(
    '.fill-excel-indicator-layer, .fill-table-context-menu, .fill-excel-label, .fill-excel-remove'
  ).forEach((node) => node.remove());
  root.querySelectorAll('[contenteditable]').forEach((node) => {
    (node as HTMLElement).removeAttribute('contenteditable');
  });
  root.querySelectorAll(
    '.document-fill-cell, .document-formula-cell, .document-table-target-cell'
  ).forEach((node) => {
    node.classList.remove(
      'document-fill-cell',
      'document-formula-cell',
      'document-table-target-cell'
    );
  });
  root.querySelectorAll('[data-fill-bound], [data-fill-menu-bound]').forEach((node) => {
    node.removeAttribute('data-fill-bound');
    node.removeAttribute('data-fill-menu-bound');
  });
  root.querySelectorAll('table[title]').forEach((node) => {
    const title = node.getAttribute('title') || '';
    if (title.includes('Click a cell to add or delete')) {
      node.removeAttribute('title');
    }
  });
}

function setImp(el: HTMLElement, prop: string, value: string): void {
  if (!value || value === 'auto' || value === 'normal') {
    return;
  }
  el.style.setProperty(prop, value, 'important');
}

function bakeBoxStyles(source: HTMLElement, target: HTMLElement): void {
  const cs = getComputedStyle(source);
  setImp(target, 'box-sizing', 'border-box');
  setImp(target, 'font-family', cs.fontFamily);
  setImp(target, 'font-size', cs.fontSize);
  setImp(target, 'font-weight', cs.fontWeight);
  setImp(target, 'font-style', cs.fontStyle);
  setImp(target, 'line-height', cs.lineHeight);
  setImp(target, 'letter-spacing', cs.letterSpacing);
  setImp(target, 'color', cs.color);
  setImp(target, 'text-align', cs.textAlign);
  setImp(target, 'vertical-align', cs.verticalAlign);
  setImp(target, 'white-space', cs.whiteSpace);
  setImp(target, 'word-break', cs.wordBreak);
  setImp(target, 'overflow-wrap', cs.overflowWrap);
  setImp(target, 'background-color', cs.backgroundColor);
  setImp(target, 'border-top', cs.borderTop);
  setImp(target, 'border-right', cs.borderRight);
  setImp(target, 'border-bottom', cs.borderBottom);
  setImp(target, 'border-left', cs.borderLeft);
  setImp(target, 'padding-top', cs.paddingTop);
  setImp(target, 'padding-right', cs.paddingRight);
  setImp(target, 'padding-bottom', cs.paddingBottom);
  setImp(target, 'padding-left', cs.paddingLeft);
  setImp(target, 'margin-top', cs.marginTop);
  setImp(target, 'margin-right', cs.marginRight);
  setImp(target, 'margin-bottom', cs.marginBottom);
  setImp(target, 'margin-left', cs.marginLeft);
}

function bakeHeightFromSource(source: HTMLElement, target: HTMLElement): void {
  const rect = source.getBoundingClientRect();
  if (rect.height > 0) {
    setImp(target, 'height', `${Math.round(rect.height)}px`);
    setImp(target, 'min-height', `${Math.round(rect.height)}px`);
  }
}

function bakeParallelLists(
  sources: HTMLElement[],
  targets: HTMLElement[],
  bake: (source: HTMLElement, target: HTMLElement) => void
): void {
  const count = Math.min(sources.length, targets.length);
  for (let i = 0; i < count; i += 1) {
    bake(sources[i], targets[i]);
  }
}

/**
 * Copy live computed styles onto the capture clone so html2canvas matches the
 * on-screen application / fill view (fonts, header fills, borders, row heights).
 */
function bakeLiveDocumentStylesOntoClone(sourceRoot: HTMLElement, cloneRoot: HTMLElement): void {
  const sourceDoc = sourceRoot.querySelector('.document-html') as HTMLElement | null;
  const cloneDoc = cloneRoot.querySelector('.document-html') as HTMLElement | null;
  if (sourceDoc && cloneDoc) {
    bakeBoxStyles(sourceDoc, cloneDoc);
    const sourceCs = getComputedStyle(sourceDoc);
    setImp(cloneDoc, 'width', '100%');
    setImp(cloneDoc, 'max-width', '100%');
    setImp(cloneDoc, 'padding', sourceCs.padding);
    setImp(cloneDoc, 'position', 'relative');
    setImp(cloneDoc, 'overflow-x', 'hidden');
  }

  bakeParallelLists(
    Array.from(sourceRoot.querySelectorAll('.document-html figure.table')) as HTMLElement[],
    Array.from(cloneRoot.querySelectorAll('.document-html figure.table')) as HTMLElement[],
    (source, target) => {
      if (isWordEditorTable(source)) {
        return;
      }
      bakeBoxStyles(source, target);
      setImp(target, 'display', 'block');
      setImp(target, 'width', '100%');
      setImp(target, 'max-width', '100%');
    }
  );

  bakeParallelLists(
    Array.from(sourceRoot.querySelectorAll('.document-html table')) as HTMLElement[],
    Array.from(cloneRoot.querySelectorAll('.document-html table')) as HTMLElement[],
    (source, target) => {
      if (isWordEditorTable(source)) {
        return;
      }
      const cs = getComputedStyle(source);
      bakeBoxStyles(source, target);
      setImp(target, 'border-collapse', cs.borderCollapse || 'collapse');
      setImp(target, 'border-spacing', cs.borderSpacing || '0px');
      setImp(target, 'table-layout', cs.tableLayout || 'fixed');
      setImp(target, 'width', '100%');
      setImp(target, 'max-width', '100%');

      // Preserve live column locks (percent or px) without remasuring.
      const sourceCols = Array.from(source.querySelectorAll(':scope > colgroup > col')) as HTMLElement[];
      const targetCols = Array.from(target.querySelectorAll(':scope > colgroup > col')) as HTMLElement[];
      bakeParallelLists(sourceCols, targetCols, (srcCol, dstCol) => {
        const width = srcCol.style.width || getComputedStyle(srcCol).width;
        if (width) {
          setImp(dstCol, 'width', width);
        }
      });
    }
  );

  bakeParallelLists(
    Array.from(sourceRoot.querySelectorAll('.document-html tr')) as HTMLElement[],
    Array.from(cloneRoot.querySelectorAll('.document-html tr')) as HTMLElement[],
    (source, target) => {
      if (isWordEditorTable(source)) {
        return;
      }
      bakeBoxStyles(source, target);
      bakeHeightFromSource(source, target);
    }
  );

  bakeParallelLists(
    Array.from(sourceRoot.querySelectorAll('.document-html th, .document-html td')) as HTMLElement[],
    Array.from(cloneRoot.querySelectorAll('.document-html th, .document-html td')) as HTMLElement[],
    (source, target) => {
      if (isWordEditorTable(source)) {
        return;
      }
      bakeBoxStyles(source, target);
      const rect = source.getBoundingClientRect();
      if (rect.height > 0) {
        setImp(target, 'height', `${Math.round(rect.height)}px`);
        setImp(target, 'min-height', `${Math.round(rect.height)}px`);
      }
      if (source.tagName === 'TH') {
        const bg = getComputedStyle(source).backgroundColor;
        if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') {
          setImp(target, 'background-color', bg);
          setImp(target, 'background', bg);
        } else {
          setImp(target, 'background-color', '#e2e8f0');
          setImp(target, 'background', '#e2e8f0');
        }
        setImp(target, 'font-weight', '700');
      }
    }
  );

  bakeParallelLists(
    Array.from(
      sourceRoot.querySelectorAll('.document-html [data-template-page-break="true"], .document-html .template-page-break')
    ) as HTMLElement[],
    Array.from(
      cloneRoot.querySelectorAll('.document-html [data-template-page-break="true"], .document-html .template-page-break')
    ) as HTMLElement[],
    (source, target) => {
      const rect = source.getBoundingClientRect();
      setImp(target, 'display', 'block');
      setImp(target, 'width', '100%');
      setImp(target, 'margin', '0');
      setImp(target, 'padding', '0');
      setImp(target, 'border', '0');
      if (rect.height > 0) {
        setImp(target, 'height', `${Math.round(rect.height)}px`);
        setImp(target, 'min-height', `${Math.round(rect.height)}px`);
      }
    }
  );
}

function ensureFallbackDocumentStyles(root: HTMLElement): void {
  const documentHtml = root.querySelector('.document-html') as HTMLElement | null;
  if (documentHtml) {
    setImp(documentHtml, 'box-sizing', 'border-box');
    setImp(documentHtml, 'width', '100%');
    setImp(documentHtml, 'max-width', '100%');
    setImp(documentHtml, 'padding', documentHtml.style.padding || '22mm 18mm');
    setImp(documentHtml, 'overflow-x', 'hidden');
    setImp(documentHtml, 'word-break', 'break-word');
    setImp(documentHtml, 'font-family', documentHtml.style.fontFamily || 'Arial, Helvetica, sans-serif');
    setImp(documentHtml, 'font-size', documentHtml.style.fontSize || '14px');
    setImp(documentHtml, 'line-height', documentHtml.style.lineHeight || '1.55');
    setImp(documentHtml, 'color', documentHtml.style.color || '#111827');
  }

  root.querySelectorAll('.document-html figure.table').forEach((node) => {
    const figure = node as HTMLElement;
    if (isWordEditorTable(figure)) {
      return;
    }
    setImp(figure, 'display', 'block');
    setImp(figure, 'width', '100%');
    setImp(figure, 'max-width', '100%');
    setImp(figure, 'margin-left', '0');
    setImp(figure, 'margin-right', '0');
  });

  root.querySelectorAll('.document-html table').forEach((node) => {
    const table = node as HTMLTableElement;
    if (isWordEditorTable(table)) {
      return;
    }
    setImp(table, 'border-collapse', 'collapse');
    setImp(table, 'border-spacing', '0');
    setImp(table, 'width', '100%');
    setImp(table, 'max-width', '100%');
    setImp(table, 'margin-left', '0');
    setImp(table, 'margin-right', '0');
    if (table.getAttribute(TEMPLATE_TABLE_GROUP_ATTR)) {
      setImp(table, 'table-layout', 'fixed');
    }
  });

  root.querySelectorAll('.document-html th').forEach((node) => {
    const cell = node as HTMLElement;
    if (isWordEditorTable(cell)) {
      return;
    }
    setImp(cell, 'background-color', cell.style.backgroundColor || '#e2e8f0');
    setImp(cell, 'background', cell.style.background || '#e2e8f0');
    setImp(cell, 'font-weight', '700');
    setImp(cell, 'border', cell.style.border || '1px solid #334155');
    setImp(cell, 'padding', cell.style.padding || '8px');
    setImp(cell, 'vertical-align', cell.style.verticalAlign || 'top');
  });

  root.querySelectorAll('.document-html td').forEach((node) => {
    const cell = node as HTMLElement;
    if (isWordEditorTable(cell)) {
      return;
    }
    setImp(cell, 'border', cell.style.border || '1px solid #334155');
    setImp(cell, 'padding', cell.style.padding || '8px');
    setImp(cell, 'vertical-align', cell.style.verticalAlign || 'top');
  });
}

function sourceMatchesCloneStructure(sourceRoot: HTMLElement, cloneRoot: HTMLElement): boolean {
  const pairs: Array<[string, string]> = [
    ['.document-html table', '.document-html table'],
    ['.document-html tr', '.document-html tr'],
    ['.document-html th, .document-html td', '.document-html th, .document-html td'],
    [
      '.document-html [data-template-page-break="true"], .document-html .template-page-break',
      '.document-html [data-template-page-break="true"], .document-html .template-page-break'
    ],
  ];
  return pairs.every(([selector]) =>
    sourceRoot.querySelectorAll(selector).length === cloneRoot.querySelectorAll(selector).length
  );
}

/**
 * Prepare body tables for PDF/email capture without reflowing pagination.
 * Prefer baking styles from the live source frame so capture matches the UI.
 */
export function prepareDocumentBodyTablesForPdfCapture(
  cloneRoot: HTMLElement,
  sourceRoot?: HTMLElement | null
): void {
  stripDocumentFillChromeForPdf(cloneRoot);

  if (sourceRoot && sourceMatchesCloneStructure(sourceRoot, cloneRoot)) {
    bakeLiveDocumentStylesOntoClone(sourceRoot, cloneRoot);
  } else {
    ensureFallbackDocumentStyles(cloneRoot);
  }
}
