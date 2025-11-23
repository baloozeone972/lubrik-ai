package com.nexusai.moderation.service;

import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.moderation.dto.ContentFlagDTO;
import com.nexusai.moderation.dto.CreateFlagRequest;
import com.nexusai.moderation.dto.ModerationActionDTO;
import com.nexusai.moderation.entity.ContentFlag;
import com.nexusai.moderation.entity.ModerationAction;
import com.nexusai.moderation.repository.ContentFlagRepository;
import com.nexusai.moderation.repository.ModerationActionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    private final ContentFlagRepository contentFlagRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final IncidentService incidentService;

    @Transactional
    public ContentFlagDTO createFlag(UUID reporterId, CreateFlagRequest request) {
        ContentFlag flag = ContentFlag.builder()
                .reporterId(reporterId)
                .contentType(request.getContentType())
                .contentId(request.getContentId())
                .flagReason(request.getFlagReason())
                .flagCategory(request.getFlagCategory())
                .description(request.getDescription())
                .severity(determineSeverity(request.getFlagCategory()))
                .status("pending")
                .build();

        flag = contentFlagRepository.save(flag);
        log.info("Content flag created: {} for {} {}", flag.getId(), request.getContentType(), request.getContentId());

        // Auto-escalate critical flags
        if ("critical".equals(flag.getSeverity())) {
            incidentService.createIncidentFromFlag(flag);
        }

        return mapFlagToDTO(flag);
    }

    @Transactional(readOnly = true)
    public Page<ContentFlagDTO> getPendingFlags(Pageable pageable) {
        return contentFlagRepository.findByStatus("pending", pageable)
                .map(this::mapFlagToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ContentFlagDTO> getFlagsByStatus(String status, Pageable pageable) {
        return contentFlagRepository.findByStatus(status, pageable)
                .map(this::mapFlagToDTO);
    }

    @Transactional(readOnly = true)
    public ContentFlagDTO getFlag(UUID flagId) {
        ContentFlag flag = contentFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentFlag", flagId.toString()));
        return mapFlagToDTO(flag);
    }

    @Transactional
    public ContentFlagDTO reviewFlag(UUID flagId, UUID moderatorId, String resolution, String notes) {
        ContentFlag flag = contentFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentFlag", flagId.toString()));

        flag.setStatus("reviewed");
        flag.setReviewedBy(moderatorId);
        flag.setReviewedAt(LocalDateTime.now());
        flag.setResolution(resolution);
        flag.setResolutionNotes(notes);

        flag = contentFlagRepository.save(flag);
        log.info("Flag {} reviewed by moderator {}: {}", flagId, moderatorId, resolution);

        return mapFlagToDTO(flag);
    }

    @Transactional
    public ModerationActionDTO takeAction(UUID moderatorId, String targetType, UUID targetId,
                                          String actionType, String reason, Integer durationHours) {
        LocalDateTime expiresAt = null;
        if (durationHours != null && durationHours > 0) {
            expiresAt = LocalDateTime.now().plusHours(durationHours);
        }

        ModerationAction action = ModerationAction.builder()
                .moderatorId(moderatorId)
                .targetType(targetType)
                .targetId(targetId)
                .actionType(actionType)
                .reason(reason)
                .durationHours(durationHours)
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        action = moderationActionRepository.save(action);
        log.info("Moderation action {} taken on {} {} by moderator {}",
                actionType, targetType, targetId, moderatorId);

        // Execute the action
        executeAction(action);

        return mapActionToDTO(action);
    }

    @Transactional
    public void revokeAction(UUID actionId, UUID moderatorId, String reason) {
        ModerationAction action = moderationActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("ModerationAction", actionId.toString()));

        action.setIsActive(false);
        action.setRevokedBy(moderatorId);
        action.setRevokedAt(LocalDateTime.now());
        action.setRevokeReason(reason);

        moderationActionRepository.save(action);
        log.info("Action {} revoked by moderator {}", actionId, moderatorId);

        // Revert the action
        revertAction(action);
    }

    @Transactional(readOnly = true)
    public Page<ModerationActionDTO> getActiveActions(String targetType, Pageable pageable) {
        return moderationActionRepository.findByTargetTypeAndIsActiveTrue(targetType, pageable)
                .map(this::mapActionToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ModerationActionDTO> getActionHistory(UUID targetId, Pageable pageable) {
        return moderationActionRepository.findByTargetIdOrderByCreatedAtDesc(targetId, pageable)
                .map(this::mapActionToDTO);
    }

    @Transactional
    public void processExpiredActions() {
        var expiredActions = moderationActionRepository
                .findByIsActiveTrueAndExpiresAtBefore(LocalDateTime.now());

        for (ModerationAction action : expiredActions) {
            action.setIsActive(false);
            moderationActionRepository.save(action);
            revertAction(action);
            log.info("Action {} expired and reverted", action.getId());
        }
    }

    private void executeAction(ModerationAction action) {
        switch (action.getActionType()) {
            case "warn" -> log.info("Warning issued to {}", action.getTargetId());
            case "mute" -> log.info("User {} muted", action.getTargetId());
            case "ban" -> log.info("User {} banned", action.getTargetId());
            case "delete_content" -> log.info("Content {} deleted", action.getTargetId());
            case "hide_content" -> log.info("Content {} hidden", action.getTargetId());
            default -> log.warn("Unknown action type: {}", action.getActionType());
        }
    }

    private void revertAction(ModerationAction action) {
        switch (action.getActionType()) {
            case "mute" -> log.info("User {} unmuted", action.getTargetId());
            case "ban" -> log.info("User {} unbanned", action.getTargetId());
            case "hide_content" -> log.info("Content {} unhidden", action.getTargetId());
            default -> log.debug("No revert needed for action type: {}", action.getActionType());
        }
    }

    private String determineSeverity(String category) {
        return switch (category.toLowerCase()) {
            case "illegal", "violence", "csam" -> "critical";
            case "harassment", "hate_speech", "threats" -> "high";
            case "spam", "inappropriate", "misinformation" -> "medium";
            default -> "low";
        };
    }

    private ContentFlagDTO mapFlagToDTO(ContentFlag flag) {
        return ContentFlagDTO.builder()
                .id(flag.getId())
                .reporterId(flag.getReporterId())
                .contentType(flag.getContentType())
                .contentId(flag.getContentId())
                .flagReason(flag.getFlagReason())
                .flagCategory(flag.getFlagCategory())
                .description(flag.getDescription())
                .severity(flag.getSeverity())
                .status(flag.getStatus())
                .resolution(flag.getResolution())
                .createdAt(flag.getCreatedAt())
                .reviewedAt(flag.getReviewedAt())
                .build();
    }

    private ModerationActionDTO mapActionToDTO(ModerationAction action) {
        return ModerationActionDTO.builder()
                .id(action.getId())
                .moderatorId(action.getModeratorId())
                .targetType(action.getTargetType())
                .targetId(action.getTargetId())
                .actionType(action.getActionType())
                .reason(action.getReason())
                .details(action.getDetails())
                .durationHours(action.getDurationHours())
                .expiresAt(action.getExpiresAt())
                .isActive(action.getIsActive())
                .createdAt(action.getCreatedAt())
                .build();
    }
}
