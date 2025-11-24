import { TestBed } from '@angular/core/testing';

import { PaginationHandler } from './pagination-handler';

describe('PaginationHandler', () => {
  let service: PaginationHandler;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PaginationHandler);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
