/*
 * Rebuild the MIDP material atlases from the recovered Wreckbox 1.8
 * tn()/en() rasterizer. No third-party packages are needed.
 *
 * Intact cells use several world-coordinate phases instead of repeating one
 * 6x6 tile everywhere. Glass/rebar keep the original alpha, and night atlases
 * use Re()'s exact 0.38/0.40/0.50 channel multipliers.
 * node tools/generate_textures.js [--check]
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const zlib = require('zlib');

const root = path.resolve(__dirname, '..');
const CHECK = process.argv.includes('--check');
const source = fs.readFileSync(path.join(root, 'original/recovered/game.pretty.js'), 'utf8');
function extract(name) {
  const start = source.indexOf('function ' + name + '(');
  if (start < 0) throw new Error('missing recovered function ' + name);
  const open = source.indexOf('{', start);
  let depth = 0, quote = '', escaped = false;
  for (let i = open; i < source.length; i++) {
    const c = source[i];
    if (quote) {
      if (escaped) escaped = false;
      else if (c === '\\') escaped = true;
      else if (c === quote) quote = '';
      continue;
    }
    if (c === '"' || c === "'" || c === '`') { quote = c; continue; }
    if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return source.slice(start, i + 1);
  }
  throw new Error('unterminated recovered function ' + name);
}

// Only fields consulted for an intact, unflagged cell are needed here. Values
// are the recovered nt[].base/hp table; Java Materials comes from the same oracle.
const bases = [
  null, [158,112,66], [170,80,56], [158,158,152], [98,110,126],
  [172,216,236], [228,224,214], [132,76,48], [118,86,56], [62,60,64],
  [168,64,46], [118,86,56], [255,120,30], [56,52,52], [184,182,174],
  [228,226,218], [150,154,150], [168,206,220], [96,100,112], [66,64,62],
  [136,126,112]
];
const hp = [1,60,100,150,320,10,24,45,110,9999,30,70,255,140,160,40,22,45,110,220,120];
const rt = [null,
  [214,92,70], [88,134,182], [232,202,112], [122,162,112], [236,233,224],
  [66,67,72], [152,147,140], [104,62,40], [196,178,140], [70,112,150],
  [120,200,194], [188,118,62], [140,44,38], [92,100,72], [196,196,190],
  [60,64,72], [72,112,82], [96,78,62], [70,62,60], [190,60,24],
  [204,176,118], [96,64,44], [92,92,98], [40,58,92], [236,168,36], [72,84,70]
];
const context = {
  Math, nt: bases.map((base, i) => ({base, hp: hp[i], fixed: false, dust: base || [0,0,0]})), rt,
  N: 12, K: 14, V: 11, H: 13, j: 15, X: 16, Z: 17, J: 18, Q: 19, tt: 20, _: 10,
  dt: 32, ct: 64, it: 16, lt: 32, st: 18, ve: 128,
  At: new Int16Array(4096).fill(214), Ct: new Float32Array(4096), p: 214, hb: null,
  De: null, Le: [], Ne: 0, He: 0, Ke: false, Te: false,
  fn: 0, xn: false, gn: false, yn: false, bn: false, hn: false,
  sn: 1, ln: 1, cn: 1
};
vm.createContext(context);
for (const name of ['q', 'U', 'tn', 'en']) vm.runInContext(extract(name), context);

function clamp(v) { return v <= 0 ? 0 : v >= 255 ? 255 : v | 0; }
const tileCache = new Map();
function tile(material, tint, vx, vy, flags) {
  const key = material + '/' + tint + '/' + vx + '/' + vy + '/' + flags;
  if (tileCache.has(key)) return tileCache.get(key);
  // Keep unclamped floats: original night multiplication happens before the
  // final 8-bit clamp, which matters for bright reflections and metal edges.
  const pixels = new Float32Array(36 * 4);
  context.tn(material, tint, hp[material], flags, 0, vx, vy,
    material, material, material, material);
  for (let y = 0; y < 6; y++) for (let x = 0; x < 6; x++) {
    context.en(x, y, 6 * vx + x, 6 * vy + y);
    const at = (y * 6 + x) * 4;
    pixels[at] = context.je; pixels[at + 1] = context.Xe;
    pixels[at + 2] = context.Ze; pixels[at + 3] = context.Je;
  }
  tileCache.set(key, pixels);
  return pixels;
}

// PNG encoder kept local so generated assets are reproducible offline.
const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
  crcTable[n] = c >>> 0;
}
function crc32(bytes) {
  let c = 0xffffffff;
  for (const b of bytes) c = crcTable[(c ^ b) & 255] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const name = Buffer.from(type, 'ascii');
  const out = Buffer.alloc(12 + data.length);
  out.writeUInt32BE(data.length, 0); name.copy(out, 4); data.copy(out, 8);
  out.writeUInt32BE(crc32(Buffer.concat([name, data])), 8 + data.length);
  return out;
}
function png(width, height, channels, pixels) {
  const stride = width * channels;
  const scan = Buffer.alloc((stride + 1) * height);
  for (let y = 0; y < height; y++) {
    scan[y * (stride + 1)] = 0;
    Buffer.from(pixels.buffer, pixels.byteOffset + y * stride, stride).copy(scan, y * (stride + 1) + 1);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = channels === 4 ? 6 : 2;
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(scan, {level: 9})), chunk('IEND', Buffer.alloc(0))]);
}

const output = path.join(root, 'src/main/resources/textures');
fs.mkdirSync(output, {recursive: true});
let failed = false;
function emit(name, bytes) {
  const file = path.join(output, name);
  if (CHECK) {
    const actual = fs.readFileSync(file);
    if (!actual.equals(bytes)) { console.error('MISMATCH ' + name); failed = true; }
    else console.log('OK ' + name + ' ' + bytes.length + ' bytes');
  } else {
    fs.writeFileSync(file, bytes);
    console.log(name + ' ' + bytes.length + ' bytes');
  }
}

function variantsX(size) { return size === 3 ? 4 : 2; }
function makeAtlas(size, night, alpha) {
  // Base rows 20/21 are jointed steel/panels; alpha row 3 is vertical rebar.
  const vxCount = variantsX(size), rows = alpha ? 4 : 22, channels = alpha ? 4 : 3;
  const width = 27 * size * vxCount, height = rows * size * 2;
  const rgb = new Uint8Array(width * height * channels);
  for (let vy = 0; vy < 2; vy++) for (let vx = 0; vx < vxCount; vx++) {
    for (let row = 0; row < rows; row++) {
      const material = alpha ? (row === 0 ? 5 : row === 2 ? 17 : 7) : row < 20 ? row + 1 : row === 20 ? 4 : 14;
      const flags = alpha ? (row === 3 ? 16 : 0) : row >= 20 ? 32 : 0;
      for (let tint = 0; tint <= 26; tint++) {
        const pixels = tile(material, tint, vx, vy, flags);
        const ox = (vx * 27 + tint) * size;
        const oy = (vy * rows + row) * size;
        for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
          const sx = Math.min(5, (x * 6 / size) | 0), sy = Math.min(5, (y * 6 / size) | 0);
          const from = (sy * 6 + sx) * 4;
          const to = ((oy + y) * width + ox + x) * channels;
          rgb[to] = clamp(pixels[from] * (night ? 0.38 : 1));
          rgb[to + 1] = clamp(pixels[from + 1] * (night ? 0.40 : 1));
          rgb[to + 2] = clamp(pixels[from + 2] * (night ? 0.50 : 1));
          if (alpha) rgb[to + 3] = clamp(pixels[from + 3]);
        }
      }
    }
  }
  return png(width, height, channels, rgb);
}

for (const size of [2, 3, 6]) {
  for (const night of [false, true]) {
    const suffix = night ? 'Night' : '';
    emit('materials' + size + suffix + '.png', makeAtlas(size, night, false));
    emit('materialsAlpha' + size + suffix + '.png', makeAtlas(size, night, true));
  }
}
if (failed) process.exitCode = 1;
