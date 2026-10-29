import type { ReactNode } from 'react';
import type { LogEntry } from '../game/types';

export function Meter({
  value,
  max,
  tone = 'green',
  label,
  right,
}: {
  value: number;
  max: number;
  tone?: 'green' | 'amber' | 'red' | 'cyan' | 'violet';
  label?: string;
  right?: string;
}) {
  const pct = max <= 0 ? 0 : Math.min(100, Math.max(0, (value / max) * 100));
  return (
    <div className="meter-wrap">
      {(label || right) && (
        <div className="meter-label">
          <span>{label}</span>
          <span>{right}</span>
        </div>
      )}
      <div className={`meter ${tone}`}>
        <span style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function Tile({
  label,
  value,
  hint,
  tone,
  icon,
}: {
  label: string;
  value: string;
  hint?: string;
  tone?: string;
  icon?: string;
}) {
  return (
    <div className="meter-tile">
      <div className="tile-label">
        {icon ? `${icon} ` : ''}
        {label}
      </div>
      <div className="tile-value" style={tone ? { color: tone } : undefined}>
        {value}
      </div>
      {hint && <div className="tiny dim">{hint}</div>}
    </div>
  );
}

export function Chip({ children, tone }: { children: ReactNode; tone?: 'green' | 'amber' | 'red' | 'cyan' | 'violet' }) {
  return <span className={`chip ${tone ?? ''}`}>{children}</span>;
}

export function Modal({
  children,
  onClose,
  wide,
}: {
  children: ReactNode;
  onClose?: () => void;
  wide?: boolean;
}) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className={`modal ${wide ? 'modal-wide' : ''}`} onClick={(e) => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}

export function Panel({
  title,
  icon,
  sub,
  children,
  right,
}: {
  title?: string;
  icon?: string;
  sub?: string;
  children: ReactNode;
  right?: ReactNode;
}) {
  return (
    <div className="panel">
      {(title || right) && (
        <div className="panel-head">
          <div>
            {title && (
              <div className="panel-title">
                {icon && <span>{icon}</span>}
                {title}
              </div>
            )}
            {sub && <div className="panel-sub">{sub}</div>}
          </div>
          {right}
        </div>
      )}
      {children}
    </div>
  );
}

export function KV({ k, v }: { k: string; v: ReactNode }) {
  return (
    <div className="kv">
      <span className="muted">{k}</span>
      <b>{v}</b>
    </div>
  );
}

export function toneColor(tone?: LogEntry['tone']): string {
  switch (tone) {
    case 'good':
      return 'var(--green)';
    case 'bad':
      return 'var(--red)';
    case 'warn':
      return 'var(--amber)';
    case 'cmd':
      return 'var(--cyan)';
    default:
      return 'var(--text)';
  }
}
