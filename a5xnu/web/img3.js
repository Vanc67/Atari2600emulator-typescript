// Firmware container formats: IMG3 images and complzss-compressed payloads.
//
// Both are needed before any real image can be loaded: an iOS 6 IPSW contains
// IMG3-wrapped kernelcache, iBoot and devicetree files, and those are stored
// complzss-compressed. Nothing here decrypts anything -- the IMG3 SHSH/KBAG tags
// are parsed and reported, not acted on, because the keys are Apple's and no
// image is bundled or fetched.
//
// IMG3 layout is unambiguous and implemented exactly: a 20-byte header (magic,
// totalSize, dataSize, shshOffset, imgType) followed by a chain of 12-byte-header
// tags. Sizes are cross-checked against the actual buffer length, so a truncated
// or self-inconsistent file is rejected rather than read past its end.
//
// complzss is a different matter and the uncertainty is stated rather than
// papered over. The *header* is exact: 0x180 bytes, magic "complzss", then
// checksum, uncompressedSize, compressedSize. The LZSS ring parameters are also
// exact and mutually corroborating -- a 4096-byte ring filled with spaces, and
// the initial write position N - F = 4096 - 18 = 4078, which is where the
// documented "dictptr = 4078" comes from. What is NOT settled is the two-byte
// match encoding: implementations in the wild disagree on byte order (which of
// the two bytes carries the length nibble) and on whether the position field is
// an absolute ring index or a distance back from the current position. This
// file implements distance-back with the length in the high nibble of the first
// byte, and verifies it by round-tripping its own encoder.
//
// That round-trip proves self-consistency. It does not prove agreement with
// Apple's encoder, and no real compressed image is obtainable here to check
// against. If one ever is, `Complzss.decode` is the function to re-derive.

var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});

(function (A5) {
    "use strict";

    function u32(b, o) {
        return ((b[o] >>> 0) | (b[o + 1] << 8) | (b[o + 2] << 16) | (b[o + 3] << 24)) >>> 0;
    }
    function put32(b, o, v) {
        b[o] = v & 0xFF; b[o + 1] = (v >>> 8) & 0xFF;
        b[o + 2] = (v >>> 16) & 0xFF; b[o + 3] = (v >>> 24) & 0xFF;
    }
    function fourcc(v) {
        // IMG3 stores its codes little-endian, so 0x41544144 prints as "DATA".
        return String.fromCharCode(v & 0xFF, (v >>> 8) & 0xFF,
                                   (v >>> 16) & 0xFF, (v >>> 24) & 0xFF);
    }
    function cc(str) {
        var s = String(str);
        while (s.length < 4) s += "\u0000";
        return ((s.charCodeAt(0) >>> 0) | (s.charCodeAt(1) << 8) |
                (s.charCodeAt(2) << 16) | (s.charCodeAt(3) << 24)) >>> 0;
    }

    // =====================================================================
    // IMG3
    // =====================================================================
    A5.IMG3 = {
        MAGIC: cc("3gmI"),          // bytes on disk are '3','g','m','I'
        HEADER_SIZE: 20,
        TAG_HEADER_SIZE: 12,
        TAG: { DATA: cc("DATA"), SHSH: cc("SHSH"), CERT: cc("CERT"),
               KBAG: cc("KBAG"), TYPE: cc("TYPE"), EPOC: cc("EPOC"),
               CHIP: cc("CHIP"), BORD: cc("BORD"), SEPO: cc("SEPO"),
               ECID: cc("ECID"), VERS: cc("VERS"), SDOM: cc("SDOM"),
               OBJP: cc("OBJP"), SBTP: cc("SBTP") },
        TYPE: { ILLB: cc("illb"), KRNL: cc("krnl"), DTRE: cc("dtre"),
                LOGO: cc("logo"), RECO: cc("reco"), LLB2: cc("llb2") }
    };

    function Img3() {}

    /**
     * Parse an IMG3 image. Returns { ok, error, imgType, tags, ... } rather than
     * throwing, because the callers are tests and a UI that has to say what was
     * wrong with the file.
     */
    Img3.parse = function (bytes) {
        var b = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
        function bad(error) { return { ok: false, error: error }; }
        if (b.length < A5.IMG3.HEADER_SIZE) return bad("too short for an IMG3 header");
        var magic = u32(b, 0);
        if (magic !== A5.IMG3.MAGIC) {
            // The byte-swapped magic is what you get by reading a big-endian
            // field as little-endian; naming it makes the failure self-explaining
            // instead of reporting two unrecognisable numbers.
            var swapped = ((magic >>> 24) | ((magic >>> 8) & 0xFF00) |
                           ((magic << 8) & 0xFF0000) | ((magic << 24) >>> 0)) >>> 0;
            return bad("bad IMG3 magic 0x" + magic.toString(16) +
                       (swapped === A5.IMG3.MAGIC ? " (byte-swapped)" : ""));
        }
        var totalSize = u32(b, 4), dataSize = u32(b, 8);
        var shshOffset = u32(b, 12), imgType = u32(b, 16);
        if (totalSize > b.length) {
            return bad("header totalSize 0x" + totalSize.toString(16) +
                       " exceeds the 0x" + b.length.toString(16) + " bytes available");
        }
        if (dataSize > totalSize - A5.IMG3.HEADER_SIZE) {
            return bad("dataSize 0x" + dataSize.toString(16) +
                       " larger than the tag area");
        }
        var tags = [], off = A5.IMG3.HEADER_SIZE;
        var end = Math.min(totalSize, b.length);
        while (off + A5.IMG3.TAG_HEADER_SIZE <= end) {
            var tMagic = u32(b, off), tTotal = u32(b, off + 4), tData = u32(b, off + 8);
            if (tTotal < A5.IMG3.TAG_HEADER_SIZE) {
                return bad("tag 0x" + off.toString(16) + " totalSize " + tTotal +
                           " smaller than its own header");
            }
            if (off + tTotal > end) {
                return bad("tag '" + fourcc(tMagic) + "' at 0x" + off.toString(16) +
                           " runs past the end of the image");
            }
            if (tData > tTotal - A5.IMG3.TAG_HEADER_SIZE) {
                return bad("tag '" + fourcc(tMagic) + "' dataSize " + tData +
                           " larger than its payload");
            }
            var payload = b.slice(off + A5.IMG3.TAG_HEADER_SIZE,
                                  off + A5.IMG3.TAG_HEADER_SIZE + tData);
            tags.push({ magic: tMagic, name: fourcc(tMagic), offset: off,
                        totalSize: tTotal, dataSize: tData, data: payload });
            off += tTotal;
        }
        if (off !== end) {
            return bad("tag chain ends at 0x" + off.toString(16) +
                       " but the image ends at 0x" + end.toString(16));
        }
        var data = null, shsh = null, kbags = [];
        for (var i = 0; i < tags.length; i++) {
            if (tags[i].magic === A5.IMG3.TAG.DATA) data = tags[i];
            if (tags[i].magic === A5.IMG3.TAG.SHSH) shsh = tags[i];
            if (tags[i].magic === A5.IMG3.TAG.KBAG) kbags.push(tags[i]);
        }
        return {
            ok: true, imgType: imgType, imgTypeName: fourcc(imgType),
            totalSize: totalSize, dataSize: dataSize, shshOffset: shshOffset,
            tags: tags, dataTag: data, data: data ? data.data : null,
            shshTag: shsh, kbagTags: kbags,
            // Reported, never used: a KBAG holds an IV and a key wrapped by the
            // device GID, and this codebase does not have and does not seek it.
            encrypted: kbags.length > 0
        };
    };

    /** Build a synthetic IMG3 image. Tags are written in the order given. */
    Img3.build = function (opts) {
        opts = opts || {};
        var imgType = typeof opts.imgType === "string" ? cc(opts.imgType)
                      : (opts.imgType >>> 0);
        var specs = opts.tags || [];
        var tagBytes = [];
        var total = A5.IMG3.HEADER_SIZE;
        for (var i = 0; i < specs.length; i++) {
            var d = specs[i].data instanceof Uint8Array ? specs[i].data
                  : new Uint8Array(specs[i].data || []);
            var t = new Uint8Array(A5.IMG3.TAG_HEADER_SIZE + d.length);
            put32(t, 0, typeof specs[i].magic === "string" ? cc(specs[i].magic)
                                                            : specs[i].magic >>> 0);
            put32(t, 4, t.length);
            put32(t, 8, d.length);
            t.set(d, A5.IMG3.TAG_HEADER_SIZE);
            tagBytes.push(t);
            total += t.length;
        }
        var shshOffset = 0, off = A5.IMG3.HEADER_SIZE;
        for (var j = 0; j < tagBytes.length; j++) {
            if (u32(tagBytes[j], 0) === A5.IMG3.TAG.SHSH) {
                // shshOffset is measured from the start of the header.
                shshOffset = off - A5.IMG3.HEADER_SIZE;
                break;
            }
            off += tagBytes[j].length;
        }
        var out = new Uint8Array(total);
        put32(out, 0, A5.IMG3.MAGIC);
        put32(out, 4, total);
        put32(out, 8, total - A5.IMG3.HEADER_SIZE);
        put32(out, 12, shshOffset);
        put32(out, 16, imgType);
        var w = A5.IMG3.HEADER_SIZE;
        for (var k = 0; k < tagBytes.length; k++) { out.set(tagBytes[k], w); w += tagBytes[k].length; }
        return out;
    };

    Img3.fourcc = fourcc;
    Img3.cc = cc;
    A5.Img3 = Img3;

    // =====================================================================
    // complzss
    // =====================================================================
    A5.COMPLZSS = {
        MAGIC: "complzss",
        HEADER_SIZE: 0x180,
        // Okumura's parameters, which the ring initialisation corroborates:
        // 4096 - 18 = 4078 is exactly the documented starting write position.
        N: 4096, F: 18, THRESHOLD: 2, DICT_START: 4078, FILL: 0x20
    };

    function Complzss() {}

    Complzss.parseHeader = function (bytes) {
        var b = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
        var M = A5.COMPLZSS.MAGIC;
        if (b.length < A5.COMPLZSS.HEADER_SIZE) {
            return { ok: false, error: "shorter than the 0x180-byte header" };
        }
        var magic = "";
        for (var i = 0; i < M.length; i++) magic += String.fromCharCode(b[i]);
        if (magic !== M) {
            return { ok: false, error: "bad magic '" + magic.replace(/[^\x20-\x7E]/g, ".") + "'" };
        }
        return {
            ok: true, magic: magic,
            checksum: u32(b, 8),
            uncompressedSize: u32(b, 12),
            compressedSize: u32(b, 16),
            word20: u32(b, 20), word24: u32(b, 24), word28: u32(b, 28)
        };
    };

    Complzss.buildHeader = function (uncompressedSize, compressedSize, checksum) {
        var out = new Uint8Array(A5.COMPLZSS.HEADER_SIZE);
        var M = A5.COMPLZSS.MAGIC;
        for (var i = 0; i < M.length; i++) out[i] = M.charCodeAt(i);
        put32(out, 8, (checksum >>> 0) || 0);
        put32(out, 12, uncompressedSize >>> 0);
        put32(out, 16, compressedSize >>> 0);
        put32(out, 20, 1);
        put32(out, 28, 1);
        return out;
    };

    function newDict() {
        var d = new Uint8Array(A5.COMPLZSS.N);
        for (var i = 0; i < A5.COMPLZSS.N; i++) d[i] = A5.COMPLZSS.FILL;
        return d;
    }

    /**
     * Decode a complzss payload. `distanceBack` selects the match-position
     * convention; see the note at the top of this file for why that is a
     * parameter rather than a fact.
     */
    Complzss.decode = function (comp, uncompressedSize, opts) {
        opts = opts || {};
        var N = A5.COMPLZSS.N;
        var dict = newDict();
        var dictPtr = A5.COMPLZSS.DICT_START;
        var out = new Uint8Array(uncompressedSize);
        var o = 0, i = 0;
        while (o < uncompressedSize) {
            if (i >= comp.length) throw new Error("complzss input exhausted at " + o +
                                                  " of " + uncompressedSize + " bytes");
            var flags = comp[i++];
            for (var bit = 0; bit < 8 && o < uncompressedSize; bit++) {
                if (flags & 0x80) {
                    // Literal.
                    if (i >= comp.length) throw new Error("complzss input exhausted mid-literal");
                    var c = comp[i++];
                    out[o++] = c;
                    dict[dictPtr] = c;
                    dictPtr = (dictPtr + 1) & (N - 1);
                } else {
                    // Match: first byte high nibble = length - 3, remaining 12
                    // bits = position. The high nibble is what Okumura's own
                    // encoder emits for length, which is why this is the default.
                    if (i + 1 >= comp.length) throw new Error("complzss input exhausted mid-match");
                    var b1 = comp[i++], b2 = comp[i++];
                    var len = (b1 >>> 4) + 3;
                    var field = ((b1 & 0x0F) << 8) | b2;
                    var src;
                    if (opts.absolutePosition) {
                        src = field;
                    } else {
                        // distance back from the current ring position
                        src = (dictPtr - field - 1) & (N - 1);
                    }
                    for (var k = 0; k < len && o < uncompressedSize; k++) {
                        var v = dict[(src + k) & (N - 1)];
                        out[o++] = v;
                        dict[dictPtr] = v;
                        dictPtr = (dictPtr + 1) & (N - 1);
                    }
                }
                flags = (flags << 1) & 0xFF;
            }
        }
        return out;
    };

    /** Encode with the same conventions decode() uses, so the pair round-trips. */
    Complzss.encode = function (src, opts) {
        opts = opts || {};
        var N = A5.COMPLZSS.N, F = A5.COMPLZSS.F, TH = A5.COMPLZSS.THRESHOLD;
        var dict = newDict();
        var dictPtr = A5.COMPLZSS.DICT_START;
        var written = 0;              // total bytes placed in the ring so far
        var out = [];
        var i = 0;
        while (i < src.length) {
            var flags = 0, payload = [];
            for (var bit = 0; bit < 8 && i < src.length; bit++) {
                var bestLen = 0, bestBack = 0;
                // Two constraints, both of which a naive search gets wrong:
                //
                // 1. Never look further back than the bytes actually written.
                //    The ring starts filled with spaces, so an unbounded search
                //    "matches" against that padding: encoding text containing
                //    ".0 " found a 3-byte match two bytes back whose third byte
                //    was a not-yet-written space slot, and the decoder -- which
                //    by then had written the real byte there -- produced
                //    ".0." instead. Random data hides this, since a run of three
                //    spaces is unlikely.
                //
                // 2. Handle matches that overlap the write position. When
                //    back <= l the candidate slot has not been written yet, and
                //    the decoder will fill it from the match itself as it copies
                //    byte by byte, so the expected value is src[i + l - back].
                //    This is what lets LZSS run-length-encode a long repeat with
                //    a short self-referencing match.
                var maxBack = Math.min(N - 1, written);
                for (var back = 1; back <= maxBack; back++) {
                    var p = (dictPtr - back) & (N - 1);
                    var l = 0;
                    while (l < F && i + l < src.length) {
                        var want = (l < back) ? dict[(p + l) & (N - 1)]
                                              : src[i + (l - back)];
                        if (want !== src[i + l]) break;
                        l++;
                    }
                    if (l > bestLen) { bestLen = l; bestBack = back; }
                }
                if (bestLen > TH) {
                    // flag bit 0 = match; the position field is back - 1 so that
                    // a one-byte distance encodes as 0 and fits in 12 bits.
                    var field = opts.absolutePosition
                        ? ((dictPtr - bestBack) & (N - 1)) : (bestBack - 1);
                    payload.push(((bestLen - 3) << 4) | ((field >>> 8) & 0x0F));
                    payload.push(field & 0xFF);
                    for (var k = 0; k < bestLen; k++) {
                        dict[dictPtr] = src[i + k];
                        dictPtr = (dictPtr + 1) & (N - 1);
                        written++;
                    }
                    i += bestLen;
                } else {
                    flags |= (0x80 >>> bit);
                    payload.push(src[i]);
                    dict[dictPtr] = src[i];
                    dictPtr = (dictPtr + 1) & (N - 1);
                    written++;
                    i++;
                }
            }
            out.push(flags);
            for (var q = 0; q < payload.length; q++) out.push(payload[q]);
        }
        return new Uint8Array(out);
    };

    /** Wrap an uncompressed buffer into a complete complzss file. */
    Complzss.pack = function (src, opts) {
        var body = Complzss.encode(src, opts);
        var head = Complzss.buildHeader(src.length, body.length, 0);
        var out = new Uint8Array(head.length + body.length);
        out.set(head, 0);
        out.set(body, head.length);
        return out;
    };

    Complzss.unpack = function (file, opts) {
        var h = Complzss.parseHeader(file);
        if (!h.ok) return h;
        var body = file.slice(A5.COMPLZSS.HEADER_SIZE);
        return { ok: true, header: h, data: Complzss.decode(body, h.uncompressedSize, opts) };
    };

    A5.Complzss = Complzss;

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
