import { Component } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {TransactionService} from "../services/transaction-service/transaction-service";
import {Location} from "@angular/common";

@Component({
  selector: 'app-transactions-details',
  templateUrl: './transactions-details.component.html',
  styleUrls: ['./transactions-details.component.css']
})
export class TransactionsDetailsComponent {

    transactions: any[] = [];
    startDate: string | null = null;
    endDate: string | null = null;
    status: string | null = null;
    title: string = 'Transactions';
    constructor(
        private route: ActivatedRoute,
        private transactionService: TransactionService,private location: Location
    ) {}

    ngOnInit(): void {
        this.route.queryParams.subscribe((params) => {
            this.startDate = params['startDate'] || null;
            this.endDate = params['endDate'] || null;
            this.status = params['status'] || null;

            if (this.startDate && this.endDate && (this.status == 'PENDING')) {
                this.fetchPendingTransactions(this.startDate, this.endDate, this.status);
                this.title = 'Pending Transactions';
            }else if(this.status == 'Hold'){
                // @ts-ignore
                this.fetchHoldTransactions(this.startDate, this.endDate, this.status);
                this.title = 'Hold Transactions';
            }else if(this.status == 'Approved'){
                // @ts-ignore
                this.fetchApprovedTransactions(this.startDate, this.endDate, this.status);
                this.title = 'Approved Transactions';
            }else{
                // @ts-ignore
                this.fetchCancelTransactions(this.startDate, this.endDate, this.status);
                this.title = 'Cancelled Transactions';
            }
        });
    }

    fetchPendingTransactions(startDate: string, endDate: string, status: string): void {
        this.transactionService.getTransactionsByDateAndStatus(startDate, endDate, status).subscribe(
            (transactions) => {
                this.transactions = transactions;
            },
            (error) => {
                console.error('Error fetching transactions:', error);
            }
        );
    }

    fetchHoldTransactions(startDate: string, endDate: string, status: string): void {
        this.transactionService.getHoldTransactionsByDateAndStatus(startDate, endDate, status).subscribe(
            (transactions) => {
                this.transactions = transactions;
            },
            (error) => {
                console.error('Error fetching transactions:', error);
            }
        );
    }

    fetchApprovedTransactions(startDate: string, endDate: string, status: string): void {
        this.transactionService.getApprovedTransactionsByDateAndStatus(startDate, endDate, status).subscribe(
            (transactions) => {
                this.transactions = transactions;

            },
            (error) => {
                console.error('Error fetching transactions:', error);
            }
        );
    }

    fetchCancelTransactions(startDate: string, endDate: string, status: string): void {
        this.transactionService.getCancelledTransactionsByDateAndStatus(startDate, endDate, status).subscribe(
            (transactions) => {
                this.transactions = transactions;
            },
            (error) => {
                console.error('Error fetching transactions:', error);
            }
        );
    }

    goBack(): void {
        this.location.back();
    }


}
