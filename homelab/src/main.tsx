import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { registerServiceWorker } from './pwa';
import './styles.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

// Убираем экран загрузки, когда React отрисовал первый кадр
requestAnimationFrame(() => {
  const boot = document.getElementById('boot');
  if (boot) {
    boot.style.transition = 'opacity .25s ease';
    boot.style.opacity = '0';
    setTimeout(() => boot.remove(), 260);
  }
});

registerServiceWorker();
