import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DateTime } from 'luxon';

import { RenterDashboard } from './renter-dashboard';
import { RentalService } from '../../../core/services/rental';

describe('RenterDashboard', () => {
  let component: RenterDashboard;
  let fixture: ComponentFixture<RenterDashboard>;

  beforeEach(async () => {
    const rentalServiceStub = {
      getMyRentals: jasmine.createSpy('getMyRentals').and.returnValue(
        of({ content: [], number: 0, totalPages: 0, totalElements: 0 })
      ),
      cancelRental: jasmine.createSpy('cancelRental').and.returnValue(of(void 0)),
      toLocalDateTime: (isoUtc: string) => DateTime.fromISO(isoUtc, { zone: 'utc' })
    };

    await TestBed.configureTestingModule({
      imports: [RenterDashboard],
      providers: [{ provide: RentalService, useValue: rentalServiceStub }]
    }).compileComponents();

    fixture = TestBed.createComponent(RenterDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
