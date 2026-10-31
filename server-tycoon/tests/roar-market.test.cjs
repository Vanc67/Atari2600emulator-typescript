/* Ultimate 7.1: node tests/roar-market.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','data','expansion','mega','pc-data','audio','long-lofi','pc-sound','game','systems','lab','server-sound','timeline','deep-lab','market-search','pc-workshop','debug']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame;
let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh(year = 2026) { const game = new Game(false); game.state.currentYear = year; game.state.startYear = year; game.events.length = 0; return game; }
function param(value = 0) { return { value, targets: [], setTargetAtTime(v) { this.value = v; this.targets.push(v); }, setValueAtTime(v) { this.value = v; }, exponentialRampToValueAtTime(v) { this.value = v; } }; }
class FakeNode {
  constructor() { this.connections = []; }
  connect(target) { this.connections.push(target); return target; }
  disconnect() { this.disconnected = true; }
}
function fakeContext() {
  const c = { currentTime: 1, sampleRate: 4000, destination: new FakeNode() };
  c.createGain = () => Object.assign(new FakeNode(), { gain: param(1) });
  c.createOscillator = () => Object.assign(new FakeNode(), { frequency: param(440), start() { this.started = true; }, stop() { this.stopped = true; } });
  c.createBuffer = (channels, length, rate) => ({ duration: length / rate, getChannelData: () => new Float32Array(length) });
  c.createBufferSource = () => Object.assign(new FakeNode(), { playbackRate: param(1), start(...args) { this.started = args; }, stop() { this.stopped = true; } });
  c.createBiquadFilter = () => Object.assign(new FakeNode(), { frequency: param(350), Q: param(1) });
  c.createStereoPanner = () => Object.assign(new FakeNode(), { pan: param(0) });
  c.createDynamicsCompressor = () => Object.assign(new FakeNode(), { threshold: param(), knee: param(), ratio: param(), attack: param(), release: param() });
  return c;
}
function soundGame() {
  const game = fresh(); const first = game.state.servers[0]; const second = game.instantiateServer('era_2006_04');
  first.powered = true; second.powered = true; game.state.servers.push(second);
  game.state.settings.serverSounds = true;
  const context = fakeContext(); const audio = { unlocked: true, ensureContext: () => context };
  return { game, first, second, context, sound: new ServerSoundscape(game, audio) };
}

test('ROAR: каждый канал содержит 7 слоёв и компрессор, а не простой тональный вой', () => {
  const { game, first, context, sound } = soundGame();
  sound.sync();
  assert.equal(sound.sessions.size, 2); assert.equal(sound.compressor.ratio.value, 5);
  const session = sound.sessions.get(first.id); const profile = sound.profile(first);
  assert.equal(session.sources.length, 7); assert.ok(session.turbulence && session.rumble && session.bearing && session.coil);
  assert.ok(profile.noiseGain > profile.fanGain * 20); assert.ok(profile.label.length > 10);
  const quietCooler = Object.assign({}, first, { parts: Object.assign({}, first.parts, { cooler: Object.assign({}, first.parts.cooler, { noise: 1 }) }) });
  const quietProfile = sound.profile(quietCooler); assert.ok(quietProfile.noiseGain < profile.noiseGain); assert.ok(quietProfile.rpm < profile.rpm);
  assert.equal(session.output.gain.value, profile.voiceGain); assert.ok(context.destination);
});

test('ROAR: включение второго сервера НЕ уменьшает громкость первого', () => {
  const game = fresh(); const first = game.state.servers[0]; first.powered = true;
  game.state.settings.serverSounds = true; const context = fakeContext();
  const sound = new ServerSoundscape(game, { unlocked: true, ensureContext: () => context });
  sound.sync(); const firstSession = sound.sessions.get(first.id); const before = firstSession.output.gain.value;
  const second = game.instantiateServer('era_2006_04'); second.powered = true; game.state.servers.push(second); sound.sync();
  assert.equal(sound.sessions.size, 2); assert.equal(firstSession.output.gain.value, before);
  const other = sound.sessions.get(second.id);
  assert.notEqual(firstSession.profile.kind, other.profile.kind); assert.notEqual(firstSession.profile.pan, other.profile.pan);
  assert.notEqual(firstSession.profile.filterHz, other.profile.filterHz); assert.notEqual(firstSession.nextClick, other.nextClick);
});

test('ROAR: MUTE/SOLO независимы, нулевая громкость сохраняется, stopAll очищает узлы', () => {
  const { game, first, second, sound } = soundGame(); sound.sync();
  assert.equal(sound.toggleServerMute(second.id), true); assert.equal(sound.sessions.size, 1); assert.ok(sound.sessions.has(first.id));
  assert.equal(sound.soloServer(second.id), second.id); assert.equal(sound.sessions.size, 1); assert.ok(sound.sessions.has(second.id));
  sound.soloServer(second.id); assert.equal(sound.sessions.size, 2);
  sound.setVolume(0); sound.sync(); assert.equal(game.state.settings.serverVolume, 0); assert.equal(sound.master.gain.value, 0);
  sound.stopAll(); assert.equal(sound.sessions.size, 0);
});

test('Avito: прямой запрос по названию и нескольким словам без refreshListings', () => {
  const game = fresh(); const part = game.availableParts().find(item => item.type === 'cpu' && item.name.split(' ').length >= 2);
  const query = part.name.split(' ').slice(0, 2).join(' '); let refreshes = 0; const refresh = game.refreshListings;
  game.refreshListings = function () { refreshes += 1; return refresh.call(this); };
  const results = game.findListings(query, { limit: 16 });
  assert.ok(results.length > 0); assert.ok(results.every(listing => game.listingMatches(listing, query))); assert.equal(refreshes, 0);
  assert.equal(game.state.listings.length <= 60, true);
});

test('Avito: filter-only по DDR2, цене и типу возвращает только подходящую RAM', () => {
  const game = fresh(); const results = game.findListings('', { partType: 'ram', ramType: 'DDR2', minPrice: 150, maxPrice: 1000, limit: 20 });
  assert.ok(results.length > 0);
  results.forEach(listing => { const details = game.listingDetails(listing); assert.equal(details.partType, 'ram'); assert.ok(details.ramTypes.includes('DDR2')); assert.ok(listing.price >= 150 && listing.price <= 1000); });
});

test('Avito: сокет + бренд + год + только серверы работают одновременно', () => {
  const game = fresh(); const template = DATA.SERVER_TEMPLATES.find(item => item.brand === 'hp' && DATA.part(item.cpu).socket);
  const socket = DATA.part(template.cpu).socket;
  const results = game.findListings('', { kind: 'servers', brand: 'hp', socket, yearFrom: template.year, yearTo: template.year, limit: 20 });
  assert.ok(results.length > 0);
  results.forEach(listing => { const details = game.listingDetails(listing); assert.equal(details.isServer, true); assert.equal(details.brand, 'hp'); assert.ok(details.sockets.includes(socket)); assert.equal(details.year, template.year); });
});

test('Avito: состояние, доступность по деньгам, RAM и диск учитываются в выдаче', () => {
  const game = fresh(1992); game.state.money = 200;
  const cheap = game.findListings('', { partType: 'ram', maxPrice: 200, affordable: true, limit: 12 });
  assert.ok(cheap.length > 0); assert.ok(cheap.every(listing => listing.price <= 200));
  game.state.currentYear = 2026; game.state.money = 10000000;
  const large = game.findListings('', { kind: 'servers', minRam: 64, minStorage: 1000, condition: 'working', limit: 12 });
  assert.ok(large.length > 0);
  large.forEach(listing => { const details = game.listingDetails(listing); assert.ok(details.ramTotal >= 64); assert.ok(details.storageTotal >= 1000); assert.equal(details.condition, 'working'); });
  assert.equal(game.listingDetails(Object.assign({}, large[0], { condition: 'noRam' })).ramTotal, 0);
  assert.equal(game.listingDetails(Object.assign({}, large[0], { condition: 'noDisks' })).storageTotal, 0);
});

test('Avito: сортировка по цене/году и покупка найденной детали сохраняют реальную экономику', () => {
  const game = fresh(); const results = game.findListings('', { partType: 'ram', sort: 'priceAsc', limit: 20 });
  assert.ok(results.length > 4); for (let i = 1; i < results.length; i += 1) assert.ok(results[i - 1].price <= results[i].price);
  const listing = results[0]; game.state.money = listing.price + 1000; const inventoryBefore = game.state.inventory.length;
  const result = game.buyListing(listing.id); assert.equal(result.ok, true); assert.equal(game.state.inventory.length, inventoryBefore + 1);
  assert.ok(game.state.inventory.some(part => part.catalogId === listing.partId));
});

test('Avito: опции фильтров следуют текущей эпохе и кеш не блокирует новые сокеты', () => {
  const game = fresh(1990); const oldOptions = game.marketFilterOptions();
  assert.ok(oldOptions.sockets.length > 0); assert.equal(oldOptions.ramTypes.includes('DDR5'), false);
  game.state.currentYear = 2026; const modern = game.marketFilterOptions();
  assert.ok(modern.ramTypes.includes('DDR5')); assert.ok(modern.types.includes('ram')); assert.ok(modern.brands.length > 3);
  assert.ok(modern.sockets.length >= oldOptions.sockets.length);
});

test('Avito: серия текстовых и фильтровых запросов сохраняет лимит 60 лотов', () => {
  const game = fresh();
  ['DDR2','DDR3','DDR4','Xeon','SSD','ProLiant'].forEach(query => game.findListings(query, { limit: 24 }));
  game.findListings('', { kind: 'servers', minRam: 32, limit: 24 });
  assert.ok(game.state.listings.length <= 60); assert.ok(game.state.listings.length > 20);
});

console.log(`\nROAR & Avito 7.1: ${passed} scenarios passed`);
