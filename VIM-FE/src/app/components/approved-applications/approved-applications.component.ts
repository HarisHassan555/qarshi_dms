import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CustomFormApplicationService } from '../../services/custom-form-application/custom-form-application.service';
import { CustomFormService } from '../../services/custom-form/custom-form.service';
import { NotificationService } from 'src/app/NotificationService';

interface Application {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    serSubmittedBy?: number;
    dteCreatedDate?: string;
    cfgTblCustomForm?: any;
    formName?: string;
    txtAssetCode?: string;
    txtPrCode?: string;
    myApprovalDate?: string;  // Date this user approved (extracted from history)
}

@Component({
    selector: 'app-approved-applications',
    templateUrl: './approved-applications.component.html',
    styleUrls: ['./approved-applications.component.css']
})
export class ApprovedApplicationsComponent implements OnInit {
    search = '';
    applications: Application[] = [];
    forms: any[] = [];
    currentUser: any;
    loading = false;

    cols = [
        { field: 'txtFormCode', title: 'Application Code' },
        { field: 'formName', title: 'Form Name' },
        { field: 'txtStatus', title: 'Status', sort: false },
        { field: 'txtAssetCode', title: 'Asset Code' },
        { field: 'txtPrCode', title: 'PR Code' },
        { field: 'myApprovalDate', title: 'Approved On' },
        { field: 'dteCreatedDate', title: 'Submitted On' },
        { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
    ];

    constructor(
        private customFormApplicationService: CustomFormApplicationService,
        private customFormService: CustomFormService,
        private notificationService: NotificationService,
        private router: Router
    ) { }

    ngOnInit() {
        const userJson = localStorage.getItem('user');
        if (userJson) {
            this.currentUser = JSON.parse(userJson);
            this.loadForms();
            this.loadApprovedApplications();
        }
    }

    loadForms() {
        this.customFormService.getAll().subscribe(
            (data: any) => { if (data) this.forms = data; },
            (error) => console.error('Error loading forms', error)
        );
    }

    loadApprovedApplications() {
        this.loading = true;
        const userId = this.currentUser?.serUserId || this.currentUser?.ser_user_id;
        if (!userId) {
            this.loading = false;
            this.notificationService.showMessage('Could not determine logged-in user ID', 'danger');
            return;
        }
        // The backend now searches ALL statuses for applications the user approved
        this.customFormApplicationService.getApplicationsApprovedByUser('APPROVED', userId).subscribe(
            (data: any) => {
                this.loading = false;
                if (data) {
                    this.applications = data.map((app: any) => ({
                        ...app,
                        formName: this.getFormName(app),
                        myApprovalDate: this.extractMyApprovalDate(app, userId)
                    }));
                }
            },
            (error) => {
                this.loading = false;
                console.error('Error loading approved applications', error);
                this.notificationService.showMessage('Error loading approved applications', 'danger');
            }
        );
    }

    /**
     * Extracts the date when the current logged-in user approved this application
     * from the txtApprovalHistory JSON array.
     */
    extractMyApprovalDate(app: any, userId: number): string | null {
        if (!app.txtApprovalHistory) return null;
        try {
            const history: any[] = typeof app.txtApprovalHistory === 'string'
                ? JSON.parse(app.txtApprovalHistory)
                : app.txtApprovalHistory;
            if (!Array.isArray(history)) return null;
            // Find the last entry where this user approved
            const myEntries = history.filter(
                (h: any) => h.approvedBy === userId && h.action === 'APPROVED'
            );
            if (myEntries.length === 0) return null;
            const lastEntry = myEntries[myEntries.length - 1];
            return lastEntry ? lastEntry.approvedDate : null;
        } catch {
            return null;
        }
    }

    getFormName(app: any): string {
        if (app.cfgTblCustomForm && app.cfgTblCustomForm.txtFormName) {
            return app.cfgTblCustomForm.txtFormName;
        }
        const form = this.forms.find(f => f.serFormId === app.serFormId);
        return form ? form.txtFormName : 'N/A';
    }

    getDisplayedApplications(): Application[] {
        if (!this.search) return this.applications;
        const s = this.search.toLowerCase();
        return this.applications.filter(app =>
            (app.txtFormCode?.toLowerCase().includes(s)) ||
            (app.formName?.toLowerCase().includes(s)) ||
            (app.txtStatus?.toLowerCase().includes(s)) ||
            (app.txtAssetCode?.toLowerCase().includes(s)) ||
            (app.txtPrCode?.toLowerCase().includes(s))
        );
    }

    getStatusClass(status?: string): string {
        switch (status) {
            case 'APPROVED': return 'badge-outline-success';
            case 'REJECTED': return 'badge-outline-danger';
            case 'IN_PROGRESS': return 'badge-outline-warning';
            case 'PENDING': return 'badge-outline-info';
            default: return 'badge-outline-dark';
        }
    }

    viewApplication(app: Application) {
        this.router.navigate(['/application-details', app.serApplicationId]);
    }

    // Open the specialized viewer (Abc/XYZ) based on form type
    openViewer(app: Application) {
        const name = (app.formName || '').toUpperCase();
        const code = (app.txtFormCode || '').toUpperCase();

        if (name.includes('CAPF') || code.startsWith('CAPF')) {
            this.router.navigate(['/abc'], { state: { application: app } });
        } else if (name.includes('BUDGET') || code.startsWith('BDG')) {
            this.router.navigate(['/xyz'], { state: { application: app } });
        } else {
            this.viewApplication(app);
        }
    }
}
