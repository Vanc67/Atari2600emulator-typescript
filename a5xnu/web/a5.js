// A5 (S5L8940x / k93ap) machine model — JavaScript port.
//
// Mirrors a5xnu/machine.py field for field so the two implementations can be
// run against the same byte-identical kernel-entry code and required to agree.
// Where the Python model and this one could drift, tests-js/test_a5_boot.js
// pins both to the same expected values.

// Resolve the namespace from the global object so that every file shares one
// A5. In a browser `var A5` at top level already does this; under Node/CommonJS
// each module has its own scope, so without this arm.js and a5.js end up with
// two different A5 objects and `new A5.Bus(...)` fails.
var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});

(function (A5) {
    "use strict";

    // -- memory map ------------------------------------------------------
    A5.RAM_BASE = 0x40000000;
    // Was 0x80000000 so that the kernel's virtual address range 0x80000000..
    // happened to be backed as well. That is an alias, not a translation: it
    // cannot fault and cannot enforce permissions, so an access to VA
    // 0xC0000000 -- which the boot page table deliberately leaves unmapped and
    // which arm_vm_init only maps later -- silently succeeded. Now that mmu.js
    // walks the real tables, RAM is sized to what boot_args reports and the
    // VA range is reachable only through a descriptor that actually exists.
    A5.RAM_SIZE = 0x20000000;          // 512 MB, = boot_args.memSize
    A5.VIRT_OFFSET = 0x40000000;       // gVirtBase: va = pa + 0x40000000
    A5.KERNEL_VA = 0x80001000;

    // Reset value of the domain access control register. All 16 domains as
    // "client" (0b01) rather than "manager" (0b11), so the AP/APX/XN bits of
    // every descriptor are actually checked instead of being waved through.
    A5.DACR_RESET = 0x55555555;

    A5.TOPOFKERNELDATA = 0x40DA0000;
    A5.BOOTARGS_PA = 0x4FF01000;
    A5.BOOTARGS_SIZE = 0x140;
    A5.DT_PA = 0x4F000000;
    A5.DT_VA = 0x8F000000;
    A5.DT_LEN = 0x11FD8;
    A5.UART0_PA = 0x02500000;

    // Section descriptor bits, per ARMv7-A short descriptors (cross-checked
    // against Linux arch/arm/include/asm/pgtable-2level-hwdef.h):
    //   TYPE_SECT 1<<0 (=0x2), BUFFERABLE 1<<2, CACHEABLE 1<<3, XN 1<<4,
    //   AP_WRITE 1<<10, AP_READ 1<<11, TEX(x) x<<12, APX 1<<15
    // 0x40E = section | B | C | AP_WRITE -> write-back cacheable, privileged RW.
    // The published k93ap worklog writes "TTB = topOfKernelData | 0x4A"; that is
    // neither a 16 KB-aligned L1 base nor a valid descriptor (bit 6 is SBZ, TEX
    // lives at [14:12], AP would be 0 = no access). See README.md.
    A5.SECTION_FLAGS = 0x040E;

    A5.STALL_THRESHOLD = 8;
    A5.MAX_UNIMPLEMENTED = 4096;

    A5.MMIO_DEVICES = [
        ["aic",   0x0F200000, 0x10000],
        ["pmgr",  0x0F100000, 0x7000],
        ["pmgr2", 0x08C00000, 0x1000],
        ["pmgr3", 0x08D00000, 0x1000],
        ["pmgr4", 0x08E00000, 0x1000],
        ["pmgr5", 0x08F00000, 0x1000],
        ["pmgr6", 0x0A500000, 0x1000],
        ["pmgr7", 0x0A600000, 0x1000],
        ["wdt",   0x0F103020, 0x10],
        ["gpio",  0x0FA00000, 0x1000],
        ["uart0", A5.UART0_PA, 0x100],
        ["uart3", 0x02800000, 0x100],
        ["uart5", 0x02A00000, 0x100],
        ["i2c0",  0x03200000, 0x1000],
        ["i2c1",  0x03300000, 0x1000],
        ["i2c2",  0x03400000, 0x1000],
        ["fmi",   0x01200000, 0x190000],
        ["spi1",  0x02100000, 0x1000],
        ["sha2",  0x02000000, 0x1000],
        ["pke",   0x03100000, 0x1000],
        ["sgx",   0x05100000, 0x10000],
        ["clcd",  0x0A100000, 0x10000],
        ["mipi",  0x09500000, 0x10000],
        ["armio", 0x3FB00000, 0x200000]
    ];

    A5.SYSREG_NAMES = {
        "0,1,0,0": "SCTLR",
        "0,1,0,1": "ACTLR",
        "0,1,0,2": "CPACR",
        "0,2,0,0": "TTBR0",
        "0,2,0,1": "TTBR1",
        "0,2,0,2": "TTBCR",
        "0,3,0,0": "DACR",
        "0,5,0,0": "DFSR",
        "0,5,0,1": "IFSR",
        "0,6,0,0": "FAR",
        "0,13,0,1": "CONTEXTIDR",
        "0,13,0,2": "TPIDRURO",
        "0,13,0,3": "TPIDRURW"
    };

    // -- boot_args (xnu-2107, ARM32) ------------------------------------
    A5.buildBootArgs = function (o) {
        o = o || {};
        var b = new Uint8Array(A5.BOOTARGS_SIZE);
        var dv = new DataView(b.buffer);
        dv.setUint16(0x00, o.revision !== undefined ? o.revision : 2, true);
        dv.setUint16(0x02, o.epoch !== undefined ? o.epoch : 0x12, true);
        dv.setUint32(0x04, (o.virtBase !== undefined ? o.virtBase : 0x80000000) >>> 0, true);
        dv.setUint32(0x08, (o.physBase !== undefined ? o.physBase : 0x40000000) >>> 0, true);
        dv.setUint32(0x0C, (o.memSize !== undefined ? o.memSize : 0x20000000) >>> 0, true);
        dv.setUint32(0x10, (o.topOfKernelData !== undefined ? o.topOfKernelData : A5.TOPOFKERNELDATA) >>> 0, true);
        // +0x14 .. +0x2B Video: zeros are acceptable
        dv.setUint32(0x2C, (o.machineType || 0) >>> 0, true);
        dv.setUint32(0x30, (o.deviceTreeP !== undefined ? o.deviceTreeP : A5.DT_VA) >>> 0, true);
        dv.setUint32(0x34, (o.deviceTreeLength !== undefined ? o.deviceTreeLength : A5.DT_LEN) >>> 0, true);

        var cmd = o.commandLine !== undefined ? o.commandLine : "-v";
        for (var i = 0; i < 255 && i < cmd.length; i++) b[0x38 + i] = cmd.charCodeAt(i) & 0xFF;
        b[0x38 + Math.min(255, cmd.length)] = 0;

        dv.setUint32(0x138, (o.bootFlags || 0) >>> 0, true);
        dv.setUint32(0x13C, (o.memSizeActual !== undefined ? o.memSizeActual
                                                          : (o.memSize !== undefined ? o.memSize : 0x20000000)) >>> 0, true);
        return b;
    };

    A5.parseBootArgs = function (b) {
        var dv = new DataView(b.buffer, b.byteOffset, b.byteLength);
        var cmd = "";
        for (var i = 0; i < 256; i++) {
            var c = b[0x38 + i];
            if (!c) break;
            cmd += String.fromCharCode(c);
        }
        return {
            revision: dv.getUint16(0x00, true),
            epoch: dv.getUint16(0x02, true),
            virtBase: dv.getUint32(0x04, true),
            physBase: dv.getUint32(0x08, true),
            memSize: dv.getUint32(0x0C, true),
            topOfKernelData: dv.getUint32(0x10, true),
            machineType: dv.getUint32(0x2C, true),
            deviceTreeP: dv.getUint32(0x30, true),
            deviceTreeLength: dv.getUint32(0x34, true),
            CommandLine: cmd,
            bootFlags: dv.getUint32(0x138, true),
            memSizeActual: dv.getUint32(0x13C, true)
        };
    };

    // -- Samsung UART (exynos register layout) --------------------------
    //
    // ULCON 0x00, UCON 0x04, UFCON 0x08, UTRSTAT 0x10, UTXH 0x20, URXH 0x24,
    // UBRDIV 0x28. UTRSTAT bit 1 = transmit buffer empty, bit 2 = transmitter
    // empty; both are always set so a kernel putc loop never blocks.
    function SamsungUart(name, sink) {
        this.name = name;
        this.sink = sink || null;
        this.tx = [];
        this.rx = [];
        this.regs = {};
    }
    SamsungUart.prototype.read = function (offset) {
        if (offset === 0x10) return 0x00000006 | (this.rx.length ? 0x01 : 0x00);
        if (offset === 0x24) return this.rx.length ? this.rx.shift() : 0x00;
        var v = this.regs[offset];
        return v === undefined ? 0 : v;
    };
    SamsungUart.prototype.write = function (offset, value) {
        if (offset === 0x20) {
            var c = value & 0xFF;
            this.tx.push(c);
            if (this.sink) this.sink(String.fromCharCode(c));
            return;
        }
        this.regs[offset] = value;
    };
    SamsungUart.prototype.text = function () {
        var s = "";
        for (var i = 0; i < this.tx.length; i++) s += String.fromCharCode(this.tx[i]);
        return s;
    };
    A5.SamsungUart = SamsungUart;

    // -- machine --------------------------------------------------------
    function A5Machine(opts) {
        opts = opts || {};
        this.consoleText = "";
        var self = this;

        this.bus = new A5.Bus({
            ramBegin: A5.RAM_BASE,
            ramEnd: (A5.RAM_BASE + A5.RAM_SIZE) >>> 0,
            autoMap: opts.autoMap !== false
        });
        this.bus.onFaultHook = function (kind, addr, size) {
            self._log({ kind: kind, addr: addr, size: size, value: 0 });
        };

        this.uarts = {};
        this.unimplemented = [];
        this.unimplementedTotal = 0;
        this.unimpDevices = {};
        this.stalled = null;
        this.fault = null;

        for (var i = 0; i < A5.MMIO_DEVICES.length; i++) {
            var d = A5.MMIO_DEVICES[i];
            var name = d[0], base = d[1], size = d[2];
            if (name.indexOf("uart") === 0) {
                var uart = new SamsungUart(name, function (ch) { self.consoleText += ch; });
                this.uarts[base] = uart;
                this._wire(name, base, size,
                    function (u) { return function (off, sz) { return u.read(off); }; }(uart),
                    function (u) { return function (off, sz, v) { u.write(off, v); }; }(uart));
            } else {
                this._wireUnimplemented(name, base, size);
            }
        }

        this.cpu = new A5.ArmCpu(this.bus, { stopAt: null });
        this.cpu.machine = this;

        // Address translation. Until this is attached the CPU's memory view is
        // the identity, and the boot sequence would still run -- the identity
        // section start() installs for its own PC makes translation a no-op
        // there -- but nothing would prove the tables it builds are correct.
        this.cpu.mmu = new A5.Mmu(this.cpu, this.bus);
        this.cpu.cp15Write(0, 3, 0, 0, A5.DACR_RESET);    // DACR
        this.cpu.cp15Write(0, 2, 0, 2, 0);                // TTBCR: N = 0, TTBR0 only
    }

    A5Machine.prototype._wire = function (name, base, size, read, write) {
        this.bus.addMMIO(name, base, size, read, write);
    };

    // A mapped-but-unmodelled block would silently absorb accesses into inert
    // RAM. Logging them is what makes the next step possible: the AIC's
    // register semantics have to be recovered from exactly this trail.
    A5Machine.prototype._wireUnimplemented = function (name, base, size) {
        var self = this;
        this.bus.addMMIO(name, base, size,
            function (off, sz) {
                self._log({ kind: "unimp-read:" + name, addr: off, size: sz, value: 0 });
                self.unimpDevices[name] = (self.unimpDevices[name] || 0) + 1;
                return 0;
            },
            function (off, sz, v) {
                self._log({ kind: "unimp-write:" + name, addr: off, size: sz, value: v >>> 0 });
                self.unimpDevices[name] = (self.unimpDevices[name] || 0) + 1;
            });
    };

    A5Machine.prototype._log = function (entry) {
        if (this.unimplemented.length < A5.MAX_UNIMPLEMENTED) this.unimplemented.push(entry);
        this.unimplementedTotal++;
    };

    A5Machine.prototype.writeBootArgs = function (args, pa) {
        pa = pa === undefined ? A5.BOOTARGS_PA : pa;
        this.bus.writeBytes(pa, args);
        return pa >>> 0;
    };

    A5Machine.prototype.write = function (addr, bytes) { this.bus.writeBytes(addr, bytes); };
    A5Machine.prototype.read32 = function (addr) { return this.bus.read32(addr) >>> 0; };
    A5Machine.prototype.write32 = function (addr, v) { this.bus.write32(addr, v); };

    A5Machine.prototype.console = function () { return this.consoleText; };

    A5Machine.prototype.sysreg = function (key) {
        var v = this.cpu.cp15[key];
        return v === undefined ? 0 : v >>> 0;
    };
    A5Machine.prototype.SCTLR = "0,1,0,0";
    A5Machine.prototype.TTBR0 = "0,2,0,0";
    A5Machine.prototype.TTBR1 = "0,2,0,1";
    A5Machine.prototype.TTBCR = "0,2,0,2";
    A5Machine.prototype.DACR  = "0,3,0,0";
    A5Machine.prototype.DFSR  = "0,5,0,0";
    A5Machine.prototype.IFSR  = "0,5,0,1";
    A5Machine.prototype.FAR   = "0,6,0,0";

    /** Walk the tables without touching the CPU, for inspection and tests. */
    A5Machine.prototype.translate = function (va, kind) {
        return this.cpu.mmu.translate(va >>> 0, kind || "read");
    };

    // Run until the CPU stops. The stop reasons that matter during bring-up:
    //   handoff                -> reached the target address (success)
    //   insn-invalid           -> UDF or an encoding Unicorn would reject too
    //   unimplemented-*        -> the model ran out of ARM; the address is the lead
    A5Machine.prototype.resume = function (opts) {
        opts = opts || {};
        this.stalled = null;
        this.cpu.resume(opts.stopAt);
    };

    A5Machine.prototype.run = function (opts) {
        opts = opts || {};
        if (opts.stopAt !== undefined && opts.stopAt !== null) this.cpu.stopAt = opts.stopAt >>> 0;
        var limit = opts.count || 1000000;
        var lastExcPc = -1, repeats = 0;
        while (!this.cpu.halted) {
            var before = this.cpu.r[15] >>> 0;
            this.cpu.step();
            var after = this.cpu.r[15] >>> 0;
            if (after === before && !this.cpu.halted) {
                // no progress and not halted: the same instruction faulted again
                if (after === lastExcPc) {
                    repeats++;
                    if (repeats >= A5.STALL_THRESHOLD) {
                        this.stalled = { pc: after, repeats: repeats };
                        this.cpu.halt("stalled", after);
                        break;
                    }
                } else { lastExcPc = after; repeats = 1; }
            }
            if (--limit <= 0) { this.cpu.halt("budget", after); break; }
        }
        return this.cpu.stopReason;
    };

    A5.A5Machine = A5Machine;

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
