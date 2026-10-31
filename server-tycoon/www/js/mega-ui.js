/* Mega Archive 3.0: поиск, эпохи и постраничный вывод больших каталогов. */
(function () {
  'use strict';
  window.addEventListener('DOMContentLoaded', () => {
    const ui = window.serverTycoonUI;
    if (!ui || !window.DATA.megaLoaded) return;
    const proto = Object.getPrototypeOf(ui);
    if (proto.megaUILoaded) return;
    const $ = (selector, root = document) => root.querySelector(selector);
    const esc = value => String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({ '&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;' }[char]));
    const money = value => `${Math.round(value).toLocaleString('ru-RU')} ₽`;
    const memory = value => value < 1 ? `${Math.round(value * 1024)} МБ` : `${Number(value.toFixed(1))} ГБ`;
    const disk = value => value >= 1000 ? `${Number((value / 1000).toFixed(1))} ТБ` : memory(value);
    const brandColor = id => {
      const value = window.BRAND_COLORS[id];
      return !value || value.toLowerCase() === '#000000' ? '#e8f4f7' : value;
    };
    ui.catalogEra = 'all';
    ui.catalogQuery = '';
    ui.softwareCategory = 'all';
    ui.softwareQuery = '';
    ui.softwarePage = 0;
    ui.softwarePerPage = 16;

    proto.filteredCatalog = function () {
      const range = DATA.ERA_RANGES.find(item => item[0] === this.catalogEra) || DATA.ERA_RANGES[0];
      const query = this.catalogQuery.trim().toLowerCase();
      return DATA.SERVER_TEMPLATES.filter(template => {
        if (this.catalogBrand !== 'all' && template.brand !== this.catalogBrand) return false;
        if (template.year < range[2] || template.year > range[3]) return false;
        if (query && !`${template.name} ${template.year} ${template.role || ''} ${DATA.part(template.cpu).name}`.toLowerCase().includes(query)) return false;
        return true;
      });
    };

    proto.catalogView = function () {
      const brands = [...new Set(DATA.SERVER_TEMPLATES.map(template => template.brand || 'server'))].sort((a,b) => a.localeCompare(b));
      const templates = this.filteredCatalog();
      const pages = Math.max(1,Math.ceil(templates.length / 12));
      this.catalogPage = Math.max(0,Math.min(this.catalogPage,pages - 1));
      const visible = templates.slice(this.catalogPage * 12,this.catalogPage * 12 + 12);
      let html = `<div class="archive-search"><input type="search" data-catalog-query value="${esc(this.catalogQuery)}" placeholder="Модель, CPU, год или роль" aria-label="Поиск по серверам"><button class="btn btn-small btn-primary" data-action="catalog-search">Найти</button></div>`;
      html += `<div class="filter-row era-filter">${DATA.ERA_RANGES.map(([id,label]) => `<button class="filter-chip ${this.catalogEra === id ? 'active' : ''}" data-action="catalog-era" data-era="${id}">${label}</button>`).join('')}<button class="filter-chip" data-action="catalog-reset">Сброс</button></div>`;
      html += `<div class="filter-row brand-filter"><button class="filter-chip ${this.catalogBrand === 'all' ? 'active' : ''}" data-action="catalog-brand" data-brand="all">Все бренды</button>${brands.map(brand => `<button class="filter-chip ${this.catalogBrand === brand ? 'active' : ''}" data-action="catalog-brand" data-brand="${brand}">${esc(brand === 'hp' ? 'HP/HPE' : brand === 'dec' ? 'DEC' : brand.toUpperCase())}</button>`).join('')}</div>`;
      const eraLabel = (DATA.ERA_RANGES.find(item => item[0] === this.catalogEra) || DATA.ERA_RANGES[0])[1];
      const archiveLabel = this.catalogBrand === 'all' ? 'Архив' : this.catalogBrand.toUpperCase();
      html += `<div class="section-label"><span>${esc(archiveLabel)} · ${esc(eraLabel)}</span><span>${templates.length} / ${DATA.SERVER_TEMPLATES.length}</span></div>`;
      if (!visible.length) html += this.emptyState('market','Ничего не найдено','Измените бренд, эпоху или поисковый запрос.','Сбросить поиск','catalog-reset');
      else html += visible.map(template => this.catalogCard(template)).join('');
      if (pages > 1) html += `<div class="pagination"><button class="btn btn-small btn-ghost" data-action="catalog-page" data-page="${this.catalogPage - 1}" ${this.catalogPage === 0 ? 'disabled' : ''}>← Назад</button><span class="badge info">${this.catalogPage + 1} / ${pages}</span><button class="btn btn-small btn-ghost" data-action="catalog-page" data-page="${this.catalogPage + 1}" ${this.catalogPage >= pages - 1 ? 'disabled' : ''}>Дальше →</button></div>`;
      return html;
    };

    proto.catalogCard = function (template) {
      const chassis = DATA.part(template.chassis);
      const cpu = DATA.part(template.cpu);
      const ram = template.ram.reduce((sum,id) => sum + DATA.part(id).capacity,0);
      const disks = template.disks.reduce((sum,id) => sum + DATA.part(id).capacity,0);
      const gpus = template.gpus || [];
      const price = Math.round(template.basePrice * 1.25);
      return `<article class="card listing-card catalog-card"><div class="listing-art" style="color:${brandColor(template.brand)}">${brandSvg(template.brand)}<span class="condition">${template.year} · ПОД ЗАКАЗ</span></div><div class="listing-info"><h3 class="listing-title">${esc(template.name)}</h3><p class="listing-desc">${cpu.cores} ядер · ${memory(ram)} ${chassis.ramType} · ${disk(disks)}${gpus.length ? `<br>${gpus.length}× GPU · ${memory(gpus.reduce((sum,id) => sum + DATA.part(id).vram,0))} VRAM` : ''}<br>${chassis.socket} · ${chassis.ramSlots} RAM · ${chassis.pcieSlots || 0} PCIe</p><div class="listing-seller">${esc(template.role || 'Server')} · архивная конфигурация</div><div class="listing-price">${money(price)}</div></div><div class="listing-actions"><button class="btn btn-small btn-primary btn-wide" data-action="buy-catalog" data-id="${template.id}">${svgIcon('plus')}Заказать и установить</button></div></article>`;
    };

    proto.softwareTab = function (server) {
      const query = this.softwareQuery.trim().toLowerCase();
      const filtered = DATA.SOFTWARE.filter(app => {
        if (this.softwareCategory === 'installed' && !server.software.includes(app.id)) return false;
        if (!['all','installed'].includes(this.softwareCategory) && app.category !== this.softwareCategory) return false;
        return !query || `${app.name} ${app.description} ${app.release || ''}`.toLowerCase().includes(query);
      });
      const pages = Math.max(1,Math.ceil(filtered.length / this.softwarePerPage));
      this.softwarePage = Math.max(0,Math.min(this.softwarePage,pages - 1));
      const apps = filtered.slice(this.softwarePage * this.softwarePerPage,(this.softwarePage + 1) * this.softwarePerPage);
      let html = `<div class="notice" style="margin-bottom:10px">${svgIcon('os')}<div><strong>${server.os ? DATA.os(server.os).name : 'Нет операционной системы'}</strong>${server.os ? 'Поиск по тысячам пакетов. Доход начисляется только на включённом сервере.' : 'Запишите образ на флешку, затем установите ОС.'}</div></div><button class="btn btn-cyan btn-wide" data-action="os-select" data-id="${server.id}">${svgIcon('os')}${server.os ? 'Переустановить / сменить ОС' : 'Выбрать флешку и установить ОС'}</button>`;
      html += `<div class="section-label"><span>Каталог пакетов</span><span>${server.software.length} установлено</span></div><div class="archive-search software-search"><input type="search" data-software-query value="${esc(this.softwareQuery)}" placeholder="Название сервиса или редакции" aria-label="Поиск сервисов"><button class="btn btn-small btn-primary" data-action="software-search">Найти</button></div>`;
      html += `<div class="filter-row software-filters">${[['all','Все'],['installed','Установленные'],...Object.entries(DATA.SERVICE_CATEGORIES)].map(([id,label]) => `<button class="filter-chip ${this.softwareCategory === id ? 'active' : ''}" data-action="software-category" data-category="${id}">${esc(label)}</button>`).join('')}<button class="filter-chip" data-action="software-reset">Сброс</button></div>`;
      html += `<div class="section-label"><span>${filtered.length} пакетов</span><span>страница ${this.softwarePage + 1}/${pages}</span></div>`;
      if (!apps.length) html += this.emptyState('apps','Пакеты не найдены','Сбросьте фильтр или попробуйте другое название.','Сбросить','software-reset');
      else html += apps.map(app => {
        const installed = server.software.includes(app.id);
        const supported = server.os && app.os.includes(server.os);
        const cores = server.parts.cpu ? server.parts.cpu.cores : 0;
        const rate = app.rate + (app.coreRate || 0) * cores;
        const description = app.description.replace(/ · \+\d+(?:\.\d+)? ₽\/ч$/,'');
        const supportText = supported || !server.os ? description : `Нужна ОС: ${app.os.slice(0,2).map(id => DATA.os(id).name).join(' / ')}${app.os.length > 2 ? ` и ещё ${app.os.length - 2}` : ''}`;
        return `<div class="software-row" data-app="${app.id}"><div class="software-icon" style="color:${brandColor(app.brand)}">${brandSvg(app.brand || app.icon)}</div><div><div class="software-name">${esc(app.name)}</div><div class="software-spec">CPU ${app.cpu}% · RAM ${memory(app.ram)} · +${rate} ₽/ч${app.year ? ` · ${app.year}` : ''}<br>${esc(supportText)}</div></div><button class="btn ${installed ? 'btn-danger' : 'btn-primary'}" data-action="${installed ? 'uninstall-soft' : 'install-soft'}" data-id="${server.id}" data-soft="${app.id}" ${!installed && !supported ? 'disabled' : ''}>${installed ? 'Удалить' : 'Установить'}</button></div>`;
      }).join('');
      if (pages > 1) html += `<div class="pagination"><button class="btn btn-small btn-ghost" data-action="software-page" data-page="${this.softwarePage - 1}" ${this.softwarePage === 0 ? 'disabled' : ''}>← Назад</button><span class="badge info">${this.softwarePage + 1} / ${pages}</span><button class="btn btn-small btn-ghost" data-action="software-page" data-page="${this.softwarePage + 1}" ${this.softwarePage >= pages - 1 ? 'disabled' : ''}>Дальше →</button></div>`;
      return html;
    };

    const baseOpenServer = proto.openServer;
    proto.openServer = function (serverId,tab = 'overview') {
      if (!this.modal || this.modal.serverId !== serverId) this.softwarePage = 0;
      return baseOpenServer.call(this,serverId,tab);
    };

    const baseHandleAction = proto.handleAction;
    proto.handleAction = function (action,element,event) {
      switch (action) {
        case 'catalog-era': this.catalogEra = element.dataset.era; this.catalogPage = 0; this.renderView(); return;
        case 'catalog-search': this.catalogQuery = ($('[data-catalog-query]',this.view) || {}).value || ''; this.catalogPage = 0; this.renderView(); return;
        case 'catalog-reset': this.catalogQuery = ''; this.catalogEra = 'all'; this.catalogBrand = 'all'; this.catalogPage = 0; this.renderView(); return;
        case 'software-category': this.softwareCategory = element.dataset.category; this.softwarePage = 0; this.renderModal(); return;
        case 'software-search': this.softwareQuery = ($('[data-software-query]',this.modalRoot) || {}).value || ''; this.softwarePage = 0; this.renderModal(); return;
        case 'software-reset': this.softwareQuery = ''; this.softwareCategory = 'all'; this.softwarePage = 0; this.renderModal(); return;
        case 'software-page': this.softwarePage = Number(element.dataset.page); this.renderModal(); $('.modal-body',this.modalRoot).scrollTop = 0; return;
        default: return baseHandleAction.call(this,action,element,event);
      }
    };

    document.addEventListener('keydown',event => {
      if (event.key !== 'Enter') return;
      if (event.target.matches('[data-catalog-query]')) {
        event.preventDefault();
        ui.catalogQuery = event.target.value; ui.catalogPage = 0; ui.renderView();
      } else if (event.target.matches('[data-software-query]')) {
        event.preventDefault();
        ui.softwareQuery = event.target.value; ui.softwarePage = 0; ui.renderModal();
      }
    });
    proto.megaUILoaded = true;
    ui.render();
    if (ui.modal) ui.renderModal();
  });
})();
