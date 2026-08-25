import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { ActivityLogService } from './services/activity-log/activity-log.service';
import { SharedDataService } from './services/shared-data/shared-data.service';
import { TemplateWorkflowService } from './services/template-workflow/template-workflow.service';
import { UserService } from './services/user/user.service';

interface DashboardApplicationItem {
    serApplicationId?: number;
    serFormId?: number;
    txtFormCode?: string;
    txtStatus?: string;
    intCurrentApprovalLevel?: number;
    dteCreatedDate?: string;
    serSubmittedBy?: number;
    templateName?: string;
    source: 'submitted' | 'pending';
    requiresAction: boolean;
    submittedByName: string;
    departmentName: string;
    stageKey: string;
    stageLabel: string;
    stageOrder: number;
}

interface DashboardStageColumn {
    key: string;
    title: string;
    count: number;
    order: number;
    items: DashboardApplicationItem[];
}

interface DashboardMetric {
    label: string;
    value: number;
    tone: 'slate' | 'amber' | 'emerald' | 'rose';
    hint: string;
}

interface DashboardActivityEntry {
    id: number;
    actionType: string;
    username: string;
    message: string;
    status: string;
    createdAt: string;
}

interface DashboardPagedResponse {
    items?: any[];
    total?: number;
}

type DashboardCsvColumnKey =
    'txtFormCode'
    | 'templateName'
    | 'txtStatus'
    | 'stageLabel'
    | 'departmentName'
    | 'submittedByName'
    | 'dteCreatedDate';

interface DashboardCsvColumn {
    key: DashboardCsvColumnKey;
    label: string;
}

@Component({
    moduleId: module.id,
    templateUrl: './finance.html',
    styleUrls: ['./finance.css'],
})
export class FinanceComponent implements OnInit {
    user: any = null;
    username = '';
    isAdmin = false;
    isLoadingDashboard = false;
    dashboardMetrics: DashboardMetric[] = [];
    boardColumns: DashboardStageColumn[] = [];
    recentApplications: DashboardApplicationItem[] = [];
    recentActivities: DashboardActivityEntry[] = [];
    scopeLabel = 'Your workflow';
    totalApplications = 0;
    pendingApplications = 0;
    completedApplications = 0;
    rejectedApplications = 0;
    actionableApplications = 0;
    filterStartDate = '';
    filterEndDate = '';
    filterDepartment = '';
    filterStatus = '';
    filterFormType = '';
    availableDepartments: string[] = [];
    availableStatuses: string[] = [];
    availableFormTypes: string[] = [];
    showDashboardCsvModal = false;
    availableDashboardCsvColumns: DashboardCsvColumn[] = [];
    selectedDashboardCsvColumns: DashboardCsvColumn[] = [];
    readonly dashboardCsvColumns: DashboardCsvColumn[] = [
        { key: 'txtFormCode', label: 'Application Code' },
        { key: 'templateName', label: 'Form Type' },
        { key: 'txtStatus', label: 'Status' },
        { key: 'stageLabel', label: 'Stage' },
        { key: 'departmentName', label: 'Department' },
        { key: 'submittedByName', label: 'Submitted By' },
        { key: 'dteCreatedDate', label: 'Submitted Date' }
    ];

    private currentUserId = 0;
    private readonly userNameById = new Map<number, string>();
    private readonly userDepartmentById = new Map<number, string>();
    private allDashboardItems: DashboardApplicationItem[] = [];

    constructor(
        private activityLogService: ActivityLogService,
        private sharedDataService: SharedDataService,
        private templateWorkflowService: TemplateWorkflowService,
        private userService: UserService,
        private router: Router
    ) {
        this.hydrateUserFromStorage();
    }

    async ngOnInit(): Promise<void> {
        await this.loadDashboard();
        this.getUser();
    }

    get hasBoardData(): boolean {
        return this.boardColumns.some((column) => column.items.length > 0);
    }

    async refreshDashboard(): Promise<void> {
        await this.loadDashboard();
    }

    applyDashboardFilters(): void {
        this.refreshDashboardView();
    }

    clearDashboardFilters(): void {
        this.filterStartDate = '';
        this.filterEndDate = '';
        this.filterDepartment = '';
        this.filterStatus = '';
        this.filterFormType = '';
        this.refreshDashboardView();
    }

    openDashboardCsvModal(): void {
        const filteredItems = this.getFilteredDashboardItems();
        if (filteredItems.length === 0) {
            return;
        }
        this.availableDashboardCsvColumns = [...this.dashboardCsvColumns];
        this.selectedDashboardCsvColumns = [];
        this.showDashboardCsvModal = true;
    }

    closeDashboardCsvModal(): void {
        this.showDashboardCsvModal = false;
    }

    selectAllDashboardCsvColumns(): void {
        this.availableDashboardCsvColumns = [];
        this.selectedDashboardCsvColumns = [...this.dashboardCsvColumns];
    }

    unselectAllDashboardCsvColumns(): void {
        this.availableDashboardCsvColumns = [...this.dashboardCsvColumns];
        this.selectedDashboardCsvColumns = [];
    }

    addDashboardCsvColumn(column: DashboardCsvColumn): void {
        if (!this.selectedDashboardCsvColumns.some((item) => item.key === column.key)) {
            this.availableDashboardCsvColumns = this.availableDashboardCsvColumns.filter((item) => item.key !== column.key);
            this.selectedDashboardCsvColumns = [...this.selectedDashboardCsvColumns, column];
        }
    }

    removeDashboardCsvColumn(column: DashboardCsvColumn): void {
        this.selectedDashboardCsvColumns = this.selectedDashboardCsvColumns.filter((item) => item.key !== column.key);
        const restoredColumns = [...this.availableDashboardCsvColumns, column];
        this.availableDashboardCsvColumns = this.dashboardCsvColumns
            .filter((candidate) => restoredColumns.some((item) => item.key === candidate.key));
    }

    downloadDashboardCsv(): void {
        const filteredItems = this.getFilteredDashboardItems();
        if (!filteredItems.length || this.selectedDashboardCsvColumns.length === 0) {
            return;
        }

        const csvRows = [
            this.selectedDashboardCsvColumns.map((column) => this.escapeCsvValue(column.label)).join(','),
            ...filteredItems.map((item) => this.selectedDashboardCsvColumns
                .map((column) => this.escapeCsvValue(this.getDashboardCsvCellValue(item, column.key)))
                .join(','))
        ];

        this.downloadBlob(
            new Blob([csvRows.join('\n')], { type: 'text/csv;charset=utf-8;' }),
            `dashboard_${new Date().toISOString().split('T')[0]}.csv`
        );
        this.showDashboardCsvModal = false;
    }

    trackByDashboardCsvColumn(index: number, column: DashboardCsvColumn): DashboardCsvColumnKey {
        return column.key;
    }

    openApplication(item: DashboardApplicationItem): void {
        const applicationId = Number(item?.serApplicationId || 0);
        if (!applicationId) {
            return;
        }
        if (item.requiresAction || this.isAdmin) {
            this.router.navigate(['/template-approval', applicationId]);
            return;
        }
        this.router.navigate(['/my-application', applicationId]);
    }

    openActivityLogs(): void {
        this.router.navigate(['/activitylogs']);
    }

    openTemplateList(): void {
        this.router.navigate(['/template-list']);
    }

    getMetricClasses(tone: DashboardMetric['tone']): string {
        switch (tone) {
            case 'amber':
                return 'border-amber-200 bg-amber-50 text-amber-900';
            case 'emerald':
                return 'border-emerald-200 bg-emerald-50 text-emerald-900';
            case 'rose':
                return 'border-rose-200 bg-rose-50 text-rose-900';
            default:
                return 'border-slate-200 bg-white text-slate-900';
        }
    }

    getStatusBadgeClasses(status: string | undefined): string {
        const normalizedStatus = String(status || '').trim().toUpperCase();
        if (normalizedStatus === 'REJECTED') {
            return 'badge-outline-danger';
        }
        if (normalizedStatus === 'APPROVED' || normalizedStatus === 'COMPLETED') {
            return 'badge-outline-success';
        }
        if (normalizedStatus === 'OPINION_PENDING') {
            return 'badge-outline-info';
        }
        return 'badge-outline-warning';
    }

    formatDate(value: string | undefined): string {
        if (!value) {
            return 'Unknown time';
        }
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return value;
        }
        return parsed.toLocaleString();
    }

    formatActivityTitle(entry: DashboardActivityEntry): string {
        const action = this.humanizeStatus(entry.actionType || 'UPDATE');
        return `${action}${entry.username ? ' by ' + entry.username : ''}`;
    }

    getApplicantLabel(item: DashboardApplicationItem): string {
        if (!this.isAdmin) {
            return item.requiresAction ? 'Needs your action' : '';
        }
        if (item.requiresAction) {
            return 'Needs your action';
        }
        return item.submittedByName ? `Submitted by ${item.submittedByName}` : 'Submitted application';
    }

    private getUser(): void {
        this.sharedDataService.getUser().subscribe((data) => {
            if (!data) {
                return;
            }
            const previousUserId = this.currentUserId;
            const previousAdminState = this.isAdmin;
            this.user = data;
            this.username = data.txtUserName || data.userName || '';
            this.currentUserId = this.resolveUserId(data);
            this.isAdmin = this.isAdminUser(data);
            this.scopeLabel = this.isAdmin ? 'Qarshi Workflow' : 'Your workflow';
            if (previousUserId !== this.currentUserId || previousAdminState !== this.isAdmin) {
                void this.loadDashboard();
            }
        });
    }

    private hydrateUserFromStorage(): void {
        const userJson = localStorage.getItem('user');
        if (!userJson) {
            return;
        }
        try {
            const parsedUser = JSON.parse(userJson);
            this.user = parsedUser;
            this.username = parsedUser?.txtUserName || parsedUser?.userName || '';
            this.currentUserId = this.resolveUserId(parsedUser);
            this.isAdmin = this.isAdminUser(parsedUser);
            this.scopeLabel = this.isAdmin ? 'All users workflow' : 'Your workflow';
        } catch (error) {
            console.error('Error parsing user data:', error);
        }
    }

    private async loadDashboard(): Promise<void> {
        if (!this.currentUserId) {
            return;
        }

        this.isLoadingDashboard = true;
        try {
            const activityFilters = this.isAdmin ? {} : { userId: this.currentUserId };
            const [submittedItemsRaw, pendingItemsRaw, usersResponse, activityLogs] = await Promise.all([
                this.fetchAllSubmittedApplications(),
                this.fetchAllPendingApprovals(),
                firstValueFrom(this.userService.getUsers()),
                this.isAdmin ? firstValueFrom(this.activityLogService.getAll(activityFilters)) : Promise.resolve([])
            ]);

            this.userNameById.clear();
            this.userDepartmentById.clear();
            (Array.isArray(usersResponse) ? usersResponse : []).forEach((user: any) => {
                const userId = this.resolveUserId(user);
                if (userId > 0) {
                    this.userNameById.set(userId, user?.txtUserName || user?.userName || user?.name || `User ${userId}`);
                    this.userDepartmentById.set(
                        userId,
                        user?.hrTblDepartment?.txtDepartmentName ||
                        user?.departmentName ||
                        user?.txtDepartmentName ||
                        ''
                    );
                }
            });

            const submittedItems = submittedItemsRaw
                .map((application: any) => this.toDashboardItem(application, 'submitted', false));
            const pendingItems = pendingItemsRaw
                .map((application: any) => this.toDashboardItem(application, 'pending', true));

            const mergedItems = this.mergeDashboardItems(submittedItems, pendingItems);

            mergedItems.forEach((item) => {
                const stageInfo = this.resolveStageInfo(item);
                item.stageKey = stageInfo.key;
                item.stageLabel = stageInfo.label;
                item.stageOrder = stageInfo.order;
            });

            this.allDashboardItems = mergedItems;
            this.availableDepartments = this.buildDistinctOptions(mergedItems.map((item) => item.departmentName));
            this.availableStatuses = this.buildDistinctOptions(mergedItems.map((item) => item.txtStatus || ''));
            this.availableFormTypes = this.buildDistinctOptions(mergedItems.map((item) => item.templateName || ''));
            this.refreshDashboardView();
            this.recentActivities = (Array.isArray(activityLogs) ? activityLogs : [])
                .map((entry: any) => this.toActivityEntry(entry))
                .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
                .slice(0, 6);
        } catch (error) {
            console.error('Dashboard load failed', error);
            this.dashboardMetrics = [];
            this.boardColumns = [];
            this.recentApplications = [];
            this.recentActivities = [];
        } finally {
            this.isLoadingDashboard = false;
        }
    }

    private async fetchAllSubmittedApplications(): Promise<any[]> {
        return this.fetchAllPages((page, pageSize) =>
            this.templateWorkflowService.getMyTemplateApplications(
                this.currentUserId,
                page,
                pageSize,
                '',
                this.isAdmin
            )
        );
    }

    private async fetchAllPendingApprovals(): Promise<any[]> {
        return this.fetchAllPages((page, pageSize) =>
            this.templateWorkflowService.getTemplatePendingApprovals(
                this.currentUserId,
                this.isAdmin,
                page,
                pageSize,
                ''
            )
        );
    }

    private async fetchAllPages(
        request: (page: number, pageSize: number) => Observable<DashboardPagedResponse>,
        pageSize = 100
    ): Promise<any[]> {
        const firstResponse = await firstValueFrom(request(0, pageSize)) as DashboardPagedResponse;
        const firstItems = Array.isArray(firstResponse?.items) ? firstResponse.items : [];
        const total = Number(firstResponse?.total || firstItems.length || 0);

        if (firstItems.length >= total) {
            return firstItems;
        }

        const totalPages = Math.ceil(total / pageSize);
        const remainingResponses = await Promise.all(
            Array.from({ length: Math.max(0, totalPages - 1) }, (_, index) =>
                firstValueFrom(request(index + 1, pageSize)) as Promise<DashboardPagedResponse>
            )
        );

        return [
            ...firstItems,
            ...remainingResponses.flatMap((response: any) => Array.isArray(response?.items) ? response.items : [])
        ];
    }

    private toDashboardItem(application: any, source: 'submitted' | 'pending', requiresAction: boolean): DashboardApplicationItem {
        const submittedBy = Number(application?.serSubmittedBy || 0);
        return {
            serApplicationId: Number(application?.serApplicationId || 0),
            serFormId: Number(application?.serFormId || 0),
            txtFormCode: application?.txtFormCode || '',
            txtStatus: application?.txtStatus || '',
            intCurrentApprovalLevel: Number(application?.intCurrentApprovalLevel || 0),
            dteCreatedDate: application?.dteCreatedDate || '',
            serSubmittedBy: submittedBy || undefined,
            templateName: application?.templateName || 'Template',
            source,
            requiresAction,
            submittedByName: this.userNameById.get(submittedBy) || '',
            departmentName: this.userDepartmentById.get(submittedBy)
                || application?.departmentName
                || application?.txtDepartmentName
                || application?.hrTblDepartment?.txtDepartmentName
                || '',
            stageKey: '',
            stageLabel: '',
            stageOrder: 999
        };
    }

    private mergeDashboardItems(
        submittedItems: DashboardApplicationItem[],
        pendingItems: DashboardApplicationItem[]
    ): DashboardApplicationItem[] {
        const mergedById = new Map<number, DashboardApplicationItem>();

        [...submittedItems, ...pendingItems].forEach((item) => {
            const applicationId = Number(item.serApplicationId || 0);
            if (!applicationId) {
                return;
            }

            const existing = mergedById.get(applicationId);
            if (!existing) {
                mergedById.set(applicationId, { ...item });
                return;
            }

            mergedById.set(applicationId, {
                ...existing,
                ...item,
                source: existing.source === 'submitted' ? existing.source : item.source,
                requiresAction: existing.requiresAction || item.requiresAction,
                submittedByName: existing.submittedByName || item.submittedByName
            });
        });

        return Array.from(mergedById.values())
            .sort((left, right) => this.getCreatedAtMillis(right) - this.getCreatedAtMillis(left));
    }

    private resolveStageInfo(item: DashboardApplicationItem): { key: string; label: string; order: number } {
        const normalizedStatus = String(item.txtStatus || '').trim().toUpperCase();
        if (normalizedStatus === 'OPINION_PENDING') {
            return { key: 'opinion-pending', label: 'Opinion Pending', order: 3 };
        }
        if (this.isCompletedStatus(normalizedStatus)) {
            return { key: 'completed', label: 'Completed', order: 4 };
        }
        if (this.isRejectedStatus(normalizedStatus)) {
            return { key: 'rejected', label: 'Rejected', order: 5 };
        }
        if (normalizedStatus === 'IN_PROGRESS') {
            return { key: 'in-progress', label: 'In Progress', order: 2 };
        }
        if (this.isOpenStatus(normalizedStatus)) {
            return { key: 'pending', label: 'Pending', order: 1 };
        }

        const fallbackLabel = this.humanizeStatus(normalizedStatus || 'PENDING');
        return { key: this.slugify(fallbackLabel), label: fallbackLabel, order: 999 };
    }

    private buildBoardColumns(items: DashboardApplicationItem[]): DashboardStageColumn[] {
        const orderedColumns: DashboardStageColumn[] = [
            { key: 'pending', title: 'Pending', count: 0, order: 1, items: [] },
            { key: 'in-progress', title: 'In Progress', count: 0, order: 2, items: [] },
            { key: 'opinion-pending', title: 'Opinion Pending', count: 0, order: 3, items: [] },
            { key: 'completed', title: 'Completed', count: 0, order: 4, items: [] },
            { key: 'rejected', title: 'Rejected', count: 0, order: 5, items: [] },
        ];
        const stageMap = new Map<string, DashboardStageColumn>(
            orderedColumns.map((column) => [column.key, column])
        );

        items.forEach((item) => {
            const key = item.stageKey || 'pending';
            const existingColumn = stageMap.get(key);
            if (!existingColumn) {
                return;
            }
            existingColumn.items.push(item);
            existingColumn.count += 1;
        });

        return orderedColumns.map((column) => ({
            ...column,
            items: [...column.items].sort((left, right) => this.getCreatedAtMillis(right) - this.getCreatedAtMillis(left))
        }));
    }

    private toActivityEntry(entry: any): DashboardActivityEntry {
        return {
            id: Number(entry?.serActivityLogId || 0),
            actionType: String(entry?.txtActionType || '').trim(),
            username: String(entry?.txtUsername || '').trim(),
            message: String(entry?.txtMessage || entry?.txtErrorMessage || '').trim(),
            status: String(entry?.txtStatus || '').trim(),
            createdAt: String(entry?.dteCreatedDate || '')
        };
    }

    private getCreatedAtMillis(item: DashboardApplicationItem): number {
        const parsed = new Date(item?.dteCreatedDate || '').getTime();
        return Number.isFinite(parsed) ? parsed : 0;
    }

    private isOpenStatus(status: string | undefined): boolean {
        const normalizedStatus = String(status || '').trim().toUpperCase();
        return normalizedStatus === 'PENDING'
            || normalizedStatus === 'IN_PROGRESS'
            || normalizedStatus === 'OPINION_PENDING'
            || normalizedStatus === 'CEO_PENDING'
            || normalizedStatus === 'ASSET_PENDING'
            || normalizedStatus === 'PR_PENDING'
            || normalizedStatus === 'PO_PENDING'
            || normalizedStatus === 'PO_VENDOR_TE_PENDING';
    }

    private isCompletedStatus(status: string | undefined): boolean {
        const normalizedStatus = String(status || '').trim().toUpperCase();
        return normalizedStatus === 'APPROVED' || normalizedStatus === 'COMPLETED';
    }

    private isRejectedStatus(status: string | undefined): boolean {
        return String(status || '').trim().toUpperCase() === 'REJECTED';
    }

    private humanizeStatus(status: string): string {
        return String(status || '')
            .trim()
            .toLowerCase()
            .split('_')
            .filter((part) => !!part)
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
            .join(' ') || 'Pending Review';
    }

    private slugify(value: string): string {
        return String(value || '')
            .trim()
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '') || 'stage';
    }

    private resolveUserId(user: any): number {
        return Number(user?.serUserId || user?.userId || user?.id || 0);
    }

    private isAdminUser(user: any): boolean {
        const roleName = (
            user?.cfgTblRole?.txtRoleName ||
            user?.txtrole ||
            user?.roleName ||
            ''
        ).toString().trim().toUpperCase();
        return roleName === 'ADMIN'
            || roleName === 'ROLE_ADMIN'
            || roleName === 'SUPER ADMIN'
            || roleName === 'ROLE_SUPER ADMIN'
            || roleName.includes('ADMIN');
    }

    private refreshDashboardView(): void {
        const filteredItems = this.getFilteredDashboardItems();
        this.totalApplications = filteredItems.length;
        this.pendingApplications = filteredItems
            .filter((item: DashboardApplicationItem) => this.isOpenStatus(item.txtStatus))
            .length;
        this.completedApplications = filteredItems
            .filter((item: DashboardApplicationItem) => this.isCompletedStatus(item.txtStatus))
            .length;
        this.rejectedApplications = filteredItems
            .filter((item: DashboardApplicationItem) => this.isRejectedStatus(item.txtStatus))
            .length;
        this.actionableApplications = filteredItems.filter((item) => item.requiresAction).length;

        this.dashboardMetrics = [
            {
                label: 'Total Applications',
                value: this.totalApplications,
                tone: 'slate',
                hint: this.isAdmin ? 'All submitted workflow applications' : 'Applications you have submitted'
            },
            {
                label: 'Pending In Workflow',
                value: this.pendingApplications,
                tone: 'amber',
                hint: 'Applications still moving through approval'
            },
            {
                label: 'Completed',
                value: this.completedApplications,
                tone: 'emerald',
                hint: 'Applications that reached final approval'
            },
            {
                label: 'Rejected',
                value: this.rejectedApplications,
                tone: 'rose',
                hint: 'Applications stopped with rejection'
            }
        ];

        this.boardColumns = this.buildBoardColumns(filteredItems);
        this.recentApplications = [...filteredItems]
            .sort((left, right) => this.getCreatedAtMillis(right) - this.getCreatedAtMillis(left))
            .slice(0, 6);
    }

    private getFilteredDashboardItems(): DashboardApplicationItem[] {
        return this.allDashboardItems.filter((item) => {
            if (this.filterDepartment && (item.departmentName || '') !== this.filterDepartment) {
                return false;
            }
            if (this.filterStatus && (item.txtStatus || '') !== this.filterStatus) {
                return false;
            }
            if (this.filterFormType && (item.templateName || '') !== this.filterFormType) {
                return false;
            }

            const createdAtMs = this.getCreatedAtMillis(item);
            if (this.filterStartDate) {
                const startMs = new Date(`${this.filterStartDate}T00:00:00`).getTime();
                if (Number.isFinite(startMs) && createdAtMs < startMs) {
                    return false;
                }
            }
            if (this.filterEndDate) {
                const endMs = new Date(`${this.filterEndDate}T23:59:59.999`).getTime();
                if (Number.isFinite(endMs) && createdAtMs > endMs) {
                    return false;
                }
            }

            return true;
        });
    }

    private buildDistinctOptions(values: string[]): string[] {
        return Array.from(new Set(
            (Array.isArray(values) ? values : [])
                .map((value) => String(value || '').trim())
                .filter((value) => !!value)
        )).sort((left, right) => left.localeCompare(right));
    }

    private getDashboardCsvCellValue(item: DashboardApplicationItem, key: DashboardCsvColumnKey): string {
        switch (key) {
            case 'txtFormCode':
                return item.txtFormCode || '';
            case 'templateName':
                return item.templateName || '';
            case 'txtStatus':
                return item.txtStatus || '';
            case 'stageLabel':
                return item.stageLabel || '';
            case 'departmentName':
                return item.departmentName || '';
            case 'submittedByName':
                return item.submittedByName || '';
            case 'dteCreatedDate':
                return this.formatDate(item.dteCreatedDate);
            default:
                return '';
        }
    }

    private escapeCsvValue(value: any): string {
        const normalized = String(value ?? '');
        return `"${normalized.replace(/"/g, '""')}"`;
    }

    private downloadBlob(blob: Blob, filename: string): void {
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(objectUrl);
    }
}
