import { ApplicationRef, Injectable } from '@angular/core';
import { OrgStructureService } from 'src/app/services/org-structure/org-structure.service';
import {
    applyDocumentHeaderBrandingCache,
    DOCUMENT_HEADER_FIELD_TYPE_TO_ORG_CODE,
    DocumentHeaderBrandingSlice,
    normalizeDocumentHeaderType,
} from 'src/app/utils/document-header.util';

@Injectable({
    providedIn: 'root',
})
export class DocumentHeaderBrandingService {
    private loadStarted = false;
    private loaded = false;

    constructor(
        private orgStructureService: OrgStructureService,
        private appRef: ApplicationRef,
    ) {}

    /** Load organization letterhead data once per session (after login). */
    ensureLoaded(): void {
        if (this.loadStarted || !localStorage.getItem('token')) {
            return;
        }
        this.loadStarted = true;
        this.orgStructureService.getOrganizations().subscribe({
            next: (orgs) => {
                this.applyOrganizations(orgs || []);
                this.loaded = true;
            },
            error: () => {
                this.loadStarted = false;
            },
        });
    }

    /** Force refresh (e.g. after login or org master data changes). */
    refresh(): void {
        if (!localStorage.getItem('token')) {
            return;
        }
        this.loadStarted = true;
        this.orgStructureService.getOrganizations().subscribe({
            next: (orgs) => {
                this.applyOrganizations(orgs || []);
                this.loaded = true;
            },
            error: () => {
                this.loadStarted = false;
                this.loaded = false;
            },
        });
    }

    isLoaded(): boolean {
        return this.loaded;
    }

    private applyOrganizations(orgs: any[]): void {
        const byCode = new Map<string, any>();
        for (const org of orgs) {
            const code = String(org?.txtOrganizationCode || '').trim().toUpperCase();
            if (code) {
                byCode.set(code, org);
            }
        }

        const cache: Record<string, DocumentHeaderBrandingSlice> = {};
        for (const [fieldType, orgCode] of Object.entries(DOCUMENT_HEADER_FIELD_TYPE_TO_ORG_CODE)) {
            const org = byCode.get(orgCode);
            if (!org) {
                continue;
            }
            const logoPath = String(org.logo || org.txtLogoPath || '').trim();
            const brandTitle = String(org.txtOrganizationName || '').trim();
            const address = String(org.txtAddress || '').trim();
            if (!logoPath && !brandTitle && !address) {
                continue;
            }
            const orgId = Number(org.serOrganizationId);
            cache[normalizeDocumentHeaderType(fieldType)] = {
                logoPath,
                brandTitle,
                address,
                organizationId: Number.isFinite(orgId) && orgId > 0 ? orgId : undefined,
            };
        }
        applyDocumentHeaderBrandingCache(cache);
        try {
            this.appRef.tick();
        } catch {
            // ignore if app is tearing down
        }
    }
}
