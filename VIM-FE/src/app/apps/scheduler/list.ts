import { Component, ViewChild } from '@angular/core';
import {SaleOrderService} from "../../services/saleorder/sale-order.service";
import {SschedulerService} from "../../services/scheduler/scheduler.service";
import {NotificationService} from "../../NotificationService";

@Component({
    moduleId: module.id,
    templateUrl: './list.html',
})

export class SchedulerComponent {

    @ViewChild ('datatable') datatable: any;
    search = '';
    saleOrderLst: any;
    items:any;
    selectedDate: string = '';
    isDateValid: boolean = false;

    validateDate() {
        this.isDateValid = !!this.selectedDate;
    }

    cols = [
        { field: 'id', title: 'Sr No' },
        { field: 'executionTime', title: 'Execution Time' },
        { field: 'status', title: 'Status' },
    ];

    constructor(private saleOrderService: SaleOrderService,private schedulerService:SschedulerService,private notificationService: NotificationService) {
        this.selectedDate = '';
    }

    ngOnInit() {
        this.schedulerService.getAllLog().subscribe((data: any) => {
            if (data) {
                this.items = data;
                console.log(this.items)
            }
        });
    }



    startJob(date :string) {
        this.validateDate();
        if(this.isDateValid && this.selectedDate) {
            this.schedulerService
                .startJob(date)
                .subscribe((data: any) => {
                    if (data == "success") {
                        this.notificationService.showMessage('Record saved successfully', 'success');
                        this.ngOnInit();
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');
                        this.ngOnInit();
                    }
                });
        }else {
            this.notificationService.showMessage('Please select a valid date before starting the job', 'danger');
        }
    }


    startWorkFlowJob() {
        /*this.validateDate();
        if(this.isDateValid && this.selectedDate) {*/
            this.schedulerService
                .startWorkFlowJob()
                .subscribe((data: any) => {
                    if (data == "Success") {
                        this.notificationService.showMessage('Record saved successfully', 'success');
                        this.ngOnInit();
                    } else {
                        this.notificationService.showMessage('Error occurred while saving', 'danger');
                        this.ngOnInit();
                    }
                });
        }/*else {
            this.notificationService.showMessage('Please select a valid date before starting the job', 'danger');
        }*/


}
