package com.agentic.pm.api.controller;

import com.agentic.pm.api.security.UserIdResolver;
import com.agentic.pm.dto.breakdown.BreakdownResponseDto;
import com.agentic.pm.service.BreakdownService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class BreakdownController {

    private static final Logger log = LoggerFactory.getLogger(BreakdownController.class);

    private final BreakdownService breakdownService;
    private final UserIdResolver userIdResolver;

    public BreakdownController(BreakdownService breakdownService, UserIdResolver userIdResolver) {
        this.breakdownService = breakdownService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/projects/{projectId}/generate-breakdown")
    public ResponseEntity<BreakdownResponseDto> generateBreakdown(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("generate-breakdown request received. projectId={} userId={}", projectId, userId);
        try {
            BreakdownResponseDto out = breakdownService.generateBreakdown(projectId, userId);
            log.info("generate-breakdown succeeded. projectId={} breakdownVersion={}", projectId, out != null ? out.getBreakdownVersion() : null);
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("generate-breakdown failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @GetMapping("/projects/{projectId}/breakdown")
    public ResponseEntity<BreakdownResponseDto> getBreakdown(@PathVariable String projectId) {
        String userId = userIdResolver.resolve();
        log.info("get-breakdown request received. projectId={} userId={}", projectId, userId);
        try {
            BreakdownResponseDto out = breakdownService.getBreakdown(projectId, userId);
            log.info("get-breakdown succeeded. projectId={} breakdownVersion={}", projectId, out != null ? out.getBreakdownVersion() : null);
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("get-breakdown failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }
}

