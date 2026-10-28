import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'icon-brand-copy-link',
    template: `
        <ng-template #template>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" [ngClass]="class">
                <rect width="24" height="24" rx="5" fill="#334155" />
                <path
                    fill="#fff"
                    d="M9 8.25h6.75A1.25 1.25 0 0 1 17 9.5v6.75A1.25 1.25 0 0 1 15.75 17.5H9A1.25 1.25 0 0 1 7.75 16.25V9.5A1.25 1.25 0 0 1 9 8.25zm0 1.5v6.5h6.75v-6.5H9zM6.25 11H5A1.25 1.25 0 0 0 3.75 12.25v6.5A1.25 1.25 0 0 0 5 20h6.25a1.25 1.25 0 0 0 1.25-1.25V18h-1.5v.75H5v-6.5h.75V11h.5z"
                />
            </svg>
        </ng-template>
    `,
})
export class IconBrandCopyLinkComponent {
    @Input() class: any = '';
    @ViewChild('template', { static: true }) template: any;
    constructor(private viewContainerRef: ViewContainerRef) {}
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        this.viewContainerRef.element.nativeElement.remove();
    }
}
