import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
  LinearProgress,
  Stack,
  Chip,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { projectService } from '../api/projectService';

const ALLOWED_EXTENSIONS = ['.pdf', '.docx', '.doc', '.txt'];
const MAX_SIZE_MB = 10;

export function CreateProjectPage() {
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async () => {
    setError(null);
    if (!name.trim()) {
      setError('Project name is required');
      return;
    }
    setLoading(true);
    try {
      const project = await projectService.create({ name: name.trim() });
      if (file) {
        await projectService.uploadDescriptionViaPresignedUrl(project.projectId, file);
      }
      navigate(`/projects/${project.projectId}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create project');
    } finally {
      setLoading(false);
    }
  };

  const ext = file?.name ? file.name.substring(file.name.lastIndexOf('.')).toLowerCase() : '';
  const fileOk = !file || (ALLOWED_EXTENSIONS.includes(ext) && file.size <= MAX_SIZE_MB * 1024 * 1024);

  return (
    <Box maxWidth="sm" mx="auto">
      <Typography variant="h5" gutterBottom>
        Create project
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Start with a project name, then optionally upload a requirements file.
      </Typography>
      <Paper sx={{ p: 3 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}
        <TextField
          fullWidth
          label="Project name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          margin="normal"
          required
          disabled={loading}
        />
        <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
          Description file (optional)
        </Typography>
        <Stack direction="row" spacing={1} sx={{ mb: 1 }} flexWrap="wrap">
          {ALLOWED_EXTENSIONS.map((type) => (
            <Chip key={type} label={type.toUpperCase()} size="small" variant="outlined" />
          ))}
          <Chip label={`Max ${MAX_SIZE_MB} MB`} size="small" variant="outlined" />
        </Stack>
        <Button
          variant="outlined"
          component="label"
          fullWidth
          sx={{ mb: 1 }}
          disabled={loading}
          startIcon={<UploadFileIcon />}
        >
          {file ? 'Change file' : 'Choose file'}
          <input
            type="file"
            hidden
            accept=".pdf,.docx,.doc,.txt"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
        </Button>
        {file && (
          <Alert icon={<CheckCircleOutlineIcon fontSize="inherit" />} severity="success" sx={{ mb: 2 }}>
            Selected: {file.name}
          </Alert>
        )}
        {file && !fileOk && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            File must be PDF, DOCX, or TXT and under {MAX_SIZE_MB} MB.
          </Alert>
        )}
        {loading && <LinearProgress sx={{ mb: 2 }} />}
        <Button
          variant="contained"
          onClick={handleCreate}
          disabled={loading || !name.trim() || (!!file && !fileOk)}
        >
          {loading ? 'Creating…' : 'Create project'}
        </Button>
      </Paper>
    </Box>
  );
}
