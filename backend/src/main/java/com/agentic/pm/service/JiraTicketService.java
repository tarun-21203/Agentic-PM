package com.agentic.pm.service;

import com.agentic.pm.domain.Breakdown;
import com.agentic.pm.domain.JiraIntegration;
import com.agentic.pm.domain.Project;
import com.agentic.pm.exception.ProjectNotFoundException;
import com.agentic.pm.integration.jira.JiraAdf;
import com.agentic.pm.integration.jira.JiraClient;
import com.agentic.pm.repository.BreakdownRepository;
import com.agentic.pm.repository.JiraIntegrationRepository;
import com.agentic.pm.repository.JiraMappingRepository;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.api.config.RuntimeEnvConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JiraTicketService {

    private static final String DEFAULT_USER_ID = "default-user";

    private final ProjectRepository projectRepository;
    private final BreakdownRepository breakdownRepository;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final JiraMappingRepository jiraMappingRepository;
    private final SecretsManagerClient secretsManager;
    private final RuntimeEnvConfig runtimeEnvConfig;

    public JiraTicketService(ProjectRepository projectRepository,
                             BreakdownRepository breakdownRepository,
                             JiraIntegrationRepository jiraIntegrationRepository,
                             JiraMappingRepository jiraMappingRepository,
                             SecretsManagerClient secretsManager,
                             RuntimeEnvConfig runtimeEnvConfig) {
        this.projectRepository = projectRepository;
        this.breakdownRepository = breakdownRepository;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.jiraMappingRepository = jiraMappingRepository;
        this.secretsManager = secretsManager;
        this.runtimeEnvConfig = runtimeEnvConfig;
    }

    public Map<String, Object> createJiraProject(String projectId, String userId) {
        Project p = requireOwnedProject(projectId, userId);
        JiraClient client = jiraClient(userId);

        Map<String, Object> myself = client.getMyself();
        String accountId = (String) myself.get("accountId");

        String key = deriveProjectKey(p.getName(), projectId);
        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("name", p.getName());
        body.put("projectTypeKey", "software");
        body.put("leadAccountId", accountId);
        body.put("assigneeType", "PROJECT_LEAD");

        Map<String, Object> created = client.createProject(body);
        String createdKey = (String) created.getOrDefault("key", key);
        ensureKanbanBoard(client, createdKey, p.getName());
        projectRepository.updateJiraProjectKey(projectId, createdKey);
        return Map.of("jiraProjectKey", createdKey);
    }

    /**
     * API-created software projects often have no board. Create a Kanban board (filter + Agile board) when none exist.
     */
    @SuppressWarnings("unchecked")
    private static void ensureKanbanBoard(JiraClient client, String projectKey, String projectName) {
        Map<String, Object> existing = client.listAgileBoardsForProject(projectKey);
        Object valuesObj = existing.get("values");
        if (valuesObj instanceof List<?> list && !list.isEmpty()) {
            return;
        }
        String label = projectName != null && !projectName.isBlank() ? projectName.trim() : projectKey;
        Map<String, Object> filterBody = new HashMap<>();
        filterBody.put("name", label + " (board)");
        filterBody.put("jql", "project = " + jqlProjectToken(projectKey) + " ORDER BY Rank ASC");

        Map<String, Object> filterResp = client.createFilter(filterBody);
        int filterId = toPositiveInt(filterResp.get("id"), "filter id");

        Map<String, Object> boardBody = new HashMap<>();
        boardBody.put("name", label + " — Kanban");
        boardBody.put("type", "kanban");
        boardBody.put("filterId", filterId);
        boardBody.put("location", Map.of(
                "type", "project",
                "projectKeyOrId", projectKey
        ));
        client.createAgileBoard(boardBody);
    }

    private static void putDescriptionAdf(Map<String, Object> fields, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        fields.put("description", JiraAdf.documentFromPlainText(text));
    }

    private static String jqlProjectToken(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            return "\"\"";
        }
        String k = projectKey.trim();
        if (k.matches("[A-Za-z][A-Za-z0-9_]*")) {
            return k;
        }
        return "\"" + k.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static int toPositiveInt(Object raw, String what) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw != null) {
            return Integer.parseInt(raw.toString().trim());
        }
        throw new IllegalStateException("JIRA response missing " + what);
    }

    public Map<String, Object> createJiraTickets(String projectId, String userId) {
        Project p = requireOwnedProject(projectId, userId);
        if (p.getJiraProjectKey() == null || p.getJiraProjectKey().isBlank()) {
            throw new IllegalStateException("JIRA project not created yet");
        }
        Breakdown breakdown = breakdownRepository.getLatest(projectId)
                .orElseThrow(() -> new IllegalStateException("Breakdown not generated yet"));

        JiraClient client = jiraClient(userId);
        String projectKey = p.getJiraProjectKey();

        Map<String, Object> createMeta = client.getIssueCreateMetadata(projectKey);
        List<Map<String, Object>> issueTypes = issueTypesFromCreateMeta(createMeta);
        Optional<String> epicTypeId = tryResolveEpicTypeId(issueTypes);
        String parentWorkItemTypeId = resolveParentWorkItemTypeId(issueTypes);
        String subTaskTypeId = resolveSubTaskTypeId(issueTypes);

        String epicKey = null;
        if (epicTypeId.isPresent()) {
            String epicNameField = runtimeEnvConfig.jiraEpicNameField();
            Map<String, Object> epicFields = new HashMap<>();
            epicFields.put("project", Map.of("key", projectKey));
            epicFields.put("summary", p.getName());
            epicFields.put("issuetype", issueTypeRef(epicTypeId.get()));
            epicFields.put(epicNameField, p.getName());

            Map<String, Object> epicResp = client.createIssue(Map.of("fields", epicFields));
            epicKey = (String) epicResp.get("key");
            jiraMappingRepository.put(projectId, "EPIC", epicKey, "Epic");
        }

        String epicLinkField = runtimeEnvConfig.jiraEpicLinkField();

        // Create tasks and subtasks
        for (Breakdown.BreakdownTask task : breakdown.getTasks()) {
            Map<String, Object> taskFields = new HashMap<>();
            taskFields.put("project", Map.of("key", projectKey));
            taskFields.put("summary", task.getTitle());
            putDescriptionAdf(taskFields, task.getDescription());
            taskFields.put("issuetype", issueTypeRef(parentWorkItemTypeId));
            if (epicKey != null) {
                taskFields.put(epicLinkField, epicKey);
            }

            Map<String, Object> taskResp = client.createIssue(Map.of("fields", taskFields));
            String taskKey = (String) taskResp.get("key");
            jiraMappingRepository.put(projectId, task.getTaskId(), taskKey, "Task");

            if (task.getSubtasks() != null) {
                int idx = 1;
                for (Breakdown.BreakdownSubtask st : task.getSubtasks()) {
                    Map<String, Object> stFields = new HashMap<>();
                    stFields.put("project", Map.of("key", projectKey));
                    stFields.put("summary", st.getTitle());
                    putDescriptionAdf(stFields, st.getDescription());
                    stFields.put("issuetype", issueTypeRef(subTaskTypeId));
                    stFields.put("parent", Map.of("key", taskKey));

                    Map<String, Object> stResp = client.createIssue(Map.of("fields", stFields));
                    String stKey = (String) stResp.get("key");
                    jiraMappingRepository.put(projectId, task.getTaskId() + ":S" + idx++, stKey, "Sub-task");
                }
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("epicKey", epicKey != null ? epicKey : "");
        out.put("epicLinked", epicKey != null);
        return out;
    }

    public Map<String, Object> updateJiraTickets(String projectId, String userId) {
        Project p = requireOwnedProject(projectId, userId);
        if (p.getJiraProjectKey() == null || p.getJiraProjectKey().isBlank()) {
            throw new IllegalStateException("JIRA project not created yet");
        }
        Breakdown breakdown = breakdownRepository.getLatest(projectId)
                .orElseThrow(() -> new IllegalStateException("Breakdown not generated yet"));

        JiraClient client = jiraClient(userId);
        Map<String, String> taskKeyById = new HashMap<>();
        jiraMappingRepository.listByProjectId(projectId).forEach(m -> taskKeyById.put(m.getInternalId(), m.getJiraIssueKey()));

        // Update Epic summary if present
        String epicKey = taskKeyById.get("EPIC");
        if (epicKey != null) {
            client.updateIssue(epicKey, Map.of("fields", Map.of("summary", p.getName())));
        }

        // Update tasks/subtasks (description must be ADF for Jira Cloud v3)
        for (Breakdown.BreakdownTask task : breakdown.getTasks()) {
            String key = taskKeyById.get(task.getTaskId());
            if (key != null) {
                Map<String, Object> fields = new HashMap<>();
                fields.put("summary", task.getTitle());
                fields.put("description", JiraAdf.documentFromPlainText(task.getDescription()));
                client.updateIssue(key, Map.of("fields", fields));
            }
        }
        return Map.of("status", "updated");
    }

    private Project requireOwnedProject(String projectId, String userId) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        return projectRepository.findById(projectId)
                .filter(pr -> effective.equals(pr.getUserId()))
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private JiraClient jiraClient(String userId) {
        String effective = userId != null && !userId.isBlank() ? userId : DEFAULT_USER_ID;
        JiraIntegration integration = jiraIntegrationRepository.getByUserId(effective)
                .orElseThrow(() -> new IllegalStateException("JIRA is not connected"));
        String token = secretsManager.getSecretValue(GetSecretValueRequest.builder()
                .secretId(integration.getSecretId())
                .build()).secretString();
        return new JiraClient(integration.getJiraSite(), integration.getJiraEmail(), token);
    }

    /** Name-based prefix + stable suffix from app projectId so Jira keys stay unique (≤10 chars). */
    private static String deriveProjectKey(String name, String projectId) {
        String base = lettersFromName(name);
        String suffix = stableSuffixFromProjectId(projectId);
        String key = (base + suffix).toUpperCase(Locale.ROOT);
        if (key.length() > 10) {
            key = key.substring(0, 10);
        }
        if (!key.isEmpty() && !Character.isLetter(key.charAt(0))) {
            key = "P" + key.substring(0, Math.min(key.length() - 1, 9));
        }
        return key;
    }

    private static String lettersFromName(String name) {
        if (name == null || name.isBlank()) {
            return "PR";
        }
        String letters = name.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (letters.length() < 2) {
            return "PR";
        }
        return letters.substring(0, Math.min(letters.length(), 6));
    }

    private static String stableSuffixFromProjectId(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return "A000";
        }
        String trimmed = projectId.trim();
        try {
            UUID u = UUID.fromString(trimmed);
            long bits = u.getMostSignificantBits() ^ u.getLeastSignificantBits();
            int v = (int) (bits & 0xFFF);
            return "A" + String.format(Locale.ROOT, "%03X", v);
        } catch (IllegalArgumentException e) {
            int v = Math.abs(trimmed.hashCode()) & 0xFFF;
            return "A" + String.format(Locale.ROOT, "%03X", v);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> issueTypesFromCreateMeta(Map<String, Object> meta) {
        Object projectsObj = meta.get("projects");
        if (!(projectsObj instanceof List<?> rawProjects) || rawProjects.isEmpty()) {
            throw new IllegalStateException("JIRA createmeta returned no projects");
        }
        Object first = rawProjects.get(0);
        if (!(first instanceof Map<?, ?> projectMap)) {
            throw new IllegalStateException("JIRA createmeta project entry malformed");
        }
        Object typesObj = projectMap.get("issuetypes");
        if (!(typesObj instanceof List<?> typeList) || typeList.isEmpty()) {
            throw new IllegalStateException("JIRA createmeta returned no issue types for this project");
        }
        return typeList.stream()
                .filter(Map.class::isInstance)
                .map(t -> (Map<String, Object>) t)
                .toList();
    }

    private static Map<String, Object> issueTypeRef(String id) {
        return Map.of("id", id);
    }

    /** Empty when the Jira project has no Epic type (e.g. team-managed with only Task / Sub-task). */
    private static Optional<String> tryResolveEpicTypeId(List<Map<String, Object>> types) {
        Optional<String> exact = findIssueTypeId(types, "Epic", false);
        if (exact.isPresent()) {
            return exact;
        }
        return types.stream()
                .filter(t -> !Boolean.TRUE.equals(t.get("subtask")))
                .filter(t -> {
                    String n = stringVal(t.get("name")).toLowerCase(Locale.ROOT);
                    return n.contains("epic");
                })
                .map(t -> stringVal(t.get("id")))
                .findFirst();
    }

    private static String resolveParentWorkItemTypeId(List<Map<String, Object>> types) {
        for (String candidate : List.of("Task", "Story", "Bug", "Feature", "New Feature")) {
            Optional<String> id = findIssueTypeId(types, candidate, false);
            if (id.isPresent() && !isLikelyEpicType(types, id.get())) {
                return id.get();
            }
        }
        return types.stream()
                .filter(t -> !Boolean.TRUE.equals(t.get("subtask")))
                .filter(t -> !nameLooksLikeEpic(t))
                .map(t -> stringVal(t.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No standard (non-subtask) issue type found for tasks. Available: " + summarizeIssueTypes(types)));
    }

    private static String resolveSubTaskTypeId(List<Map<String, Object>> types) {
        for (String candidate : List.of("Sub-task", "Subtask", "Sub task")) {
            Optional<String> id = findIssueTypeId(types, candidate, true);
            if (id.isPresent()) {
                return id.get();
            }
        }
        return types.stream()
                .filter(t -> Boolean.TRUE.equals(t.get("subtask")))
                .map(t -> stringVal(t.get("id")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "This Jira project has no sub-task issue type. Available: " + summarizeIssueTypes(types)));
    }

    private static Optional<String> findIssueTypeId(List<Map<String, Object>> types, String name, boolean subtask) {
        for (Map<String, Object> t : types) {
            if (!name.equalsIgnoreCase(stringVal(t.get("name")))) {
                continue;
            }
            boolean isSub = Boolean.TRUE.equals(t.get("subtask"));
            if (isSub == subtask) {
                return Optional.of(stringVal(t.get("id")));
            }
        }
        return Optional.empty();
    }

    private static boolean isLikelyEpicType(List<Map<String, Object>> types, String id) {
        return types.stream()
                .filter(t -> id.equals(stringVal(t.get("id"))))
                .anyMatch(JiraTicketService::nameLooksLikeEpic);
    }

    private static boolean nameLooksLikeEpic(Map<String, Object> t) {
        String n = stringVal(t.get("name")).toLowerCase(Locale.ROOT);
        return n.equals("epic") || n.contains("epic");
    }

    private static String stringVal(Object o) {
        return o == null ? "" : Objects.toString(o, "");
    }

    private static String summarizeIssueTypes(List<Map<String, Object>> types) {
        return types.stream()
                .map(t -> stringVal(t.get("name"))
                        + (Boolean.TRUE.equals(t.get("subtask")) ? " (subtask)" : ""))
                .collect(Collectors.joining(", "));
    }
}

