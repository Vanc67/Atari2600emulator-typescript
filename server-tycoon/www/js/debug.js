/* Secret Debug Console: подробная необязательная телеметрия без внешней передачи данных. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || Game.prototype.debugLoaded) return;
  const proto = Game.prototype;
  const now = () => typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now();

  class TycoonDebug {
    constructor(game) {
      this.game = game;
      this.entries = [];
      this.sequence = 0;
      this.enabled = false;
      this.paused = false;
      this.maxEntries = 1200;
      this.startedAt = now();
    }
    log(level, category, message, data = null) {
      if (this.paused || (!this.enabled && level === 'trace')) return;
      const state = this.game.state || {};
      this.entries.push({ id: ++this.sequence, at: now() - this.startedAt, day: state.day || 0,
        minute: state.minute || 0, year: state.currentYear || 2026, level, category, message: String(message), data });
      if (this.entries.length > this.maxEntries) this.entries.splice(0, this.entries.length - this.maxEntries);
    }
    clear() { this.entries.length = 0; this.log('debug', 'debug', 'BUFFER_CLEARED'); }
    snapshot() {
      const game = this.game;
      const state = game.state;
      const servers = state.servers.map(server => {
        const m = game.metrics(server);
        return { id: server.id, name: server.name, templateId: server.templateId, os: server.os, status: server.status,
          powered: server.powered, temp: server.temp, health: server.health, software: server.software.slice(),
          aiModel: server.aiModel, stress: server.stress, metrics: m };
      });
      return {
        generatedAt: new Date().toISOString(), version: state.version, timeline: game.timelineInfo(),
        farm: game.farmMetrics(), room: game.room(), network: game.network(),
        money: state.money, debt: state.debt, research: state.research, stats: state.stats,
        servers, catalog: { servers: DATA.SERVER_TEMPLATES.length, parts: DATA.PARTS.length, services: DATA.SOFTWARE.length, os: DATA.OS_LIST.length },
        runtime: {
          userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : 'node', platform: typeof navigator !== 'undefined' ? navigator.platform : 'node',
          viewport: typeof window !== 'undefined' ? { width: window.innerWidth, height: window.innerHeight, dpr: window.devicePixelRatio } : null,
          domNodes: typeof document !== 'undefined' ? document.querySelectorAll('*').length : 0,
          saveBytes: JSON.stringify(state).length,
          audio: window.tycoonSoundscape ? { enabled: game.state.settings.serverSounds, sessions: window.tycoonSoundscape.sessions.size,
            context: window.tycoonSoundscape.context && window.tycoonSoundscape.context.state } : null,
          memory: typeof performance !== 'undefined' && performance.memory ? { used: performance.memory.usedJSHeapSize, total: performance.memory.totalJSHeapSize } : null
        },
        stateJson: JSON.stringify(state)
      };
    }
    exportText() {
      return this.entries.map(entry => {
        const data = entry.data == null ? '' : ` ${typeof entry.data === 'string' ? entry.data : JSON.stringify(entry.data)}`;
        return `[${entry.at.toFixed(1)}ms D${entry.day} ${entry.year} ${entry.level.toUpperCase()} ${entry.category}] ${entry.message}${data}`;
      }).join('\n');
    }
  }

  function debugOf(game) {
    if (!game.debug) game.debug = new TycoonDebug(game);
    return game.debug;
  }
  window.TycoonDebug = TycoonDebug;
  window.tycoonDebugOf = debugOf;

  const baseNormalize = proto.normalize;
  proto.normalize = function () {
    const debug = debugOf(this);
    const result = baseNormalize.call(this);
    debug.log('debug', 'state', 'STATE_NORMALIZED', { version: this.state.version, year: this.state.currentYear,
      servers: this.state.servers.length, inventory: this.state.inventory.length, saveBytes: JSON.stringify(this.state).length });
    return result;
  };

  const baseLog = proto.log;
  proto.log = function (text, type = 'info') {
    debugOf(this).log(type === 'bad' ? 'warn' : 'info', 'game-log', text);
    return baseLog.call(this, text, type);
  };
  const baseNotify = proto.notify;
  proto.notify = function (text, type = 'info') {
    debugOf(this).log(type === 'error' ? 'error' : type === 'warning' ? 'warn' : 'info', 'event', text);
    return baseNotify.call(this, text, type);
  };

  const baseTick = proto.tick;
  proto.tick = function () {
    const debug = debugOf(this);
    if (debug.enabled) debug.log('trace', 'tick', 'TICK_BEGIN', { day: this.state.day, minute: this.state.minute,
      paused: this.state.paused, powerOut: this.state.powerOut, servers: this.state.servers.length });
    const started = now();
    const result = baseTick.call(this);
    if (debug.enabled) debug.log('trace', 'tick', 'TICK_END', { durationMs: Math.round((now() - started) * 100) / 100, farm: this.farmMetrics() });
    return result;
  };

  const baseSimulate = proto.simulateServer;
  proto.simulateServer = function (server) {
    const debug = debugOf(this);
    const before = debug.enabled ? { temp: server.temp, status: server.status, powered: server.powered,
      throttling: server.throttling, ramPressure: server.ramPressure } : null;
    const result = baseSimulate.call(this, server);
    if (debug.enabled) {
      const m = this.metrics(server);
      debug.log('trace', 'server', `SIM ${server.name}`, { id: server.id, before,
        after: { temp: server.temp, status: server.status, powered: server.powered, throttling: server.throttling,
          crashed: server.crashed, hung: server.hung, ramPressure: server.ramPressure },
        metrics: { cpuLoad: m.cpuLoad, cpuDemand: m.cpuDemand, ramUsed: m.ramUsed, ramTotal: m.ramTotal,
          ramPercent: m.ramPercent, watts: m.watts, noise: m.noise, targetTemp: m.targetTemp, efficiency: m.efficiency } });
    }
    return result;
  };

  const baseSave = proto.save;
  proto.save = function () {
    const debug = debugOf(this);
    const started = debug.enabled ? now() : 0;
    const result = baseSave.call(this);
    if (debug.enabled) debug.log('trace', 'save', 'STATE_SAVED', { bytes: JSON.stringify(this.state).length,
      durationMs: Math.round((now() - started) * 100) / 100, key: 'server-tycoon-save-v4' });
    return result;
  };

  const baseCommand = proto.executeCommand;
  proto.executeCommand = function (serverId, input) {
    const debug = debugOf(this);
    if (String(input).trim().toLowerCase() === 'debug') {
      debug.enabled = true;
      debug.log('debug', 'debug', 'SECRET_CONSOLE_UNLOCKED', { serverId });
      return { ok: true, lines: ['Debug Console unlocked · 1200-entry ring buffer'], debug: true };
    }
    if (debug.enabled) debug.log('trace', 'command', 'COMMAND_INPUT', { serverId, input: String(input) });
    const result = baseCommand.call(this, serverId, input);
    if (debug.enabled) debug.log('trace', 'command', 'COMMAND_RESULT', { serverId, ok: result.ok, lines: result.lines });
    return result;
  };

  const instrumented = ['togglePower', 'buyListing', 'buyCatalogServer', 'installSoftware', 'uninstallSoftware',
    'installAIModel', 'startStress', 'stopStress', 'writeImage', 'finishOSInstall', 'buyResearch', 'upgradeNetwork', 'buyUpgrade'];
  instrumented.forEach(method => {
    const base = proto[method];
    if (typeof base !== 'function') return;
    proto[method] = function (...args) {
      const debug = debugOf(this);
      if (debug.enabled) debug.log('trace', 'action', `${method.toUpperCase()}_BEGIN`, { args });
      const result = base.apply(this, args);
      if (debug.enabled) {
        let outcome = result;
        if (result && typeof result === 'object') outcome = { ok: result.ok, message: result.message };
        debug.log('trace', 'action', `${method.toUpperCase()}_END`, { result: outcome });
      }
      return result;
    };
  });

  proto.openDebugSnapshot = function () {
    const debug = debugOf(this);
    debug.enabled = true;
    const snapshot = debug.snapshot();
    debug.log('debug', 'snapshot', 'FULL_STATE_SNAPSHOT', snapshot);
    return snapshot;
  };

  proto.debugLoaded = true;
})();
