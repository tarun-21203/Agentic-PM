output "alb_dns_name" { value = aws_lb.api.dns_name }
output "alb_arn" { value = aws_lb.api.arn }
output "alb_security_group_id" { value = aws_security_group.alb.id }

output "ecs_cluster_id" { value = aws_ecs_cluster.this.id }
output "backend_ecr_repository_url" { value = aws_ecr_repository.backend.repository_url }

output "backend_ecs_container_image" {
  value       = local.backend_container_image
  description = "Resolved backend image URI (repository@digest) used by the ECS task definition"
}

