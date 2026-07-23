# Prompt: Build React Skeleton Matching Existing VRISTO Admin App

You are a senior React frontend engineer and product-minded UI architect. Build a new React application skeleton that replicates the look, feel, navigation model, and permission behavior of an existing Angular enterprise admin app.

The source application is an enterprise workflow/admin portal built from the **VRISTO admin dashboard template** by SBThemes. The new app should be React-based, but visually and structurally it should feel like the same product.

## Objective

Create a production-ready React skeleton for a business workflow application with:

- VRISTO/Tailwind admin dashboard look and feel.
- Left sidebar navigation with dynamic menus/submenus.
- Sticky top header.
- Dashboard landing page.
- Master Data screens.
- Velocity/workflow screens.
- User Management and RBAC screens.
- Approval/business workflow placeholders.
- Reusable page, table, form, modal, and permission components.
- Mock API/data layer that can later be replaced with real backend endpoints.

Do not build a marketing landing page. The first screen after login must be the admin application.

## Technical Stack

Use:

- React with TypeScript.
- Vite.
- React Router.
- Tailwind CSS.
- Tailwind `darkMode: "class"`.
- Nunito font.
- Lucide React icons for sidebar/header/action buttons.
- A lightweight state store such as Zustand or React Context for auth, theme, layout, menu, and permissions.
- React Hook Form for forms.
- A reusable data table component with search, sort, pagination, row actions, loading, and empty states.

Avoid:

- Material UI.
- Bootstrap.
- Ant Design.
- PrimeReact.
- shadcn as the dominant visual style.

The design should be Tailwind/VRISTO-inspired, not a generic component library skin.

## Visual Style To Replicate

Use the VRISTO admin theme style:

- App background: light `#fafafa`, dark `#060818`.
- Panels/cards: white in light mode, `#0e1726` in dark mode.
- Primary color: `#4361ee`.
- Secondary color: `#805dca`.
- Success: `#00ab55`.
- Danger: `#e7515a`.
- Warning: `#e2a03f`.
- Info: `#2196f3`.
- Text dark: `#0e1726`.
- Muted sidebar text: `#506690`.
- Form border: `#e0e6ed`.
- Dark form border: `#17263c`.
- Border radius: mostly `rounded-md`.
- Typography: Nunito, compact enterprise dashboard sizing.

Define Tailwind theme colors matching:

```ts
primary: "#4361ee"
secondary: "#805dca"
success: "#00ab55"
danger: "#e7515a"
warning: "#e2a03f"
info: "#2196f3"
dark: "#3b3f5c"
black: "#0e1726"
```

Create global component classes similar to the original app:

```css
.panel
.btn
.btn-primary
.btn-outline-primary
.btn-danger
.btn-success
.badge
.form-input
.form-select
.form-checkbox
.sidebar
.nav-item
.sub-menu
```

The UI should feel like a dense enterprise admin tool:

- Sidebar width around `260px`.
- Content padding around `24px`.
- Sticky header.
- Compact tables and forms.
- Panels with subtle shadow.
- Clear action buttons.
- Light/dark mode support.
- No large hero sections.
- No decorative gradient blobs.

## Application Layout

Build these layouts:

### Auth Layout

Routes:

- `/auth/signin`

Signin screen should use a branded auth layout with username/password form, validation, error display, and mock login.

On successful mock login, store:

```ts
token
user
role
permissions
```

Then redirect to `/Dashboard`.

### App Layout

All protected routes use the main app layout:

- Fixed/desktop sidebar.
- Mobile sidebar overlay.
- Header with sidebar toggle, user menu, theme toggle, and logout.
- Main content area.
- Footer.
- Scroll-to-top button.

Support layout config similar to the original:

```ts
theme: "light" | "dark" | "system"
menu: "vertical" | "collapsible-vertical" | "horizontal"
layout: "full" | "boxed-layout"
rtlClass: "ltr" | "rtl"
navbar: "navbar-sticky" | "navbar-floating" | "navbar-static"
semidark: boolean
```

Implement at least vertical sidebar now. Keep config extensible for the other modes.

## User Roles

Create mock roles:

- Admin
- Super Admin
- Vendor
- Marketing
- Procurement
- Finance
- Audit
- CEO
- HOD
- Department User

Admin and Super Admin can access everything.

Other users only see enabled submenus they have permission for.

Special rule:

- `Pending Approvals` should be visible to HOD and Procurement users even when it needs workflow-specific access.

## RBAC Model

Implement menu/submenu permission rows similar to the existing app.

Each permission row should include:

```ts
type SubMenuPermission = {
  id: number
  roleId: number
  userId?: number | null
  menuId: number
  subMenuId: number
  subMenuName: string
  isActive: boolean
  status: boolean
  isDeleted: boolean
  isEnabled: boolean
  canView: boolean
  canAdd: boolean
  canUpdate: boolean
  canDelete: boolean
  canApprove: boolean
  canAll: boolean
  canNewCreate: boolean
  canNewView: boolean
  canNewUpdate: boolean
}
```

Create a `PermissionService` or permission hook with:

```ts
loadPermissionRoles(roleId, userId)
canAccessSubMenu(subMenuName)
canView(subMenuName)
canAdd(subMenuName)
canUpdate(subMenuName)
canDelete(subMenuName)
canApprove(subMenuName)
canNewCreate(subMenuName)
canNewView(subMenuName)
canNewUpdate(subMenuName)
canViewForModule(moduleName)
canAddForModule(moduleName)
canUpdateForModule(moduleName)
canDeleteForModule(moduleName)
clearPermissions()
```

Permission logic:

- A submenu is visible only if there is a non-deleted, enabled/active/status permission row for the user role.
- `canNewView` falls back to `canView`.
- `canNewCreate` falls back to `canAdd`.
- `canNewUpdate` falls back to `canUpdate`.
- Admin users bypass all permission checks.
- Protected routes must redirect unauthenticated users to `/auth/signin`.
- If a logged-in user has no accessible menus, redirect to signin with an error message:
  `You do not have access to the application. Please contact the administrator for further assistance.`

## Menu Structure

Build the sidebar from a mock menu API, not hardcoded JSX only. Use a menu/submenu structure that can later come from backend data.

Create these main menus and submenus:

### Dashboard

- Dashboard
  - Route: `/Dashboard`

### Master Data

- Country
  - Route: `/country`
- City
  - Route: `/city`
- Media House
  - Route: `/media-house`
- Department
  - Route: `/department`
- Tax Category
  - Route: `/tax-category`
- Product Category
  - Route: `/product-category`
- Product
  - Route: `/product`
- Signature
  - Route: `/signature`
- Channel
  - Route: `/channel`

### Velocity

- CAPF
  - Route: `/CAPF`
- Application
  - Route: `/application`
- Applications View
  - Route: `/applicationsview`
- Department Applications
  - Route: `/department-application`
- Pending Approvals
  - Route: `/pending-approvals`
- Approved Applications
  - Route: `/approved-applications`
- Form Builder
  - Route: `/formbuilder`
- Budget Approval
  - Route: `/budget-approval`
- Budget Approval View
  - Route: `/budgetapprovalview`
- Document Builder
  - Route: `/custom-document-builder`
- Template Builder
  - Route: `/template-builder`
- Template Fill
  - Route: `/template-fill`

### Order / Procurement / Finance Flow

- Order
  - Route: `/order`
- Order By Department
  - Route: `/OrderDepartment`
- Service Order
  - Route: `/service-order`
- Sale Invoice
  - Route: `/sale-invoice`
- Sale Invoice View
  - Route: `/sale-invoice-view`
- Sale Status
  - Route: `/sale-status`
- Marketing
  - Route: `/marketing`
- Procurement
  - Route: `/procurement`
- Tax Approval
  - Route: `/tax-approval`
- Finance
  - Route: `/finance`
- Audit
  - Route: `/audit`
- Vendor
  - Route: `/vendor-view`
- Payment
  - Route: `/payment`

### User Management

- Users
  - Route: `/users`
- Roles
  - Route: `/roles`
- Permission
  - Route: `/role`
- Password Policy
  - Route: `/password-policy`
- Change Password
  - Route: `/change-password`
- Menu List
  - Route: `/menu-list`
- Submenu List
  - Route: `/submenu-list`
- Activity Logs
  - Route: `/activitylogs`

### Logs / Reports

- Scheduler Log
  - Route: `/log`
- SES Integration Logs
  - Route: `/SES`
- Integration Logs
  - Route: `/auditLog`
- Transactions Details
  - Route: `/transactions-details`
- Report
  - Route: `/report`

Also create detail/action routes:

- `/application-details/:id`
- `/assign-asset-code/:id`
- `/pr-code/:id`
- `/po-code/:id`
- `/pdf-editor/:documentId`
- `/order-details/edit/:id`
- `/apps/order/preview/:serDealId`
- `/apps/sale-invoice/edit/:serDealId`
- `/apps/sale-invoice-view/edit/:serDealId`
- `/apps/sale-invoice-status/edit/:serDealId`

## Screen Skeleton Requirements

Every list/CRUD screen should use a shared `CrudPage` or equivalent pattern:

- Page title.
- Breadcrumb.
- Search box.
- Add button if user has create permission.
- Data table.
- Row actions: view, edit, delete, approve where relevant.
- Create/edit modal or side panel.
- Toast notifications.
- Loading state.
- Empty state.
- Permission-aware disabled/hidden actions.

Every master data form should include:

- Code.
- Name.
- Description where useful.
- Status toggle.
- Save/cancel actions.
- Created/modified metadata in mock data.

## Specific Screens

### Dashboard

Create a dashboard with:

- KPI cards for pending approvals, applications, approved documents, vendors, payments.
- Recent workflow table.
- Approval status chart placeholder.
- Department summary cards.
- Quick links based on permission.

### Master Data Screens

Create reusable CRUD skeletons for:

- Country
- City
- Media House
- Department
- Tax Category
- Product Category
- Product
- Signature
- Channel

Each should have mock data and create/update/delete behavior in local state.

### User Management

#### Users

Fields:

- Employee ID
- Name
- Login Name
- Email
- Contact Number
- Designation
- Department
- Role
- Status

#### Roles

Fields:

- Role Name
- Role Code
- Status

#### Permission

Build a permission matrix screen:

- Select Role.
- Optional Select User.
- Search menu/submenu permissions.
- Group permissions by parent menu.
- Rows are submenus.
- Columns:
  - Enabled
  - View
  - Create
  - Update
  - Delete
  - Approve
- Include toggles/checkboxes.
- Include “toggle all” controls per permission column.
- Save permissions to mock store.
- Broadcast menu refresh so sidebar updates after saving.

#### Menu List / Submenu List

Create admin screens to manage menu metadata:

- Menu name.
- Icon key.
- Order.
- Status.
- Submenu name.
- Submenu URL.
- Parent menu.
- Submenu order.
- Status.

### Velocity / Workflow Screens

Build skeleton workflows for:

- CAPF.
- Application.
- Applications View.
- Department Applications.
- Pending Approvals.
- Approved Applications.
- Form Builder.
- Budget Approval.
- Budget Approval View.
- Document Builder.
- Template Builder.
- Template Fill.

Use mock application data with statuses:

- Draft
- Submitted
- Pending HOD
- Pending Procurement
- Pending Finance
- Pending Audit
- Pending CEO
- Approved
- Rejected
- Sent Back

Application fields:

- Application Code.
- Department.
- Requested By.
- Vendor.
- Amount.
- Asset Code.
- PR Code.
- PO Code.
- Current Stage.
- Status.
- Created Date.
- Last Updated Date.

Business actions:

- Submit.
- Approve.
- Reject.
- Send Back.
- Assign Asset Code.
- Assign PR Code.
- Assign PO Code.
- View Details.
- Download/Preview PDF placeholder.

### Pending Approvals

Show only items assigned to the current user/role/stage.

HOD and Procurement users should see relevant pending approvals even if sidebar rules are stricter.

### Application Details

Create a detailed page with:

- Header summary.
- Status badge.
- Approval timeline.
- Application metadata.
- Document preview placeholder.
- Action buttons controlled by RBAC and current workflow stage.

### Order / Finance Flow

Create list skeletons for:

- Order.
- Order By Department.
- Service Order.
- Sale Invoice.
- Sale Invoice View.
- Sale Status.
- Marketing.
- Procurement.
- Tax Approval.
- Finance.
- Audit.
- Vendor.
- Payment.

Use mock records with:

- Document number.
- Vendor/customer.
- Department.
- Amount.
- Status.
- Assigned department/stage.
- Created date.
- Actions.

### Logs / Reports

Create skeletons for:

- Scheduler Log.
- SES Integration Logs.
- Integration Logs.
- Activity Logs.
- Transactions Details.
- Report.

Use compact tables with filters for date range, status, module, and search.

## Mock Backend Layer

Create a mock API layer under `src/api` or `src/mocks`.

Include:

- `authApi`
- `menuApi`
- `permissionApi`
- `masterDataApi`
- `workflowApi`
- `userApi`
- `logsApi`

Use async functions with small artificial delays.

Keep data in memory initially. Structure it so it can be replaced with REST endpoints later.

## Suggested File Structure

Use a clean structure like:

```txt
src/
  app/
    App.tsx
    router.tsx
    providers.tsx
  assets/
    images/
  components/
    ui/
      Button.tsx
      Panel.tsx
      DataTable.tsx
      Modal.tsx
      Badge.tsx
      FormField.tsx
      PageHeader.tsx
      ConfirmDialog.tsx
    layout/
      AppLayout.tsx
      AuthLayout.tsx
      Sidebar.tsx
      Header.tsx
      Footer.tsx
  config/
    themeConfig.ts
    menuConfig.ts
  features/
    auth/
    dashboard/
    master-data/
    user-management/
    velocity/
    order-flow/
    logs/
    reports/
  hooks/
    useAuth.ts
    usePermissions.ts
    useTheme.ts
    useMenu.ts
  mocks/
    authApi.ts
    menuApi.ts
    permissionApi.ts
    workflowApi.ts
    masterDataApi.ts
    userApi.ts
    logsApi.ts
    seed.ts
  stores/
    authStore.ts
    layoutStore.ts
    permissionStore.ts
    menuStore.ts
  styles/
    tailwind.css
    app.css
  types/
    auth.ts
    menu.ts
    permission.ts
    workflow.ts
```

## Acceptance Criteria

The finished skeleton should:

- Start with `npm install` and `npm run dev`.
- Render a login screen at `/auth/signin`.
- Allow mock login as Admin and other roles.
- Redirect authenticated users to `/Dashboard`.
- Show a VRISTO-like sidebar/header/admin layout.
- Hide/show sidebar menus based on RBAC.
- Include all routes listed above.
- Include working mock CRUD screens for master data.
- Include permission matrix management that updates sidebar visibility.
- Include workflow list/detail placeholders with approve/reject/send-back actions.
- Include dark mode.
- Use reusable components rather than duplicating page markup everywhere.
- Keep business logic separated from presentational components.
- Be easy to connect to real APIs later.

## Important Implementation Notes

- Make the UI look like VRISTO: Tailwind utility classes, compact panels, blue primary buttons, subtle shadows, dense enterprise layout.
- Do not use Material UI, Bootstrap, Ant Design, or PrimeReact.
- Do not create decorative landing pages.
- Do not hardcode permissions only in components; centralize permission checks.
- Sidebar must be generated from menu data and filtered by permission rows.
- Protected routes must check authentication and permission.
- Use readable placeholder business logic, but keep the architecture realistic.
- Prefer complete skeleton screens over empty route placeholders.

