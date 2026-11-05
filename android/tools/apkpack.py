"""Packs and signs an APK without the Android SDK.

``aapt`` (from the apktool jar) turns the manifest -- and, if there is one, the
``res/`` tree -- into the binary form Android expects; ``classes.dex`` and the
``assets/`` payload are added as plain zip entries; then the whole thing is
signed with a v1 (JAR) signature, which is what Android 2.3 verifies.

The signature is produced by hand because jarsigner is not installed: a
SHA1 based MANIFEST.MF + CERT.SF + a PKCS#7 CERT.RSA built with openssl, which
is byte-for-byte the layout jarsigner writes and Android's JarVerifier reads.
"""

import base64
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile


class PackError(Exception):
    pass


def run(cmd, **kw):
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    out = p.communicate()[0].decode("utf-8", "replace")
    if p.returncode != 0:
        raise PackError("%s failed (%d):\n%s" % (" ".join(cmd), p.returncode, out))
    return out


def _wrap(line):
    """Splits a manifest line the way java.util.jar.Manifest does (72 bytes)."""
    raw = line.encode("utf-8")
    if len(raw) <= 72:
        return raw + b"\r\n"
    out = []
    first = True
    while raw:
        room = 72 if first else 71
        chunk = raw[:room]
        raw = raw[room:]
        out.append((b"" if first else b" ") + chunk)
        first = False
    return b"\r\n".join(out) + b"\r\n"


def _section(name, value):
    return _wrap("Name: " + name) + _wrap("SHA1-Digest: " + value) + b"\r\n"


def sign_v1(apk, key_pem, cert_pem, openssl="openssl"):
    """Rewrites *apk* in place with a JAR signature."""
    tmp = apk + ".signed"
    with zipfile.ZipFile(apk, "r") as zin:
        entries = [(i, zin.read(i.filename)) for i in zin.infolist()
                   if not i.filename.startswith("META-INF/")]
    mf = [_wrap("Manifest-Version: 1.0"), _wrap("Created-By: PSX APK tool"),
          b"\r\n"]
    sf = [_wrap("Signature-Version: 1.0"), _wrap("Created-By: PSX APK tool")]
    sections = []
    sf_sections = []
    for info, data in entries:
        digest = base64.b64encode(hashlib.sha1(data).digest()).decode("ascii")
        sec = _section(info.filename, digest)
        sections.append(sec)
        sf_sections.append(_wrap("Name: " + info.filename))
        sf_sections.append(_wrap("SHA1-Digest: " + base64.b64encode(
            hashlib.sha1(sec).digest()).decode("ascii")))
        sf_sections.append(b"\r\n")
    manifest = b"".join(mf) + b"".join(sections)
    sf.append(_wrap("SHA1-Digest-Manifest: " + base64.b64encode(
        hashlib.sha1(manifest).digest()).decode("ascii")))
    sfdoc = b"".join(sf) + b"\r\n" + b"".join(sf_sections)

    work = tempfile.mkdtemp(prefix="psxsign")
    try:
        mf_path = os.path.join(work, "MANIFEST.MF")
        sf_path = os.path.join(work, "CERT.SF")
        rsa_path = os.path.join(work, "CERT.RSA")
        open(mf_path, "wb").write(manifest)
        open(sf_path, "wb").write(sfdoc)
        run([openssl, "smime", "-sign", "-binary", "-noattr", "-md", "sha1",
             "-in", sf_path, "-signer", cert_pem, "-inkey", key_pem,
             "-outform", "DER", "-out", rsa_path])
        with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
            zout.writestr("META-INF/MANIFEST.MF", manifest)
            zout.writestr("META-INF/CERT.SF", sfdoc)
            zout.writestr("META-INF/CERT.RSA", open(rsa_path, "rb").read())
            for info, data in entries:
                zi = zipfile.ZipInfo(info.filename, date_time=(1980, 1, 1, 0, 0, 0))
                zi.compress_type = zipfile.ZIP_DEFLATED
                zi.external_attr = info.external_attr
                zout.writestr(zi, data)
    finally:
        shutil.rmtree(work, ignore_errors=True)
    os.replace(tmp, apk)


def make_key(directory, openssl="openssl"):
    key = os.path.join(directory, "psx-key.pem")
    cert = os.path.join(directory, "psx-cert.pem")
    if not (os.path.exists(key) and os.path.exists(cert)):
        run([openssl, "req", "-x509", "-newkey", "rsa:2048", "-nodes",
             "-keyout", key, "-out", cert, "-days", "10950",
             "-subj", "/CN=PSX Android Emulator/O=PSX/C=RU"])
    return key, cert


def package(manifest, out_apk, classes_dex, aapt, android_jar,
            res_dir=None, assets=None, extra=None):
    """Runs aapt, then adds dex/assets and signs."""
    work = tempfile.mkdtemp(prefix="psxpack")
    try:
        unsigned = os.path.join(work, "unsigned.apk")
        cmd = [aapt, "package", "-f", "-M", manifest, "-I", android_jar,
               "-F", unsigned]
        if res_dir:
            cmd += ["-S", res_dir]
        if assets:
            cmd += ["-A", assets]
        run(cmd)
        with zipfile.ZipFile(unsigned, "a") as z:
            with open(classes_dex, "rb") as f:
                zi = zipfile.ZipInfo("classes.dex", date_time=(1980, 1, 1, 0, 0, 0))
                zi.compress_type = zipfile.ZIP_DEFLATED
                z.writestr(zi, f.read())
            for name, path in (extra or {}).items():
                with open(path, "rb") as f:
                    zi = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                    zi.compress_type = zipfile.ZIP_DEFLATED
                    z.writestr(zi, f.read())
        shutil.copy(unsigned, out_apk)
        return out_apk
    finally:
        shutil.rmtree(work, ignore_errors=True)


def main(argv):
    if len(argv) < 2:
        sys.stderr.write("usage: apkpack.py --manifest M --dex D --out A "
                         "--aapt aapt --android jar [--sign keydir]\n")
        return 2
    args = {}
    i = 1
    while i < len(argv) - 1:
        if argv[i].startswith("--"):
            args[argv[i][2:]] = argv[i + 1]
            i += 2
        else:
            i += 1
    out = args["out"]
    package(args["manifest"], out, args["dex"], args["aapt"],
            args["android"], res_dir=args.get("res"), assets=args.get("assets"))
    if args.get("sign"):
        key, cert = make_key(args["sign"])
        sign_v1(out, key, cert)
    print("[apk] %s %d bytes" % (out, os.path.getsize(out)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
