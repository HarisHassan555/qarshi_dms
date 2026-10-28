import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ProfileDrawerService } from '../services/profile-drawer/profile-drawer.service';
import { SharedDataService } from '../services/shared-data/shared-data.service';
import { UserService } from '../services/user/user.service';
import { NotificationService } from '../NotificationService';

@Component({
  selector: 'app-profile-drawer',
  templateUrl: './profile-drawer.component.html',
  styleUrls: ['./profile-drawer.component.css'],
})
export class ProfileDrawerComponent implements OnInit, OnDestroy {
  @ViewChild('profilePhotoInput') profilePhotoInput?: ElementRef<HTMLInputElement>;

  open = false;
  user: any;
  profileDetails: any = null;
  profileLoading = false;
  profilePictureUploading = false;
  profilePhotoSafeUrl: SafeUrl | null = null;
  pendingPhotoSafeUrl: SafeUrl | null = null;
  private profilePhotoBlobUrl: string | null = null;
  private openSub?: Subscription;
  private userSub?: Subscription;

  constructor(
    private profileDrawerService: ProfileDrawerService,
    private sharedDataService: SharedDataService,
    private userService: UserService,
    private sanitizer: DomSanitizer,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.userSub = this.sharedDataService.getUser().subscribe((data) => {
      if (data) {
        this.user = data;
      }
    });
    this.openSub = this.profileDrawerService.open$.subscribe(() => this.show());
  }

  ngOnDestroy(): void {
    this.openSub?.unsubscribe();
    this.userSub?.unsubscribe();
    this.revokeProfilePhoto();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) {
      this.close();
    }
  }

  show(): void {
    this.open = true;
    this.loadProfileDetails();
    document.body.classList.add('profile-drawer-open');
  }

  close(): void {
    this.open = false;
    this.pendingPhotoSafeUrl = null;
    document.body.classList.remove('profile-drawer-open');
  }

  loadProfileDetails(): void {
    this.profileLoading = true;
    this.profileDetails = null;
    this.revokeProfilePhoto();
    this.userService.getCurrentUser().subscribe({
      next: (data) => {
        this.profileDetails = data;
        this.profileLoading = false;
        this.loadProfilePhoto(data);
      },
      error: () => {
        this.profileDetails = this.user;
        this.profileLoading = false;
        this.loadProfilePhoto(this.user);
      },
    });
  }

  private loadProfilePhoto(profile: any): void {
    this.revokeProfilePhoto();
    const userId = Number(profile?.serUserId);
    const path = String(profile?.txtProfilePicturePath || '').trim();
    if (!userId || !path) {
      return;
    }
    this.userService.loadProfilePictureObjectUrl(userId).subscribe({
      next: (objectUrl) => {
        this.profilePhotoBlobUrl = objectUrl;
        this.profilePhotoSafeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
      },
      error: () => {
        this.profilePhotoSafeUrl = null;
      },
    });
  }

  private revokeProfilePhoto(): void {
    if (this.profilePhotoBlobUrl) {
      URL.revokeObjectURL(this.profilePhotoBlobUrl);
      this.profilePhotoBlobUrl = null;
    }
    this.profilePhotoSafeUrl = null;
  }

  triggerProfilePhotoPicker(): void {
    this.profilePhotoInput?.nativeElement?.click();
  }

  onProfilePhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    input.value = '';
    if (!file || !this.profileDetails) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = String(reader.result || '');
      if (!dataUrl) {
        return;
      }
      this.pendingPhotoSafeUrl = this.sanitizer.bypassSecurityTrustUrl(dataUrl);
      this.uploadProfilePhoto(dataUrl);
    };
    reader.readAsDataURL(file);
  }

  private uploadProfilePhoto(dataUrl: string): void {
    const currentId = Number(this.user?.serUserId);
    const profileId = Number(this.profileDetails?.serUserId);
    if (!profileId || (currentId && profileId !== currentId)) {
      this.notificationService.showMessage('You can only update your own profile photo', 'danger');
      return;
    }

    this.profilePictureUploading = true;
    const payload = this.buildProfileUpdatePayload(dataUrl);
    this.userService.save(payload).subscribe({
      next: (response: unknown) => {
        this.profilePictureUploading = false;
        if (this.resolveSaveStatus(response) === 'Success') {
          this.notificationService.showMessage('Profile photo updated', 'success');
          this.pendingPhotoSafeUrl = null;
          this.profileDrawerService.notifyProfilePhotoUpdated();
          this.loadProfileDetails();
        } else {
          this.notificationService.showMessage('Could not update profile photo', 'danger');
        }
      },
      error: () => {
        this.profilePictureUploading = false;
        this.pendingPhotoSafeUrl = null;
        this.notificationService.showMessage('Could not update profile photo', 'danger');
      },
    });
  }

  private buildProfileUpdatePayload(profilePictureBase64: string): Record<string, unknown> {
    const p = this.profileDetails || {};
    const deptId = Number(p?.hrTblDepartment?.serDepartmentId);
    const payload: Record<string, unknown> = {
      serUserId: p.serUserId,
      txtUserName: p.txtUserName,
      txtLoginName: p.txtLoginName,
      txtAddress: p.txtAddress,
      txtCnic: p.txtCnic,
      txtContactNo: p.txtContactNo,
      txtDesignation: p.txtDesignation,
      txtDepartmentName: p.txtDepartmentName,
      txtOrganizationName: p.txtOrganizationName,
      txtSubareaName: p.txtSubareaName,
      txtDivisionName: p.txtDivisionName,
      serOrganizationId: p.serOrganizationId,
      serSubareaId: p.serSubareaId,
      serDivisionId: p.serDivisionId,
      txtProfilePicturePath: p.txtProfilePicturePath,
      numMaxAttachmentMb: p.numMaxAttachmentMb ?? 5,
      blnStatus: p.blnStatus,
      blIsDeleted: false,
      profilePictureBase64,
      cfgTblRole: p.cfgTblRole?.serRoleId
        ? { serRoleId: Number(p.cfgTblRole.serRoleId) }
        : p.cfgTblRole,
      cfgTblManager: p.cfgTblManager?.serUserId
        ? { serUserId: Number(p.cfgTblManager.serUserId) }
        : null,
      cfgTblCustomer: p.cfgTblCustomer?.serCustomerId
        ? { serCustomerId: Number(p.cfgTblCustomer.serCustomerId) }
        : null,
      hrTblDepartment: deptId > 0 ? { serDepartmentId: deptId } : null,
    };
    return payload;
  }

  private resolveSaveStatus(response: unknown): string {
    if (response == null) {
      return 'Failure';
    }
    if (typeof response === 'string') {
      const trimmed = response.trim();
      if (trimmed === 'Success' || trimmed === 'Failure' || trimmed === 'AX') {
        return trimmed;
      }
      try {
        const parsed = JSON.parse(trimmed);
        return String((parsed as { status?: string })?.status || trimmed);
      } catch {
        return trimmed;
      }
    }
    return String((response as { status?: string })?.status || 'Failure');
  }

  showAvatarImage(): boolean {
    return this.pendingPhotoSafeUrl != null || this.profilePhotoSafeUrl != null;
  }

  getAvatarImageUrl(): SafeUrl | null {
    return this.pendingPhotoSafeUrl || this.profilePhotoSafeUrl;
  }

  getAvatarLetter(name: string): string {
    const trimmed = String(name || '').trim();
    if (!trimmed) {
      return '?';
    }
    return trimmed.charAt(0).toUpperCase();
  }

  profileDisplayName(): string {
    return this.profileDetails?.txtUserName || this.user?.txtUserName || '';
  }

  profileField(value: unknown): string {
    const text = String(value ?? '').trim();
    return text || '—';
  }

  profileRoleName(): string {
    const role = this.profileDetails?.cfgTblRole;
    if (role && typeof role === 'object') {
      return this.profileField(role.txtRoleName);
    }
    return this.profileField(role);
  }

  profileDepartmentName(): string {
    return this.profileField(
      this.profileDetails?.txtDepartmentName
        || this.profileDetails?.hrTblDepartment?.txtDepartmentName
    );
  }

  profileStatusLabel(): string {
    const active = this.profileDetails?.blnStatus;
    if (active === true) {
      return 'Active';
    }
    if (active === false) {
      return 'Inactive';
    }
    return '—';
  }

  navigateFromDrawer(path: string): void {
    this.close();
    this.router.navigateByUrl(path);
  }

  logoutFromDrawer(): void {
    this.close();
    this.router.navigateByUrl('auth/signin');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }
}
