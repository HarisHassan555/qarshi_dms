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
  blnStatus = false;
  editCase = false;

  cols = [
    { field: 'txtDepartmentName', title: 'Department Name' },
    { field: 'txtDepartmentCode', title: 'Department Code' },
    { field: 'userCount', title: 'Users Assigned' },
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

  ngOnInit() {
    this.departmentForm = this.fb.group({
      serDepartmentId: [''],
      txtDepartmentName: ['', Validators.required],
      txtDepartmentCode: ['', Validators.required],
      txtDescription: ['']
    });

    const userJson = localStorage.getItem('user');
    let user: {
      cfgTblRole: number | undefined;
      serUserId: number;
    };

    if (userJson) {
      // @ts-ignore
      user = JSON.parse(userJson) as CfgTblUser;
    }
    // @ts-ignore
    this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
      this.getDepartments();
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
            
            // Count from employees relationship
            if (dept.hrTblEmployees) {
              userCount += dept.hrTblEmployees.length;
            }
            
            // Also count from users if available
            if (this.allUsers) {
              const usersInDept = this.allUsers.filter((user: any) => 
                user.hrTblDepartment && 
                user.hrTblDepartment.serDepartmentId === dept.serDepartmentId
              );
              userCount = Math.max(userCount, usersInDept.length);
            }
            
            return {
              ...dept,
              userCount: userCount
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
      });
  }

  add() {
    const userJson = localStorage.getItem('user');
    let user: {
      cfgTblRole: number | undefined;
      serUserId: number;
    };

    if (userJson) {
      // @ts-ignore
      user = JSON.parse(userJson) as CfgTblUser;
    }
    // @ts-ignore
    this.permissionService.loadPermissionRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe(() => {
      // @ts-ignore
      this.permissionService.canAdd('Department').subscribe(canAdd => {
        if (canAdd == true) {
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
    });
  }

  edit(department: any) {
    if (!this.permissionService.canUpdate('Department')) {
      this.notificationService.showMessage('You do not have permission to edit departments', 'danger');
      return;
    }
    this.departmentForm.reset();
    this.modal.open();
    this.departmentForm.patchValue(department);
    this.blnStatus = department.blnStatus;
    // @ts-ignore
    this.editCase = true;
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
    
    // Load users already assigned to this department from backend
    this.departmentService.getUsersByDepartment(department.serDepartmentId)
      .subscribe((users: any) => {
        if (users && users.length > 0) {
          this.selectedUserIds = users.map((user: any) => user.serUserId);
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
    } else {
      this.selectedUserIds.push(userId);
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

    // Use the backend endpoint for assigning users to department
    this.departmentService.assignUsersToDepartment(
      this.selectedDepartment.serDepartmentId,
      this.selectedUserIds
    ).subscribe(
      (response: any) => {
        if (response && (response.includes('Success') || response.includes('"status":"Success"'))) {
          this.notificationService.showMessage('Users assigned to department successfully', 'success');
          this.assignUsersModal.close();
          this.getDepartments();
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

