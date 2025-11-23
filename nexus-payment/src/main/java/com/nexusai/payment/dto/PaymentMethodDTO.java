package com.nexusai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDTO {
    private String id;
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private Boolean isDefault;
}
