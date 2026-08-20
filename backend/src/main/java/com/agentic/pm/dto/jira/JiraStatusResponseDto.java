package com.agentic.pm.dto.jira;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JiraStatusResponseDto {
    private boolean connected;
    private String jiraSite;
    private String jiraEmail;
}

