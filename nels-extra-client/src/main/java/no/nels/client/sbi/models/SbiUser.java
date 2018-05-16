package no.nels.client.sbi.models;

import com.google.gson.annotations.SerializedName;
import no.nels.client.sbi.constants.JsonKey;

/**
 * Created by weizhang on 5/18/17.
 */
public class SbiUser {
    @SerializedName(JsonKey.USER_ID)
    private long id;
    private String email;
    private String username;
    @SerializedName(JsonKey.FEDERATED_ID)
    private String federatedId;

    public SbiUser() {
    }

    public SbiUser(long id, String email, String username, String federatedId) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.federatedId = federatedId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFederatedId() {
        return federatedId;
    }

    public void setFederatedId(String federatedId) {
        this.federatedId = federatedId;
    }
}
