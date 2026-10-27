#!/usr/bin/env python3
"""Audits the 500-item feature list against the recovered sources.

Java sources store Russian text as \\uXXXX escapes, so every source is
decoded first.  For each checklist item the words are folded (Cyrillic to
Latin) and both the original and the folded form are searched, which
catches both the Russian strings and the English identifiers.

Usage:  python3 tools/audit.py [checklist] [out.md]
        writes docs/audit-500.md when no output path is given
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

FOLD = {
    'а': 'a', 'б': 'b', 'в': 'v', 'г': 'g', 'д': 'd', 'е': 'e', 'ё': 'yo',
    'ж': 'zh', 'з': 'z', 'и': 'i', 'й': 'j', 'к': 'k', 'л': 'l', 'м': 'm',
    'н': 'n', 'о': 'o', 'p': 'p', 'р': 'r', 'с': 's', 'т': 't', 'у': 'u',
    'ф': 'f', 'х': 'kh', 'ц': 'c', 'ч': 'ch', 'ш': 'sh', 'щ': 'shch',
    'ъ': '', 'ы': 'y', 'ь': '', 'э': 'e', 'ю': 'yu', 'я': 'ya',
    'ъ': '',
}

# synonyms: a checklist word maps to the tokens actually used in the code
SYNONYMS = {
    'рынок': ['базар', 'митино', 'палатка', 'лот'],
    'компьютер': ['пк'],
    'вею': ['вею'],
    'вентилятор': ['кулер', 'fan'],
    'жесткий': ['hdd', 'винчестер'],
    'дискет': ['флоппи', 'floppy'],
    'радар': [],
    'интернет': ['сеть', 'network', 'web', 'wifi', 'инет'],
    'моль': [],
    'погода': ['weather', 'дождь', 'rain', 'снег', 'snow'],
    'сезон': ['season'],
    'праздник': ['newyear', 'новый', 'день'],
    'доми': [],
}

MAX_KEYS = 6


def fold(word):
    out = []
    for ch in word:
        if ch in FOLD:
            out.append(FOLD[ch])
        else:
            out.append(ch)
    return ''.join(out)


def decode_java(text):
    """Turns \\uXXXX escapes into real characters."""
    def repl(m):
        try:
            return chr(int(m.group(1), 16))
        except ValueError:
            return m.group(0)
    return re.sub(r'\\u([0-9a-fA-F]{4})', repl, text)


def collect_sources():
    docs = []
    for dirpath, dirnames, filenames in os.walk(os.path.join(ROOT, 'src')):
        dirnames[:] = [d for d in dirnames if d != '__pycache__']
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            path = os.path.join(dirpath, fn)
            rel = os.path.relpath(path, ROOT)
            try:
                with open(path, encoding='utf-8') as fh:
                    raw = fh.read()
            except Exception:
                continue
            docs.append((rel, decode_java(raw).lower()))
    return docs


def keywords(text):
    words = re.findall(r'[\u0400-\u04ff\u00c0-\u00ffA-Za-z][\u0400-\u04ffA-Za-z0-9\'\-]*',
                       text.lower())
    out = []
    for w in words:
        w = w.strip('\'-')
        if len(w) < 4 or w in ('что', 'это', 'как', 'только', 'все', 'игры',
                               'список', 'полный', 'первый'):
            continue
        if w in out:
            continue
        out.append(w)
        if len(out) >= MAX_KEYS:
            break
    keys = set()
    for w in out:
        keys.add(w)
        keys.add(fold(w))
        if w in SYNONYMS:
            for s in SYNONYMS[w]:
                if len(s) >= 4:
                    keys.add(s)
    return out, keys


def search(docs, keys):
    """Returns (hits, files) where hits is a list of (file, lineno, snippet)."""
    hits = []
    files = set()
    for rel, text in docs:
        lines = text.split('\n')
        for i, line in enumerate(lines, 1):
            for key in keys:
                if key in line:
                    snippet = line.strip()
                    if len(snippet) > 110:
                        snippet = snippet[:110] + '…'
                    hits.append((rel, i, snippet))
                    files.add(rel)
                    break
    return hits, files


def main(args):
    checklist = args[0] if len(args) > 0 else os.path.join(
        ROOT, 'docs', 'checklist-500.md')
    out_path = args[1] if len(args) > 1 else os.path.join(
        ROOT, 'docs', 'audit-500.md')
    with open(checklist, encoding='utf-8') as fh:
        lines = fh.read().split('\n')

    items = []
    for line in lines:
        m = re.match(r'^\s*(\d+)\.\s+(.+)$', line)
        if m:
            items.append((int(m.group(1)), m.group(2).strip()))

    docs = collect_sources()
    report = []
    ok = 0
    weak = 0
    missing = []
    for num, text in items:
        words, keys = keywords(text)
        hits, files = search(docs, keys)
        if hits:
            ok += 1
            level = 'OK ' if len(hits) >= 2 else '~  '
            report.append('%s%3d. %-58s hits=%d' % (level, num, text, len(hits)))
            for rel, ln, sn in hits[:2]:
                report.append('        %s:%s  %s' % (rel, ln, sn))
        else:
            weak += 1
            missing.append((num, text))
            report.append('-- %3d. %s' % (num, text))

    header = ['# Audit of the 500 features against the sources',
              '',
              'Sources scanned: %d java files, `\\uXXXX` escapes decoded,'
              % len(docs),
              'words folded Cyrillic -> Latin so that Russian strings and',
              'English identifiers both match.',
              '',
              '**%d of %d items have a textual match in the code**, %d items'
              % (ok, len(items), weak),
              'have no keyword anywhere in the sources (they are candidates',
              'for implementation, or they live behind a generic entry).',
              '']
    report = header + report
    with open(out_path, 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(report) + '\n')

    print('\n'.join(header))
    print('\n'.join(report[len(header):len(header) + 40]))
    print('\n%s' % ('\n'.join('%3d. %s' % (n, t) for n, t in missing)))
    print('\nwritten %s' % out_path)
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
