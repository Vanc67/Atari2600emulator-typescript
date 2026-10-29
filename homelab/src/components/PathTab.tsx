import type { Dispatch } from 'react';
import { LOCATION_INFO, STAGE_INFO, UPGRADES } from '../game/data';
import { firewallScore, formatMoney, totalWatts } from '../game/rules';
import type { Action, GameState, Stage } from '../game/types';
import { Chip, Panel } from './ui';

const STAGE_GOALS: Record<Stage, string[]> = {
  1: [
    'Купить дешёвое железо на барахолке и поставить ОС',
    'Запустить первые сервисы: SMB, Jellyfin, веб',
    'Выполнить 3 контракта и набрать репутацию 15',
    'Накопить 6 000 ₽ — и переехать в гараж',
  ],
  2: [
    'Купить шкаф 12U и настоящий стоечный сервер',
    'Собрать RAID, сделать бэкапы и мониторинг',
    'Отбить первую DDoS-атаку и заработать репутацию 40',
    'Накопить 38 000 ₽ и арендовать помещение под ЦОД',
  ],
  3: [
    'Поднять блейд-шасси, СХД и 10G-магистраль',
    'Настроить BGP и фильтрацию трафика',
    'Держать аптайм и не терять клиентов',
    'Репутация 90+ и 25 выполненных контрактов — победа',
  ],
};

export function PathTab({ state, dispatch }: { state: GameState; dispatch: Dispatch<Action> }) {
  const stages: Stage[] = [1, 2, 3];
  const dcProgress = {
    contracts: `${state.stats.contractsDone} / 8`,
    rep: `${Math.round(state.rep)} / 35`,
    money: `${formatMoney(state.money)} / 38 000 ₽`,
  };

  return (
    <>
      <Panel title="Путь: от комнаты до ЦОДа" icon="🧭" sub="Три этапа, у каждого — свои цели и ограничения">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {stages.map((st) => {
            const info = STAGE_INFO[st];
            const current = state.stage === st;
            const locked = st > state.stage;
            return (
              <div key={st} className={`stage-card ${current ? 'current' : ''} ${locked ? 'locked' : ''}`}>
                <div className="stage-num">{st}</div>
                <div style={{ flex: 1 }}>
                  <div className="spread">
                    <div>
                      <div style={{ fontWeight: 800 }}>
                        {info.icon} Этап {st}: «{info.name}»
                      </div>
                      <div className="tiny dim">{info.subtitle}</div>
                    </div>
                    <div className="row">
                      <Chip tone={current ? 'green' : locked ? 'amber' : 'cyan'}>{current ? 'сейчас' : locked ? 'закрыто' : 'пройдено'}</Chip>
                      <Chip>{info.budget}</Chip>
                    </div>
                  </div>
                  <div className="req-list" style={{ marginTop: 8 }}>
                    {STAGE_GOALS[st].map((g, i) => (
                      <div key={i} className={`req ${current ? '' : locked ? '' : 'ok'}`}>
                        {current ? '▸' : locked ? '🔒' : '✓'} {g}
                      </div>
                    ))}
                  </div>
                  {st === 2 && state.stage === 1 && (
                    <div className="tiny" style={{ marginTop: 6, color: 'var(--amber)' }}>
                      Для перехода: {state.stats.contractsDone}/3 контракта, репутация {Math.round(state.rep)}/12, деньги{' '}
                      {formatMoney(state.money)}/6 000 ₽
                    </div>
                  )}
                  {st === 3 && !state.flags.dc_unlocked && state.stage >= 2 && (
                    <div className="tiny" style={{ marginTop: 6, color: 'var(--amber)' }}>
                      Для открытия: {dcProgress.contracts} контрактов, {dcProgress.rep} репутации, {dcProgress.money}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </Panel>

      <Panel title="Локации" icon="📍" sub="Переезд меняет шум, температуру и отношение людей">
        <div className="grid3">
          {(Object.keys(LOCATION_INFO) as (keyof typeof LOCATION_INFO)[]).map((loc) => {
            const info = LOCATION_INFO[loc];
            const active = state.location === loc;
            const disabled = loc === 'garage' ? state.stage < 2 : loc === 'dc' ? !state.flags.dc_unlocked : false;
            return (
              <div className={`lot ${active ? 'ready' : ''}`} key={loc} style={active ? { borderColor: 'var(--green)' } : undefined}>
                <div className="part-name">
                  {info.icon} {info.name}
                </div>
                <div className="tiny dim">{info.note}</div>
                <div className="tiny dim mono">
                  {loc === 'room' && 'шум −, место мало, семья рядом'}
                  {loc === 'garage' && 'прохладно, шкаф можно, соседи строгие'}
                  {loc === 'dc' && 'аренда 900 ₽/день + 120 ₽ за машину, холод, простор'}
                </div>
                <button
                  className={`btn btn-sm ${active ? '' : 'btn-primary'}`}
                  disabled={active || disabled}
                  onClick={() => dispatch({ type: 'MOVE_LOCATION', location: loc })}
                >
                  {active ? 'Ты здесь' : disabled ? 'Закрыто' : loc === 'dc' ? 'Арендовать (40 000 ₽)' : 'Переехать'}
                </button>
              </div>
            );
          })}
        </div>
      </Panel>

      <Panel
        title="Апгрейды инфраструктуры"
        icon="🛠️"
        sub={`Сейчас: мощность ${totalWatts(state)} Вт, защита ${firewallScore(state)}, шума ${state.upgrades.includes('soundproof') ? '−15 дБ' : 'без изоляции'}`}
      >
        <div className="grid2">
          {UPGRADES.filter((u) => u.stage <= Math.max(state.stage, 1) || state.upgrades.includes(u.id)).map((u) => {
            const owned = state.upgrades.includes(u.id);
            return (
              <div className={`upgrade ${owned ? 'owned' : ''}`} key={u.id}>
                <div className="upgrade-ico">{u.icon}</div>
                <div style={{ flex: 1 }}>
                  <div className="spread">
                    <div style={{ fontWeight: 750 }}>{u.name}</div>
                    {owned ? <Chip tone="green">куплено</Chip> : <Chip tone="amber">{formatMoney(u.price)}</Chip>}
                  </div>
                  <div className="tiny dim" style={{ margin: '3px 0 7px' }}>
                    {u.desc}
                  </div>
                  {!owned && (
                    <button
                      className="btn btn-sm btn-primary"
                      disabled={state.money < u.price}
                      onClick={() => dispatch({ type: 'BUY_UPGRADE', id: u.id })}
                    >
                      Купить
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </Panel>

      <Panel title="Статистика прохождения" icon="📈">
        <div className="grid3">
          <div className="kv">
            <span className="muted">Дней сыграно</span>
            <b>{state.stats.daysPlayed}</b>
          </div>
          <div className="kv">
            <span className="muted">Заработано</span>
            <b style={{ color: 'var(--green)' }}>{formatMoney(state.stats.moneyEarned)}</b>
          </div>
          <div className="kv">
            <span className="muted">Потрачено</span>
            <b style={{ color: 'var(--red)' }}>{formatMoney(state.stats.moneySpent)}</b>
          </div>
          <div className="kv">
            <span className="muted">Контрактов выполнено</span>
            <b>{state.stats.contractsDone}</b>
          </div>
          <div className="kv">
            <span className="muted">Аварий</span>
            <b>{state.stats.crashes}</b>
          </div>
          <div className="kv">
            <span className="muted">Кабелей обжато</span>
            <b>{state.stats.cablesCrimped}</b>
          </div>
          <div className="kv">
            <span className="muted">DDoS-пакетов отбито</span>
            <b>{state.stats.ddosBlocked}</b>
          </div>
          <div className="kv">
            <span className="muted">Команд в терминале</span>
            <b>{state.stats.commandsRun}</b>
          </div>
          <div className="kv">
            <span className="muted">Куплено деталей</span>
            <b>{state.stats.partsBought}</b>
          </div>
        </div>
      </Panel>
    </>
  );
}
