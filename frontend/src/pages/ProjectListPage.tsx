import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  CardActionArea,
  Typography,
  CircularProgress,
  Alert,
  Button,
  Stack,
  Chip,
  Grid,
} from '@mui/material';
import { projectService } from '../api/projectService';
import type { Project } from '../api/types';
import AddIcon from '@mui/icons-material/Add';

export function ProjectListPage() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    projectService
      .list()
      .then(setProjects)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load projects'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" onClose={() => setError(null)}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }} spacing={1} sx={{ mb: 2 }}>
        <Box>
          <Typography variant="h5">Projects</Typography>
          <Typography variant="body2" color="text.secondary">
            Track your project setup, clarification, documentation, and delivery flow.
          </Typography>
        </Box>
        <Stack direction="row" spacing={1} alignItems="center">
          <Chip color="primary" variant="outlined" label={`${projects.length} total`} />
          <Button
            variant="contained"
            component={Link}
            to="/projects/new"
            startIcon={<AddIcon />}
          >
            Create project
          </Button>
        </Stack>
      </Stack>
      {projects.length === 0 ? (
        <Card>
          <CardContent>
            <Typography variant="subtitle1" fontWeight={600}>
              No projects yet
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }}>
              Create your first project to start generating questions, documentation, and Jira tickets.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={2}>
          {projects.map((p) => (
            <Grid key={p.projectId} item xs={12} md={6}>
              <Card sx={{ height: '100%' }}>
                <CardActionArea component={Link} to={`/projects/${p.projectId}`} sx={{ height: '100%' }}>
                  <CardContent>
                    <Stack spacing={1}>
                      <Typography variant="h6">{p.name}</Typography>
                      <Chip
                        size="small"
                        label={p.status}
                        color={p.status === 'CLARIFIED' ? 'success' : 'default'}
                        sx={{ width: 'fit-content' }}
                      />
                      <Typography variant="body2" color="text.secondary">
                        Created: {new Date(p.createdAt).toLocaleDateString()}
                      </Typography>
                    </Stack>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  );
}
