package com.agentic.pm.service;

import com.agentic.pm.domain.Project;
import com.agentic.pm.dto.project.CreateProjectRequestDto;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private FileValidationService fileValidationService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, s3StorageService, fileValidationService);
    }

    @Test
    void createProject_assignsDefaultUserIdWhenMissing() {
        CreateProjectRequestDto req = new CreateProjectRequestDto("My Project");
        Project saved = Project.builder()
                .projectId("p1")
                .userId("default-user")
                .name("My Project")
                .status(Project.ProjectStatus.DRAFT)
                .build();

        when(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class))).thenReturn(saved);

        Project out = projectService.createProject(req, null);

        assertEquals("default-user", out.getUserId());
        assertEquals("My Project", out.getName());
        assertEquals(Project.ProjectStatus.DRAFT, out.getStatus());
    }

    @Test
    void markClarified_updatesStatusWhenUserOwnsProject() {
        Project project = Project.builder()
                .projectId("p1")
                .userId("u1")
                .name("N")
                .status(Project.ProjectStatus.IN_REVIEW)
                .build();

        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

        projectService.markClarified("p1", "u1");

        verify(projectRepository).updateStatus("p1", Project.ProjectStatus.CLARIFIED);
    }

    @Test
    void markClarified_throwsNotFoundWhenUserDoesNotOwnProject() {
        Project project = Project.builder()
                .projectId("p1")
                .userId("owner")
                .name("N")
                .status(Project.ProjectStatus.IN_REVIEW)
                .build();

        when(projectRepository.findById("p1")).thenReturn(Optional.of(project));

        assertThrows(ProjectNotFoundException.class, () -> projectService.markClarified("p1", "different-user"));
    }
}

