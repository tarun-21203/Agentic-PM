module "network" {
  source      = "./modules/network"
  name_prefix = var.name_prefix
}

module "documents_s3" {
  source                                = "./modules/s3-documents"
  bucket_name                           = var.bucket_name
  documents_bucket_cors_allowed_origins = var.documents_bucket_cors_allowed_origins
}

module "frontend_s3" {
  source               = "./modules/s3-frontend"
  name_prefix          = var.name_prefix
  frontend_bucket_name = var.frontend_bucket_name
}

module "dynamodb" {
  source                       = "./modules/dynamodb"
  projects_table_name          = var.projects_table_name
  questions_table_name         = var.questions_table_name
  answers_table_name           = var.answers_table_name
  breakdown_table_name         = var.breakdown_table_name
  jira_integrations_table_name = var.jira_integrations_table_name
  jira_mappings_table_name     = var.jira_mappings_table_name
}

module "cognito" {
  source      = "./modules/cognito"
  name_prefix = var.name_prefix
}

module "ecs_api" {
  source             = "./modules/ecs-api"
  aws_region         = var.aws_region
  name_prefix        = var.name_prefix
  log_retention_days = var.log_retention_days

  vpc_id     = module.network.vpc_id
  subnet_ids = module.network.public_subnet_ids

  documents_bucket_id  = module.documents_s3.bucket_id
  documents_bucket_arn = module.documents_s3.bucket_arn

  projects_table_arn          = module.dynamodb.projects_table_arn
  questions_table_arn         = module.dynamodb.questions_table_arn
  answers_table_arn           = module.dynamodb.answers_table_arn
  breakdown_table_arn         = module.dynamodb.breakdown_table_arn
  jira_integrations_table_arn = module.dynamodb.jira_integrations_table_arn
  jira_mappings_table_arn     = module.dynamodb.jira_mappings_table_arn

  projects_table_name          = module.dynamodb.projects_table_name
  questions_table_name         = module.dynamodb.questions_table_name
  answers_table_name           = module.dynamodb.answers_table_name
  breakdown_table_name         = module.dynamodb.breakdown_table_name
  jira_integrations_table_name = module.dynamodb.jira_integrations_table_name
  jira_mappings_table_name     = module.dynamodb.jira_mappings_table_name

  jira_epic_name_field = var.jira_epic_name_field
  jira_epic_link_field = var.jira_epic_link_field

  backend_image_tag     = var.backend_image_tag
  backend_image_digest  = var.backend_image_digest
  api_ecs_desired_count = var.api_ecs_desired_count

  ecs_task_cpu    = var.ecs_task_cpu
  ecs_task_memory = var.ecs_task_memory

  bedrock_model_id    = var.bedrock_model_id
  bedrock_use_stub    = var.bedrock_use_stub
  bedrock_max_tokens  = var.bedrock_max_tokens
  bedrock_temperature = var.bedrock_temperature

  cognito_user_pool_id = module.cognito.user_pool_id
}

module "cloudfront" {
  source                               = "./modules/cloudfront"
  name_prefix                          = var.name_prefix
  frontend_bucket_regional_domain_name = module.frontend_s3.bucket_regional_domain_name
  frontend_bucket_id                   = module.frontend_s3.bucket_id
  api_alb_dns_name                     = module.ecs_api.alb_dns_name
}

