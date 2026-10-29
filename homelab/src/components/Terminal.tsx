import { useEffect, useRef, useState } from 'react';
import type { Dispatch } from 'react';
import { ArrowDown, ArrowUp, CornerDownLeft, Eraser } from 'lucide-react';
import { COMMAND_HINTS } from '../game/commands';
import { haptic } from '../hooks/useMedia';
import type { Action, GameState, Machine } from '../game/types';

interface Line {
  text: string;
  tone?: string;
}

export function Terminal({ machine, state, dispatch }: { machine: Machine; state: GameState; dispatch: Dispatch<Action> }) {
  const host = machine.name.toLowerCase().replace(/\s+/g, '-');
  const [lines, setLines] = useState<Line[]>([
    { text: 'Ubuntu Server 22.04.4 LTS (GNU/Linux 5.15.0-91-generic x86_64)', tone: 'good' },
    { text: ' * Documentation:  https://help.ubuntu.com', tone: 'dim' },
    { text: 'Введи `help`, чтобы увидеть команды. Терминал учебный, но влияет на игру по-настоящему.', tone: 'warn' },
  ]);
  const [input, setInput] = useState('');
  const [histIdx, setHistIdx] = useState(-1);
  const historyRef = useRef<string[]>([]);
  const lastOut = useRef<GameState['cmdOutput']>(null);
  const bodyRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (state.cmdOutput && state.cmdOutput !== lastOut.current) {
      lastOut.current = state.cmdOutput;
      const out = state.cmdOutput;
      if (!out || out.machineId !== machine.id) return;
      setLines((prev) => [
        ...prev,
        { text: `root@${host}:~# ${out.input}`, tone: 'cmd' },
        ...out.lines.map((l) => ({ text: l.text, tone: l.tone })),
      ]);
    }
  }, [state.cmdOutput, machine.id, host]);

  useEffect(() => {
    const el = bodyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [lines]);

  const run = (raw: string) => {
    const cmd = raw.trim();
    if (!cmd) return;
    historyRef.current.push(cmd);
    setHistIdx(-1);
    setInput('');
    haptic(8);
    if (cmd === 'clear') {
      setLines([]);
      return;
    }
    dispatch({ type: 'CMD', machineId: machine.id, input: cmd });
  };

  const stepHistory = (dir: -1 | 1) => {
    const h = historyRef.current;
    if (!h.length) return;
    const next = histIdx < 0 ? h.length - 1 : Math.min(h.length - 1, Math.max(0, histIdx + dir));
    if (dir === 1 && histIdx === h.length - 1) {
      setHistIdx(-1);
      setInput('');
      return;
    }
    setHistIdx(next);
    setInput(h[next] ?? '');
    haptic(5);
  };

  return (
    <div className="terminal">
      <div className="term-bar">
        <span className="term-dot" style={{ background: '#ff5f56' }} />
        <span className="term-dot" style={{ background: '#ffbd2e' }} />
        <span className="term-dot" style={{ background: '#27c93f' }} />
        <span style={{ marginLeft: 8, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          root@{host}
        </span>
        <span style={{ marginLeft: 'auto', whiteSpace: 'nowrap' }}>{machine.powered ? '● онлайн' : '○ выкл.'}</span>
      </div>
      <div className="term-body" ref={bodyRef} onClick={() => inputRef.current?.focus()}>
        {lines.map((l, i) => (
          <div key={i} className={`term-line ${l.tone ?? ''}`}>
            {l.text}
          </div>
        ))}
        {!machine.powered && <div className="term-line bad">Сервер выключен. Включи его кнопкой питания.</div>}
        {machine.crashed && <div className="term-line bad">Аварийное состояние: сначала ремонт.</div>}
      </div>
      <div className="term-input">
        <span className="term-prompt">#</span>
        <input
          ref={inputRef}
          value={input}
          spellCheck={false}
          autoComplete="off"
          autoCapitalize="none"
          autoCorrect="off"
          enterKeyHint="send"
          inputMode="text"
          placeholder="введи команду…"
          onChange={(e) => setInput(e.target.value)}
          onFocus={() => {
            setTimeout(() => {
              bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight });
            }, 150);
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') run(input);
            if (e.key === 'ArrowUp') {
              e.preventDefault();
              stepHistory(-1);
            }
            if (e.key === 'ArrowDown') {
              e.preventDefault();
              stepHistory(1);
            }
          }}
        />
        <div className="term-tools">
          <button className="btn btn-sm" aria-label="Предыдущая команда" onClick={() => stepHistory(-1)}>
            <ArrowUp size={15} />
          </button>
          <button className="btn btn-sm" aria-label="Следующая команда" onClick={() => stepHistory(1)}>
            <ArrowDown size={15} />
          </button>
          <button className="btn btn-sm btn-primary" aria-label="Выполнить" onClick={() => run(input)}>
            <CornerDownLeft size={15} />
          </button>
        </div>
      </div>
      <div className="term-hints">
        <button className="btn btn-sm" onClick={() => setLines([])} title="Очистить экран">
          <Eraser size={13} /> clear
        </button>
        {COMMAND_HINTS.map((c) => (
          <button
            key={c}
            className="btn btn-sm"
            style={{ padding: '3px 7px', fontSize: 11 }}
            onClick={() => {
              setInput(c);
              inputRef.current?.focus();
              haptic(5);
            }}
          >
            {c}
          </button>
        ))}
      </div>
    </div>
  );
}
