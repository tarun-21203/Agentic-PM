package com.agentic.pm.dto.jira;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraConnectRequestDto {
    private String jiraSite;   // e.g. https://tenant.atlassian.net
    private String jiraEmail;  // Atlassian account email
    private String apiToken;   // JIRA API token
}

