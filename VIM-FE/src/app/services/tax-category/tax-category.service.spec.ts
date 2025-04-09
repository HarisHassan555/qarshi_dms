import { TestBed } from '@angular/core/testing';

import { TaxCategoryService } from './tax-category.service';

describe('TaxCategoryService', () => {
  let service: TaxCategoryService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TaxCategoryService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
