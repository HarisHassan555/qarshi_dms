import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class TaxCategoryService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllTax');
  }

  save(payload: any) {
    if (payload.serTaxId) {
      return this.http.post(urls.API_URL + 'updateTax', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewTax', payload,{ responseType: 'text' });
    }
  }
}
