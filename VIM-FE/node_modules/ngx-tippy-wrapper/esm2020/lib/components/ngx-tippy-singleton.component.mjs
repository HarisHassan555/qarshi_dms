import { isPlatformServer } from '@angular/common';
import { Component, ElementRef, Inject, Input, PLATFORM_ID, ViewChild } from '@angular/core';
import { createSingleton } from 'tippy.js';
import { NGX_TIPPY_MESSAGES } from '../ngx-tippy.tokens';
import * as i0 from "@angular/core";
import * as i1 from "../services";
/**
 * Tippy singleton - single tippy element that takes the place of an array of regular tippy instances
 */
export class NgxTippySingletonComponent {
    constructor(platform, ngxTippyService, messagesDict) {
        this.platform = platform;
        this.ngxTippyService = ngxTippyService;
        this.messagesDict = messagesDict;
    }
    ngAfterViewInit() {
        if (isPlatformServer(this.platform))
            return;
        this.setSingleton();
    }
    ngOnDestroy() {
        this.clearSingletonInstance();
    }
    /**
     * Take projected in component tooltips element
     * Take initiated tippy instances
     * Initiate `singleton addon` only for projected tooltips for current component instance
     */
    setSingleton() {
        const contentWrapperNativeEl = this.contentWrapper.nativeElement;
        const singletonTooltipIDs = Array.from(contentWrapperNativeEl.querySelectorAll('[data-tippy-singleton]')).map((el) => el._tippy.id);
        const tippyInstances = this.ngxTippyService.getInstances();
        const tippyInstancesSerialized = tippyInstances && [...tippyInstances.values()];
        this.currentSingletonChildrenTippyInstances =
            tippyInstancesSerialized &&
                tippyInstancesSerialized.filter(tippyInstance => singletonTooltipIDs.includes(tippyInstance.id));
        if (this.currentSingletonChildrenTippyInstances?.length) {
            this.initTippySingletonEntry(this.currentSingletonChildrenTippyInstances);
        }
        else {
            throw new Error(this.messagesDict.childrenInstancesNotFoundSingleton);
        }
    }
    initTippySingletonEntry(childrenSingletonInstances) {
        this.singletonInstance = createSingleton(childrenSingletonInstances, this.singletonProps);
        this.writeSingletonInstanceToStorage(this.singletonInstance);
    }
    /**
     * To manipulate singleton groups, write all instances to storage
     * `singletonName` used as unique key
     * If `singletonName` does not provided - it will be generated using id of singletonInstance
     *
     * @param tippyInstance { NgxTippySingletonInstance }
     */
    writeSingletonInstanceToStorage(singletonInstance) {
        const extendedSingletonInstance = this.extendShowFn(singletonInstance);
        this.ngxTippyService.setSingletonInstance(this.singletonName || `singleton-${singletonInstance.id}`, extendedSingletonInstance);
    }
    /**
     * Extend original `show` method
     * Purpose: manipulate tooltip state by [tippyName]
     *
     * @param singletonInstance { NgxTippySingletonInstance }
     * @returns { NgxTippySingletonInstance }
     */
    extendShowFn(singletonInstance) {
        const originalShowFn = singletonInstance.show;
        singletonInstance.show = (singletonInstanceIdentifier) => {
            if (typeof singletonInstanceIdentifier === 'string') {
                const instance = this.ngxTippyService.getInstance(singletonInstanceIdentifier);
                instance && originalShowFn(instance);
            }
            else {
                originalShowFn(singletonInstanceIdentifier);
            }
        };
        return singletonInstance;
    }
    clearSingletonInstance() {
        const singletonInstances = this.ngxTippyService.getSingletonInstances();
        if (singletonInstances && this.singletonInstance) {
            this.destroySingletonInstance();
            this.deleteEntryInStorage(singletonInstances);
        }
    }
    destroySingletonInstance() {
        this.singletonInstance.destroy();
    }
    deleteEntryInStorage(singletonInstances) {
        singletonInstances.delete(this.singletonName || `singleton-${this.singletonInstance.id}`);
    }
}
NgxTippySingletonComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippySingletonComponent, deps: [{ token: PLATFORM_ID }, { token: i1.NgxTippyService }, { token: NGX_TIPPY_MESSAGES }], target: i0.ɵɵFactoryTarget.Component });
NgxTippySingletonComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "14.0.0", version: "14.2.12", type: NgxTippySingletonComponent, selector: "ngx-tippy-singleton", inputs: { singletonProps: "singletonProps", singletonName: "singletonName" }, viewQueries: [{ propertyName: "contentWrapper", first: true, predicate: ["contentWrapper"], descendants: true, read: ElementRef }], ngImport: i0, template: `
    <div #contentWrapper>
      <ng-content></ng-content>
    </div>
  `, isInline: true });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippySingletonComponent, decorators: [{
            type: Component,
            args: [{
                    selector: 'ngx-tippy-singleton',
                    template: `
    <div #contentWrapper>
      <ng-content></ng-content>
    </div>
  `,
                }]
        }], ctorParameters: function () { return [{ type: Object, decorators: [{
                    type: Inject,
                    args: [PLATFORM_ID]
                }] }, { type: i1.NgxTippyService }, { type: undefined, decorators: [{
                    type: Inject,
                    args: [NGX_TIPPY_MESSAGES]
                }] }]; }, propDecorators: { singletonProps: [{
                type: Input
            }], singletonName: [{
                type: Input
            }], contentWrapper: [{
                type: ViewChild,
                args: ['contentWrapper', { read: ElementRef, static: false }]
            }] } });
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibmd4LXRpcHB5LXNpbmdsZXRvbi5jb21wb25lbnQuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyIuLi8uLi8uLi8uLi8uLi9wcm9qZWN0cy9uZ3gtdGlwcHktd3JhcHBlci9zcmMvbGliL2NvbXBvbmVudHMvbmd4LXRpcHB5LXNpbmdsZXRvbi5jb21wb25lbnQudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6IkFBQUEsT0FBTyxFQUFFLGdCQUFnQixFQUFFLE1BQU0saUJBQWlCLENBQUM7QUFDbkQsT0FBTyxFQUFpQixTQUFTLEVBQUUsVUFBVSxFQUFFLE1BQU0sRUFBRSxLQUFLLEVBQWEsV0FBVyxFQUFFLFNBQVMsRUFBRSxNQUFNLGVBQWUsQ0FBQztBQUN2SCxPQUFPLEVBQUUsZUFBZSxFQUFFLE1BQU0sVUFBVSxDQUFDO0FBUTNDLE9BQU8sRUFBRSxrQkFBa0IsRUFBRSxNQUFNLHFCQUFxQixDQUFDOzs7QUFHekQ7O0dBRUc7QUFTSCxNQUFNLE9BQU8sMEJBQTBCO0lBUXJDLFlBQytCLFFBQWdCLEVBQ3JDLGVBQWdDLEVBQ0osWUFBa0M7UUFGekMsYUFBUSxHQUFSLFFBQVEsQ0FBUTtRQUNyQyxvQkFBZSxHQUFmLGVBQWUsQ0FBaUI7UUFDSixpQkFBWSxHQUFaLFlBQVksQ0FBc0I7SUFDckUsQ0FBQztJQUVKLGVBQWU7UUFDYixJQUFJLGdCQUFnQixDQUFDLElBQUksQ0FBQyxRQUFRLENBQUM7WUFBRSxPQUFPO1FBQzVDLElBQUksQ0FBQyxZQUFZLEVBQUUsQ0FBQztJQUN0QixDQUFDO0lBRUQsV0FBVztRQUNULElBQUksQ0FBQyxzQkFBc0IsRUFBRSxDQUFDO0lBQ2hDLENBQUM7SUFFRDs7OztPQUlHO0lBQ0ssWUFBWTtRQUNsQixNQUFNLHNCQUFzQixHQUFnQixJQUFJLENBQUMsY0FBYyxDQUFDLGFBQWEsQ0FBQztRQUM5RSxNQUFNLG1CQUFtQixHQUFhLEtBQUssQ0FBQyxJQUFJLENBQzlDLHNCQUFzQixDQUFDLGdCQUFnQixDQUFDLHdCQUF3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QTBCO2tCQVJ0QyxTQUFTO21CQUFDO29CQUNULFFBQVEsRUFBRSxxQkFBcUI7b0JBQy9CLFFBQVEsRUFBRTs7OztHQUlUO2lCQUNGOzswQkFVSSxNQUFNOzJCQUFDLFdBQVc7OzBCQUVsQixNQUFNOzJCQUFDLGtCQUFrQjs0Q0FWbkIsY0FBYztzQkFBdEIsS0FBSztnQkFDRyxhQUFhO3NCQUFyQixLQUFLO2dCQUM0RCxjQUFjO3NCQUEvRSxTQUFTO3VCQUFDLGdCQUFnQixFQUFFLEVBQUUsSUFBSSxFQUFFLFVBQVUsRUFBRSxNQUFNLEVBQUUsS0FBSyxFQUFFIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHsgaXNQbGF0Zm9ybVNlcnZlciB9IGZyb20gJ0Bhbmd1bGFyL2NvbW1vbic7XG5pbXBvcnQgeyBBZnRlclZpZXdJbml0LCBDb21wb25lbnQsIEVsZW1lbnRSZWYsIEluamVjdCwgSW5wdXQsIE9uRGVzdHJveSwgUExBVEZPUk1fSUQsIFZpZXdDaGlsZCB9IGZyb20gJ0Bhbmd1bGFyL2NvcmUnO1xuaW1wb3J0IHsgY3JlYXRlU2luZ2xldG9uIH0gZnJvbSAndGlwcHkuanMnO1xuaW1wb3J0IHtcbiAgTmd4U2luZ2xldG9uUHJvcHMsXG4gIE5neFRpcHB5SW5zdGFuY2UsXG4gIE5neFRpcHB5TWVzc2FnZXNEaWN0LFxuICBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlLFxuICBUaXBweUhUTUxFbGVtZW50LFxufSBmcm9tICcuLi9uZ3gtdGlwcHkuaW50ZXJmYWNlcyc7XG5pbXBvcnQgeyBOR1hfVElQUFlfTUVTU0FHRVMgfSBmcm9tICcuLi9uZ3gtdGlwcHkudG9rZW5zJztcbmltcG9ydCB7IE5neFRpcHB5U2VydmljZSB9IGZyb20gJy4uL3NlcnZpY2VzJztcblxuLyoqXG4gKiBUaXBweSBzaW5nbGV0b24gLSBzaW5nbGUgdGlwcHkgZWxlbWVudCB0aGF0IHRha2VzIHRoZSBwbGFjZSBvZiBhbiBhcnJheSBvZiByZWd1bGFyIHRpcHB5IGluc3RhbmNlc1xuICovXG5AQ29tcG9uZW50KHtcbiAgc2VsZWN0b3I6ICduZ3gtdGlwcHktc2luZ2xldG9uJyxcbiAgdGVtcGxhdGU6IGBcbiAgICA8ZGl2ICNjb250ZW50V3JhcHBlcj5cbiAgICAgIDxuZy1jb250ZW50PjwvbmctY29udGVudD5cbiAgICA8L2Rpdj5cbiAgYCxcbn0pXG5leHBvcnQgY2xhc3MgTmd4VGlwcHlTaW5nbGV0b25Db21wb25lbnQgaW1wbGVtZW50cyBBZnRlclZpZXdJbml0LCBPbkRlc3Ryb3kge1xuICBASW5wdXQoKSBzaW5nbGV0b25Qcm9wcz86IE5neFNpbmdsZXRvblByb3BzO1xuICBASW5wdXQoKSBzaW5nbGV0b25OYW1lPzogc3RyaW5nO1xuICBAVmlld0NoaWxkKCdjb250ZW50V3JhcHBlcicsIHsgcmVhZDogRWxlbWVudFJlZiwgc3RhdGljOiBmYWxzZSB9KSBjb250ZW50V3JhcHBlciE6IEVsZW1lbnRSZWY7XG5cbiAgcHJpdmF0ZSBzaW5nbGV0b25JbnN0YW5jZSE6IE5neFRpcHB5U2luZ2xldG9uSW5zdGFuY2U7XG4gIHByaXZhdGUgY3VycmVudFNpbmdsZXRvbkNoaWxkcmVuVGlwcHlJbnN0YW5jZXMhOiBOZ3hUaXBweUluc3RhbmNlW10gfCBudWxsO1xuXG4gIGNvbnN0cnVjdG9yKFxuICAgIEBJbmplY3QoUExBVEZPUk1fSUQpIHByaXZhdGUgcGxhdGZvcm06IE9iamVjdCxcbiAgICBwcml2YXRlIG5neFRpcHB5U2VydmljZTogTmd4VGlwcHlTZXJ2aWNlLFxuICAgIEBJbmplY3QoTkdYX1RJUFBZX01FU1NBR0VTKSBwcml2YXRlIG1lc3NhZ2VzRGljdDogTmd4VGlwcHlNZXNzYWdlc0RpY3RcbiAgKSB7fVxuXG4gIG5nQWZ0ZXJWaWV3SW5pdCgpIHtcbiAgICBpZiAoaXNQbGF0Zm9ybVNlcnZlcih0aGlzLnBsYXRmb3JtKSkgcmV0dXJuO1xuICAgIHRoaXMuc2V0U2luZ2xldG9uKCk7XG4gIH1cblxuICBuZ09uRGVzdHJveSgpIHtcbiAgICB0aGlzLmNsZWFyU2luZ2xldG9uSW5zdGFuY2UoKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBUYWtlIHByb2plY3RlZCBpbiBjb21wb25lbnQgdG9vbHRpcHMgZWxlbWVudFxuICAgKiBUYWtlIGluaXRpYXRlZCB0aXBweSBpbnN0YW5jZXNcbiAgICogSW5pdGlhdGUgYHNpbmdsZXRvbiBhZGRvbmAgb25seSBmb3IgcHJvamVjdGVkIHRvb2x0aXBzIGZvciBjdXJyZW50IGNvbXBvbmVudCBpbnN0YW5jZVxuICAgKi9cbiAgcHJpdmF0ZSBzZXRTaW5nbGV0b24oKSB7XG4gICAgY29uc3QgY29udGVudFdyYXBwZXJOYXRpdmVFbDogSFRNTEVsZW1lbnQgPSB0aGlzLmNvbnRlbnRXcmFwcGVyLm5hdGl2ZUVsZW1lbnQ7XG4gICAgY29uc3Qgc2luZ2xldG9uVG9vbHRpcElEczogbnVtYmVyW10gPSBBcnJheS5mcm9tPFRpcHB5SFRNTEVsZW1lbnQ+KFxuICAgICAgY29udGVudFdyYXBwZXJOYXRpdmVFbC5xdWVyeVNlbGVjdG9yQWxsKCdbZGF0YS10aXBweS1zaW5nbGV0b25dJylcbiAgICApLm1hcCgoZWw6IFRpcHB5SFRNTEVsZW1lbnQpID0+IGVsLl90aXBweS5pZCk7XG5cbiAgICBjb25zdCB0aXBweUluc3RhbmNlcyA9IHRoaXMubmd4VGlwcHlTZXJ2aWNlLmdldEluc3RhbmNlcygpO1xuXG4gICAgY29uc3QgdGlwcHlJbnN0YW5jZXNTZXJpYWxpemVkID0gdGlwcHlJbnN0YW5jZXMgJiYgWy4uLnRpcHB5SW5zdGFuY2VzLnZhbHVlcygpXTtcblxuICAgIHRoaXMuY3VycmVudFNpbmdsZXRvbkNoaWxkcmVuVGlwcHlJbnN0YW5jZXMgPVxuICAgICAgdGlwcHlJbnN0YW5jZXNTZXJpYWxpemVkICYmXG4gICAgICB0aXBweUluc3RhbmNlc1NlcmlhbGl6ZWQuZmlsdGVyKHRpcHB5SW5zdGFuY2UgPT4gc2luZ2xldG9uVG9vbHRpcElEcy5pbmNsdWRlcyh0aXBweUluc3RhbmNlLmlkKSk7XG5cbiAgICBpZiAodGhpcy5jdXJyZW50U2luZ2xldG9uQ2hpbGRyZW5UaXBweUluc3RhbmNlcz8ubGVuZ3RoKSB7XG4gICAgICB0aGlzLmluaXRUaXBweVNpbmdsZXRvbkVudHJ5KHRoaXMuY3VycmVudFNpbmdsZXRvbkNoaWxkcmVuVGlwcHlJbnN0YW5jZXMpO1xuICAgIH0gZWxzZSB7XG4gICAgICB0aHJvdyBuZXcgRXJyb3IodGhpcy5tZXNzYWdlc0RpY3QuY2hpbGRyZW5JbnN0YW5jZXNOb3RGb3VuZFNpbmdsZXRvbik7XG4gICAgfVxuICB9XG5cbiAgcHJpdmF0ZSBpbml0VGlwcHlTaW5nbGV0b25FbnRyeShjaGlsZHJlblNpbmdsZXRvbkluc3RhbmNlczogTmd4VGlwcHlJbnN0YW5jZVtdKSB7XG4gICAgdGhpcy5zaW5nbGV0b25JbnN0YW5jZSA9IGNyZWF0ZVNpbmdsZXRvbihjaGlsZHJlblNpbmdsZXRvbkluc3RhbmNlcywgdGhpcy5zaW5nbGV0b25Qcm9wcyk7XG4gICAgdGhpcy53cml0ZVNpbmdsZXRvbkluc3RhbmNlVG9TdG9yYWdlKHRoaXMuc2luZ2xldG9uSW5zdGFuY2UpO1xuICB9XG5cbiAgLyoqXG4gICAqIFRvIG1hbmlwdWxhdGUgc2luZ2xldG9uIGdyb3Vwcywgd3JpdGUgYWxsIGluc3RhbmNlcyB0byBzdG9yYWdlXG4gICAqIGBzaW5nbGV0b25OYW1lYCB1c2VkIGFzIHVuaXF1ZSBrZXlcbiAgICogSWYgYHNpbmdsZXRvbk5hbWVgIGRvZXMgbm90IHByb3ZpZGVkIC0gaXQgd2lsbCBiZSBnZW5lcmF0ZWQgdXNpbmcgaWQgb2Ygc2luZ2xldG9uSW5zdGFuY2VcbiAgICpcbiAgICogQHBhcmFtIHRpcHB5SW5zdGFuY2UgeyBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlIH1cbiAgICovXG4gIHByaXZhdGUgd3JpdGVTaW5nbGV0b25JbnN0YW5jZVRvU3RvcmFnZShzaW5nbGV0b25JbnN0YW5jZTogTmd4VGlwcHlTaW5nbGV0b25JbnN0YW5jZSkge1xuICAgIGNvbnN0IGV4dGVuZGVkU2luZ2xldG9uSW5zdGFuY2UgPSB0aGlzLmV4dGVuZFNob3dGbihzaW5nbGV0b25JbnN0YW5jZSk7XG5cbiAgICB0aGlzLm5neFRpcHB5U2VydmljZS5zZXRTaW5nbGV0b25JbnN0YW5jZShcbiAgICAgIHRoaXMuc2luZ2xldG9uTmFtZSB8fCBgc2luZ2xldG9uLSR7c2luZ2xldG9uSW5zdGFuY2UuaWR9YCxcbiAgICAgIGV4dGVuZGVkU2luZ2xldG9uSW5zdGFuY2VcbiAgICApO1xuICB9XG5cbiAgLyoqXG4gICAqIEV4dGVuZCBvcmlnaW5hbCBgc2hvd2AgbWV0aG9kXG4gICAqIFB1cnBvc2U6IG1hbmlwdWxhdGUgdG9vbHRpcCBzdGF0ZSBieSBbdGlwcHlOYW1lXVxuICAgKlxuICAgKiBAcGFyYW0gc2luZ2xldG9uSW5zdGFuY2UgeyBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlIH1cbiAgICogQHJldHVybnMgeyBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlIH1cbiAgICovXG4gIHByaXZhdGUgZXh0ZW5kU2hvd0ZuKHNpbmdsZXRvbkluc3RhbmNlOiBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlKTogTmd4VGlwcHlTaW5nbGV0b25JbnN0YW5jZSB7XG4gICAgY29uc3Qgb3JpZ2luYWxTaG93Rm4gPSBzaW5nbGV0b25JbnN0YW5jZS5zaG93O1xuXG4gICAgc2luZ2xldG9uSW5zdGFuY2Uuc2hvdyA9IChzaW5nbGV0b25JbnN0YW5jZUlkZW50aWZpZXI6IHN0cmluZyB8IG51bWJlciB8IE5neFRpcHB5SW5zdGFuY2UpID0+IHtcbiAgICAgIGlmICh0eXBlb2Ygc2luZ2xldG9uSW5zdGFuY2VJZGVudGlmaWVyID09PSAnc3RyaW5nJykge1xuICAgICAgICBjb25zdCBpbnN0YW5jZSA9IHRoaXMubmd4VGlwcHlTZXJ2aWNlLmdldEluc3RhbmNlKHNpbmdsZXRvbkluc3RhbmNlSWRlbnRpZmllcik7XG4gICAgICAgIGluc3RhbmNlICYmIG9yaWdpbmFsU2hvd0ZuKGluc3RhbmNlKTtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIG9yaWdpbmFsU2hvd0ZuKHNpbmdsZXRvbkluc3RhbmNlSWRlbnRpZmllcik7XG4gICAgICB9XG4gICAgfTtcblxuICAgIHJldHVybiBzaW5nbGV0b25JbnN0YW5jZTtcbiAgfVxuXG4gIHByaXZhdGUgY2xlYXJTaW5nbGV0b25JbnN0YW5jZSgpIHtcbiAgICBjb25zdCBzaW5nbGV0b25JbnN0YW5jZXMgPSB0aGlzLm5neFRpcHB5U2VydmljZS5nZXRTaW5nbGV0b25JbnN0YW5jZXMoKTtcblxuICAgIGlmIChzaW5nbGV0b25JbnN0YW5jZXMgJiYgdGhpcy5zaW5nbGV0b25JbnN0YW5jZSkge1xuICAgICAgdGhpcy5kZXN0cm95U2luZ2xldG9uSW5zdGFuY2UoKTtcbiAgICAgIHRoaXMuZGVsZXRlRW50cnlJblN0b3JhZ2Uoc2luZ2xldG9uSW5zdGFuY2VzKTtcbiAgICB9XG4gIH1cblxuICBwcml2YXRlIGRlc3Ryb3lTaW5nbGV0b25JbnN0YW5jZSgpIHtcbiAgICB0aGlzLnNpbmdsZXRvbkluc3RhbmNlLmRlc3Ryb3koKTtcbiAgfVxuXG4gIHByaXZhdGUgZGVsZXRlRW50cnlJblN0b3JhZ2Uoc2luZ2xldG9uSW5zdGFuY2VzOiBNYXA8c3RyaW5nLCBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlPikge1xuICAgIHNpbmdsZXRvbkluc3RhbmNlcy5kZWxldGUodGhpcy5zaW5nbGV0b25OYW1lIHx8IGBzaW5nbGV0b24tJHt0aGlzLnNpbmdsZXRvbkluc3RhbmNlLmlkfWApO1xuICB9XG59XG4iXX0=