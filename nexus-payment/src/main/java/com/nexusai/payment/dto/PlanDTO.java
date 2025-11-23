package com.nexusai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {
    private String id;
    private String name;
    private String description;
    private Integer priceMonthly;
    private Integer priceYearly;
    private String stripePriceId;
    private List<String> features;
    private Boolean popular;
}
