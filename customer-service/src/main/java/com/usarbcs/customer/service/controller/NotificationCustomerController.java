package com.usarbcs.customer.service.controller;


import com.usarbcs.customer.service.dto.NotificationCustomerDto;
import com.usarbcs.customer.service.dto.page.NotificationCustomerPage;
import com.usarbcs.customer.service.mapper.NotificationCustomerMapper;
import com.usarbcs.customer.service.model.NotificationCustomer;
import com.usarbcs.customer.service.service.notification.NotificationCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.usarbcs.core.constants.ResourcePath.NOTIFICATION_CUSTOMER;
import static com.usarbcs.core.constants.ResourcePath.V1;

@RestController
@RequestMapping(V1 + NOTIFICATION_CUSTOMER)
@RequiredArgsConstructor
@Tag(name = "Customer notifications", description = "Notification history APIs")
public class NotificationCustomerController {

    private final NotificationCustomerService notificationCustomerService;
    private final NotificationCustomerMapper notificationCustomerMapper;
    private static final String VALIDATION_PROBLEM_REF = "#/components/responses/ValidationProblem";
    private static final String INTERNAL_PROBLEM_REF = "#/components/responses/InternalProblem";
        private static final String NOT_FOUND_PROBLEM_REF = "#/components/responses/NotFoundProblem";


    @GetMapping("/{customerId}")
    @Operation(summary = "List notifications", description = "Retrieves paged notifications for the specified customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = NotificationCustomerPage.class))),
                @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
                @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
    })
    public ResponseEntity<Page<NotificationCustomerDto>> getAll(@PathVariable("customerId") final String customerId,
                                                               @ParameterObject final Pageable pageable){
        final Page<NotificationCustomer> notificationCustomers = notificationCustomerService.getNotificationsCustomerById(customerId, pageable);
        return ResponseEntity.ok(notificationCustomers.map(notificationCustomerMapper::toDto));
    }
    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete notifications", description = "Deletes all notifications for the given customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notifications deleted"),
            @ApiResponse(responseCode = "400", ref = VALIDATION_PROBLEM_REF),
            @ApiResponse(responseCode = "404", ref = NOT_FOUND_PROBLEM_REF),
            @ApiResponse(responseCode = "500", ref = INTERNAL_PROBLEM_REF)
    })
    public ResponseEntity<Void> emptyNotifications(@PathVariable("customerId") final String customerId){
        notificationCustomerService.deleteAllNotificationByCustomerId(customerId);
        return ResponseEntity.noContent().build();
    }
}
