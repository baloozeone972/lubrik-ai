package com.nexusai.media.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.commons.exception.ResourceNotFoundException;
import com.nexusai.media.dto.MediaMetadata;
import com.nexusai.media.dto.MediaUploadResponse;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MinioClient minioClient;

    @Value("${nexusai.storage.minio.bucket:nexusai}")
    private String bucketName;

    @Value("${nexusai.storage.minio.endpoint:http://localhost:9000}")
    private String minioUrl;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of("audio/mpeg", "audio/wav", "audio/ogg", "audio/webm");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    public MediaUploadResponse uploadFile(UUID userId, MultipartFile file, String category) {
        validateFile(file);

        String fileName = generateFileName(userId, file.getOriginalFilename());
        String objectKey = buildObjectKey(userId, category, fileName);

        try {
            // Ensure bucket exists
            ensureBucketExists();

            // Upload file
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String url = getPresignedUrl(objectKey);

            log.info("File uploaded: {} by user {}", objectKey, userId);

            return MediaUploadResponse.builder()
                    .id(UUID.randomUUID())
                    .fileName(file.getOriginalFilename())
                    .fileType(getFileType(file.getContentType()))
                    .fileSize(file.getSize())
                    .url(url)
                    .build();
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new BusinessException("UPLOAD_FAILED", "Failed to upload file: " + e.getMessage());
        }
    }

    public List<MediaUploadResponse> uploadFiles(UUID userId, List<MultipartFile> files, String category) {
        return files.stream()
                .map(file -> uploadFile(userId, file, category))
                .toList();
    }

    public MediaMetadata getMetadata(UUID mediaId, UUID userId) {
        // Would fetch from database
        throw new ResourceNotFoundException("Media", mediaId.toString());
    }

    public FileDownload downloadFile(UUID mediaId, UUID userId) {
        // Would fetch path from database
        String objectKey = "placeholder";

        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());

            return new FileDownload(
                    new InputStreamResource(response),
                    response.headers().get("Content-Type"),
                    "download"
            );
        } catch (Exception e) {
            log.error("Failed to download file", e);
            throw new BusinessException("DOWNLOAD_FAILED", "Failed to download file");
        }
    }

    public Resource getThumbnail(UUID mediaId, UUID userId) {
        // Would generate or fetch thumbnail
        throw new ResourceNotFoundException("Thumbnail", mediaId.toString());
    }

    public void deleteFile(UUID mediaId, UUID userId) {
        // Would fetch path from database and delete
        log.info("File {} deleted by user {}", mediaId, userId);
    }

    public List<MediaMetadata> getUserMedia(UUID userId, String category) {
        // Would fetch from database
        return Collections.emptyList();
    }

    public MediaUploadResponse uploadAvatar(UUID userId, MultipartFile file) {
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException("INVALID_FILE_TYPE", "Avatar must be an image");
        }
        return uploadFile(userId, file, "avatars");
    }

    public MediaUploadResponse uploadCompanionAvatar(UUID userId, UUID companionId, MultipartFile file) {
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException("INVALID_FILE_TYPE", "Avatar must be an image");
        }
        return uploadFile(userId, file, "companions/" + companionId);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "File exceeds maximum size of 50MB");
        }

        String contentType = file.getContentType();
        if (!isAllowedType(contentType)) {
            throw new BusinessException("INVALID_FILE_TYPE", "File type not allowed: " + contentType);
        }
    }

    private boolean isAllowedType(String contentType) {
        return ALLOWED_IMAGE_TYPES.contains(contentType) ||
               ALLOWED_AUDIO_TYPES.contains(contentType) ||
               ALLOWED_VIDEO_TYPES.contains(contentType);
    }

    private String generateFileName(UUID userId, String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    private String buildObjectKey(UUID userId, String category, String fileName) {
        String prefix = category != null ? category : "general";
        return String.format("%s/%s/%s", userId, prefix, fileName);
    }

    private String getFileType(String contentType) {
        if (contentType == null) return "file";
        if (contentType.startsWith("image/")) return "image";
        if (contentType.startsWith("audio/")) return "audio";
        if (contentType.startsWith("video/")) return "video";
        return "file";
    }

    private String getPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            return minioUrl + "/" + bucketName + "/" + objectKey;
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("Created bucket: {}", bucketName);
        }
    }

    public record FileDownload(Resource resource, String contentType, String fileName) {}
}
