import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'icon-brand-teams',
    template: `
        <ng-template #template>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" [ngClass]="class">
                <path fill="#5059C9" d="M20.625 8.25h-5.25a3.375 3.375 0 00-3.375 3.375v5.25H20.25a.375.375 0 00.375-.375V8.625a.375.375 0 00-.375-.375z" />
                <circle cx="15.75" cy="6.375" r="2.625" fill="#7B83EB" />
                <path
                    fill="#7B83EB"
                    d="M11.25 4.5H4.125A1.125 1.125 0 003 5.625v11.25A1.125 1.125 0 004.125 18h7.125A1.125 1.125 0 0012.375 16.875V5.625A1.125 1.125 0 0011.25 4.5z"
                />
                <path fill="#fff" d="M7.2 9.1h1.35v5.4H7.2V9.1zm2.7 0H9.9c0-1.49-1.21-2.7-2.7-2.7S4.5 7.61 4.5 9.1c0 1.49 1.21 2.7 2.7 2.7.75 0 1.43-.3 1.93-.79l.95.95A4.46 4.46 0 017.2 13.5c-2.48 0-4.5-2.02-4.5-4.5S4.72 4.5 7.2 4.5 11.7 6.52 11.7 9.1h-1.8z" />
            </svg>
        </ng-template>
    `,
})
export class IconBrandTeamsComponent {
    @Input() class: any = '';
    @ViewChild('template', { static: true }) template: any;
    constructor(private viewContainerRef: ViewContainerRef) {}
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        this.viewContainerRef.element.nativeElement.remove();
    }
}
