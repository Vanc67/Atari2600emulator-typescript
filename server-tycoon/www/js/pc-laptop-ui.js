/* Laptop Architect 8.2 UI: графическая сборка, мобильный каталог и батарея. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI; if (!ui || !ui.game.buildLaptopPC || !DATA.LAPTOP_PART_CATALOG) return;
    const proto = Object.getPrototypeOf(ui); if (proto.pcLaptopUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0); const money = value => `${round(value).toLocaleString('ru-RU')} ₽`;
    const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${Number((value / 1024).toFixed(1))} ГБ` : `${round(value)} МБ`;
    const types = Object.keys(DATA.LAPTOP_PART_TYPES); let searchTimer = null;
    ui.laptopSlot = 'cpu'; ui.laptopPage = 0; ui.laptopQuery = ''; ui.laptopBrand = 'all'; ui.laptopSocket = 'all'; ui.laptopYear = ui.game.state.pc.year || 1990;
    ui.laptopCompatible = false; ui.laptopFavoritesOnly = false; ui.laptopSort = 'priceAsc';

    proto.pcLaptopSvg = function (report) {
      const p = report.parts; const color = type => p[type] ? '#55f6d2' : '#33485c'; const fill = type => p[type] ? '#174b49' : '#111d2a';
      const charge = p.battery ? 76 : 12;
      return `<svg class="laptop-architect-svg" viewBox="0 0 360 255" role="img" aria-label="Графическая сборка ноутбука"><defs><linearGradient id="lapScreen82" x2="0" y2="1"><stop stop-color="#173b68"/><stop offset="1" stop-color="#07111f"/></linearGradient></defs><rect x="66" y="10" width="228" height="139" rx="10" fill="#152536" stroke="${color('display')}" stroke-width="3"/><rect x="77" y="21" width="206" height="115" rx="3" fill="${p.display ? 'url(#lapScreen82)' : '#0c1520'}" stroke="${color('display')}"/><text x="180" y="70" text-anchor="middle" fill="${color('display')}" font-size="12">${p.display ? esc(p.display.resolution) : 'DISPLAY'}</text><text x="180" y="91" text-anchor="middle" fill="#7c96ad" font-size="9">${p.display ? `${p.display.sizeInch}" · ${p.display.refreshHz} Hz` : '10 PART MOBILE ARCHITECT'}</text><circle cx="180" cy="16" r="2" fill="#648299"/><path d="M45 151h270l25 66q3 12-12 12H32q-15 0-12-12l25-66Z" fill="#1b2b3a" stroke="${color('case')}" stroke-width="3"/><path d="M60 160h240M53 177h254M45 197h270" stroke="#42596c" opacity=".65"/><g fill="${fill('cpu')}" stroke="${color('cpu')}"><rect x="75" y="165" width="34" height="28" rx="3"/></g><text x="92" y="183" text-anchor="middle" fill="${color('cpu')}" font-size="8">CPU</text><rect x="120" y="165" width="45" height="28" rx="3" fill="${fill('gpu')}" stroke="${color('gpu')}"/><text x="142" y="183" text-anchor="middle" fill="${color('gpu')}" font-size="8">GPU</text><g stroke="${color('ram')}" fill="${fill('ram')}"><rect x="177" y="164" width="6" height="31"/><rect x="187" y="164" width="6" height="31"/></g><circle cx="244" cy="179" r="23" fill="none" stroke="${color('cooling')}" stroke-width="3"/><path d="M244 156v46M221 179h46M228 163l32 32M260 163l-32 32" stroke="${color('cooling')}" opacity=".5"/><rect x="283" y="163" width="29" height="36" rx="3" fill="${fill('storage')}" stroke="${color('storage')}"/><path d="M139 214h82" stroke="#607488" stroke-width="5" stroke-linecap="round"/><rect x="55" y="207" width="62" height="12" rx="3" fill="#0b151f" stroke="${color('battery')}"/><rect x="59" y="210" width="${round(charge * .5)}" height="6" rx="2" fill="${color('battery')}"/><rect x="253" y="208" width="48" height="10" rx="3" fill="${fill('psu')}" stroke="${color('psu')}"/><text x="180" y="246" text-anchor="middle" fill="#6e879c" font-size="9">LAPTOP ARCHITECT · ${report.missing.length}/10 EMPTY</text></svg>`;
    };
    proto.pcLaptopSlotButton = function (type, report) {
      const definition = DATA.LAPTOP_PART_TYPES[type]; const part = report.parts[type];
      return `<button class="architect-slot laptop-slot ${this.laptopSlot === type ? 'active' : ''} ${part ? 'filled' : ''}" data-action="pc-laptop-slot" data-type="${type}"><span style="${part ? `color:${pcBrandColor(part)}` : ''}">${part ? pcPartBrandSvg(part) : svgIcon(definition.icon)}</span><div><small>${esc(definition.name)}</small><strong>${part ? esc(part.name) : 'Выбрать мобильную деталь'}</strong><small>${part ? `${part.year} · ${esc(part.summary)}` : 'Слот пуст'}</small></div><b>${part ? money(part.value) : '+'}</b></button>`;
    };
    proto.pcLaptopCatalog = function () {
      const type = this.laptopSlot; const year = this.laptopYear; const state = this.game.pcLaptopState();
      const result = this.game.pcLaptopCatalog({ type, year, query: this.laptopQuery, brand: this.laptopBrand, socket: this.laptopSocket, sort: this.laptopSort, compatibleOnly: this.laptopCompatible, favoritesOnly: this.laptopFavoritesOnly, page: this.laptopPage, perPage: 12 }); this.laptopPage = result.page;
      const pool = year === 'all' ? DATA.LAPTOP_PART_CATALOG.filter(part => part.type === type) : DATA.LAPTOP_PART_BY_TYPE_YEAR.get(`${type}:${year}`) || [];
      const brands = [...new Set(pool.map(part => part.brand))].sort(); const sockets = [...new Set(pool.map(part => part.socket || part.interface).filter(Boolean))].sort();
      let html = `<div class="architect-catalog laptop-catalog" id="laptopCatalog"><div class="architect-catalog-head"><div><span>MOBILE CATALOG · ${esc(DATA.LAPTOP_PART_TYPES[type].name)}</span><strong>${result.total.toLocaleString('ru-RU')} вариантов</strong></div><b>${year === 'all' ? '1990—2026' : year}</b></div><form class="architect-search" data-laptop-catalog-form><input type="search" value="${esc(this.laptopQuery)}" data-laptop-query placeholder="Intel, Samsung, MXM, батарея…" aria-label="Поиск деталей ноутбука"><button class="btn btn-primary" type="submit">Найти</button></form><div class="architect-filter-grid"><label><span>Бренд</span><select data-laptop-brand><option value="all">Все бренды</option>${brands.map(brand => `<option value="${esc(brand)}" ${this.laptopBrand === brand ? 'selected' : ''}>${esc(brand)}</option>`).join('')}</select></label>${sockets.length ? `<label><span>Сокет / интерфейс</span><select data-laptop-socket><option value="all">Любой</option>${sockets.map(socket => `<option value="${esc(socket)}" ${this.laptopSocket === socket ? 'selected' : ''}>${esc(socket)}</option>`).join('')}</select></label>` : ''}<label><span>Сортировка</span><select data-laptop-sort>${[['priceAsc','Сначала дешевле'],['priceDesc','Сначала дороже'],['performance','Производительность / ёмкость'],['name','По названию']].map(([id,label]) => `<option value="${id}" ${this.laptopSort === id ? 'selected' : ''}>${label}</option>`).join('')}</select></label><label class="architect-compatible"><input type="checkbox" data-laptop-compatible ${this.laptopCompatible ? 'checked' : ''}><span>Только совместимые</span></label><label class="architect-compatible"><input type="checkbox" data-laptop-favorites ${this.laptopFavoritesOnly ? 'checked' : ''}><span>★ Избранное</span></label></div>`;
      if (!result.items.length) html += this.emptyState('inventory', 'Мобильные детали не найдены', 'Измените запрос, год или фильтр совместимости.');
      else html += `<div class="architect-part-grid">${result.items.map(part => `<article class="architect-part laptop-part ${state.draft[type] === part.id ? 'selected' : ''}"><div class="architect-part-icon pc-branded-part" style="color:${pcBrandColor(part)}">${pcPartBrandSvg(part)}<i>${svgIcon(DATA.LAPTOP_PART_TYPES[type].icon)}</i><span>${part.year}</span></div><div class="architect-part-copy"><strong>${esc(part.name)}</strong><small>${esc(part.brand)} · ${esc(part.summary)}</small><span class="architect-part-price">${money(part.value)}</span></div><div class="architect-part-actions"><div><button class="${state.favorites.includes(part.id) ? 'active' : ''}" data-action="pc-laptop-favorite" data-id="${part.id}" aria-label="Избранное">${state.favorites.includes(part.id) ? '★' : '☆'}</button><button data-action="pc-open-pc-market" data-query="${esc(part.brand)}">Авито</button></div><button class="btn btn-small ${state.draft[type] === part.id ? 'btn-ghost' : 'btn-primary'}" data-action="pc-laptop-select" data-type="${type}" data-id="${part.id}">${state.draft[type] === part.id ? 'Выбрано' : 'Выбрать'}</button></div></article>`).join('')}</div>`;
      if (result.pages > 1) html += `<div class="pagination"><button class="btn btn-small btn-ghost" data-action="pc-laptop-page" data-page="${result.page - 1}" ${result.page === 0 ? 'disabled' : ''}>← Назад</button><span class="badge info">${result.page + 1} / ${result.pages}</span><button class="btn btn-small btn-ghost" data-action="pc-laptop-page" data-page="${result.page + 1}" ${result.page >= result.pages - 1 ? 'disabled' : ''}>Дальше →</button></div>`;
      return html + '</div>';
    };
    proto.pcLaptopBuilderView = function () {
      const pc = this.game.state.pc; const state = this.game.pcLaptopState(); const report = this.game.pcLaptopCompatibility(); const m = report.metrics; const filled = 10 - report.missing.length;
      const plan = this.game.pcInventoryPlan ? this.game.pcInventoryPlan(report.parts, 'laptop') : { uids: [], discount: 0 }; const actualTotal = Math.max(0, m.total - plan.discount);
      let html = this.pcViewHead('LAPTOP ARCHITECT // 4 440 PARTS', 'Собери ноутбук', '10 компонентов, мобильная совместимость, батарея, POST и настоящая графическая ОС.');
      html += `<div class="architect-top-actions"><button class="btn btn-ghost" data-action="pc-laptop-back">← Мастерская</button><button class="btn btn-cyan" data-action="pc-laptop-auto">${svgIcon('wrench')}Автоподбор</button><button class="btn btn-ghost" data-action="pc-open-pc-market">${svgIcon('market')}PC Авито</button></div>`;
      html += `<div class="card architect-setup laptop-setup"><label><span>Название ноутбука</span><input type="text" maxlength="60" value="${esc(state.name)}" data-laptop-name></label><label><span>Год каталога</span><select data-laptop-year><option value="all" ${this.laptopYear === 'all' ? 'selected' : ''}>Все годы · 4 440</option>${DATA.LAPTOP_PART_YEARS.map(year => `<option value="${year}" ${Number(this.laptopYear) === year ? 'selected' : ''}>${year}${year > pc.year ? ' · будущее' : ''}</option>`).join('')}</select></label><div class="architect-progress"><span>Мобильных слотов</span><strong>${filled}/10</strong><div class="meter"><i style="width:${filled * 10}%"></i></div></div></div>`;
      if (Number(this.laptopYear) > pc.year && !(pc.cheats && pc.cheats.unlockAll)) html += `<div class="notice warning">${svgIcon('clock')}<div><strong>Каталог будущего · ${this.laptopYear}</strong>Можно изучать детали, но сборка доступна только после наступления нужной эпохи.</div></div>`;
      html += `<div class="architect-workbench laptop-workbench"><div class="architect-visual">${this.pcLaptopSvg(report)}<div class="architect-budget"><span>Бюджет <b>${money(pc.money)}</b></span><span>Каталог <b>${money(m.total)}</b></span><span>Доплата <b>${money(actualTotal)}</b></span></div></div><div class="architect-slots">${types.map(type => this.pcLaptopSlotButton(type, report)).join('')}</div></div>`;
      html += `<div class="card architect-summary laptop-summary"><h3 class="card-title"><span>${svgIcon('stats')}Мобильный инженерный отчёт</span><span class="badge ${report.ok ? 'good' : 'info'}">${report.ok ? 'READY' : `${report.errors.length} CHECKS`}</span></h3><div class="laptop-metrics"><div><span>CPU+GPU</span><strong>${m.performance}</strong></div><div><span>Адаптер от</span><strong>${m.requiredWatts}W</strong></div><div><span>Температура</span><strong>${m.estimatedTemp}°C</strong></div><div><span>Автономность</span><strong>${m.runtimeMinutes} мин</strong></div><div><span>Вес</span><strong>${m.weightKg} кг</strong></div><div><span>Со склада</span><strong>${plan.uids.length}/10</strong></div></div>${report.errors.length ? `<div class="architect-errors">${report.errors.map(error => `<div>${svgIcon('warning')}<span>${esc(error)}</span></div>`).join('')}</div>` : '<div class="notice success">'+svgIcon('check')+'<div><strong>Все мобильные компоненты совместимы</strong>Можно собирать; купленные на PC Авито экземпляры будут использованы автоматически.</div></div>'}${report.warnings.length ? `<p class="muted tiny">${report.warnings.map(esc).join(' · ')}</p>` : ''}<div class="architect-build-actions"><button class="btn btn-ghost" data-action="pc-laptop-clear">Очистить draft</button><button class="btn btn-primary" data-action="pc-laptop-build" ${report.ok && pc.money >= actualTotal ? '' : 'disabled'}>${svgIcon('wrench')}${report.ok ? `Собрать · доплата ${money(actualTotal)}` : 'Сборка не готова'}</button></div></div>`;
      return html + this.pcLaptopCatalog();
    };

    const baseLaptopWorkshop = proto.pcWorkshopView;
    proto.pcWorkshopView = function () {
      const html = baseLaptopWorkshop.call(this); const count = this.game.state.pc.stats.laptopBuilds || 0;
      const entry = `<div class="card architect-entry laptop-entry"><span>${svgIcon('os')}</span><div><small>LAPTOP ARCHITECT · 4 440 MOBILE PARTS</small><strong>Свой ноутбук</strong><p>10 слотов · дисплей и батарея · ${count} собрано</p></div><button class="btn btn-cyan" data-action="pc-open-laptop-builder">Собрать ноутбук</button></div>`;
      return html.includes('pc-parts-shelf') ? html.replace('<div class="card pc-parts-shelf">', entry + '<div class="card pc-parts-shelf">') : html + entry;
    };
    const baseLaptopMachineCard = proto.pcMachineCard;
    proto.pcMachineCard = function (machine) {
      let html = baseLaptopMachineCard.call(this, machine); if (!machine.laptop) return html; html = html.replace('CUSTOM BUILD · 8 PARTS', 'LAPTOP · 10 PARTS');
      const battery = machine.battery; if (battery) html = html.replace('<div class="pc-machine-meters">', `<div class="laptop-card-battery"><span>${battery.adapterConnected ? '⚡ СЕТЬ' : 'БАТАРЕЯ'}</span><div class="meter"><i style="width:${round(battery.charge)}%"></i></div><b>${round(battery.charge)}%</b></div><div class="pc-machine-meters">`);
      return html;
    };
    const baseLaptopDetail = proto.renderPCDetail;
    proto.renderPCDetail = function () {
      baseLaptopDetail.call(this); const machine = this.game.getPC(this.modal && this.modal.pcId); if (!machine || !machine.laptop || !machine.battery) return;
      const body = $('.modal-body', this.modalRoot); if (!body) return; const battery = machine.battery; const remaining = round(battery.runtimeMinutes * battery.charge / 100);
      body.insertAdjacentHTML('beforeend', `<div class="card laptop-battery-panel"><div class="laptop-battery-head"><span>${svgIcon('power')}</span><div><small>MOBILE POWER · ${battery.adapterConnected ? 'ADAPTER CONNECTED' : 'BATTERY DISCHARGE'}</small><strong data-laptop-charge>${round(battery.charge)}%</strong></div><b>${battery.health}% health</b></div><div class="meter laptop-battery-meter"><i data-laptop-charge-meter style="width:${round(battery.charge)}%"></i></div><div class="laptop-battery-stats"><span>Ёмкость <b>${battery.capacityWh} Wh</b></span><span>Осталось ≈ <b data-laptop-remaining>${remaining} мин</b></span><span>Отключений <b>${battery.shutdowns}</b></span></div><button class="btn ${battery.adapterConnected ? 'btn-amber' : 'btn-primary'} btn-wide" data-action="pc-laptop-adapter" data-id="${machine.id}">${svgIcon('power')}${battery.adapterConnected ? 'Отключить адаптер · перейти на батарею' : 'Подключить адаптер и заряжать'}</button>${!battery.adapterConnected && battery.charge <= 0 ? '<div class="notice error">Батарея пуста. Без адаптера ноутбук не включится.</div>' : ''}</div>`);
    };
    const baseLaptopRenderView = proto.renderView;
    proto.renderView = function () { if (this.game.state.campaignMode === 'pc' && this.pcTab === 'pc-laptop-builder') { this.view.innerHTML = this.pcLaptopBuilderView(); return; } return baseLaptopRenderView.call(this); };
    const baseLaptopNav = proto.renderNav;
    proto.renderNav = function () { const result = baseLaptopNav.call(this); if (this.pcTab === 'pc-laptop-builder') { const button = $('[data-tab="pc-workshop"]'); if (button) button.classList.add('active'); } return result; };

    const baseLaptopAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      if (action.startsWith('pc-laptop-') && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'pc-open-laptop-builder': this.pcTab = 'pc-laptop-builder'; this.closeModal(); this.render(); return;
        case 'pc-laptop-back': this.pcTab = 'pc-workshop'; this.render(); return;
        case 'pc-laptop-slot': this.laptopSlot = element.dataset.type; this.laptopPage = 0; this.laptopQuery = ''; this.laptopBrand = 'all'; this.laptopSocket = 'all'; this.renderView(); return;
        case 'pc-laptop-select': this.game.setLaptopDraftPart(element.dataset.type, id); this.renderView(); return;
        case 'pc-laptop-auto': { const year = this.laptopYear === 'all' ? this.game.state.pc.year : this.laptopYear; const report = this.game.pcAutoLaptopDraft(year); if (!report) this.toast('Не удалось подобрать ноутбук', 'warning'); this.renderView(); return; }
        case 'pc-laptop-clear': this.game.clearLaptopDraft(); this.renderView(); return;
        case 'pc-laptop-build': { const result = this.game.buildLaptopPC({ useInventory: true }); if (result.ok) { this.pcTab = 'pc-workshop'; this.refresh(false); this.openPC(result.machine.id); } else this.renderView(); return; }
        case 'pc-laptop-page': this.laptopPage = Number(element.dataset.page); this.renderView(); return;
        case 'pc-laptop-favorite': this.game.pcToggleLaptopFavorite(id); this.renderView(); return;
        case 'pc-laptop-adapter': this.game.toggleLaptopAdapter(id); this.refresh(); return;
        default: return baseLaptopAction.call(this, action, element, event);
      }
    };
    document.addEventListener('submit', event => { if (!event.target.matches('[data-laptop-catalog-form]')) return; event.preventDefault(); clearTimeout(searchTimer); ui.laptopQuery = $('[data-laptop-query]', event.target).value.trim(); ui.laptopPage = 0; ui.renderView(); });
    document.addEventListener('input', event => {
      if (event.target.matches('[data-laptop-name]')) ui.game.pcLaptopState().name = event.target.value;
      if (!event.target.matches('[data-laptop-query]')) return; ui.laptopQuery = event.target.value; clearTimeout(searchTimer);
      searchTimer = setTimeout(() => { if (ui.pcTab === 'pc-laptop-builder') { ui.laptopPage = 0; ui.renderView(); const input = $('[data-laptop-query]'); if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); } } }, 280);
    });
    document.addEventListener('change', event => {
      if (event.target.matches('[data-laptop-year]')) { ui.laptopYear = event.target.value === 'all' ? 'all' : Number(event.target.value); if (ui.laptopYear !== 'all') ui.game.pcLaptopState().selectedYear = ui.laptopYear; ui.laptopPage = 0; ui.renderView(); }
      if (event.target.matches('[data-laptop-brand]')) { ui.laptopBrand = event.target.value; ui.laptopPage = 0; ui.renderView(); }
      if (event.target.matches('[data-laptop-socket]')) { ui.laptopSocket = event.target.value; ui.laptopPage = 0; ui.renderView(); }
      if (event.target.matches('[data-laptop-sort]')) { ui.laptopSort = event.target.value; ui.laptopPage = 0; ui.renderView(); }
      if (event.target.matches('[data-laptop-compatible]')) { ui.laptopCompatible = event.target.checked; ui.laptopPage = 0; ui.renderView(); }
      if (event.target.matches('[data-laptop-favorites]')) { ui.laptopFavoritesOnly = event.target.checked; ui.laptopPage = 0; ui.renderView(); }
    });
    const baseLaptopTickUI = proto.onTick;
    proto.onTick = function () {
      const result = baseLaptopTickUI.call(this); const node = $('[data-laptop-charge]', this.modalRoot);
      if (node && this.modal) { const machine = this.game.getPC(this.modal.pcId); if (machine && machine.laptop && machine.battery) { node.textContent = `${round(machine.battery.charge)}%`; const meter = $('[data-laptop-charge-meter]', this.modalRoot); if (meter) meter.style.width = `${machine.battery.charge}%`; const remaining = $('[data-laptop-remaining]', this.modalRoot); if (remaining) remaining.textContent = `${round(machine.battery.runtimeMinutes * machine.battery.charge / 100)} мин`; } }
      return result;
    };
    proto.pcLaptopUILoaded = true;
  });
})();
