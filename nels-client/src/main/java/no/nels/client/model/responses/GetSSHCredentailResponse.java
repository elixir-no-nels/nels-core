package no.nels.client.model.responses;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by Kidane on 04.12.2015.
 */

public class GetSSHCredentailResponse {

    public GetSSHCredentailResponse(){}

    private String rsaKey, hostName,userName;

    public String getRsaKey() {
        return rsaKey;
    }

    public void setRsaKey(String rsaKey) {
        this.rsaKey = rsaKey;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
