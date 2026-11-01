/* Architect+ 8.1: node tests/plus.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','pc-data','pc-architect-data','audio','long-lofi','pc-sound','game','systems','lab','server-sound','timeline','deep-lab','market-search','avito-years','pc-workshop','pc-architect','pc-plus','debug','cheats']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh() { const game = new Game(false); game.startPCCampaign(); game.events.length = 0; return game; }
function custom(game, year = 2005) { game.state.pc.year = year; game.pcAutoDraft(year); game.state.pc.money = 100000000; const result = game.buildCustomPC(); assert.equal(result.ok, true); return result.machine; }

test('Blueprints: save → clear → load возвращает draft и не меняет исходник', () => {
  const game = fresh(); game.state.pc.year = 2005; game.pcAutoDraft(2005); const draft = Object.assign({}, game.pcArchitectState().draft);
  const blueprint = game.pcSaveBlueprint('Тестовый проект'); assert.ok(blueprint); assert.equal(blueprint.name, 'Тестовый проект');
  game.clearPCDraft(); assert.equal(Object.values(game.pcArchitectState().draft).every(value => value === null), true);
  assert.ok(game.pcLoadBlueprint(blueprint.id)); assert.deepEqual(game.pcArchitectState().draft, draft);
  game.clearPCDraft(); assert.deepEqual(blueprint.draft, draft); assert.equal(game.pcPlusState().blueprints.length, 1);
});

test('Blueprints: copy/archive/restore не удаляют проекты и сохраняют независимые draft', () => {
  const game = fresh(); game.pcAutoDraft(2000); const source = game.pcSaveBlueprint('Original'); const copy = game.pcDuplicateBlueprint(source.id);
  assert.notEqual(copy.id, source.id); assert.deepEqual(copy.draft, source.draft); assert.equal(game.pcPlusState().blueprints.length, 2);
  game.pcArchiveBlueprint(source.id, true); assert.equal(source.archived, true); assert.equal(game.pcLoadBlueprint(source.id), false); assert.equal(game.pcPlusState().blueprints.length, 2);
  game.pcArchiveBlueprint(source.id, false); assert.equal(source.archived, false); assert.ok(game.pcLoadBlueprint(source.id));
});

test('Blueprint JSON: offline export/import, строгая валидация и реальная сборка', () => {
  const game = fresh(); game.state.pc.year = 2005; game.pcAutoDraft(2005); const source = game.pcSaveBlueprint('JSON Build');
  const exported = game.pcExportBlueprint(source.id); const payload = JSON.parse(exported); assert.equal(payload.format, 'server-tycoon-pc-blueprint'); assert.equal(payload.version, 1);
  const imported = game.pcImportBlueprint(exported); assert.ok(imported); assert.notEqual(imported.id, source.id);
  const before = game.pcPlusState().blueprints.length; assert.equal(game.pcImportBlueprint('{bad'), null); assert.equal(game.pcImportBlueprint('{"format":"other"}'), null);
  const invalid = JSON.parse(exported); invalid.draft.cpu = 'missing-id'; assert.equal(game.pcImportBlueprint(JSON.stringify(invalid)), null); assert.equal(game.pcPlusState().blueprints.length, before);
  assert.ok(game.pcLoadBlueprint(imported.id)); game.state.pc.money = 1000000; const result = game.buildCustomPC(); assert.equal(result.ok, true); assert.equal(result.machine.custom, true);
});

test('Избранное: favoritesOnly фильтрует весь каталог до пагинации', () => {
  const game = fresh(); game.state.pc.year = 2005; const page = game.pcCatalog({ type: 'cpu', year: 2005, perPage: 12 }); const part = page.items[0];
  assert.equal(game.pcToggleFavorite(part.id), true); const favorite = game.pcCatalog({ type: 'cpu', year: 2005, favoritesOnly: true, perPage: 12 });
  assert.equal(favorite.total, 1); assert.equal(favorite.items[0].id, part.id);
  assert.equal(game.pcToggleFavorite(part.id), false); assert.equal(game.pcCatalog({ type: 'cpu', year: 2005, favoritesOnly: true }).total, 0);
});

test('Сравнение: до трёх деталей одной категории, без изменения draft', () => {
  const game = fresh(); const cpus = game.pcCatalog({ type: 'cpu', year: 2005, perPage: 12 }).items; const draft = Object.assign({}, game.pcArchitectState().draft);
  cpus.slice(0, 3).forEach(part => assert.equal(game.pcToggleCompare(part.id), true)); assert.equal(game.pcCompareParts().length, 3);
  assert.equal(game.pcToggleCompare(cpus[3].id), false); assert.equal(game.pcCompareParts().length, 3);
  const ram = game.pcCatalog({ type: 'ram', year: 2005, perPage: 4 }).items[0]; assert.equal(game.pcToggleCompare(ram.id), false);
  assert.deepEqual(game.pcArchitectState().draft, draft); game.pcToggleCompare(cpus[0].id); assert.equal(game.pcCompareParts().length, 2);
});

test('Тюнинг: CPU/GPU scale, Turbo и Silent влияют на ватты, температуру и шум', () => {
  const game = fresh(); const machine = custom(game); const base = game.pcTuningPreview(machine.id, { cpu: 0, gpu: 0, fan: 'balanced' });
  assert.ok(machine.apps && machine.tuning); assert.equal(game.setPCTuning(machine.id, 'cpu', 10), true); assert.equal(game.setPCTuning(machine.id, 'gpu', 10), true); assert.equal(game.setPCTuning(machine.id, 'fan', 'turbo'), true);
  const tuned = game.pcTuningPreview(machine.id); assert.ok(tuned.scores.cpu > base.scores.cpu && tuned.scores.gpu > base.scores.gpu); assert.ok(tuned.metrics.watts > base.metrics.watts); assert.ok(tuned.metrics.noise > base.metrics.noise); assert.equal(machine.tuningRevision, 3);
  const silent = game.pcTuningPreview(machine.id, { cpu: 0, gpu: 0, fan: 'silent' }); assert.ok(silent.metrics.targetTemp > base.metrics.targetTemp); assert.ok(silent.metrics.noise < base.metrics.noise);
  machine.powered = true; assert.equal(game.setPCTuning(machine.id, 'cpu', 20), false); assert.equal(machine.tuning.cpu, 10);
});

test('Тюнинг: недостаточный PSU блокирует разгон без изменения прошлой настройки', () => {
  const game = fresh(); const machine = custom(game); const candidate = game.pcTuningPreview(machine.id, { cpu: 20, gpu: 0, fan: 'balanced' });
  machine.psuWatts = Math.floor(candidate.metrics.watts - 1); assert.equal(game.setPCTuning(machine.id, 'cpu', 20), false); assert.equal(machine.tuning.cpu, 0); assert.equal(machine.tuningRevision, 0);
});

test('Benchmarks: разгон повышает FPS, но уменьшает Stability и попадает в metadata', () => {
  const game = fresh(); const machine = custom(game); game.installPCOS(machine.id, 'pc-winxp'); machine.powered = true; machine.temp = 24;
  const stability = game.runPCBenchmark(machine.id, 'stability'); machine.temp = 24; const gameBase = game.runPCBenchmark(machine.id, 'doom3');
  machine.powered = false; assert.ok(game.setPCTuning(machine.id, 'cpu', 10)); assert.ok(game.setPCTuning(machine.id, 'gpu', 10)); machine.powered = true; machine.crashed = false; machine.temp = 24;
  const stabilityOC = game.runPCBenchmark(machine.id, 'stability'); machine.temp = 24; const gameOC = game.runPCBenchmark(machine.id, 'doom3');
  assert.ok(stabilityOC.fps < stability.fps); assert.ok(gameOC.fps > gameBase.fps); assert.equal(gameOC.tuning.cpu, 10); assert.equal(gameOC.tuningRevision, 2); assert.ok(gameOC.performanceScores.cpu > machine.cpuScore);
  assert.ok(machine.benchmarks.stability && machine.benchmarks.doom3);
});

test('Notes: текст и revision сохраняются сразу и после JSON normalize', () => {
  const game = fresh(); const machine = custom(game); assert.ok(machine.apps); assert.equal(game.pcSaveNotes(machine.id, 'CPU: проверить\nПароль: только offline ✓'), true);
  assert.equal(machine.apps.notesRevision, 1); game.state = JSON.parse(JSON.stringify(game.state)); game.normalize();
  assert.equal(game.getPC(machine.id).apps.notes, 'CPU: проверить\nПароль: только offline ✓'); assert.equal(game.getPC(machine.id).apps.notesRevision, 1);
  for (let i = 0; i < 14; i += 1) game.pcRecordCalculation(machine.id, `${i}+1=`, i + 1); assert.equal(game.getPC(machine.id).apps.calcHistory.length, 12);
});

test('Сапёр: флажок работает, первый ход и соседи всегда безопасны', () => {
  const game = fresh(); const machine = custom(game); const board = game.pcMinesNew(machine.id); assert.equal(board.status, 'ready');
  assert.equal(game.pcMinesFlag(machine.id, 0), true); assert.equal(game.pcMinesReveal(machine.id, 0), false); assert.equal(board.status, 'ready'); assert.equal(game.pcMinesFlag(machine.id, 0), true);
  const result = game.pcMinesReveal(machine.id, 0); assert.equal(result.status, 'playing'); assert.equal(board.mines.length, 10); assert.equal(board.mines.includes(0), false);
  const safeNeighbors = [1, 9, 10]; safeNeighbors.forEach(index => assert.equal(board.mines.includes(index), false)); assert.ok(board.revealed.includes(0));
  game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); assert.deepEqual(game.getPC(machine.id).apps.mines.board.mines, board.mines);
});

test('Сапёр: открытие всех безопасных клеток даёт победу и статистику', () => {
  const game = fresh(); const machine = custom(game); const board = game.pcMinesNew(machine.id); game.pcMinesReveal(machine.id, 0);
  for (let index = 0; index < 81; index += 1) if (!board.mines.includes(index) && !board.revealed.includes(index)) game.pcMinesReveal(machine.id, index);
  assert.equal(board.status, 'won'); assert.equal(board.revealed.length, 71); assert.equal(machine.apps.mines.games, 1); assert.equal(machine.apps.mines.wins, 1); assert.ok(Number.isFinite(machine.apps.mines.bestSeconds)); assert.equal(board.flags.length, 10);
});

test('Сапёр: мина после безопасного старта даёт проигрыш, серия продолжается', () => {
  const game = fresh(); const machine = custom(game); let board = game.pcMinesNew(machine.id); game.pcMinesReveal(machine.id, 0); const mine = board.mines[0]; const lost = game.pcMinesReveal(machine.id, mine);
  assert.equal(lost.status, 'lost'); assert.equal(board.exploded, mine); assert.equal(machine.apps.mines.games, 1); assert.equal(machine.apps.mines.wins, 0);
  board = game.pcMinesNew(machine.id); assert.equal(board.status, 'ready'); assert.equal(machine.apps.mines.games, 1);
});

test('Новые ПК сразу получают apps, Custom — tuning, без ожидания reload', () => {
  const game = fresh(); const regular = game.createPC(2010, { origin: 'owned' }); game.state.pc.computers.push(regular); const machine = custom(game, 2010);
  assert.ok(regular.apps && regular.apps.mines); assert.ok(machine.apps && machine.tuning); assert.equal(machine.tuning.fan, 'balanced'); assert.ok(game.pcMinesNew(regular.id));
});

test('Улучшение мастерской пишет ровно одну запись в журнал', () => {
  const game = fresh(); game.state.pc.money = 10000; const before = game.state.pc.logs.length; assert.equal(game.buyPCUpgrade('tools'), true);
  assert.equal(game.state.pc.logs.length - before, 1); assert.match(game.state.pc.logs[0].text, /tools уровень 1/);
});

test('Багфикс: бесплатный Custom PC не затирает текущий draft/название/год', () => {
  const game = fresh(); game.state.pc.money = 0;
  assert.ok(game.pcAutoDraft(2015).ok);
  game.pcArchitectState().name = 'Незавершённый проект';
  const before = JSON.stringify(game.pcArchitectState().draft); const count = game.state.pc.computers.length; const year = game.pcArchitectState().selectedYear;
  assert.ok(game.cheatFreePC(2012));
  assert.equal(game.state.pc.computers.length, count + 1); assert.equal(game.state.pc.money, 0);
  assert.equal(JSON.stringify(game.pcArchitectState().draft), before); assert.equal(game.pcArchitectState().name, 'Незавершённый проект'); assert.equal(game.pcArchitectState().selectedYear, year);
});

test('Миграция 8.0 → 8.1 только добавляет apps/projects/tuning и сохраняет контент', () => {
  const game = fresh(); const machine = custom(game); const computers = game.state.pc.computers.length; const listings = game.state.listings.length;
  delete game.state.pc.architect.blueprints; delete game.state.pc.architect.favorites; delete game.state.pc.architect.comparison; delete machine.apps; delete machine.tuning;
  game.normalize(); assert.deepEqual(game.pcPlusState().blueprints, []); assert.deepEqual(game.pcPlusState().favorites, []); assert.ok(machine.apps && machine.tuning);
  assert.equal(game.state.pc.computers.length, computers); assert.equal(game.state.listings.length, listings); assert.equal(DATA.PC_PART_CATALOG.length, 20000); assert.equal(DATA.PARTS.length, 5570);
});

console.log(`\nArchitect+ 8.1: ${passed} scenarios passed`);
