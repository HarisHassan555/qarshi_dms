import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
// @ts-ignore
import html2pdf from 'html2pdf.js';

@Injectable({
  providedIn: 'root'
})
export class ApplicationPdfService {
  buildPdfHtmlForApplication(
    data: any,
    form: any,
    formFields: any[],
    applicationFormData: any,
    applicationMeta?: { formName?: string; txtFormCode?: string }
  ): string {
    let isCapf = this.isCapfFormMeta(data, form, applicationMeta);

    let htmlContent = '';
    let handledBudgetApproval = false;
    const resolvedFormName = (form?.txtFormName || data?.cfgTblCustomForm?.txtFormName || applicationMeta?.formName || '').trim();
    const resolvedFormCode = (data?.txtFormCode || applicationMeta?.txtFormCode || '').trim();
    const normalizedFormName = resolvedFormName.replace(/\s+/g, ' ').toUpperCase();
    const normalizedFormCode = resolvedFormCode.toUpperCase();
    const initialIsBudgetApproval = this.isBudgetApprovalFormMeta(applicationMeta);
    const forceBudgetApprovalByCode = (applicationMeta?.txtFormCode || '').toUpperCase().startsWith('BDG');

    const isBudgetApproval =
      normalizedFormName === 'BUDGET APPROVAL FORM' ||
      normalizedFormName.includes('BUDGET APPROVAL') ||
      normalizedFormCode.startsWith('BDG') ||
      normalizedFormCode.includes('BDG-') ||
      normalizedFormCode.includes('BAF') ||
      initialIsBudgetApproval ||
      forceBudgetApprovalByCode;

    if (forceBudgetApprovalByCode) {
      handledBudgetApproval = true;
      htmlContent = this.generateBudgetApprovalPdfHtml(data, formFields, applicationFormData, resolvedFormName || 'Budget Approval');
    }

    if (!handledBudgetApproval && isBudgetApproval) {
      handledBudgetApproval = true;
      htmlContent = this.generateBudgetApprovalPdfHtml(data, formFields, applicationFormData, resolvedFormName || 'Budget Approval');
    } else if (!handledBudgetApproval && isCapf) {
      let pipelines: any[] = [];
      if (form && form.cfgTblFormApprovalPipelines) {
        pipelines = form.cfgTblFormApprovalPipelines;
      } else if (data?.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblFormApprovalPipelines) {
        pipelines = data.cfgTblCustomForm.cfgTblFormApprovalPipelines;
      }
      if (pipelines) {
        pipelines.sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0));
      } else {
        pipelines = [];
      }

      htmlContent = this.generateCapfAbcHtml(data, formFields, applicationFormData, pipelines);
      if (!htmlContent || !htmlContent.includes('abc-wrapper')) {
        throw new Error('ABC HTML generation failed');
      }
    } else if (!handledBudgetApproval) {
      let pipelines: any[] = [];
      if (form && form.cfgTblFormApprovalPipelines) {
        pipelines = form.cfgTblFormApprovalPipelines;
      } else if (data?.cfgTblCustomForm && data.cfgTblCustomForm.cfgTblFormApprovalPipelines) {
        pipelines = data.cfgTblCustomForm.cfgTblFormApprovalPipelines;
      }
      if (pipelines) {
        pipelines.sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0));
      } else {
        pipelines = [];
      }

      htmlContent = this.generatePDFContent(data, resolvedFormName || 'Unknown Form', formFields, applicationFormData, pipelines);
    }

    return htmlContent;
  }

  renderHtmlToPdfBlob(htmlContent: string, filename: string): Promise<Blob> {
    return new Promise((resolve, reject) => {
      let done = false;

      const cleanup = (iframe: HTMLIFrameElement) => {
        if (document.body.contains(iframe)) {
          document.body.removeChild(iframe);
        }
      };

      const existingIframes = document.querySelectorAll('iframe[data-pdf-generator]');
      existingIframes.forEach((iframe: Element) => {
        if (iframe.parentNode) {
          iframe.parentNode.removeChild(iframe);
        }
      });

      const iframe = document.createElement('iframe');
      iframe.setAttribute('data-pdf-generator', 'true');
      iframe.style.position = 'absolute';
      iframe.style.left = '-9999px';
      iframe.style.top = '0';
      iframe.style.width = '900px';
      iframe.style.height = '1200px';
      iframe.style.border = 'none';
      iframe.style.overflow = 'hidden';
      document.body.appendChild(iframe);

      const iframeDoc = iframe.contentDocument || (iframe.contentWindow as any)?.document;
      if (!iframeDoc) {
        cleanup(iframe);
        reject(new Error('Could not access iframe document'));
        return;
      }

      iframeDoc.open();
      iframeDoc.write(htmlContent);
      iframeDoc.close();

      const generatePdf = async () => {
        if (done) return;
        try {
          const element = (iframeDoc.querySelector('.abc-wrapper') || iframeDoc.querySelector('.xyz-paper') || iframeDoc.body) as HTMLElement;
          if (!element) {
            done = true;
            cleanup(iframe);
            reject(new Error('PDF content element not found'));
            return;
          }

          if (element.classList.contains('abc-wrapper')) {
            try {
              const pdfBlob = await this.renderCapfPdfFromElement(element);
              if (done) return;
              done = true;
              cleanup(iframe);
              resolve(pdfBlob);
              return;
            } catch (capfError) {
              if (done) return;
              done = true;
              cleanup(iframe);
              reject(capfError);
              return;
            }
          }

          element.offsetHeight;
          const opt = {
            margin: [2, 5, 2, 5] as [number, number, number, number],
            filename,
            image: { type: 'jpeg' as const, quality: 0.98 },
            html2canvas: {
              scale: 1.5,
              useCORS: true,
              logging: false,
              letterRendering: true,
              allowTaint: true,
              backgroundColor: '#ffffff',
              windowWidth: element.scrollWidth || 900,
              windowHeight: element.scrollHeight || 1200,
              width: element.scrollWidth || 900,
              height: element.scrollHeight || 1200,
              onclone: (clonedDoc: Document) => {
                const clonedElement = (clonedDoc.querySelector('.abc-wrapper') || clonedDoc.querySelector('.xyz-paper') || clonedDoc.body) as HTMLElement;
                if (clonedElement) {
                  clonedElement.offsetHeight;
                }
              }
            },
            jsPDF: {
              unit: 'mm' as const,
              format: 'a4' as const,
              orientation: 'portrait' as const
            }
          };

          html2pdf().set(opt).from(element).outputPdf('blob').then((pdfBlob: Blob) => {
            if (done) return;
            done = true;
            cleanup(iframe);
            resolve(pdfBlob);
          }).catch((error: any) => {
            if (done) return;
            done = true;
            cleanup(iframe);
            reject(error);
          });
        } catch (error) {
          if (done) return;
          done = true;
          cleanup(iframe);
          reject(error);
        }
      };

      iframe.onload = () => {
        setTimeout(generatePdf, 1000);
      };

      setTimeout(() => {
        if (!done) {
          generatePdf();
        }
      }, 1500);
    });
  }

  private async renderCapfPdfFromElement(element: HTMLElement): Promise<Blob> {
    const captureTarget = (element.querySelector('.page') as HTMLElement) || element;
    const hadPdfFix = element.classList.contains('pdf-fix');
    if (!hadPdfFix) {
      element.classList.add('pdf-fix');
    }

    const emptyLineSnapshots: { el: HTMLElement; html: string }[] = [];
    const boxcheckSnapshots: { el: HTMLElement; transform: string }[] = [];
    const boxcheckSpanSnapshots: { el: HTMLElement; transform: string }[] = [];

    element.querySelectorAll('.line, .date-line, .inline-line').forEach((el) => {
      const ht = el as HTMLElement;
      if ((ht.textContent || '').trim() === '') {
        emptyLineSnapshots.push({ el: ht, html: ht.innerHTML });
        ht.innerHTML = '<span class="pdf-empty">&nbsp;</span>';
      }
    });

    element.querySelectorAll('.boxcheck').forEach((el) => {
      const ht = el as HTMLElement;
      boxcheckSnapshots.push({ el: ht, transform: ht.style.transform });
      ht.style.transform = 'translateY(6px)';
    });
    element.querySelectorAll('.boxcheck > span').forEach((el) => {
      const ht = el as HTMLElement;
      boxcheckSpanSnapshots.push({ el: ht, transform: ht.style.transform });
      ht.style.transform = 'translateY(-6px)';
    });

    await new Promise(resolve => setTimeout(resolve, 100));

    const [html2canvasModule, jsPDFModule] = await Promise.all([
      import('html2canvas'),
      import('jspdf')
    ]);

    const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
    const jsPDF = (jsPDFModule.default || jsPDFModule) as any;

    const rect = captureTarget.getBoundingClientRect();
    const contentWidthPx = rect.width || captureTarget.scrollWidth;
    const contentHeightPx = rect.height || captureTarget.scrollHeight;
    const pxToMm = (px: number) => (px * 25.4) / 96;
    const contentWidthMm = pxToMm(contentWidthPx);
    const contentHeightMm = pxToMm(contentHeightPx);

    const canvas = await html2canvas(captureTarget, {
      scale: 3,
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff',
      width: captureTarget.scrollWidth,
      height: captureTarget.scrollHeight,
      windowWidth: captureTarget.scrollWidth,
      windowHeight: captureTarget.scrollHeight
    });

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
      compress: true
    });

    const PDF_WIDTH = 210;
    const PDF_HEIGHT = 297;
    const marginX = 0;
    const marginY = 0;
    const availableWidth = PDF_WIDTH - marginX * 2;
    const availableHeight = PDF_HEIGHT - marginY * 2;
    const scaleByWidth = availableWidth / contentWidthMm;
    const scaleByHeight = availableHeight / contentHeightMm;
    const finalScale = contentHeightMm * scaleByWidth <= availableHeight ? scaleByWidth : scaleByHeight;
    const imgWidth = contentWidthMm * finalScale;
    const imgHeight = contentHeightMm * finalScale;
    const xOffset = (PDF_WIDTH - imgWidth) / 2;
    const yOffset = (PDF_HEIGHT - imgHeight) / 2;

    const imgData = canvas.toDataURL('image/jpeg', 0.98);
    pdf.addImage(imgData, 'JPEG', xOffset, yOffset, imgWidth, imgHeight);

    if (!hadPdfFix) {
      element.classList.remove('pdf-fix');
    }
    emptyLineSnapshots.forEach(({ el, html }) => {
      el.innerHTML = html;
    });
    boxcheckSnapshots.forEach(({ el, transform }) => {
      el.style.transform = transform;
    });
    boxcheckSpanSnapshots.forEach(({ el, transform }) => {
      el.style.transform = transform;
    });

    return pdf.output('blob');
  }

  private isBudgetApprovalFormMeta(applicationMeta?: { formName?: string; txtFormCode?: string }): boolean {
    const name = (applicationMeta?.formName || '').replace(/\s+/g, ' ').toUpperCase();
    const code = (applicationMeta?.txtFormCode || '').toUpperCase();
    return name === 'BUDGET APPROVAL FORM' || name.includes('BUDGET APPROVAL') || code.startsWith('BDG');
  }

  private isCapfFormMeta(data: any, form: any, applicationMeta?: { formName?: string; txtFormCode?: string }): boolean {
    if (data?.txtFormCode && data.txtFormCode.trim().toUpperCase().startsWith('CAPF')) {
      return true;
    }
    if (form && form.txtFormName && form.txtFormName.trim().toUpperCase() === 'CAPF FORM') {
      return true;
    }
    if (data?.cfgTblCustomForm && data.cfgTblCustomForm.txtFormName && data.cfgTblCustomForm.txtFormName.trim().toUpperCase() === 'CAPF FORM') {
      return true;
    }
    const name = (applicationMeta?.formName || '').trim().toUpperCase();
    const code = (applicationMeta?.txtFormCode || '').trim().toUpperCase();
    return name === 'CAPF FORM' || code.startsWith('CAPF');
  }

  private generatePDFContent(application: any, formName: string, formFields: any[], applicationFormData: any, pipelines: any[]): string {
    const formatDate = (dateStr: string | number) => {
      if (!dateStr) return '-';
      const date = new Date(dateStr);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    const formatFieldValue = (field: any, value: any): string => {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      if (field.type === 'checkbox') {
        return value ? 'Yes' : 'No';
      }
      if (field.type === 'date' && value) {
        try {
          const date = new Date(value);
          return date.toLocaleDateString();
        } catch (e) {
          return value;
        }
      }
      return String(value);
    };

    const getFieldValue = (field: any): any => {
      const fieldName = field.label.toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');

      if (applicationFormData[fieldName] !== undefined) {
        return applicationFormData[fieldName];
      } else if (applicationFormData[field.label] !== undefined) {
        return applicationFormData[field.label];
      } else {
        const fieldId = `field_${field.serFieldId}`;
        if (applicationFormData[fieldId] !== undefined) {
          return applicationFormData[fieldId];
        }
      }
      return null;
    };

    const isDepartmentApproved = (pipelineOrder: number): boolean => {
      const currentLevel = application.intCurrentApprovalLevel || 0;
      return currentLevel >= pipelineOrder;
    };

    const escapeHtml = (text: string): string => {
      if (!text) return '';
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    };

    let html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      font-size: 11px;
      line-height: 1.6;
      color: #2c3e50;
      padding: 25px;
      background-color: #ffffff;
    }
    .header {
      text-align: center;
      margin-bottom: 30px;
      padding-bottom: 20px;
      border-bottom: 4px solid #3498db;
    }
    .header h1 {
      color: #2c3e50;
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 5px;
      border: none;
      padding: 0;
    }
    .header .subtitle {
      color: #7f8c8d;
      font-size: 12px;
      font-weight: normal;
    }
    .section {
      margin-bottom: 30px;
      page-break-inside: avoid;
    }
    .section-title {
      color: #34495e;
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 15px;
      padding-bottom: 8px;
      border-bottom: 2px solid #ecf0f1;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .info-grid {
      display: grid;
      grid-template-columns: 180px 1fr;
      gap: 12px 20px;
      margin-bottom: 15px;
    }
    .info-label {
      font-weight: 600;
      color: #555;
      font-size: 11px;
    }
    .info-value {
      color: #2c3e50;
      font-size: 11px;
      word-wrap: break-word;
    }
    .badge {
      display: inline-block;
      padding: 5px 12px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .badge-success {
      background-color: #27ae60;
      color: white;
    }
    .badge-warning {
      background-color: #f39c12;
      color: white;
    }
    .badge-danger {
      background-color: #e74c3c;
      color: white;
    }
    .badge-info {
      background-color: #3498db;
      color: white;
    }
    .badge-secondary {
      background-color: #95a5a6;
      color: white;
    }
    .data-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 10px;
      box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .data-table thead {
      background: linear-gradient(to bottom, #f8f9fa, #e9ecef);
    }
    .data-table th {
      padding: 12px 15px;
      text-align: left;
      font-weight: 600;
      font-size: 11px;
      color: #2c3e50;
      border-bottom: 2px solid #dee2e6;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .data-table td {
      padding: 12px 15px;
      border-bottom: 1px solid #e9ecef;
      font-size: 11px;
      color: #495057;
    }
    .data-table tbody tr:hover {
      background-color: #f8f9fa;
    }
    .data-table tbody tr:last-child td {
      border-bottom: none;
    }
    .pipeline-section {
      margin-top: 20px;
    }
    .pipeline-flow {
      display: flex;
      align-items: center;
      justify-content: flex-start;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 15px;
    }
    .pipeline-card {
      border: 2px solid #ddd;
      border-radius: 8px;
      padding: 15px;
      min-width: 160px;
      text-align: center;
      background-color: #ffffff;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .pipeline-card.approved {
      border-color: #27ae60;
      background: linear-gradient(to bottom, #d5f4e6, #ffffff);
    }
    .pipeline-card.pending {
      border-color: #f39c12;
      background: linear-gradient(to bottom, #fef5e7, #ffffff);
    }
    .pipeline-card.not-started {
      border-color: #bdc3c7;
      background: linear-gradient(to bottom, #ecf0f1, #ffffff);
    }
    .pipeline-card .dept-name {
      font-weight: 700;
      font-size: 12px;
      margin-bottom: 8px;
      color: #2c3e50;
    }
    .pipeline-card .level {
      font-size: 10px;
      color: #7f8c8d;
      margin-bottom: 8px;
    }
    .pipeline-card .status {
      font-size: 11px;
      font-weight: 600;
      margin-bottom: 10px;
      padding: 4px 8px;
      border-radius: 4px;
      display: inline-block;
    }
    .pipeline-card.approved .status {
      background-color: #27ae60;
      color: white;
    }
    .pipeline-card.pending .status {
      background-color: #f39c12;
      color: white;
    }
    .pipeline-card.not-started .status {
      background-color: #bdc3c7;
      color: #2c3e50;
    }
    .pipeline-card .remarks {
      font-size: 9px;
      margin-top: 10px;
      padding-top: 10px;
      border-top: 1px solid #ddd;
      text-align: left;
      color: #555;
      font-style: italic;
    }
    .arrow {
      font-size: 24px;
      color: #95a5a6;
      font-weight: bold;
    }
    .footer {
      margin-top: 40px;
      padding-top: 20px;
      border-top: 2px solid #ecf0f1;
      text-align: center;
      color: #95a5a6;
      font-size: 9px;
    }
    .remarks-box {
      background-color: #f8f9fa;
      border-left: 4px solid #3498db;
      padding: 12px 15px;
      margin-top: 10px;
      border-radius: 4px;
      font-size: 11px;
      color: #495057;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Application Details</h1>
    <div class="subtitle">Application Code: ${escapeHtml(application.txtFormCode || 'N/A')}</div>
  </div>

  <div class="section">
    <h2 class="section-title">Application Information</h2>
    <div class="info-grid">
      <div class="info-label">Application Code:</div>
      <div class="info-value"><strong>${escapeHtml(application.txtFormCode || '-')}</strong></div>
      
      <div class="info-label">Form Name:</div>
      <div class="info-value">${escapeHtml(formName)}</div>
      
      <div class="info-label">Status:</div>
      <div class="info-value">
        <span class="badge badge-${application.txtStatus === 'APPROVED' ? 'success' : application.txtStatus === 'REJECTED' ? 'danger' : application.txtStatus === 'PENDING' ? 'warning' : 'secondary'}">
          ${escapeHtml(application.txtStatus || 'N/A')}
        </span>
      </div>
      
      <div class="info-label">Approval Level:</div>
      <div class="info-value">
        <span class="badge badge-info">Level ${application.intCurrentApprovalLevel || 0}</span>
      </div>
      
      <div class="info-label">Submitted Date:</div>
      <div class="info-value">${formatDate(application.dteCreatedDate)}</div>
    </div>
    ${application.txtRemarks ? `
    <div class="remarks-box">
      <strong>Remarks:</strong> ${escapeHtml(application.txtRemarks)}
    </div>
    ` : ''}
  </div>

  ${formFields.length > 0 ? `
  <div class="section">
    <h2 class="section-title">Form Data</h2>
    <table class="data-table">
      <thead>
        <tr>
          <th style="width: 40%;">Field Name</th>
          <th style="width: 60%;">Value</th>
        </tr>
      </thead>
      <tbody>
        ${formFields.map(field => `
          <tr>
            <td><strong>${escapeHtml(field.label)}${field.required ? ' <span style="color: #e74c3c;">*</span>' : ''}</strong></td>
            <td>${escapeHtml(formatFieldValue(field, getFieldValue(field)))}</td>
          </tr>
        `).join('')}
      </tbody>
    </table>
  </div>
  ` : ''}

  ${pipelines.length > 0 ? `
  <div class="section pipeline-section">
    <h2 class="section-title">Department Approval Pipeline</h2>
    <div class="pipeline-flow">
      ${pipelines.map((pipeline: any, index: number) => {
      const approved = isDepartmentApproved(pipeline.intApprovalOrder);
      const currentLevel = application.intCurrentApprovalLevel || 0;
      const isPending = !approved && currentLevel === (pipeline.intApprovalOrder - 1);
      const statusClass = approved ? 'approved' : isPending ? 'pending' : 'not-started';
      const statusText = approved ? '✓ Approved' : isPending ? '⏳ Pending' : '○ Not Started';
      const deptName = pipeline.hrTblDepartment?.txtDepartmentName || `Department ${pipeline.serDepartmentId || pipeline.intApprovalOrder}`;

      return `
          <div class="pipeline-card ${statusClass}">
            <div class="dept-name">${escapeHtml(deptName)}</div>
            <div class="level">Level ${pipeline.intApprovalOrder}</div>
            <div class="status">${statusText}</div>
            ${approved && application.txtRemarks && currentLevel === pipeline.intApprovalOrder ? `
              <div class="remarks">
                <strong>Remarks:</strong><br>
                "${escapeHtml(application.txtRemarks)}"
              </div>
            ` : ''}
          </div>
          ${index < pipelines.length - 1 ? '<span class="arrow">→</span>' : ''}
        `;
    }).join('')}
    </div>
  </div>
  ` : ''}

  <div class="footer">
    Generated on ${new Date().toLocaleString()} | Document ID: ${application.txtFormCode || application.serApplicationId || 'N/A'}
  </div>
</body>
</html>`;

    return html;
  }

  private generateBudgetApprovalPdfHtml(application: any, formFields: any[], applicationFormData: any, formName: string): string {
    const { heading, contentHtml, preparedBy, reviewers, recommenders, approver } = this.buildBudgetApprovalContent(applicationFormData);
    const headingText = heading || formName || 'Budget Approval';
    const dateStr = application?.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleDateString() : new Date().toLocaleDateString();
    return this.generateBudgetApprovalXyzHtml(headingText, dateStr, contentHtml, application?.txtApprovalHistory, preparedBy, reviewers, recommenders, approver);
  }

  private generateBudgetApprovalXyzHtml(
    headingText: string,
    dateStr: string,
    contentHtml: string,
    approvalHistoryJson?: string,
    preparedBy?: any,
    reviewers: any[] = [],
    recommenders: any[] = [],
    approver?: any
  ): string {
    let approvalHistory: any[] = [];
    if (approvalHistoryJson) {
      try {
        approvalHistory = JSON.parse(approvalHistoryJson);
      } catch (e) {
        approvalHistory = [];
      }
    }

    const getUserId = (user: any): number | null => {
      if (!user) return null;
      return user.serUserId || user.userId || user.id || null;
    };

    const getUserSignatureUrl = (user: any): string => {
      const userId = getUserId(user);
      if (!userId) return '';
      const entry = approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
      if (!entry || !entry.signaturePath) return '';
      return `${urls.API_URL}getSignature?userId=${userId}`;
    };

    const isUserApproved = (user: any): boolean => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory || approvalHistory.length === 0) return false;
      const entry = approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
      if (!entry) return false;
      if (!entry.signaturePath) return false;
      const action = (entry.action || entry.status || '').toString().toUpperCase();
      if (action === 'REJECTED') return false;
      if (action === 'APPROVED') return true;
      return !!entry.approvedDate;
    };

    const getUserApprovalDate = (user: any): string => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory || approvalHistory.length === 0) return '';
      const entry = approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId);
      if (!entry || !entry.approvedDate) return '';
      try {
        const dt = new Date(entry.approvedDate);
        if (isNaN(dt.getTime())) return String(entry.approvedDate);
        return dt.toLocaleString();
      } catch (e) {
        return String(entry.approvedDate);
      }
    };

    const renderUserCell = (user: any, fallbackName?: string, fallbackRole?: string): string => {
      if (!user && !fallbackName) return '';
      const name = user?.txtUserName || fallbackName || '';
      const role = user?.cfgTblRole?.txtRoleName || fallbackRole || '';
      const sigUrl = getUserSignatureUrl(user);
      const sigDate = getUserApprovalDate(user);
      const approved = isUserApproved(user);
      return `
        ${approved && sigUrl ? `<img class="xyz-sig-img" src="${sigUrl}" alt="Signature" crossorigin="anonymous" />` : ''}
        ${approved && sigDate ? `<div class="xyz-sig-time">${sigDate}</div>` : ''}
        <div>${name}${role ? `<br>(${role})` : ''}</div>
      `;
    };

    const renderUserNameCell = (user: any, fallbackName?: string, fallbackRole?: string): string => {
      if (!user && !fallbackName) return '';
      const name = user?.txtUserName || fallbackName || '';
      const role = user?.cfgTblRole?.txtRoleName || fallbackRole || '';
      return `<div>${name}${role ? `<br>(${role})` : ''}</div>`;
    };
    const css = `
    :root { --ink:#111827; --muted:#6b7280; --line:#c7cdd4; --accent:#0f766e; --soft:#eef4f3; }
    * { box-sizing: border-box; }
    body { margin: 0; padding: 0; background:#ffffff; color:var(--ink); }
    .xyz-page { background:#ffffff; padding: 0; display:block; }
    .xyz-paper { width: 210mm; min-height: 297mm; background:#ffffff; font-family: "Georgia", "Times New Roman", serif; font-size: 13.5px; line-height: 1.45; border: none; padding: 16mm 16mm 16mm 16mm; display:flex; flex-direction:column; }
    .xyz-date-row { display:flex; justify-content:flex-end; margin-bottom:6px; font-family: "Calibri", "Arial", sans-serif; color:var(--muted); }
    .xyz-date { font-size:12px; text-align:right; }
    .xyz-header { display:grid; grid-template-columns:70px 1fr 140px; align-items:center; column-gap:10px; margin-bottom:4px; }
    .xyz-logo { align-self:start; }
    .xyz-logo img { width:58px; height:auto; display:block; }
    .xyz-company { text-align:center; }
    .xyz-company-name { font-family: "Cambria", "Georgia", "Times New Roman", serif; font-size:26px; font-weight:700; letter-spacing:0.2px; }
    .xyz-company-address { font-family: "Calibri", "Arial", sans-serif; font-size:11.5px; color:var(--muted); margin-top:2px; }
    .xyz-meta { text-align:right; font-family: "Calibri", "Arial", sans-serif; font-size:11.5px; color:var(--muted); }
    .xyz-rule { height:1px; background:var(--line); margin:8px 0 12px 0; position:relative; }
    .xyz-rule::before, .xyz-rule::after { content:""; position:absolute; left:0; right:0; height:1px; background:var(--line); }
    .xyz-rule::before { top:-2px; }
    .xyz-rule::after { bottom:-2px; }
    .xyz-title { text-align:center; font-family: "Cambria", "Georgia", "Times New Roman", serif; font-weight:700; font-size:22px; letter-spacing:0.6px; text-transform:uppercase; margin:6px 0 14px 0; }
    .xyz-dynamic { margin-top:4px; }
    .xyz-section { margin-bottom:10px; }
    .xyz-section-title { font-family: "Georgia", "Times New Roman", serif; font-size:13.5px; font-weight:700; text-transform:uppercase; letter-spacing:0.4px; border-left:3px solid var(--accent); padding-left:8px; margin-bottom:4px; }
    .xyz-section-text { font-family: "Georgia", "Times New Roman", serif; font-size:13.5px; font-weight:400; text-align:justify; color:var(--ink); }
    .xyz-list { margin: 4px 0 0 18px; padding: 0; }
    .xyz-list li { margin-bottom: 6px; }
    .xyz-bold { font-weight:700; }
    .xyz-table { width:100%; border-collapse:collapse; margin:10px 0; font-size:13px; }
    .xyz-table th, .xyz-table td { border:1px solid var(--line); padding:6px 8px; }
    .xyz-table thead th { background:var(--soft); text-align:center; font-weight:700; }
    .xyz-table tbody td:first-child, .xyz-table tbody td:last-child { text-align:center; }
    .xyz-col-sr { width:8%; text-align:center; }
    .xyz-col-amount { width:18%; text-align:center; }
    .xyz-note { margin-top:6px; font-size:11.5px; color:var(--muted); }
    .xyz-signatures { width:100%; border-collapse:collapse; margin-top:14px; font-family: "Calibri", "Arial", sans-serif; font-size:12px; }
    .xyz-signatures th, .xyz-signatures td { border:1px solid var(--line); padding:6px 8px; vertical-align:top; text-align:left; }
    .xyz-signatures-blank td { height:62px; padding:6px 8px; background:#fff; }
    .xyz-signatures th { font-size:12.5px; font-weight:700; background:#e5e7eb; text-transform:uppercase; letter-spacing:0.3px; }
    .xyz-signatures th[colspan="2"] { text-align:center; }
    .xyz-sig-img { max-height: 30px; max-width: 100%; object-fit: contain; display:block; margin-bottom:4px; }
    .xyz-sig-time { font-size:10px; color:var(--muted); margin-bottom:4px; }
    .xyz-footer { margin-top:auto; }
    `;

    return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body>
  <div class="abc-wrapper">
    <style>${css}</style>
    <div class="xyz-page">
      <div class="xyz-paper">
      <div class="xyz-date-row"><div class="xyz-date">Date: ${dateStr}</div></div>
      <div class="xyz-header">
        <div class="xyz-logo"><img src="assets/images/qarshi-logo.png" alt="Qarshi" /></div>
        <div class="xyz-company">
          <div class="xyz-company-name">Qarshi Industries (Pvt) Ltd.</div>
          <div class="xyz-company-address">15-6, Jam-e-Shirin Boulevard, Gulberg-III, Lahore</div>
        </div>
        <div class="xyz-meta">Form: ${headingText}</div>
      </div>
      <div class="xyz-rule"></div>
      <div class="xyz-title">${headingText}</div>
      <div class="xyz-dynamic">${contentHtml}</div>

      <div class="xyz-footer">
      <table class="xyz-signatures">
        <tr class="xyz-signatures-blank">
          <td>${isUserApproved(preparedBy) ? renderUserCell(preparedBy) : ''}</td>
          <td>${isUserApproved(reviewers?.[0]) ? renderUserCell(reviewers?.[0]) : ''}</td>
          <td>${isUserApproved(reviewers?.[1]) ? renderUserCell(reviewers?.[1]) : ''}</td>
          <td>${isUserApproved(recommenders?.[0]) ? renderUserCell(recommenders?.[0]) : ''}</td>
          <td>${isUserApproved(approver) ? renderUserCell(approver) : ''}</td>
        </tr>
        <tr>
          <th>Prepared by:</th>
          <th colspan="2">Reviewed by:</th>
          <th>Recommended by:</th>
          <th>Approved by:</th>
        </tr>
        <tr>
          <td>${renderUserNameCell(preparedBy, preparedBy?.txtUserName, preparedBy?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(reviewers?.[0], reviewers?.[0]?.txtUserName, reviewers?.[0]?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(reviewers?.[1], reviewers?.[1]?.txtUserName, reviewers?.[1]?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(recommenders?.[0], recommenders?.[0]?.txtUserName, recommenders?.[0]?.cfgTblRole?.txtRoleName)}</td>
          <td>${renderUserNameCell(approver, approver?.txtUserName, approver?.cfgTblRole?.txtRoleName)}</td>
        </tr>
      </table>
      </div>
    </div>
  </div>
  </div>
</body>
</html>`;
  }

  
  private generateCapfAbcHtml(application: any, formFields: any[], applicationFormData: any, pipelines: any[] = []): string {
    // Helper function to get field value - completely self-contained, no dependency on abc component
    const getFieldValue = (fieldLabel: string): string => {
      // Helper: Slugify label to match backend keys
      const getSlug = (label: string): string => {
        return label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
      };

      if (!applicationFormData || Object.keys(applicationFormData).length === 0) {
        return '';
      }

      // 1. Concept Mapping (Template Label -> Concept)
      const templateKeyToConcept: { [key: string]: string } = {
        'DIVISION / DEPARTMENT': 'division',
        'CAPF #': 'capfNumber',
        'Date': 'date',
        'NAME OF ASSET / ITEM': 'assetName',
        'DETAIL SPECIFICATION': 'specification',
        'UTILITY & PURPOSE': 'utility',
        'FEASIBILITY REPORT ATTACHED': 'feasibilityReport',
        'IF NO THEN MENTION REASON': 'reason',
        'NAME': 'vendorName',
        'ADDRESS': 'vendorAddress',
        'APPROVED PRICE': 'approvedPrice',
        'DELIVERY PERIOD & DATE': 'deliveryPeriod',
        'TERMS & CONDITIONS': 'termsConditions',
        'Third Party assessment carried out': 'thirdPartyAssessment',
        'Third Party Assessment': 'thirdPartyAssessment',
        'Third Party assessment': 'thirdPartyAssessment'
      };

      // 2. Synonyms Mapping (Concept -> Potential Labels)
      // ORDER MATTERS: Put most specific/likely labels first to avoid collisions
      const fieldMappings: { [key: string]: string[] } = {
        'division': ['DIVISION / DEPARTMENT', 'Division', 'Department', 'division'],
        'capfNumber': ['CAPF #', 'CAPF', 'Capf Number', 'capf_number'],
        'date': ['Date', 'Submission Date', 'date'],
        'assetName': ['NAME OF ASSET / ITEM', 'Name of Asset', 'Asset Name', 'Item Name', 'asset_name'],
        'specification': ['DETAIL SPECIFICATION', 'DETAIL SPECIFICATION:', 'Detail Specification', 'Detail Specification:', 'Specification', 'specification', 'detail_specification', 'DETAIL_SPECIFICATION'],
        'utility': ['UTILITY & PURPOSE', 'Utility', 'Purpose', 'utility_purpose'],
        'feasibilityReport': ['FEASIBILITY REPORT ATTACHED', 'Feasibility Report', 'feasibility_report'],
        'reason': ['IF NO THEN MENTION REASON', 'Reason', 'If No Reason', 'reason'],
        'vendorName': ['Vendor Name', 'Vendor', 'Name of Vendor', 'NAME'], // 'NAME' moved to end to avoid collision with Asset Name 'Name'
        'vendorAddress': ['Vendor Address', 'Address', 'ADDRESS'],
        'approvedPrice': ['APPROVED PRICE', 'Approved Price', 'Price', 'Cost'],
        'deliveryPeriod': ['DELIVERY PERIOD & DATE', 'Delivery Period', 'Delivery Date'],
        'termsConditions': ['TERMS & CONDITIONS', 'Terms and Conditions', 'Terms & Conditions'],
        'thirdPartyAssessment': ['Third Party assessment carried out', 'Third Party Assessment', 'Third Party assessment', 'Third Party Assessment Carried Out', 'third_party_assessment', 'thirdPartyAssessment']
      };

      const lookupLabel = (lbl: string): string | null => {
        // Normalize label (remove colon, trim)
        const normalizedLbl = lbl.replace(/[:;]/g, '').trim();

        // Direct key check (with original label)
        if (applicationFormData[lbl] !== undefined && applicationFormData[lbl] !== null && applicationFormData[lbl] !== '') {
          return String(applicationFormData[lbl]);
        }

        // Direct key check (with normalized label)
        if (applicationFormData[normalizedLbl] !== undefined && applicationFormData[normalizedLbl] !== null && applicationFormData[normalizedLbl] !== '') {
          return String(applicationFormData[normalizedLbl]);
        }

        // Slug check (keys are usually slugs) - with original
        const slug = getSlug(lbl);
        if (applicationFormData[slug] !== undefined && applicationFormData[slug] !== null && applicationFormData[slug] !== '') {
          return String(applicationFormData[slug]);
        }

        // Slug check with normalized
        const normalizedSlug = getSlug(normalizedLbl);
        if (applicationFormData[normalizedSlug] !== undefined && applicationFormData[normalizedSlug] !== null && applicationFormData[normalizedSlug] !== '') {
          return String(applicationFormData[normalizedSlug]);
        }

        // Case-insensitive check (with original)
        const lowerLbl = lbl.toLowerCase().trim();
        for (const key in applicationFormData) {
          if (key.toLowerCase().trim() === lowerLbl && applicationFormData[key] !== null && applicationFormData[key] !== undefined && applicationFormData[key] !== '') {
            return String(applicationFormData[key]);
          }
        }

        // Case-insensitive check (with normalized - no colon)
        const lowerNormalized = normalizedLbl.toLowerCase().trim();
        for (const key in applicationFormData) {
          const keyNormalized = key.replace(/[:;]/g, '').toLowerCase().trim();
          if (keyNormalized === lowerNormalized && applicationFormData[key] !== null && applicationFormData[key] !== undefined && applicationFormData[key] !== '') {
            return String(applicationFormData[key]);
          }
        }

        // Form Fields lookup (match label exact, then checkout slug)
        if (formFields && formFields.length > 0) {
          // Try exact match first
          const field = formFields.find(f => {
            if (!f.label) return false;
            const fieldLabelNormalized = f.label.replace(/[:;]/g, '').toLowerCase().trim();
            return fieldLabelNormalized === lowerNormalized || f.label.toLowerCase().trim() === lowerLbl;
          });

          if (field) {
            const fieldSlug = getSlug(field.label);
            if (applicationFormData[fieldSlug] !== undefined && applicationFormData[fieldSlug] !== null && applicationFormData[fieldSlug] !== '') {
              return String(applicationFormData[fieldSlug]);
            }

            // Also try normalized slug
            const fieldLabelNormalized = field.label.replace(/[:;]/g, '').trim();
            const normalizedFieldSlug = getSlug(fieldLabelNormalized);
            if (applicationFormData[normalizedFieldSlug] !== undefined && applicationFormData[normalizedFieldSlug] !== null && applicationFormData[normalizedFieldSlug] !== '') {
              return String(applicationFormData[normalizedFieldSlug]);
            }
          }

          // Try partial match for "specification" related fields
          if (lowerNormalized.includes('specification') || lowerLbl.includes('specification')) {
            const specField = formFields.find(f => {
              if (!f.label) return false;
              const fieldLabelLower = f.label.toLowerCase();
              return fieldLabelLower.includes('specification') || fieldLabelLower.includes('detail');
            });
            if (specField) {
              const specFieldSlug = getSlug(specField.label);
              if (applicationFormData[specFieldSlug] !== undefined && applicationFormData[specFieldSlug] !== null && applicationFormData[specFieldSlug] !== '') {
                return String(applicationFormData[specFieldSlug]);
              }
            }
          }
        }
        return null;
      };

      const concept = templateKeyToConcept[fieldLabel];
      if (concept && fieldMappings[concept]) {
        for (const label of fieldMappings[concept]) {
          const val = lookupLabel(label);
          if (val !== null && val !== '') return val;
        }

        // Additional fallback: search through all form fields for concept-related fields
        if (formFields && formFields.length > 0) {
          const conceptKeywords: { [key: string]: string[] } = {
            'specification': ['specification', 'detail', 'spec'],
            'division': ['division', 'dept', 'department'],
            'assetName': ['asset', 'item', 'name'],
            'utility': ['utility', 'purpose'],
            'vendorName': ['vendor', 'name'],
            'vendorAddress': ['address', 'vendor'],
            'approvedPrice': ['price', 'approved', 'cost'],
            'deliveryPeriod': ['delivery', 'period', 'date'],
            'termsConditions': ['terms', 'conditions'],
            'thirdPartyAssessment': ['third', 'party', 'assessment', 'carried']
          };

          const keywords = conceptKeywords[concept];
          if (keywords) {
            for (const field of formFields) {
              if (!field.label) continue;
              const fieldLabelLower = field.label.toLowerCase();
              const matchesKeyword = keywords.some(kw => fieldLabelLower.includes(kw));

              if (matchesKeyword) {
                const fieldSlug = getSlug(field.label);
                if (applicationFormData[fieldSlug] !== undefined && applicationFormData[fieldSlug] !== null && applicationFormData[fieldSlug] !== '') {
                  return String(applicationFormData[fieldSlug]);
                }
              }
            }
          }
        }
      }

      // Fallback
      const val = lookupLabel(fieldLabel);
      return val !== null && val !== '' ? val : '';
    };

    const getCapfNumber = (): string => {
      return getFieldValue('CAPF #') || application.txtFormCode || '';
    };

    const getDate = (): string => {
      const dateValue = getFieldValue('Date') || application.dteCreatedDate || '';
      if (dateValue) {
        try {
          const date = new Date(dateValue);
          return date.toLocaleDateString();
        } catch (e) {
          return dateValue;
        }
      }
      return '';
    };

    const escapeHtml = (text: string): string => {
      if (!text) return '';
      const div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    };

    const feasibilityValue = getFieldValue('FEASIBILITY REPORT ATTACHED');
    const feasibilityYes = feasibilityValue && (feasibilityValue.toLowerCase() === 'yes' || feasibilityValue.toLowerCase() === 'true');
    const feasibilityNo = feasibilityValue && (feasibilityValue.toLowerCase() === 'no' || feasibilityValue.toLowerCase() === 'false');

    const thirdPartyValue = getFieldValue('Third Party assessment carried out') || getFieldValue('Third Party Assessment') || getFieldValue('Third Party assessment');
    const thirdPartyYes = thirdPartyValue && (thirdPartyValue.toLowerCase() === 'yes' || thirdPartyValue.toLowerCase() === 'true');
    const thirdPartyNo = thirdPartyValue && (thirdPartyValue.toLowerCase() === 'no' || thirdPartyValue.toLowerCase() === 'false');
    const thirdPartyNA = thirdPartyValue && (thirdPartyValue.toLowerCase() === 'na' || thirdPartyValue.toLowerCase() === 'n/a' || thirdPartyValue.toLowerCase() === 'not applicable');

    // Parse approval history for signatures
    let approvalHistory: any[] = [];
    if (application?.txtApprovalHistory) {
      try {
        approvalHistory = JSON.parse(application.txtApprovalHistory);
      } catch (e) {
        approvalHistory = [];
      }
    }

    const getApprovalEntryForSignature = (order: number, deptNameKeywords?: string[]): any | null => {
      if (!approvalHistory || approvalHistory.length === 0) {
        return null;
      }

      let entry = approvalHistory.find((e: any) => e.level === order);
      if (entry) {
        return entry;
      }

      if (deptNameKeywords && deptNameKeywords.length > 0) {
        const keywordsLower = deptNameKeywords.map(k => k.toLowerCase());
        entry = approvalHistory.find((e: any) => {
          const deptName = (e.departmentName || '').toString().toLowerCase();
          return keywordsLower.some(k => deptName.includes(k));
        });
        if (entry) {
          return entry;
        }
      }

      return null;
    };

    const getSignatureHtmlByIndex = (signatureIndex: number, deptNameKeywords?: string[]): string => {
      const order = signatureIndex + 1;
      const entry = getApprovalEntryForSignature(order, deptNameKeywords);
      if (!entry) {
        return '';
      }
      if (!entry.signaturePath) {
        return '';
      }
      const userId = entry.approvedBy || entry.approverUserId || entry.userId;
      if (!userId) {
        return '';
      }
      const signatureUrl = `${urls.API_URL}getSignature?userId=${userId}`;
      return `<img class="sig-img" src="${signatureUrl}" alt="Signature" crossorigin="anonymous" />`;
    };

    const getSignatureTimestampByIndex = (signatureIndex: number, deptNameKeywords?: string[]): string => {
      const order = signatureIndex + 1;
      const entry = getApprovalEntryForSignature(order, deptNameKeywords);
      if (!entry || !entry.approvedDate) {
        return '';
      }
      if (!entry.signaturePath) {
        return '';
      }
      try {
        const dt = new Date(entry.approvedDate);
        if (isNaN(dt.getTime())) return String(entry.approvedDate);
        return dt.toLocaleString();
      } catch (e) {
        return String(entry.approvedDate);
      }
    };

    // Helper function to check if a signature field should show "Approved"
    const isSignatureApproved = (signatureIndex: number): boolean => {
      const currentLevel = application.intCurrentApprovalLevel || 0;
      const status = application.txtStatus?.toUpperCase() || '';

      // If status is APPROVED, all departments have approved
      if (status === 'APPROVED') {
        return true;
      }

      // Map signature index to approval order (1-based)
      // Signature 0 = User Deptt. (HoD) = order 1
      // Signature 1 = Technical Expert = order 2
      // Signature 2 = Procurement = order 3
      // Signature 3 = Finance = order 4
      // Signature 4 = Core Team HTR. / CCT HO = order 5
      const approvalOrder = signatureIndex + 1;

      // Check if current level is greater than or equal to this order
      // Level 0 means nothing approved, level 1 means order 1 approved, etc.
      // Current level represents the last approved level (0-indexed)
      // So level 1 means order 1 has been approved
      return currentLevel >= approvalOrder;
    };

    // Alternative: Check by department name if pipelines are available
    const isSignatureApprovedByDept = (deptNameKeywords: string[]): boolean => {
      if (!pipelines || pipelines.length === 0) {
        return false;
      }

      const currentLevel = application.intCurrentApprovalLevel || 0;
      const status = application.txtStatus?.toUpperCase() || '';

      // If status is APPROVED, all departments have approved
      if (status === 'APPROVED') {
        return true;
      }

      // Find if any pipeline with matching department has been approved
      for (const pipeline of pipelines) {
        const pipelineOrder = pipeline.intApprovalOrder || 0;
        const deptName = pipeline.hrTblDepartment?.txtDepartmentName || '';
        const deptNameLower = deptName.toLowerCase();

        // Check if department name matches any keyword
        const matches = deptNameKeywords.some(keyword =>
          deptNameLower.includes(keyword.toLowerCase())
        );

        // Current level represents the last approved level (0-indexed)
        // So level 1 means order 1 has been approved
        if (matches && currentLevel >= pipelineOrder) {
          return true;
        }
      }

      return false;
    };

    const sig0Html = getSignatureHtmlByIndex(0, ['user', 'hod', 'department', 'head']);
    const sig1Html = getSignatureHtmlByIndex(1, ['technical', 'expert']);
    const sig2Html = getSignatureHtmlByIndex(2, ['procurement']);
    const sig3Html = getSignatureHtmlByIndex(3, ['finance']);
    const sig4Html = getSignatureHtmlByIndex(4, ['core team', 'htr', 'cct', 'ho']);

    const sig0Time = getSignatureTimestampByIndex(0, ['user', 'hod', 'department', 'head']);
    const sig1Time = getSignatureTimestampByIndex(1, ['technical', 'expert']);
    const sig2Time = getSignatureTimestampByIndex(2, ['procurement']);
    const sig3Time = getSignatureTimestampByIndex(3, ['finance']);
    const sig4Time = getSignatureTimestampByIndex(4, ['core team', 'htr', 'cct', 'ho']);

    // CSS styles for CAPF form PDF - exact copy of abc.component.css to ensure identical rendering

    const cssStyles = `
    :root {
      --ink: #111;
      --line: #222;
    }

    .abc-wrapper {
      box-sizing: border-box;
    }

    .abc-wrapper * {
      box-sizing: border-box;
    }

    .abc-wrapper {
      margin: 0;
      padding: 24px;
      background: #fff;
      color: var(--ink);
      font-family: Arial, Helvetica, sans-serif;
      min-height: 100vh;
      width: 100%;
      position: absolute;
      top: 0;
      left: 0;
      z-index: 9999;
      /* Ensure it covers everything if needed */
    }

    /* Embedded Mode Override */
    .abc-wrapper.embedded {
      position: relative !important;
      z-index: 1 !important;
      min-height: auto !important;
      padding: 0 !important;
      background: transparent !important;
      width: 100% !important;
    }

    .abc-wrapper.embedded .page {
      border: 1px solid var(--line) !important;
      width: 280mm !important;
      min-height: auto !important;
      height: auto !important;
      padding: 12mm !important;
      background: #fff !important;
      color: #000 !important;
      margin: 0 auto !important;
      box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
      overflow: visible !important;
    }

    .abc-wrapper.pdf-capture {
      width: 210mm !important;
      min-height: 297mm !important;
      padding: 0 !important;
      background: #fff !important;
      position: relative !important;
      top: auto !important;
      left: auto !important;
      z-index: 1 !important;
    }

    .abc-wrapper.pdf-capture .page {
      width: 210mm !important;
      min-height: 297mm !important;
      margin: 0 !important;
      padding: 10mm !important;
      border: 1px solid var(--line) !important;
      box-shadow: none !important;
      overflow: visible !important;
    }

    .abc-wrapper.pdf-capture .box,
    .abc-wrapper.pdf-capture .row,
    .abc-wrapper.pdf-capture .checks,
    .abc-wrapper.pdf-capture .sig-row,
    .abc-wrapper.pdf-capture .approved {
      break-inside: avoid;
      page-break-inside: avoid;
    }

    /* PDF capture fixes: prevent text clipping and keep lines at correct baseline */
    .abc-wrapper.pdf-capture .line,
    .abc-wrapper.pdf-capture .date-line,
    .abc-wrapper.pdf-capture .inline-line {
      min-height: 30px;
      padding-top: 3px;
      padding-bottom: 10px;
      line-height: 1.6;
      overflow: visible;
    }

    .abc-wrapper.pdf-capture .line::after,
    .abc-wrapper.pdf-capture .date-line::after,
    .abc-wrapper.pdf-capture .inline-line::after {
      bottom: 0;
    }

    /* PDF snapshot fix: minimal adjustments to avoid text clipping */
    .abc-wrapper.pdf-fix .line,
    .abc-wrapper.pdf-fix .date-line,
    .abc-wrapper.pdf-fix .inline-line {
      overflow: visible;
      align-items: flex-start;
      padding-top: 0;
      padding-bottom: 12px;
      line-height: 1.4;
      min-height: 34px;
      border-bottom: 1px solid var(--line);
    }

    .abc-wrapper.pdf-fix .line::after,
    .abc-wrapper.pdf-fix .date-line::after,
    .abc-wrapper.pdf-fix .inline-line::after {
      content: none;
    }

    .abc-wrapper.pdf-fix .pdf-empty {
      display: inline-block;
      min-height: 12px;
      line-height: 1.4;
    }

    /* PDF: lower checkbox box while keeping tick centered */
    .abc-wrapper.pdf-fix .boxcheck {
      transform: translateY(6px);
    }

    .abc-wrapper.pdf-fix .boxcheck > span {
      display: inline-block;
      transform: translateY(-6px);
    }


    /* COMMON */
    .b {
      font-weight: 700;
    }

    .u {
      text-decoration: underline;
    }

    .small {
      font-size: 12px;
    }

    .xs {
      font-size: 11px;
    }

    .tight {
      line-height: 1.1;
    }

    .mt6 {
      margin-top: 6px;
    }

    .mt8 {
      margin-top: 8px;
    }

    .mt10 {
      margin-top: 10px;
    }

    .mt12 {
      margin-top: 12px;
    }

    .mt14 {
      margin-top: 14px;
    }

    .mb6 {
      margin-bottom: 6px;
    }

    .mb8 {
      margin-bottom: 8px;
    }

    .mb10 {
      margin-bottom: 10px;
    }

    .mb12 {
      margin-bottom: 12px;
    }

    /* TABLES */
    table {
      width: 100% !important;
      max-width: 100% !important;
      border-collapse: collapse;
      margin: 0;
      box-sizing: border-box;
      table-layout: fixed;
    }

    .grid {
      width: 100%;
      table-layout: fixed;
      margin: 0;
      border-spacing: 0;
      box-sizing: border-box;
      display: table;
    }

    .grid td,
    .grid th {
      border: 1px solid var(--line);
      padding: 4px 6px;
      vertical-align: top;
      font-size: 12px;
      box-sizing: border-box;
      word-wrap: break-word !important;
      overflow-wrap: break-word !important;
    }

    .grid tr:first-child td {
      width: calc(100% / 3);
    }

    .grid th {
      font-weight: 700;
      text-align: left;
    }

    /* TOP BRAND */
    .brand-row {
      display: flex;
      gap: 10px;
      align-items: flex-start;
      margin-bottom: 4px;
    }

    .logo {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .logo img {
      max-width: 64px;
      height: auto;
    }

    .brand-title {
      font-weight: 700;
      font-size: 20px;
      letter-spacing: .2px;
    }

    /* MAIN TITLE BAR */
    .titlebar {
      border: 2px solid var(--line);
      text-align: center;
      font-weight: 700;
      padding: 6px 8px;
      margin-top: 6px;
      margin-bottom: 8px;
      letter-spacing: .5px;
    }

    /* SECTION BOX */
    .box {
      border: 2px solid var(--line);
      padding: 8px 8px 6px 8px;
      margin-top: 6px;
      overflow: hidden;
      box-sizing: border-box;
    }

    .box-title {
      text-align: center;
      font-weight: 700;
      margin: -2px 0 8px 0;
      letter-spacing: .2px;
      font-size: 13px;
    }

    /* FORM ROWS */
    .row {
      display: flex;
      gap: 8px;
      align-items: baseline;
      margin: 4px 0;
      width: 100%;
      box-sizing: border-box;
      overflow: hidden;
    }

    .field {
      display: flex;
      gap: 6px;
      align-items: baseline;
      flex: 1;
      min-width: 0;
      max-width: 100%;
      overflow: hidden;
      box-sizing: border-box;
    }

    .label {
      font-size: 13px;
      font-weight: 700;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .line {
      flex: 1;
      border-bottom: none;
      min-width: 40px;
      min-height: 26px;
      padding-top: 2px;
      padding-bottom: 7px;
      line-height: 1.4;
      display: flex;
      align-items: flex-end;
      overflow: visible;
      white-space: normal;
      word-break: break-word;
      overflow-wrap: break-word;
      box-sizing: border-box;
      position: relative;
    }

    .line::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 2px;
      border-bottom: 1px solid var(--line);
    }
    
    /* Prevent lines in fields with labels from stretching too much */
    .field .line {
      max-width: none;
    }
    
    /* Allow full width for lines without labels (like in capf-right) */
    .capf-right .line,
    .date-line {
      max-width: none;
    }

    .line.tall {
      min-height: 26px;
    }

    .line.xl {
      min-height: 32px;
    }

    /* CAPF + Date at right */
    .capf-right {
      display: flex;
      align-items: baseline;
      gap: 12px;
      white-space: nowrap;
      margin-left: auto;
    }

    .capf-box {
      display: flex;
      align-items: baseline;
      gap: 8px;
    }

    .capf-num {
      font-weight: 700;
      font-size: 20px;
      letter-spacing: 2px;
      border-bottom: 1px solid var(--line);
      padding: 0 6px 5px 6px;
      min-width: 86px;
      text-align: right;
      line-height: 1.2;
    }

    .date-line {
      width: 150px;
      border-bottom: none;
      min-height: 26px;
      padding-top: 2px;
      padding-bottom: 7px;
      line-height: 1.4;
      position: relative;
    }

    .date-line::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 2px;
      border-bottom: 1px solid var(--line);
    }

    /* CHECKBOXES */
    .checks {
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 6px 0;
      font-size: 13px;
      font-weight: 700;
    }

    .checks .label {
      flex-shrink: 0;
      white-space: nowrap;
    }

    .check-group {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 60px;
      max-width: 100%;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 700;
    }

    .boxcheck {
      width: 60px;
      height: 22px;
      border: 1px solid var(--line);
      border-radius: 4px;
      display: inline-block;
    }

    /* MULTILINE NOTES */
    .note {
      margin-top: 10px;
      font-size: 12px;
    }

    .note ol {
      margin: 6px 0 0 18px;
      padding: 0;
    }

    .note li {
      margin: 2px 0;
    }

    /* SUBSECTION HEADING */
    .subhead {
      margin: 8px -8px 8px -8px;
      padding: 5px 8px;
      border-top: 2px solid var(--line);
      /* border-bottom removed */
      font-weight: 700;
      letter-spacing: .2px;
      font-size: 13px;
    }

    /* TERMS small parenthetical */
    .paren {
      font-size: 12px;
      font-weight: normal;
    }

    /* SIGNATURES */
    .sig-row {
      display: flex;
      gap: 12px;
      align-items: flex-end;
      margin-top: 12px;
      flex-wrap: nowrap;
      width: 100%;
      justify-content: space-between;
    }

    .sig {
      flex: 1;
      min-width: 0;
      max-width: 100%;
      flex-shrink: 0;
    }
    
    .sig-line {
      border-bottom: 1px solid var(--line);
      height: 32px;
      margin-bottom: 4px;
    }

    .sig-img {
      max-height: 36px;
      max-width: 100%;
      object-fit: contain;
      display: block;
      margin: 0 auto;
    }

    .sig-time {
      font-size: 10px;
      text-align: center;
      margin-bottom: 2px;
      line-height: 1.1;
      white-space: nowrap;
    }

    .sig-label {
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
      line-height: 1.2;
    }

    .approved {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      align-items: flex-end;
      margin-top: 8px;
    }

    .approved .who {
      font-weight: 700;
      white-space: nowrap;
    }

    .approved-sig {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
    }

    .approved .appline {
      width: 150px;
      border-bottom: 1px solid var(--line);
      height: 18px;
      margin-bottom: 4px;
    }

    .approved-sig .who {
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      white-space: nowrap;
      width: 100%;
    }

    /* PART-2 + JOB COMPLETION */
    .part2-title {
      border-top: 2px solid var(--line);
      /* border-bottom removed */
      margin: 10px -10px 8px -10px;
      padding: 6px 10px;
      text-align: center;
      font-weight: 700;
      letter-spacing: .2px;
    }

    .two-col {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: 12px;
      margin-top: 8px;
    }

    .sign-block {
      width: 48%;
      max-width: 48%;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 4px;
      flex-shrink: 0;
    }

    .sign-block .sb-label {
      font-weight: 700;
      font-size: 13px;
    }

    .sign-block .sb-line {
      width: 100%;
      border-bottom: 1px solid var(--line);
      height: 18px;
    }

    .sign-block .sb-sub {
      font-size: 11px;
      font-weight: 700;
      margin-top: -2px;
      padding-left: 4px;
      text-align: left;
    }
    /* Signature blocks: label left, line to the right, sub centered under line */
    .part2-sign .sign-block,
    .job-sign .sign-block {
      display: grid;
      grid-template-columns: auto 120px;
      grid-template-rows: auto auto;
      column-gap: 2px;
      align-items: end;
    }

    .part2-sign .sign-block .sb-label,
    .job-sign .sign-block .sb-label {
      grid-column: 1;
      grid-row: 1;
      width: auto;
      white-space: nowrap;
    }

    .part2-sign .sign-block .sb-line,
    .job-sign .sign-block .sb-line {
      grid-column: 2;
      grid-row: 1;
      width: 120px;
      margin-left: 0;
      display: block;
    }

    .part2-sign .sign-block .sb-sub,
    .job-sign .sign-block .sb-sub {
      grid-column: 2;
      grid-row: 2;
      width: 120px;
      margin-left: 0;
      text-align: center;
      display: block;
      white-space: nowrap;
    }

    /* Job completion: move line + (Sign & Desg.) closer to Checked By label */
    .job-sign .sign-block:first-child .sb-line,
    .job-sign .sign-block:first-child .sb-sub {
      grid-column: 1;
      width: 120px;
      text-align: center;
      margin-left: 95px;
    }

    .part2-sign .sign-block:first-child .sb-line,
    .part2-sign .sign-block:first-child .sb-sub {
      grid-column: 1;
      width: 120px;
      text-align: center;
      margin-left: 95px;
    }

    .part2-sign .sign-block:last-child,
    .job-sign .sign-block:last-child {
      justify-content: end;
    }

    .part2-sign .sign-block:last-child .sb-label,
    .job-sign .sign-block:last-child .sb-label {
      text-align: right;
    }

    .job-title {
      text-align: center;
      font-weight: 700;
      margin: 10px -10px 10px -10px;
      padding-top: 6px;
      border-top: 2px solid var(--line);
      letter-spacing: .2px;
    }

    .job-text {
      font-size: 13px;
      font-weight: 700;
      margin-top: 8px;
      line-height: 1.35;
    }

    .inline-line {
      display: inline-block;
      border-bottom: none;
      min-height: 22px;
      vertical-align: baseline;
      width: 140px;
      margin: 0 6px;
      padding-top: 2px;
      padding-bottom: 7px;
      position: relative;
    }

    .inline-line::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 2px;
      border-bottom: 1px solid var(--line);
    }

    .inline-line.short {
      width: 110px;
    }

    .inline-line.long {
      width: 190px;
    }

    /* PAGE */
    .page {
      width: 280mm;
      min-height: auto;
      height: auto;
      margin: 0 auto;
      border: 2px solid var(--line);
      padding: 12mm;
      box-sizing: border-box;
      background: white;
      overflow: visible;
    }

    /* PDF COMPACT MODE OVERRIDES in CSS */
    :host-context(.pdf-compact) .page {
      width: 200mm !important;
      /* Fit within 210mm - 10mm (5mm+5mm margin) */
      min-height: 297mm !important;
      border: none !important;
      padding: 0mm !important;
      /* Let PDF margins handle the spacing */
      transform: none !important;
    }

    /* Compress everything vertically */
    :host-context(.pdf-compact) .row {
      margin: 1px 0 !important;
    }

    :host-context(.pdf-compact) .field {
      gap: 4px !important;
    }

    :host-context(.pdf-compact) .label {
      font-size: 11px !important;
    }

    :host-context(.pdf-compact) .line {
      height: 16px !important;
      min-height: 16px !important;
      padding-top: 1px !important;
      padding-bottom: 4px !important;
      border-bottom-width: 1px !important;
    }

    :host-context(.pdf-compact) .line::after {
      bottom: 1px !important;
    }

    :host-context(.pdf-compact) .line.xl {
      height: 20px !important;
    }

    :host-context(.pdf-compact) .box {
      padding: 4px 8px !important;
      margin-top: 4px !important;
      border-width: 1px !important;
    }

    :host-context(.pdf-compact) .box-title {
      margin-bottom: 4px !important;
      font-size: 11px !important;
    }

    :host-context(.pdf-compact) .titlebar {
      margin: 4px 0 !important;
      padding: 3px !important;
      font-size: 14px !important;
      border-width: 1px !important;
    }

    :host-context(.pdf-compact) .note {
      margin-top: 4px !important;
      font-size: 9px !important;
    }

    :host-context(.pdf-compact) .note li {
      margin: 0 !important;
    }

    :host-context(.pdf-compact) .sig-row {
      margin-top: 8px !important;
      gap: 10px !important;
    }

    :host-context(.pdf-compact) .sig-line {
      margin-bottom: 2px !important;
      height: 14px !important;
    }

    :host-context(.pdf-compact) .brand-row {
      margin-bottom: 2px !important;
    }

    :host-context(.pdf-compact) .brand-title {
      font-size: 16px !important;
    }

    :host-context(.pdf-compact) .logo img {
      max-width: 36px !important;
    }

    :host-context(.pdf-compact) .checks {
      margin: 2px 0 !important;
      font-size: 11px !important;
    }

    :host-context(.pdf-compact) .checks .boxcheck {
      height: 18px !important;
      width: 40px !important;
    }

    :host-context(.pdf-compact) .two-col {
      margin-top: 4px !important;
      gap: 10px !important;
    }

    :host-context(.pdf-compact) .job-title {
      margin: 4px -4px !important;
      padding: 2px !important;
      font-size: 11px !important;
      border-top-width: 1px !important;
    }

    :host-context(.pdf-compact) .job-text {
      margin-top: 2px !important;
      line-height: 1.1 !important;
      font-size: 10px !important;
    }

    :host-context(.pdf-compact) .subhead {
      margin: 4px -4px !important;
      padding: 2px 4px !important;
      font-size: 11px !important;
      border-top-width: 1px !important;
    }

    :host-context(.pdf-compact) .part2-title {
      margin: 6px -4px 4px -4px !important;
      padding: 2px 4px !important;
      font-size: 11px !important;
      border-top-width: 1px !important;
    }

    /* Approvals section compression */
    :host-context(.pdf-compact) .approved {
      margin-top: 4px !important;
    }

    :host-context(.pdf-compact) .approved .appline {
      width: 100px !important;
      height: 14px !important;
    }

    /* Header table compression */
    :host-context(.pdf-compact) .grid td {
      padding: 2px 4px !important;
      font-size: 10px !important;
    }


    `;

    const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body style="margin:0; padding:0; background:#fff;">
  <div class="abc-wrapper pdf-fix">
    <style>${cssStyles}</style>
    <div class="page">
      <!-- Header -->
      <div class="brand-row">
        <div class="logo">
          <img src="assets/images/qarshi-logo.png" alt="" class="ml-[5px] w-16 flex-none">
        </div>
        <div class="brand-title">Qarshi Industries (Pvt) Ltd.</div>
      </div>

      <table class="grid">
        <tr>
          <td><span class="b">Division:</span> ***</td>
          <td><span class="b">Department:</span> PRC</td>
          <td><span class="b">Section:</span> GEN</td>
        </tr>
        <tr>
          <td><span class="b">Document No.:</span> PRC-GEN-FM-03</td>
          <td><span class="b">Original Issue:</span> 01-06-2006</td>
          <td>
            <div style="display:flex; justify-content:space-between; gap:10px;">
              <span><span class="b">Rev.</span> # 05</span>
              <span><span class="b">Rev. Date:</span> 01-12-2015</span>
            </div>
          </td>
        </tr>
      </table>

      <div class="titlebar">CAPITAL ASSETS PURCHASE FORM</div>

      <!-- PART 1 -->
      <div class="box">
        <div class="box-title">PART-1 (TO BE FILLED BY <span class="u">CONCERNED</span> DEPARTMENT)</div>

        <div class="row">
          <div class="capf-right" style="margin-left: auto;">
            <div class="capf-box">
              <div class="label">CAPF #</div>
              <div class="capf-num">${escapeHtml(getCapfNumber())}</div>
            </div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DIVISION / DEPARTMENT:</div>
            <div class="line">${escapeHtml(getFieldValue('DIVISION / DEPARTMENT'))}</div>
          </div>

          <div class="capf-right">
            <div class="capf-box">
              <div class="label">Date:</div>
              <div class="date-line">${escapeHtml(getDate())}</div>
            </div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">NAME OF ASSET / ITEM:</div>
            <div class="line">${escapeHtml(getFieldValue('NAME OF ASSET / ITEM'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DETAIL SPECIFICATION:</div>
            <div class="line">${escapeHtml(getFieldValue('DETAIL SPECIFICATION'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">UTILITY &amp; PURPOSE:</div>
            <div class="line">${escapeHtml(getFieldValue('UTILITY & PURPOSE'))}</div>
          </div>
        </div>

        <div class="checks">
          <div class="label">FEASIBILITY REPORT ATTACHED:</div>
          <div class="check-group">
            <div class="check">Yes <span class="boxcheck" style="display:flex; align-items:center; justify-content:center;">${feasibilityYes ? '<span>&#10003;</span>' : ''}</span></div>
            <div class="check">No <span class="boxcheck" style="display:flex; align-items:center; justify-content:center;">${feasibilityNo ? '<span>&#10003;</span>' : ''}</span></div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">IF NO THEN MENTION REASON:</div>
            <div class="line xl">${escapeHtml(getFieldValue('IF NO THEN MENTION REASON:') || getFieldValue('IF NO THEN MENTION REASON'))}</div>
          </div>
        </div>

        <div class="note">
          <div class="b">NOTE:</div>
          <ol class="xs tight" type="i">
            <li>In case of technical item, verification of technical expert is mandatory on feasibility / proposal.</li>
            <li>In case of price more than 5 million, third party assessment is mandatory.</li>
            <li>Capital Asset Purchase Checklist (PRC-GEN-FM-29) must be completed along with Capital Asset Purchase Form (PRC-GEN-FM-03).</li>
          </ol>
        </div>

        <div class="subhead">PARTICULARS OF SELECTED VENDOR(S) (AS PER APPROVED QUOTATION)</div>

        <div class="row">
          <div class="field">
            <div class="label">NAME:</div>
            <div class="line">${escapeHtml(getFieldValue('NAME'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">ADDRESS:</div>
            <div class="line">${escapeHtml(getFieldValue('ADDRESS'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">APPROVED PRICE:</div>
            <div class="line">${escapeHtml(getFieldValue('APPROVED PRICE'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DELIVERY PERIOD &amp; DATE:</div>
            <div class="line">${escapeHtml(getFieldValue('DELIVERY PERIOD & DATE'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">TERMS &amp; CONDITIONS:</div>
            <div class="line">${escapeHtml(getFieldValue('TERMS & CONDITIONS'))}</div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label paren">(Payment, After Sale Service, Warranty etc.)</div>
            <div class="line xl">${escapeHtml(getFieldValue('TERMS & CONDITIONS'))}</div>
          </div>
        </div>

        <div class="checks">
          <div class="label">Third Party assessment carried out</div>
          <div class="check-group">
            <div class="check">Yes <span class="boxcheck" style="display:flex; align-items:center; justify-content:center;">${thirdPartyYes ? '<span>&#10003;</span>' : ''}</span></div>
            <div class="check">No <span class="boxcheck" style="display:flex; align-items:center; justify-content:center;">${thirdPartyNo ? '<span>&#10003;</span>' : ''}</span></div>
            <div class="check">NA <span class="boxcheck" style="display:flex; align-items:center; justify-content:center;">${thirdPartyNA ? '<span>&#10003;</span>' : ''}</span></div>
          </div>
        </div>

        <div class="sig-row">
          <div class="sig">
            <div class="sig-line">${sig0Html}</div>
            ${sig0Html ? '<div class="sig-time">' + escapeHtml(sig0Time) + '</div>' : ''}
            <div class="sig-label">User Deptt. (HoD)</div>
          </div>
          <div class="sig">
            <div class="sig-line">${sig1Html}</div>
            ${sig1Html ? '<div class="sig-time">' + escapeHtml(sig1Time) + '</div>' : ''}
            <div class="sig-label">Technical Expert</div>
          </div>
          <div class="sig">
            <div class="sig-line">${sig2Html}</div>
            ${sig2Html ? '<div class="sig-time">' + escapeHtml(sig2Time) + '</div>' : ''}
            <div class="sig-label">Procurement</div>
          </div>
          <div class="sig">
            <div class="sig-line">${sig3Html}</div>
            ${sig3Html ? '<div class="sig-time">' + escapeHtml(sig3Time) + '</div>' : ''}
            <div class="sig-label">Finance</div>
          </div>
          <div class="sig">
            <div class="sig-line">${sig4Html}</div>
            ${sig4Html ? '<div class="sig-time">' + escapeHtml(sig4Time) + '</div>' : ''}
            <div class="sig-label">Core Team HTR. / CCT HO</div>
          </div>
        </div>

        <div class="xs mt6"><span class="b">Note:</span> Designation must be mentioned against each signature.</div>

        <div class="approved">
          <div class="who b">Approved By:</div>
          <div class="approved-sig">
            <div class="appline"></div>
            <div class="who b">Chief Executive</div>
          </div>
        </div>

        <!-- PART 2 -->
        <div class="part2-title">PART-2 (TO BE FILLED BY PROCUREMENT DEPARTMENT)</div>

        <div class="row">
          <div class="field">
            <div class="label">P. O. No. WITH DATE:</div>
            <div class="line"></div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">PARTICULARS OF VENDOR(S):</div>
            <div class="line"></div>
          </div>
        </div>

        <div class="row">
          <div class="field">
            <div class="label">DELIVERY DATE:</div>
            <div class="line" style="max-width:220px;"></div>
          </div>
        </div>

        <div class="two-col part2-sign">
          <div class="sign-block">
            <div class="sb-label">Checked By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
          <div class="sign-block" style="align-items:flex-end;">
            <div class="sb-label">Verified By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
        </div>

        <div class="job-title">(JOB COMPLETION CERTIFICATE)</div>

        <div class="job-text">
          THIS IS TO CERTIFY THAT JOB AGAINST CAPF #
          <span class="inline-line"></span>
          DATED
          <span class="inline-line short"></span>
          HAS BEEN COMPLETED.
        </div>

        <div class="job-text">
          GRN #:
          <span class="inline-line short"></span>
          DATED
          <span class="inline-line long"></span>
          (REPORT ATTACHED)
        </div>

        <div class="two-col job-sign">
          <div class="sign-block">
            <div class="sb-label">Checked By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
          <div class="sign-block" style="align-items:flex-end;">
            <div class="sb-label">Verified By:</div>
            <div class="sb-line"></div>
            <div class="sb-sub">(Sign &amp; Desg.)</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>`;

    return html;
  }
  private buildBudgetApprovalContent(applicationFormData: any): {
    heading: string;
    contentHtml: string;
    preparedBy?: any,
    reviewers?: any[],
    recommenders?: any[],
    approver?: any
  } {
    const escapeHtml = (text: string): string => {
      if (text === null || text === undefined) return '';
      const div = document.createElement('div');
      div.textContent = String(text);
      return div.innerHTML;
    };

    const sanitizeBudgetHtml = (html: string): string => {
      const wrapper = document.createElement('div');
      wrapper.innerHTML = html;

      const candidates = wrapper.querySelectorAll('button, input, a, [role="button"]');
      candidates.forEach(el => {
        const tag = el.tagName.toLowerCase();
        const text = (el.textContent || '').toLowerCase();
        const value = (el as HTMLInputElement).value ? (el as HTMLInputElement).value.toLowerCase() : '';
        const type = tag === 'input' ? ((el as HTMLInputElement).type || '').toLowerCase() : '';
        const hasResetText = text.includes('reset') || text.includes('close & reset');
        const hasResetValue = value.includes('reset') || value.includes('close & reset');
        const isResetType = type === 'reset';
        if (hasResetText || hasResetValue || isResetType) {
          el.remove();
        }
      });

      return wrapper.innerHTML;
    };

    const getValueByLabelContains = (needle: string): any => {
      if (!applicationFormData) return '';
      const key = Object.keys(applicationFormData).find(k => k.toLowerCase().includes(needle.toLowerCase()));
      return key ? applicationFormData[key] : '';
    };

    const getValueByAnyLabel = (needles: string[]): any => {
      if (!applicationFormData) return '';
      for (const n of needles) {
        const key = Object.keys(applicationFormData).find(k => k.toLowerCase().includes(n.toLowerCase()));
        if (key) return applicationFormData[key];
      }
      return '';
    };

    const headingRaw = getValueByAnyLabel(['heading', 'title', 'subject', 'request title', 'form title']);
    const heading = headingRaw ? String(headingRaw).trim() : '';
    const background = getValueByLabelContains('background');
    const proposal = getValueByLabelContains('proposal');
    const request = getValueByLabelContains('request');
    const finances = getValueByLabelContains('finance');
    const note = getValueByLabelContains('note');

    let contentHtml = '';

    const rawHtml = applicationFormData?.content || applicationFormData?.editorContent || applicationFormData?.html;
    const result: any = {
      heading,
      contentHtml: '',
      preparedBy: applicationFormData?.preparedBy,
      reviewers: applicationFormData?.reviewers || [],
      recommenders: applicationFormData?.recommenders || [],
      approver: applicationFormData?.approver
    };

    if (rawHtml) {
      result.contentHtml = sanitizeBudgetHtml(String(rawHtml));
      return result;
    }

    if (background) {
      contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Background</div><div class="xyz-section-text">${escapeHtml(background)}</div></div>`;
    }

    if (proposal) {
      const proposalText = String(proposal);
      const items = proposalText
        .split(/\r?\n/)
        .map(i => i.replace(/\t+/g, ' ').trim())
        .filter(i => i.length > 0);
      const listItems = items.length > 1 ? items : proposalText.split(/(?=\d+\.)/).map(i => i.trim()).filter(Boolean);
      if (listItems.length > 1) {
        contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Proposal</div><ol class="xyz-list">${listItems.map(i => `<li>${escapeHtml(i.replace(/^\d+\.\s*/, '').trim())}</li>`).join('')}</ol></div>`;
      } else {
        contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Proposal</div><div class="xyz-section-text">${escapeHtml(proposalText)}</div></div>`;
      }
    }

    if (request) {
      contentHtml += `<div class="xyz-section"><div class="xyz-section-title">Request</div><div class="xyz-section-text">${escapeHtml(request)}</div></div>`;
    }

    if (finances) {
      let rows: string[][] = [];
      let headerRow: string[] | null = null;
      if (Array.isArray(finances)) {
        if (finances.length && typeof finances[0] === 'object' && !Array.isArray(finances[0])) {
          rows = finances.map((r: any) => [r.sr || r.Sr || r['Sr.'] || '', r.description || r.Description || '', r.amount || r.Amount || ''].map((c: any) => String(c)));
        } else {
          rows = finances.map((r: any) => Array.isArray(r) ? r.map((c: any) => String(c)) : [String(r)]);
        }
      } else if (typeof finances === 'object' && finances !== null) {
        if (Array.isArray(finances.rows)) {
          rows = finances.rows.map((r: any) => Array.isArray(r) ? r.map((c: any) => String(c)) : [String(r)]);
        }
      } else if (typeof finances === 'string') {
        const parts = finances.split(',').map(p => p.trim()).filter(Boolean);
        if (parts.length >= 3 && parts[0].toLowerCase().includes('sr') && parts[1].toLowerCase().includes('description')) {
          parts.splice(0, 3);
        }
        for (let i = 0; i < parts.length; i += 3) {
          rows.push([parts[i] || '', parts[i + 1] || '', parts[i + 2] || '']);
        }
      }

      if (rows.length) {
        const first = rows[0] || [];
        const isHeader =
          first.length >= 3 &&
          first[0].toLowerCase().includes('sr') &&
          first[1].toLowerCase().includes('description') &&
          first[2].toLowerCase().includes('amount');
        if (isHeader) {
          headerRow = first;
          rows = rows.slice(1);
        }

        const headerHtml = headerRow
          ? `<tr><th class="xyz-col-sr">${escapeHtml(headerRow[0])}</th><th>${escapeHtml(headerRow[1])}</th><th class="xyz-col-amount">${escapeHtml(headerRow[2])}</th></tr>`
          : `<tr><th class="xyz-col-sr">Sr.</th><th>Description</th><th class="xyz-col-amount">Amount</th></tr>`;

        contentHtml += `<table class="xyz-table"><thead>${headerHtml}</thead><tbody>`;
        rows.forEach(r => {
          contentHtml += `<tr><td>${escapeHtml(r[0] || '')}</td><td>${escapeHtml(r[1] || '')}</td><td>${escapeHtml(r[2] || '')}</td></tr>`;
        });
        contentHtml += `</tbody></table>`;
      }
    }

    if (note) {
      contentHtml += `<div class="xyz-note"><span class="xyz-bold">Note:</span> ${escapeHtml(note)}</div>`;
    }

    return { heading, contentHtml };
  }
}
