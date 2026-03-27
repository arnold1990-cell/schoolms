import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { AppLayout } from '../layouts/AppLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { CrudPageFactory } from '../pages/CrudPageFactory';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { ReportsPage } from '../pages/ReportsPage';
import { MarksPage } from '../pages/MarksPage';
import { ResultsPage } from '../pages/ResultsPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
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
        <Route path="teachers" element={<CrudPageFactory title="Teachers" endpoint="/api/teachers" addLabel="Add" />} />
        <Route path="students" element={<CrudPageFactory title="Students" endpoint="/api/students" addLabel="Add" />} />
        <Route path="classes" element={<CrudPageFactory title="Classes" endpoint="/api/classes" addLabel="Add" />} />
        <Route path="subjects" element={<CrudPageFactory title="Subjects" endpoint="/api/subjects" addLabel="Add" />} />
        <Route path="sessions" element={<CrudPageFactory title="Sessions" endpoint="/api/sessions" subtitle="Manage academic sessions and active periods." addLabel="Add" />} />
        <Route path="exams" element={<CrudPageFactory title="Exams" endpoint="/api/exams" addLabel="Create" adminOnlyAdd={false} />} />
        <Route path="marks" element={<MarksPage />} />
        <Route path="results" element={<ResultsPage />} />
        <Route path="notifications" element={<CrudPageFactory title="Notifications" endpoint="/api/notifications" addLabel="Create" adminOnlyAdd={false} />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="reports" element={<ReportsPage />} />
      </Route>
    </Routes>
  );
}
