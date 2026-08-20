package com.agentic.pm.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraIntegration {
    private String userId;
    private String jiraSite;
    private String jiraEmail;
    private String secretId;   // Secrets Manager secret id/arn containing API token
    private Instant updatedAt;
}

