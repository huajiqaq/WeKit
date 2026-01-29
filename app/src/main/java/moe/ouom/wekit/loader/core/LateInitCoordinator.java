package moe.ouom.wekit.loader.core;

import android.app.Application;

import java.util.concurrent.atomic.AtomicBoolean;

import moe.ouom.wekit.config.RuntimeConfig;
import moe.ouom.wekit.dexkit.cache.DexCacheManager;
import moe.ouom.wekit.util.common.SyncUtils;
import moe.ouom.wekit.util.log.WeLogger;

public final class LateInitCoordinator {
    private static final String TAG = "LateInitCoordinator";
    private static final AtomicBoolean sInitialized = new AtomicBoolean(false);

    private LateInitCoordinator() {
        throw new AssertionError("No instance for you!");
    }

    public static void onApplicationCreate(Application app) {
        if (!sInitialized.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader hostClassLoader = RuntimeConfig.getHostClassLoader();
            if (hostClassLoader != null) {
                DexCacheManager.INSTANCE.updateDexSetHash(hostClassLoader);
            }
        } catch (Throwable e) {
            WeLogger.e(TAG, "Failed to update dex set hash in late init", e);
        }
        WeLogger.i(TAG, "Late init done in process: " + SyncUtils.getProcessName());
        HookLoadCoordinator.onApplicationCreate(app);
    }
}
