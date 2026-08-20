data "aws_caller_identity" "current" {}

resource "aws_iam_policy" "ecs_api_app_access" {
  name = "${var.name_prefix}-ecs-api-app-access"
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "dynamodb:GetItem",
          "dynamodb:PutItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query",
          "dynamodb:BatchWriteItem"
        ],
        Resource = [
          var.projects_table_arn,
          "${var.projects_table_arn}/index/*",
          var.questions_table_arn,
          "${var.questions_table_arn}/index/*",
          var.answers_table_arn,
          var.breakdown_table_arn,
          var.jira_integrations_table_arn,
          var.jira_mappings_table_arn
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ],
        Resource = ["${var.documents_bucket_arn}/*"]
      },
      {
        Effect = "Allow",
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:CreateSecret",
          "secretsmanager:PutSecretValue"
        ],
        Resource = [
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:${var.name_prefix}/jira/*"
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "bedrock:InvokeModel",
          "bedrock:InvokeModelWithResponseStream"
        ],
        Resource = [
          "arn:aws:bedrock:${var.aws_region}::foundation-model/*"
        ]
      }
    ]
  })
}

resource "aws_ecr_repository" "backend" {
  name = "${var.name_prefix}-backend"

  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

data "aws_ecr_image" "backend" {
  count           = trimspace(var.backend_image_digest) == "" ? 1 : 0
  repository_name = aws_ecr_repository.backend.name
  image_tag       = var.backend_image_tag
}

locals {
  backend_image_digest_effective = trimspace(var.backend_image_digest) != "" ? (
    startswith(trimspace(var.backend_image_digest), "sha256:") ? trimspace(var.backend_image_digest) : "sha256:${trimspace(var.backend_image_digest)}"
  ) : data.aws_ecr_image.backend[0].image_digest

  backend_container_image = "${aws_ecr_repository.backend.repository_url}@${local.backend_image_digest_effective}"
}

resource "aws_cloudwatch_log_group" "api_ecs" {
  name              = "/ecs/${var.name_prefix}-api"
  retention_in_days = var.log_retention_days
}

resource "aws_iam_role" "ecs_api_task_execution" {
  name = "${var.name_prefix}-ecs-api-task-exec"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action    = "sts:AssumeRole",
        Effect    = "Allow",
        Principal = { Service = "ecs-tasks.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_api_task_execution" {
  role       = aws_iam_role.ecs_api_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_api_task_role" {
  name = "${var.name_prefix}-ecs-api-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action    = "sts:AssumeRole",
        Effect    = "Allow",
        Principal = { Service = "ecs-tasks.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_api_app_access" {
  role       = aws_iam_role.ecs_api_task_role.name
  policy_arn = aws_iam_policy.ecs_api_app_access.arn
}

resource "aws_security_group" "alb" {
  name   = "${var.name_prefix}-alb-sg"
  vpc_id = var.vpc_id

  ingress {
    description = "Allow HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ecs_cluster" "this" {
  name = "${var.name_prefix}-cluster"
}

resource "aws_lb" "api" {
  name               = "${var.name_prefix}-alb"
  load_balancer_type = "application"
  subnets            = var.subnet_ids
  security_groups    = [aws_security_group.alb.id]
}

resource "aws_lb_target_group" "api" {
  name        = "${var.name_prefix}-api-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/api/health"
    protocol            = "HTTP"
    matcher             = "200-399"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

resource "aws_security_group" "api_tasks" {
  name   = "${var.name_prefix}-api-ecs-sg"
  vpc_id = var.vpc_id

  ingress {
    description     = "Allow ALB to reach backend container"
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ecs_task_definition" "api" {
  family                   = "${var.name_prefix}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.ecs_task_cpu
  memory                   = var.ecs_task_memory
  execution_role_arn       = aws_iam_role.ecs_api_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_api_task_role.arn

  container_definitions = jsonencode([
    {
      name = "api"
      # Pin to digest so ECS always pulls the intended image; optional var overrides ECR data lookup (CI-friendly).
      image     = local.backend_container_image
      essential = true
      portMappings = [
        {
          containerPort = 80
          hostPort      = 80
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs",
        options = {
          awslogs-group         = aws_cloudwatch_log_group.api_ecs.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
      environment = [
        { name = "BUCKET_NAME", value = var.documents_bucket_id },
        { name = "PROJECTS_TABLE", value = var.projects_table_name },
        { name = "QUESTIONS_TABLE", value = var.questions_table_name },
        { name = "ANSWERS_TABLE", value = var.answers_table_name },
        { name = "BREAKDOWN_TABLE", value = var.breakdown_table_name },
        { name = "JIRA_INTEGRATIONS_TABLE", value = var.jira_integrations_table_name },
        { name = "JIRA_MAPPINGS_TABLE", value = var.jira_mappings_table_name },
        { name = "JIRA_EPIC_NAME_FIELD", value = var.jira_epic_name_field },
        { name = "JIRA_EPIC_LINK_FIELD", value = var.jira_epic_link_field },
        { name = "BEDROCK_MODEL_ID", value = var.bedrock_model_id },
        { name = "BEDROCK_USE_STUB", value = var.bedrock_use_stub },
        { name = "BEDROCK_MAX_TOKENS", value = tostring(var.bedrock_max_tokens) },
        { name = "BEDROCK_TEMPERATURE", value = tostring(var.bedrock_temperature) },
        {
          name  = "COGNITO_ISSUER_URI"
          value = "https://cognito-idp.${var.aws_region}.amazonaws.com/${var.cognito_user_pool_id}"
        }
      ]
    }
  ])
}

resource "aws_ecs_service" "api" {
  name            = "${var.name_prefix}-api-svc"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.api_ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.subnet_ids
    security_groups  = [aws_security_group.api_tasks.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "api"
    container_port   = 80
  }
}

