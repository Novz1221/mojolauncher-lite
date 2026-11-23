package git.artdeell.mojo.game;

import android.app.Activity;
import android.app.GameManager;
import android.app.GameState;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PerformanceHintManager;
import android.os.Process;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "More Performance": asks Android to run the game as fast as the device allows.
 * It never touches Minecraft's own settings. It only:
 *  - requests the highest refresh rate of the current resolution,
 *  - tells the system the game is running (Game Mode state, Android 13+),
 *  - gives the busiest game threads to the Android performance hint service (Android 12+),
 *    which moves them to faster cores and raises CPU frequency,
 *  - raises the priority of the busiest threads.
 * Everything is best-effort: any call the device rejects is skipped.
 */
public class PerformanceBooster {
    private static final String TAG = "MorePerformance";
    private static final int MAX_HINT_THREADS = 6;
    private static final int MAX_PRIORITY_THREADS = 3;
    private static final long FIRST_SCAN_MS = 1000;
    private static final long SCAN_INTERVAL_MS = 3000;

    private final Activity mActivity;
    private final Map<Integer, Long> mLastTicks = new HashMap<>();
    private HandlerThread mThread;
    private Handler mHandler;
    private volatile boolean mRunning = false;

    // Only touched from mHandler's thread
    private Object mSession;
    private int[] mSessionTids = new int[0];
    private long mTargetNs = 16_666_666L;
    private long mReportIntervalMs = 16;

    public PerformanceBooster(Activity activity) {
        mActivity = activity;
    }

    /** Call from the UI thread (onResume) */
    public void start() {
        if(mRunning) return;
        mRunning = true;
        applyDisplayMode();
        setGameState(true);
        mThread = new HandlerThread("MorePerformance");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
        mHandler.postDelayed(mScanTask, FIRST_SCAN_MS);
        mHandler.post(mReportTask);
        Log.i(TAG, "started");
    }

    /** Call from the UI thread (onPause) */
    public void stop() {
        if(!mRunning) return;
        mRunning = false;
        setGameState(false);
        final HandlerThread thread = mThread;
        if(mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.post(() -> {
                closeSession();
                thread.quitSafely();
            });
        }
        mHandler = null;
        mThread = null;
        Log.i(TAG, "stopped");
    }

    private final Runnable mScanTask = new Runnable() {
        @Override
        public void run() {
            if(!mRunning || mHandler == null) return;
            try {
                int[] tids = pickBusyThreads();
                if(tids.length > 0) {
                    updateSession(tids);
                    raisePriority(tids);
                }
            } catch (Throwable t) {
                Log.w(TAG, "scan failed", t);
            }
            if(mRunning && mHandler != null) mHandler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

    private final Runnable mReportTask = new Runnable() {
        @Override
        public void run() {
            if(!mRunning || mHandler == null) return;
            if(Build.VERSION.SDK_INT >= 31 && mSession != null) {
                try {
                    // Report that we are slightly late so the system keeps frequencies high
                    ((PerformanceHintManager.Session) mSession)
                            .reportActualWorkDuration(mTargetNs + mTargetNs / 4);
                } catch (Throwable t) {
                    Log.w(TAG, "report failed", t);
                    closeSession();
                }
            }
            if(mRunning && mHandler != null) mHandler.postDelayed(this, mReportIntervalMs);
        }
    };

    private void applyDisplayMode() {
        if(Build.VERSION.SDK_INT < 23) return;
        try {
            Window window = mActivity.getWindow();
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            Display.Mode current = display.getMode();
            Display.Mode best = current;
            for(Display.Mode mode : display.getSupportedModes()) {
                if(mode.getPhysicalWidth() == current.getPhysicalWidth()
                        && mode.getPhysicalHeight() == current.getPhysicalHeight()
                        && mode.getRefreshRate() > best.getRefreshRate()) {
                    best = mode;
                }
            }
            WindowManager.LayoutParams params = window.getAttributes();
            params.preferredDisplayModeId = best.getModeId();
            window.setAttributes(params);
            float rate = best.getRefreshRate();
            if(rate > 1) {
                mTargetNs = (long) (1_000_000_000d / rate);
                mReportIntervalMs = Math.max(4, Math.min(33, (long) (1000d / rate)));
            }
            Log.i(TAG, "requested refresh rate " + rate + " Hz");
        } catch (Throwable t) {
            Log.w(TAG, "display mode failed", t);
        }
    }

    private void setGameState(boolean playing) {
        if(Build.VERSION.SDK_INT < 33) return;
        try {
            GameManager manager = mActivity.getSystemService(GameManager.class);
            if(manager == null) return;
            manager.setGameState(new GameState(false, playing
                    ? GameState.MODE_GAMEPLAY_UNINTERRUPTIBLE
                    : GameState.MODE_NONE));
        } catch (Throwable t) {
            Log.w(TAG, "game state failed", t);
        }
    }

    private void updateSession(int[] tids) {
        if(Build.VERSION.SDK_INT < 31) return;
        int[] sorted = tids.clone();
        Arrays.sort(sorted);
        if(mSession != null && Arrays.equals(sorted, mSessionTids)) return;
        try {
            if(Build.VERSION.SDK_INT >= 34 && mSession != null) {
                ((PerformanceHintManager.Session) mSession).setThreads(sorted);
                mSessionTids = sorted;
                return;
            }
        } catch (Throwable t) {
            Log.w(TAG, "setThreads failed, recreating session", t);
        }
        closeSession();
        try {
            PerformanceHintManager manager = mActivity.getSystemService(PerformanceHintManager.class);
            if(manager == null) return;
            long preferred = manager.getPreferredUpdateRateNanos();
            if(preferred > 0) {
                mReportIntervalMs = Math.max(4, Math.min(33, preferred / 1_000_000L));
            }
            mSession = manager.createHintSession(sorted, mTargetNs);
            mSessionTids = sorted;
            Log.i(TAG, mSession != null ? "hint session created for " + sorted.length + " threads"
                    : "performance hints are not supported on this device");
        } catch (Throwable t) {
            Log.w(TAG, "createHintSession failed", t);
            mSession = null;
        }
    }

    private void closeSession() {
        if(mSession == null || Build.VERSION.SDK_INT < 31) return;
        try {
            ((PerformanceHintManager.Session) mSession).close();
        } catch (Throwable ignored) {}
        mSession = null;
        mSessionTids = new int[0];
    }

    private void raisePriority(int[] tids) {
        int count = Math.min(tids.length, MAX_PRIORITY_THREADS);
        for(int i = 0; i < count; i++) {
            try {
                Process.setThreadPriority(tids[i], Process.THREAD_PRIORITY_URGENT_DISPLAY);
            } catch (Throwable ignored) {}
        }
    }

    /** Returns the threads of this process that used the most CPU since the previous scan */
    private int[] pickBusyThreads() {
        String[] names = new File("/proc/self/task").list();
        if(names == null) return new int[0];
        List<long[]> busy = new ArrayList<>();
        Map<Integer, Long> current = new HashMap<>();
        for(String name : names) {
            int tid;
            try {
                tid = Integer.parseInt(name);
            } catch (NumberFormatException e) {
                continue;
            }
            long ticks = readTicks(tid);
            if(ticks < 0) continue;
            current.put(tid, ticks);
            Long last = mLastTicks.get(tid);
            if(last != null && ticks - last > 0) busy.add(new long[]{tid, ticks - last});
        }
        mLastTicks.clear();
        mLastTicks.putAll(current);
        busy.sort((a, b) -> Long.compare(b[1], a[1]));
        int count = Math.min(busy.size(), MAX_HINT_THREADS);
        int[] result = new int[count];
        for(int i = 0; i < count; i++) result[i] = (int) busy.get(i)[0];
        return result;
    }

    /** utime + stime of a thread, in clock ticks, or -1 */
    private static long readTicks(int tid) {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/task/" + tid + "/stat"))) {
            String line = reader.readLine();
            if(line == null) return -1;
            int close = line.lastIndexOf(')');
            if(close < 0) return -1;
            // fields after the ")" start at field 3 (state), so utime is [11] and stime is [12]
            String[] fields = line.substring(close + 2).split(" ");
            return Long.parseLong(fields[11]) + Long.parseLong(fields[12]);
        } catch (Exception e) {
            return -1;
        }
    }
}
