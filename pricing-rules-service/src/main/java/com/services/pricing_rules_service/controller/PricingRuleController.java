package com.services.pricing_rules_service.controller;

import com.services.pricing_rules_service.dto.CalculatePriceRequest;
import com.services.pricing_rules_service.dto.CalculatePriceResponse;
import com.services.pricing_rules_service.dto.CreatePricingRuleRequest;
import com.services.pricing_rules_service.dto.PricingRuleResponse;
import com.services.pricing_rules_service.dto.UpdatePricingRuleRequest;
import com.services.pricing_rules_service.service.PricingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for pricing rule management and price calculations.
 *
 * <p>This controller exposes a RESTful API for managing pricing rules and calculating
 * rental costs in the car sharing application. It provides CRUD operations for admin users
 * and a public calculation endpoint for all services.</p>
 *
 * <p><strong>Endpoints Overview:</strong></p>
 * <table border="1" cellpadding="5">
 *   <thead>
 *     <tr>
 *       <th>Method</th>
 *       <th>Path</th>
 *       <th>Description</th>
 *       <th>Auth Required</th>
 *       <th>Roles</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>POST</td>
 *       <td>/v1/pricing/rules</td>
 *       <td>Create new pricing rule</td>
 *       <td>Yes</td>
 *       <td>ADMIN, MANAGER</td>
 *     </tr>
 *     <tr>
 *       <td>PUT</td>
 *       <td>/v1/pricing/rules/{id}</td>
 *       <td>Update existing pricing rule</td>
 *       <td>Yes</td>
 *       <td>ADMIN, MANAGER</td>
 *     </tr>
 *     <tr>
 *       <td>DELETE</td>
 *       <td>/v1/pricing/rules/{id}</td>
 *       <td>Delete pricing rule</td>
 *       <td>Yes</td>
 *       <td>ADMIN</td>
 *     </tr>
 *     <tr>
 *       <td>GET</td>
 *       <td>/v1/pricing/rules/{id}</td>
 *       <td>Get pricing rule by ID</td>
 *       <td>Yes</td>
 *       <td>ADMIN, MANAGER</td>
 *     </tr>
 *     <tr>
 *       <td>GET</td>
 *       <td>/v1/pricing/rules</td>
 *       <td>List all pricing rules (paginated)</td>
 *       <td>Yes</td>
 *       <td>ADMIN, MANAGER</td>
 *     </tr>
 *     <tr>
 *       <td>POST</td>
 *       <td>/v1/pricing/calculate</td>
 *       <td>Calculate rental price</td>
 *       <td>Yes</td>
 *       <td>Any authenticated user</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><strong>Security Model:</strong></p>
 * <ul>
 *   <li><strong>Authentication:</strong> OAuth2 JWT token (issued by Keycloak)</li>
 *   <li><strong>Authorization:</strong> Role-based access control (RBAC) via {@code @PreAuthorize}</li>
 *   <li><strong>Roles Hierarchy:</strong>
 *     <ul>
 *       <li><strong>ADMIN:</strong> Full access (create, update, delete, read)</li>
 *       <li><strong>MANAGER:</strong> Read/write access (no delete)</li>
 *       <li><strong>USER:</strong> Calculate-only access</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Error Handling:</strong></p>
 * <ul>
 *   <li>All errors return RFC 7807 {@code ProblemDetail} responses via {@code GlobalExceptionHandler}</li>
 *   <li>Validation errors: 400 Bad Request with field-level error messages</li>
 *   <li>Not found errors: 404 Not Found</li>
 *   <li>Authorization errors: 403 Forbidden</li>
 *   <li>Internal errors: 500 Internal Server Error</li>
 * </ul>
 *
 * <p><strong>API Documentation:</strong></p>
 * <ul>
 *   <li>OpenAPI 3.0 spec available at: {@code http://localhost:8083/v3/api-docs}</li>
 *   <li>Swagger UI available at: {@code http://localhost:8083/swagger-ui.html}</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>
 * # Create a new pricing rule (ADMIN/MANAGER)
 * curl -X POST http://localhost:8083/v1/pricing/rules \
 *   -H "Authorization: Bearer $TOKEN" \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "unit": "HOUR",
 *     "vehicleCategory": "STANDARD",
 *     "pricePerUnit": 12.00,
 *     "effectiveFrom": "2025-01-01T00:00:00Z",
 *     "active": true
 *   }'
 *
 * # Calculate rental price (any authenticated user)
 * curl -X POST http://localhost:8083/v1/pricing/calculate \
 *   -H "Authorization: Bearer $TOKEN" \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "vehicleCategory": "STANDARD",
 *     "pickupDatetime": "2025-01-10T10:00:00Z",
 *     "returnDatetime": "2025-01-10T15:30:00Z"
 *   }'
 * </pre>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see PricingRuleService
 * @see CreatePricingRuleRequest
 * @see UpdatePricingRuleRequest
 * @see PricingRuleResponse
 * @see CalculatePriceRequest
 * @see CalculatePriceResponse
 */
@RestController
@RequestMapping("/v1/pricing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pricing Rules", description = "Pricing rule management and rental cost calculation API")
@SecurityRequirement(name = "bearerAuth")
public class PricingRuleController {

    private final PricingRuleService pricingRuleService;

    /**
     * Creates a new pricing rule.
     *
     * <p><strong>Access Control:</strong> ADMIN or MANAGER role required.</p>
     *
     * <p><strong>Validation:</strong></p>
     * <ul>
     *   <li>All required fields must be present (unit, vehicleCategory, pricePerUnit, effectiveFrom)</li>
     *   <li>pricePerUnit must be >= 0</li>
     *   <li>effectiveTo (if set) must be > effectiveFrom</li>
     *   <li>No temporal overlap with existing rules (enforced by DB EXCLUDE constraint)</li>
     * </ul>
     *
     * <p><strong>Example Request:</strong></p>
     * <pre>
     * POST /v1/pricing/rules
     * Content-Type: application/json
     * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * {
     *   "unit": "HOUR",
     *   "vehicleCategory": "STANDARD",
     *   "pricePerUnit": 12.00,
     *   "minDuration": "PT1H",
     *   "maxDuration": "PT24H",
     *   "effectiveFrom": "2025-01-01T00:00:00Z",
     *   "active": true
     * }
     * </pre>
     *
     * <p><strong>Example Response (201 Created):</strong></p>
     * <pre>
     * {
     *   "id": 42,
     *   "unit": "HOUR",
     *   "vehicleCategory": "STANDARD",
     *   "pricePerUnit": 12.00,
     *   "minDuration": "PT1H",
     *   "maxDuration": "PT24H",
     *   "effectiveFrom": "2025-01-01T00:00:00Z",
     *   "active": true,
     *   "createdAt": "2025-01-09T14:30:00Z",
     *   "createdBy": "admin-user-123"
     * }
     * </pre>
     *
     * @param request DTO containing pricing rule details
     * @return Created pricing rule with generated ID and audit fields
     */
    @PostMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Create new pricing rule",
        description = "Creates a new pricing rule for a specific vehicle category and time unit. " +
                      "Requires ADMIN or MANAGER role. Validates temporal uniqueness (no overlapping rules)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Pricing rule created successfully",
            content = @Content(schema = @Schema(implementation = PricingRuleResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (validation failed or temporal overlap detected)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden (insufficient permissions)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<PricingRuleResponse> createRule(@Valid @RequestBody CreatePricingRuleRequest request) {
        log.info("Received request to create pricing rule: vehicleCategory={}, unit={}",
                request.getVehicleCategory(), request.getUnit());
        
        PricingRuleResponse response = pricingRuleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing pricing rule.
     *
     * <p><strong>Access Control:</strong> ADMIN or MANAGER role required.</p>
     *
     * <p><strong>Partial Update (PATCH Semantics):</strong></p>
     * <ul>
     *   <li>Only non-null fields in the request will be updated</li>
     *   <li>Null fields are ignored (existing values preserved)</li>
     *   <li>Immutable fields: {@code id}, {@code effectivePeriod}, {@code createdAt}, {@code createdBy}</li>
     * </ul>
     *
     * <p><strong>Example Request:</strong></p>
     * <pre>
     * PUT /v1/pricing/rules/42
     * Content-Type: application/json
     * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * {
     *   "pricePerUnit": 15.00,
     *   "active": false
     * }
     * </pre>
     *
     * <p><strong>Example Response (200 OK):</strong></p>
     * <pre>
     * {
     *   "id": 42,
     *   "unit": "HOUR",
     *   "vehicleCategory": "STANDARD",
     *   "pricePerUnit": 15.00,  // Updated
     *   "minDuration": "PT1H",
     *   "maxDuration": "PT24H",
     *   "effectiveFrom": "2025-01-01T00:00:00Z",
     *   "active": false,  // Updated
     *   "lastModifiedAt": "2025-01-09T16:00:00Z",
     *   "lastModifiedBy": "manager-user-456"
     * }
     * </pre>
     *
     * @param id      ID of the pricing rule to update
     * @param request DTO containing fields to update (null fields are ignored)
     * @return Updated pricing rule
     */
    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Update existing pricing rule",
        description = "Updates an existing pricing rule (partial update - PATCH semantics). " +
                      "Only non-null fields are updated. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pricing rule updated successfully",
            content = @Content(schema = @Schema(implementation = PricingRuleResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (validation failed)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden (insufficient permissions)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pricing rule not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<PricingRuleResponse> updateRule(
            @Parameter(description = "Pricing rule ID", example = "42")
            @PathVariable Long id,
            @Valid @RequestBody UpdatePricingRuleRequest request) {
        log.info("Received request to update pricing rule id={}", id);
        
        PricingRuleResponse response = pricingRuleService.updateRule(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a pricing rule by ID.
     *
     * <p><strong>Access Control:</strong> ADMIN role required (destructive operation).</p>
     *
     * <p><strong>Hard Delete:</strong></p>
     * <ul>
     *   <li>Permanently removes the rule from the database</li>
     *   <li>Consider soft-delete (setting {@code active=false}) for audit trail preservation</li>
     * </ul>
     *
     * <p><strong>Cache Invalidation:</strong></p>
     * <ul>
     *   <li>Evicts entire "pricingRules" cache to prevent serving deleted rule</li>
     * </ul>
     *
     * <p><strong>Example Request:</strong></p>
     * <pre>
     * DELETE /v1/pricing/rules/42
     * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     * </pre>
     *
     * <p><strong>Example Response (204 No Content):</strong></p>
     * <pre>
     * HTTP/1.1 204 No Content
     * </pre>
     *
     * @param id ID of the pricing rule to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete pricing rule",
        description = "Permanently deletes a pricing rule. Requires ADMIN role (destructive operation)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Pricing rule deleted successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden (ADMIN role required)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pricing rule not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<Void> deleteRule(
            @Parameter(description = "Pricing rule ID", example = "42")
            @PathVariable Long id) {
        log.info("Received request to delete pricing rule id={}", id);
        
        pricingRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a pricing rule by ID.
     *
     * <p><strong>Access Control:</strong> ADMIN or MANAGER role required.</p>
     *
     * <p><strong>Example Request:</strong></p>
     * <pre>
     * GET /v1/pricing/rules/42
     * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     * </pre>
     *
     * <p><strong>Example Response (200 OK):</strong></p>
     * <pre>
     * {
     *   "id": 42,
     *   "unit": "HOUR",
     *   "vehicleCategory": "STANDARD",
     *   "pricePerUnit": 12.00,
     *   "minDuration": "PT1H",
     *   "maxDuration": "PT24H",
     *   "effectiveFrom": "2025-01-01T00:00:00Z",
     *   "active": true,
     *   "createdAt": "2025-01-09T14:30:00Z",
     *   "createdBy": "admin-user-123"
     * }
     * </pre>
     *
     * @param id ID of the pricing rule to retrieve
     * @return Pricing rule details
     */
    @GetMapping("/rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Get pricing rule by ID",
        description = "Retrieves a single pricing rule by its unique identifier. Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pricing rule found",
            content = @Content(schema = @Schema(implementation = PricingRuleResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden (insufficient permissions)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pricing rule not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<PricingRuleResponse> getRuleById(
            @Parameter(description = "Pricing rule ID", example = "42")
            @PathVariable Long id) {
        log.debug("Received request to get pricing rule id={}", id);
        
        PricingRuleResponse response = pricingRuleService.getRuleById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all pricing rules with pagination and sorting.
     *
     * <p><strong>Access Control:</strong> ADMIN or MANAGER role required.</p>
     *
     * <p><strong>Pagination:</strong></p>
     * <ul>
     *   <li>Default page size: 20</li>
     *   <li>Query parameters: {@code page}, {@code size}, {@code sort}</li>
     *   <li>Example: {@code ?page=0&size=10&sort=effectiveFrom,desc}</li>
     * </ul>
     *
     * <p><strong>Sorting:</strong></p>
     * <ul>
     *   <li>Supports sorting by any field (e.g., {@code effectiveFrom}, {@code pricePerUnit})</li>
     *   <li>Direction: {@code asc} (ascending) or {@code desc} (descending)</li>
     *   <li>Multiple sort fields: {@code ?sort=vehicleCategory,asc&sort=unit,asc}</li>
     * </ul>
     *
     * <p><strong>Example Request:</strong></p>
     * <pre>
     * GET /v1/pricing/rules?page=0&size=20&sort=effectiveFrom,desc
     * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     * </pre>
     *
     * <p><strong>Example Response (200 OK):</strong></p>
     * <pre>
     * {
     *   "content": [
     *     {
     *       "id": 42,
     *       "unit": "HOUR",
     *       "vehicleCategory": "STANDARD",
     *       "pricePerUnit": 12.00,
     *       "effectiveFrom": "2025-01-01T00:00:00Z",
     *       "active": true
     *     }
     *   ],
     *   "pageable": {
     *     "pageNumber": 0,
     *     "pageSize": 20,
     *     "sort": {
     *       "sorted": true,
     *       "unsorted": false
     *     }
     *   },
     *   "totalElements": 42,
     *   "totalPages": 3,
     *   "last": false
     * }
     * </pre>
     *
     * @param pageable Pagination and sorting parameters (injected from query params)
     * @return Page of pricing rules with metadata
     */
    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "List all pricing rules",
        description = "Retrieves all pricing rules with pagination and sorting support. " +
                      "Requires ADMIN or MANAGER role."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pricing rules retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden (insufficient permissions)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<Page<PricingRuleResponse>> getAllRules(
            @Parameter(description = "Pagination parameters (page, size, sort)", example = "page=0&size=20&sort=effectiveFrom,desc")
            @PageableDefault(size = 20, sort = "effectiveFrom") Pageable pageable) {
        log.debug("Received request to list all pricing rules: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
        
        Page<PricingRuleResponse> response = pricingRuleService.getAllRules(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Calculates the total rental cost for a given vehicle category and rental period.
     *
     * <p><strong>Access Control:</strong> Any authenticated user (no specific role required).</p>
     *
     * <p><strong>Algorithm:</strong></p>
     * <ol>
     *   <li>Validate request (return &gt; pickup, valid timestamps)</li>
     *   <li>Calculate duration in seconds</li>
     *   <li>Fetch active pricing rules for all units (DAY, HOUR, MINUTE)</li>
     *   <li>Break down duration into optimal units (greedy algorithm)</li>
     *   <li>Calculate cost per unit: quantity × pricePerUnit</li>
     *   <li>Validate total duration against min/max constraints</li>
     *   <li>Return total cost with detailed breakdown</li>
     * </ol>
     *
     * <p><strong>Example Request:</strong></p>
     * <pre>
     * POST /v1/pricing/calculate
     * Content-Type: application/json
     * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * {
     *   "vehicleCategory": "STANDARD",
     *   "pickupDatetime": "2025-01-10T10:00:00Z",
     *   "returnDatetime": "2025-01-10T15:30:00Z"
     * }
     * </pre>
     *
     * <p><strong>Example Response (200 OK):</strong></p>
     * <pre>
     * {
     *   "totalCost": 69.00,
     *   "totalDuration": "PT5H30M",
     *   "vehicleCategory": "STANDARD",
     *   "pickupDatetime": "2025-01-10T10:00:00Z",
     *   "returnDatetime": "2025-01-10T15:30:00Z",
     *   "breakdown": [
     *     {
     *       "unit": "HOUR",
     *       "quantity": 5,
     *       "pricePerUnit": 12.00,
     *       "subtotal": 60.00
     *     },
     *     {
     *       "unit": "MINUTE",
     *       "quantity": 30,
     *       "pricePerUnit": 0.30,
     *       "subtotal": 9.00
     *     }
     *   ],
     *   "calculatedAt": "2025-01-09T14:45:00Z"
     * }
     * </pre>
     *
     * @param request DTO containing vehicle category, pickup timestamp, return timestamp
     * @return Total cost with per-unit breakdown
     */
    @PostMapping("/calculate")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Calculate rental price",
        description = "Calculates the total rental cost for a given vehicle category and rental period. " +
                      "Returns detailed breakdown by pricing units (DAY, HOUR, MINUTE). " +
                      "Available to all authenticated users."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Price calculated successfully",
            content = @Content(schema = @Schema(implementation = CalculatePriceResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (validation failed or duration out of bounds)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized (authentication required)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error (missing pricing rules)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<CalculatePriceResponse> calculatePrice(@Valid @RequestBody CalculatePriceRequest request) {
        log.info("Received request to calculate price: vehicleCategory={}, pickup={}, return={}",
                request.getVehicleCategory(), request.getPickupDatetime(), request.getReturnDatetime());
        
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);
        return ResponseEntity.ok(response);
    }
}
