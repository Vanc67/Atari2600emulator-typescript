import { useState } from 'react';
import type { Dispatch } from 'react';
import { INSTALLABLE, PART_KIND_INFO, SERVICES, WINDOWS_LICENSE, FLAW_INFO } from '../game/data';
import {
  derive,
  osStorageReserve,
  machineStorageCap,
  machineWatts,
  usableRam,
  formatMoney,
  osName,
} from '../game/rules';
import type { Action, GameState, Machine, Part } from '../game/types';
import { Chip, Meter, Panel, Tile } from './ui';
import { KIND_NAME, machineIcon, machineStatus, condTone, partSpecs } from './format';
import { Terminal } from './Terminal';
import { WindowsDesktop } from './WindowsDesktop';

function SlotCard({
  label,
  part,
  onRemove,
  kind,
}: {
  label: string;
  part: Part | null;
  onRemove?: () => void;
  kind: string;
}) {
  return (
    <div className={`slot ${part ? 'filled' : ''}`}>
      <div className="slot-head">{label}</div>
      {part ? (
        <>
          <div className="slot-part">{part.name}</div>
          <div className="part-meta">
            {part.condition}% · {part.flawKnown ? (part.flaw === 'none' ? 'здоров' : FLAW_INFO[part.flaw].name) : 'не проверено'}
          </div>
          {onRemove && (
            <button className="slot-x" title="Снять деталь" onClick={onRemove}>
              ✕
            </button>
          )}
        </>
      ) : (
        <div className="tiny dim">пусто — {kind}</div>
      )}
    </div>
  );
}

export function ServerTab({
  state,
  dispatch,
  onCable,
}: {
  state: GameState;
  dispatch: Dispatch<Action>;
  onCable: () => void;
}) {
  const machine = state.machines.find((m) => m.id === state.selectedMachine) ?? state.machines[0];
  const [rename, setRename] = useState('');

  const baseParts = state.inventory.filter((p) => p.kind === 'base');

  if (!machine) {
    return (
      <>
        <Panel title="Сборка первого сервера" icon="🧰" sub="Купи платформу на барахолке — или используй то, что уже есть в инвентаре">
          {baseParts.length === 0 ? (
            <div className="empty">
              В инвентаре нет ни одной платформы. Старый ПК уже собран, но если хочешь второй сервер — нужен корпус или стоечный сервер.
              <div style={{ marginTop: 10 }}>
                <button className="btn btn-sm btn-amber" onClick={() => dispatch({ type: 'SET_TAB', tab: 'market' })}>
                  🛒 Открыть барахолку
                </button>
              </div>
            </div>
          ) : (
            <div className="grid-auto">
              {baseParts.map((p) => (
                <div className="lot" key={p.uid}>
                  <div className="part-name">
                    {PART_KIND_INFO.base.icon} {p.name}
                  </div>
                  <div className="tiny dim">{p.flavor}</div>
                  <div className="row">
                    {partSpecs(p).map((sp, i) => (
                      <Chip key={i}>{sp}</Chip>
                    ))}
                  </div>
                  <button className="btn btn-primary btn-wide" onClick={() => dispatch({ type: 'CREATE_MACHINE', baseUid: p.uid })}>
                    Собрать машину
                  </button>
                </div>
              ))}
            </div>
          )}
        </Panel>
      </>
    );
  }

  const d = derive(machine, state);
  const st = machineStatus(machine);
  const load = machine.services.reduce(
    (acc, id) => {
      const svc = SERVICES[id];
      acc.ram += svc?.ramMB ?? 0;
      acc.cores += svc?.cores ?? 0;
      acc.storage += svc?.storageGB ?? 0;
      return acc;
    },
    { ram: 0, cores: 0, storage: 0 },
  );
  const compatible = state.inventory.filter((p) => p.kind !== 'base');

  return (
    <>
      <div className="panel" style={{ padding: 10 }}>
        <div className="row">
          {state.machines.map((m) => (
            <button
              key={m.id}
              className={`btn btn-sm ${m.id === machine.id ? 'btn-primary' : ''}`}
              onClick={() => dispatch({ type: 'SELECT_MACHINE', id: m.id })}
            >
              <span className={`led ${m.crashed ? 'err' : m.powered ? 'on' : ''}`} />
              {machineIcon(m)} {m.name}
            </button>
          ))}
          {baseParts.length > 0 && (
            <div className="row" style={{ marginLeft: 'auto' }}>
              {baseParts.map((p) => (
                <button key={p.uid} className="btn btn-sm btn-amber" onClick={() => dispatch({ type: 'CREATE_MACHINE', baseUid: p.uid })}>
                  + {p.name}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <Panel
        title={`${machineIcon(machine)} ${machine.name}`}
        sub={`${KIND_NAME[machine.kind]} · ${machine.base.name} · ${osName(machine.os)}`}
        right={
          <div className="row">
            <Chip tone={st.tone === 'cyan' ? 'cyan' : st.tone}>{st.text}</Chip>
            <button className="btn btn-sm" onClick={() => setRename(rename || machine.name)} title="Переименовать">
              ✏️
            </button>
            <button
              className={`btn btn-sm ${machine.powered ? '' : 'btn-primary'}`}
              onClick={() => dispatch({ type: 'POWER', machineId: machine.id, on: !machine.powered })}
            >
              ⏻ {machine.powered ? 'Выключить' : 'Включить'}
            </button>
          </div>
        }
      >
        {rename && (
          <div className="row" style={{ marginBottom: 10 }}>
            <input
              className="btn"
              style={{ flex: 1, cursor: 'text' }}
              value={rename}
              onChange={(e) => setRename(e.target.value)}
              maxLength={28}
            />
            <button
              className="btn btn-sm btn-primary"
              onClick={() => {
                dispatch({ type: 'RENAME_MACHINE', machineId: machine.id, name: rename });
                setRename('');
              }}
            >
              Сохранить
            </button>
            <button className="btn btn-sm" onClick={() => setRename('')}>
              Отмена
            </button>
          </div>
        )}

        <div className="grid3">
          <Tile icon="🌡️" label="Температура" value={`${machine.temp.toFixed(1)} °C`} hint={machine.temp > 85 ? 'критично' : machine.temp > 70 ? 'жарко' : 'норма'} tone={machine.temp > 85 ? 'var(--red)' : machine.temp > 70 ? 'var(--amber)' : 'var(--green)'} />
          <Tile icon="⚡" label="Потребление" value={`${machineWatts(machine)} Вт`} hint={`не шумит: ${Math.round(d.noise)} дБ`} />
          <Tile icon="⏱️" label="Аптайм" value={`${machine.uptime} дн.`} hint={machine.crashed ? 'авария' : machine.powered ? 'в работе' : 'остановлен'} tone={machine.powered ? 'var(--green)' : 'var(--dim)'} />
        </div>

        <div className="grid2" style={{ marginTop: 10 }}>
          <div>
            <Meter label="ОЗУ" value={load.ram + (machine.os === 'linux' ? 512 : machine.os === 'windows' ? 2048 : 0)} max={Math.max(1, d.ramMB)} right={`${load.ram} сервисов / ${d.usableRam} МБ доступно`} tone={load.ram > d.usableRam ? 'red' : 'cyan'} />
            <div style={{ height: 8 }} />
            <Meter label="Ядра CPU" value={load.cores} max={Math.max(1, d.cores)} right={`${load.cores} / ${d.cores} ядер`} tone={load.cores > d.cores ? 'red' : 'green'} />
            <div style={{ height: 8 }} />
            <Meter label="Диски" value={load.storage + osStorageReserve(machine)} max={Math.max(1, machineStorageCap(machine))} right={`${load.storage + osStorageReserve(machine)} / ${machineStorageCap(machine)} ГБ`} tone="violet" />
          </div>
          <div>
            <div className="tiny dim mono" style={{ marginBottom: 4 }}>ПРОБЛЕМЫ</div>
            {d.issues.length === 0 ? (
              <div className="tiny" style={{ color: 'var(--green)' }}>✓ Всё в порядке, можно брать заказы.</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                {d.issues.slice(0, 6).map((iss, i) => (
                  <div key={i} className="tiny" style={{ color: 'var(--amber)' }}>
                    • {iss}
                  </div>
                ))}
              </div>
            )}
            <div className="hr" />
            <div className="row">
              <Chip tone={machine.os === 'linux' ? 'green' : machine.os === 'windows' ? 'cyan' : 'amber'}>{osName(machine.os)}</Chip>
              <Chip>🔌 {state.cables} кабелей</Chip>
              <Chip tone="cyan">🛡️ {d.firewall}</Chip>
            </div>
          </div>
        </div>
      </Panel>

      <div className="panel">
        <div className="panel-head">
          <div>
            <div className="panel-title">🧰 Железо и сборка</div>
            <div className="panel-sub">Менять комплектующие можно только при выключенном сервере</div>
          </div>
          <div className="row">
            <Chip tone={machine.powered ? 'amber' : 'green'}>{machine.powered ? 'выключи для разборки' : 'можно разбирать'}</Chip>
          </div>
        </div>

        <div className="slots">
          <SlotCard label="Платформа" part={machine.base} kind="корпус" />
          {machine.cpus.map((c, i) => (
            <SlotCard
              key={`cpu${i}`}
              label={machine.cpus.length > 1 ? `CPU ${i + 1}` : 'Процессор'}
              part={c}
              kind="сокет"
              onRemove={c && !machine.powered ? () => dispatch({ type: 'REMOVE_PART', machineId: machine.id, slot: 'cpu', index: i }) : undefined}
            />
          ))}
          {machine.ram.map((r, i) => (
            <SlotCard
              key={`ram${i}`}
              label={`ОЗУ ${i + 1}`}
              part={r}
              kind="слот RAM"
              onRemove={r && !machine.powered ? () => dispatch({ type: 'REMOVE_PART', machineId: machine.id, slot: 'ram', index: i }) : undefined}
            />
          ))}
          {machine.storage.map((sd, i) => (
            <SlotCard
              key={`disk${i}`}
              label={`Диск ${i + 1}`}
              part={sd}
              kind="отсек"
              onRemove={sd && !machine.powered ? () => dispatch({ type: 'REMOVE_PART', machineId: machine.id, slot: 'storage', index: i }) : undefined}
            />
          ))}
          <SlotCard label="Охлаждение" part={machine.cooler} kind="кулер" onRemove={machine.cooler && !machine.powered ? () => dispatch({ type: 'REMOVE_PART', machineId: machine.id, slot: 'cooler', index: 0 }) : undefined} />
          <SlotCard label="Сеть" part={machine.nic} kind="сетевая карта" onRemove={machine.nic && !machine.powered ? () => dispatch({ type: 'REMOVE_PART', machineId: machine.id, slot: 'nic', index: 0 }) : undefined} />
        </div>

        <div className="hr" />
        <div className="grid2">
          <div>
            <div className="tiny dim mono" style={{ marginBottom: 6 }}>ИНВЕНТАРЬ — установить в {machine.name}</div>
            {compatible.length === 0 ? (
              <div className="tiny dim">Нет свободных деталей. Купи что-нибудь на барахолке.</div>
            ) : (
              compatible.map((p) => (
                <div className="part-row" key={p.uid}>
                  <span>{PART_KIND_INFO[p.kind].icon}</span>
                  <div style={{ flex: 1 }}>
                    <div className="part-name">{p.name}</div>
                    <div className="part-meta">{partSpecs(p).join(' · ')}</div>
                  </div>
                  <button
                    className="btn btn-sm"
                    disabled={machine.powered}
                    onClick={() => dispatch({ type: 'INSTALL_PART', uid: p.uid, machineId: machine.id })}
                  >
                    Установить
                  </button>
                </div>
              ))
            )}
          </div>
          <div>
            <div className="tiny dim mono" style={{ marginBottom: 6 }}>ОБСЛУЖИВАНИЕ</div>
            <div className="grid2">
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'REPAIR', machineId: machine.id, action: 'clean' })}>
                🧹 Почистить пыль
              </button>
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'REPAIR', machineId: machine.id, action: 'paste' })}>
                🧴 Термопаста (350 ₽)
              </button>
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'REPAIR', machineId: machine.id, action: 'smart' })}>
                🔬 Диагностика
              </button>
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'REPAIR', machineId: machine.id, action: 'reseat' })}>
                🔌 Переставить планки (150 ₽)
              </button>
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'REPAIR', machineId: machine.id, action: 'fix-flaw' })}>
                🛠️ Починить дефект
              </button>
              {machine.kind === 'phone' && (
                <button className="btn btn-sm" onClick={() => dispatch({ type: 'REPAIR', machineId: machine.id, action: 'battery' })}>
                  🔋 Аккумулятор (600 ₽)
                </button>
              )}
              {machine.base.stats.hasILO && (
                <button
                  className="btn btn-sm"
                  onClick={() => dispatch({ type: 'ILO', machineId: machine.id })}
                  disabled={machine.ilo}
                  title="Удалённое управление: сервер сам перезагружается после зависания"
                >
                  {machine.ilo ? '🌐 iLO настроен' : '🌐 Настроить iLO (2 000 ₽)'}
                </button>
              )}
              <button className="btn btn-sm" onClick={onCable}>
                🔧 Обжать патч-корд
              </button>
              <button
                className="btn btn-sm"
                onClick={() => dispatch({ type: 'CREATE_MACHINE', baseUid: baseParts[0]?.uid ?? '' })}
                disabled={!baseParts.length}
              >
                ➕ Новая машина
              </button>
            </div>
            <div className="row" style={{ marginTop: 8 }}>
              <span className="tiny dim mono">RAID:</span>
              {machine.raid ? (
                <Chip tone={machine.raid.healthy ? 'green' : 'red'}>
                  RAID-{machine.raid.level} · {machine.raid.disks} диска · {machine.raid.healthy ? 'здоров' : 'деград'}
                </Chip>
              ) : (
                <Chip tone="amber">не собран</Chip>
              )}
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'RAID', machineId: machine.id, level: 1 })}>
                Собрать RAID-1 (2 диска)
              </button>
              <button className="btn btn-sm" onClick={() => dispatch({ type: 'RAID', machineId: machine.id, level: 5 })}>
                Собрать RAID-5 (3 диска)
              </button>
            </div>
            <div className="tiny dim" style={{ marginTop: 8 }}>
              Пыль добавляет жару, перегрев валит сервер. Раз в пару дней чисти корпус — это бесплатно. RAID спасает данные, но
              съедает ёмкость (RAID-5 — один диск на чётность) и при низком навыке может развалиться при сборке.
            </div>
          </div>
        </div>
      </div>

      <Panel
        title="Система и сервисы"
        icon="💻"
        sub={
          machine.os === 'linux'
            ? 'Ubuntu Server: всё через терминал. Ставь пакеты apt, запускай systemctl.'
            : machine.os === 'windows'
              ? 'Windows Server 2016: работай мышкой по иконкам на рабочем столе.'
              : machine.os === 'android'
                ? 'Android 2.3: единственный сервис — tinyllm.'
                : 'Сначала установи операционную систему'
        }
      >
        {machine.os === 'none' && (
          <div className="grid2">
            <div className="lot">
              <div className="part-name">🐧 Ubuntu Server 22.04</div>
              <div className="tiny dim">Только консоль. Хардкор, зато бесплатно и стабильно. Идеально для серверов.</div>
              <button className="btn btn-primary btn-wide" onClick={() => dispatch({ type: 'INSTALL_OS', machineId: machine.id, os: 'linux' })} disabled={!machine.powered}>
                Установить Linux
              </button>
            </div>
            <div className="lot">
              <div className="part-name">🪟 Windows Server 2016</div>
              <div className="tiny dim">
                Нужен Desktop Experience, чтобы был рабочий стол и звук. Лицензия {formatMoney(WINDOWS_LICENSE)}.
              </div>
              <button className="btn btn-wide" onClick={() => dispatch({ type: 'INSTALL_OS', machineId: machine.id, os: 'windows' })} disabled={!machine.powered}>
                Установить Windows ({formatMoney(WINDOWS_LICENSE)})
              </button>
            </div>
            <div className="tiny dim">Сервер должен быть включён. Если не хватает памяти под ОС — добавь планку.</div>
          </div>
        )}

        {machine.os === 'linux' && <Terminal key={machine.id} machine={machine} state={state} dispatch={dispatch} />}
        {machine.os === 'windows' && <WindowsDesktop key={machine.id} machine={machine} state={state} dispatch={dispatch} />}
        {machine.os === 'android' && (
          <div className="lot">
            <div className="part-name">🤖 Tiny-нейросеть (tinyllm)</div>
            <div className="tiny dim">Локальная модель на 512 МБ. Медленно, но клиенты в восторге. Требуется патч-корд — телефон должен быть в сети.</div>
            <button
              className="btn btn-primary btn-wide"
              onClick={() => dispatch({ type: 'WIN_ACTION', machineId: machine.id, action: 'tiny' })}
              disabled={machine.services.includes('tinyllm') || state.cables < 1}
            >
              {machine.services.includes('tinyllm') ? 'Уже работает' : 'Запустить tinyllm'}
            </button>
          </div>
        )}

        {machine.os === 'linux' && (
          <>
            <div className="hr" />
            <div className="tiny dim mono" style={{ marginBottom: 6 }}>БЫСТРАЯ УСТАНОВКА ПАКЕТОВ (эквивалент apt install)</div>
            <div className="grid-auto">
              {INSTALLABLE.filter((svc) => !machine.installed.includes(svc.id)).map((svc) => {
                const fits = load.ram + svc.ramMB <= usableRam(machine) - 96 && load.storage + svc.storageGB + 12 <= machineStorageCap(machine);
                return (
                  <div className="lot" key={svc.id} style={{ gap: 5 }}>
                    <div className="part-name">
                      {svc.icon} {svc.name}
                    </div>
                    <div className="tiny dim">{svc.desc}</div>
                    <div className="row">
                      <Chip tone={fits ? 'green' : 'red'}>{svc.ramMB} МБ</Chip>
                      <Chip>{svc.cores} ядра</Chip>
                      <Chip>{svc.storageGB} ГБ</Chip>
                    </div>
                    <button
                      className="btn btn-sm btn-wide"
                      disabled={!fits || !machine.powered}
                      onClick={() => dispatch({ type: 'CMD', machineId: machine.id, input: `apt install ${svc.linuxPackage}` })}
                    >
                      apt install {svc.linuxPackage}
                    </button>
                  </div>
                );
              })}
            </div>
          </>
        )}
      </Panel>

      <Panel title="Сервисы и нагрузка" icon="📊" sub="Останови лишнее, если не хватает памяти">
        {machine.services.length === 0 ? (
          <div className="empty">Сервисов нет. Установи что-нибудь — например, SMB-папку для семьи.</div>
        ) : (
          <div className="grid2">
            {machine.services.map((id) => {
              const svc = SERVICES[id];
              if (!svc) return null;
              return (
                <div className="part-row" key={id} style={{ padding: 9 }}>
                  <span style={{ fontSize: 17 }}>{svc.icon}</span>
                  <div style={{ flex: 1 }}>
                    <div className="part-name">{svc.name}</div>
                    <div className="part-meta">
                      {svc.ramMB} МБ · {svc.cores} ядра · {svc.storageGB} ГБ
                    </div>
                  </div>
                  {machine.os === 'linux' ? (
                    <button
                      className="btn btn-sm"
                      onClick={() => dispatch({ type: 'CMD', machineId: machine.id, input: `systemctl stop ${svc.linuxUnit}` })}
                    >
                      Стоп
                    </button>
                  ) : (
                    <Chip tone="green">работает</Chip>
                  )}
                </div>
              );
            })}
          </div>
        )}
        {machine.installed.length > machine.services.length && (
          <div className="tiny dim" style={{ marginTop: 8 }}>
            Остановлено: {machine.installed.filter((i) => !machine.services.includes(i)).map((i) => SERVICES[i]?.name).join(', ')} — запусти
            через `systemctl start &lt;unit&gt;`.
          </div>
        )}
        <div className="hr" />
        <div className="grid3">
          <Tile label="Нагрузка ОЗУ" value={`${Math.round((load.ram / Math.max(1, d.usableRam)) * 100)}%`} hint={`${load.ram} МБ занято`} tone={load.ram / Math.max(1, d.usableRam) > 0.85 ? 'var(--red)' : 'var(--cyan)'} />
          <Tile label="Состояние дисков" value={`${machine.storage.filter(Boolean).length}`} hint={`${machineStorageCap(machine)} ГБ всего`} />
          <Tile label="Дефектов" value={`${d.flaws.length}`} hint={d.flaws.length ? d.flaws.map((p) => FLAW_INFO[p.flaw].name).slice(0, 2).join(', ') : 'всё чисто'} tone={d.flaws.length ? 'var(--amber)' : 'var(--green)'} />
        </div>
        {d.flaws.length > 0 && (
          <div className="tiny dim" style={{ marginTop: 8 }}>
            Состояние деталей:{' '}
            {d.flaws.map((p) => `${p.name} (${FLAW_INFO[p.flaw].name})`).join('; ')}. Качество железа влияет на риск аварий и на
            требования клиентов.
          </div>
        )}
      </Panel>
    </>
  );
}
