package no.nels.portal.model;

import com.google.gson.Gson;

import java.util.Hashtable;

public class Oauth2Config {

    public class Oauth2Client {
        String clientId;
        String clientSecret;
        String redirectUri;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        @Override
        public String toString() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    String encryptionKey;
    String oauth2ServerUrl;
    Oauth2Client[] implicitClients;
    Hashtable index = new Hashtable();

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public Oauth2Client[] getImplicitClients() {
        return implicitClients;
    }

    public void setImplicitClients(Oauth2Client[] implicitClients) {
        this.implicitClients = implicitClients;
    }

    public String getOauth2ServerUrl() {
        return oauth2ServerUrl;
    }

    public void setOauth2ServerUrl(String oauth2ServerUrl) {
        this.oauth2ServerUrl = oauth2ServerUrl;
    }

    public void indexClients() {
        this.index = new Hashtable();
        for (Oauth2Client client : this.getImplicitClients()) {
            this.index.put(client.getClientId(), client);
        }
    }

    public boolean isClientIdValid(String clientId) {
        return this.index.containsKey(clientId);
    }

    public Oauth2Client getClient(String clientId) {
        if (isClientIdValid(clientId)) {
            return (Oauth2Client) this.index.get(clientId);
        }
        return null;
    }
}
