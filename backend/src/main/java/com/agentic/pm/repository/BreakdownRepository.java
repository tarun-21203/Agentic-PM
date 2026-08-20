package com.agentic.pm.repository;

import com.agentic.pm.domain.Breakdown;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BreakdownRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public BreakdownRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Breakdown save(Breakdown breakdown) {
        Instant now = Instant.now();
        if (breakdown.getCreatedAt() == null) {
            breakdown.setCreatedAt(now);
        }
        breakdown.setUpdatedAt(now);

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(breakdown))
                .build());
        return breakdown;
    }

    public Optional<Breakdown> getLatest(String projectId) {
        // For MVP: store a single version key "v1". Later, can query by SK.
        GetItemResponse resp = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "projectId", AttributeValue.builder().s(projectId).build(),
                        "breakdownVersion", AttributeValue.builder().s("v1").build()
                ))
                .build());
        if (resp.item() == null || resp.item().isEmpty()) return Optional.empty();
        return Optional.of(fromItem(resp.item()));
    }

    private Map<String, AttributeValue> toItem(Breakdown b) {
        return Map.of(
                "projectId", AttributeValue.builder().s(b.getProjectId()).build(),
                "breakdownVersion", AttributeValue.builder().s(b.getBreakdownVersion()).build(),
                "tasks", AttributeValue.builder().s(toJson(b.getTasks())).build(),
                "createdAt", AttributeValue.builder().s(b.getCreatedAt().toString()).build(),
                "updatedAt", AttributeValue.builder().s(b.getUpdatedAt().toString()).build()
        );
    }

    private Breakdown fromItem(Map<String, AttributeValue> item) {
        List<Breakdown.BreakdownTask> tasks = fromJson(item.get("tasks").s());
        return Breakdown.builder()
                .projectId(item.get("projectId").s())
                .breakdownVersion(item.get("breakdownVersion").s())
                .tasks(tasks)
                .createdAt(Instant.parse(item.get("createdAt").s()))
                .updatedAt(Instant.parse(item.get("updatedAt").s()))
                .build();
    }

    private static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize breakdown tasks", e);
        }
    }

    private static List<Breakdown.BreakdownTask> fromJson(String s) {
        try {
            return MAPPER.readValue(s, new TypeReference<List<Breakdown.BreakdownTask>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse breakdown tasks", e);
        }
    }
}

