package com.nexusai.companion.dto;

import com.nexusai.core.enums.CompanionStyle;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionUpdateRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private CompanionStyle style;

    private String personalityTraits;

    private String appearanceConfig;

    private String voiceConfig;

    private String systemPrompt;

    private Boolean isPublic;
}
