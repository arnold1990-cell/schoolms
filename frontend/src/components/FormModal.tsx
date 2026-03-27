import { ReactNode } from 'react';

interface FormModalProps {
  title: string;
  open: boolean;
  onClose: () => void;
  children: ReactNode;
}

export function FormModal({ title, open, onClose, children }: FormModalProps) {
  if (!open) return null;

  return (
    <div className="modal-backdrop" onClick={onClose} role="presentation">
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button type="button" className="text-button" onClick={onClose}>Close</button>
        </div>
        {children}
      </div>
    </div>
  );
}
