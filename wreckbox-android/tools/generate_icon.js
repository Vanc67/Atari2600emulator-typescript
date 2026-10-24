/* Deterministic 96x96 pixel icon; no external packages. */
'use strict';
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const W = 96, H = 96, pixels = new Uint8Array(W * H * 4);
function put(x, y, color) {
  if (x < 0 || y < 0 || x >= W || y >= H) return;
  const at = (y * W + x) * 4;
  pixels[at] = color[0]; pixels[at + 1] = color[1]; pixels[at + 2] = color[2]; pixels[at + 3] = 255;
}
function rect(x, y, w, h, color) { for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) put(xx, yy, color); }
function circle(cx, cy, radius, color) {
  for (let y = -radius; y <= radius; y++) for (let x = -radius; x <= radius; x++)
    if (x * x + y * y <= radius * radius) put(cx + x, cy + y, color);
}
const dark = [16, 25, 35], edge = [38, 59, 76], yellow = [230, 195, 78];
rect(0, 0, W, H, dark); rect(0, 0, W, 4, edge); rect(0, H - 4, W, 4, edge);
rect(0, 0, 4, H, edge); rect(W - 4, 0, 4, H, edge);
// Broken brick building, with lit windows and an exposed rebar column.
for (let y = 22; y < 78; y += 8) for (let x = 12; x < 48; x += 12) {
  if (x >= 36 && y < 42) continue;
  const tone = ((x + y) & 8) ? [158, 76, 52] : [178, 88, 58];
  rect(x, y, 11, 7, tone); rect(x, y + 6, 11, 1, [94, 48, 39]);
}
for (let y = 30; y < 70; y += 16) for (let x = 17; x < 42; x += 16) rect(x, y, 5, 6, [236, 198, 94]);
rect(42, 36, 3, 42, [119, 129, 135]);
for (let y = 40; y < 76; y += 8) rect(39, y, 9, 2, [102, 111, 118]);
// Chain and steel wrecking ball.
for (let i = 0; i < 35; i++) {
  const x = 82 - (i * 20 / 35 | 0), y = 9 + i;
  rect(x, y, 2, 3, i % 4 < 2 ? [159, 169, 174] : [82, 94, 104]);
}
circle(59, 56, 18, [43, 51, 59]); circle(55, 51, 13, [91, 104, 116]);
circle(51, 46, 5, [152, 163, 169]); rect(45, 70, 27, 4, [28, 34, 40]);
// Impact fragments and a small original-style yellow wordmark bar.
rect(72, 66, 5, 5, yellow); rect(78, 74, 4, 4, [202, 110, 55]); rect(68, 79, 3, 3, yellow);
rect(12, 84, 72, 3, yellow); rect(12, 89, 43, 2, [137, 157, 170]);
const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? 0xedb88320 ^ (c >>> 1) : c >>> 1; crcTable[n] = c >>> 0; }
function crc32(bytes) { let c = 0xffffffff; for (const b of bytes) c = crcTable[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }
function chunk(name, data) {
  const out = Buffer.alloc(12 + data.length); out.writeUInt32BE(data.length, 0); out.write(name, 4, 'ascii');
  data.copy(out, 8); out.writeUInt32BE(crc32(Buffer.concat([Buffer.from(name), data])), 8 + data.length); return out;
}
const raw = Buffer.alloc(H * (W * 4 + 1));
for (let y = 0; y < H; y++) { const row = y * (W * 4 + 1); raw[row] = 0; Buffer.from(pixels.buffer, y * W * 4, W * 4).copy(raw, row + 1); }
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
const bytes = Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, {level: 9})), chunk('IEND', Buffer.alloc(0))]);
const output = path.resolve(__dirname, '../res/drawable/icon.png');
fs.mkdirSync(path.dirname(output), {recursive: true}); fs.writeFileSync(output, bytes);
console.log('icon.png', bytes.length, 'bytes');
