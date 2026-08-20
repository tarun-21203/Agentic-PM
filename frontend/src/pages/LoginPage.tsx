import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  TextField,
  Button,
  Typography,
  Alert,
  InputAdornment,
  IconButton,
} from '@mui/material';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import { isAuthenticated, signInWithEmailPassword } from '../api/authService';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reasonMessage = useMemo(() => {
    const params = new URLSearchParams(location.search);
    if (params.get('reason') === 'session-expired') return 'Your session expired. Please sign in again.';
    return null;
  }, [location.search]);

  useEffect(() => {
    if (isAuthenticated()) {
      navigate('/projects', { replace: true });
    }
  }, [navigate]);

  async function onSubmit(e?: FormEvent) {
    e?.preventDefault();
    setError(null);
    const trimmed = email.trim();
    if (!trimmed || !password) {
      setError('Email and password are required');
      return;
    }

    setLoading(true);
    try {
      await signInWithEmailPassword(trimmed, password);
      const redirectPath = (location.state as { from?: string } | null)?.from || '/projects';
      navigate(redirectPath, { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Box maxWidth="sm" mx="auto" sx={{ minHeight: '70vh', display: 'flex', alignItems: 'center' }}>
      <Paper sx={{ p: 3, width: '100%' }}>
        <Typography variant="h5" gutterBottom>
          Sign in
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          Use your Cognito account to continue to Agentic PM.
        </Typography>
        {reasonMessage && (
          <Alert severity="info" sx={{ mb: 2 }}>
            {reasonMessage}
          </Alert>
        )}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box component="form" onSubmit={onSubmit}>
          <TextField
            fullWidth
            label="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            margin="normal"
            autoComplete="email"
            disabled={loading}
            type="email"
          />
          <TextField
            fullWidth
            label="Password"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            margin="normal"
            autoComplete="current-password"
            disabled={loading}
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    edge="end"
                    onClick={() => setShowPassword((s) => !s)}
                    disabled={loading}
                    aria-label="toggle password visibility"
                  >
                    {showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />

          <Button variant="contained" fullWidth sx={{ mt: 2 }} disabled={loading} type="submit">
            {loading ? 'Signing in...' : 'Sign in'}
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}

