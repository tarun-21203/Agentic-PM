resource "aws_dynamodb_table" "projects" {
  name         = var.projects_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "projectId"

  attribute {
    name = "projectId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "userId-createdAt-index"
    hash_key        = "userId"
    range_key       = "createdAt"
    projection_type = "ALL"
  }
}

resource "aws_dynamodb_table" "questions" {
  name         = var.questions_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "questionId"

  attribute {
    name = "questionId"
    type = "S"
  }

  attribute {
    name = "projectId"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "projectId-createdAt-index"
    hash_key        = "projectId"
    range_key       = "createdAt"
    projection_type = "ALL"
  }
}

resource "aws_dynamodb_table" "answers" {
  name         = var.answers_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "projectId"
  range_key    = "questionId"

  attribute {
    name = "projectId"
    type = "S"
  }

  attribute {
    name = "questionId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "breakdown" {
  name         = var.breakdown_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "projectId"
  range_key    = "breakdownVersion"

  attribute {
    name = "projectId"
    type = "S"
  }

  attribute {
    name = "breakdownVersion"
    type = "S"
  }
}

resource "aws_dynamodb_table" "jira_integrations" {
  name         = var.jira_integrations_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"

  attribute {
    name = "userId"
    type = "S"
  }
}

resource "aws_dynamodb_table" "jira_mappings" {
  name         = var.jira_mappings_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "projectId"
  range_key    = "internalId"

  attribute {
    name = "projectId"
    type = "S"
  }

  attribute {
    name = "internalId"
    type = "S"
  }
}

