package smartmail.platform.utils;

import org.apache.commons.io.FileUtils;
import smartmail.platform.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;

public class Mapper {
    public static Object getMapValue(TreeMap map, String key, Object defaultValue) {
        Object value;
        if (map != null && !map.isEmpty() && map.containsKey(key) && (value = map.get(key)) != null)
            return value;
        return defaultValue;
    }

    public static Object getMapValue(HashMap map, String key, Object defaultValue) {
        Object value;
        if (map != null && !map.isEmpty() && map.containsKey(key) && (value = map.get(key)) != null)
            return value;
        return defaultValue;
    }

    public static HashMap<String, String> readProperties(String filePath) {
        HashMap<String, String> results = new HashMap<>();
        Properties properties = new Properties();
        try {
            FileInputStream in = FileUtils.openInputStream(new File(filePath));
            properties.load(in);
            properties.stringPropertyNames().forEach(key -> {
                String value = properties.getProperty(key);
                results.put(key, value);
            });
        } catch (IOException e) {
            Logger.error(e, Mapper.class);
        }
        return results;
    }
}
