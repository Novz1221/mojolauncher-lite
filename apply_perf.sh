#!/usr/bin/env bash
# Pasang tweak FPS dengan sed/grep (tanpa Python). Jalankan dari root repo (folder berisi app_mojo).
M=app_mojo/src/main
J=$M/java/git/artdeell/mojo
FAIL=0
T=$(mktemp)

need() { [ -f "$1" ] || { echo "HILANG: $1"; FAIL=1; return 1; }; }
# cek bahwa pola ada di file
has() { grep -q -- "$2" "$1" || { echo "ANCHOR TIDAK KETEMU di $1: $2"; FAIL=1; return 1; }; }

# ---------- PerfTweaks.java (file baru) ----------
cat > $J/utils/PerfTweaks.java <<'PERFEOF'
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
    }
}
PERFEOF

# ---------- GpuUtils ----------
F=$J/utils/GpuUtils.java
if need $F && ! grep -q isPowerVR $F && has $F 'public boolean isArm()'; then
cat > $T <<'EOF'

        /**
         * Check if this GLInfo belongs to an Imagination PowerVR graphics adapter
         * @return
         */
        public boolean isPowerVR() {
            return renderer.contains("PowerVR") || vendor.contains("Imagination");
        }
EOF
sed -i '/public boolean isArm()/,/^        }/{/^        }/r '$T'
}' $F
fi

# ---------- LauncherPreferences ----------
F=$J/prefs/LauncherPreferences.java
if need $F && ! grep -q PREF_PERF_TWEAKS $F; then
has $F 'PREF_BIG_CORE_AFFINITY = false;' && sed -i '/^    public static boolean PREF_BIG_CORE_AFFINITY = false;/a\    public static boolean PREF_PERF_TWEAKS = true;\n    public static boolean PREF_PERF_HIGH_FPS = true;' $F
has $F 'PREF_RENDERER = DEFAULT_PREF.getString("renderer"' && sed -i '/PREF_RENDERER = DEFAULT_PREF.getString("renderer"/a\        PREF_PERF_TWEAKS = DEFAULT_PREF.getBoolean("perfTweaks", true);\n        PREF_PERF_HIGH_FPS = DEFAULT_PREF.getBoolean("perfHighFps", true);' $F
has $F 'getBoolean("sustainedPerformance", false)' && sed -i 's/getBoolean("sustainedPerformance", false)/getBoolean("sustainedPerformance", PREF_PERF_TWEAKS \&\& !PREF_PERF_HIGH_FPS)/' $F
has $F 'getBoolean("bigCoreAffinity", false)' && sed -i 's/getBoolean("bigCoreAffinity", false)/getBoolean("bigCoreAffinity", PREF_PERF_TWEAKS \&\& !hasAllCoreSameFreq())/' $F
has $F 'int targetSide = 540;' && sed -i 's/int targetSide = 540;/int targetSide = git.artdeell.mojo.utils.PerfTweaks.targetShortSide(GpuUtils.getGlInfo());/' $F
fi

# ---------- GLESRenderSpec ----------
F=$J/game/renderer/impl/GLESRenderSpec.java
if need $F && ! grep -q PerfTweaks $F; then
has $F 'import git.artdeell.mojo.utils.JREUtils;' && sed -i '/^import git.artdeell.mojo.utils.JREUtils;/a\import git.artdeell.mojo.utils.PerfTweaks;' $F
has $F 'envMap.put("LIBGL_NOERROR", "1");' && sed -i '/envMap.put("LIBGL_NOERROR", "1");/a\        PerfTweaks.applyGlesEnv(envMap);' $F
fi

# ---------- MesaRenderSpec ----------
F=$J/game/renderer/impl/MesaRenderSpec.java
if need $F && ! grep -q PerfTweaks $F; then
has $F 'import git.artdeell.mojo.utils.GpuUtils;' && sed -i '/^import git.artdeell.mojo.utils.GpuUtils;/a\import git.artdeell.mojo.utils.PerfTweaks;' $F
has $F 'envMap.put("MESA_GLSL_CACHE_DIR"' && sed -i '/envMap.put("MESA_GLSL_CACHE_DIR"/a\        PerfTweaks.applyMesaEnv(envMap);' $F
fi

# ---------- JavaRunner ----------
F=$J/utils/jre/JavaRunner.java
if need $F && ! grep -q PerfTweaks $F; then
has $F 'import git.artdeell.mojo.prefs.LauncherPreferences;' && sed -i '/^import git.artdeell.mojo.prefs.LauncherPreferences;/a\import git.artdeell.mojo.utils.PerfTweaks;' $F
has $F '"-XX:TieredStopAtLevel=1"' && sed -i '/"-XX:TieredStopAtLevel=1"/i\        PerfTweaks.addStableJvmArgs(userArguments);' $F
has $F 'Math.min(128, LauncherPreferences.PREF_RAM_ALLOCATION)' && sed -i 's/Math.min(128, LauncherPreferences.PREF_RAM_ALLOCATION)/PerfTweaks.initialHeapMb(LauncherPreferences.PREF_RAM_ALLOCATION)/' $F
fi

# ---------- GameActivity ----------
F=$J/game/GameActivity.java
if need $F && ! grep -q PerfTweaks $F; then
has $F 'import git.artdeell.mojo.utils.MCOptionUtils;' && sed -i '/^import git.artdeell.mojo.utils.MCOptionUtils;/a\import git.artdeell.mojo.utils.PerfTweaks;' $F
has $F 'MCOptionUtils.load(instance.getGameDirectory().getAbsolutePath());' && sed -i '/MCOptionUtils.load(instance.getGameDirectory().getAbsolutePath());/a\        PerfTweaks.applyGameOptions(instance.getGameDirectory());' $F
fi

# ---------- pref_video.xml ----------
F=$M/res/xml/pref_video.xml
if need $F && ! grep -q perfTweaks $F && has $F 'preference_sustained_performance_title'; then
cat > $T <<'EOF'
        <androidx.preference.SwitchPreferenceCompat
            android:defaultValue="true"
            android:key="perfTweaks"
            android:summary="@string/preference_perf_tweaks_description"
            android:title="@string/preference_perf_tweaks_title"
            />

        <androidx.preference.SwitchPreferenceCompat
            android:defaultValue="true"
            android:key="perfHighFps"
            android:summary="@string/preference_perf_high_fps_description"
            android:title="@string/preference_perf_high_fps_title"
            />

EOF
N=$(grep -n 'preference_sustained_performance_title' $F | head -1 | cut -d: -f1)
# blok switch dimulai satu baris sebelum baris title; sisipkan sebelum baris itu
sed -i "$((N-2))r $T" $F
fi

# ---------- strings.xml ----------
F=$M/res/values/strings.xml
if need $F && ! grep -q preference_perf_tweaks_title $F && has $F '</resources>'; then
sed -i '/<\/resources>/d' $F
cat >> $F <<'EOF'
    <string name="preference_perf_tweaks_title">FPS boost (PowerVR, Mali, Adreno)</string>
    <string name="preference_perf_tweaks_description">Applies GPU-aware tweaks: faster renderer settings, lighter in-game options on first launch, bigger starting heap and big-core pinning.</string>
    <string name="preference_perf_high_fps_title">High FPS mode</string>
    <string name="preference_perf_high_fps_description">No FPS cap, lower resolution and shorter render distance on PowerVR. Turn off for steadier but lower FPS.</string>
</resources>
EOF
fi

rm -f $T
if [ $FAIL -ne 0 ]; then echo "SELESAI DENGAN PERINGATAN (lihat di atas)"; exit 1; fi
echo "OK: semua tweak terpasang"
