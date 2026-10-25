package com.arena.apkshell;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Reference source for the WebView shell.
 *
 * THIS FILE IS NOT COMPILED. The sandbox this project was built in has a Java
 * *runtime* (jdk4py) and the Android build-tools (aapt2 / d8 / apksigner /
 * zipalign), but no `javac`, no reachable Maven Central and no reachable
 * dl.google.com, and every ECJ jar on GitHub turned out to be a Git LFS pointer
 * (media.githubusercontent.com is not reachable either).
 *
 * So tools/mkactivity.py emits MainActivity.class directly - a small writer for
 * the Java class-file format (tools/mkclass.py), producing major version 49 so
 * that no StackMapTable is required. d8 then turns that class into classes.dex.
 *
 * Keep this file and mkactivity.py in sync. To check that they agree, build and
 * disassemble:
 *
 *   tools/build-apk.sh --stop-after dex
 *   $ANDROID_SDK/build-tools/*/dexdump -d build/dex/classes.dex
 *
 * and compare against the listing in tools/mkactivity.py's docstring.
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the screen alive while the payload app runs, and stay fullscreen.
        // The manifest theme already hides the status bar; the flags make it
        // explicit and add KEEP_SCREEN_ON.
        getWindow().addFlags(WindowManagerFlags.KEEP_SCREEN_ON | WindowManagerFlags.FULLSCREEN);

        // Hide the status bar, keep layout stable. The navigation bar is left
        // alone on purpose: hiding it with IMMERSIVE_STICKY would swallow the
        // swipe-up gesture that payload apps commonly bind to.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        web = new WebView(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);            // localStorage for the payload app
        s.setAllowFileAccess(true);              // file:///android_asset
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);  // WebAudio may start on load
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setTextZoom(100);                      // ignore the host font scale

        web.setWebChromeClient(new WebChromeClient());
        // Keeping our own WebViewClient means navigation stays inside the app
        // instead of being handed to the host browser.
        web.setWebViewClient(new WebViewClient());
        web.setBackgroundColor(0xFF000000);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        web.loadUrl("file:///android_asset/www/index.html");
        setContentView(web);
    }

    @Override
    public void onBackPressed() {
        // Back navigates the web content first, then leaves the app.
        if (web != null && web.canGoBack()) {
            web.goBack();
            return;
        }
        super.onBackPressed();
    }

    /** Flag values, spelled out so the emitted bytecode needs no field refs. */
    private static final class WindowManagerFlags {
        static final int KEEP_SCREEN_ON = 128;   // WindowManager.LayoutParams
        static final int FULLSCREEN = 1024;
    }
}
