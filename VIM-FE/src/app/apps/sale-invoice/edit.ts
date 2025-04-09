import {Component, ElementRef, ViewChild} from '@angular/core';
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {ActivatedRoute, Router} from "@angular/router";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {urls} from "../../utils/urls";
import {NotificationService} from "../../NotificationService";
import {Location} from "@angular/common";
import flatpickr from "flatpickr";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";

@Component({
    moduleId: module.id,
    templateUrl: './edit.html',
})
export class SaleInvoiceEditComponent {
    selectedFile = null;
    subtotal: number = 0;
    taxRate: number = 0;
    shippingRate: number = 0;
    discountRate: number = 0;
    total: number = 0;
    params = {
        title: '',
        invoiceNo: '',
        to: {
            name: 'Jesse Cory',
            email: 'redq@company.com',
            address: '405 Mulberry Rd. Mc Grady, NC, 28649',
            phone: '(128) 666 070',
        },

        invoiceDate: '',
        dueDate: '',
        bankInfo: {
            no: '1234567890',
            name: 'Bank of America',
            swiftCode: 'VS70134',
            country: 'United States',
            ibanNo: 'K456G',
        },
        notes: '',
    };

    serDealId: string | undefined;
    saleOrderLst: any[] = [];
    items: any[] = [];
    saleOrderDetails :any[] =[];
    slsTblDeal: any;
    uploadedDocuments: any[]=[];

    newDocument: { documentName: string } = { documentName: '' };

    @ViewChild('timePicker') timePicker!: ElementRef;
    selectedTime: string | null = null;

    form: FormGroup;
    isSubmit = false;
    private userSetTotalCost: any;

    constructor(private saleOrderService: SaleOrderService, private route: ActivatedRoute,private fb: FormBuilder,
        private router: Router,
        private http: HttpClient,private notificationService: NotificationService,private location: Location) {

        this.form = this.fb.group({
            invoiceDate: ['', Validators.required],
            invoiceNo: ['', Validators.required],
            spot: ['', Validators.required],
            time:['',Validators.required]
        });

    }

    ngOnInit() {

        this.route.paramMap.subscribe(params => {
            this.serDealId = params.get('serDealId') || '';
            this.getSaleOrderDetails(this.serDealId);
        });
    }

    ngAfterViewInit() {
        flatpickr(this.timePicker.nativeElement, {
            noCalendar: true,
            enableTime: true,
            dateFormat: 'H:i',
            onChange: (selectedDates, dateStr) => {
                this.selectedTime = dateStr;
            },
        });
    }

    // @ts-ignore
    getSaleOrderDetails(id) {
        this.saleOrderService.getSaleOrderDetail(id).subscribe((data: any) => {
            console.log('Raw Data:', data);
            if (data) {
                this.items = [];
                let slsTblDeal;
                this.saleOrderLst = data;
                data.forEach((item: any) => {
                    slsTblDeal = item.slsTblDeal;
                    const processedItem = {
                        slsTblDealsDetails:item,
                        serDealDetailId:item.serDealDetailId,
                        txtSapCode: this.removeExtraZeros(item.cfgTblProduct?.txtSapCode ?? 'N/A'),
                        txtProductName: item.cfgTblProduct?.txtProductName ?? 'N/A',
                        numQuantity: 0,
                        numItemPrice: item.numItemPrice ?? 0,
                        sapLineItem:item.sapLineItem ?? 'N/A',
                        numBalance: item.numBalance,
                        serDealId:item.serDealId,
                        numTotalPrice: (item.numQuantity ?? 0) * (item.numItemPrice ?? 0),
                        numAmount : item.numAmount
                    };
                    this.items.push(processedItem);
                });

                console.log('Processed Items:', this.items);
                console.log('Extracted slsTblDeal:', slsTblDeal);
                this.slsTblDeal = slsTblDeal;
                this.calculateTotalsSaved();
            } else {
                console.error('No data received');
            }
        });
    }

    /*uploadedFiles: File[] = [];*/
    uploadedFiles: any[] = [];
    myFile: File | null = null;

    handleFileInput(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            this.myFile = input.files[0];
        }
    }



    removeFile(file: { documentName: string; size: number; timestamp: Date; documentType: any; originalName: any; file: any }) {
        // @ts-ignore
        this.uploadedDocuments = this.uploadedDocuments.filter(f => f !== file);
    }

    columns = [
        {
            key: 'txtSapCode',
            label: 'Service Number',
        },
        {
            key: 'txtProductName',
            label: 'Product Name',
        },
        {
            key: 'numQuantity',
            label: 'QTY',
        },
        {
            key: 'numItemPrice',
            label: 'Price',
            class: 'ltr:text-right rtl:text-left',
        },
        {
            key: 'numTotalPrice',
            label: 'AMOUNT',
            class: 'ltr:text-right rtl:text-left',
        },
    ];

    updateSaleOrders() {

        const quantityMap = new Map<number, number>();
        const costMap = new Map<number, number>();

        this.items = this.items.filter(item => item.numQuantity > 0);
        console.log(this.items);
        this.items.forEach(item => {
            console.log(item.numQuantity);
            if (item.numQuantity > 0 && item.serDealDetailId) {
                if(!item.totalCost || item.totalCost <= 0) {
                    item.numQuantity = 0.0;
                    quantityMap.set(item.serDealDetailId, item.numQuantity);

                }else{
                    /*item.numQuantity = 0.0;*/
                    quantityMap.set(item.serDealDetailId, item.numQuantity);
                }
            }
        });
        this.items.forEach(item => {
           /* console.log(item.numQuantity);*/
            if (item.numAmount > 0 && item.serDealDetailId) {
                costMap.set(item.serDealDetailId, item.numAmount);
            }
        });


        this.saleOrderLst = this.saleOrderLst.filter(order => {
            if (quantityMap.has(order.serDealDetailId)) {
                const matchedItem = this.items.find(item =>
                    item.slsTblDealsDetails.serDealDetailId === order.serDealDetailId
                );

                if (matchedItem) {
                    matchedItem.slsTblDealsDetails.numAmount = matchedItem.numAmount;
                    order.slsTblDealsDetails = { ...matchedItem.slsTblDealsDetails };
                    order.sapLineItem = `${matchedItem.slsTblDealsDetails.sapLineItem}-${matchedItem.serDealDetailId}`;
                    order.numQuantity = quantityMap.get(order.serDealDetailId) || 0;

                    return true;
                } else {

                    return false;
                }
            }

            return false;
        });

        /*this.saleOrderLst.forEach(order => {
            if (quantityMap.has(order.serDealDetailId)) {
                debugger;
                let matchedItem = this.items.find(item => item.slsTblDealsDetails.serDealDetailId === order.serDealDetailId);

                if (matchedItem) {
                    debugger;
                    matchedItem.slsTblDealsDetails.numAmount = matchedItem.numAmount;
                    order.slsTblDealsDetails = { ...matchedItem.slsTblDealsDetails };
                    order.sapLineItem = `${matchedItem.slsTblDealsDetails.sapLineItem}-${matchedItem.serDealDetailId}`;
                }

                order.numQuantity = quantityMap.get(order.serDealDetailId) || 0;
            }
        });*/

    }

    handleFileUpload(event: Event) {

        const input = event.target as HTMLInputElement;
        const allowedFormats = ['application/pdf', 'image/png', 'image/jpeg', 'image/webp', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel', 'application/vnd.ms-powerpoint', 'application/msword', 'application/vnd.ms-office'];

        if (input.files && input.files.length > 0) {
            const file = input.files[0];
            if (allowedFormats.includes(file.type)) {
                this.myFile = file;
                console.log('File uploaded successfully:', file.name);
            } else {
                this.notificationService.showMessage('Invalid file format. Please upload a file in one of the following formats: MS Office, PNG, JPEG, WEBP, PDF.','danger');
                return;
            }
        }

       /* const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            this.myFile = input.files[0];
        }*/

    }

    uploadDocument(): void {
        if (this.myFile && this.newDocument.documentName) {
            if (this.myFile.size > 35 * 1024 * 1024) {
               /* alert('File size exceeds the limit of 2MB.');*/
                this.notificationService.showMessage("File size exceeds the limit of 35MB.",'danger')
                return;
            }

            // @ts-ignore
            const newDocument = {
                file: this.myFile,
                documentName: this.newDocument.documentName,
                size: this.myFile.size,
                documentType: this.myFile.type,
                originalName: this.myFile.name,
                timestamp: new Date().toISOString(), // Get the current timestamp in ISO format
            };

            this.uploadedDocuments.push(newDocument);

            // Clear the form fields
            this.newDocument.documentName = '';
            this.myFile = null;

            // Display success message (you can replace this with Angular Material Snackbar or similar)
           // alert('Document added to the list successfully.');
            this.notificationService.showMessage("Document added to the list successfully",'success')
        } else {
           // alert('Please select a file and enter a document name.');
            this.notificationService.showMessage("Please select a file and enter a document name.",'danger')
        }
    }

    isInvalidTime() {
        return !this.params.title;
    }

    isInvalidInvoiceDate() {
        return !this.params.invoiceDate;
    }

    isInvalidInvoiceNo() {
        return !this.params.invoiceNo;
    }

    isInvalidSpot() {
        return !this.params.notes;
    }

    addItem() {

            if (!this.params.title || !this.params.invoiceDate || !this.params.invoiceNo || !this.params.notes) {
                this.notificationService.showMessage('Please fill in all required fields.', 'danger');
                return;
            }

            const totalsCalculated = this.calculateTotalsSaved();
            if (!totalsCalculated) {
                let warningMessage = '';
                warningMessage = 'Warning: Total cost exceeds the remaining balance for some items.';
                this.notificationService.showMessage(warningMessage, 'danger');
                return;
            }

            this.updateSaleOrders();
            const formData = new FormData();
            // @ts-ignore
            if (this.saleOrderLst && this.saleOrderLst.length == 0) {
                this.notificationService.showMessage('No sale orders to process.', 'warning');
                return;
            }
            const payload = {
                //   so:{"numDiscount":0,"numFED":0,"numAmountAfterFED":0,"numCVT":0,"numFreight":0,"numTaxOnFreight":0,"numAvanceTax":0,"numFabrication":0,"blIsGAL":"true","dteDate":"2024-09-16","txtSaleOrderNo":"PSO-001","numDiscountAmount":0,"slsTblDeal":{"serDealId":62,"blIsDeleted":false,"blnDealCompletionStatus":null,"blnIsApproved":null,"blnIsCompleted":false,"dteCreateddate":1725864219828,"dteDate":1631127600000,"dteStartDate":1631127600000,"dteEndDate":null,"dteDueDate":null,"dteModifieddate":null,"numExciseDuty":null,"numNetAmount":null,"priority":null,"serApprovedbyId":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"serPreparedbyId":null,"txtBillingAddress":null,"txtDealer":null,"txtCustomer":null,"txtProduct":null,"txtDescription":null,"txtDestination":null,"txtMachineIp":null,"txtPaymnetTerms":null,"txtPriceTerms":null,"txtReceiveStatus":null,"txtDealNo":"4400009471","txtDealName":null,"txtShipBy":null,"txtShippingAddress1":null,"txtShippingAddress2":null,"txtShippingAddress3":null,"txtDeliveryTime":null,"txtVehicleType":null,"dteRSMApproval":null,"dteFinalApproval":null,"numQuantity":1,"numPrice":null,"numTotalPrice":null,"cfgTblProduct":{"serProductId":4321,"blIsDeleted":false,"blIsProduction":null,"blnIsInventoryItem":null,"blnIsPurchaseItem":null,"blnIsSaleItem":null,"blnIsKichenItem":null,"blnIsShopItem":null,"blnIsTangible":null,"blnStatus":true,"blIspacking":null,"blIsSet":null,"blIsComponent":null,"blIsImport":null,"dteCreateddate":1725864208266,"dteModifieddate":null,"numDiscount":null,"numMarketRetailPrice":null,"numPiecesInMasterPack":null,"numProductWeight":null,"numSalePrice":null,"numTradePrice":null,"numUnitPrice":null,"numUnitsInMasterPack":null,"numInnersInMasterPack":null,"picProductImage":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtDescription":null,"txtMachineIp":null,"txtMasterPack":null,"txtPriceUnit":null,"txtProductCode":"000000000000900016","txtProductName":"WASHING SERVICES (LABCOAT, SHE","txtQuality":null,"blIsTruck":null,"blIsBus":null,"blIsPickup":null,"txtType":null,"txtSapCode":"000000000000900016","numEngineCC":null,"cfgTblBrand":null,"cfgTblProductCategory":null,"cfgTblUom":null,"txtVariant":null,"txtTransmission":null,"txtColor":null,"txtInteriorColor":null,"numOldPrice":null,"numSalesTax":null,"numFED":null,"numCVT":null,"numNonfilerAmount":null,"numfilerAmount":null,"numAmount":null,"txtSKU":null,"slsTblSoDetails":null},"blnFromSAP":true,"blnIsIncoTerm":null,"numDiscount":null,"numDiscountAmount":null,"numAmountAfterDiscount":null,"numFED":null,"numFEDAmount":null,"numAmountAfterFED":null,"numSalesTax":0,"numSalesTaxAmount":null,"numAmountAfterST":null,"numCVT":null,"numCVTAmount":null,"numAmountAfterCVT":null,"numFreight":null,"numTaxOnFreight":null,"numTOFAmount":null,"numGrossValue":null,"numAvanceTax":null,"numTotal":null,"numAmount":null,"txtType":null,"blIsGAL":null,"txtSoapReturnType":null,"txtSoapResponseMsg":null,"txtStatus":null,"txtIssueCode":null,"dteIssuedate":null,"numAmountReceived":null,"numRemainingBalance":null,"cfgTblCity":null,"cfgTblCustomer":null,"slsTblDealDetails":null,"dte_date_from":"2021-09-09","dte_date_to":null,"blIsComplementry":null,"blIsSplit":null,"cfgTblDealer":{"serCustomerId":15132,"blIsDeleted":false,"blnIsFiler":null,"blnStatus":true,"dteCreateddate":1725864206875,"dteModifieddate":null,"numDiscount":null,"numExciseDuty":null,"numSalesTax":null,"numFED":null,"numFurtherTax":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtBillingAddress":null,"txtBusinessName":null,"txtCnicNo":null,"txtCustomerCode":"0006100277","txtInvoiceName":null,"txtCustomerName":"0006100277  (Mr. Sarwar Khan)","txtDisplayAddress":null,"txtEmailAddress":"","txtGstNameOnInvoice":null,"txtGstNumber":null,"txtIsFiler":null,"txtMachineIp":null,"txtMobileNo":null,"txtNtnNo":"","txtSTR":"","txtFTN":null,"txtProvince":null,"txtFName":null,"dteExpiryDate":null,"dteDOB":null,"txtExpiryDate":null,"txtPhoneNo":"","txtPhoneNo2":null,"blnCommercial":null,"blnPassanger":null,"blnIsGst":null,"txtShippingAddress":"","cfgTblCity":{"serCityId":40,"blIsDeleted":false,"blnStatus":true,"txtCityCode":"00040","txtCityName":"LAHORE","numFreight":null,"numFreight14":null,"numFreight20":null,"numFreight40":null,"cfgTblCountry":{"serCountryId":1,"blIsDeleted":false,"blnIsnational":null,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1658312034161,"serCreatedUserId":null,"serModifiedUserId":2,"serParentCountryId":null,"txtMachineIp":null,"txtName":"Pakistan","cfgTblCities":null,"cfgTblCustomers":null},"cfgTblCustomers":null,"cfgTblSuppliers":null,"slsTblSaleOrders":null},"cfgTblCountry":null,"cfgTblCustomerCategory":null,"slsTblSaleOrders":null,"cfgTblArea":null,"cfgTblRegion":null,"cfgTblZone1":null,"cfgTblZone2":null,"cfgTblZone3":null,"txtXMSent":null,"txtReturnMsg":null,"txtXMReceive":null,"blIsDealer":true,"cfgTblCustomer":null,"txtSapNo":"0006100277","txtUserName":null,"blIsGroup":null,"blIsAccountExist":null,"blIsLabsa":null,"cfgTblGroupCustomer":null,"hrTblEmployee":null,"blnIsExport":null,"txtDivision":null,"cfgTblIncoTerm":null,"cfgTblDivision":null,"txtDesignation":null,"txtHOD":null,"txtHODMobile":null,"txtHODLandLine":null,"txtHODEmailAddress":null,"txtErrorMsgFromSap":null,"blIsPOSTEDToSAP":null},"cfgTblDistributionChannel":{"serDistributionChannelId":2,"blIsDeleted":false,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1592979402102,"serCreatedUserId":null,"serModifiedUserId":1,"txtCode":"20","txtDescription":null,"txtMachineIp":null,"txtName":"Direct Sales"},"cfgTblDivision":null,"cfgTblDocumentType":{"serDocumentTypeId":2,"blIsDeleted":false,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1658309844308,"serCreatedUserId":null,"serModifiedUserId":2,"txtCode":"ZCSH","txtDescription":null,"txtMachineIp":null,"txtName":"Cash Sales"},"cfgTblIncoTerm":null,"cfgTblPaymentTerm":null,"cfgTblSalesOrganization":{"serSalesOrganizationId":1,"blIsDeleted":false,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":null,"serCreatedUserId":null,"serModifiedUserId":null,"txtCode":"1000","txtDescription":null,"txtMachineIp":null,"txtName":"ICL"},"hrTblEmployee":null,"txtPONo":null,"dtePODate":null,"txtSapNo":"4400009471","txtImage":null,"profile_pic":null,"txtImageName":null,"txtImageType":null,"txtDCStatus":null,"txtInvoiceStatus":null,"txtDCNo":null,"txtInvoiceNo":null,"txtDCDate":null,"txtInvoiceDate":null,"txtOrderapprovalDate":null,"txtDCQty":null,"numCommission":null,"numFabrication":null,"$$hashKey":"object:77"},"numGrossValue":9600,"numTotal":9600,"cfgTblDealerOne":{"serCustomerId":15132,"blIsDeleted":false,"blnIsFiler":null,"blnStatus":true,"dteCreateddate":1725864206875,"dteModifieddate":null,"numDiscount":null,"numExciseDuty":null,"numSalesTax":null,"numFED":null,"numFurtherTax":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtBillingAddress":null,"txtBusinessName":null,"txtCnicNo":null,"txtCustomerCode":"0006100277","txtInvoiceName":null,"txtCustomerName":"0006100277  (Mr. Sarwar Khan)","txtDisplayAddress":null,"txtEmailAddress":"","txtGstNameOnInvoice":null,"txtGstNumber":null,"txtIsFiler":null,"txtMachineIp":null,"txtMobileNo":null,"txtNtnNo":"","txtSTR":"","txtFTN":null,"txtProvince":null,"txtFName":null,"dteExpiryDate":null,"dteDOB":null,"txtExpiryDate":null,"txtPhoneNo":"","txtPhoneNo2":null,"blnCommercial":null,"blnPassanger":null,"blnIsGst":null,"txtShippingAddress":"","cfgTblCity":{"serCityId":40,"blIsDeleted":false,"blnStatus":true,"txtCityCode":"00040","txtCityName":"LAHORE","numFreight":null,"numFreight14":null,"numFreight20":null,"numFreight40":null,"cfgTblCountry":{"serCountryId":1,"blIsDeleted":false,"blnIsnational":null,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1658312034161,"serCreatedUserId":null,"serModifiedUserId":2,"serParentCountryId":null,"txtMachineIp":null,"txtName":"Pakistan","cfgTblCities":null,"cfgTblCustomers":null},"cfgTblCustomers":null,"cfgTblSuppliers":null,"slsTblSaleOrders":null},"cfgTblCountry":null,"cfgTblCustomerCategory":null,"slsTblSaleOrders":null,"cfgTblArea":null,"cfgTblRegion":null,"cfgTblZone1":null,"cfgTblZone2":null,"cfgTblZone3":null,"txtXMSent":null,"txtReturnMsg":null,"txtXMReceive":null,"blIsDealer":true,"cfgTblCustomer":null,"txtSapNo":"0006100277","txtUserName":null,"blIsGroup":null,"blIsAccountExist":null,"blIsLabsa":null,"cfgTblGroupCustomer":null,"hrTblEmployee":null,"blnIsExport":null,"txtDivision":null,"cfgTblIncoTerm":null,"cfgTblDivision":null,"txtDesignation":null,"txtHOD":null,"txtHODMobile":null,"txtHODLandLine":null,"txtHODEmailAddress":null,"txtErrorMsgFromSap":null,"blIsPOSTEDToSAP":null},"cfgTblCustomer":null,"numSalesTax":0,"slsTblSoDetails":[{"cfgTblProduct":{"serProductId":4321,"blIsDeleted":false,"blIsProduction":null,"blnIsInventoryItem":null,"blnIsPurchaseItem":null,"blnIsSaleItem":null,"blnIsKichenItem":null,"blnIsShopItem":null,"blnIsTangible":null,"blnStatus":true,"blIspacking":null,"blIsSet":null,"blIsComponent":null,"blIsImport":null,"dteCreateddate":1725864208266,"dteModifieddate":null,"numDiscount":null,"numMarketRetailPrice":null,"numPiecesInMasterPack":null,"numProductWeight":null,"numSalePrice":null,"numTradePrice":null,"numUnitPrice":null,"numUnitsInMasterPack":null,"numInnersInMasterPack":null,"picProductImage":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtDescription":null,"txtMachineIp":null,"txtMasterPack":null,"txtPriceUnit":null,"txtProductCode":"000000000000900016","txtProductName":"WASHING SERVICES (LABCOAT, SHE","txtQuality":null,"blIsTruck":null,"blIsBus":null,"blIsPickup":null,"txtType":null,"txtSapCode":"000000000000900016","numEngineCC":null,"cfgTblBrand":null,"cfgTblProductCategory":null,"cfgTblUom":null,"txtVariant":null,"txtTransmission":null,"txtColor":null,"txtInteriorColor":null,"numOldPrice":null,"numSalesTax":null,"numFED":null,"numCVT":null,"numNonfilerAmount":null,"numfilerAmount":null,"numAmount":null,"txtSKU":null,"slsTblSoDetails":null},"numQuantity":"1","numItemPrice":8820,"numBalance":8820,"slsTblDealDetails":{"serDealDetailId":74},"sapLineItem":"00020","sapLineItemDescription":"LAB COAT WHITE","numTotalPrice":8820,"$$hashKey":"object:101","blIsDeleted":true,"isValid":true,"numUnitWt":null},{"cfgTblProduct":{"serProductId":4321,"blIsDeleted":false,"blIsProduction":null,"blnIsInventoryItem":null,"blnIsPurchaseItem":null,"blnIsSaleItem":null,"blnIsKichenItem":null,"blnIsShopItem":null,"blnIsTangible":null,"blnStatus":true,"blIspacking":null,"blIsSet":null,"blIsComponent":null,"blIsImport":null,"dteCreateddate":1725864208266,"dteModifieddate":null,"numDiscount":null,"numMarketRetailPrice":null,"numPiecesInMasterPack":null,"numProductWeight":null,"numSalePrice":null,"numTradePrice":null,"numUnitPrice":null,"numUnitsInMasterPack":null,"numInnersInMasterPack":null,"picProductImage":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtDescription":null,"txtMachineIp":null,"txtMasterPack":null,"txtPriceUnit":null,"txtProductCode":"000000000000900016","txtProductName":"WASHING SERVICES (LABCOAT, SHE","txtQuality":null,"blIsTruck":null,"blIsBus":null,"blIsPickup":null,"txtType":null,"txtSapCode":"000000000000900016","numEngineCC":null,"cfgTblBrand":null,"cfgTblProductCategory":null,"cfgTblUom":null,"txtVariant":null,"txtTransmission":null,"txtColor":null,"txtInteriorColor":null,"numOldPrice":null,"numSalesTax":null,"numFED":null,"numCVT":null,"numNonfilerAmount":null,"numfilerAmount":null,"numAmount":null,"txtSKU":null,"slsTblSoDetails":null},"numQuantity":"1","numItemPrice":780,"numBalance":780,"slsTblDealDetails":{"serDealDetailId":75},"sapLineItem":"00010","sapLineItemDescription":"LAB COAT BLUE","numTotalPrice":780,"$$hashKey":"object:102","blIsDeleted":true,"isValid":true,"numUnitWt":null}],"numNetAmount":9600,"numExciseDuty":0,"numAmount":9600,"numRemainingBalance":9600,"numAmountReceived":0}
                so: {
                    "numDiscount": 0,
                    "numFED": 0,
                    "numAmountAfterFED": 0,
                    "numCVT": 0,
                    "numFreight": 0,
                    "numTaxOnFreight": 0,
                    "numAvanceTax": 0,
                    "numFabrication": 0,
                    "blIsGAL": "true",
                    "dteDate": this.slsTblDeal.dte_date_from,
                    "txtSaleOrderNo": this.slsTblDeal.txtSaleOrderNo,
                    "numDiscountAmount": 0,
                    "slsTblDeal": this.slsTblDeal,
                    "numGrossValue": 0,
                    "numTotal": 0,
                    "cfgTblDealerOne": this.slsTblDeal.cfgTblDealerOne,
                    "cfgTblCustomer": null,
                    "numSalesTax": this.slsTblDeal.numSalesTax,
                    "slsTblSoDetails": this.saleOrderLst,
                    "numNetAmount": 0,
                    "numExciseDuty": 0,
                    "numAmount": this.total,
                    "numRemainingBalance": 0,
                    "numAmountReceived": 0,
                    "fbrinvoiceDate": this.params.invoiceDate,
                    "timeString": this.params.title,
                    "notes": this.params.notes,
                    "fbrinvoiceno": this.params.invoiceNo
                },
                newDocuments: this.uploadedDocuments,

            };

            // @ts-ignore
            if (payload.newDocuments.length <= 0) {

                this.notificationService.showMessage("Please upload the required file to proceed.", 'danger');
                return;
            }
            formData.append('newDocuments', JSON.stringify(payload.newDocuments));
            this.uploadedDocuments.forEach((doc: any) => {
                formData.append('file', doc.file); // Use the file object directly
            });
            /*if (this.uploadedDocuments && this.uploadedDocuments.length > 0) {
                // @ts-ignore
                this.uploadedDocuments.forEach((file: File) => {
                    formData.append("file", file);
                });
            }*/

            // @ts-ignore
            // formData.append("file", this.uploadedDocuments);
            // @ts-ignore
            formData.append('so', JSON.stringify(payload.so))
            this.http.post(urls.API_URL + 'addNewSaleOrderNew', formData, {
            observe: 'response'
            }).subscribe(
            (response: any) => {

                if (response.status === 200 && response.body.status === 'Success') {
                    this.notificationService.showMessage('Record saved successfully', 'success');
                    this.router.navigateByUrl('sale-invoice');
                } else {
                    this.notificationService.showMessage('Error occurred while saving', 'danger');
                }
            },
            error => {
                console.error('Error:', error);
                if (error.status === 200) {
                    // If status is 200 but it's treated as an error due to the body content
                    this.notificationService.showMessage('Unexpected response: ' + error.error, 'warning');
                } else {
                    this.notificationService.showMessage('Error occurred while saving', 'danger');
                }
            }
        );
            /*this.http.post(urls.API_URL + 'addNewSaleOrderNew', formData, {
                observe: 'response'
            }).subscribe(
                (response: any) => {
                    debugger;
                    if (response.body === 'Success') {
                        this.notificationService.showMessage('Record saved successfully', 'success');
                        this.router.navigateByUrl('sale-invoice');
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');
                    }
                    this.ngOnInit();
                },
                error => {
                    console.error('Error:', error);
                    this.notificationService.showMessage('Error occurred while saving', 'danger');
                    this.ngOnInit();
                }
            );*/
       // }

    }

    removeItem(item: any = null) {
        this.items = this.items.filter((d: any) => d.id != item.id);
    }

    calculateTotalsSaved() {
        /*debugger;
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {

            if (!item.totalCost || item.totalCost <= 0) {
                return;
            }

            if (item.numItemPrice > 0) {
                if (!item.userSetTotalCost) {
                    item.numQuantity = Math.min(item.totalCost / item.numItemPrice, 1);
                    item.numQuantity = parseFloat(item.numQuantity.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            } else {
                item.numQuantity = 0;
                item.numAmount = 0;
            }

            if (item.numQuantity === 0) {
                if (item.numBalance === 0) {
                    warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                    isSuccess = false;
                    item.totalCost = 0;
                    return;
                } else {
                    item.totalCost = 0;
                    return;
                }
            }

            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for this item.';
                item.totalCost = item.numBalance; // Cap totalCost to the balance
                isSuccess = false;
            } else {
                if (!item.userSetTotalCost) {
                    item.totalCost = parseFloat(item.totalCost.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            }

            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
            }
            grandTotal += item.totalCost;
        });
        this.total = grandTotal;

        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;*/
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {

            if (!item.numQuantity || item.numQuantity <= 0) {
                item.numQuantity = 0.0;
                item.totalCost = 0.0;
                return;
            }

            if (item.numItemPrice > 0) {
                if (!item.userSetTotalCost) {

                    item.totalCost = item.numQuantity * item.numItemPrice;
                    item.totalCost = parseFloat(item.totalCost.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            } else {
                // If numItemPrice is zero or invalid, reset quantity and amount
                item.numQuantity = 0;
                item.numAmount = 0;
                item.totalCost = 0;
            }

            if (item.numQuantity === 0) {
                if (item.numBalance === 0) {
                    warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                    isSuccess = false;
                    item.totalCost = 0;
                    return;
                } else {
                    item.totalCost = 0;
                    return;
                }
            }

            if (item.totalCost > item.numBalance) {
           //     warningMessage = 'Warning: Total cost exceeds the remaining balance for this item.';
                item.totalCost = item.numBalance;
                isSuccess = false;
            }


            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
            }

            // Add this item's totalCost to the grand total
            grandTotal += item.totalCost;
        });

        // Set the grand total after processing all items
        this.total = grandTotal;

        // Show any warning messages
        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;

    }

    calculateTotals() {
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {
            // If numQuantity is zero or invalid, set it to zero and skip further processing
            if (!item.numQuantity || item.numQuantity <= 0) {
                item.numQuantity = 0.0;
                item.totalCost = 0.0; // Also set totalCost to 0 when quantity is invalid
                item.numAmount = 0; // Ensure numAmount is also reset
                return;
            }

            // Calculate totalCost based on numQuantity and numItemPrice
            if (item.numItemPrice > 0) {
                if (!item.userSetTotalCost) {
                    // Calculate totalCost as the product of numQuantity and numItemPrice
                    item.totalCost = item.numQuantity * item.numItemPrice;
                    item.totalCost = parseFloat(item.totalCost.toFixed(3)); // Round to 3 decimal places
                    item.numAmount = item.totalCost; // Set numAmount to the totalCost value
                }
            } else {
                // If numItemPrice is zero or invalid, reset quantity and amount
                item.numQuantity = 0;
                item.numAmount = 0;
                item.totalCost = 0;
            }

            if (item.totalCost > item.numBalance) {
             //   warningMessage = 'Warning: Total cost exceeds the remaining balance for this item. Adjusting to available balance.';
                // Adjust numQuantity and totalCost to not exceed numBalance
                const adjustedQuantity = item.numBalance / item.numItemPrice;
                item.numQuantity = adjustedQuantity;
                item.totalCost = item.numBalance; // Cap totalCost to numBalance
                item.numAmount = item.totalCost; // Ensure numAmount reflects the totalCost
                isSuccess = false; // Indicate that the adjustment was made
            }

            // If totalCost is greater than item price, reset totalCost to 0
            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
                item.numQuantity = 0;
                item.numAmount = 0;
            }

            // Add this item's totalCost to the grand total
            grandTotal += item.totalCost;
        });

        // Set the grand total after processing all items
        this.total = grandTotal;

        // Show any warning messages
        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;
    }
    /*calculateTotals() {
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {
            // If numQuantity is zero or invalid, set it to zero and skip further processing
            if (!item.numQuantity || item.numQuantity <= 0) {
                item.numQuantity = 0.0;
                item.totalCost = 0.0; // Also set totalCost to 0 when quantity is invalid
                return;
            }

            // Calculate totalCost based on numQuantity as a percentage of numItemPrice
            if (item.numItemPrice > 0) {
                if (!item.userSetTotalCost) {
                    // Calculate totalCost as percentage of numItemPrice
                    item.totalCost = item.numQuantity * item.numItemPrice; // numQuantity is treated as a percentage here
                    item.totalCost = parseFloat(item.totalCost.toFixed(3)); // Round to 3 decimal places
                    item.numAmount = item.totalCost;
                }
            } else {
                // If numItemPrice is zero or invalid, reset quantity and amount
                item.numQuantity = 0;
                item.numAmount = 0;
                item.totalCost = 0;
            }

            // Ensure item.totalCost doesn't exceed numBalance
            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for this item.';
                item.numQuantity = 0;
                item.totalCost = item.numBalance;  // Cap totalCost to the balance
                isSuccess = false;
            }

            // If totalCost is greater than item price, reset totalCost to 0
            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
                item.numQuantity = 0;
            }

            // Add this item's totalCost to the grand total
            grandTotal += item.totalCost;
        });

        // Set the grand total after processing all items
        this.total = grandTotal;

        // Show any warning messages
        if (warningMessage) {

            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;
    }*/

    onTotalCostChange(item: any) {
        item.userSetTotalCost = true;
        if (item.numItemPrice > 0 && item.numQuantity > 0) {
            item.totalCost = item.numQuantity * item.numItemPrice;
            item.totalCost = parseFloat(item.totalCost.toFixed(3));
            item.numAmount = item.totalCost;
        }
        this.calculateTotals();
    }

    allowThreeDecimals(event: Event, item: any): void {
        const inputElement = event.target as HTMLInputElement;
        let value = inputElement.value;

        if (!value) {
            item.numQuantity = 0;
            inputElement.value = '0';
            return;
        }

        const regex = /^(1|0(\.\d{0,3})?)$/;
        if (!regex.test(value)) {
            if (value.startsWith('0.') && value.length > 2) {
                const parts = value.split('.');
                if (parts.length > 1 && parts[1].length > 3) {
                    value = parts[0] + '.' + parts[1].substring(0, 3);
                }
            } else {

                return;
            }
        } else {

            const parts = value.split('.');
            if (parts.length > 1 && parts[1].length > 3) {
                value = parts[0] + '.' + parts[1].substring(0, 3);
            }
        }

        inputElement.value = value;
        item.numQuantity = parseFloat(value); // Update the model
    }
    /*onTotalCostChange(item: any) {
        // Mark the user as having manually set the total cost
        item.userSetTotalCost = true;

        // Recalculate totals when totalCost changes
        this.calculateTotals();
    }*/

       /* debugger;
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {

            if (item.numItemPrice > 0) {

                if (!item.userSetTotalCost) {
                    item.numQuantity = Math.min(item.totalCost / item.numItemPrice, 1);
                    item.numQuantity = parseFloat(item.numQuantity.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            } else {

                item.numQuantity = 0;
            }


            if (item.numQuantity === 0) {
                if (item.numBalance === 0) {
                    warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                    isSuccess = false;
                    item.totalCost = 0;
                    return;
                } else {
                    item.totalCost = 0;
                    return;
                }
            }


            // const itemTotalCost = item.totalCost * item.numQuantity;
            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for this item.';
                item.totalCost = item.numBalance; // Cap totalCost to the balance
                isSuccess = false;
            } else {

                if (!item.userSetTotalCost) {
                    item.totalCost = parseFloat(item.totalCost.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            }

            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
            }

            grandTotal += item.totalCost;
        });

        this.total = grandTotal;


        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;*/


    /*calculateTotals() {
       /!* debugger;
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {
            if (item.numItemPrice > 0) {
                if (!item.userSetTotalCost) {
                    item.numQuantity = Math.min(item.totalCost / item.numItemPrice, 1);
                    item.numQuantity = parseFloat(item.numQuantity.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            } else {
                item.numQuantity = 0;
            }

            if (item.numQuantity === 0) {
                if (item.numBalance === 0) {
                    warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                    isSuccess = false;
                    item.totalCost = 0;
                    return;
                } else {
                    item.totalCost = 0;
                    return;
                }
            }

           // const itemTotalCost = item.totalCost * item.numQuantity;

            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for this item.';
                item.totalCost = item.numBalance; // Cap totalCost to the balance
                isSuccess = false;
            } else {

                if (!item.userSetTotalCost) {
                    item.totalCost = parseFloat(item.totalCost.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            }


            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
            }


            grandTotal += item.totalCost;
        });

        this.total = grandTotal;


        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;*!/

        debugger;
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {
            if (!item.numQuantity || item.numQuantity <= 0) {
                item.numQuantity = 0.0
                return;
            }

            if (item.numItemPrice > 0) {
                if (!item.userSetTotalCost) {
                    item.numQuantity = Math.min(item.totalCost / item.numItemPrice, 1);
                    item.numQuantity = parseFloat(item.numQuantity.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            } else {
                item.numQuantity = 0;
                item.numAmount = 0;
            }

            if (item.numQuantity === 0) {
                if (item.numBalance === 0) {
                    warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                    isSuccess = false;
                    item.totalCost = 0;
                    return;
                } else {
                   // item.totalCost = 0;
                    return;
                }
            }

            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for this item.';
                item.totalCost = item.numBalance; // Cap totalCost to the balance
                isSuccess = false;
            } else {
                if (!item.userSetTotalCost) {
                    item.totalCost = parseFloat(item.totalCost.toFixed(3));
                    item.numAmount = item.totalCost;
                }
            }

            if (item.totalCost > item.numItemPrice) {
                item.totalCost = 0;
            }

            grandTotal += item.totalCost;
        });

        this.total = grandTotal;

        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;

    }

    onTotalCostChange(item: any) {
        item.userSetTotalCost = false;
        this.calculateTotals();
    }*/

    /*calculateTotals() {

        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        debugger;
        this.items.forEach(item => {

            if (item.numItemPrice > 0) {

                item.numQuantity = Math.min(item.totalCost / item.numItemPrice, 1);
            } else {
                item.numQuantity = 0;
            }

            const itemTotalCost = item.numItemPrice * item.numQuantity;
            const percentageOfBalance = (item.numBalance > 0) ? (item.numQuantity / item.numBalance) * 100 : 0;

            // Validation checks
            if (item.numQuantity === 0 && item.numBalance === 0) {
                warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                isSuccess = false;
                return;
            }


            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for some items.';
                item.totalCost = item.numItemPrice;
                isSuccess = false;
            } else {
                item.totalCost = itemTotalCost;
            }


            if (item.totalCost > item.numItemPrice) {
                item.totalCost = item.numItemPrice;
            }

            // Ensure totalCost is at least zero
          //  item.totalCost = Math.max(item.totalCost, 0);

            item.percentageOfBalance = percentageOfBalance;
            grandTotal += item.totalCost;
        });

        this.total = grandTotal;

        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess; // Return the success status
    }*/



    /*calculateTotals() {
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {
            // Calculate quantity based on value and item price
            if (item.numItemPrice > 0) {
                // Calculate quantity as value divided by item price, ensuring it's capped at 1
                item.numQuantity = Math.min(item.totalCost / item.numItemPrice , 1);
            } else {
                item.numQuantity = 0; // Avoid division by zero
            }

            const itemTotalCost = item.numItemPrice * item.numQuantity; // Total cost based on calculated quantity
            const percentageOfBalance = (item.numBalance > 0) ? (item.numQuantity / item.numBalance) * 100 : 0;

            // Validation checks
            if (item.numQuantity === 0 && item.numBalance === 0) {
                warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                isSuccess = false;
                return;
            }

            if (item.totalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for some items.';
                item.totalCost = item.numBalance; // Cap the total cost to the balance
                isSuccess = false;
            } else {
                item.totalCost = itemTotalCost; // Set the total cost to calculated cost
            }

            item.percentageOfBalance = percentageOfBalance; // Update the percentage of balance
            grandTotal += item.totalCost; // Accumulate the grand total
        });

        this.total = grandTotal; // Set the grand total

        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger'); // Show any warning messages
        }

        return isSuccess; // Return the success status
    }*/



    /*calculateTotals() {
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;

        this.items.forEach(item => {
            const itemTotalCost = item.numItemPrice * item.numBalance; // Adjusted calculation
            const percentageOfBalance = (item.numBalance > 0) ? (item.numQuantity / item.numBalance) * 100 : 0;

            if (item.numQuantity === 0 && item.numBalance === 0) {
                warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                isSuccess = false;
                return;
            }

            if (itemTotalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for some items.';
                item.totalCost = item.numBalance;
                isSuccess = false;
            } else {
                item.totalCost = itemTotalCost;
            }
            item.percentageOfBalance = percentageOfBalance;
            grandTotal += item.totalCost;
        });

        this.total = grandTotal;

        if (warningMessage) {
            this.notificationService.showMessage(warningMessage, 'danger');
        }

        return isSuccess;
    }*/

    /*calculateTotals() {
        let grandTotal = 0;
        let warningMessage = '';
        let isSuccess = true;
        this.items.forEach(item => {
            const itemTotalCost = item.numItemPrice * item.numQuantity;
            const percentageOfBalance = (item.numQuantity / item.numBalance) * 100;

            if (item.numQuantity === 0 && item.numBalance == 0) {
                warningMessage = 'Error: Quantity cannot be zero when balance is zero for some items.';
                isSuccess = false;
                return; // Stop processing this item
            }

            if (itemTotalCost > item.numBalance) {
                warningMessage = 'Warning: Total cost exceeds the remaining balance for some items.';
                item.totalCost = item.numBalance;
                isSuccess = false;
            } else {
                item.totalCost = itemTotalCost;
            }

            // Update the percentage of balance used
            item.percentageOfBalance = percentageOfBalance;

            // Add to grand total
            grandTotal += item.totalCost;
        });

        this.total = grandTotal;


        if (warningMessage) {

            this.notificationService.showMessage(warningMessage,'danger');
        }

        return isSuccess;
    }*/

    goBack(): void {
        this.location.back();
    }

    removeExtraZeros(invoiceNo: string): string {
        console.log(invoiceNo.replace(/^0+/, ''))
        return invoiceNo.replace(/^0+/, '');
    }

}
