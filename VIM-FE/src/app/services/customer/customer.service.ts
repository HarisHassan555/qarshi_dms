import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllDealer');
  }

  getActiveDealers() {
    return this.http.get(urls.API_URL + 'getActiveDealer');
  }

  getCustomers() {
    return this.http.get(urls.API_URL + 'getAllCustomer');
  }

  getActiveCustomers() {
    return this.http.get(urls.API_URL + 'getActiveCustomer');
  }

  save(payload: any) {
    if (payload.serCustomerId) {
      return this.http.post(urls.API_URL + 'updateCustomer', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewCustomer', payload,{ responseType: 'text' });
    }
  }

  delete(payload: any) {
    return this.http.post(urls.API_URL + 'deleteCustomer', payload,{ responseType: 'text' });
  }
}

