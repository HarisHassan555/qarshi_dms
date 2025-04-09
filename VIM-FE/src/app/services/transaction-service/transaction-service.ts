import {HttpClient, HttpParams} from "@angular/common/http";
import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {urls} from "../../utils/urls";

@Injectable({
    providedIn: 'root'
})
export class TransactionService {

    constructor(private http: HttpClient) {}

    getTransactionsByDateAndStatus(startDate: string, endDate: string, status: string): Observable<any[]> {
        const startDateParam = new Date(startDate);
        const endDateParam = new Date(endDate);

        const params = new HttpParams()
            .set('startDate', startDateParam.toISOString())
            .set('endDate', endDateParam.toISOString());
      //  const params = { startDate, endDate, status };
        return this.http.get<any[]>(urls.API_URL + 'transaction-details', { params });
    }

    getHoldTransactionsByDateAndStatus(startDate: string, endDate: string, status: string): Observable<any[]> {
        const startDateParam = new Date(startDate);
        const endDateParam = new Date(endDate);

        const params = new HttpParams()
            .set('startDate', startDateParam.toISOString())
            .set('endDate', endDateParam.toISOString());
        //  const params = { startDate, endDate, status };
        return this.http.get<any[]>(urls.API_URL + 'hold-transaction', { params });
    }

    getApprovedTransactionsByDateAndStatus(startDate: string, endDate: string, status: string): Observable<any[]> {
        const startDateParam = new Date(startDate);
        const endDateParam = new Date(endDate);

        const params = new HttpParams()
            .set('startDate', startDateParam.toISOString())
            .set('endDate', endDateParam.toISOString());
        //  const params = { startDate, endDate, status };
        return this.http.get<any[]>(urls.API_URL + 'approved-transaction', { params });
    }

    getCancelledTransactionsByDateAndStatus(startDate: string, endDate: string, status: string): Observable<any[]> {
        const startDateParam = new Date(startDate);
        const endDateParam = new Date(endDate);

        const params = new HttpParams()
            .set('startDate', startDateParam.toISOString())
            .set('endDate', endDateParam.toISOString());
        //  const params = { startDate, endDate, status };
        return this.http.get<any[]>(urls.API_URL + 'cancel-transaction', { params });
    }
}
