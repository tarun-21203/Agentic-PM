import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { AppBar, Toolbar, Typography, Button, Container, Stack, Chip } from '@mui/material';
import FolderIcon from '@mui/icons-material/Folder';
import AddIcon from '@mui/icons-material/Add';
import LogoutIcon from '@mui/icons-material/Logout';
import { getAuthenticatedEmail, logout } from '../api/authService';

export function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const userEmail = getAuthenticatedEmail();

  function onLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <>
      <AppBar position="static">
        <Toolbar>
          <FolderIcon sx={{ mr: 1 }} />
          <Typography variant="h6" component={Link} to="/projects" sx={{ flexGrow: 1, textDecoration: 'none', color: 'inherit' }}>
            Agentic PM
          </Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            {userEmail && (
              <Chip size="small" label={userEmail} variant="outlined" sx={{ color: 'white', borderColor: 'rgba(255,255,255,0.5)', maxWidth: 260 }} />
            )}
            {location.pathname !== '/projects/new' && (
              <Button color="inherit" component={Link} to="/projects/new" startIcon={<AddIcon />}>
                New project
              </Button>
            )}
            <Button color="inherit" onClick={onLogout} startIcon={<LogoutIcon />}>
              Logout
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ mt: 3, mb: 3 }}>
        <Outlet />
      </Container>
    </>
  );
}
