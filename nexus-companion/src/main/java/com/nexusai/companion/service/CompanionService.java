package com.nexusai.companion.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.companion.dto.CompanionCreateRequest;
import com.nexusai.companion.dto.CompanionResponse;
import com.nexusai.companion.dto.CompanionUpdateRequest;
import com.nexusai.core.entity.Companion;
import com.nexusai.core.entity.User;
import com.nexusai.core.enums.CompanionStatus;
import com.nexusai.core.repository.CompanionRepository;
import com.nexusai.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanionService {

    private final CompanionRepository companionRepository;
    private final UserRepository userRepository;

    @Transactional
    public CompanionResponse createCompanion(UUID userId, CompanionCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check companion limit
        long currentCount = companionRepository.countActiveByUserId(userId);
        int maxCompanions = user.getSubscriptionType().getMaxCompanions();

        if (maxCompanions != -1 && currentCount >= maxCompanions) {
            throw new BusinessException("LIMIT_REACHED",
                    "Maximum companions limit reached for your subscription");
        }

        Companion companion = Companion.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .style(request.getStyle())
                .personalityTraits(request.getPersonalityTraits())
                .appearanceConfig(request.getAppearanceConfig())
                .voiceConfig(request.getVoiceConfig())
                .systemPrompt(request.getSystemPrompt())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        companion = companionRepository.save(companion);
        log.info("Companion created: {} for user: {}", companion.getId(), userId);

        return mapToResponse(companion);
    }

    @Transactional(readOnly = true)
    public CompanionResponse getCompanion(UUID companionId, UUID userId) {
        Companion companion = findCompanionByIdAndUser(companionId, userId);
        return mapToResponse(companion);
    }

    @Transactional(readOnly = true)
    public List<CompanionResponse> getUserCompanions(UUID userId) {
        return companionRepository.findByUserIdAndStatusNot(userId, CompanionStatus.DELETED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CompanionResponse updateCompanion(UUID companionId, UUID userId, CompanionUpdateRequest request) {
        Companion companion = findCompanionByIdAndUser(companionId, userId);

        if (request.getName() != null) companion.setName(request.getName());
        if (request.getDescription() != null) companion.setDescription(request.getDescription());
        if (request.getStyle() != null) companion.setStyle(request.getStyle());
        if (request.getPersonalityTraits() != null) companion.setPersonalityTraits(request.getPersonalityTraits());
        if (request.getAppearanceConfig() != null) companion.setAppearanceConfig(request.getAppearanceConfig());
        if (request.getVoiceConfig() != null) companion.setVoiceConfig(request.getVoiceConfig());
        if (request.getSystemPrompt() != null) companion.setSystemPrompt(request.getSystemPrompt());
        if (request.getIsPublic() != null) companion.setIsPublic(request.getIsPublic());

        companion = companionRepository.save(companion);
        log.info("Companion updated: {}", companionId);

        return mapToResponse(companion);
    }

    @Transactional
    public void deleteCompanion(UUID companionId, UUID userId) {
        Companion companion = findCompanionByIdAndUser(companionId, userId);
        companion.setStatus(CompanionStatus.DELETED);
        companionRepository.save(companion);
        log.info("Companion deleted: {}", companionId);
    }

    @Transactional(readOnly = true)
    public Page<CompanionResponse> getPublicCompanions(Pageable pageable) {
        return companionRepository.findPublicCompanions(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<CompanionResponse> searchPublicCompanions(String query, Pageable pageable) {
        return companionRepository.searchPublicCompanions(query, pageable)
                .map(this::mapToResponse);
    }

    private Companion findCompanionByIdAndUser(UUID companionId, UUID userId) {
        return companionRepository.findById(companionId)
                .filter(c -> c.getUserId().equals(userId) || c.getIsPublic())
                .filter(c -> c.getStatus() != CompanionStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Companion", companionId));
    }

    private CompanionResponse mapToResponse(Companion companion) {
        return CompanionResponse.builder()
                .id(companion.getId())
                .name(companion.getName())
                .description(companion.getDescription())
                .style(companion.getStyle())
                .status(companion.getStatus())
                .avatarUrl(companion.getAvatarUrl())
                .personalityTraits(companion.getPersonalityTraits())
                .totalMessages(companion.getTotalMessages())
                .likesCount(companion.getLikesCount())
                .isPublic(companion.getIsPublic())
                .createdAt(companion.getCreatedAt())
                .build();
    }
}
