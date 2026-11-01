/* Debug Cheats 8.0: явные локальные читы без удаления игрового прогресса. */
(function () {
  'use strict';
  const Game = window.ServerTycoonGame;
  if (!Game || Game.prototype.cheatsLoaded) return;
  const proto = Game.prototype;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const number = value => Number(String(value == null ? '' : value).replace(/\s/g, '').replace(',', '.'));
  const round = value => Math.round(Number(value) || 0);
  function root(game) {
    if (!game.state.cheats) game.state.cheats = { history: [] };
    if (!Array.isArray(game.state.cheats.history)) game.state.cheats.history = [];
    if (game.state.pc && !game.state.pc.cheats) game.state.pc.cheats = { unlockAll: false };
    return game.state.cheats;
  }
  const baseNormalize = proto.normalize;
  proto.normalize = function () { const result = baseNormalize.call(this); root(this); return result; };
  const baseStart = proto.startPCCampaign;
  proto.startPCCampaign = function () { const result = baseStart.call(this); root(this); return result; };
  function record(game, target, kind, value) {
    const cheats = root(game); const text = `${target === 'pc' ? 'PC' : 'SERVER'} · ${kind}${value !== undefined ? ` · ${value}` : ''}`;
    cheats.history.unshift({ target, kind, value, day: game.state.day, year: target === 'pc' ? game.state.pc.year : game.state.currentYear });
    if (cheats.history.length > 40) cheats.history.length = 40;
    if (game.debug) game.debug.log('debug', 'cheat', text);
    game.notify(`Cheat: ${text}`, 'success'); game.save();
    return true;
  }
  function validTarget(game, target) { return target === 'servers' || target === 'pc' && game.state.pc && game.state.pc.started; }

  proto.cheatSetYear = function (year, target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    year = clamp(round(number(year) || 1990), 1990, 2026);
    if (target === 'pc') { this.state.pc.year = year; this.state.pc.yearDay = 0; this.state.pc.architect && (this.state.pc.architect.selectedYear = year); }
    else { this.state.startYear = year; this.state.currentYear = year; this.state.yearDay = 0; }
    return record(this, target, 'YEAR', year);
  };
  proto.cheatSetMoney = function (amount, target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    const value = Number(number(amount)); if (!Number.isFinite(value)) return false;
    const money = clamp(round(value), 0, 1000000000000);
    if (target === 'pc') this.state.pc.money = money; else this.state.money = money;
    return record(this, target, 'MONEY =', money);
  };
  proto.cheatAddMoney = function (amount, target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    const value = Number(number(amount)); if (!Number.isFinite(value)) return false;
    const current = target === 'pc' ? this.state.pc.money : this.state.money;
    return this.cheatSetMoney(current + round(value), target);
  };
  proto.cheatSetDay = function (day, target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    const value = clamp(round(number(day) || 1), 1, 1000000);
    if (target === 'pc') this.state.pc.day = value; else this.state.day = value;
    return record(this, target, 'DAY =', value);
  };
  proto.cheatSetLevel = function (level, target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    const value = clamp(round(number(level) || 1), 1, 100);
    if (target === 'pc') { this.state.pc.level = value; this.state.pc.xp = Math.max(this.state.pc.xp, (value - 1) * (value - 1) * 350); }
    else { this.state.level = value; this.state.xp = Math.max(this.state.xp, (value - 1) * (value - 1) * 300); }
    return record(this, target, 'LEVEL =', value);
  };
  proto.cheatSetReputation = function (value, target = 'pc') {
    if (!validTarget(this, target)) return false;
    const reputation = clamp(round(number(value)), 0, 1000000);
    if (target === 'pc') this.state.pc.reputation = reputation; else { this.state.stats.reputation = reputation; }
    return record(this, target, 'REPUTATION =', reputation);
  };
  proto.cheatRepairAll = function (target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    if (target === 'pc') this.state.pc.computers.forEach(machine => {
      machine.issues.forEach(issue => { issue.revealed = true; issue.resolved = true; });
      machine.health = 100; machine.temp = 24; machine.crashed = false; machine.hung = false;
    });
    else this.state.servers.forEach(server => {
      server.health = 100; server.broken = false; server.crashed = false; server.hung = false; server.temp = DATA.room(this.state.roomId).ambient;
      Object.values(server.parts).forEach(value => {
        if (Array.isArray(value)) value.filter(Boolean).forEach(part => { part.broken = false; });
        else if (value && typeof value === 'object') value.broken = false;
      });
      this.updateStatus(server);
    });
    return record(this, target, 'REPAIR ALL');
  };
  proto.cheatUnlockAll = function (target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    if (target === 'pc') { this.state.pc.year = 2026; this.state.pc.cheats = Object.assign({}, this.state.pc.cheats, { unlockAll: true }); }
    else { this.state.startYear = 2026; this.state.currentYear = 2026; this.state.cheats.unlockAll = true; }
    return record(this, target, 'UNLOCK ALL YEARS');
  };
  proto.cheatMaxUpgrades = function (target = this.state.campaignMode) {
    if (!validTarget(this, target)) return false;
    if (target === 'pc') this.state.pc.upgrades = { bench: 3, ads: 5, tools: 3 };
    else {
      Object.entries(DATA.UPGRADES).forEach(([id, definition]) => { this.state.upgrades[id] = definition.max; });
      ['cooling','efficiency','marketing'].forEach(kind => { this.state.research[kind] = 5; });
    }
    return record(this, target, 'MAX UPGRADES');
  };
  proto.cheatClearDebt = function () { this.state.debt = 0; return record(this, 'servers', 'DEBT =', 0); };
  proto.cheatFreePC = function (year = this.state.pc.year) {
    if (!validTarget(this, 'pc')) return false;
    year = clamp(round(number(year) || this.state.pc.year), 1990, 2026);
    const report = this.pcAutoDraft(year);
    if (!report) { this.notify('Не удалось подобрать совместимую сборку', 'error'); return false; }
    const result = this.buildCustomPC({ free: true, ignoreEra: true, name: `Cheat Custom ${year}` });
    if (!result.ok) return false;
    record(this, 'pc', 'FREE CUSTOM PC', `${year} · ${result.machine.id}`);
    return result.machine;
  };
  proto.cheatHistory = function () { return root(this).history; };
  proto.cheatsLoaded = true;
})();
