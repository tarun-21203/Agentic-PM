import { getJson, postJson, putJson } from './client';
import type { AnswerRequest, Question } from './types';

const BASE = '/projects';

export const questionService = {
  generate(projectId: string): Promise<Question[]> {
    return postJson<Question[], {}>(`${BASE}/${projectId}/generate-questions`, {});
  },

  list(projectId: string): Promise<Question[]> {
    return getJson<Question[]>(`${BASE}/${projectId}/questions`);
  },

  saveAnswer(projectId: string, questionId: string, request: AnswerRequest): Promise<{ status: string }> {
    return putJson<{ status: string }, AnswerRequest>(
      `${BASE}/${projectId}/questions/${questionId}/answer`,
      request
    );
  },

  clarifyComplete(projectId: string): Promise<{ status: string }> {
    return postJson<{ status: string }, {}>(`${BASE}/${projectId}/clarify-complete`, {});
  },
};

