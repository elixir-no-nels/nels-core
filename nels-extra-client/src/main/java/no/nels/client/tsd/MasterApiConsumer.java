package no.nels.client.tsd;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import no.nels.client.sbi.SbiConfig;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

public final class MasterApiConsumer {
    public static void addPushDataToNelsJob(
            long nelsId, long jobTypeId,
            String nelsUserName, String nelsHost, String sshKey, String parentPathOfNels,
            String tsdUserName, String reference, String parentPathOfTsd, List<String> files, List<String> folders) throws TsdException{
        if (StringUtils.isEmpty(nelsUserName)
                || StringUtils.isEmpty(nelsHost)
                || StringUtils.isEmpty(sshKey)
                || StringUtils.isEmpty(parentPathOfNels)
                || StringUtils.isEmpty(tsdUserName)
                || StringUtils.isEmpty(reference)
                || StringUtils.isEmpty(parentPathOfTsd)) {
            throw new TsdException("Parameters could not be empty.");
        }

        if ((files == null || files.size() == 0) && (folders == null || folders.size() == 0)) {
            throw new TsdException("The transferred data can't be empty.");
        }

        JsonObject source = new JsonObject();
        source.addProperty("parent_path", parentPathOfTsd);
        source.addProperty("user_name", tsdUserName);
        source.addProperty("reference", reference);

        JsonArray filesArray = new JsonArray();
        files.stream().forEach(str -> filesArray.add(str));
        source.add("files", filesArray);

        JsonArray foldersArray = new JsonArray();
        folders.stream().forEach(str -> foldersArray.add(str));
        source.add("folders", foldersArray);

        JsonObject destination = new JsonObject();
        destination.addProperty("parent_path", parentPathOfNels);
        destination.addProperty("user_name", nelsUserName);
        destination.addProperty("host", nelsHost);
        destination.addProperty("ssh_key", sshKey);

        JsonObject parameters = new JsonObject();
        parameters.add("source", source);
        parameters.add("destination", destination);

        JsonObject request = new JsonObject();
        request.addProperty("nels_id", nelsId);
        request.addProperty("job_type_id", jobTypeId);
        request.add("parameters", parameters);

        addTsdJob(request.toString());
    }

    public static void addPullDataFromNelsJob(long nelsId, long jobTypeId,
                                              String nelsUserName, String nelsHost, String sshKey, String parentPathOfNels, List<String> files, List<String> folders,
                                              String tsdUserName, String reference, String parentPathOfTsd) throws TsdException{
        if (StringUtils.isEmpty(nelsUserName)
                || StringUtils.isEmpty(nelsHost)
                || StringUtils.isEmpty(sshKey)
                || StringUtils.isEmpty(parentPathOfNels)
                || StringUtils.isEmpty(tsdUserName)
                || StringUtils.isEmpty(reference)
                || StringUtils.isEmpty(parentPathOfTsd)) {
            throw new TsdException("Parameters could not be empty.");
        }

        if ((files == null || files.size() == 0) && (folders == null || folders.size() == 0)) {
            throw new TsdException("The transferred data can't be empty.");
        }

        JsonObject source = new JsonObject();
        source.addProperty("parent_path", parentPathOfNels);
        source.addProperty("user_name", nelsUserName);
        source.addProperty("host", nelsHost);
        source.addProperty("ssh_key", sshKey);

        JsonArray filesArray = new JsonArray();
        files.stream().forEach(str -> filesArray.add(str));
        source.add("files", filesArray);

        JsonArray foldersArray = new JsonArray();
        folders.stream().forEach(str -> foldersArray.add(str));
        source.add("folders", foldersArray);

        JsonObject destination = new JsonObject();
        destination.addProperty("parent_path", parentPathOfTsd);
        destination.addProperty("user_name", tsdUserName);
        destination.addProperty("reference", reference);

        JsonObject parameters = new JsonObject();
        parameters.add("source", source);
        parameters.add("destination", destination);

        JsonObject request = new JsonObject();
        request.addProperty("nels_id", nelsId);
        request.addProperty("job_type_id", jobTypeId);
        request.add("parameters", parameters);

        addTsdJob(request.toString());
    }

    private static void addTsdJob(String request) throws TsdException{
        String url = SbiConfig.getMasterApiUrl() + "/jobs/add";
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(TsdConfig.getMasterApiUsername(), TsdConfig.getMasterApiPassword())
                .build();

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(feature);

        Client client;

        String timeout = TsdConfig.getTimeout();
        if (!StringUtils.isEmpty(timeout) && Long.valueOf(timeout) > 0) {
            client = ClientBuilder.newClient(clientConfig).property(ClientProperties.CONNECT_TIMEOUT, timeout).property(ClientProperties.READ_TIMEOUT, timeout);
        } else {
            client = ClientBuilder.newClient(clientConfig);
        }


        Response response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(request));

        int status = response.getStatus();
        if (status != 201) {
            throw new TsdException("Not able to add job for url: " + url + ",response code:" + status);
        }
    }
}
