/* Control Room 4.0: node tests/lab.test.cjs */
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
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
  server.powered = true;
  game.updateStatus(server);
  return server;
}

test('Control Room: 6 моделей AI, лёгкий вход от 1 ГБ и пять локальных треков', () => {
  assert.equal(Object.keys(DATA.AI_MODELS).length, 6);
  assert.equal(DATA.AI_MODELS.tiny05.minRam, 1);
  assert.equal(Object.keys(DATA.STRESS_TESTS).length, 4);
  assert.equal(DATA.MUSIC_TRACKS.length, 5);
  for (const track of DATA.MUSIC_TRACKS) {
    assert.ok(!/^https?:/.test(track.file));
    const file = path.join(__dirname, '..', 'www', track.file);
    assert.equal(fs.readFileSync(file).subarray(0, 4).toString(), 'OggS', track.file);
  }
});

test('Local AI 0,5B устанавливается без старого барьера 32 ГБ', () => {
  const game = fresh();
  const server = boot(game);
  assert.equal(game.installAIModel(server.id, 'tiny05'), true);
  assert.equal(server.aiModel, 'tiny05');
  assert.ok(server.software.includes('ollama'));
  const m = game.metrics(server);
  assert.ok(m.ramUsed < 4, `ramUsed=${m.ramUsed}`);
  assert.ok(game.hourlyServerRevenue(server) > 0);
  assert.equal(DATA.software('ollama').minRam, 32, 'глобальная classic-запись восстановлена');
  assert.equal(DATA.software('ollama').ram, 20);
});

test('Выбор 7B реально увеличивает RAM/CPU и доход, слишком тяжёлая модель отклоняется', () => {
  const game = fresh();
  const server = boot(game);
  game.installAIModel(server.id, 'tiny05');
  const light = game.metrics(server);
  const lightRevenue = game.hourlyServerRevenue(server);
  assert.equal(game.installAIModel(server.id, 'qwen7b'), true);
  const heavy = game.metrics(server);
  assert.ok(heavy.ramUsed > light.ramUsed + 4);
  assert.ok(heavy.cpuDemand > light.cpuDemand);
  assert.ok(game.hourlyServerRevenue(server) > lightRevenue);
  assert.equal(game.installAIModel(server.id, 'llama70'), false);
  assert.equal(server.aiModel, 'qwen7b');
  assert.equal(DATA.software('ollama').cpu, 90);
});

test('Старый save с Ollama сохраняет классическую 70B и прежние 20 ГБ RAM', () => {
  const game = fresh();
  const server = boot(game);
  server.software = ['ollama'];
  delete server.aiModel;
  game.normalizeServer(server);
  assert.equal(server.aiModel, 'heavy70');
  const m = game.metrics(server);
  assert.ok(m.ramUsed >= 20);
  assert.equal(DATA.software('ollama').ram, 20);
});

test('CPU Burn создаёт 200% спроса, отключает доход и полностью снимается', () => {
  const game = fresh();
  const server = boot(game);
  assert.equal(game.installSoftware(server.id, 'nginx'), true);
  assert.ok(game.hourlyServerRevenue(server) > 0);
  const ramBefore = game.metrics(server).ramUsed;
  assert.equal(game.startStress(server.id, 'cpu'), true);
  assert.equal(server.software.filter(id => id === 'folding').length, 2);
  assert.equal(game.metrics(server).ramUsed, ramBefore, 'CPU Burn не является RAM Avalanche');
  assert.equal(DATA.software('folding').ram, 1.5, 'глобальная память Folding восстановлена');
  assert.equal(DATA.software('folding').gpuBoost, true, 'глобальный GPU-флаг Folding восстановлен');
  assert.ok(game.metrics(server).cpuDemand >= 200);
  assert.equal(game.hourlyServerRevenue(server), 0);
  assert.equal(game.state.stats.experiments, 1);
  assert.equal(game.stopStress(server.id), true);
  assert.equal(server.stress, null);
  assert.equal(server.software.includes('folding'), false);
  assert.ok(server.software.includes('nginx'));
  assert.ok(game.hourlyServerRevenue(server) > 0);
});

test('CPU Burn на сервере 1990 года даёт тепловой троттлинг без преждевременного OOM', () => {
  const game = fresh();
  const template = DATA.SERVER_TEMPLATES.find(item => item.year === 1990 && item.ram.reduce((sum,id) => sum + DATA.part(id).capacity,0) >= .064 && item.disks.reduce((sum,id) => sum + DATA.part(id).capacity,0) >= .2);
  const server = game.instantiateServer(template.id);
  boot(game, 'debian11', server);
  game.startStress(server.id, 'cpu');
  assert.ok(game.metrics(server).ramPercent < 100);
  let throttled = false;
  for (let i = 0; i < 120 && !server.crashed; i += 1) {
    game.simulateServer(server);
    throttled = throttled || server.throttling;
  }
  assert.equal(server.hung, false);
  assert.equal(throttled, true);
  assert.ok(server.temp > 81);
});

test('CPU Burn проходит путь троттлинг → реальный BSOD на слабом охлаждении', () => {
  const game = fresh();
  const server = game.instantiateServer('era_2006_04');
  server.os = 'debian'; server.powered = true; game.state.servers.push(server);
  game.startStress(server.id, 'cpu');
  let throttled = false;
  for (let i = 0; i < 60 && !server.crashed; i += 1) {
    game.simulateServer(server);
    throttled = throttled || server.throttling;
  }
  assert.equal(throttled, true);
  assert.equal(server.crashed, true);
  assert.equal(server.powered, false);
  assert.equal(server.temp, 95);
  assert.equal(game.state.stats.crashes, 1);
});

test('RAM Avalanche вызывает настоящий OOM за пять тиков, затем reboot работает', () => {
  const game = fresh();
  const server = boot(game);
  assert.equal(game.startStress(server.id, 'ram'), true);
  assert.ok(game.metrics(server).ramPercent > 100);
  for (let i = 0; i < 5; i += 1) game.simulateServer(server);
  assert.equal(server.hung, true);
  assert.equal(server.powered, false);
  assert.equal(game.state.stats.hangs, 1);
  assert.equal(game.stopStress(server.id), true);
  assert.equal(game.restartServer(server.id), true);
  assert.equal(server.hung, false);
  assert.equal(server.powered, true);
  assert.equal(server.software.includes('virtuallab'), false);
});

test('Chaos Protocol запускает 70B/VM/SD даже на древнем сервере и не приносит доход', () => {
  const game = fresh();
  const template = DATA.SERVER_TEMPLATES.find(item => item.year === 1990 && item.ram.reduce((sum,id) => sum + DATA.part(id).capacity,0) >= .064 && item.disks.reduce((sum,id) => sum + DATA.part(id).capacity,0) >= .2);
  const server = game.instantiateServer(template.id);
  boot(game, 'debian11', server);
  assert.equal(game.startStress(server.id, 'chaos'), true);
  assert.equal(server.aiModel, 'heavy70');
  assert.ok(server.software.includes('stablediffusion'));
  assert.ok(server.software.includes('ollama'));
  assert.ok(game.metrics(server).ramPercent > 100);
  assert.equal(game.hourlyServerRevenue(server), 0);
  assert.equal(game.stopStress(server.id), true);
  assert.equal(server.aiModel, null);
  assert.equal(server.software.length, 0);
});

test('Терминал выполняет реальные status/install/services/ai/network и unknown', () => {
  const game = fresh();
  const server = boot(game);
  assert.equal(game.executeCommand(server.id, 'status').ok, true);
  assert.ok(game.executeCommand(server.id, 'status').lines.join(' ').includes('CPU'));
  assert.equal(game.executeCommand(server.id, 'install nginx').ok, true);
  assert.ok(server.software.includes('nginx'));
  assert.ok(game.executeCommand(server.id, 'services').lines.join(' ').includes('NGINX'));
  assert.equal(game.executeCommand(server.id, 'ai tiny05').ok, true);
  assert.equal(server.aiModel, 'tiny05');
  assert.ok(game.executeCommand(server.id, 'network').lines.join(' ').includes('Мбит/с'));
  assert.equal(game.executeCommand(server.id, 'definitely-unknown').ok, false);
});

test('poweroff/reboot из консоли меняют питание, недоступная ОС не принимает команды', () => {
  const game = fresh();
  const server = boot(game);
  assert.equal(game.executeCommand(server.id, 'poweroff').ok, true);
  assert.equal(server.powered, false);
  assert.equal(game.executeCommand(server.id, 'status').ok, false);
  assert.equal(game.restartServer(server.id), true);
  assert.equal(server.powered, true);
  assert.equal(game.executeCommand(server.id, 'reboot').ok, true);
  assert.equal(server.powered, true);
});

test('Soundscape: частоты зависят от ID, HDD и реальной нагрузки', () => {
  const game = fresh();
  const server = boot(game);
  const soundscape = new ServerSoundscape(game, null);
  const idle = soundscape.profile(server);
  assert.ok(idle.hdd >= 1);
  game.startStress(server.id, 'cpu');
  const loaded = soundscape.profile(server);
  assert.ok(loaded.fanHz > idle.fanHz);
  assert.ok(loaded.noiseGain > idle.noiseGain);
  const other = game.instantiateServer('office6000');
  game.state.servers.push(other);
  assert.notEqual(soundscape.profile(other).fanHz, idle.fanHz);
  assert.equal(game.state.settings.serverSounds, false);
});

test('Переустановка ОС очищает владельцев нагрузки и AI-модель', () => {
  const game = fresh();
  const server = boot(game);
  game.startStress(server.id, 'chaos');
  game.powerOff(server);
  const usb = game.state.inventory.find(part => part.type === 'usb');
  assert.equal(game.finishOSInstall(server.id, usb.uid), true);
  assert.equal(server.stress, null);
  assert.equal(server.aiModel, null);
  assert.equal(server.software.length, 0);
});

test('Нормализация 4.0 сохраняет настройки и добавляет безопасные defaults', () => {
  const game = fresh();
  assert.equal(game.state.version, 8);
  assert.equal(game.state.settings.serverSounds, false);
  assert.equal(game.state.settings.serverVolume, .28);
  game.state.settings.serverVolume = .6;
  game.normalize();
  assert.equal(game.state.settings.serverVolume, .6);
  assert.equal(game.state.settings.music, true);
});

console.log(`\n${passed} сценариев Control Room 4.0 пройдено.`);
