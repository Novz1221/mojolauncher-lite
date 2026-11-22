package git.artdeell.mojo.game.renderer.impl;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import git.artdeell.mojo.utils.plugins.LibraryPlugin;

/**
 * Finds renderer plugin APKs. A plugin is an installed app with the "fclPlugin" meta-data flag and a
 * "renderer" meta-data entry in the form "Name:glLibrary:eglLibrary".
 */
public final class ExternalRenderers {
    private static final String TAG = "ExternalRenderers";
    private static final String META_FLAG = "fclPlugin";
    private static final String META_RENDERER = "renderer";
    private static final String META_DESCRIPTION = "des";
    private static final String META_ENV = "pojavEnv";

    // The plugin env also names the library itself, the launcher already handles that part
    private static final String[] IGNORED_ENV = {"POJAV_RENDERER", "POJAVEXEC_EGL", "LIBGL_EGL"};

    private static Context sContext;

    private ExternalRenderers() {}

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    /** Find an installed plugin from a renderer tag. Returns null if it is gone or not a renderer plugin. */
    public static ExternalRenderSpec get(String tag) {
        if (sContext == null || tag == null || !tag.startsWith(ExternalRenderSpec.TAG_PREFIX)) return null;
        String packageName = tag.substring(ExternalRenderSpec.TAG_PREFIX.length());
        try {
            ApplicationInfo info = sContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return fromInfo(sContext, info);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Renderer plugin is not installed: " + packageName);
            return null;
        }
    }

    /** Scan all installed apps for renderer plugins. */
    public static List<ExternalRenderSpec> scan(Context context) {
        List<ExternalRenderSpec> result = new ArrayList<>();
        try {
            for (ApplicationInfo info : context.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA)) {
                if (info.packageName.equals(context.getPackageName())) continue;
                ExternalRenderSpec spec = fromInfo(context, info);
                if (spec != null) result.add(spec);
            }
        } catch (Exception e) {
            Log.e(TAG, "Renderer plugin scan failed", e);
        }
        return result;
    }

    private static ExternalRenderSpec fromInfo(Context context, ApplicationInfo info) {
        Bundle meta = info.metaData;
        if (meta == null || !meta.getBoolean(META_FLAG, false)) return null;
        String renderer = meta.getString(META_RENDERER);
        if (renderer == null) return null;

        String[] parts = renderer.split(":");
        if (parts.length < 3 || parts[2].isEmpty()) return null;

        LibraryPlugin plugin = LibraryPlugin.discoverPlugin(context, info.packageName);
        if (plugin == null) return null;

        String description = meta.getString(META_DESCRIPTION);
        String label = description != null && !description.isEmpty() ? description : parts[0];
        return new ExternalRenderSpec(plugin, info.packageName, label, parts[2], parseEnv(meta.getString(META_ENV)));
    }

    private static Map<String, String> parseEnv(String raw) {
        Map<String, String> env = new HashMap<>();
        if (raw == null) return env;
        for (String entry : raw.split(":")) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String key = entry.substring(0, eq);
            boolean ignored = false;
            for (String skip : IGNORED_ENV) if (skip.equals(key)) ignored = true;
            if (!ignored) env.put(key, entry.substring(eq + 1));
        }
        return env;
    }
}
