package com.usarbcs.rating.command;

import com.usarbcs.core.exception.ExceptionPayloadFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;
import static com.usarbcs.core.util.Assert.assertNotNull;
import static com.usarbcs.core.util.Assert.isValid;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Payload used to create or refresh a rating")
public class RatingCommand {

    @Schema(description = "Score provided by the customer", example = "5", minimum = "1", maximum = "5")
    @NotNull
    @Min(1)
    @Max(5)
    private Integer ratingScore;

    @Schema(description = "Driver identifier", example = "drv_12345")
    @NotBlank
    private String driverId;

    @Schema(description = "Customer identifier", example = "cust_67890")
    @NotBlank
    private String customerId;

    @Schema(description = "Optional free-text comment", example = "Driver arrived earlier than expected", maxLength = 1000)
    @Size(max = 1000)
    private String comment;

    public void validate() {
        assertNotBlank(driverId);
        assertNotBlank(customerId);
        assertNotNull(ratingScore, ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        isValid(ratingScore, ExceptionPayloadFactory.INVALID_PAYLOAD.get());
    }
}
