package moe.ouom.wekit.config;

import java.util.Map;

public class WeKitConf {
    public static void setString(String setName, String key, String value){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        manager.putString(setName + ":" + key,value);
    }
    public static String getString(String setName, String key, String defValue){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        return manager.getString(setName + ":" + key,defValue);
    }
    public static boolean getBoolean(String setName, String key, boolean defValue){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        return manager.getBoolean(setName + ":" + key,defValue);
    }
    public static void setBoolean(String setName, String key, boolean value){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        manager.putBoolean(setName + ":" + key,value);
    }
    public static int getInt(String setName, String key, int defValue){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        return manager.getInt(setName + ":" + key,defValue);
    }
    public static void setInt(String setName, String key, int value){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        manager.putInt(setName + ":" + key,value);
    }
    public static void removeConfig(String setName){
        ConfigManager manager = ConfigManager.getDefaultConfig();
        Map<String, ?> map = manager.getAll();
        map.keySet().removeIf(key -> key.startsWith(setName + ":"));
    }
}
