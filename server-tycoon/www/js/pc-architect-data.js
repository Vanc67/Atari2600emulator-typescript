/* PC Architect 8.0: ровно 20 000 детерминированных деталей 1990–2026, без сети и JSON-дампов. */
(function () {
  'use strict';
  if (!window.DATA || DATA.pcArchitectDataLoaded) return;
  const counts = { cpu: 2800, motherboard: 2800, ram: 2800, gpu: 2800, storage: 2400, psu: 2000, case: 2200, cooling: 2200 };
  const labels = { cpu: 'Процессор', motherboard: 'Материнская плата', ram: 'Память', gpu: 'Видеокарта', storage: 'Накопитель', psu: 'Блок питания', case: 'Корпус', cooling: 'Охлаждение' };
  const icons = { cpu: 'chip', motherboard: 'chip', ram: 'ram', gpu: 'gpu', storage: 'hdd', psu: 'power', case: 'server', cooling: 'cooler' };
  const brands = {
    board: ['ASUS','Gigabyte','MSI','ASRock','ABIT','ECS','Biostar','Intel','Supermicro'],
    ram: ['Kingston','Corsair','Crucial','Samsung','Hynix','Patriot','G.Skill','Apacer','ADATA'],
    storage: ['Seagate','Western Digital','Samsung','Toshiba','Kingston','Crucial','SanDisk','Quantum','Maxtor'],
    psu: ['FSP','Corsair','Seasonic','be quiet!','Thermaltake','Chieftec','Delta','Enhance'],
    case: ['Cooler Master','Fractal Design','NZXT','Lian Li','Zalman','AeroCool','Chieftec','InWin'],
    cooling: ['Noctua','Cooler Master','Thermalright','Deepcool','Zalman','Scythe','Arctic','be quiet!']
  };
  const pick = (items, index) => items[Math.abs(index) % items.length];
  const round = value => Math.round(value);
  const pad = (value, size = 6) => String(value).padStart(size, '0');
  const generation = year => DATA.PC_GENERATIONS.find(item => item.year === year) || DATA.PC_GENERATIONS[0];
  const socketEras = [
    [1990,1992,['Socket 3']], [1993,1994,['Socket 4','Socket 5']], [1995,1996,['Socket 7']],
    [1997,1998,['Socket 7','Slot 1']], [1999,2000,['Socket 370','Slot A','Socket A']],
    [2001,2002,['Socket 423','Socket 478','Socket A']], [2003,2003,['Socket 478','Socket A','Socket 754']],
    [2004,2005,['LGA775','Socket 754','Socket 939']], [2006,2008,['LGA775','AM2']],
    [2009,2010,['LGA775','AM3','LGA1156']], [2011,2012,['LGA1155','AM3+','LGA2011']],
    [2013,2014,['LGA1150','FM2+','LGA2011']], [2015,2016,['LGA1151','FM2+','LGA2011-3']],
    [2017,2019,['AM4','LGA1151','TR4']], [2020,2020,['AM4','LGA1200','TRX40']],
    [2021,2021,['LGA1200','LGA1700','AM4']], [2022,2023,['LGA1700','AM5']], [2024,2026,['LGA1851','AM5','LGA1700']]
  ];
  const socket = (year, variant) => pick(socketEras.find(row => year >= row[0] && year <= row[1])[2], variant);
  const ramType = (year, variant) => {
    if (year <= 1996) return 'SIMM';
    if (year <= 2000) return 'SDRAM';
    if (year === 2001) return variant % 3 ? 'DDR' : 'SDRAM';
    if (year <= 2004) return 'DDR';
    if (year === 2005) return variant % 3 ? 'DDR2' : 'DDR';
    if (year <= 2008) return 'DDR2';
    if (year === 2009) return variant % 3 ? 'DDR3' : 'DDR2';
    if (year <= 2014) return 'DDR3';
    if (year === 2015) return variant % 3 ? 'DDR4' : 'DDR3';
    if (year <= 2021) return 'DDR4';
    if (year === 2022) return variant % 3 ? 'DDR5' : 'DDR4';
    return 'DDR5';
  };
  const gpuInterfaces = (year, variant) => {
    if (year <= 1993) return ['ISA','PCI'];
    if (year <= 1997) return ['PCI'];
    if (year <= 2003) return ['AGP','PCI'];
    if (year <= 2007) return variant % 2 ? ['PCIe','AGP','PCI'] : ['AGP','PCI','PCIe'];
    return ['PCIe'];
  };
  const storageInterfaces = year => year <= 2002 ? ['IDE'] : year <= 2007 ? ['IDE','SATA'] : year <= 2012 ? ['SATA'] : ['SATA','NVMe'];
  const ramCapacities = year => year < 1994 ? [1,2,4,8] : year < 1998 ? [4,8,16,32] : year < 2002 ? [16,32,64,128,256] : year < 2006 ? [128,256,512,1024,2048] : year < 2010 ? [512,1024,2048,4096] : year < 2015 ? [1024,2048,4096,8192,16384] : year < 2020 ? [4096,8192,16384,32768] : year < 2024 ? [8192,16384,32768,65536] : [16384,32768,65536,131072];
  const diskCapacities = year => year < 1994 ? [20,40,80,100,210,340] : year < 1998 ? [540,850,1200,2100,3200] : year < 2002 ? [4200,6400,10240,20480,40960] : year < 2006 ? [40960,61440,81920,122880,163840,245760] : year < 2010 ? [81920,163840,327680,524288,1048576] : year < 2015 ? [131072,262144,524288,1048576,2097152] : year < 2020 ? [131072,262144,524288,1048576,2097152,4194304] : [262144,524288,1048576,2097152,4194304,8388608,16777216];
  const cpuBrand = (socketId, variant) => /AM|FM|TR|Socket A|Slot A|754|939/.test(socketId) ? 'AMD' : /LGA|Slot 1|370|423|478/.test(socketId) ? 'Intel' : pick(['Intel','AMD','Cyrix','VIA'], variant);
  const cpuFamily = (year, brand, variant) => {
    if (brand === 'AMD') return year < 1994 ? 'Am386' : year < 1996 ? 'Am486' : year < 1997 ? 'K5' : year < 1999 ? 'K6-2' : year < 2002 ? 'Athlon' : year < 2004 ? 'Athlon XP' : year < 2007 ? 'Athlon 64' : year < 2011 ? 'Phenom II' : year < 2017 ? 'FX' : pick(['Ryzen 3','Ryzen 5','Ryzen 7','Ryzen 9'], variant);
    if (brand === 'Intel') return year < 1992 ? '386DX' : year < 1995 ? '486DX' : year < 1997 ? 'Pentium' : year < 1998 ? 'Pentium MMX' : year < 1999 ? 'Pentium II' : year < 2001 ? 'Pentium III' : year < 2006 ? 'Pentium 4' : year < 2009 ? 'Core 2 Duo' : year < 2024 ? pick(['Core i3','Core i5','Core i7','Core i9'], variant) : pick(['Core Ultra 5','Core Ultra 7','Core Ultra 9'], variant);
    return brand === 'Cyrix' ? '6x86MX' : 'C7';
  };
  const gpuModel = (year, variant) => year < 1994 ? `${pick(['Cirrus Logic','S3','Tseng'],variant)} VGA` : year < 1997 ? `${pick(['S3 Trio64','ATI Mach64','Matrox Millennium'],variant)}` : year < 2000 ? `${pick(['3dfx Voodoo','NVIDIA Riva','ATI Rage'],variant)}` : year < 2005 ? `${pick(['GeForce FX','Radeon 9','GeForce 6'],variant)}` : year < 2011 ? `${pick(['GeForce GTX','Radeon HD'],variant)}` : year < 2017 ? `${pick(['GeForce GTX','Radeon RX'],variant)}` : `${pick(['GeForce RTX','Radeon RX','Intel Arc'],variant)}`;
  const coresFor = (year, variant) => year < 2006 ? 1 : year < 2009 ? 1 + variant % 2 : year < 2012 ? 2 + variant % 3 : year < 2016 ? 2 + variant % 5 : year < 2019 ? 4 + variant % 5 : year < 2022 ? 6 + variant % 7 : year < 2024 ? 8 + variant % 9 : 12 + variant % 13;
  const formatMb = value => value >= 1048576 ? `${round(value / 1048576)} ТБ` : value >= 1024 ? `${round(value / 1024)} ГБ` : `${value} МБ`;
  const make = (type, year, index, local) => {
    const gen = generation(year); const variant = index * 7 + local * 13 + year;
    const id = `pc8-${type}-${pad(index + 1)}`;
    if (type === 'cpu') {
      const socketId = socket(year, variant); const brand = cpuBrand(socketId, variant); const family = cpuFamily(year, brand, variant);
      const cores = coresFor(year, variant); const score = Math.max(5, round(gen.cpuScore * (.62 + local % 19 / 12)));
      const mhz = round((year < 1995 ? 20 + (year - 1990) * 15 : year < 2000 ? 100 + (year - 1995) * 85 : year < 2005 ? 700 + (year - 2000) * 430 : 1800 + (year - 2005) * 105) * (.86 + local % 13 / 40));
      const tdp = round((year < 1998 ? 12 : 28) + cores * (year < 2006 ? 10 : 13) + score * .025);
      return { id, type, year, name: `${brand} ${family} ${mhz}${local % 3 === 0 ? 'K' : local % 3 === 1 ? 'X' : ''}`, brand, socket: socketId, cores, mhz, cpuScore: score, tdp, value: round((120 + score * 7 + cores * 24) / 10) * 10, summary: `${cores} ядер · ${mhz} МГц · ${socketId} · ${tdp} Вт` };
    }
    if (type === 'motherboard') {
      const socketId = socket(year, variant); const memory = ramType(year, variant); const formFactor = year < 1995 ? 'AT' : local % 5 === 0 ? 'ITX' : local % 2 ? 'mATX' : 'ATX';
      const ramSlots = memory === 'SIMM' ? 4 + local % 2 * 4 : 2 + local % 3 * 2;
      const brand = pick(brands.board, variant); const chipset = year < 1996 ? `${pick(['430FX','430HX','430TX','VIA Apollo'],variant)}` : year < 2005 ? `${pick(['440BX','nForce','VIA KT','Intel 8'],variant)}${year}` : `${pick(['B','Z','X','H'],variant)}${100 + year - 2000 + local % 4 * 100}`;
      return { id, type, year, name: `${brand} ${chipset} ${socketId} ${formFactor}`, brand, socket: socketId, ramType: memory, ramSlots, maxRamMb: 131072 * ramSlots, formFactor, gpuInterfaces: gpuInterfaces(year, variant), storageInterfaces: storageInterfaces(year), value: round((170 + gen.cpuScore * 2.3 + ramSlots * 22) / 10) * 10, summary: `${socketId} · ${memory} ×${ramSlots} · ${formFactor}` };
    }
    if (type === 'ram') {
      const memory = ramType(year, variant); const capacityMb = pick(ramCapacities(year), local); const modules = memory === 'SIMM' ? 1 + local % 2 : 1 + local % 4;
      const speed = memory === 'SIMM' ? 60 + local % 3 * 10 : memory === 'SDRAM' ? 66 + local % 4 * 33 : memory === 'DDR' ? 200 + local % 5 * 66 : memory === 'DDR2' ? 400 + local % 6 * 133 : memory === 'DDR3' ? 800 + local % 7 * 266 : memory === 'DDR4' ? 1600 + local % 8 * 300 : 3200 + local % 9 * 400;
      const brand = pick(brands.ram, variant);
      return { id, type, year, name: `${brand} ${memory} ${formatMb(capacityMb)} ${speed}`, brand, ramType: memory, capacityMb, modules, speed, value: round(45 + capacityMb * (year < 2000 ? 3.5 : year < 2010 ? .32 : .035) + modules * 18), summary: `${formatMb(capacityMb)} ${memory} · ${modules} мод. · ${speed} МГц` };
    }
    if (type === 'gpu') {
      const interfaces = gpuInterfaces(year, variant); const interfaceId = pick(interfaces, local); const score = Math.max(1, round(gen.gpuScore * (.55 + local % 21 / 11)));
      const vramMb = year < 1994 ? pick([.25,.5,1],local) : year < 1998 ? pick([1,2,4,8],local) : year < 2004 ? pick([8,16,32,64,128,256],local) : year < 2010 ? pick([128,256,512,1024,2048],local) : year < 2017 ? pick([1024,2048,4096,8192],local) : pick([4096,8192,12288,16384,24576,32768],local);
      const lengthMm = 90 + local % 170 + Math.min(150, round(score * .035)); const tdp = round(8 + score * .105);
      return { id, type, year, name: `${gpuModel(year, variant)} ${formatMb(vramMb)} ${interfaceId}-${local}`, brand: gpuModel(year, variant).split(' ')[0], interface: interfaceId, gpuScore: score, vramMb, lengthMm, tdp, value: round((90 + score * 8.5 + vramMb * .015) / 10) * 10, summary: `${formatMb(vramMb)} · ${interfaceId} · ${lengthMm} мм · ${tdp} Вт` };
    }
    if (type === 'storage') {
      const interfaces = storageInterfaces(year); const interfaceId = pick(interfaces, local); const storageType = interfaceId === 'NVMe' ? 'NVMe' : year >= 2011 && local % 3 !== 0 ? 'SSD' : 'HDD';
      const capacityMb = pick(diskCapacities(year), local); const brand = pick(brands.storage, variant); const readScore = storageType === 'NVMe' ? 700 + local * 35 : storageType === 'SSD' ? 180 + local * 8 : 25 + local * 2;
      return { id, type, year, name: `${brand} ${storageType} ${formatMb(capacityMb)} ${interfaceId}`, brand, interface: interfaceId, storageType, capacityMb, readScore, value: round(85 + capacityMb / (storageType === 'HDD' ? 700 : 260) + readScore * .15), summary: `${formatMb(capacityMb)} ${storageType} · ${interfaceId}` };
    }
    if (type === 'psu') {
      const watts = year < 1998 ? pick([180,200,230,250,300],local) : year < 2008 ? pick([250,300,350,400,450,500,600],local) : year < 2016 ? pick([400,450,500,600,700,850,1000],local) : pick([450,550,650,750,850,1000,1200,1500],local);
      const formFactor = year < 1998 && local % 2 === 0 ? 'AT' : local % 4 === 0 ? 'SFX' : 'ATX'; const brand = pick(brands.psu, variant); const efficiency = Math.min(94, 70 + local % 7 * 4);
      return { id, type, year, name: `${brand} ${watts}W ${formFactor} ${efficiency}%`, brand, watts, formFactor, efficiency, value: round(110 + watts * 1.35 + efficiency * 2), summary: `${watts} Вт · ${formFactor} · ${efficiency}%` };
    }
    if (type === 'case') {
      const formFactors = year < 1995 ? ['AT','ATX','mATX'] : local % 3 === 0 ? ['ATX','mATX','ITX'] : ['ATX','mATX'];
      const psuFormFactors = year < 1998 ? ['AT','ATX'] : ['ATX','SFX']; const maxGpuMm = 270 + local % 190; const maxCoolerMm = 135 + local % 70;
      const fans = 1 + local % 8; const brand = pick(brands.case, variant);
      return { id, type, year, name: `${brand} Case ${year}-${local} ${formFactors.join('/')}`, brand, formFactors, psuFormFactors, maxGpuMm, maxCoolerMm, fans, color: pick(['graphite','silver','beige','black','white'],variant), value: round(180 + maxGpuMm * .65 + fans * 35), summary: `${formFactors.join('/')} · GPU ${maxGpuMm} мм · FAN ${fans}` };
    }
    const socketId = socket(year, variant); const capacityWatts = 65 + round(gen.cpuScore * .075) + local % 200; const heightMm = 75 + local % 110;
    const kind = year >= 2005 && local % 5 === 0 ? 'liquid' : 'air'; const brand = pick(brands.cooling, variant); const noise = 12 + local % 28;
    return { id, type, year, name: `${brand} ${kind === 'liquid' ? 'Liquid' : 'Air'} ${capacityWatts}W ${socketId}`, brand, sockets: [socketId, socket(year, variant + 1)], capacityWatts, heightMm, kind, noise, value: round(80 + capacityWatts * 1.15 + (kind === 'liquid' ? 250 : 0)), summary: `${capacityWatts} Вт · ${heightMm} мм · ${kind} · ${noise} дБ` };
  };
  const catalog = [];
  Object.entries(counts).forEach(([type, count]) => {
    for (let index = 0; index < count; index += 1) {
      const year = 1990 + index % 37; const local = Math.floor(index / 37);
      const part = make(type, year, index, local); const revision = pad(local + 1, 2);
      part.sku = `PC8-${type.toUpperCase()}-${year}-${revision}`;
      part.name = `${part.name} · ${year}-${revision}`;
      catalog.push(part);
    }
  });
  DATA.PC_PART_CATALOG = catalog;
  DATA.PC_PART_INDEX = new Map(catalog.map(part => [part.id, part]));
  DATA.PC_PART_TYPES = Object.fromEntries(Object.entries(counts).map(([id, count]) => [id, { id, count, name: labels[id], icon: icons[id] }]));
  DATA.PC_PART_YEARS = Array.from({ length: 37 }, (_, index) => 1990 + index);
  DATA.PC_PART_BY_TYPE_YEAR = new Map();
  catalog.forEach(part => {
    const key = `${part.type}:${part.year}`;
    if (!DATA.PC_PART_BY_TYPE_YEAR.has(key)) DATA.PC_PART_BY_TYPE_YEAR.set(key, []);
    DATA.PC_PART_BY_TYPE_YEAR.get(key).push(part);
  });
  DATA.pcArchitectDataLoaded = true;
})();
