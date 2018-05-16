package no.nels.client.tsd;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import no.nels.client.tsd.models.TsdFileFolder;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

public final class TsdApiConsumer {
    private static Client getClient() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(TsdConfig.getTsdApiUsername(), TsdConfig.getTsdApiPassword())
                .build();

        ClientConfig config = new ClientConfig();

        String timeout = TsdConfig.getTimeout();
        if (!StringUtils.isEmpty(timeout) && Long.valueOf(timeout) > 0) {
            config.property(ClientProperties.CONNECT_TIMEOUT, timeout);
            config.property(ClientProperties.READ_TIMEOUT, timeout);
        }

        return ClientBuilder.newClient(config.register(feature));
    }

    public static String connectTo(String userName, String password, String otc) throws TsdException{
        JsonObject parameters = new JsonObject();
        parameters.addProperty("password", password);
        parameters.addProperty("otc", otc);
        Response response = getClient().target(TsdConfig.constructFullUrl("/users/{userName}/connect", userName)).request(MediaType.TEXT_PLAIN).post(Entity.json(parameters.toString()));
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            String reference = response.readEntity(String.class);
            return reference;
        } else {
            throw new TsdException(response.readEntity(String.class));
        }
    }

    public static List<TsdFileFolder> navigateTo(String reference, String userName, String path) throws TsdException{
        Response response = getClient().target(TsdConfig.getTsdRootUrl() + "/users/" + userName + "/" + path).request(MediaType.APPLICATION_JSON_TYPE).header("reference", reference).get();
        String parentPath = StringUtils.join(new String[]{userName.split("-")[0], path}, FileSystems.getDefault().getSeparator());
        return getTsdFileFolders(response, parentPath);
    }

    public static List<TsdFileFolder> navigateToHome(String reference, String userName) throws TsdException{
        Response response = getClient().target(TsdConfig.constructFullUrl("/users/{userName}", userName)).request(MediaType.APPLICATION_JSON_TYPE).header("reference", reference).get();
        String parentPath = userName.split("-")[0];
        return getTsdFileFolders(response, parentPath);
    }

    private static List<TsdFileFolder> getTsdFileFolders(Response response, String parentPath) throws TsdException{
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = jsonParser.parse(response.readEntity(String.class)).getAsJsonArray();
            List<TsdFileFolder> list = new ArrayList<>(jsonArray.size());
            jsonArray.forEach(element -> list.add(new TsdFileFolder(
                    element.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                    StringUtils.join(new String[]{parentPath, element.getAsJsonObject().getAsJsonPrimitive("name").getAsString()}, FileSystems.getDefault().getSeparator()),
                    element.getAsJsonObject().getAsJsonPrimitive("type").getAsString().equals("folder"),
                    element.getAsJsonObject().getAsJsonPrimitive("size").getAsLong())));
            return list;
        } else {
            throw new TsdException(response.readEntity(String.class));
        }
    }

    public static void disconnectTsdSession(String reference, String userName) throws TsdException{
        Response response = getClient().target(TsdConfig.constructFullUrl("/users/{userName}/disconnect", userName)).request().header("reference", reference).delete();
        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new TsdException(response.readEntity(String.class));
        }
    }
}
