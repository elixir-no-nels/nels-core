package no.nels.client;

public class Admin {

    public static boolean createUser(long callerId, long nelsId, String name, int userType){
        return APIProxy.registerNeLSUser(callerId, nelsId, name, userType);
    }

    public static boolean deleteUser(long nelsId) {
        return APIProxy.removeUser(nelsId);
    }

    public static String getStorageUsername(long callerId, long nelsId) {
        try {
            return APIProxy.getSshCredential(callerId, nelsId).getUsername();
        } catch (Exception ex) {        }
        return "";
    }
}
