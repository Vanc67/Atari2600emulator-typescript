"""Minimal JVM class-file reader (Java 6 class files, major 50).

Only what the droid toolchain needs: constant pool, fields, methods, the Code
attribute and its exception table.  Everything else is skipped.
"""

import struct

TAG_UTF8 = 1
TAG_INT = 3
TAG_FLOAT = 4
TAG_LONG = 5
TAG_DOUBLE = 6
TAG_CLASS = 7
TAG_STRING = 8
TAG_FIELDREF = 9
TAG_METHODREF = 10
TAG_IMETHODREF = 11
TAG_NAMEANDTYPE = 12
TAG_METHODHANDLE = 15
TAG_METHODTYPE = 16
TAG_DYNAMIC = 17
TAG_INVOKEDYNAMIC = 18
TAG_MODULE = 19
TAG_PACKAGE = 20


class ClassFormatError(Exception):
    pass


class Reader(object):
    __slots__ = ("b", "p")

    def __init__(self, b, p=0):
        self.b = b
        self.p = p

    def u1(self):
        v = self.b[self.p]
        self.p += 1
        return v

    def u2(self):
        v = (self.b[self.p] << 8) | self.b[self.p + 1]
        self.p += 2
        return v

    def u4(self):
        v = struct.unpack_from(">I", self.b, self.p)[0]
        self.p += 4
        return v

    def take(self, n):
        v = self.b[self.p:self.p + n]
        self.p += n
        return v


class Attr(object):
    __slots__ = ("name", "data")

    def __init__(self, name, data):
        self.name = name
        self.data = data

    def __repr__(self):
        return "<Attr %s %d>" % (self.name, len(self.data))


class Member(object):
    """A field or a method."""

    def __init__(self):
        self.access = 0
        self.name = ""
        self.desc = ""
        self.attrs = {}
        # code
        self.max_stack = 0
        self.max_locals = 0
        self.code = b""
        self.exceptions = []      # [(start_pc, end_pc, handler_pc, catch_type)]
        self.exc_types = {}       # catch_type index -> class name


class ClassFile(object):
    def __init__(self, data):
        self.data = data
        self.cp = [None]
        self.fields = []
        self.methods = []
        self.interfaces = []
        self.attrs = {}
        self._parse()
        self._resolve()

    # -- constant pool ----------------------------------------------------
    def utf8(self, i):
        e = self.cp[i]
        if e is None or e[0] != TAG_UTF8:
            raise ClassFormatError("constant %d is not Utf8" % i)
        return e[1]

    def class_name(self, i):
        e = self.cp[i]
        if e is None or e[0] != TAG_CLASS:
            raise ClassFormatError("constant %d is not Class" % i)
        return self.utf8(e[1])

    def ref(self, i):
        """Methodref/Fieldref -> (owner, name, desc, kind)."""
        e = self.cp[i]
        if e is None:
            raise ClassFormatError("bad constant %d" % i)
        tag = e[0]
        if tag not in (TAG_FIELDREF, TAG_METHODREF, TAG_IMETHODREF):
            raise ClassFormatError("constant %d is not a ref (tag %d)" % (i, tag))
        owner_idx, nat = e[1], e[2]
        name, desc = self.utf8(self.cp[nat][1]), self.utf8(self.cp[nat][2])
        return self.class_name(owner_idx), name, desc, tag

    # -- parsing ----------------------------------------------------------
    def _parse(self):
        r = Reader(self.data)
        if r.u4() != 0xCAFEBABE:
            raise ClassFormatError("not a class file")
        r.u2()          # minor
        major = r.u2()
        if major > 52:
            raise ClassFormatError("class file version %d too new" % major)
        n = r.u2()
        cp = self.cp
        i = 1
        while i < n:
            tag = r.u1()
            if tag == TAG_UTF8:
                ln = r.u2()
                raw = r.take(ln)
                cp.append((TAG_UTF8, raw.decode("utf-8", "replace")))
            elif tag == TAG_INT:
                cp.append((TAG_INT, struct.unpack_from(">i", r.b, r.p)[0]))
                r.p += 4
            elif tag == TAG_FLOAT:
                cp.append((TAG_FLOAT, struct.unpack_from(">f", r.b, r.p)[0]))
                r.p += 4
            elif tag == TAG_LONG:
                cp.append((TAG_LONG, struct.unpack_from(">q", r.b, r.p)[0]))
                r.p += 8
                cp.append(None)
                i += 1
            elif tag == TAG_DOUBLE:
                cp.append((TAG_DOUBLE, struct.unpack_from(">d", r.b, r.p)[0]))
                r.p += 8
                cp.append(None)
                i += 1
            elif tag == TAG_CLASS:
                cp.append((TAG_CLASS, None))
                cp[i] = (TAG_CLASS, r.u2())
            elif tag == TAG_STRING:
                cp.append((TAG_STRING, r.u2()))
            elif tag in (TAG_FIELDREF, TAG_METHODREF, TAG_IMETHODREF):
                cp.append((tag, r.u2(), r.u2()))
            elif tag == TAG_NAMEANDTYPE:
                cp.append((TAG_NAMEANDTYPE, r.u2(), r.u2()))
            elif tag == TAG_METHODHANDLE:
                cp.append((tag, r.u1(), r.u2()))
            elif tag in (TAG_METHODTYPE, TAG_MODULE, TAG_PACKAGE):
                cp.append((tag, r.u2()))
            elif tag in (TAG_DYNAMIC, TAG_INVOKEDYNAMIC):
                cp.append((tag, r.u2(), r.u2()))
            else:
                raise ClassFormatError("unknown constant tag %d at %d" % (tag, i))
            i += 1
        self.access = r.u2()
        self.name = None                        # resolved in _resolve
        self.this_idx = r.u2()
        self.super_idx = r.u2()
        for _ in range(r.u2()):
            self.interfaces.append(r.u2())
        self.fields = self._members(r)
        self.methods = self._members(r)
        self.attrs = self._attrs(r)

    def _members(self, r):
        out = []
        for _ in range(r.u2()):
            m = Member()
            m.access = r.u2()
            m.name = self.utf8(r.u2())
            m.desc = self.utf8(r.u2())
            m.attrs = self._attrs(r)
            code = m.attrs.get("Code")
            if code is not None:
                self._code(m, code.data)
            out.append(m)
        return out

    def _attrs(self, r):
        out = {}
        for _ in range(r.u2()):
            name = self.utf8(r.u2())
            ln = r.u4()
            out[name] = Attr(name, r.take(ln))
        return out

    def _code(self, m, data):
        r = Reader(data)
        m.max_stack = r.u2()
        m.max_locals = r.u2()
        ln = r.u4()
        m.code = r.take(ln)
        for _ in range(r.u2()):
            start = r.u2()
            end = r.u2()
            handler = r.u2()
            ctype = r.u2()
            m.exceptions.append((start, end, handler, ctype))
        # nested attributes (LineNumberTable, LocalVariableTable, StackMapTable)
        m.attrs["Code.attrs"] = self._attrs(Reader(r.b, r.p))

    def _resolve(self):
        self.name = self.class_name(self.this_idx)
        self.super_name = self.class_name(self.super_idx) if self.super_idx else None
        self.iface_names = [self.class_name(i) for i in self.interfaces]
        for m in self.fields + self.methods:
            for (start, end, handler, ctype) in m.exceptions:
                if ctype:
                    m.exc_types[ctype] = self.class_name(ctype)
        # resolve ConstantValue attributes to real literals
        for f in self.fields:
            cv = f.attrs.get("ConstantValue")
            if cv is not None:
                idx = struct.unpack_from(">H", cv.data, 0)[0]
                f.attrs["value"] = self.cp[idx]


def read_file(path):
    with open(path, "rb") as fh:
        return ClassFile(fh.read())
