/* Real Hardware & OS 8.4: дополнительные ОС, игры, эмуляторы и готовые mobile-ПК. Только добавления. */
(function () {
  'use strict';
  if (!window.DATA || DATA.pcRealismDataLoaded) return;
  const os = (id, name, year, style, minRamMb, minStorageMb, price, recommendedCpu, bootSeconds, installSeconds, extra = {}) => Object.assign({
    id, name, year, style, minRamMb, minStorageMb, price, recommendedCpu, recommendedRamMb: minRamMb,
    bootSeconds, installSeconds, family: style === 'dos' ? 'dos' : style === 'linux' ? 'linux' : 'windows'
  }, extra);
  const profiles = {
    'pc-dos33': [5, 2, 900, { family:'dos', browserLevel:1, api:'dos' }],
    'pc-dos622': [8, 3, 900, { family:'dos', browserLevel:1, api:'dos' }],
    'pc-win95': [30, 12, 900, { family:'windows', browserLevel:4, api:'win32' }],
    'pc-win98': [55, 18, 1200, { family:'windows', browserLevel:5, api:'win32' }],
    'pc-win2000': [90, 25, 1500, { family:'windows', browserLevel:6, api:'win32' }],
    'pc-winxp': [140, 35, 1800, { family:'windows', browserLevel:8, api:'win32' }],
    'pc-vista': [420, 65, 2700, { family:'windows', browserLevel:9, api:'dx10' }],
    'pc-win7': [480, 55, 2400, { family:'windows', browserLevel:11, api:'dx11' }],
    'pc-win8': [620, 45, 2100, { family:'windows', browserLevel:11, api:'dx11' }],
    'pc-win10': [1200, 75, 3000, { family:'windows', browserLevel:109, api:'dx12' }],
    'pc-win11': [3000, 95, 3600, { family:'windows', browserLevel:132, api:'dx12' }],
    'pc-ubuntu1004': [320, 35, 1500, { family:'linux', browserLevel:28, api:'linux' }],
    'pc-ubuntu2404': [1700, 70, 2700, { family:'linux', browserLevel:128, api:'linux' }]
  };
  DATA.PC_OS.forEach(item => {
    const profile = profiles[item.id] || [Math.max(20, item.minRamMb), 45, 1800, {}];
    Object.assign(item, { recommendedCpu: profile[0], recommendedRamMb: item.minRamMb, bootSeconds: profile[1], installSeconds: profile[2] }, profile[3]);
  });
  DATA.PC_REALISM_OS = [
    os('pc-freedos13','FreeDOS 1.3',2022,'dos',8,128,0,12,3,900,{ browserLevel:1,api:'dos' }),
    os('pc-msdos71','MS-DOS 7.1 + Windows 3.11',1995,'dos',8,250,250,20,5,900,{ browserLevel:2,api:'dos' }),
    os('pc-nt4','Windows NT 4.0 Workstation',1996,'classic',32,512,700,55,25,1500,{ browserLevel:4,api:'win32' }),
    os('pc-winme','Windows Millennium Edition',2000,'classic',64,1200,800,90,28,1500,{ browserLevel:6,api:'win32' }),
    os('pc-xp64','Windows XP Professional x64',2005,'xp',512,4096,1500,240,42,1800,{ browserLevel:8,api:'win32' }),
    os('pc-vista64','Windows Vista Ultimate x64',2007,'aero',2048,25600,2200,520,72,2700,{ browserLevel:9,api:'dx10' }),
    os('pc-win7starter','Windows 7 Starter',2009,'aero',1024,16384,1400,360,58,2400,{ browserLevel:11,api:'dx11' }),
    os('pc-win7x64','Windows 7 Ultimate x64',2009,'aero',2048,25600,2600,580,60,2400,{ browserLevel:11,api:'dx11' }),
    os('pc-win8core','Windows 8',2012,'metro',1024,20480,2100,600,48,2100,{ browserLevel:10,api:'dx11' }),
    os('pc-win10ltsb','Windows 10 Enterprise LTSB 2016',2016,'win10',2048,32768,3200,950,68,2700,{ browserLevel:44,api:'dx12' }),
    os('pc-win10ltsc','Windows 10 IoT Enterprise LTSC 2021',2021,'win10',2048,32768,3900,1200,72,3000,{ browserLevel:109,api:'dx12' }),
    os('pc-win11tiny','Tiny11 23H2 · community image',2023,'win11',2048,20480,0,1800,78,2700,{ browserLevel:120,api:'dx12',community:true }),
    os('pc-win11ltsc','Windows 11 Enterprise LTSC 2024',2024,'win11',4096,65536,5200,2800,88,3600,{ browserLevel:132,api:'dx12' }),
    os('pc-reactos','ReactOS 0.4.14',2022,'classic',256,2048,0,180,38,1500,{ family:'windows',browserLevel:48,api:'win32',community:true }),
    os('pc-winserver2003','Windows Server 2003 R2',2003,'classic',256,4096,1800,220,42,2100,{ browserLevel:8,api:'win32' }),
    os('pc-winserver2008','Windows Server 2008 R2',2009,'aero',1024,32768,2800,520,65,2700,{ browserLevel:11,api:'dx11' }),
    os('pc-winserver2012','Windows Server 2012 R2',2013,'metro',2048,40960,3600,850,70,3000,{ browserLevel:11,api:'dx11' }),
    os('pc-winserver2016','Windows Server 2016',2016,'win10',2048,40960,4200,1100,80,3300,{ browserLevel:44,api:'dx12' }),
    os('pc-winserver2022','Windows Server 2022',2021,'win10',4096,65536,5800,2200,90,3600,{ browserLevel:120,api:'dx12' }),
    os('pc-ubuntu0606','Ubuntu 6.06 LTS Dapper Drake',2006,'linux',256,4096,0,220,35,1500,{ browserLevel:3,api:'linux' }),
    os('pc-ubuntu1604','Ubuntu 16.04 LTS',2016,'linux',1024,16384,0,720,48,2100,{ browserLevel:68,api:'linux' }),
    os('pc-ubuntu2204','Ubuntu 22.04 LTS',2022,'linux',4096,32768,0,1500,68,2700,{ browserLevel:125,api:'linux' }),
    os('pc-debian31','Debian 3.1 Sarge',2005,'linux',128,2048,0,130,25,1200,{ browserLevel:3,api:'linux' }),
    os('pc-debian7','Debian 7 Wheezy',2013,'linux',512,8192,0,420,35,1500,{ browserLevel:24,api:'linux' }),
    os('pc-debian10','Debian 10 Buster',2019,'linux',1024,16384,0,850,45,1800,{ browserLevel:78,api:'linux' }),
    os('pc-debian12','Debian 12 Bookworm',2023,'linux',2048,20480,0,1300,55,2100,{ browserLevel:115,api:'linux' }),
    os('pc-lubuntu1804','Lubuntu 18.04 LTS',2018,'linux',512,8192,0,400,28,1500,{ browserLevel:80,api:'linux' }),
    os('pc-lubuntu2204','Lubuntu 22.04 LTS',2022,'linux',1024,16384,0,650,32,1800,{ browserLevel:120,api:'linux' }),
    os('pc-xubuntu2004','Xubuntu 20.04 LTS',2020,'linux',1024,16384,0,720,38,1800,{ browserLevel:110,api:'linux' }),
    os('pc-mint21','Linux Mint 21.3 Xfce',2024,'linux',2048,20480,0,1000,48,2100,{ browserLevel:128,api:'linux' }),
    os('pc-antix23','antiX 23 · runit',2023,'linux',256,4096,0,220,18,1200,{ browserLevel:115,api:'linux' }),
    os('pc-puppy9','Puppy Linux 9.5 Fossapup',2020,'linux',256,2048,0,180,12,900,{ browserLevel:88,api:'linux' }),
    os('pc-slax15','Slax 15 · Debian',2024,'linux',512,4096,0,450,22,1200,{ browserLevel:120,api:'linux' }),
    os('pc-knoppix9','Knoppix 9.3',2024,'linux',1024,8192,0,700,38,1800,{ browserLevel:120,api:'linux' }),
    os('pc-arch2024','Arch Linux 2024.11 · Xfce',2024,'linux',1024,16384,0,900,32,1800,{ browserLevel:132,api:'linux' }),
    os('pc-fedora40','Fedora Workstation 40',2024,'linux',4096,32768,0,1700,65,2700,{ browserLevel:132,api:'linux' }),
    os('pc-opensuse156','openSUSE Leap 15.6',2024,'linux',2048,24576,0,1300,58,2400,{ browserLevel:128,api:'linux' }),
    os('pc-zorin17','Zorin OS 17.2 Core',2024,'linux',2048,20480,0,1250,55,2400,{ browserLevel:130,api:'linux' }),
    os('pc-chromeosflex','ChromeOS Flex',2022,'linux',4096,16384,0,1450,48,2100,{ browserLevel:132,api:'linux' }),
    os('pc-android44','Android-x86 4.4 r5',2016,'linux',1024,4096,0,480,35,1500,{ family:'android',browserLevel:50,api:'android' }),
    os('pc-android9','Android-x86 9.0 r2',2022,'linux',2048,8192,0,850,42,1800,{ family:'android',browserLevel:100,api:'android' }),
    os('pc-bliss15','Bliss OS 15.9 · Android 12L',2024,'linux',4096,16384,0,1500,55,2100,{ family:'android',browserLevel:120,api:'android' }),
    os('pc-freebsd14','FreeBSD 14.1 + Xfce',2024,'linux',2048,20480,0,1100,48,2100,{ family:'bsd',browserLevel:125,api:'bsd' }),
    os('pc-openbsd75','OpenBSD 7.5 + Xenocara',2024,'linux',1024,16384,0,800,42,1800,{ family:'bsd',browserLevel:120,api:'bsd' }),
    os('pc-haiku','Haiku R1 Beta 4',2022,'linux',512,4096,0,450,18,1200,{ family:'haiku',browserLevel:105,api:'haiku' })
  ];
  DATA.PC_OS.push(...DATA.PC_REALISM_OS);

  const bench = (id, name, year, cpu, gpu, ramMb, target, description, extra = {}) => Object.assign({ id, name, year, icon:'games', cpu, gpu, ramMb, target, description }, extra);
  DATA.PC_REALISM_BENCHMARKS = {
    wolf3d:bench('wolf3d','Wolfenstein 3D · E1M1',1992,12,1,4,70,'DOS/source port · software raycasting',{ api:'dos',maxFps:70,sizeMb:12 }),
    doom2:bench('doom2','DOOM II · MAP01',1994,30,4,16,105,'DOOM source port · 640×400 · 35/70/120 Hz modes',{ api:'dos',maxFps:120,sizeMb:55 }),
    duke3d:bench('duke3d','Duke Nukem 3D · Hollywood',1996,48,8,32,90,'Build engine · software/Polymost',{ api:'dos',maxFps:144,sizeMb:180 }),
    quake2:bench('quake2','Quake II · demo1',1997,70,22,64,120,'OpenGL 1.1 · 1024×768',{ api:'win32',maxFps:240,sizeMb:320 }),
    unreal:bench('unreal','Unreal Gold · Vortex Rikers',1998,82,32,128,90,'Glide/D3D · легендарный Unreal Engine',{ api:'win32',maxFps:200,sizeMb:600 }),
    ut99:bench('ut99','Unreal Tournament · Facing Worlds',1999,95,42,128,120,'8 ботов · D3D/OpenGL',{ api:'win32',maxFps:240,sizeMb:650 }),
    quake3:bench('quake3','Quake III Arena · q3dm17',1999,105,48,128,125,'id Tech 3 · 1024×768',{ api:'win32',maxFps:250,sizeMb:500 }),
    deusex:bench('deusex','Deus Ex · Liberty Island',2000,115,45,256,90,'Unreal Engine · большой уровень',{ api:'win32',maxFps:150,sizeMb:900 }),
    maxpayne:bench('maxpayne','Max Payne · Roscoe Street',2001,145,68,256,90,'Remedy engine · bullet time',{ api:'win32',maxFps:144,sizeMb:1200 }),
    rtcw:bench('rtcw','Return to Castle Wolfenstein · Village',2001,155,75,256,90,'id Tech 3 · тяжёлая деревня',{ api:'win32',maxFps:180,sizeMb:1400 }),
    morrowind:bench('morrowind','The Elder Scrolls III: Morrowind · Balmora',2002,175,85,512,60,'Exterior · дальность прорисовки 70%',{ api:'dx8',maxFps:120,sizeMb:1600 }),
    nfsu:bench('nfsu','Need for Speed Underground · Olympic',2003,190,100,512,100,'Ночной город · высокие настройки',{ api:'dx9',maxFps:200,sizeMb:2200 }),
    farcry:bench('farcry','Far Cry · Pier',2004,235,145,512,60,'CryEngine · растительность и shaders',{ api:'dx9',maxFps:120,sizeMb:3500 }),
    hl2:bench('hl2','Half-Life 2 · Route Kanal',2004,220,135,512,120,'Source Engine · physics and water',{ api:'dx9',maxFps:300,sizeMb:4500 }),
    css:bench('css','Counter-Strike: Source · de_dust2',2004,230,145,512,144,'Source · high · 16 игроков',{ api:'dx9',maxFps:300,sizeMb:4200 }),
    wow:bench('wow','World of Warcraft · Elwynn Forest',2004,210,115,1024,100,'Classic-era scene · NPC и игроки',{ api:'dx9',maxFps:200,sizeMb:6500 }),
    fear:bench('fear','F.E.A.R. · Interval 01',2005,285,190,1024,90,'LithTech Jupiter EX · soft shadows',{ api:'dx9',maxFps:180,sizeMb:5200 }),
    oblivion:bench('oblivion','Oblivion · Imperial City',2006,340,220,2048,75,'Gamebryo · NPC и растительность',{ api:'dx9',maxFps:120,sizeMb:6000 }),
    stalker:bench('stalker','S.T.A.L.K.E.R. · Cordon',2007,380,240,2048,90,'X-Ray · dynamic lighting',{ api:'dx9',maxFps:144,sizeMb:7500 }),
    portal:bench('portal','Portal · Test Chamber 18',2007,330,210,2048,144,'Source · порталы и physics',{ api:'dx9',maxFps:300,sizeMb:3500 }),
    tf2:bench('tf2','Team Fortress 2 · 2Fort',2007,350,220,2048,144,'Source · 24 игрока',{ api:'dx9',maxFps:300,sizeMb:8500 }),
    bioshock:bench('bioshock','BioShock · Medical Pavilion',2007,390,250,2048,90,'Unreal Engine 2.5 · D3D9',{ api:'dx9',maxFps:180,sizeMb:7000 }),
    crysiswh:bench('crysiswh','Crysis Warhead · Call me Ishmael',2008,480,340,2048,60,'CryEngine 2 · High · DX10',{ api:'dx10',maxFps:144,sizeMb:8500 }),
    gta4:bench('gta4','Grand Theft Auto IV · Broker',2008,540,330,3072,60,'RAGE · городской трафик',{ api:'dx9',maxFps:120,sizeMb:16000 }),
    l4d2:bench('l4d2','Left 4 Dead 2 · Dead Center',2009,470,280,2048,144,'Source · Horde stress scene',{ api:'dx9',maxFps:300,sizeMb:9000 }),
    skyrim:bench('skyrim','Skyrim · Whiterun Tundra',2011,620,420,4096,90,'Creation Engine · exterior',{ api:'dx9',maxFps:144,sizeMb:12000 }),
    csgo:bench('csgo','Counter-Strike: Global Offensive · Dust II',2012,780,450,4096,180,'Source · 10 players · High',{ api:'dx9',maxFps:400,sizeMb:18000 }),
    dota2:bench('dota2','Dota 2 · Mid Teamfight',2013,850,470,4096,144,'Source 2 compatibility scene · 10 героев',{ api:'dx9',maxFps:240,sizeMb:20000 }),
    gtav:bench('gtav','Grand Theft Auto V · Los Santos',2015,1250,820,8192,90,'RAGE · Very High · city benchmark',{ api:'dx11',maxFps:180,sizeMb:90000 }),
    doom2016:bench('doom2016','DOOM 2016 · Foundry',2016,1500,1050,8192,120,'id Tech 6 · Vulkan · High',{ api:'vulkan',maxFps:240,sizeMb:65000 }),
    cs2:bench('cs2','Counter-Strike 2 · Ancient',2023,3200,2300,16384,180,'Source 2 · High · smoke and players',{ api:'dx11',maxFps:400,sizeMb:45000 }),
    pcsx2:bench('pcsx2','PCSX2 · Gran Turismo 4',2006,1050,480,4096,60,'PS2 emulator · 4× native · Vulkan',{ api:'emulator',maxFps:60,sizeMb:45,kind:'emulator' }),
    dolphin:bench('dolphin','Dolphin · Super Mario Sunshine',2008,1250,520,4096,60,'GameCube emulator · 2× native · dual core',{ api:'emulator',maxFps:60,sizeMb:35,kind:'emulator' }),
    retroarch:bench('retroarch','RetroArch · bsnes-mercury Accuracy',2010,950,160,2048,60,'SNES emulator · accuracy profile · run-ahead off',{ api:'emulator',maxFps:60,sizeMb:180,kind:'emulator' }),
    ppsspp:bench('ppsspp','PPSSPP · God of War: Chains of Olympus',2012,900,420,2048,60,'PSP emulator · 3× resolution · Vulkan',{ api:'emulator',maxFps:60,sizeMb:80,kind:'emulator' })
  };
  Object.assign(DATA.PC_BENCHMARKS, DATA.PC_REALISM_BENCHMARKS);
  Object.assign(DATA.PC_BENCHMARKS.doom, { api:'dos',maxFps:120,sizeMb:40 });
  Object.assign(DATA.PC_BENCHMARKS.quake, { api:'dos',maxFps:240,sizeMb:120 });
  Object.assign(DATA.PC_BENCHMARKS.halflife, { api:'win32',maxFps:240,sizeMb:350 });
  Object.assign(DATA.PC_BENCHMARKS.cs16, { api:'win32',maxFps:300,sizeMb:550 });
  Object.assign(DATA.PC_BENCHMARKS.gta3, { api:'win32',maxFps:120,sizeMb:1500 });
  Object.assign(DATA.PC_BENCHMARKS.doom3, { api:'win32',maxFps:180,sizeMb:2200 });
  Object.assign(DATA.PC_BENCHMARKS.crysis, { api:'dx10',maxFps:144,sizeMb:6500 });
  Object.assign(DATA.PC_BENCHMARKS.minecraft, { api:'java',maxFps:360,sizeMb:500 });
  Object.assign(DATA.PC_BENCHMARKS.witcher3, { api:'dx11',maxFps:180,sizeMb:35000 });
  Object.assign(DATA.PC_BENCHMARKS.cyberpunk, { api:'dx12',maxFps:240,sizeMb:70000 });

  DATA.PC_REALISM_GAME_PACKAGES = Object.values(DATA.PC_REALISM_BENCHMARKS).map(game => ({
    id:game.id,name:game.name,year:game.year,icon:game.icon,kind:game.kind || 'game',sizeMb:game.sizeMb || 500,
    description:game.description,benchmarkId:game.id,minRamMb:game.ramMb,target:game.target,api:game.api
  }));
  const pkg = (id,name,year,kind,sizeMb,description,extra = {}) => Object.assign({ id,name,year,kind,sizeMb,description,icon:'apps' },extra);
  DATA.PC_REALISM_SOFTWARE_PACKAGES = [
    pkg('supermium','Supermium 132 · Windows XP/7 modern browser',2024,'browser',185,'Современный Chromium-движок для Windows XP SP3, Vista и 7. На XP требуется минимум 512 МБ RAM.',{ minRamMb:512,browserLevel:132,supportedFamilies:['windows'],minOSYear:2001 }),
    pkg('firefox-esr','Firefox ESR 115 · legacy Windows',2023,'browser',95,'Последняя линия Firefox для Windows 7/8; XP не поддерживается.',{ minRamMb:512,browserLevel:115,supportedFamilies:['windows','linux'],minOSYear:2009 }),
    pkg('kmeleon','K-Meleon 76.4 Goanna',2021,'browser',42,'Лёгкий браузер для Windows XP; современные сайты поддерживает ограниченно.',{ minRamMb:256,browserLevel:52,supportedFamilies:['windows'],minOSYear:2001 }),
    pkg('mypal','MyPal 68 · XP browser',2022,'browser',68,'Firefox-derived браузер для Windows XP. Supermium нужен для newest-web.local.',{ minRamMb:512,browserLevel:68,supportedFamilies:['windows'],minOSYear:2001 }),
    pkg('chrome109','Google Chrome 109 · last Windows 7 release',2023,'browser',110,'Последний Chrome для Windows 7/8.1; на XP установщик несовместим.',{ minRamMb:1024,browserLevel:109,supportedFamilies:['windows'],minOSYear:2009 }),
    pkg('links','Links 2.29 · text/graphics browser',2023,'browser',8,'Лёгкий браузер для DOS, Linux и старых Windows; без современного JavaScript.',{ minRamMb:16,browserLevel:2,supportedFamilies:['dos','windows','linux','bsd'] }),
    pkg('wine','Wine 9.0 + Winetricks offline',2024,'utility',650,'Слой совместимости Windows-игр на Linux/BSD. Нужен для запуска Win32/DirectX-пакетов.',{ minRamMb:1024,supportedFamilies:['linux','bsd'] }),
    pkg('dosbox','DOSBox Staging 0.82',2024,'emulator',48,'DOS-эмулятор для новых Windows/Linux. Открывает DOS-игры без родного DOS.',{ minRamMb:256,supportedFamilies:['windows','linux','bsd'],api:'emulator' }),
    pkg('scummvm','ScummVM 2.8',2024,'emulator',95,'Интерпретатор классических adventure engines; сохранения и фильтры.',{ minRamMb:256,supportedFamilies:['windows','linux','bsd'],api:'emulator' }),
    pkg('vmware','VMware Workstation 17 Player',2024,'utility',620,'Виртуализация x86; тяжёлая на слабом CPU и требует 4 ГБ RAM.',{ minRamMb:4096,supportedFamilies:['windows','linux'] }),
    pkg('sevenzip','7-Zip 24.08',2024,'utility',12,'Offline архиватор ZIP/7z для Windows и Linux.',{ minRamMb:64,supportedFamilies:['windows','linux'] }),
    pkg('vlc','VLC 3.0.21',2024,'utility',85,'Локальный медиаплеер без сетевых зависимостей.',{ minRamMb:256,supportedFamilies:['windows','linux','bsd'] })
  ];
  /* Единый каталог остаётся обратносовместимым: старые 10 элементов сохраняют индексы, новые добавляются после них. */
  DATA.OS_WORLD_GAMES.push(...DATA.PC_REALISM_GAME_PACKAGES, ...DATA.PC_REALISM_SOFTWARE_PACKAGES);
  DATA.OS_WORLD_SITES.pc['realism.local'] = { title:'Realism Games & Emulators',description:'35 дополнительных игр и emulator benchmarks · общие downloads и FPS.' };
  DATA.OS_WORLD_SITES.pc['compatibility.local'] = { title:'Browser & Compatibility Store',description:'Supermium, Wine, DOSBox, браузеры и утилиты с проверкой ОС/RAM.' };
  DATA.OS_WORLD_SITES.pc['modern.local'] = { title:'Modern Web Compatibility Lab',description:'ES2022, WebGL2 и CSS Grid · нужен browser level 100+.',modernRequired:100 };
  DATA.OS_WORLD_SITES.pc['newest-web.local'] = { title:'Newest Web 2026',description:'Современный локальный сайт · Supermium на XP или новый системный браузер.',modernRequired:120 };

  const model = (id,name,year,kind,cpu,cpuScore,ramMb,ramType,storageMb,storageType,gpu,gpuScore,msrp,extra = {}) => Object.assign({
    id,name,year,kind,cpu,cpuScore,ramMb,ramType,storageMb,storageType,gpu,gpuScore,msrp,
    psuWatts:65,cooling:52,batteryWh:48,weightKg:2.2,displaySize:14,displayResolution:'1366×768',refreshHz:60,
    cpuUpgradeable:false,cpuSocket:'BGA',maxCpuTdp:0,ramSlots:2,ramUpgradeable:true,storageUpgradeable:true,maxRamMb:Math.max(ramMb * 4,8192),storageInterfaces:['SATA']
  },extra);
  DATA.PREBUILT_MOBILE_CATALOG = [
    model('thinkpad-760','IBM ThinkPad 760XL',1997,'laptop','Intel Pentium MMX 166',68,64,'EDO SO-DIMM',3200,'HDD','Trident Cyber9385 2MB',10,98000,{ batteryWh:28,weightKg:3.1,displaySize:12.1,displayResolution:'1024×768',cpuUpgradeable:true,cpuSocket:'MMC-1',maxCpuTdp:12,ramSlots:2,maxRamMb:144,storageInterfaces:['PATA'] }),
    model('toshiba-320','Toshiba Satellite 320CDT',1998,'laptop','Intel Pentium MMX 233',75,64,'EDO SO-DIMM',4096,'HDD','Chips & Technologies 65555',11,79000,{ batteryWh:32,weightKg:3,displaySize:12.1,displayResolution:'1024×768',ramSlots:2,maxRamMb:160,storageInterfaces:['PATA'] }),
    model('dell-latitude-cp','Dell Latitude CPi A366XT',1999,'laptop','Intel Pentium II 366',84,128,'SDRAM SO-DIMM',6400,'HDD','NeoMagic MagicMedia 256AV',14,69000,{ batteryWh:38,weightKg:2.8,displaySize:13.3,displayResolution:'1024×768',cpuUpgradeable:true,cpuSocket:'MMC-2',maxCpuTdp:18,ramSlots:2,maxRamMb:512,storageInterfaces:['PATA'] }),
    model('thinkpad-t21','IBM ThinkPad T21',2000,'laptop','Intel Pentium III 750',105,128,'SDRAM SO-DIMM',10240,'HDD','S3 Savage/IX 8MB',20,76000,{ batteryWh:42,weightKg:2.4,displaySize:14.1,displayResolution:'1024×768',cpuUpgradeable:true,cpuSocket:'MMC-2',maxCpuTdp:24,ramSlots:2,maxRamMb:512,storageInterfaces:['PATA'] }),
    model('compaq-n600','Compaq Evo N600c',2001,'laptop','Intel Pentium III-M 866',115,256,'SDRAM SO-DIMM',20480,'HDD','ATI Rage Mobility-M 8MB',24,64000,{ batteryWh:44,weightKg:2.3,displaySize:14.1,displayResolution:'1024×768',ramSlots:2,maxRamMb:1024,storageInterfaces:['PATA'] }),
    model('thinkpad-t40','IBM ThinkPad T40',2003,'laptop','Intel Pentium M 1.5',155,512,'DDR SO-DIMM',40960,'HDD','ATI Mobility Radeon 7500 32MB',45,69000,{ batteryWh:48,weightKg:2.2,displaySize:14.1,displayResolution:'1024×768',cpuUpgradeable:true,cpuSocket:'Socket 479',maxCpuTdp:25,ramSlots:2,maxRamMb:2048,storageInterfaces:['PATA'] }),
    model('acer-1640','Acer TravelMate 1640',2005,'laptop','Intel Pentium M 740 1.73',205,512,'DDR2 SO-DIMM',61440,'HDD','Intel GMA 900',38,39000,{ batteryWh:48,weightKg:2.8,displaySize:15.4,displayResolution:'1280×800',cpuUpgradeable:true,cpuSocket:'Socket 479',maxCpuTdp:27,ramSlots:2,maxRamMb:2048,storageInterfaces:['PATA','SATA'] }),
    model('macbook-2006','Apple MacBook Core Duo 1.83',2006,'laptop','Intel Core Duo T2400',270,1024,'DDR2 SO-DIMM',81920,'HDD','Intel GMA 950',48,62000,{ batteryWh:55,weightKg:2.36,displaySize:13.3,displayResolution:'1280×800',cpuUpgradeable:true,cpuSocket:'Socket M',maxCpuTdp:31,ramSlots:2,maxRamMb:4096,storageInterfaces:['SATA'] }),
    model('eee-701','ASUS Eee PC 701 4G',2007,'netbook','Intel Celeron M 353 900 MHz',44,512,'DDR2 SO-DIMM',4096,'SSD','Intel GMA 900',24,12900,{ psuWatts:36,cooling:30,batteryWh:28,weightKg:.92,displaySize:7,displayResolution:'800×480',ramSlots:1,ramUpgradeable:false,storageUpgradeable:false,maxRamMb:2048,storageInterfaces:['SATA','mini-PCIe SATA'] }),
    model('eee-900','ASUS Eee PC 900 · 900 MHz',2008,'netbook','Intel Celeron M 353 900 MHz',48,1024,'DDR2 SO-DIMM',16384,'SSD','Intel GMA 900',28,15900,{ psuWatts:36,cooling:32,batteryWh:32,weightKg:.99,displaySize:8.9,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA','mini-PCIe SATA'],legendary:true }),
    model('aspire-one','Acer Aspire One A150',2008,'netbook','Intel Atom N270 1.6 GHz',72,1024,'DDR2 SO-DIMM',163840,'HDD','Intel GMA 950',32,13900,{ psuWatts:30,cooling:32,batteryWh:24,weightKg:1.1,displaySize:8.9,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA'] }),
    model('msi-wind','MSI Wind U100',2008,'netbook','Intel Atom N270 1.6 GHz',74,1024,'DDR2 SO-DIMM',163840,'HDD','Intel GMA 950',32,14500,{ psuWatts:40,cooling:34,batteryWh:32,weightKg:1.18,displaySize:10,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA'] }),
    model('hp-mini2133','HP 2133 Mini-Note',2008,'netbook','VIA C7-M 1.2 GHz',52,1024,'DDR2 SO-DIMM',122880,'HDD','VIA Chrome9 HC',22,17900,{ psuWatts:65,cooling:34,batteryWh:28,weightKg:1.27,displaySize:8.9,displayResolution:'1280×768',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA','PATA'] }),
    model('dell-mini9','Dell Inspiron Mini 9',2008,'netbook','Intel Atom N270 1.6 GHz',72,1024,'DDR2 SO-DIMM',16384,'SSD','Intel GMA 950',30,13500,{ psuWatts:30,cooling:30,batteryWh:32,weightKg:1.03,displaySize:8.9,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA','mini-PCIe SATA'] }),
    model('samsung-nc10','Samsung NC10',2008,'netbook','Intel Atom N270 1.6 GHz',75,1024,'DDR2 SO-DIMM',163840,'HDD','Intel GMA 950',32,15900,{ psuWatts:40,cooling:36,batteryWh:56,weightKg:1.33,displaySize:10.2,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA'] }),
    model('lenovo-s10','Lenovo IdeaPad S10',2008,'netbook','Intel Atom N270 1.6 GHz',73,1024,'DDR2 SO-DIMM',163840,'HDD','Intel GMA 950',31,14200,{ psuWatts:40,cooling:34,batteryWh:28,weightKg:1.1,displaySize:10.2,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA'] }),
    model('eee-1005ha','ASUS Eee PC 1005HA Seashell',2009,'netbook','Intel Atom N280 1.66 GHz',82,1024,'DDR2 SO-DIMM',163840,'HDD','Intel GMA 950',34,16900,{ psuWatts:40,cooling:35,batteryWh:63,weightKg:1.27,displaySize:10.1,displayResolution:'1024×600',ramSlots:1,maxRamMb:2048,storageInterfaces:['SATA'] }),
    model('nokia-booklet','Nokia Booklet 3G',2009,'netbook','Intel Atom Z530 1.6 GHz',62,1024,'DDR2 SO-DIMM',122880,'HDD','Intel GMA 500',26,24900,{ psuWatts:40,cooling:28,batteryWh:57,weightKg:1.25,displaySize:10.1,displayResolution:'1280×720',ramSlots:1,ramUpgradeable:false,maxRamMb:1024,storageInterfaces:['SATA','PATA ZIF'] }),
    model('macbook-air2010','Apple MacBook Air 11 Late 2010',2010,'laptop','Intel Core 2 Duo SU9400',350,2048,'DDR3 SO-DIMM',131072,'SSD','NVIDIA GeForce 320M',180,59000,{ psuWatts:45,cooling:45,batteryWh:35,weightKg:1.06,displaySize:11.6,displayResolution:'1366×768',ramSlots:2,ramUpgradeable:false,maxRamMb:4096,storageInterfaces:['SATA','Apple proprietary'] }),
    model('thinkpad-x220','Lenovo ThinkPad X220',2011,'laptop','Intel Core i5-2520M',610,4096,'DDR3 SO-DIMM',327680,'HDD','Intel HD 3000',170,62000,{ batteryWh:63,weightKg:1.5,displaySize:12.5,cpuUpgradeable:true,cpuSocket:'Socket G2',maxCpuTdp:35,ramSlots:2,maxRamMb:16384,storageInterfaces:['SATA','mSATA'] }),
    model('samsung-series5','Samsung Series 5 Chromebook',2011,'netbook','Intel Atom N570 1.66 GHz',105,2048,'DDR3 SO-DIMM',16384,'SSD','Intel GMA 3150',38,22900,{ psuWatts:40,cooling:32,batteryWh:61,weightKg:1.48,displaySize:12.1,displayResolution:'1280×800',ramSlots:1,ramUpgradeable:false,maxRamMb:2048,storageInterfaces:['SATA','mini-PCIe SATA'] }),
    model('thinkpad-x230','Lenovo ThinkPad X230',2012,'laptop','Intel Core i5-3320M',760,4096,'DDR3 SO-DIMM',524288,'SSD','Intel HD 4000',230,68000,{ batteryWh:63,weightKg:1.52,displaySize:12.5,cpuUpgradeable:true,cpuSocket:'Socket G2',maxCpuTdp:35,ramSlots:2,maxRamMb:16384,storageInterfaces:['SATA','mSATA'] }),
    model('acer-c720','Acer Chromebook C720',2013,'netbook','Intel Celeron 2955U',420,2048,'DDR3L SO-DIMM',16384,'SSD','Intel HD Haswell',150,12900,{ psuWatts:40,cooling:36,batteryWh:30,weightKg:1.25,displaySize:11.6,ramSlots:1,ramUpgradeable:false,maxRamMb:4096,storageInterfaces:['SATA','M.2 SATA'] }),
    model('thinkpad-t450','Lenovo ThinkPad T450',2015,'laptop','Intel Core i5-5300U',1050,8192,'DDR3L SO-DIMM',524288,'SSD','Intel HD 5500',390,72000,{ batteryWh:48,weightKg:1.81,displaySize:14,ramSlots:2,maxRamMb:16384,storageInterfaces:['SATA','M.2 SATA'] }),
    model('dell-xps9350','Dell XPS 13 9350',2016,'laptop','Intel Core i5-6200U',1250,8192,'DDR3L SO-DIMM',262144,'SSD','Intel HD 520',480,89000,{ psuWatts:45,batteryWh:56,weightKg:1.29,displaySize:13.3,displayResolution:'1920×1080',ramSlots:2,ramUpgradeable:false,maxRamMb:16384,storageInterfaces:['NVMe','SATA'] }),
    model('thinkpad-t480','Lenovo ThinkPad T480',2018,'laptop','Intel Core i5-8250U',1950,8192,'DDR4 SO-DIMM',524288,'NVMe','Intel UHD 620',590,82000,{ batteryWh:48,weightKg:1.58,displaySize:14,displayResolution:'1920×1080',ramSlots:2,maxRamMb:32768,storageInterfaces:['NVMe','SATA'] }),
    model('asus-l210','ASUS Laptop L210',2020,'netbook','Intel Celeron N4020 1.1 GHz',680,4096,'DDR4 SO-DIMM',131072,'eMMC','Intel UHD 600',330,19900,{ psuWatts:33,cooling:30,batteryWh:38,weightKg:1.05,displaySize:11.6,ramSlots:1,ramUpgradeable:false,storageUpgradeable:false,maxRamMb:4096,storageInterfaces:['M.2 SATA','eMMC'] }),
    model('framework13','Framework Laptop 13 Ryzen 5',2023,'laptop','AMD Ryzen 5 7640U',5100,16384,'DDR5 SO-DIMM',1048576,'NVMe','AMD Radeon 760M',2500,99000,{ psuWatts:65,batteryWh:55,weightKg:1.3,displaySize:13.5,displayResolution:'2256×1504',cpuUpgradeable:false,cpuSocket:'Mainboard module',maxCpuTdp:0,ramSlots:2,maxRamMb:65536,storageInterfaces:['NVMe'] }),
    model('thinkpad-x1g12','Lenovo ThinkPad X1 Carbon Gen 12',2024,'laptop','Intel Core Ultra 7 155U',5900,32768,'LPDDR5x SO-DIMM',2097152,'NVMe','Intel Arc Graphics',3100,139000,{ psuWatts:65,batteryWh:57,weightKg:1.08,displaySize:14,displayResolution:'1920×1200',ramSlots:2,ramUpgradeable:false,maxRamMb:65536,storageInterfaces:['NVMe'] }),
    model('asus-rogally','ASUS ROG Ally X · mobile PC',2024,'netbook','AMD Ryzen Z1 Extreme',6800,24576,'LPDDR5x SO-DIMM',2097152,'NVMe','AMD Radeon RDNA3 8CU',4200,79900,{ psuWatts:65,cooling:72,batteryWh:80,weightKg:.68,displaySize:7,displayResolution:'1920×1080',refreshHz:120,ramSlots:2,ramUpgradeable:false,maxRamMb:32768,storageInterfaces:['NVMe'] }),
    model('framework13-2026','Framework Laptop 13 Next Gen',2026,'laptop','Next-gen mobile x86 12-core',8200,32768,'DDR5 SO-DIMM',4194304,'NVMe','Neural mobile GPU',5400,149000,{ psuWatts:100,batteryWh:75,weightKg:1.25,displaySize:13.5,displayResolution:'2880×1920',refreshHz:120,cpuUpgradeable:false,cpuSocket:'Mainboard module',maxCpuTdp:0,ramSlots:2,maxRamMb:131072,storageInterfaces:['NVMe'] })
  ];
  DATA.PREBUILT_MOBILE_INDEX = new Map(DATA.PREBUILT_MOBILE_CATALOG.map(item => [item.id,item]));
  DATA.pcRealismDataLoaded = true;
})();
