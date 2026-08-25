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

type DepartmentApplicationCsvColumnKey =
  'txtFormCode' | 'formName' | 'submittedByUserName' | 'submittedDepartmentName' | 'txtStatus' | 'dteCreatedDate';

interface DepartmentApplicationCsvColumn {
  key: DepartmentApplicationCsvColumnKey;
  label: string;
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
  showCsvModal = false;
  availableCsvColumns: DepartmentApplicationCsvColumn[] = [];
  selectedCsvColumns: DepartmentApplicationCsvColumn[] = [];
  readonly csvColumns: DepartmentApplicationCsvColumn[] = [
    { key: 'txtFormCode', label: 'Application Code' },
    { key: 'formName', label: 'Form Name' },
    { key: 'submittedByUserName', label: 'Submitted By' },
    { key: 'submittedDepartmentName', label: 'Department' },
    { key: 'txtStatus', label: 'Status' },
    { key: 'dteCreatedDate', label: 'Submitted Date' }
  ];

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

  openCsvModal(): void {
    if (this.displayedApplications.length === 0) {
      this.notificationService.showMessage('No department applications to download', 'warning');
      return;
    }
    this.availableCsvColumns = [...this.csvColumns];
    this.selectedCsvColumns = [];
    this.showCsvModal = true;
  }

  closeCsvModal(): void {
    this.showCsvModal = false;
  }

  selectAllCsvColumns(): void {
    this.availableCsvColumns = [];
    this.selectedCsvColumns = [...this.csvColumns];
  }

  unselectAllCsvColumns(): void {
    this.availableCsvColumns = [...this.csvColumns];
    this.selectedCsvColumns = [];
  }

  addCsvColumn(column: DepartmentApplicationCsvColumn): void {
    if (!this.selectedCsvColumns.some((item) => item.key === column.key)) {
      this.availableCsvColumns = this.availableCsvColumns.filter((item) => item.key !== column.key);
      this.selectedCsvColumns = [...this.selectedCsvColumns, column];
    }
  }

  removeCsvColumn(column: DepartmentApplicationCsvColumn): void {
    this.selectedCsvColumns = this.selectedCsvColumns.filter((item) => item.key !== column.key);
    const restoredColumns = [...this.availableCsvColumns, column];
    this.availableCsvColumns = this.csvColumns.filter((candidate) => restoredColumns.some((item) => item.key === candidate.key));
  }

  downloadCsv(): void {
    if (!this.selectedCsvColumns.length) {
      this.notificationService.showMessage('Select at least one column before downloading CSV.', 'warning');
      return;
    }
    const csvRows = [
      this.selectedCsvColumns.map((column) => this.escapeCsvValue(column.label)).join(','),
      ...this.displayedApplications.map((app) => this.selectedCsvColumns
        .map((column) => this.escapeCsvValue(this.getCsvCellValue(app, column.key)))
        .join(','))
    ];
    this.downloadBlob(
      new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' }),
      `department-applications_${new Date().toISOString().split('T')[0]}.csv`
    );
    this.showCsvModal = false;
  }

  trackByCsvColumn(index: number, column: DepartmentApplicationCsvColumn): DepartmentApplicationCsvColumnKey {
    return column.key;
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

  private getCsvCellValue(application: DepartmentApplication, key: DepartmentApplicationCsvColumnKey): string {
    switch (key) {
      case 'txtFormCode':
        return application.txtFormCode || '';
      case 'formName':
        return application.formName || '';
      case 'submittedByUserName':
        return application.submittedByUserName || '';
      case 'submittedDepartmentName':
        return application.submittedDepartmentName || '';
      case 'txtStatus':
        return application.txtStatus || '';
      case 'dteCreatedDate':
        return application.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleString() : '';
      default:
        return '';
    }
  }

  private escapeCsvValue(value: any): string {
    const normalized = String(value ?? '');
    return `"${normalized.replace(/"/g, '""')}"`;
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(objectUrl);
  }
}
