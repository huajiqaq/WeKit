package moe.ouom.wekit.hooks._base;

import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import moe.ouom.wekit.config.ConfigManager;
import moe.ouom.wekit.constants.Constants;
import moe.ouom.wekit.util.common.SyncUtils;

public abstract class BaseSwitchFunctionHookItem extends BaseHookItem {

    private boolean enabled;
    private final int targetProcess = targetProcess();


    public View.OnClickListener getOnClickListener() {
        return null;
    }

    /**
     * 目标进程
     */
    public int targetProcess() {
        return SyncUtils.PROC_MAIN;
    }


    public int getTargetProcess() {
        return targetProcess;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected final void tryExecute(XC_MethodHook.MethodHookParam param, HookAction hookAction) {
        if (isEnabled()) {
            super.tryExecute(param, hookAction);
        }
    }

    public boolean configIsEnable() {
        return ConfigManager.getDefaultConfig().getBooleanOrFalse(Constants.PrekXXX+this.getPath());
    }

}
