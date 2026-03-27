interface StateProps {
  title: string;
  message?: string;
  actionLabel?: string;
  onAction?: () => void;
}

function StateBox({ title, message, actionLabel, onAction }: StateProps) {
  return (
    <div className="card state-box">
      <h3>{title}</h3>
      {message ? <p>{message}</p> : null}
      {actionLabel && onAction ? <button onClick={onAction}>{actionLabel}</button> : null}
    </div>
  );
}

export function LoadingState({ title = 'Loading data...' }: { title?: string }) {
  return <StateBox title={title} message="Please wait while we fetch the latest records." />;
}

export function EmptyState({ title = 'No data yet', message = 'There are no records available for this module yet.' }: { title?: string; message?: string }) {
  return <StateBox title={title} message={message} />;
}

export function ErrorState({ title = 'Failed to load data', message = 'Please retry. If the issue persists, check API logs.', onRetry }: { title?: string; message?: string; onRetry?: () => void }) {
  return <StateBox title={title} message={message} actionLabel={onRetry ? 'Retry' : undefined} onAction={onRetry} />;
}
