package com.agentic.pm.service;

import com.agentic.pm.domain.JiraIntegration;
import com.agentic.pm.dto.jira.JiraConnectRequestDto;
import com.agentic.pm.dto.jira.JiraStatusResponseDto;
import com.agentic.pm.integration.jira.JiraClient;
import com.agentic.pm.repository.JiraIntegrationRepository;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

import java.time.Instant;
import java.util.Optional;

@Service
public class JiraIntegrationService {

    private static final String DEFAULT_USER_ID = "default-user";

    private final JiraIntegrationRepository repo;
    private final SecretsManagerClient secretsManager;

    public JiraIntegrationService(JiraIntegrationRepository repo, SecretsManagerClient secretsManager) {
        this.repo = repo;
        this.secretsManager = secretsManager;
    }

    public JiraStatusResponseDto connect(String userId, JiraConnectRequestDto request) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        if (request == null || isBlank(request.getJiraSite()) || isBlank(request.getJiraEmail()) || isBlank(request.getApiToken())) {
            throw new IllegalStateException("jiraSite, jiraEmail, and apiToken are required");
        }

        // validate credentials quickly
        JiraClient client = new JiraClient(request.getJiraSite(), request.getJiraEmail(), request.getApiToken());
        client.getMyself();

        Optional<JiraIntegration> existing = repo.getByUserId(effective);
        String secretId;
        if (existing.isPresent()) {
            secretId = existing.get().getSecretId();
            secretsManager.putSecretValue(PutSecretValueRequest.builder()
                    .secretId(secretId)
                    .secretString(request.getApiToken())
                    .build());
        } else {
            String name = "agentic-pm/jira/" + effective;
            secretId = secretsManager.createSecret(CreateSecretRequest.builder()
                    .name(name)
                    .secretString(request.getApiToken())
                    .build()).arn();
        }

        JiraIntegration integration = JiraIntegration.builder()
                .userId(effective)
                .jiraSite(normalizeSite(request.getJiraSite()))
                .jiraEmail(request.getJiraEmail().trim())
                .secretId(secretId)
                .updatedAt(Instant.now())
                .build();
        repo.upsert(integration);

        return new JiraStatusResponseDto(true, integration.getJiraSite(), integration.getJiraEmail());
    }

    public JiraStatusResponseDto status(String userId) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        return repo.getByUserId(effective)
                .map(i -> new JiraStatusResponseDto(true, i.getJiraSite(), i.getJiraEmail()))
                .orElseGet(() -> new JiraStatusResponseDto(false, null, null));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String normalizeSite(String site) {
        String s = site.trim();
        if (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}

