/* Deep Lab 6.0 UI: протоколы, SVG-графики, отчёты, открытия и fleet operations. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !window.DATA.LAB_PROTOCOLS) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.deepLabUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0);
    const money = value => `${round(value).toLocaleString('ru-RU')} ₽`;
    const compact = value => value >= 1000000 ? `${(value / 1000000).toFixed(1)}M` : value >= 1000 ? `${round(value / 1000)}K` : String(round(value));
    const statusName = status => ({ success: 'УСПЕХ', failed: 'ПРОВАЛ', aborted: 'ОСТАНОВЛЕН' }[status] || status);

    function sparkline(samples, key, color = '#55f6d2', maximum) {
      if (!samples || !samples.length) return '<div class="lab-chart-empty">Первые измерения появятся после тика</div>';
      const values = samples.map(sample => Number(sample[key]) || 0);
      const max = maximum || Math.max(1, ...values) * 1.12;
      const points = values.map((value, index) => {
        const x = values.length === 1 ? 150 : 8 + index / (values.length - 1) * 284;
        const y = 66 - Math.min(1, value / max) * 54;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      }).join(' ');
      const area = `8,66 ${points} ${values.length === 1 ? '150' : '292'},66`;
      return `<svg class="lab-sparkline" viewBox="0 0 300 76" role="img" aria-label="График ${esc(key)}"><path d="M8 12H292M8 39H292M8 66H292" stroke="#223448" stroke-width="1"/><polygon points="${area}" fill="${color}" opacity=".09"/><polyline points="${points}" fill="none" stroke="${color}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/><text x="8" y="9" fill="#7890a5" font-size="7">MAX ${Math.round(Math.max(...values) * 10) / 10}</text><text x="242" y="74" fill="#526a7e" font-size="6">${values.length} SAMPLES</text></svg>`;
    }

    function reportCard(report) {
      return `<button class="lab-report-row status-${report.status}" data-action="open-lab-report" data-report="${report.id}"><span class="lab-report-status">${statusName(report.status)}</span><span><strong>${esc(report.name)}</strong><small>${esc(report.serverName)} · D${report.day} ${report.year}</small></span><b>${report.score}/100</b></button>`;
    }

    proto.labHubCard = function () {
      const lab = this.game.state.lab;
      const active = this.game.state.servers.filter(server => server.labExperiment).length;
      const discoveries = Object.keys(lab.discoveries).length;
      return `<div class="card deep-lab-hub"><div class="deep-lab-hub-head"><span class="deep-lab-logo">${svgIcon('stats')}</span><div><span>DEEP LAB 6.0</span><strong>${active ? `${active} протокол(а) идёт` : 'Экспериментальная лаборатория'}</strong><small>${discoveries}/8 открытий · ${lab.completed} успешных · ${lab.reports.length} отчётов</small></div><span class="badge ${active ? 'good' : 'info'}">${active ? 'LIVE' : 'READY'}</span></div><div class="deep-lab-hub-actions"><button class="btn btn-primary" data-action="open-lab-hub">${svgIcon('stats')}Открыть лабораторию</button><button class="btn btn-ghost" data-action="open-operations">${svgIcon('server')}Ферма</button><button class="btn btn-ghost" data-action="open-achievements">${svgIcon('star')}Награды</button></div></div>`;
    };
    const baseTimelineCard = proto.timelineCard;
    proto.timelineCard = function () { return baseTimelineCard.call(this) + this.labHubCard(); };

    proto.serverLabCard = function (server) {
      const experiment = server.labExperiment;
      const profile = experiment && DATA.LAB_PROTOCOLS[experiment.id];
      return `<div class="card server-lab-card"><h3 class="card-title"><span>${svgIcon('stats')}Deep Lab 6.0</span><span class="badge ${experiment ? 'good' : 'info'}">${experiment ? `${experiment.elapsed}/${experiment.duration}` : `${Object.keys(server.labBadges).length}/8`}</span></h3><p class="muted tiny">${profile ? `${esc(profile.name)}: идут живые измерения, доход временно остановлен.` : '8 протоколов, графики, отчёты и постоянные открытия. Разгон и вентиляторы влияют на настоящую симуляцию.'}</p><button class="btn ${experiment ? 'btn-amber' : 'btn-cyan'} btn-wide" data-action="open-deep-lab" data-id="${server.id}">${svgIcon('stats')}${experiment ? 'Открыть живой эксперимент' : 'Запустить протокол / настроить'}</button></div>`;
    };
    const baseOverview = proto.overviewTab;
    proto.overviewTab = function (server) { return baseOverview.call(this, server) + this.serverLabCard(server); };

    proto.openDeepLab = function (serverId) { if (this.game.getServer(serverId)) this.openModal('deepLab', { serverId }); };
    proto.renderDeepLab = function () {
      const server = this.game.getServer(this.modal.serverId);
      if (!server) { this.closeModal(); return; }
      const m = this.game.metrics(server);
      const experiment = server.labExperiment;
      let body = `<div class="lab-server-strip"><span>${svgIcon('server')}</span><div><strong>${esc(server.name)}</strong><small>${esc(server.chassis.name)} · ${server.os ? esc(DATA.os(server.os).name) : 'NO OS'}</small></div><button class="btn btn-small btn-ghost" data-action="open-lab-hub">Все серверы</button></div>`;
      if (experiment) {
        const profile = DATA.LAB_PROTOCOLS[experiment.id];
        const samples = experiment.samples;
        const special = experiment.id === 'network' ? `<div><span>THROUGHPUT</span><strong>${m.labThroughput} Мбит/с</strong></div><div><span>PACKET LOSS</span><strong>${m.packetLoss.toFixed(1)}%</strong></div>` : experiment.id === 'storage' || experiment.id === 'raid' ? `<div><span>IOPS</span><strong>${m.labIops}</strong></div><div><span>RAID ЁМКОСТЬ</span><strong>${this.game.raidCapacity(server)} ГБ</strong></div>` : experiment.id === 'ai' ? `<div><span>INFERENCE</span><strong>${m.tokensPerSecond} tok/s</strong></div><div><span>МОДЕЛЬ</span><strong>${esc((DATA.AI_MODELS[server.aiModel] || {}).name || '—')}</strong></div>` : `<div><span>ОХЛАЖДЕНИЕ</span><strong>${round(m.cooling)}</strong></div><div><span>СОСТОЯНИЕ</span><strong>${server.throttling ? 'THROTTLE' : 'STABLE'}</strong></div>`;
        body += `<div class="lab-live-card"><div class="lab-live-head"><div><span>LIVE PROTOCOL · ДОХОД 0</span><strong>${esc(profile.name)}</strong><small>${esc(profile.objective)}</small></div><b>${experiment.elapsed}/${experiment.duration}</b></div><div class="meter lab-progress"><i style="width:${experiment.elapsed / experiment.duration * 100}%"></i></div><div class="lab-live-metrics"><div><span>TEMP</span><strong>${server.temp.toFixed(1)}°C</strong></div><div><span>CPU</span><strong>${round(m.cpuLoad)}%</strong></div><div><span>RAM</span><strong>${round(m.ramPercent)}%</strong></div><div><span>POWER</span><strong>${round(m.watts)}W</strong></div>${special}</div><div class="lab-chart-grid"><div><span>TEMPERATURE</span>${sparkline(samples, 'temp', '#ff7d72', 110)}</div><div><span>CPU / LOAD</span>${sparkline(samples, 'cpu', '#56d9ff', 110)}</div></div><button class="btn btn-amber btn-wide" data-action="stop-lab" data-id="${server.id}">Остановить и сохранить отчёт</button></div>`;
      } else {
        body += `<div class="lab-discovery-line"><span>ОТКРЫТИЯ ЭТОЙ МАШИНЫ</span><strong>${Object.keys(server.labBadges).length}/8</strong><span>+${server.overclock}% OC · ${esc(server.fanProfile)}</span></div><div class="lab-protocol-grid">${Object.values(DATA.LAB_PROTOCOLS).map(profile => {
          const check = this.game.canStartLab(server.id, profile.id);
          const discovered = server.labBadges[profile.id];
          const locked = profile.year > this.game.currentYear();
          return `<article class="lab-protocol ${discovered ? 'discovered' : ''} ${locked ? 'locked' : ''}"><div class="lab-protocol-head"><span>${svgIcon(profile.icon)}</span><div><strong>${esc(profile.name)}</strong><small>${profile.year} · ${profile.duration} тиков</small></div><b>${discovered ? 'OPEN' : 'NEW'}</b></div><p>${esc(profile.description)}</p><div class="lab-protocol-objective">${esc(profile.objective)}</div><div class="lab-protocol-reward">${esc(profile.reward)}</div><button class="btn btn-small ${discovered ? 'btn-ghost' : 'btn-primary'} btn-wide" data-action="start-lab" data-id="${server.id}" data-protocol="${profile.id}" ${check.ok ? '' : 'disabled'}>${check.ok ? discovered ? 'Повторить измерение' : 'Запустить протокол' : esc(check.message)}</button></article>`;
        }).join('')}</div>`;
      }
      body += `<div class="card lab-tuning-card"><h3 class="card-title"><span>${svgIcon('chip')}Hardware Tuning</span><span class="badge ${server.overclock >= 20 ? 'warning' : 'info'}">${server.powered ? 'LIVE' : 'BIOS'}</span></h3><div class="lab-tune-label"><span>CPU OVERCLOCK</span><small>${server.powered ? 'Выключите сервер для изменения' : '+30% после Overclock Stability'}</small></div><div class="lab-tune-buttons">${[0, 10, 20, 30].map(value => `<button class="${server.overclock === value ? 'active' : ''}" data-action="set-overclock" data-id="${server.id}" data-value="${value}" ${server.powered || experiment || value === 30 && !server.overclockUnlocked ? 'disabled' : ''}>${value ? `+${value}%` : 'STOCK'}</button>`).join('')}</div><div class="lab-tune-label"><span>FAN PROFILE</span><small>Применяется к температуре, шуму и ваттам</small></div><div class="lab-tune-buttons fans">${Object.values(DATA.LAB_FAN_PROFILES).map(fan => `<button class="${server.fanProfile === fan.id ? 'active' : ''}" data-action="set-fan" data-id="${server.id}" data-profile="${fan.id}" ${experiment ? 'disabled' : ''}>${esc(fan.name)}<small>${esc(fan.description)}</small></button>`).join('')}</div></div>`;
      const reports = this.game.state.lab.reports.filter(report => report.serverId === server.id).slice(0, 4);
      if (reports.length) body += `<div class="section-label"><span>Отчёты сервера</span><span>${reports.length} последних</span></div>${reports.map(reportCard).join('')}`;
      this.modalRoot.innerHTML = this.modalShell('Deep Lab', `${server.name} // REAL-TIME MEASUREMENT`, body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('deep-lab-sheet');
    };

    proto.renderLabHub = function () {
      const lab = this.game.state.lab;
      const active = this.game.state.servers.filter(server => server.labExperiment);
      let body = `<div class="lab-hub-hero"><span>DEEP LAB // 6.0</span><strong>${lab.completed}</strong><p>успешных протоколов · ${Object.keys(lab.discoveries).length}/8 технологий открыто</p><div class="lab-hub-stats"><div><span>ЗАПУСКИ</span><b>${lab.runs}</b></div><div><span>ПРОВАЛЫ</span><b>${lab.failed}</b></div><div><span>SAMPLES</span><b>${lab.totalSamples}</b></div></div></div><div class="btn-row lab-hub-buttons"><button class="btn btn-ghost" data-action="open-operations">${svgIcon('server')}Fleet Operations</button><button class="btn btn-ghost" data-action="open-achievements">${svgIcon('star')}Достижения ${Object.keys(this.game.state.achievements).length}/${DATA.LAB_ACHIEVEMENTS.length}</button></div>`;
      if (active.length) body += `<div class="section-label"><span>Сейчас измеряется</span><span>${active.length} LIVE</span></div>${active.map(server => `<button class="lab-active-server" data-action="open-deep-lab" data-id="${server.id}"><span class="lab-pulse"></span><div><strong>${esc(DATA.LAB_PROTOCOLS[server.labExperiment.id].name)}</strong><small>${esc(server.name)}</small></div><b>${server.labExperiment.elapsed}/${server.labExperiment.duration}</b></button>`).join('')}`;
      body += `<div class="section-label"><span>Выберите испытательный стенд</span><span>${this.game.state.servers.length} машин</span></div><div class="lab-server-list">${this.game.state.servers.map(server => {
        const ready = server.powered && server.os && !server.crashed && !server.hung && !server.broken;
        return `<button class="lab-server-choice" data-action="open-deep-lab" data-id="${server.id}"><span>${svgIcon('server')}</span><div><strong>${esc(server.name)}</strong><small>${ready ? server.os ? esc(DATA.os(server.os).name) : 'BIOS' : esc(server.status)} · ${Object.keys(server.labBadges).length}/8 открытий</small></div><span class="badge ${ready ? 'good' : ''}">${server.labExperiment ? 'LIVE' : ready ? 'READY' : 'OFF'}</span></button>`;
      }).join('')}</div>`;
      if (lab.reports.length) body += `<div class="section-label"><span>Последние отчёты</span><span>хранятся 12</span></div>${lab.reports.slice(0, 6).map(reportCard).join('')}`;
      this.modalRoot.innerHTML = this.modalShell('Deep Lab 6.0', 'ПРОТОКОЛЫ // ОТКРЫТИЯ // ОТЧЁТЫ', body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('deep-lab-sheet');
    };

    proto.renderLabReport = function () {
      const report = this.game.state.lab.reports.find(item => item.id === this.modal.reportId);
      if (!report) { this.openModal('labHub'); return; }
      const body = `<div class="lab-report-hero status-${report.status}"><span>${statusName(report.status)} · D${report.day} ${report.year}</span><strong>${report.score}</strong><b>/100</b><h3>${esc(report.name)}</h3><p>${esc(report.serverName)} · ${esc(report.reason)}</p></div>${report.discovery ? `<div class="notice success lab-discovery-notice">${svgIcon('star')}<div><strong>Новая технология открыта</strong>+1 RP · +${money(report.rewardMoney)} · +${report.rewardXP} XP</div></div>` : ''}<div class="lab-live-metrics report-metrics"><div><span>PEAK TEMP</span><strong>${report.peaks.temp}°C</strong></div><div><span>PEAK CPU</span><strong>${report.peaks.cpu}%</strong></div><div><span>PEAK RAM</span><strong>${report.peaks.ram}%</strong></div><div><span>PEAK POWER</span><strong>${report.peaks.watts}W</strong></div></div>${report.protocolId === 'network' ? `<div class="notice">${svgIcon('network')}<div><strong>${report.averages.throughput} Мбит/с · loss ${report.averages.packetLoss}%</strong>Средние значения за ${report.elapsed} измерений.</div></div>` : ''}${report.protocolId === 'storage' || report.protocolId === 'raid' ? `<div class="notice">${svgIcon('raid')}<div><strong>${report.averages.iops} IOPS · ${report.averages.throughput} МБ/с</strong>Ёмкость: ${report.raidBefore} → ${report.raidAfter} ГБ во время отказа → ${report.raidRestored} ГБ после восстановления.</div></div>` : ''}${report.protocolId === 'ai' ? `<div class="notice">${svgIcon('gpu')}<div><strong>${report.averages.tokens} tokens/s</strong>Средняя скорость локального inference.</div></div>` : ''}<div class="lab-chart-grid report-charts"><div><span>TEMPERATURE</span>${sparkline(report.samples, 'temp', '#ff7d72', 110)}</div><div><span>CPU / LOAD</span>${sparkline(report.samples, 'cpu', '#56d9ff', 110)}</div><div><span>RAM</span>${sparkline(report.samples, 'ram', '#b6ff56', 110)}</div><div><span>WATTS</span>${sparkline(report.samples, 'watts', '#f6c85f')}</div></div><div class="lab-sample-table">${report.samples.map(sample => `<div><span>#${sample.n}</span><b>${sample.temp}°</b><b>${sample.cpu}%</b><b>${sample.ram}%</b><b>${sample.watts}W</b></div>`).join('')}</div><button class="btn btn-primary btn-wide" data-action="lab-report-back" data-id="${report.serverId}">Вернуться в лабораторию</button>`;
      this.modalRoot.innerHTML = this.modalShell('Lab Report', `${report.name} // ${statusName(report.status)}`, body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('deep-lab-sheet');
    };

    proto.renderOperations = function () {
      const farm = this.game.farmMetrics();
      const room = this.game.room();
      const body = `<div class="operations-hero"><span>FLEET OPERATIONS</span><strong>${farm.active}/${this.game.state.servers.length}</strong><p>серверов под питанием · ${this.game.network().name}</p></div><div class="lab-live-metrics"><div><span>ЛИНИЯ</span><strong>${round(farm.watts)}/${room.powerLimit}W</strong></div><div><span>ШУМ</span><strong>${round(farm.noise)}/${room.noiseLimit} дБ</strong></div><div><span>МАКС. TEMP</span><strong>${round(farm.temp)}°C</strong></div><div><span>ДОХОД</span><strong>${money(farm.hourlyRevenue)}/ч</strong></div></div><div class="btn-row operations-actions"><button class="btn btn-primary" data-action="power-all-on">${svgIcon('power')}Запустить всё</button><button class="btn btn-danger" data-action="power-all-off">${svgIcon('power')}Остановить всё</button></div><p class="muted tiny">Запуск последовательно проверяет БП и лимит линии. Остановка сохраняет отчёты активных протоколов.</p><div class="section-label"><span>Серверы и обслуживание</span><span>${this.game.state.stats.services} сервисов</span></div>${this.game.state.servers.map(server => {
        const m = this.game.metrics(server);
        const canService = !server.powered && !server.broken && !server.crashed && !server.hung;
        return `<article class="operation-server"><button class="operation-server-main" data-action="open-deep-lab" data-id="${server.id}"><span class="operation-led ${server.powered ? 'online' : ''}"></span><div><strong>${esc(server.name)}</strong><small>${esc(server.status)} · ${server.temp.toFixed(1)}°C · ${round(m.watts)}W · +${server.overclock}% OC</small></div><span>${svgIcon('stats')}</span></button><button class="btn btn-small btn-ghost" data-action="service-server" data-id="${server.id}" ${canService ? '' : 'disabled'}>${svgIcon('wrench')}Обслужить · 350/500 ₽</button></article>`;
      }).join('')}`;
      this.modalRoot.innerHTML = this.modalShell('Fleet Operations', 'ПИТАНИЕ // МОНИТОРИНГ // ОБСЛУЖИВАНИЕ', body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('deep-lab-sheet');
    };

    proto.renderAchievements = function () {
      const unlocked = this.game.state.achievements;
      const body = `<div class="achievements-hero"><span>RESEARCH & OPERATIONS</span><strong>${Object.keys(unlocked).length}/${DATA.LAB_ACHIEVEMENTS.length}</strong><p>Каждое достижение даёт 60 XP. Прогресс вычисляется из реального состояния.</p></div><div class="achievement-grid">${DATA.LAB_ACHIEVEMENTS.map(achievement => {
        const value = achievement.value(this.game);
        const done = Boolean(unlocked[achievement.id]);
        const progress = Math.min(100, value / achievement.target * 100);
        return `<article class="achievement ${done ? 'unlocked' : ''}"><span class="achievement-icon">${svgIcon(achievement.icon)}</span><div><strong>${esc(achievement.name)}</strong><p>${esc(achievement.description)}</p><div class="meter"><i style="width:${progress}%"></i></div><small>${done ? `Получено · день ${unlocked[achievement.id]}` : `${compact(value)} / ${compact(achievement.target)} · +60 XP`}</small></div><b>${done ? '✓' : '○'}</b></article>`;
      }).join('')}</div>`;
      this.modalRoot.innerHTML = this.modalShell('Достижения', '14 ИСПЫТАНИЙ СИСАДМИНА', body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('deep-lab-sheet');
    };

    const baseRenderModal = proto.renderModal;
    proto.renderModal = function () {
      if (!this.modal) return baseRenderModal.call(this);
      if (this.modal.type === 'deepLab') { this.renderDeepLab(); return; }
      if (this.modal.type === 'labHub') { this.renderLabHub(); return; }
      if (this.modal.type === 'labReport') { this.renderLabReport(); return; }
      if (this.modal.type === 'operations') { this.renderOperations(); return; }
      if (this.modal.type === 'achievements') { this.renderAchievements(); return; }
      return baseRenderModal.call(this);
    };
    const baseOnTick = proto.onTick;
    proto.onTick = function () {
      const beforeServer = this.modal && this.modal.type === 'deepLab' ? this.game.getServer(this.modal.serverId) : null;
      const wasRunning = Boolean(beforeServer && beforeServer.labExperiment);
      const hubWasRunning = Boolean(this.modal && this.modal.type === 'labHub' && this.game.state.servers.some(s => s.labExperiment));
      const result = baseOnTick.call(this);
      const labServer = this.modal && this.modal.type === 'deepLab' ? this.game.getServer(this.modal.serverId) : null;
      const hubRunning = Boolean(this.modal && this.modal.type === 'labHub' && this.game.state.servers.some(s => s.labExperiment));
      if (this.modal && (this.modal.type === 'operations' || this.modal.type === 'labHub' && (hubWasRunning || hubRunning) || labServer && (wasRunning || labServer.labExperiment))) this.renderModal();
      return result;
    };
    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      const own = ['open-deep-lab','open-lab-hub','start-lab','stop-lab','open-lab-report','lab-report-back','set-overclock','set-fan','open-operations','power-all-on','power-all-off','service-server','open-achievements'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      if (own.includes(action) && this.game.debug && this.game.debug.enabled) this.game.debug.log('trace', 'ui', 'DEEP_LAB_UI_ACTION', { action, id: id || null, protocol: element.dataset.protocol || null });
      switch (action) {
        case 'open-deep-lab': this.openDeepLab(id); return;
        case 'open-lab-hub': this.openModal('labHub'); return;
        case 'start-lab': this.game.startLabExperiment(id, element.dataset.protocol); this.refresh(); return;
        case 'stop-lab': this.game.stopLabExperiment(id); this.refresh(); return;
        case 'open-lab-report': this.openModal('labReport', { reportId: element.dataset.report }); return;
        case 'lab-report-back': this.openDeepLab(id); return;
        case 'set-overclock': this.game.setOverclock(id, Number(element.dataset.value)); this.refresh(); return;
        case 'set-fan': this.game.setFanProfile(id, element.dataset.profile); this.refresh(); return;
        case 'open-operations': this.openModal('operations'); return;
        case 'power-all-on': this.game.powerAll(true); this.refresh(); return;
        case 'power-all-off': this.game.powerAll(false); this.refresh(); return;
        case 'service-server': this.game.serviceServer(id); this.refresh(); return;
        case 'open-achievements': this.game.checkAchievements(); this.openModal('achievements'); return;
        default: return baseHandleAction.call(this, action, element, event);
      }
    };

    proto.deepLabUILoaded = true;
    ui.render();
    if (ui.modal) ui.renderModal();
  });
})();
