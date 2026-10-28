import { ProductTourDefinition } from './tour.types';

/**
 * One tour per page. Add `data-tour` anchors in that page's template, then list steps here.
 */
export const PRODUCT_TOUR_CATALOG: ProductTourDefinition[] = [
  {
    id: 'dashboard',
    page: 'Dashboard',
    roles: ['*'],
    name: 'Dashboard walkthrough',
    icon: '🏠',
    desc: 'Welcome banner, metrics, filters, approval board, and recent activity on your home screen.',
    steps: [
      {
        target: 'dashboard-hero',
        title: 'Welcome to your dashboard',
        body:
          'This is your home in Velocity. The scope badge shows whether you are viewing your own work or a wider admin view. Use the description here to understand what this page summarizes.',
      },
      {
        target: 'dashboard-quick-actions',
        title: 'Quick actions',
        body:
          'Refresh Dashboard loads the latest counts and board data. Fill Form starts a new digital submission. The badge shows how many items need your action right now.',
      },
      {
        target: 'dashboard-metrics',
        title: 'Summary metrics',
        body:
          'These cards count applications in your dashboard scope — typically open, completed, rejected, and items awaiting you. Numbers update when you refresh or change filters.',
      },
      {
        target: 'dashboard-filters',
        title: 'Dashboard filters',
        body:
          'Narrow the board and lists by date range, department, status, or form type. Apply Filters updates the view; Clear resets. Download CSV exports the filtered set.',
      },
      {
        target: 'dashboard-board',
        title: 'Approval stage board',
        body:
          'Applications are grouped into workflow lanes (for example Initiator, HOD, Approver). Click a card to open that application. Scroll horizontally to see every stage column.',
      },
      {
        target: 'dashboard-recent-apps',
        title: 'Recent applications',
        body:
          'A compact list of the latest submissions in your scope. Status badges and dates help you jump back into work without scanning the full board.',
      },
      {
        target: 'dashboard-activity',
        skipIfMissingTarget: true,
        title: 'Recent activity (admin)',
        body:
          'Administrators see a live feed of workflow events across users. Click the heading to open the full Activity Logs screen for auditing.',
      },
    ],
  },
  {
    id: 'signature',
    page: 'signature',
    roles: ['*'],
    name: 'Signature walkthrough',
    icon: '✍️',
    desc: 'Upload your approval signature, preview it, and understand how it is used in workflows.',
    steps: [
      {
        target: 'signature-header',
        title: 'Upload signature',
        body:
          'Your signature image is used when you approve department applications and on PDF exports. Keep it up to date so documents show the correct sign-off.',
      },
      {
        target: 'signature-upload',
        title: 'Your details & file',
        body:
          'Department is taken from your profile. Enter your designation as it should appear on approvals. Choose a clear image file (JPG, PNG, or GIF, max 5MB).',
      },
      {
        target: 'signature-actions',
        title: 'Save or reset',
        body:
          'Save uploads the signature to the server and activates it for your account. Reset clears the file picker and preview before you save.',
      },
      {
        target: 'signature-preview',
        title: 'Preview',
        body:
          'After you select a file—or when the page loads your existing signature—you will see a preview here. Confirm it is readable before saving.',
      },
      {
        target: 'signature-instructions',
        title: 'Best practices',
        body:
          'Use a white background, high resolution, and PNG when possible. Saved signatures are attached to approvals you complete in Velocity.',
      },
    ],
  },
  {
    id: 'sap-hcm-employees',
    page: 'sap-hcm-employees',
    roles: ['*'],
    name: 'Sap Employees walkthrough',
    icon: '👥',
    desc: 'Sync employee master data from SAP, filter the directory, and provision Velocity users.',
    steps: [
      {
        target: 'sap-hcm-header',
        title: 'SAP HCM employee directory',
        body:
          'This screen shows employees loaded from SAP HCM (ZHCM_EMP_INFO_PAD_WS). Records are stored in the database; profile photos are kept on disk for fast display.',
      },
      {
        target: 'sap-sync',
        title: 'Sync & search',
        body:
          'Sync from SAP pulls the latest roster from the integration service. Import XML file is for offline or test payloads. Refresh reloads from the database. Use the search box to filter by name, ID, email, or other visible fields.',
      },
      {
        target: 'sap-hcm-filters',
        title: 'List filters',
        body:
          'Narrow the grid by company, department, or active/inactive status. Clear filters resets all dropdowns and shows the full list again.',
      },
      {
        target: 'sap-hcm-grid',
        title: 'Employee grid',
        body:
          'Sort columns, browse photos and org fields, and check status badges. View opens full HR details in a modal. Create User starts provisioning a Velocity account prefilled from the SAP row (org hierarchy, designation, photo when available).',
      },
    ],
  },
  {
    id: 'organizations',
    page: 'organizations',
    roles: ['*'],
    name: 'Organizations walkthrough',
    icon: '🏢',
    desc: 'Read-only organization master data synced from SAP HCM.',
    steps: [
      {
        target: 'organizations-header',
        title: 'Organizations',
        body:
          'Top-level companies in your HR org structure. Rows are created and updated automatically when SAP HCM employee data is imported or synced—not edited on this screen.',
      },
      {
        target: 'organizations-toolbar',
        title: 'Refresh & search',
        body:
          'Refresh reloads the list from the server. Search filters the grid by organization name or code. The count below shows how many records loaded.',
      },
      {
        target: 'organizations-grid',
        title: 'Organization table',
        body:
          'Columns include name, SAP code, and active status. Sort by clicking column headers. Use this reference when assigning users or reviewing hierarchy on Sub Areas and Divisions.',
      },
    ],
  },
  {
    id: 'sub-areas',
    page: 'sub-areas',
    roles: ['*'],
    name: 'Sub Areas walkthrough',
    icon: '📍',
    desc: 'Sub areas under each organization, synced from SAP HCM.',
    steps: [
      {
        target: 'sub-areas-header',
        title: 'Sub Areas',
        body:
          'Sub areas sit under an organization in the HR hierarchy. Like organizations, they are maintained via SAP HCM employee import/sync (upsert).',
      },
      {
        target: 'sub-areas-toolbar',
        title: 'Refresh & search',
        body:
          'Refresh fetches the latest sub areas. Search matches names, codes, and related fields in the table. Check the row count for load status.',
      },
      {
        target: 'sub-areas-grid',
        title: 'Sub area table',
        body:
          'Each row shows sub area name, parent organization, code, and status. Sort columns as needed. Sub areas link forward to divisions and user org assignments.',
      },
    ],
  },
  {
    id: 'divisions',
    page: 'divisions',
    roles: ['*'],
    name: 'Divisions walkthrough',
    icon: '🗂️',
    desc: 'Divisions within sub areas, synced from SAP HCM.',
    steps: [
      {
        target: 'divisions-header',
        title: 'Divisions',
        body:
          'Divisions are the next level below sub areas. Data is synced from SAP HCM when employees are imported; this page is for viewing and auditing the master list.',
      },
      {
        target: 'divisions-toolbar',
        title: 'Refresh & search',
        body:
          'Use Refresh after a SAP sync to see new divisions. Search narrows the list quickly. Errors and loading state appear above the table.',
      },
      {
        target: 'divisions-grid',
        title: 'Division table',
        body:
          'See division name, sub area, organization, code, and active badge. Sortable columns help you verify structure before provisioning users from SAP HCM Employees.',
      },
    ],
  },
  {
    id: 'workflow-pipelines',
    page: 'workflow-pipelines',
    roles: ['*'],
    name: 'Workflow Pipelines walkthrough',
    icon: '🔀',
    desc: 'Reusable approval pipeline library and conditional threshold rules for digital documents.',
    steps: [
      {
        prepare: 'wp-close-modals',
        target: 'wp-header',
        title: 'Workflow Pipelines',
        body:
          'Manage two related areas: reusable approval pipelines you can attach to forms, and threshold rules that adjust routing when users submit a document (for example when a numeric field exceeds a limit).',
      },
      {
        prepare: 'wp-close-modals',
        target: 'wp-page-tabs',
        title: 'Pipelines vs Thresholds',
        body:
          'Pipelines lists the shared step library (initiator, departments, roles, individuals). Threshold & conditions defines IF field rules that override or insert approvers at submit time for a specific form.',
      },
      {
        prepare: 'wp-tab-pipelines',
        target: 'wp-pipelines-toolbar',
        title: 'Pipeline library actions',
        body:
          'New pipeline opens the step designer. Search filters the grid by name or code. Pipelines saved here can be referenced from the Digital Document Builder or legacy form approval settings.',
      },
      {
        prepare: 'wp-tab-pipelines',
        target: 'wp-pipelines-table',
        title: 'Pipeline list',
        body:
          'Each row shows name, code, step count, type, last modified, and active status. Edit opens the designer; Dup creates a copy. Keep inactive pipelines for history without assigning them to new forms.',
      },
      {
        prepare: 'wp-tab-thresholds',
        target: 'wp-thresholds-toolbar',
        title: 'Threshold rules',
        body:
          'New rule binds to one form and a numeric field. Rules are evaluated on submit in priority order (lower number first). Search helps you find rules by name or condition text in the table.',
      },
      {
        prepare: 'wp-tab-thresholds',
        target: 'wp-thresholds-table',
        title: 'Rules grid',
        body:
          'Columns summarize the IF condition, routing action (override approver or insert step), priority, and active flag. Edit opens the rule dialog with form workflow preview. Inactive rules are skipped at runtime.',
      },
    ],
  },
  {
    id: 'template-list',
    page: 'template-list',
    roles: ['*'],
    name: 'Digital Document List walkthrough',
    icon: '📋',
    desc: 'Browse published forms, open submissions, and (for admins) manage drafts, visibility, and duplicates.',
    steps: [
      {
        target: 'template-list-header',
        title: 'Digital documents',
        body:
          'All Velocity forms you can use appear here. Published documents are live for filling; admins also work with drafts from the builder before publishing.',
      },
      {
        target: 'template-list-delegation-access',
        skipIfMissingTarget: true,
        title: 'My access vs delegated',
        body:
          'Delegates see which forms they can open directly (My access) versus forms they may fill only through an active delegation (Delegated). Hover the Delegated badge to see who you submit on behalf of. Expired delegations no longer grant access.',
      },
      {
        target: 'template-list-mode',
        skipIfMissingTarget: true,
        title: 'Published vs drafts (admin)',
        body:
          'Switch between Published (live forms) and Drafts (work in progress). Drafts are only visible to administrators until you publish from the builder.',
      },
      {
        target: 'template-list-toolbar',
        title: 'Actions & search',
        body:
          'Admins can duplicate an existing form by ID, add a new document, or start a new draft. Everyone can search by name to narrow the list.',
      },
      {
        target: 'template-list-grid',
        skipIfMissingTarget: true,
        title: 'Document table',
        body:
          'See name, code convention, status, visibility, and last updated. Open starts a new submission. Rows may show My access, Delegated, or both. Admins get Edit, Activate/Deactivate, and Visibility to control who can see each form.',
      },
      {
        target: 'template-list-pagination',
        skipIfMissingTarget: true,
        title: 'Pagination',
        body:
          'Change page size and move through results when you have many documents. Search resets to the first page automatically.',
      },
    ],
  },
  {
    id: 'template-fill',
    page: 'template-fill',
    roles: ['*'],
    name: 'Fill form walkthrough',
    icon: '📝',
    desc: 'Choose self or on-behalf access, enter values, preview on the document, attach files, and submit or save a draft.',
    steps: [
      {
        target: 'template-fill-header',
        title: 'Form header',
        body:
          'The document name and code convention identify this template. Use the description to confirm you opened the correct form before you start filling.',
      },
      {
        target: 'template-fill-actions',
        title: 'Submit & drafts',
        body:
          'Save as Draft keeps your work in progress with a draft code. Submit (or Resubmit after send-back) sends the application into the approval workflow. View Attachments appears when files are linked to the form.',
      },
      {
        target: 'template-fill-access',
        skipIfMissingTarget: true,
        title: 'For self or on behalf',
        body:
          'When you have both personal access and an active delegation for this form, choose For self or On behalf of. Pick the user you are submitting for from the list. Only on-behalf users covered by a delegation that is active today (between Valid from and Valid to, not revoked) appear here.',
      },
      {
        target: 'template-fill-access-delegated',
        skipIfMissingTarget: true,
        title: 'Delegation-only access',
        body:
          'If you only have delegated access, select who you are submitting on behalf of before filling fields. When the delegation Valid to date has passed, that user disappears from this list and you cannot submit for them.',
      },
      {
        target: 'template-fill-fields',
        skipIfMissingTarget: true,
        title: 'Fields panel',
        body:
          'Mapped fields appear here—text, dates, tables, rich text, checkboxes, radios, and attachments. Some fields may be read-only until your workflow step allows editing. Attachment fields can pull files from your computer or the portal.',
      },
      {
        target: 'template-fill-stage',
        skipIfMissingTarget: true,
        title: 'Document preview',
        body:
          'Your entries render on the saved template layout (including multi-page backgrounds). Many fields can also be edited directly on the document, especially word-processor regions.',
      },
      {
        target: 'template-fill-pipeline',
        skipIfMissingTarget: true,
        title: 'Individual footer (when enabled)',
        body:
          'If this template uses an individual approval footer, add sections, name them, and assign users. Those names flow into signature blocks on the printed layout.',
      },
    ],
  },
  {
    id: 'change-password',
    page: 'change-password',
    roles: ['*'],
    name: 'Change password walkthrough',
    icon: '🔐',
    desc: 'Update your own password or (as admin) reset another user’s password.',
    steps: [
      {
        target: 'change-password-header',
        title: 'Password security',
        body:
          'Use this screen to set a new password. Regular users change their own credentials; administrators reset passwords for selected accounts.',
      },
      {
        target: 'change-password-admin-user',
        skipIfMissingTarget: true,
        title: 'Select user (admin)',
        body:
          'Choose the account to reset. The new password you enter below will apply to that user immediately after you click Reset.',
      },
      {
        target: 'change-password-current',
        skipIfMissingTarget: true,
        title: 'Current password',
        body:
          'Enter your existing password so Velocity can verify it is really you before applying a new one.',
      },
      {
        target: 'change-password-new',
        title: 'New password',
        body:
          'Type a strong new password and confirm it in the second field. Use the eye icons to show or hide what you typed. Your organization’s password policy may apply on save.',
      },
      {
        target: 'change-password-submit',
        title: 'Save changes',
        body:
          'Click Save to update your password, or Reset when you are an admin updating someone else. Fix any validation messages shown in red before submitting.',
      },
    ],
  },
  {
    id: 'users',
    page: 'users',
    roles: ['*'],
    name: 'Users walkthrough',
    icon: '👤',
    desc: 'Manage Velocity accounts, org hierarchy, roles, and profile photos.',
    steps: [
      {
        target: 'users-header',
        title: 'User management',
        body:
          'Administrators maintain who can sign in to Velocity. The list shows everyone with their org placement, role, and status.',
      },
      {
        target: 'users-toolbar',
        title: 'Add & search',
        body:
          'Add New opens the user form for manual creation. Search filters the table by name, login, email, and other visible columns.',
      },
      {
        target: 'users-grid',
        title: 'Users table',
        body:
          'Profile photos (or initials) appear beside each name. Review organization, sub area, division, department, designation, role, and email. Use the edit icon to update a user—including org fields and profile picture. Row checkboxes support bulk actions when enabled.',
      },
    ],
  },
  {
    id: 'roles',
    page: 'roles',
    roles: ['*'],
    name: 'Role management walkthrough',
    icon: '🛡️',
    desc: 'Roles list, permissions matrix, and add/edit role modals.',
    steps: [
      {
        prepare: 'roles-close-modals',
        target: 'roles-header',
        title: 'Role management',
        body:
          'Define who sees which menus in Velocity. Each role has a code, status, user count, and permission summary. Add New Role creates a profile with menu access.',
      },
      {
        prepare: 'roles-close-modals',
        target: 'roles-search',
        title: 'Search roles',
        body:
          'Filter by role name, code, or status text. The table updates as you type.',
      },
      {
        prepare: 'roles-close-modals',
        target: 'roles-grid',
        title: 'Roles table',
        body:
          'Permission badges show assigned areas at a glance. Edit opens the role and its menu checkboxes. Delete removes a role when it is no longer needed (confirm in the prompt).',
      },
      {
        prepare: 'roles-open-add-modal',
        target: 'roles-add-modal',
        title: 'Add role dialog',
        body:
          'This modal creates a new role. You will name it, pick menus and submenus, then save. The walkthrough will open the edit dialog next to show extra fields for existing roles.',
      },
      {
        target: 'roles-add-name',
        title: 'Role name',
        body:
          'Enter a clear display name (for example Initiator or HOD). A role code is generated automatically when the role is saved.',
      },
      {
        target: 'roles-add-permissions',
        title: 'Menu permissions (new role)',
        body:
          'Check a top-level menu to select all its submenus, or expand and pick individual screens. At least one submenu is required before Save Role.',
      },
      {
        target: 'roles-add-actions',
        title: 'Save or cancel',
        body:
          'Cancel closes without saving. Save Role persists the name and selected permissions, then refreshes the list.',
      },
      {
        prepare: 'roles-open-edit-modal',
        target: 'roles-edit-modal',
        skipIfMissingTarget: true,
        title: 'Edit role dialog',
        body:
          'Editing updates an existing role. Besides permissions, you can change name, code, and active status—fields that are not available when first creating a role.',
      },
      {
        target: 'roles-edit-details',
        skipIfMissingTarget: true,
        title: 'Role details',
        body:
          'Adjust Role Name and Role Code as needed for reporting and integrations. Uncheck Status Active to disable the role without deleting assigned users’ historical data.',
      },
      {
        target: 'roles-edit-permissions',
        skipIfMissingTarget: true,
        title: 'Menu permissions (edit)',
        body:
          'Same tree as add: parent menus toggle all children. Changes here control sidebar visibility for every user with this role after you update.',
      },
      {
        target: 'roles-edit-actions',
        skipIfMissingTarget: true,
        title: 'Update role',
        body:
          'Update Permissions & Role saves both metadata and checkbox selections. Use Cancel or X to close without saving.',
      },
      {
        prepare: 'roles-close-modals',
        title: 'You are set',
        body:
          'Use Add New Role or Edit anytime to adjust access. Pair roles with Users and SAP HCM provisioning so new accounts land with the right menus.',
      },
    ],
  },
  {
    id: 'activitylogs',
    page: 'activitylogs',
    roles: ['*'],
    name: 'Activity logs walkthrough',
    icon: '📜',
    desc: 'Audit user actions, filter the log, and (when permitted) correct application transactions.',
    steps: [
      {
        prepare: 'activitylogs-close-modal',
        target: 'activitylogs-header',
        title: 'Activity logs',
        body:
          'Administrators review sign-ins, approvals, form events, and system actions. Use this audit trail for compliance and troubleshooting.',
      },
      {
        target: 'activitylogs-search',
        title: 'Quick search',
        body:
          'Search narrows the loaded table by text across visible columns without re-querying the server.',
      },
      {
        target: 'activitylogs-filters',
        title: 'Server filters',
        body:
          'Filter by action type, success/failure, user ID, related application (entity) ID, and date range. Combine filters to pinpoint an incident.',
      },
      {
        target: 'activitylogs-filter-actions',
        title: 'Apply filters',
        body:
          'Apply Filters reloads data from the API with your criteria. Clear resets all filters and loads the full recent log again.',
      },
      {
        prepare: 'activitylogs-close-modal',
        target: 'activitylogs-grid',
        title: 'Log table',
        body:
          'Each row shows action, user, IP, device, timestamp, status, entity ID, and message. Edit Transaction appears for approve/reject/send-back rows when you have edit permission.',
      },
      {
        prepare: 'activitylogs-open-edit-modal',
        target: 'activitylogs-edit-header',
        skipIfMissingTarget: true,
        title: 'Edit transaction (modal)',
        body:
          'Opens the linked application history entry. This is a direct database correction—workflow emails and routing are not re-run.',
      },
      {
        target: 'activitylogs-edit-notice',
        skipIfMissingTarget: true,
        title: 'Important notice',
        body:
          'Read this warning before changing data. Use only when you must fix audit/history records after a support investigation.',
      },
      {
        target: 'activitylogs-edit-context',
        skipIfMissingTarget: true,
        title: 'Application context',
        body:
          'Application ID, form code, status, and history index are read-only anchors so you know which workflow step you are editing.',
      },
      {
        target: 'activitylogs-edit-fields',
        skipIfMissingTarget: true,
        title: 'Transaction fields',
        body:
          'Adjust action (approve, reject, send back), level, approver user, role, IP, via channel, and transaction date. Approver name, designation, and department update when you pick a user.',
      },
      {
        target: 'activitylogs-edit-remarks',
        skipIfMissingTarget: true,
        title: 'Remarks',
        body:
          'Update approval or rejection comments shown in the history. Keep remarks factual for future auditors.',
      },
      {
        target: 'activitylogs-edit-actions',
        skipIfMissingTarget: true,
        title: 'Save or close',
        body:
          'Save Transaction persists changes to the history record. Close discards unsaved edits.',
      },
      {
        prepare: 'activitylogs-close-modal',
        title: 'Done',
        body:
          'Return here anytime from the dashboard activity feed or the Activity Logs menu. Export and deeper reporting may be added alongside your IT policies.',
      },
    ],
  },
  {
    id: 'department-application',
    page: 'department-application',
    roles: ['*'],
    name: 'Department applications walkthrough',
    icon: '🏛️',
    desc: 'View submissions from your department and export CSV.',
    steps: [
      {
        prepare: 'dept-app-close-csv',
        target: 'dept-app-header',
        title: 'Department applications',
        body:
          'See digital applications submitted by colleagues in your department. The badge shows how many records are in the current view.',
      },
      {
        target: 'dept-app-toolbar',
        title: 'Search & export',
        body:
          'Search filters the table locally. Download CSV opens a column picker and exports the department list (when data is loaded).',
      },
      {
        prepare: 'dept-app-close-csv',
        target: 'dept-app-grid',
        skipIfMissingTarget: true,
        title: 'Applications table',
        body:
          'Review form code, title, submitter, department, status, and date. Use the eye icon to open the application in the approval viewer.',
      },
      {
        prepare: 'dept-app-open-csv',
        target: 'dept-app-csv-modal',
        skipIfMissingTarget: true,
        title: 'CSV export',
        body:
          'Choose which columns to include and in what order. Select from the left, remove from the right, then download.',
      },
      {
        prepare: 'dept-app-close-csv',
        title: 'Done',
        body: 'Use this list to monitor department workload and follow up on open submissions.',
      },
    ],
  },
  {
    id: 'template-approved-applications',
    page: 'template-approved-applications',
    roles: ['*'],
    name: 'Approved applications walkthrough',
    icon: '✅',
    desc: 'Applications you have fully approved and optional CSV export.',
    steps: [
      {
        prepare: 'template-approved-close-csv',
        target: 'tpl-approved-header',
        title: 'Approved applications',
        body:
          'Lists template applications where you completed approval. The count reflects server-side pagination totals.',
      },
      {
        target: 'tpl-approved-toolbar',
        title: 'Search & CSV',
        body: 'Search within the current page results. Download CSV exports approved rows with configurable columns.',
      },
      {
        prepare: 'template-approved-close-csv',
        target: 'tpl-approved-grid',
        skipIfMissingTarget: true,
        title: 'Approved table',
        body: 'Open any row to view the read-only application. Status and your approval date help audit your history.',
      },
      {
        prepare: 'template-approved-close-csv',
        target: 'tpl-approved-pagination',
        skipIfMissingTarget: true,
        title: 'Pagination',
        body: 'Change page size or move between pages when you have many approved items.',
      },
      {
        prepare: 'template-approved-open-csv',
        target: 'tpl-approved-csv-modal',
        skipIfMissingTarget: true,
        title: 'CSV column picker',
        body: 'Same pattern as other lists: add columns, order them, then download.',
      },
      {
        prepare: 'template-approved-close-csv',
        title: 'Done',
        body: 'Cross-check with Pending Approvals for items still awaiting your action.',
      },
    ],
  },
  {
    id: 'my-application',
    page: 'my-application',
    roles: ['*'],
    name: 'My applications walkthrough',
    icon: '📂',
    desc: 'Track submissions you initiated, open details with staged loading, and resubmit when sent back.',
    steps: [
      {
        prepare: 'my-app-close-detail',
        target: 'my-app-list-header',
        title: 'My digital applications',
        body:
          'Every template application you started appears here with status and approval level.',
      },
      {
        target: 'my-app-list-toolbar',
        title: 'Search & export',
        body: 'Search loads matching pages from the server. Download CSV exports your initiated applications.',
      },
      {
        prepare: 'my-app-close-detail',
        target: 'my-app-list-grid',
        skipIfMissingTarget: true,
        title: 'Your applications',
        body:
          'Edit & Resubmit appears when the workflow sent the form back to you. Open or Preview opens the detail view.',
      },
      {
        prepare: 'my-app-close-detail',
        target: 'my-app-list-pagination',
        skipIfMissingTarget: true,
        title: 'Pagination',
        body: 'Navigate pages and change how many rows load per request.',
      },
      {
        target: 'my-app-detail-summary',
        skipIfMissingTarget: true,
        title: 'Application detail',
        body:
          'When viewing one application, see code, status, Download PDF, Download PDF With Logs, Send Reminder (when allowed), Edit & Resubmit after send-back, and Back to return to the list. Summary loads first while other sections continue loading.',
      },
      {
        target: 'my-app-detail-stage',
        skipIfMissingTarget: true,
        title: 'Document preview',
        body:
          'Read-only view of the filled template on the saved layout. The page loads sections in order—application summary, then document, attachments (if any), then approval progress—so you always see one loader at a time.',
      },
      {
        target: 'my-app-detail-progress',
        skipIfMissingTarget: true,
        title: 'Approval progress & prior logs',
        body:
          'Track workflow steps, approvers, remarks, and completion percentage. Prior Approvals lists each action with signatures where available. Download CSV exports the log with columns you choose.',
      },
      {
        prepare: 'my-app-close-detail',
        title: 'Done',
        body: 'Start new forms from Digital Document List when you need to submit again.',
      },
    ],
  },
  {
    id: 'delegation-management',
    page: 'delegation-management',
    roles: ['*'],
    name: 'Delegation management walkthrough',
    icon: '🤝',
    desc: 'Assign delegates, set valid dates and document scope, and manage on-behalf form access.',
    steps: [
      {
        prepare: 'delegation-close-modal',
        target: 'delegation-header',
        title: 'Delegation management',
        body:
          'Administrators (and authorized users) define who may create or submit digital documents on behalf of another user—for example an assistant acting for an HOD—within a date range and for specific forms.',
      },
      {
        target: 'delegation-toolbar',
        title: 'Add & search',
        body:
          'Add New opens the delegation form. Search filters the grid locally by delegate, on-behalf user, documents, or status text.',
      },
      {
        target: 'delegation-filters',
        title: 'Status filters',
        body:
          'Filter by Active, Scheduled, Expired, or Revoked. Apply Filters reloads from the server; Clear resets. Expired means Valid to is before today—those users no longer appear when delegates fill forms.',
      },
      {
        target: 'delegation-summary',
        title: 'Summary counts',
        body:
          'Quick counts of active, scheduled, and expired delegations help you spot records that need renewal before access ends.',
      },
      {
        prepare: 'delegation-close-modal',
        target: 'delegation-grid',
        title: 'Delegations table',
        body:
          'Each row shows delegate, on-behalf user, scoped documents, valid from/to, status, optional proof attachment, and actions to edit or revoke.',
      },
      {
        prepare: 'delegation-open-add-modal',
        target: 'delegation-modal',
        skipIfMissingTarget: true,
        title: 'Add delegation dialog',
        body:
          'Create a new delegation. The walkthrough opens this dialog to show each section; cancel or Close when you are done exploring.',
      },
      {
        target: 'delegation-modal-users',
        skipIfMissingTarget: true,
        title: 'Delegate & on-behalf users',
        body:
          'Delegate user is who will fill/submit. On behalf of is whose identity the submission uses (e.g. HOD). Pick published documents after selecting the on-behalf user.',
      },
      {
        target: 'delegation-modal-dates',
        skipIfMissingTarget: true,
        title: 'Valid from & valid to',
        body:
          'Access is only effective on dates within this inclusive range. After Valid to, the on-behalf user is hidden on Fill Form and delegated badges drop off Digital Document List.',
      },
      {
        target: 'delegation-modal-documents',
        skipIfMissingTarget: true,
        title: 'Document scope',
        body:
          'Select which published forms this delegation covers. Only forms the on-behalf user could see on Digital Document List are offered. Scope is stored as form IDs in txt_scope.',
      },
      {
        target: 'delegation-modal-actions',
        skipIfMissingTarget: true,
        title: 'Save delegation',
        body:
          'Save persists the record and optional email notification to both parties. Close discards unsaved changes.',
      },
      {
        prepare: 'delegation-close-modal',
        title: 'Done',
        body:
          'Pair delegations with Fill Form (self vs on-behalf) and Digital Document List badges so delegates always know how they may access each form.',
      },
    ],
  },
  {
    id: 'email-templates',
    page: 'email-templates',
    roles: ['*'],
    name: 'Email templates walkthrough',
    icon: '✉️',
    desc: 'Manage workflow email templates, HTML layouts, categories, and preview rendered messages.',
    steps: [
      {
        prepare: 'email-templates-close-modals',
        target: 'email-tpl-header',
        title: 'Email templates',
        body:
          'Central place to maintain Velocity email content—welcome messages, delegation notices, workflow alerts, and system mail. Templates can use shared layouts and placeholder tokens.',
      },
      {
        prepare: 'email-templates-close-modals',
        target: 'email-tpl-toolbar',
        title: 'Refresh & search',
        body:
          'Reload templates and layouts from the server after changes elsewhere. Search filters the active tab’s table locally.',
      },
      {
        prepare: 'email-templates-close-modals',
        target: 'email-tpl-tabs',
        title: 'Templates vs layouts',
        body:
          'Templates hold subject, body HTML, category, and optional layout. Layouts wrap content with branding and must include {{content}} where the template body is injected.',
      },
      {
        prepare: 'email-templates-tab-templates',
        target: 'email-tpl-templates-panel',
        skipIfMissingTarget: true,
        title: 'Templates grid',
        body:
          'Each row shows code, name, category, subject, linked layout, and active flag. Preview renders sample placeholder data; edit updates the record; delete removes it when unused.',
      },
      {
        prepare: 'email-templates-tab-templates',
        target: 'email-tpl-add-template',
        skipIfMissingTarget: true,
        title: 'Add template',
        body:
          'Create a new template with a unique code (for example USER_WELCOME). Assign a layout for consistent headers and footers across emails.',
      },
      {
        prepare: 'email-templates-open-template-modal',
        target: 'email-tpl-template-modal',
        skipIfMissingTarget: true,
        title: 'Template editor',
        body:
          'The walkthrough opens Add template so you can see the full form. Use Cancel when you are done exploring—no save is required.',
      },
      {
        target: 'email-tpl-template-meta',
        skipIfMissingTarget: true,
        title: 'Metadata & layout',
        body:
          'Template code, display name, category (USER, DELEGATION, WORKFLOW, SYSTEM), optional layout, sort order, and active toggle control how the backend picks this template.',
      },
      {
        target: 'email-tpl-template-body',
        skipIfMissingTarget: true,
        title: 'Body HTML',
        body:
          'Compose rich HTML with Summernote or switch to Code View for placeholders such as {{displayName}}. Placeholder help documents tokens for other admins.',
      },
      {
        prepare: 'email-templates-tab-layouts',
        target: 'email-tpl-layouts-panel',
        skipIfMissingTarget: true,
        title: 'Layouts tab',
        body:
          'Switch to Layouts to edit HTML wrappers (for example branded red welcome shell). Layout code and name identify each wrapper; only active layouts appear in template picklists.',
      },
      {
        prepare: 'email-templates-close-modals',
        title: 'Done',
        body:
          'Saved templates are used by background jobs and workflow notifications. Keep codes stable—services reference them by template code.',
      },
    ],
  },
  {
    id: 'chat',
    page: 'chat',
    roles: ['*'],
    name: 'Messages walkthrough',
    icon: '💬',
    desc: 'Start conversations, search chats, and send messages inside Velocity.',
    steps: [
      {
        target: 'chat-sidebar',
        title: 'Messenger sidebar',
        body:
          'Your conversations list lives here. Use New chat to message a colleague, search to filter threads, and enable browser notifications or message sounds if you want alerts.',
      },
      {
        target: 'chat-main',
        skipIfMissingTarget: true,
        title: 'Conversation',
        body:
          'Select a chat to read history and compose at the bottom. Delivery ticks show sent, delivered, and read status on your messages.',
      },
      {
        title: 'Done',
        body: 'Messages are scoped to your Velocity account—use chat for quick coordination alongside formal approvals.',
      },
    ],
  },
  {
    id: 'template-approval',
    page: 'template-approval',
    roles: ['*'],
    name: 'Template approval walkthrough',
    icon: '✅',
    desc: 'Review the document, edit step fields, add remarks, and approve, send back, hold, or request opinion.',
    steps: [
      {
        target: 'tpl-approval-header',
        title: 'Approval workspace',
        body:
          'You are on a specific digital application assigned to your approval step. The title shows the template name, form code, and current workflow step.',
      },
      {
        target: 'tpl-approval-header-actions',
        title: 'Tools & navigation',
        body:
          'Go back to Pending Approvals, open attachments when the form has files, download the PDF (with or without activity logs), or Save field changes before you approve.',
      },
      {
        target: 'tpl-approval-fields',
        skipIfMissingTarget: true,
        title: 'Fields for this step',
        body:
          'Only fields your step may fill or edit appear here. Updates sync to the document preview. If the step is read-only, this panel explains what you can see but not change.',
      },
      {
        target: 'tpl-approval-preview',
        skipIfMissingTarget: true,
        title: 'Document preview',
        body:
          'The live layout mirrors the printed PDF—including signatures and footer bands for your pipeline stage. Scroll the page to verify values before you act.',
      },
      {
        target: 'tpl-approval-workflow',
        skipIfMissingTarget: true,
        title: 'Remarks & decisions',
        body:
          'Enter remarks (required for Approve and Put on Hold). Use Approve to advance the workflow, Send Back or To Initiator to return work, or Put on Hold to pause the case. After approve, emails and in-app notifications are queued automatically.',
      },
      {
        target: 'tpl-approval-opinion',
        skipIfMissingTarget: true,
        title: 'Request opinion',
        body:
          'When you need input from a colleague without leaving the pipeline, pick a user and Request Opinion. They complete an opinion step and the application returns to you.',
      },
      {
        title: 'Done',
        body:
          'When you finish, you return to Pending Approvals. Track progress under My Digital Applications (initiator) or Approved Applications as items move through the pipeline.',
      },
    ],
  },
  {
    id: 'template-pending-approvals',
    page: 'template-pending-approvals',
    roles: ['*'],
    name: 'Pending approvals walkthrough',
    icon: '⏳',
    desc: 'Items waiting for your approval action.',
    steps: [
      {
        prepare: 'template-pending-close-csv',
        target: 'tpl-pending-header',
        title: 'Pending approvals',
        body:
          'Digital applications assigned to you for approval. The badge shows how many are waiting.',
      },
      {
        target: 'tpl-pending-toolbar',
        title: 'Search & export',
        body: 'Filter the current result set with search. CSV export helps you work offline or share with managers.',
      },
      {
        prepare: 'template-pending-close-csv',
        target: 'tpl-pending-grid',
        skipIfMissingTarget: true,
        title: 'Approval queue',
        body:
          'Check level, status, and submission date. Open launches the approval screen where you approve, reject, or send back.',
      },
      {
        prepare: 'template-pending-close-csv',
        target: 'tpl-pending-pagination',
        skipIfMissingTarget: true,
        title: 'Pagination',
        body: 'Move through pages when your queue is large.',
      },
      {
        prepare: 'template-pending-open-csv',
        target: 'tpl-pending-csv-modal',
        skipIfMissingTarget: true,
        title: 'CSV export',
        body: 'Pick columns and download pending items for reporting.',
      },
      {
        prepare: 'template-pending-close-csv',
        title: 'Done',
        body: 'After you act, items move to Approved Applications or back to the initiator.',
      },
    ],
  },
  {
    id: 'template-builder',
    page: 'template-builder',
    roles: ['*'],
    name: 'Document builder walkthrough',
    icon: '🛠️',
    desc: 'Design templates, place fields, and configure approval pipelines.',
    steps: [
      {
        prepare: 'template-builder-close-modals',
        target: 'builder-header',
        title: 'Document builder',
        body:
          'Name the template and set its code convention. This designer creates digital documents with embedded fields and workflows.',
      },
      {
        prepare: 'template-builder-close-modals',
        target: 'builder-header-actions',
        title: 'Save & pages',
        body:
          'Switch portrait/landscape, add pages, Save as Draft for admins, or Save to publish. Drafts appear in Digital Document List.',
      },
      {
        target: 'builder-left-panel',
        title: 'Background & fields',
        body:
          'Upload a hardcopy image or PDF, then add fields with +. Select a field to position it on the document.',
      },
      {
        target: 'builder-stage',
        title: 'Document canvas',
        body:
          'Edit rich text, drag fields onto the layout, and align with your scanned form. Multi-page backgrounds are supported.',
      },
      {
        target: 'builder-right-panel',
        title: 'Properties & pipeline',
        body:
          'Tune field properties when selected. Pipeline Steps define initiator and approver levels—click a step to edit rights or routing.',
      },
      {
        prepare: 'template-builder-open-field-modal',
        target: 'builder-field-modal',
        skipIfMissingTarget: true,
        title: 'Assign field dialog',
        body:
          'Set label, type (text, table, signature, etc.), and options before placing the field on the canvas.',
      },
      {
        prepare: 'template-builder-close-modals',
        title: 'Done',
        body: 'Publish from Save, then assign visibility on Digital Document List so users can fill the form.',
      },
    ],
  },
];
