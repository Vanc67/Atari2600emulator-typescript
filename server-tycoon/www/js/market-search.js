/* Ultimate 7.0: мгновенный локальный поиск и целевая подборка лотов Авито. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || Game.prototype.marketSearchLoaded) return;
  const proto = Game.prototype;
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const normalize = value => String(value == null ? '' : value).toLowerCase().replace(/ё/g, 'е').replace(/[^a-zа-я0-9]+/gi, '');
  const round = value => Math.round(value);

  function partText(part) {
    return normalize([part.id, part.name, part.short, part.type, part.ramType, part.socket, part.brand].join(' '));
  }
  function templateText(template) {
    const cpu = DATA.part(template.cpu);
    const ram = template.ram.map(id => DATA.part(id));
    return normalize([template.id, template.name, template.year, cpu && cpu.name, ...ram.map(part => part.ramType)].join(' '));
  }
  proto.listingMatches = function (listing, query) {
    const needle = normalize(query);
    if (!needle) return true;
    if (listing.isServer) return templateText(DATA.template(listing.templateId)).includes(needle) || normalize(`${listing.title} ${listing.description}`).includes(needle);
    const part = DATA.part(listing.partId);
    return Boolean(part && partText(part).includes(needle)) || normalize(`${listing.title} ${listing.description}`).includes(needle);
  };

  proto.findListings = function (query) {
    const needle = normalize(query);
    if (!needle) return [];
    const ramSearch = needle === 'sdram' || needle.startsWith('ddr');
    const existing = this.state.listings.filter(listing => this.listingMatches(listing, query)).filter(listing => {
      if (!ramSearch) return true;
      const part = !listing.isServer && DATA.part(listing.partId);
      return part && part.type === 'ram';
    }).sort((a, b) => Number(a.isServer) - Number(b.isServer));
    // Для запросов памяти не подмешиваем корпуса с тем же ramType.
    const results = existing.slice(0, 3);
    const used = new Set(results.map(listing => listing.isServer ? `s:${listing.templateId}` : `p:${listing.partId}`));
    const parts = this.availableParts().filter(part => (!ramSearch || part.type === 'ram') && partText(part).includes(needle)).map(part => {
      const exact = normalize(part.ramType) === needle || normalize(part.id) === needle;
      return { part, score: (exact ? 10000 : 0) + (part.year || 0) };
    }).sort((a, b) => b.score - a.score || a.part.name.localeCompare(b.part.name));
    const templates = this.availableTemplates().filter(template => templateText(template).includes(needle)).map(template => ({ template, score: template.year || 0 }))
      .sort((a, b) => b.score - a.score || a.template.name.localeCompare(b.template.name));
    const candidates = [...parts.map(item => ({ isServer: false, value: item.part, score: item.score })), ...templates.map(item => ({ isServer: true, value: item.template, score: item.score }))]
      .sort((a, b) => b.score - a.score);
    for (const candidate of candidates) {
      if (results.length >= 8) break;
      const source = candidate.value;
      const key = `${candidate.isServer ? 's' : 'p'}:${source.id}`;
      if (used.has(key)) continue;
      used.add(key);
      if (candidate.isServer) {
        const condition = 'working';
        results.push({ id: uid('lot'), isServer: true, templateId: source.id, condition, title: source.name,
          description: `Найдено по запросу «${query}» · ${DATA.CONDITIONS[condition].description}`,
          seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
          price: Math.max(1500, round(source.basePrice * (.82 + Math.random() * .3) / 50) * 50), originalPrice: null,
          bargained: false, icon: DATA.part(source.chassis).icon, year: source.year, searchFind: true });
      } else {
        const suffix = DATA.PART_LOT_SUFFIX[Math.floor(Math.random() * DATA.PART_LOT_SUFFIX.length)];
        results.push({ id: uid('lot'), isServer: false, partId: source.id, condition: 'working', title: source.name,
          description: `${source.short} · ${suffix} · поиск «${query}»`, seller: DATA.SELLERS[Math.floor(Math.random() * DATA.SELLERS.length)],
          price: Math.max(150, round(source.value * (.78 + Math.random() * .35) / 10) * 10), originalPrice: null,
          bargained: false, icon: source.icon, year: source.year, searchFind: true });
      }
    }
    const resultIds = new Set(results.map(listing => listing.id));
    this.state.listings = [...results, ...this.state.listings.filter(listing => !resultIds.has(listing.id))].slice(0, 60);
    this.log(`Авито: мгновенный поиск «${query}» · ${results.length} предложений.`, results.length ? 'good' : 'info');
    this.notify(results.length ? `Поиск: ${results.length} предложений без ожидания` : `По запросу «${query}» ничего не найдено`, results.length ? 'success' : 'warning');
    this.save();
    return results;
  };
  proto.marketSearchLoaded = true;
})();
