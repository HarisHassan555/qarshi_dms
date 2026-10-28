import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { urls } from 'src/app/utils/urls';

export interface EmailLayout {
  id?: number;
  layoutCode: string;
  layoutName: string;
  description?: string;
  htmlWrapper: string;
  active?: boolean;
}

export interface EmailTemplate {
  id?: number;
  templateCode: string;
  templateName: string;
  description?: string;
  subject: string;
  bodyHtml: string;
  layoutId?: number | null;
  layoutName?: string;
  placeholderHelp?: string;
  category?: string;
  sortOrder?: number;
  active?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class EmailTemplateService {
  constructor(private http: HttpClient) {}

  getLayouts(): Observable<EmailLayout[]> {
    return this.http.get<EmailLayout[]>(urls.API_URL + 'getAllEmailLayouts');
  }

  saveLayout(payload: EmailLayout): Observable<EmailLayout> {
    const body = {
      id: payload.id,
      layoutCode: payload.layoutCode,
      layoutName: payload.layoutName,
      description: payload.description,
      htmlWrapper: payload.htmlWrapper,
      active: payload.active !== false,
    };
    if (payload.id) {
      return this.http.post<EmailLayout>(urls.API_URL + 'updateEmailLayout', body);
    }
    return this.http.post<EmailLayout>(urls.API_URL + 'addEmailLayout', body);
  }

  deleteLayouts(ids: number[]): Observable<string> {
    const csv = (ids || []).filter((id) => id > 0).join(',');
    return this.http.post(urls.API_URL + 'deleteEmailLayout', JSON.stringify(csv), { responseType: 'text' });
  }

  getTemplates(category?: string): Observable<EmailTemplate[]> {
    const q = category ? `?category=${encodeURIComponent(category)}` : '';
    return this.http.get<EmailTemplate[]>(urls.API_URL + 'getAllEmailTemplates' + q);
  }

  getTemplate(id: number): Observable<EmailTemplate> {
    return this.http.get<EmailTemplate>(urls.API_URL + 'getEmailTemplate', {
      params: { id: String(id) },
    });
  }

  saveTemplate(payload: EmailTemplate): Observable<EmailTemplate> {
    const body = {
      id: payload.id,
      templateCode: payload.templateCode,
      templateName: payload.templateName,
      description: payload.description,
      subject: payload.subject,
      bodyHtml: payload.bodyHtml,
      layoutId: payload.layoutId && payload.layoutId > 0 ? payload.layoutId : null,
      placeholderHelp: payload.placeholderHelp,
      category: payload.category,
      sortOrder: payload.sortOrder ?? 100,
      active: payload.active !== false,
    };
    if (payload.id) {
      return this.http.post<EmailTemplate>(urls.API_URL + 'updateEmailTemplate', body);
    }
    return this.http.post<EmailTemplate>(urls.API_URL + 'addEmailTemplate', body);
  }

  deleteTemplates(ids: number[]): Observable<string> {
    const csv = (ids || []).filter((id) => id > 0).join(',');
    return this.http.post(urls.API_URL + 'deleteEmailTemplate', JSON.stringify(csv), { responseType: 'text' });
  }
}
