package com.agentic.pm.repository;

import com.agentic.pm.domain.Answer;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnswerRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public AnswerRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void putAnswer(String projectId, String questionId, String answerText) {
        Answer answer = Answer.builder()
                .projectId(projectId)
                .questionId(questionId)
                .answer(answerText)
                .updatedAt(Instant.now())
                .build();
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(answer))
                .build());
    }

    public Map<String, String> getAnswersByProjectId(String projectId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("projectId = :pid")
                .expressionAttributeValues(Map.of(
                        ":pid", AttributeValue.builder().s(projectId).build()
                ))
                .build();
        List<Map<String, AttributeValue>> items = dynamoDbClient.query(request).items();
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (var item : items) {
            String qid = getS(item, "questionId");
            String ans = getS(item, "answer");
            if (!qid.isBlank() && !ans.isBlank()) {
                map.put(qid, ans);
            }
        }
        return map;
    }

    private Map<String, AttributeValue> toItem(Answer a) {
        return Map.of(
                "projectId", AttributeValue.builder().s(a.getProjectId()).build(),
                "questionId", AttributeValue.builder().s(a.getQuestionId()).build(),
                "answer", AttributeValue.builder().s(a.getAnswer()).build(),
                "updatedAt", AttributeValue.builder().s(a.getUpdatedAt().toString()).build()
        );
    }

    private static String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return v != null && v.s() != null ? v.s() : "";
    }
}

