# Qarshi DMS UAT Document

## 1. Purpose

This document provides a UAT-oriented view of the Qarshi DMS application based on the current codebase. It focuses on:

- Main modules
- Child modules / screens
- Functional working of each area
- UAT coverage points for business validation

## 2. Application Overview

Qarshi DMS is a workflow-driven digital management system where users can:

- Create configurable digital forms
- Submit applications and documents
- Route them through approval pipelines
- Manage master data and users
- Track approvals, completed applications, and logs

The application contains both classic form workflows and document-template-based workflows.

## 3. Main Modules, Child Modules, and Working

| Main Module | Child Module / Screen | Working |
|---|---|---|
| Authentication | Sign In | User logs into the system and accesses role-based menus. |
| Authentication | Change Password | Logged-in user updates own password. |
| Dashboard | Dashboard | Landing screen after login; acts as entry point to available business functions. |
| Master Data | Country | Maintain country records used in other forms and masters. |
| Master Data | City | Maintain city records and related lookup data. |
| Master Data | Media House | Manage media house / customer master information. |
| Master Data | Tax Category | Create and update tax category definitions used in transactions/forms. |
| Master Data | Product Category | Maintain grouping of products for structured data entry. |
| Master Data | Product | Manage product master records. |
| Master Data | Department | Create and maintain departments used in user assignment and approvals. |
| Master Data | Channel | Maintain business channel information. |
| Master Data | Signature | Manage user signatures used in approval workflows and document signing. |
| User Management | Users | Create, update, activate/deactivate, and manage users. |
| User Management | Password Policy | Define password rules for the application. |
| User Management | Permission | Assign user/role level access against menus and submenus. |
| User Management | Role Management | Manage menu permissions in a structured role-based format. |
| User Management | Menu List | Maintain parent menu definitions. |
| User Management | Submenu List | Maintain child menu definitions and route mapping. |
| User Management | Activity Logs | View user/system activity for audit tracking. |
| Custom Form Workflow | Form Builder | Admin/config user creates digital forms with fields, options, document headers, attachments, and approval pipeline. |
| Custom Form Workflow | Application | End user selects a form, fills fields, attaches documents, and submits an application. |
| Custom Form Workflow | Applications View | View submitted applications, open records, and inspect generated form output. |
| Custom Form Workflow | Pending Approvals | Shows applications currently waiting for the logged-in approver. |
| Custom Form Workflow | Application Details | Displays full application data, approval history, workflow stage, and action buttons. |
| Custom Form Workflow | Approved Applications | Shows completed/approved applications for review and downstream processing. |
| Custom Form Workflow | My Application | Allows user to review their own submitted digital applications. |
| Department Workflow | My Department Digital Application | Department-specific application listing and handling screen. |
| CAPF Workflow | CAPF | Dedicated Capital Appropriation Fund process for asset/capital purchase requests. |
| CAPF Workflow | Assign Asset Code | Finance/user assigns asset code after relevant CAPF approvals. |
| CAPF Workflow | PR Code | PR code assignment after approval progression. |
| CAPF Workflow | PO Code | PO code assignment/final downstream completion step. |
| Budget Workflow | Budget Approval | Specialized budget approval form flow. |
| Budget Workflow | Budget Approval View | View budget approval submissions and related workflow state. |
| Email Workflow | Email Approval | Approve/reject application directly from email link. |
| Digital Document Workflow | Template Builder | Build document-style templates over image/PDF background with fields and approval pipeline. |
| Digital Document Workflow | Template Forms / Template List | List available digital document templates. |
| Digital Document Workflow | Template Fill | Fill and submit a selected digital document template. |
| Digital Document Workflow | Template Pending Approvals | Pending approvals for template/document-based workflows. |
| Digital Document Workflow | Template Approval | Detailed approval screen for a selected template application. |
| Digital Document Workflow | Template Approved Applications | View approved/completed template-based applications. |
| Document Tools | Custom Document Builder | Additional custom document composition capability. |
| Document Tools | PDF Editor | Open and edit selected PDF-based document output. |
| Operations / Transactional | Order | Work with order listing and order processing flow. |
| Operations / Transactional | Order Detail | View/edit individual order details. |
| Operations / Transactional | Sale Invoice | Manage sales invoice records. |
| Operations / Transactional | Sale Invoice View | Read-only / alternate invoice viewing screen. |
| Operations / Transactional | Sale Status | Track sales invoice status. |
| Operations / Transactional | Marketing | Marketing-related operational listing screen. |
| Operations / Transactional | Procurement | Procurement-related operational listing screen. |
| Operations / Transactional | Tax Approval | Tax-related approval/processing screen. |
| Operations / Transactional | Finance | Finance operational listing/processing screen. |
| Operations / Transactional | Audit | Audit-related processing/listing screen. |
| Operations / Transactional | Payment | Payment-related records and actions. |
| Operations / Transactional | Vendor View | Vendor-related listing/view screen. |
| Operations / Transactional | Service Order | Service order management screen. |
| Operations / Transactional | Order By Department | Department-wise order visibility/segregation. |
| Monitoring / Logs | Scheduler / Log | System log or scheduler execution visibility. |
| Monitoring / Logs | SES Integration Logs | SES-specific integration log tracking. |
| Monitoring / Logs | Integration Logs | General integration/audit log view. |
| Reporting | Report | Reporting output screen. |
| Reporting | Transactions Details | Transaction detail inspection screen. |

## 4. Functional Working by Business Area

### 4.1 Authentication and Access Control

- User signs in through the login screen.
- Menus are shown according to role/user permission mapping.
- Unauthorized routes are blocked through frontend guard and submenu permission checks.

### 4.2 Master Data Management

- Admin/power users maintain lookup data like country, city, department, product, tax, signatures, and channel.
- These masters support data entry, reporting, routing, and approval dependencies in other modules.

### 4.3 Custom Form Lifecycle

1. Admin creates a form in `Form Builder`.
2. Form fields are configured, including text, date, table, attachment, footer, and individual pipeline controls.
3. Approval pipeline is attached by department or individual user.
4. End user opens `Application`, selects the form, fills it, and submits it.
5. Application appears in `Applications View` and `Pending Approvals`.
6. Approver opens `Application Details` and performs approve/reject/send back action.
7. Completed items move to `Approved Applications`.

### 4.4 CAPF Lifecycle

1. User submits CAPF request.
2. Request moves through configured departmental approval stages.
3. CEO/management and finance steps are triggered according to workflow.
4. Asset code, PR code, and PO code are assigned in later stages where applicable.
5. Final approved record remains available in approved/completed views.

### 4.5 Budget Approval Lifecycle

- User submits budget approval request.
- Approval chain is executed according to configured approvers.
- Record can be reviewed in detail and tracked in budget approval related screens.

### 4.6 Digital Document / Template Workflow

1. Admin creates document layout in `Template Builder`.
2. Fields are placed over document background/PDF.
3. Workflow/pipeline is configured for the template.
4. User opens `Template Fill`, enters values, and submits the document.
5. Approver works through `Template Pending Approvals` and `Template Approval`.
6. Finalized items are visible in `Template Approved Applications`.

### 4.7 User and Permission Administration

- Users, roles, menu access, submenu access, and password policy are centrally controlled.
- Activity logs provide traceability of actions.

### 4.8 Logs, Monitoring, and Reporting

- Integration logs help validate external/system communication.
- Activity logs support audit UAT.
- Report and transaction screens help validate output and trace business records.

## 5. Suggested UAT Scope

### 5.1 Menu and Access UAT

- Verify login works for valid users.
- Verify blocked access for unauthorized users.
- Verify menus/submenus appear according to role permissions.
- Verify user can only open allowed screens.

### 5.2 Master Data UAT

- Add new master record.
- Edit existing record.
- Search/filter records.
- Validate mandatory fields.
- Verify active/inactive behavior where applicable.

### 5.3 Custom Form UAT

- Create a new form with multiple field types.
- Submit application with valid data.
- Submit application with missing mandatory fields and verify validation.
- Upload attachments and verify restrictions.
- Verify generated application code.
- Verify application appears in views after submission.

### 5.4 Approval Workflow UAT

- Verify application reaches correct approver.
- Verify approve action updates status and history.
- Verify reject action updates status correctly.
- Verify send back returns item to expected previous stage.
- Verify email approval link behavior where configured.

### 5.5 CAPF UAT

- Submit CAPF form.
- Validate departmental approval chain.
- Validate CEO/finance progression.
- Validate asset code, PR code, and PO code assignment steps.
- Verify final application visibility and history.

### 5.6 Template Workflow UAT

- Create document template.
- Place fields correctly on document background.
- Fill and submit template.
- Verify template approval route.
- Verify approved template output and stored values.

### 5.7 User Management UAT

- Create new user.
- Assign role/department.
- Update permissions.
- Validate password policy enforcement.
- Review activity logs for performed actions.

### 5.8 Reporting and Logs UAT

- Open report screens successfully.
- Verify transaction details visibility.
- Verify logs are generated for system/user actions.

## 6. Key Roles Involved in UAT

- Admin
- Form Builder / Configuration User
- Initiator / End User
- Department Approver
- Finance User
- Management / CEO Approver
- Audit / Monitoring User

## 7. UAT Entry Criteria

- Application is deployed in UAT environment.
- Required users and roles are available.
- Menu permissions are configured.
- Master data is seeded.
- Approval pipeline test users are configured.
- Email links/integrations are available if email approval is in scope.

## 8. UAT Exit Criteria

- All critical business flows executed successfully.
- High severity defects closed or accepted.
- Approval workflows validated end to end.
- Documents, logs, and reporting outputs verified.
- Business users sign off on in-scope modules.

## 9. Notes / Assumptions

- This document is prepared from the currently available frontend routes, components, and supporting workflow documents in the repository.
- Some operational modules such as `Marketing`, `Procurement`, `Finance`, `Audit`, `Payment`, and `Vendor View` are present as active screens, but their exact business rules may need final confirmation from process owners during UAT walkthrough.
- CAPF and document-template workflows are clearly implemented and should be treated as major UAT areas.
