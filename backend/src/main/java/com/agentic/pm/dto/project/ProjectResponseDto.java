package com.agentic.pm.dto.project;

import com.agentic.pm.domain.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponseDto {

    private String projectId;
    private String userId;
    private String name;
    private String status;
    private String descriptionS3Key;
    private String docS3Key;
    private String jiraProjectKey;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProjectResponseDto from(Project p) {
        return ProjectResponseDto.builder()
                .projectId(p.getProjectId())
                .userId(p.getUserId())
                .name(p.getName())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .descriptionS3Key(p.getDescriptionS3Key())
                .docS3Key(p.getDocS3Key())
                .jiraProjectKey(p.getJiraProjectKey())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

