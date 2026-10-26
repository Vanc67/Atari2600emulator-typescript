#!/usr/bin/env python3
"""The namespace preamble shared by every generated JavaScript file.

The web files are classic scripts, not ES modules: WebView refuses modules on
file://, so they share state through a global `A5` object. In a browser a
top-level `var A5` already lands on the global object, but under Node/CommonJS
each module gets its own scope -- with the naive form, arm.js and a5.js end up
holding two *different* A5 objects and `new A5.Bus(...)` throws.

Resolving through globalThis makes the browser and Node paths identical. It
lives here, in one place, because both generators emit it: a hand-patched copy
in one generated file silently drifts and the next regeneration undoes the fix
(which is exactly what happened once already).
"""

PREAMBLE = """// Resolve the namespace from the global object so that every file shares one
// A5. In a browser `var A5` at top level already does this; under Node/CommonJS
// each module has its own scope, so without this arm.js and a5.js end up with
// two different A5 objects and `new A5.Bus(...)` fails.
var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});"""

EXPORT = """if (typeof module !== "undefined" && module.exports) module.exports = A5;"""
