import { Component, OnInit } from '@angular/core';
import { PermissionService } from '../../services/shared-data/permission-service';

@Component({
  selector: 'app-capf',
  templateUrl: './capf.component.html',
  styleUrls: ['./capf.component.css']
})
export class CapfComponent implements OnInit {
  search = '';
  
  constructor(
    private permissionService: PermissionService
  ) { }

  ngOnInit() {
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
      // Component initialized
    });
  }
}









