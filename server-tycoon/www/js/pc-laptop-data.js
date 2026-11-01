/* Laptop Architect 8.2: 4 440 мобильных деталей 1990–2026, генерация без сети. */
(function () {
  'use strict';
  if (!window.DATA || !DATA.PC_GENERATIONS || DATA.pcLaptopDataLoaded) return;
  const years = Array.from({ length: 37 }, (_, index) => 1990 + index);
  const definitions = {
    cpu: ['Мобильный процессор', 'chip'], motherboard: ['Плата ноутбука', 'chip'], ram: ['Память SO-DIMM', 'ram'],
    gpu: ['Мобильный GPU', 'gpu'], storage: ['Накопитель ноутбука', 'hdd'], psu: ['Адаптер питания', 'power'],
    case: ['Шасси ноутбука', 'server'], cooling: ['Мобильное охлаждение', 'cooler'], display: ['Дисплей', 'os'], battery: ['Аккумулятор', 'power']
  };
  const types = Object.keys(definitions); const perTypeYear = 12;
  const pick = (items, seed) => items[Math.abs(seed) % items.length]; const round = value => Math.round(Number(value) || 0);
  const pad = (value, width = 2) => String(value).padStart(width, '0'); const generation = year => DATA.PC_GENERATIONS.find(item => item.year === year);
  const mb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${round(value / 1024)} ГБ` : `${value} МБ`;
  const sockets = year => year < 1995 ? ['mPGA-478','Socket 5','Socket 7'] : year < 2003 ? ['Socket 7','Socket 370','Slot 1'] : year < 2007 ? ['Socket M','Socket P','Socket 478'] : year < 2014 ? ['PGA988','Socket G2','Socket G1'] : year < 2020 ? ['BGA-1168','BGA-1356','BGA-1440'] : ['BGA-1787','BGA-2049','Mobile-SoC'];
  const gpuInterfaces = year => year < 1997 ? ['Integrated','AGP'] : year < 2004 ? ['AGP','MXM','Integrated'] : year < 2015 ? ['MXM','Integrated'] : ['MXM','Integrated','Soldered'];
  const storageInterfaces = year => year < 2004 ? ['IDE'] : year < 2012 ? ['SATA','IDE'] : year < 2015 ? ['SATA','mSATA'] : ['SATA','NVMe','M.2 SATA'];
  const displayConnectors = year => year < 1997 ? ['VGA'] : year < 2010 ? ['LVDS'] : ['eDP','LVDS'];
  const brands = {
    cpu: ['Intel','AMD','Intel','AMD','Cyrix'], board: ['HP','Dell','Lenovo','ASUS','Gigabyte','IBM','Compaq'],
    ram: ['Kingston','Samsung','Hynix','Crucial','ADATA'], gpu: ['NVIDIA','AMD','Intel','GeForce','Radeon','ATI'],
    storage: ['Seagate','Western Digital','Toshiba','Samsung','SanDisk'], psu: ['Delta','HP','Lenovo','Dell','ASUS'],
    case: ['HP','Dell','Lenovo','IBM','Compaq','ASUS','Acer','Fujitsu'], cooling: ['Arctic','Delta','Fujitsu'],
    display: ['Samsung','LG','AU Optronics','BOE','Sharp','Toshiba'], battery: ['Sanyo','Sony','Panasonic','Samsung','Duracell']
  };
  const cpuFamily = (year, brand) => brand === 'AMD' ? year < 1999 ? 'Mobile K6' : year < 2007 ? 'Mobile Athlon' : year < 2017 ? 'Mobile A-Series' : 'Ryzen Mobile' : year < 1995 ? 'Mobile 486' : year < 1999 ? 'Mobile Pentium' : year < 2003 ? 'Pentium III-M' : year < 2007 ? 'Pentium M' : year < 2009 ? 'Core 2 Mobile' : year < 2024 ? 'Core Mobile' : 'Core Ultra Mobile';
  const gpuFamily = year => year < 1997 ? 'Mobile VGA' : year < 2001 ? 'Mobility Rage' : year < 2007 ? 'Mobility Radeon' : year < 2014 ? 'GeForce Mobile' : year < 2020 ? 'GTX Mobile' : 'RTX Mobile';
  const ramCapacities = year => year < 1995 ? [1,2,4,8,16] : year < 1999 ? [4,8,16,32,64] : year < 2004 ? [32,64,128,256,512] : year < 2009 ? [256,512,1024,2048,4096] : year < 2015 ? [1024,2048,4096,8192,16384] : year < 2021 ? [4096,8192,16384,32768,65536] : [8192,16384,32768,65536,131072];
  const diskCapacities = year => year < 1995 ? [40,80,100,170,340,540] : year < 1999 ? [540,850,1200,2100,3200,6400] : year < 2005 ? [10240,20480,40960,61440,81920] : year < 2012 ? [81920,122880,163840,245760,327680,512000] : year < 2018 ? [131072,262144,524288,1048576,2097152] : [262144,524288,1048576,2097152,4194304,8388608];
  function make(type, year, local) {
    const gen = generation(year); const variant = year * 11 + local * 17 + types.indexOf(type) * 5; const id = `lap9-${type}-${year}-${pad(local + 1)}`;
    if (type === 'cpu') {
      const brand = pick(brands.cpu, variant); const socket = pick(sockets(year), variant); const score = Math.max(4, round(gen.cpuScore * (.45 + local * .025)));
      const cores = year < 2006 ? 1 : year < 2012 ? 2 : year < 2018 ? 2 + local % 3 : 4 + local % 9; const tdp = round(7 + score * .035);
      return { id, type, year, brand, name: `${brand} ${cpuFamily(year, brand)} ${100 + local * 25}U`, socket, cores, cpuScore: score, tdp, value: round(140 + score * 6.2 + cores * 20), summary: `${cores} ядер · ${socket} · ${tdp} Вт` };
    }
    if (type === 'motherboard') {
      const brand = pick(brands.board, variant); const socket = pick(sockets(year), variant); const ramType = gen.ramType; const ramSlots = local % 5 === 0 ? 1 : 2;
      return { id, type, year, brand, name: `${brand} Mobile Board ${socket} ${ramType}`, socket, ramType, ramSlots, maxRamMb: 262144 * ramSlots,
        gpuInterfaces: gpuInterfaces(year), storageInterfaces: storageInterfaces(year), value: round(220 + gen.cpuScore * 1.5 + ramSlots * 80), summary: `${socket} · ${ramType} ×${ramSlots} · mobile board` };
    }
    if (type === 'ram') {
      const brand = pick(brands.ram, variant); const capacityMb = pick(ramCapacities(year), local); const modules = local % 7 === 0 && gen.ramType !== 'SIMM' ? 2 : 1;
      const speed = gen.ramType === 'SIMM' ? 70 : gen.ramType === 'SDRAM' ? 100 + local % 3 * 33 : gen.ramType === 'DDR' ? 266 + local % 4 * 67 : gen.ramType === 'DDR2' ? 533 + local % 4 * 134 : gen.ramType === 'DDR3' ? 1066 + local % 5 * 267 : gen.ramType === 'DDR4' ? 2133 + local % 5 * 400 : 4800 + local % 5 * 800;
      return { id, type, year, brand, name: `${brand} SO-DIMM ${mb(capacityMb)} ${gen.ramType}`, ramType: gen.ramType, capacityMb, modules, speed,
        value: round(55 + capacityMb * (year < 2000 ? 2.4 : year < 2010 ? .25 : .025) + modules * 25), summary: `${mb(capacityMb)} ${gen.ramType} · ${modules} мод. · ${speed} МТ/с` };
    }
    if (type === 'gpu') {
      const brand = pick(brands.gpu, variant); const interfaceId = pick(gpuInterfaces(year), variant); const score = Math.max(1, round(gen.gpuScore * (.45 + local * .03)));
      const vramMb = year < 1997 ? pick([1,2,4],local) : year < 2004 ? pick([4,8,16,32,64],local) : year < 2012 ? pick([64,128,256,512,1024],local) : year < 2020 ? pick([1024,2048,4096,6144,8192],local) : pick([4096,6144,8192,12288,16384],local);
      const tdp = round(4 + score * .035);
      return { id, type, year, brand, name: `${brand} ${gpuFamily(year)} ${mb(vramMb)} ${interfaceId}`, interface: interfaceId, gpuScore: score, vramMb, tdp,
        value: round(110 + score * 7.1 + vramMb * .01), summary: `${mb(vramMb)} · ${interfaceId} · ${tdp} Вт` };
    }
    if (type === 'storage') {
      const brand = pick(brands.storage, variant); const interfaceId = pick(storageInterfaces(year), variant); const storageType = interfaceId === 'NVMe' ? 'NVMe' : year >= 2011 && local % 3 !== 0 ? 'SSD' : 'HDD';
      const capacityMb = pick(diskCapacities(year), local); const readScore = storageType === 'NVMe' ? 500 + local * 40 : storageType === 'SSD' ? 140 + local * 10 : 18 + local * 2;
      return { id, type, year, brand, name: `${brand} Mobile ${storageType} ${mb(capacityMb)} ${interfaceId}`, interface: interfaceId, storageType, capacityMb, readScore,
        value: round(75 + capacityMb / (storageType === 'HDD' ? 650 : 230) + readScore * .18), summary: `${mb(capacityMb)} ${storageType} · 2.5\" / ${interfaceId}` };
    }
    if (type === 'psu') {
      const brand = pick(brands.psu, variant); const watts = Math.max(45, round((45 + gen.cpuScore * .012 + gen.gpuScore * .02 + local * 6) / 5) * 5);
      const connector = year >= 2015 && local % 3 === 0 ? 'USB-C' : 'Barrel'; const efficiency = Math.min(94, 78 + local % 5 * 4);
      return { id, type, year, brand, name: `${brand} Laptop Adapter ${watts}W ${connector}`, watts, connector, efficiency,
        value: round(120 + watts * 1.7 + efficiency), summary: `${watts} Вт · ${connector} · ${efficiency}%` };
    }
    if (type === 'case') {
      const brand = pick(brands.case, variant); const maxDisplayInch = pick([13.3,14,15.6,17.3],local); const batteryVoltage = pick([10.8,14.4],local);
      const adapterConnector = year >= 2015 && local % 3 === 0 ? 'USB-C' : 'Barrel'; const weightKg = Number((1.1 + maxDisplayInch * .075 + local % 4 * .18).toFixed(2));
      return { id, type, year, brand, name: `${brand} Laptop Chassis ${maxDisplayInch}\" ${batteryVoltage}V`, maxDisplayInch, displayConnectors: displayConnectors(year), batteryVoltage, adapterConnector,
        maxCoolerMm: 8 + local % 5, fans: 1 + local % 3, weightKg, value: round(320 + maxDisplayInch * 35 + weightKg * 70), summary: `до ${maxDisplayInch}\" · ${batteryVoltage}V · ${weightKg} кг · ${adapterConnector}` };
    }
    if (type === 'cooling') {
      const brand = pick(brands.cooling, variant); const capacityWatts = round(35 + gen.cpuScore * .025 + local * 5); const heightMm = 5 + local % 6; const noise = 10 + local % 22;
      return { id, type, year, brand, name: `${brand} Mobile Blower ${capacityWatts}W ${heightMm}mm`, sockets: sockets(year), capacityWatts, heightMm, noise,
        value: round(100 + capacityWatts * 1.3 + noise * 2), summary: `${capacityWatts} Вт · ${heightMm} мм · ${noise} дБ` };
    }
    if (type === 'display') {
      const brand = pick(brands.display, variant); const sizeInch = pick([11.6,12.1,13.3,14,15.6,17.3],local); const connector = pick(displayConnectors(year), variant);
      const resolution = year < 1995 ? pick(['640×480','800×600'],local) : year < 2005 ? pick(['1024×768','1280×800','1400×1050'],local) : year < 2015 ? pick(['1366×768','1600×900','1920×1080'],local) : pick(['1920×1080','2560×1440','3840×2160'],local);
      const refreshHz = year < 2016 ? 60 : pick([60,75,120,144,165,240],local); const watts = Number((2 + sizeInch * .45 + refreshHz * .018).toFixed(1));
      return { id, type, year, brand, name: `${brand} Mobile Panel ${sizeInch}\" ${resolution}`, sizeInch, connector, resolution, refreshHz, watts,
        value: round(180 + sizeInch * 32 + refreshHz * 2.5 + (resolution === '3840×2160' ? 600 : 0)), summary: `${sizeInch}\" · ${resolution} · ${refreshHz} Гц · ${connector}` };
    }
    const brand = pick(brands.battery, variant); const cells = pick([2,3,4,6,8,9],local); const voltage = pick([10.8,14.4],local); const capacityWh = 30 + cells * 8 + local * 2;
    return { id, type, year, brand, name: `${brand} Li-ion ${cells}-cell ${capacityWh}Wh ${voltage}V`, cells, voltage, capacityWh, chargeMinutes: 70 + cells * 12,
      weightKg: Number((.18 + cells * .055).toFixed(2)), value: round(180 + capacityWh * 10 + cells * 22), summary: `${cells} cell · ${capacityWh} Wh · ${voltage} V` };
  }
  const catalog = [];
  types.forEach(type => years.forEach(year => {
    for (let local = 0; local < perTypeYear; local += 1) {
      const part = make(type, year, local); part.family = 'laptop'; part.sku = `LAP9-${type.toUpperCase()}-${year}-${pad(local + 1)}`; part.name += ` · ${year}-${type.toUpperCase()}-${pad(local + 1)}`; catalog.push(part);
    }
  }));
  DATA.LAPTOP_PART_CATALOG = catalog; DATA.LAPTOP_PART_INDEX = new Map(catalog.map(part => [part.id, part])); DATA.LAPTOP_PART_YEARS = years;
  DATA.LAPTOP_PART_TYPES = Object.fromEntries(Object.entries(definitions).map(([id, [name, icon]]) => [id, { id, name, icon, count: years.length * perTypeYear }]));
  DATA.LAPTOP_PART_BY_TYPE_YEAR = new Map(); catalog.forEach(part => { const key = `${part.type}:${part.year}`; if (!DATA.LAPTOP_PART_BY_TYPE_YEAR.has(key)) DATA.LAPTOP_PART_BY_TYPE_YEAR.set(key, []); DATA.LAPTOP_PART_BY_TYPE_YEAR.get(key).push(part); });
  DATA.pcLaptopDataLoaded = true;
})();
