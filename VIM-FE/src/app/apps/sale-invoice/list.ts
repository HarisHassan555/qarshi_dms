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

export class SaleInvoiceListComponent {
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
    currentPage = 0;   // Default page number (0-based index)
    pageSize = 10;     // Number of records per page
    totalRecords = 0;  // Total records count
    cols = [
        { field: 'serDealId', title: 'Serial Id' },
        { field: 'txtDealNo', title: 'Vim No' },
        { field: 'dte_date_from', title: 'Start Date' },
        { field: 'dte_date_from', title: 'End Date' },
        { field: 'dteCreateddate',title: 'Created Date' },
        { field: 'cfgTblDealer.txtCustomerName', title: 'Media House' },
        { field: 'blnStatus', title: 'Status' },
        { field: 'txtSapNo', title: 'Service Order No' }/*,
        { field: 'dteIssueTime', title: 'Time' },
        { field: 'note', title: 'Spot' }*//*,
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },*/
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
        const params = {
            page: this.currentPage,
            size: this.pageSize,
            search: this.search
        };
        this.saleOrderService
            .getAll(params)
            .subscribe((data: any) => {
                if (data) {
                 //   this.items = data;
                    this.items = data.map((item: any) => {
                        // @ts-ignore
                        // @ts-ignore
                        return ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null,
                            txtSaleOrderNo: item.txtSaleOrderNo,
                        });
                    });
                    console.log(this.items)
                }
            });
    }

    onPageChange(event: any) {
        this.currentPage = event.page - 1;
        this.searchSaleOrder(this.dateFrom,this.dateTo);
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

    searchSaleOrder(dateFromStr: string | null, dateToStr: string | null) {
        this.dteFrom = this.toDate(dateFromStr);
        this.dteTo = this.toDate(dateToStr);

        this.validateDates();

        if (this.areDatesValid && this.dateFrom && this.dateTo) {

            this.saleOrderService
                .searchSaleOrder(this.dteFrom, this.dteTo)
                .subscribe((data: any) => {
                    if (data) {
                        this.items = data;
                        this.items = data.map((item: any) => {
                            // @ts-ignore
                            // @ts-ignore
                            return ({
                                ...item,
                                dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                                dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null,
                                txtSaleOrderNo: item.txtSaleOrderNo,
                            });
                        });
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');

                    }
                });
        } else {
            this.notificationService.showMessage('Both dates must be valid and dateFrom must be before dateTo', 'danger');
        }
    }

}
