import {HttpClient, HttpParams} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class SaleOrderService {

  constructor(
    private http: HttpClient
  ) { }

    getAll = (params: { [x: string]: string | number | boolean; // @ts-ignore
        page?: number; // @ts-ignore
        size?: number; search?: string; } | undefined ) => {
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

        // Convert params object into HttpParams
        let httpParams = new HttpParams();
        // @ts-ignore
        Object.keys(params).forEach(key => {
            if (params) {
                httpParams = httpParams.set(key, params[key]);
            }
        });

        return this.http.post(urls.API_URL + 'searchDeal', payload,{ params: httpParams });
    };

    getSaleOrderDetail = (id: any) => {
        const payload = {
            id: id
        };

        return this.http.post(urls.API_URL + 'searchDealDetail', id);
    };


    getSaleOrder = (numLevel: any) => {
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
            numLevel:numLevel///,
           // txtStatus:'APPROVED'
        };

        return this.http.post(urls.API_URL + 'searchSaleOrder', payload);
    };


    getAllSaleOrder = () => {
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
           /* numLevel:numLevel,
            txtStatus:'pending'*/
        };

        return this.http.post(urls.API_URL + 'searchSaleOrder', payload);
    };


    getSaleOrderDetailList = (id: any) => {
        return this.http.post(urls.API_URL + 'searchSaleOrderDetail', id);
    };

    updateSaleOrder = (payload: any) => {

        const url = urls.API_URL + 'ApproveSaleOrderinListWithDateandLevel';
        return this.http.post(url, payload, {
            // Uncomment headers and options if needed for multipart/form-data
            // headers: { 'Content-Type': 'multipart/form-data' },
            observe: 'response'
        });

    };


    updateSaleOrderFromSap = (payload: any) => {
        // Return the observable from the HTTP POST request
        return this.http.post(urls.API_URL + 'updateSaleOrderSAP', payload,{ responseType: 'text' });
    };
    /*updateSaleOrderFromSap = (payload: any) => {


        this.http.post(urls.API_URL + 'updateSaleOrderSAP', payload, {
            /!* headers: { 'Content-Type': 'multipart/form-data' },
             observe: 'response'*!/
        }).subscribe((response: any) => {
            console.log('Response:', response);

        }, error => {
            console.error('Error:', error);

        });

    };*/


    searchSaleOrder(dateFrom: Date | null, dateTo: Date | null) {

        if (dateFrom && dateTo && !isNaN(dateFrom.getTime()) && !isNaN(dateTo.getTime())) {
            const formatDate = (date: Date): string => {
                const day = String(date.getDate()).padStart(2, '0');
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const year = date.getFullYear();
                return `${day}-${month}-${year}`;
            };

            const payload = {
                dte_date_from: formatDate(dateFrom),
                dte_date_to: formatDate(dateTo),
            };

            return this.http.post(urls.API_URL + 'searchDeal', payload);
        } else {
            throw new Error('Invalid date objects');
        }
    }

    searchSaleInvoice(dateFrom: Date | null, dateTo: Date | null) {

        if (dateFrom && dateTo && !isNaN(dateFrom.getTime()) && !isNaN(dateTo.getTime())) {
            const formatDate = (date: Date): string => {
                const day = String(date.getDate()).padStart(2, '0');
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const year = date.getFullYear();
                return `${day}-${month}-${year}`;
            };

            const payload = {
                dte_date_from: formatDate(dateFrom),
                dte_date_to: formatDate(dateTo),
            };

            return this.http.post(urls.API_URL + 'searchSaleOrder', payload);
        } else {
            throw new Error('Invalid date objects');
        }
    }


    searchSaleInvoiceByDepartment(dateFrom: Date | null, dateTo: Date | null,numLeveL:string |null) {

        if (dateFrom && dateTo && !isNaN(dateFrom.getTime()) && !isNaN(dateTo.getTime())) {
            const formatDate = (date: Date): string => {
                const day = String(date.getDate()).padStart(2, '0');
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const year = date.getFullYear();
                return `${day}-${month}-${year}`;
            };

            const payload = {
                dte_date_from: formatDate(dateFrom),
                dte_date_to: formatDate(dateTo),
                numLevel: numLeveL
            };

            return this.http.post(urls.API_URL + 'searchSaleOrderByDepartment', payload,{ responseType: 'text' });
        } else {
            throw new Error('Invalid date objects');
        }
    }


    getSaleOrderDetailAudit = (id: any) => {
        return this.http.post(urls.API_URL + 'searchSaleOrderAudit', id);
    };


    // @ts-ignore
    getUploadedFile(saleOrderId: string): Observable<UploadedFile> {
        const url = urls.API_URL + 'getCandidateDocument';
        // @ts-ignore
        return this.http.post<UploadedFile>(url,saleOrderId);
    }


    markAsClose(payload: any) {

        return this.http.post(urls.API_URL + 'closeDeal', payload,{ responseType: 'text' });
        /*if (dateFrom && dateTo && !isNaN(dateFrom.getTime()) && !isNaN(dateTo.getTime())) {
            const formatDate = (date: Date): string => {
                const day = String(date.getDate()).padStart(2, '0');
                const month = String(date.getMonth() + 1).padStart(2, '0');
                const year = date.getFullYear();
                return `${day}-${month}-${year}`;
            };

            const payload = {
                dte_date_from: formatDate(dateFrom),
                dte_date_to: formatDate(dateTo),
            };

            return this.http.post(urls.API_URL + 'searchDeal', payload);
        } else {
            throw new Error('Invalid date objects');
        }*/
    }

     savePdf(pdfBytes: BlobPart, documentId: any ) {
         // @ts-ignore
         const blob = new Blob([pdfBytes], {type: 'application/pdf'});
         const formData = new FormData();
         formData.append('pdf', blob, 'modified.pdf');
         // @ts-ignore
         formData.append('fileId', documentId); // Add the file ID

         this.http.post(urls.API_URL + 'uploadDocument', formData, {
             /* headers: { 'Content-Type': 'multipart/form-data' },*/
             observe: 'response'
         }).subscribe((response: any) => {
             console.log('Response:', response);
            // return response;
         }, error => {
             console.error('Error:', error);
           //  return error;
         });

       /* const response = this.http.post(urls.API_URL + 'uploadDocument', {
            method: 'POST',
            body: formData,
        });*/

        /*if (response.ok) {
           // alert(`PDF saved successfully: ${result.filename}`);
        } else {
           // alert('Error saving PDF');
        }*/

    }

}
