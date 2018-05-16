package no.nels.client.sbi;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

public final class SbiConfig {
    //https://storebio2.norstore.uio.no:8070
    private static String sbiRootUrl;

    private static String sbiApiUsername;
    private static String sbiApiPassword;

    private static String masterApiUrl;

    private static String masterApiUsername;
    private static String masterApiPassword;

    private static String timeout;

    public static URI constructFullUrl(String pathTemplate, Object... objects) {
        return UriBuilder.fromUri(getSbiRootUrl()).path(pathTemplate).build(objects);
    }

    public static URI constructFullUrl(String pathTemplate, Object[] objects, Map<String, String> queryParam) {
        UriBuilder uriBuilder = UriBuilder.fromUri(getSbiRootUrl()).path(pathTemplate);
        for (Map.Entry<String, String> entry : queryParam.entrySet()) {
            uriBuilder = uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriBuilder.build(objects);
    }

    public static String getTimeout() {
        return timeout;
    }

    public static void setTimeout(String timeout) {
        SbiConfig.timeout = timeout;
    }

    public static String getSbiRootUrl() {
        return sbiRootUrl;
    }

    public static void setSbiRootUrl(String sbiRootUrl) {
        SbiConfig.sbiRootUrl = sbiRootUrl;
    }

    public static String getMasterApiUrl() {
        return masterApiUrl;
    }

    public static void setMasterApiUrl(String masterApiUrl) {
        SbiConfig.masterApiUrl = masterApiUrl;
    }

    public static String getMasterApiUsername() {
        return masterApiUsername;
    }

    public static void setMasterApiUsername(String masterApiUsername) {
        SbiConfig.masterApiUsername = masterApiUsername;
    }

    public static String getMasterApiPassword() {
        return masterApiPassword;
    }

    public static void setMasterApiPassword(String masterApiPassword) {
        SbiConfig.masterApiPassword = masterApiPassword;
    }

    public static String getSbiApiUsername() {
        return sbiApiUsername;
    }

    public static void setSbiApiUsername(String sbiApiUsername) {
        SbiConfig.sbiApiUsername = sbiApiUsername;
    }

    public static String getSbiApiPassword() {
        return sbiApiPassword;
    }

    public static void setSbiApiPassword(String sbiApiPassword) {
        SbiConfig.sbiApiPassword = sbiApiPassword;
    }
}
