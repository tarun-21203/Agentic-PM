# CSCI5409 Final Term Project Report

## 1) Project Introduction and Context

### What the project is

This project is an **AI-assisted project planning and delivery platform**. It helps a user move from an unstructured requirements document to a structured, actionable delivery plan and synced Jira work items.

### What it is supposed to do

The application supports this workflow:

1. User signs in.
2. User creates a project and uploads a requirements document.
3. System generates clarification questions.
4. User answers questions and marks the project clarified.
5. System generates technical documentation.
6. System generates a task breakdown.
7. System syncs work into Jira (project + tickets + updates).

### Intended users

- Student teams and small engineering teams
- Technical project leads
- Product/engineering coordinators who need fast project decomposition

### Performance / quality targets used for design decisions

- **Interactive API UX target**: most non-LLM requests under ~1 second perceived response.
- **AI operations target**: generation endpoints may take longer but should return clear progress/error feedback.
- **Reliability target**: no silent failures; backend should log actionable reasons for operational issues.
- **Security target**: authenticated access, per-user data isolation, and no plaintext external-service credentials in app storage.

These targets drove choices such as **Amazon Cognito** JWT authentication, detailed backend error logging, and deterministic **Amazon Elastic Container Service (ECS)** image rollouts.

## 2) AWS Service Category Requirements and Service Selection Rationale

### Required category picks (aligned to course service menu)

| Category | Required | Selected service(s) | Why this fits this project |
|---|---:|---|---|
| Compute | Pick 2 | **Amazon Elastic Container Service (ECS)** + **AWS Fargate** | Managed container orchestration + serverless container runtime for Spring Boot API deployment |
| Storage | Pick 1 | **Amazon Simple Storage Service (S3)** | Best fit for uploaded requirement files and generated document artifacts |
| Networking and Content Delivery | Pick 1 | **Amazon CloudFront** | Global frontend delivery and edge caching; integrates cleanly with Amazon Simple Storage Service (S3) and Elastic Load Balancing origins |
| Database | Pick 1 | **Amazon DynamoDB** | Good fit for project-centric key-value/document access patterns |
| Management and Governance | Pick 1 | **Amazon CloudWatch** | Central logs/metrics for backend runtime visibility and troubleshooting |

Additional (not part of required category count, but used): **Elastic Load Balancing (ALB)**, **Amazon Elastic Container Registry (ECR)**, **Amazon Cognito**, **AWS Secrets Manager**.

### Alternatives considered and why selected services were chosen

#### Compute (picked two): Amazon Elastic Container Service (ECS) + AWS Fargate

- **Chosen: Amazon Elastic Container Service (ECS) + AWS Fargate**
  - Amazon Elastic Container Service (ECS) provides service/task orchestration.
  - AWS Fargate removes server management and matches course timeline constraints.
  - Works well for always-available Spring Boot API with predictable behavior.
- **Alternative from menu: AWS Lambda**
  - Pro: pay-per-use and scale-to-zero.
  - Con: less suitable for current monolithic API runtime style and dependency profile.
- **Alternative from menu: Amazon EC2**
  - Pro: full host/runtime control.
  - Con: higher operational overhead (patching, scaling, capacity planning).

#### Storage (picked one): Amazon Simple Storage Service (S3)

- **Chosen: Amazon Simple Storage Service (S3) (from storage menu)**
  - Natural fit for uploaded files and generated markdown artifacts.
  - Durable, cheap object storage with straightforward access patterns.
- **Alternative from menu: Amazon Elastic File System (EFS)**
  - Better for shared filesystem semantics, not needed here.

#### Networking and Content Delivery (picked one): Amazon CloudFront

- **Chosen: Amazon CloudFront (from networking menu)**
  - Improves delivery performance for frontend assets.
  - Acts as a stable edge entry for web clients.
- **Alternative from menu: Amazon API Gateway**
  - Useful for API-only serverless patterns.
  - Not selected as primary networking choice because backend already uses Elastic Load Balancing + Amazon Elastic Container Service (ECS) path.
- **Alternative from menu: Amazon Route 53**
  - DNS management service, but not sufficient alone for CDN/content acceleration.

#### Database (picked one): Amazon DynamoDB

- **Chosen: Amazon DynamoDB (from database menu)**
  - Serverless, low-ops, and suitable for current entity and access patterns.
- **Alternative from menu: Amazon Relational Database Service (RDS)**
  - Strong relational querying and joins.
  - Adds schema migration and DB-ops complexity that is unnecessary at current scale.

#### Management and Governance (picked one): Amazon CloudWatch

- **Chosen: Amazon CloudWatch (from management menu)**
  - Gives immediate operational visibility for backend flows and integration failures.
- **Alternative from menu: AWS CloudTrail**
  - Better for API audit/event history, but not a primary application runtime monitoring tool.

## 3) Final Architecture

### 3.1 Architecture Diagram

The architecture diagram source file is **`assets/agentic-pm-architecture.drawio`**. Open it in **[diagrams.net](https://app.diagrams.net/)** (File → Open from → Device), then export **PNG** or **PDF** for submission (e.g. `File → Export as → PNG`). For the report, embed the exported image where your course requires the figure.

### 3.2 How components fit together

1. Frontend is delivered through Amazon CloudFront.
2. User authenticates with Amazon Cognito from the React app.
3. Frontend sends JWT in `Authorization` header to backend API.
4. Spring Boot API on Amazon Elastic Container Service (ECS) validates JWT, resolves user identity, and processes requests.
5. API persists app state in Amazon DynamoDB, files in Amazon Simple Storage Service (S3), secrets in AWS Secrets Manager, and performs Bedrock/Jira integrations.
6. Logs go to Amazon CloudWatch.

### 3.3 Where data is stored

- **Amazon DynamoDB**: Projects, Questions, Answers, Breakdown, JiraIntegrations, JiraMappings
- **Amazon Simple Storage Service (S3)**: Uploaded requirement docs and generated documentation artifacts
- **AWS Secrets Manager**: Jira API tokens
- **Browser local storage**: frontend session token cache (id token), with expiry checks

### 3.4 Languages and coded components

- **Backend**: Java 17 + Spring Boot (controllers, services, integrations, security)
- **Frontend**: TypeScript + React + Material UI (pages, routing, auth UX, error UX)
- **Infrastructure**: Terraform (module-based IaC)
- **Deployment automation**: Bash (`deploy.sh`)

### 3.5 Deployment to cloud

- CI/script flow:
  1. Build backend Docker image.
  2. Push to Amazon Elastic Container Registry (ECR).
  3. Resolve image digest.
  4. Terraform apply with digest-pinned task definition.
  5. Build frontend and upload to Amazon Simple Storage Service (S3).
  6. Trigger Amazon Elastic Container Service (ECS) rollout and Amazon CloudFront invalidation.

### 3.6 Architecture alignment with course patterns

This design is closest to a **containerized 3-tier cloud web architecture** (presentation, API/service, data/integration).  
It is not a pure serverless pattern, because backend compute was intentionally consolidated into Amazon Elastic Container Service (ECS) for runtime consistency and simpler orchestration of multiple API capabilities.

Potentially flawed areas:

- Public-subnet style deployment and simplified networking are acceptable for class delivery but not ideal for production hardening.
- Some synchronous AI/Jira flows could be moved to asynchronous processing for better scalability.

## 4) Data Security Across Layers

### Current security posture

- **Authentication**: Amazon Cognito JWT required for protected API paths.
- **Authorization scope**: backend ties records to token-derived user identity (`sub`) and validates ownership.
- **Secrets**: Jira credentials are stored in AWS Secrets Manager, not in plain DB records.
- **Transport**: Amazon CloudFront + Elastic Load Balancing standard HTTPS path for browser traffic.
- **Storage security**: Amazon Simple Storage Service (S3) with access controls; Amazon DynamoDB access through application role.
- **Operational logging**: detailed error logs without exposing secret values.

### Known vulnerabilities / gaps and mitigation plan

- **Gap**: networking not fully hardened (private subnets + VPC endpoints not fully leveraged).
  - **Plan**: move Amazon Elastic Container Service (ECS) tasks to private subnets, restrict outbound routes, add VPC endpoints where possible.
- **Gap**: frontend token in local storage can be vulnerable to XSS.
  - **Plan**: move to httpOnly secure cookie flow with stricter CSP and token rotation.
- **Gap**: IAM least privilege can be tightened further.
  - **Plan**: narrow per-resource actions and environment-specific roles.

### Security mechanisms and rationale

- **Amazon Cognito JWT (RS256/OIDC)**: standards-based token validation and managed identity lifecycle.
- **Spring Security OAuth2 resource server**: robust server-side validation using issuer metadata.
- **AWS Secrets Manager**: secure secret-at-rest handling and controlled retrieval.
- **Amazon Simple Storage Service (S3) + Amazon DynamoDB managed services**: reduce custom crypto/storage handling burden.
- **Amazon CloudWatch logging with structured errors**: operational security visibility and incident debugging.

## 5) Cost Analysis

Cost analysis is estimated using the current Terraform configuration and the following monthly usage assumptions.

- Number of users: **1,000**
- Auth/login activity (Cognito): **~2,000 events**
- API calls via CloudFront + ALB + ECS: **~80,000 requests**
- AI generation actions (Bedrock-backed endpoints): **~2,000 requests**
- Average generated/stored artifact size: **~0.5 MB**
- S3 document + static content storage: **~2.5 GB total**
- Analytics/operational events logged to CloudWatch: **small-project level (~5 GB ingest/month)**

### 5.1 Up-front costs

There is no major up-front infrastructure cost because the platform uses managed AWS services on a pay-as-you-go model.  
Primary up-front cost is engineering and setup effort (Terraform, backend, frontend, integration hardening).

### 5.2 Running costs (Terraform-aligned estimate)

The following estimate reflects this project's deployed architecture in `us-east-1`:
- API on **Amazon ECS on AWS Fargate** (task size from tfvars: `256 CPU`, `512 MiB memory`)
- **Elastic Load Balancing (Application Load Balancer)**
- **Amazon CloudFront** in front of frontend/API routes
- **Amazon DynamoDB** on-demand tables
- **Amazon S3** for frontend hosting and generated artifacts
- **Amazon Cognito**, **AWS Secrets Manager**, **Amazon ECR**, and **Amazon CloudWatch**
- **Amazon Bedrock** for AI inference (highly variable by model/token volume)

| AWS Services | Usage | Estimated Cost ($) |
|---|---|---:|
| Amazon ECS on AWS Fargate | 1 always-on task (256 CPU / 512 MiB), ~730 hrs/month | 9.00 |
| Elastic Load Balancing (ALB) | ALB hourly + light LCU for project traffic | 18.00 |
| Amazon CloudFront | CDN requests + ~30 GB data transfer | 2.50 |
| Amazon S3 | ~2.5 GB storage + PUT/GET requests | 0.25 |
| Amazon DynamoDB | On-demand reads/writes + low storage footprint | 1.50 |
| Amazon Cognito | ~1,000 MAU (small cohort; often near free tier) | 0.00 |
| AWS Secrets Manager | A few active secrets (e.g., Jira credentials) | 0.80 |
| Amazon ECR | Backend image storage + pulls | 0.20 |
| Amazon CloudWatch | Log ingestion + storage (14-day retention) | 3.00 |
| Amazon Bedrock | ~2,000 AI requests, moderate token usage | 25.00 |
| Data Transfer (non-CDN residual) | Additional outbound transfer | 0.90 |
| **Total Monthly Cost** |  | **61.15** |

> Note: if `api_ecs_desired_count` remains `0`, ECS runtime cost is near zero; once set to `1+` for production availability, ECS + ALB become the primary fixed monthly costs.

### 5.3 Additional cost for real-world production

| Category | Details | Estimated Cost ($) |
|---|---|---:|
| AWS Route 53 | DNS hosted zone + query traffic | 12 upfront, 0.50 monthly |
| AWS WAF | ALB/API protection with managed/basic rules | 7-15 monthly |
| Additional CloudFront usage | Higher global traffic and cache-miss transfer | 0.37+ monthly |
| **Total** |  | **12 upfront, ~7.87-15.87+ monthly** |

### 5.4 Cost optimization

- Keep ECS task size small for baseline workloads and scale only when necessary.
- Add CloudFront caching policies to reduce repeated ALB/API traffic.
- Use DynamoDB on-demand for unpredictable workloads and review for provisioned mode only after stable patterns.
- Apply S3 lifecycle policies for generated artifacts and stale uploads.
- Control Bedrock costs using stricter max-token settings, prompt trimming, and request-level guardrails.
- Reduce CloudWatch retention/verbosity in non-production environments.

### 5.5 Justification

- The architecture intentionally uses ECS/Fargate (instead of all-Lambda) for predictable runtime behavior, easier containerized deployment, and stable integration with Jira and Bedrock-backed flows.
- Bedrock is the largest variable cost; this is acceptable because AI generation is core product value, and token controls are available to keep spend bounded.

## 6) How the Application Would Evolve Next

### Product features

- Team/workspace support (multi-user project collaboration)
- Notifications and approvals for clarification/documentation stages
- Better Jira synchronization status and conflict handling UX
- Version history and diff view for generated documentation/breakdowns

### Architecture/service evolution

- Asynchronous generation with SQS/Step Functions for long AI workflows
- Event-driven status updates to frontend (WebSocket or polling endpoint)
- Private networking hardening (private subnets, endpoint policies)
- Optional OpenSearch analytics over project artifacts

### Engineering maturity improvements

- Broader automated tests (integration + contract + UI flows)
- Blue/green or canary deployment strategy for backend
- Formal SLOs and alerting thresholds for latency/error budgets

## 7) Conclusion

This project delivers an end-to-end, cloud-native Agentic PM platform that combines a React frontend, containerized Spring Boot API services, managed AWS infrastructure, and Bedrock-powered AI workflows into one practical system. The implemented architecture demonstrates clear separation of concerns across presentation, application, and data layers while remaining deployable and maintainable through Terraform.

From a technical perspective, the solution balances functionality and operational realism: Amazon Cognito secures user access, Amazon ECS on AWS Fargate provides predictable backend runtime behavior, Amazon DynamoDB and Amazon S3 support scalable persistence, and Amazon CloudWatch improves observability. Integration with Jira and Bedrock enables high-value automation for project planning artifacts while preserving a path for stronger governance and reliability.

Cost and security evaluations show that the current design is suitable for small-team usage and can evolve incrementally toward production standards. Future work should prioritize private networking hardening, asynchronous processing for long-running AI tasks, tighter IAM least-privilege controls, and broader automated test coverage. Overall, the project meets its objectives and provides a strong foundation for continued feature growth, performance tuning, and enterprise-ready hardening.

