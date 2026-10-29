import { useEffect, useRef, useState } from 'react';
import type { Dispatch } from 'react';
import { COMMAND_HINTS } from '../game/commands';
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
    { text: ' * Management:     https://landscape.canonical.com', tone: 'dim' },
    { text: 'Введи `help`, чтобы увидеть команды. Этот терминал учебный, но делает всё по-настоящему.', tone: 'warn' },
  ]);
  const [input, setInput] = useState('');
  const [histIdx, setHistIdx] = useState(-1);
  const historyRef = useRef<string[]>([]);
  const lastOut = useRef<GameState['cmdOutput']>(null);
  const bodyRef = useRef<HTMLDivElement>(null);

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
    if (cmd === 'clear') {
      setLines([]);
      return;
    }
    dispatch({ type: 'CMD', machineId: machine.id, input: cmd });
  };

  return (
    <div className="terminal">
      <div className="term-bar">
        <span className="term-dot" style={{ background: '#ff5f56' }} />
        <span className="term-dot" style={{ background: '#ffbd2e' }} />
        <span className="term-dot" style={{ background: '#27c93f' }} />
        <span style={{ marginLeft: 8 }}>
          root@{host}: ~ — bash
        </span>
        <span style={{ marginLeft: 'auto' }}>
          {machine.powered ? '● ssh 192.168.1.42' : '○ питание выключено'}
        </span>
      </div>
      <div className="term-body" ref={bodyRef}>
        {lines.map((l, i) => (
          <div key={i} className={`term-line ${l.tone ?? ''}`}>
            {l.text}
          </div>
        ))}
        {!machine.powered && <div className="term-line bad">Сервер выключен. Включи его кнопкой питания.</div>}
        {machine.crashed && <div className="term-line bad">Аварийное состояние: сначала ремонт.</div>}
      </div>
      <div className="term-input">
        <span className="term-prompt">root@{host}:~#</span>
        <input
          value={input}
          spellCheck={false}
          autoComplete="off"
          placeholder="введи команду..."
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') run(input);
            if (e.key === 'ArrowUp') {
              e.preventDefault();
              const h = historyRef.current;
              if (!h.length) return;
              const next = histIdx < 0 ? h.length - 1 : Math.max(0, histIdx - 1);
              setHistIdx(next);
              setInput(h[next] ?? '');
            }
            if (e.key === 'ArrowDown') {
              e.preventDefault();
              const h = historyRef.current;
              if (histIdx < 0) return;
              const next = histIdx + 1;
              if (next >= h.length) {
                setHistIdx(-1);
                setInput('');
              } else {
                setHistIdx(next);
                setInput(h[next] ?? '');
              }
            }
          }}
        />
      </div>
      <div className="term-hints">
        {COMMAND_HINTS.map((c) => (
          <button key={c} className="btn btn-sm" style={{ padding: '3px 7px', fontSize: 11 }} onClick={() => setInput(c)}>
            {c}
          </button>
        ))}
      </div>
    </div>
  );
}
