// UI for the A5 boot lab. Everything here is plain DOM + the emulator core in
// arm.js / a5.js / disasm.js; no build step, no modules (WebView refuses ES
// modules on file://, so every script is a classic one).

(function () {
    "use strict";

    var g = (typeof globalThis !== "undefined") ? globalThis : window;
    var A5 = g.A5;

    var TRACE_CAP = 1500;

    var MODE_NAMES = {
        0x10: "usr", 0x11: "fiq", 0x12: "irq", 0x13: "svc", 0x16: "mon",
        0x17: "abt", 0x1A: "hyp", 0x1B: "und", 0x1F: "sys"
    };
    var VEC_NAMES = {
        0x00: "reset", 0x04: "undefined", 0x08: "svc", 0x0C: "prefetch abort",
        0x10: "data abort", 0x18: "irq", 0x1C: "fiq"
    };
    var FSR_NAMES = {
        0x01: "alignment", 0x03: "access flag L1", 0x05: "translation fault L1",
        0x06: "access flag L2", 0x07: "translation fault L2",
        0x09: "domain fault L1", 0x0B: "domain fault L2",
        0x0D: "permission fault L1", 0x0F: "permission fault L2"
    };

    // Stages 2 and 3 run code past the handoff, where SCTLR.M is already set, so
    // every access in them is a virtual address.
    var HANDOFF_VA = (A5.HANDOFF & ~1) >>> 0;      // 0x80017C40
    var POST_PA = (HANDOFF_VA - A5.VIRT_OFFSET) >>> 0;   // 0x40017C40
    var VEC_PA = 0x40031000;
    var SCRATCH_PA = 0x40030200;
    var STOP_VA = 0x80020200;

    var state = {
        m: null,
        trace: [],
        traceTotal: 0,
        lastRegs: null,
        ran: false,
        elapsedMs: 0,
        stage: 1
    };

    function $(id) { return document.getElementById(id); }
    function hex(v, w) {
        var s = (v >>> 0).toString(16).toUpperCase();
        while (w && s.length < w) s = "0" + s;
        return "0x" + s;
    }
    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text !== undefined && text !== null) e.textContent = text;
        return e;
    }
    function clear(node) { while (node.firstChild) node.removeChild(node.firstChild); }

    var REG_NAMES = ["r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7",
                     "r8", "r9", "r10/sl", "r11/fp", "r12/ip", "r13/sp", "r14/lr", "r15/pc"];

    // ---------------------------------------------------------------- reset

    /** Build a machine, load boot_args and the entry code, wire the tracer. */
    function bootMachine() {
        var m = new A5.A5Machine();
        var args = A5.buildBootArgs({
            virtBase: 0x80000000,
            physBase: 0x40000000,
            memSize: 0x20000000,
            topOfKernelData: A5.TOPOFKERNELDATA,
            deviceTreeP: A5.DT_VA,
            deviceTreeLength: A5.DT_LEN,
            commandLine: "-v"
        });
        m.argsPa = m.writeBootArgs(args);
        m.args = args;
        m.write(A5.ENTRY_PA, A5.ENTRY_CODE);
        m.cpu.r[0] = m.argsPa;
        m.cpu.r[14] = A5.HANDOFF;
        m.cpu.r[15] = A5.ENTRY_PA;
        m.cpu.onStep = function (pc, thumb) { recordStep(pc, thumb); };
        return m;
    }

    function clearRunState() {
        state.trace = [];
        state.traceTotal = 0;
        state.lastRegs = null;
        state.ran = false;
        state.elapsedMs = 0;
    }

    function reset() {
        state.m = bootMachine();
        state.m.cpu.stopAt = HANDOFF_VA;
        state.stage = 1;
        clearRunState();
        setStatus("сброшено · PC = " + hex(A5.ENTRY_PA), "");
        renderAll();
    }

    /**
     * Stage 2: run straight through the handoff into a Thumb stub that only ever
     * uses virtual addresses. Its stores are read back through the *physical*
     * bus afterwards, so a wrong table walk cannot hide.
     */
    function runStage2() {
        var TC = A5.TESTCODE;
        var m = bootMachine();
        state.m = m;
        state.stage = 2;
        clearRunState();
        var post = TC.thumb_mmu_post.bytes;
        m.write(POST_PA, post);
        var t0 = Date.now();
        m.run({ count: 400000, stopAt: HANDOFF_VA });
        m.resume({ stopAt: (HANDOFF_VA + post.length) >>> 0 });
        m.run({ count: 4000 });
        state.elapsedMs += (Date.now() - t0);
        state.ran = true;
        afterRun(m.cpu.stopReason);
    }

    /**
     * Stage 3: the same, but the stub touches VA 0xC0000000, which the boot page
     * table deliberately leaves unmapped, so a data abort is taken. The vector
     * page has to be installed *after* start() runs, because start() zeroes
     * 40 KB of tables at TTB and that region contains the high-vector L2.
     */
    function runStage3() {
        var TC = A5.TESTCODE;
        var m = bootMachine();
        state.m = m;
        state.stage = 3;
        clearRunState();
        var probe = TC.thumb_abort_probe.bytes;
        var handler = TC.abt_handler.bytes;
        m.write(POST_PA, probe);
        // The handler's first instruction must sit exactly on the data-abort
        // vector, i.e. at VA 0xFFFF0010, hence the +0x10.
        m.write((VEC_PA + 0x10) >>> 0, handler);
        var t0 = Date.now();
        m.run({ count: 400000, stopAt: HANDOFF_VA });
        var L2 = (A5.TOPOFKERNELDATA + 0x9000) >>> 0;
        var l2idx = ((0xFFFF0000 >>> 12) & 0xFF);
        m.write32((L2 + l2idx * 4) >>> 0, ((VEC_PA & 0xFFFFF000) | 0x12) >>> 0);
        m.resume({ stopAt: STOP_VA });
        m.run({ count: 4000 });
        state.elapsedMs += (Date.now() - t0);
        state.ran = true;
        afterRun(m.cpu.stopReason);
    }

    function recordStep(pc, thumb) {
        if (!$("cb-trace").checked) return;
        var bus = state.m.bus;
        var text, bytes;
        if (thumb) {
            var hw = bus.read16(pc);
            text = A5.disasmThumb(hw, pc);
            bytes = ("0x" + ("000" + hw.toString(16)).slice(-4));
        } else {
            var insn = bus.read32(pc);
            text = A5.disasmArm(insn, pc);
            bytes = ("0x" + ("0000000" + (insn >>> 0).toString(16)).slice(-8));
        }
        state.trace.push({ n: state.m.cpu.instrCount, pc: pc, text: text, bytes: bytes, thumb: thumb });
        if (state.trace.length > TRACE_CAP) state.trace.shift();
        state.traceTotal++;
    }

    // ---------------------------------------------------------------- run

    function step(n) {
        var m = state.m;
        if (m.cpu.halted) { setStatus("остановлено: " + m.cpu.stopReason, "bad"); return; }
        var cpu = m.cpu;
        var i = 0;
        var t0 = Date.now();
        while (i < n && !cpu.halted) {
            var before = cpu.r[15] >>> 0;
            cpu.step();
            var after = cpu.r[15] >>> 0;
            i++;
            // same no-progress guard the Python model uses
            if (after === before && !cpu.halted) {
                m._np = (m._np || 0) + 1;
                if (m._np >= A5.STALL_THRESHOLD) {
                    m.stalled = { pc: after, repeats: m._np };
                    cpu.halt("stalled", after);
                    break;
                }
            } else { m._np = 0; }
        }
        state.elapsedMs += (Date.now() - t0);
        state.ran = true;
        afterRun(cpu.stopReason);
    }

    function runAll() {
        var m = state.m;
        var cpu = m.cpu;
        if (cpu.halted) { reset(); }
        var t0 = Date.now();
        m.run({ count: 400000, stopAt: (A5.HANDOFF & ~1) >>> 0 });
        state.elapsedMs += (Date.now() - t0);
        state.ran = true;
        afterRun(cpu.stopReason);
    }

    function afterRun(reason) {
        var m = state.m;
        var stageName = state.stage === 1 ? "" : " (фаза " + state.stage + ")";
        if (reason === "handoff") {
            var c = computeChecks();
            var bad = c.filter(function (x) { return !x.ok; }).length;
            setStatus(bad ? ("handoff" + stageName + ", но " + bad + " проверок провалено")
                          : ("handoff" + stageName + " · все проверки пройдены"),
                      bad ? "bad" : "ok");
        } else if (reason) {
            setStatus("останов: " + reason + " @ " + hex(m.cpu.r[15] >>> 0), "bad");
        } else {
            setStatus("выполняется · " + m.cpu.instrCount + " инструкций", "run");
        }
        renderAll();
    }

    function setStatus(text, cls) {
        var s = $("status");
        s.textContent = text;
        s.className = "status" + (cls ? " " + cls : "");
    }

    // ---------------------------------------------------------------- checks

    var UNICORN = { instr: 45096, cpsr: 0x600001F3, r0: 0x8FF01000, r5: 512, ms: 95 };

    function computeChecks() {
        var m = state.m;
        if (!m || !state.ran) return [];
        var cpu = m.cpu;
        var TTB = A5.TOPOFKERNELDATA;
        var first = 0x80000000 >>> 20;
        var l1 = function (i) { return m.read32((TTB + i * 4) >>> 0); };
        var F = A5.SECTION_FLAGS;
        var out = [];
        function add(name, ok, det) { out.push({ name: name, ok: !!ok, det: det || "" }); }

        add("останов на точке передачи управления", cpu.stopReason === "handoff",
            "reason = " + (cpu.stopReason || "—"));
        // These five describe the machine *at* the handoff. Stages 2 and 3 run
        // past it, so the registers and the instruction count legitimately move
        // on; asserting the handoff values there would only produce red rows
        // that look like failures and are not.
        if (state.stage === 1) {
            add("PC = 0x80017C40 (arm_init, Thumb)",
                (cpu.r[15] >>> 0) === HANDOFF_VA, hex(cpu.r[15]));
            add("r0 = boot_args как VA", (cpu.r[0] >>> 0) === UNICORN.r0, hex(cpu.r[0]));
            add("CPSR совпадает с Unicorn", (cpu.cpsr >>> 0) === UNICORN.cpsr, hex(cpu.cpsr));
            add("бит CPSR.T выставлен", (cpu.cpsr & 0x20) !== 0);
            add("r5 = 512: быстрый путь заполнения", cpu.r[5] === UNICORN.r5, "r5 = " + cpu.r[5]);
        }
        add("TTB выровнена на 16 КБ", (TTB & 0x3FFF) === 0, hex(TTB));
        add("L1[0x800] = physBase | 0x40E", l1(first) === ((0x40000000 | F) >>> 0), hex(l1(first)));
        add("L1[0x9FF] = последняя из 512 секций",
            l1(first + 511) === (((0x40000000 + 511 * 0x100000) | F) >>> 0), hex(l1(first + 511)));
        add("L1[0xA00] всё ещё ноль (40 КБ очищено)", l1(first + 512) === 0);
        add("L1[pc>>20] — тождественное отображение",
            l1(A5.ENTRY_PA >>> 20) === (((A5.ENTRY_PA & 0xFFF00000) | F) >>> 0), hex(l1(A5.ENTRY_PA >>> 20)));
        add("L1[0xFFF] → таблица L2 для high vectors",
            l1(0xFFF) === (((TTB + 0x9000) | 1) >>> 0), hex(l1(0xFFF)));
        add("TTBR0 == TTBR1 == TTB",
            m.sysreg(m.TTBR0) === TTB && m.sysreg(m.TTBR1) === TTB,
            hex(m.sysreg(m.TTBR0)) + " / " + hex(m.sysreg(m.TTBR1)));
        add("SCTLR |= M|C|W|Z|I|V (0x380D)", (m.sysreg(m.SCTLR) & 0x380D) === 0x380D, hex(m.sysreg(m.SCTLR)));
        var kinds = cpu.cp15Log.map(function (e) { return e.kind; });
        // In stage 3 the abort handler reads DFSR and FAR, so two more MRCs
        // appear and the boot-time shape no longer holds.
        if (state.stage !== 3) {
            add("перехвачено 3 MCR и 1 MRC",
                kinds.filter(function (k) { return k === "mcr"; }).length === 3 &&
                kinds.filter(function (k) { return k === "mrc"; }).length === 1,
                kinds.join(" "));
        } else {
            add("обработчик прочитал DFSR и FAR через MRC",
                kinds.filter(function (k) { return k === "mrc"; }).length === 3,
                kinds.join(" "));
        }
        add("не встретилось нереализованных инструкций", cpu.unimplementedCount === 0,
            "count = " + cpu.unimplementedCount);
        if (state.stage === 1) {
            add("число инструкций совпало с Unicorn", cpu.instrCount === UNICORN.instr,
                cpu.instrCount + " (Unicorn: " + UNICORN.instr + ")");
        }

        if (state.stage === 2) {
            add("фаза 2 дошла до конца Thumb-заглушки",
                (cpu.r[15] >>> 0) === (HANDOFF_VA + A5.TESTCODE.thumb_mmu_post.bytes.length) >>> 0,
                hex(cpu.r[15]));
            var t1 = m.translate(0x80020000, "read");
            add("трансляция VA 0x80020000 → PA 0x40020000",
                t1.ok && t1.pa === 0x40020000, JSON.stringify(t1));
            add("байт, записанный по VA, виден физически",
                m.bus.read8(0x40020000) === 0xAB, hex(m.bus.read8(0x40020000), 2));
            add("слово, записанное по VA, видно физически",
                m.read32(0x40020004) === 0xAB, hex(m.read32(0x40020004)));
            add("Epoch прочитан из boot_args по VA",
                m.bus.read8(0x40020008) === 0x12, hex(m.bus.read8(0x40020008), 2));
            add("VA 0xC0000000 не отображён загрузочной таблицей",
                m.translate(0xC0000000, "read").ok === false);
            add("исключений на фазе 2 не было", cpu.exceptionCount === 0,
                "count = " + cpu.exceptionCount);
        } else if (state.stage === 3) {
            add("фаза 3 вышла через обработчик по отображённому VA",
                (cpu.r[15] >>> 0) === STOP_VA, hex(cpu.r[15]));
            add("взят ровно один data abort", m.cpu.aborts.data === 1,
                "data = " + m.cpu.aborts.data);
            add("prefetch abort не взят", m.cpu.aborts.prefetch === 0,
                "prefetch = " + m.cpu.aborts.prefetch);
            add("DFSR = ошибка трансляции уровня 1",
                m.read32(SCRATCH_PA) === A5.FSR.TRANS_LEVEL1,
                hex(m.read32(SCRATCH_PA), 2) + " " + (FSR_NAMES[m.read32(SCRATCH_PA) & 0x1F] || ""));
            add("FAR = адрес, вызвавший отказ",
                m.read32(SCRATCH_PA + 4) === 0xC0000000, hex(m.read32(SCRATCH_PA + 4)));
            add("SPSR_abt сохранил CPSR прерванной программы",
                m.read32(SCRATCH_PA + 8) === 0x400001F3, hex(m.read32(SCRATCH_PA + 8)));
            add("LR_abt = адрес ошибки + 8",
                m.read32(SCRATCH_PA + 12) === (HANDOFF_VA + 6 + 8) >>> 0,
                hex(m.read32(SCRATCH_PA + 12)));
            add("обработчик выполнен в режиме Abort",
                (m.read32(SCRATCH_PA + 16) & 0x1F) === 0x17,
                MODE_NAMES[m.read32(SCRATCH_PA + 16) & 0x1F] || "?");
            add("прерванная загрузка не записала приёмник",
                m.read32(SCRATCH_PA + 24) === 0, hex(m.read32(SCRATCH_PA + 24)));
            add("выполнение не продолжилось за точкой отказа",
                cpu.r[0] !== 0xEE, hex(cpu.r[0] >>> 0));
        }
        return out;
    }

    // ---------------------------------------------------------------- render

    function renderRegs() {
        var cpu = state.m.cpu;
        var box = $("regs");
        clear(box);
        for (var i = 0; i < 16; i++) {
            var v = cpu.r[i] >>> 0;
            box.appendChild(el("span", "rn", REG_NAMES[i]));
            var cls = "rv" + (i === 15 ? " pc" : "");
            if (state.lastRegs && state.lastRegs[i] !== v) cls += " changed";
            box.appendChild(el("span", cls, hex(v, 8)));
        }
        state.lastRegs = Array.prototype.slice.call(cpu.r, 0, 16).map(function (x) { return x >>> 0; });

        var f = $("flags");
        clear(f);
        var cpsr = cpu.cpsr >>> 0;
        var defs = [["N", 31], ["Z", 30], ["C", 29], ["V", 28], ["T", 5], ["I", 7], ["F", 6]];
        defs.forEach(function (d) {
            var on = (cpsr >>> d[1]) & 1;
            f.appendChild(el("span", "flag" + (on ? " on" : ""), d[0] + "=" + on));
        });
        f.appendChild(el("span", "flag on",
                         "mode " + hex(cpsr & 0x1F, 2) + " " + (MODE_NAMES[cpsr & 0x1F] || "?")));
        f.appendChild(el("span", "flag", "CPSR " + hex(cpsr, 8)));
    }

    function renderChecks() {
        var box = $("checks");
        clear(box);
        var list = computeChecks();
        var pass = list.filter(function (x) { return x.ok; }).length;
        $("checks-count").textContent = list.length ? (pass + " / " + list.length) : "";
        if (!list.length) {
            box.appendChild(el("div", "hint", "нажмите «Запустить»"));
            return;
        }
        list.forEach(function (c) {
            var row = el("div", "check " + (c.ok ? "pass" : "fail"));
            row.appendChild(el("span", "mark", c.ok ? "OK" : "×"));
            var nm = el("span", "name", c.name);
            if (c.det) nm.appendChild(el("span", "det", c.det));
            row.appendChild(nm);
            box.appendChild(row);
        });
    }

    // Rendering 1500 rows through createElement on every single step is
    // visibly laggy on a phone, so the trace is built as one HTML string and
    // only its tail is painted.
    var TRACE_RENDER = 260;

    function esc(t) {
        return t.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }

    function renderTrace() {
        var box = $("trace");
        var t = state.trace;
        $("trace-hint").textContent = t.length
            ? ("последние " + t.length + " из " + state.traceTotal)
            : "";
        var from = t.length > TRACE_RENDER ? t.length - TRACE_RENDER : 0;
        var html = "";
        for (var i = from; i < t.length; i++) {
            var e = t[i];
            var kind = /^(mcr|mrc)/.test(e.text) ? " cp15"
                     : (/^(b|bl|blx|bx)/.test(e.text) ? " branch" : "");
            html += '<div class="trow' + kind + '">'
                 +  '<span class="tn">' + e.n + '</span>'
                 +  '<span class="ta">' + hex(e.pc, 8) + '</span>'
                 +  '<span class="tb">' + e.bytes + '</span>'
                 +  '<span class="td">' + (e.thumb ? "[T] " : "") + esc(e.text) + '</span>'
                 +  '</div>';
        }
        box.innerHTML = html;
        box.scrollTop = box.scrollHeight;
    }

    function renderBootArgs() {
        var box = $("bootargs");
        clear(box);
        var p = A5.parseBootArgs(state.m.args);
        var rows = [
            ["revision", p.revision], ["epoch", hex(p.epoch, 4)],
            ["virtBase", hex(p.virtBase)], ["physBase", hex(p.physBase)],
            ["memSize", hex(p.memSize)], ["topOfKernelData", hex(p.topOfKernelData)],
            ["machineType", p.machineType], ["deviceTreeP", hex(p.deviceTreeP)],
            ["deviceTreeLength", hex(p.deviceTreeLength)],
            ["CommandLine", JSON.stringify(p.CommandLine)],
            ["bootFlags", hex(p.bootFlags)], ["memSizeActual", hex(p.memSizeActual)],
            ["r0 передан как", hex(state.m.argsPa)]
        ];
        rows.forEach(function (r) {
            box.appendChild(el("span", "k", r[0]));
            box.appendChild(el("span", "v", String(r[1])));
        });
    }

    function renderPageTable() {
        var box = $("pagetable");
        clear(box);
        var TTB = A5.TOPOFKERNELDATA;
        $("ttb-hint").textContent = "TTB " + hex(TTB);
        var idxs = [0x400, 0x800, 0x801, 0x8FF, 0x9FE, 0x9FF, 0xA00, 0xFFF];
        idxs.forEach(function (i) {
            var v = state.m.read32((TTB + i * 4) >>> 0);
            var note = "";
            if (i === 0x400) note = "  ← PC";
            if (i === 0x800) note = "  ← virtBase";
            if (i === 0xFFF) note = "  ← high vectors";
            box.appendChild(el("span", "k", "L1[" + hex(i, 3) + "]"));
            box.appendChild(el("span", "v", hex(v, 8) + note));
        });
    }

    function renderCp15() {
        var box = $("cp15");
        clear(box);
        var m = state.m;
        [["SCTLR", m.SCTLR], ["TTBR0", m.TTBR0], ["TTBR1", m.TTBR1]].forEach(function (r) {
            box.appendChild(el("span", "k", r[0]));
            box.appendChild(el("span", "v", hex(m.sysreg(r[1]), 8)));
        });

        var log = $("cp15log");
        clear(log);
        m.cpu.cp15Log.forEach(function (e) {
            var key = e.opc1 + "," + e.crn + "," + e.crm + "," + e.opc2;
            var name = A5.SYSREG_NAMES[key] || ("S(" + key + ")");
            var v = m.cpu.cp15[key] >>> 0;
            var row = el("div", "lrow");
            row.appendChild(el("span", "lk", e.kind));
            row.appendChild(document.createTextNode(
                "  " + hex(e.pc, 8) + "  " + name + "  r" + e.rt + " = " + hex(v, 8)));
            log.appendChild(row);
        });
    }

    function renderMmu() {
        var m = state.m;
        var box = $("mmu");
        clear(box);
        var sctlr = m.sysreg(m.SCTLR) >>> 0;
        var bits = [["M", 0], ["A", 1], ["C", 2], ["W", 3], ["Z", 11], ["I", 12], ["V", 13]];
        var on = bits.filter(function (b) { return (sctlr >>> b[1]) & 1; })
                     .map(function (b) { return b[0]; });
        var rows = [
            ["трансляция", ((sctlr & 1) ? "ВКЛЮЧЕНА" : "выключена")],
            ["SCTLR", hex(sctlr, 8) + "  (" + (on.join(" ") || "—") + ")"],
            ["TTBR0", hex(m.sysreg(m.TTBR0))],
            ["TTBR1", hex(m.sysreg(m.TTBR1))],
            ["TTBCR.N", String(m.sysreg(m.TTBCR) & 7)],
            ["DACR", hex(m.sysreg(m.DACR))],
            ["вектора", ((sctlr >>> 13) & 1) ? "0xFFFF0000" : "0x00000000"],
            ["DFSR", hex(m.sysreg(m.DFSR), 2) + "  " + (FSR_NAMES[m.sysreg(m.DFSR) & 0x1F] || "")],
            ["IFSR", hex(m.sysreg(m.IFSR), 2) + "  " + (FSR_NAMES[m.sysreg(m.IFSR) & 0x1F] || "")],
            ["FAR", hex(m.sysreg(m.FAR))]
        ];
        rows.forEach(function (r) {
            box.appendChild(el("span", "k", r[0]));
            box.appendChild(el("span", "v", r[1]));
        });
        $("mmu-walks").textContent = "обходов таблиц: " + m.cpu.mmu.walks;
    }

    function renderExceptions() {
        var m = state.m;
        var box = $("exc");
        clear(box);
        $("exc-count").textContent = m.cpu.exceptionCount
            ? (m.cpu.exceptionCount + " всего") : "";
        [["всего", m.cpu.exceptionCount],
         ["prefetch abort", m.cpu.aborts.prefetch],
         ["data abort", m.cpu.aborts.data],
         ["барьеры DSB/ISB", m.cpu.barrierCount],
         ["текущий режим", MODE_NAMES[m.cpu.mode()] || hex(m.cpu.mode(), 2)]]
            .forEach(function (r) {
                box.appendChild(el("span", "k", r[0]));
                box.appendChild(el("span", "v", String(r[1])));
            });

        var log = $("exclog");
        clear(log);
        m.cpu.exceptionLog.slice(-24).forEach(function (e) {
            var row = el("div", "lrow");
            row.appendChild(el("span", "lk", VEC_NAMES[e.vec] || ("vec " + hex(e.vec, 2))));
            row.appendChild(document.createTextNode(
                "  из " + (MODE_NAMES[e.from] || "?") + " → " + (MODE_NAMES[e.mode] || "?") +
                "  pc " + hex(e.pc) + "  lr " + hex(e.lr) + "  spsr " + hex(e.spsr)));
            log.appendChild(row);
        });
    }

    function renderMmio() {
        var m = state.m;
        $("mmio-count").textContent = m.unimplementedTotal
            ? (m.unimplementedTotal + " записей") : "";
        var log = $("mmiolog");
        clear(log);
        var entries = m.unimplemented.slice(-60);
        entries.forEach(function (e) {
            var row = el("div", "lrow");
            row.appendChild(el("span", "lk", e.kind));
            row.appendChild(document.createTextNode(
                "  off " + hex(e.addr, 4) + "  size " + e.size +
                (e.kind.indexOf("write") >= 0 ? "  val " + hex(e.value, 8) : "")));
            log.appendChild(row);
        });
    }

    function renderConsole() {
        $("console").textContent = state.m.consoleText;
    }

    function probeTranslation() {
        var box = $("tlresult");
        clear(box);
        var va = parseInt($("va-addr").value.trim(), 16);
        if (!isFinite(va)) { box.appendChild(el("div", "lrow", "некорректный адрес")); return; }
        va >>>= 0;
        var m = state.m;
        var t = m.translate(va, "read");
        var row = el("div", "lrow");
        row.appendChild(el("span", "lk", hex(va)));
        if (t.ok) {
            row.appendChild(document.createTextNode("  →  " + hex(t.pa) +
                "   (PA - VA = " + ((t.pa - va) | 0) + ")"));
        } else {
            row.appendChild(document.createTextNode("  →  ОТКАЗ  " +
                (FSR_NAMES[t.fsr] || ("fsr " + hex(t.fsr, 2))) +
                "  уровень " + t.level + "  домен " + t.domain));
        }
        box.appendChild(row);
        var l1i = va >>> 20;
        var l1v = m.read32((m.sysreg(m.TTBR0) & 0xFFFFC000) + l1i * 4);
        box.appendChild(el("div", "lrow",
            "L1[" + hex(l1i, 3) + "] = " + hex(l1v, 8) + "  тип " +
            ["отказ", "таблица страниц", "секция", "зарезервировано"][l1v & 3]));
    }

    function renderMemory() {
        var box = $("mem");
        clear(box);
        var raw = $("mem-addr").value.trim();
        var addr = parseInt(raw, 16);
        if (!isFinite(addr)) { $("mem-note").textContent = "некорректный адрес"; return; }
        $("mem-note").textContent = "";
        addr = addr >>> 0;
        var inRam = addr >= A5.RAM_BASE && addr < (A5.RAM_BASE + A5.RAM_SIZE) >>> 0;
        $("mem-note").textContent = inRam ? "RAM" : "MMIO / вне RAM";

        var m = state.m;
        for (var row = 0; row < 16; row++) {
            var a = (addr + row * 16) >>> 0;
            var line = el("div");
            line.appendChild(el("span", "off", hex(a, 8) + "  "));
            var words = [];
            for (var w = 0; w < 4; w++) words.push(hex(m.bus.read32((a + w * 4) >>> 0), 8));
            line.appendChild(document.createTextNode(words.join(" ")));
            box.appendChild(line);
        }
    }

    function renderCounters() {
        $("k-instr").textContent = state.m.cpu.instrCount;
        $("k-time").textContent = state.elapsedMs.toFixed(1);
        $("k-reason").textContent = state.m.cpu.stopReason || "—";
        var done = state.m.cpu.halted;
        $("btn-run").disabled = done && state.m.cpu.stopReason === "handoff";
    }

    function renderAll() {
        renderRegs();
        renderChecks();
        renderTrace();
        renderBootArgs();
        renderPageTable();
        renderMmu();
        renderExceptions();
        renderCp15();
        renderMmio();
        renderConsole();
        renderMemory();
        renderCounters();
    }

    // ---------------------------------------------------------------- wiring

    function init() {
        $("btn-reset").addEventListener("click", reset);
        $("btn-step").addEventListener("click", function () { step(1); });
        $("btn-step1k").addEventListener("click", function () { step(1000); });
        $("btn-run").addEventListener("click", runAll);
        $("btn-stage2").addEventListener("click", runStage2);
        $("btn-stage3").addEventListener("click", runStage3);
        $("btn-tl").addEventListener("click", probeTranslation);
        $("va-addr").addEventListener("keydown", function (ev) {
            if (ev.key === "Enter") probeTranslation();
        });
        $("btn-mem").addEventListener("click", renderMemory);
        $("mem-addr").addEventListener("keydown", function (ev) {
            if (ev.key === "Enter") renderMemory();
        });
        $("cb-trace").addEventListener("change", function () {
            if (!this.checked) { state.trace = []; renderTrace(); }
        });
        reset();
        // Kick the run off automatically so the app shows a result immediately
        // instead of an empty screen.
        setTimeout(runAll, 60);
    }

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
    else init();

    g.A5APP = { state: state, reset: reset, step: step, runAll: runAll,
                runStage2: runStage2, runStage3: runStage3, checks: computeChecks };
})();
