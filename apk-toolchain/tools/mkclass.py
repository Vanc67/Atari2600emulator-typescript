#!/usr/bin/env python3
"""
mkclass.py - a minimal Java .class file writer.

Why this exists
---------------
Building the Android wrapper for this project needs one Java class
(a WebView Activity). The sandbox has a Java *runtime* (jdk4py) plus the
Android build-tools (aapt2 / d8 / apksigner / zipalign), but no `javac`
and no reachable Maven Central / dl.google.com, and every ECJ jar on
GitHub is stored in Git LFS (media.githubusercontent.com is not
reachable from here).

So instead of compiling Java we *emit* the class file directly. Class
files are a well specified binary format, and d8 happily consumes them.

We emit major version 49 (Java 5) on purpose: class files below 50 are
verified by type inference, so no StackMapTable attribute is required.
That removes by far the most error-prone part of hand-writing bytecode.

Only what the wrapper needs is implemented: a constant pool, fields,
methods with a Code attribute, and a small instruction emitter with
two-pass label resolution.
"""

import struct

# --------------------------------------------------------------------------
# Constant pool
# --------------------------------------------------------------------------

TAG_UTF8 = 1
TAG_INTEGER = 3
TAG_CLASS = 7
TAG_STRING = 8
TAG_FIELDREF = 9
TAG_METHODREF = 10
TAG_IMETHODREF = 11
TAG_NAMETYPE = 12


class ConstantPool:
    def __init__(self):
        self._payload = []
        self._index = {}

    def _intern(self, key, payload):
        if key in self._index:
            return self._index[key]
        self._payload.append(payload)
        idx = len(self._payload)  # constant pool entries are 1-based
        self._index[key] = idx
        return idx

    def utf8(self, s):
        b = s.encode("utf-8")
        return self._intern(("utf8", s), bytes([TAG_UTF8]) + struct.pack(">H", len(b)) + b)

    def cls(self, name):
        return self._intern(("class", name), bytes([TAG_CLASS]) + struct.pack(">H", self.utf8(name)))

    def name_and_type(self, name, desc):
        return self._intern(
            ("nat", name, desc),
            bytes([TAG_NAMETYPE]) + struct.pack(">HH", self.utf8(name), self.utf8(desc)),
        )

    def method(self, owner, name, desc):
        return self._intern(
            ("method", owner, name, desc),
            bytes([TAG_METHODREF]) + struct.pack(">HH", self.cls(owner), self.name_and_type(name, desc)),
        )

    def iface_method(self, owner, name, desc):
        return self._intern(
            ("imethod", owner, name, desc),
            bytes([TAG_IMETHODREF]) + struct.pack(">HH", self.cls(owner), self.name_and_type(name, desc)),
        )

    def field(self, owner, name, desc):
        return self._intern(
            ("field", owner, name, desc),
            bytes([TAG_FIELDREF]) + struct.pack(">HH", self.cls(owner), self.name_and_type(name, desc)),
        )

    def string(self, s):
        return self._intern(("string", s), bytes([TAG_STRING]) + struct.pack(">H", self.utf8(s)))

    def integer(self, v):
        return self._intern(("int", v), bytes([TAG_INTEGER]) + struct.pack(">i", v))

    def pack(self):
        # constant_pool_count is (number of entries + 1)
        return struct.pack(">H", len(self._payload) + 1) + b"".join(self._payload)


# --------------------------------------------------------------------------
# Bytecode emitter
# --------------------------------------------------------------------------

ACC_PUBLIC = 0x0001
ACC_PRIVATE = 0x0002
ACC_PROTECTED = 0x0004
ACC_STATIC = 0x0008
ACC_FINAL = 0x0010
ACC_SUPER = 0x0020


class Code:
    """Instruction buffer with label support."""

    def __init__(self):
        self.b = bytearray()
        self.labels = {}
        self._patches = []  # (offset_in_buffer, label, origin)

    # -- raw helpers -------------------------------------------------------
    def _u1(self, v):
        self.b.append(v & 0xFF)

    def _u2(self, v):
        self.b += struct.pack(">H", v & 0xFFFF)

    def _s2(self, v):
        self.b += struct.pack(">h", v)

    # -- label handling ----------------------------------------------------
    def label(self, name):
        self.labels[name] = len(self.b)
        return self

    def _branch(self, opcode, label):
        self.b.append(opcode)
        self._patches.append((len(self.b), label))
        self._s2(0)  # placeholder, fixed up in build()

    # -- constants / locals ------------------------------------------------
    def nop(self):
        return self._u1(0x00) or self

    def aconst_null(self):
        self._u1(0x01)
        return self

    def iconst(self, n):
        if n == -1:
            self._u1(0x02)  # iconst_m1
        elif 0 <= n <= 5:
            self._u1(0x03 + n)  # iconst_0 .. iconst_5
        elif -128 <= n <= 127:
            self._u1(0x10)
            self._u1(n)
        elif -32768 <= n <= 32767:
            self._u1(0x11)
            self._s2(n)
        else:
            raise ValueError("iconst: value %d needs ldc" % n)
        return self

    def ldc(self, cp_index):
        if cp_index <= 255:
            self._u1(0x12)
            self._u1(cp_index)
        else:
            self._u1(0x13)
            self._u2(cp_index)
        return self

    def aload(self, n):
        if n <= 3:
            self._u1(0x2A + n)
        else:
            self._u1(0x19)
            self._u1(n)
        return self

    def astore(self, n):
        if n <= 3:
            self._u1(0x4B + n)
        else:
            self._u1(0x3A)
            self._u1(n)
        return self

    def iload(self, n):
        if n <= 3:
            self._u1(0x1A + n)
        else:
            self._u1(0x15)
            self._u1(n)
        return self

    def istore(self, n):
        if n <= 3:
            self._u1(0x3B + n)
        else:
            self._u1(0x36)
            self._u1(n)
        return self

    # -- stack -------------------------------------------------------------
    def pop(self):
        self._u1(0x57)
        return self

    def dup(self):
        self._u1(0x59)
        return self

    # -- control flow ------------------------------------------------------
    def ifnull(self, label):
        self._branch(0xC6, label)
        return self

    def ifnonnull(self, label):
        self._branch(0xC7, label)
        return self

    def ifeq(self, label):
        self._branch(0x99, label)
        return self

    def ifne(self, label):
        self._branch(0x9A, label)
        return self

    def goto(self, label):
        self._branch(0xA7, label)
        return self

    def _return(self, opcode):
        self._u1(opcode)
        return self

    def return_(self):
        return self._return(0xB1)

    def areturn(self):
        return self._return(0xB0)

    def ireturn(self):
        return self._return(0xAC)

    # -- fields / objects / calls ------------------------------------------
    def new(self, cp_class):
        self._u1(0xBB)
        self._u2(cp_class)
        return self

    def checkcast(self, cp_class):
        self._u1(0xC0)
        self._u2(cp_class)
        return self

    def getfield(self, cp_field):
        self._u1(0xB4)
        self._u2(cp_field)
        return self

    def putfield(self, cp_field):
        self._u1(0xB5)
        self._u2(cp_field)
        return self

    def getstatic(self, cp_field):
        self._u1(0xB2)
        self._u2(cp_field)
        return self

    def putstatic(self, cp_field):
        self._u1(0xB3)
        self._u2(cp_field)
        return self

    def invokevirtual(self, cp_method):
        self._u1(0xB6)
        self._u2(cp_method)
        return self

    def invokespecial(self, cp_method):
        self._u1(0xB7)
        self._u2(cp_method)
        return self

    def invokestatic(self, cp_method):
        self._u1(0xB8)
        self._u2(cp_method)
        return self

    def invokeinterface(self, cp_method, argcount):
        self._u1(0xB9)
        self._u2(cp_method)
        self._u1(argcount)
        self._u1(0)
        return self

    # -- finalisation ------------------------------------------------------
    def build(self):
        out = bytearray(self.b)
        for offset, label in self._patches:
            if label not in self.labels:
                raise KeyError("undefined label %r" % label)
            # branch offsets are relative to the opcode itself
            rel = self.labels[label] - (offset - 1)
            struct.pack_into(">h", out, offset, rel)
        return bytes(out)


# --------------------------------------------------------------------------
# Class assembly
# --------------------------------------------------------------------------


def _attr(cp, name, payload):
    return struct.pack(">HI", cp.utf8(name), len(payload)) + payload


def code_attribute(cp, code_bytes, max_stack=8, max_locals=4):
    payload = struct.pack(">HHI", max_stack, max_locals, len(code_bytes))
    payload += code_bytes
    payload += struct.pack(">H", 0)  # exception_table_length
    payload += struct.pack(">H", 0)  # attributes_count
    return _attr(cp, "Code", payload)


def build_class(
    this_name,
    super_name,
    cp,
    fields=(),
    methods=(),
    access_flags=ACC_PUBLIC | ACC_SUPER,
    interfaces=(),
    source_file=None,
):
    """fields:  (access, name, desc)
    methods: (access, name, desc, code_bytes, max_stack, max_locals)
    """
    # NOTE: the constant pool must be serialised *last*. Building the field,
    # method and attribute tables interns new Utf8 entries, and packing the
    # pool before that would leave those indices dangling.
    head = struct.pack(">IHH", 0xCAFEBABE, 0, 49)  # magic, minor, major (Java 5)

    body = bytearray()
    body += struct.pack(">H", access_flags)
    body += struct.pack(">H", cp.cls(this_name))
    body += struct.pack(">H", cp.cls(super_name))
    body += struct.pack(">H", len(interfaces))
    for i in interfaces:
        body += struct.pack(">H", cp.cls(i))

    body += struct.pack(">H", len(fields))
    for access, name, desc in fields:
        body += struct.pack(">HHH", access, cp.utf8(name), cp.utf8(desc))
        body += struct.pack(">H", 0)  # no field attributes

    body += struct.pack(">H", len(methods))
    for access, name, desc, code, max_stack, max_locals in methods:
        body += struct.pack(">HHH", access, cp.utf8(name), cp.utf8(desc))
        body += struct.pack(">H", 1)
        body += code_attribute(cp, code, max_stack, max_locals)

    attrs = 0
    tail = bytearray()
    if source_file:
        tail += _attr(cp, "SourceFile", struct.pack(">H", cp.utf8(source_file)))
        attrs += 1
    body += struct.pack(">H", attrs)
    body += tail

    # header, then the (now complete) constant pool, then everything else
    return head + cp.pack() + bytes(body)
