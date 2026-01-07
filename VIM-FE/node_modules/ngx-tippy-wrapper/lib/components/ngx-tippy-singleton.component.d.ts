import { AfterViewInit, ElementRef, OnDestroy } from '@angular/core';
import { NgxSingletonProps, NgxTippyMessagesDict } from '../ngx-tippy.interfaces';
import { NgxTippyService } from '../services';
import * as i0 from "@angular/core";
/**
 * Tippy singleton - single tippy element that takes the place of an array of regular tippy instances
 */
export declare class NgxTippySingletonComponent implements AfterViewInit, OnDestroy {
    private platform;
    private ngxTippyService;
    private messagesDict;
    singletonProps?: NgxSingletonProps;
    singletonName?: string;
    contentWrapper: ElementRef;
    private singletonInstance;
    private currentSingletonChildrenTippyInstances;
    constructor(platform: Object, ngxTippyService: NgxTippyService, messagesDict: NgxTippyMessagesDict);
    ngAfterViewInit(): void;
    ngOnDestroy(): void;
    /**
     * Take projected in component tooltips element
     * Take initiated tippy instances
     * Initiate `singleton addon` only for projected tooltips for current component instance
     */
    private setSingleton;
    private initTippySingletonEntry;
    /**
     * To manipulate singleton groups, write all instances to storage
     * `singletonName` used as unique key
     * If `singletonName` does not provided - it will be generated using id of singletonInstance
     *
     * @param tippyInstance { NgxTippySingletonInstance }
     */
    private writeSingletonInstanceToStorage;
    /**
     * Extend original `show` method
     * Purpose: manipulate tooltip state by [tippyName]
     *
     * @param singletonInstance { NgxTippySingletonInstance }
     * @returns { NgxTippySingletonInstance }
     */
    private extendShowFn;
    private clearSingletonInstance;
    private destroySingletonInstance;
    private deleteEntryInStorage;
    static ɵfac: i0.ɵɵFactoryDeclaration<NgxTippySingletonComponent, never>;
    static ɵcmp: i0.ɵɵComponentDeclaration<NgxTippySingletonComponent, "ngx-tippy-singleton", never, { "singletonProps": "singletonProps"; "singletonName": "singletonName"; }, {}, never, ["*"], false>;
}
