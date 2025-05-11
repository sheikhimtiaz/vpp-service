package com.sheikhimtiaz.vpp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatteryDto {
    private String name;
    private String postcode;
    private int capacity;

}

