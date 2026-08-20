package com.agentic.pm.api.controller;

import com.agentic.pm.api.security.UserIdResolver;
import com.agentic.pm.dto.jira.JiraConnectRequestDto;
import com.agentic.pm.dto.jira.JiraStatusResponseDto;
import com.agentic.pm.service.JiraIntegrationService;
import com.agentic.pm.service.JiraTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class JiraController {

    private static final Logger log = LoggerFactory.getLogger(JiraController.class);

    private final JiraIntegrationService jiraIntegrationService;
    private final JiraTicketService jiraTicketService;
    private final UserIdResolver userIdResolver;

    public JiraController(JiraIntegrationService jiraIntegrationService,
                          JiraTicketService jiraTicketService,
                          UserIdResolver userIdResolver) {
        this.jiraIntegrationService = jiraIntegrationService;
        this.jiraTicketService = jiraTicketService;
        this.userIdResolver = userIdResolver;
    }

    @PostMapping("/integrations/jira/connect")
    public ResponseEntity<JiraStatusResponseDto> jiraConnect(
            @RequestBody JiraConnectRequestDto req
    ) {
        String userId = userIdResolver.resolve();
        log.info("jira-connect request received. userId={}", userId);
        try {
            return ResponseEntity.ok(jiraIntegrationService.connect(userId, req));
        } catch (RuntimeException e) {
            log.error("jira-connect failed. userId={}", userId, e);
            throw e;
        }
    }

    @GetMapping("/integrations/jira/status")
    public ResponseEntity<JiraStatusResponseDto> jiraStatus() {
        String userId = userIdResolver.resolve();
        log.info("jira-status request received. userId={}", userId);
        try {
            JiraStatusResponseDto out = jiraIntegrationService.status(userId);
            log.info("jira-status succeeded. userId={} connected={}", userId, out != null && out.isConnected());
            return ResponseEntity.ok(out);
        } catch (RuntimeException e) {
            log.error("jira-status failed. userId={}", userId, e);
            throw e;
        }
    }

    @PostMapping("/projects/{projectId}/create-jira-project")
    public ResponseEntity<Map<String, Object>> createJiraProject(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("create-jira-project request received. projectId={} userId={}", projectId, userId);
        try {
            return ResponseEntity.ok(jiraTicketService.createJiraProject(projectId, userId));
        } catch (RuntimeException e) {
            log.error("create-jira-project failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @PostMapping("/projects/{projectId}/create-jira-tickets")
    public ResponseEntity<Map<String, Object>> createJiraTickets(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("create-jira-tickets request received. projectId={} userId={}", projectId, userId);
        try {
            return ResponseEntity.ok(jiraTicketService.createJiraTickets(projectId, userId));
        } catch (RuntimeException e) {
            log.error("create-jira-tickets failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }

    @PostMapping("/projects/{projectId}/update-jira-tickets")
    public ResponseEntity<Map<String, Object>> updateJiraTickets(
            @PathVariable String projectId
    ) {
        String userId = userIdResolver.resolve();
        log.info("update-jira-tickets request received. projectId={} userId={}", projectId, userId);
        try {
            return ResponseEntity.ok(jiraTicketService.updateJiraTickets(projectId, userId));
        } catch (RuntimeException e) {
            log.error("update-jira-tickets failed. projectId={} userId={}", projectId, userId, e);
            throw e;
        }
    }
}

