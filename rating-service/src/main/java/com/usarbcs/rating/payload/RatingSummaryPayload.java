package com.usarbcs.rating.payload;

import lombok.Getter;

import java.util.Map;

@Getter
public class RatingSummaryPayload {

    private final String driverId;
    private final double averageScore;
    private final long totalRatings;
    private final Map<Integer, Long> distribution;

    public RatingSummaryPayload(String driverId, double averageScore, long totalRatings, Map<Integer, Long> distribution) {
        this.driverId = driverId;
        this.averageScore = averageScore;
        this.totalRatings = totalRatings;
        this.distribution = Map.copyOf(distribution);
    }
}
