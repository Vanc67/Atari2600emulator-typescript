/* PC Architect & Debug Cheats 8.0: node tests/architect-cheats.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','pc-data','pc-architect-data','audio','long-lofi','pc-sound','game','systems','lab','server-sound','timeline','deep-lab','market-search','avito-years','pc-workshop','pc-architect','debug','cheats']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh(pc = true) { const game = new Game(false); if (pc) game.startPCCampaign(); game.events.length = 0; return game; }

test('PC Architect: ровно 20 000 новых деталей, уникальные ID и прежние архивы нетронуты', () => {
  assert.equal(DATA.PC_PART_CATALOG.length, 20000); assert.equal(new Set(DATA.PC_PART_CATALOG.map(part => part.id)).size, 20000);
  assert.equal(new Set(DATA.PC_PART_CATALOG.map(part => part.name)).size, 20000); assert.equal(new Set(DATA.PC_PART_CATALOG.map(part => part.sku)).size, 20000);
  assert.deepEqual(Object.fromEntries(Object.entries(DATA.PC_PART_TYPES).map(([id, item]) => [id, item.count])), { cpu: 2800, motherboard: 2800, ram: 2800, gpu: 2800, storage: 2400, psu: 2000, case: 2200, cooling: 2200 });
  assert.equal(DATA.PC_GENERATIONS.length, 37); assert.equal(DATA.PARTS.length, 5570); assert.equal(DATA.SERVER_TEMPLATES.length, 2597); assert.equal(DATA.PC_OS.length, 13);
});

test('PC Architect: все 37 лет содержат все 8 категорий, характеристики конечны', () => {
  for (let year = 1990; year <= 2026; year += 1) Object.keys(DATA.PC_PART_TYPES).forEach(type => {
    const parts = DATA.PC_PART_BY_TYPE_YEAR.get(`${type}:${year}`); assert.ok(parts && parts.length >= 50, `${type}:${year}`);
    parts.forEach(part => { assert.equal(part.year, year); assert.equal(part.type, type); assert.ok(part.name && part.summary); assert.ok(Number.isFinite(part.value) && part.value > 0); });
  });
});

test('PC Architect: автоподбор даёт совместимые 8 деталей для каждого года 1990–2026', () => {
  const game = fresh();
  for (let year = 1990; year <= 2026; year += 1) {
    game.state.pc.year = year; const report = game.pcAutoDraft(year);
    assert.ok(report, `автоподбор ${year}`); assert.equal(report.ok, true, `${year}: ${report.errors.join(', ')}`); assert.equal(report.missing.length, 0);
    assert.equal(new Set(Object.values(game.pcArchitectState().draft)).size, 8); assert.equal(report.metrics.year, year);
  }
});

test('PC Architect: сокет, RAM, GPU, PSU и размеры реально блокируют несовместимую сборку', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoDraft(2005); const draft = game.pcArchitectState().draft;
  const otherRam = DATA.PC_PART_CATALOG.find(part => part.type === 'ram' && part.year === 2005 && part.ramType !== report.parts.ram.ramType);
  game.setPCDraftPart('ram', otherRam.id); assert.equal(game.pcDraftCompatibility().ok, false); assert.ok(game.pcDraftCompatibility().errors.some(error => error.includes('Память')));
  game.setPCDraftPart('ram', draft.ram);
  const otherCpu = DATA.PC_PART_CATALOG.find(part => part.type === 'cpu' && part.year === 2005 && part.socket !== report.parts.motherboard.socket);
  game.setPCDraftPart('cpu', otherCpu.id); assert.ok(game.pcDraftCompatibility().errors.some(error => error.includes('Сокет')));
  game.setPCDraftPart('cpu', draft.cpu);
  game.state.pc.year = 2020; game.pcAutoDraft(2020);
  const hotCpu = DATA.PC_PART_CATALOG.filter(part => part.type === 'cpu' && part.year === 2020).sort((a, b) => b.tdp - a.tdp)[0];
  const hotGpu = DATA.PC_PART_CATALOG.filter(part => part.type === 'gpu' && part.year === 2020).sort((a, b) => b.tdp - a.tdp)[0];
  const weakPsu = DATA.PC_PART_CATALOG.filter(part => part.type === 'psu' && part.year === 2020).sort((a, b) => a.watts - b.watts)[0];
  game.setPCDraftPart('cpu', hotCpu.id); game.setPCDraftPart('gpu', hotGpu.id); game.setPCDraftPart('psu', weakPsu.id);
  assert.ok(game.pcDraftCompatibility().errors.some(error => error.includes('PSU')));
});

test('PC Architect: поиск, бренд, сокет, сортировка, страницы и compatible-only', () => {
  const game = fresh(); game.state.pc.year = 2005;
  const page = game.pcCatalog({ type: 'cpu', year: 2005, perPage: 12 }); assert.equal(page.items.length, 12); assert.ok(page.total > 12 && page.pages > 1);
  const brand = page.items[0].brand; const branded = game.pcCatalog({ type: 'cpu', year: 2005, query: brand, perPage: 30 }); assert.ok(branded.items.length > 0); assert.ok(branded.items.every(part => part.name.includes(brand)));
  const auto = game.pcAutoDraft(2005); const compatible = game.pcCatalog({ type: 'cpu', year: 2005, compatibleOnly: true, perPage: 30 });
  assert.ok(compatible.items.length > 0); assert.ok(compatible.items.every(part => part.socket === auto.parts.motherboard.socket));
  const sorted = game.pcCatalog({ type: 'cpu', year: 2005, sort: 'priceDesc', perPage: 30 }); for (let i = 1; i < sorted.items.length; i += 1) assert.ok(sorted.items[i - 1].value >= sorted.items[i].value);
});

test('PC Architect: сборка списывает деньги один раз и создаёт настоящий Custom PC без ОС', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoDraft(2005); const before = game.state.pc.computers.length;
  game.state.pc.money = report.metrics.total + 5000; const result = game.buildCustomPC({ name: 'Моя первая сборка' });
  assert.equal(result.ok, true); assert.equal(game.state.pc.money, 5000); assert.equal(game.state.pc.computers.length, before + 1);
  const machine = result.machine; assert.equal(machine.custom, true); assert.equal(machine.name, 'Моя первая сборка'); assert.equal(machine.os, null); assert.equal(machine.powered, false);
  assert.equal(game.pcCustomParts(machine).length, 8); assert.equal(machine.cpu, report.parts.cpu.name); assert.equal(machine.ramMb, report.parts.ram.capacityMb);
  assert.equal(Object.values(game.pcArchitectState().draft).every(value => value === null), true);
});

test('PC Architect: нехватка денег не создаёт ПК и не очищает draft', () => {
  const game = fresh(); game.state.pc.year = 2010; game.pcAutoDraft(2010); const draft = Object.assign({}, game.pcArchitectState().draft); const before = game.state.pc.computers.length;
  game.state.pc.money = 0; const result = game.buildCustomPC(); assert.equal(result.ok, false); assert.equal(game.state.pc.computers.length, before); assert.deepEqual(game.pcArchitectState().draft, draft);
});

test('PC Architect: Custom PC использует существующие ОС, питание и Stability Suite', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoDraft(2005); game.state.pc.money = 100000;
  const machine = game.buildCustomPC().machine; assert.equal(game.installPCOS(machine.id, 'pc-winxp'), true); assert.equal(machine.os, 'pc-winxp');
  assert.equal(game.togglePCPower(machine.id), true); assert.equal(machine.powered, true);
  const stability = game.runPCBenchmark(machine.id, 'stability'); assert.equal(stability.passed, true); assert.ok(machine.benchmarks.stability);
});

test('PC Architect: JSON normalize сохраняет Custom PC, draft, каталог ID и прежнюю версию state', () => {
  const game = fresh(); game.state.pc.year = 2012; game.pcAutoDraft(2012); game.state.pc.money = 1000000; const machine = game.buildCustomPC().machine; game.pcAutoDraft(2015);
  const draft = Object.assign({}, game.pcArchitectState().draft); const snapshot = JSON.parse(JSON.stringify(game.state)); game.state = snapshot; game.normalize();
  assert.equal(game.state.version, 9); assert.equal(game.getPC(machine.id).custom, true); assert.equal(game.pcCustomParts(machine).length, 8); assert.deepEqual(game.pcArchitectState().draft, draft);
  assert.ok(game.state.pc.architect && game.state.pc.cheats && game.state.cheats);
});

test('Avito Years: старые и новые лоты получают год без удаления или замены', () => {
  const game = fresh(false); const before = game.state.listings.map(listing => listing.id); const expected = game.state.listings.map(listing => game.listingDetails(listing).year);
  game.state.listings.forEach(listing => { delete listing.year; }); game.normalize();
  assert.deepEqual(game.state.listings.map(listing => listing.id), before); assert.deepEqual(game.state.listings.map(listing => listing.year), expected);
  const generated = game.generateListingSet(); assert.ok(generated.every(listing => Number.isFinite(listing.year)));
});

test('Cheats: любой год независим для серверов/PC и не очищает Авито или компьютеры', () => {
  const game = fresh(); const listings = game.state.listings.map(listing => listing.id); const computers = game.state.pc.computers.map(machine => machine.id);
  assert.equal(game.cheatSetYear(1995, 'servers'), true); assert.equal(game.cheatSetYear(2024, 'pc'), true);
  assert.equal(game.state.currentYear, 1995); assert.equal(game.state.startYear, 1995); assert.equal(game.state.pc.year, 2024);
  assert.deepEqual(game.state.listings.map(listing => listing.id), listings); assert.deepEqual(game.state.pc.computers.map(machine => machine.id), computers);
  game.normalize(); assert.equal(game.state.currentYear, 1995); assert.equal(game.state.pc.year, 2024);
});

test('Cheats: точные деньги, добавление, день, уровень и репутация', () => {
  const game = fresh(); game.cheatSetMoney(123456, 'servers'); game.cheatSetMoney(654321, 'pc');
  assert.equal(game.state.money, 123456); assert.equal(game.state.pc.money, 654321);
  game.cheatAddMoney(1000, 'pc'); assert.equal(game.state.pc.money, 655321);
  game.cheatSetDay(77, 'pc'); game.cheatSetDay(33, 'servers'); assert.equal(game.state.pc.day, 77); assert.equal(game.state.day, 33);
  game.cheatSetLevel(8, 'pc'); game.cheatSetLevel(6, 'servers'); game.cheatSetReputation(42, 'pc');
  assert.equal(game.state.pc.level, 8); assert.equal(game.state.level, 6); assert.equal(game.state.pc.reputation, 42);
});

test('Cheats: ремонт, эпохи, улучшения и долг не удаляют игровые объекты', () => {
  const game = fresh(); const machine = game.state.pc.computers[0]; machine.issues = [{ id: 'dust', revealed: false, resolved: false }]; machine.health = 20; machine.crashed = true;
  const server = game.state.servers[0]; server.health = 10; server.broken = true; const servers = game.state.servers.length; const computers = game.state.pc.computers.length;
  game.cheatRepairAll('pc'); game.cheatRepairAll('servers'); assert.equal(machine.health, 100); assert.equal(machine.issues[0].resolved, true); assert.equal(server.health, 100); assert.equal(server.broken, false);
  game.cheatUnlockAll('pc'); game.cheatUnlockAll('servers'); game.cheatMaxUpgrades('pc'); game.cheatMaxUpgrades('servers');
  assert.equal(game.state.pc.year, 2026); assert.equal(game.state.currentYear, 2026); assert.equal(game.state.pc.cheats.unlockAll, true); assert.equal(game.state.pc.upgrades.ads, 5); assert.equal(game.state.research.cooling, 5);
  game.state.debt = 999; game.cheatClearDebt(); assert.equal(game.state.debt, 0); assert.equal(game.state.servers.length, servers); assert.equal(game.state.pc.computers.length, computers);
});

test('Cheats: бесплатная авто-сборка совместима и действительно не списывает деньги', () => {
  const game = fresh(); game.state.pc.money = 0; const before = game.state.pc.computers.length; const machine = game.cheatFreePC(2005);
  assert.ok(machine); assert.equal(machine.custom, true); assert.equal(machine.year, 2005); assert.equal(game.state.pc.money, 0); assert.equal(game.state.pc.computers.length, before + 1);
  assert.equal(game.pcCustomParts(machine).length, 8); assert.ok(game.cheatHistory().some(entry => entry.kind === 'FREE CUSTOM PC'));
});

test('Миграция 7.1 → 8.0 только добавляет architect/cheats и годы, сохраняя контент', () => {
  const game = fresh(); const machine = game.cheatFreePC(2000); const computers = game.state.pc.computers.length; const listings = game.state.listings.length;
  delete game.state.pc.architect; delete game.state.pc.cheats; delete game.state.cheats; game.state.listings.forEach(listing => { delete listing.year; });
  game.normalize(); assert.ok(game.state.pc.architect && game.state.pc.cheats && game.state.cheats); assert.equal(game.state.pc.computers.length, computers); assert.equal(game.state.listings.length, listings);
  assert.equal(game.getPC(machine.id).custom, true); assert.ok(game.state.listings.every(listing => Number.isFinite(listing.year)));
});

console.log(`\nPC Architect & Cheats 8.0: ${passed} scenarios passed`);
