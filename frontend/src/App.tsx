import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout';
import { AuthGate } from './components/AuthGate';
import { ProjectListPage } from './pages/ProjectListPage';
import { CreateProjectPage } from './pages/CreateProjectPage';
import { ProjectDetailPage } from './pages/ProjectDetailPage';
import { LoginPage } from './pages/LoginPage';
import { GlobalErrorBar } from './components/GlobalErrorBar';

function App() {
  return (
    <>
      <GlobalErrorBar />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/projects" replace />} />
          <Route element={<AuthGate />}>
            <Route path="projects" element={<ProjectListPage />} />
            <Route path="projects/new" element={<CreateProjectPage />} />
            <Route path="projects/:projectId" element={<ProjectDetailPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/projects" replace />} />
      </Routes>
    </>
  );
}

export default App;
