export function PageHeader({
  title,
  subtitle,
  actionLabel,
  onAction,
  disabled,
}: {
  title: string;
  subtitle?: string;
  actionLabel?: string;
  onAction?: () => void;
  disabled?: boolean;
}) {
  return (
    <div className="page-header">
      <div>
        <h2>{title}</h2>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      {actionLabel ? (
        <button onClick={onAction} disabled={disabled} title={disabled ? 'Action unavailable for your role' : actionLabel}>
          {actionLabel}
        </button>
      ) : null}
    </div>
  );
}
