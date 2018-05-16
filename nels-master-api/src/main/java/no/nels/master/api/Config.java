package no.nels.master.api;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.master.api.constants.ConfigName;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public final class Config {
    private static Config instance;
    private static Logger logger = LoggerFactory.getLogger(Config.class);
    private Properties properties;
    private String defaultConfigPath = "../conf/config.properties";

    private Config() throws IOException {
        String configPath = System.getProperty("config.path");
        this.properties = new Properties();
        if (configPath != null && !configPath.isEmpty()) {
            this.properties.load(new FileInputStream(configPath));
        } else {
            this.properties.load(new FileInputStream(defaultConfigPath));
        }
        Enumeration keys = this.properties.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = (String)this.properties.get(key);
            logger.debug("Config values:" + key + ":" + value);
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
