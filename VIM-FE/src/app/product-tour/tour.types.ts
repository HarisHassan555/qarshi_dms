export interface ProductTourStep {
  /** Navigate before this step (omit for page-local tours). */
  route?: string;
  /** Matches `[data-tour="..."]` on the page */
  target?: string;
  /** Auto-advance when target element is not in the DOM (e.g. admin-only blocks). */
  skipIfMissingTarget?: boolean;
  /**
   * Dispatches `velocity-tour-prepare` before the step (e.g. open a modal).
   * Page components listen and update the DOM so `target` anchors exist.
   */
  prepare?: string;
  title: string;
  body: string;
}

export interface ProductTourDefinition {
  id: string;
  /**
   * App route segment for this page (e.g. `Dashboard`, `users`).
   * Tour appears in the launcher only while you are on this page.
   */
  page: string;
  /** `*` = all roles; ADMIN still sees every tour on every page. */
  roles: string[];
  name: string;
  icon: string;
  desc: string;
  steps: ProductTourStep[];
}

export interface ProductTourHistoryEntry {
  completed: boolean;
  at: string;
}

export interface ProductTourRuntimeState {
  tourId: string;
  steps: ProductTourStep[];
  index: number;
}
