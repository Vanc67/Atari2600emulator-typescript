/* Ultimate 7.0 PC Workshop: заказы, диагностика, чистка, ОС и игровые тесты. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.createPC) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.pcUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0);
    const money = value => `${round(value).toLocaleString('ru-RU')} ₽`;
    const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${Number((value / 1024).toFixed(value % 1024 ? 1 : 0))} ГБ` : `${round(value)} МБ`;

    proto.pcViewHead = function (eyebrow, title, subtitle) {
      const pc = this.game.state.pc;
      return `<div class="view-head pc-view-head"><div><div class="eyebrow">${eyebrow}</div><h1 class="view-title">${title}</h1><p class="view-subtitle">${subtitle}</p></div><div class="day-chip">${pc.year}<small>Д${pc.day} · <span data-live="pc-clock">${this.game.pcClock()}</span></small></div></div>`;
    };
    proto.pcCampaignCard = function () {
      const pc = this.game.state.pc; const time = this.game.pcTimeInfo();
      return `<div class="card pc-campaign-card"><div class="pc-campaign-head"><span class="pc-dad-avatar">П</span><div><span>ПАПИНА МАСТЕРСКАЯ</span><strong>${esc(this.game.pcRank())}</strong><small>Уровень ${pc.level} · ${pc.xp} XP · репутация ${pc.reputation}</small></div><b>${pc.year}</b></div><div class="meter pc-year-meter"><i style="width:${time.progress}%"></i></div><div class="pc-campaign-meta"><span>${time.yearDay}/12 месяцев эпохи</span><span>до нового года: ${time.daysToNextYear} дн.</span></div><div class="pc-speed-row">${Object.values(DATA.PC_SPEEDS).map(speed => `<button class="${pc.speed === speed.id ? 'active' : ''}" data-action="pc-speed" data-speed="${speed.id}"><strong>${speed.name}</strong><span>${speed.dayMinutes} мин/день</span></button>`).join('')}</div><small class="pc-speed-description">${esc(DATA.PC_SPEEDS[pc.speed].description)}</small></div>`;
    };
    proto.pcMachineCard = function (machine) {
      const metrics = this.game.pcMetrics(machine); const unresolved = machine.issues.filter(issue => !issue.resolved).length;
      const found = machine.issues.filter(issue => issue.revealed && !issue.resolved).length;
      const stability = machine.benchmarks.stability;
      return `<article class="pc-machine-card ${machine.crashed ? 'crashed' : ''}"><button class="pc-machine-main" data-action="pc-open" data-id="${machine.id}"><span class="pc-case-icon">${svgIcon(machine.year < 2000 ? 'server' : 'chip')}<i class="${machine.powered ? 'on' : ''}"></i></span><div><span>${machine.origin === 'job' ? `ЗАКАЗ · ${esc(machine.owner)}` : 'СОБСТВЕННЫЙ ПК'} · ${machine.year}</span><strong>${esc(machine.name)}</strong><small>${esc(machine.cpu)} · ${mb(machine.ramMb)} ${machine.ramType}</small><small>${machine.os ? esc(this.game.pcOS(machine.os).name) : 'NO OS'} · ${machine.experimentalOS ? 'EXPERIMENTAL' : machine.crashed ? 'THERMAL CRASH' : machine.powered ? 'POWER ON' : 'POWER OFF'}</small></div><b>${machine.crashed ? '!' : unresolved ? found ? `${found} FIX` : '? DIAG' : stability && stability.passed ? 'PASS' : 'READY'}</b></button><div class="pc-machine-meters"><span>CPU ${round(metrics.cpuLoad)}%</span><span>RAM ${round(metrics.ramPercent)}%</span><span>${round(machine.temp)}°C</span><span>${round(metrics.watts)}W</span></div></article>`;
    };
    proto.pcWorkshopView = function () {
      const pc = this.game.state.pc;
      let html = this.pcViewHead('PC // GARAGE TO BUSINESS', 'Мастерская', pc.completed < 3 ? 'Сначала друзья папы. Потом — весь город.' : 'Диагностика, ремонт, эксперименты и ретро-железо.');
      html += this.pcCampaignCard();
      if (!pc.completed) html += `<div class="notice pc-dad-note">${svgIcon('info')}<div><strong>Папа: «Начни с ПК дяди Саши»</strong>Прими заказ во вкладке «Заказы». Сделай полную диагностику, устрани найденное и прогони Stability Suite перед возвратом.</div></div>`;
      const activeJobs = pc.computers.filter(machine => machine.origin === 'job'); const own = pc.computers.filter(machine => machine.origin === 'owned');
      html += `<div class="section-label"><span>На рабочем столе</span><span>${activeJobs.length}/${1 + pc.upgrades.bench} мест · ${pc.computers.length} ПК</span></div>`;
      html += pc.computers.length ? pc.computers.map(machine => this.pcMachineCard(machine)).join('') : this.emptyState('chip','Стол пуст','Примите заказ или поищите ретро-ПК на мусорке.','К заказам','pc-go-jobs');
      html += `<div class="pc-workshop-actions"><button class="btn btn-cyan" data-action="pc-go-jobs">${svgIcon('contract')}Клиенты · ${pc.offers.length}</button><button class="btn btn-ghost" data-action="pc-go-dumpster">${svgIcon('trash')}На мусорку</button></div>`;
      html += `<div class="card pc-parts-shelf"><h3 class="card-title"><span>${svgIcon('inventory')}Полка PC-запчастей</span><span class="badge info">${pc.parts.length}</span></h3><p class="muted tiny">${pc.parts.length ? pc.parts.slice(0, 4).map(part => esc(part.name)).join(' · ') : 'Совместимые RAM, PSU, диски и GPU появятся после находок и заказов.'}</p><button class="btn btn-small btn-ghost btn-wide" data-action="pc-open-upgrades">${svgIcon('wrench')}Улучшения мастерской</button></div>`;
      return html;
    };
    proto.pcJobsView = function () {
      const pc = this.game.state.pc;
      let html = this.pcViewHead('FRIENDS → AVITO → BUSINESS', 'Заказы', 'Сначала знакомые, затем объявления и постоянные клиенты.');
      html += `<div class="pc-business-stats"><div><span>ВЫПОЛНЕНО</span><strong>${pc.completed}</strong></div><div><span>ЗАРАБОТАНО</span><strong>${money(pc.earnings)}</strong></div><div><span>РЕПУТАЦИЯ</span><strong>${pc.reputation}</strong></div></div>`;
      const jobs = pc.computers.filter(machine => machine.origin === 'job');
      if (jobs.length) html += `<div class="section-label"><span>В работе</span><span>${jobs.length}/${1 + pc.upgrades.bench}</span></div>${jobs.map(machine => this.pcMachineCard(machine)).join('')}`;
      html += `<div class="section-label"><span>Доступные обращения</span><span>обновление без ожидания</span></div>`;
      html += pc.offers.map(offer => `<article class="card pc-job-offer"><div class="pc-job-source">${offer.source === 'avito' ? 'AVITO // ОБЪЯВЛЕНИЕ' : 'ЗНАКОМЫЕ // РЕКОМЕНДАЦИЯ'}<span>до дня ${offer.deadline}</span></div><div class="pc-job-body"><span>${svgIcon('chip')}</span><div><strong>${esc(offer.owner)}</strong><h3>${esc(offer.pc.name)}</h3><p>${esc(offer.title)}</p><small>${offer.pc.year} · ${offer.pc.ramType} ${mb(offer.pc.ramMb)} · неисправностей: ${offer.pc.issues.length}</small></div><b>${money(offer.reward)}</b></div><button class="btn btn-primary btn-wide" data-action="pc-accept-job" data-id="${offer.id}" data-pc="${offer.pc.id}">${svgIcon('plus')}Принять в мастерскую</button></article>`).join('') || this.emptyState('contract','Новых обращений нет','Они появятся на следующем игровом дне.');
      return html;
    };
    proto.pcDumpsterView = function () {
      const pc = this.game.state.pc; const checked = pc.dumpsterDay === pc.day;
      const finds = pc.computers.filter(machine => machine.origin === 'owned' && machine.owner === 'Находка с мусорки');
      const svg = `<svg viewBox="0 0 320 150" class="dumpster-art" aria-label="Мусорный контейнер со старыми ПК"><defs><linearGradient id="dumpsterBody" x2="0" y2="1"><stop stop-color="#244d4b"/><stop offset="1" stop-color="#102d30"/></linearGradient></defs><path d="M30 55h260l-18 78H48z" fill="url(#dumpsterBody)" stroke="#4b8780" stroke-width="3"/><path d="M20 48h280v15H20z" fill="#376b64"/><path d="M68 132v10m184-10v10" stroke="#789c91" stroke-width="8"/><path d="M105 45V17h67v28m-60-21h52m-49 8h46" fill="#53616c" stroke="#91a2ad" stroke-width="3"/><rect x="194" y="12" width="55" height="37" rx="3" fill="#a9a795" stroke="#d8d2b7" stroke-width="3"/><rect x="201" y="18" width="41" height="20" fill="#263d42"/><circle cx="222" cy="43" r="2" fill="#b6ff56"/><path d="M87 75h147M78 98h163" stroke="#4b8780" opacity=".35"/></svg>`;
      let html = this.pcViewHead('RETRO // ONE MAN’S TRASH', 'Городская мусорка', 'Каждый игровой день — новый шанс найти ретро-ПК или деталь.');
      html += `<div class="card dumpster-card">${svg}<div class="dumpster-copy"><span>БЕСПЛАТНАЯ РЕТРО-ОХОТА</span><strong>${checked ? 'Сегодня всё проверено' : 'Контейнер ещё не проверен'}</strong><p>Находки относятся к любому году от 1990 до ${pc.year}. Сломанные ПК можно починить, протестировать и продать.</p></div><button class="btn ${checked ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="pc-dumpster-search" ${checked ? 'disabled' : ''}>${svgIcon('trash')}${checked ? 'Возвращайтесь завтра' : 'Порыться в мусорке · 40 мин'}</button></div>`;
      html += `<div class="pc-business-stats"><div><span>ВЫХОДОВ</span><strong>${pc.dumpsters}</strong></div><div><span>РЕТРО-ПК</span><strong>${finds.length}</strong></div><div><span>ДЕТАЛЕЙ</span><strong>${pc.parts.length}</strong></div></div>`;
      if (finds.length) html += `<div class="section-label"><span>Спасённые машины</span><span>${finds.length}</span></div>${finds.map(machine => this.pcMachineCard(machine)).join('')}`;
      html += `<div class="notice">${svgIcon('info')}<div><strong>Ничего не пропадает</strong>Найденные компьютеры остаются в мастерской. Детали используются для ремонта с учётом DDR/SDRAM и мощности PSU.</div></div>`;
      return html;
    };
    proto.pcSettingsView = function () {
      const pc = this.game.state.pc;
      const definitions = { bench: ['Рабочие места','+1 одновременный заказ за уровень',[3000,9000,25000]], ads: ['Реклама на Авито','Больше клиентов и выше оплата',[1500,5000,12000,30000,70000]], tools: ['Инструменты','Дешевле запчасти для ремонта',[1000,4000,12000]] };
      let html = this.pcViewHead('WORKSHOP // PROGRESSION', 'Бизнес и настройки', 'Вложения, музыка, скорость эпохи и журнал мастерской.');
      html += this.pcCampaignCard();
      html += `<div class="section-label"><span>Улучшения</span><span>бюджет ${money(pc.money)}</span></div>`;
      html += Object.entries(definitions).map(([kind, [name, description, prices]]) => { const level = pc.upgrades[kind]; const price = prices[level]; return `<article class="card pc-upgrade-card"><div><span>${svgIcon(kind === 'ads' ? 'market' : kind === 'bench' ? 'chip' : 'wrench')}</span><div><strong>${name}</strong><p>${description}</p></div><b>${level}/${prices.length}</b></div><button class="btn btn-small ${price == null ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="pc-upgrade" data-kind="${kind}" ${price == null || pc.money < price ? 'disabled' : ''}>${price == null ? 'Максимальный уровень' : `Улучшить · ${money(price)}`}</button></article>`; }).join('');
      html += this.radioCard();
      html += `<div class="card pc-log-card"><h3 class="card-title"><span>${svgIcon('files')}Журнал мастерской</span><span class="badge info">${pc.logs.length}</span></h3>${pc.logs.slice(0, 12).map(log => `<div class="pc-log-row ${log.type}"><span>${log.year} · D${log.day}</span><p>${esc(log.text)}</p></div>`).join('')}</div><button class="btn btn-ghost btn-wide" data-action="choose-server-mode">${svgIcon('server')}Переключиться на серверную ферму</button>`;
      return html;
    };

    proto.openPC = function (id) { if (this.game.getPC(id)) this.openModal('pcDetail', { pcId: id }); };
    proto.pcIssueRow = function (machine, issue) {
      const definition = DATA.PC_ISSUES[issue.id]; const spare = this.game.pcSpareForIssue(machine, issue.id); const price = this.game.pcIssuePrice(machine, issue.id);
      let action = '';
      if (!issue.resolved && issue.revealed) {
        if (definition.action === 'clean') action = `<button class="btn btn-small btn-amber" data-action="pc-clean-open" data-id="${machine.id}">Чистить 6 зон</button>`;
        else if (definition.action === 'os') action = `<button class="btn btn-small btn-primary" data-action="pc-os-open" data-id="${machine.id}">Установить ОС</button>`;
        else action = `<button class="btn btn-small ${spare || !definition.part ? 'btn-primary' : 'btn-amber'}" data-action="pc-repair" data-id="${machine.id}" data-issue="${issue.id}" data-buy="${spare || !definition.part ? 0 : 1}">${definition.part ? spare ? 'Заменить из инвентаря' : `Купить и заменить · ${money(price)}` : definition.action === 'paste' ? 'Термопаста · 50 ₽' : 'Устранить'}</button>`;
      }
      return `<div class="pc-issue ${issue.resolved ? 'resolved' : issue.revealed ? 'revealed' : ''}"><span>${svgIcon(issue.resolved ? 'check' : issue.revealed ? 'warning' : 'inventory')}</span><div><strong>${issue.revealed || issue.resolved ? esc(definition.name) : 'Неизвестная неисправность'}</strong><small>${issue.resolved ? 'Устранено' : issue.revealed ? esc(definition.description) : `Нужен тест: ${DATA.PC_DIAGNOSTICS[definition.test].name}`}</small></div>${action}</div>`;
    };
    proto.renderPCDetail = function () {
      const machine = this.game.getPC(this.modal.pcId); if (!machine) { this.closeModal(); return; }
      const m = this.game.pcMetrics(machine); const unresolved = machine.issues.filter(issue => !issue.resolved).length; const stability = machine.benchmarks.stability;
      let body = `<div class="pc-detail-hero"><span>${svgIcon('chip')}</span><div><small>${machine.origin === 'job' ? `КЛИЕНТ: ${esc(machine.owner)}` : 'СОБСТВЕННЫЙ ПК'} · ${machine.year}</small><strong>${esc(machine.name)}</strong><p>${esc(machine.cpu)}<br>${mb(machine.ramMb)} ${machine.ramType} · ${machine.storageType} ${mb(machine.storageMb)}<br>${esc(machine.gpu)} · PSU ${machine.psuWatts}W</p></div><b class="${machine.powered ? 'on' : ''}">${machine.crashed ? 'CRASH' : machine.powered ? 'ON' : 'OFF'}</b></div>`;
      body += `<div class="pc-live-metrics"><div><span>CPU</span><strong>${round(m.cpuLoad)}%</strong></div><div><span>RAM</span><strong>${round(m.ramPercent)}%</strong></div><div><span>TEMP</span><strong>${round(machine.temp)}°C</strong></div><div><span>POWER</span><strong>${round(m.watts)}W</strong></div><div><span>NOISE</span><strong>${round(m.noise)} дБ</strong></div><div><span>HEALTH</span><strong>${round(machine.health)}%</strong></div></div>`;
      if (machine.crashed) body += `<div class="notice error">${svgIcon('warning')}<div><strong>Тепловое отключение · 95°C</strong>Почистите пыль, замените термопасту, затем выполните POST и перезагрузку.</div></div>`;
      body += `<div class="action-grid pc-detail-actions"><button class="btn ${machine.powered ? 'btn-danger' : 'btn-primary'}" data-action="pc-power" data-id="${machine.id}">${svgIcon('power')}${machine.powered ? 'Выключить' : 'Включить'}</button>${machine.crashed ? `<button class="btn btn-amber" data-action="pc-reboot" data-id="${machine.id}">${svgIcon('refresh')}Сбросить аварию</button>` : ''}<button class="btn btn-cyan" data-action="pc-open-desktop" data-id="${machine.id}" ${machine.powered && !machine.crashed ? '' : 'disabled'}>${svgIcon('os')}${machine.os ? 'Рабочий стол ОС' : 'BIOS / монитор'}</button><button class="btn btn-ghost" data-action="pc-bench-open" data-id="${machine.id}">${svgIcon('stats')}Игровые тесты</button><button class="btn btn-ghost" data-action="pc-os-open" data-id="${machine.id}" ${machine.powered ? 'disabled' : ''}>${svgIcon('os')}${machine.os ? 'Сменить ОС' : 'Установить ОС'}</button></div>`;
      if (machine.experimentalOS) body += `<div class="notice warning">${svgIcon('warning')}<div><strong>Experimental OS · performance ×${machine.osPenalty.toFixed(2)}</strong>Современная ОС запущена в обход проверок эпохи и минимальных требований. FPS и отзывчивость честно снижены.</div></div>`;
      body += `<div class="section-label"><span>Диагностика</span><span>${Object.keys(machine.diagnostics).length}/6 тестов</span></div><div class="pc-diagnostic-grid">${Object.values(DATA.PC_DIAGNOSTICS).map(test => `<button class="${machine.diagnostics[test.id] ? 'done' : ''}" data-action="pc-diagnostic" data-id="${machine.id}" data-test="${test.id}">${svgIcon(test.icon)}<strong>${test.name}</strong><span>${test.minutes} мин</span></button>`).join('')}<button class="full" data-action="pc-diagnostic" data-id="${machine.id}" data-test="full">${svgIcon('stats')}<strong>Полная диагностика</strong><span>35 мин · все 6 тестов</span></button></div>`;
      body += `<div class="section-label"><span>Найденные проблемы</span><span>${unresolved} осталось</span></div>${machine.issues.length ? machine.issues.map(issue => this.pcIssueRow(machine, issue)).join('') : '<div class="notice success">'+svgIcon('check')+'<div><strong>Аппаратных замечаний нет</strong>Можно запускать Stability Suite.</div></div>'}`;
      const last = machine.lastBenchmark && machine.benchmarks[machine.lastBenchmark];
      if (last) body += `<div class="pc-last-result"><span>${esc(DATA.PC_BENCHMARKS[last.benchmarkId].name)}</span><strong>${last.fps}${last.benchmarkId === 'stability' ? ' points' : ' FPS'}</strong><b class="${last.passed ? 'good' : 'bad'}">${last.passed ? 'PASS' : 'FAIL'}</b></div>`;
      if (machine.origin === 'job') body += `<button class="btn btn-primary btn-wide pc-complete-button" data-action="pc-complete-job" data-id="${machine.id}" ${unresolved || !stability || !stability.passed || machine.powered ? 'disabled' : ''}>${svgIcon('check')}Вернуть клиенту · ${money(machine.reward)}${machine.powered ? ' · выключите ПК' : ''}</button><p class="muted tiny">Для возврата: все неисправности устранены, Stability ≥60 и PASS, компьютер выключен.</p>`;
      else body += `<button class="btn btn-ghost btn-wide" data-action="pc-sell-confirm" data-id="${machine.id}" ${machine.powered ? 'disabled' : ''}>${svgIcon('wallet')}Продать этот ПК</button>`;
      this.modalRoot.innerHTML = this.modalShell(machine.name, `${machine.year} // PC DIAGNOSTICS & REPAIR`, body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('pc-detail-sheet');
    };

    proto.renderPCCleaning = function () {
      const machine = this.game.getPC(this.modal.pcId); if (!machine) { this.closeModal(); return; }
      const spots = ['CPU FAN','GPU','PSU','FRONT INTAKE','REAR EXHAUST','MAINBOARD'];
      const caseSvg = `<svg viewBox="0 0 300 390" class="open-pc-svg"><rect x="25" y="12" width="250" height="365" rx="10" fill="#26323b" stroke="#71828b" stroke-width="4"/><rect x="43" y="34" width="214" height="290" fill="#17242b" stroke="#3e5660"/><rect x="77" y="85" width="150" height="170" rx="5" fill="#285046" stroke="#568d73"/><circle cx="145" cy="125" r="39" fill="#1b2b32" stroke="#91a7ad" stroke-width="5"/><path d="M145 125l9-32m-9 32l30 12m-30-12l-26 21m26-21l-23-23" stroke="#657c83" stroke-width="10"/><rect x="68" y="267" width="165" height="24" fill="#40505c"/><rect x="58" y="330" width="183" height="34" fill="#111a20" stroke="#55636d"/><circle cx="231" cy="55" r="22" fill="none" stroke="#738991" stroke-width="4"/><path d="M45 295h25m-25 8h25m-25 8h25" stroke="#75a19b" stroke-width="4"/></svg>`;
      const body = `<div class="pc-clean-intro"><span>MANUAL DUST CLEANING</span><strong>${machine.cleanedSpots.length}/6 зон очищено</strong><p>Коснитесь каждой запылённой зоны на открытом корпусе. Одна кнопка не заменяет настоящую чистку.</p></div><div class="pc-clean-case">${caseSvg}${spots.map((spot, index) => `<button class="dust-spot spot-${index} ${machine.cleanedSpots.includes(index) ? 'clean' : ''}" data-action="pc-clean" data-id="${machine.id}" data-spot="${index}" ${machine.cleanedSpots.includes(index) || machine.powered ? 'disabled' : ''}><i></i><span>${spot}</span></button>`).join('')}</div>${machine.powered ? '<div class="notice error">Сначала выключите компьютер.</div>' : ''}<button class="btn btn-ghost btn-wide" data-action="pc-open" data-id="${machine.id}">Вернуться к диагностике</button>`;
      this.modalRoot.innerHTML = this.modalShell('Чистка от пыли', `${machine.name} // 6 ИНТЕРАКТИВНЫХ ЗОН`, body);
    };
    proto.renderPCOSInstall = function () {
      const machine = this.game.getPC(this.modal.pcId); if (!machine) { this.closeModal(); return; }
      const year = this.game.state.pc.year;
      const body = `<div class="notice">${svgIcon('os')}<div><strong>${esc(machine.name)} · ${mb(machine.ramMb)} RAM · ${mb(machine.storageMb)}</strong>Обычная установка проверяет год, RAM и диск. Experimental Bypass доступен только на собственных ПК и честно снижает производительность.</div></div><div class="pc-os-list">${DATA.PC_OS.map(os => { const compatible = machine.ramMb >= os.minRamMb && machine.storageMb >= os.minStorageMb && os.year <= year; const experimental = machine.origin === 'owned'; return `<article class="pc-os-choice ${machine.os === os.id ? 'current' : ''}"><div><span>${svgIcon(os.style === 'dos' ? 'os' : os.style === 'linux' ? 'apps' : 'server')}</span><div><strong>${esc(os.name)}</strong><small>${os.year} · RAM ${mb(os.minRamMb)} · диск ${mb(os.minStorageMb)}</small><p>${os.year > year ? 'ОС из будущей эпохи' : os.price ? `Лицензия ${money(os.price)}` : 'Бесплатная лицензия'}</p></div></div><div class="pc-os-buttons"><button class="btn btn-small btn-primary" data-action="pc-install-os" data-id="${machine.id}" data-os="${os.id}" ${compatible && !machine.powered ? '' : 'disabled'}>${compatible ? 'Установить' : 'Не подходит'}</button>${experimental ? `<button class="btn btn-small btn-amber" data-action="pc-install-os" data-id="${machine.id}" data-os="${os.id}" data-experimental="1" ${machine.powered ? 'disabled' : ''}>Experimental Bypass</button>` : ''}</div></article>`; }).join('')}</div><button class="btn btn-ghost btn-wide" data-action="pc-open" data-id="${machine.id}">Назад к ПК</button>`;
      this.modalRoot.innerHTML = this.modalShell('PC OS Installer', 'РЕАЛЬНЫЕ ТРЕБОВАНИЯ // EXPERIMENTAL MODE', body);
    };
    proto.pcBenchmarkChart = function (result) {
      if (!result) return '';
      const max = Math.max(60, ...result.samples); const points = result.samples.map((value, index) => `${8 + index / (result.samples.length - 1) * 284},${68 - value / max * 55}`).join(' ');
      return `<svg class="pc-benchmark-chart" viewBox="0 0 300 78"><path d="M8 13h284M8 40h284M8 68h284" stroke="#26394b"/><polygon points="8,68 ${points} 292,68" fill="#55f6d2" opacity=".08"/><polyline points="${points}" fill="none" stroke="#55f6d2" stroke-width="2"/></svg>`;
    };
    proto.renderPCBenchmarks = function () {
      const machine = this.game.getPC(this.modal.pcId); if (!machine) { this.closeModal(); return; }
      const body = `<div class="pc-bench-summary"><span>${svgIcon('gpu')}</span><div><strong>${esc(machine.cpu)}</strong><small>${esc(machine.gpu)} · ${mb(machine.ramMb)} RAM</small></div><b>${machine.powered ? 'READY' : 'POWER OFF'}</b></div><div class="pc-benchmark-list">${Object.values(DATA.PC_BENCHMARKS).map(test => { const result = machine.benchmarks[test.id]; const future = test.year > this.game.state.pc.year; return `<article class="pc-benchmark ${result ? 'has-result' : ''}"><div class="pc-benchmark-head"><span>${svgIcon(test.icon)}</span><div><strong>${esc(test.name)}</strong><small>${test.year}${future ? ' · FUTURE TEST / EXPERIMENT' : ''} · цель ${test.target}${test.id === 'stability' ? ' points' : ' FPS'}</small></div>${result ? `<b class="${result.passed ? 'good' : 'bad'}">${result.fps}${test.id === 'stability' ? '' : ' FPS'}</b>` : ''}</div><p>${esc(test.description)} · требования CPU ${test.cpu} / GPU ${test.gpu} / RAM ${mb(test.ramMb)}</p>${this.pcBenchmarkChart(result)}<button class="btn btn-small ${result ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="pc-run-benchmark" data-id="${machine.id}" data-benchmark="${test.id}" ${machine.powered && !machine.crashed ? '' : 'disabled'}>${result ? 'Повторить · 30 мин' : 'Запустить · 30 мин'}</button></article>`; }).join('')}</div><button class="btn btn-ghost btn-wide" data-action="pc-open" data-id="${machine.id}">Назад к ПК</button>`;
      this.modalRoot.innerHTML = this.modalShell('PC Benchmark Library', '11 ТЕСТОВ // FPS // STABILITY', body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('pc-benchmark-sheet');
    };

    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () {
      if (!this.modal) return baseRenderModal.call(this);
      if (this.modal.type === 'pcDetail') { this.renderPCDetail(); return; }
      if (this.modal.type === 'pcCleaning') { this.renderPCCleaning(); return; }
      if (this.modal.type === 'pcOSInstall') { this.renderPCOSInstall(); return; }
      if (this.modal.type === 'pcBenchmarks') { this.renderPCBenchmarks(); return; }
      return baseRenderModal.call(this);
    };
    const baseHandleConfirmed = proto.handleConfirmed;
    proto.handleConfirmed = function (action, id) {
      if (action === 'pc-sell') { this.closeModal(); this.game.sellPC(id); this.refresh(false); return; }
      return baseHandleConfirmed.call(this, action, id);
    };
    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      const own = ['pc-open','pc-power','pc-reboot','pc-diagnostic','pc-repair','pc-clean-open','pc-clean','pc-os-open','pc-install-os','pc-bench-open','pc-run-benchmark','pc-complete-job','pc-accept-job','pc-dumpster-search','pc-sell-confirm','pc-upgrade','pc-speed','pc-open-desktop','pc-go-jobs','pc-go-dumpster','pc-open-upgrades'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'pc-open': this.openPC(id); return;
        case 'pc-power': this.game.togglePCPower(id); this.refresh(); return;
        case 'pc-reboot': this.game.rebootPC(id); this.refresh(); return;
        case 'pc-diagnostic': this.game.runPCDiagnostic(id, element.dataset.test); this.refresh(); return;
        case 'pc-repair': this.game.repairPCIssue(id, element.dataset.issue, { buy: element.dataset.buy === '1' }); this.refresh(); return;
        case 'pc-clean-open': this.openModal('pcCleaning', { pcId: id }); return;
        case 'pc-clean': this.game.cleanPCDust(id, Number(element.dataset.spot)); this.refresh(); return;
        case 'pc-os-open': this.openModal('pcOSInstall', { pcId: id }); return;
        case 'pc-install-os': this.game.installPCOS(id, element.dataset.os, element.dataset.experimental === '1'); this.refresh(); return;
        case 'pc-bench-open': this.openModal('pcBenchmarks', { pcId: id }); return;
        case 'pc-run-benchmark': this.game.runPCBenchmark(id, element.dataset.benchmark); this.refresh(); return;
        case 'pc-complete-job': if (this.game.completePCJob(id)) { this.closeModal(); this.pcTab = 'pc-jobs'; } this.refresh(); return;
        case 'pc-accept-job': if (this.game.acceptPCJob(id)) this.openPC(element.dataset.pc); else this.refresh(); return;
        case 'pc-dumpster-search': this.game.searchDumpster(); this.refresh(false); return;
        case 'pc-sell-confirm': this.openConfirm('Продать этот ПК?', 'Машина исчезнет из мастерской. Цена зависит от исправности, здоровья и характеристик.', 'pc-sell', id, true); return;
        case 'pc-upgrade': this.game.buyPCUpgrade(element.dataset.kind); this.refresh(false); return;
        case 'pc-speed': this.game.setPCSpeed(Number(element.dataset.speed)); this.refresh(false); return;
        case 'pc-open-desktop': this.openPCOS(id); return;
        case 'pc-go-jobs': this.pcTab = 'pc-jobs'; this.render(); return;
        case 'pc-go-dumpster': this.pcTab = 'pc-dumpster'; this.render(); return;
        case 'pc-open-upgrades': this.pcTab = 'pc-settings'; this.render(); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };
    const baseOnTick = proto.onTick;
    proto.onTick = function () {
      const previousYear = this.game.state.pc.year;
      const previousDay = this.game.state.pc.day;
      const result = baseOnTick.call(this);
      if (this.game.state.campaignMode === 'pc') {
        if (this.liveCounter % 3 === 0 || previousYear !== this.game.state.pc.year) { this.renderHUD(); this.renderNav(); }
        if (previousDay !== this.game.state.pc.day) { this.renderView(); this.renderNav(); }
        const clock = $('[data-live="pc-clock"]'); if (clock) clock.textContent = this.game.pcClock();
        if (this.modal && this.modal.type === 'pcDetail' && this.liveCounter % 5 === 0) this.renderModal();
      }
      return result;
    };
    proto.pcUILoaded = true;
  });
})();
