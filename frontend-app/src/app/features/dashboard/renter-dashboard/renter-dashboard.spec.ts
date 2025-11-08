import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RenterDashboard } from './renter-dashboard';

describe('RenterDashboard', () => {
  let component: RenterDashboard;
  let fixture: ComponentFixture<RenterDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RenterDashboard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RenterDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
