import { Component, ViewChild } from '@angular/core';
import {TaxCategoryService} from "../../services/tax-category/tax-category.service";
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {Validators} from "@angular/forms";
import {data} from "autoprefixer";
import {NotificationService} from "../../NotificationService";
import * as moment from 'moment';
import {MenuService} from "../../layout/menu-service/menu.service";
import {ModalComponent} from "angular-custom-modal";
import {DocumentService} from "../../services/document/document-service";
import {Router} from "@angular/router";

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})
export class TaxListComponent {
    menus: any[] = [];
    hasMarketingApprovalView: boolean = false;
    hasMarketingApprovalAdd: boolean = false;
    hasMarketingApprovalEdit: boolean = false;
    constructor(private saleOrderService: SaleOrderService,private notificationService: NotificationService,private menuService : MenuService,private documentService: DocumentService,private router: Router) {}
    @ViewChild ('datatable') datatable: any;
    search = '';
    saleOrderLst: any;
    items:any;
    @ViewChild('modal') modal: any;
    @ViewChild('modalCancel') modalCancel: any;
    selectedItem: any = null;
    status: string = '';
    remarks: string = '';
    dateFrom: string | null = null;
    dateTo: string | null = null;
    dteFrom: Date | null = null;
    dteTo: Date | null = null;
    areDatesValid: boolean = false;
    isTaxHead: boolean = false;
    uploadedDocuments : any[] = [];
    selectedSaleOrderNo: any;
    public myFile: File | null = null;
    public newDocument: any = {};
    statusChecked: boolean = false; // Initialize the checkbox state
    prepareStatusChange(item: any, status: string) {
        this.selectedItem = item;
        this.status = status;
        if (status === 'cancel' || status == 'Hold') {
            this.modalCancel.open();
        }else {
            this.modal.open();
        }
    }

    cols = [
        { field: 'serSaleOrderId', title: 'Sr No' },
        { field: 'txtDealNo', title: 'Invoice No' },
        { field: 'slsTblDeal.cfgTblDealer.txtCustomerName', title: 'Media House' },
        { field: 'dteDate', title: 'Date' },
        {field:   'dteCreateddate',title: 'Created Date'},
        { field: 'numAmount', title: 'Invoice Amount' },
        { field: 'lastApprovedName', title: 'Last Approved Name' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' }
        /*{ field: 'txtStatus1', title: 'Marketing Approval Status' },
        { field: 'txtStatus2', title: 'Procurement Approval Status' },
        { field: 'txtStatus3', title: 'Tax Approval Status' },
        { field: 'txtStatus4', title: 'Finance Approval Status' },
        { field: 'txtStatus5', title: 'Audit Approval Status' }*/
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
        this.saleOrderService
            .getSaleOrder(3)
            .subscribe((data: any) => {
                if (data) {
                    // @ts-ignore
                    this.items = data.filter(item => item.numLevel === 3 && item.txtLevel === 'THIRD'  &&  (item.txtStatus3 === 'Pending' || item.txtStatus3 == 'Hold'));
                    this.items = data.map((item: any) => ({
                        ...item,
                        dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                        dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null,
                        txtDealNo:item.slsTblDeal.txtDealNo ? item.slsTblDeal.txtDealNo : null,
                        lastApprovedName:this.getLastApprovedByName(item)
                    }));
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
                    // Check if the user is Marketing Head
                    this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);

                    // @ts-ignore
                    this.isTaxHead = user.cfgTblRole?.txtRoleName === 'TAX_HEAD';
                    console.log(this.items)
                }
            });
    }


    submitStatusChange() {

        if (!this.remarks || this.remarks.trim() === '') {
            if(this.status == 'cancel' || this.status == 'Hold'){
                this.modalCancel.open()
            }else{
                this.modal.open(); // Open the modal if remarks are missing
            }
            this.notificationService.showMessage("Please provide valid remarks.", 'danger');
            return;
        }

        if(this.status == 'cancel' || this.status == 'Hold'){
            this.modalCancel.close();
        }else{
            this.modal.close(); // Open the modal if remarks are missing
        }
        if (this.status == 'Hold' || this.status == 'cancel' || this.status == 'Unhold' || this.status == 'send') {

            this.submitHoldCancelChange();
            // this.ngOnInit();
            this.searchSaleInvoice(this.dateFrom, this.dateTo);

        } else {

            if (this.selectedItem) {
                const payload = {
                    //  txtStatus1: this.status,
                    txtTaxRemarks: this.remarks,
                    level: 3,
                    txtDivision: this.selectedItem.serSaleOrderId,
                    txtInvoiceNo: this.selectedItem.txtSapCode,
                };
                console.log('Payload:', payload);
                /*this.saleOrderService.updateSaleOrder(payload);*/
                this.saleOrderService.updateSaleOrder(payload).subscribe(
                    (response: any) => {

                        if (response && response.status === 200) {
                            console.log('Update successful:', response);
                            this.notificationService.showMessage("Sale order updated successfully.",'success');
                            this.selectedItem = null;
                            this.remarks = '';
                            this.searchSaleInvoice(this.dateFrom, this.dateTo)
                        } else {
                            console.error('Unexpected response:', response);
                            this.notificationService.showMessage("Failed to update sale order. Please try again.",'danger')
                            this.searchSaleInvoice(this.dateFrom, this.dateTo);
                        }
                    },
                    (error: any) => {
                        console.error('Error:', error);

                        this.notificationService.showMessage("An error occurred while updating the sale order.",'danger')
                        this.searchSaleInvoice(this.dateFrom, this.dateTo);
                    }
                );

                this.searchSaleInvoice(this.dateFrom, this.dateTo)
            }
        }
    }

    submitHoldCancelChange() {
        if(this.status == 'cancel' || this.status == 'Hold'){
            this.modalCancel.close();
        }else{
            this.modal.close(); // Open the modal if remarks are missing
        }
        this.dteFrom = this.toDate(this.dateFrom);
        this.dteTo = this.toDate(this.dateTo);
        if (this.selectedItem && this.remarks) {

            this.saleOrderService.searchSaleInvoice(this.dteFrom, this.dteTo).subscribe((data: any) => {
                if (data) {
                    // @ts-ignore
                    // @ts-ignore
                    this.slsTblSaleOrder = data.find(item => item.serSaleOrderId === this.selectedItem.serSaleOrderId);

                    // @ts-ignore
                    console.log(this.slsTblSaleOrder);


                    if (this.status === 'Unhold') {
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus3 = 'Pending';

                        // @ts-ignore
                        this.slsTblSaleOrder.txtoriginalStatus = this.status;
                    } else if (this.status === 'cancel') {
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus1 = 'cancel';
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus2 = 'cancel'
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus3 = 'cancel';

                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus = 'cancel';

                        // @ts-ignore
                        this.slsTblSaleOrder.txtoriginalStatus = this.status;
                        // @ts-ignore
                        this.slsTblSaleOrder.blIsVendor = this.statusChecked;
                    }else if (this.status === 'send') {
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus2 = 'Pending';
                        // @ts-ignore
                        this.slsTblSaleOrder.numLevel = 2;
                        // @ts-ignore
                        this.slsTblSaleOrder.txtLevel = 'SECOND';

                        // @ts-ignore
                        this.slsTblSaleOrder.dteApproveddate2 = null;
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus3 = 'Send Back';

                        // @ts-ignore
                        this.slsTblSaleOrder.txtoriginalStatus = 'Send Back';
                        /*// @ts-ignore
                        this.slsTblSaleOrder.txtStatus3 = 'Pending';*/

                        // @ts-ignore
                        this.slsTblSaleOrder.blIsVendor = false
                    } else {
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus3 = this.status;

                        // @ts-ignore
                        this.slsTblSaleOrder.txtoriginalStatus = this.status;
                        // @ts-ignore
                        this.slsTblSaleOrder.blIsVendor = this.statusChecked;
                    }

                    // @ts-ignore
                    this.slsTblSaleOrder.txtReasonforCancel = this.remarks;
                    // @ts-ignore
                    console.log('Payload:', this.slsTblSaleOrder);
                    // @ts-ignore
                    this.saleOrderService.updateSaleOrderFromSap(this.slsTblSaleOrder).subscribe(
                        (response: any) => {
                            let data = typeof response === 'string' ? JSON.parse(response) : response;
                            if (data && data.status === 200 && data.body.status == "Success") {

                                console.log('Update successful:', response);
                                this.notificationService.showMessage("Sale order updated successfully.", 'success');
                                this.selectedItem = null;
                                this.remarks = '';
                                this.searchSaleInvoice(this.dateFrom, this.dateTo);

                            } else {
                                console.error('Unexpected response:', response);
                                this.notificationService.showMessage("Failed to update sale order. Please try again.", 'danger');
                                this.searchSaleInvoice(this.dateFrom, this.dateTo);
                            }
                        },
                        (error: any) => {
                            console.error('Error:', error);
                            this.notificationService.showMessage("An error occurred while updating the sale order.", 'danger');
                            this.searchSaleInvoice(this.dateFrom, this.dateTo);
                        }
                    );
                }
            });


        }


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
                        // @ts-ignore
                        this.items = data.filter(item => item.numLevel === 3 && item.txtLevel  === 'THIRD'  && (item.txtStatus3 === 'Pending' || item.txtStatus3 == 'Hold'));

                        this.items = this.items.map((item: any) => ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            dteCreateddate: item.dteCreateddate ? this.formatDate(new Date(item.dteCreateddate)) : null,
                            txtDealNo:item.slsTblDeal.txtDealNo ? item.slsTblDeal.txtDealNo : null,
                            lastApprovedName:this.getLastApprovedByName(item)
                        }));

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
                        // Check if the user is Marketing Head
                        this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);

                        // @ts-ignore
                        this.isTaxHead = user.cfgTblRole?.txtRoleName === 'TAX_HEAD';

                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');
                    }
                });
        } else {
            this.notificationService.showMessage('Both dates must be valid and dateFrom must be before dateTo', 'danger');
        }
    }


    cancelStatusChange() {
        this.selectedItem = null;
        this.remarks = '';
    }

    loadPermissionRoles(roleId: number | undefined, userId: number): void {

        this.menuService.getAllSubMenuRoles(roleId, userId).subscribe({
            next: (data) => {
                this.menus = data;
                console.log(this.menus);

                const filterCriteria = 'Tax Approval';
                this.menus = this.menus.filter(menu =>
                    menu.cfgTblSubMenu.txtSubMenuName.includes(filterCriteria)
                );
                console.log("Marketing Approval",this.menus);


                if (this.menus.length > 0) {
                    this.hasMarketingApprovalView = this.menus[0].blIsview;
                    this.hasMarketingApprovalAdd = this.menus[0].blIsAdd;
                    this.hasMarketingApprovalEdit = this.menus[0].blIsUpdate;
                }
            },
            error: (error) => {
                console.error('Error fetching submenu roles:', error);
            }
        });
    }

    shouldShowApprove(): boolean {
        const canShow = (this.hasMarketingApprovalAdd && !this.hasMarketingApprovalView) ||
            this.hasMarketingApprovalAdd ||
            (!this.hasMarketingApprovalEdit && this.hasMarketingApprovalView);

        const onlyViewTrue = this.hasMarketingApprovalView &&
            !this.hasMarketingApprovalAdd &&
            !this.hasMarketingApprovalEdit;

        return canShow && !onlyViewTrue;
    }

    shouldShowHold(): boolean {

        return (this.hasMarketingApprovalEdit && !this.hasMarketingApprovalView) ||
            (this.hasMarketingApprovalEdit && this.hasMarketingApprovalAdd);
    }

    shouldShowUnhold(): boolean {

        return (this.hasMarketingApprovalEdit && !this.hasMarketingApprovalView) ||
            (this.hasMarketingApprovalEdit && this.hasMarketingApprovalAdd);
    }

    shouldShowCancel(): boolean {

        return (this.hasMarketingApprovalEdit && !this.hasMarketingApprovalView) ||
            (this.hasMarketingApprovalEdit && this.hasMarketingApprovalAdd);
    }


    @ViewChild('documentModal', { static: false }) documentModal: ModalComponent | undefined;
    @ViewChild('uploadDocumentModal', { static: false }) uploadDocumentModal: ModalComponent | undefined;
    /* @ViewChild('uploadDocumentModal', { static: true }) uploadDocumentModal: ModalComponent | undefined;*/
    documentName: string = '';

    openDocumentModal(value: any) {

        this.selectedItem = value;
        this.selectedSaleOrderNo = value.serSaleOrderId;
        this.saleOrderService.getUploadedFile(value.serSaleOrderId).subscribe(
            (data) => {
                this.uploadedDocuments = data;
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
            this.newDocument.documentName = this.documentName;
            this.newDocument.slsTblSaleOrder =  this.selectedItem;
            const dateParts = this.newDocument.slsTblSaleOrder.dteDate.split('-');
            this.newDocument.slsTblSaleOrder.dteDate = `${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`;
            this.newDocument.slsTblSaleOrder.dteCreateddate = `${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`;
            fd.append('newDocument', JSON.stringify(this.newDocument));
            this.documentService.uploadDocument(fd).subscribe(
                (response) => {

                    let data = typeof response === 'string' ? JSON.parse(response) : response;
                    if (data && data.status === 'Success') {

                        this.notificationService.showMessage('Document Uploaded Successfully','success');
                        this.newDocument = {}; // Reset
                        this.getCandidateDocuments(this.selectedSaleOrderNo);

                    } else  {
                        this.notificationService.showMessage('Document Uploading Failed...','danger');
                    }
                    /*if (data === 'Failure') {
                        this.notificationService.showMessage('Document Uploading Failed...','danger');
                    } else if (data === 'Success') {
                        this.notificationService.showMessage('Document Uploaded Successfully','success');
                        this.newDocument = {}; // Reset
                        this.getCandidateDocuments(this.selectedSaleOrderNo);
                    }*/
                },
                () => {
                    this.notificationService.showMessage('Document Uploading Failed...','danger');
                }
            );
        } else {
            this.notificationService.showMessage('Please select a file to upload','danger');
        }
    }


    public  getLastApprovedByName(item : any) {
        if (item.serApprovedbyName6 != null) return item.serApprovedbyName6;
        if (item.serApprovedbyName5 != null) return item.serApprovedbyName5;
        if (item.serApprovedbyName4 != null) return item.serApprovedbyName4;
        if (item.serApprovedbyName3 != null) return item.serApprovedbyName3;
        if (item.serApprovedbyName2 != null) return item.serApprovedbyName2;
        return item.serApprovedbyName1; // If all others are null, return the first
    }

    openPdfEditor(documentId: string) {
        this.router.navigate(['/pdf-editor', documentId]);
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
}
