import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SalesForm } from './sales-form';

describe('SalesForm', () => {
  let component: SalesForm;
  let fixture: ComponentFixture<SalesForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalesForm]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SalesForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
