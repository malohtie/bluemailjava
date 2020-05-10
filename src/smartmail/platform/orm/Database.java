package smartmail.platform.orm;

import smartmail.platform.logging.Logger;
import smartmail.platform.registry.Packager;
import smartmail.platform.utils.Mapper;

import java.io.File;
import java.util.HashMap;

public class Database {
    public static synchronized void init() {
        try {
            String dataSourcePath = (new File(System.getProperty("base.path"))).getAbsolutePath() + "/applications/bluemail/configs/databases.ini";
            HashMap<String, String> map = Mapper.readProperties(dataSourcePath);
            if (map != null && !map.isEmpty()) {
                String[] databases = { "master", "lists" };
                String defaultKey = "master";
                Packager.getInstance().getRegistry().put("default-databases-key", defaultKey);
                for (String databaseKey : databases) {
                    Connector connector = new Connector();
                    connector.setKey(databaseKey);
                    connector.setDriver(String.valueOf(Mapper.getMapValue(map, databaseKey + ".type", "pgsql")));
                    if ("pg".equalsIgnoreCase(connector.getDriver()))
                        connector.setDriver("pgsql");
                    connector.setHost(String.valueOf(Mapper.getMapValue(map, databaseKey + ".host", "")).trim());
                    connector.setPort(Integer.parseInt(String.valueOf(Mapper.getMapValue(map, databaseKey + ".port", "0"))));
                    connector.setUsername(String.valueOf(Mapper.getMapValue(map, databaseKey + ".user", "")).trim());
                    connector.setPassword(String.valueOf(Mapper.getMapValue(map, databaseKey + ".password", "")).trim());
                    connector.setName(String.valueOf(Mapper.getMapValue(map, databaseKey + ".dbname", "")).trim());
                    connector.iniDataSource();
                    Packager.getInstance().getRegistry().put(databaseKey, connector);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            Logger.error(e, Database.class);
        }
    }

    public static synchronized boolean exists(String key) {
        return (Packager.getInstance().getRegistry().containsKey(key) && Packager.getInstance().getRegistry().get(key) != null && Packager.getInstance().getRegistry().get(key) instanceof Connector);
    }

    public static synchronized Connector get(String key) {
        return exists(key) ? (Connector)Packager.getInstance().getRegistry().get(key) : null;
    }

    public static synchronized Connector getDefault() {
        return exists((String)Packager.getInstance().getRegistry().get("default-databases-key")) ? (Connector)Packager.getInstance().getRegistry().get(Packager.getInstance().getRegistry().get("default-databases-key")) : null;
    }

    public static synchronized void setDefault(String key) {
        if (exists((String)Packager.getInstance().getRegistry().get("default-databases-key")))
            Packager.getInstance().getRegistry().put("default-databases-key", key);
    }
}
