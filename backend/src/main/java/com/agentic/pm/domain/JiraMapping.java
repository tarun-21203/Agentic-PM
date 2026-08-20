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
public class JiraMapping {
    private String projectId;
    private String internalId;     // internal taskId/subtaskId
    private String jiraIssueKey;   // e.g. PROJ-123
    private String jiraIssueType;  // Epic, Task, Sub-task
    private Instant createdAt;
}

