package no.nels.client.model.responses;

import com.google.gson.annotations.SerializedName;

/**
 * Created by weizhang on 2/21/17.
 */
public class GetSettingResponse {

    private long id;
    @SerializedName("setting_key")
    private String settingKey;
    @SerializedName("setting_value")
    private String settingValue;

    public GetSettingResponse() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
