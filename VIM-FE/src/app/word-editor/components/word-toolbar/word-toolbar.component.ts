import { Component, EventEmitter, Input, Output } from '@angular/core';
import { EditorCommand, PageMargins } from '../../models/command.model';
import { SelectionState } from '../../models/document.model';

@Component({
  selector: 'app-word-toolbar',
  templateUrl: './word-toolbar.component.html',
  styleUrls: ['./word-toolbar.component.css']
})
export class WordToolbarComponent {
  @Input() selection!: SelectionState;
  @Input() canUndo = false;
  @Input() canRedo = false;
  @Input() zoom = 100;
  @Input() margins: PageMargins = { top: 96, right: 96, bottom: 96, left: 96 };

  @Output() command = new EventEmitter<EditorCommand>();

  readonly fontFamilies = ['Calibri', 'Arial', 'Times New Roman', 'Georgia', 'Verdana', 'Tahoma'];
  readonly fontSizes = ['10px', '12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px'];
  readonly specialChars = ['*', '#', '->', '<-', '(c)', '(R)', 'TM', 'S', 'P', '+/-', 'u', 'infinity'];

  run(type: EditorCommand['type'], payload?: any): void {
    this.command.emit({ type, payload });
  }

  updateMargin(side: keyof PageMargins, value: number): void {
    const numericValue = Number(value);
    const next: PageMargins = {
      top: this.margins.top,
      right: this.margins.right,
      bottom: this.margins.bottom,
      left: this.margins.left
    };

    if (Number.isFinite(numericValue)) {
      next[side] = numericValue;
    }

    this.run('setMargins', next);
  }
}
