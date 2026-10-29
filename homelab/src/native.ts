import { haptic } from './hooks/useMedia';

/** Работает ли приложение внутри Android/iOS-оболочки (Capacitor). */
export function isNative(): boolean {
  if (typeof window === 'undefined') return false;
  const cap = (window as unknown as { Capacitor?: { isNativePlatform?: () => boolean } }).Capacitor;
  return !!cap?.isNativePlatform?.();
}

/**
 * Кнопка «Назад» на Android:
 *  1. закрывает открытое окно/шторку;
 *  2. возвращает в раздел «Комната»;
 *  3. сворачивает приложение (а не выкидывает из него).
 * Возвращает функцию отписки.
 */
export async function bindBackButton(handler: () => boolean): Promise<() => void> {
  if (!isNative()) return () => undefined;
  try {
    const { App } = await import('@capacitor/app');
    const sub = await App.addListener('backButton', async () => {
      const handled = handler();
      if (!handled) {
        haptic(20);
        try {
          await App.minimizeApp();
        } catch {
          /* ignore */
        }
      }
    });
    return () => {
      void sub.remove();
    };
  } catch {
    return () => undefined;
  }
}

/**
 * Нативная оболочка: тёмные системные панели, запрет сна экрана,
 * вибрация уже работает через navigator.vibrate в WebView.
 */
export async function initNativeShell(): Promise<void> {
  if (!isNative()) return;
  try {
    const { StatusBar, Style } = await import('@capacitor/status-bar');
    await StatusBar.setStyle({ style: Style.Dark });
    await StatusBar.setBackgroundColor({ color: '#080b11' });
  } catch {
    /* плагин может отсутствовать */
  }
}
