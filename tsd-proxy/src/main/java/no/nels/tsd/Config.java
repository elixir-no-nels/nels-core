package no.nels.tsd;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class Config {
    private static Config instance;

    private Properties properties;
    private String defaultConfigPath = "../conf/tsd.service.properties";

    private Config() throws IOException {
        String configPath = System.getProperty("config.path");
        this.properties = new Properties();
        if (configPath != null && !configPath.isEmpty()) {
            this.properties.load(new FileInputStream(configPath));
        } else {
            this.properties.load(new FileInputStream(defaultConfigPath));
        }
    }

    public static void init() throws IOException {
        if (instance == null) {
            instance = new Config();
        }
    }

    public static String valueOf(String key) {
        return instance.properties.getProperty(key);
    }
}
