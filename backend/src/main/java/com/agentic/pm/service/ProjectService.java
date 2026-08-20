package com.agentic.pm.service;

import com.agentic.pm.domain.Project;
import com.agentic.pm.dto.project.CreateDescriptionUploadUrlRequestDto;
import com.agentic.pm.dto.project.CreateProjectRequestDto;
import com.agentic.pm.dto.project.PresignedUploadResponseDto;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.storage.S3StorageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private static final String DEFAULT_USER_ID = "default-user";

    private final ProjectRepository projectRepository;
    private final S3StorageService s3StorageService;
    private final FileValidationService fileValidationService;

    public ProjectService(ProjectRepository projectRepository,
                          S3StorageService s3StorageService,
                          FileValidationService fileValidationService) {
        this.projectRepository = projectRepository;
        this.s3StorageService = s3StorageService;
        this.fileValidationService = fileValidationService;
    }

    public Project createProject(CreateProjectRequestDto request, String userId) {
        String effectiveUserId = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        Project project = Project.builder()
                .userId(effectiveUserId)
                .name(request.getName())
                .status(Project.ProjectStatus.DRAFT)
                .descriptionS3Key(null)
                .docS3Key(null)
                .jiraProjectKey(null)
                .build();
        return projectRepository.save(project);
    }

    public List<Project> listProjects(String userId) {
        String effectiveUserId = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        return projectRepository.findByUserId(effectiveUserId);
    }

    public Project getProject(String projectId, String userId) {
        return projectRepository.findById(projectId)
                .filter(p -> isOwner(p, userId))
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    public PresignedUploadResponseDto createDescriptionUploadUrl(String projectId, String userId, CreateDescriptionUploadUrlRequestDto request) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> isOwner(p, userId))
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        String fileName = request != null ? request.getFileName() : null;
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("fileName is required");
        }
        fileValidationService.validateFilename(fileName);

        var presigned = s3StorageService.createPresignedPut(projectId, fileName, request.getContentType(), 15);
        projectRepository.updateDescriptionKey(project.getProjectId(), presigned.s3Key());
        return new PresignedUploadResponseDto(presigned.uploadUrl(), presigned.s3Key(), presigned.expiresInSeconds());
    }

    public Project requireClarifiedProject(String projectId, String userId) {
        Project p = getProject(projectId, userId);
        if (p.getStatus() != Project.ProjectStatus.CLARIFIED) {
            throw new IllegalStateException("Project ambiguities not cleared yet (status=" + p.getStatus() + ")");
        }
        return p;
    }

    public void markClarified(String projectId, String userId) {
        getProject(projectId, userId);
        projectRepository.updateStatus(projectId, Project.ProjectStatus.CLARIFIED);
    }

    private static boolean isOwner(Project p, String userId) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        return effective.equals(p.getUserId());
    }
}

