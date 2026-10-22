#!/usr/bin/env python3
"""Generate allocation-free CLDC data tables from the recovered 1.8 oracle."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
data = json.loads((ROOT / 'original/recovered/inventory.json').read_text())
out = ROOT / 'src/main/java/wreckbox/core'
out.mkdir(parents=True, exist_ok=True)

def q(s):
    return '"' + str(s).replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'

def arr(name, typ, values, formatter=str):
    return '  public static final ' + typ + '[] ' + name + ' = {\n    ' + ',\n    '.join(formatter(v) for v in values) + '\n  };\n'

def f(v):
    if v is True: return '1.0f'
    if v is False or v is None: return '0.0f'
    return format(float(v), '.8g') + 'f'

def b(v): return 'true' if v else 'false'

def write(name, text):
    (out / name).write_text('package wreckbox.core;\n\n/** Generated from Wreckbox 1.8; do not hand-edit. */\npublic final class ' + name[:-5] + ' {\n' + text + '\n  private ' + name[:-5] + '() {}\n}\n')

m = data['materials']
text = '  public static final int COUNT = ' + str(len(m)) + ';\n'
text += arr('NAMES', 'String', [x['name'] for x in m], q)
text += arr('COLORS', 'int', [((x['base'][0] << 16) | (x['base'][1] << 8) | x['base'][2]) for x in m], lambda v: '0x%06x' % v)
text += arr('DUST', 'int', [((x['dust'][0] << 16) | (x['dust'][1] << 8) | x['dust'][2]) for x in m], lambda v: '0x%06x' % v)
for field, name in [('dens','DENSITY'),('hp','HP'),('comp','COMPRESSION'),('shear','SHEAR'),('tens','TENSION'),('flam','FLAMMABILITY'),('burn','BURN_TIME'),('brittle','BRITTLE'),('dmgMul','DAMAGE_MULTIPLIER'),('shatter','SHATTER')]:
    text += arr(name, 'float', [x[field] for x in m], f)
text += arr('VALUES', 'int', [x['value'] for x in m])
text += arr('BEARING', 'boolean', [x['bearing'] for x in m], b)
text += arr('FIXED', 'boolean', [x['fixed'] for x in m], b)
text += arr('INDESTRUCTIBLE', 'boolean', [x['indestructible'] for x in m], b)
text += arr('SOUND', 'String', [x['snd'] for x in m], q)
text += arr('DEBRIS', 'String', [x['debris'] for x in m], q)
write('Materials.java', text)

p = data['props']; ids = data['propIds']; props = [p[x] for x in ids]
text = '  public static final int COUNT = ' + str(len(props)) + ';\n'
text += arr('IDS', 'String', ids, q)
text += arr('NAMES', 'String', [x['name'] for x in props], q)
text += arr('CATEGORIES', 'String', [x['cat'] for x in props], q)
text += arr('SOUND', 'String', [x['snd'] for x in props], q)
text += arr('DEBRIS', 'String', [x['debris'] for x in props], q)
for field, name in [('w','WIDTH'),('h','HEIGHT'),('mass','MASS'),('hp','HP'),('flam','FLAMMABILITY'),('fragile','FRAGILE')]:
    text += arr(name, 'float', [x[field] for x in props], f)
text += arr('VALUES', 'int', [x['value'] for x in props])
text += arr('VARIANTS', 'int', [x['variants'] for x in props])
text += arr('FLAGS', 'int', [(1 if x.get('vehicle') else 0)|(2 if x.get('hang') else 0)|(4 if x.get('pop') else 0)|(8 if x.get('light') else 0)|(16 if x.get('keel') else 0)|(32 if x.get('noSwing') else 0)|(64 if x.get('ammo') else 0) for x in props])
for field, name in [('r','EXPLOSION_RADIUS'),('p','EXPLOSION_POWER'),('fire','EXPLOSION_FIRE'),('fuse','EXPLOSION_FUSE'),('lag','EXPLOSION_LAG')]:
    text += arr(name, 'float', [x.get('explosive',{}).get(field,0) for x in props], f)
text += arr('EXPLOSION_FLAGS', 'int', [(1 if x.get('explosive',{}).get('napalm') else 0)|(2 if x.get('explosive',{}).get('onlyFire') else 0)|(4 if x.get('explosive',{}).get('steam') else 0) for x in props])
text += arr('LIGHT_RADIUS', 'float', [x.get('light',{}).get('r',0) for x in props], f)
text += '''  public static int find(String id) {
    for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
    return -1;
  }
'''
write('Props.java', text)

tools = data['tools']
text = '  public static final int COUNT = ' + str(len(tools)) + ';\n'
for field, name in [('id','IDS'),('name','NAMES'),('mode','MODES')]:
    text += arr(name, 'String', [x.get(field) or 'destroy' for x in tools], q)
text += arr('EXTRAS', 'String', [x['extra'] or '' for x in tools], q)
for field, name in [('min','SLIDER_MIN'),('max','SLIDER_MAX'),('def','SLIDER_DEFAULT')]:
    text += arr(name, 'int', [(x['slider'] or {}).get(field,0) for x in tools])
text += arr('SLIDER_LABELS', 'String', [(x['slider'] or {}).get('label','') for x in tools], q)
text += '''  public static int find(String id) {
    for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
    return -1;
  }
'''
write('Tools.java', text)

buildings = data['buildings']
widths = {'house':(30,38),'cottage':(28,34),'shop':(38,46),'brick':(40,60),'panel':(48,60),'office':(46,56),'sky':(56,56),'factory':(98,98),'hospital':(58,66),'firestation':(56,62),'police':(52,58),'gas':(58,58),'clinic':(46,46)}
text = '  public static final int COUNT = ' + str(len(buildings)) + ';\n'
text += arr('IDS', 'String', [x['id'] for x in buildings], q)
text += arr('NAMES', 'String', [x['name'] for x in buildings], q)
text += arr('PREFIX', 'int', [x.get('pre',0) for x in buildings])
text += arr('HEIGHT', 'int', [x['h'] for x in buildings])
text += arr('MIN_WIDTH', 'int', [widths[x['id']][0] for x in buildings])
text += arr('MAX_WIDTH', 'int', [widths[x['id']][1] for x in buildings])
write('Buildings.java', text)

maps = list(data['maps'].items())
text = '  public static final int COUNT = ' + str(len(maps)) + ';\n'
text += arr('IDS', 'String', [x[0] for x in maps], q)
text += arr('NAMES', 'String', [x[1] for x in maps], q)
text += arr('WIDTHS', 'int', [data['mobileWidths'][x[0]] for x in maps])
text += '''  public static int find(String id) {
    for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
    return -1;
  }
'''
write('Maps.java', text)
print('Generated tables:', len(m), 'materials,', len(props), 'props,', len(tools), 'tools,', len(buildings), 'building types,', len(maps), 'maps')
