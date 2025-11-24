package com.nexusai.media.dto;

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
public class MediaMetadata {
    private UUID id;
    private UUID userId;
    private String fileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String category;
    private String url;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private Integer durationSeconds;
    private LocalDateTime createdAt;
}
