import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class CountryService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllCountry');
  }

  getActiveCountries() {
    return this.http.get(urls.API_URL + 'getActiveCountry');
  }

  save(payload: any) {
    if (payload.serCountryId) {
      return this.http.post(urls.API_URL + 'updateCountry', payload,{ responseType: 'text' });
    } else {
      return this.http.post(urls.API_URL + 'addNewCountry', payload,{ responseType: 'text' });
    }
  }

}
