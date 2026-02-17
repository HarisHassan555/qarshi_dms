import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { urls } from '../../utils/urls';

@Component({
  selector: 'app-email-approval',
  templateUrl: './email-approval.component.html',
  styleUrls: ['./email-approval.component.css']
})
export class EmailApprovalComponent implements OnInit {
  applicationId: number | null = null;
  userId: number | null = null;
  action: 'approve' | 'reject' | 'sendback' = 'approve';
  isLoading: boolean = true;
  result: {
    success: boolean;
    message: string;
  } | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.applicationId = params['applicationId'] ? parseInt(params['applicationId'], 10) : null;
      this.userId = params['userId'] ? parseInt(params['userId'], 10) : null;
      
      // Determine action from route
      const path = this.route.snapshot.url[0]?.path;
      if (path === 'approveApplicationFromEmail') {
        this.action = 'approve';
      } else if (path === 'rejectApplicationFromEmail') {
        this.action = 'reject';
      } else if (path === 'sendBackApplicationFromEmail') {
        this.action = 'sendback';
      }

      if (this.applicationId && this.userId) {
        this.processAction();
      } else {
        this.result = {
          success: false,
          message: 'Invalid application ID or user ID'
        };
        this.isLoading = false;
      }
    });
  }

  processAction() {
    if (!this.applicationId || !this.userId) {
      return;
    }

    // Call the backend GET endpoint directly (no authentication required)
    const endpoint = this.action === 'approve'
      ? 'approveApplicationFromEmail'
      : this.action === 'reject'
        ? 'rejectApplicationFromEmail'
        : 'sendBackApplicationFromEmail';
    
    const url = `${urls.API_URL}${endpoint}?applicationId=${this.applicationId}&userId=${this.userId}`;
    
    // Backend returns HTML, so we use text response type
    this.http.get(url, { responseType: 'text' }).subscribe(
      (htmlResponse: string) => {
        // Parse HTML to determine success/failure
        // The backend returns HTML with success/error indicators
        const isSuccess = htmlResponse.includes('Application Approved Successfully') || 
                         htmlResponse.includes('Application Rejected') ||
                         htmlResponse.includes('Application Sent Back');
        const isError = htmlResponse.includes('Failed') || htmlResponse.includes('Error');
        
        if (isSuccess) {
          this.result = {
            success: true,
            message: `Application ${this.action === 'approve' ? 'approved' : this.action === 'reject' ? 'rejected' : 'sent back'} successfully`
          };
        } else if (isError) {
          // Try to extract error message from HTML
          const errorMatch = htmlResponse.match(/<div class='message'>(.*?)<\/div>/);
          const errorMessage = errorMatch ? errorMatch[1] : `Failed to ${this.action} application`;
          this.result = {
            success: false,
            message: errorMessage
          };
        } else {
          this.result = {
            success: false,
            message: `Unable to determine result`
          };
        }
        this.isLoading = false;
      },
      (error) => {
        this.result = {
          success: false,
          message: error.error?.message || error.message || 
            `Error ${this.action === 'approve' ? 'approving' : this.action === 'reject' ? 'rejecting' : 'sending back'} application`
        };
        this.isLoading = false;
      }
    );
  }

  goToDashboard() {
    this.router.navigate(['/Dashboard']);
  }
}
