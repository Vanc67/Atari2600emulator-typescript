/* Клиентский смоук: поднимаем jsdom, монтируем App и кликаем по интерфейсу. */
import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body><div id="root"></div></body></html>', {
  url: 'http://localhost/',
  pretendToBeVisual: true,
});
const g = globalThis as unknown as Record<string, unknown>;
g.window = dom.window;
g.document = dom.window.document;
Object.defineProperty(g, 'navigator', { value: dom.window.navigator, configurable: true });
g.HTMLElement = dom.window.HTMLElement;
g.Node = dom.window.Node;
g.Event = dom.window.Event;
g.MouseEvent = dom.window.MouseEvent;
Object.defineProperty(g, 'localStorage', { value: dom.window.localStorage, configurable: true });
g.requestAnimationFrame = (cb: FrameRequestCallback) => setTimeout(() => cb(Date.now()), 0) as unknown as number;
g.cancelAnimationFrame = (id: number) => clearTimeout(id);
g.IS_REACT_ACT_ENVIRONMENT = true;

async function main() {
  const React = await import('react');
  const { createRoot } = await import('react-dom/client');
  const App = (await import('../src/App')).default;

  const rootEl = document.getElementById('root');
  if (!rootEl) throw new Error('нет #root');
  const root = createRoot(rootEl);
  root.render(React.createElement(App));
  await new Promise((r) => setTimeout(r, 150));
  console.log('render ok');

  const text = () => document.body.textContent ?? '';
  const must = (cond: boolean, msg: string) => {
    if (!cond) {
      console.error('FAIL:', msg);
      process.exitCode = 1;
    }
  };

  must(text().includes('HOMELAB'), 'шапка отрендерилась');
  must(text().includes('Как играть'), 'модалка правил при первом запуске');
  must(text().includes('Ты — человек'), 'описание правил на месте');

  // Закрываем правила
  const buttons = () => Array.from(document.querySelectorAll('button'));
  const byText = (t: string) => buttons().find((b) => (b.textContent ?? '').includes(t));
  byText('Понятно, играем')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 80));
  must(!text().includes('Ты — человек'), 'правила закрылись');
  must(text().includes('Что делать дальше'), 'панель подсказок на месте');
  must(text().includes('Родительский ПК'), 'стартовая машина видна');

  // Переход на вкладку «Машины и ОС»
  byText('Машины и ОС')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 80));
  must(text().includes('Железо и сборка'), 'вкладка сборки открылась');

  // Включаем сервер
  byText('Включить')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 80));
  must(text().includes('Ubuntu Server') && text().includes('Установить Linux'), 'предложение установить ОС');

  // Ставим Linux
  byText('Установить Linux')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 80));
  must(text().includes('БЫСТРАЯ УСТАНОВКА') || text().includes('apt install'), 'терминал и пакеты доступны');

  // Барахолка
  byText('Барахолка')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 80));
  must(text().includes('Торг'), 'лоты с торгом на месте');

  // Завершаем день и закрываем отчёт
  byText('Завершить день')?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 120));
  must(text().includes('Итоги дня'), 'отчёт о дне открылся');
  const nextBtn = byText('Продолжить') ?? byText('Дальше');
  nextBtn?.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  await new Promise((r) => setTimeout(r, 120));
  must(text().includes('День') || text().includes('день'), 'день сменился');

  console.log('после кликов, длина текста:', text().length);
  console.log(process.exitCode === 1 ? '❌ DOM-СМОУК ПРОВАЛЕН' : '✅ DOM-СМОУК ПРОЙДЕН');
  process.exit(process.exitCode === 1 ? 1 : 0);
}

main().catch((e) => {
  console.error('CRASH', e);
  process.exitCode = 1;
});
