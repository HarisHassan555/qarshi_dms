import type { IndividualFooterTableBand } from './individual-footer-pipeline.util';
import { getIndividualFooterRenderedHeightPx } from './individual-footer-pipeline.util';

function setImp(el: HTMLElement, prop: string, value: string): void {
    el.style.setProperty(prop, value, 'important');
}

export interface IndividualFooterPdfFieldPlacement {
    fieldId: string;
    placementHeight: number;
    placementY: number;
    bandCount: number;
    minHeight?: number;
}

/**
 * html2canvas-friendly layout for template individual_pipeline_footer (supports multiple bands).
 */
export function applyIndividualFooterTemplatePdfCaptureStyles(root: HTMLElement): void {
    root.querySelectorAll('.filled-field.document-region').forEach((node) => {
        const fieldEl = node as HTMLElement;
        if (!fieldEl.querySelector('.individual-footer-preview')) {
            return;
        }
        setImp(fieldEl, 'overflow', 'visible');
        setImp(fieldEl, 'box-sizing', 'border-box');
    });

    root.querySelectorAll('.individual-footer-preview, .individual-footer-preview-inner').forEach((node) => {
        const el = node as HTMLElement;
        setImp(el, 'overflow-x', 'hidden');
        setImp(el, 'overflow-y', 'visible');
        setImp(el, 'overflow', 'visible');
        setImp(el, 'height', 'auto');
        setImp(el, 'max-height', 'none');
    });

    root.querySelectorAll('table.individual-pipeline-footer-table').forEach((tableNode) => {
        const table = tableNode as HTMLElement;
        setImp(table, 'overflow', 'visible');
        setImp(table, 'border-collapse', 'collapse');

        const sigCells = Array.from(
            table.querySelectorAll('tr.individual-footer-sig-row td, tr.xyz-signatures-blank td')
        ) as HTMLElement[];
        sigCells.forEach((cell) => {
            setImp(cell, 'vertical-align', 'middle');
            setImp(cell, 'padding-top', '0');
            setImp(cell, 'padding-bottom', '0');
            setImp(cell, 'height', '44px');
            setImp(cell, 'min-height', '44px');
            setImp(cell, 'max-height', 'none');
            setImp(cell, 'overflow', 'visible');
            setImp(cell, 'box-sizing', 'border-box');
            setImp(cell, 'position', 'static');
            setImp(cell, 'max-width', 'none');

            let wrapper: HTMLElement | null = null;
            for (const child of Array.from(cell.children)) {
                if (child instanceof HTMLElement) {
                    wrapper = child;
                    break;
                }
            }

            if (wrapper) {
                setImp(wrapper, 'position', 'static');
                setImp(wrapper, 'top', 'auto');
                setImp(wrapper, 'left', 'auto');
                setImp(wrapper, 'right', 'auto');
                setImp(wrapper, 'display', 'flex');
                setImp(wrapper, 'flex-direction', 'column');
                setImp(wrapper, 'align-items', 'center');
                setImp(wrapper, 'justify-content', 'flex-end');
                setImp(wrapper, 'overflow', 'visible');
                setImp(wrapper, 'max-width', '100%');
                setImp(wrapper, 'width', '100%');
                setImp(wrapper, 'height', 'auto');
                setImp(wrapper, 'margin', '0');
                setImp(wrapper, 'padding', '0');
                setImp(wrapper, 'box-sizing', 'border-box');
                setImp(wrapper, 'transform', 'none');
            }

            const img = cell.querySelector('.xyz-sig-img') as HTMLElement | null;
            if (img) {
                setImp(img, 'position', 'static');
                setImp(img, 'display', 'block');
                setImp(img, 'margin', '0 auto 1px auto');
                setImp(img, 'max-height', '22px');
                setImp(img, 'max-width', '92%');
                setImp(img, 'height', 'auto');
                setImp(img, 'width', 'auto');
                setImp(img, 'object-fit', 'contain');
                setImp(img, 'transform', 'none');
            }

            const timeEl = cell.querySelector('.xyz-sig-time') as HTMLElement | null;
            if (timeEl) {
                setImp(timeEl, 'position', 'static');
                setImp(timeEl, 'display', 'block');
                setImp(timeEl, 'margin', '1px auto 0 auto');
                setImp(timeEl, 'line-height', '1.05');
                setImp(timeEl, 'font-size', '7px');
                setImp(timeEl, 'transform', 'none');
            }
        });

        const headerCells = Array.from(table.querySelectorAll('tr.individual-footer-header-row th')) as HTMLElement[];
        headerCells.forEach((cell) => {
            setImp(cell, 'background-color', '#8f8f8f');
            setImp(cell, 'background', '#8f8f8f');
            setImp(cell, '-webkit-print-color-adjust', 'exact');
            setImp(cell, 'print-color-adjust', 'exact');
            setImp(cell, 'color', '#000000');
            setImp(cell, 'text-align', 'center');
            setImp(cell, 'font-weight', '700');
            setImp(cell, 'font-size', '9px');
            setImp(cell, 'padding-top', '5px');
            setImp(cell, 'padding-bottom', '5px');
            setImp(cell, 'padding-left', '3px');
            setImp(cell, 'padding-right', '3px');
            setImp(cell, 'line-height', '1.15');
            setImp(cell, 'min-height', '20px');
            setImp(cell, 'height', 'auto');
            setImp(cell, 'vertical-align', 'middle');
            setImp(cell, 'overflow', 'visible');
            setImp(cell, 'max-width', 'none');
            setImp(cell, 'box-sizing', 'border-box');
            setImp(cell, 'border', '1px solid #000000');
        });

        const nameCells = Array.from(table.querySelectorAll('tr.individual-footer-names-row td')) as HTMLElement[];
        nameCells.forEach((cell) => {
            setImp(cell, 'vertical-align', 'top');
            setImp(cell, 'padding-top', '3px');
            setImp(cell, 'padding-bottom', '0');
            setImp(cell, 'padding-left', '2px');
            setImp(cell, 'padding-right', '2px');
            setImp(cell, 'min-height', '48px');
            setImp(cell, 'height', 'auto');
            setImp(cell, 'max-height', 'none');
            setImp(cell, 'overflow', 'visible');
            setImp(cell, 'line-height', '1.2');
            setImp(cell, 'box-sizing', 'border-box');

            let inner: HTMLElement | null = cell.querySelector(':scope > span, :scope > div') as HTMLElement | null;
            if (!inner && (cell.textContent || '').trim()) {
                inner = document.createElement('div');
                inner.className = 'pdf-footer-user-inner';
                while (cell.firstChild) {
                    inner.appendChild(cell.firstChild);
                }
                cell.insertBefore(inner, cell.firstChild);
            }

            if (inner) {
                setImp(inner, 'display', 'block');
                setImp(inner, 'overflow', 'visible');
                setImp(inner, 'line-height', '1.2');
                setImp(inner, 'transform', 'none');
                setImp(inner, 'box-sizing', 'border-box');
            }

            let pad = cell.querySelector('.pdf-footer-cell-pad') as HTMLElement | null;
            if (!pad) {
                pad = document.createElement('div');
                pad.className = 'pdf-footer-cell-pad';
                pad.setAttribute('aria-hidden', 'true');
                cell.appendChild(pad);
            }
            setImp(pad, 'display', 'block');
            setImp(pad, 'width', '100%');
            setImp(pad, 'height', '6px');
            setImp(pad, 'min-height', '6px');
        });
    });
}

export function syncIndividualFooterFieldsOnPdfClone(
    root: HTMLElement,
    fields: IndividualFooterPdfFieldPlacement[]
): void {
    for (const item of fields) {
        const node = root.querySelector(`.filled-field[data-field-id="${item.fieldId}"]`) as HTMLElement | null;
        if (!node) {
            continue;
        }
        const renderedHeight = getIndividualFooterRenderedHeightPx(
            item.placementHeight,
            item.bandCount,
            item.minHeight
        );
        const extraHeight = Math.max(0, renderedHeight - item.placementHeight);
        const renderedTop = Math.max(0, item.placementY - extraHeight);
        setImp(node, 'height', `${renderedHeight}px`);
        setImp(node, 'top', `${renderedTop}px`);
        setImp(node, 'overflow', 'visible');
    }
}

/** Minor vertical tweaks after {@link applyIndividualFooterTemplatePdfCaptureStyles} (html2canvas). */
export function applyIndividualFooterPdfFineTuneStyles(root: HTMLElement): void {
    root.querySelectorAll('.individual-footer-preview .xyz-signatures .xyz-signatures-blank td > div').forEach((node) => {
        const wrapper = node as HTMLElement;
        setImp(wrapper, 'transform', 'none');
    });

    root.querySelectorAll('.individual-footer-preview .xyz-signatures .xyz-sig-time').forEach((node) => {
        const timeEl = node as HTMLElement;
        setImp(timeEl, 'font-size', '8px');
        setImp(timeEl, 'line-height', '1');
        setImp(timeEl, 'margin-top', '0');
        setImp(timeEl, 'transform', 'none');
    });

    root.querySelectorAll('.individual-footer-preview .xyz-signatures tr:last-child td').forEach((node) => {
        const cell = node as HTMLElement;
        setImp(cell, 'padding-top', '3px');
    });

    root.querySelectorAll('.individual-footer-preview .xyz-signatures tr:last-child td > span').forEach((node) => {
        const valueEl = node as HTMLElement;
        setImp(valueEl, 'transform', 'none');
        setImp(valueEl, 'line-height', '1.2');
    });
}

export function applyPipelineSignaturePdfCaptureStyles(root: HTMLElement): void {
    root.querySelectorAll('.pipeline-signature-field').forEach((node) => {
        const fieldEl = node as HTMLElement;
        setImp(fieldEl, 'box-sizing', 'border-box');
        setImp(fieldEl, 'display', 'flex');
        setImp(fieldEl, 'align-items', 'center');
        setImp(fieldEl, 'justify-content', 'center');
        setImp(fieldEl, 'width', '100%');
        setImp(fieldEl, 'height', '100%');
        setImp(fieldEl, 'overflow', 'visible');
        setImp(fieldEl, 'text-align', 'center');
    });

    root.querySelectorAll('.pipeline-signature-slot').forEach((node) => {
        const slotEl = node as HTMLElement;
        setImp(slotEl, 'display', 'flex');
        setImp(slotEl, 'align-items', 'center');
        setImp(slotEl, 'justify-content', 'center');
        setImp(slotEl, 'width', '100%');
        setImp(slotEl, 'height', '100%');
        setImp(slotEl, 'min-width', '0');
        setImp(slotEl, 'overflow', 'visible');
    });

    root.querySelectorAll('.pipeline-signature-slot img').forEach((node) => {
        const imgEl = node as HTMLElement;
        setImp(imgEl, 'display', 'block');
        setImp(imgEl, 'margin', '0 auto');
        setImp(imgEl, 'max-height', '100%');
        setImp(imgEl, 'max-width', '100%');
        setImp(imgEl, 'object-fit', 'contain');
    });
}

export async function waitForPdfCaptureImages(root: HTMLElement, timeoutMs = 8000): Promise<void> {
    const images = Array.from(root.querySelectorAll('img')) as HTMLImageElement[];
    if (images.length === 0) {
        return;
    }

    await Promise.all(
        images.map(
            (img) =>
                new Promise<void>((resolve) => {
                    if (img.complete && img.naturalHeight > 0) {
                        resolve();
                        return;
                    }
                    const timer = window.setTimeout(() => resolve(), timeoutMs);
                    const done = () => {
                        window.clearTimeout(timer);
                        img.removeEventListener('load', done);
                        img.removeEventListener('error', done);
                        resolve();
                    };
                    img.addEventListener('load', done);
                    img.addEventListener('error', done);
                })
        )
    );
}

export function buildIndividualFooterPdfFieldPlacements(
    templateFields: Array<{ id: string; type: string; placement?: { height?: number; y?: number } | null }>,
    bandCount: number,
    minHeight = 136
): IndividualFooterPdfFieldPlacement[] {
    return (templateFields || [])
        .filter((field) => field.type === 'individual_pipeline_footer' && field.placement)
        .map((field) => ({
            fieldId: field.id,
            placementHeight: Number(field.placement?.height || 0),
            placementY: Number(field.placement?.y || 0),
            bandCount: Math.max(1, bandCount),
            minHeight
        }));
}
