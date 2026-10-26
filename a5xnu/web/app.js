// UI for the A5 boot lab. Everything here is plain DOM + the emulator core in
// arm.js / a5.js / disasm.js; no build step, no modules (WebView refuses ES
// modules on file://, so every script is a classic one).

(function () {
    "use strict";

    var g = (typeof globalThis !== "undefined") ? globalThis : window;
    var A5 = g.A5;

    var TRACE_CAP = 1500;

    var state = {
        m: null,
        trace: [],
        traceTotal: 0,
        lastRegs: null,
        ran: false,
        elapsedMs: 0
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

    function reset() {
        state.m = new A5.A5Machine();
        var m = state.m;

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
        m.cpu.stopAt = (A5.HANDOFF & ~1) >>> 0;

        m.cpu.onStep = function (pc, thumb) { recordStep(pc, thumb); };

        state.trace = [];
        state.traceTotal = 0;
        state.lastRegs = null;
        state.ran = false;
        state.elapsedMs = 0;
        setStatus("сброшено · PC = " + hex(A5.ENTRY_PA), "");
        renderAll();
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
        if (reason === "handoff") {
            var c = computeChecks();
            var bad = c.filter(function (x) { return !x.ok; }).length;
            setStatus(bad ? ("handoff, но " + bad + " проверок провалено") : "handoff · все проверки пройдены",
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

    var UNICORN = { instr: 45094, cpsr: 0x600001F3, r0: 0x8FF01000, r5: 512, ms: 125 };

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
        add("PC = 0x80017C40 (arm_init, Thumb)", (cpu.r[15] >>> 0) === (A5.HANDOFF & ~1) >>> 0,
            hex(cpu.r[15]));
        add("r0 = boot_args как VA", (cpu.r[0] >>> 0) === UNICORN.r0, hex(cpu.r[0]));
        add("CPSR совпадает с Unicorn", (cpu.cpsr >>> 0) === UNICORN.cpsr, hex(cpu.cpsr));
        add("бит CPSR.T выставлен", (cpu.cpsr & 0x20) !== 0);
        add("r5 = 512: быстрый путь заполнения", cpu.r[5] === UNICORN.r5, "r5 = " + cpu.r[5]);
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
        add("перехвачено 3 MCR и 1 MRC",
            kinds.filter(function (k) { return k === "mcr"; }).length === 3 &&
            kinds.filter(function (k) { return k === "mrc"; }).length === 1,
            kinds.join(" "));
        add("не встретилось нереализованных инструкций", cpu.unimplementedCount === 0,
            "count = " + cpu.unimplementedCount);
        add("число инструкций совпало с Unicorn", cpu.instrCount === UNICORN.instr,
            cpu.instrCount + " (Unicorn: " + UNICORN.instr + ")");
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
        f.appendChild(el("span", "flag", "mode=" + hex(cpsr & 0x1F, 2)));
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

    g.A5APP = { state: state, reset: reset, step: step, runAll: runAll, checks: computeChecks };
})();
