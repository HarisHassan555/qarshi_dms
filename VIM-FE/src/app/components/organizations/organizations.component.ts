import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ModalComponent } from 'angular-custom-modal';
import { NotificationService } from 'src/app/NotificationService';
import { OrgStructureService } from 'src/app/services/org-structure/org-structure.service';
import { DocumentHeaderBrandingService } from 'src/app/services/document-header-branding/document-header-branding.service';
import { organizationLogoDisplayUrl } from 'src/app/utils/organization-logo.util';

@Component({
    selector: 'app-organizations',
    templateUrl: './organizations.component.html',
    styleUrls: ['./organizations.component.css'],
})
export class OrganizationsComponent implements OnInit {
    @ViewChild('brandingModal') brandingModal!: ModalComponent;

    search = '';
    loading = false;
    loadError = '';
    saving = false;
    brandingSubmit = false;
    rows: any[] = [];
    brandingForm!: FormGroup;
    logoPreview = '';
    logoBase64: string | null = null;
    editOrgCode = '';

    cols = [
        { field: 'logo', title: 'Logo', width: '80px', sort: false },
        { field: 'txtOrganizationName', title: 'Organization' },
        { field: 'txtOrganizationCode', title: 'Code' },
        { field: 'txtAddress', title: 'Address' },
        { field: 'blnStatus', title: 'Status', width: '100px' },
        { field: 'actions', title: 'Actions', width: '110px', sort: false, headerClass: 'justify-center' },
    ];

    constructor(
        private fb: FormBuilder,
        private orgStructureService: OrgStructureService,
        private documentHeaderBranding: DocumentHeaderBrandingService,
        private notificationService: NotificationService,
    ) {}

    ngOnInit(): void {
        this.brandingForm = this.fb.group({
            serOrganizationId: [null],
            txtOrganizationName: ['', Validators.required],
            txtAddress: [''],
            logo: [''],
        });
        this.load();
    }

    load(): void {
        this.loading = true;
        this.loadError = '';
        this.orgStructureService.getOrganizations().subscribe({
            next: (data) => {
                this.rows = (data || []).map((row) => ({ ...row, actions: true }));
                this.loading = false;
                this.documentHeaderBranding.refresh();
            },
            error: () => {
                this.loadError = 'Could not load organizations.';
                this.rows = [];
                this.loading = false;
            },
        });
    }

    organizationLogoUrl(row: { logo?: string; serOrganizationId?: number }): string {
        return organizationLogoDisplayUrl(row?.logo, row?.serOrganizationId);
    }

    openBrandingEdit(row: any): void {
        this.brandingSubmit = false;
        this.logoBase64 = null;
        this.editOrgCode = row?.txtOrganizationCode || '';
        this.brandingForm.reset({
            serOrganizationId: row?.serOrganizationId,
            txtOrganizationName: row?.txtOrganizationName || '',
            txtAddress: row?.txtAddress || '',
            logo: row?.logo || '',
        });
        this.refreshLogoPreview();
        this.brandingModal.open();
    }

    onLogoFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        if (!file) {
            return;
        }
        if (!file.type.startsWith('image/')) {
            this.notificationService.showMessage('Please choose an image file.', 'danger');
            input.value = '';
            return;
        }
        const reader = new FileReader();
        reader.onload = () => {
            const result = String(reader.result || '');
            this.logoBase64 = result;
            this.logoPreview = result;
        };
        reader.readAsDataURL(file);
        input.value = '';
    }

    clearUploadedLogo(): void {
        this.logoBase64 = null;
        this.refreshLogoPreview();
    }

    refreshLogoPreview(): void {
        if (this.logoBase64) {
            this.logoPreview = this.logoBase64;
            return;
        }
        const id = this.brandingForm.get('serOrganizationId')?.value;
        const logo = this.brandingForm.get('logo')?.value;
        this.logoPreview = organizationLogoDisplayUrl(logo, id);
    }

    saveBranding(): void {
        this.brandingSubmit = true;
        if (this.brandingForm.invalid) {
            return;
        }
        const serOrganizationId = Number(this.brandingForm.get('serOrganizationId')?.value);
        if (!Number.isFinite(serOrganizationId) || serOrganizationId <= 0) {
            this.notificationService.showMessage('Invalid organization.', 'danger');
            return;
        }
        this.saving = true;
        const payload: any = {
            serOrganizationId,
            txtOrganizationName: this.brandingForm.get('txtOrganizationName')?.value,
            txtAddress: this.brandingForm.get('txtAddress')?.value,
            logo: this.brandingForm.get('logo')?.value,
        };
        if (this.logoBase64) {
            payload.logoBase64 = this.logoBase64;
        }
        this.orgStructureService.updateOrganizationBranding(payload).subscribe({
            next: (updated) => {
                this.saving = false;
                if (updated?.serOrganizationId) {
                    const idx = this.rows.findIndex((r) => r.serOrganizationId === updated.serOrganizationId);
                    if (idx >= 0) {
                        this.rows[idx] = { ...this.rows[idx], ...updated };
                        this.rows = [...this.rows];
                    }
                }
                this.documentHeaderBranding.refresh();
                this.brandingModal.close();
                this.notificationService.showMessage('Letterhead updated.', 'success');
            },
            error: () => {
                this.saving = false;
                this.notificationService.showMessage('Could not update letterhead.', 'danger');
            },
        });
    }
}
