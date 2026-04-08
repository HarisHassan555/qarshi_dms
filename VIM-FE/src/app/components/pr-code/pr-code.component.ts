import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';
import {
  getApprovalLogApprover,
  getApprovalLogDate,
  getApprovalLogLevel,
  getApprovalLogRemarks,
  getApprovalLogRole,
  getApprovalLogRows,
  getApprovalLogSignatureUrl,
  getApprovalLogStatus,
  isCapfLike,
  normalizeFormFields,
  parseApplicationFormData,
  parseApprovalHistory,
} from 'src/app/utils/application-email-view.util';

@Component({
  selector: 'app-pr-code',
  templateUrl: './pr-code.component.html',
  styleUrls: ['./pr-code.component.css'],
})
export class PrCodeComponent implements OnInit {
  applicationId: number | null = null;
  application: any = null;
  prCode = '';
  isLoading = true;
  isSaving = false;
  currentUser: any = null;

  formFields: any[] = [];
  applicationFormData: Record<string, any> = {};
  approvalHistory: any[] = [];
  showCapfPreview = false;
  userNameMap = new Map<number, string>();

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private appService: CustomFormApplicationService,
    private customFormService: CustomFormService,
    private notification: NotificationService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const paramId = this.route.snapshot.paramMap.get('id');
    const queryId = this.route.snapshot.queryParamMap.get('applicationId');
    const id = paramId || queryId;
    this.applicationId = id ? Number(id) : null;
    if (!this.applicationId || Number.isNaN(this.applicationId)) {
      this.notification.showMessage('Invalid application id', 'danger');
      this.router.navigateByUrl('/Dashboard');
      return;
    }
    this.userService.me().subscribe((user: any) => (this.currentUser = user));
    this.userService.getUsers().subscribe({
      next: (data: any) => {
        if (!Array.isArray(data)) return;
        const map = new Map<number, string>();
        data.forEach((u: any) => {
          const uid = u?.serUserId ?? u?.userId ?? u?.id;
          const name = u?.txtUserName ?? u?.userName ?? u?.name;
          if (uid != null && name) {
            map.set(Number(uid), String(name));
          }
        });
        this.userNameMap = map;
      },
      error: () => {},
    });
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.appService.getApplicationById(this.applicationId!).subscribe({
      next: (app: any) => {
        const apply = (fullApp: any, fieldsSrc: any[]) => {
          this.application = fullApp;
          this.formFields = normalizeFormFields(fieldsSrc);
          this.applicationFormData = parseApplicationFormData(fullApp.txtApplicationData);
          this.approvalHistory = parseApprovalHistory(fullApp.txtApprovalHistory);
          this.showCapfPreview = isCapfLike(fullApp, this.approvalHistory);
          this.prCode = (fullApp as any)?.txtPrCode || '';
          this.isLoading = false;
        };

        const formFieldsFromApp =
          app?.cfgTblCustomForm?.cfgTblCustomFormFields;
        if (Array.isArray(formFieldsFromApp) && formFieldsFromApp.length > 0) {
          apply(app, formFieldsFromApp);
          return;
        }
        if (app?.serFormId) {
          this.customFormService.getById(app.serFormId).subscribe({
            next: (formData: any) => {
              const merged = {
                ...app,
                cfgTblCustomForm: formData || app.cfgTblCustomForm,
              };
              apply(merged, formData?.cfgTblCustomFormFields || []);
            },
            error: () => {
              apply(app, []);
            },
          });
          return;
        }
        apply(app, []);
      },
      error: () => {
        this.isLoading = false;
        this.notification.showMessage('Failed to load application', 'danger');
      },
    });
  }

  save(): void {
    if (!this.prCode.trim()) {
      this.notification.showMessage('PR code is required', 'warning');
      return;
    }
    this.isSaving = true;
    this.appService.assignPrCode(this.applicationId!, this.prCode.trim(), this.currentUser?.serUserId).subscribe({
      next: () => {
        this.isSaving = false;
        this.notification.showMessage('PR code saved successfully', 'success');
        this.load();
      },
      error: () => {
        this.isSaving = false;
        this.notification.showMessage('Failed to save PR code', 'danger');
      },
    });
  }

  approvalLogRows(): any[] {
    return getApprovalLogRows(this.approvalHistory);
  }

  approvalLogLevel(entry: any): string {
    return getApprovalLogLevel(entry);
  }

  approvalLogApprover(entry: any): string {
    return getApprovalLogApprover(entry, this.userNameMap);
  }

  approvalLogRole(entry: any): string {
    return getApprovalLogRole(entry);
  }

  approvalLogStatus(entry: any): string {
    return getApprovalLogStatus(entry);
  }

  approvalLogDate(entry: any): string {
    return getApprovalLogDate(entry);
  }

  approvalLogRemarks(entry: any): string {
    return getApprovalLogRemarks(entry);
  }

  approvalLogSignatureUrl(entry: any): string {
    return getApprovalLogSignatureUrl(entry);
  }

  /** Submitted-by: prefer API user object / text fields, then user list lookup (avoids raw numeric id). */
  displaySubmittedBy(): string {
    const app = this.application;
    if (!app) return '-';
    const byName =
      app.cfgTblUser?.txtUserName ||
      app.txtSubmittedBy ||
      app.submittedByName ||
      app.txtUserName;
    if (byName != null && String(byName).trim() !== '') {
      return String(byName).trim();
    }
    const id = app.serSubmittedBy ?? app.cfgTblUser?.serUserId;
    if (id != null) {
      const n = Number(id);
      const fromMap = this.userNameMap.get(n);
      if (fromMap) return fromMap;
      return `User ${n}`;
    }
    return '-';
  }
}
