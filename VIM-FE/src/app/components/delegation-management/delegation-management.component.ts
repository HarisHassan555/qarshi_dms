import { ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { ModalComponent } from 'angular-custom-modal';
import { finalize } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { UserDelegationDto, UserDelegationService } from 'src/app/services/user-delegation/user-delegation.service';
import { UserService } from 'src/app/services/user/user.service';

export type DelegationStatus = 'ACTIVE' | 'SCHEDULED' | 'EXPIRED' | 'REVOKED';

export interface DelegationUserOption {
    serUserId: number;
    txtUserName: string;
    txtCnic?: string;
    roleName?: string;
}

export interface DelegationDocumentOption {
    id: string;
    name: string;
    codeConvention?: string;
}

export interface DelegationRow {
    serDelegationId: number;
    delegateUserId: number;
    delegateUserName: string;
    delegateEmployeeId: string;
    onBehalfUserId: number;
    onBehalfUserName: string;
    onBehalfRole: string;
    validFrom: string;
    validTo: string;
    documentScopeIds: string;
    documentsLabel: string;
    status: DelegationStatus;
    createdByName: string;
    notes: string;
    hasAttachment: boolean;
    attachmentFileName: string;
    actions: boolean;
}

@Component({
    selector: 'app-delegation-management',
    templateUrl: './delegation-management.component.html',
    styleUrls: ['./delegation-management.component.css'],
})
export class DelegationManagementComponent implements OnInit, OnDestroy {
    @ViewChild('delegationModal') delegationModal!: ModalComponent;

    search = '';
    filterStatus = '';
    loading = false;
    saving = false;

    summary = { active: 0, scheduled: 0, expired: 0 };

    users: any[] = [];
    /** Stable array for ng-select — do not use a getter for [items]. */
    userSelectOptions: DelegationUserOption[] = [];
    publishedDocuments: DelegationDocumentOption[] = [];
    loadingUsers = false;
    loadingDocuments = false;
    loadError = '';

    statusOptions = [
        { value: '', label: 'All statuses' },
        { value: 'ACTIVE', label: 'Active' },
        { value: 'SCHEDULED', label: 'Scheduled' },
        { value: 'EXPIRED', label: 'Expired' },
        { value: 'REVOKED', label: 'Revoked' },
    ];

    cols = [
        { field: 'delegateUserName', title: 'Delegate' },
        { field: 'onBehalfUserName', title: 'On behalf of' },
        { field: 'documentsLabel', title: 'Documents' },
        { field: 'validFrom', title: 'Valid from' },
        { field: 'validTo', title: 'Valid to' },
        { field: 'status', title: 'Status', width: '110px' },
        { field: 'createdByName', title: 'Created by' },
        { field: 'attachment', title: 'Proof', width: '90px', sort: false },
        { field: 'actions', title: 'Actions', width: '100px', sort: false, headerClass: 'justify-center' },
    ];

    rows: DelegationRow[] = [];
    displayRows: DelegationRow[] = [];

    modalMode: 'add' | 'edit' = 'add';
    editingDelegationId: number | null = null;
    formDelegateUser: DelegationUserOption | null = null;
    formOnBehalfUser: DelegationUserOption | null = null;
    formValidFrom = '';
    formValidTo = '';
    formDocumentIds: string[] = [];
    formNotes = '';
    formAttachmentBase64: string | null = null;
    formAttachmentFileName = '';
    formExistingAttachment = false;
    formExistingAttachmentName = '';
    formRemoveAttachment = false;
    formNotifyBothParties = true;
    readonly attachmentMaxBytes = 10 * 1024 * 1024;
    readonly attachmentAccept =
        '.pdf,.jpg,.jpeg,.png,.gif,.webp,.doc,.docx,application/pdf,image/*,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document';

    private profilePhotoSafeUrls: Record<number, SafeUrl> = {};
    private profilePhotoBlobUrls: Record<number, string> = {};
    private profilePhotoLoading = new Set<number>();
    private userById = new Map<number, any>();

    constructor(
        private notificationService: NotificationService,
        private userService: UserService,
        private userDelegationService: UserDelegationService,
        private sanitizer: DomSanitizer,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnDestroy(): void {
        window.removeEventListener('velocity-tour-prepare', this.onTourPrepare);
        Object.values(this.profilePhotoBlobUrls).forEach((url) => URL.revokeObjectURL(url));
        this.profilePhotoBlobUrls = {};
        this.profilePhotoSafeUrls = {};
    }

    ngOnInit(): void {
        window.addEventListener('velocity-tour-prepare', this.onTourPrepare);
        this.loadUsers();
        this.loadDelegations();
    }

    compareUserOption = (a: DelegationUserOption | null, b: DelegationUserOption | null): boolean => {
        if (!a || !b) {
            return a === b;
        }
        return Number(a.serUserId) === Number(b.serUserId);
    };

    loadDelegations(): void {
        this.loading = true;
        this.loadError = '';
        const status = this.filterStatus || undefined;
        this.userDelegationService
            .getAll({ status })
            .pipe(finalize(() => (this.loading = false)))
            .subscribe({
                next: (data) => {
                    this.rows = (data || []).map((d) => this.mapDtoToRow(d));
                    this.displayRows = this.rows;
                    this.refreshSummary();
                    this.prefetchProfilePhotosForRows(this.rows);
                },
                error: () => {
                    this.rows = [];
                    this.displayRows = [];
                    this.loadError = 'Could not load delegations.';
                    this.refreshSummary();
                },
            });
    }

    loadUsers(): void {
        this.loadingUsers = true;
        this.userService.getUsers().subscribe({
            next: (data: any) => {
                this.users = Array.isArray(data) ? data : [];
                this.indexUsersById();
                this.refreshUserSelectOptions();
                this.prefetchProfilePhotosForRows(this.rows);
                this.loadingUsers = false;
            },
            error: () => {
                this.users = [];
                this.userSelectOptions = [];
                this.loadingUsers = false;
            },
        });
    }

    private refreshUserSelectOptions(): void {
        const preserved: DelegationUserOption[] = [];
        if (this.formDelegateUser?.serUserId) {
            preserved.push(this.formDelegateUser);
        }
        if (this.formOnBehalfUser?.serUserId) {
            preserved.push(this.formOnBehalfUser);
        }
        const mapped = (this.users || [])
            .filter((u) => this.isSelectableUser(u))
            .map((u) => this.mapUserToOption(u));
        const merged = [...mapped];
        preserved.forEach((opt) => {
            if (!merged.some((m) => m.serUserId === opt.serUserId)) {
                merged.push(opt);
            }
        });
        this.userSelectOptions = merged.sort((a, b) => a.txtUserName.localeCompare(b.txtUserName));
    }

    private mapUserToOption(u: any): DelegationUserOption {
        return {
            serUserId: Number(u.serUserId),
            txtUserName: (u.txtUserName || u.txtLoginName || `User #${u.serUserId}`).toString(),
            txtCnic: u.txtCnic,
            roleName: this.getUserRoleName(u),
        };
    }

    private resolveUserOption(
        userId: number,
        name: string,
        employeeId?: string,
        roleName?: string
    ): DelegationUserOption {
        const id = Number(userId);
        const existing = this.userSelectOptions.find((o) => o.serUserId === id);
        if (existing) {
            return existing;
        }
        const fromUsers = this.userById.get(id);
        if (fromUsers) {
            return this.mapUserToOption(fromUsers);
        }
        const synthetic: DelegationUserOption = {
            serUserId: id,
            txtUserName: name || `User #${id}`,
            txtCnic: employeeId,
            roleName,
        };
        this.userSelectOptions = [...this.userSelectOptions, synthetic].sort((a, b) =>
            a.txtUserName.localeCompare(b.txtUserName)
        );
        return synthetic;
    }

    private isSelectableUser(u: any): boolean {
        if (!u) {
            return false;
        }
        const id = Number(u.serUserId);
        if (!Number.isFinite(id) || id <= 0) {
            return false;
        }
        if (u.blIsDeleted === true || u.blIsDeleted === 1 || u.blIsDeleted === '1') {
            return false;
        }
        return true;
    }

    onOnBehalfUserChanged(user: DelegationUserOption | null): void {
        const onBehalfUserId = Number(user?.serUserId);
        if (!onBehalfUserId) {
            this.publishedDocuments = [];
            this.formDocumentIds = [];
            return;
        }
        this.loadDocumentsForOnBehalfUser(onBehalfUserId, true);
    }

    loadDocumentsForOnBehalfUser(onBehalfUserId: number, pruneInvalidSelections = false): void {
        const id = Number(onBehalfUserId);
        if (!Number.isFinite(id) || id <= 0) {
            this.publishedDocuments = [];
            return;
        }
        this.loadingDocuments = true;
        this.userDelegationService
            .getDelegatableDocuments(id)
            .pipe(finalize(() => (this.loadingDocuments = false)))
            .subscribe({
                next: (docs) => {
                    this.publishedDocuments = (docs || [])
                        .filter((d) => d?.serFormId)
                        .map((d) => ({
                            id: String(d.serFormId),
                            name: d.txtTemplateName || `Form #${d.serFormId}`,
                            codeConvention: d.txtCodeConvention,
                        }));
                    if (pruneInvalidSelections) {
                        this.pruneDocumentSelectionToAvailable();
                    }
                },
                error: () => {
                    this.publishedDocuments = [];
                    if (pruneInvalidSelections) {
                        this.formDocumentIds = [];
                    }
                    this.notificationService.showMessage(
                        'Could not load documents for the on-behalf user.',
                        'danger'
                    );
                },
            });
    }

    private pruneDocumentSelectionToAvailable(): void {
        const allowed = new Set(this.publishedDocuments.map((d) => String(d.id)));
        const next = (this.formDocumentIds || []).filter((docId) => allowed.has(String(docId)));
        if (next.length !== (this.formDocumentIds || []).length) {
            this.formDocumentIds = next;
        }
    }

    private onTourPrepare = (event: Event): void => {
        const action = (event as CustomEvent<{ action?: string }>).detail?.action;
        switch (action) {
            case 'delegation-close-modal':
                this.delegationModal?.close();
                break;
            case 'delegation-open-add-modal':
                if (!document.querySelector('[data-tour="delegation-modal"]')) {
                    this.openAddModal();
                }
                break;
            default:
                break;
        }
    };

    openAddModal(): void {
        this.modalMode = 'add';
        this.editingDelegationId = null;
        this.formDelegateUser = null;
        this.formOnBehalfUser = null;
        this.formValidFrom = '';
        this.formValidTo = '';
        this.formDocumentIds = [];
        this.formNotes = '';
        this.publishedDocuments = [];
        this.resetAttachmentForm();
        this.formNotifyBothParties = true;
        if (!this.userSelectOptions.length) {
            this.loadUsers();
        }
        this.delegationModal.open();
    }

    openEditModal(row: DelegationRow): void {
        this.modalMode = 'edit';
        this.editingDelegationId = row.serDelegationId;
        this.formDelegateUser = this.resolveUserOption(
            row.delegateUserId,
            row.delegateUserName,
            row.delegateEmployeeId
        );
        this.formOnBehalfUser = this.resolveUserOption(
            row.onBehalfUserId,
            row.onBehalfUserName,
            undefined,
            row.onBehalfRole
        );
        this.formValidFrom = row.validFrom.slice(0, 10);
        this.formValidTo = row.validTo.slice(0, 10);
        this.formDocumentIds = this.parseScopeIds(row.documentScopeIds);
        this.formNotes = row.notes;
        this.formAttachmentBase64 = null;
        this.formAttachmentFileName = '';
        this.formRemoveAttachment = false;
        this.formExistingAttachment = row.hasAttachment;
        this.formExistingAttachmentName = row.attachmentFileName || '';
        this.formNotifyBothParties = false;
        if (!this.userSelectOptions.length) {
            this.loadUsers();
        }
        this.loadDocumentsForOnBehalfUser(row.onBehalfUserId, true);
        this.delegationModal.open();
    }

    saveDelegation(): void {
        const delegateUserId = Number(this.formDelegateUser?.serUserId);
        const onBehalfUserId = Number(this.formOnBehalfUser?.serUserId);
        if (!delegateUserId || !onBehalfUserId) {
            this.notificationService.showMessage('Please select delegate and on-behalf users.', 'danger');
            return;
        }
        if (delegateUserId === onBehalfUserId) {
            this.notificationService.showMessage('Delegate and on-behalf user must be different.', 'danger');
            return;
        }
        if (!this.formValidFrom || !this.formValidTo) {
            this.notificationService.showMessage('Valid from and valid to are required.', 'danger');
            return;
        }
        if (this.formValidTo < this.formValidFrom) {
            this.notificationService.showMessage('Valid to must be on or after valid from.', 'danger');
            return;
        }
        if (!onBehalfUserId) {
            this.notificationService.showMessage('Please select the on-behalf user first.', 'danger');
            return;
        }
        if (!this.formDocumentIds?.length) {
            this.notificationService.showMessage('Select at least one document visible to the on-behalf user.', 'danger');
            return;
        }

        const payload: any = {
            serDelegationId: this.editingDelegationId ?? undefined,
            serDelegateUserId: delegateUserId,
            serOnBehalfUserId: onBehalfUserId,
            dteValidFrom: this.formValidFrom,
            dteValidTo: this.formValidTo,
            documentIds: this.formDocumentIds.map((id) => String(id)),
            txtNotes: this.formNotes?.trim() || undefined,
        };
        if (this.formAttachmentBase64) {
            payload.attachmentBase64 = this.formAttachmentBase64;
            payload.attachmentFileName = this.formAttachmentFileName || 'attachment';
        } else if (this.formRemoveAttachment) {
            payload.removeAttachment = true;
        }
        payload.notifyBothParties = !!this.formNotifyBothParties;

        this.saving = true;
        const request$ =
            this.modalMode === 'edit' && this.editingDelegationId
                ? this.userDelegationService.update({
                      ...payload,
                      serDelegationId: this.editingDelegationId,
                  })
                : this.userDelegationService.create(payload);

        request$.pipe(finalize(() => (this.saving = false))).subscribe({
            next: () => {
                this.delegationModal.close();
                const notifyMsg = this.formNotifyBothParties
                    ? ' Delegation emails are queued for both parties.'
                    : '';
                this.notificationService.showMessage('Delegation saved successfully.' + notifyMsg, 'success');
                this.loadDelegations();
            },
            error: (err) => {
                this.notificationService.showMessage(err?.message || 'Could not save delegation.', 'danger');
            },
        });
    }

    onAttachmentSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        input.value = '';
        if (!file) {
            return;
        }
        if (file.size > this.attachmentMaxBytes) {
            this.notificationService.showMessage('Attachment must be 10 MB or smaller.', 'danger');
            return;
        }
        const allowed = /\.(pdf|jpe?g|png|gif|webp|doc|docx)$/i.test(file.name);
        if (!allowed && !this.isAllowedAttachmentMime(file.type)) {
            this.notificationService.showMessage('Use PDF, image, or Word document.', 'danger');
            return;
        }
        const reader = new FileReader();
        reader.onload = () => {
            this.formAttachmentBase64 = String(reader.result || '');
            this.formAttachmentFileName = file.name;
            this.formRemoveAttachment = false;
            this.formExistingAttachment = false;
            this.cdr.markForCheck();
        };
        reader.readAsDataURL(file);
    }

    clearNewAttachment(): void {
        this.formAttachmentBase64 = null;
        this.formAttachmentFileName = '';
    }

    removeExistingAttachment(): void {
        this.formRemoveAttachment = true;
        this.formExistingAttachment = false;
        this.formExistingAttachmentName = '';
        this.clearNewAttachment();
    }

    downloadRowAttachment(row: DelegationRow): void {
        if (!row?.serDelegationId || !row.hasAttachment) {
            return;
        }
        this.userDelegationService.downloadAttachment(row.serDelegationId).subscribe({
            next: (blob) => {
                const url = URL.createObjectURL(blob);
                const anchor = document.createElement('a');
                anchor.href = url;
                anchor.download = row.attachmentFileName || `delegation-${row.serDelegationId}-proof`;
                anchor.click();
                URL.revokeObjectURL(url);
            },
            error: () => {
                this.notificationService.showMessage('Could not download attachment.', 'danger');
            },
        });
    }

    get attachmentFormLabel(): string {
        if (this.formAttachmentFileName) {
            return this.formAttachmentFileName;
        }
        if (this.formExistingAttachment && this.formExistingAttachmentName) {
            return this.formExistingAttachmentName;
        }
        return '';
    }

    private resetAttachmentForm(): void {
        this.formAttachmentBase64 = null;
        this.formAttachmentFileName = '';
        this.formExistingAttachment = false;
        this.formExistingAttachmentName = '';
        this.formRemoveAttachment = false;
    }

    private isAllowedAttachmentMime(mime: string): boolean {
        const m = (mime || '').toLowerCase();
        return (
            m === 'application/pdf' ||
            m.startsWith('image/') ||
            m === 'application/msword' ||
            m === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        );
    }

    revokeDelegation(row: DelegationRow): void {
        this.userDelegationService.revoke(row.serDelegationId).subscribe({
            next: () => {
                this.notificationService.showMessage('Delegation revoked.', 'success');
                this.loadDelegations();
            },
            error: (err) => {
                this.notificationService.showMessage(err?.message || 'Could not revoke delegation.', 'danger');
            },
        });
    }

    getUserRoleName(user: any): string {
        return (
            user?.cfgTblRole?.txtRoleName ||
            user?.txtrole ||
            user?.roleName ||
            ''
        ).toString();
    }

    getAvatarLetter(name: string): string {
        const trimmed = String(name || '')
            .trim()
            .replace(/^(mr|ms|mrs|dr)\.?\s+/i, '');
        if (!trimmed) {
            return '?';
        }
        return trimmed.charAt(0).toUpperCase();
    }

    hasProfilePhoto(userId: number): boolean {
        const id = Number(userId);
        return id > 0 && !!this.profilePhotoSafeUrls[id];
    }

    getProfilePhotoUrl(userId: number): SafeUrl | null {
        const id = Number(userId);
        if (!id) {
            return null;
        }
        return this.profilePhotoSafeUrls[id] || null;
    }

    documentsDisplay(row: DelegationRow): string {
        const label = (row?.documentsLabel || '').trim();
        return label || '—';
    }

    documentsDisplayTitle(row: DelegationRow): string {
        return this.documentsDisplay(row);
    }

    statusLabel(status: DelegationStatus): string {
        const opt = this.statusOptions.find((o) => o.value === status);
        return opt?.label || status;
    }

    statusBadgeClass(status: DelegationStatus): string {
        switch (status) {
            case 'ACTIVE':
                return 'badge-outline-success';
            case 'SCHEDULED':
                return 'badge-outline-info';
            case 'EXPIRED':
                return 'badge-outline-secondary';
            case 'REVOKED':
                return 'badge-outline-danger';
            default:
                return 'badge-outline-primary';
        }
    }

    applyFilters(): void {
        this.loadDelegations();
    }

    clearFilters(): void {
        this.search = '';
        this.filterStatus = '';
        this.loadDelegations();
    }

    private mapDtoToRow(d: UserDelegationDto): DelegationRow {
        const status = (d.txtStatus || 'ACTIVE').toUpperCase() as DelegationStatus;
        return {
            serDelegationId: d.serDelegationId,
            delegateUserId: d.serDelegateUserId,
            delegateUserName: d.delegateUserName || '—',
            delegateEmployeeId: d.delegateEmployeeId || '',
            onBehalfUserId: d.serOnBehalfUserId,
            onBehalfUserName: d.onBehalfUserName || '—',
            onBehalfRole: d.onBehalfRole || '',
            validFrom: d.dteValidFrom,
            validTo: d.dteValidTo,
            documentScopeIds: d.txtScope || (d.documentIds || []).join(','),
            documentsLabel: d.documentsLabel || '',
            status,
            createdByName: d.createdByName || '—',
            notes: d.txtNotes || '',
            hasAttachment: !!d.hasAttachment,
            attachmentFileName: d.attachmentFileName || '',
            actions: true,
        };
    }

    private parseScopeIds(csv: string): string[] {
        return (csv || '')
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean);
    }

    private getCurrentUserId(): number | null {
        try {
            const raw = localStorage.getItem('user');
            if (!raw) {
                return null;
            }
            const user = JSON.parse(raw);
            const id = Number(user?.serUserId);
            return Number.isFinite(id) && id > 0 ? id : null;
        } catch {
            return null;
        }
    }

    private refreshSummary(): void {
        this.summary.active = this.rows.filter((r) => r.status === 'ACTIVE').length;
        this.summary.scheduled = this.rows.filter((r) => r.status === 'SCHEDULED').length;
        this.summary.expired = this.rows.filter((r) => r.status === 'EXPIRED').length;
    }

    private indexUsersById(): void {
        this.userById.clear();
        (this.users || []).forEach((user) => {
            const id = Number(user?.serUserId);
            if (id > 0) {
                this.userById.set(id, user);
            }
        });
    }

    private prefetchProfilePhotosForRows(rows: DelegationRow[]): void {
        const ids = new Set<number>();
        (rows || []).forEach((row) => {
            if (row.delegateUserId) {
                ids.add(row.delegateUserId);
            }
            if (row.onBehalfUserId) {
                ids.add(row.onBehalfUserId);
            }
        });
        ids.forEach((id) => this.prefetchProfilePhoto(id));
    }

    private prefetchProfilePhoto(userId: number): void {
        const id = Number(userId);
        if (!id || this.profilePhotoSafeUrls[id] || this.profilePhotoLoading.has(id)) {
            return;
        }
        const user = this.userById.get(id);
        if (!this.userHasProfilePicturePath(user)) {
            return;
        }
        this.profilePhotoLoading.add(id);
        this.userService.loadProfilePictureObjectUrl(id).subscribe({
            next: (objectUrl) => {
                this.profilePhotoBlobUrls[id] = objectUrl;
                this.profilePhotoSafeUrls[id] = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
                this.profilePhotoLoading.delete(id);
                this.cdr.markForCheck();
            },
            error: () => {
                this.profilePhotoLoading.delete(id);
            },
        });
    }

    private userHasProfilePicturePath(user: any): boolean {
        const path = String(user?.txtProfilePicturePath || '').trim();
        return path.length > 0;
    }
}
