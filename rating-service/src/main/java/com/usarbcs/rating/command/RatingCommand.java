package com.usarbcs.rating.command;

import com.usarbcs.core.exception.ExceptionPayloadFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;
import static com.usarbcs.core.util.Assert.assertNotNull;
import static com.usarbcs.core.util.Assert.isValid;

@Getter
@Setter
@NoArgsConstructor
public class RatingCommand {

    private Integer ratingScore;
    private String driverId;
    private String customerId;
    private String comment;

    public void validate() {
        assertNotBlank(driverId);
        assertNotBlank(customerId);
        assertNotNull(ratingScore, ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        isValid(ratingScore, ExceptionPayloadFactory.INVALID_PAYLOAD.get());
    }
}
