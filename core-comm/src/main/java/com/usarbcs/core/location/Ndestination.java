package com.usarbcs.core.location;


import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class Ndestination implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private CurrentLocation currentLocation;
    private GoLocation goLocation;
    private PriceEstimation priceEstimation;
}
