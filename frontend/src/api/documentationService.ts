import { getJson, postJson } from './client';
import type { DocumentationResponse } from './types';

const BASE = '/projects';

export const documentationService = {
  generate(projectId: string): Promise<DocumentationResponse> {
    return postJson<DocumentationResponse, {}>(`${BASE}/${projectId}/generate-documentation`, {});
  },

  get(projectId: string): Promise<DocumentationResponse> {
    return getJson<DocumentationResponse>(`${BASE}/${projectId}/documentation`);
  },

  async fetchMarkdown(downloadUrl: string): Promise<string> {
    const res = await fetch(downloadUrl, { method: 'GET' });
    if (!res.ok) throw new Error('Failed to fetch documentation content');
    return res.text();
  },
};

