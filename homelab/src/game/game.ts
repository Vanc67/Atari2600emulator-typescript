import { execCommand } from './commands';
import {
  CONTRACT_TEMPLATES,
  FLAW_INFO,
  HOURS_PER_DAY,
  INSTALLABLE,
  OS_OVERHEAD,
  SERVICES,
  STAGE_INFO,
  TARIFF,
  UPGRADES,
  WINDOWS_LICENSE,
} from './data';
import { rollEvent } from './events';
import { chance, clamp, generateListings, makeContract, makeMachine, makePart, pick, rnd, uid } from './market';
import {
  contractAvailable,
  contractBlocker,
  contractMachine,
  derive,
  firewallScore,
  machineCores,
  machineRam,
  machineStorageCap,
  machineStorage,
  osStorageReserve,
  machineTemp,
  machineUtil,
  totalNoise,
  totalWatts,
  usableRam,
} from './rules';
import type {
  Action,
  Contract,
  DayReport,
  GameEvent,
  GameState,
  Listing,
  LogEntry,
  Machine,
  Part,
  ServiceDef,
  Skills,
  Stage,
} from './types';

const SAVE_VERSION = 3;

export function addLog(s: GameState, text: string, tone: LogEntry['tone'] = 'info'): void {
  s.log.unshift({ id: uid('log'), day: s.day, text, tone });
  if (s.log.length > 140) s.log.length = 140;
}

export function toast(s: GameState, text: string, tone: LogEntry['tone'] = 'info'): void {
  s.toast = { text, tone, id: Date.now() + Math.random() };
}

function addSkill(s: GameState, gain: Partial<Skills>): void {
  if (gain.hardware) s.skills.hardware = clamp(s.skills.hardware + gain.hardware, 1, 20);
  if (gain.network) s.skills.network = clamp(s.skills.network + gain.network, 1, 20);
  if (gain.sysadmin) s.skills.sysadmin = clamp(s.skills.sysadmin + gain.sysadmin, 1, 20);
}

function spend(s: GameState, amount: number): boolean {
  s.money -= amount;
  s.stats.moneySpent += amount;
  return true;
}

function clone<T>(v: T): T {
  return structuredClone(v);
}

export function createInitialState(): GameState {
  const base: GameState = {
    version: SAVE_VERSION,
    day: 1,
    hours: 0,
    money: 5000,
    rep: 0,
    family: 62,
    stage: 1,
    location: 'room',
    skills: { hardware: 2, network: 1, sysadmin: 1 },
    machines: [],
    inventory: [],
    pending: [],
    cables: 1,
    upgrades: [],
    listings: [],
    offers: [],
    contracts: [],
    log: [],
    event: null,
    report: null,
    selectedMachine: '',
    tab: 'room',
    flags: {},
    stats: {
      daysPlayed: 1,
      moneyEarned: 0,
      moneySpent: 0,
      crashes: 0,
      ddosBlocked: 0,
      cablesCrimped: 0,
      contractsDone: 0,
      commandsRun: 0,
      partsBought: 0,
    },
    victory: false,
    gameOver: false,
    helpOpen: false,
    toast: null,
    cmdOutput: null,
  };

  const cpu = makePart('cpu_i5', { flaw: 'none' });
  const ram = makePart('ram_ddr3_4', { flaw: 'none', condition: 70 });
  const disk = makePart('hdd_320', { flaw: chance(0.7) ? 'badsectors' : 'none', condition: 45 });
  disk.flawKnown = false;
  const cooler = makePart('cooler_tower', { flaw: 'none' });
  const pcBase = makePart('base_compaq', { flaw: 'noisyfan', condition: 65 });
  pcBase.stats.baseNoise = 14;

  const machine = makeMachine(pcBase, base, 'Родительский ПК');
  machine.cpus[0] = cpu;
  machine.ram[0] = ram;
  machine.storage[0] = disk;
  machine.cooler = cooler;
  machine.dust = 2;
  machine.temp = 34;
  base.machines = [machine];
  base.selectedMachine = machine.id;

  const phone = makePart('base_phone', { flaw: 'deadbattery', condition: 45 });
  base.inventory = [
    phone,
    makePart('ram_ddr3_2', { flaw: 'none', condition: 60 }),
    makePart('nic_gbit', { flaw: 'none', condition: 85 }),
    makePart('hdd_500', { flaw: 'badsectors', condition: 55 }),
  ];
  base.listings = generateListings(base, 7);
  base.offers = refreshOffers(base);
  addLog(base, 'День 1. В комнате стоит старый родительский ПК, на антресолях — древний смартфон. Бюджет: 5 000 ₽.', 'info');
  addLog(base, 'План: купить дешёвое железо на Авито, собрать домашний сервер и начать зарабатывать.', 'dim' as LogEntry['tone']);
  return base;
}

function refreshOffers(s: GameState): Contract[] {
  const stagePool = CONTRACT_TEMPLATES.filter((t) => t.stage <= s.stage);
  const activeIds = s.contracts.filter((c) => c.active).map((c) => c.id);
  const pool = stagePool.filter((t) => !activeIds.includes(t.id));
  const available = pool.filter((t) => {
    const c = makeContract(t, s);
    return contractAvailable(s, c);
  });
  const notAvailable = pool.filter((t) => !available.some((a) => a.id === t.id));
  const chosen: Contract[] = [];
  available
    .sort(() => Math.random() - 0.5)
    .slice(0, 5)
    .forEach((t) => chosen.push(makeContract(t, s)));
  notAvailable
    .sort(() => Math.random() - 0.5)
    .slice(0, 2)
    .forEach((t) => chosen.push(makeContract(t, s)));
  return chosen;
}

export function contractReady(s: GameState, c: Contract): boolean {
  if (!contractAvailable(s, c)) return false;
  const test = clone(c);
  const machine = contractMachine(s, test);
  return !!machine;
}

function installServiceByID(s: GameState, m: Machine, id: string): string {
  const svc = SERVICES[id];
  if (!svc) return 'Неизвестный сервис';
  if (m.services.includes(id)) return `${svc.name} уже работает`;
  const load = m.services.reduce((a, sid) => a + SERVICES[sid].ramMB, 0);
  const diskLoad = m.services.reduce((a, sid) => a + SERVICES[sid].storageGB, 0);
  if (load + svc.ramMB > usableRam(m) - 96) return 'Недостаточно ОЗУ — добавь планку памяти';
  if (diskLoad + svc.storageGB + osStorageReserve(m) > machineStorageCap(m)) return 'Недостаточно места на дисках';
  if (!m.installed.includes(id)) m.installed.push(id);
  if (!m.services.includes(id)) m.services.push(id);
  return '';
}

export function reducer(state: GameState, action: Action): GameState {
  if (action.type === 'LOAD') return action.state;
  if (action.type === 'RESET') return createInitialState();

  const s = clone(state);

  switch (action.type) {
    case 'SET_TAB': {
      s.tab = action.tab;
      if (action.tab === 'server' && !s.selectedMachine && s.machines[0]) s.selectedMachine = s.machines[0].id;
      return s;
    }
    case 'SELECT_MACHINE':
      s.selectedMachine = action.id;
      return s;
    case 'TOGGLE_HELP':
      s.helpOpen = action.open ?? !s.helpOpen;
      return s;
    case 'CLOSE_REPORT':
      s.report = null;
      return s;
    case 'TOAST':
      toast(s, action.text, action.tone);
      return s;

    case 'BUY': {
      const lot = s.listings.find((l) => l.id === action.listingId);
      if (!lot || lot.sold) return s;
      if (s.money < lot.askPrice) {
        toast(s, `Не хватает денег: нужно ${lot.askPrice.toLocaleString('ru-RU')} ₽`, 'bad');
        return s;
      }
      spend(s, lot.askPrice);
      lot.sold = true;
      lot.delivered = true;
      s.stats.partsBought += 1;
      s.pending.push({ part: lot.part, arriveDay: s.day + 1 });
      s.listings = s.listings.filter((l) => l.id !== lot.id);
      addLog(s, `Купил ${lot.part.name} за ${lot.askPrice.toLocaleString('ru-RU')} ₽ у ${lot.seller}. Доставка завтра.`, 'good');
      addSkill(s, { hardware: 0 });
      return s;
    }

    case 'HAGGLE': {
      const lot = s.listings.find((l) => l.id === action.listingId);
      if (!lot || lot.sold) return s;
      const sellerGreed = lot.honest ? 0.55 : 0.7;
      const skillBonus = s.skills.network * 0.01 + s.rep * 0.001;
      if (action.line === 'ask') {
        if (lot.revealed) {
          toast(s, 'Продавец уже всё рассказал', 'warn');
          return s;
        }
        lot.revealed = true;
        const flaw = lot.part.flaw;
        let text: string;
        if (flaw === 'none') {
          text = lot.honest ? 'Слушай, честно: всё в порядке, проверял. Приезжай смотреть.' : 'Всё идеально! Проверял лично. Ну, диск иногда щёлкает, но это от пыли.';
        } else {
          text = lot.honest
            ? `Слушай, честно: есть нюанс — ${FLAW_INFO[flaw].name.toLowerCase()}. Поэтому и цена такая.`
            : 'Всё новое, ни единой проблемы! Не выдумывай.';
        }
        lot.chat.push({ from: 'me', text: 'Расскажи подробнее, есть ли косяки?' });
        lot.chat.push({ from: 'seller', text });
        if (flaw !== 'none' && lot.honest) lot.part.flawKnown = true;
        return s;
      }
      if (action.line === 'pressure') {
        lot.attempts += 1;
        const target = Math.max(60, Math.round(lot.askPrice * (0.72 - skillBonus)));
        if (Math.random() < 0.45 + s.skills.hardware * 0.03) {
          lot.chat.push({ from: 'me', text: 'Я посмотрел SMART/фото. Есть вопросы. Скинешь?' });
          lot.part.flawKnown = true;
          lot.askPrice = Math.min(lot.askPrice, Math.round(target / 10) * 10);
          lot.chat.push({
            from: 'seller',
            text: `А, ты разбираешься. Тогда слушай: ${lot.part.flaw === 'none' ? 'всё правда нормально, но за наглость скину' : FLAW_INFO[lot.part.flaw].name.toLowerCase()}. Новая цена — ${lot.askPrice.toLocaleString('ru-RU')} ₽.`,
          });
          addSkill(s, { hardware: 1 });
          return s;
        }
        lot.chat.push({ from: 'seller', text: 'Да ты что, это же идеальное состояние. Не выдумывай.' });
        if (lot.attempts > 2) {
          lot.chat.push({ from: 'seller', text: 'Ещё один такой вопрос — и я заблокирую.' });
        }
        return s;
      }
      if (action.line === 'offer') {
        lot.attempts += 1;
        const offer = Math.round((lot.askPrice * (0.68 - skillBonus * 0.5)) / 10) * 10;
        const accepted = offer >= lot.askPrice * sellerGreed * (1 - s.skills.network * 0.012);
        lot.chat.push({ from: 'me', text: `Готов забрать за ${offer.toLocaleString('ru-RU')} ₽.` });
        if (accepted) {
          lot.askPrice = Math.max(40, offer);
          lot.chat.push({ from: 'seller', text: `Хм, ну ладно. ${offer.toLocaleString('ru-RU')} ₽, сам забираешь. По рукам.` });
          addSkill(s, { network: 1 });
        } else if (lot.attempts > 2 && Math.random() < 0.5) {
          lot.chat.push({ from: 'seller', text: 'Всё, торг закончился. Другие пишут, заберут по полной цене.' });
          lot.askPrice = Math.round(lot.askPrice * 1.05);
        } else {
          lot.chat.push({ from: 'seller', text: 'Нет, это уже наглость. Минимум могу отдать дешевле на 5%.' });
          lot.askPrice = Math.round((lot.askPrice * 0.95) / 10) * 10;
        }
        return s;
      }
      if (action.line === 'accept') {
        const lot2 = s.listings.find((l) => l.id === action.listingId);
        if (!lot2) return s;
        if (s.money < lot2.askPrice) {
          toast(s, 'Не хватает денег', 'bad');
          return s;
        }
        spend(s, lot2.askPrice);
        s.pending.push({ part: lot2.part, arriveDay: s.day + 1 });
        s.stats.partsBought += 1;
        s.listings = s.listings.filter((l) => l.id !== lot2.id);
        addLog(s, `Купил ${lot2.part.name} за ${lot2.askPrice.toLocaleString('ru-RU')} ₽ у ${lot2.seller}. Доставка завтра.`, 'good');
        lot2.chat.push({ from: 'seller', text: 'Д договорились!' });
        return s;
      }
      return s;
    }

    case 'REVEAL_FLAW': {
      const lot = s.listings.find((l) => l.id === action.listingId);
      if (!lot) return s;
      const cost = 150;
      if (s.money < cost) {
        toast(s, 'Нет денег на проверку (150 ₽)', 'bad');
        return s;
      }
      spend(s, cost);
      lot.part.flawKnown = true;
      lot.part.condition = clamp(lot.part.condition + rnd(11) - 5, 5, 100);
      lot.chat.push({ from: 'me', text: 'Привёз ноутбук, проверил SMART и память.' });
      lot.chat.push({
        from: 'system',
        text:
          lot.part.flaw === 'none'
            ? 'Проверка: дефектов не найдено. Состояние честное.'
            : `Проверка: обнаружено — ${FLAW_INFO[lot.part.flaw].name}. ${FLAW_INFO[lot.part.flaw].desc}`,
      });
      addSkill(s, { hardware: 2 });
      return s;
    }

    case 'CREATE_MACHINE': {
      const idx = s.inventory.findIndex((p) => p.uid === action.baseUid);
      if (idx < 0) return s;
      const part = s.inventory[idx];
      if (part.kind !== 'base') {
        toast(s, 'Это не платформа — такую деталь ставят в сервер', 'warn');
        return s;
      }
      const rackKinds = ['rack', 'blade', 'storage'];
      const kindOf = (p: Part) => (p.defId === 'base_phone' ? 'phone' : rackKinds.some((k) => p.defId.includes(k)) ? 'rack' : 'desktop');
      const kind = kindOf(part);
      if (kind === 'rack' && !s.upgrades.includes('rack')) {
        toast(s, 'Для настоящего сервера нужен шкаф 12U (Магазин → Апгрейды)', 'bad');
        return s;
      }
      s.inventory.splice(idx, 1);
      const m = makeMachine(part, s);
      s.machines.push(m);
      s.selectedMachine = m.id;
      s.tab = 'server';
      addLog(s, `Новая машина: ${m.name}. Установи процессор, память и диски через панель сборки.`, 'good');
      addSkill(s, { hardware: 1 });
      return s;
    }

    case 'INSTALL_PART': {
      const m = s.machines.find((x) => x.id === action.machineId);
      const idx = s.inventory.findIndex((p) => p.uid === action.uid);
      if (!m || idx < 0) return s;
      const part = s.inventory[idx];
      if (m.powered) {
        toast(s, 'Сначала выключи сервер — железо меняют только на обесточенной машине', 'warn');
        return s;
      }
      if (m.kind === 'phone' && part.kind !== 'storage') {
        toast(s, 'В смартфоне всё распаяно — можно только вставить карту памяти', 'warn');
        return s;
      }
      let placed = false;
      if (part.kind === 'cpu') {
        const i = m.cpus.findIndex((p) => !p);
        if (i >= 0) {
          m.cpus[i] = part;
          placed = true;
        } else toast(s, 'Все слоты процессоров заняты', 'warn');
      } else if (part.kind === 'ram') {
        if (m.base.stats.memType === 'ecc' && part.stats.memType !== 'ecc') {
          toast(s, 'Сервер не понимает обычную память — нужна ECC', 'bad');
          return s;
        }
        if (m.base.stats.memType !== 'ecc' && part.stats.memType === 'ecc') {
          toast(s, 'Десктопная плата не поддерживает регистровую ECC-память', 'bad');
          return s;
        }
        const i = m.ram.findIndex((p) => !p);
        if (i >= 0) {
          m.ram[i] = part;
          placed = true;
        } else toast(s, 'Слоты памяти закончились', 'warn');
      } else if (part.kind === 'storage') {
        const i = m.storage.findIndex((p) => !p);
        if (i >= 0) {
          m.storage[i] = part;
          placed = true;
        } else toast(s, 'Отсеки под диски закончились', 'warn');
      } else if (part.kind === 'nic') {
        if (!m.nic) {
          m.nic = part;
          placed = true;
        } else toast(s, 'Сетевая карта уже стоит', 'warn');
      } else if (part.kind === 'cooler') {
        if (!m.cooler) {
          m.cooler = part;
          placed = true;
        } else toast(s, 'Кулер уже установлен', 'warn');
      } else {
        toast(s, 'Это платформа: создай из неё новую машину в панели «Сборка»', 'warn');
      }
      if (placed) {
        s.inventory.splice(idx, 1);
        m.temp = machineTemp(m);
        addSkill(s, { hardware: 2 });
        addLog(s, `Установил ${part.name} в ${m.name}.`, 'good');
      }
      return s;
    }

    case 'REMOVE_PART': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      if (m.powered) {
        toast(s, 'Выключи сервер перед разборкой', 'warn');
        return s;
      }
      let part: Part | null = null;
      if (action.slot === 'nic') {
        part = m.nic;
        m.nic = null;
      } else if (action.slot === 'cooler') {
        part = m.cooler;
        m.cooler = null;
      } else if (action.slot === 'cpu') {
        part = m.cpus[action.index];
        m.cpus[action.index] = null;
      } else if (action.slot === 'ram') {
        part = m.ram[action.index];
        m.ram[action.index] = null;
      } else if (action.slot === 'storage') {
        part = m.storage[action.index];
        m.storage[action.index] = null;
      }
      if (part) {
        s.inventory.push(part);
        addLog(s, `Снял ${part.name} с ${m.name}.`, 'info');
      }
      return s;
    }

    case 'RENAME_MACHINE': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (m && action.name.trim()) m.name = action.name.trim().slice(0, 28);
      return s;
    }

    case 'POWER': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      if (action.on) {
        if (m.crashed) {
          toast(s, 'Сервер в аварии. Сначала ремонт (вкладка «Железо»).', 'bad');
          return s;
        }
        if (machineCores(m) <= 0) {
          toast(s, 'Нет процессора — не запустится', 'bad');
          return s;
        }
        if (machineRam(m) <= 0) {
          toast(s, 'Нет оперативной памяти', 'bad');
          return s;
        }
        m.powered = true;
        m.services = [...m.installed];
        m.temp = machineTemp(m);
        addLog(s, `${m.name} включён. ${m.os === 'none' ? 'BIOS пищит: нет загрузочного устройства.' : 'Система загружается.'}`, 'info');
      } else {
        m.powered = false;
        m.services = [];
        addLog(s, `${m.name} выключен.`, 'info');
      }
      return s;
    }

    case 'INSTALL_OS': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      if (!m.powered) {
        toast(s, 'Включи сервер, вставь установочную флешку и начни установку', 'warn');
        return s;
      }
      if (m.crashed) {
        toast(s, 'Сервер не в состоянии устанавливать ОС', 'bad');
        return s;
      }
      if (machineStorageCap(m) <= 0) {
        toast(s, 'Негде ставить систему: нужен диск', 'bad');
        return s;
      }
      if (action.os === 'windows' && machineRam(m) < 2560) {
        toast(s, 'Windows Server 2016 требует минимум 2,5 ГБ памяти — добавь планку', 'bad');
        return s;
      }
      if (action.os === 'linux' && m.kind !== 'phone') {
        m.os = 'linux';
        m.installed = [];
        m.services = [];
        addLog(s, `На ${m.name} установлен Ubuntu Server 22.04. Теперь работай в терминале: apt update.`, 'good');
        addSkill(s, { sysadmin: 3 });
        s.flags.installed_linux = 1;
      } else if (action.os === 'windows' && m.kind !== 'phone') {
        if (!m.windowsLicensed) {
          if (s.money < WINDOWS_LICENSE) {
            toast(s, 'Лицензия Windows Server стоит 1 400 ₽', 'bad');
            return s;
          }
          spend(s, WINDOWS_LICENSE);
          m.windowsLicensed = true;
        }
        m.os = 'windows';
        m.installed = [];
        m.services = [];
        m.desktopExperience = false;
        m.audioService = false;
        m.shares = [];
        addLog(s, `На ${m.name} установлен Windows Server 2016. Лицензия активирована, деньги ушли.`, 'good');
        addSkill(s, { sysadmin: 2 });
        s.flags.installed_windows = 1;
      } else if (m.kind === 'phone') {
        m.os = 'android';
        m.installed = [];
        m.services = [];
        addLog(s, `На ${m.name} Android 2.3. Медленно, зато с веб-интерфейсом и сервисом tinyllm.`, 'good');
        addSkill(s, { sysadmin: 1 });
      }
      return s;
    }

    case 'CMD': {
      const target = s.machines.find((x) => x.id === action.machineId);
      if (!target) return s;
      s.stats.commandsRun += 1;
      const working = structuredClone(target);
      const result = execCommand(s, working, action.input);
      Object.assign(target, working);
      if (result.skill) addSkill(s, result.skill);
      if (result.money) {
        s.money += result.money;
        s.stats.moneyEarned += Math.max(0, result.money);
      }
      s.cmdOutput = { machineId: action.machineId, input: action.input, lines: result.lines };
      const tone: LogEntry['tone'] = result.lines.some((l) => l.tone === 'bad') ? 'warn' : 'info';
      if (result.lines.length) addLog(s, `$ ${action.input} — ${result.lines[result.lines.length - 1].text}`, tone);
      return s;
    }

    case 'WIN_ACTION': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      if (action.action === 'tiny') {
        if (m.kind !== 'phone') {
          toast(s, 'tinyllm живёт только на старом смартфоне', 'warn');
          return s;
        }
        if (s.cables < 1) {
          toast(s, 'Сначала обожми патч-корд — телефон должен быть в сети', 'bad');
          return s;
        }
        const err = installServiceByID(s, m, 'tinyllm');
        if (err) {
          toast(s, err, 'bad');
          return s;
        }
        addLog(s, `На ${m.name} запущена tiny-нейросеть. Веб-морда доступна по адресу телефона.`, 'good');
        addSkill(s, { sysadmin: 2 });
        return s;
      }
      if (m.os !== 'windows') {
        toast(s, 'Это действие доступно на Windows Server', 'warn');
        return s;
      }
      const svc: Record<string, string> = {
        share: 'smb',
        jellyfin: 'jellyfin',
        iis: 'webserver',
        cctv: 'cctv',
      };
      if (action.action === 'desktop-experience') {
        if (m.desktopExperience) {
          toast(s, 'Desktop Experience уже установлен', 'warn');
          return s;
        }
        if (s.money < 400) {
          toast(s, 'Нужно 400 ₽ на компонент Desktop Experience', 'bad');
          return s;
        }
        spend(s, 400);
        m.desktopExperience = true;
        addLog(s, `Desktop Experience установлен на ${m.name}. Теперь есть мышка, окна и вот это всё.`, 'good');
        addSkill(s, { sysadmin: 2 });
        return s;
      }
      if (action.action === 'audio') {
        if (!m.desktopExperience) {
          toast(s, 'Звук требует Desktop Experience', 'bad');
          return s;
        }
        m.audioService = !m.audioService;
        addLog(s, m.audioService ? 'Служба Windows Audio включена. Сервер умеет пищать.' : 'Служба Windows Audio отключена.', 'info');
        return s;
      }
      if (action.action === 'taskmgr') {
        s.cmdOutput = {
          machineId: m.id,
          input: 'Диспетчер задач',
          lines: [
            { text: 'Диспетчер задач — Windows Server 2016', tone: 'dim' },
            { text: `ЦП: ${Math.round(machineUtil(m) * 100)}%   ОЗУ: ${m.services.reduce((a, id) => a + (SERVICES[id]?.ramMB ?? 0), 0)} МБ`, tone: 'info' },
            { text: `Память: ${Math.round((usableRam(m) / Math.max(1, m.ram.reduce((a, p) => a + (p?.stats.ramMB ?? 0), 0))) * 100)}% доступно`, tone: 'dim' },
            { text: 'Если RAM забита в 100% — сервер начнёт свопиться на диск и умрёт от тоски.', tone: 'warn' },
          ],
        };
        return s;
      }
      if (action.action === 'diskmgr') {
        s.cmdOutput = {
          machineId: m.id,
          input: 'Управление дисками',
          lines: [
            { text: 'Управление дисками — Windows Server 2016', tone: 'dim' },
            { text: `Диски: ${m.storage.filter(Boolean).length}, суммарно ${machineStorageCap(m)} ГБ`, tone: 'info' },
            { text: m.raid ? `Массив ${m.raid.level === 5 ? 'RAID-5' : 'RAID-1'}: ${m.raid.healthy ? 'здоров' : 'ДЕГРАДИРОВАН'}` : 'Программный RAID не настроен', tone: m.raid?.healthy ? 'good' : 'warn' },
            { text: 'Ошибка в конфигурации RAID — и данные не восстановить. Делай бэкапы.', tone: 'dim' },
          ],
        };
        return s;
      }
      const id = svc[action.action];
      if (!id) {
        toast(s, 'Неизвестное действие', 'warn');
        return s;
      }
      const error = installServiceByID(s, m, id);
      if (error) {
        toast(s, error, 'bad');
        return s;
      }
      if (id === 'smb' && !m.shares.includes('Общая папка')) m.shares.push('Общая папка');
      addLog(s, `На ${m.name} через графический интерфейс включён сервис «${SERVICES[id].name}».`, 'good');
      addSkill(s, { sysadmin: 2 });
      return s;
    }

    case 'REPAIR': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      if (action.action === 'clean') {
        m.dust = Math.max(0, m.dust - 1);
        m.temp = machineTemp(m);
        addSkill(s, { hardware: 2 });
        addLog(s, `Почистил ${m.name} от пыли. Стало прохладнее на ${(m.dust * 4).toFixed(0)} °C.`, 'good');
        return s;
      }
      if (action.action === 'paste') {
        if (s.money < 350) {
          toast(s, 'Термопаста и спирт стоят 350 ₽', 'bad');
          return s;
        }
        spend(s, 350);
        s.flags.tempOffset = (s.flags.tempOffset ?? 0) - 4;
        addSkill(s, { hardware: 3 });
        addLog(s, `Заменил термопасту на ${m.name}. Температура пошла вниз.`, 'good');
        return s;
      }
      if (action.action === 'smart') {
        let found = 0;
        [...m.storage, ...m.ram].forEach((p) => {
          if (p && p.flaw !== 'none') {
            p.flawKnown = true;
            found += 1;
          }
        });
        addSkill(s, { hardware: 2 });
        toast(
          s,
          found ? `Проверка: найдено проблем — ${found}. Смотри карточку железа.` : 'Проверка: железо выглядит здоровым.',
          found ? 'warn' : 'good',
        );
        return s;
      }
      if (action.action === 'reseat') {
        if (s.money < 150) {
          toast(s, 'Нужно 150 ₽ на спирт и салфетки', 'bad');
          return s;
        }
        spend(s, 150);
        const target = [...m.storage, ...m.ram].find((p) => p && p.flaw !== 'none');
        if (target && Math.random() < 0.5) {
          target.flaw = 'none';
          target.flawKnown = true;
          addSkill(s, { hardware: 3 });
          addLog(s, `Протёр контакты ${target.name} спиртом и переставил планку — дефект ушёл. Иногда помогает.`, 'good');
        } else {
          addSkill(s, { hardware: 2 });
          addLog(s, 'Переставил планки и продул слоты. Не помогло, но стало чище.', 'warn');
        }
        return s;
      }
      if (action.action === 'battery') {
        if (s.money < 600) {
          toast(s, 'Аккумулятор стоит 600 ₽', 'bad');
          return s;
        }
        spend(s, 600);
        const bat = m.base.flaw === 'deadbattery' ? m.base : null;
        if (bat) bat.flaw = 'none';
        addSkill(s, { hardware: 2 });
        addLog(s, `Заменил аккумулятор ${m.name}. Теперь смартфон живёт дольше четырёх минут.`, 'good');
        return s;
      }
      if (action.action === 'fix-flaw' && m.crashed) {
        m.crashed = false;
        m.services = [...m.installed];
        m.temp = machineTemp(m);
        addSkill(s, { hardware: 3 });
        addLog(s, `Авария на ${m.name} устранена: разобрал, продул, перезапустил. Сервисы вернулись.`, 'good');
        return s;
      }
      if (action.action === 'fix-flaw') {
        const fixable: Record<string, number> = {
          noisyfan: 400,
          coilwhine: 500,
          weakpsu: 1400,
          bentpins: 900,
          memoryerrors: 600,
          badsectors: 0,
          slowdisk: 0,
          deadbattery: 600,
          none: 0,
        };
        const flawed = [
          m.base,
          ...m.cpus.filter((p): p is Part => !!p),
          ...m.ram.filter((p): p is Part => !!p),
          ...m.storage.filter((p): p is Part => !!p),
          m.nic,
          m.cooler,
        ].filter((p): p is Part => !!p && p.flaw !== 'none');
        if (!flawed.length) {
          toast(s, 'Дефектов нет — всё работает как надо', 'good');
          return s;
        }
        const part = flawed[0];
        const cost = fixable[part.flaw] ?? 0;
        if (cost === 0) {
          toast(s, `${FLAW_INFO[part.flaw].name} не лечится — деталь нужно менять (сними её и поставь другую)`, 'warn');
          return s;
        }
        if (s.money < cost) {
          toast(s, `Ремонт стоит ${cost} ₽`, 'bad');
          return s;
        }
        spend(s, cost);
        part.flaw = 'none';
        part.flawKnown = true;
        addSkill(s, { hardware: 3 });
        addLog(s, `Отремонтировал ${part.name} за ${cost} ₽. Дефект «${FLAW_INFO[part.flaw].name}» больше не мешает.`, 'good');
        return s;
      }
      return s;
    }

    case 'ACCEPT_CONTRACT': {
      const offer = s.offers.find((o) => o.uid === action.uid);
      if (!offer) return s;
      if (!contractReady(s, offer)) {
        toast(s, `Пока не готов: ${contractBlocker(s, offer)}`, 'bad');
        return s;
      }
      const c: Contract = {
        ...clone(offer),
        active: true,
        acceptedDay: s.day,
        daysLeft: offer.days,
        failed: false,
        payments: 0,
        machineId: null,
        missedDays: 0,
      };
      const test = clone(c);
      const machine = contractMachine(s, test);
      c.machineId = machine?.id ?? null;
      s.contracts.push(c);
      s.offers = s.offers.filter((o) => o.uid !== action.uid);
      s.money += c.setupPay;
      s.stats.moneyEarned += c.setupPay;
      addLog(s, `Заключён договор «${c.title}» с ${c.client}. Аванс ${c.setupPay.toLocaleString('ru-RU')} ₽, далее ${c.dailyPay.toLocaleString('ru-RU')} ₽/день.`, 'good');
      addSkill(s, { sysadmin: 1, network: 1 });
      return s;
    }

    case 'ABANDON_CONTRACT': {
      const c = s.contracts.find((x) => x.uid === action.uid);
      if (!c) return s;
      s.contracts = s.contracts.filter((x) => x.uid !== action.uid);
      s.rep = clamp(s.rep - 8, 0, 100);
      s.money -= c.penalty;
      addLog(s, `Расторгнут договор «${c.title}». Неустойка ${c.penalty.toLocaleString('ru-RU')} ₽, репутация пострадала.`, 'bad');
      return s;
    }

    case 'EVENT_CHOICE': {
      if (!s.event) return s;
      const choice = s.event.choices[action.index];
      if (!choice) return s;
      if (choice.cost && s.money < choice.cost) {
        toast(s, `Не хватает денег: нужно ${choice.cost.toLocaleString('ru-RU')} ₽`, 'bad');
        return s;
      }
      applyEffect(s, choice.effect);
      s.flags[`evt_${s.event.id}`] = (s.flags[`evt_${s.event.id}`] ?? 0) + 1;
      s.event = null;
      return s;
    }

    case 'BUY_UPGRADE': {
      const up = UPGRADES.find((u) => u.id === action.id);
      if (!up) return s;
      if (s.upgrades.includes(up.id)) {
        toast(s, 'Уже куплено', 'warn');
        return s;
      }
      if (s.money < up.price) {
        toast(s, `Не хватает денег: ${up.name} стоит ${up.price.toLocaleString('ru-RU')} ₽`, 'bad');
        return s;
      }
      spend(s, up.price);
      s.upgrades.push(up.id);
      addLog(s, `Куплено: ${up.icon} ${up.name}. ${up.desc}`, 'good');
      if (up.id === 'monitoring') addSkill(s, { sysadmin: 3 });
      if (up.id === 'switch') addSkill(s, { network: 3 });
      if (up.id === 'firewall') addSkill(s, { network: 2 });
      return s;
    }

    case 'MOVE_LOCATION': {
      if (action.location === 'room') {
        if (s.location === 'room') return s;
        s.location = 'room';
        s.machines.forEach((m) => (m.location = 'room'));
        addLog(s, 'Ты вернулся в комнату. Домашние недовольны, но экономно.', 'warn');
        return s;
      }
      if (action.location === 'garage') {
        if (s.stage < 2) {
          toast(s, 'Переезд в гараж откроется на этапе 2', 'warn');
          return s;
        }
        s.location = 'garage';
        s.machines.forEach((m) => (m.location = 'garage'));
        addLog(s, 'Переезд в гараж! Прохладно, пахнет бензином, соседи стучат по трубе.', 'good');
        s.flags.moved_garage = 1;
        return s;
      }
      if (action.location === 'dc') {
        if (!s.flags.dc_unlocked) {
          toast(s, 'ЦОД откроется на этапе 3 — сначала заработай репутацию и деньги', 'warn');
          return s;
        }
        if (!s.flags.dc_paid) {
          if (s.money < 40000) {
            toast(s, 'Первый месяц аренды и стойка стоят 40 000 ₽', 'bad');
            return s;
          }
          spend(s, 40000);
          s.flags.dc_paid = 1;
        }
        s.location = 'dc';
        s.machines.forEach((m) => (m.location = 'dc'));
        addLog(s, 'Ты в ЦОДе. Холодный коридор, 42U и огонёк, ради которого всё это было.', 'good');
        return s;
      }
      return s;
    }

    case 'CABLE_DONE': {
      const gained = action.perfect ? 3 : 2;
      s.flags.cable_day = s.day;
      delete s.flags.cable_pending;
      s.cables += gained;
      s.stats.cablesCrimped += gained;
      addSkill(s, { network: action.perfect ? 4 : 2 });
      s.money += 800;
      s.stats.moneyEarned += 800;
      addLog(
        s,
        action.perfect
          ? `Обжим идеальный: все ${gained} кабеля прозвонились с первого раза. +800 ₽, навык сети вырос.`
          : `Обжал ${gained} кабеля. Один пришлось переделать, но клиент доволен. +800 ₽.`,
        'good',
      );
      return s;
    }

    case 'RAID': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      const disks = m.storage.filter((p): p is Part => !!p);
      const need = action.level === 5 ? 3 : 2;
      if (disks.length < need) {
        toast(s, `Для RAID-${action.level} нужно минимум ${need} диска`, 'bad');
        return s;
      }
      if (m.powered) {
        toast(s, 'Массив собирают на выключенной машине (или через «Управление дисками» на Windows)', 'warn');
        if (m.os !== 'windows') return s;
      }
      const skill = s.skills.sysadmin + s.skills.hardware;
      const risk = Math.max(0.05, 0.45 - skill * 0.02);
      if (Math.random() < risk) {
        // Ошибка в конфигурации — данные потеряны
        const victim = disks[Math.floor(Math.random() * disks.length)];
        victim.flaw = 'badsectors';
        victim.flawKnown = true;
        m.raid = null;
        s.rep = clamp(s.rep - 4, 0, 100);
        addSkill(s, { sysadmin: 2 });
        addLog(s, `💥 Ошибка в конфигурации RAID-${action.level}: массив развалился, данные на ${victim.name} стёрлись. Классика.`, 'bad');
        return s;
      }
      m.raid = { level: action.level, disks: disks.length, healthy: true };
      addSkill(s, { sysadmin: 4, hardware: 2 });
      addLog(
        s,
        `Массив RAID-${action.level} собран на ${m.name}: ${disks.length} диска, ёмкость ${machineStorageCap(m)} ГБ. Теперь один диск можно потерять без потери данных.`,
        'good',
      );
      return s;
    }

    case 'ILO': {
      const m = s.machines.find((x) => x.id === action.machineId);
      if (!m) return s;
      if (!m.base.stats.hasILO) {
        toast(s, 'У этой платформы нет iLO — нужен настоящий стоечный сервер', 'bad');
        return s;
      }
      if (m.ilo) {
        toast(s, 'iLO уже настроен', 'warn');
        return s;
      }
      if (s.money < 2000) {
        toast(s, 'Лицензия и настройка iLO стоят 2 000 ₽', 'bad');
        return s;
      }
      spend(s, 2000);
      m.ilo = true;
      addSkill(s, { hardware: 2, network: 3, sysadmin: 2 });
      addLog(s, `iLO настроен на ${m.name}: удалённая консоль, питание и логи. Теперь сервер можно перезагрузить из браузера.`, 'good');
      return s;
    }

    case 'DDOS_RESULT': {
      const ratio = action.total > 0 ? action.blocked / action.total : 1;
      s.stats.ddosBlocked += action.blocked;
      delete s.flags.ddos_pending;
      if (ratio >= 0.85) {
        s.rep = clamp(s.rep + 8, 0, 100);
        addSkill(s, { network: 5, sysadmin: 2 });
        s.money += 1200;
        s.stats.moneyEarned += 1200;
        addLog(s, `DDoS отбит: заблокировано ${action.blocked} из ${action.total} вредных потоков. Клиент в восторге, +1 200 ₽ премии.`, 'good');
      } else if (ratio >= 0.55) {
        s.rep = clamp(s.rep + 2, 0, 100);
        addSkill(s, { network: 3 });
        addLog(s, `Атака частично отбита (${action.blocked}/${action.total}). Сайт лежал 12 минут, но клиент остался.`, 'warn');
      } else {
        s.rep = clamp(s.rep - 6, 0, 100);
        addSkill(s, { network: 2 });
        s.money -= 800;
        addLog(s, `Атака пробила защиту (${action.blocked}/${action.total}). Клиент ушёл, ты запомнил, как фильтровать трафик.`, 'bad');
      }
      return s;
    }

    case 'NEXT_DAY':
      return nextDay(s);

    default:
      return s;
  }
}

function applyEffect(s: GameState, effect: import('./types').EventEffect): void {
  if (effect.money) {
    s.money += effect.money;
    if (effect.money > 0) s.stats.moneyEarned += effect.money;
    else s.stats.moneySpent += -effect.money;
  }
  if (effect.rep) s.rep = clamp(s.rep + effect.rep, 0, 100);
  if (effect.family) s.family = clamp(s.family + effect.family, 0, 100);
  if (effect.skill) addSkill(s, effect.skill);
  if (effect.temp) {
    s.flags.tempOffset = (s.flags.tempOffset ?? 0) + effect.temp;
    s.machines.forEach((m) => (m.temp = clamp(m.temp + (effect.temp ?? 0), 20, 110)));
  }
  if (effect.dust && s.machines.length) {
    const m = s.machines[rnd(s.machines.length)];
    m.dust = clamp(m.dust + effect.dust, 0, 8);
  }
  if (effect.crash) {
    const running = s.machines.filter((m) => m.powered);
    if (running.length) {
      const m = running[rnd(running.length)];
      m.crashed = true;
      m.powered = false;
      m.services = [];
      s.stats.crashes += 1;
    }
  }
  if (effect.breakDisk) {
    const protectedByRaid = s.machines.some((m) => m.raid?.healthy);
    const protectedByBackup = s.upgrades.includes('backup') || protectedByRaid;
    const withDisk = s.machines.filter((m) => m.storage.some(Boolean));
    if (withDisk.length && !protectedByBackup) {
      const m = withDisk[rnd(withDisk.length)];
      const disk = m.storage.find(Boolean);
      if (disk) {
        disk.flaw = 'badsectors';
        disk.flawKnown = true;
      }
      addLog(s, 'Данные на одном диске повреждены. Бэкапа нет — часть файлов потеряна.', 'bad');
    } else if (protectedByBackup) {
      addLog(s, 'Диск засыпался, но бэкап спас ситуацию. Ты восстановил данные за вечер.', 'good');
      s.rep = clamp(s.rep + 3, 0, 100);
    }
  }
  if (effect.downtime) {
    s.machines.forEach((m) => {
      if (m.powered) {
        m.powered = false;
        m.services = [];
      }
    });
    s.rep = clamp(s.rep - 2, 0, 100);
  }
  if (effect.addUpgrade && !s.upgrades.includes(effect.addUpgrade)) {
    s.upgrades.push(effect.addUpgrade);
  }
  if (effect.givePart) {
    try {
      const p = makePart(effect.givePart);
      s.pending.push({ part: p, arriveDay: s.day + 1 });
    } catch {
      // ignore unknown part
    }
  }
  if (effect.ddos) s.flags.ddos_pending = 1;
  if (effect.cableGame) s.flags.cable_pending = 1;
  addLog(s, effect.text, effect.money && effect.money > 0 ? 'good' : 'info');
}

function recomputeTemps(s: GameState): void {
  const ac = s.upgrades.includes('ac') ? 10 : 0;
  const offset = s.flags.tempOffset ?? 0;
  s.machines.forEach((m) => {
    if (!m.powered) {
      m.temp = Math.round((m.temp - 2) * 10) / 10;
      return;
    }
    m.temp = Math.round((machineTemp(m) - ac + offset + (m.dust * 1.5)) * 10) / 10;
  });
}

function nextDay(s: GameState): GameState {
  s.day += 1;
  s.hours = 0;
  s.stats.daysPlayed += 1;
  s.report = null;
  s.cmdOutput = null;

  const lines: { label: string; amount: number }[] = [];
  let income = 0;
  let expenses = 0;

  // Доставка
  const arrived = s.pending.filter((p) => p.arriveDay <= s.day);
  s.pending = s.pending.filter((p) => p.arriveDay > s.day);
  arrived.forEach((p) => {
    s.inventory.push(p.part);
    addLog(s, `Курьер привёз ${p.part.name}. Деталь в инвентаре.`, 'good');
  });

  // Аптайм и температура
  s.machines.forEach((m) => {
    if (m.powered && !m.crashed) m.uptime += 1;
  });
  recomputeTemps(s);
  s.machines.forEach((m) => {
    if (!m.powered) return;
    const d = derive(m, s);
    const heatRisk = m.temp > 88 ? 0.4 : m.temp > 80 ? 0.16 : 0.008;
    const flawed = d.flaws.length * (m.raid?.healthy ? 0.005 : 0.01);
    const monitoring = s.upgrades.includes('monitoring') ? 0.5 : 1;
    const employees = s.upgrades.includes('employees') ? 0.6 : 1;
    if (chance((heatRisk + flawed) * monitoring * employees)) {
      s.stats.crashes += 1;
      if (m.ilo) {
        s.rep = clamp(s.rep - 1, 0, 100);
        addLog(s, `⚠ ${m.name} завис, но iLO удалённо перезагрузил его за 40 секунд. Клиенты почти не заметили.`, 'warn');
      } else {
        m.crashed = true;
        m.powered = false;
        m.services = [];
        addLog(s, `💥 ${m.name} упал: ${m.temp > 85 ? 'перегрев' : 'сбой железа'}. Клиенты заметили.`, 'bad');
      }
    }
    m.dust = clamp(m.dust + (chance(0.25) ? 1 : 0), 0, 8);
  });
  s.flags.tempOffset = Math.round((s.flags.tempOffset ?? 0) * 0.6);

  // Контракты
  let served = 0;
  const failedNames: string[] = [];
  const remaining: Contract[] = [];
  s.contracts
    .filter((c) => c.active)
    .sort((a, b) => b.dailyPay - a.dailyPay)
    .forEach((c) => {
      const machine = contractMachine(s, c);
      c.machineId = machine?.id ?? null;
      if (!machine) {
        c.missedDays += 1;
        if (c.missedDays < 2) {
          s.rep = clamp(s.rep - 1, 0, 100);
          addLog(s, `⚠ Клиент «${c.client}» не получил сервис: ${contractBlocker(s, c) || 'сервис недоступен'}. День простоя — ещё один, и договор сорвётся.`, 'warn');
          remaining.push(c);
          return;
        }
        c.failed = true;
        failedNames.push(c.title);
        s.rep = clamp(s.rep - 4, 0, 100);
        s.money -= c.penalty;
        expenses += c.penalty;
        addLog(s, `❌ Договор «${c.title}» сорван: ${contractBlocker(s, c) || 'сервис недоступен'}. Неустойка ${c.penalty.toLocaleString('ru-RU')} ₽, репутация −4.`, 'bad');
        lines.push({ label: `Неустойка: ${c.title}`, amount: -c.penalty });
        return;
      }
      c.missedDays = 0;
      if (machine.temp > 85) {
        s.rep = clamp(s.rep - 2, 0, 100);
        addLog(s, `⚠ ${machine.name} перегревается, клиент «${c.client}» жалуется на задержки.`, 'warn');
      }
      const bonus = 1 + s.rep / 500;
      const pay = Math.round(c.dailyPay * bonus * (c.daysLeft <= 2 ? 1.1 : 1));
      c.payments += 1;
      c.daysLeft -= 1;
      income += pay;
      served += 1;
      s.money += pay;
      s.stats.moneyEarned += pay;
      lines.push({ label: `Оплата: ${c.title}`, amount: pay });
      if (c.daysLeft <= 0) {
        s.rep = clamp(s.rep + c.repReward, 0, 100);
        s.stats.contractsDone += 1;
        const doneBonus = Math.round(c.dailyPay * 1.5);
        s.money += doneBonus;
        income += doneBonus;
        s.stats.moneyEarned += doneBonus;
        lines.push({ label: `Бонус за завершение: ${c.title}`, amount: doneBonus });
        addLog(s, `✅ Договор «${c.title}» выполнен! Репутация +${c.repReward}, премия ${doneBonus.toLocaleString('ru-RU')} ₽.`, 'good');
      } else {
        remaining.push(c);
      }
    });
  s.contracts = remaining;

  // Расходы
  const watts = totalWatts(s);
  const electric = Math.round((watts * 24) / 1000 * TARIFF);
  if (electric > 0) {
    expenses += electric;
    s.money -= electric;
    lines.push({ label: `Электричество (${watts} Вт)`, amount: -electric });
  }
  if (s.location === 'dc') {
    const rent = 900 + s.machines.length * 120;
    expenses += rent;
    s.money -= rent;
    lines.push({ label: 'Аренда стойки в ЦОД', amount: -rent });
  }
  if (s.upgrades.includes('employees')) {
    const salary = 700;
    expenses += salary;
    s.money -= salary;
    lines.push({ label: 'Зарплата смене эникеев', amount: -salary });
  }
  if (s.upgrades.includes('generator')) {
    const fuel = 800;
    expenses += fuel;
    s.money -= fuel;
    lines.push({ label: 'Солярка для генератора', amount: -fuel });
  }
  if (s.upgrades.includes('router')) {
    const isp = 120;
    expenses += isp;
    s.money -= isp;
    lines.push({ label: 'Интернет для серверов', amount: -isp });
  }

  // Семья и шум
  const noise = totalNoise(s);
  if (s.location === 'room') {
    if (noise > 58) {
      s.family = clamp(s.family - 7, 0, 100);
      s.rep = clamp(s.rep - 1, 0, 100);
      addLog(s, `🏠 Домашние в ярости: ${Math.round(noise)} дБ в комнате. Мама пригрозила выдернуть шнур.`, 'bad');
    } else if (noise > 45) {
      s.family = clamp(s.family - 2, 0, 100);
    } else if (noise < 35) {
      s.family = clamp(s.family + 2, 0, 100);
    }
  } else if (s.location === 'garage') {
    if (noise > 70) {
      s.rep = clamp(s.rep - 1, 0, 100);
      addLog(s, '🔊 Соседи по гаражу жалуются на гул. Нужна шумоизоляция.', 'warn');
    }
    if (s.family < 100) s.family = clamp(s.family + 1, 0, 100);
  } else if (s.family < 100) {
    s.family = clamp(s.family + 1, 0, 100);
  }
  if (s.family <= 10) {
    s.money -= 500;
    addLog(s, '😤 Семья в ультиматуме: «Или сервер, или мы». Пришлось потратить 500 ₽ на примирение.', 'bad');
    lines.push({ label: 'Примирение с семьёй', amount: -500 });
  }

  // Использование навыков и прогресс
  if (noise > 0) addSkill(s, { hardware: 0 });
  checkStage(s);
  if (s.rep >= 90 && s.stats.contractsDone >= 25 && s.stage === 3 && !s.victory) {
    s.victory = true;
    addLog(s, '🏆 Ты построил настоящий ЦОД: репутация 90+, 25 выполненных контрактов, BGP, генератор и команда. Это победа.', 'good');
  }
  if (s.money < -8000 && !s.gameOver) {
    s.gameOver = true;
    addLog(s, '💀 Долги превысили 8 000 ₽. Банк забрал стойку, мама забрала шнур. Игра окончена — но можно начать заново.', 'bad');
  }

  // Рынок
  s.listings.forEach((l) => (l.daysListed += 1));
  s.listings = s.listings.filter((l) => l.daysListed < 3 && !l.sold);
  if (chance(0.5)) {
    s.listings = s.listings.filter((l) => l.daysListed < 2 || chance(0.6));
  }
  const target = 7;
  if (s.listings.length < target) s.listings = s.listings.concat(generateListings(s, target - s.listings.length));

  // Заказы
  s.offers = refreshOffers(s);

  // Событие
  const event = rollEvent(s);
  if (event) {
    s.event = event;
    addLog(s, `Событие: ${event.title}`, event.tone === 'bad' ? 'warn' : 'info');
  }

  const report: DayReport = {
    day: s.day,
    income,
    expenses,
    lines,
    events: event ? [event.title] : [],
    failedContracts: failedNames,
    served,
    total: s.contracts.length,
    electric,
  };
  s.report = report;
  if (!event && (served > 0 || expenses > 0)) {
    addLog(s, `Итог дня: +${income.toLocaleString('ru-RU')} ₽ доход, −${expenses.toLocaleString('ru-RU')} ₽ расход.`, 'info');
  }
  return s;
}

function checkStage(s: GameState): void {
  if (s.stage === 1 && s.stats.contractsDone >= 3 && s.rep >= 12 && s.money >= 6000) {
    s.stage = 2;
    addLog(s, `📦 Этап 2: «Гаражный сисадмин». ${STAGE_INFO[2].subtitle}. Переезжай в гараж (вкладка «Путь»).`, 'good');
    s.rep = clamp(s.rep + 5, 0, 100);
  }
  if (s.stage === 2 && s.stats.contractsDone >= 8 && s.rep >= 35 && s.money >= 38000 && !s.flags.dc_unlocked) {
    s.flags.dc_unlocked = 1;
    addLog(s, '🏭 Открыт этап 3: можно арендовать помещение под ЦОД (вкладка «Путь», 40 000 ₽).', 'good');
  }
  if (s.stage === 2 && s.flags.dc_unlocked && s.location === 'dc') {
    s.stage = 3;
    addLog(s, `🏭 Этап 3: «Владелец ЦОД». ${STAGE_INFO[3].subtitle}.`, 'good');
    s.rep = clamp(s.rep + 8, 0, 100);
  }
}

export function upgradeById(id: string) {
  return UPGRADES.find((u) => u.id === id);
}

export function serviceById(id: string): ServiceDef | undefined {
  return SERVICES[id];
}

export function availableServicesFor(m: Machine): ServiceDef[] {
  return INSTALLABLE.filter((s) => !m.installed.includes(s.id));
}

export function machineUp(m: Machine): boolean {
  return derive(m).up;
}

export function summaryNoise(s: GameState): number {
  return Math.round(totalNoise(s));
}

export function summaryFirewall(s: GameState): number {
  return firewallScore(s);
}

export function summaryWatts(s: GameState): number {
  return totalWatts(s);
}

export function coresOf(m: Machine): number {
  return machineCores(m);
}

export const STAGE_ORDER: Stage[] = [1, 2, 3];
export const SAVE_KEY = 'homelab-save-v3';
export const SAVE_VERSION_CURRENT = SAVE_VERSION;
