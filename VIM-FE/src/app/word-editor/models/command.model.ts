export type EditorCommandType =
  | 'undo'
  | 'redo'
  | 'bold'
  | 'italic'
  | 'underline'
  | 'strikeThrough'
  | 'superscript'
  | 'subscript'
  | 'clearFormatting'
  | 'fontName'
  | 'fontSize'
  | 'foreColor'
  | 'hiliteColor'
  | 'formatBlock'
  | 'alignLeft'
  | 'alignCenter'
  | 'alignRight'
  | 'alignJustify'
  | 'lineHeight'
  | 'spacingBefore'
  | 'spacingAfter'
  | 'indent'
  | 'outdent'
  | 'orderedList'
  | 'unorderedList'
  | 'blockQuote'
  | 'insertTable'
  | 'tableAddRow'
  | 'tableRemoveRow'
  | 'tableAddColumn'
  | 'tableRemoveColumn'
  | 'tableMergeRight'
  | 'insertImage'
  | 'insertHorizontalRule'
  | 'insertLink'
  | 'insertSpecialChar'
  | 'insertPageBreak'
  | 'save'
  | 'load'
  | 'exportHtml'
  | 'exportJson'
  | 'zoom'
  | 'setMargins';

export interface EditorCommand {
  type: EditorCommandType;
  payload?: any;
}

export interface PageMargins {
  top: number;
  right: number;
  bottom: number;
  left: number;
}

