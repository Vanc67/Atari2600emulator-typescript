/* PC Parts Avito 8.2 UI: поиск, фильтры, склад и сборка из купленных деталей. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI; if (!ui || !ui.game.pcFindPartsListings) return;
    const proto = Object.getPrototypeOf(ui); if (proto.pcPartsMarketUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0); const money = value => `${round(value).toLocaleString('ru-RU')} ₽`;
    const conditionNames = { new: 'Новый', excellent: 'Отличное', good: 'Хорошее', used: 'Б/у' };
    const defaults = () => ({ family: 'all', type: 'all', brand: 'all', socket: 'all', year: '', minPrice: '', maxPrice: '', sort: 'relevance' });
    ui.pcMarketQuery = ''; ui.pcMarketFilters = defaults(); ui.pcInventoryFamily = 'all'; let searchTimer = null;
    const partFor = (family, id) => family === 'laptop' ? DATA.LAPTOP_PART_INDEX.get(id) : DATA.PC_PART_INDEX.get(id);
    const marketTypeNames = { cpu: 'Процессоры', motherboard: 'Материнские платы', ram: 'Память', gpu: 'GPU', storage: 'Накопители', psu: 'PSU / адаптеры', case: 'Корпуса / шасси', cooling: 'Охлаждение', display: 'Дисплеи', battery: 'Батареи' };
    const typeName = type => marketTypeNames[type] || (DATA.LAPTOP_PART_TYPES[type] || DATA.PC_PART_TYPES[type] || { name: type }).name;

    proto.pcMarketPartCard = function (listing) {
      const part = partFor(listing.family, listing.catalogId); const affordable = this.game.state.pc.money >= listing.price;
      return `<article class="pc-market-card"><div class="pc-market-art" style="color:${pcBrandColor(part)}">${pcPartBrandSvg(part)}<span>${listing.family === 'laptop' ? 'LAPTOP' : 'DESKTOP'}</span></div><div class="pc-market-copy"><div class="pc-market-tags"><span>ГОД ${part.year}</span><span>${esc(typeName(part.type))}</span><span>${esc(conditionNames[listing.condition])} · ${listing.health}%</span></div><strong>${esc(part.name)}</strong><small>${esc(part.brand)} · ${esc(part.summary)}</small><p>${esc(listing.seller)} · offline лот</p><div class="pc-market-buy"><b>${money(listing.price)}</b><button class="btn btn-small ${affordable ? 'btn-primary' : 'btn-ghost'}" data-action="pc-market-buy" data-id="${listing.id}" ${affordable ? '' : 'disabled'}>${affordable ? 'Купить на склад' : 'Не хватает денег'}</button></div></div></article>`;
    };
    proto.pcPartsMarketView = function () {
      const pc = this.game.state.pc; const results = this.game.pcFindPartsListings(this.pcMarketQuery, this.pcMarketFilters); const f = this.pcMarketFilters;
      const available = [...DATA.PC_PART_CATALOG, ...DATA.LAPTOP_PART_CATALOG].filter(part => part.year <= pc.year);
      const brands = [...new Set(available.map(part => part.brand))].sort(); const sockets = [...new Set(available.map(part => part.socket || part.interface).filter(Boolean))].sort();
      const types = [...new Set(available.map(part => part.type))].sort();
      let html = this.pcViewHead('PC AVITO // DESKTOP + LAPTOP', 'Детали ПК с Авито', '24 440 компонентов · покупка на склад · использование в настольных и ноутбучных сборках.');
      html += `<div class="architect-top-actions pc-market-actions"><button class="btn btn-ghost" data-action="pc-market-back">← Мастерская</button><button class="btn btn-cyan" data-action="pc-market-refresh">${svgIcon('refresh')}Новые лоты</button><button class="btn btn-ghost" data-action="pc-open-component-inventory">${svgIcon('inventory')}Склад · ${pc.componentInventory.length}</button></div>`;
      html += `<form class="architect-search pc-market-search" data-pc-market-form><input type="search" value="${esc(this.pcMarketQuery)}" data-pc-market-query placeholder="Название: Intel, HP, Samsung, MXM, SKU…" aria-label="Поиск PC-деталей на Авито"><button class="btn btn-primary" type="submit">Найти</button></form>`;
      html += `<div class="card pc-market-filters"><div class="pc-market-filter-grid"><label><span>Формат</span><select data-pc-market-field="family"><option value="all">Desktop + Laptop</option><option value="desktop" ${f.family === 'desktop' ? 'selected' : ''}>Настольные</option><option value="laptop" ${f.family === 'laptop' ? 'selected' : ''}>Ноутбучные</option></select></label><label><span>Категория</span><select data-pc-market-field="type"><option value="all">Все категории</option>${types.map(type => `<option value="${type}" ${f.type === type ? 'selected' : ''}>${esc(typeName(type))}</option>`).join('')}</select></label><label><span>Бренд</span><select data-pc-market-field="brand"><option value="all">Все бренды</option>${brands.map(brand => `<option value="${esc(brand)}" ${f.brand === brand ? 'selected' : ''}>${esc(brand)}</option>`).join('')}</select></label><label><span>Сокет / интерфейс</span><select data-pc-market-field="socket"><option value="all">Любой</option>${sockets.map(socket => `<option value="${esc(socket)}" ${f.socket === socket ? 'selected' : ''}>${esc(socket)}</option>`).join('')}</select></label><label><span>Точный год</span><select data-pc-market-field="year"><option value="">Все годы</option>${DATA.PC_PART_YEARS.map(year => `<option value="${year}" ${Number(f.year) === year ? 'selected' : ''}>${year}${year > pc.year ? ' · будущее' : ''}</option>`).join('')}</select></label><label><span>Сортировка</span><select data-pc-market-field="sort">${[['relevance','По совпадению'],['priceAsc','Сначала дешевле'],['priceDesc','Сначала дороже'],['yearDesc','Сначала новые'],['yearAsc','Сначала ретро'],['name','По названию']].map(([id,label]) => `<option value="${id}" ${f.sort === id ? 'selected' : ''}>${label}</option>`).join('')}</select></label><label><span>Цена от, ₽</span><input type="number" min="0" inputmode="numeric" value="${esc(f.minPrice)}" data-pc-market-field="minPrice"></label><label><span>Цена до, ₽</span><input type="number" min="0" inputmode="numeric" value="${esc(f.maxPrice)}" data-pc-market-field="maxPrice"></label></div><button class="text-button" data-action="pc-market-reset">Сбросить поиск и фильтры</button></div>`;
      html += `<div class="section-label"><span>Найдено: ${results.length}</span><span>Бюджет ${money(pc.money)}</span></div>`;
      html += results.length ? `<div class="pc-market-grid">${results.map(listing => this.pcMarketPartCard(listing)).join('')}</div>` : this.emptyState('market', 'Детали не найдены', 'Измените запрос, цену, сокет, год или формат.', 'Сбросить фильтры', 'pc-market-reset');
      return html;
    };
    proto.pcComponentInventoryView = function () {
      const pc = this.game.state.pc; const inventory = pc.componentInventory.filter(item => this.pcInventoryFamily === 'all' || item.family === this.pcInventoryFamily);
      let html = this.pcViewHead('PC COMPONENT STORAGE', 'Склад PC-деталей', `${pc.componentInventory.length}/200 экземпляров · купленные детали не списываются до успешной сборки.`);
      html += `<div class="architect-top-actions"><button class="btn btn-ghost" data-action="pc-market-back">← Мастерская</button><button class="btn btn-cyan" data-action="pc-open-pc-market">${svgIcon('market')}PC Авито</button><button class="btn btn-ghost" data-action="pc-open-builder">${svgIcon('wrench')}PC Architect</button></div>`;
      html += `<div class="filter-row pc-inventory-tabs">${[['all','Все'],['desktop','Настольные'],['laptop','Ноутбучные']].map(([id,label]) => `<button class="filter-chip ${this.pcInventoryFamily === id ? 'active' : ''}" data-action="pc-inventory-filter" data-family="${id}">${label}</button>`).join('')}</div>`;
      if (!inventory.length) html += this.emptyState('inventory', 'Склад пуст', 'Купите деталь на PC Авито или измените фильтр.', 'Открыть PC Авито', 'pc-open-pc-market');
      else html += `<div class="pc-inventory-grid">${inventory.map(item => { const part = partFor(item.family, item.catalogId); return `<article class="pc-market-card pc-inventory-card"><div class="pc-market-art" style="color:${pcBrandColor(part)}">${pcPartBrandSvg(part)}<span>${item.family === 'laptop' ? 'LAPTOP' : 'DESKTOP'}</span></div><div class="pc-market-copy"><div class="pc-market-tags"><span>ГОД ${part.year}</span><span>${esc(conditionNames[item.condition])} · ${item.health}%</span></div><strong>${esc(part.name)}</strong><small>${esc(part.summary)}</small><p>Куплено за ${money(item.paid)} · день ${item.acquiredDay}</p><div class="pc-inventory-actions"><button class="btn btn-small btn-cyan" data-action="pc-inventory-use" data-id="${item.uid}">В draft</button><button class="btn btn-small btn-ghost" data-action="pc-inventory-sell" data-id="${item.uid}">Продать</button></div></div></article>`; }).join('')}</div>`;
      return html;
    };

    const baseMarketWorkshopUI = proto.pcWorkshopView;
    proto.pcWorkshopView = function () {
      const html = baseMarketWorkshopUI.call(this); const pc = this.game.state.pc;
      const entry = `<div class="card architect-entry pc-market-entry"><span>${svgIcon('market')}</span><div><small>PC AVITO + COMPONENT STORAGE</small><strong>Детали ПК и ноутбуков</strong><p>Прямой поиск · бренды · годы · б/у · склад ${pc.componentInventory.length}/200</p></div><div class="pc-market-entry-actions"><button class="btn btn-primary" data-action="pc-open-pc-market">Открыть PC Авито</button><button class="btn btn-ghost" data-action="pc-open-component-inventory">Склад</button></div></div>`;
      return html.includes('pc-parts-shelf') ? html.replace('<div class="card pc-parts-shelf">', entry + '<div class="card pc-parts-shelf">') : html + entry;
    };
    const baseDesktopBuilderMarket = proto.pcBuilderView;
    proto.pcBuilderView = function () {
      let html = baseDesktopBuilderMarket.call(this); const report = this.game.pcDraftCompatibility(); const plan = this.game.pcInventoryPlan(report.parts, 'desktop'); const actual = Math.max(0, report.metrics.total - plan.discount);
      const button = `<button class="btn btn-cyan" data-action="pc-builder-build-owned" ${report.ok && this.game.state.pc.money >= actual ? '' : 'disabled'}>${svgIcon('inventory')}Склад ${plan.uids.length}/8 · доплата ${money(actual)}</button>`;
      return html.replace('<div class="architect-build-actions">', `<div class="architect-build-actions has-owned">${button}`);
    };
    const baseMarketRenderView = proto.renderView;
    proto.renderView = function () {
      if (this.game.state.campaignMode === 'pc' && this.pcTab === 'pc-pc-market') { this.view.innerHTML = this.pcPartsMarketView(); return; }
      if (this.game.state.campaignMode === 'pc' && this.pcTab === 'pc-component-inventory') { this.view.innerHTML = this.pcComponentInventoryView(); return; }
      return baseMarketRenderView.call(this);
    };
    const baseMarketNav = proto.renderNav;
    proto.renderNav = function () { const result = baseMarketNav.call(this); if (['pc-pc-market','pc-component-inventory'].includes(this.pcTab)) { const button = $('[data-tab="pc-workshop"]'); if (button) button.classList.add('active'); } return result; };

    const baseMarketAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      if ((action.startsWith('pc-market-') || action.startsWith('pc-inventory-')) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'pc-open-pc-market': this.pcMarketQuery = element.dataset.query || ''; this.pcTab = 'pc-pc-market'; this.closeModal(); this.render(); return;
        case 'pc-open-component-inventory': this.pcTab = 'pc-component-inventory'; this.closeModal(); this.render(); return;
        case 'pc-market-back': this.pcTab = 'pc-workshop'; this.render(); return;
        case 'pc-market-refresh': this.pcMarketQuery = ''; this.pcMarketFilters = defaults(); this.game.refreshPCPartsMarket(); this.renderView(); return;
        case 'pc-market-reset': this.pcMarketQuery = ''; this.pcMarketFilters = defaults(); this.renderView(); return;
        case 'pc-market-buy': this.game.buyPCPartListing(id); this.refresh(false); return;
        case 'pc-inventory-filter': this.pcInventoryFamily = element.dataset.family; this.renderView(); return;
        case 'pc-inventory-use': { const result = this.game.pcUseInventoryItem(id); if (result) { this.pcTab = result.item.family === 'laptop' ? 'pc-laptop-builder' : 'pc-builder'; this.render(); } return; }
        case 'pc-inventory-sell': this.game.sellPCComponent(id); this.refresh(false); return;
        case 'pc-builder-build-owned': { const result = this.game.buildCustomPC({ useInventory: true }); if (result.ok) { this.pcTab = 'pc-workshop'; this.refresh(false); this.openPC(result.machine.id); } else this.renderView(); return; }
        default: return baseMarketAction.call(this, action, element, event);
      }
    };
    const runSearch = () => { if (ui.pcTab === 'pc-pc-market') { ui.renderView(); const input = $('[data-pc-market-query]'); if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); } } };
    document.addEventListener('submit', event => { if (!event.target.matches('[data-pc-market-form]')) return; event.preventDefault(); clearTimeout(searchTimer); ui.pcMarketQuery = $('[data-pc-market-query]', event.target).value.trim(); ui.renderView(); });
    document.addEventListener('input', event => {
      if (event.target.matches('[data-pc-market-query]')) { ui.pcMarketQuery = event.target.value; clearTimeout(searchTimer); searchTimer = setTimeout(runSearch, 300); }
      if (event.target.matches('input[data-pc-market-field]')) { ui.pcMarketFilters[event.target.dataset.pcMarketField] = event.target.value; clearTimeout(searchTimer); searchTimer = setTimeout(runSearch, 300); }
    });
    document.addEventListener('change', event => {
      if (!event.target.matches('select[data-pc-market-field]')) return; ui.pcMarketFilters[event.target.dataset.pcMarketField] = event.target.value; ui.renderView();
    });
    proto.pcPartsMarketUILoaded = true;
  });
})();
