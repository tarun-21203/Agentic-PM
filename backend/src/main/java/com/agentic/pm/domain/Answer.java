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
public class Answer {

    private String projectId;
    private String questionId;
    private String answer;
    private Instant updatedAt;
}

