import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';

interface TemplatePendingApplication {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    dteCreatedDate?: string;
    templateName?: string;
}

@Component({
    selector: 'app-template-pending-approvals',
    templateUrl: './template-pending-approvals.component.html',
    styleUrls: ['./template-pending-approvals.component.css']
})
export class TemplatePendingApprovalsComponent implements OnInit, OnDestroy {
    search = '';
    currentUser: any = null;
    isLoading = false;
    pendingApprovals: TemplatePendingApplication[] = [];
    page = 0;
    pageSize = 10;
    totalPendingApprovals = 0;
    private searchTimer: ReturnType<typeof setTimeout> | null = null;

    cols = [
        { field: 'txtFormCode', title: 'Application Code' },
        { field: 'templateName', title: 'Template' },
        { field: 'txtStatus', title: 'Status' },
        { field: 'intCurrentApprovalLevel', title: 'Approval Level' },
        { field: 'dteCreatedDate', title: 'Submitted Date' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService,
        private router: Router
    ) { }

    ngOnInit(): void {
        try {
            this.currentUser = JSON.parse(localStorage.getItem('user') || 'null');
        } catch {
            this.currentUser = null;
        }
        this.loadPendingApprovals();
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
    }

    loadPendingApprovals(): void {
        this.isLoading = true;

        this.templateWorkflowService.getTemplatePendingApprovals(
            this.currentUser?.serUserId || this.currentUser?.userId || this.currentUser?.id || 0,
            false,
            this.page,
            this.pageSize,
            this.search
        ).pipe(finalize(() => this.isLoading = false)).subscribe({
            next: (response: any) => {
                this.pendingApprovals = (Array.isArray(response?.items) ? response.items : [])
                    .map((application: any) => this.toShallowPendingApplication(application));
                this.totalPendingApprovals = Number(response?.total || 0);
            },
            error: (error) => {
                this.pendingApprovals = [];
                this.totalPendingApprovals = 0;
                this.notificationService.showMessage(
                    'Error loading template pending approvals: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
    }

    onSearchChanged(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => {
            this.page = 0;
            this.loadPendingApprovals();
        }, 300);
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.loadPendingApprovals();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 10;
        this.page = 0;
        this.loadPendingApprovals();
    }

    exportPendingApprovalsCsv(): void {
        const userId = this.currentUser?.serUserId || this.currentUser?.userId || this.currentUser?.id || 0;
        const exportSize = Math.max(this.totalPendingApprovals || 0, this.pageSize, 1000);
        this.templateWorkflowService.getTemplatePendingApprovals(userId, false, 0, exportSize, this.search).subscribe({
            next: (response: any) => {
                const rows = (Array.isArray(response?.items) ? response.items : [])
                    .map((application: any) => this.toShallowPendingApplication(application));
                if (!rows.length) {
                    this.notificationService.showMessage('No pending approvals available to export.', 'warning');
                    return;
                }
                const headers = ['Application Code', 'Template', 'Status', 'Approval Level', 'Submitted Date'];
                const csvRows = [
                    headers.join(','),
                    ...rows.map((application: TemplatePendingApplication) => ([
                        application.txtFormCode || '',
                        application.templateName || '',
                        application.txtStatus || '',
                        String(application.intCurrentApprovalLevel || 1),
                        application.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleString() : ''
                    ].map((value) => this.escapeCsvValue(value)).join(',')))
                ];
                this.downloadBlob(
                    new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' }),
                    `template-pending-approvals_${new Date().toISOString().split('T')[0]}.csv`
                );
            },
            error: (error) => {
                this.notificationService.showMessage(
                    'Pending approvals CSV could not be exported: ' + (error.error?.message || error.message),
                    'danger'
                );
            }
        });
    }

    get totalPages(): number {
        return Math.max(1, Math.ceil(this.totalPendingApprovals / this.pageSize));
    }

    get pageStart(): number {
        return this.totalPendingApprovals === 0 ? 0 : (this.page * this.pageSize) + 1;
    }

    get pageEnd(): number {
        return Math.min(this.totalPendingApprovals, (this.page + 1) * this.pageSize);
    }

    getDisplayedPendingApprovals(): TemplatePendingApplication[] {
        return this.pendingApprovals || [];
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
            case 'OPINION_PENDING':
                return 'badge-outline-info';
            default:
                return 'badge-outline-secondary';
        }
    }

    openApplication(application: TemplatePendingApplication): void {
        if (!application.serApplicationId) {
            this.notificationService.showMessage('Invalid application ID', 'danger');
            return;
        }
        this.router.navigate(['/template-approval', application.serApplicationId]);
    }

    private toShallowPendingApplication(application: any): TemplatePendingApplication {
        return {
            serApplicationId: application?.serApplicationId,
            serFormId: application?.serFormId,
            txtFormCode: application?.txtFormCode,
            txtStatus: application?.txtStatus,
            intCurrentApprovalLevel: application?.intCurrentApprovalLevel,
            dteCreatedDate: application?.dteCreatedDate,
            templateName: application?.templateName || 'Template'
        };
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
