/* PC Architect+ 8.1: проекты, избранное, тюнинг и локальные OS-приложения. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || !DATA.PC_PART_CATALOG || Game.prototype.pcPlusLoaded) return;
  const proto = Game.prototype;
  const types = Object.keys(DATA.PC_PART_TYPES);
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(Number(value) || 0);
  const clone = value => JSON.parse(JSON.stringify(value));
  const uid = prefix => `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
  const unique = items => [...new Set(items)];
  const ocValues = [0, 10, 20];
  const fanValues = ['silent', 'balanced', 'turbo'];
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const randomFor = initial => { let x = initial >>> 0 || 1; return () => { x ^= x << 13; x ^= x >>> 17; x ^= x << 5; return (x >>> 0) / 4294967296; }; };

  function sanitizeTuning(source) {
    const tuning = Object.assign({ cpu: 0, gpu: 0, fan: 'balanced' }, source || {});
    tuning.cpu = ocValues.includes(Number(tuning.cpu)) ? Number(tuning.cpu) : 0;
    tuning.gpu = ocValues.includes(Number(tuning.gpu)) ? Number(tuning.gpu) : 0;
    tuning.fan = fanValues.includes(tuning.fan) ? tuning.fan : 'balanced';
    return tuning;
  }
  function appState(machine) {
    if (!machine.apps || typeof machine.apps !== 'object') machine.apps = {};
    const apps = machine.apps;
    apps.notes = String(apps.notes || '').slice(0, 20000);
    apps.notesRevision = Math.max(0, Number(apps.notesRevision) || 0);
    apps.notesUpdated = apps.notesUpdated && typeof apps.notesUpdated === 'object' ? apps.notesUpdated : null;
    apps.calcHistory = Array.isArray(apps.calcHistory) ? apps.calcHistory.slice(0, 12) : [];
    if (!apps.mines || typeof apps.mines !== 'object') apps.mines = {};
    apps.mines.games = Math.max(0, Number(apps.mines.games) || 0);
    apps.mines.wins = Math.max(0, Number(apps.mines.wins) || 0);
    apps.mines.bestSeconds = Number.isFinite(apps.mines.bestSeconds) ? Math.max(0, apps.mines.bestSeconds) : null;
    const board = apps.mines.board;
    if (board && typeof board === 'object') {
      board.width = 9; board.height = 9; board.mineTotal = 10;
      ['mines', 'revealed', 'flags'].forEach(key => { board[key] = Array.isArray(board[key]) ? unique(board[key].map(Number).filter(index => Number.isInteger(index) && index >= 0 && index < 81)) : []; });
      board.status = ['ready', 'playing', 'won', 'lost'].includes(board.status) ? board.status : 'ready';
      board.startedAt = Math.max(0, Number(board.startedAt) || 0); board.elapsed = Math.max(0, Number(board.elapsed) || 0);
      board.exploded = Number.isInteger(board.exploded) ? board.exploded : -1;
    } else apps.mines.board = null;
    return apps;
  }
  function machineState(machine) {
    appState(machine);
    if (machine.custom) {
      machine.tuning = sanitizeTuning(machine.tuning);
      machine.tuningRevision = Math.max(0, Number(machine.tuningRevision) || 0);
      machine.tuningHistory = Array.isArray(machine.tuningHistory) ? machine.tuningHistory.slice(0, 20) : [];
    }
  }

  proto.pcPlusState = function () {
    const architect = this.pcArchitectState();
    ['blueprints', 'favorites', 'comparison'].forEach(key => { if (!Array.isArray(architect[key])) architect[key] = []; });
    architect.favorites = unique(architect.favorites.filter(id => Boolean(this.pcPart(id))));
    architect.comparison = unique(architect.comparison.filter(id => Boolean(this.pcPart(id)))).slice(0, 3);
    architect.blueprints = architect.blueprints.filter(item => item && typeof item === 'object').map(item => {
      item.id = String(item.id || uid('blueprint')); item.name = String(item.name || 'Проект ПК').slice(0, 60);
      item.draft = this.pcPlusCleanDraft(item.draft); item.archived = Boolean(item.archived);
      item.createdDay = Math.max(1, Number(item.createdDay) || this.state.pc.day); item.createdYear = clamp(Number(item.createdYear) || this.state.pc.year, 1990, 2026);
      return item;
    });
    return architect;
  };
  proto.pcPlusCleanDraft = function (source) {
    const draft = Object.fromEntries(types.map(type => [type, null]));
    types.forEach(type => {
      const id = source && source[type]; const part = id ? this.pcPart(id) : null;
      if (part && part.type === type) draft[type] = id;
    });
    return draft;
  };
  const basePlusNormalize = proto.normalize;
  proto.normalize = function () {
    const result = basePlusNormalize.call(this);
    if (this.state.pc) {
      this.pcPlusState(); this.state.pc.computers.forEach(machineState);
      this.state.pc.offers.forEach(offer => { if (offer.pc) machineState(offer.pc); });
    }
    return result;
  };
  const basePlusStart = proto.startPCCampaign;
  proto.startPCCampaign = function () { const result = basePlusStart.call(this); this.pcPlusState(); this.state.pc.computers.forEach(machineState); return result; };
  const basePlusCreatePC = proto.createPC;
  proto.createPC = function (year, options) { const machine = basePlusCreatePC.call(this, year, options); machineState(machine); return machine; };
  const basePlusBuildCustom = proto.buildCustomPC;
  proto.buildCustomPC = function (options) { const result = basePlusBuildCustom.call(this, options); if (result && result.machine) machineState(result.machine); return result; };
  const basePlusBuildLaptop = proto.buildLaptopPC;
  if (basePlusBuildLaptop) proto.buildLaptopPC = function (options) { const result = basePlusBuildLaptop.call(this, options); if (result && result.machine) machineState(result.machine); return result; };

  proto.pcSaveBlueprint = function (name) {
    const architect = this.pcPlusState(); const draft = this.pcPlusCleanDraft(architect.draft);
    if (!Object.values(draft).some(Boolean)) { this.notify('Выберите хотя бы одну деталь для проекта', 'warning'); return null; }
    const blueprint = { id: uid('blueprint'), name: String(name || architect.name || 'Проект ПК').trim().slice(0, 60) || 'Проект ПК', draft: clone(draft), archived: false,
      createdDay: this.state.pc.day, createdYear: this.state.pc.year };
    architect.blueprints.unshift(blueprint); this.pcLog(`PC Architect: сохранён проект «${blueprint.name}».`, 'good'); this.save(); return blueprint;
  };
  proto.pcLoadBlueprint = function (id) {
    const architect = this.pcPlusState(); const blueprint = architect.blueprints.find(item => item.id === id);
    if (!blueprint || blueprint.archived) return false;
    architect.draft = this.pcPlusCleanDraft(blueprint.draft); architect.name = blueprint.name;
    const years = Object.values(architect.draft).filter(Boolean).map(partId => this.pcPart(partId).year);
    if (years.length) architect.selectedYear = Math.max(...years);
    this.notify(`Проект «${blueprint.name}» загружен · исходник сохранён`, 'success'); this.save(); return this.pcDraftCompatibility();
  };
  proto.pcDuplicateBlueprint = function (id) {
    const architect = this.pcPlusState(); const source = architect.blueprints.find(item => item.id === id); if (!source) return null;
    const copy = { id: uid('blueprint'), name: `${source.name} · копия`.slice(0, 60), draft: clone(source.draft), archived: false, createdDay: this.state.pc.day, createdYear: this.state.pc.year };
    architect.blueprints.unshift(copy); this.save(); return copy;
  };
  proto.pcArchiveBlueprint = function (id, archived) {
    const architect = this.pcPlusState(); const blueprint = architect.blueprints.find(item => item.id === id); if (!blueprint) return false;
    blueprint.archived = archived === undefined ? !blueprint.archived : Boolean(archived); this.save(); return blueprint.archived;
  };
  proto.pcExportBlueprint = function (id) {
    const blueprint = this.pcPlusState().blueprints.find(item => item.id === id); if (!blueprint) return '';
    return JSON.stringify({ format: 'server-tycoon-pc-blueprint', version: 1, name: blueprint.name, draft: this.pcPlusCleanDraft(blueprint.draft) }, null, 2);
  };
  proto.pcImportBlueprint = function (text) {
    let payload;
    try { payload = JSON.parse(String(text || '')); } catch (_) { this.notify('JSON проекта повреждён', 'error'); return null; }
    if (!payload || payload.format !== 'server-tycoon-pc-blueprint' || payload.version !== 1 || !payload.draft || typeof payload.draft !== 'object' || Array.isArray(payload.draft)) {
      this.notify('Это не проект PC Architect версии 1', 'error'); return null;
    }
    if (Object.keys(payload.draft).some(type => !types.includes(type))) { this.notify('В проекте есть неизвестная категория', 'error'); return null; }
    for (const type of types) {
      const id = payload.draft[type]; if (id == null) continue;
      const part = typeof id === 'string' ? this.pcPart(id) : null;
      if (!part || part.type !== type) { this.notify(`Деталь ${type} не найдена в локальном каталоге`, 'error'); return null; }
    }
    const architect = this.pcPlusState(); const draft = this.pcPlusCleanDraft(payload.draft);
    if (!Object.values(draft).some(Boolean)) { this.notify('Пустой проект не импортирован', 'warning'); return null; }
    const blueprint = { id: uid('blueprint'), name: String(payload.name || 'Импортированный проект').trim().slice(0, 60) || 'Импортированный проект', draft, archived: false,
      createdDay: this.state.pc.day, createdYear: this.state.pc.year };
    architect.blueprints.unshift(blueprint); this.pcLog(`PC Architect: импортирован проект «${blueprint.name}».`, 'good'); this.save(); return blueprint;
  };
  proto.pcToggleFavorite = function (id) {
    if (!this.pcPart(id)) return false; const architect = this.pcPlusState(); const index = architect.favorites.indexOf(id);
    if (index >= 0) architect.favorites.splice(index, 1); else architect.favorites.push(id);
    this.save(); return index < 0;
  };
  proto.pcToggleCompare = function (id) {
    const part = this.pcPart(id); if (!part) return false; const architect = this.pcPlusState(); const index = architect.comparison.indexOf(id);
    if (index >= 0) { architect.comparison.splice(index, 1); this.save(); return false; }
    const selected = architect.comparison.map(partId => this.pcPart(partId)).filter(Boolean);
    if (selected.some(item => item.type !== part.type)) { this.notify('Сравнивайте детали одной категории', 'warning'); return false; }
    if (selected.length >= 3) { this.notify('В сравнении максимум три детали', 'warning'); return false; }
    architect.comparison.push(id); this.save(); return true;
  };
  proto.pcCompareParts = function () { return this.pcPlusState().comparison.map(id => this.pcPart(id)).filter(Boolean); };

  proto.pcTuning = function (machine) { return machine && machine.custom ? sanitizeTuning(machine.tuning) : null; };
  proto.pcPerformanceScores = function (machine) {
    const tuning = this.pcTuning(machine);
    return { cpu: machine.cpuScore * (tuning ? 1 + tuning.cpu / 100 : 1), gpu: machine.gpuScore * (tuning ? 1 + tuning.gpu / 100 : 1) };
  };
  proto.pcStabilityPenalty = function (machine) { const tuning = this.pcTuning(machine); return tuning ? (tuning.cpu + tuning.gpu) * .18 : 0; };
  const basePlusMetrics = proto.pcMetrics;
  proto.pcMetrics = function (machine) {
    const result = basePlusMetrics.call(this, machine); const tuning = this.pcTuning(machine);
    if (!tuning || !machine.powered || tuning.cpu === 0 && tuning.gpu === 0 && tuning.fan === 'balanced') return result;
    const wattFactor = 1 + tuning.cpu * .0045 + tuning.gpu * .006;
    const baseWatts = result.watts; const baseCooling = Math.max(25, machine.cooling);
    const dust = Math.max(0, result.targetTemp - 24 - baseWatts * 42 / baseCooling);
    const fanCooling = tuning.fan === 'turbo' ? 1.25 : tuning.fan === 'silent' ? .82 : 1;
    const fanNoise = tuning.fan === 'turbo' ? 1.18 : tuning.fan === 'silent' ? .82 : 1;
    result.watts = baseWatts * wattFactor;
    result.targetTemp = 24 + result.watts * 42 / Math.max(25, machine.cooling * fanCooling) + dust;
    result.noise = clamp(result.noise * fanNoise + (tuning.cpu + tuning.gpu) * .03, 0, 100);
    result.performance = clamp(result.performance * (1 + (tuning.cpu + tuning.gpu) * .002), .05, 2);
    result.tuning = Object.assign({}, tuning); return result;
  };
  proto.pcTuningPreview = function (id, candidate) {
    const machine = this.getPC(id); if (!machine || !machine.custom) return null;
    const tuning = sanitizeTuning(candidate || machine.tuning); const preview = Object.assign({}, machine, { powered: true, crashed: false, load: 1, tuning });
    const metrics = this.pcMetrics(preview);
    return { tuning, metrics, psuHeadroom: machine.psuWatts - metrics.watts, stabilityPenalty: this.pcStabilityPenalty(preview), scores: this.pcPerformanceScores(preview) };
  };
  proto.setPCTuning = function (id, kind, value) {
    const machine = this.getPC(id); if (!machine || !machine.custom || machine.origin !== 'owned') return false;
    if (machine.powered) { this.notify('Для тюнинга выключите компьютер', 'warning'); return false; }
    const tuning = sanitizeTuning(machine.tuning);
    if (['cpu', 'gpu'].includes(kind)) { value = Number(value); if (!ocValues.includes(value)) return false; }
    else if (kind === 'fan') { if (!fanValues.includes(value)) return false; }
    else return false;
    if (tuning[kind] === value) return true;
    const candidate = Object.assign({}, tuning, { [kind]: value });
    if (['cpu', 'gpu'].includes(kind)) {
      const preview = this.pcTuningPreview(id, candidate);
      if (preview.metrics.watts > machine.psuWatts) { this.notify(`PSU ${machine.psuWatts}W недостаточен для разгона · нужно больше ${Math.ceil(preview.metrics.watts)}W`, 'error'); return false; }
    }
    machine.tuning = candidate; machine.tuningRevision = (machine.tuningRevision || 0) + 1;
    machine.tuningHistory = Array.isArray(machine.tuningHistory) ? machine.tuningHistory : [];
    machine.tuningHistory.unshift({ day: this.state.pc.day, year: this.state.pc.year, revision: machine.tuningRevision, kind, value });
    if (machine.tuningHistory.length > 20) machine.tuningHistory.length = 20;
    this.pcLog(`${machine.name}: тюнинг ${kind}=${value}, revision ${machine.tuningRevision}.`, 'good'); this.save(); return true;
  };
  const basePlusBenchmark = proto.runPCBenchmark;
  proto.runPCBenchmark = function (id, benchmarkId) {
    const result = basePlusBenchmark.call(this, id, benchmarkId); const machine = this.getPC(id);
    if (result && machine && machine.custom) {
      result.tuning = sanitizeTuning(machine.tuning); result.tuningRevision = machine.tuningRevision || 0; result.performanceScores = this.pcPerformanceScores(machine); this.save();
    }
    return result;
  };

  proto.pcSaveNotes = function (id, text) {
    const machine = this.getPC(id); if (!machine) return false; const apps = appState(machine);
    apps.notes = String(text == null ? '' : text).slice(0, 20000); apps.notesRevision += 1;
    apps.notesUpdated = { day: this.state.pc.day, year: this.state.pc.year, minute: this.state.pc.minute };
    this.save(); return true;
  };
  proto.pcRecordCalculation = function (id, expression, result) {
    const machine = this.getPC(id); if (!machine) return false; const apps = appState(machine);
    apps.calcHistory.unshift({ expression: String(expression).slice(0, 80), result: String(result).slice(0, 40), day: this.state.pc.day });
    if (apps.calcHistory.length > 12) apps.calcHistory.length = 12; this.save(); return true;
  };
  const neighbors = index => {
    const row = Math.floor(index / 9); const col = index % 9; const result = [];
    for (let dr = -1; dr <= 1; dr += 1) for (let dc = -1; dc <= 1; dc += 1) {
      if (!dr && !dc) continue; const r = row + dr; const c = col + dc;
      if (r >= 0 && r < 9 && c >= 0 && c < 9) result.push(r * 9 + c);
    }
    return result;
  };
  const adjacent = (board, index) => neighbors(index).filter(cell => board.mines.includes(cell)).length;
  proto.pcMinesNew = function (id) {
    const machine = this.getPC(id); if (!machine) return null; const apps = appState(machine);
    apps.mines.board = { width: 9, height: 9, mineTotal: 10, mines: [], revealed: [], flags: [], status: 'ready', startedAt: 0, elapsed: 0, exploded: -1 };
    this.save(); return apps.mines.board;
  };
  proto.pcMinesElapsed = function (board) {
    if (!board || board.status === 'ready') return 0;
    return board.status === 'playing' ? Math.max(0, Math.floor((Date.now() - board.startedAt) / 1000)) : board.elapsed;
  };
  proto.pcMinesFlag = function (id, index) {
    const machine = this.getPC(id); const board = machine && appState(machine).mines.board; index = Number(index);
    if (!board || !Number.isInteger(index) || index < 0 || index >= 81 || ['won', 'lost'].includes(board.status) || board.revealed.includes(index)) return false;
    if (board.flags.includes(index)) board.flags = board.flags.filter(cell => cell !== index); else board.flags.push(index);
    this.save(); return true;
  };
  proto.pcMinesReveal = function (id, index) {
    const machine = this.getPC(id); const board = machine && appState(machine).mines.board; index = Number(index);
    if (!board || !Number.isInteger(index) || index < 0 || index >= 81 || ['won', 'lost'].includes(board.status) || board.revealed.includes(index) || board.flags.includes(index)) return false;
    if (board.status === 'ready') {
      const safe = new Set([index, ...neighbors(index)]); const random = randomFor(hash(`${machine.id}:${index}:${Date.now()}`)); let guard = 0;
      while (board.mines.length < board.mineTotal && guard < 1000) { const cell = Math.floor(random() * 81); if (!safe.has(cell) && !board.mines.includes(cell)) board.mines.push(cell); guard += 1; }
      if (board.mines.length < board.mineTotal) for (let cell = 0; cell < 81 && board.mines.length < board.mineTotal; cell += 1) if (!safe.has(cell) && !board.mines.includes(cell)) board.mines.push(cell);
      board.mines.sort((a, b) => a - b); board.status = 'playing'; board.startedAt = Date.now();
    }
    if (board.mines.includes(index)) {
      board.revealed = unique([...board.revealed, ...board.mines]); board.status = 'lost'; board.exploded = index;
      board.elapsed = this.pcMinesElapsed(Object.assign({}, board, { status: 'playing' })); appState(machine).mines.games += 1;
      this.save(); return { status: 'lost', board };
    }
    const stack = [index];
    while (stack.length) {
      const cell = stack.pop(); if (board.revealed.includes(cell) || board.flags.includes(cell) || board.mines.includes(cell)) continue;
      board.revealed.push(cell);
      if (adjacent(board, cell) === 0) neighbors(cell).forEach(next => { if (!board.revealed.includes(next) && !board.mines.includes(next)) stack.push(next); });
    }
    if (board.revealed.length >= 81 - board.mineTotal) {
      board.status = 'won'; board.elapsed = this.pcMinesElapsed(Object.assign({}, board, { status: 'playing' })); board.flags = unique([...board.flags, ...board.mines]);
      const apps = appState(machine); apps.mines.games += 1; apps.mines.wins += 1;
      apps.mines.bestSeconds = apps.mines.bestSeconds == null ? board.elapsed : Math.min(apps.mines.bestSeconds, board.elapsed);
    }
    this.save(); return { status: board.status, board };
  };

  proto.pcPlusLoaded = true;
})();
