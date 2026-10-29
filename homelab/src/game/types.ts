export type Stage = 1 | 2 | 3;
export type TabId = 'room' | 'market' | 'server' | 'jobs' | 'path';
export type Location = 'room' | 'garage' | 'dc';

export type PartKind =
  | 'base'
  | 'cpu'
  | 'ram'
  | 'storage'
  | 'cooler'
  | 'nic';

export type Flaw =
  | 'none'
  | 'badsectors'
  | 'coilwhine'
  | 'bentpins'
  | 'weakpsu'
  | 'noisyfan'
  | 'memoryerrors'
  | 'slowdisk'
  | 'deadbattery';

export type MachineKind = 'desktop' | 'rack' | 'blade' | 'storage' | 'phone';

export interface PartStats {
  cores?: number;
  ghz?: number;
  ramMB?: number;
  memType?: 'ddr3' | 'ddr4' | 'ecc';
  capacityGB?: number;
  tdp?: number;
  noise?: number;
  cooling?: number;
  reliability?: number;
  watts?: number;
  va?: number;
  ports?: number;
  speed?: number;
  cpuSlots?: number;
  ramSlots?: number;
  storageSlots?: number;
  rackUnits?: number;
  tier?: number;
  baseNoise?: number;
  baseWatts?: number;
  hasILO?: boolean;
  baseRamMB?: number;
  baseCores?: number;
  baseStorageGB?: number;
}

export interface Part {
  uid: string;
  defId: string;
  name: string;
  kind: PartKind;
  price: number;
  condition: number;
  machineKind?: MachineKind;
  flaw: Flaw;
  flawKnown: boolean;
  stats: PartStats;
  flavor: string;
  tier: number;
}

export interface RaidConfig {
  level: 1 | 5;
  disks: number;
  healthy: boolean;
  migrated?: boolean;
}

export interface Machine {
  id: string;
  name: string;
  kind: MachineKind;
  base: Part;
  cpus: (Part | null)[];
  ram: (Part | null)[];
  storage: (Part | null)[];
  nic: Part | null;
  cooler: Part | null;
  os: 'none' | 'linux' | 'windows' | 'android';
  installed: string[];
  services: string[];
  shares: string[];
  raid: RaidConfig | null;
  powered: boolean;
  crashed: boolean;
  temp: number;
  uptime: number;
  firewall: number;
  ilo: boolean;
  desktopExperience: boolean;
  windowsLicensed: boolean;
  audioService: boolean;
  location: Location;
  dust: number;
  notes: string[];
  acquiredDay: number;
}

export interface Listing {
  id: string;
  part: Part;
  seller: string;
  sellerTag: string;
  askPrice: number;
  honest: boolean;
  attempts: number;
  revealed: boolean;
  sold: boolean;
  delivered: boolean;
  chat: ChatMessage[];
  daysListed: number;
}

export interface ChatMessage {
  from: 'seller' | 'me' | 'system';
  text: string;
}

export interface GameUpgrade {
  id: string;
  name: string;
  icon: string;
  price: number;
  stage: number;
  desc: string;
}

export interface ServiceDef {
  id: string;
  name: string;
  icon: string;
  ramMB: number;
  cores: number;
  storageGB: number;
  desc: string;
  linuxPackage?: string;
  linuxUnit?: string;
  needsLinux?: boolean;
  needsWindows?: boolean;
  phoneOnly?: boolean;
  requiresCables?: boolean;
  requiresFirewall?: number;
}

export interface Requirements {
  services?: string[];
  os?: 'linux' | 'windows';
  minRamMB?: number;
  minCores?: number;
  minStorageGB?: number;
  firewall?: number;
  cables?: number;
  kinds?: MachineKind[];
  minMachines?: number;
  minUptime?: number;
  needsSwitch?: boolean;
  needsBackup?: boolean;
  upgrades?: string[];
}

export interface ContractDef {
  id: string;
  title: string;
  client: string;
  desc: string;
  stage: Stage;
  requirements: Requirements;
  load: { ramMB: number; cores: number; storageGB: number };
  setupPay: number;
  dailyPay: number;
  repReward: number;
  days: number;
  penalty: number;
  icon: string;
  trouble?: string;
}

export interface Contract extends ContractDef {
  uid: string;
  acceptedDay: number;
  daysLeft: number;
  active: boolean;
  failed: boolean;
  payments: number;
  machineId: string | null;
  missedDays: number;
}

export interface LogEntry {
  id: string;
  day: number;
  text: string;
  tone: 'info' | 'good' | 'bad' | 'warn' | 'cmd';
}

export interface EventEffect {
  text: string;
  money?: number;
  rep?: number;
  family?: number;
  skill?: Partial<Skills>;
  temp?: number;
  crash?: boolean;
  downtime?: boolean;
  breakDisk?: boolean;
  note?: string;
  addUpgrade?: string;
  givePart?: string;
  ddos?: boolean;
  cableGame?: boolean;
  dust?: number;
}

export interface EventChoice {
  label: string;
  hint?: string;
  cost?: number;
  effect: EventEffect;
}

export interface GameEvent {
  id: string;
  title: string;
  text: string;
  icon: string;
  tone: 'bad' | 'good' | 'warn' | 'info';
  choices: EventChoice[];
}

export interface Skills {
  hardware: number;
  network: number;
  sysadmin: number;
}

export interface DayReport {
  day: number;
  income: number;
  expenses: number;
  lines: { label: string; amount: number }[];
  events: string[];
  failedContracts: string[];
  served: number;
  total: number;
  electric: number;
}

export interface Stats {
  daysPlayed: number;
  moneyEarned: number;
  moneySpent: number;
  crashes: number;
  ddosBlocked: number;
  cablesCrimped: number;
  contractsDone: number;
  commandsRun: number;
  partsBought: number;
}

export interface GameState {
  version: number;
  day: number;
  hours: number;
  money: number;
  rep: number;
  family: number;
  stage: Stage;
  location: Location;
  skills: Skills;
  machines: Machine[];
  inventory: Part[];
  pending: { part: Part; arriveDay: number }[];
  cables: number;
  upgrades: string[];
  listings: Listing[];
  offers: Contract[];
  contracts: Contract[];
  log: LogEntry[];
  event: GameEvent | null;
  report: DayReport | null;
  selectedMachine: string;
  tab: TabId;
  flags: Record<string, number>;
  stats: Stats;
  victory: boolean;
  gameOver: boolean;
  helpOpen: boolean;
  toast: { text: string; tone: LogEntry['tone']; id: number } | null;
  cmdOutput: { machineId: string; input: string; lines: { text: string; tone?: string }[] } | null;
}

export type Action =
  | { type: 'LOAD'; state: GameState }
  | { type: 'RESET' }
  | { type: 'SET_TAB'; tab: TabId }
  | { type: 'SELECT_MACHINE'; id: string }
  | { type: 'BUY'; listingId: string }
  | { type: 'HAGGLE'; listingId: string; line: HaggleLine }
  | { type: 'REVEAL_FLAW'; listingId: string }
  | { type: 'INSTALL_PART'; uid: string; machineId: string }
  | { type: 'REMOVE_PART'; machineId: string; slot: 'cpu' | 'ram' | 'storage' | 'nic' | 'cooler'; index: number }
  | { type: 'CREATE_MACHINE'; baseUid: string }
  | { type: 'RENAME_MACHINE'; machineId: string; name: string }
  | { type: 'POWER'; machineId: string; on: boolean }
  | { type: 'INSTALL_OS'; machineId: string; os: 'linux' | 'windows' }
  | { type: 'CMD'; machineId: string; input: string }
  | { type: 'WIN_ACTION'; machineId: string; action: string; payload?: string }
  | { type: 'REPAIR'; machineId: string; action: 'clean' | 'paste' | 'smart' | 'reseat' | 'battery' | 'fix-flaw' }
  | { type: 'ACCEPT_CONTRACT'; uid: string }
  | { type: 'ABANDON_CONTRACT'; uid: string }
  | { type: 'EVENT_CHOICE'; index: number }
  | { type: 'BUY_UPGRADE'; id: string }
  | { type: 'MOVE_LOCATION'; location: Location }
  | { type: 'NEXT_DAY' }
  | { type: 'CLOSE_REPORT' }
  | { type: 'CABLE_DONE'; perfect: boolean }
  | { type: 'DDOS_RESULT'; blocked: number; total: number }
  | { type: 'RAID'; machineId: string; level: 1 | 5 }
  | { type: 'ILO'; machineId: string }
  | { type: 'TOGGLE_HELP'; open?: boolean }
  | { type: 'TOAST'; text: string; tone?: LogEntry['tone'] };

export type HaggleLine = 'offer' | 'ask' | 'pressure' | 'accept';
