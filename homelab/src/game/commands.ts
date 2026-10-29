import { SERVICES } from './data';
import { machineCores, machineRam, usableRam, machineLoad, machineStorage, machineStorageCap } from './rules';
import type { GameState, Machine } from './types';

export type LineTone = 'info' | 'good' | 'bad' | 'warn' | 'cmd' | 'dim';

export interface CmdResult {
  lines: { text: string; tone?: LineTone }[];
  machine?: Machine;
  money?: number;
  skill?: { hardware?: number; network?: number; sysadmin?: number };
  rep?: number;
  flag?: string;
  clear?: boolean;
}

const HELP = [
  'Доступные команды (учебный терминал, как в жизни, только без риска):',
  '  apt update                 — обновить список пакетов',
  '  apt install <пакет>        — установить сервис (samba, nginx, jellyfin, ...)',
  '  apt remove <пакет>         — удалить сервис',
  '  systemctl status <unit>    — состояние сервиса',
  '  systemctl start|stop <unit>— запустить/остановить сервис',
  '  ufw enable / ufw allow 80  — настроить фаервол',
  '  free -h                    — сколько памяти',
  '  df -h                      — сколько места на дисках',
  '  top                        — что грузит процессор',
  '  lsblk / smartctl -a /dev/sda — диски и их здоровье',
  '  sensors                    — температуры',
  '  ip a                       — сеть, адреса и порты',
  '  docker compose up -d       — поднять всё установленное разом',
  '  neofetch                   — показать, что у тебя за железо',
  '  help                       — эта справка',
];

function pkgToService(pkg: string): string | null {
  const s = Object.values(SERVICES).find((x) => x.linuxPackage === pkg || x.linuxUnit === pkg || x.id === pkg);
  return s ? s.id : null;
}

function bar(used: number, total: number, width = 18): string {
  const ratio = total <= 0 ? 1 : Math.min(1, used / total);
  const filled = Math.round(ratio * width);
  return `${'█'.repeat(filled)}${'░'.repeat(width - filled)} ${Math.round(ratio * 100)}%`;
}

export function execCommand(state: GameState, machine: Machine, raw: string): CmdResult {
  // ВАЖНО: функция меняет переданную машину на месте — редьюсер передаёт копию
  const m: Machine = machine;
  let input = raw.trim();
  if (!input) return { lines: [] };
  const history = state.flags['shell_history'] ?? 0;
  void history;

  if (input.startsWith('sudo ')) input = input.slice(5).trim();
  const parts = input.split(/\s+/);
  const cmd = parts[0].toLowerCase();
  const args = parts.slice(1);
  const out = (text: string, tone?: LineTone) => ({ text, tone });

  if (m.os !== 'linux') {
    return {
      lines: [out('bash: эта оболочка есть только на Linux. На Windows Server работай мышкой.', 'bad')],
    };
  }
  if (!m.powered) {
    return { lines: [out('Нет соединения: сервер выключен.', 'bad')] };
  }
  if (m.crashed) {
    return { lines: [out('Сервер в аварийном состоянии. Сначала ремонт.', 'bad')] };
  }

  switch (cmd) {
    case 'help':
    case '?':
      return { lines: HELP.map((t) => out(t, 'dim')) };
    case 'clear':
      return { lines: [], clear: true };
    case 'ls':
      return {
        lines: [out('bin  boot  dev  etc  home  lib  media  mnt  opt  proc  root  run  srv  sys  tmp  usr  var', 'info')],
      };
    case 'whoami':
      return { lines: [out('root', 'info')] };
    case 'pwd':
      return { lines: [out('/root', 'info')] };
    case 'history':
      return { lines: [out('  1  apt update', 'dim'), out('  2  apt install samba', 'dim'), out('  3  systemctl status smbd', 'dim'), out('  4  df -h', 'dim')] };
    case 'neofetch': {
      const d = {
        cores: machineCores(m),
        ram: machineRam(m),
        disk: machineStorage(m),
        services: m.services.length,
      };
      return {
        lines: [
          out(`       .-.        root@${m.name.toLowerCase().replace(/\s+/g, '-')}`, 'good'),
          out('      |o_o|       ------------------------', 'dim'),
          out('      |:_/|       OS: Ubuntu Server 22.04 LTS', 'dim'),
          out(`     //   \\ \\      Host: ${m.base.name}`, 'dim'),
          out(`    (|     | )     CPU: ${d.cores} ядер`, 'dim'),
          out(`   /'\\_   _/\`\\     RAM: ${d.ram} МБ`, 'dim'),
          out(`   \\___)=(___/     Disk: ${d.disk} ГБ`, 'dim'),
          out(`                   Services: ${d.services}`, 'dim'),
        ],
      };
    }
    case 'apt': {
      const sub = args[0] ?? '';
      const pkg = (args[1] ?? '').toLowerCase();
      if (sub === 'update') {
        return {
          lines: [
            out('Hit:1 http://archive.ubuntu.com/ubuntu jammy InRelease', 'dim'),
            out('Get:2 http://archive.ubuntu.com/ubuntu jammy-updates InRelease [119 kB]', 'dim'),
            out('Reading package lists... Done', 'dim'),
            out('Список пакетов обновлён. Теперь можно ставить сервисы.', 'good'),
          ],
          skill: { sysadmin: 1 },
        };
      }
      if (sub === 'install') {
        if (!pkg) return { lines: [out('apt install: не указан пакет. Пример: apt install nginx', 'bad')] };
        const id = pkgToService(pkg);
        if (!id) {
          return { lines: [out(`E: Unable to locate package ${pkg}`, 'bad'), out('Подсказка: samba, nginx, jellyfin, minecraft-server, wireguard, postfix, qbittorrent-nox, restic, motion, unbound', 'dim')] };
        }
        const svc = SERVICES[id];
        if (svc.phoneOnly) return { lines: [out('E: Этот пакет не для сервера.', 'bad')] };
        if (m.installed.includes(id)) return { lines: [out(`${svc.name} уже установлен.`, 'warn')] };
        const load = machineLoad(m);
        const ramNeeded = load.ram + svc.ramMB;
        if (ramNeeded > usableRam(m) - 128) {
          return {
            lines: [
              out(`E: Недостаточно памяти. ${svc.name} требует ${svc.ramMB} МБ, свободно ${Math.max(0, usableRam(m) - load.ram)} МБ.`, 'bad'),
              out('Добавь планку памяти или останови лишние сервисы.', 'dim'),
            ],
          };
        }
        if (load.storage + svc.storageGB + 12 > machineStorageCap(m)) {
          return { lines: [out(`E: Недостаточно места. ${svc.name} требует ${svc.storageGB} ГБ.`, 'bad')] };
        }
        m.installed.push(id);
        m.services.push(id);
        return {
          lines: [
            out(`Reading package lists... Done`, 'dim'),
            out(`Building dependency tree... Done`, 'dim'),
            out(`The following NEW packages will be installed: ${svc.linuxPackage}`, 'dim'),
            out(`Unpacking ${svc.linuxPackage}...`, 'dim'),
            out(`Setting up ${svc.linuxPackage}...`, 'dim'),
            out(`Created symlink /etc/systemd/system/multi-user.target.wants/${svc.linuxUnit}.service.`, 'dim'),
            out(`✓ ${svc.name} установлен и запущен. Память: ${svc.ramMB} МБ, ядра: ${svc.cores}.`, 'good'),
          ],
          skill: { sysadmin: 2 },
        };
      }
      if (sub === 'remove' || sub === 'purge') {
        const id = pkgToService(pkg);
        if (!id) return { lines: [out(`E: Пакет ${pkg} не найден`, 'bad')] };
        if (!m.installed.includes(id)) return { lines: [out('Пакет не установлен', 'warn')] };
        m.installed = m.installed.filter((x) => x !== id);
        m.services = m.services.filter((x) => x !== id);
        return { lines: [out(`Removing ${SERVICES[id].linuxPackage}... Done`, 'dim'), out(`${SERVICES[id].name} удалён.`, 'good')], skill: { sysadmin: 1 } };
      }
      return { lines: [out('Использование: apt update | apt install <пакет> | apt remove <пакет>', 'warn')] };
    }
    case 'systemctl': {
      const sub = args[0];
      const unit = (args[1] ?? '').replace(/\.service$/, '');
      const id = pkgToService(unit);
      if (!sub) return { lines: [out('systemctl: нужна команда (status/start/stop/restart/list-units)', 'warn')] };
      if (sub === 'list-units' || sub === 'list-unit-files') {
        const rows = Object.values(SERVICES)
          .filter((s) => s.linuxPackage)
          .map((s) => {
            const act = m.services.includes(s.id) ? 'active (running)' : 'inactive (dead)';
            return out(`  ${m.services.includes(s.id) ? '●' : '○'} ${s.linuxUnit}.service    ${act}`, m.services.includes(s.id) ? 'good' : 'dim');
          });
        return { lines: [out('UNIT                    LOAD   ACTIVE', 'dim'), ...rows] };
      }
      if (!id) return { lines: [out(`Unit ${unit}.service could not be found.`, 'bad')] };
      const svc = SERVICES[id];
      if (sub === 'status') {
        const active = m.services.includes(id);
        const load = machineLoad(m);
        return {
          lines: [
            out(`● ${svc.linuxUnit}.service - ${svc.name}`, active ? 'good' : 'warn'),
            out(`     Loaded: loaded (/lib/systemd/system/${svc.linuxUnit}.service; enabled)`, 'dim'),
            out(`     Active: ${active ? 'active (running)' : 'inactive (dead)'}`, active ? 'good' : 'warn'),
            out(`     Memory: ${(svc.ramMB / 1024).toFixed(2)}G`, 'dim'),
            out(`     CPU: ${svc.cores} ядра`, 'dim'),
            out(`     Нагрузка сервера: ОЗУ ${load.ram}/${usableRam(m)} МБ`, 'dim'),
          ],
        };
      }
      if (!m.installed.includes(id)) {
        return { lines: [out(`Failed to ${sub} ${svc.linuxUnit}.service: Unit not found (пакет не установлен)`, 'bad')] };
      }
      if (sub === 'start' || sub === 'restart' || sub === 'enable') {
        if (m.services.includes(id) && sub !== 'restart') return { lines: [out(`${svc.linuxUnit}.service уже запущен`, 'warn')] };
        const load = machineLoad(m);
        if (!m.services.includes(id) && load.ram + svc.ramMB > usableRam(m) - 128) {
          return { lines: [out('Job for ' + svc.linuxUnit + '.service failed: not enough memory.', 'bad')] };
        }
        if (!m.services.includes(id)) m.services.push(id);
        return { lines: [out(`✓ ${svc.linuxUnit}.service: ${sub === 'restart' ? 'перезапущен' : 'запущен'}`, 'good')], skill: { sysadmin: 1 } };
      }
      if (sub === 'stop' || sub === 'disable') {
        if (!m.services.includes(id)) return { lines: [out(`${svc.linuxUnit}.service уже остановлен`, 'warn')] };
        m.services = m.services.filter((x) => x !== id);
        return { lines: [out(`${svc.linuxUnit}.service остановлен.`, 'warn')] };
      }
      return { lines: [out('systemctl: неизвестное действие', 'warn')] };
    }
    case 'ufw': {
      const sub = args[0];
      if (sub === 'enable') {
        m.firewall = Math.min(25, m.firewall + 8);
        return { lines: [out('Firewall is active and enabled on system startup', 'good'), out(`Защита сервера: +${Math.min(25, m.firewall)}`, 'dim')], skill: { network: 2 } };
      }
      if (sub === 'allow') {
        m.firewall = Math.min(25, m.firewall + 3);
        return { lines: [out(`Rule added: allow ${args.slice(1).join(' ') || '22/tcp'}`, 'good')], skill: { network: 1 } };
      }
      if (sub === 'status') {
        return { lines: [out('Status: active', 'good'), out('To                         Action      From', 'dim'), out('22/tcp                     ALLOW       Anywhere', 'dim'), out('80,443/tcp                 ALLOW       Anywhere', 'dim'), out('32400/tcp                  ALLOW       Anywhere', 'dim')] };
      }
      return { lines: [out('Использование: ufw enable | ufw allow 80 | ufw status', 'warn')] };
    }
    case 'free': {
      const total = machineRam(m);
      const used = Math.min(total, machineLoad(m).ram + 512);
      const avail = Math.max(0, total - used);
      return {
        lines: [
          out('               total        used        free      shared  buff/cache   available', 'dim'),
          out(`Mem:        ${String(total).padStart(7)}M  ${String(used).padStart(8)}M  ${String(avail).padStart(8)}M  ${'0M'.padStart(8)}  ${'256M'.padStart(9)}  ${String(avail).padStart(9)}M`, 'info'),
          out(`Swap:             0B          0B          0B`, 'dim'),
          out(`Загрузка ОЗУ: ${bar(used, total)}`, used / total > 0.9 ? 'bad' : 'good'),
        ],
      };
    }
    case 'df': {
      const total = machineStorage(m);
      const used = machineLoad(m).storage + 12;
      return {
        lines: [
          out('Filesystem      Size  Used Avail Use% Mounted on', 'dim'),
          out(`/dev/sda1       ${String(total).padStart(4)}G  ${String(used).padStart(4)}G  ${String(Math.max(0, total - used)).padStart(4)}G  ${String(Math.round((used / Math.max(1, total)) * 100)).padStart(3)}% /`, 'info'),
          out(`Заполнено дисков: ${bar(used, total)}`, used / total > 0.92 ? 'bad' : 'good'),
        ],
      };
    }
    case 'lsblk':
      return {
        lines: [
          out('NAME   MAJ:MIN RM   SIZE RO TYPE MOUNTPOINTS', 'dim'),
          ...m.storage.filter(Boolean).map((d, i) =>
            out(`sd${String.fromCharCode(97 + i)}      8:${i}    0 ${String(d!.stats.capacityGB ?? 0).padStart(5)}G  0 disk /`, 'info'),
          ),
        ],
      };
    case 'smartctl': {
      const disk = m.storage.filter(Boolean)[0];
      if (!disk) return { lines: [out('/dev/sda: not found', 'bad')] };
      const bad = disk.flaw === 'badsectors';
      disk.flawKnown = true;
      return {
        lines: [
          out(`smartctl 7.2 - ${disk.name}`, 'dim'),
          out(`SMART overall-health self-assessment test result: ${bad ? 'FAILED!' : 'PASSED'}`, bad ? 'bad' : 'good'),
          out(`Reallocated_Sector_Ct   ${bad ? '148' : '0'}`, bad ? 'bad' : 'dim'),
          out(`Power_On_Hours          ${12000 + disk.condition * 130}`, 'dim'),
          out(bad ? '⚠ Обнаружены переназначенные сектора. Диск сыпется — нужна замена.' : 'Диск выглядит здоровым.', bad ? 'bad' : 'good'),
        ],
        skill: { hardware: 2 },
      };
    }
    case 'memtest':
    case 'memtester': {
      const flawed = m.ram.find((r) => r && r.flaw === 'memoryerrors');
      if (flawed) flawed.flawKnown = true;
      return {
        lines: flawed
          ? [out('memtester version 4.5.1', 'dim'), out('FAILURE: 0x000001a4 != 0x000001a0 at offset 0x1f3d2c40', 'bad'), out(`Похоже, планка ${flawed.name} сыпет ошибками.`, 'bad')]
          : [out('memtester version 4.5.1', 'dim'), out('ok — все тесты пройдены', 'good')],
        skill: { hardware: 2 },
      };
    }
    case 'sensors': {
      return {
        lines: [
          out('coretemp-isa-0000', 'dim'),
          out(`Package id 0:  +${m.temp.toFixed(1)}°C  ${m.temp > 80 ? '(критично!)' : m.temp > 65 ? '(высокая)' : '(ok)'}`, m.temp > 80 ? 'bad' : m.temp > 65 ? 'warn' : 'good'),
          out(`Ambient:       +${(m.temp - 12).toFixed(1)}°C`, 'dim'),
          out(`Вентиляторы:   ${machineCores(m) > 8 ? '4800' : '2100'} RPM`, 'dim'),
        ],
      };
    }
    case 'top':
    case 'htop': {
      const load = machineLoad(m);
      const rows = m.services.map((id) => {
        const s = SERVICES[id];
        return out(`  ${String(Math.round(s.cores * 100)).padStart(5)}  ${String(s.ramMB).padStart(6)}  ${s.linuxUnit ?? s.id}`, 'info');
      });
      return {
        lines: [
          out(`top - uptime ${m.uptime} дн., load average: ${(load.cores / Math.max(1, machineCores(m))).toFixed(2)}`, 'dim'),
          out('  %CPU   RES(MB)  COMMAND', 'dim'),
          ...rows,
          out(`  Итого: ${Math.round(load.cores * 100)}% CPU, ${load.ram} МБ ОЗУ`, 'dim'),
        ],
      };
    }
    case 'uptime':
      return { lines: [out(` ${new Date().toLocaleTimeString('ru-RU')} up ${m.uptime} days,  2 users,  load average: 0.42, 0.51, 0.48`, 'info')] };
    case 'ip': {
      const up = m.services.length > 0;
      return {
        lines: [
          out('1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536', 'dim'),
          out('    inet 127.0.0.1/8 scope host lo', 'dim'),
          out(`2: enp3s0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 state UP`, 'dim'),
          out(`    inet 192.168.1.${40 + (m.name.length % 50)}/24 brd 192.168.1.255 scope global enp3s0`, 'good'),
          out(`    Открытые порты: ${up ? '22, 80, 443, 8096' : '22'}`, 'info'),
        ],
      };
    }
    case 'ping': {
      const host = args[0] ?? '8.8.8.8';
      return {
        lines: [
          out(`PING ${host} (${host}) 56(84) bytes of data.`, 'dim'),
          out(`64 bytes from ${host}: icmp_seq=1 ttl=117 time=${(Math.random() * 20 + 3).toFixed(1)} ms`, 'dim'),
          out(`64 bytes from ${host}: icmp_seq=2 ttl=117 time=${(Math.random() * 20 + 3).toFixed(1)} ms`, 'dim'),
          out(`64 bytes from ${host}: icmp_seq=3 ttl=117 time=${(Math.random() * 20 + 3).toFixed(1)} ms`, 'dim'),
          out(`--- ${host} ping statistics --- 3 packets transmitted, 3 received, 0% packet loss`, 'good'),
        ],
        skill: { network: 1 },
      };
    }
    case 'curl':
      return { lines: [out('HTTP/1.1 200 OK', 'good'), out('Server: nginx/1.18.0 (Ubuntu)', 'dim'), out('Content-Type: text/html', 'dim'), out('<html><body><h1>It works!</h1></body></html>', 'dim')] };
    case 'docker': {
      if (args[0] !== 'compose') return { lines: [out('docker: попробуй `docker compose up -d`', 'warn')] };
      if (!m.services.length) return { lines: [out('no configuration file provided: not found', 'bad')] };
      return {
        lines: [
          out('Creating network "homelab_default" with the default driver', 'dim'),
          ...m.services.map((id) => out(` ✔ Container ${SERVICES[id].linuxUnit ?? id}  Started`, 'good')),
          out('Все сервисы подняты через docker compose. Красиво.', 'good'),
        ],
        skill: { sysadmin: 3 },
      };
    }
    case 'crontab':
      return { lines: [out('crontab: installing new crontab', 'dim'), out('0 3 * * * /usr/local/bin/backup.sh  # бэкапы в 3 утра', 'dim'), out('Главное правило: бэкап, который не проверяли — не бэкап.', 'good')], skill: { sysadmin: 2 } };
    case 'nano':
    case 'vim':
      return { lines: [out(`Открыт ${args[0] ?? '/etc/nginx/nginx.conf'}. (В учебном терминале редактор закрылся сам, не переживай.)`, 'dim')] };
    case 'chmod':
      return { lines: [out('Права обновлены. Теперь скрипт можно запускать.', 'good')] };
    case 'reboot':
    case 'poweroff': {
      m.powered = false;
      m.services = [...m.installed];
      return { lines: [out(cmd === 'reboot' ? 'Rebooting...' : 'Powering off...', 'warn'), out('Сервер выключен. Включи его кнопкой питания.', 'dim')] };
    }
    case 'vtysh':
    case 'bird':
    case 'bgp':
      return {
        lines: [
          out('FRRouting 8.4 — vtysh', 'dim'),
          out('router bgp 64512', 'dim'),
          out('  neighbor 10.20.0.1 remote-as 64500', 'dim'),
          out('  address-family ipv4 unicast', 'dim'),
          out('    network 192.168.1.0/24', 'dim'),
          out('BGP state = Established, up for 00:14:22', 'good'),
          out('Prefixes received: 892 000. Hold time: 180 sec.', 'dim'),
          out('Если сессия не поднимается — проверь AS, MTU и firewall. Классика.', 'warn'),
        ],
        skill: { network: 3 },
      };
    case 'exit':
      return { lines: [out('logout', 'dim')] };
    case 'rm':
      return { lines: [out('rm: отказано. Это типичная ловушка для новичков — не удаляй / на сервере.', 'bad')] };
    default:
      return { lines: [out(`bash: ${cmd}: command not found`, 'bad'), out('Введи `help`, чтобы увидеть список команд.', 'dim')] };
  }
}

export const COMMAND_HINTS = ['apt update', 'apt install samba', 'systemctl status smbd', 'free -h', 'df -h', 'sensors', 'ip a', 'docker compose up -d', 'neofetch', 'help'];

