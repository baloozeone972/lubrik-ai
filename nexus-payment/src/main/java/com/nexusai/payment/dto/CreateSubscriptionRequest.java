package com.nexusai.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank(message = "Price ID is required")
    private String priceId;

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    private String email;
}
