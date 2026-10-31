/* Deep Lab 6.0: node tests/deep-lab.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','game','systems','lab','timeline','deep-lab','debug']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh() { const game = new Game(false); game.events.length = 0; return game; }
function boot(game, os = 'debian', server) {
  server = server || game.state.servers[0];
  if (!game.state.servers.includes(server)) game.state.servers.push(server);
  const usb = game.state.inventory.find(part => part.type === 'usb');
  assert.equal(game.writeImage(usb.uid, os), true);
  assert.equal(game.finishOSInstall(server.id, usb.uid), true);
  server.powered = true; game.updateStatus(server);
  return server;
}
function runProtocol(game, server, id) {
  assert.equal(game.startLabExperiment(server.id, id), true, id);
  let guard = 0;
  while (server.labExperiment && guard < 35) { game.tick(); guard += 1; }
  assert.equal(server.labExperiment, null, `${id} завершился`);
  const report = game.state.lab.reports[0];
  assert.equal(report.protocolId, id);
  return report;
}

test('Deep Lab: 8 протоколов, 14 достижений, version 8 и безопасные defaults', () => {
  const game = fresh(); const server = game.state.servers[0];
  assert.equal(Object.keys(DATA.LAB_PROTOCOLS).length, 8);
  assert.equal(DATA.LAB_ACHIEVEMENTS.length, 14);
  assert.equal(game.state.version, 8);
  assert.equal(server.overclock, 0); assert.equal(server.fanProfile, 'balanced');
  assert.deepEqual(game.state.lab.reports, []);
  assert.equal(game.state.lab.completed, 0);
});

test('Thermal Soak: настоящая CPU-нагрузка, samples, +1 RP и cooling discovery', () => {
  const game = fresh(); const server = boot(game);
  const idle = game.metrics(server); const money = game.state.money; const rp = game.state.research.points;
  assert.equal(game.startLabExperiment(server.id, 'thermal'), true);
  assert.ok(game.metrics(server).cpuDemand > idle.cpuDemand + 60);
  assert.equal(game.hourlyServerRevenue(server), 0);
  for (let i = 0; i < 8; i += 1) game.tick();
  const report = game.state.lab.reports[0];
  assert.equal(report.status, 'success'); assert.equal(report.samples.length, 8);
  assert.ok(report.peaks.temp > server.temp - 2); assert.ok(Number.isFinite(report.score));
  assert.equal(game.state.research.points, rp + 1); assert.equal(game.state.money, money + 1500);
  assert.equal(server.coolingBonus, 2); assert.equal(server.labBadges.thermal, true);
  assert.equal(game.state.achievements.first_lab, 1);
});

test('Memory Mapping достигает 96% без OOM и открывает −2% расхода RAM', () => {
  const game = fresh(); const server = boot(game);
  const before = game.metrics(server).ramUsed;
  const report = runProtocol(game, server, 'memory');
  assert.equal(report.status, 'success'); assert.ok(report.peaks.ram >= 95 && report.peaks.ram < 100);
  assert.equal(server.hung, false); assert.equal(server.memoryTuning, .02);
  assert.ok(game.metrics(server).ramUsed < before);
});

test('Packet Storm считает throughput/loss, а авария провайдера — настоящий провал', () => {
  const game = fresh(); const server = boot(game);
  const report = runProtocol(game, server, 'network');
  assert.equal(report.status, 'success'); assert.ok(report.averages.throughput > 0);
  assert.equal(report.averages.packetLoss, 0); assert.equal(server.networkTuning, .03);
  game.state.outageDay = game.state.day;
  const failed = runProtocol(game, server, 'network');
  assert.equal(failed.status, 'failed'); assert.match(failed.reason, /провайдера/);
});

test('Disk Endurance измеряет IOPS, изнашивает диск на 1% и улучшает storage factor', () => {
  const game = fresh(); const server = boot(game); const disk = server.parts.disks[0];
  const wear = disk.wear; const storage = game.metrics(server).storageFactor;
  const report = runProtocol(game, server, 'storage');
  assert.equal(report.status, 'success'); assert.ok(report.averages.iops >= 145);
  assert.equal(disk.wear, wear - 1); assert.equal(server.storageTuning, .03);
  assert.ok(game.metrics(server).storageFactor > storage);
});

test('RAID Failure Drill: 500→250 ГБ при отказе, 500 ГБ после rebuild, ОС сохранена', () => {
  const game = fresh(); const server = boot(game); const os = server.os;
  server.parts.disks.push(game.makePart(server.parts.disks[0].catalogId)); server.raid = 'raid1';
  assert.equal(game.raidCapacity(server), 500);
  assert.equal(game.startLabExperiment(server.id, 'raid'), true);
  game.tick(); assert.equal(game.raidCapacity(server), 250);
  assert.equal(server.parts.disks.filter(disk => disk.labFault).length, 1);
  while (server.labExperiment) game.tick();
  const report = game.state.lab.reports[0];
  assert.equal(report.status, 'success'); assert.equal(report.raidAfter, 250); assert.equal(report.raidRestored, 500);
  assert.equal(game.raidCapacity(server), 500); assert.equal(server.os, os);
  assert.equal(server.parts.disks.some(disk => disk.labFault), false);
});

test('RAID 0 и одиночный диск не проходят допуск Failure Drill', () => {
  const game = fresh(); const server = boot(game);
  assert.equal(game.canStartLab(server.id, 'raid').ok, false);
  server.parts.disks.push(game.makePart(server.parts.disks[0].catalogId)); server.raid = 'raid0';
  assert.equal(game.startLabExperiment(server.id, 'raid'), false);
  assert.equal(server.labExperiment, null);
});

test('PSU Transient повышает ватты и открывает постоянную экономию', () => {
  const game = fresh(); const server = boot(game); const base = game.metrics(server).watts;
  game.startLabExperiment(server.id, 'power');
  assert.ok(game.metrics(server).watts > base + 40);
  while (server.labExperiment) game.tick();
  assert.equal(game.state.lab.reports[0].status, 'success');
  assert.equal(server.powerTuning, .02); assert.ok(game.metrics(server).watts < base);
});

test('PSU Transient может реально выбить автомат и получает failed/power loss', () => {
  const game = fresh(); const server = boot(game);
  const originalRoom = game.room;
  game.room = function () { return Object.assign(originalRoom.call(this), { powerLimit: 100 }); };
  const report = runProtocol(game, server, 'power');
  assert.equal(game.state.powerOut, true); assert.equal(server.powered, false);
  assert.equal(report.status, 'failed'); assert.match(report.reason, /питания/);
});

test('Overclock tuning меняет CPU demand, тепло, ватты и доход только вне включённого сервера', () => {
  const game = fresh(); const server = boot(game); game.installSoftware(server.id, 'nginx');
  const stock = game.metrics(server); const stockRevenue = game.hourlyServerRevenue(server);
  assert.equal(game.setOverclock(server.id, 20), false);
  game.powerOff(server); assert.equal(game.setOverclock(server.id, 20), true);
  assert.equal(game.setOverclock(server.id, 30), false);
  assert.equal(game.togglePower(server.id), true);
  const tuned = game.metrics(server);
  assert.ok(tuned.cpuDemand < stock.cpuDemand); assert.ok(tuned.watts > stock.watts); assert.ok(tuned.targetTemp > stock.targetTemp);
  assert.ok(game.hourlyServerRevenue(server) > stockRevenue * 1.19);
});

test('Overclock Stability временно разгоняет, восстанавливает preset и открывает +30%', () => {
  const game = fresh(); const server = boot(game); game.powerOff(server); game.setOverclock(server.id, 10); game.togglePower(server.id);
  const report = runProtocol(game, server, 'overclock');
  assert.equal(report.status, 'success'); assert.equal(server.overclock, 10); assert.equal(server.overclockUnlocked, true);
  game.powerOff(server); assert.equal(game.setOverclock(server.id, 30), true); assert.equal(server.overclock, 30);
});

test('Silent/Balanced/Turbo реально меняют cooling, температуру, шум и ватты', () => {
  const game = fresh(); const server = boot(game); const balanced = game.metrics(server);
  game.setFanProfile(server.id, 'silent'); const silent = game.metrics(server);
  assert.ok(silent.cooling < balanced.cooling); assert.ok(silent.targetTemp > balanced.targetTemp); assert.equal(silent.noise, balanced.noise - 5);
  game.setFanProfile(server.id, 'turbo'); const turbo = game.metrics(server);
  assert.ok(turbo.cooling > balanced.cooling); assert.ok(turbo.targetTemp < balanced.targetTemp); assert.equal(turbo.noise, balanced.noise + 7); assert.ok(turbo.watts > balanced.watts);
});

test('AI Inference Bench требует модель, считает tokens/s и повышает AI-доход', () => {
  const game = fresh(); const server = boot(game);
  assert.equal(game.startLabExperiment(server.id, 'ai'), false);
  game.installAIModel(server.id, 'tiny05'); const revenue = game.hourlyServerRevenue(server);
  const report = runProtocol(game, server, 'ai');
  assert.equal(report.status, 'success'); assert.ok(report.averages.tokens > 0);
  assert.equal(server.aiTuning, .03); assert.ok(game.hourlyServerRevenue(server) > revenue * 1.029);
});

test('Deep Lab и Torture Lab взаимоисключаются; stop сохраняет aborted report', () => {
  const game = fresh(); const server = boot(game);
  game.startLabExperiment(server.id, 'thermal'); assert.equal(game.startStress(server.id, 'cpu'), false);
  assert.equal(game.stopLabExperiment(server.id), true);
  assert.equal(game.state.lab.reports[0].status, 'aborted'); assert.equal(server.labExperiment, null);
  assert.equal(game.startStress(server.id, 'cpu'), true); assert.equal(game.startLabExperiment(server.id, 'memory'), false);
  game.stopStress(server.id);
});

test('BSOD во время протокола — failed, а не фальшивый успех', () => {
  const game = fresh(); const server = boot(game);
  game.startLabExperiment(server.id, 'thermal'); server.temp = 110; game.tick();
  assert.equal(server.crashed, true); assert.equal(server.labExperiment, null);
  assert.equal(game.state.lab.reports[0].status, 'failed'); assert.match(game.state.lab.reports[0].reason, /BSOD/);
  assert.equal(game.state.lab.failed, 1);
});

test('Переполнение RAM во время протокола приводит к настоящему OOM/failed', () => {
  const game = fresh(); const server = boot(game);
  server.software.push('virtuallab'); game.startLabExperiment(server.id, 'memory');
  let guard = 0; while (server.labExperiment && guard < 10) { game.tick(); guard += 1; }
  assert.equal(server.hung, true); assert.equal(game.state.lab.reports[0].status, 'failed');
  assert.match(game.state.lab.reports[0].reason, /OOM/);
});

test('Повторы не фармят RP/бонус; журнал ограничен 12 отчётами', () => {
  const game = fresh(); const server = boot(game);
  for (let i = 0; i < 13; i += 1) runProtocol(game, server, 'thermal');
  assert.equal(game.state.lab.reports.length, 12); assert.equal(game.state.lab.completed, 13);
  assert.equal(game.state.research.points, 2); assert.equal(server.coolingBonus, 2);
  assert.equal(Object.keys(game.state.lab.discoveries).length, 1);
});

test('Save/load посреди RAID Drill сохраняет samples, отказ и продолжение', () => {
  const store = new Map();
  global.localStorage = { getItem: key => store.get(key) || null, setItem: (key, value) => store.set(key, value), removeItem: key => store.delete(key) };
  try {
    const game = fresh(); const server = boot(game);
    server.parts.disks.push(game.makePart(server.parts.disks[0].catalogId)); server.raid = 'raid1';
    game.startLabExperiment(server.id, 'raid'); game.tick(); game.tick(); game.save();
    const restored = new Game(true); const restoredServer = restored.state.servers[0];
    assert.equal(restored.state.version, 8); assert.equal(restoredServer.labExperiment.elapsed, 2);
    assert.equal(restoredServer.labExperiment.samples.length, 2); assert.equal(restored.raidCapacity(restoredServer), 250);
    while (restoredServer.labExperiment) restored.tick();
    assert.equal(restored.state.lab.reports[0].status, 'success'); assert.equal(restored.raidCapacity(restoredServer), 500);
  } finally { delete global.localStorage; }
});

test('Достижения открываются один раз и начисляют XP за реальный прогресс', () => {
  const game = fresh(); const server = boot(game); runProtocol(game, server, 'thermal');
  assert.ok(game.state.achievements.first_lab);
  game.state.lab.discoveries = { thermal: true, memory: true, network: true, storage: true };
  const xp = game.state.xp; const unlocked = game.checkAchievements();
  assert.ok(unlocked.includes('four_discoveries')); assert.ok(game.state.xp >= xp + 60);
  assert.deepEqual(game.checkAchievements(), []);
});

test('Fleet powerAll останавливает/запускает машины и сохраняет aborted отчёт', () => {
  const game = fresh(); const server = boot(game); game.startLabExperiment(server.id, 'thermal');
  const off = game.powerAll(false); assert.equal(off.changed, 1); assert.equal(server.powered, false);
  assert.equal(game.state.lab.reports[0].status, 'aborted');
  const on = game.powerAll(true); assert.equal(on.changed, 1); assert.equal(server.powered, true);
});

test('Регламентное обслуживание: toolkit discount, +4 cooling, +2 health, статистика', () => {
  const game = fresh(); const server = boot(game); server.health = 90;
  assert.equal(game.serviceServer(server.id), false);
  game.powerOff(server); const money = game.state.money; const cooling = server.coolingBonus;
  assert.equal(game.serviceServer(server.id), true); assert.equal(game.state.money, money - 350);
  assert.equal(server.coolingBonus, cooling + 4); assert.equal(server.health, 92); assert.equal(game.state.stats.services, 1);
});

test('Терминал: lab start/stop/report и tune oc/fan управляют реальным состоянием', () => {
  const game = fresh(); const server = boot(game);
  assert.ok(game.executeCommand(server.id, 'help').lines.join(' ').includes('lab [start'));
  assert.equal(game.executeCommand(server.id, 'lab start network').ok, true); assert.equal(server.labExperiment.id, 'network');
  assert.equal(game.executeCommand(server.id, 'lab stop').ok, true); assert.equal(server.labExperiment, null);
  assert.equal(game.executeCommand(server.id, 'lab report').ok, true);
  game.powerOff(server); assert.equal(game.executeCommand(server.id, 'tune oc 20').ok, true); assert.equal(server.overclock, 20);
  assert.equal(game.executeCommand(server.id, 'tune fan turbo').ok, true); assert.equal(server.fanProfile, 'turbo');
});

test('Time Machine ограничивает Overclock до 1995 и AI Bench до 2016', () => {
  const game = fresh(); game.startTimeline(1990); const server = boot(game, 'unixv4');
  assert.equal(game.canStartLab(server.id, 'thermal').ok, true);
  assert.equal(game.canStartLab(server.id, 'overclock').ok, false);
  assert.equal(game.canStartLab(server.id, 'ai').ok, false);
  game.state.currentYear = 1995; assert.equal(game.canStartLab(server.id, 'overclock').ok, true);
});

test('Старый save получает Deep Lab defaults без потери настроек и контента', () => {
  const game = fresh(); delete game.state.lab; delete game.state.achievements;
  const server = game.state.servers[0]; delete server.overclock; delete server.fanProfile; delete server.labBadges;
  game.state.settings.serverVolume = .61; game.normalize();
  assert.equal(game.state.version, 8); assert.equal(server.overclock, 0); assert.equal(server.fanProfile, 'balanced');
  assert.deepEqual(server.labBadges, {}); assert.equal(game.state.settings.serverVolume, .61);
  assert.equal(game.availableTemplates().length, 2597);
});

test('Debug Console видит Deep Lab actions, live protocol, lab state и achievements', () => {
  const game = fresh(); const server = boot(game); game.debug.enabled = true;
  game.startLabExperiment(server.id, 'network'); game.tick();
  const snapshot = game.openDebugSnapshot();
  assert.ok(snapshot.lab); assert.ok(snapshot.achievements); assert.equal(snapshot.servers[0].labExperiment.id, 'network');
  assert.ok(snapshot.servers[0].metrics.networkLoad > 0);
  assert.ok(game.debug.exportText().includes('STARTLABEXPERIMENT_BEGIN'));
});

test('Сквозной прогон: все 8 открытий и достижение Полная лаборатория', () => {
  const game = fresh(); const server = boot(game);
  server.parts.disks.push(game.makePart(server.parts.disks[0].catalogId)); server.raid = 'raid1';
  game.installAIModel(server.id, 'tiny05');
  for (const id of Object.keys(DATA.LAB_PROTOCOLS)) {
    const report = runProtocol(game, server, id); assert.equal(report.status, 'success', `${id}: ${report.reason}`);
  }
  assert.equal(Object.keys(game.state.lab.discoveries).length, 8);
  assert.ok(game.state.achievements.all_protocols); assert.equal(game.state.lab.completed, 8);
  assert.equal(game.state.research.points, 9);
});

console.log(`\n${passed} сценариев Deep Lab 6.0 пройдено.`);
