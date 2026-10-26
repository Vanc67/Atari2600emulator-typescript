package rpc.harness;

import java.io.ByteArrayOutputStream;
import java.util.zip.Inflater;

import rpc.Images$Img;

/**
 * Minimal PNG decoder (8 bit RGB/RGBA/grey, non interlaced) used only by the
 * desktop harness - handsets decode PNGs natively.
 */
public final class Png {

    private Png() {
    }

    public static Images$Img decode(byte[] data) {
        if (data == null || data.length < 8) {
            return null;
        }
        int pos = 8;
        int gw = 0;
        int gh = 0;
        int depth = 8;
        int ctype = 0;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        while (pos + 8 <= data.length) {
            int len = be32(data, pos);
            String type = new String(data, pos + 4, 4);
            int body = pos + 8;
            if (type.equals("IHDR")) {
                gw = be32(data, body);
                gh = be32(data, body + 4);
                depth = data[body + 8] & 0xFF;
                ctype = data[body + 9] & 0xFF;
            } else if (type.equals("IDAT")) {
                idat.write(data, body, len);
            } else if (type.equals("IEND")) {
                break;
            }
            pos = body + len + 4;
        }
        if (gw <= 0 || gh <= 0 || depth != 8) {
            return null;
        }
        int channels;
        if (ctype == 0) {
            channels = 1;
        } else if (ctype == 2) {
            channels = 3;
        } else if (ctype == 4) {
            channels = 2;
        } else if (ctype == 6) {
            channels = 4;
        } else {
            return null;
        }
        byte[] raw = inflate(idat.toByteArray());
        int stride = gw * channels;
        if (raw == null || raw.length < (stride + 1) * gh) {
            return null;
        }
        byte[] out = new byte[stride * gh];
        int prev = 0;
        int rowLen = stride;
        for (int y = 0; y < gh; y++) {
            int filter = raw[y * (rowLen + 1)] & 0xFF;
            int src = y * (rowLen + 1) + 1;
            int dst = y * rowLen;
            for (int x = 0; x < rowLen; x++) {
                int a = x >= channels ? (out[dst + x - channels] & 0xFF) : 0;
                int b = y > 0 ? (out[prev + x] & 0xFF) : 0;
                int c = (x >= channels && y > 0)
                        ? (out[prev + x - channels] & 0xFF) : 0;
                int v = raw[src + x] & 0xFF;
                switch (filter) {
                    case 0:
                        break;
                    case 1:
                        v += a;
                        break;
                    case 2:
                        v += b;
                        break;
                    case 3:
                        v += (a + b) / 2;
                        break;
                    case 4:
                        v += paeth(a, b, c);
                        break;
                    default:
                        return null;
                }
                out[dst + x] = (byte) v;
            }
            prev = dst;
        }
        Images$Img img = new Images$Img(gw, gh);
        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                int i = (y * gw + x) * channels;
                int r;
                int g;
                int b;
                int a = 255;
                if (channels == 1) {
                    r = out[i] & 0xFF;
                    g = r;
                    b = r;
                } else if (channels == 2) {
                    r = out[i] & 0xFF;
                    g = r;
                    b = r;
                    a = out[i + 1] & 0xFF;
                } else if (channels == 3) {
                    r = out[i] & 0xFF;
                    g = out[i + 1] & 0xFF;
                    b = out[i + 2] & 0xFF;
                } else {
                    r = out[i] & 0xFF;
                    g = out[i + 1] & 0xFF;
                    b = out[i + 2] & 0xFF;
                    a = out[i + 3] & 0xFF;
                }
                img.argb[y * gw + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return img;
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) {
            return a;
        }
        return pb <= pc ? b : c;
    }

    private static byte[] inflate(byte[] data) {
        Inflater inf = new Inflater();
        inf.setInput(data, 0, data.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n <= 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
        } catch (Throwable t) {
            return null;
        }
        inf.end();
        return out.toByteArray();
    }

    private static int be32(byte[] d, int off) {
        return ((d[off] & 0xFF) << 24) | ((d[off + 1] & 0xFF) << 16)
                | ((d[off + 2] & 0xFF) << 8) | (d[off + 3] & 0xFF);
    }
}
