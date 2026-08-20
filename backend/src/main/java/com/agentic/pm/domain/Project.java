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
public class Project {

    private String projectId;
    private String userId;
    private String name;
    private ProjectStatus status;
    private String descriptionS3Key;
    private String docS3Key;
    private String jiraProjectKey;
    private Instant createdAt;
    private Instant updatedAt;

    public enum ProjectStatus {
        DRAFT,
        IN_REVIEW,
        CLARIFIED
    }
}

