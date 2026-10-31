/* Time Machine 5.0 + Debug Console: node tests/timeline.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','game','systems','lab','timeline','debug']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh() { const game = new Game(false); game.events.length = 0; return game; }
function boot(game, osId, server) {
  server = server || game.state.servers[0];
  if (!game.state.servers.includes(server)) game.state.servers.push(server);
  const usb = game.state.inventory.find(part => part.type === 'usb');
  assert.equal(game.writeImage(usb.uid, osId), true, osId);
  assert.equal(game.finishOSInstall(server.id, usb.uid), true, osId);
  server.powered = true;
  game.updateStatus(server);
  return server;
}

test('каждый год 1990–2026: рабочий стартер, ОС и 5–10 эпохальных лотов', () => {
  const game = fresh();
  for (let year = 1990; year <= 2026; year += 1) {
    assert.equal(game.startTimeline(year), true, String(year));
    const server = game.state.servers[0];
    if (year === 2026) assert.equal(server.templateId, 'office6000');
    else assert.equal(server.chassis.year, year, String(year));
    assert.equal(game.hardwareReport(server).ok, true, `${year}: ${game.hardwareReport(server).issues}`);
    assert.ok(game.availableOS().length > 0, `нет ОС ${year}`);
    assert.ok(game.availableTemplates().length >= 68, `нет серверов ${year}`);
    assert.ok(game.state.listings.length >= 5 && game.state.listings.length <= 10);
    for (const lot of game.state.listings) {
      const itemYear = lot.isServer ? DATA.template(lot.templateId).year : DATA.part(lot.partId).year;
      assert.ok(itemYear <= year, `${year}: будущий лот ${itemYear}`);
    }
  }
});

test('старт 1995: железо и дополнительная RAM совместимы, будущее скрыто', () => {
  const game = fresh();
  game.startTimeline(1995);
  const server = game.state.servers[0];
  assert.equal(game.currentYear(), 1995);
  assert.equal(game.state.startYear, 1995);
  assert.equal(server.templateId.startsWith('era_1995_'), true);
  const spare = game.state.inventory.find(part => part.type === 'ram');
  assert.equal(spare.ramType, server.chassis.ramType);
  assert.equal(game.availableTemplates().every(template => template.year <= 1995), true);
  assert.equal(game.availableOS().every(os => os.year <= 1995), true);
  assert.equal(game.buyCatalogServer('era_1996_01'), false);
  const usb = game.state.inventory.find(part => part.type === 'usb');
  assert.equal(game.writeImage(usb.uid, 'debian11'), false, 'Debian 1996 недоступен в 1995');
  assert.equal(usb.image, null);
});

test('1995 + Slackware: доступны 18+ legacy-сервисов вместо требования Debian 12/13', () => {
  const game = fresh(); game.startTimeline(1995);
  const compatible = DATA.SOFTWARE.filter(app => app.os.includes('slackware3'));
  assert.ok(compatible.length >= 18);
  const server = boot(game, 'slackware3');
  assert.equal(game.installSoftware(server.id, 'legacy_service_0'), true);
  assert.ok(game.hourlyServerRevenue(server) > 0);
});

test('регрессия: отдельный сервер модели 1995 + Debian 1.1 принимает legacy-сервис', () => {
  const game = fresh();
  const server = game.instantiateServer('era_1995_08');
  boot(game, 'debian11', server);
  assert.ok(DATA.SOFTWARE.filter(app => app.os.includes('debian11')).length >= 18);
  assert.equal(game.installSoftware(server.id, 'legacy_service_0'), true);
  assert.equal(game.installSoftware(server.id, 'svc_web_0_standard'), false);
  assert.ok(game.hourlyServerRevenue(server) > 0);
});

test('1990: UNIX System V загружается и имеет работающие сервисы', () => {
  const game = fresh(); game.startTimeline(1990);
  assert.ok(game.availableOS().some(os => os.id === 'unixv4'));
  const server = boot(game, 'unixv4');
  assert.ok(DATA.SOFTWARE.filter(app => app.os.includes('unixv4')).length >= 18);
  assert.equal(game.installSoftware(server.id, 'legacy_service_0'), true);
  assert.ok(game.hourlyServerRevenue(server) > 0);
});

test('12 игровых дней открывают 1996, Debian 1.1 и новые лоты, начисляют RP/XP', () => {
  const game = fresh(); game.startTimeline(1995); game.state.flags.onboarded = true;
  const xp = game.state.xp;
  for (let i = 0; i < 12; i += 1) game.endDay();
  assert.equal(game.currentYear(), 1996);
  assert.equal(game.state.research.points, 2);
  assert.equal(game.state.research.yearsCompleted, 1);
  assert.ok(game.state.xp >= xp + 200);
  assert.ok(game.availableOS().some(os => os.id === 'debian11'));
  assert.ok(game.state.listings.every(lot => (lot.isServer ? DATA.template(lot.templateId).year : DATA.part(lot.partId).year) <= 1996));
});

test('2026 является мягким cap: год не выходит за архив, но Modern R&D продолжается', () => {
  const game = fresh();
  const points = game.state.research.points;
  for (let i = 0; i < 12; i += 1) game.endDay();
  assert.equal(game.currentYear(), 2026);
  assert.equal(game.state.research.points, points + 1);
  assert.equal(game.state.research.yearsCompleted, 1);
  assert.equal(game.availableTemplates().length, 2597);
});

test('Research Cooling реально снижает target temperature и повышает cooling', () => {
  const game = fresh(); const server = boot(game, 'debian');
  const before = game.metrics(server);
  assert.equal(game.buyResearch('cooling'), true);
  const after = game.metrics(server);
  assert.ok(after.targetTemp < before.targetTemp);
  assert.ok(after.cooling > before.cooling);
  assert.equal(game.state.research.points, 0);
});

test('Research Efficiency и Marketing меняют реальные ватты и доход', () => {
  const game = fresh(); const server = boot(game, 'debian');
  game.installSoftware(server.id, 'nginx');
  const before = game.metrics(server).watts;
  const revenue = game.hourlyServerRevenue(server);
  game.state.research.points = 2;
  assert.equal(game.buyResearch('efficiency'), true);
  assert.ok(game.metrics(server).watts < before);
  game.state.research.points = 2;
  assert.equal(game.buyResearch('marketing'), true);
  assert.ok(game.hourlyServerRevenue(server) > revenue * 1.039);
});

test('стоимость исследований растёт, максимум 5 и недостаток RP блокирует покупку', () => {
  const game = fresh(); game.state.research.points = 0;
  assert.equal(game.buyResearch('cooling'), false);
  game.state.research.points = 15;
  for (let i = 0; i < 5; i += 1) assert.equal(game.buyResearch('cooling'), true);
  assert.equal(game.state.research.cooling, 5);
  assert.equal(game.state.research.points, 0);
  assert.equal(game.buyResearch('cooling'), false);
});

test('секретная команда debug включает verbose tick/server/save телеметрию', () => {
  const game = fresh(); const server = boot(game, 'debian');
  assert.equal(game.debug.enabled, false);
  const unlock = game.executeCommand(server.id, 'debug');
  assert.equal(unlock.debug, true);
  assert.equal(game.debug.enabled, true);
  game.debug.clear(); game.saveTimer = 9; game.tick();
  const categories = new Set(game.debug.entries.map(entry => entry.category));
  assert.ok(categories.has('tick'));
  assert.ok(categories.has('server'));
  assert.ok(categories.has('save'));
  const metricEntry = game.debug.entries.find(entry => entry.category === 'server');
  assert.ok(Number.isFinite(metricEntry.data.after.temp));
  assert.ok(Number.isFinite(metricEntry.data.metrics.watts));
});

test('Debug Console пишет действия, команды, события и полный snapshot', () => {
  const game = fresh(); const server = boot(game, 'debian'); game.debug.enabled = true;
  game.executeCommand(server.id, 'install nginx');
  const snapshot = game.openDebugSnapshot();
  assert.ok(snapshot.stateJson.length > 100);
  assert.equal(snapshot.servers.length, 1);
  assert.ok(snapshot.servers[0].metrics);
  assert.ok(snapshot.runtime);
  assert.equal(snapshot.catalog.services, 2046);
  const text = game.debug.exportText();
  assert.ok(text.includes('COMMAND_INPUT'));
  assert.ok(text.includes('INSTALLSOFTWARE_BEGIN'));
  assert.ok(text.includes('FULL_STATE_SNAPSHOT'));
});

test('debug pause/clear ограничивают буфер и останавливают запись', () => {
  const game = fresh(); game.debug.enabled = true;
  game.debug.paused = true;
  const sequence = game.debug.sequence;
  game.notify('hidden'); game.tick();
  assert.equal(game.debug.sequence, sequence);
  game.debug.clear(); assert.equal(game.debug.entries.length, 0);
  game.debug.paused = false; game.notify('visible');
  assert.ok(game.debug.entries.some(entry => entry.message === 'visible'));
  for (let i = 0; i < 1250; i += 1) game.debug.log('trace', 'test', `entry ${i}`);
  assert.equal(game.debug.entries.length, 1200);
});

test('старый save без Time Machine остаётся в 2026 со всем контентом', () => {
  const game = fresh();
  delete game.state.startYear; delete game.state.currentYear; delete game.state.research;
  game.normalize();
  assert.equal(game.state.version, 7);
  assert.equal(game.currentYear(), 2026);
  assert.equal(game.availableTemplates().length, 2597);
  assert.equal(game.availableOS().length, DATA.OS_LIST.length);
  assert.equal(game.state.research.points, 1);
});

test('ранние ОС имеют уникальные ID, а NetWare 2.1 — файловые службы', () => {
  assert.equal(new Set(DATA.OS_LIST.map(os => os.id)).size, DATA.OS_LIST.length);
  assert.ok(DATA.OS_LIST.length >= 61);
  assert.ok(DATA.SOFTWARE.filter(app => app.os.includes('netware21')).length >= 3);
  const game = fresh(); game.startTimeline(1990);
  const server = boot(game, 'netware21');
  assert.equal(game.installSoftware(server.id, 'legacy_service_15'), true);
});

console.log(`\n${passed} сценариев Time Machine 5.0 и Debug Console пройдено.`);
