/* Development oracle: execute recovered source with inert browser platform stubs. */
const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const root = path.resolve(__dirname, '..');
const shell = fs.readFileSync(path.join(root, 'original/recovered/shell.html'), 'utf8');
const dom = new JSDOM(shell, {
  url: 'https://wreckbox.invalid/',
  runScripts: 'outside-only',
  pretendToBeVisual: true
});
const { window } = dom;
const { document } = window;

function gradient() { return { addColorStop() {} }; }
function context(canvas) {
  const target = {
    canvas,
    createImageData(w, h) {
      w = Math.max(1, w | 0); h = Math.max(1, h | 0);
      return { width: w, height: h, data: new Uint8ClampedArray(w * h * 4) };
    },
    getImageData(x, y, w, h) { return this.createImageData(w, h); },
    createLinearGradient: gradient,
    createRadialGradient: gradient,
    createPattern() { return null; },
    measureText(s) { return { width: String(s).length * 6 }; },
    isPointInPath() { return false; }
  };
  return new Proxy(target, {
    get(obj, key) {
      if (key in obj) return obj[key];
      if (typeof key === 'symbol') return obj[key];
      return function() {};
    },
    set(obj, key, value) { obj[key] = value; return true; }
  });
}
const contexts = new WeakMap();
window.HTMLCanvasElement.prototype.getContext = function(kind) {
  if (kind !== '2d') return null;
  if (!contexts.has(this)) contexts.set(this, context(this));
  return contexts.get(this);
};
window.HTMLCanvasElement.prototype.toDataURL = function() { return 'data:image/png;base64,'; };
Object.defineProperty(window.navigator, 'webdriver', { value: true });
Object.defineProperty(window.navigator, 'language', { value: 'ru-RU' });
Object.defineProperty(window.navigator, 'languages', { value: ['ru-RU'] });
Object.defineProperty(window, 'innerWidth', { value: 128, writable: true });
Object.defineProperty(window, 'innerHeight', { value: 160, writable: true });
Object.defineProperty(window, 'devicePixelRatio', { value: 1, writable: true });
Object.defineProperty(window.screen, 'width', { value: 128 });
Object.defineProperty(window.screen, 'height', { value: 160 });
window.matchMedia = () => ({ matches: false, addListener() {}, removeListener() {} });
window.requestAnimationFrame = () => 1;
window.cancelAnimationFrame = () => {};
window.setTimeout = () => 1;
window.clearTimeout = () => {};
window.setInterval = () => 1;
window.clearInterval = () => {};
window.ResizeObserver = class { observe() {} disconnect() {} };
window.fetch = () => Promise.reject(new Error('Network disabled in oracle'));
window.ontouchstart = null;
let randomState = 0x1badc0de;
function seededRandom() {
  randomState = (randomState + 0x6D2B79F5) | 0;
  let t = randomState;
  t = Math.imul(t ^ (t >>> 15), t | 1);
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
}
window.__setRandomSeed = seed => { randomState = seed | 0; };
window.Math.random = seededRandom;
window.HTMLElement.prototype.scrollIntoView = function() {};
Object.defineProperty(document, 'readyState', { value: 'complete' });

let source = fs.readFileSync(path.join(root, 'original/recovered/game.js'), 'utf8');
const end = source.lastIndexOf('})();');
if (end < 0) throw new Error('IIFE end not found');
const exposure = `
window.__wb = {
  materials: nt, tools: nv, buildings: Nc, maps: Zc, widths: tf, mapConfig: ef,
  crackable: nh, crackWalk: [2, tt, j, Z], crackStop: N,
  props: El, buildingsOut: jl, bodies: Ia, people: gp,
  arrays: { material: pt, health: mt, tint: xt, flags: bt, ground: At },
  state: function() { return { width: u, size: b, map: Xc, seed: Jc, time: Dt, gravity: F, tool: ov }; },
  loadMap: function(map, seed) {
    let h = 2166136261;
    const text = map + ':' + (seed || 1);
    for (let i = 0; i < text.length; ++i) h = Math.imul(h ^ text.charCodeAt(i), 16777619);
    window.__setRandomSeed(h);
    lf(map, seed || 1);
  },
  reset: function() { tk(); },
  structural: function() { Ho(); },
  strike: function(x, y) { fv(x, y); },
  selectTool: function(id) { Uv(id); },
  explode: function(x, y, power) { yb(x, y, power); },
  stepTools: function(dt) { nv[ov].update(dt); },
  step: function(dt) { lT(dt); },
  stats: Lt,
  special: function() { return { npp: kf, base: Ff, volcano: hb, river: Xl, power: By }; },
  internals: { cellAt: ee, damage: le, destroy: ce, setCell: oe, remove: se, clear: re }
};
`;
source = source.slice(0, end) + exposure + source.slice(end);

try {
  window.eval(source);
} catch (error) {
  console.error(error && error.stack || error);
  process.exit(1);
}
const wb = window.__wb;
if (!wb) throw new Error('Oracle did not initialize');

const command = process.argv[2] || 'inventory';
if (command === 'inventory') {
  const simplify = value => JSON.parse(JSON.stringify(value, (key, v) => typeof v === 'function' ? undefined : v));
  const result = {
    state: wb.state(),
    maps: simplify(wb.maps),
    mobileWidths: simplify(wb.widths),
    materials: simplify(wb.materials),
    crackable: Array.from(wb.crackable), crackWalk: wb.crackWalk, crackStop: wb.crackStop,
    tools: Object.keys(wb.tools).map(id => ({
      id,
      name: wb.tools[id].name,
      mode: wb.tools[id].mode || 'destroy',
      slider: simplify(wb.tools[id].slider),
      extra: wb.tools[id].extra || null
    })),
    buildings: simplify(wb.buildings),
    propIds: Object.keys(wb.props),
    props: simplify(wb.props),
    initial: mapDigest(wb).summary
  };
  const out = path.join(root, 'original/recovered/inventory.json');
  fs.writeFileSync(out, JSON.stringify(result, null, 2) + '\n');
  console.log(JSON.stringify({ output: out, state: result.state, materials: result.materials.length,
    tools: result.tools.map(t => t.id), buildings: result.buildings.map(b => b.id),
    propCount: result.propIds.length, initial: result.initial }, null, 2));
} else if (command === 'maps') {
  const outDir = path.join(root, 'original/recovered/map-oracle');
  fs.mkdirSync(outDir, { recursive: true });
  for (const map of Object.keys(wb.maps)) {
    wb.loadMap(map, 1);
    const digest = mapDigest(wb);
    fs.writeFileSync(path.join(outDir, map + '.json'), JSON.stringify(digest) + '\n');
    console.log(map, JSON.stringify(digest.summary));
  }
} else if (command === 'export') {
  const outDir = path.join(root, 'src/main/resources/maps');
  fs.mkdirSync(outDir, { recursive: true });
  const manifest = { format: 'WBMP/1', cellHeight: 260, cellPixels: 6, maps: [] };
  let mapIndex = 0;
  for (const map of Object.keys(wb.maps)) {
    wb.loadMap(map, 1);
    const data = exportMap(wb, mapIndex++);
    fs.writeFileSync(path.join(outDir, map + '.wbmp'), data.bytes);
    delete data.bytes;
    manifest.maps.push(Object.assign({ id: map, file: map + '.wbmp' }, data));
    console.log(map, JSON.stringify(data));
  }
  fs.writeFileSync(path.join(outDir, 'index.json'), JSON.stringify(manifest, null, 2) + '\n');
} else if (command === 'natural') {
  const map = process.argv[3] || 'port';
  const seconds = Number(process.argv[4] || 40);
  wb.loadMap(map, 1);
  let solid = 0;
  for (let i = 0; i < wb.arrays.material.length; ++i) if (wb.arrays.material[i]) ++solid;
  const steps = Math.round(seconds * 60);
  const track = Number(process.argv[5] || 45), trace = [];
  function snapshot(step) {
    if (track < 0 || track >= wb.people.length) return;
    const p = wb.people[track];
    trace.push({ step, x: p.x, y: p.y, hp: p.hp, waterT: p.waterT,
      state: p.state, dead: !!p.dead, onGround: p.onGround, rag: !!p.rag });
  }
  snapshot(0);
  for (let i = 0; i < steps; ++i) {
    wb.step(1 / 60);
    if ((i + 1) % 600 === 0) snapshot(i + 1);
  }
  let remaining = 0;
  for (let i = 0; i < wb.arrays.material.length; ++i) if (wb.arrays.material[i]) ++remaining;
  const dead = [];
  wb.people.forEach((p, index) => {
    if (p.dead) dead.push({ index, x: p.x, y: p.y, cause: p.cause, waterT: p.waterT });
  });
  console.log(JSON.stringify({ map, seconds, steps, deaths: wb.stats.dead, dead,
    lostCells: solid - remaining, bodies: wb.bodies.length,
    deadBodies: wb.bodies.filter(b => b.dead).length, trace }, null, 2));
} else if (command === 'inspect') {
  function describe(value) {
    if (!value) return value;
    const out = {};
    for (const key of Object.keys(value)) {
      const v = value[key];
      if (v == null || typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') out[key] = v;
      else if (Array.isArray(v) || ArrayBuffer.isView(v)) out[key] = { type: v.constructor.name, length: v.length, sample: Array.from(v).slice(0, 12) };
      else out[key] = { type: v.constructor && v.constructor.name || 'Object', keys: Object.keys(v).slice(0, 30) };
    }
    return out;
  }
  console.log(JSON.stringify({ bodies: wb.bodies.slice(0, 3).map(describe), people: wb.people.slice(0, 2).map(describe),
    firstProps: Object.keys(wb.props).slice(0, 5).map(k => ({ id: k, value: describe(wb.props[k]) })),
    specials: Object.fromEntries(Object.entries(wb.special()).map(([k, v]) => [k, describe(v)])) }, null, 2));
} else if (command === 'strike') {
  wb.loadMap(process.argv[3] || 'city', Number(process.argv[4] || 1));
  const before = mapDigest(wb);
  const beforeHealth = wb.arrays.health.slice();
  wb.selectTool('hammer');
  const state = wb.state();
  const hit = findSolid(wb), randomBefore = randomState;
  wb.strike(hit.x * 6 + 3, hit.y * 6 + 3);
  wb.structural();
  const after = mapDigest(wb), healthChanges = [];
  for (let i = 0; i < beforeHealth.length; ++i) if (beforeHealth[i] !== wb.arrays.health[i]) {
    healthChanges.push({ index: i, x: i % before.summary.width, y: (i / before.summary.width) | 0,
      material: before.material[i], before: beforeHealth[i], after: wb.arrays.health[i] });
  }
  console.log(JSON.stringify({ hit, randomBefore, before: before.summary, after: after.summary,
    changed: changedCells(wb, before.material), healthChanges }, null, 2));
} else {
  throw new Error('Unknown command: ' + command);
}

function findSolid(wb) {
  const s = wb.state();
  for (let y = 213; y >= 0; --y) for (let x = (s.width / 2) | 0; x < s.width; ++x) {
    const m = wb.arrays.material[y * s.width + x];
    if (m && m !== 8 && m !== 9) return { x, y, material: m };
  }
  throw new Error('No destructible cell');
}
function mapDigest(wb) {
  const s = wb.state();
  const a = wb.arrays;
  const counts = new Array(wb.materials.length).fill(0);
  let solid = 0, user = 0, health = 0, hash = 2166136261 >>> 0;
  const material = new Uint8Array(s.size);
  for (let i = 0; i < s.size; ++i) {
    const m = a.material[i]; material[i] = m;
    if (m) { ++solid; ++counts[m]; health += a.health[i]; }
    if (a.flags[i] & 1) ++user;
    hash ^= m | (((m ? a.health[i] : 0) * 16) | 0) << 8 | a.flags[i] << 24;
    hash = Math.imul(hash, 16777619) >>> 0;
  }
  return { state: s, counts, material: Array.from(material),
    summary: { map: s.map, seed: s.seed, width: s.width, cells: s.size, solid, user,
      health: Math.round(health * 100) / 100, bodies: wb.bodies.length, people: wb.people.length,
      buildings: wb.buildingsOut.length, hash: ('00000000' + hash.toString(16)).slice(-8) } };
}
function changedCells(wb, before) {
  const s = wb.state(); const out = [];
  for (let i = 0; i < s.size && out.length < 100; ++i) {
    if (before[i] !== wb.arrays.material[i]) out.push({ index: i, before: before[i], after: wb.arrays.material[i] });
  }
  return out;
}

function exportMap(wb, mapIndex) {
  const state = wb.state(), a = wb.arrays, runs = [], patches = [];
  let previousEnd = 0;
  for (let i = 0; i < state.size;) {
    const m = a.material[i];
    if (!m) { ++i; continue; }
    const start = i, tint = a.tint[i], flags = a.flags[i];
    let end = i + 1;
    while (end < state.size && a.material[end] === m && a.tint[end] === tint && a.flags[end] === flags) ++end;
    runs.push([start - previousEnd, end - start, m, tint, flags]);
    previousEnd = end; i = end;
  }
  for (let i = 0; i < state.size; ++i) {
    const m = a.material[i];
    if (m && Math.abs(a.health[i] - wb.materials[m].hp) > 0.001) patches.push([i, a.health[i]]);
  }
  let oracleHash = 2166136261 >>> 0, solid = 0, user = 0;
  for (let i = 0; i < state.size; ++i) {
    const m = a.material[i];
    if (m) { ++solid; if (a.flags[i] & 1) ++user; }
    oracleHash ^= m | (((m ? a.health[i] : 0) * 16) | 0) << 8 | a.flags[i] << 24;
    oracleHash = Math.imul(oracleHash, 16777619) >>> 0;
  }
  const bodies = wb.bodies.filter(b => !b.dead);
  const people = wb.people.filter(p => !p.gone);
  const typeNames = Array.from(new Set(bodies.map(b => b.pt || ('@kind' + b.kind))));
  const special = wb.special();
  const nodes = special.power && special.power.nodes || [];
  const spans = special.power && special.power.spans || [];
  const out = [];
  const u8 = v => out.push(v & 255);
  const u16 = v => { u8(v >>> 8); u8(v); };
  const u32 = v => { u8(v >>> 24); u8(v >>> 16); u8(v >>> 8); u8(v); };
  const vint = v => { v >>>= 0; while (v >= 128) { u8((v & 127) | 128); v >>>= 7; } u8(v); };
  const f32 = v => { const b = Buffer.allocUnsafe(4); b.writeFloatBE(Number.isFinite(v) ? v : 0, 0); for (const x of b) out.push(x); };
  const ascii = s => { const b = Buffer.from(s, 'ascii'); u8(b.length); for (const x of b) out.push(x); };
  for (const c of [87, 66, 77, 80, 1, mapIndex]) u8(c); // WBMP, version 1
  u16(state.width); u16(260); u32(runs.length); u16(bodies.length); u16(people.length);
  u16(typeNames.length); u16(nodes.length); u16(spans.length); u32(patches.length);
  const river = special.river, volcano = special.volcano;
  f32(river ? river.x0 : 0); f32(river ? river.x1 : 0); f32(river ? river.bottom : 0); f32(river ? river.waterY : 0);
  f32(volcano ? volcano.xc : 0); f32(volcano ? volcano.craterY : 0); f32(volcano ? volcano.x0 : 0); f32(volcano ? volcano.H : 0);
  u32((river ? 1 : 0) | (volcano ? 2 : 0) | (special.npp ? 4 : 0) | (special.base ? 8 : 0) | (river && river.sea ? 16 : 0));
  for (const run of runs) { vint(run[0]); vint(run[1]); u8(run[2]); u8(run[3]); u8(run[4]); }
  for (const patch of patches) { vint(patch[0]); f32(patch[1]); }
  for (const name of typeNames) ascii(name);
  for (const b of bodies) {
    u8(b.kind); u16(typeNames.indexOf(b.pt || ('@kind' + b.kind)));
    f32(b.x); f32(b.y); f32(b.a); f32(b.mass); f32(b.php || 1); f32(b.pw || 4); f32(b.ph || 4);
    u8(b.variant || 0);
    u8((b.pinned ? 1 : 0) | (b.sleeping ? 2 : 0) | (b.def && b.def.explosive ? 4 : 0) | (b.charge ? 8 : 0) | (b.hangLen ? 16 : 0));
  }
  for (const p of people) {
    f32(p.x); f32(p.y); f32(p.hp); f32(p.spd); f32(p.runSpd);
    u8(p.dir < 0 ? 255 : 1); u8(p.id & 255); u8(p.role === 'work' ? 1 : p.role === 'home' ? 2 : 0);
  }
  for (const n of nodes) { f32(n.x); f32(n.top); u32(n.att >>> 0); u8((n.ok ? 1 : 0) | (n.pw ? 2 : 0) | (n.edge ? 4 : 0)); }
  for (const s of spans) { u16(s.a); u16(s.b); u8(s.type === 'air' ? 0 : s.type === 'cable' ? 1 : s.type === 'roof' ? 2 : 3); u8(s.ok ? 1 : 0); }
  const bytes = Buffer.from(out);
  return { bytes, width: state.width, cells: state.size, solid, user, oracleHash: ('00000000' + oracleHash.toString(16)).slice(-8), runs: runs.length, healthPatches: patches.length,
    bodies: bodies.length, people: people.length, propTypes: typeNames.length, powerNodes: nodes.length,
    powerSpans: spans.length, fileBytes: bytes.length, sha256: require('crypto').createHash('sha256').update(bytes).digest('hex') };
}
