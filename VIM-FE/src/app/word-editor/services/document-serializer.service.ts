import { Injectable } from '@angular/core';
import {
  ImageBlock,
  ListBlock,
  PageBreakBlock,
  ParagraphBlock,
  RuleBlock,
  TableBlock,
  TableCell,
  TableRow,
  TextRun,
  WordBlock,
  WordDocument
} from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentSerializerService {
  toModel(html: string, title = 'Untitled Document'): WordDocument {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = html || '<p><br/></p>';
    const blocks: WordBlock[] = Array.from(wrapper.children)
      .map((node) => this.nodeToBlock(node as HTMLElement))
      .filter((block): block is WordBlock => !!block);

    const now = new Date().toISOString();
    return {
      id: 'doc-' + Date.now(),
      title,
      createdAt: now,
      updatedAt: now,
      sections: [
        {
          id: 'section-1',
          header: { html: '' },
          footer: { html: '' },
          page: { marginTop: 96, marginRight: 96, marginBottom: 96, marginLeft: 96 },
          blocks
        }
      ]
    };
  }

  toHtml(model: WordDocument): string {
    if (!model?.sections?.length) return '<p><br/></p>';
    return model.sections
      .flatMap((section) => section.blocks)
      .map((block) => block.html)
      .join('');
  }

  private nodeToBlock(node: HTMLElement): WordBlock | null {
    const tag = node.tagName.toLowerCase();
    const html = node.outerHTML;

    if (tag === 'p' || tag.startsWith('h') || tag === 'blockquote') {
      const level = /^h[1-6]$/.test(tag) ? Number(tag[1]) as 1 | 2 | 3 | 4 | 5 | 6 : undefined;
      const block: ParagraphBlock = {
        id: this.id('p'),
        type: tag === 'blockquote' ? 'blockquote' : level ? 'heading' : 'paragraph',
        level,
        align: (node.style.textAlign || 'left') as any,
        lineHeight: node.style.lineHeight || '',
        spacingBefore: node.style.marginTop || '',
        spacingAfter: node.style.marginBottom || '',
        indentLeft: node.style.paddingLeft || '',
        html,
        runs: this.extractRuns(node)
      };
      return block;
    }

    if (tag === 'ul' || tag === 'ol') {
      const list: ListBlock = {
        id: this.id('list'),
        type: 'list',
        ordered: tag === 'ol',
        items: Array.from(node.querySelectorAll('li')).map((li) => li.textContent || ''),
        html
      };
      return list;
    }

    if (tag === 'table') {
      const rows: TableRow[] = Array.from(node.querySelectorAll('tr')).map((tr) => ({
        cells: Array.from(tr.children).map((cell) => {
          const el = cell as HTMLTableCellElement;
          const tableCell: TableCell = {
            html: el.innerHTML,
            rowSpan: el.rowSpan || undefined,
            colSpan: el.colSpan || undefined
          };
          return tableCell;
        })
      }));
      const table: TableBlock = {
        id: this.id('tbl'),
        type: 'table',
        rows,
        html
      };
      return table;
    }

    if (tag === 'img') {
      const image: ImageBlock = {
        id: this.id('img'),
        type: 'image',
        src: node.getAttribute('src') || '',
        alt: node.getAttribute('alt') || '',
        width: node.getAttribute('width') || '',
        html
      };
      return image;
    }

    if (tag === 'hr' && node.classList.contains('word-page-break')) {
      const pageBreak: PageBreakBlock = { id: this.id('pb'), type: 'page-break', html };
      return pageBreak;
    }

    if (tag === 'hr') {
      const rule: RuleBlock = { id: this.id('hr'), type: 'horizontal-rule', html };
      return rule;
    }

    if (node.textContent?.trim()) {
      const block: ParagraphBlock = {
        id: this.id('p'),
        type: 'paragraph',
        align: 'left',
        html: `<p>${node.textContent}</p>`,
        runs: [{ text: node.textContent }]
      };
      return block;
    }

    return null;
  }

  private extractRuns(root: HTMLElement): TextRun[] {
    const runs: TextRun[] = [];
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    let textNode = walker.nextNode();
    while (textNode) {
      const text = textNode.textContent || '';
      if (text.trim()) {
        const parent = textNode.parentElement;
        runs.push({
          text,
          bold: this.hasTag(parent, 'b', 'strong'),
          italic: this.hasTag(parent, 'i', 'em'),
          underline: this.hasTag(parent, 'u'),
          strike: this.hasTag(parent, 's', 'strike'),
          superscript: this.hasTag(parent, 'sup'),
          subscript: this.hasTag(parent, 'sub'),
          fontFamily: parent?.style.fontFamily || '',
          fontSize: parent?.style.fontSize || '',
          color: parent?.style.color || '',
          highlight: parent?.style.backgroundColor || ''
        });
      }
      textNode = walker.nextNode();
    }
    return runs;
  }

  private hasTag(node: HTMLElement | null, ...tags: string[]): boolean {
    let current = node;
    while (current) {
      if (tags.includes(current.tagName.toLowerCase())) return true;
      current = current.parentElement;
    }
    return false;
  }

  private id(prefix: string): string {
    return `${prefix}-${Math.random().toString(36).slice(2, 10)}`;
  }
}

