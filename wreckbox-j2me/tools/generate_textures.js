/*
 * Rebuild the small MIDP material atlases from the recovered Wreckbox 1.8
 * tn()/en() rasterizer. No third-party packages are needed: node tools/generate_textures.js
 */
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const zlib = require('zlib');

const root = path.resolve(__dirname, '..');
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

// Only the fields consulted for an intact, unflagged cell are needed here.
// Values are the recovered nt[].base/hp table (the Java Materials table is
// generated from the same oracle).
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
function tile(material, tint) {
  const pixels = new Uint8Array(36 * 3);
  context.tn(material, tint, hp[material], 0, 0, 0, 0, material, material, material, material);
  for (let y = 0; y < 6; y++) for (let x = 0; x < 6; x++) {
    context.en(x, y, x, y);
    const at = (y * 6 + x) * 3;
    pixels[at] = clamp(context.je); pixels[at + 1] = clamp(context.Xe); pixels[at + 2] = clamp(context.Ze);
  }
  return pixels;
}

// PNG encoder kept local so the generated assets are reproducible in the
// offline build environment as well as on a developer machine.
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
function png(width, height, rgb) {
  const scan = Buffer.alloc((width * 3 + 1) * height);
  for (let y = 0; y < height; y++) {
    scan[y * (width * 3 + 1)] = 0;
    Buffer.from(rgb.buffer, rgb.byteOffset + y * width * 3, width * 3).copy(scan, y * (width * 3 + 1) + 1);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0); ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; ihdr[9] = 2; // 8-bit RGB, no alpha: old MIDP PNG decoders are happiest this way.
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(scan, {level: 9})), chunk('IEND', Buffer.alloc(0))]);
}

const output = path.join(root, 'src/main/resources/textures');
fs.mkdirSync(output, {recursive: true});
const tiles = [];
for (let m = 1; m <= 20; m++) for (let tint = 0; tint <= 26; tint++) tiles.push({m, tint, rgb: tile(m, tint)});
for (const size of [2, 3, 6]) {
  const width = 27 * size, height = 20 * size, rgb = new Uint8Array(width * height * 3);
  for (const entry of tiles) {
    const ox = entry.tint * size, oy = (entry.m - 1) * size;
    for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
      const sx = Math.min(5, (x * 6 / size) | 0), sy = Math.min(5, (y * 6 / size) | 0);
      const from = (sy * 6 + sx) * 3, to = ((oy + y) * width + ox + x) * 3;
      rgb[to] = entry.rgb[from]; rgb[to + 1] = entry.rgb[from + 1]; rgb[to + 2] = entry.rgb[from + 2];
    }
  }
  const file = path.join(output, 'materials' + size + '.png');
  fs.writeFileSync(file, png(width, height, rgb));
  console.log(path.relative(root, file) + ' ' + width + 'x' + height + ' ' + fs.statSync(file).size + ' bytes');
}
