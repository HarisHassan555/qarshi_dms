import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { CityService } from 'src/app/services/city/city.service';
import { CountryService } from 'src/app/services/country/country.service';
import {alphabetOnlyAsyncValidator} from "../../utils/alphabetOnlyAsyncValidator";
import {PermissionService} from "../../services/shared-data/permission-service";

@Component({
  selector: 'app-city',
  templateUrl: './city.component.html',
  styleUrls: ['./city.component.css']
})
export class CityComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  search = '';
  form!: FormGroup;
  isSubmit = false;
  cities: any
  countries: any;
  blnStatus = false;
   // @ts-ignore
    editCase = false;
  cols = [
    // { field: 'serCityId', title: 'Sr. No' },
    { field: 'txtCityCode', title: 'City Code' },
    { field: 'txtCityName', title: 'City Name' },
    { field: 'cfgTblCountry.txtName', title: 'Country' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private cityService: CityService,
    private countryService: CountryService,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
    this.form = this.fb.group({
      serCityId: [''],
      cfgTblCountry: this.fb.group({
        serCountryId: ['', Validators.required]
      }),
      txtCityCode: ['', Validators.required],
      txtCityName: ['', Validators.required,alphabetOnlyAsyncValidator()],
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
          this.getCities();
      });


  }

  getCountries() {
    this.countries = [];
    this.countryService
      .getActiveCountries()
      .subscribe((data: any) => {
        if (data) {
          this.countries = data;
        }
      });
  }

  getCities() {
    this.cities = [];
    this.cityService
      .getAll()
      .subscribe((data: any) => {
        if (data) {
          this.cities = data;
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
          this.permissionService.canAdd('City').subscribe(canAdd => {
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
                  this.notificationService.showMessage('You do not have permission to add new Cities', 'danger');
                  return;
              }

          });
      });
    /*this.isSubmit = false;
    this.form.reset();
    this.blnStatus = false;
    this.modal.open();*/
  }

  edit(country: any) {
      if (!this.permissionService.canUpdate('City')) {
          this.notificationService.showMessage('You do not have permission to edit countries', 'danger');
          return;
      }
    this.form.reset();
    this.modal.open();
    this.form.patchValue(country);
    this.blnStatus = country.blnStatus;
    // @ts-ignore
      this.editCase = true;
  }

  submit() {
      console.log(this.form.value);
      this.isSubmit = true;
      if (this.form.invalid) return;
      const payload = this.form.value;

      if (payload.serCityId) {
          payload.blnStatus = this.blnStatus;
          payload.blIsDeleted = false;
      } else {
          delete payload.serCityId;
      }

      // @ts-ignore
      let existingCity;
      // @ts-ignore
      if(this.editCase){
          // @ts-ignore
          // @ts-ignore
          existingCity  = this.cities.find(country =>
              country.txtName === payload.txtCityName && country.serCityId !== payload.serCityId
          );
      }else{
          // @ts-ignore
          existingCity = this.cities.find(city => city.txtCityName === payload.txtCityName);
      }
      // check duplication on front end
      // @ts-ignore
     // const existingCity = this.cities.find(city => city.txtCityName === payload.txtCityName);
      if (existingCity) {
          // If the city is found, return the existing city
          console.log('City already exists:', existingCity);
          this.notificationService.showMessage('City already exists', 'danger');
          return existingCity;
      } else {
          this.cityService
              .save(payload)
              .subscribe((data: any) => {
                  if (data == "Success") {
                      this.notificationService.showMessage('Country saved successfully', 'success');
                      this.isSubmit = false;
                      this.form.reset();
                      this.modal.close();
                      this.getCities();
                  } else {
                      this.notificationService.showMessage('Error occured while saving', 'danger');
                  }
              });
      }
  }
  get cfgTblCountry(){
    return this.form.get('cfgTblCountry') as FormGroup;
  }
}
