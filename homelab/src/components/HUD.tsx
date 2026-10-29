import { HelpCircle, MoonStar, RotateCcw, Zap } from 'lucide-react';
import type { Dispatch } from 'react';
import { STAGE_INFO } from '../game/data';
import { formatMoney, totalWatts, firewallScore } from '../game/rules';
import type { Action, GameState } from '../game/types';

export function HUD({ state, dispatch }: { state: GameState; dispatch: Dispatch<Action> }) {
  const stages = STAGE_INFO[state.stage];
  return (
    <header className="hud">
      <div className="brand">
        <div className="brand-mark">🗄️</div>
        <div>
          <div className="brand-title">HOMELAB</div>
          <div className="brand-sub">от Авито до Дата-Центра</div>
        </div>
      </div>

      <div className="hud-stats">
        <div className="pill stage" title={stages.subtitle}>
          <span className="pill-ico">{stages.icon}</span>
          <div className="pill-body">
            <span className="pill-label">Этап {state.stage} · день {state.day}</span>
            <span className="pill-value">{stages.name}</span>
          </div>
        </div>

        <div className="pill money">
          <span className="pill-ico">💵</span>
          <div className="pill-body">
            <span className="pill-label">Баланс</span>
            <span className="pill-value">{formatMoney(state.money)}</span>
          </div>
        </div>

        <div className="pill rep" title="Репутация влияет на цену заказов и доступ к этапам">
          <span className="pill-ico">⭐</span>
          <div className="pill-body">
            <span className="pill-label">Репутация</span>
            <span className="pill-value">{Math.round(state.rep)} / 100</span>
          </div>
        </div>

        <div className="pill family" title="Если семья взбунтуется — будут штрафы">
          <span className="pill-ico">🏠</span>
          <div className="pill-body">
            <span className="pill-label">Семья</span>
            <span className="pill-value">{Math.round(state.family)} / 100</span>
          </div>
        </div>

        <div className="pill" title="Суммарное потребление включённых машин">
          <span className="pill-ico">
            <Zap size={15} color="var(--amber)" />
          </span>
          <div className="pill-body">
            <span className="pill-label">Мощность · защита</span>
            <span className="pill-value">
              {totalWatts(state)} Вт <span className="dim">/</span> <span style={{ color: 'var(--cyan)' }}>{firewallScore(state)}</span>
            </span>
          </div>
        </div>
      </div>

      <div className="hud-actions">
        <button className="btn btn-sm btn-ghost" onClick={() => dispatch({ type: 'TOGGLE_HELP', open: true })} title="Как играть">
          <HelpCircle size={15} /> Правила
        </button>
        <button
          className="btn btn-sm"
          onClick={() => {
            if (confirm('Начать игру заново? Текущий прогресс будет потерян.')) dispatch({ type: 'RESET' });
          }}
          title="Новая игра"
        >
          <RotateCcw size={15} />
        </button>
        <button className="btn btn-primary" onClick={() => dispatch({ type: 'NEXT_DAY' })} title="Завершить день">
          <MoonStar size={16} /> Завершить день
        </button>
      </div>
    </header>
  );
}
