import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { ProductService } from 'src/app/services/product/product.service';
import { ServiceOrderService } from 'src/app/services/service-order/service-order.service';
import { TaxCategoryService } from 'src/app/services/tax-category/tax-category.service';
import { serviceOrder } from './dummy';
import {NotificationService} from "../../NotificationService";

@Component({
  selector: 'app-service-order',
  templateUrl: './service-order.component.html',
  styleUrls: ['./service-order.component.css']
})
export class ServiceOrderComponent implements OnInit {
  dealers: any;
  customers: any;
  products: any;
  taxes: any;
  serviceOrders: any = [];
  form!: FormGroup;
  isSubmit = false;
  slsTblDealDetails: any = [];


  items: any = [];
  selectedFile = null;
  params = {
      title: '',
      invoiceNo: '',
      to: {
          name: '',
          email: '',
          address: '',
          phone: '',
      },

      invoiceDate: '',
      dueDate: '',
      bankInfo: {
          no: '',
          name: '',
          swiftCode: '',
          country: '',
          ibanNo: '',
      },
      notes: '',
  };
  currencyList = [
      'USD - US Dollar',
      'GBP - British Pound',
      'IDR - Indonesian Rupiah',
      'INR - Indian Rupee',
      'BRL - Brazilian Real',
      'EUR - Germany (Euro)',
      'TRY - Turkish Lira',
  ];
  selectedCurrency = 'USD - US Dollar';
  tax:number | undefined;
  discount :number | undefined;
  shippingCharge:number | undefined;
  paymentMethod = '';

  constructor(
    private customerService: CustomerService,
    private productService: ProductService,
    private serviceOrderService: ServiceOrderService,
    private taxService: TaxCategoryService,
    private fb: FormBuilder,private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      dtedate: ['', Validators.required],
      txtdealno: ['', Validators.required],

      cfgTblDealer: this.fb.group({
        serCustomerId: ['', Validators.required]
      }),

      cfgTblCustomer: this.fb.group({
        serCustomerId: ['', Validators.required]
      }),

      dteStartDate: ['', Validators.required],
      dteEndDate: ['', Validators.required],

      txtSapNo: ['', Validators.required],

      tax: [''],
      serProductId: [null, Validators.required],
      numItemPrice: ['', Validators.required]
    });
    this.getActiveDealers();
    this.getActiveCustomers();
    this.getActiveProducts();
    this.getDealNo();
    this.getTaxes();
  }

  getDealNo() {
    this.serviceOrderService.getDealNo().subscribe(data => {
      this.form.patchValue({
        txtdealno: data
      })
    })
  }

  getActiveDealers() {
    this.customerService.getActiveDealers().subscribe(data => {
      this.dealers = data;
    })
  }

  getActiveCustomers() {
    this.customerService.getActiveCustomers().subscribe(data => {
      this.customers = data;
    })
  }

  getActiveProducts() {
    this.productService.getActiveProducts().subscribe(data => {
      this.products = data;
    })
  }

  getTaxes() {
    this.taxService.getAll().subscribe((data: any) => {
      this.taxes = data.filter((d: any) => d.blnStatus);
    });
  }

  onChangeProduct($event: any) {
    console.log($event.target.value);
  }

  addSO() {

    this.isSubmit = true;
    if (this.form.invalid) return;
    const form = this.form.value;

    if (!this.isAlreadyExist(form.serProductId)) {
      const data = {
        numQuantity: 1,
        cfgTblProduct: this.products.find((p: any) => p.serProductId == form.serProductId),
        numItemPrice: form.numItemPrice,
      }
      this.slsTblDealDetails.push(data);
      console.log(this.slsTblDealDetails);
      console.log(this.form.value);
    } else {
      alert('Duplicate entry. Please select a different product.');
    }
  }

  isAlreadyExist(serProductId: any) {
    const product = this.slsTblDealDetails.find((so: any) => so.serProductId == serProductId);
    return product ? true : false;
  }

  deleteSO(serviceOrder: any) {
  }

  getTotalQuantity() {
    let quantity = 0;
    this.slsTblDealDetails.forEach((deal: any) => {
      quantity = quantity + parseInt(deal.numQuantity);
    });
    return quantity;
  }

  save() {
    let payload  = serviceOrder;
    let netAmount = 0;
    if (!this.slsTblDealDetails.length) return;

    let form = this.form.value;

   // payload.dtedate = form.dtedate;
    payload.txtDealNo = form.txtdealno;
    payload.cfgTblDealer = this.dealers.find((dealer: any) => dealer.serCustomerId == form.cfgTblDealer.serCustomerId);
    payload.cfgTblCustomer = this.customers.find((dealer: any) => dealer.serCustomerId == form.cfgTblCustomer.serCustomerId);
    payload.numSalesTax = form.tax;
    payload.dteStartDate = form.dteStartDate;
    payload.dteEndDate = form.dteEndDate;
    payload.dte_date_from = form.dteStartDate;
    payload.dte_date_to = form.dteEndDate;

    payload.slsTblDealDetails = this.slsTblDealDetails;

    this.slsTblDealDetails.forEach((deal: any) => {
      netAmount = netAmount + (deal.numQuantity * deal.numItemPrice);
    });

    payload.numNetAmount = netAmount + ((netAmount * form.tax) / 100);

    this.serviceOrderService.save(payload).subscribe(data => {
      if (data) {
        console.log(data);
        this.notificationService.showMessage("Service Order Created Successfully",'success');
        this.form.reset();
        this.slsTblDealDetails = [];
        this.isSubmit = false;
      }
     // this.n
    });
  }

  get cfgTblDealer(){
    return this.form.get('cfgTblDealer') as FormGroup;
  }

  get cfgTblCustomer(){
    return this.form.get('cfgTblCustomer') as FormGroup;
  }
}
