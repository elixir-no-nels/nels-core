package no.nels.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class Config {
    private static Config instance;

    private Properties properties;

    private String configPath = "../conf/config.properties";

    private Config() throws IOException {
        this.properties = new Properties();
        this.properties.load(new FileInputStream(configPath));
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
