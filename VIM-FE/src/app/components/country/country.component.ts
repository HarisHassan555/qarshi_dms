import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CountryService } from 'src/app/services/country/country.service';
import {alphabetOnlyAsyncValidator} from "../../utils/alphabetOnlyAsyncValidator";
import {PermissionService} from "../../services/shared-data/permission-service";

@Component({
  selector: 'app-country',
  templateUrl: './country.component.html',
  styleUrls: ['./country.component.css']
})
export class CountryComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  countryForm!: FormGroup;
  isSubmit = false;
  countries: any;
  blnStatus = false;
// @ts-ignore
  editCase = false;
  cols = [
    // { field: 'serCountryId', title: 'Sr. No' },
    { field: 'txtName', title: 'Country' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private countryService: CountryService,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
    this.countryForm = this.fb.group({
      serCountryId: [''],
      txtName: ['', Validators.required,[alphabetOnlyAsyncValidator()]]
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
     /* this.selectedRoleId = 1; // Mock value; replace with actual roleId
      this.selectedUserId = 3; // Mock value; replace with actual userId*/
      // @ts-ignore
      this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
          this.getCountries();
      });

  }

  getCountries() {
    this.countries = [];
    this.countryService
      .getAll()
      .subscribe((data: any) => {
        if (data) {
          this.countries = data;
        }
      });
  }

  add() {
      // @ts-ignore
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
          this.permissionService.canAdd('Country').subscribe(canAdd => {
              if (canAdd == true) {
                  /* this.notificationService.showMessage('You do not have permission to add new countries', 'danger');*/
                  this.isSubmit = false;
                  this.countryForm.reset();
                  this.blnStatus = false;
                  this.modal.open();
                  return;
              }else{
                  this.notificationService.showMessage('You do not have permission to add new countries', 'danger');
                  return;
              }

          });
      });

  }

  edit(country: any) {
   if (!this.permissionService.canUpdate('Country')) {
          this.notificationService.showMessage('You do not have permission to edit countries', 'danger');
          return;
    }
    this.countryForm.reset();
    this.modal.open();
    this.countryForm.patchValue(country);
    this.blnStatus = country.blnStatus;
    // @ts-ignore
      this.editCase = true;
  }

  submit() {
    this.isSubmit = true;
    if (this.countryForm.invalid) return;
    const payload = this.countryForm.value;

    if (payload.serCountryId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serCountryId;
    }
      let existingCity;
     // @ts-ignore
      if(this.editCase){
         // @ts-ignore
         // @ts-ignore
         existingCity  = this.countries.find(country =>
             country.txtName === payload.txtName && country.serCountryId !== payload.serCountryId
         );
     }else{
         // @ts-ignore
          existingCity = this.countries.find(country => country.txtName === payload.txtName);
      }
      // @ts-ignore

      if (existingCity) {
        //  console.log('City already exists:', existingCity);
          this.notificationService.showMessage('Country already Exists', 'danger');
          return existingCity;
      } else {
          this.countryService
              .save(payload)
              .subscribe((data: any) => {
                  if (data == "Success") {
                      this.notificationService.showMessage('Record saved successfully','success');
                      this.isSubmit = false;
                      this.countryForm.reset();
                      this.modal.close();
                      this.getCountries();
                  } else {
                      this.notificationService.showMessage('Error occured while saving','danger');
                  }
              });
      }
  }
}
