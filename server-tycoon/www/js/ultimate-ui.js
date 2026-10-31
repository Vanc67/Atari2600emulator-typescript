/* Ultimate 7.1: ROAR-микшер, расширенный Avito Search и две независимые кампании. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.startPCCampaign) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.ultimateUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'\"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const defaultMarketFilters = () => ({ minPrice: '', maxPrice: '', socket: 'all', ramType: 'all', partType: 'all', brand: 'all', condition: 'all', yearFrom: '', yearTo: '', minRam: 'all', minStorage: 'all', affordable: false, sort: 'relevance' });
    ui.pcTab = 'pc-workshop'; ui.marketSearchQuery = ''; ui.marketSearchResults = null;
    ui.marketAdvanced = defaultMarketFilters(); ui.marketFiltersOpen = false;
    ui.audio.radio(); ui.pcSoundscape = new PCSoundscape(ui.audio);
    const brandRow = $('.brand-row');
    if (brandRow && !$('#modeBtn')) {
      const button = document.createElement('button'); button.id = 'modeBtn'; button.className = 'mode-button'; button.dataset.action = 'open-mode-switch'; button.setAttribute('aria-label', 'Выбор режима');
      brandRow.insertBefore(button, $('#audioBtn'));
    }
    document.title = 'Server Tycoon 7.1: ROAR & Avito';

    proto.readMarketFilters = function () {
      const fields = this.view.querySelectorAll('[data-market-field]');
      fields.forEach(field => { this.marketAdvanced[field.dataset.marketField] = field.type === 'checkbox' ? field.checked : field.value; });
      return this.marketAdvanced;
    };
    proto.hasAdvancedMarketFilters = function () { return this.game.hasMarketFilters(Object.assign({}, this.marketAdvanced, { kind: 'all' })); };
    proto.performMarketSearch = function (query) {
      clearTimeout(searchTimer);
      this.marketSearchQuery = String(query || '').trim();
      const filtered = this.hasAdvancedMarketFilters();
      if (!this.marketSearchQuery && !filtered) {
        this.marketSearchResults = null; this.renderView();
        return this.game.filterListings(this.game.state.listings, '', Object.assign({}, this.marketAdvanced, { kind: this.marketFilter }));
      }
      const results = this.game.findListings(this.marketSearchQuery, Object.assign({}, this.marketAdvanced, { kind: this.marketFilter === 'catalog' ? 'all' : this.marketFilter, limit: filtered ? 24 : 8 }));
      this.marketSearchResults = results.map(listing => listing.id);
      this.renderView(); this.processEvents();
      const input = $('[data-market-search]'); if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); }
      return results;
    };
    proto.marketKindRow = function () {
      return `<div class="filter-row avito-kind-row">${[['all','Всё'],['servers','Серверы'],['parts','Запчасти'],['catalog',`Каталог ${DATA.SERVER_TEMPLATES.length}`]].map(([id,label]) => `<button class="filter-chip ${this.marketFilter === id ? 'active' : ''}" data-action="market-filter" data-filter="${id}">${label}</button>`).join('')}</div>`;
    };
    proto.marketActiveChips = function () {
      const f = this.marketAdvanced; const chips = [];
      const add = (field, label) => chips.push(`<button class="avito-active-filter" data-action="market-remove-filter" data-field="${field}">${esc(label)} <b>×</b></button>`);
      if (f.minPrice !== '') add('minPrice', `от ${Number(f.minPrice).toLocaleString('ru-RU')} ₽`);
      if (f.maxPrice !== '') add('maxPrice', `до ${Number(f.maxPrice).toLocaleString('ru-RU')} ₽`);
      if (f.socket !== 'all') add('socket', f.socket);
      if (f.ramType !== 'all') add('ramType', f.ramType);
      if (f.partType !== 'all') add('partType', f.partType.toUpperCase());
      if (f.brand !== 'all') add('brand', f.brand.toUpperCase());
      if (f.condition !== 'all') add('condition', DATA.CONDITIONS[f.condition] ? DATA.CONDITIONS[f.condition].name : f.condition);
      if (f.yearFrom !== '') add('yearFrom', `с ${f.yearFrom} года`);
      if (f.yearTo !== '') add('yearTo', `до ${f.yearTo} года`);
      if (f.minRam !== 'all') add('minRam', `RAM от ${f.minRam} ГБ`);
      if (f.minStorage !== 'all') add('minStorage', `Диск от ${f.minStorage} ГБ`);
      if (f.affordable) add('affordable', 'По карману');
      return chips.length ? `<div class="avito-active-filters">${chips.join('')}<button class="text-button" data-action="market-reset-filters">Сбросить всё</button></div>` : '';
    };
    proto.marketAdvancedPanel = function () {
      if (!this.marketFiltersOpen) return '';
      const f = this.marketAdvanced; const options = this.game.marketFilterOptions();
      const selectOptions = (values, current, label) => `<option value="all">${label}</option>` + values.map(value => `<option value="${esc(value)}" ${String(current) === String(value) ? 'selected' : ''}>${esc(value)}</option>`).join('');
      const typeNames = { cpu: 'Процессоры', ram: 'Память RAM', hdd: 'HDD', ssd: 'SSD / NVMe', gpu: 'Видеокарты', psu: 'Блоки питания', cooler: 'Охлаждение', chassis: 'Корпуса', usb: 'USB-флешки', tool: 'Инструменты' };
      const conditionOptions = Object.entries(DATA.CONDITIONS).map(([id, item]) => `<option value="${id}" ${f.condition === id ? 'selected' : ''}>${esc(item.name)}</option>`).join('');
      return `<form class="avito-advanced" data-market-filter-form><div class="avito-advanced-head"><div><strong>Точный подбор</strong><small>Фильтры работают и без текстового запроса</small></div><button class="icon-button" type="button" data-action="market-toggle-filters" aria-label="Скрыть фильтры">${svgIcon('close')}</button></div><div class="avito-filter-grid"><fieldset class="price-filter"><legend>Цена, ₽</legend><label><span>от</span><input type="number" min="0" step="50" inputmode="numeric" placeholder="0" value="${esc(f.minPrice)}" data-market-field="minPrice"></label><label><span>до</span><input type="number" min="0" step="50" inputmode="numeric" placeholder="∞" value="${esc(f.maxPrice)}" data-market-field="maxPrice"></label></fieldset><label class="filter-field"><span>Сокет</span><select data-market-field="socket">${selectOptions(options.sockets, f.socket, 'Любой сокет')}</select></label><label class="filter-field"><span>Тип памяти</span><select data-market-field="ramType">${selectOptions(options.ramTypes, f.ramType, 'Любая память')}</select></label><label class="filter-field"><span>Категория</span><select data-market-field="partType"><option value="all">Все категории</option>${options.types.map(type => `<option value="${esc(type)}" ${f.partType === type ? 'selected' : ''}>${esc(typeNames[type] || type)}</option>`).join('')}</select></label><label class="filter-field"><span>Бренд</span><select data-market-field="brand">${selectOptions(options.brands, f.brand, 'Все бренды')}</select></label><label class="filter-field"><span>Состояние</span><select data-market-field="condition"><option value="all">Любое состояние</option>${conditionOptions}</select></label><fieldset class="price-filter year-filter"><legend>Год выпуска</legend><label><span>с</span><input type="number" min="1990" max="2026" inputmode="numeric" placeholder="1990" value="${esc(f.yearFrom)}" data-market-field="yearFrom"></label><label><span>по</span><input type="number" min="1990" max="2026" inputmode="numeric" placeholder="2026" value="${esc(f.yearTo)}" data-market-field="yearTo"></label></fieldset><label class="filter-field"><span>Оперативная память</span><select data-market-field="minRam">${[['all','Любой объём'],['1','от 1 ГБ'],['4','от 4 ГБ'],['8','от 8 ГБ'],['16','от 16 ГБ'],['64','от 64 ГБ'],['256','от 256 ГБ']].map(([value,label]) => `<option value="${value}" ${String(f.minRam) === value ? 'selected' : ''}>${label}</option>`).join('')}</select></label><label class="filter-field"><span>Накопитель</span><select data-market-field="minStorage">${[['all','Любой объём'],['100','от 100 ГБ'],['500','от 500 ГБ'],['1000','от 1 ТБ'],['4000','от 4 ТБ'],['10000','от 10 ТБ']].map(([value,label]) => `<option value="${value}" ${String(f.minStorage) === value ? 'selected' : ''}>${label}</option>`).join('')}</select></label></div><label class="affordable-filter"><input type="checkbox" data-market-field="affordable" ${f.affordable ? 'checked' : ''}><span><b>Могу купить сейчас</b><small>Не дороже ${Math.round(this.game.state.money).toLocaleString('ru-RU')} ₽</small></span></label><div class="avito-filter-actions"><button class="btn btn-ghost" type="button" data-action="market-reset-filters">Сбросить</button><button class="btn btn-primary" type="submit">Показать лоты</button></div></form>`;
    };
    proto.marketView = function () {
      if (this.marketFilter === 'catalog') return this.viewHead('ARCHIVE // 1990—2026', 'Каталог серверов', `${DATA.SERVER_TEMPLATES.length} конфигураций · новые и под заказ.`) + this.marketKindRow() + this.catalogView();
      const query = this.marketSearchQuery || ''; const advanced = this.hasAdvancedMarketFilters();
      const source = this.marketSearchResults ? this.marketSearchResults.map(id => this.game.state.listings.find(listing => listing.id === id)).filter(Boolean) : this.game.state.listings.slice();
      const listings = this.game.filterListings(source, query, Object.assign({}, this.marketAdvanced, { kind: this.marketFilter }));
      const filterCount = ['minPrice','maxPrice','socket','ramType','partType','brand','condition','yearFrom','yearTo','minRam','minStorage','affordable'].filter(key => {
        const value = this.marketAdvanced[key]; return key === 'affordable' ? value : value !== '' && value !== 'all';
      }).length;
      let html = this.viewHead('USED // ADVANCED SEARCH', 'Авито', 'Введите название или сразу настройте цену, сокет, состояние и характеристики.');
      html += `<form class="avito-search" data-market-search-form><span>${svgIcon('market')}</span><input type="search" inputmode="search" autocomplete="off" placeholder="Название: Xeon, DDR2, ProLiant…" value="${esc(query)}" data-market-search aria-label="Поиск по названию на Авито"><button class="btn btn-primary" type="submit">Найти</button>${query ? `<button class="icon-button" type="button" data-action="market-search-clear" aria-label="Очистить">${svgIcon('close')}</button>` : ''}</form><div class="avito-quick">${['SDRAM','DDR2','DDR3','DDR4','DDR5','SSD','Xeon','ProLiant'].map(term => `<button class="${query.toLowerCase() === term.toLowerCase() ? 'active' : ''}" data-action="market-quick-search" data-query="${term}">${term}</button>`).join('')}</div>`;
      html += this.marketKindRow();
      html += `<div class="avito-tools"><button class="btn btn-small ${this.marketFiltersOpen || filterCount ? 'btn-primary' : 'btn-ghost'}" data-action="market-toggle-filters" aria-expanded="${this.marketFiltersOpen}">${svgIcon('wrench')} Фильтры ${filterCount ? `<b>${filterCount}</b>` : ''}</button><label class="avito-sort"><span>Сортировка</span><select data-market-sort aria-label="Сортировка лотов">${[['relevance','По relevance'],['priceAsc','Сначала дешевле'],['priceDesc','Сначала дороже'],['yearDesc','Сначала новее'],['yearAsc','Сначала старше'],['name','По названию']].map(([value,label]) => `<option value="${value}" ${this.marketAdvanced.sort === value ? 'selected' : ''}>${label === 'По relevance' ? 'По совпадению' : label}</option>`).join('')}</select></label></div>`;
      html += this.marketAdvancedPanel() + this.marketActiveChips();
      html += `<div class="avito-result-line"><span>${query ? `«${esc(query)}» · ` : advanced ? 'По фильтрам · ' : ''}<b>${listings.length}</b> лотов</span><button class="text-button" data-action="refresh-market">${svgIcon('refresh')} Обновить выдачу</button></div>`;
      if (!listings.length) html += this.emptyState('market', query || advanced ? 'Ничего не найдено' : 'Лоты закончились', query || advanced ? 'Уберите часть фильтров, измените цену или попробуйте другое название.' : 'Обновите выдачу или начните поиск.', query || advanced ? 'Сбросить поиск и фильтры' : 'Обновить', query || advanced ? 'market-reset-all' : 'refresh-market');
      else html += listings.map(listing => this.listingCard(listing)).join('');
      return html;
    };
    const baseUltimateListingCard = proto.listingCard;
    proto.listingCard = function (listing) {
      const html = baseUltimateListingCard.call(this, listing); const details = this.game.listingDetails(listing);
      const tags = [details.sockets[0], details.ramTypes[0], details.year, details.ramTotal ? `${details.ramTotal} ГБ RAM` : '', details.storageTotal ? `${details.storageTotal >= 1000 ? `${Number((details.storageTotal / 1000).toFixed(1))} ТБ` : `${details.storageTotal} ГБ`}` : ''].filter(Boolean);
      return html.replace('<div class="listing-seller">', `<div class="listing-tags">${tags.map(tag => `<span>${esc(tag)}</span>`).join('')}</div><div class="listing-seller">`);
    };

    proto.radioCard = function () {
      const radio = ui.audio.radio(); const status = radio.status(); const settings = this.game.state.settings;
      const minute = value => `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(Math.floor(value % 60)).padStart(2, '0')}`;
      return `<div class="card lofi-card"><div class="lofi-hero"><span class="lofi-disc ${status.playing && settings.musicMode === 'lofi' && settings.music ? 'spinning' : ''}">${svgIcon('music')}</span><div><span>OFFLINE LO-FI RADIO</span><strong>${esc(status.track.title)}</strong><small>${minute(status.position)} / 30:00 · трек ${status.index + 1}/20</small></div><b>10:00:00</b></div><div class="meter lofi-progress"><i style="width:${status.position / 1800 * 100}%"></i></div><div class="lofi-controls"><button class="btn btn-small btn-ghost" data-action="lofi-previous">←</button><button class="btn btn-small ${settings.musicMode === 'lofi' && settings.music ? 'btn-danger' : 'btn-primary'}" data-action="music-mode" data-mode="lofi">${settings.musicMode === 'lofi' && settings.music ? 'Выключить радио' : 'Включить lo-fi'}</button><button class="btn btn-small btn-ghost" data-action="lofi-next">→</button></div><p class="muted tiny">20 длинных генеративных lo-fi треков × 30 минут. Общая длительность 10 часов, затем плейлист повторяется. Ни одного сетевого запроса.</p><details class="lofi-playlist"><summary>Плейлист · 20 треков / 600 минут</summary><div>${radio.tracks.map((track, index) => `<button class="${index === status.index ? 'active' : ''}" data-action="lofi-track" data-index="${index}"><span>${String(index + 1).padStart(2, '0')}</span><strong>${esc(track.title)}</strong><small>30:00 · ${track.bpm} BPM</small></button>`).join('')}</div></details><button class="btn btn-small btn-ghost btn-wide" data-action="music-mode" data-mode="tracks">Классические 5 треков</button><button class="btn btn-small btn-ghost btn-wide" data-action="toggle-pc-sounds">Звуки домашних ПК: ${settings.pcSounds !== false ? 'включены' : 'выключены'}</button></div>`;
    };
    const baseAudioView = proto.audioView;
    proto.audioView = function () { return this.radioCard() + baseAudioView.call(this); };

    const baseRenderNav = proto.renderNav;
    proto.renderNav = function () {
      const pcMode = this.game.state.campaignMode === 'pc';
      const strong = $('.brand-copy strong'); const subtitle = $('.brand-copy span');
      if (strong) strong.textContent = pcMode ? 'PC WORKSHOP' : 'SERVER TYCOON';
      if (subtitle) subtitle.textContent = 'ULTIMATE 7.1 // ROAR + SEARCH';
      const mode = $('#modeBtn'); if (mode) { mode.innerHTML = svgIcon(pcMode ? 'chip' : 'server'); mode.classList.toggle('pc', pcMode); }
      if (!pcMode) return baseRenderNav.call(this);
      const tabs = [['pc-workshop','wrench','Мастерская'],['pc-jobs','contract','Заказы'],['pc-dumpster','trash','Мусорка'],['pc-settings','music','Ещё']];
      $('#bottomNav').innerHTML = tabs.map(([id, icon, label]) => `<button class="nav-button ${this.pcTab === id ? 'active' : ''}" data-tab="${id}" aria-label="${label}">${svgIcon(icon)}<span>${label}</span>${id === 'pc-jobs' && this.game.state.pc.offers.length ? `<span class="nav-badge">${this.game.state.pc.offers.length}</span>` : ''}</button>`).join('');
    };
    const baseRenderHUD = proto.renderHUD;
    proto.renderHUD = function () {
      if (this.game.state.campaignMode !== 'pc') return baseRenderHUD.call(this);
      const pc = this.game.state.pc;
      $('#hud').innerHTML = `${this.hudItem('wallet','БЮДЖЕТ',`${Math.round(pc.money).toLocaleString('ru-RU')} ₽`,'var(--lime)')}${this.hudItem('clock','ГОД',String(pc.year),'var(--cyan)')}${this.hudItem('star','РЕПУТАЦИЯ',String(pc.reputation),'var(--violet)')}${this.hudItem('chip','ЗАКАЗЫ',String(pc.completed),'var(--amber)')}`;
      this.renderPause();
    };
    const baseRenderView = proto.renderView;
    proto.renderView = function () {
      if (this.game.state.campaignMode !== 'pc') return baseRenderView.call(this);
      if (this.pcTab === 'pc-jobs') this.view.innerHTML = this.pcJobsView();
      else if (this.pcTab === 'pc-dumpster') this.view.innerHTML = this.pcDumpsterView();
      else if (this.pcTab === 'pc-settings') this.view.innerHTML = this.pcSettingsView();
      else this.view.innerHTML = this.pcWorkshopView();
      this.view.classList.remove('view'); void this.view.offsetWidth; this.view.classList.add('view');
    };
    const baseSwitchTab = proto.switchTab;
    proto.switchTab = function (tab) {
      if (tab.startsWith('pc-') && this.game.state.campaignMode === 'pc') {
        this.pcTab = tab; this.closeModal(); this.render(); window.scrollTo({ top: 0, behavior: 'auto' }); return;
      }
      return baseSwitchTab.call(this, tab);
    };

    proto.openModeSwitch = function () { this.openModal('modeSelect'); };
    proto.renderModeSelect = function () {
      const pc = this.game.state.pc;
      const body = `<div class="mode-select-hero"><span>ULTIMATE DUAL MODE</span><strong>Две независимые карьеры</strong><p>Прогресс, деньги, время и железо сохраняются отдельно. Переключаться можно в любой момент.</p></div><div class="mode-choice-grid"><article class="mode-choice server"><span class="mode-choice-art">${svgIcon('server')}</span><small>РЕЖИМ 01</small><h3>Серверная ферма</h3><p>2 597 серверов, Time Machine, Deep Lab, Local AI, ОС и стойка.</p><b>${this.game.state.servers.length} серверов · ${Math.round(this.game.state.money).toLocaleString('ru-RU')} ₽</b><button class="btn btn-primary btn-wide" data-action="choose-server-mode">Продолжить ферму</button></article><article class="mode-choice pc"><span class="mode-choice-art">${svgIcon('chip')}</span><small>РЕЖИМ 02</small><h3>Папина PC-мастерская</h3><p>1990→2026, ремонт друзьям, заказы, мусорка, Windows и игровые тесты.</p><b>${pc.started ? `${pc.year} · ${pc.completed} заказов · ${Math.round(pc.money).toLocaleString('ru-RU')} ₽` : 'Новая карьера · 2 500 ₽ от папы'}</b><button class="btn btn-cyan btn-wide" data-action="${pc.started ? 'continue-pc-mode' : 'start-pc-mode'}">${pc.started ? 'Продолжить мастерскую' : 'Начать в 1990 году'}</button></article></div>`;
      this.modalRoot.innerHTML = this.modalShell('Выбор режима', 'SERVER TYCOON 7.1 // ULTIMATE', body, { center: true });
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('mode-sheet');
    };
    const baseOnboarding = proto.renderOnboarding;
    proto.renderOnboarding = function () {
      if (this.showServerOnboarding || this.game.state.flags.modeChosen) return baseOnboarding.call(this);
      const body = `<div class="onboarding-logo ultimate-logo">${svgIcon('chip')}</div><h2 class="onboarding-title">ULTIMATE 7.1</h2><div class="onboarding-sub">СИСАДМИН: ПЕСОЧНИЦА // DUAL MODE</div><p class="timeline-intro">Выберите первую карьеру. Вторая останется доступна через кнопку режима в верхней панели.</p><div class="mode-choice-grid onboarding-modes"><button class="mode-choice server" data-action="onboard-server"><span class="mode-choice-art">${svgIcon('server')}</span><h3>Серверный сисадмин</h3><p>Time Machine, ферма, Авито, Deep Lab и Local AI.</p><b>ВЫБРАТЬ СЕРВЕРЫ →</b></button><button class="mode-choice pc" data-action="onboard-pc"><span class="mode-choice-art">${svgIcon('chip')}</span><h3>Обычные ПК</h3><p>Папа ремонтирует друзьям, 1990 год, первая мастерская.</p><b>НАЧАТЬ У ПАПЫ →</b></button></div><p class="muted tiny timeline-offline">Офлайн · все сохранения и ассеты внутри APK</p>`;
      this.modalRoot.innerHTML = this.modalShell('Ultimate Edition', 'ВЫБЕРИТЕ СВОЮ ИСТОРИЮ', body, { center: true, noClose: true });
    };
    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () {
      if (this.modal && this.modal.type === 'modeSelect') { this.renderModeSelect(); return; }
      return baseRenderModal.call(this);
    };

    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const own = ['open-mode-switch','choose-server-mode','start-pc-mode','continue-pc-mode','onboard-server','onboard-pc','market-search','market-quick-search','market-search-clear','market-toggle-filters','market-reset-filters','market-reset-all','market-remove-filter','market-filter','refresh-market','music-mode','lofi-next','lofi-previous','lofi-track','toggle-pc-sounds'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'open-mode-switch': this.openModeSwitch(); return;
        case 'choose-server-mode': this.game.switchCampaign('servers'); this.tab = 'farm'; this.closeModal(); this.refresh(false); return;
        case 'start-pc-mode': this.game.startPCCampaign(); this.game.state.flags.onboarded = true; this.game.state.flags.modeChosen = true; this.pcTab = 'pc-workshop'; this.closeModal(); this.refresh(false); return;
        case 'continue-pc-mode': this.game.switchCampaign('pc'); this.pcTab = 'pc-workshop'; this.closeModal(); this.refresh(false); return;
        case 'onboard-server': this.game.state.flags.modeChosen = true; this.showServerOnboarding = true; this.game.switchCampaign('servers'); this.renderModal(); return;
        case 'onboard-pc': this.game.startPCCampaign(); this.game.state.flags.onboarded = true; this.game.state.flags.modeChosen = true; this.pcTab = 'pc-workshop'; this.closeModal(); this.refresh(false); return;
        case 'market-search': this.performMarketSearch($('[data-market-search]').value); return;
        case 'market-quick-search': this.performMarketSearch(element.dataset.query); return;
        case 'market-search-clear': this.performMarketSearch(''); return;
        case 'market-toggle-filters': this.readMarketFilters(); this.marketFiltersOpen = !this.marketFiltersOpen; this.renderView(); return;
        case 'market-reset-filters': { const sort = this.marketAdvanced.sort; this.marketAdvanced = defaultMarketFilters(); this.marketAdvanced.sort = sort; this.performMarketSearch(this.marketSearchQuery); return; }
        case 'market-reset-all': this.marketAdvanced = defaultMarketFilters(); this.marketSearchQuery = ''; this.marketSearchResults = null; this.renderView(); return;
        case 'market-remove-filter': this.readMarketFilters(); this.marketAdvanced[element.dataset.field] = defaultMarketFilters()[element.dataset.field]; this.performMarketSearch(this.marketSearchQuery); return;
        case 'market-filter': this.marketFilter = element.dataset.filter; if (this.marketFilter === 'catalog') { this.marketSearchResults = null; this.renderView(); } else this.performMarketSearch(this.marketSearchQuery); return;
        case 'refresh-market': this.marketAdvanced = defaultMarketFilters(); this.marketSearchQuery = ''; this.marketSearchResults = null; this.game.refreshListings(); this.refresh(false); return;
        case 'music-mode':
          if (element.dataset.mode === 'lofi' && this.game.state.settings.musicMode === 'lofi' && this.game.state.settings.music) this.audio.toggle();
          else this.audio.setMusicMode(element.dataset.mode);
          this.refresh(); return;
        case 'lofi-next': this.audio.radio().next(); this.audio.setMusicMode('lofi'); this.refresh(); return;
        case 'lofi-previous': this.audio.radio().previous(); this.audio.setMusicMode('lofi'); this.refresh(); return;
        case 'lofi-track': this.audio.radio().selectTrack(Number(element.dataset.index)); this.audio.setMusicMode('lofi'); this.refresh(); return;
        case 'toggle-pc-sounds': this.game.state.settings.pcSounds = this.game.state.settings.pcSounds === false; this.game.save(); if (this.game.state.settings.pcSounds === false) this.pcSoundscape.stop(); this.refresh(); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };
    document.addEventListener('submit', event => {
      if (event.target.matches('[data-market-search-form]')) { event.preventDefault(); ui.performMarketSearch($('[data-market-search]', event.target).value); }
      if (event.target.matches('[data-market-filter-form]')) { event.preventDefault(); ui.readMarketFilters(); ui.performMarketSearch(ui.marketSearchQuery); }
    });
    let searchTimer = null;
    document.addEventListener('input', event => {
      if (!event.target.matches('[data-market-search]')) return;
      ui.marketSearchQuery = event.target.value;
      clearTimeout(searchTimer);
      const query = event.target.value.trim();
      if (query.length < 2) {
        if (!query) { if (ui.hasAdvancedMarketFilters()) ui.performMarketSearch(''); else { ui.marketSearchResults = null; ui.renderView(); } }
        return;
      }
      searchTimer = setTimeout(() => { if (ui.tab === 'market' && ui.game.state.campaignMode !== 'pc') ui.performMarketSearch(query); }, 260);
    });
    document.addEventListener('change', event => {
      if (event.target.matches('[data-market-field]')) {
        ui.readMarketFilters();
        if (event.target.type === 'checkbox' || event.target.tagName === 'SELECT') ui.performMarketSearch(ui.marketSearchQuery);
      }
      if (event.target.matches('[data-market-sort]')) {
        ui.marketAdvanced.sort = event.target.value;
        if (ui.marketSearchQuery || ui.hasAdvancedMarketFilters()) ui.performMarketSearch(ui.marketSearchQuery);
        else ui.renderView();
      }
    });
    const baseOnTick = proto.onTick;
    proto.onTick = function () {
      const result = baseOnTick.call(this);
      const radio = this.audio.radio(); radio.tick();
      if (this.liveCounter % 10 === 0 && (this.modal && ['audio','pcSettings'].includes(this.modal.type) || this.game.state.campaignMode === 'pc' && this.pcTab === 'pc-settings')) {
        const card = $('.lofi-card'); if (card) { const status = radio.status(); const time = card.querySelector('.lofi-hero small'); if (time) time.textContent = `${String(Math.floor(status.position / 60)).padStart(2,'0')}:${String(Math.floor(status.position % 60)).padStart(2,'0')} / 30:00 · трек ${status.index + 1}/20`; const bar = card.querySelector('.lofi-progress i'); if (bar) bar.style.width = `${status.position / 18}%`; }
      }
      return result;
    };
    proto.ultimateUILoaded = true;
    ui.render(); if (ui.modal) ui.renderModal();
  });
})();
