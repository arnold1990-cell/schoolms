import { useCallback, useEffect, useState } from 'react';
import { AxiosError } from 'axios';
import api from '../services/api';
import { useAuth } from '../hooks/useAuth';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';

interface DashboardData {
  totalTeachers?: number;
  totalStudents?: number;
  totalClasses?: number;
  totalSubjects?: number;
  totalExams?: number;
  upcomingExams?: Array<Record<string, unknown>>;
  notifications?: Array<Record<string, unknown>>;
}

function getErrorMessage(error: unknown) {
  const axiosError = error as AxiosError<{ message?: string }>;
  return axiosError.response?.data?.message ?? axiosError.message ?? 'Unable to fetch dashboard data.';
}

export function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = useCallback(async () => {
    if (!user?.role) return;
    setLoading(true);
    setError('');

    const endpoint = user.role === 'ADMIN' ? '/api/admin/dashboard' : '/api/teacher/dashboard';
    try {
      if (import.meta.env.DEV) {
        console.debug('[Dashboard] loading', endpoint);
      }
      const response = await api.get(endpoint);
      setData(response.data?.data ?? null);
    } catch (err) {
      setError(getErrorMessage(err));
      if (import.meta.env.DEV) {
        console.error('[Dashboard] load failed', err);
      }
    } finally {
      setLoading(false);
    }
  }, [user?.role]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  const stats = [
    { label: 'Total Teachers', value: data?.totalTeachers ?? 0 },
    { label: 'Total Students', value: data?.totalStudents ?? 0 },
    { label: 'Total Classes', value: data?.totalClasses ?? 0 },
    { label: 'Total Subjects', value: data?.totalSubjects ?? 0 },
    { label: 'Total Exams', value: data?.totalExams ?? 0 },
  ];

  return (
    <div className="page">
      <PageHeader title={`${user?.role ?? ''} Dashboard`.trim()} subtitle="School-wide metrics and recent activity." />
      {loading ? <LoadingState title="Loading dashboard..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void loadDashboard()} /> : null}
      {!loading && !error && !data ? <EmptyState title="Dashboard unavailable" message="No dashboard data was returned by the API." /> : null}

      {!loading && !error && data ? (
        <>
          <div className="grid">
            {stats.map((stat) => (
              <div className="card" key={stat.label}>
                <p>{stat.label}</p>
                <h3>{stat.value}</h3>
              </div>
            ))}
          </div>

          <div className="grid" style={{ marginTop: 16 }}>
            <div className="card">
              <h3>Upcoming Exams</h3>
              {data.upcomingExams?.length ? (
                <ul>
                  {data.upcomingExams.slice(0, 5).map((exam, index) => (
                    <li key={index}>{String(exam.title ?? exam.examCode ?? 'Scheduled exam')}</li>
                  ))}
                </ul>
              ) : (
                <p>No upcoming exams yet.</p>
              )}
            </div>
            <div className="card">
              <h3>Recent Notifications</h3>
              {data.notifications?.length ? (
                <ul>
                  {data.notifications.slice(0, 5).map((item, index) => (
                    <li key={index}>{String(item.title ?? item.message ?? 'Notification')}</li>
                  ))}
                </ul>
              ) : (
                <p>No recent notifications.</p>
              )}
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
