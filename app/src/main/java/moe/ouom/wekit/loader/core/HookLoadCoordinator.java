package moe.ouom.wekit.loader.core;

import android.app.Application;

import java.util.concurrent.atomic.AtomicBoolean;

import moe.ouom.wekit.util.log.WeLogger;

public final class HookLoadCoordinator {
    private static final String TAG = "HookLoadCoordinator";
    private static final AtomicBoolean sLateInitDone = new AtomicBoolean(false);
    private static final AtomicBoolean sLoadTriggered = new AtomicBoolean(false);
    private static volatile Integer sPendingProcessType = null;

    private HookLoadCoordinator() {
        throw new AssertionError("No instance for you!");
    }

    public static void requestLoad(int processType) {
        sPendingProcessType = processType;
        if (sLateInitDone.get()) {
            triggerLoadIfNeeded(processType);
        } else {
            WeLogger.i(TAG, "Deferring hook load until Application.onCreate");
        }
    }

    public static void onApplicationCreate(Application app) {
        if (!sLateInitDone.compareAndSet(false, true)) {
            return;
        }
        WeLogger.i(TAG, "Application.onCreate observed, triggering deferred hook load");
        Integer pending = sPendingProcessType;
        if (pending != null) {
            triggerLoadIfNeeded(pending);
        }
    }

    private static void triggerLoadIfNeeded(int processType) {
        if (!sLoadTriggered.compareAndSet(false, true)) {
            return;
        }
        try {
            SecretLoader.load(processType);
        } catch (Throwable e) {
            WeLogger.e(TAG, "Failed to load modules via SecretLoader", e);
        }
    }
}
