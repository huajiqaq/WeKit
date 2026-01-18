package moe.ouom.wekit.loader.startup;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;

import moe.ouom.wekit.host.impl.HostInfo;
import moe.ouom.wekit.loader.core.NativeCoreBridge;
import moe.ouom.wekit.loader.core.WeLauncher;
import moe.ouom.wekit.loader.hookimpl.InMemoryClassLoaderHelper;
import moe.ouom.wekit.loader.hookimpl.LibXposedNewApiByteCodeGenerator;
import moe.ouom.wekit.util.Initiator;
import moe.ouom.wekit.util.common.SyncUtils;
import moe.ouom.wekit.util.log.Logger;

public class StartupRoutine {

    private StartupRoutine() {
        throw new AssertionError("No instance for you!");
    }

    /**
     * From now on, kotlin, androidx or third party libraries may be accessed without crashing the ART.
     * <p>
     * Kotlin and androidx are dangerous, and should be invoked only after the class loader is ready.
     *
     * @param ctx         Application context for host
     * @param step        Step instance
     * @param lpwReserved null, not used
     * @param bReserved   false, not used
     */
    public static void execPostStartupInit(@NonNull Context ctx, @Nullable Object step, String lpwReserved, boolean bReserved) {
        // init all kotlin utils here
        HostInfo.init((Application) ctx);
        Initiator.init(ctx.getClassLoader());
        // perform full initialization for native core -- including primary and secondary native libraries
        StartupInfo.getLoaderService().setClassLoaderHelper(InMemoryClassLoaderHelper.INSTANCE);
        LibXposedNewApiByteCodeGenerator.init();
        NativeCoreBridge.initNativeCore();

        // ------------------------------------------

        Logger.d("execPostStartupInit -> processName: " + SyncUtils.getProcessName());
        WeLauncher launcher = new WeLauncher();
        launcher.init(ctx.getClassLoader(), ctx.getApplicationInfo(), ctx.getApplicationInfo().sourceDir, ctx);
    }

}
