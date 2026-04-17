import { Injectable } from '@angular/core';
import { EditorCommand } from '../models/command.model';
import { SelectionState } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class EditorCommandService {
  run(editorEl: HTMLElement, command: EditorCommand): void {
    editorEl.focus();

    switch (command.type) {
      case 'bold':
      case 'italic':
      case 'underline':
      case 'strikeThrough':
      case 'subscript':
      case 'superscript':
      case 'indent':
      case 'outdent':
        this.exec(command.type);
        break;
      case 'orderedList':
        this.exec('insertOrderedList');
        break;
      case 'unorderedList':
        this.exec('insertUnorderedList');
        break;
      case 'clearFormatting':
        this.exec('removeFormat');
        this.exec('unlink');
        break;
      case 'fontName':
        this.exec('fontName', command.payload);
        break;
      case 'fontSize':
        this.applyFontSize(command.payload || '14px');
        break;
      case 'foreColor':
        this.exec('foreColor', command.payload);
        break;
      case 'hiliteColor':
        this.exec('hiliteColor', command.payload);
        break;
      case 'formatBlock':
        this.exec('formatBlock', command.payload || 'p');
        break;
      case 'alignLeft':
        this.exec('justifyLeft');
        break;
      case 'alignCenter':
        this.exec('justifyCenter');
        break;
      case 'alignRight':
        this.exec('justifyRight');
        break;
      case 'alignJustify':
        this.exec('justifyFull');
        break;
      case 'lineHeight':
        this.applyBlockStyle('lineHeight', command.payload || '1.5');
        break;
      case 'spacingBefore':
        this.applyBlockStyle('marginTop', command.payload || '0');
        break;
      case 'spacingAfter':
        this.applyBlockStyle('marginBottom', command.payload || '0');
        break;
      case 'blockQuote':
        this.exec('formatBlock', 'blockquote');
        break;
      case 'insertTable':
        this.insertTable(command.payload?.rows || 3, command.payload?.cols || 3);
        break;
      case 'tableAddRow':
        this.tableAddRow();
        break;
      case 'tableRemoveRow':
        this.tableRemoveRow();
        break;
      case 'tableAddColumn':
        this.tableAddColumn();
        break;
      case 'tableRemoveColumn':
        this.tableRemoveColumn();
        break;
      case 'tableMergeRight':
        this.tableMergeRight();
        break;
      case 'insertImage':
        this.insertImage(command.payload);
        break;
      case 'insertHorizontalRule':
        this.exec('insertHorizontalRule');
        break;
      case 'insertLink':
        this.insertLink(command.payload);
        break;
      case 'insertSpecialChar':
        this.exec('insertText', command.payload || '•');
        break;
      case 'insertPageBreak':
        this.exec('insertHTML', '<hr class="word-page-break" data-page-break="true" />');
        break;
      default:
        break;
    }
  }

  captureSelectionState(): SelectionState {
    const color = this.execValue('foreColor');
    const bg = this.execValue('hiliteColor');
    return {
      bold: this.execState('bold'),
      italic: this.execState('italic'),
      underline: this.execState('underline'),
      strikeThrough: this.execState('strikeThrough'),
      subscript: this.execState('subscript'),
      superscript: this.execState('superscript'),
      orderedList: this.execState('insertOrderedList'),
      unorderedList: this.execState('insertUnorderedList'),
      align: this.resolveAlign(),
      fontName: (this.execValue('fontName') || 'Calibri').replace(/"/g, ''),
      fontSize: this.normalizeSize(this.execValue('fontSize')),
      foreColor: this.normalizeColor(color),
      hiliteColor: this.normalizeColor(bg || '#ffffff'),
      formatBlock: (this.execValue('formatBlock') || 'p').toLowerCase().replace(/[<>]/g, '')
    };
  }

  private insertTable(rows: number, cols: number): void {
    const safeRows = Math.max(1, Math.min(15, rows));
    const safeCols = Math.max(1, Math.min(12, cols));
    let html = '<table class="word-table"><tbody>';
    for (let r = 0; r < safeRows; r++) {
      html += '<tr>';
      for (let c = 0; c < safeCols; c++) {
        html += '<td>&nbsp;</td>';
      }
      html += '</tr>';
    }
    html += '</tbody></table><p><br/></p>';
    this.exec('insertHTML', html);
  }

  private tableAddRow(): void {
    const cell = this.currentCell();
    if (!cell) return;
    const row = cell.parentElement as HTMLTableRowElement;
    const table = row.closest('table');
    if (!table) return;
    const newRow = row.cloneNode(true) as HTMLTableRowElement;
    Array.from(newRow.cells).forEach((c) => (c.innerHTML = '&nbsp;'));
    row.after(newRow);
  }

  private tableRemoveRow(): void {
    const cell = this.currentCell();
    if (!cell) return;
    const row = cell.parentElement as HTMLTableRowElement;
    const table = row.closest('table');
    if (!table) return;
    if (table.querySelectorAll('tr').length <= 1) return;
    row.remove();
  }

  private tableAddColumn(): void {
    const cell = this.currentCell();
    if (!cell) return;
    const row = cell.parentElement as HTMLTableRowElement;
    const index = cell.cellIndex;
    const table = row.closest('table');
    if (!table) return;
    table.querySelectorAll('tr').forEach((tr) => {
      const newCell = (tr as HTMLTableRowElement).insertCell(index + 1);
      newCell.innerHTML = '&nbsp;';
    });
  }

  private tableRemoveColumn(): void {
    const cell = this.currentCell();
    if (!cell) return;
    const index = cell.cellIndex;
    const row = cell.parentElement as HTMLTableRowElement;
    const table = row.closest('table');
    if (!table) return;
    if (row.cells.length <= 1) return;
    table.querySelectorAll('tr').forEach((tr) => {
      const rowEl = tr as HTMLTableRowElement;
      if (rowEl.cells[index]) {
        rowEl.deleteCell(index);
      }
    });
  }

  private tableMergeRight(): void {
    const cell = this.currentCell();
    if (!cell) return;
    const next = cell.nextElementSibling as HTMLTableCellElement | null;
    if (!next) return;
    const colspan = Number(cell.getAttribute('colspan') || '1');
    const nextColspan = Number(next.getAttribute('colspan') || '1');
    cell.setAttribute('colspan', String(colspan + nextColspan));
    cell.innerHTML = `${cell.innerHTML}<br/>${next.innerHTML}`;
    next.remove();
  }

  private insertImage(payload: string | undefined): void {
    const src = (payload || '').trim() || window.prompt('Image URL');
    if (!src) return;
    this.exec('insertHTML', `<img src="${src}" alt="Image" style="max-width:100%;height:auto;" />`);
  }

  private insertLink(payload: string | undefined): void {
    const url = (payload || '').trim() || window.prompt('Link URL');
    if (!url) return;
    this.exec('createLink', url);
  }

  private applyBlockStyle(prop: string, value: string): void {
    const sel = window.getSelection();
    if (!sel?.anchorNode) return;
    const node = sel.anchorNode as Node;
    const el = this.closestBlock(node);
    if (!el) return;
    (el.style as any)[prop] = value;
  }

  private applyFontSize(value: string): void {
    this.exec('fontSize', '7');
    document.querySelectorAll('font[size="7"]').forEach((node) => {
      const span = document.createElement('span');
      span.style.fontSize = value;
      span.innerHTML = (node as HTMLElement).innerHTML;
      node.replaceWith(span);
    });
  }

  private closestBlock(node: Node): HTMLElement | null {
    const startEl = node.nodeType === Node.ELEMENT_NODE ? (node as HTMLElement) : node.parentElement;
    if (!startEl) return null;
    return startEl.closest('p,h1,h2,h3,h4,h5,h6,blockquote,li,div');
  }

  private currentCell(): HTMLTableCellElement | null {
    const sel = window.getSelection();
    if (!sel?.anchorNode) return null;
    const node = sel.anchorNode.nodeType === Node.ELEMENT_NODE
      ? (sel.anchorNode as HTMLElement)
      : sel.anchorNode.parentElement;
    return (node?.closest('td,th') as HTMLTableCellElement) || null;
  }

  private exec(command: string, value?: string): void {
    try {
      document.execCommand(command, false, value);
    } catch {
      // Keep editor resilient even when command is unsupported.
    }
  }

  private execState(command: string): boolean {
    try {
      return document.queryCommandState(command);
    } catch {
      return false;
    }
  }

  private execValue(command: string): string {
    try {
      return String(document.queryCommandValue(command) || '');
    } catch {
      return '';
    }
  }

  private resolveAlign(): SelectionState['align'] {
    if (this.execState('justifyCenter')) return 'center';
    if (this.execState('justifyRight')) return 'right';
    if (this.execState('justifyFull')) return 'justify';
    return 'left';
  }

  private normalizeColor(raw: string): string {
    if (!raw) return '#000000';
    if (raw.startsWith('#')) return raw;
    const rgb = raw.match(/\d+/g);
    if (!rgb || rgb.length < 3) return raw;
    const [r, g, b] = rgb.map((v) => Number(v).toString(16).padStart(2, '0'));
    return `#${r}${g}${b}`;
  }

  private normalizeSize(raw: string): string {
    if (!raw) return '14px';
    if (/^\d+px$/.test(raw)) return raw;
    const sizeMap: Record<string, string> = {
      '1': '10px',
      '2': '13px',
      '3': '16px',
      '4': '18px',
      '5': '24px',
      '6': '32px',
      '7': '48px'
    };
    return sizeMap[raw] || raw;
  }
}

