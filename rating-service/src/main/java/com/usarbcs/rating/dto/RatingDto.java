package com.usarbcs.rating.dto;

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
public class RatingDto {

    private UUID id;
    private String driverId;
    private String customerId;
    private Integer ratingScore;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
