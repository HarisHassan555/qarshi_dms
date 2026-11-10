# Velocity DMS — Template builder and dynamic pipeline (summary)

This note complements [capf-form-flow.md](./capf-form-flow.md) and [individual-pipeline-footer-flow.md](./individual-pipeline-footer-flow.md).

## Making a form template

1. **Digital Document Builder** (`template-builder`) — Designers place fields on a canvas (text, dates, tables, attachments, document headers, etc.). Optional **workflow pipeline** steps (initiator, department, individual users, roles) define who approves and in what order. Saving calls `TemplateWorkflowService.saveTemplate()`, which POSTs `addNewCustomForm` / `updateCustomForm` and stores the layout JSON in `CfgTblCustomForm` (`txtFormDefinition` / fields + `txtApprovalPipeline`).

2. **Legacy form builder** (`formbuilder`, `univer-form-builder`) — Older paths still map fields to `CfgTblCustomFormField` rows and department pipelines on the form entity.

3. **Workflow pipelines library** (`workflow-pipelines`) — Reusable pipeline definitions can be attached when building templates.

## Filling a template and running the pipeline

1. **Template list / fill** (`template-list`, `template-fill/:id`) — End users open an active template, enter values, and submit. The SPA builds `txtApplicationData` (field values + optional `footerFields` for individual pipeline footers).

2. **Application record** — Submit creates `CfgTblCustomFormApplication` with status/level (`intCurrentApprovalLevel`), serialized pipeline from the form, and often a **PDF snapshot** (`blbPdfData`).

3. **Dynamic pipeline execution** — `CfgTblCustomFormApplicationDAO.approveApplication()` advances by pipeline index or individual footer sequence, appends `txtApprovalHistory`, sends email notifications, and may overlay signatures on the PDF. CAPF and budget-approval forms have specialized branches; most Velocity templates use department pipeline JSON or **individual pipeline footer** sequences.

4. **Approvals UI** — `template-pending-approvals`, `template-approval/:id`, and `application-details` show pipeline cards, history, and approve/reject/send-back actions.

## Official document archive (new)

The **Official Document Archive** (`official-document-archive`) is separate from approval templates: users scan or upload PDFs/images, Gemini proposes title/summary/label, and documents are searchable by those fields. See the PR description for API and configuration (`GEMINI_API_KEY`).
