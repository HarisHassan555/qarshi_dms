# CAPF (Capital Appropriation Fund) Form — Business & Technical Flow

This document describes how **CAPF** works in Qarshi DMS: lifecycle, statuses, departmental pipeline, post-pipeline steps (CEO → Finance → asset/PR), PDF handling, and how **email** content is built (including the HTML snippet / inline image paths).

---

## 1. Business view

### 1.1 Purpose

CAPF supports **capital asset purchase** requests: structured form data (asset, vendor, feasibility, third-party assessment, etc.), a **departmental approval pipeline** configured on the form (`txtApprovalPipeline`), optional **initial signer** step, then **CEO** sign-off, **Finance** asset code assignment, and finally **PR code** / completion steps as implemented in code and UI.

### 1.2 Typical journey

1. **Initiator** submits the application; a **PDF snapshot** of the form is stored (often with per-stage copies for CAPF overlays).
2. **Departmental stages** run in pipeline order (e.g. initiator HoD, technical expert, procurement, …). Multiple **heads per department** may each need to approve before the level advances.
3. When the **last configured pipeline stage** completes, the case moves to **`CEO_PENDING`** (if a CEO user exists in role configuration) or straight to **`ASSET_PENDING`** / Finance.
4. **CEO** approves → status **`ASSET_PENDING`**; **Finance** receives email to assign **asset code** (dedicated UI route).
5. After asset code (and any finance approval rules), workflow can progress to **final approval** / **PR code** assignment and **`APPROVED`** as implemented.

### 1.3 Channels

- **Web** — Approver identity from JWT/session; SPA may apply extra client-side guards (e.g. multi-HOD, level mapping).
- **Email** — Links such as `/approveApplicationFromEmail?applicationId=…&userId=…` pass **explicit `userId`**; backend authorizes and records **that** user. Outbound emails include **action buttons**, **prior approvals** table HTML, and often a **visual preview** (see §2.5).

---

## 2. Technical view

### 2.1 How the system recognizes CAPF

**Backend** (`CfgTblCustomFormApplicationDAO.isCapfForm` and related):

- Form **name** / **code** contains CAPF / capital-assets wording, or
- Application flags / status (e.g. `CEO_PENDING`, `ASSET_PENDING`, PR/asset markers) align with CAPF lifecycle.

**Frontend** (`application-details.component.ts` — `isCapfForm()`):

- Uses `txtFormCode`, form name, `isCapfForm` flag, negative `intCurrentApprovalLevel` in edge cases, and history heuristics.

CAPF is explicitly **excluded** from the **individual pipeline footer** approval sequence (`useIndividualPipelineFlow` is false when `isCapfForm`).

### 2.2 Levels and pipeline indexing (important)

- `intCurrentApprovalLevel` is the **pending** step index used across the DAO and UI. CAPF uses special values:
  - **`-1`** — **Initial signer** (when configured in application JSON).
  - **`0`** — **Initiator’s department HoD** (virtual stage; not always the first row of `txtApprovalPipeline`).
  - **`1`…** — Align with **pipeline indices** such that **`pipelineIndex = currentLevel - 1`** in several branches (after HoD / initial signer logic).

The UI may inject a **synthetic initiator** pipeline row (`intApprovalOrder = -1`) so cards line up with history.

**History `level`**: departmental approvals use **positive** levels tied to pipeline order; **CEO** is stored with **`level = -99`** and **`role = "CEO"`** in `appendHistoryEntry` (short-circuit CEO path).

### 2.3 Approval recording and PDF persistence

- Each departmental approval appends to **`txtApprovalHistory`** (and related prior-approvals fields where used).
- For CAPF, after pipeline approvals the DAO calls **`persistCapfSignedPdf(application, form)`** so **`blbPdfData`** (and **`blbPdfForStage(n)`**) reflect **signatures overlaid** on a **pristine base** (prefer **`blbPdfForStage(0)`** / original snapshot) to avoid double-drawing.
- **CEO** stage: the dedicated `CEO_PENDING` block must also refresh the stored PDF ( **`persistCapfSignedPdf`** ) **before** finance notification emails, so the **merged PDF** includes the CEO slot; otherwise preview sources that read **per-stage blobs** can miss the CEO line.

### 2.4 Email “wrapper” (all forms, including CAPF)

Most notifications use **`generateApprovalEmailHtml(...)`**, which builds:

- Outlook-oriented **HTML layout** (tables, MSO conditionals).
- **Greeting** and **metadata** (application code, form name, level, status, remarks).
- **`buildApprovalHistoryHtml`** — **“Prior Approvals”** table (level, approver, role, status, date, signature image via `getSignature?userId=`).
- Optional **Approve / Reject / Send back / Send back to initiator** links (level ≥ 2 rules for send-back buttons).

That wrapper is **not** the CAPF pixel-perfect form; it is the **standard notification shell** around CAPF-specific assets below.

### 2.5 CAPF inline image + PDF attachment (`sendEmailWithInlineFormPreview`)

For **`isCapf == true`**, `sendEmailWithInlineFormPreview` (DAO):

1. Resolves **PDF bytes** via **`resolveBestPdfBytesForEmail`** (stored application PDF / generated fallback).
2. **Always attaches** the PDF file to the email (when bytes exist).
3. Builds an **inline PNG** for the message body:
   - Prefer **`renderPdfToPng(pdfBytes)`** on those bytes.
   - Else **`buildCapfPreviewPng(application, form)`**, which:
     - For **`ASSET_PENDING`**, prefers **`blbPdfData`** first (post-CEO merged PDF) so finance emails match the on-disk signed document.
     - Else may use **`blbPdfForStage(intCurrentApprovalLevel)`**, then **`blbPdfData`**, then **generated CAPF PDF** (`generateCapfPdf` / `getOrBuildCapfPdf`).
4. Embeds the image with **`appendCapfInlineImage(html, cid)`** (adds `<img src="cid:…">`).
5. Sends via **`sendHtmlEmailWithInlineImage`** or **`sendHtmlEmailWithInlineImageAndAttachments`**.

So the **“email version of the output”** the user sees as a **picture of the form** is primarily **first page of the stored/generated PDF**, not the fragment template alone.

**Practical layout note:** the CAPF form image used in emails is usually generated from the Angular PDF capture in `VIM-FE/src/app/services/application-pdf/application-pdf.service.ts` (`generateCapfAbcHtml` + `renderCapfPdfFromElement`). For visible CAPF email-output spacing fixes, update that frontend PDF HTML/CSS first. Existing stored CAPF PDF blobs will keep the old layout until the PDF snapshot is regenerated or a new CAPF is submitted.

Recent CAPF email-output layout adjustments live in that service:

- Header/meta grid cells use vertical-middle alignment with safer padding/line-height so the text sits inside the boxes instead of touching the lower border.
- The PART-1 heading text is balanced vertically and `CONCERNED` is not underlined.
- The bottom job-completion signature row uses extra bottom spacing so the border under `(Sign & Desg.)` is lower and does not crowd the label.

### 2.6 HTML snippet / template path (`capf-email-fragment.html`)

The repo includes **`src/main/resources/templates/capf-email-fragment.html`** — a large, **email-safe** HTML fragment styled like the CAPF print layout (logo, fields, checkboxes, etc.).

**Hydration** is implemented in **`generateCapfEmailFragment(application, form, pipelines)`**:

1. **`loadCapfEmailTemplate()`** — reads the file once and caches it.
2. **Field placeholders** — values come from **`txtApplicationData`** via **`getFieldValue(...)`** using **`CfgTblCustomFormField`** labels (e.g. `CAPF #`, `Date`, `DIVISION / DEPARTMENT`, vendor block, feasibility / third-party checks, `{{FEASIBILITY_YES}}` / `{{THIRD_PARTY_NO}}`, etc.).
3. **`{{SIGNATURE_SLOTS}}`** — replaced with **`buildCapfSignatureSlotsHtml(pipelines, historyForEmail, baseUrl, hideFromOrder)`**:
   - Builds a **table of signature columns** matching pipeline departments (or **fallback slots** if pipeline empty).
   - Appends a **CEO column** from history (**level -99** / CEO role), not from pipeline JSON.
   - Uses **filtered history** for in-flight apps: **`filterCapfApprovalHistoryForCurrentStage`** and **`hideFromOrder`** so “future” signatures are not shown prematurely after send-back (see comments in DAO).

**Integration note:** `generateCapfEmailFragment` is the **single** caller of `buildCapfSignatureSlotsHtml` in the DAO. If your deployment’s emails **only** show the generic wrapper + PDF PNG and **not** this fragment body, confirm whether any mail path still **appends** the fragment into `generateApprovalEmailHtml` (or a dedicated CAPF mail builder). The **mechanism** for “snippet-based” CAPF email is this template + replacement + signature HTML generator.

### 2.7 CEO, Finance, and idempotency

- **`CEO_PENDING`** is handled in a **short-circuit** at the top of **`approveApplication`**: CEO role check, **idempotent** detection of an existing **CEO approval** in history ( **`level -99` / CEO-like role** ), **`appendHistoryEntry`**, **`persistCapfSignedPdf`** for CAPF, transition to **`ASSET_PENDING`**, **`sendFinanceEmails`**.
- **Finance** mail (`sendFinanceEmails`) for CAPF uses **`buildCapfPreviewPng`** for the inline PNG when attaching the CAPF inline image path.

### 2.8 CAPF-specific email / PDF utilities (reference)

| Concern | Location (approx.) |
|--------|---------------------|
| CAPF form detection | `CfgTblCustomFormApplicationDAO.isCapfForm` |
| Approve / CEO / persist PDF | `approveApplication`, `persistCapfSignedPdf`, `filterCapfApprovalHistoryForCurrentStage` |
| Generate CAPF PDF | `generateCapfPdf` |
| Email wrapper | `generateApprovalEmailHtml`, `buildApprovalHistoryHtml` |
| Inline image send | `sendEmailWithInlineFormPreview`, `appendCapfInlineImage`, `buildCapfPreviewPng`, `renderPdfToPng` |
| Snippet template | `resources/templates/capf-email-fragment.html` |
| Snippet + signature columns | `loadCapfEmailTemplate`, `generateCapfEmailFragment`, `buildCapfSignatureSlotsHtml`, `buildCeoSlot` |
| Finance / CEO subjects | `sendFinanceEmails`, CEO notification block in DAO |

**Frontend** (preview / tiles / PDF capture):

- `application-details.component.ts` — CAPF layout, `getPipelineData` initiator injection, **`shouldShowCapfExtraStages`**, **`getPipelineCardsForDisplay`** (multi-HOD cards), CAPF extra stages (CEO / asset / PR tiles), `id="capf-pdf-content"` for capture.

---

## 3. Replication checklist

| Topic | Verify |
|--------|--------|
| Pipeline JSON | `txtApprovalPipeline` order and department IDs match business stages |
| HoD map | `departmentHeadMap` / backend head resolution supports **multiple** heads per dept |
| Level math | CAPF `currentLevel` vs `pipelineIndex` vs history `level` (CEO **-99**) |
| PDF baseline | `blbPdfForStage(0)` preserved; overlays use filtered history |
| CEO → Finance | Status **`ASSET_PENDING`**; PDF updated **before** finance email |
| Email body | Wrapper HTML + optional **PDF→PNG** inline; fragment template if wired |
| Email links | `applicationId` + `userId` for state-changing GET endpoints |

---

## 4. Related doc

- [Individual Pipeline Footer flow](./individual-pipeline-footer-flow.md) — different product path; **not** used for CAPF sequence in backend.

---

*Aligned with `CfgTblCustomFormApplicationDAO` CAPF branches, `sendEmailWithInlineFormPreview`, `capf-email-fragment.html`, and Angular `application-details` CAPF handling.*
