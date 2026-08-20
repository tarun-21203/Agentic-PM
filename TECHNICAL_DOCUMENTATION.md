# Technical Documentation: Agentic AI Project Management with JIRA Integration

**Version:** 1.0  
**Date:** March 16, 2025  
**Related:** Implementation Plan v2.0 (IMPLEMENTATION_PLAN_AGENTIC_AI_PROJECT_MANAGEMENT.md)

---

## 1. Overview

This document describes the technical architecture, data models, APIs, and security for an **agentic AI project management** application that:

1. Accepts a **project description** (file upload: PDF, DOCX, TXT) and a **user-provided project name**.
2. Generates **clarifying questions** via AWS Bedrock; the same user answers them **in-app**.
3. Supports **draft** JIRA ticket creation before clarification and **updates** tickets after “ambiguities cleared.”
4. Generates **technical documentation** from the description + Q&A and displays it in the **frontend**.
5. Breaks the project into **tasks and subtasks with hour estimates** and creates a **new JIRA project** plus **Epic → Stories/Tasks → Subtasks** in JIRA Cloud.

**Target users:** PMs and developers/team leads. **Auth:** Cognito (email/password). **Timeline:** MVP target this week (phased delivery recommended).

**Tech stack:** **Backend:** Java 17, Gradle (e.g. Spring Boot). **Frontend:** React (TypeScript, React Router, MUI). **Infrastructure:** Terraform.

---

## 2. AWS Architecture

### 2.1 Mandatory Services

| Category | Service | Usage |
|----------|---------|--------|
| **Compute 1** | **Amazon ECS (Fargate)** | **Java 17** backend (Gradle/Spring Boot): REST API, Bedrock, JIRA API, file parsing, presigned URLs. |
| **Compute 2** | **AWS Lambda** | Event-driven jobs (async doc generation, bulk JIRA sync); Java 17 or Node. |
| **Database** | **Amazon DynamoDB** | Projects, questions, answers, breakdown, JIRA mapping, integration config. |
| **Storage** | **Amazon S3** | Uploaded description files; generated technical documentation (by project name). |
| **Networking & CDN** | **Amazon CloudFront** | Serve React SPA; optional API caching. **API Gateway** or **ALB** for Java API. |
| **Management & Governance** | **Amazon CloudWatch** | Logs, metrics, alarms, dashboard. |

### 2.2 Architecture Diagram (Text)

```
                    [User]
                       |
                 [CloudFront]
                       |
            +----------+----------+
            |                     |
     [React SPA]            [API Gateway]
            |                     |
            |              [ECS: Java 17 API]
            |                     |
            |         +-----------+-----------+
            |         |           |           |
            |    [DynamoDB]   [Bedrock]   [S3]
            |         |           |           |
            |         |    [Lambda: async workers]
            |         |           |           |
            |         |      [JIRA Cloud API]
            |         |           |
            +---------+-----------+
                  (Cognito for auth)
```

### 2.3 Data Flow (Summary)

- **Create project:** User provides name → Java API creates project in DynamoDB → user uploads file → stored in S3, text extracted and stored/linked.
- **Questions:** Java service reads description from S3/DynamoDB → Bedrock generates questions → stored in DynamoDB.
- **Q&A:** User submits answers via API → stored in DynamoDB; user marks “clarified.”
- **Documentation:** Java service (or Lambda) uses description + Q&A → Bedrock generates doc → upload to S3, key saved in DynamoDB → frontend fetches via API (presigned URL or proxy).
- **Breakdown:** Java service (or Lambda) uses technical doc → Bedrock generates tasks/subtasks + hours → stored in DynamoDB.
- **JIRA:** Java service creates JIRA project (when user creates app project) → creates Epic, then Stories/Tasks, then Subtasks; supports draft now / update later. Mapping stored in DynamoDB.

---

## 3. Data Model (DynamoDB)

### 3.1 Table Design

**Projects**

| Attribute | Type | Key | Description |
|-----------|------|-----|-------------|
| projectId | String | PK | UUID. |
| userId | String | — | Cognito sub. |
| name | String | — | User-provided project name. |
| status | String | — | draft \| in_review \| clarified. |
| descriptionS3Key | String | — | S3 key of uploaded file. |
| docS3Key | String | — | S3 key of generated technical doc. |
| jiraProjectKey | String | — | JIRA project key once created. |
| createdAt | String | — | ISO8601. |
| updatedAt | String | — | ISO8601. |

**GSI:** `userId-createdAt-index` (PK: userId, SK: createdAt) for listing user’s projects.

---

**Questions**

| Attribute | Type | Key | Description |
|-----------|------|-----|-------------|
| questionId | String | PK | UUID. |
| projectId | String | — | FK to project. |
| text | String | — | Question text. |
| category | String | — | Optional category. |
| order | Number | — | Display order. |
| createdAt | String | — | ISO8601. |

**GSI:** `projectId-createdAt-index` (PK: projectId, SK: createdAt).

---

**Answers**

| Attribute | Type | Key | Description |
|-----------|------|-----|-------------|
| projectId | String | PK | Partition by project. |
| questionId | String | SK | Sort key. |
| answer | String | — | User’s answer. |
| updatedAt | String | — | ISO8601. |

---

**Breakdown**

| Attribute | Type | Key | Description |
|-----------|------|-----|-------------|
| projectId | String | PK | One item per project. |
| breakdownVersion | String | SK | e.g. "v1" or timestamp. |
| tasks | List (JSON) | — | Array of { taskId, title, description, estimatedHours, subtasks: [{ title, description, estimatedHours }] }. |
| createdAt | String | — | ISO8601. |
| updatedAt | String | — | ISO8601. |

---

**JiraMappings**

| Attribute | Type | Key | Description |
|-----------|------|-----|-------------|
| projectId | String | PK | Partition by project. |
| internalId | String | SK | taskId or subtaskId from breakdown. |
| jiraIssueKey | String | — | e.g. PROJ-123. |
| jiraIssueType | String | — | Epic, Story, Task, Sub-task. |
| createdAt | String | — | ISO8601. |

---

**JiraIntegrations** (per-user JIRA connection)

| Attribute | Type | Key | Description |
|-----------|------|-----|-------------|
| userId | String | PK | Cognito sub. |
| secretId | String | — | Secrets Manager secret ID (token). |
| jiraSite | String | — | e.g. tenant.atlassian.net. |
| updatedAt | String | — | ISO8601. |

---

## 4. API Specification (REST)

Base URL: `https://<api-id>.execute-api.<region>.amazonaws.com/<stage>` or via CloudFront.

**Auth:** All endpoints (except auth) require `Authorization: Bearer <Cognito JWT>`.

### 4.1 Projects

| Method | Path | Description |
|--------|------|-------------|
| POST | /projects | Create project. Body: `{ "name": "string" }`. Returns projectId. |
| GET | /projects | List current user’s projects (query: limit, nextToken). |
| GET | /projects/{projectId} | Get project metadata. |
| POST | /projects/{projectId}/description | Upload description file (multipart or presigned URL upload). |

### 4.2 Questions & Answers

| Method | Path | Description |
|--------|------|-------------|
| POST | /projects/{projectId}/generate-questions | Trigger Bedrock; persist questions. |
| GET | /projects/{projectId}/questions | List questions. |
| PUT | /projects/{projectId}/questions/{questionId}/answer | Body: `{ "answer": "string" }`. |
| POST | /projects/{projectId}/clarify-complete | Mark project as clarified. |

### 4.3 Documentation

| Method | Path | Description |
|--------|------|-------------|
| POST | /projects/{projectId}/generate-documentation | Generate technical doc (Bedrock + S3), update DynamoDB. |
| GET | /projects/{projectId}/documentation | Return presigned URL or inline doc for frontend viewer. |

### 4.4 Breakdown

| Method | Path | Description |
|--------|------|-------------|
| POST | /projects/{projectId}/generate-breakdown | Generate tasks/subtasks + hours (Bedrock), persist. |
| GET | /projects/{projectId}/breakdown | Get current breakdown. |

### 4.5 JIRA

| Method | Path | Description |
|--------|------|-------------|
| POST | /integrations/jira/connect | Body: `{ "jiraSite", "apiToken" }`. Store in Secrets Manager, reference in DynamoDB. |
| GET | /integrations/jira/status | Connection status. |
| POST | /projects/{projectId}/create-jira-project | Create JIRA project (name from app project). |
| POST | /projects/{projectId}/create-jira-tickets | Create draft Epic + Stories/Tasks + Subtasks from breakdown. |
| POST | /projects/{projectId}/update-jira-tickets | Update existing JIRA issues after clarification. |

### 4.6 Auth (Cognito)

- Sign-up / Sign-in via Cognito User Pool (frontend or API with Cognito SDK).
- API Gateway authorizer: Cognito JWT validation; userId (sub) passed to Java API.

---

## 5. Security

- **Authentication:** Cognito User Pool; JWT validated at API Gateway or ALB (authorizer).
- **Authorization:** Users can only access their own projects (projectId ↔ userId in DynamoDB).
- **JIRA credentials:** Stored in AWS Secrets Manager; Java backend retrieves by secretId stored in DynamoDB per userId.
- **S3:** Bucket private; access via IAM from Java backend (and Lambda if used); frontend receives short-lived presigned URLs for doc view.
- **Data in transit:** HTTPS (CloudFront, API Gateway).
- **Logging:** CloudWatch logs; no PII or secrets in log payloads.

---

## 6. Frontend (React)

- **Stack:** React, TypeScript, React Router, Material UI (as specified).
- **Service layer:** Typed API client (e.g. Axios/fetch) in `src/api` or `src/services`; DTOs for request/response.
- **Screens:** Login/Signup; Project list; Create project (name + file upload); Project detail (description, questions, answers, “Confirm clarified”); Technical documentation viewer; Task breakdown view (with hours); Actions: Generate questions, Generate doc, Generate breakdown, Connect JIRA, Create JIRA project, Create/Update JIRA tickets.
- **Hosting:** S3 + CloudFront (static build).

---

## 7. MVP Phasing (Target: This Week)

Given the “this week” target, recommended order:

1. **Day 1–2:** **Terraform** infra (Task 9): VPC (minimal), S3, DynamoDB, ECS (Java 17), API Gateway/ALB, CloudFront; Auth (Task 11): Cognito + authorizer.
2. **Day 2–3:** **Java 17 (Gradle)** backend: Task 1 (ingestion), Task 2 (questions), Task 3 (Q&A + clarify).
3. **Day 3–4:** Task 6 (JIRA connect), **React** frontend (Task 8): list, create, detail, Q&A, doc viewer, breakdown view, buttons.
4. **Day 4–5:** Task 4 (doc generation), Task 5 (breakdown), Task 7 (JIRA project + draft tickets; update after clarify), Task 10 (CloudWatch).
5. **Optional:** Task 12 (E2E, runbook) as follow-up.

Reduce scope if needed: e.g. only TXT upload first; skip “update JIRA tickets” and only create once after clarify; run all logic in the **Java API** (minimize Lambda use for MVP).

---

## 8. References

- Implementation plan: `IMPLEMENTATION_PLAN_AGENTIC_AI_PROJECT_MANAGEMENT.md` (Part 4 = JIRA-ready tasks and subtasks with hours).
- **Backend:** Java 17, Gradle, Spring Boot; AWS SDK for DynamoDB, S3, Bedrock, Secrets Manager.
- **Frontend:** React, TypeScript, React Router, MUI.
- **Infrastructure:** Terraform (AWS provider); modules for VPC, ECS, Lambda, DynamoDB, S3, CloudFront, Cognito, CloudWatch.
- JIRA Cloud REST API: Create project, Create issue, Edit issue.

---

*End of Technical Documentation.*
