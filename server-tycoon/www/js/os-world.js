/* OS World 8.3: браузер, загрузки, файлы, процессы, игры и службы на каждой машине. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !DATA.OS_WORLD_GAMES || Game.prototype.osWorldLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(Number(value) || 0);
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').trim();
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const validFamily = family => family === 'server' ? 'server' : 'pc';
  const downloadStatuses = ['downloading','paused','complete','installed','canceled'];
  const dayFor = (game, family) => family === 'server' ? game.state.day : game.state.pc.day;
  const yearFor = (game, family) => family === 'server' ? game.state.currentYear : game.state.pc.year;
  const machineFor = (game, id, family) => family === 'server' ? game.getServer(id) : game.getPC(id);
  const usable = machine => Boolean(machine && machine.powered && machine.os && !machine.crashed && !machine.hung && !machine.broken);

  function defaultFiles(machine, family) {
    if (family === 'server') return [
      { id: 'file_hostname', name: 'hostname', path: '/etc/hostname', content: `${machine.name}\n`, revision: 0 },
      { id: 'file_services', name: 'services.conf', path: '/etc/os-world/services.conf', content: '# Управление службами: Services app или start/stop/restart в Shell\n', revision: 0 },
      { id: 'file_readme', name: 'readme.txt', path: '/home/root/readme.txt', content: 'OS World offline. Browser, Packages, Downloads, Files и Processes управляют этим сервером.\n', revision: 0 }
    ];
    return [
      { id: 'file_readme', name: 'README.TXT', path: 'C:/README.TXT', content: 'OS WORLD 8.3\r\nОткройте Browser → games.local, скачайте игру и запустите её из Games.\r\n', revision: 0 },
      { id: 'file_config', name: 'OSWORLD.INI', path: 'C:/OSWORLD.INI', content: '[network]\r\nmode=offline-simulation\r\n[power]\r\nplan=balanced\r\n', revision: 0 }
    ];
  }

  proto.osWorld = function (machine, family = 'pc') {
    family = validFamily(family); if (!machine) return null;
    if (!machine.osWorld || typeof machine.osWorld !== 'object') machine.osWorld = {};
    const state = machine.osWorld; state.version = 1; state.family = family;
    state.powerPlan = DATA.OS_WORLD_POWER_PLANS[state.powerPlan] ? state.powerPlan : 'balanced';
    state.defaultSpeed = DATA.OS_WORLD_SPEEDS[state.defaultSpeed] ? state.defaultSpeed : 'normal';
    if (!state.browser || typeof state.browser !== 'object') state.browser = {};
    const home = family === 'server' ? 'home.local' : 'home.local'; state.browser.url = String(state.browser.url || home).slice(0, 120);
    state.browser.back = Array.isArray(state.browser.back) ? state.browser.back.map(String).slice(-30) : [];
    state.browser.forward = Array.isArray(state.browser.forward) ? state.browser.forward.map(String).slice(-30) : [];
    state.bookmarks = Array.isArray(state.bookmarks) ? [...new Set(state.bookmarks.map(String))].slice(0, 20) : [home];
    state.downloads = Array.isArray(state.downloads) ? state.downloads.filter(item => item && this.osWorldPackage(family, item.packageId)).map(item => {
      const pkg = this.osWorldPackage(family, item.packageId); item.id = String(item.id || uid('download')); item.packageId = pkg.id; item.family = family;
      item.totalMb = pkg.sizeMb; item.progressMb = clamp(Number(item.progressMb) || 0, 0, pkg.sizeMb); item.speed = DATA.OS_WORLD_SPEEDS[item.speed] ? item.speed : 'normal';
      item.status = downloadStatuses.includes(item.status) ? item.status : 'paused'; item.actualMbps = Math.max(0, Number(item.actualMbps) || 0);
      item.createdDay = Math.max(1, Number(item.createdDay) || dayFor(this, family)); item.completedDay = Math.max(0, Number(item.completedDay) || 0);
      if (item.progressMb >= item.totalMb && ['downloading','paused'].includes(item.status)) { item.progressMb = item.totalMb; item.status = 'complete'; }
      if (item.status === 'installed') item.progressMb = item.totalMb;
      return item;
    }) : [];
    if (!state.games || typeof state.games !== 'object') state.games = {};
    Object.keys(state.games).forEach(id => {
      if (!DATA.OS_WORLD_GAMES.some(game => game.id === id)) { delete state.games[id]; return; }
      const game = state.games[id] || {}; state.games[id] = game; game.running = Boolean(game.running); game.launches = Math.max(0, Number(game.launches) || 0);
      game.lastFps = Math.max(0, Number(game.lastFps) || 0); game.bestFps = Math.max(0, Number(game.bestFps) || 0); game.lastPassed = Boolean(game.lastPassed);
      game.lastDay = Math.max(0, Number(game.lastDay) || 0);
    });
    state.files = Array.isArray(state.files) ? state.files.filter(file => file && file.id).slice(0, 24).map(file => {
      file.name = String(file.name || 'untitled.txt').replace(/[\\/]/g, '_').slice(0, 60); file.path = String(file.path || file.name).slice(0, 140);
      file.content = String(file.content || '').slice(0, 100000); file.revision = Math.max(0, Number(file.revision) || 0); return file;
    }) : defaultFiles(machine, family);
    /* Не фильтруем по software здесь: metrics() временно скрывает остановленные ID. */
    state.stoppedServices = Array.isArray(state.stoppedServices) ? [...new Set(state.stoppedServices.filter(id => family === 'server' && typeof id === 'string'))] : [];
    if (!state.stats || typeof state.stats !== 'object') state.stats = {};
    ['downloadsCompleted','gamesLaunched','serviceRestarts'].forEach(key => { state.stats[key] = Math.max(0, Number(state.stats[key]) || 0); });
    return state;
  };

  proto.osWorldPackage = function (family, id) {
    family = validFamily(family);
    if (family === 'pc') return DATA.OS_WORLD_GAMES.find(game => game.id === id) || null;
    const app = DATA.software(id); if (!app) return null;
    return { id: app.id, name: app.name, year: Number(app.year) || 1990, icon: app.icon || 'apps', kind: 'service', serviceId: app.id,
      sizeMb: Math.max(20, round(40 + (Number(app.ram) || 0) * 128 + (Number(app.cpu) || 0) * 2)), description: app.description || '', minRam: app.minRam || 0 };
  };
  proto.osWorldServerPackages = function (serverId, query = '', compatibleOnly = true) {
    const server = this.getServer(serverId); if (!server) return []; const text = normalize(query); const year = this.state.currentYear;
    return DATA.SOFTWARE.filter(app => {
      if (compatibleOnly && server.os && !app.os.includes(server.os)) return false;
      if ((Number(app.year) || 1990) > year) return false;
      if (text && !normalize(`${app.name} ${app.id} ${app.description} ${app.category || ''}`).includes(text)) return false;
      return true;
    }).map(app => this.osWorldPackage('server', app.id)).sort((a, b) => a.name.localeCompare(b.name, 'ru'));
  };
  proto.osWorldStorage = function (machine, family = 'pc') {
    family = validFamily(family); const state = this.osWorld(machine, family); const os = machine.os ? (family === 'server' ? DATA.os(machine.os) : this.pcOS(machine.os)) : null;
    const capacityMb = family === 'server' ? Math.max(0, Number(this.raidCapacity(machine)) || 0) * 1024 : Math.max(0, Number(machine.storageMb) || 0);
    const systemMb = os ? (family === 'server' ? os.minDisk * 1024 : os.minStorageMb) : 0;
    const packagesMb = state.downloads.filter(item => item.status !== 'canceled').reduce((sum, item) => sum + item.totalMb, 0);
    const filesMb = state.files.reduce((sum, file) => sum + String(file.content).length / 1048576, 0);
    const usedMb = systemMb + packagesMb + filesMb;
    return { capacityMb, systemMb, packagesMb, filesMb, usedMb, freeMb: Math.max(0, capacityMb - usedMb), overMb: Math.max(0, usedMb - capacityMb) };
  };
  proto.osWorldNavigate = function (machineId, family, url, direction = '') {
    family = validFamily(family); const machine = machineFor(this, machineId, family); const state = this.osWorld(machine, family); if (!state) return null;
    const sites = DATA.OS_WORLD_SITES[family]; let target = String(url || state.browser.url).trim().toLowerCase().replace(/^https?:\/\//, '').replace(/\/$/, '').slice(0, 120) || 'home.local';
    if (direction === 'back' && state.browser.back.length) { state.browser.forward.push(state.browser.url); target = state.browser.back.pop(); }
    else if (direction === 'forward' && state.browser.forward.length) { state.browser.back.push(state.browser.url); target = state.browser.forward.pop(); }
    else if (!direction && target !== state.browser.url) { state.browser.back.push(state.browser.url); state.browser.back = state.browser.back.slice(-30); state.browser.forward = []; }
    state.browser.url = target; this.save();
    const site = sites[target]; return { url: target, known: Boolean(site), title: site ? site.title : 'Offline · сайт не найден', description: site ? site.description : 'Внешняя сеть не используется. Выберите локальный адрес.' };
  };
  proto.osWorldBookmark = function (machineId, family) {
    family = validFamily(family); const state = this.osWorld(machineFor(this, machineId, family), family); if (!state) return false;
    const url = state.browser.url; const index = state.bookmarks.indexOf(url); if (index >= 0) state.bookmarks.splice(index, 1); else state.bookmarks.push(url);
    this.save(); return index < 0;
  };
  proto.osWorldStartDownload = function (machineId, family, packageId, speed = 'normal') {
    family = validFamily(family); const machine = machineFor(this, machineId, family); const state = this.osWorld(machine, family); const pkg = this.osWorldPackage(family, packageId);
    if (!machine || !state || !pkg) return { ok: false }; if (!usable(machine)) { this.notify('Запустите ОС без аварий для загрузки', 'warning'); return { ok: false }; }
    if (family === 'pc' && machine.origin !== 'owned') { this.notify('Игры можно устанавливать только на собственные ПК', 'warning'); return { ok: false }; }
    const unlock = family === 'pc' ? machine && this.state.pc.cheats && this.state.pc.cheats.unlockAll : this.state.cheats && this.state.cheats.unlockAll;
    if (pkg.year > yearFor(this, family) && !unlock) { this.notify(`Пакет доступен с ${pkg.year} года`, 'warning'); return { ok: false }; }
    if (family === 'server' && !DATA.software(packageId).os.includes(machine.os)) { this.notify('Пакет несовместим с этой ОС', 'error'); return { ok: false }; }
    const existing = state.downloads.find(item => item.packageId === packageId && item.status !== 'canceled');
    if (existing) { this.notify('Этот пакет уже есть в Download Manager', 'warning'); return { ok: false, download: existing }; }
    const storage = this.osWorldStorage(machine, family); if (storage.freeMb < pkg.sizeMb) { this.notify(`Нужно ${pkg.sizeMb} МБ свободного места`, 'error'); return { ok: false, storage }; }
    speed = DATA.OS_WORLD_SPEEDS[speed] ? speed : 'normal'; state.defaultSpeed = speed;
    const download = { id: uid('download'), packageId, family, status: 'downloading', progressMb: 0, totalMb: pkg.sizeMb, speed, actualMbps: DATA.OS_WORLD_SPEEDS[speed].mbps, createdDay: dayFor(this, family), completedDay: 0 };
    state.downloads.push(download); this.notify(`${pkg.name}: загрузка начата · ${DATA.OS_WORLD_SPEEDS[speed].name}`, 'success'); this.save(); return { ok: true, download, package: pkg };
  };
  proto.osWorldDownloadAction = function (machineId, family, downloadId, action, value) {
    family = validFamily(family); const machine = machineFor(this, machineId, family); const state = this.osWorld(machine, family); const item = state && state.downloads.find(download => download.id === downloadId);
    if (!item) return false;
    if (action === 'speed' && DATA.OS_WORLD_SPEEDS[value] && !['complete','installed','canceled'].includes(item.status)) { item.speed = value; state.defaultSpeed = value; }
    else if (action === 'pause' && item.status === 'downloading') item.status = 'paused';
    else if (action === 'resume' && item.status === 'paused' && usable(machine)) item.status = 'downloading';
    else if (action === 'cancel' && ['downloading','paused','complete'].includes(item.status)) { item.status = 'canceled'; item.actualMbps = 0; }
    else return false;
    this.save(); return true;
  };
  proto.osWorldEffectiveSpeed = function (machine, family, item) {
    const base = DATA.OS_WORLD_SPEEDS[item.speed] ? DATA.OS_WORLD_SPEEDS[item.speed].mbps : 1;
    if (family === 'server') { if (this.state.outageDay === this.state.day) return 0; const network = this.network(); return Math.min(base, Math.max(1, Number(network.speed) || base)); }
    const factor = machine.storageType === 'NVMe' ? 1.35 : machine.storageType === 'SSD' ? 1.15 : .85; return base * factor;
  };
  proto.osWorldAdvance = function (machine, realSeconds, family = 'pc') {
    family = validFamily(family); const state = this.osWorld(machine, family); if (!state || !usable(machine)) return [];
    const seconds = clamp(Number(realSeconds) || 0, 0, 86400); const completed = [];
    state.downloads.forEach(item => {
      if (item.status !== 'downloading') return; const mbps = this.osWorldEffectiveSpeed(machine, family, item); item.actualMbps = mbps; if (mbps <= 0 || seconds <= 0) return;
      item.progressMb = Math.min(item.totalMb, item.progressMb + mbps / 8 * seconds);
      if (item.progressMb >= item.totalMb) { item.progressMb = item.totalMb; item.status = 'complete'; item.completedDay = dayFor(this, family); item.actualMbps = 0; state.stats.downloadsCompleted += 1; completed.push(item); }
    });
    if (completed.length) { completed.forEach(item => this.notify(`${this.osWorldPackage(family, item.packageId).name}: загрузка завершена, можно установить`, 'success')); this.save(); }
    return completed;
  };
  proto.osWorldInstall = function (machineId, family, downloadId) {
    family = validFamily(family); const machine = machineFor(this, machineId, family); const state = this.osWorld(machine, family); const item = state && state.downloads.find(download => download.id === downloadId);
    if (!item || item.status !== 'complete' || !usable(machine)) return { ok: false }; const pkg = this.osWorldPackage(family, item.packageId);
    if (family === 'server') {
      if (!machine.software.includes(pkg.serviceId) && !this.installSoftware(machine.id, pkg.serviceId)) return { ok: false };
      item.status = 'installed'; this.log(`${machine.name}: package ${pkg.name} установлен через OS World.`, 'good');
    } else {
      if (machine.origin !== 'owned') return { ok: false }; const previous = state.games[pkg.id] || {};
      state.games[pkg.id] = Object.assign(previous, { running: false, launches: previous.launches || 0, lastFps: previous.lastFps || 0, bestFps: previous.bestFps || 0 });
      item.status = 'installed'; this.pcLog(`${machine.name}: ${pkg.name} установлена из offline package.`, 'good');
    }
    this.notify(`${pkg.name}: установлено`, 'success'); this.save(); return { ok: true, download: item, package: pkg };
  };
  proto.osWorldPlayGame = function (machineId, gameId) {
    const machine = this.getPC(machineId); const state = this.osWorld(machine, 'pc'); const game = state && state.games[gameId];
    if (!machine || !game || !DATA.OS_WORLD_GAMES.some(item => item.id === gameId)) { this.notify('Сначала скачайте и установите игру', 'warning'); return false; }
    const result = this.runPCBenchmark(machineId, gameId); if (!result) return false;
    game.launches += 1; game.lastFps = result.fps; game.bestFps = Math.max(game.bestFps || 0, result.fps); game.lastPassed = result.passed; game.lastDay = this.state.pc.day; game.running = machine.powered && !machine.crashed;
    state.stats.gamesLaunched += 1; this.save(); return result;
  };
  proto.osWorldStopGame = function (machineId, gameId) { const state = this.osWorld(this.getPC(machineId), 'pc'); if (!state || !state.games[gameId]) return false; state.games[gameId].running = false; this.save(); return true; };
  proto.osWorldPowerPlan = function (machineId, plan) {
    const machine = this.getPC(machineId); if (!machine || !DATA.OS_WORLD_POWER_PLANS[plan]) return false; this.osWorld(machine, 'pc').powerPlan = plan; this.pcLog(`${machine.name}: power plan ${plan}.`); this.save(); return true;
  };
  proto.osWorldSaveFile = function (machineId, family, fileId, name, content) {
    family = validFamily(family); const machine = machineFor(this, machineId, family); const state = this.osWorld(machine, family); if (!state) return false;
    let file = state.files.find(item => item.id === fileId); const cleanName = String(name || 'untitled.txt').trim().replace(/[\\/]/g, '_').slice(0, 60) || 'untitled.txt';
    if (!file) { if (state.files.length >= 24) { this.notify('Достигнут лимит 24 файла', 'warning'); return false; } file = { id: uid('file'), path: family === 'server' ? `/home/root/${cleanName}` : `C:/USERS/ADMIN/${cleanName}`, revision: 0 }; state.files.push(file); }
    file.name = cleanName; file.content = String(content || '').slice(0, 100000); file.revision += 1; this.save(); return file;
  };
  proto.osWorldService = function (serverId, serviceId, action) {
    const server = this.getServer(serverId); if (!server || !server.software.includes(serviceId) || !DATA.software(serviceId)) return false; const state = this.osWorld(server, 'server');
    if (action === 'stop') { if (!state.stoppedServices.includes(serviceId)) state.stoppedServices.push(serviceId); }
    else if (['start','restart'].includes(action)) { state.stoppedServices = state.stoppedServices.filter(id => id !== serviceId); if (action === 'restart') state.stats.serviceRestarts += 1; }
    else return false;
    server.ramPressure = 0; this.log(`${server.name}: ${DATA.software(serviceId).name} · ${action}.`); this.notify(`${DATA.software(serviceId).name}: ${action === 'stop' ? 'остановлено' : action === 'restart' ? 'перезапущено' : 'запущено'}`, 'success'); this.save(); return true;
  };
  proto.osWorldProcesses = function (machine, family = 'pc') {
    family = validFamily(family); const state = this.osWorld(machine, family); if (!state || !machine.os) return []; const metrics = family === 'server' ? this.metrics(machine) : this.pcMetrics(machine);
    const processes = [{ id: 'kernel', pid: 100 + hash(machine.id) % 800, name: family === 'server' ? DATA.os(machine.os).name : this.pcOS(machine.os).name, kind: 'system', status: 'running', cpu: round(metrics.cpuLoad * .25), ramMb: family === 'server' ? round(metrics.ramUsed * 250) : round(metrics.ramUsedMb * .55) }];
    state.downloads.filter(item => ['downloading','paused'].includes(item.status)).forEach(item => processes.push({ id: item.id, pid: 1000 + hash(item.id) % 28000, name: this.osWorldPackage(family, item.packageId).name, kind: 'download', status: item.status, cpu: item.status === 'downloading' ? Math.max(1, round(item.actualMbps / 20)) : 0, ramMb: 24 }));
    if (family === 'pc') Object.entries(state.games).filter(([, game]) => game.running).forEach(([id, game]) => processes.push({ id, pid: 2000 + hash(id) % 26000, name: DATA.PC_BENCHMARKS[id].name, kind: 'game', status: 'running', cpu: 18, ramMb: DATA.PC_BENCHMARKS[id].ramMb, fps: game.lastFps }));
    else [...new Set(machine.software)].forEach(id => { const app = DATA.software(id); if (app) processes.push({ id, pid: 3000 + hash(id) % 24000, name: app.name, kind: 'service', status: state.stoppedServices.includes(id) ? 'stopped' : 'running', cpu: state.stoppedServices.includes(id) ? 0 : app.cpu, ramMb: state.stoppedServices.includes(id) ? 0 : round(app.ram * 1024) }); });
    return processes;
  };

  const baseWorldCreatePC = proto.createPC;
  proto.createPC = function (...args) { const machine = baseWorldCreatePC.apply(this, args); if (machine) this.osWorld(machine, 'pc'); return machine; };

  const baseWorldNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseWorldNormalize.call(this);
    if (this.state.pc) { this.state.pc.computers.forEach(machine => this.osWorld(machine, 'pc')); this.state.pc.offers.forEach(offer => { if (offer.pc) this.osWorld(offer.pc, 'pc'); }); }
    if (Array.isArray(this.state.servers)) this.state.servers.forEach(server => this.osWorld(server, 'server'));
    return result;
  };
  function syncWorldSoftware(game, server) {
    if (!server) return; const state = game.osWorld(server, 'server'); const installed = new Set(server.software || []);
    state.stoppedServices = state.stoppedServices.filter(id => installed.has(id));
    state.downloads.forEach(item => { if (item.status === 'installed' && !installed.has(item.packageId)) item.status = 'complete'; });
  }
  const baseWorldFinishOS = proto.finishOSInstall;
  proto.finishOSInstall = function (...args) { const result = baseWorldFinishOS.apply(this, args); if (result) { syncWorldSoftware(this, this.getServer(args[0])); this.save(); } return result; };
  const baseWorldRAID = proto.configureRAID;
  proto.configureRAID = function (...args) { const result = baseWorldRAID.apply(this, args); if (result) { syncWorldSoftware(this, this.getServer(args[0])); this.save(); } return result; };
  const baseWorldStopStress = proto.stopStress;
  proto.stopStress = function (...args) { const result = baseWorldStopStress.apply(this, args); if (result) { syncWorldSoftware(this, this.getServer(args[0])); this.save(); } return result; };

  const baseWorldUninstall = proto.uninstallSoftware;
  proto.uninstallSoftware = function (serverId, softwareId) {
    const result = baseWorldUninstall.call(this, serverId, softwareId); const server = this.getServer(serverId);
    if (result && server) { const state = this.osWorld(server, 'server'); state.stoppedServices = state.stoppedServices.filter(id => id !== softwareId); state.downloads.forEach(item => { if (item.packageId === softwareId && item.status === 'installed') item.status = 'complete'; }); this.save(); }
    return result;
  };

  const baseWorldPCMetrics = proto.pcMetrics;
  proto.pcMetrics = function (machine) {
    const result = baseWorldPCMetrics.call(this, machine); if (!machine || !machine.powered) return result; const state = this.osWorld(machine, 'pc'); const plan = DATA.OS_WORLD_POWER_PLANS[state.powerPlan];
    const games = Object.values(state.games).filter(game => game.running).length; const downloads = state.downloads.filter(item => item.status === 'downloading').length;
    result.cpuLoad = clamp(result.cpuLoad + games * 16 + downloads * 2, 0, 100); result.gpuLoad = clamp(result.gpuLoad + games * 28, 0, 100);
    result.watts = result.watts * plan.watts + games * 24 + downloads * 3; result.targetTemp += plan.temp + games * 2; result.noise = clamp(result.noise * (plan.id === 'eco' ? .9 : plan.id === 'performance' ? 1.08 : 1) + games, 0, 96);
    return result;
  };
  const baseWorldScores = proto.pcPerformanceScores;
  proto.pcPerformanceScores = function (machine) {
    const result = baseWorldScores.call(this, machine); if (!machine || !result) return result; const state = this.osWorld(machine, 'pc'); const plan = DATA.OS_WORLD_POWER_PLANS[state.powerPlan];
    return Object.assign({}, result, { cpu: result.cpu * plan.score, gpu: result.gpu * plan.score });
  };
  const baseWorldBenchmark = proto.runPCBenchmark;
  proto.runPCBenchmark = function (id, benchmarkId) { const result = baseWorldBenchmark.call(this, id, benchmarkId); const machine = this.getPC(id); if (result && machine) result.powerPlan = this.osWorld(machine, 'pc').powerPlan; return result; };
  function withActiveServices(game, server, callback) {
    const stopped = new Set(game.osWorld(server, 'server').stoppedServices); if (!stopped.size || !Array.isArray(server.software)) return callback();
    const original = server.software; server.software = original.filter(id => !stopped.has(id)); try { return callback(); } finally { server.software = original; }
  }
  const baseWorldServerMetrics = proto.metrics;
  proto.metrics = function (server) { return withActiveServices(this, server, () => baseWorldServerMetrics.call(this, server)); };
  const baseWorldRevenue = proto.hourlyServerRevenue;
  proto.hourlyServerRevenue = function (server) { return withActiveServices(this, server, () => baseWorldRevenue.call(this, server)); };
  const baseWorldPCTick = proto.tickPC;
  proto.tickPC = function (realSeconds = 1) { const result = baseWorldPCTick.call(this, realSeconds); if (this.state.pc && this.state.pc.started && !this.state.paused) this.state.pc.computers.forEach(machine => this.osWorldAdvance(machine, realSeconds, 'pc')); return result; };
  const baseWorldTick = proto.tick;
  proto.tick = function () { const result = baseWorldTick.call(this); if (this.state.campaignMode !== 'pc' && !this.state.paused && Array.isArray(this.state.servers)) this.state.servers.forEach(server => this.osWorldAdvance(server, 1, 'server')); return result; };

  const baseWorldPCCommand = proto.executePCCommand;
  proto.executePCCommand = function (id, input) {
    const raw = String(input || '').trim(); const parts = raw.split(/\s+/).filter(Boolean); const command = (parts.shift() || '').toLowerCase(); const machine = this.getPC(id);
    const ok = lines => ({ ok: true, lines: Array.isArray(lines) ? lines : [lines] });
    if (command === 'help') { const result = baseWorldPCCommand.call(this, id, input); result.lines.push('store · downloads · download <game> [slow|normal|fast|fiber] · play <game> · stopgame <game>', 'plan <eco|balanced|performance> · files · pause/resume/cancel <downloadId>'); return result; }
    if (command === 'store') return ok(DATA.OS_WORLD_GAMES.map(game => `${game.id} · ${game.name} · ${game.sizeMb} МБ · ${game.year}`));
    if (command === 'downloads') { const state = this.osWorld(machine, 'pc'); return ok(state.downloads.length ? state.downloads.map(item => `${item.id} · ${item.packageId} · ${item.status} · ${round(item.progressMb)}/${item.totalMb} МБ`) : ['Загрузок нет']); }
    if (command === 'download') { const result = this.osWorldStartDownload(id, 'pc', parts[0], parts[1] || 'normal'); return ok(result.ok ? `${parts[0]}: download started` : 'Download rejected'); }
    if (['pause','resume','cancel'].includes(command)) return ok(this.osWorldDownloadAction(id, 'pc', parts[0], command) ? `${parts[0]}: ${command}` : 'Download action rejected');
    if (command === 'play') { const result = this.osWorldPlayGame(id, parts[0]); return ok(result ? `${parts[0]}: ${result.fps} FPS · ${result.passed ? 'PASS' : 'FAIL'}` : 'Game is not installed or cannot start'); }
    if (command === 'stopgame') return ok(this.osWorldStopGame(id, parts[0]) ? `${parts[0]} stopped` : 'Game is not running');
    if (command === 'plan') return ok(this.osWorldPowerPlan(id, parts[0]) ? `Power plan: ${parts[0]}` : 'Unknown power plan');
    if (command === 'files') return ok(this.osWorld(machine, 'pc').files.map(file => `${file.path} · ${file.content.length} chars · rev ${file.revision}`));
    return baseWorldPCCommand.call(this, id, input);
  };
  const baseWorldServerCommand = proto.executeCommand;
  proto.executeCommand = function (serverId, input) {
    const raw = String(input || '').trim(); const parts = raw.split(/\s+/).filter(Boolean); let command = (parts.shift() || '').toLowerCase(); if (command === 'sudo') command = (parts.shift() || '').toLowerCase();
    const server = this.getServer(serverId); const ok = lines => ({ ok: true, lines: Array.isArray(lines) ? lines : [lines] });
    if (['start','stop','restart'].includes(command) && parts[0]) return ok(this.osWorldService(serverId, parts[0], command) ? `${parts[0]}: ${command} OK` : `${parts[0]}: service not installed`);
    if (command === 'packages' || command === 'store') return ok(this.osWorldServerPackages(serverId, parts.join(' '), true).slice(0, 30).map(pkg => `${pkg.id} · ${pkg.name} · ${pkg.sizeMb} МБ`));
    if (command === 'downloads') { const state = this.osWorld(server, 'server'); return ok(state.downloads.length ? state.downloads.map(item => `${item.id} · ${item.packageId} · ${item.status} · ${round(item.progressMb)}/${item.totalMb} МБ`) : ['Package downloads: пусто']); }
    if (command === 'fetch') { const result = this.osWorldStartDownload(serverId, 'server', parts[0], parts[1] || 'normal'); return ok(result.ok ? `${parts[0]}: fetch started` : 'Fetch rejected'); }
    if (command === 'services') { const state = this.osWorld(server, 'server'); const ids = [...new Set(server.software)]; return ok(ids.length ? ids.map(id => `${id} · ${DATA.software(id).name} · ${state.stoppedServices.includes(id) ? 'stopped' : 'active'}`) : ['Активных сервисов нет']); }
    if (command === 'help') { const result = baseWorldServerCommand.call(this, serverId, input); result.lines.push('packages/store · fetch <id> [speed] · downloads · start/stop/restart <serviceId>'); return result; }
    return baseWorldServerCommand.call(this, serverId, input);
  };
  proto.osWorldLoaded = true;
})();
