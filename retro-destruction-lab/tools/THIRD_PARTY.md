# Compile-only third-party tools

The game itself has **no runtime libraries**. The following files are downloaded only to compile and preverify the MIDlet; none of them is packed into the game JAR.

| File | Upstream location | Purpose |
|---|---|---|
| `cldc-1.1.jar` | `Enough-Software/j2mepolish`, `enough-polish-build/import/` | CLDC 1.1 compile-time API |
| `midp-2.0.jar` | `Enough-Software/j2mepolish`, `enough-polish-build/import/` | MIDP 2.0 compile-time API |
| `proguard.jar` 4.4 | `Enough-Software/j2mepolish`, `enough-polish-build/lib/` | CLDC preverification (`-microedition`) |

Upstream repository: <https://github.com/Enough-Software/j2mepolish>. J2ME Polish and its bundled ProGuard are historically GPL-licensed build tools; review their upstream license notices when redistributing the tools themselves. The build script downloads them into the ignored `lib/` directory instead of vendoring them.

A JDK 8 compiler is required for `-source 1.2 -target 1.2`. Newer JDKs can package the release but their bundled `javac` no longer supports that target.
