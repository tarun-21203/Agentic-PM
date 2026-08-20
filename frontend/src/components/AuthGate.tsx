import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { isAuthenticated } from '../api/authService';

export function AuthGate() {
  const location = useLocation();
  const authed = isAuthenticated();
  if (!authed) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

