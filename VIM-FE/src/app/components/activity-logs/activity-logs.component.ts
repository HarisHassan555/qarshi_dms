import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from 'src/app/NotificationService';
import { ActivityLogService } from 'src/app/services/activity-log/activity-log.service';
import { PermissionService } from 'src/app/services/shared-data/permission-service';
import { UserService } from 'src/app/services/user/user.service';

@Component({
  selector: 'app-activity-logs',
  templateUrl: './activity-logs.component.html',
  styleUrls: ['./activity-logs.component.css']
})
export class ActivityLogsComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;

  search = '';
  logs: any[] = [];
  transactionForm!: FormGroup;
  isSubmit = false;
  canEditLogs = false;
  loadingTransaction = false;
  transactionEditable = false;
  users: any[] = [];
  selectedLog: any = null;

  private readonly subMenuName = 'Activity Logs';

  historyActions = [
    { value: 'APPROVED', label: 'Approved' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'SENT_BACK', label: 'Sent Back' },
    { value: 'SENT_BACK_TO_INITIATOR', label: 'Sent Back to Initiator' },
    { value: 'PO_VENDOR_TE_APPROVED', label: 'PO Vendor TE Approved' },
    { value: 'PO_VENDOR_TE_REJECTED', label: 'PO Vendor TE Rejected' },
  ];
  actionOptions: { value: string; label: string }[] = [];

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
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private activityLogService: ActivityLogService,
    private notificationService: NotificationService,
    private permissionService: PermissionService,
    private userService: UserService,
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

      this.canEditLogs = this.permissionService.canEditSubMenu(this.subMenuName);
      this.initForm();
      this.loadLogs();
      this.loadUsers();
    });
  }

  private initForm(): void {
    this.transactionForm = this.fb.group({
      serActivityLogId: [''],
      applicationId: [''],
      formCode: [''],
      applicationStatus: [''],
      historyIndex: [''],
      historyAction: [''],
      remarks: [''],
      approvedBy: [''],
      approverName: [''],
      level: [''],
      role: [''],
      approvedIp: [''],
      approvedVia: [''],
      approvedDate: [''],
      designation: [''],
      departmentName: [''],
    });
  }

  private loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (data: any) => {
        this.users = Array.isArray(data) ? data : [];
      },
      error: () => {
        this.users = [];
      }
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

  canEditRow(row: any): boolean {
    if (!this.canEditLogs || !row) {
      return false;
    }
    const action = (row.txtActionType || '').toString().toUpperCase();
    return action === 'APPROVE' || action === 'REJECT' || action === 'SEND_BACK';
  }

  edit(row: any): void {
    if (!this.canEditRow(row)) {
      this.notificationService.showMessage(
        'Only approve, reject, and send back application transactions can be edited.',
        'danger'
      );
      return;
    }

    this.isSubmit = false;
    this.loadingTransaction = true;
    this.transactionEditable = false;
    this.selectedLog = row;
    this.transactionForm.reset();
    this.modal.open();

    this.activityLogService.getTransaction(row.serActivityLogId).subscribe({
      next: (tx) => {
        this.loadingTransaction = false;
        if (!tx?.editable) {
          this.notificationService.showMessage(tx?.message || 'This transaction cannot be edited', 'danger');
          this.modal.close();
          return;
        }

        this.transactionEditable = true;
        this.actionOptions = this.buildActionOptions(tx.historyAction);
        this.transactionForm.patchValue({
          serActivityLogId: tx.serActivityLogId,
          applicationId: tx.applicationId,
          formCode: tx.formCode,
          applicationStatus: tx.applicationStatus,
          historyIndex: tx.historyIndex,
          historyAction: tx.historyAction || 'APPROVED',
          remarks: tx.remarks || '',
          approvedBy: tx.approvedBy ?? '',
          approverName: tx.approverName || '',
          level: tx.level ?? '',
          role: tx.role || '',
          approvedIp: tx.approvedIp || '',
          approvedVia: tx.approvedVia || 'SYSTEM',
          approvedDate: this.formatDateForInput(tx.approvedDate || tx.logCreatedDate),
          designation: tx.designation || '',
          departmentName: tx.departmentName || '',
        });
      },
      error: () => {
        this.loadingTransaction = false;
        this.notificationService.showMessage('Failed to load application transaction', 'danger');
        this.modal.close();
      }
    });
  }

  onApproverChange(): void {
    const userId = Number(this.transactionForm.get('approvedBy')?.value || 0);
    const user = this.users.find((u) => Number(u.serUserId) === userId);
    if (user) {
      this.transactionForm.patchValue({
        approverName: user.txtUserName || '',
        designation: user.txtDesignation || '',
        departmentName: user.txtDepartmentName || '',
      });
    }
  }

  submit(): void {
    if (!this.canEditLogs || !this.transactionEditable) {
      return;
    }

    this.isSubmit = true;
    const raw = this.transactionForm.getRawValue();
    const payload: any = {
      serActivityLogId: Number(raw.serActivityLogId),
      historyAction: raw.historyAction,
      remarks: raw.remarks,
      approvedBy: raw.approvedBy !== '' ? Number(raw.approvedBy) : null,
      level: raw.level !== '' ? Number(raw.level) : null,
      role: raw.role,
      approvedIp: raw.approvedIp,
      approvedVia: raw.approvedVia,
    };

    if (raw.approvedDate) {
      payload.approvedDate = raw.approvedDate.replace('T', ' ');
    }

    this.activityLogService.updateTransaction(payload).subscribe({
      next: (res) => {
        if (res?.status === 'Success') {
          this.notificationService.showMessage(res.message || 'Application transaction updated', 'success');
          this.modal.close();
          this.loadLogs();
        } else {
          this.notificationService.showMessage(res?.message || 'Update failed', 'danger');
        }
      },
      error: () => {
        this.notificationService.showMessage('Error updating application transaction', 'danger');
      }
    });
  }

  formatDateForInput(value: any): string {
    if (!value) return '';
    const d = new Date(value);
    if (isNaN(d.getTime())) {
      const normalized = String(value).replace(' ', 'T');
      const retry = new Date(normalized);
      if (isNaN(retry.getTime())) return '';
      return this.toLocalInput(retry);
    }
    return this.toLocalInput(d);
  }

  private toLocalInput(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private buildActionOptions(currentAction?: string): { value: string; label: string }[] {
    const options = [...this.historyActions];
    const current = (currentAction || '').trim();
    if (current && !options.some((opt) => opt.value === current)) {
      options.unshift({ value: current, label: current });
    }
    return options;
  }

  truncate(value: string, max: number): string {
    if (!value) return '';
    return value.length > max ? value.substring(0, max) + '...' : value;
  }
}
