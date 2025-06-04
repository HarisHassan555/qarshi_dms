import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Routes, RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// modal
import { ModalModule } from 'angular-custom-modal';

// sortable
import { SortablejsModule } from '@dustfoundation/ngx-sortablejs';

// headlessui
import { MenuModule } from 'headlessui-angular';

// perfect-scrollbar
import { NgScrollbarModule } from 'ngx-scrollbar';

// quill editor
import { QuillModule } from 'ngx-quill';

// fullcalendar
import { FullCalendarModule } from '@fullcalendar/angular';

// tippy
import { NgxTippyModule } from 'ngx-tippy-wrapper';

// datatable
import { DataTableModule } from '@bhplugin/ng-datatable';

// icon
import { IconModule } from 'src/app/shared/icon/icon.module';

import { ScrumboardComponent } from './scrumboard';
import { ContactsComponent } from './contacts';
import { NotesComponent } from './notes';
import { TodolistComponent } from './todolist';
import { InvoicePreviewComponent } from './invoice/preview';
import { InvoiceAddComponent } from './invoice/add';
import { InvoiceEditComponent } from './invoice/edit';
import { CalendarComponent } from './calendar';
import { ChatComponent } from './chat';
import { MailboxComponent } from './mailbox';
import { InvoiceListComponent } from './invoice/list';
import {OrderPreviewComponent} from "./order/preview";
import {SaleInvoiceEditComponent} from "./sale-invoice/edit";
import {SaleInvoiceViewEditComponent} from "./sale-invoice-view/edit";
import {SaleInvoiceViewStatusEditComponent} from "./sale-invoice-status/edit";
import {SidebarComponent} from "../layouts/sidebar";

const routes: Routes = [
    // { path: 'apps/chat', component: ChatComponent, title: 'Chat | VRISTO - Multipurpose Tailwind Dashboard Template' },
    // { path: 'apps/mailbox', component: MailboxComponent, title: 'Mailbox | VRISTO - Multipurpose Tailwind Dashboard Template' },
    // { path: 'apps/scrumboard', component: ScrumboardComponent, title: 'Scrumboard | VRISTO - Multipurpose Tailwind Dashboard Template' },
    // { path: 'apps/contacts', component: ContactsComponent, title: 'Contacts | VRISTO - Multipurpose Tailwind Dashboard Template' },
    // { path: 'apps/notes', component: NotesComponent, title: 'Notes | VRISTO - Multipurpose Tailwind Dashboard Template' },
    // { path: 'apps/todolist', component: TodolistComponent, title: 'Todolist | VRISTO - Multipurpose Tailwind Dashboard Template' },
    { path: 'apps/invoice/list', component: InvoiceListComponent, title: 'Invoice List' },
    { path: 'apps/invoice/preview', component: InvoicePreviewComponent, title: 'Invoice Preview' },
    { path: 'apps/invoice/add', component: InvoiceAddComponent, title: 'Invoice Add' },
    { path: 'apps/invoice/edit', component: InvoiceEditComponent, title: 'Invoice Edit' },
    // { path: 'apps/calendar', component: CalendarComponent, title: 'Calendar' },
    { path: 'apps/order/preview/:serDealId', component: OrderPreviewComponent, title: 'Invoice Preview' },
    { path: 'apps/sale-invoice/edit/:serDealId', component: SaleInvoiceEditComponent, title: 'Invoice Edit' },
    { path: 'apps/sale-invoice-view/edit/:serDealId', component: SaleInvoiceViewEditComponent, title: 'Invoice Edit' },
    { path: 'apps/sale-invoice-status/edit/:serDealId', component: SaleInvoiceViewStatusEditComponent, title: 'Invoice Edit' },
];

@NgModule({
    imports: [
        RouterModule.forChild(routes),
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        ModalModule,
        SortablejsModule,
        MenuModule,
        NgScrollbarModule.withConfig({
            visibility: 'hover',
            appearance: 'standard',
        }),
        QuillModule.forRoot(),
        FullCalendarModule,
        NgxTippyModule,
        DataTableModule,
        IconModule,
    ],
    declarations: [
        ChatComponent,
        ScrumboardComponent,
        ContactsComponent,
        NotesComponent,
        TodolistComponent,
        InvoiceListComponent,
        InvoicePreviewComponent,
        InvoiceAddComponent,
        InvoiceEditComponent,
        CalendarComponent,
        MailboxComponent
    ],
})
export class AppsModule {}
