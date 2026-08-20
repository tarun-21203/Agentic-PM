package com.agentic.pm.service;

import com.agentic.pm.domain.Project;
import com.agentic.pm.domain.Question;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.integration.bedrock.BedrockClient;
import com.agentic.pm.api.config.RuntimeEnvConfig;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.repository.QuestionRepository;
import com.agentic.pm.repository.AnswerRepository;
import com.agentic.pm.utils.DescriptionTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    private static final String DEFAULT_USER_ID = "default-user";
    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final ProjectRepository projectRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final BedrockClient bedrockClient;
    private final S3Client s3Client;
    private final DescriptionTextExtractor textExtractor;
    private final RuntimeEnvConfig runtimeEnvConfig;

    public QuestionService(ProjectRepository projectRepository,
                           QuestionRepository questionRepository,
                           AnswerRepository answerRepository,
                           BedrockClient bedrockClient,
                           S3Client s3Client,
                           DescriptionTextExtractor textExtractor,
                           RuntimeEnvConfig runtimeEnvConfig) {
        this.projectRepository = projectRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.bedrockClient = bedrockClient;
        this.s3Client = s3Client;
        this.textExtractor = textExtractor;
        this.runtimeEnvConfig = runtimeEnvConfig;
    }

    public List<Question> generateQuestions(String projectId, String userId) {
        Project project = loadOwnedProject(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (project.getDescriptionS3Key() == null || project.getDescriptionS3Key().isBlank()) {
            throw new IllegalStateException("Project has no description uploaded yet");
        }

        String descriptionText;
        try {
            descriptionText = fetchDescriptionText(project.getDescriptionS3Key());
        } catch (RuntimeException e) {
            log.error(
                    "Failed extracting project description for question generation. projectId={} userId={} s3Key={} descriptionChars={}",
                    projectId,
                    userId,
                    project.getDescriptionS3Key(),
                    e.getMessage(),
                    e
            );
            throw e;
        }

        List<String> questionTexts;
        log.info(
                "question generation started. projectId={} userId={} descriptionChars={}",
                projectId,
                userId,
                descriptionText != null ? descriptionText.length() : 0
        );
        try {
            questionTexts = bedrockClient.generateQuestions(descriptionText);
        } catch (RuntimeException e) {
            log.error(
                    "Bedrock question generation failed. projectId={} userId={} descriptionChars={}",
                    projectId,
                    userId,
                    descriptionText != null ? descriptionText.length() : 0,
                    e
            );
            throw e;
        }
        log.info(
                "question generation completed. projectId={} userId={} generatedCount={}",
                projectId,
                userId,
                questionTexts != null ? questionTexts.size() : 0
        );
        List<Question> questions = new ArrayList<>();
        int order = 0;
        for (String text : questionTexts) {
            if (text == null || text.isBlank()) continue;
            questions.add(Question.builder()
                    .projectId(projectId)
                    .text(text.trim())
                    .category(null)
                    .order(order++)
                    .build());
        }
        if (questions.isEmpty()) return List.of();
        try {
            return questionRepository.saveAll(questions);
        } catch (RuntimeException e) {
            log.error(
                    "Failed persisting generated questions. projectId={} userId={} questionCount={}",
                    projectId,
                    userId,
                    questions.size(),
                    e
            );
            throw e;
        }
    }

    public List<Question> listQuestions(String projectId, String userId) {
        loadOwnedProject(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        return questionRepository.findByProjectId(projectId);
    }

    public void saveAnswer(String projectId, String questionId, String userId, String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalStateException("answer is required");
        }
        loadOwnedProject(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        answerRepository.putAnswer(projectId, questionId, answerText.trim());
    }

    public List<String> listQuestionTexts(String projectId, String userId) {
        return listQuestions(projectId, userId).stream()
                .map(q -> q.getText() != null ? q.getText() : "")
                .filter(s -> !s.isBlank())
                .toList();
    }

    public java.util.Map<String, String> getAnswersByProjectId(String projectId, String userId) {
        loadOwnedProject(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        return answerRepository.getAnswersByProjectId(projectId);
    }

    private Optional<Project> loadOwnedProject(String projectId, String userId) {
        String effectiveUserId = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        return projectRepository.findById(projectId)
                .filter(p -> effectiveUserId.equals(p.getUserId()));
    }

    private String fetchDescriptionText(String s3Key) {
        String bucket = runtimeEnvConfig.bucketName();
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();
        try (var in = s3Client.getObject(request)) {
            return textExtractor.extractText(s3Key, in);
        } catch (Exception e) {
            log.error("Failed fetching/parsing description from S3. bucket={} key={}", bucket, s3Key, e);
            throw new IllegalStateException("Failed to read description from S3: " + e.getMessage(), e);
        }
    }
}

