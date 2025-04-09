import { Component, ViewChild } from '@angular/core';
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {NotificationService} from "../../NotificationService";
import * as moment from 'moment';
import {MenuService} from "../../layout/menu-service/menu.service";
import {of} from "rxjs";
import {ModalComponent} from "angular-custom-modal";
import {DocumentService} from "../../services/document/document-service";
import {NgModel} from "@angular/forms";

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})
export class DeparmentComponent {
    menus: any[] = [];
    hasMarketingApprovalView: boolean = false;
    hasMarketingApprovalAdd: boolean = false;
    hasMarketingApprovalEdit: boolean = false;
    isMarketingHead: boolean = false;
    departmentFilter: string = '';
    constructor(private saleOrderService: SaleOrderService,private notificationService: NotificationService,private menuService : MenuService,private documentService: DocumentService) {}
    @ViewChild ('datatable') datatable: any;
    // @ts-ignore
   /* @ViewChild('documentModal', { static: true }) documentModal: ModalComponent;
    @ViewChild('uploadDocumentModal', { static: true }) uploadDocumentModal: ModalComponent | undefined;*/
    search = '';
    saleOrderLst: any;
    items:any;
    @ViewChild('modal') modal: any;
    selectedItem: any = null;
    status: string = '';
    remarks: string = '';
    dateFrom: string ='';
    dateTo: string = '';
    dteFrom: Date | null = null;
    dteTo: Date | null = null;
    areDatesValid: boolean = false;
    uploadedDocuments : any[] = [];
    selectedSaleOrderNo: any;
    numLevel:any;
    prepareStatusChange(item: any, status: string) {
        this.selectedItem = item;
        this.status = status;
        this.modal.open();
    }
    public myFile: File | null = null;
    public newDocument: any = {};
    isFiltered: boolean = false;
   cols = [
       { field: 'serSaleOrderId', title: 'Sr No' },
       { field: 'txtDealNo', title: 'Invoice No' },
       { field: 'slsTblDeal.cfgTblDealer.txtCustomerName', title: 'Media House' },
       { field: 'dteDate', title: 'Date' }, /*       { field: 'dteCreateddate',title: 'Created Date' },*/
       { field: 'numAmount', title: 'Invoice Amount' },
       { field: 'lastApprovedName', title: 'Last Approved Name' },
  /*     { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' }*/
    ];
    private userJson: any;

    ngOnInit() {
        const from = moment().subtract(8,'d').format('YYYY-MM-DD');
        const to = moment().format('YYYY-MM-DD');
        // @ts-ignore
        this.dateFrom = from ? from : null;
        // @ts-ignore
        this.dateTo = to ? to : null;
        this.getSaleOrder();
        const userJson = localStorage.getItem('user');
        // @ts-ignore
        let user: {
            cfgTblRole: number | undefined;
            serUserId: number; };
        if (userJson) {
            // @ts-ignore
             user = JSON.parse(userJson) as CfgTblUser;
        }
        const storedValue = localStorage.getItem('selectedDepartment');
        this.numLevel = storedValue ? Number(storedValue) : this.uniqueDepartments[0].id;
        // @ts-ignore
        /*this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);
        // @ts-ignore
        this.isMarketingHead = user.cfgTblRole?.txtRoleName === 'Marketing Head';*/
    }

    getSaleOrder() {
        this.saleOrderLst = [];
        this.saleOrderService
            .getSaleOrder(this.numLevel)
            .subscribe((data: any) => {
                if (data) {
                    // @ts-ignore
                   /* this.items = data.filter(item => item.numLevel === 1 && item.txtLevel === 'First'  && (item.txtStatus1 === 'Pending' || item.txtStatus1 === 'Hold')
                    );*/
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
                    const storedValue = localStorage.getItem('selectedDepartment');
                    if (storedValue) {
                        this.numLevel = storedValue ? storedValue : this.uniqueDepartments[0].id;
                    } else {
                        this.numLevel = ''; // Default value if nothing is stored
                    }
                    // @ts-ignore
                    this.isMarketingHead = user.cfgTblRole?.txtRoleName === 'MARKETING_HEAD';
                    console.log(this.items)
                }
            });
    }


   /* submitStatusChange() {
        console.log("Date from", this.dateFrom);
        console.log("Date to", this.dateTo);


        if (!this.remarks || this.remarks.trim() === '') {
            this.modal.open(); // Open the modal if remarks are missing
            this.notificationService.showMessage("Please provide valid remarks.", 'danger');
            return;
        }

        this.modal.close();

        if (this.status == 'Hold' || this.status == 'cancel' || this.status == 'Unhold' || this.status == 'send') {
            this.submitHoldCancelChange();
            this.searchSaleInvoice(this.dateFrom, this.dateTo);
        } else {
            if (this.selectedItem) {
                const payload = {
                    txtMarketingRemarks: this.remarks,
                    level: 1,
                    txtDivision: this.selectedItem.serSaleOrderId,
                    txtInvoiceNo: this.selectedItem.txtSapCode,
                };
                console.log('Payload:', payload);

                this.saleOrderService.updateSaleOrder(payload).subscribe(
                    (response: any) => {
                        if (response && response.status === 200) {
                            console.log('Update successful:', response);
                            this.notificationService.showMessage("Sale order updated successfully.", 'success');
                            this.selectedItem = null;
                            this.remarks = '';
                            this.searchSaleInvoice(this.dateFrom, this.dateTo);
                        } else {
                            console.error('Unexpected response:', response);
                            this.notificationService.showMessage("Sale order details have encountered errors during processing with SAP. Please review the information and try again", 'danger');
                            this.searchSaleInvoice(this.dateFrom, this.dateTo);
                        }
                    },
                    (error: any) => {
                        console.error('Error:', error);
                        this.notificationService.showMessage("An error occurred while updating the sale order.", 'danger');
                        this.searchSaleInvoice(this.dateFrom, this.dateTo);
                    }
                );

                this.searchSaleInvoice(this.dateFrom, this.dateTo);
            }
        }
    }

    submitHoldCancelChange() {


        this.modal.close();
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
                        this.slsTblSaleOrder.txtStatus1 = 'Pending';
                    }else if (this.status === 'cancel') {
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus1 = 'cancel';
                    }else {
                        // @ts-ignore
                        this.slsTblSaleOrder.txtStatus1 = this.status;
                    }

                    // @ts-ignore
                    this.slsTblSaleOrder.txtReasonforCancel = this.remarks;

                    // @ts-ignore
                    console.log('Payload:', this.slsTblSaleOrder);

                    // @ts-ignore
                    this.saleOrderService.updateSaleOrderFromSap(this.slsTblSaleOrder).subscribe(
                        (response: any) => {

                            if (response === 'Success') {
                                console.log('Update successful:', response);
                                // Show a success message to the user
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
*/

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
    ngOnDestroy() {
        // Store the selected value in local storage
        if (this.numLevel) {
            localStorage.setItem('selectedDepartment', this.numLevel.toString());
        }
    }

    searchSaleInvoice(dateFromStr: string | null, dateToStr: string | null) {

        this.dteFrom = this.toDate(dateFromStr);
        this.dteTo = this.toDate(dateToStr);

        if (this.numLevel) {
            localStorage.setItem('selectedDepartment', this.numLevel);
        }
      /*  this.validateDates();*/

        if (this.areDatesValid && this.dateFrom && this.dateTo) {
            console.log("date from", this.dateFrom);
            console.log("date from", this.dateTo);
            this.saleOrderService
                .searchSaleInvoiceByDepartment(this.dteFrom, this.dteTo,this.numLevel)
                .subscribe((data: any) => {
                    if (data) {
                        this.items = data;
                        // @ts-ignore
                        /*this.items = data.filter(item => item.numLevel === 1 && item.txtLevel === 'First'  && (item.txtStatus1 === 'Pending' || item.txtStatus1 == 'Hold'));*/
                        this.items = this.items.map((item: any) => ({
                            ...item,
                            dteDate: item.dteDate ? this.formatDate(new Date(item.dteDate)) : null,
                            txtDealNo:item.slsTblDeal.txtDealNo ? item.slsTblDeal.txtDealNo : null,
                            lastApprovedName:this.getLastApprovedByName(item)
                        }));
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');

                    }
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
                    const storedValue = localStorage.getItem('selectedDepartment');
                    if (storedValue) {
                        this.numLevel = storedValue ? Number(storedValue) : this.uniqueDepartments[0];
                    } else {
                        this.numLevel = ''; // Default value if nothing is stored
                    }
                    // @ts-ignore
                  /*  this.isMarketingHead = user.cfgTblRole?.txtRoleName === 'MARKETING_HEAD';
                    console.log(this.items)*/
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

                const filterCriteria = 'Marketing Approval'; // Change this to your actual filter criteria
                this.menus = this.menus.filter(menu =>
                    menu.cfgTblSubMenu.txtSubMenuName.includes(filterCriteria)
                );
                console.log("Marketing Approval",this.menus);


                if (this.menus.length > 0) {

                    // @ts-ignore

                    this.hasMarketingApprovalView = this.menus[0].blIsview;
                    this.hasMarketingApprovalAdd = this.menus[0].blIsAdd;
                    this.hasMarketingApprovalEdit = this.menus[0].blIsUpdate;
                //    return;
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

    documentName: string = '';

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
                // @ts-ignore
                /*this.uploadedDocuments.dteCreateddate = this.formatDateLocale(new Date(this.uploadedDocuments[0].dteCreateddate))*/
                console.log('Documents:', this.uploadedDocuments[0].dteCreateddate);
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
                // Create a new PDF document
                // @ts-ignore
                const pdf = new jsPDF();

                // Add the downloaded content to the PDF (if applicable)
                // Here you can add the text or other elements based on the downloaded data
                pdf.text('Your PDF content here', 10, 10); // Add your document content as needed

                // Load the footer image
                const footerImage = new Image();
                footerImage.src = 'path/to/your/footer-image.png'; // Update with your image path

                footerImage.onload = () => {
                    const imgWidth = 40; // Adjust width
                    const imgHeight = 20; // Adjust height
                    const xPosition = (pdf.internal.pageSize.width - imgWidth) / 2; // Center the image
                    const yPosition = pdf.internal.pageSize.height - imgHeight - 10; // Position it above the bottom

                    // Add the footer image to the PDF
                    pdf.addImage(footerImage, 'PNG', xPosition, yPosition, imgWidth, imgHeight);

                    // Save or open the PDF
                    const pdfBlob = pdf.output('blob');
                    const objectUrl = window.URL.createObjectURL(pdfBlob);
                    window.open(objectUrl, '_blank');

                };
            },
            (error) => {
                this.notificationService.showMessage('Unable to view Document...', 'danger');
            }
        );
    }

    /*downloadDocument(documentId: string) {
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
    }*/

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


    // @ts-ignore
    // @ts-ignore
    // @ts-ignore
    // @ts-ignore
    public  getLastApprovedByName(item : any) {
        if (item.serApprovedbyName6 != null) return item.serApprovedbyName6;
        if (item.serApprovedbyName5 != null) return item.serApprovedbyName5;
        if (item.serApprovedbyName4 != null) return item.serApprovedbyName4;
        if (item.serApprovedbyName3 != null) return item.serApprovedbyName3;
        if (item.serApprovedbyName2 != null) return item.serApprovedbyName2;
        return item.serApprovedbyName1;
    }


    get uniqueDepartments() {
        return [
            { id: 1, name: 'Marketing' },
            { id: 2, name: 'Procurement' },
            { id: 3, name: 'Tax' },
            { id: 4, name: 'Finance' },
            { id: 5, name: 'Audit' },
            { id: 6, name: 'Payment' }
        ];
    }

    // Method to filter items
    get filteredItems() {
        return this.items.filter((item: { name: string; department: string; }) => {
            const matchesSearch = item.name.toLowerCase().includes(this.search.toLowerCase());
            const matchesDepartment = this.departmentFilter ? item.department === this.departmentFilter : true;
            return matchesSearch && matchesDepartment;
        });
    }

}
