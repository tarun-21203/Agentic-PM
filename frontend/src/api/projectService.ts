import { getJson, postJson } from './client';
import type {
  Project,
  CreateProjectRequest,
  CreateDescriptionUploadUrlRequest,
  PresignedUpload,
} from './types';

const BASE = '/projects';

export const projectService = {
  list(): Promise<Project[]> {
    return getJson<Project[]>(BASE);
  },

  get(projectId: string): Promise<Project> {
    return getJson<Project>(`${BASE}/${projectId}`);
  },

  create(request: CreateProjectRequest): Promise<Project> {
    return postJson<Project, CreateProjectRequest>(BASE, request);
  },

  createDescriptionUploadUrl(
    projectId: string,
    request: CreateDescriptionUploadUrlRequest
  ): Promise<PresignedUpload> {
    return postJson<PresignedUpload, CreateDescriptionUploadUrlRequest>(
      `${BASE}/${projectId}/description-url`,
      request
    );
  },

  async uploadDescriptionViaPresignedUrl(
    projectId: string,
    file: File
  ): Promise<Project> {
    const presigned = await projectService.createDescriptionUploadUrl(projectId, {
      fileName: file.name,
      contentType: file.type || 'application/octet-stream',
    });

    const putRes = await fetch(presigned.uploadUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
    });
    if (!putRes.ok) {
      const text = await putRes.text();
      throw new Error(text || 'Failed to upload to S3');
    }

    return projectService.get(projectId);
  },
};

