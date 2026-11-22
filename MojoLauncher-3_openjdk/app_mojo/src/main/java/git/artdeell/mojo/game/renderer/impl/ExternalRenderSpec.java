package git.artdeell.mojo.game.renderer.impl;

import android.content.Context;

import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojo.game.renderer.extra.GLESProvider;
import git.artdeell.mojo.utils.JREUtils;
import git.artdeell.mojo.utils.plugins.LibraryPlugin;

/**
 * RenderSpec for a renderer shipped as a separate plugin APK (FCL/Zalith plugin format).
 * The plugin is described by its manifest meta-data and ships its libraries in its own native directory.
 */
public class ExternalRenderSpec extends GLESRenderSpec {
    public static final String TAG_PREFIX = "plugin:";

    private final LibraryPlugin plugin;
    private final String packageName;
    private final String label;
    private final String library;
    private final Map<String, String> pluginEnv;

    ExternalRenderSpec(LibraryPlugin plugin, String packageName, String label, String library, Map<String, String> pluginEnv) {
        this.plugin = plugin;
        this.packageName = packageName;
        this.label = label;
        this.library = library;
        this.pluginEnv = pluginEnv;
    }

    /** Name shown in the renderer list. */
    public String label() {
        return label;
    }

    @Override
    protected int glesVersion() {
        return 3;
    }

    @Override
    public boolean compatibleDevice(Context context) {
        return JREUtils.getDetectedVersion() >= 3 && plugin.checkLibraries(library);
    }

    @Override
    public String name() {
        return label;
    }

    @Override
    public int displayName() {
        return R.string.mcl_setting_renderer_external;
    }

    @Override
    public String tag() {
        return TAG_PREFIX + packageName;
    }

    /** Absolute path, the plugin library lives outside of the launcher native directory. */
    @Override
    public String library() {
        return plugin.resolveAbsolutePath(library);
    }

    @Override
    public String librarySearchPath() {
        return plugin.getLibraryPath();
    }

    @Override
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        super.setupEnvironment(context, envMap);
        envMap.putAll(pluginEnv);
    }
}
