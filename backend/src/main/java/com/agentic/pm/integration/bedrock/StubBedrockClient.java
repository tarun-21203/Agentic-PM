package com.agentic.pm.integration.bedrock;

import java.util.List;

public class StubBedrockClient implements BedrockClient {

    @Override
    public List<String> generateQuestions(String descriptionText) {
        return List.of(
                "What are the primary business objectives of this project?",
                "What are the key user roles and their responsibilities?",
                "What systems or APIs does this project need to integrate with?",
                "Are there any regulatory, security, or compliance requirements?",
                "What is the expected timeline and major milestones?"
        );
    }

    @Override
    public String generateTechnicalDocumentation(String projectName,
                                                 String descriptionText,
                                                 List<QuestionAnswerPair> clarificationQa) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Technical Documentation: ").append(projectName).append("\n\n");
        sb.append("## Overview\n");
        sb.append("This document was generated from the uploaded project description and the clarification Q&A.\n\n");

        sb.append("## Source Description (Extract)\n");
        String desc = descriptionText == null ? "" : descriptionText.trim();
        if (desc.length() > 1200) {
            desc = desc.substring(0, 1200) + "\n... (truncated)\n";
        }
        sb.append("```\n").append(desc).append("\n```\n\n");

        sb.append("## Clarification Q&A\n");
        if (clarificationQa == null || clarificationQa.isEmpty()) {
            sb.append("- No clarification pairs recorded.\n\n");
        } else {
            int i = 1;
            for (QuestionAnswerPair qa : clarificationQa) {
                sb.append("### ").append(i++).append(". Question\n");
                sb.append(qa.questionText()).append("\n\n");
                sb.append("**Answer:** ").append(qa.answerText()).append("\n\n");
            }
        }

        sb.append("## Architecture (High-level)\n");
        sb.append("- API: AWS API Gateway → Java 17 AWS Lambda handlers\n");
        sb.append("- Data: DynamoDB (Projects, Questions, Answers)\n");
        sb.append("- Storage: S3 (inputs + generated documentation)\n\n");

        sb.append("## Assumptions\n");
        sb.append("- JIRA Cloud integration is configured per project.\n");
        sb.append("- Set BEDROCK_USE_STUB=false and BEDROCK_MODEL_ID for Amazon Bedrock Converse (production).\n");
        return sb.toString();
    }

    @Override
    public List<com.agentic.pm.domain.Breakdown.BreakdownTask> generateBreakdown(String projectName, String technicalDocumentationMarkdown) {
        return List.of(
                com.agentic.pm.domain.Breakdown.BreakdownTask.builder()
                        .taskId("T1")
                        .title("Project setup and infrastructure")
                        .description("Provision AWS resources (S3, DynamoDB) and configure environment variables.")
                        .estimatedHours(6)
                        .subtasks(List.of(
                                com.agentic.pm.domain.Breakdown.BreakdownSubtask.builder()
                                        .title("Terraform baseline")
                                        .description("Define and apply Terraform for S3/DynamoDB tables.")
                                        .estimatedHours(3)
                                        .build(),
                                com.agentic.pm.domain.Breakdown.BreakdownSubtask.builder()
                                        .title("CI/build setup")
                                        .description("Configure build pipeline for Java Lambda artifacts.")
                                        .estimatedHours(3)
                                        .build()
                        ))
                        .build(),
                com.agentic.pm.domain.Breakdown.BreakdownTask.builder()
                        .taskId("T2")
                        .title("API implementation (projects, Q&A)")
                        .description("Implement project ingestion endpoints, Q&A endpoints, and validations.")
                        .estimatedHours(10)
                        .subtasks(List.of(
                                com.agentic.pm.domain.Breakdown.BreakdownSubtask.builder()
                                        .title("Project endpoints")
                                        .description("Create/list/get projects and description upload URL flow.")
                                        .estimatedHours(4)
                                        .build(),
                                com.agentic.pm.domain.Breakdown.BreakdownSubtask.builder()
                                        .title("Questions + answers endpoints")
                                        .description("Generate questions, save answers, mark clarified.")
                                        .estimatedHours(6)
                                        .build()
                        ))
                        .build()
        );
    }
}

