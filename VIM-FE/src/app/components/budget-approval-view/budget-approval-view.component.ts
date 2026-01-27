import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BudgetApprovalService } from 'src/app/services/budget-approval/budget-approval.service';

@Component({
    selector: 'app-budget-approval-view',
    templateUrl: './budget-approval-view.component.html',
})
export class BudgetApprovalViewComponent implements OnInit {
    content: SafeHtml = '';

    constructor(private sanitizer: DomSanitizer, private budgetApprovalService: BudgetApprovalService) { }

    ngOnInit() {
        this.budgetApprovalService.getLatest().subscribe({
            next: (data) => {
                if (data && data.content) {
                    this.content = this.sanitizer.bypassSecurityTrustHtml(data.content);
                } else {
                    this.content = this.sanitizer.bypassSecurityTrustHtml('<p>No content submitted yet.</p>');
                }
            },
            error: (err) => {
                console.error('Error fetching budget approval', err);
                this.content = this.sanitizer.bypassSecurityTrustHtml('<p>Error fetching content.</p>');
            }
        });
    }
}
