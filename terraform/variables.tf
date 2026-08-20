variable "aws_region" {
  description = "AWS region"
  type        = string
}

variable "tags" {
  description = "Tags applied to all resources via the AWS provider"
  type        = map(string)
}

variable "bucket_name" {
  description = "S3 bucket for documents"
  type        = string
}

variable "documents_bucket_cors_allowed_origins" {
  description = "Allowed origins for documents bucket CORS"
  type        = list(string)
}

variable "projects_table_name" {
  description = "DynamoDB Projects table name"
  type        = string
}

variable "questions_table_name" {
  description = "DynamoDB Questions table name"
  type        = string
}

variable "answers_table_name" {
  description = "DynamoDB Answers table name"
  type        = string
}

variable "breakdown_table_name" {
  description = "DynamoDB Breakdown table name"
  type        = string
}

variable "jira_integrations_table_name" {
  description = "DynamoDB JiraIntegrations table name"
  type        = string
}

variable "jira_mappings_table_name" {
  description = "DynamoDB JiraMappings table name"
  type        = string
}

variable "name_prefix" {
  description = "Prefix for named resources"
  type        = string
}

variable "jira_epic_name_field" {
  description = "JIRA Epic Name custom field id"
  type        = string
}

variable "jira_epic_link_field" {
  description = "JIRA Epic Link custom field id"
  type        = string
}

variable "frontend_bucket_name" {
  description = "S3 bucket to host the frontend build"
  type        = string
}

variable "log_retention_days" {
  description = "CloudWatch log retention for Lambda log groups"
  type        = number
}

variable "ecs_task_cpu" {
  description = "Fargate task CPU units (as string)"
  type        = string
}

variable "ecs_task_memory" {
  description = "Fargate task memory (MiB, as string)"
  type        = string
}

variable "backend_image_tag" {
  description = "Docker image tag for the backend API (stored in ECR)"
  type        = string
}

variable "backend_image_digest" {
  description = <<-EOT
    Optional full digest (sha256:...) for the backend image after push. When set, the ECS task uses this digest and Terraform does not read ECR at plan time for that tag — use in CI so every deploy matches the image you just pushed.
    Example (after push): aws ecr describe-images --repository-name <prefix>-backend --image-ids imageTag=latest --query 'imageDetails[0].imageDigest' --output text
  EOT
  type        = string
  default     = ""
}

variable "api_ecs_desired_count" {
  description = "Number of running Fargate tasks for the backend API"
  type        = number
}

variable "bedrock_model_id" {
  description = "Amazon Bedrock model ID for Converse (use a Google Gemma/Gemini model ID). Use us.* inference profile IDs in supported regions if required."
  type        = string
}

variable "bedrock_use_stub" {
  description = "Set true only for offline testing without Bedrock"
  type        = string
}

variable "bedrock_max_tokens" {
  description = "Max output tokens for Bedrock Converse"
  type        = number
}

variable "bedrock_temperature" {
  description = "Sampling temperature for Bedrock Converse"
  type        = number
}
