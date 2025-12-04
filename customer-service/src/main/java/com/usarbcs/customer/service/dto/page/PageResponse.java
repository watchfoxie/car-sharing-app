package com.usarbcs.customer.service.dto.page;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

/**
 * Generic page wrapper used only to document Spring Data responses in OpenAPI.
 */
@Schema(description = "Spring Data page response")
public class PageResponse<T> extends PageImpl<T> {

    public PageResponse() {
        super(Collections.emptyList());
    }

    public PageResponse(List<T> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }

    public PageResponse(List<T> content) {
        super(content);
    }
}
