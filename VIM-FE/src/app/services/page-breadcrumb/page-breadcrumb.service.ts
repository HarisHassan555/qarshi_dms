import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ProductTourService } from '../../product-tour/product-tour.service';

export interface PageBreadcrumbItem {
  label: string;
  /** Router link when the crumb is navigable */
  link?: string | any[];
}

/** Parent menu label shown before the current page (matches sidebar groups). */
const PAGE_SECTION: Record<string, string> = {
  signature: 'Master Data',
  'sap-hcm-employees': 'Master Data',
  organizations: 'Master Data',
  'sub-areas': 'Master Data',
  divisions: 'Master Data',
  department: 'Master Data',
  'workflow-pipelines': 'Master Data',
  'template-list': 'Velocity',
  'template-builder': 'Velocity',
  'template-fill': 'Velocity',
  'template-pending-approvals': 'Velocity',
  'template-approved-applications': 'Velocity',
  'my-application': 'Velocity',
  'department-application': 'Velocity',
  'all-applications': 'Velocity',
  'template-approval': 'Velocity',
  'univer-form-builder': 'Velocity',
  users: 'User Management',
  roles: 'User Management',
  'change-password': 'User Management',
  activitylogs: 'User Management',
  'delegation-management': 'User Management',
  chat: 'Velocity',
  'notification-management': 'Velocity',
  reports: 'Velocity',
  'password-policy': 'User Management',
};

@Injectable({
  providedIn: 'root',
})
export class PageBreadcrumbService {
  private readonly crumbsSubject = new BehaviorSubject<PageBreadcrumbItem[]>([]);
  readonly crumbs$ = this.crumbsSubject.asObservable();

  constructor(
    private router: Router,
    private productTourService: ProductTourService
  ) {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.refresh());

    this.refresh();
  }

  refresh(): void {
    this.crumbsSubject.next(this.buildCrumbs());
  }

  private buildCrumbs(): PageBreadcrumbItem[] {
    const segments = this.urlSegments();
    if (segments[0] === 'share') {
      return [];
    }

    const pageKey = this.productTourService.getPageKey();
    const pageLabel = this.productTourService.getCurrentPageLabel();

    if (pageKey === 'Dashboard' || segments.length === 0) {
      return [{ label: 'Dashboard' }];
    }

    const crumbs: PageBreadcrumbItem[] = [{ label: 'Home', link: '/Dashboard' }];
    const section = PAGE_SECTION[pageKey];
    if (section) {
      crumbs.push({ label: section });
    }

    if (pageKey === 'my-application' && this.hasRouteParamAfter('my-application', segments)) {
      crumbs.push({ label: pageLabel, link: '/my-application' });
      crumbs.push({ label: 'Application details' });
      return crumbs;
    }

    if (pageKey === 'template-fill' && this.hasRouteParamAfter('template-fill', segments)) {
      const id = segments[segments.indexOf('template-fill') + 1];
      crumbs.push({ label: pageLabel, link: '/template-list' });
      crumbs.push({ label: id ? `Form ${id}` : pageLabel });
      return crumbs;
    }

    if (pageKey === 'template-builder') {
      const id = this.router.parseUrl(this.router.url).queryParamMap.get('id');
      crumbs.push({ label: pageLabel });
      if (id) {
        crumbs.push({ label: `Template ${id}` });
      }
      return crumbs;
    }

    crumbs.push({ label: pageLabel });
    return crumbs;
  }

  private urlSegments(): string[] {
    let path = this.router.url.split('?')[0] || '';
    path = path.replace(/^\/velocity\/?/i, '/');
    return path.split('/').filter(Boolean);
  }

  private hasRouteParamAfter(segment: string, segments: string[]): boolean {
    const idx = segments.indexOf(segment);
    return idx >= 0 && !!segments[idx + 1];
  }
}
