import { SERVICES } from './data';
import { contractAvailable, derive, firewallScore, machineRam, usableRam } from './rules';
import type { GameState } from './types';

export interface Hint {
  text: string;
  icon: string;
  tone?: 'amber' | 'green' | 'cyan';
}

/** Подсказки «что делать дальше» — короткий план на текущий момент. */
export function nextSteps(s: GameState): Hint[] {
  const hints: Hint[] = [];
  const machines = s.machines;
  const up = machines.filter((m) => derive(m, s).up);

  if (machines.length === 0) {
    hints.push({ icon: '🛒', text: 'Купи платформу на барахолке (фильтр «Платформа»), дождись доставки и собери машину во вкладке «Машины и ОС».', tone: 'amber' });
    return hints;
  }

  if (up.length === 0) {
    const m = machines[0];
    if (!m.powered) hints.push({ icon: '⏻', text: `Включи ${m.name} во вкладке «Машины и ОС».`, tone: 'amber' });
    else if (m.crashed) hints.push({ icon: '🧰', text: `${m.name} в аварии — нажми «Починить дефект» в блоке обслуживания.`, tone: 'amber' });
    else if (!m.cpus.some(Boolean) && m.kind !== 'phone') hints.push({ icon: '🧩', text: 'Установи процессор в сокет — без него сервер не запустится.' });
    else if (machineRam(m) === 0) hints.push({ icon: '🧠', text: 'Нужна оперативная память. Купи планку DDR3 и вставь в слот.' });
    else if (m.os === 'none') hints.push({ icon: '💿', text: 'Установи ОС: Linux (через терминал) или Windows Server (нужно ≥2,5 ГБ памяти).' });
    return hints;
  }

  const best = up[0];
  const ready = s.offers.filter((o) => contractAvailable(s, o));
  if (ready.length === 0) {
    const needed = s.offers[0];
    if (needed) {
      const services = needed.requirements.services?.filter((sv) => !s.machines.some((m) => m.services.includes(sv))) ?? [];
      if (services.length) {
        const svc = SERVICES[services[0]];
        hints.push({
          icon: '📦',
          text: `Для заказа «${needed.title}» нужен сервис ${svc?.name}. ${best.os === 'linux' ? `Введи в терминале: apt install ${svc?.linuxPackage}` : 'Включи его на рабочем столе Windows'}.`,
          tone: 'amber',
        });
      } else if (needed.requirements.firewall && firewallScore(s) < needed.requirements.firewall) {
        hints.push({ icon: '🛡️', text: `Нужен firewall ≥ ${needed.requirements.firewall}. Купи апгрейды защиты или подними навык сети.`, tone: 'amber' });
      } else if (needed.requirements.cables && s.cables < needed.requirements.cables) {
        hints.push({ icon: '🔌', text: 'Обожми патч-корд (кнопка в «Комнате» или «Машинах») — он нужен для сетевых заказов.' });
      } else {
        hints.push({ icon: '🧠', text: 'Железу не хватает ресурсов под заказы: добавь памяти, дисков или более мощный процессор.' });
      }
    } else {
      hints.push({ icon: '📋', text: 'Новые заказы приходят каждый день — заверши день, чтобы обновить список.' });
    }
  } else {
    hints.push({ icon: '🤝', text: `Есть готовый заказ «${ready[0].title}» — бери его во вкладке «Заказы», аванс платят сразу.`, tone: 'green' });
  }

  if (usableRam(best) < 3000) {
    hints.push({ icon: '🧠', text: 'Памяти мало: докупи планку, иначе сервисы и заказы не поместятся.', tone: 'amber' });
  }
  if (best.temp > 70) {
    hints.push({ icon: '🌡️', text: `${best.name} греется (${best.temp.toFixed(0)} °C): почисти пыль, замени термопасту или купи кулер.`, tone: 'amber' });
  }
  if (best.dust > 2) {
    hints.push({ icon: '🧹', text: 'В корпусе пыль — почисти, это бесплатно и снижает температуру.' });
  }
  if (s.stage === 1 && s.family < 45) {
    hints.push({ icon: '🏠', text: 'Семья на грани бунта: снизь шум (шумоизоляция, тихий кулер) или выключай сервер на ночь.', tone: 'amber' });
  }
  if (s.stage === 1 && s.stats.contractsDone >= 3 && s.rep >= 12 && s.money < 6000) {
    hints.push({ icon: '💰', text: `До переезда в гараж нужно 6 000 ₽ (сейчас ${Math.round(s.money).toLocaleString('ru-RU')} ₽). Не трать на лишнее железо.`, tone: 'cyan' });
  }
  if (s.stage === 2 && !s.upgrades.includes('rack')) {
    hints.push({ icon: '🗄️', text: 'Купи шкаф 12U — без него не поставить настоящий стоечный сервер.', tone: 'cyan' });
  }
  if (s.stage >= 2 && !s.upgrades.includes('backup')) {
    hints.push({ icon: '💾', text: 'Бэкапы спасают данные при смерти диска. Купи внешний диск в «Путь → Апгрейды».' });
  }
  if (s.stage >= 2 && s.rep >= 35 && s.stats.contractsDone >= 8 && s.money >= 38000 && !s.flags.dc_unlocked) {
    hints.push({ icon: '🏭', text: 'Ты готов к ЦОДу! Открой вкладку «Путь» и арендуй помещение (40 000 ₽).', tone: 'green' });
  }
  return hints.slice(0, 4);
}
