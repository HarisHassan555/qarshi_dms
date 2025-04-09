import {Component, inject} from '@angular/core';
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {ActivatedRoute, Router} from "@angular/router";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {urls} from "../../utils/urls";
import {Location} from "@angular/common";
import {NotificationService} from "../../NotificationService";

@Component({
    moduleId: module.id,
    templateUrl: './edit.html',
})
export class SaleInvoiceViewStatusEditComponent {
    selectedFile = null;
    params = {
        title: 'Tailwind',
        invoiceNo: '#0001',
        to: {
            name: 'Jesse Cory',
            email: 'redq@company.com',
            address: '405 Mulberry Rd. Mc Grady, NC, 28649',
            phone: '(128) 666 070',
        },

        invoiceDate: new Date().toString(),
        dueDate: '',
        bankInfo: {
            no: '1234567890',
            name: 'Bank of America',
            swiftCode: 'VS70134',
            country: 'United States',
            ibanNo: 'K456G',
        },
        notes: 'It was a pleasure working with you and your team. We hope you will keep us in mind for future freelance projects. Thank You!',
    };

    serDealId: string | undefined;
    saleOrderDetailLst: any[] = [];
    items: any[] = [];
    saleOrderDetails :any[] =[];
    slsTblDeal: any;
    slsTblSaleOrder:any
    constructor(private saleOrderService: SaleOrderService, private route: ActivatedRoute,private router: Router,private http: HttpClient,private location: Location,private notificationService: NotificationService,) {}

    ngOnInit() {

        this.route.paramMap.subscribe(params => {
            this.serDealId = params.get('serDealId') || '';
            this.getSaleOrderDetails(this.serDealId);
        });
    }

    // @ts-ignore
    getSaleOrderDetails(id) {
        this.saleOrderService.getSaleOrderDetailList(id).subscribe((data: any) => {
            console.log('Raw Data:', data);
            if (data) {

                this.items = [];
                let slsTblSaleOrder;
                this.saleOrderDetailLst = data;
                data.forEach((item: any) => {
                    slsTblSaleOrder = item.slsTblSaleOrder;
                    const processedItem = {
                        serSoDetailId:item.serSoDetailId,
                        txtSapCode:     this.removeExtraZeros(item.cfgTblProduct?.txtSapCode ?? 'N/A'),
                        txtProductName: item.cfgTblProduct?.txtProductName ?? 'N/A',
                        numQuantity: item.numQuantity ?? 0,
                        numItemPrice: item.numItemPrice ?? 0,
                        sapLineItem:item.sapLineItem ?? 'N/A',
                        txtSoapResponseMsg:item.txtSoapResponseMsg,
                        numAmount: item.slsTblSaleOrder.numAmount,
                        numTotalPrice: item.numAmount,
                        txtSoapReturnType : item.txtSoapReturnType
                    };
                    this.items.push(processedItem);
                });

                console.log('Processed Items:', this.items);
                // @ts-ignore
                console.log('Extracted slsTblDeal:', slsTblSaleOrder);
                this.slsTblSaleOrder = slsTblSaleOrder;
                this.slsTblDeal = this.slsTblSaleOrder.slsTblDeal;

            } else {
                console.error('No data received');
            }
        });
    }

    uploadedFiles: File[] = [];

    handleFileUpload(event: Event) {
        const target = event.target as HTMLInputElement;
        const files = target.files;

        if (files) {
            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                if (file.size <= 2 * 1024 * 1024) {
                    this.uploadedFiles.push(file);
                } else {
                    alert(`${file.name} exceeds the 2MB size limit and was not uploaded.`);
                }
            }
        }
    }

    removeFile(file: File) {
        this.uploadedFiles = this.uploadedFiles.filter(f => f !== file);
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
        {
            key: 'txtSoapResponseMsg',
            label: 'SAP Response Message',
            class: 'ltr:text-right rtl:text-left',
        }
    ];
    //txtSoapResponseMsg
    item: any;
    addItem() {

        // Create a new FormData object
        const formData = new FormData();

        const payload = {

     //   so:{"numDiscount":0,"numFED":0,"numAmountAfterFED":0,"numCVT":0,"numFreight":0,"numTaxOnFreight":0,"numAvanceTax":0,"numFabrication":0,"blIsGAL":"true","dteDate":"2024-09-16","txtSaleOrderNo":"PSO-001","numDiscountAmount":0,"slsTblDeal":{"serDealId":62,"blIsDeleted":false,"blnDealCompletionStatus":null,"blnIsApproved":null,"blnIsCompleted":false,"dteCreateddate":1725864219828,"dteDate":1631127600000,"dteStartDate":1631127600000,"dteEndDate":null,"dteDueDate":null,"dteModifieddate":null,"numExciseDuty":null,"numNetAmount":null,"priority":null,"serApprovedbyId":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"serPreparedbyId":null,"txtBillingAddress":null,"txtDealer":null,"txtCustomer":null,"txtProduct":null,"txtDescription":null,"txtDestination":null,"txtMachineIp":null,"txtPaymnetTerms":null,"txtPriceTerms":null,"txtReceiveStatus":null,"txtDealNo":"4400009471","txtDealName":null,"txtShipBy":null,"txtShippingAddress1":null,"txtShippingAddress2":null,"txtShippingAddress3":null,"txtDeliveryTime":null,"txtVehicleType":null,"dteRSMApproval":null,"dteFinalApproval":null,"numQuantity":1,"numPrice":null,"numTotalPrice":null,"cfgTblProduct":{"serProductId":4321,"blIsDeleted":false,"blIsProduction":null,"blnIsInventoryItem":null,"blnIsPurchaseItem":null,"blnIsSaleItem":null,"blnIsKichenItem":null,"blnIsShopItem":null,"blnIsTangible":null,"blnStatus":true,"blIspacking":null,"blIsSet":null,"blIsComponent":null,"blIsImport":null,"dteCreateddate":1725864208266,"dteModifieddate":null,"numDiscount":null,"numMarketRetailPrice":null,"numPiecesInMasterPack":null,"numProductWeight":null,"numSalePrice":null,"numTradePrice":null,"numUnitPrice":null,"numUnitsInMasterPack":null,"numInnersInMasterPack":null,"picProductImage":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtDescription":null,"txtMachineIp":null,"txtMasterPack":null,"txtPriceUnit":null,"txtProductCode":"000000000000900016","txtProductName":"WASHING SERVICES (LABCOAT, SHE","txtQuality":null,"blIsTruck":null,"blIsBus":null,"blIsPickup":null,"txtType":null,"txtSapCode":"000000000000900016","numEngineCC":null,"cfgTblBrand":null,"cfgTblProductCategory":null,"cfgTblUom":null,"txtVariant":null,"txtTransmission":null,"txtColor":null,"txtInteriorColor":null,"numOldPrice":null,"numSalesTax":null,"numFED":null,"numCVT":null,"numNonfilerAmount":null,"numfilerAmount":null,"numAmount":null,"txtSKU":null,"slsTblSoDetails":null},"blnFromSAP":true,"blnIsIncoTerm":null,"numDiscount":null,"numDiscountAmount":null,"numAmountAfterDiscount":null,"numFED":null,"numFEDAmount":null,"numAmountAfterFED":null,"numSalesTax":0,"numSalesTaxAmount":null,"numAmountAfterST":null,"numCVT":null,"numCVTAmount":null,"numAmountAfterCVT":null,"numFreight":null,"numTaxOnFreight":null,"numTOFAmount":null,"numGrossValue":null,"numAvanceTax":null,"numTotal":null,"numAmount":null,"txtType":null,"blIsGAL":null,"txtSoapReturnType":null,"txtSoapResponseMsg":null,"txtStatus":null,"txtIssueCode":null,"dteIssuedate":null,"numAmountReceived":null,"numRemainingBalance":null,"cfgTblCity":null,"cfgTblCustomer":null,"slsTblDealDetails":null,"dte_date_from":"2021-09-09","dte_date_to":null,"blIsComplementry":null,"blIsSplit":null,"cfgTblDealer":{"serCustomerId":15132,"blIsDeleted":false,"blnIsFiler":null,"blnStatus":true,"dteCreateddate":1725864206875,"dteModifieddate":null,"numDiscount":null,"numExciseDuty":null,"numSalesTax":null,"numFED":null,"numFurtherTax":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtBillingAddress":null,"txtBusinessName":null,"txtCnicNo":null,"txtCustomerCode":"0006100277","txtInvoiceName":null,"txtCustomerName":"0006100277  (Mr. Sarwar Khan)","txtDisplayAddress":null,"txtEmailAddress":"","txtGstNameOnInvoice":null,"txtGstNumber":null,"txtIsFiler":null,"txtMachineIp":null,"txtMobileNo":null,"txtNtnNo":"","txtSTR":"","txtFTN":null,"txtProvince":null,"txtFName":null,"dteExpiryDate":null,"dteDOB":null,"txtExpiryDate":null,"txtPhoneNo":"","txtPhoneNo2":null,"blnCommercial":null,"blnPassanger":null,"blnIsGst":null,"txtShippingAddress":"","cfgTblCity":{"serCityId":40,"blIsDeleted":false,"blnStatus":true,"txtCityCode":"00040","txtCityName":"LAHORE","numFreight":null,"numFreight14":null,"numFreight20":null,"numFreight40":null,"cfgTblCountry":{"serCountryId":1,"blIsDeleted":false,"blnIsnational":null,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1658312034161,"serCreatedUserId":null,"serModifiedUserId":2,"serParentCountryId":null,"txtMachineIp":null,"txtName":"Pakistan","cfgTblCities":null,"cfgTblCustomers":null},"cfgTblCustomers":null,"cfgTblSuppliers":null,"slsTblSaleOrders":null},"cfgTblCountry":null,"cfgTblCustomerCategory":null,"slsTblSaleOrders":null,"cfgTblArea":null,"cfgTblRegion":null,"cfgTblZone1":null,"cfgTblZone2":null,"cfgTblZone3":null,"txtXMSent":null,"txtReturnMsg":null,"txtXMReceive":null,"blIsDealer":true,"cfgTblCustomer":null,"txtSapNo":"0006100277","txtUserName":null,"blIsGroup":null,"blIsAccountExist":null,"blIsLabsa":null,"cfgTblGroupCustomer":null,"hrTblEmployee":null,"blnIsExport":null,"txtDivision":null,"cfgTblIncoTerm":null,"cfgTblDivision":null,"txtDesignation":null,"txtHOD":null,"txtHODMobile":null,"txtHODLandLine":null,"txtHODEmailAddress":null,"txtErrorMsgFromSap":null,"blIsPOSTEDToSAP":null},"cfgTblDistributionChannel":{"serDistributionChannelId":2,"blIsDeleted":false,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1592979402102,"serCreatedUserId":null,"serModifiedUserId":1,"txtCode":"20","txtDescription":null,"txtMachineIp":null,"txtName":"Direct Sales"},"cfgTblDivision":null,"cfgTblDocumentType":{"serDocumentTypeId":2,"blIsDeleted":false,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1658309844308,"serCreatedUserId":null,"serModifiedUserId":2,"txtCode":"ZCSH","txtDescription":null,"txtMachineIp":null,"txtName":"Cash Sales"},"cfgTblIncoTerm":null,"cfgTblPaymentTerm":null,"cfgTblSalesOrganization":{"serSalesOrganizationId":1,"blIsDeleted":false,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":null,"serCreatedUserId":null,"serModifiedUserId":null,"txtCode":"1000","txtDescription":null,"txtMachineIp":null,"txtName":"ICL"},"hrTblEmployee":null,"txtPONo":null,"dtePODate":null,"txtSapNo":"4400009471","txtImage":null,"profile_pic":null,"txtImageName":null,"txtImageType":null,"txtDCStatus":null,"txtInvoiceStatus":null,"txtDCNo":null,"txtInvoiceNo":null,"txtDCDate":null,"txtInvoiceDate":null,"txtOrderapprovalDate":null,"txtDCQty":null,"numCommission":null,"numFabrication":null,"$$hashKey":"object:77"},"numGrossValue":9600,"numTotal":9600,"cfgTblDealerOne":{"serCustomerId":15132,"blIsDeleted":false,"blnIsFiler":null,"blnStatus":true,"dteCreateddate":1725864206875,"dteModifieddate":null,"numDiscount":null,"numExciseDuty":null,"numSalesTax":null,"numFED":null,"numFurtherTax":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtBillingAddress":null,"txtBusinessName":null,"txtCnicNo":null,"txtCustomerCode":"0006100277","txtInvoiceName":null,"txtCustomerName":"0006100277  (Mr. Sarwar Khan)","txtDisplayAddress":null,"txtEmailAddress":"","txtGstNameOnInvoice":null,"txtGstNumber":null,"txtIsFiler":null,"txtMachineIp":null,"txtMobileNo":null,"txtNtnNo":"","txtSTR":"","txtFTN":null,"txtProvince":null,"txtFName":null,"dteExpiryDate":null,"dteDOB":null,"txtExpiryDate":null,"txtPhoneNo":"","txtPhoneNo2":null,"blnCommercial":null,"blnPassanger":null,"blnIsGst":null,"txtShippingAddress":"","cfgTblCity":{"serCityId":40,"blIsDeleted":false,"blnStatus":true,"txtCityCode":"00040","txtCityName":"LAHORE","numFreight":null,"numFreight14":null,"numFreight20":null,"numFreight40":null,"cfgTblCountry":{"serCountryId":1,"blIsDeleted":false,"blnIsnational":null,"blnStatus":true,"dteCreateddate":null,"dteModifieddate":1658312034161,"serCreatedUserId":null,"serModifiedUserId":2,"serParentCountryId":null,"txtMachineIp":null,"txtName":"Pakistan","cfgTblCities":null,"cfgTblCustomers":null},"cfgTblCustomers":null,"cfgTblSuppliers":null,"slsTblSaleOrders":null},"cfgTblCountry":null,"cfgTblCustomerCategory":null,"slsTblSaleOrders":null,"cfgTblArea":null,"cfgTblRegion":null,"cfgTblZone1":null,"cfgTblZone2":null,"cfgTblZone3":null,"txtXMSent":null,"txtReturnMsg":null,"txtXMReceive":null,"blIsDealer":true,"cfgTblCustomer":null,"txtSapNo":"0006100277","txtUserName":null,"blIsGroup":null,"blIsAccountExist":null,"blIsLabsa":null,"cfgTblGroupCustomer":null,"hrTblEmployee":null,"blnIsExport":null,"txtDivision":null,"cfgTblIncoTerm":null,"cfgTblDivision":null,"txtDesignation":null,"txtHOD":null,"txtHODMobile":null,"txtHODLandLine":null,"txtHODEmailAddress":null,"txtErrorMsgFromSap":null,"blIsPOSTEDToSAP":null},"cfgTblCustomer":null,"numSalesTax":0,"slsTblSoDetails":[{"cfgTblProduct":{"serProductId":4321,"blIsDeleted":false,"blIsProduction":null,"blnIsInventoryItem":null,"blnIsPurchaseItem":null,"blnIsSaleItem":null,"blnIsKichenItem":null,"blnIsShopItem":null,"blnIsTangible":null,"blnStatus":true,"blIspacking":null,"blIsSet":null,"blIsComponent":null,"blIsImport":null,"dteCreateddate":1725864208266,"dteModifieddate":null,"numDiscount":null,"numMarketRetailPrice":null,"numPiecesInMasterPack":null,"numProductWeight":null,"numSalePrice":null,"numTradePrice":null,"numUnitPrice":null,"numUnitsInMasterPack":null,"numInnersInMasterPack":null,"picProductImage":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtDescription":null,"txtMachineIp":null,"txtMasterPack":null,"txtPriceUnit":null,"txtProductCode":"000000000000900016","txtProductName":"WASHING SERVICES (LABCOAT, SHE","txtQuality":null,"blIsTruck":null,"blIsBus":null,"blIsPickup":null,"txtType":null,"txtSapCode":"000000000000900016","numEngineCC":null,"cfgTblBrand":null,"cfgTblProductCategory":null,"cfgTblUom":null,"txtVariant":null,"txtTransmission":null,"txtColor":null,"txtInteriorColor":null,"numOldPrice":null,"numSalesTax":null,"numFED":null,"numCVT":null,"numNonfilerAmount":null,"numfilerAmount":null,"numAmount":null,"txtSKU":null,"slsTblSoDetails":null},"numQuantity":"1","numItemPrice":8820,"numBalance":8820,"slsTblDealDetails":{"serDealDetailId":74},"sapLineItem":"00020","sapLineItemDescription":"LAB COAT WHITE","numTotalPrice":8820,"$$hashKey":"object:101","blIsDeleted":true,"isValid":true,"numUnitWt":null},{"cfgTblProduct":{"serProductId":4321,"blIsDeleted":false,"blIsProduction":null,"blnIsInventoryItem":null,"blnIsPurchaseItem":null,"blnIsSaleItem":null,"blnIsKichenItem":null,"blnIsShopItem":null,"blnIsTangible":null,"blnStatus":true,"blIspacking":null,"blIsSet":null,"blIsComponent":null,"blIsImport":null,"dteCreateddate":1725864208266,"dteModifieddate":null,"numDiscount":null,"numMarketRetailPrice":null,"numPiecesInMasterPack":null,"numProductWeight":null,"numSalePrice":null,"numTradePrice":null,"numUnitPrice":null,"numUnitsInMasterPack":null,"numInnersInMasterPack":null,"picProductImage":null,"serCreatedUserId":6,"serGroupId":null,"serModifiedUserId":null,"txtDescription":null,"txtMachineIp":null,"txtMasterPack":null,"txtPriceUnit":null,"txtProductCode":"000000000000900016","txtProductName":"WASHING SERVICES (LABCOAT, SHE","txtQuality":null,"blIsTruck":null,"blIsBus":null,"blIsPickup":null,"txtType":null,"txtSapCode":"000000000000900016","numEngineCC":null,"cfgTblBrand":null,"cfgTblProductCategory":null,"cfgTblUom":null,"txtVariant":null,"txtTransmission":null,"txtColor":null,"txtInteriorColor":null,"numOldPrice":null,"numSalesTax":null,"numFED":null,"numCVT":null,"numNonfilerAmount":null,"numfilerAmount":null,"numAmount":null,"txtSKU":null,"slsTblSoDetails":null},"numQuantity":"1","numItemPrice":780,"numBalance":780,"slsTblDealDetails":{"serDealDetailId":75},"sapLineItem":"00010","sapLineItemDescription":"LAB COAT BLUE","numTotalPrice":780,"$$hashKey":"object:102","blIsDeleted":true,"isValid":true,"numUnitWt":null}],"numNetAmount":9600,"numExciseDuty":0,"numAmount":9600,"numRemainingBalance":9600,"numAmountReceived":0}
            so : {
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
                    "txtSaleOrderNo": "PSO-001",
                    "numDiscountAmount": 0,
                    "slsTblDeal": this.slsTblDeal,
                    "numGrossValue": 0,
                    "numTotal": 0,
                    "cfgTblDealerOne": this.slsTblDeal.cfgTblDealerOne,
                    "cfgTblCustomer": null,
                    "numSalesTax": 0,
                    "slsTblSoDetails": this.saleOrderDetailLst
                ,
                "numNetAmount": 0,
                "numExciseDuty": 0,
                "numAmount": 0,
                "numRemainingBalance": 0,
                "numAmountReceived": 0
            },
          newDocuments: this.uploadedFiles,
        };

        formData.append('newDocuments', JSON.stringify(payload.newDocuments));
        // @ts-ignore
        formData.append("file", this.uploadedFiles);
        // @ts-ignore
        formData.append('so',JSON.stringify(payload.so))


        this.http.post(urls.API_URL + 'addNewSaleOrderNew', formData, {
           /* headers: { 'Content-Type': 'multipart/form-data' },*/
            observe: 'response'
        }).subscribe((response: any) => {
            console.log('Response:', response);

        }, error => {
            console.error('Error:', error);

        });

    }

    removeItem(item: any = null) {
        this.items = this.items.filter((d: any) => d.id != item.id);
    }

    goBack(): void {
        this.location.back();
    }

    repost(item: any) {

        const payload = {
            sapCode: item.txtSapCode,
            saleOrderNo: this.serDealId,
            saleOrderDetailId:item.serSoDetailId,
            lineItem: item.sapLineItem,
            quantity: item.numQuantity,
            postedDate:item.postDate
        };

        // Make the HTTP POST request
        // @ts-ignore
        this.http.post(urls.API_URL + 'repost', payload).
        subscribe(
            (response: any) => {
                let data = typeof response === 'string' ? JSON.parse(response) : response;
                if (data.status === 'Success') {
                    console.log('Update successful:', response);
                    // Show a success message to the user
                    this.notificationService.showMessage("Repost successful",'success');
                    this.router.navigateByUrl('sale-status');

                } else {
                    console.error('Unexpected response:', response);
                    this.notificationService.showMessage("Repost failed, please try again.", 'danger');

                }
            },
            (error: any) => {
                console.error('Error:', error);
                this.notificationService.showMessage("An error occurred while updating the sale order.", 'danger');

            }
        );/*subscribe
            (response: any) => {

                if (response === 'Success') {
                    console.log('Update successful:', response);
                    // Show a success message to the user
                    this.notificationService.showMessage("Sale order updated successfully.", 'success');


                } else {
                    console.error('Unexpected response:', response);
                    this.notificationService.showMessage("Failed to update sale order. Please try again.", 'danger');
                    this.searchSaleInvoice(this.dateFrom, this.dateTo);
                }
           /!* (response == '') => {
                console.log('Reposted successfully:', response);
               // item.txtSoapResponseMsg = 'Repost successful';
               // item.postDate = new Date();
                this.notificationService.showMessage("Repost successful",'success');
            },
            (error) => {
                console.error('Repost failed:', error);
                item.txtSoapResponseMsg = 'Repost failed, please try again';
                this.notificationService.showMessage("Repost failed, please try again",'danger');
            }*!/
        );*/
    }
    editPostDate(item: any) {
        item.isEditable = true;
    }


    removeExtraZeros(invoiceNo: string): string {
        console.log(invoiceNo.replace(/^0+/, ''))
        return invoiceNo.replace(/^0+/, '');
    }
}
