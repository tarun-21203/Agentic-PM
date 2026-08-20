variable "aws_region" { type = string }
variable "name_prefix" { type = string }
variable "log_retention_days" { type = number }

variable "vpc_id" { type = string }
variable "subnet_ids" { type = list(string) }

variable "documents_bucket_id" { type = string }
variable "documents_bucket_arn" { type = string }

variable "projects_table_arn" { type = string }
variable "questions_table_arn" { type = string }
variable "answers_table_arn" { type = string }
variable "breakdown_table_arn" { type = string }
variable "jira_integrations_table_arn" { type = string }
variable "jira_mappings_table_arn" { type = string }

variable "jira_epic_name_field" { type = string }
variable "jira_epic_link_field" { type = string }

variable "backend_image_tag" { type = string }

# If set (e.g. sha256:abc... from `aws ecr describe-images`), task definition uses this digest and skips
# the aws_ecr_image data source — avoids stale refresh and guarantees the revision matches the image you just pushed.
variable "backend_image_digest" {
  type        = string
  default     = ""
  description = "Optional. Full image digest for the backend container; when empty, digest is read from ECR for backend_image_tag."
}

variable "api_ecs_desired_count" { type = number }

variable "ecs_task_cpu" { type = string }
variable "ecs_task_memory" { type = string }

variable "projects_table_name" { type = string }
variable "questions_table_name" { type = string }
variable "answers_table_name" { type = string }
variable "breakdown_table_name" { type = string }
variable "jira_integrations_table_name" { type = string }
variable "jira_mappings_table_name" { type = string }

variable "bedrock_model_id" { type = string }
variable "bedrock_use_stub" { type = string }
variable "bedrock_max_tokens" { type = number }
variable "bedrock_temperature" { type = number }

variable "cognito_user_pool_id" { type = string }

