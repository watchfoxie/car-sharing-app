package com.usarbcs.customer.service.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingCommand {
    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "Rating score from 1 (worst) to 5 (best)", example = "4")
    private Integer ratingScore;

    @NotBlank
    @Schema(description = "Rated driver identifier", example = "d9d648df-8729-4800-829c-165429c7265d")
    private String driverId;

    @NotBlank
    @Schema(description = "Customer identifier who submits the rating", example = "a73c6d75-6f47-4b7d-b1f4-2ea48ed25997")
    private String customerId;
}
