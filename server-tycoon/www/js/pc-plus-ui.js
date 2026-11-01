/* PC Architect+ 8.1 UI: проекты, сравнение, тюнинг и настоящие OS-приложения. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.pcPlusState) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.pcPlusUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const round = value => Math.round(Number(value) || 0);
    const money = value => `${round(value).toLocaleString('ru-RU')} ₽`;
    const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${Number((value / 1024).toFixed(1))} ГБ` : `${round(value)} МБ`;
    ui.pcBlueprintExport = ''; ui.pcBlueprintArchiveView = false;

    proto.pcBlueprintsView = function () {
      const architect = this.game.pcPlusState(); const archived = this.pcBlueprintArchiveView;
      const blueprints = architect.blueprints.filter(item => item.archived === archived);
      const cards = blueprints.map(blueprint => {
        const ids = Object.values(blueprint.draft).filter(Boolean); const years = ids.map(id => this.game.pcPart(id).year);
        return `<article class="plus-blueprint ${blueprint.archived ? 'archived' : ''}"><div><span>${svgIcon('files')}</span><div><strong>${esc(blueprint.name)}</strong><small>${ids.length}/8 деталей · ${years.length ? Math.max(...years) : blueprint.createdYear} · сохранён D${blueprint.createdDay}</small><code>${esc(blueprint.id)}</code></div></div><div class="plus-blueprint-actions">${blueprint.archived ? '' : `<button class="btn btn-small btn-primary" data-action="pc-blueprint-load" data-id="${blueprint.id}">Загрузить</button>`}<button class="btn btn-small btn-ghost" data-action="pc-blueprint-duplicate" data-id="${blueprint.id}">Копия</button><button class="btn btn-small btn-ghost" data-action="pc-blueprint-export" data-id="${blueprint.id}">JSON</button><button class="btn btn-small btn-ghost" data-action="pc-blueprint-archive" data-id="${blueprint.id}">${blueprint.archived ? 'Вернуть' : 'В архив'}</button></div></article>`;
      }).join('');
      return `<section class="plus-panel blueprint-panel"><div class="plus-panel-head"><div><span>LOCAL BLUEPRINTS</span><strong>Проекты сборок</strong><small>${architect.blueprints.filter(item => !item.archived).length} активных · ${architect.blueprints.filter(item => item.archived).length} в архиве</small></div><button class="btn btn-small btn-cyan" data-action="pc-blueprint-save">${svgIcon('plus')}Сохранить draft</button></div><div class="plus-blueprint-tabs"><button class="${!archived ? 'active' : ''}" data-action="pc-blueprint-tab" data-archived="0">Активные</button><button class="${archived ? 'active' : ''}" data-action="pc-blueprint-tab" data-archived="1">Архив · ничего не удаляется</button></div>${this.pcBlueprintExport ? `<label class="plus-json-box"><span>Экспортированный JSON · скопируйте в файл или сообщение</span><textarea readonly data-pc-blueprint-export>${esc(this.pcBlueprintExport)}</textarea></label>` : ''}<details class="plus-import"><summary>Импортировать проект JSON offline</summary><textarea data-pc-blueprint-import placeholder='{"format":"server-tycoon-pc-blueprint","version":1,...}'></textarea><button class="btn btn-small btn-primary" data-action="pc-blueprint-import">Добавить как новый проект</button></details><div class="plus-blueprint-list">${cards || `<p class="muted tiny">${archived ? 'Архив пуст. Проекты перемещаются сюда без удаления.' : 'Сохраните текущий draft: детали, название и год останутся в локальном save.'}</p>`}</div></section>`;
    };
    const comparisonRows = type => {
      const rows = [['Год', part => part.year], ['Цена', part => money(part.value)]];
      if (type === 'cpu') rows.push(['Сокет', part => part.socket], ['Ядра', part => part.cores], ['Частота', part => `${part.mhz} МГц`], ['CPU score', part => part.cpuScore], ['TDP', part => `${part.tdp} Вт`]);
      if (type === 'motherboard') rows.push(['Сокет', part => part.socket], ['RAM', part => `${part.ramType} ×${part.ramSlots}`], ['Формат', part => part.formFactor], ['GPU', part => part.gpuInterfaces.join('/')]);
      if (type === 'ram') rows.push(['Тип', part => part.ramType], ['Объём', part => mb(part.capacityMb)], ['Модули', part => part.modules], ['Частота', part => `${part.speed} МГц`]);
      if (type === 'gpu') rows.push(['Интерфейс', part => part.interface], ['GPU score', part => part.gpuScore], ['VRAM', part => part.vramMb < 1 ? `${part.vramMb * 1024} КБ` : mb(part.vramMb)], ['Длина', part => `${part.lengthMm} мм`], ['TDP', part => `${part.tdp} Вт`]);
      if (type === 'storage') rows.push(['Тип', part => part.storageType], ['Объём', part => mb(part.capacityMb)], ['Интерфейс', part => part.interface], ['Read score', part => part.readScore]);
      if (type === 'psu') rows.push(['Мощность', part => `${part.watts} Вт`], ['Формат', part => part.formFactor], ['КПД', part => `${part.efficiency}%`]);
      if (type === 'case') rows.push(['Платы', part => part.formFactors.join('/')], ['PSU', part => part.psuFormFactors.join('/')], ['GPU до', part => `${part.maxGpuMm} мм`], ['Кулер до', part => `${part.maxCoolerMm} мм`], ['Вентиляторы', part => part.fans]);
      if (type === 'cooling') rows.push(['Сокеты', part => part.sockets.join('/')], ['TDP до', part => `${part.capacityWatts} Вт`], ['Высота', part => `${part.heightMm} мм`], ['Тип', part => part.kind], ['Шум', part => `${part.noise} дБ`]);
      return rows;
    };
    proto.pcComparisonView = function () {
      const parts = this.game.pcCompareParts(); if (!parts.length) return '';
      const rows = comparisonRows(parts[0].type);
      return `<section class="plus-panel compare-panel"><div class="plus-panel-head"><div><span>PART COMPARATOR · ${parts.length}/3</span><strong>${esc(DATA.PC_PART_TYPES[parts[0].type].name)}</strong><small>Сравнение не меняет draft, пока не нажата кнопка «Выбрать».</small></div></div><div class="plus-compare-scroll"><div class="plus-compare-table" style="--compare-count:${parts.length}"><div class="plus-compare-row head"><b>Параметр</b>${parts.map(part => `<div><strong>${esc(part.name)}</strong><small>${esc(part.sku)}</small><button data-action="pc-compare" data-id="${part.id}">Убрать из сравнения</button><button class="btn btn-small btn-primary" data-action="pc-builder-select" data-type="${part.type}" data-id="${part.id}">Выбрать</button></div>`).join('')}</div>${rows.map(([label, getter]) => `<div class="plus-compare-row"><b>${esc(label)}</b>${parts.map(part => `<span>${esc(getter(part))}</span>`).join('')}</div>`).join('')}</div></div></section>`;
    };
    const basePlusBuilderView = proto.pcBuilderView;
    proto.pcBuilderView = function () { return basePlusBuilderView.call(this) + this.pcComparisonView() + this.pcBlueprintsView(); };

    proto.renderPCTuning = function () {
      const machine = this.game.getPC(this.modal && this.modal.pcId); if (!machine || !machine.custom) { this.closeModal(); return; }
      const tuning = this.game.pcTuning(machine); const preview = this.game.pcTuningPreview(machine.id, tuning); const m = preview.metrics;
      const group = (kind, values, labels) => `<div class="plus-tuning-group"><span>${kind === 'cpu' ? 'CPU Overclock' : kind === 'gpu' ? 'GPU Overclock' : 'Fan profile'}</span><div>${values.map(value => `<button class="${tuning[kind] === value ? 'active' : ''}" data-action="pc-tuning-set" data-kind="${kind}" data-value="${value}" ${machine.powered ? 'disabled' : ''}>${labels ? labels[value] : `+${value}%`}</button>`).join('')}</div></div>`;
      const body = `<div class="notice ${machine.powered ? 'warning' : ''}">${svgIcon('warning')}<div><strong>${machine.powered ? 'Выключите ПК для изменения' : 'Тюнинг с реальными последствиями'}</strong>Разгон повышает FPS, ватты и нагрев, но снижает Stability. Silent экономит шум ценой температуры, Turbo усиливает охлаждение и шум.</div></div>${group('cpu',[0,10,20])}${group('gpu',[0,10,20])}${group('fan',['silent','balanced','turbo'],{ silent:'Silent', balanced:'Balanced', turbo:'Turbo' })}<div class="plus-tuning-metrics"><div><span>CPU score</span><strong>${round(preview.scores.cpu)}</strong></div><div><span>GPU score</span><strong>${round(preview.scores.gpu)}</strong></div><div><span>Полная нагрузка</span><strong>${round(m.watts)}W</strong></div><div><span>Целевая темп.</span><strong>${round(m.targetTemp)}°C</strong></div><div><span>Шум</span><strong>${round(m.noise)} дБ</strong></div><div><span>Stability</span><strong>−${preview.stabilityPenalty.toFixed(1)}</strong></div></div><div class="notice ${preview.psuHeadroom < 0 ? 'error' : preview.psuHeadroom < 60 ? 'warning' : 'success'}">${svgIcon('power')}<div><strong>PSU ${machine.psuWatts}W · запас ${round(preview.psuHeadroom)}W</strong>${preview.psuHeadroom < 0 ? 'Такой разгон будет отклонён: нужен более мощный PSU.' : 'Проверка мощности выполняется при каждом изменении.'}</div></div><div class="section-label"><span>История тюнинга</span><span>revision ${machine.tuningRevision || 0}</span></div>${(machine.tuningHistory || []).slice(0, 5).map(item => `<div class="plus-history-row"><span>D${item.day} · rev ${item.revision}</span><strong>${esc(item.kind)} = ${esc(item.value)}</strong></div>`).join('') || '<p class="muted tiny">Заводские настройки. Старые benchmark-результаты сохраняются.</p>'}<button class="btn btn-ghost btn-wide" data-action="pc-open" data-id="${machine.id}">Назад к Custom PC</button>`;
      this.modalRoot.innerHTML = this.modalShell('Custom PC Tuning', `${machine.name} // OVERCLOCK + FAN + PSU`, body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('pc-detail-sheet', 'plus-tuning-sheet');
    };
    const basePlusDetail = proto.renderPCDetail;
    proto.renderPCDetail = function () {
      basePlusDetail.call(this); const machine = this.game.getPC(this.modal && this.modal.pcId);
      if (!machine || !machine.custom || machine.origin !== 'owned') return;
      const actions = $('.pc-detail-actions', this.modalRoot);
      if (actions) actions.insertAdjacentHTML('beforeend', `<button class="btn btn-amber" data-action="pc-tuning-open" data-id="${machine.id}">${svgIcon('stats')}Тюнинг · rev ${machine.tuningRevision || 0}</button>`);
    };
    const basePlusRenderModal = proto.renderModal;
    proto.renderModal = function () { if (this.modal && this.modal.type === 'pcTuning') { this.renderPCTuning(); return; } return basePlusRenderModal.call(this); };

    const mineNeighbors = index => { const row = Math.floor(index / 9); const col = index % 9; const cells = []; for (let dr = -1; dr <= 1; dr += 1) for (let dc = -1; dc <= 1; dc += 1) { if (!dr && !dc) continue; const r = row + dr; const c = col + dc; if (r >= 0 && r < 9 && c >= 0 && c < 9) cells.push(r * 9 + c); } return cells; };
    const mineCount = (board, index) => mineNeighbors(index).filter(cell => board.mines.includes(cell)).length;
    proto.pcCalculator = function () {
      if (!this.pcSession.calc) this.pcSession.calc = { display: '0', accumulator: null, operator: null, waiting: false, expression: '' };
      return this.pcSession.calc;
    };
    const calcFormat = value => Number.isFinite(value) ? String(Number.parseFloat(value.toPrecision(12))) : 'Ошибка';
    const calcApply = (left, right, operator) => operator === '+' ? left + right : operator === '-' ? left - right : operator === '*' ? left * right : right === 0 ? NaN : left / right;
    proto.pcPressCalculator = function (value) {
      const calc = this.pcCalculator();
      if (/^\d$/.test(value)) { calc.display = calc.waiting || calc.display === '0' || calc.display === 'Ошибка' ? value : `${calc.display}${value}`.slice(0, 18); calc.waiting = false; }
      else if (value === '.') { if (calc.waiting) { calc.display = '0.'; calc.waiting = false; } else if (!calc.display.includes('.')) calc.display += '.'; }
      else if (value === 'C') Object.assign(calc, { display: '0', accumulator: null, operator: null, waiting: false, expression: '' });
      else if (value === 'back') { if (!calc.waiting) calc.display = calc.display.length > 1 ? calc.display.slice(0, -1) : '0'; }
      else if (value === 'sign') calc.display = calcFormat(-Number(calc.display));
      else if (value === '%') calc.display = calcFormat(Number(calc.display) / 100);
      else if (['+', '-', '*', '/'].includes(value)) {
        const current = Number(calc.display); if (!Number.isFinite(current)) return;
        if (calc.operator && !calc.waiting) { calc.accumulator = calcApply(calc.accumulator, current, calc.operator); calc.display = calcFormat(calc.accumulator); }
        else calc.accumulator = current;
        calc.operator = value; calc.waiting = true; calc.expression = `${calcFormat(calc.accumulator)} ${value}`;
      } else if (value === '=' && calc.operator) {
        const left = calc.accumulator; const right = Number(calc.display); const operator = calc.operator; const result = calcApply(left, right, operator);
        calc.expression = `${calcFormat(left)} ${operator} ${calcFormat(right)} =`; calc.display = calcFormat(result);
        this.game.pcRecordCalculation(this.modal.pcId, calc.expression, calc.display);
        calc.accumulator = null; calc.operator = null; calc.waiting = true;
      }
    };
    proto.pcNotesContent = function (machine) {
      return `<div class="plus-notes"><div class="plus-app-toolbar"><span>LOCAL TXT · revision ${machine.apps.notesRevision}</span><button class="pc-native-button accent" data-action="pc-notes-save">Сохранить · Ctrl+S</button></div><textarea data-pc-notes maxlength="20000" placeholder="Заметки по сборке, пароли игровых проектов, результаты тестов…">${esc(this.pcSession.notes)}</textarea><small>${this.pcSession.notes.length}/20 000 · сохраняется на этом ПК, не отправляется в сеть.</small></div>`;
    };
    proto.pcCalculatorContent = function (machine) {
      const calc = this.pcCalculator(); const buttons = ['C','back','%','/','7','8','9','*','4','5','6','-','1','2','3','+','sign','0','.','='];
      const labels = { back:'⌫', sign:'±' };
      return `<div class="plus-calculator"><div class="plus-calc-display"><small>${esc(calc.expression || 'LOCAL CALC')}</small><strong>${esc(calc.display)}</strong></div><div class="plus-calc-grid">${buttons.map(value => `<button class="${['+','-','*','/'].includes(value) ? 'operator' : value === '=' ? 'equal' : ''}" data-action="pc-calc" data-value="${value}">${labels[value] || value}</button>`).join('')}</div>${machine.apps.calcHistory.length ? `<div class="plus-calc-history">${machine.apps.calcHistory.slice(0, 5).map(item => `<div><span>${esc(item.expression)}</span><strong>${esc(item.result)}</strong></div>`).join('')}</div>` : '<p class="muted tiny">Клавиатура тоже работает: цифры, Enter, Backspace и Escape.</p>'}</div>`;
    };
    proto.pcMinesContent = function (machine) {
      const stats = machine.apps.mines; const board = stats.board;
      if (!board) return `<div class="plus-app-empty"><button class="pc-native-button accent" data-action="pc-mines-new">Новая игра 9×9</button></div>`;
      const status = { ready:'Первый ход всегда безопасен', playing:'Поле открыто', won:'Победа · все мины отмечены', lost:'Взрыв · попробуйте ещё раз' }[board.status];
      const cells = Array.from({ length: 81 }, (_, index) => {
        const revealed = board.revealed.includes(index); const flag = board.flags.includes(index); const mine = board.mines.includes(index); const count = revealed && !mine ? mineCount(board, index) : 0;
        const text = revealed && mine ? '✹' : revealed ? count || '' : flag ? '⚑' : '';
        return `<button class="plus-mine-cell ${revealed ? 'revealed' : ''} ${revealed && mine ? 'mine' : ''} ${flag && !revealed ? 'flagged' : ''} ${board.exploded === index ? 'exploded' : ''} n${count}" data-action="pc-mines-reveal" data-cell="${index}" ${revealed || ['won','lost'].includes(board.status) ? 'disabled' : ''} aria-label="Клетка ${index + 1}">${text}</button>`;
      }).join('');
      return `<div class="plus-mines"><div class="plus-mines-status"><div><span>${status}</span><strong><b data-mines-time>${this.game.pcMinesElapsed(board)}</b> сек · ✹ ${board.mineTotal}</strong></div><b>${stats.wins}/${stats.games} побед${stats.bestSeconds == null ? '' : ` · рекорд ${stats.bestSeconds}с`}</b></div><div class="plus-mine-grid">${cells}</div><div class="plus-app-toolbar"><button class="pc-native-button" data-action="pc-mines-new">Новое поле</button><button class="pc-native-button ${this.pcSession.minesFlagMode ? 'accent' : ''}" data-action="pc-mines-flag-mode">⚑ Флажок: ${this.pcSession.minesFlagMode ? 'ON' : 'OFF'}</button></div><small>Режим флажка заменяет long press и удобен на сенсорном экране. Поле сохраняется при выходе.</small></div>`;
    };
    const basePlusAppWindow = proto.pcAppWindow;
    proto.pcAppWindow = function (machine, os) {
      const app = this.pcSession.app; if (!['notes', 'calculator', 'mines'].includes(app)) return basePlusAppWindow.call(this, machine, os);
      const content = app === 'notes' ? this.pcNotesContent(machine) : app === 'calculator' ? this.pcCalculatorContent(machine) : this.pcMinesContent(machine);
      const title = app === 'notes' ? 'Блокнот · Local Notes' : app === 'calculator' ? 'Калькулятор' : 'Сапёр · 9×9 / 10 мин';
      const icon = app === 'notes' ? 'files' : app === 'calculator' ? 'stats' : 'games';
      return `<div class="pc-app-window plus-app-window ${app}-window"><div class="pc-window-title"><span>${svgIcon(icon)} ${title}</span><div><button data-action="pc-os-close-app">—</button><button data-action="pc-os-close-app">×</button></div></div><div class="pc-window-content">${content}</div><div class="pc-window-status">LOCAL APP · SAVE ON THIS PC<span>${app === 'mines' ? 'FIRST CLICK SAFE' : 'OFFLINE'}</span></div></div>`;
    };

    const basePlusAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const id = element.dataset.id;
      if (action === 'pc-os-app' && ['notes', 'mines'].includes(element.dataset.app)) {
        const machine = this.game.getPC(this.modal.pcId);
        if (machine && element.dataset.app === 'notes') this.pcSession.notes = machine.apps.notes;
        if (machine && element.dataset.app === 'mines' && !machine.apps.mines.board) this.game.pcMinesNew(machine.id);
        return basePlusAction.call(this, action, element, event);
      }
      const own = ['pc-favorite','pc-compare','pc-blueprint-save','pc-blueprint-load','pc-blueprint-duplicate','pc-blueprint-export','pc-blueprint-archive','pc-blueprint-import','pc-blueprint-tab','pc-tuning-open','pc-tuning-set','pc-notes-save','pc-calc','pc-mines-new','pc-mines-flag-mode','pc-mines-reveal'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'pc-favorite': this.game.pcToggleFavorite(id); this.renderView(); return;
        case 'pc-compare': this.game.pcToggleCompare(id); this.renderView(); return;
        case 'pc-blueprint-save': this.game.pcSaveBlueprint(); this.renderView(); return;
        case 'pc-blueprint-load': if (this.game.pcLoadBlueprint(id)) { this.pcBuilderYear = this.game.pcPlusState().selectedYear; this.pcBuilderPage = 0; } this.renderView(); return;
        case 'pc-blueprint-duplicate': this.game.pcDuplicateBlueprint(id); this.renderView(); return;
        case 'pc-blueprint-export': this.pcBlueprintExport = this.game.pcExportBlueprint(id); this.renderView(); return;
        case 'pc-blueprint-archive': this.game.pcArchiveBlueprint(id); this.renderView(); return;
        case 'pc-blueprint-import': { const input = $('[data-pc-blueprint-import]', this.view); if (input && this.game.pcImportBlueprint(input.value)) this.pcBlueprintExport = ''; this.renderView(); return; }
        case 'pc-blueprint-tab': this.pcBlueprintArchiveView = element.dataset.archived === '1'; this.renderView(); return;
        case 'pc-tuning-open': this.openModal('pcTuning', { pcId: id }); return;
        case 'pc-tuning-set': this.game.setPCTuning(this.modal.pcId, element.dataset.kind, element.dataset.kind === 'fan' ? element.dataset.value : Number(element.dataset.value)); this.refresh(false); this.renderPCTuning(); return;
        case 'pc-notes-save': this.game.pcSaveNotes(this.modal.pcId, this.pcSession.notes); this.toast('Заметки сохранены на этом ПК', 'success'); this.renderModal(); return;
        case 'pc-calc': this.pcPressCalculator(element.dataset.value); this.renderModal(); return;
        case 'pc-mines-new': this.game.pcMinesNew(this.modal.pcId); this.pcSession.minesFlagMode = false; this.renderModal(); return;
        case 'pc-mines-flag-mode': this.pcSession.minesFlagMode = !this.pcSession.minesFlagMode; this.renderModal(); return;
        case 'pc-mines-reveal': if (this.pcSession.minesFlagMode) this.game.pcMinesFlag(this.modal.pcId, Number(element.dataset.cell)); else this.game.pcMinesReveal(this.modal.pcId, Number(element.dataset.cell)); this.renderModal(); return;
        default: return basePlusAction.call(this, action, element, event);
      }
    };
    document.addEventListener('input', event => { if (event.target.matches('[data-pc-notes]') && ui.pcSession) ui.pcSession.notes = event.target.value; });
    document.addEventListener('keydown', event => {
      if (event.target.matches('[data-pc-notes]') && event.ctrlKey && event.key.toLowerCase() === 's') { event.preventDefault(); ui.game.pcSaveNotes(ui.modal.pcId, ui.pcSession.notes); ui.toast('Заметки сохранены на этом ПК', 'success'); ui.renderModal(); return; }
      if (!ui.pcSession || ui.pcSession.app !== 'calculator' || !ui.modal || ui.modal.type !== 'pcOS' || /INPUT|TEXTAREA/.test(event.target.tagName)) return;
      const map = { Enter:'=', '=':'=', Escape:'C', Backspace:'back', '+':'+', '-':'-', '*':'*', '/':'/', '.':'.', '%':'%' };
      const value = /^\d$/.test(event.key) ? event.key : map[event.key]; if (!value) return;
      event.preventDefault(); ui.pcPressCalculator(value); ui.renderModal();
    });
    const basePlusTick = proto.onTick;
    proto.onTick = function () {
      const result = basePlusTick.call(this); const node = $('[data-mines-time]', this.modalRoot);
      if (node && this.modal && this.modal.type === 'pcOS') { const machine = this.game.getPC(this.modal.pcId); if (machine && machine.apps.mines.board) node.textContent = this.game.pcMinesElapsed(machine.apps.mines.board); }
      return result;
    };
    proto.pcPlusUILoaded = true;
  });
})();
