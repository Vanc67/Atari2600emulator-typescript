/* Mega Archive 3.0: node tests/mega.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','game','systems','lab','timeline','deep-lab','debug']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name,fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh() { const game = new Game(false); game.events.length = 0; return game; }

test('масштаб: 2500+ серверов, 5500+ деталей, 2000+ сервисов и 58 ОС',() => {
  assert.ok(DATA.SERVER_TEMPLATES.length >= 2500);
  assert.ok(DATA.PARTS.length >= 5500);
  assert.ok(DATA.SOFTWARE.length >= 2000);
  assert.ok(DATA.OS_LIST.length >= 58);
  assert.equal(DATA.MEGA_STATS.startYear,1990);
  assert.equal(DATA.MEGA_STATS.endYear,2026);
});

test('каждый год 1990–2026 содержит 68 новых конфигураций',() => {
  for (let year = 1990; year <= 2026; year += 1) {
    assert.equal(DATA.SERVER_TEMPLATES.filter(template => template.id.startsWith(`era_${year}_`)).length,68,String(year));
  }
});

test('бренды не выпадают из заявленных эпох',() => {
  for (let year = 1990; year <= 2002; year += 1) assert.ok(DATA.SERVER_TEMPLATES.some(template => template.year === year && template.brand === 'compaq'),`Compaq ${year}`);
  for (let year = 1994; year <= 2026; year += 1) assert.ok(DATA.SERVER_TEMPLATES.some(template => template.year === year && template.brand === 'dell'),`Dell ${year}`);
  for (let year = 2018; year <= 2026; year += 1) assert.ok(DATA.SERVER_TEMPLATES.some(template => template.year === year && template.brand === 'arm'),`ARM ${year}`);
});

test('уникальные ID и все ссылки деталей/ОС/сервисов существуют',() => {
  assert.equal(new Set(DATA.PARTS.map(part => part.id)).size,DATA.PARTS.length);
  assert.equal(new Set(DATA.SERVER_TEMPLATES.map(template => template.id)).size,DATA.SERVER_TEMPLATES.length);
  assert.equal(new Set(DATA.SOFTWARE.map(app => app.id)).size,DATA.SOFTWARE.length);
  assert.equal(new Set(DATA.OS_LIST.map(os => os.id)).size,DATA.OS_LIST.length);
  for (const template of DATA.SERVER_TEMPLATES) {
    for (const id of [template.chassis,template.cpu,...template.ram,...template.disks,template.cooler,template.psu,...(template.gpus || [])]) assert.ok(DATA.part(id),`${template.id}: ${id}`);
  }
  for (const app of DATA.SOFTWARE) for (const id of app.os) assert.ok(DATA.os(id),`${app.id}: ${id}`);
});

test('все 2597 шаблонов совместимы по сокету, DDR, PCIe и PSU',() => {
  for (const template of DATA.SERVER_TEMPLATES) {
    const chassis = DATA.part(template.chassis);
    const cpu = DATA.part(template.cpu);
    const psu = DATA.part(template.psu);
    assert.equal(cpu.socket,chassis.socket,template.id);
    assert.ok(template.ram.length <= chassis.ramSlots,template.id);
    assert.ok(template.disks.length <= chassis.sataPorts,template.id);
    template.ram.forEach(id => assert.equal(DATA.part(id).ramType,chassis.ramType,template.id));
    const gpus = template.gpus || [];
    assert.ok(gpus.length <= (chassis.pcieSlots || 0),template.id);
    const required = Math.round(70 + cpu.tdp + cpu.cores * 35 + template.disks.length * 10 + template.ram.length * 4 + gpus.reduce((sum,id) => sum + DATA.part(id).tdp * 1.25,0));
    assert.ok(psu.watts >= required,`${template.id}: ${psu.watts} < ${required}`);
  }
});

test('машины каждой эпохи создаются с рабочим hardware report',() => {
  const game = fresh();
  for (const year of [1990,1995,2000,2005,2010,2015,2020,2026]) {
    const server = game.instantiateServer(`era_${year}_01`);
    const report = game.hardwareReport(server);
    assert.equal(report.ok,true,`${year}: ${report.issues.join(', ')}`);
    assert.equal(server.chassis.year,year);
    assert.equal(game.metrics(server).watts,0);
  }
});

test('1990-е: Debian 1.1 и NCSA HTTPd работают на мегабайтах RAM',() => {
  const game = fresh();
  const template = DATA.SERVER_TEMPLATES.find(item => item.year === 1990 && item.ram.reduce((sum,id) => sum + DATA.part(id).capacity,0) >= .064 && item.disks.reduce((sum,id) => sum + DATA.part(id).capacity,0) >= .2);
  assert.ok(template);
  const server = game.instantiateServer(template.id);
  game.state.servers.push(server);
  const usb = game.state.inventory.find(part => part.type === 'usb');
  assert.equal(game.writeImage(usb.uid,'debian11'),true);
  assert.equal(game.finishOSInstall(server.id,usb.uid),true);
  assert.equal(game.installSoftware(server.id,'legacy_service_0'),true);
  assert.ok(server.software.includes('legacy_service_0'));
});

test('современный сервис из большого каталога устанавливается и приносит доход',() => {
  const game = fresh();
  const server = game.state.servers[0];
  const usb = game.state.inventory.find(part => part.type === 'usb');
  game.writeImage(usb.uid,'debian');
  game.finishOSInstall(server.id,usb.uid);
  assert.equal(game.installSoftware(server.id,'svc_web_0_standard'),true);
  assert.ok(game.hourlyServerRevenue(server) > 0);
});

test('2026: GPU-конфигурация имеет VRAM и корректный запас БП',() => {
  const game = fresh();
  const server = game.instantiateServer('era_2026_01');
  const report = game.hardwareReport(server);
  assert.ok(report.gpuCount >= 1);
  assert.ok(report.gpuVram > 0);
  assert.equal(report.ok,true);
});

test('новая архивная модель доступна покупке, Авито остаётся ограниченным 5–10 лотами',() => {
  const game = fresh();
  const before = game.state.servers.length;
  assert.equal(game.buyCatalogServer('era_1990_01'),true);
  assert.equal(game.state.servers.length,before + 1);
  const listings = game.generateListingSet();
  assert.ok(listings.length >= 5 && listings.length <= 10);
});

console.log(`\n${passed} сценариев Mega Archive 3.0 пройдено.`);
