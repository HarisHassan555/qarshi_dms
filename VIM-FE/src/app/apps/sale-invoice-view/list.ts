import { Component, ViewChild } from '@angular/core';
import {TaxCategoryService} from "../../services/tax-category/tax-category.service";
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {Validators} from "@angular/forms";
import {NotificationService} from "../../NotificationService";
import * as moment from 'moment';
import {ModalComponent} from "angular-custom-modal";
import {DocumentService} from "../../services/document/document-service";
import {Router} from "@angular/router";

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})

export class SaleInvoiceViewListComponent {
    constructor(private saleOrderService: SaleOrderService,private notificationService: NotificationService,private documentService: DocumentService,private router: Router) {}
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
        /* { field: 'dteDate', title: 'Start Date' },
        { field: 'dteCreateddate', title: 'Date' },*/
        {
            field: 'dteDate',
            title: 'Start Date',
            formatter: function(value: string | number | Date) {
                if (!value) return ''; // Handle null or undefined
                const date = new Date(value);
                // @ts-ignore
                if (isNaN(date)) return value; // Return original if invalid
                return date.toLocaleDateString('en-GB', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric'
                }).split('/').join('-'); // Outputs DD-MM-YYYY
            }
        },
        {
            field: 'dteCreateddate',
            title: 'Date',
            formatter: function(value: string | number | Date) {
                if (!value) return '';
                const date = new Date(value);
                // @ts-ignore
                if (isNaN(date)) return value;
                return date.toLocaleDateString('en-GB', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric'
                }).split('/').join('-');
            }
        },
        { field: 'numAmount', title: 'Invoice Amount' },
        { field: 'fbrinvoiceno', title: 'Sale tax Invoice No' },
        { field: 'txtSapInvoiceNo', title: 'Procurement No' },
       /* { field: 'dte_date_from', title: 'End Date' }, dteCreateddate*/
        { field: 'txtStatus1', title: 'Marketing Approval Status' },
        { field: 'txtStatus2', title: 'Procurement Approval Status' },
        { field: 'txtStatus3', title: 'Tax Approval Status' },
        { field: 'txtStatus4', title: 'Finance Approval Status' },
        { field: 'txtStatus5', title: 'Audit Approval Status' },
        { field: 'txtStatus6', title: 'Payment Approval Status' },
        { field: 'timeString', title: 'Time' },
        { field: 'notes', title: 'Spot'},
        { field: 'fbrinvoiceDate', title: 'Invoice Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' }
    ];
    @ViewChild('modal') modal: any;
    @ViewChild('modalCancel') modalCancel: any;
    selectedItem: any = null;
    selectedSaleOrderNo: any;
    uploadedDocuments : any[] = [];

    public newDocument: any = {};
    isFiltered: boolean = false;
    @ViewChild('documentModal', { static: false }) documentModal: ModalComponent | undefined;
    @ViewChild('uploadDocumentModal', { static: false }) uploadDocumentModal: ModalComponent | undefined;
    public myFile: File | null = null;
    documentName: string = '';
    isHead: boolean = false;
    ngOnInit() {
        const from = moment().subtract(8,'d').format('YYYY-MM-DD');
        const to = moment().format('YYYY-MM-DD');
        this.dateFrom = from ? from : null;
        this.dateTo = to ? to : null;
        const userJson = localStorage.getItem('user');
        // @ts-ignore
        let user: {
            cfgTblRole: number | undefined;
            serUserId: number; };
        if (userJson) {
            // @ts-ignore
            user = JSON.parse(userJson) as CfgTblUser;
        }
        // @ts-ignore
        //this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);
        // @ts-ignore
        this.isHead = user.cfgTblRole?.txtRoleName === 'MARKETING_HEAD' || user.cfgTblRole?.txtRoleName === 'ADMIN';
        this.getSaleOrder();

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


    formatDateLocale(date: Date | null): string | null {

        // @ts-ignore
        date = new Date(date);
        if (date instanceof Date && !isNaN(date.getTime())) {
            const options = {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false,
            };

            // @ts-ignore
            const formattedDate = date.toLocaleString('en-US', options);
            return formattedDate.replace(',', '');
            //  return formattedDate;
        } else {
            return null;
        }
    }


    getSaleOrder() {
        this.saleOrderLst = [];
        this.saleOrderService
            .getAllSaleOrder()
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
                    console.log(this.items);
                }
            });
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
                        // @ts-ignore
                        // @ts-ignore
                        this.items = data.map((item: any) => ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null,
                            txtSaleOrderNo: this.removeExtraZeros(item.txtSaleOrderNo),
                        }));
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');

                    }
                });
        } else {
            this.notificationService.showMessage('Both dates must be valid and dateFrom must be before dateTo', 'danger');
        }
    }

    // Function to remove extra zeros
    removeExtraZeros(invoiceNo: string): string {
        console.log(invoiceNo.replace(/^0+/, ''))
        return invoiceNo.replace(/^0+/, '');
    }

    openDocumentModal(value: any) {

        this.selectedItem = value;
        this.selectedSaleOrderNo = value.serSaleOrderId;
        this.saleOrderService.getUploadedFile(value.serSaleOrderId).subscribe(
            (data) => {
                this.uploadedDocuments = data;
                // this.formatDate(new Date(this.uploadedDocuments.dteCreateddate));
                console.log('Documents:', this.uploadedDocuments);
            },
            (error) => {
                console.error('Error fetching documents:', error);
            });
        // @ts-ignore
        this.documentModal?.open();
    }

    onFileChange(event: any) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            this.myFile = input.files[0];
        }
    }


    openPdfEditor(documentId: string) {
        // Navigate to the PDF editor, passing the document ID as a route parameter
        this.router.navigate(['/pdf-editor', documentId]);
    }


    uploadDocument() {
        const fd = new FormData();
        if (this.myFile) {
            fd.append('file', this.myFile);
            this.newDocument.documentName = this.documentName;
            this.newDocument.slsTblSaleOrder =  this.selectedItem;
            const dateParts = this.newDocument.slsTblSaleOrder.dteDate.split('-');
            this.newDocument.slsTblSaleOrder.dteDate = `${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`;
            this.newDocument.slsTblSaleOrder.dteCreateddate = `${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`;
            fd.append('newDocument', JSON.stringify(this.newDocument));
            this.documentService.uploadDocument(fd).subscribe(
                (data) => {
                    if (data === 'Failure') {
                        this.notificationService.showMessage('Document Uploading Failed...','danger');
                    } else if (data === 'Success') {
                        this.notificationService.showMessage('Document Uploaded Successfully','success');
                        this.newDocument = {}; // Reset
                        this.getCandidateDocuments(this.selectedSaleOrderNo);
                    }
                },
                () => {
                    this.notificationService.showMessage('Document Uploading Failed...','danger');
                }
            );
        } else {
            this.notificationService.showMessage('Please select a file to upload','danger');
        }
    }
    getCandidateDocuments(candidateId: string): void {

        this.saleOrderService.getUploadedFile(candidateId).subscribe(
            (data) => {
                this.uploadedDocuments = data;
                // @ts-ignore
                /*this.uploadedDocuments.dteCreateddate = this.formatDateLocale(new Date(this.uploadedDocuments[0].dteCreateddate))*/
                //console.log('Documents:', this.uploadedDocuments[0].dteCreateddate);
            },
            (error) => {
                console.error('Error fetching documents:', error);
            });
    }

    downloadDocument(documentId: string) {
        this.documentService.downloadDocument(documentId).subscribe(
            (data) => {
                const blob = new Blob([data], { type: 'application/pdf' });
                const objectUrl = window.URL.createObjectURL(blob);
                window.open(objectUrl, '_blank');
            },
            () => {
                this.notificationService.showMessage('Unable to view Document...','danger');
            }
        );
    }

    exportTable(type: string) {
        let columns: any = this.cols.map((d: { field: any }) => {
            return d.field;
        });

        let records = this.items;
        let filename = 'table';

        let newVariable: any;
        newVariable = window.navigator;

        if (type == 'csv') {
            let coldelimiter = ';';
            let linedelimiter = '\n';
            let result = columns
                .map((d: any) => {
                    return this.capitalize(d);
                })
                .join(coldelimiter);
            result += linedelimiter;
            records.map((item: { [x: string]: any }) => {
                columns.map((d: any, index: number) => {
                    if (index > 0) {
                        result += coldelimiter;
                    }
                    let val = item[d] ? item[d] : '';
                    result += val;
                });
                result += linedelimiter;
            });

            if (result == null) return;
            if (!result.match(/^data:text\/csv/i) && !newVariable.msSaveOrOpenBlob) {
                var data = 'data:application/csv;charset=utf-8,' + encodeURIComponent(result);
                var link = document.createElement('a');
                link.setAttribute('href', data);
                link.setAttribute('download', filename + '.csv');
                link.click();
            } else {
                var blob = new Blob([result]);
                if (newVariable.msSaveOrOpenBlob) {
                    newVariable.msSaveBlob(blob, filename + '.csv');
                }
            }
        } else if (type == 'print') {
            var rowhtml = '<p>' + filename + '</p>';
            rowhtml +=
                '<table style="width: 100%; " cellpadding="0" cellcpacing="0"><thead><tr style="color: #515365; background: #eff5ff; -webkit-print-color-adjust: exact; print-color-adjust: exact; "> ';
            columns.map((d: any) => {
                rowhtml += '<th>' + this.capitalize(d) + '</th>';
            });
            rowhtml += '</tr></thead>';
            rowhtml += '<tbody>';

            records.map((item: { [x: string]: any }) => {
                rowhtml += '<tr>';
                columns.map((d: any) => {
                    let val = item[d] ? item[d] : '';
                    rowhtml += '<td>' + val + '</td>';
                });
                rowhtml += '</tr>';
            });
            rowhtml +=
                '<style>body {font-family:Arial; color:#495057;}p{text-align:center;font-size:18px;font-weight:bold;margin:15px;}table{ border-collapse: collapse; border-spacing: 0; }th,td{font-size:12px;text-align:left;padding: 4px;}th{padding:8px 4px;}tr:nth-child(2n-1){background:#f7f7f7; }</style>';
            rowhtml += '</tbody></table>';
            var winPrint: any = window.open('', '', 'left=0,top=0,width=1000,height=600,toolbar=0,scrollbars=0,status=0');
            winPrint.document.write('<title>Print</title>' + rowhtml);
            winPrint.document.close();
            winPrint.focus();
            winPrint.print();
            // winPrint.close();
        } else if (type == 'txt') {
            let coldelimiter = ',';
            let linedelimiter = '\n';
            let result = columns
                .map((d: any) => {
                    return this.capitalize(d);
                })
                .join(coldelimiter);
            result += linedelimiter;
            records.map((item: { [x: string]: any }) => {
                columns.map((d: any, index: number) => {
                    if (index > 0) {
                        result += coldelimiter;
                    }
                    let val = item[d] ? item[d] : '';
                    result += val;
                });
                result += linedelimiter;
            });

            if (result == null) return;
            if (!result.match(/^data:text\/txt/i) && !newVariable.msSaveOrOpenBlob) {
                var data = 'data:application/txt;charset=utf-8,' + encodeURIComponent(result);
                var link = document.createElement('a');
                link.setAttribute('href', data);
                link.setAttribute('download', filename + '.txt');
                link.click();
            } else {
                var blob = new Blob([result]);
                if (newVariable.msSaveOrOpenBlob) {
                    newVariable.msSaveBlob(blob, filename + '.txt');
                }
            }
        }
    }

    capitalize(text: string) {
        return text
            .replace('_', ' ')
            .replace('-', ' ')
            .toLowerCase()
            .split(' ')
            .map((s: string) => s.charAt(0).toUpperCase() + s.substring(1))
            .join(' ');
    }
}
