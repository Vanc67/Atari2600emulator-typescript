import type { Dispatch } from 'react';
import { SERVICES } from '../game/data';
import { contractBlocker, contractMachine, derive, firewallScore, formatMoney } from '../game/rules';
import type { Action, Contract, GameState } from '../game/types';
import { Chip, Meter, Panel } from './ui';

function reqRows(state: GameState, c: Contract) {
  const req = c.requirements;
  const rows: { text: string; ok: boolean }[] = [];
  if (req.services) {
    req.services.forEach((s) => {
      const has = state.machines.some((m) => m.services.includes(s));
      rows.push({ text: `${SERVICES[s]?.icon ?? '•'} сервис ${SERVICES[s]?.name ?? s}`, ok: has });
    });
  }
  if (req.os) {
    const has = state.machines.some((m) => m.os === req.os);
    rows.push({ text: `ОС ${req.os === 'linux' ? 'Ubuntu Server' : 'Windows Server'}`, ok: has });
  }
  if (req.minRamMB) {
    const has = state.machines.some((m) => derive(m, state).ramMB >= req.minRamMB!);
    rows.push({ text: `≥ ${(req.minRamMB / 1024).toFixed(0)} ГБ оперативной памяти`, ok: has });
  }
  if (req.minCores) {
    const has = state.machines.some((m) => derive(m, state).cores >= req.minCores!);
    rows.push({ text: `≥ ${req.minCores} ядер`, ok: has });
  }
  if (req.minStorageGB) {
    const has = state.machines.some((m) => derive(m, state).storageGB >= req.minStorageGB!);
    rows.push({ text: `≥ ${req.minStorageGB} ГБ дисков`, ok: has });
  }
  if (req.firewall) {
    rows.push({ text: `firewall ≥ ${req.firewall} (сейчас ${firewallScore(state)})`, ok: firewallScore(state) >= req.firewall });
  }
  if (req.cables) {
    rows.push({ text: `патч-кордов: ${req.cables}`, ok: state.cables >= req.cables });
  }
  if (req.needsSwitch) rows.push({ text: 'коммутатор Cisco', ok: state.upgrades.includes('switch') });
  if (req.needsBackup) rows.push({ text: 'настроенные бэкапы', ok: state.upgrades.includes('backup') });
  if (req.minUptime) {
    const has = state.machines.some((m) => m.uptime >= req.minUptime!);
    rows.push({ text: `аптайм ≥ ${req.minUptime} дней`, ok: has });
  }
  if (req.kinds) {
    const has = state.machines.some((m) => req.kinds!.includes(m.kind) && derive(m, state).up);
    rows.push({ text: `машина типа: ${req.kinds.join(' / ')}`, ok: has });
  }
  return rows;
}

export function JobsTab({ state, dispatch }: { state: GameState; dispatch: Dispatch<Action> }) {
  const offers = state.offers;
  const active = state.contracts;

  return (
    <>
      <Panel
        title="Активные договоры"
        icon="📋"
        sub="Проверяй каждый день: клиент платит, пока его сервис жив"
        right={<Chip tone="cyan">Репутация {Math.round(state.rep)}</Chip>}
      >
        {active.length === 0 ? (
          <div className="empty">Договоров пока нет. Возьми первый заказ ниже — семья ищет файлообменник.</div>
        ) : (
          <div className="grid-auto">
            {active.map((c) => {
              const m = contractMachine(state, c);
              const d = m ? derive(m, state) : null;
              return (
                <div className={`contract ${m ? 'ready' : 'failed'}`} key={c.uid}>
                  <div className="spread">
                    <div className="part-name">
                      {c.icon} {c.title}
                    </div>
                    <Chip tone={m ? 'green' : 'red'}>{m ? 'работает' : 'НЕТ СЕРВИСА'}</Chip>
                  </div>
                  <div className="tiny dim">{c.client}</div>
                  <div className="row">
                    <Chip tone="green">{formatMoney(c.dailyPay)} / день</Chip>
                    <Chip tone={c.daysLeft <= 3 ? 'amber' : undefined}>осталось {c.daysLeft} дн.</Chip>
                    <Chip>получено {formatMoney(c.payments * c.dailyPay)}</Chip>
                  </div>
                  <Meter label="Срок" value={c.days - c.daysLeft} max={c.days} right={`${c.daysLeft} / ${c.days}`} tone={c.daysLeft <= 3 ? 'amber' : 'green'} />
                  <div className="tiny" style={{ color: m ? 'var(--dim)' : 'var(--red)' }}>
                    {m ? `Обслуживает: ${m.name} · ${d?.temp.toFixed(0)} °C` : contractBlocker(state, c)}
                  </div>
                  {c.trouble && <div className="tiny" style={{ color: 'var(--amber)' }}>⚠ {c.trouble}</div>}
                  <button className="btn btn-sm btn-danger" style={{ marginTop: 'auto' }} onClick={() => dispatch({ type: 'ABANDON_CONTRACT', uid: c.uid })}>
                    Расторгнуть (−{c.penalty} ₽)
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </Panel>

      <Panel
        title="Новые заказы"
        icon="🤝"
        sub="Аванс платится сразу. Если требования не выполнены — заказ серый, наведи на подсказку"
      >
        {offers.length === 0 ? (
          <div className="empty">Заказов нет. Подними репутацию и железо — клиенты придут. Новые предложения появляются каждый день.</div>
        ) : (
          <div className="grid-auto">
            {offers.map((c) => {
              const blocker = contractBlocker(state, c);
              const ready = !blocker;
              const rows = reqRows(state, c);
              return (
                <div className={`contract ${ready ? 'ready' : ''}`} key={c.uid}>
                  <div className="spread">
                    <div className="part-name">
                      {c.icon} {c.title}
                    </div>
                    <Chip tone={c.stage === 3 ? 'violet' : c.stage === 2 ? 'cyan' : undefined}>этап {c.stage}</Chip>
                  </div>
                  <div className="tiny dim">{c.client}</div>
                  <div className="tiny" style={{ color: '#b7c8db' }}>
                    {c.desc}
                  </div>
                  <div className="row">
                    <span className="contract-pay">{formatMoney(c.setupPay)}</span>
                    <span className="tiny dim">аванс</span>
                    <Chip tone="green">+{formatMoney(c.dailyPay)} / день</Chip>
                    <Chip>{c.days} дн.</Chip>
                  </div>
                  <div className="row">
                    <Chip tone="violet">нужно {c.load.ramMB} МБ ОЗУ</Chip>
                    <Chip>нагрузка {c.load.cores} ядра</Chip>
                    <Chip>{c.load.storageGB} ГБ</Chip>
                  </div>
                  <div className="req-list">
                    {rows.map((r, i) => (
                      <div key={i} className={`req ${r.ok ? 'ok' : 'bad'}`}>
                        {r.ok ? '✓' : '✕'} {r.text}
                      </div>
                    ))}
                  </div>
                  <div className="tiny" style={{ color: ready ? 'var(--green)' : 'var(--red)' }}>
                    {ready ? '✓ Всё готово — можно брать' : `✕ ${blocker}`}
                  </div>
                  <button className="btn btn-sm btn-primary" disabled={!ready} onClick={() => dispatch({ type: 'ACCEPT_CONTRACT', uid: c.uid })}>
                    Заключить договор
                  </button>
                </div>
              );
            })}
          </div>
        )}
        <div className="hr" />
        <div className="tiny dim">
          Неустойка за сорванный договор: клиент не получил сервис хотя бы один день — плати штраф и теряй репутацию. Следи за
          перегревом и памятью: сервис должен реально работать.
        </div>
      </Panel>
    </>
  );
}
