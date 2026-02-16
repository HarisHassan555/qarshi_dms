import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { BudgetApprovalService } from 'src/app/services/budget-approval/budget-approval.service';

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

    // Signature properties
    preparedBy: any = null;
    reviewers: any[] = [];
    recommenders: any[] = [];
    approver: any = null;

    constructor(
        private router: Router,
        private sanitizer: DomSanitizer,
        private budgetApprovalService: BudgetApprovalService
    ) { }

    ngOnInit(): void {
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
}
