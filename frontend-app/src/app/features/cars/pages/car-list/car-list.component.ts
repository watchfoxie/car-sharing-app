import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { debounceTime } from 'rxjs/operators';

import { CarService, Car, CarFilter, CarSort } from '../../services/car.service';
import { AuthService } from '../../../../core/services/auth.service';
import { RealtimeService, CarAvailabilityEvent } from '../../../../core/services/realtime.service';

/**
 * Car Listing Component
 * 
 * Features:
 * - Paginated car grid with Material cards
 * - Filters: brand, category, price range
 * - Sorting: A-Z/Z-A, price ascending/descending
 * - TrackBy for performance optimization
 * - Responsive layout (grid adapts to screen size)
 * - Loading states and error handling
 * - Real-time updates via SSE for car availability changes
 */
@Component({
  selector: 'app-car-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatIconModule,
    MatBadgeModule
  ],
  templateUrl: './car-list.component.html',
  styleUrl: './car-list.component.css'
})
export class CarListComponent implements OnInit, OnDestroy {
  private readonly carService = inject(CarService);
  private readonly authService = inject(AuthService);
  private readonly realtimeService = inject(RealtimeService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // Reactive state with signals
  cars = signal<Car[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  
  // Pagination state
  page = signal<number>(0);
  pageSize = signal<number>(12);
  totalElements = signal<number>(0);
  
  // Filter & sort state
  brands = signal<string[]>([]);
  categories = signal<string[]>([]);
  currentSort = signal<CarSort>({ field: 'brand', direction: 'asc' });

  // Computed properties
  isAuthenticated = this.authService.isAuthenticated;
  isRenter = this.authService.isRenter;
  
  // Filter form
  filterForm!: FormGroup;

  // Sort options for dropdown
  sortOptions = [
    { label: 'Brand (A-Z)', value: { field: 'brand', direction: 'asc' } },
    { label: 'Brand (Z-A)', value: { field: 'brand', direction: 'desc' } },
    { label: 'Price (Low to High)', value: { field: 'dailyRate', direction: 'asc' } },
    { label: 'Price (High to Low)', value: { field: 'dailyRate', direction: 'desc' } },
    { label: 'Year (Newest)', value: { field: 'year', direction: 'desc' } },
    { label: 'Year (Oldest)', value: { field: 'year', direction: 'asc' } }
  ];

  ngOnInit(): void {
    this.initializeFilterForm();
    this.loadFilterOptions();
    this.loadCars();
    this.setupFilterListeners();
    this.setupRealtimeUpdates();
  }

  /**
   * Angular lifecycle hook - cleanup subscriptions.
   */
  ngOnDestroy(): void {
    this.realtimeService.disconnect();
  }

  /**
   * Initializes the reactive filter form.
   */
  private initializeFilterForm(): void {
    this.filterForm = this.fb.group({
      brand: [''],
      category: [''],
      minPrice: [null],
      maxPrice: [null],
      sort: [this.sortOptions[0].value]
    });
  }

  /**
   * Loads available brands and categories for filters.
   */
  private loadFilterOptions(): void {
    this.carService.getBrands().subscribe({
      next: (brands) => this.brands.set(brands),
      error: (err) => console.error('Failed to load brands', err)
    });

    this.carService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (err) => console.error('Failed to load categories', err)
    });
  }

  /**
   * Sets up reactive filter listeners with debouncing.
   */
  private setupFilterListeners(): void {
    this.filterForm.valueChanges
      .pipe(debounceTime(400))
      .subscribe(() => {
        this.page.set(0); // Reset to first page on filter change
        this.loadCars();
      });
  }

  /**
   * Sets up real-time updates via SSE.
   */
  private setupRealtimeUpdates(): void {
    // Connect to SSE stream
    this.realtimeService.connect();

    // Subscribe to car availability events
    this.realtimeService.carEvents$.subscribe((event: CarAvailabilityEvent) => {
      console.log('Car availability event received:', event);
      
      // Update car in the current list if it exists
      const carIndex = this.cars().findIndex(car => car.id === event.carId);
      if (carIndex !== -1) {
        const updatedCars = [...this.cars()];
        updatedCars[carIndex] = { ...updatedCars[carIndex], status: event.status };
        this.cars.set(updatedCars);
      }
    });
  }

  /**
   * Loads cars with current filters, sort, and pagination.
   */
  private loadCars(): void {
    this.loading.set(true);
    this.error.set(null);

    const filter: CarFilter = {
      brand: this.filterForm.value.brand || undefined,
      category: this.filterForm.value.category || undefined,
      minPrice: this.filterForm.value.minPrice || undefined,
      maxPrice: this.filterForm.value.maxPrice || undefined,
      status: 'AVAILABLE' // Only show available cars in public listing
    };

    const sort: CarSort = this.filterForm.value.sort;
    this.currentSort.set(sort);

    this.carService.getCars(this.page(), this.pageSize(), filter, sort).subscribe({
      next: (response) => {
        this.cars.set(response.content);
        this.totalElements.set(response.page.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load cars', err);
        this.error.set('Failed to load cars. Please try again later.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Handles pagination events.
   */
  onPageChange(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadCars();
  }

  /**
   * Clears all filters and resets to defaults.
   */
  clearFilters(): void {
    this.filterForm.reset({
      brand: '',
      category: '',
      minPrice: null,
      maxPrice: null,
      sort: this.sortOptions[0].value
    });
  }

  /**
   * Navigates to car details page.
   */
  viewCarDetails(carId: string): void {
    this.router.navigate(['/cars', carId]);
  }

  /**
   * Navigates to booking page for a specific car (requires authentication).
   */
  bookCar(carId: string): void {
    if (!this.isAuthenticated()) {
      this.authService.login();
      return;
    }
    this.router.navigate(['/booking/new'], { queryParams: { carId } });
  }

  /**
   * TrackBy function for performance optimization in *ngFor.
   */
  trackByCar(index: number, car: Car): string {
    return car.id;
  }
}
