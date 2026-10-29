import { useState } from 'react';
import type { Dispatch } from 'react';
import { SERVICES } from '../game/data';
import { machineLoad, machineStorageCap, machineUtil, usableRam } from '../game/rules';
import type { Action, GameState, Machine } from '../game/types';
import { Chip } from './ui';

const ICONS: { id: string; label: string; glyph: string }[] = [
  { id: 'desktop-experience', label: 'Desktop Experience', glyph: '🖥️' },
  { id: 'taskmgr', label: 'Диспетчер задач', glyph: '📊' },
  { id: 'diskmgr', label: 'Управление дисками', glyph: '💽' },
  { id: 'share', label: 'Общая папка', glyph: '📁' },
  { id: 'jellyfin', label: 'Jellyfin', glyph: '🎬' },
  { id: 'iis', label: 'IIS / веб', glyph: '🌐' },
  { id: 'cctv', label: 'Камеры', glyph: '📹' },
  { id: 'audio', label: 'Служба звука', glyph: '🔊' },
];

export function WindowsDesktop({ machine, state, dispatch }: { machine: Machine; state: GameState; dispatch: Dispatch<Action> }) {
  const [openWin, setOpenWin] = useState<string | null>(null);
  const out = state.cmdOutput && state.cmdOutput.machineId === machine.id ? state.cmdOutput : null;
  const load = machineLoad(machine);
  const ramTotal = machine.ram.reduce((a, p) => a + (p?.stats.ramMB ?? 0), 0);

  return (
    <div className="desk">
      <div className="desk-icons">
        {ICONS.map((ic) => (
          <div
            className="desk-icon"
            key={ic.id}
            onClick={() => {
              if (ic.id === 'taskmgr' || ic.id === 'diskmgr') {
                dispatch({ type: 'WIN_ACTION', machineId: machine.id, action: ic.id });
                setOpenWin(ic.id);
              } else {
                dispatch({ type: 'WIN_ACTION', machineId: machine.id, action: ic.id });
              }
            }}
          >
            <span className="gl">{ic.glyph}</span>
            <span>{ic.label}</span>
          </div>
        ))}
      </div>

      {openWin && out && (
        <div className="win-window">
          <div className="win-title">
            <span>{openWin === 'taskmgr' ? 'Диспетчер задач — Windows Server 2016' : 'Управление дисками'}</span>
            <span className="win-x" onClick={() => setOpenWin(null)}>
              ✕
            </span>
          </div>
          <div className="win-body">
            {out.lines.map((l, i) => (
              <div key={i} className="mrow">
                <span>{l.text}</span>
              </div>
            ))}
            <div className="mrow" style={{ marginTop: 6 }}>
              <span>Загрузка ЦП</span>
              <b>{Math.round(machineUtil(machine) * 100)}%</b>
            </div>
            <div className="mrow">
              <span>ОЗУ</span>
              <b>
                {load.ram + 512} / {ramTotal} МБ
              </b>
            </div>
            <div className="mrow">
              <span>Свободное место</span>
              <b>{Math.max(0, machineStorageCap(machine) - load.storage - 12)} ГБ</b>
            </div>
          </div>
        </div>
      )}

      <div className="win-taskbar">
        <span className="win-start">⊞ Пуск</span>
        <span className="win-app">Сервер</span>
        <span className="win-app">PowerShell</span>
        <span className="win-app">Диспетчер задач</span>
        <span style={{ marginLeft: 'auto' }}>
          {machine.os === 'windows' ? 'Windows Server 2016' : 'Android'} · {machine.powered ? 'онлайн' : 'офлайн'}
        </span>
      </div>

      <div className="row" style={{ padding: 10, background: 'rgba(4,10,16,.5)', borderTop: '1px solid #1d3040' }}>
        <Chip tone={machine.desktopExperience ? 'green' : 'amber'}>
          {machine.desktopExperience ? 'Desktop Experience установлен' : 'Только консоль — нужен Desktop Experience (400 ₽)'}
        </Chip>
        <Chip tone={machine.audioService ? 'green' : undefined}>🔊 {machine.audioService ? 'включена' : 'выключена'}</Chip>
        <Chip tone={machine.windowsLicensed ? 'green' : undefined}>{machine.windowsLicensed ? 'лицензия активна' : 'без лицензии'}</Chip>
        <Chip tone={machine.shares.length ? 'cyan' : undefined}>📁 {machine.shares.join(', ') || 'папки не расшарены'}</Chip>
        <Chip>
          🧠 {load.ram} / {usableRam(machine)} МБ
        </Chip>
        <Chip tone="violet">сервисов: {machine.services.length}</Chip>
      </div>
      {machine.services.length > 0 && (
        <div className="row" style={{ padding: '0 10px 10px', background: 'rgba(4,10,16,.5)' }}>
          {machine.services.map((id) => (
            <span key={id} className="service-tag">
              {SERVICES[id]?.icon} {SERVICES[id]?.name}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
