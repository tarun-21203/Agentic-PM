package com.agentic.pm.storage;

import com.agentic.pm.api.config.RuntimeEnvConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucketName;

    public S3StorageService(S3Client s3Client, S3Presigner presigner, RuntimeEnvConfig runtimeEnvConfig) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucketName = runtimeEnvConfig.bucketName();
    }

    /**
     * Build S3 key for project description: projects/{projectId}/inputs/{filename}
     */
    public String buildDescriptionKey(String projectId, String originalFilename) {
        String ext = "";
        int i = originalFilename != null ? originalFilename.lastIndexOf('.') : -1;
        if (i > 0 && i < (originalFilename != null ? originalFilename.length() : 0) - 1) {
            ext = originalFilename.substring(i);
        }
        String safeName = originalFilename != null && !originalFilename.isBlank()
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "upload";
        return String.format("projects/%s/inputs/%s_%s%s", projectId, UUID.randomUUID().toString(), safeName, ext);
    }

    /**
     * Create presigned PUT URL for uploading a project description file.
     */
    public PresignedPut createPresignedPut(String projectId, String originalFilename, String contentType, int expiresMinutes) {
        String key = buildDescriptionKey(projectId, originalFilename);
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream")
                .build();
        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiresMinutes))
                .putObjectRequest(putReq)
                .build();
        String url = presigner.presignPutObject(presignReq).url().toString();
        return new PresignedPut(url, key, expiresMinutes * 60);
    }

    public String buildDocumentationKey(String projectId, String projectName) {
        String safeName = projectName != null && !projectName.isBlank()
                ? projectName.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "project";
        return String.format("projects/%s/docs/%s_%s.md", projectId, safeName, UUID.randomUUID().toString());
    }

    public String putMarkdown(String s3Key, String markdown) {
        byte[] bytes = (markdown != null ? markdown : "").getBytes(StandardCharsets.UTF_8);
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType("text/markdown; charset=utf-8")
                        .build(),
                RequestBody.fromBytes(bytes));
        return s3Key;
    }

    public PresignedGet createPresignedGet(String s3Key, int expiresMinutes) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiresMinutes))
                .getObjectRequest(getReq)
                .build();
        String url = presigner.presignGetObject(presignReq).url().toString();
        return new PresignedGet(url, s3Key, expiresMinutes * 60);
    }

    public record PresignedPut(String uploadUrl, String s3Key, int expiresInSeconds) {}

    public record PresignedGet(String downloadUrl, String s3Key, int expiresInSeconds) {}
}
