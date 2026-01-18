package moe.ouom.wekit.loader;

import android.content.Context;

import java.util.Objects;

public class TransitClassLoader extends ClassLoader {

    private static final ClassLoader sSystem = Context.class.getClassLoader();
    private static final ClassLoader sCurrent = TransitClassLoader.class.getClassLoader();

    public TransitClassLoader() {
        super(sSystem);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name != null && name.startsWith("moe.ouom.wekit.loader.")) {
            return Objects.requireNonNull(sCurrent).loadClass(name);
        }
        return super.findClass(name);
    }

}
