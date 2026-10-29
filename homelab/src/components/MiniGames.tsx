import { useEffect, useState } from 'react';
import { Modal } from './ui';

/* ─── Обжим кабеля: повтори порядок цветов T568B ─────────────────────── */

const COLORS = [
  { id: 'ow', name: 'бело-оранжевый', bg: '#f0a563' },
  { id: 'o', name: 'оранжевый', bg: '#ef7c1e' },
  { id: 'gw', name: 'бело-зелёный', bg: '#7bdca6' },
  { id: 'b', name: 'синий', bg: '#3d7bea' },
  { id: 'bw', name: 'бело-синий', bg: '#8fb8ff' },
  { id: 'g', name: 'зелёный', bg: '#25b06a' },
  { id: 'brw', name: 'бело-коричневый', bg: '#c79b7c' },
  { id: 'br', name: 'коричневый', bg: '#8a5a35' },
];

const T568B = ['ow', 'o', 'gw', 'b', 'bw', 'g', 'brw', 'br'];

export function CableGame({ onDone, onClose }: { onDone: (perfect: boolean) => void; onClose: () => void }) {
  const [placed, setPlaced] = useState<string[]>([]);
  const [errors, setErrors] = useState(0);
  const [wrongIdx, setWrongIdx] = useState<number | null>(null);
  const [finished, setFinished] = useState(false);

  const next = T568B[placed.length] ?? null;

  const click = (id: string) => {
    if (finished || !next) return;
    if (id === next) {
      setPlaced((p) => [...p, id]);
      setWrongIdx(null);
      if (placed.length + 1 === T568B.length) setFinished(true);
    } else {
      setErrors((e) => e + 1);
      setWrongIdx(COLORS.findIndex((c) => c.id === id));
      setTimeout(() => setWrongIdx(null), 400);
    }
  };

  return (
    <Modal onClose={onClose}>
      <div className="modal-title" style={{ marginBottom: 4 }}>
        <span className="big">🔧</span> Обжим патч-корда
      </div>
      <div className="tiny dim" style={{ marginBottom: 10 }}>
        Схема T568B, слева направо. Ошибёшься — кабель придётся переделывать (и клиент это заметит).
      </div>

      <div className="row" style={{ marginBottom: 8 }}>
        <span className="tiny dim mono">НУЖНЫЙ ПОРЯДОК:</span>
        {T568B.map((id, i) => (
          <span
            key={id + i}
            className="chip"
            style={{ background: COLORS.find((c) => c.id === id)?.bg, color: '#111', borderColor: 'transparent', fontWeight: 800 }}
          >
            {i + 1}
          </span>
        ))}
      </div>

      <div className="wire">
        {COLORS.map((c, i) => (
          <div
            key={c.id}
            className={`wire-cell ${wrongIdx === i ? 'wrong' : ''}`}
            style={{ background: c.bg }}
            title={c.name}
            onClick={() => click(c.id)}
          >
            {placed.includes(c.id) ? placed.indexOf(c.id) + 1 : ''}
          </div>
        ))}
      </div>

      <div className="row" style={{ marginTop: 10 }}>
        <span className="chip cyan">уложено {placed.length} / 8</span>
        <span className={`chip ${errors ? 'red' : 'green'}`}>ошибок {errors}</span>
        <div style={{ flex: 1 }} />
        {finished ? (
          <button className="btn btn-primary" onClick={() => onDone(errors === 0)}>
            ✅ Обжать и закрыть (всё {errors === 0 ? 'идеально' : 'работает'})
          </button>
        ) : (
          <button className="btn btn-sm" onClick={() => setPlaced([])}>
            Начать заново
          </button>
        )}
      </div>
      <div className="tiny dim" style={{ marginTop: 8 }}>
        Порядок T568B: бело-оранжевый, оранжевый, бело-зелёный, синий, бело-синий, зелёный, бело-коричневый, коричневый.
      </div>
    </Modal>
  );
}

/* ─── DDoS: отфильтруй плохие пакеты ─────────────────────────────────── */

interface Packet {
  id: number;
  bad: boolean;
  label: string;
  dead: boolean;
}

export function DdosGame({
  firewall,
  onDone,
  onClose,
}: {
  firewall: number;
  onDone: (blocked: number, total: number) => void;
  onClose: () => void;
}) {
  const total = 32;
  const secondsLimit = 12 + Math.floor(firewall / 8);
  const [state, setState] = useState<Packet[]>(() =>
    Array.from({ length: total }, (_, i) => {
      const bad = Math.random() < 0.55;
      return { id: i, bad, dead: false, label: bad ? 'SYN' : 'GET' };
    }),
  );
  const [seconds, setSeconds] = useState(secondsLimit);
  const [running, setRunning] = useState(true);

  useEffect(() => {
    if (!running) return;
    const t = setInterval(() => {
      setSeconds((s) => {
        if (s <= 1) {
          setRunning(false);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(t);
  }, [running]);

  const blocked = state.filter((p) => p.bad && p.dead).length;
  const badTotal = state.filter((p) => p.bad).length;
  const passed = state.filter((p) => !p.bad && !p.dead).length;
  const honest = state.filter((p) => !p.bad).length;
  const mistakes = state.filter((p) => !p.bad && p.dead).length;

  const toggle = (id: number) => {
    if (!running) return;
    setState((prev) => prev.map((p) => (p.id === id ? { ...p, dead: !p.dead } : p)));
  };

  return (
    <Modal onClose={onClose} wide>
      <div className="modal-title" style={{ marginBottom: 4 }}>
        <span className="big">💥</span> DDoS-атака: фильтрация трафика
      </div>
      <div className="tiny dim" style={{ marginBottom: 10 }}>
        Кликай по <b>вредоносным пакетам</b> (SYN-флуд), чтобы заблокировать их. Легитимные запросы (GET) не трогай — иначе уронишь
        честных клиентов. Твой firewall даёт {secondsLimit} секунд.
      </div>

      <div className="row" style={{ marginBottom: 10 }}>
        <span className={`chip ${seconds < 4 ? 'red' : 'amber'}`}>⏱ {seconds} с</span>
        <span className="chip green">заблокировано {blocked} / {badTotal}</span>
        <span className="chip cyan">пропущено нормальных {passed} / {honest}</span>
        <span className={`chip ${mistakes ? 'red' : 'green'}`}>ошибок {mistakes}</span>
      </div>

      <div className="packet-grid">
        {state.map((p) => (
          <div
            key={p.id}
            className={`packet ${p.bad ? 'bad' : 'good'} ${p.dead ? 'cleared' : ''}`}
            onClick={() => toggle(p.id)}
            title={p.bad ? 'SYN-флуд — блокировать' : 'Легитимный запрос — пропустить'}
          >
            {p.dead ? '✕' : p.label}
          </div>
        ))}
      </div>

      <div className="row" style={{ marginTop: 12 }}>
        <span className="tiny dim">Итог защиты: {blocked + passed} из {badTotal + honest} пакетов обработано верно.</span>
        <div style={{ flex: 1 }} />
        <button className="btn btn-primary" onClick={() => onDone(blocked + passed, badTotal + honest)}>
          {running ? 'Завершить раньше' : 'Завершить защиту'}
        </button>
      </div>
      <div className="tiny dim" style={{ marginTop: 8 }}>
        Нормально обработано должно быть ≥ 85%, чтобы клиент остался доволен. Если пропустить атаку — сайт ляжет и придётся платить
        неустойку.
      </div>
    </Modal>
  );
}
