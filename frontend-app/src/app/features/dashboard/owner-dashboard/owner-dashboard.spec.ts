import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DateTime } from 'luxon';

import { OwnerDashboard } from './owner-dashboard';
import { FeedbackService } from '../../../core/services/feedback';
import { CarService } from '../../../core/services/car';
import { RentalService } from '../../../core/services/rental';

describe('OwnerDashboard', () => {
  let component: OwnerDashboard;
  let fixture: ComponentFixture<OwnerDashboard>;

  beforeEach(async () => {
    const feedbackStub = {
      getTopCars: jasmine.createSpy('getTopCars').and.returnValue(of([]))
    };

    const carStub = {
      getCarById: jasmine.createSpy('getCarById').and.returnValue(
        of({ id: 1, brand: 'Test', model: 'Car', registrationNumber: 'MD-01-AAA', category: 'STANDARD' })
      )
    };

    const rentalStub = {
      getRentalsForCar: jasmine.createSpy('getRentalsForCar').and.returnValue(
        of({ content: [], number: 0, totalPages: 0, totalElements: 0 })
      ),
      toLocalDateTime: (isoUtc: string) => DateTime.fromISO(isoUtc, { zone: 'utc' })
    };

    await TestBed.configureTestingModule({
      imports: [OwnerDashboard],
      providers: [
        { provide: FeedbackService, useValue: feedbackStub },
        { provide: CarService, useValue: carStub },
        { provide: RentalService, useValue: rentalStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OwnerDashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
