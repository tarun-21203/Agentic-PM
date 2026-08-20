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
public class Question {

    private String questionId;
    private String projectId;
    private String text;
    private String category;
    private int order;
    private Instant createdAt;
}

