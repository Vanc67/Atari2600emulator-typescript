/* OS World 8.3: встроенные сайты, скорости и offline game packages. */
(function () {
  'use strict';
  if (!window.DATA || DATA.osWorldDataLoaded) return;
  DATA.OS_WORLD_SPEEDS = {
    slow: { id: 'slow', name: 'Медленно', mbps: 1, description: '1 Мбит/с · очередь без спешки' },
    normal: { id: 'normal', name: 'Обычно', mbps: 25, description: '25 Мбит/с' },
    fast: { id: 'fast', name: 'Быстро', mbps: 100, description: '100 Мбит/с' },
    fiber: { id: 'fiber', name: 'Fiber', mbps: 500, description: '500 Мбит/с' }
  };
  const sizes = { doom: 40, quake: 120, halflife: 350, cs16: 550, gta3: 1500, doom3: 2200, crysis: 6500, minecraft: 500, witcher3: 35000, cyberpunk: 70000 };
  DATA.OS_WORLD_GAMES = Object.values(DATA.PC_BENCHMARKS).filter(game => game.id !== 'stability').map(game => ({
    id: game.id, name: game.name, year: game.year, icon: game.icon, kind: 'game', sizeMb: sizes[game.id] || 500,
    description: game.description, benchmarkId: game.id, minRamMb: game.ramMb, target: game.target
  }));
  DATA.OS_WORLD_SITES = {
    pc: {
      'home.local': { title: 'Local Start', description: 'Полностью offline-портал этой машины.' },
      'games.local': { title: 'Game Runtime Store', description: '10 offline-пакетов: загрузка, установка и FPS.' },
      'downloads.local': { title: 'Download Manager', description: 'Скорость, пауза, продолжение и установка.' },
      'handbook.local': { title: 'PC Handbook', description: 'Документация по системе и железу.' },
      'docs.local': { title: 'PC Handbook', description: 'Руководство по Offline Browser, загрузкам и играм.' },
      'avito.local': { title: 'PC Авито', description: 'Существующий offline-маркет деталей ПК и ноутбуков.' }
    },
    server: {
      'home.local': { title: 'Server Intranet', description: 'Локальная точка управления сервером.' },
      'packages.local': { title: 'Package Repository', description: 'Реальные службы из каталога DATA.SOFTWARE.' },
      'downloads.local': { title: 'Package Downloads', description: 'Загрузка и установка server packages.' },
      'services.local': { title: 'Service Manager', description: 'Start, stop и restart установленных служб.' },
      'handbook.local': { title: 'Server Handbook', description: 'Документация Control Room.' },
      'repo.local': { title: 'Package Repository', description: 'Поиск и установка реальных серверных служб.' },
      'docs.local': { title: 'Server Handbook', description: 'Руководство по пакетам, службам и Shell.' },
      'status.local': { title: 'Server Status', description: 'Нагрузка, доход и установленные service IDs.' }
    }
  };
  DATA.OS_WORLD_POWER_PLANS = {
    eco: { id: 'eco', name: 'Эко', score: .88, watts: .84, temp: -2, description: '−12% scores, меньше ватты и шум' },
    balanced: { id: 'balanced', name: 'Баланс', score: 1, watts: 1, temp: 0, description: 'Заводской баланс' },
    performance: { id: 'performance', name: 'Производительность', score: 1.1, watts: 1.16, temp: 4, description: '+10% FPS, больше ватты и тепло' }
  };
  DATA.osWorldDataLoaded = true;
})();
