import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';
import { finalize } from 'rxjs/operators';

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
  currentUser: any;
  isLoading = false;

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
    const userJson = localStorage.getItem('user');
    if (userJson) {
      try {
        this.currentUser = JSON.parse(userJson);
      } catch (e) {
        this.currentUser = null;
      }
    }
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
    this.isLoading = true;
    const userId = Number(this.currentUser?.serUserId || 0);

    if (userId <= 0) {
      this.pendingApprovals = [];
      this.isLoading = false;
      this.notificationService.showMessage('User context is missing. Please sign in again.', 'warning');
      return;
    }

    const request = this.customFormApplicationService.getApplicationsPendingApproval(userId);

    request.pipe(finalize(() => this.isLoading = false)).subscribe(
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
