package com.agentic.pm.repository;

import com.agentic.pm.domain.Project;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProjectRepository {

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public ProjectRepository(DynamoDbClient dynamoDb, String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    public Project save(Project project) {
        if (project.getProjectId() == null || project.getProjectId().isBlank()) {
            project.setProjectId(UUID.randomUUID().toString());
        }
        Instant now = Instant.now();
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(now);
        }
        project.setUpdatedAt(now);

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(project))
                .build());
        return project;
    }

    public Optional<Project> findById(String projectId) {
        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "projectId", AttributeValue.builder().s(projectId).build()
                ))
                .build());
        if (response.item() == null || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(fromItem(response.item()));
    }

    public List<Project> findByUserId(String userId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName("userId-createdAt-index")
                .keyConditionExpression("userId = :uid")
                .expressionAttributeValues(Map.of(":uid", AttributeValue.builder().s(userId).build()))
                .scanIndexForward(false)
                .build();
        return dynamoDb.query(request).items().stream()
                .map(this::fromItem)
                .collect(Collectors.toList());
    }

    public void updateDescriptionKey(String projectId, String descriptionS3Key) {
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("projectId", AttributeValue.builder().s(projectId).build()))
                .updateExpression("SET descriptionS3Key = :k, updatedAt = :t")
                .expressionAttributeValues(Map.of(
                        ":k", AttributeValue.builder().s(descriptionS3Key).build(),
                        ":t", AttributeValue.builder().s(Instant.now().toString()).build()
                ))
                .build());
    }

    public void updateStatus(String projectId, Project.ProjectStatus status) {
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("projectId", AttributeValue.builder().s(projectId).build()))
                .updateExpression("SET #s = :s, updatedAt = :t")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(
                        ":s", AttributeValue.builder().s(status.name()).build(),
                        ":t", AttributeValue.builder().s(Instant.now().toString()).build()
                ))
                .build());
    }

    public void updateDocKey(String projectId, String docS3Key) {
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("projectId", AttributeValue.builder().s(projectId).build()))
                .updateExpression("SET docS3Key = :k, updatedAt = :t")
                .expressionAttributeValues(Map.of(
                        ":k", AttributeValue.builder().s(docS3Key).build(),
                        ":t", AttributeValue.builder().s(Instant.now().toString()).build()
                ))
                .build());
    }

    public void updateJiraProjectKey(String projectId, String jiraProjectKey) {
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("projectId", AttributeValue.builder().s(projectId).build()))
                .updateExpression("SET jiraProjectKey = :k, updatedAt = :t")
                .expressionAttributeValues(Map.of(
                        ":k", AttributeValue.builder().s(jiraProjectKey).build(),
                        ":t", AttributeValue.builder().s(Instant.now().toString()).build()
                ))
                .build());
    }

    private Map<String, AttributeValue> toItem(Project p) {
        Map<String, AttributeValue> item = new java.util.HashMap<>();
        item.put("projectId", AttributeValue.builder().s(p.getProjectId()).build());
        item.put("userId", AttributeValue.builder().s(p.getUserId()).build());
        item.put("name", AttributeValue.builder().s(p.getName()).build());
        item.put("status", AttributeValue.builder().s(p.getStatus().name()).build());
        item.put("createdAt", AttributeValue.builder().s(p.getCreatedAt().toString()).build());
        item.put("updatedAt", AttributeValue.builder().s(p.getUpdatedAt().toString()).build());
        if (p.getDescriptionS3Key() != null && !p.getDescriptionS3Key().isBlank()) {
            item.put("descriptionS3Key", AttributeValue.builder().s(p.getDescriptionS3Key()).build());
        }
        if (p.getDocS3Key() != null && !p.getDocS3Key().isBlank()) {
            item.put("docS3Key", AttributeValue.builder().s(p.getDocS3Key()).build());
        }
        if (p.getJiraProjectKey() != null && !p.getJiraProjectKey().isBlank()) {
            item.put("jiraProjectKey", AttributeValue.builder().s(p.getJiraProjectKey()).build());
        }
        return item;
    }

    private Project fromItem(Map<String, AttributeValue> item) {
        return Project.builder()
                .projectId(getS(item, "projectId"))
                .userId(getS(item, "userId"))
                .name(getS(item, "name"))
                .status(Project.ProjectStatus.valueOf(getS(item, "status")))
                .descriptionS3Key(emptyToNull(getS(item, "descriptionS3Key")))
                .docS3Key(emptyToNull(getS(item, "docS3Key")))
                .jiraProjectKey(emptyToNull(getS(item, "jiraProjectKey")))
                .createdAt(Instant.parse(getS(item, "createdAt")))
                .updatedAt(Instant.parse(getS(item, "updatedAt")))
                .build();
    }

    private static String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null && v.s() != null ? v.s() : "";
    }

    private static String emptyToNull(String s) {
        return s != null && !s.isBlank() ? s : null;
    }
}

