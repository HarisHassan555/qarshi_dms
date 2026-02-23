import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';

interface Application {
  serApplicationId?: number;
  serFormId?: number;
  txtFormCode?: string;
  txtStatus?: string;
  intCurrentApprovalLevel?: number;
  dteCreatedDate?: string;
  cfgTblCustomForm?: any;
  formName?: string;
}

@Component({
  selector: 'app-pending-approvals',
  templateUrl: './pending-approvals.component.html',
  styleUrls: ['./pending-approvals.component.css']
})
export class PendingApprovalsComponent implements OnInit {
  search = '';
  pendingApprovals: Application[] = [];
  forms: any[] = [];

  cols = [
    { field: 'txtFormCode', title: 'Application Code' },
    { field: 'formName', title: 'Form Name' },
    { field: 'intCurrentApprovalLevel', title: 'Approval Level' },
    { field: 'dteCreatedDate', title: 'Submitted Date' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private customFormApplicationService: CustomFormApplicationService,
    private customFormService: CustomFormService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadForms();
    this.loadPendingApprovals();
  }

  loadForms() {
    this.customFormService.getAll().subscribe(
      (data: any) => {
        if (data) {
          this.forms = data;
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading forms: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  loadPendingApprovals() {
    this.customFormApplicationService.getAllApplicationsPendingApproval().subscribe(
      (data: any) => {
        if (data) {
          this.pendingApprovals = data.map((app: any) => ({
            ...app,
            formName: this.getFormName(app)
          }));
        } else {
          this.pendingApprovals = [];
        }
      },
      (error) => {
        this.notificationService.showMessage('Error loading pending approvals: ' + (error.error?.message || error.message), 'danger');
      }
    );
  }

  getFormName(app: any): string {
    if (app.cfgTblCustomForm && app.cfgTblCustomForm.txtFormName) {
      return app.cfgTblCustomForm.txtFormName;
    }
    if (app.serFormId && this.forms && this.forms.length > 0) {
      const form = this.forms.find(f => f.serFormId === app.serFormId);
      if (form && form.txtFormName) return form.txtFormName;
    }
    return app.serFormId ? 'Unknown Form' : 'N/A';
  }

  getDisplayedPendingApprovals(): Application[] {
    if (!this.search) return this.pendingApprovals;
    const searchLower = this.search.toLowerCase();
    return this.pendingApprovals.filter(app =>
      (app.txtFormCode && app.txtFormCode.toLowerCase().includes(searchLower)) ||
      (app.formName && app.formName.toLowerCase().includes(searchLower)) ||
      (app.intCurrentApprovalLevel !== undefined && app.intCurrentApprovalLevel !== null &&
        String(app.intCurrentApprovalLevel).toLowerCase().includes(searchLower)) ||
      (app.dteCreatedDate && new Date(app.dteCreatedDate).toLocaleString().toLowerCase().includes(searchLower))
    );
  }

  viewApplication(application: Application) {
    if (!application.serApplicationId) {
      this.notificationService.showMessage('Invalid application ID', 'danger');
      return;
    }
    this.router.navigate(['/application-details', application.serApplicationId], {
      queryParams: { from: 'pending' }
    });
  }
}
