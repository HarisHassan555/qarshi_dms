# Individual Pipeline Footer — Business & Technical Flow

This document describes how the **Individual Pipeline (Footer)** feature works in Qarshi DMS so it can be replicated or maintained without rediscovering behavior from the codebase.

---

## 1. Business view

### 1.1 Purpose

Some custom forms need a **linear approval chain** where each stage is tied to **specific people** (not only departments). The form designer adds an **Individual Pipeline Footer** block: it defines **sections** (e.g. “Reviewed by”, “Recommended by”, “Final approval”), and each section can list **one or many named approvers**.

The physical form preview shows a **signature table** at the end: each column is an approver slot; signatures and timestamps appear as people approve.

### 1.2 How work moves

1. **Submit** — The initiator completes the form; selected approvers and order are stored in the application payload.
2. **Sequential approval** — The system moves through approvers in a **fixed sequence** derived from footer sections (top to bottom; within a section, left-to-right / list order).
3. **Each approver** — Must be the current person in the sequence; they can **approve**, **reject**, or (when allowed) **send back** / **send back to initiator**.
4. **Parallel people in one section** — If a section lists **several** users, they are **separate steps** in the same order (not “any one of them” unless only one is configured).
5. **Send-back loop** — If a later approver sends the application back, earlier steps may need to **sign again**. After they re-approve, the workflow returns to the later stage; **prior send-back actions must not block** the same person from approving again on the web UI.
6. **Completion** — When the last approver in the sequence signs (and the backend transitions state accordingly), the individual-pipeline flow can mark the case **completed** (see backend for exact status rules vs budget-specific paths).

### 1.3 Channels

- **Web** — Logged-in user; identity comes from the session/JWT.
- **Email** — Link typically includes `applicationId` and `userId`; backend approves **as that user** without the SPA’s client-side guards.

Both channels should converge on the **same server-side rules** for who may act at the current step.

---

## 2. Technical view

### 2.1 Form builder (frontend)

- Field type: `individual_pipeline_footer` (see form builder field types).
- Control value: array of **sections**, each with at least:
  - `key`, `label`, `order`
  - `users`: array of user objects (`serUserId`, `txtUserName`, department/role fields as available)
- **Prepared-by**-style blocks may use `key: prepared_by`; backend sequence building can **skip** `prepared_by` when building the approval chain (see below).

### 2.2 Application payload (submission)

- On submit, the footer is normalized and stored (e.g. under `footerFields` and/or the per-field name) with **stable user ids**.
- Backend detection of “dynamic footer flow” uses **`footerFields`** in parsed `txtApplicationData` (non-empty list of footer maps with `users`).

### 2.3 Backend sequence & levels (`CfgTblCustomFormApplicationDAO`, Java)

**Sequence (`getBudgetApprovalSequence` / `BudgetApprover`)**

- Reads `footerFields` from application JSON.
- Sorts sections by `order`.
- Skips sections whose `key` is `prepared_by`.
- For each remaining section, **every user** in `users` becomes **one step** in order.
- Fallback: if no footer fields, legacy paths use `reviewers`, `recommenders`, `approver` arrays (budget-style).

**`intCurrentApprovalLevel`**

- **0-based index** into that sequence: `0` = first approver, `1` = second, etc.
- After a successful approve, the backend increments this index (or completes the workflow on the last step).

**Approval history (`txtApprovalHistory`)**

- For this flow, new entries typically set **`level` = (index at time of approval) + 1** — i.e. **1-based step number** matching “stage 1, 2, 3…” in UI tables.
- This **differs** from `intCurrentApprovalLevel`, which is **0-based** and points at the **pending** approver.

**Individual-pipeline mode**

- Activated when the form is **not** CAPF and either the form is **budget approval** or **dynamic footer** is detected (non-empty `footerFields` from stored application data).

**Authorization**

- Current approver must match `sequence.get(intCurrentApprovalLevel)`.
- Email approve passes explicit `userId`; web uses logged-in user id.
- Duplicate/idempotent cases may short-circuit if the user is already in approved history (server-side).

**PDF / signatures**

- Backend can append signature graphics to stored PDF for footer stages; avoid having the SPA upload a full regenerated PDF on approve if that would overwrite layered signatures when mixing email and portal.

### 2.4 Frontend — Application details (`application-details.component.ts`, Angular)

**Data sources**

- Footer for display: `applicationFormData.footerFields` or `individual_pipeline_footer`.
- **Slots per section (`getIndividualFooterSlots`)** must show **every configured user** in a section, while **enriching** from history by user id (signatures, dates). Do **not** replace the section’s user list with **only** history rows (that would hide people who have not signed yet).

**History level vs pending index**

- For individual footer (and budget approval in this app), when checking whether the **current pending stage** already has a terminal history row, compare:
  - **expected history `level` = `intCurrentApprovalLevel + 1`**
  - not `intCurrentApprovalLevel` directly (off-by-one caused false “not authorized” after the first approval).

**Send-back and “already acted” guards (web only)**

- Client blocks approve/reject if it thinks the current stage is already finished **this round**.
- **`SENT_BACK` / send-back variants must not** count as “stage completed” for that guard; otherwise when the workflow returns to the same approver after a lower stage re-approves, an **old send-back line** at the same `level` falsely blocks the button.
- **Only `APPROVED` and `REJECTED`** at the expected level (after optional “round reset” time from higher-level send-back) should mark the stage as handled for duplicate prevention.

**Dynamic approval workflow UI**

- `getDynamicApprovalWorkflow()` walks footer slots (post-merge) so each configured approver gets a **node** in the sidebar/progress UI, consistent with the signature table.

### 2.5 CAPF / mixed behaviors

- CAPF forms follow different level rules (e.g. initiator HOD as level `0`). Individual footer document layout may special-case CAPF (e.g. first section order). When replicating in another module, **branch CAPF explicitly** rather than assuming one global level scheme.

---

## 3. Replication checklist

| Area | What to verify |
|------|----------------|
| **Data model** | Footer sections + `users[]` stored on application; backend reads `footerFields` |
| **Level math** | 0-based `intCurrentApprovalLevel` vs 1-based history `level` |
| **Multi-user section** | Sequence contains **N** steps for **N** users; UI shows **N** columns |
| **Web guards** | Expected history level = pending index + 1; send-back ≠ stage complete |
| **Email vs web** | Same DAO authorization checks; beware SPA-only locks |
| **PDF** | Prefer server layering signatures; avoid clobbering multi-channel history |

---

## 4. Key files (reference)

| Layer | Location (approx.) |
|--------|---------------------|
| Backend DAO | `VIM-BE/.../CfgTblCustomFormApplicationDAO.java` — approve path, `getBudgetApprovalSequence`, `extractFooterFields`, history `level` |
| Backend API | `VIM-BE/.../CustomFormApplicationController.java` — `approveApplication`, `approveApplicationFromEmail` |
| Form fill | `VIM-FE/.../application.component.ts` — footer sections, submit payload |
| Form builder | `VIM-FE/.../form-builder.component.*` — `individual_pipeline_footer` |
| Details / PDF preview | `VIM-FE/.../application-details.component.ts` — slots, workflow, guards |

---

*Last aligned with implementation discussions around multi-approver footer display, web “not authorized” guards (level mapping + send-back), and email/web parity.*
