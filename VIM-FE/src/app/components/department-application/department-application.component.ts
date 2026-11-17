import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { NotificationService } from 'src/app/NotificationService';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { resolveApplicationTitle } from 'src/app/utils/application-title.util';

interface DepartmentApplication {
  serApplicationId?: number;
  serSubmittedBy?: number;
  serFormId?: number;
  txtFormCode?: string;
  txtStatus?: string;
  txtApplicationData?: string;
  dteCreatedDate?: string;
  cfgTblCustomForm?: any;
  formName?: string;
  submittedDepartmentName?: string;
  submittedByUserName?: string;
  applicationTitle?: string;
}

type DepartmentApplicationCsvColumnKey =
  'txtFormCode' | 'applicationTitle' | 'formName' | 'submittedByUserName' | 'submittedDepartmentName' | 'txtStatus' | 'dteCreatedDate';

interface DepartmentApplicationCsvColumn {
  key: DepartmentApplicationCsvColumnKey;
  label: string;
}

interface DepartmentApplicationMetric {
  label: string;
  value: number;
  tone: 'slate' | 'amber' | 'emerald' | 'rose' | 'sky';
  hint: string;
}

@Component({
  selector: 'app-department-application',
  templateUrl: './department-application.component.html',
  styleUrls: ['./department-application.component.css']
})
export class DepartmentApplicationComponent implements OnInit, OnDestroy {
  search = '';
  filterStartDate = '';
  filterEndDate = '';
  filterStatus = '';
  filterFormType = '';
  availableStatuses: string[] = [];
  availableFormTypes: string[] = [];
  applicationMetrics: DepartmentApplicationMetric[] = [];
  applications: DepartmentApplication[] = [];
  displayedApplications: DepartmentApplication[] = [];
  currentUser: any;
  isLoading = false;
  openingApplicationId: number | null = null;
  showCsvModal = false;
  availableCsvColumns: DepartmentApplicationCsvColumn[] = [];
  selectedCsvColumns: DepartmentApplicationCsvColumn[] = [];
  readonly csvColumns: DepartmentApplicationCsvColumn[] = [
    { key: 'txtFormCode', label: 'Application Code' },
    { key: 'applicationTitle', label: 'Title' },
    { key: 'formName', label: 'Form Name' },
    { key: 'submittedByUserName', label: 'Submitted By' },
    { key: 'submittedDepartmentName', label: 'Department' },
    { key: 'txtStatus', label: 'Status' },
    { key: 'dteCreatedDate', label: 'Submitted Date' }
  ];

  cols = [
    { field: 'txtFormCode', title: 'Application Code' },
    { field: 'applicationTitle', title: 'Title' },
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
    this.applicationMetrics = this.buildApplicationMetrics([]);
    this.loadDepartmentApplications();
    window.addEventListener('velocity-tour-prepare', this.onTourPrepare);
  }

  ngOnDestroy(): void {
    window.removeEventListener('velocity-tour-prepare', this.onTourPrepare);
  }

  private onTourPrepare = (event: Event): void => {
    const action = (event as CustomEvent<{ action?: string }>).detail?.action;
    if (action === 'dept-app-close-csv') {
      this.closeCsvModal();
    } else if (action === 'dept-app-open-csv' && this.displayedApplications.length > 0) {
      this.openCsvModal();
    }
  };

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
              applicationTitle: resolveApplicationTitle(app),
            }));
          } else {
            this.applications = [];
          }
          this.availableStatuses = this.buildDistinctOptions(this.applications.map((item) => item.txtStatus || ''));
          this.availableFormTypes = this.buildDistinctOptions(this.applications.map((item) => item.formName || ''));
          this.applySearchFilter();
        },
        (error) => {
          this.applications = [];
          this.displayedApplications = [];
          this.applicationMetrics = this.buildApplicationMetrics([]);
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

  applyApplicationFilters(): void {
    this.applySearchFilter();
  }

  clearApplicationFilters(): void {
    this.search = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.filterStatus = '';
    this.filterFormType = '';
    this.applySearchFilter();
  }

  getMetricClasses(tone: DepartmentApplicationMetric['tone']): string {
    switch (tone) {
      case 'amber':
        return 'border-amber-200 bg-amber-50 text-amber-900';
      case 'emerald':
        return 'border-emerald-200 bg-emerald-50 text-emerald-900';
      case 'rose':
        return 'border-rose-200 bg-rose-50 text-rose-900';
      case 'sky':
        return 'border-sky-200 bg-sky-50 text-sky-900';
      default:
        return 'border-slate-200 bg-white text-slate-900';
    }
  }

  applySearchFilter(): void {
    const searchLower = this.search.trim().toLowerCase();
    this.displayedApplications = this.applications.filter((app) => {
      if (this.filterStatus && (app.txtStatus || '') !== this.filterStatus) {
        return false;
      }
      if (this.filterFormType && (app.formName || '') !== this.filterFormType) {
        return false;
      }
      const createdAt = new Date(app.dteCreatedDate || '').getTime();
      if (this.filterStartDate) {
        const startMs = new Date(`${this.filterStartDate}T00:00:00`).getTime();
        if (Number.isFinite(startMs) && (!Number.isFinite(createdAt) || createdAt < startMs)) {
          return false;
        }
      }
      if (this.filterEndDate) {
        const endMs = new Date(`${this.filterEndDate}T23:59:59.999`).getTime();
        if (Number.isFinite(endMs) && (!Number.isFinite(createdAt) || createdAt > endMs)) {
          return false;
        }
      }
      if (!searchLower) {
        return true;
      }
      return (
        (app.txtFormCode && app.txtFormCode.toLowerCase().includes(searchLower)) ||
        (app.applicationTitle && app.applicationTitle.toLowerCase().includes(searchLower)) ||
        (app.formName && app.formName.toLowerCase().includes(searchLower)) ||
        (app.submittedByUserName && app.submittedByUserName.toLowerCase().includes(searchLower)) ||
        (app.submittedDepartmentName && app.submittedDepartmentName.toLowerCase().includes(searchLower)) ||
        (app.txtStatus && app.txtStatus.toLowerCase().includes(searchLower)) ||
        (app.dteCreatedDate && new Date(app.dteCreatedDate).toLocaleString().toLowerCase().includes(searchLower))
      );
    });
    this.applicationMetrics = this.buildApplicationMetrics(this.displayedApplications);
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
      case 'DRAFT':
        return 'badge-outline-secondary';
      case 'PENDING':
      case 'HOLD':
        return 'badge-outline-warning';
      case 'APPROVED':
      case 'COMPLETED':
        return 'badge-outline-success';
      case 'REJECTED':
        return 'badge-outline-danger';
      case 'IN_PROGRESS':
      case 'OPINION_PENDING':
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

  private buildApplicationMetrics(items: DepartmentApplication[]): DepartmentApplicationMetric[] {
    const draft = items.filter((item) => this.isDraftStatus(item.txtStatus)).length;
    const pending = items.filter((item) => this.isOpenWorkflowStatus(item.txtStatus)).length;
    const completed = items.filter((item) => this.isCompletedStatus(item.txtStatus)).length;
    const rejected = items.filter((item) => this.isRejectedStatus(item.txtStatus)).length;
    return [
      {
        label: 'Total Applications',
        value: items.length - draft,
        tone: 'slate',
        hint: 'Submitted applications in the current filters'
      },
      {
        label: 'Draft Applications',
        value: draft,
        tone: 'sky',
        hint: 'Saved as draft and not yet submitted'
      },
      {
        label: 'Pending In Workflow',
        value: pending,
        tone: 'amber',
        hint: 'Applications still moving through approval'
      },
      {
        label: 'Completed',
        value: completed,
        tone: 'emerald',
        hint: 'Applications that reached final approval'
      },
      {
        label: 'Rejected',
        value: rejected,
        tone: 'rose',
        hint: 'Applications stopped with rejection'
      }
    ];
  }

  private isOpenWorkflowStatus(status: string | undefined): boolean {
    const normalizedStatus = String(status || '').trim().toUpperCase();
    return normalizedStatus === 'PENDING'
      || normalizedStatus === 'IN_PROGRESS'
      || normalizedStatus === 'OPINION_PENDING'
      || normalizedStatus === 'CEO_PENDING'
      || normalizedStatus === 'ASSET_PENDING'
      || normalizedStatus === 'PR_PENDING'
      || normalizedStatus === 'PO_PENDING'
      || normalizedStatus === 'PO_VENDOR_TE_PENDING';
  }

  private isCompletedStatus(status: string | undefined): boolean {
    const normalizedStatus = String(status || '').trim().toUpperCase();
    return normalizedStatus === 'APPROVED' || normalizedStatus === 'COMPLETED';
  }

  private isRejectedStatus(status: string | undefined): boolean {
    return String(status || '').trim().toUpperCase() === 'REJECTED';
  }

  private isDraftStatus(status: string | undefined): boolean {
    return String(status || '').trim().toUpperCase() === 'DRAFT';
  }

  private buildDistinctOptions(values: string[]): string[] {
    return Array.from(new Set(
      values.map((value) => String(value || '').trim()).filter((value) => !!value)
    )).sort((left, right) => left.localeCompare(right));
  }

  isOpeningApplication(application: DepartmentApplication): boolean {
    return !!application.serApplicationId && this.openingApplicationId === application.serApplicationId;
  }

  viewApplication(application: DepartmentApplication) {
    if (!application.serApplicationId) {
      this.notificationService.showMessage('Invalid application ID', 'danger');
      return;
    }

    this.openingApplicationId = application.serApplicationId;
    const formDescription = (application.cfgTblCustomForm?.txtFormDescription || '').toString().trim().toLowerCase();
    const navigation = formDescription === 'template-builder'
      ? this.router.navigate(['/my-application', application.serApplicationId], {
        queryParams: { from: 'department-application' }
      })
      : this.router.navigate(['/application-details', application.serApplicationId], {
        queryParams: { from: 'department-application' }
      });

    navigation.then((opened) => {
      if (!opened) {
        this.openingApplicationId = null;
      }
    }).catch(() => {
      this.openingApplicationId = null;
      this.notificationService.showMessage('Unable to open application. Please try again.', 'danger');
    });
  }

  private getCsvCellValue(application: DepartmentApplication, key: DepartmentApplicationCsvColumnKey): string {
    switch (key) {
      case 'txtFormCode':
        return application.txtFormCode || '';
      case 'applicationTitle':
        return application.applicationTitle || '';
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
