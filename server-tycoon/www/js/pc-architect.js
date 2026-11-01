/* PC Architect 8.0: конструктор собственных ПК поверх существующей мастерской. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !DATA.PC_PART_CATALOG || Game.prototype.pcArchitectLoaded) return;
  const proto = Game.prototype;
  const types = Object.keys(DATA.PC_PART_TYPES);
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(Number(value) || 0);
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').replace(/[^a-z0-9а-я]+/gi, '');
  const money = value => Math.max(0, round(value));
  const emptyDraft = () => Object.fromEntries(types.map(type => [type, null]));

  proto.pcArchitectState = function () {
    const pc = this.state.pc;
    if (!pc.architect) pc.architect = { draft: emptyDraft(), name: 'Мой Custom PC', completed: 0, selectedYear: pc.year || 1990 };
    if (!pc.architect.draft) pc.architect.draft = emptyDraft();
    types.forEach(type => { if (typeof pc.architect.draft[type] !== 'string') pc.architect.draft[type] = null; });
    if (!pc.architect.name) pc.architect.name = 'Мой Custom PC';
    pc.architect.completed = Math.max(0, Number(pc.architect.completed) || 0);
    pc.architect.selectedYear = clamp(Number(pc.architect.selectedYear) || pc.year || 1990, 1990, 2026);
    return pc.architect;
  };
  const baseArchitectNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseArchitectNormalize.call(this);
    if (this.state.pc) {
      this.pcArchitectState();
      this.state.pc.stats.customBuilds = Math.max(0, Number(this.state.pc.stats.customBuilds) || 0);
    }
    return result;
  };
  const baseArchitectStart = proto.startPCCampaign;
  proto.startPCCampaign = function () { const result = baseArchitectStart.call(this); this.pcArchitectState(); return result; };

  proto.pcPart = function (id) { return DATA.PC_PART_INDEX.get(id) || null; };
  proto.pcDraftParts = function (draft) {
    const source = draft || this.pcArchitectState().draft; const result = {};
    types.forEach(type => { result[type] = source && source[type] ? this.pcPart(source[type]) : null; });
    return result;
  };
  proto.pcDraftCompatibility = function (draft, options = {}) {
    const source = draft || this.pcArchitectState().draft; const p = this.pcDraftParts(source);
    const errors = []; const warnings = []; const missing = types.filter(type => !p[type]);
    const partial = Boolean(options.partial); const ignoreEra = Boolean(options.ignoreEra);
    if (!partial) missing.forEach(type => errors.push(`Выберите: ${DATA.PC_PART_TYPES[type].name.toLowerCase()}`));
    const selected = types.map(type => p[type]).filter(Boolean);
    const total = selected.reduce((sum, part) => sum + part.value, 0);
    const year = selected.length ? Math.max(...selected.map(part => part.year)) : this.state.pc.year;
    const unlockAll = Boolean(this.state.pc.cheats && this.state.pc.cheats.unlockAll);
    if (!ignoreEra && !unlockAll) selected.forEach(part => { if (part.year > this.state.pc.year) errors.push(`${part.name}: доступно с ${part.year} года`); });
    if (p.cpu && p.motherboard && p.cpu.socket !== p.motherboard.socket) errors.push(`Сокет CPU ${p.cpu.socket} не совпадает с платой ${p.motherboard.socket}`);
    if (p.ram && p.motherboard) {
      if (p.ram.ramType !== p.motherboard.ramType) errors.push(`Память ${p.ram.ramType} не подходит к ${p.motherboard.ramType}`);
      if (p.ram.modules > p.motherboard.ramSlots) errors.push(`На плате ${p.motherboard.ramSlots} слота RAM, в комплекте ${p.ram.modules}`);
      if (p.ram.capacityMb > p.motherboard.maxRamMb) errors.push('Объём RAM превышает возможности платы');
    }
    if (p.gpu && p.motherboard && !p.motherboard.gpuInterfaces.includes(p.gpu.interface)) errors.push(`У платы нет интерфейса GPU ${p.gpu.interface}`);
    if (p.storage && p.motherboard && !p.motherboard.storageInterfaces.includes(p.storage.interface)) errors.push(`У платы нет интерфейса накопителя ${p.storage.interface}`);
    if (p.cooling && p.cpu && !p.cooling.sockets.includes(p.cpu.socket)) errors.push(`Кулер не поддерживает ${p.cpu.socket}`);
    if (p.motherboard && p.case && !p.case.formFactors.includes(p.motherboard.formFactor)) errors.push(`Корпус не поддерживает плату ${p.motherboard.formFactor}`);
    if (p.psu && p.case && !p.case.psuFormFactors.includes(p.psu.formFactor)) errors.push(`Корпус не поддерживает PSU ${p.psu.formFactor}`);
    if (p.gpu && p.case && p.gpu.lengthMm > p.case.maxGpuMm) errors.push(`GPU ${p.gpu.lengthMm} мм не помещается в корпус (${p.case.maxGpuMm} мм)`);
    if (p.cooling && p.case && p.cooling.heightMm > p.case.maxCoolerMm) errors.push(`Кулер ${p.cooling.heightMm} мм выше доступных ${p.case.maxCoolerMm} мм`);
    const requiredWatts = Math.ceil(((p.cpu ? p.cpu.tdp : 0) + (p.gpu ? p.gpu.tdp : 0) + 75) * 1.18 / 10) * 10;
    if (p.psu && requiredWatts > p.psu.watts) errors.push(`Нужен PSU от ${requiredWatts} Вт, выбран ${p.psu.watts} Вт`);
    if (p.cooling && p.cpu && p.cooling.capacityWatts < p.cpu.tdp) errors.push(`Охлаждение ${p.cooling.capacityWatts} Вт слабее CPU ${p.cpu.tdp} Вт`);
    const cpuScore = p.cpu ? p.cpu.cpuScore : 0; const gpuScore = p.gpu ? p.gpu.gpuScore : 0;
    const gen = DATA.PC_GENERATIONS.find(item => item.year === year) || DATA.PC_GENERATIONS[0];
    const cooling = p.cooling && p.cpu ? round(gen.cooling * (.68 + p.cooling.capacityWatts / (p.cpu.tdp + 85) * .32) + (p.case ? p.case.fans * 4 : 0)) : gen.cooling;
    const estimatedTemp = p.cpu ? round(24 + requiredWatts * 32 / Math.max(30, cooling)) : 24;
    const noise = (p.cooling ? p.cooling.noise : 0) + (p.case ? p.case.fans * 1.4 : 0) + (p.storage && p.storage.storageType === 'HDD' ? 4 : 0);
    if (!partial && !missing.length) {
      if (p.psu.watts < requiredWatts * 1.2) warnings.push('Запас PSU меньше 20%');
      if (p.cooling.capacityWatts < p.cpu.tdp * 1.15) warnings.push('Охлаждение почти на пределе');
      if (estimatedTemp >= 80) warnings.push(`Расчётная температура высокая: ${estimatedTemp}°C`);
      if (Math.max(...selected.map(part => part.year)) - Math.min(...selected.map(part => part.year)) > 10) warnings.push('Компоненты из очень разных эпох');
    }
    const metrics = { total, year, requiredWatts, cooling, estimatedTemp, noise, cpuScore, gpuScore,
      ramMb: p.ram ? p.ram.capacityMb : 0, storageMb: p.storage ? p.storage.capacityMb : 0,
      performance: round(cpuScore * .55 + gpuScore * .45) };
    return { ok: errors.length === 0 && (partial || missing.length === 0), errors: [...new Set(errors)], warnings: [...new Set(warnings)], missing, parts: p, metrics,
      canAfford: total <= this.state.pc.money, canBuild: errors.length === 0 && missing.length === 0 && total <= this.state.pc.money };
  };
  proto.setPCDraftPart = function (type, id) {
    if (!types.includes(type)) return false;
    if (id != null && !this.pcPart(id)) return false;
    this.pcArchitectState().draft[type] = id || null; this.save(); return true;
  };
  proto.clearPCDraft = function () { this.pcArchitectState().draft = emptyDraft(); this.save(); return true; };
  proto.pcCatalog = function (filters = {}) {
    const type = types.includes(filters.type) ? filters.type : 'cpu';
    const year = filters.year === 'all' ? 'all' : clamp(Number(filters.year) || this.state.pc.year, 1990, 2026);
    let source = year === 'all' ? DATA.PC_PART_CATALOG.filter(part => part.type === type) : (DATA.PC_PART_BY_TYPE_YEAR.get(`${type}:${year}`) || []);
    const query = normalize(filters.query); const minPrice = Number(filters.minPrice); const maxPrice = Number(filters.maxPrice);
    const draft = filters.draft || this.pcArchitectState().draft;
    const baseErrors = filters.compatibleOnly ? new Set(this.pcDraftCompatibility(draft, { partial: true, ignoreEra: true }).errors) : null;
    let items = source.filter(part => {
      if (filters.favoritesOnly && !(this.pcArchitectState().favorites || []).includes(part.id)) return false;
      if (query && !normalize(`${part.name} ${part.sku || ''} ${part.brand} ${part.summary} ${part.socket || ''} ${part.ramType || ''}`).includes(query)) return false;
      if (filters.brand && filters.brand !== 'all' && part.brand !== filters.brand) return false;
      if (filters.socket && filters.socket !== 'all' && part.socket !== filters.socket && !(part.sockets || []).includes(filters.socket)) return false;
      if (filters.ramType && filters.ramType !== 'all' && part.ramType !== filters.ramType) return false;
      if (Number.isFinite(minPrice) && part.value < minPrice) return false;
      if (Number.isFinite(maxPrice) && filters.maxPrice !== '' && part.value > maxPrice) return false;
      if (filters.compatibleOnly) {
        const candidate = Object.assign({}, draft, { [type]: part.id });
        const errors = this.pcDraftCompatibility(candidate, { partial: true, ignoreEra: true }).errors;
        if (errors.some(error => !baseErrors.has(error))) return false;
      }
      return true;
    });
    const sort = filters.sort || 'priceAsc';
    items = items.slice().sort((a, b) => sort === 'priceDesc' ? b.value - a.value : sort === 'name' ? a.name.localeCompare(b.name, 'ru') : sort === 'performance' ? ((b.cpuScore || b.gpuScore || b.readScore || b.capacityWatts || 0) - (a.cpuScore || a.gpuScore || a.readScore || a.capacityWatts || 0)) || a.value - b.value : a.value - b.value || a.name.localeCompare(b.name, 'ru'));
    const perPage = clamp(Number(filters.perPage) || 12, 4, 30); const pages = Math.max(1, Math.ceil(items.length / perPage));
    const page = clamp(Number(filters.page) || 0, 0, pages - 1);
    return { items: items.slice(page * perPage, page * perPage + perPage), total: items.length, page, pages, perPage, type, year };
  };
  proto.pcAutoDraft = function (year = this.state.pc.year) {
    year = clamp(Number(year) || this.state.pc.year, 1990, 2026);
    const list = type => (DATA.PC_PART_BY_TYPE_YEAR.get(`${type}:${year}`) || []).slice().sort((a, b) => a.value - b.value);
    const cpus = list('cpu'); const boards = list('motherboard'); const rams = list('ram'); const gpus = list('gpu');
    const disks = list('storage'); const psus = list('psu'); const cases = list('case'); const coolers = list('cooling');
    for (const motherboard of boards) {
      const cpu = cpus.find(part => part.socket === motherboard.socket); if (!cpu) continue;
      const ram = rams.find(part => part.ramType === motherboard.ramType && part.modules <= motherboard.ramSlots && part.capacityMb <= motherboard.maxRamMb); if (!ram) continue;
      const gpu = gpus.filter(part => motherboard.gpuInterfaces.includes(part.interface)).sort((a, b) => a.gpuScore - b.gpuScore || a.value - b.value)[0]; if (!gpu) continue;
      const storage = disks.find(part => motherboard.storageInterfaces.includes(part.interface)); if (!storage) continue;
      const requiredWatts = Math.ceil((cpu.tdp + gpu.tdp + 75) * 1.18 / 10) * 10;
      const psu = psus.find(part => part.watts >= requiredWatts); if (!psu) continue;
      const compatibleCoolers = coolers.filter(part => part.sockets.includes(cpu.socket) && part.capacityWatts >= cpu.tdp).sort((a, b) => a.heightMm - b.heightMm || a.value - b.value);
      for (const cooling of compatibleCoolers) {
        const box = cases.find(item => item.formFactors.includes(motherboard.formFactor) && item.psuFormFactors.includes(psu.formFactor) && item.maxGpuMm >= gpu.lengthMm && item.maxCoolerMm >= cooling.heightMm);
        if (!box) continue;
        const draft = { cpu: cpu.id, motherboard: motherboard.id, ram: ram.id, gpu: gpu.id, storage: storage.id, psu: psu.id, case: box.id, cooling: cooling.id };
        const report = this.pcDraftCompatibility(draft, { ignoreEra: true });
        if (report.ok) {
          const architect = this.pcArchitectState(); architect.draft = draft; architect.selectedYear = year; this.save(); return report;
        }
      }
    }
    return null;
  };
  proto.buildCustomPC = function (options = {}) {
    const architect = this.pcArchitectState(); const report = this.pcDraftCompatibility(architect.draft, { ignoreEra: Boolean(options.ignoreEra) });
    if (!report.ok) { this.notify(report.errors[0] || 'Сборка не завершена', 'error'); return { ok: false, report }; }
    if (!options.free && this.state.pc.money < report.metrics.total) { this.notify(`Для сборки нужно ${money(report.metrics.total)} ₽`, 'error'); return { ok: false, report }; }
    if (!options.free) this.state.pc.money -= report.metrics.total;
    const name = String(options.name || architect.name || `Custom PC ${architect.completed + 1}`).trim().slice(0, 60) || `Custom PC ${architect.completed + 1}`;
    const machine = this.createPC(report.metrics.year, { origin: 'owned', owner: 'PC Architect', os: null, name });
    const p = report.parts;
    machine.custom = true; machine.partIds = Object.assign({}, architect.draft); machine.architect = { completedDay: this.state.pc.day, total: options.free ? 0 : report.metrics.total };
    machine.cpu = p.cpu.name; machine.cpuScore = p.cpu.cpuScore; machine.ramType = p.ram.ramType; machine.ramMb = p.ram.capacityMb;
    machine.storageType = p.storage.storageType; machine.storageMb = p.storage.capacityMb; machine.gpu = p.gpu.name; machine.gpuScore = p.gpu.gpuScore;
    machine.psuWatts = p.psu.watts; machine.cooling = report.metrics.cooling; machine.health = 100; machine.temp = 24; machine.powered = false; machine.os = null;
    this.state.pc.computers.push(machine); architect.completed += 1; this.state.pc.stats.customBuilds = (this.state.pc.stats.customBuilds || 0) + 1;
    architect.draft = emptyDraft(); this.addPCXP(160); this.advancePCMinutes(120);
    this.pcLog(`PC Architect: собран ${name} из 8 компонентов за ${options.free ? 0 : report.metrics.total} ₽.`, 'good');
    this.notify(`${name} собран · установите ОС и запустите`, 'success'); this.save();
    return { ok: true, machine, report };
  };
  proto.pcCustomParts = function (machine) {
    if (!machine || !machine.custom || !machine.partIds) return [];
    return types.map(type => ({ type, part: this.pcPart(machine.partIds[type]) })).filter(item => item.part);
  };
  proto.pcArchitectLoaded = true;
})();
