// Rasterise an SVG to PNG with transparent background using headless Chrome (CDP).
import { spawn } from 'node:child_process';
import fs from 'node:fs';
const [,, svgPath, out, size = '1024'] = process.argv;
const svg = fs.readFileSync(svgPath, 'utf8');
const html = `<html><body style="margin:0;background:transparent">${svg.replace(/width="\d+" height="\d+"/, `width="${size}" height="${size}"`)}</body></html>`;
fs.writeFileSync(process.cwd() + '/_r.html', html);
const port = 9400 + Math.floor(Math.random() * 400);
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  ['--headless=new', '--disable-gpu', `--remote-debugging-port=${port}`, '--user-data-dir=' + process.cwd() + '/chprof_r' + port, 'about:blank']);
await new Promise(r => setTimeout(r, 1500));
const t = (await (await fetch(`http://127.0.0.1:${port}/json`)).json()).find(t => t.type === 'page');
const ws = new WebSocket(t.webSocketDebuggerUrl); let id = 0; const pend = {};
const send = (m, p = {}) => new Promise(res => { const i = ++id; pend[i] = res; ws.send(JSON.stringify({ id: i, method: m, params: p })); });
ws.onmessage = e => { const m = JSON.parse(e.data); if (m.id && pend[m.id]) pend[m.id](m.result); };
await new Promise(r => ws.onopen = r);
await send('Emulation.setDeviceMetricsOverride', { width: +size, height: +size, deviceScaleFactor: 1, mobile: false });
await send('Emulation.setDefaultBackgroundColorOverride', { color: { r: 0, g: 0, b: 0, a: 0 } });
await send('Page.navigate', { url: 'file://' + process.cwd() + '/_r.html' });
await new Promise(r => setTimeout(r, 800));
const shot = await send('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: 0, width: +size, height: +size, scale: 1 } });
fs.writeFileSync(out, Buffer.from(shot.data, 'base64'));
ws.close(); chrome.kill();
