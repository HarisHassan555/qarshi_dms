import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { TemplateWorkflowService } from 'src/app/services/template-workflow/template-workflow.service';
import {
    buildApiSearchQuery,
    createEmptyFacets,
    mapApiReportItem,
    ReportSearchFacets,
    ReportSearchResult,
} from './reports-search.engine';

interface FacetGroup {
    label: string;
    facetKey: 'type' | 'department' | 'uploadedBy' | 'status';
    entries: { value: string; count: number }[];
}

@Component({
    moduleId: module.id,
    selector: 'app-reports',
    templateUrl: './reports.component.html',
    styleUrls: ['./reports.component.css'],
})
export class ReportsComponent implements OnInit, OnDestroy {
    query = '';
    semantic = false;
    sort = 'relevance';
    facets: ReportSearchFacets = createEmptyFacets();

    results: ReportSearchResult[] = [];
    elapsedMs = 0;
    totalResults = 0;
    page = 0;
    pageSize = 25;
    isLoading = false;
    loadError = '';
    showHelpModal = false;
    toastMessage = '';
    downloadingApplicationId = 0;

    facetGroups: FacetGroup[] = [];

    private searchTimer: ReturnType<typeof setTimeout> | null = null;
    private toastTimer: ReturnType<typeof setTimeout> | null = null;
    private searchRequestId = 0;

    constructor(
        private templateWorkflowService: TemplateWorkflowService,
        private notificationService: NotificationService,
        private router: Router
    ) {}

    ngOnInit(): void {
        this.runSearch();
    }

    ngOnDestroy(): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        if (this.toastTimer) {
            clearTimeout(this.toastTimer);
        }
    }

    @HostListener('document:keydown', ['$event'])
    onGlobalKeydown(event: KeyboardEvent): void {
        if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
            event.preventDefault();
            const input = document.getElementById('reportsSearchInput') as HTMLInputElement | null;
            input?.focus();
        }
    }

    onSearchInput(value: string): void {
        this.query = value;
        this.scheduleSearch(500);
    }

    setQuery(value: string): void {
        this.query = value;
        this.runSearch();
    }

    setQueryCapitalAssetPhrase(): void {
        this.setQuery('"capital asset"');
    }

    clearSearch(): void {
        this.query = '';
        this.runSearch();
    }

    toggleSemantic(): void {
        this.semantic = !this.semantic;
        this.runSearch();
    }

    onSortChange(value: string): void {
        this.sort = value;
        this.runSearch();
    }

    clearFacets(): void {
        this.facets = createEmptyFacets();
        this.runSearch();
    }

    toggleFacet(facetKey: FacetGroup['facetKey'], value: string, checked: boolean): void {
        const set = this.facets[facetKey];
        if (checked) {
            set.add(value);
        } else {
            set.delete(value);
        }
        this.scheduleSearch(350);
    }

    isFacetChecked(facetKey: FacetGroup['facetKey'], value: string): boolean {
        return this.facets[facetKey].has(value);
    }

    onFacetDateChange(field: 'dateFrom' | 'dateTo', value: string): void {
        this.facets[field] = value;
        this.scheduleSearch(400);
    }

    onFacetAmountChange(field: 'amountMin' | 'amountMax', value: string): void {
        this.facets[field] = value;
        this.scheduleSearch(400);
    }

    private scheduleSearch(delayMs: number): void {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        this.searchTimer = setTimeout(() => this.runSearch(false), delayMs);
    }

    changePage(nextPage: number): void {
        const maxPage = this.totalPages - 1;
        const target = Math.max(0, Math.min(nextPage, maxPage));
        if (target === this.page) {
            return;
        }
        this.page = target;
        this.runSearch(true);
        this.scrollResultsIntoView();
    }

    changePageSize(size: string | number): void {
        const nextSize = Number(size);
        this.pageSize = Number.isFinite(nextSize) && nextSize > 0 ? nextSize : 25;
        this.page = 0;
        this.runSearch(true);
    }

    get totalPages(): number {
        return Math.max(1, Math.ceil(this.totalResults / this.pageSize));
    }

    get pageStart(): number {
        return this.totalResults === 0 ? 0 : this.page * this.pageSize + 1;
    }

    get pageEnd(): number {
        return Math.min(this.totalResults, (this.page + 1) * this.pageSize);
    }

    isDownloading(doc: ReportSearchResult): boolean {
        return this.downloadingApplicationId === Number(doc.serApplicationId || 0);
    }

    async runSearch(keepCurrentPage = false): Promise<void> {
        if (!keepCurrentPage) {
            this.page = 0;
        }
        const requestId = ++this.searchRequestId;
        this.isLoading = true;
        this.loadError = '';

        try {
            let response = await firstValueFrom(
                this.templateWorkflowService.searchVelocityReports(this.buildSearchRequestParams())
            );

            if (requestId !== this.searchRequestId) {
                return;
            }

            let total = Number(response?.total ?? 0);
            const maxPage = Math.max(0, Math.ceil(total / this.pageSize) - 1);
            if (this.page > maxPage && total > 0) {
                this.page = maxPage;
                response = await firstValueFrom(
                    this.templateWorkflowService.searchVelocityReports(this.buildSearchRequestParams())
                );
                if (requestId !== this.searchRequestId) {
                    return;
                }
                total = Number(response?.total ?? 0);
            }

            const items = Array.isArray(response?.items) ? response.items : [];
            this.results = items.map((item: any) => mapApiReportItem(item));
            this.totalResults = total || this.results.length;
            this.elapsedMs = Number(response?.elapsedMs ?? 0);
            this.buildFacetGroups(response?.facetCounts);
        } catch (error) {
            if (requestId !== this.searchRequestId) {
                return;
            }
            console.error('Reports search failed', error);
            this.loadError = 'Could not load report results. Please try again.';
            this.results = [];
            this.totalResults = 0;
            this.facetGroups = [];
        } finally {
            if (requestId === this.searchRequestId) {
                this.isLoading = false;
            }
        }
    }

    openHelp(): void {
        this.showHelpModal = true;
    }

    closeHelp(): void {
        this.showHelpModal = false;
    }

    onHelpBackdropClick(event: MouseEvent): void {
        if ((event.target as HTMLElement)?.id === 'reportsHelpModal') {
            this.closeHelp();
        }
    }

    showToast(message: string): void {
        this.toastMessage = message;
        if (this.toastTimer) {
            clearTimeout(this.toastTimer);
        }
        this.toastTimer = setTimeout(() => {
            this.toastMessage = '';
        }, 2600);
    }

    openDocument(doc: ReportSearchResult): void {
        const applicationId = Number(doc.serApplicationId || 0);
        if (!applicationId) {
            this.showToast('Application not found.');
            return;
        }
        const status = String(doc.txtStatus || doc.status || '').trim().toUpperCase();
        if (status === 'DRAFT') {
            this.router.navigate(['/my-application', applicationId]);
            return;
        }
        if (this.isOpenWorkflowStatus(status)) {
            this.router.navigate(['/template-approval', applicationId]);
            return;
        }
        this.router.navigate(['/my-application', applicationId]);
    }

    downloadDocument(doc: ReportSearchResult): void {
        const applicationId = Number(doc.serApplicationId || 0);
        if (!applicationId) {
            this.showToast('Application not found.');
            return;
        }
        if (!doc.hasPdf) {
            this.showToast('No PDF is available for this application yet.');
            return;
        }
        if (this.downloadingApplicationId === applicationId) {
            return;
        }

        this.downloadingApplicationId = applicationId;
        this.templateWorkflowService.getTemplateApplicationPdfBase64(applicationId).subscribe({
            next: (response) => {
                const base64 = String(response?.base64 || '').trim();
                if (response?.status !== 'Success' || !base64) {
                    this.notificationService.showMessage(response?.message || 'PDF could not be loaded.', 'error');
                    return;
                }
                const mimeType = String(response?.mimeType || 'application/pdf');
                const fileName = String(response?.fileName || doc.filename || `application-${applicationId}.pdf`);
                this.saveBase64File(base64, mimeType, fileName);
            },
            error: () => {
                this.notificationService.showMessage('Could not download the application PDF.', 'error');
            },
            complete: () => {
                if (this.downloadingApplicationId === applicationId) {
                    this.downloadingApplicationId = 0;
                }
            },
        });
    }

    statusBadgeClass(status: string): string {
        const normalized = String(status || '').trim().toLowerCase();
        if (normalized === 'approved' || normalized === 'completed' || normalized === 'signed') {
            return 'bg-emerald-100 text-emerald-700';
        }
        if (normalized === 'rejected') {
            return 'bg-rose-100 text-rose-700';
        }
        if (normalized === 'draft') {
            return 'bg-slate-200 text-slate-700';
        }
        if (normalized.includes('pending') || normalized === 'in progress') {
            return 'bg-amber-100 text-amber-700';
        }
        return 'bg-slate-100 text-slate-600';
    }

    private saveBase64File(base64: string, mimeType: string, fileName: string): void {
        try {
            const binary = atob(base64);
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) {
                bytes[i] = binary.charCodeAt(i);
            }
            const blob = new Blob([bytes], { type: mimeType });
            const url = window.URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = fileName;
            anchor.click();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Failed to decode PDF base64', error);
            this.notificationService.showMessage('Download failed while decoding the PDF.', 'error');
        }
    }

    private buildFacetGroups(facetCounts: any): void {
        const toEntries = (map: Record<string, number> | undefined) =>
            Object.entries(map || {})
                .sort((a, b) => b[1] - a[1])
                .map(([value, count]) => ({ value, count }));

        this.facetGroups = [
            { label: 'Document Type', facetKey: 'type', entries: toEntries(facetCounts?.type) },
            { label: 'Department', facetKey: 'department', entries: toEntries(facetCounts?.department) },
            { label: 'Uploaded By', facetKey: 'uploadedBy', entries: toEntries(facetCounts?.uploadedBy) },
            { label: 'Status', facetKey: 'status', entries: toEntries(facetCounts?.status) },
        ];
    }

    private facetSetToCsv(set: Set<string>): string {
        return Array.from(set).join(',');
    }

    private isOpenWorkflowStatus(status: string): boolean {
        return status === 'PENDING'
            || status === 'IN_PROGRESS'
            || status === 'OPINION_PENDING'
            || status === 'CEO_PENDING'
            || status === 'ASSET_PENDING'
            || status === 'PR_PENDING'
            || status === 'PO_PENDING'
            || status === 'PO_VENDOR_TE_PENDING'
            || status === 'HOLD';
    }

    private buildSearchRequestParams() {
        return {
            page: this.page,
            pageSize: this.pageSize,
            search: buildApiSearchQuery(this.query, this.semantic),
            sort: this.sort,
            all: this.isAdminUser(),
            statuses: this.facetSetToCsv(this.facets.status),
            types: this.facetSetToCsv(this.facets.type),
            departments: this.facetSetToCsv(this.facets.department),
            uploadedBy: this.facetSetToCsv(this.facets.uploadedBy),
            dateFrom: this.facets.dateFrom,
            dateTo: this.facets.dateTo,
            amountMin: this.facets.amountMin,
            amountMax: this.facets.amountMax,
        };
    }

    private scrollResultsIntoView(): void {
        const el = document.getElementById('reportsResultsTop');
        el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    private isAdminUser(): boolean {
        const userJson = localStorage.getItem('user');
        if (!userJson) {
            return false;
        }
        try {
            const user = JSON.parse(userJson);
            const roleName = (
                user?.cfgTblRole?.txtRoleName ||
                user?.txtrole ||
                user?.roleName ||
                ''
            ).toString().trim().toUpperCase();
            return roleName.includes('ADMIN');
        } catch {
            return false;
        }
    }
}
