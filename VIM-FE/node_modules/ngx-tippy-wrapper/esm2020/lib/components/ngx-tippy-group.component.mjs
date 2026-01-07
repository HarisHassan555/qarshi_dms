import { isPlatformServer } from '@angular/common';
import { Component, ElementRef, Inject, Input, PLATFORM_ID, ViewChild } from '@angular/core';
import tippy from 'tippy.js';
import { NGX_TIPPY_MESSAGES } from '../ngx-tippy.tokens';
import * as i0 from "@angular/core";
/**
 * Different tooltip content to many different elements, but only one tippy instance
 */
export class NgxTippyGroupComponent {
    constructor(platform, messagesDict) {
        this.platform = platform;
        this.messagesDict = messagesDict;
    }
    ngAfterViewInit() {
        if (isPlatformServer(this.platform))
            return;
        this.setTooltips();
    }
    setTooltips() {
        const contentWrapperNativeEl = this.contentWrapper.nativeElement;
        const tooltips = Array.from(contentWrapperNativeEl.querySelectorAll('[data-tippy-grouped]'));
        if (tooltips.length) {
            this.initTippy(tooltips);
        }
        else {
            throw new Error(this.messagesDict.childrenInstancesNotFoundGrouped);
        }
    }
    initTippy(tooltips) {
        tippy(tooltips, this.groupedProps);
    }
}
NgxTippyGroupComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippyGroupComponent, deps: [{ token: PLATFORM_ID }, { token: NGX_TIPPY_MESSAGES }], target: i0.ɵɵFactoryTarget.Component });
NgxTippyGroupComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "14.0.0", version: "14.2.12", type: NgxTippyGroupComponent, selector: "ngx-tippy-group", inputs: { groupedProps: "groupedProps" }, viewQueries: [{ propertyName: "contentWrapper", first: true, predicate: ["contentWrapper"], descendants: true, read: ElementRef }], ngImport: i0, template: `
    <div #contentWrapper>
      <ng-content></ng-content>
    </div>
  `, isInline: true });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippyGroupComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'ngx-tippy-group',
                    template: `
    <div #contentWrapper>
      <ng-content></ng-content>
    </div>
  `,
                }]
        }], ctorParameters: function () { return [{ type: Object, decorators: [{
                    type: Inject,
                    args: [PLATFORM_ID]
                }] }, { type: undefined, decorators: [{
                    type: Inject,
                    args: [NGX_TIPPY_MESSAGES]
                }] }]; }, propDecorators: { groupedProps: [{
                type: Input
            }], contentWrapper: [{
                type: ViewChild,
                args: ['contentWrapper', { read: ElementRef, static: false }]
            }] } });
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibmd4LXRpcHB5LWdyb3VwLmNvbXBvbmVudC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbIi4uLy4uLy4uLy4uLy4uL3Byb2plY3RzL25neC10aXBweS13cmFwcGVyL3NyYy9saWIvY29tcG9uZW50cy9uZ3gtdGlwcHktZ3JvdXAuY29tcG9uZW50LnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBLE9BQU8sRUFBRSxnQkFBZ0IsRUFBRSxNQUFNLGlCQUFpQixDQUFDO0FBQ25ELE9BQU8sRUFBaUIsU0FBUyxFQUFFLFVBQVUsRUFBRSxNQUFNLEVBQUUsS0FBSyxFQUFFLFdBQVcsRUFBRSxTQUFTLEVBQUUsTUFBTSxlQUFlLENBQUM7QUFDNUcsT0FBTyxLQUFLLE1BQU0sVUFBVSxDQUFDO0FBRTdCLE9BQU8sRUFBRSxrQkFBa0IsRUFBRSxNQUFNLHFCQUFxQixDQUFDOztBQUV6RDs7R0FFRztBQVNILE1BQU0sT0FBTyxzQkFBc0I7SUFJakMsWUFDK0IsUUFBZ0IsRUFDVCxZQUFrQztRQUR6QyxhQUFRLEdBQVIsUUFBUSxDQUFRO1FBQ1QsaUJBQVksR0FBWixZQUFZLENBQXNCO0lBQ3JFLENBQUM7SUFFSixlQUFlO1FBQ2IsSUFBSSxnQkFBZ0IsQ0FBQyxJQUFJLENBQUMsUUFBUSxDQUFDO1lBQUUsT0FBTztRQUM1QyxJQUFJLENBQUMsV0FBVyxFQUFFLENBQUM7SUFDckIsQ0FBQztJQUVELFdBQVc7UUFDVCxNQUFNLHNCQUFzQixHQUFnQixJQUFJLENBQUMsY0FBYyxDQUFDLGFBQWEsQ0FBQztRQUM5RSxNQUFNLFFBQVEsR0FBa0IsS0FBSyxDQUFDLElBQUksQ0FBQyxzQkFBc0IsQ0FBQyxnQkFBZ0IsQ0FBQyxzQkFBc0IsQ0FBQyxDQUFDLENBQUM7UUFFNUcsSUFBSSxRQUFRLENBQUMsTUFBTSxFQUFFO1lBQ25CLElBQUksQ0FBQyxTQUFTLENBQUMsUUFBUSxDQUFDLENBQUM7U0FDMUI7YUFBTTtZQUNMLE1BQU0sSUFBSSxLQUFLLENBQUMsSUFBSSxDQUFDLFlBQVksQ0FBQyxnQ0FBZ0MsQ0FBQyxDQUFDO1NBQ3JFO0lBQ0gsQ0FBQztJQUVELFNBQVMsQ0FBQyxRQUF1QjtRQUMvQixLQUFLLENBQUMsUUFBUSxFQUFFLElBQUksQ0FBQyxZQUFZLENBQUMsQ0FBQztJQUNyQyxDQUFDOztvSEEzQlUsc0JBQXNCLGtCQUt2QixXQUFXLGFBQ1gsa0JBQWtCO3dHQU5qQixzQkFBc0IsOExBRUksVUFBVSw2QkFSckM7Ozs7R0FJVDs0RkFFVSxzQkFBc0I7a0JBUmxDLFNBQVM7bUJBQUM7b0JBQ1QsUUFBUSxFQUFFLGlCQUFpQjtvQkFDM0IsUUFBUSxFQUFFOzs7O0dBSVQ7aUJBQ0Y7OzBCQU1JLE1BQU07MkJBQUMsV0FBVzs7MEJBQ2xCLE1BQU07MkJBQUMsa0JBQWtCOzRDQUxuQixZQUFZO3NCQUFwQixLQUFLO2dCQUM0RCxjQUFjO3NCQUEvRSxTQUFTO3VCQUFDLGdCQUFnQixFQUFFLEVBQUUsSUFBSSxFQUFFLFVBQVUsRUFBRSxNQUFNLEVBQUUsS0FBSyxFQUFFIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHsgaXNQbGF0Zm9ybVNlcnZlciB9IGZyb20gJ0Bhbmd1bGFyL2NvbW1vbic7XG5pbXBvcnQgeyBBZnRlclZpZXdJbml0LCBDb21wb25lbnQsIEVsZW1lbnRSZWYsIEluamVjdCwgSW5wdXQsIFBMQVRGT1JNX0lELCBWaWV3Q2hpbGQgfSBmcm9tICdAYW5ndWxhci9jb3JlJztcbmltcG9ydCB0aXBweSBmcm9tICd0aXBweS5qcyc7XG5pbXBvcnQgeyBOZ3hUaXBweU1lc3NhZ2VzRGljdCwgTmd4VGlwcHlQcm9wcyB9IGZyb20gJy4uL25neC10aXBweS5pbnRlcmZhY2VzJztcbmltcG9ydCB7IE5HWF9USVBQWV9NRVNTQUdFUyB9IGZyb20gJy4uL25neC10aXBweS50b2tlbnMnO1xuXG4vKipcbiAqIERpZmZlcmVudCB0b29sdGlwIGNvbnRlbnQgdG8gbWFueSBkaWZmZXJlbnQgZWxlbWVudHMsIGJ1dCBvbmx5IG9uZSB0aXBweSBpbnN0YW5jZVxuICovXG5AQ29tcG9uZW50KHtcbiAgc2VsZWN0b3I6ICduZ3gtdGlwcHktZ3JvdXAnLFxuICB0ZW1wbGF0ZTogYFxuICAgIDxkaXYgI2NvbnRlbnRXcmFwcGVyPlxuICAgICAgPG5nLWNvbnRlbnQ+PC9uZy1jb250ZW50PlxuICAgIDwvZGl2PlxuICBgLFxufSlcbmV4cG9ydCBjbGFzcyBOZ3hUaXBweUdyb3VwQ29tcG9uZW50IGltcGxlbWVudHMgQWZ0ZXJWaWV3SW5pdCB7XG4gIEBJbnB1dCgpIGdyb3VwZWRQcm9wcz86IE5neFRpcHB5UHJvcHM7XG4gIEBWaWV3Q2hpbGQoJ2NvbnRlbnRXcmFwcGVyJywgeyByZWFkOiBFbGVtZW50UmVmLCBzdGF0aWM6IGZhbHNlIH0pIGNvbnRlbnRXcmFwcGVyITogRWxlbWVudFJlZjtcblxuICBjb25zdHJ1Y3RvcihcbiAgICBASW5qZWN0KFBMQVRGT1JNX0lEKSBwcml2YXRlIHBsYXRmb3JtOiBPYmplY3QsXG4gICAgQEluamVjdChOR1hfVElQUFlfTUVTU0FHRVMpIHByaXZhdGUgbWVzc2FnZXNEaWN0OiBOZ3hUaXBweU1lc3NhZ2VzRGljdFxuICApIHt9XG5cbiAgbmdBZnRlclZpZXdJbml0KCkge1xuICAgIGlmIChpc1BsYXRmb3JtU2VydmVyKHRoaXMucGxhdGZvcm0pKSByZXR1cm47XG4gICAgdGhpcy5zZXRUb29sdGlwcygpO1xuICB9XG5cbiAgc2V0VG9vbHRpcHMoKSB7XG4gICAgY29uc3QgY29udGVudFdyYXBwZXJOYXRpdmVFbDogSFRNTEVsZW1lbnQgPSB0aGlzLmNvbnRlbnRXcmFwcGVyLm5hdGl2ZUVsZW1lbnQ7XG4gICAgY29uc3QgdG9vbHRpcHM6IEhUTUxFbGVtZW50W10gPSBBcnJheS5mcm9tKGNvbnRlbnRXcmFwcGVyTmF0aXZlRWwucXVlcnlTZWxlY3RvckFsbCgnW2RhdGEtdGlwcHktZ3JvdXBlZF0nKSk7XG5cbiAgICBpZiAodG9vbHRpcHMubGVuZ3RoKSB7XG4gICAgICB0aGlzLmluaXRUaXBweSh0b29sdGlwcyk7XG4gICAgfSBlbHNlIHtcbiAgICAgIHRocm93IG5ldyBFcnJvcih0aGlzLm1lc3NhZ2VzRGljdC5jaGlsZHJlbkluc3RhbmNlc05vdEZvdW5kR3JvdXBlZCk7XG4gICAgfVxuICB9XG5cbiAgaW5pdFRpcHB5KHRvb2x0aXBzOiBIVE1MRWxlbWVudFtdKSB7XG4gICAgdGlwcHkodG9vbHRpcHMsIHRoaXMuZ3JvdXBlZFByb3BzKTtcbiAgfVxufVxuIl19