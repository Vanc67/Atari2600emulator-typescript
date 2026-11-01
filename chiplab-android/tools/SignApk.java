import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tiny CLI on top of Google's apksig library (compiled from the LineageOS /
 * AOSP mirror sources by tools/setup_toolchain.sh):
 *   SignApk sign   <keystore> <storePass> <alias> <keyPass> <in.apk> <out.apk>
 *   SignApk verify <apk>
 *   SignApk manifest <AndroidManifest.xml>   (parses AXML with Google's parser)
 */
public class SignApk {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: SignApk sign|verify|manifest ...");
            System.exit(2);
        }
        if ("sign".equals(args[0])) {
            sign(args[1], args[2], args[3], args[4], new File(args[5]), new File(args[6]));
        } else if ("verify".equals(args[0])) {
            verify(new File(args[1]));
        } else if ("manifest".equals(args[0])) {
            byte[] data = Files.readAllBytes(new File(args[1]).toPath());
            ByteBuffer buf = ByteBuffer.wrap(data);
            int minSdk = ApkUtils.getMinSdkVersionFromBinaryAndroidManifest(buf.duplicate());
            String pkg = ApkUtils.getPackageNameFromBinaryAndroidManifest(buf.duplicate());
            System.out.println("[axml-check] package=" + pkg + " minSdk=" + minSdk);
            if (minSdk != 21 || !"com.chiplab".equals(pkg)) {
                System.out.println("[axml-check] UNEXPECTED VALUES");
                System.exit(1);
            }
        } else {
            System.exit(2);
        }
    }

    static void sign(String ksPath, String storePass, String alias, String keyPass,
                     File in, File out) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(ksPath);
        try {
            ks.load(fis, storePass.toCharArray());
        } finally {
            fis.close();
        }
        PrivateKey key = (PrivateKey) ks.getKey(alias, keyPass.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        for (Certificate cert : chain) certs.add((X509Certificate) cert);
        ApkSigner.SignerConfig config =
                new ApkSigner.SignerConfig.Builder("CERT", key, certs).build();
        new ApkSigner.Builder(Collections.singletonList(config))
                .setInputApk(in)
                .setOutputApk(out)
                .setMinSdkVersion(21)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .setCreatedBy("ChipLab build (apksig)")
                .build()
                .sign();
        System.out.println("[sign] signed -> " + out + " (" + out.length() + " bytes)");
    }

    static void verify(File apk) throws Exception {
        ApkVerifier.Result result = new ApkVerifier.Builder(apk).build().verify();
        System.out.println("[verify] verified=" + result.isVerified()
                + " v1=" + result.isVerifiedUsingV1Scheme()
                + " v2=" + result.isVerifiedUsingV2Scheme()
                + " v3=" + result.isVerifiedUsingV3Scheme());
        for (ApkVerifier.IssueWithParams err : result.getAllErrors()) {
            System.out.println("[verify] ERROR: " + err);
        }
        for (ApkVerifier.IssueWithParams warn : result.getWarnings()) {
            System.out.println("[verify] warning: " + warn);
        }
        List<X509Certificate> certs = result.getSignerCertificates();
        for (X509Certificate cert : certs) {
            System.out.println("[verify] signer: " + cert.getSubjectDN());
        }
        if (!result.isVerified()) System.exit(1);
    }
}
