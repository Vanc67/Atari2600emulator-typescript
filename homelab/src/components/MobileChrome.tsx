import type { Dispatch, ReactNode } from 'react';
import {
  Compass,
  Home,
  ListChecks,
  Menu,
  MoonStar,
  Server,
  ShoppingCart,
  X,
} from 'lucide-react';
import { STAGE_INFO } from '../game/data';
import { firewallScore, formatMoney, totalNoise, totalWatts } from '../game/rules';
import type { Action, GameState, TabId } from '../game/types';
import { Chip, Meter } from './ui';
import { haptic } from '../hooks/useMedia';

export const TAB_META: { id: TabId; label: string; short: string; icon: ReactNode }[] = [
  { id: 'room', label: 'Комната', short: 'Комната', icon: <Home size={20} /> },
  { id: 'server', label: 'Машины и ОС', short: 'Сервер', icon: <Server size={20} /> },
  { id: 'market', label: 'Барахолка', short: 'Авито', icon: <ShoppingCart size={20} /> },
  { id: 'jobs', label: 'Заказы', short: 'Заказы', icon: <ListChecks size={20} /> },
  { id: 'path', label: 'Путь', short: 'Путь', icon: <Compass size={20} /> },
];

/** Верхняя панель телефона: деньги, день, репутация + меню. */
export function MobileHeader({
  state,
  onMenu,
  onNextDay,
}: {
  state: GameState;
  onMenu: () => void;
  onNextDay: () => void;
}) {
  const stage = STAGE_INFO[state.stage];
  return (
    <header className="mhead">
      <button className="mhead-icon" onClick={onMenu} aria-label="Меню">
        <Menu size={20} />
      </button>
      <button className="mhead-money" onClick={onMenu} aria-label="Баланс и подробности">
        <span className="mhead-label">Баланс</span>
        <span className="mhead-value" style={{ color: state.money < 0 ? 'var(--red)' : 'var(--green)' }}>
          {formatMoney(state.money)}
        </span>
      </button>
      <div className="mhead-day">
        <span className="mhead-label">
          {stage.icon} этап {state.stage}
        </span>
        <span className="mhead-value">День {state.day}</span>
      </div>
      <button className="mhead-icon mhead-next" onClick={onNextDay} aria-label="Завершить день">
        <MoonStar size={20} />
      </button>
    </header>
  );
}

/** Нижняя навигация + большая кнопка «завершить день». */
export function BottomNav({
  state,
  dispatch,
  badge,
}: {
  state: GameState;
  dispatch: Dispatch<Action>;
  badge: boolean;
}) {
  return (
    <nav className="bottomnav">
      {TAB_META.map((t) => (
        <button
          key={t.id}
          className={`bnav-btn ${state.tab === t.id ? 'active' : ''}`}
          onClick={() => {
            haptic(8);
            dispatch({ type: 'SET_TAB', tab: t.id });
          }}
        >
          <span className="bnav-ico">{t.icon}</span>
          <span className="bnav-label">{t.short}</span>
          {t.id === 'jobs' && badge && <span className="bnav-dot" />}
        </button>
      ))}
    </nav>
  );
}

/** Круглая кнопка «следующий день» над нижней навигацией. */
export function DayFab({ state, dispatch }: { state: GameState; dispatch: Dispatch<Action> }) {
  return (
    <button
      className="dayfab"
      onClick={() => {
        haptic(18);
        dispatch({ type: 'NEXT_DAY' });
      }}
    >
      <MoonStar size={22} />
      <span>День</span>
      {state.stage > 1 && <span className="dayfab-stage">{state.stage}</span>}
    </button>
  );
}

/** Шторка-меню телефона: подробная статистика, журнал, правила, сброс. */
export function MobileMenu({
  state,
  dispatch,
  open,
  onClose,
  onHelp,
  onReset,
  logSlot,
  installSlot,
}: {
  state: GameState;
  dispatch: Dispatch<Action>;
  open: boolean;
  onClose: () => void;
  onHelp: () => void;
  onReset: () => void;
  logSlot: ReactNode;
  installSlot?: ReactNode;
}) {
  if (!open) return null;
  return (
    <div className="sheet-overlay" onClick={onClose}>
      <div className="sheet sheet-menu" onClick={(e) => e.stopPropagation()}>
        <div className="sheet-grip" />
        <div className="sheet-head">
          <div className="panel-title">🗄️ HomeLab · день {state.day}</div>
          <button className="sheet-close" onClick={onClose} aria-label="Закрыть">
            <X size={18} />
          </button>
        </div>

        <div className="grid2" style={{ marginBottom: 10 }}>
          <div className="meter-tile">
            <div className="tile-label">Репутация</div>
            <div className="tile-value" style={{ color: 'var(--cyan)' }}>
              {Math.round(state.rep)} / 100
            </div>
            <Meter value={state.rep} max={100} tone="cyan" />
          </div>
          <div className="meter-tile">
            <div className="tile-label">Семья</div>
            <div className="tile-value" style={{ color: 'var(--pink)' }}>
              {Math.round(state.family)} / 100
            </div>
            <Meter value={state.family} max={100} tone={state.family < 35 ? 'red' : 'violet'} />
          </div>
        </div>

        <div className="row" style={{ marginBottom: 10 }}>
          <Chip tone="amber">⚡ {totalWatts(state)} Вт</Chip>
          <Chip tone="cyan">🛡️ защита {firewallScore(state)}</Chip>
          <Chip tone={totalNoise(state) > 45 ? 'red' : 'green'}>🔊 {Math.round(totalNoise(state))} дБ</Chip>
          <Chip>🔌 кабелей {state.cables}</Chip>
          <Chip>📦 деталей {state.inventory.length}</Chip>
        </div>

        <div className="row" style={{ marginBottom: 10 }}>
          <Chip tone="violet">клавиатура: {state.skills.hardware}</Chip>
          <Chip tone="violet">сети: {state.skills.network}</Chip>
          <Chip tone="violet">админ: {state.skills.sysadmin}</Chip>
        </div>

        <div className="sheet-log">{logSlot}</div>

        <div className="sheet-actions">
          {installSlot}
          <button className="btn" onClick={onHelp}>
            ❓ Правила
          </button>
          <button className="btn btn-danger" onClick={onReset}>
            ⟲ Новая игра
          </button>
          <button className="btn btn-primary" onClick={() => { dispatch({ type: 'TOGGLE_HELP', open: false }); onClose(); }}>
            Продолжить
          </button>
        </div>
      </div>
    </div>
  );
}
