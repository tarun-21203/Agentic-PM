export interface Project {
  projectId: string;
  userId: string;
  name: string;
  status: string;
  descriptionS3Key: string | null;
  docS3Key: string | null;
  jiraProjectKey: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  name: string;
}

export interface PresignedUpload {
  uploadUrl: string;
  s3Key: string;
  expiresInSeconds: number;
}

export interface CreateDescriptionUploadUrlRequest {
  fileName: string;
  contentType: string;
}

export interface Question {
  questionId: string;
  projectId: string;
  text: string;
  category: string | null;
  order: number;
  createdAt: string;
}

export interface AnswerRequest {
  answer: string;
}

export interface DocumentationResponse {
  downloadUrl: string;
  s3Key: string;
  expiresInSeconds: number;
}

export interface BreakdownSubtask {
  title: string;
  description: string;
  estimatedHours: number;
}

export interface BreakdownTask {
  taskId: string;
  title: string;
  description: string;
  estimatedHours: number;
  subtasks: BreakdownSubtask[];
}

export interface BreakdownResponse {
  projectId: string;
  breakdownVersion: string;
  tasks: BreakdownTask[];
  createdAt: string;
  updatedAt: string;
}

export interface JiraConnectRequest {
  jiraSite: string;
  jiraEmail: string;
  apiToken: string;
}

export interface JiraStatusResponse {
  connected: boolean;
  jiraSite: string | null;
  jiraEmail: string | null;
}
