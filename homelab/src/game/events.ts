import { UPGRADES } from './data';
import { chance, pick } from './market';
import { totalNoise, totalWatts } from './rules';
import type { GameEvent, GameState, Stage } from './types';

interface EventTemplate {
  id: string;
  stage?: Stage;
  weight: number;
  test?: (s: GameState) => boolean;
  make: (s: GameState) => GameEvent;
}

function noiseComplainer(s: GameState): GameEvent {
  const noisy = s.machines.filter((m) => m.powered).map((m) => m.name)[0] ?? 'сервер';
  return {
    id: 'noise',
    icon: '📣',
    tone: 'bad',
    title: 'Мама стучит в дверь',
    text: `«Это что, самолёт? У меня голова болит!» ${noisy} гудит на всю квартиру (${Math.round(totalNoise(s))} дБ). Ночью спать невозможно.`,
    choices: [
      {
        label: 'Купить шумоизоляцию и извиниться',
        hint: '−2 800 ₽, семья +8',
        cost: 2800,
        effect: { text: 'Ты оклеил дверь и корпус. Стало тише, мама почти довольна.', money: -2800, family: 8, addUpgrade: 'soundproof', skill: { hardware: 2 } },
      },
      {
        label: 'Обещать «выключать на ночь»',
        hint: 'Семья +4, но сервер будет простаивать',
        effect: { text: 'Ты пообещал. Мама вздохнула. Утром ты забыл выключить — как всегда.', family: 4, rep: -1 },
      },
      {
        label: 'Сказать, что это «для работы»',
        hint: 'Семья −10, деньги +0',
        effect: { text: '«Для работы?» — переспросила мама. И ушла. Молча. Это хуже крика.', family: -10, rep: 1 },
      },
    ],
  };
}

function diskFailing(s: GameState): GameEvent {
  return {
    id: 'disk',
    icon: '💿',
    tone: 'bad',
    title: 'Диск с Авито начинает сыпаться',
    text: 'SMART показывает 148 переназначенных секторов. Ещё вчера было 12. Диск, купленный «за 300 рублей, работает тихо», готовится уйти в мир иной.',
    choices: [
      {
        label: 'Срочно сделать бэкап',
        hint: '−0 ₽, но нужен свободный диск',
        effect: { text: 'Ты скопировал всё важное на внешний диск. Скучный, но правильный ход.', skill: { sysadmin: 3 }, rep: 2 },
      },
      {
        label: 'Заменить диск (если есть запасной)',
        hint: 'Нужен свободный диск в инвентаре',
        cost: 0,
        effect: { text: 'Ты выдернул умирающий диск и поставил запасной. Пальцы в пыли, зато данные целы.', skill: { hardware: 2 }, breakDisk: true },
      },
      {
        label: 'Пока работает — пусть работает',
        hint: 'Риск аварии',
        effect: { text: '«Успею». Классические последние слова сисадмина.', money: 0, rep: -2, breakDisk: true },
      },
    ],
  };
}

function powerTrip(s: GameState): GameEvent {
  return {
    id: 'power',
    icon: '⚡',
    tone: 'warn',
    title: 'Выбило пробки',
    text: `Ты включил пылесос, и автомат в щитке щёлкнул. Суммарно твоя стойка жрёт ${Math.round(totalWatts(s))} Вт, а проводка в квартире помнит Брежнева.`,
    choices: [
      {
        label: 'Вызвать электрика и сделать отдельную линию',
        hint: '−1 500 ₽, надёжно',
        cost: 1500,
        effect: { text: 'Электрик поставил отдельный автомат и розетку. Теперь можно не бояться пылесоса.', money: -1500, skill: { hardware: 2 }, rep: 1 },
      },
      {
        label: 'Раскидать по разным розеткам',
        hint: 'Бесплатно, но некрасиво',
        effect: { text: 'Удлинители по всей комнате. Папа скажет, что это пожарная опасность. Он прав.', money: 0, family: -3 },
      },
      {
        label: 'Купить ИБП',
        hint: '−4 200 ₽',
        cost: 4200,
        effect: { text: 'APC 1500VA встал под стол и пискнул при тесте. Спокойствие — бесценно.', money: -4200, addUpgrade: 'ups', rep: 2 },
      },
    ],
  };
}

function internetDown(): GameEvent {
  return {
    id: 'isp',
    icon: '📡',
    tone: 'warn',
    title: 'Провайдер отключил интернет',
    text: '«Технические работы в вашем районе, срок — до двух суток». Твои клиенты уже пишут в чат: «А почему не открывается?».',
    choices: [
      {
        label: 'Раздать интернет с телефона',
        hint: '−300 ₽, канал медленный',
        cost: 300,
        effect: { text: 'Телефон в режиме модема, 30 ГБ трафика. Работает, но видео с Jellyfin только в 360p.', money: -300, rep: -1 },
      },
      {
        label: 'Позвонить и ругаться',
        hint: 'Шанс ускорить ремонт',
        effect: { text: 'Ты был вежлив, но твёрд. Мастер приехал на 6 часов раньше. Иногда это работает.', rep: 2, money: 0 },
      },
      {
        label: 'Устроить внеплановый день профилактики',
        hint: '+навыки',
        effect: { text: 'Пока интернета нет, ты почистил пыль, обновил систему и поставил мониторинг. Не так уж плохо.', skill: { sysadmin: 3, hardware: 2 } },
      },
    ],
  };
}

function freeStuff(): GameEvent {
  return {
    id: 'free',
    icon: '🎁',
    tone: 'good',
    title: 'На антресолях нашлась плата',
    text: 'В коробке от старого телевизора лежит видеокарта и планка памяти. Пахнет 2011-м и надеждой.',
    choices: [
      {
        label: 'Забрать всё в инвентарь',
        hint: '+ DDR3 4 ГБ',
        effect: { text: 'Планка DDR3 4 ГБ отправляется в твой ящик с железом. Видеокарту оставил — пригодится для Jellyfin-транскодинга.', givePart: 'ram_ddr3_4', skill: { hardware: 1 } },
      },
      {
        label: 'Продать на Авито',
        hint: '+1 200 ₽',
        effect: { text: 'Покупатель приехал через час. Деньги приятно пахнут.', money: 1200 },
      },
      {
        label: 'Оставить как есть — вдруг это музей',
        hint: 'Семья +2',
        effect: { text: 'Ты накрыл коробку пледом. Бабушка одобряет: «Вот это по-нашему».', family: 2 },
      },
    ],
  };
}

function dustEvent(): GameEvent {
  return {
    id: 'dust',
    icon: '🌫️',
    tone: 'warn',
    title: 'Пыль как в пустыне',
    text: 'Снял боковую крышку — внутри войлочный ковёр. Вентиляторы уже гудят на пределе, температура ползёт вверх.',
    choices: [
      {
        label: 'Разобрать и почистить всё',
        hint: '−0 ₽, труд',
        effect: { text: 'Два часа с кисточкой и пылесосом. Температура упала, настроение тоже, зато сервер счастлив.', dust: -2, skill: { hardware: 3 } },
      },
      {
        label: 'Пылесосом снаружи, не открывая',
        hint: 'Быстро',
        effect: { text: 'Ты подул в решётку. Пыль улетела внутрь. Отличная работа.', dust: 1, skill: { hardware: 1 } },
      },
      {
        label: 'Поставить фильтр на вдув',
        hint: '−400 ₽',
        cost: 400,
        effect: { text: 'Нейлоновый фильтр перед вентилятором. Теперь пыль оседает на нём, а не на радиаторе.', money: -400, dust: -1, skill: { hardware: 2 } },
      },
    ],
  };
}

function neighborComplaint(s: GameState): GameEvent {
  return {
    id: 'neighbor',
    icon: '🚪',
    tone: 'bad',
    title: 'Сосед снизу пришёл «поговорить»',
    text: `«У вас там что, производство? У меня люстра дрожит!» Стойка шумит на ${Math.round(totalNoise(s))} дБ и вибрирует через перекрытие.`,
    choices: [
      {
        label: 'Купить антивибрационные подложки',
        hint: '−900 ₽',
        cost: 900,
        effect: { text: 'Резиновые ножки и войлок под стойку. Люстра перестала дрожать, сосед — приходить.', money: -900, rep: 3, family: 3 },
      },
      {
        label: 'Подарить соседу роутер и настроить Wi-Fi',
        hint: '−1 200 ₽, репутация +',
        cost: 1200,
        effect: { text: 'Сосед теперь ловит твой Wi-Fi в туалете и считает тебя гением. Жалоб больше нет.', money: -1200, rep: 5 },
      },
      {
        label: 'Сделать вид, что не открыл',
        hint: 'Репутация −',
        effect: { text: 'Ты сидел тихо, как мышь. Сосед написал жалобу в управляющую компанию.', rep: -4, family: -2 },
      },
    ],
  };
}

function customerDiscount(): GameEvent {
  return {
    id: 'discount',
    icon: '💬',
    tone: 'info',
    title: 'Клиент просит скидку',
    text: '«Слушай, у нас бюджет подрезали. Может, 30% скидки? Мы же друзья!» Друзья, которые платят — редкий вид.',
    choices: [
      {
        label: 'Дать 15% и попросить отзыв',
        hint: '−600 ₽, репутация +',
        effect: { text: 'Клиент оставил отзыв на 5 звёзд и рассказал о тебе двум знакомым.', money: -600, rep: 5 },
      },
      {
        label: 'Отказать, но предложить бонус-сервис',
        hint: 'Репутация +2',
        effect: { text: '«Скидок нет, зато настрою бэкапы бесплатно». Клиент согласился. Взрослый подход.', rep: 3, skill: { sysadmin: 2 } },
      },
      {
        label: 'Согласиться на 30%',
        hint: 'Деньги −, репутация +1',
        effect: { text: 'Ты согласился. Внутри что-то неприятно скрипнуло, но клиент доволен.', money: -1200, rep: 1, family: -1 },
      },
    ],
  };
}

function powerOutage(s: GameState): GameEvent {
  const hasUps = s.upgrades.includes('ups');
  return {
    id: 'outage',
    icon: '🔌',
    tone: 'bad',
    title: 'Отключили свет на два часа',
    text: hasUps
      ? 'В подъезде щёлкнуло, свет погас. Твой ИБП пискнул и продолжил держать сервер. Ты спокойно допил чай.'
      : 'Свет погас без предупреждения. Сервер вырубился на середине записи, и SSD теперь думает о жизни.',
    choices: hasUps
      ? [
          { label: 'Написать в чат: «Всё под контролем»', hint: 'Репутация +5', effect: { text: 'Клиенты в восторге: «У него даже свет не влияет!» Ты — профессионал.', rep: 5, family: 2 } },
          { label: 'Проверить статус ИБП', hint: 'Навыки +', effect: { text: 'Батареи 84%, нагрузка 31%. Ты снял скриншот для будущих собеседований.', skill: { hardware: 2, sysadmin: 1 }, rep: 2 } },
        ]
      : [
          { label: 'Срочно проверить диски', hint: 'Потеря данных', effect: { text: 'Файловая система в порядке, но один диск начал сыпать ошибками. Ты включил сервер и перекрестился.', money: 0, rep: -2, breakDisk: true } },
          { label: 'Купить ИБП прямо сейчас', hint: '−4 200 ₽', cost: 4200, effect: { text: 'Ты поехал в магазин в темноте. ИБП куплен, сервер больше не боится блэкаутов.', money: -4200, addUpgrade: 'ups', rep: 2 } },
          { label: 'Подождать и ничего не делать', hint: 'Риск', effect: { text: 'Свет вернулся. Сервер загрузился, но в логах — грязный shutdown. Повезло.', rep: -1, skill: { sysadmin: 1 } } },
        ],
  };
}

function catIncident(): GameEvent {
  return {
    id: 'cat',
    icon: '🐈',
    tone: 'bad',
    title: 'Кот опрокинул чай на сервер',
    text: 'Раздался щелчок, потом запах жареного, потом очень громкая тишина. На полу — лужа и очень виноватый кот.',
    choices: [
      {
        label: 'Обесточить и сушить феном',
        hint: 'Ремонт, но шанс спасти',
        effect: { text: 'Ты выдернул кабель, разобрал и сушил два часа. Материнская плата выжила. Кот — под подозрением.', skill: { hardware: 4 }, crash: true, money: -300 },
      },
      {
        label: 'Паника и тряпка',
        hint: 'Хуже',
        effect: { text: 'Вода попала в слоты памяти. Сервер включается, но только чтобы показать синий экран.', crash: true, rep: -3 },
      },
      {
        label: 'Закрыть сервер в шкаф и купить замок',
        hint: '−900 ₽',
        cost: 900,
        effect: { text: 'Теперь кот смотрит на сервер через стекло и мстит взглядом.', money: -900, rep: 1, family: 2 },
      },
    ],
  };
}

function youtubeVideo(): GameEvent {
  return {
    id: 'youtube',
    icon: '🎥',
    tone: 'good',
    title: 'Про твой сервер сняли видео',
    text: 'Местный ютубер с 12 тысячами подписчиков сделал обзор «Сервер из Авито за 5000 рублей». В комментариях уже спорят, сколько ты потратил.',
    choices: [
      {
        label: 'Дать интервью и рассказать про RAID',
        hint: 'Репутация +10',
        effect: { text: 'Ты объяснил разницу между RAID 1 и RAID 0 так, что даже комментаторы поняли. Подписчики зовут тебя «тот мужик с сервером».', rep: 10, money: 0 },
      },
      {
        label: 'Смонтировать ответное видео',
        hint: 'Навыки, деньги',
        effect: { text: 'Твоё видео набрало больше просмотров. Пришли заказы на настройку домашних серверов.', money: 900, rep: 6, skill: { sysadmin: 2 } },
      },
      {
        label: 'Написать «это не всё, у меня ещё стойка есть»',
        hint: 'Скромно',
        effect: { text: 'Комментарий с фото стойки собрал 400 лайков. Ты стал легендой локального сообщества.', rep: 4, family: 1 },
      },
    ],
  };
}

function ddosEvent(): GameEvent {
  return {
    id: 'ddos',
    icon: '💥',
    tone: 'bad',
    title: 'DDoS-атака',
    text: 'Входящий трафик подскочил с 2 Мбит/с до 8 Гбит/с. Клиенты писать не могут — их сайт лежит. В логах сотни тысяч пакетов с одного диапазона.',
    choices: [
      {
        label: 'Принять бой: фильтровать трафик вручную',
        hint: 'Мини-игра, защита',
        effect: { text: 'Ты сел за фильтрацию пакетов.', ddos: true },
      },
      {
        label: 'Позвонить провайдеру и попросить mitigation',
        hint: '−3 000 ₽, безопасно',
        cost: 3000,
        effect: { text: 'Провайдер включил защиту за деньги. Атака ушла в никуда, но клиенты заметили простой.', money: -3000, rep: -1 },
      },
      {
        label: 'Просто выключить сервер на время',
        hint: 'Репутация −8',
        effect: { text: 'Ты выдернул кабель. Атака прекратилась, а клиент ушёл к конкурентам.', rep: -8, money: -1000 },
      },
    ],
  };
}

function heatWave(): GameEvent {
  return {
    id: 'heat',
    icon: '🌡️',
    tone: 'bad',
    title: 'Июльская жара',
    text: 'За окном +34, кондиционера нет, а в комнате сервер и монитор работают как обогреватель. Температура внутри корпуса ползёт к критической.',
    choices: [
      {
        label: 'Купить мобильный кондиционер',
        hint: '−13 000 ₽',
        cost: 13000,
        effect: { text: 'Кондиционер гудит, но теперь в комнате +22. Сервер дышит, ты тоже.', money: -13000, addUpgrade: 'ac', rep: 3, temp: -10 },
      },
      {
        label: 'Открыть окно и все корпуса',
        hint: 'Бесплатно, риск пыли',
        effect: { text: 'Стало чуть прохладнее, но пыль и тополиный пух летят прямо в вентиляторы.', dust: 2, temp: -4 },
      },
      {
        label: 'Снизить нагрузку: выключить Jellyfin',
        hint: 'Температура −, репутация −',
        effect: { text: 'Ты остановил транскодинг. Фильмы в 360p, зато сервер жив.', temp: -6, rep: -1 },
      },
    ],
  };
}

function electricianOffer(): GameEvent {
  return {
    id: 'bypass',
    icon: '🧑‍🔧',
    tone: 'warn',
    title: 'Электрик предлагает «оптимизировать» счётчик',
    text: '«Слушай, у тебя там стойка круглосуточно. Могу подключить мимо счётчика — сэкономишь тысяч пять в месяц. Никто не заметит».',
    choices: [
      {
        label: 'Отказаться',
        hint: 'Спокойно',
        effect: { text: 'Ты вежливо отказался. Электрик пожал плечами. Спать спокойнее.', rep: 2 },
      },
      {
        label: 'Согласиться',
        hint: '+3 000 ₽, потом может аукнуться',
        effect: { text: 'Счётчик «задумался». Деньги в кармане, но каждый звонок в дверь теперь немного пугает.', money: 3000, rep: -4, family: -2 },
      },
      {
        label: 'Купить у него б/у ИБП вместо этого',
        hint: '−2 000 ₽',
        cost: 2000,
        effect: { text: 'ИБП с наклейкой «Сбербанк, не отключать». Работает!', money: -2000, addUpgrade: 'ups', rep: 1 },
      },
    ],
  };
}

function unpaidClient(): GameEvent {
  return {
    id: 'unpaid',
    icon: '📉',
    tone: 'bad',
    title: 'Клиент «забыл» заплатить',
    text: 'Деньги за неделю не пришли. В ответ на напоминание клиент пишет: «Ну мы же на доверии».',
    choices: [
      {
        label: 'Вежливо напомнить и приложить акт',
        hint: 'Шанс вернуть деньги',
        effect: { text: 'Ты отправил акт и скриншот тарифа. Клиент нехотя перевёл сумму.', money: 600, rep: 2 },
      },
      {
        label: 'Пригрозить отключением',
        hint: 'Деньги или репутация',
        effect: { text: 'Через час деньги пришли. Клиент больше не здоровается, зато платит.', money: 900, rep: -2 },
      },
      {
        label: 'Простить и промолчать',
        hint: '−',
        effect: { text: 'Ты промолчал. Клиент решил, что так и надо. Дальше будет только хуже.', money: 0, rep: -2, family: -1 },
      },
    ],
  };
}

function garageNeighbors(): GameEvent {
  return {
    id: 'garage',
    icon: '🚗',
    tone: 'warn',
    title: 'Соседи по гаражу недовольны',
    text: 'Мужики из соседних боксов собрались у твоей двери. «Ты что, тут майнишь? Счётчик крутится, аж свистит».',
    choices: [
      {
        label: 'Показать графики и объяснить',
        hint: 'Репутация +5',
        effect: { text: 'Ты показал Zabbix. Мужики ушли, унося слово «аптайм» в массы.', rep: 5, skill: { sysadmin: 1 } },
      },
      {
        label: 'Подарить по бутылке лимонада',
        hint: '−300 ₽',
        cost: 300,
        effect: { text: 'Конфликт закрыт. Сосед Пётр теперь здоровается и просит «настроить телевизор».', money: -300, rep: 2, family: 2 },
      },
      {
        label: 'Сделать шумоизоляцию по-настоящему',
        hint: '−2 800 ₽',
        cost: 2800,
        effect: { text: 'Стены обшиты, дверь уплотнена. Гул стал тихим шелестом.', money: -2800, addUpgrade: 'soundproof', rep: 4 },
      },
    ],
  };
}

function acFail(): GameEvent {
  return {
    id: 'acfail',
    icon: '🥵',
    tone: 'bad',
    title: 'Кондиционер в гермозоне сдох',
    text: 'Температура в холодном коридоре растёт на градус каждые четыре минуты. Один сервер уже ушёл в троттлинг, второй держится.',
    choices: [
      {
        label: 'Открыть двери и гонять воздух вентиляторами',
        hint: 'Бесплатно',
        effect: { text: 'Ты снял с себя футболку и махал ею у решётки. Помогло немного. Температура стабилизировалась.', temp: -5, skill: { hardware: 2 }, rep: 1 },
      },
      {
        label: 'Вызвать сервис по кондиционерам',
        hint: '−12 000 ₽',
        cost: 12000,
        effect: { text: 'Мастер приехал с фреоном и спас положение. Восемь часов простоя, но железо живое.', money: -12000, rep: 3 },
      },
      {
        label: 'Выключить часть стоек',
        hint: 'Простой клиентов',
        effect: { text: 'Ты остановил второстепенные сервисы. Клиенты заметили и написали в поддержку.', rep: -4, downtime: true },
      },
    ],
  };
}

function employeeCoffee(): GameEvent {
  return {
    id: 'coffee',
    icon: '☕',
    tone: 'bad',
    title: 'Эникей залил кофе в стойку',
    text: 'Нанятый сотрудник поставил стакан с кофе на верхнюю полку шкафа. Зачем — он сам не смог объяснить. Итог: сгоревший коммутатор и запах карамельного латте.',
    choices: [
      {
        label: 'Уволить и починить сам',
        hint: '−1 500 ₽',
        cost: 1500,
        effect: { text: 'Ты починил коммутатор и повесил табличку «Напитки в гермозону — только в каске».', money: -1500, skill: { hardware: 3 }, rep: 2 },
      },
      {
        label: 'Провести инструктаж',
        hint: 'Деньги −, опыт +',
        effect: { text: 'Инструктаж на 40 минут, чай теперь только в кухонной зоне. Сотрудник запомнил.', skill: { sysadmin: 4 }, rep: 3 },
      },
      {
        label: 'Сделать вид, что ничего не было',
        hint: 'Риск',
        effect: { text: 'Стойка пахнет латте и немного дымит. Через неделю это выйдет боком.', rep: -3, money: -500 },
      },
    ],
  };
}

function bgpNeighbor(): GameEvent {
  return {
    id: 'bgp',
    icon: '🛰️',
    tone: 'good',
    title: 'Соседний оператор предлагает пиринг',
    text: '«Слушай, у нас свободный волсокно до твоего здания. Давай пиринг — у тебя трафик, у нас маршруты». Пахнет настоящим интернетом.',
    choices: [
      {
        label: 'Согласовать пиринг и настроить BGP',
        hint: '−8 000 ₽, репутация +',
        cost: 8000,
        effect: { text: 'Ты поднял BGP-сессию с первого раза. В логах `BGP state = Established`. Это красивее заката.', money: -8000, rep: 12, skill: { network: 6 }, addUpgrade: 'switch10g' },
      },
      {
        label: 'Отложить до стабилизации',
        hint: 'Спокойно',
        effect: { text: 'Ты записал номер на салфетке. Салфетка потерялась, но решение взрослое.', rep: 1 },
      },
      {
        label: 'Попросить транзит бесплатно «по дружбе»',
        hint: 'Репутация −',
        effect: { text: 'Оператор смеялся минуты три. Потом сказал: «Ну ты наглый. Уважаю».', rep: -1, money: 0 },
      },
    ],
  };
}

function clientGov(): GameEvent {
  return {
    id: 'gov',
    icon: '🏛️',
    tone: 'good',
    title: 'Заказ от «серьёзной организации»',
    text: 'Письмо на бланке: «Требуется размещение сервиса с аптаймом не ниже 99,9%. Оплата по счёту, сроки жёсткие».',
    choices: [
      {
        label: 'Взяться и показать графики',
        hint: '+2 500 ₽, репутация +',
        effect: { text: 'Ты подготовил SLA, графики и документацию. Заказчик остался доволен. Так начинается настоящий бизнес.', money: 2500, rep: 8, skill: { sysadmin: 3 } },
      },
      {
        label: 'Отказаться: пока не готов',
        hint: 'Честно',
        effect: { text: 'Ты отказался и попросил вернуться через полгода. Впервые в жизни сказал «нет» деньгам.', rep: 3, skill: { sysadmin: 1 } },
      },
      {
        label: 'Взять, но сделать «как получится»',
        hint: 'Деньги, риск',
        effect: { text: 'Деньги пришли сразу, а через неделю — грозное письмо про недоступность сервиса.', money: 4000, rep: -6, family: -2 },
      },
    ],
  };
}

function cableOrder(): GameEvent {
  return {
    id: 'cables',
    icon: '🔌',
    tone: 'info',
    title: 'Заказ: «нужно 4 патч-корда»',
    text: 'Клиент привёз коробку коннекторов и моток кабеля. «Сможешь обжать? Только чтобы по цветам, как положено». Твои навыки обжима под вопросом.',
    choices: [
      {
        label: 'Сесть и обжать вручную',
        hint: 'Мини-игра обжима',
        effect: { text: 'Ты достал кримпер.', cableGame: true },
      },
      {
        label: 'Купить готовые в магазине и выдать за свои',
        hint: '−1 200 ₽',
        cost: 1200,
        effect: { text: 'Клиент не заметил. Совесть немного скрипит, но кабели рабочие.', money: -1200, rep: 1 },
      },
    ],
  };
}

function hackathon(): GameEvent {
  return {
    id: 'hackathon',
    icon: '🏆',
    tone: 'good',
    title: 'Локальный хакатон по self-hosted',
    text: '«Собери домашний сервер за вечер и расскажи, что запустил». Приз — SSD на 1 ТБ и уважение местного чата.',
    choices: [
      {
        label: 'Участвовать',
        hint: 'Навыки +, шанс приза',
        effect: { text: 'Ты поднял Jellyfin, SMB и VPN за 3 часа 12 минут. Жюри аплодировало. Приз — SSD 1 ТБ.', givePart: 'ssd_1tb', skill: { sysadmin: 5, network: 3 }, rep: 6 },
      },
      {
        label: 'Судить вместо участия',
        hint: 'Репутация +',
        effect: { text: 'Ты оценивал чужие конфиги и раздавал советы. Один участник подписался на тебя.', rep: 4, skill: { sysadmin: 2 } },
      },
      {
        label: 'Проигнорировать, у тебя же аптайм',
        hint: 'Спокойно',
        effect: { text: 'Ты остался следить за графиками. Скучно — и правильно.', skill: { sysadmin: 1 } },
      },
    ],
  };
}

function firmwareBricked(): GameEvent {
  return {
    id: 'firmware',
    icon: '🧨',
    tone: 'bad',
    title: 'Прошивка роутера легла',
    text: 'После обновления роутер мигает всеми лампочками и не выдаёт DHCP. Вся сеть — включая домашних с их сериалами — умерла.',
    choices: [
      {
        label: 'Восстановить через TFTP',
        hint: 'Навыки +',
        effect: { text: 'Ты нашёл прошивку на сайте производителя и поднял роутер через TFTP. Домашние даже не заметили перерыва.', skill: { network: 5 }, rep: 2 },
      },
      {
        label: 'Сбросить и настроить заново',
        hint: 'Долго, но надёжно',
        effect: { text: 'Сброс, пароли, порты, VLAN. Три часа боли — и всё работает по-старому.', skill: { network: 3, sysadmin: 2 }, family: -2 },
      },
      {
        label: 'Купить новый роутер',
        hint: '−2 200 ₽',
        cost: 2200,
        effect: { text: 'MikroTik встал на полку. Ты почти забыл про старый.', money: -2200, addUpgrade: 'router', skill: { network: 2 } },
      },
    ],
  };
}

function thermalPaste(): GameEvent {
  return {
    id: 'paste',
    icon: '🧴',
    tone: 'warn',
    title: 'Термопаста высохла в камень',
    text: 'Температура под нагрузкой выросла на 12 градусов. Ты снял кулер: паста превратилась в серый сухарь.',
    choices: [
      {
        label: 'Заменить пасту',
        hint: '−350 ₽',
        cost: 350,
        effect: { text: 'Свежая MX-4, ровный слой, кулер притянут крест-накрест. Температура упала на 9 градусов.', money: -350, temp: -6, skill: { hardware: 4 } },
      },
      {
        label: 'Просто прикрутить обратно',
        hint: 'Температура останется',
        effect: { text: 'Ты собрал как было. Стало даже жарче. Но теперь ты это знаешь.', temp: 3, skill: { hardware: 1 } },
      },
      {
        label: 'Снизить нагрузку и не трогать',
        hint: 'Реплика',
        effect: { text: 'Ты убрал из автозапуска лишние сервисы. Не решение, но время купить.', temp: -2, rep: -1 },
      },
    ],
  };
}

const TEMPLATES: EventTemplate[] = [
  { id: 'noise', weight: 3, stage: 1, test: (s) => totalNoise(s) > 22, make: noiseComplainer },
  { id: 'disk', weight: 2, test: (s) => s.machines.some((m) => [...m.storage, ...m.ram].some((p) => p && p.flaw !== 'none')), make: diskFailing },
  { id: 'power', weight: 2, test: (s) => totalWatts(s) > 120, make: powerTrip },
  { id: 'isp', weight: 2, make: internetDown },
  { id: 'free', weight: 2, make: freeStuff },
  { id: 'dust', weight: 2, test: (s) => s.machines.length > 0, make: dustEvent },
  { id: 'neighbor', weight: 2, stage: 1, test: (s) => totalNoise(s) > 20, make: neighborComplaint },
  { id: 'discount', weight: 2, test: (s) => s.contracts.some((c) => c.active), make: customerDiscount },
  { id: 'outage', weight: 2, stage: 1, make: powerOutage },
  { id: 'cat', weight: 1, stage: 1, make: catIncident },
  { id: 'youtube', weight: 1, test: (s) => s.rep > 15, make: youtubeVideo },
  { id: 'ddos', weight: 3, test: (s) => s.stage >= 2 && s.contracts.some((c) => c.active), make: ddosEvent },
  { id: 'heat', weight: 2, test: (s) => s.machines.some((m) => m.temp > 62), make: heatWave },
  { id: 'bypass', weight: 1, stage: 1, test: (s) => totalWatts(s) > 180, make: electricianOffer },
  { id: 'unpaid', weight: 2, test: (s) => s.contracts.some((c) => c.active), make: unpaidClient },
  { id: 'garage', weight: 2, test: (s) => s.location === 'garage', make: garageNeighbors },
  { id: 'acfail', weight: 3, test: (s) => s.location === 'dc', make: acFail },
  { id: 'coffee', weight: 2, test: (s) => s.upgrades.includes('employees'), make: employeeCoffee },
  { id: 'bgp', weight: 2, test: (s) => s.location === 'dc', make: bgpNeighbor },
  { id: 'gov', weight: 1, test: (s) => s.rep > 30, make: clientGov },
  { id: 'cables', weight: 2, stage: 1, make: cableOrder },
  { id: 'hackathon', weight: 1, make: hackathon },
  { id: 'firmware', weight: 1, stage: 1, make: firmwareBricked },
  { id: 'paste', weight: 2, test: (s) => s.machines.some((m) => m.temp > 55), make: thermalPaste },
];

export function rollEvent(state: GameState): GameEvent | null {
  if (!chance(0.75)) return null;
  const pool = TEMPLATES.filter((t) => {
    if (t.stage && t.stage !== state.stage) return false;
    if (t.test && !t.test(state)) return false;
    const seen = state.flags[`evt_${t.id}`] ?? 0;
    if (seen >= 2) return false;
    return true;
  });
  if (!pool.length) return null;
  const weighted: EventTemplate[] = [];
  pool.forEach((t) => {
    for (let i = 0; i < t.weight; i += 1) weighted.push(t);
  });
  const chosen = pick(weighted);
  return chosen.make(state);
}

export function upgradeAvailable(id: string): string | undefined {
  return UPGRADES.find((u) => u.id === id)?.name;
}
