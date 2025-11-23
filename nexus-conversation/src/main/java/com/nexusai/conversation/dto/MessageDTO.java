package com.nexusai.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private UUID id;
    private UUID conversationId;
    private String role;
    private String type;
    private String content;
    private Integer tokensUsed;
    private String mediaUrl;
    private String mediaType;
    private Boolean isEdited;
    private List<AttachmentDTO> attachments;
    private LocalDateTime createdAt;
}
