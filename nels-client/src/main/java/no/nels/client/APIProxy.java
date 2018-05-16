package no.nels.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.nels.client.model.FileFolder;
import no.nels.client.model.SSHCredential;
import no.nels.client.model.responses.GetVersionResponse;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Kidane on 25.11.2015.
 */
public final class APIProxy extends ApiClient{

    private static Logger logger = LoggerFactory.getLogger(APIProxy.class);

    public static String getUploadReference(long nelsId, String relativePath) {
        return getReference(nelsId, relativePath, Config.getExtraApiUrl() + "/users/" + nelsId + "/upload/reference");
    }

    public static String getDownloadReference(long nelsId, String relativePath) {
        return getReference(nelsId, relativePath, Config.getExtraApiUrl() + "/users/" + nelsId + "/download/reference");
    }

    private static String getReference(long nelsId, String relativePath, String url) {

        JsonObject body = new JsonObject();
        body.put("path", relativePath);
        Response response = getStorageClient().target(url).request(MediaType.APPLICATION_JSON).post(Entity.json(body.encode()));
        if (response.getStatus() != 200) {
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
        }
        return response.readEntity(String.class);
    }

    public static String getVersion() throws Exception {
        Response response = getStorageClient().target(Config.getFullUrl("version")).request(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() != 200) {
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
        }
        return new ObjectMapper().readValue(response.readEntity(String.class), GetVersionResponse.class).getVersion();
    }

    public static boolean registerNeLSUser(long callerId, long nelsId, String name, int userType) {

        Response response = getMasterClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId)).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(""));
        return response.getStatus() == 201;
    }

    public static boolean removeUser(long nelsId) {

        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId)).request().delete();

        return response.getStatus() == 204;
    }

    public static SSHCredential getSshCredential(long callerId, long nelsId) throws Exception {


        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId)).request(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() != 200) {
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
        }
        JsonObject jsonObject = new JsonObject(response.readEntity(String.class));

        return new SSHCredential(jsonObject.getString("ssh_host"), jsonObject.getString("user_name"), jsonObject.getString("ssh_key"));
    }

    private static Date parseDate(String dateString) {
        // parsing the datetime string to an object
        int year = Integer.valueOf(StringUtils.substringBefore(dateString, "-").trim());
        String rightOfyear = StringUtils.substringAfter(dateString, "-").trim();
        int month = Integer.valueOf(StringUtils.substringBefore(rightOfyear, "-").trim());
        String rightOfMonth = StringUtils.substringAfter(rightOfyear, "-").trim();
        int date = Integer.valueOf(StringUtils.substringBefore(rightOfMonth, " ").trim());
        String rightOfDate = StringUtils.substringAfter(rightOfMonth, " ").trim();
        String[] hourMinute = StringUtils.substringAfter(rightOfMonth, " ").split(":");

        return new Date(year - 1900, month - 1, date,
                Integer.valueOf(hourMinute[0].trim()),
                Integer.valueOf(hourMinute[1].trim()));
    }

    public static List<FileFolder> listItems(long callerId, long nelsId,
                                             String path) throws Exception {
        logger.debug("in listItems. nelsId:" + nelsId + ",path:" + path);
        JsonObject body = new JsonObject();
        body.put("path", path);
        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId) + "/list")
                .request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body.encode()));
        if (response.getStatus() != 200) {
            String desc = response.readEntity(String.class);
            logger.error("listItems error. " + response.getStatus() + ",desc:" + desc);
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + desc));
        }

        List<FileFolder> ret = new ArrayList<>();
        JsonArray jsonArray = new JsonArray(response.readEntity(String.class));
        jsonArray.stream().map(object -> JsonObject.class.cast(object)).forEach(item -> {
            if (item.getString("type").equals("file")) {
                ret.add(new FileFolder(item.getString("name"), path + FileSystems.getDefault().getSeparator() + item.getString("name"), "", new Date(item.getLong("modified_time")), item.getLong("size"), false));
            } else {
                ret.add(new FileFolder(item.getString("name"), path + FileSystems.getDefault().getSeparator() + item.getString("name"), "", new Date(item.getLong("modified_time")), item.getLong("size"), true));
            }
        });
        return ret;
    }

    public static void renameFileSystemElement(long callerId, long nelsId,
                                               String parentFolderPath, String oldName, String newName)
            throws Exception {

        JsonObject body = new JsonObject();
        body.put("parent_path", parentFolderPath);
        body.put("old", oldName);
        body.put("new", newName);


        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId) + "/rename").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body.encode()));

        if (response.getStatus() != 204) {
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
        }
    }

    public static void createFolder(long callerId, long nelsId,
                                    String parentFolderPath, String name) throws Exception {
        {
            String fullFolderPath = parentFolderPath.endsWith("/") ? parentFolderPath + name : parentFolderPath + "/" + name;
            JsonObject body = new JsonObject();
            body.put("path", fullFolderPath);


            Response response = getStorageClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId) + "/create").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body.encode()));

            if (response.getStatus() != 201) {
                throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
            }
        }
    }

    public static void createFile(long callerId, long nelsId,
                                  String parentFolderPath, String name, InputStream inputStream) throws Exception {
        String fullFilePath = parentFolderPath.endsWith("/") ? parentFolderPath + name : parentFolderPath + "/" + name;
        String reference = getUploadReference(nelsId, fullFilePath);

        String url = Config.getExtraApiUrl() + "/upload/" + reference;
        Response response = getStorageClient().target(url).request()
                .property(ClientProperties.CHUNKED_ENCODING_SIZE, "4096")
                .property(ClientProperties.REQUEST_ENTITY_PROCESSING, "CHUNKED")
                .post(Entity.entity(inputStream, MediaType.MULTIPART_FORM_DATA));

        if (response.getStatus() != 200) {
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
        }
    }
    public static void deleteFileSystemElement(long callerId, long nelsId, List<String> selectedItems) throws Exception {
        JsonArray body = new JsonArray(selectedItems);

        Response response = getStorageClient().target(Config.getExtraApiUrl() + "/users/" + String.valueOf(nelsId) + "/delete").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body.encode()));

        if (response.getStatus() != 204) {
            throw (new RuntimeException("Failed: HTTP error code - " + response.getStatus() + "Error: " + response.readEntity(String.class)));
        }
    }

    public static void copyFileSystemElement(long callerId,long nelsId,String[] sourceElementPath,String destination_folder) throws Exception {

        Jobs.addCopyJob(nelsId, sourceElementPath, destination_folder);
    }

    public static void moveFileSystemElement(long callerId,long nelsId,String[] sourceElementPath,String destination_folder) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"source\":[\"").append(StringUtils.join(sourceElementPath, ",").toString()).append("\"],\"destination\":\"").append(destination_folder).append("\"}");


        Response response = ClientBuilder.newClient().target(Config.getExtraApiUrl() + "/users/" + Long.toString(nelsId) + "/data/move")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(builder.toString()));

        if (response.getStatus() != 200) {
            throw (new Exception("Failed: HTTP error code - " + response.getStatus()));
        }
    }
}
