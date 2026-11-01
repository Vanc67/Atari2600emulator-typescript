/* Laptop Architect & PC Parts Avito 8.2: node tests/laptop-market.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','pc-branding','data','expansion','mega','pc-data','pc-architect-data','pc-laptop-data','audio','long-lofi','pc-sound','game','systems','lab','server-sound','timeline','deep-lab','market-search','avito-years','pc-workshop','pc-architect','pc-laptop','pc-parts-market','pc-plus','debug','cheats']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame; let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh() { const game = new Game(false); game.startPCCampaign(); game.events.length = 0; return game; }
function laptop(game, year = 2005) { game.state.pc.year = year; const report = game.pcAutoLaptopDraft(year); assert.ok(report); game.state.pc.money = report.metrics.total + 100000; const result = game.buildLaptopPC(); assert.equal(result.ok, true); return result.machine; }
function ownedItem(part, family, suffix, health = 60) { return { uid: `test_${suffix}`, catalogId: part.id, family, type: part.type, condition: 'used', health, paid: Math.round(part.value * .5), year: part.year, acquiredDay: 1 }; }

test('Каталоги: 4 440 laptop + неизменные 20 000 desktop, уникальные ID/names/SKU', () => {
  assert.equal(DATA.LAPTOP_PART_CATALOG.length, 4440); assert.equal(DATA.PC_PART_CATALOG.length, 20000);
  for (const key of ['id','name','sku']) assert.equal(new Set(DATA.LAPTOP_PART_CATALOG.map(part => part[key])).size, 4440);
  assert.equal(DATA.LAPTOP_PART_YEARS.length, 37); assert.equal(Object.keys(DATA.LAPTOP_PART_TYPES).length, 10);
  assert.equal(DATA.PARTS.length, 5570); assert.equal(DATA.SERVER_TEMPLATES.length, 2597); assert.equal(DATA.PC_OS.length, 13);
});

test('Laptop Architect: каждая из 10 категорий существует во всех 37 годах', () => {
  for (let year = 1990; year <= 2026; year += 1) for (const type of Object.keys(DATA.LAPTOP_PART_TYPES)) {
    const parts = DATA.LAPTOP_PART_BY_TYPE_YEAR.get(`${type}:${year}`); assert.equal(parts.length, 12, `${type}:${year}`);
    for (const part of parts) { assert.equal(part.family, 'laptop'); assert.equal(part.year, year); assert.ok(Number.isFinite(part.value) && part.value > 0); assert.ok(part.summary && part.brand); }
  }
});

test('Laptop Architect: автоподбор совместим для всех 37 лет и заполняет 10 уникальных слотов', () => {
  const game = fresh();
  for (let year = 1990; year <= 2026; year += 1) { game.state.pc.year = year; const report = game.pcAutoLaptopDraft(year); assert.ok(report, String(year)); assert.equal(report.ok, true, `${year}: ${report.errors.join('; ')}`); assert.equal(report.missing.length, 0); assert.equal(new Set(Object.values(game.pcLaptopState().draft)).size, 10); assert.ok(report.metrics.runtimeMinutes > 0 && report.metrics.weightKg > 0); }
});

test('Laptop Architect: мобильные сокеты, RAM, дисплей, батарея и адаптер реально проверяются', () => {
  const game = fresh(); game.state.pc.year = 2010; const auto = game.pcAutoLaptopDraft(2010); const draft = Object.assign({}, game.pcLaptopState().draft); game.state.pc.year = 2026;
  const wrongCpu = DATA.LAPTOP_PART_CATALOG.find(part => part.type === 'cpu' && part.year === 2010 && part.socket !== auto.parts.motherboard.socket);
  game.setLaptopDraftPart('cpu', wrongCpu.id); assert.ok(game.pcLaptopCompatibility().errors.some(error => error.includes('сокет'))); game.setLaptopDraftPart('cpu', draft.cpu);
  const wrongRam = DATA.LAPTOP_PART_CATALOG.find(part => part.type === 'ram' && part.ramType !== auto.parts.motherboard.ramType);
  game.setLaptopDraftPart('ram', wrongRam.id); assert.ok(game.pcLaptopCompatibility().errors.some(error => error.includes('SO-DIMM'))); game.setLaptopDraftPart('ram', draft.ram);
  const wrongBattery = DATA.LAPTOP_PART_CATALOG.find(part => part.type === 'battery' && part.year === 2010 && part.voltage !== auto.parts.case.batteryVoltage);
  game.setLaptopDraftPart('battery', wrongBattery.id); assert.ok(game.pcLaptopCompatibility().errors.some(error => error.includes('Батарея'))); game.setLaptopDraftPart('battery', draft.battery);
  const wrongAdapter = DATA.LAPTOP_PART_CATALOG.find(part => part.type === 'psu' && part.connector !== auto.parts.case.adapterConnector);
  game.setLaptopDraftPart('psu', wrongAdapter.id); assert.ok(game.pcLaptopCompatibility().errors.some(error => error.includes('Адаптер'))); game.setLaptopDraftPart('psu', draft.psu);
  const hugeDisplay = DATA.LAPTOP_PART_CATALOG.find(part => part.type === 'display' && (part.sizeInch > auto.parts.case.maxDisplayInch || !auto.parts.case.displayConnectors.includes(part.connector)));
  game.setLaptopDraftPart('display', hugeDisplay.id); assert.ok(game.pcLaptopCompatibility().errors.some(error => error.includes('Дисплей') || error.includes('дисплей')));
});

test('Laptop Architect: поиск, бренд, сокет, избранное, совместимость и пагинация', () => {
  const game = fresh(); game.state.pc.year = 2005; game.pcAutoLaptopDraft(2005);
  const page = game.pcLaptopCatalog({ type: 'cpu', year: 2005, perPage: 4 }); assert.equal(page.items.length, 4); assert.equal(page.total, 12); assert.equal(page.pages, 3);
  const intel = game.pcLaptopCatalog({ type: 'cpu', year: 2005, query: 'Intel', perPage: 30 }); assert.ok(intel.total > 0 && intel.items.every(part => part.name.includes('Intel')));
  const compatible = game.pcLaptopCatalog({ type: 'cpu', year: 2005, compatibleOnly: true, perPage: 30 }); assert.ok(compatible.total > 0 && compatible.items.every(part => part.socket === game.pcLaptopCompatibility().parts.motherboard.socket));
  const favorite = page.items[0]; assert.equal(game.pcToggleLaptopFavorite(favorite.id), true); assert.equal(game.pcLaptopCatalog({ type: 'cpu', year: 2005, favoritesOnly: true }).total, 1);
});

test('Сборка ноутбука: деньги один раз, 10 деталей, батарея, display и сразу apps/tuning', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoLaptopDraft(2005); game.state.pc.money = report.metrics.total + 5000;
  const result = game.buildLaptopPC({ name: 'Мобильная станция' }); assert.equal(result.ok, true); assert.equal(game.state.pc.money, 5000);
  const machine = result.machine; assert.equal(machine.name, 'Мобильная станция'); assert.equal(machine.custom, true); assert.equal(machine.laptop, true); assert.equal(machine.os, null); assert.equal(machine.powered, false);
  assert.equal(game.pcCustomParts(machine).length, 10); assert.equal(machine.battery.charge, 100); assert.ok(machine.battery.capacityWh > 0 && machine.laptopDisplay.sizeInch > 0);
  assert.ok(machine.apps && machine.tuning); assert.equal(game.pcSaveNotes(machine.id, 'Laptop offline'), true); assert.equal(machine.apps.notesRevision, 1);
  assert.equal(game.installPCOS(machine.id, 'pc-winxp'), true); assert.equal(game.togglePCPower(machine.id), true); assert.ok(game.runPCBenchmark(machine.id, 'stability'));
});

test('Батарея: разряд без адаптера, заряд от сети и сохранение состояния', () => {
  const game = fresh(); const machine = laptop(game); machine.powered = true; machine.battery.adapterConnected = false; machine.battery.charge = 70;
  game.tickPC(10); const discharged = machine.battery.charge; assert.ok(discharged < 70 && discharged > 0);
  game.toggleLaptopAdapter(machine.id); assert.equal(machine.battery.adapterConnected, true); game.tickPC(10); assert.ok(machine.battery.charge > discharged);
  const charge = machine.battery.charge; game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); const restored = game.getPC(machine.id); assert.ok(Math.abs(restored.battery.charge - charge) < .0001); assert.equal(restored.battery.adapterConnected, true); assert.equal(restored.laptop, true);
});

test('Батарея: low-battery warning, реальное отключение на 0% и блокировка запуска', () => {
  const game = fresh(); const machine = laptop(game); machine.powered = true; machine.battery.adapterConnected = false; machine.battery.charge = 14;
  game.tickPC(1); assert.equal(machine.battery.lowWarned, true);
  machine.battery.charge = .001; game.tickPC(60); assert.equal(machine.battery.charge, 0); assert.equal(machine.powered, false); assert.equal(machine.battery.shutdowns, 1);
  assert.equal(game.togglePCPower(machine.id), false); assert.equal(machine.powered, false);
  game.toggleLaptopAdapter(machine.id); game.tickPC(60); assert.ok(machine.battery.charge > 0);
});

test('PC Авито: прямой typed-name поиск и семейство/год/бренд/цена/сокет работают вместе', () => {
  const game = fresh(); game.state.pc.year = 2005; game.state.pc.money = 10000000;
  const intel = game.pcFindPartsListings('Intel', { family: 'laptop', year: 2005, brand: 'Intel', minPrice: 1, maxPrice: 10000000, limit: 60 });
  assert.ok(intel.length > 0); assert.ok(intel.every(listing => listing.family === 'laptop' && listing.year === 2005 && listing.brand === 'Intel' && listing.price >= 1));
  const part = DATA.LAPTOP_PART_INDEX.get(intel[0].catalogId); const socket = part.socket || part.interface;
  if (socket) { const filtered = game.pcFindPartsListings('', { family: 'laptop', year: 2005, socket, minPrice: 1, maxPrice: 10000000, limit: 60 }); assert.ok(filtered.length > 0); assert.ok(filtered.every(listing => { const p = DATA.LAPTOP_PART_INDEX.get(listing.catalogId); return p.socket === socket || p.interface === socket || (p.sockets || []).includes(socket); })); }
  const desktop = game.pcFindPartsListings('Intel', { family: 'desktop', year: 2005, limit: 10 }); assert.ok(desktop.length > 0 && desktop.every(listing => listing.family === 'desktop'));
});

test('PC Авито: покупка создаёт отдельный owned instance, а проданный лот не возвращается', () => {
  const game = fresh(); game.state.pc.year = 2005; game.state.pc.money = 1000000; const listing = game.pcFindPartsListings('Intel', { family: 'laptop', year: 2005, limit: 1 })[0]; assert.ok(listing);
  const before = game.state.pc.money; const result = game.buyPCPartListing(listing.id); assert.equal(result.ok, true); assert.equal(game.state.pc.money, before - listing.price); assert.equal(game.state.pc.componentInventory.length, 1); assert.equal(result.item.catalogId, listing.catalogId);
  const part = result.part; const repeated = game.pcFindPartsListings(part.sku, { family: 'laptop', year: part.year, limit: 60 }); assert.ok(!repeated.some(item => item.id === listing.id)); assert.equal(game.buyPCPartListing(listing.id).ok, false); assert.equal(game.state.pc.money, before - listing.price);
});

test('PC Авито: нехватка денег не создаёт inventory и не списывает бюджет', () => {
  const game = fresh(); game.state.pc.year = 2005; game.state.pc.money = 0; const listing = game.pcFindPartsListings('Intel', { family: 'desktop', year: 2005, limit: 1 })[0];
  const result = game.buyPCPartListing(listing.id); assert.equal(result.ok, false); assert.equal(game.state.pc.money, 0); assert.equal(game.state.pc.componentInventory.length, 0);
});

test('Склад: выбор в draft не расходует деталь, продажа возвращает деньги', () => {
  const game = fresh(); game.state.pc.year = 2005; game.state.pc.money = 1000000; const listing = game.pcFindPartsListings('Intel', { family: 'laptop', year: 2005, limit: 1 })[0]; const bought = game.buyPCPartListing(listing.id);
  const selected = game.pcUseInventoryItem(bought.item.uid); assert.equal(selected.part.id, bought.part.id); assert.equal(game.pcLaptopState().draft[bought.part.type], bought.part.id); assert.equal(game.state.pc.componentInventory.length, 1);
  const before = game.state.pc.money; const sale = game.sellPCComponent(bought.item.uid); assert.ok(sale > 0); assert.equal(game.state.pc.money, before + sale); assert.equal(game.state.pc.componentInventory.length, 0);
});

test('Desktop inventory build: полная стоимость owned детали вычитается, экземпляр расходуется один раз', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoDraft(2005); const part = report.parts.cpu; game.state.pc.componentInventory.push(ownedItem(part, 'desktop', 'desktop_cpu', 50));
  const expected = report.metrics.total - part.value; game.state.pc.money = expected + 700; const result = game.buildCustomPC({ useInventory: true });
  assert.equal(result.ok, true); assert.equal(game.state.pc.money, 700); assert.equal(game.state.pc.componentInventory.length, 0); assert.equal(result.machine.architect.usedParts, 1); assert.ok(result.machine.health < 100 && result.machine.health > 90);
  assert.equal(game.pcConsumeInventory(result.plan), 0);
});

test('Прежняя desktop build без useInventory сохраняет полную цену и не трогает склад', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoDraft(2005); game.state.pc.componentInventory.push(ownedItem(report.parts.cpu, 'desktop', 'legacy_button_cpu'));
  game.state.pc.money = report.metrics.total + 100; const result = game.buildCustomPC(); assert.equal(result.ok, true); assert.equal(game.state.pc.money, 100); assert.equal(game.state.pc.componentInventory.length, 1); assert.equal(result.machine.architect.usedParts, 0); assert.equal(result.machine.health, 100);
});

test('Laptop inventory build: owned battery влияет на health/runtime и не покупается повторно', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoLaptopDraft(2005); const part = report.parts.battery; game.state.pc.componentInventory.push(ownedItem(part, 'laptop', 'laptop_battery', 50));
  const expected = report.metrics.total - part.value; game.state.pc.money = expected + 900; const result = game.buildLaptopPC({ useInventory: true });
  assert.equal(result.ok, true); assert.equal(game.state.pc.money, 900); assert.equal(game.state.pc.componentInventory.length, 0); assert.equal(result.machine.battery.health, 50); assert.ok(result.machine.battery.runtimeMinutes <= Math.ceil(report.metrics.runtimeMinutes * .5)); assert.equal(result.machine.architect.usedParts, 1);
});

test('Неудачная сборка не расходует склад, деньги, машины или draft', () => {
  const game = fresh(); game.state.pc.year = 2005; const report = game.pcAutoLaptopDraft(2005); game.state.pc.componentInventory.push(ownedItem(report.parts.cpu, 'laptop', 'keep_cpu')); const draft = Object.assign({}, game.pcLaptopState().draft); const count = game.state.pc.computers.length; game.state.pc.money = 0;
  const result = game.buildLaptopPC({ useInventory: true }); assert.equal(result.ok, false); assert.equal(game.state.pc.componentInventory.length, 1); assert.equal(game.state.pc.computers.length, count); assert.equal(game.state.pc.money, 0); assert.deepEqual(game.pcLaptopState().draft, draft);
});

test('Миграция 8.1 → 8.2 добавляет поля, сохраняя desktop/server/Avito и старую версию state', () => {
  const game = fresh(); const oldMachine = game.state.pc.computers[0]; const desktopDraft = Object.assign({}, game.pcArchitectState().draft); const listings = game.state.listings.map(item => item.id); const serverMoney = game.state.money;
  delete game.state.pc.laptopArchitect; delete game.state.pc.pcPartsMarket; delete game.state.pc.componentInventory; delete game.state.pc.stats.laptopBuilds;
  game.state = JSON.parse(JSON.stringify(game.state)); game.normalize();
  assert.ok(game.state.pc.laptopArchitect && game.state.pc.pcPartsMarket && Array.isArray(game.state.pc.componentInventory)); assert.equal(game.state.pc.stats.laptopBuilds, 0); assert.equal(game.state.version, 9);
  assert.equal(game.getPC(oldMachine.id).laptop, undefined); assert.deepEqual(game.pcArchitectState().draft, desktopDraft); assert.deepEqual(game.state.listings.map(item => item.id), listings); assert.equal(game.state.money, serverMoney); assert.equal(DATA.PC_PART_CATALOG.length, 20000);
});

test('Брендинг: HP/Intel и aliases используют embedded SVG, остальные получают безопасный wordmark', () => {
  for (const brand of ['HP','Intel','GeForce','Radeon','Fujitsu','Huawei']) { const svg = pcPartBrandSvg({ brand, name: brand }); assert.ok(svg.startsWith('<svg')); assert.ok(!svg.includes('<text')); assert.ok(!svg.includes('http://') && !svg.includes('https://') || svg.includes('xmlns="http://www.w3.org/2000/svg"')); }
  assert.equal(pcBrandKey('intel'), 'intel'); assert.equal(pcBrandKey('GeForce'), 'nvidia'); assert.equal(pcBrandKey('ATI'), 'amd');
  const fallback = pcPartBrandSvg({ brand: 'Western Digital', name: 'WD disk' }); assert.ok(fallback.includes('>WD</text>')); const escaped = pcPartBrandSvg({ brand: '<script>', name: '<script>' }); assert.ok(!escaped.includes('<script>'));
});

console.log(`\nLaptop Architect & PC Avito 8.2: ${passed} scenarios passed`);
