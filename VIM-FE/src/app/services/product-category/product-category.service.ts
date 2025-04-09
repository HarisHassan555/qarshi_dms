import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class ProductCategoryService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllProductCategory');
  }

  save(payload: any) {
    if (payload.serProductCategoryId) {
      return this.http.post(urls.API_URL + 'updateProductCategory', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewProductCategory', payload,{ responseType: 'text' });
    }
  }
}
