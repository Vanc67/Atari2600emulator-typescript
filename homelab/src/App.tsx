import { useEffect, useMemo, useReducer, useState } from 'react';
import { Cable, Home, ListChecks, Server, ShoppingCart, Compass, Bug } from 'lucide-react';
import { HUD } from './components/HUD';
import { RoomTab } from './components/RoomTab';
import { MarketTab } from './components/MarketTab';
import { ServerTab } from './components/ServerTab';
import { JobsTab } from './components/JobsTab';
import { PathTab } from './components/PathTab';
import { HaggleModal } from './components/HaggleModal';
import { CableGame, DdosGame } from './components/MiniGames';
import { EventModal, GameOverModal, HelpModal, ReportModal, VictoryModal } from './components/Modals';
import { Panel } from './components/ui';
import { createInitialState, reducer, SAVE_KEY, SAVE_VERSION_CURRENT } from './game/game';
import { firewallScore } from './game/rules';
import type { Action, GameState, TabId } from './game/types';

function loadState(): GameState {
  try {
    const raw = localStorage.getItem(SAVE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as GameState;
      if (parsed.version === SAVE_VERSION_CURRENT) {
        return { ...parsed, event: null, report: null, helpOpen: false };
      }
    }
  } catch {
    /* ignore */
  }
  const fresh = createInitialState();
  fresh.helpOpen = true;
  return fresh;
}

const TABS: { id: TabId; label: string; icon: React.ReactNode }[] = [
  { id: 'room', label: 'Комната', icon: <Home size={15} /> },
  { id: 'server', label: 'Машины и ОС', icon: <Server size={15} /> },
  { id: 'market', label: 'Барахолка', icon: <ShoppingCart size={15} /> },
  { id: 'jobs', label: 'Заказы', icon: <ListChecks size={15} /> },
  { id: 'path', label: 'Путь и апгрейды', icon: <Compass size={15} /> },
];

export default function App() {
  const [state, dispatch] = useReducer(reducer, undefined, loadState);

  const [toastVisible, setToastVisible] = useState(false);
  const [haggleId, setHaggleId] = useState<string | null>(null);
  const [cableOpen, setCableOpen] = useState(false);
  const [ddosOpen, setDdosOpen] = useState(false);
  const [victorySeen, setVictorySeen] = useState(false);
  const [gameOverSeen, setGameOverSeen] = useState(false);

  // Автосохранение
  useEffect(() => {
    try {
      localStorage.setItem(SAVE_KEY, JSON.stringify(state));
    } catch {
      /* quota */
    }
  }, [state]);

  // Тост
  useEffect(() => {
    if (!state.toast) return;
    setToastVisible(true);
    const t = setTimeout(() => setToastVisible(false), 3400);
    return () => clearTimeout(t);
  }, [state.toast?.id]);

  // Мини-игры, запущенные событиями
  useEffect(() => {
    if (state.flags.cable_pending && !cableOpen) setCableOpen(true);
    if (state.flags.ddos_pending && !ddosOpen) setDdosOpen(true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.flags.cable_pending, state.flags.ddos_pending]);

  const activeContracts = state.contracts.length;
  const readyOffers = useMemo(
    () => state.offers.filter((o) => !state.contracts.some((c) => c.id === o.id)).length,
    [state.offers, state.contracts],
  );

  const dispatchAction = (a: Action) => dispatch(a);

  return (
    <div className="app">
      <HUD state={state} dispatch={dispatchAction} />

      <nav className="tabs">
        {TABS.map((t) => (
          <button key={t.id} className={`tab ${state.tab === t.id ? 'active' : ''}`} onClick={() => dispatch({ type: 'SET_TAB', tab: t.id })}>
            {t.icon}
            {t.label}
            {t.id === 'jobs' && (activeContracts > 0 || readyOffers > 0) && <span className="badge-dot" />}
          </button>
        ))}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="chip cyan">🛡️ защита {firewallScore(state)}</span>
          <button
            className="btn btn-sm"
            onClick={() =>
              dispatch({
                type: 'TOAST',
                text: `День ${state.day}: ${state.machines.length} маш., ${state.inventory.length} деталей на складе, ${state.cables} кабелей`,
                tone: 'info',
              })
            }
            title="Краткая сводка"
          >
            <Bug size={14} /> Сводка
          </button>
        </div>
      </nav>

      <div className="layout">
        <main>
          {state.tab === 'room' && <RoomTab state={state} dispatch={dispatchAction} onCable={() => setCableOpen(true)} />}
          {state.tab === 'server' && <ServerTab state={state} dispatch={dispatchAction} onCable={() => setCableOpen(true)} />}
          {state.tab === 'market' && <MarketTab state={state} dispatch={dispatchAction} onHaggle={setHaggleId} />}
          {state.tab === 'jobs' && <JobsTab state={state} dispatch={dispatchAction} />}
          {state.tab === 'path' && <PathTab state={state} dispatch={dispatchAction} />}
        </main>

        <aside className="log-panel panel">
          <div className="panel-head" style={{ marginBottom: 8 }}>
            <div className="panel-title">📟 Журнал</div>
            <span className="tiny dim mono">день {state.day}</span>
          </div>
          <div className="log-list">
            {state.log.map((l) => (
              <div key={l.id} className={`log-item ${l.tone}`}>
                <span className="log-day">Д{l.day}</span>
                {l.text}
              </div>
            ))}
          </div>
          <div className="hr" />
          <Panel title="Как это работает" icon="💡">
            <div className="tiny dim" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <div>
                <Cable size={12} /> Патч-корды нужны для сетевых заказов — обжимай вручную.
              </div>
              <div>⏭ «Завершить день» — списание электричества, оплата клиентов, события.</div>
              <div>💾 Прогресс сохраняется автоматически.</div>
            </div>
          </Panel>
        </aside>
      </div>

      <div className="footer-note">
        HomeLab: От Авито до Дата-Центра · симулятор домашнего сервера · `help` в терминале — твой лучший друг
      </div>

      {/* ── Модальные окна ─────────────────────────────── */}
      {state.helpOpen && <HelpModal onClose={() => dispatch({ type: 'TOGGLE_HELP', open: false })} />}
      {state.report && <ReportModal report={state.report} state={state} dispatch={dispatchAction} />}
      {!state.report && state.event && <EventModal event={state.event} state={state} dispatch={dispatchAction} />}
      {haggleId && <HaggleModal state={state} dispatch={dispatchAction} listingId={haggleId} onClose={() => setHaggleId(null)} />}
      {cableOpen && (
        <CableGame
          onClose={() => {
            setCableOpen(false);
            dispatch({ type: 'TOAST', text: 'Ты отложил кримпер на потом', tone: 'warn' });
          }}
          onDone={(perfect) => {
            dispatch({ type: 'CABLE_DONE', perfect });
            setCableOpen(false);
          }}
        />
      )}
      {ddosOpen && (
        <DdosGame
          firewall={firewallScore(state)}
          onClose={() => {
            setDdosOpen(false);
            dispatch({ type: 'DDOS_RESULT', blocked: 0, total: 32 });
          }}
          onDone={(blocked, total) => {
            dispatch({ type: 'DDOS_RESULT', blocked, total });
            setDdosOpen(false);
          }}
        />
      )}
      {state.victory && !victorySeen && <VictoryModal state={state} onClose={() => setVictorySeen(true)} />}
      {state.gameOver && !gameOverSeen && (
        <GameOverModal
          state={state}
          onClose={() => {
            setGameOverSeen(true);
            dispatch({ type: 'RESET' });
          }}
        />
      )}

      {state.toast && toastVisible && <div className="toasts"><div className={`toast ${state.toast.tone}`}>{state.toast.text}</div></div>}
    </div>
  );
}
