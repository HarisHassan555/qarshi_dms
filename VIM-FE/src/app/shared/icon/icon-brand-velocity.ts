import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'icon-brand-velocity',
    template: `
        <ng-template #template>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" [ngClass]="class">
                <rect width="24" height="24" rx="6" fill="#e11d48" />
                <path
                    fill="#fff"
                    d="M6.5 7.25h11a1.25 1.25 0 0 1 1.25 1.25v7a1.25 1.25 0 0 1-1.25 1.25H9.2l-2.45 2.2v-2.45H6.5a1.25 1.25 0 0 1-1.25-1.25v-7A1.25 1.25 0 0 1 6.5 7.25zm1.25 2.5v5h8.5v-5H7.75zm2.35 1.15.95 1.65 1.7.35-1.25 1.05.4 1.65-1.45-.95-1.45.95.4-1.65-1.25-1.05 1.7-.35.95-1.65z"
                />
            </svg>
        </ng-template>
    `,
})
export class IconBrandVelocityComponent {
    @Input() class: any = '';
    @ViewChild('template', { static: true }) template: any;
    constructor(private viewContainerRef: ViewContainerRef) {}
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        this.viewContainerRef.element.nativeElement.remove();
    }
}
