import { renderToStaticMarkup } from 'react-dom/server';
import { createInitialState, reducer } from '../src/game/game';
import { RoomTab } from '../src/components/RoomTab';
import { MarketTab } from '../src/components/MarketTab';
import { ServerTab } from '../src/components/ServerTab';
import { JobsTab } from '../src/components/JobsTab';
import { PathTab } from '../src/components/PathTab';
import { HUD } from '../src/components/HUD';
import { EventModal, HelpModal, ReportModal } from '../src/components/Modals';
import { makePart } from '../src/game/market';
import type { GameState } from '../src/game/types';

const noop = () => {};
const dispatch = noop as never;

let s: GameState = createInitialState();
const pc = s.machines[0].id;
s = reducer(s, { type: 'POWER', machineId: pc, on: true });
s = reducer(s, { type: 'INSTALL_OS', machineId: pc, os: 'linux' });
s = reducer(s, { type: 'CMD', machineId: pc, input: 'apt install samba' });
s = reducer(s, { type: 'CMD', machineId: pc, input: 'apt install jellyfin' });
s.inventory.push(makePart('base_dl380', { flaw: 'noisyfan' }), makePart('ram_ecc_8', { flaw: 'none' }), makePart('hdd_8tb', { flaw: 'none' }));
s = reducer(s, { type: 'CREATE_MACHINE', baseUid: s.inventory[s.inventory.length - 3].uid });
s = reducer(s, { type: 'BUY_UPGRADE', id: 'rack' });
const winState: GameState = JSON.parse(JSON.stringify(s));
winState.money = 50000;
const wm = winState.machines[0];
wm.os = 'windows';
wm.windowsLicensed = true;

const cases: [string, () => string][] = [
  ['HUD', () => renderToStaticMarkup(<HUD state={s} dispatch={dispatch} />)],
  ['RoomTab', () => renderToStaticMarkup(<RoomTab state={s} dispatch={dispatch} onCable={noop} />)],
  ['MarketTab', () => renderToStaticMarkup(<MarketTab state={s} dispatch={dispatch} onHaggle={noop} />)],
  ['ServerTab linux', () => renderToStaticMarkup(<ServerTab state={s} dispatch={dispatch} onCable={noop} />)],
  ['ServerTab windows', () => renderToStaticMarkup(<ServerTab state={winState} dispatch={dispatch} onCable={noop} />)],
  ['JobsTab', () => renderToStaticMarkup(<JobsTab state={s} dispatch={dispatch} />)],
  ['PathTab', () => renderToStaticMarkup(<PathTab state={s} dispatch={dispatch} />)],
  ['HelpModal', () => renderToStaticMarkup(<HelpModal onClose={noop} />)],
  ['EventModal', () => {
    const st = { ...s, event: { id: 'x', title: 'Тест', text: 'Текст', icon: '⚡', tone: 'bad' as const, choices: [{ label: 'A', hint: 'hint', effect: { text: 'ok' } }] } };
    return renderToStaticMarkup(<EventModal event={st.event!} state={st} dispatch={dispatch} />);
  }],
  ['ReportModal', () => {
    const st = { ...s, report: { day: 2, income: 500, expenses: 200, lines: [{ label: 'Оплата', amount: 500 }], events: [], failedContracts: [], served: 1, total: 1, electric: 10 } };
    return renderToStaticMarkup(<ReportModal report={st.report!} state={st} dispatch={dispatch} />);
  }],
  ['empty state', () => {
    const empty: GameState = { ...s, machines: [], inventory: [], selectedMachine: '', offers: [], contracts: [], listings: [] };
    return renderToStaticMarkup(<ServerTab state={empty} dispatch={dispatch} onCable={noop} />) + renderToStaticMarkup(<RoomTab state={empty} dispatch={dispatch} onCable={noop} />);
  }],
];

let failed = 0;
for (const [name, fn] of cases) {
  try {
    const html = fn();
    if (!html || html.length < 50) throw new Error('пустой вывод');
    if (html.includes('undefined') || html.includes('NaN')) {
      console.warn('⚠', name, 'содержит undefined/NaN');
    }
    console.log('✓', name, `(${(html.length / 1024).toFixed(1)} kB)`);
  } catch (e) {
    failed += 1;
    console.error('✕', name, (e as Error).message);
  }
}
console.log(failed === 0 ? '✅ РЕНДЕР ВСЕХ ЭКРАНОВ ОК' : `❌ ОШИБОК: ${failed}`);
process.exit(failed === 0 ? 0 : 1);
