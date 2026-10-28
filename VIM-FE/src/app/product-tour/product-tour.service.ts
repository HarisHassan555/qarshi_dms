import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { filter } from 'rxjs/operators';
import { PRODUCT_TOUR_CATALOG } from './tour-definitions';
import {
  ProductTourDefinition,
  ProductTourHistoryEntry,
  ProductTourRuntimeState,
  ProductTourStep,
} from './tour.types';

const HISTORY_KEY = 'velocity_tour_history';

export interface ProductTourUiState {
  active: boolean;
  launcherOpen: boolean;
  runtime: ProductTourRuntimeState | null;
  step: ProductTourStep | null;
  stepIndex: number;
  stepCount: number;
  progressPct: number;
  hasTarget: boolean;
  /** Suppress spotlight CSS transition while the target moves (modals, tabs, editors). */
  suppressSpotlightTransition: boolean;
  spotlight: { left: number; top: number; width: number; height: number } | null;
  popover: { left: number; top: number; transform: string };
}

@Injectable({
  providedIn: 'root',
})
export class ProductTourService {
  private readonly uiSubject = new BehaviorSubject<ProductTourUiState>(this.emptyUi());
  readonly ui$ = this.uiSubject.asObservable();

  private runtime: ProductTourRuntimeState | null = null;
  private layoutInProgress = false;
  private stepBusy = false;
  private positionRefreshTimer: number | null = null;

  constructor(private router: Router) {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => {
        if (this.runtime) {
          this.schedulePositionRefresh(120);
        }
      });

    window.addEventListener('resize', () => {
      if (this.runtime) {
        this.schedulePositionRefresh(80);
      }
    });
    window.addEventListener(
      'scroll',
      () => {
        if (this.runtime && !this.layoutInProgress) {
          this.schedulePositionRefresh(100);
        }
      },
      true
    );
  }

  openLauncher(): void {
    this.patchUi({ launcherOpen: true });
  }

  closeLauncher(): void {
    this.patchUi({ launcherOpen: false });
  }

  getToursForCurrentUser(): ProductTourDefinition[] {
    const role = this.currentRoleName();
    const page = this.currentPageKey();
    return PRODUCT_TOUR_CATALOG.filter((t) => {
      if (t.page !== page) {
        return false;
      }
      return this.roleCanSeeTour(role, t);
    });
  }

  /** Start the only tour for this page, or open the launcher when several exist. */
  openTourForCurrentPage(): void {
    const tours = this.getToursForCurrentUser();
    if (tours.length === 1) {
      void this.startTour(tours[0].id);
      return;
    }
    this.openLauncher();
  }

  /** Route segment used for tours and breadcrumbs (e.g. `template-fill`, `Dashboard`). */
  getPageKey(): string {
    return this.currentPageKey();
  }

  getCurrentPageLabel(): string {
    const page = this.currentPageKey();
    if (page === 'Dashboard') {
      return 'Dashboard';
    }
    if (page === 'signature') {
      return 'Signature';
    }
    if (page === 'sap-hcm-employees') {
      return 'Sap Employees';
    }
    if (page === 'organizations') {
      return 'Organizations';
    }
    if (page === 'sub-areas') {
      return 'Sub Areas';
    }
    if (page === 'divisions') {
      return 'Divisions';
    }
    if (page === 'workflow-pipelines') {
      return 'Workflow Pipelines';
    }
    if (page === 'template-list') {
      return 'Digital Document List';
    }
    if (page === 'template-fill') {
      return 'Fill Form';
    }
    if (page === 'change-password') {
      return 'Change Password';
    }
    if (page === 'users') {
      return 'Users';
    }
    if (page === 'roles') {
      return 'Role Management';
    }
    if (page === 'activitylogs') {
      return 'Activity Logs';
    }
    if (page === 'chat') {
      return 'Messages';
    }
    if (page === 'delegation-management') {
      return 'Delegation Management';
    }
    if (page === 'email-templates') {
      return 'Email Templates';
    }
    if (page === 'department-application') {
      return 'Department Applications';
    }
    if (page === 'template-approved-applications') {
      return 'Approved Applications';
    }
    if (page === 'my-application') {
      return 'My Digital Applications';
    }
    if (page === 'template-pending-approvals') {
      return 'Pending Approvals';
    }
    if (page === 'template-approval') {
      return 'Template Approval';
    }
    if (page === 'template-builder') {
      return 'Document Builder';
    }
    return page.replace(/-/g, ' ');
  }

  isTourCompleted(tourId: string): boolean {
    const history = this.loadHistory();
    return !!history[tourId]?.completed;
  }

  async startTour(tourId: string): Promise<void> {
    const def = PRODUCT_TOUR_CATALOG.find((t) => t.id === tourId);
    if (!def || !def.steps.length) {
      return;
    }
    if (def.page && this.currentPageKey() !== def.page) {
      await this.router.navigate([def.page]);
      await this.delay(300);
    }
    this.runtime = { tourId, steps: def.steps, index: 0 };
    this.closeLauncher();
    this.patchUi({ active: true, runtime: this.runtime });
    await this.runStepWithLock();
  }

  async next(): Promise<void> {
    if (!this.runtime || this.stepBusy) {
      return;
    }
    if (this.runtime.index >= this.runtime.steps.length - 1) {
      this.end(true);
      return;
    }
    this.runtime.index += 1;
    await this.runStepWithLock();
  }

  async prev(): Promise<void> {
    if (!this.runtime || this.runtime.index <= 0 || this.stepBusy) {
      return;
    }
    this.runtime.index -= 1;
    await this.runStepWithLock();
  }

  private async runStepWithLock(): Promise<void> {
    if (this.stepBusy) {
      return;
    }
    this.stepBusy = true;
    try {
      await this.runStep();
    } finally {
      this.stepBusy = false;
    }
  }

  end(completed = false): void {
    if (!this.runtime) {
      return;
    }
    const history = this.loadHistory();
    history[this.runtime.tourId] = {
      completed,
      at: new Date().toISOString(),
    };
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
    window.dispatchEvent(
      new CustomEvent('velocity-tour-prepare', { detail: { action: 'roles-close-modals' } })
    );
    window.dispatchEvent(
      new CustomEvent('velocity-tour-prepare', { detail: { action: 'activitylogs-close-modal' } })
    );
    [
      'dept-app-close-csv',
      'template-approved-close-csv',
      'template-pending-close-csv',
      'template-builder-close-modals',
      'my-app-close-detail',
      'email-templates-close-modals',
    ].forEach((action) => {
      window.dispatchEvent(new CustomEvent('velocity-tour-prepare', { detail: { action } }));
    });
    this.runtime = null;
    this.patchUi({
      active: false,
      runtime: null,
      spotlight: null,
      step: null,
      hasTarget: false,
    });
  }

  skip(): void {
    this.end(false);
  }

  private async runStep(): Promise<void> {
    if (!this.runtime) {
      return;
    }
    const step = this.runtime.steps[this.runtime.index];
    const stepIndex = this.runtime.index;
    const stepCount = this.runtime.steps.length;
    const progressPct = ((stepIndex + 1) / stepCount) * 100;

    this.patchUi({
      step,
      stepIndex,
      stepCount,
      progressPct,
      spotlight: null,
      suppressSpotlightTransition: true,
    });

    if (step.route) {
      const target = step.route.startsWith('/') ? step.route.slice(1) : step.route;
      if (this.currentPageKey() !== target) {
        await this.router.navigate([target]);
        await this.delay(250);
      }
    }
    if (step.prepare) {
      await this.runPrepare(step.prepare, step);
    }
    if (step.target && step.skipIfMissingTarget) {
      const el = await this.waitForElement(step.target, 400);
      if (!el) {
        await this.skipMissingStep();
        return;
      }
    }
    await this.layoutStepPosition();
    if (this.shouldSettleTourTarget(step)) {
      this.schedulePositionRefresh(320);
      this.schedulePositionRefresh(720);
    }
  }

  private shouldSettleTourTarget(step: ProductTourStep): boolean {
    const prepare = step.prepare || '';
    if (
      prepare.includes('email-templates-open') ||
      prepare.includes('email-templates-tab-')
    ) {
      return true;
    }
    const target = step.target || '';
    return target.startsWith('email-tpl-');
  }

  private async runPrepare(action: string, step: ProductTourStep): Promise<void> {
    if (!this.prepareNeedsDomUpdate(action)) {
      return;
    }
    window.dispatchEvent(new CustomEvent('velocity-tour-prepare', { detail: { action } }));
    const waitMs = this.prepareWaitMs(action);
    await this.delay(waitMs);
    if (step.target) {
      await this.waitForElement(step.target, 2500);
    }
  }

  private prepareWaitMs(action: string): number {
    if (action === 'roles-open-edit-modal') {
      return 280;
    }
    if (action === 'activitylogs-open-edit-modal') {
      return 700;
    }
    if (action === 'email-templates-open-template-modal') {
      return 520;
    }
    if (action === 'email-templates-tab-templates' || action === 'email-templates-tab-layouts') {
      return 220;
    }
    if (action === 'email-templates-close-modals') {
      return 160;
    }
    return 200;
  }

  private prepareNeedsDomUpdate(action: string): boolean {
    switch (action) {
      case 'roles-close-modals':
        return !!(
          document.querySelector('[data-tour="roles-add-modal"]') ||
          document.querySelector('[data-tour="roles-edit-modal"]')
        );
      case 'roles-open-add-modal':
        return !document.querySelector('[data-tour="roles-add-modal"]');
      case 'roles-open-edit-modal':
        return !document.querySelector('[data-tour="roles-edit-modal"]');
      case 'activitylogs-close-modal':
        return !!document.querySelector('[data-tour="activitylogs-edit-form"]');
      case 'activitylogs-open-edit-modal':
        return !document.querySelector('[data-tour="activitylogs-edit-form"]');
      case 'dept-app-close-csv':
        return !!document.querySelector('[data-tour="dept-app-csv-modal"]');
      case 'dept-app-open-csv':
        return !document.querySelector('[data-tour="dept-app-csv-modal"]');
      case 'template-approved-close-csv':
        return !!document.querySelector('[data-tour="tpl-approved-csv-modal"]');
      case 'template-approved-open-csv':
        return !document.querySelector('[data-tour="tpl-approved-csv-modal"]');
      case 'template-pending-close-csv':
        return !!document.querySelector('[data-tour="tpl-pending-csv-modal"]');
      case 'template-pending-open-csv':
        return !document.querySelector('[data-tour="tpl-pending-csv-modal"]');
      case 'template-builder-close-modals':
        return true;
      case 'template-builder-open-field-modal':
        return !document.querySelector('[data-tour="builder-field-modal"]');
      case 'my-app-close-detail':
        return !!document.querySelector('[data-tour="my-app-detail-summary"]');
      case 'delegation-close-modal':
        return !!document.querySelector('[data-tour="delegation-modal"]');
      case 'delegation-open-add-modal':
        return !document.querySelector('[data-tour="delegation-modal"]');
      case 'email-templates-close-modals':
        return (
          this.isTourTargetVisible('email-tpl-template-modal') ||
          this.isTourTargetVisible('email-tpl-layout-modal') ||
          this.isTourTargetVisible('email-tpl-preview-modal')
        );
      case 'email-templates-open-template-modal':
        return !this.isTourTargetVisible('email-tpl-template-modal');
      case 'email-templates-tab-templates':
      case 'email-templates-tab-layouts':
        return true;
      case 'wp-close-modals':
        return true;
      case 'wp-tab-pipelines':
        return true;
      case 'wp-tab-thresholds':
        return true;
      default:
        return true;
    }
  }

  private async skipMissingStep(): Promise<void> {
    if (!this.runtime) {
      return;
    }
    if (this.runtime.index >= this.runtime.steps.length - 1) {
      this.end(true);
      return;
    }
    this.runtime.index += 1;
    await this.runStep();
  }

  private schedulePositionRefresh(delayMs = 100): void {
    if (this.positionRefreshTimer) {
      clearTimeout(this.positionRefreshTimer);
    }
    this.positionRefreshTimer = window.setTimeout(() => {
      this.positionRefreshTimer = null;
      void this.layoutStepPosition();
    }, delayMs);
  }

  private async layoutStepPosition(): Promise<void> {
    if (!this.runtime) {
      return;
    }
    const step = this.runtime.steps[this.runtime.index];
    const stepCount = this.runtime.steps.length;
    const stepIndex = this.runtime.index;
    const progressPct = ((stepIndex + 1) / stepCount) * 100;

    this.layoutInProgress = true;
    this.patchUi({ suppressSpotlightTransition: true });

    let spotlight: ProductTourUiState['spotlight'] = null;
    let popover: ProductTourUiState['popover'] = {
      left: window.innerWidth / 2,
      top: window.innerHeight / 2,
      transform: 'translate(-50%, -50%)',
    };
    let hasTarget = false;

    if (step.target) {
      const el = (await this.waitForElement(step.target, 200)) as HTMLElement | null;
      if (el) {
        hasTarget = true;
        const inModal = !!el.closest(
          '.rm-modal, .rm-backdrop, .modal, .modal-dialog, .activity-log-modal, .log-csv-modal-backdrop, .log-csv-modal, .builder-modal, .modal-backdrop, [role="dialog"], .modal-wrapper'
        );
        el.scrollIntoView({
          block: inModal ? 'center' : 'nearest',
          inline: 'nearest',
          behavior: inModal ? 'auto' : 'smooth',
        });
        await this.delay(inModal ? 80 : 180);
        await this.waitForNextFrame(2);
        const r = el.getBoundingClientRect();
        const pad = 8;
        spotlight = {
          left: r.left - pad,
          top: r.top - pad,
          width: r.width + pad * 2,
          height: r.height + pad * 2,
        };
        popover = this.placePopoverNear(r);
      }
    }

    this.patchUi({
      step,
      stepIndex,
      stepCount,
      progressPct,
      hasTarget,
      spotlight,
      popover,
      suppressSpotlightTransition: false,
    });

    window.setTimeout(() => {
      this.layoutInProgress = false;
    }, 120);
  }

  private isTourTargetVisible(target: string): boolean {
    const el = document.querySelector(`[data-tour="${target}"]`) as HTMLElement | null;
    if (!el) {
      return false;
    }
    const rect = el.getBoundingClientRect();
    return rect.width > 8 && rect.height > 8 && rect.bottom > 0 && rect.top < window.innerHeight;
  }

  private waitForElement(target: string, timeoutMs: number): Promise<Element | null> {
    return new Promise((resolve) => {
      const existing = document.querySelector(`[data-tour="${target}"]`);
      if (existing) {
        resolve(existing);
        return;
      }
      if (timeoutMs <= 0) {
        resolve(null);
        return;
      }
      const started = performance.now();
      const tick = (): void => {
        const el = document.querySelector(`[data-tour="${target}"]`);
        if (el) {
          resolve(el);
          return;
        }
        if (performance.now() - started >= timeoutMs) {
          resolve(null);
          return;
        }
        requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    });
  }

  private waitForNextFrame(count = 1): Promise<void> {
    return new Promise((resolve) => {
      let remaining = count;
      const step = (): void => {
        remaining -= 1;
        if (remaining <= 0) {
          resolve();
          return;
        }
        requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    });
  }

  private placePopoverNear(rect: DOMRect): ProductTourUiState['popover'] {
    const pw = 360;
    const ph = 220;
    const gap = 16;
    let left = rect.left;
    let top = rect.bottom + gap;

    if (left + pw > window.innerWidth - 16) {
      left = window.innerWidth - pw - 16;
    }
    if (left < 16) {
      left = 16;
    }
    if (top + ph > window.innerHeight - 16) {
      top = rect.top - ph - gap;
    }
    if (top < 16) {
      top = 16;
    }

    return { left, top, transform: 'none' };
  }

  private roleCanSeeTour(role: string, tour: ProductTourDefinition): boolean {
    if (tour.roles.includes('*')) {
      return true;
    }
    if (role === 'ADMIN') {
      return true;
    }
    return tour.roles.some((r) => r === role);
  }

  private currentPageKey(): string {
    let path = this.router.url.split('?')[0] || '';
    path = path.replace(/^\/velocity\/?/i, '/');
    const segments = path.split('/').filter(Boolean);
    if (segments.length === 0) {
      return 'Dashboard';
    }
    if (segments[0] === 'template-fill') {
      return 'template-fill';
    }
    if (segments[0] === 'template-approval') {
      return 'template-approval';
    }
    if (segments[0] === 'my-application') {
      return 'my-application';
    }
    return segments[segments.length - 1];
  }

  private currentRoleName(): string {
    try {
      const raw = localStorage.getItem('user');
      if (!raw) {
        return '';
      }
      const user = JSON.parse(raw);
      const role =
        user?.cfgTblRole?.txtRoleName ||
        user?.cfgTblRole?.txtRoleCode ||
        user?.txtrole ||
        '';
      return String(role).trim().toUpperCase().replace(/\s+/g, '_');
    } catch {
      return '';
    }
  }

  private loadHistory(): Record<string, ProductTourHistoryEntry> {
    try {
      const raw = localStorage.getItem(HISTORY_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }

  private delay(ms: number): Promise<void> {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
  }

  private emptyUi(): ProductTourUiState {
    return {
      active: false,
      launcherOpen: false,
      runtime: null,
      step: null,
      stepIndex: 0,
      stepCount: 0,
      progressPct: 0,
      hasTarget: false,
      suppressSpotlightTransition: false,
      spotlight: null,
      popover: { left: 0, top: 0, transform: 'translate(-50%, -50%)' },
    };
  }

  private patchUi(partial: Partial<ProductTourUiState>): void {
    this.uiSubject.next({ ...this.uiSubject.value, ...partial });
  }
}
