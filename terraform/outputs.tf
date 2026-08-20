output "bucket_name" {
  value = module.documents_s3.bucket_id
}

output "projects_table_name" {
  value = module.dynamodb.projects_table_name
}

output "questions_table_name" {
  value = module.dynamodb.questions_table_name
}

output "answers_table_name" {
  value = module.dynamodb.answers_table_name
}

output "breakdown_table_name" {
  value = module.dynamodb.breakdown_table_name
}

output "jira_integrations_table_name" {
  value = module.dynamodb.jira_integrations_table_name
}

output "jira_mappings_table_name" {
  value = module.dynamodb.jira_mappings_table_name
}

output "frontend_cloudfront_domain" {
  value = module.cloudfront.domain_name
}

output "backend_ecr_repository_url" {
  value = module.ecs_api.backend_ecr_repository_url
}

output "backend_ecs_container_image" {
  value       = module.ecs_api.backend_ecs_container_image
  description = "Backend image URI@digest wired into the ECS task definition"
}

output "frontend_s3_bucket_name" {
  value = module.frontend_s3.bucket_id
}

output "frontend_cloudfront_distribution_id" {
  value = module.cloudfront.distribution_id
}

output "cognito_user_pool_id" {
  value = module.cognito.user_pool_id
}

output "cognito_user_pool_client_id" {
  value = module.cognito.user_pool_client_id
}
