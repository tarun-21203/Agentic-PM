package com.agentic.pm.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Breakdown {

    private String projectId;
    private String breakdownVersion;
    private List<BreakdownTask> tasks;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownTask {
        private String taskId;
        private String title;
        private String description;
        private double estimatedHours;
        private List<BreakdownSubtask> subtasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownSubtask {
        private String title;
        private String description;
        private double estimatedHours;
    }
}

