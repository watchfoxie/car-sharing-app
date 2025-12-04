package com.usarbcs.rating.model;

import com.usarbcs.rating.command.RatingCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_rating_driver_customer", columnNames = {"driver_id", "customer_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Rating extends BaseEntity {

    @Column(name = "driver_id", nullable = false)
    private String driverId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "rating_score", nullable = false)
    private Integer ratingScore;

    @Column(name = "comment", length = 1000)
    private String comment;

    public static Rating create(RatingCommand command) {
        final Rating rating = new Rating();
        rating.driverId = sanitize(command.getDriverId());
        rating.customerId = sanitize(command.getCustomerId());
        rating.ratingScore = command.getRatingScore();
        rating.comment = normalizeComment(command.getComment());
        return rating;
    }

    public void refresh(Integer ratingScore, String comment) {
        this.ratingScore = ratingScore;
        this.comment = normalizeComment(comment);
    }

    private static String normalizeComment(String comment) {
        return comment == null ? null : comment.trim();
    }

    private static String sanitize(String value) {
        return value == null ? null : value.trim();
    }
}
