import { Routes } from '@angular/router';

import { AbcComponent } from './pages/abc/abc.component';

// dashboard
import { IndexComponent } from './index';
import { AnalyticsComponent } from './analytics';
import { FinanceComponent } from './finance';
import { CryptoComponent } from './crypto';

// widgets
import { WidgetsComponent } from './widgets';

// tables
import { TablesComponent } from './tables';

// font-icons
import { FontIconsComponent } from './font-icons';

// charts
import { ChartsComponent } from './charts';

// dragndrop
import { DragndropComponent } from './dragndrop';

// layouts
import { AppLayout } from './layouts/app-layout';
import { AuthLayout } from './layouts/auth-layout';

// pages
import { KnowledgeBaseComponent } from './pages/knowledge-base';
import { FaqComponent } from './pages/faq';
import { CountryComponent } from './components/country/country.component';
import { CityComponent } from './components/city/city.component';
import { CustomerComponent } from './components/customer/customer.component';
import { TaxCategoryComponent } from './components/tax-category/tax-category.component';
import { ProductCategoryComponent } from './components/product-category/product-category.component';
import { ProductComponent } from './components/product/product.component';
import { DepartmentComponent } from './components/department/department.component';
import { SignatureComponent } from './components/signature/signature.component';
import { CapfComponent } from './components/capf/capf.component';
import { ApplicationComponent } from './components/application/application.component';
import { ApplicationsViewComponent } from './components/applications-view/applications-view.component';
import { ApplicationDetailsComponent } from './components/application-details/application-details.component';
import { FormBuilderComponent } from './components/form-builder/form-builder.component';
import { EmailApprovalComponent } from './components/email-approval/email-approval.component';
import { OrderListComponent } from "./apps/order/list";
import { SaleInvoiceListComponent } from "./apps/sale-invoice/list";
import { SaleInvoiceViewListComponent } from "./apps/sale-invoice-view/list";
import { MarketingListComponent } from "./apps/marketing/list";
import { ProcurementListComponent } from "./apps/procurement/list";
import { TaxListComponent } from "./apps/tax/list";
import { FinanceListComponent } from "./apps/finance/list";
import { AuditListComponent } from "./apps/audit/list";
import { SaleInvoiceStatusListComponent } from "./apps/sale-invoice-status/list";
import { SchedulerComponent } from "./apps/scheduler/list";
import { UserListComponent } from './components/user/user-list/user-list.component';
import { PasswordPolicyComponent } from './components/user/password-policy/password-policy.component';
import { ChangePasswordComponent } from './components/user/change-password/change-password.component';
import { canActivate } from './guards/auth/auth.guard';
import { PermissionComponent } from "./components/permission/permission-component";
import { ChannelComponent } from './components/channel/channel.component';
import { ServiceOrderComponent } from './components/service-order/service-order.component';
import { VendorListComponent } from "./apps/vendor-view/list";
import { PaymentComponent } from "./apps/payment/list";
import { DeparmentComponent } from "./apps/department/list";
import { PdfEditorComponent } from "./pdf-editor/pdf-editor.component";
import { SesLogComponent } from "./apps/ses-log/list";
import { IntegrationLogComponent } from "./apps/inetgration-log/list";
import { TransactionsDetailsComponent } from "./transactions-details/transactions-details.component";
import { ReportViewComponent } from "./report-view/report-view.component";
import { OrderDetailComponent } from "./order-detail/edit";
import { BudgetApprovalComponent } from './components/budget-approval/budget-approval.component';
import { BudgetApprovalViewComponent } from './components/budget-approval-view/budget-approval-view.component';


export const routes: Routes = [
    {
        path: '',
        component: AppLayout,
        children: [
            // dashboard
            {
                path: '',
                // component: IndexComponent,
                title: 'Dashboard',
                redirectTo: '/Dashboard', pathMatch: 'full'
            },
            // { path: 'analytics', component: AnalyticsComponent, title: 'Analytics Admin | VRISTO - Multipurpose Tailwind Dashboard Template' },
            /*  {
                  path: 'dashboard',
                  component: FinanceComponent,
                  title: 'Dashboard',
                  canActivate: [canActivate]
              },*/
            // { path: 'crypto', component: CryptoComponent, title: 'Crypto Admin | VRISTO - Multipurpose Tailwind Dashboard Template' },
            {
                path: 'country',
                component: CountryComponent,
                title: 'Country',
                canActivate: [canActivate]
            },
            {
                path: 'city',
                component: CityComponent,
                title: 'City',
                canActivate: [canActivate]
            },
            {
                path: 'media-house',
                component: CustomerComponent,
                title: 'Media House',
                canActivate: [canActivate]
            },
            { path: 'tax-category', canActivate: [canActivate], component: TaxCategoryComponent, title: 'Tax Category' },
            { path: 'product-category', canActivate: [canActivate], component: ProductCategoryComponent, title: 'Product Category' },
            { path: 'product', canActivate: [canActivate], component: ProductComponent, title: 'Product' },
            { path: 'department', canActivate: [canActivate], component: DepartmentComponent, title: 'Department' },
            { path: 'signature', canActivate: [canActivate], component: SignatureComponent, title: 'Signature' },
            { path: 'CAPF', canActivate: [canActivate], component: CapfComponent, title: 'CAPF' },
            { path: 'application', canActivate: [canActivate], component: ApplicationComponent, title: 'Application' },
            { path: 'applicationsview', canActivate: [canActivate], component: ApplicationsViewComponent, title: 'Applications View' },
            { path: 'application-details/:id', canActivate: [canActivate], component: ApplicationDetailsComponent, title: 'Application Details' },
            { path: 'formbuilder', canActivate: [canActivate], component: FormBuilderComponent, title: 'Form Builder' },
            { path: 'order', canActivate: [canActivate], component: OrderListComponent, title: 'Order' },
            { path: 'sale-invoice', canActivate: [canActivate], component: SaleInvoiceListComponent, title: 'Sale Invoice' },
            { path: 'sale-invoice-view', canActivate: [canActivate], component: SaleInvoiceViewListComponent, title: 'Sale Invoice View' },
            { path: 'marketing', canActivate: [canActivate], component: MarketingListComponent, title: 'Marketing' },
            { path: 'procurement', canActivate: [canActivate], component: ProcurementListComponent, title: 'Procurement' },
            { path: 'tax-approval', canActivate: [canActivate], component: TaxListComponent, title: 'Tax Approval' },
            { path: 'finance', canActivate: [canActivate], component: FinanceListComponent, title: 'Finance' },
            { path: 'audit', canActivate: [canActivate], component: AuditListComponent, title: 'Audit' },
            { path: 'sale-status', canActivate: [canActivate], component: SaleInvoiceStatusListComponent, title: 'Sale Status' },
            { path: 'log', canActivate: [canActivate], component: SchedulerComponent, title: 'Log' },
            { path: 'users', canActivate: [canActivate], component: UserListComponent, title: 'Users' },
            { path: 'password-policy', canActivate: [canActivate], component: PasswordPolicyComponent, title: 'Password Policy' },
            {
                path: 'change-password',
                component: ChangePasswordComponent,
                title: 'Change Password',
                canActivate: [canActivate]
            },
            { path: 'Dashboard', canActivate: [canActivate], component: FinanceComponent, title: 'Dash Board' },
            { path: 'role', canActivate: [canActivate], component: PermissionComponent, title: 'Permission' },
            { path: 'channel', canActivate: [canActivate], component: ChannelComponent, title: 'Channel' },
            { path: 'service-order', canActivate: [canActivate], component: ServiceOrderComponent, title: 'Service Order' },
            { path: 'vendor-view', canActivate: [canActivate], component: VendorListComponent, title: 'Vendor' },
            { path: 'payment', canActivate: [canActivate], component: PaymentComponent, title: 'Payment' },
            { path: 'OrderDepartment', canActivate: [canActivate], component: DeparmentComponent, title: 'Order By Department' },
            { path: 'pdf-editor/:documentId', component: PdfEditorComponent }, // PDF editor route
            { path: 'SES', canActivate: [canActivate], component: SesLogComponent, title: 'SES Integration Logs' },
            { path: 'auditLog', canActivate: [canActivate], component: IntegrationLogComponent, title: 'Integration Logs' },
            { path: 'transactions-details', component: TransactionsDetailsComponent },
            { path: 'report', component: ReportViewComponent },
            { path: 'order-details/edit/:id', component: OrderDetailComponent },
            { path: 'budget-approval', component: BudgetApprovalComponent, title: 'Budget Approval' },
            { path: 'budgetapprovalview', component: BudgetApprovalViewComponent, title: 'Budget Approval View' },



            //apps- comments
            { path: '', loadChildren: () => import('./apps/apps.module').then((d) => d.AppsModule) },


            // widgets
            { path: 'widgets', component: WidgetsComponent, title: 'Widgets | VRISTO - Multipurpose Tailwind Dashboard Template' },

            // components
            { path: '', loadChildren: () => import('./components/components.module').then((d) => d.ComponentsModule) },

            // elements
            { path: '', loadChildren: () => import('./elements/elements.module').then((d) => d.ElementsModule) },

            // forms
            { path: '', loadChildren: () => import('./forms/form.module').then((d) => d.FormModule) },

            // users
            { path: '', loadChildren: () => import('./users/user.module').then((d) => d.UsersModule) },

            // tables
            { path: 'tables', component: TablesComponent, title: 'Tables | VRISTO - Multipurpose Tailwind Dashboard Template' },
            { path: '', loadChildren: () => import('./datatables/datatables.module').then((d) => d.DatatablesModule) },

            // font-icons
            // { path: 'font-icons', component: FontIconsComponent, title: 'Font Icons | VRISTO - Multipurpose Tailwind Dashboard Template' },

            // charts
            // { path: 'charts', component: ChartsComponent, title: 'Charts | VRISTO - Multipurpose Tailwind Dashboard Template' },

            // dragndrop
            // { path: 'dragndrop', component: DragndropComponent, title: 'Dragndrop | VRISTO - Multipurpose Tailwind Dashboard Template' },

            // pages
            // { path: 'pages/knowledge-base', component: KnowledgeBaseComponent, title: 'Knowledge Base | VRISTO - Multipurpose Tailwind Dashboard Template' },
            // { path: 'pages/faq', component: FaqComponent, title: 'FAQ | VRISTO - Multipurpose Tailwind Dashboard Template' },
        ],
    },

    {
        path: '',
        component: AuthLayout,
        children: [
            // pages
            { path: '', loadChildren: () => import('./pages/pages.module').then((d) => d.PagesModule) },

            // auth
            { path: '', loadChildren: () => import('./auth/auth.module').then((d) => d.AuthModule) },
        ],
    },

    // Email approval routes - no authentication required, no layout wrapper
    { path: 'approveApplicationFromEmail', component: EmailApprovalComponent, title: 'Approve Application' },
    { path: 'rejectApplicationFromEmail', component: EmailApprovalComponent, title: 'Reject Application' },
    { path: 'abc', component: AbcComponent, title: 'Capital Assets Purchase Form' },

];
