/* Expansion 2.0: каталог реального железа, ОС, сервисов, сети, RAID и инфраструктуры. */
(function () {
  'use strict';
  if (!window.DATA || window.DATA.expansionLoaded) return;
  const P = DATA.PARTS;
  const T = DATA.SERVER_TEMPLATES;
  const OS = DATA.OS_LIST;
  const APPS = DATA.SOFTWARE;
  const addPart = part => { if (!DATA.part(part.id)) P.push(part); };
  const cpu = (id, name, socket, cores, tdp, performance, value, brand = 'intel') => addPart({
    id, type: 'cpu', name, short: `${cores} ядер · ${tdp}W`, icon: 'cpu', value,
    socket, cores, tdp, performance, brand
  });
  const ram = (id, ramType, capacity, watts, value) => addPart({
    id, type: 'ram', name: `${ramType} ${capacity} ГБ ${ramType.includes('DDR5') ? 'ECC REG' : 'ECC'}`, short: `${capacity} ГБ · ${ramType}`, icon: 'ram', value, ramType, capacity, watts, brand: 'samsung'
  });
  const gpu = (id, name, tdp, vram, compute, value, brand = 'nvidia') => addPart({
    id, type: 'gpu', name, short: `${vram} ГБ VRAM · ${tdp}W`, icon: 'gpu', value, tdp, vram, compute, watts: tdp, brand
  });
  const chassis = (id, name, brand, socket, ramType, ramSlots, sataPorts, basePower, baseNoise, coolingFactor, value, pcieSlots = 1, icon = 'server') => addPart({
    id, type: 'chassis', name, short: `${name.split(' ').slice(-2).join(' ')} · ${socket}`, icon, value,
    socket, ramType, ramSlots, sataPorts, basePower, baseNoise, coolingFactor, pcieSlots, brand
  });

  // Существующие модели получают PCIe и брендинг, старые сохранения остаются совместимыми.
  P.filter(part => part.type === 'chassis').forEach(part => {
    part.brand = part.brand || deviceBrand(part.name);
    if (!Number.isFinite(part.pcieSlots)) part.pcieSlots = part.value >= 30000 ? 2 : 1;
  });
  const brandByOs = { debian: 'debian', windows: 'windows', truenas: 'truenas', proxmox: 'proxmox' };
  const familyByOs = { debian: 'linux', windows: 'windows', truenas: 'nas', proxmox: 'hypervisor' };
  OS.forEach(item => { item.brand = brandByOs[item.id]; item.family = familyByOs[item.id]; });
  const brandByApp = { jellyfin: 'jellyfin', filebrowser: 'files', boinc: 'boinc', localai: 'ollama' };
  APPS.forEach(item => { item.brand = brandByApp[item.id]; });

  // Дополнительная память, процессоры, ускорители, диски, питание и охлаждение.
  ram('ddr2-1', 'DDR2', 1, 4, 250);
  ram('ddr3-32', 'DDR3', 32, 10, 10500);
  ram('ddr4-128', 'DDR4', 128, 15, 42000);
  ram('ddr5-16', 'DDR5', 16, 8, 7500);
  ram('ddr5-32', 'DDR5', 32, 10, 13500);
  ram('ddr5-64', 'DDR5', 64, 13, 26000);
  ram('ddr5-128', 'DDR5', 128, 18, 59000);
  ram('ddr5-256', 'DDR5', 256, 24, 139000);
  ram('lpddr4-4', 'LPDDR4', 4, 2, 2800);
  ram('lpddr4-8', 'LPDDR4', 8, 2, 4600);

  cpu('x5355', 'Intel Xeon X5355', 'LGA771', 4, 120, 1.5, 1800);
  cpu('x5460', 'Intel Xeon X5460', 'LGA771', 4, 120, 1.8, 2500);
  cpu('x5550', 'Intel Xeon X5550', 'LGA1366', 4, 95, 2.2, 2800);
  cpu('x5650', 'Intel Xeon X5650', 'LGA1366', 6, 95, 3.4, 3900);
  cpu('x5690', 'Intel Xeon X5690', 'LGA1366', 6, 130, 4.1, 6200);
  cpu('e52650v2', 'Intel Xeon E5-2650 v2', 'LGA2011', 8, 95, 4.4, 10500);
  cpu('e52680v2', 'Intel Xeon E5-2680 v2', 'LGA2011', 10, 115, 5.2, 14500);
  cpu('e52650v4', 'Intel Xeon E5-2650 v4', 'LGA2011-3', 12, 105, 6.1, 18500);
  cpu('e52680v4', 'Intel Xeon E5-2680 v4', 'LGA2011-3', 14, 120, 7.2, 24000);
  cpu('e52699v4', 'Intel Xeon E5-2699 v4', 'LGA2011-3', 22, 145, 10.5, 43000);
  cpu('silver4110', 'Intel Xeon Silver 4110', 'LGA3647', 8, 85, 5.5, 15500);
  cpu('gold6130', 'Intel Xeon Gold 6130', 'LGA3647', 16, 125, 10.2, 39000);
  cpu('gold6248', 'Intel Xeon Gold 6248', 'LGA3647', 20, 150, 13.5, 68000);
  cpu('platinum8280', 'Intel Xeon Platinum 8280', 'LGA3647', 28, 205, 18.5, 125000);
  cpu('silver4310', 'Intel Xeon Silver 4310', 'LGA4189', 12, 120, 8.5, 39000);
  cpu('gold6338', 'Intel Xeon Gold 6338', 'LGA4189', 32, 205, 22.0, 145000);
  cpu('platinum8380', 'Intel Xeon Platinum 8380', 'LGA4189', 40, 270, 28.0, 245000);
  cpu('gold6430', 'Intel Xeon Gold 6430', 'LGA4677', 32, 270, 24.0, 195000);
  cpu('platinum8480', 'Intel Xeon Platinum 8480+', 'LGA4677', 56, 350, 39.0, 520000);
  cpu('epyc7302', 'AMD EPYC 7302', 'SP3', 16, 155, 12.0, 42000, 'amd');
  cpu('epyc7542', 'AMD EPYC 7542', 'SP3', 32, 225, 23.5, 115000, 'amd');
  cpu('epyc7742', 'AMD EPYC 7742', 'SP3', 64, 225, 43.0, 240000, 'amd');
  cpu('epyc7h12', 'AMD EPYC 7H12', 'SP3', 64, 280, 47.0, 315000, 'amd');
  cpu('epyc9124', 'AMD EPYC 9124', 'SP5', 16, 200, 16.0, 95000, 'amd');
  cpu('epyc9354', 'AMD EPYC 9354', 'SP5', 32, 280, 31.0, 215000, 'amd');
  cpu('epyc9654', 'AMD EPYC 9654', 'SP5', 96, 360, 78.0, 610000, 'amd');
  cpu('epyc9754', 'AMD EPYC 9754', 'SP5', 128, 360, 96.0, 890000, 'amd');
  cpu('ryzen3600', 'AMD Ryzen 5 3600', 'AM4', 6, 65, 5.2, 8500, 'amd');
  cpu('ryzen5950', 'AMD Ryzen 9 5950X', 'AM4', 16, 105, 15.0, 32000, 'amd');
  cpu('ryzen7950', 'AMD Ryzen 9 7950X', 'AM5', 16, 170, 20.0, 52000, 'amd');
  cpu('ryzen9950', 'AMD Ryzen 9 9950X', 'AM5', 16, 170, 23.0, 69000, 'amd');
  cpu('i512400', 'Intel Core i5-12400', 'LGA1700', 6, 65, 6.5, 14500);
  cpu('i912900', 'Intel Core i9-12900K', 'LGA1700', 16, 125, 15.5, 34000);
  cpu('i914900', 'Intel Core i9-14900K', 'LGA1700', 24, 125, 22.5, 51000);
  cpu('e2224', 'Intel Xeon E-2224', 'LGA1151v2', 4, 71, 4.2, 13500);
  cpu('x3216', 'AMD Opteron X3216', 'FT3', 2, 15, 1.2, 4200, 'amd');
  cpu('tr3975', 'AMD Threadripper PRO 3975WX', 'sWRX8', 32, 280, 30.0, 165000, 'amd');
  cpu('tr5995', 'AMD Threadripper PRO 5995WX', 'sWRX8', 64, 280, 57.0, 390000, 'amd');
  cpu('atomc2538', 'Intel Atom C2538', 'BGA', 4, 15, 1.4, 3500);
  cpu('atomc3538', 'Intel Atom C3538', 'BGA', 4, 15, 1.8, 4800);
  cpu('xeond1541', 'Intel Xeon D-1541', 'BGA', 8, 45, 5.8, 15000);
  cpu('celeronj3455', 'Intel Celeron J3455', 'BGA', 4, 10, 1.3, 2500);
  cpu('cortexa76', 'ARM Cortex-A76 BCM2712', 'ARM', 4, 10, 2.8, 6000, 'amd');
  cpu('power9', 'IBM POWER9 Sforza', 'P9', 16, 190, 15.0, 95000, 'ibm');
  cpu('sparc_m8', 'Oracle SPARC M8', 'SPARC', 32, 240, 22.0, 180000, 'oracle');

  gpu('tesla_k80', 'NVIDIA Tesla K80 24GB', 300, 24, 8, 18000);
  gpu('tesla_p40', 'NVIDIA Tesla P40 24GB', 250, 24, 12, 42000);
  gpu('tesla_p100', 'NVIDIA Tesla P100 16GB', 250, 16, 20, 69000);
  gpu('tesla_v100', 'NVIDIA Tesla V100 32GB', 300, 32, 30, 145000);
  gpu('tesla_a100', 'NVIDIA A100 80GB SXM', 400, 80, 42, 690000);
  gpu('tesla_h100', 'NVIDIA H100 80GB SXM', 700, 80, 85, 1950000);
  gpu('rtx3060', 'NVIDIA GeForce RTX 3060 12GB', 170, 12, 13, 28000);
  gpu('rtx4090', 'NVIDIA GeForce RTX 4090 24GB', 450, 24, 52, 165000);
  gpu('l40s', 'NVIDIA L40S 48GB', 350, 48, 55, 590000);
  gpu('mi50', 'AMD Instinct MI50 32GB', 300, 32, 20, 65000, 'amd');
  gpu('mi100', 'AMD Instinct MI100 32GB', 300, 32, 30, 125000, 'amd');
  gpu('mi250', 'AMD Instinct MI250X 128GB', 560, 128, 65, 690000, 'amd');

  [
    ['hdd10000', 'hdd', 'HDD 10 ТБ Enterprise', 10000, 10, 1.2, 15500],
    ['hdd18000', 'hdd', 'HDD 18 ТБ Helium', 18000, 9, 1.25, 28500],
    ['hdd22000', 'hdd', 'HDD 22 ТБ Exos', 22000, 9, 1.3, 39000],
    ['ssd8000', 'ssd', 'SSD 8 ТБ NVMe U.2', 8000, 12, 4.2, 65000],
    ['ssd16000', 'ssd', 'SSD 16 ТБ NVMe U.2', 16000, 16, 4.5, 145000],
    ['ssd30000', 'ssd', 'SSD 30 ТБ Datacenter', 30000, 18, 4.8, 310000]
  ].forEach(([id, type, name, capacity, watts, speed, value]) => addPart({ id, type, name, short: `${capacity / 1000} ТБ · ${speed} GB/s`, icon: type, value, capacity, watts, speed, brand: 'seagate' }));
  [
    ['cool40', 'Бесшумный кулер 40W', 40, -4, 900],
    ['cool450', 'Два башенных кулера 450W', 450, 8, 22000],
    ['cool700', 'Промышленная СЖО 700W', 700, 4, 59000],
    ['cool1000', 'Immersion Cooling 1000W', 1000, -8, 145000],
    ['cool1600', 'Immersion Cooling 1600W', 1600, -6, 240000],
    ['cool2400', 'Direct-to-Chip Liquid 2400W', 2400, -4, 390000]
  ].forEach(([id, name, cooling, noise, value]) => addPart({ id, type: 'cooler', name, short: `TDP до ${cooling} Вт`, icon: 'cooler', value, cooling, noise }));
  [
    ['psu1300', 1300, .94, 7, 14500], ['psu1600', 1600, .95, 9, 23000],
    ['psu2000', 2000, .96, 11, 39000], ['psu2400', 2400, .96, 12, 52000],
    ['psu3000', 3000, .97, 14, 85000], ['psu4000', 4000, .97, 16, 125000],
    ['psu5000', 5000, .97, 18, 175000], ['psu6000', 6000, .98, 20, 240000],
    ['psu8000', 8000, .98, 22, 360000]
  ].forEach(([id, watts, efficiency, noise, value]) => addPart({ id, type: 'psu', name: `БП ${watts}W Titanium`, short: `${watts} Вт · ${Math.round(efficiency * 100)}%`, icon: 'psu', value, watts, efficiency, noise }));

  const coolerFor = tdp => tdp <= 40 ? 'cool40' : tdp <= 75 ? 'cool75' : tdp <= 130 ? 'cool130' : tdp <= 200 ? 'cool200' : tdp <= 320 ? 'cool320' : tdp <= 450 ? 'cool450' : tdp <= 700 ? 'cool700' : tdp <= 1000 ? 'cool1000' : tdp <= 1600 ? 'cool1600' : 'cool2400';
  const psuFor = needed => ['psu300', 'psu500', 'psu750', 'psu1100', 'psu1300', 'psu1600', 'psu2000', 'psu2400', 'psu3000', 'psu4000', 'psu5000', 'psu6000', 'psu8000'].find(id => DATA.part(id).watts >= needed) || 'psu8000';
  function server(id, name, brand, socket, ramType, ramSlots, sata, power, noise, cooling, value, cpuId, ramId, ramCount, disks, options = {}) {
    chassis(id, name, brand, socket, ramType, ramSlots, sata, power, noise, cooling, value, Number.isFinite(options.pcieSlots) ? options.pcieSlots : 1, options.icon || 'server');
    const cpuPart = DATA.part(cpuId);
    const gpus = options.gpus || [];
    const gpuTdp = gpus.reduce((sum, gpuId) => sum + DATA.part(gpuId).tdp, 0);
    const needed = Math.round(70 + cpuPart.tdp + cpuPart.cores * 35 + disks.length * 10 + ramCount * 4 + gpuTdp * 1.25);
    const coolerId = options.cooler || coolerFor(cpuPart.tdp + gpuTdp * 0.35);
    const psuId = options.psu || psuFor(needed);
    const partIds = [id, cpuId, ...Array(ramCount).fill(ramId), ...disks, coolerId, psuId, ...gpus];
    const rawValue = partIds.reduce((sum, partId) => sum + DATA.part(partId).value, 0);
    T.push({
      id, name, chassis: id, cpu: cpuId, ram: Array(ramCount).fill(ramId), disks,
      cooler: coolerId, psu: psuId, gpus, basePrice: Math.round(rawValue * 1.18 / 100) * 100,
      weight: options.weight || 2, brand
    });
  }

  // HP / HPE
  server('dl360g5', 'HP ProLiant DL360 G5', 'hp', 'LGA771', 'DDR2', 8, 6, 35, 62, .94, 12000, 'x5355', 'ddr2-2', 4, ['hdd500']);
  server('dl380g6', 'HP ProLiant DL380 G6', 'hp', 'LGA1366', 'DDR3', 12, 8, 40, 66, .98, 19000, 'x5550', 'ddr3-4', 4, ['hdd500', 'hdd500']);
  server('dl380g7', 'HP ProLiant DL380 G7', 'hp', 'LGA1366', 'DDR3', 18, 8, 42, 68, 1.0, 24000, 'x5650', 'ddr3-8', 4, ['hdd1000', 'hdd1000']);
  server('dl380gen9', 'HP ProLiant DL380 Gen9', 'hp', 'LGA2011-3', 'DDR4', 24, 12, 50, 67, 1.05, 52000, 'e52680v4', 'ddr4-16', 4, ['hdd1000', 'ssd960']);
  server('dl380gen10', 'HPE ProLiant DL380 Gen10', 'hp', 'LGA3647', 'DDR4', 24, 12, 58, 65, 1.1, 95000, 'gold6130', 'ddr4-32', 4, ['ssd960', 'hdd4000'], { pcieSlots: 3 });
  server('dl380gen10plus', 'HPE ProLiant DL380 Gen10 Plus', 'hp', 'LGA4189', 'DDR4', 32, 12, 62, 64, 1.14, 155000, 'gold6338', 'ddr4-32', 8, ['ssd4000', 'ssd4000'], { pcieSlots: 4, weight: 1 });
  server('dl380gen11', 'HPE ProLiant DL380 Gen11', 'hp', 'LGA4677', 'DDR5', 32, 16, 70, 61, 1.2, 290000, 'gold6430', 'ddr5-32', 8, ['ssd8000', 'ssd8000'], { pcieSlots: 4, weight: 1 });
  server('ml350gen9', 'HP ProLiant ML350 Gen9', 'hp', 'LGA2011-3', 'DDR4', 24, 12, 42, 43, 1.12, 49000, 'e52650v4', 'ddr4-16', 4, ['hdd4000', 'hdd4000'], { icon: 'chassis' });
  server('microgen10', 'HP MicroServer Gen10', 'hp', 'FT3', 'DDR4', 2, 4, 12, 25, 1.0, 18000, 'x3216', 'ddr4-8', 2, ['hdd1000'], { icon: 'chassis' });
  server('microgen10plus', 'HPE MicroServer Gen10 Plus', 'hp', 'LGA1151v2', 'DDR4', 4, 4, 18, 27, 1.02, 39000, 'e2224', 'ddr4-16', 2, ['hdd4000'], { icon: 'chassis' });
  server('apollo4200', 'HPE Apollo 4200 Gen10', 'hp', 'LGA3647', 'DDR4', 24, 24, 65, 68, 1.16, 125000, 'gold6248', 'ddr4-32', 4, ['hdd10000', 'hdd10000', 'hdd10000', 'hdd10000'], { pcieSlots: 3 });
  server('apollo6500', 'HPE Apollo 6500 Gen10 Plus', 'hp', 'LGA4189', 'DDR4', 32, 10, 85, 76, 1.3, 240000, 'platinum8380', 'ddr4-64', 8, ['ssd8000', 'ssd8000'], { pcieSlots: 4, gpus: ['tesla_a100', 'tesla_a100', 'tesla_a100', 'tesla_a100'], psu: 'psu4000', cooler: 'cool1600', weight: 1 });

  // Dell EMC
  server('r610', 'Dell PowerEdge R610', 'dell', 'LGA1366', 'DDR3', 12, 6, 38, 69, .98, 15000, 'x5550', 'ddr3-4', 4, ['hdd500']);
  server('r710', 'Dell PowerEdge R710', 'dell', 'LGA1366', 'DDR3', 18, 8, 43, 72, 1.0, 21000, 'x5650', 'ddr3-8', 4, ['hdd1000', 'hdd1000']);
  server('r730', 'Dell PowerEdge R730', 'dell', 'LGA2011-3', 'DDR4', 24, 12, 52, 70, 1.08, 59000, 'e52650v4', 'ddr4-16', 4, ['hdd4000', 'ssd960']);
  server('r740', 'Dell PowerEdge R740', 'dell', 'LGA3647', 'DDR4', 24, 16, 60, 68, 1.12, 105000, 'gold6248', 'ddr4-32', 4, ['ssd4000', 'hdd4000'], { pcieSlots: 3 });
  server('r750', 'Dell PowerEdge R750', 'dell', 'LGA4189', 'DDR4', 32, 16, 68, 66, 1.16, 175000, 'gold6338', 'ddr4-32', 8, ['ssd8000', 'ssd8000'], { pcieSlots: 4, weight: 1 });
  server('r760', 'Dell PowerEdge R760', 'dell', 'LGA4677', 'DDR5', 32, 24, 75, 64, 1.22, 310000, 'platinum8480', 'ddr5-64', 8, ['ssd16000', 'ssd16000'], { pcieSlots: 4, weight: 1 });
  server('r640', 'Dell PowerEdge R640', 'dell', 'LGA3647', 'DDR4', 24, 10, 52, 66, 1.08, 89000, 'silver4110', 'ddr4-16', 4, ['ssd960']);
  server('r650', 'Dell PowerEdge R650', 'dell', 'LGA4189', 'DDR4', 32, 10, 60, 65, 1.12, 145000, 'silver4310', 'ddr4-32', 4, ['ssd4000']);
  server('r660', 'Dell PowerEdge R660', 'dell', 'LGA4677', 'DDR5', 32, 12, 66, 63, 1.18, 260000, 'gold6430', 'ddr5-32', 4, ['ssd8000']);
  server('t620', 'Dell PowerEdge T620', 'dell', 'LGA2011', 'DDR3', 24, 12, 40, 46, 1.1, 32000, 'e52650v2', 'ddr3-8', 4, ['hdd4000'], { icon: 'chassis' });
  server('t640', 'Dell PowerEdge T640', 'dell', 'LGA3647', 'DDR4', 24, 18, 48, 45, 1.15, 85000, 'gold6130', 'ddr4-32', 4, ['hdd10000', 'hdd10000'], { icon: 'chassis', pcieSlots: 3 });
  server('t760', 'Dell PowerEdge T760', 'dell', 'LGA4677', 'DDR5', 32, 18, 62, 43, 1.24, 260000, 'gold6430', 'ddr5-64', 4, ['ssd8000', 'hdd18000'], { icon: 'chassis', pcieSlots: 4 });
  server('xe8545', 'Dell PowerEdge XE8545', 'dell', 'SP3', 'DDR4', 32, 10, 90, 78, 1.35, 270000, 'epyc7742', 'ddr4-64', 8, ['ssd8000', 'ssd8000'], { pcieSlots: 4, gpus: ['tesla_a100', 'tesla_a100', 'tesla_a100', 'tesla_a100'], psu: 'psu5000', cooler: 'cool1600', weight: 1 });

  // IBM / Lenovo
  server('x3650m3', 'IBM System x3650 M3', 'ibm', 'LGA1366', 'DDR3', 18, 8, 42, 67, 1.0, 20000, 'x5650', 'ddr3-8', 4, ['hdd1000']);
  server('x3650m4', 'IBM System x3650 M4', 'ibm', 'LGA2011', 'DDR3', 24, 8, 46, 65, 1.03, 35000, 'e52680v2', 'ddr3-16', 4, ['hdd1000', 'ssd960']);
  server('x3650m5', 'Lenovo System x3650 M5', 'lenovo', 'LGA2011-3', 'DDR4', 24, 12, 50, 64, 1.07, 56000, 'e52680v4', 'ddr4-16', 4, ['ssd960']);
  server('sr650', 'Lenovo ThinkSystem SR650', 'lenovo', 'LGA3647', 'DDR4', 24, 16, 58, 63, 1.12, 99000, 'gold6248', 'ddr4-32', 4, ['ssd4000']);
  server('sr650v2', 'Lenovo ThinkSystem SR650 V2', 'lenovo', 'LGA4189', 'DDR4', 32, 16, 65, 62, 1.16, 160000, 'gold6338', 'ddr4-32', 8, ['ssd8000']);
  server('sr650v3', 'Lenovo ThinkSystem SR650 V3', 'lenovo', 'LGA4677', 'DDR5', 32, 20, 72, 60, 1.22, 295000, 'platinum8480', 'ddr5-64', 8, ['ssd16000'], { weight: 1 });
  server('sr665', 'Lenovo ThinkSystem SR665', 'lenovo', 'SP3', 'DDR4', 32, 16, 68, 64, 1.2, 175000, 'epyc7542', 'ddr4-64', 8, ['ssd8000'], { pcieSlots: 4 });
  server('sr675v3', 'Lenovo ThinkSystem SR675 V3', 'lenovo', 'SP5', 'DDR5', 48, 12, 95, 77, 1.4, 420000, 'epyc9654', 'ddr5-128', 8, ['ssd16000', 'ssd16000'], { pcieSlots: 4, gpus: ['tesla_h100', 'tesla_h100', 'tesla_h100', 'tesla_h100'], psu: 'psu8000', cooler: 'cool2400', weight: 1 });

  // Supermicro
  server('sm6015', 'Supermicro SuperServer 6015', 'supermicro', 'LGA771', 'DDR2', 8, 4, 35, 67, .96, 11000, 'x5460', 'ddr2-4', 4, ['hdd500']);
  server('sm6027', 'Supermicro SuperServer 6027', 'supermicro', 'LGA2011', 'DDR3', 24, 8, 45, 65, 1.04, 34000, 'e52690v2', 'ddr3-16', 4, ['hdd1000']);
  server('sm6039', 'Supermicro SuperServer 6039', 'supermicro', 'LGA2011-3', 'DDR4', 24, 12, 50, 62, 1.1, 59000, 'e52699v4', 'ddr4-32', 4, ['ssd4000']);
  server('sm1029', 'Supermicro SuperServer 1029', 'supermicro', 'LGA3647', 'DDR4', 24, 10, 55, 61, 1.12, 85000, 'gold6130', 'ddr4-32', 4, ['ssd4000']);
  server('sm2029', 'Supermicro SuperServer 2029', 'supermicro', 'LGA3647', 'DDR4', 24, 12, 60, 63, 1.15, 105000, 'platinum8280', 'ddr4-64', 4, ['ssd8000']);
  server('sm1024', 'Supermicro SuperServer 1024', 'supermicro', 'LGA4189', 'DDR4', 32, 10, 62, 60, 1.16, 145000, 'gold6338', 'ddr4-64', 4, ['ssd8000']);
  server('sm2024', 'Supermicro A+ Server 2024', 'supermicro', 'SP3', 'DDR4', 32, 12, 70, 62, 1.22, 180000, 'epyc7742', 'ddr4-64', 8, ['ssd8000']);
  server('sm1025', 'Supermicro SuperServer 1025', 'supermicro', 'LGA4677', 'DDR5', 32, 12, 70, 58, 1.22, 260000, 'gold6430', 'ddr5-64', 4, ['ssd16000']);
  server('sm2025', 'Supermicro A+ Server 2025', 'supermicro', 'SP5', 'DDR5', 48, 16, 82, 60, 1.3, 390000, 'epyc9754', 'ddr5-128', 8, ['ssd16000', 'ssd16000'], { weight: 1 });
  server('sm7049', 'Supermicro SuperWorkstation 7049', 'supermicro', 'LGA3647', 'DDR4', 24, 8, 48, 42, 1.2, 95000, 'gold6248', 'ddr4-32', 4, ['ssd4000'], { icon: 'chassis', pcieSlots: 4, gpus: ['rtx4090'] });
  server('as4124', 'Supermicro AS-4124GS-TNR', 'supermicro', 'SP3', 'DDR4', 32, 12, 92, 78, 1.4, 285000, 'epyc7h12', 'ddr4-128', 8, ['ssd16000', 'ssd16000'], { pcieSlots: 4, gpus: ['tesla_a100', 'tesla_a100', 'tesla_a100', 'tesla_a100'], psu: 'psu5000', cooler: 'cool1600', weight: 1 });

  // Cisco, Fujitsu, Huawei, Inspur, Oracle / Sun
  server('c220m3', 'Cisco UCS C220 M3', 'cisco', 'LGA2011', 'DDR3', 16, 8, 43, 66, 1.02, 30000, 'e52650v2', 'ddr3-8', 4, ['hdd1000']);
  server('c240m4', 'Cisco UCS C240 M4', 'cisco', 'LGA2011-3', 'DDR4', 24, 12, 52, 65, 1.08, 58000, 'e52680v4', 'ddr4-32', 4, ['ssd4000']);
  server('c220m5', 'Cisco UCS C220 M5', 'cisco', 'LGA3647', 'DDR4', 24, 10, 55, 62, 1.12, 92000, 'gold6248', 'ddr4-32', 4, ['ssd4000']);
  server('c240m6', 'Cisco UCS C240 M6', 'cisco', 'LGA4189', 'DDR4', 32, 16, 66, 61, 1.17, 165000, 'gold6338', 'ddr4-64', 4, ['ssd8000']);
  server('rx200s7', 'Fujitsu PRIMERGY RX200 S7', 'fujitsu', 'LGA2011', 'DDR3', 24, 8, 42, 63, 1.03, 31000, 'e52650v2', 'ddr3-8', 4, ['hdd1000']);
  server('rx300s8', 'Fujitsu PRIMERGY RX300 S8', 'fujitsu', 'LGA2011-3', 'DDR4', 24, 12, 50, 62, 1.08, 54000, 'e52650v4', 'ddr4-16', 4, ['ssd960']);
  server('rx2540m5', 'Fujitsu PRIMERGY RX2540 M5', 'fujitsu', 'LGA3647', 'DDR4', 24, 16, 58, 61, 1.13, 96000, 'gold6248', 'ddr4-32', 4, ['ssd4000']);
  server('rx2540m7', 'Fujitsu PRIMERGY RX2540 M7', 'fujitsu', 'LGA4677', 'DDR5', 32, 20, 72, 58, 1.22, 285000, 'gold6430', 'ddr5-64', 4, ['ssd16000']);
  server('rh2288v3', 'Huawei FusionServer RH2288 V3', 'huawei', 'LGA2011-3', 'DDR4', 24, 12, 50, 64, 1.08, 52000, 'e52680v4', 'ddr4-32', 4, ['ssd4000']);
  server('rh2288v5', 'Huawei FusionServer RH2288 V5', 'huawei', 'LGA3647', 'DDR4', 24, 16, 60, 62, 1.14, 94000, 'gold6248', 'ddr4-32', 4, ['ssd8000']);
  server('2288hv6', 'Huawei FusionServer 2288H V6', 'huawei', 'LGA4189', 'DDR4', 32, 16, 65, 60, 1.18, 155000, 'gold6338', 'ddr4-64', 4, ['ssd8000']);
  server('nf5280m5', 'Inspur NF5280M5', 'inspur', 'LGA3647', 'DDR4', 24, 16, 60, 63, 1.14, 88000, 'gold6130', 'ddr4-32', 4, ['ssd4000']);
  server('nf5280m6', 'Inspur NF5280M6', 'inspur', 'LGA4189', 'DDR4', 32, 16, 65, 61, 1.18, 150000, 'gold6338', 'ddr4-64', 4, ['ssd8000']);
  server('sunx4150', 'Oracle Sun Fire X4150', 'oracle', 'LGA771', 'DDR2', 8, 4, 36, 70, .95, 13500, 'x5460', 'ddr2-4', 4, ['hdd500']);
  server('sunx4170m3', 'Oracle Sun Fire X4170 M3', 'oracle', 'LGA2011', 'DDR3', 18, 8, 44, 67, 1.02, 32000, 'e52680v2', 'ddr3-16', 4, ['hdd1000']);
  server('serverx62', 'Oracle Server X6-2', 'oracle', 'LGA2011-3', 'DDR4', 24, 8, 50, 63, 1.1, 62000, 'e52699v4', 'ddr4-32', 4, ['ssd4000']);
  server('serverx82', 'Oracle Server X8-2', 'oracle', 'LGA3647', 'DDR4', 24, 8, 62, 61, 1.16, 115000, 'platinum8280', 'ddr4-64', 4, ['ssd8000']);

  // NAS, самосбор, ARM и альтернативные архитектуры.
  server('rs814', 'Synology RackStation RS814', 'synology', 'BGA', 'DDR3', 2, 4, 14, 32, 1.0, 28000, 'atomc2538', 'ddr3-2', 2, ['hdd1000', 'hdd1000'], { pcieSlots: 0 });
  server('rs1219', 'Synology RackStation RS1219+', 'synology', 'BGA', 'DDR3', 2, 8, 16, 34, 1.0, 52000, 'atomc2538', 'ddr3-4', 2, ['hdd4000', 'hdd4000'], { pcieSlots: 0 });
  server('rs2418', 'Synology RackStation RS2418+', 'synology', 'BGA', 'DDR4', 4, 12, 22, 36, 1.02, 85000, 'atomc3538', 'ddr4-8', 2, ['hdd10000', 'hdd10000'], { pcieSlots: 0 });
  server('rs3621', 'Synology RackStation RS3621xs+', 'synology', 'BGA', 'DDR4', 4, 12, 35, 40, 1.06, 165000, 'xeond1541', 'ddr4-16', 4, ['hdd18000', 'hdd18000'], { pcieSlots: 1 });
  server('rs4021', 'Synology RackStation RS4021xs+', 'synology', 'BGA', 'DDR4', 4, 16, 38, 41, 1.08, 220000, 'xeond1541', 'ddr4-32', 4, ['hdd22000', 'hdd22000'], { pcieSlots: 1 });
  server('qnap453', 'QNAP TS-453D', 'qnap', 'BGA', 'DDR4', 2, 4, 16, 28, 1.0, 42000, 'celeronj3455', 'ddr4-8', 2, ['hdd4000', 'hdd4000'], { pcieSlots: 0, icon: 'chassis' });
  server('qnap873', 'QNAP TVS-873', 'qnap', 'AM4', 'DDR4', 4, 8, 28, 33, 1.05, 85000, 'ryzen3600', 'ddr4-16', 2, ['hdd10000', 'ssd960'], { pcieSlots: 2, icon: 'chassis' });
  server('qnaph874', 'QNAP TVS-h874', 'qnap', 'LGA1700', 'DDR4', 4, 8, 35, 34, 1.08, 155000, 'i512400', 'ddr4-32', 2, ['hdd18000', 'ssd4000'], { pcieSlots: 2, icon: 'chassis' });
  server('customam4', 'Custom Ryzen Server AM4', 'amd', 'AM4', 'DDR4', 4, 8, 26, 30, 1.15, 18000, 'ryzen5950', 'ddr4-32', 4, ['ssd4000'], { icon: 'chassis', pcieSlots: 3, gpus: ['rtx3060'] });
  server('customam5', 'Custom Ryzen Server AM5', 'amd', 'AM5', 'DDR5', 4, 8, 34, 31, 1.2, 32000, 'ryzen9950', 'ddr5-64', 4, ['ssd8000'], { icon: 'chassis', pcieSlots: 3, gpus: ['rtx4090'] });
  server('customtr', 'Custom Threadripper PRO', 'amd', 'sWRX8', 'DDR4', 8, 10, 52, 38, 1.3, 95000, 'tr5995', 'ddr4-128', 8, ['ssd16000'], { icon: 'chassis', pcieSlots: 4, gpus: ['rtx4090', 'rtx4090'] });
  server('rpi5', 'Raspberry Pi 5 Cluster Node', 'raspberrypi', 'ARM', 'LPDDR4', 1, 2, 5, 18, 1.1, 6500, 'cortexa76', 'lpddr4-8', 1, ['ssd240'], { pcieSlots: 0, icon: 'chassis', cooler: 'cool40', psu: 'psu300' });
  server('s922', 'IBM Power System S922', 'ibm', 'P9', 'DDR4', 32, 12, 70, 65, 1.22, 210000, 'power9', 'ddr4-64', 8, ['ssd8000'], { pcieSlots: 4 });
  server('sparcm8', 'Oracle SPARC M8-8', 'oracle', 'SPARC', 'DDR4', 64, 16, 85, 66, 1.25, 340000, 'sparc_m8', 'ddr4-128', 8, ['ssd16000'], { pcieSlots: 4 });

  // Операционные системы.
  const addOS = (id, name, brand, family, price, minRam, minDisk, baseRam, baseCpu, description) => OS.push({
    id, name, brand, family, icon: 'os', price, minRam, minDisk, baseRam, baseCpu, description,
    terminal: `${id}: install -- unattended`
  });
  addOS('ubuntu', 'Ubuntu Server 24.04', 'ubuntu', 'linux', 0, 2, 25, .8, 3, 'LTS · пакеты APT · Snap');
  addOS('debian13', 'Debian 13', 'debian', 'linux', 0, 1, 20, .55, 2, 'Stable · новое ядро · APT');
  addOS('rocky', 'Rocky Linux 9', 'rockylinux', 'linux', 0, 2, 25, 1, 4, 'RHEL-compatible · DNF');
  addOS('almalinux', 'AlmaLinux 9', 'almalinux', 'linux', 0, 2, 25, 1, 4, 'Enterprise · DNF · SELinux');
  addOS('fedora', 'Fedora Server', 'redhat', 'linux', 0, 2, 25, 1.1, 4, 'Свежие пакеты · DNF');
  addOS('opensuse', 'openSUSE Leap', 'suse', 'linux', 0, 2, 30, 1.2, 4, 'YaST · Btrfs · Snapper');
  addOS('nixos', 'NixOS 25.05', 'nixos', 'linux', 0, 2, 30, .9, 3, 'Декларативно · Nix flakes');
  addOS('arch', 'Arch Linux', 'archlinux', 'linux', 0, 1, 20, .6, 3, 'Rolling release · pacman');
  addOS('freebsd', 'FreeBSD 14', 'freebsd', 'bsd', 0, 2, 25, .8, 3, 'ZFS · Jails · pf');
  addOS('omv', 'OpenMediaVault 7', 'openmediavault', 'nas', 0, 4, 32, 1.8, 4, 'NAS · SMB · ZFS · Docker');
  addOS('unraid', 'Unraid OS', 'unraid', 'nas', 3900, 4, 32, 2, 5, 'Array · Docker · VM · лицензия');
  addOS('pfsense', 'pfSense CE', 'pfsense', 'network', 0, 2, 16, .7, 3, 'Firewall · NAT · VPN');
  addOS('openwrt', 'OpenWrt', 'openwrt', 'network', 0, 1, 8, .35, 2, 'Router · nftables · LuCI');
  addOS('esxi', 'VMware ESXi 8', 'vmware', 'hypervisor', 0, 8, 64, 3.5, 7, 'vSphere · VMFS · виртуализация');
  addOS('xcpng', 'XCP-ng 8.3', 'vmware', 'hypervisor', 0, 4, 48, 2, 5, 'Xen · XO · виртуализация');
  addOS('windows2025', 'Windows Server 2025', 'windows', 'windows', 11500, 8, 80, 3.2, 9, 'GUI · Hyper-V · лицензия');
  addOS('solaris', 'Oracle Solaris 11', 'oracle', 'unix', 0, 8, 64, 3, 6, 'SPARC/x86 · ZFS · Zones');

  const LINUX = ['debian', 'debian13', 'ubuntu', 'rocky', 'almalinux', 'fedora', 'opensuse', 'nixos', 'arch', 'proxmox', 'xcpng'];
  const NAS = ['truenas', 'omv', 'unraid'];
  const WINDOWS = ['windows', 'windows2025'];
  const VIRT = [...LINUX, 'esxi'];
  const ALL_UNIX = [...LINUX, ...NAS, 'freebsd', 'solaris'];
  // Старые сервисы получают поддержку новых дистрибутивов.
  APPS.forEach(app => {
    if (app.os.includes('debian')) app.os = [...new Set([...app.os, ...LINUX])];
    if (app.os.includes('windows')) app.os = [...new Set([...app.os, ...WINDOWS])];
    if (app.id === 'filebrowser' || app.id === 'jellyfin') app.os = [...new Set([...app.os, ...NAS, 'freebsd'])];
    if (app.id === 'localai') { app.brand = 'ollama'; app.gpuBoost = true; }
  });

  function app(id, name, brand, icon, ram, cpuLoad, rate, minRam, os, description, options = {}) {
    APPS.push(Object.assign({ id, name, brand, icon, ram, cpu: cpuLoad, rate, minRam, os, description }, options));
  }
  app('plex', 'Plex Media Server', 'plex', 'jellyfin', 3, 25, 95, 4, [...LINUX, ...NAS, ...WINDOWS], 'Медиа · транскодинг · +95 ₽/ч');
  app('nextcloud', 'Nextcloud', 'nextcloud', 'files', 4, 25, 150, 8, [...LINUX, ...NAS], 'Облако · файлы · +150 ₽/ч');
  app('docker', 'Docker Engine', 'docker', 'apps', 1.5, 10, 60, 2, VIRT, 'Контейнеры · +60 ₽/ч');
  app('portainer', 'Portainer CE', 'docker', 'apps', .7, 4, 45, 1, VIRT, 'Docker UI · +45 ₽/ч');
  app('homeassistant', 'Home Assistant', 'homeassistant', 'apps', 1.2, 8, 50, 2, [...LINUX, ...NAS], 'Умный дом · +50 ₽/ч');
  app('pihole', 'Pi-hole', 'pihole', 'apps', .5, 3, 35, 1, LINUX, 'DNS · фильтрация · +35 ₽/ч');
  app('wireguard', 'WireGuard VPN', 'wireguard', 'apps', .4, 4, 45, 1, [...LINUX, 'freebsd', 'pfsense', 'openwrt'], 'VPN · +45 ₽/ч');
  app('nginx', 'NGINX Gateway', 'nginx', 'apps', .5, 8, 55, 1, [...LINUX, 'freebsd'], 'Reverse proxy · +55 ₽/ч');
  app('minecraft', 'Minecraft Server', 'minecraft', 'apps', 4, 55, 220, 8, [...LINUX, ...WINDOWS], 'Игровой мир · +220 ₽/ч');
  app('valheim', 'Valheim Dedicated', 'server', 'apps', 3, 45, 170, 6, [...LINUX, ...WINDOWS], 'Co-op world · +170 ₽/ч');
  app('rustserver', 'Rust Dedicated', 'server', 'apps', 8, 80, 420, 16, [...LINUX, ...WINDOWS], 'Игровой сервер · +420 ₽/ч');
  app('factorio', 'Factorio Headless', 'server', 'apps', 2, 45, 145, 4, [...LINUX, ...WINDOWS], 'Фабрика · +145 ₽/ч');
  app('postgres', 'PostgreSQL', 'postgresql', 'apps', 3, 18, 140, 4, ALL_UNIX, 'SQL database · +140 ₽/ч');
  app('mysql', 'MySQL', 'mysql', 'apps', 2.5, 16, 125, 4, [...LINUX, ...WINDOWS], 'SQL database · +125 ₽/ч');
  app('redis', 'Redis', 'redis', 'apps', 2, 12, 115, 4, LINUX, 'In-memory cache · +115 ₽/ч');
  app('mongodb', 'MongoDB', 'mongodb', 'apps', 4, 22, 165, 8, [...LINUX, ...WINDOWS], 'NoSQL · +165 ₽/ч');
  app('gitlab', 'GitLab CE', 'gitlab', 'apps', 6, 35, 260, 12, LINUX, 'Git · CI/CD · +260 ₽/ч');
  app('gitea', 'Gitea', 'gitea', 'apps', 1.2, 8, 80, 2, [...LINUX, 'freebsd'], 'Git hosting · +80 ₽/ч');
  app('vaultwarden', 'Vaultwarden', 'vaultwarden', 'apps', .5, 3, 50, 1, LINUX, 'Passwords · +50 ₽/ч');
  app('immich', 'Immich Photos', 'nextcloud', 'files', 6, 45, 300, 16, [...LINUX, ...NAS], 'Фото · ML · +300 ₽/ч', { gpuBoost: true });
  app('photoprism', 'PhotoPrism', 'nextcloud', 'files', 4, 35, 220, 8, [...LINUX, ...NAS], 'Фотоархив · +220 ₽/ч');
  app('frigate', 'Frigate NVR', 'homeassistant', 'apps', 4, 55, 250, 8, LINUX, 'Видеонаблюдение · +250 ₽/ч', { gpuBoost: true });
  app('ollama', 'Ollama 70B', 'ollama', 'ai', 20, 90, 780, 32, [...LINUX, ...WINDOWS], 'LLM inference · +780 ₽/ч', { gpuBoost: true, coreRate: 4 });
  app('stablediffusion', 'Stable Diffusion XL', 'nvidia', 'ai', 10, 95, 850, 24, [...LINUX, ...WINDOWS], 'Image generation · +850 ₽/ч', { gpuBoost: true });
  app('k3s', 'K3s Cluster', 'kubernetes', 'apps', 3, 25, 160, 6, LINUX, 'Kubernetes lite · +160 ₽/ч');
  app('kubernetes', 'Kubernetes', 'kubernetes', 'apps', 6, 40, 300, 12, LINUX, 'Orchestration · +300 ₽/ч');
  app('folding', 'Folding@home', 'boinc', 'boinc', 1.5, 100, 55, 2, [...LINUX, ...WINDOWS], 'Наука · CPU/GPU · доход за ядра', { coreRate: 22, gpuBoost: true });
  app('monitoring', 'Prometheus + Grafana', 'stats', 'stats', 1.5, 7, 65, 2, LINUX, 'Мониторинг · +65 ₽/ч');
  app('teamspeak', 'TeamSpeak Server', 'server', 'apps', .5, 4, 45, 1, [...LINUX, ...WINDOWS], 'Voice · +45 ₽/ч');
  app('syncthing', 'Syncthing', 'files', 'files', 1, 8, 65, 2, [...ALL_UNIX, ...WINDOWS], 'P2P sync · +65 ₽/ч');
  app('qbittorrent', 'qBittorrent ISO', 'files', 'files', 1, 10, 60, 2, [...LINUX, ...NAS, ...WINDOWS], 'Раздача Linux ISO · +60 ₽/ч');
  app('virtuallab', 'Virtual Lab · 8 VM', 'vmware', 'apps', 12, 65, 480, 24, ['proxmox', 'esxi', 'xcpng', 'windows2025'], 'Аренда VM · +480 ₽/ч');

  DATA.NETWORKS = [
    { id: 'home100', name: 'Домашний 100 Мбит/с', speed: 100, daily: 35, price: 0, revenue: 1, reliability: .998, description: 'Dynamic IP · NAT' },
    { id: 'home1g', name: 'Гигабит от Ростелекома', speed: 1000, daily: 85, price: 5000, revenue: 1.08, reliability: .9985, description: '1 Гбит/с · static IP' },
    { id: 'business1g', name: 'Бизнес 1G SLA', speed: 1000, daily: 220, price: 18000, revenue: 1.18, reliability: .9995, description: 'SLA 99.95% · /29 subnet' },
    { id: 'dc10g', name: 'ЦОД Uplink 10G', speed: 10000, daily: 750, price: 65000, revenue: 1.38, reliability: .9998, description: '10 Гбит/с · BGP · DDoS filter' },
    { id: 'dc100g', name: 'Tier III Uplink 100G', speed: 100000, daily: 2800, price: 240000, revenue: 1.65, reliability: .99995, description: '100 Гбит/с · 2 провайдера' }
  ];
  DATA.RAIDS = {
    none: { id: 'none', name: 'Без RAID', minDisks: 0, speed: 1, description: 'Все диски отдельно' },
    raid0: { id: 'raid0', name: 'RAID 0 · Stripe', minDisks: 2, speed: 1.35, description: 'Максимум скорости, нет защиты' },
    raid1: { id: 'raid1', name: 'RAID 1 · Mirror', minDisks: 2, speed: 1.08, description: 'Зеркало · 50% ёмкости' },
    raid5: { id: 'raid5', name: 'RAID 5 · Parity', minDisks: 3, speed: 1.18, description: 'Паритет · ёмкость N−1' },
    raid6: { id: 'raid6', name: 'RAID 6 · Dual Parity', minDisks: 4, speed: 1.12, description: 'Двойной паритет · N−2' },
    raid10: { id: 'raid10', name: 'RAID 10', minDisks: 4, speed: 1.5, description: 'Зеркало + stripe · 50%' }
  };
  DATA.UPGRADES = {
    ac: { name: 'Кондиционер и вентиляция', icon: 'cooler', max: 3, prices: [15000, 42000, 95000], description: '−3°C за уровень · +45W' },
    pdu: { name: 'PDU и электропроводка', icon: 'bolt', max: 3, prices: [12000, 38000, 85000], description: '+1000W к лимиту за уровень' },
    ups: { name: 'ИБП сетевой стойки', icon: 'shield', max: 2, prices: [22000, 58000], description: 'Шанс пережить аварию и скачок' }
  };
  DATA.CONTRACT_TEMPLATES = [
    { title: 'Видеотека для кафе', requiredSoft: 'jellyfin', minRam: 4, minCores: 2, reward: 14000, duration: 2, xp: 250 },
    { title: 'Наука без сна', requiredSoft: 'boinc', minRam: 4, minCores: 4, reward: 18000, duration: 2, xp: 280 },
    { title: 'Файловое облако стартапа', requiredSoft: 'nextcloud', minRam: 8, minCores: 4, reward: 26000, duration: 3, xp: 400 },
    { title: 'Медиа под ключ', requiredSoft: 'plex', minRam: 8, minCores: 4, reward: 22000, duration: 2, xp: 330 },
    { title: 'Умный офис', requiredSoft: 'homeassistant', minRam: 4, minCores: 2, reward: 16000, duration: 2, xp: 260 },
    { title: 'Minecraft для школы', requiredSoft: 'minecraft', minRam: 8, minCores: 4, reward: 24000, duration: 3, xp: 390 },
    { title: 'Приватный Git', requiredSoft: 'gitea', minRam: 4, minCores: 2, reward: 19000, duration: 2, xp: 300 },
    { title: 'CI/CD для агентства', requiredSoft: 'gitlab', minRam: 12, minCores: 6, reward: 42000, duration: 3, xp: 550 },
    { title: 'Локальная нейросеть', requiredSoft: 'ollama', minRam: 32, minCores: 8, reward: 85000, duration: 3, xp: 900 },
    { title: 'Генерация рекламы', requiredSoft: 'stablediffusion', minRam: 24, minCores: 8, reward: 92000, duration: 3, xp: 950 },
    { title: 'Kubernetes production', requiredSoft: 'kubernetes', minRam: 16, minCores: 12, reward: 62000, duration: 4, xp: 800 },
    { title: 'Лаборатория виртуализации', requiredSoft: 'virtuallab', minRam: 24, minCores: 12, reward: 70000, duration: 4, xp: 850 },
    { title: 'Фотобанк семьи', requiredSoft: 'immich', minRam: 16, minCores: 6, reward: 38000, duration: 3, xp: 520 },
    { title: 'VPN для remote-команды', requiredSoft: 'wireguard', minRam: 2, minCores: 2, reward: 15000, duration: 2, xp: 240 }
  ];
  DATA.MUSIC_TRACKS = [
    { id: 'electro', name: 'Neon Rack · Electro Swing', file: 'audio/electro-swing-type.ogg', author: 'G_P · CC0' },
    { id: 'funky', name: 'Avito Dealer · Funky Menu', file: 'audio/funky-menu-loop.ogg', author: 'iamoneabe · CC0' },
    { id: 'coffee', name: 'Night Shift · Coffee House', file: 'audio/coffee-house-bump.ogg', author: 'OatCog · CC0' },
    { id: 'lofi-rack', name: 'Night Rack · Lo-Fi', file: 'audio/night-rack-lofi.ogg', author: 'Server Tycoon · original synthesis' },
    { id: 'lofi-aisle', name: 'Cold Aisle · Lo-Fi', file: 'audio/cold-aisle-lofi.ogg', author: 'Server Tycoon · original synthesis' }
  ];
  DATA.ROOMS.push(
    { id: 'warehouse', name: 'Серверный ангар', subtitle: 'Линия 30 кВт · 20 мест', slots: 20, rent: 18000, ambient: 14, powerLimit: 30000, noiseLimit: 92, price: 650000 },
    { id: 'tier3', name: 'Tier III Datacenter', subtitle: 'Линия 100 кВт · 40 мест', slots: 40, rent: 55000, ambient: 12, powerLimit: 100000, noiseLimit: 100, price: 2500000 }
  );
  DATA.expansionLoaded = true;
  DATA.network = id => DATA.NETWORKS.find(item => item.id === id) || DATA.NETWORKS[0];
})();
