package git.artdeell.mojo.utils;

import git.artdeell.mojo.prefs.LauncherPreferences;

import java.util.List;

/** JVM launch tweaks: Fast load and the initial heap size. */
public class PerfTweaks {
    /** Initial heap size (MB) */
    public static int initialHeapMb(int maxHeapMb) {
        return Math.min(128, maxHeapMb);
    }

    /** Fast load: skip bytecode verification of loaded classes (shorter loading, no effect on FPS) */
    public static void addFastLoadArgs(List<String> args) {
        if(!LauncherPreferences.PREF_PERF_FAST_LOAD) return;
        args.add(0, "-XX:-BytecodeVerificationRemote");
        // Must come before the diagnostic option, newer JVMs refuse to start otherwise
        args.add(0, "-XX:+UnlockDiagnosticVMOptions");
    }
}
