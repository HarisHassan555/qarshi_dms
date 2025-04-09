import { Component, ViewChild } from '@angular/core';
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {ActivatedRoute} from "@angular/router";
import { Location } from '@angular/common';

@Component({
    moduleId: module.id,
    templateUrl: './preview.html',
})
export class OrderPreviewComponent {

    serDealId: string | undefined;
    saleOrderLst: any[] = [];
    items: any[] = [];
    saleOrderDetails :any[] =[];
    slsTblDeal: any;

    constructor(private saleOrderService: SaleOrderService, private route: ActivatedRoute,private location: Location) {}

    ngOnInit() {
        this.route.paramMap.subscribe(params => {
            this.serDealId = params.get('serDealId') || '';
            this.getSaleOrderDetails(this.serDealId);
        });
    }

    // @ts-ignore
    getSaleOrderDetails(id) {
        this.saleOrderService.getSaleOrderDetail(id).subscribe((data: any) => {
            console.log('Raw Data:', data);
            if (data) {

                this.items = [];
                let slsTblDeal;
                data.forEach((item: any) => {
                    slsTblDeal = item.slsTblDeal;
                    const processedItem = {
                        txtSapCode: this.removeExtraZeros(item.cfgTblProduct?.txtSapCode ?? 'N/A'),
                        txtProductName: item.cfgTblProduct?.txtProductName ?? 'N/A',
                        numQuantity: item.numQuantity ?? 0,
                        numItemPrice: item.numItemPrice ?? 0,
                        numTotalPrice: (item.numQuantity ?? 0) * (item.numItemPrice ?? 0),
                    };
                    this.items.push(processedItem);
                });

                console.log('Processed Items:', this.items);
                console.log('Extracted slsTblDeal:', slsTblDeal);
                this.slsTblDeal = slsTblDeal;

            } else {
                console.error('No data received');
            }
        });
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

    print = () => {
        window.print();
    };


    goBack(): void {
        this.location.back();
    }

    removeExtraZeros(invoiceNo: string): string {
        console.log(invoiceNo.replace(/^0+/, ''))
        return invoiceNo.replace(/^0+/, '');
    }
}
