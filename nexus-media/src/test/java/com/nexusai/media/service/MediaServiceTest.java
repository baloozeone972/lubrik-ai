package com.nexusai.media.service;

import com.nexusai.commons.exception.BusinessException;
import com.nexusai.media.dto.MediaUploadResponse;
import io.minio.*;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Tests")
class MediaServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private MediaService mediaService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(mediaService, "bucketName", "nexusai");
        ReflectionTestUtils.setField(mediaService, "minioUrl", "http://localhost:9000");
    }

    @Nested
    @DisplayName("UploadFile Tests")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload image file successfully")
        void shouldUploadImageFileSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", "test image content".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/test.jpg");

            MediaUploadResponse result = mediaService.uploadFile(userId, file, "images");

            assertThat(result).isNotNull();
            assertThat(result.getFileName()).isEqualTo("test.jpg");
            assertThat(result.getFileType()).isEqualTo("image");
            assertThat(result.getFileSize()).isEqualTo(file.getSize());
            assertThat(result.getUrl()).contains("localhost");
        }

        @Test
        @DisplayName("Should create bucket if not exists")
        void shouldCreateBucketIfNotExists() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", "test content".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/test.png");

            mediaService.uploadFile(userId, file, "images");

            verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        }

        @Test
        @DisplayName("Should throw exception for empty file")
        void shouldThrowExceptionForEmptyFile() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> mediaService.uploadFile(userId, file, "images"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("EMPTY_FILE");
        }

        @Test
        @DisplayName("Should throw exception for file exceeding size limit")
        void shouldThrowExceptionForFileTooLarge() {
            byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.jpg", "image/jpeg", largeContent);

            assertThatThrownBy(() -> mediaService.uploadFile(userId, file, "images"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("FILE_TOO_LARGE");
        }

        @Test
        @DisplayName("Should throw exception for invalid file type")
        void shouldThrowExceptionForInvalidFileType() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.exe", "application/x-executable", "content".getBytes());

            assertThatThrownBy(() -> mediaService.uploadFile(userId, file, "images"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("INVALID_FILE_TYPE");
        }

        @Test
        @DisplayName("Should accept video files")
        void shouldAcceptVideoFiles() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.mp4", "video/mp4", "video content".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/test.mp4");

            MediaUploadResponse result = mediaService.uploadFile(userId, file, "videos");

            assertThat(result.getFileType()).isEqualTo("video");
        }

        @Test
        @DisplayName("Should accept audio files")
        void shouldAcceptAudioFiles() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.mp3", "audio/mpeg", "audio content".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/test.mp3");

            MediaUploadResponse result = mediaService.uploadFile(userId, file, "audio");

            assertThat(result.getFileType()).isEqualTo("audio");
        }
    }

    @Nested
    @DisplayName("UploadFiles Tests")
    class UploadFilesTests {

        @Test
        @DisplayName("Should upload multiple files")
        void shouldUploadMultipleFiles() throws Exception {
            MockMultipartFile file1 = new MockMultipartFile(
                    "files", "test1.jpg", "image/jpeg", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile(
                    "files", "test2.png", "image/png", "content2".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/test.jpg");

            List<MediaUploadResponse> results = mediaService.uploadFiles(userId, List.of(file1, file2), "images");

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("UploadAvatar Tests")
    class UploadAvatarTests {

        @Test
        @DisplayName("Should upload avatar successfully")
        void shouldUploadAvatarSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "avatar.jpg", "image/jpeg", "avatar content".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/avatar.jpg");

            MediaUploadResponse result = mediaService.uploadAvatar(userId, file);

            assertThat(result).isNotNull();
            assertThat(result.getFileType()).isEqualTo("image");
        }

        @Test
        @DisplayName("Should reject non-image avatar")
        void shouldRejectNonImageAvatar() {
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "avatar.mp4", "video/mp4", "video content".getBytes());

            assertThatThrownBy(() -> mediaService.uploadAvatar(userId, file))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("INVALID_FILE_TYPE");
        }
    }

    @Nested
    @DisplayName("UploadCompanionAvatar Tests")
    class UploadCompanionAvatarTests {

        @Test
        @DisplayName("Should upload companion avatar")
        void shouldUploadCompanionAvatar() throws Exception {
            UUID companionId = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "companion.png", "image/png", "image content".getBytes());

            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://localhost:9000/nexusai/companion.png");

            MediaUploadResponse result = mediaService.uploadCompanionAvatar(userId, companionId, file);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should reject non-image companion avatar")
        void shouldRejectNonImageCompanionAvatar() {
            UUID companionId = UUID.randomUUID();
            MockMultipartFile file = new MockMultipartFile(
                    "avatar", "audio.mp3", "audio/mpeg", "audio".getBytes());

            assertThatThrownBy(() -> mediaService.uploadCompanionAvatar(userId, companionId, file))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("INVALID_FILE_TYPE");
        }
    }

    @Nested
    @DisplayName("DownloadFile Tests")
    class DownloadFileTests {

        @Test
        @DisplayName("Should throw BusinessException on download failure")
        void shouldThrowBusinessExceptionOnDownloadFailure() throws Exception {
            UUID mediaId = UUID.randomUUID();
            when(minioClient.getObject(any(GetObjectArgs.class)))
                    .thenThrow(new RuntimeException("Connection failed"));

            assertThatThrownBy(() -> mediaService.downloadFile(mediaId, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("DOWNLOAD_FAILED");
        }
    }
}
