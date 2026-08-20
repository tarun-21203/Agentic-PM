package com.agentic.pm.repository;

import com.agentic.pm.domain.Question;
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

public class QuestionRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public QuestionRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public List<Question> saveAll(List<Question> questions) {
        Instant now = Instant.now();
        List<Question> enriched = new ArrayList<>(questions.size());
        for (Question q : questions) {
            if (q.getQuestionId() == null || q.getQuestionId().isBlank()) {
                q.setQuestionId(UUID.randomUUID().toString());
            }
            if (q.getCreatedAt() == null) {
                q.setCreatedAt(now);
            }
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(toItem(q))
                    .build());
            enriched.add(q);
        }
        return enriched;
    }

    public List<Question> findByProjectId(String projectId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .indexName("projectId-createdAt-index")
                .keyConditionExpression("projectId = :pid")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.builder().s(projectId).build()
                ))
                .scanIndexForward(true)
                .build();
        return dynamoDbClient.query(request).items().stream()
                .map(this::fromItem)
                .collect(Collectors.toList());
    }

    private Map<String, AttributeValue> toItem(Question q) {
        Map<String, AttributeValue> item = new java.util.HashMap<>();
        item.put("questionId", AttributeValue.builder().s(q.getQuestionId()).build());
        item.put("projectId", AttributeValue.builder().s(q.getProjectId()).build());
        item.put("text", AttributeValue.builder().s(q.getText()).build());
        item.put("questionOrder", AttributeValue.builder().n(Integer.toString(q.getOrder())).build());
        item.put("createdAt", AttributeValue.builder().s(q.getCreatedAt().toString()).build());
        if (q.getCategory() != null && !q.getCategory().isBlank()) {
            item.put("category", AttributeValue.builder().s(q.getCategory()).build());
        }
        return item;
    }

    private Question fromItem(Map<String, AttributeValue> item) {
        String category = getS(item, "category");
        return Question.builder()
                .questionId(getS(item, "questionId"))
                .projectId(getS(item, "projectId"))
                .text(getS(item, "text"))
                .category(category.isBlank() ? null : category)
                .order(Integer.parseInt(item.getOrDefault("questionOrder", AttributeValue.builder().n("0").build()).n()))
                .createdAt(Instant.parse(getS(item, "createdAt")))
                .build();
    }

    private static String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null && v.s() != null ? v.s() : "";
    }
}

