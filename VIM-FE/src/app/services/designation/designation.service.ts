import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class DesignationService {

  constructor(
    private http: HttpClient
  ) { }

  getAll() {
    return this.http.get(urls.API_URL + 'getAllDesignations');
  }
}
