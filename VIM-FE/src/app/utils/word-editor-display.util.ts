export type WordEditorTextAlign = 'left' | 'center' | 'right' | 'justify';

export interface TemplateFieldStyleLike {
  fontSize?: number;
  fontFamily?: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  textAlign?: string;
}

export interface TemplateFieldLike {
  type?: string;
  style?: TemplateFieldStyleLike;
}

function normalizeTextAlign(value: string | null | undefined): WordEditorTextAlign | null {
  const normalized = String(value || '').trim().toLowerCase();
  if (normalized === 'center' || normalized === 'right' || normalized === 'justify' || normalized === 'left') {
    return normalized;
  }
  return null;
}

/** Read block alignment from CKEditor HTML (inline style or legacy Quill classes). */
export function readWordEditorElementTextAlign(el: HTMLElement | null | undefined): WordEditorTextAlign | null {
  if (!el) {
    return null;
  }

  for (const className of Array.from(el.classList)) {
    if (className === 'ql-align-center') {
      return 'center';
    }
    if (className === 'ql-align-right') {
      return 'right';
    }
    if (className === 'ql-align-justify') {
      return 'justify';
    }
  }

  const inlineAlign = normalizeTextAlign(el.style.textAlign);
  if (inlineAlign) {
    return inlineAlign;
  }

  const attrStyle = el.getAttribute('style') || '';
  const match = attrStyle.match(/text-align\s*:\s*(left|center|right|justify)/i);
  if (match) {
    return normalizeTextAlign(match[1]);
  }

  return null;
}

export function qlAlignClassForTextAlign(align: WordEditorTextAlign | null | undefined): string {
  switch (align) {
    case 'center':
      return 'ql-align-center';
    case 'right':
      return 'ql-align-right';
    case 'justify':
      return 'ql-align-justify';
    default:
      return '';
  }
}

/** Classes + optional inline style for PDF line splitting (preserves CKEditor alignment). */
export function resolveWordEditorLineAlignment(fragmentEl: HTMLElement | null): {
  classSuffix: string;
  styleAttr: string;
} {
  const align = readWordEditorElementTextAlign(fragmentEl);
  const qlClass = qlAlignClassForTextAlign(align);
  const classSuffix = qlClass ? ` ${qlClass}` : '';
  const styleAttr = align && align !== 'left' ? ` style="text-align:${align};"` : '';
  return { classSuffix, styleAttr };
}

const WORD_EDITOR_ALIGNMENT_BLOCK_SELECTOR =
  'p,h1,h2,h3,h4,h5,h6,li,blockquote,figcaption,td,th';

/**
 * Spreadsheet HTML often stores real column widths on `<col width>` while CKEditor
 * percentage widths squeeze the first columns. Fixed layout with those pixel ratios
 * keeps merged cells (rowspan/colspan) lined up with the original document.
 */
export function stabilizeWordEditorTables(root: ParentNode): void {
  root.querySelectorAll('figure.table').forEach((node) => {
    const figure = node as HTMLElement;
    figure.style.boxSizing = 'border-box';
    figure.style.display = 'block';
    figure.style.width = '100%';
    figure.style.maxWidth = '100%';
  });

  root.querySelectorAll('table').forEach((node) => {
    const table = node as HTMLTableElement;
    applySpreadsheetColumnWidths(table);
    table.style.boxSizing = 'border-box';
    table.style.width = '100%';
    table.style.maxWidth = '100%';
    table.style.tableLayout = 'fixed';
    table.style.borderCollapse = 'collapse';
    table.querySelectorAll('td, th').forEach((cellNode) => {
      const cell = cellNode as HTMLElement;
      cell.style.boxSizing = 'border-box';
      cell.style.border = '1px solid #000000';
      if (!cell.style.background && !cell.style.backgroundColor) {
        cell.style.backgroundColor = 'transparent';
      }
      cell.style.padding = '3px 4px';
      cell.style.verticalAlign = 'middle';
      cell.style.overflowWrap = 'break-word';
      cell.style.wordBreak = 'normal';
    });
  });
}

function applySpreadsheetColumnWidths(table: HTMLTableElement): void {
  const cols = Array.from(table.querySelectorAll('colgroup col')) as HTMLElement[];
  if (!cols.length) {
    return;
  }
  const pixels = cols.map((col) => readColumnPixelWidth(col));
  if (pixels.some((value) => value <= 0)) {
    return;
  }
  const total = pixels.reduce((sum, value) => sum + value, 0);
  if (total <= 0) {
    return;
  }
  cols.forEach((col, index) => {
    col.style.width = `${(pixels[index] / total) * 100}%`;
  });
  table.querySelectorAll('td, th').forEach((cellNode) => {
    const cell = cellNode as HTMLElement;
    cell.removeAttribute('width');
    cell.style.removeProperty('width');
  });
}

function readColumnPixelWidth(col: HTMLElement): number {
  const attributeWidth = Number.parseFloat(col.getAttribute('width') || '');
  if (Number.isFinite(attributeWidth) && attributeWidth > 0) {
    return attributeWidth;
  }
  const styleWidth = String(col.style.width || '').trim();
  if (styleWidth.endsWith('px')) {
    const parsed = Number.parseFloat(styleWidth);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
  }
  return 0;
}

/** Adds ql-align-* classes so preview/PDF CSS matches CKEditor inline alignment. */
export function applyWordEditorAlignmentClasses(root: ParentNode): void {
  root.querySelectorAll(WORD_EDITOR_ALIGNMENT_BLOCK_SELECTOR).forEach((node) => {
    const el = node as HTMLElement;
    const qlClass = qlAlignClassForTextAlign(readWordEditorElementTextAlign(el));
    if (qlClass) {
      el.classList.add(qlClass);
    }
  });
}

export function getTemplateFieldNgStyle(
  field: TemplateFieldLike,
  options?: { includeTextAlign?: boolean }
): Record<string, string | number> {
  const style = field.style;
  const includeTextAlign =
    options?.includeTextAlign ?? field.type !== 'word_editor';

  const result: Record<string, string | number> = {
    'font-size.px': style?.fontSize || 14,
    'font-family': style?.fontFamily || 'Arial, sans-serif',
    'font-weight': style?.bold ? '700' : '400',
    'font-style': style?.italic ? 'italic' : 'normal',
    'text-decoration': style?.underline ? 'underline' : 'none',
  };

  if (includeTextAlign) {
    result['text-align'] = style?.textAlign || 'left';
  }

  return result;
}
