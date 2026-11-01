/* Laptop Architect 8.2: сборка, совместимость, батарея и мобильные метрики. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !DATA.LAPTOP_PART_CATALOG || Game.prototype.pcLaptopLoaded) return;
  const proto = Game.prototype; const types = Object.keys(DATA.LAPTOP_PART_TYPES);
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value)); const round = value => Math.round(Number(value) || 0);
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').replace(/[^a-z0-9а-я]+/gi, '');
  const emptyDraft = () => Object.fromEntries(types.map(type => [type, null]));

  proto.pcLaptopState = function () {
    const pc = this.state.pc;
    if (!pc.laptopArchitect) pc.laptopArchitect = { draft: emptyDraft(), name: 'Мой ноутбук', completed: 0, selectedYear: pc.year || 1990, favorites: [] };
    const state = pc.laptopArchitect; if (!state.draft) state.draft = emptyDraft();
    types.forEach(type => { if (typeof state.draft[type] !== 'string') state.draft[type] = null; });
    state.name = String(state.name || 'Мой ноутбук').slice(0, 60); state.completed = Math.max(0, Number(state.completed) || 0);
    state.selectedYear = clamp(Number(state.selectedYear) || pc.year || 1990, 1990, 2026);
    state.favorites = Array.isArray(state.favorites) ? [...new Set(state.favorites.filter(id => DATA.LAPTOP_PART_INDEX.has(id)))] : [];
    return state;
  };
  function laptopMachineState(machine) {
    if (!machine || !machine.laptop) return;
    const batteryPart = machine.partIds && DATA.LAPTOP_PART_INDEX.get(machine.partIds.battery);
    if (!machine.battery || typeof machine.battery !== 'object') machine.battery = {};
    const battery = machine.battery; battery.capacityWh = Math.max(1, Number(battery.capacityWh) || batteryPart && batteryPart.capacityWh || 40);
    battery.health = clamp(Number(battery.health) || 100, 1, 100); battery.charge = clamp(Number(battery.charge) || 0, 0, 100);
    battery.runtimeMinutes = Math.max(1, Number(battery.runtimeMinutes) || 180); battery.chargeMinutes = Math.max(20, Number(battery.chargeMinutes) || batteryPart && batteryPart.chargeMinutes || 120);
    battery.adapterConnected = battery.adapterConnected !== false; battery.lowWarned = Boolean(battery.lowWarned); battery.shutdowns = Math.max(0, Number(battery.shutdowns) || 0);
    const display = machine.partIds && DATA.LAPTOP_PART_INDEX.get(machine.partIds.display);
    if (!machine.laptopDisplay && display) machine.laptopDisplay = { sizeInch: display.sizeInch, resolution: display.resolution, refreshHz: display.refreshHz, watts: display.watts };
  }
  const baseLaptopNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseLaptopNormalize.call(this);
    if (this.state.pc) { this.pcLaptopState(); this.state.pc.computers.forEach(laptopMachineState); this.state.pc.stats.laptopBuilds = Math.max(0, Number(this.state.pc.stats.laptopBuilds) || 0); }
    return result;
  };
  const baseLaptopStart = proto.startPCCampaign;
  proto.startPCCampaign = function () { const result = baseLaptopStart.call(this); this.pcLaptopState(); return result; };

  proto.pcLaptopPart = function (id) { return DATA.LAPTOP_PART_INDEX.get(id) || null; };
  proto.pcLaptopDraftParts = function (draft) {
    const source = draft || this.pcLaptopState().draft; const result = {};
    types.forEach(type => { result[type] = source && source[type] ? this.pcLaptopPart(source[type]) : null; }); return result;
  };
  proto.pcLaptopCompatibility = function (draft, options = {}) {
    const source = draft || this.pcLaptopState().draft; const p = this.pcLaptopDraftParts(source); const errors = []; const warnings = [];
    const missing = types.filter(type => !p[type]); const partial = Boolean(options.partial); const unlockAll = Boolean(this.state.pc.cheats && this.state.pc.cheats.unlockAll);
    if (!partial) missing.forEach(type => errors.push(`Выберите: ${DATA.LAPTOP_PART_TYPES[type].name.toLowerCase()}`));
    const selected = types.map(type => p[type]).filter(Boolean); const total = selected.reduce((sum, part) => sum + part.value, 0);
    const year = selected.length ? Math.max(...selected.map(part => part.year)) : this.state.pc.year;
    if (!options.ignoreEra && !unlockAll) selected.forEach(part => { if (part.year > this.state.pc.year) errors.push(`${part.name}: доступно с ${part.year} года`); });
    if (p.cpu && p.motherboard && p.cpu.socket !== p.motherboard.socket) errors.push(`Мобильный сокет ${p.cpu.socket} не совпадает с платой ${p.motherboard.socket}`);
    if (p.ram && p.motherboard) {
      if (p.ram.ramType !== p.motherboard.ramType) errors.push(`SO-DIMM ${p.ram.ramType} не подходит к ${p.motherboard.ramType}`);
      if (p.ram.modules > p.motherboard.ramSlots) errors.push(`На плате ${p.motherboard.ramSlots} слота SO-DIMM, в комплекте ${p.ram.modules}`);
      if (p.ram.capacityMb > p.motherboard.maxRamMb) errors.push('Объём SO-DIMM превышает возможности платы');
    }
    if (p.gpu && p.motherboard && !p.motherboard.gpuInterfaces.includes(p.gpu.interface)) errors.push(`Плата не поддерживает мобильный GPU ${p.gpu.interface}`);
    if (p.storage && p.motherboard && !p.motherboard.storageInterfaces.includes(p.storage.interface)) errors.push(`Плата не поддерживает накопитель ${p.storage.interface}`);
    if (p.cooling && p.cpu && !p.cooling.sockets.includes(p.cpu.socket)) errors.push(`Мобильный кулер не поддерживает ${p.cpu.socket}`);
    if (p.cooling && p.cpu && p.gpu && p.cooling.capacityWatts < p.cpu.tdp + p.gpu.tdp * .25) errors.push('Мобильное охлаждение не покрывает теплопакет CPU/GPU');
    if (p.cooling && p.case && p.cooling.heightMm > p.case.maxCoolerMm) errors.push(`Кулер ${p.cooling.heightMm} мм не помещается в шасси (${p.case.maxCoolerMm} мм)`);
    if (p.display && p.case) {
      if (p.display.sizeInch > p.case.maxDisplayInch) errors.push(`Дисплей ${p.display.sizeInch}\" больше шасси ${p.case.maxDisplayInch}\"`);
      if (!p.case.displayConnectors.includes(p.display.connector)) errors.push(`Шасси не поддерживает дисплей ${p.display.connector}`);
    }
    if (p.battery && p.case && p.battery.voltage !== p.case.batteryVoltage) errors.push(`Батарея ${p.battery.voltage}V не совпадает с шасси ${p.case.batteryVoltage}V`);
    if (p.psu && p.case && p.psu.connector !== p.case.adapterConnector) errors.push(`Адаптер ${p.psu.connector} не подходит к шасси ${p.case.adapterConnector}`);
    const requiredWatts = Math.ceil(((p.cpu ? p.cpu.tdp : 0) + (p.gpu ? p.gpu.tdp : 0) + (p.display ? p.display.watts : 0) + 25) * 1.15 / 5) * 5;
    if (p.psu && requiredWatts > p.psu.watts) errors.push(`Нужен адаптер от ${requiredWatts} Вт, выбран ${p.psu.watts} Вт`);
    const gen = DATA.PC_GENERATIONS.find(item => item.year === year) || DATA.PC_GENERATIONS[0];
    const cooling = p.cooling ? round(gen.cooling * .45 + p.cooling.capacityWatts * .45 + (p.case ? p.case.fans * 3 : 0)) : round(gen.cooling * .45);
    const estimatedTemp = p.cpu ? round(24 + requiredWatts * 25 / Math.max(30, cooling)) : 24;
    const noise = (p.cooling ? p.cooling.noise : 0) + (p.case ? p.case.fans : 0) + (p.storage && p.storage.storageType === 'HDD' ? 4 : 0);
    const typicalWatts = 12 + (p.cpu ? p.cpu.tdp * .35 : 0) + (p.gpu ? p.gpu.tdp * .25 : 0) + (p.display ? p.display.watts : 0);
    const runtimeMinutes = p.battery ? round(p.battery.capacityWh * 60 / Math.max(8, typicalWatts)) : 0;
    const weightKg = Number(((p.case ? p.case.weightKg : 0) + (p.battery ? p.battery.weightKg : 0)).toFixed(2));
    if (!partial && !missing.length) {
      if (p.psu.watts < requiredWatts * 1.15) warnings.push('Запас адаптера меньше 15%');
      if (runtimeMinutes < 90) warnings.push(`Автономность всего ${runtimeMinutes} мин`);
      if (estimatedTemp >= 80) warnings.push(`Расчётная температура высокая: ${estimatedTemp}°C`);
      if (weightKg > 3.5) warnings.push(`Тяжёлый ноутбук: ${weightKg} кг`);
    }
    const metrics = { total, year, requiredWatts, cooling, estimatedTemp, noise, runtimeMinutes, typicalWatts, weightKg,
      cpuScore: p.cpu ? p.cpu.cpuScore : 0, gpuScore: p.gpu ? p.gpu.gpuScore : 0, ramMb: p.ram ? p.ram.capacityMb : 0,
      storageMb: p.storage ? p.storage.capacityMb : 0, storageType: p.storage ? p.storage.storageType : 'HDD',
      performance: round((p.cpu ? p.cpu.cpuScore : 0) * .55 + (p.gpu ? p.gpu.gpuScore : 0) * .45) };
    return { ok: errors.length === 0 && (partial || missing.length === 0), errors: [...new Set(errors)], warnings: [...new Set(warnings)], missing, parts: p, metrics,
      canAfford: total <= this.state.pc.money, canBuild: errors.length === 0 && missing.length === 0 && total <= this.state.pc.money };
  };
  proto.setLaptopDraftPart = function (type, id) {
    if (!types.includes(type)) return false; if (id != null && !this.pcLaptopPart(id)) return false;
    this.pcLaptopState().draft[type] = id || null; this.save(); return true;
  };
  proto.clearLaptopDraft = function () { this.pcLaptopState().draft = emptyDraft(); this.save(); return true; };
  proto.pcToggleLaptopFavorite = function (id) {
    if (!this.pcLaptopPart(id)) return false; const favorites = this.pcLaptopState().favorites; const index = favorites.indexOf(id);
    if (index >= 0) favorites.splice(index, 1); else favorites.push(id); this.save(); return index < 0;
  };
  proto.pcLaptopCatalog = function (filters = {}) {
    const type = types.includes(filters.type) ? filters.type : 'cpu'; const year = filters.year === 'all' ? 'all' : clamp(Number(filters.year) || this.state.pc.year, 1990, 2026);
    let source = year === 'all' ? DATA.LAPTOP_PART_CATALOG.filter(part => part.type === type) : DATA.LAPTOP_PART_BY_TYPE_YEAR.get(`${type}:${year}`) || [];
    const query = normalize(filters.query); const draft = filters.draft || this.pcLaptopState().draft;
    const baseErrors = filters.compatibleOnly ? new Set(this.pcLaptopCompatibility(draft, { partial: true, ignoreEra: true }).errors) : null;
    let items = source.filter(part => {
      if (filters.favoritesOnly && !this.pcLaptopState().favorites.includes(part.id)) return false;
      if (query && !normalize(`${part.name} ${part.sku} ${part.brand} ${part.summary} ${part.socket || ''} ${part.interface || ''}`).includes(query)) return false;
      if (filters.brand && filters.brand !== 'all' && part.brand !== filters.brand) return false;
      if (filters.socket && filters.socket !== 'all' && part.socket !== filters.socket && part.interface !== filters.socket) return false;
      if (filters.compatibleOnly) {
        const candidate = Object.assign({}, draft, { [type]: part.id }); const errors = this.pcLaptopCompatibility(candidate, { partial: true, ignoreEra: true }).errors;
        if (errors.some(error => !baseErrors.has(error))) return false;
      }
      return true;
    });
    const sort = filters.sort || 'priceAsc'; items = items.slice().sort((a, b) => sort === 'priceDesc' ? b.value - a.value : sort === 'performance' ? ((b.cpuScore || b.gpuScore || b.capacityWh || b.capacityWatts || 0) - (a.cpuScore || a.gpuScore || a.capacityWh || a.capacityWatts || 0)) || a.value - b.value : sort === 'name' ? a.name.localeCompare(b.name, 'ru') : a.value - b.value || a.name.localeCompare(b.name, 'ru'));
    const perPage = clamp(Number(filters.perPage) || 12, 4, 30); const pages = Math.max(1, Math.ceil(items.length / perPage)); const page = clamp(Number(filters.page) || 0, 0, pages - 1);
    return { items: items.slice(page * perPage, page * perPage + perPage), total: items.length, pages, page, perPage, type, year };
  };
  proto.pcAutoLaptopDraft = function (year = this.state.pc.year) {
    year = clamp(Number(year) || this.state.pc.year, 1990, 2026); const list = type => (DATA.LAPTOP_PART_BY_TYPE_YEAR.get(`${type}:${year}`) || []).slice().sort((a, b) => a.value - b.value);
    const cpus = list('cpu'); const boards = list('motherboard'); const rams = list('ram'); const gpus = list('gpu'); const disks = list('storage'); const adapters = list('psu');
    const cases = list('case'); const coolers = list('cooling'); const displays = list('display'); const batteries = list('battery');
    for (const motherboard of boards) {
      const cpu = cpus.find(part => part.socket === motherboard.socket); if (!cpu) continue;
      const ram = rams.find(part => part.ramType === motherboard.ramType && part.modules <= motherboard.ramSlots && part.capacityMb <= motherboard.maxRamMb); if (!ram) continue;
      const gpu = gpus.filter(part => motherboard.gpuInterfaces.includes(part.interface)).sort((a, b) => a.gpuScore - b.gpuScore || a.value - b.value)[0]; if (!gpu) continue;
      const storage = disks.find(part => motherboard.storageInterfaces.includes(part.interface)); if (!storage) continue;
      for (const box of cases) {
        const display = displays.find(part => part.sizeInch <= box.maxDisplayInch && box.displayConnectors.includes(part.connector)); if (!display) continue;
        const battery = batteries.find(part => part.voltage === box.batteryVoltage); if (!battery) continue;
        const requiredWatts = Math.ceil((cpu.tdp + gpu.tdp + display.watts + 25) * 1.15 / 5) * 5;
        const psu = adapters.find(part => part.connector === box.adapterConnector && part.watts >= requiredWatts); if (!psu) continue;
        const cooling = coolers.find(part => part.sockets.includes(cpu.socket) && part.heightMm <= box.maxCoolerMm && part.capacityWatts >= cpu.tdp + gpu.tdp * .25); if (!cooling) continue;
        const draft = { cpu: cpu.id, motherboard: motherboard.id, ram: ram.id, gpu: gpu.id, storage: storage.id, psu: psu.id, case: box.id, cooling: cooling.id, display: display.id, battery: battery.id };
        const report = this.pcLaptopCompatibility(draft, { ignoreEra: true });
        if (report.ok) { const state = this.pcLaptopState(); state.draft = draft; state.selectedYear = year; this.save(); return report; }
      }
    }
    return null;
  };
  proto.buildLaptopPC = function (options = {}) {
    const state = this.pcLaptopState(); const report = this.pcLaptopCompatibility(state.draft, { ignoreEra: Boolean(options.ignoreEra) });
    if (!report.ok) { this.notify(report.errors[0] || 'Сборка ноутбука не завершена', 'error'); return { ok: false, report }; }
    const plan = options.useInventory !== false && this.pcInventoryPlan ? this.pcInventoryPlan(report.parts, 'laptop') : { uids: [], items: [], discount: 0, healthTotal: 0 };
    const total = Math.max(0, report.metrics.total - plan.discount);
    if (!options.free && this.state.pc.money < total) { this.notify(`Доплата за ноутбук: ${total} ₽`, 'error'); return { ok: false, report, plan }; }
    if (!options.free) this.state.pc.money -= total;
    const name = String(options.name || state.name || `Laptop ${state.completed + 1}`).trim().slice(0, 60) || `Laptop ${state.completed + 1}`;
    const machine = this.createPC(report.metrics.year, { origin: 'owned', owner: 'Laptop Architect', os: null, name }); const p = report.parts;
    machine.custom = true; machine.laptop = true; machine.formFactor = 'laptop'; machine.partIds = Object.assign({}, state.draft);
    machine.architect = { completedDay: this.state.pc.day, total: options.free ? 0 : total, catalogTotal: report.metrics.total, usedParts: plan.uids.length, kind: 'laptop' };
    machine.usedComponents = plan.items.map(item => ({ uid: item.uid, catalogId: item.catalogId, condition: item.condition, health: item.health, paid: item.paid }));
    machine.cpu = p.cpu.name; machine.cpuScore = p.cpu.cpuScore; machine.ramType = p.ram.ramType; machine.ramMb = p.ram.capacityMb;
    machine.storageType = p.storage.storageType; machine.storageMb = p.storage.capacityMb; machine.gpu = p.gpu.name; machine.gpuScore = p.gpu.gpuScore;
    machine.psuWatts = p.psu.watts; machine.cooling = report.metrics.cooling; machine.temp = 24; machine.powered = false; machine.os = null;
    machine.health = plan.items.length ? round((plan.healthTotal + (types.length - plan.items.length) * 100) / types.length) : 100;
    machine.laptopDisplay = { sizeInch: p.display.sizeInch, resolution: p.display.resolution, refreshHz: p.display.refreshHz, watts: p.display.watts };
    const usedBattery = plan.items.find(item => item.catalogId === p.battery.id); const batteryHealth = usedBattery ? usedBattery.health : 100;
    machine.battery = { charge: 100, capacityWh: p.battery.capacityWh, health: batteryHealth, runtimeMinutes: Math.max(1, round(report.metrics.runtimeMinutes * batteryHealth / 100)),
      chargeMinutes: p.battery.chargeMinutes, adapterConnected: true, lowWarned: false, shutdowns: 0 };
    this.state.pc.computers.push(machine); if (this.pcConsumeInventory) this.pcConsumeInventory(plan);
    state.completed += 1; this.state.pc.stats.laptopBuilds = (this.state.pc.stats.laptopBuilds || 0) + 1; this.state.pc.stats.customBuilds = (this.state.pc.stats.customBuilds || 0) + 1; this.state.pc.stats.pcPartsUsed = (this.state.pc.stats.pcPartsUsed || 0) + plan.uids.length;
    state.draft = emptyDraft(); this.addPCXP(200); this.advancePCMinutes(150);
    this.pcLog(`Laptop Architect: собран ${name} из 10 компонентов; склад ${plan.uids.length}, доплата ${options.free ? 0 : total} ₽.`, 'good');
    this.notify(`${name} собран · батарея 100%, установите ОС`, 'success'); this.save(); return { ok: true, machine, report, plan };
  };
  const baseCustomParts = proto.pcCustomParts;
  proto.pcCustomParts = function (machine) {
    if (!machine || !machine.laptop) return baseCustomParts.call(this, machine);
    return types.map(type => ({ type, part: this.pcLaptopPart(machine.partIds && machine.partIds[type]) })).filter(item => item.part);
  };
  proto.toggleLaptopAdapter = function (id) {
    const machine = this.getPC(id); if (!machine || !machine.laptop) return false; laptopMachineState(machine);
    machine.battery.adapterConnected = !machine.battery.adapterConnected;
    this.pcLog(`${machine.name}: адаптер ${machine.battery.adapterConnected ? 'подключен' : 'отключен'}; заряд ${round(machine.battery.charge)}%.`);
    this.save(); return machine.battery.adapterConnected;
  };
  const baseLaptopPower = proto.togglePCPower;
  proto.togglePCPower = function (id) {
    const machine = this.getPC(id); if (machine && machine.laptop && !machine.powered) {
      laptopMachineState(machine); if (!machine.battery.adapterConnected && machine.battery.charge <= 0) { this.notify('Батарея пуста · подключите адаптер', 'error'); return false; }
    }
    return baseLaptopPower.call(this, id);
  };
  const baseLaptopReboot = proto.rebootPC;
  proto.rebootPC = function (id) {
    const machine = this.getPC(id); if (machine && machine.laptop) { laptopMachineState(machine); if (!machine.battery.adapterConnected && machine.battery.charge <= 0) { this.notify('Батарея пуста · подключите адаптер', 'error'); return false; } }
    return baseLaptopReboot.call(this, id);
  };
  const baseLaptopMetrics = proto.pcMetrics;
  proto.pcMetrics = function (machine) {
    const result = baseLaptopMetrics.call(this, machine); if (!machine || !machine.laptop) return result; laptopMachineState(machine);
    if (machine.powered) {
      const displayWatts = machine.laptopDisplay ? machine.laptopDisplay.watts : 5; result.watts += displayWatts;
      result.targetTemp += displayWatts * 16 / Math.max(25, machine.cooling); result.noise = clamp(result.noise * .82, 8, 75);
    }
    result.batteryCharge = machine.battery.charge; result.adapterConnected = machine.battery.adapterConnected; return result;
  };
  const baseLaptopTick = proto.tickPC;
  proto.tickPC = function (realSeconds = 1) {
    const result = baseLaptopTick.call(this, realSeconds); const pc = this.state.pc;
    if (!pc || !pc.started || this.state.paused) return result;
    const gameMinutes = Math.max(0, Number(realSeconds) || 0) * pc.speed * 12 / 45;
    pc.computers.forEach(machine => {
      if (!machine.laptop) return; laptopMachineState(machine); const battery = machine.battery;
      if (battery.adapterConnected) {
        battery.charge = clamp(battery.charge + gameMinutes * 100 / battery.chargeMinutes, 0, 100);
      } else if (machine.powered) {
        const metrics = this.pcMetrics(machine); const drainWh = Math.max(5, metrics.watts) * gameMinutes / 60;
        battery.charge = clamp(battery.charge - drainWh / battery.capacityWh * 100, 0, 100);
        if (battery.charge <= 15 && !battery.lowWarned) { battery.lowWarned = true; this.notify(`${machine.name}: осталось ${round(battery.charge)}% батареи`, 'warning'); }
        if (battery.charge <= 0) {
          machine.powered = false; machine.load = 0; battery.shutdowns += 1;
          this.pcLog(`${machine.name}: батарея разряжена, питание отключено.`, 'bad'); this.notify(`${machine.name}: батарея пуста · нужен адаптер`, 'error');
        }
      }
      if (battery.charge > 20) battery.lowWarned = false;
    });
    return result;
  };
  proto.pcLaptopLoaded = true;
})();
