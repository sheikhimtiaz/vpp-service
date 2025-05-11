package com.sheikhimtiaz.vpp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "battery")
public class Battery {
    @Id
    private String id;

    private String name;
    private String postcode;
    private int capacity;

    public Battery(String name, String postcode, int capacity) {
        this.capacity = capacity;
        this.name = name;
        this.postcode = postcode;
    }
}
