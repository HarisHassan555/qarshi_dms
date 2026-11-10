import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import {
    OfficialDocumentArchiveItem,
    OfficialDocumentArchiveService,
} from 'src/app/services/official-document-archive/official-document-archive.service';

@Component({
    selector: 'app-official-document-archive',
    templateUrl: './official-document-archive.component.html',
    styleUrls: ['./official-document-archive.component.css'],
})
export class OfficialDocumentArchiveComponent implements OnInit, OnDestroy {
    @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
    @ViewChild('scanInput') scanInput?: ElementRef<HTMLInputElement>;

    documents: OfficialDocumentArchiveItem[] = [];
    labels: string[] = [];
    geminiConfigured = false;
    search = '';
    selectedLabel = '';
    page = 0;
    pageSize = 12;
    total = 0;
    loading = false;
    uploading = false;
    loadError = '';
    selectedPreviewId: number | null = null;
    private searchTimer: ReturnType<typeof setTimeout> | null = null;

    readonly labelDisplay: Record<string, string> = {
        FINANCE: 'Finance',
        PROCUREMENT: 'Procurement',
        LEGAL: 'Legal',
        HR: 'HR',
        OPERATIONS: 'Operations',
        COMPLIANCE: 'Compliance',
        GENERAL: 'General',
    };

    constructor(
        private archiveService: OfficialDocumentArchiveService,
        private notificationService: NotificationService
    ) {}

    ngOnInit(): void {
        this.load();
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
    }

    load(): void {
        this.loading = true;
        this.loadError = '';
        this.archiveService
            .list(this.search, this.selectedLabel, this.page, this.pageSize)
            .pipe(finalize(() => (this.loading = false)))
            .subscribe({
                next: (res) => {
                    this.documents = res.items || [];
                    this.total = res.total || 0;
                    this.labels = res.labels || [];
                    this.geminiConfigured = !!res.geminiConfigured;
                },
                error: () => {
                    this.loadError = 'Could not load archived documents.';
                },
            });
    }

    onSearchChange(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.page = 0;
        this.searchTimer = setTimeout(() => this.load(), 350);
    }

    onLabelChange(): void {
        this.page = 0;
        this.load();
    }

    triggerUpload(): void {
        this.fileInput?.nativeElement.click();
    }

    triggerScan(): void {
        this.scanInput?.nativeElement.click();
    }

    onFileChosen(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        if (!file) {
            return;
        }
        this.uploadFile(file);
        input.value = '';
    }

    private uploadFile(file: File): void {
        const allowed =
            file.type === 'application/pdf' ||
            file.type.startsWith('image/');
        if (!allowed) {
            this.notificationService.showMessage('Upload a PDF or image file', 'danger');
            return;
        }
        if (file.size > 50 * 1024 * 1024) {
            this.notificationService.showMessage('Maximum file size is 50 MB', 'danger');
            return;
        }

        this.uploading = true;
        this.archiveService
            .upload(file, true)
            .pipe(finalize(() => (this.uploading = false)))
            .subscribe({
                next: (res) => {
                    const msg = res?.message || 'Document archived.';
                    this.notificationService.showMessage(msg, res?.document ? 'success' : 'warning');
                    this.page = 0;
                    this.load();
                },
                error: (err) => {
                    const msg = err?.error?.message || 'Upload failed';
                    this.notificationService.showMessage(msg, 'danger');
                },
            });
    }

    openPreview(doc: OfficialDocumentArchiveItem): void {
        this.selectedPreviewId = doc.serOfficialDocumentId;
        window.open(this.archiveService.downloadUrl(doc.serOfficialDocumentId), '_blank');
    }

    deleteDoc(doc: OfficialDocumentArchiveItem): void {
        if (!confirm('Remove this document from the archive?')) {
            return;
        }
        this.archiveService.delete(doc.serOfficialDocumentId).subscribe({
            next: () => {
                this.notificationService.showMessage('Document removed', 'success');
                this.load();
            },
            error: () => this.notificationService.showMessage('Delete failed', 'danger'),
        });
    }

    reanalyze(doc: OfficialDocumentArchiveItem): void {
        this.archiveService.reanalyze(doc.serOfficialDocumentId).subscribe({
            next: (res) => {
                this.notificationService.showMessage(res?.message || 'Re-analyzed', 'success');
                this.load();
            },
            error: (err) => {
                const msg = err?.error?.message || 'Re-analysis failed';
                this.notificationService.showMessage(msg, 'danger');
            },
        });
    }

    prevPage(): void {
        if (this.page > 0) {
            this.page--;
            this.load();
        }
    }

    nextPage(): void {
        if ((this.page + 1) * this.pageSize < this.total) {
            this.page++;
            this.load();
        }
    }

    formatLabel(code: string): string {
        return this.labelDisplay[code] || code || 'General';
    }

    formatDate(value: string | number | undefined): string {
        if (value == null) {
            return '—';
        }
        const d = typeof value === 'number' ? new Date(value) : new Date(value);
        if (Number.isNaN(d.getTime())) {
            return '—';
        }
        return d.toLocaleString();
    }
}
