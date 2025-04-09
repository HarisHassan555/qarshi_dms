import { Component, ViewChild } from '@angular/core';
import {TaxCategoryService} from "../../services/tax-category/tax-category.service";
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {Validators} from "@angular/forms";
import {NotificationService} from "../../NotificationService";
import * as moment from 'moment';

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})

export class SaleInvoiceStatusListComponent {
    constructor(private saleOrderService: SaleOrderService,private notificationService: NotificationService) {}
    @ViewChild ('datatable') datatable: any;
    search = '';
    saleOrderLst: any;
    items:any;
    dateFrom: string | null = null;
    dateTo: string | null = null;
    dteFrom: Date | null = null;
    dteTo: Date | null = null;
    areDatesValid: boolean = false;
    cols = [
        { field: 'serSaleOrderId', title: 'Sr No' },
        { field: 'txtSaleOrderNo', title: 'Invoice No' },
        { field: 'slsTblDeal.cfgTblDealer.txtCustomerName', title: 'Media House' },
        { field: 'dteDate', title: 'Date' },
        { field: 'dteCreateddate', title: 'Date' },
        { field: 'numAmount', title: 'Invoice Amount' },
        { field: 'txtSoapReturnType', title: 'Integration Status' },
        { field: 'txtStatus1', title: 'Marketing Approval Status' },
        { field: 'txtStatus2', title: 'Procurement Approval Status' },
        { field: 'txtStatus3', title: 'Tax Approval Status' },
        { field: 'txtStatus4', title: 'Finance Approval Status' },
        { field: 'txtStatus5', title: 'Audit Approval Status' },
        { field: 'txtSoapResponseMsg', title: 'Sap Response' }
    ];

    ngOnInit() {
        const from = moment().subtract(8,'d').format('YYYY-MM-DD');
        const to = moment().format('YYYY-MM-DD');
        this.dateFrom = from ? from : null;
        this.dateTo = to ? to : null;
        this.getSaleOrder();
    }

    getSaleOrder() {
        this.saleOrderLst = [];
        this.searchSaleInvoice(this.dateFrom,this.dateTo)
    }

    validateDates() {

        if (this.dateFrom && this.dateTo) {
            this.areDatesValid = this.dateFrom <= this.dateTo;
        } else {
            this.areDatesValid = false;
        }
    }

    toDate(input: string | null): Date | null {
        if (input === null) return null;
        const date = new Date(input);
        return isNaN(date.getTime()) ? null : date;
    }

    formatDate(date: Date | null): string | null {
        if (date instanceof Date && !isNaN(date.getTime())) {
            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const year = date.getFullYear();
            return `${day}-${month}-${year}`;
        } else {
            return null;
        }
    }


    searchSaleInvoice(dateFromStr: string | null, dateToStr: string | null) {
        this.dteFrom = this.toDate(dateFromStr);
        this.dteTo = this.toDate(dateToStr);

        this.validateDates();

        if (this.areDatesValid && this.dateFrom && this.dateTo) {

            this.saleOrderService
                .searchSaleInvoice(this.dteFrom, this.dteTo)
                .subscribe((data: any) => {
                    if (data) {
                        this.items = data;
                        this.items = this.items.map((item: any) => ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null,
                            txtSoapResponseMsg : item.txtSoapResponseMsg && item.txtSoapResponseMsg.trim() !== ''
                                ? item.txtSoapResponseMsg
                                : item.txtSoapResponseMsg,
                            txtSoapReturnType : item.txtSoapReturnType && item.txtSoapReturnType.trim() !== ''
                                ? (item.txtSoapReturnType === 'S' ? 'SUCCESS' : item.txtSoapReturnType)
                                : item.txtSoapReturnType
                        }));

                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');

                    }
                });
        } else {
            this.notificationService.showMessage('Both dates must be valid and dateFrom must be before dateTo', 'danger');
        }
    }
}
