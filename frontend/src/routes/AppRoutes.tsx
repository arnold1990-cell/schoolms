import { Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { AppLayout } from '../layouts/AppLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { CrudPageFactory } from '../pages/CrudPageFactory';
import { AnalyticsPage } from '../pages/AnalyticsPage';
import { ReportsPage } from '../pages/ReportsPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="teachers" element={<CrudPageFactory title="Teachers" endpoint="/api/teachers" />} />
        <Route path="students" element={<CrudPageFactory title="Students" endpoint="/api/students" />} />
        <Route path="classes" element={<CrudPageFactory title="Classes" endpoint="/api/classes" />} />
        <Route path="subjects" element={<CrudPageFactory title="Subjects" endpoint="/api/subjects" />} />
        <Route path="sessions" element={<CrudPageFactory title="Sessions" endpoint="/api/sessions" />} />
        <Route path="exams" element={<CrudPageFactory title="Exams" endpoint="/api/exams" />} />
        <Route path="marks" element={<CrudPageFactory title="Marks" endpoint="/api/marks/exam/1" />} />
        <Route path="results" element={<CrudPageFactory title="Results" endpoint="/api/results/class/1" />} />
        <Route path="notifications" element={<CrudPageFactory title="Notifications" endpoint="/api/notifications" />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="reports" element={<ReportsPage />} />
      </Route>
    </Routes>
  );
}
