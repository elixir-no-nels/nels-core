package no.nels.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import no.nels.client.model.StatsInfo;
import no.nels.client.model.StatsRequest;
import no.nels.client.model.enumerations.StatsContextType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by weizhang on 3/29/16.
 */
public class StatisticsApi extends ApiClient{

    private static void addStats(long type, long id, double value) {
        StatsInfo statsInfo = new StatsInfo(id, value);
        ObjectMapper mapper = new ObjectMapper();
        ObjectMapper mapper1 = new ObjectMapper();
        String text = null, payload = null;
        try {
            text = mapper.writeValueAsString(statsInfo);
            StatsRequest request = new StatsRequest(id, type, value);
            payload = mapper1.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Response response = getMasterClient().target(Config.getMasterApiUrl()+"/stats/add").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));

        int status = response.getStatus();

        if(201 != status){
            throw new RuntimeException("Not able to add statistics info for url: " + Config.getMasterApiUrl() + ",username:" + Config.getMasterApiUsername() + ",password:" + Config.getMasterApiPassword() +",type:" + type + ",id:" + id  + ",response code:" + status + ",pay load: " + payload + ",text:" + text);
        }
    }

    public static void addPersonalDiskUsageStats(long id, double value) {
        addStats(StatsContextType.PERSONAL_DISK_USAGE, id, value);
    }

    public static void addProjectDiskUsageStats(long id, double value) {
        addStats(StatsContextType.PROJECT_DISK_USAGE, id, value);

    }

    public static JsonArray getStatsOfTarget(long contextId, long targetId) {
        Response response = getMasterClient().target(Config.getMasterApiUrl() + "/stats/contexts/" + contextId + "/targets/" + targetId).request().get();
        if (response.getStatus() == HttpResponseStatus.OK.code()) {
            return new JsonArray(response.readEntity(String.class));
        } else {
            throw new InternalServerErrorException();
        }
    }
}
