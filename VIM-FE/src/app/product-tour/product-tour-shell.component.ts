import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { ProductTourService, ProductTourUiState } from './product-tour.service';
import { ProductTourDefinition } from './tour.types';

@Component({
  selector: 'app-product-tour-shell',
  templateUrl: './product-tour-shell.component.html',
  styleUrls: ['./product-tour-shell.component.css'],
})
export class ProductTourShellComponent implements OnInit, OnDestroy {
  ui: ProductTourUiState = {
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
  tours: ProductTourDefinition[] = [];
  pageLabel = 'this page';
  private sub?: Subscription;

  constructor(private productTour: ProductTourService) {}

  ngOnInit(): void {
    this.sub = this.productTour.ui$.subscribe((ui) => {
      this.ui = ui;
      if (ui.launcherOpen) {
        this.pageLabel = this.productTour.getCurrentPageLabel();
        this.tours = this.productTour.getToursForCurrentUser();
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (!this.ui.active) {
      return;
    }
    if (event.key === 'Escape') {
      this.productTour.skip();
    } else if (event.key === 'ArrowRight' || event.key === 'Enter') {
      event.preventDefault();
      void this.productTour.next();
    } else if (event.key === 'ArrowLeft') {
      event.preventDefault();
      void this.productTour.prev();
    }
  }

  closeLauncher(): void {
    this.productTour.closeLauncher();
  }

  startTour(id: string): void {
    void this.productTour.startTour(id);
  }

  isCompleted(id: string): boolean {
    return this.productTour.isTourCompleted(id);
  }

  next(): void {
    void this.productTour.next();
  }

  prev(): void {
    void this.productTour.prev();
  }

  skip(): void {
    this.productTour.skip();
  }

  get nextLabel(): string {
    if (!this.ui.runtime) {
      return 'Next';
    }
    return this.ui.stepIndex >= this.ui.stepCount - 1 ? 'Finish' : 'Next';
  }
}
