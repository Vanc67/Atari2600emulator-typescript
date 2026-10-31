/* Ultimate Dual Mode 7.0: node tests/ultimate.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','pc-data','audio','long-lofi','pc-sound','game','systems','lab','timeline','deep-lab','market-search','pc-workshop','debug']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh() { const game = new Game(false); game.events.length = 0; return game; }
function pcGame() { const game = fresh(); game.startPCCampaign(); game.events.length = 0; return game; }
function addPC(game, year, issues = [], origin = 'owned') { const pc = game.createPC(year, { origin, issues }); game.state.pc.computers.push(pc); return pc; }

test('Ultimate data: 37 поколений, 13 PC OS, 11 тестов, state version 9', () => {
  const game = fresh();
  assert.equal(DATA.PC_GENERATIONS.length, 37); assert.equal(DATA.PC_GENERATIONS[0].year, 1990); assert.equal(DATA.PC_GENERATIONS[36].year, 2026);
  assert.equal(DATA.PC_OS.length, 13); assert.equal(Object.keys(DATA.PC_BENCHMARKS).length, 11);
  assert.equal(game.state.version, 9); assert.equal(game.state.campaignMode, 'servers'); assert.equal(game.state.pc.started, false);
});

test('Папин старт 1990: тестовый 386, 2500 ₽, друзья и независимый server balance', () => {
  const game = pcGame(); const serverMoney = game.state.money;
  assert.equal(game.state.pc.year, 1990); assert.equal(game.state.pc.money, 2500); assert.equal(game.state.pc.computers[0].name, 'Папин тестовый 386');
  assert.equal(game.state.pc.offers.length, 3); assert.ok(game.state.pc.offers.every(offer => offer.source === 'friend'));
  game.state.pc.money -= 500; assert.equal(game.state.money, serverMoney);
});

test('Два режима независимо сохраняются и восстанавливаются через normalize', () => {
  const game = pcGame(); game.state.pc.money = 7777; game.state.pc.year = 2005; game.switchCampaign('servers');
  const restored = fresh(); restored.state = JSON.parse(JSON.stringify(game.state)); restored.normalize();
  assert.equal(restored.state.version, 9); assert.equal(restored.state.pc.money, 7777); assert.equal(restored.state.pc.year, 2005);
  assert.equal(restored.state.campaignMode, 'servers'); assert.equal(restored.state.servers.length, game.state.servers.length);
  restored.switchCampaign('pc'); assert.equal(restored.state.campaignMode, 'pc');
});

test('Миграция 6.0 → 7.0 сохраняет ферму/Deep Lab и добавляет незапущенный PC-режим', () => {
  const game = fresh(); const servers = JSON.stringify(game.state.servers); const lab = JSON.stringify(game.state.lab);
  game.state.version = 8; delete game.state.pc; delete game.state.campaignMode; game.normalize();
  assert.equal(game.state.version, 9); assert.equal(game.state.pc.started, false); assert.equal(game.state.campaignMode, 'servers');
  assert.equal(JSON.stringify(game.state.servers), servers); assert.equal(JSON.stringify(game.state.lab), lab);
});

test('Long speed: 90 минут/день, 12 дней/год, 1990→2026 = 27 суток', () => {
  const game = pcGame();
  assert.equal(game.pcTimeInfo().secondsForGameDay, 5400); assert.equal(game.pcTimeInfo().remainingRealDays, 27);
  for (let i = 0; i < 12; i += 1) game.advancePCMinutes(1440);
  assert.equal(game.state.pc.year, 1991); assert.equal(game.state.pc.yearDay, 0);
  game.setPCSpeed(15); assert.equal(game.pcTimeInfo().secondsForGameDay, 360);
  game.setPCSpeed(60); assert.equal(game.pcTimeInfo().secondsForGameDay, 90);
  assert.equal(game.setPCSpeed(999), false);
});

test('PC tick не тратит server time/деньги; server tick не двигает PC-кампанию', () => {
  const game = pcGame(); const serverMinute = game.state.minute; const pcMinute = game.state.pc.minute;
  for (let i = 0; i < 45; i += 1) game.tick();
  assert.equal(game.state.minute, serverMinute); assert.equal(game.state.pc.minute, pcMinute + 12);
  game.switchCampaign('servers'); const frozenPCMinute = game.state.pc.minute; game.tick();
  assert.equal(game.state.minute, serverMinute + 12); assert.equal(game.state.pc.minute, frozenPCMinute);
});

test('Заказ: accept → full diagnostics → repair → Stability PASS → return с оплатой', () => {
  const game = pcGame(); const offer = game.state.pc.offers[0];
  offer.pc.issues = [{ id: 'virus', revealed: false, resolved: false }];
  assert.equal(game.acceptPCJob(offer.id), true); const pc = game.getPC(offer.pc.id);
  assert.equal(game.completePCJob(pc.id), false); game.runPCDiagnostic(pc.id, 'full'); assert.equal(pc.issues[0].revealed, true);
  assert.equal(game.repairPCIssue(pc.id, 'virus'), true); assert.equal(pc.issues[0].resolved, true);
  game.togglePCPower(pc.id); const result = game.runPCBenchmark(pc.id, 'stability'); assert.equal(result.passed, true);
  assert.equal(game.completePCJob(pc.id), false); game.togglePCPower(pc.id);
  const money = game.state.pc.money; const completed = game.state.pc.completed;
  const returned = game.completePCJob(pc.id); assert.equal(returned.ok, true); assert.ok(game.state.pc.money > money);
  assert.equal(game.state.pc.completed, completed + 1); assert.equal(game.state.pc.reputation, 1); assert.equal(game.getPC(pc.id), undefined);
});

test('Пыль: ровно 6 ручных зон, cooling +15; на включённом ПК чистка запрещена', () => {
  const game = pcGame(); const pc = addPC(game, 2005, ['dust']); const cooling = pc.cooling;
  game.runPCDiagnostic(pc.id, 'thermal'); game.togglePCPower(pc.id); assert.equal(game.cleanPCDust(pc.id, 0), false); game.togglePCPower(pc.id);
  for (let i = 0; i < 5; i += 1) assert.equal(game.cleanPCDust(pc.id, i), i + 1);
  assert.equal(pc.issues[0].resolved, false); assert.equal(game.cleanPCDust(pc.id, 5), 6);
  assert.equal(pc.issues[0].resolved, true); assert.equal(pc.cooling, cooling + 15); assert.equal(pc.cleanedSpots.length, 6);
});

test('RAM repair выбирает только DDR2, не SDRAM и не SIMM', () => {
  const game = pcGame(); const pc = addPC(game, 2005, ['ram']);
  game.state.pc.parts.push(game.makePCPart('ram', 1998), game.makePCPart('ram', 2005));
  game.runPCDiagnostic(pc.id, 'memory'); const spare = game.pcSpareForIssue(pc, 'ram');
  assert.equal(spare.ramType, 'DDR2'); const ram = pc.ramMb;
  assert.equal(game.repairPCIssue(pc.id, 'ram'), true); assert.equal(pc.ramType, 'DDR2'); assert.ok(pc.ramMb >= ram);
  assert.equal(game.state.pc.parts.some(part => part.uid === spare.uid), false);
});

test('Windows 11 на ПК 2005: Experimental Bypass работает, обычная установка запрещена', () => {
  const game = pcGame(); const pc = addPC(game, 2005);
  assert.equal(game.installPCOS(pc.id, 'pc-win11', false), false);
  assert.equal(game.installPCOS(pc.id, 'pc-win11', true), true);
  assert.equal(pc.os, 'pc-win11'); assert.equal(pc.experimentalOS, true); assert.ok(pc.osPenalty < .2);
  assert.equal(game.state.pc.stats.experimentalOS, 1);
});

test('Experimental Bypass нельзя использовать на чужом заказном ПК', () => {
  const game = pcGame(); const pc = addPC(game, 2005, [], 'job');
  assert.equal(game.installPCOS(pc.id, 'pc-win11', true), false); assert.notEqual(pc.os, 'pc-win11');
});

test('Установка ОС и аппаратный ремонт требуют выключенного ПК', () => {
  const game = pcGame(); const pc = addPC(game, 2005, ['thermalPaste']); game.runPCDiagnostic(pc.id, 'thermal'); game.togglePCPower(pc.id);
  assert.equal(game.installPCOS(pc.id, 'pc-winxp', true), false); assert.equal(game.repairPCIssue(pc.id, 'thermalPaste'), false);
  game.togglePCPower(pc.id); assert.equal(game.repairPCIssue(pc.id, 'thermalPaste'), true);
});

test('Benchmark: ретро 1990 не тянет Cyberpunk, современный ПК получает высокий FPS', () => {
  const game = pcGame(); const retro = addPC(game, 1990); const modern = addPC(game, 2026);
  game.togglePCPower(retro.id); game.togglePCPower(modern.id);
  const low = game.runPCBenchmark(retro.id, 'cyberpunk'); const high = game.runPCBenchmark(modern.id, 'cyberpunk');
  assert.equal(low.passed, false); assert.ok(low.fps < 1); assert.equal(high.passed, true); assert.ok(high.fps >= 48);
  assert.equal(low.samples.length, 16); assert.equal(high.samples.length, 16);
});

test('Thermal crash на 95°C выключает ПК; после остывания доступен reboot', () => {
  const game = pcGame(); const pc = addPC(game, 2026); game.togglePCPower(pc.id); pc.temp = 94;
  const result = game.runPCBenchmark(pc.id, 'cyberpunk'); assert.equal(pc.crashed, true); assert.equal(pc.powered, false); assert.equal(result.passed, false);
  for (let i = 0; i < 50; i += 1) game.tickPC(0);
  assert.ok(pc.temp < 85); assert.equal(game.rebootPC(pc.id), true); assert.equal(pc.crashed, false); assert.equal(pc.powered, true);
});

test('Мусорка даёт ретро-ПК или деталь, но только один раз за игровой день', () => {
  const game = pcGame(); const before = game.state.pc.computers.length + game.state.pc.parts.length;
  const find = game.searchDumpster(); assert.ok(['pc','part'].includes(find.type));
  assert.equal(game.state.pc.computers.length + game.state.pc.parts.length, before + 1);
  assert.equal(game.searchDumpster(), false); game.advancePCMinutes(1440); assert.notEqual(game.searchDumpster(), false);
});

test('Workbench/ads/tools upgrades тратят PC-бюджет и меняют прогрессию', () => {
  const game = pcGame(); const money = game.state.pc.money;
  assert.equal(game.buyPCUpgrade('ads'), true); assert.equal(game.state.pc.upgrades.ads, 1); assert.equal(game.state.pc.money, money - 1500);
  assert.equal(game.generatePCOffers().length, 4); assert.equal(game.buyPCUpgrade('unknown'), false);
});

test('PC command shell управляет настоящим состоянием: status/scan/bench/tests', () => {
  const game = pcGame(); const pc = game.state.pc.computers[0]; game.togglePCPower(pc.id);
  assert.equal(game.executePCCommand(pc.id, 'status').ok, true); assert.equal(game.executePCCommand(pc.id, 'scan').ok, true);
  assert.equal(game.executePCCommand(pc.id, 'tests').lines.length, 11);
  const result = game.executePCCommand(pc.id, 'bench stability'); assert.equal(result.ok, true); assert.ok(pc.benchmarks.stability);
  assert.equal(game.executePCCommand(pc.id, 'not-a-command').ok, false);
});

test('Avito instant search: SDRAM/DDR2/DDR4 возвращают только RAM без refresh', () => {
  const game = fresh(); const originalRefresh = game.refreshListings; let refreshes = 0; game.refreshListings = function () { refreshes += 1; return originalRefresh.call(this); };
  for (const query of ['SDRAM','DDR2','DDR4']) {
    const results = game.findListings(query); assert.equal(results.length, 8); assert.ok(results.every(listing => !listing.isServer && DATA.part(listing.partId).type === 'ram'));
    assert.ok(results.every(listing => DATA.part(listing.partId).ramType.toLowerCase().includes(query.toLowerCase())));
  }
  assert.equal(refreshes, 0); assert.ok(game.state.listings.length <= 60);
});

test('Найденную DDR2 можно сразу купить: деталь появляется в server inventory', () => {
  const game = fresh(); const result = game.findListings('DDR2')[0]; const before = game.state.inventory.length;
  const purchase = game.buyListing(result.id); assert.equal(purchase.ok, true); assert.equal(game.state.inventory.length, before + 1);
  assert.equal(purchase.item.type, 'ram'); assert.equal(purchase.item.ramType, 'DDR2'); assert.ok(game.state.inventory.some(part => part.uid === purchase.item.uid));
});

test('Avito search: неизвестный запрос не создаёт фальшивых совпадений', () => {
  const game = fresh(); const results = game.findListings('неттакогожелеза12345'); assert.deepEqual(results, []);
});

test('Lo-fi radio: 20 × 1800 секунд = ровно 10 часов, playlist зациклен', () => {
  const audio = { context: { currentTime: 0 }, settings() { return { volume: .32 }; } };
  const radio = new LongLofiRadio(audio); assert.equal(radio.tracks.length, 20); assert.equal(radio.totalDuration, 36000);
  assert.ok(radio.tracks.every(track => track.duration === 1800)); radio.selectTrack(19); assert.equal(radio.next().id, 'lofi-01');
  radio.makeBus = () => { radio.bus = {}; }; radio.schedule = () => {}; radio.index = 0; radio.position = 0; radio.startedAt = 0; radio.playing = true; audio.context.currentTime = 1801; radio.tick(); assert.equal(radio.index, 1);
  radio.index = 0; radio.startedAt = 0; audio.context.currentTime = 36001; radio.tick(); assert.equal(radio.index, 0); assert.ok(Math.abs(radio.status().position - 1) < .001);
});

test('PC Soundscape отделён от Server Soundscape и не создаёт звук без пользовательского unlock', () => {
  const game = fresh(); const audio = new TycoonAudio(game); const sound = new PCSoundscape(audio);
  assert.equal(sound.enabled(), false); assert.equal(sound.sync(game.state.pc.computers[0], {}), false); assert.equal(sound.nodes, null);
  assert.equal(game.state.settings.pcSounds, undefined); audio.settings(); assert.equal(audio.settings().musicMode, 'lofi');
  const legacy = fresh(); legacy.state.flags.onboarded = true; const legacyAudio = new TycoonAudio(legacy);
  assert.equal(legacyAudio.settings().musicMode, 'tracks', 'сохранение 6.0 не теряет выбранный классический режим музыки');
});

console.log(`\nUltimate 7.0: ${passed} scenarios passed`);
