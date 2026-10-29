import {
  FLAW_INFO,
  LOCATION_INFO,
  OS_OVERHEAD,
  SERVICES,
} from './data';
import type {
  Contract,
  GameState,
  Machine,
  Part,
  ServiceDef,
  Stage,
} from './types';

export interface Derived {
  cores: number;
  ramMB: number;
  usableRam: number;
  usedRam: number;
  storageGB: number;
  usedStorageGB: number;
  watts: number;
  noise: number;
  temp: number;
  up: boolean;
  firewall: number;
  issues: string[];
  flaws: Part[];
  memType: string;
  hasILO: boolean;
}

const DUST_HEAT = 4;
export const AMBIENT = { room: 29, garage: 23, dc: 19 } as const;

export function sumParts(parts: (Part | null)[]): Part[] {
  return parts.filter((p): p is Part => !!p);
}

export function machineCores(m: Machine): number {
  const cpus = sumParts(m.cpus);
  const builtin = m.base.stats.baseCores ?? 0;
  if (!cpus.length) return builtin;
  return builtin + cpus.reduce((acc, c) => {
    const cores = c.stats.cores ?? 0;
    return acc + (c.flaw === 'bentpins' ? Math.max(1, Math.ceil(cores / 2)) : cores);
  }, 0);
}

export function machineRam(m: Machine): number {
  return (m.base.stats.baseRamMB ?? 0) + sumParts(m.ram).reduce((a, p) => a + (p.stats.ramMB ?? 0), 0);
}

export function machineStorage(m: Machine): number {
  const disks = sumParts(m.storage).map((p) => {
    const cap = p.stats.capacityGB ?? 0;
    return p.flaw === 'badsectors' ? Math.round(cap * 0.65) : cap;
  });
  let total = disks.reduce((a, b) => a + b, 0);
  if (m.raid && m.raid.healthy && disks.length >= 2) {
    if (m.raid.level === 1) total = Math.floor(total / 2);
    if (m.raid.level === 5 && disks.length >= 3) total = total - Math.min(...disks);
  }
  return (m.base.stats.baseStorageGB ?? 0) + total;
}

export function machineWatts(m: Machine, loadFactor = 0.3): number {
  const cpus = sumParts(m.cpus);
  const base = m.base.stats.baseWatts ?? 20;
  const cpu = cpus.reduce((a, c) => a + (c.stats.tdp ?? 0), 0);
  const ram = sumParts(m.ram).length * 3;
  const disks = sumParts(m.storage).length * 5;
  const nic = m.nic?.stats.watts ?? 0;
  const cooling = m.cooler?.stats.watts ?? 2;
  const idle = base + cpu * 0.35 + ram + disks + nic + cooling;
  const total = idle * (1 + 0.55 * loadFactor);
  const flawed = sumParts(m.cpus).some((c) => c.flaw === 'bentpins');
  return Math.round(total * (flawed ? 1.12 : 1));
}

export function serviceLoad(serviceIds: string[]) {
  return serviceIds.reduce(
    (acc, id) => {
      const s = SERVICES[id];
      if (!s) return acc;
      acc.ram += s.ramMB;
      acc.cores += s.cores;
      acc.storage += s.storageGB;
      return acc;
    },
    { ram: 0, cores: 0, storage: 0 },
  );
}

export function machineLoad(m: Machine) {
  return serviceLoad(m.services);
}

export function machineStorageCap(m: Machine): number {
  return machineStorage(m);
}

export function osStorageReserve(m: Machine): number {
  if (m.os === 'none') return 0;
  if (m.os === 'android') return 1;
  return 12;
}

export function osRamOverhead(m: Machine): number {
  return OS_OVERHEAD[m.os];
}

export function usableRam(m: Machine): number {
  return Math.max(0, machineRam(m) - osRamOverhead(m) - 64);
}

export function machineUtil(m: Machine): number {
  if (!m.services.length) return 0.05;
  const load = machineLoad(m);
  const ram = Math.max(1, usableRam(m));
  const cores = Math.max(1, machineCores(m));
  const disk = Math.max(1, machineStorageCap(m));
  return Math.max(load.ram / ram, load.cores / cores, load.storage / disk, 0.05);
}

export function machineNoise(m: Machine): number {
  if (!m.powered) return 0;
  const base = m.base.stats.baseNoise ?? 20;
  const coolers = sumParts([m.cooler]).reduce((a, c) => a + (c.stats.noise ?? 0), 0);
  const fanFlaw = sumParts([m.base]).some((b) => b.flaw === 'noisyfan') ? 6 : 0;
  return base + coolers + fanFlaw;
}

export function machineTemp(m: Machine): number {
  const util = machineUtil(m);
  const ambient = AMBIENT[m.location] + m.dust * DUST_HEAT;
  const cooling = (m.cooler?.stats.cooling ?? 6) + (m.base.stats.cooling ?? 8);
  const cpuTdp = sumParts(m.cpus).reduce((a, c) => a + (c.stats.tdp ?? 0), 0);
  const heat = cpuTdp * 0.09 * (1 + util) + (m.base.stats.baseWatts ?? 20) * 0.04;
  return Math.round((ambient + heat - cooling * 0.55) * 10) / 10;
}

export function flawList(m: Machine): Part[] {
  return [m.base, ...sumParts(m.cpus), ...sumParts(m.ram), ...sumParts(m.storage), m.nic, m.cooler].filter(
    (p): p is Part => !!p && p.flaw !== 'none',
  );
}

export function derive(m: Machine, state?: GameState): Derived {
  const flaws = flawList(m);
  const ram = machineRam(m);
  const usable = usableRam(m);
  const load = machineLoad(m);
  const storageGB = machineStorageCap(m);
  const issues: string[] = [];
  const up = m.powered && !m.crashed && m.os !== 'none' && machineCores(m) > 0;

  if (!m.os || m.os === 'none') issues.push('ОС не установлена');
  if (machineCores(m) <= 0) issues.push('Нет процессора');
  if (ram === 0) issues.push('Нет памяти');
  if (!sumParts(m.storage).length) issues.push('Нет дисков');
  if (m.os !== 'none' && usable <= 0) issues.push('Памяти меньше, чем требует ОС');
  if (load.ram > usable) issues.push(`Не хватает ОЗУ: ${load.ram} МБ нужно, ${usable} МБ есть`);
  if (load.cores > machineCores(m)) issues.push('Не хватает ядер CPU');
  if (load.storage > storageGB) issues.push('Не хватает места на дисках');
  if (m.temp >= 85) issues.push(`Перегрев ${Math.round(m.temp)} °C`);
  flaws.forEach((p) => issues.push(FLAW_INFO[p.flaw].name));

  let firewall = 8;
  if (state) firewall = firewallScore(state);
  else {
    firewall += m.services.length ? 5 : 0;
    firewall += m.os === 'linux' ? 10 : 0;
  }

  return {
    cores: machineCores(m),
    ramMB: ram,
    usableRam: usable,
    usedRam: load.ram,
    storageGB,
    usedStorageGB: load.storage + osStorageReserve(m),
    watts: machineWatts(m, machineUtil(m)),
    noise: machineNoise(m),
    temp: m.temp,
    up,
    firewall,
    issues,
    flaws,
    memType: m.ram.find((r) => r)?.stats.memType ?? '—',
    hasILO: m.ilo,
  };
}

export function firewallScore(state: GameState): number {
  let fw = 8;
  if (state.upgrades.includes('router')) fw += 6;
  if (state.upgrades.includes('firewall')) fw += 22;
  if (state.upgrades.includes('monitoring')) fw += 8;
  if (state.upgrades.includes('switch')) fw += 8;
  if (state.upgrades.includes('switch10g')) fw += 10;
  fw += Math.min(20, state.skills.network * 3);
  fw += Math.min(14, state.machines.reduce((a, m) => a + m.firewall, 0));
  fw += Math.min(10, state.rep * 0.2);
  return Math.min(100, Math.round(fw));
}

export function totalNoise(state: GameState): number {
  const powered = state.machines.filter((m) => m.powered);
  const raw = powered.reduce((a, m) => a + machineNoise(m), 0);
  const reduction = state.upgrades.includes('soundproof') ? 15 : 0;
  return Math.max(0, raw - reduction);
}

export function totalWatts(state: GameState): number {
  return state.machines.reduce((a, m) => a + (m.powered ? machineWatts(m, machineUtil(m)) : 0), 0);
}

export function tempPenalty(state: GameState): number {
  const ac = state.upgrades.includes('ac') ? 12 : 0;
  const boost = state.upgrades.includes('employees') ? 4 : 0;
  return ac + boost;
}

export function machineAmbient(state: GameState, m: Machine): number {
  return AMBIENT[m.location] + m.dust * DUST_HEAT - tempPenalty(state);
}

export function stageFor(value: number): Stage {
  return value >= 3 ? 3 : value >= 2 ? 2 : 1;
}

export function machineStage(m: Machine): Stage {
  return stageFor(m.base.tier);
}

export function locationLabel(state: GameState): string {
  return LOCATION_INFO[state.location].name;
}

export function machineTitle(m: Machine): string {
  return m.name;
}

export function cpuModel(m: Machine): string {
  const cpu = m.cpus.find(Boolean);
  return cpu ? cpu.name : '—';
}

export function capacitySummary(m: Machine) {
  const d = derive(m);
  const load = machineLoad(m);
  return {
    ram: `${load.ram} / ${d.usableRam} МБ`,
    cores: `${Math.round(load.cores * 10) / 10} / ${d.cores}`,
    storage: `${load.storage + (m.os !== 'none' ? 12 : 0)} / ${d.storageGB} ГБ`,
  };
}

export function contractMachine(state: GameState, contract: Contract): Machine | null {
  const candidates = state.machines.filter((m) => {
    const d = derive(m, state);
    return d.up && d.temp < 92;
  });
  for (const m of candidates) {
    const ok = serviceFit(m, contract.requirements.services ?? []);
    if (!ok) continue;
    const d = derive(m, state);
    if (contract.requirements.minRamMB && machineRam(m) < contract.requirements.minRamMB) continue;
    if (contract.requirements.minCores && d.cores < contract.requirements.minCores) continue;
    if (contract.requirements.minStorageGB && d.storageGB < contract.requirements.minStorageGB) continue;
    if (contract.requirements.os && m.os !== contract.requirements.os) continue;
    if (contract.requirements.kinds && !contract.requirements.kinds.includes(m.kind)) continue;
    const otherLoad = state.contracts
      .filter((c) => c.active && c.uid !== contract.uid && c.machineId === m.id)
      .reduce(
        (acc, c) => {
          acc.ram += c.load.ramMB;
          acc.cores += c.load.cores;
          acc.storage += c.load.storageGB;
          return acc;
        },
        { ram: 0, cores: 0, storage: 0 },
      );
    const load = machineLoad(m);
    const freeRam = d.usableRam - load.ram - otherLoad.ram;
    const freeCores = d.cores - load.cores - otherLoad.cores;
    const freeStorage = d.storageGB - load.storage - osStorageReserve(m) - otherLoad.storage;
    if (freeRam < contract.load.ramMB) continue;
    if (freeCores < contract.load.cores) continue;
    if (freeStorage < contract.load.storageGB) continue;
    return m;
  }
  return null;
}

export function serviceFit(m: Machine, services: string[]): boolean {
  return services.every((s) => m.services.includes(s));
}

export function contractBlocker(state: GameState, contract: Contract): string {
  const req = contract.requirements;
  const upMachines = state.machines.filter((m) => {
    const d = derive(m, state);
    return d.up;
  });
  if (!upMachines.length) return 'Нет включённого сервера с ОС';
  if (req.firewall && firewallScore(state) < req.firewall) return `Нужен firewall ≥ ${req.firewall}`;
  if (req.needsSwitch && !state.upgrades.includes('switch')) return 'Нужен коммутатор Cisco';
  if (req.needsBackup && !state.upgrades.includes('backup')) return 'Нужны бэкапы (внешний диск)';
  if (req.upgrades) {
    for (const u of req.upgrades) {
      if (!state.upgrades.includes(u)) return `Нужен апгрейд: ${u === 'router' ? 'роутер MikroTik' : u}`;
    }
  }
  if (req.minMachines && upMachines.length < req.minMachines) return `Нужно минимум ${req.minMachines} рабочих машин`;
  if (req.cables && state.cables < req.cables) return `Нужны обжатые кабели (${state.cables}/${req.cables})`;
  if (req.services) {
    for (const s of req.services) {
      const hasService = state.machines.some((m) => m.services.includes(s));
      if (!hasService) return `Не установлен сервис: ${SERVICES[s]?.name ?? s}`;
    }
  }
  for (const m of upMachines) {
    if (req.kinds && !req.kinds.includes(m.kind)) continue;
    const d = derive(m, state);
    if (req.minRamMB && machineRam(m) < req.minRamMB) continue;
    if (req.minCores && d.cores < req.minCores) continue;
    if (req.minStorageGB && d.storageGB < req.minStorageGB) continue;
    if (req.os && m.os !== req.os) continue;
    if (req.services && !serviceFit(m, req.services)) continue;
    const load = machineLoad(m);
    const freeRam = d.usableRam - load.ram;
    const freeCores = d.cores - load.cores;
    const freeStorage = d.storageGB - load.storage - osStorageReserve(m);
    if (freeRam < contract.load.ramMB) return `Не хватает свободной ОЗУ на ${m.name}`;
    if (freeCores < contract.load.cores) return 'Не хватает ядер CPU';
    if (freeStorage < contract.load.storageGB) return 'Не хватает места на дисках';
    return '';
  }
  return 'Нет машины, подходящей по железу';
}

export function contractAvailable(state: GameState, contract: Contract): boolean {
  const req = contract.requirements;
  if (req.firewall && firewallScore(state) < req.firewall) return false;
  if (req.needsSwitch && !state.upgrades.includes('switch')) return false;
  if (req.needsBackup && !state.upgrades.includes('backup')) return false;
  if (req.upgrades) {
    for (const u of req.upgrades) if (!state.upgrades.includes(u)) return false;
  }
  for (const s of req.services ?? []) {
    const exists = state.machines.some((m) => m.services.includes(s));
    if (!exists) return false;
  }
  const kinds = req.kinds;
  const machines = state.machines.filter((m) => !kinds || kinds.includes(m.kind));
  if (!machines.length) return false;
  if (!machines.some((m) => derive(m, state).up)) return false;
  if (!contractMachine(state, contract)) {
    const blocker = contractBlocker(state, contract);
    return blocker === '' ? false : false;
  }
  return true;
}

export function canInstallService(m: Machine, service: ServiceDef): string {
  if (!m.powered) return 'Сначала включи сервер';
  if (m.crashed) return 'Сервер в аварии — нужен ремонт';
  if (m.os === 'none') return 'Сначала установи ОС';
  if (service.phoneOnly) {
    if (m.kind !== 'phone') return 'Этот сервис ставится только на смартфон';
    if (m.os !== 'android') return 'На смартфоне не Android';
    return '';
  }
  if (m.kind === 'phone') return 'Смартфон не тянет серверные сервисы';
  if (service.needsLinux && m.os !== 'linux') return 'Нужен Linux';
  if (service.needsWindows && m.os !== 'windows') return 'Нужен Windows Server';
  if (m.os === 'linux' && !service.linuxPackage) return 'Для Linux нужен пакет из apt';
  if (m.os === 'linux') return '';
  if (m.os === 'windows') {
    const allowed = ['smb', 'jellyfin', 'webserver', 'cctv'];
    return allowed.includes(service.id) ? '' : 'Этот сервис ставится только на Linux';
  }
  return 'Неизвестная ОС';
}

export function formatMoney(v: number): string {
  const sign = v < 0 ? '−' : '';
  const abs = Math.abs(Math.round(v));
  return `${sign}${abs.toLocaleString('ru-RU')} ₽`;
}

export function pct(v: number): string {
  return `${Math.round(v * 100)}%`;
}

export function osName(os: Machine['os']): string {
  return os === 'linux' ? 'Ubuntu Server 22.04' : os === 'windows' ? 'Windows Server 2016' : os === 'android' ? 'Android 2.3' : 'нет ОС';
}

export function dayLabel(day: number): string {
  return `День ${day}`;
}
