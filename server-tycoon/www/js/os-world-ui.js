/* OS World UI 8.3: настоящие offline-приложения внутри существующих PC/server OS. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI; if (!ui || !ui.game.osWorld) return;
    const proto = Object.getPrototypeOf(ui); if (proto.osWorldUILoaded) return;
    document.title = 'Server Tycoon 8.3: OS World, Offline Games & Services';
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0);
    const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${Number((value / 1024).toFixed(1))} ГБ` : `${round(value)} МБ`;
    const pcApps = ['browser','downloads','games','worldfiles','processes','power'];
    const serverApps = ['browser','packages','downloads','worldfiles','services','processes'];
    const appNames = { browser:'Offline Browser', downloads:'Download Manager', games:'My Games · FPS', packages:'Package Manager', worldfiles:'File Manager', processes:'Processes', services:'Services', power:'Power & Performance' };
    const appIcons = { browser:'network', downloads:'disk', games:'play', packages:'apps', worldfiles:'files', processes:'stats', services:'server', power:'power' };
    const context = view => {
      if (!view.modal) return null;
      if (view.modal.type === 'pcOS' && view.pcSession) { const machine = view.game.getPC(view.modal.pcId); return machine ? { family:'pc', machine, session:view.pcSession } : null; }
      if (view.modal.type === 'session' && view.session) { const machine = view.game.getServer(view.modal.serverId); return machine ? { family:'server', machine, session:view.session } : null; }
      return null;
    };
    const navButton = (url, icon, label, description) => `<button type="button" class="ow-site-link" data-action="world-browser-nav" data-url="${esc(url)}">${svgIcon(icon)}<span><strong>${esc(label)}</strong><small>${esc(description)}</small></span><b>→</b></button>`;
    const speedOptions = selected => Object.values(DATA.OS_WORLD_SPEEDS).map(speed => `<option value="${speed.id}" ${selected === speed.id ? 'selected' : ''}>${esc(speed.name)}</option>`).join('');

    proto.worldDownloadState = function (machine, family, packageId) {
      const state = this.game.osWorld(machine, family); return state.downloads.find(item => item.packageId === packageId && item.status !== 'canceled');
    };
    proto.worldStore = function (machine, family, session) {
      const state = this.game.osWorld(machine, family); const query = session.worldPackageQuery || '';
      const packages = family === 'pc' ? DATA.OS_WORLD_GAMES.filter(pkg => !query || `${pkg.name} ${pkg.description}`.toLowerCase().includes(query.toLowerCase())) : this.game.osWorldServerPackages(machine.id, query, true);
      const shown = family === 'server' ? packages.slice(0, 30) : packages;
      return `<div class="ow-store-toolbar"><form data-world-package-form><input type="search" data-world-package-query value="${esc(query)}" placeholder="${family === 'pc' ? 'Найти игру…' : 'Поиск среди 2 046 пакетов: nginx, docker…'}" aria-label="Поиск пакетов"><button class="pc-native-button accent" type="submit">Найти</button></form><label><span>Скорость</span><select data-world-default-speed aria-label="Скорость новых загрузок">${speedOptions(state.defaultSpeed)}</select></label></div><div class="ow-storage-line"><span>${family === 'pc' ? 'Offline Game Store' : `Совместимо: ${packages.length}`}</span><span>Свободно ${mb(this.game.osWorldStorage(machine, family).freeMb)}</span></div><div class="ow-package-list">${shown.map(pkg => {
        const download = this.worldDownloadState(machine, family, pkg.id); const installed = family === 'server' ? machine.software.includes(pkg.id) : Boolean(state.games[pkg.id]);
        return `<article class="ow-package-card"><div class="ow-package-icon">${svgIcon(family === 'pc' ? 'play' : pkg.icon || 'apps')}</div><div class="ow-package-copy"><div><strong>${esc(pkg.name)}</strong><span>${pkg.year}</span></div><p>${esc(pkg.description)}</p><small>${mb(pkg.sizeMb)}${pkg.minRamMb || pkg.minRam ? ` · RAM от ${mb(pkg.minRamMb || pkg.minRam)}` : ''}${pkg.kind === 'service' ? ' · настоящая служба' : ' · 16 FPS samples'}</small></div><div class="ow-package-action">${installed ? '<span class="ow-installed">✓ Установлено</span>' : download ? `<button type="button" class="pc-native-button" data-action="world-open-downloads">${download.status === 'complete' ? 'Готово к установке' : 'В Download Manager'}</button>` : `<button type="button" class="pc-native-button accent" data-action="world-download" data-package="${esc(pkg.id)}">${svgIcon('disk')}Скачать</button>`}</div></article>`;
      }).join('') || '<div class="ow-empty">Пакеты не найдены. Измените запрос.</div>'}</div>${family === 'server' && packages.length > shown.length ? `<p class="ow-footnote">Показаны первые ${shown.length}. Уточните поиск — в каталоге ничего не удалено.</p>` : ''}`;
    };
    proto.worldDownloads = function (machine, family) {
      const state = this.game.osWorld(machine, family); const storage = this.game.osWorldStorage(machine, family);
      const statusNames = { downloading:'Загрузка', paused:'Пауза', complete:'Скачано', installed:'Установлено', canceled:'Отменено · место освобождено' };
      return `<div class="ow-download-summary"><div><span>Локальный диск</span><strong>${mb(storage.freeMb)} свободно</strong></div><div class="ow-storage-meter"><i style="width:${storage.capacityMb ? Math.min(100, storage.usedMb / storage.capacityMb * 100) : 0}%"></i></div><small>${mb(storage.usedMb)} / ${mb(storage.capacityMb)} · пакеты ${mb(storage.packagesMb)}</small></div><div class="ow-download-list">${state.downloads.slice().reverse().map(item => {
        const pkg = this.game.osWorldPackage(family, item.packageId); const percent = item.totalMb ? item.progressMb / item.totalMb * 100 : 0; const active = ['downloading','paused'].includes(item.status);
        return `<article class="ow-download-row" data-world-download-row="${esc(item.id)}" data-status="${item.status}"><div class="ow-download-head"><div><strong>${esc(pkg.name)}</strong><small>${esc(item.id)} · ${mb(item.totalMb)}</small></div><span data-world-status>${statusNames[item.status]}</span></div><div class="ow-progress"><i data-world-progress style="width:${percent}%"></i></div><div class="ow-download-meta"><span data-world-progress-label>${round(item.progressMb)} / ${round(item.totalMb)} МБ · ${percent.toFixed(1)}%</span><b data-world-rate>${item.status === 'downloading' ? `${Number(item.actualMbps.toFixed(1))} Мбит/с` : ''}</b></div>${active ? `<label class="ow-row-speed"><span>Профиль</span><select data-world-download-speed data-id="${esc(item.id)}">${speedOptions(item.speed)}</select></label>` : ''}<div class="ow-row-actions">${item.status === 'downloading' ? `<button type="button" class="pc-native-button" data-action="world-download-action" data-command="pause" data-id="${esc(item.id)}">Пауза</button>` : ''}${item.status === 'paused' ? `<button type="button" class="pc-native-button accent" data-action="world-download-action" data-command="resume" data-id="${esc(item.id)}">Продолжить</button>` : ''}${item.status === 'complete' ? `<button type="button" class="pc-native-button accent" data-action="world-install" data-id="${esc(item.id)}">${svgIcon('check')}Установить</button>` : ''}${['downloading','paused','complete'].includes(item.status) ? `<button type="button" class="pc-native-button danger" data-action="world-download-action" data-command="cancel" data-id="${esc(item.id)}">Отмена</button>` : ''}${item.status === 'installed' && family === 'pc' ? `<button type="button" class="pc-native-button accent" data-action="world-play" data-package="${esc(item.packageId)}">${svgIcon('play')}Играть</button>` : ''}</div></article>`;
      }).join('') || '<div class="ow-empty">Загрузок пока нет. Откройте магазин и выберите скорость.</div>'}</div><button type="button" class="pc-native-button" data-action="world-open-store">${svgIcon('market')}Открыть магазин пакетов</button>`;
    };
    proto.worldBrowser = function (machine, family, session) {
      const state = this.game.osWorld(machine, family); const url = state.browser.url; const site = DATA.OS_WORLD_SITES[family][url]; let content = '';
      if (!site) content = `<div class="ow-offline-error">${svgIcon('network')}<h3>Адрес недоступен offline</h3><p>«${esc(url)}» не найден в локальной сети. APK не выполняет внешние запросы.</p>${navButton('home.local','home','Домашняя страница','Вернуться к локальным сайтам')}</div>`;
      else if (url === 'home.local') content = `<div class="ow-web-hero"><span>LOCAL NETWORK · ${family === 'pc' ? 'PC' : 'SERVER'}</span><h2>${esc(site.title)}</h2><p>${esc(site.description)}</p></div><div class="ow-site-grid">${navButton(family === 'pc' ? 'games.local' : 'repo.local','market',family === 'pc' ? 'Games Store' : 'Package Repository',family === 'pc' ? '10 игр · скачать · установить · FPS' : '2 046 пакетов · службы и репозитории')}${navButton('downloads.local','disk','Download Manager','Пауза, продолжение, скорость и установка')}${navButton('docs.local','files','Документация','Команды и руководство offline')}${family === 'pc' ? navButton('avito.local','inventory','PC Авито','24 440 деталей · прямая ссылка в игровой маркет') : navButton('services.local','server','Service Manager','Запуск, остановка и restart служб')}${family === 'server' ? navButton('status.local','stats','Server Status','Реальные метрики и установленные ID') : ''}</div>`;
      else if (url === 'games.local' || url === 'repo.local' || url === 'packages.local') content = `<div class="ow-web-title"><span>${esc(site.description)}</span><h2>${esc(site.title)}</h2></div>${this.worldStore(machine, family, session)}`;
      else if (url === 'downloads.local') content = `<div class="ow-web-title"><span>RESUMABLE LOCAL PACKAGES</span><h2>Download Manager</h2></div>${this.worldDownloads(machine, family)}`;
      else if (url === 'docs.local' || url === 'handbook.local') content = `<div class="ow-doc"><h2>${esc(site.title)}</h2><p>Браузер хранит URL, Back/Forward и закладки отдельно на этой машине. Загрузки идут по игровому tick и останавливаются без питания или при аварии.</p><h3>Как установить</h3><ol><li>Откройте ${family === 'pc' ? 'games.local' : 'repo.local'}.</li><li>Выберите Slow / Normal / Fast / Fiber и скачайте пакет.</li><li>Дождитесь 100%, нажмите «Установить».</li><li>${family === 'pc' ? 'Запустите игру в My Games: FPS зависит от CPU, GPU, RAM, storage, ОС и power plan.' : 'Откройте Services: start/stop/restart меняют нагрузку и доход.'}</li></ol><h3>Shell</h3><code>${family === 'pc' ? 'store · download doom fiber · downloads · play doom · plan eco' : 'packages nginx · fetch nginx fast · services · stop nginx · start nginx'}</code><p>Файлы сохраняются в local save. Отмена загрузки освобождает зарезервированное место, но оставляет запись в истории.</p></div>`;
      else if (url === 'avito.local') content = `<div class="ow-web-hero"><span>PC AVITO · EXISTING GAME MARKET</span><h2>Детали ПК с Авито</h2><p>24 440 компонентов, прямой поиск, цена, сокет, годы и склад. Откроется существующий игровой маркет без сети.</p><button type="button" class="pc-native-button accent" data-action="pc-open-pc-market">${svgIcon('market')}Перейти на PC Авито</button></div>`;
      else if (url === 'services.local') content = `<div class="ow-web-title"><span>INSTALLED SERVICE IDS</span><h2>Service Manager</h2></div>${this.worldServices(machine)}`;
      else if (url === 'status.local') { const m = this.game.metrics(machine); content = `<div class="ow-web-title"><span>LIVE SIMULATION</span><h2>${esc(machine.name)}</h2></div><div class="ow-metric-grid"><div><span>CPU</span><strong>${round(m.cpuLoad)}%</strong></div><div><span>RAM</span><strong>${round(m.ramPercent)}%</strong></div><div><span>Доход</span><strong>${round(this.game.hourlyServerRevenue(machine))} ₽/ч</strong></div><div><span>Сервисы</span><strong>${this.game.osWorldProcesses(machine, 'server').filter(process => process.kind === 'service' && process.status !== 'stopped').length}</strong></div></div><pre class="ow-status-pre">${esc(machine.software.join('\n') || 'Нет установленных служб')}</pre>`; }
      return `<div class="ow-browser"><div class="ow-browser-chrome"><button type="button" data-action="world-browser-nav" data-direction="back" ${state.browser.back.length ? '' : 'disabled'} aria-label="Назад">←</button><button type="button" data-action="world-browser-nav" data-direction="forward" ${state.browser.forward.length ? '' : 'disabled'} aria-label="Вперёд">→</button><form data-world-browser-form><span>offline://</span><input data-world-browser-url value="${esc(url)}" aria-label="Адрес локального сайта" autocomplete="off" spellcheck="false"><button type="submit">GO</button></form><button type="button" class="${state.bookmarks.includes(url) ? 'active' : ''}" data-action="world-bookmark" aria-label="Закладка">★</button></div><div class="ow-bookmarks">${state.bookmarks.map(bookmark => `<button type="button" data-action="world-browser-nav" data-url="${esc(bookmark)}">★ ${esc(bookmark)}</button>`).join('')}</div><div class="ow-webpage">${content}</div><div class="ow-browser-status"><span>OFFLINE · 0 внешних запросов</span><span>${esc(url)}</span></div></div>`;
    };
    proto.worldGames = function (machine) {
      const state = this.game.osWorld(machine, 'pc'); const entries = Object.entries(state.games); const scores = this.game.pcPerformanceScores(machine);
      return `<div class="ow-game-hardware"><span>${esc(machine.cpu)}</span><span>${esc(machine.gpu)}</span><span>${mb(machine.ramMb)} · ${machine.storageType}</span><b>${DATA.OS_WORLD_POWER_PLANS[state.powerPlan].name}</b></div>${entries.length ? `<div class="ow-game-list">${entries.map(([id, game]) => { const def = DATA.PC_BENCHMARKS[id]; const result = machine.benchmarks[id]; const samples = result && result.samples || []; const max = Math.max(60, ...samples); return `<article class="ow-game-card"><div class="ow-game-title"><div>${svgIcon('play')}<span><strong>${esc(def.name)}</strong><small>${def.year} · target ${def.target} FPS · ${game.launches} запусков</small></span></div><b class="${game.running ? 'good' : ''}">${game.running ? 'RUNNING' : 'INSTALLED'}</b></div><div class="ow-fps-result"><div><span>Последний FPS</span><strong>${game.lastFps || '—'}</strong></div><div><span>Лучший FPS</span><strong>${game.bestFps || '—'}</strong></div><div><span>Вердикт</span><strong class="${game.lastPassed ? 'good' : ''}">${game.launches ? game.lastPassed ? 'PASS' : 'LOW FPS' : '—'}</strong></div></div>${samples.length ? `<div class="ow-fps-chart" aria-label="16 FPS samples">${samples.map(sample => `<i style="height:${Math.max(4, sample / max * 100)}%" title="${sample} FPS"></i>`).join('')}</div><small class="ow-chart-caption">16 hardware-dependent samples · мин ${Math.min(...samples)} / макс ${Math.max(...samples)} FPS</small>` : '<p class="ow-footnote">Нажмите «Играть»: симулятор рассчитает FPS и 16 samples.</p>'}<div class="ow-row-actions"><button type="button" class="pc-native-button accent" data-action="world-play" data-package="${id}">${svgIcon('play')}${game.launches ? 'Запустить снова' : 'Играть · показать FPS'}</button>${game.running ? `<button type="button" class="pc-native-button" data-action="world-stop-game" data-package="${id}">Завершить процесс</button>` : ''}</div></article>`; }).join('')}</div>` : '<div class="ow-empty">Установленных игр нет. Browser → games.local → скачать → установить.</div>'}<button type="button" class="pc-native-button" data-action="world-open-store">${svgIcon('market')}Открыть Games Store</button><p class="ow-footnote">CPU score ${round(scores.cpu)} · GPU score ${round(scores.gpu)}. Запуск использует существующий benchmark engine и занимает 30 PC-минут.</p>`;
    };
    proto.worldFiles = function (machine, family, session) {
      const state = this.game.osWorld(machine, family); if (!state.files.length) return '<div class="ow-empty">Файлов нет. Создайте новый.</div>';
      if (!state.files.some(file => file.id === session.worldFileId)) session.worldFileId = state.files[0].id;
      const file = state.files.find(item => item.id === session.worldFileId); const draft = session.worldFileDraft && session.worldFileDraft.id === file.id ? session.worldFileDraft : null;
      return `<div class="ow-files"><aside><button type="button" class="pc-native-button accent" data-action="world-file-new">${svgIcon('plus')}Новый файл</button>${state.files.map(item => `<button type="button" class="${item.id === file.id ? 'active' : ''}" data-action="world-file-select" data-id="${esc(item.id)}">${svgIcon('files')}<span><strong>${esc(item.name)}</strong><small>rev ${item.revision}</small></span></button>`).join('')}</aside><section><label class="ow-file-path">${esc(file.path)}</label><input class="ow-file-name" data-world-file-name value="${esc(draft ? draft.name : file.name)}" maxlength="60" aria-label="Имя файла"><textarea data-world-file-content maxlength="100000" spellcheck="false" aria-label="Содержимое файла">${esc(draft ? draft.content : file.content)}</textarea><div class="ow-editor-footer"><span>LOCAL TEXT · максимум 100 000 символов</span><button type="button" class="pc-native-button accent" data-action="world-file-save">Сохранить · rev ${file.revision + 1}</button></div></section></div>`;
    };
    proto.worldProcesses = function (machine, family) {
      const processes = this.game.osWorldProcesses(machine, family);
      return `<div class="ow-process-head"><span>${processes.length} процессов</span><span>LIVE · установленные ID не удаляются при stop</span></div><div class="ow-process-table"><div class="ow-process-row head"><b>PID</b><b>Процесс</b><b>CPU</b><b>RAM</b><b>Статус</b><b>Действие</b></div>${processes.map(process => `<div class="ow-process-row"><code>${process.pid}</code><span><strong>${esc(process.name)}</strong><small>${process.kind}</small></span><b>${round(process.cpu)}%</b><b>${mb(process.ramMb)}</b><span class="ow-status-${process.status}">${process.status}${process.fps ? ` · ${process.fps} FPS` : ''}</span><span>${process.kind === 'game' ? `<button type="button" data-action="world-stop-game" data-package="${process.id}">Stop</button>` : process.kind === 'download' ? `<button type="button" data-action="world-download-action" data-id="${process.id}" data-command="${process.status === 'paused' ? 'resume' : 'pause'}">${process.status === 'paused' ? 'Resume' : 'Pause'}</button>` : process.kind === 'service' ? `<button type="button" data-action="world-service" data-id="${process.id}" data-command="${process.status === 'stopped' ? 'start' : 'stop'}">${process.status === 'stopped' ? 'Start' : 'Stop'}</button>` : '—'}</span></div>`).join('')}</div>`;
    };
    proto.worldServices = function (machine) {
      const state = this.game.osWorld(machine, 'server'); const processes = this.game.osWorldProcesses(machine, 'server').filter(process => process.kind === 'service');
      return `<div class="ow-service-summary"><div><span>Активные</span><strong>${processes.filter(item => item.status !== 'stopped').length}</strong></div><div><span>Остановлены</span><strong>${state.stoppedServices.length}</strong></div><div><span>Доход сейчас</span><strong>${round(this.game.hourlyServerRevenue(machine))} ₽/ч</strong></div></div><div class="ow-service-list">${processes.map(process => `<article><div>${svgIcon('server')}<span><strong>${esc(process.name)}</strong><small>${esc(process.id)} · PID ${process.pid}</small></span><b class="ow-status-${process.status}">${process.status}</b></div><p>CPU ${round(process.cpu)}% · RAM ${mb(process.ramMb)} · установленный package сохранён</p><div class="ow-row-actions">${process.status === 'stopped' ? `<button type="button" class="pc-native-button accent" data-action="world-service" data-id="${process.id}" data-command="start">Start</button>` : `<button type="button" class="pc-native-button" data-action="world-service" data-id="${process.id}" data-command="stop">Stop</button>`}<button type="button" class="pc-native-button" data-action="world-service" data-id="${process.id}" data-command="restart">Restart</button></div></article>`).join('') || '<div class="ow-empty">Служб нет. Packages → найти nginx/docker → скачать и установить.</div>'}</div><button type="button" class="pc-native-button" data-action="world-open-store">${svgIcon('apps')}Package Manager</button>`;
    };
    proto.worldPower = function (machine) {
      const state = this.game.osWorld(machine, 'pc'); const metrics = this.game.pcMetrics(machine); const scores = this.game.pcPerformanceScores(machine);
      return `<div class="ow-power-plans">${Object.values(DATA.OS_WORLD_POWER_PLANS).map(plan => `<button type="button" class="${state.powerPlan === plan.id ? 'active' : ''}" data-action="world-power-plan" data-plan="${plan.id}">${svgIcon(plan.id === 'eco' ? 'shield' : plan.id === 'performance' ? 'bolt' : 'power')}<strong>${esc(plan.name)}</strong><span>${esc(plan.description)}</span><small>Score ×${plan.score} · Watts ×${plan.watts}</small></button>`).join('')}</div><div class="ow-metric-grid"><div><span>CPU score</span><strong>${round(scores.cpu)}</strong></div><div><span>GPU score</span><strong>${round(scores.gpu)}</strong></div><div><span>Питание</span><strong>${round(metrics.watts)} W</strong></div><div><span>Шум</span><strong>${round(metrics.noise)} дБ</strong></div></div><p class="ow-footnote">План реально меняет benchmark FPS, ватты, целевую температуру и процедурный звук. Старый тюнинг и fan profile сохранены.</p>`;
    };
    proto.worldAppContent = function (machine, family, app, session) {
      if (app === 'browser') return this.worldBrowser(machine, family, session);
      if (app === 'downloads') return this.worldDownloads(machine, family);
      if (app === 'games') return this.worldGames(machine);
      if (app === 'packages') return this.worldStore(machine, family, session);
      if (app === 'worldfiles') return this.worldFiles(machine, family, session);
      if (app === 'processes') return this.worldProcesses(machine, family);
      if (app === 'services') return this.worldServices(machine);
      if (app === 'power') return this.worldPower(machine);
      return '';
    };

    const baseWorldPCWindow = proto.pcAppWindow;
    proto.pcAppWindow = function (machine, os) {
      const app = this.pcSession && this.pcSession.app; if (!pcApps.includes(app)) return baseWorldPCWindow.call(this, machine, os);
      return `<div class="pc-app-window ow-window ow-${app}"><div class="pc-window-title"><span>${svgIcon(appIcons[app])} ${appNames[app]}</span><div><button type="button" data-action="pc-os-close-app">—</button><button type="button" data-action="pc-os-close-app">×</button></div></div><div class="pc-window-content">${this.worldAppContent(machine, 'pc', app, this.pcSession)}</div><div class="pc-window-status">OS WORLD 8.3 · PER-PC SAVE<span>${esc(machine.name)}</span></div></div>`;
    };
    const baseWorldPCStart = proto.pcStartMenu;
    proto.pcStartMenu = function (machine, os) {
      const html = baseWorldPCStart.call(this, machine, os); if (!html) return html;
      const buttons = pcApps.map(app => `<button type="button" data-action="pc-os-app" data-app="${app}">${svgIcon(appIcons[app])}<span>${appNames[app]}</span></button>`).join('');
      return html.replace('</div><div class="pc-start-bottom">', `${buttons}</div><div class="pc-start-bottom">`);
    };
    const baseWorldPCDesktop = proto.pcDesktop;
    proto.pcDesktop = function (machine, os) {
      let html = baseWorldPCDesktop.call(this, machine, os);
      if (os.style === 'dos') return html.replace(/<\/div>$/, `<div class="ow-dos-shortcuts"><span>OS WORLD COMMANDS:</span>${['store','downloads','files','plan balanced'].map(command => `<button type="button" data-action="pc-os-command" data-command="${command}">${command}</button>`).join('')}</div></div>`);
      const dock = `<div class="pc-world-dock">${pcApps.map(app => `<button type="button" class="${this.pcSession.app === app ? 'active' : ''}" data-action="pc-os-app" data-app="${app}" title="${appNames[app]}">${svgIcon(appIcons[app])}<span>${app === 'worldfiles' ? 'Files' : app === 'power' ? 'Power' : appNames[app].split(' ')[0]}</span></button>`).join('')}</div>`;
      return html.replace(/<\/div>$/, `${dock}</div>`);
    };
    proto.serverWorldWindow = function (server) {
      const app = this.session && this.session.worldApp; if (!serverApps.includes(app)) return '';
      return `<section class="server-world-window ow-${app}"><header><span>${svgIcon(appIcons[app])}<strong>${appNames[app]}</strong></span><button type="button" data-action="server-world-close" aria-label="Закрыть">×</button></header><div class="server-world-content">${this.worldAppContent(server, 'server', app, this.session)}</div><footer>OS WORLD 8.3 · PER-SERVER SAVE<span>${esc(server.name)}</span></footer></section>`;
    };
    const worldServerButtons = `<div class="server-world-launcher">${[['browser','network','Browser'],['packages','apps','Packages'],['downloads','disk','Downloads'],['worldfiles','files','Files'],['services','server','Services'],['processes','stats','Processes']].map(([app,icon,label]) => `<button type="button" data-action="server-world-app" data-app="${app}">${svgIcon(icon)}<span>${label}</span></button>`).join('')}</div>`;
    const baseWorldServerDesktop = proto.renderSessionDesktop;
    proto.renderSessionDesktop = function (server) { const html = baseWorldServerDesktop.call(this, server); return html.replace('<div class="desktop-taskbar">', `${worldServerButtons}${this.serverWorldWindow(server)}<div class="desktop-taskbar">`); };
    const baseWorldServerTerminal = proto.renderSessionTerminal;
    proto.renderSessionTerminal = function (server) { const html = baseWorldServerTerminal.call(this, server); return html.replace(/<\/div>$/, `<div class="ow-terminal-apps">OS WORLD: ${['browser','packages','downloads','worldfiles','services'].map(app => `<button type="button" data-action="server-world-app" data-app="${app}">${app === 'worldfiles' ? 'files' : app}</button>`).join('')}</div></div>`); };

    const finishWorldAction = view => { view.processEvents(); view.renderHUD(); view.renderModal(); };
    const baseWorldAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const ctx = context(this); const own = action.startsWith('world-') || action === 'server-world-app' || action === 'server-world-close';
      if (own && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      if (action === 'pc-os-app' && pcApps.includes(element.dataset.app)) { if (this.pcSession) this.pcSession.worldFileDraft = null; return baseWorldAction.call(this, action, element, event); }
      if (!ctx || !own) return baseWorldAction.call(this, action, element, event);
      const { family, machine, session } = ctx; const id = element.dataset.id; const packageId = element.dataset.package;
      switch (action) {
        case 'server-world-app': session.worldApp = element.dataset.app; session.worldFileDraft = null; session.mode = 'desktop'; this.renderSession(); return;
        case 'server-world-close': session.worldApp = null; session.worldFileDraft = null; this.renderSession(); return;
        case 'world-browser-nav': this.game.osWorldNavigate(machine.id, family, element.dataset.url || '', element.dataset.direction || ''); finishWorldAction(this); return;
        case 'world-bookmark': this.game.osWorldBookmark(machine.id, family); finishWorldAction(this); return;
        case 'world-download': { const result = this.game.osWorldStartDownload(machine.id, family, packageId, this.game.osWorld(machine, family).defaultSpeed); if (result.ok) { if ((family === 'pc' ? session.app : session.worldApp) === 'browser') this.game.osWorldNavigate(machine.id, family, 'downloads.local'); else if (family === 'server') session.worldApp = 'downloads'; } finishWorldAction(this); return; }
        case 'world-download-action': this.game.osWorldDownloadAction(machine.id, family, id, element.dataset.command); finishWorldAction(this); return;
        case 'world-install': this.game.osWorldInstall(machine.id, family, id); finishWorldAction(this); return;
        case 'world-play': if (family === 'pc') { this.game.osWorldPlayGame(machine.id, packageId); session.app = 'games'; } finishWorldAction(this); return;
        case 'world-stop-game': if (family === 'pc') this.game.osWorldStopGame(machine.id, packageId); finishWorldAction(this); return;
        case 'world-power-plan': if (family === 'pc') this.game.osWorldPowerPlan(machine.id, element.dataset.plan); finishWorldAction(this); return;
        case 'world-open-downloads': if ((family === 'pc' ? session.app : session.worldApp) === 'browser') this.game.osWorldNavigate(machine.id, family, 'downloads.local'); else if (family === 'pc') session.app = 'downloads'; else session.worldApp = 'downloads'; finishWorldAction(this); return;
        case 'world-open-store': if (family === 'pc') { session.app = 'browser'; this.game.osWorldNavigate(machine.id, 'pc', 'games.local'); } else if (session.worldApp === 'browser') this.game.osWorldNavigate(machine.id, 'server', 'repo.local'); else session.worldApp = 'packages'; finishWorldAction(this); return;
        case 'world-file-select': session.worldFileId = id; session.worldFileDraft = null; this.renderModal(); return;
        case 'world-file-new': { const file = this.game.osWorldSaveFile(machine.id, family, null, `new-${this.game.osWorld(machine, family).files.length + 1}.txt`, ''); if (file) { session.worldFileId = file.id; session.worldFileDraft = null; } finishWorldAction(this); return; }
        case 'world-file-save': { const content = $('[data-world-file-content]', this.modalRoot); const name = $('[data-world-file-name]', this.modalRoot); const file = this.game.osWorldSaveFile(machine.id, family, session.worldFileId, name ? name.value : 'untitled.txt', content ? content.value : ''); if (file) { session.worldFileId = file.id; session.worldFileDraft = null; this.toast('Файл сохранён на этой машине', 'success'); } finishWorldAction(this); return; }
        case 'world-service': if (family === 'server') this.game.osWorldService(machine.id, id, element.dataset.command); finishWorldAction(this); return;
        default: return baseWorldAction.call(this, action, element, event);
      }
    };
    document.addEventListener('submit', event => {
      const ctx = context(ui); if (!ctx) return;
      if (event.target.matches('[data-world-browser-form]')) { event.preventDefault(); const input = $('[data-world-browser-url]', event.target); ui.game.osWorldNavigate(ctx.machine.id, ctx.family, input.value); ui.renderModal(); }
      if (event.target.matches('[data-world-package-form]')) { event.preventDefault(); const input = $('[data-world-package-query]', event.target); ctx.session.worldPackageQuery = input.value.trim(); ui.renderModal(); }
    });
    document.addEventListener('change', event => {
      const ctx = context(ui); if (!ctx) return;
      if (event.target.matches('[data-world-default-speed]')) { ctx.session.worldFileDraft = null; ui.game.osWorld(ctx.machine, ctx.family).defaultSpeed = event.target.value; ui.game.save(); }
      if (event.target.matches('[data-world-download-speed]')) { ui.game.osWorldDownloadAction(ctx.machine.id, ctx.family, event.target.dataset.id, 'speed', event.target.value); ui.renderModal(); }
    });
    document.addEventListener('input', event => {
      const ctx = context(ui); if (!ctx || !event.target.matches('[data-world-file-content],[data-world-file-name]')) return;
      const content = $('[data-world-file-content]', ui.modalRoot); const name = $('[data-world-file-name]', ui.modalRoot);
      ctx.session.worldFileDraft = { id: ctx.session.worldFileId, content: content ? content.value : '', name: name ? name.value : 'untitled.txt' };
    });
    const worldStatusSignature = ctx => ctx ? ui.game.osWorld(ctx.machine, ctx.family).downloads.map(item => `${item.id}:${item.status}`).join('|') : '';
    const baseWorldTick = proto.onTick;
    proto.onTick = function () {
      const before = context(this); const signature = worldStatusSignature(before); const result = baseWorldTick.call(this); const ctx = context(this); if (!ctx) return result;
      const state = this.game.osWorld(ctx.machine, ctx.family);
      Array.from(this.modalRoot.querySelectorAll('[data-world-download-row]')).forEach(row => {
        const item = state.downloads.find(download => download.id === row.dataset.worldDownloadRow); if (!item) return; const percent = item.totalMb ? item.progressMb / item.totalMb * 100 : 0;
        const bar = $('[data-world-progress]', row); const label = $('[data-world-progress-label]', row); const rate = $('[data-world-rate]', row);
        if (bar) bar.style.width = `${percent}%`; if (label) label.textContent = `${round(item.progressMb)} / ${round(item.totalMb)} МБ · ${percent.toFixed(1)}%`; if (rate) rate.textContent = item.status === 'downloading' ? `${Number(item.actualMbps.toFixed(1))} Мбит/с` : '';
      });
      const app = ctx.family === 'pc' ? ctx.session.app : ctx.session.worldApp;
      if (signature !== worldStatusSignature(ctx) && ['browser','downloads','packages'].includes(app) && !(ctx.session.worldFileDraft)) this.renderModal();
      return result;
    };
    const baseWorldBrandNav = proto.renderNav;
    proto.renderNav = function () { const result = baseWorldBrandNav.call(this); const subtitle = $('.brand-copy span'); if (subtitle) subtitle.textContent = 'OS WORLD 8.3 // OFFLINE GAMES · SERVICES · PC AVITO'; return result; };
    const baseWorldOnboarding = proto.renderOnboarding;
    proto.renderOnboarding = function () { const result = baseWorldOnboarding.call(this); const title = $('.onboarding-title', this.modalRoot); if (title) title.textContent = 'OS WORLD 8.3'; return result; };
    const baseWorldModeSelect = proto.renderModeSelect;
    proto.renderModeSelect = function () { const result = baseWorldModeSelect.call(this); const subtitle = $('.modal-subtitle', this.modalRoot); if (subtitle) subtitle.textContent = 'SERVER TYCOON 8.3 // OS WORLD · OFFLINE GAMES · SERVICES'; return result; };
    ui.renderNav();
    if (ui.modal && ui.modal.type === 'onboarding') ui.renderOnboarding();
    if (ui.modal && ui.modal.type === 'modeSelect') ui.renderModeSelect();
    proto.osWorldUILoaded = true;
  });
})();
