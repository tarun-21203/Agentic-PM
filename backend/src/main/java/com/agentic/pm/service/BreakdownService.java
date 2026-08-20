package com.agentic.pm.service;

import com.agentic.pm.domain.Breakdown;
import com.agentic.pm.domain.Project;
import com.agentic.pm.dto.breakdown.BreakdownResponseDto;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.integration.bedrock.BedrockClient;
import com.agentic.pm.repository.BreakdownRepository;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.api.config.RuntimeEnvConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BreakdownService {

    private static final String DEFAULT_USER_ID = "default-user";
    private static final Logger log = LoggerFactory.getLogger(BreakdownService.class);

    private final ProjectRepository projectRepository;
    private final DocumentationService documentationService;
    private final BreakdownRepository breakdownRepository;
    private final BedrockClient bedrockClient;
    private final ProjectService projectService;
    private final S3Client s3Client;
    private final RuntimeEnvConfig runtimeEnvConfig;

    public BreakdownService(ProjectRepository projectRepository,
                            DocumentationService documentationService,
                            BreakdownRepository breakdownRepository,
                            BedrockClient bedrockClient,
                            ProjectService projectService,
                            S3Client s3Client,
                            RuntimeEnvConfig runtimeEnvConfig) {
        this.projectRepository = projectRepository;
        this.documentationService = documentationService;
        this.breakdownRepository = breakdownRepository;
        this.bedrockClient = bedrockClient;
        this.projectService = projectService;
        this.s3Client = s3Client;
        this.runtimeEnvConfig = runtimeEnvConfig;
    }

    public BreakdownResponseDto generateBreakdown(String projectId, String userId) {
        Project project = projectService.requireClarifiedProject(projectId, userId);
        // Ensure documentation exists; if not, generate it first
        if (project.getDocS3Key() == null || project.getDocS3Key().isBlank()) {
            try {
                documentationService.generateDocumentation(projectId, userId);
            } catch (RuntimeException e) {
                log.error("Failed generating documentation before breakdown. projectId={} userId={}", projectId, userId, e);
                throw e;
            }
            project = projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
        }
        String markdown;
        try {
            markdown = fetchDocMarkdown(project.getDocS3Key());
        } catch (RuntimeException e) {
            log.error("Failed reading documentation markdown for breakdown. projectId={} userId={} docS3Key={}",
                    projectId, userId, project.getDocS3Key(), e);
            throw e;
        }

        List<Breakdown.BreakdownTask> tasks;
        try {
            tasks = bedrockClient.generateBreakdown(project.getName(), markdown);
        } catch (RuntimeException e) {
            log.error(
                    "Bedrock breakdown generation failed. projectId={} userId={} descriptionChars={} ",
                    projectId,
                    userId,
                    markdown != null ? markdown.length() : 0,
                    e
            );
            throw e;
        }

        Breakdown breakdown = Breakdown.builder()
                .projectId(projectId)
                .breakdownVersion("v1")
                .tasks(tasks)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        try {
            breakdownRepository.save(breakdown);
        } catch (RuntimeException e) {
            log.error("Failed persisting breakdown. projectId={} userId={} taskCount={}", projectId, userId,
                    tasks != null ? tasks.size() : 0, e);
            throw e;
        }
        return BreakdownResponseDto.from(breakdown);
    }

    public BreakdownResponseDto getBreakdown(String projectId, String userId) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        Project project = projectRepository.findById(projectId)
                .filter(p -> effective.equals(p.getUserId()))
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Breakdown breakdown = breakdownRepository.getLatest(projectId)
                .orElseThrow(() -> new IllegalStateException("Breakdown has not been generated yet"));
        return BreakdownResponseDto.from(breakdown);
    }

    private String fetchDocMarkdown(String docS3Key) {
        // MVP: use a presigned URL then fetch is not suitable inside lambda; instead use S3 directly.
        // We reuse DocumentationService S3 bucket env and pull the object content via S3 client.
        String bucket = runtimeEnvConfig.bucketName();
        try (var in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(docS3Key)
                .build())) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed fetching documentation markdown from S3. bucket={} key={}", bucket, docS3Key, e);
            throw new IllegalStateException("Failed to read documentation markdown from S3: " + e.getMessage(), e);
        }
    }
}

