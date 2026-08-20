package com.agentic.pm.integration.bedrock;

import java.util.List;

public interface BedrockClient {

    List<String> generateQuestions(String descriptionText);

    /**
     * Generate technical documentation (markdown) using project description + ordered clarification Q&A.
     */
    String generateTechnicalDocumentation(String projectName,
                                          String descriptionText,
                                          List<QuestionAnswerPair> clarificationQa);

    /**
     * Generate task/subtask breakdown with estimated hours from technical documentation markdown.
     */
    List<com.agentic.pm.domain.Breakdown.BreakdownTask> generateBreakdown(String projectName, String technicalDocumentationMarkdown);
}

