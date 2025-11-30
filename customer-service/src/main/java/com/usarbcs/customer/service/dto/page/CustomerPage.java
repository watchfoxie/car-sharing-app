package com.usarbcs.customer.service.dto.page;

import com.usarbcs.customer.service.dto.CustomerDto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CustomerPage", description = "Paged response that wraps customer resources")
public class CustomerPage extends PageResponse<CustomerDto> {
    public CustomerPage() {
        super();
    }
}
