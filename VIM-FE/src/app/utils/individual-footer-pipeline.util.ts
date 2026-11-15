/** Max signature columns per row in Individual Pipeline (Footer) tables. */
export const INDIVIDUAL_FOOTER_PIPELINE_MAX_SLOTS_PER_ROW = 6;

/** Legacy rounded band height (sig + header + names). Prefer {@link getIndividualFooterBandStackHeightPx}. */
export const INDIVIDUAL_FOOTER_BAND_HEIGHT_PX = 92;

/** Matches `table.individual-pipeline-footer-table tr.xyz-signatures-blank` min height in global styles. */
export const INDIVIDUAL_FOOTER_SIG_ROW_HEIGHT_PX = 44;

export const INDIVIDUAL_FOOTER_HEADER_ROW_HEIGHT_PX = 20;

/** Space for name + designation + department (up to ~4 wrapped lines at 7.5px). */
export const INDIVIDUAL_FOOTER_NAMES_ROW_HEIGHT_PX = 48;

export const INDIVIDUAL_FOOTER_BAND_GAP_PX = 4;

export const INDIVIDUAL_FOOTER_MIN_RENDERED_HEIGHT_PX = 136;

export interface IndividualFooterSlotCell {
    section: any;
    slotUser: any;
    slotIndex: number;
}

export interface IndividualFooterHeaderSegment {
    section: any;
    colspan: number;
}

export interface IndividualFooterTableBand {
    cells: IndividualFooterSlotCell[];
    headerSegments: IndividualFooterHeaderSegment[];
}

export function buildIndividualFooterHeaderSegments(cells: IndividualFooterSlotCell[]): IndividualFooterHeaderSegment[] {
    const segments: IndividualFooterHeaderSegment[] = [];
    for (const cell of cells) {
        const last = segments[segments.length - 1];
        if (last && last.section === cell.section) {
            last.colspan += 1;
        } else {
            segments.push({ section: cell.section, colspan: 1 });
        }
    }
    return segments;
}

export function buildIndividualFooterTableBands(
    sections: any[],
    slotsForSection: (section: any) => any[],
    maxSlotsPerRow = INDIVIDUAL_FOOTER_PIPELINE_MAX_SLOTS_PER_ROW
): IndividualFooterTableBand[] {
    const flat: IndividualFooterSlotCell[] = [];
    for (const section of sections || []) {
        const slots = slotsForSection(section) || [];
        slots.forEach((slotUser, slotIndex) => {
            flat.push({ section, slotUser, slotIndex });
        });
    }
    if (flat.length === 0) {
        return [];
    }
    const bands: IndividualFooterTableBand[] = [];
    for (let i = 0; i < flat.length; i += maxSlotsPerRow) {
        const cells = flat.slice(i, i + maxSlotsPerRow);
        bands.push({
            cells,
            headerSegments: buildIndividualFooterHeaderSegments(cells)
        });
    }
    return bands;
}

export function getIndividualFooterBandStackHeightPx(bandCount: number): number {
    const bands = Math.max(1, Number(bandCount) || 1);
    const rowStack =
        INDIVIDUAL_FOOTER_SIG_ROW_HEIGHT_PX
        + INDIVIDUAL_FOOTER_HEADER_ROW_HEIGHT_PX
        + INDIVIDUAL_FOOTER_NAMES_ROW_HEIGHT_PX;
    const gaps = Math.max(0, bands - 1) * INDIVIDUAL_FOOTER_BAND_GAP_PX;
    const safety = bands * 6;
    return bands * rowStack + gaps + safety + 8;
}

export function getIndividualFooterRenderedHeightPx(
    placementHeight: number,
    bandCount: number,
    minHeight = INDIVIDUAL_FOOTER_MIN_RENDERED_HEIGHT_PX
): number {
    const base = Math.max(Number(placementHeight) || 0, minHeight);
    const bands = Math.max(1, Number(bandCount) || 1);
    const stackHeight = getIndividualFooterBandStackHeightPx(bands);
    return Math.max(base, stackHeight);
}
