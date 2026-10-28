import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'icon-brand-email',
    template: `
        <ng-template #template>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" [ngClass]="class">
                <rect width="24" height="24" rx="5" fill="#64748b" />
                <path fill="#fff" d="M5 8.5h14v7H5v-7zm0-1.5A1.5 1.5 0 0 1 6.5 5.5h11A1.5 1.5 0 0 1 19 7v10a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 17V7a1.5 1.5 0 0 1 1.5-1.5z" />
                <path fill="#cbd5e1" d="M6.2 8.1 12 12.2l5.8-4.1H6.2z" />
            </svg>
        </ng-template>
    `,
})
export class IconBrandEmailComponent {
    @Input() class: any = '';
    @ViewChild('template', { static: true }) template: any;
    constructor(private viewContainerRef: ViewContainerRef) {}
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        this.viewContainerRef.element.nativeElement.remove();
    }
}
