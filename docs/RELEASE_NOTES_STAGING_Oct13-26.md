# Velocity DMS — Staging Release Notes

**Source:** WFpoints_Oct13-26 (1).xlsx  
**Scope:** Items marked **Available** under **Staging Deployment**  
**Period:** 13–26 Oct 2026 (tracking sheet)

---

## 1. Template / form creation

| # | Item | Notes |
|---|------|--------|
| 1 | Remove “Test Email Admin” from form creation | Test Email Admin control removed from the form/template creation screen. |
| 10 | Save as Draft for initiator and admin | Save as Draft is available for initiators and administrators (template drafts and document drafts). Drafts do not enter approval until submitted. Document codes may not stay strictly sequential when drafts are reserved. |

## 2. Workflow, email, and approvals

| # | Item | Notes |
|---|------|--------|
| 2 | Opinion / approval actions from email | Authorized users can complete applicable workflow opinions and actions from email links and the portal. Duplicate opinion acceptance shows an “already accepted” style message. |
| 11 | Confirmation before Reject / Send Back / Send Back to Initiator | Confirmation prompts run before Reject, Send Back, and Send Back to Initiator (portal and email flows where applicable). |
| 12 | Initiator in individual approval pipeline | Initiator can be selected/configured in the individual approval pipeline in the template builder. |
| 13 | “Already approved” messaging | When a step is already approved, users see “Already approved by you” or “Already approved by [name]” (with current level context) instead of a generic not authorized message. |
| 15 | Reject button hidden (temporary) | Reject is hidden on the portal approval screen and in approval emails per client request. |
| 16 | Signature required before actions | Users without an uploaded signature are prompted to upload a signature before approve/send back/hold-type actions. |
| 18 | Initiator manual email to current step | Initiator can send a manual reminder email to the current-step approver (Send Reminder / manual reminder APIs). |
| 21 | Put application on Hold | Approvers can put in-progress applications on Hold (with reason) and release them with Unhold. Hold blocks other workflow actions until released. |
| 34 | Conditional workflow forwarding | Workflow threshold rules support conditional routing (e.g. override/insert steps) via Workflow Pipelines → Thresholds. |

## 3. Attachments and file upload

| # | Item | Notes |
|---|------|--------|
| 3 | Remove attachments before submit | Users can remove uploaded attachments from the fill screen before final submission. |
| 4 | File size validation (3 MB error) | Per-user max attachment size is read from user master (`numMaxAttachmentMb`); frontend and backend use the same limit. |
| 17 | Attach existing application from portal | Attachment fields support local files and **portal** picker; portal lists completed template applications for selection. |
| 20 | Configurable max file size (e.g. 10 MB) | Administrators set max attachment size (MB) per user in User Management (and SAP employee user creation). Default remains 5 MB unless changed. |

## 4. UI, menu, PDF, and performance

| # | Item | Notes |
|---|------|--------|
| 5 | Form description on Menu Items | Form/menu configuration includes a description/title field for identification (per UAT: Form Title). |
| 6 | Activity logs in PDF | PDF download supports workflow activity logs (e.g. **Download PDF With Logs** on application details). |
| 7 | Loaders on long operations | Loading states added for operations such as save, upload, submit, and PDF/report generation. |
| 8 | Table add row/column on fill | Users can add rows/columns on predefined table fields during fill where the table field allows it. |
| 9 | Portal speed optimization | General performance improvements on frequently used portal paths (no single UI change). |

## 5. Access, visibility, and delegation

| # | Item | Notes |
|---|------|--------|
| 14 | Individual user on Form Visibility | Individual users can be assigned on form visibility settings. |
| 24 | Super User role | Role for broader access to forms/reports across departments (per requirement sheet; configure via roles/permissions). |
| 30 | SAP employee master + manual users | SAP HCM employee sync/list and option to create Velocity users from employee records; manual user creation remains available. |
| 33 | Create form on behalf of another user | Delegation / on-behalf fill: users with active delegation can start or continue applications for the delegating user. |

## 6. Individual footer pipeline and PDF

| # | Item | Notes |
|---|------|--------|
| 39 | PDF signatures with individual footer pipeline | PDF generation includes approver signatures for individual footer pipeline layouts when approval history and signatures exist. |
| 40 | Fixed vs runtime approver users | Pipeline supports configured fixed users and runtime user selection on fill where the template allows. |
| 43 | Last footer user showing Pending at 100% | Corrected status display for the last individual footer slot when the application is fully approved. |
| 44 | More than five footer columns | Footer preview allows more signature columns on one row (increased per-row limit; layout height adjusts for multiple bands). |
| 45 | Dashboard data | Dashboard figures/cards aligned with underlying application data. |

## 7. Audit / action metadata

| # | Item | Notes |
|---|------|--------|
| 19 | Action source and IP | Approval history entries can record client IP (and related metadata) for actions taken via portal/email links. MAC address is not collected in standard browser flows. |

---

## Out of scope for this staging list

Requirements in the same workbook **not** marked **Available** on staging (e.g. general cell calculations, Word editor auto margins, landscape column shrink, read-only role, custom PDF/PNG form upload, user manuals, GUI/mobile pass) are excluded from this document.

---

## Suggested client UAT focus

1. End-to-end submit with attachments (remove file, portal attach, size limit per user).  
2. Draft save/resume and final submit numbering.  
3. Email link approve/opinion/hold/send back with confirmations and signature check.  
4. Individual footer pipeline on fill, approval, and **Download PDF With Logs**.  
5. SAP employee → create user; delegation / on-behalf fill.  
6. Hold / unhold and manual reminder from initiator.
