package com.services.common.idempotency;

/**
 * Idempotency support for HTTP operations.
 * 
 * <p>Provides constants and guidelines for implementing idempotent operations
 * in the Car Sharing microservices platform.
 * 
 * <h2>What is Idempotency?</h2>
 * <p>An idempotent operation produces the same result when executed multiple times.
 * This is critical for distributed systems where network failures or retries may cause
 * duplicate requests.
 * 
 * <h2>Idempotency-Key Header</h2>
 * <p>Clients should include an {@code Idempotency-Key} header with a unique UUID
 * for non-idempotent operations (POST, PATCH). The server stores the operation result
 * keyed by this ID and returns the cached result if the same key is reused.
 * 
 * <h3>Example Request:</h3>
 * <pre>
 * POST /api/v1/rentals HTTP/1.1
 * Host: localhost:8080
 * Authorization: Bearer {jwt-token}
 * Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 * Content-Type: application/json
 * 
 * {
 *   "carId": 123,
 *   "startDate": "2025-11-10T10:00:00Z",
 *   "endDate": "2025-11-15T10:00:00Z"
 * }
 * </pre>
 * 
 * <h3>Example Response (First Request):</h3>
 * <pre>
 * HTTP/1.1 201 Created
 * Location: /api/v1/rentals/456
 * Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 * Content-Type: application/json
 * 
 * {
 *   "id": 456,
 *   "carId": 123,
 *   "status": "PENDING",
 *   "startDate": "2025-11-10T10:00:00Z",
 *   "endDate": "2025-11-15T10:00:00Z"
 * }
 * </pre>
 * 
 * <h3>Example Response (Duplicate Request with Same Key):</h3>
 * <pre>
 * HTTP/1.1 200 OK
 * Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 * Content-Type: application/json
 * 
 * {
 *   "id": 456,
 *   "carId": 123,
 *   "status": "CONFIRMED",
 *   "startDate": "2025-11-10T10:00:00Z",
 *   "endDate": "2025-11-15T10:00:00Z"
 * }
 * </pre>
 * 
 * <h2>HTTP Method Idempotency</h2>
 * <table border="1">
 *   <tr>
 *     <th>Method</th>
 *     <th>Naturally Idempotent?</th>
 *     <th>Requires Idempotency-Key?</th>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>Yes (read-only)</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>PUT</td>
 *     <td>Yes (full replacement)</td>
 *     <td>No (but recommended for tracking)</td>
 *   </tr>
 *   <tr>
 *     <td>DELETE</td>
 *     <td>Yes (repeated deletes succeed)</td>
 *     <td>No (but recommended for tracking)</td>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>No (creates new resource)</td>
 *     <td><strong>Yes</strong></td>
 *   </tr>
 *   <tr>
 *     <td>PATCH</td>
 *     <td>No (partial update)</td>
 *     <td><strong>Yes</strong></td>
 *   </tr>
 * </table>
 * 
 * <h2>Implementation Guidelines</h2>
 * <h3>1. Storage</h3>
 * <p>Store idempotency keys with operation results in Redis with TTL (24-72 hours).
 * 
 * <pre>
 * Key: idempotency:{service-name}:{idempotency-key}
 * Value: {
 *   "status": "COMPLETED",
 *   "statusCode": 201,
 *   "body": {...},
 *   "timestamp": "2025-11-05T12:00:00Z"
 * }
 * TTL: 86400 seconds (24 hours)
 * </pre>
 * 
 * <h3>2. Processing Flow</h3>
 * <ol>
 *   <li>Extract Idempotency-Key from header (validate UUID format)</li>
 *   <li>Check if key exists in Redis cache</li>
 *   <li>If found: Return cached response (200 OK or original status)</li>
 *   <li>If not found: Process operation, store result with key, return response</li>
 * </ol>
 * 
 * <h3>3. Conflict Detection</h3>
 * <p>If a key is reused for a different operation (different request body),
 * return 409 Conflict with details:
 * 
 * <pre>
 * HTTP/1.1 409 Conflict
 * Content-Type: application/problem+json
 * 
 * {
 *   "type": "https://carsharing.example.com/problems/idempotency-conflict",
 *   "title": "Idempotency Key Conflict",
 *   "status": 409,
 *   "detail": "This idempotency key was already used for a different operation",
 *   "instance": "/api/v1/rentals",
 *   "timestamp": "2025-11-05T12:00:00Z"
 * }
 * </pre>
 * 
 * <h3>4. Spring Implementation Example</h3>
 * <pre>
 * {@literal @}PostMapping("/rentals")
 * public ResponseEntity&lt;RentalDto&gt; createRental(
 *         {@literal @}RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
 *         {@literal @}Valid {@literal @}RequestBody CreateRentalRequest request) {
 *     
 *     if (idempotencyKey != null) {
 *         // Check cache
 *         Optional&lt;CachedResponse&gt; cached = idempotencyService.get(idempotencyKey);
 *         if (cached.isPresent()) {
 *             return ResponseEntity.status(cached.get().getStatusCode())
 *                 .body(cached.get().getBody());
 *         }
 *     }
 *     
 *     // Process operation
 *     RentalDto rental = rentalService.createRental(request);
 *     
 *     if (idempotencyKey != null) {
 *         // Store result
 *         idempotencyService.store(idempotencyKey, HttpStatus.CREATED, rental);
 *     }
 *     
 *     return ResponseEntity.status(HttpStatus.CREATED)
 *         .header("Idempotency-Key", idempotencyKey)
 *         .body(rental);
 * }
 * </pre>
 * 
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Generate Idempotency-Key on the client (UUID v4)</li>
 *   <li>Store keys in client-side storage to retry with same key on failure</li>
 *   <li>Use short TTL (24-72 hours) to avoid cache bloat</li>
 *   <li>Log idempotency key in distributed tracing</li>
 *   <li>Return Idempotency-Key in response headers for debugging</li>
 *   <li>For GET requests, use conditional requests (ETag, If-None-Match)</li>
 *   <li>For state transitions, validate FSM transitions are idempotent</li>
 * </ul>
 * 
 * <h2>Angular Client Example</h2>
 * <pre>
 * import { v4 as uuidv4 } from 'uuid';
 * 
 * createRental(rental: CreateRentalRequest): Observable&lt;Rental&gt; {
 *   const idempotencyKey = uuidv4();
 *   const headers = new HttpHeaders({
 *     'Idempotency-Key': idempotencyKey
 *   });
 *   
 *   return this.http.post&lt;Rental&gt;('/api/v1/rentals', rental, { headers })
 *     .pipe(
 *       retry({
 *         count: 3,
 *         delay: 1000,
 *         resetOnSuccess: true
 *       }),
 *       catchError(this.handleError)
 *     );
 * }
 * </pre>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
public final class IdempotencyConstants {

    /**
     * HTTP header name for idempotency key.
     * 
     * <p>Value should be a UUID (format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
     */
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /**
     * Redis key prefix for idempotency cache entries.
     * 
     * <p>Full key format: {@code idempotency:{service-name}:{idempotency-key}}
     */
    public static final String IDEMPOTENCY_CACHE_PREFIX = "idempotency:";

    /**
     * Default TTL for idempotency cache entries (24 hours).
     */
    public static final long IDEMPOTENCY_CACHE_TTL_SECONDS = 86400L;

    /**
     * Extended TTL for critical operations (72 hours).
     */
    public static final long IDEMPOTENCY_CACHE_TTL_EXTENDED_SECONDS = 259200L;

    /**
     * RFC 7807 problem type for idempotency conflicts.
     */
    public static final String IDEMPOTENCY_CONFLICT_TYPE = 
        "https://carsharing.example.com/problems/idempotency-conflict";

    private IdempotencyConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
