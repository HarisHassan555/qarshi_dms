import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { NotificationService } from 'src/app/NotificationService';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';

interface DepartmentApplication {
  serApplicationId?: number;
  serSubmittedBy?: number;
  serFormId?: number;
  txtFormCode?: string;
  txtStatus?: string;
  dteCreatedDate?: string;
  cfgTblCustomForm?: any;
  formName?: string;
  submittedDepartmentName?: string;
  submittedByUserName?: string;
}

@Component({
  selector: 'app-department-application',
  templateUrl: './department-application.component.html',
  styleUrls: ['./department-application.component.css']
})
export class DepartmentApplicationComponent implements OnInit {
  search = '';
  applications: DepartmentApplication[] = [];
  displayedApplications: DepartmentApplication[] = [];
  currentUser: any;
  isLoading = false;

  cols = [
    { field: 'txtFormCode', title: 'Application Code' },
    { field: 'formName', title: 'Form Name' },
    { field: 'submittedByUserName', title: 'Submitted By' },
    { field: 'submittedDepartmentName', title: 'Department' },
    { field: 'txtStatus', title: 'Status' },
    { field: 'dteCreatedDate', title: 'Submitted Date' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private customFormApplicationService: CustomFormApplicationService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit() {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      this.currentUser = JSON.parse(userJson);
    }
    this.loadDepartmentApplications();
  }

  loadDepartmentApplications() {
    if (!this.currentUser?.serUserId) {
      this.applications = [];
      this.displayedApplications = [];
      return;
    }

    this.isLoading = true;
    this.customFormApplicationService
      .getDepartmentApplications(this.currentUser.serUserId)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe(
        (data: any) => {
          if (data) {
            this.applications = data.map((app: any) => ({
              ...app,
              formName: this.getFormName(app),
            }));
          } else {
            this.applications = [];
          }
          this.applySearchFilter();
        },
        (error) => {
          this.applications = [];
          this.displayedApplications = [];
          this.notificationService.showMessage(
            'Error loading department applications: ' + (error.error?.message || error.message),
            'danger'
          );
        }
      );
  }

  getFormName(app: any): string {
    if (app.cfgTblCustomForm && app.cfgTblCustomForm.txtFormName) {
      return app.cfgTblCustomForm.txtFormName;
    }
    return app.serFormId ? 'Unknown Form' : 'N/A';
  }

  applySearchFilter(): void {
    if (!this.search) {
      this.displayedApplications = [...this.applications];
      return;
    }
    const searchLower = this.search.toLowerCase();
    this.displayedApplications = this.applications.filter((app) =>
      (app.txtFormCode && app.txtFormCode.toLowerCase().includes(searchLower)) ||
      (app.formName && app.formName.toLowerCase().includes(searchLower)) ||
      (app.submittedByUserName && app.submittedByUserName.toLowerCase().includes(searchLower)) ||
      (app.submittedDepartmentName && app.submittedDepartmentName.toLowerCase().includes(searchLower)) ||
      (app.txtStatus && app.txtStatus.toLowerCase().includes(searchLower)) ||
      (app.dteCreatedDate && new Date(app.dteCreatedDate).toLocaleString().toLowerCase().includes(searchLower))
    );
  }

  getStatusBadgeClass(status: string | undefined): string {
    if (!status) return 'badge-outline-secondary';
    switch (status.toUpperCase()) {
      case 'PENDING':
        return 'badge-outline-warning';
      case 'APPROVED':
        return 'badge-outline-success';
      case 'REJECTED':
        return 'badge-outline-danger';
      case 'IN_PROGRESS':
      case 'CEO_PENDING':
      case 'ASSET_PENDING':
      case 'PR_PENDING':
      case 'PO_PENDING':
      case 'PO_VENDOR_TE_PENDING':
        return 'badge-outline-info';
      default:
        return 'badge-outline-secondary';
    }
  }

  viewApplication(application: DepartmentApplication) {
    if (!application.serApplicationId) {
      this.notificationService.showMessage('Invalid application ID', 'danger');
      return;
    }

    const formDescription = (application.cfgTblCustomForm?.txtFormDescription || '').toString().trim().toLowerCase();
    if (formDescription === 'template-builder') {
      this.router.navigate(['/my-application', application.serApplicationId], {
        queryParams: { from: 'department-application' }
      });
      return;
    }

    this.router.navigate(['/application-details', application.serApplicationId], {
      queryParams: { from: 'department-application' }
    });
  }
}
