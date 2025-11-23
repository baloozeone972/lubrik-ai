package com.nexusai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String subscriptionId;
    private String status;
    private Long currentPeriodStart;
    private Long currentPeriodEnd;
    private Boolean cancelAtPeriodEnd;
    private String clientSecret;
}
