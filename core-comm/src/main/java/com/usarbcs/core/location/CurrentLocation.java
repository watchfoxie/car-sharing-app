package com.usarbcs.core.location;


import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class CurrentLocation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private float latitude;
    private float longitude;
}
