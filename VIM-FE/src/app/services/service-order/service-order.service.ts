import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class ServiceOrderService {

  constructor(
    private http: HttpClient
  ) { }

  getDealNo() {
    return this.http.get(urls.API_URL + 'generateDealNo');
  }

  save(payload: any) {
    return this.http.post(urls.API_URL + 'addNewDeal', payload,{ responseType: 'text' });
  }
}
