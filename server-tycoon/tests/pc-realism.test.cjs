/* Real Hardware & OS 8.4: node tests/pc-realism.test.cjs */
'use strict';
const assert = require('node:assert/strict');
global.window = global;
for (const name of ['icons','brands','pc-branding','data','expansion','mega','pc-data','pc-architect-data','pc-laptop-data','os-world-data','pc-realism-data','audio','long-lofi','pc-sound','pc-world-sound','game','systems','lab','server-sound','timeline','deep-lab','market-search','avito-years','pc-workshop','pc-architect','pc-laptop','pc-parts-market','pc-plus','os-world','pc-realism','debug','cheats']) require(`../www/js/${name}.js`);
const Game = global.ServerTycoonGame; let passed = 0;
function test(name,fn) { fn(); passed += 1; console.log(`✓ ${name}`); }
function fresh(year = 2026) { const game = new Game(false); game.startPCCampaign(); game.state.pc.year = year; game.state.pc.money = 1e9; game.events.length = 0; return game; }
function addPC(game,year = 2007,os = 'pc-winxp',options = {}) { const machine = game.createPC(year,Object.assign({ origin:'owned',owner:'test',os:null },options)); game.state.pc.computers.push(machine); if (os) assert.equal(game.installPCOS(machine.id,os,Boolean(options.experimental)),true); return machine; }
function buyModel(game,id) { const model = DATA.PREBUILT_MOBILE_INDEX.get(id); const listing = game.pcFindPrebuiltListings(model.name)[0]; assert.ok(listing,`listing ${id}`); const result = game.buyPrebuiltMobile(listing.id); assert.equal(result.ok,true); return result.machine; }
function completeDownload(game,machine,item) { const speed = game.osWorldEffectiveSpeed(machine,'pc',item); game.osWorldAdvance(machine,item.totalMb / (speed / 8) + 1,'pc'); assert.equal(item.status,'complete'); }
function downloadAndInstall(game,machine,packageId) { const result = game.osWorldStartDownload(machine.id,'pc',packageId,'fiber'); assert.equal(result.ok,true,packageId); completeDownload(game,machine,result.download); return game.osWorldInstall(machine.id,'pc',result.download.id); }

test('Каталоги 8.4 аддитивны: 58 ОС, 46 benchmarks, 57 packages и 31 prebuilt', () => {
  assert.equal(DATA.PC_OS.length,58); assert.equal(DATA.PC_REALISM_OS.length,45); assert.equal(Object.keys(DATA.PC_BENCHMARKS).length,46); assert.equal(Object.keys(DATA.PC_REALISM_BENCHMARKS).length,35);
  assert.equal(DATA.OS_WORLD_GAMES.length,57); assert.equal(DATA.PREBUILT_MOBILE_CATALOG.length,31); assert.equal(DATA.PC_PART_CATALOG.length,20000); assert.equal(DATA.LAPTOP_PART_CATALOG.length,4440); assert.equal(DATA.SOFTWARE.length,2046);
});
test('Прежние 13 OS IDs, 11 benchmark IDs и 10 game packages сохраняют позиции', () => {
  ['pc-dos33','pc-win95','pc-winxp','pc-win7','pc-win10','pc-win11','pc-ubuntu2404'].forEach(id => assert.ok(DATA.PC_OS.find(os => os.id === id)));
  ['stability','doom','quake','cs16','crysis','cyberpunk'].forEach(id => assert.ok(DATA.PC_BENCHMARKS[id])); assert.deepEqual(DATA.OS_WORLD_GAMES.slice(0,10).map(pkg => pkg.id),['doom','quake','halflife','cs16','gta3','doom3','crysis','minecraft','witcher3','cyberpunk']);
});
test('ASUS Eee PC 900 — маленький слабый нетбук с реальными 900 МГц', () => {
  const model = DATA.PREBUILT_MOBILE_INDEX.get('eee-900'); assert.ok(model.legendary); assert.equal(model.kind,'netbook'); assert.ok(model.cpu.includes('900 MHz')); assert.equal(model.cpuScore,48); assert.equal(model.displaySize,8.9); assert.ok(model.weightKg < 1); assert.equal(model.cpuUpgradeable,false); assert.equal(model.upgrade,undefined);
});
test('Mobile Авито: typed search, kind/year/price filters и разные цены', () => {
  const game = fresh(); const eee = game.pcFindPrebuiltListings('ASUS Eee PC 900'); assert.equal(eee.length,1); assert.equal(eee[0].modelId,'eee-900');
  const netbooks = game.pcFindPrebuiltListings('',{ kind:'netbook',year:'2008',minPrice:'1',maxPrice:'100000',sort:'priceAsc' }); assert.ok(netbooks.length >= 6); assert.ok(netbooks.every(item => item.kind === 'netbook' && item.year === 2008)); assert.ok(new Set(netbooks.map(item => item.price)).size > 2);
  assert.deepEqual(netbooks.map(item => item.price),netbooks.map(item => item.price).slice().sort((a,b) => a - b));
});
test('Покупка prebuilt — транзакция: машина, батарея, деньги, sold guard', () => {
  const game = fresh(); const model = DATA.PREBUILT_MOBILE_INDEX.get('eee-900'); const listing = game.pcFindPrebuiltListings(model.name)[0]; const money = game.state.pc.money;
  const result = game.buyPrebuiltMobile(listing.id); assert.equal(result.ok,true); assert.equal(game.state.pc.money,money - listing.price); assert.equal(result.machine.prebuilt,true); assert.equal(result.machine.laptop,true); assert.equal(result.machine.custom,false); assert.equal(result.machine.battery.capacityWh,32); assert.ok(game.state.pc.computers.includes(result.machine));
  const after = game.state.pc.money; assert.equal(game.buyPrebuiltMobile(listing.id).ok,false); assert.equal(game.state.pc.money,after); assert.equal(game.state.pc.stats.prebuiltBought,1);
  assert.equal(result.machine.osBasePenalty,1); assert.ok(Math.abs(result.machine.osPenalty - game.pcOSPerformance(result.machine).speedFactor) < 1e-9);
});
test('Hardware Inspector работает для custom, generation, job и dumpster ПК', () => {
  const game = fresh(); const customReport = game.pcAutoDraft(2020); const custom = game.buildCustomPC({ free:true }).machine; const generation = addPC(game,2007); const job = addPC(game,2005,'pc-winxp',{ origin:'job',owner:'client' }); const dumpster = addPC(game,1998,null,{ owner:'Находка с мусорки',name:'Выброшенный ПК' });
  assert.ok(game.pcHardware(custom).length >= 8); for (const machine of [generation,job,dumpster]) { const parts = game.pcHardware(machine); assert.ok(parts.length >= 6); assert.ok(parts.some(part => part.type === 'cpu')); assert.ok(parts.some(part => part.type === 'storage')); }
});
test('Universal tuning доступен не только custom и сохраняет PSU guard', () => {
  const game = fresh(); const job = addPC(game,2007,'pc-winxp',{ origin:'job' }); assert.equal(game.setPCTuning(job.id,'cpu',10),true); assert.equal(game.setPCTuning(job.id,'gpu',10),true); assert.equal(game.setPCTuning(job.id,'fan','turbo'),true); assert.equal(job.tuningRevision,3); assert.ok(game.pcPerformanceScores(job).cpu > job.cpuScore);
  const before = Object.assign({},job.tuning); job.psuWatts = 1; assert.equal(game.setPCTuning(job.id,'cpu',20),false); assert.deepEqual(job.tuning,before);
});
test('Eee RAM upgrade требует отключить адаптер и соблюдает 1 slot / 2 ГБ', () => {
  const game = fresh(); const machine = buyModel(game,'eee-900'); const options = game.pcPrebuiltUpgradeOptions(machine,'ram'); assert.ok(options.length > 0); assert.ok(options.every(part => part.capacityMb <= 2048 && part.modules === 1 && part.ramType === 'DDR2'));
  assert.equal(game.upgradePrebuiltMobile(machine.id,'ram',options[0].id).ok,false); assert.equal(machine.ramMb,1024); game.toggleLaptopAdapter(machine.id); const result = game.upgradePrebuiltMobile(machine.id,'ram',options[0].id); assert.equal(result.ok,true); assert.equal(machine.ramMb,options[0].capacityMb); assert.equal(machine.upgradeHistory.length,1); assert.equal(machine.prebuiltParts.ram,options[0].id);
});
test('Eee storage upgrade увеличивает диск и сохраняет установленную ОС', () => {
  const game = fresh(); const machine = buyModel(game,'eee-900'); const os = machine.os; game.toggleLaptopAdapter(machine.id); const options = game.pcPrebuiltUpgradeOptions(machine,'storage'); assert.ok(options.length > 0); const result = game.upgradePrebuiltMobile(machine.id,'storage',options[0].id); assert.equal(result.ok,true); assert.ok(machine.storageMb > 16384); assert.equal(machine.os,os); assert.equal(machine.upgradeHistory[0].type,'storage');
});
test('BGA CPU upgrade отклоняется до денег и изменения машины', () => {
  const game = fresh(); const machine = buyModel(game,'eee-900'); game.toggleLaptopAdapter(machine.id); const part = DATA.LAPTOP_PART_CATALOG.find(item => item.type === 'cpu'); const money = game.state.pc.money; const before = machine.cpu;
  assert.equal(game.pcPrebuiltUpgradeOptions(machine,'cpu').length,0); assert.equal(game.upgradePrebuiltMobile(machine.id,'cpu',part.id).ok,false); assert.equal(machine.cpu,before); assert.equal(game.state.pc.money,money); assert.equal(machine.upgradeHistory.length,0);
});
test('Soldered RAM/storage заблокированы, а Framework RAM остаётся сменной', () => {
  const game = fresh(); const soldered = buyModel(game,'asus-l210'); assert.equal(game.pcPrebuiltUpgradeOptions(soldered,'ram').length,0); assert.equal(game.pcPrebuiltUpgradeOptions(soldered,'storage').length,0);
  assert.equal(game.pcHardware(soldered).find(part => part.type === 'ram').upgradeable,false); assert.equal(game.pcHardware(soldered).find(part => part.type === 'storage').upgradeable,false);
  const framework = buyModel(game,'framework13'); assert.ok(game.pcPrebuiltUpgradeOptions(framework,'ram').length > 0); assert.ok(game.pcPrebuiltUpgradeOptions(framework,'storage').length > 0); assert.equal(game.pcPrebuiltUpgradeOptions(framework,'cpu').length,0);
});
test('Socket G2 на ThinkPad X220 разрешает реальный CPU upgrade по TDP', () => {
  const game = fresh(); const machine = buyModel(game,'thinkpad-x220'); const options = game.pcPrebuiltUpgradeOptions(machine,'cpu'); assert.ok(options.length > 0); assert.ok(options.every(part => part.socket === 'Socket G2' && part.tdp <= 35 && part.cpuScore > machine.cpuScore)); game.toggleLaptopAdapter(machine.id); const old = machine.cpuScore; const result = game.upgradePrebuiltMobile(machine.id,'cpu',options[0].id); assert.equal(result.ok,true); assert.ok(machine.cpuScore > old);
});
test('Owned SO-DIMM со склада расходуется вместо денег ровно один раз', () => {
  const game = fresh(); const machine = buyModel(game,'eee-900'); const part = game.pcPrebuiltUpgradeOptions(machine,'ram')[0]; game.state.pc.componentInventory.push({ uid:'owned-ram-test',catalogId:part.id,family:'laptop',type:'ram',paid:100,health:90 }); game.toggleLaptopAdapter(machine.id); const money = game.state.pc.money;
  const result = game.upgradePrebuiltMobile(machine.id,'ram',part.id); assert.equal(result.ok,true); assert.equal(result.usedInventory,true); assert.equal(result.cost,0); assert.equal(game.state.pc.money,money); assert.equal(game.state.pc.componentInventory.some(item => item.uid === 'owned-ram-test'),false);
});
test('OS speed зависит от железа: Win11 на Eee загружается часами', () => {
  const game = fresh(); const ee = buyModel(game,'eee-900'); const desktop = addPC(game,2007); const xpEe = game.pcOSPerformance(Object.assign({},ee,{ os:'pc-winxp' })); const xpDesktop = game.pcOSPerformance(desktop); const heavy = game.pcOSPerformance(Object.assign({},ee,{ os:'pc-win11' }));
  assert.ok(xpEe.bootSeconds > xpDesktop.bootSeconds * 2); assert.ok(heavy.bootSeconds >= 7000); assert.equal(heavy.installSeconds,14400); assert.ok(heavy.speedFactor < xpEe.speedFactor); assert.ok(heavy.responsiveness < 1);
});
test('USB setup начинается с media → writing → BIOS, а не ставит ОС мгновенно', () => {
  const game = fresh(2008); const machine = addPC(game,2007,null); const previous = machine.os; const result = game.startPCOSInstallation(machine.id,'pc-winxp'); assert.equal(result.ok,true); assert.equal(machine.os,previous); assert.equal(machine.osInstall.phase,'media');
  assert.equal(game.pcOSInstallationAction(machine.id,'write'),true); assert.equal(machine.osInstall.phase,'writing'); game.advancePCOSInstallation(machine.id,machine.osInstall.mediaSeconds); assert.equal(machine.osInstall.phase,'bios'); assert.equal(machine.os,null); assert.equal(game.pcOSInstallationAction(machine.id,'usb'),true); assert.equal(machine.osInstall.phase,'copy'); assert.equal(machine.osInstall.elapsedSeconds,0);
});
test('Долгий режим: одно нажатие пропускает строго 10 секунд', () => {
  const game = fresh(2008); game.state.pc.speed = 1; const machine = addPC(game,2007,null); game.startPCOSInstallation(machine.id,'pc-winxp'); game.pcOSInstallationAction(machine.id,'write'); game.advancePCOSInstallation(machine.id,999); game.pcOSInstallationAction(machine.id,'usb'); game.pcOSInstallationAction(machine.id,'skip'); assert.equal(machine.osInstall.elapsedSeconds,10); assert.equal(machine.osInstall.phase,'copy'); game.pcOSInstallationAction(machine.id,'skip'); assert.equal(machine.osInstall.elapsedSeconds,20);
});
test('Сюжетный и быстрый режимы: одно нажатие пропускает 600 секунд', () => {
  for (const speed of [15,60]) { const game = fresh(2008); game.state.pc.speed = speed; const machine = addPC(game,2007,null); game.startPCOSInstallation(machine.id,'pc-winxp'); game.pcOSInstallationAction(machine.id,'write'); game.advancePCOSInstallation(machine.id,999); game.pcOSInstallationAction(machine.id,'usb'); game.pcOSInstallationAction(machine.id,'skip'); assert.equal(machine.osInstall.elapsedSeconds,600); assert.equal(machine.osInstall.phase,'copy'); }
});
test('Setup идёт на game tick в фоне и останавливается на pause', () => {
  const game = fresh(2008); const machine = addPC(game,2007,null); game.startPCOSInstallation(machine.id,'pc-winxp'); game.pcOSInstallationAction(machine.id,'write'); game.tickPC(1); assert.equal(machine.osInstall.mediaElapsed,1); game.state.paused = true; game.tickPC(5); assert.equal(machine.osInstall.mediaElapsed,1); game.state.paused = false; game.advancePCOSInstallation(machine.id,999); game.pcOSInstallationAction(machine.id,'usb'); game.tickPC(3); assert.equal(machine.osInstall.elapsedSeconds,3);
});
test('Полный USB setup завершает установку, списывает лицензию и сохраняет report', () => {
  const game = fresh(2008); game.state.pc.money = 5000; const machine = addPC(game,2007,null); game.startPCOSInstallation(machine.id,'pc-winxp'); game.pcOSInstallationAction(machine.id,'write'); game.advancePCOSInstallation(machine.id,999); game.pcOSInstallationAction(machine.id,'usb'); const total = machine.osInstall.totalSeconds; game.advancePCOSInstallation(machine.id,total); assert.equal(machine.osInstall.phase,'complete'); assert.equal(machine.os,'pc-winxp'); assert.equal(game.state.pc.money,3800); assert.equal(machine.lastOSInstall.totalSeconds,total); assert.ok(machine.osBootSeconds > 0);
});
test('Два независимых installer state продвигаются раздельно и переживают normalize', () => {
  const game = fresh(2008); const a = addPC(game,2007,null); const b = addPC(game,2006,null); for (const machine of [a,b]) { game.startPCOSInstallation(machine.id,'pc-winxp'); game.pcOSInstallationAction(machine.id,'write'); } game.tickPC(2); assert.equal(a.osInstall.mediaElapsed,2); assert.equal(b.osInstall.mediaElapsed,2); game.advancePCOSInstallation(a.id,999); assert.equal(a.osInstall.phase,'bios'); assert.equal(b.osInstall.phase,'writing'); const aid = a.id; game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); assert.equal(game.getPC(aid).osInstall.phase,'bios');
});
test('2007 desktop: DOOM 105 FPS и CS 1.6 180 FPS, а не 20 FPS', () => {
  const game = fresh(); const machine = addPC(game,2007); machine.powered = true; const doom = game.runPCBenchmark(machine.id,'doom'); const cs = game.runPCBenchmark(machine.id,'cs16'); assert.ok(doom.fps >= 90 && doom.fps <= 120,`${doom.fps}`); assert.equal(doom.fps,105); assert.ok(cs.fps > 100); assert.equal(cs.fps,180); assert.equal(doom.frameCap,120); assert.ok(doom.onePercentLow > 60);
});
test('FPS сохраняет hardware scaling и emulator frame cap 60', () => {
  const game = fresh(); const retro = addPC(game,2007); const modern = addPC(game,2020,'pc-win11'); retro.powered = modern.powered = true; const low = game.runPCBenchmark(retro.id,'pcsx2'); const high = game.runPCBenchmark(modern.id,'pcsx2'); assert.ok(high.fps > low.fps); assert.ok(high.fps <= 60); assert.equal(high.frameCap,60); assert.equal(high.api,'emulator');
});
test('Тяжёлая ОС реально снижает скорость/FPS на одинаковом слабом железе', () => {
  const game = fresh(); const light = addPC(game,2007,'pc-winxp'); const heavy = addPC(game,2007,'pc-win11',{ experimental:true }); light.powered = heavy.powered = true; assert.ok(heavy.osPenalty < light.osPenalty); assert.ok(game.runPCBenchmark(heavy.id,'cs16').fps < game.runPCBenchmark(light.id,'cs16').fps); assert.ok(game.pcMetrics(heavy).osBootSeconds > game.pcMetrics(light).osBootSeconds);
  assert.ok(game.osWorldEffectiveSpeed(heavy,'pc',{ speed:'normal' }) < game.osWorldEffectiveSpeed(light,'pc',{ speed:'normal' }));
});
test('Новая DOS-игра на Win11 требует DOSBox; после установки запускается', () => {
  const game = fresh(); const machine = addPC(game,2020,'pc-win11'); machine.powered = true; machine.storageMb = 500000; const download = game.osWorldStartDownload(machine.id,'pc','doom2','fiber'); completeDownload(game,machine,download.download); const rejected = game.osWorldInstall(machine.id,'pc',download.download.id); assert.equal(rejected.ok,false); assert.ok(rejected.reasons[0].includes('DOSBox'));
  assert.equal(downloadAndInstall(game,machine,'dosbox').ok,true); const second = game.osWorldDownloadAction(machine.id,'pc',download.download.id,'cancel'); assert.equal(second,true); assert.equal(downloadAndInstall(game,machine,'doom2').ok,true); assert.ok(game.osWorldPlayGame(machine.id,'doom2').fps > 0);
});
test('Windows-игра на Linux требует Wine, который затем открывает совместимость', () => {
  const game = fresh(); const machine = addPC(game,2020,'pc-ubuntu2404'); machine.powered = true; machine.storageMb = 500000; assert.equal(game.pcPackageCompatibility(machine,'hl2').ok,false); assert.ok(game.pcPackageCompatibility(machine,'hl2').reasons[0].includes('Wine')); assert.equal(downloadAndInstall(game,machine,'wine').ok,true); assert.equal(game.pcPackageCompatibility(machine,'hl2').ok,true);
});
test('Windows XP: modern.local закрыт до Supermium и открыт после установки', () => {
  const game = fresh(); const machine = addPC(game,2007,'pc-winxp'); machine.powered = true; machine.storageMb = 500000; assert.equal(game.pcBrowserInfo(machine).level,8); assert.equal(game.osWorldNavigate(machine.id,'pc','modern.local').compatible,false);
  const result = downloadAndInstall(game,machine,'supermium'); assert.equal(result.ok,true); const browser = game.pcBrowserInfo(machine); assert.equal(browser.source,'supermium'); assert.equal(browser.level,132); assert.equal(browser.modern,true); assert.equal(game.osWorldNavigate(machine.id,'pc','modern.local').compatible,true); assert.equal(game.osWorldNavigate(machine.id,'pc','newest-web.local').compatible,true);
});
test('Chrome 109 несовместим с XP, а Supermium специально совместим', () => {
  const game = fresh(); const machine = addPC(game,2007,'pc-winxp'); machine.powered = true; assert.equal(game.pcPackageCompatibility(machine,'chrome109').ok,false); assert.ok(game.pcPackageCompatibility(machine,'chrome109').reasons.some(reason => reason.includes('2009'))); assert.equal(game.pcPackageCompatibility(machine,'supermium').ok,true); assert.equal(game.osWorldPlayGame(machine.id,'supermium'),false);
});
test('Старые 10 games сохраняют прежнюю установку без нового DOSBox requirement', () => {
  const game = fresh(); const machine = addPC(game,2020,'pc-win11'); machine.powered = true; machine.storageMb = 500000; assert.equal(downloadAndInstall(game,machine,'doom').ok,true); assert.ok(game.osWorldPlayGame(machine.id,'doom').fps > 0);
});
test('Prebuilt upgrades/tuning/setup сериализуются без потери старых saves', () => {
  const game = fresh(); const machine = buyModel(game,'eee-900'); game.toggleLaptopAdapter(machine.id); const ram = game.pcPrebuiltUpgradeOptions(machine,'ram')[0]; game.upgradePrebuiltMobile(machine.id,'ram',ram.id); game.setPCTuning(machine.id,'fan','silent'); game.startPCOSInstallation(machine.id,'pc-win7starter'); const id = machine.id; const oldServer = game.state.servers[0].id;
  game.state = JSON.parse(JSON.stringify(game.state)); game.normalize(); const restored = game.getPC(id); assert.equal(restored.ramMb,ram.capacityMb); assert.equal(restored.tuning.fan,'silent'); assert.equal(restored.prebuiltModelId,'eee-900'); assert.equal(restored.osInstall.phase,'media'); assert.equal(game.state.servers[0].id,oldServer); assert.ok(game.osWorld(restored,'pc'));
});
test('Legacy installPCOS остаётся мгновенным совместимым API', () => {
  const game = fresh(); const machine = addPC(game,2007,null); assert.equal(game.installPCOS(machine.id,'pc-winxp'),true); assert.equal(machine.os,'pc-winxp'); assert.equal(machine.osInstall,undefined);
});
test('Музыка громче: default 0.58, output gain ×1.25, mute сохранён', () => {
  const game = fresh(); const audio = new TycoonAudio(game); assert.equal(audio.settings().volume,.58); assert.equal(audio.outputVolume(),.725); audio.setVolume(0); assert.equal(audio.outputVolume(),0); audio.setVolume(1); assert.equal(audio.outputVolume(),1);
});
console.log(`\n${passed}/${passed} Real Hardware & OS 8.4 tests passed`);
