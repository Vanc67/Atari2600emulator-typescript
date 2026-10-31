/* Time Machine 5.0 + Secret Debug Console UI. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.timelineInfo) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.timelineUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const memory = value => value < 1 ? `${Math.round(value * 1024)} МБ` : `${Number(value.toFixed(1))} ГБ`;
    const disk = value => value >= 1000 ? `${Number((value / 1000).toFixed(1))} ТБ` : memory(value);
    const money = value => `${Math.round(value).toLocaleString('ru-RU')} ₽`;
    ui.selectedStartYear = ui.game.state.startYear || 2026;
    ui.debugFilter = 'all';

    proto.starterPreview = function (year) {
      const template = this.game.starterTemplateForYear(year);
      const cpu = DATA.part(template.cpu);
      const ram = template.ram.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const storage = template.disks.reduce((sum, id) => sum + DATA.part(id).capacity, 0);
      const servers = DATA.SERVER_TEMPLATES.filter(item => (item.year || 2020) <= year).length;
      const os = DATA.OS_LIST.filter(item => (item.year || 2020) <= year).length;
      return `<div class="starter-preview"><div class="starter-preview-icon">${svgIcon(DATA.part(template.chassis).icon)}</div><div><strong>${esc(template.name)}</strong><span>${esc(cpu.name)} · ${memory(ram)} RAM · ${disk(storage)}</span><small>${servers} серверов и ${os} ОС доступно на старте · 100 000 ₽</small></div></div>`;
    };

    proto.renderOnboarding = function () {
      const year = this.selectedStartYear;
      const presets = [1990, 1995, 2000, 2005, 2010, 2015, 2020, 2026];
      const body = `<div class="onboarding-logo timeline-logo">${svgIcon('clock')}</div><h2 class="onboarding-title">SERVER TYCOON 5.0</h2><div class="onboarding-sub">TIME MACHINE // СИСАДМИН: ПЕСОЧНИЦА</div><div class="timeline-intro">Выберите год старта. Каждые 12 игровых дней наступает новый год: железо, ОС и Авито эпохи обновляются, а вы получаете очки исследований.</div><div class="year-picker"><button data-action="start-year-step" data-step="-1" aria-label="Год назад">−</button><div><span>ВАШ СТАРТОВЫЙ ГОД</span><strong data-start-year-value>${year}</strong></div><button data-action="start-year-step" data-step="1" aria-label="Год вперёд">+</button></div><input class="year-slider" type="range" min="1990" max="2026" step="1" value="${year}" data-start-year aria-label="Стартовый год"><div class="year-presets">${presets.map(item => `<button class="${item === year ? 'active' : ''}" data-action="start-year" data-year="${item}">${item}</button>`).join('')}</div><div data-starter-preview>${this.starterPreview(year)}</div><div class="timeline-rules"><span>01 <b>Соберите сервер и установите ОС эпохи</b></span><span>02 <b>Развивайтесь до 2026 года</b></span><span>03 <b>Следите за RAM, температурой и шумом</b></span></div><button class="btn btn-primary btn-wide" data-action="finish-onboarding">${svgIcon('power')}Начать в ${year} году</button><p class="muted tiny timeline-offline">Всё работает офлайн · прогресс сохраняется на устройстве</p>`;
      this.modalRoot.innerHTML = this.modalShell('Time Machine', 'ВЫБОР ЭПОХИ', body, { center: true, noClose: true });
    };

    proto.timelineCard = function () {
      const info = this.game.timelineInfo();
      const yearLabel = info.currentYear >= 2026 ? `2026 · R&D через ${info.nextIn} дн.` : `${info.currentYear} · через ${info.nextIn} дн. новый год`;
      return `<div class="card timeline-card"><div class="timeline-card-head"><div><span>TIME MACHINE</span><strong>${yearLabel}</strong></div><button class="btn btn-small btn-ghost" data-action="open-research">Research · ${info.research.points}</button></div><div class="meter timeline-meter"><i style="width:${info.currentYear >= 2026 ? 100 : info.progress}%"></i></div><div class="timeline-card-meta"><span>${info.availableServers}/${info.totalServers} серверов</span><span>${info.availableOS}/${info.totalOS} ОС</span><span>12 дней = 1 год</span></div></div>`;
    };
    const baseFarmView = proto.farmView;
    proto.farmView = function () {
      const html = baseFarmView.call(this);
      const marker = '<div class="section-label"><span>Стоечные юниты';
      return html.includes(marker) ? html.replace(marker, this.timelineCard() + marker) : this.timelineCard() + html;
    };

    proto.renderResearch = function () {
      const info = this.game.timelineInfo();
      const research = info.research;
      const definitions = {
        cooling: ['Криогенный контур', '+5% охлаждения за уровень', 'cooler'],
        efficiency: ['Энергоэффективность', '−4% потребления за уровень', 'bolt'],
        marketing: ['Продажи хостинга', '+4% дохода за уровень', 'stats']
      };
      const nextYear = Math.min(2026, info.currentYear + 1);
      const unlocks = DATA.SERVER_TEMPLATES.filter(template => template.year === nextYear).length;
      let body = `<div class="research-hero"><span>START ${info.startYear}</span><strong>${info.currentYear}</strong><p>${info.currentYear >= 2026 ? 'Достигнута современность. Весь архив открыт, исследования продолжаются.' : `До ${nextYear} года: ${info.nextIn} игровых дней · откроется ${unlocks} конфигураций.`}</p><div class="meter"><i style="width:${info.currentYear >= 2026 ? 100 : info.progress}%"></i></div></div><div class="research-summary"><div><span>СЕРВЕРЫ</span><strong>${info.availableServers}</strong></div><div><span>ДЕТАЛИ</span><strong>${info.availableParts}</strong></div><div><span>ОС</span><strong>${info.availableOS}</strong></div><div><span>ОЧКИ</span><strong>${research.points}</strong></div></div><div class="section-label"><span>Исследования</span><span>завершено лет: ${research.yearsCompleted}</span></div>`;
      body += Object.entries(definitions).map(([kind, [name, effect, icon]]) => {
        const level = research[kind];
        const cost = level + 1;
        return `<article class="card research-card"><div class="research-card-head"><span>${svgIcon(icon)}</span><div><strong>${name}</strong><small>${effect}</small></div><b>${level}/5</b></div><div class="research-pips">${Array.from({ length: 5 }, (_, index) => `<i class="${index < level ? 'active' : ''}"></i>`).join('')}</div><button class="btn btn-small ${level >= 5 ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="buy-research" data-kind="${kind}" ${level >= 5 || research.points < cost ? 'disabled' : ''}>${level >= 5 ? 'Максимальный уровень' : `Исследовать · ${cost} RP`}</button></article>`;
      }).join('');
      body += `<p class="muted tiny">+1 RP каждый новый год. Стартовый запас: 1 RP. Бонусы применяются к реальной симуляции и сохраняются.</p>`;
      this.modalRoot.innerHTML = this.modalShell('Time Machine & Research', 'РАЗВИТИЕ ТЕХНОЛОГИЙ', body);
    };

    const baseFilteredCatalog = proto.filteredCatalog;
    proto.filteredCatalog = function () {
      const year = this.game.currentYear();
      return baseFilteredCatalog.call(this).filter(template => (template.year || 2020) <= year);
    };

    proto.renderWriteImage = function () {
      const usb = this.game.getPart(this.modal.partUid);
      if (!usb) { this.closeModal(); return; }
      const systems = this.game.availableOS();
      const body = `<div class="notice era-notice">${svgIcon('clock')}<div><strong>${this.game.currentYear()} год · ${systems.length} образов доступно</strong>Будущие ОС появятся с развитием Time Machine. Для старых машин выбирайте системы своей эпохи.</div></div><div class="notice" style="margin:10px 0 13px">${svgIcon('usb')}<div><strong>${esc(usb.name)}</strong>${usb.image ? `Сейчас записан ${DATA.os(usb.image).name}. Новый образ заменит старый.` : 'Флешка останется в инвентаре и сможет устанавливать ОС повторно.'}</div></div><div class="os-grid">${systems.map(os => `<button class="os-card ${usb.image === os.id ? 'selected' : ''}" data-action="confirm-image" data-id="${usb.uid}" data-os="${os.id}">${svgIcon(os.icon)}<strong>${esc(os.name)}</strong><span>${os.year} · ${esc(os.description)}<br>RAM от ${memory(os.minRam)} · диск от ${disk(os.minDisk)}</span><span class="${os.price ? 'amber' : 'mint'}" style="margin-top:8px">${os.price ? `Лицензия ${money(os.price)}` : 'БЕСПЛАТНО'}</span></button>`).join('')}</div>`;
      this.modalRoot.innerHTML = this.modalShell('Записать образ', `TIME MACHINE // ${this.game.currentYear()}`, body);
    };

    proto.openDebug = function () {
      const debug = this.game.debug || window.tycoonDebugOf(this.game);
      debug.enabled = true;
      debug.log('debug', 'debug', 'DEBUG_UI_OPENED', { filter: this.debugFilter, uiTab: this.tab, modal: this.modal && this.modal.type });
      this.openModal('debug');
    };
    proto.debugEntryText = function (entry) {
      if (entry.data == null) return '';
      const text = typeof entry.data === 'string' ? entry.data : JSON.stringify(entry.data, null, 2);
      return text.length > 6000 ? `${text.slice(0, 6000)}\n… truncated in view; full data in Copy Log` : text;
    };
    proto.renderDebug = function () {
      const debug = this.game.debug;
      if (!debug) return;
      const old = $('.debug-stream', this.modalRoot);
      const scroll = old ? { top: old.scrollTop, bottom: old.scrollHeight - old.scrollTop - old.clientHeight < 80 } : { top: 0, bottom: true };
      const filtered = debug.entries.filter(entry => {
        if (this.debugFilter === 'all') return true;
        if (this.debugFilter === 'error') return entry.level === 'error' || entry.level === 'warn';
        if (this.debugFilter === 'event') return ['event', 'game-log'].includes(entry.category);
        return entry.category === this.debugFilter;
      });
      const visible = filtered.slice(-180);
      const filters = [['all','Все'],['server','Серверы'],['action','Действия'],['event','События'],['command','Команды'],['save','Saves'],['error','Ошибки']];
      const body = `<div class="debug-toolbar"><div><span>SECRET // LOCAL ONLY</span><strong>${debug.entries.length} entries · #${debug.sequence}</strong></div><span class="badge ${debug.paused ? 'info' : 'good'}">${debug.paused ? 'PAUSED' : 'LIVE'}</span></div><div class="debug-filters">${filters.map(([id,label]) => `<button class="${this.debugFilter === id ? 'active' : ''}" data-action="debug-filter" data-filter="${id}">${label}</button>`).join('')}</div><div class="debug-actions"><button data-action="debug-pause">${debug.paused ? 'Resume' : 'Pause'}</button><button data-action="debug-snapshot">Snapshot</button><button data-action="debug-copy">Copy log</button><button data-action="debug-clear">Clear</button></div><div class="debug-stream">${visible.map(entry => `<article class="debug-entry level-${entry.level}"><div class="debug-entry-head"><span>#${entry.id} · ${entry.at.toFixed(0)}ms · D${entry.day} ${entry.year}</span><b>${esc(entry.category)} / ${esc(entry.level)}</b></div><strong>${esc(entry.message)}</strong>${entry.data == null ? '' : `<pre>${esc(this.debugEntryText(entry))}</pre>`}</article>`).join('') || '<div class="debug-empty">Буфер пуст. Возобновите запись или выполните действие.</div>'}</div><div class="debug-status">YEAR ${this.game.currentYear()} · DAY ${this.game.state.day} · DOM ${document.querySelectorAll('*').length} · SAVE ${JSON.stringify(this.game.state).length} B · AUDIO ${window.tycoonSoundscape ? window.tycoonSoundscape.sessions.size : 0}</div>`;
      this.modalRoot.innerHTML = this.modalShell('Secret Debug Console', 'VERBOSE RING BUFFER // НИЧЕГО НЕ ОТПРАВЛЯЕТСЯ', body);
      const sheet = $('.modal-sheet', this.modalRoot);
      if (sheet) sheet.classList.add('debug-sheet');
      const stream = $('.debug-stream', this.modalRoot);
      if (stream) stream.scrollTop = scroll.bottom ? stream.scrollHeight : scroll.top;
    };
    proto.copyDebugLog = function () {
      const text = this.game.debug.exportText();
      const fallback = () => {
        const area = document.createElement('textarea');
        area.value = text; area.style.position = 'fixed'; area.style.opacity = '0';
        document.body.appendChild(area); area.select();
        try { document.execCommand('copy'); } catch (_) { /* WebView может запретить clipboard */ }
        area.remove();
        this.toast('Лог подготовлен в буфер обмена', 'success');
      };
      if (navigator.clipboard && navigator.clipboard.writeText) navigator.clipboard.writeText(text).then(() => this.toast('Debug log скопирован', 'success')).catch(fallback);
      else fallback();
    };

    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () {
      if (this.modal && this.modal.type === 'research') { this.renderResearch(); return; }
      if (this.modal && this.modal.type === 'debug') { this.renderDebug(); return; }
      return baseRenderModal.call(this);
    };
    const baseOnTick = proto.onTick;
    proto.onTick = function () {
      const result = baseOnTick.call(this);
      if (this.modal && this.modal.type === 'debug') this.renderDebug();
      return result;
    };
    const baseExecuteSessionCommand = proto.executeSessionCommand;
    proto.executeSessionCommand = function (serverId, command) {
      const result = baseExecuteSessionCommand.call(this, serverId, command);
      if (String(command).trim().toLowerCase() === 'debug') this.openDebug();
      return result;
    };

    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const debug = this.game.debug;
      if (debug && debug.enabled) debug.log('trace', 'ui', 'UI_ACTION', { action, id: element.dataset.id || null,
        year: element.dataset.year || null, kind: element.dataset.kind || null, soft: element.dataset.soft || null });
      switch (action) {
        case 'start-year': this.selectedStartYear = Number(element.dataset.year); this.renderOnboarding(); return;
        case 'start-year-step': this.selectedStartYear = Math.max(1990, Math.min(2026, this.selectedStartYear + Number(element.dataset.step))); this.renderOnboarding(); return;
        case 'finish-onboarding':
          this.game.startTimeline(this.selectedStartYear);
          this.game.state.flags.onboarded = true;
          this.game.save(); this.closeModal();
          this.toast(`Time Machine: старт в ${this.game.currentYear()} году`, 'success');
          this.refresh(false); return;
        case 'open-research': this.openModal('research'); return;
        case 'buy-research': this.game.buyResearch(element.dataset.kind); this.refresh(); return;
        case 'debug-filter': this.debugFilter = element.dataset.filter; this.renderDebug(); return;
        case 'debug-pause': this.game.debug.paused = !this.game.debug.paused; this.renderDebug(); return;
        case 'debug-snapshot': this.game.openDebugSnapshot(); this.renderDebug(); return;
        case 'debug-copy': this.copyDebugLog(); return;
        case 'debug-clear': this.game.debug.clear(); this.renderDebug(); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };

    const baseHandleConfirmed = proto.handleConfirmed;
    proto.handleConfirmed = function (action, id) {
      const result = baseHandleConfirmed.call(this, action, id);
      if (action === 'reset') { this.selectedStartYear = 2026; this.openOnboarding(); }
      return result;
    };

    document.addEventListener('input', event => {
      if (event.target.matches('[data-start-year]')) {
        ui.selectedStartYear = Number(event.target.value);
        const label = $('[data-start-year-value]');
        if (label) label.textContent = event.target.value;
      }
    });
    document.addEventListener('change', event => {
      if (event.target.matches('[data-start-year]')) ui.renderOnboarding();
    });

    let logoTaps = 0;
    let firstTap = 0;
    const logo = $('.brand-mark');
    if (logo) logo.addEventListener('click', () => {
      const time = Date.now();
      if (time - firstTap > 4000) logoTaps = 0;
      if (!logoTaps) firstTap = time;
      logoTaps += 1;
      if (logoTaps >= 5) { logoTaps = 0; ui.openDebug(); }
    });

    proto.timelineUILoaded = true;
    ui.render();
    if (ui.modal) ui.renderModal();
  });
})();
