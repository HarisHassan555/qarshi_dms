import { Inject, Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import tippy, { hideAll } from 'tippy.js';
import { InstanceChangeReasonEnum, NgxTippyNamesEnum, } from '../ngx-tippy.interfaces';
import { NGX_TIPPY_MESSAGES } from '../ngx-tippy.tokens';
import { setTemplateVisible } from '../utils';
import * as i0 from "@angular/core";
import * as i1 from "./dev-mode.service";
import * as i2 from "./ngx-view.service";
export class NgxTippyService {
    constructor(rendererFactory, devModeService, ngxViewService, messagesDict) {
        this.devModeService = devModeService;
        this.ngxViewService = ngxViewService;
        this.messagesDict = messagesDict;
        this.tippyInstances = new Map();
        this.tippySingletonEntryInstances = new Map();
        this.tippyInstances$ = new Subject();
        this.createRenderer(rendererFactory);
    }
    /**
     * Working with storage
     */
    /**
     * Write tippy instances to storage
     *
     * @param name { string } name of tippy instance
     * @param state { NgxTippyInstance } tippy instance
     */
    setInstance(name, state) {
        const instance = this.tippyInstances.get(name);
        if (instance) {
            const errMessage = this.getMessage({
                reason: 'instanceAlreadyExist',
                messageFor: NgxTippyNamesEnum.TippyName,
                name,
            });
            this.throwError(errMessage);
        }
        else {
            this.tippyInstances.set(name, state);
            this.emitInstancesChange({
                name,
                reason: InstanceChangeReasonEnum.SetInstance,
                instance: state,
            });
        }
    }
    /**
     * Get specific tippy instance
     *
     * @param name { string } name of tippy instance
     * @returns { NgxTippyInstance | null } specific tippy instance or null
     */
    getInstance(name) {
        return this.tippyInstances.has(name) ? this.tippyInstances.get(name) : null;
    }
    /**
     * Get all tippy instances from storage
     *
     * @returns { Map<string, NgxTippyInstance> | null } all tippy instances or null
     */
    getInstances() {
        return this.tippyInstances.size ? this.tippyInstances : null;
    }
    /**
     * Write singleton instances to storage
     *
     * @param name { string } name of tippy instance
     * @param state { NgxTippyInstance } tippy instance
     */
    setSingletonInstance(name, state) {
        if (this.tippySingletonEntryInstances.has(name)) {
            const errMessage = this.getMessage({
                reason: 'singletonInstanceAlreadyExist',
                messageFor: NgxTippyNamesEnum.SingletonName,
                name,
            });
            this.throwError(errMessage);
        }
        else {
            this.tippySingletonEntryInstances.set(name, state);
        }
    }
    /**
     * Get specific singleton tippy instance
     *
     * @param name { string } name of singleton tippy instance
     * @returns { NgxTippySingletonInstance | null } specific singleton tippy instance or null
     */
    getSingletonInstance(name) {
        return this.tippySingletonEntryInstances.has(name) ? this.tippySingletonEntryInstances.get(name) : null;
    }
    /**
     * Get all singleton tippy instances
     *
     * @returns { Map<string, NgxTippyInstance> | null } all singleton tippy instances or null
     */
    getSingletonInstances() {
        return this.tippySingletonEntryInstances.size ? this.tippySingletonEntryInstances : null;
    }
    /**
     * Working with tippy instance methods
     */
    /**
     * Programmatically show the tippy
     *
     * @param name { string } name of tippy instance
     */
    show(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.Show);
    }
    /**
     * Programmatically hide the tippy
     *
     * @param name { string } name of tippy instance
     */
    hide(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.Hide);
    }
    /**
     * Will hide the tippy only if the cursor is outside of the tippy's interactive region
     * This allows you to programmatically hook into interactive behavior upon a mouseleave event if implementing custom event listeners
     *
     * @param name { string } name of tippy instance
     * @param name { mouseEvent } pass the mouse event object in from your event listener
     */
    hideWithInteractivity(name, mouseEvent) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.HideWithInteractivity, mouseEvent);
    }
    /**
     * Prevent a tippy from showing or hiding
     *
     * @param name { string } name of tippy instance
     */
    disable(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.Disable);
    }
    /**
     * Re-enable a tippy
     *
     * @param name { string } name of tippy instance
     */
    enable(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.Enable);
    }
    /**
     * Update any tippy props
     *
     * @param name { string } name of tippy instance
     * @param tippyProps { NgxTippyProps } new props
     */
    setProps(name, tippyProps) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.SetProps, tippyProps);
    }
    /**
     * Update the content for tippy
     *
     * @param name { string } name of tippy instance
     * @param tippyContent { NgxTippyContent } new content
     */
    setContent(name, tippyContent) {
        const instance = this.getInstance(name);
        if (!instance) {
            this.throwErrorInstanceNotExist(name);
            return;
        }
        if (tippyContent) {
            const viewRef = this.ngxViewService.getViewRefInstance(tippyContent, instance.tippyName);
            const content = viewRef.getElement();
            if (content) {
                setTemplateVisible(content, this.renderer);
                instance.setContent(content);
                instance.viewRef = viewRef;
                this.emitInstancesChange({
                    name,
                    reason: 'setContent',
                    instance,
                });
            }
            else {
                this.throwError('message');
            }
        }
    }
    /**
     * Unmount the tippy from the DOM
     *
     * @param name { string } name of tippy instance
     */
    unmount(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.Unmount);
    }
    /**
     * Clears the instances delay timeouts
     *
     * @param name { string } name of tippy instance
     */
    clearDelayTimeouts(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.ClearDelayTimeouts);
    }
    /**
     * Permanently destroy and clean up the tippy instance
     *
     * @param name { string } name of tippy instance
     */
    destroy(name) {
        this.callNativeTippyMethod(name, InstanceChangeReasonEnum.Destroy);
        this.tippyInstances.delete(name);
    }
    /** Working with tippy static methods */
    /**
     * Set the default props for each new tippy instance
     *
     * @param tippyProps { NgxTippyDefaultProps } default props
     */
    setDefaultProps(tippyProps) {
        tippy.setDefaultProps(tippyProps);
    }
    /**
     * Show all tippies
     */
    showAll() {
        this.tippyInstances.forEach((instance, name) => {
            instance.show();
            this.emitInstancesChange({
                name,
                reason: 'show',
                instance,
            });
        });
    }
    /**
     * Hide all tippies or hide all except a particular one
     * Additional hide them with duration
     *
     * @param { NgxTippyHideAllOptions } [options] - additional hiding options
     */
    hideAll(options) {
        const exclude = this.getInstance(options?.excludeName || '');
        const duration = options?.duration;
        hideAll({ duration, ...exclude });
    }
    /**
     * Subscription to change of tippy instances
     *
     * @returns { Observable<InstancesChanges> } observable of tippy instances change
     */
    get instancesChanges() {
        return this.tippyInstances$.asObservable();
    }
    /**
     * Service methods
     */
    callNativeTippyMethod(name, method, arg) {
        const instance = this.getInstance(name);
        if (instance) {
            instance[method](arg);
            this.emitInstancesChange({
                name,
                reason: method,
                instance,
            });
        }
        else {
            this.throwErrorInstanceNotExist(name);
        }
    }
    emitInstancesChange({ name, reason, instance, }) {
        this.tippyInstances$.next({ name, reason, instance });
    }
    createRenderer(rendererFactory) {
        this.renderer = rendererFactory.createRenderer(null, null);
    }
    throwErrorInstanceNotExist(name) {
        const errMessage = this.getMessage({
            reason: 'instanceNotExist',
            messageFor: NgxTippyNamesEnum.TippyName,
            name,
        });
        this.throwError(errMessage);
    }
    throwError(message, errorConstructor = Error) {
        if (this.devModeService.isDevMode())
            throw new errorConstructor(message);
    }
    getMessage({ reason, messageFor, name, }) {
        return this.messagesDict[reason].replace(`#${messageFor}`, `'${name}'`);
    }
}
NgxTippyService.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippyService, deps: [{ token: i0.RendererFactory2 }, { token: i1.DevModeService }, { token: i2.NgxViewService }, { token: NGX_TIPPY_MESSAGES }], target: i0.ɵɵFactoryTarget.Injectable });
NgxTippyService.ɵprov = i0.ɵɵngDeclareInjectable({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippyService, providedIn: 'root' });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "14.2.12", ngImport: i0, type: NgxTippyService, decorators: [{
            type: Injectable,
            args: [{
                    providedIn: 'root',
                }]
        }], ctorParameters: function () { return [{ type: i0.RendererFactory2 }, { type: i1.DevModeService }, { type: i2.NgxViewService }, { type: undefined, decorators: [{
                    type: Inject,
                    args: [NGX_TIPPY_MESSAGES]
                }] }]; } });
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibmd4LXRpcHB5LnNlcnZpY2UuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyIuLi8uLi8uLi8uLi8uLi9wcm9qZWN0cy9uZ3gtdGlwcHktd3JhcHBlci9zcmMvbGliL3NlcnZpY2VzL25neC10aXBweS5zZXJ2aWNlLnRzIl0sIm5hbWVzIjpbXSwibWFwcGluZ3MiOiJBQUFBLE9BQU8sRUFBRSxNQUFNLEVBQUUsVUFBVSxFQUErQixNQUFNLGVBQWUsQ0FBQztBQUNoRixPQUFPLEVBQWMsT0FBTyxFQUFFLE1BQU0sTUFBTSxDQUFDO0FBQzNDLE9BQU8sS0FBSyxFQUFFLEVBQUUsT0FBTyxFQUFFLE1BQU0sVUFBVSxDQUFDO0FBQzFDLE9BQU8sRUFFTCx3QkFBd0IsRUFReEIsaUJBQWlCLEdBR2xCLE1BQU0seUJBQXlCLENBQUM7QUFDakMsT0FBTyxFQUFFLGtCQUFrQixFQUFFLE1BQU0scUJBQXFCLENBQUM7QUFDekQsT0FBTyxFQUFFLGtCQUFrQixFQUFFLE1BQU0sVUFBVSxDQUFDOzs7O0FBTzlDLE1BQU0sT0FBTyxlQUFlO0lBTTFCLFlBQ0UsZUFBaUMsRUFDekIsY0FBOEIsRUFDOUIsY0FBOEIsRUFDRixZQUFrQztRQUY5RCxtQkFBYyxHQUFkLGNBQWMsQ0FBZ0I7UUFDOUIsbUJBQWMsR0FBZCxjQUFjLENBQWdCO1FBQ0YsaUJBQVksR0FBWixZQUFZLENBQXNCO1FBVGhFLG1CQUFjLEdBQWtDLElBQUksR0FBRyxFQUFFLENBQUM7UUFDMUQsaUNBQTRCLEdBQTJDLElBQUksR0FBRyxFQUFFLENBQUM7UUFDakYsb0JBQWUsR0FBRyxJQUFJLE9BQU8sRUFBb0IsQ0FBQztRQVN4RCxJQUFJLENBQUMsY0FBYyxDQUFDLGVBQWUsQ0FBQyxDQUFDO0lBQ3ZDLENBQUM7SUFDRDs7T0FFRztJQUVIOzs7OztPQUtHO0lBQ0gsV0FBVyxDQUFDLElBQVksRUFBRSxLQUF1QjtRQUMvQyxNQUFNLFFBQVEsR0FBRyxJQUFJLENBQUMsY0FBYyxDQUFDLEdBQUcsQ0FBQyxJ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VSxDQUFDLFVBQVUsQ0FBQyxDQUFDO0lBQzlCLENBQUM7SUFFTyxVQUFVLENBQUMsT0FBZSxFQUFFLG1CQUFxQyxLQUFLO1FBQzVFLElBQUksSUFBSSxDQUFDLGNBQWMsQ0FBQyxTQUFTLEVBQUU7WUFBRSxNQUFNLElBQUksZ0JBQWdCLENBQUMsT0FBTyxDQUFDLENBQUM7SUFDM0UsQ0FBQztJQUVPLFVBQVUsQ0FBQyxFQUNqQixNQUFNLEVBQ04sVUFBVSxFQUNWLElBQUksR0FLTDtRQUNDLE9BQU8sSUFBSSxDQUFDLFlBQVksQ0FBQyxNQUFNLENBQUMsQ0FBQyxPQUFPLENBQUMsSUFBSSxVQUFVLEVBQUUsRUFBRSxJQUFJLElBQUksR0FBRyxDQUFDLENBQUM7SUFDMUUsQ0FBQzs7NkdBelVVLGVBQWUsOEdBVWhCLGtCQUFrQjtpSEFWakIsZUFBZSxjQUZkLE1BQU07NEZBRVAsZUFBZTtrQkFIM0IsVUFBVTttQkFBQztvQkFDVixVQUFVLEVBQUUsTUFBTTtpQkFDbkI7OzBCQVdJLE1BQU07MkJBQUMsa0JBQWtCIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHsgSW5qZWN0LCBJbmplY3RhYmxlLCBSZW5kZXJlcjIsIFJlbmRlcmVyRmFjdG9yeTIgfSBmcm9tICdAYW5ndWxhci9jb3JlJztcbmltcG9ydCB7IE9ic2VydmFibGUsIFN1YmplY3QgfSBmcm9tICdyeGpzJztcbmltcG9ydCB0aXBweSwgeyBoaWRlQWxsIH0gZnJvbSAndGlwcHkuanMnO1xuaW1wb3J0IHtcbiAgSW5zdGFuY2VDaGFuZ2VSZWFzb24sXG4gIEluc3RhbmNlQ2hhbmdlUmVhc29uRW51bSxcbiAgSW5zdGFuY2VzQ2hhbmdlcyxcbiAgTmd4VGlwcHlDb250ZW50LFxuICBOZ3hUaXBweURlZmF1bHRQcm9wcyxcbiAgTmd4VGlwcHlIaWRlQWxsT3B0aW9ucyxcbiAgTmd4VGlwcHlJbnN0YW5jZSxcbiAgTmd4VGlwcHlNZXNzYWdlc0RpY3QsXG4gIE5neFRpcHB5TWVzc2FnZXNUeXBlcyxcbiAgTmd4VGlwcHlOYW1lc0VudW0sXG4gIE5neFRpcHB5UHJvcHMsXG4gIE5neFRpcHB5U2luZ2xldG9uSW5zdGFuY2UsXG59IGZyb20gJy4uL25neC10aXBweS5pbnRlcmZhY2VzJztcbmltcG9ydCB7IE5HWF9USVBQWV9NRVNTQUdFUyB9IGZyb20gJy4uL25neC10aXBweS50b2tlbnMnO1xuaW1wb3J0IHsgc2V0VGVtcGxhdGVWaXNpYmxlIH0gZnJvbSAnLi4vdXRpbHMnO1xuaW1wb3J0IHsgRGV2TW9kZVNlcnZpY2UgfSBmcm9tICcuL2Rldi1tb2RlLnNlcnZpY2UnO1xuaW1wb3J0IHsgTmd4Vmlld1NlcnZpY2UgfSBmcm9tICcuL25neC12aWV3LnNlcnZpY2UnO1xuXG5ASW5qZWN0YWJsZSh7XG4gIHByb3ZpZGVkSW46ICdyb290Jyxcbn0pXG5leHBvcnQgY2xhc3MgTmd4VGlwcHlTZXJ2aWNlIHtcbiAgcHJpdmF0ZSB0aXBweUluc3RhbmNlczogTWFwPHN0cmluZywgTmd4VGlwcHlJbnN0YW5jZT4gPSBuZXcgTWFwKCk7XG4gIHByaXZhdGUgdGlwcHlTaW5nbGV0b25FbnRyeUluc3RhbmNlczogTWFwPHN0cmluZywgTmd4VGlwcHlTaW5nbGV0b25JbnN0YW5jZT4gPSBuZXcgTWFwKCk7XG4gIHByaXZhdGUgdGlwcHlJbnN0YW5jZXMkID0gbmV3IFN1YmplY3Q8SW5zdGFuY2VzQ2hhbmdlcz4oKTtcbiAgcHJpdmF0ZSByZW5kZXJlciE6IFJlbmRlcmVyMjtcblxuICBjb25zdHJ1Y3RvcihcbiAgICByZW5kZXJlckZhY3Rvcnk6IFJlbmRlcmVyRmFjdG9yeTIsXG4gICAgcHJpdmF0ZSBkZXZNb2RlU2VydmljZTogRGV2TW9kZVNlcnZpY2UsXG4gICAgcHJpdmF0ZSBuZ3hWaWV3U2VydmljZTogTmd4Vmlld1NlcnZpY2UsXG4gICAgQEluamVjdChOR1hfVElQUFlfTUVTU0FHRVMpIHByaXZhdGUgbWVzc2FnZXNEaWN0OiBOZ3hUaXBweU1lc3NhZ2VzRGljdFxuICApIHtcbiAgICB0aGlzLmNyZWF0ZVJlbmRlcmVyKHJlbmRlcmVyRmFjdG9yeSk7XG4gIH1cbiAgLyoqXG4gICAqIFdvcmtpbmcgd2l0aCBzdG9yYWdlXG4gICAqL1xuXG4gIC8qKlxuICAgKiBXcml0ZSB0aXBweSBpbnN0YW5jZXMgdG8gc3RvcmFnZVxuICAgKlxuICAgKiBAcGFyYW0gbmFtZSB7IHN0cmluZyB9IG5hbWUgb2YgdGlwcHkgaW5zdGFuY2VcbiAgICogQHBhcmFtIHN0YXRlIHsgTmd4VGlwcHlJbnN0YW5jZSB9IHRpcHB5IGluc3RhbmNlXG4gICAqL1xuICBzZXRJbnN0YW5jZShuYW1lOiBzdHJpbmcsIHN0YXRlOiBOZ3hUaXBweUluc3RhbmNlKSB7XG4gICAgY29uc3QgaW5zdGFuY2UgPSB0aGlzLnRpcHB5SW5zdGFuY2VzLmdldChuYW1lKTtcblxuICAgIGlmIChpbnN0YW5jZSkge1xuICAgICAgY29uc3QgZXJyTWVzc2FnZSA9IHRoaXMuZ2V0TWVzc2FnZSh7XG4gICAgICAgIHJlYXNvbjogJ2luc3RhbmNlQWxyZWFkeUV4aXN0JyxcbiAgICAgICAgbWVzc2FnZUZvcjogTmd4VGlwcHlOYW1lc0VudW0uVGlwcHlOYW1lLFxuICAgICAgICBuYW1lLFxuICAgICAgfSk7XG4gICAgICB0aGlzLnRocm93RXJyb3IoZXJyTWVzc2FnZSk7XG4gICAgfSBlbHNlIHtcbiAgICAgIHRoaXMudGlwcHlJbnN0YW5jZXMuc2V0KG5hbWUsIHN0YXRlKTtcbiAgICAgIHRoaXMuZW1pdEluc3RhbmNlc0NoYW5nZSh7XG4gICAgICAgIG5hbWUsXG4gICAgICAgIHJlYXNvbjogSW5zdGFuY2VDaGFuZ2VSZWFzb25FbnVtLlNldEluc3RhbmNlLFxuICAgICAgICBpbnN0YW5jZTogc3RhdGUsXG4gICAgICB9KTtcbiAgICB9XG4gIH1cblxuICAvKipcbiAgICogR2V0IHNwZWNpZmljIHRpcHB5IGluc3RhbmNlXG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKiBAcmV0dXJucyB7IE5neFRpcHB5SW5zdGFuY2UgfCBudWxsIH0gc3BlY2lmaWMgdGlwcHkgaW5zdGFuY2Ugb3IgbnVsbFxuICAgKi9cbiAgZ2V0SW5zdGFuY2UobmFtZTogc3RyaW5nKTogTmd4VGlwcHlJbnN0YW5jZSB8IG51bGwge1xuICAgIHJldHVybiB0aGlzLnRpcHB5SW5zdGFuY2VzLmhhcyhuYW1lKSA/IHRoaXMudGlwcHlJbnN0YW5jZXMuZ2V0KG5hbWUpISA6IG51bGw7XG4gIH1cblxuICAvKipcbiAgICogR2V0IGFsbCB0aXBweSBpbnN0YW5jZXMgZnJvbSBzdG9yYWdlXG4gICAqXG4gICAqIEByZXR1cm5zIHsgTWFwPHN0cmluZywgTmd4VGlwcHlJbnN0YW5jZT4gfCBudWxsIH0gYWxsIHRpcHB5IGluc3RhbmNlcyBvciBudWxsXG4gICAqL1xuICBnZXRJbnN0YW5jZXMoKTogTWFwPHN0cmluZywgTmd4VGlwcHlJbnN0YW5jZT4gfCBudWxsIHtcbiAgICByZXR1cm4gdGhpcy50aXBweUluc3RhbmNlcy5zaXplID8gdGhpcy50aXBweUluc3RhbmNlcyA6IG51bGw7XG4gIH1cblxuICAvKipcbiAgICogV3JpdGUgc2luZ2xldG9uIGluc3RhbmNlcyB0byBzdG9yYWdlXG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKiBAcGFyYW0gc3RhdGUgeyBOZ3hUaXBweUluc3RhbmNlIH0gdGlwcHkgaW5zdGFuY2VcbiAgICovXG4gIHNldFNpbmdsZXRvbkluc3RhbmNlKG5hbWU6IHN0cmluZywgc3RhdGU6IE5neFRpcHB5U2luZ2xldG9uSW5zdGFuY2UpIHtcbiAgICBpZiAodGhpcy50aXBweVNpbmdsZXRvbkVudHJ5SW5zdGFuY2VzLmhhcyhuYW1lKSkge1xuICAgICAgY29uc3QgZXJyTWVzc2FnZSA9IHRoaXMuZ2V0TWVzc2FnZSh7XG4gICAgICAgIHJlYXNvbjogJ3NpbmdsZXRvbkluc3RhbmNlQWxyZWFkeUV4aXN0JyxcbiAgICAgICAgbWVzc2FnZUZvcjogTmd4VGlwcHlOYW1lc0VudW0uU2luZ2xldG9uTmFtZSxcbiAgICAgICAgbmFtZSxcbiAgICAgIH0pO1xuICAgICAgdGhpcy50aHJvd0Vycm9yKGVyck1lc3NhZ2UpO1xuICAgIH0gZWxzZSB7XG4gICAgICB0aGlzLnRpcHB5U2luZ2xldG9uRW50cnlJbnN0YW5jZXMuc2V0KG5hbWUsIHN0YXRlKTtcbiAgICB9XG4gIH1cblxuICAvKipcbiAgICogR2V0IHNwZWNpZmljIHNpbmdsZXRvbiB0aXBweSBpbnN0YW5jZVxuICAgKlxuICAgKiBAcGFyYW0gbmFtZSB7IHN0cmluZyB9IG5hbWUgb2Ygc2luZ2xldG9uIHRpcHB5IGluc3RhbmNlXG4gICAqIEByZXR1cm5zIHsgTmd4VGlwcHlTaW5nbGV0b25JbnN0YW5jZSB8IG51bGwgfSBzcGVjaWZpYyBzaW5nbGV0b24gdGlwcHkgaW5zdGFuY2Ugb3IgbnVsbFxuICAgKi9cbiAgZ2V0U2luZ2xldG9uSW5zdGFuY2UobmFtZTogc3RyaW5nKTogTmd4VGlwcHlTaW5nbGV0b25JbnN0YW5jZSB8IG51bGwge1xuICAgIHJldHVybiB0aGlzLnRpcHB5U2luZ2xldG9uRW50cnlJbnN0YW5jZXMuaGFzKG5hbWUpID8gdGhpcy50aXBweVNpbmdsZXRvbkVudHJ5SW5zdGFuY2VzLmdldChuYW1lKSEgOiBudWxsO1xuICB9XG5cbiAgLyoqXG4gICAqIEdldCBhbGwgc2luZ2xldG9uIHRpcHB5IGluc3RhbmNlc1xuICAgKlxuICAgKiBAcmV0dXJucyB7IE1hcDxzdHJpbmcsIE5neFRpcHB5SW5zdGFuY2U+IHwgbnVsbCB9IGFsbCBzaW5nbGV0b24gdGlwcHkgaW5zdGFuY2VzIG9yIG51bGxcbiAgICovXG4gIGdldFNpbmdsZXRvbkluc3RhbmNlcygpOiBNYXA8c3RyaW5nLCBOZ3hUaXBweVNpbmdsZXRvbkluc3RhbmNlPiB8IG51bGwge1xuICAgIHJldHVybiB0aGlzLnRpcHB5U2luZ2xldG9uRW50cnlJbnN0YW5jZXMuc2l6ZSA/IHRoaXMudGlwcHlTaW5nbGV0b25FbnRyeUluc3RhbmNlcyA6IG51bGw7XG4gIH1cblxuICAvKipcbiAgICogV29ya2luZyB3aXRoIHRpcHB5IGluc3RhbmNlIG1ldGhvZHNcbiAgICovXG5cbiAgLyoqXG4gICAqIFByb2dyYW1tYXRpY2FsbHkgc2hvdyB0aGUgdGlwcHlcbiAgICpcbiAgICogQHBhcmFtIG5hbWUgeyBzdHJpbmcgfSBuYW1lIG9mIHRpcHB5IGluc3RhbmNlXG4gICAqL1xuICBzaG93KG5hbWU6IHN0cmluZykge1xuICAgIHRoaXMuY2FsbE5hdGl2ZVRpcHB5TWV0aG9kKG5hbWUsIEluc3RhbmNlQ2hhbmdlUmVhc29uRW51bS5TaG93KTtcbiAgfVxuXG4gIC8qKlxuICAgKiBQcm9ncmFtbWF0aWNhbGx5IGhpZGUgdGhlIHRpcHB5XG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKi9cbiAgaGlkZShuYW1lOiBzdHJpbmcpIHtcbiAgICB0aGlzLmNhbGxOYXRpdmVUaXBweU1ldGhvZChuYW1lLCBJbnN0YW5jZUNoYW5nZVJlYXNvbkVudW0uSGlkZSk7XG4gIH1cblxuICAvKipcbiAgICogV2lsbCBoaWRlIHRoZSB0aXBweSBvbmx5IGlmIHRoZSBjdXJzb3IgaXMgb3V0c2lkZSBvZiB0aGUgdGlwcHkncyBpbnRlcmFjdGl2ZSByZWdpb25cbiAgICogVGhpcyBhbGxvd3MgeW91IHRvIHByb2dyYW1tYXRpY2FsbHkgaG9vayBpbnRvIGludGVyYWN0aXZlIGJlaGF2aW9yIHVwb24gYSBtb3VzZWxlYXZlIGV2ZW50IGlmIGltcGxlbWVudGluZyBjdXN0b20gZXZlbnQgbGlzdGVuZXJzXG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKiBAcGFyYW0gbmFtZSB7IG1vdXNlRXZlbnQgfSBwYXNzIHRoZSBtb3VzZSBldmVudCBvYmplY3QgaW4gZnJvbSB5b3VyIGV2ZW50IGxpc3RlbmVyXG4gICAqL1xuICBoaWRlV2l0aEludGVyYWN0aXZpdHkobmFtZTogc3RyaW5nLCBtb3VzZUV2ZW50OiBNb3VzZUV2ZW50KSB7XG4gICAgdGhpcy5jYWxsTmF0aXZlVGlwcHlNZXRob2QobmFtZSwgSW5zdGFuY2VDaGFuZ2VSZWFzb25FbnVtLkhpZGVXaXRoSW50ZXJhY3Rpdml0eSwgbW91c2VFdmVudCk7XG4gIH1cblxuICAvKipcbiAgICogUHJldmVudCBhIHRpcHB5IGZyb20gc2hvd2luZyBvciBoaWRpbmdcbiAgICpcbiAgICogQHBhcmFtIG5hbWUgeyBzdHJpbmcgfSBuYW1lIG9mIHRpcHB5IGluc3RhbmNlXG4gICAqL1xuICBkaXNhYmxlKG5hbWU6IHN0cmluZykge1xuICAgIHRoaXMuY2FsbE5hdGl2ZVRpcHB5TWV0aG9kKG5hbWUsIEluc3RhbmNlQ2hhbmdlUmVhc29uRW51bS5EaXNhYmxlKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBSZS1lbmFibGUgYSB0aXBweVxuICAgKlxuICAgKiBAcGFyYW0gbmFtZSB7IHN0cmluZyB9IG5hbWUgb2YgdGlwcHkgaW5zdGFuY2VcbiAgICovXG4gIGVuYWJsZShuYW1lOiBzdHJpbmcpIHtcbiAgICB0aGlzLmNhbGxOYXRpdmVUaXBweU1ldGhvZChuYW1lLCBJbnN0YW5jZUNoYW5nZVJlYXNvbkVudW0uRW5hYmxlKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBVcGRhdGUgYW55IHRpcHB5IHByb3BzXG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKiBAcGFyYW0gdGlwcHlQcm9wcyB7IE5neFRpcHB5UHJvcHMgfSBuZXcgcHJvcHNcbiAgICovXG4gIHNldFByb3BzKG5hbWU6IHN0cmluZywgdGlwcHlQcm9wczogTmd4VGlwcHlQcm9wcykge1xuICAgIHRoaXMuY2FsbE5hdGl2ZVRpcHB5TWV0aG9kKG5hbWUsIEluc3RhbmNlQ2hhbmdlUmVhc29uRW51bS5TZXRQcm9wcywgdGlwcHlQcm9wcyk7XG4gIH1cblxuICAvKipcbiAgICogVXBkYXRlIHRoZSBjb250ZW50IGZvciB0aXBweVxuICAgKlxuICAgKiBAcGFyYW0gbmFtZSB7IHN0cmluZyB9IG5hbWUgb2YgdGlwcHkgaW5zdGFuY2VcbiAgICogQHBhcmFtIHRpcHB5Q29udGVudCB7IE5neFRpcHB5Q29udGVudCB9IG5ldyBjb250ZW50XG4gICAqL1xuICBzZXRDb250ZW50KG5hbWU6IHN0cmluZywgdGlwcHlDb250ZW50OiBOZ3hUaXBweUNvbnRlbnQpIHtcbiAgICBjb25zdCBpbnN0YW5jZSA9IHRoaXMuZ2V0SW5zdGFuY2UobmFtZSk7XG5cbiAgICBpZiAoIWluc3RhbmNlKSB7XG4gICAgICB0aGlzLnRocm93RXJyb3JJbnN0YW5jZU5vdEV4aXN0KG5hbWUpO1xuICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIGlmICh0aXBweUNvbnRlbnQpIHtcbiAgICAgIGNvbnN0IHZpZXdSZWYgPSB0aGlzLm5neFZpZXdTZXJ2aWNlLmdldFZpZXdSZWZJbnN0YW5jZSh0aXBweUNvbnRlbnQsIGluc3RhbmNlLnRpcHB5TmFtZSk7XG4gICAgICBjb25zdCBjb250ZW50ID0gdmlld1JlZi5nZXRFbGVtZW50KCk7XG5cbiAgICAgIGlmIChjb250ZW50KSB7XG4gICAgICAgIHNldFRlbXBsYXRlVmlzaWJsZShjb250ZW50LCB0aGlzLnJlbmRlcmVyKTtcbiAgICAgICAgaW5zdGFuY2Uuc2V0Q29udGVudChjb250ZW50KTtcbiAgICAgICAgaW5zdGFuY2Uudmlld1JlZiA9IHZpZXdSZWY7XG4gICAgICAgIHRoaXMuZW1pdEluc3RhbmNlc0NoYW5nZSh7XG4gICAgICAgICAgbmFtZSxcbiAgICAgICAgICByZWFzb246ICdzZXRDb250ZW50JyxcbiAgICAgICAgICBpbnN0YW5jZSxcbiAgICAgICAgfSk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICB0aGlzLnRocm93RXJyb3IoJ21lc3NhZ2UnKTtcbiAgICAgIH1cbiAgICB9XG4gIH1cblxuICAvKipcbiAgICogVW5tb3VudCB0aGUgdGlwcHkgZnJvbSB0aGUgRE9NXG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKi9cbiAgdW5tb3VudChuYW1lOiBzdHJpbmcpIHtcbiAgICB0aGlzLmNhbGxOYXRpdmVUaXBweU1ldGhvZChuYW1lLCBJbnN0YW5jZUNoYW5nZVJlYXNvbkVudW0uVW5tb3VudCk7XG4gIH1cblxuICAvKipcbiAgICogQ2xlYXJzIHRoZSBpbnN0YW5jZXMgZGVsYXkgdGltZW91dHNcbiAgICpcbiAgICogQHBhcmFtIG5hbWUgeyBzdHJpbmcgfSBuYW1lIG9mIHRpcHB5IGluc3RhbmNlXG4gICAqL1xuICBjbGVhckRlbGF5VGltZW91dHMobmFtZTogc3RyaW5nKSB7XG4gICAgdGhpcy5jYWxsTmF0aXZlVGlwcHlNZXRob2QobmFtZSwgSW5zdGFuY2VDaGFuZ2VSZWFzb25FbnVtLkNsZWFyRGVsYXlUaW1lb3V0cyk7XG4gIH1cblxuICAvKipcbiAgICogUGVybWFuZW50bHkgZGVzdHJveSBhbmQgY2xlYW4gdXAgdGhlIHRpcHB5IGluc3RhbmNlXG4gICAqXG4gICAqIEBwYXJhbSBuYW1lIHsgc3RyaW5nIH0gbmFtZSBvZiB0aXBweSBpbnN0YW5jZVxuICAgKi9cbiAgZGVzdHJveShuYW1lOiBzdHJpbmcpIHtcbiAgICB0aGlzLmNhbGxOYXRpdmVUaXBweU1ldGhvZChuYW1lLCBJbnN0YW5jZUNoYW5nZVJlYXNvbkVudW0uRGVzdHJveSk7XG4gICAgdGhpcy50aXBweUluc3RhbmNlcy5kZWxldGUobmFtZSk7XG4gIH1cblxuICAvKiogV29ya2luZyB3aXRoIHRpcHB5IHN0YXRpYyBtZXRob2RzICovXG5cbiAgLyoqXG4gICAqIFNldCB0aGUgZGVmYXVsdCBwcm9wcyBmb3IgZWFjaCBuZXcgdGlwcHkgaW5zdGFuY2VcbiAgICpcbiAgICogQHBhcmFtIHRpcHB5UHJvcHMgeyBOZ3hUaXBweURlZmF1bHRQcm9wcyB9IGRlZmF1bHQgcHJvcHNcbiAgICovXG4gIHNldERlZmF1bHRQcm9wcyh0aXBweVByb3BzOiBOZ3hUaXBweURlZmF1bHRQcm9wcykge1xuICAgIHRpcHB5LnNldERlZmF1bHRQcm9wcyh0aXBweVByb3BzKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBTaG93IGFsbCB0aXBwaWVzXG4gICAqL1xuICBzaG93QWxsKCkge1xuICAgIHRoaXMudGlwcHlJbnN0YW5jZXMuZm9yRWFjaCgoaW5zdGFuY2U6IE5neFRpcHB5SW5zdGFuY2UsIG5hbWU6IHN0cmluZykgPT4ge1xuICAgICAgaW5zdGFuY2Uuc2hvdygpO1xuICAgICAgdGhpcy5lbWl0SW5zdGFuY2VzQ2hhbmdlKHtcbiAgICAgICAgbmFtZSxcbiAgICAgICAgcmVhc29uOiAnc2hvdycsXG4gICAgICAgIGluc3RhbmNlLFxuICAgICAgfSk7XG4gICAgfSk7XG4gIH1cblxuICAvKipcbiAgICogSGlkZSBhbGwgdGlwcGllcyBvciBoaWRlIGFsbCBleGNlcHQgYSBwYXJ0aWN1bGFyIG9uZVxuICAgKiBBZGRpdGlvbmFsIGhpZGUgdGhlbSB3aXRoIGR1cmF0aW9uXG4gICAqXG4gICAqIEBwYXJhbSB7IE5neFRpcHB5SGlkZUFsbE9wdGlvbnMgfSBbb3B0aW9uc10gLSBhZGRpdGlvbmFsIGhpZGluZyBvcHRpb25zXG4gICAqL1xuICBoaWRlQWxsKG9wdGlvbnM/OiBOZ3hUaXBweUhpZGVBbGxPcHRpb25zKSB7XG4gICAgY29uc3QgZXhjbHVkZSA9IHRoaXMuZ2V0SW5zdGFuY2Uob3B0aW9ucz8uZXhjbHVkZU5hbWUgfHwgJycpO1xuICAgIGNvbnN0IGR1cmF0aW9uID0gb3B0aW9ucz8uZHVyYXRpb247XG5cbiAgICBoaWRlQWxsKHsgZHVyYXRpb24sIC4uLmV4Y2x1ZGUgfSk7XG4gIH1cblxuICAvKipcbiAgICogU3Vic2NyaXB0aW9uIHRvIGNoYW5nZSBvZiB0aXBweSBpbnN0YW5jZXNcbiAgICpcbiAgICogQHJldHVybnMgeyBPYnNlcnZhYmxlPEluc3RhbmNlc0NoYW5nZXM+IH0gb2JzZXJ2YWJsZSBvZiB0aXBweSBpbnN0YW5jZXMgY2hhbmdlXG4gICAqL1xuICBnZXQgaW5zdGFuY2VzQ2hhbmdlcygpOiBPYnNlcnZhYmxlPEluc3RhbmNlc0NoYW5nZXM+IHtcbiAgICByZXR1cm4gdGhpcy50aXBweUluc3RhbmNlcyQuYXNPYnNlcnZhYmxlKCk7XG4gIH1cblxuICAvKipcbiAgICogU2VydmljZSBtZXRob2RzXG4gICAqL1xuXG4gIHByaXZhdGUgY2FsbE5hdGl2ZVRpcHB5TWV0aG9kKG5hbWU6IHN0cmluZywgbWV0aG9kOiBhbnksIGFyZz86IGFueSkge1xuICAgIGNvbnN0IGluc3RhbmNlID0gdGhpcy5nZXRJbnN0YW5jZShuYW1lKTtcblxuICAgIGlmIChpbnN0YW5jZSkge1xuICAgICAgaW5zdGFuY2VbbWV0aG9kIGFzIEV4Y2x1ZGU8SW5zdGFuY2VDaGFuZ2VSZWFzb24sICdzZXRJbnN0YW5jZSc+XShhcmcpO1xuXG4gICAgICB0aGlzLmVtaXRJbnN0YW5jZXNDaGFuZ2Uoe1xuICAgICAgICBuYW1lLFxuICAgICAgICByZWFzb246IG1ldGhvZCxcbiAgICAgICAgaW5zdGFuY2UsXG4gICAgICB9KTtcbiAgICB9IGVsc2Uge1xuICAgICAgdGhpcy50aHJvd0Vycm9ySW5zdGFuY2VOb3RFeGlzdChuYW1lKTtcbiAgICB9XG4gIH1cblxuICBwcml2YXRlIGVtaXRJbnN0YW5jZXNDaGFuZ2Uoe1xuICAgIG5hbWUsXG4gICAgcmVhc29uLFxuICAgIGluc3RhbmNlLFxuICB9OiB7XG4gICAgcmVhc29uOiBJbnN0YW5jZUNoYW5nZVJlYXNvbjtcbiAgICBuYW1lOiBzdHJpbmc7XG4gICAgaW5zdGFuY2U6IE5neFRpcHB5SW5zdGFuY2U7XG4gIH0pIHtcbiAgICB0aGlzLnRpcHB5SW5zdGFuY2VzJC5uZXh0KHsgbmFtZSwgcmVhc29uLCBpbnN0YW5jZSB9KTtcbiAgfVxuXG4gIHByaXZhdGUgY3JlYXRlUmVuZGVyZXIocmVuZGVyZXJGYWN0b3J5OiBSZW5kZXJlckZhY3RvcnkyKSB7XG4gICAgdGhpcy5yZW5kZXJlciA9IHJlbmRlcmVyRmFjdG9yeS5jcmVhdGVSZW5kZXJlcihudWxsLCBudWxsKTtcbiAgfVxuXG4gIHByaXZhdGUgdGhyb3dFcnJvckluc3RhbmNlTm90RXhpc3QobmFtZTogc3RyaW5nKSB7XG4gICAgY29uc3QgZXJyTWVzc2FnZSA9IHRoaXMuZ2V0TWVzc2FnZSh7XG4gICAgICByZWFzb246ICdpbnN0YW5jZU5vdEV4aXN0JyxcbiAgICAgIG1lc3NhZ2VGb3I6IE5neFRpcHB5TmFtZXNFbnVtLlRpcHB5TmFtZSxcbiAgICAgIG5hbWUsXG4gICAgfSk7XG4gICAgdGhpcy50aHJvd0Vycm9yKGVyck1lc3NhZ2UpO1xuICB9XG5cbiAgcHJpdmF0ZSB0aHJvd0Vycm9yKG1lc3NhZ2U6IHN0cmluZywgZXJyb3JDb25zdHJ1Y3RvcjogRXJyb3JDb25zdHJ1Y3RvciA9IEVycm9yKSB7XG4gICAgaWYgKHRoaXMuZGV2TW9kZVNlcnZpY2UuaXNEZXZNb2RlKCkpIHRocm93IG5ldyBlcnJvckNvbnN0cnVjdG9yKG1lc3NhZ2UpO1xuICB9XG5cbiAgcHJpdmF0ZSBnZXRNZXNzYWdlKHtcbiAgICByZWFzb24sXG4gICAgbWVzc2FnZUZvcixcbiAgICBuYW1lLFxuICB9OiB7XG4gICAgcmVhc29uOiBOZ3hUaXBweU1lc3NhZ2VzVHlwZXM7XG4gICAgbWVzc2FnZUZvcjogc3RyaW5nO1xuICAgIG5hbWU6IHN0cmluZztcbiAgfSk6IHN0cmluZyB7XG4gICAgcmV0dXJuIHRoaXMubWVzc2FnZXNEaWN0W3JlYXNvbl0ucmVwbGFjZShgIyR7bWVzc2FnZUZvcn1gLCBgJyR7bmFtZX0nYCk7XG4gIH1cbn1cbiJdfQ==