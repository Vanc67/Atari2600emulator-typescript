/* Ultimate 7.0: поколения домашних ПК, потребительские ОС и игровые тесты. */
(function () {
  'use strict';
  if (!window.DATA || DATA.pcDataLoaded) return;
  const rows = [
    [1990,'IBM PS/2 386','Intel 386DX-25',10,4,'SIMM',100,'HDD','VGA 256K',1,45,180],
    [1991,'Compaq Deskpro 386','Intel 386DX-40',13,8,'SIMM',210,'HDD','SVGA 512K',2,50,200],
    [1992,'IBM PS/ValuePoint 486','Intel 486SX-25',18,8,'SIMM',340,'HDD','Cirrus VGA 512K',3,55,200],
    [1993,'Compaq ProLinea 486','Intel 486DX2-66',26,16,'SIMM',540,'HDD','S3 Trio 1MB',5,60,220],
    [1994,'AcerPower 486','Intel 486DX4-100',34,16,'SIMM',850,'HDD','S3 Trio64 1MB',7,65,230],
    [1995,'Dell Dimension P75','Intel Pentium 75',44,32,'SIMM',1200,'HDD','ATI Mach64 2MB',10,70,250],
    [1996,'HP Vectra Pentium','Intel Pentium 133',56,64,'SIMM',2100,'HDD','S3 Virge 2MB',15,75,250],
    [1997,'IBM PC 300GL','Intel Pentium MMX 166',68,64,'SIMM',3200,'HDD','3dfx Voodoo 4MB',24,80,300],
    [1998,'Dell Dimension V400','Intel Pentium II 400',82,128,'SDRAM',6400,'HDD','Riva TNT 16MB',34,90,300],
    [1999,'Compaq Presario K6-2','AMD K6-2 450',98,128,'SDRAM',10240,'HDD','Voodoo3 16MB',44,100,300],
    [2000,'HP Pavilion Athlon','AMD Athlon 800',118,256,'SDRAM',20480,'HDD','GeForce 256 32MB',58,110,350],
    [2001,'Dell Dimension P4','Intel Pentium 4 1.6',138,256,'SDRAM',40960,'HDD','GeForce2 MX 32MB',65,120,350],
    [2002,'IBM NetVista P4','Intel Pentium 4 2.4',158,512,'DDR',61440,'HDD','Radeon 8500 64MB',85,135,350],
    [2003,'Acer Aspire Athlon XP','AMD Athlon XP 2800+',182,512,'DDR',81920,'HDD','Radeon 9600 128MB',105,145,400],
    [2004,'HP Compaq Presario','Intel Pentium 4 3.0 HT',205,512,'DDR',122880,'HDD','GeForce FX 5700 128MB',120,160,400],
    [2005,'Dell Dimension 5150','Intel Pentium 4 630',232,512,'DDR2',163840,'HDD','GeForce 6600 128MB',145,180,450],
    [2006,'HP Pavilion Core 2','Intel Core 2 Duo E6300',285,1024,'DDR2',245760,'HDD','GeForce 7300 GT 256MB',170,200,450],
    [2007,'Dell Inspiron 530','Intel Core 2 Duo E6550',340,2048,'DDR2',327680,'HDD','GeForce 8600 GT 256MB',230,220,500],
    [2008,'HP Pavilion Phenom','AMD Phenom X4 9550',405,2048,'DDR2',512000,'HDD','Radeon HD 3850 256MB',290,240,500],
    [2009,'Dell Studio XPS','Intel Core i7-920',485,4096,'DDR3',655360,'HDD','GeForce GTX 260 896MB',360,260,600],
    [2010,'HP Pavilion i5','Intel Core i5-750',565,4096,'DDR3',1048576,'HDD','Radeon HD 5770 1GB',430,280,600],
    [2011,'Custom Sandy Bridge','Intel Core i5-2500K',680,4096,'DDR3',1048576,'HDD','GeForce GTX 560 1GB',520,300,650],
    [2012,'Dell XPS 8500','Intel Core i7-3770',790,8192,'DDR3',131072,'SSD','GeForce GTX 660 2GB',610,320,650],
    [2013,'Custom Haswell','Intel Core i5-4670K',920,8192,'DDR3',262144,'SSD','GeForce GTX 770 2GB',720,340,700],
    [2014,'HP Envy 700','Intel Core i7-4790',1080,8192,'DDR3',262144,'SSD','GeForce GTX 970 4GB',860,360,700],
    [2015,'Custom Skylake','Intel Core i5-6600K',1260,8192,'DDR4',524288,'SSD','Radeon R9 390 8GB',980,390,750],
    [2016,'Dell XPS 8920','Intel Core i7-7700',1510,16384,'DDR4',524288,'SSD','GeForce GTX 1070 8GB',1180,410,750],
    [2017,'Custom Ryzen 7','AMD Ryzen 7 1700X',1820,16384,'DDR4',524288,'SSD','GeForce GTX 1080 Ti 11GB',1400,440,800],
    [2018,'HP Omen 880','Intel Core i7-8700K',2180,16384,'DDR4',1048576,'SSD','RTX 2080 8GB',1680,470,850],
    [2019,'Custom Ryzen 3600','AMD Ryzen 5 3600',2580,32768,'DDR4',1048576,'SSD','RX 5700 XT 8GB',1900,500,850],
    [2020,'Custom Zen 3','AMD Ryzen 7 5800X',3100,32768,'DDR4',1048576,'NVMe','RTX 3070 8GB',2250,540,900],
    [2021,'HP Omen 30L','Intel Core i9-11900K',3720,32768,'DDR4',1048576,'NVMe','RTX 3080 10GB',2650,580,950],
    [2022,'Custom Alder Lake','Intel Core i7-12700K',4420,32768,'DDR5',2097152,'NVMe','RTX 3080 Ti 12GB',3050,620,1000],
    [2023,'Custom Ryzen 7800X3D','AMD Ryzen 7 7800X3D',5220,65536,'DDR5',2097152,'NVMe','RTX 4070 Ti 12GB',3500,660,1050],
    [2024,'Custom Core Ultra','Intel Core Ultra 9 285K',6180,65536,'DDR5',2097152,'NVMe','RTX 4080 Super 16GB',4050,700,1100],
    [2025,'Custom Ryzen 9950X3D','AMD Ryzen 9 9950X3D',7350,65536,'DDR5',4194304,'NVMe','RTX 5090 32GB',4800,750,1200],
    [2026,'Custom Next-Gen PC','Next-gen x86/ARM hybrid',8600,131072,'DDR5',4194304,'NVMe','Neural GPU 32GB',5700,800,1300]
  ];
  DATA.PC_GENERATIONS = rows.map(([year,name,cpu,cpuScore,ramMb,ramType,storageMb,storageType,gpu,gpuScore,cooling,psuWatts]) => ({ year,name,cpu,cpuScore,ramMb,ramType,storageMb,storageType,gpu,gpuScore,cooling,psuWatts }));
  DATA.PC_OS = [
    { id:'pc-dos33',name:'IBM PC DOS 3.3',year:1990,style:'dos',minRamMb:1,minStorageMb:20,price:0 },
    { id:'pc-dos622',name:'MS-DOS 6.22',year:1994,style:'dos',minRamMb:4,minStorageMb:50,price:0 },
    { id:'pc-win95',name:'Windows 95',year:1995,style:'classic',minRamMb:8,minStorageMb:100,price:500 },
    { id:'pc-win98',name:'Windows 98 SE',year:1998,style:'classic',minRamMb:32,minStorageMb:300,price:700 },
    { id:'pc-win2000',name:'Windows 2000 Professional',year:2000,style:'classic',minRamMb:64,minStorageMb:1000,price:900 },
    { id:'pc-winxp',name:'Windows XP Professional',year:2001,style:'xp',minRamMb:128,minStorageMb:2048,price:1200 },
    { id:'pc-vista',name:'Windows Vista',year:2007,style:'aero',minRamMb:1024,minStorageMb:20480,price:1800 },
    { id:'pc-win7',name:'Windows 7 Professional',year:2009,style:'aero',minRamMb:1024,minStorageMb:20480,price:2200 },
    { id:'pc-win8',name:'Windows 8.1',year:2013,style:'metro',minRamMb:1024,minStorageMb:20480,price:2500 },
    { id:'pc-win10',name:'Windows 10 Pro',year:2015,style:'win10',minRamMb:2048,minStorageMb:32768,price:3500 },
    { id:'pc-win11',name:'Windows 11 Pro',year:2021,style:'win11',minRamMb:4096,minStorageMb:65536,price:4500 },
    { id:'pc-ubuntu1004',name:'Ubuntu 10.04 LTS',year:2010,style:'linux',minRamMb:512,minStorageMb:8192,price:0 },
    { id:'pc-ubuntu2404',name:'Ubuntu 24.04 LTS',year:2024,style:'linux',minRamMb:4096,minStorageMb:32768,price:0 }
  ];
  DATA.PC_BENCHMARKS = {
    stability:{ id:'stability',name:'POST + Stability Suite',year:1990,icon:'chip',cpu:0,gpu:0,ramMb:1,target:60,description:'Универсальная проверка для возврата заказа' },
    doom:{ id:'doom',name:'DOOM · E1M1',year:1993,icon:'games',cpu:25,gpu:3,ramMb:8,target:35,description:'DOS · 320×200 · двери и демоны' },
    quake:{ id:'quake',name:'Quake · Timedemo',year:1996,icon:'games',cpu:55,gpu:15,ramMb:32,target:30,description:'Software/OpenGL · настоящий 3D' },
    halflife:{ id:'halflife',name:'Half-Life · Hazard Course',year:1998,icon:'games',cpu:75,gpu:28,ramMb:64,target:40,description:'OpenGL · тренировочный комплекс' },
    cs16:{ id:'cs16',name:'Counter-Strike 1.6 · de_dust2',year:2000,icon:'games',cpu:95,gpu:35,ramMb:128,target:60,description:'Online-бой локально · smoke и 16 игроков' },
    gta3:{ id:'gta3',name:'GTA III · Portland',year:2001,icon:'games',cpu:130,gpu:48,ramMb:256,target:30,description:'Первый открытый 3D-город' },
    doom3:{ id:'doom3',name:'DOOM 3 · Mars City',year:2004,icon:'games',cpu:205,gpu:95,ramMb:512,target:30,description:'Dynamic lights · тяжёлый OpenGL' },
    crysis:{ id:'crysis',name:'Crysis · Island',year:2007,icon:'games',cpu:420,gpu:280,ramMb:2048,target:30,description:'Легендарный вопрос: но потянет ли?' },
    minecraft:{ id:'minecraft',name:'Minecraft · Chunk Storm',year:2011,icon:'games',cpu:480,gpu:110,ramMb:2048,target:60,description:'Java · генерация чанков и мобы' },
    witcher3:{ id:'witcher3',name:'The Witcher 3 · Novigrad',year:2015,icon:'games',cpu:1150,gpu:700,ramMb:8192,target:60,description:'Город, NPC и плотная растительность' },
    cyberpunk:{ id:'cyberpunk',name:'Cyberpunk 2077 · Night City',year:2020,icon:'games',cpu:2800,gpu:2100,ramMb:16384,target:60,description:'Тяжёлый город · без RT и с RT-штрафом' }
  };
  DATA.PC_ISSUES = {
    dust:{ id:'dust',name:'Пыль и перегрев',test:'thermal',action:'clean',description:'Решётки забиты, нужна ручная чистка' },
    thermalPaste:{ id:'thermalPaste',name:'Высохшая термопаста',test:'thermal',action:'paste',description:'Заменить термоинтерфейс · 50 ₽' },
    psu:{ id:'psu',name:'Неисправный блок питания',test:'post',action:'part',part:'psu',description:'Нет старта или питание пропадает' },
    ram:{ id:'ram',name:'Сбойная / недостаточная RAM',test:'memory',action:'part',part:'ram',description:'MemTest находит ошибки адресации' },
    disk:{ id:'disk',name:'BAD-секторы накопителя',test:'disk',action:'part',part:'disk',description:'SMART и чтение нестабильны' },
    gpu:{ id:'gpu',name:'Артефакты видеокарты',test:'graphics',action:'part',part:'gpu',description:'Видеотест показывает артефакты' },
    virus:{ id:'virus',name:'Вирусы и майнер',test:'software',action:'virus',description:'Провести offline antivirus scan' },
    os:{ id:'os',name:'Повреждена операционная система',test:'software',action:'os',description:'Нужна установка или переустановка ОС' },
    bios:{ id:'bios',name:'Сбой настроек BIOS',test:'post',action:'bios',description:'Clear CMOS и проверка POST' }
  };
  DATA.PC_DIAGNOSTICS = {
    post:{ id:'post',name:'POST / Power',minutes:10,icon:'power' },
    memory:{ id:'memory',name:'MemTest',minutes:20,icon:'ram' },
    disk:{ id:'disk',name:'SMART / Surface',minutes:20,icon:'hdd' },
    thermal:{ id:'thermal',name:'Thermal Scan',minutes:15,icon:'temp' },
    graphics:{ id:'graphics',name:'Video Test',minutes:15,icon:'gpu' },
    software:{ id:'software',name:'OS / Virus Scan',minutes:20,icon:'warning' }
  };
  DATA.PC_SPEEDS = {
    1:{ id:1,name:'Долгая',dayMinutes:90,description:'1990→2026 ≈ 27 суток реального времени' },
    15:{ id:15,name:'Сюжетная',dayMinutes:6,description:'Полная эпоха ≈ 43 часов' },
    60:{ id:60,name:'Быстрая',dayMinutes:1.5,description:'Полная эпоха ≈ 11 часов' }
  };
  DATA.pcDataLoaded = true;
})();
