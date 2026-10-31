/* Expansion 2.0: игровые системы поверх базового симулятора, включая миграцию старых saves. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || Game.prototype.systemsLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const clone = object => JSON.parse(JSON.stringify(object));

  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseNormalize.call(this);
    const s = this.state;
    s.version = 5;
    s.settings = Object.assign({ music: true, sfx: true, volume: .32, track: 'electro' }, s.settings || {});
    s.networkId = s.networkId || 'home100';
    s.upgrades = Object.assign({ ac: 0, pdu: 0, ups: 0 }, s.upgrades || {});
    s.xp = Number.isFinite(s.xp) ? s.xp : 0;
    s.level = Number.isFinite(s.level) ? s.level : 1;
    s.activeContract = s.activeContract || null;
    s.contracts = Array.isArray(s.contracts) ? s.contracts : [];
    s.outageDay = s.outageDay || 0;
    s.heatwaveDay = s.heatwaveDay || 0;
    s.revenueBoostDay = s.revenueBoostDay || 0;
    s.upsUsedDay = s.upsUsedDay || 0;
    s.stats.network = s.stats.network || 0;
    if (!s.contracts.length) s.contracts = this.generateContractOffers();
    s.servers.forEach(server => {
      if (!server.parts.gpus) server.parts.gpus = [];
      if (!server.raid) server.raid = 'none';
    });
    return result;
  };

  const baseNormalizeServer = proto.normalizeServer;
  proto.normalizeServer = function (server) {
    const result = baseNormalizeServer.call(this, server);
    server.parts.gpus = Array.isArray(server.parts.gpus) ? server.parts.gpus : [];
    server.raid = server.raid || 'none';
    if (!Number.isFinite(server.chassis.pcieSlots)) server.chassis.pcieSlots = server.chassis.value >= 30000 ? 2 : 1;
    return result;
  };

  const baseInstantiate = proto.instantiateServer;
  proto.instantiateServer = function (templateId, condition = 'working') {
    const server = baseInstantiate.call(this, templateId, condition);
    const template = DATA.template(templateId);
    server.parts.gpus = (template.gpus || []).map(id => this.makePart(id));
    server.raid = 'none';
    return server;
  };

  const baseCreateServer = proto.createServer;
  proto.createServer = function (chassisUid) {
    const server = baseCreateServer.call(this, chassisUid);
    if (server) {
      server.parts.gpus = [];
      server.raid = 'none';
      this.addXP(60);
    }
    return server;
  };

  const baseServerSellPrice = proto.serverSellPrice;
  proto.serverSellPrice = function (server) {
    return baseServerSellPrice.call(this, server) + (server.parts.gpus || []).reduce((sum, part) => sum + this.partSellPrice(part), 0);
  };

  const baseDismantle = proto.dismantleServer;
  proto.dismantleServer = function (serverId) {
    const server = this.getServer(serverId);
    if (server && !server.powered && server.parts.gpus) this.state.inventory.push(...server.parts.gpus);
    return baseDismantle.call(this, serverId);
  };

  const baseSlotType = proto.slotType;
  proto.slotType = function (slotKey) {
    if (slotKey.startsWith('gpu')) return 'gpu';
    return baseSlotType.call(this, slotKey);
  };

  const baseGetSlot = proto.getSlotPart;
  proto.getSlotPart = function (server, slotKey) {
    if (slotKey.startsWith('gpu')) return (server.parts.gpus || [])[Number(slotKey.slice(3))] || null;
    return baseGetSlot.call(this, server, slotKey);
  };

  const baseSetSlot = proto.setSlotPart;
  proto.setSlotPart = function (server, slotKey, part) {
    if (slotKey.startsWith('gpu')) {
      server.parts.gpus[Number(slotKey.slice(3))] = part;
      server.parts.gpus = server.parts.gpus.filter(Boolean);
      return;
    }
    return baseSetSlot.call(this, server, slotKey, part);
  };

  proto.raidCapacity = function (server) {
    const disks = server.parts.disks.filter(part => part && !part.broken);
    if (!disks.length) return 0;
    const capacities = disks.map(part => part.capacity).sort((a, b) => b - a);
    const total = capacities.reduce((sum, value) => sum + value, 0);
    switch (server.raid) {
      case 'raid1': return Math.round(total / 2);
      case 'raid5': return Math.max(0, total - (capacities[0] || 0));
      case 'raid6': return Math.max(0, total - (capacities[0] || 0) - (capacities[1] || 0));
      case 'raid10': return Math.round(total / 2);
      default: return total;
    }
  };

  proto.configureRAID = function (serverId, raidId) {
    const server = this.getServer(serverId);
    const raid = DATA.RAIDS[raidId];
    if (!server || !raid) return false;
    if (server.powered) {
      this.notify('Выключите сервер перед изменением RAID', 'warning');
      return false;
    }
    const count = server.parts.disks.length;
    if (count < raid.minDisks || (raidId === 'raid10' && count % 2)) {
      this.notify(`${raid.name}: нужно ${raid.minDisks}${raidId === 'raid10' ? ' (чётное число)' : ''} диска`, 'error');
      return false;
    }
    server.raid = raidId;
    server.os = null;
    server.software = [];
    server.powered = false;
    this.updateStatus(server);
    this.addXP(40);
    this.log(`${server.name}: массив пересобран в ${raid.name}. Данные стерты.`, 'bad');
    this.notify(`${raid.name} настроен · установите ОС заново`, 'success');
    this.save();
    return true;
  };

  const baseHardwareReport = proto.hardwareReport;
  proto.hardwareReport = function (server) {
    const report = baseHardwareReport.call(this, server);
    const gpuReserve = (server.parts.gpus || []).reduce((sum, part) => sum + part.tdp * 1.25, 0);
    report.requiredPsu = Math.round(report.requiredPsu + gpuReserve);
    report.diskTotal = this.raidCapacity(server);
    report.gpuCount = (server.parts.gpus || []).length;
    report.gpuVram = (server.parts.gpus || []).reduce((sum, part) => sum + part.vram, 0);
    const psu = server.parts.psu;
    if (psu && !psu.broken && psu.watts < report.requiredPsu && !report.issues.some(issue => issue.includes('БП '))) {
      report.issues.push(`БП ${psu.watts}W слаб для CPU/GPU: нужно ${report.requiredPsu}W`);
    }
    report.ok = report.issues.length === 0;
    return report;
  };

  const baseRoom = proto.room;
  proto.room = function () {
    const base = baseRoom.call(this);
    const upgrades = this.state && this.state.upgrades ? this.state.upgrades : { ac: 0, pdu: 0, ups: 0 };
    const heatwave = this.state && this.state.heatwaveDay === this.state.day ? 6 : 0;
    return Object.assign({}, base, {
      ambient: base.ambient - (upgrades.ac || 0) * 3 + heatwave,
      powerLimit: base.powerLimit + (upgrades.pdu || 0) * 1000
    });
  };

  const baseMetrics = proto.metrics;
  proto.metrics = function (server) {
    const metrics = baseMetrics.call(this, server);
    const gpus = server.parts.gpus || [];
    const apps = server.software.map(id => DATA.software(id)).filter(Boolean);
    const gpuActive = server.powered && apps.some(app => app.gpuBoost);
    const gpuLoad = gpuActive ? clamp(metrics.rawLoad, 20, 100) : server.powered && gpus.length ? 3 : 0;
    const gpuTdp = gpus.reduce((sum, part) => sum + part.tdp, 0);
    const gpuWatts = server.powered ? gpuTdp * (.1 + .9 * gpuLoad / 100) : 0;
    const psuEfficiency = server.parts.psu && !server.parts.psu.broken ? server.parts.psu.efficiency : .75;
    metrics.watts += gpuWatts / psuEfficiency;
    if (server.powered) metrics.targetTemp += gpuTdp * (.12 + .88 * gpuLoad / 100) * 64.8 / Math.max(25, metrics.cooling);
    metrics.noise = server.powered ? clamp(metrics.noise + (gpuLoad > 80 ? 3 : 0), 0, 100) : 0;
    metrics.gpuCompute = gpus.reduce((sum, part) => sum + part.compute, 0);
    metrics.gpuVram = gpus.reduce((sum, part) => sum + part.vram, 0);
    metrics.gpuLoad = gpuLoad;
    const raid = DATA.RAIDS[server.raid] || DATA.RAIDS.none;
    const disks = server.parts.disks.filter(Boolean);
    const diskSpeed = disks.length ? disks.reduce((sum, part) => sum + (part.speed || 1), 0) / disks.length : 1;
    metrics.storageFactor = clamp(.85 + .15 * diskSpeed * raid.speed, .65, 1.65);
    return metrics;
  };

  proto.network = function () {
    return DATA.network(this.state.networkId);
  };

  proto.upgradeNetwork = function (networkId) {
    const network = DATA.network(networkId);
    if (!network || network.id === this.state.networkId) return false;
    if (!this.spend(network.price, `Подключение: ${network.name}`)) return false;
    this.state.networkId = network.id;
    this.addXP(75);
    this.log(`Новый uplink: ${network.name} · ${network.speed} Мбит/с.`, 'good');
    this.notify(`Интернет подключён: ${network.name}`, 'success');
    this.save();
    return true;
  };

  proto.hourlyServerRevenue = function (server) {
    if (!server.powered || !server.os || server.crashed || server.hung || this.state.powerOut) return 0;
    if (this.state.outageDay === this.state.day) return 0;
    const metrics = this.metrics(server);
    const cores = server.parts.cpu ? server.parts.cpu.cores : 0;
    const network = this.network();
    const eventBoost = this.state.revenueBoostDay === this.state.day ? 1.5 : 1;
    return server.software.reduce((sum, id) => {
      const app = DATA.software(id);
      if (!app) return sum;
      let efficiency = metrics.efficiency * metrics.storageFactor;
      if (app.gpuBoost) efficiency *= 1 + Math.min(2.5, metrics.gpuCompute / 40);
      return sum + (app.rate + (app.coreRate || 0) * cores) * efficiency * network.revenue * eventBoost;
    }, 0);
  };

  const baseTogglePower = proto.togglePower;
  proto.togglePower = function (serverId) {
    const server = this.getServer(serverId);
    if (server && !server.powered && !this.state.powerOut) {
      server.powered = true;
      const projectedServerWatts = this.metrics(server).watts;
      server.powered = false;
      const projected = this.farmMetrics().watts + projectedServerWatts;
      if (projected > this.room().powerLimit) {
        this.notify(`Линия не выдержит ${Math.round(projected)}W · нужен PDU или другое помещение`, 'error');
        return false;
      }
    }
    return baseTogglePower.call(this, serverId);
  };

  const baseFarmMetrics = proto.farmMetrics;
  proto.farmMetrics = function () {
    const result = baseFarmMetrics.call(this);
    const ac = this.state.upgrades ? this.state.upgrades.ac || 0 : 0;
    if (result.active > 0 && ac > 0) result.watts += ac * 45;
    result.network = this.network();
    result.outage = this.state.outageDay === this.state.day;
    return result;
  };

  const baseTick = proto.tick;
  proto.tick = function () {
    if (!this.state.paused && this.state.upgrades && this.state.upgrades.ac > 0 && this.state.servers.some(server => server.powered)) {
      this.state.stats.energyToday += this.state.upgrades.ac * 45 * .2 / 1000;
    }
    return baseTick.call(this);
  };

  const baseTripPower = proto.tripPower;
  proto.tripPower = function (reason, debt = false) {
    const ups = this.state.upgrades ? this.state.upgrades.ups || 0 : 0;
    if (!debt && ups > 0 && this.state.upsUsedDay !== this.state.day) {
      const active = this.state.servers.filter(server => server.powered).sort((a, b) => this.metrics(b).watts - this.metrics(a).watts);
      if (active.length) {
        this.state.upsUsedDay = this.state.day;
        this.powerOff(active[0]);
        this.notify(`ИБП спас ферму · ${active[0].name} отключён для снижения нагрузки`, 'warning');
        this.log('ИБП отработал аварию. Самый прожорливый сервер безопасно остановлен.', 'good');
        this.addXP(35);
        return;
      }
    }
    return baseTripPower.call(this, reason, debt);
  };

  proto.buyUpgrade = function (kind) {
    const definition = DATA.UPGRADES[kind];
    if (!definition) return false;
    const level = this.state.upgrades[kind] || 0;
    if (level >= definition.max) return false;
    const price = definition.prices[level];
    if (!this.spend(price, `${definition.name} · уровень ${level + 1}`)) return false;
    this.state.upgrades[kind] = level + 1;
    this.addXP(90);
    this.log(`${definition.name}: уровень ${level + 1}.`, 'good');
    this.notify(`${definition.name} улучшено`, 'success');
    this.save();
    return true;
  };

  proto.buyCatalogServer = function (templateId) {
    const template = DATA.template(templateId);
    if (!template) return false;
    if (this.state.servers.length >= this.room().slots) {
      this.notify('В помещении нет места. Нужен апгрейд.', 'warning');
      return false;
    }
    const price = Math.round(template.basePrice * 1.25);
    if (!this.spend(price, `Новый сервер: ${template.name}`)) return false;
    const server = this.instantiateServer(templateId, 'working');
    this.state.servers.push(server);
    this.addXP(120);
    this.log(`Из каталога доставлен ${template.name} за ${this.formatMoney(price)}.`, 'good');
    this.notify(`${template.name} установлен в стойку`, 'success');
    this.save();
    return true;
  };

  proto.playerRank = function () {
    const ranks = ['Эникейщик', 'Сисадмин', 'DevOps', 'Архитектор', 'Владелец ЦОД', 'Бог локалки'];
    return ranks[Math.min(ranks.length - 1, Math.max(0, this.state.level - 1))];
  };

  proto.addXP = function (amount) {
    if (!this.state || !Number.isFinite(amount) || amount <= 0) return;
    this.state.xp = (this.state.xp || 0) + Math.round(amount);
    const level = 1 + Math.floor(Math.sqrt(this.state.xp / 300));
    if (level > (this.state.level || 1)) {
      this.state.level = level;
      this.notify(`Уровень ${level}: ${this.playerRank()}`, 'success');
      this.log(`Получен уровень ${level} · ${this.playerRank()}.`, 'good');
    } else {
      this.state.level = Math.max(this.state.level || 1, level);
    }
  };

  proto.generateContractOffers = function () {
    const pool = DATA.CONTRACT_TEMPLATES.slice();
    const offers = [];
    while (pool.length && offers.length < 3) {
      const index = Math.floor(Math.random() * pool.length);
      const source = pool.splice(index, 1)[0];
      offers.push(Object.assign(clone(source), { id: uid('contract') }));
    }
    return offers;
  };

  proto.acceptContract = function (contractId) {
    if (this.state.activeContract) {
      this.notify('Сначала завершите активный контракт', 'warning');
      return false;
    }
    const contract = this.state.contracts.find(item => item.id === contractId);
    if (!contract) return false;
    this.state.activeContract = Object.assign(clone(contract), { progress: 0, misses: 0, acceptedDay: this.state.day });
    this.state.contracts = this.state.contracts.filter(item => item.id !== contractId);
    this.log(`Подписан контракт «${contract.title}» · ${contract.duration} дн.`, 'good');
    this.notify('Контракт активен. Держите нужный сервер онлайн.', 'success');
    this.save();
    return true;
  };

  proto.abandonContract = function () {
    if (!this.state.activeContract) return false;
    this.log(`Контракт «${this.state.activeContract.title}» расторгнут.`, 'bad');
    this.state.activeContract = null;
    this.state.contracts = this.generateContractOffers();
    this.notify('Контракт расторгнут', 'warning');
    this.save();
    return true;
  };

  proto.processContract = function () {
    const contract = this.state.activeContract;
    if (!contract) return;
    const eligible = this.state.servers.some(server => {
      if (!server.powered || !server.os || server.crashed || server.hung || !server.software.includes(contract.requiredSoft)) return false;
      const cores = server.parts.cpu ? server.parts.cpu.cores : 0;
      return this.hardwareReport(server).ramTotal >= contract.minRam && cores >= contract.minCores;
    });
    if (eligible) {
      contract.progress += 1;
      contract.misses = 0;
      this.notify(`Контракт: день ${contract.progress}/${contract.duration} принят`, 'success');
    } else {
      contract.misses += 1;
      this.notify(`Контракт: требования не выполнены (${contract.misses}/2)`, 'warning');
    }
    if (contract.progress >= contract.duration) {
      this.addMoney(contract.reward);
      this.state.stats.revenueToday += contract.reward;
      this.addXP(contract.xp);
      this.log(`Контракт «${contract.title}» завершён: +${this.formatMoney(contract.reward)}.`, 'good');
      this.notify(`Контракт завершён: +${this.formatMoney(contract.reward)}`, 'success');
      this.state.activeContract = null;
      this.state.contracts = this.generateContractOffers();
    } else if (contract.misses >= 2) {
      this.log(`Контракт «${contract.title}» сорван: сервер не соответствовал требованиям.`, 'bad');
      this.state.activeContract = null;
      this.state.contracts = this.generateContractOffers();
      this.notify('Заказчик расторг контракт', 'error');
    }
  };

  proto.rollDailyEvent = function () {
    if (!this.state.flags.onboarded || Math.random() > .22) return;
    const day = this.state.day;
    const roll = Math.random();
    if (roll < .2) {
      this.state.heatwaveDay = day;
      this.notify('Событие: аномальная жара · +6°C в помещении', 'warning');
      this.log('Прогноз на день: аномальная жара, охлаждение работает с нагрузкой.', 'bad');
    } else if (roll < .4) {
      this.state.outageDay = day;
      this.notify('Событие: авария у провайдера · доход онлайн-сервисов остановлен', 'error');
      this.log('Провайдер сообщил об аварии на магистрали. Доход сетевых сервисов paused.', 'bad');
    } else if (roll < .65) {
      this.state.revenueBoostDay = day;
      this.notify('Событие: вирусный трафик · доход ×1.5', 'success');
      this.log('Ваши сервисы попали в рекомендации: суточный доход увеличен на 50%.', 'good');
    } else if (roll < .85) {
      this.state.listings = this.generateListingSet().map(listing => Object.assign(listing, { originalPrice: listing.price, price: Math.max(100, Math.round(listing.price * .85 / 10) * 10) }));
      this.notify('Событие: распродажа на Авито · −15%', 'success');
      this.log('На Авито началась распродажа складов. Все новые лоты подешевели.', 'good');
    } else {
      this.addMoney(5000);
      this.notify('Событие: бывший клиент вернул старый долг · +5 000 ₽', 'success');
      this.log('Неожиданный перевод от старого клиента: +5 000 ₽.', 'good');
    }
  };

  const baseEndDay = proto.endDay;
  proto.endDay = function () {
    const endedDay = this.state.day;
    this.processContract();
    const networkFee = this.network().daily;
    if (networkFee) this.payBill(networkFee, 'network', `Интернет · ${this.network().name}`);
    const result = baseEndDay.call(this);
    const record = this.state.stats.daily.find(item => item.day === endedDay);
    if (record) {
      record.network = networkFee;
      record.profit -= networkFee;
      if (record.profit > 0) this.addXP(Math.max(10, Math.floor(record.profit / 500)));
    }
    this.rollDailyEvent();
    this.save();
    return result;
  };

  const baseBuyListing = proto.buyListing;
  proto.buyListing = function (listingId) {
    const result = baseBuyListing.call(this, listingId);
    if (result.ok) this.addXP(result.isServer ? 80 : 15);
    return result;
  };

  const baseFinishOS = proto.finishOSInstall;
  proto.finishOSInstall = function (serverId, usbUid) {
    const result = baseFinishOS.call(this, serverId, usbUid);
    if (result) this.addXP(90);
    return result;
  };

  const baseInstallSoftware = proto.installSoftware;
  proto.installSoftware = function (serverId, softwareId) {
    const result = baseInstallSoftware.call(this, serverId, softwareId);
    if (result) this.addXP(35);
    return result;
  };

  Game.prototype.systemsLoaded = true;
})();
