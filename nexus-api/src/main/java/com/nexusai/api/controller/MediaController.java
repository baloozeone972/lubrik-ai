package com.nexusai.api.controller;

import com.nexusai.auth.security.UserPrincipal;
import com.nexusai.media.dto.MediaUploadResponse;
import com.nexusai.media.dto.MediaMetadata;
import com.nexusai.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "File upload and management endpoints")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file")
    public ResponseEntity<MediaUploadResponse> uploadFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category) {
        MediaUploadResponse response = mediaService.uploadFile(
                principal.getUserId(), file, category);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple files")
    public ResponseEntity<List<MediaUploadResponse>> uploadFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String category) {
        List<MediaUploadResponse> responses = mediaService.uploadFiles(
                principal.getUserId(), files, category);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media metadata")
    public ResponseEntity<MediaMetadata> getMediaMetadata(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID mediaId) {
        MediaMetadata metadata = mediaService.getMetadata(mediaId, principal.getUserId());
        return ResponseEntity.ok(metadata);
    }

    @GetMapping("/{mediaId}/download")
    @Operation(summary = "Download a file")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID mediaId) {
        MediaService.FileDownload download = mediaService.downloadFile(mediaId, principal.getUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.fileName() + "\"")
                .body(download.resource());
    }

    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Get file thumbnail")
    public ResponseEntity<Resource> getThumbnail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID mediaId) {
        Resource thumbnail = mediaService.getThumbnail(mediaId, principal.getUserId());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(thumbnail);
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete a file")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID mediaId) {
        mediaService.deleteFile(mediaId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    @Operation(summary = "Get user's uploaded files")
    public ResponseEntity<List<MediaMetadata>> getUserMedia(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category) {
        List<MediaMetadata> media = mediaService.getUserMedia(principal.getUserId(), category);
        return ResponseEntity.ok(media);
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload user avatar")
    public ResponseEntity<MediaUploadResponse> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        MediaUploadResponse response = mediaService.uploadAvatar(principal.getUserId(), file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/companion/{companionId}/avatar")
    @Operation(summary = "Upload companion avatar")
    public ResponseEntity<MediaUploadResponse> uploadCompanionAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID companionId,
            @RequestParam("file") MultipartFile file) {
        MediaUploadResponse response = mediaService.uploadCompanionAvatar(
                principal.getUserId(), companionId, file);
        return ResponseEntity.ok(response);
    }
}
