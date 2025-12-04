package com.usarbcs.core.location;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class PriceEstimation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @JsonProperty("currency_code")
    private String currencyCode;

    @JsonProperty("display_name")
    private String displayName;

    private String estimate;

    @JsonProperty("low_estimate")
    private BigDecimal lowEstimate;

    @JsonProperty("high_estimate")
    private BigDecimal highEstimate;

    @JsonProperty("surge_multiplier")
    private Float surgeMultiplier;
    private Integer duration;
    private Float distance;
}
