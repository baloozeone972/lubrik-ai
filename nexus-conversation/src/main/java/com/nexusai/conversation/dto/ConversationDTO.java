package com.nexusai.conversation.dto;

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
public class ConversationDTO {
    private UUID id;
    private UUID userId;
    private UUID companionId;
    private String companionName;
    private String companionAvatar;
    private String title;
    private String status;
    private Integer messageCount;
    private Long totalTokens;
    private String lastMessage;
    private LocalDateTime lastActivityAt;
    private LocalDateTime createdAt;
}
