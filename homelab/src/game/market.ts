import { MARKET_FLAVOR, PART_TEMPLATES, SELLERS, SERVICES } from './data';
import type { Contract, ContractDef, Flaw, GameState, Listing, Machine, Part, PartKind, Stage } from './types';

let uidCounter = 0;
export function uid(prefix = 'p'): string {
  uidCounter += 1;
  return `${prefix}_${Date.now().toString(36)}_${uidCounter}`;
}

export function rnd(n: number): number {
  return Math.floor(Math.random() * n);
}
export function pick<T>(arr: T[]): T {
  return arr[rnd(arr.length)];
}
export function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v));
}
export function chance(p: number): boolean {
  return Math.random() < p;
}

export function makePart(defId: string, opts: { flaw?: Flaw; condition?: number; price?: number } = {}): Part {
  const t = PART_TEMPLATES.find((p) => p.defId === defId);
  if (!t) throw new Error(`Unknown part ${defId}`);
  const condition = opts.condition ?? t.cond[0] + rnd(Math.max(1, t.cond[1] - t.cond[0]));
  let flaw: Flaw = opts.flaw ?? 'none';
  if (!opts.flaw) {
    if (t.flaws.length && chance(t.flawChance)) flaw = pick(t.flaws);
  }
  let price = opts.price ?? Math.round((t.price * (0.55 + condition / 200)) / 10) * 10;
  if (flaw === 'badsectors') price = Math.round(price * 0.7);
  if (flaw === 'bentpins') price = Math.round(price * 0.8);
  if (flaw === 'weakpsu') price = Math.round(price * 0.78);
  return {
    uid: uid('part'),
    defId: t.defId,
    name: t.name,
    kind: t.kind,
    price,
    condition,
    machineKind: t.machineKind,
    flaw,
    flawKnown: false,
    stats: { ...t.stats },
    flavor: t.flavor,
    tier: t.tier,
  };
}

export function makeListing(part: Part, state: GameState): Listing {
  const seller = pick(SELLERS);
  const ask = Math.round((part.price * (0.95 + Math.random() * 0.5)) / 10) * 10;
  return {
    id: uid('lot'),
    part,
    seller: seller.name,
    sellerTag: seller.tag,
    askPrice: Math.max(50, ask),
    honest: chance(0.55),
    attempts: 0,
    revealed: false,
    sold: false,
    delivered: false,
    chat: [
      {
        from: 'seller',
        text: `${pick(MARKET_FLAVOR)} ${part.name}, состояние ${part.condition}%. Забирать на ${pick(['Северной', 'Ленина', 'Мира', 'Гагарина', 'Строителей'])}.`,
      },
    ],
    daysListed: 0,
  };
}

const STAGE_POOL: Record<Stage, string[]> = {
  1: ['base_compaq', 'base_dell', 'base_phone', 'cpu_i3', 'cpu_i5', 'ram_ddr3_2', 'ram_ddr3_4', 'ram_ddr3_8', 'hdd_320', 'hdd_500', 'hdd_2tb', 'ssd_120', 'cooler_tower', 'nic_gbit'],
  2: ['base_dl380', 'base_dl360', 'cpu_xeon_5640', 'cpu_i5', 'ram_ddr3_8', 'ram_ecc_8', 'hdd_2tb', 'ssd_120', 'ssd_1tb', 'sas_300', 'cooler_silent', 'cooler_tower', 'nic_dual', 'nic_gbit'],
  3: ['base_blade', 'base_storage', 'base_dl380', 'cpu_xeon_2670', 'cpu_gold', 'ram_ecc_16', 'ssd_1tb', 'hdd_8tb', 'sas_300', 'cooler_silent', 'nic_dual'],
};

export function generateListings(state: GameState, count: number): Listing[] {
  const out: Listing[] = [];
  const stages: Stage[] = state.stage >= 3 ? [1, 2, 3] : state.stage >= 2 ? [1, 2] : [1];
  const pool = stages.flatMap((s) => STAGE_POOL[s]);
  for (let i = 0; i < count; i += 1) {
    // Один раз в несколько лотов подкидываем что-то для следующего этапа
    const stage = chance(0.15) && state.stage < 3 ? ((state.stage + 1) as Stage) : pick(stages);
    const defs = pool.filter((d) => STAGE_POOL[stage].includes(d));
    const def = pick(defs.length ? defs : STAGE_POOL[state.stage]);
    const part = makePart(def);
    out.push(makeListing(part, state));
  }
  return out;
}

export function makeMachine(base: Part, state: GameState, name?: string): Machine {
  const kind = base.machineKind ?? 'desktop';
  const count = state.machines.filter((m) => m.kind === kind).length + 1;
  const defaults: Record<string, string> = {
    desktop: 'Домашний сервер',
    phone: 'Смартфон-сервер',
    rack: 'Стойка',
    blade: 'Блейд-шасси',
    storage: 'СХД',
  };
  return {
    id: uid('m'),
    name: name ?? `${defaults[kind]} ${count}`,
    kind,
    base,
    cpus: new Array(base.stats.cpuSlots ?? 1).fill(null),
    ram: new Array(base.stats.ramSlots ?? 2).fill(null),
    storage: new Array(base.stats.storageSlots ?? 2).fill(null),
    nic: null,
    cooler: null,
    os: 'none',
    installed: [],
    services: [],
    shares: [],
    raid: null,
    powered: false,
    crashed: false,
    temp: 25,
    uptime: 0,
    firewall: 0,
    ilo: false,
    desktopExperience: false,
    windowsLicensed: false,
    audioService: false,
    location: state.location,
    dust: rnd(3),
    notes: [],
    acquiredDay: state.day,
  };
}

export function makeContract(def: ContractDef, state: GameState): Contract {
  return {
    ...def,
    uid: uid('c'),
    acceptedDay: state.day,
    daysLeft: def.days,
    active: true,
    failed: false,
    payments: 0,
    machineId: null,
    missedDays: 0,
  };
}

export function installedApps(m: Machine): string[] {
  return m.services.filter((s) => SERVICES[s]);
}

export function serviceByName(name: string): string | null {
  const entry = Object.values(SERVICES).find((s) => s.linuxPackage === name || s.linuxUnit === name || s.id === name);
  return entry ? entry.id : null;
}
