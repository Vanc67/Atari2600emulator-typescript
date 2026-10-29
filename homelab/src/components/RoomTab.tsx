import { Power, ShieldCheck, Thermometer, Volume2, Zap, Cable, Wrench, ArrowRight, ServerOff } from 'lucide-react';
import type { Dispatch } from 'react';
import { LOCATION_INFO } from '../game/data';
import { derive, formatMoney, machineStorageCap, machineWatts, totalNoise, totalWatts } from '../game/rules';
import type { Action, GameState } from '../game/types';
import { Chip, Meter, Panel, Tile } from './ui';
import { nextSteps } from '../game/guidance';
import { KIND_NAME, machineIcon, machineStatus, condTone } from './format';

const ART: Record<string, string> = {
  room: '/art/room.jpg',
  garage: '/art/garage.jpg',
  dc: '/art/dc.jpg',
};

export function RoomTab({
  state,
  dispatch,
  onCable,
}: {
  state: GameState;
  dispatch: Dispatch<Action>;
  onCable: () => void;
}) {
  const noise = Math.round(totalNoise(state));
  const watts = totalWatts(state);
  const hottest = state.machines.reduce((a, m) => Math.max(a, m.temp), 0);
  const machines = state.machines;
  const powered = machines.filter((m) => m.powered).length;
  const cableDone = state.flags.cable_day === state.day;

  return (
    <>
      <div className="hero" style={{ backgroundImage: `linear-gradient(90deg, rgba(5,9,14,.55), rgba(5,9,14,.1)), url(${ART[state.location]})` }}>
        <div>
          <div className="row" style={{ marginBottom: 4 }}>
            <Chip tone="cyan">
              {LOCATION_INFO[state.location].icon} {LOCATION_INFO[state.location].name}
            </Chip>
            <Chip tone={state.stage === 3 ? 'violet' : 'amber'}>Этап {state.stage}</Chip>
            <Chip tone={powered > 0 ? 'green' : 'red'}>{powered} из {machines.length} машин включено</Chip>
          </div>
          <div className="hero-title">
            {state.location === 'room' && 'Комната. Пахнет пылью и приключениями.'}
            {state.location === 'garage' && 'Гараж. Гул турбин и запах солярки.'}
            {state.location === 'dc' && 'ЦОД. Холодный коридор и сотни зелёных огоньков.'}
          </div>
          <div className="hero-sub">
            {LOCATION_INFO[state.location].note}.{' '}
            {state.location === 'room'
              ? 'Домашние терпят, пока ты не начинаешь гудеть на всю квартиру.'
              : state.location === 'garage'
                ? 'Прохладно и никто не жалуется на свет — пока не начнёшь шуметь.'
                : 'Здесь всё серьёзно: аренда, клиенты, DDoS и обязательства.'}
          </div>
        </div>
      </div>

      <Panel title="Что делать дальше" icon="🧭" sub="Короткий план на текущий момент">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {nextSteps(state).map((h, i) => (
            <div key={i} className="part-row" style={{ alignItems: 'flex-start' }}>
              <span style={{ fontSize: 16 }}>{h.icon}</span>
              <div className="small" style={{ color: h.tone === 'amber' ? 'var(--amber)' : h.tone === 'green' ? 'var(--green)' : '#b7c8db' }}>
                {h.text}
              </div>
            </div>
          ))}
        </div>
      </Panel>

      <div className="grid3" style={{ marginTop: 12 }}>
        <Tile icon="🔊" label="Шум" value={`${noise} дБ`} hint={noise > 55 ? 'домашние в ярости' : noise > 35 ? 'мама недовольна' : 'в пределах терпимого'} tone={noise > 55 ? 'var(--red)' : noise > 35 ? 'var(--amber)' : 'var(--green)'} />
        <Tile icon="🌡️" label="Макс. температура" value={`${hottest.toFixed(0)} °C`} hint={hottest > 85 ? 'риск аварии!' : hottest > 70 ? 'жарко' : 'норма'} tone={hottest > 85 ? 'var(--red)' : hottest > 70 ? 'var(--amber)' : 'var(--green)'} />
        <Tile icon="⚡" label="Потребление" value={`${watts} Вт`} hint={`≈ ${formatMoney(Math.round((watts * 24) / 1000 * 5.6))} / день`} tone="var(--amber)" />
      </div>

      <Panel
        title="Машины"
        icon="🗄️"
        sub="Кликни по машине, чтобы открыть панель сборки и терминал"
        right={
          <div className="row">
            <Chip tone={state.cables > 0 ? 'green' : 'amber'}>🔌 патч-кордов: {state.cables}</Chip>
            <button className="btn btn-sm" onClick={onCable} disabled={cableDone} title={cableDone ? 'Сегодня уже обжимал' : 'Сделать патч-корд вручную'}>
              <Cable size={14} /> Обжать кабель
            </button>
          </div>
        }
      >
        {machines.length === 0 ? (
          <div className="empty">
            <ServerOff size={22} style={{ marginBottom: 6 }} />
            <div>Пока никакого железа. Купи платформу на барахолке и создай первую машину.</div>
            <button className="btn btn-sm btn-amber" style={{ marginTop: 10 }} onClick={() => dispatch({ type: 'SET_TAB', tab: 'market' })}>
              На Авито <ArrowRight size={13} />
            </button>
          </div>
        ) : (
          <div className="grid-auto">
            {machines.map((m) => {
              const d = derive(m, state);
              const st = machineStatus(m);
              return (
                <div
                  key={m.id}
                  className={`machine-card ${state.selectedMachine === m.id ? 'selected' : ''} ${m.crashed ? 'crashed' : ''}`}
                  onClick={() => {
                    dispatch({ type: 'SELECT_MACHINE', id: m.id });
                    dispatch({ type: 'SET_TAB', tab: 'server' });
                  }}
                >
                  <div className="spread" style={{ marginBottom: 6 }}>
                    <div className="machine-name">
                      <span className="machine-ico">{machineIcon(m)}</span>
                      <span>{m.name}</span>
                    </div>
                    <span className={`led ${m.crashed ? 'err' : m.powered ? 'on' : ''}`} title={st.text} />
                  </div>
                  <div className="row" style={{ marginBottom: 8 }}>
                    <Chip>{KIND_NAME[m.kind]}</Chip>
                    <Chip tone={st.tone === 'cyan' ? 'cyan' : st.tone}>{st.text}</Chip>
                    {m.temp > 0 && <Chip tone={m.temp > 80 ? 'red' : 'amber'}>{m.temp.toFixed(0)} °C</Chip>}
                  </div>
                  <div className="tiny dim mono" style={{ marginBottom: 6 }}>
                    {m.base.name} · {m.cpus.find(Boolean)?.name ?? 'без CPU'} · {m.os === 'linux' ? 'Ubuntu' : m.os === 'windows' ? 'Windows Server' : m.os === 'android' ? 'Android' : 'нет ОС'}
                  </div>
                  <Meter
                    label="ОЗУ"
                    right={`${d.usedRam} / ${d.usableRam} МБ`}
                    value={d.usedRam}
                    max={Math.max(1, d.usableRam)}
                    tone={d.usedRam / Math.max(1, d.usableRam) > 0.9 ? 'red' : 'cyan'}
                  />
                  <div style={{ height: 6 }} />
                  <Meter
                    label="Диски"
                    right={`${d.usedStorageGB} / ${machineStorageCap(m)} ГБ`}
                    value={d.usedStorageGB}
                    max={Math.max(1, machineStorageCap(m))}
                    tone={d.usedStorageGB / Math.max(1, machineStorageCap(m)) > 0.9 ? 'red' : 'violet'}
                  />
                  <div className="spread" style={{ marginTop: 8 }}>
                    <span className="tiny dim mono">
                      {machineWatts(m)} Вт · {m.services.length} сервисов
                    </span>
                    <button
                      className={`btn btn-sm ${m.powered ? '' : 'btn-primary'}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        dispatch({ type: 'POWER', machineId: m.id, on: !m.powered });
                      }}
                    >
                      <Power size={13} /> {m.powered ? 'Выкл' : 'Вкл'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Panel>

      <div className="grid2">
        <Panel title="Дом и клиенты" icon="🏠" sub="Следи за балансом: семья и репутация — тоже ресурсы">
          <Meter label="Семья" value={state.family} max={100} tone={state.family < 30 ? 'red' : state.family < 60 ? 'amber' : 'violet'} right={`${Math.round(state.family)} / 100`} />
          <div style={{ height: 10 }} />
          <Meter label="Репутация" value={state.rep} max={100} tone={state.rep > 60 ? 'green' : 'cyan'} right={`${Math.round(state.rep)} / 100`} />
          <div style={{ height: 10 }} />
          <div className="row">
            <Chip tone="green">Выполнено контрактов: {state.stats.contractsDone}</Chip>
            <Chip tone={state.contracts.length ? 'cyan' : undefined}>Активных: {state.contracts.length}</Chip>
          </div>
          <div className="hr" />
          <div className="tiny dim">
            Репутация растёт за выполненные заказы и падает за сорванные SLA. Семья важна: если она на нуле — штрафы каждый день.
          </div>
        </Panel>

        <Panel title="Быстрые действия" icon="🛠️">
          <div className="grid2">
            <button className="btn btn-wide" onClick={() => dispatch({ type: 'SET_TAB', tab: 'market' })}>
              🛒 Барахолка
            </button>
            <button className="btn btn-wide" onClick={() => dispatch({ type: 'SET_TAB', tab: 'server' })}>
              🖥️ Сборка и ОС
            </button>
            <button className="btn btn-wide" onClick={() => dispatch({ type: 'SET_TAB', tab: 'jobs' })}>
              📋 Заказы
            </button>
            <button className="btn btn-wide" onClick={() => dispatch({ type: 'SET_TAB', tab: 'path' })}>
              🧭 Путь и апгрейды
            </button>
          </div>
          <div className="hr" />
          <div className="tiny dim" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <div>
              <Thermometer size={12} /> Пыль на радиаторах повышает температуру. Чисти корпуса во вкладке «Сборка».
            </div>
            <div>
              <Volume2 size={12} /> Шум зависит от корпуса, кулеров и шумоизоляции.
            </div>
            <div>
              <Zap size={12} /> Электричество списывается каждый день по счётчику.
            </div>
            <div>
              <ShieldCheck size={12} /> Защита растёт от фаервола, коммутатора и навыка сети.
            </div>
            <div>
              <Wrench size={12} /> Сломанный сервер чинится на вкладке «Сборка» — сначала выключи его.
            </div>
          </div>
        </Panel>
      </div>

      <div className="footer-note">
        Уровень железа: {state.skills.hardware} · сети: {state.skills.network} · администрирования: {state.skills.sysadmin}
      </div>
    </>
  );
}
