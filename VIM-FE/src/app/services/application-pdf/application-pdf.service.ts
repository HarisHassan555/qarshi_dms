import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';
import {
  resolveCapfLogoPath,
  resolveCapfBrandTitle,
  resolveCapfLogoCssClass,
  resolveCapfFormNameFromSources,
} from 'src/app/utils/capf-logo.util';
import { formatCapfFormNumberDisplay } from 'src/app/utils/capf-form.util';
import {
  A4_LONG_EDGE_MM,
  A4_SHORT_EDGE_MM,
  FormOrientation,
  getA4PaperSizeMm,
  resolveFormOrientation,
} from 'src/app/utils/form-orientation.util';
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
    applicationMeta?: { formName?: string; txtFormCode?: string; omitApprovalSignaturesInPdf?: boolean }
  ): string {
    let isCapf = this.isCapfFormMeta(data, form, applicationMeta);
    const isExpenseClaim = this.isExpenseClaimFormMeta(data, form, applicationMeta);
    const isTemporaryAdvanceSlip = this.isTemporaryAdvanceSlipFormMeta(data, form, applicationMeta);

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
      htmlContent = this.generateBudgetApprovalPdfHtml(
        data,
        formFields,
        applicationFormData,
        resolvedFormName || 'Budget Approval',
        applicationMeta
      );
    }

    if (!handledBudgetApproval && isBudgetApproval) {
      handledBudgetApproval = true;
      htmlContent = this.generateBudgetApprovalPdfHtml(
        data,
        formFields,
        applicationFormData,
        resolvedFormName || 'Budget Approval',
        applicationMeta
      );
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

      htmlContent = this.generateCapfAbcHtml(data, formFields, applicationFormData, pipelines, applicationMeta);
      if (!htmlContent || !htmlContent.includes('abc-wrapper')) {
        throw new Error('ABC HTML generation failed');
      }
    } else if (!handledBudgetApproval && isExpenseClaim) {
      htmlContent = this.generateExpenseClaimSlipPdfHtml(
        data,
        formFields,
        applicationFormData,
        applicationMeta
      );
    } else if (!handledBudgetApproval && isTemporaryAdvanceSlip) {
      htmlContent = this.generateTemporaryAdvanceSlipPdfHtml(
        data,
        formFields,
        applicationFormData,
        applicationMeta
      );
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

      htmlContent = this.generateGenericApplicationPdfHtml(
        data,
        formFields,
        applicationFormData,
        resolvedFormName || 'Unknown Form',
        applicationMeta
      );
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

          // Generic / budget XYZ layouts also use .abc-wrapper (same outer class as CAPF). Route by content:
          // — .xyz-paper => multi-page split via renderXyzPdfFromElement (must run before CAPF branch).
          // — .page inside wrapper => CAPF layout.
          const abcRoot = (iframeDoc.querySelector('.abc-wrapper') as HTMLElement) || null;
          const hasXyzPaper = !!iframeDoc.querySelector('.xyz-paper');
          const hasCapfPageLayout = !!(iframeDoc.querySelector('.abc-wrapper .page') || iframeDoc.querySelector('.page'));

          if (hasXyzPaper) {
            const xyzRoot = (abcRoot || element) as HTMLElement;
            try {
              const pdfBlob = await this.renderXyzPdfFromElement(xyzRoot);
              if (done) return;
              done = true;
              cleanup(iframe);
              resolve(pdfBlob);
              return;
            } catch (xyzError) {
              console.error('XYZ deterministic PDF generation failed, falling back to html2pdf:', xyzError);
            }
          } else if (hasCapfPageLayout && abcRoot) {
            try {
              const pdfBlob = await this.renderCapfPdfFromElement(abcRoot);
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

          const isXyzPaper = element.classList.contains('xyz-paper') || !!element.querySelector('.xyz-paper');

          element.offsetHeight;
          // For xyz-paper elements, ensure natural height for PDF generation
          if (isXyzPaper) {
            // Add a class to the element to trigger PDF-specific styles
            element.classList.add('pdf-generation-mode');
            const paperElement = (element.querySelector('.xyz-paper') as HTMLElement) || element;
            const hasIndividualPipelineSignatures = !!paperElement?.querySelector(
              '.xyz-footer .xyz-signatures, .ec-slip-pipeline-footer .xyz-signatures'
            );
            const footerPinned =
              !!paperElement &&
              (paperElement.classList.contains('xyz-paper-footer-pinned') ||
                paperElement.classList.contains('xyz-paper--footer-pinned') ||
                !!paperElement.querySelector('.xyz-footer-spacer'));
            if (paperElement) {
              paperElement.classList.add('pdf-generation-mode');
              if (hasIndividualPipelineSignatures) {
                const setIframeStyle = (el: HTMLElement | null, prop: string, value: string) => {
                  if (el) {
                    el.style.setProperty(prop, value, 'important');
                  }
                };
                this.applyIndividualPipelineFooterPdfCaptureStyles(paperElement, setIframeStyle);
              }
              if (footerPinned) {
                const paperLandscape = paperElement.classList.contains('xyz-paper--landscape');
                const pageHeightMm = paperLandscape ? A4_SHORT_EDGE_MM : A4_LONG_EDGE_MM;
                paperElement.style.setProperty('display', 'flex', 'important');
                paperElement.style.setProperty('flex-direction', 'column', 'important');
                paperElement.style.setProperty('min-height', `${pageHeightMm}mm`, 'important');
                paperElement.style.setProperty('height', 'auto', 'important');
                paperElement.style.setProperty('max-height', paperLandscape ? `${pageHeightMm}mm` : 'none', 'important');
                paperElement.style.setProperty('overflow', 'visible', 'important');
              } else {
                paperElement.style.height = 'auto';
                paperElement.style.maxHeight = 'none';
                paperElement.style.minHeight = 'auto';
                paperElement.style.overflow = 'visible';
                paperElement.style.display = 'block';
              }
            }
            const contentArea = element.querySelector('.xyz-content-area') as HTMLElement;
            if (contentArea) {
              if (footerPinned) {
                contentArea.style.setProperty('overflow', 'visible', 'important');
              } else {
                contentArea.style.overflow = 'visible';
                contentArea.style.minHeight = 'auto';
                contentArea.style.flex = 'none';
                contentArea.style.height = 'auto';
              }
            }
          }

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
                  // Add PDF generation class
                  clonedElement.classList.add('pdf-generation-mode');
                  
                  // Remove ALL height constraints for PDF generation
                  const paperEl = clonedDoc.querySelector('.xyz-paper') as HTMLElement;
                  if (paperEl) {
                    paperEl.classList.add('pdf-generation-mode');
                    paperEl.style.setProperty('height', 'auto', 'important');
                    paperEl.style.setProperty('max-height', 'none', 'important');
                    paperEl.style.setProperty('min-height', 'auto', 'important');
                    paperEl.style.setProperty('overflow', 'visible', 'important');
                    paperEl.style.setProperty('display', 'block', 'important');
                  }
                  const contentArea = clonedDoc.querySelector('.xyz-content-area') as HTMLElement;
                  if (contentArea) {
                    contentArea.style.setProperty('overflow', 'visible', 'important');
                    contentArea.style.setProperty('min-height', 'auto', 'important');
                    contentArea.style.setProperty('flex', 'none', 'important');
                    contentArea.style.setProperty('height', 'auto', 'important');
                    contentArea.style.setProperty('display', 'block', 'important');
                  }
                  // Remove flex from header and footer too
                  const headerContainer = clonedDoc.querySelector('.xyz-header-container') as HTMLElement;
                  if (headerContainer) {
                    headerContainer.style.setProperty('flex-shrink', '0', 'important');
                  }
                  const footer = clonedDoc.querySelector('.xyz-footer') as HTMLElement;
                  if (footer) {
                    footer.style.setProperty('flex-shrink', '0', 'important');
                    footer.style.setProperty('margin-top', 'auto', 'important');
                  }
                }
              }
            },
            jsPDF: {
              unit: 'mm' as const,
              format: 'a4' as const,
              orientation: 'portrait' as const,
              compress: true
            }
          };

          html2pdf().set(opt).from(element).outputPdf('blob').then((pdfBlob: Blob) => {
            if (done) return;
            done = true;
            // Remove PDF generation class
            element.classList.remove('pdf-generation-mode');
            const paperElement = element.querySelector('.xyz-paper') as HTMLElement;
            if (paperElement) {
              paperElement.classList.remove('pdf-generation-mode');
            }
            cleanup(iframe);
            resolve(pdfBlob);
          }).catch((error: any) => {
            if (done) return;
            done = true;
            // Remove PDF generation class on error too
            element.classList.remove('pdf-generation-mode');
            const paperElement = element.querySelector('.xyz-paper') as HTMLElement;
            if (paperElement) {
              paperElement.classList.remove('pdf-generation-mode');
            }
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

  /** Single-sheet DOM capture for a host that uses `.xyz-paper` (e.g. expense claim live preview). */
  async renderXyzHostElementToPdf(element: HTMLElement): Promise<Blob> {
    return this.renderXyzPdfFromElement(element);
  }

  private async renderXyzPdfFromElement(element: HTMLElement): Promise<Blob> {
    const papers = Array.from(element.querySelectorAll('.xyz-paper')) as HTMLElement[];
    if (papers.length > 1) {
      return this.renderMultiPageXyzPapersToPdfBlob(element);
    }

    const captureTarget = (papers[0] as HTMLElement) || (element.querySelector('.xyz-paper') as HTMLElement) || element;

    const [html2canvasModule, jsPDFModule] = await Promise.all([
      import('html2canvas'),
      import('jspdf')
    ]);
    const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
    const jsPDF = (jsPDFModule.default || jsPDFModule) as any;

    // Ensure full content is visible for capture.
    const savedStyles: Array<{ el: HTMLElement; prop: string; value: string }> = [];
    const setStyle = (el: HTMLElement | null, prop: string, value: string) => {
      if (!el) return;
      savedStyles.push({ el, prop, value: el.style.getPropertyValue(prop) });
      el.style.setProperty(prop, value, 'important');
    };

    const paper = (captureTarget.querySelector('.xyz-paper') as HTMLElement) || captureTarget;
    const contentArea = captureTarget.querySelector('.xyz-content-area') as HTMLElement;
    const paperLandscape = paper.classList.contains('xyz-paper--landscape');
    const PDF_WIDTH = paperLandscape ? A4_LONG_EDGE_MM : A4_SHORT_EDGE_MM;
    const PDF_HEIGHT = paperLandscape ? A4_SHORT_EDGE_MM : A4_LONG_EDGE_MM;
    const footerPinned = paper.classList.contains('xyz-paper-footer-pinned');
    setStyle(paper, 'overflow', 'visible');
    if (footerPinned) {
      setStyle(paper, 'display', 'flex');
      setStyle(paper, 'flex-direction', 'column');
      setStyle(paper, 'min-height', `${PDF_HEIGHT}mm`);
      setStyle(paper, 'height', 'auto');
      setStyle(paper, 'max-height', 'none');
      const footer = paper.querySelector('.xyz-footer') as HTMLElement | null;
      const spacer = paper.querySelector('.xyz-footer-spacer') as HTMLElement | null;
      if (footer) {
        setStyle(footer, 'flex-shrink', '0');
        setStyle(footer, 'margin-top', '0');
      }
      if (spacer) {
        setStyle(spacer, 'flex', '1 1 auto');
      }
      if (contentArea) {
        setStyle(contentArea, 'overflow', 'visible');
        setStyle(contentArea, 'flex', '0 1 auto');
      }
    } else {
      setStyle(paper, 'height', 'auto');
      setStyle(paper, 'max-height', 'none');
      setStyle(paper, 'min-height', 'auto');
      if (contentArea) {
        setStyle(contentArea, 'overflow', 'visible');
        setStyle(contentArea, 'height', 'auto');
        setStyle(contentArea, 'max-height', 'none');
        setStyle(contentArea, 'min-height', 'auto');
      }
    }

    await new Promise(resolve => setTimeout(resolve, 80));

    const paperRect = paper.getBoundingClientRect();
    const paperWidthPx = Math.max(paperRect.width || paper.scrollWidth || 0, 1);
    const pageHeightPx = Math.max(Math.round((paperWidthPx * PDF_HEIGHT) / PDF_WIDTH), 1);

    // Footer-pinned forms: align total paper height to whole A4 pages so footer lands at bottom of final page.
    if (footerPinned) {
      const naturalHeightPx = Math.max(paper.scrollHeight, paper.offsetHeight, pageHeightPx);
      const snappedHeightPx = Math.ceil(naturalHeightPx / pageHeightPx) * pageHeightPx;
      setStyle(paper, 'height', `${snappedHeightPx}px`);
      setStyle(paper, 'min-height', `${snappedHeightPx}px`);
    }

    await new Promise(resolve => setTimeout(resolve, 50));

    const canvas = await html2canvas(captureTarget, {
      scale: 2,
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff',
      width: captureTarget.scrollWidth,
      height: captureTarget.scrollHeight,
      windowWidth: captureTarget.scrollWidth,
      windowHeight: captureTarget.scrollHeight
    });

    // Restore styles
    savedStyles.forEach(({ el, prop, value }) => {
      if (value) {
        el.style.setProperty(prop, value);
      } else {
        el.style.removeProperty(prop);
      }
    });

    const pdf = new jsPDF({
      orientation: paperLandscape ? 'landscape' : 'portrait',
      unit: 'mm',
      format: 'a4',
      compress: true
    });

    const pdfWidth = PDF_WIDTH;
    const pdfHeight = PDF_HEIGHT;
    const pageSliceHeightPx = Math.max(Math.round((canvas.width * pdfHeight) / pdfWidth), 1);
    const totalPages = Math.max(Math.ceil(canvas.height / pageSliceHeightPx), 1);

    for (let pageIndex = 0; pageIndex < totalPages; pageIndex++) {
      const srcY = pageIndex * pageSliceHeightPx;
      const remaining = canvas.height - srcY;
      const sliceHeightPx = Math.max(Math.min(pageSliceHeightPx, remaining), 1);

      const pageCanvas = document.createElement('canvas');
      pageCanvas.width = canvas.width;
      pageCanvas.height = sliceHeightPx;
      const pageCtx = pageCanvas.getContext('2d');
      if (!pageCtx) {
        throw new Error('Failed to create canvas context for page snapshot');
      }
      pageCtx.fillStyle = '#ffffff';
      pageCtx.fillRect(0, 0, pageCanvas.width, pageCanvas.height);
      pageCtx.drawImage(canvas, 0, srcY, canvas.width, sliceHeightPx, 0, 0, canvas.width, sliceHeightPx);

      const imgData = pageCanvas.toDataURL('image/jpeg', 0.98);
      const renderedHeightMm = (sliceHeightPx * pdfWidth) / canvas.width;

      if (pageIndex > 0) {
        pdf.addPage('a4', paperLandscape ? 'landscape' : 'portrait');
      }
      pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, renderedHeightMm);
    }

    return pdf.output('blob');
  }

  /**
   * Multi-page PDF from the same DOM structure as /application and /application-details previews:
   * one PDF page per `.xyz-paper` (measured A4 pagination for general forms with individual pipeline footer).
   * Matches {@link ApplicationDetailsComponent.generatePdf} non-CAPF path so stored PDF/email previews align with on-screen line breaks.
   * Do not use for CAPF.
   */
  /**
   * Email/stored PDF capture for individual pipeline footer tables: inline styles so html2canvas
   * keeps header and user-name text off the cell borders (stylesheet-only rules are often ignored).
   * See VIM-FE/docs/individual-pipeline-footer-pdf-spacing.md
   */
  private applyIndividualPipelineFooterPdfCaptureStyles(
    root: HTMLElement,
    setStyle: (el: HTMLElement | null, prop: string, value: string) => void
  ): void {
    const tables = Array.from(
      root.querySelectorAll('.xyz-footer .xyz-signatures, .ec-slip-pipeline-footer .xyz-signatures')
    ) as HTMLElement[];
    tables.forEach((table) => {
      const sigCells = Array.from(table.querySelectorAll('tr.xyz-signatures-blank td')) as HTMLElement[];
      sigCells.forEach((cell) => {
        const hasSigContent = !!cell.querySelector('.xyz-sig-img, .xyz-sig-time');
        setStyle(cell, 'vertical-align', 'top');
        setStyle(cell, 'padding-top', '2px');
        setStyle(cell, 'padding-bottom', '2px');
        setStyle(cell, 'height', '46px');
        setStyle(cell, 'min-height', '46px');
        setStyle(cell, 'box-sizing', 'border-box');
        if (hasSigContent) {
          setStyle(cell, 'position', 'relative');
        }

        let wrapper: HTMLElement | null = null;
        for (const child of Array.from(cell.children)) {
          if (child instanceof HTMLElement && !child.classList.contains('pdf-footer-sig-top-spacer')) {
            wrapper = child;
            break;
          }
        }

        if (wrapper && hasSigContent) {
          setStyle(wrapper, 'position', 'absolute');
          setStyle(wrapper, 'top', '1px');
          setStyle(wrapper, 'left', '0');
          setStyle(wrapper, 'right', '0');
          setStyle(wrapper, 'display', 'flex');
          setStyle(wrapper, 'flex-direction', 'column');
          setStyle(wrapper, 'align-items', 'center');
          setStyle(wrapper, 'justify-content', 'flex-start');
          setStyle(wrapper, 'height', 'auto');
          setStyle(wrapper, 'min-height', '0');
          setStyle(wrapper, 'margin', '0');
          setStyle(wrapper, 'padding', '0');
          setStyle(wrapper, 'box-sizing', 'border-box');
        }

        const img = cell.querySelector('.xyz-sig-img') as HTMLElement | null;
        if (img) {
          setStyle(img, 'display', 'block');
          setStyle(img, 'margin', '0 auto 2px auto');
          setStyle(img, 'max-height', '14px');
        }

        const timeEl = cell.querySelector('.xyz-sig-time') as HTMLElement | null;
        if (timeEl) {
          setStyle(timeEl, 'display', 'block');
          setStyle(timeEl, 'margin', '0 auto');
          setStyle(timeEl, 'line-height', '1.1');
          setStyle(timeEl, 'font-size', '7px');
        }
      });

      const headerCells = Array.from(table.querySelectorAll('tr:nth-child(2) th')) as HTMLElement[];
      headerCells.forEach((cell) => {
        setStyle(cell, 'padding-top', '12px');
        setStyle(cell, 'padding-bottom', '12px');
        setStyle(cell, 'line-height', '1.45');
        setStyle(cell, 'vertical-align', 'middle');
        setStyle(cell, 'box-sizing', 'border-box');
      });

      const userRow = table.querySelector('tr:last-child') as HTMLElement | null;
      if (userRow) {
        setStyle(userRow, 'height', '58px');
      }

      const userCells = Array.from(table.querySelectorAll('tr:last-child td')) as HTMLElement[];
      userCells.forEach((cell) => {
        setStyle(cell, 'vertical-align', 'top');
        setStyle(cell, 'padding-top', '10px');
        setStyle(cell, 'padding-bottom', '0');
        setStyle(cell, 'padding-left', '6px');
        setStyle(cell, 'padding-right', '6px');
        setStyle(cell, 'min-height', '58px');
        setStyle(cell, 'height', '58px');
        setStyle(cell, 'line-height', '1.45');
        setStyle(cell, 'box-sizing', 'border-box');

        let inner: HTMLElement | null = null;
        for (const child of Array.from(cell.children)) {
          if (!(child instanceof HTMLElement) || child.classList.contains('pdf-footer-cell-pad')) {
            continue;
          }
          const tag = child.tagName.toLowerCase();
          if (tag === 'span' || tag === 'div') {
            inner = child;
            break;
          }
        }

        if (!inner && (cell.textContent || '').trim()) {
          inner = document.createElement('div');
          inner.className = 'pdf-footer-user-inner';
          while (cell.firstChild) {
            inner.appendChild(cell.firstChild);
          }
          cell.insertBefore(inner, cell.firstChild);
        }

        if (inner) {
          setStyle(inner, 'display', 'block');
          setStyle(inner, 'padding-top', '0');
          setStyle(inner, 'padding-bottom', '0');
          setStyle(inner, 'margin', '0');
          setStyle(inner, 'line-height', '1.45');
          setStyle(inner, 'box-sizing', 'border-box');
        }

        let pad = cell.querySelector('.pdf-footer-cell-pad') as HTMLElement | null;
        if (!pad) {
          pad = document.createElement('div');
          pad.className = 'pdf-footer-cell-pad';
          pad.setAttribute('aria-hidden', 'true');
          cell.appendChild(pad);
        }
        setStyle(pad, 'display', 'block');
        setStyle(pad, 'width', '100%');
        setStyle(pad, 'height', '16px');
        setStyle(pad, 'min-height', '16px');
        setStyle(pad, 'margin', '0');
        setStyle(pad, 'padding', '0');
        setStyle(pad, 'border', '0');
        setStyle(pad, 'box-sizing', 'border-box');
        setStyle(pad, 'background', 'transparent');
        setStyle(pad, 'pointer-events', 'none');
      });
    });
  }

  async renderMultiPageXyzPapersToPdfBlob(container: HTMLElement): Promise<Blob> {
    // Capture from an offscreen clone so live /application preview never changes while snapshot is generated.
    const captureHost = document.createElement('div');
    captureHost.style.position = 'fixed';
    captureHost.style.left = '-100000px';
    captureHost.style.top = '0';
    captureHost.style.opacity = '0';
    captureHost.style.pointerEvents = 'none';
    captureHost.style.zIndex = '-1';
    const cloneRoot = container.cloneNode(true) as HTMLElement;
    const captureScopeId = `pdf-capture-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
    cloneRoot.setAttribute('data-pdf-capture-scope', captureScopeId);
    const captureStyle = document.createElement('style');
    captureStyle.textContent = `
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-rule.thick::before,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-rule.thick::after {
        content: none !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr.xyz-signatures-blank td,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr.xyz-signatures-blank td {
        vertical-align: top !important;
        padding-top: 6px !important;
        padding-bottom: 2px !important;
        height: 46px !important;
        min-height: 46px !important;
        box-sizing: border-box !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr.xyz-signatures-blank td:has(.xyz-sig-img),
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr.xyz-signatures-blank td:has(.xyz-sig-time),
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr.xyz-signatures-blank td:has(.xyz-sig-img),
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr.xyz-signatures-blank td:has(.xyz-sig-time) {
        position: relative !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr.xyz-signatures-blank td > div,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr.xyz-signatures-blank td > div {
        position: absolute !important;
        top: 1px !important;
        left: 0 !important;
        right: 0 !important;
        display: flex !important;
        flex-direction: column !important;
        align-items: center !important;
        justify-content: flex-start !important;
        height: auto !important;
        min-height: 0 !important;
        margin: 0 !important;
        padding: 0 !important;
        box-sizing: border-box !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr.xyz-signatures-blank .xyz-sig-img,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr.xyz-signatures-blank .xyz-sig-img {
        display: block !important;
        margin: 0 auto 2px auto !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr.xyz-signatures-blank .xyz-sig-time,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr.xyz-signatures-blank .xyz-sig-time {
        display: block !important;
        margin: 0 auto !important;
        line-height: 1.1 !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr:nth-child(2) th,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr:nth-child(2) th {
        padding-top: 12px !important;
        padding-bottom: 12px !important;
        line-height: 1.45 !important;
        vertical-align: middle !important;
        box-sizing: border-box !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr:last-child,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr:last-child {
        height: 58px !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr:last-child td,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr:last-child td {
        vertical-align: top !important;
        padding-top: 10px !important;
        padding-bottom: 0 !important;
        min-height: 58px !important;
        height: 58px !important;
        line-height: 1.45 !important;
        box-sizing: border-box !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr:last-child td > span,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures tr:last-child td > div:not(.pdf-footer-cell-pad),
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr:last-child td > span,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures tr:last-child td > div:not(.pdf-footer-cell-pad) {
        display: block !important;
        margin: 0 !important;
        line-height: 1.45 !important;
        box-sizing: border-box !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-footer .xyz-signatures .pdf-footer-cell-pad,
      [data-pdf-capture-scope="${captureScopeId}"] .ec-slip-pipeline-footer .xyz-signatures .pdf-footer-cell-pad {
        display: block !important;
        width: 100% !important;
        height: 16px !important;
        min-height: 16px !important;
        margin: 0 !important;
        padding: 0 !important;
        border: 0 !important;
        box-sizing: border-box !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor table,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper table,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-table {
        border-collapse: collapse !important;
        border-spacing: 0 !important;
        border: none !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor table tbody tr,
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor table thead tr,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper table tbody tr,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper table thead tr,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-table tbody tr {
        border: none !important;
        background: transparent !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor th,
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor td,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper th,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper td,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-table th,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-table td {
        border: none !important;
        border-right: 1px solid #000 !important;
        border-bottom: 1px solid #000 !important;
        box-sizing: border-box !important;
        font-weight: normal !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor tr > :first-child,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper tr > :first-child,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-table tr > :first-child {
        border-left: 1px solid #000 !important;
      }
      [data-pdf-capture-scope="${captureScopeId}"] .ql-editor table > :first-child > tr:first-child > *,
      [data-pdf-capture-scope="${captureScopeId}"] .q-table-wrapper table > :first-child > tr:first-child > *,
      [data-pdf-capture-scope="${captureScopeId}"] .xyz-table > :first-child > tr:first-child > * {
        border-top: 1px solid #000 !important;
      }
    `;
    cloneRoot.prepend(captureStyle);
    captureHost.appendChild(cloneRoot);
    document.body.appendChild(captureHost);

    try {
      const papers = (Array.from(cloneRoot.querySelectorAll('.xyz-paper-page')) as HTMLElement[])
        .filter((paper: HTMLElement) => {
          const cs = window.getComputedStyle(paper);
          if (cs.display === 'none' || cs.visibility === 'hidden' || cs.opacity === '0') {
            return false;
          }
          const rect = paper.getBoundingClientRect();
          return rect.width > 0 && rect.height > 0;
        });
      if (papers.length === 0) {
        throw new Error('No .xyz-paper pages found for multi-page PDF capture');
      }

      const [html2canvasModule, jsPDFModule] = await Promise.all([
        import('html2canvas'),
        import('jspdf')
      ]);
      const html2canvas = (html2canvasModule.default || html2canvasModule) as any;
      const jsPDF = (jsPDFModule.default || jsPDFModule) as any;

      await new Promise(resolve => setTimeout(resolve, 100));

      const defaultLandscape = papers.some((paper) => paper.classList.contains('xyz-paper--landscape'));
      const pdf = new jsPDF({
        orientation: defaultLandscape ? 'landscape' : 'portrait',
        unit: 'mm',
        format: 'a4',
        compress: true
      });
      let firstPdfPage = true;
      let addedPages = 0;

    const hasVisibleInk = (canvasEl: HTMLCanvasElement): boolean => {
      const probe = document.createElement('canvas');
      probe.width = 64;
      probe.height = 64;
      const probeCtx = probe.getContext('2d');
      if (!probeCtx) {
        return true;
      }
      probeCtx.fillStyle = '#ffffff';
      probeCtx.fillRect(0, 0, probe.width, probe.height);
      probeCtx.drawImage(canvasEl, 0, 0, probe.width, probe.height);
      const img = probeCtx.getImageData(0, 0, probe.width, probe.height).data;
      for (let p = 0; p < img.length; p += 4) {
        const a = img[p + 3];
        if (a < 10) continue;
        const r = img[p];
        const g = img[p + 1];
        const b = img[p + 2];
        // Keep anything that is not near-white.
        if (r < 245 || g < 245 || b < 245) {
          return true;
        }
      }
      return false;
    };

      for (let i = 0; i < papers.length; i++) {
        const target = papers[i];
        const paperLandscape = target.classList.contains('xyz-paper--landscape');
        const PDF_WIDTH = paperLandscape ? 297 : 210;
        const PDF_HEIGHT = paperLandscape ? 210 : 297;
        const savedStyles: Array<{ el: HTMLElement; prop: string; value: string }> = [];
        const setStyle = (el: HTMLElement | null, prop: string, value: string) => {
          if (!el) return;
          savedStyles.push({ el, prop, value: el.style.getPropertyValue(prop) });
          el.style.setProperty(prop, value, 'important');
        };

      // PDF-only hardening: force the header separator to render as 3 full-width lines
      // without relying on pseudo-elements (which html2canvas can truncate on the right).
      const thickRules = Array.from(target.querySelectorAll('.xyz-rule.thick')) as HTMLElement[];
      thickRules.forEach((rule) => {
        // Build 3 explicit 1px lines in the capture clone so html2canvas
        // cannot merge/crop pseudo-element based separators.
        rule.innerHTML = '<span class="pdf-rule-line"></span><span class="pdf-rule-line"></span><span class="pdf-rule-line"></span>';
        setStyle(rule, 'display', 'flex');
        setStyle(rule, 'flex-direction', 'column');
        setStyle(rule, 'gap', '1px');
        setStyle(rule, 'height', 'auto');
        setStyle(rule, 'min-height', '5px');
        setStyle(rule, 'background', 'transparent');
        setStyle(rule, 'border', '0');
        setStyle(rule, 'padding', '0');
        setStyle(rule, 'box-sizing', 'border-box');
        setStyle(rule, 'position', 'static');
        setStyle(rule, 'overflow', 'visible');

        const lineEls = Array.from(rule.querySelectorAll('.pdf-rule-line')) as HTMLElement[];
        lineEls.forEach((lineEl, idx) => {
          const isCenter = idx === 1;
          setStyle(lineEl, 'display', 'block');
          setStyle(lineEl, 'width', '100%');
          setStyle(lineEl, 'height', isCenter ? '2px' : '1px');
          setStyle(lineEl, 'min-height', isCenter ? '2px' : '1px');
          setStyle(lineEl, 'background', '#000');
          setStyle(lineEl, 'flex', isCenter ? '0 0 2px' : '0 0 1px');
        });
      });

      // Ensure export uses real paper metrics, not any preview scaling transform.
      setStyle(target, 'transform', 'none');
      setStyle(target, 'transform-origin', 'top left');
      setStyle(target, 'width', `${PDF_WIDTH}mm`);
      setStyle(target, 'max-width', `${PDF_WIDTH}mm`);
      setStyle(target, 'min-width', `${PDF_WIDTH}mm`);
      setStyle(target, 'font-size', '13.5px');
      setStyle(target, 'line-height', '1.35');
      if (paperLandscape) {
        setStyle(target, 'height', `${PDF_HEIGHT}mm`);
        setStyle(target, 'max-height', `${PDF_HEIGHT}mm`);
      }

      const individualSignatureSelector =
        '.xyz-footer .xyz-signatures, .ec-slip-pipeline-footer .xyz-signatures';
      const hasIndividualPipelineSignatures = !!target.querySelector(individualSignatureSelector);
      const footerPinned =
        target.classList.contains('xyz-paper-footer-pinned') ||
        target.classList.contains('xyz-paper--footer-pinned') ||
        (!!target.querySelector('.xyz-footer-spacer') && hasIndividualPipelineSignatures);
      let targetPageHeightPx = 0;
      if (footerPinned) {
        const contentArea = (target.querySelector('.xyz-content-area') ||
          target.querySelector('.xyz-content-body') ||
          target.querySelector('.xyz-generic-content')) as HTMLElement | null;
        const spacer = target.querySelector('.xyz-footer-spacer') as HTMLElement | null;
        const footer = target.querySelector('.xyz-footer') as HTMLElement | null;

        setStyle(target, 'display', 'flex');
        setStyle(target, 'flex-direction', 'column');
        setStyle(target, 'overflow', 'visible');
        setStyle(target, 'min-height', `${PDF_HEIGHT}mm`);
        setStyle(target, 'height', 'auto');
        setStyle(target, 'max-height', paperLandscape ? `${PDF_HEIGHT}mm` : 'none');
        if (contentArea) {
          setStyle(contentArea, 'flex', paperLandscape ? '1 1 auto' : '0 1 auto');
          setStyle(contentArea, 'overflow', paperLandscape ? 'hidden' : 'visible');
        }
        if (spacer) {
          setStyle(spacer, 'flex', '1 1 auto');
          setStyle(spacer, 'min-height', '0');
        }
        if (footer) {
          setStyle(footer, 'flex-shrink', '0');
          setStyle(footer, 'margin-top', spacer ? '0' : 'auto');
        }

        // Keep footer at the bottom of the final page by snapping paper height
        // to full A4 page multiples before image slicing.
        const widthPx = Math.max(target.getBoundingClientRect().width || target.scrollWidth || 0, 1);
        const pageHeightPx = Math.max(Math.round((widthPx * PDF_HEIGHT) / PDF_WIDTH), 1);
        targetPageHeightPx = pageHeightPx;
        const naturalHeightPx = Math.max(
          target.scrollHeight || 0,
          target.offsetHeight || 0,
          Math.round(target.getBoundingClientRect().height || 0),
          pageHeightPx
        );
        const snappedHeightPx = Math.ceil(naturalHeightPx / pageHeightPx) * pageHeightPx;
        setStyle(target, 'height', `${snappedHeightPx}px`);
        setStyle(target, 'min-height', `${snappedHeightPx}px`);
      }

      if (hasIndividualPipelineSignatures) {
        this.applyIndividualPipelineFooterPdfCaptureStyles(target, setStyle);
      }

      await new Promise(resolve => setTimeout(resolve, 50));
      const captureWidthPx = Math.max(
        target.offsetWidth || 0,
        Math.round(target.getBoundingClientRect().width || 0),
        target.scrollWidth || 0,
        1
      );
      const captureHeightPx = Math.max(
        target.offsetHeight || 0,
        Math.round(target.getBoundingClientRect().height || 0),
        target.scrollHeight || 0,
        1
      );
      const canvas = await html2canvas(target, {
        scale: 3,
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff',
        width: captureWidthPx,
        height: captureHeightPx,
        windowWidth: captureWidthPx,
        windowHeight: captureHeightPx
      });

      // Slice each rendered paper into true A4-height snapshots.
      let pageSliceHeightPx = Math.max(Math.round((canvas.width * PDF_HEIGHT) / PDF_WIDTH), 1);
      // For footer-pinned documents, map the exact DOM page-height to canvas pixels.
      // This avoids off-by-some-pixels slicing drift that makes the last page look cut short.
      if (footerPinned && targetPageHeightPx > 0) {
        const domRenderedHeight = Math.max(captureHeightPx, 1);
        const domToCanvasY = canvas.height / domRenderedHeight;
        pageSliceHeightPx = Math.max(Math.round(targetPageHeightPx * domToCanvasY), 1);
      }
      const totalSlices = Math.max(Math.ceil(canvas.height / pageSliceHeightPx), 1);

      for (let sliceIndex = 0; sliceIndex < totalSlices; sliceIndex++) {
        const srcY = sliceIndex * pageSliceHeightPx;
        const remaining = canvas.height - srcY;
        const sliceHeightPx = Math.max(Math.min(pageSliceHeightPx, remaining), 1);
        if (sliceHeightPx < 24) {
          continue;
        }

        const pageCanvas = document.createElement('canvas');
        pageCanvas.width = canvas.width;
        // Keep every PDF page at full A4 frame height; draw remaining content on top.
        // This preserves "footer fixed at bottom of last page" visual alignment.
        pageCanvas.height = pageSliceHeightPx;
        const pageCtx = pageCanvas.getContext('2d');
        if (!pageCtx) {
          throw new Error('Failed to create page canvas context');
        }
        pageCtx.fillStyle = '#ffffff';
        pageCtx.fillRect(0, 0, pageCanvas.width, pageCanvas.height);
        pageCtx.drawImage(canvas, 0, srcY, canvas.width, sliceHeightPx, 0, 0, canvas.width, sliceHeightPx);

        const renderedHeightMm = PDF_HEIGHT;
        const imgData = pageCanvas.toDataURL('image/jpeg', 0.98);
        const nonBlankSlice = hasVisibleInk(pageCanvas);
        if (!nonBlankSlice && addedPages > 0) {
          continue;
        }
        if (!firstPdfPage) {
          pdf.addPage('a4', paperLandscape ? 'landscape' : 'portrait');
        }
        pdf.addImage(imgData, 'JPEG', 0, 0, PDF_WIDTH, renderedHeightMm);
        firstPdfPage = false;
        addedPages += 1;
      }

        savedStyles.forEach(({ el, prop, value }) => {
          if (value) {
            el.style.setProperty(prop, value);
          } else {
            el.style.removeProperty(prop);
          }
        });
      }

      return pdf.output('blob');
    } finally {
      if (captureHost.parentNode) {
        captureHost.parentNode.removeChild(captureHost);
      }
    }
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
    if (form && form.txtFormName && form.txtFormName.trim().toUpperCase().includes('CAPF')) {
      return true;
    }
    if (data?.cfgTblCustomForm && data.cfgTblCustomForm.txtFormName && data.cfgTblCustomForm.txtFormName.trim().toUpperCase().includes('CAPF')) {
      return true;
    }
    const name = (applicationMeta?.formName || '').trim().toUpperCase();
    const code = (applicationMeta?.txtFormCode || '').trim().toUpperCase();
    return name.includes('CAPF') || code.startsWith('CAPF');
  }

  private isExpenseClaimFormMeta(data: any, form: any, applicationMeta?: { formName?: string; txtFormCode?: string }): boolean {
    const codeFromData = (data?.txtFormCode || '').trim().toUpperCase();
    if (codeFromData.startsWith('EXP-')) {
      return true;
    }
    const codeMeta = (applicationMeta?.txtFormCode || '').trim().toUpperCase();
    if (codeMeta.startsWith('EXP-')) {
      return true;
    }
    const codeForm = (form?.txtFormCode || '').trim().toUpperCase();
    if (codeForm.startsWith('EXP-')) {
      return true;
    }
    const name = `${form?.txtFormName || form?.name || applicationMeta?.formName || ''}`.replace(/\s+/g, ' ').toLowerCase();
    if (name.includes('expense claim')) {
      return true;
    }
    const cfgName = `${data?.cfgTblCustomForm?.txtFormName || ''}`.replace(/\s+/g, ' ').toLowerCase();
    return cfgName.includes('expense claim');
  }

  private isTemporaryAdvanceSlipFormMeta(data: any, form: any, applicationMeta?: { formName?: string; txtFormCode?: string }): boolean {
    const codeFromData = (data?.txtFormCode || '').trim().toUpperCase();
    if (codeFromData.startsWith('TAS-')) {
      return true;
    }
    const codeMeta = (applicationMeta?.txtFormCode || '').trim().toUpperCase();
    if (codeMeta.startsWith('TAS-')) {
      return true;
    }
    const codeForm = (form?.txtFormCode || '').trim().toUpperCase();
    if (codeForm.startsWith('TAS-')) {
      return true;
    }
    const name = `${form?.txtFormName || form?.name || applicationMeta?.formName || ''}`.replace(/\s+/g, ' ').toLowerCase();
    if (name.includes('temporary advance')) {
      return true;
    }
    const cfgName = `${data?.cfgTblCustomForm?.txtFormName || ''}`.replace(/\s+/g, ' ').toLowerCase();
    return cfgName.includes('temporary advance');
  }

  /**
   * HTML/PDF snapshot for expense claim slips (EXP-*). Matches portal slip preview; used when DOM capture is unavailable.
   */
  private generateExpenseClaimSlipPdfHtml(
    application: any,
    formFields: any[],
    applicationFormData: any,
    applicationMeta?: { formName?: string; txtFormCode?: string; omitApprovalSignaturesInPdf?: boolean }
  ): string {
    const omitApprovalSignatures = !!applicationMeta?.omitApprovalSignaturesInPdf;
    const escapeHtml = (text: string): string => {
      const div = document.createElement('div');
      div.textContent = text ?? '';
      return div.innerHTML;
    };
    const normalizeFieldType = (fieldType: any): string => String(fieldType || '').toLowerCase().replace(/\s+/g, '_');
    const slugify = (label: string): string =>
      String(label || '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
    const getFieldLabel = (field: any): string =>
      field?.label || field?.txtFieldLabel || field?.name || field?.txtFieldName || 'Field';
    const getFieldValue = (field: any): any => {
      if (!applicationFormData || typeof applicationFormData !== 'object') {
        return null;
      }
      const label = getFieldLabel(field);
      const slug = slugify(label);
      const directKeys = [
        label,
        slug,
        field?.name,
        field?.key,
        field?.fieldName,
        field?.txtFieldName,
        field?.txtFieldLabel,
        field?.txtFieldLabel ? slugify(field.txtFieldLabel) : null,
        field?.serFieldId ? `field_${field.serFieldId}` : null
      ].filter(Boolean);
      for (const key of directKeys) {
        if ((applicationFormData as any)[key] !== undefined) {
          return (applicationFormData as any)[key];
        }
      }
      return null;
    };
    const getHeaderPreview = (...needles: string[]): string => {
      const lowered = needles.map((n) => n.toLowerCase());
      for (const field of formFields || []) {
        const label = getFieldLabel(field).toLowerCase();
        if (!lowered.some((needle) => label.includes(needle))) {
          continue;
        }
        const value = getFieldValue(field);
        if (value !== undefined && value !== null && String(value).trim().length > 0) {
          return String(value);
        }
      }
      return '';
    };
    const slipCell = (v: string | undefined | null): string => {
      const t = (v ?? '').trim();
      return escapeHtml(t.length > 0 ? t : '\u00a0');
    };

    const rawLines = Array.isArray(applicationFormData?.expenseClaimLines) ? applicationFormData.expenseClaimLines : [];
    const rows = rawLines.map((row: any) => ({
      description: row?.description != null ? String(row.description) : '',
      deptName: row?.deptName != null ? String(row.deptName) : row?.deptt_name != null ? String(row.deptt_name) : '',
      sign: row?.sign != null ? String(row.sign) : '',
      amount: row?.amount != null ? String(row.amount) : ''
    }));
    let sum = 0;
    for (const row of rawLines) {
      const raw = String(row?.amount ?? '').trim().replace(/,/g, '');
      const n = parseFloat(raw);
      if (!Number.isNaN(n)) {
        sum += n;
      }
    }
    const hasAmount = rawLines.some((r: any) => String(r?.amount ?? '').trim() !== '');
    const totalDisplay =
      sum !== 0 || hasAmount ? sum.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 }) : '';

    let approvalHistory: any[] = [];
    if (application?.txtApprovalHistory) {
      try {
        approvalHistory = JSON.parse(application.txtApprovalHistory);
      } catch {
        approvalHistory = [];
      }
    }
    let footerFields: any[] = Array.isArray(applicationFormData?.footerFields) ? [...applicationFormData.footerFields] : [];
    if (footerFields.length === 0 && Array.isArray(applicationFormData?.slipApprovalPipeline)) {
      footerFields = [...applicationFormData.slipApprovalPipeline];
    }
    footerFields = footerFields.sort((a: any, b: any) => (Number(a?.order) || 0) - (Number(b?.order) || 0));

    const getUserId = (user: any): number | null => {
      if (!user) {
        return null;
      }
      return user.serUserId || user.userId || user.id || null;
    };
    const getUserSignatureUrl = (user: any, sectionLabel?: string): string => {
      const userId = getUserId(user);
      if (!userId) {
        return '';
      }
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) {
          return false;
        }
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.signaturePath) {
        return '';
      }
      return `${urls.API_URL}getSignature?userId=${userId}`;
    };
    const isUserApproved = (user: any, sectionLabel?: string): boolean => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory.length) {
        return false;
      }
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) {
          return false;
        }
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.signaturePath) {
        return false;
      }
      const action = (entry.action || entry.status || '').toString().toUpperCase();
      if (action === 'REJECTED') {
        return false;
      }
      if (action === 'APPROVED') {
        return true;
      }
      return !!entry.approvedDate;
    };
    const getUserApprovalDate = (user: any, sectionLabel?: string): string => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory.length) {
        return '';
      }
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) {
          return false;
        }
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.approvedDate) {
        return '';
      }
      try {
        const dt = new Date(entry.approvedDate);
        if (isNaN(dt.getTime())) {
          return String(entry.approvedDate);
        }
        return dt.toLocaleString();
      } catch {
        return String(entry.approvedDate);
      }
    };
    const getFooterSlots = (section: any): any[] => {
      const users = Array.isArray(section?.users) ? section.users : [];
      return users.length > 0 ? users : [null];
    };
    const renderUserCell = (user: any, sectionLabel?: string): string => {
      if (omitApprovalSignatures || !user) {
        return '';
      }
      const sigUrl = getUserSignatureUrl(user, sectionLabel);
      const sigDate = getUserApprovalDate(user, sectionLabel);
      const approved = isUserApproved(user, sectionLabel);
      const content = `
        ${approved && sigUrl ? `<img class="xyz-sig-img" src="${sigUrl}" alt="" crossorigin="anonymous" />` : ''}
        ${approved && sigDate ? `<div class="xyz-sig-time">${escapeHtml(sigDate)}</div>` : ''}
      `;
      return approved ? `<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;">${content}</div>` : '';
    };
    const renderFooterUserSlot = (user: any, section: any): string => {
      if (!user) {
        return '&nbsp;';
      }
      const name = user?.txtUserName || user?.userName || user?.name || '';
      const designation = user?.txtDesignation || user?.designation || '';
      let dept = user?.hrTblDepartment?.txtDepartmentName || user?.departmentName || user?.txtDepartmentName || '';
      if (!dept) {
        const userId = getUserId(user);
        const entry = userId ? approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId) : null;
        dept = entry?.departmentName || '';
      }
      const parts = [name, designation, dept].filter((p: string) => !!p);
      return parts.map((p) => escapeHtml(p)).join('<br>');
    };

    const tbodyRows = rows
      .map(
        (row: { description: string; deptName: string; sign: string; amount: string }, i: number) => `
        <tr>
          <td class="text-center">${i + 1}</td>
          <td>${slipCell(row.description)}</td>
          <td>${slipCell(row.deptName)}</td>
          <td>${slipCell(row.sign)}</td>
          <td class="text-right">${slipCell(row.amount)}</td>
        </tr>`
      )
      .join('');
    const totalCell = slipCell(totalDisplay);

    const hasPipelineFooter = footerFields.length > 0;
    const pipelineFooterHtml = hasPipelineFooter
      ? `
      <div class="ec-slip-pipeline-footer">
        <table class="xyz-signatures">
          <tr class="xyz-signatures-blank">
            ${footerFields
              .map((section: any) =>
                getFooterSlots(section)
                  .map(
                    (slotUser: any) =>
                      `<td>${slotUser && isUserApproved(slotUser, section.label) ? renderUserCell(slotUser, section.label) : ''}</td>`
                  )
                  .join('')
              )
              .join('')}
          </tr>
          <tr>
            ${footerFields
              .map(
                (section: any) =>
                  `<th colspan="${getFooterSlots(section).length}">${escapeHtml(section.label || 'New Field')}:</th>`
              )
              .join('')}
          </tr>
          <tr>
            ${footerFields
              .map((section: any) =>
                getFooterSlots(section)
                  .map((slotUser: any) => `<td><div class="xyz-footer-user">${renderFooterUserSlot(slotUser, section)}</div></td>`)
                  .join('')
              )
              .join('')}
          </tr>
        </table>
      </div>`
      : '';

    const formCode = (application?.txtFormCode || applicationMeta?.txtFormCode || '—').toString();

    const slipCss = `
    * { box-sizing: border-box; }
    body { margin: 0; padding: 12px; background: #f3f4f6; }
    .abc-wrapper { width: 210mm; max-width: 100%; margin: 0 auto; text-align: left; }
    .expense-claim-preview-root.xyz-paper.ec-slip {
      font-family: Helvetica, Arial, sans-serif;
      color: #000;
      background: #fff;
      border: 1px solid #cfcfcf;
      padding: 18px 20px 28px !important;
      display: flex !important;
      flex-direction: column !important;
      min-height: 297mm !important;
      max-height: 297mm !important;
      height: 297mm !important;
      overflow: hidden !important;
      box-shadow: 0 2px 6px rgba(0,0,0,0.08);
    }
    .ec-slip-main { flex: 1 1 auto; min-height: 0; overflow: hidden; }
    .ec-slip-footer-fixed { flex: 0 0 auto; width: 100%; padding-top: 6px; background: #fff; }
    .ec-slip-title-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 14px; }
    .ec-slip-title { margin: 0; font-size: 17px; font-weight: 700; letter-spacing: 0.02em; }
    .ec-slip-title-rule { width: 100%; border-bottom: 1px solid #000; margin-top: 6px; margin-bottom: 4px; height: 0; }
    .ec-slip-logo { width: 52px; height: auto; display: block; }
    .ec-slip-meta-rows { margin-top: 12px; margin-bottom: 10px; font-size: 11px; }
    .ec-slip-meta-row { display: grid; grid-template-columns: 1fr 1fr; gap: 18px 32px; margin-bottom: 6px; }
    .ec-slip-field { display: flex; flex-direction: row; align-items: flex-end; gap: 8px; min-width: 0; padding: 4px 10px; }
    .ec-slip-label { flex-shrink: 0; font-weight: 700; text-transform: uppercase; font-size: 9px; white-space: nowrap; }
    .ec-slip-value { flex: 1; min-width: 0; min-height: 1.15em; border-bottom: 1px solid #000; font-size: 11px; padding: 0 2px 2px; word-break: break-word; }
    .expense-claim-preview-table-wrap { display: flex; justify-content: center; width: 100%; margin-top: 18px; }
    .expense-claim-preview-table { width: 100%; max-width: 96%; border-collapse: collapse; font-size: 12px; }
    .expense-claim-preview-table th, .expense-claim-preview-table td { border: 1px solid #000; padding: 6px 8px; vertical-align: middle; }
    .expense-claim-preview-table th { font-weight: 700; text-align: center; background: #f3f4f6; font-size: 11px; }
    .ec-total-row td { border-top-width: 2px; }
    .ec-total-label { text-align: right; font-weight: 700; text-transform: uppercase; font-size: 11px; }
    .text-center { text-align: center; }
    .text-right { text-align: right; }
    .font-bold { font-weight: 700; }
    .ec-slip-pipeline-footer .xyz-signatures { width: 100%; border-collapse: collapse; font-size: 11px; margin-top: 6px; }
    .ec-slip-pipeline-footer .xyz-signatures th, .ec-slip-pipeline-footer .xyz-signatures td { border: 1px solid #000; padding: 4px 6px; text-align: center; vertical-align: middle; }
    .ec-slip-pipeline-footer .xyz-signatures-blank td { height: 48px; vertical-align: top !important; }
    .xyz-sig-img { max-height: 14px; max-width: 85%; display: block; margin: 0 auto; }
    .xyz-sig-time { font-size: 7px; color: #6b7280; }
    .ec-slip-footer-box { display: grid; grid-template-columns: repeat(3, 1fr); border: 1px solid #000; margin-top: 16px; font-size: 9px; }
    .ec-slip-footer-cell { border-right: 1px solid #000; padding: 6px 8px; min-height: 40px; display: flex; flex-direction: column; justify-content: space-between; }
    .ec-slip-footer-cell:last-child { border-right: none; }
    .ec-slip-footer-key { font-weight: 700; }
    .ec-slip-footer-meta { display: flex; justify-content: space-between; margin-top: 8px; font-size: 10px; }
    `;

    return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>${slipCss}</style>
</head>
<body>
  <div class="abc-wrapper">
    <div class="expense-claim-preview-root xyz-paper ec-slip">
      <div class="ec-slip-main">
        <div class="ec-slip-top">
          <div class="ec-slip-title-row">
            <h1 class="ec-slip-title">Expense Claim Slip</h1>
            <div class="ec-slip-logo-wrap"><img class="ec-slip-logo" src="assets/images/qarshi-logo.png" alt="" /></div>
          </div>
          <div class="ec-slip-title-rule" aria-hidden="true"></div>
        </div>
        <div class="ec-slip-meta-rows">
          <div class="ec-slip-meta-row">
            <div class="ec-slip-field"><span class="ec-slip-label">ACCOUNT HEAD:</span><span class="ec-slip-value">${slipCell(getHeaderPreview('account head'))}</span></div>
            <div class="ec-slip-field"><span class="ec-slip-label">DATED:</span><span class="ec-slip-value">${slipCell(getHeaderPreview('dated', 'date'))}</span></div>
          </div>
          <div class="ec-slip-meta-row">
            <div class="ec-slip-field"><span class="ec-slip-label">APPROVED BUDGET HEAD:</span><span class="ec-slip-value">${slipCell(getHeaderPreview('approved budget head', 'budget head'))}</span></div>
            <div class="ec-slip-field"><span class="ec-slip-label">BUDGET PERIOD:</span><span class="ec-slip-value">${slipCell(getHeaderPreview('budget period', 'period'))}</span></div>
          </div>
        </div>
        <div class="expense-claim-preview-table-wrap">
          <table class="expense-claim-preview-table">
            <thead>
              <tr>
                <th rowspan="2" style="width:36px;">S.no.</th>
                <th rowspan="2">Description</th>
                <th colspan="2">EXPENSE CHARGED TO DEPARTMENT</th>
                <th rowspan="2" style="width:72px;">AMOUNT</th>
              </tr>
              <tr><th>DEPTT. NAME</th><th>SIGN.</th></tr>
            </thead>
            <tbody>
              ${tbodyRows}
              <tr class="ec-total-row">
                <td colspan="4" class="ec-total-label">TOTAL</td>
                <td class="text-right font-bold">${totalCell}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="ec-slip-footer-fixed">
        ${pipelineFooterHtml}
        <div class="ec-slip-footer-box">
          <div class="ec-slip-footer-cell"><div class="ec-slip-footer-key">Version:</div><div>1</div></div>
          <div class="ec-slip-footer-cell"><div class="ec-slip-footer-key">Published Date:</div><div>25.11.2022</div></div>
          <div class="ec-slip-footer-cell"><div class="ec-slip-footer-key">Number:</div><div>FIN_BKP_FM-07</div></div>
        </div>
        <div class="ec-slip-footer-meta">
          <span>ID: ${escapeHtml(formCode)}</span>
          <span>Page 1 of 1</span>
        </div>
      </div>
    </div>
  </div>
</body>
</html>`;
  }

  private generateTemporaryAdvanceSlipPdfHtml(
    application: any,
    formFields: any[],
    applicationFormData: any,
    applicationMeta?: { formName?: string; txtFormCode?: string; omitApprovalSignaturesInPdf?: boolean }
  ): string {
    const omitApprovalSignatures = !!applicationMeta?.omitApprovalSignaturesInPdf;
    const escapeHtml = (text: string): string => {
      const div = document.createElement('div');
      div.textContent = text ?? '';
      return div.innerHTML;
    };
    const getFieldLabel = (field: any): string =>
      field?.label || field?.txtFieldLabel || field?.name || field?.txtFieldName || 'Field';
    const slugify = (label: string): string =>
      String(label || '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
    const getFieldValue = (field: any): any => {
      if (!applicationFormData || typeof applicationFormData !== 'object') {
        return null;
      }
      const label = getFieldLabel(field);
      const slug = slugify(label);
      const directKeys = [
        label,
        slug,
        field?.name,
        field?.key,
        field?.fieldName,
        field?.txtFieldName,
        field?.txtFieldLabel,
        field?.txtFieldLabel ? slugify(field.txtFieldLabel) : null,
        field?.serFieldId ? `field_${field.serFieldId}` : null
      ].filter(Boolean);
      for (const key of directKeys) {
        if ((applicationFormData as any)[key] !== undefined) {
          return (applicationFormData as any)[key];
        }
      }
      return null;
    };
    const getHeaderPreview = (...needles: string[]): string => {
      const lowered = needles.map((n) => n.toLowerCase());
      for (const field of formFields || []) {
        const label = getFieldLabel(field).toLowerCase();
        if (!lowered.some((needle) => label.includes(needle))) {
          continue;
        }
        const value = getFieldValue(field);
        if (value !== undefined && value !== null && String(value).trim().length > 0) {
          return String(value);
        }
      }
      return '';
    };
    const slipCell = (v: string | undefined | null): string => {
      const t = (v ?? '').trim();
      return escapeHtml(t.length > 0 ? t : '\u00a0');
    };
    const firstNonEmpty = (a: string, fallback: string): string => {
      const t = (a ?? '').trim();
      return t.length > 0 ? t : fallback;
    };

    let approvalHistory: any[] = [];
    if (application?.txtApprovalHistory) {
      try {
        approvalHistory = JSON.parse(application.txtApprovalHistory);
      } catch {
        approvalHistory = [];
      }
    }
    let footerFields: any[] = Array.isArray(applicationFormData?.footerFields) ? [...applicationFormData.footerFields] : [];
    if (footerFields.length === 0 && Array.isArray(applicationFormData?.slipApprovalPipeline)) {
      footerFields = [...applicationFormData.slipApprovalPipeline];
    }
    footerFields = footerFields.sort((a: any, b: any) => (Number(a?.order) || 0) - (Number(b?.order) || 0));

    const getUserId = (user: any): number | null => {
      if (!user) {
        return null;
      }
      return user.serUserId || user.userId || user.id || null;
    };
    const getUserSignatureUrl = (user: any, sectionLabel?: string): string => {
      const userId = getUserId(user);
      if (!userId) {
        return '';
      }
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) {
          return false;
        }
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.signaturePath) {
        return '';
      }
      return `${urls.API_URL}getSignature?userId=${userId}`;
    };
    const isUserApproved = (user: any, sectionLabel?: string): boolean => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory.length) {
        return false;
      }
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) {
          return false;
        }
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.signaturePath) {
        return false;
      }
      const action = (entry.action || entry.status || '').toString().toUpperCase();
      if (action === 'REJECTED') {
        return false;
      }
      if (action === 'APPROVED') {
        return true;
      }
      return !!entry.approvedDate;
    };
    const getFooterSlots = (section: any): any[] => {
      const users = Array.isArray(section?.users) ? section.users : [];
      return users.length > 0 ? users : [null];
    };
    const renderUserCell = (user: any, sectionLabel?: string): string => {
      if (omitApprovalSignatures || !user) {
        return '';
      }
      const sigUrl = getUserSignatureUrl(user, sectionLabel);
      const approved = isUserApproved(user, sectionLabel);
      const content = approved && sigUrl ? `<img class="xyz-sig-img" src="${sigUrl}" alt="" crossorigin="anonymous" />` : '';
      return approved ? `<div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;">${content}</div>` : '';
    };
    const renderFooterUserSlot = (user: any, section: any): string => {
      if (!user) {
        return '&nbsp;';
      }
      const name = user?.txtUserName || user?.userName || user?.name || '';
      const designation = user?.txtDesignation || user?.designation || '';
      let dept = user?.hrTblDepartment?.txtDepartmentName || user?.departmentName || user?.txtDepartmentName || '';
      if (!dept) {
        const userId = getUserId(user);
        const entry = userId ? approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId) : null;
        dept = entry?.departmentName || '';
      }
      const parts = [name, designation, dept].filter((p: string) => !!p);
      return parts.map((p) => escapeHtml(p)).join('<br>');
    };

    const hasPipelineFooter = footerFields.length > 0;
    const pipelineFooterHtml = hasPipelineFooter
      ? `
      <div class="ec-slip-pipeline-footer">
        <table class="xyz-signatures">
          <tr class="xyz-signatures-blank">
            ${footerFields
              .map((section: any) =>
                getFooterSlots(section)
                  .map(
                    (slotUser: any) =>
                      `<td>${slotUser && isUserApproved(slotUser, section.label) ? renderUserCell(slotUser, section.label) : ''}</td>`
                  )
                  .join('')
              )
              .join('')}
          </tr>
          <tr>
            ${footerFields
              .map(
                (section: any) =>
                  `<th colspan="${getFooterSlots(section).length}">${escapeHtml(section.label || 'New Field')}:</th>`
              )
              .join('')}
          </tr>
          <tr>
            ${footerFields
              .map((section: any) =>
                getFooterSlots(section)
                  .map((slotUser: any) => `<td><div class="xyz-footer-user">${renderFooterUserSlot(slotUser, section)}</div></td>`)
                  .join('')
              )
              .join('')}
          </tr>
        </table>
      </div>`
      : '';

    const dateLine = firstNonEmpty(
      getHeaderPreview('slip date', 'dated', 'form date', 'advance date'),
      application?.dteCreatedDate
        ? new Date(application.dteCreatedDate).toLocaleDateString('en-GB')
        : new Date().toLocaleDateString('en-GB')
    );

    const purposeText = firstNonEmpty(
      getHeaderPreview('for the purpose', 'purpose of', 'purpose'),
      ''
    );

    const slipCss = `
    * { box-sizing: border-box; }
    body { margin: 0; padding: 12px; background: #f3f4f6; }
    .abc-wrapper { width: 210mm; max-width: 100%; margin: 0 auto; text-align: left; }
    .tas-slip-preview-root.xyz-paper.tas-slip {
      font-family: Helvetica, Arial, sans-serif;
      color: #000;
      background: #fff;
      border: 1px solid #000;
      padding: 16px 18px 28px !important;
      display: flex !important;
      flex-direction: column !important;
      min-height: 297mm !important;
      max-height: 297mm !important;
      height: 297mm !important;
      overflow: hidden !important;
    }
    .tas-slip-main { flex: 1 1 auto; min-height: 0; overflow: hidden; }
    .tas-slip-footer-fixed { flex: 0 0 auto; width: 100%; padding-top: 6px; background: #fff; display: flex; flex-direction: column; }
    .tas-slip-company-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .tas-slip-logo { width: 40px; height: auto; display: block; }
    .tas-slip-company-name { font-weight: 700; font-size: 13px; letter-spacing: 0.02em; font-family: "Times New Roman", Times, Georgia, serif; }
    .tas-slip-admin-unified { width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 9px; border: 1px solid #000; margin: 0; }
    .tas-slip-admin-unified col { width: 8.333333%; }
    .tas-slip-admin-unified td { border: 1px solid #000; padding: 5px 7px; vertical-align: top; text-align: left; word-break: break-word; }
    .tas-slip-banner { background: #d8dadc; text-align: center; font-weight: 700; font-size: 11px; font-family: "Times New Roman", Times, Georgia, serif; padding: 7px 8px; margin: 8px 0 10px; border: 1px solid #9ca3af; letter-spacing: 0.06em; }
    .tas-slip-body { font-size: 11px; }
    .tas-slip-field-row { display: flex; align-items: flex-end; gap: 8px; margin-bottom: 10px; }
    .tas-slip-field-row--tight { flex-wrap: wrap; }
    .tas-slip-label { font-weight: 700; font-size: 9px; text-transform: uppercase; flex-shrink: 0; }
    .tas-slip-label--long { font-size: 8px; max-width: 58%; line-height: 1.25; }
    .tas-slip-value { flex: 1; min-width: 0; border-bottom: 1px solid #000; min-height: 1.2em; padding: 0 2px 2px; word-break: break-word; }
    .tas-slip-purpose-label { font-weight: 700; font-size: 9px; text-transform: uppercase; margin: 4px 0 2px; }
    .tas-slip-purpose-line { border-bottom: 1px solid #000; min-height: 1.15em; margin-bottom: 5px; font-size: 11px; white-space: pre-wrap; }
    .tas-slip-sig-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 28px; margin-top: 14px; margin-bottom: 12px; }
    .tas-slip-sig-cell { display: flex; flex-direction: column; align-items: stretch; }
    .tas-slip-sig-line { border-bottom: 1px solid #000; min-height: 28px; width: 100%; }
    .tas-slip-sig-caption { font-size: 8px; font-weight: 700; text-align: center; text-transform: uppercase; margin-bottom: 6px; line-height: 1.25; }
    .tas-slip-bottom-row { width: 100%; table-layout: fixed; border-collapse: collapse; font-size: 9px; margin-top: 4px; }
    .tas-slip-footer-prepared { margin-top: 10px; }
    .tas-slip-footer-prepared.tas-slip-bottom-row td { width: 33.33%; vertical-align: middle; padding: 0; border: 1px solid #000; height: auto; min-height: 36px; }
    .tas-slip-bottom-cell-inner { display: flex; flex-direction: row; flex-wrap: nowrap; align-items: center; gap: 8px; min-height: 28px; padding: 8px 10px; box-sizing: border-box; }
    .tas-slip-bottom-key { font-weight: 700; flex-shrink: 0; text-align: left; text-transform: none; }
    .tas-slip-bottom-sig-slot { flex: 1 1 auto; min-width: 16px; min-height: 1.35em; }
    .ec-slip-pipeline-footer .xyz-signatures { width: 100%; border-collapse: collapse; font-size: 11px; margin-top: 6px; }
    .ec-slip-pipeline-footer .xyz-signatures th, .ec-slip-pipeline-footer .xyz-signatures td { border: 1px solid #000; padding: 4px 6px; text-align: center; vertical-align: middle; }
    .ec-slip-pipeline-footer .xyz-signatures-blank td { height: 48px; vertical-align: top !important; }
    .xyz-sig-img { max-height: 14px; max-width: 85%; display: block; margin: 0 auto; }
    `;

    return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>${slipCss}</style>
</head>
<body>
  <div class="abc-wrapper">
    <div class="tas-slip-preview-root xyz-paper tas-slip">
      <div class="tas-slip-main">
        <div class="tas-slip-company-row">
          <img class="tas-slip-logo" src="assets/images/qarshi-logo.png" alt="" />
          <div class="tas-slip-company-name">QARSHI INDUSTRIES (PVT.) LTD.</div>
        </div>
        <table class="tas-slip-admin tas-slip-admin-unified">
          <colgroup>
            <col /><col /><col /><col /><col /><col /><col /><col /><col /><col /><col /><col />
          </colgroup>
          <tr>
            <td colspan="4">Division: ${slipCell(firstNonEmpty(getHeaderPreview('division'), 'Finance'))}</td>
            <td colspan="4">Department: ${slipCell(firstNonEmpty(getHeaderPreview('department', 'dept'), 'Book Keeping'))}</td>
            <td colspan="4">Section: ${slipCell(firstNonEmpty(getHeaderPreview('section'), '***'))}</td>
          </tr>
          <tr>
            <td colspan="4">Document No. ${slipCell(firstNonEmpty(getHeaderPreview('document no', 'fin-bkp', 'form number'), 'FIN-BKP-FM-06'))}</td>
            <td colspan="4">Original Issue: ${slipCell(firstNonEmpty(getHeaderPreview('original issue'), '01-06-2006'))}</td>
            <td colspan="2">Rev.# ${slipCell(getHeaderPreview('rev #', 'rev.', 'revision'))}</td>
            <td colspan="2">Rev. Date: ${slipCell(getHeaderPreview('rev. date', 'revision date', 'rev date'))}</td>
          </tr>
        </table>
        <div class="tas-slip-banner">TEMPORARY ADVANCE SLIP</div>
        <div class="tas-slip-body">
          <div class="tas-slip-field-row">
            <span class="tas-slip-label">DATE:</span>
            <span class="tas-slip-value">${slipCell(dateLine)}</span>
          </div>
          <div class="tas-slip-field-row">
            <span class="tas-slip-label">PLEASE PAY RS.:</span>
            <span class="tas-slip-value">${slipCell(getHeaderPreview('please pay', 'pay rs', 'amount rs'))}</span>
          </div>
          <div class="tas-slip-field-row">
            <span class="tas-slip-label">RUPEES:</span>
            <span class="tas-slip-value">${slipCell(getHeaderPreview('rupees', 'in words'))}</span>
          </div>
          <div class="tas-slip-field-row">
            <span class="tas-slip-label">TO MR. / MS.:</span>
            <span class="tas-slip-value">${slipCell(getHeaderPreview('to mr', 'payee', 'mr/ms', 'mr / ms'))}</span>
          </div>
          <div class="tas-slip-purpose-label">FOR THE PURPOSE OF:</div>
          <div class="tas-slip-purpose-line">${slipCell(purposeText)}</div>
          <div class="tas-slip-purpose-line">${slipCell('')}</div>
          <div class="tas-slip-purpose-line">${slipCell('')}</div>
          <div class="tas-slip-field-row tas-slip-field-row--tight">
            <span class="tas-slip-label tas-slip-label--long">THE AMOUNT WILL BE ADJUSTED ON OR BEFORE:</span>
            <span class="tas-slip-value">${slipCell(getHeaderPreview('adjusted', 'on or before', 'adjust'))}</span>
          </div>
        </div>
        <div class="tas-slip-sig-grid">
          <div class="tas-slip-sig-cell"><div class="tas-slip-sig-caption">SIGNATURE BY<br/>APPLICANT</div><div class="tas-slip-sig-line"></div></div>
          <div class="tas-slip-sig-cell"><div class="tas-slip-sig-caption">APPROVED BY<br/>FINANCE WING</div><div class="tas-slip-sig-line"></div></div>
          <div class="tas-slip-sig-cell"><div class="tas-slip-sig-caption">RECOMMENDED BY<br/>DEPTT. HEAD</div><div class="tas-slip-sig-line"></div></div>
          <div class="tas-slip-sig-cell"><div class="tas-slip-sig-caption">RECEIVED<br/>BY</div><div class="tas-slip-sig-line"></div></div>
        </div>
      </div>
      <div class="tas-slip-footer-fixed">
        ${pipelineFooterHtml}
        <table class="tas-slip-bottom-row tas-slip-footer-prepared">
          <tr>
            <td><div class="tas-slip-bottom-cell-inner"><span class="tas-slip-bottom-key">Prepared by:</span><span class="tas-slip-bottom-sig-slot"></span></div></td>
            <td><div class="tas-slip-bottom-cell-inner"><span class="tas-slip-bottom-key">Reviewed By:</span><span class="tas-slip-bottom-sig-slot"></span></div></td>
            <td><div class="tas-slip-bottom-cell-inner"><span class="tas-slip-bottom-key">Approved By:</span><span class="tas-slip-bottom-sig-slot"></span></div></td>
          </tr>
        </table>
      </div>
    </div>
  </div>
</body>
</html>`;
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

    const isWordEditorType = (fieldType: string | undefined): boolean => {
      const normalizedType = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
      return normalizedType === 'word_editor' || normalizedType === 'wordeditor' || normalizedType === 'rich_text' || normalizedType === 'richtext';
    };
    const isAttachmentFieldType = (fieldType: string | undefined): boolean => {
      const t = (fieldType || '').toLowerCase().replace(/\s+/g, '_');
      return t === 'attachment' || t === 'file' || t === 'multi_attachment';
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

    const getFieldDisplayHtml = (field: any): string => {
      const value = formatFieldValue(field, getFieldValue(field));
      if (isWordEditorType(field.type) && value !== '-') {
        return `<div class="word-editor-value"><div class="ql-editor">${String(value)}</div></div>`;
      }
      return escapeHtml(String(value));
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
    .word-editor-value {
      font-size: 11px;
      line-height: 1.45;
    }
    .word-editor-value .ql-editor {
      padding: 0;
    }
    .word-editor-value p {
      margin: 0 0 6px 0;
    }
    .word-editor-value ul,
    .word-editor-value ol {
      margin: 0 0 6px 18px;
      padding: 0;
    }
    .word-editor-value table {
      width: 100%;
      border-collapse: collapse;
      margin: 6px 0;
    }
    .word-editor-value td,
    .word-editor-value th {
      border: 1px solid #d9d9d9;
      padding: 4px 6px;
    }
    .word-editor-value .ql-align-center { text-align: center; }
    .word-editor-value .ql-align-right { text-align: right; }
    .word-editor-value .ql-align-justify { text-align: justify; }
    .word-editor-value .ql-direction-rtl { direction: rtl; text-align: inherit; }
    .word-editor-value .ql-size-small { font-size: 0.75em; }
    .word-editor-value .ql-size-large { font-size: 1.5em; }
    .word-editor-value .ql-size-huge { font-size: 2.5em; }
    .word-editor-value .ql-indent-1 { padding-left: 3em; }
    .word-editor-value .ql-indent-2 { padding-left: 6em; }
    .word-editor-value .ql-indent-3 { padding-left: 9em; }
    .word-editor-value .ql-indent-4 { padding-left: 12em; }
    .word-editor-value .ql-indent-5 { padding-left: 15em; }
    .word-editor-value .ql-indent-6 { padding-left: 18em; }
    .word-editor-value .ql-indent-7 { padding-left: 21em; }
    .word-editor-value .ql-indent-8 { padding-left: 24em; }
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

  ${formFields.filter((f: any) => !isAttachmentFieldType(f.type)).length > 0 ? `
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
        ${formFields.filter((f: any) => !isAttachmentFieldType(f.type)).map((field: any) => `
          <tr>
            <td><strong>${escapeHtml(field.label)}${field.required ? ' <span style="color: #e74c3c;">*</span>' : ''}</strong></td>
            <td>${getFieldDisplayHtml(field)}</td>
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

  private generateBudgetApprovalPdfHtml(
    application: any,
    formFields: any[],
    applicationFormData: any,
    formName: string,
    applicationMeta?: { omitApprovalSignaturesInPdf?: boolean }
  ): string {
    const { heading, contentHtml, preparedBy, reviewers, recommenders, approver, footerFields } = this.buildBudgetApprovalContent(applicationFormData);
    
    // Extract document_header field value for heading if available
    let headingText = heading || formName || 'Budget Approval';
    const normalizeFieldType = (fieldType: any): string => String(fieldType || '').toLowerCase().replace(/\s+/g, '_');
    const getFieldLabel = (field: any): string => field?.label || field?.txtFieldLabel || field?.name || field?.txtFieldName || 'Field';
    const getFieldType = (field: any): string => normalizeFieldType(field?.type || field?.txtFieldType || field?.fieldType);
    const isDocumentHeaderType = (fieldType: string): boolean => {
      const t = normalizeFieldType(fieldType);
      return t === 'document_header';
    };
    const slugify = (label: string): string =>
      String(label || '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
    const getFieldValue = (field: any): any => {
      if (!applicationFormData || typeof applicationFormData !== 'object') return null;
      const label = getFieldLabel(field);
      const slug = slugify(label);
      const directKeys = [
        label,
        slug,
        field?.name,
        field?.key,
        field?.fieldName,
        field?.txtFieldName,
        field?.txtFieldLabel,
        field?.txtFieldLabel ? slugify(field.txtFieldLabel) : null,
        field?.serFieldId ? `field_${field.serFieldId}` : null
      ].filter(Boolean);
      for (const key of directKeys) {
        if ((applicationFormData as any)[key] !== undefined) {
          return (applicationFormData as any)[key];
        }
      }
      return null;
    };
    
    const headerField = (formFields || []).find((field: any) => isDocumentHeaderType(getFieldType(field)));
    if (headerField) {
      const headerValue = getFieldValue(headerField);
      if (headerValue !== null && headerValue !== undefined && String(headerValue).trim() !== '') {
        headingText = String(headerValue).trim();
      }
    }
    
    const dateStr = application?.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleDateString() : new Date().toLocaleDateString();
    const formOrientation = resolveFormOrientation(applicationFormData, formFields);
    return this.generateBudgetApprovalXyzHtml(
      headingText,
      dateStr,
      contentHtml,
      application?.txtApprovalHistory,
      preparedBy,
      reviewers,
      recommenders,
      approver,
      footerFields || [],
      {
        ...applicationMeta,
        formOrientation,
        contentBlocks: contentHtml.trim() ? [contentHtml] : [],
      }
    );
  }

  private generateGenericApplicationPdfHtml(
    application: any,
    formFields: any[],
    applicationFormData: any,
    formName: string,
    applicationMeta?: { omitApprovalSignaturesInPdf?: boolean }
  ): string {
    const normalizeFieldType = (fieldType: any): string => String(fieldType || '').toLowerCase().replace(/\s+/g, '_');
    const slugify = (label: string): string =>
      String(label || '')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
    const escapeHtml = (text: string): string => {
      const div = document.createElement('div');
      div.textContent = text || '';
      return div.innerHTML;
    };
    const getFieldLabel = (field: any): string =>
      field?.label || field?.txtFieldLabel || field?.name || field?.txtFieldName || 'Field';
    const getFieldType = (field: any): string =>
      normalizeFieldType(field?.type || field?.txtFieldType || field?.fieldType);
    const isWordEditorType = (fieldType: string): boolean => {
      const t = normalizeFieldType(fieldType);
      return t === 'word_editor' || t === 'wordeditor' || t === 'rich_text' || t === 'richtext';
    };
    const isIndividualFooterType = (fieldType: string): boolean => {
      const t = normalizeFieldType(fieldType);
      return t === 'individual_pipeline_footer';
    };
    const isDocumentHeaderType = (fieldType: string): boolean => {
      const t = normalizeFieldType(fieldType);
      return t === 'document_header';
    };
    const isAttachmentFieldType = (fieldType: string): boolean => {
      const t = normalizeFieldType(fieldType);
      return t === 'attachment' || t === 'file' || t === 'multi_attachment';
    };
    const getFieldValue = (field: any): any => {
      if (!applicationFormData || typeof applicationFormData !== 'object') return null;
      const label = getFieldLabel(field);
      const slug = slugify(label);
      const directKeys = [
        label,
        slug,
        field?.name,
        field?.key,
        field?.fieldName,
        field?.txtFieldName,
        field?.txtFieldLabel,
        field?.txtFieldLabel ? slugify(field.txtFieldLabel) : null,
        field?.serFieldId ? `field_${field.serFieldId}` : null
      ].filter(Boolean);
      for (const key of directKeys) {
        if ((applicationFormData as any)[key] !== undefined) {
          return (applicationFormData as any)[key];
        }
      }
      return null;
    };
    const normalizeWordEditorHtml = (rawHtml: any): string => {
      const html = String(rawHtml || '').trim();
      if (!html) return '';
      const wrapper = document.createElement('div');
      wrapper.innerHTML = html;
      wrapper.querySelectorAll('textarea').forEach((el) => {
        const ta = el as HTMLTextAreaElement;
        const text = (ta.value || ta.textContent || '').trim();
        const span = document.createElement('span');
        span.textContent = text;
        ta.replaceWith(span);
      });
      wrapper.querySelectorAll('script, style, button, input, textarea, select').forEach((el) => el.remove());
      wrapper.querySelectorAll('td, th').forEach((cell) => {
        const text = (cell.textContent || '').replace(/\u00a0/g, ' ').trim();
        const hasNode = !!cell.querySelector('img, table, ul, ol, div, p, span');
        if (!text && !hasNode) {
          cell.innerHTML = '<span style="display:block;min-height:1.35em;line-height:1.35;">&nbsp;</span>';
        }
      });
      return wrapper.innerHTML;
    };

    const contentBlocks: string[] = [];
    let dynamicFooterFields: any[] = Array.isArray(applicationFormData?.footerFields) ? applicationFormData.footerFields : [];

    (formFields || []).forEach((field: any) => {
      const fieldType = getFieldType(field);
      const label = getFieldLabel(field);

      if (isDocumentHeaderType(fieldType) || fieldType === 'footer' || isAttachmentFieldType(fieldType)) {
        return;
      }

      const value = getFieldValue(field);

      if (isIndividualFooterType(fieldType)) {
        if ((!dynamicFooterFields || dynamicFooterFields.length === 0) && value) {
          if (typeof value === 'string') {
            try {
              const parsed = JSON.parse(value);
              dynamicFooterFields = Array.isArray(parsed?.sections) ? parsed.sections : [];
            } catch {
              dynamicFooterFields = [];
            }
          } else if (typeof value === 'object' && Array.isArray((value as any).sections)) {
            dynamicFooterFields = (value as any).sections;
          }
        }
        return;
      }

      if (isWordEditorType(fieldType)) {
        const html = normalizeWordEditorHtml(value);
        if (html) {
          contentBlocks.push(`<div class="xyz-generic-word"><div class="ql-editor">${html}</div></div>`);
        }
        return;
      }

      if (value === null || value === undefined || value === '') {
        return;
      }

      let displayValue = '';
      if (typeof value === 'string') {
        displayValue = escapeHtml(value);
      } else if (typeof value === 'number' || typeof value === 'boolean') {
        displayValue = escapeHtml(String(value));
      } else {
        displayValue = escapeHtml(JSON.stringify(value));
      }

      contentBlocks.push(`
        <div class="xyz-generic-field">
          <div class="xyz-generic-label">${escapeHtml(label)}</div>
          <div class="xyz-generic-value">${displayValue}</div>
        </div>
      `);
    });

    const contentHtml = contentBlocks.join('');
    
    // Extract document_header field value for heading
    let headingText = formName || 'Application Form';
    const headerField = (formFields || []).find((field: any) => isDocumentHeaderType(getFieldType(field)));
    if (headerField) {
      const headerValue = getFieldValue(headerField);
      if (headerValue !== null && headerValue !== undefined && String(headerValue).trim() !== '') {
        headingText = String(headerValue).trim();
      }
    }
    headingText = headingText.trim();
    
    const dateStr = application?.dteCreatedDate ? new Date(application.dteCreatedDate).toLocaleDateString() : new Date().toLocaleDateString();
    const individualPipelineFooter = Array.isArray(dynamicFooterFields) && dynamicFooterFields.length > 0;
    const formOrientation = resolveFormOrientation(applicationFormData, formFields);
    return this.generateBudgetApprovalXyzHtml(
      headingText,
      dateStr,
      contentHtml,
      application?.txtApprovalHistory,
      undefined,
      [],
      [],
      undefined,
      Array.isArray(dynamicFooterFields) ? dynamicFooterFields : [],
      {
        ...applicationMeta,
        individualPipelineFooter,
        formOrientation,
        contentBlocks,
      }
    );
  }

  private estimateHtmlTextLength(html: string): number {
    const div = document.createElement('div');
    div.innerHTML = html;
    return (div.textContent || '').replace(/\s+/g, ' ').trim().length;
  }

  private splitHtmlBlockForEmailPagination(blockHtml: string, maxChars: number): string[] {
    const trimmed = String(blockHtml || '').trim();
    if (!trimmed) {
      return [];
    }
    if (this.estimateHtmlTextLength(trimmed) <= maxChars) {
      return [trimmed];
    }

    const wrapper = document.createElement('div');
    wrapper.innerHTML = trimmed;
    const outerEl = wrapper.firstElementChild as HTMLElement | null;
    const isGenericWord = !!outerEl?.classList.contains('xyz-generic-word');
    const innerContainer = isGenericWord && outerEl
      ? ((outerEl.querySelector('.ql-editor') as HTMLElement) || outerEl)
      : (outerEl || wrapper);

    const blocks = Array.from(innerContainer.childNodes).filter((node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        return (node.textContent || '').trim().length > 0;
      }
      if (node.nodeType !== Node.ELEMENT_NODE) {
        return false;
      }
      const tag = (node as Element).tagName.toLowerCase();
      return tag === 'p' || tag === 'div' || tag === 'table' || tag === 'ul' || tag === 'ol' || tag.startsWith('h');
    });

    if (blocks.length <= 1) {
      return [trimmed];
    }

    const wrapChunk = (inner: string): string => {
      if (isGenericWord) {
        return `<div class="xyz-generic-word"><div class="ql-editor">${inner}</div></div>`;
      }
      return inner;
    };

    const chunks: string[] = [];
    let currentHtml = '';
    let currentChars = 0;

    for (const node of blocks) {
      const nodeHtml =
        node.nodeType === Node.TEXT_NODE
          ? `<p>${(node.textContent || '').trim()}</p>`
          : (node as Element).outerHTML;
      const nodeLen = this.estimateHtmlTextLength(nodeHtml);

      if (currentChars > 0 && currentChars + nodeLen > maxChars) {
        chunks.push(wrapChunk(currentHtml));
        currentHtml = '';
        currentChars = 0;
      }

      currentHtml += nodeHtml;
      currentChars += nodeLen;
    }

    if (currentHtml) {
      chunks.push(wrapChunk(currentHtml));
    }

    return chunks.length ? chunks : [trimmed];
  }

  private paginateEmailContentBlocks(blocks: string[], orientation: FormOrientation): string[][] {
    const maxChars = orientation === 'landscape' ? 1500 : 2200;
    const expanded: string[] = [];
    for (const block of blocks) {
      expanded.push(...this.splitHtmlBlockForEmailPagination(block, maxChars));
    }

    const pages: string[][] = [];
    let current: string[] = [];
    let currentChars = 0;

    for (const part of expanded) {
      const partLen = this.estimateHtmlTextLength(part);
      if (current.length > 0 && currentChars + partLen > maxChars) {
        pages.push(current);
        current = [];
        currentChars = 0;
      }
      current.push(part);
      currentChars += partLen;
    }

    if (current.length) {
      pages.push(current);
    }

    if (!pages.length) {
      return [[]];
    }

    return this.trimLastEmailPageForFooter(pages, orientation);
  }

  /** Move overflow off the last page so the pipeline footer does not overlap body content. */
  private trimLastEmailPageForFooter(pages: string[][], orientation: FormOrientation): string[][] {
    const maxChars = orientation === 'landscape' ? 1500 : 2200;
    const footerReserve = orientation === 'landscape' ? 500 : 400;
    const lastPageLimit = Math.max(700, maxChars - footerReserve);
    const result = pages.map((page) => [...page]);
    let lastPage = result[result.length - 1];
    let lastChars = lastPage.reduce((sum, block) => sum + this.estimateHtmlTextLength(block), 0);

    while (lastChars > lastPageLimit && lastPage.length > 1) {
      const moved = lastPage.pop();
      if (!moved) {
        break;
      }
      lastChars -= this.estimateHtmlTextLength(moved);
      result.splice(result.length - 1, 0, [moved]);
      lastPage = result[result.length - 1];
      lastChars = lastPage.reduce((sum, block) => sum + this.estimateHtmlTextLength(block), 0);
    }

    return result;
  }

  private generateBudgetApprovalXyzHtml(
    headingText: string,
    dateStr: string,
    contentHtml: string,
    approvalHistoryJson?: string,
    preparedBy?: any,
    reviewers: any[] = [],
    recommenders: any[] = [],
    approver?: any,
    footerFields: any[] = [],
    applicationMeta?: {
      omitApprovalSignaturesInPdf?: boolean;
      individualPipelineFooter?: boolean;
      formOrientation?: FormOrientation;
      contentBlocks?: string[];
    }
  ): string {
    const omitApprovalSignatures = !!applicationMeta?.omitApprovalSignaturesInPdf;
    const isIndividualPipelineEmail = !!applicationMeta?.individualPipelineFooter;
    const orientation: FormOrientation = applicationMeta?.formOrientation ?? 'portrait';
    const isLandscape = orientation === 'landscape';
    const { widthMm: paperWidthMm, heightMm: paperHeightMm } = getA4PaperSizeMm(orientation);
    const rawContentBlocks =
      applicationMeta?.contentBlocks?.length
        ? applicationMeta.contentBlocks
        : (contentHtml.trim() ? [contentHtml] : []);
    const contentPages = this.paginateEmailContentBlocks(rawContentBlocks, orientation);
    const multiPage = contentPages.length > 1;
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

    const getUserSignatureUrl = (user: any, sectionLabel?: string): string => {
      const userId = getUserId(user);
      if (!userId) return '';
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) return false;
        // If section label is provided, match by role (section label)
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.signaturePath) return '';
      return `${urls.API_URL}getSignature?userId=${userId}`;
    };

    const isUserApproved = (user: any, sectionLabel?: string): boolean => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory || approvalHistory.length === 0) return false;
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) return false;
        // If section label is provided, match by role (section label)
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry) return false;
      if (!entry.signaturePath) return false;
      const action = (entry.action || entry.status || '').toString().toUpperCase();
      if (action === 'REJECTED') return false;
      if (action === 'APPROVED') return true;
      return !!entry.approvedDate;
    };

    const getUserApprovalDate = (user: any, sectionLabel?: string): string => {
      const userId = getUserId(user);
      if (!userId || !approvalHistory || approvalHistory.length === 0) return '';
      const entry = approvalHistory.find((e: any) => {
        const entryUserId = e.approvedBy || e.userId;
        if (entryUserId !== userId) return false;
        // If section label is provided, match by role (section label)
        if (sectionLabel) {
          const entryRole = (e.role || '').toString().trim();
          return entryRole.toUpperCase() === sectionLabel.toUpperCase();
        }
        return true;
      });
      if (!entry || !entry.approvedDate) return '';
      try {
        const dt = new Date(entry.approvedDate);
        if (isNaN(dt.getTime())) return String(entry.approvedDate);
        return dt.toLocaleString();
      } catch (e) {
        return String(entry.approvedDate);
      }
    };

    const renderUserCell = (user: any, sectionLabel?: string): string => {
      if (omitApprovalSignatures) {
        return '';
      }
      if (!user) return '';
      const sigUrl = getUserSignatureUrl(user, sectionLabel);
      const sigDate = getUserApprovalDate(user, sectionLabel);
      const approved = isUserApproved(user, sectionLabel);
      const content = `
        ${approved && sigUrl ? `<img class="xyz-sig-img" src="${sigUrl}" alt="Signature" crossorigin="anonymous" />` : ''}
        ${approved && sigDate ? `<div class="xyz-sig-time">${sigDate}</div>` : ''}
      `;
      return approved ? `<div style="display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%;">${content}</div>` : '';
    };

    const renderUserNameCell = (user: any, fallbackName?: string, fallbackRole?: string): string => {
      if (!user && !fallbackName) return '';
      const name = user?.txtUserName || fallbackName || '';
      const role = user?.cfgTblRole?.txtRoleName || fallbackRole || '';
      let dept = user?.hrTblDepartment?.txtDepartmentName || user?.departmentName || user?.txtDepartmentName || '';
      if (!dept) {
        const userId = getUserId(user);
        const entry = userId ? approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId) : null;
        dept = entry?.departmentName || '';
      }
      return `<div>${name}${role ? `<br>(${role})` : ''}${dept ? `<br>${dept}` : ''}</div>`;
    };
    const getFooterSlots = (section: any): any[] => {
      const users = Array.isArray(section?.users) ? section.users : [];
      return users.length > 0 ? users : [null];
    };
    const renderFooterSignatureSlot = (user: any, sectionLabel?: string): string => {
      if (!user || !isUserApproved(user, sectionLabel)) return '';
      return renderUserCell(user, sectionLabel);
    };
    const renderFooterUserSlot = (user: any): string => {
      if (!user) return '&nbsp;';
      const name = user?.txtUserName || user?.userName || user?.name || '';
      const designation = user?.txtDesignation || user?.designation || '';
      let dept = user?.hrTblDepartment?.txtDepartmentName || user?.departmentName || user?.txtDepartmentName || '';
      if (!dept) {
        const userId = getUserId(user);
        const entry = userId ? approvalHistory.find((e: any) => e.approvedBy === userId || e.userId === userId) : null;
        dept = entry?.departmentName || '';
      }
      const parts = [name, designation, dept].filter((p: string) => !!p);
      const safe = parts.map((p: string) => {
        const div = document.createElement('div');
        div.textContent = p;
        return div.innerHTML;
      });
      const inner = safe.join('<br>');
      if (isIndividualPipelineEmail) {
        return `<div class="xyz-footer-user-text">${inner}</div>`;
      }
      return inner;
    };
    const hasDynamicFooter = Array.isArray(footerFields) && footerFields.length > 0;
    const useFooterPinned = hasDynamicFooter && !multiPage && !isLandscape;
    const paperExtraClass = isIndividualPipelineEmail ? ' xyz-individual-pipeline-email' : '';
    const css = `
    * { box-sizing: border-box; }
    body { margin: 0; padding: 0; background:#ffffff; color:#000; text-align: center; }
    .abc-wrapper { width: ${paperWidthMm}mm; max-width: 100%; margin-left: auto; margin-right: auto; text-align: left; }
    .abc-wrapper.xyz-orientation-landscape { width: ${A4_LONG_EDGE_MM}mm; max-width: ${A4_LONG_EDGE_MM}mm; }
    .xyz-paper.xyz-paper--landscape {
      width: ${A4_LONG_EDGE_MM}mm;
      min-width: ${A4_LONG_EDGE_MM}mm;
      max-width: ${A4_LONG_EDGE_MM}mm;
      height: ${A4_SHORT_EDGE_MM}mm;
      min-height: ${A4_SHORT_EDGE_MM}mm;
      max-height: ${A4_SHORT_EDGE_MM}mm;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .xyz-paper.xyz-paper--landscape .xyz-content-area {
      flex: 1 1 auto;
      min-height: 0;
      overflow: hidden;
    }
    .xyz-paper.xyz-paper--landscape > .xyz-footer {
      flex-shrink: 0;
      margin-top: 0;
    }
    .xyz-paper.xyz-paper-page {
      page-break-after: always;
      break-after: page;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }
    .xyz-paper.xyz-paper-page:not(.xyz-paper--landscape) {
      height: ${A4_LONG_EDGE_MM}mm;
      min-height: ${A4_LONG_EDGE_MM}mm;
      max-height: ${A4_LONG_EDGE_MM}mm;
    }
    .xyz-paper.xyz-paper-page .xyz-content-area {
      flex: 1 1 auto;
      min-height: 0;
      overflow: hidden;
    }
    .xyz-paper.xyz-paper-page > .xyz-footer {
      flex-shrink: 0;
      margin-top: 0;
    }
    .xyz-page { background:#ffffff; padding: 0; display:block; }
    .xyz-paper {
      width: 100%;
      background:#ffffff;
      font-family: "Times New Roman", Times, serif;
      font-size: 13.5px;
      line-height: 1.35;
      border: none;
      padding: 10mm 10mm 10mm 12mm;
      display:flex;
      flex-direction:column;
      min-height: ${paperHeightMm}mm;
      overflow: visible;
      position: relative;
    }
    @media print {
      .xyz-paper {
        height: auto !important;
        min-height: auto !important;
        max-height: none !important;
        overflow: visible !important;
      }
    }
    .pdf-generation-mode .xyz-paper,
    .pdf-generation-mode.xyz-paper {
      height: auto !important;
      min-height: auto !important;
      max-height: none !important;
      overflow: visible !important;
      display: block !important;
    }
    .pdf-generation-mode .xyz-header-container {
      display: block !important;
      position: static !important;
      flex-shrink: 0 !important;
    }
    .pdf-generation-mode .xyz-content-area {
      overflow: visible !important;
      flex: none !important;
      min-height: auto !important;
      height: auto !important;
      display: block !important;
      width: 100% !important;
    }
    .pdf-generation-mode .xyz-footer {
      display: block !important;
      position: static !important;
      flex-shrink: 0 !important;
      margin-top: auto !important;
    }
    .pdf-generation-mode.xyz-paper.xyz-paper-footer-pinned,
    .pdf-generation-mode .xyz-paper.xyz-paper-footer-pinned {
      display: flex !important;
      flex-direction: column !important;
      min-height: ${paperHeightMm}mm !important;
    }
    .pdf-generation-mode .xyz-paper.xyz-paper--landscape {
      height: ${A4_SHORT_EDGE_MM}mm !important;
      min-height: ${A4_SHORT_EDGE_MM}mm !important;
      max-height: ${A4_SHORT_EDGE_MM}mm !important;
      overflow: hidden !important;
    }
    .pdf-generation-mode .xyz-paper-footer-pinned > .xyz-content-area {
      flex: 0 1 auto !important;
    }
    .pdf-generation-mode .xyz-paper-footer-pinned > .xyz-footer-spacer {
      flex: 1 1 auto !important;
    }
    .pdf-generation-mode .xyz-rule.thick,
    .xyz-paper.pdf-generation-mode .xyz-rule.thick {
      height: 1px !important;
      background: #000 !important;
      border-top: 1px solid #000 !important;
      border-bottom: 1px solid #000 !important;
      padding: 1px 0 !important;
      box-sizing: content-box !important;
      position: static !important;
      overflow: visible !important;
    }
    .pdf-generation-mode .xyz-rule.thick::before,
    .pdf-generation-mode .xyz-rule.thick::after,
    .xyz-paper.pdf-generation-mode .xyz-rule.thick::before,
    .xyz-paper.pdf-generation-mode .xyz-rule.thick::after {
      content: none !important;
    }
    .xyz-header-container {
      flex-shrink: 0;
      background:#ffffff;
      padding-bottom: 6px;
      page-break-inside: avoid;
      break-inside: avoid;
      position: sticky;
      top: 0;
      z-index: 10;
      width: 100%;
    }
    @media print {
      .xyz-header-container {
        position: static !important;
      }
    }
    .xyz-date-row { display:flex; justify-content:flex-end; margin-bottom:6px; }
    .xyz-date { font-size:14px; text-align:right; }
    .xyz-header { display:grid; grid-template-columns:90px 1fr 90px; align-items:end; column-gap:12px; margin-bottom:6px; }
    .xyz-logo { align-self:start; margin-top:-10px; }
    .xyz-logo img { width:65px; height:auto; display:block; }
    .xyz-company { text-align:center; margin:0; }
    .xyz-header-spacer { height:1px; }
    .xyz-company-name { font-family: "Book Antiqua", "Palatino Linotype", Palatino, "Times New Roman", serif; font-size:30px; font-weight:700; }
    .xyz-company-address { font-family: Verdana, Arial, sans-serif; font-size:12px; color:#000; margin-top:2px; }
    .xyz-rule { height:1px; background:#000; margin:8px 0 12px 0; }
    .xyz-rule.thick { position:relative; height:2px; background:#000; overflow:visible; }
    .xyz-rule.thick::before, .xyz-rule.thick::after { content:""; position:absolute; left:0; width:100%; height:1px; background:#000; }
    .xyz-rule.thick::before { top:-2px; }
    .xyz-rule.thick::after { bottom:-2px; }
    .xyz-title { text-align:center; font-family: "Book Antiqua", "Palatino Linotype", Palatino, "Times New Roman", serif; font-weight:700; font-size:26px; line-height:1.25; margin:6px 0 16px 0; }
    .xyz-content-area {
      flex: 1 1 auto;
      overflow: visible;
      min-height: 0;
    }
    @media print {
      .xyz-content-area {
        overflow: visible !important;
        flex: none !important;
        min-height: auto !important;
        height: auto !important;
      }
    }
    .xyz-dynamic { margin-top:4px; }
    .xyz-section { margin-bottom:10px; page-break-inside: avoid; break-inside: avoid; }
    .xyz-section-title { font-family: "Georgia", "Times New Roman", serif; font-size:13.5px; font-weight:700; text-transform:uppercase; letter-spacing:0.4px; border-left:3px solid var(--accent); padding-left:8px; margin-bottom:4px; }
    .xyz-section-text { font-family: "Georgia", "Times New Roman", serif; font-size:13.5px; font-weight:400; text-align:justify; color:var(--ink); }
    .xyz-list { margin: 4px 0 0 18px; padding: 0; }
    .xyz-list li { margin-bottom: 6px; }
    .xyz-bold { font-weight:700; }
    .xyz-table { width:100%; border-collapse:collapse; margin:10px 0; font-size:13px; page-break-inside: avoid; break-inside: avoid; }
    .xyz-table th, .xyz-table td { border:1px solid #000; padding:2px 4px; line-height:1.15; }
    .xyz-table thead th { background:#8bc34a; text-align:center; font-weight:700; }
    .xyz-table tbody td:first-child, .xyz-table tbody td:last-child { text-align:center; }
    .xyz-col-sr { width:8%; text-align:center; }
    .xyz-col-amount { width:18%; text-align:center; }
    .xyz-note { margin-top:6px; font-size:11px; }
    .xyz-footer-spacer {
      flex: 1 1 auto;
      min-height: 0;
      width: 100%;
    }
    .xyz-paper.xyz-paper-footer-pinned > .xyz-content-area {
      flex: 0 1 auto !important;
      min-height: 0;
      overflow: visible;
    }
    .xyz-paper.xyz-paper-footer-pinned > .xyz-footer-spacer {
      flex: 1 1 auto;
      min-height: 0;
    }
    .xyz-paper.xyz-paper-footer-pinned > .xyz-footer {
      flex-shrink: 0;
      margin-top: 0;
      padding-top: 6px;
      page-break-inside: avoid;
      break-inside: avoid;
      position: static;
      width: 100%;
    }
    .xyz-footer {
      flex-shrink: 0;
      background:#ffffff;
      margin-top: auto;
      padding-top: 6px;
      page-break-inside: avoid;
      break-inside: avoid;
      position: static;
      width: 100%;
    }
    @media print {
      .xyz-footer {
        position: static !important;
      }
    }
    .xyz-signatures { width:100%; border-collapse:collapse; font-family: "Calibri", "Arial", sans-serif; font-size:12px; table-layout:fixed; }
    .xyz-signatures th, .xyz-signatures td { border:1px solid #000; padding:4px 6px; text-align:center !important; vertical-align:middle !important; word-wrap:break-word; overflow-wrap:break-word; max-width:0; }
    .xyz-signatures-blank td { height:52px; min-height:52px; padding:2px 4px 4px 4px; background:#fff; overflow:hidden; position:relative; box-sizing:border-box; vertical-align:top !important; text-align:center !important; border-bottom:1px solid #000; }
    .xyz-signatures tr:nth-child(2) th { padding-top:8px; padding-bottom:5px; }
    .xyz-signatures th { font-family: Calibri, "Calibri (Body)", Arial, sans-serif; font-size:11px; font-weight:700; background:#8f8f8f; text-align:center; text-transform:none; letter-spacing:0; }
    .xyz-signatures th[colspan="2"] { text-align:center; }
    .xyz-signatures td { font-family: Calibri, "Calibri (Body)", Arial, sans-serif; font-size:12px; text-align:center !important; vertical-align:middle !important; }
    .xyz-sig-img { max-height: 14px; max-width: 85%; width: auto; height: auto; object-fit: contain; display:block; margin:0 auto 1px auto; box-sizing:border-box; vertical-align:bottom; }
    .xyz-signatures-blank td .xyz-sig-img { max-height: 14px !important; max-width: calc(85% - 8px) !important; width: auto !important; height: auto !important; object-fit: contain !important; display: block !important; margin: 0 auto 1px auto !important; vertical-align: bottom !important; }
    .xyz-signatures-blank td > div { text-align:center; vertical-align:middle; display:flex; flex-direction:column; align-items:center; justify-content:center; width:100%; height:100%; padding-top:2px; margin-top:0; }
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures-blank td { padding-top:2px !important; padding-bottom:2px !important; vertical-align:top !important; }
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures-blank td > div { justify-content:flex-start !important; height:auto !important; min-height:0 !important; padding-top:0 !important; }
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures-blank .xyz-sig-img { margin:0 auto 2px auto !important; }
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures-blank .xyz-sig-time { display:block !important; margin:0 auto !important; line-height:1.1 !important; }
    .xyz-sig-time { font-size:7px; color:#6b7280; margin-top:0; line-height:1.1; }
    .xyz-signatures-blank td .xyz-sig-time { font-size: 7px !important; margin-top: 0 !important; }
    .xyz-generic-field { margin-bottom:10px; }
    .xyz-generic-label { font-size:12px; font-weight:700; margin-bottom:3px; text-transform:uppercase; letter-spacing:.2px; }
    .xyz-generic-value { font-size:13.5px; }
    .xyz-generic-word { margin:8px 0 12px 0; }
    .xyz-generic-word .ql-editor { padding:0; }
    .xyz-generic-word .ql-editor p { margin:0 0 6px 0; }
    .xyz-generic-word .ql-editor table { width:100%; border-collapse:collapse; border-spacing:0; border:none; table-layout:fixed; margin:4px 0; }
    .xyz-generic-word .ql-editor table tbody tr, .xyz-generic-word .ql-editor table thead tr { border:none; background:transparent; }
    .xyz-generic-word .ql-editor table tr { min-height:0; }
    .xyz-generic-word .ql-editor th, .xyz-generic-word .ql-editor td {
      border:none;
      border-right:1px solid #000;
      border-bottom:1px solid #000;
      padding:6px 6px !important;
      min-height:0 !important;
      line-height:1.35 !important;
      vertical-align:middle;
      box-sizing:border-box !important;
      word-wrap:break-word !important;
      overflow-wrap:break-word !important;
      word-break:break-word;
      max-width:0;
      white-space:normal !important;
      font-weight:normal;
    }
    .xyz-generic-word .ql-editor tr > :first-child { border-left:1px solid #000; }
    .xyz-generic-word .ql-editor table > :first-child > tr:first-child > * { border-top:1px solid #000; }
    .xyz-generic-word .ql-editor td > *, .xyz-generic-word .ql-editor th > * {
      margin:0 !important;
      padding:0 !important; 
      line-height:1 !important;
    }
    .xyz-generic-word .ql-editor p:last-child { margin-bottom:0; }
    .xyz-generic-word .ql-align-center { text-align:center; }
    .xyz-generic-word .ql-align-right { text-align:right; }
    .xyz-generic-word .ql-align-justify { text-align:justify; }
    .xyz-footer-user { text-align:center; }
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures tr:nth-child(2) th {
      padding-top: 10px !important;
      padding-bottom: 10px !important;
      line-height: 1.45 !important;
    }
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures tr:last-child td,
    .xyz-paper.xyz-individual-pipeline-email .xyz-signatures td.xyz-footer-user {
      vertical-align: top !important;
      padding-top: 8px !important;
      padding-bottom: 14px !important;
      line-height: 1.45 !important;
    }
    .xyz-paper.xyz-individual-pipeline-email .xyz-footer-user-text {
      display: block;
      padding-bottom: 6px;
      line-height: 1.45;
    }
    `;

    const headerHtml = `
      <div class="xyz-header-container">
        <div class="xyz-date-row"><div class="xyz-date">Date: ${dateStr}</div></div>
        <div class="xyz-header">
          <div class="xyz-logo"><img src="assets/images/qarshi-logo.png" alt="Qarshi" /></div>
          <div class="xyz-company">
            <div class="xyz-company-name">Qarshi Industries (Pvt) Ltd.</div>
            <div class="xyz-company-address">15-6, Jam-e-Shirin Boulevard, Gulberg-III, Lahore</div>
          </div>
          <div class="xyz-header-spacer"></div>
        </div>
        <div class="xyz-rule thick"></div>
        <div class="xyz-title">${headingText}</div>
      </div>`;

    const footerTableBody = hasDynamicFooter
      ? `
        <tr class="xyz-signatures-blank">
          ${(footerFields || []).map((f: any) =>
            getFooterSlots(f).map((slot: any) => `<td>${renderFooterSignatureSlot(slot, f?.label)}</td>`).join('')
          ).join('')}
        </tr>
        <tr>
          ${(footerFields || []).map((f: any) => `<th colspan="${getFooterSlots(f).length}">${f?.label || 'New Field'}:</th>`).join('')}
        </tr>
        <tr>
          ${(footerFields || []).map((f: any) =>
            getFooterSlots(f).map((slot: any) => `<td class="xyz-footer-user">${renderFooterUserSlot(slot)}</td>`).join('')
          ).join('')}
        </tr>`
      : `
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
        </tr>`;

    const footerHtml = `
      <div class="xyz-footer">
      <table class="xyz-signatures">
        ${footerTableBody}
      </table>
      </div>`;

    const papersHtml = contentPages
      .map((pageBlocks, pageIndex) => {
        const isFirst = pageIndex === 0;
        const isLast = pageIndex === contentPages.length - 1;
        const pageClassParts = [
          'xyz-paper',
          isLandscape ? 'xyz-paper--landscape' : 'xyz-paper--portrait',
          multiPage ? 'xyz-paper-page' : '',
          useFooterPinned ? 'xyz-paper-footer-pinned' : '',
          paperExtraClass.trim(),
        ].filter(Boolean);

        return `
      <div class="${pageClassParts.join(' ')}">
      ${isFirst ? headerHtml : ''}
      <div class="xyz-content-area">
        <div class="xyz-dynamic">${pageBlocks.join('')}</div>
      </div>
      ${useFooterPinned && isLast ? '<div class="xyz-footer-spacer" aria-hidden="true"></div>' : ''}
      ${isLast ? footerHtml : ''}
    </div>`;
      })
      .join('\n');

    const wrapperOrientationClass = isLandscape ? ' xyz-orientation-landscape' : '';

    return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
</head>
<body>
    <div class="abc-wrapper${wrapperOrientationClass}">
    <style>${css}</style>
    <div class="xyz-page">
      ${papersHtml}
  </div>
  </div>
</body>
</html>`;
  }

  
  private generateCapfAbcHtml(
    application: any,
    formFields: any[],
    applicationFormData: any,
    pipelines: any[] = [],
    applicationMeta?: { formName?: string; txtFormCode?: string; omitApprovalSignaturesInPdf?: boolean }
  ): string {
    const omitApprovalSignaturesInPdf = !!applicationMeta?.omitApprovalSignaturesInPdf;
    const capfFormName = resolveCapfFormNameFromSources(applicationMeta?.formName, application);
    const capfLogoPath = resolveCapfLogoPath(capfFormName);
    const capfBrandTitle = resolveCapfBrandTitle(capfFormName);
    const capfLogoClass = resolveCapfLogoCssClass();
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
      const raw = getFieldValue('CAPF #') || application.txtFormCode || '';
      return formatCapfFormNumberDisplay(raw);
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

    const getApprovalEntryForPipeline = (order: number, departmentId?: number, departmentName?: string): any | null => {
      if (!approvalHistory || approvalHistory.length === 0) return null;

      let entry = null;
      if (departmentId) {
        entry = approvalHistory.find((e: any) =>
          (e.level === order || e.intApprovalOrder === order) &&
          (Number(e.departmentId) === Number(departmentId) || Number(e.serDepartmentId) === Number(departmentId))
        );
      }
      if (!entry) {
        entry = approvalHistory.find((e: any) => e.level === order || e.intApprovalOrder === order);
      }
      if (!entry && departmentId) {
        entry = approvalHistory.find((e: any) =>
          Number(e.departmentId) === Number(departmentId) || Number(e.serDepartmentId) === Number(departmentId)
        );
      }
      if (!entry && departmentName) {
        const nameLower = departmentName.toLowerCase();
        entry = approvalHistory.find((e: any) =>
          (e.departmentName || '').toString().toLowerCase() === nameLower
        );
      }

      return entry || null;
    };

    const getNameText = (entry: any): string => {
      return (
        entry?.approverName ||
        entry?.approvedByName ||
        entry?.userName ||
        (entry?.approvedBy && isNaN(Number(entry.approvedBy)) ? String(entry.approvedBy) : '') ||
        ''
      );
    };

    const getDesignationText = (entry: any): string => {
      return (
        entry?.txtDesignation ||
        entry?.designation ||
        entry?.approverDesignation ||
        entry?.role ||
        ''
      );
    };

    const formatEntryDate = (entry: any): string => {
      if (!entry?.approvedDate) return '';
      try {
        const dt = new Date(entry.approvedDate);
        return isNaN(dt.getTime()) ? String(entry.approvedDate) : dt.toLocaleString();
      } catch {
        return String(entry.approvedDate);
      }
    };

    const isCeoEntry = (entry: any): boolean => {
      const level = Number(entry?.level ?? entry?.intApprovalOrder);
      const haystack = (
        `${entry?.role || ''} ${entry?.departmentName || ''} ${entry?.txtDepartmentName || ''} ${entry?.approverDesignation || ''}`
      ).toLowerCase();
      return level === -99 ||
        haystack.includes('chief executive') ||
        haystack.includes(' ceo') ||
        haystack.includes('ceo ') ||
        haystack.includes('md');
    };

    const getLatestCeoEntry = (): any | null => {
      if (!Array.isArray(approvalHistory) || approvalHistory.length === 0) return null;
      const ceoEntries = approvalHistory.filter((e: any) => isCeoEntry(e));
      if (ceoEntries.length === 0) return null;
      const toMs = (e: any): number => {
        if (!e?.approvedDate) return 0;
        try {
          const t = new Date(e.approvedDate).getTime();
          return isNaN(t) ? 0 : t;
        } catch {
          return 0;
        }
      };
      return [...ceoEntries].sort((a: any, b: any) => toMs(a) - toMs(b))[ceoEntries.length - 1] || null;
    };

    /** Invisible box matching real signature image footprint so server-side PDF overlay aligns after vendor edits. */
    const pipelineSigPlaceholderHtml =
      '<span class="sig-img sig-img-placeholder" aria-hidden="true"></span>';
    const ceoSigPlaceholderHtml =
      '<span class="sig-img sig-img-placeholder sig-img-placeholder--ceo" aria-hidden="true"></span>';

    const buildSignatureSlots = (): { nameText: string; designationText: string; departmentText: string; html: string; time: string }[] => {
      const staticLabels = [
        'User Deptt. (HoD)',
        'Technical Expert',
        'Procurement',
        'Finance',
        'Core Team HTR. / CCT HO'
      ];

      const sortedPipelines = Array.isArray(pipelines)
        ? [...pipelines].sort((a: any, b: any) => (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0))
        : [];

      if (sortedPipelines.length === 0) {
        const fallback = [
          { label: 'User Deptt. (HoD)', order: 1 },
          { label: 'Technical Expert', order: 2 },
          { label: 'Procurement', order: 3 },
          { label: 'Finance', order: 4 },
          { label: 'Core Team HTR. / CCT HO', order: 5 }
        ];
        return fallback.map((f) => {
          const entry = getApprovalEntryForPipeline(f.order);
          const nameText = omitApprovalSignaturesInPdf ? '' : getNameText(entry);
          const designationText = omitApprovalSignaturesInPdf ? '' : getDesignationText(entry);
          const departmentText = f.label;
          const userId = entry?.approvedBy || entry?.approverUserId || entry?.userId;
          const hasSignature = !!entry?.signaturePath;
          const signatureUrl = userId && hasSignature ? `${urls.API_URL}getSignature?userId=${userId}` : '';
          let html = '';
          if (omitApprovalSignaturesInPdf) {
            html = pipelineSigPlaceholderHtml;
          } else if (signatureUrl) {
            html = `<img class="sig-img" src="${signatureUrl}" alt="Signature" crossorigin="anonymous" />`;
          }
          const time = !omitApprovalSignaturesInPdf && hasSignature ? formatEntryDate(entry) : '';
          return { nameText, designationText, departmentText, html, time };
        });
      }

      return sortedPipelines.slice(0, staticLabels.length).map((pipeline: any, index: number) => {
        const order = pipeline.intApprovalOrder || (index + 1);
        const departmentId = pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId || pipeline.departmentId;
        const entry = getApprovalEntryForPipeline(order, departmentId);
        const nameText = omitApprovalSignaturesInPdf ? '' : getNameText(entry);
        const designationText = omitApprovalSignaturesInPdf ? '' : getDesignationText(entry);
        const departmentText = staticLabels[index] || `Department ${order}`;
        const userId = entry?.approvedBy || entry?.approverUserId || entry?.userId;
        const hasSignature = !!entry?.signaturePath;
        const signatureUrl = userId && hasSignature ? `${urls.API_URL}getSignature?userId=${userId}` : '';
        let html = '';
        if (omitApprovalSignaturesInPdf) {
          html = pipelineSigPlaceholderHtml;
        } else if (signatureUrl) {
          html = `<img class="sig-img" src="${signatureUrl}" alt="Signature" crossorigin="anonymous" />`;
        }
        const time = !omitApprovalSignaturesInPdf && hasSignature ? formatEntryDate(entry) : '';
        return { nameText, designationText, departmentText, html, time };
      });
    };

    const signatureSlots = buildSignatureSlots();
    const ceoEntry = getLatestCeoEntry();
    const ceoUserId = ceoEntry?.approvedBy || ceoEntry?.approverUserId || ceoEntry?.userId;
    const ceoHasSignature = !!ceoEntry?.signaturePath && !!ceoUserId;
    const ceoSignatureUrl = ceoHasSignature ? `${urls.API_URL}getSignature?userId=${ceoUserId}` : '';
    let ceoSignatureHtml = '';
    if (omitApprovalSignaturesInPdf) {
      ceoSignatureHtml = ceoSigPlaceholderHtml;
    } else if (ceoSignatureUrl) {
      ceoSignatureHtml = `<img class="sig-img" src="${ceoSignatureUrl}" alt="Signature" crossorigin="anonymous" />`;
    }
    const ceoTimeText = omitApprovalSignaturesInPdf ? '' : formatEntryDate(ceoEntry);
    const ceoNameText = omitApprovalSignaturesInPdf ? '' : getNameText(ceoEntry);
    const ceoDesignationText = omitApprovalSignaturesInPdf ? '' : getDesignationText(ceoEntry);

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

    table td,
    table th {
      word-wrap: break-word !important;
      overflow-wrap: break-word !important;
      word-break: break-word;
      max-width: 0;
      box-sizing: border-box;
      white-space: normal !important;
      min-height: 32px;
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
      padding: 5px 6px;
      vertical-align: middle;
      font-size: 12px;
      line-height: 1.2;
      box-sizing: border-box;
      word-wrap: break-word !important;
      overflow-wrap: break-word !important;
      min-height: 32px;
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
      height: 32px;
      width: auto;
      max-width: none;
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
      line-height: 1.2;
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
      margin: 0 0 8px 0;
      letter-spacing: .2px;
      font-size: 13px;
      line-height: 1.25;
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
      display: flex;
      flex-direction: column;
      align-items: stretch;
    }
    
    .sig-line {
      border-bottom: 1px solid var(--line);
      height: 32px;
      margin-bottom: 4px;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }

    .sig-img {
      max-height: 34px;
      max-width: 100%;
      object-fit: contain;
      display: block;
      margin: 0 auto;
      transform: translateY(-4px) !important;
    }

    /* Same box model as real signatures so rasterized base PDF matches submission layout for server overlay */
    .sig-img-placeholder {
      display: block;
      margin: 0 auto;
      width: 80px;
      height: 34px;
      max-height: 34px;
      max-width: 100%;
      flex-shrink: 0;
      visibility: hidden;
      pointer-events: none;
      transform: translateY(-4px) !important;
    }

    .sig-img-placeholder--ceo {
      width: 120px;
      height: 18px;
      max-height: 18px;
      transform: none !important;
    }

    .sig-time {
      font-size: 8px;
      width: 100%;
      display: block;
      text-align: center;
      margin-bottom: 2px;
      line-height: 1.1;
      white-space: nowrap;
    }

    .sig-meta {
      font-size: 8px;
      width: 100%;
      display: block;
      text-align: center;
      margin-bottom: 2px;
      line-height: 1.1;
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
    }

    .sig-line + .sig-time,
    .sig-line + .sig-meta {
      margin-top: -2px;
    }

    .sig-name {
      font-size: 10px;
      text-align: center;
      margin-bottom: 2px;
      line-height: 1.1;
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
    }

    .sig-label {
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
      line-height: 1.2;
      margin-top: 34px;
    }

    .approved {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      align-items: flex-start;
      margin-top: 12px;
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
      margin-bottom: 6px;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      overflow: visible;
    }

    .approved-sig .who {
      font-size: 12px;
      font-weight: 700;
      text-align: center;
      white-space: nowrap;
      width: 100%;
    }

    .approved-sig .approved-time,
    .approved-sig .approved-meta {
      width: 150px;
      display: block;
      text-align: center;
      font-size: 8px;
      line-height: 1.1;
      margin-bottom: 1px;
    }

    .approved-sig .approved-meta {
      white-space: normal;
      word-wrap: break-word;
      overflow-wrap: break-word;
    }

    .approved-sig .approved-meta-wrap {
      width: 150px;
      min-height: 30px;
    }

    .approved-sig .who.b {
      margin-top: 4px;
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

    .job-sign {
      margin-bottom: 8px;
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

    :host-context(.pdf-compact) .sig-img {
      transform: translateY(-4px) !important;
    }

    :host-context(.pdf-compact) .sig-time {
      font-size: 8.5px !important;
    }

    :host-context(.pdf-compact) .sig-name {
      font-size: 8.5px !important;
    }

    :host-context(.pdf-compact) .brand-row {
      margin-bottom: 2px !important;
    }

    :host-context(.pdf-compact) .brand-title {
      font-size: 16px !important;
    }

    :host-context(.pdf-compact) .logo img {
      height: 18px !important;
      width: auto !important;
      max-width: none !important;
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
          <img src="${capfLogoPath}" alt="" class="${capfLogoClass}">
        </div>
        <div class="brand-title">${capfBrandTitle}</div>
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
        <div class="box-title">PART-1 (TO BE FILLED BY CONCERNED DEPARTMENT)</div>

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
          ${signatureSlots.map((slot) => {
            const timeRow = omitApprovalSignaturesInPdf
              ? `<div class="sig-time">${slot.time ? escapeHtml(slot.time) : '&nbsp;'}</div>`
              : slot.html && slot.time
                ? `<div class="sig-time">${escapeHtml(slot.time)}</div>`
                : '';
            return `
            <div class="sig">
              <div class="sig-line">${slot.html}</div>
              ${timeRow}
              ${slot.nameText ? '<div class="sig-meta">' + escapeHtml(slot.nameText) + '</div>' : ''}
              ${slot.designationText ? '<div class="sig-meta">' + escapeHtml(slot.designationText) + '</div>' : ''}
              <div class="sig-label">${escapeHtml(slot.departmentText)}</div>
            </div>
          `;
          }).join('')}
        </div>

        <div class="xs mt6"><span class="b">Note:</span> Designation must be mentioned against each signature.</div>

        <div class="approved">
          <div class="who b">Approved By:</div>
          <div class="approved-sig">
            <div class="appline">${ceoSignatureHtml}</div>
            <div class="approved-meta-wrap">
              <div class="approved-time">${ceoTimeText ? escapeHtml(ceoTimeText) : '&nbsp;'}</div>
              <div class="approved-meta">${ceoNameText ? escapeHtml(ceoNameText) : '&nbsp;'}</div>
              <div class="approved-meta">${ceoDesignationText ? escapeHtml(ceoDesignationText) : '&nbsp;'}</div>
            </div>
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
    approver?: any,
    footerFields?: any[]
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
      approver: applicationFormData?.approver,
      footerFields: applicationFormData?.footerFields || []
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
