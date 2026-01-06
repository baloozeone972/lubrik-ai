package com.nexusai.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    private boolean approved;
    private double confidenceScore;
    private Set<String> flaggedCategories;
    private boolean needsHumanReview;
    private String message;
}
