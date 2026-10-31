/* Expansion 2.0: node tests/expansion.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons', 'brands', 'data', 'expansion', 'game', 'systems', 'lab', 'timeline', 'deep-lab', 'debug']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) {
  fn();
  passed += 1;
  console.log(`✓ ${name}`);
}
function fresh() {
  const game = new Game(false);
  game.events.length = 0;
  return game;
}
function bootDebian(game, apps = []) {
  const server = game.state.servers[0];
  const usb = game.state.inventory.find(part => part.type === 'usb');
  game.writeImage(usb.uid, 'debian');
  assert.equal(game.finishOSInstall(server.id, usb.uid), true);
  for (const app of apps) assert.equal(game.installSoftware(server.id, app), true, app);
  return server;
}

test('каталог: 80+ серверов, 190+ деталей, 20+ ОС и 35+ сервисов', () => {
  assert.ok(DATA.SERVER_TEMPLATES.length >= 80);
  assert.ok(DATA.PARTS.length >= 190);
  assert.ok(DATA.OS_LIST.length >= 20);
  assert.ok(DATA.SOFTWARE.length >= 35);
  assert.equal(DATA.MUSIC_TRACKS.length, 5);
});

test('все шаблоны имеют совместимые сокеты, DDR, PSU и PCIe', () => {
  const ids = new Set();
  for (const part of DATA.PARTS) {
    assert.equal(ids.has(part.id), false, `duplicate ${part.id}`);
    ids.add(part.id);
  }
  for (const template of DATA.SERVER_TEMPLATES) {
    const chassis = DATA.part(template.chassis);
    const cpu = DATA.part(template.cpu);
    const psu = DATA.part(template.psu);
    assert.equal(chassis.type, 'chassis', template.id);
    assert.equal(cpu.socket, chassis.socket, template.id);
    template.ram.forEach(id => assert.equal(DATA.part(id).ramType, chassis.ramType, template.id));
    const gpus = template.gpus || [];
    assert.ok((chassis.pcieSlots || 0) >= gpus.length, template.id);
    const required = Math.round(70 + cpu.tdp + cpu.cores * 35 + template.disks.length * 10 + template.ram.length * 4 + gpus.reduce((sum, id) => sum + DATA.part(id).tdp * 1.25, 0));
    assert.ok(psu.watts >= required, `${template.id}: ${psu.watts} < ${required}`);
  }
});

test('GPU устанавливается в PCIe, учитывает PSU и не потребляет энергию выключенной', () => {
  const game = fresh();
  const server = game.state.servers[0];
  const gpu = game.makePart('rtx3060');
  game.state.inventory.push(gpu);
  assert.equal(game.installPart(server.id, gpu.uid, 'gpu0').ok, true);
  assert.equal(server.parts.gpus.length, 1);
  assert.ok(game.hardwareReport(server).requiredPsu > 300);
  assert.equal(game.togglePower(server.id), false);
  const psu = game.makePart('psu500');
  game.state.inventory.push(psu);
  game.installPart(server.id, psu.uid, 'psu');
  assert.equal(game.hardwareReport(server).ok, true);
  assert.equal(game.metrics(server).gpuCompute, 13);
  assert.equal(game.metrics(server).watts, 0);
});

test('AI-шаблон содержит реальную GPU и усиляет GPU-софт', () => {
  const game = fresh();
  const server = game.instantiateServer('customam5');
  game.state.servers.push(server);
  assert.equal(server.parts.gpus[0].catalogId, 'rtx4090');
  assert.equal(game.hardwareReport(server).gpuVram, 24);
  assert.equal(game.togglePower(server.id), true);
  server.os = 'debian';
  server.software = ['stablediffusion'];
  const withoutGpu = server.parts.gpus;
  server.parts.gpus = [];
  const base = game.hourlyServerRevenue(server);
  server.parts.gpus = withoutGpu;
  const accelerated = game.hourlyServerRevenue(server);
  assert.ok(accelerated > base * 2);
});

test('RAID 1 уменьшает ёмкость и меняет скорость, конфигурация стирает ОС', () => {
  const game = fresh();
  const server = bootDebian(game);
  game.powerOff(server);
  const disk = game.makePart('hdd500');
  game.state.inventory.push(disk);
  game.installPart(server.id, disk.uid, 'disk1');
  assert.equal(game.configureRAID(server.id, 'raid1'), true);
  assert.equal(game.hardwareReport(server).diskTotal, 500);
  assert.equal(server.os, null);
  assert.ok(game.metrics(server).storageFactor > 1);
});

test('тариф влияет на доход, авария провайдера останавливает его', () => {
  const game = fresh();
  const server = bootDebian(game, ['boinc']);
  const base = game.hourlyServerRevenue(server);
  assert.equal(game.upgradeNetwork('dc10g'), true);
  const fast = game.hourlyServerRevenue(server);
  assert.ok(fast > base * 1.3);
  game.state.outageDay = game.state.day;
  assert.equal(game.hourlyServerRevenue(server), 0);
});

test('кондиционер и PDU меняют физические параметры помещения', () => {
  const game = fresh();
  const ambient = game.room().ambient;
  const limit = game.room().powerLimit;
  assert.equal(game.buyUpgrade('ac'), true);
  assert.equal(game.buyUpgrade('pdu'), true);
  assert.equal(game.room().ambient, ambient - 3);
  assert.equal(game.room().powerLimit, limit + 1000);
});

test('ИБП предотвращает аварийное отключение и снимает самую тяжёлую нагрузку', () => {
  const game = fresh();
  const server = game.state.servers[0];
  game.buyUpgrade('ups');
  server.powered = true;
  game.tripPower('Автомат сработал', false);
  assert.equal(game.state.powerOut, false);
  assert.equal(server.powered, false);
  assert.equal(game.state.upsUsedDay, game.state.day);
});

test('контракт проверяет реальный софт/RAM/CPU и выплачивает награду', () => {
  const game = fresh();
  const template = DATA.CONTRACT_TEMPLATES.find(item => item.requiredSoft === 'jellyfin');
  const contract = Object.assign({}, template, { id: 'test-contract' });
  game.state.contracts = [contract];
  assert.equal(game.acceptContract(contract.id), true);
  bootDebian(game, ['jellyfin']);
  const before = game.state.money;
  game.processContract();
  assert.equal(game.state.activeContract.progress, 1);
  game.processContract();
  assert.equal(game.state.activeContract, null);
  assert.equal(game.state.money, before + contract.reward);
  assert.ok(game.state.xp >= contract.xp);
});

test('каталог новых серверов продаёт и устанавливает модель в стойку', () => {
  const game = fresh();
  const count = game.state.servers.length;
  assert.equal(game.buyCatalogServer('r610'), true);
  assert.equal(game.state.servers.length, count + 1);
  assert.equal(game.state.servers.at(-1).templateId, 'r610');
});

test('случайное событие жары применяется к комнате на следующий день', () => {
  const game = fresh();
  game.state.flags.onboarded = true;
  const ambient = game.room().ambient;
  const random = Math.random;
  Math.random = () => .1;
  try { game.rollDailyEvent(); } finally { Math.random = random; }
  assert.equal(game.state.heatwaveDay, game.state.day);
  assert.equal(game.room().ambient, ambient + 6);
});

test('миграция старого сохранения добавляет GPU/RAID/сеть/настройки', () => {
  const game = fresh();
  const server = game.state.servers[0];
  delete server.parts.gpus;
  delete server.raid;
  delete game.state.settings;
  delete game.state.upgrades;
  delete game.state.networkId;
  game.normalize();
  assert.deepEqual(server.parts.gpus, []);
  assert.equal(server.raid, 'none');
  assert.equal(game.state.settings.track, 'electro');
  assert.equal(game.state.networkId, 'home100');
  assert.equal(game.state.upgrades.ac, 0);
});

console.log(`\n${passed} сценариев Expansion 2.0 пройдено.`);
