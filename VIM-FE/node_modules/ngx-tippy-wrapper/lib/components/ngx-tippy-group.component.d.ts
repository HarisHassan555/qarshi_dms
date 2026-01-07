import { AfterViewInit, ElementRef } from '@angular/core';
import { NgxTippyMessagesDict, NgxTippyProps } from '../ngx-tippy.interfaces';
import * as i0 from "@angular/core";
/**
 * Different tooltip content to many different elements, but only one tippy instance
 */
export declare class NgxTippyGroupComponent implements AfterViewInit {
    private platform;
    private messagesDict;
    groupedProps?: NgxTippyProps;
    contentWrapper: ElementRef;
    constructor(platform: Object, messagesDict: NgxTippyMessagesDict);
    ngAfterViewInit(): void;
    setTooltips(): void;
    initTippy(tooltips: HTMLElement[]): void;
    static ɵfac: i0.ɵɵFactoryDeclaration<NgxTippyGroupComponent, never>;
    static ɵcmp: i0.ɵɵComponentDeclaration<NgxTippyGroupComponent, "ngx-tippy-group", never, { "groupedProps": "groupedProps"; }, {}, never, ["*"], false>;
}
