import {
  AfterViewInit,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

declare global {
  interface Window {
    $?: any;
    jQuery?: any;
  }
}

@Component({
  selector: 'app-summernote-editor',
  templateUrl: './summernote-editor.component.html',
  styleUrls: ['./summernote-editor.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SummernoteEditorComponent),
      multi: true,
    },
  ],
})
export class SummernoteEditorComponent implements ControlValueAccessor, AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('editorHost') editorHost!: ElementRef<HTMLDivElement>;

  /** Increment when parent modal opens so Summernote initializes on visible DOM. */
  @Input() initToken = 0;
  @Input() height = 280;
  @Input() placeholder = '';

  private value = '';
  private initialized = false;
  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  ngAfterViewInit(): void {
    this.scheduleInit();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initToken'] && !changes['initToken'].firstChange) {
      this.destroyEditor();
      this.scheduleInit();
    }
  }

  ngOnDestroy(): void {
    this.destroyEditor();
  }

  writeValue(value: string | null): void {
    this.value = value || '';
    this.applyCodeToEditor();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(_isDisabled: boolean): void {
    // Summernote lite has no stable disable API; ignore for admin editors.
  }

  private scheduleInit(): void {
    setTimeout(() => this.initEditor(), 80);
  }

  private initEditor(): void {
    const $ = this.getJQuery();
    if (!$ || !$.fn?.summernote || !this.editorHost?.nativeElement) {
      return;
    }
    const el = this.editorHost.nativeElement;
    if ($(el).next('.note-editor').length > 0) {
      this.applyCodeToEditor();
      return;
    }

    $(el).summernote({
      placeholder: this.placeholder,
      height: this.height,
      tabsize: 2,
      disableDragAndDrop: true,
      toolbar: [
        ['style', ['style']],
        ['font', ['bold', 'italic', 'underline', 'clear']],
        ['fontname', ['fontname']],
        ['color', ['color']],
        ['para', ['ul', 'ol', 'paragraph']],
        ['table', ['table']],
        ['insert', ['link', 'picture']],
        ['view', ['fullscreen', 'codeview', 'help']],
      ],
      callbacks: {
        onChange: (contents: string) => {
          this.value = contents || '';
          this.onChange(this.value);
        },
        onBlur: () => this.onTouched(),
      },
    });
    $(el).summernote('code', this.value);
    this.initialized = true;
  }

  private applyCodeToEditor(): void {
    if (!this.initialized && !this.editorHost) {
      return;
    }
    const $ = this.getJQuery();
    if (!$ || !this.editorHost?.nativeElement) {
      return;
    }
    const el = this.editorHost.nativeElement;
    if ($(el).next('.note-editor').length === 0) {
      return;
    }
    const current = $(el).summernote('code');
    if (current !== this.value) {
      $(el).summernote('code', this.value);
    }
  }

  private destroyEditor(): void {
    const $ = this.getJQuery();
    if (!$ || !this.editorHost?.nativeElement) {
      this.initialized = false;
      return;
    }
    try {
      const el = this.editorHost.nativeElement;
      if ($(el).next('.note-editor').length > 0) {
        $(el).summernote('destroy');
      }
    } catch {
      // ignore teardown errors
    }
    this.initialized = false;
  }

  private getJQuery(): any {
    return window.$ || window.jQuery;
  }
}
