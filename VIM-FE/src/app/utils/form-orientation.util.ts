export type FormOrientation = 'portrait' | 'landscape';

export const A4_SHORT_EDGE_MM = 210;
export const A4_LONG_EDGE_MM = 297;

export function isOrientationFieldType(fieldType: string | undefined): boolean {
  return (fieldType || '').toString().trim().toLowerCase() === 'orientation';
}

export function formHasOrientationField(
  fields: Array<{ type?: string }> | null | undefined
): boolean {
  return (fields || []).some((field) => isOrientationFieldType(field?.type));
}

export function resolveFormOrientation(
  appData: Record<string, unknown> | null | undefined,
  _fields?: Array<{ type?: string }> | null | undefined
): FormOrientation {
  const raw = appData?.['_formOrientation'] ?? appData?.['formOrientation'];
  if (raw != null && String(raw).trim() !== '') {
    return String(raw).toLowerCase() === 'landscape' ? 'landscape' : 'portrait';
  }
  return 'portrait';
}

export function isLandscapeFormOrientation(orientation: FormOrientation): boolean {
  return orientation === 'landscape';
}

export function getA4PaperSizeMm(orientation: FormOrientation): { widthMm: number; heightMm: number } {
  if (orientation === 'landscape') {
    return { widthMm: A4_LONG_EDGE_MM, heightMm: A4_SHORT_EDGE_MM };
  }
  return { widthMm: A4_SHORT_EDGE_MM, heightMm: A4_LONG_EDGE_MM };
}

export function getPreviewPaperOrientationClass(
  orientation: FormOrientation
): Record<string, boolean> {
  return {
    'xyz-paper--landscape': orientation === 'landscape',
    'xyz-paper--portrait': orientation !== 'landscape'
  };
}

export function getPreviewPaperStyle(orientation: FormOrientation): Record<string, string> {
  const { widthMm, heightMm } = getA4PaperSizeMm(orientation);
  if (orientation === 'landscape') {
    return {
      width: `${widthMm}mm`,
      minWidth: `${widthMm}mm`,
      maxWidth: `${widthMm}mm`,
      height: `${heightMm}mm`,
      minHeight: `${heightMm}mm`,
      maxHeight: `${heightMm}mm`,
      aspectRatio: `${widthMm} / ${heightMm}`,
      boxSizing: 'border-box'
    };
  }
  return {
    width: `${widthMm}mm`,
    minHeight: `${heightMm}mm`,
    boxSizing: 'border-box'
  };
}

/** Apply landscape/portrait mm box on a paper element (inline !important beats conflicting CSS). */
export function applyPaperElementDimensions(
  paper: HTMLElement | null | undefined,
  orientation: FormOrientation
): void {
  if (!paper) {
    return;
  }
  const { widthMm, heightMm } = getA4PaperSizeMm(orientation);
  paper.style.setProperty('box-sizing', 'border-box', 'important');
  paper.style.setProperty('width', `${widthMm}mm`, 'important');
  paper.style.setProperty('min-width', `${widthMm}mm`, 'important');
  paper.style.setProperty('max-width', `${widthMm}mm`, 'important');
  if (orientation === 'landscape') {
    paper.style.setProperty('height', `${heightMm}mm`, 'important');
    paper.style.setProperty('min-height', `${heightMm}mm`, 'important');
    paper.style.setProperty('max-height', `${heightMm}mm`, 'important');
    paper.style.setProperty('aspect-ratio', `${widthMm} / ${heightMm}`, 'important');
    paper.style.setProperty('overflow', 'hidden', 'important');
  } else {
    paper.style.setProperty('height', 'auto', 'important');
    paper.style.setProperty('min-height', `${heightMm}mm`, 'important');
    paper.style.setProperty('max-height', 'none', 'important');
    paper.style.removeProperty('aspect-ratio');
    paper.style.setProperty('overflow', 'visible', 'important');
  }
}
