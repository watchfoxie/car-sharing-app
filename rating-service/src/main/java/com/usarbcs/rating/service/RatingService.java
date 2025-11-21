package com.usarbcs.rating.service;

import com.usarbcs.rating.command.RatingCommand;
import com.usarbcs.rating.dto.RatingDto;
import com.usarbcs.rating.payload.RatingSummaryPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RatingService {

    RatingDto saveRating(RatingCommand command);

    RatingDto getRating(UUID ratingId);

    Page<RatingDto> search(String driverId, String customerId, Pageable pageable);

    RatingSummaryPayload summarizeDriver(String driverId);

    void delete(UUID ratingId);
}
