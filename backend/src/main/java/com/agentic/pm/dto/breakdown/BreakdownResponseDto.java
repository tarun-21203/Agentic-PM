package com.agentic.pm.dto.breakdown;

import com.agentic.pm.domain.Breakdown;
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
public class BreakdownResponseDto {

    private String projectId;
    private String breakdownVersion;
    private List<BreakdownTaskDto> tasks;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownTaskDto {
        private String taskId;
        private String title;
        private String description;
        private double estimatedHours;
        private List<BreakdownSubtaskDto> subtasks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownSubtaskDto {
        private String title;
        private String description;
        private double estimatedHours;
    }

    public static BreakdownResponseDto from(Breakdown b) {
        return BreakdownResponseDto.builder()
                .projectId(b.getProjectId())
                .breakdownVersion(b.getBreakdownVersion())
                .tasks(b.getTasks() == null ? List.of() : b.getTasks().stream().map(t ->
                        BreakdownTaskDto.builder()
                                .taskId(t.getTaskId())
                                .title(t.getTitle())
                                .description(t.getDescription())
                                .estimatedHours(t.getEstimatedHours())
                                .subtasks(t.getSubtasks() == null ? List.of() : t.getSubtasks().stream().map(st ->
                                        BreakdownSubtaskDto.builder()
                                                .title(st.getTitle())
                                                .description(st.getDescription())
                                                .estimatedHours(st.getEstimatedHours())
                                                .build()
                                ).toList())
                                .build()
                ).toList())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}

