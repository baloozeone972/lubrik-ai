package com.nexusai.conversation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotNull(message = "Companion ID is required")
    private UUID companionId;

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String initialMessage;
}
