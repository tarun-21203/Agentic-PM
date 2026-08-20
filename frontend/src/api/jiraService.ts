import { getJson, postJson } from './client';
import type { JiraConnectRequest, JiraStatusResponse } from './types';

export const jiraService = {
  connect(request: JiraConnectRequest): Promise<JiraStatusResponse> {
    return postJson<JiraStatusResponse, JiraConnectRequest>(`/integrations/jira/connect`, request);
  },

  status(): Promise<JiraStatusResponse> {
    return getJson<JiraStatusResponse>(`/integrations/jira/status`);
  },

  createProject(projectId: string): Promise<{ jiraProjectKey: string }> {
    return postJson<{ jiraProjectKey: string }, {}>(`/projects/${projectId}/create-jira-project`, {});
  },

  createTickets(projectId: string): Promise<{ epicKey: string; epicLinked?: boolean }> {
    return postJson<{ epicKey: string; epicLinked?: boolean }, {}>(
      `/projects/${projectId}/create-jira-tickets`,
      {},
    );
  },

  updateTickets(projectId: string): Promise<{ status: string }> {
    return postJson<{ status: string }, {}>(`/projects/${projectId}/update-jira-tickets`, {});
  },
};

