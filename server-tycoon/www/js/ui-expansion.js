/* Expansion 2.0: дополнительные экраны поверх базового мобильного UI. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.expansionUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
    const money = value => `${Math.round(value).toLocaleString('ru-RU')} ₽`;
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const color = id => {
      const value = window.BRAND_COLORS[id];
      return !value || value.toLowerCase() === '#000000' ? '#e8f4f7' : value;
    };
    const vendor = (id, className = 'vendor-logo') => `<span class="${className}" style="--brand:${color(id)}">${brandSvg(id)}</span>`;

    ui.audio = new TycoonAudio(ui.game);
    ui.catalogBrand = 'all';
    ui.catalogPage = 0;

    const baseRenderPause = proto.renderPause;
    proto.renderPause = function () {
      baseRenderPause.call(this);
      const button = $('#audioBtn');
      if (!button) return;
      const enabled = this.game.state.settings ? this.game.state.settings.music : true;
      button.dataset.action = 'toggle-music';
      button.innerHTML = svgIcon(enabled ? 'music' : 'mute');
      button.classList.toggle('active', enabled);
      button.setAttribute('aria-label', enabled ? 'Выключить музыку' : 'Включить музыку');
    };

    const baseToast = proto.toast;
    proto.toast = function (text, type = 'info') {
      if (this.audio) this.audio.sfx(type);
      return baseToast.call(this, text, type);
    };

    const baseSwitchTab = proto.switchTab;
    proto.switchTab = function (tab) {
      if (this.audio) this.audio.unlock();
      return baseSwitchTab.call(this, tab);
    };

    const baseServerCard = proto.serverCard;
    proto.serverCard = function (server) {
      const html = baseServerCard.call(this, server);
      return html.replace('<span class="server-leds">', `${vendor(server.chassis.brand || deviceBrand(server.chassis.name))}<span class="server-leds">`);
    };

    const baseListingCard = proto.listingCard;
    proto.listingCard = function (listing) {
      const html = baseListingCard.call(this, listing);
      const brand = listing.isServer ? DATA.part(DATA.template(listing.templateId).chassis).brand : (listing.partId && DATA.part(listing.partId) && DATA.part(listing.partId).brand) || deviceBrand(listing.title);
      return html.replace('<span class="condition">', `${vendor(brand)}<span class="condition">`);
    };

    proto.marketView = function () {
      const catalog = this.marketFilter === 'catalog';
      let html = this.viewHead('USED // MARKET // 6.0', 'Авито и каталог', `${DATA.SERVER_TEMPLATES.length} моделей · ${DATA.PARTS.length} деталей · б/у и новое железо.`);
      html += `<div style="display:flex;gap:8px;margin-bottom:13px"><button class="btn btn-primary" style="flex:1" data-action="refresh-market">${svgIcon('refresh')}Обновить лоты</button><span class="day-chip" style="display:flex;align-items:center">${this.game.state.listings.length} лотов</span></div>`;
      html += `<div class="filter-row">${[['all','Авито'],['servers','Серверы'],['parts','Запчасти'],['catalog',`Каталог ${DATA.SERVER_TEMPLATES.length}`]].map(([id,label]) => `<button class="filter-chip ${this.marketFilter === id ? 'active' : ''}" data-action="market-filter" data-filter="${id}">${label}</button>`).join('')}</div>`;
      if (catalog) return html + this.catalogView();
      const listings = this.game.state.listings.filter(listing => this.marketFilter === 'all' || (this.marketFilter === 'servers' ? listing.isServer : !listing.isServer));
      if (!listings.length) html += this.emptyState('market', 'Лоты закончились', 'Обновите выдачу — продавцы уже выкладывают новое железо.', 'Обновить', 'refresh-market');
      else html += listings.map(listing => this.listingCard(listing)).join('');
      return html;
    };

    proto.catalogView = function () {
      const brands = [...new Set(DATA.SERVER_TEMPLATES.map(template => DATA.part(template.chassis).brand || 'server'))];
      const preferred = ['hp','dell','supermicro','lenovo','ibm','cisco','synology','qnap','fujitsu','huawei','oracle','inspur','amd','raspberrypi'];
      brands.sort((a, b) => preferred.indexOf(a) - preferred.indexOf(b));
      const templates = DATA.SERVER_TEMPLATES.filter(template => this.catalogBrand === 'all' || DATA.part(template.chassis).brand === this.catalogBrand);
      const pages = Math.max(1, Math.ceil(templates.length / 12));
      this.catalogPage = Math.max(0, Math.min(this.catalogPage, pages - 1));
      const visible = templates.slice(this.catalogPage * 12, this.catalogPage * 12 + 12);
      let html = `<div class="filter-row brand-filter"><button class="filter-chip ${this.catalogBrand === 'all' ? 'active' : ''}" data-action="catalog-brand" data-brand="all">Все бренды</button>${brands.map(brand => `<button class="filter-chip ${this.catalogBrand === brand ? 'active' : ''}" data-action="catalog-brand" data-brand="${brand}">${brand === 'hp' ? 'HP/HPE' : brand === 'raspberrypi' ? 'Raspberry Pi' : brand.charAt(0).toUpperCase() + brand.slice(1)}</button>`).join('')}</div>`;
      html += `<div class="section-label"><span>Новое · под заказ</span><span>${templates.length} моделей</span></div>`;
      html += visible.map(template => this.catalogCard(template)).join('');
      if (pages > 1) html += `<div class="pagination"><button class="btn btn-small btn-ghost" data-action="catalog-page" data-page="${this.catalogPage - 1}" ${this.catalogPage === 0 ? 'disabled' : ''}>← Назад</button><span class="badge info">${this.catalogPage + 1} / ${pages}</span><button class="btn btn-small btn-ghost" data-action="catalog-page" data-page="${this.catalogPage + 1}" ${this.catalogPage >= pages - 1 ? 'disabled' : ''}>Дальше →</button></div>`;
      return html;
    };

    proto.catalogCard = function (template) {
      const chassis = DATA.part(template.chassis);
      const cpu = DATA.part(template.cpu);
      const ram = template.ram.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const disks = template.disks.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const gpus = template.gpus || [];
      const price = Math.round(template.basePrice * 1.25);
      return `<article class="card listing-card catalog-card"><div class="listing-art" style="color:${color(chassis.brand)}">${brandSvg(chassis.brand)}<span class="condition">НОВЫЙ · ПОД ЗАКАЗ</span></div><div class="listing-info"><h3 class="listing-title">${esc(template.name)}</h3><p class="listing-desc">${cpu.cores} ядер · ${ram} ГБ ${chassis.ramType} · ${disks >= 1000 ? `${disks / 1000} ТБ` : `${disks} ГБ`}${gpus.length ? `<br>${gpus.length}× GPU · ${gpus.reduce((sum, id) => sum + DATA.part(id).vram, 0)} ГБ VRAM` : ''}<br>${chassis.socket} · ${chassis.ramSlots} RAM · ${chassis.pcieSlots || 0} PCIe</p><div class="listing-seller">Официальный поставщик · гарантия</div><div class="listing-price">${money(price)}</div></div><div class="listing-actions"><button class="btn btn-small btn-primary btn-wide" data-action="buy-catalog" data-id="${template.id}">${svgIcon('plus')}Заказать и установить</button></div></article>`;
    };

    const baseFarmView = proto.farmView;
    proto.farmView = function () {
      let html = baseFarmView.call(this);
      let notices = '';
      if (this.game.state.outageDay === this.game.state.day) notices += `<div class="notice error infra-notice">${svgIcon('network')}<div><strong>Авария у провайдера</strong>Серверы работают, но онлайн-доход остановлен до конца дня.</div></div>`;
      if (this.game.state.heatwaveDay === this.game.state.day) notices += `<div class="notice warning infra-notice">${svgIcon('temp')}<div><strong>Аномальная жара · +6°C</strong>Следите за температурой стоек и улучшайте кондиционер.</div></div>`;
      const infra = `${notices}<button class="btn btn-ghost btn-wide infra-button" data-action="open-infra">${svgIcon('network')}Инфраструктура: ${this.game.network().name} · AC ${this.game.state.upgrades.ac} · PDU ${this.game.state.upgrades.pdu} · UPS ${this.game.state.upgrades.ups}</button>`;
      return html.replace('<div class="section-label"><span>Стоечные юниты', `${infra}<div class="section-label"><span>Стоечные юниты`);
    };

    const baseHardwareTab = proto.hardwareTab;
    proto.hardwareTab = function (server) {
      let html = baseHardwareTab.call(this, server);
      const report = this.game.hardwareReport(server);
      html += `<div class="card" style="margin-top:12px"><h3 class="card-title"><span>${svgIcon('gpu')}PCIe и хранилище</span><span class="badge violet">${report.gpuCount} GPU</span></h3><div class="stat-grid"><div class="stat-box"><span>GPU VRAM</span><strong>${report.gpuVram} ГБ</strong></div><div class="stat-box"><span>RAID ЁМКОСТЬ</span><strong>${report.diskTotal} ГБ</strong></div></div></div>`;
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('raid')}RAID-контроллер</span><span class="badge info">${DATA.RAIDS[server.raid].name}</span></h3><p class="muted tiny">Изменение массива стирает ОС и софт. Сервер должен быть выключен.</p><div class="raid-grid">${Object.values(DATA.RAIDS).map(raid => `<button class="btn btn-small ${server.raid === raid.id ? 'btn-primary' : 'btn-ghost'}" data-action="set-raid" data-id="${server.id}" data-raid="${raid.id}" ${server.powered ? 'disabled' : ''}>${raid.name}</button>`).join('')}</div></div>`;
      return html;
    };

    const baseStatsView = proto.statsView;
    proto.statsView = function () {
      let html = baseStatsView.call(this);
      const marker = html.indexOf('Sandbox-пульт');
      if (marker > 0) {
        const insertAt = html.lastIndexOf('<div class="card">', marker);
        html = html.slice(0, insertAt) + this.progressionView() + this.audioView() + html.slice(insertAt);
      } else {
        html += this.progressionView() + this.audioView();
      }
      return html;
    };

    proto.progressionView = function () {
      const s = this.game.state;
      const nextXP = s.level * s.level * 300;
      const previousXP = (s.level - 1) * (s.level - 1) * 300;
      const progress = nextXP > previousXP ? (s.xp - previousXP) / (nextXP - previousXP) * 100 : 100;
      let html = `<div class="card"><h3 class="card-title"><span>${svgIcon('star')}Уровень ${s.level} · ${this.game.playerRank()}</span><span class="badge violet">${s.xp} XP</span></h3><div class="meter xp-meter"><i style="width:${Math.max(0, Math.min(100, progress))}%"></i></div><p class="muted tiny">До следующего уровня: ${Math.max(0, nextXP - s.xp)} XP · опыт начисляется за сборки, ОС, софт, контракты и прибыль.</p></div>`;
      const active = s.activeContract;
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('contract')}Контракты хостинга</span><span class="badge ${active ? 'good' : 'info'}">${active ? 'АКТИВЕН' : 'СВОБОДЕН'}</span></h3>`;
      if (active) {
        const app = DATA.software(active.requiredSoft);
        html += `<div class="contract-active"><strong>${esc(active.title)}</strong><p class="muted small">${app.name} · RAM от ${active.minRam} ГБ · CPU от ${active.minCores} ядер</p><div class="meter"><i style="width:${active.progress / active.duration * 100}%"></i></div><div class="contract-meta"><span>${active.progress} / ${active.duration} дней</span><span class="mint">${money(active.reward)}</span></div><button class="btn btn-small btn-ghost" data-action="abandon-contract">Расторгнуть</button></div>`;
      } else {
        html += s.contracts.map(contract => `<div class="contract-offer"><div><strong>${esc(contract.title)}</strong><p>${DATA.software(contract.requiredSoft).name} · RAM ${contract.minRam}+ · CPU ${contract.minCores}+ · ${contract.duration} дн.</p></div><div class="contract-reward"><strong>${money(contract.reward)}</strong><button class="btn btn-small btn-primary" data-action="accept-contract" data-id="${contract.id}">Взять</button></div></div>`).join('');
      }
      return html + '</div>';
    };

    proto.audioView = function () {
      const settings = this.game.state.settings;
      return `<div class="card"><h3 class="card-title"><span>${svgIcon('music')}Server Room Radio</span><span class="badge ${settings.music ? 'good' : ''}">${settings.music ? 'ON AIR' : 'MUTED'}</span></h3><div class="music-list">${DATA.MUSIC_TRACKS.map(track => `<button class="music-track ${settings.track === track.id && settings.music ? 'active' : ''}" data-action="set-track" data-id="${track.id}"><span class="music-equalizer"><i></i><i></i><i></i></span><span><strong>${track.name}</strong><small>${track.author}</small></span>${settings.track === track.id && settings.music ? '<b>ИГРАЕТ</b>' : '<b>PLAY</b>'}</button>`).join('')}</div><label class="volume-row">${svgIcon('noise')}<input type="range" min="0" max="1" step="0.01" value="${settings.volume}" data-volume aria-label="Громкость музыки"><span data-volume-label>${Math.round(settings.volume * 100)}%</span></label><div class="btn-row"><button class="btn btn-small btn-ghost" data-action="toggle-music">${settings.music ? 'Выключить музыку' : 'Включить музыку'}</button><button class="btn btn-small btn-ghost" data-action="toggle-sfx">SFX: ${settings.sfx ? 'вкл' : 'выкл'}</button></div><p class="muted tiny">CC0 и оригинальные lo-fi треки встроены в APK. Музыка начинается после первого касания — это требование Android WebView.</p></div>`;
    };

    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () {
      if (this.modal && this.modal.type === 'infra') {
        this.renderInfraModal();
        return;
      }
      baseRenderModal.call(this);
      this.enhanceModalBrands();
    };

    proto.enhanceModalBrands = function () {
      $$('.software-row', this.modalRoot).forEach(row => {
        const button = $('[data-soft]', row);
        const app = button ? DATA.software(button.dataset.soft) : null;
        const icon = $('.software-icon', row);
        if (app && icon) {
          icon.innerHTML = brandSvg(app.brand || app.icon);
          icon.style.color = color(app.brand);
        }
      });
      $$('.os-card', this.modalRoot).forEach(card => {
        const os = DATA.os(card.dataset.os);
        const svg = $('svg', card);
        if (os && svg) svg.outerHTML = brandSvg(os.brand || os.icon);
      });
      if (this.modal && this.modal.type === 'server') {
        const server = this.game.getServer(this.modal.serverId);
        const art = $('.detail-art', this.modalRoot);
        if (server && art && !$('.detail-vendor', art)) art.insertAdjacentHTML('afterbegin', vendor(server.chassis.brand || deviceBrand(server.chassis.name), 'detail-vendor'));
      }
    };

    proto.renderInfraModal = function () {
      const s = this.game.state;
      let body = `<div class="section-label"><span>Uplink и провайдер</span><span>оплата ежедневно</span></div>`;
      body += DATA.NETWORKS.map(network => {
        const active = s.networkId === network.id;
        return `<article class="card network-card"><div class="network-head"><span class="room-icon">${svgIcon('network')}</span><div><strong>${network.name}</strong><p>${network.speed} Мбит/с · ${network.description}</p></div><span class="badge ${active ? 'good' : 'info'}">×${network.revenue}</span></div><div class="room-metrics"><div class="room-metric"><span>ТАРИФ</span><strong>${money(network.daily)}/д</strong></div><div class="room-metric"><span>SLA</span><strong>${(network.reliability * 100).toFixed(2)}%</strong></div><div class="room-metric"><span>ПОДКЛЮЧЕНИЕ</span><strong>${money(network.price)}</strong></div></div><button class="btn ${active ? 'btn-ghost' : 'btn-primary'} btn-wide" style="margin-top:10px" data-action="select-network" data-id="${network.id}" ${active ? 'disabled' : ''}>${active ? 'Подключено' : 'Сменить тариф'}</button></article>`;
      }).join('');
      body += `<div class="section-label"><span>Питание и климат</span><span>работает в любом помещении</span></div>`;
      body += Object.entries(DATA.UPGRADES).map(([kind, upgrade]) => {
        const level = s.upgrades[kind];
        const maxed = level >= upgrade.max;
        return `<article class="card upgrade-card"><div class="network-head"><span class="room-icon">${svgIcon(upgrade.icon)}</span><div><strong>${upgrade.name}</strong><p>${upgrade.description}</p></div><span class="badge violet">${level} / ${upgrade.max}</span></div><div class="upgrade-pips">${Array.from({ length: upgrade.max }, (_, index) => `<i class="${index < level ? 'active' : ''}"></i>`).join('')}</div><button class="btn ${maxed ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="buy-upgrade" data-kind="${kind}" ${maxed ? 'disabled' : ''}>${maxed ? 'Максимальный уровень' : `Улучшить за ${money(upgrade.prices[level])}`}</button></article>`;
      }).join('');
      this.modalRoot.innerHTML = this.modalShell('Инфраструктура', `${this.game.room().ambient}°C · лимит ${this.game.room().powerLimit}W · ${this.game.network().speed} Мбит/с`, body);
    };

    const baseOpenConfirm = proto.openConfirm;
    proto.openConfirm = function (title, text, confirmAction, id, danger = false, extra = null) {
      baseOpenConfirm.call(this, title, text, confirmAction, id, danger);
      if (this.modal) this.modal.extra = extra;
    };

    const baseHandleConfirmed = proto.handleConfirmed;
    proto.handleConfirmed = function (action, id) {
      if (action === 'raid') {
        const raid = this.modal.extra;
        this.closeModal();
        this.game.configureRAID(id, raid);
        this.refresh(false);
        return;
      }
      if (action === 'abandon-contract') {
        this.closeModal();
        this.game.abandonContract();
        this.refresh(false);
        return;
      }
      return baseHandleConfirmed.call(this, action, id);
    };

    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      if (this.audio) {
        this.audio.unlock();
        this.audio.sfx('click');
      }
      const id = element.dataset.id;
      switch (action) {
        case 'catalog-brand': this.catalogBrand = element.dataset.brand; this.catalogPage = 0; this.renderView(); return;
        case 'catalog-page': this.catalogPage = Number(element.dataset.page); this.renderView(); window.scrollTo({ top: 0, behavior: 'auto' }); return;
        case 'buy-catalog': this.game.buyCatalogServer(id); this.refresh(false); return;
        case 'open-infra': this.openModal('infra'); return;
        case 'select-network': this.game.upgradeNetwork(id); this.refresh(); return;
        case 'buy-upgrade': this.game.buyUpgrade(element.dataset.kind); this.refresh(); return;
        case 'accept-contract': this.game.acceptContract(id); this.refresh(false); return;
        case 'abandon-contract': this.openConfirm('Расторгнуть контракт?', 'Прогресс будет потерян, а заказчик уйдёт. Новые предложения появятся сразу.', 'abandon-contract', '', true); return;
        case 'set-raid': this.openConfirm('Пересобрать RAID?', `${DATA.RAIDS[element.dataset.raid].name}. ОС, софт и данные массива будут стёрты. Детали останутся.`, 'raid', id, true, element.dataset.raid); return;
        case 'toggle-music': this.audio.toggle(); this.refresh(false); return;
        case 'set-track': this.audio.setTrack(id); this.refresh(false); return;
        case 'toggle-sfx': this.audio.toggleSfx(); this.refresh(false); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };

    document.addEventListener('input', event => {
      if (event.target.matches('[data-volume]')) {
        ui.audio.setVolume(event.target.value);
        const label = $('[data-volume-label]');
        if (label) label.textContent = `${Math.round(Number(event.target.value) * 100)}%`;
      }
    });
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) ui.audio.pauseForBackground();
      else ui.audio.resumeFromBackground();
    });

    proto.expansionUILoaded = true;
    ui.render();
    if (ui.modal) ui.renderModal();
  });
})();
