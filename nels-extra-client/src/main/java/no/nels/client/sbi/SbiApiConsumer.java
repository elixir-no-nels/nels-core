package no.nels.client.sbi;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import no.nels.client.sbi.constants.JsonKey;
import no.nels.client.sbi.constants.URL;
import no.nels.client.sbi.models.*;
import no.nels.commons.constants.JsonObjectKey;
import no.nels.commons.constants.SbiRole;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static no.nels.client.sbi.constants.URL.CREATE_USER;
import static no.nels.client.sbi.constants.URL.QUOTAS;

public final class SbiApiConsumer {

    public static Client getClient() {
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .nonPreemptive()
                .credentials(SbiConfig.getSbiApiUsername(), SbiConfig.getSbiApiPassword())
                .build();

        ClientConfig config = new ClientConfig();

        String timeout = SbiConfig.getTimeout();
        if (!StringUtils.isEmpty(timeout) && Long.valueOf(timeout) > 0) {
            config.property(ClientProperties.CONNECT_TIMEOUT, timeout);
            config.property(ClientProperties.READ_TIMEOUT, timeout);
        }

        return ClientBuilder.newClient(config.register(feature));
    }

    public static String getProjectMembers(long projectId) throws SbiException{
        return new Gson().toJson(getProjectMembersJson(projectId));
    }

    public static JsonArray getProjectMembersJson(long projectId) throws SbiException{
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/users", projectId)).request(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = jsonParser.parse(response.readEntity(String.class)).getAsJsonArray();
            JsonArray newJsonArray = new JsonArray();
            JsonObject jsonObject;
            for (JsonElement element : jsonArray) {
                jsonObject = new JsonObject();
                jsonObject.addProperty(JsonKey.ID, element.getAsJsonObject().getAsJsonPrimitive(JsonKey.USER_ID).getAsInt());
                String firstname = element.getAsJsonObject().get(JsonKey.FIRSTNAME).isJsonNull() ? "" : element.getAsJsonObject().getAsJsonPrimitive(JsonKey.FIRSTNAME).getAsString();
                String surname = element.getAsJsonObject().get(JsonKey.SURNAME).isJsonNull() ? "" : element.getAsJsonObject().getAsJsonPrimitive(JsonKey.SURNAME).getAsString();
                jsonObject.addProperty(JsonKey.NAME, firstname + " " + surname);
                jsonObject.addProperty(JsonKey.FEDERATED_ID, element.getAsJsonObject().get(JsonKey.FEDERATED_ID).isJsonNull() ? element.getAsJsonObject().getAsJsonPrimitive(JsonKey.EMAIL).getAsString() : element.getAsJsonObject().getAsJsonPrimitive(JsonKey.FEDERATED_ID).getAsString());
                jsonObject.addProperty(JsonKey.ROLE, SbiRole.nameOf(element.getAsJsonObject().getAsJsonPrimitive(JsonKey.URP_ROLE_ID).getAsInt()).getName());
                newJsonArray.add(jsonObject);
            }
            return newJsonArray;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String removeUsersFromProject(long projectId, String jsonBody) throws SbiException {
        return changeMembers(projectId, jsonBody);
    }

    public static String addUsersToProject(long projectId, String jsonBody) throws SbiException{
        return changeMembers(projectId, jsonBody);
    }

    private static String changeMembers(long projectId, String jsonBody) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/users/do", projectId)).request().post(Entity.json(jsonBody));
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class));
        } else {
            return response.readEntity(String.class);
        }
    }

    public static SbiProject getProject(long projectId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}", projectId))
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(response.readEntity(String.class)).getAsJsonObject();
            Date date = Date.from(LocalDate.parse(jsonObject.getAsJsonPrimitive("startdate").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx")).atStartOfDay(ZoneId.systemDefault()).toInstant());
            return new SbiProject(
                    jsonObject.getAsJsonPrimitive("project_id").getAsLong(),
                    jsonObject.getAsJsonPrimitive("name").getAsString(),
                    jsonObject.get("contact_person").isJsonNull() ? "" : jsonObject.getAsJsonPrimitive("contact_person").getAsString(),
                    jsonObject.get("description").isJsonNull()? "" : jsonObject.getAsJsonPrimitive("description").getAsString(),
                    jsonObject.get("contact_email").isJsonNull()? "" : jsonObject.getAsJsonPrimitive("contact_email").getAsString(),
                    jsonObject.get("contact_affiliation").isJsonNull()? "" : jsonObject.getAsJsonPrimitive("contact_affiliation").getAsString(),
                    date,
                    jsonObject.getAsJsonPrimitive("quota_name").getAsString(),
                    jsonObject.getAsJsonPrimitive("quota_size").getAsLong(),
                    jsonObject.getAsJsonPrimitive("used_size").getAsLong(),
                    jsonObject.getAsJsonPrimitive("disk_usage").getAsLong());
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static void deleteQuota(long quotaId, String federatedId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/quotas/{quotaId}", quotaId)).request().header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).delete();

        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static void updateQuota(long quotaId, String body) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/quotas/{quotaId}", quotaId)).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(body));
        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static void createQuota(String body) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/quotas")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body));
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static SbiQuota getQuota(long quotaId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/quotas/{quotaId}", quotaId))
                .request(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(response.readEntity(String.class)).getAsJsonObject();
            Date date = Date.from(LocalDate.parse(jsonObject.getAsJsonPrimitive("created").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay(ZoneId.systemDefault()).toInstant());
            return new SbiQuota(
                    jsonObject.getAsJsonPrimitive("id").getAsLong(),
                    jsonObject.getAsJsonPrimitive("name").getAsString(), date,
                    jsonObject.get("description").isJsonNull() ? "" : jsonObject.getAsJsonPrimitive("description").getAsString(),
                    jsonObject.getAsJsonPrimitive("quota_size").getAsLong(),
                    jsonObject.getAsJsonPrimitive("used_size").getAsLong(),
                    jsonObject.getAsJsonPrimitive("quota_id").getAsLong());
        } else {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static String getAllQuotas(String limit, String offset, String sort) throws ParseException, SbiException {
        List<String> query = new ArrayList<>();
        String queryStr;

        if (!limit.isEmpty()) {
            query.add("limit=" + limit);
        }
        if (!offset.isEmpty()) {
            query.add("offset=" + offset);
        }
        if (!sort.isEmpty()) {
            query.add("sort=" + sort);
        }

        switch (query.size()) {
            case 0:
                queryStr = "";
                break;
            case 1:
                queryStr = "?" + query.get(0);
                break;
            default:
                queryStr = "?" + query.stream().collect(Collectors.joining("&"));
                break;
        }

        Response response = getClient().target(SbiConfig.constructFullUrl(QUOTAS) + queryStr).request(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }

    }




    public static List<SbiUser> searchSbiUsers(List<String> federatedIdLst) throws SbiException {

        JsonObject json = new JsonObject();

        JsonArray federatedIds = new JsonArray();
        for (String f : federatedIdLst) {
            federatedIds.add(f);
        }
        json.add(JsonKey.FEDERATED_ID, federatedIds);

        String payload = new Gson().toJson(json);
        Response response = getClient().target(SbiConfig.constructFullUrl(URL.QUERY_USER)).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(payload));
        int status = response.getStatus();
        if (status == Response.Status.OK.getStatusCode()) {
            String responseStr = response.readEntity(String.class);
            List<SbiUser> sbiUsers;
            sbiUsers = new Gson().fromJson(responseStr, new TypeToken<ArrayList<SbiUser>>() {
            }.getType());
            return sbiUsers;

        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String getUsers(Optional<String> limit, Optional<String> offset) throws SbiException {
        Map<String, String> queryParam = new HashMap<>();
        limit.ifPresent(param -> queryParam.put("limit", param));
        offset.ifPresent(param -> queryParam.put("offset", param));
        Response response;
        if (queryParam.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/users", new Object[0], queryParam)).request(MediaType.APPLICATION_JSON_TYPE).get();
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/users")).request(MediaType.APPLICATION_JSON_TYPE).get();
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonObject responseBody = jsonParser.parse(response.readEntity(String.class)).getAsJsonObject();
            JsonArray jsonArray = responseBody.getAsJsonArray(JsonKey.DATA);
            JsonArray newJsonArray = new JsonArray();
            JsonObject jsonObject;
            for (JsonElement element : jsonArray) {
                jsonObject = new JsonObject();
                jsonObject.addProperty(JsonKey.ID, element.getAsJsonObject().getAsJsonPrimitive(JsonKey.USER_ID).getAsInt());
                String firstname = element.getAsJsonObject().get(JsonKey.FIRSTNAME).isJsonNull() ? "" : element.getAsJsonObject().getAsJsonPrimitive(JsonKey.FIRSTNAME).getAsString();
                String surname = element.getAsJsonObject().get(JsonKey.SURNAME).isJsonNull() ? "" : element.getAsJsonObject().getAsJsonPrimitive(JsonKey.SURNAME).getAsString();
                jsonObject.addProperty(JsonKey.NAME, firstname + " " + surname);
                jsonObject.addProperty(JsonKey.EMAIL, element.getAsJsonObject().getAsJsonPrimitive(JsonKey.EMAIL).getAsString());
                jsonObject.addProperty(JsonKey.FEDERATED_ID, element.getAsJsonObject().get(JsonKey.FEDERATED_ID).isJsonNull() ? element.getAsJsonObject().getAsJsonPrimitive(JsonKey.EMAIL).getAsString() : element.getAsJsonObject().getAsJsonPrimitive(JsonKey.FEDERATED_ID).getAsString());
                newJsonArray.add(jsonObject);
            }
            responseBody.remove(JsonKey.DATA);
            responseBody.add(JsonKey.DATA, newJsonArray);
            return new Gson().toJson(responseBody);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String createProject(String body) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body));
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static void deleteProject(long projectId, String body) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}", projectId)).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(body));

        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static void updateProject(long projectId, String body) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}", projectId)).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(body));
        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static String getProjects(Map<String, String> urlParameters) throws SbiException {
        Response response;
        if (urlParameters != null && urlParameters.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/projects", new Object[0], urlParameters)).request(MediaType.APPLICATION_JSON_TYPE).get();
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/projects")).request(MediaType.APPLICATION_JSON_TYPE).get();
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String getProjectsInQuota(long quotaId, Map<String, String> urlParameters) throws SbiException {
        Response response;
        if (urlParameters != null && urlParameters.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/quotas/{quotaId}/projects", new Object[]{quotaId}, urlParameters)).request(MediaType.APPLICATION_JSON_TYPE).get();
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/quotas/{quotaId}/projects", quotaId)).request(MediaType.APPLICATION_JSON_TYPE).get();
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static List<SbiProject> getProjects(String federatedId) throws ParseException, SbiException {

        Response response = getClient().target(SbiConfig.constructFullUrl("/projects")).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        return returnProjects(response);
    }

    private static List<SbiProject> returnProjects(Response response) throws ParseException, SbiException {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = jsonParser.parse(response.readEntity(String.class)).getAsJsonArray();
            List<SbiProject> sbiProjects = new ArrayList<>(jsonArray.size());
            Date date;
            for (JsonElement item : jsonArray) {
                date = Date.from(LocalDate.parse(item.getAsJsonObject().getAsJsonPrimitive("startdate").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx")).atStartOfDay(ZoneId.systemDefault()).toInstant());
                sbiProjects.add(
                        new SbiProject(
                                item.getAsJsonObject().getAsJsonPrimitive("project_id").getAsLong(),
                                item.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                                item.getAsJsonObject().get("contact_person").isJsonNull() ? "" : item.getAsJsonObject().getAsJsonPrimitive("contact_person").getAsString(),
                                item.getAsJsonObject().get("description").isJsonNull()? "" : item.getAsJsonObject().getAsJsonPrimitive("description").getAsString(),
                                date));
            }
            return sbiProjects;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static boolean createDataSet(String federatedId, long projectId, long datasetTypeId, String name, String description) {

        JsonObject json = new JsonObject();
        json.addProperty("data_set_type_id", datasetTypeId);
        json.addProperty("name", name);
        json.addProperty("description", description);
        String payload = new Gson().toJson(json);
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets", projectId))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId)
                .post(Entity.json(payload));
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            return true;
        } else {
            return false;
        }

    }

    public static List<SbiDataSetType> getDataSetTypes(String federatedId) throws ParseException, SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/datasettypes")).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = jsonParser.parse(response.readEntity(String.class)).getAsJsonObject().getAsJsonArray("data").getAsJsonArray();
            List<SbiDataSetType> sbiDataSetTypes = new ArrayList<>(jsonArray.size());
            for (JsonElement item : jsonArray) {

                sbiDataSetTypes.add(new SbiDataSetType(
                        item.getAsJsonObject().getAsJsonPrimitive("id").getAsLong(),
                        item.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                        item.getAsJsonObject().getAsJsonPrimitive("description").getAsString()));
            }
            return sbiDataSetTypes;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String getDataSetTypesAsString(Map<String, String> urlParameters) throws SbiException {
        Response response;
        if (urlParameters != null && urlParameters.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/datasettypes", new Object[0], urlParameters)).request(MediaType.APPLICATION_JSON).get();
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/datasettypes")).request(MediaType.APPLICATION_JSON).get();
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String getDataSetType(long dataSetTypeId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/datasettypes/{dataSetTypeId}", dataSetTypeId)).request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static void deleteDataSetType(long dataSetTypeId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/datasettypes/{dataSetTypeId}", dataSetTypeId)).request().delete();
        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }

    public static void createDataSetType(String body) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/datasettypes")).request().post(Entity.json(body));
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static List<SbiDataSet> getDataSets(String federatedId, long projectId) throws ParseException, SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets", projectId)).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = jsonParser.parse(response.readEntity(String.class)).getAsJsonArray();
            List<SbiDataSet> sbiDataSets = new ArrayList<>(jsonArray.size());
            Date date;
            for (JsonElement item : jsonArray) {
                date = Date.from(LocalDate.parse(item.getAsJsonObject().getAsJsonPrimitive("created").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx")).atStartOfDay(ZoneId.systemDefault()).toInstant());
                sbiDataSets.add(new SbiDataSet(
                        item.getAsJsonObject().getAsJsonPrimitive("id").getAsLong(),
                        item.getAsJsonObject().getAsJsonPrimitive("data_set_id").getAsString(),
                        item.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                        item.getAsJsonObject().getAsJsonPrimitive("type").getAsString(),
                        item.getAsJsonObject().get("locked").isJsonNull() ? false : item.getAsJsonObject().getAsJsonPrimitive("locked").getAsBoolean(),
                        item.getAsJsonObject().get("owner_name").isJsonNull() ? "" : item.getAsJsonObject().getAsJsonPrimitive("owner_name").getAsString(),
                        item.getAsJsonObject().get("description").isJsonNull() ? "" : item.getAsJsonObject().getAsJsonPrimitive("description").getAsString(),
                        date));
            }
            return sbiDataSets;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static void deleteProjectDataSet(String federatedId, long projectId, long dataSetId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets/{dataSetId}", projectId, dataSetId)).request().header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).delete();

        if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            throw new SbiException(response.readEntity(String.class), response.getStatus());
        }
    }


    public static SbiDataSet getDataSet(String federatedId, long projectId, long dataSetId) throws ParseException, SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets/{dataSetId}", projectId, dataSetId)).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(response.readEntity(String.class)).getAsJsonObject();

            return new SbiDataSet(
                    jsonObject.getAsJsonPrimitive("id").getAsLong(),
                    jsonObject.getAsJsonPrimitive("data_set_id").getAsString(),
                    jsonObject.getAsJsonPrimitive("name").getAsString(),
                    jsonObject.getAsJsonPrimitive("type").getAsString(),
                    !jsonObject.get("locked").isJsonNull() && jsonObject.getAsJsonPrimitive("locked").getAsBoolean(),
                    jsonObject.get("owner_name").isJsonNull() ? "" : jsonObject.getAsJsonPrimitive("owner_name").getAsString(),
                    jsonObject.get("description").isJsonNull() ? "" : jsonObject.getAsJsonPrimitive("description").getAsString(),
                    Date.from(LocalDate.parse(jsonObject.getAsJsonPrimitive("created").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx")).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static List<SbiSubtype> getSubtypes(String federatedId, long projectId, long dataSetId) throws ParseException, SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets/{dataSetId}/subtypes", projectId, dataSetId)).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = jsonParser.parse(response.readEntity(String.class)).getAsJsonArray();
            List<SbiSubtype> sbiSubtypes = new ArrayList<>(jsonArray.size());
            Date date;
            for (JsonElement item : jsonArray) {
                date = Date.from(LocalDate.parse(item.getAsJsonObject().getAsJsonPrimitive("created").getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx")).atStartOfDay(ZoneId.systemDefault()).toInstant());
                sbiSubtypes.add(new SbiSubtype(
                        item.getAsJsonObject().getAsJsonPrimitive("id").getAsLong(),
                        item.getAsJsonObject().getAsJsonPrimitive("name").getAsString(),
                        item.getAsJsonObject().getAsJsonPrimitive("type").getAsString(),
                        item.getAsJsonObject().getAsJsonPrimitive("size").getAsLong(),
                        date));
            }
            return sbiSubtypes;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static List<SbiData> getSubtype(String federatedId, long projectId, long dataSetId, long subtypeId) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets/{dataSetId}/subtypes/{subtypeId}", projectId, dataSetId, subtypeId)).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        return fetchSbiSubtypeContent(response);
    }

    public static List<SbiData> getContent(String federatedId, long projectId, long dataSetId, long subtypeId, String relativePath) throws SbiException {
        Response response = getClient().target(SbiConfig.constructFullUrl("/projects/{projectId}/datasets/{dataSetId}/subtypes/{subtypeId}/" + relativePath, projectId, dataSetId, subtypeId)).request(MediaType.APPLICATION_JSON_TYPE).header(JsonKey.FEDERATED_ID_IN_HEADER, federatedId).get();
        return fetchSbiSubtypeContent(response);
    }

    private static List<SbiData> fetchSbiSubtypeContent(Response response) throws SbiException {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(response.readEntity(String.class)).getAsJsonObject();
            String relativePath = jsonObject.getAsJsonPrimitive("path").getAsString();
            JsonObject files = jsonObject.getAsJsonObject("files");
            JsonArray folders = jsonObject.getAsJsonArray("folders");
            List<SbiData> sbiDataList = new ArrayList<>();
            int number = 0;
            for (JsonElement item : folders) {
                sbiDataList.add(new SbiData(number++, item.getAsString(), 0, true, relativePath));
            }
            for (Map.Entry<String, JsonElement> entry : files.entrySet()) {
                sbiDataList.add(new SbiData(number++, entry.getKey(), Long.parseLong(entry.getValue().getAsString()), false, relativePath));
            }
            return sbiDataList;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static boolean createUser(String name, String email, String federatedId) throws SbiException {

        JsonObject json = new JsonObject();
        json.addProperty(JsonKey.NAME, name);
        json.addProperty(JsonKey.EMAIL, email);
        json.addProperty(JsonKey.FEDERATED_ID, federatedId);
        String payload = new Gson().toJson(json);
        Response response = getClient().target(SbiConfig.constructFullUrl(CREATE_USER))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(payload));
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            return true;
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String searchProjects(String query, Map<String, String> urlParameters) throws SbiException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty(JsonObjectKey.QUERY, query);
        String json = new Gson().toJson(requestBody);
        Response response;
        if (urlParameters != null && urlParameters.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/projects/query", new Object[0], urlParameters)).request(MediaType.APPLICATION_JSON).post(Entity.json(json));
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/projects/query")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

    public static String searchQuotas(String queryStr, Optional<String> limit, Optional<String> offset, Optional<String> sort) throws SbiException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty(JsonObjectKey.QUERY, queryStr);
        Map<String, String> queryParam = new HashMap<>();
        limit.ifPresent(param -> queryParam.put("limit", param));
        offset.ifPresent(param -> queryParam.put("offset", param));
        sort.ifPresent(param -> queryParam.put("sort", param));
        String json = new Gson().toJson(requestBody);
        Response response;
        if (queryParam.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/quotas/query", new Object[0], queryParam)).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/quotas/query")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(json));
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }

    }

    public static String searchDataSetTypes(String queryBody, Map<String, String> urlParameters) throws SbiException {
        Response response;
        if (urlParameters != null && urlParameters.size() > 0) {
            response = getClient().target(SbiConfig.constructFullUrl("/datasettypes/query", new Object[0], urlParameters)).request(MediaType.APPLICATION_JSON).post(Entity.json(queryBody));
        } else {
            response = getClient().target(SbiConfig.constructFullUrl("/datasettypes/query")).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(queryBody));
        }
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        } else {
            throw new SbiException(response.readEntity(String.class));
        }
    }

}
