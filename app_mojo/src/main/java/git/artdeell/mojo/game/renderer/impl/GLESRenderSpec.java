package git.artdeell.mojo.game.renderer.impl;

import android.content.Context;
import android.util.Log;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.game.renderer.RenderSpec;
import git.artdeell.mojo.game.renderer.def.Renderers;
import git.artdeell.mojo.game.renderer.extra.GLESProvider;
import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.utils.JREUtils;
import git.artdeell.mojo.utils.plugins.LibraryPlugin;

import java.io.File;
import java.util.Map;

import git.artdeell.mojo.R;
import git.artdeell.mojoexec.MojoExec;

/**
 * Base GLES RenderSpec. Represents a desktop OpenGL wrapper running on-top of {@link GLESProvider}
 */
public abstract class GLESRenderSpec implements RenderSpec {
    protected boolean nsBypass = false;
    protected abstract int glesVersion();
    public void setupEnvironment(Context context, Map<String, String> envMap) {
        GLESProvider provider = GLESProvider.getGlesProvider(context, LauncherPreferences.PREF_USE_ANGLE);
        Log.i("GLESRenderSpec", "Using GLESProvider: " + provider.type());
        provider.setEnvironment(envMap);
        this.nsBypass = provider.requiresNamespace();
        if (LauncherPreferences.PREF_DUMP_SHADERS)
            envMap.put("LIBGL_VGPU_DUMP", "1");
        envMap.put("force_glsl_extensions_warn", "true");
        envMap.put("allow_higher_compat_version", "true");
        envMap.put("allow_glsl_extension_directive_midshader", "true");
        // Prevent OptiFine (and other error-reporting stuff in Minecraft) from balooning the log
        envMap.put("LIBGL_NOERROR", "1");
    }
    public boolean setupRenderer() {
        return MojoExec.prepareEgl(library(), nsBypass, true, glesVersion());
    }

    public static class LTWRenderSpec extends GLESRenderSpec {
        public boolean compatibleDevice(Context context) {
            return JREUtils.getDetectedVersion() >= 3 && new File(Tools.NATIVE_LIB_DIR, this.library()).exists();
        }
        public String name() {
            return "OpenLTW";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_ltw;
        }
        public String tag() {
            return Renderers.LTW_RENDERER;
        }
        public String library() {
            return "libltw.so";
        }
        protected int glesVersion() {
            return 3;
        }
    }

    /**
     * MobileGlues (OpenGL 4.x on top of OpenGL ES 3). The library is taken from the launcher's native
     * directory if bundled, otherwise from the installed MobileGlues plugin app.
     */
    public static class MobileGluesRenderSpec extends GLESRenderSpec {
        private static final String LIB = "libmobileglues.so";
        private LibraryPlugin plugin;

        private boolean hasBundledLibrary() {
            return new File(Tools.NATIVE_LIB_DIR, LIB).exists();
        }
        private LibraryPlugin discover(Context context) {
            if(plugin == null) plugin = LibraryPlugin.discoverPlugin(context, LibraryPlugin.ID_MOBILEGLUES_PLUGIN);
            return plugin;
        }
        public boolean compatibleDevice(Context context) {
            if(JREUtils.getDetectedVersion() < 3) return false;
            if(hasBundledLibrary()) return true;
            LibraryPlugin p = discover(context);
            return p != null && p.checkLibraries(LIB);
        }
        public String name() {
            return "MobileGlues";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_mobileglues;
        }
        public String tag() {
            return Renderers.MOBILEGLUES_RENDERER;
        }
        public String library() {
            return LIB;
        }
        public String librarySearchPath() {
            if(hasBundledLibrary() || plugin == null) return null;
            return plugin.getLibraryPath();
        }
        protected int glesVersion() {
            return 3;
        }
        public void setupEnvironment(Context context, Map<String, String> envMap) {
            discover(context);
            super.setupEnvironment(context, envMap);
            // MobileGlues keeps its config.json and logs here
            File mgDir = new File(Tools.DIR_GAME_HOME, "mobileglues");
            if(mgDir.isDirectory() || mgDir.mkdirs()) {
                envMap.put("MG_DIR_PATH", mgDir.getAbsolutePath());
            }
        }
        public boolean setupRenderer() {
            if(!hasBundledLibrary() && plugin != null) {
                return MojoExec.prepareEgl(plugin.resolveAbsolutePath(LIB), nsBypass, true, glesVersion());
            }
            return super.setupRenderer();
        }
    }

    public static class GL4ESRenderSpec extends GLESRenderSpec {
        public boolean compatibleDevice(Context context) {
            return true;
        }
        public String name() {
            return "GL4ES";
        }
        public int displayName() {
            return R.string.mcl_setting_renderer_gles2_4;
        }
        public String tag() {
            return Renderers.GL4ES_RENDERER;
        }
        public String library() {
            return "libgl4es_114.so";
        }
        protected int glesVersion() {
            return 2;
        }
    }
}
