package com.usarbcs.rating.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rating resource exposed by the API")
public class RatingDto {

    @Schema(description = "Unique identifier for the rating", example = "4b6d8621-1a4e-4b1a-b06a-0f5b5f65d0a9", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "Identifier of the driver being rated", example = "drv_12345")
    private String driverId;

    @Schema(description = "Identifier of the customer who submitted the rating", example = "cust_67890")
    private String customerId;

    @Schema(description = "Score provided by the customer", example = "4", minimum = "1", maximum = "5")
    private Integer ratingScore;

    @Schema(description = "Optional comment supplied with the rating", example = "Great ride, clean car.")
    private String comment;

    @Schema(description = "Creation timestamp", example = "2024-10-09T11:42:11", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-10-10T08:15:32", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}
