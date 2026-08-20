package com.agentic.pm.repository;

import com.agentic.pm.domain.JiraMapping;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class JiraMappingRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public JiraMappingRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void put(String projectId, String internalId, String jiraIssueKey, String jiraIssueType) {
        JiraMapping mapping = JiraMapping.builder()
                .projectId(projectId)
                .internalId(internalId)
                .jiraIssueKey(jiraIssueKey)
                .jiraIssueType(jiraIssueType)
                .createdAt(Instant.now())
                .build();
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(mapping))
                .build());
    }

    public List<JiraMapping> listByProjectId(String projectId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("projectId = :pid")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.builder().s(projectId).build()
                ))
                .build();
        var items = dynamoDbClient.query(request).items();
        if (items == null) return List.of();
        return items.stream().map(this::fromItem).collect(Collectors.toList());
    }

    private Map<String, AttributeValue> toItem(JiraMapping m) {
        return Map.of(
                "projectId", AttributeValue.builder().s(m.getProjectId()).build(),
                "internalId", AttributeValue.builder().s(m.getInternalId()).build(),
                "jiraIssueKey", AttributeValue.builder().s(m.getJiraIssueKey()).build(),
                "jiraIssueType", AttributeValue.builder().s(m.getJiraIssueType()).build(),
                "createdAt", AttributeValue.builder().s(m.getCreatedAt().toString()).build()
        );
    }

    private JiraMapping fromItem(Map<String, AttributeValue> item) {
        return JiraMapping.builder()
                .projectId(item.get("projectId").s())
                .internalId(item.get("internalId").s())
                .jiraIssueKey(item.get("jiraIssueKey").s())
                .jiraIssueType(item.get("jiraIssueType").s())
                .createdAt(Instant.parse(item.get("createdAt").s()))
                .build();
    }
}

