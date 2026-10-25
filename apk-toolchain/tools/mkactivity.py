#!/usr/bin/env python3
"""
mkactivity.py - emits MainActivity.class for the Android WebView shell.

This is the hand-written equivalent of:

    package com.arena.apkshell;

    public class MainActivity extends Activity {
        private WebView web;

        protected void onCreate(Bundle b) {
            super.onCreate(b);
            getWindow().addFlags(FLAG_KEEP_SCREEN_ON | FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_LAYOUT_STABLE
                | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            web = new WebView(this);
            WebSettings s = web.getSettings();
            s.setJavaScriptEnabled(true);
            ...
            web.loadUrl("file:///android_asset/www/index.html");
            setContentView(web);
        }

        public void onBackPressed() {
            if (web != null && web.canGoBack()) { web.goBack(); return; }
            super.onBackPressed();
        }
    }

The .java text above is kept in android/src/ for readability; the binary
is produced here because the sandbox has no Java compiler (see mkclass.py).
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from mkclass import (  # noqa: E402
    ACC_PRIVATE,
    ACC_PROTECTED,
    ACC_PUBLIC,
    ACC_SUPER,
    Code,
    ConstantPool,
    build_class,
)

PKG = "com/arena/apkshell"
THIS = PKG + "/MainActivity"

ACTIVITY = "android/app/Activity"
BUNDLE = "android/os/Bundle"
CONTEXT = "android/content/Context"
WINDOW = "android/view/Window"
VIEW = "android/view/View"
WEBVIEW = "android/webkit/WebView"
WEBSETTINGS = "android/webkit/WebSettings"
WEBCHROMECLIENT = "android/webkit/WebChromeClient"
WEBVIEWCLIENT = "android/webkit/WebViewClient"

START_URL = "file:///android_asset/www/index.html"

# Window / View flag values (kept as literals so the class needs no field refs)
FLAG_KEEP_SCREEN_ON = 128
FLAG_FULLSCREEN = 1024
SYSUI_FULLSCREEN = 4
SYSUI_LAYOUT_STABLE = 256
SYSUI_LAYOUT_FULLSCREEN = 1024


def emit():
    cp = ConstantPool()

    # ---- constant pool entries, resolved up-front ------------------------
    m_activity_oncreate = cp.method(ACTIVITY, "onCreate", "(L" + BUNDLE + ";)V")
    m_activity_getwindow = cp.method(ACTIVITY, "getWindow", "()L" + WINDOW + ";")
    m_activity_setcontentview = cp.method(ACTIVITY, "setContentView", "(L" + VIEW + ";)V")
    m_activity_onbackpressed = cp.method(ACTIVITY, "onBackPressed", "()V")
    m_activity_init = cp.method(ACTIVITY, "<init>", "()V")

    m_window_addflags = cp.method(WINDOW, "addFlags", "(I)V")
    m_window_getdecor = cp.method(WINDOW, "getDecorView", "()L" + VIEW + ";")
    m_view_setsysui = cp.method(VIEW, "setSystemUiVisibility", "(I)V")

    m_webview_init = cp.method(WEBVIEW, "<init>", "(L" + CONTEXT + ";)V")
    m_webview_getsettings = cp.method(WEBVIEW, "getSettings", "()L" + WEBSETTINGS + ";")
    m_webview_setchrome = cp.method(
        WEBVIEW, "setWebChromeClient", "(L" + WEBCHROMECLIENT + ";)V"
    )
    m_webview_setclient = cp.method(WEBVIEW, "setWebViewClient", "(L" + WEBVIEWCLIENT + ";)V")
    m_webview_loadurl = cp.method(WEBVIEW, "loadUrl", "(Ljava/lang/String;)V")
    m_webview_cangoback = cp.method(WEBVIEW, "canGoBack", "()Z")
    m_webview_goback = cp.method(WEBVIEW, "goBack", "()V")
    m_webview_setbgcolor = cp.method(VIEW, "setBackgroundColor", "(I)V")  # inherited
    m_webview_setoverscroll = cp.method(VIEW, "setOverScrollMode", "(I)V")  # inherited

    m_chrome_init = cp.method(WEBCHROMECLIENT, "<init>", "()V")
    m_client_init = cp.method(WEBVIEWCLIENT, "<init>", "()V")

    c_webview = cp.cls(WEBVIEW)
    c_chrome = cp.cls(WEBCHROMECLIENT)
    c_client = cp.cls(WEBVIEWCLIENT)

    f_web = cp.field(THIS, "web", "L" + WEBVIEW + ";")

    s_url = cp.string(START_URL)

    # settings toggles: (method name, descriptor, value)
    settings_calls = [
        ("setJavaScriptEnabled", "(Z)V", 1),
        ("setDomStorageEnabled", "(Z)V", 1),
        ("setAllowFileAccess", "(Z)V", 1),
        ("setAllowUniversalAccessFromFileURLs", "(Z)V", 1),
        ("setAllowFileAccessFromFileURLs", "(Z)V", 1),
        ("setAllowContentAccess", "(Z)V", 1),
        ("setMediaPlaybackRequiresUserGesture", "(Z)V", 0),
        ("setUseWideViewPort", "(Z)V", 1),
        ("setLoadWithOverviewMode", "(Z)V", 1),
        ("setSupportZoom", "(Z)V", 0),
        ("setBuiltInZoomControls", "(Z)V", 0),
        ("setDisplayZoomControls", "(Z)V", 0),
        ("setJavaScriptCanOpenWindowsAutomatically", "(Z)V", 1),
        ("setTextZoom", "(I)V", 100),
    ]
    settings_refs = [(cp.method(WEBSETTINGS, n, d), v) for n, d, v in settings_calls]

    # ---- <init>()V -------------------------------------------------------
    c = Code()
    c.aload(0).invokespecial(m_activity_init).return_()
    init_code = c.build()

    # ---- onCreate(Landroid/os/Bundle;)V ----------------------------------
    c = Code()
    # super.onCreate(savedInstanceState)
    c.aload(0).aload(1).invokespecial(m_activity_oncreate)
    # getWindow().addFlags(KEEP_SCREEN_ON | FULLSCREEN)
    c.aload(0).invokevirtual(m_activity_getwindow)
    c.iconst(FLAG_KEEP_SCREEN_ON | FLAG_FULLSCREEN)
    c.invokevirtual(m_window_addflags)
    # getWindow().getDecorView().setSystemUiVisibility(...)
    c.aload(0).invokevirtual(m_activity_getwindow).invokevirtual(m_window_getdecor)
    c.iconst(SYSUI_FULLSCREEN | SYSUI_LAYOUT_STABLE | SYSUI_LAYOUT_FULLSCREEN)
    c.invokevirtual(m_view_setsysui)
    # this.web = new WebView(this)
    c.aload(0)
    c.new(c_webview).dup().aload(0).invokespecial(m_webview_init)
    c.putfield(f_web)
    # WebSettings s = web.getSettings()
    c.aload(0).getfield(f_web).invokevirtual(m_webview_getsettings).astore(2)
    for ref, value in settings_refs:
        c.aload(2)
        if value <= 1:
            c.iconst(value)
        else:
            c.iconst(value)
        c.invokevirtual(ref)
    # web.setWebChromeClient(new WebChromeClient())
    c.aload(0).getfield(f_web)
    c.new(c_chrome).dup().invokespecial(m_chrome_init)
    c.invokevirtual(m_webview_setchrome)
    # web.setWebViewClient(new WebViewClient())
    c.aload(0).getfield(f_web)
    c.new(c_client).dup().invokespecial(m_client_init)
    c.invokevirtual(m_webview_setclient)
    # web.setBackgroundColor(0xFF000000)
    c.aload(0).getfield(f_web)
    c.ldc(cp.integer(-16777216))
    c.invokevirtual(m_webview_setbgcolor)
    # web.setOverScrollMode(OVER_SCROLL_NEVER)
    c.aload(0).getfield(f_web).iconst(2).invokevirtual(m_webview_setoverscroll)
    # web.loadUrl(START_URL)
    c.aload(0).getfield(f_web).ldc(s_url).invokevirtual(m_webview_loadurl)
    # setContentView(web)
    c.aload(0).aload(0).getfield(f_web).invokevirtual(m_activity_setcontentview)
    c.return_()
    oncreate_code = c.build()

    # ---- onBackPressed()V ------------------------------------------------
    c = Code()
    c.aload(0).getfield(f_web).ifnull("SUPER")
    c.aload(0).getfield(f_web).invokevirtual(m_webview_cangoback).ifeq("SUPER")
    c.aload(0).getfield(f_web).invokevirtual(m_webview_goback)
    c.goto("RET")
    c.label("SUPER")
    c.aload(0).invokespecial(m_activity_onbackpressed)
    c.label("RET")
    c.return_()
    onback_code = c.build()

    return build_class(
        THIS,
        ACTIVITY,
        cp,
        fields=[(ACC_PRIVATE, "web", "L" + WEBVIEW + ";")],
        methods=[
            (ACC_PUBLIC, "<init>", "()V", init_code, 1, 1),
            (ACC_PROTECTED, "onCreate", "(L" + BUNDLE + ";)V", oncreate_code, 6, 3),
            (ACC_PUBLIC, "onBackPressed", "()V", onback_code, 4, 1),
        ],
        access_flags=ACC_PUBLIC | ACC_SUPER,
        source_file="MainActivity.java",
    )


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "build"
    if out.endswith(".class"):
        # explicit file path: honour it exactly
        target = os.path.abspath(out)
    else:
        # root directory: lay the class out in its package path so d8/zip find it
        target = os.path.join(os.path.abspath(out), PKG, "MainActivity.class")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    data = emit()
    with open(target, "wb") as fh:
        fh.write(data)
    print("wrote %s (%d bytes)" % (target, len(data)))


if __name__ == "__main__":
    main()
