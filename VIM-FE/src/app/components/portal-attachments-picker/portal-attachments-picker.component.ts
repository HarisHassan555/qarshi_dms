import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';

export interface PortalAttachmentItem {
    applicationId: number;
    formCode?: string;
    status?: string;
    createdDate?: string;
    templateName?: string;
    fieldId: string;
    fieldLabel?: string;
    attachmentIndex: number;
    fileName: string;
    mimeType?: string;
}

export interface PortalAttachmentPayload {
    fileName: string;
    mimeType: string;
    dataUrl: string;
    base64: string;
}

@Component({
    selector: 'app-portal-attachments-picker',
    templateUrl: './portal-attachments-picker.component.html',
    styleUrls: ['./portal-attachments-picker.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class PortalAttachmentsPickerComponent implements OnInit, AfterViewInit, OnDestroy {
    @Output() closed = new EventEmitter<void>();
    @Output() filesSelected = new EventEmitter<PortalAttachmentPayload[]>();
    @ViewChild('portalSearchInput') portalSearchInput?: ElementRef<HTMLInputElement>;

    loading = false;
    importing = false;
    viewMode: 'details' | 'list' = 'details';
    searchText = '';
    allItems: PortalAttachmentItem[] = [];
    filteredItems: PortalAttachmentItem[] = [];
    selectedKeys = new Set<string>();

    showPreview = false;
    previewLoading = false;
    previewError = '';
    previewName = '';
    previewIsPdf = false;
    previewUrl: SafeResourceUrl | null = null;

    private previewObjectUrl = '';
    private searchTimer: ReturnType<typeof setTimeout> | null = null;
    private searchListener: ((event: Event) => void) | null = null;
    private searchBound = false;

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService,
        private sanitizer: DomSanitizer,
        private cdr: ChangeDetectorRef,
        private ngZone: NgZone
    ) { }

    get selectedCount(): number {
        return this.selectedKeys.size;
    }

    async ngOnInit(): Promise<void> {
        await this.loadItems();
    }

    ngAfterViewInit(): void {
        this.bindSearchOutsideAngular();
    }

    ngOnDestroy(): void {
        this.unbindSearchOutsideAngular();
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
            this.searchTimer = null;
        }
        this.revokePreviewObjectUrl();
    }

    close(): void {
        this.closed.emit();
    }

    setViewMode(mode: 'details' | 'list'): void {
        this.viewMode = mode === 'list' ? 'list' : 'details';
        this.cdr.markForCheck();
    }

    isSelected(item: PortalAttachmentItem): boolean {
        return this.selectedKeys.has(this.getKey(item));
    }

    toggleSelection(item: PortalAttachmentItem, event: Event): void {
        const checked = !!(event.target as HTMLInputElement)?.checked;
        const key = this.getKey(item);
        const next = new Set(this.selectedKeys);
        if (checked) {
            next.add(key);
        } else {
            next.delete(key);
        }
        this.selectedKeys = next;
        this.cdr.markForCheck();
    }

    isImageFile(fileName: string): boolean {
        const ext = this.getExtension(fileName).toLowerCase();
        return ext === 'png' || ext === 'jpg' || ext === 'jpeg' || ext === 'webp';
    }

    async previewItem(item: PortalAttachmentItem, event?: Event): Promise<void> {
        event?.preventDefault();
        event?.stopPropagation();
        this.showPreview = true;
        this.previewLoading = true;
        this.previewError = '';
        this.previewName = item.fileName || 'File preview';
        this.previewUrl = null;
        this.revokePreviewObjectUrl();
        this.cdr.markForCheck();
        try {
            const payload = await this.fetchPayload(item);
            const mimeType = String(payload.mimeType || 'application/octet-stream').toLowerCase();
            const objectUrl = this.createObjectUrl(payload.base64, mimeType);
            const dataUrl = String(payload.dataUrl || '').trim();
            const resolved = objectUrl || dataUrl;
            if (!resolved) {
                throw new Error('Preview content is unavailable');
            }
            this.previewObjectUrl = objectUrl;
            this.previewIsPdf = mimeType.includes('pdf') || /\.pdf$/i.test(payload.fileName || '');
            this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(resolved);
        } catch (error: any) {
            this.previewError = error?.error?.message || error?.message || 'Failed to preview file.';
        } finally {
            this.previewLoading = false;
            this.cdr.markForCheck();
        }
    }

    closePreview(): void {
        this.showPreview = false;
        this.previewLoading = false;
        this.previewError = '';
        this.previewName = '';
        this.previewIsPdf = false;
        this.previewUrl = null;
        this.revokePreviewObjectUrl();
        this.cdr.markForCheck();
    }

    async openSelected(): Promise<void> {
        if (this.importing || this.selectedKeys.size === 0) {
            return;
        }
        const selectedItems = this.filteredItems.filter((item) => this.isSelected(item));
        if (!selectedItems.length) {
            return;
        }
        this.importing = true;
        this.cdr.markForCheck();
        try {
            const payloads: PortalAttachmentPayload[] = [];
            for (const item of selectedItems) {
                payloads.push(await this.fetchPayload(item));
            }
            this.filesSelected.emit(payloads);
        } catch (error: any) {
            this.notificationService.showMessage(
                error?.error?.message || error?.message || 'Failed to add portal attachment.',
                'danger'
            );
        } finally {
            this.importing = false;
            this.cdr.markForCheck();
        }
    }

    private async loadItems(): Promise<void> {
        this.loading = true;
        this.cdr.markForCheck();
        try {
            const response = await firstValueFrom(
                this.templateWorkflowService.getCompletedTemplatePortalAttachments(0, 200, '')
            );
            this.allItems = Array.isArray(response?.items) ? response.items : [];
            this.applySearchFilter(this.searchText);
            this.selectedKeys = new Set<string>();
        } catch (error: any) {
            this.allItems = [];
            this.filteredItems = [];
            this.notificationService.showMessage(
                error?.error?.message || error?.message || 'Failed to load portal attachments.',
                'danger'
            );
        } finally {
            this.loading = false;
            this.cdr.markForCheck();
            this.bindSearchOutsideAngular();
        }
    }

    private bindSearchOutsideAngular(): void {
        if (this.searchBound) {
            return;
        }
        setTimeout(() => {
            const input = this.portalSearchInput?.nativeElement;
            if (!input || this.searchBound) {
                return;
            }
            this.ngZone.runOutsideAngular(() => {
                this.searchListener = () => {
                    const value = input.value || '';
                    if (this.searchTimer) {
                        clearTimeout(this.searchTimer);
                    }
                    this.searchTimer = setTimeout(() => {
                        this.ngZone.run(() => {
                            this.searchText = value;
                            this.applySearchFilter(value);
                            this.cdr.markForCheck();
                        });
                    }, 200);
                };
                input.addEventListener('input', this.searchListener);
                this.searchBound = true;
            });
        });
    }

    private unbindSearchOutsideAngular(): void {
        const input = this.portalSearchInput?.nativeElement;
        if (input && this.searchListener) {
            input.removeEventListener('input', this.searchListener);
        }
        this.searchListener = null;
        this.searchBound = false;
    }

    private applySearchFilter(rawSearch: string): void {
        const search = String(rawSearch || '').trim().toLowerCase();
        if (!search) {
            this.filteredItems = [...this.allItems];
            return;
        }
        this.filteredItems = this.allItems.filter((item) => {
            const haystack = [
                item.fileName,
                item.templateName,
                item.formCode,
                item.fieldLabel,
                item.status,
                String(item.applicationId || '')
            ]
                .map((value) => String(value || '').toLowerCase())
                .join(' ');
            return haystack.includes(search);
        });
    }

    private getKey(item: PortalAttachmentItem): string {
        return `${item.applicationId}|${item.fieldId}|${item.attachmentIndex}`;
    }

    private getExtension(fileName: string): string {
        const name = String(fileName || '').trim();
        const dot = name.lastIndexOf('.');
        const ext = dot >= 0 ? name.substring(dot + 1).toUpperCase() : 'FILE';
        return ext || 'FILE';
    }

    private async fetchPayload(item: PortalAttachmentItem): Promise<PortalAttachmentPayload> {
        const response = await firstValueFrom(
            this.templateWorkflowService.getCompletedTemplatePortalAttachmentContent(
                item.applicationId,
                item.fieldId,
                item.attachmentIndex
            )
        );
        if (!response || response.status !== 'Success' || !response.attachment) {
            throw new Error(response?.message || 'Could not load portal attachment content');
        }
        const attachment = response.attachment;
        const fileName = String(attachment.fileName || attachment.name || '').trim();
        const mimeType = String(attachment.mimeType || attachment.type || 'application/octet-stream').trim();
        const dataUrl = String(attachment.dataUrl || '').trim();
        const base64 = String(
            attachment.base64 || (dataUrl.includes(',') ? dataUrl.split(',', 2)[1] : '')
        ).trim();
        if (!fileName || !base64) {
            throw new Error('Portal attachment content is invalid');
        }
        return {
            fileName,
            mimeType,
            base64,
            dataUrl: dataUrl || `data:${mimeType};base64,${base64}`
        };
    }

    private createObjectUrl(base64: string, mimeType: string): string {
        try {
            const binary = atob(base64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            return URL.createObjectURL(new Blob([bytes], { type: mimeType || 'application/octet-stream' }));
        } catch {
            return '';
        }
    }

    private revokePreviewObjectUrl(): void {
        if (this.previewObjectUrl) {
            URL.revokeObjectURL(this.previewObjectUrl);
            this.previewObjectUrl = '';
        }
    }
}
