import { Component, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { BudgetApprovalService } from 'src/app/services/budget-approval/budget-approval.service';
import { QuillEditorComponent } from 'ngx-quill';

@Component({
    selector: 'app-budget-approval',
    templateUrl: './budget-approval.component.html',
})
export class BudgetApprovalComponent {
    @ViewChild('editor', { static: false }) editor!: QuillEditorComponent;
    editorContent = '';

    quillModules = {
        toolbar: [
            ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
            ['blockquote', 'code-block'],
            [{ 'header': 1 }, { 'header': 2 }],               // custom button values
            [{ 'list': 'ordered' }, { 'list': 'bullet' }],
            [{ 'script': 'sub' }, { 'script': 'super' }],      // superscript/subscript
            [{ 'indent': '-1' }, { 'indent': '+1' }],          // outdent/indent
            [{ 'direction': 'rtl' }],                         // text direction
            [{ 'size': ['small', false, 'large', 'huge'] }],  // custom dropdown
            [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
            [{ 'color': [] }, { 'background': [] }],          // dropdown with defaults from theme
            [{ 'font': [] }],
            [{ 'align': [] }],
            ['clean'],                                         // remove formatting
            ['link', 'image', 'video']                         // link and image, video
        ],
    };

    tableRows: any[] = [];
    tableColumns: string[] = ['Column 1', 'Column 2'];

    constructor(private router: Router, private budgetApprovalService: BudgetApprovalService) {
        this.addRow();
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
        let html = '<table style="width: 100%; border-collapse: collapse; border: 1px solid #ccc; margin: 10px 0;">';

        // Header
        html += '<thead><tr style="background-color: #f1f1f1;">';
        this.tableColumns.forEach(col => {
            html += `<th style="border: 1px solid #ccc; padding: 8px; font-weight: bold; text-align: left;">${col}</th>`;
        });
        html += '</tr></thead>';

        // Body
        html += '<tbody>';
        this.tableRows.forEach(row => {
            html += '<tr>';
            row.forEach((cell: string) => {
                html += `<td style="border: 1px solid #ccc; padding: 8px;">${cell || '&nbsp;'}</td>`;
            });
            html += '</tr>';
        });
        html += '</tbody></table><p><br></p>'; // Add paragraphs for spacing after table

        return html;
    }

    insertTable() {
        const hasData = this.tableRows.length > 0;
        if (hasData && this.editor && this.editor.quillEditor) {
            const tableHtml = this.generateTableHtml();
            const quill = this.editor.quillEditor;

            let range = quill.getSelection(true);
            let index = range ? range.index : quill.getLength();

            quill.clipboard.dangerouslyPasteHTML(index, tableHtml);

            // Reset table builder after insertion
            this.tableColumns = ['Column 1', 'Column 2'];
            this.tableRows = [];
            this.addRow();
        }
    }

    save() {
        const quillHtml = this.editor?.quillEditor?.root?.innerHTML ?? '';
        const content = (this.editorContent && this.editorContent.trim()) ? this.editorContent : quillHtml;

        this.budgetApprovalService.save(content).subscribe({
            next: () => {
                this.router.navigate(['/xyz'], { state: { content } });
            },
            error: (err) => {
                console.error('Error saving budget approval', err);
                // You might want to show an alert here
            }
        });
    }
}
