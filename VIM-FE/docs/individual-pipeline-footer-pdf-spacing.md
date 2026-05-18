# Individual pipeline footer — PDF user-name spacing

Reference for the **email/stored PDF** fix that keeps approver names in the individual-pipeline signature table off the bottom cell border. Do not change budget/CAPF/expense slip footers when editing this.

## Problem

- Email PDFs for general forms with an **individual pipeline footer** are captured with **html2canvas** from the live preview DOM (`renderMultiPageXyzPapersToPdfBlob`).
- html2canvas often **ignores** `padding-bottom` on `<td>` and `transform: translateY()` on footer text.
- User names in the **last row** of `.xyz-signatures` looked glued to the line below.

## Correct fix (use this)

### Frontend — primary path

| Item | Location |
|------|----------|
| Main logic | `VIM-FE/src/app/services/application-pdf/application-pdf.service.ts` → `applyIndividualPipelineFooterPdfCaptureStyles()` |
| When it runs | `renderMultiPageXyzPapersToPdfBlob()` — on the **offscreen clone** before html2canvas |
| Scoped tables only | `.xyz-footer .xyz-signatures`, `.ec-slip-pipeline-footer .xyz-signatures` |
| Mechanism | Last footer row (`tr:last-child td`): fixed row height, `vertical-align: top`, and a real DOM spacer **`<div class="pdf-footer-cell-pad">`** (16px block) under the name — html2canvas renders this reliably |
| Capture CSS | Injected `<style>` on clone with `[data-pdf-capture-scope="…"]` — same selectors as above, **not** bare `.xyz-signatures` (avoids touching other tables) |

### Backend — fallback regeneration only

| Item | Location |
|------|----------|
| PDFBox drawing | `VIM-BE/.../CfgTblCustomFormApplicationDAO.java` → `drawDynamicBudgetSignatureTable()` |
| Name row Y | Top-align in the bottom name band: `ty = y + rowSig - bottomPadding - textBlockHeight` |

Stored email PDFs normally come from the **frontend snapshot** (`blbPdfData`). Regenerate PDF after code changes.

## What NOT to do

1. **Do not** add `padding-bottom` on `.xyz-footer`, `.xyz-generic-content`, `.ql-editor`, or generic tables for this fix.
2. **Do not** treat every last page with any `.xyz-footer` as footer-pinned. A4 flex snap is only for:
   - `.xyz-paper-footer-pinned`
   - `.xyz-paper--footer-pinned`
   - pages with `.xyz-footer-spacer` **and** individual-pipeline `.xyz-signatures`
3. **Do not** style all `.xyz-signatures` on the page — budget/other forms may use that class.
4. **Do not** rely on `.xyz-individual-pipeline-email` CSS alone for email capture; preview DOM does not use that class during html2canvas.
5. **Do not** use only `padding-bottom` / `translateY` on footer cells for PDF capture.

## Footer table structure (individual pipeline)

```
Row 1: .xyz-signatures-blank  — signature images + `.xyz-sig-time` (email/PDF only: top-aligned via `applyIndividualPipelineFooterPdfCaptureStyles`; screen preview keeps centered layout)
Row 2: th (grey)              — field labels ("New Field:")
Row 3: td > span|div          — user names  ← spacing fix applies here only
```

## Signature + timestamp vertical position (email/PDF only)

After approval, email PDFs **overlay** signatures on the stored snapshot via `applyDynamicFooterSignaturesToPdf()` → `drawDynamicFooterSignaturesOnly()` / `drawDynamicFooterSignatureLastEntryOnly()`. Layout uses `computeDynamicFooterOverlaySignatureYs()` so the timestamp is **above the grey header row**, not on it.

Frontend html2canvas (submission snapshot): `applyIndividualPipelineFooterPdfCaptureStyles()` pins `.xyz-signatures-blank td > div` to `position: absolute; top: 4px` when sig/time is present.

Do **not** change screen preview CSS for signature row alignment.

Screen preview (`application-details.component.css`, `application.component.css`) should keep `justify-content: center` on `.xyz-signatures-blank td > div`.

## How to verify

1. Form with **individual pipeline footer** (not department-only footer).
2. Submit or regenerate PDF (`updateApplicationPdf` / download from application details).
3. Open email attachment or stored PDF: names in footer row 3 should sit **above** the bottom border with clear gap.
4. Word-editor and body tables should **not** have extra blank space below cells or a large gap above the footer unless the form uses footer-pinned layout on purpose.

## Changelog

- **2026-05-15**: Spacer div + scoped selectors; removed broad footer-pinned on any `hasFooter` last page (fixed extra gap under word editor / body tables).
