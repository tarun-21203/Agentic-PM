package com.agentic.pm.repository;

import com.agentic.pm.domain.JiraIntegration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class JiraIntegrationRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public JiraIntegrationRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void upsert(JiraIntegration integration) {
        if (integration.getUpdatedAt() == null) {
            integration.setUpdatedAt(Instant.now());
        }
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(integration))
                .build());
    }

    public Optional<JiraIntegration> getByUserId(String userId) {
        GetItemResponse resp = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("userId", AttributeValue.builder().s(userId).build()))
                .build());
        if (resp.item() == null || resp.item().isEmpty()) return Optional.empty();
        return Optional.of(fromItem(resp.item()));
    }

    private Map<String, AttributeValue> toItem(JiraIntegration j) {
        return Map.of(
                "userId", AttributeValue.builder().s(j.getUserId()).build(),
                "jiraSite", AttributeValue.builder().s(j.getJiraSite()).build(),
                "jiraEmail", AttributeValue.builder().s(j.getJiraEmail()).build(),
                "secretId", AttributeValue.builder().s(j.getSecretId()).build(),
                "updatedAt", AttributeValue.builder().s(j.getUpdatedAt().toString()).build()
        );
    }

    private JiraIntegration fromItem(Map<String, AttributeValue> item) {
        return JiraIntegration.builder()
                .userId(item.get("userId").s())
                .jiraSite(item.get("jiraSite").s())
                .jiraEmail(item.get("jiraEmail").s())
                .secretId(item.get("secretId").s())
                .updatedAt(Instant.parse(item.get("updatedAt").s()))
                .build();
    }
}

