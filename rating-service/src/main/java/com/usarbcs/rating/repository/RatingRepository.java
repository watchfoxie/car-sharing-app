package com.usarbcs.rating.repository;

import com.usarbcs.rating.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByDriverIdAndCustomerId(String driverId, String customerId);

    Page<Rating> findAllByDriverId(String driverId, Pageable pageable);

    Page<Rating> findAllByCustomerId(String customerId, Pageable pageable);

    Page<Rating> findAllByDriverIdAndCustomerId(String driverId, String customerId, Pageable pageable);

    @Query("select coalesce(avg(r.ratingScore), 0d) from Rating r where r.driverId = :driverId")
    Double findAverageScoreByDriverId(@Param("driverId") String driverId);

    long countByDriverId(String driverId);

    @Query("select r.ratingScore, count(r) from Rating r where r.driverId = :driverId group by r.ratingScore")
    List<Object[]> findDistributionByDriverId(@Param("driverId") String driverId);
}
