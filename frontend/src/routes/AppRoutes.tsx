import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { AppLayout } from '../layouts/AppLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { ReportsPage } from '../pages/ReportsPage';
import { MarksPage } from '../pages/MarksPage';
import { ResultsPage } from '../pages/ResultsPage';
import { TeachersPage } from '../pages/TeachersPage';
import { StudentsPage } from '../pages/StudentsPage';
import { ClassesPage } from '../pages/ClassesPage';
import { SubjectsPage } from '../pages/SubjectsPage';
import { SessionsPage } from '../pages/SessionsPage';
import { ExamsPage } from '../pages/ExamsPage';
import { NotificationsPage } from '../pages/NotificationsPage';
import { AccessDeniedPage } from '../pages/AccessDeniedPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/access-denied" element={<AccessDeniedPage />} />
      <Route
        path="/"
        element={(
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        )}
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="teachers" element={<ProtectedRoute allowedRoles={['ADMIN']}><TeachersPage /></ProtectedRoute>} />
        <Route path="students" element={<ProtectedRoute allowedRoles={['ADMIN']}><StudentsPage /></ProtectedRoute>} />
        <Route path="classes" element={<ProtectedRoute allowedRoles={['ADMIN']}><ClassesPage /></ProtectedRoute>} />
        <Route path="subjects" element={<SubjectsPage />} />
        <Route path="sessions" element={<ProtectedRoute allowedRoles={['ADMIN']}><SessionsPage /></ProtectedRoute>} />
        <Route path="exams" element={<ExamsPage />} />
        <Route path="marks" element={<MarksPage />} />
        <Route path="results" element={<ResultsPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="reports" element={<ProtectedRoute allowedRoles={['ADMIN']}><ReportsPage /></ProtectedRoute>} />
      </Route>
    </Routes>
  );
}
