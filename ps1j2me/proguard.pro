# ProGuard configuration for PS1J2ME
#
# The MIDlet class must survive obfuscation together with its public
# constructor, everything else may be renamed and shrunk.

-microedition
-target 1.3
# Optimisation passes: shrinking + inlining + class merging.
-optimizationpasses 2
-allowaccessmodification
-dontpreverify
-dontnote
-dontwarn

-verbose

-keep public class psx.PS1Midlet {
    public <init>();
    public void startApp();
    public void pauseApp();
    public void destroyApp(boolean);
}

# The browser opens the BIOS as a resource, keep the name of the entry point
-keepnames class psx.MainCanvas

# Keep line numbers so that on-device stack traces stay readable
-keepattributes SourceFile,LineNumberTable

# JSR-75 / MIDP callbacks are invoked by the runtime
-keep class javax.** { *; }
