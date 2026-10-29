import type { Machine, MachineKind, Part } from '../game/types';
import { FLAW_INFO } from '../game/data';

export const KIND_ICON: Record<MachineKind, string> = {
  desktop: '🖥️',
  phone: '📱',
  rack: '🗄️',
  blade: '🧱',
  storage: '💽',
};

export const KIND_NAME: Record<MachineKind, string> = {
  desktop: 'Десктоп',
  phone: 'Смартфон',
  rack: 'Стоечный сервер',
  blade: 'Блейд-шасси',
  storage: 'СХД',
};

export function machineIcon(m: Machine): string {
  return KIND_ICON[m.kind] ?? '🖥️';
}

export function machineStatus(m: Machine): { text: string; tone: 'green' | 'amber' | 'red' | 'cyan' } {
  if (m.crashed) return { text: 'АВАРИЯ', tone: 'red' };
  if (!m.powered) return { text: 'Выключен', tone: 'amber' };
  if (m.os === 'none') return { text: 'BIOS', tone: 'cyan' };
  if (m.temp > 85) return { text: 'Перегрев', tone: 'red' };
  return { text: 'Работает', tone: 'green' };
}

export function flawChip(p: Part): { text: string; tone: 'red' | 'amber' | 'green' } {
  if (p.flaw === 'none') return { text: 'без дефектов', tone: 'green' };
  if (p.flawKnown) return { text: FLAW_INFO[p.flaw].name, tone: 'red' };
  return { text: 'есть нюансы?', tone: 'amber' };
}

export function condTone(c: number): 'green' | 'amber' | 'red' {
  return c >= 75 ? 'green' : c >= 45 ? 'amber' : 'red';
}

export function partSpecs(p: Part): string[] {
  const s = p.stats;
  const out: string[] = [];
  if (s.cores) out.push(`${s.cores} ядер`);
  if (s.ghz) out.push(`${s.ghz} ГГц`);
  if (s.ramMB) out.push(`${s.ramMB >= 1024 ? `${(s.ramMB / 1024).toFixed(0)} ГБ` : `${s.ramMB} МБ`} ${s.memType?.toUpperCase() ?? ''}`);
  if (s.capacityGB) out.push(`${s.capacityGB >= 1024 ? `${(s.capacityGB / 1024).toFixed(1)} ТБ` : `${s.capacityGB} ГБ`}`);
  if (s.cpuSlots) out.push(`${s.cpuSlots} сокета`);
  if (s.ramSlots) out.push(`${s.ramSlots} слота RAM`);
  if (s.storageSlots) out.push(`${s.storageSlots} отсеков`);
  if (s.cooling) out.push(`охлаждение ${s.cooling}`);
  if (s.noise) out.push(`${s.noise} дБ`);
  if (s.ports) out.push(`${s.ports} порта`);
  if (s.rackUnits) out.push(`${s.rackUnits}U`);
  return out;
}
