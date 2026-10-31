/* Control Room 4.0: Local AI, настоящие стресс-нагрузки, команды ОС и звук стойки. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !window.DATA || Game.prototype.labLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(value);
  const hash = value => {
    let result = 2166136261;
    for (const char of String(value)) result = Math.imul(result ^ char.charCodeAt(0), 16777619);
    return result >>> 0;
  };

  DATA.AI_MODELS = {
    tiny05: { id: 'tiny05', name: 'Qwen 0.5B · Q4', size: '0,4 ГБ', minRam: 1, ram: .65, cpu: 14, rate: 65, coreRate: 1, description: 'Самый лёгкий вход в Local AI · CPU' },
    llama3b: { id: 'llama3b', name: 'Llama 3.2 · 3B Q4', size: '2,0 ГБ', minRam: 4, ram: 2.6, cpu: 30, rate: 210, coreRate: 2, description: 'Баланс для домашнего сервера' },
    qwen7b: { id: 'qwen7b', name: 'Qwen 2.5 · 7B Q4', size: '4,7 ГБ', minRam: 8, ram: 5.4, cpu: 52, rate: 390, coreRate: 3, description: 'Заметно умнее · GPU ускоряет доход' },
    qwen14b: { id: 'qwen14b', name: 'Qwen 2.5 · 14B Q4', size: '9,0 ГБ', minRam: 16, ram: 10, cpu: 72, rate: 560, coreRate: 4, description: 'Тяжёлая локальная модель' },
    heavy70: { id: 'heavy70', name: 'Ollama 70B · Classic', size: '40 ГБ', minRam: 32, ram: 20, cpu: 90, rate: 780, coreRate: 4, description: 'Баланс версии 3.0 · для старых saves' },
    llama70: { id: 'llama70', name: 'Llama 3.1 · 70B Q4', size: '42 ГБ', minRam: 48, ram: 42, cpu: 96, rate: 980, coreRate: 5, description: 'Максимальная нагрузка RAM/CPU/GPU' }
  };
  DATA.STRESS_TESTS = {
    cpu: { id: 'cpu', name: 'CPU Burn ×2', icon: 'chip', apps: ['folding', 'folding'], appOverride: { folding: { ram: 0, gpuBoost: false } }, description: 'Два CPU-worker в общей памяти: 200% спроса, нагрев и троттлинг без RAM Avalanche.' },
    ram: { id: 'ram', name: 'RAM Avalanche', icon: 'ram', apps: ['virtuallab', 'virtuallab', 'virtuallab', 'virtuallab'], description: '+48 ГБ занятой памяти: на слабой машине наступит настоящий OOM.' },
    gpu: { id: 'gpu', name: 'GPU Inferno', icon: 'gpu', apps: ['stablediffusion', 'folding'], description: 'Stable Diffusion и Folding: CPU/GPU, питание, шум и температура.' },
    chaos: { id: 'chaos', name: '1990 Chaos Protocol', icon: 'warning', apps: ['folding', 'folding', 'stablediffusion', 'stablediffusion', 'virtuallab', 'virtuallab', 'virtuallab', 'ollama'], aiModel: 'heavy70', description: '70B + VM + наука + SD. Дохода нет: только наблюдение за аварией.' }
  };
  const ollama = DATA.software('ollama');
  if (ollama) {
    ollama.name = 'Ollama · Local AI';
    ollama.description = 'Выбор модели от 0,5B до 70B · доход зависит от варианта';
  }

  function aiModel(server) {
    return DATA.AI_MODELS[server.aiModel] || DATA.AI_MODELS.tiny05;
  }
  function withAI(server, callback) {
    const app = DATA.software('ollama');
    if (!app || !server || (!server.software.includes('ollama') && !server.aiModel)) return callback();
    const model = aiModel(server);
    const keys = ['ram', 'cpu', 'rate', 'minRam', 'coreRate', 'gpuBoost'];
    const saved = {};
    keys.forEach(key => { saved[key] = app[key]; });
    Object.assign(app, { ram: model.ram, cpu: model.cpu, rate: model.rate, minRam: model.minRam, coreRate: model.coreRate, gpuBoost: true });
    try { return callback(); } finally { Object.assign(app, saved); }
  }

  function withWorkload(server, callback) {
    return withAI(server, () => {
      const profile = server && server.stress && DATA.STRESS_TESTS[server.stress.id];
      const overrides = profile && profile.appOverride;
      if (!overrides) return callback();
      const saved = [];
      Object.entries(overrides).forEach(([id, values]) => {
        const app = DATA.software(id);
        if (app) Object.entries(values).forEach(([key, value]) => {
          saved.push([app, key, app[key]]);
          app[key] = value;
        });
      });
      try { return callback(); } finally { saved.reverse().forEach(([app, key, value]) => { app[key] = value; }); }
    });
  }

  const baseNormalizeServer = proto.normalizeServer;
  proto.normalizeServer = function (server) {
    const result = baseNormalizeServer.call(this, server);
    if (!Object.prototype.hasOwnProperty.call(server, 'aiModel')) server.aiModel = server.software.includes('ollama') ? 'heavy70' : null;
    if (server.aiModel && !DATA.AI_MODELS[server.aiModel]) server.aiModel = server.software.includes('ollama') ? 'tiny05' : null;
    if (server.stress && (!DATA.STRESS_TESTS[server.stress.id] || !Array.isArray(server.stress.added))) server.stress = null;
    if (!server.stress) server.stress = null;
    return result;
  };

  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseNormalize.call(this);
    this.state.version = 6;
    this.state.settings = Object.assign({ serverSounds: false, serverVolume: .28 }, this.state.settings || {});
    this.state.stats.experiments = this.state.stats.experiments || 0;
    return result;
  };

  const baseInstantiate = proto.instantiateServer;
  proto.instantiateServer = function (templateId, condition = 'working') {
    const server = baseInstantiate.call(this, templateId, condition);
    server.aiModel = null;
    server.stress = null;
    return server;
  };
  const baseCreateServer = proto.createServer;
  proto.createServer = function (chassisUid) {
    const server = baseCreateServer.call(this, chassisUid);
    if (server) { server.aiModel = null; server.stress = null; }
    return server;
  };

  const baseMetrics = proto.metrics;
  proto.metrics = function (server) {
    return withWorkload(server, () => baseMetrics.call(this, server));
  };
  const baseHourly = proto.hourlyServerRevenue;
  proto.hourlyServerRevenue = function (server) {
    if (server.stress) return 0;
    return withWorkload(server, () => baseHourly.call(this, server));
  };

  const baseInstallSoftware = proto.installSoftware;
  proto.installSoftware = function (serverId, softwareId, modelId) {
    const server = this.getServer(serverId);
    if (!server || softwareId !== 'ollama') return baseInstallSoftware.call(this, serverId, softwareId);
    const previous = server.aiModel;
    server.aiModel = DATA.AI_MODELS[modelId] ? modelId : (server.aiModel || 'tiny05');
    const result = withAI(server, () => baseInstallSoftware.call(this, serverId, softwareId));
    if (!result) server.aiModel = previous;
    return result;
  };

  proto.installAIModel = function (serverId, modelId) {
    const server = this.getServer(serverId);
    const model = DATA.AI_MODELS[modelId];
    if (!server || !model) return false;
    if (!server.powered || !server.os || server.hung || server.crashed) {
      this.notify('Local AI: сервер должен быть запущен', 'warning');
      return false;
    }
    if (server.stress) {
      this.notify('Сначала остановите Torture Lab', 'warning');
      return false;
    }
    if (!ollama || !ollama.os.includes(server.os)) {
      this.notify('Эта ОС не поддерживает Ollama', 'error');
      return false;
    }
    const ramTotal = this.hardwareReport(server).ramTotal;
    if (ramTotal < model.minRam) {
      this.notify(`${model.name}: нужно минимум ${model.minRam} ГБ ОЗУ`, 'error');
      return false;
    }
    if (!server.software.includes('ollama')) return this.installSoftware(serverId, 'ollama', modelId);
    server.aiModel = modelId;
    server.ramPressure = 0;
    this.log(`${server.name}: загружена модель ${model.name} (${model.size}).`, 'good');
    this.notify(`${model.name} готова к inference`, 'success');
    this.save();
    return true;
  };

  const baseUninstallSoftware = proto.uninstallSoftware;
  proto.uninstallSoftware = function (serverId, softwareId) {
    const result = baseUninstallSoftware.call(this, serverId, softwareId);
    const server = this.getServer(serverId);
    if (server && softwareId === 'ollama' && !server.software.includes('ollama') && !server.stress) server.aiModel = null;
    return result;
  };

  const baseFinishOS = proto.finishOSInstall;
  proto.finishOSInstall = function (serverId, usbUid) {
    const result = baseFinishOS.call(this, serverId, usbUid);
    const server = this.getServer(serverId);
    if (result && server) { server.aiModel = null; server.stress = null; }
    return result;
  };
  const baseRAID = proto.configureRAID;
  proto.configureRAID = function (serverId, raidId) {
    const result = baseRAID.call(this, serverId, raidId);
    const server = this.getServer(serverId);
    if (result && server) { server.aiModel = null; server.stress = null; }
    return result;
  };

  proto.startStress = function (serverId, profileId) {
    const server = this.getServer(serverId);
    const profile = DATA.STRESS_TESTS[profileId];
    if (!server || !profile) return false;
    if (!server.powered || !server.os || server.crashed || server.hung) {
      this.notify('Для эксперимента нужен работающий сервер с ОС', 'warning');
      return false;
    }
    if (server.stress) {
      this.notify('Сначала остановите текущий эксперимент', 'warning');
      return false;
    }
    const previousAiModel = server.aiModel;
    const addedOllama = profileId === 'chaos' && !server.software.includes('ollama');
    if (profile.aiModel) server.aiModel = profile.aiModel;
    server.software.push(...profile.apps);
    server.stress = { id: profileId, added: profile.apps.slice(), startedDay: this.state.day, previousAiModel, addedOllama };
    server.ramPressure = 0;
    this.state.stats.experiments = (this.state.stats.experiments || 0) + 1;
    this.log(`${server.name}: TORTURE LAB · ${profile.name}. Доход остановлен.`, 'bad');
    this.notify(`${profile.name}: эксперимент запущен`, 'warning');
    this.save();
    return true;
  };

  proto.stopStress = function (serverId) {
    const server = this.getServer(serverId);
    if (!server || !server.stress) return false;
    const stress = server.stress;
    stress.added.forEach(id => {
      const index = server.software.lastIndexOf(id);
      if (index >= 0) server.software.splice(index, 1);
    });
    server.aiModel = stress.previousAiModel || null;
    if (!server.software.includes('ollama')) server.aiModel = null;
    server.stress = null;
    server.ramPressure = 0;
    this.log(`${server.name}: эксперимент остановлен, добавленные процессы сняты.`, 'good');
    this.notify('Torture Lab остановлен', 'success');
    this.save();
    return true;
  };

  proto.restartServer = function (serverId) {
    const server = this.getServer(serverId);
    if (!server || server.broken || server.crashed) {
      this.notify('Сначала устраните аппаратную аварию', 'error');
      return false;
    }
    if (server.hung) {
      server.hung = false;
      server.powered = false;
      server.ramPressure = 0;
    } else if (server.powered) {
      this.powerOff(server);
    }
    this.log(`${server.name}: команда перезагрузки из ОС.`, 'info');
    return this.togglePower(serverId);
  };

  proto.executeCommand = function (serverId, input) {
    const server = this.getServer(serverId);
    if (!server) return { ok: false, lines: ['Сервер не найден'] };
    const raw = String(input || '').trim();
    if (!raw) return { ok: true, lines: [] };
    if (!server.powered || !server.os || server.crashed || server.hung) return { ok: false, lines: ['Система недоступна: проверьте питание и состояние ОС'] };
    let parts = raw.split(/\s+/);
    let command = parts.shift().toLowerCase();
    if (command === 'sudo') { command = (parts.shift() || '').toLowerCase(); }
    if (['apt', 'apt-get', 'dnf', 'yum', 'pkg'].includes(command)) command = (parts.shift() || '').toLowerCase();
    if (command === 'systemctl') command = parts.join(' ').includes('status') ? 'status' : 'services';
    if (command === 'docker' && parts[0] === 'ps') command = 'services';
    if (command === 'service' && parts[1] === 'start') { command = 'install'; parts = [parts[0]]; }
    const aliases = {
      '?': 'help', htop: 'top', 'get-process': 'services', 'get-service': 'services', 'get-ciminstance': 'status',
      'restart-computer': 'reboot', 'stop-computer': 'poweroff', shutdown: 'poweroff', 'get-psdrive': 'df',
      journalctl: 'logs', 'ip': 'network', ifconfig: 'network', ls: 'services', dir: 'services', ver: 'uname'
    };
    command = aliases[command] || command;
    const m = this.metrics(server);
    const memory = value => value < 1 ? `${round(value * 1024)} МБ` : `${value.toFixed(1)} ГБ`;
    const os = DATA.os(server.os);
    const ok = lines => ({ ok: true, lines: Array.isArray(lines) ? lines : [lines] });
    const fail = lines => ({ ok: false, lines: Array.isArray(lines) ? lines : [lines] });
    switch (command) {
      case 'help': return ok(['Команды: status · top · services · free · df · sensors · network · logs', 'install <id> · remove <id> · ai [модель] · stress [cpu|ram|gpu|chaos|stop]', 'reboot · poweroff · clear · exit. Команды управляют этой игровой машиной.']);
      case 'clear': return { ok: true, lines: [], clear: true };
      case 'exit': return { ok: true, lines: ['Сессия завершена'], close: true };
      case 'whoami': return ok(os.family === 'windows' ? 'nt authority\\system' : 'root');
      case 'uname': return ok(`${os.name} · ${os.family} · Server Tycoon Control Room 4.0`);
      case 'echo': return ok(parts.join(' '));
      case 'status': case 'top': case 'uptime':
        return ok([`${server.name} · ${os.name}`, `CPU ${round(m.cpuLoad)}% (спрос ${round(m.cpuDemand)}%) · RAM ${memory(m.ramUsed)} / ${memory(m.ramTotal)}`, `${round(server.temp)}°C · ${round(m.watts)}W · ${round(m.noise)}дБ · ${server.throttling ? 'THROTTLING' : 'stable'}`, server.stress ? `TORTURE LAB: ${DATA.STRESS_TESTS[server.stress.id].name} · доход 0` : `Доход ${round(this.hourlyServerRevenue(server))} ₽/ч`]);
      case 'free': return ok([`Память: ${memory(m.ramUsed)} занято / ${memory(m.ramTotal)} всего`, `${round(m.ramPercent)}% · pressure ${server.ramPressure || 0}/60`]);
      case 'df': case 'zpool':
        return ok([`${server.raid || 'none'} · ${memory(this.raidCapacity(server))}`, ...server.parts.disks.map((disk, index) => `sd${String.fromCharCode(97 + index)} · ${disk.name} · ${memory(disk.capacity)}`)]);
      case 'sensors': return ok([`CPU package: ${server.temp.toFixed(1)}°C · target ${m.targetTemp.toFixed(1)}°C`, `cooling ${round(m.cooling)} · fan noise ${round(m.noise)}дБ`, server.throttling ? 'WARNING: thermal throttling' : 'thermal state: OK']);
      case 'network': case 'ip addr': { const network = this.network(); return ok([`${network.name} · ${network.speed} Мбит/с`, `SLA ${(network.reliability * 100).toFixed(2)}% · uplink ${this.state.outageDay === this.state.day ? 'DOWN' : 'UP'}`]); }
      case 'services': case 'ps': {
        const ids = [...new Set(server.software)];
        return ok(ids.length ? ids.map(id => { const app = DATA.software(id); const count = server.software.filter(item => item === id).length; return `${app ? app.name : id} · active${count > 1 ? ` ×${count}` : ''}`; }) : ['Активных сервисов нет']);
      }
      case 'logs': { const logs = this.state.logs.filter(log => log.text.includes(server.name)).slice(0, 6); return ok(logs.length ? logs.map(log => `D${log.day}: ${log.text}`) : ['Журнал этой машины пуст']); }
      case 'install': case 'remove': {
        const id = parts[0];
        const app = DATA.software(id);
        if (!app) return fail(`Пакет «${id || '?'}» не найден. Используйте ID из каталога.`);
        const result = command === 'install' ? this.installSoftware(serverId, id) : this.uninstallSoftware(serverId, id);
        return result ? ok(`${app.name}: ${command === 'install' ? 'installed / active' : 'removed'}`) : fail(`${app.name}: операция отклонена · проверьте ОС, RAM и состояние`);
      }
      case 'ai': {
        if (!parts[0]) return ok(Object.values(DATA.AI_MODELS).map(model => `${model.id} · ${model.name} · RAM ${model.minRam}+ ГБ · ${model.size}`));
        const result = this.installAIModel(serverId, parts[0]);
        return result ? ok(`Модель ${aiModel(server).name} загружена и активна`) : fail('Модель не установлена: проверьте RAM, ОС и питание');
      }
      case 'stress': {
        if (!parts[0]) return ok(Object.values(DATA.STRESS_TESTS).map(profile => `${profile.id} · ${profile.name}`));
        if (parts[0] === 'stop') return this.stopStress(serverId) ? ok('Эксперимент остановлен') : fail('Активного эксперимента нет');
        const result = this.startStress(serverId, parts[0]);
        return result ? ok(`${DATA.STRESS_TESTS[parts[0]].name}: START · доход отключён`) : fail('Эксперимент не запущен');
      }
      case 'reboot': return this.restartServer(serverId) ? ok('Перезагрузка выполнена') : fail('Перезагрузка отклонена');
      case 'poweroff': this.powerOff(server); this.save(); return ok('Питание выключено');
      default: return fail(`${command}: команда не найдена · введите help`);
    }
  };

  class ServerSoundscape {
    constructor(game, audio) {
      this.game = game;
      this.audio = audio;
      this.context = null;
      this.master = null;
      this.noiseBuffer = null;
      this.sessions = new Map();
      this.clickBudget = 0;
    }
    settings() {
      const settings = this.game.state.settings;
      settings.serverSounds = Boolean(settings.serverSounds);
      if (!Number.isFinite(settings.serverVolume)) settings.serverVolume = .28;
      return settings;
    }
    ensure() {
      if (!this.audio) return null;
      this.audio.unlocked = true;
      this.context = this.audio.ensureContext();
      if (!this.context) return null;
      if (!this.master) {
        this.master = this.context.createGain();
        this.master.connect(this.context.destination);
      }
      this.master.gain.setTargetAtTime(this.settings().serverVolume * .55, this.context.currentTime, .04);
      return this.context;
    }
    toggle() {
      const settings = this.settings();
      settings.serverSounds = !settings.serverSounds;
      if (settings.serverSounds && !this.ensure()) {
        settings.serverSounds = false;
        this.game.notify('WebAudio недоступен в этом WebView', 'error');
      }
      if (!settings.serverSounds) this.stopAll(); else this.sync();
      this.game.save();
      return settings.serverSounds;
    }
    setVolume(value) {
      const settings = this.settings();
      settings.serverVolume = clamp(Number(value) || 0, 0, 1);
      if (this.master && this.context) this.master.gain.setTargetAtTime(settings.serverVolume * .55, this.context.currentTime, .04);
      this.game.save();
    }
    profile(server) {
      const m = this.game.metrics(server);
      const seed = hash(server.id);
      const cooler = server.parts.cooler;
      const hdd = server.parts.disks.filter(part => part && part.type === 'hdd').length;
      const ssd = server.parts.disks.filter(part => part && part.type === 'ssd').length;
      const load = clamp(m.rawLoad / 100, 0, 1);
      const age = new Date().getFullYear() - (server.chassis.year || 2004);
      const detune = .9 + (seed % 220) / 1000;
      const base = (47 + (cooler ? Math.max(0, cooler.noise + 5) * 2.2 : 12) + server.chassis.coolingFactor * 9) * detune;
      return {
        fanHz: base * (.82 + load * .34), harmonicHz: hdd ? 112 + (seed % 18) : base * 2.03,
        hdd, ssd, load, age, filterHz: 360 + load * 1450 + Math.max(0, age) * 4,
        fanGain: .038 + load * .035, noiseGain: .022 + load * .042 + hdd * .004,
        pan: ((seed % 200) / 100 - 1) * .65
      };
    }
    makeNoise() {
      if (this.noiseBuffer) return this.noiseBuffer;
      const length = this.context.sampleRate;
      this.noiseBuffer = this.context.createBuffer(1, length, this.context.sampleRate);
      const data = this.noiseBuffer.getChannelData(0);
      let last = 0;
      for (let i = 0; i < length; i += 1) {
        const white = Math.random() * 2 - 1;
        last = (last + .025 * white) / 1.025;
        data[i] = last * 3.2;
      }
      return this.noiseBuffer;
    }
    createSession(server, profile) {
      const c = this.context;
      const output = c.createGain();
      const fan = c.createOscillator();
      const fanGain = c.createGain();
      const harmonic = c.createOscillator();
      const harmonicGain = c.createGain();
      const noise = c.createBufferSource();
      const filter = c.createBiquadFilter();
      const noiseGain = c.createGain();
      fan.type = 'sine'; harmonic.type = 'triangle';
      noise.buffer = this.makeNoise(); noise.loop = true;
      filter.type = 'lowpass'; filter.Q.value = .7;
      fan.connect(fanGain); fanGain.connect(output);
      harmonic.connect(harmonicGain); harmonicGain.connect(output);
      noise.connect(filter); filter.connect(noiseGain); noiseGain.connect(output);
      let panner = null;
      if (typeof c.createStereoPanner === 'function') { panner = c.createStereoPanner(); output.connect(panner); panner.connect(this.master); }
      else output.connect(this.master);
      fan.start(); harmonic.start(); noise.start();
      const session = { output, fan, fanGain, harmonic, harmonicGain, noise, filter, noiseGain, panner, seed: hash(server.id) };
      this.sessions.set(server.id, session);
      this.updateSession(session, profile);
      return session;
    }
    updateSession(session, p) {
      const now = this.context.currentTime;
      session.fan.frequency.setTargetAtTime(p.fanHz, now, .12);
      session.harmonic.frequency.setTargetAtTime(p.harmonicHz * (.82 + p.load * .25), now, .12);
      session.fanGain.gain.setTargetAtTime(p.fanGain, now, .15);
      session.harmonicGain.gain.setTargetAtTime(p.hdd ? .018 + p.load * .018 : .012, now, .15);
      session.noiseGain.gain.setTargetAtTime(p.noiseGain, now, .15);
      session.filter.frequency.setTargetAtTime(p.filterHz, now, .18);
      if (session.panner) session.panner.pan.setTargetAtTime(p.pan, now, .2);
    }
    click(session, p) {
      const c = this.context;
      const oscillator = c.createOscillator();
      const gain = c.createGain();
      oscillator.type = 'square';
      oscillator.frequency.value = 75 + (session.seed % 90);
      gain.gain.setValueAtTime(.018 + p.load * .02, c.currentTime);
      gain.gain.exponentialRampToValueAtTime(.0001, c.currentTime + .025);
      oscillator.connect(gain); gain.connect(session.output);
      oscillator.start(); oscillator.stop(c.currentTime + .03);
      oscillator.onended = () => { oscillator.disconnect(); gain.disconnect(); };
    }
    sync() {
      if (!this.settings().serverSounds || (typeof document !== 'undefined' && document.hidden)) { this.stopAll(); return; }
      if (!this.audio || !this.audio.unlocked || !this.ensure()) return;
      const active = this.game.state.servers.filter(server => server.powered && !this.game.state.powerOut);
      const alive = new Set(active.map(server => server.id));
      for (const [id, session] of this.sessions) if (!alive.has(id)) this.stopSession(id, session);
      this.clickBudget = 3;
      active.forEach(server => {
        const profile = this.profile(server);
        const session = this.sessions.get(server.id) || this.createSession(server, profile);
        this.updateSession(session, profile);
        session.output.gain.setTargetAtTime(.65 / Math.sqrt(Math.max(1, active.length)), this.context.currentTime, .15);
        if (profile.hdd && profile.load > .08 && this.clickBudget > 0 && Math.random() < .045 + profile.load * .08) {
          this.click(session, profile); this.clickBudget -= 1;
        }
      });
    }
    stopSession(id, session) {
      ['fan', 'harmonic', 'noise'].forEach(key => { try { session[key].stop(); } catch (_) { /* уже остановлен */ } });
      try { session.output.disconnect(); if (session.panner) session.panner.disconnect(); } catch (_) { /* noop */ }
      this.sessions.delete(id);
    }
    stopAll() { for (const [id, session] of this.sessions) this.stopSession(id, session); }
  }

  window.ServerSoundscape = ServerSoundscape;
  proto.labLoaded = true;
})();
