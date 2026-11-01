/* PC Architect 8.0 UI: графический конструктор, каталог и POST/OS boot для custom ПК. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.buildCustomPC || !DATA.PC_PART_CATALOG) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.pcArchitectUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0);
    const money = value => `${round(value).toLocaleString('ru-RU')} ₽`;
    const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${Number((value / 1024).toFixed(1))} ГБ` : `${round(value)} МБ`;
    const types = Object.keys(DATA.PC_PART_TYPES);
    ui.pcBuilderSlot = 'cpu'; ui.pcBuilderPage = 0; ui.pcBuilderQuery = ''; ui.pcBuilderBrand = 'all'; ui.pcBuilderSocket = 'all';
    ui.pcBuilderCompatible = false; ui.pcBuilderFavoritesOnly = false; ui.pcBuilderSort = 'priceAsc'; ui.pcBuilderYear = ui.game.state.pc.year || 1990;
    let searchTimer = null;

    proto.pcArchitectSvg = function (report) {
      const p = report.parts; const color = type => p[type] ? '#55f6d2' : '#33485c'; const fill = type => p[type] ? '#174b49' : '#111d2a';
      return `<svg class="architect-case-svg" viewBox="0 0 360 255" role="img" aria-label="Графическая сборка ПК"><defs><linearGradient id="archCase" x2="0" y2="1"><stop stop-color="#26394a"/><stop offset="1" stop-color="#101b28"/></linearGradient><radialGradient id="archFan"><stop stop-color="#56d9ff" stop-opacity=".35"/><stop offset="1" stop-color="#56d9ff" stop-opacity="0"/></radialGradient></defs><rect x="8" y="8" width="344" height="239" rx="16" fill="url(#archCase)" stroke="#496278" stroke-width="2"/><rect x="54" y="27" width="216" height="173" rx="5" fill="#173b36" stroke="#477c68"/><path d="M70 45h180M70 180h180M80 35v155M250 35v155" stroke="#396154" opacity=".55"/><rect x="115" y="59" width="54" height="48" rx="4" fill="${fill('cpu')}" stroke="${color('cpu')}" stroke-width="2"/><text x="142" y="87" text-anchor="middle" fill="${color('cpu')}" font-size="10">CPU</text><circle cx="142" cy="79" r="39" fill="url(#archFan)" stroke="${color('cooling')}"/><path d="M142 47v64M110 79h64M119 56l46 46M165 56l-46 46" stroke="${color('cooling')}" opacity=".55"/><g fill="${fill('ram')}" stroke="${color('ram')}"><rect x="190" y="50" width="8" height="67"/><rect x="204" y="50" width="8" height="67"/><rect x="218" y="50" width="8" height="67"/><rect x="232" y="50" width="8" height="67"/></g><rect x="86" y="137" width="145" height="24" rx="3" fill="${fill('gpu')}" stroke="${color('gpu')}" stroke-width="2"/><text x="158" y="153" text-anchor="middle" fill="${color('gpu')}" font-size="9">GPU</text><rect x="276" y="35" width="51" height="71" rx="3" fill="${fill('storage')}" stroke="${color('storage')}"/><text x="301" y="74" text-anchor="middle" fill="${color('storage')}" font-size="8">DRIVE</text><rect x="73" y="207" width="95" height="29" rx="3" fill="${fill('psu')}" stroke="${color('psu')}"/><text x="120" y="226" text-anchor="middle" fill="${color('psu')}" font-size="9">PSU</text><path d="M24 42v150M336 42v150" stroke="${color('case')}" stroke-width="4"/><circle cx="309" cy="180" r="28" fill="none" stroke="${color('case')}"/><circle cx="309" cy="180" r="18" fill="none" stroke="${color('case')}" stroke-dasharray="4 3"/><text x="190" y="229" text-anchor="middle" fill="${color('motherboard')}" font-size="9">MAINBOARD · ${report.missing.length}/8 EMPTY</text></svg>`;
    };
    proto.pcBuilderSlotButton = function (type, report) {
      const definition = DATA.PC_PART_TYPES[type]; const part = report.parts[type];
      return `<button class="architect-slot ${this.pcBuilderSlot === type ? 'active' : ''} ${part ? 'filled' : ''}" data-action="pc-builder-slot" data-type="${type}"><span>${svgIcon(definition.icon)}</span><div><small>${esc(definition.name)}</small><strong>${part ? esc(part.name) : 'Выбрать деталь'}</strong><small>${part ? `${part.year} · ${esc(part.summary)}` : 'Слот пуст'}</small></div><b>${part ? money(part.value) : '+'}</b></button>`;
    };
    proto.pcBuilderCatalog = function () {
      const type = this.pcBuilderSlot; const year = this.pcBuilderYear;
      const result = this.game.pcCatalog({ type, year, query: this.pcBuilderQuery, brand: this.pcBuilderBrand, socket: this.pcBuilderSocket, sort: this.pcBuilderSort, compatibleOnly: this.pcBuilderCompatible, favoritesOnly: this.pcBuilderFavoritesOnly, page: this.pcBuilderPage, perPage: 12 });
      this.pcBuilderPage = result.page;
      const pool = year === 'all' ? DATA.PC_PART_CATALOG.filter(part => part.type === type) : DATA.PC_PART_BY_TYPE_YEAR.get(`${type}:${year}`) || [];
      const brands = [...new Set(pool.map(part => part.brand).filter(Boolean))].sort();
      const sockets = [...new Set(pool.flatMap(part => part.sockets || [part.socket]).filter(Boolean))].sort();
      const architect = this.game.pcArchitectState(); const draft = architect.draft;
      const favorites = architect.favorites || []; const comparison = architect.comparison || [];
      let html = `<div class="architect-catalog" id="architectCatalog"><div class="architect-catalog-head"><div><span>КАТАЛОГ · ${esc(DATA.PC_PART_TYPES[type].name)}</span><strong>${result.total.toLocaleString('ru-RU')} вариантов</strong></div><b>${year === 'all' ? '1990—2026' : year}</b></div><form class="architect-search" data-pc-catalog-form><input type="search" value="${esc(this.pcBuilderQuery)}" data-pc-builder-query placeholder="Название, бренд, сокет…" aria-label="Поиск PC-деталей"><button class="btn btn-primary" type="submit">Найти</button></form><div class="architect-filter-grid"><label><span>Бренд</span><select data-pc-builder-brand><option value="all">Все бренды</option>${brands.map(brand => `<option value="${esc(brand)}" ${this.pcBuilderBrand === brand ? 'selected' : ''}>${esc(brand)}</option>`).join('')}</select></label>${sockets.length ? `<label><span>Сокет</span><select data-pc-builder-socket><option value="all">Любой</option>${sockets.map(socket => `<option value="${esc(socket)}" ${this.pcBuilderSocket === socket ? 'selected' : ''}>${esc(socket)}</option>`).join('')}</select></label>` : ''}<label><span>Сортировка</span><select data-pc-builder-sort>${[['priceAsc','Сначала дешевле'],['priceDesc','Сначала дороже'],['performance','Производительность'],['name','По названию']].map(([id,label]) => `<option value="${id}" ${this.pcBuilderSort === id ? 'selected' : ''}>${label}</option>`).join('')}</select></label><label class="architect-compatible"><input type="checkbox" data-pc-builder-compatible ${this.pcBuilderCompatible ? 'checked' : ''}><span>Только совместимые</span></label><label class="architect-compatible"><input type="checkbox" data-pc-builder-favorites ${this.pcBuilderFavoritesOnly ? 'checked' : ''}><span>★ Избранное</span></label></div>`;
      if (!result.items.length) html += this.emptyState('inventory', 'Детали не найдены', 'Измените запрос, год или отключите фильтр совместимости.');
      else html += `<div class="architect-part-grid">${result.items.map(part => `<article class="architect-part ${draft[type] === part.id ? 'selected' : ''}"><div class="architect-part-icon">${svgIcon(DATA.PC_PART_TYPES[type].icon)}<span>${part.year}</span></div><div class="architect-part-copy"><strong>${esc(part.name)}</strong><small>${esc(part.brand)} · ${esc(part.summary)}</small><span class="architect-part-price">${money(part.value)}</span></div><div class="architect-part-actions"><div><button class="${favorites.includes(part.id) ? 'active' : ''}" data-action="pc-favorite" data-id="${part.id}" aria-label="Избранное">${favorites.includes(part.id) ? '★' : '☆'}</button><button class="${comparison.includes(part.id) ? 'active' : ''}" data-action="pc-compare" data-id="${part.id}">⇄ Сравнить</button></div><button class="btn btn-small ${draft[type] === part.id ? 'btn-ghost' : 'btn-primary'}" data-action="pc-builder-select" data-type="${type}" data-id="${part.id}">${draft[type] === part.id ? 'Выбрано' : 'Выбрать'}</button></div></article>`).join('')}</div>`;
      if (result.pages > 1) html += `<div class="pagination"><button class="btn btn-small btn-ghost" data-action="pc-builder-page" data-page="${result.page - 1}" ${result.page === 0 ? 'disabled' : ''}>← Назад</button><span class="badge info">${result.page + 1} / ${result.pages}</span><button class="btn btn-small btn-ghost" data-action="pc-builder-page" data-page="${result.page + 1}" ${result.page >= result.pages - 1 ? 'disabled' : ''}>Дальше →</button></div>`;
      return html + '</div>';
    };
    proto.pcBuilderView = function () {
      const pc = this.game.state.pc; const architect = this.game.pcArchitectState(); const report = this.game.pcDraftCompatibility();
      const filled = 8 - report.missing.length; const m = report.metrics;
      let html = this.pcViewHead('PC ARCHITECT // 20 000 PARTS', 'Собери свой ПК', 'Восемь компонентов. Настоящая совместимость. POST, ОС и рабочий стол после сборки.');
      html += `<div class="architect-top-actions"><button class="btn btn-ghost" data-action="pc-builder-back">← Мастерская</button><button class="btn btn-cyan" data-action="pc-builder-auto">${svgIcon('wrench')}Автоподбор</button><button class="btn btn-ghost" data-action="open-cheats">${svgIcon('wrench')}Читы</button></div>`;
      html += `<div class="card architect-setup"><label><span>Название компьютера</span><input type="text" maxlength="60" value="${esc(architect.name)}" data-pc-builder-name></label><label><span>Год каталога</span><select data-pc-builder-year><option value="all" ${this.pcBuilderYear === 'all' ? 'selected' : ''}>Все годы · 20 000</option>${DATA.PC_PART_YEARS.map(year => `<option value="${year}" ${Number(this.pcBuilderYear) === year ? 'selected' : ''}>${year}${year > pc.year ? ' · будущее' : ''}</option>`).join('')}</select></label><div class="architect-progress"><span>Слотов заполнено</span><strong>${filled}/8</strong><div class="meter"><i style="width:${filled / 8 * 100}%"></i></div></div></div>`;
      if (Number(this.pcBuilderYear) > pc.year && !(pc.cheats && pc.cheats.unlockAll)) html += `<div class="notice warning">${svgIcon('clock')}<div><strong>Каталог будущего · ${this.pcBuilderYear}</strong>Смотреть можно, но для сборки в ${pc.year} году нужны детали текущей эпохи. Debug → Читы может установить любой год или открыть все эпохи.</div></div>`;
      html += `<div class="architect-workbench"><div class="architect-visual">${this.pcArchitectSvg(report)}<div class="architect-budget"><span>Бюджет <b>${money(pc.money)}</b></span><span>Сборка <b>${money(m.total)}</b></span></div></div><div class="architect-slots">${types.map(type => this.pcBuilderSlotButton(type, report)).join('')}</div></div>`;
      html += `<div class="card architect-summary"><h3 class="card-title"><span>${svgIcon('stats')}Инженерный отчёт</span><span class="badge ${report.ok ? 'good' : 'info'}">${report.ok ? 'READY' : `${report.errors.length} CHECKS`}</span></h3><div class="architect-metrics"><div><span>CPU+GPU</span><strong>${m.performance}</strong></div><div><span>PSU от</span><strong>${m.requiredWatts}W</strong></div><div><span>Температура</span><strong>${m.estimatedTemp}°C</strong></div><div><span>Шум</span><strong>${round(m.noise)} дБ</strong></div></div>${report.errors.length ? `<div class="architect-errors">${report.errors.map(error => `<div>${svgIcon('warning')}<span>${esc(error)}</span></div>`).join('')}</div>` : '<div class="notice success">'+svgIcon('check')+'<div><strong>Все компоненты совместимы</strong>Можно собирать, затем установить ОС и пройти POST.</div></div>'}${report.warnings.length ? `<p class="muted tiny">${report.warnings.map(esc).join(' · ')}</p>` : ''}<div class="architect-build-actions"><button class="btn btn-ghost" data-action="pc-builder-clear">Очистить draft</button><button class="btn btn-primary" data-action="pc-builder-build" ${report.canBuild ? '' : 'disabled'}>${svgIcon('wrench')}${report.ok ? report.canAfford ? `Собрать за ${money(m.total)}` : `Нужно ${money(m.total)}` : 'Сборка не готова'}</button></div></div>`;
      html += this.pcBuilderCatalog();
      return html;
    };

    const baseArchitectWorkshop = proto.pcWorkshopView;
    proto.pcWorkshopView = function () {
      const html = baseArchitectWorkshop.call(this);
      const entry = `<div class="card architect-entry"><span>${svgIcon('chip')}</span><div><small>НОВЫЙ РЕЖИМ · НИЧЕГО НЕ ЗАМЕНЯЕТ</small><strong>PC Architect</strong><p>20 000 деталей · своя сборка · POST и графическая ОС</p></div><button class="btn btn-primary" data-action="pc-open-builder">Собрать ПК</button></div>`;
      return html.includes('pc-parts-shelf') ? html.replace('<div class="card pc-parts-shelf">', entry + '<div class="card pc-parts-shelf">') : html + entry;
    };
    const baseArchitectMachineCard = proto.pcMachineCard;
    proto.pcMachineCard = function (machine) { const html = baseArchitectMachineCard.call(this, machine); return machine.custom ? html.replace('СОБСТВЕННЫЙ ПК', 'CUSTOM BUILD · 8 PARTS') : html; };
    const baseArchitectDetail = proto.renderPCDetail;
    proto.renderPCDetail = function () {
      baseArchitectDetail.call(this);
      const machine = this.game.getPC(this.modal && this.modal.pcId); if (!machine || !machine.custom) return;
      const body = $('.modal-body', this.modalRoot); if (!body) return;
      const parts = this.game.pcCustomParts(machine);
      body.insertAdjacentHTML('beforeend', `<div class="architect-owned-parts"><div class="section-label"><span>Компоненты PC Architect</span><span>${parts.length}/8</span></div>${parts.map(({ type, part }) => `<div><span>${svgIcon(DATA.PC_PART_TYPES[type].icon)}</span><p><strong>${esc(DATA.PC_PART_TYPES[type].name)}</strong><small>${part.year} · ${esc(part.name)} · ${esc(part.summary)}</small></p></div>`).join('')}</div>`);
    };
    const baseArchitectRenderView = proto.renderView;
    proto.renderView = function () {
      if (this.game.state.campaignMode === 'pc' && this.pcTab === 'pc-builder') { this.view.innerHTML = this.pcBuilderView(); return; }
      return baseArchitectRenderView.call(this);
    };
    const baseArchitectNav = proto.renderNav;
    proto.renderNav = function () { const result = baseArchitectNav.call(this); if (this.pcTab === 'pc-builder') { const button = $('[data-tab="pc-workshop"]'); if (button) button.classList.add('active'); } return result; };

    proto.renderArchitectBoot = function (machine, boot) {
      const os = this.game.pcOS(machine.os); const progress = Math.min(100, boot.progress); const post = progress < 42;
      const lines = [`ARCHITECT UEFI BIOS 8.1`, `CPU: ${machine.cpu}`, `Memory: ${machine.ramMb} MB ${machine.ramType} · OK`, `GPU: ${machine.gpu}`, `Storage: ${machine.storageType} ${mb(machine.storageMb)} · OK`, `USB HID · SATA/NVMe · PCIe initialized`];
      const body = `<div class="pc-os-session"><div class="pc-os-monitor"><div class="pc-monitor-brand">${post ? 'ARCHITECT UEFI POST' : esc(os.name)}<span>CUSTOM BOOT</span></div><div class="architect-boot ${post ? 'post' : `os-${os.style}`}"><div class="architect-boot-logo">${post ? svgIcon('chip') : os.style === 'linux' ? svgIcon('apps') : brandSvg('windows')}</div><strong>${post ? 'PC ARCHITECT' : esc(os.name)}</strong><p>${post ? 'Power-On Self Test · проверка восьми компонентов' : 'Загрузка графического рабочего стола…'}</p>${post ? `<div class="architect-post-lines">${lines.slice(0, Math.max(2, Math.ceil(progress / 8))).map(line => `<span>${esc(line)}</span>`).join('')}</div>` : '<div class="architect-boot-spinner"></div>'}<div class="architect-boot-progress"><i style="width:${progress}%"></i></div><small>${progress}% · ${post ? 'POST' : 'OS KERNEL / GUI'}</small><button data-action="pc-boot-skip">Пропустить загрузку</button></div></div><div class="pc-os-footer"><span>${esc(machine.name)} · CUSTOM</span><button data-action="pc-os-back" data-id="${machine.id}">Вернуться в мастерскую</button></div></div>`;
      this.modalRoot.innerHTML = this.modalShell(machine.name, `${os.name} // CUSTOM BOOT SEQUENCE`, body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('pc-os-sheet', 'architect-boot-sheet');
    };
    const baseArchitectOpenOS = proto.openPCOS;
    proto.openPCOS = function (id) {
      const result = baseArchitectOpenOS.call(this, id); const machine = this.game.getPC(id);
      if (machine && machine.custom && machine.powered && machine.os) {
        this.pcSession.architectBoot = { progress: 0, done: false };
        this.renderArchitectBoot(machine, this.pcSession.architectBoot);
      }
      return result;
    };
    const baseArchitectRenderOS = proto.renderPCOS;
    proto.renderPCOS = function () {
      const machine = this.game.getPC(this.modal && this.modal.pcId); const boot = this.pcSession && this.pcSession.architectBoot;
      if (machine && machine.custom && machine.powered && machine.os && boot && !boot.done) { this.renderArchitectBoot(machine, boot); return; }
      return baseArchitectRenderOS.call(this);
    };

    const baseArchitectAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      switch (action) {
        case 'pc-open-builder': this.pcTab = 'pc-builder'; this.closeModal(); this.render(); return;
        case 'pc-builder-back': this.pcTab = 'pc-workshop'; this.render(); return;
        case 'pc-builder-slot': this.pcBuilderSlot = element.dataset.type; this.pcBuilderPage = 0; this.pcBuilderQuery = ''; this.pcBuilderBrand = 'all'; this.pcBuilderSocket = 'all'; this.renderView(); return;
        case 'pc-builder-select': this.game.setPCDraftPart(element.dataset.type, id); this.renderView(); return;
        case 'pc-builder-auto': { const year = this.pcBuilderYear === 'all' ? this.game.state.pc.year : this.pcBuilderYear; const report = this.game.pcAutoDraft(year); if (!report) this.toast('Не удалось подобрать совместимый набор', 'warning'); this.renderView(); return; }
        case 'pc-builder-clear': this.game.clearPCDraft(); this.renderView(); return;
        case 'pc-builder-build': { const result = this.game.buildCustomPC(); if (result.ok) { this.pcTab = 'pc-workshop'; this.refresh(false); this.openPC(result.machine.id); } else this.renderView(); return; }
        case 'pc-builder-page': this.pcBuilderPage = Number(element.dataset.page); this.renderView(); return;
        case 'pc-boot-skip': if (this.pcSession && this.pcSession.architectBoot) { this.pcSession.architectBoot.progress = 100; this.pcSession.architectBoot.done = true; this.renderModal(); } return;
        default: return baseArchitectAction.call(this, action, element, event);
      }
    };
    document.addEventListener('submit', event => {
      if (!event.target.matches('[data-pc-catalog-form]')) return;
      event.preventDefault(); clearTimeout(searchTimer); ui.pcBuilderQuery = $('[data-pc-builder-query]', event.target).value.trim(); ui.pcBuilderPage = 0; ui.renderView();
    });
    document.addEventListener('input', event => {
      if (event.target.matches('[data-pc-builder-name]')) ui.game.pcArchitectState().name = event.target.value;
      if (!event.target.matches('[data-pc-builder-query]')) return;
      ui.pcBuilderQuery = event.target.value; clearTimeout(searchTimer);
      searchTimer = setTimeout(() => { if (ui.pcTab === 'pc-builder') { ui.pcBuilderPage = 0; ui.renderView(); const input = $('[data-pc-builder-query]'); if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); } } }, 280);
    });
    document.addEventListener('change', event => {
      if (event.target.matches('[data-pc-builder-year]')) { ui.pcBuilderYear = event.target.value === 'all' ? 'all' : Number(event.target.value); if (ui.pcBuilderYear !== 'all') ui.game.pcArchitectState().selectedYear = ui.pcBuilderYear; ui.pcBuilderPage = 0; ui.renderView(); }
      if (event.target.matches('[data-pc-builder-brand]')) { ui.pcBuilderBrand = event.target.value; ui.pcBuilderPage = 0; ui.renderView(); }
      if (event.target.matches('[data-pc-builder-socket]')) { ui.pcBuilderSocket = event.target.value; ui.pcBuilderPage = 0; ui.renderView(); }
      if (event.target.matches('[data-pc-builder-sort]')) { ui.pcBuilderSort = event.target.value; ui.pcBuilderPage = 0; ui.renderView(); }
      if (event.target.matches('[data-pc-builder-compatible]')) { ui.pcBuilderCompatible = event.target.checked; ui.pcBuilderPage = 0; ui.renderView(); }
      if (event.target.matches('[data-pc-builder-favorites]')) { ui.pcBuilderFavoritesOnly = event.target.checked; ui.pcBuilderPage = 0; ui.renderView(); }
    });
    const baseArchitectTick = proto.onTick;
    proto.onTick = function () {
      const result = baseArchitectTick.call(this);
      if (this.modal && this.modal.type === 'pcOS' && this.pcSession && this.pcSession.architectBoot && !this.pcSession.architectBoot.done) {
        const boot = this.pcSession.architectBoot; boot.progress = Math.min(100, boot.progress + 25); if (boot.progress >= 100) boot.done = true; this.renderModal();
      }
      return result;
    };
    proto.pcArchitectUILoaded = true;
  });
})();
