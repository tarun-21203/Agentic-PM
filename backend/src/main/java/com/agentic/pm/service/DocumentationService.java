package com.agentic.pm.service;

import com.agentic.pm.domain.Project;
import com.agentic.pm.domain.Question;
import com.agentic.pm.dto.documentation.DocumentationResponseDto;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.integration.bedrock.BedrockClient;
import com.agentic.pm.integration.bedrock.QuestionAnswerPair;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.storage.S3StorageService;
import com.agentic.pm.utils.DescriptionTextExtractor;
import com.agentic.pm.api.config.RuntimeEnvConfig;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentationService {

    private static final String DEFAULT_USER_ID = "default-user";
    private static final Logger log = LoggerFactory.getLogger(DocumentationService.class);

    private final ProjectRepository projectRepository;
    private final S3StorageService s3StorageService;
    private final S3Client s3Client;
    private final DescriptionTextExtractor descriptionTextExtractor;
    private final BedrockClient bedrockClient;
    private final QuestionService questionService;
    private final ProjectService projectService;
    private final RuntimeEnvConfig runtimeEnvConfig;

    public DocumentationService(ProjectRepository projectRepository,
                                S3StorageService s3StorageService,
                                S3Client s3Client,
                                DescriptionTextExtractor descriptionTextExtractor,
                                BedrockClient bedrockClient,
                                QuestionService questionService,
                                ProjectService projectService,
                                RuntimeEnvConfig runtimeEnvConfig) {
        this.projectRepository = projectRepository;
        this.s3StorageService = s3StorageService;
        this.s3Client = s3Client;
        this.descriptionTextExtractor = descriptionTextExtractor;
        this.bedrockClient = bedrockClient;
        this.questionService = questionService;
        this.projectService = projectService;
        this.runtimeEnvConfig = runtimeEnvConfig;
    }

    public DocumentationResponseDto generateDocumentation(String projectId, String userId) {
        Project project = projectService.requireClarifiedProject(projectId, userId);
        if (project.getDescriptionS3Key() == null || project.getDescriptionS3Key().isBlank()) {
            throw new IllegalStateException("Project has no description uploaded yet");
        }

        String descriptionText;
        try {
            descriptionText = fetchDescriptionText(project.getDescriptionS3Key());
        } catch (RuntimeException e) {
            log.error(
                    "Failed extracting project description for documentation generation. projectId={} userId={} s3Key={} ",
                    projectId,
                    userId,
                    project.getDescriptionS3Key(),
                    e
            );
            throw e;
        }

        List<Question> questions = questionService.listQuestions(projectId, userId);
        Map<String, String> answersByQuestionId = questionService.getAnswersByProjectId(projectId, userId);
        List<QuestionAnswerPair> clarificationQa = questions.stream()
                .map(q -> new QuestionAnswerPair(
                        q.getText() != null ? q.getText() : "",
                        answersByQuestionId.getOrDefault(q.getQuestionId(), "")))
                .toList();

        String markdown;
        try {
            markdown = bedrockClient.generateTechnicalDocumentation(project.getName(), descriptionText, clarificationQa);
        } catch (RuntimeException e) {
            log.error(
                    "Bedrock technical documentation generation failed. projectId={} userId={} descriptionChars={} clarificationCount={}",
                    projectId,
                    userId,
                    descriptionText != null ? descriptionText.length() : 0,
                    clarificationQa != null ? clarificationQa.size() : 0,
                    e
            );
            throw e;
        }

        String docKey = s3StorageService.buildDocumentationKey(projectId, project.getName());
        try {
            s3StorageService.putMarkdown(docKey, markdown);
            projectRepository.updateDocKey(projectId, docKey);
        } catch (RuntimeException e) {
            log.error(
                    "Failed persisting generated documentation. projectId={} userId={} docKey={}",
                    projectId,
                    userId,
                    docKey,
                    e
            );
            throw e;
        }

        var presigned = s3StorageService.createPresignedGet(docKey, 15);
        return new DocumentationResponseDto(presigned.downloadUrl(), presigned.s3Key(), presigned.expiresInSeconds());
    }

    public DocumentationResponseDto getDocumentation(String projectId, String userId) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        Project project = projectRepository.findById(projectId)
                .filter(p -> effective.equals(p.getUserId()))
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        if (project.getDocS3Key() == null || project.getDocS3Key().isBlank()) {
            throw new IllegalStateException("Documentation has not been generated yet");
        }
        var presigned = s3StorageService.createPresignedGet(project.getDocS3Key(), 15);
        return new DocumentationResponseDto(presigned.downloadUrl(), presigned.s3Key(), presigned.expiresInSeconds());
    }

    private String fetchDescriptionText(String s3Key) {
        String bucket = runtimeEnvConfig.bucketName();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        try (var in = s3Client.getObject(request)) {
            return descriptionTextExtractor.extractText(s3Key, in);
        } catch (Exception e) {
            log.error("Failed fetching/parsing project description from S3. bucket={} key={}", bucket, s3Key, e);
            throw new IllegalStateException("Failed to read description from S3: " + e.getMessage(), e);
        }
    }
}

