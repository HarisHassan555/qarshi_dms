import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { DepartmentService } from 'src/app/services/department/department.service';
import { UserService } from 'src/app/services/user/user.service';
import { PermissionService } from '../../services/shared-data/permission-service';

@Component({
  selector: 'app-department',
  templateUrl: './department.component.html',
  styleUrls: ['./department.component.css']
})
export class DepartmentComponent implements OnInit {
  @ViewChild('datatable') datatable: any;
  @ViewChild('modal') modal: any;
  @ViewChild('assignUsersModal') assignUsersModal: any;

  search = '';
  departmentForm!: FormGroup;
  isSubmit = false;
  departments: any;
  users: any;
  allUsers: any;
  selectedDepartment: any = null;
  selectedUserIds: number[] = [];
  selectedDepartmentHeadIds: number[] = [];
  blnStatus = false;
  editCase = false;

  cols = [
    { field: 'txtDepartmentName', title: 'Department Name' },
    { field: 'txtDepartmentCode', title: 'Department Code' },
    { field: 'userCount', title: 'Users Assigned' },
    { field: 'departmentHeadName', title: 'Department Head' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' },
  ];

  constructor(
    private fb: FormBuilder,
    private departmentService: DepartmentService,
    private userService: UserService,
    private notificationService: NotificationService,
    private permissionService: PermissionService
  ) { }

  private getCurrentUserContext(): { roleId?: number; userId?: number } {
    const userJson = localStorage.getItem('user');
    if (!userJson) {
      return {};
    }

    const user: any = JSON.parse(userJson);
    const roleId = Number(user?.cfgTblRole?.serRoleId ?? user?.cfgTblRole);
    const userId = Number(user?.serUserId);

    return {
      roleId: Number.isFinite(roleId) ? roleId : undefined,
      userId: Number.isFinite(userId) ? userId : undefined
    };
  }

  ngOnInit() {
    this.departmentForm = this.fb.group({
      serDepartmentId: [''],
      txtDepartmentName: ['', Validators.required],
      txtDepartmentCode: ['', Validators.required],
      txtDescription: ['']
    });

    const { roleId, userId } = this.getCurrentUserContext();
    if (!roleId || !userId) {
      this.getAllUsers();
      return;
    }

    this.permissionService.loadPermissionRoles(roleId, userId).subscribe(() => {
      this.getAllUsers();
    });
  }

  getDepartments() {
    this.departments = [];
    this.departmentService
      .getAll()
      .subscribe((data: any) => {
        if (data) {
          // Count users assigned to each department
          this.departments = data.map((dept: any) => {
            let userCount = 0;

            // Count from cfgTblUsers array (primary source)
            if (dept.cfgTblUsers && Array.isArray(dept.cfgTblUsers)) {
              userCount = dept.cfgTblUsers.length;
            }

            // Fallback: Count from employees relationship if cfgTblUsers is not available
            if (userCount === 0 && dept.hrTblEmployees && Array.isArray(dept.hrTblEmployees)) {
              userCount = dept.hrTblEmployees.length;
            }

            // Fallback: Count from allUsers if neither cfgTblUsers nor hrTblEmployees is available
            if (userCount === 0 && this.allUsers) {
              const usersInDept = this.allUsers.filter((user: any) =>
                user.hrTblDepartment &&
                user.hrTblDepartment.serDepartmentId === dept.serDepartmentId
              );
              userCount = usersInDept.length;
            }

            // Get department head name
            let departmentHeadName = '-';
            if (dept.departmentHead && dept.departmentHead.txtUserName) {
              departmentHeadName = dept.departmentHead.txtUserName;
            } else if (dept.serDepartmentHeadId && this.allUsers) {
              // Fallback: find users from allUsers if departmentHead relationship is not loaded
              const headIds = String(dept.serDepartmentHeadId).split(',').map(id => id.trim());
              const headNames: string[] = [];
              headIds.forEach(id => {
                const headUser = this.allUsers.find((user: any) => String(user.serUserId) === id);
                if (headUser && headUser.txtUserName) {
                  headNames.push(headUser.txtUserName);
                }
              });
              if (headNames.length > 0) {
                departmentHeadName = headNames.join(', ');
              }
            }

            return {
              ...dept,
              userCount: userCount,
              departmentHeadName: departmentHeadName
            };
          });
        }
      });
  }

  getAllUsers() {
    this.userService.getUsers()
      .subscribe((data: any) => {
        if (data) {
          this.allUsers = data;
        }
        this.getDepartments();
      });
  }

  add() {
    this.permissionService.canAdd('Department').subscribe(canAdd => {
      if (canAdd === true) {
        this.isSubmit = false;
        this.departmentForm.reset();
        this.blnStatus = false;
        this.editCase = false;
        this.modal.open();
        return;
      } else {
        this.notificationService.showMessage('You do not have permission to add new departments', 'danger');
        return;
      }
    });
  }

  edit(department: any) {
    this.permissionService.canUpdateAsync('Department').subscribe(canUpdate => {
      if (!canUpdate) {
        this.notificationService.showMessage('You do not have permission to edit departments', 'danger');
        return;
      }
      this.departmentForm.reset();
      this.modal.open();
      this.departmentForm.patchValue(department);
      this.blnStatus = department.blnStatus;
      // @ts-ignore
      this.editCase = true;
    });
  }

  submit() {
    this.isSubmit = true;
    if (this.departmentForm.invalid) return;

    const payload = this.departmentForm.value;

    if (payload.serDepartmentId) {
      payload.blnStatus = this.blnStatus;
      payload.blIsDeleted = false;
    } else {
      delete payload.serDepartmentId;
    }

    let existingDepartment;
    // @ts-ignore
    if (this.editCase) {
      // @ts-ignore
      existingDepartment = this.departments.find(dept =>
        (dept.txtDepartmentName === payload.txtDepartmentName || dept.txtDepartmentCode === payload.txtDepartmentCode) &&
        dept.serDepartmentId !== payload.serDepartmentId
      );
    } else {
      // @ts-ignore
      existingDepartment = this.departments.find(dept =>
        dept.txtDepartmentName === payload.txtDepartmentName || dept.txtDepartmentCode === payload.txtDepartmentCode
      );
    }

    // @ts-ignore
    if (existingDepartment) {
      this.notificationService.showMessage('Department with this name or code already exists', 'danger');
      return;
    }

    this.departmentService
      .save(payload)
      .subscribe((data: any) => {
        if (data == "Success") {
          this.notificationService.showMessage('Record saved successfully', 'success');
          this.isSubmit = false;
          this.departmentForm.reset();
          this.modal.close();
          this.getDepartments();
        } else {
          this.notificationService.showMessage('Error occurred while saving', 'danger');
        }
      });
  }

  openAssignUsersModal(department: any) {
    this.selectedDepartment = department;
    this.selectedUserIds = [];
    this.selectedDepartmentHeadIds = [];

    if (department.serDepartmentHeadId) {
      this.selectedDepartmentHeadIds = String(department.serDepartmentHeadId)
        .split(',')
        .map(id => Number(id.trim()))
        .filter(id => !isNaN(id));

      // Pre-fill selectedUserIds with HODs to ensure they are consistent
      this.selectedDepartmentHeadIds.forEach(id => {
        if (!this.selectedUserIds.includes(id)) {
          this.selectedUserIds.push(id);
        }
      });
    }

    // Load users already assigned to this department from backend
    this.departmentService.getUsersByDepartment(department.serDepartmentId)
      .subscribe((users: any) => {
        if (users && users.length > 0) {
          users.forEach((user: any) => {
            if (!this.selectedUserIds.includes(user.serUserId)) {
              this.selectedUserIds.push(user.serUserId);
            }
          });
        }
      });

    // Also check if users are already loaded and have department reference
    if (this.allUsers) {
      this.allUsers.forEach((user: any) => {
        if (user.hrTblDepartment && user.hrTblDepartment.serDepartmentId === department.serDepartmentId) {
          if (this.selectedUserIds.indexOf(user.serUserId) === -1) {
            this.selectedUserIds.push(user.serUserId);
          }
        }
      });
    }

    this.assignUsersModal.open();
  }

  toggleUserSelection(userId: number) {
    const index = this.selectedUserIds.indexOf(userId);
    if (index > -1) {
      this.selectedUserIds.splice(index, 1);
      // If the deselected user was a department head, remove from heads list
      const headIndex = this.selectedDepartmentHeadIds.indexOf(userId);
      if (headIndex > -1) {
        this.selectedDepartmentHeadIds.splice(headIndex, 1);
      }
    } else {
      this.selectedUserIds.push(userId);
    }
  }

  toggleDepartmentHead(userId: number) {
    const index = this.selectedDepartmentHeadIds.indexOf(userId);
    if (index > -1) {
      this.selectedDepartmentHeadIds.splice(index, 1);
    } else {
      this.selectedDepartmentHeadIds.push(userId);
      // Automatically select the user as a member if they are made HOD
      if (!this.isUserSelected(userId)) {
        this.selectedUserIds.push(userId);
      }
    }
  }

  isUserSelected(userId: number): boolean {
    return this.selectedUserIds.indexOf(userId) > -1;
  }

  assignUsers() {
    if (!this.selectedDepartment) {
      this.notificationService.showMessage('Please select a department', 'danger');
      return;
    }

    // Validate that department heads are selected from assigned users
    if (this.selectedDepartmentHeadIds.length > 0) {
      const invalidHeads = this.selectedDepartmentHeadIds.filter(id => !this.selectedUserIds.includes(id));
      if (invalidHeads.length > 0) {
        this.notificationService.showMessage('All department heads must be selected from the assigned users', 'danger');
        return;
      }
    }

    // Use the backend endpoint for assigning users to department
    const headsString = this.selectedDepartmentHeadIds.length > 0 ? this.selectedDepartmentHeadIds.join(',') : null;

    this.departmentService.assignUsersToDepartment(
      this.selectedDepartment.serDepartmentId,
      this.selectedUserIds,
      headsString
    ).subscribe(
      (response: any) => {
        if (response && (response.includes('Success') || response.includes('"status":"Success"'))) {
          this.notificationService.showMessage('Users assigned to department successfully', 'success');
          this.assignUsersModal.close();
          this.getAllUsers();
        } else {
          this.notificationService.showMessage('Error occurred while assigning users', 'danger');
        }
      },
      (error) => {
        console.error('Error assigning users:', error);
        this.notificationService.showMessage('Error occurred while assigning users', 'danger');
      }
    );
  }

  getUsersForDepartment(department: any): any[] {
    if (!department.hrTblEmployees) return [];
    return department.hrTblEmployees;
  }
}
