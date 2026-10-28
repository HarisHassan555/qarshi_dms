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
