import { RendererFactory2 } from '@angular/core';
import { Observable } from 'rxjs';
import { InstancesChanges, NgxTippyContent, NgxTippyDefaultProps, NgxTippyHideAllOptions, NgxTippyInstance, NgxTippyMessagesDict, NgxTippyProps, NgxTippySingletonInstance } from '../ngx-tippy.interfaces';
import { DevModeService } from './dev-mode.service';
import { NgxViewService } from './ngx-view.service';
import * as i0 from "@angular/core";
export declare class NgxTippyService {
    private devModeService;
    private ngxViewService;
    private messagesDict;
    private tippyInstances;
    private tippySingletonEntryInstances;
    private tippyInstances$;
    private renderer;
    constructor(rendererFactory: RendererFactory2, devModeService: DevModeService, ngxViewService: NgxViewService, messagesDict: NgxTippyMessagesDict);
    /**
     * Working with storage
     */
    /**
     * Write tippy instances to storage
     *
     * @param name { string } name of tippy instance
     * @param state { NgxTippyInstance } tippy instance
     */
    setInstance(name: string, state: NgxTippyInstance): void;
    /**
     * Get specific tippy instance
     *
     * @param name { string } name of tippy instance
     * @returns { NgxTippyInstance | null } specific tippy instance or null
     */
    getInstance(name: string): NgxTippyInstance | null;
    /**
     * Get all tippy instances from storage
     *
     * @returns { Map<string, NgxTippyInstance> | null } all tippy instances or null
     */
    getInstances(): Map<string, NgxTippyInstance> | null;
    /**
     * Write singleton instances to storage
     *
     * @param name { string } name of tippy instance
     * @param state { NgxTippyInstance } tippy instance
     */
    setSingletonInstance(name: string, state: NgxTippySingletonInstance): void;
    /**
     * Get specific singleton tippy instance
     *
     * @param name { string } name of singleton tippy instance
     * @returns { NgxTippySingletonInstance | null } specific singleton tippy instance or null
     */
    getSingletonInstance(name: string): NgxTippySingletonInstance | null;
    /**
     * Get all singleton tippy instances
     *
     * @returns { Map<string, NgxTippyInstance> | null } all singleton tippy instances or null
     */
    getSingletonInstances(): Map<string, NgxTippySingletonInstance> | null;
    /**
     * Working with tippy instance methods
     */
    /**
     * Programmatically show the tippy
     *
     * @param name { string } name of tippy instance
     */
    show(name: string): void;
    /**
     * Programmatically hide the tippy
     *
     * @param name { string } name of tippy instance
     */
    hide(name: string): void;
    /**
     * Will hide the tippy only if the cursor is outside of the tippy's interactive region
     * This allows you to programmatically hook into interactive behavior upon a mouseleave event if implementing custom event listeners
     *
     * @param name { string } name of tippy instance
     * @param name { mouseEvent } pass the mouse event object in from your event listener
     */
    hideWithInteractivity(name: string, mouseEvent: MouseEvent): void;
    /**
     * Prevent a tippy from showing or hiding
     *
     * @param name { string } name of tippy instance
     */
    disable(name: string): void;
    /**
     * Re-enable a tippy
     *
     * @param name { string } name of tippy instance
     */
    enable(name: string): void;
    /**
     * Update any tippy props
     *
     * @param name { string } name of tippy instance
     * @param tippyProps { NgxTippyProps } new props
     */
    setProps(name: string, tippyProps: NgxTippyProps): void;
    /**
     * Update the content for tippy
     *
     * @param name { string } name of tippy instance
     * @param tippyContent { NgxTippyContent } new content
     */
    setContent(name: string, tippyContent: NgxTippyContent): void;
    /**
     * Unmount the tippy from the DOM
     *
     * @param name { string } name of tippy instance
     */
    unmount(name: string): void;
    /**
     * Clears the instances delay timeouts
     *
     * @param name { string } name of tippy instance
     */
    clearDelayTimeouts(name: string): void;
    /**
     * Permanently destroy and clean up the tippy instance
     *
     * @param name { string } name of tippy instance
     */
    destroy(name: string): void;
    /** Working with tippy static methods */
    /**
     * Set the default props for each new tippy instance
     *
     * @param tippyProps { NgxTippyDefaultProps } default props
     */
    setDefaultProps(tippyProps: NgxTippyDefaultProps): void;
    /**
     * Show all tippies
     */
    showAll(): void;
    /**
     * Hide all tippies or hide all except a particular one
     * Additional hide them with duration
     *
     * @param { NgxTippyHideAllOptions } [options] - additional hiding options
     */
    hideAll(options?: NgxTippyHideAllOptions): void;
    /**
     * Subscription to change of tippy instances
     *
     * @returns { Observable<InstancesChanges> } observable of tippy instances change
     */
    get instancesChanges(): Observable<InstancesChanges>;
    /**
     * Service methods
     */
    private callNativeTippyMethod;
    private emitInstancesChange;
    private createRenderer;
    private throwErrorInstanceNotExist;
    private throwError;
    private getMessage;
    static ɵfac: i0.ɵɵFactoryDeclaration<NgxTippyService, never>;
    static ɵprov: i0.ɵɵInjectableDeclaration<NgxTippyService>;
}
