#!/usr/bin/env python3
"""Compile the CLDC/MIDP API *stubs* (signature-only .java files) into class
files without a JDK.

Why this exists
---------------
The game is compiled for CLDC 1.1 / MIDP 2.0.  javac needs a boot class path
with the J2ME API on it, but a desktop JDK only ships its own (much newer)
java.base; javac refuses to read those class files when asked for old targets
and the J2ME API is not part of a desktop JDK at all.

Instead of shipping a J2ME SDK we ship the API as plain .java sources under
``stubs/`` and turn them into class files here.  The generated classes are
never packaged into the final jar - they are only used as the compiler's
boot class path (and by the desktop harness / decompiler for type lookups).

The parser handles exactly the subset used by ``stubs/``:
    package / import
    public [final|abstract] class X [extends Y] [implements A, B] { ... }
    public interface X [extends A] { ... }
    fields  (with ConstantValue for compile time constants)
    methods / constructors (with a minimal body and an Exceptions attribute)

Usage:
    python3 tools/gen_stub_classes.py --src stubs --out build/stub-classes
"""

import argparse
import os
import struct
import sys

# ---------------------------------------------------------------------------
#  constant pool
# ---------------------------------------------------------------------------

ACC_PUBLIC = 0x0001
ACC_PRIVATE = 0x0002
ACC_PROTECTED = 0x0004
ACC_STATIC = 0x0008
ACC_FINAL = 0x0010
ACC_SUPER = 0x0020
ACC_SYNCHRONIZED = 0x0020
ACC_VOLATILE = 0x0040
ACC_TRANSIENT = 0x0080
ACC_NATIVE = 0x0100
ACC_INTERFACE = 0x0200
ACC_ABSTRACT = 0x0400

CLASS_VERSION_MAJOR = 46  # Java 1.2, accepted by every CLDC 1.1 device
CLASS_VERSION_MINOR = 0

PRIMITIVES = {
    'void': 'V', 'boolean': 'Z', 'byte': 'B', 'char': 'C',
    'short': 'S', 'int': 'I', 'long': 'J', 'float': 'F', 'double': 'D',
}


class Pool(object):
    def __init__(self):
        self.entries = []          # list of bytes (each a complete cp_info)
        self.index = {}            # key -> index
        self.next_index = 1

    def _add(self, key, raw, slots=1):
        if key in self.index:
            return self.index[key]
        idx = self.next_index
        self.entries.append(raw)
        self.index[key] = idx
        self.next_index += slots
        return idx

    def utf8(self, s):
        data = s.encode('utf-8')
        raw = struct.pack('>BH', 1, len(data)) + data
        return self._add(('utf8', s), raw)

    def cls(self, name):
        n = self.utf8(name)
        return self._add(('class', name), struct.pack('>BH', 7, n))

    def string(self, s):
        n = self.utf8(s)
        return self._add(('string', s), struct.pack('>BH', 8, n))

    def integer(self, v):
        return self._add(('int', v), struct.pack('>Bi', 3, v))

    def long(self, v):
        return self._add(('long', v), struct.pack('>Bq', 5, v), slots=2)

    def double(self, v):
        return self._add(('double', v), struct.pack('>Bd', 6, v), slots=2)

    def float(self, v):
        return self._add(('float', v), struct.pack('>Bf', 4, v))

    def nat(self, name, desc):
        n = self.utf8(name)
        d = self.utf8(desc)
        return self._add(('nat', name, desc), struct.pack('>BHH', 12, n, d))

    def fieldref(self, cls, name, desc):
        c = self.cls(cls)
        nt = self.nat(name, desc)
        return self._add(('field', cls, name, desc),
                         struct.pack('>BHH', 9, c, nt))

    def methodref(self, cls, name, desc):
        c = self.cls(cls)
        nt = self.nat(name, desc)
        return self._add(('method', cls, name, desc),
                         struct.pack('>BHH', 10, c, nt))

    def dump(self):
        return struct.pack('>H', self.next_index) + b''.join(self.entries)


# ---------------------------------------------------------------------------
#  tiny tokenizer for the stub subset
# ---------------------------------------------------------------------------

PUNCT = set('{}();,=<>[].-+*/!&|?~:')


def tokenize(text):
    toks = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c in ' \t\r\n':
            i += 1
            continue
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            j = text.find('\n', i)
            i = n if j < 0 else j + 1
            continue
        if c == '/' and i + 1 < n and text[i + 1] == '*':
            j = text.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if c == '"':
            j = i + 1
            buf = []
            while j < n and text[j] != '"':
                if text[j] == '\\':
                    nxt = text[j + 1]
                    if nxt == 'n':
                        buf.append('\n')
                    elif nxt == 't':
                        buf.append('\t')
                    elif nxt == 'r':
                        buf.append('\r')
                    elif nxt == '0':
                        buf.append('\0')
                    elif nxt == '\\':
                        buf.append('\\')
                    elif nxt == '"':
                        buf.append('"')
                    else:
                        buf.append(nxt)
                    j += 2
                    continue
                buf.append(text[j])
                j += 1
            toks.append(('str', ''.join(buf)))
            i = j + 1
            continue
        if c == "'":
            j = i + 1
            if text[j] == '\\':
                nxt = text[j + 1]
                val = {'n': '\n', 't': '\t', 'r': '\r', '0': '\0',
                       '\\': '\\', "'": "'"}.get(nxt, nxt)
                j += 2
            else:
                val = text[j]
                j += 1
            toks.append(('char', val))
            i = j + 1
            continue
        if c.isdigit():
            j = i
            while j < n and (text[j].isalnum() or text[j] in '._'):
                j += 1
            toks.append(('num', text[i:j]))
            i = j
            continue
        if c.isalpha() or c in '_$':
            j = i
            while j < n and (text[j].isalnum() or text[j] in '_$'):
                j += 1
            toks.append(('id', text[i:j]))
            i = j
            continue
        if text.startswith('...', i):
            toks.append(('p', '...'))
            i += 3
            continue
        if c in PUNCT:
            toks.append(('p', c))
            i += 1
            continue
        i += 1
    return toks


class Source(object):
    def __init__(self, path):
        self.path = path
        self.toks = tokenize(open(path, encoding='utf-8').read())
        self.pos = 0

    def peek(self, k=0):
        p = self.pos + k
        return self.toks[p] if p < len(self.toks) else ('eof', '')

    def next(self):
        t = self.peek()
        self.pos += 1
        return t

    def expect(self, kind, val=None):
        t = self.next()
        if t[0] != kind or (val is not None and t[1] != val):
            raise SyntaxError('%s: expected %r %r, got %r'
                              % (self.path, kind, val, t))
        return t

    def accept(self, kind, val=None):
        t = self.peek()
        if t[0] == kind and (val is None or t[1] == val):
            self.pos += 1
            return t
        return None

    def skip_balanced(self, open_ch, close_ch):
        # the caller has already consumed the opening bracket
        depth = 1
        while True:
            t = self.next()
            if t[0] == 'eof':
                raise SyntaxError('%s: unbalanced %r' % (self.path, open_ch))
            if t[0] == 'p' and t[1] == open_ch:
                depth += 1
            elif t[0] == 'p' and t[1] == close_ch:
                depth -= 1
                if depth == 0:
                    return


# ---------------------------------------------------------------------------
#  parser
# ---------------------------------------------------------------------------

MODIFIERS = {
    'public': ACC_PUBLIC, 'private': ACC_PRIVATE, 'protected': ACC_PROTECTED,
    'static': ACC_STATIC, 'final': ACC_FINAL, 'abstract': ACC_ABSTRACT,
    'native': ACC_NATIVE, 'synchronized': ACC_SYNCHRONIZED,
    'transient': ACC_TRANSIENT, 'volatile': ACC_VOLATILE,
}


class Member(object):
    def __init__(self):
        self.name = ''
        self.type_tokens = []
        self.params = []          # list of token lists (one per parameter)
        self.throws = []
        self.flags = 0
        self.constant = None      # ('int'|'long'|'str', value)


class TypeDecl(object):
    def __init__(self):
        self.package = ''
        self.imports = {}
        self.wildcards = []
        self.name = ''
        self.access = 0
        self.is_interface = False
        self.super_name = None
        self.interfaces = []
        self.fields = []
        self.methods = []


def parse_file(path):
    src = Source(path)
    decl = TypeDecl()

    if src.accept('id', 'package'):
        parts = []
        while True:
            t = src.next()
            if t[0] == 'id':
                parts.append(t[1])
            elif t[0] == 'p' and t[1] == '.':
                continue
            elif t[0] == 'p' and t[1] == ';':
                break
        decl.package = '.'.join(parts)

    while src.accept('id', 'import'):
        parts = []
        while True:
            t = src.next()
            if t[0] == 'id':
                parts.append(t[1])
            elif t[0] == 'p' and t[1] == '.':
                continue
            elif t[0] == 'p' and t[1] == '*':
                parts.append('*')
            elif t[0] == 'p' and t[1] == ';':
                break
        full = '.'.join(parts)
        if full.endswith('.*'):
            decl.wildcards.append(full[:-2])
        else:
            decl.imports[full.split('.')[-1]] = full

    # modifiers
    access = 0
    while True:
        t = src.peek()
        if t[0] == 'id' and t[1] in MODIFIERS:
            access |= MODIFIERS[t[1]]
            src.next()
            continue
        break

    is_interface = False
    if src.accept('id', 'interface'):
        is_interface = True
        access |= ACC_INTERFACE | ACC_ABSTRACT
    elif src.accept('id', 'class'):
        pass
    else:
        raise SyntaxError('%s: expected class/interface, got %r'
                          % (path, src.peek()))

    decl.is_interface = is_interface
    decl.access = access
    decl.name = src.expect('id')[1]

    if src.accept('id', 'extends'):
        names = [src.expect('id')[1]]
        while src.accept('p', '.'):
            names.append(src.expect('id')[1])
        # interfaces use extends for their super-interfaces
        if is_interface:
            decl.interfaces.append('.'.join(names))
        else:
            decl.super_name = '.'.join(names)
    if src.accept('id', 'implements'):
        while True:
            names = [src.expect('id')[1]]
            while src.accept('p', '.'):
                names.append(src.expect('id')[1])
            decl.interfaces.append('.'.join(names))
            if not src.accept('p', ','):
                break

    src.expect('p', '{')
    parse_members(src, decl)
    return decl


def strip_param_name(tokens):
    """Drop the trailing parameter name (``int[] rgb`` -> ``int[]``)."""
    toks = [t for t in tokens if not (t[0] == 'id' and t[1] == 'final')]
    if len(toks) >= 2 and toks[-1][0] == 'id' and toks[-2][0] != 'p':
        return toks[:-1]
    if len(toks) >= 3 and toks[-1][0] == 'id':
        return toks[:-1]
    return toks


def parse_members(src, decl):
    while True:
        t = src.peek()
        if t[0] == 'p' and t[1] == '}':
            src.next()
            return
        if t[0] == 'eof':
            return
        if t[0] == 'p' and t[1] == ';':
            src.next()
            continue

        m = Member()
        while True:
            t = src.peek()
            if t[0] == 'id' and t[1] in MODIFIERS:
                m.flags |= MODIFIERS[t[1]]
                src.next()
                continue
            break

        # nested types are skipped (the stubs do not use them)
        t = src.peek()
        if t[0] == 'id' and t[1] in ('class', 'interface'):
            src.next()
            src.expect('id')
            while not (src.peek()[0] == 'p' and src.peek()[1] == '{'):
                src.next()
            src.expect('p', '{')
            src.skip_balanced('{', '}')
            continue

        # collect the declaration head up to '(' '=' ';' or '{'
        head = []
        kind = None
        while True:
            t = src.peek()
            if t[0] == 'p' and t[1] in '(=;':
                kind = t[1]
                break
            head.append(src.next())

        if kind == '(':
            src.next()
            # name is the last identifier of the head
            name = head[-1][1]
            m.name = name
            m.type_tokens = head[:-1]
            # parameters
            cur = []
            depth = 0
            while True:
                t = src.peek()
                if t[0] == 'p' and t[1] == '(':
                    depth += 1
                    cur.append(src.next())
                    continue
                if t[0] == 'p' and t[1] == ')':
                    if depth == 0:
                        src.next()
                        break
                    depth -= 1
                    cur.append(src.next())
                    continue
                if t[0] == 'p' and t[1] == ',' and depth == 0:
                    src.next()
                    m.params.append(strip_param_name(cur))
                    cur = []
                    continue
                cur.append(src.next())
            if cur:
                m.params.append(strip_param_name(cur))
            if src.accept('id', 'throws'):
                while True:
                    names = [src.expect('id')[1]]
                    while src.accept('p', '.'):
                        names.append(src.expect('id')[1])
                    m.throws.append('.'.join(names))
                    if not src.accept('p', ','):
                        break
            if src.peek()[0] == 'p' and src.peek()[1] == '{':
                src.next()
                src.skip_balanced('{', '}')
            else:
                src.expect('p', ';')
                m.flags |= ACC_ABSTRACT
            decl.methods.append(m)
            continue

        if kind == '=':
            # field with initialiser: name is last id of head
            src.next()
            m.name = head[-1][1]
            m.type_tokens = head[:-1]
            lit = src.next()
            if lit[0] == 'p' and lit[1] == '-':
                lit = src.next()
                lit = (lit[0], '-' + lit[1])
            if lit[0] == 'char':
                m.constant = ('int', ord(lit[1]))
            elif lit[0] == 'str':
                m.constant = ('str', lit[1])
            elif lit[0] == 'id' and lit[1] in ('true', 'false'):
                m.constant = ('int', 1 if lit[1] == 'true' else 0)
            elif lit[0] == 'id' and lit[1] != 'null':
                # e.g. `new Foo()` or another name: no ConstantValue
                while not (src.peek()[0] == 'p' and src.peek()[1] == ';'):
                    src.next()
                src.next()
                decl.fields.append(m)
                continue
            elif lit[0] == 'num':
                txt = lit[1]
                if txt.lower().endswith('l'):
                    m.constant = ('long', int(txt[:-1], 0))
                elif txt.lower().endswith('d'):
                    m.constant = ('double', float(txt[:-1]))
                elif txt.lower().endswith('f'):
                    m.constant = ('float', float(txt[:-1]))
                elif '.' in txt or 'e' in txt.lower():
                    m.constant = ('double', float(txt))
                else:
                    m.constant = ('int', int(txt, 0))
            if not (src.peek()[0] == 'p' and src.peek()[1] == ';'):
                # compound initialiser - keep the field, drop the constant
                m.constant = None
                depth = 0
                while True:
                    t = src.peek()
                    if t[0] == 'p' and t[1] in '({[':
                        depth += 1
                    elif t[0] == 'p' and t[1] in ')}]':
                        depth -= 1
                    elif t[0] == 'p' and t[1] == ';' and depth == 0:
                        src.next()
                        break
                    src.next()
            else:
                src.next()
            decl.fields.append(m)
            continue

        if kind == ';':
            src.next()
            m.name = head[-1][1]
            m.type_tokens = head[:-1]
            decl.fields.append(m)
            continue


# ---------------------------------------------------------------------------
#  descriptors
# ---------------------------------------------------------------------------

# well known unqualified names used inside wildcard imports
KNOWN_CLASSES = {
    'PrintStream': 'java/io/PrintStream',
    'InputStream': 'java/io/InputStream',
    'OutputStream': 'java/io/OutputStream',
    'DataInputStream': 'java/io/DataInputStream',
    'DataOutputStream': 'java/io/DataOutputStream',
    'FileConnection': 'javax/microedition/io/file/FileConnection',
    'Connection': 'javax/microedition/io/Connection',
    'StreamConnection': 'javax/microedition/io/StreamConnection',
    'Connector': 'javax/microedition/io/Connector',
    'IOException': 'java/io/IOException',
    'Vector': 'java/util/Vector',
    'Hashtable': 'java/util/Hashtable',
    'Enumeration': 'java/util/Enumeration',
    'Random': 'java/util/Random',
}


def type_descriptor(tokens, decl):
    """tokens -> (descriptor, is_primitive_void)"""
    dims = 0
    name_tokens = []
    i = 0
    while i < len(tokens):
        t = tokens[i]
        if t[0] == 'p' and t[1] == '[':
            dims += 1
            i += 1
            continue
        if t[0] == 'p' and t[1] == ']':
            i += 1
            continue
        if t[0] == 'p' and t[1] == '.':
            name_tokens.append('.')
            i += 1
            continue
        if t[0] == 'id':
            name_tokens.append(t[1])
        i += 1
    name = ''.join(name_tokens)
    if not name:
        raise SyntaxError('empty type in %s' % decl.name)
    base = PRIMITIVES.get(name)
    if base is None:
        internal = resolve_class_name(name, decl)
        base = 'L' + internal + ';'
    return '[' * dims + base


def method_descriptor(m, decl):
    params = ''
    for p in m.params:
        # ignore 'final' on parameters
        toks = [t for t in p if not (t[0] == 'id' and t[1] == 'final')]
        params += type_descriptor(toks, decl)
    ret = type_descriptor(m.type_tokens, decl) if m.type_tokens else 'V'
    return '(' + params + ')' + ret


KNOWN_DECLS = set()   # "pkg/Name" of every stub type, filled by main()


def resolve_class_name(name, decl):
    """Dotted/simple class name -> internal (slash) name.

    Resolution order follows javac closely enough for the stubs: an explicit
    import wins, then the current package, then java.lang, then wildcard
    imports.
    """
    if '.' in name:
        return name.replace('.', '/')
    if name in decl.imports:
        return decl.imports[name].replace('.', '/')
    if decl.package:
        same_pkg = decl.package.replace('.', '/') + '/' + name
        if same_pkg in KNOWN_DECLS:
            return same_pkg
    if 'java/lang/' + name in KNOWN_DECLS:
        return 'java/lang/' + name
    if name in KNOWN_CLASSES and KNOWN_CLASSES[name] in KNOWN_DECLS:
        return KNOWN_CLASSES[name]
    for pkg in decl.wildcards:
        cand = pkg.replace('.', '/') + '/' + name
        if cand in KNOWN_DECLS:
            return cand
    if decl.package:
        return decl.package.replace('.', '/') + '/' + name
    return name


def slot_count(desc, is_static):
    """max_locals for a method descriptor"""
    n = 0 if is_static else 1
    i = 1
    while i < len(desc) - 1:
        c = desc[i]
        if c == '[':
            n += 1
            while desc[i] == '[':
                i += 1
            if desc[i] == 'L':
                i = desc.index(';', i)
            i += 1
            continue
        if c == 'L':
            i = desc.index(';', i) + 1
            n += 1
            continue
        n += 2 if c in 'JD' else 1
        i += 1
    return max(n, 1)


# ---------------------------------------------------------------------------
#  class file writer
# ---------------------------------------------------------------------------

def default_return_op(desc):
    ret = desc.split(')')[-1]
    return {'V': 0xb1, 'I': 0xac, 'Z': 0xac, 'B': 0xac, 'C': 0xac,
            'S': 0xac, 'J': 0xad, 'F': 0xae, 'D': 0xaf}.get(ret, 0xb0)


def build_class(decl):
    pool = Pool()
    this_name = (decl.package.replace('.', '/') + '/' + decl.name
                 if decl.package else decl.name)
    super_name = 'java/lang/Object'
    if decl.super_name and not decl.is_interface:
        super_name = resolve_class_name(decl.super_name, decl)

    # ---- fields
    fields = []
    for f in decl.fields:
        flags = f.flags
        if flags == 0 or (decl.is_interface and not flags & ACC_PUBLIC):
            flags |= ACC_PUBLIC
        if decl.is_interface:
            flags |= ACC_PUBLIC | ACC_STATIC | ACC_FINAL
        desc = type_descriptor(f.type_tokens, decl)
        attrs = b''
        n = 0
        if f.constant is not None:
            kind, val = f.constant
            if kind == 'int':
                idx = pool.integer(val)
            elif kind == 'long':
                idx = pool.long(val)
            elif kind == 'double':
                idx = pool.double(val)
            elif kind == 'float':
                idx = pool.float(val)
            else:
                idx = pool.string(val)
            cname = pool.utf8('ConstantValue')
            attrs = struct.pack('>HI', cname, 2) + struct.pack('>H', idx)
            n = 1
        fields.append((flags, f.name, desc, attrs, n))

    # ---- methods
    methods = []
    if not decl.is_interface and not any(
            m.name == decl.name and not m.type_tokens for m in decl.methods):
        # implicit default constructor (the class file format has no such
        # thing, javac needs to see one when compiling a subclass)
        ctor = Member()
        ctor.name = decl.name
        ctor.params = []
        ctor.flags = ACC_PUBLIC
        decl.methods.insert(0, ctor)
    for m in decl.methods:
        desc = method_descriptor(m, decl)
        flags = m.flags
        is_static = bool(flags & ACC_STATIC)
        if decl.is_interface:
            flags |= ACC_PUBLIC | ACC_ABSTRACT
        is_ctor = (m.name == decl.name and not m.type_tokens
                   and not flags & ACC_STATIC)
        if is_ctor:
            m = m
            name = '<init>'
            desc = desc  # constructors already have void return
        else:
            name = m.name
        attrs = []
        code_body = None
        if not (flags & ACC_ABSTRACT) and not (flags & ACC_NATIVE):
            max_locals = slot_count(desc, is_static)
            max_stack = 2
            if is_ctor:
                code = bytes([0x2a, 0xb7]) + struct.pack('>H', pool.methodref(
                    super_name, '<init>', '()V')) + bytes([0xb1])
                max_stack = 1
            else:
                ret = desc.split(')')[-1]
                if ret == 'V':
                    code = bytes([0xb1])
                    max_stack = 1
                elif ret in 'IJFD':
                    code = bytes([0x00 if ret in 'I' else 0x09, 0x00, 0x00,
                                  default_return_op(desc)])
                    max_stack = 2
                else:
                    code = bytes([0x01, default_return_op(desc)])
                    max_stack = 1
            code_attr = (struct.pack('>HHI', max_stack, max_locals, len(code))
                         + code
                         + struct.pack('>HH', 0, 0))  # no handlers, no attrs
            cname = pool.utf8('Code')
            attrs.append(struct.pack('>HI', cname, len(code_attr)) + code_attr)
        if m.throws:
            ex = b''.join(struct.pack('>H', pool.cls(
                resolve_class_name(t, decl))) for t in m.throws)
            ename = pool.utf8('Exceptions')
            payload = struct.pack('>H', len(m.throws)) + ex
            attrs.append(struct.pack('>HI', ename, len(payload)) + payload)
        methods.append((flags, name, desc, b''.join(attrs), len(attrs)))

    # ---- class attributes
    src_name = pool.utf8('SourceFile')
    src_idx = pool.utf8(decl.name + '.java')
    class_attrs = struct.pack('>HI', src_name, 2) + struct.pack('>H', src_idx)

    ifaces = b''.join(struct.pack('>H', pool.cls(resolve_class_name(i, decl)))
                      for i in decl.interfaces)

    access = decl.access | ACC_SUPER if not decl.is_interface else decl.access

    out = [struct.pack('>IHH', 0xCAFEBABE, CLASS_VERSION_MINOR,
                       CLASS_VERSION_MAJOR)]
    # NOTE: pool.dump() must run last, after every index has been created
    body = []
    super_index = 0 if this_name == 'java/lang/Object' else pool.cls(super_name)
    body.append(struct.pack('>HHH', access, pool.cls(this_name), super_index))
    body.append(struct.pack('>H', len(decl.interfaces)) + ifaces)
    body.append(struct.pack('>H', len(fields)))
    for flags, name, desc, attrs, n in fields:
        body.append(struct.pack('>HHHH', flags, pool.utf8(name),
                                pool.utf8(desc), n) + attrs)
    body.append(struct.pack('>H', len(methods)))
    for flags, name, desc, attrs, n in methods:
        body.append(struct.pack('>HHHH', flags, pool.utf8(name),
                                pool.utf8(desc), n) + attrs)
    body.append(struct.pack('>H', 1) + class_attrs)
    out.append(pool.dump())
    out.append(b''.join(body))
    return b''.join(out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--src', default='stubs')
    ap.add_argument('--out', default='build/stub-classes')
    ap.add_argument('--quiet', action='store_true')
    args = ap.parse_args()

    files = []
    for root, _dirs, names in os.walk(args.src):
        for n in sorted(names):
            if n.endswith('.java'):
                files.append(os.path.join(root, n))
    if not files:
        print('no stub sources found in %s' % args.src, file=sys.stderr)
        return 2

    decls = []
    for path in files:
        try:
            decl = parse_file(path)
        except Exception as exc:  # noqa: BLE001
            print('FAILED %s: %s' % (path, exc), file=sys.stderr)
            return 1
        internal = (decl.package.replace('.', '/') + '/' + decl.name
                    if decl.package else decl.name)
        KNOWN_DECLS.add(internal)
        decls.append((path, decl))

    ok = 0
    for path, decl in decls:
        try:
            data = build_class(decl)
        except Exception as exc:  # noqa: BLE001 - report and continue
            print('FAILED %s: %s' % (path, exc), file=sys.stderr)
            return 1
        rel = (decl.package.replace('.', '/') + '/' + decl.name
               if decl.package else decl.name) + '.class'
        dest = os.path.join(args.out, rel)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, 'wb') as fh:
            fh.write(data)
        ok += 1
        if not args.quiet:
            print('  %-58s -> %s' % (path, rel))
    print('generated %d stub classes in %s' % (ok, args.out))
    return 0


if __name__ == '__main__':
    sys.exit(main())
