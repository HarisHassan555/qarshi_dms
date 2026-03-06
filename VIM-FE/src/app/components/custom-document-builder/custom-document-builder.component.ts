import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { BudgetApprovalService } from 'src/app/services/budget-approval/budget-approval.service';

interface FooterField {
    label: string;
    users: any[];
}

@Component({
    selector: 'app-custom-document-builder',
    templateUrl: './custom-document-builder.component.html',
    styleUrls: ['./custom-document-builder.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class CustomDocumentBuilderComponent implements OnInit {
    content: SafeHtml = '';
    heading = '';
    dateText = '';
    footerFields: FooterField[] = [];

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
        const stateAppData = state?.applicationFormData || state?.appData || null;

        if (stateContent) {
            this.content = this.sanitizer.bypassSecurityTrustHtml(stateContent);
            this.heading = stateHeading || '';
            this.dateText = stateDate || '';
            this.footerFields = this.buildFooterFields(stateAppData || state);
            return;
        }

        this.budgetApprovalService.getLatest().subscribe({
            next: (data: any) => {
                if (data?.content) {
                    this.content = this.sanitizer.bypassSecurityTrustHtml(data.content);
                } else {
                    this.content = this.sanitizer.bypassSecurityTrustHtml('<p>No content submitted yet.</p>');
                }
                this.heading = data?.heading || this.heading;
                this.dateText = data?.date || this.dateText;

                let parsedData: any = null;
                try {
                    parsedData = data?.txtApplicationData ? JSON.parse(data.txtApplicationData) : null;
                } catch (e) {
                    parsedData = null;
                }
                this.footerFields = this.buildFooterFields(parsedData || data || {});
            },
            error: () => {
                this.content = this.sanitizer.bypassSecurityTrustHtml('<p>Error fetching content.</p>');
            }
        });
    }

    private buildFooterFields(source: any): FooterField[] {
        if (!source) return [];

        if (Array.isArray(source.footerFields) && source.footerFields.length > 0) {
            return source.footerFields.map((f: any) => ({
                label: f?.label || 'New Field',
                users: Array.isArray(f?.users) ? f.users : []
            }));
        }

        const fieldLabels = source.fieldLabels || {};

        const preparedUsers = Array.isArray(source.preparedByUsers)
            ? source.preparedByUsers
            : (source.preparedBy ? [source.preparedBy] : []);

        const reviewers = Array.isArray(source.reviewers) ? source.reviewers : [];
        const recommenders = Array.isArray(source.recommenders) ? source.recommenders : [];

        const approvers = Array.isArray(source.approvers)
            ? source.approvers
            : (source.approver ? [source.approver] : []);

        const defaults: FooterField[] = [
            { label: fieldLabels.preparedBy || 'Prepared By', users: preparedUsers },
            { label: fieldLabels.reviewedBy || 'Reviewed By (Multi)', users: reviewers },
            { label: fieldLabels.recommendedBy || 'Recommended By (Multi)', users: recommenders },
            { label: fieldLabels.approvedBy || 'Approved By (Multi)', users: approvers }
        ];

        const dynamic: FooterField[] = Array.isArray(source.dynamicUserFields)
            ? source.dynamicUserFields.map((f: any) => ({
                label: f?.label || 'New Field',
                users: Array.isArray(f?.selectedUsers) ? f.selectedUsers : []
            }))
            : [];

        return [...defaults, ...dynamic];
    }

    formatUser(user: any): string {
        if (!user) return '';
        const name = user.txtUserName || user.userName || user.name || '';
        const role = user.cfgTblRole?.txtRoleName || user.role || user.designation || '';
        return role ? `${name} (${role})` : name;
    }

    formatUsers(users: any[]): string {
        if (!Array.isArray(users) || users.length === 0) return '--';
        return users.map((u) => this.formatUser(u)).filter((v) => !!v).join(', ');
    }
}
