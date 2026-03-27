import { useCallback, useEffect, useState } from 'react';
import api from '../services/api';
import { PageHeader } from '../components/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/PageStates';
import { apiErrorMessage, unwrapList } from '../utils/apiHelpers';

interface NotificationItem {
  id: number;
  title?: string;
  message?: string;
  read: boolean;
  createdAt?: string;
}

export function NotificationsPage() {
  const [rows, setRows] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/notifications');
      setRows(unwrapList<NotificationItem>(response.data));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load notifications.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const markAsRead = async (id: number) => {
    try {
      await api.put(`/api/notifications/${id}/read`);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to update notification.'));
    }
  };

  return (
    <div className="page">
      <PageHeader title="Notifications" subtitle="Review incoming notifications and mark them as read." actionLabel="Create Notification" disabled disabledReason="Creation endpoint is not yet available in backend API." />
      {loading ? <LoadingState title="Loading notifications..." /> : null}
      {!loading && error ? <ErrorState message={error} onRetry={() => void load()} /> : null}
      {!loading && !error && rows.length === 0 ? <EmptyState title="No notifications" message="You're all caught up." /> : null}
      {!loading && !error && rows.length > 0 ? (
        <table className="table"><thead><tr><th>Title</th><th>Message</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody>{rows.map((item) => <tr key={item.id}><td>{item.title || '-'}</td><td>{item.message || '-'}</td><td><span className={item.read ? 'badge active' : 'badge'}>{item.read ? 'Read' : 'Unread'}</span></td><td>{item.createdAt || '-'}</td><td><button type="button" disabled={item.read} onClick={() => void markAsRead(item.id)}>{item.read ? 'Read' : 'Mark as read'}</button></td></tr>)}</tbody></table>
      ) : null}
    </div>
  );
}
