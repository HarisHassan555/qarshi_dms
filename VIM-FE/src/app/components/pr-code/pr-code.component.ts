import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { NotificationService } from 'src/app/NotificationService';
import { UserService } from 'src/app/services/user/user.service';

@Component({
  selector: 'app-pr-code',
  templateUrl: './pr-code.component.html',
  styleUrls: ['./pr-code.component.css']
})
export class PrCodeComponent implements OnInit {
  applicationId: number | null = null;
  application: any = null;
  prCode = '';
  isLoading = true;
  isSaving = false;
  currentUser: any = null;

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private appService: CustomFormApplicationService,
    private notification: NotificationService,
    private userService: UserService
  ) { }

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
    this.userService.me().subscribe((user: any) => this.currentUser = user);
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.appService.getApplicationById(this.applicationId!).subscribe({
      next: (app: any) => {
        this.application = app;
        this.prCode = (app as any)?.txtPrCode || '';
        this.isLoading = false;
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
}
