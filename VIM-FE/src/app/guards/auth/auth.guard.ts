import { inject, Injectable } from '@angular/core';
import { ActivatedRoute, ActivatedRouteSnapshot, CanActivate, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { catchError, map, Observable, pipe } from 'rxjs';
import { MenuService } from 'src/app/layout/menu-service/menu.service';
import { SharedDataService } from 'src/app/services/shared-data/shared-data.service';
import { UserService } from 'src/app/services/user/user.service';

// @Injectable({
//   providedIn: 'root'
// })
// export class AuthGuard implements CanActivate {
//   canActivate(
//     route: ActivatedRouteSnapshot,
//     state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
//     return true;
//   }

// }
export const canActivate: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  // const authService = inject(AuthenticationService);
  const router = inject(Router);
  const userService = inject(UserService);
  const sharedDataService = inject(SharedDataService);
  const menuService = inject(MenuService);

  // console.log('AuthGuard', route);
  // console.log('AuthGuard', route.url[0].path);
  const path = route.url[0] ? route.url[0].path : '';
  const normalizePath = (value: string | undefined | null): string =>
      (value || '').toString().trim().replace(/^\/+/, '').toLowerCase();
  const resolvedPath = normalizePath(path);

  const getRoleIdFromUser = (u: any): number | null => {
      if (!u) return null;
      const candidate = u?.cfgTblRole?.serRoleId ?? u?.cfgTblRole ?? u?.cfgTblRoleId ?? u?.roleId;
      const num = Number(candidate);
      if (Number.isFinite(num) && num > 0) return num;

      const roleName = (u?.cfgTblRole?.txtRoleName || u?.txtrole || '').toString().toUpperCase().trim();
      const roleMap: { [key: string]: number } = {
          'ADMIN': 1,
          'VENDOR': 2,
          'MARKETING': 3,
          'PROCURE': 4,
          'PROCUREMENT': 4,
          'FINANCE': 5,
          'AUDIT': 6,
          'CEO': 7
      };
      return roleMap[roleName] || null;
  };

  const getNormalizedUserRole = (u: any): string => {
      const roleName = (
          u?.cfgTblRole?.txtRoleName ||
          u?.txtrole ||
          u?.roleName ||
          ''
      ).toString().trim().toUpperCase();
      if (!roleName) return '';
      return roleName.startsWith('ROLE_') ? roleName : `ROLE_${roleName}`;
  };

  const isTrueFlag = (v: any): boolean =>
      v === true || v === 1 || v === '1' || v === 'true';

  // Routes that don't require menu permission check (master data routes)
  const allowedRoutesWithoutMenu = ['Dashboard', 'department', 'country', 'city', 'media-house', 
    'tax-category', 'product-category', 'product', 'users', 'password-policy', 'change-password', 
    'signature', 'role', 'channel', 'service-order', 'vendor-view', 'payment', 'OrderDepartment', 'pdf-editor', 
    'SES', 'auditLog', 'transactions-details', 'report', 'order-details', 'application', 'applicationsview', 
    'application-details', 'formbuilder', 'CAPF', 'approveApplicationFromEmail', 'rejectApplicationFromEmail',
    'pending-approvals', 'approved-applications', 'assign-asset-code', 'pr-code', 'po-code', 'menu-list', 'submenu-list'];

  let user: any = localStorage.getItem('user');
  if (user && user !== null) {
    user = JSON.parse(user);
  }

  const roleName = (
      user?.cfgTblRole?.txtRoleName ||
      user?.txtrole ||
      user?.roleName ||
      ''
  ).toString().trim().toUpperCase();
  const isAdmin =
      roleName === 'ADMIN' ||
      roleName === 'ROLE_ADMIN' ||
      roleName === 'SUPER ADMIN' ||
      roleName === 'ROLE_SUPER ADMIN';

  // Allow email approval/rejection routes without authentication
  if (path === 'approveApplicationFromEmail' || path === 'rejectApplicationFromEmail') {
    return true;
  }

  if (!localStorage.getItem('token')) {
    if (path !== 'auth') {
      router.navigateByUrl('auth/signin');
    }
    return false;
  }

  // Only admin can access Role Management page
  if (path === 'roles' && !isAdmin) {
    router.navigateByUrl('Dashboard');
    return false;
  }

  // Allow admin into Role Management even if submenu-role row is not seeded yet.
  if (path === 'roles' && isAdmin) {
    userService.me().subscribe((user: any) => {
      sharedDataService.saveUser(user);
    });
    return true;
  }

  if (localStorage.getItem('token')) {
    if (path === 'auth') {
      router.navigateByUrl('Dashboard');
    }

    // Skip menu check for allowed routes
    if (allowedRoutesWithoutMenu.includes(path)) {
      userService.me().subscribe((user: any) => {
        sharedDataService.saveUser(user);
      });
      return true;
    }

    // For other routes, check menu permissions
    const resolvedRoleId = getRoleIdFromUser(user);
    user && resolvedRoleId ? menuService.getAllSubMenuRoles(resolvedRoleId, user.serUserId).subscribe({
      next: (data) => {
        if (data && data.length) {
          const menus = data;
          const menu = menus.find(menu =>
            isTrueFlag(menu?.blIsEnabled) &&
            normalizePath(menu?.cfgTblSubMenu?.txtSubMenuUrl) === resolvedPath
          );
          if (menu) return;
        } else {
          // continue to fallback check below
        }

        // Fallback to allMenu role mapping when submenu-role row is missing.
        const normalizedUserRole = getNormalizedUserRole(user);
        menuService.getUserMenus().subscribe({
          next: (allMenus: any) => {
            const submenuList = (allMenus || []).reduce((acc: any[], m: any) => {
              if (m?.subMenus?.length) acc.push(...m.subMenus);
              return acc;
            }, []);
            const submenu = submenuList.find((sm: any) =>
              normalizePath(sm?.subMenuAction || sm?.txtSubMenuUrl) === resolvedPath
            );

            if (!submenu) {
              router.navigateByUrl('Dashboard');
              return;
            }

            const rolesStr = (submenu?.roles || '').toString().trim();
            if (!rolesStr) return; // open when roles empty

            const roles = rolesStr.split(',').map((r: string) => r.trim().toUpperCase()).filter(Boolean);
            if (normalizedUserRole && roles.includes(normalizedUserRole)) return;

            router.navigateByUrl('Dashboard');
          },
          error: () => {
            router.navigateByUrl('Dashboard');
          }
        });
      },
      error: (error) => {
        console.error('Error fetching submenu roles:', error);
      }
    }) : '';
    
    // Load user data for all authenticated users
    userService.me().subscribe((user: any) => {
      sharedDataService.saveUser(user);
    });
  } 
  // if (path === 'auth' && localStorage.getItem('token')) {
  //   router.navigateByUrl('dashboard');
  // }

  // if (localStorage.getItem('token')) {
  //   return true;
  // } else {
  //   router.navigateByUrl('auth/signin');
  // }

  // return userService.me().pipe(
  //   map(() => true),
  //   catchError(() => {
  //     return router.navigateByUrl('auth/sigin');
  //   })
  // );

  return true;
};
