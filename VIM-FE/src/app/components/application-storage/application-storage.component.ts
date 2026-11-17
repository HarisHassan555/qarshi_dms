import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';

interface StorageBreakdownItem {
    label: string;
    bytes: number;
    path?: string;
}

@Component({
    selector: 'app-application-storage',
    templateUrl: './application-storage.component.html',
    styleUrls: ['./application-storage.component.css'],
})
export class ApplicationStorageComponent implements OnInit, OnDestroy {
    loading = false;
    contentLoading = false;
    info: any = null;
    private contentRequest: Subscription | null = null;

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService
    ) {}

    ngOnInit(): void {
        this.loadStorage();
    }

    ngOnDestroy(): void {
        this.contentRequest?.unsubscribe();
    }

    loadStorage(): void {
        this.contentRequest?.unsubscribe();
        this.loading = true;
        this.contentLoading = false;
        this.templateWorkflowService.getApplicationStorageInfo(false).subscribe({
            next: (response) => {
                this.loading = false;
                if (String(response?.status || '').toLowerCase() === 'failure') {
                    this.info = null;
                    this.notificationService.showMessage(response?.message || 'Could not load storage.', 'danger');
                    return;
                }
                this.info = response;
                this.loadExactContent();
            },
            error: (error) => {
                this.loading = false;
                this.info = null;
                this.notificationService.showMessage(
                    error.error?.message || 'Could not load application storage.',
                    'danger'
                );
            },
        });
    }

    private loadExactContent(): void {
        this.contentLoading = true;
        this.contentRequest = this.templateWorkflowService.getApplicationStorageInfo(true).subscribe({
            next: (response) => {
                this.contentLoading = false;
                if (response && String(response.status || '').toLowerCase() !== 'failure') {
                    this.info = response;
                }
            },
            error: () => {
                this.contentLoading = false;
            },
        });
    }

    get uploadBreakdown(): StorageBreakdownItem[] {
        const items = this.info?.fileStorage?.breakdown;
        return Array.isArray(items) ? items : [];
    }

    formatBytes(value: unknown): string {
        const bytes = Number(value || 0);
        if (!Number.isFinite(bytes) || bytes <= 0) {
            return '0 B';
        }
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        const power = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
        const amount = bytes / Math.pow(1024, power);
        return `${amount >= 10 || power === 0 ? amount.toFixed(0) : amount.toFixed(1)} ${units[power]}`;
    }

    usedPercent(): number {
        const percent = Number(this.info?.hostVolume?.usedPercent || 0);
        if (!Number.isFinite(percent)) {
            return 0;
        }
        return Math.max(0, Math.min(100, percent));
    }

}
