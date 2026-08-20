import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Alert,
  CircularProgress,
  Button,
  Divider,
  Stack,
  TextField,
  Chip,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { projectService } from '../api/projectService';
import type { BreakdownResponse, DocumentationResponse, Project, Question } from '../api/types';
import { questionService } from '../api/questionService';
import { documentationService } from '../api/documentationService';
import { breakdownService } from '../api/breakdownService';
import { jiraService } from '../api/jiraService';

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [questions, setQuestions] = useState<Question[]>([]);
  const [answerDrafts, setAnswerDrafts] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState<string | null>(null);

  const [docMeta, setDocMeta] = useState<DocumentationResponse | null>(null);
  const [docMarkdown, setDocMarkdown] = useState<string | null>(null);

  const [breakdown, setBreakdown] = useState<BreakdownResponse | null>(null);

  const [jiraConnected, setJiraConnected] = useState<boolean | null>(null);
  const [jiraSite, setJiraSite] = useState('');
  const [jiraEmail, setJiraEmail] = useState('');
  const [jiraToken, setJiraToken] = useState('');

  useEffect(() => {
    if (!projectId) return;
    projectService
      .get(projectId)
      .then(setProject)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load project'))
      .finally(() => setLoading(false));
  }, [projectId]);

  const refreshProject = async () => {
    if (!projectId) return;
    const p = await projectService.get(projectId);
    setProject(p);
  };

  const loadQuestions = async () => {
    if (!projectId) return;
    const q = await questionService.list(projectId);
    setQuestions(q);
  };

  const loadJiraStatus = async () => {
    const status = await jiraService.status();
    setJiraConnected(status.connected);
    setJiraSite(status.jiraSite ?? '');
    setJiraEmail(status.jiraEmail ?? '');
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !project) {
    return (
      <Alert severity="error">
        {error ?? 'Project not found'}
        <Button component={Link} to="/projects" sx={{ ml: 1 }}>
          Back to list
        </Button>
      </Alert>
    );
  }

  return (
    <Box>
      <Button component={Link} to="/projects" startIcon={<ArrowBackIcon />} sx={{ mb: 2 }}>
        Back to projects
      </Button>
      <Typography variant="h5" gutterBottom>
        {project.name}
      </Typography>
      <Paper sx={{ p: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Project ID: {project.projectId}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Status: {project.status}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Created: {new Date(project.createdAt).toLocaleString()}
        </Typography>
        {project.descriptionS3Key && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Description file uploaded.
          </Typography>
        )}
        <Box mt={1}>
          <Chip
            size="small"
            label={project.status}
            color={project.status === 'CLARIFIED' ? 'success' : 'default'}
          />
        </Box>
      </Paper>

      <Divider sx={{ my: 3 }} />

      <Stack spacing={2}>
        <Typography variant="h6">Questions & Answers</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap">
          <Button
            variant="contained"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('generateQuestions');
              setError(null);
              try {
                const q = await questionService.generate(projectId);
                setQuestions(q);
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to generate questions');
              } finally {
                setBusy(null);
              }
            }}
          >
            Generate questions
          </Button>
          <Button
            variant="outlined"
            disabled={busy !== null}
            onClick={async () => {
              setBusy('loadQuestions');
              setError(null);
              try {
                await loadQuestions();
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load questions');
              } finally {
                setBusy(null);
              }
            }}
          >
            Refresh questions
          </Button>
          <Button
            variant="outlined"
            disabled={busy !== null || project.status === 'CLARIFIED'}
            onClick={async () => {
              if (!projectId) return;
              setBusy('clarify');
              setError(null);
              try {
                await questionService.clarifyComplete(projectId);
                await refreshProject();
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to mark clarified');
              } finally {
                setBusy(null);
              }
            }}
          >
            Mark clarified
          </Button>
        </Stack>

        {questions.length === 0 ? (
          <Typography color="text.secondary">
            No questions yet. Generate questions to start the clarification flow.
          </Typography>
        ) : (
          <Paper sx={{ p: 2 }}>
            <Stack spacing={2}>
              {questions
                .slice()
                .sort((a, b) => a.order - b.order)
                .map((q) => (
                  <Box key={q.questionId}>
                    <Typography fontWeight={600}>
                      {q.order + 1}. {q.text}
                    </Typography>
                    <TextField
                      fullWidth
                      multiline
                      minRows={2}
                      label="Answer"
                      value={answerDrafts[q.questionId] ?? ''}
                      onChange={(e) =>
                        setAnswerDrafts((prev) => ({ ...prev, [q.questionId]: e.target.value }))
                      }
                      sx={{ mt: 1 }}
                    />
                    <Button
                      sx={{ mt: 1 }}
                      size="small"
                      variant="outlined"
                      disabled={busy !== null}
                      onClick={async () => {
                        if (!projectId) return;
                        setBusy(`answer:${q.questionId}`);
                        setError(null);
                        try {
                          await questionService.saveAnswer(projectId, q.questionId, {
                            answer: answerDrafts[q.questionId] ?? '',
                          });
                        } catch (e) {
                          setError(e instanceof Error ? e.message : 'Failed to save answer');
                        } finally {
                          setBusy(null);
                        }
                      }}
                    >
                      Save answer
                    </Button>
                  </Box>
                ))}
            </Stack>
          </Paper>
        )}

        <Divider sx={{ my: 1 }} />

        <Typography variant="h6">Technical documentation</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap">
          <Button
            variant="contained"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('genDoc');
              setError(null);
              try {
                const meta = await documentationService.generate(projectId);
                setDocMeta(meta);
                const md = await documentationService.fetchMarkdown(meta.downloadUrl);
                setDocMarkdown(md);
                await refreshProject();
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to generate documentation');
              } finally {
                setBusy(null);
              }
            }}
          >
            Generate documentation
          </Button>
          <Button
            variant="outlined"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('getDoc');
              setError(null);
              try {
                const meta = await documentationService.get(projectId);
                setDocMeta(meta);
                const md = await documentationService.fetchMarkdown(meta.downloadUrl);
                setDocMarkdown(md);
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load documentation');
              } finally {
                setBusy(null);
              }
            }}
          >
            View latest documentation
          </Button>
        </Stack>
        {docMarkdown && (
          <Paper sx={{ p: 2, bgcolor: 'grey.50' }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Document key: {docMeta?.s3Key}
            </Typography>
            <pre style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{docMarkdown}</pre>
          </Paper>
        )}

        <Divider sx={{ my: 1 }} />

        <Typography variant="h6">Breakdown</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap">
          <Button
            variant="contained"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('genBreakdown');
              setError(null);
              try {
                const b = await breakdownService.generate(projectId);
                setBreakdown(b);
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to generate breakdown');
              } finally {
                setBusy(null);
              }
            }}
          >
            Generate breakdown
          </Button>
          <Button
            variant="outlined"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('getBreakdown');
              setError(null);
              try {
                const b = await breakdownService.get(projectId);
                setBreakdown(b);
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load breakdown');
              } finally {
                setBusy(null);
              }
            }}
          >
            View latest breakdown
          </Button>
        </Stack>
        {breakdown && (
          <Paper sx={{ p: 2 }}>
            <Stack spacing={2}>
              {breakdown.tasks.map((t) => (
                <Box key={t.taskId}>
                  <Typography fontWeight={700}>
                    {t.taskId}: {t.title} ({t.estimatedHours}h)
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t.description}
                  </Typography>
                  {t.subtasks.length > 0 && (
                    <Box mt={1} ml={2}>
                      {t.subtasks.map((st, idx) => (
                        <Typography key={idx} variant="body2">
                          - {st.title} ({st.estimatedHours}h)
                        </Typography>
                      ))}
                    </Box>
                  )}
                </Box>
              ))}
            </Stack>
          </Paper>
        )}

        <Divider sx={{ my: 1 }} />

        <Typography variant="h6">JIRA</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap">
          <Button
            variant="outlined"
            disabled={busy !== null}
            onClick={async () => {
              setBusy('jiraStatus');
              setError(null);
              try {
                await loadJiraStatus();
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load JIRA status');
              } finally {
                setBusy(null);
              }
            }}
          >
            Refresh JIRA status
          </Button>
          {jiraConnected !== null && (
            <Chip
              size="small"
              color={jiraConnected ? 'success' : 'default'}
              label={jiraConnected ? 'Connected' : 'Not connected'}
            />
          )}
        </Stack>

        <Paper sx={{ p: 2 }}>
          <Stack spacing={2}>
            <Typography fontWeight={600}>Connect JIRA (Cloud)</Typography>
            <TextField
              fullWidth
              label="JIRA Site (e.g. https://tenant.atlassian.net)"
              value={jiraSite}
              onChange={(e) => setJiraSite(e.target.value)}
            />
            <TextField
              fullWidth
              label="JIRA Email"
              value={jiraEmail}
              onChange={(e) => setJiraEmail(e.target.value)}
            />
            <TextField
              fullWidth
              label="API Token"
              value={jiraToken}
              onChange={(e) => setJiraToken(e.target.value)}
              type="password"
            />
            <Button
              variant="contained"
              disabled={busy !== null}
              onClick={async () => {
                setBusy('jiraConnect');
                setError(null);
                try {
                  const status = await jiraService.connect({
                    jiraSite,
                    jiraEmail,
                    apiToken: jiraToken,
                  });
                  setJiraConnected(status.connected);
                  setJiraToken('');
                } catch (e) {
                  setError(e instanceof Error ? e.message : 'Failed to connect JIRA');
                } finally {
                  setBusy(null);
                }
              }}
            >
              Connect
            </Button>
          </Stack>
        </Paper>

        <Stack direction="row" spacing={1} flexWrap="wrap">
          <Button
            variant="contained"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('jiraCreateProject');
              setError(null);
              try {
                await jiraService.createProject(projectId);
                await refreshProject();
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to create JIRA project');
              } finally {
                setBusy(null);
              }
            }}
          >
            Create JIRA project
          </Button>
          <Button
            variant="outlined"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('jiraCreateTickets');
              setError(null);
              try {
                await jiraService.createTickets(projectId);
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to create JIRA tickets');
              } finally {
                setBusy(null);
              }
            }}
          >
            Create JIRA tickets
          </Button>
          <Button
            variant="outlined"
            disabled={busy !== null}
            onClick={async () => {
              if (!projectId) return;
              setBusy('jiraUpdateTickets');
              setError(null);
              try {
                await jiraService.updateTickets(projectId);
              } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to update JIRA tickets');
              } finally {
                setBusy(null);
              }
            }}
          >
            Update JIRA tickets
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
}
