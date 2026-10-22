import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.dirname(fileURLToPath(import.meta.url));
const runtimeRoot = path.join(root, 'node_modules', 'j2me-web', 'dist');
const releaseRoot = path.join(root, '..', 'release');
const port = Number(process.env.PORT || 3000);

const files = {
  '/': [path.join(root, 'index.html'), 'text/html; charset=utf-8'],
  '/runtime.js': [path.join(runtimeRoot, 'index.js'), 'text/javascript; charset=utf-8'],
  '/game.jar': [path.join(releaseRoot, 'RetroDestructionLab.jar'), 'application/java-archive'],
  '/game.jad': [path.join(releaseRoot, 'RetroDestructionLab.jad'), 'text/vnd.sun.j2me.app-descriptor'],
};

const server = http.createServer((request, response) => {
  const url = new URL(request.url, 'http://preview.invalid');
  let file = files[url.pathname];
  if (!file && url.pathname.startsWith('/static/')) {
    const relative = path.normalize(url.pathname).replace(/^[/\\]+/, '');
    const candidate = path.join(runtimeRoot, relative);
    if (candidate.startsWith(runtimeRoot + path.sep)) {
      file = [candidate, url.pathname.endsWith('.wasm')
        ? 'application/wasm' : 'application/octet-stream'];
    }
  }
  if (!file) {
    response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
    response.end('Not found');
    return;
  }
  fs.stat(file[0], (error, stat) => {
    if (error || !stat.isFile()) {
      response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
      response.end('Build the MIDlet and install preview dependencies first.');
      return;
    }
    response.writeHead(200, {
      'content-type': file[1],
      'content-length': stat.size,
      'cache-control': 'no-cache',
      'cross-origin-resource-policy': 'same-origin',
    });
    if (request.method === 'HEAD') {
      response.end();
    } else {
      fs.createReadStream(file[0]).pipe(response);
    }
  });
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Retro Destruction Lab J2ME preview: http://0.0.0.0:${port}`);
});
