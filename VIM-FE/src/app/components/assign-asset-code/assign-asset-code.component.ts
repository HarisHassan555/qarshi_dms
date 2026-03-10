import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { NotificationService } from 'src/app/NotificationService';

@Component({
  selector: 'app-assign-asset-code',
  templateUrl: './assign-asset-code.component.html',
  styleUrls: ['./assign-asset-code.component.css']
})
export class AssignAssetCodeComponent implements OnInit {
  applicationId: number | null = null;
  application: any = null;
  isLoading = true;
  isSaving = false;
  assetCode = '';

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private appService: CustomFormApplicationService,
    private notification: NotificationService
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
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.appService.getApplicationById(this.applicationId!).subscribe({
      next: (app) => {
        this.application = app;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.notification.showMessage('Failed to load application', 'danger');
      },
    });
  }

  save(): void {
    if (!this.assetCode.trim()) {
      this.notification.showMessage('Asset code is required', 'warning');
      return;
    }
    this.isSaving = true;
    this.appService
      .assignAssetCode(this.applicationId!, this.assetCode.trim())
      .subscribe({
        next: () => {
          this.isSaving = false;
          this.notification.showMessage('Asset code saved. Application approved.', 'success');
          this.router.navigateByUrl('/pending-approvals');
        },
        error: () => {
          this.isSaving = false;
          this.notification.showMessage('Failed to save asset code', 'danger');
        },
      });
  }
}
