package com.usarbcs.customer.service.criteria;
import io.swagger.v3.oas.annotations.media.Schema;


public record CustomerCriteria(@Schema(description = "Filter customers whose first name matches the provided value", example = "Ana") String firstName) {
}
