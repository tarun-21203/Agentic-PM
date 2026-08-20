package com.agentic.pm.service;

import com.agentic.pm.api.config.RuntimeEnvConfig;
import com.agentic.pm.domain.Project;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.integration.bedrock.BedrockClient;
import com.agentic.pm.repository.AnswerRepository;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.repository.QuestionRepository;
import com.agentic.pm.utils.DescriptionTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private BedrockClient bedrockClient;
    @Mock
    private S3Client s3Client;
    @Mock
    private DescriptionTextExtractor descriptionTextExtractor;
    @Mock
    private RuntimeEnvConfig runtimeEnvConfig;

    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(
                projectRepository,
                questionRepository,
                answerRepository,
                bedrockClient,
                s3Client,
                descriptionTextExtractor,
                runtimeEnvConfig
        );
    }

    @Test
    void saveAnswer_throwsWhenBlank() {
        assertThrows(IllegalStateException.class, () -> questionService.saveAnswer("p1", "q1", "u1", "   "));
    }

    @Test
    void saveAnswer_throwsWhenProjectNotOwned() {
        Project project = Project.builder().projectId("p1").userId("owner").build();
        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

        assertThrows(ProjectNotFoundException.class, () -> questionService.saveAnswer("p1", "q1", "u1", "ok"));
    }

    @Test
    void saveAnswer_persistsTrimmedAnswer() {
        Project project = Project.builder().projectId("p1").userId("u1").build();
        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

        questionService.saveAnswer("p1", "q1", "u1", "  done  ");

        verify(answerRepository).putAnswer("p1", "q1", "done");
    }
}

