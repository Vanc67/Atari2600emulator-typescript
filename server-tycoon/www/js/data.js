(function () {
  'use strict';

  const PARTS = [
    // Корпуса и материнские платы
    { id: 'dc7800', type: 'chassis', name: 'HP Compaq dc7800', short: 'dc7800 · LGA775', icon: 'chassis', value: 4800, socket: 'LGA775', ramType: 'DDR2', ramSlots: 4, sataPorts: 4, basePower: 20, baseNoise: 31, coolingFactor: 0.92 },
    { id: 'hp6000', type: 'chassis', name: 'HP Compaq 6000 MT', short: '6000 MT · LGA775', icon: 'chassis', value: 7600, socket: 'LGA775', ramType: 'DDR3', ramSlots: 4, sataPorts: 4, basePower: 22, baseNoise: 33, coolingFactor: 1.0 },
    { id: 'hpgen8', type: 'chassis', name: 'HP ProLiant DL380 Gen8', short: 'DL380 Gen8 · 2U', icon: 'server', value: 39000, socket: 'LGA2011', ramType: 'DDR3', ramSlots: 4, sataPorts: 6, basePower: 48, baseNoise: 64, coolingFactor: 1.05 },
    { id: 'r720', type: 'chassis', name: 'Dell PowerEdge R720', short: 'R720 · 2U · 8 bay', icon: 'server', value: 46000, socket: 'LGA2011', ramType: 'DDR3', ramSlots: 4, sataPorts: 8, basePower: 52, baseNoise: 69, coolingFactor: 1.08 },
    { id: 'superx10', type: 'chassis', name: 'Supermicro X10 4U', short: 'X10DRi · 4U · 10 bay', icon: 'server', value: 71000, socket: 'LGA2011-3', ramType: 'DDR4', ramSlots: 4, sataPorts: 10, basePower: 58, baseNoise: 55, coolingFactor: 1.18 },

    // Процессоры
    { id: 'e8400', type: 'cpu', name: 'Intel Core 2 Duo E8400', short: '2 ядра · 3.0 GHz', icon: 'cpu', value: 2100, socket: 'LGA775', cores: 2, tdp: 65, performance: 1.0 },
    { id: 'q9650', type: 'cpu', name: 'Intel Core 2 Quad Q9650', short: '4 ядра · 3.0 GHz', icon: 'cpu', value: 4600, socket: 'LGA775', cores: 4, tdp: 95, performance: 1.75 },
    { id: 'e52620v2', type: 'cpu', name: 'Intel Xeon E5-2620 v2', short: '6 ядер · 12 потоков', icon: 'cpu', value: 8900, socket: 'LGA2011', cores: 6, tdp: 80, performance: 3.1 },
    { id: 'e52690v2', type: 'cpu', name: 'Intel Xeon E5-2690 v2', short: '10 ядер · 20 потоков', icon: 'cpu', value: 18500, socket: 'LGA2011', cores: 10, tdp: 130, performance: 5.5 },
    { id: 'e52678v3', type: 'cpu', name: 'Intel Xeon E5-2678 v3', short: '12 ядер · 24 потока', icon: 'cpu', value: 26000, socket: 'LGA2011-3', cores: 12, tdp: 120, performance: 6.7 },
    { id: 'e52697v3', type: 'cpu', name: 'Intel Xeon E5-2697 v3', short: '14 ядер · 28 потоков', icon: 'cpu', value: 37000, socket: 'LGA2011-3', cores: 14, tdp: 145, performance: 8.1 },

    // Оперативная память
    { id: 'ddr2-2', type: 'ram', name: 'DDR2 2 ГБ 800 MHz', short: '2 ГБ · PC2-6400', icon: 'ram', value: 450, ramType: 'DDR2', capacity: 2, watts: 5 },
    { id: 'ddr2-4', type: 'ram', name: 'DDR2 4 ГБ 800 MHz', short: '4 ГБ · ECC', icon: 'ram', value: 1200, ramType: 'DDR2', capacity: 4, watts: 6 },
    { id: 'ddr3-2', type: 'ram', name: 'DDR3 2 ГБ 1333 MHz', short: '2 ГБ · PC3-10600', icon: 'ram', value: 400, ramType: 'DDR3', capacity: 2, watts: 4 },
    { id: 'ddr3-4', type: 'ram', name: 'DDR3 4 ГБ 1600 MHz', short: '4 ГБ · PC3-12800', icon: 'ram', value: 950, ramType: 'DDR3', capacity: 4, watts: 5 },
    { id: 'ddr3-8', type: 'ram', name: 'DDR3 8 ГБ ECC REG', short: '8 ГБ · PC3L-12800R', icon: 'ram', value: 2300, ramType: 'DDR3', capacity: 8, watts: 6 },
    { id: 'ddr3-16', type: 'ram', name: 'DDR3 16 ГБ ECC REG', short: '16 ГБ · PC3-14900R', icon: 'ram', value: 5200, ramType: 'DDR3', capacity: 16, watts: 8 },
    { id: 'ddr4-8', type: 'ram', name: 'DDR4 8 ГБ 2400 MHz', short: '8 ГБ · PC4-19200', icon: 'ram', value: 2500, ramType: 'DDR4', capacity: 8, watts: 5 },
    { id: 'ddr4-16', type: 'ram', name: 'DDR4 16 ГБ ECC REG', short: '16 ГБ · PC4-21300R', icon: 'ram', value: 4900, ramType: 'DDR4', capacity: 16, watts: 7 },
    { id: 'ddr4-32', type: 'ram', name: 'DDR4 32 ГБ ECC REG', short: '32 ГБ · PC4-24000R', icon: 'ram', value: 9800, ramType: 'DDR4', capacity: 32, watts: 9 },
    { id: 'ddr4-64', type: 'ram', name: 'DDR4 64 ГБ LRDIMM', short: '64 ГБ · PC4-25600L', icon: 'ram', value: 19500, ramType: 'DDR4', capacity: 64, watts: 12 },

    // Накопители
    { id: 'hdd500', type: 'hdd', name: 'HDD 500 ГБ SATA', short: '500 ГБ · 7200 rpm', icon: 'hdd', value: 650, capacity: 500, watts: 8, speed: 1.0 },
    { id: 'hdd1000', type: 'hdd', name: 'HDD 1 ТБ SATA', short: '1 ТБ · 7200 rpm', icon: 'hdd', value: 1450, capacity: 1000, watts: 9, speed: 1.05 },
    { id: 'hdd4000', type: 'hdd', name: 'HDD 4 ТБ Enterprise', short: '4 ТБ · 7200 rpm', icon: 'hdd', value: 4800, capacity: 4000, watts: 11, speed: 1.15 },
    { id: 'ssd240', type: 'ssd', name: 'SSD 240 ГБ SATA', short: '240 ГБ · 520 MB/s', icon: 'ssd', value: 1750, capacity: 240, watts: 4, speed: 2.8 },
    { id: 'ssd960', type: 'ssd', name: 'SSD 960 ГБ Enterprise', short: '960 ГБ · 550 MB/s', icon: 'ssd', value: 5600, capacity: 960, watts: 6, speed: 3.1 },
    { id: 'ssd4000', type: 'ssd', name: 'SSD 4 ТБ Datacenter', short: '4 ТБ · 560 MB/s', icon: 'ssd', value: 19000, capacity: 4000, watts: 8, speed: 3.3 },

    // Охлаждение
    { id: 'cool75', type: 'cooler', name: 'Боксовый кулер 75W', short: 'TDP до 75 Вт', icon: 'cooler', value: 650, cooling: 75, noise: 3 },
    { id: 'cool130', type: 'cooler', name: 'Башенный кулер 130W', short: 'TDP до 130 Вт', icon: 'cooler', value: 1850, cooling: 130, noise: 2 },
    { id: 'cool200', type: 'cooler', name: 'Серверный кулер 200W', short: 'TDP до 200 Вт · громкий', icon: 'cooler', value: 4300, cooling: 200, noise: 12 },
    { id: 'cool320', type: 'cooler', name: 'СЖО 320W Silent', short: 'TDP до 320 Вт · тихая', icon: 'cooler', value: 12800, cooling: 320, noise: -5 },

    // Блоки питания
    { id: 'psu300', type: 'psu', name: 'БП 300W OEM', short: '300 Вт · 82% efficiency', icon: 'psu', value: 950, watts: 300, efficiency: 0.82, noise: 2 },
    { id: 'psu500', type: 'psu', name: 'БП 500W Bronze', short: '500 Вт · 85% efficiency', icon: 'psu', value: 2600, watts: 500, efficiency: 0.85, noise: 3 },
    { id: 'psu750', type: 'psu', name: 'БП 750W Platinum', short: '750 Вт · 92% efficiency', icon: 'psu', value: 5400, watts: 750, efficiency: 0.92, noise: 4 },
    { id: 'psu1100', type: 'psu', name: 'БП 1100W Redundant', short: '1100 Вт · 94% efficiency', icon: 'psu', value: 9800, watts: 1100, efficiency: 0.94, noise: 7 },

    // Расходники
    { id: 'usb16', type: 'usb', name: 'Флешка USB 16 ГБ', short: 'USB 3.0 · образ не записан', icon: 'usb', value: 650, capacity: 16 },
    { id: 'toolkit', type: 'tool', name: 'Набор сисадмина', short: 'Отвёртки · термопаста · тестер', icon: 'tool', value: 1800 }
  ];

  const SERVER_TEMPLATES = [
    { id: 'office7800', name: 'Старый офисный ПК', chassis: 'dc7800', cpu: 'e8400', ram: ['ddr2-2', 'ddr2-2'], disks: ['hdd500'], cooler: 'cool75', psu: 'psu300', basePrice: 10500, weight: 18 },
    { id: 'office6000', name: 'HP Compaq · Core 2 Duo', chassis: 'hp6000', cpu: 'e8400', ram: ['ddr3-4', 'ddr3-4'], disks: ['hdd500'], cooler: 'cool130', psu: 'psu300', basePrice: 16500, weight: 22 },
    { id: 'quad6000', name: 'Домашний сервер · Core 2 Quad', chassis: 'hp6000', cpu: 'q9650', ram: ['ddr3-4', 'ddr3-4'], disks: ['hdd1000'], cooler: 'cool130', psu: 'psu500', basePrice: 24500, weight: 15 },
    { id: 'gen8', name: 'HP ProLiant DL380 Gen8', chassis: 'hpgen8', cpu: 'e52620v2', ram: ['ddr3-8', 'ddr3-8', 'ddr3-8', 'ddr3-8'], disks: ['hdd1000', 'hdd1000'], cooler: 'cool200', psu: 'psu750', basePrice: 76500, weight: 15 },
    { id: 'r720', name: 'Dell PowerEdge R720', chassis: 'r720', cpu: 'e52690v2', ram: ['ddr3-16', 'ddr3-16', 'ddr3-16', 'ddr3-16'], disks: ['hdd4000', 'hdd4000', 'ssd960'], cooler: 'cool200', psu: 'psu1100', basePrice: 133000, weight: 10 },
    { id: 'monster', name: 'Монстр из дата-центра · 256 ГБ', chassis: 'superx10', cpu: 'e52697v3', ram: ['ddr4-64', 'ddr4-64', 'ddr4-64', 'ddr4-64'], disks: ['ssd4000', 'hdd4000', 'hdd4000'], cooler: 'cool320', psu: 'psu1100', basePrice: 259000, weight: 5 }
  ];

  const CONDITIONS = {
    working: { name: 'Рабочий', factor: 1, description: 'Включается, проходит POST. Продавец показал видео.' },
    noRam: { name: 'Без ОЗУ', factor: 0.78, description: 'Память продали отдельно. Остальное, по словам продавца, целое.' },
    noDisks: { name: 'Без дисков', factor: 0.84, description: 'Салазки на месте, накопителей нет. Под ваши HDD/SSD.' },
    burntPsu: { name: 'Спаленый БП', factor: 0.69, description: 'Пахнет гарью, блок питания под замену. Плата не проверена.' },
    brick: { name: 'Кирпич', factor: 0.38, description: 'Не включается. Для ремонта или на запчасти, возврат не предусмотрен.' }
  };

  const OS_LIST = [
    { id: 'debian', name: 'Debian 12', icon: 'os', price: 0, minRam: 1, minDisk: 16, baseRam: 0.5, baseCpu: 2, description: 'Свободная · стабильная', terminal: 'apt-get install server-tycoon' },
    { id: 'windows', name: 'Windows Server', icon: 'os', price: 6500, minRam: 4, minDisk: 64, baseRam: 2.2, baseCpu: 8, description: 'Лицензия · GUI · 64 ГБ диск', terminal: 'DISM /Apply-Image' },
    { id: 'truenas', name: 'TrueNAS SCALE', icon: 'os', price: 0, minRam: 8, minDisk: 32, baseRam: 3, baseCpu: 4, description: 'ZFS · хранилище · мин. 8 ГБ', terminal: 'zpool create tank mirror' },
    { id: 'proxmox', name: 'Proxmox VE', icon: 'os', price: 0, minRam: 4, minDisk: 32, baseRam: 1.5, baseCpu: 5, description: 'Виртуализация · LXC · KVM', terminal: 'pve-installer --ext4' }
  ];

  const SOFTWARE = [
    { id: 'jellyfin', name: 'Jellyfin', icon: 'jellyfin', ram: 2, cpu: 18, rate: 75, minRam: 2, os: ['windows', 'debian', 'proxmox'], description: 'Медиасервер · +75 ₽/ч' },
    { id: 'filebrowser', name: 'File Browser', icon: 'files', ram: 0.6, cpu: 5, rate: 30, minRam: 1, os: ['windows', 'debian', 'truenas', 'proxmox'], description: 'Файловое облако · +30 ₽/ч' },
    { id: 'boinc', name: 'BOINC', icon: 'boinc', ram: 1, cpu: 100, rate: 45, coreRate: 18, minRam: 1, os: ['windows', 'debian', 'proxmox'], description: 'Наука · ЦП 100% · доход за ядра' },
    { id: 'localai', name: 'Local AI', icon: 'ai', ram: 12, cpu: 70, rate: 410, minRam: 16, os: ['debian', 'proxmox'], description: 'LLM · 12 ГБ ОЗУ · +410 ₽/ч' }
  ];

  const ROOMS = [
    { id: 'room', name: 'Комната у мамы', subtitle: 'Розетка 2.2 кВт · 4 места', slots: 4, rent: 900, ambient: 24, powerLimit: 2200, noiseLimit: 70, price: 0 },
    { id: 'garage', name: 'Свой гараж', subtitle: 'Розетка 5 кВт · 8 мест', slots: 8, rent: 2400, ambient: 18, powerLimit: 5000, noiseLimit: 78, price: 65000 },
    { id: 'minidc', name: 'Мини-ЦОД', subtitle: 'Линия 12 кВт · 12 мест', slots: 12, rent: 6800, ambient: 16, powerLimit: 12000, noiseLimit: 85, price: 180000 }
  ];

  const SELLERS = ['Алексей · 4.9★', 'Сергей · 4.7★', 'Владимир · 5.0★', 'IT-Разбор · 4.8★', 'Дмитрий · 4.5★', 'Старый серверный · 4.9★', 'Марина · 4.6★', 'Роман · 4.8★'];
  const PART_LOT_SUFFIX = ['с рабочего ПК', 'снято при апгрейде', 'из серверной', 'проверено', 'лежало на полке', 'осталось после ремонта'];
  const INSTALL_LINES = [
    'Загрузка ядра... OK', 'Проверка SHA256... OK', 'Разметка системного диска...', 'Копирование пакетов...', 'Настройка сетевого интерфейса...', 'Обновление initramfs...', 'Установка загрузчика...', 'Финальная проверка конфигурации...'
  ];
  const INSTALL_ERRORS = [
    'ERR_PACKAGE_TIMEOUT: зеркало не отвечает', 'ERR_IO_RETRY: диск пропустил сектор', 'ERR_DHCP_LEASE: повторная настройка сети', 'WARN_INITRAMFS: модуль загружен не с первого раза'
  ];

  window.DATA = {
    PARTS, SERVER_TEMPLATES, CONDITIONS, OS_LIST, SOFTWARE, ROOMS, SELLERS,
    PART_LOT_SUFFIX, INSTALL_LINES, INSTALL_ERRORS,
    part(id) { return PARTS.find(p => p.id === id); },
    template(id) { return SERVER_TEMPLATES.find(t => t.id === id); },
    os(id) { return OS_LIST.find(o => o.id === id); },
    software(id) { return SOFTWARE.find(s => s.id === id); },
    room(id) { return ROOMS.find(r => r.id === id); }
  };
})();
