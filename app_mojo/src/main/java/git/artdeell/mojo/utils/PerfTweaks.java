package git.artdeell.mojo.utils;

import android.util.Log;

import git.artdeell.mojo.prefs.LauncherPreferences;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * GPU-aware performance tweaks (PowerVR, Mali, Adreno and others).
 * Everything here is gated by {@link LauncherPreferences#PREF_PERF_TWEAKS}.
 */
public class PerfTweaks {
    private static final String TAG = "PerfTweaks";
    private static final String APPLIED_MARKER = ".mojo_perf_applied_v3";

    /** Environment for the OpenGL ES based renderers (GL4ES, OpenLTW) */
    public static void applyGlesEnv(Map<String, String> env) {
        if(!LauncherPreferences.PREF_PERF_TWEAKS) return;
        env.put("LIBGL_FASTMATH", "1");
        env.put("LIBGL_NOBANNER", "1");
    }

    /** Environment for the Mesa based renderers (Zink, Freedreno, Mesa) */
    public static void applyMesaEnv(Map<String, String> env) {
        if(!LauncherPreferences.PREF_PERF_TWEAKS) return;
        // Skip GL error checking and move GL command submission onto a separate thread
        env.put("MESA_NO_ERROR", "1");
        env.put("mesa_glthread", "true");
    }

    /** Maximum render/simulation distance (chunks) used when capping for the current GPU */
    private static int distanceCap(GpuUtils.GLInfo gpu) {
        if(gpu.isPowerVR()) return LauncherPreferences.PREF_PERF_HIGH_FPS ? 2 : 3;
        if(gpu.isArm()) return 6;
        return 8;
    }

    private static void capOption(String key, int cap) {
        String value = MCOptionUtils.get(key);
        if(value == null) return;
        try {
            if(Integer.parseInt(value.trim()) > cap) MCOptionUtils.set(key, String.valueOf(cap));
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Lower the costly in-game options once per game directory.
     * Afterwards the user can raise them again in the game without being overridden.
     * Call right after {@link MCOptionUtils#load(String)}.
     */
    public static void applyGameOptions(File gameDir) {
        if(!LauncherPreferences.PREF_PERF_TWEAKS) return;
        File marker = new File(gameDir, APPLIED_MARKER);
        if(marker.exists()) return;
        GpuUtils.GLInfo gpu = GpuUtils.getGlInfo();
        Log.i(TAG, "Applying game options for GPU: " + gpu.renderer);

                // 260 means unlimited in Minecraft; the 60 cap keeps frame times even
        MCOptionUtils.set("maxFps", LauncherPreferences.PREF_PERF_HIGH_FPS ? "260" : "60");
        MCOptionUtils.set("enableVsync", "false");
        MCOptionUtils.set("fancyGraphics", "false");
        MCOptionUtils.set("graphicsMode", "0");
        MCOptionUtils.set("ao", "0");
        MCOptionUtils.set("biomeBlendRadius", "0");
        MCOptionUtils.set("entityShadows", "false");
        MCOptionUtils.set("renderClouds", "\"false\"");
        MCOptionUtils.set("clouds", "false");
        MCOptionUtils.set("mipmapLevels", "0");
        MCOptionUtils.set("particles", "2");
        MCOptionUtils.set("entityDistanceScaling", gpu.isPowerVR() ? "0.3" : "0.5");
        if(gpu.isPowerVR()) {
            // PowerVR is a tile-based GPU with weak fill rate: drop the costly effects fully
            MCOptionUtils.set("fboEnable", "true");
            MCOptionUtils.set("anisotropicFiltering", "1");
            MCOptionUtils.set("viewBobbing", "false");
            MCOptionUtils.set("bobView", "false");
            MCOptionUtils.set("fov", "0.0");
            MCOptionUtils.set("showAutosaveIndicator", "false");
            MCOptionUtils.set("cloudStatus", "\"off\"");
            MCOptionUtils.set("weatherRadius", "3");
        }
        int cap = distanceCap(gpu);
        capOption("renderDistance", cap);
        capOption("simulationDistance", cap);
        MCOptionUtils.save();
        try {
            marker.createNewFile();
        } catch (IOException e) {
            Log.w(TAG, "Could not write the marker file", e);
        }
    }

    /** Lowest short side (px) the game surface is scaled down to on weak GPUs */
    public static int targetShortSide(GpuUtils.GLInfo gpu) {
        if(!LauncherPreferences.PREF_PERF_TWEAKS) return 540;
        if(!gpu.isPowerVR()) return 540;
        return LauncherPreferences.PREF_PERF_HIGH_FPS ? 360 : 432;
    }

    /** Initial heap size (MB): same as the max so the heap never has to grow during play */
    public static int initialHeapMb(int maxHeapMb) {
        if(!LauncherPreferences.PREF_PERF_TWEAKS) return Math.min(128, maxHeapMb);
        return maxHeapMb;
    }

    /** Extra JVM flags for steady frame times (fewer, shorter GC pauses) */
    public static void addStableJvmArgs(java.util.List<String> args) {
        if(!LauncherPreferences.PREF_PERF_TWEAKS) return;
        // Half of the heap as young generation: minor GCs become rare
        args.add(0, "-XX:NewRatio=1");
        // Skip bytecode verification of loaded classes: shorter loading, no effect on FPS
        if(LauncherPreferences.PREF_PERF_FAST_LOAD) {
            args.add(0, "-XX:-BytecodeVerificationRemote");
            // Must come before the diagnostic option, newer JVMs refuse to start otherwise
            args.add(0, "-XX:+UnlockDiagnosticVMOptions");
        }
    }
}
