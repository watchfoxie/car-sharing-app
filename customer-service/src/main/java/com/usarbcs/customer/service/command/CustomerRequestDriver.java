package com.usarbcs.customer.service.command;

import com.usarbcs.core.location.Ndestination;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.io.Serial;
import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Jacksonized
@Builder
@Schema(description = "Payload used by a customer to request a driver ride")
public class  CustomerRequestDriver implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "Identifier of the requesting customer", example = "0f516c0f-9025-4b33-9f94-b548d0dd1222")
    private String customerId;

    @NotBlank
    @Schema(description = "Identifier of the targeted driver", example = "f92444b9-8f5a-443c-b43d-31471b19735a")
    private String driverId;

    @Valid
    @NotNull
    @Schema(description = "Requested destination coordinates")
    private Ndestination destination;
}
