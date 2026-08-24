import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { BudgetApprovalService } from 'src/app/services/budget-approval/budget-approval.service';
import {
    DEFAULT_DOCUMENT_HEADER_BRAND_TITLE,
    DEFAULT_DOCUMENT_HEADER_LOGO,
    DOCUMENT_HEADER_ADDRESS,
    QF_DOCUMENT_HEADER_BRAND_TITLE,
    QF_DOCUMENT_HEADER_LOGO,
    QB_DOCUMENT_HEADER_BRAND_TITLE,
    QRI_DOCUMENT_HEADER_ADDRESS,
    QRI_DOCUMENT_HEADER_BRAND_TITLE,
    QRI_DOCUMENT_HEADER_LOGO,
    QU_DOCUMENT_HEADER_BRAND_TITLE,
    QU_DOCUMENT_HEADER_LOGO,
} from 'src/app/utils/document-header.util';

@Component({
    selector: 'app-xyz',
    templateUrl: './xyz.component.html',
    styleUrls: ['./xyz.component.css'],
    encapsulation: ViewEncapsulation.None,
})
export class XyzComponent implements OnInit {
    content: SafeHtml = '';
    heading = '';
    dateText = '';
    logoPath = DEFAULT_DOCUMENT_HEADER_LOGO;
    companyName = DEFAULT_DOCUMENT_HEADER_BRAND_TITLE;
    companyAddress = DOCUMENT_HEADER_ADDRESS;
    approverTitle = 'Chief Executive';

    // Signature properties
    preparedBy: any = null;
    reviewers: any[] = [];
    recommenders: any[] = [];
    approver: any = null;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private sanitizer: DomSanitizer,
        private budgetApprovalService: BudgetApprovalService
    ) { }

    ngOnInit(): void {
        this.applyBrandingForRoute();

        const navigation = this.router.getCurrentNavigation();
        const state = navigation?.extras?.state || history.state;

        const stateContent = state?.content;
        const stateHeading = state?.heading;
        const stateDate = state?.date;

        // Extract signatures
        this.preparedBy = state?.preparedBy;
        this.reviewers = state?.reviewers || [];
        this.recommenders = state?.recommenders || [];
        this.approver = state?.approver;

        if (stateContent) {
            this.content = this.sanitizer.bypassSecurityTrustHtml(stateContent);
            this.heading = stateHeading || this.heading;
            this.dateText = stateDate || this.dateText;
            return;
        }

        this.budgetApprovalService.getLatest().subscribe({
            next: (data) => {
                if (data && data.content) {
                    this.content = this.sanitizer.bypassSecurityTrustHtml(data.content);

                    // Try to parse application data from latest if possible (fallback)
                    try {
                        const appData = JSON.parse(data.txtApplicationData);
                        this.preparedBy = appData.preparedBy;
                        this.reviewers = appData.reviewers || [];
                        this.recommenders = appData.recommenders || [];
                        this.approver = appData.approver;
                    } catch (e) { }
                } else {
                    this.content = this.sanitizer.bypassSecurityTrustHtml('<p>No content submitted yet.</p>');
                }
            },
            error: () => {
                this.content = this.sanitizer.bypassSecurityTrustHtml('<p>Error fetching content.</p>');
            }
        });
    }

    formatUserForSignature(selectedUsers: any[], index: number): string {
        if (!selectedUsers || !selectedUsers[index]) return '';
        const user = selectedUsers[index];
        const role = user.cfgTblRole?.txtRoleName || 'Reviewer';
        return `${user.txtUserName}<br>(${role})`;
    }

    getApproverRoleLabel(): string {
        const role = (this.approver?.cfgTblRole?.txtRoleName || '').trim();
        if (!role) {
            return this.approverTitle;
        }
        if (this.approverTitle === 'Vice Chancellor' && role.toLowerCase() === 'chief executive') {
            return this.approverTitle;
        }
        return role;
    }

    private applyBrandingForRoute(): void {
        const routePath = (this.route.snapshot.routeConfig?.path || '').toLowerCase();

        if (routePath === 'xyzqu') {
            this.logoPath = QU_DOCUMENT_HEADER_LOGO;
            this.companyName = QU_DOCUMENT_HEADER_BRAND_TITLE;
            this.companyAddress = DOCUMENT_HEADER_ADDRESS;
            this.approverTitle = 'Vice Chancellor';
            return;
        }

        if (routePath === 'xyzqf') {
            this.logoPath = QF_DOCUMENT_HEADER_LOGO;
            this.companyName = QF_DOCUMENT_HEADER_BRAND_TITLE;
            this.companyAddress = DOCUMENT_HEADER_ADDRESS;
            return;
        }

        if (routePath === 'xyzqri') {
            this.logoPath = QRI_DOCUMENT_HEADER_LOGO;
            this.companyName = QRI_DOCUMENT_HEADER_BRAND_TITLE;
            this.companyAddress = QRI_DOCUMENT_HEADER_ADDRESS;
            return;
        }

        if (routePath === 'xyzqb') {
            this.logoPath = DEFAULT_DOCUMENT_HEADER_LOGO;
            this.companyName = QB_DOCUMENT_HEADER_BRAND_TITLE;
            this.companyAddress = DOCUMENT_HEADER_ADDRESS;
            return;
        }
    }
}
