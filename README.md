# Agentic AI Project Management

An end-to-end project management platform that ingests project documents, stores project metadata, and provides a typed full-stack foundation for agentic workflows.

This repository includes:
- A Spring Boot backend API for project creation, retrieval, and description upload.
- A React frontend for project creation and browsing.
- Terraform IaC for provisioning required AWS resources.

## Project Description

The application is designed to support an agentic product/project-management workflow where users can:
- Create a project.
- Upload a project description file (`PDF`, `DOCX`, `TXT`).
- Persist metadata and file references.
- Build toward AI-driven capabilities (question generation, clarification, execution planning).

Current implementation focuses on ingestion and storage, with architecture prepared for future AI and workflow modules.

## Tech Stack

### Backend
- Java 17
- Spring Boot (REST API)
- Gradle
- AWS SDK integrations (S3 + DynamoDB)

### Frontend
- React
- TypeScript
- Vite
- Material UI

### Cloud & Infrastructure
- AWS S3 (document object storage)
- AWS DynamoDB (project metadata store)
- Terraform (infrastructure provisioning)
- LocalStack (optional local AWS emulation)

## High-Level Architecture

```text
React (Vite + MUI)
      |
      | HTTP /api
      v
Spring Boot API
  - Project endpoints
  - File upload handling
  - Service/repository layers
      |                      |
      v                      v
AWS S3 (files)        AWS DynamoDB (metadata)
```

### Architectural Notes
- Frontend and backend are separated and communicate through typed API contracts.
- Backend separates concerns into API/controller, service/business logic, and persistence/storage integrations.
- Binary document content is stored in S3, while lightweight metadata is stored in DynamoDB for fast retrieval and scalability.
- Infrastructure is declarative through Terraform for repeatable environments.

## Repository Structure

```text
backend/                    # Spring Boot API
  src/main/java/com/agentic/pm/
    project/                # API, domain, repository, service
    config/                 # AWS, storage, CORS and app config
    storage/                # S3 upload/storage integration

frontend/                   # React + TypeScript + Vite + MUI
  src/
    api/                    # Typed API client/services/types
    components/             # Shared UI components/layout
    pages/                  # Project list/create/detail pages

terraform/                  # Infrastructure as Code
  # S3 bucket, DynamoDB table (+ indexes as defined)
```

## Cloud Services

### Amazon S3
Used to store uploaded project description files.
- Handles large/binary documents efficiently.
- Stores objects keyed by project context.
- Keeps storage concerns independent from metadata concerns.

### Amazon DynamoDB
Used to store project metadata and lookup fields.
- Supports low-latency reads/writes.
- Suitable for project-centric access patterns and scale.
- Maintains references to associated S3 objects.

### LocalStack (Optional)
Provides local emulation of AWS services for development and testing without deploying to AWS.

## Deployment and Environment Strategy

The project supports two common workflows:
- **Local development**: frontend + backend locally, optionally with LocalStack.
- **AWS deployment path**: provision resources with Terraform and run backend/frontend against AWS.

## Prerequisites

- JDK 17+
- Node.js 18+
- Terraform 1.0+
- AWS CLI configured (for AWS deployment)
- Optional: [LocalStack](https://localstack.cloud/) for local AWS emulation

## Run the Project Locally

### 1) Provision Infrastructure

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

Optional (shell export of Terraform outputs):

```bash
export BUCKET_NAME=$(terraform output -raw bucket_name)
export PROJECTS_TABLE=$(terraform output -raw projects_table_name)
```

### 2) Start Backend

```bash
cd backend
./gradlew bootRun
```

With local profile / LocalStack endpoints:

```bash
export AWS_DYNAMODB_ENDPOINT=http://localhost:4566
export AWS_S3_ENDPOINT=http://localhost:4566
./gradlew bootRun --args='--spring.profiles.active=local'
```

Backend API base URL: `http://localhost:8080/api`

### 3) Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL: `http://localhost:5173`  
Vite proxies `/api` to `http://localhost:8080`.

## API Endpoints (Current)

- `POST /api/projects` - create a project (`{"name":"My Project"}`)
- `GET /api/projects` - list projects (optional `X-User-Id` header)
- `GET /api/projects/{projectId}` - fetch project details
- `POST /api/projects/{projectId}/description` - upload project description file (multipart field: `file`)

## AWS Deployment Notes

- Use Terraform in `terraform/` to create/update cloud resources.
- Configure backend runtime credentials and region via environment variables or IAM roles.
- Ensure backend environment has access to:
  - S3 bucket for document storage
  - DynamoDB table for project metadata
- Frontend can be deployed independently (for example as static assets behind a CDN), configured to call the deployed backend base URL.

## Roadmap

- AI question generation and clarification workflow (Bedrock integration path).
- Clarified/unclarified project state transitions.
- Authentication and authorization (Cognito-based flow).
- Production hardening (observability, CI/CD, environment promotion).
