"use client";

export function AdminTitle({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="ad-title">
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {children}
    </div>
  );
}
export function Notice({
  error,
  message,
}: {
  error?: string;
  message?: string;
}) {
  if (error)
    return (
      <div className="ad-notice error" role="alert">
        {error}
      </div>
    );
  if (message)
    return (
      <div className="ad-notice" role="status">
        {message}
      </div>
    );
  return null;
}
export function Empty({ children }: { children: React.ReactNode }) {
  return <p className="ad-empty">{children}</p>;
}
export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="ad-pagination">
      <button
        className="ad-btn secondary"
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
      >
        Trước
      </button>
      <span>
        Trang {page}/{totalPages}
      </span>
      <button
        className="ad-btn secondary"
        disabled={page >= totalPages}
        onClick={() => onChange(page + 1)}
      >
        Sau
      </button>
    </div>
  );
}
export function Confirm({
  title,
  description,
  pending,
  onConfirm,
  onCancel,
}: {
  title: string;
  description: string;
  pending: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="ad-confirm" role="region" aria-label={title}>
      <h2>{title}</h2>
      <p>{description}</p>
      <div className="ad-actions">
        <button
          autoFocus
          className="ad-btn secondary"
          disabled={pending}
          onClick={onCancel}
        >
          Quay lại
        </button>
        <button
          className="ad-btn danger"
          disabled={pending}
          onClick={onConfirm}
        >
          {pending ? "Đang xử lý…" : "Xác nhận"}
        </button>
      </div>
    </div>
  );
}
