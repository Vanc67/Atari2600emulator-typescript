/* Deep Lab 6.0: измеримые протоколы, отчёты, открытия, tuning и fleet operations. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !window.DATA || Game.prototype.deepLabLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(value);
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const avg = (samples, key) => samples.length ? samples.reduce((sum, sample) => sum + (Number(sample[key]) || 0), 0) / samples.length : 0;
  const peak = (samples, key) => samples.reduce((max, sample) => Math.max(max, Number(sample[key]) || 0), 0);

  DATA.LAB_PROTOCOLS = {
    thermal: { id: 'thermal', name: 'Thermal Soak', icon: 'temp', year: 1990, duration: 8, cpu: 72,
      description: '72% дополнительной CPU-нагрузки. 8 тиков ниже 85°C.', objective: 'Пик < 85°C · без BSOD', reward: '+2 cooling на этом сервере' },
    memory: { id: 'memory', name: 'Memory Mapping', icon: 'ram', year: 1990, duration: 6, cpu: 8, memoryTarget: .96,
      description: 'Контролируемое заполнение до 96% RAM без скрытого OOM.', objective: 'Пик ≥ 80% · без зависания', reward: '−2% расхода RAM' },
    network: { id: 'network', name: 'Packet Storm', icon: 'network', year: 1990, duration: 8, cpu: 34,
      description: 'Насыщение uplink, реальные потери от CPU/RAM и аварии провайдера.', objective: 'Средний packet loss < 5%', reward: '+3% сетевого дохода' },
    storage: { id: 'storage', name: 'Disk Endurance', icon: 'hdd', year: 1990, duration: 6, cpu: 18,
      description: 'IOPS всех HDD/SSD с учётом RAID. Диски теряют 1% ресурса.', objective: 'Измерить IOPS · сохранить ОС', reward: '+3% storage factor' },
    raid: { id: 'raid', name: 'RAID Failure Drill', icon: 'raid', year: 1990, duration: 8, cpu: 24,
      description: 'Временный отказ одного диска и rebuild на избыточном массиве.', objective: 'RAID 1/5/6/10 · сохранить ёмкость', reward: '+2% storage · +2 health' },
    power: { id: 'power', name: 'PSU Transient', icon: 'bolt', year: 1990, duration: 6, cpu: 12, powerSurge: true,
      description: 'Импульсная нагрузка на БП и общую линию. Автомат может сработать по-настоящему.', objective: '6 тиков без отключения', reward: '−2% потребления' },
    overclock: { id: 'overclock', name: 'Overclock Stability', icon: 'chip', year: 1995, duration: 10, cpu: 55,
      description: 'Временный разгон минимум +20%: больше производительности, тепла и ватт.', objective: '10 тиков без BSOD', reward: 'Открывает +30% OC' },
    ai: { id: 'ai', name: 'AI Inference Bench', icon: 'gpu', year: 2016, duration: 8, cpu: 14,
      description: 'Tokens/s выбранной локальной модели, CPU/GPU и память под inference.', objective: 'Модель загружена · стабильный inference', reward: '+3% дохода Local AI' }
  };
  DATA.LAB_FAN_PROFILES = {
    silent: { id: 'silent', name: 'Silent', cooling: .88, noise: -5, watts: -3, description: '−12% cooling · −5 дБ' },
    balanced: { id: 'balanced', name: 'Balanced', cooling: 1, noise: 0, watts: 0, description: 'Заводской баланс' },
    turbo: { id: 'turbo', name: 'Turbo', cooling: 1.18, noise: 7, watts: 6, description: '+18% cooling · +7 дБ' }
  };
  DATA.LAB_ACHIEVEMENTS = [
    { id: 'first_lab', name: 'Первый опыт', icon: 'stats', description: 'Завершить один протокол', target: 1, value: g => g.state.lab.completed },
    { id: 'four_discoveries', name: 'Исследователь', icon: 'star', description: 'Открыть четыре технологии', target: 4, value: g => Object.keys(g.state.lab.discoveries).length },
    { id: 'all_protocols', name: 'Полная лаборатория', icon: 'star', description: 'Открыть все восемь протоколов', target: 8, value: g => Object.keys(g.state.lab.discoveries).length },
    { id: 'ten_experiments', name: 'Испытатель', icon: 'warning', description: 'Запустить десять экспериментов', target: 10, value: g => g.state.stats.experiments || 0 },
    { id: 'raid_master', name: 'Не потерять ни байта', icon: 'raid', description: 'Пройти RAID Failure Drill', target: 1, value: g => g.state.lab.discoveries.raid ? 1 : 0 },
    { id: 'overclocker', name: 'За пределами спецификации', icon: 'chip', description: 'Пройти Overclock Stability', target: 1, value: g => g.state.lab.discoveries.overclock ? 1 : 0 },
    { id: 'ai_scientist', name: 'Токены под контролем', icon: 'gpu', description: 'Пройти AI Inference Bench', target: 1, value: g => g.state.lab.discoveries.ai ? 1 : 0 },
    { id: 'time_developer', name: 'Десятилетие развития', icon: 'clock', description: 'Пройти десять лет Time Machine', target: 10, value: g => Math.max(0, g.currentYear() - g.state.startYear) },
    { id: 'modern_lab', name: 'Лаборатория будущего', icon: 'clock', description: 'Добраться из прошлого до 2026 года', target: 1, value: g => g.state.startYear < 2026 && g.currentYear() >= 2026 ? 1 : 0 },
    { id: 'fleet_four', name: 'Своя серверная', icon: 'server', description: 'Иметь четыре сервера', target: 4, value: g => g.state.servers.length },
    { id: 'million', name: 'Миллион на хостинге', icon: 'wallet', description: 'Заработать 1 000 000 ₽ за игру', target: 1000000, value: g => g.state.stats.earned || 0 },
    { id: 'builder', name: 'Сборщик', icon: 'wrench', description: 'Собрать пять серверов', target: 5, value: g => g.state.stats.built || 0 },
    { id: 'green_lab', name: 'Зелёный ЦОД', icon: 'bolt', description: 'Энергоэффективность уровня 3', target: 3, value: g => g.state.research.efficiency || 0 },
    { id: 'maintenance', name: 'Регламентные работы', icon: 'wrench', description: 'Обслужить три сервера', target: 3, value: g => g.state.stats.services || 0 }
  ];

  function normalizeLabServer(server) {
    if (!Object.prototype.hasOwnProperty.call(server, 'overclock')) server.overclock = 0;
    if (![0, 10, 20, 30].includes(server.overclock)) server.overclock = 0;
    server.fanProfile = DATA.LAB_FAN_PROFILES[server.fanProfile] ? server.fanProfile : 'balanced';
    server.overclockUnlocked = Boolean(server.overclockUnlocked || server.overclock >= 30);
    ['memoryTuning', 'networkTuning', 'storageTuning', 'powerTuning', 'aiTuning'].forEach(key => {
      server[key] = Number.isFinite(server[key]) ? clamp(server[key], 0, .15) : 0;
    });
    server.labBadges = server.labBadges && typeof server.labBadges === 'object' ? server.labBadges : {};
    if (server.labExperiment && (!DATA.LAB_PROTOCOLS[server.labExperiment.id] || !Array.isArray(server.labExperiment.samples))) server.labExperiment = null;
    if (!server.labExperiment) server.labExperiment = null;
    server.parts.disks.forEach(disk => { if (!server.labExperiment || server.labExperiment.faultDiskUid !== disk.uid) disk.labFault = false; });
  }

  const baseNormalizeServer = proto.normalizeServer;
  proto.normalizeServer = function (server) {
    const result = baseNormalizeServer.call(this, server);
    normalizeLabServer(server);
    return result;
  };
  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseNormalize.call(this);
    const s = this.state;
    s.version = 8;
    s.lab = Object.assign({ runs: 0, completed: 0, failed: 0, aborted: 0, totalSamples: 0, reports: [], discoveries: {} }, s.lab || {});
    s.lab.reports = Array.isArray(s.lab.reports) ? s.lab.reports.slice(0, 12) : [];
    s.lab.discoveries = s.lab.discoveries && typeof s.lab.discoveries === 'object' ? s.lab.discoveries : {};
    ['runs', 'completed', 'failed', 'aborted', 'totalSamples'].forEach(key => { s.lab[key] = Math.max(0, Number(s.lab[key]) || 0); });
    s.achievements = s.achievements && typeof s.achievements === 'object' ? s.achievements : {};
    s.stats.services = Math.max(0, Number(s.stats.services) || 0);
    s.servers.forEach(normalizeLabServer);
    return result;
  };
  const baseInstantiate = proto.instantiateServer;
  proto.instantiateServer = function (...args) {
    const server = baseInstantiate.apply(this, args);
    normalizeLabServer(server);
    return server;
  };
  const baseCreateServer = proto.createServer;
  proto.createServer = function (...args) {
    const server = baseCreateServer.apply(this, args);
    if (server) normalizeLabServer(server);
    return server;
  };

  function storageSample(game, server) {
    const disks = server.parts.disks.filter(disk => disk && !disk.broken && !disk.labFault);
    const raid = DATA.RAIDS[server.raid] || DATA.RAIDS.none;
    const iops = disks.reduce((sum, disk) => sum + (disk.type === 'ssd' ? 5200 : 145) * (disk.speed || 1), 0) * raid.speed;
    return { iops: round(iops), throughput: round(iops * .064 * 10) / 10 };
  }

  const baseRaidCapacity = proto.raidCapacity;
  proto.raidCapacity = function (server) {
    const faults = server.parts.disks.filter(disk => disk && disk.labFault && !disk.broken);
    if (!faults.length) return baseRaidCapacity.call(this, server);
    const saved = faults.map(disk => [disk, disk.broken]);
    faults.forEach(disk => { disk.broken = true; });
    try { return baseRaidCapacity.call(this, server); } finally { saved.forEach(([disk, broken]) => { disk.broken = broken; }); }
  };

  const baseMetrics = proto.metrics;
  proto.metrics = function (server) {
    const m = baseMetrics.call(this, server);
    const experiment = server.labExperiment;
    const profile = experiment && DATA.LAB_PROTOCOLS[experiment.id];
    const fan = DATA.LAB_FAN_PROFILES[server.fanProfile] || DATA.LAB_FAN_PROFILES.balanced;
    const cpu = server.parts.cpu;
    const psuEfficiency = server.parts.psu && !server.parts.psu.broken ? server.parts.psu.efficiency : .75;
    m.overclock = server.overclock || 0;
    m.fanProfile = fan.id;
    m.labIops = 0; m.labThroughput = 0; m.networkLoad = 0; m.packetLoss = 0; m.tokensPerSecond = 0;

    if (server.powered && fan.id !== 'balanced') {
      const ambient = this.room().ambient;
      m.targetTemp = ambient + (m.targetTemp - ambient) / fan.cooling;
      m.cooling *= fan.cooling;
      m.noise = clamp(m.noise + fan.noise, 0, 100);
      m.watts = Math.max(0, m.watts + fan.watts);
    }
    if (server.powered && server.overclock > 0) {
      const factor = 1 + server.overclock / 100;
      m.cpuDemand /= factor;
      if (cpu) {
        const extra = cpu.tdp * server.overclock / 100;
        m.watts += extra * (.3 + .7 * clamp(m.cpuDemand, 0, 100) / 100) / psuEfficiency;
        m.targetTemp += extra * 42 / Math.max(25, m.cooling);
      }
    }
    if (server.powered && profile) {
      m.cpuDemand += profile.cpu || 0;
      if (profile.memoryTarget && m.ramTotal > 0) {
        const target = m.ramTotal * profile.memoryTarget * (1 - (server.memoryTuning || 0));
        m.ramUsed = Math.max(m.ramUsed, target);
      }
      if (cpu && profile.cpu) {
        m.watts += cpu.tdp * profile.cpu / 100 * .62 / psuEfficiency;
        m.targetTemp += cpu.tdp * profile.cpu / 100 * 38 / Math.max(25, m.cooling);
      }
      if (profile.powerSurge) m.watts += Math.max(55, m.watts * .32);
      if (profile.id === 'network') {
        const network = this.network();
        m.networkLoad = clamp(52 + m.cpuDemand * .42, 0, 100);
        m.packetLoss = this.state.outageDay === this.state.day ? 100 : clamp(Math.max(0, m.cpuDemand - 82) * .28 + Math.max(0, m.networkLoad - 92) * .35, 0, 100);
        m.labThroughput = round(network.speed * m.networkLoad / 100 * (1 - m.packetLoss / 100) * 10) / 10;
      }
      if (profile.id === 'storage' || profile.id === 'raid') {
        const storage = storageSample(this, server);
        m.labIops = storage.iops; m.labThroughput = storage.throughput;
      }
      if (profile.id === 'ai') {
        const model = DATA.AI_MODELS[server.aiModel];
        if (model) m.tokensPerSecond = round(clamp((38 + (cpu ? cpu.cores * 9 : 0) + m.gpuCompute * 1.8) * (1 + server.overclock / 100) / Math.sqrt(Math.max(1, model.ram)), .1, 10000) * 10) / 10;
      }
    }
    if (server.memoryTuning && !profile) m.ramUsed *= 1 - server.memoryTuning;
    if (server.powerTuning && server.powered) m.watts *= 1 - server.powerTuning;
    if (server.storageTuning) m.storageFactor *= 1 + server.storageTuning;

    m.ramPercent = m.ramTotal ? m.ramUsed / m.ramTotal * 100 : 0;
    m.rawLoad = clamp(m.cpuDemand, 0, 100);
    m.cpuLoad = server.powered ? m.rawLoad * (server.temp > 85 ? .55 : 1) : 0;
    m.throttling = server.temp > 85;
    let efficiency = 1;
    if (server.temp > 85) efficiency *= .45;
    if (m.ramPercent > 90) efficiency *= m.ramPercent >= 100 ? .15 : .65;
    if (m.cpuDemand > 100) efficiency *= 100 / m.cpuDemand;
    efficiency *= clamp(server.health / 100, .5, 1);
    m.efficiency = clamp(efficiency, 0, 1);
    return m;
  };

  const baseHourly = proto.hourlyServerRevenue;
  proto.hourlyServerRevenue = function (server) {
    if (server.labExperiment) return 0;
    let result = baseHourly.call(this, server);
    if (server.overclock) result *= 1 + server.overclock / 100;
    if (server.networkTuning) result *= 1 + server.networkTuning;
    if (server.aiTuning && server.aiModel) result *= 1 + server.aiTuning;
    return result;
  };

  proto.canStartLab = function (serverId, protocolId) {
    const server = this.getServer(serverId);
    const profile = DATA.LAB_PROTOCOLS[protocolId];
    if (!server || !profile) return { ok: false, message: 'Протокол не найден' };
    if (profile.year > this.currentYear()) return { ok: false, message: `Откроется в ${profile.year} году` };
    if (this.state.paused) return { ok: false, message: 'Снимите паузу для эксперимента' };
    if (!server.powered || !server.os || server.crashed || server.hung || server.broken) return { ok: false, message: 'Нужен работающий сервер с ОС' };
    if (server.stress) return { ok: false, message: 'Сначала остановите Torture Lab' };
    if (server.labExperiment) return { ok: false, message: 'Другой протокол уже выполняется' };
    if (protocolId === 'raid' && (!['raid1', 'raid5', 'raid6', 'raid10'].includes(server.raid) || server.parts.disks.filter(d => !d.broken).length < 2)) return { ok: false, message: 'Нужны RAID 1/5/6/10 и минимум два диска' };
    if (protocolId === 'storage' && !server.parts.disks.some(d => !d.broken)) return { ok: false, message: 'Нужен исправный HDD или SSD' };
    if (protocolId === 'ai' && (!server.aiModel || !server.software.includes('ollama'))) return { ok: false, message: 'Сначала загрузите Local AI модель' };
    return { ok: true, message: 'Готов к запуску' };
  };

  proto.startLabExperiment = function (serverId, protocolId) {
    const server = this.getServer(serverId);
    const profile = DATA.LAB_PROTOCOLS[protocolId];
    const check = this.canStartLab(serverId, protocolId);
    if (!check.ok) { this.notify(check.message, 'warning'); return false; }
    const experiment = { id: protocolId, elapsed: 0, duration: profile.duration, startedDay: this.state.day,
      startedYear: this.currentYear(), samples: [], previousOverclock: null, faultDiskUid: null, initialRaidCapacity: this.raidCapacity(server) };
    if (protocolId === 'overclock') {
      experiment.previousOverclock = server.overclock;
      server.overclock = Math.max(20, server.overclock);
    }
    if (protocolId === 'raid') {
      const disk = server.parts.disks.find(item => item && !item.broken);
      if (disk) { disk.labFault = true; experiment.faultDiskUid = disk.uid; }
    }
    server.labExperiment = experiment;
    server.ramPressure = 0;
    this.state.lab.runs += 1;
    this.state.stats.experiments = (this.state.stats.experiments || 0) + 1;
    this.log(`${server.name}: DEEP LAB · ${profile.name}, ${profile.duration} тиков. Доход остановлен.`, 'info');
    this.notify(`${profile.name}: измерения начались`, 'success');
    this.save();
    return true;
  };

  function restoreLab(server, experiment) {
    if (experiment.previousOverclock != null) server.overclock = experiment.previousOverclock;
    server.parts.disks.forEach(disk => { disk.labFault = false; });
    if (!server.hung) server.ramPressure = 0;
    server.labExperiment = null;
  }

  function makeReport(game, server, experiment, status, reason) {
    const samples = experiment.samples || [];
    const report = { id: uid('report'), serverId: server.id, serverName: server.name, protocolId: experiment.id,
      name: DATA.LAB_PROTOCOLS[experiment.id].name, status, reason, score: 0, day: game.state.day, year: game.currentYear(),
      elapsed: experiment.elapsed, duration: experiment.duration, samples: samples.slice(-12),
      peaks: { temp: round(peak(samples, 'temp') * 10) / 10, cpu: round(peak(samples, 'cpu')), ram: round(peak(samples, 'ram') * 10) / 10,
        watts: round(peak(samples, 'watts')), packetLoss: round(peak(samples, 'packetLoss') * 10) / 10 },
      averages: { throughput: round(avg(samples, 'throughput') * 10) / 10, iops: round(avg(samples, 'iops')),
        tokens: round(avg(samples, 'tokens') * 10) / 10, packetLoss: round(avg(samples, 'packetLoss') * 100) / 100 },
      raidBefore: experiment.initialRaidCapacity, raidAfter: game.raidCapacity(server), rewardRP: 0, rewardMoney: 0, rewardXP: 0, discovery: false };
    switch (experiment.id) {
      case 'thermal': report.score = round(clamp(115 - report.peaks.temp, 0, 100)); break;
      case 'memory': report.score = round(clamp(report.peaks.ram, 0, 100)); break;
      case 'network': report.score = round(clamp(100 - report.averages.packetLoss * 5, 0, 100)); break;
      case 'storage': report.score = round(clamp(report.averages.iops / 2, 1, 100)); break;
      case 'raid': report.score = status === 'success' ? 100 : 0; break;
      case 'power': report.score = round(clamp(110 - report.peaks.watts / Math.max(1, game.room().powerLimit) * 100, 0, 100)); break;
      case 'overclock': report.score = round(clamp(125 - report.peaks.temp, 0, 100)); break;
      case 'ai': report.score = round(clamp(report.averages.tokens * 2, 1, 100)); break;
    }
    return report;
  }

  function applyDiscovery(game, server, protocolId) {
    if (protocolId === 'thermal') server.coolingBonus = Math.min(20, server.coolingBonus + 2);
    if (protocolId === 'memory') server.memoryTuning = Math.min(.1, server.memoryTuning + .02);
    if (protocolId === 'network') server.networkTuning = Math.min(.15, server.networkTuning + .03);
    if (protocolId === 'storage') server.storageTuning = Math.min(.15, server.storageTuning + .03);
    if (protocolId === 'raid') { server.storageTuning = Math.min(.15, server.storageTuning + .02); server.health = Math.min(100, server.health + 2); }
    if (protocolId === 'power') server.powerTuning = Math.min(.1, server.powerTuning + .02);
    if (protocolId === 'overclock') server.overclockUnlocked = true;
    if (protocolId === 'ai') server.aiTuning = Math.min(.15, server.aiTuning + .03);
  }

  function finalizeLab(game, server, status, reason) {
    const experiment = server.labExperiment;
    if (!experiment) return null;
    if (experiment.id === 'storage') server.parts.disks.forEach(disk => { disk.wear = clamp((disk.wear || 90) - 1, 1, 100); });
    const report = makeReport(game, server, experiment, status, reason);
    restoreLab(server, experiment);
    report.raidRestored = game.raidCapacity(server);
    game.state.lab.totalSamples += experiment.samples.length;
    if (status === 'success') {
      const firstGlobal = !game.state.lab.discoveries[experiment.id];
      const firstServer = !server.labBadges[experiment.id];
      game.state.lab.completed += 1;
      game.state.lab.discoveries[experiment.id] = true;
      if (firstServer) { server.labBadges[experiment.id] = true; applyDiscovery(game, server, experiment.id); }
      if (firstGlobal) {
        report.discovery = true; report.rewardRP = 1; report.rewardMoney = 1500; report.rewardXP = 150;
        game.state.research.points += 1; game.addMoney(1500); game.addXP(150);
      } else { report.rewardXP = 25; game.addXP(25); }
    } else if (status === 'failed') game.state.lab.failed += 1;
    else game.state.lab.aborted += 1;
    game.state.lab.reports.unshift(report);
    game.state.lab.reports = game.state.lab.reports.slice(0, 12);
    game.log(`${server.name}: ${report.name} · ${status === 'success' ? 'УСПЕХ' : status === 'failed' ? 'ПРОВАЛ' : 'ОСТАНОВЛЕН'} · ${reason} · score ${report.score}.`, status === 'success' ? 'good' : status === 'failed' ? 'bad' : 'info');
    game.notify(`${report.name}: ${status === 'success' ? `успех · ${report.score}/100` : reason}`, status === 'success' ? 'success' : status === 'failed' ? 'error' : 'warning');
    game.updateStatus(server);
    game.checkAchievements();
    game.save();
    return report;
  }

  proto.stopLabExperiment = function (serverId, reason = 'Остановлено оператором') {
    const server = this.getServer(serverId);
    if (!server || !server.labExperiment) return false;
    finalizeLab(this, server, 'aborted', reason);
    return true;
  };

  function sampleAndEvaluate(game, server) {
    const experiment = server.labExperiment;
    if (!experiment) return;
    const m = game.metrics(server);
    experiment.elapsed += 1;
    experiment.samples.push({ n: experiment.elapsed, temp: round(server.temp * 10) / 10, cpu: round(m.cpuLoad), ram: round(m.ramPercent * 10) / 10,
      watts: round(m.watts), noise: round(m.noise), throughput: m.labThroughput || 0, iops: m.labIops || 0,
      packetLoss: round(m.packetLoss * 10) / 10, tokens: m.tokensPerSecond || 0, raid: game.raidCapacity(server) });
    experiment.samples = experiment.samples.slice(-12);
    if (server.crashed || server.hung || server.broken) {
      finalizeLab(game, server, 'failed', server.crashed ? 'BSOD / перегрев' : server.hung ? 'OOM / зависание' : 'Аппаратная авария'); return;
    }
    if (!server.powered || game.state.powerOut) { finalizeLab(game, server, 'failed', 'Потеря питания'); return; }
    if (experiment.id === 'network' && game.state.outageDay === game.state.day) { finalizeLab(game, server, 'failed', 'Магистраль провайдера недоступна'); return; }
    if (experiment.elapsed < experiment.duration) return;
    let success = true;
    let reason = 'Протокол выполнен';
    const samples = experiment.samples;
    if (experiment.id === 'thermal' && peak(samples, 'temp') >= 85) { success = false; reason = 'Температура вышла за 85°C'; }
    if (experiment.id === 'memory' && (peak(samples, 'ram') < 80 || peak(samples, 'ram') >= 100)) { success = false; reason = 'Не достигнут безопасный диапазон RAM'; }
    if (experiment.id === 'network' && avg(samples, 'packetLoss') >= 5) { success = false; reason = 'Packet loss ≥ 5%'; }
    if (experiment.id === 'storage' && avg(samples, 'iops') <= 0) { success = false; reason = 'Нет данных IOPS'; }
    if (experiment.id === 'raid' && (!experiment.faultDiskUid || game.raidCapacity(server) <= 0)) { success = false; reason = 'Массив не пережил отказ диска'; }
    if (experiment.id === 'ai' && avg(samples, 'tokens') <= 0) { success = false; reason = 'Inference не запущен'; }
    finalizeLab(game, server, success ? 'success' : 'failed', reason);
  }

  const baseTick = proto.tick;
  proto.tick = function () {
    const result = baseTick.call(this);
    if (this.state.paused) return result;
    this.state.servers.slice().forEach(server => { if (server.labExperiment) sampleAndEvaluate(this, server); });
    if (this.tickCounter % 5 === 0) this.checkAchievements();
    return result;
  };

  proto.setOverclock = function (serverId, percent) {
    const server = this.getServer(serverId);
    percent = Number(percent);
    if (!server || ![0, 10, 20, 30].includes(percent)) return false;
    if (server.powered || server.labExperiment) { this.notify('Разгон меняется только на выключенном сервере вне протокола', 'warning'); return false; }
    if (percent === 30 && !server.overclockUnlocked) { this.notify('+30% откроется после Overclock Stability', 'warning'); return false; }
    server.overclock = percent;
    this.log(`${server.name}: CPU overclock ${percent ? `+${percent}%` : 'выключен'}.`, percent >= 20 ? 'bad' : 'info');
    this.notify(`Overclock: ${percent ? `+${percent}%` : 'stock'}`, 'success');
    this.checkAchievements(); this.save(); return true;
  };
  proto.setFanProfile = function (serverId, profileId) {
    const server = this.getServer(serverId);
    if (!server || !DATA.LAB_FAN_PROFILES[profileId]) return false;
    if (server.labExperiment) { this.notify('Во время протокола условия фиксированы', 'warning'); return false; }
    server.fanProfile = profileId;
    this.log(`${server.name}: вентиляторы ${DATA.LAB_FAN_PROFILES[profileId].name}.`);
    this.notify(`Fan profile: ${DATA.LAB_FAN_PROFILES[profileId].name}`, 'success');
    this.save(); return true;
  };
  proto.serviceServer = function (serverId) {
    const server = this.getServer(serverId);
    if (!server) return false;
    if (server.powered) { this.notify('Обслуживание выполняется на выключенном сервере', 'warning'); return false; }
    if (server.broken || server.crashed || server.hung) { this.notify('Сначала выполните ремонт или перезагрузку', 'warning'); return false; }
    const tools = this.state.inventory.some(part => part.type === 'tool');
    const price = tools ? 350 : 500;
    if (!this.spend(price, `Регламентное обслуживание ${server.name}`)) return false;
    server.coolingBonus = Math.min(20, server.coolingBonus + 4);
    server.health = Math.min(100, server.health + 2);
    server.temp = this.room().ambient + 2;
    this.state.stats.services += 1;
    this.addXP(40);
    this.log(`${server.name}: пыль удалена, термопаста обновлена · cooling +4.`, 'good');
    this.notify('Обслуживание завершено · cooling +4', 'success');
    this.checkAchievements(); this.save(); return true;
  };
  proto.powerAll = function (powerOn) {
    let changed = 0; let failed = 0;
    this.state.servers.forEach(server => {
      if (!powerOn && server.powered) {
        if (server.labExperiment) this.stopLabExperiment(server.id, 'Массовое отключение фермы');
        this.powerOff(server); changed += 1;
      } else if (powerOn && !server.powered) {
        if (this.togglePower(server.id)) changed += 1; else failed += 1;
      }
    });
    this.log(`Fleet operations: ${powerOn ? 'запущено' : 'остановлено'} ${changed} серверов${failed ? `, отклонено ${failed}` : ''}.`, failed ? 'bad' : 'good');
    this.notify(`${powerOn ? 'Запуск' : 'Остановка'}: ${changed}${failed ? ` · ошибок ${failed}` : ''}`, failed ? 'warning' : 'success');
    this.save();
    return { ok: failed === 0, changed, failed };
  };

  proto.checkAchievements = function () {
    if (!this.state.achievements || !this.state.lab) return [];
    const unlocked = [];
    DATA.LAB_ACHIEVEMENTS.forEach(achievement => {
      if (this.state.achievements[achievement.id] || achievement.value(this) < achievement.target) return;
      this.state.achievements[achievement.id] = this.state.day;
      unlocked.push(achievement.id);
      this.addXP(60);
      this.log(`Достижение: «${achievement.name}» · +60 XP.`, 'good');
      this.notify(`Достижение: ${achievement.name}`, 'success');
    });
    return unlocked;
  };

  const baseStartStress = proto.startStress;
  proto.startStress = function (serverId, profileId) {
    const server = this.getServer(serverId);
    if (server && server.labExperiment) { this.notify('Сначала завершите Deep Lab протокол', 'warning'); return false; }
    return baseStartStress.call(this, serverId, profileId);
  };
  const basePowerOff = proto.powerOff;
  proto.powerOff = function (server) {
    if (server && server.powered && server.labExperiment) this.stopLabExperiment(server.id, 'Питание отключено оператором');
    return basePowerOff.call(this, server);
  };
  const baseFinishOS = proto.finishOSInstall;
  proto.finishOSInstall = function (serverId, usbUid) {
    const server = this.getServer(serverId);
    if (server && server.labExperiment) this.stopLabExperiment(serverId, 'Переустановка ОС');
    return baseFinishOS.call(this, serverId, usbUid);
  };
  const baseEndDay = proto.endDay;
  proto.endDay = function () {
    const result = baseEndDay.call(this);
    this.checkAchievements();
    return result;
  };

  const baseCommand = proto.executeCommand;
  proto.executeCommand = function (serverId, input) {
    const server = this.getServer(serverId);
    const raw = String(input || '').trim();
    const parts = raw.split(/\s+/).filter(Boolean);
    if (parts[0] === 'sudo') parts.shift();
    const command = (parts.shift() || '').toLowerCase();
    if (command === 'help') {
      const result = baseCommand.call(this, serverId, input);
      if (result.ok) result.lines.push('lab [start <id>|stop|report] · ' + Object.keys(DATA.LAB_PROTOCOLS).join('/'), 'tune [oc 0|10|20|30] · tune fan [silent|balanced|turbo]');
      return result;
    }
    if (!server || !['lab', 'tune'].includes(command)) return baseCommand.call(this, serverId, input);
    const ok = lines => ({ ok: true, lines: Array.isArray(lines) ? lines : [lines] });
    const fail = lines => ({ ok: false, lines: Array.isArray(lines) ? lines : [lines] });
    if (command === 'lab') {
      const sub = (parts[0] || '').toLowerCase();
      if (!sub) return ok([server.labExperiment ? `RUN ${server.labExperiment.id} · ${server.labExperiment.elapsed}/${server.labExperiment.duration}` : 'Deep Lab готов: ' + Object.keys(DATA.LAB_PROTOCOLS).join(' · '), `OC +${server.overclock}% · fan ${server.fanProfile}`]);
      if (sub === 'stop') return this.stopLabExperiment(serverId) ? ok('Протокол остановлен, отчёт сохранён') : fail('Активного протокола нет');
      if (sub === 'report') {
        const report = this.state.lab.reports.find(item => item.serverId === server.id);
        return report ? ok([`${report.name} · ${report.status} · score ${report.score}/100`, report.reason, `PEAK ${report.peaks.temp}°C · RAM ${report.peaks.ram}% · ${report.peaks.watts}W`]) : fail('Отчётов пока нет');
      }
      const id = sub === 'start' ? parts[1] : sub;
      return this.startLabExperiment(serverId, id) ? ok(`${DATA.LAB_PROTOCOLS[id].name}: START · доход 0`) : fail(this.canStartLab(serverId, id).message);
    }
    const sub = (parts[0] || '').toLowerCase();
    if (!sub) return ok([`CPU overclock +${server.overclock}% · fan ${server.fanProfile}`, `+30% OC: ${server.overclockUnlocked ? 'unlocked' : 'locked'}`]);
    if (sub === 'oc') return this.setOverclock(serverId, Number(parts[1])) ? ok(`Overclock +${server.overclock}%`) : fail('Изменение разгона отклонено');
    if (sub === 'fan') return this.setFanProfile(serverId, parts[1]) ? ok(`Fan profile ${server.fanProfile}`) : fail('Профиль не изменён');
    return fail('tune oc <0|10|20|30> · tune fan <silent|balanced|turbo>');
  };

  proto.deepLabLoaded = true;
})();
