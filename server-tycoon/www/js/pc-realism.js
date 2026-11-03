/* Real Hardware & OS 8.4: универсальный tuning, prebuilt mobile-Авито, апгрейды, USB setup и совместимость. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !DATA.pcRealismDataLoaded || Game.prototype.pcRealismLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(Number(value) || 0);
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').replace(/[^a-z0-9а-я]+/gi, '');
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const randomFor = initial => { let x = initial >>> 0 || 1; return () => { x ^= x << 13; x ^= x >>> 17; x ^= x << 5; return (x >>> 0) / 4294967296; }; };
  const ocValues = [0,10,20]; const fanValues = ['silent','balanced','turbo'];
  const installPhases = ['media','writing','bios','copy','complete','error'];
  const conditions = {
    new:{ id:'new',name:'Новый',multiplier:.96,health:100 }, excellent:{ id:'excellent',name:'Отличное',multiplier:.79,health:91 },
    good:{ id:'good',name:'Хорошее',multiplier:.61,health:77 }, used:{ id:'used',name:'Б/у',multiplier:.43,health:59 }
  };
  const sellers = ['Алексей · локально','Ноутбуки б/у','Ретро-мастерская','Склад организации','Частный продавец','Mobile Resale'];
  function tuning(source) {
    const result = Object.assign({ cpu:0,gpu:0,fan:'balanced' }, source || {});
    result.cpu = ocValues.includes(Number(result.cpu)) ? Number(result.cpu) : 0;
    result.gpu = ocValues.includes(Number(result.gpu)) ? Number(result.gpu) : 0;
    result.fan = fanValues.includes(result.fan) ? result.fan : 'balanced';
    return result;
  }
  function storageFactor(machine) { return machine.storageType === 'NVMe' ? .45 : machine.storageType === 'SSD' ? .65 : machine.storageType === 'eMMC' ? 1.3 : 1; }
  function ramGeneration(machine) { return String(machine.ramType || '').replace(/\s+SO-DIMM$/i,'').replace('DDR3L','DDR3'); }
  function storageInterfaces(machine) {
    const source = machine.upgrade && Array.isArray(machine.upgrade.storageInterfaces) ? machine.upgrade.storageInterfaces : ['SATA'];
    const aliases = { PATA:'IDE','PATA ZIF':'IDE','mini-PCIe SATA':'mSATA' };
    return [...new Set(source.map(item => aliases[item] || item))];
  }

  proto.pcOSPerformance = function (machine) {
    const os = machine && machine.os ? this.pcOS(machine.os) : null;
    if (!machine || !os) return { os:null,responsiveness:1,speedFactor:1,bootSeconds:2,installSeconds:900,storageFactor:1,cpuRatio:1,ramRatio:1 };
    const recommendedCpu = Math.max(1, Number(os.recommendedCpu) || Math.max(20, os.minRamMb));
    const recommendedRam = Math.max(1, Number(os.recommendedRamMb) || os.minRamMb);
    const cpuRatio = Math.max(.01, machine.cpuScore / recommendedCpu);
    const ramRatio = Math.max(.01, machine.ramMb / recommendedRam);
    const diskFactor = storageFactor(machine);
    const responsiveness = clamp(cpuRatio * .62 + Math.min(2,ramRatio) * .26 + (1 / diskFactor) * .12, .03, 2);
    const speedFactor = clamp(.2 + responsiveness * .8, .05, 1);
    const bootSeconds = clamp((Number(os.bootSeconds) || 45) * Math.max(.35,1 / cpuRatio) * diskFactor / clamp(Math.min(2,ramRatio),.55,1.25),2,7200);
    const installSeconds = clamp((Number(os.installSeconds) || 1800) * Math.max(.75,recommendedCpu / Math.max(1,machine.cpuScore)) * diskFactor,300,14400);
    return { os,responsiveness,speedFactor,bootSeconds:Math.ceil(bootSeconds),installSeconds:Math.ceil(installSeconds),storageFactor:diskFactor,cpuRatio,ramRatio };
  };
  proto.syncPCOSPerformance = function (machine, preserveBase = true) {
    if (!machine) return null; const profile = this.pcOSPerformance(machine);
    if (!preserveBase || !Number.isFinite(machine.osBasePenalty)) machine.osBasePenalty = Number.isFinite(machine.osPenalty) ? clamp(machine.osPenalty,.05,1) : 1;
    machine.osPenalty = machine.os ? clamp(machine.osBasePenalty * profile.speedFactor,.05,1) : 1;
    machine.osBootSeconds = profile.bootSeconds; machine.osResponsiveness = Number(profile.responsiveness.toFixed(3));
    return profile;
  };

  proto.pcPrebuiltState = function () {
    const pc = this.state.pc;
    if (!pc.prebuiltMarket) pc.prebuiltMarket = { listings:[],refreshes:0,lastDay:0,soldIds:[] };
    const market = pc.prebuiltMarket; market.refreshes = Math.max(0,Number(market.refreshes) || 0); market.lastDay = Math.max(0,Number(market.lastDay) || 0);
    market.soldIds = Array.isArray(market.soldIds) ? [...new Set(market.soldIds.map(String))] : [];
    market.listings = Array.isArray(market.listings) ? market.listings.filter(item => item && DATA.PREBUILT_MOBILE_INDEX.has(item.modelId)).slice(0,80) : [];
    return market;
  };
  function makePrebuiltListing(model, year, salt = 0) {
    const seed = hash(`${model.id}:${salt}`); const keys = Object.keys(conditions); const condition = conditions[keys[seed % keys.length]];
    const age = Math.max(0,year - model.year); const depreciation = Math.max(.035,1 - age * .075);
    const price = Math.max(350,round(model.msrp * condition.multiplier * depreciation * (.9 + (seed >>> 8) % 21 / 100)));
    return { id:`mobile_${model.id}_${condition.id}_${seed % 9973}`,modelId:model.id,year:model.year,kind:model.kind,condition:condition.id,
      health:condition.health,price,seller:sellers[(seed >>> 4) % sellers.length],createdDay:0,sold:false };
  }
  proto.pcPrebuiltListing = function (modelId, salt = 0) { const model = DATA.PREBUILT_MOBILE_INDEX.get(modelId); return model ? makePrebuiltListing(model,this.state.pc.year,salt) : null; };
  proto.refreshPrebuiltMarket = function () {
    const market = this.pcPrebuiltState(); const year = this.state.pc.year; const random = randomFor(hash(`mobile-market:${this.state.pc.day}:${market.refreshes}`));
    const available = DATA.PREBUILT_MOBILE_CATALOG.filter(model => model.year <= year); const listings = []; const ids = new Set(); let guard = 0;
    while (available.length && listings.length < Math.min(24,available.length) && guard++ < 200) {
      const model = available[Math.floor(random() * available.length)]; const listing = makePrebuiltListing(model,year,market.refreshes * 37 + guard);
      if (ids.has(listing.id) || market.soldIds.includes(listing.id)) continue; ids.add(listing.id); listing.createdDay = this.state.pc.day; listings.push(listing);
    }
    market.listings = listings; market.refreshes += 1; market.lastDay = this.state.pc.day; this.save(); return listings;
  };
  function ensurePrebuiltMarket(game) { const market = game.pcPrebuiltState(); if (!market.listings.length) game.refreshPrebuiltMarket(); return market; }
  proto.pcFindPrebuiltListings = function (query = '', filters = {}) {
    const market = ensurePrebuiltMarket(this); const text = normalize(query); const year = this.state.pc.year; const unlockAll = this.state.pc.cheats && this.state.pc.cheats.unlockAll;
    const filtered = Boolean(text || filters.kind && filters.kind !== 'all' || filters.brand && filters.brand !== 'all' || filters.year || filters.minPrice !== '' && filters.minPrice != null || filters.maxPrice !== '' && filters.maxPrice != null);
    const sold = new Set(market.soldIds);
    const matches = (model,listing) => {
      if (sold.has(listing.id)) return false; if (!unlockAll && model.year > year) return false;
      const brand = model.name.split(' ')[0];
      if (text && !normalize(`${model.name} ${model.cpu} ${model.gpu} ${model.kind} ${brand}`).includes(text)) return false;
      if (filters.kind && filters.kind !== 'all' && model.kind !== filters.kind) return false;
      if (filters.brand && filters.brand !== 'all' && brand !== filters.brand) return false;
      if (filters.year && Number(filters.year) !== model.year) return false;
      if (filters.minPrice !== '' && filters.minPrice != null && listing.price < Number(filters.minPrice)) return false;
      if (filters.maxPrice !== '' && filters.maxPrice != null && listing.price > Number(filters.maxPrice)) return false;
      return true;
    };
    let results;
    if (filtered) {
      results = DATA.PREBUILT_MOBILE_CATALOG.map((model,index) => ({ model,listing:makePrebuiltListing(model,year,index) })).filter(item => matches(item.model,item.listing)).map(item => item.listing);
      if (text) results.sort((a,b) => { const am = DATA.PREBUILT_MOBILE_INDEX.get(a.modelId); const bm = DATA.PREBUILT_MOBILE_INDEX.get(b.modelId); const an = normalize(am.name).startsWith(text) ? 0 : 1; const bn = normalize(bm.name).startsWith(text) ? 0 : 1; return an - bn || a.price - b.price; });
    } else results = market.listings.filter(listing => matches(DATA.PREBUILT_MOBILE_INDEX.get(listing.modelId),listing));
    const sort = filters.sort || 'relevance'; results.sort((a,b) => sort === 'priceAsc' ? a.price - b.price : sort === 'priceDesc' ? b.price - a.price : sort === 'yearDesc' ? b.year - a.year || a.price - b.price : sort === 'yearAsc' ? a.year - b.year || a.price - b.price : sort === 'name' ? DATA.PREBUILT_MOBILE_INDEX.get(a.modelId).name.localeCompare(DATA.PREBUILT_MOBILE_INDEX.get(b.modelId).name,'ru') : 0);
    results = results.slice(0,clamp(Number(filters.limit) || 60,1,60)); const ids = new Set(results.map(item => item.id));
    market.listings = [...results,...market.listings.filter(item => !ids.has(item.id))].slice(0,80); return results;
  };
  proto.buyPrebuiltMobile = function (listingId) {
    const market = ensurePrebuiltMarket(this); const listing = market.listings.find(item => item.id === listingId); const model = listing && DATA.PREBUILT_MOBILE_INDEX.get(listing.modelId);
    if (!listing || !model || market.soldIds.includes(listingId)) { this.notify('Готовый mobile-ПК уже продан или лот обновлён','warning'); return { ok:false }; }
    if (model.year > this.state.pc.year && !(this.state.pc.cheats && this.state.pc.cheats.unlockAll)) { this.notify(`Модель доступна с ${model.year} года`,'warning'); return { ok:false }; }
    if (this.state.pc.money < listing.price) { this.notify(`Нужно ${listing.price} ₽`,'error'); return { ok:false }; }
    this.state.pc.money -= listing.price;
    const machine = this.createPC(model.year,{ origin:'owned',owner:'PC Авито · готовый mobile-ПК',name:model.name });
    Object.assign(machine,{ name:model.name,year:model.year,cpu:model.cpu,cpuScore:model.cpuScore,ramType:model.ramType,ramMb:model.ramMb,
      storageType:model.storageType,storageMb:model.storageMb,gpu:model.gpu,gpuScore:model.gpuScore,psuWatts:model.psuWatts,cooling:model.cooling,
      laptop:true,prebuilt:true,custom:false,formFactor:model.kind,prebuiltModelId:model.id,purchasePrice:listing.price,msrp:model.msrp,
      health:listing.health,weightKg:model.weightKg,upgrade:{ cpuUpgradeable:model.cpuUpgradeable,cpuSocket:model.cpuSocket,maxCpuTdp:model.maxCpuTdp,
        ramUpgradeable:model.ramUpgradeable !== false,storageUpgradeable:model.storageUpgradeable !== false,ramSlots:model.ramSlots,maxRamMb:model.maxRamMb,storageInterfaces:model.storageInterfaces.slice() },
      laptopDisplay:{ sizeInch:model.displaySize,resolution:model.displayResolution,refreshHz:model.refreshHz,watts:Number((2 + model.displaySize * .35).toFixed(1)) },
      battery:{ charge:listing.condition === 'new' ? 100 : Math.max(35,listing.health),capacityWh:model.batteryWh,health:listing.health,runtimeMinutes:round(model.batteryWh * 60 / Math.max(9,12 + model.cpuScore * .025) * listing.health / 100),chargeMinutes:120,adapterConnected:true,lowWarned:false,shutdowns:0 },
      prebuiltParts:{},upgradeHistory:[] });
    machine.osBasePenalty = 1; this.state.pc.computers.push(machine); listing.sold = true; market.soldIds.push(listingId);
    this.state.pc.stats.prebuiltBought = (this.state.pc.stats.prebuiltBought || 0) + 1;
    this.syncPCOSPerformance(machine); this.pcRealismMachineState(machine); this.pcLog(`Mobile Авито: ${model.name} куплен за ${listing.price} ₽ · ${conditions[listing.condition].name}.`,'good');
    this.notify(`${model.name} добавлен в мастерскую · RAM/диск можно улучшать`,'success'); this.save(); return { ok:true,machine,listing,model };
  };

  proto.pcPrebuiltUpgradeOptions = function (machineOrId, type) {
    const machine = typeof machineOrId === 'string' ? this.getPC(machineOrId) : machineOrId;
    if (!machine || !machine.prebuilt || !machine.laptop || !['ram','storage','cpu'].includes(type)) return [];
    const year = this.state.pc.year; const unlock = this.state.pc.cheats && this.state.pc.cheats.unlockAll;
    return DATA.LAPTOP_PART_CATALOG.filter(part => {
      if (part.type !== type || !unlock && part.year > year) return false;
      if (type === 'ram') return machine.upgrade.ramUpgradeable !== false && part.ramType === ramGeneration(machine) && part.capacityMb > machine.ramMb && part.capacityMb <= machine.upgrade.maxRamMb && part.modules <= machine.upgrade.ramSlots;
      if (type === 'storage') return machine.upgrade.storageUpgradeable !== false && part.capacityMb > machine.storageMb && storageInterfaces(machine).includes(part.interface);
      return machine.upgrade.cpuUpgradeable && part.socket === machine.upgrade.cpuSocket && part.cpuScore > machine.cpuScore && part.tdp <= machine.upgrade.maxCpuTdp;
    }).sort((a,b) => type === 'cpu' ? a.cpuScore - b.cpuScore || a.value - b.value : type === 'ram' ? a.capacityMb - b.capacityMb || a.value - b.value : a.capacityMb - b.capacityMb || a.value - b.value).slice(0,12);
  };
  proto.upgradePrebuiltMobile = function (id,type,partId) {
    const machine = this.getPC(id); const part = DATA.LAPTOP_PART_INDEX.get(partId);
    if (!machine || !machine.prebuilt || !part) return { ok:false };
    if (machine.origin !== 'owned') { this.notify('Апгрейд доступен только на собственном mobile-ПК','warning'); return { ok:false }; }
    if (machine.powered) { this.notify('Выключите ноутбук и отключите адаптер перед апгрейдом','warning'); return { ok:false }; }
    if (machine.battery && machine.battery.adapterConnected) { this.notify('Отключите адаптер перед разборкой','warning'); return { ok:false }; }
    if (!this.pcPrebuiltUpgradeOptions(machine,type).some(item => item.id === partId)) { this.notify(type === 'cpu' ? 'CPU распаян, сокет/TDP не поддерживается или это не апгрейд' : 'Деталь несовместима с разъёмами, лимитом RAM или текущей конфигурацией','error'); return { ok:false }; }
    const inventory = this.state.pc.componentInventory || []; const owned = inventory.find(item => item.family === 'laptop' && item.catalogId === partId);
    const cost = owned ? 0 : Math.max(1,round(part.value * .9)); if (this.state.pc.money < cost) { this.notify(`Нужно ${cost} ₽ для совместимой детали`,'error'); return { ok:false,cost }; }
    const before = { cpu:machine.cpu,cpuScore:machine.cpuScore,ramMb:machine.ramMb,ramType:machine.ramType,storageMb:machine.storageMb,storageType:machine.storageType };
    if (owned) this.state.pc.componentInventory = inventory.filter(item => item.uid !== owned.uid); else this.state.pc.money -= cost;
    if (type === 'ram') { machine.ramMb = part.capacityMb; machine.ramType = part.ramType; }
    else if (type === 'storage') { machine.storageMb = part.capacityMb; machine.storageType = part.storageType; }
    else { machine.cpu = part.name; machine.cpuScore = part.cpuScore; machine.upgrade.cpuSocket = part.socket; }
    if (!machine.prebuiltParts) machine.prebuiltParts = {}; machine.prebuiltParts[type] = part.id;
    if (!Array.isArray(machine.upgradeHistory)) machine.upgradeHistory = [];
    machine.upgradeHistory.push({ day:this.state.pc.day,year:this.state.pc.year,type,partId,before,cost,inventoryUid:owned ? owned.uid : null });
    if (machine.battery) machine.battery.runtimeMinutes = Math.max(30,round(machine.battery.runtimeMinutes * (type === 'cpu' ? before.cpuScore / part.cpuScore : 1)));
    this.syncPCOSPerformance(machine); this.advancePCMinutes(type === 'storage' ? 90 : 60); this.state.pc.stats.prebuiltUpgrades = (this.state.pc.stats.prebuiltUpgrades || 0) + 1;
    this.pcLog(`${machine.name}: ${type} → ${part.name}; ${owned ? 'деталь со склада' : `${cost} ₽`}.`,'good'); this.notify(`${part.name} установлен · конфигурация и OS speed обновлены`,'success'); this.save();
    return { ok:true,machine,part,cost,usedInventory:Boolean(owned) };
  };

  proto.pcHardware = function (machineOrId) {
    const machine = typeof machineOrId === 'string' ? this.getPC(machineOrId) : machineOrId; if (!machine) return [];
    if (machine.custom) return this.pcCustomParts(machine).map(({ type,part }) => ({ type,name:part.name,summary:part.summary || '',year:part.year,part,upgradeable:false }));
    const specs = [
      ['cpu',machine.cpu,`CPU score ${round(machine.cpuScore)}${machine.laptop ? ` · ${machine.upgrade && machine.upgrade.cpuUpgradeable ? machine.upgrade.cpuSocket : 'BGA / soldered'}` : ''}`],
      ['ram',`${machine.ramType} ${machine.ramMb} МБ`,`${machine.ramMb} МБ${machine.prebuilt ? ` · slots ${machine.upgrade.ramSlots} · max ${machine.upgrade.maxRamMb} МБ` : ''}`],
      ['gpu',machine.gpu,`GPU score ${round(machine.gpuScore)}${machine.laptop ? ' · integrated/soldered' : ''}`],
      ['storage',`${machine.storageType} ${machine.storageMb} МБ`,`${machine.storageMb} МБ${machine.prebuilt ? ` · ${storageInterfaces(machine).join(' / ')}` : ''}`],
      ['psu',`${machine.laptop ? 'Adapter' : 'PSU'} ${machine.psuWatts}W`,`Power budget ${machine.psuWatts} W`],
      ['cooling',`${machine.laptop ? 'Mobile blower' : 'System cooling'} ${machine.cooling}`,`Cooling score ${machine.cooling}`]
    ];
    if (machine.laptop) specs.push(['display',`${machine.laptopDisplay ? machine.laptopDisplay.sizeInch : 14}\" ${machine.laptopDisplay ? machine.laptopDisplay.resolution : ''}`,`${machine.weightKg || '—'} кг · ${machine.laptopDisplay ? machine.laptopDisplay.refreshHz : 60} Гц`],['battery',`${machine.battery ? machine.battery.capacityWh : 40} Wh`,`${machine.battery ? round(machine.battery.charge) : 0}% · health ${machine.battery ? machine.battery.health : 0}%`]);
    return specs.map(([type,name,summary]) => ({ type,name:String(name),summary:String(summary),year:machine.year,part:null,upgradeable:Boolean(machine.prebuilt && (type === 'storage' && machine.upgrade.storageUpgradeable !== false || type === 'ram' && machine.upgrade.ramUpgradeable !== false || type === 'cpu' && machine.upgrade.cpuUpgradeable)) }));
  };

  /* Тюнинг теперь существует у любой машины; прежние +0/+10/+20 и PSU checks сохранены. */
  proto.pcTuning = function (machine) { return machine ? tuning(machine.tuning) : null; };
  proto.pcTuningPreview = function (id,candidate) {
    const machine = this.getPC(id); if (!machine) return null; const value = tuning(candidate || machine.tuning);
    const preview = Object.assign({},machine,{ powered:true,crashed:false,load:1,tuning:value }); const metrics = this.pcMetrics(preview);
    return { tuning:value,metrics,psuHeadroom:machine.psuWatts - metrics.watts,stabilityPenalty:this.pcStabilityPenalty(preview),scores:this.pcPerformanceScores(preview) };
  };
  proto.setPCTuning = function (id,kind,value) {
    const machine = this.getPC(id); if (!machine) return false; if (machine.powered) { this.notify('Для тюнинга выключите компьютер','warning'); return false; }
    const current = tuning(machine.tuning);
    if (['cpu','gpu'].includes(kind)) { value = Number(value); if (!ocValues.includes(value)) return false; }
    else if (kind === 'fan') { if (!fanValues.includes(value)) return false; } else return false;
    if (current[kind] === value) return true; const candidate = Object.assign({},current,{ [kind]:value });
    if (['cpu','gpu'].includes(kind)) { const preview = this.pcTuningPreview(id,candidate); if (preview.metrics.watts > machine.psuWatts) { this.notify(`${machine.laptop ? 'Адаптер' : 'PSU'} ${machine.psuWatts}W недостаточен · нужно больше ${Math.ceil(preview.metrics.watts)}W`,'error'); return false; } }
    machine.tuning = candidate; machine.tuningRevision = (machine.tuningRevision || 0) + 1; machine.tuningHistory = Array.isArray(machine.tuningHistory) ? machine.tuningHistory : [];
    machine.tuningHistory.unshift({ day:this.state.pc.day,year:this.state.pc.year,revision:machine.tuningRevision,kind,value }); if (machine.tuningHistory.length > 20) machine.tuningHistory.length = 20;
    this.syncPCOSPerformance(machine); this.pcLog(`${machine.name}: universal tuning ${kind}=${value}, revision ${machine.tuningRevision}.`,'good'); this.save(); return true;
  };

  proto.pcRealismMachineState = function (machine) {
    if (!machine) return; machine.tuning = tuning(machine.tuning); machine.tuningRevision = Math.max(0,Number(machine.tuningRevision) || 0);
    machine.tuningHistory = Array.isArray(machine.tuningHistory) ? machine.tuningHistory : [];
    if (machine.prebuiltModelId) {
      const model = DATA.PREBUILT_MOBILE_INDEX.get(machine.prebuiltModelId); machine.prebuilt = true; machine.laptop = true; machine.custom = false; machine.formFactor = model ? model.kind : machine.formFactor || 'laptop';
      if (model && !machine.upgrade) machine.upgrade = { cpuUpgradeable:model.cpuUpgradeable,cpuSocket:model.cpuSocket,maxCpuTdp:model.maxCpuTdp,ramUpgradeable:model.ramUpgradeable !== false,storageUpgradeable:model.storageUpgradeable !== false,ramSlots:model.ramSlots,maxRamMb:model.maxRamMb,storageInterfaces:model.storageInterfaces.slice() };
      if (!Array.isArray(machine.upgradeHistory)) machine.upgradeHistory = []; if (!machine.prebuiltParts) machine.prebuiltParts = {};
    }
    if (machine.osInstall && typeof machine.osInstall === 'object') {
      const setup = machine.osInstall; if (!this.pcOS(setup.osId) || !installPhases.includes(setup.phase)) { setup.phase = 'error'; setup.error = 'Образ установки больше недоступен'; }
      setup.experimental = Boolean(setup.experimental); setup.elapsedSeconds = Math.max(0,Number(setup.elapsedSeconds) || 0); setup.mediaElapsed = Math.max(0,Number(setup.mediaElapsed) || 0);
      setup.mediaSeconds = clamp(Number(setup.mediaSeconds) || 30,5,600); setup.totalSeconds = clamp(Number(setup.totalSeconds) || 1800,300,14400); setup.skippedSeconds = Math.max(0,Number(setup.skippedSeconds) || 0);
    }
    this.syncPCOSPerformance(machine);
  };
  proto.pcInstallEstimate = function (machineOrId,osId) { const machine = typeof machineOrId === 'string' ? this.getPC(machineOrId) : machineOrId; const os = this.pcOS(osId); return os && machine ? this.pcOSPerformance(Object.assign({},machine,{ os:os.id })).installSeconds : 0; };
  proto.startPCOSInstallation = function (id,osId,experimental = false) {
    const machine = this.getPC(id); const os = this.pcOS(osId); if (!machine || !os) return { ok:false };
    if (machine.powered) { this.notify('Выключите ПК перед записью установочной флешки','warning'); return { ok:false }; }
    if (experimental && machine.origin !== 'owned') { this.notify('Experimental Bypass разрешён только на собственных ПК','warning'); return { ok:false }; }
    const compatible = machine.ramMb >= os.minRamMb && machine.storageMb >= os.minStorageMb && os.year <= this.state.pc.year;
    if (!experimental && !compatible) { this.notify(`${os.name}: проверьте год, RAM и диск`,'warning'); return { ok:false }; }
    if (!experimental && os.price && this.state.pc.money < os.price) { this.notify(`Лицензия стоит ${os.price} ₽`,'error'); return { ok:false }; }
    const totalSeconds = this.pcInstallEstimate(machine,osId); const mediaSeconds = clamp(Math.ceil(os.minStorageMb / 500),15,180);
    machine.osInstall = { id:uid('usb-setup'),osId,experimental,phase:'media',elapsedSeconds:0,totalSeconds,mediaElapsed:0,mediaSeconds,skippedSeconds:0,
      startedDay:this.state.pc.day,startedYear:this.state.pc.year,media:`USB ${machine.year >= 2012 ? '3.0' : machine.year >= 2000 ? '2.0' : '1.1'} · ${os.name}`,error:null };
    this.pcLog(`${machine.name}: подготовлен USB installer ${os.name}; оценка ${round(totalSeconds / 60)} мин.`); this.save(); return { ok:true,setup:machine.osInstall };
  };
  proto.pcOSInstallationAction = function (id,action) {
    const machine = this.getPC(id); const setup = machine && machine.osInstall; if (!setup) return false;
    if (action === 'write' && setup.phase === 'media') { setup.phase = 'writing'; this.notify('Запись образа на флешку началась','success'); }
    else if (action === 'usb' && setup.phase === 'bios') { setup.phase = 'copy'; setup.elapsedSeconds = 0; this.notify('USB выбран в Boot Menu · установка началась','success'); }
    else if (action === 'skip' && setup.phase === 'copy') { const seconds = this.state.pc.speed === 1 ? 10 : 600; setup.skippedSeconds += seconds; this.advancePCOSInstallation(id,seconds,'skip'); return true; }
    else if (action === 'cancel' && setup.phase !== 'complete') { setup.phase = 'media'; setup.elapsedSeconds = 0; setup.mediaElapsed = 0; this.notify('USB setup сброшен к выбору образа; прежняя ОС не затронута','warning'); }
    else if (action === 'new') { machine.osInstall = null; this.save(); return true; }
    else return false; this.save(); return true;
  };
  proto.advancePCOSInstallation = function (id,realSeconds = 1,source = 'tick') {
    const machine = this.getPC(id); const setup = machine && machine.osInstall; if (!setup || this.state.paused) return false;
    const seconds = clamp(Number(realSeconds) || 0,0,86400); if (seconds <= 0) return false;
    if (setup.phase === 'writing') {
      setup.mediaElapsed = Math.min(setup.mediaSeconds,setup.mediaElapsed + seconds);
      if (setup.mediaElapsed >= setup.mediaSeconds) { setup.phase = 'bios'; this.notify('Флешка готова · войдите в BIOS и выберите USB','success'); this.save(); }
      return true;
    }
    if (setup.phase !== 'copy') return false; setup.elapsedSeconds = Math.min(setup.totalSeconds,setup.elapsedSeconds + seconds);
    if (setup.elapsedSeconds >= setup.totalSeconds) {
      const installed = this.installPCOS(id,setup.osId,setup.experimental);
      if (installed) { setup.phase = 'complete'; setup.completedDay = this.state.pc.day; setup.completedYear = this.state.pc.year; machine.lastOSInstall = Object.assign({},setup); this.notify('Установка завершена · можно загружать ОС','success'); }
      else { setup.phase = 'error'; setup.error = 'Финальная проверка отклонена: лицензия/совместимость. Прежняя ОС сохранена.'; }
      this.save();
    } else if (source === 'skip') this.save();
    return true;
  };

  proto.pcPackageCompatibility = function (machineOrId,packageId) {
    const machine = typeof machineOrId === 'string' ? this.getPC(machineOrId) : machineOrId; const pkg = DATA.OS_WORLD_GAMES.find(item => item.id === packageId);
    const reasons = []; if (!machine || !pkg) return { ok:false,reasons:['Пакет или машина не найдены'] }; const os = machine.os ? this.pcOS(machine.os) : null;
    if (!os) reasons.push('Сначала установите ОС'); const ram = pkg.minRamMb || 0; if (ram && machine.ramMb < ram) reasons.push(`Нужно ${ram} МБ RAM, установлено ${machine.ramMb} МБ`);
    if (os && pkg.supportedFamilies && !pkg.supportedFamilies.includes(os.family)) reasons.push(`${pkg.name}: ${os.family} не поддерживается`);
    if (os && pkg.minOSYear && os.year < pkg.minOSYear) reasons.push(`${pkg.name}: требуется более новая ОС (от ${pkg.minOSYear})`);
    const state = machine.osWorld || { games:{} }; const installed = id => Boolean(state.games && state.games[id]);
    const isLegacyGame = DATA.OS_WORLD_GAMES.slice(0,10).some(item => item.id === packageId);
    if (os && !isLegacyGame && (pkg.kind === 'game' || pkg.kind === 'emulator' && pkg.benchmarkId)) {
      const api = pkg.api || (DATA.PC_BENCHMARKS[pkg.benchmarkId] || {}).api || 'win32';
      if (api === 'dos' && os.family !== 'dos' && !(os.family === 'windows' && os.year <= 2001) && !installed('dosbox')) reasons.push('Нужен родной DOS/Windows ≤2001 или установленный DOSBox Staging');
      if (api === 'emulator' && os.family === 'dos') reasons.push('Эмулятору нужна графическая Windows/Linux/BSD ОС');
      if (!['dos','emulator','java'].includes(api) && os.family === 'dos') reasons.push('DOS не поддерживает этот Win32/DirectX-пакет');
      if (!['dos','emulator','java'].includes(api) && ['linux','bsd'].includes(os.family) && !installed('wine')) reasons.push('На Linux/BSD установите Wine 9.0 для Windows-игры');
    }
    return { ok:reasons.length === 0,reasons,pkg,os };
  };
  proto.pcBrowserInfo = function (machineOrId) {
    const machine = typeof machineOrId === 'string' ? this.getPC(machineOrId) : machineOrId; const os = machine && machine.os ? this.pcOS(machine.os) : null;
    let result = { engine:os ? os.family === 'linux' || os.family === 'bsd' ? 'System Firefox' : os.family === 'dos' ? 'Links text mode' : `System browser ${os.browserLevel || 1}` : 'No OS',level:os ? Number(os.browserLevel) || 1 : 0,modern:false,source:'system' };
    const state = machine && machine.osWorld; if (state && state.games) DATA.PC_REALISM_SOFTWARE_PACKAGES.filter(pkg => pkg.kind === 'browser' && state.games[pkg.id]).forEach(pkg => { if (pkg.browserLevel > result.level) result = { engine:pkg.name.split(' · ')[0],level:pkg.browserLevel,modern:pkg.browserLevel >= 100,source:pkg.id }; });
    result.modern = result.level >= 100; return result;
  };

  const baseRealismInstallOS = proto.installPCOS;
  proto.installPCOS = function (id,osId,experimental = false) {
    const result = baseRealismInstallOS.call(this,id,osId,experimental); const machine = this.getPC(id);
    if (result && machine) { machine.osBasePenalty = machine.osPenalty; this.syncPCOSPerformance(machine,false); this.save(); } return result;
  };
  const baseRealismCreate = proto.createPC;
  proto.createPC = function (...args) { const machine = baseRealismCreate.apply(this,args); if (machine) this.pcRealismMachineState(machine); return machine; };
  const baseRealismNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseRealismNormalize.call(this); if (this.state.pc) { this.pcPrebuiltState(); this.state.pc.computers.forEach(machine => this.pcRealismMachineState(machine)); this.state.pc.offers.forEach(offer => { if (offer.pc) this.pcRealismMachineState(offer.pc); }); } return result;
  };
  const baseRealismStart = proto.startPCCampaign;
  proto.startPCCampaign = function () { const result = baseRealismStart.call(this); this.pcPrebuiltState(); this.state.pc.computers.forEach(machine => this.pcRealismMachineState(machine)); return result; };
  const baseRealismMetrics = proto.pcMetrics;
  proto.pcMetrics = function (machine) {
    const result = baseRealismMetrics.call(this,machine); if (!machine || !machine.os) return result; const profile = this.pcOSPerformance(machine);
    result.osResponsiveness = profile.responsiveness; result.osSpeedFactor = profile.speedFactor; result.osBootSeconds = profile.bootSeconds;
    if (machine.powered) {
      const os = profile.os; result.ramUsedMb = Math.min(machine.ramMb,Math.max(result.ramUsedMb || 0,Math.min(machine.ramMb * .92,os.minRamMb * .42 + machine.ramMb * .08)));
      result.ramPercent = machine.ramMb ? result.ramUsedMb / machine.ramMb * 100 : 0; result.cpuLoad = clamp((result.cpuLoad || 0) + Math.max(0,1 - profile.responsiveness) * 18,0,100);
      result.performance = clamp((result.performance || 1) * clamp(.5 + profile.responsiveness * .5,.1,1),.05,2);
    }
    return result;
  };
  const baseRealismBenchmark = proto.runPCBenchmark;
  proto.runPCBenchmark = function (id,benchmarkId) {
    const result = baseRealismBenchmark.call(this,id,benchmarkId); const machine = this.getPC(id); const benchmark = DATA.PC_BENCHMARKS[benchmarkId];
    if (!result || !machine || !benchmark || benchmarkId === 'stability') return result; const cap = Number(benchmark.maxFps) || 360;
    result.uncappedFps = result.fps; result.frameCap = cap; result.api = benchmark.api || 'legacy'; result.fps = Math.min(cap,result.fps);
    result.samples = result.samples.map(sample => Math.min(cap,sample)); result.onePercentLow = round(result.fps * .79 * 10) / 10; result.osSpeedFactor = this.pcOSPerformance(machine).speedFactor;
    result.score = round(clamp(result.fps / benchmark.target * 100,0,100)); result.passed = result.passed && result.fps >= benchmark.target * .8;
    machine.benchmarks[benchmarkId] = result; this.save(); return result;
  };
  const baseRealismInstallPackage = proto.osWorldInstall;
  proto.osWorldInstall = function (machineId,family,downloadId) {
    if (family === 'pc') { const machine = this.getPC(machineId); const state = machine && machine.osWorld; const item = state && state.downloads.find(download => download.id === downloadId); const pkg = item && DATA.OS_WORLD_GAMES.find(entry => entry.id === item.packageId);
      if (pkg) { const compatibility = this.pcPackageCompatibility(machine,pkg.id); if (!compatibility.ok) { this.notify(compatibility.reasons.join(' · '),'error'); return { ok:false,reasons:compatibility.reasons,package:pkg }; } }
    }
    const result = baseRealismInstallPackage.call(this,machineId,family,downloadId); if (result && result.ok && family === 'pc') { const machine = this.getPC(machineId); const browser = this.pcBrowserInfo(machine); if (result.package.kind === 'browser') this.notify(`${browser.engine}: движок ${browser.level} активен · modern.local доступен`,'success'); } return result;
  };
  const baseRealismPlay = proto.osWorldPlayGame;
  proto.osWorldPlayGame = function (machineId,gameId) {
    const pkg = DATA.OS_WORLD_GAMES.find(item => item.id === gameId); if (!pkg || !DATA.PC_BENCHMARKS[pkg.benchmarkId || pkg.id]) { this.notify('Это приложение, а не игра · откройте Browser или Downloads','warning'); return false; }
    const compatibility = this.pcPackageCompatibility(machineId,gameId); if (!compatibility.ok) { this.notify(compatibility.reasons.join(' · '),'error'); return false; }
    return baseRealismPlay.call(this,machineId,gameId);
  };
  const baseRealismNavigate = proto.osWorldNavigate;
  proto.osWorldNavigate = function (machineId,family,url,direction = '') {
    const result = baseRealismNavigate.call(this,machineId,family,url,direction); if (result && family === 'pc') { const machine = this.getPC(machineId); const site = DATA.OS_WORLD_SITES.pc[result.url]; const browser = this.pcBrowserInfo(machine); result.browser = browser; result.compatible = !site || !site.modernRequired || browser.level >= site.modernRequired; if (site && site.modernRequired && !result.compatible) result.description = `Нужен browser level ${site.modernRequired}+. На Windows XP скачайте Supermium; текущий ${browser.engine} level ${browser.level}.`; } return result;
  };
  const baseRealismDownloadSpeed = proto.osWorldEffectiveSpeed;
  proto.osWorldEffectiveSpeed = function (machine,family,item) { const speed = baseRealismDownloadSpeed.call(this,machine,family,item); return family === 'pc' && machine && machine.os ? speed * this.pcOSPerformance(machine).speedFactor : speed; };
  const baseRealismTick = proto.tickPC;
  proto.tickPC = function (realSeconds = 1) { const result = baseRealismTick.call(this,realSeconds); if (this.state.pc && this.state.pc.started && !this.state.paused) this.state.pc.computers.forEach(machine => { if (machine.osInstall && ['writing','copy'].includes(machine.osInstall.phase)) this.advancePCOSInstallation(machine.id,realSeconds,'tick'); }); return result; };
  proto.pcRealismLoaded = true;
})();
