import type { ContractDef, Flaw, GameUpgrade, Part, PartKind, PartStats, ServiceDef, Stage } from './types';

export const TARIFF = 5.6; // ₽ за кВт·ч
export const OS_OVERHEAD = { none: 0, linux: 512, windows: 2048, android: 128 } as const;
export const WINDOWS_LICENSE = 1400;
export const HOURS_PER_DAY = 10;

export const STAGE_INFO: Record<Stage, { name: string; subtitle: string; budget: string; icon: string }> = {
  1: { name: 'Комнатный хакер', subtitle: 'Комната в квартире, старый ПК и очень терпеливая мама', budget: '5 000 ₽', icon: '🛋️' },
  2: { name: 'Гаражный сисадмин', subtitle: 'Гараж, первый настоящий сервер и звук турбин', budget: '50 000 ₽', icon: '🚗' },
  3: { name: 'Владелец ЦОД', subtitle: 'Аренда, гермозона, BGP и отражение DDoS', budget: 'Миллионы', icon: '🏭' },
};

export const LOCATION_INFO = {
  room: { name: 'Комната', icon: '🛏️', note: 'Шум раздражает домашних' },
  garage: { name: 'Гараж', icon: '🚗', note: 'Прохладно, но нужен шкаф' },
  dc: { name: 'ЦОД', icon: '🏭', note: 'Аренда 900 ₽/день' },
} as const;

export const FLAW_INFO: Record<Flaw, { name: string; desc: string; icon: string }> = {
  none: { name: 'Без дефектов', desc: 'Продавец клянётся, что всё работает', icon: '✅' },
  badsectors: { name: 'Bad-секторы', desc: 'Диск сыпется: SMART показывает переназначенные сектора', icon: '🩹' },
  coilwhine: { name: 'Писк дросселей', desc: 'Тонкий писк, который слышно ночью из другой комнаты', icon: '🎵' },
  bentpins: { name: 'Погнутые ножки', desc: 'Часть ножек сокета погнута — не все ядра работают стабильно', icon: '📌' },
  weakpsu: { name: 'Умирающий БП', desc: 'Блок питания просаживается под нагрузкой — привет, перезагрузка', icon: '⚡' },
  noisyfan: { name: 'Гудящий вентилятор', desc: 'Подшипник умер, вентилятор гудит как самолёт', icon: '🌀' },
  memoryerrors: { name: 'Ошибки памяти', desc: 'Планка сыпет ошибками: синие экраны и тихая порча данных', icon: '🧠' },
  slowdisk: { name: 'Медленный диск', desc: '5400 rpm и полный мусор — скорость как у дискеты', icon: '🐢' },
  deadbattery: { name: 'Мёртвая батарея', desc: 'Держит 4 минуты — при отключении света сервер умрёт', icon: '🔋' },
};

interface PartTemplate {
  defId: string;
  name: string;
  kind: PartKind;
  tier: number;
  machineKind?: 'desktop' | 'rack' | 'blade' | 'storage' | 'phone';
  stage: Stage;
  price: number;
  cond: [number, number];
  flaws: Flaw[];
  flawChance: number;
  stats: PartStats;
  flavor: string;
  short?: string;
}

export const PART_TEMPLATES: PartTemplate[] = [
  // ── Корпуса и платформы ─────────────────────────────────────────────
  {
    defId: 'base_compaq', machineKind: 'desktop', name: 'HP Compaq 6300 Pro', kind: 'base', tier: 1, stage: 1, price: 3600,
    cond: [55, 85], flaws: ['noisyfan', 'coilwhine'], flawChance: 0.35,
    stats: { cpuSlots: 1, ramSlots: 4, storageSlots: 2, memType: 'ddr3', baseNoise: 12, baseWatts: 35, cooling: 10, reliability: 70 },
    flavor: 'Офисный системник из 2013-го. Внутри слой пыли толщиной с палец и наклейка «Не выключать!».',
  },
  {
    defId: 'base_dell', machineKind: 'desktop', name: 'Dell OptiPlex 7010', kind: 'base', tier: 1, stage: 1, price: 4200,
    cond: [60, 90], flaws: ['noisyfan', 'weakpsu'], flawChance: 0.3,
    stats: { cpuSlots: 1, ramSlots: 4, storageSlots: 2, memType: 'ddr3', baseNoise: 14, baseWatts: 30, cooling: 11, reliability: 78 },
    flavor: 'Тихий офисный Dell. Хозяйка говорит: «Работал у бухгалтера, ничего не делал».',
  },
  {
    defId: 'base_phone', machineKind: 'phone', name: 'Samsung Galaxy Ace (2011)', kind: 'base', tier: 1, stage: 1, price: 800,
    cond: [30, 60], flaws: ['deadbattery', 'slowdisk'], flawChance: 0.5,
    stats: { cpuSlots: 0, ramSlots: 0, storageSlots: 0, memType: 'ddr3', baseNoise: 0, baseWatts: 2, cooling: 4, reliability: 40, baseRamMB: 512, baseCores: 1, baseStorageGB: 2 },
    flavor: 'Древний телефон. 512 МБ памяти, Android 2.3. Идеален для локальной нейросетки и веб-морды.',
  },
  {
    defId: 'base_dl380', machineKind: 'rack', name: 'HP ProLiant DL380 G7', kind: 'base', tier: 2, stage: 2, price: 17500,
    cond: [45, 80], flaws: ['noisyfan', 'weakpsu', 'badsectors'], flawChance: 0.45,
    stats: { cpuSlots: 2, ramSlots: 8, storageSlots: 8, memType: 'ecc', baseNoise: 48, baseWatts: 95, cooling: 18, reliability: 72, rackUnits: 2, hasILO: true },
    flavor: '2U, два Xeon, восемь слотов под ECC. Продаётся со склада конторы, которую «оптимизировали».',
  },
  {
    defId: 'base_dl360', machineKind: 'rack', name: 'Dell PowerEdge R710', kind: 'base', tier: 2, stage: 2, price: 14000,
    cond: [40, 75], flaws: ['noisyfan'], flawChance: 0.5,
    stats: { cpuSlots: 2, ramSlots: 8, storageSlots: 6, memType: 'ecc', baseNoise: 55, baseWatts: 110, cooling: 16, reliability: 64, rackUnits: 2, hasILO: true },
    flavor: 'Настоящая турбина. Соседи по гаражу уже интересуются, не реактор ли у тебя.',
  },
  {
    defId: 'base_blade', machineKind: 'blade', name: 'HP BladeSystem c7000', kind: 'base', tier: 3, stage: 3, price: 210000,
    cond: [55, 85], flaws: ['weakpsu'], flawChance: 0.3,
    stats: { cpuSlots: 4, ramSlots: 16, storageSlots: 8, memType: 'ecc', baseNoise: 62, baseWatts: 260, cooling: 26, reliability: 82, rackUnits: 10, hasILO: true },
    flavor: 'Полупустое блейд-шасси с аукциона ЦОД. 260 ватт только на охлаждение, но это уже серьёзная заявка.',
  },
  {
    defId: 'base_storage', machineKind: 'storage', name: 'NetApp FAS2240 (СХД)', kind: 'base', tier: 3, stage: 3, price: 165000,
    cond: [50, 80], flaws: ['badsectors'], flawChance: 0.35,
    stats: { cpuSlots: 1, ramSlots: 8, storageSlots: 16, memType: 'ecc', baseNoise: 50, baseWatts: 180, cooling: 20, reliability: 80, rackUnits: 4 },
    flavor: '24 отсека под SAS. Шумная, горячая, но как она умеет держать данные...',
  },

  // ── Процессоры ──────────────────────────────────────────────────────
  {
    defId: 'cpu_i3', name: 'Intel Core i3-3220', kind: 'cpu', tier: 1, stage: 1, price: 700,
    cond: [70, 95], flaws: ['bentpins'], flawChance: 0.2,
    stats: { cores: 2, ghz: 3.3, tdp: 55, speed: 2 },
    flavor: 'Два ядра, четыре потока. Его вытащили из офисного ПК аккуратно, почти без отвёртки.',
  },
  {
    defId: 'cpu_i5', name: 'Intel Core i5-3470', kind: 'cpu', tier: 1, stage: 1, price: 1600,
    cond: [65, 95], flaws: ['bentpins'], flawChance: 0.2,
    stats: { cores: 4, ghz: 3.2, tdp: 77, speed: 3 },
    flavor: 'Четыре ядра на LGA1155 — легенда домашних серверов. Греется, но зато тянет Jellyfin.',
  },
  {
    defId: 'cpu_xeon_5640', name: 'Xeon E5640', kind: 'cpu', tier: 2, stage: 2, price: 1100,
    cond: [60, 90], flaws: ['bentpins'], flawChance: 0.3,
    stats: { cores: 4, ghz: 2.66, tdp: 80, speed: 3 },
    flavor: 'Серверный Xeon с LGA1366. Горячий, шумный, но 4 ядра за 1100 рублей — это подарок.',
  },
  {
    defId: 'cpu_xeon_2670', name: 'Xeon E5-2670', kind: 'cpu', tier: 3, stage: 3, price: 3800,
    cond: [70, 95], flaws: ['bentpins'], flawChance: 0.2,
    stats: { cores: 8, ghz: 2.6, tdp: 115, speed: 5 },
    flavor: 'Восемь ядер и шестнадцать потоков. Китайский серверный магазин, отзывов — 4000.',
  },
  {
    defId: 'cpu_gold', name: 'Xeon Gold 6130', kind: 'cpu', tier: 3, stage: 3, price: 22000,
    cond: [80, 98], flaws: ['bentpins'], flawChance: 0.1,
    stats: { cores: 16, ghz: 2.1, tdp: 125, speed: 8 },
    flavor: 'Шестнадцать ядер. Продавец уверяет, что снял с рабочего кластера, а не с мусорки.',
  },

  // ── Память ──────────────────────────────────────────────────────────
  {
    defId: 'ram_ddr3_2', name: 'DDR3 2 ГБ', kind: 'ram', tier: 1, stage: 1, price: 250,
    cond: [60, 95], flaws: ['memoryerrors'], flawChance: 0.25,
    stats: { ramMB: 2048, memType: 'ddr3' },
    flavor: 'Планка из системника, который «работал, потом перестал».',
  },
  {
    defId: 'ram_ddr3_4', name: 'DDR3 4 ГБ', kind: 'ram', tier: 1, stage: 1, price: 550,
    cond: [65, 95], flaws: ['memoryerrors'], flawChance: 0.22,
    stats: { ramMB: 4096, memType: 'ddr3' },
    flavor: 'Kingston с радиаторами. Наклейка «гарантия 2014» уже пожелтела.',
  },
  {
    defId: 'ram_ddr3_8', name: 'DDR3 8 ГБ', kind: 'ram', tier: 2, stage: 1, price: 1200,
    cond: [70, 95], flaws: ['memoryerrors'], flawChance: 0.2,
    stats: { ramMB: 8192, memType: 'ddr3' },
    flavor: 'Восемь гигабайт — уже серьёзная заявка. Цена как за видеокарту в 2016-м.',
  },
  {
    defId: 'ram_ecc_8', name: 'ECC DDR3 8 ГБ', kind: 'ram', tier: 2, stage: 2, price: 900,
    cond: [65, 95], flaws: ['memoryerrors'], flawChance: 0.18,
    stats: { ramMB: 8192, memType: 'ecc' },
    flavor: 'Серверная регистровая память с eBay. Работает только в серверах — зато с коррекцией ошибок.',
  },
  {
    defId: 'ram_ecc_16', name: 'ECC DDR4 16 ГБ', kind: 'ram', tier: 3, stage: 3, price: 2600,
    cond: [75, 98], flaws: ['memoryerrors'], flawChance: 0.15,
    stats: { ramMB: 16384, memType: 'ecc' },
    flavor: 'Шестнадцать гигабайт ECC из разобранного кластера. Ммм, коррекция ошибок.',
  },

  // ── Диски ───────────────────────────────────────────────────────────
  {
    defId: 'hdd_320', name: 'HDD 320 ГБ', kind: 'storage', tier: 1, stage: 1, price: 300,
    cond: [30, 65], flaws: ['badsectors'], flawChance: 0.55,
    stats: { capacityGB: 320, speed: 1, reliability: 45 },
    flavor: '«Работает тихо, иногда щёлкает» — цитата продавца, которая должна была тебя насторожить.',
  },
  {
    defId: 'hdd_500', name: 'HDD 500 ГБ', kind: 'storage', tier: 1, stage: 1, price: 500,
    cond: [50, 85], flaws: ['badsectors', 'slowdisk'], flawChance: 0.35,
    stats: { capacityGB: 500, speed: 2, reliability: 60 },
    flavor: 'Western Digital Blue из 2012-го. Часы наработки неизвестны, но точно больше, чем у тебя.',
  },
  {
    defId: 'hdd_2tb', name: 'HDD 2 ТБ', kind: 'storage', tier: 2, stage: 1, price: 2200,
    cond: [65, 95], flaws: ['badsectors'], flawChance: 0.22,
    stats: { capacityGB: 2048, speed: 2, reliability: 72 },
    flavor: 'Два терабайта для фильмов. Заполнен на 90% «семейным архивом» — надеемся, что фильмами.',
  },
  {
    defId: 'ssd_120', name: 'SSD 120 ГБ', kind: 'storage', tier: 1, stage: 1, price: 750,
    cond: [70, 98], flaws: ['slowdisk'], flawChance: 0.1,
    stats: { capacityGB: 120, speed: 5, reliability: 85 },
    flavor: 'Система на SSD — первый шаг к счастью. Модель без названия, но SATA честный.',
  },
  {
    defId: 'ssd_1tb', name: 'SSD 1 ТБ', kind: 'storage', tier: 2, stage: 2, price: 4200,
    cond: [75, 98], flaws: ['slowdisk'], flawChance: 0.1,
    stats: { capacityGB: 1024, speed: 6, reliability: 88 },
    flavor: 'Терабайт NVMe в переходнике. Горячий, быстрый, обожает троттлить.',
  },
  {
    defId: 'sas_300', name: 'SAS 300 ГБ 10K', kind: 'storage', tier: 2, stage: 2, price: 600,
    cond: [45, 85], flaws: ['badsectors'], flawChance: 0.4,
    stats: { capacityGB: 300, speed: 4, reliability: 65 },
    flavor: 'Серверный диск, который «прошёл только 40 000 часов». Это почти пять лет работы.',
  },
  {
    defId: 'hdd_8tb', name: 'HDD 8 ТБ', kind: 'storage', tier: 3, stage: 3, price: 9500,
    cond: [70, 98], flaws: ['badsectors'], flawChance: 0.2,
    stats: { capacityGB: 8192, speed: 3, reliability: 78 },
    flavor: 'Восемь терабайт. Для бэкапов, торрентов и всего, что «потом разберу».',
  },

  // ── Охлаждение и сеть ──────────────────────────────────────────────
  {
    defId: 'cooler_tower', name: 'Башня DeepCool', kind: 'cooler', tier: 1, stage: 1, price: 900,
    cond: [70, 95], flaws: ['noisyfan'], flawChance: 0.2,
    stats: { cooling: 16, noise: 18, watts: 3 },
    flavor: 'Три тепловые трубки. На упаковке нарисован дракон, но работает честно.',
  },
  {
    defId: 'cooler_silent', name: 'Noctua NH-L9i', kind: 'cooler', tier: 2, stage: 2, price: 2800,
    cond: [85, 100], flaws: ['noisyfan'], flawChance: 0.05,
    stats: { cooling: 20, noise: 9, watts: 2 },
    flavor: 'Тихий культовый кулер. Домашние перестанут спрашивать, что гудит ночью.',
  },
  {
    defId: 'nic_gbit', name: 'Сетевая карта 1 Гбит', kind: 'nic', tier: 1, stage: 1, price: 400,
    cond: [70, 98], flaws: [], flawChance: 0,
    stats: { ports: 1, watts: 3 },
    flavor: 'Intel PRO/1000 из системного блока. Сеть — это когда сервер уже не остров.',
  },
  {
    defId: 'nic_dual', name: 'Сетевая карта 2×1 Гбит', kind: 'nic', tier: 2, stage: 2, price: 1200,
    cond: [75, 98], flaws: [], flawChance: 0,
    stats: { ports: 2, watts: 5 },
    flavor: 'Два порта — можно в LAN и в интернет, не разрываясь между семьёй и геймерами.',
  },
];

export const UPGRADES: GameUpgrade[] = [
  { id: 'router', name: 'Роутер MikroTik', icon: '📶', price: 2200, stage: 1, desc: 'Нормальный интернет и проброс портов. +1 заказ в день, ISP больше не роняет канал.' },
  { id: 'switch', name: 'Коммутатор Cisco 2960', icon: '🔀', price: 3200, stage: 2, desc: 'Восемь портов, VLAN и огонёк, который все замечают. Нужен для сетевых заказов.' },
  { id: 'rack', name: 'Шкаф 12U', icon: '🗄️', price: 6000, stage: 2, desc: 'Можно ставить настоящие серверы. Без шкафа DL380 просто лежит на полу и грустит.' },
  { id: 'ups', name: 'ИБП APC 1500VA', icon: '🔋', price: 4200, stage: 2, desc: 'Держит 15 минут при отключении света — сервер переживает короткие блэкауты.' },
  { id: 'ac', name: 'Мобильный кондиционер', icon: '❄️', price: 13000, stage: 2, desc: '−12 °C к температуре стойки. Летом без него — троттлинг и паника.' },
  { id: 'soundproof', name: 'Шумоизоляция', icon: '🔇', price: 2800, stage: 2, desc: '−15 дБ. Соседи и домашние перестают писать жалобы в чат дома.' },
  { id: 'monitoring', name: 'Мониторинг Zabbix', icon: '📈', price: 2600, stage: 2, desc: 'Графики, алерты и меньше внезапных смертей: риск аварий −50%.' },
  { id: 'backup', name: 'Бэкапы на внешний диск', icon: '💾', price: 4500, stage: 2, desc: 'При смерти диска данные выживают. Настоящий сисадмин делает бэкапы.' },
  { id: 'firewall', name: 'Аппаратный фаервол', icon: '🛡️', price: 9500, stage: 2, desc: '+20 к защите и упрощённая мини-игра с DDoS. Клиенты любят, когда их не ломают.' },
  { id: 'generator', name: 'Дизель-генератор', icon: '🛢️', price: 48000, stage: 3, desc: 'Переживает отключение города. Бензин 800 ₽/день, зато ЦОД жив.' },
  { id: 'switch10g', name: '10G-магистраль', icon: '🚀', price: 26000, stage: 3, desc: 'Оптика, SFP+ и транзитный трафик. Нужна для BGP-маршрутизации.' },
  { id: 'employees', name: 'Нанять эникеев', icon: '👷', price: 15000, stage: 3, desc: 'Два сотрудника чинят железо и снимают дежурства. Аварий меньше, ты — босс.' },
];

export const SERVICES: Record<string, ServiceDef> = {
  smb: {
    id: 'smb', name: 'SMB-папка', icon: '📁', ramMB: 256, cores: 0.5, storageGB: 100,
    desc: 'Общий доступ к файлам для семьи и мелких офисов.',
    linuxPackage: 'samba', linuxUnit: 'smbd',
  },
  jellyfin: {
    id: 'jellyfin', name: 'Jellyfin', icon: '🎬', ramMB: 1024, cores: 0.5, storageGB: 50,
    desc: 'Медиасервер: фильмы, сериалы, «Друзья» в 480p.',
    linuxPackage: 'jellyfin', linuxUnit: 'jellyfin',
  },
  webserver: {
    id: 'webserver', name: 'Nginx', icon: '🌐', ramMB: 256, cores: 0.5, storageGB: 10,
    desc: 'Веб-сервер: сайты, визитки и веб-морда нейросетки.',
    linuxPackage: 'nginx', linuxUnit: 'nginx',
  },
  minecraft: {
    id: 'minecraft', name: 'Minecraft', icon: '⛏️', ramMB: 2048, cores: 1, storageGB: 40,
    desc: 'Сервер Minecraft. Геймеры платят вовремя, если сервер не лагает.',
    linuxPackage: 'minecraft-server', linuxUnit: 'minecraft',
  },
  vpn: {
    id: 'vpn', name: 'WireGuard', icon: '🔐', ramMB: 512, cores: 0.5, storageGB: 5,
    desc: 'VPN для фрилансеров и параноиков.',
    linuxPackage: 'wireguard', linuxUnit: 'wg-quick',
  },
  mail: {
    id: 'mail', name: 'Postfix+Roundcube', icon: '📧', ramMB: 1024, cores: 0.5, storageGB: 80,
    desc: 'Почтовый сервер. Спам, спам и ещё немного спама.',
    linuxPackage: 'postfix', linuxUnit: 'postfix',
  },
  torrent: {
    id: 'torrent', name: 'qBittorrent', icon: '🧲', ramMB: 1024, cores: 0.5, storageGB: 500,
    desc: 'Торрент-качалка на сервере. Только для легальных ISO, разумеется.',
    linuxPackage: 'qbittorrent-nox', linuxUnit: 'qbittorrent-nox',
  },
  backup: {
    id: 'backup', name: 'Restic', icon: '💾', ramMB: 512, cores: 0.5, storageGB: 500,
    desc: 'Ночные бэкапы чужих ноутбуков и твоих собственных ошибок.',
    linuxPackage: 'restic', linuxUnit: 'restic',
  },
  tinyllm: {
    id: 'tinyllm', name: 'Tiny-нейросеть', icon: '🤖', ramMB: 128, cores: 0.5, storageGB: 1,
    desc: 'Крошечная LLM на старом смартфоне. Отвечает «как настоящий ИИ», только медленно.',
    phoneOnly: true, requiresCables: true,
  },
  cctv: {
    id: 'cctv', name: 'Видеонаблюдение', icon: '📹', ramMB: 1024, cores: 0.5, storageGB: 2000,
    desc: 'Архив с камер. Кто-то всегда «просто мимо проходил».',
    linuxPackage: 'motion', linuxUnit: 'motion',
  },
  gameserver: {
    id: 'gameserver', name: 'CS 1.6', icon: '🎮', ramMB: 1536, cores: 1, storageGB: 20,
    desc: 'Легендарный сервер 1.6. Геймеры будут играть в него вечно.',
    linuxPackage: 'hlds', linuxUnit: 'hlds',
  },
  dns: {
    id: 'dns', name: 'Unbound DNS', icon: '🧭', ramMB: 256, cores: 0.5, storageGB: 2,
    desc: 'Локальный DNS-резолвер. Интернет становится быстрее на 3 миллисекунды.',
    linuxPackage: 'unbound', linuxUnit: 'unbound',
  },
};

export const INSTALLABLE = Object.values(SERVICES).filter((s) => s.linuxPackage);

export const CONTRACT_TEMPLATES: ContractDef[] = [
  {
    id: 'cable_job', title: 'Прокладка кабеля в офисе', client: 'ООО «Тюльпан»', icon: '🔌', stage: 1,
    desc: '«Нужно протянуть сеть в дальний кабинет и обжать концы». Только руки и кримпер.',
    requirements: { cables: 2 },
    load: { ramMB: 0, cores: 0, storageGB: 0 },
    setupPay: 700, dailyPay: 110, repReward: 3, days: 8, penalty: 100,
  },
  {
    id: 'wifi_setup', title: 'Настройка Wi-Fi дома', client: 'Семья Смирновых', icon: '📶', stage: 1,
    desc: '«Роутер из коробки, интернет не работает, дети плачут». Нужен нормальный роутер и понимание сетей.',
    requirements: { upgrades: ['router'], firewall: 16 },
    load: { ramMB: 0, cores: 0, storageGB: 0 },
    setupPay: 800, dailyPay: 130, repReward: 4, days: 9, penalty: 150,
  },
  {
    id: 'family_smb', title: 'Семейный файлообменник', client: 'Семья Копейкиных', icon: '📁', stage: 1,
    desc: '«Хотим фото с отпуска на все устройства, а не на флешке у папы». Нужна SMB-папка.',
    requirements: { services: ['smb'], minRamMB: 2048, minStorageGB: 100 },
    load: { ramMB: 256, cores: 0.5, storageGB: 100 },
    setupPay: 1100, dailyPay: 190, repReward: 4, days: 14, penalty: 200,
  },
  {
    id: 'cinema', title: 'Домашний кинотеатр', client: 'Дядя Гена', icon: '🎬', stage: 1,
    desc: '«Хочу смотреть фильмы на телевизоре, а не с ноутбука». Нужен Jellyfin.',
    requirements: { services: ['jellyfin'], minRamMB: 2048, minCores: 2 },
    load: { ramMB: 1024, cores: 1, storageGB: 50 },
    setupPay: 1400, dailyPay: 220, repReward: 5, days: 12, penalty: 250,
  },
  {
    id: 'portfolio', title: 'Сайт-визитка фотографа', client: 'Алина, фотограф', icon: '🌐', stage: 1,
    desc: 'Портфолио на своём домене. Нужен веб-сервер и открытый порт.',
    requirements: { services: ['webserver'], minRamMB: 1024 },
    load: { ramMB: 256, cores: 0.5, storageGB: 10 },
    setupPay: 700, dailyPay: 130, repReward: 3, days: 10, penalty: 100,
  },
  {
    id: 'tiny_ai', title: 'Нейросеть на смартфоне', client: 'Стартап «Гараж.AI»', icon: '🤖', stage: 1,
    desc: 'Локальная нейросетка с веб-интерфейсом. Нужен старый смартфон с сервисом tinyllm.',
    requirements: { services: ['tinyllm'], kinds: ['phone'], minRamMB: 300 },
    load: { ramMB: 128, cores: 0.5, storageGB: 1 },
    setupPay: 900, dailyPay: 170, repReward: 6, days: 10, penalty: 150,
    trouble: 'Смартфон греется и разряжается — держи его на зарядке.',
  },
  {
    id: 'mine_1', title: 'Minecraft для друзей', client: 'Клан «Кубик»', icon: '⛏️', stage: 2,
    desc: '5 геймеров, ванильный сервер, «чтобы не лагало, как у Пети». Нужен Minecraft.',
    requirements: { services: ['minecraft'], minRamMB: 4096, minCores: 2, cables: 1 },
    load: { ramMB: 3072, cores: 2, storageGB: 40 },
    setupPay: 2600, dailyPay: 520, repReward: 8, days: 18, penalty: 700,
  },
  {
    id: 'office_files', title: 'Файловый сервер офиса', client: 'ООО «Ромашка»', icon: '🗂️', stage: 2,
    desc: 'Восемь менеджеров, 1 ТБ договоров, «чтобы не терялось». SMB + бэкап.',
    requirements: { services: ['smb', 'backup'], minRamMB: 4096, minStorageGB: 500 },
    load: { ramMB: 1024, cores: 1, storageGB: 700 },
    setupPay: 3000, dailyPay: 620, repReward: 9, days: 20, penalty: 800,
  },
  {
    id: 'cctv_shop', title: 'Камеры в магазине', client: 'Магазин «У дома»', icon: '📹', stage: 2,
    desc: 'Ночной архив с 4 камер. Нужен сервис видеонаблюдения и 2 ТБ дисков.',
    requirements: { services: ['cctv'], minStorageGB: 2000, minRamMB: 4096 },
    load: { ramMB: 1024, cores: 1, storageGB: 2000 },
    setupPay: 4000, dailyPay: 780, repReward: 10, days: 16, penalty: 900,
  },
  {
    id: 'vpn_free', title: 'VPN для фрилансера', client: 'Максим, дизайнер', icon: '🔐', stage: 2,
    desc: 'Работа из кафе, доступ к домашней сети. Нужен WireGuard.',
    requirements: { services: ['vpn'], minRamMB: 2048, cables: 1 },
    load: { ramMB: 512, cores: 0.5, storageGB: 5 },
    setupPay: 1600, dailyPay: 320, repReward: 5, days: 15, penalty: 300,
  },
  {
    id: 'mail_office', title: 'Почта для юрфирмы', client: '«Иванов и партнёры»', icon: '📧', stage: 2,
    desc: 'Свой домен, 20 ящиков, антиспам. Нужен Linux и почтовый сервис.',
    requirements: { services: ['mail'], os: 'linux', minRamMB: 4096, minCores: 2 },
    load: { ramMB: 1024, cores: 1, storageGB: 80 },
    setupPay: 4200, dailyPay: 850, repReward: 12, days: 18, penalty: 1200,
  },
  {
    id: 'torrent_mirror', title: 'Зеркало Linux-дистрибутивов', client: 'Локалка «Пингвин»', icon: '🧲', stage: 2,
    desc: 'Раздача ISO для местного сообщества. Торрент + 1 ТБ на дисках.',
    requirements: { services: ['torrent'], minStorageGB: 1024, minRamMB: 2048 },
    load: { ramMB: 1024, cores: 1, storageGB: 900 },
    setupPay: 1800, dailyPay: 380, repReward: 7, days: 20, penalty: 500,
  },
  {
    id: 'ddos_guard', title: 'Защита от DDoS для форума', client: 'Форум «Железо»', icon: '🛡️', stage: 2,
    desc: 'Форум постоянно роняют. Нужен сервер с firewall ≥ 45 и хороший аптайм.',
    requirements: { firewall: 45, minRamMB: 4096, minUptime: 3 },
    load: { ramMB: 1024, cores: 1, storageGB: 20 },
    setupPay: 5000, dailyPay: 1100, repReward: 15, days: 15, penalty: 1500,
    trouble: 'Раз в несколько дней будет прилетать настоящая атака.',
  },
  {
    id: 'web_hosting', title: 'Хостинг 20 сайтов', client: 'Веб-студия «Пиксель»', icon: '🌐', stage: 3,
    desc: 'Двадцать визиток на одном сервере. Нужен Nginx на rack-сервере и шкаф.',
    requirements: { services: ['webserver'], kinds: ['rack', 'blade'], minRamMB: 8192, minMachines: 1 },
    load: { ramMB: 4096, cores: 2, storageGB: 100 },
    setupPay: 12000, dailyPay: 2800, repReward: 15, days: 20, penalty: 3000,
  },
  {
    id: 'game_network', title: 'Сеть игровых серверов', client: 'Киберклуб «Респавн»', icon: '🎮', stage: 3,
    desc: 'Minecraft и CS для 50 человек. Нужен blade с 16 ГБ RAM и кабели.',
    requirements: { services: ['minecraft', 'gameserver'], kinds: ['blade', 'rack'], minRamMB: 16384, minCores: 8, cables: 2 },
    load: { ramMB: 12288, cores: 6, storageGB: 200 },
    setupPay: 20000, dailyPay: 5200, repReward: 20, days: 22, penalty: 5000,
  },
  {
    id: 'backup_enterprise', title: 'Корпоративные бэкапы', client: 'Завод «Прогресс»', icon: '💾', stage: 3,
    desc: 'Каждый день 8 ТБ архивов. Нужна СХД и сервис restic, плюс бэкапы у тебя самого.',
    requirements: { services: ['backup'], kinds: ['storage', 'rack', 'blade'], minStorageGB: 8000, minRamMB: 8192, needsBackup: true },
    load: { ramMB: 2048, cores: 2, storageGB: 8000 },
    setupPay: 25000, dailyPay: 6800, repReward: 22, days: 25, penalty: 7000,
  },
  {
    id: 'ddos_shield', title: 'Щит против DDoS', client: 'Магазин «Мегабайт»', icon: '⚡', stage: 3,
    desc: 'Боевая защита с фильтрацией трафика. Firewall ≥ 70, оптическая магистраль.',
    requirements: { firewall: 70, needsSwitch: true, minRamMB: 8192, minCores: 4 },
    load: { ramMB: 2048, cores: 2, storageGB: 50 },
    setupPay: 30000, dailyPay: 8000, repReward: 30, days: 20, penalty: 9000,
    trouble: 'Атаки будут регулярно — готовь мини-игру.',
  },
  {
    id: 'bgp', title: 'Транзит и BGP', client: 'Оператор «Светлая Сеть»', icon: '🛰️', stage: 3,
    desc: 'Анонс префиксов, пиринг, 10G-магистраль. Только для настоящих ЦОДов.',
    requirements: { firewall: 60, needsSwitch: true, kinds: ['blade', 'rack'], minMachines: 2, minUptime: 5 },
    load: { ramMB: 4096, cores: 4, storageGB: 100 },
    setupPay: 40000, dailyPay: 11000, repReward: 35, days: 20, penalty: 12000,
  },
  {
    id: 'colocation', title: 'Колокация для стартапа', client: 'Стартап «Дрон-Дрон»', icon: '🏭', stage: 3,
    desc: 'Два сервера в стойке, круглосуточный аптайм, охрана и холод.',
    requirements: { minMachines: 2, kinds: ['rack', 'blade'], minUptime: 4, minRamMB: 8192 },
    load: { ramMB: 4096, cores: 4, storageGB: 200 },
    setupPay: 22000, dailyPay: 6500, repReward: 25, days: 24, penalty: 5000,
  },
];

export const SELLERS = [
  { name: 'Сергей_1987', tag: 'отдаёт из гаража, торг у капота' },
  { name: 'АвитоДед', tag: 'продаёт «всё работает» уже 9 лет' },
  { name: 'Тимофей', tag: 'молодой, но странно много серверов' },
  { name: 'МастерБайт', tag: 'ремонт и разборка, чек не даёт' },
  { name: 'Аня', tag: 'продаёт железо бывшего мужа' },
  { name: 'СисадминВася', tag: 'уволен из ЦОДа, распродаёт склад' },
  { name: 'Гаражный_Лев', tag: 'всё проверено лично, почти' },
  { name: 'Петрович', tag: 'знает цену каждой планке DDR3' },
];

export const SELLER_LINES = {
  offer: [
    'Хм. Ну... я вообще рассчитывал на большее. Давай тогда за {price} и сам забираешь.',
    'Ты серьёзно? Ладно, вижу, что ты в теме. {price} — и разошлись.',
    'За {price} только если сегодня заберёшь. И без «а можно ещё дешевле».',
    'Мне тут уже писали за полную цену... но ты первый доехал. {price}.',
    'Не, за столько не отдам. Ладно, {price}, но пакет не дам.',
  ],
  refuse: [
    'Нет, это уже наглость. Цена окончательная.',
    'Я лучше выброшу, чем отдам за такие деньги.',
    'Ты думаешь, это барахолка? Это рынок, брат.',
    'Ещё один такой вопрос — и я заблокирую.',
  ],
  ask: [
    'Всё идеально! Проверял лично. Ну, диск иногда щёлкает, но это так, от пыли.',
    'Слушай, честно: есть один нюанс — {flaw}. Поэтому и цена такая.',
    'Проверь сам при встрече. Мне скрывать нечего... наверное.',
    'Работало, когда выключали. Больше сказать не могу.',
  ],
  pressure: [
    'А, ты разбираешься. Тогда слушай: {flaw}. Скину, если не будешь трепаться.',
    'Что? Нет! Всё новое! Хотя... ладно, пару секторов можно найти.',
    'Ты что, смотрел SMART? Ладно, минус {discount}.',
    'Да, есть грешок: {flaw}. Но за эти деньги — подарок.',
  ],
  accept: ['Договорились! Приятно иметь дело с понимающим человеком.'],
  reveal: [
    'Вот это ты глазастый. Да, {flaw}.',
    'Не придирайся, зато дешево.',
    'Слушай, ну ты и зануда. Не нравится — не бери.',
  ],
};

export const MARKET_FLAVOR = [
  'Срочно, переезд!',
  'Работает, но иногда...',
  'Отдам за символическую цену.',
  'Досталось от дедушки.',
  'Снято с рабочего сервера.',
  'Проверено, есть видео.',
  'Обмен на что-нибудь интересное.',
  'Только самовывоз, третий этаж без лифта.',
];

export function partLabel(p: Part): string {
  return p.name;
}

export const PART_KIND_INFO: Record<PartKind, { name: string; icon: string }> = {
  base: { name: 'Платформа', icon: '🖥️' },
  cpu: { name: 'Процессор', icon: '🧩' },
  ram: { name: 'Память', icon: '🧠' },
  storage: { name: 'Диск', icon: '💿' },
  cooler: { name: 'Охлаждение', icon: '🌀' },
  nic: { name: 'Сеть', icon: '🔌' },
};
