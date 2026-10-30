/* Запуск: node tests/logic.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
require('../www/js/data.js');
require('../www/js/game.js');
const Game = global.ServerTycoonGame;

let passed = 0;
function test(name, fn) {
  try {
    fn();
    passed += 1;
    console.log(`✓ ${name}`);
  } catch (error) {
    console.error(`✗ ${name}`);
    throw error;
  }
}

function fresh() {
  const game = new Game(false);
  game.events.length = 0;
  return game;
}

test('старт: 100 000 ₽, сервер, USB и инструменты', () => {
  const game = fresh();
  assert.equal(game.state.money, 100000);
  assert.equal(game.state.servers.length, 1);
  assert.ok(game.state.inventory.some(p => p.type === 'usb'));
  assert.ok(game.state.inventory.some(p => p.type === 'tool'));
});

test('DDR4 физически не устанавливается в DDR3', () => {
  const game = fresh();
  const server = game.state.servers[0];
  const ram = game.makePart('ddr4-8');
  game.state.inventory.push(ram);
  const result = game.installPart(server.id, ram.uid, 'ram0');
  assert.equal(result.ok, false);
  assert.ok(game.getPart(ram.uid));
});

test('БП 300W не запускает 4-ядерный Q9650, БП 500W запускает', () => {
  const game = fresh();
  const server = game.state.servers[0];
  const cpu = game.makePart('q9650');
  game.state.inventory.push(cpu);
  assert.equal(game.installPart(server.id, cpu.uid, 'cpu').ok, true);
  assert.ok(game.hardwareReport(server).requiredPsu > 300);
  assert.equal(game.togglePower(server.id), false);
  const psu = game.makePart('psu500');
  game.state.inventory.push(psu);
  assert.equal(game.installPart(server.id, psu.uid, 'psu').ok, true);
  assert.equal(game.hardwareReport(server).ok, true);
  assert.equal(game.togglePower(server.id), true);
  assert.equal(server.powered, true);
});

test('USB → Debian → BOINC → почасовой доход', () => {
  const game = fresh();
  const server = game.state.servers[0];
  const usb = game.state.inventory.find(p => p.type === 'usb');
  assert.equal(game.writeImage(usb.uid, 'debian'), true);
  assert.equal(game.checkOSInstall(server.id, usb.uid).ok, true);
  assert.equal(game.finishOSInstall(server.id, usb.uid), true);
  assert.equal(server.os, 'debian');
  assert.equal(server.powered, true);
  assert.equal(game.installSoftware(server.id, 'boinc'), true);
  assert.ok(game.hourlyServerRevenue(server) > 0);
  const before = game.state.money;
  game.collectHourlyRevenue();
  assert.ok(game.state.money > before);
});

test('95°C вызывает BSOD, ремонт снимает аварию', () => {
  const game = fresh();
  const server = game.state.servers[0];
  server.powered = true;
  server.temp = 110;
  game.simulateServer(server);
  assert.equal(server.crashed, true);
  assert.equal(server.status, 'bsod');
  assert.equal(server.powered, false);
  assert.equal(game.repairServer(server.id), true);
  assert.equal(server.crashed, false);
});

test('ОЗУ >100% приводит к зависанию за пять тиков', () => {
  const game = fresh();
  const server = game.state.servers[0];
  const usb = game.state.inventory.find(p => p.type === 'usb');
  game.writeImage(usb.uid, 'debian');
  // Доводим память до 16 ГБ, чтобы Local AI можно было установить.
  const extra = game.state.inventory.find(p => p.type === 'ram');
  game.installPart(server.id, extra.uid, 'ram2');
  const ram = game.makePart('ddr3-4');
  game.state.inventory.push(ram);
  game.installPart(server.id, ram.uid, 'ram3');
  game.finishOSInstall(server.id, usb.uid);
  for (const id of ['jellyfin', 'filebrowser', 'boinc', 'localai']) {
    assert.equal(game.installSoftware(server.id, id), true, id);
  }
  assert.ok(game.metrics(server).ramPercent > 100);
  for (let i = 0; i < 5; i += 1) game.simulateServer(server);
  assert.equal(server.hung, true);
  assert.equal(server.status, 'hung');
});

test('генератор выдаёт 5–10 лотов, успешный торг даёт ровно −15%', () => {
  const game = fresh();
  for (let i = 0; i < 30; i += 1) {
    const count = game.generateListingSet().length;
    assert.ok(count >= 5 && count <= 10);
  }
  const listing = game.state.listings[0];
  const oldRandom = Math.random;
  Math.random = () => 0.1;
  try {
    const before = listing.price;
    const result = game.bargain(listing.id);
    assert.equal(result.ok, true);
    assert.equal(listing.price, Math.max(100, Math.round(before * 0.85 / 10) * 10));
  } finally {
    Math.random = oldRandom;
  }
});

test('отрицательный баланс → долг и отключение, помощь мамы восстанавливает свет', () => {
  const game = fresh();
  game.state.money = 0;
  game.endDay();
  assert.ok(game.state.debt > 0);
  assert.equal(game.state.powerOut, true);
  assert.equal(game.momHelp(), true);
  assert.equal(game.state.debt, 0);
  assert.equal(game.state.powerOut, false);
  assert.ok(game.state.money > 0);
});

test('ускоренный день проходит через обычный tick и завершается один раз', () => {
  const game = fresh();
  const before = game.state.day;
  game.skipDay();
  assert.equal(game.state.day, before + 1);
  assert.equal(game.state.stats.daily.length, 1);
  assert.ok(game.state.minute >= 0 && game.state.minute < 1440);
});

console.log(`\n${passed} игровых сценариев пройдено.`);
