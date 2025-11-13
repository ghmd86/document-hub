package com.example.droolspoc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Customer Fact - Input data for Drools rules
 *
 * This represents customer information used in eligibility rules.
 * In a real application, this would be fetched from database reactively.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFact {

    private String customerId;
    private String tier;             // GOLD, PLATINUM, BLACK
    private LocalDate enrollmentDate;
    private Integer creditScore;
    private String state;            // CA, NY, TX, etc.
    private Integer age;
}
