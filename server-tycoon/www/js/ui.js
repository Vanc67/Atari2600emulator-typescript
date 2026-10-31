(function () {
  'use strict';

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
  const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const fmt = value => Math.round(value).toLocaleString('ru-RU');
  const money = value => `${fmt(value)} ₽`;
  const compactMoney = value => Math.abs(value) >= 1000000 ? `${(value / 1000000).toFixed(2).replace('.', ',')} млн ₽` : money(value);
  const capacity = gb => gb >= 1000 ? `${(gb / 1000).toFixed(gb % 1000 ? 1 : 0)} ТБ` : `${gb} ГБ`;

  class GameUI {
    constructor() {
      this.game = new ServerTycoonGame(true);
      this.tab = 'farm';
      this.marketFilter = 'all';
      this.inventoryFilter = 'all';
      this.modal = null;
      this.modalTab = 'overview';
      this.selectedPartUid = null;
      this.installer = null;
      this.lastStatusSignature = '';
      this.liveCounter = 0;
      this.drag = null;
      this.view = $('#view');
      this.modalRoot = $('#modalRoot');
      this.toastRoot = $('#toastRoot');
      this.init();
    }

    init() {
      $('.brand-mark').innerHTML = svgIcon('server');
      document.addEventListener('click', event => {
        const button = event.target.closest('[data-action], [data-tab]');
        if (!button || button.disabled) return;
        if (button.dataset.tab) {
          this.switchTab(button.dataset.tab);
          return;
        }
        this.handleAction(button.dataset.action, button, event);
      });
      document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && this.modal) this.closeModal();
      });
      document.addEventListener('visibilitychange', () => {
        if (document.hidden) this.game.save();
      });
      window.addEventListener('beforeunload', () => this.game.save());
      this.render();
      this.timer = setInterval(() => this.onTick(), 1000);
      if (!this.game.state.flags.onboarded) this.openOnboarding();
    }

    onTick() {
      this.game.tick();
      this.processEvents();
      this.renderHUD();
      this.liveCounter += 1;
      const signature = this.game.state.servers.map(server => `${server.id}:${server.status}:${server.throttling ? 1 : 0}`).join('|') + `:${this.game.state.day}:${this.game.state.powerOut}`;
      if (signature !== this.lastStatusSignature) {
        this.lastStatusSignature = signature;
        this.renderView();
        this.renderNav();
        if (this.modal && !['installer', 'onboarding'].includes(this.modal.type)) this.renderModal();
      } else {
        this.updateLiveNodes();
      }
      const clock = $('[data-live="clock"]');
      if (clock) clock.textContent = this.game.clock();
      if (this.liveCounter % 5 === 0) this.drawVisibleCharts();
    }

    processEvents() {
      this.game.drainEvents().forEach(event => this.toast(event.text, event.type));
    }

    toast(text, type = 'info') {
      const element = document.createElement('div');
      element.className = `toast ${type === 'error' ? 'error' : type === 'warning' ? 'warning' : ''}`;
      const icon = type === 'error' || type === 'warning' ? 'warning' : type === 'success' ? 'check' : 'info';
      element.innerHTML = `${svgIcon(icon)}<span>${esc(text)}</span>`;
      this.toastRoot.appendChild(element);
      while (this.toastRoot.children.length > 3) this.toastRoot.firstChild.remove();
      setTimeout(() => {
        element.classList.add('out');
        setTimeout(() => element.remove(), 220);
      }, 3000);
    }

    render() {
      this.renderHUD();
      this.renderNav();
      this.renderView();
      this.renderPause();
      this.lastStatusSignature = this.game.state.servers.map(server => `${server.id}:${server.status}:${server.throttling ? 1 : 0}`).join('|') + `:${this.game.state.day}:${this.game.state.powerOut}`;
    }

    refresh(includeModal = true) {
      this.processEvents();
      this.render();
      if (includeModal && this.modal) this.renderModal();
    }

    renderPause() {
      const paused = this.game.state.paused;
      $('#pauseBtn').dataset.action = 'pause';
      $('#pauseBtn').innerHTML = svgIcon(paused ? 'play' : 'pause');
      $('#pauseBtn').setAttribute('aria-label', paused ? 'Продолжить' : 'Пауза');
    }

    renderHUD() {
      const metrics = this.game.farmMetrics();
      const state = this.game.state;
      const tempColor = metrics.temp >= 85 ? 'var(--red)' : metrics.temp >= 70 ? 'var(--amber)' : 'var(--cyan)';
      const noiseColor = metrics.noise > this.game.room().noiseLimit ? 'var(--red)' : 'var(--violet)';
      $('#hud').innerHTML = `
        ${this.hudItem('wallet', 'БАЛАНС', state.debt > 0 ? `Долг ${compactMoney(state.debt)}` : compactMoney(state.money), state.debt > 0 ? 'var(--red)' : 'var(--lime)')}
        ${this.hudItem('bolt', 'ЭНЕРГИЯ', `${fmt(metrics.watts)} <small>Вт</small>`, 'var(--amber)')}
        ${this.hudItem('temp', 'ТЕМП.', `${Math.round(metrics.temp)} <small>°C</small>`, tempColor)}
        ${this.hudItem('noise', 'ШУМ', `${Math.round(metrics.noise)} <small>дБ</small>`, noiseColor)}
      `;
      this.renderPause();
    }

    hudItem(icon, label, value, color) {
      return `<div class="hud-item" style="--hud-color:${color}"><span class="hud-label">${svgIcon(icon)}${label}</span><strong class="hud-value">${value}</strong></div>`;
    }

    renderNav() {
      const tabs = [
        ['farm', 'farm', 'Ферма'], ['market', 'market', 'Авито'],
        ['inventory', 'inventory', 'Инвентарь'], ['stats', 'stats', 'Статистика']
      ];
      $('#bottomNav').innerHTML = tabs.map(([id, icon, label]) => {
        const badge = id === 'inventory' && this.game.state.inventory.length ? `<span class="nav-badge">${this.game.state.inventory.length}</span>` : '';
        return `<button class="nav-button ${this.tab === id ? 'active' : ''}" data-tab="${id}" aria-label="${label}">${svgIcon(icon)}<span>${label}</span>${badge}</button>`;
      }).join('');
    }

    renderView() {
      if (this.tab === 'market') this.view.innerHTML = this.marketView();
      else if (this.tab === 'inventory') this.view.innerHTML = this.inventoryView();
      else if (this.tab === 'stats') this.view.innerHTML = this.statsView();
      else this.view.innerHTML = this.farmView();
      this.view.classList.remove('view');
      void this.view.offsetWidth;
      this.view.classList.add('view');
      requestAnimationFrame(() => this.drawVisibleCharts());
    }

    viewHead(eyebrow, title, subtitle) {
      return `<div class="view-head"><div><div class="eyebrow">${eyebrow}</div><h1 class="view-title">${title}</h1><p class="view-subtitle">${subtitle}</p></div><div class="day-chip">ДЕНЬ ${this.game.state.day}<small data-live="clock">${this.game.clock()}</small></div></div>`;
    }

    farmView() {
      const state = this.game.state;
      const room = this.game.room();
      const metrics = this.game.farmMetrics();
      const nextRoom = DATA.ROOMS[DATA.ROOMS.findIndex(item => item.id === room.id) + 1];
      let html = this.viewHead('RACK // CONTROL', 'Моя ферма', 'Железо, софт и немного электрического безумия.');
      if (state.powerOut) {
        html += `<div class="notice error" style="margin-bottom:12px">${svgIcon('bolt')}<div><strong>Электричество отключено</strong>${state.debt > 0 ? `Долг провайдеру: <b>${money(state.debt)}</b>. Продайте детали или попросите помощь.` : 'Проверьте автомат и запускайте нагрузку по очереди.'}<button class="btn btn-small btn-danger" style="margin-top:9px" data-action="restore-power" ${state.debt > 0 ? 'disabled' : ''}>${svgIcon('power')}Восстановить свет</button></div></div>`;
      }
      if (state.paused) html += `<div class="notice warning" style="margin-bottom:12px">${svgIcon('pause')}<div><strong>Симуляция на паузе</strong>Нажмите ▶ в верхней панели, чтобы продолжить.</div></div>`;
      html += `<div class="card room-card">
        <div class="room-top"><div class="room-name"><div class="room-icon">${svgIcon('home')}</div><div><strong>${room.name}</strong><span><i class="status-dot ${state.powerOut ? 'off' : ''}"></i>${state.powerOut ? 'Нет питания' : room.subtitle}</span></div></div>${nextRoom ? `<button class="btn btn-small btn-ghost" data-action="upgrade-room">Апгрейд</button>` : '<span class="badge good">MAX</span>'}</div>
        <div class="room-metrics"><div class="room-metric"><span>СЕРВЕРЫ</span><strong>${state.servers.length} / ${room.slots}</strong></div><div class="room-metric"><span>ДОХОД</span><strong class="mint">+${fmt(metrics.hourlyRevenue)} ₽/ч</strong></div><div class="room-metric"><span>СЕГОДНЯ</span><strong>${state.stats.energyToday.toFixed(2)} кВт·ч</strong></div></div>
      </div>`;
      html += `<div class="section-label"><span>Стоечные юниты</span><span>${metrics.online} онлайн</span></div><div class="server-grid">`;
      html += state.servers.map(server => this.serverCard(server)).join('');
      if (state.servers.length < room.slots) {
        html += `<button class="server-card add-server" data-action="new-server"><span class="add-circle">${svgIcon('plus')}</span><strong>Новая сборка</strong><span>Нужен корпус из инвентаря</span></button>`;
      }
      html += `</div><div class="notice" style="margin-top:14px">${svgIcon('clock')}<div>1 игровой час = 5 секунд. Доход начисляется ежечасно, счета за свет и аренду — в конце дня.<button class="text-button" data-action="skip-day">Завершить день ускоренно →</button></div></div>`;
      return html;
    }

    statusMeta(server) {
      if (server.status === 'online') return { label: server.throttling ? 'ТРОТТЛИНГ' : 'ОНЛАЙН', className: server.throttling ? 'warning' : 'online' };
      if (server.status === 'bios') return { label: 'BIOS · НЕТ ОС', className: 'info' };
      if (server.status === 'bsod') return { label: 'BSOD · 95°C', className: 'error' };
      if (server.status === 'hung') return { label: 'ЗАВИС · OOM', className: 'error' };
      if (server.status === 'brick') return { label: 'КИРПИЧ', className: 'error' };
      return { label: 'ВЫКЛЮЧЕН', className: '' };
    }

    serverCard(server) {
      const status = this.statusMeta(server);
      const error = ['bsod', 'hung', 'brick'].includes(server.status);
      const metrics = this.game.metrics(server);
      return `<article class="server-card ${server.powered ? 'online' : ''} ${server.throttling ? 'warning' : ''} ${error ? 'error' : ''}" data-action="server-detail" data-id="${server.id}">
        <div class="server-art">${svgIcon(server.chassis.icon)}<span class="server-leds"><i></i><i></i></span></div>
        <div class="server-name">${esc(server.name)}</div><div class="server-model">${esc(server.chassis.short)}</div>
        <div class="server-status"><span class="badge ${status.className}" data-server-status="${server.id}">${status.label}</span><span class="server-temp" data-server-temp="${server.id}">${Math.round(server.temp)}°C</span></div>
        <div class="meter" style="margin-top:9px;--meter-color:${server.temp > 85 ? 'var(--red)' : 'var(--mint)'}"><i data-server-load="${server.id}" style="width:${clamp(metrics.cpuLoad, 0, 100)}%"></i></div>
      </article>`;
    }

    marketView() {
      const listings = this.game.state.listings.filter(listing => this.marketFilter === 'all' || (this.marketFilter === 'servers' ? listing.isServer : !listing.isServer));
      let html = this.viewHead('USED // MARKET', 'Авито', 'Рабочее, почти рабочее и «просто не включается».');
      html += `<div style="display:flex;gap:8px;margin-bottom:13px"><button class="btn btn-primary" style="flex:1" data-action="refresh-market">${svgIcon('refresh')}Обновить лоты</button><span class="day-chip" style="display:flex;align-items:center">${this.game.state.listings.length} лотов</span></div>`;
      html += `<div class="filter-row">${[['all','Всё'],['servers','Серверы и ПК'],['parts','Запчасти']].map(([id,label]) => `<button class="filter-chip ${this.marketFilter === id ? 'active' : ''}" data-action="market-filter" data-filter="${id}">${label}</button>`).join('')}</div>`;
      if (!listings.length) html += this.emptyState('market', 'Лоты закончились', 'Обновите выдачу — продавцы уже выкладывают новое железо.', 'Обновить', 'refresh-market');
      else html += listings.map(listing => this.listingCard(listing)).join('');
      return html;
    }

    listingSpec(listing) {
      if (!listing.isServer) return listing.description;
      const template = DATA.template(listing.templateId);
      const cpu = DATA.part(template.cpu);
      const ram = template.ram.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const disks = template.disks.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const condition = listing.condition;
      return `${cpu.cores} ядер · ${condition === 'noRam' ? 0 : ram} ГБ ОЗУ · ${condition === 'noDisks' ? 'без дисков' : capacity(disks)}`;
    }

    listingCard(listing) {
      const condition = DATA.CONDITIONS[listing.condition];
      return `<article class="card listing-card">
        <div class="listing-art">${svgIcon(listing.icon)}<span class="condition">${listing.isServer ? condition.name : 'Б/У · ДЕТАЛЬ'}</span></div>
        <div class="listing-info"><h3 class="listing-title">${esc(listing.title)}</h3><p class="listing-desc">${esc(this.listingSpec(listing))}</p><div class="listing-seller">${esc(listing.seller)}</div><div class="listing-price">${money(listing.price)}${listing.originalPrice ? `<del>${money(listing.originalPrice)}</del>` : ''}</div></div>
        <div class="listing-actions"><button class="btn btn-small btn-ghost" data-action="bargain" data-id="${listing.id}" ${listing.bargained ? 'disabled' : ''}>${svgIcon('wallet')}${listing.bargained ? 'Торг закрыт' : 'Торг −15%'}</button><button class="btn btn-small btn-primary" data-action="buy" data-id="${listing.id}">${svgIcon('plus')}Купить</button></div>
      </article>`;
    }

    inventoryView() {
      const filters = [['all','Всё'],['cpu','CPU'],['gpu','GPU'],['ram','ОЗУ'],['disk','Диски'],['cooling','Охлаждение'],['other','Прочее']];
      const parts = this.game.state.inventory.filter(part => {
        if (this.inventoryFilter === 'all') return true;
        if (this.inventoryFilter === 'disk') return ['hdd','ssd'].includes(part.type);
        if (this.inventoryFilter === 'cooling') return ['cooler','psu'].includes(part.type);
        if (this.inventoryFilter === 'other') return !['cpu','ram','hdd','ssd','cooler','psu'].includes(part.type);
        return part.type === this.inventoryFilter;
      });
      let html = this.viewHead('PARTS // STORAGE', 'Инвентарь', 'Каждая деталь пригодится. Некоторые — только на продажу.');
      html += `<div class="card inventory-summary"><div><span>ДЕТАЛЕЙ НА ПОЛКЕ</span><strong>${this.game.state.inventory.length}</strong></div><div style="text-align:right"><span>ЛИКВИДНАЯ СТОИМОСТЬ</span><strong>${money(this.game.inventoryValue())}</strong></div></div>`;
      html += `<div class="filter-row">${filters.map(([id,label]) => `<button class="filter-chip ${this.inventoryFilter === id ? 'active' : ''}" data-action="inventory-filter" data-filter="${id}">${label}</button>`).join('')}</div>`;
      if (!parts.length) html += this.emptyState('inventory', 'Полка пуста', 'Купите запчасти на Авито или разберите лишний сервер.', 'На Авито', 'go-market');
      else html += `<div class="inventory-grid">${parts.map(part => this.inventoryPartCard(part)).join('')}</div>`;
      return html;
    }

    inventoryPartCard(part) {
      let action = '';
      if (part.type === 'chassis') action = `<button class="btn btn-small btn-primary" data-action="build-with" data-id="${part.uid}" title="Собрать сервер">${svgIcon('plus')}</button>`;
      else if (part.type === 'usb') action = `<button class="btn btn-small btn-cyan" data-action="write-image" data-id="${part.uid}" title="Записать образ">${svgIcon('os')}</button>`;
      else if (part.type === 'tool') action = `<span class="badge good" title="Скидка на ремонт">В НАБОРЕ</span>`;
      return `<article class="part-card"><div class="part-art">${svgIcon(part.icon)}</div><div class="part-name">${esc(part.name)}</div><div class="part-spec">${esc(this.partSpec(part))}</div><div class="part-footer"><span class="part-value">~${money(this.game.partSellPrice(part))}</span><div class="btn-row">${action}<button class="icon-button part-action btn-ghost" data-action="sell-part" data-id="${part.uid}" title="Продать">${svgIcon('trash')}</button></div></div></article>`;
    }

    partSpec(part) {
      if (part.type === 'cpu') return `${part.socket} · ${part.cores} ядер · ${part.tdp}W`;
      if (part.type === 'gpu') return `${part.vram} ГБ VRAM · ${part.tdp}W · ${part.compute} CU`;
      if (part.type === 'ram') return `${part.ramType} · ${part.capacity} ГБ · ${part.watts}W`;
      if (['hdd','ssd'].includes(part.type)) return `${capacity(part.capacity)} · SATA · ${part.watts}W`;
      if (part.type === 'cooler') return `TDP ${part.cooling}W · ${part.noise >= 0 ? '+' : ''}${part.noise} дБ`;
      if (part.type === 'psu') return `${part.watts}W · ${Math.round(part.efficiency * 100)}%${part.broken ? ' · СГОРЕЛ' : ''}`;
      if (part.type === 'chassis') return `${part.socket} · ${part.ramType} · ${part.sataPorts} SATA`;
      if (part.type === 'usb') return part.image ? `Образ: ${DATA.os(part.image).name}` : 'Образ не записан';
      return part.short || '';
    }

    statsView() {
      const s = this.game.state.stats;
      const state = this.game.state;
      const lastDays = s.daily.slice(-7);
      const best = s.daily.reduce((max, day) => Math.max(max, day.profit), 0);
      const nextMom = Math.max(0, 3 - (state.day - state.flags.lastMomDay));
      let html = this.viewHead('LOGS // ANALYTICS', 'Статистика', 'Бухгалтерия, аварии и пульт песочницы.');
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('stats')}Финансы</span><span class="badge ${state.debt ? 'error' : 'good'}">${state.debt ? `ДОЛГ ${money(state.debt)}` : 'БЕЗ ДОЛГОВ'}</span></h3><div class="stat-grid"><div class="stat-box"><span>ВСЕГО ЗАРАБОТАНО</span><strong class="mint">+${fmt(s.earned)} ₽</strong></div><div class="stat-box"><span>ВСЕГО ПОТРАЧЕНО</span><strong class="red">−${fmt(s.spent)} ₽</strong></div><div class="stat-box"><span>ЭЛЕКТРИЧЕСТВО</span><strong>${fmt(s.electricity)} ₽</strong></div><div class="stat-box"><span>АРЕНДА</span><strong>${fmt(s.rent)} ₽</strong></div></div></div>`;
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('stats')}Прибыль по дням</span><span class="muted tiny">рекорд ${money(best)}</span></h3>${lastDays.length ? `<div class="chart-wrap"><canvas id="financeChart" aria-label="График прибыли"></canvas></div>` : '<p class="muted small">График появится после первого игрового дня.</p>'}</div>`;
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('warning')}Эксплуатация</span></h3><div class="stat-grid"><div class="stat-box"><span>ПЕРЕГРЕВОВ / BSOD</span><strong class="amber">${s.crashes}</strong></div><div class="stat-box"><span>ЗАВИСАНИЙ OOM</span><strong class="amber">${s.hangs}</strong></div><div class="stat-box"><span>ШТРАФЫ ЗА ШУМ</span><strong>${fmt(s.noiseFines)} ₽</strong></div><div class="stat-box"><span>УДАЧНЫХ ТОРГОВ</span><strong>${s.bargains}</strong></div></div></div>`;
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('wrench')}Sandbox-пульт</span><span class="badge violet">ЧИТЫ ВКЛ.</span></h3><div class="btn-row"><button class="btn btn-primary" data-action="cheat" data-amount="100000">+100 000 ₽</button><button class="btn btn-cyan" data-action="cheat" data-amount="1000000">+1 млн ₽</button></div><div class="btn-row" style="margin-top:8px"><button class="btn btn-amber" data-action="mom-help" ${nextMom ? 'disabled' : ''}>${svgIcon('home')}Мама, помоги!</button><button class="btn btn-ghost" data-action="skip-day">${svgIcon('clock')}Следующий день</button></div>${nextMom ? `<p class="muted tiny">Мама снова поможет через ${nextMom} дн.</p>` : '<p class="muted tiny">Мама может выдать 20 000 ₽ раз в 3 игровых дня.</p>'}<div class="separator"></div><button class="btn btn-small btn-ghost btn-wide" data-action="reset-confirm">Сбросить сохранение</button></div>`;
      html += `<div class="card"><h3 class="card-title"><span>${svgIcon('info')}Журнал событий</span><span class="muted tiny">${state.logs.length} записей</span></h3><div class="log-list">${state.logs.slice(0, 20).map(log => `<div class="log-item"><span class="log-day">Д${log.day}</span><i class="log-marker ${log.type === 'bad' ? 'bad' : log.type === 'good' ? 'good' : ''}"></i><span class="log-text">${esc(log.text)}</span></div>`).join('')}</div></div>`;
      return html;
    }

    emptyState(icon, title, text, buttonLabel, action) {
      return `<div class="empty-state">${svgIcon(icon)}<strong>${title}</strong><p>${text}</p>${buttonLabel ? `<button class="btn btn-primary" data-action="${action}">${buttonLabel}</button>` : ''}</div>`;
    }

    switchTab(tab) {
      if (!['farm','market','inventory','stats'].includes(tab)) return;
      this.tab = tab;
      this.closeModal();
      this.render();
      window.scrollTo({ top: 0, behavior: 'auto' });
    }

    openModal(type, data = {}) {
      this.modal = Object.assign({ type }, data);
      this.modalRoot.classList.add('open');
      this.modalRoot.setAttribute('aria-hidden', 'false');
      document.body.style.overflow = 'hidden';
      this.renderModal();
    }

    closeModal() {
      if (this.installer && this.installer.timer) {
        clearInterval(this.installer.timer);
        this.installer = null;
      }
      this.modal = null;
      this.selectedPartUid = null;
      this.modalRoot.classList.remove('open');
      this.modalRoot.setAttribute('aria-hidden', 'true');
      this.modalRoot.innerHTML = '';
      document.body.style.overflow = '';
    }

    modalShell(title, subtitle, body, options = {}) {
      const tabs = options.tabs ? `<div class="modal-tabs">${options.tabs.map(([id,label]) => `<button class="modal-tab ${this.modalTab === id ? 'active' : ''}" data-action="detail-tab" data-tab-id="${id}">${label}</button>`).join('')}</div>` : '';
      return `<div class="modal-backdrop" data-action="close-modal"></div><section class="modal-sheet ${options.center ? 'center' : ''}" role="dialog" aria-modal="true" aria-label="${esc(title)}">${options.center ? '' : '<div class="modal-handle"></div>'}<div class="modal-head"><div class="modal-head-copy"><h2 class="modal-title">${esc(title)}</h2>${subtitle ? `<div class="modal-subtitle">${esc(subtitle)}</div>` : ''}</div>${options.noClose ? '' : `<button class="icon-button modal-close" data-action="close-modal" aria-label="Закрыть">${svgIcon('close')}</button>`}</div>${tabs}<div class="modal-body">${body}</div></section>`;
    }

    renderModal() {
      if (!this.modal) return;
      const type = this.modal.type;
      if (type === 'server') this.renderServerModal();
      else if (type === 'constructor') this.renderConstructor();
      else if (type === 'newServer') this.renderNewServer();
      else if (type === 'writeImage') this.renderWriteImage();
      else if (type === 'osSelect') this.renderOSSelect();
      else if (type === 'installer') this.renderInstaller();
      else if (type === 'room') this.renderRoomModal();
      else if (type === 'confirm') this.renderConfirm();
      else if (type === 'onboarding') this.renderOnboarding();
      if (type === 'constructor') this.bindDragAndDrop();
      requestAnimationFrame(() => this.drawVisibleCharts());
    }

    openServer(serverId, tab = 'overview') {
      const server = this.game.getServer(serverId);
      if (!server) return;
      this.modalTab = tab;
      this.openModal('server', { serverId });
    }

    renderServerModal() {
      const server = this.game.getServer(this.modal.serverId);
      if (!server) { this.closeModal(); return; }
      const tabs = [['overview','Мониторинг'],['software','ОС и софт'],['hardware','Железо']];
      let body = '';
      if (this.modalTab === 'software') body = this.softwareTab(server);
      else if (this.modalTab === 'hardware') body = this.hardwareTab(server);
      else body = this.overviewTab(server);
      this.modalRoot.innerHTML = this.modalShell(server.name, `${server.chassis.name} · ${DATA.CONDITIONS[server.condition] ? DATA.CONDITIONS[server.condition].name : 'Самосбор'}`, body, { tabs });
    }

    overviewTab(server) {
      const m = this.game.metrics(server);
      const report = this.game.hardwareReport(server);
      const status = this.statusMeta(server);
      const powerLabel = server.powered ? 'Выключить' : 'Включить';
      let html = `<div class="detail-hero"><div class="detail-art">${svgIcon(server.chassis.icon)}</div><div><h3 class="detail-name">${esc(server.name)}</h3><div class="detail-spec">${server.parts.cpu ? esc(server.parts.cpu.name) : 'CPU не установлен'}<br>${m.ramTotal} ГБ ОЗУ · ${capacity(report.diskTotal)}<br>${server.os ? DATA.os(server.os).name : 'Операционная система не установлена'}</div><span class="badge ${status.className}" style="margin-top:7px">${status.label}</span></div></div>`;
      if (server.crashed) html += `<div class="notice error" style="margin-top:12px">${svgIcon('warning')}<div><strong>Синий экран смерти</strong>Температура достигла 95°C. Переберите охлаждение перед запуском.</div></div>`;
      if (server.hung) html += `<div class="notice error" style="margin-top:12px">${svgIcon('warning')}<div><strong>Out of memory</strong>ОЗУ заполнена на 100%. Перезагрузите сервер и удалите часть софта.</div></div>`;
      if (server.broken) html += `<div class="notice error" style="margin-top:12px">${svgIcon('warning')}<div><strong>Не проходит POST</strong>Куплен как «кирпич». Нужна диагностика и ремонт платы.</div></div>`;
      html += `<div class="gauge-grid">
        ${this.gauge('Температура', `${Math.round(server.temp)}°C`, server.temp / 100 * 100, server.temp > 85 ? 'var(--red)' : 'var(--cyan)', `data-live-gauge="temp" data-id="${server.id}"`)}
        ${this.gauge('Нагрузка CPU', `${Math.round(m.cpuLoad)}%`, m.cpuLoad, server.throttling ? 'var(--amber)' : 'var(--mint)', `data-live-gauge="cpu" data-id="${server.id}"`)}
        ${this.gauge('ОЗУ', `${m.ramUsed.toFixed(1)} / ${m.ramTotal} ГБ`, m.ramPercent, m.ramPercent >= 90 ? 'var(--red)' : 'var(--violet)', `data-live-gauge="ram" data-id="${server.id}"`)}
        ${this.gauge('Питание / шум', `${Math.round(m.watts)}W · ${Math.round(m.noise)}дБ`, m.watts / Math.max(1, this.game.room().powerLimit) * 100, 'var(--amber)', `data-live-gauge="watts" data-id="${server.id}"`)}
      </div>`;
      html += `<div class="action-grid"><button class="btn ${server.powered ? 'btn-danger' : 'btn-primary'}" data-action="power" data-id="${server.id}">${svgIcon('power')}${powerLabel}</button>`;
      if (server.crashed || server.broken) html += `<button class="btn btn-amber" data-action="repair" data-id="${server.id}">${svgIcon('wrench')}Отремонтировать</button>`;
      else if (server.hung) html += `<button class="btn btn-amber" data-action="reboot" data-id="${server.id}">${svgIcon('refresh')}Перезагрузить</button>`;
      else html += `<button class="btn btn-cyan" data-action="os-select" data-id="${server.id}">${svgIcon('os')}Установить ОС</button>`;
      html += `<button class="btn btn-ghost" data-action="open-constructor" data-id="${server.id}">${svgIcon('chip')}Конструктор</button><button class="btn btn-ghost" data-action="sell-server-confirm" data-id="${server.id}">${svgIcon('wallet')}Продать · ${money(this.game.serverSellPrice(server))}</button></div>`;
      if (!report.ok && !server.powered) html += `<div class="notice warning" style="margin-top:12px">${svgIcon('info')}<div><strong>Чек-лист запуска</strong>${report.issues.map(esc).join('<br>')}</div></div>`;
      html += `<div class="section-label"><span>История телеметрии</span><span>последние 4 мин</span></div><div class="chart-wrap"><canvas id="telemetryChart" data-server="${server.id}"></canvas></div><div class="btn-row"><span class="badge cyan">— °C</span><span class="badge good">— CPU %</span><span class="badge violet">— RAM %</span></div>`;
      return html;
    }

    gauge(label, value, percent, color, attributes = '') {
      return `<div class="gauge" ${attributes}><div class="gauge-head"><span>${label}</span><strong>${value}</strong></div><div class="meter" style="--meter-color:${color}"><i style="width:${clamp(percent, 0, 100)}%"></i></div></div>`;
    }

    softwareTab(server) {
      let html = `<div class="notice" style="margin-bottom:10px">${svgIcon('os')}<div><strong>${server.os ? DATA.os(server.os).name : 'Нет операционной системы'}</strong>${server.os ? 'Доход начисляется только на включённом сервере. Следите за суммарной нагрузкой.' : 'Запишите образ на флешку в инвентаре, затем установите ОС.'}</div></div><button class="btn btn-cyan btn-wide" data-action="os-select" data-id="${server.id}">${svgIcon('os')}${server.os ? 'Переустановить / сменить ОС' : 'Выбрать флешку и установить ОС'}</button><div class="section-label"><span>Пакеты и сервисы</span><span>${server.software.length} / ${DATA.SOFTWARE.length}</span></div>`;
      html += DATA.SOFTWARE.map(app => {
        const installed = server.software.includes(app.id);
        const supported = server.os && app.os.includes(server.os);
        const cores = server.parts.cpu ? server.parts.cpu.cores : 0;
        const rate = app.rate + (app.coreRate || 0) * cores;
        const description = app.description.replace(/ · \+\d+(?:\.\d+)? ₽\/ч$/, '');
        const supportText = supported || !server.os ? description : `Нужна ОС: ${app.os.slice(0, 2).map(id => DATA.os(id).name).join(' / ')}${app.os.length > 2 ? ` и ещё ${app.os.length - 2}` : ''}`;
        return `<div class="software-row"><div class="software-icon">${svgIcon(app.icon)}</div><div><div class="software-name">${app.name}</div><div class="software-spec">CPU ${app.cpu}% · RAM ${app.ram} ГБ · +${rate} ₽/ч<br>${supportText}</div></div><button class="btn ${installed ? 'btn-danger' : 'btn-primary'}" data-action="${installed ? 'uninstall-soft' : 'install-soft'}" data-id="${server.id}" data-soft="${app.id}" ${!installed && !supported ? 'disabled' : ''}>${installed ? 'Удалить' : 'Установить'}</button></div>`;
      }).join('');
      return html;
    }

    hardwareTab(server) {
      const report = this.game.hardwareReport(server);
      return `<div class="detail-hero"><div class="detail-art">${svgIcon('motherboard')}</div><div><h3 class="detail-name">${esc(server.chassis.name)}</h3><div class="detail-spec">Сокет: ${server.chassis.socket}<br>Память: ${server.chassis.ramType} · ${server.chassis.ramSlots} слота<br>SATA: ${server.chassis.sataPorts} · охлаждение ×${server.chassis.coolingFactor}</div></div></div><div class="card" style="margin-top:13px"><div class="stat-grid"><div class="stat-box"><span>ТРЕБУЕТСЯ БП</span><strong>${report.requiredPsu}W</strong></div><div class="stat-box"><span>УСТАНОВЛЕНО</span><strong>${server.parts.psu ? server.parts.psu.watts : 0}W</strong></div><div class="stat-box"><span>ОЗУ</span><strong>${report.ramTotal} ГБ</strong></div><div class="stat-box"><span>ДИСКИ</span><strong>${capacity(report.diskTotal)}</strong></div></div></div><button class="btn btn-primary btn-wide" data-action="open-constructor" data-id="${server.id}">${svgIcon('chip')}Открыть 2D-конструктор</button><div class="action-grid"><button class="btn btn-ghost" data-action="dismantle-confirm" data-id="${server.id}">${svgIcon('trash')}Разобрать всё</button><button class="btn btn-ghost" data-action="repair" data-id="${server.id}" ${!server.broken && !server.crashed ? 'disabled' : ''}>${svgIcon('wrench')}Ремонт</button></div>${server.powered ? '<div class="notice warning" style="margin-top:12px">Для изменения конфигурации выключите сервер.</div>' : ''}`;
    }

    renderNewServer() {
      const chassis = this.game.state.inventory.filter(part => part.type === 'chassis');
      let body = '';
      if (!chassis.length) body = this.emptyState('chassis', 'Нет свободного корпуса', 'Купите корпус/материнскую плату на Авито или разберите сервер.', 'Найти на Авито', 'go-market');
      else body = `<div class="constructor-help">Выберите основу нового сервера. Корпус определяет сокет CPU, тип DDR, число слотов ОЗУ и SATA.</div>${chassis.map(part => `<article class="card" style="display:grid;grid-template-columns:70px 1fr auto;align-items:center;gap:10px"><div style="width:70px;height:70px">${svgIcon(part.icon)}</div><div><strong class="small">${esc(part.name)}</strong><div class="part-spec">${esc(this.partSpec(part))}</div></div><button class="btn btn-small btn-primary" data-action="confirm-build" data-id="${part.uid}">Собрать</button></article>`).join('')}`;
      this.modalRoot.innerHTML = this.modalShell('Новая сборка', `${this.game.state.servers.length} / ${this.game.room().slots} мест занято`, body);
    }

    renderConstructor() {
      const server = this.game.getServer(this.modal.serverId);
      if (!server) { this.closeModal(); return; }
      const parts = this.game.state.inventory.filter(part => !['usb','tool','chassis'].includes(part.type));
      let body = `<div class="constructor-help"><b>Мобильный монтаж:</b> перетащите деталь на слот пальцем или коснитесь детали, а затем слота. DDR4 не встанет в DDR3, сокеты тоже проверяются.${server.powered ? '<br><b class="red">Сервер включён — монтаж заблокирован.</b>' : ''}</div>`;
      body += `<div class="motherboard"><div class="board-label">MAINBOARD // ${esc(server.chassis.name)} · ${server.chassis.socket} · ${server.chassis.ramType}</div><div class="slot-grid">`;
      body += this.slotHTML(server, 'cpu', 'CPU SOCKET');
      for (let i = 0; i < server.chassis.ramSlots; i += 1) body += this.slotHTML(server, `ram${i}`, `DDR ${i + 1}`);
      body += this.slotHTML(server, 'cooler', 'CPU COOLER') + this.slotHTML(server, 'psu', 'POWER SUPPLY');
      const pcieSlots = Number.isFinite(server.chassis.pcieSlots) ? server.chassis.pcieSlots : 1;
      for (let i = 0; i < pcieSlots; i += 1) body += this.slotHTML(server, `gpu${i}`, `PCIE GPU ${i + 1}`);
      for (let i = 0; i < server.chassis.sataPorts; i += 1) body += this.slotHTML(server, `disk${i}`, `SATA ${i}`);
      body += `</div></div><div class="section-label"><span>Детали на полке</span><span>${this.selectedPartUid ? 'Теперь выберите слот' : 'Перетащите на плату'}</span></div>`;
      if (!parts.length) body += this.emptyState('inventory', 'Нет деталей для установки', 'Загляните на Авито за процессором, памятью, дисками, кулером и БП.', 'На Авито', 'go-market');
      else body += `<div class="constructor-inventory">${parts.map(part => {
        const selected = this.selectedPartUid === part.uid;
        return `<article class="part-card draggable-part ${selected ? 'selected' : ''}" draggable="true" data-action="select-part" data-id="${part.uid}" data-part-uid="${part.uid}"><div class="part-art">${svgIcon(part.icon)}</div><div class="part-name">${esc(part.name)}</div><div class="part-spec">${esc(this.partSpec(part))}</div>${selected ? '<div class="compatibility ok">ВЫБРАНО · ТАП ПО СЛОТУ</div>' : ''}</article>`;
      }).join('')}</div>`;
      body += `<button class="btn btn-ghost btn-wide" data-action="back-to-server" data-id="${server.id}">${svgIcon('chevron')}Вернуться к серверу</button>`;
      this.modalRoot.innerHTML = this.modalShell('2D-конструктор', `${server.name} · ${server.powered ? 'ВКЛЮЧЁН' : 'выключен, можно монтировать'}`, body);
    }

    slotHTML(server, slotKey, label) {
      const part = this.game.getSlotPart(server, slotKey);
      return `<div class="slot ${part ? 'filled' : ''}" data-action="slot" data-slot="${slotKey}" data-id="${server.id}"><span class="slot-type">${label}</span>${part ? `<button class="slot-remove" data-action="remove-part" data-slot="${slotKey}" data-id="${server.id}" aria-label="Снять">${svgIcon('close')}</button><div class="slot-part">${svgIcon(part.icon)}<div><strong>${esc(part.name)}</strong><span>${esc(this.partSpec(part))}</span></div></div>` : `<div class="slot-empty">${svgIcon('plus')}Пустой слот</div>`}</div>`;
    }

    renderWriteImage() {
      const usb = this.game.getPart(this.modal.partUid);
      if (!usb) { this.closeModal(); return; }
      const body = `<div class="notice" style="margin-bottom:13px">${svgIcon('usb')}<div><strong>${esc(usb.name)}</strong>${usb.image ? `Сейчас записан ${DATA.os(usb.image).name}. Новый образ заменит старый.` : 'Выберите образ. Флешка останется в инвентаре и сможет устанавливать ОС повторно.'}</div></div><div class="os-grid">${DATA.OS_LIST.map(os => `<button class="os-card ${usb.image === os.id ? 'selected' : ''}" data-action="confirm-image" data-id="${usb.uid}" data-os="${os.id}">${svgIcon(os.icon)}<strong>${os.name}</strong><span>${os.description}<br>RAM от ${os.minRam} ГБ · диск от ${os.minDisk} ГБ</span><span class="${os.price ? 'amber' : 'mint'}" style="margin-top:8px">${os.price ? `Лицензия ${money(os.price)}` : 'БЕСПЛАТНО'}</span></button>`).join('')}</div>`;
      this.modalRoot.innerHTML = this.modalShell('Записать образ', 'USB WRITE // OFFLINE MEDIA', body);
    }

    renderOSSelect() {
      const server = this.game.getServer(this.modal.serverId);
      if (!server) { this.closeModal(); return; }
      const usbs = this.game.state.inventory.filter(part => part.type === 'usb' && part.image);
      let body = `<div class="notice" style="margin-bottom:12px">${svgIcon('info')}<div><strong>Установка на ${esc(server.name)}</strong>При смене ОС текущий софт будет удалён. Железо и накопители останутся в игре.</div></div>`;
      if (!usbs.length) body += this.emptyState('usb', 'Нет загрузочной флешки', 'Откройте инвентарь и запишите Debian, Windows Server, TrueNAS или Proxmox на USB.', 'Записать образ', 'go-inventory');
      else body += usbs.map(usb => {
        const check = this.game.checkOSInstall(server.id, usb.uid);
        const os = DATA.os(usb.image);
        return `<article class="card"><div style="display:flex;align-items:center;gap:12px"><div style="width:58px;height:58px">${svgIcon('usb')}</div><div style="flex:1"><strong>${os.name}</strong><div class="part-spec">${esc(usb.name)} · RAM от ${os.minRam} ГБ · диск от ${os.minDisk} ГБ</div></div></div>${!check.ok ? `<div class="notice warning" style="margin-top:10px">${svgIcon('warning')}<div>${esc(check.message)}</div></div>` : ''}<button class="btn btn-primary btn-wide" style="margin-top:11px" data-action="start-os" data-id="${server.id}" data-usb="${usb.uid}" ${!check.ok ? 'disabled' : ''}>${svgIcon('os')}Начать установку</button></article>`;
      }).join('');
      this.modalRoot.innerHTML = this.modalShell('Загрузочные образы', 'Выберите USB-накопитель', body);
    }

    startInstaller(serverId, usbUid) {
      const check = this.game.checkOSInstall(serverId, usbUid);
      if (!check.ok) { this.game.notify(check.message, 'error'); this.refresh(); return; }
      this.installer = {
        serverId, usbUid, os: check.os, progress: 0, errors: 0, error: false,
        lines: [`boot: USB / ${check.os.name}`, check.os.terminal], timer: null, lastLine: -1
      };
      this.openModal('installer', { serverId });
      this.installer.timer = setInterval(() => this.installerTick(), 360);
    }

    installerTick() {
      const install = this.installer;
      if (!install || install.error || install.progress >= 100) return;
      install.progress = Math.min(100, install.progress + 2 + Math.random() * 5);
      const lineIndex = Math.min(DATA.INSTALL_LINES.length - 1, Math.floor(install.progress / 13));
      if (lineIndex !== install.lastLine) {
        install.lastLine = lineIndex;
        install.lines.push(DATA.INSTALL_LINES[lineIndex]);
      }
      if (install.progress > 18 && install.progress < 94 && install.errors < 2 && Math.random() < 0.045) {
        install.error = true;
        install.errors += 1;
        install.lines.push(DATA.INSTALL_ERRORS[Math.floor(Math.random() * DATA.INSTALL_ERRORS.length)]);
        this.renderInstaller();
        return;
      }
      if (install.progress >= 100) {
        clearInterval(install.timer);
        install.timer = null;
        install.lines.push('INSTALL_COMPLETE · rebooting...');
        this.game.finishOSInstall(install.serverId, install.usbUid);
        this.processEvents();
        this.render();
      }
      this.updateInstallerDOM();
    }

    renderInstaller() {
      const install = this.installer;
      if (!install) { this.closeModal(); return; }
      const done = install.progress >= 100;
      const body = `<div class="install-progress"><div class="install-animation">${svgIcon(done ? 'check' : install.os.icon)}</div><h3 style="margin:0">${done ? 'Система установлена' : install.error ? 'Ошибка установки' : `Установка ${esc(install.os.name)}`}</h3><p class="muted small">${done ? 'Сервер перезагружен и готов к установке софта.' : install.error ? 'Нажмите «Исправить» — прогресс сохранён.' : 'Не извлекайте флешку. Почти как настоящий сервер.'}</p><div class="progress-track"><div id="installFill" class="progress-fill" style="width:${install.progress}%"></div></div><strong id="installPercent" class="mono violet">${Math.floor(install.progress)}%</strong><div id="installTerminal" class="terminal">${install.lines.slice(-7).map(line => `<div class="${line.startsWith('ERR') ? 'error-line' : ''}">&gt; ${esc(line)}</div>`).join('')}</div>${install.error ? `<button class="btn btn-amber btn-wide" style="margin-top:13px" data-action="retry-install">${svgIcon('wrench')}Исправить и продолжить</button>` : ''}${done ? `<button class="btn btn-primary btn-wide" style="margin-top:13px" data-action="install-done" data-id="${install.serverId}">${svgIcon('apps')}Установить софт</button>` : ''}</div>`;
      this.modalRoot.innerHTML = this.modalShell(done ? 'Установка завершена' : 'USB Installer', install.os.name, body, { noClose: !done && !install.error });
      const terminal = $('#installTerminal');
      if (terminal) terminal.scrollTop = terminal.scrollHeight;
    }

    updateInstallerDOM() {
      const install = this.installer;
      if (!install) return;
      const fill = $('#installFill');
      const percent = $('#installPercent');
      const terminal = $('#installTerminal');
      if (fill) fill.style.width = `${install.progress}%`;
      if (percent) percent.textContent = `${Math.floor(install.progress)}%`;
      if (terminal) {
        terminal.innerHTML = install.lines.slice(-7).map(line => `<div class="${line.startsWith('ERR') ? 'error-line' : ''}">&gt; ${esc(line)}</div>`).join('');
        terminal.scrollTop = terminal.scrollHeight;
      }
      if (install.progress >= 100) this.renderInstaller();
    }

    renderRoomModal() {
      const current = this.game.room();
      const body = DATA.ROOMS.map((room, index) => {
        const active = room.id === current.id;
        const previousOwned = DATA.ROOMS.findIndex(item => item.id === current.id) >= index;
        return `<article class="card"><div class="room-name"><div class="room-icon">${svgIcon('home')}</div><div><strong>${room.name}</strong><span>${room.subtitle}</span></div></div><div class="room-metrics"><div class="room-metric"><span>АРЕНДА</span><strong>${money(room.rent)}/д</strong></div><div class="room-metric"><span>ТЕМП.</span><strong>${room.ambient}°C</strong></div><div class="room-metric"><span>ШУМ ДО</span><strong>${room.noiseLimit} дБ</strong></div></div><button class="btn ${active ? 'btn-ghost' : 'btn-primary'} btn-wide" style="margin-top:11px" data-action="confirm-room" data-id="${room.id}" ${active || previousOwned ? 'disabled' : ''}>${active ? 'Вы здесь' : previousOwned ? 'Нужен предыдущий апгрейд' : `Переехать за ${money(room.price)}`}</button></article>`;
      }).join('');
      this.modalRoot.innerHTML = this.modalShell('Помещение', 'Больше места, холоднее воздух, выше аренда', body);
    }

    openConfirm(title, text, confirmAction, id, danger = false) {
      this.openModal('confirm', { title, text, confirmAction, id, danger });
    }

    renderConfirm() {
      const data = this.modal;
      const body = `<div class="notice ${data.danger ? 'warning' : ''}">${svgIcon(data.danger ? 'warning' : 'info')}<div>${esc(data.text)}</div></div><div class="btn-row" style="margin-top:15px"><button class="btn btn-ghost" data-action="close-modal">Отмена</button><button class="btn ${data.danger ? 'btn-danger' : 'btn-primary'}" data-action="confirm-action">Подтвердить</button></div>`;
      this.modalRoot.innerHTML = this.modalShell(data.title, '', body, { center: true });
    }

    openOnboarding() { this.openModal('onboarding', {}); }

    renderOnboarding() {
      const body = `<div class="onboarding-logo">${svgIcon('server')}</div><h2 class="onboarding-title">SERVER TYCOON</h2><div class="onboarding-sub">СИСАДМИН: ПЕСОЧНИЦА</div><div class="tutorial-step"><span class="tutorial-number">01</span><div><strong>Соберите и запустите</strong><p>У вас уже есть HP Compaq и 100 000 ₽. Проверьте железо в конструкторе и включите сервер.</p></div></div><div class="tutorial-step"><span class="tutorial-number">02</span><div><strong>Установите ОС и софт</strong><p>В инвентаре лежит USB. Запишите Debian, затем поставьте BOINC, Jellyfin или Local AI.</p></div></div><div class="tutorial-step"><span class="tutorial-number">03</span><div><strong>Не устройте пожар</strong><p>Следите за ваттами, шумом, ОЗУ и температурой. Выше 95°C будет BSOD, а за долги провайдер вырубит свет.</p></div></div><button class="btn btn-primary btn-wide" style="margin-top:12px" data-action="finish-onboarding">${svgIcon('power')}Войти в серверную</button><p class="muted tiny" style="text-align:center;margin-top:12px">Всё работает офлайн · прогресс сохраняется на устройстве</p>`;
      this.modalRoot.innerHTML = this.modalShell('Добро пожаловать', '', body, { center: true, noClose: true });
    }

    updateLiveNodes() {
      this.game.state.servers.forEach(server => {
        const temp = $(`[data-server-temp="${server.id}"]`);
        if (temp) temp.textContent = `${Math.round(server.temp)}°C`;
        const load = $(`[data-server-load="${server.id}"]`);
        if (load) load.style.width = `${clamp(this.game.metrics(server).cpuLoad, 0, 100)}%`;
      });
      if (this.modal && this.modal.type === 'server') {
        const server = this.game.getServer(this.modal.serverId);
        if (!server) return;
        const m = this.game.metrics(server);
        const values = {
          temp: [`${Math.round(server.temp)}°C`, server.temp],
          cpu: [`${Math.round(m.cpuLoad)}%`, m.cpuLoad],
          ram: [`${m.ramUsed.toFixed(1)} / ${m.ramTotal} ГБ`, m.ramPercent],
          watts: [`${Math.round(m.watts)}W · ${Math.round(m.noise)}дБ`, m.watts / this.game.room().powerLimit * 100]
        };
        Object.keys(values).forEach(key => {
          const gauge = $(`[data-live-gauge="${key}"][data-id="${server.id}"]`);
          if (!gauge) return;
          $('.gauge-head strong', gauge).textContent = values[key][0];
          $('.meter i', gauge).style.width = `${clamp(values[key][1], 0, 100)}%`;
        });
      }
    }

    drawVisibleCharts() {
      const finance = $('#financeChart');
      if (finance) this.drawFinanceChart(finance);
      const telemetry = $('#telemetryChart');
      if (telemetry) {
        const server = this.game.getServer(telemetry.dataset.server);
        if (server) this.drawTelemetryChart(telemetry, server);
      }
    }

    prepareCanvas(canvas) {
      const rect = canvas.getBoundingClientRect();
      if (!rect.width || !rect.height) return null;
      const ratio = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.round(rect.width * ratio);
      canvas.height = Math.round(rect.height * ratio);
      const ctx = canvas.getContext('2d');
      ctx.scale(ratio, ratio);
      return { ctx, width: rect.width, height: rect.height };
    }

    drawFinanceChart(canvas) {
      const setup = this.prepareCanvas(canvas);
      if (!setup) return;
      const { ctx, width, height } = setup;
      const data = this.game.state.stats.daily.slice(-10);
      if (!data.length) return;
      const pad = { left: 34, right: 10, top: 15, bottom: 22 };
      const w = width - pad.left - pad.right;
      const h = height - pad.top - pad.bottom;
      const max = Math.max(1000, ...data.map(day => Math.abs(day.profit))) * 1.15;
      ctx.font = '9px monospace';
      ctx.lineWidth = 1;
      for (let i = 0; i <= 4; i += 1) {
        const y = pad.top + h * i / 4;
        ctx.strokeStyle = 'rgba(70,105,128,.18)';
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.stroke();
        const value = max - max * 2 * i / 4;
        ctx.fillStyle = '#647e91'; ctx.textAlign = 'right';
        ctx.fillText(`${Math.round(value / 1000)}k`, pad.left - 5, y + 3);
      }
      const zeroY = pad.top + h / 2;
      const barWidth = Math.max(7, Math.min(24, w / data.length * 0.5));
      data.forEach((day, index) => {
        const x = pad.left + w * (index + 0.5) / data.length;
        const barHeight = Math.abs(day.profit) / max * h / 2;
        ctx.fillStyle = day.profit >= 0 ? 'rgba(85,246,210,.65)' : 'rgba(255,100,124,.65)';
        ctx.fillRect(x - barWidth / 2, day.profit >= 0 ? zeroY - barHeight : zeroY, barWidth, Math.max(2, barHeight));
        ctx.fillStyle = '#647e91'; ctx.textAlign = 'center'; ctx.fillText(`Д${day.day}`, x, height - 6);
      });
      ctx.strokeStyle = 'rgba(170,205,220,.35)'; ctx.beginPath(); ctx.moveTo(pad.left, zeroY); ctx.lineTo(width - pad.right, zeroY); ctx.stroke();
    }

    drawTelemetryChart(canvas, server) {
      const setup = this.prepareCanvas(canvas);
      if (!setup) return;
      const { ctx, width, height } = setup;
      const pad = { left: 25, right: 7, top: 10, bottom: 15 };
      const w = width - pad.left - pad.right;
      const h = height - pad.top - pad.bottom;
      ctx.font = '8px monospace';
      for (let i = 0; i <= 4; i += 1) {
        const y = pad.top + h * i / 4;
        ctx.strokeStyle = 'rgba(70,105,128,.18)';
        ctx.beginPath(); ctx.moveTo(pad.left, y); ctx.lineTo(width - pad.right, y); ctx.stroke();
        ctx.fillStyle = '#647e91'; ctx.textAlign = 'right'; ctx.fillText(String(100 - i * 25), pad.left - 4, y + 3);
      }
      const series = [
        { data: server.history.temp, color: '#56d9ff' },
        { data: server.history.cpu, color: '#55f6d2' },
        { data: server.history.ram, color: '#b692ff' }
      ];
      series.forEach(item => {
        if (item.data.length < 2) return;
        ctx.strokeStyle = item.color; ctx.lineWidth = 1.8; ctx.shadowColor = item.color; ctx.shadowBlur = 5;
        ctx.beginPath();
        item.data.forEach((value, index) => {
          const x = pad.left + w * index / Math.max(47, item.data.length - 1);
          const y = pad.top + h * (1 - clamp(value, 0, 100) / 100);
          if (!index) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke(); ctx.shadowBlur = 0;
      });
      if (!server.history.temp.length) {
        ctx.fillStyle = '#647e91'; ctx.textAlign = 'center'; ctx.fillText('Включите сервер — телеметрия появится здесь', width / 2, height / 2);
      }
    }

    bindDragAndDrop() {
      const serverId = this.modal.serverId;
      $$('.draggable-part', this.modalRoot).forEach(card => {
        card.addEventListener('dragstart', event => {
          event.dataTransfer.setData('text/plain', card.dataset.partUid);
          event.dataTransfer.effectAllowed = 'move';
          card.classList.add('dragging');
        });
        card.addEventListener('dragend', () => card.classList.remove('dragging'));
        card.addEventListener('pointerdown', event => {
          if (event.pointerType === 'mouse') return;
          this.drag = { uid: card.dataset.partUid, startX: event.clientX, startY: event.clientY, active: false, pointerId: event.pointerId, card };
          card.setPointerCapture(event.pointerId);
        });
        card.addEventListener('pointermove', event => {
          if (!this.drag || this.drag.pointerId !== event.pointerId) return;
          const distance = Math.hypot(event.clientX - this.drag.startX, event.clientY - this.drag.startY);
          if (distance > 9 && !this.drag.active) {
            this.drag.active = true;
            const part = this.game.getPart(this.drag.uid);
            const ghost = $('#dragGhost');
            ghost.innerHTML = svgIcon(part.icon);
            ghost.classList.add('active');
            card.classList.add('dragging');
          }
          if (this.drag.active) {
            event.preventDefault();
            const ghost = $('#dragGhost');
            ghost.style.left = `${event.clientX}px`;
            ghost.style.top = `${event.clientY}px`;
            $$('.slot.drag-over', this.modalRoot).forEach(slot => slot.classList.remove('drag-over'));
            const under = document.elementFromPoint(event.clientX, event.clientY);
            const slot = under && under.closest('.slot');
            if (slot) slot.classList.add('drag-over');
          }
        });
        card.addEventListener('pointerup', event => {
          if (!this.drag || this.drag.pointerId !== event.pointerId) return;
          if (this.drag.active) {
            const under = document.elementFromPoint(event.clientX, event.clientY);
            const slot = under && under.closest('.slot');
            if (slot) this.game.installPart(serverId, this.drag.uid, slot.dataset.slot);
            this.suppressClickUntil = Date.now() + 350;
            this.refresh();
          }
          $('#dragGhost').classList.remove('active');
          card.classList.remove('dragging');
          $$('.slot.drag-over', this.modalRoot).forEach(slot => slot.classList.remove('drag-over'));
          this.drag = null;
        });
        card.addEventListener('pointercancel', () => {
          $('#dragGhost').classList.remove('active');
          card.classList.remove('dragging');
          this.drag = null;
        });
      });
      $$('.slot', this.modalRoot).forEach(slot => {
        slot.addEventListener('dragover', event => { event.preventDefault(); slot.classList.add('drag-over'); });
        slot.addEventListener('dragleave', () => slot.classList.remove('drag-over'));
        slot.addEventListener('drop', event => {
          event.preventDefault();
          slot.classList.remove('drag-over');
          const partUid = event.dataTransfer.getData('text/plain');
          this.game.installPart(serverId, partUid, slot.dataset.slot);
          this.refresh();
        });
      });
    }

    handleAction(action, element, event) {
      const id = element.dataset.id;
      if (Date.now() < (this.suppressClickUntil || 0) && action === 'select-part') return;
      switch (action) {
        case 'pause':
          this.game.state.paused = !this.game.state.paused;
          this.game.save(); this.refresh(false); break;
        case 'refresh-market': this.game.refreshListings(); this.refresh(false); break;
        case 'market-filter': this.marketFilter = element.dataset.filter; this.renderView(); break;
        case 'inventory-filter': this.inventoryFilter = element.dataset.filter; this.renderView(); break;
        case 'go-market': this.switchTab('market'); break;
        case 'go-inventory': this.switchTab('inventory'); break;
        case 'bargain': this.game.bargain(id); this.refresh(false); break;
        case 'buy': this.game.buyListing(id); this.refresh(false); break;
        case 'sell-part':
          this.openConfirm('Продать деталь?', 'Деталь уйдёт с полки, а деньги сразу поступят на баланс. Сначала долг будет отправлен провайдеру.', 'sell-part', id); break;
        case 'server-detail': this.openServer(id); break;
        case 'detail-tab': this.modalTab = element.dataset.tabId; this.renderModal(); break;
        case 'new-server': this.openModal('newServer'); break;
        case 'build-with': {
          const server = this.game.createServer(id);
          if (server) this.openServer(server.id, 'hardware');
          else this.refresh();
          break;
        }
        case 'confirm-build': {
          const server = this.game.createServer(id);
          if (server) this.openServer(server.id, 'hardware');
          else this.refresh();
          break;
        }
        case 'power': this.game.togglePower(id); this.refresh(); break;
        case 'open-constructor':
          this.selectedPartUid = null;
          this.openModal('constructor', { serverId: id }); break;
        case 'back-to-server': this.openServer(id, 'hardware'); break;
        case 'select-part':
          this.selectedPartUid = this.selectedPartUid === id ? null : id;
          this.renderModal(); break;
        case 'slot':
          if (this.selectedPartUid) {
            this.game.installPart(id, this.selectedPartUid, element.dataset.slot);
            this.selectedPartUid = null;
            this.refresh();
          }
          break;
        case 'remove-part': event.stopPropagation(); this.game.removePart(id, element.dataset.slot); this.refresh(); break;
        case 'write-image': this.openModal('writeImage', { partUid: id }); break;
        case 'confirm-image':
          this.game.writeImage(id, element.dataset.os);
          this.refresh();
          break;
        case 'os-select': this.openModal('osSelect', { serverId: id }); break;
        case 'start-os': this.startInstaller(id, element.dataset.usb); break;
        case 'retry-install':
          if (this.installer) {
            this.installer.error = false;
            this.installer.lines.push('RETRY_OK · установка продолжена');
            this.renderInstaller();
          }
          break;
        case 'install-done': this.openServer(id, 'software'); this.installer = null; break;
        case 'install-soft': this.game.installSoftware(id, element.dataset.soft); this.refresh(); break;
        case 'uninstall-soft': this.game.uninstallSoftware(id, element.dataset.soft); this.refresh(); break;
        case 'repair': this.game.repairServer(id); this.refresh(); break;
        case 'reboot': this.game.rebootServer(id); this.refresh(); break;
        case 'restore-power': this.game.restorePower(); this.refresh(false); break;
        case 'upgrade-room': this.openModal('room'); break;
        case 'confirm-room':
          if (id === DATA.ROOMS[DATA.ROOMS.findIndex(room => room.id === this.game.state.roomId) + 1]?.id) this.game.upgradeRoom();
          this.closeModal(); this.refresh(false); break;
        case 'mom-help': this.game.momHelp(); this.refresh(false); break;
        case 'cheat': this.game.cheat(Number(element.dataset.amount)); this.refresh(false); break;
        case 'skip-day': this.game.skipDay(); this.refresh(false); break;
        case 'sell-server-confirm':
          this.openConfirm('Продать сервер?', 'Сервер исчезнет из стойки. Цена рассчитана по остаточной стоимости всех установленных деталей.', 'sell-server', id, true); break;
        case 'dismantle-confirm':
          this.openConfirm('Разобрать на детали?', 'Корпус и все комплектующие вернутся в инвентарь. Установленная ОС и софт будут потеряны.', 'dismantle', id, true); break;
        case 'reset-confirm':
          this.openConfirm('Начать заново?', 'Текущая ферма, баланс, инвентарь и статистика будут удалены безвозвратно.', 'reset', '', true); break;
        case 'confirm-action': this.handleConfirmed(this.modal.confirmAction, this.modal.id); break;
        case 'finish-onboarding':
          this.game.state.flags.onboarded = true; this.game.save(); this.closeModal(); this.toast('HP Compaq ждёт. Запишите Debian на USB!', 'success'); break;
        case 'close-modal': this.closeModal(); break;
        default: break;
      }
    }

    handleConfirmed(action, id) {
      this.closeModal();
      if (action === 'sell-part') this.game.sellPart(id);
      else if (action === 'sell-server') this.game.sellServer(id);
      else if (action === 'dismantle') this.game.dismantleServer(id);
      else if (action === 'reset') { this.game.reset(); this.tab = 'farm'; }
      this.refresh(false);
    }
  }

  window.addEventListener('DOMContentLoaded', () => {
    window.serverTycoonUI = new GameUI();
  });
})();
