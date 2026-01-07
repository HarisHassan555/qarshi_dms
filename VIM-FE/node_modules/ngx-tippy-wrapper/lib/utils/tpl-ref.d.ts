import { ApplicationRef, TemplateRef } from '@angular/core';
import { ViewRef } from '../ngx-tippy.interfaces';
interface CustomTmlRefArgs<C> {
    tpl: TemplateRef<C>;
    context: C;
    appRef: ApplicationRef;
}
export declare class TplRef<C> implements ViewRef {
    private args;
    private viewRef;
    private element;
    constructor(args: CustomTmlRefArgs<C>);
    detectChanges(): this;
    getElement(): Element | null;
    destroy(): void;
}
export {};
