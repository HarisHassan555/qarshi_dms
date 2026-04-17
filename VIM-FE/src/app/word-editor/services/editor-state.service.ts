import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { PageMargins } from '../models/command.model';
import { EditorMetrics, SelectionState, WordDocument } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class EditorStateService {
  private readonly emptySelection: SelectionState = {
    bold: false,
    italic: false,
    underline: false,
    strikeThrough: false,
    subscript: false,
    superscript: false,
    orderedList: false,
    unorderedList: false,
    align: 'left',
    fontName: 'Calibri',
    fontSize: '14px',
    foreColor: '#000000',
    hiliteColor: '#ffffff',
    formatBlock: 'p'
  };

  private readonly undoStack: string[] = [];
  private readonly redoStack: string[] = [];
  private readonly maxHistory = 120;

  readonly html$ = new BehaviorSubject<string>('<p><br/></p>');
  readonly document$ = new BehaviorSubject<WordDocument | null>(null);
  readonly selection$ = new BehaviorSubject<SelectionState>({ ...this.emptySelection });
  readonly metrics$ = new BehaviorSubject<EditorMetrics>({ words: 0, characters: 0 });
  readonly zoom$ = new BehaviorSubject<number>(100);
  readonly margins$ = new BehaviorSubject<PageMargins>({ top: 96, right: 96, bottom: 96, left: 96 });
  readonly canUndo$ = new BehaviorSubject<boolean>(false);
  readonly canRedo$ = new BehaviorSubject<boolean>(false);

  setHtml(html: string): void {
    this.html$.next(html || '<p><br/></p>');
  }

  setDocument(doc: WordDocument): void {
    this.document$.next(doc);
  }

  setSelection(selection: SelectionState): void {
    this.selection$.next(selection);
  }

  setMetrics(metrics: EditorMetrics): void {
    this.metrics$.next(metrics);
  }

  setZoom(zoom: number): void {
    this.zoom$.next(Math.max(50, Math.min(200, zoom)));
  }

  setMargins(margins: PageMargins): void {
    this.margins$.next({
      top: Math.max(24, margins.top),
      right: Math.max(24, margins.right),
      bottom: Math.max(24, margins.bottom),
      left: Math.max(24, margins.left)
    });
  }

  snapshot(html: string): void {
    if (!html) return;
    const currentTop = this.undoStack[this.undoStack.length - 1];
    if (currentTop === html) return;
    this.undoStack.push(html);
    if (this.undoStack.length > this.maxHistory) {
      this.undoStack.shift();
    }
    this.redoStack.length = 0;
    this.refreshHistoryFlags();
  }

  undo(currentHtml: string): string | null {
    if (!this.undoStack.length) return null;
    const previous = this.undoStack.pop() || null;
    if (previous === null) return null;
    this.redoStack.push(currentHtml);
    this.refreshHistoryFlags();
    return previous;
  }

  redo(currentHtml: string): string | null {
    if (!this.redoStack.length) return null;
    const next = this.redoStack.pop() || null;
    if (next === null) return null;
    this.undoStack.push(currentHtml);
    this.refreshHistoryFlags();
    return next;
  }

  resetHistory(initialHtml: string): void {
    this.undoStack.length = 0;
    this.redoStack.length = 0;
    this.snapshot(initialHtml);
    this.refreshHistoryFlags();
  }

  private refreshHistoryFlags(): void {
    this.canUndo$.next(this.undoStack.length > 0);
    this.canRedo$.next(this.redoStack.length > 0);
  }
}

