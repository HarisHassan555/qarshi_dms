import { NgModule } from '@angular/core';
import { BrowserModule, Title } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, HttpClient, HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';

//Routes
import { routes } from './app.route';

import { AppComponent } from './app.component';

// service
import { AppService } from './service/app.service';

// store
import { StoreModule } from '@ngrx/store';
import { indexReducer } from './store/index.reducer';

// i18n
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

// perfect-scrollbar
import { NgScrollbarModule } from 'ngx-scrollbar';

// apexchart
// @ts-ignore
import { NgApexchartsModule } from 'ng-apexcharts';

// highlightjs
import { HighlightModule, HIGHLIGHT_OPTIONS } from 'ngx-highlightjs';

// tippy
// @ts-ignore
import { NgxTippyModule } from 'ngx-tippy-wrapper';

// headlessui
import { MenuModule } from 'headlessui-angular';

// modal
import { ModalModule } from 'angular-custom-modal';

// sortable
import { SortablejsModule } from '@dustfoundation/ngx-sortablejs';

// quill editor
import { QuillModule } from 'ngx-quill';

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

// pages
import { KnowledgeBaseComponent } from './pages/knowledge-base';
import { FaqComponent } from './pages/faq';

// Layouts
import { AppLayout } from './layouts/app-layout';
import { AuthLayout } from './layouts/auth-layout';

import { HeaderComponent } from './layouts/header';
import { FooterComponent } from './layouts/footer';
import { SidebarComponent } from './layouts/sidebar';
import { ThemeCustomizerComponent } from './layouts/theme-customizer';
import { IconModule } from './shared/icon/icon.module';
import { DataTableModule } from "@bhplugin/ng-datatable";
import { AngJson2excelBtnModule } from "ang-json2excel-btn";
import { NotificationService } from "./NotificationService";
import { CountryComponent } from './components/country/country.component';
import { CityComponent } from './components/city/city.component';
import { AuthInterceptor } from './interceptors/auth/auth.interceptor';
import { CustomerComponent } from './components/customer/customer.component';
import { TaxCategoryComponent } from './components/tax-category/tax-category.component';
import { ProductCategoryComponent } from './components/product-category/product-category.component';
import { ProductComponent } from './components/product/product.component';
import { DepartmentComponent } from './components/department/department.component';
import { SignatureComponent } from './components/signature/signature.component';
import { CapfComponent } from './components/capf/capf.component';
import { ApplicationComponent } from './components/application/application.component';
import { ApplicationsViewComponent } from './components/applications-view/applications-view.component';
import { PendingApprovalsComponent } from './components/pending-approvals/pending-approvals.component';
import { ApplicationDetailsComponent } from './components/application-details/application-details.component';
import { AssignAssetCodeComponent } from './components/assign-asset-code/assign-asset-code.component';
import { PrCodeComponent } from './components/pr-code/pr-code.component';
import { FormBuilderComponent } from './components/form-builder/form-builder.component';
import { EmailApprovalComponent } from './components/email-approval/email-approval.component';
import { OrderListComponent } from "./apps/order/list";
import { OrderPreviewComponent } from "./apps/order/preview";
import { SaleInvoiceListComponent } from "./apps/sale-invoice/list";
import { SaleInvoiceEditComponent } from "./apps/sale-invoice/edit";
import { SaleInvoiceViewListComponent } from "./apps/sale-invoice-view/list";
import { SaleInvoiceViewEditComponent } from "./apps/sale-invoice-view/edit";
import { MarketingListComponent } from "./apps/marketing/list";
import { ProcurementListComponent } from "./apps/procurement/list";
import { TaxListComponent } from "./apps/tax/list";
import { FinanceListComponent } from "./apps/finance/list";
import { AuditListComponent } from "./apps/audit/list";
import { SaleInvoiceStatusListComponent } from "./apps/sale-invoice-status/list";
import { SaleInvoiceViewStatusEditComponent } from "./apps/sale-invoice-status/edit";
import { SchedulerComponent } from "./apps/scheduler/list";
import { UserListComponent } from './components/user/user-list/user-list.component';
import { PasswordPolicyComponent } from './components/user/password-policy/password-policy.component';
import { ChangePasswordComponent } from './components/user/change-password/change-password.component';
import { InvoiceAddComponent } from "./apps/order/add";
import { InvoiceEditComponent } from "./apps/order/edit";
import { PermissionComponent } from "./components/permission/permission-component";
import { ChannelComponent } from './components/channel/channel.component';
import { ServiceOrderComponent } from './components/service-order/service-order.component';
import { Ng2FlatpickrModule } from 'ng2-flatpickr';
import { VendorListComponent } from "./apps/vendor-view/list";
import { PaymentComponent } from "./apps/payment/list";

import { NgSelectModule } from '@ng-select/ng-select';
import { DeparmentComponent } from "./apps/department/list";
import { PdfEditorComponent } from './pdf-editor/pdf-editor.component';
import { IntegrationLogComponent } from "./apps/inetgration-log/list";
import { SesLogComponent } from "./apps/ses-log/list";
import { TransactionsDetailsComponent } from './transactions-details/transactions-details.component';
import { ReportViewComponent } from './report-view/report-view.component';
import { ApprovedApplicationsComponent } from './components/approved-applications/approved-applications.component';
import { OrderDetailComponent } from './order-detail/edit';
import { BudgetApprovalComponent } from './components/budget-approval/budget-approval.component';
import { BudgetApprovalViewComponent } from './components/budget-approval-view/budget-approval-view.component';
import { XyzComponent } from './pages/xyz/xyz.component';
import { AbcComponent } from './pages/abc/abc.component';
import { FormDocumentHeaderComponent } from './components/form-document-header/form-document-header.component';
import { CustomDocumentBuilderComponent } from './components/custom-document-builder/custom-document-builder.component';


@NgModule({
    imports: [
        RouterModule.forRoot(routes, { scrollPositionRestoration: 'enabled' }),
        BrowserModule,
        BrowserModule,
        BrowserAnimationsModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        HttpClientModule,
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: httpTranslateLoader,
                deps: [HttpClient],
            },
        }),
        MenuModule,
        StoreModule.forRoot({ index: indexReducer }),
        NgxTippyModule,
        NgApexchartsModule,
        NgScrollbarModule.withConfig({
            visibility: 'hover',
            appearance: 'standard',
        }),
        HighlightModule,
        SortablejsModule,
        ModalModule,
        QuillModule.forRoot(),
        IconModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        DataTableModule,
        NgApexchartsModule,
        NgxTippyModule,
        MenuModule,
        AngJson2excelBtnModule,
        IconModule,
        Ng2FlatpickrModule,
        NgSelectModule,
        AbcComponent
    ],
    declarations: [
        AppComponent,
        HeaderComponent,
        FooterComponent,
        SidebarComponent,
        ThemeCustomizerComponent,
        TablesComponent,
        FontIconsComponent,
        ChartsComponent,
        IndexComponent,
        AnalyticsComponent,
        FinanceComponent,
        CryptoComponent,
        WidgetsComponent,
        DragndropComponent,
        AppLayout,
        AuthLayout,
        KnowledgeBaseComponent,
        FaqComponent,
        CountryComponent,
        CityComponent,
        CustomerComponent,
        TaxCategoryComponent,
        ProductCategoryComponent,
        ProductComponent,
        DepartmentComponent,
        SignatureComponent,
        CapfComponent,
        ApplicationComponent,
        ApplicationsViewComponent,
        PendingApprovalsComponent,
        ApplicationDetailsComponent,
        AssignAssetCodeComponent,
        PrCodeComponent,
        FormBuilderComponent,
        EmailApprovalComponent,
        OrderListComponent,
        OrderPreviewComponent,
        SaleInvoiceListComponent,
        SaleInvoiceEditComponent,
        SaleInvoiceViewListComponent,
        SaleInvoiceViewEditComponent,
        MarketingListComponent,
        ProcurementListComponent,
        InvoiceAddComponent,
        InvoiceEditComponent,
        TaxListComponent,
        FinanceListComponent,
        AuditListComponent,
        SaleInvoiceStatusListComponent,
        SaleInvoiceViewStatusEditComponent,
        SchedulerComponent,
        ProductComponent,
        UserListComponent,
        PasswordPolicyComponent,
        ChangePasswordComponent,
        PermissionComponent,
        ChannelComponent,
        ServiceOrderComponent,
        VendorListComponent,
        PaymentComponent,
        DeparmentComponent,
        PdfEditorComponent,
        IntegrationLogComponent,
        SesLogComponent,
        TransactionsDetailsComponent,
        ReportViewComponent,
        OrderDetailComponent,
        SidebarComponent,
        BudgetApprovalComponent,
        BudgetApprovalViewComponent,
        XyzComponent,
        FormDocumentHeaderComponent,
        CustomDocumentBuilderComponent,
        ApprovedApplicationsComponent
    ],

    providers: [
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true
        },
        AppService,
        NotificationService,
        Title,
        {
            provide: HIGHLIGHT_OPTIONS,
            useValue: {
                coreLibraryLoader: () => import('highlight.js/lib/core'),
                languages: {
                    json: () => import('highlight.js/lib/languages/json'),
                    typescript: () => import('highlight.js/lib/languages/typescript'),
                    xml: () => import('highlight.js/lib/languages/xml'),
                },
            },
        },
    ],
    bootstrap: [AppComponent],
})
export class AppModule { }

// AOT compilation support
export function httpTranslateLoader(http: HttpClient) {
    return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}
