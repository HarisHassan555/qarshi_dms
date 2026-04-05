import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { switchMap } from 'rxjs';
import { NotificationService } from 'src/app/NotificationService';
import { MenuService } from 'src/app/layout/menu-service/menu.service';

@Component({
  selector: 'app-submenu-list',
  templateUrl: './submenu-list.component.html',
  styleUrls: ['./submenu-list.component.css']
})
export class SubmenuListComponent implements OnInit {
  @ViewChild('modal') modal: any;

  search = '';
  subMenuForm!: FormGroup;
  isSubmit = false;
  subMenus: any[] = [];
  menus: any[] = [];
  editingSubMenu: any = null;
  blnStatus = true;
  blIsActive = true;

  cols = [
    { field: 'txtSubMenuName', title: 'Submenu Name' },
    { field: 'txtSubMenuUrl', title: 'Route' },
    { field: 'menuName', title: 'Parent Menu' },
    { field: 'intSubMenuOrder', title: 'Order' },
    { field: 'blnStatus', title: 'Status' },
    { field: 'blIsActive', title: 'Active' },
    { field: 'actions', title: 'Actions', sort: false, headerClass: 'justify-center' }
  ];

  constructor(
    private fb: FormBuilder,
    private menuService: MenuService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.subMenuForm = this.fb.group({
      serSubMenuId: ['', Validators.required],
      txtSubMenuName: ['', Validators.required],
      txtSubMenuUrl: ['', Validators.required],
      intSubMenuOrder: [0, Validators.required],
      serMenuId: ['', Validators.required]
    });
    this.getSubMenus();
  }

  getMenus(): void {
    this.menuService.getAllMenusRaw().subscribe((data: any) => {
      this.menus = Array.isArray(data) ? data : [];
    });
  }

  getSubMenus(): void {
    this.menuService.getAllMenusRaw().subscribe((data: any) => {
      const allMenus = (Array.isArray(data) ? data : []).filter((menu: any) =>
        menu?.blIsDeleted !== true
      );
      this.menus = allMenus;
      this.subMenus = allMenus.flatMap((menu: any) =>
        (menu?.cfgTblSubMenus || [])
          .filter((sm: any) => sm?.blIsDeleted !== true)
          .map((sm: any) => ({
          ...sm,
          cfgTblMenu: { serMenuId: menu?.serMenuId, txtMenuName: menu?.txtMenuName },
          menuName: menu?.txtMenuName || '-'
          }))
      );
    });
  }

  edit(subMenu: any): void {
    this.editingSubMenu = JSON.parse(JSON.stringify(subMenu));
    this.subMenuForm.patchValue({
      serSubMenuId: this.editingSubMenu.serSubMenuId,
      txtSubMenuName: this.editingSubMenu.txtSubMenuName,
      txtSubMenuUrl: this.editingSubMenu.txtSubMenuUrl,
      intSubMenuOrder: this.editingSubMenu.intSubMenuOrder,
      serMenuId: this.editingSubMenu?.cfgTblMenu?.serMenuId
    });
    this.blnStatus = this.editingSubMenu.blnStatus !== false;
    this.blIsActive = this.editingSubMenu.blIsActive !== false;
    this.isSubmit = false;
    this.modal.open();
  }

  submit(): void {
    this.isSubmit = true;
    if (this.subMenuForm.invalid || !this.editingSubMenu) return;

    const formValue = this.subMenuForm.value;
    const payload = {
      serSubMenuId: Number(formValue.serSubMenuId),
      txtSubMenuName: (formValue.txtSubMenuName || '').toString().trim(),
      txtSubMenuUrl: (formValue.txtSubMenuUrl || '').toString().trim(),
      intSubMenuOrder: Number(formValue.intSubMenuOrder),
      blnStatus: this.blnStatus,
      blIsActive: this.blIsActive,
      blIsDeleted: false,
      cfgTblMenu: { serMenuId: Number(formValue.serMenuId) }
    };

    this.menuService.updateSubMenu(payload).subscribe((res: any) => {
      if (this.isSuccessResponse(res)) {
        this.notificationService.showMessage('Submenu updated successfully', 'success');
        this.modal.close();
        this.getSubMenus();
      } else {
        this.notificationService.showMessage('Error occurred while updating submenu', 'danger');
      }
    });
  }

  deleteSubMenu(subMenu: any): void {
    if (!subMenu?.serSubMenuId) {
      this.notificationService.showMessage('Invalid submenu selected', 'danger');
      return;
    }

    const confirmed = window.confirm(
      `Delete submenu "${subMenu?.txtSubMenuName || ''}"?\nThis will also remove its permission entries.`
    );
    if (!confirmed) return;

    const subMenuId = Number(subMenu.serSubMenuId);

    this.menuService.deleteSubMenuRoleBySubMenu([subMenuId]).pipe(
      switchMap((roleDeleteRes: any) => {
        if (String(roleDeleteRes).trim() !== 'Success') {
          throw new Error('Failed to delete submenu permission rows');
        }
        return this.menuService.deleteSubMenu([subMenuId]);
      })
    ).subscribe({
      next: (res: any) => {
        if (String(res).trim() === 'Success') {
          this.notificationService.showMessage('Submenu deleted successfully', 'success');
          this.getSubMenus();
          return;
        }
        this.notificationService.showMessage('Error occurred while deleting submenu', 'danger');
      },
      error: () => {
        this.notificationService.showMessage('Error occurred while deleting submenu', 'danger');
      }
    });
  }

  private isSuccessResponse(res: any): boolean {
    const raw = (res ?? '').toString().trim();
    if (raw.toUpperCase() === 'SUCCESS') return true;
    if (typeof res === 'object' && res !== null) {
      const status = (res?.status || res?.message || '').toString().trim().toUpperCase();
      return status === 'SUCCESS';
    }
    if (raw.startsWith('{')) {
      try {
        const obj = JSON.parse(raw);
        const status = (obj?.status || obj?.message || '').toString().trim().toUpperCase();
        return status === 'SUCCESS';
      } catch {
        return false;
      }
    }
    return false;
  }
}
