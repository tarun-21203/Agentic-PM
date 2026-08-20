output "projects_table_name" { value = aws_dynamodb_table.projects.name }
output "questions_table_name" { value = aws_dynamodb_table.questions.name }
output "answers_table_name" { value = aws_dynamodb_table.answers.name }
output "breakdown_table_name" { value = aws_dynamodb_table.breakdown.name }
output "jira_integrations_table_name" { value = aws_dynamodb_table.jira_integrations.name }
output "jira_mappings_table_name" { value = aws_dynamodb_table.jira_mappings.name }

output "projects_table_arn" { value = aws_dynamodb_table.projects.arn }
output "questions_table_arn" { value = aws_dynamodb_table.questions.arn }
output "answers_table_arn" { value = aws_dynamodb_table.answers.arn }
output "breakdown_table_arn" { value = aws_dynamodb_table.breakdown.arn }
output "jira_integrations_table_arn" { value = aws_dynamodb_table.jira_integrations.arn }
output "jira_mappings_table_arn" { value = aws_dynamodb_table.jira_mappings.arn }

