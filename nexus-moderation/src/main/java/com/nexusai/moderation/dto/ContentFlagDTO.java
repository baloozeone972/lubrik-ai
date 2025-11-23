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
public class ContentFlagDTO {
    private UUID id;
    private UUID reporterId;
    private String contentType;
    private UUID contentId;
    private String flagReason;
    private String flagCategory;
    private String description;
    private String severity;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
