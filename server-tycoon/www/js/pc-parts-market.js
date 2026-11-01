/* PC Parts Avito 8.2: desktop/laptop лоты, склад и использование деталей в сборках. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !DATA.PC_PART_CATALOG || !DATA.LAPTOP_PART_CATALOG || Game.prototype.pcPartsMarketLoaded) return;
  const proto = Game.prototype; const clamp = (value, min, max) => Math.max(min, Math.min(max, value)); const round = value => Math.round(Number(value) || 0);
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').replace(/[^a-z0-9а-я]+/gi, '');
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const randomFor = initial => { let x = initial >>> 0 || 1; return () => { x ^= x << 13; x ^= x >>> 17; x ^= x << 5; return (x >>> 0) / 4294967296; }; };
  const conditions = {
    new: { id: 'new', name: 'Новый', multiplier: .96, health: 100 }, excellent: { id: 'excellent', name: 'Отличное', multiplier: .79, health: 90 },
    good: { id: 'good', name: 'Хорошее', multiplier: .63, health: 75 }, used: { id: 'used', name: 'Б/у', multiplier: .46, health: 55 }
  };
  const sellers = ['Алексей · локально','PC-разбор','Склад техники','Мастерская города','Частный продавец','Retro Components'];
  const partFor = (family, id) => family === 'laptop' ? DATA.LAPTOP_PART_INDEX.get(id) : DATA.PC_PART_INDEX.get(id);
  const validFamily = family => family === 'laptop' ? 'laptop' : 'desktop';

  proto.pcPartsMarketState = function () {
    const pc = this.state.pc; if (!pc.pcPartsMarket) pc.pcPartsMarket = { listings: [], refreshes: 0, lastDay: 0, soldIds: [] };
    const market = pc.pcPartsMarket; market.refreshes = Math.max(0, Number(market.refreshes) || 0); market.lastDay = Math.max(0, Number(market.lastDay) || 0);
    market.soldIds = Array.isArray(market.soldIds) ? [...new Set(market.soldIds.map(String))].slice(-200) : [];
    market.listings = Array.isArray(market.listings) ? market.listings.filter(listing => listing && partFor(listing.family, listing.catalogId)).slice(0, 60) : [];
    if (!Array.isArray(pc.componentInventory)) pc.componentInventory = [];
    pc.componentInventory = pc.componentInventory.filter(item => item && partFor(item.family, item.catalogId)).slice(0, 200).map(item => {
      const part = partFor(item.family, item.catalogId); item.family = validFamily(item.family); item.type = part.type; item.uid = String(item.uid || uid('component'));
      item.condition = conditions[item.condition] ? item.condition : 'good'; item.health = clamp(Number(item.health) || conditions[item.condition].health, 1, 100);
      item.paid = Math.max(0, Number(item.paid) || 0); item.year = part.year; item.acquiredDay = Math.max(1, Number(item.acquiredDay) || pc.day); return item;
    });
    return market;
  };
  const baseMarketNormalize = proto.normalize;
  proto.normalize = function () { const result = baseMarketNormalize.call(this); if (this.state.pc) this.pcPartsMarketState(); return result; };
  const baseMarketStart = proto.startPCCampaign;
  proto.startPCCampaign = function () { const result = baseMarketStart.call(this); this.pcPartsMarketState(); return result; };

  function makeListing(part, family, salt = 0) {
    const seed = hash(`${part.id}:${salt}`); const conditionKeys = Object.keys(conditions); const condition = conditions[conditionKeys[seed % conditionKeys.length]];
    const price = Math.max(10, round(part.value * condition.multiplier * (.9 + (seed >>> 8) % 20 / 100)));
    return { id: `pcm_${part.id}_${condition.id}_${seed % 997}`, catalogId: part.id, family, type: part.type, year: part.year, brand: part.brand,
      condition: condition.id, health: condition.health, price, seller: sellers[(seed >>> 4) % sellers.length], createdDay: 0 };
  }
  proto.pcPartsMarketListing = function (part, family, salt = 0) { return makeListing(part, validFamily(family), salt); };
  proto.refreshPCPartsMarket = function () {
    const market = this.pcPartsMarketState(); const year = this.state.pc.year; const random = randomFor(hash(`pc-market:${this.state.pc.day}:${market.refreshes}`));
    const desktop = DATA.PC_PART_CATALOG.filter(part => part.year <= year); const laptop = DATA.LAPTOP_PART_CATALOG.filter(part => part.year <= year);
    const listings = []; const ids = new Set(); let guard = 0;
    while (listings.length < 24 && guard < 300) {
      guard += 1; const family = random() < .58 ? 'desktop' : 'laptop'; const source = family === 'desktop' ? desktop : laptop; if (!source.length) continue;
      const part = source[Math.floor(random() * source.length)]; const listing = makeListing(part, family, market.refreshes * 31 + guard);
      if (ids.has(listing.id)) continue; ids.add(listing.id); listing.createdDay = this.state.pc.day; listings.push(listing);
    }
    market.listings = listings; market.refreshes += 1; market.lastDay = this.state.pc.day; this.save(); return listings;
  };
  function ensureMarket(game) { const market = game.pcPartsMarketState(); if (!market.listings.length) game.refreshPCPartsMarket(); return market; }
  proto.pcFindPartsListings = function (query = '', filters = {}) {
    const market = ensureMarket(this); const text = normalize(query); const currentYear = this.state.pc.year; const unlockAll = this.state.pc.cheats && this.state.pc.cheats.unlockAll;
    const filtered = Boolean(text || filters.family && filters.family !== 'all' || filters.type && filters.type !== 'all' || filters.brand && filters.brand !== 'all' ||
      filters.socket && filters.socket !== 'all' || filters.year || filters.minPrice !== '' && filters.minPrice != null || filters.maxPrice !== '' && filters.maxPrice != null);
    const sold = new Set(market.soldIds);
    const matches = (part, listing) => {
      if (sold.has(listing.id)) return false;
      if (!unlockAll && part.year > currentYear) return false;
      if (text && !normalize(`${part.name} ${part.sku} ${part.brand} ${part.summary} ${part.socket || ''} ${part.interface || ''}`).includes(text)) return false;
      if (filters.family && filters.family !== 'all' && filters.family !== listing.family) return false;
      if (filters.type && filters.type !== 'all' && part.type !== filters.type) return false;
      if (filters.brand && filters.brand !== 'all' && part.brand !== filters.brand) return false;
      if (filters.socket && filters.socket !== 'all' && part.socket !== filters.socket && part.interface !== filters.socket && !(part.sockets || []).includes(filters.socket)) return false;
      if (filters.year && Number(filters.year) !== part.year) return false;
      if (filters.minPrice !== '' && filters.minPrice != null && listing.price < Number(filters.minPrice)) return false;
      if (filters.maxPrice !== '' && filters.maxPrice != null && listing.price > Number(filters.maxPrice)) return false;
      return true;
    };
    let results;
    if (filtered) {
      const candidates = [];
      DATA.PC_PART_CATALOG.forEach(part => { const listing = makeListing(part, 'desktop'); if (matches(part, listing)) candidates.push({ part, listing }); });
      DATA.LAPTOP_PART_CATALOG.forEach(part => { const listing = makeListing(part, 'laptop'); if (matches(part, listing)) candidates.push({ part, listing }); });
      candidates.sort((a, b) => {
        if (text) { const an = normalize(a.part.name); const bn = normalize(b.part.name); const ar = an.startsWith(text) ? 0 : an.includes(text) ? 1 : 2; const br = bn.startsWith(text) ? 0 : bn.includes(text) ? 1 : 2; if (ar !== br) return ar - br; }
        return a.part.year - b.part.year || a.listing.price - b.listing.price || a.part.name.localeCompare(b.part.name, 'ru');
      });
      results = candidates.slice(0, Math.min(60, Number(filters.limit) || 24)).map(item => item.listing);
    } else results = market.listings.filter(listing => { const part = partFor(listing.family, listing.catalogId); return part && matches(part, listing); }).slice();
    const sort = filters.sort || 'relevance';
    results.sort((a, b) => sort === 'priceAsc' ? a.price - b.price : sort === 'priceDesc' ? b.price - a.price : sort === 'yearDesc' ? b.year - a.year || a.price - b.price : sort === 'yearAsc' ? a.year - b.year || a.price - b.price : sort === 'name' ? partFor(a.family, a.catalogId).name.localeCompare(partFor(b.family, b.catalogId).name, 'ru') : 0);
    const limit = clamp(Number(filters.limit) || 24, 1, 60); results = results.slice(0, limit);
    const ids = new Set(results.map(listing => listing.id)); market.listings = [...results, ...market.listings.filter(listing => !ids.has(listing.id))].slice(0, 60); return results;
  };
  proto.buyPCPartListing = function (id) {
    const market = ensureMarket(this); const listing = market.listings.find(item => item.id === id); if (!listing) { this.notify('Лот уже продан или обновлён', 'warning'); return { ok: false }; }
    const part = partFor(listing.family, listing.catalogId); if (!part) return { ok: false };
    if (this.state.pc.componentInventory.length >= 200) { this.notify('Склад заполнен: 200 деталей', 'warning'); return { ok: false }; }
    if (part.year > this.state.pc.year && !(this.state.pc.cheats && this.state.pc.cheats.unlockAll)) { this.notify(`Деталь доступна с ${part.year} года`, 'warning'); return { ok: false }; }
    if (this.state.pc.money < listing.price) { this.notify(`Нужно ${listing.price} ₽`, 'error'); return { ok: false }; }
    this.state.pc.money -= listing.price;
    const item = { uid: uid('component'), catalogId: part.id, family: listing.family, type: part.type, condition: listing.condition,
      health: listing.health, paid: listing.price, year: part.year, acquiredDay: this.state.pc.day };
    this.state.pc.componentInventory.unshift(item); market.listings = market.listings.filter(other => other.id !== id); market.soldIds.push(id); market.soldIds = market.soldIds.slice(-200);
    this.state.pc.stats.pcPartsBought = (this.state.pc.stats.pcPartsBought || 0) + 1;
    this.pcLog(`PC Авито: куплена ${part.name} за ${listing.price} ₽ (${conditions[listing.condition].name}).`, 'good');
    this.notify(`${part.name} → склад PC-мастерской`, 'success'); this.save(); return { ok: true, item, part, listing };
  };
  proto.sellPCComponent = function (uidValue) {
    const inventory = this.state.pc.componentInventory; const item = inventory.find(entry => entry.uid === uidValue); if (!item) return false;
    const part = partFor(item.family, item.catalogId); const price = Math.max(1, round((item.paid || part.value * .5) * .55));
    this.state.pc.componentInventory = inventory.filter(entry => entry.uid !== uidValue); this.state.pc.money += price;
    this.pcLog(`Склад PC: продана ${part.name} за ${price} ₽.`); this.notify(`Продано за ${price} ₽`, 'success'); this.save(); return price;
  };
  proto.pcInventoryPlan = function (parts, family) {
    const used = new Set(); const items = []; let discount = 0; let healthTotal = 0;
    Object.keys(parts || {}).forEach(type => {
      const part = parts[type]; if (!part) return;
      const item = this.state.pc.componentInventory.find(entry => entry.family === family && entry.catalogId === part.id && !used.has(entry.uid));
      if (!item) return; used.add(item.uid); items.push(item); discount += part.value; healthTotal += item.health;
    });
    return { uids: items.map(item => item.uid), items, discount, healthTotal, family };
  };
  proto.pcConsumeInventory = function (plan) {
    if (!plan || !plan.uids.length) return 0; const used = new Set(plan.uids); const before = this.state.pc.componentInventory.length;
    this.state.pc.componentInventory = this.state.pc.componentInventory.filter(item => !used.has(item.uid)); return before - this.state.pc.componentInventory.length;
  };
  proto.pcUseInventoryItem = function (uidValue) {
    const item = this.state.pc.componentInventory.find(entry => entry.uid === uidValue); if (!item) return null; const part = partFor(item.family, item.catalogId); if (!part) return null;
    const result = item.family === 'laptop' ? this.setLaptopDraftPart(part.type, part.id) : this.setPCDraftPart(part.type, part.id);
    if (!result) return null; this.notify(`${part.name} выбрана в ${item.family === 'laptop' ? 'ноутбучный' : 'настольный'} draft`, 'success'); return { item, part };
  };
  proto.pcPartsMarketLoaded = true;
})();
