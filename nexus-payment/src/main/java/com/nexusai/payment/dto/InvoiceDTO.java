package com.nexusai.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private String id;
    private String number;
    private String status;
    private Long amountDue;
    private Long amountPaid;
    private String currency;
    private String pdfUrl;
    private Long createdAt;
}
