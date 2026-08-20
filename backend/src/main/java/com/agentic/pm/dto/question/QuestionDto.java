package com.agentic.pm.dto.question;

import com.agentic.pm.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {

    private String questionId;
    private String projectId;
    private String text;
    private String category;
    private int order;
    private Instant createdAt;

    public static QuestionDto from(Question q) {
        return QuestionDto.builder()
                .questionId(q.getQuestionId())
                .projectId(q.getProjectId())
                .text(q.getText())
                .category(q.getCategory())
                .order(q.getOrder())
                .createdAt(q.getCreatedAt())
                .build();
    }
}

