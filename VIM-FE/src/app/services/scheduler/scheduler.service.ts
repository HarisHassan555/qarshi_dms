import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

@Injectable({
  providedIn: 'root'
})
export class SschedulerService {

  constructor(
    private http: HttpClient
  ) { }

    getAllLog = () => {
        // @ts-ignore
        return this.http.get(urls.API_URL + 'allLogs');
    };

    startJob = (startDate: string) => {
        const payload = { erDate: startDate };
        return this.http.post(urls.API_URL + 'sendPost', payload,{ responseType: 'text' });
    }

    getSaleOrderDetail = (id: any) => {
        const payload = {
            id: id
        };

        return this.http.post(urls.API_URL + 'searchDealDetail', id);
    };


    getSaleOrder = () => {
        const currentDate = new Date();
        const eightDaysAgo = new Date();
        eightDaysAgo.setDate(currentDate.getDate() - 8);

        // @ts-ignore
        const formatDate = (date) => {
            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const year = date.getFullYear();
            return `${day}-${month}-${year}`;
        };
        const payload = {
            dte_date_from: formatDate(eightDaysAgo),
            dte_date_to: formatDate(currentDate),
        };

        return this.http.post(urls.API_URL + 'searchSaleOrder', payload);
    };


    getSaleOrderDetailList = (id: any) => {
        return this.http.post(urls.API_URL + 'searchSaleOrderDetail', id);
    };

    updateSaleOrder = (payload: any) => {


        this.http.post(urls.API_URL + 'ApproveSaleOrderinListWithDateandLevel', payload, {
           /* headers: { 'Content-Type': 'multipart/form-data' },
            observe: 'response'*/
        }).subscribe((response: any) => {
            console.log('Response:', response);

        }, error => {
            console.error('Error:', error);

        });

    };


    updateSaleOrderFromSap = (payload: any) => {


        this.http.post(urls.API_URL + 'updateSaleOrderSAP', payload, {
            /* headers: { 'Content-Type': 'multipart/form-data' },
             observe: 'response'*/
        }).subscribe((response: any) => {
            console.log('Response:', response);

        }, error => {
            console.error('Error:', error);

        });

    };

    startWorkFlowJob = () => {
        /*const payload = { erDate: startDate };*/
        return this.http.get(urls.API_URL + 'workFlowSap',{ responseType: 'text' });
    }

    getAllSESLog = () => {
        // @ts-ignore
        return this.http.get(urls.API_URL + 'sesLog');
    };
}
