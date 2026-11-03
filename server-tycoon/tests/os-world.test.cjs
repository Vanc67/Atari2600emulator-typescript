/* OS World 8.3: node tests/os-world.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','pc-branding','data','expansion','mega','pc-data','pc-architect-data','pc-laptop-data','os-world-data','audio','long-lofi','pc-sound','pc-world-sound','game','systems','lab','server-sound','timeline','deep-lab','market-search','avito-years','pc-workshop','pc-architect','pc-laptop','pc-parts-market','pc-plus','os-world','debug','cheats']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame; let passed = 0;
function test(name, fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function freshPC(year = 2026) {
  const game = new Game(false); game.startPCCampaign(); game.state.pc.year = year; game.state.pc.money = 1e9;
  const report = game.pcAutoDraft(Math.min(year, 2020)); assert.ok(report && report.ok); const result = game.buildCustomPC({ free: true }); assert.equal(result.ok, true);
  const machine = result.machine; machine.storageMb = 200000; assert.equal(game.installPCOS(machine.id, year >= 2020 ? 'pc-win11' : 'pc-winxp'), true); assert.equal(game.togglePCPower(machine.id), true); game.events.length = 0; return { game, machine };
}
function freshServer() {
  const game = new Game(false); const server = game.state.servers[0]; server.os = 'debian'; server.powered = true; server.crashed = false; server.hung = false; server.broken = false; server.software = []; game.state.currentYear = 2026; game.events.length = 0; game.osWorld(server, 'server'); return { game, server };
}
function completeDownload(game, machine, family, id) { const item = game.osWorld(machine, family).downloads.find(download => download.id === id); const effective = game.osWorldEffectiveSpeed(machine, family, item); game.osWorldAdvance(machine, item.totalMb / (effective / 8) + 1, family); return item; }

test('Каталог OS World: 10 игр, 4 скорости, планы и все локальные адреса без сети', () => {
  assert.equal(DATA.OS_WORLD_GAMES.length, 10); assert.deepEqual(Object.keys(DATA.OS_WORLD_SPEEDS), ['slow','normal','fast','fiber']);
  assert.equal(DATA.OS_WORLD_GAMES.find(game => game.id === 'cyberpunk').sizeMb, 70000); assert.equal(DATA.OS_WORLD_GAMES.find(game => game.id === 'doom').sizeMb, 40);
  for (const family of ['pc','server']) for (const url of ['home.local','downloads.local','docs.local','handbook.local']) assert.ok(DATA.OS_WORLD_SITES[family][url]);
  assert.ok(DATA.OS_WORLD_SITES.pc['games.local'] && DATA.OS_WORLD_SITES.pc['avito.local']); assert.ok(DATA.OS_WORLD_SITES.server['repo.local'] && DATA.OS_WORLD_SITES.server['packages.local'] && DATA.OS_WORLD_SITES.server['status.local']);
  assert.equal(DATA.PC_PART_CATALOG.length, 20000); assert.equal(DATA.LAPTOP_PART_CATALOG.length, 4440); assert.equal(DATA.SOFTWARE.length, 2046);
});

test('Per-machine state: независимые browser/files/downloads/games и defaults после normalize', () => {
  const { game, machine } = freshPC(); const second = game.createPC(2005, { origin:'owned', owner:'test', os:'pc-winxp', name:'Second' }); game.state.pc.computers.push(second);
  const a = game.osWorld(machine, 'pc'); const b = game.osWorld(second, 'pc'); assert.notEqual(a, b); assert.notEqual(a.files, b.files); assert.equal(a.powerPlan, 'balanced'); assert.equal(a.browser.url, 'home.local');
  a.browser.url = 'games.local'; a.files[0].content = 'ONLY FIRST'; assert.equal(b.browser.url, 'home.local'); assert.notEqual(b.files[0].content, 'ONLY FIRST');
  game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); assert.equal(game.getPC(machine.id).osWorld.browser.url, 'games.local'); assert.equal(game.getPC(second.id).osWorld.browser.url, 'home.local');
});

test('Browser: navigation, 30-entry history, Back/Forward и закладки работают реально', () => {
  const { game, machine } = freshPC(); assert.equal(game.osWorldNavigate(machine.id, 'pc', 'https://games.local/').known, true); assert.equal(game.osWorld(machine, 'pc').browser.url, 'games.local');
  game.osWorldNavigate(machine.id, 'pc', 'docs.local'); assert.equal(game.osWorldNavigate(machine.id, 'pc', '', 'back').url, 'games.local'); assert.equal(game.osWorldNavigate(machine.id, 'pc', '', 'forward').url, 'docs.local');
  assert.equal(game.osWorldBookmark(machine.id, 'pc'), true); assert.ok(game.osWorld(machine, 'pc').bookmarks.includes('docs.local')); assert.equal(game.osWorldBookmark(machine.id, 'pc'), false);
  assert.equal(game.osWorldNavigate(machine.id, 'pc', 'example.com').known, false); assert.equal(game.osWorld(machine, 'pc').browser.url, 'example.com');
});

test('Download transaction: точная скорость, duplicate guard, pause/resume и complete', () => {
  const { game, machine } = freshPC(); const result = game.osWorldStartDownload(machine.id, 'pc', 'doom', 'fast'); assert.equal(result.ok, true); const item = result.download;
  game.osWorldAdvance(machine, 1, 'pc'); const effectiveStep = game.osWorldEffectiveSpeed(machine, 'pc', item) / 8; assert.equal(item.progressMb, effectiveStep); assert.equal(game.osWorldStartDownload(machine.id, 'pc', 'doom', 'fiber').ok, false); assert.equal(game.osWorld(machine, 'pc').downloads.length, 1);
  assert.equal(game.osWorldDownloadAction(machine.id, 'pc', item.id, 'pause'), true); game.osWorldAdvance(machine, 20, 'pc'); assert.equal(item.progressMb, effectiveStep);
  assert.equal(game.osWorldDownloadAction(machine.id, 'pc', item.id, 'resume'), true); game.osWorldAdvance(machine, 4, 'pc'); assert.equal(item.status, 'complete'); assert.equal(item.progressMb, 40); assert.equal(game.osWorld(machine, 'pc').stats.downloadsCompleted, 1);
});

test('Cancel освобождает все зарезервированные МБ и разрешает новую загрузку', () => {
  const { game, machine } = freshPC(); const free = game.osWorldStorage(machine, 'pc').freeMb; const first = game.osWorldStartDownload(machine.id, 'pc', 'quake', 'slow');
  assert.ok(Math.abs(game.osWorldStorage(machine, 'pc').freeMb - (free - 120)) < .001); game.osWorldAdvance(machine, 5, 'pc'); assert.equal(game.osWorldDownloadAction(machine.id, 'pc', first.download.id, 'cancel'), true);
  assert.ok(Math.abs(game.osWorldStorage(machine, 'pc').freeMb - free) < .001); const second = game.osWorldStartDownload(machine.id, 'pc', 'quake', 'fiber'); assert.equal(second.ok, true); assert.notEqual(second.download.id, first.download.id); assert.equal(game.osWorld(machine, 'pc').downloads.length, 2);
});

test('Питание/авария/глобальная пауза останавливают прогресс без потери состояния', () => {
  const { game, machine } = freshPC(); const item = game.osWorldStartDownload(machine.id, 'pc', 'doom', 'normal').download; game.osWorldAdvance(machine, 1, 'pc'); const progress = item.progressMb;
  machine.powered = false; game.osWorldAdvance(machine, 10, 'pc'); assert.equal(item.progressMb, progress); assert.equal(game.osWorldDownloadAction(machine.id, 'pc', item.id, 'resume'), false);
  machine.powered = true; machine.crashed = true; game.osWorldAdvance(machine, 10, 'pc'); assert.equal(item.progressMb, progress); machine.crashed = false;
  game.state.paused = true; game.tick(); assert.equal(item.progressMb, progress); game.state.paused = false; game.tick(); assert.ok(item.progressMb > progress);
});

test('Storage guard: нехватка места не создаёт package и не меняет диск', () => {
  const { game, machine } = freshPC(); machine.storageMb = 1; const before = game.osWorld(machine, 'pc').downloads.length; const result = game.osWorldStartDownload(machine.id, 'pc', 'doom', 'fiber');
  assert.equal(result.ok, false); assert.equal(game.osWorld(machine, 'pc').downloads.length, before); assert.equal(game.osWorldStorage(machine, 'pc').freeMb, 0);
});

test('Год и владение: future package и чужой job PC отклоняются, unlock cheat сохранён', () => {
  const { game, machine } = freshPC(2005); assert.equal(game.osWorldStartDownload(machine.id, 'pc', 'cyberpunk', 'fiber').ok, false);
  game.state.pc.cheats.unlockAll = true; machine.storageMb = 200000; assert.equal(game.osWorldStartDownload(machine.id, 'pc', 'cyberpunk', 'fiber').ok, true);
  machine.origin = 'job'; assert.equal(game.osWorldStartDownload(machine.id, 'pc', 'doom', 'fiber').ok, false);
});

test('Игры: install один раз, запуск создаёт FPS + 16 samples и live process', () => {
  const { game, machine } = freshPC(); const item = game.osWorldStartDownload(machine.id, 'pc', 'doom', 'fiber').download; completeDownload(game, machine, 'pc', item.id);
  assert.equal(game.osWorldInstall(machine.id, 'pc', item.id).ok, true); assert.equal(item.status, 'installed'); assert.equal(game.osWorldInstall(machine.id, 'pc', item.id).ok, false);
  const result = game.osWorldPlayGame(machine.id, 'doom'); assert.ok(result.fps > 0); assert.equal(result.samples.length, 16); assert.equal(machine.osWorld.games.doom.lastFps, result.fps); assert.equal(machine.osWorld.games.doom.running, true);
  assert.ok(game.osWorldProcesses(machine, 'pc').some(process => process.kind === 'game' && process.id === 'doom')); assert.equal(game.osWorldStopGame(machine.id, 'doom'), true); assert.equal(machine.osWorld.games.doom.running, false);
});

test('FPS зависит от hardware: слабый GPU не выдаёт тот же результат', () => {
  const { game, machine } = freshPC(); const state = game.osWorld(machine, 'pc'); state.games.doom = { running:false }; const original = machine.gpuScore;
  machine.gpuScore = 10; machine.cpuScore = 10; const low = game.osWorldPlayGame(machine.id, 'doom').fps; machine.gpuScore = original * 4; machine.cpuScore = original * 4; const high = game.osWorldPlayGame(machine.id, 'doom').fps;
  assert.ok(high > low, `${high} <= ${low}`); assert.equal(machine.osWorld.games.doom.launches, 2); assert.equal(machine.osWorld.games.doom.bestFps, high);
});

test('Power plans реально меняют scores/FPS/watts/temp/noise и попадают в результат', () => {
  const { game, machine } = freshPC(); machine.load = .8; game.osWorldPowerPlan(machine.id, 'eco'); const ecoScores = game.pcPerformanceScores(machine); const eco = game.pcMetrics(machine);
  game.osWorldPowerPlan(machine.id, 'performance'); const performanceScores = game.pcPerformanceScores(machine); const performance = game.pcMetrics(machine);
  assert.ok(performanceScores.cpu > ecoScores.cpu); assert.ok(performanceScores.gpu > ecoScores.gpu); assert.ok(performance.watts > eco.watts); assert.ok(performance.targetTemp > eco.targetTemp); assert.ok(performance.noise > eco.noise);
  const state = game.osWorld(machine, 'pc'); state.games.doom = { running:false }; const result = game.osWorldPlayGame(machine.id, 'doom'); assert.equal(result.powerPlan, 'performance');
});

test('Files: create/edit/revision/100 KB limit и независимое сохранение', () => {
  const { game, machine } = freshPC(); const file = game.osWorldSaveFile(machine.id, 'pc', null, 'notes/readme.txt', 'first'); assert.ok(file); assert.equal(file.name, 'notes_readme.txt');
  const revision = file.revision; game.osWorldSaveFile(machine.id, 'pc', file.id, file.name, 'x'.repeat(100500)); assert.equal(file.revision, revision + 1); assert.equal(file.content.length, 100000);
  game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); assert.equal(game.getPC(machine.id).osWorld.files.find(item => item.id === file.id).content.length, 100000);
});

test('PC Shell: store/download/pause/resume/play/plan/files используют тот же движок', () => {
  const { game, machine } = freshPC(); assert.ok(game.executePCCommand(machine.id, 'store').lines.some(line => line.includes('doom')));
  game.executePCCommand(machine.id, 'download doom fiber'); const item = game.osWorld(machine, 'pc').downloads[0]; assert.ok(item); game.executePCCommand(machine.id, `pause ${item.id}`); assert.equal(item.status, 'paused'); game.executePCCommand(machine.id, `resume ${item.id}`); assert.equal(item.status, 'downloading');
  completeDownload(game, machine, 'pc', item.id); game.osWorldInstall(machine.id, 'pc', item.id); assert.ok(game.executePCCommand(machine.id, 'play doom').lines[0].includes('FPS')); assert.equal(game.executePCCommand(machine.id, 'plan eco').lines[0], 'Power plan: eco'); assert.ok(game.executePCCommand(machine.id, 'files').lines.length >= 2);
});

test('Server repository: поиск среди реальных 2 046 пакетов и OS compatibility', () => {
  const { game, server } = freshServer(); const nginx = game.osWorldServerPackages(server.id, 'nginx'); assert.ok(nginx.some(pkg => pkg.id === 'nginx')); assert.equal(game.osWorldPackage('server', 'nginx').serviceId, 'nginx');
  const docker = game.osWorldServerPackages(server.id, 'docker'); assert.ok(docker.some(pkg => pkg.id === 'docker')); assert.equal(game.osWorldServerPackages(server.id, 'definitely-no-such-package').length, 0);
  server.os = 'windows2016'; assert.ok(game.osWorldServerPackages(server.id, '', true).every(pkg => DATA.software(pkg.id).os.includes('windows2016')));
});

test('Server download/install вызывает настоящий installSoftware и занимает RAM/CPU', () => {
  const { game, server } = freshServer(); const result = game.osWorldStartDownload(server.id, 'server', 'nginx', 'fiber'); assert.equal(result.ok, true); completeDownload(game, server, 'server', result.download.id);
  const before = game.metrics(server).cpuLoad; assert.equal(game.osWorldInstall(server.id, 'server', result.download.id).ok, true); assert.ok(server.software.includes('nginx')); assert.equal(result.download.status, 'installed'); assert.ok(game.metrics(server).cpuLoad > before); assert.ok(game.hourlyServerRevenue(server) > 0);
});

test('Service stop/start/restart сохраняет installed ID и реально меняет load/revenue', () => {
  const { game, server } = freshServer(); server.software = ['nginx']; const activeLoad = game.metrics(server).cpuLoad; const activeRevenue = game.hourlyServerRevenue(server);
  assert.equal(game.osWorldService(server.id, 'nginx', 'stop'), true); assert.ok(server.software.includes('nginx')); assert.ok(game.osWorld(server, 'server').stoppedServices.includes('nginx'));
  assert.equal(game.hourlyServerRevenue(server), 0); assert.ok(game.metrics(server).cpuLoad < activeLoad); assert.equal(game.osWorldProcesses(server, 'server').find(process => process.id === 'nginx').status, 'stopped');
  assert.equal(game.osWorldService(server.id, 'nginx', 'restart'), true); assert.equal(game.osWorld(server, 'server').stoppedServices.length, 0); assert.equal(game.hourlyServerRevenue(server), activeRevenue); assert.ok(game.metrics(server).cpuLoad >= activeLoad); assert.equal(game.osWorld(server, 'server').stats.serviceRestarts, 1);
});

test('Старый uninstall синхронизирует package status и не удаляет скачанные МБ', () => {
  const { game, server } = freshServer(); const item = game.osWorldStartDownload(server.id, 'server', 'nginx', 'fiber').download; completeDownload(game, server, 'server', item.id); game.osWorldInstall(server.id, 'server', item.id); game.osWorldService(server.id, 'nginx', 'stop');
  assert.equal(game.uninstallSoftware(server.id, 'nginx'), true); assert.equal(item.status, 'complete'); assert.ok(!game.osWorld(server, 'server').stoppedServices.includes('nginx')); assert.equal(game.osWorldStorage(server, 'server').packagesMb, item.totalMb);
  assert.equal(game.osWorldInstall(server.id, 'server', item.id).ok, true); assert.ok(server.software.includes('nginx'));
});

test('OS/RAID lifecycle очищает phantom stopped flags, но сохраняет packages/files', () => {
  const { game, server } = freshServer(); const item = game.osWorldStartDownload(server.id, 'server', 'nginx', 'fiber').download; completeDownload(game, server, 'server', item.id); game.osWorldInstall(server.id, 'server', item.id); game.osWorldService(server.id, 'nginx', 'stop'); const file = game.osWorldSaveFile(server.id, 'server', null, 'keep.txt', 'preserve');
  server.powered = false; const raid = Object.values(DATA.RAIDS).find(profile => profile.minDisks <= server.parts.disks.length && (profile.id !== 'raid10' || server.parts.disks.length % 2 === 0)); assert.ok(raid); assert.equal(game.configureRAID(server.id, raid.id), true);
  assert.equal(server.software.length, 0); assert.equal(game.osWorld(server, 'server').stoppedServices.length, 0); assert.equal(item.status, 'complete'); assert.equal(game.osWorld(server, 'server').files.find(entry => entry.id === file.id).content, 'preserve');
});

test('Server Shell: packages/fetch/downloads/stop/start/restart/services', () => {
  const { game, server } = freshServer(); assert.ok(game.executeCommand(server.id, 'packages nginx').lines.some(line => line.includes('nginx'))); game.executeCommand(server.id, 'fetch nginx fiber'); const item = game.osWorld(server, 'server').downloads[0];
  assert.ok(game.executeCommand(server.id, 'downloads').lines[0].includes(item.id)); completeDownload(game, server, 'server', item.id); game.osWorldInstall(server.id, 'server', item.id);
  assert.ok(game.executeCommand(server.id, 'stop nginx').lines[0].includes('OK')); assert.ok(game.executeCommand(server.id, 'services').lines[0].includes('stopped')); assert.ok(game.executeCommand(server.id, 'sudo start nginx').lines[0].includes('OK')); assert.ok(game.executeCommand(server.id, 'restart nginx').lines[0].includes('OK'));
});

test('Server files/browser/processes и network outage остаются per-server', () => {
  const { game, server } = freshServer(); const file = game.osWorldSaveFile(server.id, 'server', null, 'motd', 'offline'); assert.equal(file.path, '/home/root/motd'); game.osWorldNavigate(server.id, 'server', 'repo.local'); assert.equal(game.osWorld(server, 'server').browser.url, 'repo.local');
  const item = game.osWorldStartDownload(server.id, 'server', 'nginx', 'fiber').download; game.state.outageDay = game.state.day; game.osWorldAdvance(server, 10, 'server'); assert.equal(item.progressMb, 0); assert.equal(game.osWorldEffectiveSpeed(server, 'server', item), 0);
  game.state.outageDay = 0; game.osWorldAdvance(server, 1, 'server'); assert.ok(item.progressMb > 0); assert.ok(game.osWorldProcesses(server, 'server').some(process => process.kind === 'download'));
});

test('Save/reload: progress, installed games, FPS, power plan, stopped services и файлы', () => {
  const { game, machine } = freshPC(); game.osWorldNavigate(machine.id, 'pc', 'games.local'); game.osWorldPowerPlan(machine.id, 'eco'); const file = game.osWorldSaveFile(machine.id, 'pc', null, 'save.txt', 'persist');
  const pending = game.osWorldStartDownload(machine.id, 'pc', 'quake', 'normal').download; game.osWorldAdvance(machine, 1, 'pc'); const initialProgress = pending.progressMb; const doom = game.osWorldStartDownload(machine.id, 'pc', 'doom', 'fiber').download; completeDownload(game, machine, 'pc', doom.id); const progress = pending.progressMb; assert.ok(progress > initialProgress); game.osWorldInstall(machine.id, 'pc', doom.id); const result = game.osWorldPlayGame(machine.id, 'doom');
  game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); const restored = game.getPC(machine.id); assert.equal(restored.osWorld.browser.url, 'games.local'); assert.equal(restored.osWorld.powerPlan, 'eco'); assert.equal(restored.osWorld.downloads.find(item => item.id === pending.id).progressMb, progress); assert.equal(restored.osWorld.games.doom.lastFps, result.fps); assert.equal(restored.osWorld.files.find(item => item.id === file.id).content, 'persist');
});

test('Большой 70 GB package: resumable progress, capacity accounting и завершение', () => {
  const { game, machine } = freshPC(); const item = game.osWorldStartDownload(machine.id, 'pc', 'cyberpunk', 'fiber').download; const expected = game.osWorldEffectiveSpeed(machine, 'pc', item) / 8 * 10; game.osWorldAdvance(machine, 10, 'pc'); assert.equal(item.progressMb, expected); game.osWorldDownloadAction(machine.id, 'pc', item.id, 'pause'); game.osWorldAdvance(machine, 10, 'pc'); assert.equal(item.progressMb, expected);
  game.osWorldDownloadAction(machine.id, 'pc', item.id, 'resume'); completeDownload(game, machine, 'pc', item.id); assert.equal(item.status, 'complete'); assert.equal(item.progressMb, 70000); assert.equal(game.osWorldStorage(machine, 'pc').packagesMb, 70000);
});

test('Laptop/desktop/server baseline и процедурный audio wrapper не удалены', () => {
  assert.equal(typeof PCSoundscape, 'function'); assert.equal(PCSoundscape.prototype.worldAudioLoaded, true); assert.equal(typeof ServerSoundscape, 'function'); assert.equal(DATA.PC_BENCHMARKS && Object.keys(DATA.PC_BENCHMARKS).length, 11);
  const { game, machine } = freshPC(2005); assert.equal(machine.osWorld.family, 'pc'); assert.equal(game.pcTuning(machine).fan, 'balanced'); assert.ok(machine.apps && Array.isArray(machine.benchmarks ? Object.values(machine.benchmarks) : []));
  const serverGame = new Game(false); assert.ok(serverGame.state.servers[0].osWorld); assert.equal(serverGame.state.servers[0].osWorld.family, 'server');
});

console.log(`\n${passed}/${passed} OS World tests passed`);
