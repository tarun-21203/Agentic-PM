package com.agentic.pm.api.controller;

import com.agentic.pm.api.security.UserIdResolver;
import com.agentic.pm.dto.documentation.DocumentationResponseDto;
import com.agentic.pm.service.DocumentationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class DocumentationController {

    private static final Logger log = LoggerFactory.getLogger(DocumentationController.class);

    private final DocumentationService documentationService;
    private final UserIdResolver userIdResolver;

    public DocumentationController(DocumentationService documentationService, UserIdResolver userIdResolver) {
        this.documentationService = documentationService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/projects/{projectId}/generate-documentation")
    public ResponseEntity<DocumentationResponseDto> generateDocumentation(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("generate-documentation request received. projectId={} userId={}", projectId, userId);
        try {
            DocumentationResponseDto out = documentationService.generateDocumentation(projectId, userId);
            log.info("generate-documentation succeeded. projectId={} docS3Key={}", projectId, out != null ? out.getS3Key() : null);
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("generate-documentation failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @GetMapping("/projects/{projectId}/documentation")
    public ResponseEntity<DocumentationResponseDto> getDocumentation(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("get-documentation request received. projectId={} userId={}", projectId, userId);
        try {
            DocumentationResponseDto out = documentationService.getDocumentation(projectId, userId);
            log.info("get-documentation succeeded. projectId={} docS3Key={}", projectId, out != null ? out.getS3Key() : null);
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("get-documentation failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }
}

