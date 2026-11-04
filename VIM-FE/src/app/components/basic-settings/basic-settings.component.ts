import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { NotificationService } from 'src/app/NotificationService';
import { ApplicationSetting, ApplicationSettingService } from 'src/app/services/application-setting/application-setting.service';

@Component({
    selector: 'app-basic-settings',
    templateUrl: './basic-settings.component.html',
    styleUrls: ['./basic-settings.component.css'],
})
export class BasicSettingsComponent implements OnInit, OnDestroy {
    form!: FormGroup;
    activeTab: 'basic' | 'smtp' = 'basic';
    loading = false;
    saving = false;
    deleting = false;
    submitted = false;
    loadError = '';
    settingId: number | null = null;
    logoFile: File | null = null;
    heroFile: File | null = null;
    clearLogo = false;
    clearHero = false;
    logoPreview: SafeUrl | null = null;
    heroPreview: SafeUrl | null = null;
    private logoObjectUrl = '';
    private heroObjectUrl = '';

    constructor(
        private fb: FormBuilder,
        private applicationSettingService: ApplicationSettingService,
        private notificationService: NotificationService,
        private sanitizer: DomSanitizer
    ) {}

    ngOnInit(): void {
        this.form = this.fb.group({
            txtApplicationName: ['', Validators.required],
            txtAddress: [''],
            txtEmail: ['', Validators.email],
            blIsActive: [true],
            txtSmtpHost: [''],
            txtSmtpPort: [''],
            txtSmtpUsername: [''],
            txtSmtpPassword: [''],
            blSmtpAuth: [true],
            blSmtpSslEnable: [true],
            blSmtpStarttlsEnable: [false],
        });
        this.load();
    }

    ngOnDestroy(): void {
        this.revokePreview('logo');
        this.revokePreview('hero');
    }

    setTab(tab: 'basic' | 'smtp'): void {
        this.activeTab = tab;
    }

    load(): void {
        this.loading = true;
        this.loadError = '';
        this.applicationSettingService.getCurrent().subscribe({
            next: (row) => {
                this.loading = false;
                this.applyRow(row);
            },
            error: () => {
                this.loading = false;
                this.loadError = 'Could not load application settings.';
            },
        });
    }

    onImageSelected(kind: 'logo' | 'hero', event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files && input.files[0];
        input.value = '';
        if (!file) {
            return;
        }
        if (!file.type.startsWith('image/')) {
            this.notificationService.showMessage('Choose a PNG, JPG, WEBP, or GIF file.', 'danger');
            return;
        }
        if (file.size > 8 * 1024 * 1024) {
            this.notificationService.showMessage('Image must be 8 MB or smaller.', 'danger');
            return;
        }
        if (kind === 'logo') {
            this.logoFile = file;
            this.clearLogo = false;
        } else {
            this.heroFile = file;
            this.clearHero = false;
        }
        this.showPreview(kind, URL.createObjectURL(file));
    }

    removeImage(kind: 'logo' | 'hero'): void {
        this.revokePreview(kind);
        if (kind === 'logo') {
            this.logoFile = null;
            this.clearLogo = true;
            this.logoPreview = null;
            return;
        }
        this.heroFile = null;
        this.clearHero = true;
        this.heroPreview = null;
    }

    save(): void {
        this.submitted = true;
        if (this.form.invalid) {
            this.activeTab = this.form.controls['txtApplicationName'].invalid || this.form.controls['txtEmail'].invalid
                ? 'basic'
                : this.activeTab;
            return;
        }
        this.saving = true;
        const updating = this.settingId != null;
        this.applicationSettingService.save(this.buildPayload(updating), updating).subscribe({
            next: (saved) => {
                this.saving = false;
                this.submitted = false;
                this.notificationService.showMessage(updating ? 'Settings updated.' : 'Settings saved.', 'success');
                this.applicationSettingService.notifyBrandingChanged();
                this.applyRow(saved);
            },
            error: (error) => {
                this.saving = false;
                const message = error?.error?.message || 'Could not save application settings.';
                this.notificationService.showMessage(message, 'danger');
            },
        });
    }

    deleteSetting(): void {
        if (this.settingId == null) {
            return;
        }
        this.deleting = true;
        this.applicationSettingService.delete(this.settingId).subscribe({
            next: () => {
                this.deleting = false;
                this.notificationService.showMessage('Settings deleted.', 'success');
                this.applicationSettingService.notifyBrandingChanged();
                this.applyRow(null);
            },
            error: (error) => {
                this.deleting = false;
                const message = error?.error?.message || 'Could not delete application settings.';
                this.notificationService.showMessage(message, 'danger');
            },
        });
    }

    private applyRow(row: ApplicationSetting | null): void {
        this.logoFile = null;
        this.heroFile = null;
        this.clearLogo = false;
        this.clearHero = false;
        this.revokePreview('logo');
        this.revokePreview('hero');
        this.logoPreview = null;
        this.heroPreview = null;
        if (!row || !row.serApplicationSettingId) {
            this.settingId = null;
            this.form.reset({
                txtApplicationName: '',
                txtAddress: '',
                txtEmail: '',
                blIsActive: true,
                txtSmtpHost: '',
                txtSmtpPort: '',
                txtSmtpUsername: '',
                txtSmtpPassword: '',
                blSmtpAuth: true,
                blSmtpSslEnable: true,
                blSmtpStarttlsEnable: false,
            });
            return;
        }
        this.settingId = row.serApplicationSettingId;
        this.form.patchValue({
            txtApplicationName: row.txtApplicationName || '',
            txtAddress: row.txtAddress || '',
            txtEmail: row.txtEmail || '',
            blIsActive: row.blIsActive !== false,
            txtSmtpHost: row.txtSmtpHost || '',
            txtSmtpPort: row.txtSmtpPort ?? '',
            txtSmtpUsername: row.txtSmtpUsername || '',
            txtSmtpPassword: row.txtSmtpPassword || '',
            blSmtpAuth: !!row.blSmtpAuth,
            blSmtpSslEnable: !!row.blSmtpSslEnable,
            blSmtpStarttlsEnable: !!row.blSmtpStarttlsEnable,
        });
        if (row.txtLogoPath) {
            this.loadStoredImage('logo', row.serApplicationSettingId);
        }
        if (row.txtDashboardHeroImagePath) {
            this.loadStoredImage('hero', row.serApplicationSettingId);
        }
    }

    private loadStoredImage(kind: 'logo' | 'hero', id: number): void {
        this.applicationSettingService.getImage(id, kind).subscribe({
            next: (blob) => {
                if (!blob || blob.size === 0) {
                    return;
                }
                if (blob.type && !blob.type.startsWith('image/') && blob.type !== 'application/octet-stream') {
                    return;
                }
                const typedBlob = blob.type ? blob : new Blob([blob], { type: 'image/jpeg' });
                this.showPreview(kind, URL.createObjectURL(typedBlob));
            },
            error: () => {
                if (kind === 'logo') {
                    this.logoPreview = null;
                } else {
                    this.heroPreview = null;
                }
            },
        });
    }

    private buildPayload(updating: boolean): FormData {
        const value = this.form.getRawValue();
        const data = new FormData();
        if (updating && this.settingId != null) {
            data.append('serApplicationSettingId', String(this.settingId));
            data.append('clearLogo', String(this.clearLogo));
            data.append('clearHero', String(this.clearHero));
        }
        data.append('txtApplicationName', value.txtApplicationName || '');
        data.append('txtAddress', value.txtAddress || '');
        data.append('txtEmail', value.txtEmail || '');
        data.append('blIsActive', String(!!value.blIsActive));
        data.append('txtSmtpHost', value.txtSmtpHost || '');
        data.append('txtSmtpPort', value.txtSmtpPort === null || value.txtSmtpPort === undefined ? '' : String(value.txtSmtpPort));
        data.append('txtSmtpUsername', value.txtSmtpUsername || '');
        data.append('txtSmtpPassword', value.txtSmtpPassword || '');
        data.append('blSmtpAuth', String(!!value.blSmtpAuth));
        data.append('blSmtpSslEnable', String(!!value.blSmtpSslEnable));
        data.append('blSmtpStarttlsEnable', String(!!value.blSmtpStarttlsEnable));
        if (this.logoFile) {
            data.append('logo', this.logoFile, this.logoFile.name);
        }
        if (this.heroFile) {
            data.append('hero', this.heroFile, this.heroFile.name);
        }
        return data;
    }

    private showPreview(kind: 'logo' | 'hero', objectUrl: string): void {
        this.revokePreview(kind);
        const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        if (kind === 'logo') {
            this.logoObjectUrl = objectUrl;
            this.logoPreview = safeUrl;
            return;
        }
        this.heroObjectUrl = objectUrl;
        this.heroPreview = safeUrl;
    }

    private revokePreview(kind: 'logo' | 'hero'): void {
        const current = kind === 'logo' ? this.logoObjectUrl : this.heroObjectUrl;
        if (current) {
            URL.revokeObjectURL(current);
        }
        if (kind === 'logo') {
            this.logoObjectUrl = '';
        } else {
            this.heroObjectUrl = '';
        }
    }
}
