import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

type EditorMode = 'excel' | 'word';

/** Bump when Word/Grid WASM or embed URL changes so iframes reload. */
const EMBED_BUILD = '3';

type GridcraftHostMessage =
    | { type: 'gridcraft:ready'; version?: string }
    | { type: 'gridcraft:save'; name: string; bytes: ArrayBuffer }
    | { type: 'gridcraft:dirty'; dirty: boolean }
    | { type: 'gridcraft:failed'; error: string }; 

type WordcraftHostMessage =
    | { type: 'wordcraft:ready'; version?: string; hostShell?: boolean; skin?: string }
    | { type: 'wordcraft:save'; name: string; bytes: ArrayBuffer }
    | { type: 'wordcraft:dirty'; dirty: boolean }
    | { type: 'wordcraft:failed'; error: string };

/**
 * Host page for GridCraft (Excel) and WordCraft (Word) WASM builds under /assets/.
 * Word embed is detected inside the iframe (parent window), not only from query params.
 */
@Component({
    selector: 'app-excelpro',
    templateUrl: './excelpro.component.html',
    styleUrls: ['./excelpro.component.css'],
})
export class ExcelproComponent implements OnInit, OnDestroy {
    mode: EditorMode = 'excel';
    gridcraftUrl: SafeResourceUrl | null = null;
    wordcraftUrl: SafeResourceUrl | null = null;
    wordFrameKey = 0;
    loadError: string | null = null;
    wordHostShell: boolean | null = null;
    isDirty = false;

    private gridcraftFrame: HTMLIFrameElement | null = null;
    private wordcraftFrame: HTMLIFrameElement | null = null;
    private pendingGridOpen: { name: string; bytes: ArrayBuffer } | null = null;
    private pendingWordOpen: { name: string; bytes: ArrayBuffer } | null = null;
    private messageListener = (event: MessageEvent) => this.onEditorMessage(event);

    constructor(
        private sanitizer: DomSanitizer,
        private route: ActivatedRoute,
    ) {}

    ngOnInit(): void {
        const stored = localStorage.getItem('excelpro-mode');
        if (stored === 'word' || stored === 'excel') {
            this.mode = stored;
        }

        window.addEventListener('message', this.messageListener);

        const author = this.resolveAuthor();
        const sample = this.route.snapshot.queryParamMap.get('sample');
        const params = new URLSearchParams({ host: 'parent', author, vim: EMBED_BUILD, zoom: '100', lockZoom: '1' });
        if (sample) {
            params.set('sample', sample);
        }

        this.gridcraftUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.assetPage('gridcraft/index.html', params));
        this.wordcraftUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.assetPage('wordcraft/index.html', params));
    }

    ngOnDestroy(): void {
        window.removeEventListener('message', this.messageListener);
    }

    setMode(mode: EditorMode): void {
        if (this.mode === mode) {
            return;
        }
        this.mode = mode;
        localStorage.setItem('excelpro-mode', mode);
        this.loadError = null;
        if (mode === 'word') {
            this.wordFrameKey += 1;
            this.wordHostShell = null;
        }
    }

    onGridcraftLoad(iframe: HTMLIFrameElement): void {
        this.gridcraftFrame = iframe;
        if (this.pendingGridOpen) {
            this.postGridOpen(this.pendingGridOpen.name, this.pendingGridOpen.bytes);
            this.pendingGridOpen = null;
        }
    }

    onWordcraftLoad(iframe: HTMLIFrameElement): void {
        this.wordcraftFrame = iframe;
        this.configureWordcraft(iframe);
        if (this.pendingWordOpen) {
            this.postWordOpen(this.pendingWordOpen.name, this.pendingWordOpen.bytes);
            this.pendingWordOpen = null;
        }
    }

    openWorkbook(name: string, bytes: ArrayBuffer): void {
        if (this.gridcraftFrame?.contentWindow) {
            this.postGridOpen(name, bytes);
        } else {
            this.pendingGridOpen = { name, bytes };
        }
    }

    openDocument(name: string, bytes: ArrayBuffer): void {
        if (this.wordcraftFrame?.contentWindow) {
            this.postWordOpen(name, bytes);
        } else {
            this.pendingWordOpen = { name, bytes };
        }
    }

    @HostListener('window:beforeunload', ['$event'])
    onBeforeUnload(event: BeforeUnloadEvent): void {
        if (this.isDirty) {
            event.preventDefault();
            event.returnValue = '';
        }
    }

    private assetPage(assetHtml: string, params: URLSearchParams): string {
        const baseHref = document.querySelector('base')?.getAttribute('href') ?? '/';
        const url = new URL(`assets/${assetHtml}`, window.location.origin + baseHref);
        params.forEach((value, key) => url.searchParams.set(key, value));
        return url.pathname + url.search;
    }

    private configureWordcraft(iframe: HTMLIFrameElement): void {
        const send = () => {
            iframe.contentWindow?.postMessage(
                {
                    type: 'wordcraft:configure',
                    embed: true,
                    skin: 'vim',
                    zoom: 100,
                    zoomPercent: 100,
                    lockZoom: true,
                    hideZoomControls: true,
                },
                window.location.origin,
            );
        };
        send();
        window.setTimeout(send, 250);
        window.setTimeout(send, 1500);
    }

    private onEditorMessage(event: MessageEvent): void {
        if (event.origin !== window.location.origin) {
            return;
        }
        const data = event.data as GridcraftHostMessage | WordcraftHostMessage | null;
        if (!data || typeof data !== 'object' || !('type' in data)) {
            return;
        }

        switch (data.type) {
            case 'gridcraft:ready':
                this.loadError = null;
                break;
            case 'wordcraft:ready':
                this.loadError = null;
                this.wordHostShell = !!data.hostShell;
                if (this.mode === 'word' && data.hostShell === false) {
                    this.loadError =
                        'Word editor is not in embed mode. Hard-refresh this page (Ctrl+Shift+R) or restart ng serve so the latest WordCraft build loads.';
                }
                break;
            case 'gridcraft:dirty':
            case 'wordcraft:dirty':
                if (this.isMessageForActiveMode(data.type)) {
                    this.isDirty = !!data.dirty;
                }
                break;
            case 'gridcraft:failed':
                if (this.mode === 'excel') {
                    this.loadError = data.error || 'GridCraft failed to start';
                }
                break;
            case 'wordcraft:failed':
                if (this.mode === 'word') {
                    this.loadError = data.error || 'WordCraft failed to start';
                }
                break;
            case 'gridcraft:save':
                this.handleSave(data.name, data.bytes, 'excel');
                break;
            case 'wordcraft:save':
                this.handleSave(data.name, data.bytes, 'word');
                break;
        }
    }

    private isMessageForActiveMode(type: string): boolean {
        return (this.mode === 'excel' && type.startsWith('gridcraft:')) || (this.mode === 'word' && type.startsWith('wordcraft:'));
    }

    private handleSave(name: string, bytes: ArrayBuffer, kind: 'excel' | 'word'): void {
        if ((kind === 'excel' && this.mode !== 'excel') || (kind === 'word' && this.mode !== 'word')) {
            return;
        }
        this.isDirty = false;
        const mime =
            kind === 'excel'
                ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
                : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
        const fallback = kind === 'excel' ? 'Workbook.xlsx' : 'Document.docx';
        const blob = new Blob([bytes], { type: mime });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = name || fallback;
        anchor.click();
        URL.revokeObjectURL(url);
    }

    private postGridOpen(name: string, bytes: ArrayBuffer): void {
        const target = this.gridcraftFrame?.contentWindow;
        if (!target) {
            return;
        }
        target.postMessage({ type: 'gridcraft:open', name, bytes }, window.location.origin, [bytes]);
    }

    private postWordOpen(name: string, bytes: ArrayBuffer): void {
        const target = this.wordcraftFrame?.contentWindow;
        if (!target) {
            return;
        }
        target.postMessage({ type: 'wordcraft:open', name, bytes }, window.location.origin, [bytes]);
    }

    private resolveAuthor(): string {
        try {
            const raw = localStorage.getItem('user');
            if (!raw) {
                return '';
            }
            const user = JSON.parse(raw) as { txtUserName?: string; txtEmail?: string };
            return (user.txtUserName || user.txtEmail || '').trim();
        } catch {
            return '';
        }
    }
}
