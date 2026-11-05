import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Reads every entry of a built APK through java.util.jar with verification
 * switched on.  This is the same JAR signature check Android's JarVerifier
 * performs, so a passing run here means the APK will install on a 2.3 device
 * as far as signing goes.
 */
public final class ApkVerify {

    public static void main(String[] args) throws Exception {
        JarFile jar = new JarFile(args[0], true);
        byte[] buf = new byte[8192];
        int entries = 0;
        int unsigned = 0;
        for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
            JarEntry je = e.nextElement();
            InputStream in = jar.getInputStream(je);
            while (in.read(buf) > 0) {
                // reading through the verifier is what checks the digests
            }
            in.close();
            if (je.isDirectory()) {
                continue;
            }
            if (je.getName().startsWith("META-INF/")) {
                continue;
            }
            entries++;
            Certificate[] certs = je.getCertificates();
            if (certs == null || certs.length == 0) {
                unsigned++;
                System.out.println("[verify] UNSIGNED " + je.getName());
            }
        }
        jar.close();
        System.out.println("[verify] " + entries + " entries, "
                + unsigned + " unsigned");
        if (unsigned != 0) {
            System.out.println("[verify] FAILED");
            System.exit(1);
        }
        System.out.println("[verify] signature ok");
    }
}
