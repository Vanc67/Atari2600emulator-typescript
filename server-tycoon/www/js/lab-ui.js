/* Control Room 4.0: интерактивные ОС, AI Model Manager, Torture Lab и звук. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !window.DATA.AI_MODELS) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.labUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const memory = value => value < 1 ? `${Math.round(value * 1024)} МБ` : `${Number(value.toFixed(1))} ГБ`;
    const clamp = (value, min, max) => Math.max(min, Math.min(max, value));

    ui.soundscape = new ServerSoundscape(ui.game, ui.audio);
    window.tycoonSoundscape = ui.soundscape;
    ui.session = null;

    const baseOnTick = proto.onTick;
    proto.onTick = function () {
      baseOnTick.call(this);
      if (this.soundscape) this.soundscape.sync();
      if (this.modal && this.modal.type === 'session') this.updateSessionLive();
    };
    const baseRefresh = proto.refresh;
    proto.refresh = function (includeModal = true) {
      const result = baseRefresh.call(this, includeModal);
      if (this.soundscape) this.soundscape.sync();
      return result;
    };

    proto.sessionSkin = function (server) {
      const family = DATA.os(server.os).family;
      if (family === 'windows') return 'windows';
      if (family === 'nas') return 'nas';
      if (family === 'network') return 'network';
      if (family === 'hypervisor') return 'hypervisor';
      if (family === 'legacy') return 'legacy';
      if (family === 'unix') return 'unix';
      if (family === 'bsd') return 'bsd';
      return 'linux';
    };
    proto.sessionPrompt = function (server) {
      const os = DATA.os(server.os);
      const host = server.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'server';
      return os.family === 'windows' ? 'PS C:\\>' : `root@${host}:~#`;
    };

    proto.controlRoomCard = function (server) {
      const running = server.powered && server.os && !server.crashed && !server.hung && !server.broken;
      const model = server.aiModel && DATA.AI_MODELS[server.aiModel];
      const stress = server.stress && DATA.STRESS_TESTS[server.stress.id];
      let html = `<div class="card control-room-card"><h3 class="card-title"><span>${svgIcon('os')}Control Room 4.0</span><span class="badge ${running ? 'good' : ''}">${running ? 'LIVE' : 'OFFLINE'}</span></h3><p class="muted tiny">Запустите ОС: desktop, web-панель или терминал зависят от системы. Команды управляют реальным состоянием сервера.</p><button class="btn btn-cyan btn-wide" data-action="open-session" data-id="${server.id}" ${running ? '' : 'disabled'}>${svgIcon('os')}Запустить интерактивную систему</button>`;
      html += `<div class="control-ai-row"><div><span class="control-label">LOCAL AI</span><strong>${model ? esc(model.name) : 'Модель не выбрана'}</strong><small>${model ? `${memory(model.ram)} RAM · ${model.cpu}% CPU` : 'от 0,5B до 70B · вход от 1 ГБ RAM'}</small></div><button class="btn btn-small btn-ghost" data-action="open-ai" data-id="${server.id}">Выбрать</button></div>`;
      html += `<div class="lab-heading"><span>TORTURE LAB</span><span>${stress ? esc(stress.name) : 'доход во время теста = 0'}</span></div>`;
      if (stress) html += `<div class="notice warning lab-active">${svgIcon('warning')}<div><strong>${esc(stress.name)} идёт</strong>Нагрузка настоящая: следите за температурой, RAM, питанием и шумом.</div></div><button class="btn btn-amber btn-wide" data-action="stop-stress" data-id="${server.id}">Остановить эксперимент</button>`;
      else {
        html += `<div class="stress-grid">${Object.values(DATA.STRESS_TESTS).map(profile => `<button class="stress-button" data-action="start-stress" data-id="${server.id}" data-profile="${profile.id}" ${running ? '' : 'disabled'}>${svgIcon(profile.icon)}<strong>${esc(profile.name)}</strong><span>${esc(profile.description)}</span></button>`).join('')}</div>`;
      }
      const sounds = this.game.state.settings.serverSounds;
      html += `<button class="btn btn-small btn-ghost btn-wide sound-inline" data-action="toggle-server-sounds">${svgIcon('noise')}Звук стойки: ${sounds ? 'включён' : 'выключен'}</button></div>`;
      return html;
    };

    const baseOverview = proto.overviewTab;
    proto.overviewTab = function (server) {
      return baseOverview.call(this, server) + this.controlRoomCard(server);
    };
    const baseSoftware = proto.softwareTab;
    proto.softwareTab = function (server) {
      const model = server.aiModel && DATA.AI_MODELS[server.aiModel];
      const panel = `<div class="notice ai-notice">${svgIcon('chip')}<div><strong>Local AI Model Manager</strong>${model ? `Активна ${esc(model.name)} · ${memory(model.ram)} RAM.` : 'Скачайте лёгкую 0,5B/3B или тяжёлую 14B/70B модель.'}<button class="btn btn-small btn-cyan ai-open" data-action="open-ai" data-id="${server.id}">Открыть Model Manager</button></div></div>`;
      return panel + baseSoftware.call(this, server);
    };

    const baseAudioView = proto.audioView;
    proto.audioView = function () {
      return baseAudioView.call(this) + this.soundscapeView();
    };
    proto.soundscapeView = function () {
      const settings = this.game.state.settings;
      const active = this.game.state.servers.filter(server => server.powered).length;
      return `<div class="card"><h3 class="card-title"><span>${svgIcon('noise')}Server Soundscape</span><span class="badge ${settings.serverSounds ? 'good' : ''}">${settings.serverSounds ? `${active} LIVE` : 'MUTED'}</span></h3><p class="muted tiny">Отдельный синтез для каждой машины: обороты вентиляторов, HDD, возраст железа и текущая нагрузка. Не использует сеть или аудиофайлы.</p><label class="volume-row">${svgIcon('noise')}<input type="range" min="0" max="1" step="0.01" value="${settings.serverVolume}" data-server-volume aria-label="Громкость серверов"><span data-server-volume-label>${Math.round(settings.serverVolume * 100)}%</span></label><button class="btn ${settings.serverSounds ? 'btn-danger' : 'btn-primary'} btn-wide" data-action="toggle-server-sounds">${settings.serverSounds ? 'Выключить звуки серверов' : 'Включить звуки серверов'}</button><p class="muted tiny">Работает независимо от музыки и SFX. При сворачивании все аудиоузлы останавливаются.</p></div>`;
    };

    proto.openAIModels = function (serverId) {
      if (!this.game.getServer(serverId)) return;
      this.openModal('aiModels', { serverId });
    };
    proto.renderAIModels = function () {
      const server = this.game.getServer(this.modal.serverId);
      if (!server) { this.closeModal(); return; }
      const ram = this.game.hardwareReport(server).ramTotal;
      const running = server.powered && server.os && !server.crashed && !server.hung;
      let body = `<div class="notice">${svgIcon('chip')}<div><strong>${esc(server.name)} · ${memory(ram)} RAM</strong>Выбор применяется сразу. Размер — имитируемая локальная загрузка, интернет из APK не нужен.</div></div>`;
      if (server.stress) body += `<div class="notice warning ai-warning">${svgIcon('warning')}<div>Сначала остановите Torture Lab: он владеет выбранной моделью.</div></div>`;
      body += `<div class="ai-model-list">${Object.values(DATA.AI_MODELS).map(model => {
        const active = server.aiModel === model.id && server.software.includes('ollama');
        const allowed = running && !server.stress && ram >= model.minRam;
        return `<article class="ai-model ${active ? 'active' : ''}"><div class="ai-model-head"><div><strong>${esc(model.name)}</strong><span>${esc(model.description)}</span></div><span class="badge ${active ? 'good' : 'info'}">${active ? 'ACTIVE' : model.size}</span></div><div class="ai-model-stats"><span>RAM <b>${memory(model.ram)}</b></span><span>CPU <b>${model.cpu}%</b></span><span>Мин. <b>${model.minRam} ГБ</b></span><span>Доход <b>${model.rate}+ ₽/ч</b></span></div><button class="btn btn-small ${active ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="download-ai" data-id="${server.id}" data-model="${model.id}" ${allowed && !active ? '' : 'disabled'}>${active ? 'Модель активна' : ram < model.minRam ? `Нужно ${model.minRam} ГБ RAM` : `Загрузить ${model.size}`}</button></article>`;
      }).join('')}</div><button class="btn btn-ghost btn-wide" data-action="session-back" data-id="${server.id}">Вернуться к серверу</button>`;
      this.modalRoot.innerHTML = this.modalShell('Local AI Model Manager', 'OFFLINE WEIGHTS // CPU + OPTIONAL GPU', body);
    };

    proto.openSession = function (serverId) {
      const server = this.game.getServer(serverId);
      if (!server || !server.powered || !server.os || server.crashed || server.hung || server.broken) {
        this.game.notify('Система недоступна: проверьте питание и аварии', 'warning');
        this.processEvents();
        return;
      }
      const skin = this.sessionSkin(server);
      const desktop = ['windows', 'nas', 'network', 'hypervisor'].includes(skin);
      this.session = { serverId, skin, mode: desktop ? 'desktop' : 'terminal', lines: [`${DATA.os(server.os).name} · Control Room session`, 'Канал управления симулятором активен. Введите help.'], history: [], historyIndex: 0, draft: '' };
      this.openModal('session', { serverId });
    };

    proto.renderSessionFault = function (server) {
      if (server.crashed) return `<div class="os-fault bsod"><div class="bsod-face">:(</div><h2>Система остановлена</h2><p>Температура достигла критических 95°C. Железо и охлаждение сохранены в симуляторе.</p><code>STOP: THERMAL_SHUTDOWN_AT_95C</code><button class="btn btn-primary" data-action="session-repair" data-id="${server.id}">Отремонтировать охлаждение</button>${server.stress ? `<button class="btn btn-ghost" data-action="session-stop-stress" data-id="${server.id}">Снять стресс-нагрузку</button>` : ''}</div>`;
      if (server.hung) return `<div class="os-fault oom"><div class="fault-code">KERNEL PANIC</div><h2>OUT OF MEMORY</h2><p>RAM заполнена на 100%. OOM-killer не справился, система зависла.</p><code>ramPressure → 60 · power → OFF</code>${server.stress ? `<button class="btn btn-amber" data-action="session-stop-stress" data-id="${server.id}">Остановить эксперимент</button>` : ''}<button class="btn btn-primary" data-action="session-reboot" data-id="${server.id}">Перезагрузить</button></div>`;
      if (server.broken) return `<div class="os-fault no-signal"><h2>POST FAILED</h2><p>Материнская плата не отвечает.</p><button class="btn btn-primary" data-action="session-repair" data-id="${server.id}">Диагностика и ремонт</button></div>`;
      if (!server.powered) return `<div class="os-fault no-signal"><div class="standby-led"></div><h2>НЕТ СИГНАЛА</h2><p>${this.game.state.powerOut ? 'Электричество в помещении отключено.' : 'Сервер выключен. Данные и состояние сохранены.'}</p><button class="btn btn-primary" data-action="session-power" data-id="${server.id}" ${this.game.state.powerOut ? 'disabled' : ''}>${svgIcon('power')}Включить сервер</button></div>`;
      return `<div class="os-fault no-signal"><h2>NO BOOTABLE DEVICE</h2><p>Установите ОС с загрузочной флешки.</p></div>`;
    };

    proto.renderSessionDesktop = function (server) {
      const m = this.game.metrics(server);
      const skin = this.session.skin;
      const dashboard = skin !== 'windows';
      const tiles = [
        ['stats', 'Мониторинг', 'status'], ['apps', 'Службы', 'services'], ['chip', 'Local AI', 'ai'],
        ['warning', 'Torture Lab', 'stress'], ['os', 'Терминал', '']
      ];
      return `<div class="os-desktop ${dashboard ? 'dashboard' : 'windows-desktop'}"><div class="desktop-brand">${brandSvg(DATA.os(server.os).brand || 'server')}<div><strong>${esc(DATA.os(server.os).name)}</strong><span>${dashboard ? 'WEB CONTROL PANEL' : 'SERVER DESKTOP'}</span></div></div><div class="desktop-metrics"><div><span>CPU</span><strong data-session-live="cpu">${Math.round(m.cpuLoad)}%</strong></div><div><span>RAM</span><strong data-session-live="ram">${Math.round(m.ramPercent)}%</strong></div><div><span>TEMP</span><strong data-session-live="temp">${Math.round(server.temp)}°</strong></div><div><span>POWER</span><strong data-session-live="watts">${Math.round(m.watts)}W</strong></div></div><div class="desktop-icons">${tiles.map(([icon, label, command]) => `<button data-action="session-command" data-id="${server.id}" data-command="${command}">${svgIcon(icon)}<span>${label}</span></button>`).join('')}</div>${this.session.notice ? `<div class="desktop-notice">${esc(this.session.notice)}</div>` : ''}<div class="desktop-taskbar"><button data-action="session-mode" data-mode="desktop" class="start-button">${brandSvg(DATA.os(server.os).brand || 'server')}<span>ПУСК</span></button><span data-session-live="clock">${this.game.clock()}</span><button data-action="session-reboot" data-id="${server.id}" aria-label="Перезагрузить">${svgIcon('refresh')}</button><button data-action="session-shutdown" data-id="${server.id}" aria-label="Выключить">${svgIcon('power')}</button></div></div>`;
    };

    proto.renderSessionTerminal = function (server) {
      const prompt = this.sessionPrompt(server);
      return `<div class="os-terminal"><div class="terminal-output" data-terminal-output>${this.session.lines.map(line => `<div class="term-line">${esc(line)}</div>`).join('')}</div><form class="terminal-form" data-terminal-form><span>${esc(prompt)}</span><input type="text" inputmode="text" autocomplete="off" autocapitalize="off" spellcheck="false" data-terminal-input value="${esc(this.session.draft)}" aria-label="Команда терминала" placeholder="help"><button class="btn btn-small btn-primary" type="submit">RUN</button></form><div class="terminal-quick">${['status','services','free','df','ai','stress'].map(command => `<button type="button" data-action="session-command" data-id="${server.id}" data-command="${command}">${command}</button>`).join('')}</div></div>`;
    };

    proto.renderSession = function () {
      const server = this.game.getServer(this.modal.serverId);
      if (!server) { this.closeModal(); return; }
      if (!this.session || this.session.serverId !== server.id) {
        const skin = server.os ? this.sessionSkin(server) : 'linux';
        this.session = { serverId: server.id, skin, mode: 'terminal', lines: ['Сессия восстановлена'], history: [], historyIndex: 0, draft: '' };
      }
      const oldInput = $('[data-terminal-input]', this.modalRoot);
      const restoreFocus = oldInput && document.activeElement === oldInput;
      if (oldInput) this.session.draft = oldInput.value;
      const os = server.os ? DATA.os(server.os) : null;
      const usable = server.powered && server.os && !server.crashed && !server.hung && !server.broken;
      let content = '';
      if (!usable) content = this.renderSessionFault(server);
      else if (this.session.mode === 'desktop') content = this.renderSessionDesktop(server);
      else content = this.renderSessionTerminal(server);
      const body = `<div class="os-session skin-${this.session.skin}"><div class="os-window"><div class="os-window-bar"><span class="os-window-logo">${os ? brandSvg(os.brand || 'server') : svgIcon('os')}</span><strong>${os ? esc(os.name) : 'BIOS'}</strong><span class="os-live-dot ${usable ? 'online' : ''}"></span><div class="os-window-actions"><button data-action="session-mode" data-mode="desktop" ${usable ? '' : 'disabled'}>Desktop</button><button data-action="session-mode" data-mode="terminal" ${usable ? '' : 'disabled'}>Shell</button></div></div>${content}</div><div class="session-footer"><span>${server.stress ? `TORTURE: ${esc(DATA.STRESS_TESTS[server.stress.id].name)}` : 'LOCAL SESSION · NO NETWORK'}</span><button data-action="session-back" data-id="${server.id}">К серверу</button></div></div>`;
      this.modalRoot.innerHTML = this.modalShell(server.name, `${server.chassis.name} · CONTROL ROOM 4.0`, body);
      const sheet = $('.modal-sheet', this.modalRoot);
      if (sheet) sheet.classList.add('session-sheet');
      const output = $('[data-terminal-output]', this.modalRoot);
      if (output) output.scrollTop = output.scrollHeight;
      if (restoreFocus) {
        const input = $('[data-terminal-input]', this.modalRoot);
        if (input) { input.focus(); input.setSelectionRange(input.value.length, input.value.length); }
      }
    };

    proto.updateSessionLive = function () {
      if (!this.session) return;
      const server = this.game.getServer(this.session.serverId);
      if (!server || !server.powered) return;
      const m = this.game.metrics(server);
      const values = { cpu: `${Math.round(m.cpuLoad)}%`, ram: `${Math.round(m.ramPercent)}%`, temp: `${Math.round(server.temp)}°`, watts: `${Math.round(m.watts)}W`, clock: this.game.clock() };
      Object.entries(values).forEach(([key, value]) => $$('[data-session-live="' + key + '"]', this.modalRoot).forEach(node => { node.textContent = value; }));
    };

    proto.executeSessionCommand = function (serverId, command) {
      const server = this.game.getServer(serverId);
      if (!server || !this.session) return;
      this.session.mode = 'terminal';
      if (!command) { this.session.notice = ''; this.renderSession(); return; }
      const result = this.game.executeCommand(serverId, command);
      if (result.clear) this.session.lines = [];
      else {
        this.session.lines.push(`${this.sessionPrompt(server)} ${command}`, ...result.lines);
        this.session.lines = this.session.lines.slice(-90);
      }
      this.session.history.push(command);
      this.session.history = this.session.history.slice(-30);
      this.session.historyIndex = this.session.history.length;
      this.session.draft = '';
      this.session.notice = result.lines[0] || '';
      this.processEvents();
      this.renderHUD();
      if (this.soundscape) this.soundscape.sync();
      if (result.close) this.openServer(serverId); else this.renderSession();
    };

    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () {
      if (this.modal && this.modal.type === 'session') { this.renderSession(); return; }
      if (this.modal && this.modal.type === 'aiModels') { this.renderAIModels(); return; }
      return baseRenderModal.call(this);
    };

    const baseCloseModal = proto.closeModal;
    proto.closeModal = function () {
      if (this.modal && this.modal.type === 'session') this.session = null;
      return baseCloseModal.call(this);
    };

    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      const own = ['open-session','open-ai','download-ai','start-stress','stop-stress','toggle-server-sounds','session-mode','session-command','session-back','session-power','session-reboot','session-repair','session-shutdown','session-stop-stress'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'open-session': this.openSession(id); return;
        case 'open-ai': this.openAIModels(id); return;
        case 'download-ai': this.game.installAIModel(id, element.dataset.model); this.refresh(); return;
        case 'start-stress': this.game.startStress(id, element.dataset.profile); this.refresh(); return;
        case 'stop-stress': this.game.stopStress(id); this.refresh(); return;
        case 'toggle-server-sounds': this.soundscape.toggle(); this.refresh(); return;
        case 'session-mode': if (this.session) { this.session.mode = element.dataset.mode; this.session.notice = ''; this.renderSession(); } return;
        case 'session-command': this.executeSessionCommand(id, element.dataset.command || ''); return;
        case 'session-back': this.openServer(id); return;
        case 'session-power': this.game.togglePower(id); this.refresh(); return;
        case 'session-reboot': this.game.restartServer(id); this.refresh(); return;
        case 'session-repair': this.game.repairServer(id); this.refresh(); return;
        case 'session-shutdown': { const server = this.game.getServer(id); if (server) { this.game.powerOff(server); this.game.save(); } this.refresh(); return; }
        case 'session-stop-stress': this.game.stopStress(id); this.refresh(); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };

    document.addEventListener('submit', event => {
      if (!event.target.matches('[data-terminal-form]')) return;
      event.preventDefault();
      const input = $('[data-terminal-input]', event.target);
      if (input && ui.modal && ui.modal.type === 'session') ui.executeSessionCommand(ui.modal.serverId, input.value);
    });
    document.addEventListener('input', event => {
      if (event.target.matches('[data-server-volume]')) {
        ui.soundscape.setVolume(event.target.value);
        const label = $('[data-server-volume-label]');
        if (label) label.textContent = `${Math.round(Number(event.target.value) * 100)}%`;
      }
      if (event.target.matches('[data-terminal-input]') && ui.session) ui.session.draft = event.target.value;
    });
    document.addEventListener('keydown', event => {
      if (!event.target.matches('[data-terminal-input]') || !ui.session) return;
      if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
        event.preventDefault();
        const history = ui.session.history;
        ui.session.historyIndex = clamp(ui.session.historyIndex + (event.key === 'ArrowUp' ? -1 : 1), 0, history.length);
        event.target.value = history[ui.session.historyIndex] || '';
        ui.session.draft = event.target.value;
      }
    });
    document.addEventListener('visibilitychange', () => {
      if (!ui.soundscape) return;
      if (document.hidden) ui.soundscape.stopAll(); else ui.soundscape.sync();
    });

    proto.labUILoaded = true;
    ui.render();
    if (ui.modal) ui.renderModal();
  });
})();
