package com.nexusai.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationActionDTO {
    private UUID id;
    private UUID moderatorId;
    private String targetType;
    private UUID targetId;
    private String actionType;
    private String reason;
    private String details;
    private Integer durationHours;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
