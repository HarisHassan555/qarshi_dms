import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'icon-brand-more',
    template: `
        <ng-template #template>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" [ngClass]="class">
                <rect width="24" height="24" rx="5" fill="#475569" />
                <circle cx="7" cy="12" r="1.5" fill="#fff" />
                <circle cx="12" cy="12" r="1.5" fill="#fff" />
                <circle cx="17" cy="12" r="1.5" fill="#fff" />
            </svg>
        </ng-template>
    `,
})
export class IconBrandMoreComponent {
    @Input() class: any = '';
    @ViewChild('template', { static: true }) template: any;
    constructor(private viewContainerRef: ViewContainerRef) {}
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        this.viewContainerRef.element.nativeElement.remove();
    }
}
