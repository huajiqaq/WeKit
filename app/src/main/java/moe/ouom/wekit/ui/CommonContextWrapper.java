package moe.ouom.wekit.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;

import moe.ouom.wekit.util.common.ModuleRes;
import moe.ouom.wekit.util.log.Logger;

/**
 * 为解决 Xposed 模块 UI 注入时的环境冲突设计
 * <p>
 * 它可以：
 * 1. 资源代理：将 Resources/Theme 代理到 ModuleRes，确保能正确加载模块内的 Layout 和 Style
 * 2. ClassLoader 统一：重写 getClassLoader() 返回模块原本的加载器，而非 createPackageContext 生成的副本
 * 3. View 创建拦截：注入自定义 LayoutInflater Factory，强制 XML 中的控件由模块 ClassLoader 加载，
 * 解决宿主与模块之间的 "ClassCastException" 类隔离冲突问题。
 */
public class CommonContextWrapper extends ContextWrapper {

    private final Resources.Theme mTheme;
    private final int mThemeResource;
    private LayoutInflater mInflater;

    public CommonContextWrapper(Context base, int themeResId) {
        super(base);
        this.mThemeResource = themeResId;

        // 资源依然要用 ModuleRes 的，因为那是专门用来查资源的 Context
        if (ModuleRes.getContext() != null) {
            this.mTheme = ModuleRes.getContext().getResources().newTheme();
            this.mTheme.setTo(ModuleRes.getContext().getTheme());
        } else {
            this.mTheme = base.getResources().newTheme();
        }
        this.mTheme.applyStyle(mThemeResource, true);
    }

    /**
     * 返回当前类的 ClassLoader (Xposed Loader)
     * 这样 XML 解析出来的类，和代码里引用的类，才是同一个 Loader 加载的
     */
    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    // ---------------------------------------------------------
    // 资源查找代理给 ModuleRes
    // ---------------------------------------------------------
    @Override
    public Resources getResources() {
        return ModuleRes.getContext().getResources();
    }

    @Override
    public AssetManager getAssets() {
        return ModuleRes.getContext().getAssets();
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme;
    }

    @Override
    public void setTheme(int resid) {
        mTheme.applyStyle(resid, true);
    }

    // ---------------------------------------------------------
    // Factory 拦截逻辑
    // ---------------------------------------------------------
    @Override
    public Object getSystemService(String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater == null) {
                mInflater = new ModuleLayoutInflater(LayoutInflater.from(getBaseContext()), this);
            }
            return mInflater;
        }
        return super.getSystemService(name);
    }

    private static class ModuleLayoutInflater extends LayoutInflater {
        private static final String[] sClassPrefixList = {
                "android.widget.",
                "android.webkit.",
                "android.app."
        };

        protected ModuleLayoutInflater(LayoutInflater original, Context newContext) {
            super(original, newContext);
            // 这里 newContext.getClassLoader() 会调用上面我们修复过的方法
            setFactory2(new ModuleFactory(newContext.getClassLoader()));
        }

        @Override
        public LayoutInflater cloneInContext(Context newContext) {
            return new ModuleLayoutInflater(this, newContext);
        }

        @Override
        protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
            for (String prefix : sClassPrefixList) {
                try {
                    View view = createView(name, prefix, attrs);
                    if (view != null) {
                        return view;
                    }
                } catch (ClassNotFoundException e) { }
            }
            return super.onCreateView(name, attrs);
        }
    }

    private static class ModuleFactory implements LayoutInflater.Factory2 {
        private final ClassLoader mClassLoader;

        public ModuleFactory(ClassLoader cl) {
            this.mClassLoader = cl;
        }

        @Nullable
        @Override
        public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
            if (name.contains(".")) {
                try {
                    Class<?> clazz = mClassLoader.loadClass(name);
                    Constructor<? extends View> constructor = clazz.asSubclass(View.class).getConstructor(Context.class, AttributeSet.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(context, attrs);
                } catch (Exception e) {
                    // 加载失败 (如 FrameLayout) 则忽略
                }
            }
            return null;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
            return onCreateView(null, name, context, attrs);
        }
    }

    public static Context createAppCompatContext(@NonNull Context base) {
        int themeId = ModuleRes.getId("Theme.WeKit", "style");
        if (themeId == 0) {
            themeId = android.R.style.Theme_DeviceDefault_Light_NoActionBar;
        }
        return new CommonContextWrapper(base, themeId);
    }
}