package no.nels.client;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;

public class Config {

    public static String getFullUrl(String relativeUrl) throws Exception {

        return (apiUrl.endsWith("/") ? (apiUrl + relativeUrl) : (apiUrl + "/" + relativeUrl)).replace(" ", "%20");
    }
    public static String getVersion() throws Exception {
        return APIProxy.getVersion();
    }

    public static String getApiUrl() {
        return apiUrl;
    }

    public static void setApiUrl(String apiUrl) {
        Config.apiUrl = apiUrl;
    }

    private static String apiUrl = "";

    private static String extraApiUrl = "";

    private static String extraApiUsername;

    private static String extraApiPassword;

    private static String masterApiUrl = "";

    private static String masterApiUsername = "";
    private static String masterApiPassword = "";

    private static String timeout;

    public static String getTimeout() {
        return timeout;
    }

    public static void setTimeout(String timeout) {
        Config.timeout = timeout;
    }

    public static String getMasterApiUsername() {
        return masterApiUsername;
    }

    public static void setMasterApiUsername(String masterApiUsername) {
        Config.masterApiUsername = masterApiUsername;
    }

    public static String getMasterApiPassword() {
        return masterApiPassword;
    }

    public static void setMasterApiPassword(String masterApiPassword) {
        Config.masterApiPassword = masterApiPassword;
    }

    public static void setMasterApiUrl(String masterApiUrl) {
        Config.masterApiUrl = masterApiUrl;
    }

    public static String getMasterApiUrl() {
        return masterApiUrl;
    }

    public static String getExtraApiUrl() {
        return extraApiUrl;
    }

    public static void setExtraApiUrl(String extraApiUrl) {
        Config.extraApiUrl = extraApiUrl;
    }

    public static String getExtraApiUsername() {
        return extraApiUsername;
    }

    public static void setExtraApiUsername(String extraApiUsername) {
        Config.extraApiUsername = extraApiUsername;
    }

    public static String getExtraApiPassword() {
        return extraApiPassword;
    }

    public static void setExtraApiPassword(String extraApiPassword) {
        Config.extraApiPassword = extraApiPassword;
    }

    public static URI constructFullUrl(String pathTemplate, Object... objects) {
        return UriBuilder.fromUri(getMasterApiUrl()).path(pathTemplate).build(objects);
    }

    public static URI constructFullUrl(String pathTemplate, Object[] objects, Map<String, String> queryParam) {
        UriBuilder uriBuilder = UriBuilder.fromUri(getMasterApiUrl()).path(pathTemplate);
        for (Map.Entry<String, String> entry : queryParam.entrySet()) {
            uriBuilder = uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        return uriBuilder.build(objects);
    }
}
