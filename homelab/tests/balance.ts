import { createInitialState, reducer } from '../src/game/game';
import { contractAvailable, derive, firewallScore, usableRam } from '../src/game/rules';
import { makePart } from '../src/game/market';
import type { GameState } from '../src/game/types';

/* Симуляция «нормального игрока»: покупка по потребности, установка сервисов,
   заключение контрактов, апгрейды. Проверяет, что прогресс вообще достижим. */

let s: GameState = createInitialState();
const log = (...args: unknown[]) => console.log(...args);
const act = (a: Parameters<typeof reducer>[1]) => {
  s = reducer(s, a);
};

function buyCheapest(kind: string, maxPrice = 999999): boolean {
  const lots = s.listings.filter((l) => l.part.kind === kind && l.askPrice <= maxPrice).sort((a, b) => a.askPrice - b.askPrice);
  if (!lots.length) return false;
  act({ type: 'BUY', listingId: lots[0].id });
  return true;
}

function installAll(machineId: string) {
  for (const part of [...s.inventory]) {
    if (part.kind === 'base') continue;
    act({ type: 'INSTALL_PART', uid: part.uid, machineId });
  }
}

function installServices(machineId: string) {
  const m = s.machines.find((x) => x.id === machineId);
  if (!m || m.os !== 'linux' || !m.powered) return;
  // Ставим только пакеты, которые нужны активным/предложенным контрактам
  const wanted = new Set<string>();
  for (const c of [...s.contracts.filter((c) => c.active), ...s.offers]) {
    (c.requirements.services ?? []).forEach((svc) => {
      const pkg = ({ smb: 'samba', webserver: 'nginx', jellyfin: 'jellyfin', minecraft: 'minecraft-server', cctv: 'motion', vpn: 'wireguard', mail: 'postfix', torrent: 'qbittorrent-nox', backup: 'restic', gameserver: 'hlds', dns: 'unbound' } as Record<string, string>)[svc];
      if (pkg) wanted.add(pkg);
    });
  }
  for (const pkg of wanted) act({ type: 'CMD', machineId, input: `apt install ${pkg}` });
}

function acceptEverything() {
  for (const off of [...s.offers]) {
    if (contractAvailable(s, off)) act({ type: 'ACCEPT_CONTRACT', uid: off.uid });
  }
}

for (let day = 1; day <= 90; day += 1) {
  if (s.event) act({ type: 'EVENT_CHOICE', index: 0 });

  const main = s.machines[0];
  // Первичная сборка
  if (main && !main.powered) act({ type: 'POWER', machineId: main.id, on: true });
  if (main && main.os === 'none' && s.money > 500) act({ type: 'INSTALL_OS', machineId: main.id, os: 'linux' });

  // Докупаем память, если её мало
  if (main && s.money > 2000 && usableRam(main) < 7000) buyCheapest('ram', 1600);
  // Докупаем диски, если места мало
  if (main && s.money > 2500) {
    const d = derive(main, s);
    if (d.storageGB < 900) buyCheapest('storage', 2600);
  }
  installAll(main?.id ?? '');

  // Апгрейды по приоритету
  const priority = ['router', 'rack', 'switch', 'ups', 'backup', 'monitoring', 'soundproof', 'ac', 'firewall', 'switch10g', 'employees', 'generator'];
  for (const id of priority) {
    if (!s.upgrades.includes(id) && s.money > 9000) act({ type: 'BUY_UPGRADE', id });
  }
  // Останавливаем лишние сервисы, если не хватает ресурсов
  for (const m of s.machines) {
    if (m.services.length > 3) {
      const extra = m.services.slice(2);
      for (const svc of extra) act({ type: 'CMD', machineId: m.id, input: `systemctl stop ${svc}` });
    }
  }

  // Ремонт и профилактика
  for (const m of s.machines) {
    if (m.crashed) act({ type: 'REPAIR', machineId: m.id, action: 'fix-flaw' });
    if (m.dust > 2) act({ type: 'REPAIR', machineId: m.id, action: 'clean' });
    if (!m.powered && !m.crashed) act({ type: 'POWER', machineId: m.id, on: true });
    if (m.os === 'none' && s.money > 1000) act({ type: 'INSTALL_OS', machineId: m.id, os: 'linux' });
    installServices(m.id);
  }

  // Второй сервер на этапе 2+
  if (s.stage >= 2 && s.machines.length < 2) {
    const base = s.listings.find((l) => l.part.kind === 'base' && l.part.machineKind === 'rack' && l.askPrice < s.money - 5000);
    if (base) {
      act({ type: 'BUY', listingId: base.id });
      act({ type: 'NEXT_DAY' });
      act({ type: 'CLOSE_REPORT' });
      if (s.event) act({ type: 'EVENT_CHOICE', index: 0 });
      const basePart = s.inventory.find((p) => p.kind === 'base' && p.machineKind === 'rack');
      if (basePart) {
        act({ type: 'CREATE_MACHINE', baseUid: basePart.uid });
        const nm = s.machines[s.machines.length - 1];
        installAll(nm.id);
        act({ type: 'POWER', machineId: nm.id, on: true });
        act({ type: 'INSTALL_OS', machineId: nm.id, os: 'linux' });
        installServices(nm.id);
      }
      continue;
    }
  }

  acceptEverything();

  if (day % 10 === 0) {
    log(
      `день ${s.day}: ${Math.round(s.money)} ₽ | этап ${s.stage} | реп ${Math.round(s.rep)} | контрактов ${s.stats.contractsDone} | активных ${s.contracts.length} | машин ${s.machines.length} | fw ${firewallScore(s)} | семья ${Math.round(s.family)}`,
    );
  }

  act({ type: 'NEXT_DAY' });
  act({ type: 'CLOSE_REPORT' });
  if (s.event) act({ type: 'EVENT_CHOICE', index: 0 });
  if (s.gameOver) {
    log('GAME OVER на дне', s.day);
    break;
  }
}

log('--- ИТОГ ---');
log('день', s.day, 'деньги', Math.round(s.money), 'этап', s.stage, 'репутация', Math.round(s.rep), 'победа', s.victory);
log('контрактов выполнено', s.stats.contractsDone, 'провалено', s.log.filter((l) => l.text.includes('сорван')).length);
log('Downtime/аварии', s.stats.crashes, 'events', s.log.length);
const errors = s.log.filter((l) => l.text.includes('undefined') || l.text.includes('NaN'));
log('ошибок в логе:', errors.length, errors.slice(0, 3).map((e) => e.text));

// диагностика
log('диагностика:');
for (const m of s.machines) {
  const d = derive(m, s);
  log(' ', m.name, 'os', m.os, 'power', m.powered, 'crashed', m.crashed, 'services', m.services, 'ram', d.ramMB, 'usable', d.usableRam, 'issues', d.issues);
}
log(' offers', s.offers.map((o) => `${o.id}:${contractAvailable(s, o)}`));
