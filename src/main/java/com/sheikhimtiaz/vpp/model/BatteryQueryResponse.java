package com.sheikhimtiaz.vpp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BatteryQueryResponse implements Serializable {
    private List<String> batteryNames;
    private double totalWattCapacity;
    private double averageWattCapacity;
    private long totalBatteries;
    private int page;
    private int size;
}
