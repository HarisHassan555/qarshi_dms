import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SavedTemplateDefinition, TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';

@Component({
    selector: 'app-template-forms',
    templateUrl: './template-forms.component.html',
    styleUrls: ['./template-forms.component.css']
})
export class TemplateFormsComponent implements OnInit {
    templates: SavedTemplateDefinition[] = [];
    loadError = '';

    constructor(
        private router: Router,
        private templateWorkflowService: TemplateWorkflowService
    ) { }

    ngOnInit(): void {
        this.templateWorkflowService.getTemplates().subscribe({
            next: (templates) => {
                this.templates = (templates || []).filter((template: any) => template && template.id);
                this.loadError = '';
            },
            error: () => {
                this.templates = [];
                this.loadError = 'Saved template list could not be loaded.';
            }
        });
    }

    fillTemplate(template: SavedTemplateDefinition): void {
        this.router.navigate(['/template-fill', template.id]);
    }

    editTemplate(template: SavedTemplateDefinition): void {
        this.router.navigate(['/template-builder'], { queryParams: { id: template.id } });
    }

    getTemplateName(template: SavedTemplateDefinition): string {
        return template?.name || template?.payload?.name || 'Untitled Template';
    }

    getCodeConvention(template: SavedTemplateDefinition): string {
        const convention: any = template?.codeConvention || template?.payload?.codeConvention;
        if (typeof convention === 'string') {
            return convention;
        }
        return convention?.pattern || template?.payload?.codeConvention || 'TPL-0000';
    }
}
