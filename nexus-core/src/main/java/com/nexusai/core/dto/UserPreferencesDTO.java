package com.nexusai.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {
    private String language;
    private String theme;
    private Boolean notificationsEnabled;
    private Boolean emailNotifications;
    private Boolean marketingEmails;
    private String defaultModel;
    private Integer contextMessageLimit;
    private Boolean streamResponses;
    private Boolean showTypingIndicator;
}
