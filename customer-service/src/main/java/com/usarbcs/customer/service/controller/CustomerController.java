package com.usarbcs.customer.service.controller;


import com.usarbcs.customer.service.command.CustomerCommand;
import com.usarbcs.customer.service.command.CustomerInfoUpdateCmd;
import com.usarbcs.customer.service.command.CustomerRequestDriver;
import com.usarbcs.customer.service.command.RatingCommand;
import com.usarbcs.customer.service.criteria.CustomerCriteria;
import com.usarbcs.customer.service.dto.CustomerDto;
import com.usarbcs.customer.service.mapper.CustomerMapper;
import com.usarbcs.customer.service.model.Customer;
import com.usarbcs.customer.service.model.Driver;
import com.usarbcs.customer.service.payload.CustomerDetails;
import com.usarbcs.customer.service.service.customer.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

import static com.usarbcs.core.constants.ResourcePath.*;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping(V1 + CUSTOMERS)
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
@Tag(name = "Customers", description = "Customer management REST APIs")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;
    private static final String VALIDATION_PROBLEM_REF = "#/components/responses/ValidationProblem";
    private static final String BUSINESS_PROBLEM_REF = "#/components/responses/BusinessProblem";
    private static final String INTERNAL_PROBLEM_REF = "#/components/responses/InternalProblem";


        @PostMapping
        @Operation(summary = "Create customer", description = "Registers a new customer profile using the provided payload.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "409", ref = BUSINESS_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<CustomerDto> create(@Valid @RequestBody final CustomerCommand customerCommand){
        final Customer customer = customerService.create(customerCommand);
        final URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(customer.getId()).toUri();
        return ResponseEntity.created(uri).body(customerMapper.toDto(customer));
    }
        @PostMapping(SEND_REQUEST)
        @Operation(summary = "Request driver", description = "Sends a ride request to the driver-service via messaging.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request published",
                content = @Content(mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "409", ref = BUSINESS_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<String> sendRequestToDriver(@Valid @RequestBody final CustomerRequestDriver customerRequestDriver){
        customerService.sendRequestDriver(customerRequestDriver);
        return ResponseEntity.ok("Message send successfully");
    }
        @GetMapping
        @Operation(summary = "List customers", description = "Retrieves customers with pagination support.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers retrieved",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<Page<CustomerDto>> getAll(@ParameterObject final Pageable pageable){
        final Page<Customer> customers = customerService.findAllByDeletedFalse(pageable);
        return ResponseEntity.ok(customers.map(customerMapper::toDto));
    }
        @GetMapping(DRIVER_AVAILABLE)
        @Operation(summary = "Drivers availability", description = "Returns available drivers visible to the customer service.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Drivers retrieved",
                content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = Driver.class)))),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<Set<Driver>> getAllDriversAvailable(){
        return ResponseEntity.ok(customerService.getDriversAvailable());
    }
        @GetMapping("/{customerId}")
        @Operation(summary = "Get customer", description = "Fetches a single customer by identifier.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer retrieved",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "409", ref = BUSINESS_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<CustomerDto> getOne(@PathVariable("customerId") final String customerId){
        final Customer customer = customerService.findById(customerId);
        return ResponseEntity.ok(customerMapper.toDto(customer));
    }
        @PutMapping("/{customerId}")
        @Operation(summary = "Update customer", description = "Applies profile updates for the provided customer id.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Customer updated"),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "409", ref = BUSINESS_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<Void> update(@PathVariable("customerId") final String customerId,
                           @Valid @RequestBody final CustomerInfoUpdateCmd command){
        customerService.updateInfo(command, customerId);
        return ResponseEntity.noContent().build();
    }
        @PostMapping(RATINGS)
        @Operation(summary = "Send rating", description = "Posts customer feedback for a completed ride.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating sent",
                content = @Content(mediaType = "text/plain",
                    schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "409", ref = BUSINESS_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<String> sendRating(@Valid @RequestBody final RatingCommand ratingCommand){
        return ResponseEntity.ok(customerService.sendRating(ratingCommand));
    }
        @GetMapping(CRITERIA)
        @Operation(summary = "Search customers", description = "Filters customers based on flexible criteria.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customers filtered",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CustomerDto.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<Page<CustomerDto>> getAllByCriteria(@ParameterObject final CustomerCriteria customerCriteria,
                                     @ParameterObject final Pageable pageable){
        final Page<Customer> customers = customerService.getAllByCriteria(pageable, customerCriteria);
       return ResponseEntity.ok(customers.map(customerMapper::toDto));
    }
        @GetMapping(CUSTOMER_DETAILS + "/{customerId}")
        @Operation(summary = "Customer details", description = "Aggregates multiple data points for the requested customer.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Details retrieved",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = CustomerDetails.class))),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "409", ref = BUSINESS_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
        })
        public ResponseEntity<CustomerDetails> findCustomerDetailsByCustomerId(@PathVariable("customerId") final String customerId){
        return ResponseEntity.ok(customerService.findCustomerDetailsById(customerId));
    }
}
