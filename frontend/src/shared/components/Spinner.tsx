export function Spinner() {
  return (
    <span className="spinner" aria-label="Loading">
      <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
        <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" fill="none" strokeDasharray="31.4 31.4" strokeLinecap="round" />
      </svg>
    </span>
  );
}
