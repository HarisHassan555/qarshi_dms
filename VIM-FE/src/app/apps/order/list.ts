import { Component, ViewChild } from '@angular/core';
import { SaleOrderService } from "../../services/saleorder/sale-order.service";
import { NotificationService } from "../../NotificationService";
import * as moment from 'moment';

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})
export class OrderListComponent {
    @ViewChild('datatable') datatable: any;
    search = '';
    saleOrderLst: any;
    items: any;
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
       /* { field: 'dte_date_from', title: 'Date' },*/
        { field: 'dte_date_from', title: 'Start Date' },
        { field: 'dte_date_from', title: 'End Date' },
        { field: 'cfgTblDealer.txtCustomerName', title: 'Media House' },
        { field: 'txtStatus', title: 'Status' },
        { field: 'txtSapNo', title: 'Service Order No' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];

    constructor(private saleOrderService: SaleOrderService, private notificationService: NotificationService) {
        this.setDefaultDates();

    }

    setDefaultDates() {
        const today = new Date();
        const formattedDate = this.formatDate(today);
        this.dateFrom = formattedDate;
        this.dateTo = formattedDate;
    }

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
                    //this.items = data.filter((item: { txtStatus: string; }) => item.txtStatus != "close");
                    this.items = data;
                    console.log(this.items);
                }
            });
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
                     //   this.items = data.filter((item: { txtStatus: string; }) => item.txtStatus != "close");
                      this.items = data;
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');

                    }
                });
        } else {
            this.notificationService.showMessage('Both dates must be valid and dateFrom must be before dateTo', 'danger');
        }
    }


    markAsClose(value: string) {
        const payload = { id: value};
        this.saleOrderService
            .markAsClose(payload)
            .subscribe((data: any) => {
                if (data === "Success") {
                    this.notificationService.showMessage('Order Closed saved successfully','success');
                   // this.items = data;
                    this.ngOnInit();
                } else {
                    this.notificationService.showMessage('Error occurred while saving', 'danger');

                }
            });
    }
}
