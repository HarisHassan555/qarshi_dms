import { Component, Input, ViewChild, ViewContainerRef } from '@angular/core';

@Component({
    moduleId: module.id,
    selector: 'icon-brand-outlook',
    template: `
        <ng-template #template>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" role="img" [ngClass]="class">
                <path fill="#0078D4" d="M24 7.387v10.478c0 .23-.08.424-.238.576-.158.152-.352.228-.578.228h-8.096v-6.09l1.87 1.344c.114.078.246.117.378.117.132 0 .264-.039.378-.117l6.286-4.536z" />
                <path
                    fill="#28A8EA"
                    d="M24 6.807v-.588l-7.814 5.538-7.814-5.538V6.19c0-.23.08-.424.238-.576.158-.152.352-.228.578-.228h14.192c.226 0 .42.076.578.228.158.152.238.346.238.576z"
                />
                <path
                    fill="#0078D4"
                    d="M0 6.19v11.622c0 .228.084.422.252.576.168.154.372.23.612.23H13.27c.24 0 .444-.076.612-.23.168-.154.252-.348.252-.576V6.19c0-.228-.084-.422-.252-.576C13.714 5.46 13.51 5.384 13.27 5.384H.864C.624 5.384.42 5.46.252 5.614.084 5.768 0 5.962 0 6.19z"
                />
                <path fill="#50D9FF" d="M3.5 8.5h6v7h-6v-7z" />
                <path fill="#fff" d="M5.2 10.1 8.1 12l-2.9 1.9V10.1z" />
            </svg>
        </ng-template>
    `,
})
export class IconBrandOutlookComponent {
    @Input() class: any = '';
    @ViewChild('template', { static: true }) template: any;
    constructor(private viewContainerRef: ViewContainerRef) {}
    ngOnInit() {
        this.viewContainerRef.createEmbeddedView(this.template);
        this.viewContainerRef.element.nativeElement.remove();
    }
}
