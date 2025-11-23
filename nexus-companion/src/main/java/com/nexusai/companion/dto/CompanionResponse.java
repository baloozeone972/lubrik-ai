package com.nexusai.companion.dto;

import com.nexusai.core.enums.CompanionStatus;
import com.nexusai.core.enums.CompanionStyle;
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
public class CompanionResponse {

    private UUID id;
    private String name;
    private String description;
    private CompanionStyle style;
    private CompanionStatus status;
    private String avatarUrl;
    private String personalityTraits;
    private Long totalMessages;
    private Integer likesCount;
    private Boolean isPublic;
    private LocalDateTime createdAt;
}
