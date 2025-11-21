package com.usarbcs.rating.controller;

import com.usarbcs.rating.command.RatingCommand;
import com.usarbcs.rating.dto.RatingDto;
import com.usarbcs.rating.payload.RatingSummaryPayload;
import com.usarbcs.rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import static com.usarbcs.core.constants.ResourcePath.RATINGS;
import static com.usarbcs.core.constants.ResourcePath.V1;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping(V1 + RATINGS)
@RequiredArgsConstructor
@Slf4j
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingDto> create(@RequestBody RatingCommand command) {
        final RatingDto rating = ratingService.saveRating(command);
        final URI location = fromCurrentRequest().path("/{id}").buildAndExpand(rating.getId()).toUri();
        return ResponseEntity.created(location).body(rating);
    }

    @GetMapping("/{ratingId}")
    public ResponseEntity<RatingDto> getById(@PathVariable UUID ratingId) {
        return ResponseEntity.ok(ratingService.getRating(ratingId));
    }

    @GetMapping
    public ResponseEntity<Page<RatingDto>> search(@RequestParam(required = false) String driverId,
                                                  @RequestParam(required = false) String customerId,
                                                  @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ratingService.search(driverId, customerId, pageable));
    }

    @GetMapping("/driver/{driverId}/summary")
    public ResponseEntity<RatingSummaryPayload> driverSummary(@PathVariable String driverId) {
        return ResponseEntity.ok(ratingService.summarizeDriver(driverId));
    }

    @DeleteMapping("/{ratingId}")
    public ResponseEntity<Void> delete(@PathVariable UUID ratingId) {
        ratingService.delete(ratingId);
        return ResponseEntity.noContent().build();
    }
}
