/* Time Machine 5.0: стартовый год, развитие эпох, исследования и совместимость legacy. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !window.DATA || Game.prototype.timelineLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(value);
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;

  // Ранние стартовые системы: без них в архиве 1990 года не было ни одного загрузочного образа.
  DATA.OS_LIST.push(
    { id: 'unixv4', name: 'UNIX System V/386 4.0', year: 1990, family: 'unix', brand: 'server', icon: 'os', price: 0, minRam: .016, minDisk: .05, baseRam: .012, baseCpu: 1, description: 'System V · shell · ранний UNIX', terminal: 'sysv: setup /dev/hd0' },
    { id: 'netware21', name: 'Novell NetWare 2.1', year: 1990, family: 'legacy', brand: 'files', icon: 'os', price: 0, minRam: .016, minDisk: .05, baseRam: .012, baseCpu: 1, description: 'IPX · файлы и печать · ранний NetWare', terminal: 'netware: bindery init' },
    { id: 'msdos50', name: 'MS-DOS 5.0', year: 1991, family: 'legacy', brand: 'windows', icon: 'os', price: 0, minRam: .008, minDisk: .025, baseRam: .006, baseCpu: 0, description: 'DOS · FAT16 · ранние утилиты', terminal: 'C:\\> setup.exe' }
  );
  const osIndex = new Map(DATA.OS_LIST.map(os => [os.id, os]));
  DATA.os = id => osIndex.get(id);

  // Исторические службы больше не зависят от современных Debian 12/13.
  const windowsLegacy = DATA.OS_LIST.filter(os => os.family === 'windows' && os.year <= 2008).map(os => os.id);
  const unixLegacy = DATA.OS_LIST.filter(os => os.year <= 2008 && ['linux', 'unix', 'bsd'].includes(os.family)).map(os => os.id);
  const netwareLegacy = DATA.OS_LIST.filter(os => /^netware/.test(os.id)).map(os => os.id);
  DATA.SOFTWARE.filter(app => app.category === 'legacy').forEach(app => {
    let expanded = app.os.slice();
    if (app.os.some(id => /^netware/.test(id))) expanded = [...expanded, ...netwareLegacy];
    else if (app.os.includes('os2warp')) expanded = [...expanded, 'os2warp'];
    else if (app.os.some(id => windowsLegacy.includes(id))) expanded = [...expanded, ...windowsLegacy];
    else expanded = [...expanded, ...unixLegacy];
    app.os = [...new Set(expanded)];
  });

  proto.currentYear = function () {
    return this.state && Number.isFinite(this.state.currentYear) ? this.state.currentYear : 2026;
  };
  proto.availableTemplates = function () {
    const year = this.currentYear();
    return DATA.SERVER_TEMPLATES.filter(template => (template.year || 2020) <= year);
  };
  proto.availableParts = function () {
    const year = this.currentYear();
    return DATA.PARTS.filter(part => (part.year || 2020) <= year);
  };
  proto.availableOS = function () {
    const year = this.currentYear();
    return DATA.OS_LIST.filter(os => (os.year || 2020) <= year);
  };
  proto.starterTemplateForYear = function (year) {
    year = clamp(Math.round(Number(year) || 2026), 1990, 2026);
    if (year === 2026) return DATA.template('office6000');
    const candidates = DATA.SERVER_TEMPLATES.filter(template => template.year === year);
    const minRam = year < 2000 ? .128 : year < 2005 ? 1 : 4;
    const minDisk = year < 2000 ? .5 : 16;
    const scored = candidates.map(template => {
      const cpu = DATA.part(template.cpu);
      const ram = template.ram.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const disk = template.disks.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const cooling = DATA.part(template.cooler).cooling / Math.max(1, cpu.tdp);
      const viable = ram >= minRam && disk >= minDisk;
      const score = (viable ? 10000 : 0) + Math.min(ram, minRam * 2) * 30 + Math.min(disk, minDisk * 2) + cooling * 20 + cpu.cores * 2;
      return { template, score };
    }).sort((a, b) => b.score - a.score || a.template.id.localeCompare(b.template.id));
    return (scored[0] && scored[0].template) || DATA.SERVER_TEMPLATES.find(template => template.year === year) || DATA.template('office6000');
  };

  const baseCreateInitialState = proto.createInitialState;
  proto.createInitialState = function (startYear = 2026) {
    const state = baseCreateInitialState.call(this);
    startYear = clamp(Math.round(Number(startYear) || 2026), 1990, 2026);
    this.state = state;
    state.startYear = startYear;
    state.currentYear = startYear;
    state.yearDay = 0;
    state.daysPerYear = 12;
    state.research = { points: 1, cooling: 0, efficiency: 0, marketing: 0, yearsCompleted: 0 };
    if (startYear !== 2026) {
      const template = this.starterTemplateForYear(startYear);
      const starter = this.instantiateServer(template.id, 'working');
      starter.name = `Первая машина · ${startYear}`;
      starter.os = null;
      starter.powered = false;
      state.servers = [starter];
      state.inventory = [this.makePart('usb16'), this.makePart(template.ram[0]), this.makePart('toolkit')];
      state.logs = [];
      this.log(`Time Machine: старт в ${startYear} году. Новые технологии открываются каждые 12 игровых дней.`, 'good');
      state.listings = this.generateListingSet();
    }
    return state;
  };

  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const result = baseNormalize.call(this);
    const s = this.state;
    s.version = 7;
    s.startYear = Number.isFinite(s.startYear) ? clamp(s.startYear, 1990, 2026) : 2026;
    s.currentYear = Number.isFinite(s.currentYear) ? clamp(s.currentYear, s.startYear, 2026) : 2026;
    s.yearDay = Number.isFinite(s.yearDay) ? clamp(s.yearDay, 0, 11) : 0;
    s.daysPerYear = 12;
    s.research = Object.assign({ points: 1, cooling: 0, efficiency: 0, marketing: 0, yearsCompleted: 0 }, s.research || {});
    ['cooling', 'efficiency', 'marketing'].forEach(key => { s.research[key] = clamp(Number(s.research[key]) || 0, 0, 5); });
    s.research.points = Math.max(0, Number(s.research.points) || 0);
    s.research.yearsCompleted = Math.max(0, Number(s.research.yearsCompleted) || 0);
    return result;
  };

  proto.startTimeline = function (year) {
    if (this.state.flags.onboarded) {
      this.notify('Смена эпохи доступна только в новой игре', 'warning');
      return false;
    }
    this.state = this.createInitialState(year);
    this.normalize();
    this.events.length = 0;
    this.save();
    this.notify(`Time Machine: ${this.currentYear()} год`, 'success');
    return true;
  };

  proto.timelineInfo = function () {
    const year = this.currentYear();
    return {
      startYear: this.state.startYear, currentYear: year, yearDay: this.state.yearDay,
      daysPerYear: 12, nextIn: 12 - this.state.yearDay,
      progress: this.state.yearDay / 12 * 100,
      availableServers: this.availableTemplates().length, totalServers: DATA.SERVER_TEMPLATES.length,
      availableParts: this.availableParts().length, totalParts: DATA.PARTS.length,
      availableOS: this.availableOS().length, totalOS: DATA.OS_LIST.length,
      research: this.state.research
    };
  };

  // Тот же генератор 5–10 лотов, но только с уже существующим железом эпохи.
  proto.generateListingSet = function () {
    const listings = [];
    const templates = this.availableTemplates();
    const parts = this.availableParts();
    const count = 5 + Math.floor(Math.random() * 6);
    const conditions = [
      { id: 'working', weight: 34 }, { id: 'noRam', weight: 18 },
      { id: 'noDisks', weight: 17 }, { id: 'burntPsu', weight: 16 }, { id: 'brick', weight: 15 }
    ];
    for (let i = 0; i < count; i += 1) {
      const isServer = Math.random() < .56;
      if (isServer && templates.length) {
        const template = this.weighted(templates);
        const condition = this.weighted(conditions).id;
        const conditionData = DATA.CONDITIONS[condition];
        const price = Math.max(1500, round(template.basePrice * conditionData.factor * (.78 + Math.random() * .48) / 50) * 50);
        listings.push({ id: uid('lot'), isServer: true, templateId: template.id, condition, title: template.name,
          description: conditionData.description, seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
          price, originalPrice: null, bargained: false, icon: DATA.part(template.chassis).icon, year: template.year });
      } else if (parts.length) {
        const source = parts[Math.floor(Math.random() * parts.length)];
        const suffix = DATA.PART_LOT_SUFFIX[Math.floor(Math.random() * DATA.PART_LOT_SUFFIX.length)];
        listings.push({ id: uid('lot'), isServer: false, partId: source.id, condition: 'working', title: source.name,
          description: `${source.short} · ${suffix}`, seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
          price: Math.max(150, round(source.value * (.72 + Math.random() * .55) / 10) * 10),
          originalPrice: null, bargained: false, icon: source.icon, year: source.year });
      }
    }
    return listings;
  };

  const baseBuyCatalog = proto.buyCatalogServer;
  proto.buyCatalogServer = function (templateId) {
    const template = DATA.template(templateId);
    if (template && (template.year || 2020) > this.currentYear()) {
      this.notify(`${template.year}: технология ещё не открыта`, 'warning');
      return false;
    }
    return baseBuyCatalog.call(this, templateId);
  };
  const baseWriteImage = proto.writeImage;
  proto.writeImage = function (partUid, osId) {
    const os = DATA.os(osId);
    if (os && (os.year || 2020) > this.currentYear()) {
      this.notify(`${os.name}: образ появится в ${os.year} году`, 'warning');
      return false;
    }
    return baseWriteImage.call(this, partUid, osId);
  };
  const baseCheckOS = proto.checkOSInstall;
  proto.checkOSInstall = function (serverId, usbUid) {
    const check = baseCheckOS.call(this, serverId, usbUid);
    if (check.ok && check.os && (check.os.year || 2020) > this.currentYear()) return { ok: false, message: `${check.os.name}: нужен ${check.os.year} год` };
    return check;
  };

  const baseEndDay = proto.endDay;
  proto.endDay = function () {
    const result = baseEndDay.call(this);
    const s = this.state;
    if (s.currentYear < 2026) {
      s.yearDay += 1;
      if (s.yearDay >= 12) {
        s.yearDay = 0;
        s.currentYear += 1;
        s.research.points += 1;
        s.research.yearsCompleted += 1;
        this.addXP(200);
        this.log(`Наступил ${s.currentYear} год: каталог и Авито обновлены, получено 1 очко исследований.`, 'good');
        this.notify(`${s.currentYear} год · новые технологии и +1 research`, 'success');
        s.listings = this.generateListingSet();
      }
    } else {
      s.yearDay += 1;
      if (s.yearDay >= 12) {
        s.yearDay = 0;
        s.research.points += 1;
        s.research.yearsCompleted += 1;
        this.addXP(100);
        this.log('Завершён цикл R&D 2026: получено 1 очко исследований.', 'good');
        this.notify('Modern R&D · +1 research', 'success');
      }
    }
    this.save();
    return result;
  };

  const baseMetrics = proto.metrics;
  proto.metrics = function (server) {
    const m = baseMetrics.call(this, server);
    const research = this.state && this.state.research;
    if (!research) return m;
    if (research.cooling > 0) {
      const factor = 1 + research.cooling * .05;
      m.targetTemp = this.room().ambient + (m.targetTemp - this.room().ambient) / factor;
      m.cooling *= factor;
    }
    if (research.efficiency > 0) m.watts *= Math.max(.8, 1 - research.efficiency * .04);
    return m;
  };
  const baseHourly = proto.hourlyServerRevenue;
  proto.hourlyServerRevenue = function (server) {
    const base = baseHourly.call(this, server);
    const marketing = this.state && this.state.research ? this.state.research.marketing : 0;
    return base * (1 + marketing * .04);
  };

  proto.buyResearch = function (kind) {
    if (!['cooling', 'efficiency', 'marketing'].includes(kind)) return false;
    const research = this.state.research;
    const level = research[kind];
    const cost = level + 1;
    if (level >= 5 || research.points < cost) {
      this.notify(level >= 5 ? 'Исследование уже завершено' : `Нужно ${cost} очков исследований`, 'warning');
      return false;
    }
    research.points -= cost;
    research[kind] += 1;
    const names = { cooling: 'Криогенный контур', efficiency: 'Энергоэффективность', marketing: 'Продажи хостинга' };
    this.log(`${names[kind]}: уровень ${research[kind]}.`, 'good');
    this.notify(`${names[kind]} · уровень ${research[kind]}`, 'success');
    this.addXP(100);
    this.save();
    return true;
  };

  proto.timelineLoaded = true;
})();
