import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { DocumentShareService } from 'src/app/services/document-share/document-share.service';

@Component({
    moduleId: module.id,
    selector: 'app-document-share-view',
    templateUrl: './document-share-view.component.html',
    styleUrls: ['./document-share-view.component.css'],
})
export class DocumentShareViewComponent implements OnInit, OnDestroy {
    token = '';
    isLoading = true;
    loadError = '';
    meta: any = null;
    pdfUrl = '';
    safePdfUrl: SafeResourceUrl | null = null;

    constructor(
        private route: ActivatedRoute,
        private documentShareService: DocumentShareService,
        private sanitizer: DomSanitizer
    ) {}

    ngOnInit(): void {
        this.route.paramMap.subscribe((params) => {
            this.token = String(params.get('token') || '').trim();
            void this.loadShare();
        });
    }

    ngOnDestroy(): void {
        if (this.pdfUrl) {
            URL.revokeObjectURL(this.pdfUrl);
        }
    }

    private async loadShare(): Promise<void> {
        this.isLoading = true;
        this.loadError = '';
        this.meta = null;
        if (this.pdfUrl) {
            URL.revokeObjectURL(this.pdfUrl);
            this.pdfUrl = '';
            this.safePdfUrl = null;
        }

        if (!this.token) {
            this.loadError = 'Invalid share link.';
            this.isLoading = false;
            return;
        }

        try {
            const metaResponse = await firstValueFrom(this.documentShareService.getPublicShare(this.token));
            if (metaResponse?.status !== 'Success') {
                this.loadError = metaResponse?.message || 'This share link is not available.';
                this.isLoading = false;
                return;
            }
            this.meta = metaResponse;

            if (!metaResponse.hasPdf) {
                this.loadError = 'A PDF is not available for this document yet.';
                this.isLoading = false;
                return;
            }

            const pdfResponse = await firstValueFrom(this.documentShareService.getPublicSharePdf(this.token));
            const base64 = String(pdfResponse?.base64 || '').trim();
            if (pdfResponse?.status !== 'Success' || !base64) {
                this.loadError = pdfResponse?.message || 'Could not load the document PDF.';
                this.isLoading = false;
                return;
            }

            const mimeType = String(pdfResponse?.mimeType || 'application/pdf');
            const binary = atob(base64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            const blob = new Blob([bytes], { type: mimeType });
            this.pdfUrl = URL.createObjectURL(blob);
            this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfUrl);
        } catch {
            this.loadError = 'Could not load this shared document.';
        } finally {
            this.isLoading = false;
        }
    }
}
