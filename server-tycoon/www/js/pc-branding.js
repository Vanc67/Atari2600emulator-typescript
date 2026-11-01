/* PC Branding 8.2: фирменные SVG-значки компонентов, полностью offline. */
(function () {
  'use strict';
  const aliases = {
    intel: 'intel', amd: 'amd', ati: 'amd', radeon: 'amd', nvidia: 'nvidia', geforce: 'nvidia',
    hp: 'hp', hpe: 'hp', compaq: 'hp', dell: 'dell', lenovo: 'lenovo', ibm: 'ibm',
    asus: 'asus', gigabyte: 'gigabyte', supermicro: 'supermicro', seagate: 'seagate', samsung: 'samsung',
    fujitsu: 'fujitsu', huawei: 'huawei', toshiba: 'toshiba', lg: 'lg', Acer: 'acer'
  };
  const known = new Set(['intel','amd','nvidia','hp','dell','lenovo','ibm','asus','gigabyte','supermicro','seagate','samsung','fujitsu','huawei']);
  const short = { 'western digital': 'WD', 'g.skill': 'G.SKILL', 'au optronics': 'AUO', 'western': 'WD', 'biostar': 'BIOSTAR', 'crucial': 'CRUCIAL', 'kingston': 'KINGSTON', 'corsair': 'CORSAIR', 'panasonic': 'PANASONIC' };
  const normalize = value => String(value || '').trim().toLowerCase();
  const esc = value => String(value).replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
  const hash = value => { let h = 0; for (const char of String(value)) h = (Math.imul(h, 31) + char.charCodeAt(0)) >>> 0; return h; };
  function brandName(part) { return typeof part === 'string' ? part : part && (part.brand || part.manufacturer || part.name) || 'PC'; }
  function pcBrandKey(part) {
    const name = brandName(part); const key = normalize(name);
    if (aliases[key]) return aliases[key];
    const lower = Object.keys(aliases).find(alias => alias === alias.toLowerCase() && key.includes(alias));
    return lower ? aliases[lower] : key || 'pc';
  }
  function pcBrandColor(part) {
    const key = pcBrandKey(part); if (window.BRAND_COLORS && BRAND_COLORS[key]) return BRAND_COLORS[key];
    return `hsl(${195 + hash(key) % 125} 72% 65%)`;
  }
  function pcPartBrandSvg(part, className = '') {
    const key = pcBrandKey(part); const classes = `architect-brand ${className}`.trim();
    if (known.has(key)) return window.brandSvg(key, classes);
    const name = brandName(part); const normalized = normalize(name);
    const label = short[normalized] || String(name).replace(/[^a-z0-9. ]/gi, '').split(/\s+/).map(word => word.length > 2 ? word.slice(0, 3) : word).join(' ').slice(0, 10).toUpperCase() || 'PC';
    return `<svg class="${esc(classes)}" viewBox="0 0 64 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><rect x="1" y="1" width="62" height="22" rx="5" stroke="currentColor" stroke-opacity=".45"/><text x="32" y="15.5" text-anchor="middle" fill="currentColor" font-family="Arial,sans-serif" font-size="${label.length > 7 ? 7 : 9}" font-weight="800" letter-spacing=".3">${esc(label)}</text></svg>`;
  }
  window.pcBrandKey = pcBrandKey; window.pcBrandColor = pcBrandColor; window.pcPartBrandSvg = pcPartBrandSvg;
})();
