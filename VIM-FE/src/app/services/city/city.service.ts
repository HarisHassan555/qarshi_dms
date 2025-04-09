import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class CityService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllCity');
  }

  getActiveCities() {
    return this.http.get(urls.API_URL + 'getActiveCity');
  }

  save(payload: any) {
    if (payload.serCityId) {
      return this.http.post(urls.API_URL + 'updateCity', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewCity', payload,{ responseType: 'text' });
    }
  }
}
