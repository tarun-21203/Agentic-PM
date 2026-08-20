package com.agentic.pm.integration.bedrock;

import com.agentic.pm.domain.Breakdown;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Amazon Bedrock foundation models via the Converse API (not a local stub).
 */
public class BedrockConverseClient implements BedrockClient {

    private static final int DEFAULT_MAX_TOKENS = 8192;
    /** Google Gemma 3 4B IT via Amazon Bedrock (enable model access in the Bedrock console). */
    private static final String DEFAULT_MODEL_ID = "google.gemma-3-4b-it";
    private static final Logger log = LoggerFactory.getLogger(BedrockConverseClient.class);
    private static final int MAX_DESCRIPTION_CHARS = 180_000;

    private final BedrockRuntimeClient runtimeClient;
    private final String modelId;
    private final int maxTokens;
    private final double temperature;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BedrockConverseClient(Region region, String modelId, int maxTokens, double temperature) {
        this.runtimeClient = BedrockRuntimeClient.builder()
                .region(region)
                .build();
        this.modelId = modelId != null && !modelId.isBlank() ? modelId.trim() : DEFAULT_MODEL_ID;
        this.maxTokens = maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS;
        this.temperature = temperature >= 0 ? temperature : 0.2;
    }

    public static BedrockConverseClient fromEnvironment() {
        Region region = Region.of(firstNonBlank(System.getenv("AWS_REGION"), "us-east-1"));
        String modelId = firstNonBlank(System.getenv("BEDROCK_MODEL_ID"), DEFAULT_MODEL_ID);
        int max = parseInt(System.getenv("BEDROCK_MAX_TOKENS"), DEFAULT_MAX_TOKENS);
        double temp = parseDouble(System.getenv("BEDROCK_TEMPERATURE"), 0.25);
        return new BedrockConverseClient(region, modelId, max, temp);
    }

    @Override
    public List<String> generateQuestions(String descriptionText) {
        String doc = truncateDescription(descriptionText);
        String prompt = """
                You are a senior product analyst and solutions architect.

                Read the PROJECT DESCRIPTION below. It is the only source of truth.

                Produce 5 to 8 clarification questions that are **specific to this document**:
                - Each question must either (a) cite or paraphrase a concrete detail from the document, or (b) name a concrete gap (missing API, SLA, auth model, data entity, integration, deployment target, etc.).
                - Ask about what is **missing, ambiguous, or inconsistent** — not about generic best practices.
                - Do **not** ask boilerplate questions (e.g. "What are the business objectives?") unless the document truly omits them.
                - Prefer: integrations/APIs, security/auth, data model, NFRs (performance, availability), roles/permissions, edge cases, environments, compliance — only where the document leaves them open.

                Return **only** a JSON array of strings (no markdown fences, no keys, no commentary).
                Example:
                ["Question 1...", "Question 2..."]

                PROJECT DESCRIPTION:
                """
                + doc;

        String raw;
        try {
            raw = converse(prompt);
        } catch (RuntimeException e) {
            log.error(
                    "Bedrock converse failed for question generation. modelId={} descriptionChars={} promptChars={}",
                    modelId,
                    doc != null ? doc.length() : 0,
                    prompt != null ? prompt.length() : 0,
                    e
            );
            throw e;
        }
        String json = stripMarkdownFence(raw);
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (list == null) return List.of();
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .limit(12)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            String preview = json == null ? "" : json.substring(0, Math.min(500, json.length()));
            log.error(
                    "Bedrock returned invalid JSON array for questions. modelId={} jsonPreview='{}'",
                    modelId,
                    preview,
                    e
            );
            throw new IllegalStateException("Bedrock did not return a valid JSON string array for questions: " + json, e);
        }
    }

    @Override
    public String generateTechnicalDocumentation(String projectName,
                                                 String descriptionText,
                                                 List<QuestionAnswerPair> clarificationQa) {
        String doc = truncateDescription(descriptionText);
        StringBuilder qaSection = new StringBuilder();
        if (clarificationQa != null) {
            int i = 1;
            for (QuestionAnswerPair qa : clarificationQa) {
                qaSection.append("### ").append(i++).append(". Q\n")
                        .append(qa.questionText() != null ? qa.questionText() : "").append("\n\n")
                        .append("**A:** ")
                        .append(qa.answerText() != null ? qa.answerText() : "").append("\n\n");
            }
        }

        String prompt = """
                You are a principal engineer writing technical documentation for implementation.

                Project name: **%s**

                Produce **one** Markdown document that is exhaustive and implementation-ready. Use the project description and the clarification Q&A as sources. Where the document is silent, list assumptions explicitly in an "Assumptions" section — do not invent external facts.

                Required sections (use these headings and order, expand with subsections as needed):

                1. # Executive summary
                2. ## Goals and scope (in/out of scope)
                3. ## Stakeholders and user roles
                4. ## System context (C4-style narrative: system, users, external systems)
                5. ## Functional requirements (numbered, testable)
                6. ## Non-functional requirements (performance, availability, scalability, observability, compliance)
                7. ## Architecture
                   - Logical components and responsibilities
                   - **Runtime / deployment platform** (e.g. cloud, regions, containers, serverless)
                   - **Data stores** and retention
                   - **Messaging / events** if any
                8. ## API specification
                   - For each REST or HTTP surface: **method**, **path**, **purpose**, **auth**, **request/response** shapes (JSON), **errors**
                   - If the document implies GraphQL or gRPC, describe those instead
                   - If APIs are unknown, list **TBD** with what must be decided
                9. ## Integrations (third-party systems, webhooks, OAuth, API keys)
                10. ## Security and privacy (authN/Z, secrets, PII, threat notes)
                11. ## Development platform and tooling
                    - Languages, frameworks, repo layout, CI/CD, build
                    - Local dev prerequisites
                12. ## Testing strategy (unit, integration, e2e, load)
                13. ## Deployment and operations (environments, config, secrets, rollout, monitoring)
                14. ## Risks and open issues
                15. ## Appendices (glossary, glossary of acronyms, decision log)

                Use tables where appropriate.

                ---

                ## Project description (source)

                %s

                ---

                ## Clarification Q&A

                %s

                """.formatted(
                escapeFormat(projectName),
                doc,
                qaSection.length() > 0 ? qaSection.toString() : "_No clarification Q&A provided._"
        );

        try {
            return converse(prompt);
        } catch (RuntimeException e) {
            log.error(
                    "Bedrock converse failed for technical documentation. modelId={} descriptionChars={} promptChars={}",
                    modelId,
                    doc != null ? doc.length() : 0,
                    prompt != null ? prompt.length() : 0,
                    e
            );
            throw e;
        }
    }

    @Override
    public List<Breakdown.BreakdownTask> generateBreakdown(String projectName, String technicalDocumentationMarkdown) {
        String tech = technicalDocumentationMarkdown == null ? "" : technicalDocumentationMarkdown;
        if (tech.length() > MAX_DESCRIPTION_CHARS) {
            tech = tech.substring(0, MAX_DESCRIPTION_CHARS) + "\n\n... (truncated for model input)\n";
        }

        String prompt = """
                You are a principal technical program manager.

                Break the following technical documentation into implementation tasks. Project name: **%s**

                Return **only** valid JSON (no markdown fences) with this exact shape:
                {
                  "tasks": [
                    {
                      "taskId": "T1",
                      "title": "short title",
                      "description": "technical description of work",
                      "estimatedHours": 8.0,
                      "subtasks": [
                        {
                          "title": "subtask title",
                          "description": "what to do",
                          "estimatedHours": 2.0
                        }
                      ]
                    }
                  ]
                }

                Rules:
                - 4 to 12 tasks unless the doc is tiny; cap at 20.
                - estimatedHours must be positive numbers (decimals allowed).
                - Subtasks are optional but preferred for larger tasks.
                - taskId must be unique (T1, T2, ...).

                TECHNICAL DOCUMENTATION:

                %s
                """.formatted(escapeFormat(projectName), tech);

        String raw;
        try {
            raw = converse(prompt);
        } catch (RuntimeException e) {
            log.error(
                    "Bedrock converse failed for breakdown generation. modelId={} projectName={} techChars={}",
                    modelId,
                    projectName,
                    tech != null ? tech.length() : 0,
                    e
            );
            throw e;
        }
        String json = stripMarkdownFence(raw);
        return parseBreakdownJson(json);
    }

    private String converse(String userPrompt) {
        try {
            ConverseRequest request = ConverseRequest.builder()
                    .modelId(modelId)
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(userPrompt))
                            .build())
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(maxTokens)
                            .temperature((float) temperature)
                            .build())
                    .build();

            ConverseResponse response = runtimeClient.converse(request);
            return extractOutputText(response);
        } catch (SdkException e) {
            log.error(
                    "Bedrock Converse failed. modelId={} maxTokens={} temperature={} promptChars={}",
                    modelId,
                    maxTokens,
                    temperature,
                    userPrompt != null ? userPrompt.length() : 0,
                    e
            );
            throw new IllegalStateException("Bedrock Converse failed: " + e.getMessage(), e);
        }
    }

    private static String extractOutputText(ConverseResponse response) {
        if (response == null || response.output() == null || response.output().message() == null) {
            return "";
        }
        if (response.output().message().content() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : response.output().message().content()) {
            if (block == null) continue;
            if (block.text() != null && !block.text().isBlank()) {
                sb.append(block.text());
            }
        }
        return sb.toString().trim();
    }

    private List<Breakdown.BreakdownTask> parseBreakdownJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode tasks = root.get("tasks");
            if (tasks == null || !tasks.isArray()) {
                throw new IllegalStateException("Missing tasks array");
            }
            List<Breakdown.BreakdownTask> out = new ArrayList<>();
            int seq = 1;
            for (JsonNode t : tasks) {
                String taskId = textOr(t, "taskId", "T" + seq++);
                String title = textOr(t, "title", "Task");
                String description = textOr(t, "description", "");
                double hours = doubleOr(t.get("estimatedHours"), 1.0);

                List<Breakdown.BreakdownSubtask> subs = new ArrayList<>();
                JsonNode st = t.get("subtasks");
                if (st != null && st.isArray()) {
                    for (JsonNode s : st) {
                        subs.add(Breakdown.BreakdownSubtask.builder()
                                .title(textOr(s, "title", "Subtask"))
                                .description(textOr(s, "description", ""))
                                .estimatedHours(doubleOr(s.get("estimatedHours"), 1.0))
                                .build());
                    }
                }
                out.add(Breakdown.BreakdownTask.builder()
                        .taskId(taskId)
                        .title(title)
                        .description(description)
                        .estimatedHours(hours)
                        .subtasks(subs)
                        .build());
            }
            if (out.isEmpty()) {
                throw new IllegalStateException("Empty tasks");
            }
            return out;
        } catch (Exception e) {
            String preview = json == null ? "" : json.substring(0, Math.min(500, json.length()));
            log.error(
                    "Bedrock returned invalid breakdown JSON. modelId={} jsonPreview='{}'",
                    modelId,
                    preview,
                    e
            );
            throw new IllegalStateException("Failed to parse breakdown JSON from Bedrock: " + json, e);
        }
    }

    private static String textOr(JsonNode node, String field, String def) {
        JsonNode v = node != null ? node.get(field) : null;
        if (v == null || v.isNull()) return def;
        String s = v.asText();
        return s == null || s.isBlank() ? def : s.trim();
    }

    private static double doubleOr(JsonNode node, double def) {
        if (node == null || node.isNull() || !node.isNumber()) return def;
        return node.asDouble();
    }

    private static String stripMarkdownFence(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (!s.startsWith("```")) {
            return s;
        }
        int firstNl = s.indexOf('\n');
        int lastFence = s.lastIndexOf("```");
        if (firstNl >= 0 && lastFence > firstNl) {
            return s.substring(firstNl + 1, lastFence).trim();
        }
        return s;
    }

    private static String truncateDescription(String descriptionText) {
        String doc = descriptionText == null ? "" : descriptionText.trim();
        if (doc.length() > MAX_DESCRIPTION_CHARS) {
            return doc.substring(0, MAX_DESCRIPTION_CHARS) + "\n\n... (truncated for model input)\n";
        }
        return doc;
    }

    private static String escapeFormat(String s) {
        if (s == null) return "";
        return s.replace("%", "%%");
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }

    private static int parseInt(String v, int def) {
        if (v == null || v.isBlank()) return def;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double parseDouble(String v, double def) {
        if (v == null || v.isBlank()) return def;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
