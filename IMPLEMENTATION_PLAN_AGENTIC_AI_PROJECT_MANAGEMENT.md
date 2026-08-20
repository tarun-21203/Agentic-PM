# Implementation Plan: Agentic AI Project Management with JIRA Integration

**Document version:** 2.0  
**Date:** March 16, 2025  
**Status:** Final — Clarifications incorporated; see *TECHNICAL_DOCUMENTATION.md* for full spec.

---

## Part 1: Clarification Questions — Answered

### 1. Project description source and upload

- **Q1.1:** Where does the “project description” come from?
  - [ ] User pastes text in the UI  
  - [*] User uploads a file (PDF, DOCX, TXT, etc.)  
  - [ ] Fetched from an existing system (e.g. Confluence, JIRA Epic description)  
  - [ ] Other (please specify): _______________

- **Q1.2:** Where should the generated “understanding / technical documentation” be uploaded?
  - [ ] JIRA Confluence space (with project name)  
  - [ ] As attachment/link on a JIRA Epic or Project  
  - [ ] Internal storage (e.g. S3) with link stored in our DB and optionally in JIRA  
  - [*] Other: Create a frontend for it

- **Q1.3:** What is the “project name” — a user-provided name, JIRA project key/name, or both? 
User provided name

### 2. Agentic AI and workflow

- **Q2.1:** When should the system create JIRA tickets?
  - [ ] Only after “all ambiguities are cleared” (human reviews questions and confirms)  
  - [*] Option to create draft tickets before clarification, then update after  
  - [ ] Other: _______________

- **Q2.2:** Who answers the “questions related to the project description”?
  - [*] Same user who provided the description (in-app Q&A)  
  - [ ] External stakeholder (e.g. via link or email)  
  - [ ] Both (configurable)

- **Q2.3:** Which LLM/AI provider should be used?
  - [*] AWS Bedrock (recommended for AWS-only)  
  - [ ] Other (e.g. OpenAI) — if so, where should it run?: _______________

### 3. JIRA integration

- **Q3.1:** JIRA environment?
  - [*] JIRA Cloud  
  - [ ] JIRA Server / Data Center  

- **Q3.2:** Which issue types should the system create?
  - [*] Epic → Stories/Tasks → Subtasks  
  - [ ] Only Tasks and Subtasks (no Epic)  
  - [ ] Custom hierarchy (please specify): _______________

- **Q3.3:** Should tickets be created in a single fixed JIRA project, or should the user select the project (and optionally board)?
Create a new project when user create project in frontend and give the description
### 4. Users and access

- **Q4.1:** Who are the primary users?
  - [ ] Project managers only  
  - [*] PMs + developers / team leads  
  - [ ] Multi-tenant (multiple organizations/teams)

- **Q4.2:** Do you need SSO / identity integration (e.g. SAML, Cognito with enterprise IdP), or is email/password (Cognito user pool) sufficient for v1?
Cognito email password is fine
### 5. AWS and technical preferences

- **Q5.1:** For “two compute services,” do you have a preference?
  - Suggested default: **AWS Lambda** (API + async jobs) + **Amazon ECS Fargate** or **App Runner** (long-running agent/orchestration if needed).  
  - Alternative: Lambda + EC2, or two Lambda “layers” (e.g. API vs. workers).  
  - Your preference: _Suggested______________

- **Q5.2:** Database preference for application data (users, project metadata, doc versions, Q&A state)?
  - Suggested: **Amazon RDS (PostgreSQL)** or **Amazon DynamoDB**.  
  - Your choice: _DynamoDB______________

- **Q5.3:** For “management and governance,” which is mandatory for you?
  - Suggested: **AWS CloudWatch** (logging, metrics, alarms) + **AWS Config** or **AWS Systems Manager**.  
  - Other (e.g. Control Tower, Organizations): _______________
Cloudwatch
### 6. Scope and timeline

- **Q6.1:** Is this a greenfield project (no existing codebase), or do you have an existing app to extend?
new applicatiopn
- **Q6.2:** Any hard deadline or target phase (e.g. MVP in 3 months)?
This week
---

## Part 2: Confirmed Assumptions (From Your Answers)

| Area | Assumption |
|------|------------|
| Project description | User **uploads a file** (PDF, DOCX, TXT, etc.) as primary input. |
| Project name | **User-provided name** when creating the project. |
| Documentation | Generated technical doc is **displayed in the frontend** (stored in S3, served/viewed in-app). |
| Workflow | **Draft JIRA tickets** can be created before clarification; **update tickets** after ambiguities are cleared. Same user answers questions in-app. |
| AI | **AWS Bedrock** for question generation, doc generation, task breakdown, and technical descriptions. |
| JIRA | **JIRA Cloud**. Create a **new JIRA project** when the user creates a project in the frontend (with description). Issue hierarchy: **Epic → Stories/Tasks → Subtasks**. |
| Users | PMs + developers/team leads; **Cognito email/password** for v1. |
| Compute | **ECS Fargate** (Java 17 API) + **Lambda** (event-driven workers). |
| Database | **DynamoDB** for app data (projects, questions, answers, breakdown, JIRA mapping). |
| Storage | **S3** for uploaded description files and generated documentation. |
| Networking | **CloudFront** + API Gateway / ALB. |
| Management & governance | **CloudWatch** (logs, metrics, alarms). |
| **Backend** | **Java 17** with **Gradle** (e.g. Spring Boot); REST API and business logic. |
| **Frontend** | **React** (TypeScript, React Router, MUI, typed service layer). |
| **Infrastructure** | **Terraform** for all AWS provisioning (VPC, S3, DynamoDB, ECS, Lambda, API Gateway, CloudFront, Cognito, CloudWatch). |
| Scope | **New application** (greenfield). Target: **this week** (MVP phasing recommended; see Technical Documentation). |

---

## Part 3: High-Level Architecture (AWS)

### 3.1 Mandatory AWS Services Mapping

| Category | Service | Role |
|----------|---------|------|
| **Compute 1** | **Amazon ECS (Fargate)** | **Java 17** backend (Gradle/Spring Boot): REST API, Bedrock, JIRA API, S3. |
| **Compute 2** | **AWS Lambda** | Event-driven jobs (async doc generation, bulk JIRA sync); Java 17 or Node. |
| **Database** | **Amazon DynamoDB** | Users, projects, project metadata, Q&A state, task breakdown, JIRA issue mapping, S3 doc keys. |
| **Storage** | **Amazon S3** | Uploaded project description files; generated technical documentation (PDF/HTML); versioning. |
| **Networking & Content Delivery** | **Amazon CloudFront** | CDN for frontend; optional caching for API. Plus **VPC**, **ALB** or **API Gateway** for routing. |
| **Management & Governance** | **Amazon CloudWatch** | Logs, metrics, alarms, dashboards; optional **AWS Config** for governance. |

### 3.2 Conceptual Architecture Diagram (Text)

```
[User] → [CloudFront] → [React SPA]
                ↓
         [API Gateway / ALB]
                ↓
    ┌──────────┴──────────┐
    ↓                     ↓
[ECS: Java 17 API]    [Lambda: async workers]
    |                     |
    ├─ Bedrock (LLM)      ├─ Bedrock (doc gen, task breakdown)
    ├─ DynamoDB           ├─ S3 (read/write docs)
    ├─ S3 (presigned)     ├─ JIRA API (create project, Epic/Tasks/Subtasks)
    └─ JIRA API
```
*Backend: Java 17 (Gradle). Infra: Terraform.*

---

## Part 4: Feature Breakdown and Suggested JIRA Structure

Epic and tasks below are written so they can be copied into JIRA. **Hours are estimates** and should be adjusted after your answers to Part 1.

---

### EPIC: Agentic AI Project Management – Core Platform

**Description:** Build an agentic AI system that ingests a project description, produces clarifying questions, generates technical documentation after ambiguities are cleared, and creates JIRA Epic/Tasks/Subtasks with technical descriptions and hour estimates.

**Acceptance criteria:**  
- User can submit a project description (paste or file).  
- System generates questions; user answers; system marks “ambiguities cleared.”  
- System generates technical documentation and stores it under project name.  
- System creates JIRA hierarchy (Epic → Tasks → Subtasks) with descriptions and hours.

**Estimated hours (Epic):** 320–400 (sum of tasks below).

---

### TASK 1: Project ingestion and storage

**Technical description:**  
Implement upload and storage of project descriptions. **Primary input: file upload** (PDF, DOCX, TXT). Validate file type and size; store raw content in S3 with key `projects/{projectId}/inputs/{filename}`. Persist metadata in **DynamoDB**: project name (user-provided), userId, timestamps, S3 keys, status (draft / in_review / clarified). Expose `POST /projects` (create project with name), `POST /projects/{id}/description` (file upload). Use presigned URLs for uploads from frontend.  
**Subtasks:**  
1.1 Design S3 bucket structure and DynamoDB tables (Projects, ProjectInputs) — 4h  
1.2 Implement Java 17 (Gradle) API for project CRUD and description upload — 8h  
1.3 Implement file parsing (text extraction from PDF/DOCX) — 8h  
1.4 Add validation, error handling, and unit tests — 4h  

**Estimated hours (task):** 24

---

### TASK 2: AI-powered question generation

**Technical description:**  
Use AWS Bedrock (e.g. Google Gemma/Gemini model) to analyze the project description and generate clarifying questions. Store questions in **DynamoDB** (Questions table, GSI by projectId); support versioning if description is updated. Expose `POST /projects/{id}/generate-questions` and `GET /projects/{id}/questions`. Return structured JSON (questionId, text, category optional).  
**Subtasks:**  
2.1 Define Bedrock prompt and response schema for question generation — 4h  
2.2 Implement Java service that calls Bedrock and persists questions to DynamoDB — 6h  
2.3 API and integration tests — 4h  

**Estimated hours (task):** 14

---

### TASK 3: Q&A collection and “ambiguities cleared” state

**Technical description:**  
Allow the **same user** (in-app) to submit answers to generated questions. Store answers in **DynamoDB** (Answers table or item collection: projectId, questionId, answer text, updatedAt). Provide endpoint to mark project as “ambiguities cleared” when user confirms. Expose `GET /projects/{id}/questions`, `PUT /projects/{id}/questions/{qid}/answer`, `POST /projects/{id}/clarify-complete`. Allow **draft** JIRA ticket creation before clarification; **update** tickets after status is clarified.  
**Subtasks:**  
3.1 DynamoDB schema for answers and project status — 2h  
3.2 Java API handlers and routes — 6h  
3.3 Frontend: Q&A UI and “Confirm clarified” action — 8h  

**Estimated hours (task):** 16

---

### TASK 4: Technical documentation generation and upload

**Technical description:**  
When “ambiguities cleared” (or on demand), call Bedrock with project description + Q&A to generate technical documentation (markdown/HTML). Upload to S3 under `projects/{projectId}/docs/` with project name in filename. Save doc version and S3 key in **DynamoDB**. **Frontend** will fetch and display this doc (Task 8). Expose `POST /projects/{id}/generate-documentation` and `GET /projects/{id}/documentation` (URL or signed link for frontend).  
**Subtasks:**  
4.1 Bedrock prompt and template for technical doc — 6h  
4.2 Java service (or Lambda): generate doc, upload to S3, update DynamoDB — 8h  
4.3 API for frontend to retrieve doc (presigned URL or proxy) — 4h  
4.4 Tests and error handling — 4h  

**Estimated hours (task):** 22

---

### TASK 5: Task breakdown and hour estimation (AI)

**Technical description:**  
Use Bedrock to break the project into tasks and subtasks from the technical documentation. Output structured JSON: tasks with title, technical description, subtasks (title, description), and **estimated hours**. Store breakdown in **DynamoDB** (Breakdown table or single item per project). Expose `POST /projects/{id}/generate-breakdown` and `GET /projects/{id}/breakdown`. Support **draft** JIRA creation from this breakdown; **update** JIRA issues after clarification.  
**Subtasks:**  
5.1 Define JSON schema and Bedrock prompt for task/subtask + hours — 6h  
5.2 Java service (or Lambda): call Bedrock, parse, validate, persist to DynamoDB — 8h  
5.3 API and validation tests — 4h  

**Estimated hours (task):** 18

---

### TASK 6: JIRA integration – connection and auth

**Technical description:**  
Support **JIRA Cloud** REST API. Implement API token auth (or OAuth 2.0); store credentials in **AWS Secrets Manager**; reference in **DynamoDB** (user or project-scoped). Provide “Connect JIRA” in UI: user enters site (e.g. tenant.atlassian.net) and API token; save per-user in DynamoDB. Expose `POST /integrations/jira/connect` and `GET /integrations/jira/status`. Validate connection with a simple JIRA API call.  
**Subtasks:**  
6.1 Design credential storage (Secrets Manager) and DynamoDB schema — 4h  
6.2 Implement JIRA auth flow in Java (token or OAuth) — 8h  
6.3 API and “test connection” — 4h  

**Estimated hours (task):** 16

---

### TASK 7: JIRA project creation and ticket creation (Epic, Tasks, Subtasks)

**Technical description:**  
**Create a new JIRA project** when the user creates a project in the frontend (with name and description). Use JIRA Cloud REST API for project creation (if permitted by Atlassian; otherwise use project template and document project-key convention). Then implement creation of JIRA issues from the stored breakdown: one **Epic** per project; for each task in the breakdown, create a **Story/Task**; for each subtask, create a **Sub-task** under the parent. Populate summary, description (AI technical description), and “estimated hours” (custom field or description). Support **draft** creation (before clarification) and **update** existing JIRA issues after “ambiguities cleared.” Store mapping (internal taskId → JIRA issue key) in **DynamoDB**. Expose `POST /projects/{id}/create-jira-project`, `POST /projects/{id}/create-jira-tickets`, `POST /projects/{id}/update-jira-tickets`. Idempotency and partial-failure handling.  
**Subtasks:**  
7.1 JIRA API: create project (or template flow) and obtain project key — 6h  
7.2 JIRA API client (create Epic, Story/Task, Sub-task, link) — 8h  
7.3 Map breakdown to JIRA issue types and custom fields (hours) — 6h  
7.4 Java orchestration, idempotency, DynamoDB mapping, draft vs update — 6h  
7.5 Error handling and manual retry path — 4h  

**Estimated hours (task):** 30

---

### TASK 8: Frontend – project list, detail, and workflow

**Technical description:**  
Build **React SPA** (React Router, MUI) with: project list; create project with **user-provided name** and **file upload** (description); project detail (description, questions, answers, “clarified” state); **technical documentation viewer** (fetch from API and display in frontend — primary way to view generated doc); task breakdown view with hours; “Create JIRA project,” “Create draft tickets,” “Update tickets after clarify” actions. Typed service layer for all API calls. Responsive layout and loading/error states.  
**Subtasks:**  
8.1 Project list and create project (name + file upload) — 8h  
8.2 Project detail: description, questions, answers, clarify — 12h  
8.3 **Technical doc viewer** (frontend) and breakdown view — 6h  
8.4 “Generate questions,” “Generate doc,” “Generate breakdown,” “Create JIRA project/tickets,” “Update tickets” — 6h  

**Estimated hours (task):** 32

---

### TASK 9: AWS infrastructure and networking (Terraform)

**Technical description:**  
Provision and secure AWS infrastructure using **Terraform**: VPC, private subnets for **ECS (Java 17 backend)**; Lambda for async workers if used; S3 bucket(s) with IAM and encryption; **DynamoDB** tables (Projects, Questions, Answers, Breakdown, JiraMapping, Users/Integrations); API Gateway or **ALB** for ECS-backed API; **CloudFront** for frontend and optional API caching. All resources defined in Terraform (modules for network, compute, data, frontend).  
**Subtasks:**  
9.1 VPC, subnets, security groups — 6h  
9.2 S3 bucket(s), IAM, encryption — 4h  
9.3 DynamoDB tables and GSIs — 6h  
9.4 API Gateway / ALB and ECS (Java) service integration — 6h  
9.5 CloudFront distribution — 4h  

**Estimated hours (task):** 26

---

### TASK 10: Management, governance, and observability

**Technical description:**  
Configure **CloudWatch** (mandatory): log groups for Lambda and ECS; metrics (invocations, errors, latency); alarms for error rate and latency; dashboard. Ensure logging does not capture PII or secrets.  
**Subtasks:**  
10.1 CloudWatch log groups and retention — 2h  
10.2 Metrics and alarms — 4h  
10.3 Dashboard and runbooks — 4h  

**Estimated hours (task):** 10

---

### TASK 11: Authentication and authorization

**Technical description:**  
Implement user authentication with Amazon Cognito (user pool): sign-up, sign-in, JWT validation at API. Authorize so users can only access their own projects (or org-scoped if multi-tenant). Frontend: login/signup pages and token attachment to API requests.  
**Subtasks:**  
11.1 Cognito user pool and app client — 4h  
11.2 API authorizer (Lambda or API Gateway) — 6h  
11.3 Frontend auth flows and protected routes — 6h  

**Estimated hours (task):** 16

---

### TASK 12: End-to-end testing and documentation

**Technical description:**  
E2E tests for critical path: create project → upload description → generate questions → answer → clarify → generate doc → generate breakdown → create JIRA tickets. Update README and architecture doc. Add runbook for deployment and troubleshooting.  
**Subtasks:**  
12.1 E2E test suite (e.g. Playwright/Cypress) — 12h  
12.2 README, architecture diagram, and runbook — 6h  

**Estimated hours (task):** 18

---

## Part 5: Summary – Total Estimated Hours

| Task | Title | Hours |
|------|--------|-------|
| 1 | Project ingestion and storage | 24 |
| 2 | AI-powered question generation | 14 |
| 3 | Q&A collection and “ambiguities cleared” | 16 |
| 4 | Technical documentation generation and upload | 22 |
| 5 | Task breakdown and hour estimation (AI) | 18 |
| 6 | JIRA integration – connection and auth | 16 |
| 7 | JIRA project creation and ticket creation (Epic, Tasks, Subtasks) | 30 |
| 8 | Frontend – project list, detail, and workflow | 32 |
| 9 | AWS infrastructure and networking | 26 |
| 10 | Management, governance, and observability | 10 |
| 11 | Authentication and authorization | 16 |
| 12 | End-to-end testing and documentation | 18 |
| **Total** | | **270** |

*(Note: 270h total. Target “this week” implies a minimal MVP: consider Tasks 9, 11, 1, 2, 3, 6, 8 (core flow) first; then 4, 5, 7, 10, 12.)*

---

## Part 6: Next Steps

1. ~~Fill in Part 1~~ — **Done.**  
2. **Final technical documentation** — See *TECHNICAL_DOCUMENTATION.md*.  
3. **JIRA:** Copy Epic and Tasks from Part 4 into your JIRA (create project first if needed); add custom field “Estimated hours” where desired.  
4. **Prioritize for “this week” MVP:** Infra (9) → Auth (11) → Ingestion (1) → Questions (2) → Q&A (3) → JIRA connect (6) → Frontend (8); then Doc (4), Breakdown (5), JIRA create/update (7), Observability (10), E2E (12).

---

*Implementation plan v2.0 — clarifications incorporated. See TECHNICAL_DOCUMENTATION.md for full technical spec.*
