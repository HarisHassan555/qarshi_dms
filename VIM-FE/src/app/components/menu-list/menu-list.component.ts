import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NotificationService } from 'src/app/NotificationService';
import { MenuService } from 'src/app/layout/menu-service/menu.service';

@Component({
  selector: 'app-menu-list',
  templateUrl: './menu-list.component.html',
  styleUrls: ['./menu-list.component.css']
})
export class MenuListComponent implements OnInit {
  @ViewChild('modal') modal: any;

  search = '';
  menuForm!: FormGroup;
  isSubmit = false;
  menus: any[] = [];
  editingMenu: any = null;
  blnStatus = true;
  blIsActive = true;

  cols = [
    { field: 'txtMenuName', title: 'Menu Name' },
    { field: 'txtMenuIcons', title: 'Icon' },
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
    this.menuForm = this.fb.group({
      serMenuId: ['', Validators.required],
      txtMenuName: ['', Validators.required],
      txtMenuIcons: ['']
    });
    this.getMenus();
  }

  getMenus(): void {
    this.menuService.getAllMenusRaw().subscribe((data: any) => {
      this.menus = Array.isArray(data) ? data : [];
    });
  }

  edit(menu: any): void {
    this.editingMenu = JSON.parse(JSON.stringify(menu));
    this.menuForm.patchValue({
      serMenuId: this.editingMenu.serMenuId,
      txtMenuName: this.editingMenu.txtMenuName,
      txtMenuIcons: this.editingMenu.txtMenuIcons
    });
    this.blnStatus = this.editingMenu.blnStatus !== false;
    this.blIsActive = this.editingMenu.blIsActive !== false;
    this.isSubmit = false;
    this.modal.open();
  }

  submit(): void {
    this.isSubmit = true;
    if (this.menuForm.invalid || !this.editingMenu) return;

    const formValue = this.menuForm.value;
    const payload = {
      serMenuId: Number(formValue.serMenuId),
      txtMenuName: (formValue.txtMenuName || '').toString().trim(),
      txtMenuIcons: (formValue.txtMenuIcons || '').toString().trim(),
      blnStatus: this.blnStatus,
      blIsActive: this.blIsActive,
      blIsDeleted: false
    };

    this.menuService.updateMenu(payload).subscribe((res: any) => {
      if (this.isSuccessResponse(res)) {
        this.notificationService.showMessage('Menu updated successfully', 'success');
        this.modal.close();
        this.getMenus();
      } else {
        this.notificationService.showMessage('Error occurred while updating menu', 'danger');
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
