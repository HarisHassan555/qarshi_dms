import { Component, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from 'src/app/NotificationService';
import { ActivityLogService } from 'src/app/services/activity-log/activity-log.service';
import { PermissionService } from 'src/app/services/shared-data/permission-service';

@Component({
  selector: 'app-activity-logs',
  templateUrl: './activity-logs.component.html',
  styleUrls: ['./activity-logs.component.css']
})
export class ActivityLogsComponent implements OnInit {
  @ViewChild('datatable') datatable: any;

  search = '';
  logs: any[] = [];

  private readonly subMenuName = 'Activity Logs';

  actionTypes = [
    { value: '', label: 'All Actions' },
    { value: 'LOGIN', label: 'Login' },
    { value: 'APPROVE', label: 'Approve' },
    { value: 'REJECT', label: 'Reject' },
    { value: 'SEND_BACK', label: 'Send Back' },
    { value: 'UPDATE', label: 'Update' },
    { value: 'FORM_SUBMIT', label: 'Form Submit' },
  ];

  statusOptions = [
    { value: '', label: 'All Statuses' },
    { value: 'SUCCESS', label: 'Success' },
    { value: 'FAILURE', label: 'Failure' },
  ];

  filterActionType = '';
  filterStatus = '';
  filterUserId = '';
  filterEntityId = '';
  filterStartDate = '';
  filterEndDate = '';

  cols = [
    { field: 'serActivityLogId', title: 'ID' },
    { field: 'txtActionType', title: 'Action' },
    { field: 'serUserId', title: 'User ID' },
    { field: 'txtUsername', title: 'Username' },
    { field: 'txtIpAddress', title: 'IP' },
    { field: 'txtDevice', title: 'Device' },
    { field: 'dteCreatedDate', title: 'Time' },
    { field: 'txtStatus', title: 'Status' },
    { field: 'serEntityId', title: 'Entity ID' },
    { field: 'txtMessage', title: 'Message' },
  ];

  constructor(
    private activityLogService: ActivityLogService,
    private notificationService: NotificationService,
    private permissionService: PermissionService,
    private router: Router
  ) { }

  ngOnInit(): void {
    const user = this.permissionService.getUserFromLocalStorage();
    if (!user?.serUserId) {
      this.router.navigateByUrl('Dashboard');
      return;
    }

    const roleId = typeof user.cfgTblRole === 'object'
      ? (user.cfgTblRole as any)?.serRoleId
      : user.cfgTblRole;

    this.permissionService.loadPermissionRoles(roleId, user.serUserId).subscribe(() => {
      if (!this.permissionService.canAccessSubMenu(this.subMenuName)) {
        this.notificationService.showMessage('You do not have permission to view activity logs.', 'danger');
        this.router.navigateByUrl('Dashboard');
        return;
      }

      this.loadLogs();
    });
  }

  loadLogs(): void {
    const filters: any = {};
    if (this.filterActionType) filters.actionType = this.filterActionType;
    if (this.filterStatus) filters.status = this.filterStatus;
    if (this.filterUserId) filters.userId = Number(this.filterUserId);
    if (this.filterEntityId) filters.entityId = Number(this.filterEntityId);
    if (this.filterStartDate) filters.startDate = this.filterStartDate;
    if (this.filterEndDate) filters.endDate = this.filterEndDate;

    this.activityLogService.getAll(filters).subscribe({
      next: (data) => {
        this.logs = (data || []).map((row: any) => ({
          ...row,
          txtDeviceShort: this.truncate(row.txtDevice, 40),
        }));
      },
      error: () => {
        this.notificationService.showMessage('Failed to load activity logs', 'danger');
      }
    });
  }

  clearFilters(): void {
    this.filterActionType = '';
    this.filterStatus = '';
    this.filterUserId = '';
    this.filterEntityId = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.loadLogs();
  }

  truncate(value: string, max: number): string {
    if (!value) return '';
    return value.length > max ? value.substring(0, max) + '...' : value;
  }
}
