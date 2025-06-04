import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { ProductCategoryService } from 'src/app/services/product-category/product-category.service';
import { ProductService } from 'src/app/services/product/product.service';
import {PermissionService} from "../../services/shared-data/permission-service";

@Component({
  selector: 'app-product',
  templateUrl: './product.component.html',
  styleUrls: ['./product.component.css']
})
export class ProductComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  products: any;
  productCategories: any;
  blnStatus = false;

  cols = [
    // { field: 'serTaxId', title: 'Sr. No' },
    { field: 'txtProductCode', title: 'Product Code' },
    { field: 'txtProductName', title: 'Product Name' },
    { field: 'cfgTblProductCategory.txtProductCategoryName', title: 'Category' },
    { field: 'txtSapCode', title: 'SAP Code' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private productService: ProductService,
    private productCategoryService: ProductCategoryService,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
    this.form = this.fb.group({
      serProductId: [''],
      cfgTblProductCategory: this.fb.group({
        serProductCategoryId: ['', Validators.required]
      }),
      txtSapCode: ['', Validators.required],
      txtProductCode: ['', Validators.required],
      txtProductName: ['', Validators.required],
      txtDescription: ['']
    });
      const userJson = localStorage.getItem('user');
      let user: {
          cfgTblRole: number | undefined;
          serUserId: number;
      };

      if (userJson) {
          // @ts-ignore
          user = JSON.parse(userJson) as CfgTblUser;
      }
      // @ts-ignore
      this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
          this.getProducts();
          this.getProductCategories()
      });


  }

  getProducts() {
    this.products = [];
    this.productService
      .getAll()
      .subscribe((data: any) => {
        if (data) {
          this.products = data;
        }
      });
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

      const userJson = localStorage.getItem('user');
      let user: {
          cfgTblRole: number | undefined;
          serUserId: number;
      };

      if (userJson) {
          // @ts-ignore
          user = JSON.parse(userJson) as CfgTblUser;
      }
      // @ts-ignore
      this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
          // @ts-ignore
          this.permissionService.canAdd('Product').subscribe(canAdd => {
              if (canAdd == true) {
                  /* this.notificationService.showMessage('You do not have permission to add new countries', 'danger');*/
                  /*this.isSubmit = false;
                  this.countryForm.reset();
                  this.blnStatus = false;
                  this.modal.open();*/
                  this.isSubmit = false;
                  this.form.reset();
                  this.blnStatus = false;
                  this.modal.open();
                  return;
              }else{
                  this.notificationService.showMessage('You do not have permission to add new Product', 'danger');
                  return;
              }

          });
      });


  }

  edit(product: any) {

    if (!this.permissionService.canUpdate('Product')) {
          this.notificationService.showMessage('You do not have permission to edit Product', 'danger');
          return;
    }
    this.form.reset();
    this.modal.open();
    this.form.patchValue(product);
    this.blnStatus = product.blnStatus;
  }

  submit() {
    this.isSubmit = true;
    if (this.form.invalid) return;
    let payload = this.form.value;

    if (payload.serProductId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serProductId;
    }

    this.productService
      .save(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Record saved successfully','success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getProducts();
        } else {
          this.notificationService.showMessage('Error occured while saving','danger');
        }
      });
  }

  delete(productId: any = null) {
    let selectedIds;
    if (confirm('Are you sure want to delete selected record?')) {
      if (productId) {
        // this.products = this.products.filter((d: any) => d.serProductId != item);
        selectedIds = [productId];
      } else {
        let selectedRows = this.datatable.getSelectedRows();
        const ids = selectedRows.map((d: any) => {
          return d.serProductId;
        });
        selectedIds = ids;
        // this.products = this.products.filter((d: any) => !ids.includes(d.serProductId as never));
      }

      if (selectedIds && selectedIds.length) {
        this.productService
          .delete(selectedIds.toString())
          .subscribe((data: any) => {
            if (data) {
              this.notificationService.showMessage('Record deleted successfully');
              this.getProducts();
              // this.datatable.clearSelectedRows();
            }
          });
      }
    }
  }

  get cfgTblProductCategory(){
    return this.form.get('cfgTblProductCategory') as FormGroup;
  }
}
