import { useEffect, useState } from 'react';

/** Медиа-запрос как React-состояние (SSR-safe). */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false;
    return window.matchMedia(query).matches;
  });

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mql = window.matchMedia(query);
    const handler = (e: MediaQueryListEvent) => setMatches(e.matches);
    setMatches(mql.matches);
    if (mql.addEventListener) mql.addEventListener('change', handler);
    else mql.addListener(handler);
    return () => {
      if (mql.removeEventListener) mql.removeEventListener('change', handler);
      else mql.removeListener(handler);
    };
  }, [query]);

  return matches;
}

/** Принудительно мобильный вид: ?mobile=1 в адресе или флаг в localStorage. */
function forcedMobile(): boolean {
  if (typeof window === 'undefined') return false;
  try {
    const params = new URLSearchParams(window.location.search);
    if (params.get('mobile') === '1') return true;
    if (params.get('mobile') === '0') return false;
    return window.localStorage?.getItem('homelab-mobile') === '1';
  } catch {
    return false;
  }
}

/** Телефон / узкий экран: другая навигация и крупные элементы управления. */
export function useIsMobile(): boolean {
  const narrow = useMediaQuery('(max-width: 760px)');
  return narrow || forcedMobile();
}

export function useIsTouch(): boolean {
  const coarse = useMediaQuery('(pointer: coarse)');
  const [touched, setTouched] = useState(false);
  useEffect(() => {
    setTouched(typeof window !== 'undefined' && ('ontouchstart' in window || (navigator.maxTouchPoints ?? 0) > 0));
  }, []);
  return coarse || touched;
}

/** Короткая вибрация для тактильного отклика (Android WebView / телефоны). */
export function haptic(pattern: number | number[] = 12): void {
  try {
    if (typeof navigator !== 'undefined' && typeof navigator.vibrate === 'function') {
      navigator.vibrate(pattern);
    }
  } catch {
    /* не поддерживается — молча игнорируем */
  }
}
