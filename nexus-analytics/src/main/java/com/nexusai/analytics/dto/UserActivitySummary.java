package com.nexusai.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivitySummary {
    private UUID userId;
    private LocalDate date;
    private Integer conversationsStarted;
    private Integer messagesSent;
    private Integer messagesReceived;
    private Integer tokensUsed;
    private Integer companionsCreated;
    private Integer timeSpentSeconds;
    private UUID mostUsedCompanionId;
    private String mostUsedCompanionName;
}
