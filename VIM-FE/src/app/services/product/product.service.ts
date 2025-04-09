import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllProduct');
  }

  getActiveProducts() {
    return this.http.get(urls.API_URL + 'getActiveProduct');
  }

  save(payload: any) {
    if (payload.serProductId) {
      return this.http.post(urls.API_URL + 'updateProduct', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewProduct', payload,{ responseType: 'text' });
    }
  }

  delete(payload: any) {
    return this.http.post(urls.API_URL + 'deleteProduct', payload)
  }
}
