import { Type, ViewContainerRef } from '@angular/core';
import { ViewRef } from '../ngx-tippy.interfaces';
interface Args<C> {
    component: Type<C>;
    viewContainerRef: ViewContainerRef;
}
export declare class CompRef<T> implements ViewRef {
    private args;
    private compRef;
    constructor(args: Args<T>);
    detectChanges(): this;
    getElement<T extends Element>(): T;
    destroy(): void;
}
export {};
