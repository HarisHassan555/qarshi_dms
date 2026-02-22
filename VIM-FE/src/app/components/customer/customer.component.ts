import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CityService } from 'src/app/services/city/city.service';
import { CountryService } from 'src/app/services/country/country.service';
import { countries } from '../country/dummy_data';
import { customers, newCity } from './dummy_data';
import { CustomerService } from 'src/app/services/customer/customer.service';
import { cities } from '../city/dummy_data';

@Component({
  selector: 'app-customer',
  templateUrl: './customer.component.html',
  styleUrls: ['./customer.component.css']
})
export class CustomerComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  cities: any;
  countries: any;
  filteredCities: any;
  customers: any;
  blnIsFiler = false;
  blnStatus = false;

  cols = [
    { field: 'serCustomerId', title: 'Sr. No' },
    { field: 'txtCustomerCode', title: 'Media House Code' },
    { field: 'txtCustomerName', title: 'Media House Name' },
    { field: 'txtCnicNo', title: 'CNIC' },
    { field: 'txtNtnNo', title: 'NTN' },
    { field: 'txtSapNo', title: 'SAP ID' },
    { field: 'txtSTR', title: 'STRN' },
    { field: 'txtEmailAddress', title: 'Email' },
    { field: 'cfgTblCountry.txtName', title: 'Country' },
    { field: 'cfgTblCity.txtCityName', title: 'City' },
    { field: 'blnIsFiler', title: 'Filer' },
    { field: 'txtBillingAddress', title: 'Billing Address' },
    { field: 'txtShippingAddress', title: 'Shipping Address' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  entityLabel = 'Media House';
  entityCodeLabel = 'Media House Code';
  entityNameLabel = 'Media House Name';

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private cityService: CityService,
    private countryService: CountryService,
    private notificationService: NotificationService,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.cols = this.cols.map((col: any) => {
      if (col.field === 'txtCustomerCode') {
        return { ...col, title: this.entityCodeLabel };
      }
      if (col.field === 'txtCustomerName') {
        return { ...col, title: this.entityNameLabel };
      }
      return col;
    });

    this.form = this.fb.group({
      serCustomerId: [''],
      txtCustomerCode: ['', Validators.required],
      txtCustomerName: ['', Validators.required],

      txtCnicNo: ['', Validators.pattern(/^\d{5}-\d{7}-\d{1}$/)],
      txtNtnNo: ['', Validators.required],

      cfgTblCountry: this.fb.group({
        serCountryId: ['', Validators.required]
      }),

      cfgTblCity: this.fb.group({
        serCityId: ['', Validators.required]
      }),

      txtPhoneNo: [''],
      txtPhoneNo2: ['', Validators.required],


      txtSapNo: ['', Validators.required],
      txtEmailAddress: ['', Validators.email],

      txtSTR: [''],
      txtBillingAddress: ['', Validators.required],
      txtShippingAddress: ['', Validators.required],

      numFurtherTax: [0.02]
    });
    this.getCountries();
    this.getCities();
    this.getCustomers();
  }

  getCountries() {
    this.countryService
      .getActiveCountries()
      .subscribe((data: any) => {
        if (data) {
          this.countries = data;
        }
      });
  }

  getCities() {
    this.cityService
      .getActiveCities()
      .subscribe((data: any) => {
        if (data) {
          this.cities = data;
        }
      });
  }

  getCustomers() {
    this.customers = [];
    const source$ = this.entityLabel === 'Vendor'
      ? this.customerService.getCustomers()
      : this.customerService.getAll();
    source$.subscribe((data: any) => {
      if (data) {
        this.customers = data;
      }
    });
  }

  onChangeCountry(evt: any) {
    if (evt.target.value) {
      const countryId = evt.target.value
      this.filteredCities = this.cities.filter((city: any) => city.cfgTblCountry ? city.cfgTblCountry.serCountryId == countryId : false);
    }
  }

  add() {
    this.isSubmit = false;
    this.form.reset();
    this.blnStatus = false;
    this.blnIsFiler = false;
    this.modal.open();
  }

  edit(customer: any) {
    this.form.reset();
    this.modal.open();
    this.form.patchValue(customer);
    this.blnStatus = customer.blnStatus;
    this.blnIsFiler = customer.blnIsFiler;
    console.log(customer);
    this.filteredCities = this.cities.filter(
      (city: any) => city.cfgTblCountry.serCountryId == customer.cfgTblCountry.serCountryId
    );
  }

  submit() {
    console.log(this.form.value);
    this.isSubmit = true;
    if (this.form.invalid) return;
    const payload = this.form.value;
    payload.blIsDealer = true;
    payload.blnIsFiler = this.blnIsFiler;

    if (payload.serCustomerId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serCustomerId;
    }

    this.customerService
      .save(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Media House saved successfully','success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getCustomers();
        } else {
          this.notificationService.showMessage('Error occured while saving','danger');
        }
      });
  }

  delete(customerId: any = null) {
    let selectedIds;
    if (confirm('Are you sure want to delete selected record?')) {
      if (customerId) {
        // this.products = this.products.filter((d: any) => d.serProductId != item);
        selectedIds = [customerId];
      } else {
        let selectedRows = this.datatable.getSelectedRows();
        const ids = selectedRows.map((d: any) => {
          return d.serCustomerId;
        });
        selectedIds = ids;
        // this.products = this.products.filter((d: any) => !ids.includes(d.serProductId as never));
      }

      if (selectedIds && selectedIds.length) {
        this.customerService
          .delete(selectedIds.toString())
          .subscribe((data: any) => {
            if (data) {
              this.notificationService.showMessage('Record deleted successfully');
              this.getCustomers();
              // this.datatable.clearSelectedRows();
            }
          });
      }
    }
  }

  get cfgTblCountry(){
    return this.form.get('cfgTblCountry') as FormGroup;
  }

  get cfgTblCity(){
    return this.form.get('cfgTblCity') as FormGroup;
  }
}
