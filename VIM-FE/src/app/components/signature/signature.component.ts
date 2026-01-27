import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { NotificationService } from 'src/app/NotificationService';
import { PermissionService } from '../../services/shared-data/permission-service';
import { urls } from 'src/app/utils/urls';

@Component({
  selector: 'app-signature',
  templateUrl: './signature.component.html',
  styleUrls: ['./signature.component.css']
})
export class SignatureComponent implements OnInit {
  selectedFile: File | null = null;
  previewUrl: string | null = null;
  currentSignaturePath: string | null = null;
  isLoading = false;
  hasSignature = false;

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
    this.loadCurrentSignature();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.notificationService.showMessage('Please select an image file', 'danger');
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.notificationService.showMessage('File size should be less than 5MB', 'danger');
        return;
      }

      this.selectedFile = file;

      // Create preview
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.previewUrl = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  uploadSignature() {
    if (!this.selectedFile) {
      this.notificationService.showMessage('Please select a file to upload', 'danger');
      return;
    }

    this.isLoading = true;

    const formData = new FormData();
    formData.append('file', this.selectedFile);

    // Angular HttpClient automatically sets Content-Type to multipart/form-data with boundary for FormData
    // Don't set headers - let Angular handle it automatically
    this.http.post<any>(`${urls.API_URL}uploadSignature`, formData)
      .subscribe(
        (response) => {
          if (response.status === 'Success') {
            this.notificationService.showMessage('Signature uploaded successfully', 'success');
            this.currentSignaturePath = response.signaturePath;
            this.hasSignature = true;
            this.selectedFile = null;
            this.loadCurrentSignature();
          } else {
            this.notificationService.showMessage(response.message || 'Failed to upload signature', 'danger');
          }
          this.isLoading = false;
        },
        (error) => {
          console.error('Error uploading signature:', error);
          this.notificationService.showMessage(
            error.error?.message || 'Error uploading signature. Please try again.',
            'danger'
          );
          this.isLoading = false;
        }
      );
  }

  loadCurrentSignature() {
    this.isLoading = true;
    this.http.get<any>(`${urls.API_URL}getUserSignaturePath`)
      .subscribe(
        (response) => {
          if (response.status === 'Success') {
            this.hasSignature = response.hasSignature;
            this.currentSignaturePath = response.signaturePath;
            
            if (this.hasSignature && this.currentSignaturePath) {
              // Load signature image
              this.loadSignatureImage();
            }
          }
          this.isLoading = false;
        },
        (error) => {
          console.error('Error loading signature:', error);
          this.isLoading = false;
        }
      );
  }

  loadSignatureImage() {
    if (!this.currentSignaturePath) return;

    this.http.get(`${urls.API_URL}getSignature`, {
      responseType: 'blob'
    }).subscribe(
      (blob) => {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.previewUrl = e.target.result;
        };
        reader.readAsDataURL(blob);
      },
      (error) => {
        console.error('Error loading signature image:', error);
      }
    );
  }

  removeFile() {
    this.selectedFile = null;
    this.previewUrl = this.currentSignaturePath ? this.previewUrl : null;
  }

  clearSignature() {
    this.selectedFile = null;
    this.previewUrl = null;
  }
}

