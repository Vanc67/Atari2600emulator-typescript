#!/usr/bin/env python3
# =============================================================================
#  ChipLab - generator of the bundled example chips (assets/*.bin)
#  and the AI prompt (assets/prompt.txt).
#
#  Every chip in the game - CPU, RAM, GPU, display - is ONE .bin file in the
#  CHIPBIN1 format. This script assembles the example programs with a tiny
#  two-pass assembler and writes real .bin files that the game parses,
#  simulates and draws under the "microscope".
#
#  Usage:  python3 tools/make_bins.py [assets-dir]
# =============================================================================
import base64
import math
import os
import struct
import sys

# ---------------------------------------------------------------------------
#  ISA (must match src/com/chiplab/Cpu.java and the AI prompt!)
# ---------------------------------------------------------------------------
OPS = {
    # mnemonic: (opcode, operand kinds)   r=register, i=imm8, a=addr8, j=addr16
    "NOP":    (0x00, ""),
    "LDI":    (0x01, "ri"),
    "MOV":    (0x02, "rr"),
    "ADD":    (0x03, "rr"),
    "SUB":    (0x04, "rr"),
    "AND":    (0x05, "rr"),
    "OR":     (0x06, "rr"),
    "XOR":    (0x07, "rr"),
    "NOT":    (0x08, "r"),
    "SHL":    (0x09, "r"),
    "SHR":    (0x0A, "r"),
    "INC":    (0x0B, "r"),
    "DEC":    (0x0C, "r"),
    "LOAD":   (0x0D, "ra"),
    "STORE":  (0x0E, "ar"),
    "JMP":    (0x0F, "j"),
    "JZ":     (0x10, "j"),
    "JNZ":    (0x11, "j"),
    "JC":     (0x12, "j"),
    "OUT":    (0x13, "r"),
    "IN":     (0x14, "r"),
    "CMP":    (0x15, "rr"),
    "LOADR":  (0x16, "rr"),
    "STORER": (0x17, "rr"),
    "HALT":   (0xFF, ""),
}
OPLEN = {code: 1 + sum(2 if k == "j" else 1 for k in kinds)
         for code, kinds in OPS.values()}


def assemble(src):
    """Two-pass assembler. Lines: [label:] MNEM op, op  ; comment"""
    labels = {}
    items = []  # (mnemonic, [operands], lineno)
    for lineno, raw in enumerate(src.strip().splitlines(), 1):
        line = raw.split(";")[0].strip()
        if not line:
            continue
        if ":" in line:
            lab, line = line.split(":", 1)
            labels[lab.strip()] = None  # address fixed in pass 1 below
            cur_label = lab.strip()
            line = line.strip()
            if not line:
                items.append(("__LABEL__", [cur_label], lineno))
                continue
        else:
            cur_label = None
        parts = line.replace(",", " ").split()
        mnem, operands = parts[0].upper(), parts[1:]
        if cur_label:
            items.append(("__LABEL__", [cur_label], lineno))
        items.append((mnem, operands, lineno))
    # pass 1: addresses
    addr = 0
    for mnem, operands, lineno in items:
        if mnem == "__LABEL__":
            labels[operands[0]] = addr
        else:
            if mnem not in OPS:
                raise ValueError("line %d: unknown mnemonic %s" % (lineno, mnem))
            addr += OPLEN[OPS[mnem][0]]
    # pass 2: emit
    out = bytearray()
    for mnem, operands, lineno in items:
        if mnem == "__LABEL__":
            continue
        code, kinds = OPS[mnem]
        if len(operands) != len(kinds):
            raise ValueError("line %d: %s expects %d operands" % (lineno, mnem, len(kinds)))
        out.append(code)
        for kind, operand in zip(kinds, operands):
            if kind == "r":
                if not operand.upper().startswith("R"):
                    raise ValueError("line %d: bad register %s" % (lineno, operand))
                out.append(int(operand[1:]))
            elif kind in ("i", "a"):
                out.append(int(operand, 0) & 0xFF)
            elif kind == "j":
                target = labels[operand] if operand in labels else int(operand, 0)
                out += struct.pack("<H", target)
    return bytes(out)


# ---------------------------------------------------------------------------
#  CHIPBIN1 writer (must match src/com/chiplab/ChipBin.java)
# ---------------------------------------------------------------------------
KIND = {"logic": 0, "regs": 1, "ram": 2, "alu": 3, "bus": 4, "screen": 5, "control": 6}


def chipbin(type_, bits, regs, mem, clock_khz, screen_w, screen_h, transistors,
            name, desc, blocks, isa_text, program):
    name_b = name.encode("utf-8")
    desc_b = desc.encode("utf-8")
    isa_b = isa_text.encode("utf-8")
    blocks_b = bytearray()
    for (bname, x, y, w, h, trans, kind, param) in blocks:
        nb = bname.encode("utf-8")
        blocks_b.append(len(nb))
        blocks_b += nb
        blocks_b += struct.pack("<BBBBHB B", x, y, w, h, trans, KIND[kind], param)
    header = struct.pack(
        "<8sBBBBHHBBHHHH",
        b"CHIPBIN1", 1, type_, bits & 0xFF, regs & 0xFF,
        mem & 0xFFFF, clock_khz & 0xFFFF, screen_w & 0xFF, screen_h & 0xFF,
        transistors & 0xFFFF, len(program) & 0xFFFF, len(name_b) & 0xFFFF,
        len(desc_b) & 0xFFFF)
    # header is 28 bytes here; spec fixes a 32 byte header: pad + last two lens
    header = header + struct.pack("<HHH", len(blocks_b) & 0xFFFF, len(isa_b) & 0xFFFF, 0)
    assert len(header) == 32, len(header)
    return header + name_b + desc_b + bytes(blocks_b) + isa_b + program


ISA_TEXT = (
    "00 NOP        ничего не делает\n"
    "01 LDI Rd,n   Rd = n\n"
    "02 MOV Rd,Rs  Rd = Rs\n"
    "03 ADD Rd,Rs  Rd = Rd + Rs (флаги Z,C)\n"
    "04 SUB Rd,Rs  Rd = Rd - Rs (флаги Z,C)\n"
    "05 AND Rd,Rs  Rd = Rd & Rs\n"
    "06 OR  Rd,Rs  Rd = Rd | Rs\n"
    "07 XOR Rd,Rs  Rd = Rd ^ Rs\n"
    "08 NOT Rd     Rd = ~Rd\n"
    "09 SHL Rd     Rd = Rd << 1 (C = выпавший бит)\n"
    "0A SHR Rd     Rd = Rd >> 1 (C = выпавший бит)\n"
    "0B INC Rd     Rd = Rd + 1\n"
    "0C DEC Rd     Rd = Rd - 1\n"
    "0D LOAD Rd,a  Rd = ПАМЯТЬ[a]\n"
    "0E STORE a,Rs ПАМЯТЬ[a] = Rs\n"
    "0F JMP a      переход на адрес a (2 байта, младший первым)\n"
    "10 JZ a       переход, если Z=1\n"
    "11 JNZ a      переход, если Z=0\n"
    "12 JC a       переход, если C=1 (был перенос)\n"
    "13 OUT Rs     вывести Rs на выходной порт (число/пиксель)\n"
    "14 IN Rd      Rd = входной порт (счётчик времени игры)\n"
    "15 CMP Rd,Rs  сравнить Rd и Rs (как SUB, но без записи)\n"
    "16 LOADR Rd,Rs  Rd = ПАМЯТЬ[Rs]\n"
    "17 STORER Ra,Rv ПАМЯТЬ[Ra] = Rv\n"
    "FF HALT       стоп\n"
    "Z=1 если результат 0. C=1 если был перенос за пределы разрядности."
)


def cpu_blocks(alu_t, reg_t, ram_t, ctrl_t, with_screen=False, ram_cells=0):
    b = [
        ("УСТРОЙСТВО УПРАВЛЕНИЯ", 0, 0, 7, 3, ctrl_t, "control", 0),
        ("АЛУ", 8, 0, 7, 3, alu_t, "alu", 0),
        ("РЕГИСТРЫ", 0, 4, 7, 5, reg_t, "regs", 0),
        ("ОЗУ", 8, 4, 7, 5, ram_t, "ram", 0),
        ("ШИНА ДАННЫХ", 0, 10, 15, 1, 24, "bus", 0),
        ("ВЫВОДЫ / ПИНЫ", 0, 12, 15, 2, 16, "logic", 0),
    ]
    if with_screen:
        b[3] = ("ВИДЕОПАМЯТЬ", 8, 4, 7, 5, ram_t, "screen", 0)
    return b


CHIPS = []


def add(fname, data):
    CHIPS.append((fname, data))
    return data


# ---------------------------------------------------------------------------
#  1-bit CPU - a living full adder
# ---------------------------------------------------------------------------
prog1 = assemble("""
        LDI R1, 1        ; константа 1
loop:   OUT R0           ; показать бит A на светодиоде
        ADD R0, R1       ; A = A + 1 : 0+1=1(C=0), 1+1=0(C=1)
        JC carry         ; был перенос?
        JMP loop
carry:  LOAD R0, 0       ; счётчик переносов в памяти[0] (тоже 1 бит!)
        INC R0
        STORE 0, R0
        LDI R0, 0
        JMP loop
""")
cpu1 = add("01_cpu1.bin", chipbin(
    1, 1, 2, 4, 1, 0, 0, 92,
    "1-БИТНЫЙ ПРОЦЕССОР «БИТ-1»",
    "Самый маленький настоящий процессор. Регистры хранят всего 1 бит: 0 или 1.\n"
    "АЛУ - это один полный сумматор из ~28 транзисторов: СУММА = A xor B xor Cin, "
    "ПЕРЕНОС = большинство(A,B,Cin).\n"
    "Программа прибавляет 1 снова и снова: 0+1=1, а 1+1=0 и перенос C=1. "
    "Открой МИКРОСКОП -> блок АЛУ: видно каждый транзистор сумматора.\n"
    "Транзисторов всего: 92. Частота: 1 кГц. Память: 4 ячейки по 1 биту.",
    cpu_blocks(28, 16, 24, 24), ISA_TEXT, prog1))

# ---------------------------------------------------------------------------
#  4-bit CPU - counter 0..15
# ---------------------------------------------------------------------------
prog4 = assemble("""
        LDI R0, 0
loop:   OUT R0           ; вывести число
        STORER R0, R0    ; ПАМЯТЬ[i] = i
        INC R0           ; после 15 будет 0 и Z=1
        JNZ loop
        HALT
""")
add("02_cpu4.bin", chipbin(
    1, 4, 4, 16, 10, 0, 0, 640,
    "4-БИТНЫЙ ПРОЦЕССОР «НИББЛ-4»",
    "4 бита = числа 0..15 (один шестнадцатеричный разряд, «ниббл»).\n"
    "АЛУ - цепочка из 4 полных сумматоров: перенос бежит от бита к биту, "
    "это видно под микроскопом.\n"
    "Программа считает 0,1,2,...15, выводит каждое число (OUT) и записывает "
    "его в ОЗУ по адресу, равному самому числу (STORER).\n"
    "Регистров: 4. ОЗУ: 16 ячеек x 4 бита. Транзисторов: ~640 (как у Intel 4004 - у него было 2300).",
    cpu_blocks(140, 96, 256, 148), ISA_TEXT, prog4))

# ---------------------------------------------------------------------------
#  8-bit CPU - Fibonacci
# ---------------------------------------------------------------------------
prog8 = assemble("""
        LDI R3, 12
        STORE 60, R3     ; ПАМЯТЬ[60] = 12 (сколько чисел)
        LDI R0, 0        ; a = 0
        LDI R1, 1        ; b = 1
        LDI R2, 0        ; i = 0
loop:   OUT R0           ; вывести a
        STORER R2, R0    ; ПАМЯТЬ[i] = a
        MOV R3, R0       ; t = a
        ADD R3, R1       ; t = a + b
        MOV R0, R1       ; a = b
        MOV R1, R3       ; b = t
        INC R2           ; i++
        LOAD R3, 60      ; R3 = 12
        CMP R2, R3       ; i == 12 ?
        JNZ loop
        HALT
""")
add("03_cpu8.bin", chipbin(
    1, 8, 4, 64, 100, 0, 0, 3500,
    "8-БИТНЫЙ ПРОЦЕССОР «БАЙТ-8»",
    "Классика: 8 бит = 1 байт, числа 0..255. На таких процессорах работали "
    "Atari 2600, ZX Spectrum и первые ПК.\n"
    "Программа считает числа Фибоначчи: 0,1,1,2,3,5,8,13,21,34,55,89 - "
    "выводит их (OUT) и складывает в ОЗУ по адресам 0..11.\n"
    "Регистров: 4 (R0..R3). ОЗУ: 64 байта. Транзисторов: ~3500 (как у MOS 6502). "
    "Под микроскопом в блоке РЕГИСТРЫ каждый бит - настоящий триггер из 6 транзисторов.",
    cpu_blocks(320, 192, 2048, 940), ISA_TEXT, prog8))

# ---------------------------------------------------------------------------
#  16-bit CPU - sum 1..100 = 5050
# ---------------------------------------------------------------------------
prog16 = assemble("""
        LDI R0, 0        ; сумма = 0
        LDI R1, 1        ; i = 1
        LDI R2, 100      ; предел = 100
loop:   ADD R0, R1       ; сумма += i
        CMP R1, R2       ; i == 100 ?
        JZ done
        INC R1
        JMP loop
done:   OUT R0           ; вывести 5050
        STORE 0, R0      ; сохранить в ПАМЯТЬ[0]
        HALT
""")
add("04_cpu16.bin", chipbin(
    1, 16, 8, 256, 1000, 0, 0, 29000,
    "16-БИТНЫЙ ПРОЦЕССОР «СЛОВО-16»",
    "16 бит = числа 0..65535. Такой процессор уже может посчитать сумму "
    "1+2+...+100 = 5050 в одном регистре (8-битный не смог бы - переполнение после 255!).\n"
    "Программа: цикл ADD/CMP/JZ, результат 5050 уходит в OUT и в ПАМЯТЬ[0].\n"
    "Регистров: 8. ОЗУ: 256 ячеек x 16 бит. Транзисторов: ~29000 (как у Intel 8086). "
    "Частота: 1 МГц.",
    cpu_blocks(900, 768, 16384, 10948), ISA_TEXT, prog16))

# ---------------------------------------------------------------------------
#  RAM chips (program section = initial cell contents)
# ---------------------------------------------------------------------------
add("05_ram16x4.bin", chipbin(
    2, 4, 1, 16, 0, 0, 0, 460,
    "ОЗУ 16 x 4 БИТ «ПАМЯТЬ-16»",
    "Оперативная память: 16 ячеек, в каждой 4 бита.\n"
    "Ячейка SRAM - это 6 транзисторов (два инвертора кольцом + 2 ключа): "
    "она сама держит свой бит, пока есть питание.\n"
    "В симуляции нажимай на ячейки: адресный дешифратор выберет строку и "
    "столбец, а шина покажет чтение/запись.\n"
    "Начальное содержимое: в ячейке N записано число N.",
    [("ДЕШИФРАТОР АДРЕСА", 0, 0, 15, 2, 64, "control", 0),
     ("МАССИВ ЯЧЕЕК SRAM", 0, 3, 11, 8, 384, "ram", 0),
     ("УСИЛИТЕЛИ ЧТЕНИЯ", 12, 3, 3, 8, 48, "logic", 0),
     ("ШИНА ДАННЫХ", 0, 12, 15, 1, 16, "bus", 0)],
    "Это чип памяти, программы у него нет.\nСекция данных .bin = начальное содержимое ячеек.",
    bytes(range(16))))

add("06_sram256.bin", chipbin(
    2, 8, 1, 256, 0, 0, 0, 13500,
    "ОЗУ 256 БАЙТ «SRAM-256»",
    "Настоящий чип статической памяти на 256 байт (2048 бит).\n"
    "2048 ячеек x 6 транзисторов = 12288 транзисторов только на хранение бит, "
    "плюс дешифраторы строк/столбцов и усилители.\n"
    "Такие чипы стояли в первых домашних компьютерах. "
    "Начальный узор в памяти: ячейка N = (N*7) mod 256 - посмотри под микроскопом.",
    [("ДЕШИФРАТОР СТРОК", 0, 0, 15, 2, 512, "control", 0),
     ("МАССИВ ЯЧЕЕК 256x8", 0, 3, 11, 9, 12288, "ram", 0),
     ("ДЕШИФРАТОР СТОЛБЦОВ", 12, 3, 3, 9, 512, "control", 0),
     ("ШИНА ДАННЫХ", 0, 13, 15, 1, 32, "bus", 0)],
    "Это чип памяти, программы у него нет.\nСекция данных .bin = начальное содержимое ячеек.",
    bytes(((i * 7) & 0xFF) for i in range(256))))

# ---------------------------------------------------------------------------
#  GPU 32x16 - animated xor plasma
# ---------------------------------------------------------------------------
prog_gpu = assemble("""
        LDI R3, 32
        STORE 60, R3     ; ширина = 32
        LDI R3, 16
        STORE 61, R3     ; высота = 16
frame:  IN R0            ; t = время
        LDI R2, 0        ; y = 0
yloop:  LDI R1, 0        ; x = 0
xloop:  MOV R3, R1       ; цвет = x
        XOR R3, R2       ; цвет ^= y
        XOR R3, R0       ; цвет ^= t
        OUT R3           ; нарисовать пиксель (курсор идёт сам)
        INC R1
        LOAD R3, 60
        CMP R1, R3       ; x == 32 ?
        JNZ xloop
        INC R2
        LOAD R3, 61
        CMP R2, R3       ; y == 16 ?
        JNZ yloop
        JMP frame
""")
add("07_gpu32.bin", chipbin(
    3, 4, 4, 64, 500, 32, 16, 8200,
    "ВИДЕОКАРТА «ПИКСЕЛЬ-32» 32x16",
    "Графический процессор с экраном 32x16 пикселей, 16 цветов (4 бита на пиксель).\n"
    "Команда OUT рисует следующий пиксель: видеоконтроллер сам двигает курсор "
    "слева направо, сверху вниз - как электронный луч в старом телевизоре.\n"
    "Программа рисует живую «плазму»: цвет = X xor Y xor ВРЕМЯ. "
    "Команда IN читает счётчик времени, поэтому картинка дышит.\n"
    "Видеопамять: 512 пикселей x 4 бита = 2048 бит.",
    cpu_blocks(300, 128, 4096, 3676, with_screen=True), ISA_TEXT, prog_gpu))

# ---------------------------------------------------------------------------
#  Display 16x16 - moving checkerboard
# ---------------------------------------------------------------------------
prog_scr = assemble("""
        LDI R3, 16
        STORE 60, R3     ; размер = 16
frame:  IN R0            ; t
        LDI R2, 0        ; y
yloop:  LDI R1, 0        ; x
xloop:  MOV R3, R1       ; цвет = x + y + t
        ADD R3, R2
        ADD R3, R0
        OUT R3
        INC R1
        LOAD R3, 60
        CMP R1, R3
        JNZ xloop
        INC R2
        LOAD R3, 60
        CMP R2, R3
        JNZ yloop
        JMP frame
""")
add("08_screen16.bin", chipbin(
    4, 4, 4, 64, 250, 16, 16, 4100,
    "МОНИТОР «ЭКРАН-16» 16x16",
    "Чип экрана 16x16 с собственным маленьким контроллером.\n"
    "Каждый пиксель - ячейка видеопамяти на 4 бита (16 цветов) + транзистор, "
    "который включает свой светодиод.\n"
    "Демо-программа: цвет пикселя = X + Y + ВРЕМЯ, получается бегущая "
    "диагональная волна. Под микроскопом блок ЭКРАН показывает сами пиксели-транзисторы.",
    [("КОНТРОЛЛЕР", 0, 0, 7, 3, 800, "control", 0),
     ("АЛУ", 8, 0, 7, 3, 300, "alu", 0),
     ("МАТРИЦА ПИКСЕЛЕЙ", 0, 4, 15, 8, 2560, "screen", 0),
     ("ШИНА ДАННЫХ", 0, 13, 15, 1, 40, "bus", 0)],
    ISA_TEXT, prog_scr))


# ---------------------------------------------------------------------------
#  Expanded library (v1.2): many processors, memories, video chips, circuits
#  and complete devices.  Interactive screen devices read the on-screen
#  gamepad through memory cell 250 (1=left, 2=right, 4=up, 8=down,
#  16=A, 32=B); the game writes that cell, the device program clears it.
# ---------------------------------------------------------------------------
INPUT_ADDR = 250
_label_uid = 0


def _lab(prefix):
    global _label_uid
    _label_uid += 1
    return "%s%d" % (prefix, _label_uid)


def _cap_transistors(v):
    return max(1, min(65535, int(v)))


def _std_blocks(bits, regs, mem, screen=False, names=None):
    blocks = cpu_blocks(
        min(14000, 240 + bits * 95),
        min(14000, regs * bits * 8 + 32),
        min(50000, mem * bits * 6 + 64),
        min(16000, 700 + bits * 170 + regs * 24),
        with_screen=screen)
    if names:
        blocks = [(names[i] if i < len(names) else b[0],) + b[1:]
                  for i, b in enumerate(blocks)]
    return blocks


def _cpu_transistors(bits, regs, mem, screen_pixels=0, extra=0):
    return _cap_transistors(900 + bits * 220 + regs * bits * 8 +
                            mem * bits * 6 + screen_pixels * 4 + extra)


def _add_chip(fname, type_, bits, regs, mem, clock, sw, sh, name, desc,
              blocks, program, transistors=None, isa=ISA_TEXT):
    if transistors is None:
        transistors = _cpu_transistors(bits, regs, mem, sw * sh)
    return add(fname, chipbin(type_, bits, regs, mem, clock, sw, sh,
                              _cap_transistors(transistors), name, desc,
                              blocks, isa, program))


def _memory_blocks(array_name, bits, mem, rom=False):
    array_t = min(60000, mem * bits * 6)
    return [
        ("ДЕШИФРАТОР АДРЕСА", 0, 0, 15, 2, min(6000, 128 + mem * 2), "control", 0),
        (array_name, 0, 3, 11, 9, array_t, "ram", 0),
        ("УСИЛИТЕЛИ И ВЫХОД" if not rom else "ВЫХОДНЫЕ БУФЕРЫ", 12, 3, 3, 9,
         min(4000, 96 + mem), "logic", 0),
        ("ШИНА ДАННЫХ", 0, 13, 15, 1, 32, "bus", 0),
    ]


def _circuit_blocks(core_name, core_t=900):
    return [
        ("ТАКТОВЫЙ ГЕНЕРАТОР", 0, 0, 7, 3, 180, "control", 0),
        (core_name, 8, 0, 7, 3, core_t, "logic", 0),
        ("РЕГИСТР СОСТОЯНИЯ", 0, 4, 7, 5, 256, "regs", 0),
        ("ТАБЛИЦА / БУФЕР", 8, 4, 7, 5, 512, "ram", 0),
        ("ВЫХОДНАЯ ШИНА", 0, 10, 15, 1, 32, "bus", 0),
        ("ВХОДЫ И ВЫХОДЫ", 0, 12, 15, 2, 48, "logic", 0),
    ]


def _device_blocks(bits=8, regs=8, mem=256, pc=False):
    names = (["БИОС / ЧИПСЕТ", "ЦЕНТРАЛЬНЫЙ ПРОЦЕССОР", "КЭШ И РЕГИСТРЫ",
              "ВИДЕОПАМЯТЬ", "ШИНА, КЛАВИАТУРА", "ЗВУК, СЕТЬ, ПИТАНИЕ"] if pc else
             ["ПРОЦЕССОР УСТРОЙСТВА", "АЛУ / ИГРОВАЯ ЛОГИКА", "РЕГИСТРЫ СОСТОЯНИЯ",
              "ЭКРАН И ВИДЕОПАМЯТЬ", "КНОПКИ / ШИНА ВВОДА", "ЗВУК, ПИТАНИЕ, ДАТЧИКИ"])
    return _std_blocks(bits, regs, mem, screen=True, names=names)


# ---- small reusable compute programs ------------------------------------
def _counter_program(last, down=False):
    if down:
        return assemble("""
            LDI R0, %d
            LDI R2, 0
    loop:   OUT R0
            STORER R0, R0
            CMP R0, R2
            JZ done
            DEC R0
            JMP loop
    done:   HALT
    """ % last)
    return assemble("""
        LDI R0, 0
        LDI R2, %d
loop:   OUT R0
        STORER R0, R0
        CMP R0, R2
        JZ done
        INC R0
        JMP loop
done:   HALT
""" % last)


def _triangular_program(last):
    return assemble("""
        LDI R0, 0
        LDI R1, 1
        LDI R2, %d
loop:   ADD R0, R1
        OUT R0
        STORER R1, R0
        CMP R1, R2
        JZ done
        INC R1
        JMP loop
done:   HALT
""" % last)


def _squares_program(last):
    return assemble("""
        LDI R0, 0
        LDI R1, 1
        LDI R2, 1
        LDI R3, %d
loop:   ADD R0, R1
        OUT R0
        STORER R2, R0
        INC R1
        INC R1
        CMP R2, R3
        JZ done
        INC R2
        JMP loop
done:   HALT
""" % last)


def _powers_program():
    return assemble("""
        LDI R0, 1
        LDI R1, 0
loop:   OUT R0
        STORER R1, R0
        INC R1
        SHL R0
        JNZ loop
        HALT
""")


def _fib_program(count):
    return assemble("""
        LDI R0, 0
        LDI R1, 1
        LDI R2, 0
loop:   OUT R0
        STORER R2, R0
        MOV R3, R0
        ADD R3, R1
        MOV R0, R1
        MOV R1, R3
        INC R2
        LDI R3, %d
        CMP R2, R3
        JNZ loop
        HALT
""" % count)


def _multiply_program(a, b):
    return assemble("""
        LDI R0, 0
        LDI R1, %d
        LDI R2, %d
        LDI R3, 0
loop:   CMP R1, R3
        JZ done
        ADD R0, R2
        DEC R1
        JMP loop
done:   OUT R0
        STORE 0, R0
        HALT
""" % (b, a))


def _divide_program(a, b):
    return assemble("""
        LDI R0, %d
        LDI R1, %d
        LDI R2, 0
loop:   CMP R0, R1
        JC done
        SUB R0, R1
        INC R2
        JMP loop
done:   OUT R2
        STORE 0, R2
        STORE 1, R0
        HALT
""" % (a, b))


def _gcd_program(a, b):
    return assemble("""
        LDI R0, %d
        LDI R1, %d
loop:   CMP R0, R1
        JZ done
        JC swap
        SUB R0, R1
        JMP loop
swap:   MOV R2, R0
        MOV R0, R1
        MOV R1, R2
        JMP loop
done:   OUT R0
        STORE 0, R0
        HALT
""" % (a, b))


def _lfsr_program(seed, poly, count):
    return assemble("""
        LDI R0, %d
        LDI R2, %d
        LDI R3, 0
loop:   OUT R0
        STORER R3, R0
        INC R3
        SHL R0
        JC xorit
        JMP check
xorit:  XOR R0, R2
check:  LDI R1, %d
        CMP R3, R1
        JNZ loop
        HALT
""" % (seed, poly, count))


def _gray_program(last):
    return assemble("""
        LDI R0, 0
        LDI R3, %d
loop:   MOV R1, R0
        SHR R1
        MOV R2, R0
        XOR R2, R1
        OUT R2
        STORER R0, R2
        CMP R0, R3
        JZ done
        INC R0
        JMP loop
done:   HALT
""" % last)


def _ring_program(johnson=False):
    feed = """
        SHL R0
        JC check
        INC R0
        JMP check
""" if johnson else """
        SHL R0
        JNZ check
        LDI R0, 1
"""
    return assemble("""
        LDI R0, 1
        LDI R2, 0
loop:   OUT R0
        STORER R2, R0
        INC R2
""" + feed + """
check:  LDI R1, 24
        CMP R2, R1
        JNZ loop
        HALT
""")


def _bcd_program():
    return assemble("""
        LDI R0, 0
        LDI R1, 0
loop:   STORE 0, R0
        STORE 1, R1
        OUT R1
        OUT R0
        INC R0
        LDI R2, 10
        CMP R0, R2
        JNZ loop
        LDI R0, 0
        INC R1
        LDI R2, 10
        CMP R1, R2
        JNZ loop
        HALT
""")


def _stack_program():
    pushes = ""
    for v in (5, 9, 2, 7, 12):
        pushes += "        LDI R0, %d\n        STORER R1, R0\n        INC R1\n" % v
    return assemble("""
        LDI R1, 10
""" + pushes + """
pop:    LDI R2, 10
        CMP R1, R2
        JZ done
        DEC R1
        LOADR R0, R1
        OUT R0
        JMP pop
done:   HALT
""")


def _sort_program():
    values = [5, 1, 4, 1, 3, 5, 2, 4]
    init = ""
    for i, v in enumerate(values):
        init += "        LDI R0, %d\n        STORE %d, R0\n" % (v, i)
    count = ""
    for i in range(len(values)):
        count += ("        LOAD R0, %d\n        LDI R1, 32\n        ADD R1, R0\n"
                  "        LOADR R2, R1\n        INC R2\n        STORER R1, R2\n" % i)
    return assemble(init + count + """
        LDI R3, 16
        LDI R0, 0
sortv:  LDI R1, 32
        ADD R1, R0
        LOADR R2, R1
sortr:  LDI R1, 0
        CMP R2, R1
        JZ nextv
        OUT R0
        STORER R3, R0
        INC R3
        DEC R2
        JMP sortr
nextv:  LDI R1, 5
        CMP R0, R1
        JZ done
        INC R0
        JMP sortv
done:   HALT
""")


def _dsp_program():
    body = "        LDI R2, 0\n        LDI R3, 16\n"
    for v in (4, 12, 20, 28, 20, 12, 6, 14, 22, 30, 18, 8):
        body += ("        LDI R0, %d\n        MOV R1, R0\n        ADD R1, R2\n"
                 "        SHR R1\n        OUT R1\n        STORER R3, R1\n"
                 "        INC R3\n        MOV R2, R0\n" % v)
    return assemble(body + "        HALT\n")


def _perceptron_program():
    return assemble("""
        LDI R0, 9
        SHL R0
        SHL R0
        LDI R1, 7
        SHL R1
        ADD R0, R1
        LDI R1, 5
        ADD R0, R1
        STORE 0, R0
        LDI R1, 40
        CMP R0, R1
        JC zero
        LDI R2, 1
        JMP done
zero:   LDI R2, 0
done:   OUT R0
        OUT R2
        STORE 1, R2
        HALT
""")


# ---- screen program scaffolding ------------------------------------------
def _formula_code(mode):
    formulas = {
        "xor": "MOV R3, R1\nXOR R3, R2\nXOR R3, R0",
        "add": "MOV R3, R1\nADD R3, R2\nADD R3, R0",
        "sub": "MOV R3, R1\nSUB R3, R2\nADD R3, R0",
        "blocks": "MOV R3, R1\nSHR R3\nSHR R3\nMOV R4, R2\nSHR R4\nADD R3, R4\nXOR R3, R0",
        "tunnel": "MOV R3, R1\nXOR R3, R2\nMOV R4, R2\nSHL R4\nXOR R3, R4\nADD R3, R0",
        "noise": "MOV R3, R1\nSHL R3\nXOR R3, R2\nMOV R4, R0\nSHL R4\nXOR R3, R4\nSHR R3",
        "rings": "MOV R3, R1\nXOR R3, R2\nMOV R4, R1\nADD R4, R2\nXOR R3, R4\nADD R3, R0",
        "fire": "MOV R3, R2\nADD R3, R0\nXOR R3, R1\nADD R3, R2",
        "ocean": "MOV R3, R1\nSHR R3\nADD R3, R2\nMOV R4, R0\nSHR R4\nXOR R3, R4",
        "matrix": ("MOV R3, R2\nADD R3, R0\nXOR R3, R1\nMOV R4, R1\n"
                   "LDI R5, 3\nAND R4, R5\nJZ pixok\nLDI R3, 0\npixok: NOP"),
    }
    return formulas[mode]


def _screen_program(w, h, pixel_code, init_code="", update_code=""):
    src = """
        LDI R3, %d
        STORE 60, R3
        LDI R3, %d
        STORE 61, R3
%s
frame:  IN R0
        LOAD R4, 250
        LDI R5, 0
        STORE 250, R5
%s
        LDI R2, 0
yloop:  LDI R1, 0
xloop:
%s
        OUT R3
        INC R1
        LOAD R3, 60
        CMP R1, R3
        JNZ xloop
        INC R2
        LOAD R3, 61
        CMP R2, R3
        JNZ yloop
        JMP frame
""" % (w, h, init_code, update_code, pixel_code)
    return assemble(src)


def _set_color_code(color, color_addr):
    if color_addr is not None:
        return "        LOAD R3, %d\n" % color_addr
    return "        LDI R3, %d\n" % color


def _dyn_rect(x_addr, y_addr, w, h, color=15, color_addr=None):
    no, xok, yok = _lab("rno"), _lab("rx"), _lab("ry")
    return """
        LOAD R4, %d
        MOV R5, R1
        CMP R5, R4
        JC %s
        LDI R5, %d
        ADD R4, R5
        MOV R5, R1
        CMP R5, R4
        JZ %s
        JC %s
        JMP %s
%s:     LOAD R4, %d
        MOV R5, R2
        CMP R5, R4
        JC %s
        LDI R5, %d
        ADD R4, R5
        MOV R5, R2
        CMP R5, R4
        JZ %s
        JC %s
        JMP %s
%s:
%s%s:
""" % (x_addr, no, w, no, xok, no, xok, y_addr, no, h, no, yok, no, yok,
       _set_color_code(color, color_addr), no)


def _dyn_point(x_addr, y_addr, color=15, color_addr=None):
    no = _lab("pno")
    return """
        LOAD R4, %d
        CMP R1, R4
        JNZ %s
        LOAD R4, %d
        CMP R2, R4
        JNZ %s
%s%s:
""" % (x_addr, no, y_addr, no, _set_color_code(color, color_addr), no)


def _static_rect(x, y, w, h, color):
    no, xok, yok = _lab("sno"), _lab("sx"), _lab("sy")
    return """
        LDI R4, %d
        MOV R5, R1
        CMP R5, R4
        JC %s
        LDI R4, %d
        MOV R5, R1
        CMP R5, R4
        JZ %s
        JC %s
        JMP %s
%s:     LDI R4, %d
        MOV R5, R2
        CMP R5, R4
        JC %s
        LDI R4, %d
        MOV R5, R2
        CMP R5, R4
        JZ %s
        JC %s
        JMP %s
%s:     LDI R3, %d
%s:
""" % (x, no, x + w, no, xok, no, xok, y, no, y + h, no, yok, no, yok, color, no)


def _input_move_code():
    # Cursor in cells 32/33, clamps are supplied by each program's own code
    # where the usable rectangle differs. This generic version wraps safely
    # at byte underflow and lets pixel tests ignore out-of-range points.
    return """
        MOV R6, R4
        LDI R7, 1
        AND R6, R7
        JZ cur_r
        LOAD R6, 32
        DEC R6
        STORE 32, R6
cur_r:  MOV R6, R4
        LDI R7, 2
        AND R6, R7
        JZ cur_u
        LOAD R6, 32
        INC R6
        STORE 32, R6
cur_u:  MOV R6, R4
        LDI R7, 4
        AND R6, R7
        JZ cur_d
        LOAD R6, 33
        DEC R6
        STORE 33, R6
cur_d:  MOV R6, R4
        LDI R7, 8
        AND R6, R7
        JZ cur_done
        LOAD R6, 33
        INC R6
        STORE 33, R6
cur_done: NOP
"""


# ---- processors 09..28 ----------------------------------------------------
_processor_specs = [
    ("09_cpu2.bin", 2, 3, 16, _counter_program(3), "2-БИТНЫЙ ПРОЦЕССОР «ДУЭТ-2»",
     "Два бита хранят числа 0..3. Программа считает по кругу и показывает, как после 3 снова получается перенос в следующий разряд, которого внутри регистра уже нет."),
    ("10_cpu3.bin", 3, 3, 16, _triangular_program(4), "3-БИТНЫЙ ПРОЦЕССОР «ТРИО-3»",
     "Числа 0..7 и треугольные суммы 1, 3, 6, 10 с переполнением по модулю 8: отличная демонстрация того, почему разрядность важна."),
    ("11_cpu5.bin", 5, 4, 32, _squares_program(5), "5-БИТНЫЙ ПРОЦЕССОР «КВИНТЕТ-5»",
     "Считает квадраты без умножения: прибавляет нечётные числа 1, 3, 5, 7, 9. В АЛУ видно цепочку из пяти полных сумматоров."),
    ("12_cpu6.bin", 6, 4, 64, _powers_program(), "6-БИТНЫЙ ПРОЦЕССОР «СДВИГ-6»",
     "Выводит степени двойки одним сдвигом SHL за шаг: 1, 2, 4, 8, 16, 32. Следующий сдвиг выбрасывает единицу в флаг C."),
    ("13_cpu7.bin", 7, 4, 64, _fib_program(10), "7-БИТНЫЙ ПРОЦЕССОР «СЕПТЕТ-7»",
     "Числа Фибоначчи в семи битах. Программа успевает дойти до 34, а дальше значения начали бы приближаться к пределу 127."),
    ("14_cpu9.bin", 9, 6, 128, _triangular_program(30), "9-БИТНЫЙ ПРОЦЕССОР «ДЕВЯТКА-9»",
     "Нестандартная разрядность 9 бит: сумма 1..30 равна 465 и уже не помещается в байт, но свободно живёт в этом процессоре."),
    ("15_cpu10.bin", 10, 6, 128, _squares_program(31), "10-БИТНЫЙ ПРОЦЕССОР «ДЕСЯТКА-10»",
     "Считает квадраты до 31²=961 прибавлением нечётных чисел. Регистры и ячейки памяти здесь десятибитные."),
    ("16_cpu11.bin", 11, 6, 256, _counter_program(200), "11-БИТНЫЙ ПРОЦЕССОР «ОДИННАДЦАТЬ»",
     "Счётчик до 200 с записью каждого числа в память. Удобно сравнивать ширину шин и регистров с 8- и 16-битными моделями."),
    ("17_cpu12.bin", 12, 8, 256, _multiply_program(37, 41), "12-БИТНЫЙ ПРОЦЕССОР-УМНОЖИТЕЛЬ",
     "Умножает 37×41 повторным сложением и получает 1517. Под микроскопом в АЛУ видны все промежуточные суммы и переносы."),
    ("18_cpu13.bin", 13, 8, 256, _fib_program(18), "13-БИТНЫЙ ПРОЦЕССОР «ФИБОНАЧЧИ-13»",
     "Восемнадцать чисел Фибоначчи с записью в ОЗУ. Разрядности хватает до значения 2584."),
    ("19_cpu14.bin", 14, 8, 256, _gcd_program(252, 105), "14-БИТНЫЙ ПРОЦЕССОР «ЕВКЛИД-14»",
     "Находит НОД(252,105)=21 древним алгоритмом Евклида: только сравнения, обмены и вычитания."),
    ("20_cpu15.bin", 15, 8, 512, _triangular_program(120), "15-БИТНЫЙ ПРОЦЕССОР «ПОЧТИ-16»",
     "Сумма 1..120 равна 7260. Процессор на один бит уже 16-битного, но для этой задачи запаса всё равно много."),
    ("21_cpu8_mul.bin", 8, 4, 64, _multiply_program(13, 17), "8-БИТНЫЙ ПРОЦЕССОР «УМНОЖИТЕЛЬ-8»",
     "Команды умножения нет, поэтому 13×17 считается семнадцатью сложениями. Ответ 221 лежит в ячейке 0."),
    ("22_cpu16_div.bin", 16, 8, 256, _divide_program(255, 7), "16-БИТНЫЙ ПРОЦЕССОР «ДЕЛИТЕЛЬ-16»",
     "Делит 255 на 7 повторным вычитанием: частное 36 в ячейке 0, остаток 3 в ячейке 1."),
    ("23_cpu8_stack.bin", 8, 4, 64, _stack_program(), "8-БИТНЫЙ СТЕКОВЫЙ ПРОЦЕССОР",
     "Кладёт 5, 9, 2, 7, 12 в стек в памяти, а затем снимает обратно: OUT покажет 12, 7, 2, 9, 5. Так работают вызовы подпрограмм."),
    ("24_cpu8_bcd.bin", 8, 4, 64, _bcd_program(), "8-БИТНЫЙ BCD-ПРОЦЕССОР ЧАСОВ",
     "Считает десятки и единицы отдельно, как электронные часы и старые калькуляторы. Ячейка 0 — единицы, ячейка 1 — десятки."),
    ("25_cpu8_sort.bin", 8, 4, 64, _sort_program(), "8-БИТНЫЙ ПРОЦЕССОР-СОРТИРОВЩИК",
     "Сортирует массив подсчётом частот. Исходные числа лежат по адресам 0..7, отсортированные — с адреса 16."),
    ("26_cpu8_rng.bin", 8, 4, 64, _lfsr_program(0x5A, 0x1D, 32), "8-БИТНЫЙ ГЕНЕРАТОР ПСЕВДОСЛУЧАЙНОСТИ",
     "LFSR: регистр сдвигается, а выпавший бит через XOR меняет состояние. Получается длинная псевдослучайная последовательность без настоящего случайного источника."),
    ("27_dsp_filter.bin", 8, 4, 64, _dsp_program(), "DSP-ПРОЦЕССОР «ФИЛЬТР-8»",
     "Цифровой фильтр скользящего среднего: каждый выход равен (текущий отсчёт + предыдущий)/2. Такие процессоры обрабатывают звук и сигналы датчиков."),
    ("28_vector_ai.bin", 16, 8, 256, _perceptron_program(), "16-БИТНЫЙ НЕЙРОУСКОРИТЕЛЬ «ПЕРЦЕПТРОН»",
     "Один искусственный нейрон: 9×4 + 7×2 + 5×1 = 55. Сумма больше порога 40, поэтому класс на выходе равен 1."),
]
for _fname, _bits, _regs, _mem, _prog, _name, _desc in _processor_specs:
    _add_chip(_fname, 1, _bits, _regs, _mem, min(65000, 20 + _bits * _bits * 12),
              0, 0, _name,
              _desc + "\nРегистров: %d. Память: %d ячеек. Нажми «ПРОСМОТР ПРОЦЕССОРА», чтобы увидеть кристалл и живые сигналы." % (_regs, _mem),
              _std_blocks(_bits, _regs, _mem), _prog)


# ---- memory and ROM 29..36 -----------------------------------------------
_segment_codes = [0x7E, 0x30, 0x6D, 0x79, 0x33, 0x5B, 0x5F, 0x70, 0x7F, 0x7B]
_memory_specs = [
    ("29_cache64.bin", 2, 8, 64, "КЭШ-ПАМЯТЬ SRAM 64×8",
     "Быстрый кэш рядом с процессором. Узор ячеек: адрес XOR сдвинутый адрес.", lambda i: i ^ (i << 1)),
    ("30_dram512.bin", 2, 8, 512, "ДИНАМИЧЕСКАЯ ПАМЯТЬ DRAM 512×8",
     "Учебная модель DRAM: 512 байт с детерминированным псевдослучайным узором линейного генератора.", lambda i: (i * 73 + 19) ^ (i >> 2)),
    ("31_vram256.bin", 2, 4, 256, "ВИДЕОПАМЯТЬ VRAM 256×4",
     "Каждый ниббл — цвет пикселя. В ячейках записан шахматный и диагональный узор кадра.", lambda i: ((i & 15) ^ (i >> 4)) & 15),
    ("32_eeprom128.bin", 5, 8, 128, "ПЗУ EEPROM 128 БАЙТ",
     "Энергонезависимая память с повторяющейся текстовой подписью CHIPLAB в кодах символов.", lambda i: b"CHIPLAB-EEPROM-2026 "[i % 20]),
    ("33_flash1024.bin", 5, 8, 1024, "ФЛЕШ-ПАМЯТЬ 1024×8",
     "Килобайт флеш-памяти с таблицей синуса: значения плавно поднимаются и опускаются волной.", lambda i: int((math.sin(i / 40.0) + 1.0) * 127.0)),
    ("34_rom_font.bin", 5, 8, 256, "ПЗУ ЗНАКОГЕНЕРАТОРА И СЕГМЕНТОВ",
     "Коды семисегментных цифр повторяются по всей памяти: так ПЗУ превращает число в включённые сегменты индикатора.", lambda i: _segment_codes[i % len(_segment_codes)]),
    ("35_rom_sine16.bin", 5, 16, 256, "ПЗУ ТАБЛИЦЫ СИНУСА 256×16",
     "Таблица для синтеза звука и графики. Начальные байты задают младшую часть каждой 16-битной ячейки.", lambda i: int((math.sin(i * math.pi / 128.0) + 1.0) * 127.0)),
    ("36_fifo64.bin", 2, 16, 64, "РЕГИСТРОВЫЙ ФАЙЛ / FIFO 64×16",
     "Очередь из 64 широких ячеек. Внутри записан обратный счёт, удобный для изучения указателей чтения и записи.", lambda i: 255 - i),
]
for _fname, _type, _bits, _mem, _name, _desc, _pat in _memory_specs:
    _data = bytes((_pat(i) & 0xFF) for i in range(_mem))
    _add_chip(_fname, _type, _bits, 1, _mem, 0, 0, 0, _name,
              _desc + "\nОткрой вкладку РАБОТА, чтобы нажимать на ячейки, переключать биты и видеть дешифратор адреса. Кнопка просмотра покажет массив ячеек под микроскопом.",
              _memory_blocks("МАССИВ ЯЧЕЕК", _bits, _mem, rom=(_type == 5)), _data,
              transistors=min(65535, 700 + _bits * _mem * 6),
              isa="Это чип памяти, программы у него нет.\nСекция данных .bin = начальное содержимое ячеек.")


# ---- GPUs and displays 37..44 ---------------------------------------------
_video_specs = [
    ("37_gpu64x32.bin", 3, 4, 64, 32, "blocks", "ВИДЕОКАРТА «МЕГАПИКСЕЛЬ» 64×32",
     "Большой экран 64×32 и крупные бегущие цветные кварталы: координаты делятся сдвигами на блоки."),
    ("38_gpu64x64.bin", 3, 8, 64, 64, "xor", "ВИДЕОКАРТА «КВАДРАТ-64» 64×64",
     "Максимальный квадратный кадр ChipLab: XOR-плазма на 4096 пикселей."),
    ("39_oled32.bin", 4, 4, 32, 32, "rings", "OLED-ЭКРАН «КОЛЬЦА» 32×32",
     "Каждый пиксель светится сам. Формула координат рисует расширяющиеся ромбы и кольца."),
    ("40_eink32.bin", 4, 2, 32, 16, "blocks", "E-INK ЭКРАН 32×16",
     "Всего два бита на пиксель — четыре оттенка. Медленная спокойная картинка электронной бумаги."),
    ("41_vector48.bin", 3, 4, 48, 32, "tunnel", "ВЕКТОРНЫЙ ДИСПЛЕЙ «ТУННЕЛЬ» 48×32",
     "Координатная формула создаёт псевдотрёхмерный туннель, как в старых векторных аркадах."),
    ("42_led_matrix.bin", 4, 4, 16, 8, "matrix", "СВЕТОДИОДНАЯ МАТРИЦА «ДОЖДЬ» 16×8",
     "Столбцы падают вниз в стиле цифрового дождя. Каждый светодиод управляется отдельной ячейкой."),
    ("43_crt40.bin", 4, 4, 40, 24, "ocean", "МОНИТОР ЭЛТ «ВОЛНА» 40×24",
     "Электронный луч рисует морскую волну построчно. Под микроскопом виден курсор развёртки."),
    ("44_holo48.bin", 3, 8, 48, 48, "fire", "ГОЛОГРАФИЧЕСКИЙ ЭКРАН 48×48",
     "Огненная XOR-текстура на большом кадре: цвет зависит от X, Y и времени."),
]
for _fname, _type, _bits, _w, _h, _mode, _name, _desc in _video_specs:
    _prog = _screen_program(_w, _h, _formula_code(_mode))
    _add_chip(_fname, _type, _bits, 8, 256, 800 + _w * _h // 4, _w, _h, _name,
              _desc + "\nOUT рисует пиксели по порядку, а IN даёт время анимации. Кнопка «ПРОСМОТР ПРОЦЕССОРА» покажет видеоконтроллер, видеопамять и транзисторы пикселей.",
              _std_blocks(_bits, 8, 256, screen=True), _prog)


# ---- discrete circuits 45..56 ---------------------------------------------
def _full_adder_program():
    return assemble("""
        LDI R0, 0
loop:   MOV R1, R0
        LDI R2, 0
        LDI R3, 3
        LDI R4, 0
pc:     SHR R1
        JC add1
        JMP test
add1:   INC R2
test:   DEC R3
        CMP R3, R4
        JNZ pc
        OUT R2
        LDI R1, 16
        ADD R1, R0
        STORER R1, R2
        LDI R1, 7
        CMP R0, R1
        JZ done
        INC R0
        JMP loop
done:   HALT
""")


def _gate_program(op):
    return assemble("""
        LDI R0, 0
        LDI R1, 10
loop:   MOV R2, R0
        %s R2, R1
        OUT R2
        STORER R0, R2
        LDI R3, 15
        CMP R0, R3
        JZ done
        INC R0
        JMP loop
done:   HALT
""" % op)


def _mux_program():
    return assemble("""
        LDI R0, 0
        LDI R3, 0
loop:   LDI R1, 5
        LDI R2, 10
        LDI R4, 0
        CMP R0, R4
        JZ outa
        OUT R2
        JMP next
outa:   OUT R1
next:   INC R0
        LDI R4, 2
        CMP R0, R4
        JNZ count
        LDI R0, 0
count:  INC R3
        LDI R4, 16
        CMP R3, R4
        JNZ loop
        HALT
""")


def _comparator_program():
    return assemble("""
        LDI R0, 0
        LDI R1, 7
loop:   CMP R0, R1
        JZ eq
        JC less
        LDI R3, 4
        JMP emit
less:   LDI R3, 1
        JMP emit
eq:     LDI R3, 2
emit:   OUT R3
        STORER R0, R3
        LDI R2, 15
        CMP R0, R2
        JZ done
        INC R0
        JMP loop
done:   HALT
""")


def _barrel_program():
    return assemble("""
        LDI R0, 0
        STORE 0, R0
outer:  LOAD R1, 0
        LDI R2, 3
        MOV R3, R1
inner:  LDI R4, 0
        CMP R3, R4
        JZ emit
        SHL R2
        DEC R3
        JMP inner
emit:   OUT R2
        LOAD R1, 0
        LDI R4, 16
        ADD R4, R1
        STORER R4, R2
        INC R1
        STORE 0, R1
        LDI R4, 8
        CMP R1, R4
        JNZ outer
        HALT
""")


def _pwm_program(duty):
    return assemble("""
        LDI R0, 0
        LDI R2, %d
        LDI R4, 0
loop:   CMP R0, R2
        JC on
        LDI R3, 0
        JMP emit
on:     LDI R3, 1
emit:   OUT R3
        STORER R0, R3
        INC R0
        LDI R1, 16
        CMP R0, R1
        JNZ count
        LDI R0, 0
        INC R4
count:  LDI R1, 4
        CMP R4, R1
        JNZ loop
        HALT
""" % duty)


def _uart_program(value):
    return assemble("""
        LDI R3, 0
        OUT R3
        LDI R0, %d
        LDI R2, 0
loop:   MOV R1, R0
        LDI R3, 1
        AND R1, R3
        OUT R1
        SHR R0
        INC R2
        LDI R3, 8
        CMP R2, R3
        JNZ loop
        LDI R3, 1
        OUT R3
        HALT
""" % value)


_circuit_specs = [
    ("45_circuit_adder.bin", 4, _full_adder_program(), "СХЕМА: ПОЛНЫЙ СУММАТОР",
     "Перебирает все 8 комбинаций A, B и Cin. Выход 0..3 сразу показывает сумму и перенос: 2 означает S=0,C=1; 3 означает S=1,C=1."),
    ("46_circuit_alu.bin", 4, _gate_program("XOR"), "СХЕМА: ЛОГИЧЕСКИЙ БЛОК АЛУ XOR",
     "Для каждого A от 0 до 15 вычисляет A XOR 1010. Таблица результатов записывается в память по адресу входа."),
    ("47_circuit_mux.bin", 4, _mux_program(), "СХЕМА: МУЛЬТИПЛЕКСОР 2→1",
     "Селектор по очереди выбирает вход 5 или вход 10. Так одна шина обслуживает много источников сигнала."),
    ("48_circuit_decoder.bin", 8, _ring_program(False), "СХЕМА: ДЕШИФРАТОР 3→8",
     "На выходе бежит одна единица: 1, 2, 4, 8, 16, 32, 64, 128. Именно такой one-hot сигнал выбирает строку памяти."),
    ("49_circuit_encoder.bin", 8, _powers_program(), "СХЕМА: ПРИОРИТЕТНЫЙ ШИФРАТОР",
     "Показывает входные one-hot коды степеней двойки; номер цикла в памяти соответствует двоичному номеру активной линии."),
    ("50_circuit_comparator.bin", 4, _comparator_program(), "СХЕМА: ЦИФРОВОЙ КОМПАРАТОР",
     "Сравнивает A с числом 7: выход 1 если меньше, 2 если равно, 4 если больше. Флаги Z и C видны в симуляции."),
    ("51_circuit_barrel.bin", 8, _barrel_program(), "СХЕМА: БАРЕЛЬНЫЙ СДВИГАТЕЛЬ",
     "Сдвигает число 3 влево на 0..7 позиций. В настоящем железе это делается сеткой мультиплексоров за один такт."),
    ("52_circuit_ring.bin", 8, _ring_program(False), "СХЕМА: КОЛЬЦЕВОЙ СЧЁТЧИК",
     "Один горячий бит циркулирует по регистру. Такие счётчики задают фазы тактов и сканирование клавиатур."),
    ("53_circuit_johnson.bin", 8, _ring_program(True), "СХЕМА: СЧЁТЧИК ДЖОНСОНА",
     "Скрученное кольцо: инвертированный старший бит подаётся на вход, поэтому последовательность вдвое длиннее обычного кольца."),
    ("54_circuit_gray.bin", 8, _gray_program(31), "СХЕМА: ГЕНЕРАТОР КОДА ГРЕЯ",
     "Соседние значения отличаются ровно одним битом. Код Грея используют в энкодерах положения, чтобы не было ложных промежуточных чисел."),
    ("55_circuit_pwm.bin", 4, _pwm_program(5), "СХЕМА: ШИМ-КОНТРОЛЛЕР PWM",
     "Выход включён 5 тактов из 16. Изменяя скважность, управляют яркостью светодиода и скоростью мотора."),
    ("56_circuit_uart.bin", 8, _uart_program(0xA5), "СХЕМА: ПЕРЕДАТЧИК UART",
     "Посылает стартовый 0, восемь бит числа A5 младшим битом вперёд и стоповую 1. Журнал OUT — временной график последовательной линии."),
]
for _fname, _bits, _prog, _name, _desc in _circuit_specs:
    _add_chip(_fname, 1, _bits, 8, 64, 100, 0, 0, _name,
              _desc + "\nЭто отдельная учебная схема, но внутри неё тоже есть тактовый генератор, состояние и выходная шина — нажми «ПРОСМОТР ПРОЦЕССОРА», чтобы рассмотреть вентили под микроскопом.",
              _circuit_blocks("ЛОГИЧЕСКИЕ ВЕНТИЛИ"), _prog,
              transistors=700 + _bits * 120)

# ---- interactive devices and complete systems 57..80 --------------------
def _init_state(pairs):
    code = ""
    for addr, value in pairs:
        code += "        LDI R3, %d\n        STORE %d, R3\n" % (value, addr)
    return code


def _move_axis(mask_dec, mask_inc, addr, minv, maxv):
    a, b, done = _lab("mdec"), _lab("minc"), _lab("mdone")
    return """
        MOV R6, R4
        LDI R7, %d
        AND R6, R7
        JZ %s
        LOAD R6, %d
        LDI R7, %d
        CMP R6, R7
        JZ %s
        DEC R6
        STORE %d, R6
%s:     MOV R6, R4
        LDI R7, %d
        AND R6, R7
        JZ %s
        LOAD R6, %d
        LDI R7, %d
        CMP R6, R7
        JZ %s
        INC R6
        STORE %d, R6
%s:     NOP
""" % (mask_dec, a, addr, minv, a, addr, a, mask_inc, done, addr, maxv, done, addr, done)


def _press(mask, body):
    done = _lab("press")
    return """
        MOV R6, R4
        LDI R7, %d
        AND R6, R7
        JZ %s
%s
%s:     NOP
""" % (mask, done, body, done)


def _tetris_program():
    update = _move_axis(1, 2, 32, 0, 14)
    update += _press(8, """
        LOAD R6, 33
        INC R6
        INC R6
        STORE 33, R6
""")
    update += _press(16, """
        LOAD R6, 34
        INC R6
        LDI R7, 16
        CMP R6, R7
        JNZ tet_col_ok
        LDI R6, 1
tet_col_ok: STORE 34, R6
""")
    update += """
        LOAD R6, 33
        INC R6
        STORE 33, R6
        LDI R5, 96
        LOAD R6, 32
        ADD R5, R6
        LOADR R6, R5
        INC R5
        LOADR R7, R5
        CMP R6, R7
        JC tet_max2
        JMP tet_have_max
tet_max2: MOV R6, R7
tet_have_max: STORE 37, R6
        LDI R5, 22
        SUB R5, R6
        LOAD R7, 33
        CMP R7, R5
        JC tet_no_land
        LDI R5, 96
        LOAD R6, 32
        ADD R5, R6
        LOADR R6, R5
        INC R6
        INC R6
        STORER R5, R6
        INC R5
        LOADR R6, R5
        INC R6
        INC R6
        STORER R5, R6
        LOAD R6, 35
        INC R6
        STORE 35, R6
        LOAD R6, 37
        INC R6
        INC R6
        LDI R7, 22
        CMP R6, R7
        JC tet_spawn
        LDI R5, 96
        LDI R6, 0
        LDI R7, 112
tet_clear: STORER R5, R6
        INC R5
        CMP R5, R7
        JNZ tet_clear
        STORE 35, R6
tet_spawn: LDI R6, 0
        STORE 33, R6
        MOV R6, R0
        LOAD R7, 35
        ADD R6, R7
        LDI R7, 14
        AND R6, R7
        STORE 32, R6
        LOAD R6, 35
        LDI R7, 2
        ADD R6, R7
        LDI R7, 16
        CMP R6, R7
        JC tet_new_col
        LDI R6, 2
tet_new_col: STORE 34, R6
tet_no_land: NOP
"""
    pixel = """
        LDI R3, 0
        LDI R4, 96
        ADD R4, R1
        LOADR R5, R4
        LDI R6, 24
        SUB R6, R5
        MOV R7, R2
        CMP R7, R6
        JC tet_no_stack
        MOV R3, R1
        ADD R3, R2
        LDI R4, 7
        AND R3, R4
        LDI R4, 2
        ADD R3, R4
tet_no_stack: NOP
""" + _dyn_rect(32, 33, 2, 2, color_addr=34)
    return _screen_program(16, 24,
                             init_code=_init_state([(32, 7), (33, 0), (34, 9), (35, 0)]),
                             update_code=update, pixel_code=pixel)


def _tamagotchi_program():
    update = _move_axis(1, 2, 32, 0, 10)
    update += _press(16, """
        LOAD R6, 34
        INC R6
        INC R6
        INC R6
        INC R6
        LDI R7, 15
        CMP R6, R7
        JZ tama_h_ok
        JC tama_h_ok
        LDI R6, 15
tama_h_ok: STORE 34, R6
        LOAD R6, 33
        LDI R7, 15
        CMP R6, R7
        JZ tama_feed_done
        INC R6
        STORE 33, R6
tama_feed_done: NOP
""")
    update += _press(32, """
        LOAD R6, 33
        INC R6
        INC R6
        INC R6
        INC R6
        LDI R7, 15
        CMP R6, R7
        JZ tama_m_ok
        JC tama_m_ok
        LDI R6, 15
tama_m_ok: STORE 33, R6
        LOAD R6, 34
        LDI R7, 0
        CMP R6, R7
        JZ tama_play_done
        DEC R6
        STORE 34, R6
tama_play_done: NOP
""")
    update += """
        LOAD R6, 35
        INC R6
        STORE 35, R6
        LDI R7, 4
        CMP R6, R7
        JNZ tama_no_auto
        LDI R6, 0
        STORE 35, R6
        LOAD R6, 36
        LDI R7, 1
        CMP R6, R7
        JZ tama_go_right
        LOAD R6, 32
        LDI R7, 0
        CMP R6, R7
        JZ tama_turn_right
        DEC R6
        STORE 32, R6
        JMP tama_no_auto
tama_turn_right: LDI R6, 1
        STORE 36, R6
        LDI R6, 1
        STORE 32, R6
        JMP tama_no_auto
tama_go_right: LOAD R6, 32
        LDI R7, 10
        CMP R6, R7
        JZ tama_turn_left
        INC R6
        STORE 32, R6
        JMP tama_no_auto
tama_turn_left: LDI R6, 0
        STORE 36, R6
        LDI R6, 9
        STORE 32, R6
tama_no_auto: LOAD R6, 37
        INC R6
        STORE 37, R6
        LDI R7, 16
        CMP R6, R7
        JNZ tama_no_decay
        LDI R6, 0
        STORE 37, R6
        LOAD R6, 34
        LDI R7, 0
        CMP R6, R7
        JZ tama_decay_mood
        DEC R6
        STORE 34, R6
tama_decay_mood: LOAD R6, 33
        LDI R7, 0
        CMP R6, R7
        JZ tama_no_decay
        DEC R6
        STORE 33, R6
tama_no_decay: LOAD R6, 32
        MOV R7, R6
        INC R7
        STORE 40, R7
        MOV R7, R6
        INC R7
        INC R7
        INC R7
        INC R7
        STORE 41, R7
        MOV R7, R6
        INC R7
        INC R7
        STORE 42, R7
        LOAD R6, 33
        LDI R7, 4
        CMP R6, R7
        JC tama_sad
        LDI R6, 10
        JMP tama_color
tama_sad: LDI R6, 3
tama_color: STORE 43, R6
        LOAD R6, 34
        LDI R7, 3
        CMP R6, R7
        JC tama_hungry
        LDI R6, 15
        JMP tama_mouth
tama_hungry: LDI R6, 3
tama_mouth: STORE 45, R6
"""
    pixel = """
        MOV R3, R1
        XOR R3, R2
        LDI R4, 1
        AND R3, R4
        JZ tama_bg_a
        LDI R3, 12
        JMP tama_bg_done
tama_bg_a: LDI R3, 5
tama_bg_done: LDI R4, 12
        MOV R5, R2
        CMP R5, R4
        JC tama_no_floor
        LDI R3, 8
tama_no_floor: LDI R4, 0
        CMP R2, R4
        JNZ tama_no_hbar
        LOAD R4, 34
        MOV R5, R1
        CMP R5, R4
        JNC tama_no_hbar
        LDI R3, 3
tama_no_hbar: LDI R4, 1
        CMP R2, R4
        JNZ tama_no_mbar
        LOAD R4, 33
        MOV R5, R1
        CMP R5, R4
        JNC tama_no_mbar
        LDI R3, 11
tama_no_mbar: NOP
"""
    # The assembler has no JNC mnemonic; rewrite those two tests in JC form
    # by swapping the operands (state < x means the bar has ended).
    pixel = pixel.replace("MOV R5, R1\n        CMP R5, R4\n        JNC tama_no_hbar",
                          "MOV R5, R4\n        MOV R4, R1\n        CMP R5, R4\n        JZ tama_no_hbar\n        JC tama_no_hbar")
    pixel = pixel.replace("MOV R5, R1\n        CMP R5, R4\n        JNC tama_no_mbar",
                          "MOV R5, R4\n        MOV R4, R1\n        CMP R5, R4\n        JZ tama_no_mbar\n        JC tama_no_mbar")
    pixel += _dyn_rect(32, 38, 6, 6, color_addr=43)
    pixel += _dyn_point(40, 39, color=0)
    pixel += _dyn_point(41, 39, color=0)
    pixel += _dyn_rect(42, 44, 2, 1, color_addr=45)
    init = _init_state([(32, 5), (33, 10), (34, 10), (35, 0), (36, 1), (37, 0),
                        (38, 6), (39, 8), (40, 6), (41, 9), (42, 7), (43, 10),
                        (44, 10), (45, 15)])
    return _screen_program(16, 16, init_code=init, update_code=update, pixel_code=pixel)


def _snake_program():
    update = """
        MOV R6, R4
        LDI R7, 0
        CMP R6, R7
        JZ snake_dir_done
        STORE 34, R4
snake_dir_done: LOAD R6, 40
        STORE 42, R6
        LOAD R6, 41
        STORE 43, R6
        LOAD R6, 38
        STORE 40, R6
        LOAD R6, 39
        STORE 41, R6
        LOAD R6, 32
        STORE 38, R6
        LOAD R6, 33
        STORE 39, R6
        LOAD R6, 34
        LDI R7, 1
        CMP R6, R7
        JZ snake_left
        LDI R7, 2
        CMP R6, R7
        JZ snake_right
        LDI R7, 4
        CMP R6, R7
        JZ snake_up
        LDI R7, 8
        CMP R6, R7
        JZ snake_down
        JMP snake_food
snake_left: LOAD R6, 32
        DEC R6
        LDI R7, 255
        CMP R6, R7
        JNZ snake_l_ok
        LDI R6, 15
snake_l_ok: STORE 32, R6
        JMP snake_food
snake_right: LOAD R6, 32
        INC R6
        LDI R7, 16
        CMP R6, R7
        JNZ snake_r_ok
        LDI R6, 0
snake_r_ok: STORE 32, R6
        JMP snake_food
snake_up: LOAD R6, 33
        DEC R6
        LDI R7, 255
        CMP R6, R7
        JNZ snake_u_ok
        LDI R6, 15
snake_u_ok: STORE 33, R6
        JMP snake_food
snake_down: LOAD R6, 33
        INC R6
        LDI R7, 16
        CMP R6, R7
        JNZ snake_d_ok
        LDI R6, 0
snake_d_ok: STORE 33, R6
snake_food: LOAD R6, 32
        LOAD R7, 35
        CMP R6, R7
        JNZ snake_no_eat
        LOAD R6, 33
        LOAD R7, 36
        CMP R6, R7
        JNZ snake_no_eat
        LOAD R6, 37
        INC R6
        STORE 37, R6
        MOV R6, R0
        LOAD R7, 37
        ADD R6, R7
        LDI R7, 15
        AND R6, R7
        STORE 35, R6
        MOV R6, R0
        SHR R6
        SHR R6
        SHR R6
        LOAD R7, 37
        ADD R6, R7
        LDI R7, 15
        AND R6, R7
        STORE 36, R6
snake_no_eat: NOP
"""
    pixel = """
        MOV R3, R1
        XOR R3, R2
        LDI R4, 1
        AND R3, R4
        JZ snake_bg
        LDI R3, 5
        JMP snake_draw
snake_bg: LDI R3, 0
snake_draw: NOP
"""
    pixel += _dyn_point(35, 36, color=3)
    pixel += _dyn_point(42, 43, color=10)
    pixel += _dyn_point(40, 41, color=10)
    pixel += _dyn_point(38, 39, color=10)
    pixel += _dyn_point(32, 33, color=14)
    init = _init_state([(32, 8), (33, 8), (34, 2), (35, 3), (36, 4), (37, 0),
                        (38, 7), (39, 8), (40, 6), (41, 8), (42, 5), (43, 8)])
    return _screen_program(16, 16, init_code=init, update_code=update, pixel_code=pixel)


def _pong_program():
    update = _move_axis(4, 8, 36, 0, 11)
    update += """
        LOAD R6, 37
        INC R6
        INC R6
        LOAD R7, 33
        CMP R6, R7
        JZ pong_ai_done
        JC pong_ai_down
        LOAD R6, 37
        LDI R7, 0
        CMP R6, R7
        JZ pong_ai_done
        DEC R6
        STORE 37, R6
        JMP pong_ai_done
pong_ai_down: LOAD R6, 37
        LDI R7, 11
        CMP R6, R7
        JZ pong_ai_done
        INC R6
        STORE 37, R6
pong_ai_done: LOAD R6, 34
        LDI R7, 1
        CMP R6, R7
        JZ pong_right
        LOAD R6, 32
        DEC R6
        STORE 32, R6
        JMP pong_y
pong_right: LOAD R6, 32
        INC R6
        STORE 32, R6
pong_y: LOAD R6, 35
        LDI R7, 1
        CMP R6, R7
        JZ pong_down
        LOAD R6, 33
        DEC R6
        STORE 33, R6
        JMP pong_bounce_y
pong_down: LOAD R6, 33
        INC R6
        STORE 33, R6
pong_bounce_y: LOAD R6, 33
        LDI R7, 0
        CMP R6, R7
        JNZ pong_not_top
        LDI R6, 1
        STORE 35, R6
pong_not_top: LOAD R6, 33
        LDI R7, 15
        CMP R6, R7
        JNZ pong_paddles
        LDI R6, 0
        STORE 35, R6
pong_paddles: LOAD R6, 32
        LDI R7, 1
        CMP R6, R7
        JNZ pong_check_r
        LOAD R6, 33
        LOAD R7, 36
        CMP R6, R7
        JC pong_check_r
        LOAD R6, 36
        INC R6
        INC R6
        INC R6
        INC R6
        INC R6
        LOAD R7, 33
        CMP R7, R6
        JZ pong_check_r
        JC pong_hit_l
        JMP pong_check_r
pong_hit_l: LDI R6, 1
        STORE 34, R6
pong_check_r: LOAD R6, 32
        LDI R7, 30
        CMP R6, R7
        JNZ pong_goals
        LOAD R6, 33
        LOAD R7, 37
        CMP R6, R7
        JC pong_goals
        LOAD R6, 37
        INC R6
        INC R6
        INC R6
        INC R6
        INC R6
        LOAD R7, 33
        CMP R7, R6
        JZ pong_goals
        JC pong_hit_r
        JMP pong_goals
pong_hit_r: LDI R6, 0
        STORE 34, R6
pong_goals: LOAD R6, 32
        LDI R7, 0
        CMP R6, R7
        JNZ pong_goal_r
        LOAD R6, 39
        INC R6
        STORE 39, R6
        LDI R6, 16
        STORE 32, R6
        LDI R6, 8
        STORE 33, R6
        LDI R6, 1
        STORE 34, R6
pong_goal_r: LOAD R6, 32
        LDI R7, 31
        CMP R6, R7
        JNZ pong_done
        LOAD R6, 38
        INC R6
        STORE 38, R6
        LDI R6, 16
        STORE 32, R6
        LDI R6, 8
        STORE 33, R6
        LDI R6, 0
        STORE 34, R6
pong_done: NOP
"""
    pixel = "        LDI R3, 0\n"
    pixel += _static_rect(15, 0, 1, 16, 5)
    pixel += _dyn_rect(41, 36, 1, 5, color=11)
    pixel += _dyn_rect(42, 37, 1, 5, color=3)
    pixel += _dyn_point(32, 33, color=15)
    init = _init_state([(32, 16), (33, 8), (34, 1), (35, 1), (36, 6), (37, 6),
                        (38, 0), (39, 0), (41, 0), (42, 31)])
    return _screen_program(32, 16, init_code=init, update_code=update, pixel_code=pixel)


def _flappy_program():
    update = _press(16, """
        LOAD R6, 32
        LDI R7, 3
        CMP R6, R7
        JC flap_top
        DEC R6
        DEC R6
        DEC R6
        JMP flap_store
flap_top: LDI R6, 0
flap_store: STORE 32, R6
""")
    update += """
        LOAD R6, 32
        LDI R7, 15
        CMP R6, R7
        JZ flap_pipe
        INC R6
        STORE 32, R6
flap_pipe: LOAD R6, 33
        LDI R7, 0
        CMP R6, R7
        JZ flap_respawn
        DEC R6
        STORE 33, R6
        JMP flap_pre
flap_respawn: LDI R6, 15
        STORE 33, R6
        LOAD R6, 35
        INC R6
        STORE 35, R6
        MOV R6, R0
        LOAD R7, 35
        ADD R6, R7
        LDI R7, 7
        AND R6, R7
        INC R6
        INC R6
        INC R6
        STORE 34, R6
flap_pre: LOAD R6, 33
        INC R6
        STORE 39, R6
        LOAD R6, 34
        INC R6
        INC R6
        INC R6
        INC R6
        STORE 40, R6
        LOAD R6, 33
        LDI R7, 4
        CMP R6, R7
        JZ flap_hit_check
        LDI R7, 5
        CMP R6, R7
        JNZ flap_done
flap_hit_check: LOAD R6, 32
        LOAD R7, 34
        CMP R6, R7
        JC flap_reset
        LOAD R6, 32
        LOAD R7, 40
        CMP R6, R7
        JZ flap_reset
        JC flap_done
flap_reset: LDI R6, 8
        STORE 32, R6
        LDI R6, 0
        STORE 35, R6
        LDI R6, 12
        STORE 33, R6
flap_done: NOP
"""
    pixel = """
        LDI R4, 12
        MOV R5, R2
        CMP R5, R4
        JC flap_sky
        LDI R3, 8
        JMP flap_px
flap_sky: LDI R3, 6
flap_px: LOAD R4, 33
        CMP R1, R4
        JZ flap_pipe_x
        LOAD R4, 39
        CMP R1, R4
        JNZ flap_no_pipe
flap_pipe_x: LOAD R4, 34
        MOV R5, R2
        CMP R5, R4
        JC flap_pipe_col
        LOAD R4, 40
        MOV R5, R2
        CMP R5, R4
        JZ flap_pipe_col
        JC flap_no_pipe
flap_pipe_col: LDI R3, 10
flap_no_pipe: NOP
"""
    pixel += _dyn_rect(37, 32, 2, 1, color=14)
    init = _init_state([(32, 8), (33, 12), (34, 6), (35, 0), (37, 4), (39, 13), (40, 10)])
    return _screen_program(16, 16, init_code=init, update_code=update, pixel_code=pixel)


def _breakout_program():
    update = _move_axis(1, 2, 32, 0, 26)
    update += """
        LOAD R6, 35
        LDI R7, 1
        CMP R6, R7
        JZ br_right
        LOAD R6, 33
        DEC R6
        STORE 33, R6
        JMP br_y
br_right: LOAD R6, 33
        INC R6
        STORE 33, R6
br_y:   LOAD R6, 36
        LDI R7, 1
        CMP R6, R7
        JZ br_down
        LOAD R6, 34
        DEC R6
        STORE 34, R6
        JMP br_walls
br_down: LOAD R6, 34
        INC R6
        STORE 34, R6
br_walls: LOAD R6, 33
        LDI R7, 0
        CMP R6, R7
        JNZ br_not_l
        LDI R6, 1
        STORE 35, R6
br_not_l: LOAD R6, 33
        LDI R7, 31
        CMP R6, R7
        JNZ br_not_r
        LDI R6, 0
        STORE 35, R6
br_not_r: LOAD R6, 34
        LDI R7, 4
        CMP R6, R7
        JNC br_paddle
        LDI R6, 1
        STORE 36, R6
        LOAD R6, 37
        INC R6
        STORE 37, R6
br_paddle: LOAD R6, 34
        LDI R7, 14
        CMP R6, R7
        JNZ br_bottom
        LOAD R6, 33
        LOAD R7, 32
        CMP R6, R7
        JC br_bottom
        LOAD R6, 32
        INC R6
        INC R6
        INC R6
        INC R6
        INC R6
        INC R6
        LOAD R7, 33
        CMP R7, R6
        JZ br_bottom
        JC br_hit
        JMP br_bottom
br_hit: LDI R6, 0
        STORE 36, R6
br_bottom: LOAD R6, 34
        LDI R7, 15
        CMP R6, R7
        JNZ br_done
        LDI R6, 16
        STORE 33, R6
        LDI R6, 8
        STORE 34, R6
        LDI R6, 0
        STORE 37, R6
br_done: NOP
"""
    update = update.replace("CMP R6, R7\n        JNC br_paddle",
                            "CMP R6, R7\n        JZ br_paddle\n        JC br_brick\n        JMP br_paddle\nbr_brick: NOP")
    pixel = "        LDI R3, 0\n"
    for yy, col in ((1, 3), (2, 14), (3, 11)):
        for xx in (1, 8, 15, 22):
            pixel += _static_rect(xx, yy, 6, 1, col)
    pixel += _dyn_rect(32, 38, 6, 1, color=15)
    pixel += _dyn_point(33, 34, color=14)
    init = _init_state([(32, 13), (33, 16), (34, 8), (35, 1), (36, 0), (37, 0), (38, 14)])
    return _screen_program(32, 16, init_code=init, update_code=update, pixel_code=pixel)


def _racing_program():
    update = _move_axis(1, 2, 32, 2, 12)
    update += """
        LOAD R6, 34
        INC R6
        MOV R7, R4
        LDI R5, 16
        AND R7, R5
        JZ race_slow
        INC R6
race_slow: STORE 34, R6
        LDI R7, 24
        CMP R6, R7
        JC race_hit
        LDI R6, 0
        STORE 34, R6
        LOAD R6, 35
        INC R6
        STORE 35, R6
        MOV R6, R0
        LOAD R7, 35
        ADD R6, R7
        LDI R7, 7
        AND R6, R7
        INC R6
        INC R6
        INC R6
        STORE 33, R6
race_hit: LOAD R6, 34
        LDI R7, 19
        CMP R6, R7
        JC race_done
        LDI R7, 22
        CMP R6, R7
        JZ race_done
        JC race_x
        JMP race_done
race_x: LOAD R6, 33
        LOAD R7, 32
        CMP R6, R7
        JC race_done
        LOAD R6, 32
        INC R6
        INC R6
        LOAD R7, 33
        CMP R7, R6
        JZ race_done
        JC race_crash
        JMP race_done
race_crash: LDI R6, 0
        STORE 35, R6
        STORE 34, R6
race_done: NOP
"""
    pixel = """
        LDI R3, 8
        LDI R4, 2
        MOV R5, R1
        CMP R5, R4
        JC race_px
        LDI R4, 14
        MOV R5, R1
        CMP R5, R4
        JZ race_road
        JC race_road
        JMP race_px
race_road: LDI R3, 5
        LDI R4, 8
        CMP R1, R4
        JNZ race_px
        MOV R4, R2
        ADD R4, R0
        LDI R5, 3
        AND R4, R5
        JNZ race_px
        LDI R3, 15
race_px: NOP
"""
    pixel += _dyn_rect(33, 34, 3, 2, color=3)
    pixel += _dyn_rect(32, 37, 3, 2, color=14)
    init = _init_state([(32, 7), (33, 5), (34, 0), (35, 0), (37, 20)])
    return _screen_program(16, 24, init_code=init, update_code=update, pixel_code=pixel)


def _invaders_program():
    update = _move_axis(1, 2, 32, 0, 29)
    update += _press(16, """
        LOAD R6, 35
        LDI R7, 0
        CMP R6, R7
        JNZ inv_fire_done
        LOAD R6, 32
        INC R6
        STORE 33, R6
        LDI R6, 13
        STORE 34, R6
        LDI R6, 1
        STORE 35, R6
        LDI R6, 15
        STORE 42, R6
inv_fire_done: NOP
""")
    update += """
        LOAD R6, 35
        LDI R7, 0
        CMP R6, R7
        JZ inv_alien
        LOAD R6, 34
        LDI R7, 0
        CMP R6, R7
        JZ inv_bullet_off
        DEC R6
        STORE 34, R6
        JMP inv_alien
inv_bullet_off: LDI R6, 0
        STORE 35, R6
        STORE 42, R6
        LDI R6, 15
        STORE 34, R6
inv_alien: LOAD R6, 40
        INC R6
        STORE 40, R6
        LDI R7, 2
        CMP R6, R7
        JNZ inv_hit
        LDI R6, 0
        STORE 40, R6
        LOAD R6, 38
        LDI R7, 1
        CMP R6, R7
        JZ inv_right
        LOAD R6, 36
        LDI R7, 0
        CMP R6, R7
        JZ inv_bounce_r
        DEC R6
        STORE 36, R6
        JMP inv_hit
inv_bounce_r: LDI R6, 1
        STORE 38, R6
        LOAD R6, 37
        INC R6
        STORE 37, R6
        JMP inv_hit
inv_right: LOAD R6, 36
        LDI R7, 29
        CMP R6, R7
        JZ inv_bounce_l
        INC R6
        STORE 36, R6
        JMP inv_hit
inv_bounce_l: LDI R6, 0
        STORE 38, R6
        LOAD R6, 37
        INC R6
        STORE 37, R6
inv_hit: LOAD R6, 35
        LDI R7, 0
        CMP R6, R7
        JZ inv_lose
        LOAD R6, 34
        LOAD R7, 37
        CMP R6, R7
        JNZ inv_lose
        LOAD R6, 33
        LOAD R7, 36
        CMP R6, R7
        JC inv_lose
        LOAD R6, 36
        INC R6
        INC R6
        INC R6
        LOAD R7, 33
        CMP R7, R6
        JZ inv_lose
        JC inv_kill
        JMP inv_lose
inv_kill: LOAD R6, 39
        INC R6
        STORE 39, R6
        LDI R6, 0
        STORE 35, R6
        STORE 42, R6
        LDI R6, 1
        STORE 37, R6
        MOV R6, R0
        LDI R7, 15
        AND R6, R7
        STORE 36, R6
inv_lose: LOAD R6, 37
        LDI R7, 13
        CMP R6, R7
        JNZ inv_done
        LDI R6, 1
        STORE 37, R6
        LDI R6, 0
        STORE 39, R6
inv_done: NOP
"""
    pixel = _formula_code("noise") + "\n"
    pixel += _dyn_rect(36, 37, 3, 2, color=3)
    pixel += _dyn_rect(32, 41, 3, 1, color=11)
    pixel += _dyn_point(33, 34, color_addr=42)
    init = _init_state([(32, 14), (33, 0), (34, 15), (35, 0), (36, 4), (37, 1),
                        (38, 1), (39, 0), (40, 0), (41, 14), (42, 0)])
    return _screen_program(32, 16, init_code=init, update_code=update, pixel_code=pixel)


def _sprite_program(w, h, bg_mode, player_color, goal_color):
    xmask = w - 1 if w in (16, 32) else 15
    ymask = h - 1 if h in (16, 32) else 15
    update = _input_move_code()
    update += """
        LOAD R6, 32
        LDI R7, 255
        CMP R6, R7
        JNZ spr_x_hi
        LDI R6, 0
        STORE 32, R6
spr_x_hi: LOAD R6, 32
        LDI R7, %d
        CMP R6, R7
        JC spr_y_lo
        LDI R6, %d
        STORE 32, R6
spr_y_lo: LOAD R6, 33
        LDI R7, 255
        CMP R6, R7
        JNZ spr_y_hi
        LDI R6, 0
        STORE 33, R6
spr_y_hi: LOAD R6, 33
        LDI R7, %d
        CMP R6, R7
        JC spr_goal_move
        LDI R6, %d
        STORE 33, R6
spr_goal_move: LOAD R6, 34
        INC R6
        LDI R7, %d
        CMP R6, R7
        JC spr_goal_ok
        LDI R6, 0
spr_goal_ok: STORE 34, R6
        LOAD R6, 32
        LOAD R7, 34
        CMP R6, R7
        JNZ spr_done
        LOAD R6, 33
        LOAD R7, 35
        CMP R6, R7
        JNZ spr_done
        LOAD R6, 36
        INC R6
        STORE 36, R6
        MOV R6, R0
        LOAD R7, 36
        ADD R6, R7
        LDI R7, %d
        AND R6, R7
        STORE 34, R6
        MOV R6, R0
        SHR R6
        SHR R6
        SHR R6
        LOAD R7, 36
        ADD R6, R7
        LDI R7, %d
        AND R6, R7
        STORE 35, R6
spr_done: NOP
""" % (w, w - 2, h, h - 2, w, xmask, ymask)
    pixel = _formula_code(bg_mode) + "\n"
    pixel += _dyn_point(34, 35, color=goal_color)
    pixel += _dyn_rect(32, 33, 2, 2, color=player_color)
    init = _init_state([(32, w // 2), (33, h // 2), (34, 2), (35, 2), (36, 0)])
    return _screen_program(w, h, init_code=init, update_code=update, pixel_code=pixel)


def _calculator_program():
    update = _press(16, """
        LOAD R6, 32
        INC R6
        STORE 32, R6
""")
    update += _press(32, """
        LOAD R6, 32
        DEC R6
        STORE 32, R6
""")
    update += _press(1, """
        LOAD R6, 32
        SHL R6
        STORE 32, R6
""")
    update += _press(2, """
        LOAD R6, 32
        SHR R6
        STORE 32, R6
""")
    update += _press(4, """
        LOAD R6, 32
        LDI R7, 10
        ADD R6, R7
        STORE 32, R6
""")
    update += _press(8, """
        LDI R6, 0
        STORE 32, R6
""")
    # Expand VAL into eight LED colours at 64..71 (MSB first) and a bar width.
    update += """
        LOAD R6, 32
        STORE 40, R6
"""
    for i in range(8):
        addr = 71 - i
        on, done = _lab("calc_on"), _lab("calc_done")
        update += """
        LOAD R6, 40
        LDI R7, 1
        AND R6, R7
        JNZ %s
        LDI R6, 5
        JMP %s
%s:     LDI R6, 14
%s:     STORE %d, R6
        LOAD R6, 40
        SHR R6
        STORE 40, R6
""" % (on, done, on, done, addr)
    update += """
        LOAD R6, 32
        SHR R6
        SHR R6
        SHR R6
        SHR R6
        STORE 41, R6
"""
    pixel = """
        LDI R3, 12
        LDI R4, 5
        MOV R5, R2
        CMP R5, R4
        JC calc_disp
        LDI R3, 5
calc_disp: LDI R4, 2
        CMP R2, R4
        JNZ calc_bar
        LDI R4, 8
        MOV R5, R1
        CMP R5, R4
        JZ calc_bar
        JC calc_led
        JMP calc_bar
calc_led: LDI R4, 64
        ADD R4, R1
        LOADR R3, R4
calc_bar: LDI R4, 12
        CMP R2, R4
        JNZ calc_keys
        LOAD R4, 41
        MOV R5, R1
        CMP R5, R4
        JZ calc_keys
        JC calc_bar_on
        JMP calc_keys
calc_bar_on: LDI R3, 10
calc_keys: NOP
"""
    for yy in (7, 9, 11):
        for xx in (1, 4, 7, 10, 13):
            pixel += _static_rect(xx, yy, 2, 1, 15 if (xx + yy) % 3 else 3)
    return _screen_program(16, 16, init_code=_init_state([(32, 0)]),
                             update_code=update, pixel_code=pixel)


def _synth_program():
    update = _press(16, """
        LOAD R6, 32
        INC R6
        LDI R7, 3
        CMP R6, R7
        JNZ synth_mode_ok
        LDI R6, 0
synth_mode_ok: STORE 32, R6
""")
    update += _move_axis(1, 2, 33, 0, 7)
    pixel = """
        LOAD R4, 32
        LDI R5, 0
        CMP R4, R5
        JZ synth_m0
        LDI R5, 1
        CMP R4, R5
        JZ synth_m1
        MOV R6, R1
        SHL R6
        XOR R6, R0
        JMP synth_h
synth_m0: MOV R6, R1
        XOR R6, R0
        JMP synth_h
synth_m1: MOV R6, R1
        ADD R6, R0
synth_h: LDI R7, 15
        AND R6, R7
        LOAD R7, 33
        ADD R6, R7
        LDI R7, 16
        SUB R7, R6
        MOV R6, R2
        CMP R6, R7
        JC synth_off
        MOV R3, R1
        ADD R3, R0
        JMP synth_done
synth_off: LDI R3, 0
synth_done: NOP
"""
    return _screen_program(32, 16, init_code=_init_state([(32, 0), (33, 0)]),
                             update_code=update, pixel_code=pixel)


def _watch_program():
    update = _press(16, """
        LOAD R6, 35
        LDI R7, 14
        CMP R6, R7
        JZ watch_dim
        LDI R6, 14
        JMP watch_col
watch_dim: LDI R6, 10
watch_col: STORE 35, R6
""")
    update += """
        LOAD R6, 34
        LDI R7, 0
        CMP R6, R7
        JZ watch_r
        LDI R7, 1
        CMP R6, R7
        JZ watch_d
        LDI R7, 2
        CMP R6, R7
        JZ watch_l
        LOAD R6, 33
        DEC R6
        STORE 33, R6
        LDI R7, 2
        CMP R6, R7
        JNZ watch_done
        LDI R6, 0
        STORE 34, R6
        JMP watch_done
watch_r: LOAD R6, 32
        INC R6
        STORE 32, R6
        LDI R7, 13
        CMP R6, R7
        JNZ watch_done
        LDI R6, 1
        STORE 34, R6
        JMP watch_done
watch_d: LOAD R6, 33
        INC R6
        STORE 33, R6
        LDI R7, 13
        CMP R6, R7
        JNZ watch_done
        LDI R6, 2
        STORE 34, R6
        JMP watch_done
watch_l: LOAD R6, 32
        DEC R6
        STORE 32, R6
        LDI R7, 2
        CMP R6, R7
        JNZ watch_done
        LDI R6, 3
        STORE 34, R6
watch_done: NOP
"""
    pixel = "        LDI R3, 0\n"
    pixel += _static_rect(2, 2, 12, 12, 12)
    pixel += _static_rect(3, 3, 10, 10, 0)
    pixel += _static_rect(7, 7, 2, 2, 15)
    pixel += _dyn_point(32, 33, color_addr=35)
    init = _init_state([(32, 2), (33, 2), (34, 0), (35, 10)])
    return _screen_program(16, 16, init_code=init, update_code=update, pixel_code=pixel)


def _desktop_program(w, h, mode):
    update = _input_move_code()
    update += """
        LOAD R6, 32
        LDI R7, 255
        CMP R6, R7
        JNZ desk_x_hi
        LDI R6, 0
        STORE 32, R6
desk_x_hi: LOAD R6, 32
        LDI R7, %d
        CMP R6, R7
        JC desk_y_lo
        LDI R6, %d
        STORE 32, R6
desk_y_lo: LOAD R6, 33
        LDI R7, 255
        CMP R6, R7
        JNZ desk_y_hi
        LDI R6, 0
        STORE 33, R6
desk_y_hi: LOAD R6, 33
        LDI R7, %d
        CMP R6, R7
        JC desk_done
        LDI R6, %d
        STORE 33, R6
desk_done: NOP
""" % (w, w - 1, h, h - 1)
    pixel = _formula_code(mode) + "\n"
    pixel += _static_rect(0, 0, w, 2, 12)
    pixel += _static_rect(w // 5, h // 5, max(6, w // 2), max(5, h // 2), 15)
    pixel += _static_rect(w // 5 + 1, h // 5 + 2, max(4, w // 2 - 2), max(3, h // 2 - 3), 6)
    pixel += _static_rect(0, h - 2, w, 2, 5)
    pixel += _dyn_rect(32, 33, 2, 2, color=15)
    init = _init_state([(32, w // 2), (33, h // 2)])
    return _screen_program(w, h, init_code=init, update_code=update, pixel_code=pixel)


def _add_device(fname, name, desc, w, h, program, pc=False, type_=3, clock=2500):
    return _add_chip(fname, type_, 8, 8, 256, clock, w, h, name,
                     desc + "\nУправление на вкладке РАБОТА: стрелки и кнопки A/B виртуального геймпада пишут код в ячейку ввода 250, а процессор устройства читает её каждый кадр. Кнопка «ПРОСМОТР ПРОЦЕССОРА» открывает кристалл этого устройства под микроскопом — можно играть и одновременно смотреть регистры, АЛУ и шины.",
                     _device_blocks(pc=pc), program)


_add_device("57_tetris.bin", "ТЕТРИС «КУБИКИ-16»",
            "Карманный тетрис: блоки 2×2 падают в стакан 16×24, складываются по высоте столбцов, а заполненный стакан очищается. Стрелки двигают блок, вниз ускоряет, A меняет цвет.",
            16, 24, _tetris_program())
_add_device("58_tamagotchi.bin", "ТАМАГОЧИ «ПИКСЕЛЬ-ПИТОМЕЦ»",
            "Электронный питомец гуляет по комнате. Верхняя полоска — сытость, вторая — настроение. A кормит, B играет, стрелки зовут питомца влево или вправо; голод и настроение со временем уменьшаются.",
            16, 16, _tamagotchi_program())
_add_device("59_snake.bin", "ИГРА «ЗМЕЙКА-16»",
            "Змейка из головы и трёх сегментов ползёт через края поля и ест красную еду. Счёт хранится в ячейке 37, новая еда появляется из счётчика времени процессора.",
            16, 16, _snake_program())
_add_device("60_pong.bin", "ИГРА «ПОНГ-32»",
            "Левая ракетка управляется стрелками вверх/вниз, правая ведётся простым ИИ: процессор сравнивает центр ракетки с мячом каждый кадр. Счёт сторон лежит в ячейках 38 и 39.",
            32, 16, _pong_program())
_add_device("61_flappy.bin", "ИГРА «ФЛАППИ-ПТИЦА»",
            "Гравитация каждый кадр тянет птицу вниз, A даёт взмах вверх. Трубы движутся справа, зазор выбирается из времени и счёта; столкновение сбрасывает игру.",
            16, 16, _flappy_program())
_add_device("62_breakout.bin", "ИГРА «АРКАНОИД-32»",
            "Платформа, мяч и три ряда тренировочных кирпичей. Мяч честно отскакивает от стен, кирпичной зоны и платформы; счёт касаний кирпичей хранится в ячейке 37.",
            32, 16, _breakout_program())
_add_device("63_racing.bin", "ИГРА «ГОНКИ-24»",
            "Машинка игрока внизу дороги, встречная машинка появляется сверху. A — ускорение, стрелки — руль. Столкновение сбрасывает счёт обгонов.",
            16, 24, _racing_program())
_add_device("64_invaders.bin", "ИГРА «ЗВЁЗДНЫЙ ЗАЩИТНИК»",
            "Корабль внизу стреляет кнопкой A, пришелец ходит зигзагом и спускается. Попадание увеличивает счёт в ячейке 39 и возвращает пришельца наверх.",
            32, 16, _invaders_program())
_add_device("65_console16.bin", "ИГРОВАЯ ПРИСТАВКА «ГЕРОЙ-16»",
            "Мини-приставка с игрой-собирателем: герой 2×2 ловит движущуюся цель на плазменном поле. Цель убегает по горизонтали, счёт попаданий лежит в ячейке 36.",
            16, 16, _sprite_program(16, 16, "xor", 14, 3))
_add_device("66_calculator.bin", "КАЛЬКУЛЯТОР «БИНАРИК»",
            "Двоичный калькулятор: A прибавляет 1, B вычитает 1, вверх прибавляет 10, вниз очищает, влево/вправо сдвигают число. Восемь светодиодов показывают биты MSB→LSB, нижняя полоска — старшие биты.",
            16, 16, _calculator_program())
_add_device("67_synthesizer.bin", "СИНТЕЗАТОР «ВОЛНА-32»",
            "Визуальный синтезатор-эквалайзер. A переключает три формы волны, стрелки влево/вправо меняют базовую амплитуду столбцов.",
            32, 16, _synth_program())
_add_device("68_smartwatch.bin", "УМНЫЕ ЧАСЫ «ОРБИТА»",
            "Секундная точка обегает периметр циферблата конечным автоматом из четырёх состояний. A переключает яркость подсветки.",
            16, 16, _watch_program())
_add_device("69_robot.bin", "РОБОТ-СБОРЩИК «КЛАД-16»",
            "Управляй роботом 2×2 и собирай батареи на складе. Цель медленно дрейфует, счёт собранных батарей процессор хранит в ячейке 36.",
            16, 16, _sprite_program(16, 16, "blocks", 11, 14))
_add_device("70_drone.bin", "ДРОН «ДОСТАВКА-32»",
            "FPV-экран дрона: веди аппарат к движущейся точке доставки над шумовой картой местности. Координаты, цель и счёт — настоящие ячейки памяти бортового процессора.",
            32, 16, _sprite_program(32, 16, "noise", 15, 3))


_pc_specs = [
    ("71_mini_pc.bin", "МИНИ-ПК «РАБОЧИЙ СТОЛ-16»", 32, 16, "blocks",
     "Полноценная учебная модель ПК: 16-битный по духу рабочий стол, окно, панель задач и курсор, который двигается геймпадом."),
    ("72_laptop.bin", "НОУТБУК «ДОРОЖНЫЙ-32»", 32, 16, "ocean",
     "Ноутбук с морским рабочим столом и окном приложения. Внутри — процессор, кэш, видеопамять, шина клавиатуры и контроллеры."),
    ("73_retro_pc.bin", "РЕТРО-КОМПЬЮТЕР «ТЕРМИНАЛ-84»", 40, 24, "matrix",
     "Домашний компьютер эпохи 8 бит: зелёные столбцы терминала, окно и курсор. Под микроскопом видна вся его несложная электроника."),
    ("74_server_pc.bin", "СЕРВЕРНЫЙ КОМПЬЮТЕР «МОНИТОРИНГ»", 48, 32, "noise",
     "Панель мониторинга сервера с шумовой телеметрией, окном графиков и курсором оператора."),
    ("75_tablet_pc.bin", "ПЛАНШЕТНЫЙ КОМПЬЮТЕР «ТАЧ-32»", 32, 32, "rings",
     "Планшет с квадратным экраном и кольцевыми обоями. Виртуальный геймпад играет роль сенсорного указателя."),
    ("76_gaming_pc.bin", "ИГРОВОЙ ПК «НЕОН-64»", 64, 32, "fire",
     "Игровой компьютер с огненным рабочим столом 64×32. Окно игры и панель задач рисует тот же процессор, который можно разобрать под микроскопом."),
    ("77_workstation.bin", "РАБОЧАЯ СТАНЦИЯ «ГРАФИКА-48»", 48, 32, "tunnel",
     "Станция для графики с туннельным тестовым изображением и большим окном предпросмотра."),
    ("78_arcade_pc.bin", "АРКАДНЫЙ КОМПЬЮТЕР «АВТОМАТ-32»", 32, 16, "xor",
     "Плазменный экран аркадного автомата, окно игры и курсор выбора. Внутри — процессор, видеопамять и шина кнопок."),
    ("79_super_pc.bin", "СУПЕРКОМПЬЮТЕР «КЛАСТЕР-64»", 64, 48, "blocks",
     "Учебная модель узла кластера: большой экран состояния, блоки вычислений и курсор оператора. Транзисторов в модели 65535 — предел поля формата, у настоящего узла их миллиарды."),
    ("80_quantum_pc.bin", "КВАНТОВЫЙ ТЕРМИНАЛ-ПК «ИНТЕРФЕЙС»", 48, 32, "rings",
     "Честная учебная модель только классического терминала управления: обычный процессор рисует кольца состояния и окно. Настоящий квантовый процессор работает совсем иначе, и игра этого не скрывает."),
]
for _fname, _name, _w, _h, _mode, _desc in _pc_specs:
    _add_device(_fname, _name, _desc, _w, _h, _desktop_program(_w, _h, _mode),
                pc=True, type_=1, clock=8000)


# ---------------------------------------------------------------------------
#  AI prompt (assets/prompt.txt) - the format contract for any AI
# ---------------------------------------------------------------------------
def make_prompt(example_b64):
    return """ТЫ - КОНСТРУКТОР МИКРОСХЕМ ДЛЯ ИГРЫ «ChipLab: Симулятор чипов» (Android).

Твоя задача: по моей просьбе спроектировать микросхему или целое устройство
(процессор, память, видеокарту, экран, логическую схему, игровую приставку,
тамагочи, калькулятор, робота или ПК) и вернуть её как ОДИН бинарный файл
формата CHIPBIN1, закодированный в Base64. Игра сама распарсит файл,
нарисует кристалл под микроскопом (блоки и транзисторы) и честно
просимулирует работу чипа по тактам: регистры, АЛУ, память, экран и кнопки.

============================================================
ФОРМАТ CHIPBIN1 (все числа - little-endian, младший байт первым)
============================================================
Смещение Размер  Поле
0        8       Магия: ASCII «CHIPBIN1» (43 48 49 50 42 49 4E 31)
8        1       Версия = 1
9        1       Тип чипа: 1=процессор(CPU) 2=память(RAM) 3=видеокарта(GPU)
                   4=монитор/экран(DISPLAY) 5=ПЗУ(ROM)
10       1       Разрядность (бит): любое значение 1..16
                   (у RAM - бит в ячейке, у GPU/экрана - бит на пиксель/цвет)
11       1       Количество регистров процессора (1..16); у RAM поставь 1
12       2       Размер памяти в ячейках (1..1024)
14       2       Частота в кГц (для красоты/статистики, 0..65000)
16       1       Ширина экрана в пикселях (0 = экрана нет; иначе 8..64)
17       1       Высота экрана в пикселях (0 или 8..64)
18       2       Число транзисторов (статистика, прикинь честно: сумматор
                   на 1 бит ~28Т, бит регистра/памяти ~6Т, логика - на глаз)
20       2       P = длина секции ПРОГРАММА в байтах (у RAM: начальные данные)
22       2       N = длина имени
24       2       D = длина описания
26       2       B = длина секции БЛОКИ
28       2       I = длина секции ISA (текст-подсказка, можно 0)
30       2       Резерв = 0
32       N       Имя чипа, UTF-8 (например «8-БИТНЫЙ ПРОЦЕССОР МОЙ-8»)
32+N     D       Описание, UTF-8: КАК работает чип, числа, что делает программа
...      B       Секция БЛОКИ: записи подряд, каждая:
                   1 байт - длина имени блока, имя (UTF-8),
                   1 байт X, 1 байт Y, 1 байт ширина, 1 байт высота
                   (координаты на кристалле, сетка 16x16: X+W<=16, Y+H<=16),
                   2 байта - транзисторов в блоке,
                   1 байт - вид блока: 0=логика 1=регистры 2=память 3=АЛУ
                   4=шина 5=экран/видеопамять 6=управление,
                   1 байт - параметр (обычно 0)
                 Блоки игра рисует под микроскопом как настоящий кристалл.
...      I       Секция ISA: текст UTF-8 (можно скопировать таблицу ниже)
...      P       Секция ПРОГРАММА: машинный код (у RAM - начальные байты ячеек)

ОГРАНИЧЕНИЯ ФОРМАТА: каждая длина N, D, B, I и P хранится в u16, поэтому
каждая секция <= 65535 байт; блоков <= 64; регистров 1..16; память
1..1024 ячеек; экран <= 64x64; число транзисторов в заголовке и в каждом
блоке <= 65535. Для удобной вставки Base64 старайся делать файл <= 64 КБ,
хотя парсер игры принимает и больше в пределах длин секций.
Если блоков нет (B=0) - игра сама построит стандартный кристалл, но с
блоками гораздо красивее: рисуй управление сверху, АЛУ рядом, регистры и
память снизу, шину полосой. Для устройства добавляй блоки экрана, кнопок,
звука, питания и датчиков - у игрока будет кнопка «ПРОСМОТР ПРОЦЕССОРА».

============================================================
МАШИННЫЙ ЯЗЫК (один для всех процессоров игры)
============================================================
""" + ISA_TEXT + """

Правила:
- Регистры R0..R(число-1). Значения всегда обрезаются по разрядности:
  у 4-битного процессора 15+1 = 0 и C=1.
- LDI загружает 8-битное число (0..255), потом оно обрезается по разрядности.
  Большие числа для 16-битного собирай сдвигами SHL и сложением.
- Адреса переходов (JMP/JZ/JNZ/JC) - СМЕЩЕНИЕ В БАЙТАХ от начала программы,
  2 байта little-endian. Считай длины команд: 1 опкод + операнды (переход = 3 байта).
- OUT: если у чипа есть экран - рисует СЛЕДУЮЩИЙ пиксель цветом = значение
  (младшие биты, палитра 16 цветов), курсор двигается сам слева-направо,
  сверху-вниз и по кругу. Если экрана нет - число попадает на «светодиоды»
  и в журнал вывода.
- IN: читает счётчик времени игры (растёт сам) - для анимаций и «случайности».
- ГЕЙМПАД УСТРОЙСТВА: если у чипа экран и память >= 256 ячеек, игра может
  записывать в ПАМЯТЬ[250] код нажатой кнопки: 1=влево, 2=вправо, 4=вверх,
  8=вниз, 16=A, 32=B. В начале кадра делай LOAD Rd,250, затем запиши 0
  обратно (STORE 250,Rноль), чтобы одно нажатие сработало один раз.
  Так делаются Тетрис, Тамагочи, приставки, калькуляторы, роботы и ПК.
- Программа может зациклиться (JMP на начало) - для экранов так и надо.
  Для вычислительных задач заканчивай HALT (FF).
- У чипа RAM программы нет: секция ПРОГРАММА = начальное содержимое ячеек
  (по 1 байту на ячейку, значения обрежутся по разрядности ячейки).

============================================================
ПРИМЕР ГОТОВОГО ЧИПА (1-битный процессор из игры, Base64)
============================================================
""" + example_b64 + """

============================================================
КАК ОТВЕЧАТЬ
============================================================
1. Одной-двумя фразами: что за чип и что делает его программа.
2. Затем ТОЛЬКО Base64-код файла CHIPBIN1 одним блоком (его вставят в игру:
   Каталог -> Импорт .bin от ИИ).
3. Проверь себя перед ответом: магия CHIPBIN1, все длины секций сходятся,
   суммарный размер = 32 + N + D + B + I + P, переходы попадают на начала
   команд, регистры не выходят за количество, адреса памяти < размера памяти.

МОЯ ПРОСЬБА: сделай """


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "..", "assets")
    os.makedirs(out_dir, exist_ok=True)
    for fname, data in CHIPS:
        path = os.path.join(out_dir, fname)
        open(path, "wb").write(data)
        print("[bins] %-18s %5d bytes" % (fname, len(data)))
    b64 = base64.encodebytes(cpu1).decode("ascii")
    # wrap at 64 chars exactly like android Base64.DEFAULT-ish; already done
    prompt = make_prompt(b64.strip())
    open(os.path.join(out_dir, "prompt.txt"), "w", encoding="utf-8").write(prompt)
    print("[bins] prompt.txt       %5d chars" % len(prompt))


if __name__ == "__main__":
    main()
