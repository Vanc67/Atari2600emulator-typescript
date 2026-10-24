import java.io.File;
import brut.androlib.src.SmaliBuilder;
import brut.directory.ExtFile;

/**
 * Assembles a tree of .smali files into a classes.dex.
 *
 * dexlib2's command line front end is stripped out of the apktool jar but the
 * assembler itself (brut.androlib.src.SmaliBuilder) is there and is exactly
 * what apktool uses when it rebuilds an APK, so we drive it directly.
 */
public final class Smali2Dex {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: Smali2Dex <smali-dir> <out.dex> [api-level]");
            System.exit(2);
        }
        File in = new File(args[0]);
        File out = new File(args[1]);
        int api = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        SmaliBuilder.build(new ExtFile(in), out, api);
        System.out.println("[dex] " + out.getName() + " "
                + out.length() + " bytes, api " + api);
    }
}
