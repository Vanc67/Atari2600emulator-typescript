(function () {
  'use strict';

  const SAVE_KEY = 'server-tycoon-save-v4';
  const SAVE_VERSION = 4;
  const ELECTRICITY_PRICE = 7.2;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(value);
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const clone = object => JSON.parse(JSON.stringify(object));

  class ServerTycoonGame {
    constructor(autoLoad = true) {
      this.events = [];
      this.saveTimer = 0;
      this.tickCounter = 0;
      this.state = autoLoad ? this.load() : this.createInitialState();
      if (!this.state) this.state = this.createInitialState();
      this.normalize();
    }

    createInitialState() {
      const state = {
        version: SAVE_VERSION,
        money: 100000,
        debt: 0,
        day: 1,
        minute: 0,
        roomId: 'room',
        powerOut: false,
        powerReason: '',
        paused: false,
        inventory: [],
        servers: [],
        listings: [],
        logs: [],
        flags: { onboarded: false, lastMomDay: -3 },
        stats: {
          earned: 0,
          spent: 0,
          electricity: 0,
          rent: 0,
          noiseFines: 0,
          crashes: 0,
          hangs: 0,
          built: 0,
          bargains: 0,
          momHelp: 0,
          cheated: 0,
          energyToday: 0,
          revenueToday: 0,
          finesToday: 0,
          daily: []
        }
      };
      this.state = state;
      const starter = this.instantiateServer('office6000', 'working');
      starter.name = 'Первая печка';
      starter.os = null;
      state.servers.push(starter);
      state.inventory.push(this.makePart('usb16'), this.makePart('ddr3-4'), this.makePart('toolkit'));
      state.listings = this.generateListingSet();
      this.log('Стартовый капитал: 100 000 ₽. В комнате уже ждёт HP Compaq.', 'good');
      return clone(state);
    }

    normalize() {
      const s = this.state;
      s.version = SAVE_VERSION;
      s.inventory = Array.isArray(s.inventory) ? s.inventory : [];
      s.servers = Array.isArray(s.servers) ? s.servers : [];
      s.listings = Array.isArray(s.listings) ? s.listings : [];
      s.logs = Array.isArray(s.logs) ? s.logs : [];
      s.flags = Object.assign({ onboarded: false, lastMomDay: -3 }, s.flags || {});
      s.stats = Object.assign(this.createStats(), s.stats || {});
      s.stats.daily = Array.isArray(s.stats.daily) ? s.stats.daily : [];
      s.servers.forEach(server => this.normalizeServer(server));
      if (!DATA.room(s.roomId)) s.roomId = 'room';
      if (!s.listings.length) s.listings = this.generateListingSet();
    }

    createStats() {
      return {
        earned: 0, spent: 0, electricity: 0, rent: 0, noiseFines: 0,
        crashes: 0, hangs: 0, built: 0, bargains: 0, momHelp: 0, cheated: 0,
        energyToday: 0, revenueToday: 0, finesToday: 0, daily: []
      };
    }

    normalizeServer(server) {
      server.parts = Object.assign({ cpu: null, ram: [], disks: [], cooler: null, psu: null }, server.parts || {});
      server.parts.ram = Array.isArray(server.parts.ram) ? server.parts.ram : [];
      server.parts.disks = Array.isArray(server.parts.disks) ? server.parts.disks : [];
      server.software = Array.isArray(server.software) ? server.software : [];
      server.history = Object.assign({ temp: [], cpu: [], ram: [] }, server.history || {});
      ['temp', 'cpu', 'ram'].forEach(key => {
        if (!Array.isArray(server.history[key])) server.history[key] = [];
        server.history[key] = server.history[key].slice(-48);
      });
      server.temp = Number.isFinite(server.temp) ? server.temp : DATA.room(this.state.roomId).ambient;
      server.health = Number.isFinite(server.health) ? server.health : 100;
      server.coolingBonus = server.coolingBonus || 0;
      server.ramPressure = server.ramPressure || 0;
      server.revenueBuffer = server.revenueBuffer || 0;
      server.name = server.name || server.chassis.name;
      server.powered = Boolean(server.powered) && !this.state.powerOut;
      this.updateStatus(server);
    }

    load() {
        try {
        if (typeof localStorage === 'undefined') return null;
        const raw = localStorage.getItem(SAVE_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (!parsed || !Array.isArray(parsed.servers) || !Number.isFinite(parsed.money)) return null;
        return parsed;
      } catch (error) {
        console.warn('Save load failed:', error);
        return null;
      }
    }

    save() {
      try {
        if (typeof localStorage !== 'undefined') localStorage.setItem(SAVE_KEY, JSON.stringify(this.state));
      } catch (error) {
        console.warn('Save failed:', error);
      }
    }

    reset() {
      try { if (typeof localStorage !== 'undefined') localStorage.removeItem(SAVE_KEY); } catch (_) { /* noop */ }
      this.state = this.createInitialState();
      this.normalize();
      this.save();
      this.notify('Новая песочница создана', 'success');
    }

    makePart(catalogId) {
      const source = DATA.part(catalogId);
      if (!source) throw new Error(`Unknown part: ${catalogId}`);
      const part = Object.assign({}, clone(source), {
        uid: uid(source.type),
        catalogId: source.id,
        broken: false,
        image: null,
        wear: round(82 + Math.random() * 18)
      });
      delete part.id;
      return part;
    }

    instantiateServer(templateId, condition = 'working') {
      const template = DATA.template(templateId);
      if (!template) throw new Error(`Unknown server template: ${templateId}`);
      const chassis = this.makePart(template.chassis);
      const server = {
        id: uid('srv'),
        name: template.name,
        templateId,
        chassis,
        parts: {
          cpu: this.makePart(template.cpu),
          ram: condition === 'noRam' ? [] : template.ram.map(id => this.makePart(id)),
          disks: condition === 'noDisks' ? [] : template.disks.map(id => this.makePart(id)),
          cooler: this.makePart(template.cooler),
          psu: this.makePart(template.psu)
        },
        condition,
        os: null,
        software: [],
        powered: false,
        status: 'off',
        broken: condition === 'brick',
        crashed: false,
        hung: false,
        throttling: false,
        temp: DATA.room(this.state ? this.state.roomId : 'room').ambient,
        health: condition === 'brick' ? 72 : 100,
        coolingBonus: 0,
        ramPressure: 0,
        revenueBuffer: 0,
        history: { temp: [], cpu: [], ram: [] },
        createdDay: this.state ? this.state.day : 1
      };
      if (condition === 'burntPsu') server.parts.psu.broken = true;
      if (condition === 'brick' && Math.random() < 0.45) server.parts.cooler = null;
      return server;
    }

    weighted(items, weightKey = 'weight') {
      const total = items.reduce((sum, item) => sum + (item[weightKey] || 1), 0);
      let cursor = Math.random() * total;
      for (const item of items) {
        cursor -= item[weightKey] || 1;
        if (cursor <= 0) return item;
      }
      return items[items.length - 1];
    }

    generateListingSet() {
      const listings = [];
      const count = 5 + Math.floor(Math.random() * 6);
      const conditions = [
        { id: 'working', weight: 34 }, { id: 'noRam', weight: 18 },
        { id: 'noDisks', weight: 17 }, { id: 'burntPsu', weight: 16 }, { id: 'brick', weight: 15 }
      ];
      for (let i = 0; i < count; i += 1) {
        const isServer = Math.random() < 0.56;
        if (isServer) {
          const template = this.weighted(DATA.SERVER_TEMPLATES);
          const condition = this.weighted(conditions).id;
          const conditionData = DATA.CONDITIONS[condition];
          const originalPrice = Math.max(1500, round(template.basePrice * conditionData.factor * (0.78 + Math.random() * 0.48) / 50) * 50);
          listings.push({
            id: uid('lot'), isServer: true, templateId: template.id, condition,
            title: template.name, description: conditionData.description,
            seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
            price: originalPrice, originalPrice: null, bargained: false,
            icon: DATA.part(template.chassis).icon
          });
        } else {
          const source = DATA.PARTS[Math.floor(Math.random() * DATA.PARTS.length)];
          const suffix = DATA.PART_LOT_SUFFIX[Math.floor(Math.random() * DATA.PART_LOT_SUFFIX.length)];
          listings.push({
            id: uid('lot'), isServer: false, partId: source.id, condition: 'working',
            title: source.name, description: `${source.short} · ${suffix}`,
            seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
            price: Math.max(150, round(source.value * (0.72 + Math.random() * 0.55) / 10) * 10),
            originalPrice: null, bargained: false, icon: source.icon
          });
        }
      }
      return listings;
    }

    refreshListings() {
      this.state.listings = this.generateListingSet();
      this.log(`Авито: найдено ${this.state.listings.length} новых лотов.`);
      this.notify('Лоты обновлены', 'success');
      this.save();
      return this.state.listings;
    }

    bargain(listingId) {
      const listing = this.state.listings.find(item => item.id === listingId);
      if (!listing) return { ok: false, message: 'Лот уже продан' };
      if (listing.bargained) return { ok: false, message: 'Торг уже закрыт' };
      listing.bargained = true;
      const roll = Math.random();
      if (roll < 0.68) {
        listing.originalPrice = listing.price;
        listing.price = Math.max(100, round(listing.price * 0.85 / 10) * 10);
        this.state.stats.bargains += 1;
        this.notify(`Торг удался: −15% · ${this.formatMoney(listing.price)}`, 'success');
        this.log(`Торг удался: «${listing.title}» теперь стоит ${this.formatMoney(listing.price)}.`, 'good');
        this.save();
        return { ok: true, listing };
      }
      if (roll < 0.9) {
        this.notify('Продавец: «Цена и так смешная»', 'warning');
        this.save();
        return { ok: false, message: 'В торге отказано' };
      }
      this.state.listings = this.state.listings.filter(item => item.id !== listingId);
      this.notify('Продавец послал и удалил лот', 'error');
      this.log(`Неудачный торг: продавец удалил «${listing.title}».`, 'bad');
      this.save();
      return { ok: false, removed: true, message: 'Лот удалён продавцом' };
    }

    buyListing(listingId) {
      const listing = this.state.listings.find(item => item.id === listingId);
      if (!listing) return { ok: false, message: 'Лот уже продан' };
      if (listing.isServer && this.state.servers.length >= this.room().slots) {
        this.notify('В помещении нет свободного места', 'warning');
        return { ok: false, message: 'Нет места' };
      }
      if (!this.spend(listing.price, `Покупка: ${listing.title}`)) return { ok: false, message: 'Недостаточно денег' };
      let result;
      if (listing.isServer) {
        result = this.instantiateServer(listing.templateId, listing.condition);
        this.state.servers.push(result);
        this.log(`Куплен ${result.name} за ${this.formatMoney(listing.price)} (${DATA.CONDITIONS[listing.condition].name}).`, 'good');
      } else {
        result = this.makePart(listing.partId);
        this.state.inventory.push(result);
        this.log(`Куплена деталь «${result.name}» за ${this.formatMoney(listing.price)}.`, 'good');
      }
      this.state.listings = this.state.listings.filter(item => item.id !== listingId);
      this.notify(`Куплено: ${listing.title}`, 'success');
      this.save();
      return { ok: true, item: result, isServer: listing.isServer };
    }

    spend(amount, reason) {
      amount = round(amount);
      if (this.state.money < amount) {
        this.notify(`Не хватает ${this.formatMoney(amount - this.state.money)}`, 'error');
        return false;
      }
      this.state.money -= amount;
      this.state.stats.spent += amount;
      if (reason) this.log(`${reason}: −${this.formatMoney(amount)}.`);
      return true;
    }

    payBill(amount, category, title) {
      amount = round(amount);
      if (amount <= 0) return;
      this.state.stats.spent += amount;
      this.state.stats[category] = (this.state.stats[category] || 0) + amount;
      this.state.money -= amount;
      this.log(`${title}: −${this.formatMoney(amount)}.`);
      if (this.state.money < 0) {
        this.state.debt += -this.state.money;
        this.state.money = 0;
        this.tripPower(`Провайдер отключил свет за долг ${this.formatMoney(this.state.debt)}`, true);
      }
    }

    addMoney(amount, options = {}) {
      amount = Math.max(0, round(amount));
      if (!amount) return 0;
      let remaining = amount;
      if (options.earned !== false) this.state.stats.earned += amount;
      if (this.state.debt > 0) {
        const payment = Math.min(this.state.debt, remaining);
        this.state.debt -= payment;
        remaining -= payment;
        if (payment > 0) this.log(`Автоплатёж провайдеру: −${this.formatMoney(payment)}.`);
      }
      this.state.money += remaining;
      if (this.state.debt <= 0 && this.state.powerOut && this.state.powerReason === 'debt') {
        this.state.powerOut = false;
        this.state.powerReason = '';
        this.notify('Долг погашен — провайдер включил свет', 'success');
      }
      return remaining;
    }

    sellPart(partUid) {
      const index = this.state.inventory.findIndex(part => part.uid === partUid);
      if (index < 0) return false;
      const part = this.state.inventory[index];
      const price = this.partSellPrice(part);
      this.state.inventory.splice(index, 1);
      this.addMoney(price);
      this.log(`Продано «${part.name}»: +${this.formatMoney(price)}.`, 'good');
      this.notify(`Продано за ${this.formatMoney(price)}`, 'success');
      this.save();
      return true;
    }

    partSellPrice(part) {
      return Math.max(50, round(part.value * 0.58 * (part.wear || 90) / 100 * (part.broken ? 0.22 : 1) / 10) * 10);
    }

    serverSellPrice(server) {
      const parts = [server.chassis, server.parts.cpu, server.parts.cooler, server.parts.psu, ...server.parts.ram, ...server.parts.disks].filter(Boolean);
      const value = parts.reduce((sum, part) => sum + this.partSellPrice(part), 0);
      return round(value * (server.broken ? 0.7 : 1));
    }

    sellServer(serverId) {
      const server = this.getServer(serverId);
      if (!server) return false;
      if (server.powered) {
        this.notify('Сначала выключите сервер', 'warning');
        return false;
      }
      const price = this.serverSellPrice(server);
      this.state.servers = this.state.servers.filter(item => item.id !== serverId);
      this.addMoney(price);
      this.log(`Сервер «${server.name}» продан за ${this.formatMoney(price)}.`, 'good');
      this.notify(`Сервер продан за ${this.formatMoney(price)}`, 'success');
      this.save();
      return true;
    }

    getServer(id) { return this.state.servers.find(server => server.id === id); }
    getPart(uidValue) { return this.state.inventory.find(part => part.uid === uidValue); }
    room() { return DATA.room(this.state.roomId) || DATA.ROOMS[0]; }

    createServer(chassisUid) {
      if (this.state.servers.length >= this.room().slots) {
        this.notify('Нет свободного места в стойке', 'warning');
        return null;
      }
      const index = this.state.inventory.findIndex(part => part.uid === chassisUid && part.type === 'chassis');
      if (index < 0) {
        this.notify('Нужен корпус с материнской платой', 'warning');
        return null;
      }
      const chassis = this.state.inventory.splice(index, 1)[0];
      const number = this.state.stats.built + 1;
      const server = {
        id: uid('srv'), name: `Новая сборка #${number}`, templateId: null, chassis,
        parts: { cpu: null, ram: [], disks: [], cooler: null, psu: null },
        condition: 'working', os: null, software: [], powered: false, status: 'off',
        broken: false, crashed: false, hung: false, throttling: false,
        temp: this.room().ambient, health: 100, coolingBonus: 0, ramPressure: 0,
        revenueBuffer: 0, history: { temp: [], cpu: [], ram: [] }, createdDay: this.state.day
      };
      this.state.servers.push(server);
      this.state.stats.built += 1;
      this.log(`Начата сборка «${server.name}» на ${chassis.name}.`, 'good');
      this.notify('Корпус установлен в стойку', 'success');
      this.save();
      return server;
    }

    slotType(slotKey) {
      if (slotKey === 'cpu' || slotKey === 'cooler' || slotKey === 'psu') return slotKey;
      if (slotKey.startsWith('ram')) return 'ram';
      if (slotKey.startsWith('disk')) return 'disk';
      return null;
    }

    checkCompatibility(server, part, slotKey) {
      const expected = this.slotType(slotKey);
      if (!server || !part || !expected) return { ok: false, message: 'Неверный слот' };
      if (expected === 'disk') {
        if (!['hdd', 'ssd'].includes(part.type)) return { ok: false, message: 'Сюда нужен HDD или SSD' };
      } else if (part.type !== expected) {
        return { ok: false, message: `Нужен компонент: ${expected.toUpperCase()}` };
      }
      if (part.type === 'cpu' && part.socket !== server.chassis.socket) {
        return { ok: false, message: `${part.socket} не подходит к ${server.chassis.socket}` };
      }
      if (part.type === 'ram' && part.ramType !== server.chassis.ramType) {
        return { ok: false, message: `${part.ramType} не встаёт в слот ${server.chassis.ramType}` };
      }
      if (part.broken) return { ok: false, message: 'Деталь неисправна' };
      return { ok: true };
    }

    getSlotPart(server, slotKey) {
      if (slotKey.startsWith('ram')) return server.parts.ram[Number(slotKey.slice(3))] || null;
      if (slotKey.startsWith('disk')) return server.parts.disks[Number(slotKey.slice(4))] || null;
      return server.parts[slotKey] || null;
    }

    setSlotPart(server, slotKey, part) {
      if (slotKey.startsWith('ram')) server.parts.ram[Number(slotKey.slice(3))] = part;
      else if (slotKey.startsWith('disk')) server.parts.disks[Number(slotKey.slice(4))] = part;
      else server.parts[slotKey] = part;
      server.parts.ram = server.parts.ram.filter(Boolean);
      server.parts.disks = server.parts.disks.filter(Boolean);
    }

    installPart(serverId, partUid, slotKey) {
      const server = this.getServer(serverId);
      const part = this.getPart(partUid);
      if (!server || !part) return { ok: false, message: 'Деталь не найдена' };
      if (server.powered) {
        this.notify('Выключите сервер перед разборкой', 'warning');
        return { ok: false, message: 'Сервер включён' };
      }
      const compatible = this.checkCompatibility(server, part, slotKey);
      if (!compatible.ok) {
        this.notify(compatible.message, 'error');
        return compatible;
      }
      const current = this.getSlotPart(server, slotKey);
      const index = this.state.inventory.findIndex(item => item.uid === partUid);
      this.state.inventory.splice(index, 1);
      if (current) this.state.inventory.push(current);
      this.setSlotPart(server, slotKey, part);
      this.updateStatus(server);
      this.log(`${server.name}: установлено «${part.name}».`, 'good');
      this.notify(`${part.name} установлен`, 'success');
      this.save();
      return { ok: true, part };
    }

    removePart(serverId, slotKey) {
      const server = this.getServer(serverId);
      if (!server) return false;
      if (server.powered) {
        this.notify('Сначала выключите сервер', 'warning');
        return false;
      }
      const part = this.getSlotPart(server, slotKey);
      if (!part) return false;
      this.setSlotPart(server, slotKey, null);
      this.state.inventory.push(part);
      this.updateStatus(server);
      this.log(`${server.name}: снято «${part.name}».`);
      this.save();
      return true;
    }

    dismantleServer(serverId) {
      const server = this.getServer(serverId);
      if (!server || server.powered) {
        this.notify('Сервер должен быть выключен', 'warning');
        return false;
      }
      const parts = [server.chassis, server.parts.cpu, server.parts.cooler, server.parts.psu, ...server.parts.ram, ...server.parts.disks].filter(Boolean);
      this.state.inventory.push(...parts);
      this.state.servers = this.state.servers.filter(item => item.id !== serverId);
      this.log(`«${server.name}» разобран: ${parts.length} деталей в инвентаре.`);
      this.notify('Сервер разобран на детали', 'success');
      this.save();
      return true;
    }

    hardwareReport(server) {
      const issues = [];
      const cpu = server.parts.cpu;
      const psu = server.parts.psu;
      const ramTotal = server.parts.ram.reduce((sum, part) => sum + part.capacity, 0);
      const diskTotal = server.parts.disks.reduce((sum, part) => sum + part.capacity, 0);
      let requiredPsu = 0;
      if (server.broken) issues.push('Материнская плата не отвечает — нужен ремонт');
      if (server.crashed) issues.push('BSOD после перегрева — нужен ремонт охлаждения');
      if (server.hung) issues.push('Сервер завис — нужна перезагрузка');
      if (!cpu) issues.push('Установите процессор');
      if (!server.parts.ram.length) issues.push('Установите хотя бы одну планку ОЗУ');
      if (!server.parts.cooler) issues.push('Установите кулер');
      if (!psu) issues.push('Установите блок питания');
      if (psu && psu.broken) issues.push('Блок питания сгорел — замените его');
      if (cpu) {
        requiredPsu = round(70 + cpu.tdp + cpu.cores * 35 + server.parts.disks.length * 10 + server.parts.ram.length * 4);
        if (psu && !psu.broken && psu.watts < requiredPsu) issues.push(`БП ${psu.watts}W слаб: нужно минимум ${requiredPsu}W`);
      }
      return { ok: issues.length === 0, issues, requiredPsu, ramTotal, diskTotal };
    }

    togglePower(serverId) {
      const server = this.getServer(serverId);
      if (!server) return false;
      if (server.powered) {
        this.powerOff(server);
        this.save();
        return true;
      }
      if (this.state.powerOut) {
        this.notify(this.state.debt > 0 ? `Свет отключён: долг ${this.formatMoney(this.state.debt)}` : 'Свет отключён — проверьте автомат', 'error');
        return false;
      }
      const report = this.hardwareReport(server);
      if (!report.ok) {
        this.notify(report.issues[0], 'error');
        return false;
      }
      const projected = this.farmMetrics().watts + this.metrics(server).watts;
      if (projected > this.room().powerLimit) {
        this.notify(`Линия не выдержит ${round(projected)}W`, 'error');
        return false;
      }
      server.powered = true;
      server.hung = false;
      server.ramPressure = 0;
      this.updateStatus(server);
      this.log(`${server.name}: питание включено.`, 'good');
      this.notify(server.os ? 'Сервер запущен' : 'POST пройден · теперь установите ОС', 'success');
      this.save();
      return true;
    }

    powerOff(server) {
      server.powered = false;
      server.ramPressure = 0;
      server.throttling = false;
      this.updateStatus(server);
      this.log(`${server.name}: питание выключено.`);
    }

    rebootServer(serverId) {
      const server = this.getServer(serverId);
      if (!server || !server.hung) return false;
      server.hung = false;
      server.powered = false;
      server.ramPressure = 0;
      this.updateStatus(server);
      this.notify('Перезагрузка выполнена. Снизьте расход ОЗУ перед запуском.', 'warning');
      this.log(`${server.name}: аварийная перезагрузка после зависания.`, 'bad');
      this.save();
      return true;
    }

    repairServer(serverId) {
      const server = this.getServer(serverId);
      if (!server || (!server.broken && !server.crashed)) return false;
      if (server.powered) {
        this.notify('Выключите сервер перед ремонтом', 'warning');
        return false;
      }
      const hasTools = this.state.inventory.some(part => part.type === 'tool');
      const base = server.broken ? 2800 + this.serverSellPrice(server) * 0.08 : 1200;
      const price = round(base * (hasTools ? 0.75 : 1));
      if (!this.spend(price, `Ремонт ${server.name}`)) return false;
      const wasBrick = server.broken;
      server.broken = false;
      server.crashed = false;
      server.hung = false;
      server.powered = false;
      server.temp = this.room().ambient + 4;
      server.health = Math.max(40, server.health - (wasBrick ? 4 : 1));
      if (hasTools && server.coolingBonus < 12) server.coolingBonus += 8;
      this.updateStatus(server);
      this.log(`${server.name}: ${wasBrick ? 'плата восстановлена' : 'охлаждение перебрано'} за ${this.formatMoney(price)}.`, 'good');
      this.notify(hasTools ? 'Ремонт готов · нанесена свежая термопаста' : 'Ремонт завершён', 'success');
      this.save();
      return true;
    }

    restorePower() {
      if (!this.state.powerOut) return false;
      if (this.state.debt > 0) {
        this.notify(`Сначала погасите долг ${this.formatMoney(this.state.debt)}`, 'error');
        return false;
      }
      this.state.powerOut = false;
      this.state.powerReason = '';
      this.notify('Автомат включён. Запускайте серверы по очереди.', 'success');
      this.log('Электропитание в помещении восстановлено.', 'good');
      this.save();
      return true;
    }

    tripPower(reason, debt = false) {
      this.state.powerOut = true;
      this.state.powerReason = debt ? 'debt' : 'breaker';
      this.state.servers.forEach(server => {
        server.powered = false;
        server.ramPressure = 0;
        this.updateStatus(server);
      });
      this.notify(reason, 'error');
      this.log(reason, 'bad');
    }

    writeImage(partUid, osId) {
      const usb = this.getPart(partUid);
      const os = DATA.os(osId);
      if (!usb || usb.type !== 'usb' || !os) return false;
      if (os.price && !this.spend(os.price, `Лицензия ${os.name}`)) return false;
      usb.image = os.id;
      this.log(`На ${usb.name} записан образ ${os.name}.`, 'good');
      this.notify(`${os.name} записан на флешку`, 'success');
      this.save();
      return true;
    }

    checkOSInstall(serverId, usbUid) {
      const server = this.getServer(serverId);
      const usb = this.getPart(usbUid);
      if (!server || !usb || usb.type !== 'usb' || !usb.image) return { ok: false, message: 'Нужна флешка с записанным образом' };
      const os = DATA.os(usb.image);
      const report = this.hardwareReport(server);
      const issues = report.issues.filter(issue => !issue.includes('завис'));
      if (this.state.powerOut) issues.unshift('Нет электричества');
      if (report.ramTotal < os.minRam) issues.push(`${os.name}: нужно минимум ${os.minRam} ГБ ОЗУ`);
      if (report.diskTotal < os.minDisk) issues.push(`${os.name}: нужно минимум ${os.minDisk} ГБ накопителей`);
      if (!report.diskTotal) issues.push('Установите HDD или SSD');
      if (issues.length) return { ok: false, message: issues[0], issues };
      return { ok: true, server, usb, os };
    }

    finishOSInstall(serverId, usbUid) {
      const check = this.checkOSInstall(serverId, usbUid);
      if (!check.ok) {
        this.notify(check.message, 'error');
        return false;
      }
      const { server, os } = check;
      server.os = os.id;
      server.software = [];
      server.powered = true;
      server.hung = false;
      server.ramPressure = 0;
      this.updateStatus(server);
      this.log(`${server.name}: установлена ${os.name}.`, 'good');
      this.notify(`${os.name} установлена — сервер онлайн`, 'success');
      this.save();
      return true;
    }

    installSoftware(serverId, softwareId) {
      const server = this.getServer(serverId);
      const software = DATA.software(softwareId);
      if (!server || !software) return false;
      if (!server.powered || !server.os || server.hung || server.crashed) {
        this.notify('Сервер должен быть запущен под управлением ОС', 'warning');
        return false;
      }
      if (!software.os.includes(server.os)) {
        this.notify(`${software.name} не поддерживается на ${DATA.os(server.os).name}`, 'error');
        return false;
      }
      if (server.software.includes(softwareId)) return false;
      const ramTotal = this.hardwareReport(server).ramTotal;
      if (ramTotal < software.minRam) {
        this.notify(`Нужно минимум ${software.minRam} ГБ ОЗУ`, 'error');
        return false;
      }
      server.software.push(softwareId);
      const metrics = this.metrics(server);
      this.log(`${server.name}: установлен ${software.name}.`, 'good');
      this.notify(`${software.name} установлен`, 'success');
      if (metrics.ramPercent >= 90) this.notify(`Внимание: ОЗУ будет занята на ${round(metrics.ramPercent)}%`, 'warning');
      this.save();
      return true;
    }

    uninstallSoftware(serverId, softwareId) {
      const server = this.getServer(serverId);
      if (!server) return false;
      const software = DATA.software(softwareId);
      server.software = server.software.filter(id => id !== softwareId);
      server.ramPressure = 0;
      this.log(`${server.name}: удалён ${software ? software.name : softwareId}.`);
      this.notify('Софт удалён', 'success');
      this.save();
      return true;
    }

    metrics(server) {
      const room = this.room();
      const cpu = server.parts.cpu;
      const psu = server.parts.psu;
      const ramTotal = server.parts.ram.reduce((sum, part) => sum + part.capacity, 0);
      const os = server.os ? DATA.os(server.os) : null;
      const apps = server.software.map(id => DATA.software(id)).filter(Boolean);
      const ramUsed = (os ? os.baseRam : 0.15) + apps.reduce((sum, app) => sum + app.ram, 0);
      const ramPercent = ramTotal ? ramUsed / ramTotal * 100 : 0;
      const cpuDemand = server.powered ? (os ? os.baseCpu : 3) + apps.reduce((sum, app) => sum + app.cpu, 0) : 0;
      const rawLoad = clamp(cpuDemand, 0, 100);
      const throttleFactor = server.temp > 85 ? 0.55 : 1;
      const cpuLoad = server.powered ? rawLoad * throttleFactor : 0;
      const cooling = server.parts.cooler ? (server.parts.cooler.cooling + server.coolingBonus) * server.chassis.coolingFactor : 20;
      const heat = cpu ? cpu.tdp * (0.15 + 0.85 * cpuLoad / 100) + server.chassis.basePower * 0.55 : 5;
      const targetTemp = server.powered ? room.ambient + heat * 64.8 / Math.max(25, cooling) : room.ambient;
      const baseWatts = server.powered ? server.chassis.basePower + 8 : 0;
      const cpuWatts = server.powered && cpu ? cpu.tdp * (0.18 + 0.82 * cpuLoad / 100) : 0;
      const ramWatts = server.powered ? server.parts.ram.reduce((sum, part) => sum + part.watts, 0) : 0;
      const diskWatts = server.powered ? server.parts.disks.reduce((sum, part) => sum + part.watts, 0) : 0;
      const watts = (baseWatts + cpuWatts + ramWatts + diskWatts) / (psu && !psu.broken ? psu.efficiency : 0.75);
      const noise = server.powered ? clamp(server.chassis.baseNoise + (server.parts.cooler ? server.parts.cooler.noise : 14) + (psu ? psu.noise : 5) + (rawLoad > 80 ? 4 : 0), 15, 96) : 0;
      let efficiency = 1;
      if (server.temp > 85) efficiency *= 0.45;
      if (ramPercent > 90) efficiency *= ramPercent >= 100 ? 0.15 : 0.65;
      if (cpuDemand > 100) efficiency *= 100 / cpuDemand;
      efficiency *= clamp(server.health / 100, 0.5, 1);
      return {
        watts, noise, targetTemp, cpuLoad, rawLoad, cpuDemand, ramUsed, ramTotal,
        ramPercent, efficiency: clamp(efficiency, 0, 1), cooling,
        throttling: server.temp > 85
      };
    }

    farmMetrics() {
      const active = this.state.servers.filter(server => server.powered);
      const metrics = active.map(server => this.metrics(server));
      const watts = metrics.reduce((sum, item) => sum + item.watts, 0);
      const noise = metrics.length ? 10 * Math.log10(metrics.reduce((sum, item) => sum + Math.pow(10, item.noise / 10), 0)) : 0;
      const temps = this.state.servers.map(server => server.temp).filter(Number.isFinite);
      const hourlyRevenue = active.reduce((sum, server) => sum + this.hourlyServerRevenue(server), 0);
      return {
        watts, noise, hourlyRevenue,
        temp: temps.length ? Math.max(...temps) : this.room().ambient,
        online: active.filter(server => server.os && !server.hung && !server.crashed).length,
        active: active.length
      };
    }

    hourlyServerRevenue(server) {
      if (!server.powered || !server.os || server.crashed || server.hung || this.state.powerOut) return 0;
      const metrics = this.metrics(server);
      const cores = server.parts.cpu ? server.parts.cpu.cores : 0;
      return server.software.reduce((sum, id) => {
        const app = DATA.software(id);
        if (!app) return sum;
        return sum + (app.rate + (app.coreRate || 0) * cores) * metrics.efficiency;
      }, 0);
    }

    updateStatus(server) {
      if (server.broken) server.status = 'brick';
      else if (server.crashed) server.status = 'bsod';
      else if (server.hung) server.status = 'hung';
      else if (server.powered) server.status = server.os ? 'online' : 'bios';
      else server.status = 'off';
    }

    simulateServer(server) {
      const ambient = this.room().ambient;
      if (!server.powered) {
        server.temp += (ambient - server.temp) * 0.025;
        this.updateStatus(server);
        return;
      }
      const metrics = this.metrics(server);
      server.temp += (metrics.targetTemp - server.temp) * 0.075 + (Math.random() - 0.5) * 0.18;
      server.temp = clamp(server.temp, ambient - 2, 110);
      if (server.temp > 85 && !server.throttling) {
        server.throttling = true;
        this.notify(`${server.name}: троттлинг · ${round(server.temp)}°C`, 'warning');
        this.log(`${server.name}: температура выше 85°C, производительность снижена.`, 'bad');
      } else if (server.temp < 81) {
        server.throttling = false;
      }
      if (server.temp >= 95) {
        server.crashed = true;
        server.powered = false;
        server.throttling = false;
        server.temp = 95;
        this.state.stats.crashes += 1;
        this.updateStatus(server);
        this.notify(`${server.name}: BSOD от перегрева!`, 'error');
        this.log(`${server.name}: критические 95°C, синий экран и аварийное отключение.`, 'bad');
        return;
      }
      if (metrics.ramPercent >= 100 && server.software.length) {
        server.ramPressure += 12;
        if (server.ramPressure === 36) this.notify(`${server.name}: ОЗУ 100%, сервер теряет пакеты`, 'warning');
        if (server.ramPressure >= 60) {
          server.hung = true;
          server.powered = false;
          server.ramPressure = 0;
          this.state.stats.hangs += 1;
          this.updateStatus(server);
          this.notify(`${server.name}: завис из-за нехватки ОЗУ`, 'error');
          this.log(`${server.name}: OOM-killer не справился, система зависла.`, 'bad');
          return;
        }
      } else {
        server.ramPressure = Math.max(0, server.ramPressure - 18);
      }
      this.state.stats.energyToday += metrics.watts * (12 / 60) / 1000;
      if (this.tickCounter % 5 === 0) {
        const history = server.history;
        history.temp.push(round(server.temp * 10) / 10);
        history.cpu.push(round(metrics.cpuLoad));
        history.ram.push(round(clamp(metrics.ramPercent, 0, 120)));
        ['temp', 'cpu', 'ram'].forEach(key => { if (history[key].length > 48) history[key].shift(); });
      }
      this.updateStatus(server);
    }

    collectHourlyRevenue() {
      let total = 0;
      this.state.servers.forEach(server => {
        const amount = this.hourlyServerRevenue(server);
        if (amount <= 0) return;
        server.revenueBuffer += amount;
        if (server.revenueBuffer >= 1) {
          const whole = Math.floor(server.revenueBuffer);
          server.revenueBuffer -= whole;
          total += whole;
        }
      });
      if (total > 0) {
        this.addMoney(total);
        this.state.stats.revenueToday += total;
      }
    }

    endDay() {
      const endedDay = this.state.day;
      const electricity = round(this.state.stats.energyToday * ELECTRICITY_PRICE);
      const rent = this.room().rent;
      const roomNoise = this.farmMetrics().noise;
      const fine = roomNoise > this.room().noiseLimit ? round(1200 + (roomNoise - this.room().noiseLimit) * 180) : 0;
      const revenue = this.state.stats.revenueToday;
      if (electricity) this.payBill(electricity, 'electricity', `Электричество · ${this.state.stats.energyToday.toFixed(2)} кВт·ч`);
      this.payBill(rent, 'rent', `Аренда «${this.room().name}»`);
      if (fine) {
        this.state.stats.finesToday += fine;
        this.payBill(fine, 'noiseFines', `Штраф за шум · ${round(roomNoise)} дБ`);
        this.notify(this.state.roomId === 'room' ? 'Мама: «Этот самолёт не даёт спать!» Штраф.' : 'Соседи пожаловались на шум. Штраф.', 'warning');
      }
      const profit = revenue - electricity - rent - fine;
      this.state.stats.daily.push({
        day: endedDay, revenue, electricity, rent, fine, profit,
        energy: round(this.state.stats.energyToday * 100) / 100
      });
      if (this.state.stats.daily.length > 40) this.state.stats.daily.shift();
      this.log(`Итог дня ${endedDay}: ${profit >= 0 ? '+' : ''}${this.formatMoney(profit)} · доход ${this.formatMoney(revenue)}.`, profit >= 0 ? 'good' : 'bad');
      this.notify(`День ${endedDay}: ${profit >= 0 ? 'прибыль' : 'убыток'} ${this.formatMoney(Math.abs(profit))}`, profit >= 0 ? 'success' : 'warning');
      this.state.day += 1;
      this.state.stats.energyToday = 0;
      this.state.stats.revenueToday = 0;
      this.state.stats.finesToday = 0;
      this.save();
    }

    tick() {
      if (this.state.paused) return;
      this.tickCounter += 1;
      const previousMinute = this.state.minute;
      this.state.minute += 12;
      this.state.servers.forEach(server => this.simulateServer(server));
      const farm = this.farmMetrics();
      if (!this.state.powerOut && farm.watts > this.room().powerLimit) {
        this.tripPower(`Автомат сработал: ${round(farm.watts)}W при лимите ${this.room().powerLimit}W`);
      }
      if (Math.floor(previousMinute / 60) !== Math.floor(this.state.minute / 60)) this.collectHourlyRevenue();
      if (this.state.minute >= 1440) {
        this.state.minute -= 1440;
        this.endDay();
      }
      this.saveTimer += 1;
      if (this.saveTimer >= 10) {
        this.saveTimer = 0;
        this.save();
      }
    }

    upgradeRoom() {
      const index = DATA.ROOMS.findIndex(room => room.id === this.state.roomId);
      const next = DATA.ROOMS[index + 1];
      if (!next) return false;
      if (!this.spend(next.price, `Переезд: ${next.name}`)) return false;
      this.state.servers.forEach(server => { if (server.powered) this.powerOff(server); });
      this.state.roomId = next.id;
      this.state.powerOut = false;
      this.state.powerReason = '';
      this.log(`Новое помещение: ${next.name}. Аренда ${this.formatMoney(next.rent)}/день.`, 'good');
      this.notify(`Добро пожаловать: ${next.name}`, 'success');
      this.save();
      return true;
    }

    momHelp() {
      if (this.state.day - this.state.flags.lastMomDay < 3) {
        this.notify('Мама помогает не чаще одного раза в 3 дня', 'warning');
        return false;
      }
      this.state.flags.lastMomDay = this.state.day;
      this.state.stats.momHelp += 1;
      this.addMoney(20000);
      this.log('Мама выдала 20 000 ₽ «на нормальную работу».', 'good');
      this.notify('Мама помогла: +20 000 ₽', 'success');
      this.save();
      return true;
    }

    cheat(amount = 100000) {
      amount = clamp(round(amount), 1000, 10000000);
      this.state.stats.cheated += amount;
      this.addMoney(amount, { earned: false });
      this.log(`Sandbox-чит: +${this.formatMoney(amount)}.`, 'good');
      this.notify(`Баланс пополнен на ${this.formatMoney(amount)}`, 'success');
      this.save();
      return true;
    }

    skipDay() {
      if (this.state.paused) this.state.paused = false;
      // Быстро прогоняем те же тики, что работают в реальном времени:
      // доход, нагрев, аварии и отключения не пропускаются.
      const startDay = this.state.day;
      let guard = 0;
      while (this.state.day === startDay && guard < 125) {
        this.tick();
        guard += 1;
      }
      this.save();
    }

    inventoryValue() {
      return this.state.inventory.reduce((sum, part) => sum + this.partSellPrice(part), 0);
    }

    log(text, type = 'info') {
      if (!this.state || !this.state.logs) return;
      this.state.logs.unshift({ id: uid('log'), day: this.state.day, text, type });
      if (this.state.logs.length > 80) this.state.logs.length = 80;
    }

    notify(text, type = 'info') {
      this.events.push({ text, type, id: uid('event') });
      if (this.events.length > 8) this.events.shift();
    }

    drainEvents() {
      const events = this.events.slice();
      this.events.length = 0;
      return events;
    }

    formatMoney(value) {
      return `${round(value).toLocaleString('ru-RU')} ₽`;
    }

    clock() {
      const hours = Math.floor(this.state.minute / 60) % 24;
      const minutes = Math.floor(this.state.minute % 60);
      return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
    }
  }

  window.ServerTycoonGame = ServerTycoonGame;
  if (typeof module !== 'undefined' && module.exports) module.exports = ServerTycoonGame;
})();
