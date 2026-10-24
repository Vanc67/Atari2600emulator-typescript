package wreckbox.platform;

import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import wreckbox.core.ResourceLoader;

/** Maps the original /maps and /textures resource names to APK assets. */
public final class AndroidResourceLoader implements ResourceLoader {
  private final AssetManager assets;
  public AndroidResourceLoader(AssetManager assets) { this.assets = assets; }
  public InputStream open(String path) {
    try { return assets.open(path.startsWith("/") ? path.substring(1) : path, AssetManager.ACCESS_STREAMING); }
    catch (IOException unavailable) { return null; }
  }
}
