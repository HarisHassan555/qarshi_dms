import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { Subscription } from 'rxjs';
import { EditorCommand, PageMargins } from '../../models/command.model';
import { EditorMetrics, SelectionState } from '../../models/document.model';
import { DocumentSerializerService } from '../../services/document-serializer.service';
import { DocumentStorageService } from '../../services/document-storage.service';
import { EditorCommandService } from '../../services/editor-command.service';
import { EditorStateService } from '../../services/editor-state.service';

@Component({
  selector: 'app-word-editor',
  templateUrl: './word-editor.component.html',
  styleUrls: ['./word-editor.component.css']
})
export class WordEditorComponent implements AfterViewInit, OnDestroy {
  @ViewChild('editorSurface', { static: true }) editorSurface!: ElementRef<HTMLElement>;
  @ViewChild('docHeader', { static: true }) docHeader!: ElementRef<HTMLElement>;
  @ViewChild('docFooter', { static: true }) docFooter!: ElementRef<HTMLElement>;

  selection!: SelectionState;
  metrics: EditorMetrics = { words: 0, characters: 0 };
  zoom = 100;
  margins: PageMargins = { top: 96, right: 96, bottom: 96, left: 96 };
  canUndo = false;
  canRedo = false;
  lastSavedAt = '';

  private subs = new Subscription();
  private pendingSnapshot: any = null;

  constructor(
    private state: EditorStateService,
    private commands: EditorCommandService,
    private serializer: DocumentSerializerService,
    private storage: DocumentStorageService
  ) {}

  ngAfterViewInit(): void {
    const initialHtml = '<p>Start typing your document...</p>';
    this.editorSurface.nativeElement.innerHTML = initialHtml;
    this.state.setHtml(initialHtml);
    this.state.setDocument(this.serializer.toModel(initialHtml));
    this.state.resetHistory(initialHtml);
    this.recalcMetrics();
    this.refreshSelection();

    this.subs.add(this.state.selection$.subscribe((v) => (this.selection = v)));
    this.subs.add(this.state.metrics$.subscribe((v) => (this.metrics = v)));
    this.subs.add(this.state.zoom$.subscribe((v) => (this.zoom = v)));
    this.subs.add(this.state.margins$.subscribe((v) => (this.margins = v)));
    this.subs.add(this.state.canUndo$.subscribe((v) => (this.canUndo = v)));
    this.subs.add(this.state.canRedo$.subscribe((v) => (this.canRedo = v)));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
    if (this.pendingSnapshot) {
      clearTimeout(this.pendingSnapshot);
      this.pendingSnapshot = null;
    }
  }

  onInput(): void {
    this.syncModel();
    this.recalcMetrics();
    this.refreshSelection();
    this.queueSnapshot();
  }

  onToolbarCommand(command: EditorCommand): void {
    if (command.type === 'undo') {
      this.undo();
      return;
    }
    if (command.type === 'redo') {
      this.redo();
      return;
    }
    if (command.type === 'save') {
      this.save();
      return;
    }
    if (command.type === 'load') {
      this.load();
      return;
    }
    if (command.type === 'exportHtml') {
      this.storage.exportHtml(this.currentHtml(), 'word-editor-document.html');
      return;
    }
    if (command.type === 'exportJson') {
      const doc = this.serializer.toModel(this.currentHtml(), 'Word Editor Document');
      this.storage.exportJson(doc, 'word-editor-document.json');
      return;
    }
    if (command.type === 'zoom') {
      this.state.setZoom(Number(command.payload || 100));
      return;
    }
    if (command.type === 'setMargins') {
      this.state.setMargins(command.payload as PageMargins);
      return;
    }

    const before = this.currentHtml();
    this.state.snapshot(before);
    this.commands.run(this.editorSurface.nativeElement, command);
    this.syncModel();
    this.recalcMetrics();
    this.refreshSelection();
  }

  @HostListener('document:selectionchange')
  onSelectionChange(): void {
    const editor = this.editorSurface?.nativeElement;
    if (!editor) return;
    const sel = window.getSelection();
    if (!sel?.anchorNode || !editor.contains(sel.anchorNode)) return;
    this.refreshSelection();
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (!event.ctrlKey && !event.metaKey) return;
    const key = event.key.toLowerCase();
    if (key === 'b' || key === 'i' || key === 'u') {
      event.preventDefault();
      const type = key === 'b' ? 'bold' : key === 'i' ? 'italic' : 'underline';
      this.onToolbarCommand({ type });
      return;
    }
    if (key === 'z') {
      event.preventDefault();
      this.undo();
      return;
    }
    if (key === 'y') {
      event.preventDefault();
      this.redo();
    }
  }

  private undo(): void {
    const previous = this.state.undo(this.currentHtml());
    if (previous === null) return;
    this.editorSurface.nativeElement.innerHTML = previous;
    this.syncModel();
    this.recalcMetrics();
    this.refreshSelection();
  }

  private redo(): void {
    const next = this.state.redo(this.currentHtml());
    if (next === null) return;
    this.editorSurface.nativeElement.innerHTML = next;
    this.syncModel();
    this.recalcMetrics();
    this.refreshSelection();
  }

  private save(): void {
    const payload = this.storage.save(this.currentHtml(), 'Word Editor Document');
    this.lastSavedAt = payload.updatedAt;
    this.state.setDocument(payload.model);
  }

  private load(): void {
    const payload = this.storage.load();
    if (!payload) return;
    this.editorSurface.nativeElement.innerHTML = payload.html || '<p><br/></p>';
    this.state.setHtml(payload.html);
    this.state.setDocument(payload.model);
    this.state.resetHistory(payload.html);
    this.recalcMetrics();
    this.refreshSelection();
    this.lastSavedAt = payload.updatedAt;
  }

  private recalcMetrics(): void {
    const text = (this.editorSurface.nativeElement.innerText || '').trim();
    const words = text ? text.split(/\s+/).length : 0;
    const characters = text.replace(/\s/g, '').length;
    this.state.setMetrics({ words, characters });
  }

  private refreshSelection(): void {
    this.state.setSelection(this.commands.captureSelectionState());
  }

  private syncModel(): void {
    const html = this.currentHtml();
    this.state.setHtml(html);
    this.state.setDocument(this.serializer.toModel(html, 'Word Editor Document'));
  }

  private currentHtml(): string {
    return this.editorSurface.nativeElement.innerHTML;
  }

  private queueSnapshot(): void {
    if (this.pendingSnapshot) {
      clearTimeout(this.pendingSnapshot);
    }
    this.pendingSnapshot = setTimeout(() => {
      this.pendingSnapshot = null;
      this.state.snapshot(this.currentHtml());
    }, 250);
  }
}

