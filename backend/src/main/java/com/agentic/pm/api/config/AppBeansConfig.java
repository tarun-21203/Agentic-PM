package com.agentic.pm.api.config;

import com.agentic.pm.integration.bedrock.BedrockClient;
import com.agentic.pm.integration.bedrock.BedrockConverseClient;
import com.agentic.pm.integration.bedrock.StubBedrockClient;
import com.agentic.pm.repository.AnswerRepository;
import com.agentic.pm.repository.BreakdownRepository;
import com.agentic.pm.repository.JiraIntegrationRepository;
import com.agentic.pm.repository.JiraMappingRepository;
import com.agentic.pm.repository.ProjectRepository;
import com.agentic.pm.repository.QuestionRepository;
import com.agentic.pm.service.FileValidationService;
import com.agentic.pm.utils.DescriptionTextExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Set;

@Configuration
public class AppBeansConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(Environment env) {
        return DynamoDbClient.builder()
                .region(Region.of(env.getProperty("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public S3Client s3Client(Environment env) {
        return S3Client.builder()
                .region(Region.of(env.getProperty("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(Environment env) {
        return S3Presigner.builder()
                .region(Region.of(env.getProperty("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient(Environment env) {
        return SecretsManagerClient.builder()
                .region(Region.of(env.getProperty("AWS_REGION", "us-east-1")))
                .build();
    }

    @Bean
    public BedrockClient bedrockClient(Environment env) {
        boolean useStub = "true".equalsIgnoreCase(env.getProperty("BEDROCK_USE_STUB", "false"));
        if (useStub) {
            return new StubBedrockClient();
        }
        String awsRegion = env.getProperty("AWS_REGION", "us-east-1");
        String modelId = env.getProperty("BEDROCK_MODEL_ID", "google.gemma-3-4b-it");
        int maxTokens = Integer.parseInt(env.getProperty("BEDROCK_MAX_TOKENS", "8192"));
        double temperature = Double.parseDouble(env.getProperty("BEDROCK_TEMPERATURE", "0.25"));
        return new BedrockConverseClient(Region.of(awsRegion), modelId, maxTokens, temperature);
    }

    @Bean
    public DescriptionTextExtractor descriptionTextExtractor() {
        return new DescriptionTextExtractor();
    }

    @Bean
    public FileValidationService fileValidationService(Environment env) {
        long maxBytes = Long.parseLong(env.getProperty("MAX_FILE_SIZE_BYTES", "10485760"));
        Set<String> allowed = Set.of("pdf", "docx", "doc", "txt");
        return new FileValidationService(maxBytes, allowed);
    }

    @Bean
    public ProjectRepository projectRepository(DynamoDbClient dynamoDbClient, Environment env) {
        return new ProjectRepository(dynamoDbClient, env.getProperty("PROJECTS_TABLE", "Projects"));
    }

    @Bean
    public QuestionRepository questionRepository(DynamoDbClient dynamoDbClient, Environment env) {
        return new QuestionRepository(dynamoDbClient, env.getProperty("QUESTIONS_TABLE", "Questions"));
    }

    @Bean
    public AnswerRepository answerRepository(DynamoDbClient dynamoDbClient, Environment env) {
        return new AnswerRepository(dynamoDbClient, env.getProperty("ANSWERS_TABLE", "Answers"));
    }

    @Bean
    public BreakdownRepository breakdownRepository(DynamoDbClient dynamoDbClient, Environment env) {
        return new BreakdownRepository(dynamoDbClient, env.getProperty("BREAKDOWN_TABLE", "Breakdown"));
    }

    @Bean
    public JiraIntegrationRepository jiraIntegrationRepository(DynamoDbClient dynamoDbClient, Environment env) {
        return new JiraIntegrationRepository(dynamoDbClient, env.getProperty("JIRA_INTEGRATIONS_TABLE", "JiraIntegrations"));
    }

    @Bean
    public JiraMappingRepository jiraMappingRepository(DynamoDbClient dynamoDbClient, Environment env) {
        return new JiraMappingRepository(dynamoDbClient, env.getProperty("JIRA_MAPPINGS_TABLE", "JiraMappings"));
    }
}

