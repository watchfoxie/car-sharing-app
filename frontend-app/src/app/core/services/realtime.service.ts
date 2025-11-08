import { Injectable, inject, signal, OnDestroy } from '@angular/core';
import { Observable, Subject, fromEvent } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/**
 * Car availability event from SSE stream.
 */
export interface CarAvailabilityEvent {
  type: 'AVAILABILITY_CHANGED' | 'CAR_CREATED' | 'CAR_UPDATED' | 'CAR_DELETED';
  carId: string;
  status: 'AVAILABLE' | 'RENTED' | 'MAINTENANCE';
  timestamp: string;
}

/**
 * Real-time updates service using Server-Sent Events (SSE).
 * 
 * Features:
 * - Persistent connection to car-service SSE endpoint
 * - Automatic reconnection on connection loss
 * - Event streaming with reactive signals
 * - Graceful connection cleanup on destroy
 * 
 * Usage:
 * ```typescript
 * constructor(private realtimeService: RealtimeService) {}
 * 
 * ngOnInit() {
 *   this.realtimeService.connect();
 *   
 *   this.realtimeService.carEvents$.subscribe(event => {
 *     console.log('Car availability changed:', event);
 *     // Update UI with new car status
 *   });
 * }
 * 
 * ngOnDestroy() {
 *   this.realtimeService.disconnect();
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class RealtimeService implements OnDestroy {
  private eventSource: EventSource | null = null;
  private destroy$ = new Subject<void>();
  
  // Connection state
  private _connected = signal<boolean>(false);
  readonly connected = this._connected.asReadonly();

  // Event stream
  private carEventsSubject = new Subject<CarAvailabilityEvent>();
  readonly carEvents$ = this.carEventsSubject.asObservable();

  // SSE endpoint URL
  private readonly sseUrl = `${environment.apiUrl}/v1/cars/stream`;

  /**
   * Establishes SSE connection to car-service.
   */
  connect(): void {
    if (this.eventSource) {
      console.warn('SSE connection already established');
      return;
    }

    try {
      this.eventSource = new EventSource(this.sseUrl);

      // Connection opened
      this.eventSource.onopen = () => {
        console.log('SSE connection established');
        this._connected.set(true);
      };

      // Receive messages
      this.eventSource.onmessage = (event: MessageEvent) => {
        try {
          const data: CarAvailabilityEvent = JSON.parse(event.data);
          this.carEventsSubject.next(data);
        } catch (error) {
          console.error('Failed to parse SSE event:', error);
        }
      };

      // Handle errors
      this.eventSource.onerror = (error: Event) => {
        console.error('SSE connection error:', error);
        this._connected.set(false);
        
        // EventSource automatically reconnects, but we'll track the state
        if (this.eventSource?.readyState === EventSource.CLOSED) {
          console.log('SSE connection closed');
          this.cleanup();
        }
      };
    } catch (error) {
      console.error('Failed to establish SSE connection:', error);
      this._connected.set(false);
    }
  }

  /**
   * Closes the SSE connection.
   */
  disconnect(): void {
    this.cleanup();
  }

  /**
   * Cleanup resources.
   */
  private cleanup(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this._connected.set(false);
      console.log('SSE connection closed');
    }
  }

  /**
   * Angular lifecycle hook - cleanup on service destroy.
   */
  ngOnDestroy(): void {
    this.cleanup();
    this.destroy$.next();
    this.destroy$.complete();
    this.carEventsSubject.complete();
  }

  /**
   * Gets the current connection status.
   */
  isConnected(): boolean {
    return this._connected();
  }

  /**
   * Gets the underlying EventSource (for advanced usage).
   */
  getEventSource(): EventSource | null {
    return this.eventSource;
  }
}
