package com.usarbcs.customer.service.dto.page;

import com.usarbcs.customer.service.dto.NotificationCustomerDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NotificationCustomerPage", description = "Paged response containing notification history entries")
public class NotificationCustomerPage extends PageResponse<NotificationCustomerDto> {
    public NotificationCustomerPage() {
        super();
    }
}
