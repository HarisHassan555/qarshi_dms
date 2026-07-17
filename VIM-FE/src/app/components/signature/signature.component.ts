import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { NotificationService } from 'src/app/NotificationService';
import { PermissionService } from '../../services/shared-data/permission-service';
import { DepartmentService } from '../../services/department/department.service';
import { urls } from 'src/app/utils/urls';

@Component({
  selector: 'app-signature',
  templateUrl: './signature.component.html',
  styleUrls: ['./signature.component.css']
})
export class SignatureComponent implements OnInit {
  @ViewChild('signatureFileInput') signatureFileInput!: ElementRef<HTMLInputElement>;
  selectedFile: File | null = null;
  previewUrl: string | null = null;
  currentSignaturePath: string | null = null;
  isLoading = false;
  hasSignature = false;
  department = '';
  designation = '';
  departments: string[] = [];

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService,
    private permissionService: PermissionService,
    private departmentService: DepartmentService
  ) { }

  ngOnInit() {
    this.loadUserFieldsFromLocalStorage();
    this.loadDepartments();
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

    if (!this.department.trim() || !this.designation.trim()) {
      this.notificationService.showMessage('Department and designation are required', 'danger');
      return;
    }

    this.isLoading = true;

      // Convert file to base64
      const reader = new FileReader();
      reader.onload = (e: any) => {
          const base64Data = e.target.result;

          // Prepare request body with base64 data
          const requestBody = {
              signature: base64Data,
              fileType: this.selectedFile!.type || 'image/png',
              department: this.department.trim(),
              designation: this.designation.trim(),
              txtDepartmentName: this.department.trim(),
              txtDesignation: this.designation.trim()
          };

          // Add userId if available
          const userJson = localStorage.getItem('user');
          let params = {};
          if (userJson) {
              try {
                  const user = JSON.parse(userJson);
                  if (user?.serUserId) {
                      params = { userId: String(user.serUserId) };
                  }
              } catch (e) {
                  console.error('Error parsing user from localStorage', e);
              }
          }

          // Send as JSON with base64 data
          const headers = new HttpHeaders({
              'Content-Type': 'application/json'
          });


          console.log('Uploading signature with request body:', requestBody, 'and params:', params)

          this.http.post<any>(`${urls.API_URL}uploadSignature`, requestBody, { headers, params })
              .subscribe(
                  (response) => {
                      if (response.status === 'Success') {
                          this.notificationService.showMessage('Signature uploaded successfully', 'success');
                          this.currentSignaturePath = response.signaturePath;
                          this.hasSignature = true;
                          this.updateUserInLocalStorage();
                          this.selectedFile = null;
                          this.loadCurrentSignature();
                      } else {
                          console.error('Failed to upload signature:', response);
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
      };

      reader.readAsDataURL(this.selectedFile);

  }

  saveDetails() {
    if (!this.department.trim() || !this.designation.trim()) {
      this.notificationService.showMessage('Department and designation are required', 'danger');
      return;
    }

    this.isLoading = true;

    const requestBody = {
      signature: '',
      fileType: 'image/png',
      department: this.department.trim(),
      designation: this.designation.trim(),
      txtDepartmentName: this.department.trim(),
      txtDesignation: this.designation.trim()
    };

    const userJson = localStorage.getItem('user');
    let params = {};
    if (userJson) {
      try {
        const user = JSON.parse(userJson);
        if (user?.serUserId) {
          params = { userId: String(user.serUserId) };
        }
      } catch (e) {
        console.error('Error parsing user from localStorage', e);
      }
    }

    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    this.http.post<any>(`${urls.API_URL}uploadSignature`, requestBody, { headers, params })
      .subscribe(
        (response) => {
          if (response.status === 'Success') {
            this.notificationService.showMessage('Details saved successfully', 'success');
            this.mapUserFieldsFromResponse(response);
            this.updateUserInLocalStorage();
          } else {
            this.notificationService.showMessage(response.message || 'Failed to save details', 'danger');
          }
          this.isLoading = false;
        },
        (error) => {
          console.error('Error saving details:', error);
          this.notificationService.showMessage(
            error.error?.message || 'Error saving details. Please try again.',
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
            this.mapUserFieldsFromResponse(response);

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

  onSave() {
    if (this.selectedFile) {
      this.uploadSignature();
      return;
    }
    this.saveDetails();
  }

  onReset() {
    this.selectedFile = null;
    this.department = '';
    this.designation = '';
    this.loadUserFieldsFromLocalStorage();
    if (this.hasSignature && this.currentSignaturePath) {
      this.loadSignatureImage();
    } else {
      this.previewUrl = null;
    }
    if (this.signatureFileInput?.nativeElement) {
      this.signatureFileInput.nativeElement.value = '';
    }
  }

  onDepartmentChange(event: Event) {
    const target = event.target as HTMLSelectElement;
    this.department = target?.value || '';
  }

  onDesignationInput(event: Event) {
    const target = event.target as HTMLInputElement;
    this.designation = target?.value || '';
  }

  private loadUserFieldsFromLocalStorage() {
    const userJson = localStorage.getItem('user');
    if (!userJson) return;

    try {
      const user = JSON.parse(userJson);
      this.department =
        user?.txtDepartmentName ||
        user?.departmentName ||
        user?.hrTblDepartment?.txtDepartmentName ||
        this.department;
      this.designation =
        user?.txtDesignation ||
        user?.designation ||
        this.designation;
      this.ensureCurrentDepartmentOption();
    } catch (e) {
      console.error('Error parsing user from localStorage', e);
    }
  }

  private loadDepartments() {
    this.departmentService.getAll().subscribe(
      (response: any) => {
        const departments = Array.isArray(response) ? response : [];
        this.departments = Array.from(
          new Set(
            departments
              .map((dept: any) => (dept?.txtDepartmentName || dept?.departmentName || '').trim())
              .filter((name: string) => !!name)
          )
        ).sort((a, b) => a.localeCompare(b));
        this.ensureCurrentDepartmentOption();
      },
      (error) => {
        console.error('Error loading departments:', error);
        this.ensureCurrentDepartmentOption();
      }
    );
  }

  private mapUserFieldsFromResponse(response: any) {
    this.department =
      response?.txtDepartmentName ||
      response?.departmentName ||
      response?.department ||
      this.department;
    this.designation =
      response?.txtDesignation ||
      response?.designation ||
      this.designation;
    this.ensureCurrentDepartmentOption();
  }

  private updateUserInLocalStorage() {
    const userJson = localStorage.getItem('user');
    if (!userJson) return;

    try {
      const user = JSON.parse(userJson);
      user.txtDepartmentName = this.department.trim();
      user.departmentName = this.department.trim();
      user.txtDesignation = this.designation.trim();
      localStorage.setItem('user', JSON.stringify(user));
    } catch (e) {
      console.error('Error updating user in localStorage', e);
    }
  }

  private ensureCurrentDepartmentOption() {
    const currentDepartment = this.department.trim();
    if (!currentDepartment || this.departments.includes(currentDepartment)) {
      return;
    }

    this.departments = [...this.departments, currentDepartment].sort((a, b) => a.localeCompare(b));
  }
}
