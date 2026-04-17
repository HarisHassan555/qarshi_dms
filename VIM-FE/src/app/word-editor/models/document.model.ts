export type WordBlockType =
  | 'paragraph'
  | 'heading'
  | 'list'
  | 'blockquote'
  | 'table'
  | 'image'
  | 'horizontal-rule'
  | 'page-break';

export interface WordDocument {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  sections: WordSection[];
}

export interface WordSection {
  id: string;
  header: WordHeaderFooter;
  footer: WordHeaderFooter;
  page: PageLayout;
  blocks: WordBlock[];
}

export interface WordHeaderFooter {
  html: string;
}

export interface PageLayout {
  marginTop: number;
  marginRight: number;
  marginBottom: number;
  marginLeft: number;
}

export type WordBlock =
  | ParagraphBlock
  | ListBlock
  | TableBlock
  | ImageBlock
  | RuleBlock
  | PageBreakBlock;

export interface ParagraphBlock {
  id: string;
  type: 'paragraph' | 'heading' | 'blockquote';
  level?: 1 | 2 | 3 | 4 | 5 | 6;
  align?: 'left' | 'center' | 'right' | 'justify';
  lineHeight?: string;
  spacingBefore?: string;
  spacingAfter?: string;
  indentLeft?: string;
  html: string;
  runs: TextRun[];
}

export interface ListBlock {
  id: string;
  type: 'list';
  ordered: boolean;
  items: string[];
  html: string;
}

export interface TextRun {
  text: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  strike?: boolean;
  fontFamily?: string;
  fontSize?: string;
  color?: string;
  highlight?: string;
  superscript?: boolean;
  subscript?: boolean;
}

export interface TableBlock {
  id: string;
  type: 'table';
  rows: TableRow[];
  html: string;
}

export interface TableRow {
  cells: TableCell[];
}

export interface TableCell {
  html: string;
  rowSpan?: number;
  colSpan?: number;
}

export interface ImageBlock {
  id: string;
  type: 'image';
  src: string;
  alt?: string;
  width?: string;
  html: string;
}

export interface RuleBlock {
  id: string;
  type: 'horizontal-rule';
  html: string;
}

export interface PageBreakBlock {
  id: string;
  type: 'page-break';
  html: string;
}

export interface SelectionState {
  bold: boolean;
  italic: boolean;
  underline: boolean;
  strikeThrough: boolean;
  subscript: boolean;
  superscript: boolean;
  orderedList: boolean;
  unorderedList: boolean;
  align: 'left' | 'center' | 'right' | 'justify';
  fontName: string;
  fontSize: string;
  foreColor: string;
  hiliteColor: string;
  formatBlock: string;
}

export interface EditorMetrics {
  words: number;
  characters: number;
}

