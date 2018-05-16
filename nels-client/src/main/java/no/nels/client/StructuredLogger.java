package no.nels.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nels.client.model.LogRequest;
import no.nels.client.model.UserLoginEvent;
import no.nels.commons.constants.LogContextType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by weizhang on 3/4/16.
 */
public class StructuredLogger {

    public static boolean addUserLoginLog(long userId, String userName, String ip) {
        UserLoginEvent event = new UserLoginEvent(userName, ip);
        ObjectMapper mapper = new ObjectMapper();
        ObjectMapper mapper1 = new ObjectMapper();
        String text = null, payload = null;
        try {
            text = mapper.writeValueAsString(event);
            LogRequest request = new LogRequest(LogContextType.USER_LOGIN.getValue(), userId, userId, text);
            payload = mapper1.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(Config.getMasterApiUsername(), Config.getMasterApiPassword())
                .build();

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(feature) ;

        Client client = ClientBuilder.newClient(clientConfig);
        Response response = client.target(Config.getMasterApiUrl()+"/logs/add").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));

        int status = response.getStatus();
        boolean ret = true;
        if(201 != status){
            ret = false;
        }
        return ret;

    }
}
