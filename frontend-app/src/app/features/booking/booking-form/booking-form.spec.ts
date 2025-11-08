import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { DateTime } from 'luxon';

import { BookingForm } from './booking-form';
import { CarService } from '../../../core/services/car';
import { PricingService } from '../../../core/services/pricing';
import { RentalService } from '../../../core/services/rental';

class ActivatedRouteStub {
  readonly queryParamMap = of(convertToParamMap({}));
}

describe('BookingForm', () => {
  let component: BookingForm;
  let fixture: ComponentFixture<BookingForm>;

  beforeEach(async () => {
    const carServiceStub = {
      getCarById: jasmine.createSpy('getCarById').and.returnValue(
        of({ id: 1, brand: 'Test', model: 'Car', category: 'STANDARD' })
      )
    };

    const pricingServiceStub = {
      estimate: jasmine.createSpy('estimate').and.returnValue(
        of({
          totalCost: 100,
          totalDuration: 'PT24H',
          vehicleCategory: 'STANDARD',
          pickupDatetime: DateTime.now().toUTC().toISO() ?? '',
          returnDatetime: DateTime.now().plus({ days: 1 }).toUTC().toISO() ?? '',
          breakdown: [],
          calculatedAt: DateTime.now().toUTC().toISO() ?? ''
        })
      ),
      toLocalDateTime: (isoUtc: string) => DateTime.fromISO(isoUtc, { zone: 'utc' })
    };

    const rentalServiceStub = {
      createRental: jasmine.createSpy('createRental').and.returnValue(
        of({ id: 1, renterId: 'user', carsId: 1, pickupDatetime: '', returnDatetime: '', status: 'PENDING' })
      ),
      toIsoUtc: (date: DateTime) => date.toUTC().toISO({ suppressMilliseconds: true }) ?? '',
      toLocalDateTime: (isoUtc: string) => DateTime.fromISO(isoUtc, { zone: 'utc' })
    };

    await TestBed.configureTestingModule({
      imports: [BookingForm],
      providers: [
        { provide: ActivatedRoute, useClass: ActivatedRouteStub },
        { provide: CarService, useValue: carServiceStub },
        { provide: PricingService, useValue: pricingServiceStub },
        { provide: RentalService, useValue: rentalServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BookingForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
