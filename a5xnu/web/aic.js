// Apple Interrupt Controller v1 and the Cortex-A9 private timer / watchdog.
//
// The AIC was previously one of the ~25 blocks that k93ap fills with
// create_unimplemented_device, and Linux's irq-apple-aic.c was assumed to be no
// help because it is written for AIC v2/v3. It is: the version 1 branch of
// aic_init() derives every offset arithmetically, and reading that branch gives
// the whole v1 map. Offsets below are taken from
// drivers/irqchip/irq-apple-aic.c, case 1, not guessed:
//
//   off = start_off = AIC_TARGET_CPU (0x3000)
//   off += 4 * max_irq, max_irq = AIC_MAX_IRQ = 0x400   -> 0x4000
//   sw_set   = 0x4000, each block is 4 * (max_irq >> 5) = 128 bytes
//   sw_clr   = 0x4080
//   mask_set = 0x4100
//   mask_clr = 0x4180
//   hw_state = 0x4200
//   die_stride = 0x1280
//
// which fits inside the 0x10000 window k93ap declares at 0x0F200000.
//
// The one semantic that is inferred rather than read off a constant:
// aic_handle_irq() reads AIC_EVENT in a `do { ... } while (event)` loop, so a
// read has to consume the event, or that loop would never terminate. Reporting
// the lowest-numbered pending IRQ first is an assumption -- nothing consulted
// here states a priority order.
//
// The Cortex-A9 private timer and watchdog are the standard MPCore private
// memory region peripherals (QEMU models them in a9mpcore_priv). Their register
// semantics are public and implemented here faithfully. Their *placement* is a
// harness choice, not a claim: k93ap declares no timer block at all, and no
// source consulted here establishes the A5's PERIPHBASE, so this device is not
// wired into the board map by default and tests attach it explicitly.

var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});

(function (A5) {
    "use strict";

    // ---- AIC v1 register map -------------------------------------------
    A5.AIC = {
        INFO:        0x0004,
        CONFIG:      0x0010,
        WHOAMI:      0x2000,
        EVENT:       0x2004,
        IPI_SEND:    0x2008,
        IPI_ACK:     0x200C,
        IPI_MASK_SET:0x2024,
        IPI_MASK_CLR:0x2028,
        TARGET_CPU:  0x3000,
        SW_SET:      0x4000,
        SW_CLR:      0x4080,
        MASK_SET:    0x4100,
        MASK_CLR:    0x4180,
        HW_STATE:    0x4200,
        MAX_IRQ:     0x400,
        DIE_STRIDE:  0x1280,
        BLOCK_BYTES: 128,          // 4 * (MAX_IRQ >> 5)
        EVENT_TYPE_FIQ: 0,
        EVENT_TYPE_IRQ: 1,
        EVENT_TYPE_IPI: 4
    };

    function Aic(opts) {
        opts = opts || {};
        // AIC_INFO reports how many interrupt lines this instance actually has.
        // The A5's value is not published anywhere consulted here; the S5L89xx
        // family exposes 32 lines in the low block, and TARGET_CPU is still
        // sized for the architectural maximum.
        this.nrIrq = opts.nrIrq !== undefined ? opts.nrIrq : 32;
        this.maxIrq = A5.AIC.MAX_IRQ;
        var words = this.maxIrq >> 5;
        this.targetCpu = new Int32Array(this.maxIrq);
        this.swSet  = new Int32Array(words);
        // All lines masked at reset. The hardware reset value for AIC v1 is not
        // published in any source consulted here, but this is the state Linux's
        // driver leaves it in after masking everything at probe, and starting
        // unmasked would let a modelled device interrupt code that has not
        // installed a vector yet.
        this.mask   = new Int32Array(words);
        this.mask.fill(-1);
        this.hw     = new Int32Array(words);
        this.whoami = opts.whoami || 0;
        this.ipiPending = 0;
        this.ipiMask = 0;
        this.config = 0;
        this.eventReads = 0;
        this.eventsDelivered = [];
        this.maxLog = 512;
    }

    Aic.prototype._w = function (irq) { return irq >> 5; };
    Aic.prototype._b = function (irq) { return (1 << (irq & 31)) | 0; };

    /** Set a hardware interrupt line, the way a device would. */
    Aic.prototype.raise = function (irq) {
        if (irq < 0 || irq >= this.maxIrq) return;
        this.hw[this._w(irq)] = (this.hw[this._w(irq)] | this._b(irq)) | 0;
    };
    Aic.prototype.lower = function (irq) {
        if (irq < 0 || irq >= this.maxIrq) return;
        this.hw[this._w(irq)] = (this.hw[this._w(irq)] & ~this._b(irq)) | 0;
    };

    Aic.prototype._pendingWord = function (i) {
        return ((this.swSet[i] | this.hw[i]) & ~this.mask[i]) | 0;
    };

    /** Lowest-numbered unmasked pending IRQ, or -1. */
    Aic.prototype.highestPriority = function () {
        for (var i = 0; i < this.swSet.length; i++) {
            var w = this._pendingWord(i);
            if (!w) continue;
            var bit = w & (-w);                 // isolate lowest set bit
            var n = 0;
            while (bit > 1) { bit >>>= 1; n++; }
            return (i << 5) + n;
        }
        return -1;
    };

    Aic.prototype.anyPending = function () {
        for (var i = 0; i < this.swSet.length; i++) if (this._pendingWord(i)) return true;
        return false;
    };

    // The CPU-facing interrupt lines. AIC v1 has no separate FIQ grouping in the
    // register map; FIQ sources on Apple parts come from IMP-DEF sysregs that
    // the consulted sources do not cover, so nothing here asserts FIQ on its own.
    Aic.prototype.irqPending = function () {
        if (this.ipiPending & ~this.ipiMask) return true;
        return this.anyPending();
    };
    Aic.prototype.fiqPending = function () { return false; };

    Aic.prototype.read = function (off, size) {
        var R = A5.AIC;
        if (off === R.INFO) return (this.nrIrq & 0xFFFF) >>> 0;
        if (off === R.CONFIG) return this.config >>> 0;
        if (off === R.WHOAMI) return this.whoami >>> 0;
        if (off === R.EVENT) {
            this.eventReads++;
            // An IPI outranks a device IRQ only if no IRQ is pending; the driver
            // treats type 4 with num 1 as the IPI case.
            if (this.ipiPending & ~this.ipiMask) {
                var ipiBit = (this.ipiPending & ~this.ipiMask);
                this.ipiPending = 0;                       // consumed by the read
                return ((R.EVENT_TYPE_IPI << 16) | (ipiBit === (1 << 31) ? 2 : 1)) >>> 0;
            }
            var irq = this.highestPriority();
            if (irq < 0) return 0;
            // Consuming the event is what makes the driver's read-until-zero
            // loop terminate. Only the software-generated bit is cleared here:
            // a hardware line stays asserted until the device clears its own
            // interrupt status, and re-raises on the next tick.
            this.swSet[this._w(irq)] = (this.swSet[this._w(irq)] & ~this._b(irq)) | 0;
            this.hw[this._w(irq)] = (this.hw[this._w(irq)] & ~this._b(irq)) | 0;
            if (this.eventsDelivered.length < this.maxLog) this.eventsDelivered.push(irq);
            return ((R.EVENT_TYPE_IRQ << 16) | (irq & 0xFFFF)) >>> 0;
        }
        if (off >= R.TARGET_CPU && off < R.TARGET_CPU + this.maxIrq * 4) {
            return this.targetCpu[(off - R.TARGET_CPU) >> 2] >>> 0;
        }
        if (off >= R.SW_SET && off < R.SW_SET + R.BLOCK_BYTES) {
            return this.swSet[(off - R.SW_SET) >> 2] >>> 0;
        }
        if (off >= R.MASK_SET && off < R.MASK_SET + R.BLOCK_BYTES) {
            return this.mask[(off - R.MASK_SET) >> 2] >>> 0;
        }
        if (off >= R.HW_STATE && off < R.HW_STATE + R.BLOCK_BYTES) {
            return this.hw[(off - R.HW_STATE) >> 2] >>> 0;
        }
        return null;                                     // not a register we model
    };

    Aic.prototype.write = function (off, size, v) {
        var R = A5.AIC;
        v >>>= 0;
        if (off === R.CONFIG) { this.config = v; return true; }
        if (off === R.IPI_SEND) { this.ipiPending = (this.ipiPending | v) | 0; return true; }
        if (off === R.IPI_ACK) { this.ipiPending = (this.ipiPending & ~v) | 0; return true; }
        if (off === R.IPI_MASK_SET) { this.ipiMask = (this.ipiMask | v) | 0; return true; }
        if (off === R.IPI_MASK_CLR) { this.ipiMask = (this.ipiMask & ~v) | 0; return true; }
        if (off === R.EVENT || off === R.INFO || off === R.WHOAMI) return true;  // read-only
        if (off >= R.TARGET_CPU && off < R.TARGET_CPU + this.maxIrq * 4) {
            this.targetCpu[(off - R.TARGET_CPU) >> 2] = v | 0; return true;
        }
        if (off >= R.SW_SET && off < R.SW_SET + R.BLOCK_BYTES) {
            var i = (off - R.SW_SET) >> 2;
            this.swSet[i] = (this.swSet[i] | v) | 0; return true;
        }
        if (off >= R.SW_CLR && off < R.SW_CLR + R.BLOCK_BYTES) {
            var j = (off - R.SW_CLR) >> 2;
            this.swSet[j] = (this.swSet[j] & ~v) | 0; return true;
        }
        if (off >= R.MASK_SET && off < R.MASK_SET + R.BLOCK_BYTES) {
            var k = (off - R.MASK_SET) >> 2;
            this.mask[k] = (this.mask[k] | v) | 0; return true;
        }
        if (off >= R.MASK_CLR && off < R.MASK_CLR + R.BLOCK_BYTES) {
            var l = (off - R.MASK_CLR) >> 2;
            this.mask[l] = (this.mask[l] & ~v) | 0; return true;
        }
        return false;                                    // not a register we model
    };

    A5.Aic = Aic;

    // ---- Cortex-A9 private timer and watchdog --------------------------
    A5.PRIVTIMER = {
        LOAD: 0x00, COUNTER: 0x04, CONTROL: 0x08, INT_STATUS: 0x0C,
        WDOG_LOAD: 0x20, WDOG_COUNTER: 0x24, WDOG_CONTROL: 0x28,
        WDOG_INT_STATUS: 0x2C, WDOG_DISABLE: 0x30,
        CTRL_ENABLE: 1, CTRL_AUTO_RELOAD: 2, CTRL_IRQ_ENABLE: 4,
        WDOG_CTRL_ENABLE: 1, WDOG_CTRL_AUTO_RELOAD: 2, WDOG_CTRL_IRQ_ENABLE: 4,
        WDOG_CTRL_RESET_ENABLE: 8
    };

    /**
     * Counts instructions rather than cycles: this model has no clock, and a
     * cycle count would have to be invented. The ratio is fixed and documented
     * so a test can predict exactly when the line asserts.
     */
    function A9PrivateTimer(opts) {
        opts = opts || {};
        var T = A5.PRIVTIMER;
        this.load = 0; this.counter = 0; this.control = 0; this.intStatus = 0;
        this.wdogLoad = 0; this.wdogCounter = 0; this.wdogControl = 0; this.wdogIntStatus = 0;
        this.irq = opts.irq !== undefined ? opts.irq : -1;
        this.intc = opts.intc || null;
        this.expiries = 0;
        this.wdogExpiries = 0;
        this.wdogResetRequests = 0;
    }

    A9PrivateTimer.prototype._syncIrq = function () {
        if (!this.intc || this.irq < 0) return;
        var asserted = ((this.control & A5.PRIVTIMER.CTRL_IRQ_ENABLE) !== 0 &&
                        (this.intStatus & 1) !== 0);
        if (asserted) this.intc.raise(this.irq); else this.intc.lower(this.irq);
    };

    A9PrivateTimer.prototype.tick = function (n) {
        n = n || 1;
        var T = A5.PRIVTIMER;
        if ((this.control & T.CTRL_ENABLE) && this.counter > 0) {
            this.counter -= n;
            if (this.counter <= 0) {
                this.counter = 0;
                this.intStatus |= 1;
                this.expiries++;
                if (this.control & T.CTRL_AUTO_RELOAD) this.counter = this.load;
            }
        }
        if ((this.wdogControl & T.WDOG_CTRL_ENABLE) && this.wdogCounter > 0) {
            this.wdogCounter -= n;
            if (this.wdogCounter <= 0) {
                this.wdogCounter = 0;
                this.wdogIntStatus |= 1;
                this.wdogExpiries++;
                if (this.wdogControl & T.WDOG_CTRL_RESET_ENABLE) {
                    // A real SoC resets here. Silently resetting the model would
                    // destroy the evidence of why, so the request is recorded and
                    // the harness decides.
                    this.wdogResetRequests++;
                }
                if (this.wdogControl & T.WDOG_CTRL_AUTO_RELOAD) this.wdogCounter = this.wdogLoad;
            }
        }
        this._syncIrq();
    };

    A9PrivateTimer.prototype.read = function (off, size) {
        var T = A5.PRIVTIMER;
        switch (off) {
            case T.LOAD: return this.load >>> 0;
            case T.COUNTER: return this.counter >>> 0;
            case T.CONTROL: return this.control >>> 0;
            case T.INT_STATUS: return this.intStatus >>> 0;
            case T.WDOG_LOAD: return this.wdogLoad >>> 0;
            case T.WDOG_COUNTER: return this.wdogCounter >>> 0;
            case T.WDOG_CONTROL: return this.wdogControl >>> 0;
            case T.WDOG_INT_STATUS: return this.wdogIntStatus >>> 0;
            default: return null;
        }
    };

    A9PrivateTimer.prototype.write = function (off, size, v) {
        var T = A5.PRIVTIMER;
        v >>>= 0;
        switch (off) {
            case T.LOAD:
                this.load = v;
                // Writing the load register also loads the counter, which is how
                // a driver programs the period.
                this.counter = v;
                this.intStatus &= ~1;
                break;
            case T.COUNTER: this.counter = v; break;
            case T.CONTROL: this.control = v & 0xFF; break;
            case T.INT_STATUS: this.intStatus &= ~v; break;      // write 1 to clear
            case T.WDOG_LOAD: this.wdogLoad = v; this.wdogCounter = v; this.wdogIntStatus &= ~1; break;
            case T.WDOG_COUNTER: this.wdogCounter = v; break;
            case T.WDOG_CONTROL: this.wdogControl = v & 0xFF; break;
            case T.WDOG_INT_STATUS: this.wdogIntStatus &= ~v; break;
            case T.WDOG_DISABLE:
                // The watchdog is only disabled by this exact magic sequence, so
                // that a stray write cannot turn it off.
                if (v === 0x12345678) { this.wdogControl = 0; this.wdogCounter = 0; }
                break;
            default: return false;
        }
        this._syncIrq();
        return true;
    };

    A5.A9PrivateTimer = A9PrivateTimer;

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
