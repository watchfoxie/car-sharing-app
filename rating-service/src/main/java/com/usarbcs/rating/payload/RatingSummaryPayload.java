package com.usarbcs.rating.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Map;

@Getter
@Schema(description = "Aggregated rating metrics for a driver")
public class RatingSummaryPayload {

    @Schema(description = "Driver identifier", example = "drv_12345")
    private final String driverId;

    @Schema(description = "Average score computed across all ratings", example = "4.6")
    private final double averageScore;

    @Schema(description = "Total number of ratings recorded for the driver", example = "27")
    private final long totalRatings;

    @Schema(description = "Distribution of rating counts keyed by score (1-5)", example = "{\"1\":1,\"2\":0,\"3\":2,\"4\":8,\"5\":16}")
    private final Map<Integer, Long> distribution;

    public RatingSummaryPayload(String driverId, double averageScore, long totalRatings, Map<Integer, Long> distribution) {
        this.driverId = driverId;
        this.averageScore = averageScore;
        this.totalRatings = totalRatings;
        this.distribution = Map.copyOf(distribution);
    }
}
