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

  // Routes that don't require menu permission check (master data routes)
  const allowedRoutesWithoutMenu = ['Dashboard', 'department', 'country', 'city', 'media-house', 
    'tax-category', 'product-category', 'product', 'users', 'password-policy', 'change-password', 
    'role', 'channel', 'service-order', 'vendor-view', 'payment', 'OrderDepartment', 'pdf-editor', 
    'SES', 'auditLog', 'transactions-details', 'report', 'order-details', 'application', 'applicationsview', 'formbuilder', 'CAPF'];

  let user: any = localStorage.getItem('user');
  if (user && user !== null) {
    user = JSON.parse(user);
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
    user ? menuService.getAllSubMenuRoles(user.cfgTblRole.serRoleId, user.serUserId).subscribe({
      next: (data) => {
        if (data && data.length) {
          const menus = data;
          const menu = menus.find(menu =>  menu.blIsEnabled === true &&
            menu.cfgTblSubMenu.txtSubMenuUrl.includes(path)
          );
          debugger;
          if (!menu) {
            router.navigateByUrl('Dashboard');
          }
        }
      },
      error: (error) => {
        console.error('Error fetching submenu roles:', error);
      }
    }) : '';
    
    // Load user data for all authenticated users
    userService.me().subscribe((user: any) => {
      sharedDataService.saveUser(user);
    });
  } else {
    if (path !== 'auth') {
        router.navigateByUrl('auth/signin');
    }
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
