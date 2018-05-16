package no.nels.portal.model.enumerations;


public final class Oauth2ResponseTypes {
    public static final String TOKEN = "token";
    public static final String CODE = "code";

    public static boolean isValid(String responseType) {
        return responseType.equalsIgnoreCase(TOKEN) || responseType.equalsIgnoreCase(CODE);
    }
}
