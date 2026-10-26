// Firmware preparation: Mach-O segment loading.
//
// Ported field for field from a5xnu/machine.py so both implementations parse
// the same bytes the same way. The point of having this at all is that a
// kernelcache cannot be loaded by copying the file to one address: the segments
// have to go where their vmaddr says, and the parts of vmsize that the file does
// not cover have to stay zero.
//
// No firmware is included, fetched or implied. These routines are exercised
// against synthetic fixtures built in the tests, which is the most that can be
// verified here -- no iOS kernelcache, DeviceTree or IPSW is obtainable from
// this environment, and the only Mach-O structures that matter for parsing are
// public ABI.

var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});

(function (A5) {
    "use strict";

    A5.MH_MAGIC    = 0xFEEDFACE;
    A5.MH_MAGIC_64 = 0xFEEDFACF;
    // 0xCEFAEDFE is the byte-swapped 32-bit magic (MH_CIGAM). It is a valid
    // Mach-O on a big-endian host, but a kernelcache for the A5 is little-endian,
    // so it has to be rejected rather than silently byte-swapped.
    A5.MH_CIGAM    = 0xCEFAEDFE;
    A5.LC_SEGMENT   = 0x01;
    A5.LC_SEGMENT_64 = 0x19;

    // sizeof(mach_header_32) == 28 and sizeof(mach_header_64) == 32 (it adds a
    // 4-byte reserved field). sizeof(segment_command) == 56 exactly, and
    // segment_command_64 == 72. These are not round numbers and guessing them
    // off by four misparses every load command after the first.
    A5.MACH_HEADER_32_SIZE = 28;
    A5.MACH_HEADER_64_SIZE = 32;

    function MachOLoadError(message) {
        this.name = "MachOLoadError";
        this.message = message;
    }
    MachOLoadError.prototype = Object.create(Error.prototype);
    MachOLoadError.prototype.constructor = MachOLoadError;
    A5.MachOLoadError = MachOLoadError;

    function u16(data, off) { return data[off] | (data[off + 1] << 8); }
    function u32(data, off) {
        return ((data[off] | (data[off + 1] << 8) |
                 (data[off + 2] << 16) | (data[off + 3] << 24)) >>> 0);
    }
    function u64lo(data, off) {
        // Segment addresses in a 64-bit Mach-O are 64-bit, but an armv7
        // kernelcache never uses the high half. Reading the low word is enough
        // and avoids pretending to model 64-bit addresses.
        return u32(data, off);
    }
    function cstr(data, off, max) {
        var out = "";
        for (var i = 0; i < max; i++) {
            var c = data[off + i];
            if (!c) break;
            out += String.fromCharCode(c);
        }
        return out;
    }

    /**
     * Parse every LC_SEGMENT / LC_SEGMENT_64 and return
     * [{cmd, segname, vmaddr, vmsize, fileoff, filesize, bytes}].
     *
     * `bytes` is the file content for the segment, which may be shorter than
     * vmsize -- the remainder is __bss and must stay zero.
     */
    A5.loadMachoSegments = function (data) {
        if (!data || data.length < 32) throw new MachOLoadError("too short to be a Mach-O");
        var magic = u32(data, 0);
        var is64;
        if (magic === A5.MH_MAGIC) is64 = false;
        else if (magic === A5.MH_MAGIC_64) is64 = true;
        else if (magic === A5.MH_CIGAM) {
            throw new MachOLoadError(
                "byte-swapped Mach-O magic 0xCEFAEDFE: an A5 kernelcache is little-endian");
        } else {
            throw new MachOLoadError(
                "bad Mach-O magic 0x" + ("00000000" + magic.toString(16)).slice(-8) +
                " (not a kernelcache)");
        }

        var ncmds = u32(data, 16);
        var off = is64 ? A5.MACH_HEADER_64_SIZE : A5.MACH_HEADER_32_SIZE;
        var out = [];
        for (var i = 0; i < ncmds; i++) {
            if (off + 8 > data.length) break;
            var cmd = u32(data, off);
            var cmdsize = u32(data, off + 4);
            if (cmdsize === 0 || off + cmdsize > data.length) break;
            if (cmd === A5.LC_SEGMENT || cmd === A5.LC_SEGMENT_64) {
                var segname = cstr(data, off + 8, 16);
                var vmaddr, vmsize, fileoff, filesize;
                if (cmd === A5.LC_SEGMENT) {
                    vmaddr   = u32(data, off + 24);
                    vmsize   = u32(data, off + 28);
                    fileoff  = u32(data, off + 32);
                    filesize = u32(data, off + 36);
                } else {
                    vmaddr   = u64lo(data, off + 24);
                    vmsize   = u64lo(data, off + 32);
                    fileoff  = u64lo(data, off + 40);
                    filesize = u64lo(data, off + 48);
                }
                var chunk = new Uint8Array(0);
                if (filesize && fileoff + filesize <= data.length) {
                    chunk = data.subarray(fileoff, fileoff + filesize);
                }
                if (vmsize) {
                    out.push({
                        cmd: cmd, segname: segname,
                        vmaddr: vmaddr >>> 0, vmsize: vmsize >>> 0,
                        fileoff: fileoff >>> 0, filesize: filesize >>> 0,
                        bytes: chunk
                    });
                }
            }
            off += cmdsize;
        }
        return out;
    };

    /**
     * Write each segment at its own physical address. A Mach-O vmaddr in an
     * armv7 kernelcache IS the kernel virtual address, and this runs before the
     * MMU exists, so the conversion is va - virtBase + physBase.
     */
    A5.loadMacho = function (machine, data, opts) {
        opts = opts || {};
        var virtBase = opts.virtBase !== undefined ? opts.virtBase : 0x80000000;
        var physBase = opts.physBase !== undefined ? opts.physBase : 0x40000000;
        var segs = A5.loadMachoSegments(data);
        var loaded = [];
        for (var i = 0; i < segs.length; i++) {
            var s = segs[i];
            var pa = (s.vmaddr - virtBase + physBase) >>> 0;
            // Only the file-backed part is written. The gap up to vmsize is
            // __bss/__common and must remain zero, which is the whole reason
            // segments are loaded individually.
            machine.write(pa, s.bytes);
            loaded.push({
                segname: s.segname, va: s.vmaddr >>> 0, pa: pa,
                vmsize: s.vmsize, filesize: s.bytes.length
            });
        }
        return loaded;
    };

    /**
     * Build a synthetic 32-bit Mach-O for tests. Kept here rather than in the
     * test file so both the JavaScript and (via the equivalent Python fixture)
     * the Unicorn side describe the same structure.
     */
    A5.buildFakeMacho = function (segments) {
        var cmds = [];
        var body = [];
        var dataOff = A5.MACH_HEADER_32_SIZE;
        var i, s;
        for (i = 0; i < segments.length; i++) {
            s = segments[i];
            dataOff += 56;
        }
        var fileOff = dataOff;
        for (i = 0; i < segments.length; i++) {
            s = segments[i];
            var c = new Uint8Array(56);
            var dv = new DataView(c.buffer);
            dv.setUint32(0, A5.LC_SEGMENT, true);
            dv.setUint32(4, 56, true);
            for (var k = 0; k < s.segname.length && k < 16; k++) c[8 + k] = s.segname.charCodeAt(k);
            dv.setUint32(24, s.vmaddr >>> 0, true);
            dv.setUint32(28, s.vmsize >>> 0, true);
            dv.setUint32(32, s.fileoff !== undefined ? s.fileoff : fileOff, true);
            dv.setUint32(36, s.bytes ? s.bytes.length : 0, true);
            dv.setUint32(40, 7, true);            // maxprot
            dv.setUint32(44, s.initprot !== undefined ? s.initprot : 7, true);
            dv.setUint32(48, 0, true);            // nsects
            dv.setUint32(52, 0, true);            // flags
            cmds.push(c);
            if (s.bytes && s.bytes.length) body.push(s.bytes);
            fileOff += s.bytes ? s.bytes.length : 0;
        }
        var total = dataOff;
        for (i = 0; i < body.length; i++) total += body[i].length;
        var out = new Uint8Array(total);
        var hdv = new DataView(out.buffer);
        hdv.setUint32(0, A5.MH_MAGIC, true);
        hdv.setUint32(4, 12, true);               // CPU_TYPE_ARM
        hdv.setUint32(8, 9, true);                // CPU_SUBTYPE_ARM_V7
        hdv.setUint32(12, 2, true);               // MH_KEXT_BUNDLE-ish; value is not parsed
        hdv.setUint32(16, segments.length, true); // ncmds
        hdv.setUint32(20, cmds.length * 56, true);// sizeofcmds
        hdv.setUint32(24, 0, true);               // flags
        var p = A5.MACH_HEADER_32_SIZE;
        for (i = 0; i < cmds.length; i++) { out.set(cmds[i], p); p += 56; }
        for (i = 0; i < body.length; i++) { out.set(body[i], p); p += body[i].length; }
        return out;
    };

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
