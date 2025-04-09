import { Component, ViewChild } from '@angular/core';
import {TaxCategoryService} from "../../services/tax-category/tax-category.service";
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {Validators} from "@angular/forms";
import {NotificationService} from "../../NotificationService";
import * as moment from 'moment';
import {ModalComponent} from "angular-custom-modal";
import {DocumentService} from "../../services/document/document-service";
import {UserService} from "../../services/user/user.service";

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})

export class VendorListComponent {
    constructor(private saleOrderService: SaleOrderService,private notificationService: NotificationService,private documentService: DocumentService,private userService: UserService) {}
    @ViewChild ('datatable') datatable: any;
    search = '';
    saleOrderLst: any;
    items:any;
    dateFrom: string | null = null;
    dateTo: string | null = null;
    dteFrom: Date | null = null;
    dteTo: Date | null = null;
    areDatesValid: boolean = false;
    uploadedDocuments : any[] = [];
    public myFile: File | null = null;
    selectedItem: any = null;
    selectedSaleOrderNo: any;
    public newDocument: any = {};
    roles:  any;
    users: any;
    cols = [
        { field: 'serSaleOrderId', title: 'Sr No' },
        { field: 'txtSaleOrderNo', title: 'Invoice No' },
        { field: 'slsTblDeal.cfgTblDealer.txtCustomerName', title: 'Media House' },
        { field: 'dteDate', title: 'Start Date' },
        { field: 'dteCreateddate', title: 'Date' },
        { field: 'numAmount', title: 'Invoice Amount' },
        { field: 'txtStatus', title: 'Status' },/*,
        { field:  '',title:'Comments'}*/
       /* { field: 'dte_date_from', title: 'End Date' }, dteCreateddate*/
        /*{ field: 'txtStatus1', title: 'Marketing Approval Status' },
        { field: 'txtStatus2', title: 'Procurement Approval Status' },
        { field: 'txtStatus3', title: 'Tax Approval Status' },
        { field: 'txtStatus4', title: 'Finance Approval Status' },
        { field: 'txtStatus5', title: 'Audit Approval Status' },*/
        { field: 'timeString', title: 'Time' },
        { field: 'notes', title: 'Spot'},
        { field: 'fbrinvoiceno', title: 'Invoice No(Internal)' },
        { field: 'fbrinvoiceDate', title: 'Invoice Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },



    ];

    ngOnInit() {
        const from = moment().subtract(8,'d').format('YYYY-MM-DD');
        const to = moment().format('YYYY-MM-DD');
        this.dateFrom = from ? from : null;
        this.dateTo = to ? to : null;
        this.getSaleOrder();
        /*this.getRoles();*/
        this.getUsers();
    }


    getUsers() {
        this.users = [];
        this.userService.getUsers()
            .subscribe(data => {
                this.users = data;
                this.users = this.users.map((user: { cfgTblRole: null; }) => {
                    if (user.cfgTblRole) {
                        const filteredRole = this.roles.find((role: { serRoleId: null; }) => role.serRoleId === user.cfgTblRole);
                        user.cfgTblRole = filteredRole || null;
                    } else {
                        user.cfgTblRole = null;
                    }

                    return user;
                });

                console.log(this.users);
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


    getSaleOrder() {
        this.saleOrderLst = [];
        this.saleOrderService
            .getAllSaleOrder()
            .subscribe((data: any) => {
                if (data) {
                    this.items = data;
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
                   this.items = data.filter(item => item.serCreatedUserId == user.serUserId);
                    this.items = this.items.map((item: any) => {
                        return ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null
                        });
                    });
                    console.log(this.items)
                }
            });
    }

    getRoles() {
        this.userService.getRoles()
            .subscribe(data => {
                if (data) {
                    this.roles = data;
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
                        this.items = data.filter(item => item.serCreatedUserId == user.serUserId);
                        this.items = this.items.map((item: any) => ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null
                        }));
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');

                    }
                });
        } else {
            this.notificationService.showMessage('Both dates must be valid and dateFrom must be before dateTo', 'danger');
        }
    }


    @ViewChild('documentModal', { static: false }) documentModal: ModalComponent | undefined;
    @ViewChild('uploadDocumentModal', { static: false }) uploadDocumentModal: ModalComponent | undefined;

    documentName: string = '';

    openDocumentModal(value: any) {

        this.selectedItem = value;
        this.selectedSaleOrderNo = value.serSaleOrderId;
        this.saleOrderService.getUploadedFile(value.serSaleOrderId).subscribe(
            (data) => {
                const userJson = localStorage.getItem('user');
                // @ts-ignore
                let user: {
                    cfgTblRole: number | undefined;
                    serUserId: number; };
                if (userJson) {
                    // @ts-ignore
                    user = JSON.parse(userJson) as CfgTblUser;
                }
                debugger;
                console.log("",this.users);
                /*this.uploadedDocuments = data.filter((item: { serCreatedUserId: number; }) => item.serCreatedUserId == user.serUserId);*/
                this.uploadedDocuments = data.filter((item: {
                    txtrole: String;
                    serCreatedUserId: number }) =>
                    item.serCreatedUserId === user.serUserId ||
                    this.users.some((u: { serUserId: number; }) => item.serCreatedUserId === u.serUserId && item.txtrole == "VENDOR"));

               // this.uploadedDocuments = data;
                console.log('Documents:', this.uploadedDocuments);
            },
            (error) => {
                console.error('Error fetching documents:', error);
            });
        // @ts-ignore
        this.documentModal?.open();
    }



    closeDocumentModal() {
        // @ts-ignore
        this.documentModal.hide();
    }

    openUploadDocumentModal() {
        // @ts-ignore
        this.uploadDocumentModal.show();
    }

    closeUploadDocumentModal() {
        // @ts-ignore
        this.uploadDocumentModal.hide();
    }

    onFileChange(event: any) {

        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            this.myFile = input.files[0];
        }
    }




    getCandidateDocuments(candidateId: string): void {

        this.saleOrderService.getUploadedFile(candidateId).subscribe(
            (data) => {
                this.uploadedDocuments = data;
                console.log('Documents:', this.uploadedDocuments);
            },
            (error) => {
                console.error('Error fetching documents:', error);
            });
    }



    removeDocument(documentId: string) {

        this.documentService.removeDocument(documentId).subscribe(
            (data) => {
                if (data === 'Failure') {
                    this.notificationService.showMessage('Unable to remove Document','danger');
                } else if (data === 'Success') {
                    this.notificationService.showMessage('Document removed successfully','success');
                    this.getCandidateDocuments(this.selectedSaleOrderNo);
                }
            },
            () => {
                this.notificationService.showMessage('Unable to Remove Document \n Internal Error','danger');
            }
        );
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

    uploadDocument() {
        const fd = new FormData();
        if (this.myFile) {
            fd.append('file', this.myFile);

            // Format the date to yyyy-MM-dd


            this.newDocument.documentName = this.documentName;
            this.newDocument.slsTblSaleOrder =  this.selectedItem;
            const dateParts = this.newDocument.slsTblSaleOrder.dteDate.split('-');
            this.newDocument.slsTblSaleOrder.dteDate = `${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`;
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
}
