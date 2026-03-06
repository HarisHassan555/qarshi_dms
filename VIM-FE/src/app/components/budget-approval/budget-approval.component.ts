import { Component, ViewChild, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { BudgetApprovalService } from 'src/app/services/budget-approval/budget-approval.service';
import { CustomFormApplicationService } from 'src/app/services/custom-form-application/custom-form-application.service';
import { CustomFormService } from 'src/app/services/custom-form/custom-form.service';
import { UserService } from 'src/app/services/user/user.service';
import { NotificationService } from 'src/app/NotificationService';
import { ApplicationPdfService } from 'src/app/services/application-pdf/application-pdf.service';
import { QuillEditorComponent } from 'ngx-quill';
import * as QuillNamespace from 'quill';
const Quill: any = QuillNamespace;

// Register a custom blot for tables to prevent Quill from stripping them
const BlockEmbed = Quill.import('blots/block/embed');
class TableBlot extends BlockEmbed {
    static create(value: string) {
        let node = super.create();
        node.innerHTML = value;
        node.setAttribute('contenteditable', 'false');
        node.style.userSelect = 'all'; // Allow easy selection/deletion
        return node;
    }
    static value(node: HTMLElement) {
        return node.innerHTML;
    }
}
TableBlot['blotName'] = 'table-blot';
TableBlot['tagName'] = 'div';
TableBlot['className'] = 'q-table-wrapper';
Quill.register(TableBlot);
@Component({
    selector: 'app-budget-approval',
    templateUrl: './budget-approval.component.html',
    styleUrls: ['./budget-approval.component.css']
})
export class BudgetApprovalComponent implements OnInit {
    @Input() serFormId: number | undefined;
    @Input() formCode: string | null = null;
    @Input() editData: any = null;
    @Output() onReset = new EventEmitter<void>();

    isEditMode = false;
    serApplicationId: number | null = null;

    @ViewChild('editor', { static: false }) editor!: QuillEditorComponent;
    editorContent = '';
    formHeading = '';
    viewMode = false;
    savedContent: SafeHtml = '';

    // Signature State
    users: any[] = [];
    selectedPreparedBy: any[] = [];
    selectedReviewers: any[] = [];
    selectedRecommenders: any[] = [];
    selectedApprovers: any[] = [];
    currentDate: string = '';
    fieldLabels: any = {
        preparedBy: 'Prepared By',
        recommendedBy: 'Recommended By (Multi)',
        reviewedBy: 'Reviewed By (Multi)',
        approvedBy: 'Approved By (Multi)'
    };
    editingFieldLabel: any = {
        preparedBy: false,
        recommendedBy: false,
        reviewedBy: false,
        approvedBy: false
    };
    dynamicUserFields: Array<{ label: string; selectedUsers: any[]; isEditingLabel: boolean }> = [];

    quillModules = {
        toolbar: [
            ['bold', 'italic', 'underline', 'strike'],
            ['blockquote', 'code-block'],
            [{ 'header': 1 }, { 'header': 2 }],
            [{ 'list': 'ordered' }, { 'list': 'bullet' }],
            [{ 'script': 'sub' }, { 'script': 'super' }],
            [{ 'indent': '-1' }, { 'indent': '+1' }],
            [{ 'direction': 'rtl' }],
            [{ 'size': ['small', false, 'large', 'huge'] }],
            [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
            [{ 'color': [] }, { 'background': [] }],
            [{ 'font': [] }],
            [{ 'align': [] }],
            ['clean'],
        ],
    };

    tableRows: any[] = [];
    tableColumns: string[] = ['Column 1', 'Column 2'];

    constructor(
        private router: Router,
        private budgetApprovalService: BudgetApprovalService,
        private customFormApplicationService: CustomFormApplicationService,
        private customFormService: CustomFormService,
        private applicationPdfService: ApplicationPdfService,
        private userService: UserService,
        private notificationService: NotificationService,
        private sanitizer: DomSanitizer
    ) {
        this.addRow();
        this.currentDate = new Date().toLocaleDateString('en-GB', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        }).replace(/ /g, '-');
    }

    ngOnInit() {
        this.fetchUsers();
        if (this.editData) {
            this.loadEditData();
        } else {
            this.setCurrentUser();
        }
    }

    loadEditData() {
        this.isEditMode = true;
        this.serApplicationId = this.editData.serApplicationId;
        this.serFormId = this.editData.serFormId;
        this.formCode = this.editData.txtFormCode;

        if (this.editData.txtApplicationData) {
            try {
                const appData = JSON.parse(this.editData.txtApplicationData);
                this.editorContent = appData.content || '';
                this.formHeading = appData.heading || '';
                this.currentDate = appData.date || this.currentDate;
                const footerFields = Array.isArray(appData.footerFields) ? appData.footerFields : [];
                if (footerFields.length > 0) {
                    this.loadFromFooterFields(footerFields);
                }
                const preparedByUsers = Array.isArray(appData.preparedByUsers) ? appData.preparedByUsers : [];
                if (footerFields.length === 0 && preparedByUsers.length > 0) {
                    this.selectedPreparedBy = preparedByUsers;
                } else if (footerFields.length === 0 && Array.isArray(appData.preparedBy)) {
                    this.selectedPreparedBy = appData.preparedBy;
                } else if (footerFields.length === 0 && appData.preparedBy) {
                    this.selectedPreparedBy = [appData.preparedBy];
                } else if (footerFields.length === 0) {
                    this.selectedPreparedBy = [];
                }
                if (footerFields.length === 0) {
                    this.selectedReviewers = appData.reviewers || [];
                    this.selectedRecommenders = appData.recommenders || [];
                }
                const approvers = Array.isArray(appData.approvers) ? appData.approvers : [];
                if (footerFields.length === 0 && approvers.length > 0) {
                    this.selectedApprovers = approvers;
                } else if (footerFields.length === 0 && Array.isArray(appData.approver)) {
                    this.selectedApprovers = appData.approver;
                } else if (footerFields.length === 0 && appData.approver) {
                    this.selectedApprovers = [appData.approver];
                } else if (footerFields.length === 0) {
                    this.selectedApprovers = [];
                }
                this.fieldLabels = {
                    ...this.fieldLabels,
                    ...(appData.fieldLabels || {})
                };
                if (footerFields.length === 0) {
                    this.dynamicUserFields = Array.isArray(appData.dynamicUserFields)
                    ? appData.dynamicUserFields.map((f: any) => ({
                        label: f?.label || 'New Field',
                        selectedUsers: Array.isArray(f?.selectedUsers) ? f.selectedUsers : [],
                        isEditingLabel: false
                    }))
                    : [];
                }
            } catch (e) {
                console.error('Error parsing edit data:', e);
            }
        }
    }

    private loadFromFooterFields(footerFields: any[]) {
        this.dynamicUserFields = [];
        footerFields.forEach((field: any) => {
            const key = (field?.key || '').toString().toLowerCase();
            const label = field?.label || 'New Field';
            const users = Array.isArray(field?.users) ? field.users : [];
            if (key === 'prepared_by') {
                this.fieldLabels.preparedBy = label;
                this.selectedPreparedBy = users;
            } else if (key === 'recommended_by') {
                this.fieldLabels.recommendedBy = label;
                this.selectedRecommenders = users;
            } else if (key === 'reviewed_by') {
                this.fieldLabels.reviewedBy = label;
                this.selectedReviewers = users;
            } else if (key === 'approved_by') {
                this.fieldLabels.approvedBy = label;
                this.selectedApprovers = users;
            } else {
                this.dynamicUserFields.push({
                    label,
                    selectedUsers: users,
                    isEditingLabel: false
                });
            }
        });
    }

    private buildFooterFieldsPayload(): any[] {
        const footerFields: any[] = [
            {
                key: 'prepared_by',
                label: this.fieldLabels.preparedBy || 'Prepared By',
                order: 1,
                users: this.selectedPreparedBy || []
            },
            {
                key: 'recommended_by',
                label: this.fieldLabels.recommendedBy || 'Recommended By (Multi)',
                order: 2,
                users: this.selectedRecommenders || []
            },
            {
                key: 'reviewed_by',
                label: this.fieldLabels.reviewedBy || 'Reviewed By (Multi)',
                order: 3,
                users: this.selectedReviewers || []
            },
            {
                key: 'approved_by',
                label: this.fieldLabels.approvedBy || 'Approved By (Multi)',
                order: 4,
                users: this.selectedApprovers || []
            }
        ];

        this.dynamicUserFields.forEach((f: any, index: number) => {
            footerFields.push({
                key: `wf_${index + 1}`,
                label: f?.label || 'New Field',
                order: 5 + index,
                users: Array.isArray(f?.selectedUsers) ? f.selectedUsers : []
            });
        });

        return footerFields;
    }

    fetchUsers() {
        this.userService.getUsers().subscribe({
            next: (data: any) => {
                this.users = data || [];
            },
            error: (err) => {
                console.error('Error fetching users', err);
            }
        });
    }

    setCurrentUser() {
        const userJson = localStorage.getItem('user');
        if (userJson) {
            try {
                const user = JSON.parse(userJson);
                this.selectedPreparedBy = user ? [user] : [];
            } catch (e) {
                console.error('Error parsing user data:', e);
            }
        }
    }

    toggleFieldLabelEdit(fieldKey: string) {
        this.editingFieldLabel[fieldKey] = !this.editingFieldLabel[fieldKey];
    }

    addDynamicUserField() {
        this.dynamicUserFields.push({
            label: 'New Field',
            selectedUsers: [],
            isEditingLabel: false
        });
    }

    removeDynamicUserField(index: number) {
        this.dynamicUserFields.splice(index, 1);
    }

    toggleDynamicFieldLabelEdit(index: number) {
        if (!this.dynamicUserFields[index]) return;
        this.dynamicUserFields[index].isEditingLabel = !this.dynamicUserFields[index].isEditingLabel;
    }

    getPreparedByPrimary(): any {
        return Array.isArray(this.selectedPreparedBy) && this.selectedPreparedBy.length > 0
            ? this.selectedPreparedBy[0]
            : null;
    }

    // Backward compatibility for existing consumers expecting a single preparedBy object.
    get preparedBy(): any {
        return this.getPreparedByPrimary();
    }

    get selectedApprover(): any {
        return Array.isArray(this.selectedApprovers) && this.selectedApprovers.length > 0
            ? this.selectedApprovers[0]
            : null;
    }

    set selectedApprover(value: any) {
        if (!value) {
            this.selectedApprovers = [];
            return;
        }
        this.selectedApprovers = Array.isArray(value) ? value : [value];
    }

    getUserDisplayName(user: any): string {
        if (!user) return '';
        return user.txtUserName || user.userName || user.name || '';
    }

    getDynamicFieldUsersDisplay(users: any[]): string {
        if (!Array.isArray(users) || users.length === 0) return '--';
        return users.map((u: any) => this.getUserDisplayName(u)).filter((n: string) => !!n).join(', ');
    }

    addColumn() {
        this.tableColumns.push(`Column ${this.tableColumns.length + 1}`);
        this.tableRows.forEach(row => row.push(''));
    }

    removeColumn(index: number) {
        if (this.tableColumns.length > 1) {
            this.tableColumns.splice(index, 1);
            this.tableRows.forEach(row => row.splice(index, 1));
        }
    }

    addRow() {
        const newRow = new Array(this.tableColumns.length).fill('');
        this.tableRows.push(newRow);
    }

    removeRow(index: number) {
        if (this.tableRows.length > 1) {
            this.tableRows.splice(index, 1);
        }
    }

    generateTableHtml(): string {
        // Use standard table tags (they will be preserved inside the custom blot)
        let html = '<table style="width: 100%; border-collapse: collapse; border: 1px solid #000; margin: 10px 0;">';

        // Header Row
        html += '<tr style="background-color: #f1f1f1; font-weight: bold;">';
        this.tableColumns.forEach(col => {
            html += `<td style="border: 1px solid #000; padding: 8px; text-align: center;">${col}</td>`;
        });
        html += '</tr>';

        // Body Rows
        this.tableRows.forEach(row => {
            html += '<tr>';
            row.forEach((cell: string) => {
                html += `<td style="border: 1px solid #000; padding: 8px; text-align: left;">${cell || '&nbsp;'}</td>`;
            });
            html += '</tr>';
        });
        html += '</table>';

        return html;
    }

    insertTable() {
        const hasData = this.tableRows.length > 0;
        if (hasData && this.editor && this.editor.quillEditor) {
            const tableHtml = this.generateTableHtml();
            const quill = this.editor.quillEditor;

            let range = quill.getSelection(true);
            let index = range ? range.index : quill.getLength();

            // Insert using our custom blot
            quill.insertEmbed(index, 'table-blot', tableHtml, 'user');

            // Move cursor after the table
            quill.setSelection({ index: index + 1, length: 0 }, 'api');

            // Sync the model immediately
            this.editorContent = quill.root.innerHTML;

            // Reset table builder after insertion
            this.tableColumns = ['Column 1', 'Column 2'];
            this.tableRows = [];
            this.addRow();
        }
    }

    async save() {
        await this.ensureFormId();
        if (!this.serFormId) {
            this.notificationService.showMessage('Form is not mapped. Please select a valid form before saving.', 'danger');
            return;
        }

        // ALWAYS use the live HTML from the quill root to ensure table content is included
        const quillHtml = this.editor?.quillEditor?.root?.innerHTML ?? '';
        const content = quillHtml || this.editorContent;

        // Get current user from localStorage
        const userJson = localStorage.getItem('user');
        let userId: number | null = null;
        if (userJson) {
            try {
                const user = JSON.parse(userJson);
                userId = user.serUserId || null;
            } catch (e) {
                console.error('Error parsing user data:', e);
            }
        }

        // Prepare payload for application submission/update
        const applicationFormData = {
            content: content,
            heading: this.formHeading,
            date: this.currentDate,
            preparedBy: this.getPreparedByPrimary(),
            preparedByUsers: this.selectedPreparedBy,
            reviewers: this.selectedReviewers,
            recommenders: this.selectedRecommenders,
            approver: this.selectedApprover,
            approvers: this.selectedApprovers,
            fieldLabels: this.fieldLabels,
            dynamicUserFields: this.dynamicUserFields.map((f: any) => ({
                label: f.label,
                selectedUsers: f.selectedUsers || []
            })),
            footerFields: this.buildFooterFieldsPayload()
        };

        const payload: any = {
            serApplicationId: this.serApplicationId,
            serFormId: this.serFormId,
            txtFormCode: this.formCode || null,
            txtApplicationData: JSON.stringify({
                content: content,
                heading: this.formHeading,
                date: this.currentDate,
                preparedBy: this.getPreparedByPrimary(),
                preparedByUsers: this.selectedPreparedBy,
                reviewers: this.selectedReviewers,
                recommenders: this.selectedRecommenders,
                approver: this.selectedApprover,
                approvers: this.selectedApprovers,
                fieldLabels: this.fieldLabels,
                dynamicUserFields: this.dynamicUserFields.map((f: any) => ({
                    label: f.label,
                    selectedUsers: f.selectedUsers || []
                })),
                footerFields: this.buildFooterFieldsPayload()
            }),
            txtStatus: this.editData?.txtStatus || 'PENDING',
            intCurrentApprovalLevel: this.editData?.intCurrentApprovalLevel || 0,
            serSubmittedBy: userId || this.editData?.serSubmittedBy,
            blIsActive: true,
            blIsDeleted: false,
            blnStatus: true
        };

        if (!this.isEditMode) {
            payload.deferEmail = true;
        }

        const request = this.isEditMode
            ? this.customFormApplicationService.updateApplication(payload)
            : this.customFormApplicationService.submitApplication(payload);

        try {
            const response: any = await firstValueFrom(request);
            if (response && response.status === 'Success') {
                let showSuccessToast = true;
                if (!this.isEditMode) {
                    const applicationId = Number(response.applicationId);
                    if (!applicationId) {
                        this.notificationService.showMessage('Application submitted but ID was not returned.', 'warning');
                        showSuccessToast = false;
                    } else {
                        try {
                            await this.uploadBudgetPdfAndSendEmails(applicationId, applicationFormData);
                        } catch (emailError: any) {
                            console.error('Failed to upload budget PDF or send emails:', emailError);
                            this.notificationService.showMessage('Application submitted, but approval email could not be sent.', 'warning');
                            showSuccessToast = false;
                        }
                    }
                }

                if (showSuccessToast) {
                    this.notificationService.showMessage(
                        this.isEditMode ? 'Application updated successfully!' : 'Application submitted successfully!',
                        'success'
                    );
                }
                this.savedContent = this.sanitizer.bypassSecurityTrustHtml(content);
                this.viewMode = true;
            } else {
                this.notificationService.showMessage(response?.message || 'Failed to save application', 'danger');
            }
        } catch (err: any) {
            this.notificationService.showMessage('Error saving application', 'danger');
            console.error('Error saving budget approval', err);
        }
    }

    private async ensureFormId(): Promise<void> {
        if (this.serFormId) return;
        try {
            const forms: any = await firstValueFrom(this.customFormService.getAll());
            const list = Array.isArray(forms) ? forms : [];
            if (!list.length) return;

            const norm = (v: any) => String(v || '').trim().toLowerCase();
            const byName = (needle: string) =>
                list.find((f: any) => norm(f?.txtFormName || f?.name).includes(needle));

            const matched =
                byName('document builder') ||
                byName('budget approval') ||
                list[0];

            this.serFormId = matched?.serFormId || this.serFormId;
            if (this.serFormId && !this.formCode) {
                this.customFormApplicationService.getNextApplicationCode(this.serFormId).subscribe({
                    next: (resp: any) => {
                        if (resp?.status === 'Success' && resp?.code) {
                            this.formCode = resp.code;
                        }
                    }
                });
            }
        } catch (e) {
            console.error('Failed to resolve serFormId', e);
        }
    }

    private async uploadBudgetPdfAndSendEmails(applicationId: number, applicationFormData: any): Promise<void> {
        const applicationResponse: any = await firstValueFrom(
            this.customFormApplicationService.getApplicationById(applicationId)
        );
        const application = applicationResponse || {};
        const formName = 'Budget Approval Form';
        const formCode = (application?.txtFormCode || this.formCode || '').trim();

        const htmlContent = this.applicationPdfService.buildPdfHtmlForApplication(
            application,
            null,
            [],
            applicationFormData,
            { formName, txtFormCode: formCode }
        );
        if (!htmlContent) {
            throw new Error('PDF HTML generation failed');
        }

        const filename = `application_${formCode || applicationId}.pdf`;
        const pdfBlob = await this.applicationPdfService.renderHtmlToPdfBlob(htmlContent, filename);
        const pdfResponse: any = await firstValueFrom(
            this.customFormApplicationService.updateApplicationPdf(applicationId, pdfBlob, filename)
        );
        if (!pdfResponse || pdfResponse.status !== 'Success') {
            throw new Error(pdfResponse?.message || 'Failed to update application PDF');
        }

        const emailResponse: any = await firstValueFrom(
            this.customFormApplicationService.sendSubmissionEmails(applicationId)
        );
        if (!emailResponse || emailResponse.status !== 'Success') {
            throw new Error(emailResponse?.message || 'Failed to send submission emails');
        }
    }

    formatUserForSignature(selectedUsers: any[], index: number): string {
        if (!selectedUsers || !selectedUsers[index]) return '';
        const user = selectedUsers[index];
        const role = user.cfgTblRole?.txtRoleName || 'Reviewer';
        return `${user.txtUserName}<br>(${role})`;
    }

    reset() {
        this.viewMode = false;
        this.editorContent = '';
        this.formHeading = '';
        this.savedContent = '';
        this.dynamicUserFields = [];
        this.fieldLabels = {
            preparedBy: 'Prepared By',
            recommendedBy: 'Recommended By (Multi)',
            reviewedBy: 'Reviewed By (Multi)',
            approvedBy: 'Approved By (Multi)'
        };
        this.editingFieldLabel = {
            preparedBy: false,
            recommendedBy: false,
            reviewedBy: false,
            approvedBy: false
        };
        this.onReset.emit();
    }

}
