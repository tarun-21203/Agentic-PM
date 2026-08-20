aws_region  = "us-east-1"
name_prefix = "agentic-pm"

tags = {
  Project = "agentic-pm"
}

bucket_name                           = "agentic-pm-documents"
documents_bucket_cors_allowed_origins = ["*"]

frontend_bucket_name = "agentic-pm-frontend"

projects_table_name          = "Projects"
questions_table_name         = "Questions"
answers_table_name           = "Answers"
breakdown_table_name         = "Breakdown"
jira_integrations_table_name = "JiraIntegrations"
jira_mappings_table_name     = "JiraMappings"

jira_epic_name_field = "customfield_10011"
jira_epic_link_field = "customfield_10014"

log_retention_days = 14
ecs_task_cpu       = "256"
ecs_task_memory    = "512"

backend_image_tag     = "latest"
api_ecs_desired_count = 0

bedrock_model_id    = "google.gemma-3-4b-it"
bedrock_use_stub    = "false"
bedrock_max_tokens  = 8192
bedrock_temperature = 0.25

