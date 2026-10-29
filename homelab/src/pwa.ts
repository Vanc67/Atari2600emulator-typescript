/** Регистрация service worker (только в браузере и только на http(s)). */
export function registerServiceWorker(): void {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) return;
  if (location.protocol !== 'http:' && location.protocol !== 'https:') return;
  window.addEventListener('load', () => {
    const url = new URL('sw.js', document.baseURI).toString();
    navigator.serviceWorker.register(url).catch(() => undefined);
  });
}
