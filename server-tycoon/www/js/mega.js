/* Mega Archive 3.0: процедурный каталог 1990–2026. Все позиции генерируются локально. */
(function () {
  'use strict';
  if (!window.DATA || DATA.megaLoaded) return;
  const P = DATA.PARTS;
  const T = DATA.SERVER_TEMPLATES;
  const OS = DATA.OS_LIST;
  const APPS = DATA.SOFTWARE;
  const ids = new Set(P.map(part => part.id));
  const templateIds = new Set(T.map(template => template.id));
  const osIds = new Set(OS.map(os => os.id));
  const appIds = new Set(APPS.map(app => app.id));
  const addPart = part => { if (!ids.has(part.id)) { ids.add(part.id); P.push(part); } return part; };
  const round = (value, digits = 2) => Number(value.toFixed(digits));
  const LINUX = ['debian', 'debian13', 'ubuntu', 'rocky', 'almalinux', 'fedora', 'opensuse', 'nixos', 'arch', 'proxmox'];
  const WINDOWS = ['windows', 'windows2025'];
  const NAS = ['truenas', 'omv', 'unraid'];
  const MODERN_UNIX = [...LINUX, ...NAS, 'freebsd'];

  // Годы базовой коллекции нужны общему фильтру эпох.
  const inferYear = part => {
    if (part.year) return part.year;
    const text = `${part.socket || ''} ${part.ramType || ''} ${part.name || ''}`;
    if (/SIMM|SOCKET5|SOCKET7/.test(text)) return 1993;
    if (/SLOT1|SOCKET370|SDRAM/.test(text)) return 1998;
    if (/DDR5|LGA4677|SP5|9950|14900/.test(text)) return 2024;
    if (/LGA4189|AM5|7950/.test(text)) return 2021;
    if (/SP3|LGA3647|AM4|DDR4|12900/.test(text)) return 2018;
    if (/LGA2011-3|DDR4/.test(text)) return 2015;
    if (/LGA2011|LGA1366|DDR3/.test(text)) return 2011;
    if (/LGA771|DDR2/.test(text)) return 2007;
    return 2004;
  };
  P.forEach(part => { part.year = inferYear(part); });
  T.forEach(template => {
    const chassis = P.find(part => part.id === template.chassis);
    template.year = template.year || (chassis ? chassis.year : 2012);
    template.brand = template.brand || (chassis && chassis.brand) || deviceBrand(template.name);
  });
  const osYears = { windows: 2022, debian: 2023, truenas: 2022, proxmox: 2023, ubuntu: 2024, debian13: 2025, rocky: 2022, almalinux: 2022, fedora: 2024, opensuse: 2024, nixos: 2025, arch: 2026, freebsd: 2023, omv: 2024, unraid: 2024, pfsense: 2024, openwrt: 2024, esxi: 2022, xcpng: 2024, windows2025: 2025, solaris: 2018 };
  OS.forEach(os => { os.year = osYears[os.id] || 2020; });
  APPS.forEach(app => { app.category = app.category || 'server'; app.release = app.release || 'Standard'; app.year = app.year || 2020; });

  // Дополнительные исторические бренды рисуются локальными SVG-wordmark.
  const baseBrandSvg = window.brandSvg;
  const marks = {
    compaq: ['COMPAQ', '#E21B23'], dec: ['DIGITAL', '#2878B8'], sgi: ['SGI', '#7B61FF'],
    sun: ['SUN', '#F7941E'], nec: ['NEC', '#244AA5'], bull: ['BULL', '#E64545'],
    hitachi: ['HITACHI', '#E60012'], unisys: ['UNISYS', '#E23C3C'], tandem: ['TANDEM', '#58C9C9'],
    arm: ['ARM', '#48A9A6']
  };
  Object.entries(marks).forEach(([id, data]) => { window.BRAND_COLORS[id] = data[1]; });
  window.brandSvg = function (id, className = '') {
    if (marks[id]) {
      const size = marks[id][0].length > 6 ? 4.2 : 5.8;
      return `<svg class="${className}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><rect x="1" y="5" width="22" height="14" rx="4" fill="none" stroke="currentColor" opacity=".35"/><text x="12" y="14.5" text-anchor="middle" font-family="monospace" font-size="${size}" font-weight="bold" fill="currentColor">${marks[id][0]}</text></svg>`;
    }
    return baseBrandSvg(id, className);
  };
  const baseDeviceBrand = window.deviceBrand;
  window.deviceBrand = function (name) {
    const text = String(name || '').toLowerCase();
    for (const id of Object.keys(marks)) if (text.includes(id) || (id === 'dec' && text.includes('digital')) || (id === 'sun' && text.includes('sparc'))) return id;
    return baseDeviceBrand(name);
  };

  // Исторические ОС: у ранних систем требования измеряются долями ГБ.
  function legacyOS(id, name, year, family, minRam, minDisk, baseRam, baseCpu, brand, description) {
    if (osIds.has(id)) return;
    osIds.add(id);
    OS.push({ id, name, year, family, brand, icon: 'os', price: 0, minRam, minDisk, baseRam, baseCpu, description, terminal: `${id}: legacy setup --year=${year}` });
  }
  [
    ['dos622','MS-DOS 6.22',1994,'legacy',.016,.05,.008,0,'windows','FAT16 · драйверы DOS'],
    ['nt31','Windows NT 3.1 Advanced Server',1993,'windows',.032,.1,.024,1,'windows','NTFS · ранний домен'],
    ['nt40','Windows NT 4.0 Server',1996,'windows',.064,.25,.04,1,'windows','IIS 4 · NTFS · домен NT'],
    ['win2000','Windows 2000 Server',2000,'windows',.128,.5,.08,2,'windows','Active Directory · IIS 5'],
    ['server2003','Windows Server 2003',2003,'windows',.25,2,.15,3,'windows','AD · IIS 6 · 32/64-bit'],
    ['server2008','Windows Server 2008 R2',2009,'windows',.5,8,.35,4,'windows','Hyper-V · IIS 7'],
    ['server2012','Windows Server 2012 R2',2013,'windows',1,16,.6,5,'windows','Hyper-V · Storage Spaces'],
    ['server2016','Windows Server 2016',2016,'windows',2,24,.9,6,'windows','Nano Server · контейнеры'],
    ['server2019','Windows Server 2019',2019,'windows',2,32,1.2,6,'windows','HCI · Kubernetes'],
    ['netware3','Novell NetWare 3.12',1993,'legacy',.016,.1,.012,1,'files','IPX/SPX · файловый сервер'],
    ['netware4','Novell NetWare 4.11',1996,'legacy',.032,.25,.024,1,'files','NDS · IPX/SPX'],
    ['netware5','Novell NetWare 5.1',2000,'legacy',.064,.5,.04,2,'files','eDirectory · TCP/IP'],
    ['netware65','Novell NetWare 6.5',2003,'legacy',.128,1,.08,2,'files','iFolder · Apache'],
    ['os2warp','IBM OS/2 Warp Server',1995,'legacy',.032,.2,.024,1,'ibm','HPFS · LAN Server'],
    ['sco5','SCO OpenServer 5',1995,'unix',.032,.2,.024,1,'server','UnixWare · SMB через Samba'],
    ['irix62','SGI IRIX 6.2',1996,'unix',.064,.5,.04,2,'sgi','XFS · MIPS · Challenge'],
    ['irix65','SGI IRIX 6.5',1998,'unix',.128,1,.08,2,'sgi','MIPS · CXFS · OpenGL'],
    ['solaris26','Sun Solaris 2.6',1997,'unix',.064,.5,.04,2,'sun','SPARC · NFS · NIS+'],
    ['solaris10','Sun Solaris 10',2005,'unix',.5,4,.25,4,'sun','ZFS · Zones · SPARC/x86'],
    ['aix43','IBM AIX 4.3',1997,'unix',.064,.5,.04,2,'ibm','POWER · JFS · HACMP'],
    ['aix73','IBM AIX 7.3',2021,'unix',2,20,.8,5,'ibm','POWER10 · WPAR · JFS2'],
    ['hpux11','HP-UX 11i',1997,'unix',.064,.5,.04,2,'hp','PA-RISC · LVM · Serviceguard'],
    ['freebsd1','FreeBSD 1.0',1993,'unix',.016,.1,.012,1,'freebsd','BSD TCP/IP · ports'],
    ['freebsd4','FreeBSD 4.11',2005,'unix',.128,1,.08,2,'freebsd','jail · ports · ipfw'],
    ['openbsd3','OpenBSD 3.0',2001,'unix',.064,.5,.04,2,'server','pf · OpenSSH · chroot'],
    ['netbsd1','NetBSD 1.0',1993,'unix',.016,.1,.012,1,'server','PKGSRC · кросс-платформа'],
    ['debian11','Debian 1.1 Buzz',1996,'linux',.032,.2,.024,1,'debian','dpkg · ранний APT'],
    ['debian30','Debian 3.0 Woody',2002,'linux',.064,.5,.04,2,'debian','APT · stable · i386'],
    ['debian60','Debian 6.0 Squeeze',2011,'linux',.25,2,.15,3,'debian','Linux/FreeBSD · LTS'],
    ['redhat52','Red Hat Linux 5.2',1998,'linux',.032,.25,.024,1,'redhat','RPM · Apache 1.3'],
    ['rhel5','Red Hat Enterprise Linux 5',2007,'linux',.5,4,.25,3,'redhat','Xen · RPM · SELinux'],
    ['slackware3','Slackware Linux 3.0',1995,'linux',.016,.2,.012,1,'server','BSD init · i386'],
    ['suse60','SuSE Linux 6.0',1999,'linux',.064,.5,.04,2,'suse','YaST · RPM · KDE'],
    ['ubuntu410','Ubuntu 4.10 Warty Warthog',2004,'linux',.128,1,.08,2,'ubuntu','Debian · первая версия'],
    ['ubuntu1004','Ubuntu Server 10.04 LTS',2010,'linux',.5,4,.25,3,'ubuntu','Lucid · LTS · cloud-init'],
    ['esx35','VMware ESX 3.5',2008,'hypervisor',1,8,.5,5,'vmware','VMFS · VirtualCenter'],
    ['esxi67','VMware ESXi 6.7',2018,'hypervisor',4,32,2,6,'vmware','vSphere · vSAN']
  ].forEach(args => legacyOS(...args));

  const families = [
    { brand:'compaq', prefix:'Compaq ProLiant', start:1990, end:2002, arch:'x86', models:['800','1200','1600','3000','5000','5500','6400','7000','8000','ML370','ML530','DL360'] },
    { brand:'dec', prefix:'DEC AlphaServer', start:1990, end:2001, arch:'alpha', models:['400','800','1000A','1200','2100','4000','4100','8000','8200','8400','DS10','DS20'] },
    { brand:'sgi', prefix:'SGI Challenge', start:1990, end:2005, arch:'mips', models:['S','M','L','XL','DM','Origin 200','Origin 300','Origin 350','Origin 2000','Altix 350','Altix 3000'] },
    { brand:'sun', prefix:'Sun Enterprise', start:1990, end:2011, arch:'sparc', models:['220R','250','420R','450','3000','3500','4500','5500','6000','6500','10000','V240','T2000'] },
    { brand:'ibm', prefix:'IBM RS/6000', start:1990, end:2006, arch:'power', models:['H50','H70','H80','S70','S80','F50','F80','M80','p615','p630','p650','p670'] },
    { brand:'hp', prefix:'HP 9000', start:1990, end:2007, arch:'parisc', models:['D220','D320','K200','K400','L1000','L2000','N4000','rp2400','rp3400','rp4400','rp7400','rp8400'] },
    { brand:'nec', prefix:'NEC Express5800', start:1990, end:2026, arch:'x86', models:['120Lj','120Ri','140Re','320Fe','R120d','R120e','R120f','R120g','R120h','R120i','R120j','R120k'] },
    { brand:'bull', prefix:'Bull Escala', start:1990, end:2020, arch:'power', models:['PL100','PL200','PL300','PL400','PL600','PL700','PL800','PL900','PL1000','PL1200'] },
    { brand:'hitachi', prefix:'Hitachi HA8000', start:1991, end:2026, arch:'x86', models:['RS110','RS120','RS220','RS225','RS320','RS520','TS10','TS20','TS220','TS240','TS250'] },
    { brand:'unisys', prefix:'Unisys ES7000', start:1991, end:2018, arch:'x86', models:['Aries','Orion 100','Orion 200','Orion 400','7600R','7600G','7600C','7600D'] },
    { brand:'tandem', prefix:'Tandem NonStop', start:1990, end:2008, arch:'mips', models:['Himalaya S100','S200','S1000','S2000','S4000','S7000','S7400','S8600'] },
    { brand:'supermicro', prefix:'Supermicro SuperServer', start:1993, end:2026, arch:'x86', models:['5013','5015','6015','6025','6026','6027','6036','6037','6047','6048','1029','2029','1025','2025'] },
    { brand:'dell', prefix:'Dell PowerEdge', start:1994, end:2026, arch:'x86', models:['2100','2300','2400','4300','4400','6300','6400','6450','6650','2600','2650','2850','2950','R610','R710','R720','R730','R740','R750','R760'] },
    { brand:'fujitsu', prefix:'Fujitsu PRIMERGY', start:1994, end:2026, arch:'x86', models:['TX120','TX150','TX200','TX300','RX100','RX200','RX300','RX500','RX600','RX900','RX1330','RX2530','RX2540','RX4770'] },
    { brand:'ibm', prefix:'IBM Netfinity / xSeries', start:1997, end:2013, arch:'x86', models:['3000','3500','5000','5100','5500','5600','6000','7000','x206','x226','x235','x255','x336','x346','x3650','x3850'] },
    { brand:'hp', prefix:'HP ProLiant', start:2002, end:2026, arch:'x86', models:['DL20','DL60','DL80','DL120','DL160','DL180','DL320','DL325','DL360','DL380','DL385','DL560','DL580','ML30','ML110','ML150','ML350'] },
    { brand:'inspur', prefix:'Inspur NF / K1', start:2000, end:2026, arch:'x86', models:['NF3120','NF5170','NF5180','NF5260','NF5270','NF5280','NF5468','NF5488','NF8260','NF8480','K1 Power S914','K1 Power S922'] },
    { brand:'amd', prefix:'AMD Opteron Reference Server', start:2003, end:2017, arch:'x86', models:['Dual 240','Quad 880','1100 Series','1200 Series','2200 Series','4200 Series','6200 Series','6300 Series'] },
    { brand:'huawei', prefix:'Huawei FusionServer', start:2004, end:2026, arch:'x86', models:['RH1288','RH2285','RH2288','RH2485','RH2488','RH5885','RH8100','1288H','2288H','2488H','5288H'] },
    { brand:'asus', prefix:'ASUS Server / ESC', start:2005, end:2026, arch:'x86', models:['RS100','RS300','RS500','RS520','RS700','RS720','ESC1000','ESC2000','ESC4000','ESC8000','ESC N8'] },
    { brand:'gigabyte', prefix:'GIGABYTE Server', start:2007, end:2026, arch:'x86', models:['R120','R152','R181','R182','R272','R282','H261','H262','G221','G242','G292','G482'] },
    { brand:'cisco', prefix:'Cisco UCS', start:2009, end:2026, arch:'x86', models:['B200 M1','B200 M2','B200 M3','B200 M4','B200 M5','B200 M6','C220 M3','C220 M4','C220 M5','C240 M4','C240 M5','C240 M6'] },
    { brand:'ibm', prefix:'IBM Power System', start:2008, end:2026, arch:'power', models:['S710','S722','S814','S822','S914','S922','S924','S1014','S1022','S1024','E850','E950','E980','E1080'] },
    { brand:'sun', prefix:'Oracle SPARC Server', start:2011, end:2026, arch:'sparc', models:['T4-1','T4-2','T5-2','T5-4','T7-1','T7-2','T8-1','T8-2','M8-8','S7-2'] },
    { brand:'lenovo', prefix:'Lenovo ThinkSystem', start:2014, end:2026, arch:'x86', models:['ST50','ST250','ST550','ST650','SR250','SR530','SR550','SR570','SR630','SR650','SR655','SR665','SR850','SR860','SR950','SD530'] },
    { brand:'arm', prefix:'ARM Cloud Node', start:2018, end:2026, arch:'arm', models:['Ampere eMAG','Altra 32','Altra 64','Altra Max','Graviton2','Graviton3','Graviton4','Neoverse N1','Neoverse N2','Neoverse V2'] }
  ];

  function ramType(year) {
    if (year <= 1994) return 'SIMM';
    if (year <= 1998) return 'SDRAM';
    if (year <= 2002) return 'DDR';
    if (year <= 2007) return 'DDR2';
    if (year <= 2014) return 'DDR3';
    if (year <= 2021) return 'DDR4';
    return 'DDR5';
  }
  function socketFor(year, arch, variant) {
    if (arch !== 'x86') return { alpha:'ALPHA', mips:'MIPS', sparc:'SPARC', power:'POWER', parisc:'PA-RISC', arm:'ARM' }[arch];
    if (variant % 4 === 3 && year >= 2003) {
      if (year <= 2006) return 'SOCKET940';
      if (year <= 2016) return 'AM3';
      if (year <= 2022) return 'SP3';
      return 'SP5';
    }
    if (year <= 1992) return 'SOCKET5';
    if (year <= 1995) return 'SOCKET7';
    if (year <= 1998) return 'SLOT1';
    if (year <= 2002) return 'SOCKET370';
    if (year <= 2005) return 'SOCKET604';
    if (year <= 2008) return 'LGA771';
    if (year <= 2011) return 'LGA1366';
    if (year <= 2013) return 'LGA2011';
    if (year <= 2017) return 'LGA2011-3';
    if (year <= 2020) return 'LGA3647';
    if (year <= 2022) return 'LGA4189';
    return 'LGA4677';
  }
  function coresFor(year, variant, arch) {
    const tier = variant % 4;
    let base;
    if (year <= 2001) base = 1;
    else if (year <= 2006) base = 2;
    else if (year <= 2011) base = 4;
    else if (year <= 2016) base = 8;
    else if (year <= 2020) base = 16;
    else base = 32;
    let cores = base * (1 + tier);
    if (arch !== 'x86' && year < 2005) cores = Math.max(1, Math.ceil(cores / 2));
    return Math.min(128, cores);
  }
  function cpuName(year, arch, socket, cores, variant) {
    const model = String(100 + (year - 1990) * 7 + variant);
    if (arch === 'alpha') return `DEC Alpha 21${year < 1997 ? '164' : '264'} ${model}`;
    if (arch === 'mips') return `MIPS R${year < 1996 ? '10000' : year < 2001 ? '12000' : '16000'} ${model}`;
    if (arch === 'sparc') return `${year < 2001 ? 'UltraSPARC II' : year < 2008 ? 'UltraSPARC IV' : year < 2018 ? 'SPARC T' : 'SPARC M'} ${model}`;
    if (arch === 'power') return `IBM POWER${Math.min(11, Math.max(3, Math.floor((year - 1990) / 4) + 3))} ${model}`;
    if (arch === 'parisc') return `HP PA-${year < 1996 ? '7200' : year < 2001 ? '8500' : '8800'} ${model}`;
    if (arch === 'arm') return `ARM Neoverse ${year < 2021 ? 'N1' : year < 2024 ? 'N2' : 'V2'} ${model}`;
    if (/SP3|SP5|SOCKET940|AM3/.test(socket)) return `${year < 2017 ? 'AMD Opteron' : 'AMD EPYC'} ${model}`;
    if (year < 1995) return `Intel 80486DX ${model}`;
    if (year < 2000) return `Intel Pentium ${year < 1997 ? 'II' : 'III'} Xeon ${model}`;
    if (year < 2006) return `Intel Xeon DP ${model}`;
    return `Intel Xeon ${year < 2017 ? 'E5' : year < 2023 ? 'Scalable' : 'Max'} ${model}`;
  }
  const ramCaps = year => year <= 1994 ? [.016,.032,.064,.128] : year <= 1999 ? [.032,.064,.128,.256] : year <= 2004 ? [.128,.256,.512,1] : year <= 2009 ? [.5,1,2,4] : year <= 2014 ? [2,4,8,16] : year <= 2019 ? [4,8,16,32] : year <= 2023 ? [8,16,32,64] : [16,32,64,128];
  const diskCaps = year => year <= 1994 ? [.1,.2,.5,1] : year <= 1999 ? [.5,1,2,4] : year <= 2004 ? [4,10,20,40] : year <= 2009 ? [40,80,160,300] : year <= 2014 ? [300,500,1000,2000] : year <= 2019 ? [1000,2000,4000,8000] : year <= 2023 ? [2000,4000,8000,16000] : [4000,8000,16000,30000];
  const coolerFor = tdp => tdp <= 40 ? 'cool40' : tdp <= 75 ? 'cool75' : tdp <= 130 ? 'cool130' : tdp <= 200 ? 'cool200' : tdp <= 320 ? 'cool320' : tdp <= 450 ? 'cool450' : tdp <= 700 ? 'cool700' : tdp <= 1000 ? 'cool1000' : tdp <= 1600 ? 'cool1600' : 'cool2400';
  const psuFor = needed => ['psu300','psu500','psu750','psu1100','psu1300','psu1600','psu2000','psu2400','psu3000','psu4000','psu5000','psu6000','psu8000'].find(id => P.find(part => part.id === id).watts >= needed) || 'psu8000';
  function ramPart(year, type, tier) {
    const capacity = ramCaps(year)[tier];
    const id = `ram_era_${year}_${tier}`;
    return addPart({ id, type:'ram', name:`${type} ${capacity < 1 ? `${Math.round(capacity * 1024)} МБ` : `${capacity} ГБ`} ECC ${year}`, short:`${capacity < 1 ? `${Math.round(capacity * 1024)} МБ` : `${capacity} ГБ`} · ${type}`, icon:'ram', value:Math.round(40 + capacity * (120 + (year - 1990) * 9)), ramType:type, capacity, watts:round(1 + capacity * .08,1), year, brand:'samsung' });
  }
  function diskPart(year, tier) {
    let capacity = diskCaps(year)[tier];
    const ssd = year >= 2009 && tier >= 2;
    if (ssd && year < 2016) capacity = Math.max(.064, capacity / 8);
    const id = `disk_era_${year}_${tier}`;
    const connection = year >= 2016 && ssd ? 'NVMe U.2' : ssd ? 'SATA SSD' : year <= 2002 ? 'SCSI' : 'SATA';
    return addPart({ id, type:ssd ? 'ssd' : 'hdd', name:`${ssd ? 'SSD' : 'HDD'} ${capacity < 1 ? `${Math.round(capacity * 1024)} МБ` : `${capacity} ГБ`} ${connection} ${year}`, short:`${capacity < 1 ? `${Math.round(capacity * 1024)} МБ` : `${capacity} ГБ`} · ${connection}`, icon:ssd ? 'ssd' : 'hdd', value:Math.round(60 + capacity * (ssd ? 18 : 4)), capacity, watts:ssd ? 8 : 10, speed:round(ssd ? 2 + (year - 2009) * .16 : .6 + (year - 1990) * .025,2), year, brand:ssd ? 'samsung' : 'seagate' });
  }
  function gpuPart(year, tier) {
    const id = `gpu_era_${year}_${tier}`;
    const caps = year <= 2009 ? [.25,.5,1,2] : year <= 2015 ? [1,2,4,8] : year <= 2020 ? [4,8,16,32] : [8,24,80,192];
    const vram = caps[tier];
    const tdp = Math.round(25 + (year - 2006) * 4 + tier * (35 + (year - 2006) * 3));
    const compute = round((tier + 1) * Math.max(.5, (year - 2005) * 1.25),1);
    const brand = tier % 3 === 2 ? 'amd' : 'nvidia';
    return addPart({ id, type:'gpu', name:`${brand === 'amd' ? 'AMD Radeon Instinct' : year < 2012 ? 'NVIDIA Tesla' : year < 2020 ? 'NVIDIA Tesla / Quadro' : 'NVIDIA Data Center GPU'} ${year}-${tier + 1}`, short:`${vram} ГБ VRAM · ${tdp}W`, icon:'gpu', value:Math.round(1200 + compute * compute * 180 + vram * 300), tdp, vram, compute, watts:tdp, year, brand });
  }

  const roles = year => year < 2000 ? ['Workgroup','Department','Enterprise','File Server','Compute','Database'] : year < 2010 ? ['Rack','Tower','Blade','Storage','Web','Database'] : year < 2020 ? ['Virtualization','Cloud','Storage','HPC','Blade','Rack'] : ['AI','Cloud Native','NVMe','Edge','HPC','GPU'];
  const editions = ['A','B','C','D','E','F','G','H'];
  let generatedServers = 0;
  for (let year = 1990; year <= 2026; year += 1) {
    const eligible = families.filter(family => year >= family.start && year <= family.end);
    for (let slot = 0; slot < 68; slot += 1) {
      const family = eligible[(slot + year) % eligible.length];
      const model = family.models[(slot * 3 + year) % family.models.length];
      const roleList = roles(year);
      const role = roleList[slot % roleList.length];
      const id = `era_${year}_${String(slot + 1).padStart(2,'0')}`;
      if (templateIds.has(id)) continue;
      const tier = slot % 4;
      const arch = family.arch;
      const socket = socketFor(year, arch, slot);
      const cores = coresFor(year, slot, arch);
      const tdp = Math.round(14 + cores * (year < 2005 ? 18 : 2.4) + Math.max(0, year - 2015) * 2);
      const performance = round(cores * (.3 + (year - 1990) * .034) * (arch === 'x86' ? 1 : .88),1);
      const cpuId = `cpu_${id}`;
      const cpuValue = Math.round(180 + cores * cores * 42 + performance * 780 + (year - 1990) * 55);
      addPart({ id:cpuId, type:'cpu', name:`${cpuName(year,arch,socket,cores,slot)} · ${cores}C`, short:`${cores} ядер · ${tdp}W`, icon:'cpu', value:cpuValue, socket, cores, tdp, performance, year, brand:/SP3|SP5|SOCKET940|AM3/.test(socket) ? 'amd' : family.brand === 'arm' ? 'arm' : 'intel' });
      const type = ramType(year);
      const ram = ramPart(year,type,tier);
      const ramSlots = year < 1996 ? 2 + tier * 2 : year < 2006 ? 4 + tier * 2 : year < 2016 ? 8 + tier * 4 : 12 + tier * 4;
      const ramCount = 1 + ((slot + year) % Math.min(4,ramSlots));
      const disk = diskPart(year,tier);
      const driveSlots = Math.min(24,2 + tier * 2 + Math.floor((year - 1990) / 6));
      const diskCount = 1 + ((slot * 2 + year) % Math.min(4,driveSlots));
      const disks = Array(diskCount).fill(disk.id);
      const gpuAllowed = year >= 2006 && slot % 6 === 0;
      const gpus = gpuAllowed ? Array(slot % 12 === 0 && year >= 2018 ? 2 : 1).fill(gpuPart(year,tier).id) : [];
      const pcieSlots = year < 2005 ? 0 : gpuAllowed ? Math.min(4,Math.max(gpus.length,1 + tier)) : Math.min(4,Math.floor(tier / 2));
      const chassisValue = Math.round(500 + (year - 1990) * 180 + ramSlots * 130 + driveSlots * 100 + pcieSlots * 350);
      const name = `${family.prefix} ${model} ${role} ${editions[Math.floor(slot / 8) % editions.length]} · ${year}`;
      addPart({ id, type:'chassis', name, short:`${family.prefix} ${model} · ${socket}`, icon:/Tower|Edge/.test(role) ? 'chassis' : 'server', value:chassisValue, socket, ramType:type, ramSlots, sataPorts:driveSlots, basePower:Math.round(8 + ramSlots * 1.4 + driveSlots * 1.2), baseNoise:Math.round(24 + tier * 7 + Math.max(0,year - 1995) * .45), coolingFactor:round(.78 + (year - 1990) * .014 + (role === 'AI' ? .18 : 0),2), pcieSlots, year, brand:family.brand });
      const gpuTdp = gpus.reduce((sum,gpuId) => sum + P.find(part => part.id === gpuId).tdp,0);
      const needed = Math.round(70 + tdp + cores * 35 + diskCount * 10 + ramCount * 4 + gpuTdp * 1.25);
      const cooler = coolerFor(tdp + gpuTdp * .35);
      const psu = psuFor(needed);
      const partValue = [cpuValue,chassisValue,ram.value * ramCount,disk.value * diskCount,...gpus.map(gpuId => P.find(part => part.id === gpuId).value)].reduce((a,b) => a + b,0);
      templateIds.add(id);
      T.push({ id, name, chassis:id, cpu:cpuId, ram:Array(ramCount).fill(ram.id), disks, cooler, psu, gpus, basePrice:Math.round(partValue * 1.12 / 100) * 100, weight:year >= 2018 && role === 'AI' ? 1 : 2, year, brand:family.brand, role });
      generatedServers += 1;
    }
  }

  // Каталог сервисов: 240 семейств × 9 рабочих редакций.
  const familyLists = {
    web:['Apache HTTP Server','NGINX Plus','Caddy','Lighttpd','Cherokee','Hiawatha','OpenResty','Traefik','Envoy Proxy','HAProxy','Varnish Cache','Apache Tomcat','Jetty','Undertow','Microsoft IIS','Node.js Runtime','Deno Runtime','Bun Runtime','Gunicorn','PHP-FPM'],
    database:['MySQL','MariaDB','PostgreSQL','SQLite','MongoDB','Redis','Valkey','CouchDB','Apache Cassandra','ScyllaDB','ClickHouse','DuckDB','InfluxDB','TimescaleDB','Neo4j','CockroachDB','Oracle Database','Microsoft SQL Server','Firebird','IBM Db2'],
    cloud:['Nextcloud','ownCloud','Seafile','Syncthing','MinIO','Ceph RADOS Gateway','OpenStack Swift','OpenStack Nova','OpenStack Cinder','Garage S3','File Browser Plus','SFTPGo','Rclone Serve','Resilio Sync','Pydio Cells','Samba AD','FreeIPA','Keycloak','Authentik','Vaultwarden Plus'],
    devops:['Docker Engine','Podman','containerd','Kubernetes','K3s','OpenShift OKD','Rancher','Portainer','Docker Swarm','HashiCorp Nomad','Terraform','Ansible AWX','Jenkins','GitLab CE','Gitea','Drone CI','Argo CD','Harbor Registry','Forgejo','Woodpecker CI'],
    monitor:['Prometheus','Grafana','Zabbix','Nagios Core','Icinga 2','LibreNMS','Observium','Netdata','Checkmk','Uptime Kuma','Graylog','OpenSearch','Loki','Vector','Fluent Bit','Telegraf','Elastic APM','Jaeger','OpenTelemetry Collector','Sentry'],
    security:['Pi-hole','AdGuard Home','Unbound DNS','BIND9','PowerDNS','WireGuard','OpenVPN','IPsec strongSwan','Tailscale Headscale','OPNsense Sensei','Suricata IDS','Snort IDS','Zeek NSM','Wazuh SIEM','Falco','ClamAV','HashiCorp Vault','Step CA','CrowdSec','ModSecurity WAF'],
    media:['Jellyfin','Plex','Emby','Kodi Headless','Navidrome','Airsonic-Advanced','Subsonic','Ampache','Funkwhale','Immich','PhotoPrism','LibrePhotos','Frigate NVR','Shinobi CCTV','ZoneMinder','MotionEye','Restreamer','Owncast','PeerTube','Icecast'],
    ai:['Ollama','LocalAI','llama.cpp Server','vLLM','Text Generation WebUI','Stable Diffusion WebUI','ComfyUI','Fooocus','InvokeAI','Whisper.cpp','Faster Whisper','Speaches','Open WebUI','AnythingLLM','Dify','Flowise','Langfuse','Haystack','MLflow','JupyterHub'],
    games:['Minecraft Vanilla','Minecraft Paper','Minecraft Spigot','Minecraft Forge','Fabric Server','Valheim','Rust Dedicated','Factorio','ARK Survival','Palworld','7 Days to Die','Terraria','TShock','Counter-Strike 2','Team Fortress 2','SA-MP','FiveM','OpenTTD','Xonotic','Minetest'],
    business:['WordPress','Drupal','Joomla','Ghost','MediaWiki','BookStack','Outline','Wiki.js','Discourse','Flarum','phpBB','Mattermost','Rocket.Chat','Zulip','Mautic','ERPNext','Odoo','Dolibarr','Invoice Ninja','Kimai'],
    network:['Mailcow','Postfix','Dovecot','Haraka','Stalwart Mail','Rspamd','Roundcube','SOGo','OpenSMTPD','ProFTPD','vsftpd','NFS Server','iSCSI Target','FreeRADIUS','NTPsec','Chrony','DHCP Kea','TFTP Server','Coturn','XMPP Prosody']
  };
  const profiles = {
    web:[.5,8,45,'apps'],database:[2,18,120,'apps'],cloud:[1,12,80,'files'],devops:[1.5,20,110,'apps'],
    monitor:[1,10,70,'stats'],security:[.5,8,55,'shield'],media:[2,25,140,'jellyfin'],ai:[8,65,450,'ai'],
    games:[2,45,180,'apps'],business:[1,15,90,'files'],network:[.6,10,65,'network']
  };
  const categoryNames = { web:'Web и прокси',database:'Базы данных',cloud:'Облако и файлы',devops:'DevOps',monitor:'Мониторинг',security:'Сеть и безопасность',media:'Медиа и видео',ai:'AI и ML',games:'Игровые серверы',business:'CMS и бизнес',network:'Почта и сеть',server:'Серверные сервисы',legacy:'Legacy-сервисы' };
  const releases = [
    ['legacy','Legacy',.25,.4,.35,1998],['classic','Classic',.5,.6,.5,2003],['standard','Standard',1,1,1,2008],
    ['workgroup','Workgroup',1.25,1.1,1.25,2013],['enterprise','Enterprise',2,1.5,1.8,2018],['cluster','Cluster',3,1.8,2.4,2021],
    ['cloud','Cloud',1.5,1.25,1.6,2023],['pro','Pro',2.5,1.7,2.1,2025],['next','Next',4,2,3,2026]
  ];
  const slug = text => text.toLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');
  function serviceBrand(name, icon) {
    const text = name.toLowerCase();
    const brand = Object.keys(window.BRAND_COLORS).find(id => id.length > 3 && text.includes(id));
    return brand || icon;
  }
  let generatedServices = 0;
  Object.entries(familyLists).forEach(([category,names]) => {
    const [baseRam,baseCpu,baseRate,icon] = profiles[category];
    names.forEach((name,familyIndex) => {
      releases.forEach(([release,label,ramMul,cpuMul,rateMul,year]) => {
        const id = `svc_${category}_${familyIndex}_${release}`;
        if (appIds.has(id)) return;
        const ram = Math.max(.025,round(baseRam * ramMul,3));
        const os = category === 'devops' ? [...LINUX,'esxi','xcpng'] : category === 'security' || category === 'network' ? [...LINUX,'freebsd','pfsense','openwrt'] : category === 'cloud' || category === 'media' ? [...MODERN_UNIX,...WINDOWS] : [...LINUX,...WINDOWS];
        appIds.add(id);
        APPS.push({ id, name:`${name} · ${label}`, family:name, brand:serviceBrand(name,icon), icon, category, release:label, year,
          ram, cpu:Math.min(100,Math.round(baseCpu * cpuMul)), rate:Math.max(5,Math.round(baseRate * rateMul / 5) * 5), minRam:Math.max(.05,round(ram * .8,3)), os,
          description:`${categoryNames[category]} · пакет ${label}`, gpuBoost:(category === 'ai' || category === 'media') && ['enterprise','cluster','pro','next'].includes(release) });
        generatedServices += 1;
      });
    });
  });

  const legacyGroups = {
    unix:['freebsd1','solaris26','debian11','redhat52','slackware3','sco5','irix65','aix43','hpux11'],
    linux:['debian11','debian30','redhat52','slackware3','suse60','freebsd1','freebsd4'],
    nt:['nt31','nt40','win2000','server2003'],
    netware:['netware3','netware4','netware5','netware65'],
    os2:['os2warp']
  };
  const legacyApps = [
    ['NCSA HTTPd 1.5','unix',.016,4,8],['CERN HTTPd 3.0','unix',.016,3,7],['Apache 1.3','linux',.032,5,12],
    ['Netscape Enterprise Server','nt',.064,8,18],['Microsoft IIS 4.0','nt',.064,7,16],['BIND 4.9','unix',.016,2,6],
    ['Sendmail 8','unix',.032,4,9],['WU-FTPD 2.6','unix',.016,3,6],['Telnet Daemon','unix',.008,1,3],
    ['INN News Server','unix',.064,6,12],['Qpopper POP3','unix',.016,2,5],['NFS 2.0 Server','unix',.032,4,9],
    ['NIS+ Directory','unix',.032,3,8],['Samba 1.9','linux',.032,4,10],['Squid 1.0 Proxy','linux',.064,7,14],
    ['NetWare File Services','netware',.032,5,15],['NetWare Print Services','netware',.016,3,8],['NDS Directory','netware',.064,5,16],
    ['LAN Server for OS/2','os2',.032,4,12],['Lotus Domino 4.6','nt',.128,12,28],['Microsoft Exchange 5.5','nt',.128,10,26],
    ['Microsoft SQL Server 6.5','nt',.128,12,30],['Oracle7 Workgroup Server','unix',.128,14,32],['IBM DB2 UDB 5','nt',.128,12,28],
    ['Gopher Server','unix',.008,1,3],['WAIS Index Server','unix',.016,2,4],['IRC Hybrid 6','unix',.016,2,5],
    ['DOOM II Dedicated','nt',.032,15,12],['QuakeWorld Server','linux',.064,22,18],['Counter-Strike 1.6','nt',.128,30,35]
  ];
  legacyApps.forEach(([name,group,ram,cpu,rate],index) => {
    const id = `legacy_service_${index}`;
    appIds.add(id);
    APPS.push({ id, name, family:name, brand:serviceBrand(name,'server'), icon:'apps', category:'legacy', release:'Historical', year:1990 + index % 14,
      ram, cpu, rate, minRam:ram, os:legacyGroups[group], description:`Legacy · ${group.toUpperCase()} · архивный сервис` });
    generatedServices += 1;
  });

  // Map-индексы не дают большому каталогу замедлить тик симуляции.
  const partMap = new Map(P.map(part => [part.id,part]));
  const templateMap = new Map(T.map(template => [template.id,template]));
  const osMap = new Map(OS.map(os => [os.id,os]));
  const appMap = new Map(APPS.map(app => [app.id,app]));
  DATA.part = id => partMap.get(id);
  DATA.template = id => templateMap.get(id);
  DATA.os = id => osMap.get(id);
  DATA.software = id => appMap.get(id);
  DATA.SERVICE_CATEGORIES = categoryNames;
  DATA.ERA_RANGES = [['all','Все годы',0,9999],['1990s','1990–1999',1990,1999],['2000s','2000–2009',2000,2009],['2010s','2010–2019',2010,2019],['2020s','2020–2026',2020,2026]];
  DATA.MEGA_STATS = { generatedServers, generatedServices, startYear:1990, endYear:2026 };
  DATA.megaLoaded = true;
})();
