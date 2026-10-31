/* Ultimate 7.0: локальные PC OS sessions с разными desktop/skin и настоящими командами. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.pcMetrics) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.pcOSUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0);
    const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${Number((value / 1024).toFixed(1))} ГБ` : `${round(value)} МБ`;

    proto.openPCOS = function (id) {
      const machine = this.game.getPC(id);
      if (!machine) return;
      const style = machine.os ? this.game.pcOS(machine.os).style : 'bios';
      this.pcSession = { id, app: style === 'dos' ? 'terminal' : style === 'metro' ? null : 'system', start: false,
        lines: [`${machine.os ? this.game.pcOS(machine.os).name : 'BIOS'} · локальная PC-сессия`, 'Канал управления активен. Введите help.'], history: [], historyIndex: 0, notes: '' };
      this.openModal('pcOS', { pcId: id });
    };
    proto.pcOSPrompt = function (machine) {
      const os = this.game.pcOS(machine.os);
      if (!os || os.style === 'dos') return 'C:\\>';
      if (os.style === 'linux') return 'user@retro-pc:~$';
      return 'PS C:\\>';
    };
    proto.pcWallpaper = function (style) {
      if (style === 'xp') return `<svg viewBox="0 0 800 500" preserveAspectRatio="xMidYMid slice"><defs><linearGradient id="xpSky" x2="0" y2="1"><stop stop-color="#1684df"/><stop offset="1" stop-color="#b8e8ff"/></linearGradient><linearGradient id="xpHill" x2="0" y2="1"><stop stop-color="#7ec835"/><stop offset="1" stop-color="#28750e"/></linearGradient></defs><rect width="800" height="500" fill="url(#xpSky)"/><g fill="#fff" opacity=".8"><ellipse cx="160" cy="105" rx="80" ry="22"/><ellipse cx="590" cy="72" rx="105" ry="25"/></g><path d="M0 345Q185 165 410 335T800 280V500H0Z" fill="url(#xpHill)"/><path d="M0 405Q250 275 500 405T800 360V500H0Z" fill="#4e9d1d"/></svg>`;
      if (style === 'aero') return `<svg viewBox="0 0 800 500" preserveAspectRatio="xMidYMid slice"><defs><radialGradient id="aeroBg"><stop stop-color="#197aa6"/><stop offset=".55" stop-color="#073e6d"/><stop offset="1" stop-color="#020d26"/></radialGradient><linearGradient id="aeroLine"><stop stop-color="#5ef4e0" stop-opacity="0"/><stop offset=".5" stop-color="#8fe9ff"/><stop offset="1" stop-color="#9682ff" stop-opacity="0"/></linearGradient></defs><rect width="800" height="500" fill="url(#aeroBg)"/><g fill="none" stroke="url(#aeroLine)" opacity=".8"><path d="M-80 410Q200 40 860 180" stroke-width="42"/><path d="M-40 470Q280 100 880 260" stroke-width="12"/><path d="M60 520Q360 170 830 350" stroke-width="4"/></g><circle cx="640" cy="115" r="90" fill="#57c7ff" opacity=".06"/></svg>`;
      if (style === 'win10') return `<svg viewBox="0 0 800 500" preserveAspectRatio="xMidYMid slice"><defs><linearGradient id="w10bg" x2="1" y2="1"><stop stop-color="#0067b8"/><stop offset="1" stop-color="#002d57"/></linearGradient></defs><rect width="800" height="500" fill="url(#w10bg)"/><g transform="translate(325 145)" fill="#8ed9ff" opacity=".7"><path d="M0 20l65-9v88H0zm75-11l80-11v101H75zM0 109h65v88l-65-9zm75 0h80v101l-80-11z"/></g></svg>`;
      if (style === 'win11') return `<svg viewBox="0 0 800 500" preserveAspectRatio="xMidYMid slice"><defs><radialGradient id="w11a"><stop stop-color="#88d8ff"/><stop offset=".5" stop-color="#3767c7"/><stop offset="1" stop-color="#071a46"/></radialGradient><linearGradient id="w11b" x2="1" y2="1"><stop stop-color="#8b6ee8"/><stop offset=".5" stop-color="#3b86d9"/><stop offset="1" stop-color="#4ad6c8"/></linearGradient></defs><rect width="800" height="500" fill="url(#w11a)"/><path d="M430 40C650 20 770 180 655 280S355 350 350 480H710C820 290 865 90 620 0Z" fill="url(#w11b)" opacity=".75"/><path d="M380 500C330 350 520 320 570 225S520 70 450 0H230C410 140 390 210 280 295S180 430 205 500Z" fill="#67bfe9" opacity=".25"/></svg>`;
      if (style === 'linux') return `<svg viewBox="0 0 800 500" preserveAspectRatio="xMidYMid slice"><defs><linearGradient id="ubg" x2="1" y2="1"><stop stop-color="#3c0f38"/><stop offset=".55" stop-color="#8b3157"/><stop offset="1" stop-color="#e87532"/></linearGradient></defs><rect width="800" height="500" fill="url(#ubg)"/><g fill="none" stroke="#fff" opacity=".12"><circle cx="590" cy="250" r="170" stroke-width="50"/><circle cx="590" cy="250" r="90" stroke-width="3"/><path d="M0 440L280 160l140 140L800 40" stroke-width="4"/></g></svg>`;
      return '';
    };
    proto.pcStartMenu = function (machine, os) {
      if (!this.pcSession.start) return '';
      const apps = [['system','stats',os.style === 'win11' ? 'Настройки' : 'Системный монитор'],['files','files',os.style === 'classic' ? 'Мой компьютер' : 'Проводник'],['tests','gpu','Игровые тесты'],['terminal','os',os.style === 'linux' ? 'Терминал' : 'Командная строка']];
      return `<div class="pc-start-menu"><div class="pc-start-user"><span>${os.style === 'linux' ? 'U' : 'A'}</span><div><strong>${os.style === 'linux' ? 'user' : 'Администратор'}</strong><small>${esc(machine.name)}</small></div></div><div class="pc-start-apps">${apps.map(([app,icon,label]) => `<button data-action="pc-os-app" data-app="${app}">${svgIcon(icon)}<span>${label}</span></button>`).join('')}</div><div class="pc-start-bottom"><button data-action="pc-os-reboot">${svgIcon('refresh')}Перезагрузка</button><button data-action="pc-os-power">${svgIcon('power')}Завершение работы</button></div></div>`;
    };
    proto.pcAppWindow = function (machine, os) {
      const app = this.pcSession.app; if (!app) return '';
      const m = this.game.pcMetrics(machine);
      if (app === 'terminal') return `<div class="pc-app-window terminal-window"><div class="pc-window-title"><span>${svgIcon('os')} ${os.style === 'linux' ? 'Terminal' : os.style === 'dos' ? 'MS-DOS Prompt' : 'Windows PowerShell'}</span><button data-action="pc-os-close-app">×</button></div>${this.pcTerminal(machine, os)}</div>`;
      let title = 'Системный монитор'; let content = '';
      if (app === 'files') {
        title = os.style === 'classic' ? 'Мой компьютер' : os.style === 'linux' ? 'Files' : 'Проводник';
        content = `<div class="pc-file-toolbar"><button>←</button><button>→</button><span>${os.style === 'dos' ? 'C:\\' : 'Этот компьютер'}</span></div><div class="pc-drive"><span>${svgIcon('hdd')}</span><div><strong>Локальный диск (C:)</strong><div class="pc-drive-meter"><i style="width:${Math.min(82, (os.minStorageMb / machine.storageMb) * 100)}%"></i></div><small>${mb(os.minStorageMb)} занято системой · ${mb(machine.storageMb)} всего · ${machine.storageType}</small></div></div><div class="pc-folder-grid">${['Документы','Драйверы','Диагностика','Игры'].map(name => `<button data-action="pc-os-app" data-app="${name === 'Игры' ? 'tests' : name === 'Диагностика' ? 'system' : 'files'}">${svgIcon('files')}<span>${name}</span></button>`).join('')}</div>`;
      } else if (app === 'tests') {
        title = 'Game Benchmark Library';
        const results = Object.values(machine.benchmarks);
        content = `<div class="pc-app-pad"><h3>Игровые эксперименты</h3><p>11 сценариев от POST Stability до Cyberpunk 2077. Нагрузка, температура и FPS рассчитывает симулятор.</p>${results.length ? results.slice(-5).reverse().map(result => `<div class="pc-os-result"><span>${esc(DATA.PC_BENCHMARKS[result.benchmarkId].name)}</span><strong>${result.fps}${result.benchmarkId === 'stability' ? ' pts' : ' FPS'}</strong><b class="${result.passed ? 'good' : 'bad'}">${result.passed ? 'PASS' : 'FAIL'}</b></div>`).join('') : '<div class="pc-os-empty">Тестов ещё не было.</div>'}<button class="pc-native-button accent" data-action="pc-os-benchmarks">Открыть все тесты</button></div>`;
      } else {
        title = os.style === 'classic' ? 'Свойства системы' : os.style === 'linux' ? 'System Monitor' : os.style === 'win11' ? 'Настройки · Система' : 'Диспетчер задач';
        content = `<div class="pc-app-tabs"><button class="active">Производительность</button><button data-action="pc-os-app" data-app="tests">Тесты</button><button data-action="pc-os-app" data-app="files">Устройство</button></div><div class="pc-native-metrics"><div><span>ЦП</span><strong data-pc-live="cpu">${round(m.cpuLoad)}%</strong><div><i style="width:${m.cpuLoad}%"></i></div><small>${esc(machine.cpu)}</small></div><div><span>Память</span><strong data-pc-live="ram">${round(m.ramPercent)}%</strong><div><i style="width:${m.ramPercent}%"></i></div><small>${mb(m.ramUsedMb)} / ${mb(machine.ramMb)}</small></div><div><span>GPU</span><strong>${round(m.gpuLoad)}%</strong><div><i style="width:${m.gpuLoad}%"></i></div><small>${esc(machine.gpu)}</small></div><div><span>Электропитание</span><strong data-pc-live="watts">${round(m.watts)} Вт</strong><div><i style="width:${Math.min(100, m.watts / machine.psuWatts * 100)}%"></i></div><small>PSU ${machine.psuWatts}W · <b data-pc-live="temp">${round(machine.temp)}°C</b></small></div></div><div class="pc-device-spec"><span>Устройство</span><strong>${esc(machine.name)}</strong><small>Процессор ${esc(machine.cpu)}<br>ОЗУ ${mb(machine.ramMb)} (${machine.ramType})<br>Система ${esc(os.name)}<br>Компьютер ${machine.year} года</small></div>`;
      }
      return `<div class="pc-app-window"><div class="pc-window-title"><span>${svgIcon(app === 'files' ? 'files' : app === 'tests' ? 'gpu' : 'stats')} ${title}</span><div><button data-action="pc-os-close-app">—</button><button data-action="pc-os-close-app">×</button></div></div><div class="pc-window-content">${content}</div><div class="pc-window-status">${machine.experimentalOS ? 'EXPERIMENTAL OS · PERFORMANCE ×' + machine.osPenalty.toFixed(2) : 'LOCAL PC · NO NETWORK'}<span>${round(m.noise)} дБ</span></div></div>`;
    };
    proto.pcTerminal = function (machine, os) {
      return `<div class="pc-terminal ${os.style === 'dos' ? 'dos' : ''}"><div class="pc-terminal-output" data-pc-terminal-output>${this.pcSession.lines.slice(-80).map(line => `<div>${esc(line)}</div>`).join('')}</div><form class="pc-terminal-form" data-pc-terminal-form><span>${esc(this.pcOSPrompt(machine))}</span><input type="text" autocomplete="off" autocapitalize="off" spellcheck="false" data-pc-terminal-input aria-label="Команда PC terminal" placeholder="help"><button type="submit">RUN</button></form><div class="pc-terminal-quick">${['help','status','tests','scan','bench stability'].map(command => `<button data-action="pc-os-command" data-command="${command}">${command}</button>`).join('')}</div></div>`;
    };
    proto.pcDesktop = function (machine, os) {
      const style = os.style;
      if (style === 'dos') return `<div class="pc-dos-desktop"><div class="pc-dos-header">${esc(os.name)}<br>Copyright (C) 1990–1994<br><br>Memory: ${mb(machine.ramMb)} · HDD ${mb(machine.storageMb)}<br>Type help for local simulator commands.<br></div>${this.pcTerminal(machine, os)}</div>`;
      const icons = [['system','stats',style === 'classic' ? 'Свойства системы' : 'Монитор'],['files','files',style === 'classic' ? 'Мой компьютер' : 'Проводник'],['tests','gpu','Игры и тесты'],['terminal','os',style === 'linux' ? 'Терминал' : 'Командная строка']];
      if (style === 'metro') return `<div class="pc-os-desktop skin-metro"><div class="pc-metro-head"><strong>Пуск</strong><span>Администратор · ${esc(machine.name)}</span></div><div class="pc-metro-tiles">${icons.map(([app,icon,label], index) => `<button class="tile-${index}" data-action="pc-os-app" data-app="${app}">${svgIcon(icon)}<span>${label}</span></button>`).join('')}<button class="tile-wide" data-action="pc-os-benchmarks">${svgIcon('stats')}<span>Benchmarks</span><b>${Object.keys(machine.benchmarks).length} результатов</b></button></div>${this.pcAppWindow(machine, os)}<div class="pc-metro-clock" data-pc-live="clock">${this.game.pcClock()}</div></div>`;
      return `<div class="pc-os-desktop skin-${style}"><div class="pc-os-wallpaper">${this.pcWallpaper(style)}</div>${style === 'linux' ? `<div class="pc-linux-topbar"><span>Activities</span><b>${esc(os.name)}</b><span data-pc-live="clock">${this.game.pcClock()}</span></div><div class="pc-linux-dock">${icons.map(([app,icon,label]) => `<button data-action="pc-os-app" data-app="${app}" title="${label}">${svgIcon(icon)}</button>`).join('')}<button data-action="pc-os-power">${svgIcon('power')}</button></div>` : ''}<div class="pc-desktop-icons">${icons.map(([app,icon,label]) => `<button data-action="pc-os-app" data-app="${app}">${svgIcon(icon)}<span>${label}</span></button>`).join('')}</div>${machine.experimentalOS ? `<div class="pc-experimental-watermark">Experimental OS<br>${machine.year} hardware · performance ×${machine.osPenalty.toFixed(2)}</div>` : ''}${this.pcAppWindow(machine, os)}${this.pcStartMenu(machine, os)}${style !== 'linux' ? `<div class="pc-taskbar"><button class="pc-start-button ${this.pcSession.start ? 'active' : ''}" data-action="pc-os-start">${style === 'win11' ? svgIcon('apps') : brandSvg('windows')} ${style !== 'win11' ? '<span>Пуск</span>' : ''}</button><div class="pc-task-buttons"><button data-action="pc-os-app" data-app="files">${svgIcon('files')}</button><button data-action="pc-os-app" data-app="system">${svgIcon('stats')}</button><button data-action="pc-os-app" data-app="terminal">${svgIcon('os')}</button></div><div class="pc-task-tray"><span>${round(machine.temp)}°C</span><span data-pc-live="clock">${this.game.pcClock()}</span><button data-action="pc-os-power" aria-label="Выключить">${svgIcon('power')}</button></div></div>` : ''}</div>`;
    };
    proto.pcOSFault = function (machine) {
      if (machine.crashed) return `<div class="pc-bsod"><div>:(</div><h2>Ваш ПК перегрелся и был остановлен</h2><p>THERMAL_SHUTDOWN_AT_95C<br>Температура: ${round(machine.temp)}°C</p><button data-action="pc-reboot" data-id="${machine.id}">Перезагрузить после ремонта</button><button data-action="pc-os-back">Вернуться к ремонту</button></div>`;
      if (!machine.powered) return `<div class="pc-no-signal"><i></i><h2>NO SIGNAL</h2><p>Компьютер выключен. Состояние сохранено.</p><button data-action="pc-power" data-id="${machine.id}">${svgIcon('power')}Включить ПК</button><button data-action="pc-os-back">К мастерской</button></div>`;
      return `<div class="pc-bios"><strong>AMIBIOS / UEFI LOCAL SETUP</strong><p>CPU: ${esc(machine.cpu)}<br>Memory Test: ${machine.ramMb}K OK<br>Boot drive: ${machine.storageType} ${mb(machine.storageMb)}</p><h2>NO BOOTABLE DEVICE</h2><button data-action="pc-os-install" data-id="${machine.id}">Установить операционную систему</button><button data-action="pc-os-back">К мастерской</button></div>`;
    };
    proto.renderPCOS = function () {
      const machine = this.game.getPC(this.modal.pcId); if (!machine) { this.closeModal(); return; }
      if (!this.pcSession || this.pcSession.id !== machine.id) { this.openPCOS(machine.id); return; }
      const os = machine.os ? this.game.pcOS(machine.os) : null;
      const usable = machine.powered && os && !machine.crashed;
      const content = !usable ? this.pcOSFault(machine) : this.pcDesktop(machine, os);
      const body = `<div class="pc-os-session"><div class="pc-os-monitor"><div class="pc-monitor-brand">${os ? esc(os.name) : 'BIOS'}<span>LOCAL SESSION</span></div>${content}</div><div class="pc-os-footer"><span>${esc(machine.cpu)} · ${mb(machine.ramMb)} RAM</span><button data-action="pc-os-back" data-id="${machine.id}">Вернуться в мастерскую</button></div></div>`;
      this.modalRoot.innerHTML = this.modalShell(machine.name, `${os ? os.name : 'FIRMWARE'} // PC CONTROL ROOM`, body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('pc-os-sheet', `os-${os ? os.style : 'bios'}`);
      requestAnimationFrame(() => { const output = $('[data-pc-terminal-output]', this.modalRoot); if (output) output.scrollTop = output.scrollHeight; });
    };
    proto.runPCOSCommand = function (command) {
      const machine = this.game.getPC(this.modal.pcId); if (!machine || !this.pcSession) return;
      const result = this.game.executePCCommand(machine.id, command);
      this.pcSession.lines.push(`${this.pcOSPrompt(machine)} ${command}`);
      if (!result.clear) this.pcSession.lines.push(...result.lines); else this.pcSession.lines = [];
      this.pcSession.history.push(command); this.pcSession.historyIndex = this.pcSession.history.length;
      if (String(command).trim() === 'exit') { this.openPC(machine.id); return; }
      this.processEvents(); this.renderModal();
    };

    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () { if (this.modal && this.modal.type === 'pcOS') { this.renderPCOS(); return; } return baseRenderModal.call(this); };
    const baseCloseModal = proto.closeModal;
    proto.closeModal = function () { if (this.modal && ['pcOS', 'pcDetail'].includes(this.modal.type) && this.pcSoundscape) this.pcSoundscape.stop(); return baseCloseModal.call(this); };
    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const own = ['pc-os-app','pc-os-start','pc-os-close-app','pc-os-power','pc-os-reboot','pc-os-back','pc-os-benchmarks','pc-os-install','pc-os-command'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'pc-os-app': this.pcSession.app = element.dataset.app; this.pcSession.start = false; this.renderModal(); return;
        case 'pc-os-start': this.pcSession.start = !this.pcSession.start; this.renderModal(); return;
        case 'pc-os-close-app': this.pcSession.app = null; this.renderModal(); return;
        case 'pc-os-power': this.game.togglePCPower(this.modal.pcId); this.refresh(); return;
        case 'pc-os-reboot': this.game.rebootPC(this.modal.pcId); this.pcSession.lines.push('Reboot requested by user'); this.refresh(); return;
        case 'pc-os-back': this.openPC(this.modal.pcId); return;
        case 'pc-os-benchmarks': this.openModal('pcBenchmarks', { pcId: this.modal.pcId }); return;
        case 'pc-os-install': { const machine = this.game.getPC(element.dataset.id); if (machine && machine.powered) this.game.togglePCPower(machine.id); this.openModal('pcOSInstall', { pcId: element.dataset.id }); return; }
        case 'pc-os-command': this.runPCOSCommand(element.dataset.command); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };
    document.addEventListener('submit', event => {
      if (!event.target.matches('[data-pc-terminal-form]')) return;
      event.preventDefault(); const input = $('[data-pc-terminal-input]', event.target); const command = input.value.trim(); if (command) ui.runPCOSCommand(command);
    });
    document.addEventListener('keydown', event => {
      if (!event.target.matches('[data-pc-terminal-input]') || !ui.pcSession) return;
      if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
        event.preventDefault(); const direction = event.key === 'ArrowUp' ? -1 : 1;
        ui.pcSession.historyIndex = Math.max(0, Math.min(ui.pcSession.history.length, ui.pcSession.historyIndex + direction));
        event.target.value = ui.pcSession.history[ui.pcSession.historyIndex] || '';
      }
    });
    const baseOnTick = proto.onTick;
    proto.onTick = function () {
      const wasUsable = this.modal && this.modal.type === 'pcOS' ? (() => { const pc = this.game.getPC(this.modal.pcId); return pc && pc.powered && !pc.crashed; })() : false;
      const result = baseOnTick.call(this);
      if (this.modal && this.modal.type === 'pcOS') {
        const machine = this.game.getPC(this.modal.pcId);
        if (machine) {
          const metrics = this.game.pcMetrics(machine);
          if (this.pcSoundscape) { this.pcSoundscape.sync(machine, metrics); this.pcSoundscape.tick(machine, metrics); }
          const usable = machine.powered && !machine.crashed;
          if (wasUsable !== usable) this.renderModal();
          else {
            ['cpu','ram','watts','temp'].forEach(key => { const node = $(`[data-pc-live="${key}"]`, this.modalRoot); if (node) node.textContent = key === 'temp' ? `${round(machine.temp)}°C` : key === 'watts' ? `${round(metrics.watts)} Вт` : `${round(metrics[key === 'cpu' ? 'cpuLoad' : 'ramPercent'])}%`; });
            const clock = $('[data-pc-live="clock"]', this.modalRoot); if (clock) clock.textContent = this.game.pcClock();
          }
        }
      } else if (this.modal && this.modal.type === 'pcDetail' && this.pcSoundscape) {
        const machine = this.game.getPC(this.modal.pcId);
        if (machine) { const metrics = this.game.pcMetrics(machine); this.pcSoundscape.sync(machine, metrics); this.pcSoundscape.tick(machine, metrics); }
      } else if (this.pcSoundscape) this.pcSoundscape.stop();
      return result;
    };
    proto.pcOSUILoaded = true;
  });
})();
