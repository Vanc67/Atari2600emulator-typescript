/* Ultimate 7.0: независимая кампания «Папина PC-мастерская». */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !window.DATA.PC_GENERATIONS || Game.prototype.pcWorkshopLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(value);
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const clone = object => JSON.parse(JSON.stringify(object));
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const names = ['Дядя Саша','Тётя Лена','Сосед Виктор','Папин друг Миша','Коллега мамы','Знакомый с работы','Алексей из объявления','Марина из объявления','Студент Дима','Офис на районе','Школьный учитель','Фотограф Игорь'];
  const issueIds = Object.keys(DATA.PC_ISSUES);
  const speeds = [1, 15, 60];

  function emptyPCState() {
    return { started: false, year: 1990, yearDay: 0, day: 1, minute: 480, speed: 1, accumulator: 0, cycles: 0,
      money: 2500, reputation: 0, xp: 0, level: 1, completed: 0, earnings: 0, dumpsters: 0, sold: 0,
      offers: [], computers: [], parts: [], logs: [], upgrades: { bench: 0, ads: 0, tools: 0 }, stats: { diagnostics: 0, repairs: 0, benchmarks: 0, experimentalOS: 0 } };
  }
  function generationForYear(year) {
    year = clamp(round(Number(year) || 1990), 1990, 2026);
    return DATA.PC_GENERATIONS.find(item => item.year === year) || DATA.PC_GENERATIONS[0];
  }
  function osById(id) { return DATA.PC_OS.find(os => os.id === id); }
  function defaultIssue(id) { return { id, revealed: false, resolved: false }; }

  function normalizeComputer(pc) {
    pc.issues = Array.isArray(pc.issues) ? pc.issues.filter(issue => DATA.PC_ISSUES[issue.id]) : [];
    pc.diagnostics = pc.diagnostics && typeof pc.diagnostics === 'object' ? pc.diagnostics : {};
    pc.benchmarks = pc.benchmarks && typeof pc.benchmarks === 'object' ? pc.benchmarks : {};
    pc.cleanedSpots = Array.isArray(pc.cleanedSpots) ? pc.cleanedSpots.slice(0, 6) : [];
    pc.health = Number.isFinite(pc.health) ? clamp(pc.health, 1, 100) : 100;
    pc.temp = Number.isFinite(pc.temp) ? pc.temp : 24;
    pc.cooling = Number.isFinite(pc.cooling) ? pc.cooling : 60;
    pc.load = Number.isFinite(pc.load) ? clamp(pc.load, 0, 1) : 0;
    pc.powered = Boolean(pc.powered) && !pc.crashed;
    pc.origin = pc.origin || 'owned';
    pc.osPenalty = Number.isFinite(pc.osPenalty) ? clamp(pc.osPenalty, .1, 1) : 1;
    if (pc.os && !osById(pc.os)) pc.os = null;
  }

  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseNormalize.call(this);
    const s = this.state;
    s.version = 9;
    s.campaignMode = s.campaignMode === 'pc' ? 'pc' : 'servers';
    s.pc = Object.assign(emptyPCState(), s.pc || {});
    const pc = s.pc;
    pc.upgrades = Object.assign({ bench: 0, ads: 0, tools: 0 }, pc.upgrades || {});
    pc.stats = Object.assign({ diagnostics: 0, repairs: 0, benchmarks: 0, experimentalOS: 0 }, pc.stats || {});
    ['offers', 'computers', 'parts', 'logs'].forEach(key => { pc[key] = Array.isArray(pc[key]) ? pc[key] : []; });
    pc.computers.forEach(normalizeComputer);
    pc.offers.forEach(offer => { if (offer.pc) normalizeComputer(offer.pc); });
    pc.year = clamp(Number(pc.year) || 1990, 1990, 2026);
    pc.yearDay = clamp(Number(pc.yearDay) || 0, 0, 11);
    pc.speed = speeds.includes(Number(pc.speed)) ? Number(pc.speed) : 1;
    pc.accumulator = Number.isFinite(pc.accumulator) ? Math.max(0, pc.accumulator) : 0;
    pc.money = Number.isFinite(pc.money) ? pc.money : 2500;
    pc.reputation = Math.max(0, Number(pc.reputation) || 0);
    pc.level = Math.max(1, Number(pc.level) || 1);
    if (!s.pc.started) s.campaignMode = 'servers';
    return result;
  };

  proto.pcGeneration = generationForYear;
  proto.pcOS = osById;
  proto.makePCPart = function (type, source) {
    const gen = source.year ? source : generationForYear(source);
    const values = {
      ram: { name: `${gen.ramType} ${gen.ramMb >= 1024 ? `${gen.ramMb / 1024} ГБ` : `${gen.ramMb} МБ`}`, ramType: gen.ramType, ramMb: gen.ramMb, value: Math.max(80, round(gen.ramMb * (gen.year < 2000 ? 8 : 1.5))) },
      psu: { name: `ATX PSU ${gen.psuWatts}W`, watts: gen.psuWatts, value: Math.max(150, round(gen.psuWatts * 2.2)) },
      disk: { name: `${gen.storageType} ${gen.storageMb >= 1048576 ? `${round(gen.storageMb / 1048576)} ТБ` : gen.storageMb >= 1024 ? `${round(gen.storageMb / 1024)} ГБ` : `${gen.storageMb} МБ`}`, storageType: gen.storageType, storageMb: gen.storageMb, value: Math.max(120, round(gen.storageMb / 90)) },
      gpu: { name: gen.gpu, gpuScore: gen.gpuScore, value: Math.max(100, gen.gpuScore * 12) }
    };
    return Object.assign({ uid: uid(`pc-${type}`), type, year: gen.year, broken: false }, values[type]);
  };

  proto.createPC = function (year, options = {}) {
    const gen = generationForYear(year);
    const id = uid('pc');
    const issueCount = options.issues ? options.issues.length : options.issuesCount == null ? 0 : options.issuesCount;
    let issues = options.issues ? options.issues.slice() : [];
    if (!options.issues && issueCount) {
      const pool = issueIds.filter(issue => gen.year >= 1993 || !['virus', 'os', 'gpu'].includes(issue));
      while (issues.length < issueCount && pool.length) issues.push(pool.splice(Math.floor(Math.random() * pool.length), 1)[0]);
    }
    const pc = { id, name: options.name || gen.name, year: gen.year, origin: options.origin || 'owned', owner: options.owner || 'Мой ПК',
      cpu: gen.cpu, cpuScore: gen.cpuScore, ramType: gen.ramType, ramMb: gen.ramMb, storageType: gen.storageType, storageMb: gen.storageMb,
      gpu: gen.gpu, gpuScore: gen.gpuScore, psuWatts: gen.psuWatts, cooling: gen.cooling,
      os: options.os === undefined ? (gen.year < 1994 ? 'pc-dos33' : gen.year < 1995 ? 'pc-dos622' : gen.year < 1998 ? 'pc-win95' : gen.year < 2001 ? 'pc-win98' : gen.year < 2009 ? 'pc-winxp' : gen.year < 2015 ? 'pc-win7' : gen.year < 2021 ? 'pc-win10' : 'pc-win11') : options.os,
      issues: issues.map(defaultIssue), diagnostics: {}, benchmarks: {}, cleanedSpots: [], health: issues.length ? 72 : 100,
      temp: 24, powered: false, crashed: false, load: 0, experimentalOS: false, osPenalty: 1, jobId: options.jobId || null, createdDay: this.state.pc ? this.state.pc.day : 1 };
    normalizeComputer(pc);
    return pc;
  };

  proto.pcLog = function (text, type = 'info') {
    const pc = this.state.pc;
    pc.logs.unshift({ id: uid('pc-log'), day: pc.day, year: pc.year, text, type });
    if (pc.logs.length > 80) pc.logs.length = 80;
  };
  proto.pcRank = function () {
    return ['Папин помощник', 'Ремонтник для друзей', 'Мастер по объявлению', 'Владелец мастерской', 'Ретро-эксперт', 'Легенда железа'][Math.min(5, this.state.pc.level - 1)];
  };
  proto.addPCXP = function (amount) {
    const pc = this.state.pc;
    pc.xp += round(amount);
    pc.level = Math.min(6, 1 + Math.floor(Math.sqrt(pc.xp / 350)));
  };

  proto.generatePCOffers = function () {
    const pc = this.state.pc;
    const count = 3 + Math.min(2, pc.upgrades.ads || 0);
    const offers = [];
    for (let i = 0; i < count; i += 1) {
      const early = pc.completed < 3;
      const owner = early ? names[i % 6] : names[Math.floor(Math.random() * names.length)];
      const source = early || pc.level < 2 ? 'friend' : Math.random() < .75 ? 'avito' : 'friend';
      const year = clamp(pc.year - Math.floor(Math.random() * Math.min(5, Math.max(1, pc.level))), 1990, pc.year);
      const issuesCount = 1 + Math.floor(Math.random() * Math.min(3, 1 + pc.level / 2));
      const jobId = uid('job');
      const machine = this.createPC(year, { origin: 'job', owner, issuesCount, jobId });
      const repairEstimate = machine.issues.reduce((sum, issue) => sum + this.pcIssuePrice(machine, issue.id), 0);
      const reward = round((250 + machine.cpuScore * 2.4 + issuesCount * 210 + repairEstimate * 1.3) * (1 + pc.upgrades.ads * .12) * (source === 'avito' ? 1.2 : 1));
      offers.push({ id: jobId, pc: machine, owner, source, reward, deadline: pc.day + 4 + pc.upgrades.ads,
        title: `${machine.name} · ${DATA.PC_ISSUES[machine.issues[0].id].name}` });
    }
    return offers;
  };

  proto.startPCCampaign = function () {
    const s = this.state;
    if (!s.pc.started) {
      const fresh = emptyPCState(); fresh.started = true; s.pc = fresh;
      const own = this.createPC(1990, { name: 'Папин тестовый 386', origin: 'owned', owner: 'Папа', os: 'pc-dos33' });
      fresh.computers.push(own);
      fresh.parts.push(this.makePCPart('ram', 1990), this.makePCPart('psu', 1990));
      fresh.offers = this.generatePCOffers();
      this.pcLog('1990 год. Папа дал 2 500 ₽, тестовый 386 и первые контакты друзей.', 'good');
      this.notify('Папина мастерская открыта · первые клиенты уже ждут', 'success');
    }
    s.campaignMode = 'pc';
    this.save();
    return true;
  };
  proto.switchCampaign = function (mode) {
    if (mode === 'pc') {
      if (!this.state.pc.started) { this.state.campaignMode = 'servers'; return false; }
      this.state.campaignMode = 'pc';
    } else this.state.campaignMode = 'servers';
    this.save(); return true;
  };
  proto.setPCSpeed = function (speed) {
    speed = Number(speed);
    if (!speeds.includes(speed)) return false;
    this.state.pc.speed = speed;
    this.pcLog(`Скорость кампании: ${DATA.PC_SPEEDS[speed].name}.`);
    this.notify(`${DATA.PC_SPEEDS[speed].name} скорость · игровой день ${DATA.PC_SPEEDS[speed].dayMinutes} мин`, 'success');
    this.save(); return true;
  };
  proto.pcTimeInfo = function () {
    const pc = this.state.pc;
    return { year: pc.year, day: pc.day, minute: pc.minute, yearDay: pc.yearDay, speed: pc.speed,
      secondsForGameDay: 5400 / pc.speed, daysToNextYear: 12 - pc.yearDay,
      progress: pc.yearDay / 12 * 100, remainingRealDays: Math.max(0, (2026 - pc.year) * 12 - pc.yearDay) * 90 / pc.speed / 1440 };
  };
  proto.pcClock = function () {
    const pc = this.state.pc;
    return `${String(Math.floor(pc.minute / 60) % 24).padStart(2, '0')}:${String(Math.floor(pc.minute % 60)).padStart(2, '0')}`;
  };

  proto.endPCDay = function () {
    const pc = this.state.pc;
    pc.day += 1; pc.yearDay += 1;
    if (pc.yearDay >= 12) {
      pc.yearDay = 0;
      if (pc.year < 2026) {
        pc.year += 1; this.addPCXP(200);
        this.pcLog(`Наступил ${pc.year} год: новые ПК, ОС, игры и клиенты.`, 'good');
        this.notify(`${pc.year} год в мастерской · новое железо и тесты`, 'success');
      } else { pc.cycles += 1; this.addPCXP(100); }
    }
    pc.offers = pc.offers.filter(offer => offer.deadline >= pc.day);
    const desired = 3 + Math.min(2, pc.upgrades.ads);
    if (pc.day % 2 === 0 || pc.offers.length < desired) pc.offers = this.generatePCOffers();
    this.pcLog(`День ${pc.day}: мастерская открыта, репутация ${pc.reputation}.`);
  };
  proto.advancePCMinutes = function (minutes) {
    const pc = this.state.pc;
    if (!pc.started) return;
    pc.minute += Math.max(0, Number(minutes) || 0);
    while (pc.minute >= 1440) { pc.minute -= 1440; this.endPCDay(); }
  };
  proto.tickPC = function (realSeconds = 1) {
    const pc = this.state.pc;
    if (!pc.started || this.state.paused) return;
    pc.accumulator += Math.max(0, Number(realSeconds) || 0) * pc.speed;
    let guard = 0;
    while (pc.accumulator >= 45 && guard < 1000) { pc.accumulator -= 45; this.advancePCMinutes(12); guard += 1; }
    pc.computers.forEach(machine => {
      if (!machine.powered) { machine.temp += (24 - machine.temp) * .05; return; }
      const m = this.pcMetrics(machine);
      machine.temp += (m.targetTemp - machine.temp) * .08;
      machine.load = Math.max(0, machine.load - .08);
      if (machine.temp >= 95) {
        machine.crashed = true; machine.powered = false; machine.temp = 95;
        this.pcLog(`${machine.name}: аварийное отключение при 95°C.`, 'bad');
        this.notify(`${machine.name}: перегрев, нужен ремонт`, 'error');
      }
    });
    if ((this.tickCounter + pc.day) % 15 === 0) this.save();
  };

  proto.acceptPCJob = function (jobId) {
    const pc = this.state.pc;
    const index = pc.offers.findIndex(offer => offer.id === jobId);
    if (index < 0) return false;
    const active = pc.computers.filter(machine => machine.origin === 'job').length;
    if (active >= 1 + pc.upgrades.bench) { this.notify('Все рабочие места заняты', 'warning'); return false; }
    const offer = pc.offers.splice(index, 1)[0];
    offer.pc.jobId = offer.id; offer.pc.reward = offer.reward; offer.pc.deadline = offer.deadline;
    pc.computers.push(offer.pc);
    this.pcLog(`Принят заказ ${offer.owner}: ${offer.pc.name}, оплата ${offer.reward} ₽.`, 'good');
    this.notify('ПК доставлен в мастерскую · начните диагностику', 'success');
    this.save(); return true;
  };
  proto.getPC = function (id) { return this.state.pc.computers.find(pc => pc.id === id); };

  proto.runPCDiagnostic = function (id, testId) {
    const machine = this.getPC(id);
    if (!machine || testId !== 'full' && !DATA.PC_DIAGNOSTICS[testId]) return false;
    const tests = testId === 'full' ? Object.keys(DATA.PC_DIAGNOSTICS) : [testId];
    tests.forEach(test => { machine.diagnostics[test] = true; });
    machine.issues.forEach(issue => { if (tests.includes(DATA.PC_ISSUES[issue.id].test)) issue.revealed = true; });
    const minutes = testId === 'full' ? 35 : DATA.PC_DIAGNOSTICS[testId].minutes;
    this.advancePCMinutes(minutes); this.state.pc.stats.diagnostics += 1;
    this.pcLog(`${machine.name}: ${testId === 'full' ? 'полная диагностика' : DATA.PC_DIAGNOSTICS[testId].name}, найдено ${machine.issues.filter(issue => issue.revealed).length} проблем.`);
    this.notify('Диагностика завершена · неисправности раскрыты', 'success');
    this.save(); return true;
  };
  proto.cleanPCDust = function (id, spot) {
    const machine = this.getPC(id);
    spot = Number(spot);
    if (!machine || machine.powered || spot < 0 || spot > 5) return false;
    if (!machine.cleanedSpots.includes(spot)) machine.cleanedSpots.push(spot);
    if (machine.cleanedSpots.length >= 6) {
      machine.issues.filter(issue => issue.id === 'dust').forEach(issue => { issue.resolved = true; issue.revealed = true; });
      machine.cooling += 15; machine.health = Math.min(100, machine.health + 10);
      this.state.pc.stats.repairs += 1; this.addPCXP(25);
      this.pcLog(`${machine.name}: шесть зон очищены от пыли, airflow восстановлен.`, 'good');
      this.notify('Чистка завершена · cooling +15', 'success');
    }
    this.save(); return machine.cleanedSpots.length;
  };

  proto.pcSpareForIssue = function (machine, issueId) {
    const issue = DATA.PC_ISSUES[issueId];
    if (!issue || !issue.part) return null;
    return this.state.pc.parts.find(part => {
      if (part.type !== issue.part) return false;
      if (part.type === 'ram') return part.ramType === machine.ramType;
      if (part.type === 'psu') return part.watts >= machine.psuWatts;
      return true;
    }) || null;
  };
  proto.pcIssuePrice = function (machine, issueId) {
    const issue = DATA.PC_ISSUES[issueId];
    if (!issue) return 0;
    if (issueId === 'thermalPaste') return 50;
    if (!issue.part) return 0;
    const part = this.makePCPart(issue.part, machine.year);
    return round(part.value * 1.25 * (1 - this.state.pc.upgrades.tools * .08));
  };
  proto.repairPCIssue = function (id, issueId, options = {}) {
    const machine = this.getPC(id); const issue = machine && machine.issues.find(item => item.id === issueId);
    const definition = DATA.PC_ISSUES[issueId];
    if (!machine || !issue || !definition || issue.resolved) return false;
    if (!issue.revealed) { this.notify('Сначала выполните нужную диагностику', 'warning'); return false; }
    if (machine.powered && ['part', 'paste', 'clean'].includes(definition.action)) { this.notify('Выключите ПК перед аппаратным ремонтом', 'warning'); return false; }
    if (definition.action === 'clean') return this.cleanPCDust(id, 0) && false;
    if (definition.action === 'os') { machine.os = null; this.pcLog(`${machine.name}: повреждённая ОС удалена, выберите новый образ.`); this.save(); return true; }
    if (definition.action === 'part') {
      let spare = this.pcSpareForIssue(machine, issueId);
      if (!spare && options.buy) {
        const price = this.pcIssuePrice(machine, issueId);
        if (this.state.pc.money < price) { this.notify(`Нужно ${price} ₽ для запчасти`, 'error'); return false; }
        this.state.pc.money -= price;
        spare = this.makePCPart(definition.part, machine.year); this.state.pc.parts.push(spare);
        this.pcLog(`Куплена запчасть ${spare.name} за ${price} ₽.`);
      }
      if (!spare) { this.notify('Нет совместимой запчасти · можно купить и заменить', 'warning'); return false; }
      this.state.pc.parts = this.state.pc.parts.filter(part => part.uid !== spare.uid);
      if (definition.part === 'ram') { machine.ramMb = Math.max(machine.ramMb, spare.ramMb); machine.ramType = spare.ramType; }
      if (definition.part === 'psu') machine.psuWatts = Math.max(machine.psuWatts, spare.watts);
      if (definition.part === 'disk') { machine.storageMb = Math.max(machine.storageMb, spare.storageMb); machine.storageType = spare.storageType; }
      if (definition.part === 'gpu') { machine.gpuScore = Math.max(machine.gpuScore, spare.gpuScore); machine.gpu = spare.name; }
    }
    if (definition.action === 'paste') {
      if (this.state.pc.money < 50) { this.notify('Нужно 50 ₽ на термопасту', 'error'); return false; }
      this.state.pc.money -= 50; machine.cooling += 12;
    }
    if (definition.action === 'virus') machine.load = 0;
    if (definition.action === 'bios') machine.crashed = false;
    issue.resolved = true; machine.health = Math.min(100, machine.health + 12);
    this.state.pc.stats.repairs += 1; this.addPCXP(35); this.advancePCMinutes(25);
    this.pcLog(`${machine.name}: устранено — ${definition.name}.`, 'good');
    this.notify(`${definition.name}: устранено`, 'success');
    this.save(); return true;
  };

  proto.installPCOS = function (id, osId, experimental = false) {
    const machine = this.getPC(id); const os = osById(osId);
    if (!machine || !os) return false;
    if (machine.powered) { this.notify('Выключите ПК перед установкой ОС', 'warning'); return false; }
    if (experimental && machine.origin !== 'owned') { this.notify('Эксперименты разрешены только на своих ПК', 'warning'); return false; }
    const compatible = machine.ramMb >= os.minRamMb && machine.storageMb >= os.minStorageMb && os.year <= this.state.pc.year;
    if (!experimental && !compatible) { this.notify(`${os.name}: проверьте RAM, диск и год выпуска`, 'warning'); return false; }
    if (!experimental && os.price && this.state.pc.money < os.price) { this.notify(`Лицензия стоит ${os.price} ₽`, 'error'); return false; }
    if (!experimental && os.price) this.state.pc.money -= os.price;
    machine.os = os.id;
    machine.experimentalOS = experimental && (os.year > machine.year || machine.ramMb < os.minRamMb || machine.storageMb < os.minStorageMb);
    machine.osPenalty = clamp(Math.min(1, machine.ramMb / os.minRamMb, machine.storageMb / os.minStorageMb), .12, 1);
    machine.crashed = false;
    machine.issues.filter(issue => issue.id === 'os').forEach(issue => { issue.revealed = true; issue.resolved = true; });
    if (machine.experimentalOS) { this.state.pc.stats.experimentalOS += 1; this.addPCXP(80); }
    this.advancePCMinutes(45);
    this.pcLog(`${machine.name}: установлена ${os.name}${machine.experimentalOS ? ' · EXPERIMENTAL BYPASS' : ''}.`, machine.experimentalOS ? 'bad' : 'good');
    this.notify(machine.experimentalOS ? `${os.name} запущена на ${machine.year} ПК · режим эксперимента` : `${os.name} установлена`, 'success');
    this.save(); return true;
  };

  proto.togglePCPower = function (id) {
    const machine = this.getPC(id);
    if (!machine) return false;
    if (machine.powered) { machine.powered = false; machine.load = 0; this.save(); return true; }
    if (machine.crashed) { this.notify('Сначала устраните перегрев и выполните диагностику', 'warning'); return false; }
    if (machine.issues.some(issue => issue.id === 'psu' && !issue.resolved)) { this.notify('Блок питания неисправен', 'error'); return false; }
    machine.powered = true; machine.load = .05; this.save(); return true;
  };
  proto.rebootPC = function (id) {
    const machine = this.getPC(id);
    if (!machine) return false;
    if (machine.issues.some(issue => issue.id === 'psu' && !issue.resolved)) { this.notify('Блок питания неисправен', 'error'); return false; }
    if (machine.crashed) {
      if (machine.temp >= 85) { this.notify('Дайте ПК остыть ниже 85°C', 'warning'); return false; }
      if (machine.issues.some(issue => ['dust', 'thermalPaste', 'psu'].includes(issue.id) && !issue.resolved)) { this.notify('Сначала устраните причину аварии', 'warning'); return false; }
    }
    machine.crashed = false; machine.powered = true; machine.load = .05;
    this.pcLog(`${machine.name}: перезагрузка выполнена.`); this.save(); return true;
  };
  proto.pcMetrics = function (machine) {
    const os = osById(machine.os);
    const load = machine.powered ? machine.load || .05 : 0;
    const ramUsedMb = machine.powered ? Math.min(machine.ramMb, (os ? Math.min(os.minRamMb * .55, machine.ramMb * .7) : 64) + machine.ramMb * load * .25) : 0;
    const watts = machine.powered ? 28 + machine.cpuScore * (.02 + load * .065) + machine.gpuScore * load * .075 : 0;
    const dust = machine.issues.some(issue => issue.id === 'dust' && !issue.resolved) ? 28 : 0;
    const targetTemp = machine.powered ? 24 + watts * 42 / Math.max(25, machine.cooling) + dust : 24;
    const noise = machine.powered ? clamp(15 + watts * .035 + (machine.storageType === 'HDD' ? 5 : 0), 12, 85) : 0;
    return { cpuLoad: clamp(load * 100, 0, 100), gpuLoad: clamp(load * 95, 0, 100), ramUsedMb, ramPercent: machine.ramMb ? ramUsedMb / machine.ramMb * 100 : 0,
      watts, targetTemp, noise, performance: clamp(machine.health / 100 * machine.osPenalty, .05, 1) };
  };

  proto.runPCBenchmark = function (id, benchmarkId) {
    const machine = this.getPC(id); const benchmark = DATA.PC_BENCHMARKS[benchmarkId];
    if (!machine || !benchmark) return false;
    if (!machine.powered || machine.crashed) { this.notify('Включите исправный ПК для теста', 'warning'); return false; }
    if (benchmarkId !== 'stability' && !machine.os) { this.notify('Сначала установите ОС', 'warning'); return false; }
    const unresolved = machine.issues.filter(issue => !issue.resolved).length;
    let fps; let passed;
    if (benchmarkId === 'stability') {
      fps = clamp(100 - unresolved * 24 - Math.max(0, machine.temp - 75) * 1.5, 0, 100);
      passed = unresolved === 0 && fps >= 60 && !machine.crashed;
    } else {
      const cpuRatio = machine.cpuScore / benchmark.cpu;
      const gpuRatio = machine.gpuScore / benchmark.gpu;
      const ramRatio = machine.ramMb / benchmark.ramMb;
      const storage = machine.storageType === 'NVMe' ? 1.15 : machine.storageType === 'SSD' ? 1.08 : 1;
      const os = osById(machine.os);
      const driver = os && os.style === 'dos' && benchmark.year > 1996 ? .55 : 1;
      fps = clamp(benchmark.target * Math.min(3, cpuRatio * .42 + gpuRatio * .46 + Math.min(2, ramRatio) * .12) * storage * driver * machine.osPenalty * clamp(machine.health / 100, .4, 1), .1, 360);
      passed = fps >= benchmark.target * .8 && unresolved === 0;
    }
    const seed = hash(machine.id + benchmarkId);
    const samples = Array.from({ length: 16 }, (_, index) => round(clamp(fps * (1 + Math.sin(index * 1.7 + seed % 20) * .045 + ((seed >> index % 16) & 3) * .006), 0, 360) * 10) / 10);
    const result = { benchmarkId, fps: round(fps * 10) / 10, score: round(clamp(fps / benchmark.target * 100, 0, 100)), passed,
      samples, temp: round(machine.temp * 10) / 10, day: this.state.pc.day, year: this.state.pc.year };
    machine.benchmarks[benchmarkId] = result; machine.lastBenchmark = benchmarkId; machine.load = 1;
    machine.temp += benchmarkId === 'stability' ? 3 : clamp(4 + machine.gpuScore / 250, 4, 18);
    if (machine.temp >= 95) { machine.temp = 95; machine.crashed = true; machine.powered = false; result.passed = false; }
    this.state.pc.stats.benchmarks += 1; this.addPCXP(benchmarkId === 'stability' ? 25 : 40); this.advancePCMinutes(30);
    this.pcLog(`${machine.name}: ${benchmark.name} · ${result.fps}${benchmarkId === 'stability' ? ' points' : ' FPS'} · ${passed ? 'PASS' : 'FAIL'}.`, passed ? 'good' : 'bad');
    this.notify(`${benchmark.name}: ${result.fps}${benchmarkId === 'stability' ? ' points' : ' FPS'}`, passed ? 'success' : 'warning');
    this.save(); return result;
  };

  proto.completePCJob = function (id) {
    const pc = this.state.pc; const machine = this.getPC(id);
    if (!machine || machine.origin !== 'job') return false;
    const unresolved = machine.issues.filter(issue => !issue.resolved).length;
    const stability = machine.benchmarks.stability;
    if (machine.powered) { this.notify('Выключите ПК перед возвратом клиенту', 'warning'); return false; }
    if (unresolved || !stability || !stability.passed) { this.notify('Устраните все неисправности и пройдите Stability Suite', 'warning'); return false; }
    const reward = round(machine.reward * (1 + Math.min(20, pc.reputation) * .01));
    pc.money += reward; pc.earnings += reward; pc.completed += 1; pc.reputation += 1;
    this.addPCXP(100 + machine.issues.length * 35);
    pc.computers = pc.computers.filter(item => item.id !== id);
    pc.offers = [...pc.offers, ...this.generatePCOffers().slice(0, 1)];
    this.pcLog(`${machine.owner}: ПК возвращён, получено ${reward} ₽ и +1 репутация.`, 'good');
    this.notify(`Заказ закрыт · +${reward} ₽ · репутация ${pc.reputation}`, 'success');
    this.advancePCMinutes(60); this.save(); return { ok: true, reward };
  };

  proto.searchDumpster = function () {
    const pc = this.state.pc;
    if (pc.dumpsterDay === pc.day) { this.notify('Сегодня мусорка уже проверена · возвращайтесь завтра', 'warning'); return false; }
    pc.dumpsterDay = pc.day; pc.dumpsters += 1;
    const isPC = Math.random() < .72;
    let item;
    if (isPC) {
      const year = 1990 + Math.floor(Math.random() * (pc.year - 1990 + 1));
      item = this.createPC(year, { origin: 'owned', owner: 'Находка с мусорки', issuesCount: 2 + Math.floor(Math.random() * 2), os: null,
        name: `Выброшенный ${generationForYear(year).name}` });
      pc.computers.push(item);
    } else {
      const year = 1990 + Math.floor(Math.random() * (pc.year - 1990 + 1));
      const types = ['ram', 'psu', 'disk', 'gpu']; item = this.makePCPart(types[Math.floor(Math.random() * types.length)], year);
      item.value = round(item.value * .45); pc.parts.push(item);
    }
    this.addPCXP(45); this.advancePCMinutes(40);
    this.pcLog(`Мусорка: найден ${isPC ? item.name : item.name}.`, 'good');
    this.notify(isPC ? `Ретро-находка: ${item.name}` : `На мусорке нашли: ${item.name}`, 'success');
    this.save(); return { type: isPC ? 'pc' : 'part', item };
  };
  proto.sellPC = function (id) {
    const pc = this.state.pc; const machine = this.getPC(id);
    if (!machine || machine.origin !== 'owned' || machine.powered) return false;
    const unresolved = machine.issues.filter(issue => !issue.resolved).length;
    const base = machine.cpuScore * 15 + machine.gpuScore * 11 + machine.ramMb * 1.2 + machine.storageMb / 100;
    const price = Math.max(100, round(base * (unresolved ? .25 : .7) * machine.health / 100));
    pc.money += price; pc.earnings += price; pc.sold += 1;
    pc.computers = pc.computers.filter(item => item.id !== id);
    this.pcLog(`${machine.name}: продано за ${price} ₽.`, 'good'); this.notify(`ПК продан · +${price} ₽`, 'success');
    this.save(); return price;
  };

  proto.buyPCUpgrade = function (kind) {
    const definitions = { bench: [3000, 9000, 25000], ads: [1500, 5000, 12000, 30000, 70000], tools: [1000, 4000, 12000] };
    const prices = definitions[kind]; const pc = this.state.pc;
    if (!prices) return false;
    const level = pc.upgrades[kind]; const price = prices[level];
    if (price == null || pc.money < price) { this.notify(price == null ? 'Улучшение уже максимальное' : `Нужно ${price} ₽`, 'warning'); return false; }
    pc.money -= price; pc.upgrades[kind] += 1; this.addPCXP(100);
    this.pcLog(`Мастерская: ${kind} уровень ${pc.upgrades[kind]}, инвестиция ${price} ₽.`, 'good');
    this.notify('Мастерская улучшена', 'success'); this.save(); return true;
  };

  proto.executePCCommand = function (id, input) {
    const machine = this.getPC(id); const parts = String(input || '').trim().split(/\s+/).filter(Boolean); const command = (parts.shift() || '').toLowerCase();
    const ok = lines => ({ ok: true, lines: Array.isArray(lines) ? lines : [lines] });
    const fail = lines => ({ ok: false, lines: Array.isArray(lines) ? lines : [lines] });
    if (!machine) return fail('ПК не найден');
    if (command === 'help') return ok(['status · scan · repair <issue> · bench <id> · install <os> [experimental]', 'poweroff · reboot · tests · clear · exit']);
    if (command === 'status') { const m = this.pcMetrics(machine); return ok([`${machine.name} · ${machine.year} · ${machine.os ? osById(machine.os).name : 'NO OS'}`, `CPU ${round(m.cpuLoad)}% · RAM ${round(m.ramPercent)}% · ${round(machine.temp)}°C · ${round(m.watts)}W`, `${machine.issues.filter(issue => !issue.resolved).length} unresolved issues`]); }
    if (command === 'scan') return this.runPCDiagnostic(id, 'full') ? ok('Full diagnostics complete') : fail('Scan failed');
    if (command === 'repair') return this.repairPCIssue(id, parts[0], { buy: true }) ? ok(`${parts[0]} repaired`) : fail('Repair rejected');
    if (command === 'tests') return ok(Object.values(DATA.PC_BENCHMARKS).map(test => `${test.id} · ${test.name} · ${test.year}`));
    if (command === 'bench') { const result = this.runPCBenchmark(id, parts[0]); return result ? ok(`${result.fps} ${parts[0] === 'stability' ? 'points' : 'FPS'} · ${result.passed ? 'PASS' : 'FAIL'}`) : fail('Benchmark rejected'); }
    if (command === 'install') return this.installPCOS(id, parts[0], parts[1] === 'experimental') ? ok('OS installed') : fail('OS installation rejected');
    if (command === 'poweroff') { machine.powered = false; this.save(); return ok('Power off'); }
    if (command === 'reboot') return this.rebootPC(id) ? ok('Reboot complete') : fail('Reboot rejected: check temperature and hardware');
    if (command === 'clear') return { ok: true, lines: [], clear: true };
    if (command === 'exit') return ok('Session closed');
    return fail(`${command || '?'}: команда не найдена · help`);
  };

  const baseCampaignTick = proto.tick;
  proto.tick = function () {
    if (this.state.campaignMode === 'pc' && this.state.pc.started) {
      if (!this.state.paused) { this.tickCounter += 1; this.tickPC(1); }
      return;
    }
    return baseCampaignTick.call(this);
  };

  proto.pcWorkshopLoaded = true;
})();
