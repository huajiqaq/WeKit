package moe.ouom.wekit.util.io;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import moe.ouom.wekit.host.impl.HostInfo;
import moe.ouom.wekit.util.log.Logger;


public class PathTool {


    public static String getDataSavePath(Context context, String dirName) {
        // getExternalFilesDir()：SDCard/Android/data/你的应用的包名/files/dirName
        return context.getExternalFilesDir(dirName).getAbsolutePath();
    }

    public static String getStorageDirectory() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getModuleDataPath() {
        String directory = getStorageDirectory() + "/Android/data/" + HostInfo.getHostInfo().getPackageName() + "/WeKit";
        File file = new File(directory);
        if (!file.exists()) {
            Logger.d("file.mkdirs(): " + file.mkdirs());
        }
        return directory;
    }

    public static String getModuleCachePath(String dirName) {
        File cache = new File(getModuleDataPath() + "/cache/" + dirName);
        if (!cache.exists()) Logger.d("cache.mkdirs(): " + cache.mkdirs());
        return cache.getAbsolutePath();
    }


}
