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

    constructor(
        private router: Router,
        private sanitizer: DomSanitizer,
        private budgetApprovalService: BudgetApprovalService
    ) {}

    ngOnInit(): void {
        const navigation = this.router.getCurrentNavigation();
        const stateContent = navigation?.extras?.state?.['content'] ?? history.state?.content;
        const stateHeading = navigation?.extras?.state?.['heading'] ?? history.state?.heading;
        const stateDate = navigation?.extras?.state?.['date'] ?? history.state?.date;

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
                } else {
                    this.content = this.sanitizer.bypassSecurityTrustHtml('<p>No content submitted yet.</p>');
                }
            },
            error: () => {
                this.content = this.sanitizer.bypassSecurityTrustHtml('<p>Error fetching content.</p>');
            }
        });
    }
}
