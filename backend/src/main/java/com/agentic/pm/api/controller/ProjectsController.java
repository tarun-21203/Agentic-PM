package com.agentic.pm.api.controller;

import com.agentic.pm.api.security.UserIdResolver;
import com.agentic.pm.dto.project.CreateDescriptionUploadUrlRequestDto;
import com.agentic.pm.dto.project.CreateProjectRequestDto;
import com.agentic.pm.dto.project.PresignedUploadResponseDto;
import com.agentic.pm.dto.project.ProjectResponseDto;
import com.agentic.pm.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProjectsController {

    private static final Logger log = LoggerFactory.getLogger(ProjectsController.class);

    private final ProjectService projectService;
    private final UserIdResolver userIdResolver;

    public ProjectsController(ProjectService projectService, UserIdResolver userIdResolver) {
        this.projectService = projectService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectResponseDto> createProject(
            @RequestBody CreateProjectRequestDto req
    ) {
        String userId = userIdResolver.resolve();
        log.info("create-project request received. userId={}", userId);
        try {
            var created = projectService.createProject(req, userId);
            log.info("create-project succeeded. userId={} projectId={}", userId, created.getProjectId());
            return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponseDto.from(created));
        } catch (RuntimeException e) {
            log.error("create-project failed. userId={}", userId, e);
            throw e;
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponseDto>> listProjects() {
        String userId = userIdResolver.resolve();
        log.info("list-projects request received. userId={}", userId);
        try {
            var out = projectService.listProjects(userId).stream().map(ProjectResponseDto::from).toList();
            log.info("list-projects succeeded. userId={} count={}", userId, out.size());
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("list-projects failed. userId={}", userId, e);
            throw e;
        }
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponseDto> getProject(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("get-project request received. projectId={} userId={}", projectId, userId);
        try {
            return ResponseEntity.ok(ProjectResponseDto.from(projectService.getProject(projectId, userId)));
        } catch (RuntimeException e) {
            log.error("get-project failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @PostMapping("/projects/{projectId}/description-url")
    public ResponseEntity<PresignedUploadResponseDto> createDescriptionUploadUrl(
            @PathVariable String projectId,
            @RequestBody CreateDescriptionUploadUrlRequestDto req
    ) {
        String userId = userIdResolver.resolve();
        log.info("create-description-url request received. projectId={} userId={}", projectId, userId);
        try {
            return ResponseEntity.ok(projectService.createDescriptionUploadUrl(projectId, userId, req));
        } catch (RuntimeException e) {
            log.error("create-description-url failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @PostMapping("/projects/{projectId}/clarify-complete")
    public ResponseEntity<Map<String, String>> clarifyComplete(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("clarify-complete request received. projectId={} userId={}", projectId, userId);
        try {
            projectService.markClarified(projectId, userId);
            return ResponseEntity.ok(Map.of("status", "clarified"));
        } catch (RuntimeException e) {
            log.error("clarify-complete failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }
}

