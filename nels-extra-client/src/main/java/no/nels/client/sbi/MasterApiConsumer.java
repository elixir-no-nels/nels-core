package no.nels.client.sbi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
            String remoteHost, String userName, String sshKey,
            String dataSetId, String dataSet, String subtype, long subtypeId,
            String parentPathOfSource, String destination_path, List<String> files, List<String> folders) throws SbiException{
        if (StringUtils.isEmpty(remoteHost)
                || StringUtils.isEmpty(userName)
                || StringUtils.isEmpty(sshKey)
                || StringUtils.isEmpty(dataSetId)
                || StringUtils.isEmpty(dataSet)
                || StringUtils.isEmpty(subtype)
                || parentPathOfSource == null
                || StringUtils.isEmpty(destination_path)) {
            throw new SbiException("Parameters could not be empty.");
        }

        if ((files == null || files.size() == 0) && (folders == null || folders.size() == 0)) {
            throw new SbiException("The transferred data can't be empty.");
        }

        JsonObject parameters = new JsonObject();
        parameters.addProperty("remote_host", remoteHost);
        parameters.addProperty("user_name", userName);
        parameters.addProperty("ssh_key", sshKey);
        parameters.addProperty("dataset_id", dataSetId);
        parameters.addProperty("dataset", dataSet);
        parameters.addProperty("subtype", subtype);
        parameters.addProperty("subtype_id", subtypeId);
        parameters.addProperty("parent_path_of_source", parentPathOfSource);
        parameters.addProperty("destination_path", destination_path);

        JsonArray filesArray = new JsonArray();
        files.stream().forEach(str -> filesArray.add(str));
        parameters.add("files", filesArray);

        JsonArray foldersArray = new JsonArray();
        folders.stream().forEach(str -> foldersArray.add(str));
        parameters.add("folders", foldersArray);


        JsonObject request = new JsonObject();
        request.addProperty("nels_id", nelsId);
        request.addProperty("job_type_id", jobTypeId);
        request.add("parameters", parameters);

        addSbiJob(request.toString());
    }

    public static void addPullDataFromNelsJob(
            long nelsId, long jobTypeId,
            String remoteHost, String userName, String sshKey,
            String dataSetId, String dataSet, String subtype, long subtypeId,
            String parentPathOfSource, String destination_path, List<String> files, List<String> folders) throws SbiException{

        if (StringUtils.isEmpty(remoteHost)
                || StringUtils.isEmpty(userName)
                || StringUtils.isEmpty(sshKey)
                || StringUtils.isEmpty(dataSetId)
                || StringUtils.isEmpty(dataSet)
                || StringUtils.isEmpty(subtype)
                || StringUtils.isEmpty(parentPathOfSource)
                || destination_path == null) {
            throw new SbiException("Parameters could not be empty.");
        }

        if ((files == null || files.size() == 0) && (folders == null || folders.size() == 0)) {
            throw new SbiException("The transferred data can't be empty.");
        }

        JsonObject parameters = new JsonObject();
        parameters.addProperty("remote_host", remoteHost);
        parameters.addProperty("user_name", userName);
        parameters.addProperty("ssh_key", sshKey);
        parameters.addProperty("dataset_id", dataSetId);
        parameters.addProperty("dataset", dataSet);
        parameters.addProperty("subtype", subtype);
        parameters.addProperty("subtype_id", subtypeId);
        parameters.addProperty("parent_path_of_source", parentPathOfSource);
        parameters.addProperty("destination_path", destination_path);
        JsonArray filesArray = new JsonArray();
        files.stream().forEach(str -> filesArray.add(str));
        parameters.add("files", filesArray);

        JsonArray foldersArray = new JsonArray();
        folders.stream().forEach(str -> foldersArray.add(str));
        parameters.add("folders", foldersArray);

        JsonObject request = new JsonObject();
        request.addProperty("nels_id", nelsId);
        request.addProperty("job_type_id", jobTypeId);
        request.add("parameters", parameters);

        addSbiJob(request.toString());
    }

    private static void addSbiJob(String request) throws SbiException{
        String url = SbiConfig.getMasterApiUrl() + "/jobs/add";
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(SbiConfig.getMasterApiUsername(), SbiConfig.getMasterApiPassword())
                .build();

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(feature);

        Client client;

        String timeout = SbiConfig.getTimeout();
        if (!StringUtils.isEmpty(timeout) && Long.valueOf(timeout) > 0) {
            client = ClientBuilder.newClient(clientConfig).property(ClientProperties.CONNECT_TIMEOUT, timeout).property(ClientProperties.READ_TIMEOUT, timeout);
        } else {
            client = ClientBuilder.newClient(clientConfig);
        }


        Response response = client.target(url).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(request));

        int status = response.getStatus();
        if (status != 201) {
            throw new SbiException("Not able to add job for url: " + url + ",response code:" + status);
        }
    }
}
