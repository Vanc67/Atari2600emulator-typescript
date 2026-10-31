/* Ultimate 7.1: прямой поиск и расширенная фильтрация локальной базы Avito. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || Game.prototype.marketSearchLoaded) return;
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').replace(/[^a-z0-9а-я]+/gi, '');
  const words = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').trim().split(/\s+/).map(normalize).filter(Boolean);
  const matchesText = (text, query) => {
    if (!String(query || '').trim()) return true;
    const haystack = normalize(text); const needle = normalize(query);
    return haystack.includes(needle) || words(query).every(word => haystack.includes(word));
  };
  const numeric = value => {
    if (value === '' || value == null) return null;
    const result = Number(String(value).replace(/\s/g, '').replace(',', '.'));
    return Number.isFinite(result) ? result : null;
  };
  const unique = values => [...new Set(values.filter(Boolean))];
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const round = value => Math.round(value);

  Game.prototype.createFoundListing = function (source, isServer, query) {
    if (isServer) return { id: uid('lot'), isServer: true, templateId: source.id, condition: 'working', title: source.name,
      description: `Найдено по запросу «${query || 'фильтры'}» · ${DATA.CONDITIONS.working.description}`,
      seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
      price: Math.max(1500, round(source.basePrice * (.82 + Math.random() * .3) / 50) * 50), originalPrice: null,
      bargained: false, icon: DATA.part(source.chassis).icon, year: source.year, searchFind: true };
    const suffix = DATA.PART_LOT_SUFFIX[Math.floor(Math.random() * DATA.PART_LOT_SUFFIX.length)];
    return { id: uid('lot'), isServer: false, partId: source.id, condition: 'working', title: source.name,
      description: `${source.short} · ${suffix} · поиск «${query || 'фильтры'}»`, seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
      price: Math.max(150, round(source.value * (.78 + Math.random() * .35) / 10) * 10), originalPrice: null,
      bargained: false, icon: source.icon, year: source.year, searchFind: true };
  };

  Game.prototype.normalizeMarketFilters = function (filters = {}) {
    return {
      kind: ['all', 'servers', 'parts'].includes(filters.kind) ? filters.kind : 'all',
      minPrice: numeric(filters.minPrice), maxPrice: numeric(filters.maxPrice),
      socket: filters.socket && filters.socket !== 'all' ? String(filters.socket) : '',
      ramType: filters.ramType && filters.ramType !== 'all' ? String(filters.ramType) : '',
      partType: filters.partType && filters.partType !== 'all' ? String(filters.partType) : '',
      brand: filters.brand && filters.brand !== 'all' ? String(filters.brand) : '',
      condition: filters.condition && filters.condition !== 'all' ? String(filters.condition) : '',
      yearFrom: numeric(filters.yearFrom), yearTo: numeric(filters.yearTo),
      minRam: numeric(filters.minRam), minStorage: numeric(filters.minStorage),
      affordable: Boolean(filters.affordable), sort: filters.sort || 'relevance',
      limit: Math.max(1, Math.min(40, Number(filters.limit) || 8))
    };
  };
  Game.prototype.hasMarketFilters = function (filters = {}) {
    const f = this.normalizeMarketFilters(filters);
    return f.kind !== 'all' || f.minPrice != null || f.maxPrice != null || Boolean(f.socket || f.ramType || f.partType || f.brand || f.condition) ||
      f.yearFrom != null || f.yearTo != null || f.minRam != null || f.minStorage != null || f.affordable;
  };
  Game.prototype.partSearchDetails = function (part, listing = null) {
    const ram = part.type === 'ram' ? Number(part.capacity) || 0 : 0;
    const storage = ['hdd', 'ssd'].includes(part.type) ? Number(part.capacity) || 0 : 0;
    return {
      isServer: false, title: part.name, partType: part.type,
      sockets: part.socket ? [part.socket] : [], ramTypes: part.ramType ? [part.ramType] : [],
      brand: part.brand || '', year: listing && listing.year || part.year || null,
      condition: listing && listing.condition || 'working', price: listing ? listing.price : Number(part.value || part.basePrice) || 0,
      ramTotal: ram, storageTotal: storage, cores: Number(part.cores) || 0, hasGpu: part.type === 'gpu',
      text: `${part.name} ${part.brand || ''} ${part.type} ${part.short || ''} ${part.socket || ''} ${part.ramType || ''} ${part.capacity || ''} ${part.year || ''} ${listing ? listing.title : ''}`
    };
  };
  Game.prototype.templateSearchDetails = function (template, listing = null) {
    const chassis = DATA.part(template.chassis); const cpu = DATA.part(template.cpu);
    const ramParts = (template.ram || []).map(id => DATA.part(id)).filter(Boolean);
    const diskParts = (template.disks || []).map(id => DATA.part(id)).filter(Boolean);
    const condition = listing && listing.condition || 'working';
    const ramTotal = condition === 'noRam' ? 0 : ramParts.reduce((sum, part) => sum + (part.capacity || 0), 0);
    const storageTotal = condition === 'noDisks' ? 0 : diskParts.reduce((sum, part) => sum + (part.capacity || 0), 0);
    return {
      isServer: true, title: template.name, partType: 'server',
      sockets: unique([chassis && chassis.socket, cpu && cpu.socket]),
      ramTypes: unique([chassis && chassis.ramType, ...ramParts.map(part => part.ramType)]),
      brand: template.brand || chassis && chassis.brand || cpu && cpu.brand || '',
      year: listing && listing.year || template.year || chassis && chassis.year || null,
      condition, price: listing ? listing.price : Number(template.basePrice) || 0,
      ramTotal, storageTotal, cores: cpu && cpu.cores || 0, hasGpu: Boolean((template.gpus || []).length),
      text: `${template.name} ${template.brand || ''} ${template.role || ''} ${template.year || ''} ${chassis ? chassis.name : ''} ${cpu ? cpu.name : ''} ${cpu ? cpu.socket || '' : ''} ${chassis ? chassis.socket || '' : ''} ${ramParts.map(part => `${part.name} ${part.ramType || ''}`).join(' ')} ${ramTotal}GB ${storageTotal}GB ${listing ? listing.title : ''}`
    };
  };
  Game.prototype.listingDetails = function (listing) {
    if (listing.isServer) {
      const template = DATA.template(listing.templateId);
      if (template) return this.templateSearchDetails(template, listing);
    } else {
      const part = DATA.part(listing.partId);
      if (part) return this.partSearchDetails(part, listing);
    }
    return { isServer: Boolean(listing.isServer), title: listing.title, partType: listing.isServer ? 'server' : '', sockets: [], ramTypes: [], brand: '', year: listing.year || null,
      condition: listing.condition || 'working', price: listing.price || 0, ramTotal: 0, storageTotal: 0, cores: 0, hasGpu: false, text: `${listing.title} ${listing.seller || ''}` };
  };
  Game.prototype.searchDetailsMatch = function (details, query, filters = {}, ignorePrice = false) {
    const f = this.normalizeMarketFilters(filters);
    if (!matchesText(details.text, query)) return false;
    if (f.kind === 'servers' && !details.isServer || f.kind === 'parts' && details.isServer) return false;
    if (f.partType && details.partType !== f.partType) return false;
    if (f.socket && !details.sockets.some(socket => normalize(socket) === normalize(f.socket))) return false;
    if (f.ramType && !details.ramTypes.some(type => normalize(type) === normalize(f.ramType))) return false;
    if (f.brand && normalize(details.brand) !== normalize(f.brand)) return false;
    if (f.condition && details.condition !== f.condition) return false;
    if (!ignorePrice) {
      if (f.minPrice != null && details.price < f.minPrice) return false;
      if (f.maxPrice != null && details.price > f.maxPrice) return false;
      if (f.affordable && details.price > this.state.money) return false;
    }
    if (f.yearFrom != null && (!details.year || details.year < f.yearFrom)) return false;
    if (f.yearTo != null && (!details.year || details.year > f.yearTo)) return false;
    if (f.minRam != null && details.ramTotal < f.minRam) return false;
    if (f.minStorage != null && details.storageTotal < f.minStorage) return false;
    return true;
  };
  Game.prototype.listingMatches = function (listing, query) {
    return matchesText(this.listingDetails(listing).text, query);
  };
  Game.prototype.listingMatchesFilters = function (listing, filters = {}, query = '') {
    return this.searchDetailsMatch(this.listingDetails(listing), query, filters);
  };
  Game.prototype.sortListings = function (listings, sort = 'relevance') {
    return listings.map((listing, index) => ({ listing, index })).sort((a, b) => {
      const left = a.listing; const right = b.listing;
      if (sort === 'priceAsc') return left.price - right.price || a.index - b.index;
      if (sort === 'priceDesc') return right.price - left.price || a.index - b.index;
      if (sort === 'yearAsc' || sort === 'yearDesc') {
        const ay = this.listingDetails(left).year || 0; const by = this.listingDetails(right).year || 0;
        return (sort === 'yearAsc' ? ay - by : by - ay) || a.index - b.index;
      }
      if (sort === 'name') return left.title.localeCompare(right.title, 'ru') || a.index - b.index;
      return (right._searchScore || 0) - (left._searchScore || 0) || a.index - b.index;
    }).map(item => item.listing);
  };
  Game.prototype.filterListings = function (listings, query = '', filters = {}) {
    return this.sortListings(listings.filter(listing => this.listingMatchesFilters(listing, filters, query)), this.normalizeMarketFilters(filters).sort);
  };
  Game.prototype.marketFilterOptions = function () {
    const cacheKey = `${this.currentYear()}:${DATA.PARTS.length}:${DATA.SERVER_TEMPLATES.length}`;
    if (this._marketFilterOptions && this._marketFilterOptionsKey === cacheKey) return this._marketFilterOptions;
    const sockets = []; const ramTypes = []; const brands = []; const types = [];
    this.availableParts().forEach(part => {
      const details = this.partSearchDetails(part);
      sockets.push(...details.sockets); ramTypes.push(...details.ramTypes); brands.push(details.brand); types.push(details.partType);
    });
    this.availableTemplates().forEach(template => {
      const details = this.templateSearchDetails(template);
      sockets.push(...details.sockets); ramTypes.push(...details.ramTypes); brands.push(details.brand);
    });
    const byName = (a, b) => String(a).localeCompare(String(b), 'ru');
    this._marketFilterOptions = { sockets: unique(sockets).sort(byName), ramTypes: unique(ramTypes).sort(byName), brands: unique(brands).sort(byName), types: unique(types).sort(byName) };
    this._marketFilterOptionsKey = cacheKey;
    return this._marketFilterOptions;
  };
  Game.prototype.findListings = function (query, filters = {}) {
    const f = this.normalizeMarketFilters(filters);
    const needle = String(query || '').trim();
    const advanced = this.hasMarketFilters(f);
    if (!needle && !advanced) return [];
    const limit = f.limit;
    const ramSearch = /(^|[^a-z0-9])(sdram|ddr[1-5]?)([^a-z0-9]|$)/i.test(needle);
    const existing = this.state.listings.filter(listing => this.listingMatchesFilters(listing, f, needle)).filter(listing => {
      if (!ramSearch || advanced) return true;
      const part = !listing.isServer && DATA.part(listing.partId);
      return Boolean(part && part.type === 'ram');
    });
    existing.forEach(listing => {
      const details = this.listingDetails(listing); const title = normalize(details.title);
      listing._searchScore = (title === normalize(needle) ? 20000 : title.startsWith(normalize(needle)) ? 12000 : 0) + 10000 + (details.year || 0);
    });
    const results = this.sortListings(existing, f.sort).slice(0, advanced ? limit : ramSearch ? 3 : 6);
    const used = new Set(results.map(listing => listing.isServer ? `s:${listing.templateId}` : `p:${listing.partId}`));
    const candidates = [];
    const sourceFilters = Object.assign({}, f, { minPrice: null, maxPrice: null, affordable: false });
    if (f.kind !== 'servers') this.availableParts().forEach(part => {
      if (ramSearch && part.type !== 'ram') return;
      const details = this.partSearchDetails(part);
      if (!this.searchDetailsMatch(details, needle, sourceFilters, true)) return;
      const title = normalize(part.name);
      const score = (title === normalize(needle) ? 20000 : title.startsWith(normalize(needle)) ? 12000 : 0) + (part.type === 'ram' && /(^|[^a-z0-9])(sdram|ddr[1-5]?)([^a-z0-9]|$)/i.test(needle) ? 10000 : 0) + (part.year || 0);
      candidates.push({ server: false, source: part, score, estimated: Number(part.value || part.basePrice) || 0 });
    });
    if (f.kind !== 'parts' && !ramSearch) this.availableTemplates().forEach(template => {
      const details = this.templateSearchDetails(template);
      if (!this.searchDetailsMatch(details, needle, sourceFilters, true)) return;
      const title = normalize(template.name);
      const score = (title === normalize(needle) ? 20000 : title.startsWith(normalize(needle)) ? 12000 : 0) + (template.year || 0);
      candidates.push({ server: true, source: template, score, estimated: Number(template.basePrice) || 0 });
    });
    candidates.sort((a, b) => {
      if (f.sort === 'priceAsc') return a.estimated - b.estimated || b.score - a.score;
      if (f.sort === 'priceDesc') return b.estimated - a.estimated || b.score - a.score;
      if (f.sort === 'name') return (a.source.name || '').localeCompare(b.source.name || '', 'ru');
      if (f.sort === 'yearAsc') return (a.source.year || 0) - (b.source.year || 0) || b.score - a.score;
      if (f.sort === 'yearDesc') return (b.source.year || 0) - (a.source.year || 0) || b.score - a.score;
      return b.score - a.score || a.estimated - b.estimated;
    });
    let created = 0;
    for (const candidate of candidates) {
      if (results.length >= limit) break;
      const key = candidate.server ? `s:${candidate.source.id}` : `p:${candidate.source.id}`;
      if (used.has(key)) continue;
      const listing = this.createFoundListing(candidate.source, candidate.server, needle);
      listing._searchScore = candidate.score;
      if (!this.listingMatchesFilters(listing, f, needle)) continue;
      results.push(listing); used.add(key); created += 1;
    }
    const sorted = this.sortListings(results, f.sort).slice(0, limit);
    const ids = new Set(sorted.map(listing => listing.id));
    this.state.listings = [...sorted, ...this.state.listings.filter(listing => !ids.has(listing.id))].slice(0, 60);
    this.log(`Avito: поиск «${needle || 'расширенные фильтры'}» · ${sorted.length} предложений.`, sorted.length ? 'good' : 'info');
    this.notify(sorted.length ? `Avito: найдено ${sorted.length}${created ? ` · новых ${created}` : ''}` : 'Avito: по этим условиям ничего нет — смягчите фильтры', sorted.length ? 'success' : 'warning');
    this.save();
    return sorted;
  };
  Game.prototype.marketSearchLoaded = true;
})();
