/**
 * Car List Component
 * 
 * Displays a searchable, filterable, and sortable list of available cars.
 * Features:
 * - Filters: brand, category, price range, location, status
 * - Sorting: brand A-Z/Z-A, price low/high, year
 * - Pagination with page size options
 * - TrackBy for performance optimization
 * - Responsive grid layout
 * 
 * @module CarListComponent
 */

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CarService } from '../../../core/services/car.service';
import {
  CarListItem,
  CarCategory,
  CarStatus,
  CarSortOption,
  CarFilterParams,
  PaginationParams
} from '../../../core/models/car.model';

@Component({
  selector: 'app-car-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './car-list.component.html',
  styleUrl: './car-list.component.scss'
})
export class CarListComponent implements OnInit {
  private readonly carService = inject(CarService);
  private readonly router = inject(Router);

  // Signals for reactive state management
  cars = signal<CarListItem[]>([]);
  isLoading = signal<boolean>(false);
  totalElements = signal<number>(0);
  
  // Filter signals
  brandFilter = signal<string>('');
  categoryFilter = signal<CarCategory | ''>('');
  priceMinFilter = signal<number | undefined>(undefined);
  priceMaxFilter = signal<number | undefined>(undefined);
  locationFilter = signal<string>('');
  statusFilter = signal<CarStatus | ''>('');
  
  // Pagination & sorting signals
  currentPage = signal<number>(0);
  pageSize = signal<number>(12);
  sortOption = signal<CarSortOption>(CarSortOption.BRAND_ASC);
  
  // Enums for template access
  readonly CarCategory = CarCategory;
  readonly CarStatus = CarStatus;
  readonly CarSortOption = CarSortOption;
  
  // Dropdown options
  readonly categories = Object.values(CarCategory);
  readonly statuses = Object.values(CarStatus);
  readonly sortOptions = [
    { value: CarSortOption.BRAND_ASC, label: 'Brand A-Z' },
    { value: CarSortOption.BRAND_DESC, label: 'Brand Z-A' },
    { value: CarSortOption.PRICE_ASC, label: 'Price: Low to High' },
    { value: CarSortOption.PRICE_DESC, label: 'Price: High to Low' },
    { value: CarSortOption.YEAR_ASC, label: 'Year: Oldest First' },
    { value: CarSortOption.YEAR_DESC, label: 'Year: Newest First' }
  ];

  ngOnInit(): void {
    this.loadCars();
  }

  /**
   * Load cars with current filters, sorting, and pagination
   */
  loadCars(): void {
    this.isLoading.set(true);
    
    const filters: CarFilterParams = {
      brand: this.brandFilter() || undefined,
      category: this.categoryFilter() || undefined,
      priceMin: this.priceMinFilter(),
      priceMax: this.priceMaxFilter(),
      location: this.locationFilter() || undefined,
      status: this.statusFilter() || undefined
    };
    
    const pagination: PaginationParams = {
      page: this.currentPage(),
      size: this.pageSize(),
      sort: this.sortOption()
    };
    
    this.carService.getCars(filters, pagination).subscribe({
      next: (response) => {
        this.cars.set(response.content);
        this.totalElements.set(response.totalElements);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading cars:', error);
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Apply filters and reset to first page
   */
  applyFilters(): void {
    this.currentPage.set(0);
    this.loadCars();
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.brandFilter.set('');
    this.categoryFilter.set('');
    this.priceMinFilter.set(undefined);
    this.priceMaxFilter.set(undefined);
    this.locationFilter.set('');
    this.statusFilter.set('');
    this.currentPage.set(0);
    this.loadCars();
  }

  /**
   * Handle pagination change
   */
  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadCars();
  }

  /**
   * Handle sort change
   */
  onSortChange(): void {
    this.currentPage.set(0);
    this.loadCars();
  }

  /**
   * Navigate to car details
   */
  viewCarDetails(carId: string): void {
    this.router.navigate(['/cars', carId]);
  }

  /**
   * TrackBy function for *ngFor performance optimization
   */
  trackByCarId(index: number, car: CarListItem): string {
    return car.id;
  }

  /**
   * Get category display label
   */
  getCategoryLabel(category: CarCategory): string {
    return category.charAt(0) + category.slice(1).toLowerCase();
  }

  /**
   * Get status display label with color
   */
  getStatusClass(status: CarStatus): string {
    const statusClasses: Record<CarStatus, string> = {
      [CarStatus.AVAILABLE]: 'status-available',
      [CarStatus.RENTED]: 'status-rented',
      [CarStatus.MAINTENANCE]: 'status-maintenance',
      [CarStatus.UNAVAILABLE]: 'status-unavailable'
    };
    return statusClasses[status] || '';
  }
}
