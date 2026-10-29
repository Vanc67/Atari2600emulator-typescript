import { createInitialState, reducer, SAVE_VERSION_CURRENT } from '../src/game/game';
import { execCommand } from '../src/game/commands';
import { contractAvailable, derive, firewallScore, usableRam } from '../src/game/rules';
import { makePart } from '../src/game/market';
import type { Action, GameState } from '../src/game/types';

let failures = 0;
function assert(cond: unknown, msg: string) {
  if (!cond) {
    failures += 1;
    console.error('FAIL:', msg);
  }
}

const send = (s: GameState, a: Action): GameState => {
  const next = reducer(s, a);
  assert(next !== undefined, `reducer returned undefined for ${a.type}`);
  assert(Number.isFinite(next.money), `money is not finite after ${a.type}: ${next.money}`);
  assert(Number.isFinite(next.rep), `rep broken after ${a.type}`);
  assert(next.day >= 1, 'day invalid');
  return next;
};

/* 1. Стартовое состояние */
let s = createInitialState();
assert(s.machines.length === 1, 'должна быть стартовая машина');
assert(s.money === 5000, 'стартовые деньги');
assert(s.version === SAVE_VERSION_CURRENT, 'версия сохранения');

/* 2. Терминал */
const pc = s.machines[0];
s = send(s, { type: 'POWER', machineId: pc.id, on: true });
s = send(s, { type: 'INSTALL_OS', machineId: pc.id, os: 'linux' });
assert(s.machines[0].os === 'linux', 'Linux должен установиться');

const cmds = [
  'help', 'apt update', 'apt install samba', 'apt install nginx', 'systemctl status smbd',
  'free -h', 'df -h', 'lsblk', 'smartctl -a /dev/sda', 'sensors', 'top', 'ip a', 'ping 8.8.8.8',
  'curl localhost', 'docker compose up -d', 'neofetch', 'apt install minecraft-server',
  'apt install wireguard', 'systemctl stop smbd', 'ufw enable', 'memtester', 'rm -rf /', 'foobar',
];
for (const c of cmds) {
  const m = structuredClone(s.machines[0]);
  const res = execCommand(s, m, c);
  assert(res.lines.length >= 0, `command ${c} crashed`);
}
s = send(s, { type: 'CMD', machineId: s.machines[0].id, input: 'apt install samba' });
assert(s.machines[0].services.includes('smb'), 'SMB должен появиться после apt install samba');

/* 3. Магазин и торг */
const lot = s.listings[0];
s = send(s, { type: 'HAGGLE', listingId: lot.id, line: 'ask' });
s = send(s, { type: 'HAGGLE', listingId: lot.id, line: 'pressure' });
s = send(s, { type: 'HAGGLE', listingId: lot.id, line: 'offer' });
assert(s.listings.find((l) => l.id === lot.id)?.chat.length > 1, 'чат должен обновляться');

/* 4. Покупка и установка */
const buyable = s.listings.filter((l) => l.askPrice < s.money).sort((a, b) => a.askPrice - b.askPrice)[0];
if (buyable) {
  s = send(s, { type: 'BUY', listingId: buyable.id });
  assert(s.pending.length >= 1, 'покупка должна уйти в доставку');
}

/* 5. Апгрейды */
s = send(s, { type: 'BUY_UPGRADE', id: 'router' });
s = send(s, { type: 'BUY_UPGRADE', id: 'rack' });
assert(s.upgrades.includes('router'), 'роутер куплен');

/* 6. Обжим кабеля и DDoS */
s = send(s, { type: 'CABLE_DONE', perfect: true });
assert(s.cables >= 3, 'кабели должны добавиться');
s = send(s, { type: 'DDOS_RESULT', blocked: 30, total: 32 });

/* 7. Контракты */
s = send(s, { type: 'ACCEPT_CONTRACT', uid: s.offers[0]?.uid ?? 'nope' });
const ready = s.offers.find((o) => contractAvailable(s, o));
if (ready) {
  const before = s.contracts.length;
  s = send(s, { type: 'ACCEPT_CONTRACT', uid: ready.uid });
  assert(s.contracts.length === before + 1, 'контракт должен добавиться');
}

/* 8. 90 дней симуляции: покупаем всё, что можем, чиним, отвечаем на события */
for (let day = 0; day < 90; day += 1) {
  if (s.gameOver) break;
  if (s.event) {
    const affordable = s.event.choices.findIndex((c) => !c.cost || s.money >= c.cost);
    s = send(s, { type: 'EVENT_CHOICE', index: Math.max(0, affordable) });
  }
  // Скупаем железо
  for (const l of [...s.listings].sort((a, b) => a.askPrice - b.askPrice)) {
    if (s.money - l.askPrice > 1500) {
      s = send(s, { type: 'BUY', listingId: l.id });
    }
  }
  // Собираем машины из платформ
  for (const p of [...s.inventory].filter((p) => p.kind === 'base')) {
    if (s.upgrades.includes('rack') || p.machineKind === 'desktop' || p.machineKind === 'phone') {
      s = send(s, { type: 'CREATE_MACHINE', baseUid: p.uid });
    }
  }
  // Ставим детали в машины
  for (const part of [...s.inventory]) {
    if (part.kind === 'base') continue;
    const target = s.machines.find((m) => {
      if (part.kind === 'ram') return m.ram.some((r) => !r);
      if (part.kind === 'cpu') return m.cpus.some((c) => !c);
      if (part.kind === 'storage') return m.storage.some((d) => !d);
      return true;
    });
    if (target && !target.powered) {
      s = send(s, { type: 'INSTALL_PART', uid: part.uid, machineId: target.id });
    }
  }
  // ОС и сервисы
  for (const m of s.machines) {
    if (!m.powered && !m.crashed) s = send(s, { type: 'POWER', machineId: m.id, on: true });
    if (m.os === 'none' && m.storage.some(Boolean) && s.money > 2000) {
      s = send(s, { type: 'INSTALL_OS', machineId: m.id, os: 'linux' });
    }
    if (m.os === 'linux' && m.powered) {
      for (const svc of ['samba', 'nginx', 'jellyfin']) {
        if (!m.installed.includes(svc)) s = send(s, { type: 'CMD', machineId: m.id, input: `apt install ${svc}` });
      }
    }
  }
  // Апгрейды
  for (const id of ['switch', 'ups', 'backup', 'monitoring', 'soundproof', 'ac', 'firewall']) {
    if (!s.upgrades.includes(id)) s = send(s, { type: 'BUY_UPGRADE', id });
  }
  // Чистим пыль
  for (const m of s.machines) {
    if (m.dust > 2) s = send(s, { type: 'REPAIR', machineId: m.id, action: 'clean' });
  }
  // Контракты
  for (const off of [...s.offers]) {
    if (contractAvailable(s, off)) s = send(s, { type: 'ACCEPT_CONTRACT', uid: off.uid });
  }
  s = send(s, { type: 'NEXT_DAY' });
  assert(s.report !== null, 'после дня должен быть отчёт');
  s = send(s, { type: 'CLOSE_REPORT' });
  if (s.event) {
    const idx = s.event.choices.findIndex((c) => !c.cost || s.money >= c.cost);
    s = send(s, { type: 'EVENT_CHOICE', index: Math.max(0, idx) });
  }
}

console.log('--- Итог симуляции ---');
console.log('день:', s.day, 'деньги:', Math.round(s.money), 'репутация:', Math.round(s.rep), 'этап:', s.stage);
console.log('машины:', s.machines.length, 'контрактов активно:', s.contracts.length, 'выполнено:', s.stats.contractsDone);
console.log('событий в журнале:', s.log.length, 'победа:', s.victory, 'gameOver:', s.gameOver);

/* 9. Инварианты */
assert(s.machines.every((m) => Number.isFinite(m.temp)), 'температура должна быть числом');
assert(s.machines.every((m) => Number.isFinite(m.uptime)), 'аптайм должен быть числом');
assert(s.stats.contractsDone >= 0, 'счётчик контрактов');
assert(s.log.length <= 140, 'журнал не должен расти бесконечно');

/* 10. Проверка ОС Windows и графических действий */
let w = createInitialState();
w.money = 20000;
w.inventory.push(makePart('ram_ddr3_4', { flaw: 'none' }));
w = send(w, { type: 'INSTALL_PART', uid: w.inventory[w.inventory.length - 1].uid, machineId: w.machines[0].id });
const wm = w.machines[0];
w = send(w, { type: 'POWER', machineId: wm.id, on: true });
w = send(w, { type: 'INSTALL_OS', machineId: wm.id, os: 'windows' });
w = send(w, { type: 'WIN_ACTION', machineId: wm.id, action: 'desktop-experience' });
w = send(w, { type: 'WIN_ACTION', machineId: wm.id, action: 'share' });
w = send(w, { type: 'WIN_ACTION', machineId: wm.id, action: 'taskmgr' });
assert(w.machines[0].services.includes('smb'), 'Windows SMB должна работать');
assert(w.machines[0].desktopExperience, 'Desktop Experience должен ставиться');

/* 11. Смартфон */
let p = createInitialState();
const phone = p.inventory.find((i) => i.defId === 'base_phone');
if (phone) {
  p = send(p, { type: 'CREATE_MACHINE', baseUid: phone.uid });
  const pm = p.machines.find((m) => m.kind === 'phone')!;
  p = send(p, { type: 'POWER', machineId: pm.id, on: true });
  p = send(p, { type: 'INSTALL_OS', machineId: pm.id, os: 'linux' });
  p = send(p, { type: 'WIN_ACTION', machineId: pm.id, action: 'tiny' });
  assert(p.machines.find((m) => m.id === pm.id)!.services.includes('tinyllm'), 'tinyllm должен ставиться на телефон');
}

/* 12. Сброс */
const r = send(s, { type: 'RESET' });
assert(r.day === 1 && r.money === 5000, 'reset должен вернуть старт');

/* 13. Проверка блокировщиков контрактов */
let q = createInitialState();
q.money = 100000;
for (const off of q.offers) {
  const blocker = contractAvailable(q, off);
  assert(typeof blocker === 'boolean', 'contractAvailable должен вернуть boolean');
}

/* 14. Firewall и derive */
assert(firewallScore(q) > 0 && firewallScore(q) <= 100, 'firewall в диапазоне 0..100');
assert(derive(q.machines[0], q).cores === 4, 'стартовый i5 = 4 ядра');
assert(usableRam(q.machines[0]) === 4096 - 0 - 64, 'без ОС доступна вся память минус 64');
assert(derive(q.machines[0], q).ramMB === 4096, 'стартовые 4 ГБ памяти');

/* 15. Деталь с дефектом */
const broken = makePart('hdd_320', { flaw: 'badsectors' });
assert(broken.flaw === 'badsectors', 'дефект задаётся явно');

console.log(failures === 0 ? '✅ ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ' : `❌ ПРОВАЛЕНО ПРОВЕРОК: ${failures}`);
process.exit(failures === 0 ? 0 : 1);
