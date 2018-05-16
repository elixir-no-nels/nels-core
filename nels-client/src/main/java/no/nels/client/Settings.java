package no.nels.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.codec.http.HttpResponseStatus;
import no.nels.client.model.responses.GetSettingResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by weizhang on 1/24/17.
 */
public class Settings extends ApiClient{
    private static final Logger logger = LogManager.getLogger(Settings.class);

    public  static boolean isSettingFound(String settingKey) {
        JsonObject json = new JsonObject();
        json.addProperty("key", settingKey);

        String payload =  new Gson().toJson(json);
        Response response = getMasterClient().target(Config.getMasterApiUrl()+"/settings/query").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));
        int status = response.getStatus();
        if (status != HttpResponseStatus.OK.code()) {
            logger.error("response code:" + status);
            return false;
        }

        List<GetSettingResponse> setting;
        String responseBody = response.readEntity(String.class);
        setting = new Gson().fromJson(responseBody, new TypeToken<ArrayList<GetSettingResponse>>() {}.getType());
        return !setting.isEmpty();
    }

    public  static boolean isSettingFound(String settingKey, long itemId) {
        return  isSettingFound(getItemSettingKey(settingKey, itemId));
    }

    public static boolean isSettingMatch(String settingKey, long itemId, String value) {
        JsonObject json = new JsonObject();
        json.addProperty("key", getItemSettingKey(settingKey, itemId));
        String payload =  new Gson().toJson(json);
        Response response = getMasterClient().target(Config.getMasterApiUrl()+"/settings/query").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));
        int status = response.getStatus();
        if (status != HttpResponseStatus.OK.code()) {
            logger.error(",response code:" + status);
            return false;
        }
        List<GetSettingResponse> setting;
        String responseBody = response.readEntity(String.class);
        setting = new Gson().fromJson(responseBody, new TypeToken<ArrayList<GetSettingResponse>>() {}.getType());
        return !setting.isEmpty() && setting.size() == 1 && setting.get(0).getSettingValue().equals(value);
    }



    public static boolean removeSetting(String settingKey, long itemId) {
        return removeSetting(getItemSettingKey(settingKey, itemId));
    }

    public static boolean removeSetting(String settingKey){
        JsonObject json = new JsonObject();
        json.addProperty("key", settingKey);
        json.addProperty("method", "delete");
        String payload =  new Gson().toJson(json);
        Response response = getMasterClient().target(no.nels.client.Config.getMasterApiUrl()+"/settings/do").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));

        int status = response.getStatus();

        if(HttpResponseStatus.NO_CONTENT.code() != status){
            return false;
        }
        return true;
    }

    private static String getItemSettingKey(String settingKey, long itemId) {
        return  settingKey + "-" + itemId;
    }

    public static boolean setSetting(String settingKey, String settingValue) {
        boolean ret;
        if(!isSettingFound(settingKey)){
            ret = createSetting(settingKey, settingValue);
        }else {
            ret = updateSetting(settingKey, settingValue);
        }
        return ret;
    }

    private static boolean createSetting(String settingKey, String settingValue) {
        JsonObject json = new JsonObject();
        json.addProperty("key", settingKey);
        json.addProperty("value", settingValue);
        String payload =  new Gson().toJson(json);

        Response response = getMasterClient().target(Config.getMasterApiUrl()+"/settings").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));
        int status = response.getStatus();
        if(HttpResponseStatus.CREATED.code() != status){
            return false;
        }
        return true;
    }



    private static boolean updateSetting(String settingKey, String settingValue) {

        JsonObject json = new JsonObject();
        json.addProperty("key", settingKey);
        json.addProperty("value", settingValue);
        json.addProperty("method", "update");

        String payload =  new Gson().toJson(json);

        Response response = getMasterClient().target(Config.getMasterApiUrl()+"/settings/do").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));
        int status = response.getStatus();
        if (status != HttpResponseStatus.NO_CONTENT.code()) {

            return false;
        }
        return true;
    }

    public static boolean setSetting(String settingKey, long itemId, String settingValue) {
        return setSetting(getItemSettingKey(settingKey, itemId), settingValue);
    }

    public static String getSetting(String settingKey) {
        JsonObject json = new JsonObject();
        json.addProperty("key", settingKey);

        String payload = new Gson().toJson(json);
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/settings/query").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));
        int status = response.getStatus();
        String result = null;
        if (status != HttpResponseStatus.OK.code()) {
            logger.error("response code:" + status);
            return result;
        }

        List<GetSettingResponse> setting;
        String responseBody = response.readEntity(String.class);
        setting = new Gson().fromJson(responseBody, new TypeToken<ArrayList<GetSettingResponse>>() {}.getType());
        if(setting.isEmpty()){
            result = null;
        }else {
            result = setting.get(0).getSettingValue();
        }

        return result;

    }

    public static String getSetting(String settingKey, long itemId) {
        return getSetting(getItemSettingKey(settingKey, itemId));
    }
}
