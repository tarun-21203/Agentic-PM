import { getJson, postJson } from './client';
import type { BreakdownResponse } from './types';

const BASE = '/projects';

export const breakdownService = {
  generate(projectId: string): Promise<BreakdownResponse> {
    return postJson<BreakdownResponse, {}>(`${BASE}/${projectId}/generate-breakdown`, {});
  },

  get(projectId: string): Promise<BreakdownResponse> {
    return getJson<BreakdownResponse>(`${BASE}/${projectId}/breakdown`);
  },
};

