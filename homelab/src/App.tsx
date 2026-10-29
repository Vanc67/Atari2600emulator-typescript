import { useEffect, useMemo, useReducer, useRef, useState } from 'react';
import { Bug, Compass, Home, ListChecks, Server, ShoppingCart } from 'lucide-react';
import { HUD } from './components/HUD';
import { BottomNav, DayFab, MobileHeader, MobileMenu, TAB_META } from './components/MobileChrome';
import { RoomTab } from './components/RoomTab';
import { MarketTab } from './components/MarketTab';
import { ServerTab } from './components/ServerTab';
import { JobsTab } from './components/JobsTab';
import { PathTab } from './components/PathTab';
import { HaggleModal } from './components/HaggleModal';
import { CableGame, DdosGame } from './components/MiniGames';
import { EventModal, GameOverModal, HelpModal, ReportModal, VictoryModal } from './components/Modals';
import { Panel } from './components/ui';
import { contractReady, createInitialState, reducer, SAVE_KEY, SAVE_VERSION_CURRENT } from './game/game';
import { firewallScore } from './game/rules';
import { haptic, useIsMobile } from './hooks/useMedia';
import { useInstallPrompt } from './hooks/usePwa';
import { bindBackButton, initNativeShell } from './native';
import type { Action, GameState, TabId } from './game/types';

const TAB_ICONS: Record<TabId, React.ReactNode> = {
  room: <Home size={15} />,
  server: <Server size={15} />,
  market: <ShoppingCart size={15} />,
  jobs: <ListChecks size={15} />,
  path: <Compass size={15} />,
};

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

function LogList({ state }: { state: GameState }) {
  return (
    <div className="log-list">
      {state.log.map((l) => (
        <div key={l.id} className={`log-item ${l.tone}`}>
          <span className="log-day">Д{l.day}</span>
          {l.text}
        </div>
      ))}
    </div>
  );
}

export default function App() {
  const [state, dispatch] = useReducer(reducer, undefined, loadState);
  const isMobile = useIsMobile();
  const { canInstall, install } = useInstallPrompt();

  const [toastVisible, setToastVisible] = useState(false);
  const [haggleId, setHaggleId] = useState<string | null>(null);
  const [cableOpen, setCableOpen] = useState(false);
  const [ddosOpen, setDdosOpen] = useState(false);
  const [victorySeen, setVictorySeen] = useState(false);
  const [gameOverSeen, setGameOverSeen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    document.documentElement.classList.toggle('mobile-shell', isMobile);
    void initNativeShell();
  }, [isMobile]);

  // Кнопка «Назад» на Android: сначала закрываем окна, потом возвращаемся в «Комнату»
  useEffect(() => {
    const closer = () => {
      if (state.helpOpen) {
        dispatch({ type: 'TOGGLE_HELP', open: false });
        return true;
      }
      if (state.report) {
        dispatch({ type: 'CLOSE_REPORT' });
        return true;
      }
      if (haggleId) {
        setHaggleId(null);
        return true;
      }
      if (cableOpen) {
        setCableOpen(false);
        return true;
      }
      if (ddosOpen) return true;
      if (menuOpen) {
        setMenuOpen(false);
        return true;
      }
      if (state.tab !== 'room') {
        dispatch({ type: 'SET_TAB', tab: 'room' });
        return true;
      }
      return false;
    };
    let dispose = () => undefined as void;
    void bindBackButton(closer).then((d) => {
      dispose = d;
    });
    return () => dispose();
  }, [state.helpOpen, state.report, state.tab, haggleId, cableOpen, ddosOpen, menuOpen]);

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
  const readyOffers = useMemo(() => state.offers.filter((o) => contractReady(state, o)).length, [state]);

  const dispatchAction = (a: Action) => dispatch(a);

  /* Свайп между разделами на телефоне */
  const touchStart = useRef<{ x: number; y: number; blocked: boolean } | null>(null);
  const onTouchStart = (e: React.TouchEvent) => {
    const t = e.touches[0];
    const el = e.target as HTMLElement;
    const blocked = !!el.closest('[data-noswipe], input, textarea, .term-body, .chat, .term-hints, .bottomnav, .slots');
    touchStart.current = { x: t.clientX, y: t.clientY, blocked };
  };
  const onTouchEnd = (e: React.TouchEvent) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start || start.blocked || !isMobile) return;
    const t = e.changedTouches[0];
    const dx = t.clientX - start.x;
    const dy = t.clientY - start.y;
    if (Math.abs(dx) < 70 || Math.abs(dy) > 55) return;
    const order = TAB_META.map((x) => x.id);
    const idx = order.indexOf(state.tab);
    const next = order[Math.min(order.length - 1, Math.max(0, idx + (dx < 0 ? 1 : -1)))];
    if (next !== state.tab) {
      haptic(8);
      dispatch({ type: 'SET_TAB', tab: next });
    }
  };

  return (
    <div className={`app ${isMobile ? 'is-mobile' : ''}`}>
      {isMobile ? (
        <MobileHeader state={state} onMenu={() => setMenuOpen(true)} onNextDay={() => dispatch({ type: 'NEXT_DAY' })} />
      ) : (
        <HUD state={state} dispatch={dispatchAction} />
      )}

      {!isMobile && (
        <nav className="tabs">
          {TAB_META.map((t) => (
            <button key={t.id} className={`tab ${state.tab === t.id ? 'active' : ''}`} onClick={() => dispatch({ type: 'SET_TAB', tab: t.id })}>
              {TAB_ICONS[t.id]}
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
      )}

      <div className="layout">
        <main onTouchStart={onTouchStart} onTouchEnd={onTouchEnd}>
          {state.tab === 'room' && <RoomTab state={state} dispatch={dispatchAction} onCable={() => setCableOpen(true)} />}
          {state.tab === 'server' && <ServerTab state={state} dispatch={dispatchAction} onCable={() => setCableOpen(true)} />}
          {state.tab === 'market' && <MarketTab state={state} dispatch={dispatchAction} onHaggle={setHaggleId} />}
          {state.tab === 'jobs' && <JobsTab state={state} dispatch={dispatchAction} />}
          {state.tab === 'path' && <PathTab state={state} dispatch={dispatchAction} />}
        </main>

        {!isMobile && (
          <aside className="log-panel panel">
            <div className="panel-head" style={{ marginBottom: 8 }}>
              <div className="panel-title">📟 Журнал</div>
              <span className="tiny dim mono">день {state.day}</span>
            </div>
            <LogList state={state} />
            <div className="hr" />
            <Panel title="Как это работает" icon="💡">
              <div className="tiny dim" style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                <div>🔌 Патч-корды нужны для сетевых заказов — обжимай вручную.</div>
                <div>⏭ «Завершить день» — списание электричества, оплата клиентов, события.</div>
                <div>💾 Прогресс сохраняется автоматически.</div>
              </div>
            </Panel>
          </aside>
        )}
      </div>

      {!isMobile && (
        <div className="footer-note">
          HomeLab: От Авито до Дата-Центра · симулятор домашнего сервера · `help` в терминале — твой лучший друг
        </div>
      )}

      {isMobile && (
        <>
          <DayFab state={state} dispatch={dispatchAction} />
          <BottomNav state={state} dispatch={dispatchAction} badge={activeContracts > 0 || readyOffers > 0} />
          <MobileMenu
            state={state}
            dispatch={dispatchAction}
            open={menuOpen}
            onClose={() => setMenuOpen(false)}
            onHelp={() => {
              setMenuOpen(false);
              dispatch({ type: 'TOGGLE_HELP', open: true });
            }}
            onReset={() => {
              if (confirm('Начать игру заново? Текущий прогресс будет потерян.')) {
                setMenuOpen(false);
                dispatch({ type: 'RESET' });
              }
            }}
            logSlot={<LogList state={state} />}
            installSlot={
              canInstall ? (
                <button
                  className="btn btn-amber"
                  onClick={async () => {
                    await install();
                    setMenuOpen(false);
                  }}
                >
                  ⬇️ Установить приложение
                </button>
              ) : null
            }
          />
        </>
      )}

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

      {state.toast && toastVisible && (
        <div className="toasts">
          <div className={`toast ${state.toast.tone}`}>{state.toast.text}</div>
        </div>
      )}
    </div>
  );
}
