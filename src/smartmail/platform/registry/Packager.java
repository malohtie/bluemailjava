package smartmail.platform.registry;

import java.util.HashMap;

public class Packager {
    private HashMap registry;

    private static Packager instance;

    public static Packager getInstance() {
        if (instance == null)
            instance = new Packager();
        return instance;
    }

    private Packager() {
        this.registry = new HashMap<>();
    }

    public HashMap getRegistry() {
        return this.registry;
    }

    public void setRegistry(HashMap registry) {
        this.registry = registry;
    }
}
