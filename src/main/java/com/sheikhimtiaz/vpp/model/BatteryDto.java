package com.sheikhimtiaz.vpp.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatteryDto {
    @NotBlank(message = "Battery name is required")
    private String name;

    @NotBlank(message = "Postcode is required")
    @Pattern(regexp = "^\\d{4}$", message = "Postcode must be exactly 4 digits")
    private String postcode;

    @Min(value = 0, message = "Capacity must be non negative!")
    private int capacity;

}

