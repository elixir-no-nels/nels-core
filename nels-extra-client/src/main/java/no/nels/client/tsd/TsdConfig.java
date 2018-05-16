package no.nels.client.tsd;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class TsdConfig {
    private static String tsdRootUrl;

    private static String tsdApiUsername;
    private static String tsdApiPassword;

    private static String masterApiUrl;

    private static String masterApiUsername;
    private static String masterApiPassword;

    private static String timeout;

    public static URI constructFullUrl(String pathTemplate, Object... objects) {
        return UriBuilder.fromUri(getTsdRootUrl()).path(pathTemplate).build(objects);
    }

    public static String getTimeout() {
        return timeout;
    }

    public static void setTimeout(String timeout) {
        TsdConfig.timeout = timeout;
    }

    public static String getTsdRootUrl() {
        return tsdRootUrl;
    }

    public static void setTsdRootUrl(String tsdRootUrl) {
        TsdConfig.tsdRootUrl = tsdRootUrl;
    }

    public static String getMasterApiUrl() {
        return masterApiUrl;
    }

    public static void setMasterApiUrl(String masterApiUrl) {
        TsdConfig.masterApiUrl = masterApiUrl;
    }

    public static String getMasterApiUsername() {
        return masterApiUsername;
    }

    public static void setMasterApiUsername(String masterApiUsername) {
        TsdConfig.masterApiUsername = masterApiUsername;
    }

    public static String getMasterApiPassword() {
        return masterApiPassword;
    }

    public static void setMasterApiPassword(String masterApiPassword) {
        TsdConfig.masterApiPassword = masterApiPassword;
    }

    public static String getTsdApiUsername() {
        return tsdApiUsername;
    }

    public static void setTsdApiUsername(String tsdApiUsername) {
        TsdConfig.tsdApiUsername = tsdApiUsername;
    }

    public static String getTsdApiPassword() {
        return tsdApiPassword;
    }

    public static void setTsdApiPassword(String tsdApiPassword) {
        TsdConfig.tsdApiPassword = tsdApiPassword;
    }
}
