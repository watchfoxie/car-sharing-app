package com.usarbcs.rating.service;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.util.Assert;
import com.usarbcs.rating.command.RatingCommand;
import com.usarbcs.rating.dto.RatingDto;
import com.usarbcs.rating.mapper.RatingMapper;
import com.usarbcs.rating.model.Rating;
import com.usarbcs.rating.payload.RatingSummaryPayload;
import com.usarbcs.rating.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;

    @Override
    public RatingDto saveRating(RatingCommand command) {
        command.validate();
        log.info("Persist rating for driver {} provided by customer {}", command.getDriverId(), command.getCustomerId());
        final Rating rating = ratingRepository.findByDriverIdAndCustomerId(command.getDriverId(), command.getCustomerId())
                .map(current -> {
                    current.refresh(command.getRatingScore(), command.getComment());
                    return current;
                })
                .orElseGet(() -> Rating.create(command));

        final Rating persisted = ratingRepository.save(rating);
        log.debug("Rating {} saved successfully", persisted.getId());
        return ratingMapper.toDto(persisted);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingDto getRating(UUID ratingId) {
        return ratingMapper.toDto(resolveRating(ratingId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RatingDto> search(String driverId, String customerId, Pageable pageable) {
        final Page<Rating> ratings;
        if (StringUtils.hasText(driverId) && StringUtils.hasText(customerId)) {
            ratings = ratingRepository.findAllByDriverIdAndCustomerId(driverId, customerId, pageable);
        } else if (StringUtils.hasText(driverId)) {
            ratings = ratingRepository.findAllByDriverId(driverId, pageable);
        } else if (StringUtils.hasText(customerId)) {
            ratings = ratingRepository.findAllByCustomerId(customerId, pageable);
        } else {
            ratings = ratingRepository.findAll(pageable);
        }
        return ratings.map(ratingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryPayload summarizeDriver(String driverId) {
        Assert.assertNotBlank(driverId);
        log.info("Computing rating summary for driver {}", driverId);
        final Double averageScore = ratingRepository.findAverageScoreByDriverId(driverId);
        final double average = averageScore == null ? 0D : averageScore;
        final long total = ratingRepository.countByDriverId(driverId);
        final Map<Integer, Long> distribution = new TreeMap<>();
        ratingRepository.findDistributionByDriverId(driverId)
            .forEach(tuple -> distribution.put(((Number) tuple[0]).intValue(), ((Number) tuple[1]).longValue()));
        for (int score = 1; score <= 5; score++) {
            distribution.putIfAbsent(score, 0L);
        }
        return new RatingSummaryPayload(driverId, average, total, distribution);
    }

    @Override
    public void delete(UUID ratingId) {
        final Rating rating = resolveRating(ratingId);
        ratingRepository.delete(rating);
        log.info("Rating {} removed", ratingId);
    }

    private Rating resolveRating(UUID ratingId) {
        return ratingRepository.findById(ratingId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.RATING_NOT_FOUND.get()));
    }
}
