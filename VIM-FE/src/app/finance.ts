import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
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

    private currentUserId = 0;
    private readonly userNameById = new Map<number, string>();

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
            return 'bg-rose-100 text-rose-700';
        }
        if (normalizedStatus === 'APPROVED' || normalizedStatus === 'COMPLETED') {
            return 'bg-emerald-100 text-emerald-700';
        }
        if (normalizedStatus === 'OPINION_PENDING') {
            return 'bg-sky-100 text-sky-700';
        }
        return 'bg-amber-100 text-amber-700';
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
            this.scopeLabel = this.isAdmin ? 'All users workflow' : 'Your workflow';
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
            const [submittedResponse, pendingResponse, usersResponse, activityLogs] = await Promise.all([
                firstValueFrom(this.templateWorkflowService.getMyTemplateApplications(
                    this.currentUserId,
                    0,
                    100,
                    '',
                    this.isAdmin
                )),
                firstValueFrom(this.templateWorkflowService.getTemplatePendingApprovals(
                    this.currentUserId,
                    this.isAdmin,
                    0,
                    100,
                    ''
                )),
                firstValueFrom(this.userService.getUsers()),
                firstValueFrom(this.activityLogService.getAll(activityFilters))
            ]);

            this.userNameById.clear();
            (Array.isArray(usersResponse) ? usersResponse : []).forEach((user: any) => {
                const userId = this.resolveUserId(user);
                if (userId > 0) {
                    this.userNameById.set(userId, user?.txtUserName || user?.userName || user?.name || `User ${userId}`);
                }
            });

            const submittedItems = (Array.isArray(submittedResponse?.items) ? submittedResponse.items : [])
                .map((application: any) => this.toDashboardItem(application, 'submitted', false));
            const pendingItems = (Array.isArray(pendingResponse?.items) ? pendingResponse.items : [])
                .map((application: any) => this.toDashboardItem(application, 'pending', true));

            const mergedItems = this.mergeDashboardItems(submittedItems, pendingItems);

            mergedItems.forEach((item) => {
                const stageInfo = this.resolveStageInfo(item);
                item.stageKey = stageInfo.key;
                item.stageLabel = stageInfo.label;
                item.stageOrder = stageInfo.order;
            });

            this.totalApplications = Number(submittedResponse?.total || submittedItems.length || 0);
            this.pendingApplications = submittedItems
                .filter((item: DashboardApplicationItem) => this.isOpenStatus(item.txtStatus))
                .length;
            this.completedApplications = submittedItems
                .filter((item: DashboardApplicationItem) => this.isCompletedStatus(item.txtStatus))
                .length;
            this.rejectedApplications = submittedItems
                .filter((item: DashboardApplicationItem) => this.isRejectedStatus(item.txtStatus))
                .length;
            this.actionableApplications = Number(pendingResponse?.total || pendingItems.length || 0);

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

            this.boardColumns = this.buildBoardColumns(mergedItems);
            this.recentApplications = [...mergedItems]
                .sort((left, right) => this.getCreatedAtMillis(right) - this.getCreatedAtMillis(left))
                .slice(0, 6);
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
}
