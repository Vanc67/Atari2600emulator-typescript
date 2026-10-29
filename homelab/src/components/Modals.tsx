import type { Dispatch } from 'react';
import { LOCATION_INFO, STAGE_INFO } from '../game/data';
import { formatMoney } from '../game/rules';
import type { Action, DayReport, GameEvent, GameState } from '../game/types';
import { Chip, Modal, Panel } from './ui';

export function EventModal({ event, state, dispatch }: { event: GameEvent; state: GameState; dispatch: Dispatch<Action> }) {
  return (
    <Modal>
      <div className="modal-title">
        <span className="big">{event.icon}</span> {event.title}
      </div>
      <div className="row" style={{ margin: '6px 0 12px' }}>
        <Chip tone={event.tone === 'bad' ? 'red' : event.tone === 'good' ? 'green' : event.tone === 'warn' ? 'amber' : 'cyan'}>
          событие дня {state.day}
        </Chip>
        <Chip>{formatMoney(state.money)}</Chip>
      </div>
      <p style={{ color: '#c6d6e7', fontSize: 13.5, lineHeight: 1.55 }}>{event.text}</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 14 }}>
        {event.choices.map((c, i) => {
          const disabled = !!c.cost && state.money < c.cost;
          return (
            <button key={i} className="choice" disabled={disabled} onClick={() => dispatch({ type: 'EVENT_CHOICE', index: i })}>
              <span className="choice-label">{c.label}</span>
              <span className="choice-hint">
                {c.hint ?? ''} {disabled ? '· не хватает денег' : ''}
              </span>
            </button>
          );
        })}
      </div>
    </Modal>
  );
}

export function ReportModal({ report, state, dispatch }: { report: DayReport; state: GameState; dispatch: Dispatch<Action> }) {
  return (
    <Modal>
      <div className="modal-title">
        <span className="big">📅</span> Итоги дня {report.day - 1}
      </div>
      <div className="row" style={{ margin: '8px 0 12px' }}>
        <Chip tone="green">доход {formatMoney(report.income)}</Chip>
        <Chip tone="red">расход {formatMoney(report.expenses)}</Chip>
        <Chip tone={report.income - report.expenses >= 0 ? 'green' : 'red'}>
          итог {formatMoney(report.income - report.expenses)}
        </Chip>
        <Chip>
          клиентов обслужено: {report.served} / {report.served + report.failedContracts.length}
        </Chip>
      </div>

      {report.lines.length > 0 && (
        <div style={{ marginBottom: 12 }}>
          <div className="tiny dim mono" style={{ marginBottom: 4 }}>
            ДВИЖЕНИЕ ДЕНЕГ
          </div>
          {report.lines.map((l, i) => (
            <div className="kv" key={i}>
              <span className="muted">{l.label}</span>
              <b style={{ color: l.amount >= 0 ? 'var(--green)' : 'var(--red)' }}>{formatMoney(l.amount)}</b>
            </div>
          ))}
        </div>
      )}

      {report.failedContracts.length > 0 && (
        <div className="panel" style={{ padding: 10, borderColor: '#63232b', background: '#180d10', marginBottom: 10 }}>
          <div className="tiny" style={{ color: 'var(--red)' }}>
            ❌ Сорваны: {report.failedContracts.join(', ')}. Клиенты не получили сервис — проверь железо и свободную память.
          </div>
        </div>
      )}

      <div className="tiny dim" style={{ marginBottom: 12 }}>
        Свет: {formatMoney(report.electric)} за электричество. Баланс: {formatMoney(state.money)}. Семья: {Math.round(state.family)} / 100.
        Репутация: {Math.round(state.rep)} / 100.
      </div>

      <button
        className="btn btn-primary btn-wide"
        onClick={() => {
          dispatch({ type: 'CLOSE_REPORT' });
          if (!report.events.length) return;
        }}
      >
        {state.event ? 'Дальше →' : 'Продолжить'}
      </button>
    </Modal>
  );
}

export function HelpModal({ onClose }: { onClose: () => void }) {
  return (
    <Modal onClose={onClose} wide>
      <div className="modal-title" style={{ marginBottom: 8 }}>
        <span className="big">🎮</span> Как играть
      </div>
      <p className="small" style={{ color: '#c6d6e7' }}>
        Ты — человек, который решил построить домашний сервер из барахла с Авито. Никаких кнопок «улучшить процессор»: покупаешь
        железо, собираешь руками, ставишь ОС, работаешь в терминале, берёшь заказы и следишь, чтобы сервер не сгорел, а мама не
        выдернула шнур.
      </p>
      <div className="grid2" style={{ marginTop: 10 }}>
        <Panel title="Цикл игры" icon="🔄">
          <ol className="small" style={{ paddingLeft: 18, color: '#b7c8db', margin: 0, lineHeight: 1.7 }}>
            <li>Барахолка: купи платформу, CPU, память, диск.</li>
            <li>Сборка: поставь детали, включи, выбери ОС.</li>
            <li>Сервисы: на Linux — командами `apt install`, на Windows — мышкой.</li>
            <li>Заказы: заключи договор, получай оплату каждый день.</li>
            <li>Заверши день: свет, шум, аварии, события и новые заказы.</li>
          </ol>
        </Panel>
        <Panel title="Что важно" icon="⚠️">
          <div className="small" style={{ display: 'flex', flexDirection: 'column', gap: 5, color: '#b7c8db' }}>
            <div>🌡️ Перегрев (85 °C+) валит серверы. Чисти пыль, ставь кулеры и кондиционер.</div>
            <div>🧠 Память: ОС сама съедает 512 МБ (Linux) или 2 ГБ (Windows).</div>
            <div>🔊 Шум бесит семью и соседей. Шумоизоляция и тихие кулеры спасают.</div>
            <div>⚡ Электричество списывается каждый день, счётчик не обманешь.</div>
            <div>📉 Сорванный договор = неустойка и −репутация.</div>
            <div>🛡️ Firewall = апгрейды + навык сети + защита серверов.</div>
          </div>
        </Panel>
      </div>
      <div className="hr" />
      <div className="grid3">
        {(Object.keys(STAGE_INFO) as unknown as (keyof typeof STAGE_INFO)[]).map((k) => (
          <div key={String(k)} className="meter-tile">
            <div className="tile-label">
              {STAGE_INFO[k].icon} Этап {String(k)}
            </div>
            <div className="tile-value" style={{ fontSize: 14 }}>
              {STAGE_INFO[k].name}
            </div>
            <div className="tiny dim">{STAGE_INFO[k].subtitle}</div>
          </div>
        ))}
      </div>
      <div className="hr" />
      <div className="small" style={{ color: '#b7c8db', display: 'flex', flexDirection: 'column', gap: 4 }}>
        <div>
          <b>Горячие клавиши терминала:</b> ↑/↓ — история команд, Enter — выполнить. `help` — список всех команд.
        </div>
        <div>
          <b>Локации:</b> {Object.values(LOCATION_INFO).map((l) => `${l.icon} ${l.name}`).join(' · ')} — влияют на шум, температуру и
          аренду.
        </div>
        <div>Прогресс сохраняется автоматически в браузере. Кнопка ⟲ в шапке начинает заново.</div>
      </div>
      <div style={{ marginTop: 14, textAlign: 'right' }}>
        <button className="btn btn-primary" onClick={onClose}>
          Понятно, играем
        </button>
      </div>
    </Modal>
  );
}

export function VictoryModal({ state, onClose }: { state: GameState; onClose: () => void }) {
  return (
    <Modal onClose={onClose}>
      <div className="modal-title">
        <span className="big">🏆</span> Ты построил ЦОД
      </div>
      <p className="small" style={{ color: '#c6d6e7' }}>
        День {state.day}. Репутация {Math.round(state.rep)}, контрактов выполнено {state.stats.contractsDone}, отбито{' '}
        {state.stats.ddosBlocked} DDoS-пакетов, обжато {state.stats.cablesCrimped} кабелей. Из комнаты с родительским ПК — до
        гермозоны с BGP и генератором. Мама, кстати, тобой гордится.
      </p>
      <button className="btn btn-primary btn-wide" onClick={onClose} style={{ marginTop: 10 }}>
        Продолжить строить империю
      </button>
    </Modal>
  );
}

export function GameOverModal({ state, onClose }: { state: GameState; onClose: () => void }) {
  return (
    <Modal onClose={onClose}>
      <div className="modal-title">
        <span className="big">💀</span> Долги тебя догнали
      </div>
      <p className="small" style={{ color: '#c6d6e7' }}>
        Баланс {formatMoney(state.money)}. Банк забрал стойку, провайдер отключил канал, мама забрала шнур питания. Ничего — у
        настоящих сисадминов тоже бывают плохие кварталы.
      </p>
      <button className="btn btn-primary btn-wide" onClick={onClose} style={{ marginTop: 10 }}>
        Начать заново
      </button>
    </Modal>
  );
}
