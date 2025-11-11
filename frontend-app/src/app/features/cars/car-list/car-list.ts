import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BehaviorSubject, combineLatest, of } from 'rxjs';
import { catchError, debounceTime, map, startWith, switchMap, tap } from 'rxjs/operators';

import { Car, CarAvailabilityEvent, CarListFilters, CarService, VehicleCategory } from '../../../core/services/car';
import { Page } from '../../../core/models/api';

@Component({
  selector: 'app-car-list',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './car-list.html',
  styleUrl: './car-list.css',
})
export class CarList {
  private readonly carService = inject(CarService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly categories: VehicleCategory[] = ['ECONOM', 'STANDARD', 'PREMIUM'];
  protected readonly sortOptions = [
    { label: 'Brand A → Z', value: 'brand,asc' },
    { label: 'Brand Z → A', value: 'brand,desc' },
    { label: 'Price Low → High', value: 'dailyPrice,asc' },
    { label: 'Price High → Low', value: 'dailyPrice,desc' }
  ];

  protected readonly filtersForm = new FormGroup({
    brand: new FormControl<string>(''),
    category: new FormControl<VehicleCategory | null>(null),
    minPrice: new FormControl<number | null>(null),
    maxPrice: new FormControl<number | null>(null),
    sort: new FormControl<string>('brand,asc')
  });

  protected readonly cars = signal<Car[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly pageSize = signal(10);
  private readonly pageChanges$ = new BehaviorSubject<{ page: number; size: number }>({ page: 0, size: 10 });

  constructor() {
    this.observeFilters();
    this.observeAvailability();
  }

  protected onPageEvent(event: PageEvent): void {
    this.pageChanges$.next({ page: event.pageIndex, size: event.pageSize });
  }

  protected resetFilters(): void {
    this.filtersForm.reset({
      brand: '',
      category: null,
      minPrice: null,
      maxPrice: null,
      sort: 'brand,asc'
    });
    this.pageChanges$.next({ page: 0, size: this.pageSize() });
  }

  protected trackByCarId(_index: number, car: Car): number {
    return car.id;
  }

  private observeFilters(): void {
    const filters$ = this.filtersForm.valueChanges.pipe(
      startWith(this.filtersForm.getRawValue()),
      debounceTime(250),
      map((value) => ({
        brand: value.brand?.trim() || undefined,
        category: value.category ?? undefined,
        minPrice: value.minPrice ?? undefined,
        maxPrice: value.maxPrice ?? undefined,
        sort: value.sort ?? 'brand,asc'
      } as CarListFilters)),
      tap(() => this.pageChanges$.next({ page: 0, size: this.pageSize() }))
    );

    combineLatest([filters$, this.pageChanges$])
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set(null);
        }),
        switchMap(([filters, pagination]) =>
          this.fetchCars(filters, pagination.page, pagination.size).pipe(
            catchError((error) => {
              console.error('Failed to load cars', error);
              this.errorMessage.set('Unable to load the fleet. Please try again later.');
              return of<Page<Car>>({
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: pagination.page,
                size: pagination.size
              });
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((page) => {
        this.cars.set(page.content);
        this.total.set(page.totalElements);
        this.pageSize.set(page.size);
        this.loading.set(false);
      });
  }

  private observeAvailability(): void {
    this.carService
      .listenToAvailability()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => this.applyAvailabilityEvent(event),
        error: () => {
          // Stream might not be enabled in all environments; degrade silently.
        }
      });
  }

  private applyAvailabilityEvent(event: CarAvailabilityEvent): void {
    this.cars.update((current) =>
      current.map((car) => (car.id === event.carId ? { ...car, shareable: event.shareable, archived: event.archived } : car))
    );
  }

  private fetchCars(filters: CarListFilters, page: number, size: number) {
    return this.carService.listPublicCars({
      ...filters,
      page,
      size
    });
  }

}
