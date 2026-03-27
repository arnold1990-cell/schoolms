export function PageHeader({
  title,
  subtitle,
  actionLabel,
  onAction,
  disabled,
  disabledReason,
}: {
  title: string;
  subtitle?: string;
  actionLabel?: string;
  onAction?: () => void;
  disabled?: boolean;
  disabledReason?: string;
}) {
  return (
    <div className="page-header">
      <div>
        <h2>{title}</h2>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      {actionLabel ? (
        <div>
          <button onClick={onAction} disabled={disabled} title={disabled ? (disabledReason ?? 'Action unavailable for your role') : actionLabel}>
            {actionLabel}
          </button>
          {disabled && disabledReason ? <p className="hint-text">{disabledReason}</p> : null}
        </div>
      ) : null}
    </div>
  );
}
