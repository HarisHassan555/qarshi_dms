import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { TaxCategoryService } from 'src/app/services/tax-category/tax-category.service';


@Component({
  selector: 'app-tax-category',
  templateUrl: './tax-category.component.html',
  styleUrls: ['./tax-category.component.css']
})
export class TaxCategoryComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  taxCategories: any;
  blnStatus = false;

  cols = [
    // { field: 'serTaxId', title: 'Sr. No' },
    { field: 'txtTaxOrganization', title: 'Tax Organization' },
    { field: 'txtOrganizationStatus', title: 'Organization Status' },
    { field: 'numTaxPercentage', title: 'Tax Percentage' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private taxCategoryService: TaxCategoryService,
    private notificationService: NotificationService
  ) { }

  ngOnInit() {
    this.form = this.fb.group({
      serTaxId: [''],
      txtTaxOrganization: ['', Validators.required],
      txtOrganizationStatus: ['', Validators.required],
      numTaxPercentage: ['', Validators.required],
    });
    this.getTaxCategories();
  }

  getTaxCategories() {
    this.taxCategories = [];
    this.taxCategoryService
      .getAll()
      .subscribe((data: any) => {
        if (data) {
          this.taxCategories = data;
        }
      });
  }

  add() {
    this.isSubmit = false;
    this.form.reset();
    this.blnStatus = false;
    this.modal.open();
  }

  edit(taxCategory: any) {
    this.form.reset();
    this.modal.open();
    this.form.patchValue(taxCategory);
    this.blnStatus = taxCategory.blnStatus;
  }

  submit() {
    this.isSubmit = true;
    if (this.form.invalid) return;
    let payload = this.form.value;

    if (payload.serTaxId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serTaxId;
    }

    this.taxCategoryService
      .save(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Record saved successfully','success');
          this.isSubmit = false;
          this.form.reset();
          this.modal.close();
          this.getTaxCategories();
        } else {
          this.notificationService.showMessage('Error occured while saving','danger');
        }
      });
  }
}
