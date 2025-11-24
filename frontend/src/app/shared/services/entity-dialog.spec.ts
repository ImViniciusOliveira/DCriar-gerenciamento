import { TestBed } from '@angular/core/testing';

import { EntityDialog } from './entity-dialog';

describe('EntityDialog', () => {
  let service: EntityDialog;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EntityDialog);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
