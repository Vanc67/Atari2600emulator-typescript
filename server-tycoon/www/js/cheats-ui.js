/* Debug Cheats 8.0 UI: новая вкладка рядом с существующим логом. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !ui.game.cheatSetYear) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.cheatsUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
    const money = value => `${Math.round(Number(value) || 0).toLocaleString('ru-RU')} ₽`;
    ui.debugPane = 'log'; ui.cheatTarget = ui.game.state.campaignMode === 'pc' && ui.game.state.pc.started ? 'pc' : 'servers';
    const tabs = () => `<div class="debug-pane-tabs"><button data-action="debug-pane" data-pane="log" class="${ui.debugPane === 'log' ? 'active' : ''}">Журнал</button><button data-action="debug-pane" data-pane="cheats" class="${ui.debugPane === 'cheats' ? 'active' : ''}">Читы</button></div>`;
    const row = (kind, label, value, min, max, suffix = '') => `<div class="cheat-row"><label><span>${label}</span><div><input type="number" inputmode="numeric" data-cheat-field="${kind}" value="${value}" ${min != null ? `min="${min}"` : ''} ${max != null ? `max="${max}"` : ''}>${suffix ? `<b>${suffix}</b>` : ''}</div></label><button class="btn btn-small btn-primary" data-action="cheat-apply" data-kind="${kind}">Применить</button></div>`;

    proto.renderCheats = function () {
      const target = this.cheatTarget === 'pc' && this.game.state.pc.started ? 'pc' : 'servers'; this.cheatTarget = target;
      const pc = target === 'pc'; const state = this.game.state; const campaign = pc ? state.pc : state;
      const year = pc ? campaign.year : state.currentYear; const level = campaign.level || 1; const reputation = pc ? campaign.reputation : 0;
      const body = `<div class="debug-toolbar"><div><span>CHEATS // LOCAL ONLY</span><strong>Ничего не удаляется</strong></div><span class="badge good">DEBUG</span></div>${tabs()}<div class="cheat-target"><button data-action="cheat-target" data-target="servers" class="${!pc ? 'active' : ''}">${svgIcon('server')}Серверная ферма</button><button data-action="cheat-target" data-target="pc" class="${pc ? 'active' : ''}" ${state.pc.started ? '' : 'disabled'}>${svgIcon('chip')}PC-мастерская</button></div><div class="notice warning">${svgIcon('warning')}<div><strong>Режим читов · ${pc ? 'PC' : 'Серверы'}</strong>Год и деньги меняются явно. Лоты Авито, заказы, компьютеры, инвентарь и старые saves не очищаются.</div></div><div class="cheat-panel"><div class="cheat-current"><span>Сейчас</span><strong>${year} · ${money(campaign.money)}</strong><small>День ${campaign.day} · уровень ${level}${pc ? ` · репутация ${reputation}` : ''}</small></div>${row('year','Любой год игры',year,1990,2026)}${row('money','Установить деньги',Math.round(campaign.money),0,1000000000000,'₽')}${row('day','Игровой день',campaign.day,1,1000000)}${row('level','Уровень',level,1,100)}${pc ? row('reputation','Репутация мастерской',reputation,0,1000000) : ''}</div><div class="cheat-quick"><span>Быстрые годы</span><div>${[1990,1995,2000,2005,2010,2015,2020,2026].map(value => `<button data-action="cheat-quick" data-kind="year" data-value="${value}">${value}</button>`).join('')}</div><span>Добавить деньги</span><div>${[10000,1000000,1000000000].map(value => `<button data-action="cheat-quick" data-kind="addMoney" data-value="${value}">+${value >= 1000000000 ? '1 млрд' : value >= 1000000 ? '1 млн' : '10 000'} ₽</button>`).join('')}</div></div><div class="cheat-actions"><button class="btn btn-cyan" data-action="cheat-quick" data-kind="unlock">${svgIcon('clock')}Открыть все эпохи</button><button class="btn btn-primary" data-action="cheat-quick" data-kind="repair">${svgIcon('wrench')}Починить все машины</button><button class="btn btn-ghost" data-action="cheat-quick" data-kind="upgrades">Максимальные улучшения</button>${pc ? `<button class="btn btn-amber" data-action="cheat-quick" data-kind="freePC">${svgIcon('chip')}Бесплатный Custom PC · год из поля</button>` : `<button class="btn btn-ghost" data-action="cheat-quick" data-kind="debt">Списать весь долг</button>`}</div><div class="cheat-history"><div class="section-label"><span>История читов</span><span>${this.game.cheatHistory().length}/40</span></div>${this.game.cheatHistory().slice(0, 12).map(entry => `<div><span>${entry.target === 'pc' ? 'PC' : 'SRV'} · D${entry.day}</span><strong>${esc(entry.kind)} ${entry.value == null ? '' : esc(entry.value)}</strong></div>`).join('') || '<p class="muted tiny">Пока нет применённых читов.</p>'}</div>`;
      this.modalRoot.innerHTML = this.modalShell('Debug Cheats', 'ЛЮБОЙ ГОД · ЛЮБЫЕ ДЕНЬГИ · БЕЗ УДАЛЕНИЯ ПРОГРЕССА', body);
      const sheet = $('.modal-sheet', this.modalRoot); if (sheet) sheet.classList.add('debug-sheet', 'cheat-sheet');
    };

    const baseCheatDebug = proto.renderDebug;
    proto.renderDebug = function () {
      if (this.debugPane === 'cheats') {
        const active = document.activeElement;
        if (active && active.matches('[data-cheat-field]') && this.modalRoot.contains(active)) return;
        this.renderCheats(); return;
      }
      const result = baseCheatDebug.call(this); const toolbar = $('.debug-toolbar', this.modalRoot);
      if (toolbar && !$('.debug-pane-tabs', this.modalRoot)) toolbar.insertAdjacentHTML('afterend', tabs());
      return result;
    };
    const baseCheatSettings = proto.pcSettingsView;
    proto.pcSettingsView = function () { return baseCheatSettings.call(this) + `<button class="btn btn-ghost btn-wide cheat-settings-button" data-action="open-cheats">${svgIcon('wrench')}Secret Debug → Читы</button>`; };
    const baseCheatAction = proto.handleAction;
    proto.handleAction = function (action, element, event) {
      const own = ['open-cheats','debug-pane','cheat-target','cheat-apply','cheat-quick'];
      if (own.includes(action) && this.audio) { this.audio.unlock(); this.audio.sfx('click'); }
      switch (action) {
        case 'open-cheats': this.debugPane = 'cheats'; this.cheatTarget = this.game.state.campaignMode === 'pc' && this.game.state.pc.started ? 'pc' : 'servers'; this.openDebug(); return;
        case 'debug-pane': this.debugPane = element.dataset.pane; this.renderDebug(); return;
        case 'cheat-target': this.cheatTarget = element.dataset.target; this.renderCheats(); return;
        case 'cheat-apply': {
          const kind = element.dataset.kind; const input = $(`[data-cheat-field="${kind}"]`, this.modalRoot); if (!input) return;
          const methods = { year: 'cheatSetYear', money: 'cheatSetMoney', day: 'cheatSetDay', level: 'cheatSetLevel', reputation: 'cheatSetReputation' };
          this.game[methods[kind]](input.value, this.cheatTarget); this.refresh(false); return;
        }
        case 'cheat-quick': {
          const kind = element.dataset.kind; const value = Number(element.dataset.value); const target = this.cheatTarget;
          if (kind === 'year') this.game.cheatSetYear(value, target);
          if (kind === 'addMoney') this.game.cheatAddMoney(value, target);
          if (kind === 'unlock') this.game.cheatUnlockAll(target);
          if (kind === 'repair') this.game.cheatRepairAll(target);
          if (kind === 'upgrades') this.game.cheatMaxUpgrades(target);
          if (kind === 'debt') this.game.cheatClearDebt();
          if (kind === 'freePC') { const input = $('[data-cheat-field="year"]', this.modalRoot); this.game.cheatFreePC(input ? Number(input.value) : this.game.state.pc.year); }
          this.refresh(false); return;
        }
        default: return baseCheatAction.call(this, action, element, event);
      }
    };
    document.addEventListener('keydown', event => {
      if (!event.target.matches('[data-cheat-field]') || event.key !== 'Enter') return;
      event.preventDefault(); const button = $(`[data-action="cheat-apply"][data-kind="${event.target.dataset.cheatField}"]`, ui.modalRoot); if (button) button.click();
    });
    proto.cheatsUILoaded = true;
  });
})();
