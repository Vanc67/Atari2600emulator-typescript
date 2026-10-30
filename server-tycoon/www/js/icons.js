/* Вся графика игры — локальные inline SVG. Внешние изображения и шрифты не используются. */
(function () {
  'use strict';

  const wrap = (body, viewBox = '0 0 64 64') =>
    `<svg viewBox="${viewBox}" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">${body}</svg>`;

  const hardware = {
    server: wrap(`
      <path d="M10 17 34 5l21 11-23 13L10 17Z" fill="#15273a" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m10 17 22 12v29L10 46V17Z" fill="#0b1625" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m32 29 23-13v29L32 58V29Z" fill="#101f31" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m15 24 12 6v6l-12-6v-6Zm0 12 12 6v6l-12-6v-6Z" fill="#172f42" stroke="#3ccfb4"/>
      <path d="m38 30 12-6v5l-12 6v-5Zm0 11 12-6v5l-12 6v-5Z" fill="#172f42" stroke="#3ccfb4"/>
      <circle cx="48" cy="43" r="1.6" fill="#b6ff56"/><circle cx="44" cy="45" r="1.2" fill="#56d9ff"/>
      <path d="M17 14 39 25M24 10l21 11" stroke="#29475a" stroke-width="1"/>
    `),
    chassis: wrap(`
      <path d="M7 20 34 7l23 12-27 15L7 20Z" fill="#15273a" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m7 20 23 14v25L7 45V20Z" fill="#0b1625" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m30 34 27-15v25L30 59V34Z" fill="#101f31" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m13 25 11 6v17l-11-6V25Z" fill="#132b3c" stroke="#294c61"/>
      <circle cx="46" cy="39" r="7" stroke="#55f6d2"/><circle cx="46" cy="39" r="2" fill="#55f6d2"/>
      <path d="m46 32 2 5 5 2-5 2-2 5-2-5-5-2 5-2 2-5Z" fill="#55f6d2" opacity=".45"/>
    `),
    cpu: wrap(`
      <path d="m13 22 25-12 15 9-25 13-15-10Z" fill="#173149" stroke="#b6ff56" stroke-width="1.5"/>
      <path d="m13 22 15 10v17l-15-9V22Z" fill="#0d1c2b" stroke="#b6ff56" stroke-width="1.5"/>
      <path d="m28 32 25-13v17L28 49V32Z" fill="#122638" stroke="#b6ff56" stroke-width="1.5"/>
      <path d="m23 22 14-7 8 5-14 7-8-5Z" fill="#263e4d" stroke="#e8ffb8"/>
      <path d="M17 43v7m5-4v7m5-4v7m5-5v7m7-11v7m6-10v7m5-11v7" stroke="#f6c85f" stroke-width="2"/>
      <path d="M16 15v-5m7 2V7m7 3V5m7 3V4m7 6V5m-34 19H5m8 5H7m8 5H9" stroke="#f6c85f" stroke-width="1.5"/>
    `),
    ram: wrap(`
      <path d="M8 20 48 8l9 6-40 13L8 20Z" fill="#173149" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m8 20 9 7v25l-9-6V20Z" fill="#0d1c2b" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m17 27 40-13v25l-9 3v5l-6 1v-5l-8 3v5l-6 1v-5l-11 4V27Z" fill="#123329" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m23 27 7-2v12l-7 2V27Zm11-4 7-2v12l-7 2V23Zm11-4 7-2v12l-7 2V19Z" fill="#0a121c" stroke="#7dffb0"/>
      <path d="M24 46v5m7-7v5m7-8v5m7-8v5" stroke="#f6c85f" stroke-width="2"/>
    `),
    hdd: wrap(`
      <path d="M9 17 43 7l13 8-35 12L9 17Z" fill="#26384a" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m9 17 12 10v27L9 44V17Z" fill="#0c1825" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m21 27 35-12v27L21 54V27Z" fill="#182a3a" stroke="#56d9ff" stroke-width="1.5"/>
      <ellipse cx="38" cy="31" rx="12" ry="8" transform="rotate(-18 38 31)" fill="#0b1420" stroke="#a8d9e8"/>
      <ellipse cx="38" cy="31" rx="3" ry="2" transform="rotate(-18 38 31)" fill="#56d9ff"/>
      <path d="m45 39 7-15 2 1-5 16-4-2Z" fill="#f6c85f"/><circle cx="14" cy="39" r="1.5" fill="#b6ff56"/>
    `),
    ssd: wrap(`
      <path d="M9 18 46 7l10 7-37 13L9 18Z" fill="#252044" stroke="#b692ff" stroke-width="1.5"/>
      <path d="m9 18 10 9v27L9 45V18Z" fill="#121128" stroke="#b692ff" stroke-width="1.5"/>
      <path d="m19 27 37-13v27L19 54V27Z" fill="#191735" stroke="#b692ff" stroke-width="1.5"/>
      <path d="m27 29 9-3v9l-9 3v-9Zm13-5 9-3v9l-9 3v-9ZM27 42l9-3v7l-9 3v-7Zm13-5 9-3v9l-9 3v-9Z" fill="#0b1220" stroke="#d3bdff"/>
    `),
    cooler: wrap(`
      <path d="M32 5 56 18 32 32 8 18 32 5Z" fill="#173149" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="M8 18 32 32v25L8 43V18Z" fill="#0b1625" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m32 32 24-14v25L32 57V32Z" fill="#102334" stroke="#56d9ff" stroke-width="1.5"/>
      <ellipse cx="32" cy="18" rx="15" ry="9" fill="#09121e" stroke="#55f6d2"/>
      <path d="M32 17c-8-8-13 1-5 3-8 5 0 12 5 3 7 8 14 0 5-4 8-6-1-12-5-2Z" fill="#55f6d2" opacity=".7"/>
      <circle cx="32" cy="18" r="2.5" fill="#e8ffff"/>
      <path d="M14 33v9m6-6v9m18-7v9m6-12v9m6-12v9" stroke="#294c61" stroke-width="2"/>
    `),
    psu: wrap(`
      <path d="M8 20 42 7l15 9-34 14L8 20Z" fill="#2d3040" stroke="#f6c85f" stroke-width="1.5"/>
      <path d="m8 20 15 10v27L8 47V20Z" fill="#141824" stroke="#f6c85f" stroke-width="1.5"/>
      <path d="m23 30 34-14v27L23 57V30Z" fill="#202534" stroke="#f6c85f" stroke-width="1.5"/>
      <ellipse cx="39" cy="35" rx="11" ry="8" transform="rotate(-20 39 35)" stroke="#f6c85f"/>
      <path d="m31 30 15 9m-17-5 15 9m-10-17 13 8m-17 1 14 8" stroke="#6c643e"/>
      <path d="M13 36v7m4-5v7" stroke="#ff6b7a" stroke-width="2"/>
    `),
    usb: wrap(`
      <path d="m23 7 18 5-5 15-18-5 5-15Z" fill="#b9c7d6" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m18 22 18 5-11 30-18-5 11-30Z" fill="#18293a" stroke="#56d9ff" stroke-width="1.5"/>
      <path d="m27 10 2 6m6-4 2 6" stroke="#243445" stroke-width="3"/>
      <path d="m14 38 10 3" stroke="#55f6d2" stroke-width="2"/><circle cx="17" cy="47" r="2" fill="#b6ff56"/>
    `),
    tool: wrap(`
      <path d="M43 8c-8-3-15 4-12 11L9 41c-5 5 2 14 8 9l22-22c7 3 14-4 11-12l-8 9-7-2-2-7 10-8Z" fill="#172b3d" stroke="#f6c85f" stroke-width="2"/>
      <circle cx="14" cy="45" r="3" stroke="#f6c85f"/><path d="m27 25 9 9" stroke="#fff0b0"/>
    `),
    motherboard: wrap(`
      <path d="M7 15 46 5l12 8-39 12L7 15Z" fill="#153b35" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m7 15 12 10v32L7 47V15Z" fill="#0b201e" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m19 25 39-12v32L19 57V25Z" fill="#102e2a" stroke="#55f6d2" stroke-width="1.5"/>
      <path d="m26 27 12-4v12l-12 4V27Zm17-6 9-3v9l-9 3v-9ZM26 44l25-8m-25 14 25-8" stroke="#8dffbf"/>
      <circle cx="49" cy="38" r="4" stroke="#f6c85f"/>
    `)
  };

  const ui = {
    farm: wrap(`<path d="M4 20 12 4l8 16M12 4v16M2 20h20M7 20v-8m10 8v-8" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M9 9h6" stroke="currentColor" stroke-width="1.8"/>`, '0 0 24 24'),
    market: wrap(`<path d="M3 9h18l-2-5H5L3 9Zm2 0v11h14V9M9 20v-7h6v7" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M3 9c0 3 4 3 4 0 0 3 5 3 5 0 0 3 5 3 5 0 0 3 4 3 4 0" stroke="currentColor" stroke-width="1.5"/>`, '0 0 24 24'),
    inventory: wrap(`<path d="m4 7 8-4 8 4-8 4-8-4Zm0 0v10l8 4 8-4V7M12 11v10" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="m8 5 8 4" stroke="currentColor" stroke-width="1.5"/>`, '0 0 24 24'),
    stats: wrap(`<path d="M4 20V10m6 10V4m6 16v-7m5 7H2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="m3 7 5-4 6 6 7-6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>`, '0 0 24 24'),
    wallet: wrap(`<path d="M3 6c0-2 2-3 4-3h12v4H7c-2 0-4-1-4-3Zm0 0v13c0 2 2 3 4 3h14V7H7" stroke="currentColor" stroke-width="1.7"/><path d="M15 11h6v5h-6c-3 0-3-5 0-5Z" fill="currentColor" opacity=".25"/><circle cx="16" cy="13.5" r="1" fill="currentColor"/>`, '0 0 24 24'),
    bolt: wrap(`<path d="m14 2-9 12h7l-2 8 9-12h-7l2-8Z" fill="currentColor" opacity=".18" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>`, '0 0 24 24'),
    temp: wrap(`<path d="M9 14V5a3 3 0 0 1 6 0v9a5 5 0 1 1-6 0Z" stroke="currentColor" stroke-width="1.8"/><path d="M12 8v10" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="18" r="2" fill="currentColor"/>`, '0 0 24 24'),
    noise: wrap(`<path d="M4 10v4h4l5 4V6L8 10H4Z" fill="currentColor" opacity=".2" stroke="currentColor" stroke-width="1.7"/><path d="M16 9c2 2 2 4 0 6m3-9c4 4 4 8 0 12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>`, '0 0 24 24'),
    pause: wrap(`<path d="M8 5v14M16 5v14" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>`, '0 0 24 24'),
    play: wrap(`<path d="m8 4 12 8-12 8V4Z" fill="currentColor" opacity=".2" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>`, '0 0 24 24'),
    plus: wrap(`<path d="M12 4v16M4 12h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>`, '0 0 24 24'),
    refresh: wrap(`<path d="M20 7V3l-3 3a8 8 0 1 0 2 9M20 3v5h-5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>`, '0 0 24 24'),
    power: wrap(`<path d="M12 2v10m-5-8a9 9 0 1 0 10 0" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>`, '0 0 24 24'),
    close: wrap(`<path d="m5 5 14 14M19 5 5 19" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>`, '0 0 24 24'),
    chevron: wrap(`<path d="m9 5 7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`, '0 0 24 24'),
    chip: wrap(`<rect x="6" y="6" width="12" height="12" rx="2" stroke="currentColor" stroke-width="1.7"/><path d="M9 1v4m6-4v4M9 19v4m6-4v4M1 9h4m-4 6h4m14-6h4m-4 6h4" stroke="currentColor" stroke-width="1.7"/>`, '0 0 24 24'),
    ramUi: wrap(`<path d="M3 6h18v10h-3v3h-3v-3H9v3H6v-3H3V6Z" stroke="currentColor" stroke-width="1.7"/><path d="M7 9h3v4H7zm7 0h3v4h-3z" fill="currentColor" opacity=".35"/>`, '0 0 24 24'),
    disk: wrap(`<path d="M4 4h16v16H4z" stroke="currentColor" stroke-width="1.7"/><circle cx="12" cy="11" r="5" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="11" r="1" fill="currentColor"/><path d="m14 16 4-5" stroke="currentColor" stroke-width="1.7"/>`, '0 0 24 24'),
    os: wrap(`<rect x="3" y="4" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.7"/><path d="M8 21h8m-4-3v3M7 9l3 2-3 2m5 0h4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>`, '0 0 24 24'),
    apps: wrap(`<rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/><rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/><rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/><rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.7"/>`, '0 0 24 24'),
    wrench: wrap(`<path d="M14 6a5 5 0 0 0-6 6l-5 5c-2 2 1 5 3 3l5-5a5 5 0 0 0 6-6l-3 3-3-3 3-3Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>`, '0 0 24 24'),
    trash: wrap(`<path d="M4 7h16M9 3h6l1 4H8l1-4Zm-3 4 1 14h10l1-14M10 11v6m4-6v6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>`, '0 0 24 24'),
    check: wrap(`<path d="m4 12 5 5L20 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>`, '0 0 24 24'),
    warning: wrap(`<path d="M12 3 2 21h20L12 3Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M12 9v5m0 3v1" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>`, '0 0 24 24'),
    clock: wrap(`<circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.7"/><path d="M12 6v6l4 2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>`, '0 0 24 24'),
    home: wrap(`<path d="m3 11 9-8 9 8M5 9v12h14V9M9 21v-7h6v7" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/>`, '0 0 24 24'),
    info: wrap(`<circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.7"/><path d="M12 11v6m0-10v1" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>`, '0 0 24 24'),
    jellyfin: wrap(`<path d="m12 2 9 16c-5 5-13 5-18 0l9-16Z" stroke="currentColor" stroke-width="1.7"/><path d="m12 8 5 9c-3 2-7 2-10 0l5-9Z" fill="currentColor" opacity=".3"/>`, '0 0 24 24'),
    files: wrap(`<path d="M3 6h7l2 3h9v11H3V6Z" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M3 10h18" stroke="currentColor" stroke-width="1.5"/>`, '0 0 24 24'),
    boinc: wrap(`<circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.7"/><path d="m7 15 3-8 3 8 4-6M5 12h14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>`, '0 0 24 24'),
    ai: wrap(`<path d="M8 4h8l4 5v7l-5 5H9l-5-5V8l4-4Z" stroke="currentColor" stroke-width="1.7"/><path d="M8 15V9l4 6V9m3 6V9h3m-3 3h3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>`, '0 0 24 24')
  };

  window.ICONS = Object.assign({}, hardware, ui);
  window.svgIcon = function (name, className = '') {
    const source = window.ICONS[name] || window.ICONS.inventory;
    return source.replace('<svg ', `<svg class="${className}" `);
  };
})();
