import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class CustomFormService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllCustomForms');
  }

  getById(formId: number) {
    return this.http.get(urls.API_URL + 'getCustomFormById?formId=' + formId);
  }

  save(payload: any) {
    if (payload.serFormId) {
      return this.http.post(urls.API_URL + 'updateCustomForm', payload);
    } else {
      return this.http.post(urls.API_URL + 'addNewCustomForm', payload);
    }
  }

  delete(formId: number) {
    return this.http.post(urls.API_URL + 'deleteCustomForm?formId=' + formId, {});
  }

  getActive() {
    return this.http.get(urls.API_URL + 'getActiveCustomForms');
  }
}

