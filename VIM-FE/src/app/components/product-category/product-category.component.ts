import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { ProductCategoryService } from 'src/app/services/product-category/product-category.service';

@Component({
  selector: 'app-product-category',
  templateUrl: './product-category.component.html',
  styleUrls: ['./product-category.component.css']
})
export class ProductCategoryComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  productCategories: any;
  blnStatus = false;

  cols = [
    // { field: 'serTaxId', title: 'Sr. No' },
    { field: 'txtProductCategoryCode', title: 'Product Category Code' },
    { field: 'txtProductCategoryName', title: 'Product Category Name' },
    { field: 'txtSAPCode', title: 'SAP Code' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private productCategoryService: ProductCategoryService,
    private notificationService: NotificationService
  ) { }

  ngOnInit() {
    this.form = this.fb.group({
      serProductCategoryId: [''],
      txtProductCategoryCode: ['', Validators.required],
      txtProductCategoryName: ['', Validators.required],
      txtSAPCode: ['', Validators.required],
      txtDescription: ['']
    });
    this.getProductCategories();
  }

  getProductCategories() {
    this.productCategories = [];
    this.productCategoryService
      .getAll()
      .subscribe((data: any) => {
        if (data) {
          this.productCategories = data;
        }
      });
  }

  add() {
    this.isSubmit = false;
    this.form.reset();
    this.blnStatus = false;
    this.modal.open();
  }

  edit(productCategory: any) {
    this.form.reset();
    this.modal.open();
    this.form.patchValue(productCategory);
    this.blnStatus = productCategory.blnStatus;
  }

  submit() {
    this.isSubmit = true;
    if (this.form.invalid) return;
    let payload = this.form.value;

    if (payload.serProductCategoryId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serProductCategoryId;
    }

    this.productCategoryService
      .save(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Record saved successfully','success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getProductCategories();
        } else {
          this.notificationService.showMessage('Error occured while saving','danger');
        }
      });
  }
}
