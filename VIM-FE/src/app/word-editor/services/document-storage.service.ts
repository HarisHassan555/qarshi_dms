import { Injectable } from '@angular/core';
import { DocumentSerializerService } from './document-serializer.service';
import { WordDocument } from '../models/document.model';

export interface StoredWordDocument {
  html: string;
  model: WordDocument;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentStorageService {
  private readonly key = 'word-editor-document-v1';

  constructor(private serializer: DocumentSerializerService) {}

  save(html: string, title = 'Untitled Document'): StoredWordDocument {
    const model = this.serializer.toModel(html, title);
    const payload: StoredWordDocument = {
      html,
      model,
      updatedAt: new Date().toISOString()
    };
    localStorage.setItem(this.key, JSON.stringify(payload));
    return payload;
  }

  load(): StoredWordDocument | null {
    const raw = localStorage.getItem(this.key);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredWordDocument;
    } catch {
      return null;
    }
  }

  exportHtml(html: string, filename = 'document.html'): void {
    this.download(filename, 'text/html', html);
  }

  exportJson(model: WordDocument, filename = 'document.json'): void {
    this.download(filename, 'application/json', JSON.stringify(model, null, 2));
  }

  private download(filename: string, mimeType: string, content: string): void {
    const blob = new Blob([content], { type: `${mimeType};charset=utf-8` });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}

